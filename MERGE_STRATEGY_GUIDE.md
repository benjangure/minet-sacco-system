# Git Merge Strategy Guide
## Merging Developer's Updates with Your Local Changes

## Situation
- Previous developer had local changes but didn't push to GitHub
- You cloned the repo (got old code)
- Developer has now pushed their updates
- You've made your own updates locally
- Need to merge both sets of changes

---

## ⚠️ IMPORTANT: Backup First!

**Before doing ANYTHING, create a backup:**

```powershell
# Go to parent directory
cd C:\Users\Lenovo\Desktop\minet-sacco

# Create a complete backup
Copy-Item -Path "minet-sacco-system" -Destination "minet-sacco-system-BACKUP-$(Get-Date -Format 'yyyy-MM-dd-HHmm')" -Recurse

# Verify backup exists
Get-ChildItem | Where-Object {$_.Name -like "minet-sacco-system-BACKUP*"}
```

This creates a timestamped backup you can return to if anything goes wrong.

---

## Option 1: Safe Merge Strategy (RECOMMENDED)

This approach preserves all your work and handles conflicts carefully.

### Step 1: Check Current Status
```powershell
cd C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system

# See what you've changed
git status

# See your commits
git log --oneline -10
```

### Step 2: Stash Your Changes Temporarily
```powershell
# Save all your uncommitted changes
git stash push -m "My treasurer loan changes before merge"

# Verify stash saved
git stash list
```

### Step 3: Fetch Developer's Updates
```powershell
# Get latest from GitHub without merging
git fetch origin

# See what's different
git log HEAD..origin/main --oneline
```

### Step 4: Pull Developer's Changes
```powershell
# Pull and merge developer's updates
git pull origin main
```

### Step 5: Apply Your Changes Back
```powershell
# Reapply your stashed changes
git stash pop
```

**If conflicts occur**, Git will tell you which files have conflicts. See "Handling Conflicts" section below.

### Step 6: Review and Test
```powershell
# Check status - will show conflicts if any
git status

# If no conflicts, test the application
# Backend: mvn clean install
# Frontend: npm run build

# If everything works, commit
git add .
git commit -m "Merged developer updates with treasurer loan delete/edit features"
git push origin main
```

---

## Option 2: Create a Merge Branch (SAFEST)

Keep everything organized with branches.

### Step 1: Create Branch for Your Work
```powershell
cd C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system

# Create branch with your changes
git checkout -b treasurer-loan-features

# Commit all your changes
git add .
git commit -m "Add treasurer loan delete/edit functionality"
```

### Step 2: Go Back to Main and Update
```powershell
# Switch back to main branch
git checkout main

# Pull developer's updates
git pull origin main
```

### Step 3: Merge Your Branch
```powershell
# Merge your features into updated main
git merge treasurer-loan-features

# If conflicts, resolve them (see below)
```

### Step 4: Push Everything
```powershell
# Push merged result
git push origin main

# Optionally keep your branch for reference
git push origin treasurer-loan-features
```

---

## Option 3: Interactive Rebase (ADVANCED)

For a cleaner history, but more complex.

```powershell
# Fetch updates
git fetch origin

# Rebase your changes on top of developer's
git rebase origin/main

# If conflicts, resolve and continue:
# git add <resolved-files>
# git rebase --continue

# Push (may need force)
git push origin main --force-with-lease
```

---

## Handling Merge Conflicts

### When Conflicts Occur

Git will show something like:
```
Auto-merging backend/src/main/java/com/minet/sacco/controller/LoanController.java
CONFLICT (content): Merge conflict in LoanController.java
Automatic merge failed; fix conflicts and then commit the result.
```

### Step 1: See Which Files Have Conflicts
```powershell
git status
```

Output shows:
```
Unmerged paths:
  both modified:   backend/src/main/java/com/minet/sacco/controller/LoanController.java
  both modified:   minetsacco-main/src/pages/Loans.tsx
```

### Step 2: Open Conflicted Files

Conflicts look like this:
```java
<<<<<<< HEAD (Your changes)
@DeleteMapping("/{loanId}")
@PreAuthorize("hasRole('ROLE_TREASURER')")
public ResponseEntity<ApiResponse<String>> deleteLoan(@PathVariable Long loanId) {
    // Your implementation
}
=======
@DeleteMapping("/{loanId}")
public ResponseEntity<ApiResponse<String>> removeLoan(@PathVariable Long loanId) {
    // Developer's implementation
}
>>>>>>> origin/main (Developer's changes)
```

### Step 3: Resolve Each Conflict

**Choose one of:**

**A. Keep Your Changes:**
```java
@DeleteMapping("/{loanId}")
@PreAuthorize("hasRole('ROLE_TREASURER')")
public ResponseEntity<ApiResponse<String>> deleteLoan(@PathVariable Long loanId) {
    // Your implementation
}
```

**B. Keep Developer's Changes:**
```java
@DeleteMapping("/{loanId}")
public ResponseEntity<ApiResponse<String>> removeLoan(@PathVariable Long loanId) {
    // Developer's implementation
}
```

**C. Combine Both (Best approach):**
```java
@DeleteMapping("/{loanId}")
@PreAuthorize("hasRole('ROLE_TREASURER')")
public ResponseEntity<ApiResponse<String>> deleteLoan(@PathVariable Long loanId) {
    // Combine logic from both versions
    // Use your authorization but developer's other improvements
}
```

