#!/usr/bin/env node

/**
 * Create placeholder PWA assets
 * For production, replace these with actual PNG files
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const publicDir = path.join(__dirname, 'public');

// Create public directory if it doesn't exist
if (!fs.existsSync(publicDir)) {
  fs.mkdirSync(publicDir, { recursive: true });
}

// Minimal valid PNG (1x1 red pixel) - base64 encoded
const minimalPNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==',
  'base64'
);

try {
  console.log('Creating PWA asset placeholders...');

  // Create icon files
  fs.writeFileSync(path.join(publicDir, 'icon-192.png'), minimalPNG);
  console.log('✓ icon-192.png created');

  fs.writeFileSync(path.join(publicDir, 'icon-512.png'), minimalPNG);
  console.log('✓ icon-512.png created');

  // Create splash screen files
  fs.writeFileSync(path.join(publicDir, 'splash-1125x2436.png'), minimalPNG);
  console.log('✓ splash-1125x2436.png created');

  fs.writeFileSync(path.join(publicDir, 'splash-1080x1920.png'), minimalPNG);
  console.log('✓ splash-1080x1920.png created');

  console.log('\n✅ PWA asset placeholders created!');
  console.log('\n⚠️  NOTE: These are placeholder images.');
  console.log('For production, replace with actual PNG files:');
  console.log('  - icon-192.png (192x192px)');
  console.log('  - icon-512.png (512x512px)');
  console.log('  - splash-1125x2436.png (iPhone)');
  console.log('  - splash-1080x1920.png (Android)');
} catch (error) {
  console.error('❌ Error creating PWA assets:', error.message);
  process.exit(1);
}
