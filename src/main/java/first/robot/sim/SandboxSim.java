package first.robot.sim;

import org.wpilib.framework.RobotBase;
import org.wpilib.hardware.hal.SimDouble;
import org.wpilib.math.system.DCMotor;
import org.wpilib.math.system.Models;
import org.wpilib.networktables.BooleanEntry;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.simulation.BatterySim;
import org.wpilib.simulation.DIOSim;
import org.wpilib.simulation.FlywheelSim;
import org.wpilib.simulation.RoboRioSim;
import org.wpilib.simulation.SimDeviceSim;

import first.robot.Constants;
import first.robot.Robot;

/**
 * Simulation only: the physics of the intake rollers and shooter flywheel, and a simple model of
 * one game piece moving through the robot. It reads the robot from the outside (motor throttles
 * and the beam break) and never changes the robot code.
 */
public final class SandboxSim {
  /** Where the game piece is. */
  public enum PieceState {
    /** No game piece anywhere. */
    NONE,
    /** Waiting on the floor in front of the intake. */
    ON_FLOOR,
    /** Between the rollers, travelling in (or out). */
    IN_INTAKE,
    /** Stopped at the beam break, blocking the beam. */
    HELD,
    /** Moving from the beam break into the shooter. */
    FEEDING,
    /** Flying out of the shooter. */
    LAUNCHED,
    /** Rolled out the front of the intake. */
    EJECTED
  }

  // Path positions: 0.0 is the floor in front of the intake, 0.5 the beam break, 1.0 the shooter.
  static final double kBeamBreakPosition = 0.5;
  static final double kShooterPosition = 1.0;

  /** The flywheel must be at least this fast to take a game piece; slower and it would jam. */
  static final double kMinFeedRPM = 2000.0;

  /** Path travelled per second for each roller RPM: about 1 s from floor to beam break at 1000 RPM. */
  static final double kPathPerSecondPerRollerRPM = 0.5 / 1000.0;

  /**
   * Flywheel radius (a 4 inch wheel) and how much of its surface speed the game piece keeps (it
   * slips and squishes). Low enough that the arc lands about 2.5 m behind the robot, in view.
   */
  static final double kFlywheelRadiusMeters = 0.0508;
  static final double kLaunchEfficiency = 0.2;

  /** How far a game piece moves the moment it leaves the beam break, so it can't bounce back. */
  static final double kLeaveBeamDistance = 0.01;

  /**
   * How quickly a coasting flywheel slows: it loses about 63% of its speed every this many seconds.
   * From full speed it stays above the feed speed for about 2 s.
   */
  static final double kFlywheelCoastSeconds = 2.5;

  /** Below this a coasting flywheel counts as stopped, in rad/s (about 10 RPM). */
  static final double kStoppedRadPerSec = 1.0;

  /** Throttles smaller than this count as "not running". */
  static final double kRunningThrottle = 0.05;

  /** Launched and ejected game pieces disappear after this long. */
  static final double kGoneAfterSeconds = 2.0;

  /**
   * For the battery only: roughly what a motor controller's supply current limit lets each motor
   * draw, so a spin-up sags the battery a little instead of browning it out.
   */
  static final double kSupplyCurrentLimitAmps = 60.0;

  // Physics: each spinning part is a FlywheelSim (a roller is just a small flywheel).
  private final FlywheelSim roller =
      flywheel(DCMotor.getKrakenX60(1), 0.0005, 3.0); // J in kg*m^2, gearing
  private final FlywheelSim flywheel = flywheel(DCMotor.getKrakenX60(1), 0.004, 1.0);

  // How the sim reads and writes the robot without touching the subsystems.
  private final SimDouble intakeThrottleSim;
  private final SimDouble shooterThrottleSim;
  private final DIOSim beamBreak = new DIOSim(Constants.kBeamBreakChannel);

  // Buttons on the web page.
  private final BooleanEntry placePieceInput;
  private final BooleanEntry removePieceInput;

  private double batteryVolts = 12.0;
  private double rollerAngleDeg;
  private double flywheelAngleDeg;

  private PieceState pieceState = PieceState.NONE;
  private double piecePosition;
  private double launchSpeed;
  private double secondsSinceGone;

