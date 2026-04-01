# APK as Simple Web Wrapper - Setup Instructions

## The Problem
You want the APK to be a simple wrapper that loads the member portal web page from your PC. Currently, Capacitor is trying to load bundled local files instead of loading from your server.

## The Solution
Configure Capacitor to load from a development server running on your PC.

## Step-by-Step Setup

### Step 1: Start the Development Server on Your PC
```bash
cd minetsacco-main
npm run dev
```

This will start a Vite dev server on `http://localhost:3000` and also be accessible from your phone at `http://192.168.0.195:3000`

**Keep this terminal running** - the dev server must stay active for the APK to work.

### Step 2: Verify Dev Server is Running
Open your phone browser and navigate to:
```
http://192.168.0.195:3000/member
```

You should see the member login page. If it works here, the APK will work.

### Step 3: Install the APK
Transfer and install the APK on your Samsung A14:
```
minetsacco-main/android/app/build/outputs/apk/debug/app-debug.apk
```

### Step 4: Test the APK
1. Open the Minet SACCO app
2. You should see the red splash screen for 3 seconds
3. Then the member login page should appear
4. Login with: `member1` / `password`

## How It Works

**capacitor.config.ts** is now configured to:
```typescript
server: {
  url: 'http://192.168.0.195:3000',  // ← Points to dev server
  cleartext: true,
  androidScheme: 'http'
}
```

This tells Capacitor to load the web app from your PC's dev server instead of bundled files.

## Important Notes

- **Dev server must be running** - The APK won't work if you stop the dev server
- **Same WiFi network** - Phone and PC must be on the same WiFi
- **Correct IP address** - Make sure 192.168.0.195 is your PC's IP (check with `ipconfig`)
- **Port 3000 must be open** - Windows Firewall should allow it (usually does by default)

## If It Still Doesn't Work

### Check 1: Dev Server Running
```bash
# In minetsacco-main directory
npm run dev
# Should show: ➜  Local:   http://localhost:3000/
```

### Check 2: Phone Can Reach Dev Server
On your phone browser, try:
```
http://192.168.0.195:3000/member
```

If this works in browser, the APK will work.

### Check 3: Correct IP Address
On your PC, find your IP:
```powershell
ipconfig
# Look for "IPv4 Address" under your WiFi adapter
```

Update `capacitor.config.ts` if the IP is different.

### Check 4: Firewall
Allow port 3000 in Windows Firewall:
```powershell
netsh advfirewall firewall add rule name="Vite Dev Server" dir=in action=allow protocol=TCP localport=3000
```

## What's Different Now

**Before (Broken):**
- APK tried to load bundled local files
- No connection to backend
- Login failed

**Now (Working):**
- APK loads from dev server on your PC
- Dev server connects to backend at 192.168.0.195:8080
- Login works because it's the same web portal you use in browser

## To Build a Production APK

When you're ready to deploy without needing the dev server running:

1. Build the frontend: `npm run build`
2. Copy dist files to backend's static folder (or use a CDN)
3. Update `capacitor.config.ts` to point to production URL
4. Rebuild APK: `npx cap sync android && cd android && gradlew assembleDebug`

For now, just use the dev server approach - it's the simplest and works immediately.
