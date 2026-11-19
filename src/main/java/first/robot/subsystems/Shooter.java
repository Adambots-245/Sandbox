package first.robot.subsystems;

import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.drivers.motor.PWMTalonFX;

import first.robot.Constants;

/** The shooter: one flywheel motor. */
public class Shooter implements Mechanism {
  private final PWMTalonFX motor;

  public Shooter() {
    motor = new PWMTalonFX(Constants.kShooterMotorChannel);
  }

  /** Runs every loop, whatever command is running. Good for reading sensors and sending telemetry. */
  public void periodic() {
    // Nothing yet.
  }

  /** A command that spins the flywheel at shooting speed for as long as it runs. It prints once when it starts. */
  public Command spinUp() {
    return run(coroutine -> {
          System.out.println("Shooter: spinning at " + Constants.kShooterSpeed);
          while (true) {
            motor.setThrottle(Constants.kShooterSpeed);
            coroutine.yield();
          }
        })
        .named("Shooter Spin Up");
  }

  /** A command that keeps the flywheel stopped. */
  public Command stop() {
    return run(coroutine -> {
          System.out.println("Shooter: stopped");
          while (true) {
            motor.setThrottle(0.0);
            coroutine.yield();
          }
        })
        .named("Shooter Stop");
  }
}
