# Sandbox 3D Visualizer: build spec

**Goal:** students see a 3D robot react to their real robot code in simulation. Press A in the simulator and the intake rollers spin, a game piece rides in, the beam break goes dark and the command stops. Hold the left bumper and the flywheel spools up.
**Scope of this phase:** an immobile robot with exactly the Sandbox's mechanisms (intake rollers with a beam break, shooter flywheel). No drivetrain, no field, no driving around. Those are later phases (see the end).

---

## 1. Ground rules (read first)

1. **Keep simulation and visualizer code out of the code students read.** Students open `Intake`, `Shooter`, `Robot` and the OpModes to learn how WPILib works, so those files must not grow any simulation or NetworkTables code: no sim fields, no `isSimulation()` checks, no extra getters added for the visualizer. All of it lives in its own package, `first.robot.sim`, which students can ignore. The **only** change to an existing file is one line in `Robot`'s constructor (section 4). The public API of `Intake`, `Shooter`, `Buttons`, `DriverControl` and `Constants` stays exactly as it is, because the Session 4 and 5 decks, `TRY-IT-5.md` and the Session 5 solutions depend on it.
2. **Robot code does the physics; the web page only draws.** Never compute mechanism behavior in JavaScript. The page reads values from NetworkTables and renders them. This keeps one source of truth and means the same page can later show a real robot.
3. **Everything still works without the page.** If nobody opens the visualizer, the Sandbox builds, simulates and behaves exactly as today (Sim GUI, keyboard bindings, DIO 0 toggling for the Session 5 Try-its).
4. **Works offline.** Students' laptops are often offline at the shop. No CDN at runtime: vendor any JavaScript libraries into the repo.
5. **Team conventions:** "subsystem" in comments and UI text (the code type is still `Mechanism`); Commands v3 only; `Buttons` for bindings; no em dashes in any text.

## 2. WPILib version (Alpha 7 and Alpha 8)

- Keep the project on **GradleRIO 2027.0.0-alpha-8** as it is today. Students with the Alpha 7 VS Code install can still build it: Gradle downloads the Alpha 8 libraries on the first (online) build. This was checked in November 2026.
- The visualizer does not depend on the WPILib version: it speaks the NetworkTables 4.1 protocol over WebSocket, which is the same in Alpha 7 and Alpha 8.
- **Do not add Phoenix 6 in this phase.** CTRE has a Phoenix 6 build for WPILib 2027 Alpha 7 (26.70.0-alpha-2) but none listed for Alpha 8 yet. Keep `PWMTalonFX`. Design the sim so that switching to Phoenix 6 `TalonFX` later only changes how motor output is read and how sensor values are fed back (see section 9).
- Use only classes that exist in Alpha 8 (all checked): `org.wpilib.simulation.FlywheelSim`, `DIOSim`, `SimDeviceSim`, `RoboRioSim`, `BatterySim`, `org.wpilib.math.system.DCMotor` (`getKrakenX60(1)`), `org.wpilib.math.system.Models` (`flywheelFromPhysicalConstants(motor, J, gearing)`; in 2027 this replaces the old `LinearSystemId`), `org.wpilib.networktables.NetworkTableInstance` (`getDoubleTopic`, `getBooleanTopic`, `getStringTopic`), `org.wpilib.framework.OpModeRobot#addPeriodic(Runnable, double)`, `RobotBase.isSimulation()`, `org.wpilib.command3.Scheduler#getRunningCommandsFor(Mechanism)`, `PWMMotorController#getThrottle()`.

## 3. Architecture

