# Sandbox

A practice repository for Adambots programmers. Use it to learn the Git and GitHub workflow
we use on robot code: create a branch, commit, push, open a pull request and get it reviewed.
Nothing here is robot code, so you can't break anything that matters.

## The rules

- **`main` is protected.** You can't push to it directly. All changes reach `main` through a
  pull request that a mentor reviews and merges.
- **Work on your own branch.** Name it `yourname/what-you-are-doing`, for example
  `alex/first-branch`.
- **Clean up after yourself.** Delete your branch once your pull request is merged or closed.

## Practice exercise

1. **Clone the repository** (once per computer):

   ```
   git clone https://github.com/Adambots-245/Sandbox.git
   cd Sandbox
   ```

2. **Create your branch** and switch to it:

   ```
   git switch -c yourname/first-branch
   ```

3. **Make a change.** Add a file under `practice/` named after yourself (for example
   `practice/alex.md`) and write a line or two in it.

4. **Commit it:**

   ```
   git add practice/alex.md
   git commit -m "Add Alex's practice file"
   ```

5. **Push your branch to GitHub:**

   ```
   git push -u origin yourname/first-branch
   ```

6. **Open a pull request.** GitHub prints a link after the push, or go to the repository page
   and click **Compare & pull request**. Describe what you changed and why.

7. **Respond to review.** If a reviewer asks for changes, make them on the same branch, commit
   and push again. The pull request updates automatically.

8. **After it's merged,** update your local `main` and delete the branch:

   ```
   git switch main
   git pull
   git branch -d yourname/first-branch
   ```

## Next challenges

- **Keep a branch up to date.** While your pull request is open, someone else's gets merged.
  Bring their changes into your branch with `git pull origin main`.
- **Resolve a merge conflict.** Pair up with a teammate and both edit the same line of
  `practice/shared.md` on separate branches. Whoever merges second has to resolve the conflict.
- **Use the tools you'll use on the robot.** Repeat the exercise from VS Code's Source Control
  panel or GitHub Desktop instead of the command line.

## Getting Git

- **Windows:** install [Git for Windows](https://git-scm.com/download/win). VS Code and
  GitHub Desktop both use it.
- **macOS:** run `git --version` in Terminal. If Git isn't installed, macOS offers to install it.
- **GitHub Desktop** (optional, Windows and macOS): <https://desktop.github.com>

The first time you commit, tell Git who you are:

```
git config --global user.name "Your Name"
git config --global user.email "the-email-on-your-github-account@example.com"
```
