package first.robot.sim;

import java.io.File;

import org.wpilib.framework.RobotBase;
import org.wpilib.net.WebServer;

import first.robot.Robot;

/** Starts the 3D visualizer support. {@code Robot} calls {@link #start} once; nothing else does. */
public final class Visualizer {
  /** How often the sim and the publisher run, in seconds (the same 20 ms as the robot loop). */
  static final double kPeriodSeconds = 0.020;

  /** In simulation the page is served at http://localhost:5811/ (see visualizer/README.md). */
  public static final int kWebPort = 5811;

  private Visualizer() {}

  /**
   * Creates the simulation (only when simulating) and the NetworkTables publisher, and runs them
   * every 20 ms alongside the robot's own loop.
   *
   * @param robot the robot to show
   */
  public static void start(Robot robot) {
    SandboxSim sim = null;
    if (RobotBase.isSimulation()) {
      sim = new SandboxSim(robot);
      servePage();
    }
    VisualizerPublisher publisher = new VisualizerPublisher(robot, sim);

    SandboxSim simOrNull = sim;
    robot.addPeriodic(
        () -> {
          if (simOrNull != null) {
            simOrNull.update(kPeriodSeconds);
          }
          publisher.periodic();
        },
        kPeriodSeconds);
  }

  /**
   * Browsers won't load the page's JavaScript files from a double-clicked file, so the simulation
   * serves the {@code visualizer} folder over http. Looks for it in the folder the simulation runs
   * in and the folders above it.
   */
  private static void servePage() {
    File dir = new File("").getAbsoluteFile();
    while (dir != null && !new File(dir, "visualizer/index.html").isFile()) {
      dir = dir.getParentFile();
    }
    if (dir == null) {
      System.out.println("Visualizer: visualizer/index.html not found, so the page isn't served");
      return;
    }
    try {
      WebServer.start(kWebPort, new File(dir, "visualizer").getPath());
      System.out.println("Visualizer: open http://localhost:" + kWebPort + "/ to see the robot");
    } catch (RuntimeException e) {
      System.out.println("Visualizer: couldn't serve the page on port " + kWebPort + ": " + e);
    }
  }
}