  /** The DIO 0 value this sim last saw or wrote. A different value means someone else changed it. */
  private boolean lastBeamValue;

  SandboxSim(Robot robot) {
    intakeThrottleSim = throttleOf(Constants.kIntakeMotorChannel);
    shooterThrottleSim = throttleOf(Constants.kShooterMotorChannel);
    lastBeamValue = beamBreak.getValue();

    var nt = NetworkTableInstance.getDefault();
    placePieceInput = nt.getBooleanTopic("/Visualizer/Input/placePiece").getEntry(false);
    removePieceInput = nt.getBooleanTopic("/Visualizer/Input/removePiece").getEntry(false);
    placePieceInput.set(false);
    removePieceInput.set(false);
  }

  private static FlywheelSim flywheel(DCMotor motor, double j, double gearing) {
    return new FlywheelSim(Models.flywheelFromPhysicalConstants(motor, j, gearing), motor);
  }

  private static SimDouble throttleOf(int channel) {
    return new SimDeviceSim("PWMMotorController[" + channel + "]").getDouble("Throttle");
  }

  /** Runs one step of the simulation. */
  void update(double dt) {
    handlePageButtons();
    followOutsideBeamChanges();
    stepPhysics(dt);
    stepGamePiece(dt);
  }

  private void handlePageButtons() {
    if (removePieceInput.get()) {
      removePieceInput.set(false);
      pieceState = PieceState.NONE;
      piecePosition = 0.0;
      setBeamBlocked(false);
    }
    if (placePieceInput.get()) {
      placePieceInput.set(false);
      // One game piece at a time: a new one replaces any piece already in the robot.
      if (pieceState == PieceState.HELD) {
        setBeamBlocked(false);
      }
      pieceState = PieceState.ON_FLOOR;
      piecePosition = 0.0;
    }
  }

  /**
   * If DIO 0 changed from outside (for example a student flipping it in the Sim GUI), the game
   * piece follows: blocked means a piece is at the beam break, cleared means it is gone.
   */
  private void followOutsideBeamChanges() {
    boolean value = beamBreak.getValue();
    if (value == lastBeamValue) {
      return;
    }
    lastBeamValue = value;
    boolean blocked = !value; // low means blocked, like Intake.hasGamePiece()
    if (blocked && pieceState != PieceState.HELD) {
      pieceState = PieceState.HELD;
      piecePosition = kBeamBreakPosition;
    } else if (!blocked && pieceState == PieceState.HELD) {
      pieceState = PieceState.NONE;
      piecePosition = 0.0;
    }
  }

  private void stepPhysics(double dt) {
    // A disabled robot's motor controllers output nothing.
    boolean enabled = RobotBase.isEnabled();
    double shooterThrottle = enabled ? shooterThrottle() : 0.0;
    roller.setInputVoltage(enabled ? intakeThrottle() * batteryVolts : 0.0);
    roller.update(dt);
    if (Math.abs(shooterThrottle) > kRunningThrottle) {
      flywheel.setInputVoltage(shooterThrottle * batteryVolts);
      flywheel.update(dt);
    } else {
      // Motor in coast (no output): only friction slows the heavy flywheel, so it keeps spinning
      // for a few seconds. FlywheelSim at 0 V would brake it to a stop in well under a second.
      double coasting = flywheel.getAngularVelocity() * Math.exp(-dt / kFlywheelCoastSeconds);
      flywheel.setInputVoltage(0.0);
      flywheel.setAngularVelocity(Math.abs(coasting) < kStoppedRadPerSec ? 0.0 : coasting);
    }

    rollerAngleDeg += Math.toDegrees(roller.getAngularVelocity()) * dt;
    flywheelAngleDeg += Math.toDegrees(flywheel.getAngularVelocity()) * dt;

    batteryVolts =
        BatterySim.calculateDefaultBatteryLoadedVoltage(
            Math.min(Math.abs(roller.getCurrentDraw()), kSupplyCurrentLimitAmps),
            Math.min(Math.abs(flywheelCurrent()), kSupplyCurrentLimitAmps));
    RoboRioSim.setVInVoltage(batteryVolts);
  }

