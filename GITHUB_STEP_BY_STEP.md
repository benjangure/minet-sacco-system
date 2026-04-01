# GitHub Step-by-Step Visual Guide

Complete walkthrough with screenshots descriptions and exact commands.

---

## PART 1: ONE-TIME SETUP

### Step 1.1: Install Git

**What to do:**
1. Go to https://git-scm.com/download/win
2. Click the download button (Windows Standalone Installer)
3. Run the installer
4. Click "Next" through all screens (defaults are fine)
5. Click "Install"
6. Click "Finish"

**Verify it worked:**
Open PowerShell and type:
```bash
git --version
```

You should see: `git version 2.x.x.windows.x`

---

### Step 1.2: Configure Git

Open PowerShell and run these commands (one at a time):

```bash
git config --global user.name "John Doe"
```
Replace "John Doe" with your actual name.

```bash
git config --global user.email "john@example.com"
```
Replace with your actual email.

**Verify it worked:**
```bash
git config --global user.name
git config --global user.email
```

You should see your name and email printed.

---

### Step 1.3: Create GitHub Account

1. Go to https://github.com
2. Click "Sign up" (top right)
3. Enter your email
4. Create a password
5. Choose a username (e.g., `johndoe`)
6. Click "Create account"
7. Verify your email (check your inbox)
8. Complete the setup wizard

**You now have a GitHub account!**

---

### Step 1.4: Create GitHub Repository

