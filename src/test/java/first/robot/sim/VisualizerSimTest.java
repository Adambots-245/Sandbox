package first.robot.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;
import org.wpilib.hardware.hal.HAL;
import org.wpilib.hardware.hal.OpModeOption;
import org.wpilib.hardware.hal.RobotMode;
import org.wpilib.networktables.BooleanEntry;
import org.wpilib.networktables.DoubleSubscriber;
import org.wpilib.networktables.MultiSubscriber;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.NetworkTableValue;
import org.wpilib.networktables.StringSubscriber;
import org.wpilib.networktables.TopicInfo;
import org.wpilib.simulation.DIOSim;
import org.wpilib.simulation.DriverStationSim;
import org.wpilib.simulation.GamepadSim;
import org.wpilib.simulation.SimDeviceSim;
import org.wpilib.simulation.SimHooks;

import first.robot.Constants;
import first.robot.Robot;

/**
 * Runs the real robot code in simulation, with the visualizer support started by {@code Robot}, and
 * checks that existing behavior is unchanged and that the sim and topics behave as the spec says.
 * One robot is shared by every test (a robot can only be created once per program).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VisualizerSimTest {
  private static final double kLoop = 0.020;

  private Robot robot;
  private Thread robotThread;
  private GamepadSim gamepad;
  private DIOSim beamBreak;
  private SimDeviceSim intakeMotor;
  private SimDeviceSim shooterMotor;
  private BooleanEntry placePiece;
  private BooleanEntry removePiece;
  private NetworkTableInstance nt;

  @BeforeAll
  void startRobot() throws InterruptedException {
    assertTrue(HAL.initialize());
    SimHooks.pauseTiming();
    DriverStationSim.resetData();

    robot = new Robot();
    robotThread = new Thread(robot::startCompetition, "robot");
    robotThread.setDaemon(true);
    robotThread.start();
    SimHooks.waitForProgramStart();

    gamepad = new GamepadSim(Constants.kDriverControllerPort);
    beamBreak = new DIOSim(Constants.kBeamBreakChannel);
    intakeMotor = new SimDeviceSim("PWMMotorController[" + Constants.kIntakeMotorChannel + "]");
    shooterMotor = new SimDeviceSim("PWMMotorController[" + Constants.kShooterMotorChannel + "]");
    nt = NetworkTableInstance.getDefault();
    placePiece = nt.getBooleanTopic("/Visualizer/Input/placePiece").getEntry(false);
    removePiece = nt.getBooleanTopic("/Visualizer/Input/removePiece").getEntry(false);

    // Teleoperated, Driver Control, enabled: what a student clicks in the Sim GUI.
    long driverControl = 0;
    for (OpModeOption option : DriverStationSim.getOpModeOptions()) {
      if (option.name.equals("Driver Control")) {
        driverControl = option.id;
      }
    }
    assertTrue(driverControl != 0, "Driver Control OpMode should be registered");
    DriverStationSim.setDsAttached(true);
    DriverStationSim.setRobotMode(RobotMode.TELEOPERATED);
    DriverStationSim.setOpMode(driverControl);
    DriverStationSim.setEnabled(true);
    DriverStationSim.notifyNewData();
    step(0.2);
  }

  @AfterAll
  void stopRobot() throws InterruptedException {
    robot.endCompetition();
    SimHooks.resumeTiming();
    robotThread.join(2000);
  }

  @BeforeEach
  void reset() {
    gamepad.setRightBumperButton(false);
    gamepad.setLeftBumperButton(false);
    gamepad.notifyNewData();
    removePiece.set(true);
    // Let the rollers stop and the flywheel coast to a stop.
    for (int i = 0; i < 1000 && topicDouble("shooter/flywheelRPM") > 0.0; i++) {
      step(kLoop);
    }
    step(0.5);
    assertEquals(0.0, topicDouble("shooter/flywheelRPM"), "flywheel should coast to a stop");
    assertTrue(beamBreak.getValue(), "beam should be clear after removing the piece");
  }

  /** Lets the robot run for some simulated time, 20 ms at a time. */
  private void step(double seconds) {
    for (double t = 0; t < seconds - 1e-9; t += kLoop) {
      SimHooks.stepTiming(kLoop);
    }
  }

  // One subscriber per topic, made once: making a new one on every read runs out of handles.
  private final Map<String, DoubleSubscriber> doubles = new HashMap<>();
  private final Map<String, StringSubscriber> strings = new HashMap<>();

  private double topicDouble(String name) {
    return doubles
        .computeIfAbsent(name, n -> nt.getDoubleTopic("/Visualizer/" + n).subscribe(Double.NaN))
        .get();
  }

  private String topicString(String name) {
    return strings
        .computeIfAbsent(name, n -> nt.getStringTopic("/Visualizer/" + n).subscribe("?"))
        .get();
  }

  private void holdRightBumper(boolean held) {
    gamepad.setRightBumperButton(held);
    gamepad.notifyNewData();
  }

  private void holdLeftBumper(boolean held) {
    gamepad.setLeftBumperButton(held);
    gamepad.notifyNewData();
  }

  // 2. Existing behavior unchanged

  @Test
  void bumpersSetTheSameThrottlesAsBefore() {
    holdRightBumper(true);
    step(0.1);
    assertEquals(Constants.kIntakeSpeed, intakeMotor.getDouble("Throttle").get(), 1e-9);
    holdRightBumper(false);
    holdLeftBumper(true);
    step(0.1);
    assertEquals(Constants.kShooterSpeed, shooterMotor.getDouble("Throttle").get(), 1e-9);
    assertEquals(0.0, intakeMotor.getDouble("Throttle").get(), 1e-9);
    holdLeftBumper(false);
    step(0.1);
    assertEquals(0.0, shooterMotor.getDouble("Throttle").get(), 1e-9);
  }

  @Test
  void dioSetFromOutsideIsNeverOverwritten() {
    // Like a student flipping Hardware -> DIO -> In[0] in the Sim GUI.
    beamBreak.setValue(false);
    step(0.02);
    assertTrue(robot.intake.hasGamePiece());
    assertEquals("HELD", topicString("piece/state"), "the piece appears when the beam is blocked");
    step(2.0);
    assertFalse(beamBreak.getValue(), "SandboxSim must not write DIO 0 every loop");
    assertTrue(robot.intake.hasGamePiece());

    beamBreak.setValue(true);
    step(0.02);
    assertFalse(robot.intake.hasGamePiece());
    assertEquals("NONE", topicString("piece/state"), "the piece goes away when the beam clears");
    step(2.0);
    assertTrue(beamBreak.getValue());
  }

  // 3. Physics and world model

  @Test
  void flywheelSpinsUpOverAboutASecondAndCoastsDown() {
    holdLeftBumper(true);
    step(0.2);
    double rpmAt02 = topicDouble("shooter/flywheelRPM");
    step(0.3);
    double rpmAt05 = topicDouble("shooter/flywheelRPM");
    step(0.5);
    double rpmAt10 = topicDouble("shooter/flywheelRPM");
    step(1.0);
    double rpmAt20 = topicDouble("shooter/flywheelRPM");

    assertTrue(rpmAt02 > 0 && rpmAt02 < rpmAt05 && rpmAt05 < rpmAt10, "RPM should keep rising");
    assertTrue(rpmAt02 < 0.6 * rpmAt20, "should not be at speed almost at once: " + rpmAt02);
    assertTrue(rpmAt10 > 0.85 * rpmAt20, "should be nearly at speed after a second: " + rpmAt10);
    assertTrue(rpmAt20 > 3000, "0.75 throttle should be well above the 2000 RPM feed speed");

    // Released: the motor coasts, so the flywheel slows down over a few seconds, not at once.
    holdLeftBumper(false);
    step(0.5);
    double rpmAfterStop = topicDouble("shooter/flywheelRPM");
    assertTrue(rpmAfterStop < rpmAt20, "RPM should drop once stopped");
    assertTrue(rpmAfterStop > 0.7 * rpmAt20, "should coast, not brake: " + rpmAfterStop);
    step(1.0);
    assertTrue(topicDouble("shooter/flywheelRPM") > SandboxSim.kMinFeedRPM,
        "still fast enough to feed 1.5 s after letting go");
    step(10.0);
    assertTrue(topicDouble("shooter/flywheelRPM") < 0.05 * rpmAt20);
  }

  @Test
  void placedPieceRidesInAndBlocksTheBeam() {
    placePiece.set(true);
    step(0.1);
    assertEquals("ON_FLOOR", topicString("piece/state"));
    assertFalse(placePiece.get(), "SandboxSim resets the input after handling it");

    holdRightBumper(true);
    double lastAngle = topicDouble("intake/rollerAngleDeg");
    for (int i = 0; i < 150 && !robot.intake.hasGamePiece(); i++) {
      step(kLoop);
      if (i > 5) {
        assertTrue(topicDouble("intake/rollerAngleDeg") > lastAngle, "rollers keep turning");
      }
      lastAngle = topicDouble("intake/rollerAngleDeg");
    }
    assertTrue(robot.intake.hasGamePiece(), "the piece should reach the beam break within 3 s");
    assertFalse(beamBreak.getValue(), "DIO 0 goes low when the piece arrives");
    assertEquals("HELD", topicString("piece/state"));
    assertEquals(0.5, topicDouble("piece/position"), 1e-9);

    // Flywheel stopped: the piece stays held even though the intake keeps pushing.
    step(1.0);
    assertEquals("HELD", topicString("piece/state"));
    assertTrue(robot.intake.hasGamePiece());
  }

  @Test
  void heldPieceLaunchesOnlyWithAFastFlywheel() {
    beamBreak.setValue(false); // a piece at the beam break
    step(0.1);
    assertEquals("HELD", topicString("piece/state"));

    holdLeftBumper(true);
    step(1.5);
    assertTrue(topicDouble("shooter/flywheelRPM") > 2000);
    holdRightBumper(true);
    String state = "";
    for (int i = 0; i < 150 && !state.equals("LAUNCHED"); i++) {
      step(kLoop);
      state = topicString("piece/state");
    }
    assertEquals("LAUNCHED", state);
    assertTrue(beamBreak.getValue(), "DIO 0 goes high once the piece leaves the beam break");
    assertFalse(robot.intake.hasGamePiece());
    assertTrue(topicDouble("piece/launchSpeed") > 3.0, "the piece should fly out");

    step(2.5);
    assertEquals("NONE", topicString("piece/state"), "launched pieces disappear after 2 s");
  }

  @Test
  void pieceStillLaunchesRightAfterLettingGoOfTheShooter() {
    // Spin up with Q, let go, then run the intake with P: the coasting flywheel still takes it.
    beamBreak.setValue(false);
    step(0.1);
    holdLeftBumper(true);
    step(1.5);
    holdLeftBumper(false);
    step(0.3);
    holdRightBumper(true);
    String state = "";
    for (int i = 0; i < 150 && !state.equals("LAUNCHED"); i++) {
      step(kLoop);
      state = topicString("piece/state");
    }
    assertEquals("LAUNCHED", state);
  }

  @Test
  void spinningTheIntakeOutwardEjectsTheHeldPiece() {
    beamBreak.setValue(false);
    step(0.1);
    // The Sandbox has no spit-out command yet (it's the Session 5 "Done early?" task), so a test
    // command holds the intake while the test drives its motor backward through the sim.
    Command hold =
        robot.intake.run(coroutine -> {
              while (true) {
                coroutine.yield();
              }
            })
            .named("Test Spit Out");
    Scheduler.getDefault().schedule(hold);
    String state = "HELD";
    for (int i = 0; i < 150 && !state.equals("EJECTED"); i++) {
      intakeMotor.getDouble("Throttle").set(-Constants.kIntakeSpeed);
      step(kLoop);
      state = topicString("piece/state");
      if (!state.equals("HELD")) {
        assertTrue(beamBreak.getValue(), "the beam clears as the piece backs out");
      }
    }
    assertEquals("EJECTED", state);
    Scheduler.getDefault().cancel(hold);
  }

  // 4. Topics

  @Test
  void aNetworkTablesClientSeesEveryTopicInTheContract() throws InterruptedException {
    holdRightBumper(true);
    step(0.5);

    NetworkTableInstance client = NetworkTableInstance.create();
    try {
      client.startClient("visualizer-test");
      client.setServer("localhost", NetworkTableInstance.DEFAULT_PORT);
      try (MultiSubscriber sub = new MultiSubscriber(client, new String[] {"/Visualizer/"})) {
        Map<String, Object> values = new HashMap<>();
        for (int i = 0; i < 100 && values.size() < 18; i++) {
          Thread.sleep(50);
          for (TopicInfo info : client.getTopicInfo("/Visualizer/")) {
            NetworkTableValue value = client.getTopic(info.name).getGenericEntry().get();
            if (value.isValid()) {
              values.put(info.name.substring("/Visualizer/".length()), value.getValue());
            }
          }
        }

        String[] expected = {
          "robot/enabled", "robot/mode", "robot/opMode", "robot/batteryVolts",
          "intake/throttle", "intake/rollerRPM", "intake/rollerAngleDeg", "intake/beamBlocked",
          "intake/command", "shooter/throttle", "shooter/flywheelRPM", "shooter/wheelAngleDeg",
          "shooter/command", "shooter/minFeedRPM", "piece/state", "piece/position", "piece/launchSpeed", "meta/version"
        };
        for (String name : expected) {
          assertTrue(values.containsKey(name), "missing topic " + name + " in " + values.keySet());
        }
        assertEquals(true, values.get("robot/enabled"));
        assertEquals("Teleop", values.get("robot/mode"));
        assertEquals("Driver Control", values.get("robot/opMode"));
        double volts = (Double) values.get("robot/batteryVolts");
        assertTrue(volts > 9 && volts <= 12.5, "battery volts " + volts);
        assertEquals(Constants.kIntakeSpeed, (Double) values.get("intake/throttle"), 1e-9);
        assertTrue((Double) values.get("intake/rollerRPM") > 100);
        assertEquals("Intake", values.get("intake/command"));
        assertEquals("Shooter Stop", values.get("shooter/command"));
        assertEquals(false, values.get("intake/beamBlocked"));
        assertEquals("NONE", values.get("piece/state"));
        assertEquals("1", values.get("meta/version"));
      }
    } finally {
      client.close();
    }
  }
}
