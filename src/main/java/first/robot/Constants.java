package first.robot;

/**
 * Every fixed number for the robot lives here.
 * Names start with k. Values are static final, so they never change while the robot runs.
 */
public final class Constants {
  // Driver Station: the same port number the wizard's template used
  public static final int kDriverControllerPort = 1;

  // Intake
  public static final int kIntakeMotorChannel = 0;
  public static final int kBeamBreakChannel = 0;
  public static final double kIntakeSpeed = 0.5;

  // Shooter
  public static final int kShooterMotorChannel = 1;
  public static final double kShooterSpeed = 0.75;
}
