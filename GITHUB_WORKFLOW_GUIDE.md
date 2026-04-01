# GitHub Workflow Guide - Minet SACCO

Complete instructions for pushing code to GitHub and making updates.

## Table of Contents

1. [Initial Setup](#initial-setup)
2. [First Push to GitHub](#first-push-to-github)
3. [Daily Workflow](#daily-workflow)
4. [Making Updates](#making-updates)
5. [Handling Conflicts](#handling-conflicts)
6. [Best Practices](#best-practices)
7. [Troubleshooting](#troubleshooting)

---

## Initial Setup

### Step 1: Install Git

**Windows:**
1. Download from: https://git-scm.com/download/win
2. Run the installer
3. Accept all defaults (or customize if needed)
4. Restart your computer

**Verify Installation:**
```bash
git --version
```

### Step 2: Configure Git

Open PowerShell and run:

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

Replace with your actual name and email.

**Verify Configuration:**
```bash
git config --global user.name
git config --global user.email
```

### Step 3: Create GitHub Account

1. Go to https://github.com
2. Click "Sign up"
3. Create account with your email
4. Verify email address
5. Complete profile setup

### Step 4: Create GitHub Repository

1. Log in to GitHub
2. Click "+" icon (top right) → "New repository"
3. Fill in details:
   - Repository name: `minetsacco` (or your preferred name)
   - Description: "SACCO Management System - Member Portal & Backend"
   - Visibility: **Public** (for open source) or **Private** (for confidential)
   - Do NOT initialize with README (we have one)
4. Click "Create repository"

You'll see a page with setup instructions. Copy the repository URL (looks like `https://github.com/yourusername/minetsacco.git`)

---

## First Push to GitHub

### Step 1: Initialize Git in Your Project

Open PowerShell in your project root directory and run:

```bash
cd C:\Users\Elitebook\OneDrive\Desktop\minetsacco-main
git init
```

### Step 2: Add Remote Repository

Replace `YOUR_REPO_URL` with the URL from GitHub:

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

### Step 3: Create .gitignore File

Create a file named `.gitignore` in your project root with this content:

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

# Debug documents (exclude these)
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

### Step 4: Stage Files

Add all files to Git:

```bash
git add .
```

**Check what will be added:**
```bash
git status
```

You should see green "new file" entries for all files you want to push.

### Step 5: Create First Commit

```bash
git commit -m "Initial commit: Minet SACCO system with backend and member portal"
```

### Step 6: Push to GitHub

```bash
git branch -M main
git push -u origin main
```

**First time only:** You may be prompted to authenticate. Follow the GitHub authentication flow.

**Verify:** Go to your GitHub repository URL and refresh. You should see all your files!

---

## Daily Workflow

### Before Starting Work

Always pull latest changes:

```bash
git pull origin main
```

This ensures you have the latest code if others made changes.

### After Making Changes

#### Step 1: Check Status

```bash
git status
```

You'll see modified files in red.

#### Step 2: Stage Changes

Stage specific files:
```bash
git add path/to/file.ts
git add path/to/another/file.tsx
```

Or stage all changes:
```bash
git add .
```

#### Step 3: Commit Changes

```bash
git commit -m "Brief description of what changed"
```

**Good commit messages:**
- ✅ "Add member dashboard page"
- ✅ "Fix loan approval workflow bug"
- ✅ "Update documentation for deployment"
- ❌ "changes"
- ❌ "fix stuff"

#### Step 4: Push to GitHub

```bash
git push origin main
```

---

## Making Updates

### Scenario 1: Update a Single File

```bash
# Make changes to the file
# Then:
git add path/to/file.md
git commit -m "Update documentation for feature X"
git push origin main
```

### Scenario 2: Update Multiple Files

```bash
# Make changes to multiple files
# Then:
git add .
git status  # Review what will be committed
git commit -m "Implement new loan approval workflow"
git push origin main
```

### Scenario 3: Update Backend Code

```bash
cd backend
# Make changes to Java files
cd ..
git add backend/
git commit -m "Add new endpoint for member KYC verification"
git push origin main
```

### Scenario 4: Update Frontend Code

```bash
cd minetsacco-main
# Make changes to React/TypeScript files
cd ..
git add minetsacco-main/
git commit -m "Improve member dashboard UI and performance"
git push origin main
```

### Scenario 5: Update Documentation

```bash
# Edit markdown files
git add *.md
git commit -m "Update usage guide and deployment instructions"
git push origin main
```

---

## Handling Conflicts

### If Someone Else Made Changes

When you `git pull`, you might see:

```
CONFLICT (content merge): Merge conflict in file.ts
```

#### Step 1: Open the Conflicted File

You'll see markers like:

```
<<<<<<< HEAD
Your changes here
=======
Their changes here
>>>>>>> branch-name
```

#### Step 2: Resolve Manually

Edit the file to keep what you want:

```
Final version of the code
```

#### Step 3: Complete the Merge

```bash
git add file.ts
git commit -m "Resolve merge conflict in file.ts"
git push origin main
```

### If You Accidentally Pushed Wrong Code

```bash
# Undo last commit (keeps changes locally)
git reset --soft HEAD~1

# Or undo last commit (discards changes)
git reset --hard HEAD~1

# Then make correct changes and push again
git add .
git commit -m "Correct commit message"
git push origin main
```

---

## Best Practices

### 1. Commit Frequently

Make small, logical commits instead of one giant commit:

```bash
# Good: Multiple focused commits
git commit -m "Add member registration form"
git commit -m "Add form validation"
git commit -m "Add success notification"

# Bad: One huge commit
git commit -m "Add member registration with validation and notifications"
```

### 2. Write Clear Commit Messages

```bash
# Good
git commit -m "Fix null pointer exception in loan calculation"

# Bad
git commit -m "fix bug"
```

### 3. Pull Before Pushing

Always pull latest changes before pushing:

```bash
git pull origin main
git push origin main
```

### 4. Don't Commit Sensitive Data

Never commit:
- `.env` files with passwords
- API keys
- Database credentials
- Personal information

Use `.gitignore` to exclude these files.

### 5. Review Changes Before Committing

```bash
git diff  # See all changes
git diff path/to/file.ts  # See changes to specific file
```

### 6. Use Branches for Major Features (Optional)

For large features, create a branch:

```bash
# Create and switch to new branch
git checkout -b feature/new-loan-product

# Make changes and commit
git add .
git commit -m "Implement new loan product feature"

# Push branch to GitHub
git push origin feature/new-loan-product

# On GitHub, create Pull Request to merge into main
# After review, merge and delete branch
```

---

## Troubleshooting

### "fatal: not a git repository"

You're not in the project directory. Navigate to it:

```bash
cd C:\Users\Elitebook\OneDrive\Desktop\minetsacco-main
```

### "error: src refspec main does not match any"

You haven't made any commits yet. Do this:

```bash
git add .
git commit -m "Initial commit"
git branch -M main
git push -u origin main
```

### "Permission denied (publickey)"

GitHub authentication failed. Try:

```bash
# Use HTTPS instead of SSH
git remote set-url origin https://github.com/yourusername/minetsacco.git
git push origin main
```

### "Your branch is ahead of 'origin/main' by X commits"

You have commits not pushed yet. Push them:

```bash
git push origin main
```

### "Your branch is behind 'origin/main' by X commits"

Someone else pushed changes. Pull them:

```bash
git pull origin main
```

### "Changes not staged for commit"

You modified files but didn't stage them. Stage them:

```bash
git add .
git commit -m "Your message"
git push origin main
```

### "fatal: The current branch main has no upstream branch"

First push of a new branch. Use:

```bash
git push -u origin main
```

---

## Quick Reference

### Common Commands

```bash
# Check status
git status

# See changes
git diff

# Stage all changes
git add .

# Commit changes
git commit -m "Your message"

# Push to GitHub
git push origin main

# Pull latest changes
git pull origin main

# View commit history
git log

# Undo last commit (keep changes)
git reset --soft HEAD~1

# Undo last commit (discard changes)
git reset --hard HEAD~1

# Create new branch
git checkout -b branch-name

# Switch to branch
git checkout branch-name

# Delete branch
git branch -d branch-name
```

### Daily Workflow Summary

```bash
# Start of day
git pull origin main

# During day (repeat as needed)
git add .
git commit -m "Description of changes"
git push origin main

# End of day
git push origin main  # Make sure everything is pushed
```

---

## Next Steps

1. Follow "Initial Setup" section
2. Follow "First Push to GitHub" section
3. Your code is now on GitHub!
4. Use "Daily Workflow" for future updates

## Support

For more help:
- GitHub Docs: https://docs.github.com
- Git Tutorial: https://git-scm.com/book/en/v2
- GitHub Desktop (GUI alternative): https://desktop.github.com
