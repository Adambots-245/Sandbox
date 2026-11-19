package first.robot;

import org.wpilib.command3.Scheduler;
import org.wpilib.framework.OpModeRobot;

import first.robot.sim.Visualizer;
import first.robot.subsystems.Intake;
import first.robot.subsystems.Shooter;

/** The robot: builds every subsystem once, and runs them and the scheduler every loop. The gamepad lives in Buttons. */
public class Robot extends OpModeRobot {
  public final Intake intake = new Intake();
  public final Shooter shooter = new Shooter();

  public Robot() {
    // When no other command is using them, the intake and shooter stay stopped.
    intake.setDefaultCommand(intake.stop());
    shooter.setDefaultCommand(shooter.stop());
    // Shows this robot in the 3D visualizer (see visualizer/README.md). Safe to ignore.
    Visualizer.start(this);
  }

  @Override
  public void robotPeriodic() {
    // Runs every 20 ms, in every mode.
    // First each subsystem's periodic(), then the scheduler runs the commands.
    intake.periodic();
    shooter.periodic();
    Scheduler.getDefault().run();
  }
}
