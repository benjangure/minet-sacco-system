import { createRoot } from "react-dom/client";
import App from "./App.tsx";
import "./index.css";
import { SplashScreen } from '@capacitor/splash-screen';

// Suppress known benign console warnings
const originalError = console.error;
console.error = (...args) => {
  const message = args.join(' ');
  // Suppress permissions policy violation from sockjs-client (websocket library)
  if (message.includes('Violation') && message.includes('Permissions policy') && message.includes('unload')) {
    return;
  }
  originalError.apply(console, args);
};

// Show splash immediately
SplashScreen.show({
  showDuration: 3000,
  autoHide: true,
  backgroundColor: '#ef4444',
  spinnerStyle: 'large',
  spinnerColor: '#ffffff',
});

// Service Worker update handler - forces logo cache refresh
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/service-worker.js')
      .then(registration => {
        console.log('Service Worker registered:', registration);
        
        // Check for updates every 5 minutes
        setInterval(() => {
          registration.update();
        }, 5 * 60 * 1000);
        
        // Handle service worker updates
        registration.addEventListener('updatefound', () => {
          const newWorker = registration.installing;
          if (newWorker) {
            newWorker.addEventListener('statechange', () => {
              if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                // New service worker installed - logo cache will be updated
                console.log('New service worker installed - logo updated');
                
                // Optionally show update notification to user
                if (window.confirm('New version available with updated logo! Reload to see changes?')) {
                  newWorker.postMessage({ type: 'SKIP_WAITING' });
                  window.location.reload();
                }
              }
            });
          }
        });
        
        // Listen for controller change (new service worker activated)
        navigator.serviceWorker.addEventListener('controllerchange', () => {
          console.log('Service worker updated - reloading to show new logo');
          window.location.reload();
        });
      })
      .catch(error => {
        console.error('Service Worker registration failed:', error);
      });
  });
}

createRoot(document.getElementById("root")!).render(<App />);