```
 Robot code (real Sandbox code)                 Browser (visualizer/index.html)
 ┌───────────────────────────────┐   NT4 over    ┌──────────────────────────────┐
 │ Intake, Shooter, commands     │   WebSocket   │ tiny NT4 client (JS)          │
 │ PWMTalonFX, DigitalInput      │  ws://host:   │ subscribes to /Visualizer/    │
 │ (no sim code in these files)  │  5810/nt/viz  │ Three.js scene: robot, rollers│
 │ sim/SandboxSim (sim only):    │ ───────────►  │ flywheel, game piece, beam    │
 │  reads motor throttles        │               │ HUD: mode, commands, numbers  │
 │  steps two FlywheelSims       │ ◄───────────  │ buttons: place / remove piece │
 │  game-piece world model       │  (2 inputs)   │                               │
 │  sets DIO 0 (beam break)      │               │                               │
 │ sim/VisualizerPublisher:      │               │                               │
 │  publishes /Visualizer/*      │               │                               │
 └───────────────────────────────┘               └──────────────────────────────┘
```

The robot simulation (`Simulate Robot Code`, or `./gradlew run`) already runs a NetworkTables 4 server on `localhost:5810`. The page connects to it directly.

## 4. Robot-side code

### Where the code goes
Everything lives in the package `first.robot.sim` (folder `src/main/java/first/robot/sim/`), separate from the subsystems:
- `Visualizer.java`: the single entry point. `public static void start(Robot robot)`. It creates the two classes below and registers them with `robot.addPeriodic(..., 0.020)` (an `OpModeRobot` method that exists in Alpha 8), so `Robot` needs no `simulationPeriodic()` override and no extra fields. It creates `SandboxSim` only when `RobotBase.isSimulation()` is true; `VisualizerPublisher` runs in both simulation and on a real robot.
- `SandboxSim.java`: simulation-only physics and the game-piece world model.
- `VisualizerPublisher.java`: publishes the `/Visualizer/` topics.
- `package-info.java`: one paragraph for curious students: "Simulation and 3D visualizer support. You don't need to read this to use the Sandbox. The robot code in `subsystems/` and `opmode/` works the same with or without it."

### The one-line change to existing code
In `Robot`'s constructor, at the end:
```java
    // Shows this robot in the 3D visualizer (see visualizer/README.md). Safe to ignore.
    Visualizer.start(this);
```
Nothing else in `Robot`, and nothing at all in `Intake`, `Shooter`, `Buttons`, `DriverControl` or `Constants`.

### How the sim reads the robot without touching the subsystems
- Motor output: `new SimDeviceSim("PWMMotorController[" + channel + "]")`, value `"Throttle"`, using the channel numbers from `Constants`.
- Beam break: `DIOSim(Constants.kBeamBreakChannel)` to read and to set it; `robot.intake.hasGamePiece()` (already public) for the publisher.
- Running commands: `Scheduler.getDefault().getRunningCommandsFor(robot.intake)` and the same for `robot.shooter` (`robot.intake` and `robot.shooter` are already public fields).
- If something seems to need a new public method on a subsystem, find a simulation-side way instead, or stop and ask.

### When the Sandbox moves to Phoenix 6
Keep the same split: the subsystem uses `TalonFX` normally, and `SandboxSim` gets the motor's `getSimState()` through a tiny package-private hook or a constructor parameter, never sim code inside the subsystem. Record the chosen approach in `package-info.java`.

