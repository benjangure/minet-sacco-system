/**
 * Generate Android App Icons for Play Store from Minet Logo
 * 
 * This script generates all required icon sizes for Android app deployment:
 * - Launcher icons (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
 * - Play Store icon (512x512)
 * - Feature graphic (1024x500)
 * - Adaptive icons (foreground and background)
 */

const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

// Source logo
const SOURCE_LOGO = path.join(__dirname, 'public', 'Minet-Logo1.png');

// Output directories
const MIPMAP_BASE = path.join(__dirname, 'android', 'app', 'src', 'main', 'res');
const PLAY_STORE_DIR = path.join(__dirname, 'playstore-assets');

// Icon sizes for different densities
const LAUNCHER_ICON_SIZES = {
  'mipmap-mdpi': 48,
  'mipmap-hdpi': 72,
  'mipmap-xhdpi': 96,
  'mipmap-xxhdpi': 144,
  'mipmap-xxxhdpi': 192
};

const ADAPTIVE_ICON_SIZES = {
  'mipmap-mdpi': 108,
  'mipmap-hdpi': 162,
  'mipmap-xhdpi': 216,
  'mipmap-xxhdpi': 324,
  'mipmap-xxxhdpi': 432
};

// Ensure directories exist
function ensureDir(dirPath) {
  if (!fs.existsSync(dirPath)) {
    fs.mkdirSync(dirPath, { recursive: true });
  }
}

// Generate launcher icons (legacy)
async function generateLauncherIcons() {
  console.log('\n📱 Generating launcher icons...');
  
  for (const [density, size] of Object.entries(LAUNCHER_ICON_SIZES)) {
    const outputDir = path.join(MIPMAP_BASE, density);
    ensureDir(outputDir);
    
    const outputPath = path.join(outputDir, 'ic_launcher.png');
    
    await sharp(SOURCE_LOGO)
      .resize(size, size, {
        fit: 'contain',
        background: { r: 255, g: 255, b: 255, alpha: 0 }
      })
      .png()
      .toFile(outputPath);
    
    console.log(`  ✓ ${density}/ic_launcher.png (${size}x${size})`);
  }
}

// Generate round launcher icons
async function generateRoundIcons() {
  console.log('\n⭕ Generating round launcher icons...');
  
  for (const [density, size] of Object.entries(LAUNCHER_ICON_SIZES)) {
    const outputDir = path.join(MIPMAP_BASE, density);
    ensureDir(outputDir);
    
    const outputPath = path.join(outputDir, 'ic_launcher_round.png');
    
    // Create circular mask
    const circle = Buffer.from(
      `<svg><circle cx="${size/2}" cy="${size/2}" r="${size/2}" /></svg>`
    );
    
    await sharp(SOURCE_LOGO)
      .resize(size, size, {
        fit: 'contain',
        background: { r: 255, g: 255, b: 255, alpha: 0 }
      })
      .composite([{
        input: circle,
        blend: 'dest-in'
      }])
      .png()
      .toFile(outputPath);
    
    console.log(`  ✓ ${density}/ic_launcher_round.png (${size}x${size})`);
  }
}

// Generate adaptive icon foregrounds (Android 8.0+)
async function generateAdaptiveIcons() {
  console.log('\n🎨 Generating adaptive icon foregrounds...');
  
  for (const [density, size] of Object.entries(ADAPTIVE_ICON_SIZES)) {
    const outputDir = path.join(MIPMAP_BASE, density);
    ensureDir(outputDir);
    
    const outputPath = path.join(outputDir, 'ic_launcher_foreground.png');
    
    await sharp(SOURCE_LOGO)
      .resize(size, size, {
        fit: 'contain',
        background: { r: 0, g: 0, b: 0, alpha: 0 }
      })
      .png()
      .toFile(outputPath);
    
    console.log(`  ✓ ${density}/ic_launcher_foreground.png (${size}x${size})`);
  }
}

// Generate adaptive icon backgrounds
async function generateAdaptiveBackgrounds() {
  console.log('\n🎨 Generating adaptive icon backgrounds...');
  
  // White background for clean look
  for (const [density, size] of Object.entries(ADAPTIVE_ICON_SIZES)) {
    const outputDir = path.join(MIPMAP_BASE, density);
    ensureDir(outputDir);
    
    const outputPath = path.join(outputDir, 'ic_launcher_background.png');
    
    await sharp({
      create: {
        width: size,
        height: size,
        channels: 4,
        background: { r: 255, g: 255, b: 255, alpha: 255 }
      }
    })
    .png()
    .toFile(outputPath);
    
    console.log(`  ✓ ${density}/ic_launcher_background.png (${size}x${size})`);
  }
}