### Step 4: Mark as Resolved
```powershell
# After fixing conflicts in a file
git add backend/src/main/java/com/minet/sacco/controller/LoanController.java
git add minetsacco-main/src/pages/Loans.tsx

# Continue the merge
git commit -m "Merged developer updates with treasurer features, resolved conflicts"
```

---

## Your Specific Changes to Track

Based on our work, these are YOUR changes that need to be preserved:

### Backend Changes:
1. **LoanController.java**
   - Added: `deleteLoan()` method (DELETE endpoint)
   - Added: `updateLoanFinancials()` method (PUT endpoint)
   - Both with `@PreAuthorize("hasRole('ROLE_TREASURER')")`

2. **LoanService.java**
   - Added: `deleteLoan()` method (business logic)
   - Added: `updateLoanFinancials()` method (business logic)

3. **application.properties**
   - Changed database URL from `sacco_db` to `minetsacco`
   - Changed username from `root` to `minetsacco`

4. **CorsConfig.java**
   - Added: `10.39.60.*:*` to allowed origins

### Frontend Changes:
1. **23 files** - Changed from hardcoded `http://localhost:9090` to `getApiBaseUrl()`
   - Index.tsx, Loans.tsx, Members.tsx, etc.

2. **Loans.tsx**
   - Added: Edit button with modal
   - Added: Delete button with confirmation
   - Added: Enhanced error messages for delete
   - Added: State management for edit/delete

---

## Testing After Merge

### 1. Backend Testing
```powershell
cd backend
mvn clean install -DskipTests
```

**If build fails**, conflicts weren't resolved properly.

### 2. Frontend Testing
```powershell
cd minetsacco-main
npm install
npm run build
```

**If build fails**, check for syntax errors in resolved conflicts.

### 3. Integration Testing
- Start backend: `java -jar target\minet-sacco-backend-0.0.1-SNAPSHOT.jar`
- Deploy frontend to IIS
- Test treasurer login
- Test loan delete/edit functionality
- Verify network access works

---

## If Things Go Wrong

### Option A: Abort the Merge
```powershell
# Stop merge and go back to before
git merge --abort

# Or if in rebase
git rebase --abort
```

### Option B: Restore from Backup
```powershell
# Go to parent directory
cd C:\Users\Lenovo\Desktop\minet-sacco

# Delete current folder
Remove-Item -Path "minet-sacco-system" -Recurse -Force

# Restore from backup
Copy-Item -Path "minet-sacco-system-BACKUP-2025-01-23-1600" -Destination "minet-sacco-system" -Recurse
```

### Option C: Reset to Remote
```powershell
# Discard ALL local changes and get developer's version
# ⚠️ WARNING: This loses your work!
git fetch origin
git reset --hard origin/main
```

---

## Communication Strategy

### Talk to the Previous Developer

**Ask them:**
1. What major changes did they make?
2. Which files did they modify most?
3. Are there any breaking changes?
4. Can they review your merge?

**Share with them:**
1. List of your changes (backend DELETE/PUT endpoints, frontend fixes)
2. Ask if your changes conflict with theirs
3. Request they test after merge

---

## Best Practice Going Forward

### 1. Always Pull Before Working
```powershell
git pull origin main
```

### 2. Commit Frequently
```powershell
git add .
git commit -m "Descriptive message"
```

### 3. Push Regularly
```powershell
git push origin main
```

### 4. Use Branches for Big Features
```powershell
git checkout -b feature-name
# Make changes
git commit -am "Feature work"
git push origin feature-name
# Create Pull Request on GitHub
```

### 5. Document Your Changes
Keep a `CHANGELOG.md` with:
- What you changed
- Why you changed it
- When you changed it

---

## Quick Decision Tree

```
Do you have uncommitted changes?
├─ YES → git stash
└─ NO  → Continue

Are your changes committed?
├─ NO  → git commit -am "My changes"
└─ YES → Continue

git pull origin main

Did conflicts occur?
├─ YES → Resolve conflicts manually
│        git add <files>
│        git commit
└─ NO  → Test and push

Does everything work?
├─ YES → git push origin main
└─ NO  → Fix issues, test again
```

---

## Commands Reference Sheet

```powershell
# Backup
Copy-Item -Path "minet-sacco-system" -Destination "minet-sacco-system-BACKUP" -Recurse

# Status
git status
git log --oneline -10

# Save work temporarily
git stash push -m "Description"
git stash list
git stash pop

# Update from remote
git fetch origin
git pull origin main

# Branch strategy
git checkout -b my-feature
git checkout main
git merge my-feature

# Conflict resolution
git add <resolved-file>
git commit -m "Resolved conflicts"

# Abort merge
git merge --abort

# Push changes
git push origin main
```

---

## Recommended Approach for You

Given your situation, I recommend **Option 1 (Safe Merge Strategy)**:

1. ✅ Backup everything first
2. ✅ Commit your changes
3. ✅ Pull developer's updates
4. ✅ Resolve any conflicts carefully
5. ✅ Test thoroughly
6. ✅ Push merged result

This preserves all work and gives you full control over conflict resolution.