### SandboxSim
- **Battery:** `RoboRioSim.setVInVoltage(...)` from `BatterySim` with the total current of both motors, so heavy use sags the voltage a little (nice to show; keep it simple).
- **Intake rollers:** a `FlywheelSim` (a spinning roller is a small flywheel) from `Models.flywheelFromPhysicalConstants(DCMotor.getKrakenX60(1), J, gearing)`; start with J = 0.0005 kg·m², gearing 3.0. Input voltage = intake throttle × battery voltage. Output: roller speed (RPM, from `getAngularVelocity()` in rad/s) and an accumulated angle that SandboxSim integrates itself (degrees, for drawing the spin).
- **Shooter flywheel:** `FlywheelSim` from `Models.flywheelFromPhysicalConstants(DCMotor.getKrakenX60(1), 0.004, 1.0)` (J = 0.004 kg·m², gearing 1.0). Output: flywheel RPM and accumulated angle. Spin-up should take roughly a second at 0.75 throttle so students can see it build.
- **Game-piece world model** (one piece at a time). States: `NONE`, `ON_FLOOR` (waiting in front of the intake), `IN_INTAKE` (travelling in), `HELD` (at the beam break), `FEEDING` (moving into the shooter), `LAUNCHED` (flying out), `EJECTED` (rolled out the front).
  - `piecePosition` 0.0 to 1.0 along the path floor → intake → beam break (0.5) → shooter (1.0).
  - Intake running inward with a piece `ON_FLOOR`: the piece moves in at a speed proportional to roller RPM. Reaching 0.5 makes it `HELD` and **sets DIO 0 low (beam blocked)**.
  - Intake running inward while `HELD` **and** the flywheel is above 2000 RPM: `FEEDING`, the beam clears (**DIO 0 high**), and at 1.0 it becomes `LAUNCHED` with an exit speed from flywheel RPM. If the flywheel is slow, the piece stays `HELD` (it would jam): show that on the page.
  - Intake running outward (spit out) while `HELD`: the piece moves back toward 0, the beam clears, and it becomes `EJECTED`.
  - `LAUNCHED` and `EJECTED` pieces disappear after 2 seconds (state `NONE`).
- **DIO 0 rule (important for the Session 5 Try-its):** SandboxSim may only write DIO 0 **on state changes** (when a piece arrives at or leaves the beam break, or when the page places or removes a piece). It must never write DIO 0 every loop, so a student flipping `Hardware → DIO → In[0]` in the Sim GUI still works exactly as the Try-it describes. If DIO 0 changes from outside, the world model follows it: blocked with no piece means a piece appears `HELD`; cleared while `HELD` means the piece is removed.
- **Inputs from the page** (subscribe in SandboxSim; simulation only):
  - `/Visualizer/Input/placePiece` (boolean, set true by the page, reset to false by SandboxSim after handling): put a piece `ON_FLOOR`.
  - `/Visualizer/Input/removePiece` (same pattern): remove any piece and clear DIO 0.
- With no page connected, no pieces ever appear on their own: the Try-its behave exactly as today.

### VisualizerPublisher: the topic contract
All topics under `/Visualizer/`. Plain types only (double, boolean, string); **no structs** in this phase, so the page's decoder stays small.

| Topic | Type | Meaning |
|---|---|---|
| `robot/enabled` | boolean | robot enabled |
| `robot/mode` | string | `Disabled`, `Teleop`, `Autonomous`, `Utility` |
| `robot/opMode` | string | selected OpMode name, if available (else empty) |
| `robot/batteryVolts` | double | battery voltage |
| `intake/throttle` | double | commanded throttle, -1 to 1 |
| `intake/rollerRPM` | double | roller speed (sim: from the roller FlywheelSim; real robot: 0 until we have an encoder) |
| `intake/rollerAngleDeg` | double | accumulated roller angle, for drawing |
| `intake/beamBlocked` | boolean | `Intake.hasGamePiece()` (the real method) |
| `intake/command` | string | name of the command using the intake (`Scheduler.getDefault().getRunningCommandsFor(intake)`), or empty |
| `shooter/throttle` | double | commanded throttle |
| `shooter/flywheelRPM` | double | flywheel speed |
| `shooter/wheelAngleDeg` | double | accumulated flywheel angle |
| `shooter/command` | string | name of the command using the shooter |
| `piece/state` | string | world-model state (`NONE`, `ON_FLOOR`, ...) |
| `piece/position` | double | 0.0 to 1.0 along the path |
| `piece/launchSpeed` | double | m/s at launch, for the arc |
| `meta/version` | string | contract version, start at `1` |

Publish every loop (20 ms). Keep publishers as fields; create them once.

## 5. The web page

### Files
- `visualizer/index.html`: the page (HTML, CSS and a `<script type="module">`).
- `visualizer/nt4.js`: the small NetworkTables 4 client.
- `visualizer/msgpack.js`: a minimal MessagePack decoder (and encoder for the two input booleans).
- `visualizer/lib/three.module.min.js` and `visualizer/lib/OrbitControls.js`: Three.js, vendored with an exact version (use a release at least a few months old, and keep its MIT license file in `visualizer/lib/`).
- `visualizer/README.md`: how to open it (student-facing, short).

