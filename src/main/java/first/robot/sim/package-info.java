/**
 * Simulation and 3D visualizer support. You don't need to read this to use the Sandbox. The robot
 * code in {@code subsystems/} and {@code opmode/} works the same with or without it.
 *
 * <p>How it fits together: {@link first.robot.sim.Visualizer#start} is called once from {@code
 * Robot}'s constructor. In simulation, {@link first.robot.sim.SandboxSim} reads the motor outputs
 * the robot code sets, runs the physics (two flywheel models and a game piece) and sets the beam
 * break. {@link first.robot.sim.VisualizerPublisher} sends everything to NetworkTables under {@code
 * /Visualizer/}, and {@code visualizer/index.html} draws it. The robot code does the physics; the
 * page only draws.
 *
 * <p>Nothing in {@code subsystems/} or {@code opmode/} depends on this package. The sim reads the
 * robot from the outside: motor throttles through {@code SimDeviceSim}, the beam break through
 * {@code DIOSim}, and running commands through the scheduler.
 *
 * <p>Moving to Phoenix 6 later: the subsystems will use {@code TalonFX} normally and still contain
 * no sim code. {@code SandboxSim} will get each motor's {@code TalonFXSimState} through a small
 * package-private hook (for example a {@code static TalonFX motorOf(Intake)} helper in this package
 * that reads the field by reflection, the same way {@link first.robot.sim.VisualizerPublisher}
 * reads PWM motors on a real robot). Only the lines in {@code SandboxSim} that read motor output
 * and feed back rotor position, velocity and supply voltage change; the topic contract and the
 * page stay the same.
 */
package first.robot.sim;