1. Log in to GitHub (https://github.com)
2. Click the "+" icon in the top right corner
3. Click "New repository"
4. Fill in the form:
   - **Repository name:** `minetsacco`
   - **Description:** `SACCO Management System - Member Portal & Backend`
   - **Visibility:** Choose "Public" (open source) or "Private" (confidential)
   - **Initialize this repository with:** Leave unchecked
5. Click "Create repository"

**You'll see a page with setup instructions. Copy the URL that looks like:**
```
https://github.com/yourusername/minetsacco.git
```

**Save this URL - you'll need it in the next step!**

---

## PART 2: FIRST PUSH TO GITHUB

### Step 2.1: Open PowerShell in Your Project

1. Open File Explorer
2. Navigate to: `C:\Users\Elitebook\OneDrive\Desktop\minetsacco-main`
3. Right-click in empty space
4. Click "Open in Terminal" (or "Open PowerShell window here")

You should see a PowerShell window open in that directory.

---

### Step 2.2: Initialize Git

In PowerShell, type:

```bash
git init
```

You should see: `Initialized empty Git repository in C:\Users\...\minetsacco-main\.git`

---

### Step 2.3: Add Remote Repository

Replace `YOUR_REPO_URL` with the URL you saved from Step 1.4:

```bash
git remote add origin https://github.com/yourusername/minetsacco.git
```

**Verify it worked:**
```bash
git remote -v
```

You should see:
```
origin  https://github.com/yourusername/minetsacco.git (fetch)
origin  https://github.com/yourusername/minetsacco.git (push)
```

---

### Step 2.4: Create .gitignore File

1. In PowerShell, type:
```bash
notepad .gitignore
```

2. A Notepad window opens. Paste this content:

```
# Dependencies
node_modules/
backend/target/
.gradle/
.idea/

# Environment files
.env
.env.local
.env.*.local

# Build outputs
dist/
build/
android/app/build/
*.apk
*.aab

# IDE
.vscode/
*.swp
*.swo
*~

# OS
.DS_Store
Thumbs.db

# Logs
*.log
npm-debug.log*

# Temporary files
*.tmp
.kiro/

# Debug documents
APK_BUILD_ISSUE_REPORT.md
APK_CURRENT_STATE_AND_ISSUES.md
APK_FIXES_APPLIED.md
APK_FINAL_FIX_SUMMARY.md
APK_LOGIN_ERROR_FIX.md
APK_REBUILD_INSTRUCTIONS.md
APK_SIMPLE_WRAPPER_SETUP.md
APK_TESTING_*.md
AUDIT_TRAIL_*.md
CHANGES_MADE.md
READY_FOR_TESTING.md
MOBILE_DOWNLOAD_CONFIRMATION.md
DEPOSIT_APPROVAL_TRANSACTION_FIX.md
LOAN_APPLICATION_STATUS_VERIFIED.md
SHARES_ACCOUNT_DORMANT_IMPLEMENTATION.md
SYSTEM_AUDIT_AND_REQUIREMENTS.md
APP_VERSION_MANAGEMENT.md
AUTHENTICATION_FLOW_DIAGRAM.md
```

3. Press Ctrl+S to save
4. Close Notepad

---

### Step 2.5: Stage All Files

In PowerShell, type:

```bash
git add .
```

This tells Git to prepare all files for upload.

**Check what will be uploaded:**
```bash
git status
```

You should see many files listed in green with "new file:" prefix.

---

### Step 2.6: Create First Commit

In PowerShell, type:

```bash
git commit -m "Initial commit: Minet SACCO system with backend and member portal"
```

You should see output showing files being committed.

---

### Step 2.7: Set Main Branch

In PowerShell, type:

```bash
git branch -M main
```

This ensures your branch is named "main" (GitHub's default).

---

### Step 2.8: Push to GitHub

In PowerShell, type:

```bash
git push -u origin main
```

**First time only:** You may see a browser window asking you to authenticate with GitHub. Follow the prompts to authorize.

After authentication, you should see:
```
Enumerating objects: ...
Counting objects: ...
Compressing objects: ...
Writing objects: ...
```

**Success!** Your code is now on GitHub!

---

### Step 2.9: Verify on GitHub

1. Go to https://github.com/yourusername/minetsacco
2. You should see all your files listed
3. Click on files to view them
4. Check the commit history (click "Commits")

---

## PART 3: DAILY WORKFLOW

### Before Starting Work

Open PowerShell in your project directory and pull latest changes:

```bash
git pull origin main
```

This ensures you have the latest code if others made changes.

---

### After Making Changes

#### Step 3.1: Check What Changed

```bash
git status
```

You'll see files listed in red (modified) or green (new).

#### Step 3.2: Stage Changes

Option A - Stage all changes:
```bash
git add .
```

Option B - Stage specific files:
```bash
git add path/to/file.ts
git add path/to/another/file.tsx
```

#### Step 3.3: Review Changes (Optional)

```bash
git diff
```

This shows exactly what changed in each file.

#### Step 3.4: Commit Changes

```bash
git commit -m "Brief description of what you changed"
```

**Good commit messages:**
- "Add member dashboard page"
- "Fix loan approval workflow bug"
- "Update documentation for deployment"
- "Improve member portal performance"

**Bad commit messages:**
- "changes"
- "fix"
- "update"
- "asdf"

#### Step 3.5: Push to GitHub

```bash
git push origin main
```

You should see:
```
Enumerating objects: ...
Writing objects: ...
```

**Done!** Your changes are now on GitHub.

---

## PART 4: COMMON SCENARIOS

### Scenario A: Update Only Documentation

```bash
# Make changes to .md files
git add *.md
git commit -m "Update documentation"
git push origin main
```

### Scenario B: Update Backend Code

```bash
# Make changes to Java files in backend/
git add backend/
git commit -m "Add new endpoint for member verification"
git push origin main
```

### Scenario C: Update Frontend Code

```bash
# Make changes to React files in minetsacco-main/
git add minetsacco-main/
git commit -m "Improve member dashboard UI"
git push origin main
```

### Scenario D: Update Everything

```bash
# Make changes to multiple parts
git add .
git commit -m "Update backend, frontend, and documentation"
git push origin main
```

### Scenario E: Update Single File

```bash
# Make changes to one file
git add path/to/specific/file.ts
git commit -m "Fix bug in file.ts"
git push origin main
```

---

## PART 5: TROUBLESHOOTING

### Problem: "fatal: not a git repository"

**Solution:** You're not in the right directory.

```bash
cd C:\Users\Elitebook\OneDrive\Desktop\minetsacco-main
git status
```

### Problem: "error: src refspec main does not match any"

**Solution:** You haven't made any commits yet.

```bash
git add .
git commit -m "Initial commit"
git branch -M main
git push -u origin main
```

### Problem: "Permission denied (publickey)"

**Solution:** GitHub authentication failed. Use HTTPS instead:

```bash
git remote set-url origin https://github.com/yourusername/minetsacco.git
git push origin main
```

### Problem: "Your branch is ahead of 'origin/main' by X commits"

**Solution:** You have commits not pushed yet.

```bash
git push origin main
```

### Problem: "Your branch is behind 'origin/main' by X commits"

**Solution:** Someone else pushed changes. Pull them first.

```bash
git pull origin main
```

### Problem: "Changes not staged for commit"

**Solution:** You modified files but didn't stage them.

```bash
git add .
git commit -m "Your message"
git push origin main
```

### Problem: "fatal: The current branch main has no upstream branch"

**Solution:** First push of a new branch.

```bash
git push -u origin main
```

---

## PART 6: QUICK REFERENCE CARD

Print this and keep it handy:

```
DAILY WORKFLOW:

1. Start of day:
   git pull origin main

2. Make changes to files

3. After changes:
   git add .
   git commit -m "Description"
   git push origin main

4. Repeat step 2-3 as needed

5. End of day:
   git push origin main
```

---

## PART 7: NEXT STEPS

1. ✅ Install Git (Step 1.1)
2. ✅ Configure Git (Step 1.2)
3. ✅ Create GitHub Account (Step 1.3)
4. ✅ Create Repository (Step 1.4)
5. ✅ Initialize Git (Step 2.1-2.2)
6. ✅ Add Remote (Step 2.3)
7. ✅ Create .gitignore (Step 2.4)
8. ✅ Push to GitHub (Step 2.5-2.8)
9. ✅ Verify (Step 2.9)
10. ✅ Use Daily Workflow (Part 3)

**You're done!** Your code is on GitHub and you know how to update it.

---

## SUPPORT

- GitHub Help: https://docs.github.com
- Git Documentation: https://git-scm.com/doc
- GitHub Desktop (GUI): https://desktop.github.com (easier alternative to command line)
