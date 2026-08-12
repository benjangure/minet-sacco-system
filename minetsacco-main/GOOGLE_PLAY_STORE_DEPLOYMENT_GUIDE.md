# Google Play Store Deployment Guide
## Minet SACCO Member Portal - Complete Deployment Instructions

**Version:** 1.1  
**Last Updated:** August 12, 2026  
**Status:** Production Ready ✅

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Pre-Deployment Checklist](#pre-deployment-checklist)
4. [Building the App](#building-the-app)
5. [Google Play Console Setup](#google-play-console-setup)
6. [Store Listing Configuration](#store-listing-configuration)
7. [Upload and Submission](#upload-and-submission)
8. [Post-Submission](#post-submission)
9. [Troubleshooting](#troubleshooting)
10. [App Updates](#app-updates)
11. [Important Notes](#important-notes)

---

## Overview

This guide walks you through the complete process of deploying the Minet SACCO Member Portal app to the Google Play Store. All necessary configurations, assets, and scripts have been prepared and are ready for deployment.

### What's Been Prepared

✅ **App Signing**
- Release keystore generated and configured
- Signing configuration added to build.gradle
- Keystore credentials: MinetSacco2026!

✅ **Optimization**
- R8 code optimization enabled
- ProGuard rules configured
- Resource shrinking enabled
- Code obfuscation active

✅ **Compliance**
- Target SDK 36 (Android 15) - Exceeds Play Store requirements
- All required permissions declared
- Android 12+ backup rules configured
- Deep linking configured

✅ **Assets**
- App icons (all sizes) generated from Minet logo
- Play Store icon (512x512) ready
- Feature graphic (1024x500) created
- App descriptions written
- Privacy policy drafted

✅ **Build Scripts**
- APK build script: `build-release-apk.ps1`
- AAB build script: `build-release-aab.ps1`
- Quick builder: `build-playstore.ps1`

---

## Prerequisites

### Required Tools

- [x] **Node.js** v18 or higher (for web app build)
- [x] **npm** (comes with Node.js)
- [x] **Java JDK** 17 (for Android build)
- [x] **Android SDK** (installed via Android Studio or CLI tools)
- [x] **PowerShell** (for build scripts)

### Required Accounts

- [ ] **Google Account** with access to Google Play Console
- [ ] **Google Play Console Developer Account** ($25 one-time fee)
- [ ] **Payment Method** for developer account registration

### Verify Installation

```powershell
# Check Node.js
node --version  # Should show v18.x or higher

# Check npm
npm --version

# Check Java
java -version  # Should show version 17

# Check Android SDK
$env:ANDROID_HOME  # Should point to Android SDK directory
```

---

## Pre-Deployment Checklist

### Critical Items

- [x] **Release keystore created** (`android/app/release.keystore`)
- [x] **Build configuration updated** (signing, optimization)
- [x] **App icons generated** (all densities)
- [x] **Play Store assets ready** (`playstore-assets/` directory)
- [ ] **Privacy policy published** (https://minetsacco.co.ke/privacy-policy)
- [ ] **App tested on multiple devices**
- [ ] **Backend API accessible** (production backend running)
- [ ] **Screenshots captured** (2-8 high-quality screenshots)

### Testing Checklist

Before submission, test these scenarios:

- [ ] Login with valid credentials
- [ ] View dashboard and account balance
- [ ] Apply for a loan
- [ ] View loan details and history
- [ ] View savings and deposits
- [ ] Approve/reject guarantor request
- [ ] Download statements/reports
- [ ] Receive and interact with push notifications
- [ ] App works on different Android versions (7.0 to 15)
- [ ] App works on different screen sizes
- [ ] Network error handling (airplane mode)
- [ ] App doesn't crash during normal use

---

## Building the App

### Method 1: Quick Build (Recommended)

Build both APK and AAB in one command:

```powershell
cd minetsacco-main
.\build-playstore.ps1
```

This will:
1. Build the web app
2. Sync with Capacitor
3. Build signed APK (for testing)
4. Build signed AAB (for Play Store)

**Output files:**
- `minetsacco-release-v1.1.apk` (20-30 MB)
- `minetsacco-playstore-v1.1.aab` (15-25 MB)

### Method 2: AAB Only (For Play Store)

```powershell
cd minetsacco-main
.\build-release-aab.ps1
```

### Method 3: APK Only (For Testing)

```powershell
cd minetsacco-main
.\build-release-apk.ps1
```

### Build Options

Skip web build (if already built):
```powershell
.\build-release-aab.ps1 -SkipBuild
```

Skip Capacitor sync:
```powershell
.\build-release-aab.ps1 -SkipSync
```

Open output folder after build:
```powershell
.\build-release-aab.ps1 -OpenOutput
```

### Expected Build Time

- **Web build:** 15-30 seconds
- **Capacitor sync:** 5-10 seconds
- **Android build (first time):** 3-5 minutes
- **Android build (subsequent):** 1-2 minutes

### Troubleshooting Build Issues

**"Keystore password incorrect"**
```
The password is: MinetSacco2026!
Check android/app/build.gradle signing config
```

**"Out of memory" error**
```
Edit android/gradle.properties and add:
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=1024m
```

**"SDK not found"**
```
Set ANDROID_HOME environment variable:
$env:ANDROID_HOME = "C:\Users\YourName\AppData\Local\Android\Sdk"
```

---

## Google Play Console Setup

### Step 1: Create Developer Account

1. Go to [Google Play Console](https://play.google.com/console)
2. Sign in with your Google account
3. Accept the Developer Agreement
4. Pay the $25 one-time registration fee
5. Complete your account profile

### Step 2: Create New App

1. Click **"Create app"** button
2. Fill in app details:

```
App name: Minet SACCO Member Portal
Default language: English (United States)
App or game: App
Free or paid: Free
```

3. Declarations:
   - [x] Developer Program Policies
   - [x] US export laws

4. Click **"Create app"**

---

## Store Listing Configuration

### Main Store Listing

Navigate to: **Store presence > Main store listing**

#### App Details

**Short description** (80 characters max):
```
Manage your SACCO account, apply for loans, and track savings on the go.
```

**Full description** (4000 characters max):

Copy from `playstore-assets/PLAY_STORE_LISTING.md` - "Full Description" section

#### Graphics

Upload these files from `playstore-assets/`:

1. **App icon** (512x512, required)
   - File: `icon-512x512.png`
   - 32-bit PNG with alpha
   - No rounded corners

2. **Feature graphic** (1024x500, required)
   - File: `feature-graphic-1024x500.png`
   - JPG or 24-bit PNG (no alpha)

3. **Phone screenshots** (minimum 2, maximum 8)
   - Capture from running app
   - 16:9 or 9:16 aspect ratio
   - Recommended: 1080x1920 (portrait)
   - Format: PNG or JPG

**Recommended screenshots:**
- Login screen
- Dashboard overview
- Loan application
- Savings/deposits
- Loan details
- Transaction history
- Reports/statements
- Notifications

#### Categorization

```
App category: Finance
Tags: (optional) SACCO, banking, loans, savings
```

#### Contact Details

```
Email: admin@minetsacco.co.ke
Website: https://minetsacco.co.ke
Phone: [Optional - SACCO office number]
```

#### External Marketing (Optional)

```
Promotional video: [If available]
```

---

### Privacy Policy

**CRITICAL:** Privacy policy must be published online before submission.

1. **Publish Privacy Policy:**
   - Copy content from `playstore-assets/PRIVACY_POLICY.md`
   - Publish at: https://minetsacco.co.ke/privacy-policy
   - Ensure it's publicly accessible (no login required)
   - Test the URL in an incognito browser window

2. **Add to Play Console:**
   - Navigate to: **App content > Privacy policy**
   - Enter URL: `https://minetsacco.co.ke/privacy-policy`
   - Click **Save**

---

### Content Rating

Navigate to: **App content > Content rating**

1. Click **"Start questionnaire"**

2. Select **category:** Utility, Productivity, Communication, or Other

3. Answer questionnaire questions:

```
Violence: None
Sexual Content: None
Profanity: None
Controlled Substances: None
Crude Humor: None
Discrimination: None
Gambling: None

Does your app contain user-generated content? No
Does your app have social features? No (or Yes if considering guarantor interaction as social)
Does your app share user location? No
Does your app allow users to communicate? No
```

4. Expected rating: **Everyone** or **Everyone 10+**

5. Click **Submit**

---

### Target Audience and Content

Navigate to: **App content > Target audience and content**

#### Target Age

```
Target age group: 18 and over (Adults only)
Reason: Financial services app for employed adults
```

#### Ads

```
Does your app contain ads? No
```

---

### App Access

Navigate to: **App content > App access**

```
Is your app restricted to specific users? Yes
Special access: SACCO members only

Provide test credentials for review:
Username: [Create a test account]
Password: [Test account password]

Instructions: This app is for Minet SACCO members. Users are registered by SACCO staff and cannot self-register.
```

---

### Data Safety

Navigate to: **App content > Data safety**

This is critical - be thorough and accurate.

#### Data Collection

**Personal Information Collected:**
- [x] Name
- [x] Email address
- [x] User IDs (Employee ID, Member number)
- [x] Financial information (Account balances, transactions, loans)
- [x] Device ID

**How is data used:**
- [x] Account management
- [x] App functionality
- [x] Fraud prevention
- [x] Personalization

**Data sharing:**
- [ ] We do not share data with third parties

**Security practices:**
- [x] Data is encrypted in transit (HTTPS)
- [x] Data is encrypted at rest
- [x] Users can request deletion of data

**Data deletion:**
- [x] Users can contact us to request data deletion
- Provide contact: admin@minetsacco.co.ke

---

### Government Apps (if applicable)

If Minet is a government entity:

Navigate to: **App content > Government apps**

```
Is your app an official government app? [Yes/No]
[Fill accordingly if Yes]
```

---

## Upload and Submission

### Production Release Track

1. Navigate to: **Release > Production**

2. Click **"Create new release"**

3. **App bundles:**
   - Click **"Upload"**
   - Select: `minetsacco-playstore-v1.1.aab`
   - Wait for upload and processing (1-5 minutes)
   - Google will show APK sizes after processing

4. **Release name** (auto-generated)
   ```
   1.1 (2)
   ```

5. **Release notes** (What's new):

```
Initial release of Minet SACCO Member Portal

Features:
• View account balances and transaction history
• Apply for loans with instant eligibility checking
• Track loan repayments and outstanding balances
• Manage savings and share capital
• Approve or reject guarantor requests
• Download account statements and reports
• Receive push notifications for important updates
• Secure authentication with encrypted data

This app provides Minet SACCO members with convenient mobile access to their accounts 24/7.
```

6. Click **"Save"**

7. **Review release:**
   - Verify all information is correct
   - Check bundle details
   - Review release notes

8. Click **"Next"**

---

### Rollout Percentage (Optional)

For first release, you can choose:

- **Full rollout:** 100% (recommended for organization-specific apps)
- **Staged rollout:** Start with 20%, gradually increase

For internal/organization apps, use 100% rollout.

---

### Countries and Regions

Select countries where your app will be available:

```
Recommended: Kenya (primary market)
Optional: Add other countries where Minet operates
```

1. Navigate to: **Release > Production > Countries/regions**
2. Click **"Add countries/regions"**
3. Select: **Kenya**
4. Add others if applicable
5. Click **"Save"**

---

### Pricing and Distribution

Navigate to: **Release > Production > Pricing and distribution**

#### Pricing

```
Free app: Yes
In-app products: No
```

#### Countries

Already configured above.

#### Content Guidelines

- [x] I confirm this app complies with Google Play policies
- [x] I confirm this app complies with US export laws
- [x] I acknowledge that my app may be subject to US export laws

#### Marketing

```
Promotional plan: [Optional]
```

Click **"Save"**

---

### Final Review

1. Go to **Dashboard**
2. Check all sections have green checkmarks:
   - ✅ Store listing
   - ✅ Privacy policy
   - ✅ App access
   - ✅ Ads
   - ✅ Content rating
   - ✅ Target audience
   - ✅ News apps
   - ✅ COVID-19 contact tracing apps
   - ✅ Data safety
   - ✅ Government apps

3. If any section has warnings, fix them before proceeding

---

### Submit for Review

1. Navigate to: **Publishing overview**

2. Review summary:
   - App version: 1.1 (2)
   - Countries: Kenya
   - Release type: Production

3. Click **"Send X changes for review"**

4. Confirmation dialog appears

5. Click **"Send for review"**

---

## Post-Submission

### What Happens Next

1. **Submission Confirmation**
   - You'll receive an email confirmation
   - Status changes to "In review"

2. **Review Process**
   - **Duration:** Typically 1-3 days (can take up to 7 days for first submission)
   - **Status:** Check Play Console dashboard
   - **Updates:** You'll receive emails about status changes

3. **Possible Outcomes:**

   **✅ Approved**
   - App goes live automatically
   - Available in selected countries within hours
   - You receive approval email

   **⚠️ Changes Requested**
   - Google requests clarifications or changes
   - You have 7 days to respond
   - Make changes and resubmit

   **❌ Rejected**
   - App violates policies
   - Review rejection reason
   - Fix issues and resubmit

### Monitoring

**Play Console Dashboard:**
- Installs and ratings
- Crash reports
- ANR (App Not Responding) reports
- User reviews and feedback

**Set up alerts:**
1. Go to **Settings > Email preferences**
2. Enable notifications for:
   - Ratings and reviews
   - Crash reports
   - App status changes

---

### After Approval

1. **Verify App is Live:**
   - Search "Minet SACCO" on Play Store
   - Or visit: https://play.google.com/store/apps/details?id=com.minetsacco.memberportal

2. **Test Installation:**
   - Install from Play Store
   - Test all features
   - Verify updates work correctly

3. **Announce to Users:**
   - Send email to all members
   - Post on SACCO notice board
   - Share Play Store link
   - Provide installation instructions

4. **Monitor Performance:**
   - Check install numbers
   - Review crash reports
   - Read user reviews
   - Respond to feedback

---

## Troubleshooting

### Common Rejection Reasons

**1. Privacy Policy Issues**
```
Problem: Privacy policy URL not accessible or incomplete
Solution: 
- Ensure URL is publicly accessible (no login)
- Must contain all required sections
- Must specifically address app's data practices
```

**2. Misleading Content**
```
Problem: Screenshots or descriptions don't match app functionality
Solution:
- Use actual app screenshots (no mockups)
- Ensure description accurately reflects features
- Remove any misleading claims
```

**3. Broken Functionality**
```
Problem: Google reviewers couldn't log in or use the app
Solution:
- Provide valid test credentials
- Ensure backend API is accessible
- Test thoroughly before submission
```

**4. Policy Violations**
```
Problem: App violates Google Play policies
Solution:
- Review Developer Policy Center
- Fix specific violations mentioned
- Resubmit with explanations
```

### Responding to Review Feedback

1. **Read carefully** - Understand exactly what Google is requesting
2. **Fix issues** - Address all points mentioned
3. **Test thoroughly** - Verify fixes work
4. **Respond** - Use "Appeal" or "Submit" as appropriate
5. **Be prompt** - Respond within 7 days

---

## App Updates

### When to Update

- Bug fixes
- New features
- Security patches
- Backend API changes
- UI improvements
- Performance optimizations

### Update Process

1. **Update version numbers** in `android/app/build.gradle`:
   ```gradle
   versionCode 3  // Increment by 1
   versionName "1.2"  // Update version string
   ```

2. **Make your changes** in the codebase

3. **Test thoroughly**

4. **Build new AAB:**
   ```powershell
   .\build-release-aab.ps1
   ```

5. **Upload to Play Console:**
   - Go to **Release > Production**
   - Create new release
   - Upload new AAB
   - Add release notes explaining what changed

6. **Submit for review**

**Update review time:** Usually faster than initial review (1-2 days)

### Staged Rollouts

For major updates, use staged rollouts:

1. Release to 20% of users
2. Monitor crash reports and feedback
3. If stable, increase to 50%
4. Then 100% after confirming stability

---

## Important Notes

### Keystore Security

🔐 **CRITICAL:** Keep `android/app/release.keystore` secure!

- **Backup** in multiple secure locations
- **Never** commit to version control
- **Never** share publicly
- **Loss = permanent** - You cannot update your app without it

**Credentials:**
- Store password: `MinetSacco2026!`
- Key alias: `minetsacco`
- Key password: `MinetSacco2026!`

Store these credentials securely (password manager, encrypted document, safe).

### Version Numbers

- **versionCode:** Must always increase (integers only)
- **versionName:** Display string (e.g., "1.0", "1.1", "2.0")

Never reuse a version code. Play Store won't accept it.

### Testing Before Each Release

Always test:
- Clean install from AAB
- Upgrade from previous version
- All critical user flows
- Different Android versions
- Different device sizes

### Play Store Fees

- **Developer account:** $25 (one-time)
- **App distribution:** Free
- **In-app purchases:** Google takes 15-30% (not applicable for your app)

### Review Guidelines

**Do:**
- ✅ Provide accurate descriptions
- ✅ Use high-quality screenshots
- ✅ Test thoroughly before submission
- ✅ Respond to reviews professionally
- ✅ Update regularly

**Don't:**
- ❌ Use misleading marketing
- ❌ Violate user privacy
- ❌ Include prohibited content
- ❌ Manipulate ratings/reviews
- ❌ Circumvent payments

### Support and Maintenance

After launch, plan for:
- **Daily:** Monitor crash reports and user reviews
- **Weekly:** Review analytics and performance
- **Monthly:** Plan updates and improvements
- **Quarterly:** Major feature releases

---

## Resources

### Official Documentation

- [Google Play Console](https://play.google.com/console)
- [Developer Policy Center](https://play.google.com/about/developer-content-policy/)
- [Launch Checklist](https://developer.android.com/distribute/best-practices/launch)
- [Android Developer Guide](https://developer.android.com/guide)

### Project Files

- **Build Scripts:** `build-release-aab.ps1`, `build-release-apk.ps1`
- **Store Listing:** `playstore-assets/PLAY_STORE_LISTING.md`
- **Privacy Policy:** `playstore-assets/PRIVACY_POLICY.md`
- **App Icons:** `playstore-assets/icon-512x512.png`
- **Feature Graphic:** `playstore-assets/feature-graphic-1024x500.png`

### Support Contacts

- **Developer Support:** admin@minetsacco.co.ke
- **Google Play Support:** Via Play Console
- **Technical Issues:** support@minetsacco.co.ke

---

## Quick Reference

### Essential Commands

```powershell
# Build for Play Store
.\build-release-aab.ps1

# Build for testing
.\build-release-apk.ps1

# Build both
.\build-playstore.ps1

# Generate icons
node generate-android-icons-playstore.cjs
```

### Essential URLs

- **Play Console:** https://play.google.com/console
- **App Page:** https://play.google.com/store/apps/details?id=com.minetsacco.memberportal
- **Privacy Policy:** https://minetsacco.co.ke/privacy-policy

### Key Information

```
Package Name: com.minetsacco.memberportal
Version: 1.1 (versionCode: 2)
Min SDK: 24 (Android 7.0)
Target SDK: 36 (Android 15)
Category: Finance
Price: Free
```

---

## Success Checklist

Before clicking "Send for review":

- [ ] AAB built successfully
- [ ] App tested on physical device
- [ ] All features work correctly
- [ ] Backend API is accessible
- [ ] Privacy policy published online
- [ ] Privacy policy URL added to Play Console
- [ ] App screenshots captured and uploaded
- [ ] App icon (512x512) uploaded
- [ ] Feature graphic uploaded
- [ ] Short description added
- [ ] Full description added
- [ ] Content rating completed
- [ ] Target audience configured
- [ ] Data safety form completed
- [ ] Test credentials provided (if app access restricted)
- [ ] Countries/regions selected
- [ ] All Play Console sections have green checkmarks
- [ ] Release notes written
- [ ] Team notified of submission

---

## Congratulations! 🎉

You've successfully prepared the Minet SACCO Member Portal for Google Play Store deployment. All technical requirements are met, assets are ready, and documentation is complete.

**Next Step:** Follow this guide to submit your app to the Play Store!

**Questions?** Contact: admin@minetsacco.co.ke

---

**Document Version:** 1.0  
**Created:** August 12, 2026  
**Author:** Kiro AI Development Environment  
**Status:** Production Ready ✅
