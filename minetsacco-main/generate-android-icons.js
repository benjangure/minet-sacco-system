#!/usr/bin/env node

/**
 * Generate Android app icons from logo.png
 * This script converts the logo to all required Android icon sizes
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import sharp from 'sharp';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const logoPath = path.join(__dirname, 'src/assets/images/logo.png');
const resDir = path.join(__dirname, 'android/app/src/main/res');

// Icon sizes for different densities
const sizes = {
  'mipmap-mdpi': 48,
  'mipmap-hdpi': 72,
  'mipmap-xhdpi': 96,
  'mipmap-xxhdpi': 144,
  'mipmap-xxxhdpi': 192
};

async function generateIcons() {
  try {
    // Check if logo exists
    if (!fs.existsSync(logoPath)) {
      console.error(`❌ Logo not found at: ${logoPath}`);
      process.exit(1);
    }

    console.log('🎨 Generating Android app icons from logo...');
    console.log(`📁 Source: ${logoPath}`);

    // Generate icons for each density
    for (const [dir, size] of Object.entries(sizes)) {
      const outputDir = path.join(resDir, dir);
      
      // Create directory if it doesn't exist
      if (!fs.existsSync(outputDir)) {
        fs.mkdirSync(outputDir, { recursive: true });
      }

      const outputPath = path.join(outputDir, 'ic_launcher_foreground_logo.png');

      // Generate icon
      await sharp(logoPath)
        .resize(size, size, {
          fit: 'contain',
          background: { r: 255, g: 255, b: 255, alpha: 0 }
        })
        .png()
        .toFile(outputPath);

      console.log(`✅ Generated ${dir}/ic_launcher_foreground_logo.png (${size}x${size})`);
    }

    console.log('\n✅ All Android icons generated successfully!');
    console.log('\n📝 Next steps:');
    console.log('1. Run: npm run build');
    console.log('2. Run: npx cap sync android');
    console.log('3. Run: cd android && ./gradlew assembleRelease');
    console.log('4. APK will be at: android/app/build/outputs/apk/release/app-release.apk');

  } catch (error) {
    console.error('❌ Error generating icons:', error.message);
    process.exit(1);
  }
}

generateIcons();
