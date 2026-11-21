# 3D visualizer

See your robot code move a 3D robot. Hold a button in the simulator and the intake rollers spin, a game piece rides in, the beam break goes dark and the flywheel spools up.

## Open it
1. In VS Code: WPILib menu → **Simulate Robot Code** (Sim GUI). Leave it running.
2. In a browser (Chrome or Edge), open **http://localhost:5811/**. The simulation serves the page there.
   You can also double-click `visualizer/index.html`: it waits for the simulation and then opens that address itself.
3. In the simulation window's **Robot State** box: click **Teleoperated**, pick **Driver Control**, then click **Enable**.
4. Click in the simulation window so it gets your keys, then:

| Key | Button | What happens |
|---|---|---|
| **P** | right bumper | intake rollers spin |
| **Q** | left bumper | flywheel spools up over about a second |
| **Z** | A | `intakeUntilFull` (after the Session 5 Try-it) |
| **X** | B | spit out (after "Done early?") |
| **V** | Y | shoot (Session 5 solutions) |

Click **Place game piece** to put a game piece on the floor in front of the intake. Hold **P** and it rides in until the beam break sees it.

To shoot it, the intake has to push it up into a fast flywheel: hold **Q** and, while still holding it, hold **P**. Q on its own only spins the flywheel. The flywheel needs at least 2000 RPM to take the game piece. When you let go of Q it coasts, so pressing P within about 2 seconds still works. If the flywheel is too slow, the piece stays stuck at the beam break and the page shows the flywheel's speed.

The page works offline: everything it needs is in this folder.

## How it works
The robot code does the physics; the page just draws what NetworkTables says. This is the same path a dashboard uses on the real robot.

- `src/main/java/first/robot/sim/` runs only alongside your code. `SandboxSim` reads the motor throttles your commands set, spins two simulated flywheels (the intake rollers and the shooter), moves the game piece and sets the beam break (DIO 0). `VisualizerPublisher` publishes everything under `/Visualizer/` in NetworkTables. Publishing a value is two lines: make a publisher once, `set` it every loop.
- The page connects to NetworkTables at `ws://localhost:5810` and draws the robot with Three.js.
- Nothing in `subsystems/` or `opmode/` uses the `sim` package. You can ignore it entirely.

You can still flip **Hardware → DIO → In[0]** in the Sim GUI, exactly as the Try-it says: the page shows a game piece appear at the beam break (0, blocked) and disappear (1, clear).

To watch a real robot later, type its address (for example `10.2.45.2`) in the **Robot address** box.

## Files
- `index.html`: the page.
- `nt4.js`: a small NetworkTables 4 client. `msgpack.js`: the MessagePack decoder it uses.
- `lib/`: Three.js 0.170.0 (`three.module.min.js`, `OrbitControls.js`) and its MIT `LICENSE`.

## Troubleshooting
- **"trying ws://localhost:5810"**: the simulation isn't running yet, or it's still starting. The page keeps retrying every second.
- **http://localhost:5811/ doesn't load**: make sure **Simulate Robot Code** is running. The console in VS Code prints `Visualizer: open http://localhost:5811/` when the page is ready.
- **Nothing moves**: check the Sim GUI says **Enabled** and **Driver Control** is selected, and that the simulation window (not the browser) has your keys.
