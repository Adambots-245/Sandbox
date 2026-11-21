package first.robot.sim;

import java.lang.reflect.Field;
import java.util.List;

import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.command3.Scheduler;
import org.wpilib.framework.RobotBase;
import org.wpilib.hardware.motor.PWMMotorController;
import org.wpilib.networktables.BooleanPublisher;
import org.wpilib.networktables.DoublePublisher;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.StringPublisher;
import org.wpilib.system.RobotController;

import first.robot.Robot;

/**
 * Sends what the robot is doing to NetworkTables under {@code /Visualizer/}, every 20 ms. The page
 * in {@code visualizer/} reads these values and draws them. Runs in simulation and on a real robot.
 *
 * <p>Publishing a value takes two lines: make a publisher once, then {@code set} it every loop.
 */
public final class VisualizerPublisher {
  /** Version of the topic contract. Change it if a topic is renamed or changes meaning. */
  static final String kContractVersion = "1";

  private final Robot robot;
  private final SandboxSim sim; // null on a real robot

  // On a real robot there is no SimDevice, so the throttle comes from the motor object itself.
  private final PWMMotorController intakeMotor;
  private final PWMMotorController shooterMotor;

  private final BooleanPublisher enabled;
  private final StringPublisher mode;
  private final StringPublisher opMode;
  private final DoublePublisher batteryVolts;

  private final DoublePublisher intakeThrottle;
  private final DoublePublisher rollerRPM;
  private final DoublePublisher rollerAngleDeg;
  private final BooleanPublisher beamBlocked;
  private final StringPublisher intakeCommand;

  private final DoublePublisher shooterThrottle;
  private final DoublePublisher flywheelRPM;
  private final DoublePublisher wheelAngleDeg;
  private final StringPublisher shooterCommand;
  private final DoublePublisher minFeedRPM;

  private final StringPublisher pieceState;
  private final DoublePublisher piecePosition;
  private final DoublePublisher launchSpeed;
  private final StringPublisher version;

  VisualizerPublisher(Robot robot, SandboxSim sim) {
    this.robot = robot;
    this.sim = sim;
    intakeMotor = sim == null ? findMotor(robot.intake) : null;
    shooterMotor = sim == null ? findMotor(robot.shooter) : null;

    var nt = NetworkTableInstance.getDefault();
    enabled = nt.getBooleanTopic("/Visualizer/robot/enabled").publish();
    mode = nt.getStringTopic("/Visualizer/robot/mode").publish();
    opMode = nt.getStringTopic("/Visualizer/robot/opMode").publish();
    batteryVolts = nt.getDoubleTopic("/Visualizer/robot/batteryVolts").publish();

    intakeThrottle = nt.getDoubleTopic("/Visualizer/intake/throttle").publish();
    rollerRPM = nt.getDoubleTopic("/Visualizer/intake/rollerRPM").publish();
    rollerAngleDeg = nt.getDoubleTopic("/Visualizer/intake/rollerAngleDeg").publish();
    beamBlocked = nt.getBooleanTopic("/Visualizer/intake/beamBlocked").publish();
    intakeCommand = nt.getStringTopic("/Visualizer/intake/command").publish();

    shooterThrottle = nt.getDoubleTopic("/Visualizer/shooter/throttle").publish();
    flywheelRPM = nt.getDoubleTopic("/Visualizer/shooter/flywheelRPM").publish();
    wheelAngleDeg = nt.getDoubleTopic("/Visualizer/shooter/wheelAngleDeg").publish();
    shooterCommand = nt.getStringTopic("/Visualizer/shooter/command").publish();
    minFeedRPM = nt.getDoubleTopic("/Visualizer/shooter/minFeedRPM").publish();

    pieceState = nt.getStringTopic("/Visualizer/piece/state").publish();
    piecePosition = nt.getDoubleTopic("/Visualizer/piece/position").publish();
    launchSpeed = nt.getDoubleTopic("/Visualizer/piece/launchSpeed").publish();

    version = nt.getStringTopic("/Visualizer/meta/version").publish();
  }

  void periodic() {
    enabled.set(RobotBase.isEnabled());
    mode.set(modeName());
    String selected = RobotBase.getOpMode();
    opMode.set(selected == null ? "" : selected);

    // Intake
    intakeThrottle.set(sim != null ? sim.intakeThrottle() : throttleOf(intakeMotor));
    rollerRPM.set(sim != null ? sim.getRollerRPM() : 0.0); // real robot: 0 until it has an encoder
    rollerAngleDeg.set(sim != null ? sim.getRollerAngleDeg() : 0.0);
    beamBlocked.set(robot.intake.hasGamePiece());
    intakeCommand.set(commandUsing(robot.intake));

    // Shooter
    shooterThrottle.set(sim != null ? sim.shooterThrottle() : throttleOf(shooterMotor));
    flywheelRPM.set(sim != null ? sim.getFlywheelRPM() : 0.0);
    wheelAngleDeg.set(sim != null ? sim.getFlywheelAngleDeg() : 0.0);
    shooterCommand.set(commandUsing(robot.shooter));
    minFeedRPM.set(SandboxSim.kMinFeedRPM);

    // Battery and game piece
    batteryVolts.set(sim != null ? sim.getBatteryVolts() : RobotController.getBatteryVoltage());
    pieceState.set(sim != null ? sim.getPieceState().name() : SandboxSim.PieceState.NONE.name());
    piecePosition.set(sim != null ? sim.getPiecePosition() : 0.0);
    launchSpeed.set(sim != null ? sim.getLaunchSpeed() : 0.0);
    version.set(kContractVersion);
  }

  private static String modeName() {
    if (RobotBase.isDisabled()) {
      return "Disabled";
    } else if (RobotBase.isAutonomous()) {
      return "Autonomous";
    } else if (RobotBase.isUtility()) {
      return "Utility";
    }
    return "Teleop";
  }

  /** Name of the command using a subsystem, or "" when none is. For a group, the outermost name. */
  private static String commandUsing(Mechanism subsystem) {
    List<Command> running = Scheduler.getDefault().getRunningCommandsFor(subsystem);
    return running.isEmpty() ? "" : running.get(0).name();
  }

  private static double throttleOf(PWMMotorController motor) {
    return motor == null ? 0.0 : motor.getThrottle();
  }

  /**
   * Finds the motor inside a subsystem by looking at its fields, so the subsystem doesn't need an
   * extra getter just for the visualizer. Returns null if there isn't one.
   */
  private static PWMMotorController findMotor(Object subsystem) {
    for (Field field : subsystem.getClass().getDeclaredFields()) {
      if (PWMMotorController.class.isAssignableFrom(field.getType())) {
        try {
          field.setAccessible(true);
          return (PWMMotorController) field.get(subsystem);
        } catch (ReflectiveOperationException | RuntimeException e) {
          return null;
        }
      }
    }
    return null;
  }
}
