# Session 5 Try-its

Both Try-its happen in simulation, on your own branch. No robot needed.

## Before you start
1. On `main`, click **Sync** (bottom-left of VS Code).
2. Click the branch name → **Create new branch** → `yourname-sensors`.

## Try-it 1: sensor detective (8 min)
Goal: prove which way the beam break reads, without a robot.

1. Open `src/main/java/first/robot/Robot.java`.
2. Add a field inside the `Robot` class, outside any method:
   ```java
   private int loops = 0;
   ```
3. In `robotPeriodic()`, under the scheduler line, add:
   ```java
   loops++;
   if (loops % 50 == 0) {
       System.out.println("Game piece? " + intake.hasGamePiece());
   }
   ```
   The loop runs every 20 ms, so 50 loops is one second.
4. WPILib menu → **Build Robot Code**, then **Simulate Robot Code** (Sim GUI).
5. Watch the console in VS Code: `Game piece? false` once a second.
6. In the simulation window, find the **DIO** list and change **DIO 0** (the beam break).
7. The printout flips to `true`. Which DIO value means "game piece"? (Hint: look at `hasGamePiece()` and its `!`.)
8. Source Control → stage `Robot.java` → message `Print the beam break every second` → **Commit**.

## Try-it 2: button plus sensor (12 min)
Goal: press A, and the intake runs until the beam break sees a game piece.

1. In `src/main/java/first/robot/subsystems/Intake.java`, add this method inside the class:
   ```java
   public Command intakeUntilFull() {
       return run(coroutine -> {
           motor.setThrottle(Constants.kIntakeSpeed);
           coroutine.waitUntil(this::hasGamePiece);
           motor.setThrottle(0.0);
       }).named("Intake Until Full");
   }
   ```
2. In `src/main/java/first/robot/opmode/DriverControl.java`, add one line in the constructor, under the bumper lines:
   ```java
   Buttons.XboxAButton.onTrue(robot.intake.intakeUntilFull());
   ```
3. **Build Robot Code**, then **Simulate Robot Code**.
4. In the simulation window's **Robot State** box: click **Teleoperated**, pick the **Driver Control** OpMode (it says GOOD), then click **Enable**. The keyboard is already in joystick slot 1.
5. Press **Z** (the A button). In **Other Devices → PWMMotorController[0]**, Throttle goes to **0.5** and stays there.
6. Open **Hardware → DIO**. Change **In[0]** from **1 (high)** to **0 (low)**: the beam is now blocked, so the command finishes and Throttle goes to **0**. Set it back to **1 (high)** afterwards.
7. Commit (`Add intake until full on A`) and click **Publish Branch**.

## Done early?
Make B (`Buttons.XboxBButton`, the **X** key in simulation) spit the game piece out: a new command in `Intake` that runs the motor at `-Constants.kIntakeSpeed`, waits one second with `coroutine.wait(Seconds.of(1));`, then stops. You'll need `import static org.wpilib.units.Units.Seconds;` at the top (the light bulb can add it).

## Stuck?
Ask a mentor. After the session, the finished code goes on the bootcamp site: **Download solutions (.zip)** on the Session 5 card. Unzip it into `C:\bootcamp` and open the `session5-solutions` folder to run it, or copy single files into your own Sandbox branch.

## Homework
On github.com/Adambots-245/Sandbox, open a pull request from `yourname-sensors` into `main`.
