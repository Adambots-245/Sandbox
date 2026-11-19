package first.robot.opmode;

import org.wpilib.opmode.OpMode;
import org.wpilib.opmode.Teleop;

import first.robot.Buttons;
import first.robot.Robot;

/** Teleop: the drivers are in control. Button bindings for this OpMode go here. */
@Teleop(name = "Driver Control")
public class DriverControl implements OpMode {
  public DriverControl(Robot robot) {
    // Hold the right bumper to run the intake.
    Buttons.XboxRightBumper.whileTrue(robot.intake.intake());

    // Hold the left bumper to spin up the shooter.
    Buttons.XboxLeftBumper.whileTrue(robot.shooter.spinUp());
  }
}