### NT4 client (keep it under about 250 lines, readable)
- Connect to `ws://<host>:5810/nt/visualizer` with subprotocol `v4.1.networktables.first.wpi.edu` (fall back to `networktables.first.wpi.edu`).
- Text frames are JSON arrays of `{method, params}`. Send `subscribe` with `{topics: ["/Visualizer/"], subuid: 1, options: {prefix: true}}`. Handle `announce` (map topic id → name and type) and `unannounce`.
- Binary frames are MessagePack arrays `[topicId, timestampMicros, typeId, value]`; keep the latest value per topic name.
- To set the two input booleans: send `publish` with a pubuid for each topic (type `boolean`), then binary `[pubuid, 0, 0, true]`.
- Reconnect every second while disconnected; show the connection state on the page.
- Host box at the top, default `localhost`, remembered in `localStorage` (wrapped in try/catch). Later this lets the page point at a real robot (`10.2.45.2`).
- Ignore the RTT/time-sync messages except as needed to stay connected.

### Scene (Three.js, simple shapes, no CAD)
- WPILib coordinates are x forward, y left, z up; Three.js uses y up. Convert in one place.
- Floor grid; a chassis box about 0.7 m × 0.7 m × 0.15 m on bumpers (AdamBots gold `#FFD800` top plate, red or blue bumper toggle with "245" on them).
- **Intake** at the front: two horizontal roller cylinders that spin by `intake/rollerAngleDeg`, with stripes or a painted dot so the spin direction is obvious (a plain cylinder looks still when it spins).
- **Beam break**: a small emitter and receiver either side of the path at position 0.5, with a visible beam line that is red and solid when clear, and dark when blocked.
- **Shooter** at the back, angled up about 45°: one flywheel with a marker dot, spinning by `shooter/wheelAngleDeg`, plus a simple hood.
- **Game piece**: an orange sphere about 0.18 m; its location is interpolated along a path (floor in front → between the rollers → beam break → up into the shooter). `LAUNCHED` pieces fly out on a simple projectile arc using `piece/launchSpeed`; `EJECTED` pieces roll out the front.
- Lighting and camera: orbit controls, a sensible starting angle (front three-quarter view), dark background matching the bootcamp decks.

### HUD (HTML overlay, large and readable on a projector)
- Connection: connected / trying `ws://localhost:5810`.
- Mode and enabled state, with a clear "DISABLED" banner when disabled.
- One card per subsystem: name, running command (from `*/command`), throttle bar from -1 to 1, RPM.
- Beam break: "clear" / "blocked".
- Battery volts.
- Buttons: **Place game piece**, **Remove game piece**.
- A one-line hint: "Simulate Robot Code, pick Teleoperated, Driver Control, Enable. Hold P (intake) or Q (shooter); A, B, Y if you've done the Session 5 Try-it."

### Opening it
- Primary: double-click `visualizer/index.html` (a `file://` page can open a WebSocket to `ws://localhost:5810`). **Verify this works**; if the NT server rejects the browser's origin, fall back to the next option.
- Fallback: serve the folder from the robot program with `org.wpilib.net.WebServer.start(port, path)` (exists in Alpha 8), in simulation only, and open `http://localhost:<port>/`. If used, document the port in the README.

## 6. Testing 

1. **Build:** `./gradlew build` passes on GradleRIO 2027.0.0-alpha-8 with no new vendordeps.
2. **Existing behavior unchanged:** a JUnit simulation test (use `HAL.initialize()`, `DriverStationSim`, `GamepadSim`, `DIOSim`, `SimDeviceSim`, and run `robot.startCompetition()` on a thread) shows:
   - right bumper held: intake throttle 0.5; left bumper held: shooter throttle 0.75;
   - DIO 0 set low from the test still reads as `hasGamePiece() == true` and is **not** overwritten by SandboxSim in the following loops.
