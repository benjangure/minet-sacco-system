/**
 * Force Logo Update Script
 * 
 * This script forces the PWA to update its cached logo and icons.
 * Run this from the browser console on the installed PWA to immediately update the logo.
 * 
 * Usage:
 * 1. Open the installed PWA
 * 2. Open DevTools (F12)
 * 3. Go to Console tab
 * 4. Paste this entire script and press Enter
 */

(async function forceLogoUpdate() {
  console.log('🔄 Starting logo update...');
  
  // Check if service worker is supported
  if (!('serviceWorker' in navigator)) {
    console.error('❌ Service Worker not supported in this browser');
    return;
  }
  
  try {
    // Get the service worker registration
    const registration = await navigator.serviceWorker.getRegistration();
    
    if (!registration) {
      console.error('❌ No service worker registered');
      return;
    }
    
    console.log('✅ Service Worker found');
    
    // Clear all caches
    const cacheNames = await caches.keys();
    console.log(`🗑️  Clearing ${cacheNames.length} caches...`);
    
    await Promise.all(
      cacheNames.map(cacheName => {
        console.log(`  - Deleting cache: ${cacheName}`);
        return caches.delete(cacheName);
      })
    );
    
    console.log('✅ All caches cleared');
    
    // Force service worker update
    console.log('🔄 Checking for service worker updates...');
    await registration.update();
    console.log('✅ Service worker update checked');
    
    // Send message to service worker to skip waiting
    if (registration.waiting) {
      console.log('📨 Telling new service worker to activate immediately...');
      registration.waiting.postMessage({ type: 'SKIP_WAITING' });
    }
    
    // Pre-fetch logo files with cache bypass
    const logoFiles = [
      '/Minet-Logo1.png',
      '/icon-192.png',
      '/icon-512.png',
      '/splash-1080x1920.png',
      '/splash-1125x2436.png',
      '/manifest.json'
    ];
    
    console.log('📥 Fetching updated logo files...');
    await Promise.all(
      logoFiles.map(async (file) => {
        try {
          const response = await fetch(file, { 
            cache: 'reload',
            headers: { 'Cache-Control': 'no-cache' }
          });
          if (response.ok) {
            console.log(`  ✅ Fetched: ${file}`);
          } else {
            console.log(`  ⚠️  Failed to fetch: ${file} (${response.status})`);
          }
        } catch (error) {
          console.log(`  ⚠️  Error fetching: ${file}`, error);
        }
      })
    );
    
    console.log('✅ Logo files fetched');
    
    // Wait a moment for service worker to activate
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    console.log('');
    console.log('✨ Logo update complete!');
    console.log('🔄 Reloading page to show new logo...');
    console.log('');
    
    // Reload the page to show new logo
    setTimeout(() => {
      window.location.reload(true);
    }, 1000);
    
  } catch (error) {
    console.error('❌ Error during logo update:', error);
  }
})();

// Export for manual use
window.forceLogoUpdate = async function() {
  await forceLogoUpdate();
};

console.log('💡 Tip: You can run this anytime by calling: forceLogoUpdate()');
