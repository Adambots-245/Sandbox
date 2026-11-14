# Merge conflict practice (pairs)

Goal: make a merge conflict on purpose, then fix it in VS Code. This branch is only for practice: **no pull request, and it never goes into `main`.**

Your mentor gives each pair a number. Below, replace `N` with it.

## Round 1: `kIntakeSpeed`
1. **Partner A:** on `main`, click **Sync**. Click the branch name (bottom-left) → **Create new branch** → `pair-N`. Then click **Publish Branch**.
2. **Partner B:** click **Sync**, then click the branch name and pick `origin/pair-N`. You're both on the same branch now.
3. **Both:** open `src/main/java/first/robot/Constants.java` and change `kIntakeSpeed` to a **different** number each (for example A: `0.8`, B: `0.6`). Save, stage, and commit with a message like `Set intake speed to 0.8`.
4. **Partner A:** click **Sync Changes** first. It works.
5. **Partner B:** click **Sync Changes**. VS Code reports a conflict in `Constants.java`. Open the file:

   ```
   <<<<<<< HEAD
   public static final double kIntakeSpeed = 0.6;
   =======
   public static final double kIntakeSpeed = 0.8;
   >>>>>>> 8f3c2e1d...
   ```

   (The text after `>>>>>>>` is the id of your partner's commit. Yours will look different.)

   - The top part is **Current** (what you had). The bottom part is **Incoming** (your partner's commit).
   - Talk to your partner: which number is right?
   - Click **Accept Current Change**, **Accept Incoming Change** or **Accept Both Changes** above the markers. (Both would define the same name twice, so it won't compile here.) Or edit by hand: keep one line, delete the three marker lines.
6. **Partner B:** save, stage `Constants.java` with **+**, click **Commit** (the message is filled in for you), then **Sync Changes**.
7. **Partner A:** click **Sync**. You both have the same file again.

## Round 2: swap roles
Repeat steps 3 to 7 with `kShooterSpeed`, with Partner B syncing first this time.

## Check
- No `<<<<<<<`, `=======` or `>>>>>>>` lines left anywhere. (Search across files: **Ctrl+Shift+F**, Mac **Cmd+Shift+F**.)
- WPILib menu → **Build Robot Code** says `BUILD SUCCESSFUL`.