3. **Physics and world model** (JUnit, same harness, publish `placePiece` via an NT client in the test or call SandboxSim directly):
   - flywheel RPM rises over about a second at 0.75 throttle and decays when stopped;
   - with a placed piece and the intake running, the piece reaches the beam break, DIO 0 goes low, `hasGamePiece()` becomes true;
   - with the flywheel above 2000 RPM and the intake running, the piece launches and DIO 0 goes high; with the flywheel stopped, it stays held.
4. **Topics:** a test NT client (Java `NetworkTableInstance.create()` as a client to localhost) sees every topic in the contract with sensible values.
5. **Page, end to end:** start the robot simulation headless (`./gradlew run` with the Sim GUI disabled, or the test harness), open `visualizer/index.html` in headless Chromium with Playwright, and check: the page reports "connected"; HUD values match the topics; pressing the right bumper (via the sim) changes the intake card's throttle to 0.5 and the roller angle keeps increasing; Place game piece puts a piece on the floor. Take screenshots at each step for review.
6. **Offline:** with the network disabled, the page still loads everything (no CDN requests).

## 7. Acceptance criteria
- `git diff` against `main` shows no changes in `subsystems/`, `opmode/`, `Buttons.java` or `Constants.java`, and exactly the two-line addition (comment plus `Visualizer.start(this);`) in `Robot.java`, plus its import.
- A student opens the Sandbox, runs Simulate Robot Code, opens `visualizer/index.html`, enables Driver Control, holds P: the intake rollers visibly spin. Holds Q: the flywheel spools up over about a second.
- With the Session 5 solution code (`intakeUntilFull` on A, `spitOut` on B, `ScoringCommands.shoot` on Y): Place game piece, press A, the piece rides in and the command stops by itself at the beam break; press Y, the piece launches in an arc; B spits it out the front.
- Flipping DIO 0 in the Sim GUI still works exactly as the Try-it says, and the page shows the piece appear and disappear accordingly.
- Nothing in the decks, `TRY-IT-5.md` or the Session 5 solutions needs to change.

## 8. Teaching notes (for the README and a possible slide)
- "The robot code does the physics; the page just draws what NetworkTables says." This is the same path a dashboard uses on the real robot.
- Students can open `sim/VisualizerPublisher` to see that publishing a value is two lines, but nothing in `subsystems/` or `opmode/` depends on the `sim` package, so they can ignore it entirely.

## 9. Later phases (not now)
1. **Phoenix 6:** when CTRE ships an Alpha 8 build, switch the subsystems to Phoenix 6 `TalonFX` and feed `TalonFXSimState` (rotor position and velocity, before gear ratio, plus supply voltage) from the same two `FlywheelSim`s. Only `SandboxSim`'s input and output lines change; the topic contract and page stay the same.
2. **Real robot:** point the page's host box at the robot's address and it shows measured values (with encoders on the rollers and flywheel).
3. **Drive around:** a drivetrain subsystem, `DifferentialDrivetrainSim` (or a swerve sim library once one supports 2027), robot pose published as plain doubles (`x`, `y`, `headingDeg`), a field floor and a follow camera.
4. **Arm or elevator:** `SingleJointedArmSim` or `ElevatorSim` (with `Models.singleJointedArmFromPhysicalConstants` or `elevatorFromPhysicalConstants`) in the same pattern, for teaching mechanisms with positions and limits.

## References
- NetworkTables 4 protocol: `ntcore/doc/networktables4.adoc` in wpilibsuite/allwpilib (port 5810, `/nt/<name>`, JSON text frames, MessagePack binary frames).
- WPILib "New for 2027" changelog (NT3 removed; SmartDashboard replaced by Telemetry and Tunables APIs).
- CTRE Phoenix 6 "Introduction to Simulation" (SimState pattern).
- wpilibsuite/SystemcoreTesting `CTR-Phoenix.md` (Phoenix 6 builds per WPILib alpha).
