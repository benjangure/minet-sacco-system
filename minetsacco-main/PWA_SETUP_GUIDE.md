# PWA Setup Guide - Minet SACCO Member Portal

## Quick Start (3 Steps)

```bash
# 1. Generate asset placeholders
npm run pwa:generate

# 2. Start app
npm run dev

# 3. Test on phone
# Visit: http://<YOUR_PC_IP>:3000/member
# Android: Tap "Install app" in Chrome
# iPhone: Share → Add to Home Screen
```

**Note**: The generated PNG files are placeholders. For production, replace them with actual branded icons and splash screens.

## Overview
The Minet SACCO Member Portal is now configured as a Progressive Web App (PWA). This allows members to install it on their mobile devices like a native app, with offline support and a splash screen.

## What's Been Done

### 1. PNG Icons Generated
- **icon-192.png** - For home screen and app launcher (192x192px)
- **icon-512.png** - For splash screens and larger displays (512x512px)
- Both icons feature the "MS" (Minet SACCO) logo with proper maskable design

### 2. Splash Screens Created
- **splash-1125x2436.png** - iPhone splash screen
- **splash-1080x1920.png** - Android splash screen
- Features: Red background, white MS logo, "MINET SACCO" title, "Member Portal" subtitle, loading spinner

### 3. Manifest Updated
- Changed from SVG to PNG icons for better Android compatibility
- Added `"purpose": "any maskable"` for proper icon rendering without browser badge
- Updated `start_url` to `/member/dashboard` for direct dashboard access
- Configured screenshots for app store listings

### 4. Service Worker Enhanced
- Network-first strategy for optimal performance
- Offline support for critical member pages:
  - `/member/dashboard`
  - `/member/apply-loan`
  - `/member/account-statement`
  - `/member/loan-balances`
- Separate caching for API calls with fallback
- Automatic cache cleanup on updates

### 5. Mobile Optimization
- Responsive text sizing (xs on mobile, sm-lg on desktop)
- Optimized spacing and padding for small screens
- Better icon sizing for mobile devices
- Improved card layouts for mobile viewing

## How to Generate PWA Assets

If you need to regenerate the PNG icons and splash screens:

```bash
npm run pwa:generate
```

This creates placeholder PNG files in `public/`:
- `icon-192.png` (192x192px)
- `icon-512.png` (512x512px)
- `splash-1125x2436.png` (iPhone)
- `splash-1080x1920.png` (Android)

**For Production**: Replace these placeholder PNGs with actual branded images using any image editor (Photoshop, GIMP, Figma, etc.).

## Testing the PWA

### On Desktop (Chrome DevTools)
1. Open your member portal: `http://localhost:3000/member/dashboard`
2. Press `F12` to open DevTools
3. Go to **Application** tab → **Manifest**
4. Verify all icons load correctly and no warnings appear
5. Check **Service Workers** tab - should show "activated and running"

### On Android Phone
1. Connect phone to same WiFi as your PC
2. Visit: `http://<YOUR_PC_IP>:3000/member`
3. Chrome should show "Install app" prompt
4. Tap to install
5. App appears on home screen with your icon
6. Opens with splash screen, then loads dashboard

### On iPhone
1. Open Safari on iPhone
2. Visit: `http://<YOUR_PC_IP>:3000/member`
3. Tap Share → Add to Home Screen
4. Customize name (optional) and tap Add
5. App appears on home screen
6. Opens with splash screen on first launch

## Offline Support

The service worker caches critical pages. When offline:
- Dashboard loads from cache
- Loan application page loads from cache
- Account statement loads from cache
- API calls show "Offline - data may be outdated" message

## Deployment Checklist

Before going live, ensure:

- [ ] HTTPS is enabled (required for PWA)
- [ ] All PNG icons are in `public/` folder
- [ ] Splash screen images are in `public/` folder
- [ ] `manifest.json` is correctly linked in `index.html`
- [ ] Service worker registration works (check browser console)
- [ ] Test on actual Android and iOS devices
- [ ] Verify "Install app" prompt appears on Chrome/Android

## File Structure

```
minetsacco-main/
├── public/
│   ├── icon-192.png              ← App icon (small)
│   ├── icon-512.png              ← App icon (large)
│   ├── splash-1125x2436.png      ← iPhone splash
│   ├── splash-1080x1920.png      ← Android splash
│   ├── manifest.json             ← PWA configuration
│   └── service-worker.js         ← Offline support
├── index.html                    ← PWA meta tags
├── generate-pwa-assets.js        ← Asset generator
└── src/
    └── pages/
        └── MemberDashboard.tsx   ← Mobile optimized
```

## Troubleshooting

### "Install app" prompt not showing
- Check that manifest.json is valid (DevTools → Application → Manifest)
- Verify service worker is registered and active
- Ensure site is served over HTTPS (or localhost)
- Clear browser cache and reload

### Icons showing with browser badge
- Ensure `"purpose": "any maskable"` is set in manifest
- Icons must be PNG, not SVG
- Regenerate icons with `node generate-pwa-assets.js`

### Offline pages not loading
- Check service worker in DevTools (Application → Service Workers)
- Verify critical pages are in the CRITICAL_PAGES array
- Clear cache: DevTools → Application → Cache Storage → Delete all

### Splash screen not showing
- On Android: Automatic from manifest background_color and icon
- On iPhone: Requires `<link rel="apple-touch-startup-image">` in HTML
- Verify splash images exist in `public/` folder

## Next Steps

1. **Generate Assets**: Run `node generate-pwa-assets.js`
2. **Test Locally**: Visit on phone via WiFi
3. **Deploy**: Push to production with HTTPS
4. **Share Link**: Members can install from any browser

## Resources

- [PWA Documentation](https://web.dev/progressive-web-apps/)
- [Manifest Specification](https://www.w3.org/TR/appmanifest/)
- [Service Worker Guide](https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API)
- [Maskable Icons](https://web.dev/maskable-icon/)