// Generate Play Store icon (512x512)
async function generatePlayStoreIcon() {
  console.log('\n🏪 Generating Play Store icon...');
  
  ensureDir(PLAY_STORE_DIR);
  const outputPath = path.join(PLAY_STORE_DIR, 'icon-512x512.png');
  
  await sharp(SOURCE_LOGO)
    .resize(512, 512, {
      fit: 'contain',
      background: { r: 255, g: 255, b: 255, alpha: 255 }
    })
    .png()
    .toFile(outputPath);
  
  console.log(`  ✓ Play Store icon (512x512) saved to: ${outputPath}`);
}

// Generate feature graphic (1024x500)
async function generateFeatureGraphic() {
  console.log('\n🖼️  Generating feature graphic...');
  
  ensureDir(PLAY_STORE_DIR);
  const outputPath = path.join(PLAY_STORE_DIR, 'feature-graphic-1024x500.png');
  
  // Create feature graphic with logo centered on branded background
  await sharp({
    create: {
      width: 1024,
      height: 500,
      channels: 4,
      background: { r: 0, g: 102, b: 204, alpha: 255 } // Blue background
    }
  })
  .composite([{
    input: await sharp(SOURCE_LOGO)
      .resize(400, 400, {
        fit: 'contain',
        background: { r: 0, g: 0, b: 0, alpha: 0 }
      })
      .toBuffer(),
    gravity: 'center'
  }])
  .png()
  .toFile(outputPath);
  
  console.log(`  ✓ Feature graphic (1024x500) saved to: ${outputPath}`);
}

// Generate screenshot template
async function generateScreenshotTemplate() {
  console.log('\n📸 Generating screenshot template...');
  
  ensureDir(PLAY_STORE_DIR);
  const outputPath = path.join(PLAY_STORE_DIR, 'screenshot-template-info.txt');
  
  const info = `
PLAY STORE SCREENSHOTS REQUIREMENTS
====================================

For optimal Play Store listing, you need:

1. PHONE SCREENSHOTS (Required)
   - Minimum: 2 screenshots
   - Maximum: 8 screenshots
   - Dimensions: 16:9 or 9:16 aspect ratio
   - Recommended: 1080 x 1920 pixels (portrait) or 1920 x 1080 pixels (landscape)
   - Format: PNG or JPEG

2. 7-INCH TABLET SCREENSHOTS (Optional)
   - Minimum: 1 screenshot
   - Maximum: 8 screenshots
   - Dimensions: Same as phone

3. 10-INCH TABLET SCREENSHOTS (Optional)
   - Minimum: 1 screenshot
   - Maximum: 8 screenshots
   - Dimensions: Same as phone

HOW TO CAPTURE SCREENSHOTS:
---------------------------
1. Run your app on an emulator or physical device
2. Navigate to key screens (login, dashboard, loans, etc.)
3. Take screenshots using:
   - Android Studio: View > Tool Windows > Running Devices > Screenshot
   - Device: Power + Volume Down
   - ADB: adb shell screencap -p /sdcard/screenshot.png

RECOMMENDED SCREENS TO CAPTURE:
-------------------------------
1. Login screen
2. Dashboard (member overview)
3. Loans list/details
4. Savings/deposits
5. Payment history
6. Profile/settings

TIPS:
-----
- Use demo/test data for privacy
- Ensure UI looks clean and professional
- Show actual app functionality
- Include diverse content
- No personal/sensitive information

Generated icons are ready in: ${PLAY_STORE_DIR}
`;
  
  fs.writeFileSync(outputPath, info.trim());
  console.log(`  ✓ Screenshot guide saved to: ${outputPath}`);
}

// Main function
async function main() {
  console.log('🚀 Minet SACCO Android Icon Generator for Play Store');
  console.log('====================================================');
  
  // Check if source logo exists
  if (!fs.existsSync(SOURCE_LOGO)) {
    console.error(`❌ Error: Source logo not found at ${SOURCE_LOGO}`);
    process.exit(1);
  }
  
  try {
    await generateLauncherIcons();
    await generateRoundIcons();
    await generateAdaptiveIcons();
    await generateAdaptiveBackgrounds();
    await generatePlayStoreIcon();
    await generateFeatureGraphic();
    await generateScreenshotTemplate();
    
    console.log('\n✅ All icons generated successfully!');
    console.log('\n📦 Play Store assets saved to:', PLAY_STORE_DIR);
    console.log('📱 Android app icons updated in:', MIPMAP_BASE);
    console.log('\n🎯 Next steps:');
    console.log('   1. Review generated icons in playstore-assets/');
    console.log('   2. Capture app screenshots following the guide');
    console.log('   3. Build signed APK/AAB');
    console.log('   4. Upload to Google Play Console');
    
  } catch (error) {
    console.error('❌ Error generating icons:', error);
    process.exit(1);
  }
}

// Run the script
main();
