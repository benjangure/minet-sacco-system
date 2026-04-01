# GitHub Quick Start - 5 Minutes

Fast track to get your code on GitHub.

## Prerequisites

- Git installed: https://git-scm.com/download/win
- GitHub account: https://github.com

## Step 1: Configure Git (One Time)

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

## Step 2: Create GitHub Repository

1. Go to https://github.com/new
2. Name: `minetsacco`
3. Description: "SACCO Management System"
4. Click "Create repository"
5. Copy the URL shown (looks like `https://github.com/yourusername/minetsacco.git`)

## Step 3: Initialize Git in Your Project

```bash
cd C:\Users\Elitebook\OneDrive\Desktop\minetsacco-main
git init
git remote add origin https://github.com/yourusername/minetsacco.git
```

Replace the URL with your actual GitHub repository URL.

## Step 4: Create .gitignore

Create a file named `.gitignore` in your project root with:

```
node_modules/
backend/target/
.gradle/
.idea/
.env
dist/
build/
android/app/build/
*.apk
.vscode/
.DS_Store
*.log
.kiro/
APK_*.md
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

## Step 5: Push to GitHub

```bash
git add .
git commit -m "Initial commit: Minet SACCO system"
git branch -M main
git push -u origin main
```

**Done!** Your code is now on GitHub.

---

## After First Push - Daily Workflow

### Before starting work:
```bash
git pull origin main
```

### After making changes:
```bash
git add .
git commit -m "Description of what you changed"
git push origin main
```

That's it! Repeat the "After making changes" section every time you want to update GitHub.

---

## Common Scenarios

### Update a specific file:
```bash
git add path/to/file.ts
git commit -m "Update file description"
git push origin main
```

### Update documentation:
```bash
git add *.md
git commit -m "Update documentation"
git push origin main
```

### Update backend:
```bash
git add backend/
git commit -m "Backend changes description"
git push origin main
```

### Update frontend:
```bash
git add minetsacco-main/
git commit -m "Frontend changes description"
git push origin main
```

---

## Verify It Worked

1. Go to https://github.com/yourusername/minetsacco
2. You should see all your files
3. Click on a file to view it
4. Check the commit history

---

## Need Help?

See `GITHUB_WORKFLOW_GUIDE.md` for detailed instructions and troubleshooting.
