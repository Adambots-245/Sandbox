package first.robot;

import org.wpilib.command3.Trigger;
import org.wpilib.command3.button.CommandGamepad;

/**
 * Every button the drivers use, in one place. OpModes bind commands to these,
 * for example {@code Buttons.XboxAButton.onTrue(...)}.
 * Everything here is static, so nobody makes a Buttons object.
 */
public final class Buttons {
  private Buttons() {}

  /** The driver's gamepad. */
  public static final CommandGamepad driver = new CommandGamepad(Constants.kDriverControllerPort);

  // Face buttons, named for an Xbox controller
  public static final Trigger XboxAButton = driver.faceDown();
  public static final Trigger XboxBButton = driver.faceRight();
  public static final Trigger XboxXButton = driver.faceLeft();
  public static final Trigger XboxYButton = driver.faceUp();

  // Bumpers
  public static final Trigger XboxLeftBumper = driver.leftBumper();
  public static final Trigger XboxRightBumper = driver.rightBumper();
}
