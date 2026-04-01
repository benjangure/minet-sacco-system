# App Version Management - Force Logout on Updates

## Overview
The app now uses a version system to automatically clear old sessions when the app is updated. This ensures users see the login page after app restarts or deployments.

## How It Works

### Version Checking
When the app starts, it checks if the stored app version matches the current app version:
- **Match:** Session is restored (user stays logged in)
- **Mismatch:** Session is cleared (user sees login page)

### Location
**File:** `minetsacco-main/src/contexts/AuthContext.tsx`

```typescript
const APP_VERSION = "1.0.0"; // Change this to force logout
```

## When to Update the Version

### Development
- After making significant changes to authentication
- After changing session storage format
- After dev server restarts (automatic, no action needed)

### Production
- After deploying new features
- After security updates
- After fixing bugs that affect authentication

## How to Update

### Step 1: Increment Version
Edit `minetsacco-main/src/contexts/AuthContext.tsx`:

```typescript
// Before
const APP_VERSION = "1.0.0";

// After
const APP_VERSION = "1.0.1";
```

### Step 2: Rebuild and Deploy
```bash
cd minetsacco-main
npm run build
# Deploy the build
```

### Step 3: All Users Automatically Logged Out
- When users access the app after deployment
- They will see the login page
- They need to log in again
- New session is created with new version

## Version Numbering

Use semantic versioning:
- **Major.Minor.Patch** (e.g., 1.0.0)
- Increment patch for bug fixes: 1.0.1
- Increment minor for new features: 1.1.0
- Increment major for breaking changes: 2.0.0

## Examples

### Example 1: Bug Fix
```typescript
// Before
const APP_VERSION = "1.0.0";

// After
const APP_VERSION = "1.0.1";
```

### Example 2: New Feature
```typescript
// Before
const APP_VERSION = "1.0.1";

// After
const APP_VERSION = "1.1.0";
```

### Example 3: Major Update
```typescript
// Before
const APP_VERSION = "1.9.9";

// After
const APP_VERSION = "2.0.0";
```

## Testing Version Update

### Local Testing
1. Login to app
2. Navigate to different page
3. Open browser console
4. Run: `localStorage.setItem("appVersion", "0.0.0")`
5. Refresh page
6. Verify: You see login page (session was cleared)

### Production Testing
1. Deploy new version with updated APP_VERSION
2. Users access the app
3. Verify: Users see login page
4. Users log in
5. Verify: Users can access the app normally

## Troubleshooting

### Users Still See Old Session
**Cause:** Browser cache not cleared
**Solution:** 
- Hard refresh (Ctrl+Shift+R or Cmd+Shift+R)
- Clear browser cache
- Verify APP_VERSION was incremented

### Users Can't Log In After Update
**Cause:** Backend session validation issue
**Solution:**
- Check backend logs
- Verify token generation is working
- Check token expiration settings

### Version Mismatch Errors
**Cause:** localStorage corruption
**Solution:**
- Clear browser localStorage
- Hard refresh
- Log in again

## Best Practices

✅ **Always increment version on deployment**
✅ **Use semantic versioning**
✅ **Document version changes**
✅ **Test version update locally first**
✅ **Notify users of forced logout if needed**
✅ **Keep version history in git**

## Version History

Track your version updates:

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2024-03-23 | Initial release |
| 1.0.1 | 2024-03-24 | Fixed navigation state issue |
| 1.1.0 | 2024-03-25 | Added bulk processing features |

## Automatic Version Management

The version system is automatic:
- No manual session clearing needed
- No user intervention required
- Works across all browsers
- Works across all devices

## Security Notes

✅ **Secure:** Sessions are cleared on version mismatch
✅ **Safe:** Old tokens are invalidated
✅ **Clean:** No stale data persists
✅ **Reliable:** Works even if localStorage is corrupted

## Support

If you need to force logout all users immediately:
1. Increment APP_VERSION
2. Deploy the change
3. All users will be logged out on next access
4. Users will need to log in again

This is useful for:
- Security incidents
- Major bug fixes
- System maintenance
- Policy changes
