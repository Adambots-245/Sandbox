package first.robot.subsystems;

import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.drivers.motor.PWMSparkMax;
import org.wpilib.hardware.discrete.DigitalInput;

import first.robot.Constants;

/** The intake: one motor that pulls game pieces in, and a beam break that sees them. */
public class Intake implements Mechanism {
  private final PWMSparkMax motor;
  private final DigitalInput beamBreak;

  public Intake() {
    motor = new PWMSparkMax(Constants.kIntakeMotorChannel);
    beamBreak = new DigitalInput(Constants.kBeamBreakChannel);
    System.out.println("Intake ready on channel " + Constants.kIntakeMotorChannel);
  }

  /** True when the beam break sees a game piece (the beam is blocked). */
  public boolean hasGamePiece() {
    return !beamBreak.get();
  }

  /** A command that spins the intake inward for as long as it runs. It prints once when it starts. */
  public Command intake() {
    return run(coroutine -> {
          System.out.println("Intake: running at " + Constants.kIntakeSpeed);
          while (true) {
            motor.setThrottle(Constants.kIntakeSpeed);
            coroutine.yield();
          }
        })
        .named("Intake");
  }

  /** A command that keeps the intake motor stopped. */
  public Command stop() {
    return run(coroutine -> {
          System.out.println("Intake: stopped");
          while (true) {
            motor.setThrottle(0.0);
            coroutine.yield();
          }
        })
        .named("Intake Stop");
  }
}
