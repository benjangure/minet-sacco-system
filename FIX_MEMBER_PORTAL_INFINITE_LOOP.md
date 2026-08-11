# Fix Member Portal Infinite Loop

## Issue
Member portal was stuck in an infinite redirect loop with error:
```
Maximum update depth exceeded. This can happen when a component calls setState inside useEffect
```

## Root Cause
The `MemberLogin.tsx` component was using `navigate()` from React Router to redirect authenticated users. This kept the component in the React lifecycle, causing it to remount and check the session again, creating an infinite loop.

## Solution
Changed from `navigate()` to `window.location.href` for redirects. This performs a full page navigation, breaking the React Router cycle.

### Code Change
```typescript
// OLD - causes infinite loop
navigate('/member/dashboard', { replace: true });

// NEW - breaks the loop
window.location.href = '/member/dashboard';
```

## How to Fix Your Browser Right Now

### Step 1: Clear the Infinite Loop
**Open DevTools Console** (F12) and run:
```javascript
localStorage.clear();
sessionStorage.clear();
location.reload();
```

### Step 2: Or Manually Clear
1. Press F12 to open DevTools
2. Go to "Application" tab
3. Under "Storage" on the left, click "Local Storage"
4. Right-click and select "Clear"
5. Do the same for "Session Storage"
6. Close DevTools and refresh the page (Ctrl+R)

### Step 3: Hard Refresh
After clearing storage, do a hard refresh:
- Windows: `Ctrl + Shift + R` or `Ctrl + F5`
- Mac: `Cmd + Shift + R`

## Prevention
The fix ensures this won't happen again by using proper navigation methods that break the React re-render cycle.

## Files Modified
- `minetsacco-main/src/pages/MemberLogin.tsx`

## Date
August 10, 2026
