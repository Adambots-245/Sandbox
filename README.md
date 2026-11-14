# AdamBots Sandbox

A practice robot project for Team 245 rookies. Break things here, not on the competition robot.

Everything below happens in **WPILib VS Code 2027** (look for the WPILib logo in the top-right corner). No terminal and no GitHub Desktop needed.

## 1. Get the project (clone)
1. Press **Ctrl+Shift+P** (Mac: **Cmd+Shift+P**) and run **Git: Clone**.
2. Paste `https://github.com/Adambots-245/Sandbox` and press Enter.
3. Pick a folder (for example `Documents/Robotics`), then click **Open**.
4. The first time, sign in to GitHub in the browser when VS Code asks.

## 2. Start new work (branch)
1. Make sure you're on `main`: the branch name is in the **bottom-left** corner of the window.
2. Click **Sync** (the circling arrows next to it) to get everyone's latest work.
3. Click the branch name, choose **Create new branch**, and type a name: `yourname-topic`, all lower case, words joined with dashes. Examples: `maya-first-commit`, `leo-intake-beam-break`.

## 3. Save your work (commit)
1. Open **Source Control** (third icon on the left, or **Ctrl+Shift+G**).
2. Click a changed file to see what changed.
3. Hover over a file and click **+** to stage it. Stage only the files that belong to one idea.
4. Type a message that finishes the sentence "This commit will...", for example `Add Maya to the roster`.
5. Click **Commit**.

## 4. Share your work (push)
- New branch: click **Publish Branch**.
- After that: click **Sync Changes** (it pulls, then pushes).
- Nobody pushes straight to `main`. Changes get into `main` through a pull request.

## 5. Build and test
- WPILib menu (the logo, top right) → **Build Robot Code**. Look for `BUILD SUCCESSFUL`. The first build needs internet.
- WPILib menu → **Simulate Robot Code**, pick **Sim GUI**. A simulation window opens; console output appears in VS Code.
  1. In the **Robot State** box, click **Teleoperated**.
  2. In the **OpMode** list under it, pick **Driver Control**. The word next to it changes to **GOOD**.
  3. Click **Enable**.
  4. The keyboard is already plugged into joystick slot 1 (that setting is saved in `simgui-ds.json`). Hold **P** for the right bumper (intake) or **Q** for the left bumper (shooter).
  5. The VS Code console prints `Intake: running at 0.5` when you press P and `Intake: stopped` when you let go (Q: `Shooter: spinning at 0.75`). In the simulation window, **Other Devices** → **PWMMotorController[0]** (intake) or **[1]** (shooter) shows the Throttle too.
- Keyboard keys for the gamepad: **Z** = A, **X** = B, **C** = X, **V** = Y, **Q** = left bumper, **P** = right bumper. Using a real gamepad? Plug it in and drag it from **System Joysticks** onto **Joystick[1]**.
- Weird build problem? WPILib menu → **Run Gradle Clean**, then build again.
- Deploy (**Shift+F5**) only when a mentor says so, with our three rules: **Look**, **Call** "Enabling!", **Hand on E-Stop**. Never power off the robot during a deploy.

## 6. Homework: your first pull request
1. Go to https://github.com/Adambots-245/Sandbox. After you publish a branch, GitHub shows a yellow bar with **Compare & pull request**. Click it. (Or: **Pull requests** tab → **New pull request** → base: `main`, compare: your branch.)
2. Title: what the change does. Description: one or two sentences on why.
3. Click **Create pull request**.
4. A mentor will review it. If they ask for a change, make it on the **same branch**, commit and Sync: the pull request updates by itself.
5. After it's approved, a mentor merges it. Then in VS Code: switch to `main` and click **Sync**.

## What's in here
A real WPILib 2027 robot project, made with the **Command v3 Robot** template (team 245), plus our own code.

| Path | What it is |
|---|---|
| `src/main/java/first/robot/Robot.java` | The robot: builds the subsystems and the gamepad, runs the scheduler every loop |
| `src/main/java/first/robot/Constants.java` | Every fixed number: motor channels, speeds, the gamepad port |
| `src/main/java/first/robot/subsystems/Intake.java` | The intake: a motor and a beam break |
| `src/main/java/first/robot/subsystems/Shooter.java` | The shooter: one flywheel motor |
| `src/main/java/first/robot/opmode/DriverControl.java` | The teleop OpMode: hold the right bumper to intake, the left bumper to spin the shooter |
| `src/main/java/first/Main.java` | Starts the robot program. Don't edit it |
| `src/main/deploy/` | Extra files sent to the robot with every deploy (autonomous paths go here) |
| `vendordeps/` | Vendor libraries. `CommandsV3.json` adds Commands v3 |
| `build.gradle`, `settings.gradle`, `gradlew`, `gradle/` | Gradle, the tool that builds the code. Don't edit unless a mentor asks |
| `.wpilib/wpilib_preferences.json` | Our team number (245), so Deploy finds our robot |
| `simgui-ds.json` | Simulation settings: puts the keyboard in joystick slot 1 with P and Q as the bumpers |
| `roster/` | One file per team member. Your first commit adds yours |
| `CONFLICT-PRACTICE.md` | Steps for the merge-conflict practice in pairs |
| Branch `scavenger-hunt` | Planted mistakes for the VS Code scavenger hunt, downloaded as a zip. Never merged |

## Try a change
1. Make a branch (step 2 above).
2. In `Constants.java`, change `kIntakeSpeed` from `0.5` to `0.8`.
3. **Simulate Robot Code**, then Teleoperated, Driver Control, Enable (section 5), and hold **P**. The console now says `Intake: running at 0.8`.
4. Commit, then **Publish Branch**.
