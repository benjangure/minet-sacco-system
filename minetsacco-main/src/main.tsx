import { createRoot } from "react-dom/client";
import App from "./App.tsx";
import "./index.css";
import { SplashScreen } from '@capacitor/splash-screen';
import { Capacitor } from '@capacitor/core';

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

// Production: silence debug logging (request URLs, payloads) in the browser console
if (import.meta.env.PROD) {
  const noop = () => {};
  console.log = noop;
  console.debug = noop;
  console.info = noop;
}

// Show splash immediately
SplashScreen.show({
  showDuration: 3000,
  autoHide: true,
});

// Service worker (web/PWA only - the native Android shell bundles its own assets)
if ('serviceWorker' in navigator && !Capacitor.isNativePlatform()) {
  window.addEventListener('load', () => {
    const hadController = !!navigator.serviceWorker.controller;
    navigator.serviceWorker.register('/service-worker.js')
      .then(registration => {
        // Check for updates every 15 minutes
        setInterval(() => registration.update().catch(() => {}), 15 * 60 * 1000);
      })
      .catch(error => {
        console.error('Service Worker registration failed:', error);
      });

    // Reload once when a NEW worker takes over an already-controlled page (not on first install)
    let reloaded = false;
    navigator.serviceWorker.addEventListener('controllerchange', () => {
      if (!hadController || reloaded) return;
      reloaded = true;
      window.location.reload();
    });
  });
}

createRoot(document.getElementById("root")!).render(<App />);
