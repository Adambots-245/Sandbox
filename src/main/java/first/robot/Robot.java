package first.robot;

import org.wpilib.command3.Scheduler;
import org.wpilib.command3.button.CommandGamepad;
import org.wpilib.framework.OpModeRobot;

import first.robot.subsystems.Intake;
import first.robot.subsystems.Shooter;

/** The robot: builds every mechanism once, and runs the scheduler every loop. */
public class Robot extends OpModeRobot {
  public final Intake intake = new Intake();
  public final Shooter shooter = new Shooter();
  public final CommandGamepad driver = new CommandGamepad(Constants.kDriverControllerPort);

  public Robot() {
    // When no other command is using them, the intake and shooter stay stopped.
    intake.setDefaultCommand(intake.stop());
    shooter.setDefaultCommand(shooter.stop());
  }

  @Override
  public void robotPeriodic() {
    // Runs every 20 ms, in every mode.
    Scheduler.getDefault().run();
  }
}