  private void stepGamePiece(double dt) {
    // The game piece moves with the rollers; the intake "pushing" means the motor is commanded.
    double travel = getRollerRPM() * kPathPerSecondPerRollerRPM * dt; // + is inward, - is outward
    double push = RobotBase.isEnabled() ? intakeThrottle() : 0.0;
    boolean pushingIn = push > kRunningThrottle;
    boolean pushingOut = push < -kRunningThrottle;
    // While the roller is still turning the old way after a reversal, the piece doesn't move back.
    if (pushingIn) {
      travel = Math.max(travel, 0.0);
    } else if (pushingOut) {
      travel = Math.min(travel, 0.0);
    }

    switch (pieceState) {
      case ON_FLOOR -> {
        if (pushingIn) {
          pieceState = PieceState.IN_INTAKE;
        }
      }
      case IN_INTAKE -> {
        piecePosition += travel;
        if (piecePosition >= kBeamBreakPosition) {
          piecePosition = kBeamBreakPosition;
          pieceState = PieceState.HELD;
          setBeamBlocked(true);
        } else if (piecePosition <= 0.0) {
          piecePosition = 0.0;
          pieceState = PieceState.EJECTED;
          secondsSinceGone = 0.0;
        }
      }
      case HELD -> {
        if (pushingIn && getFlywheelRPM() >= kMinFeedRPM) {
          pieceState = PieceState.FEEDING;
          piecePosition = kBeamBreakPosition + kLeaveBeamDistance + travel;
          setBeamBlocked(false);
        } else if (pushingOut) {
          // Spitting out: back toward the floor.
          pieceState = PieceState.IN_INTAKE;
          piecePosition = kBeamBreakPosition - kLeaveBeamDistance + travel;
          setBeamBlocked(false);
        }
        // Pushing in with a slow flywheel: the piece stays here (it would jam).
      }
      case FEEDING -> {
        piecePosition += Math.max(travel, 0.0); // the flywheel has it now: it can't back out
        if (piecePosition >= kShooterPosition) {
          piecePosition = kShooterPosition;
          pieceState = PieceState.LAUNCHED;
          launchSpeed = flywheel.getAngularVelocity() * kFlywheelRadiusMeters * kLaunchEfficiency;
          secondsSinceGone = 0.0;
        }
      }
      case LAUNCHED, EJECTED -> {
        secondsSinceGone += dt;
        if (secondsSinceGone >= kGoneAfterSeconds) {
          pieceState = PieceState.NONE;
          piecePosition = 0.0;
        }
      }
      case NONE -> {}
    }
  }

  /** A coasting motor draws no current. */
  private double flywheelCurrent() {
    return flywheel.getInputVoltage() == 0.0 ? 0.0 : flywheel.getCurrentDraw();
  }

  /** Writes DIO 0. Only called when a game piece arrives at or leaves the beam break. */
  private void setBeamBlocked(boolean blocked) {
    boolean value = !blocked;
    beamBreak.setValue(value);
    lastBeamValue = value;
  }

  /** The throttle the robot code set on the intake motor, -1 to 1. */
  public double intakeThrottle() {
    return intakeThrottleSim == null ? 0.0 : intakeThrottleSim.get();
  }

  /** The throttle the robot code set on the shooter motor, -1 to 1. */
  public double shooterThrottle() {
    return shooterThrottleSim == null ? 0.0 : shooterThrottleSim.get();
  }

  public double getRollerRPM() {
    return radPerSecToRPM(roller.getAngularVelocity());
  }

  public double getRollerAngleDeg() {
    return rollerAngleDeg;
  }

  public double getFlywheelRPM() {
    return radPerSecToRPM(flywheel.getAngularVelocity());
  }

  public double getFlywheelAngleDeg() {
    return flywheelAngleDeg;
  }

  public double getBatteryVolts() {
    return batteryVolts;
  }

  public PieceState getPieceState() {
    return pieceState;
  }

  public double getPiecePosition() {
    return piecePosition;
  }

  /** Exit speed of the last launched game piece, in m/s. */
  public double getLaunchSpeed() {
    return launchSpeed;
  }

  private static double radPerSecToRPM(double radPerSec) {
    return radPerSec * 60.0 / (2.0 * Math.PI);
  }
}
