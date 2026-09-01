/**
 * Install Prompt Component
 * Displays a banner prompting users to install the PWA on their device
 * Only shown on member portal routes (/member/*)
 */

import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { Capacitor } from '@capacitor/core';
import { Button } from '@/components/ui/button';
import { X, Download, Smartphone } from 'lucide-react';
import { Card } from '@/components/ui/card';

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
}

export const InstallPrompt = () => {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [showPrompt, setShowPrompt] = useState(false);
  const [isInstalled, setIsInstalled] = useState(false);
  const location = useLocation();

  // Detect platform once at render time (not as state — avoids async mismatch)
  const userAgent = window.navigator.userAgent.toLowerCase();
  const platform: 'ios' | 'android' | 'desktop' = /iphone|ipad|ipod/.test(userAgent)
    ? 'ios'
    : /android/.test(userAgent)
    ? 'android'
    : 'desktop';

  // Only show on member portal routes
  const isMemberPortal = location.pathname.startsWith('/member');

  useEffect(() => {
    // Never show inside the native Capacitor app — it's already installed
    if (Capacitor.isNativePlatform()) {
      return;
    }

    // Check if app is already installed as PWA
    const isAlreadyInstalled =
      window.matchMedia('(display-mode: standalone)').matches ||
      (window.navigator as any).standalone === true;

    if (isAlreadyInstalled) {
      setIsInstalled(true);
      return;
    }

    // Check if prompt was previously dismissed within 7 days
    const promptDismissed = localStorage.getItem('pwa-install-prompt-dismissed');
    const dismissedAt = promptDismissed ? parseInt(promptDismissed) : 0;
    const daysSinceDismissed = (Date.now() - dismissedAt) / (1000 * 60 * 60 * 24);
    if (promptDismissed && daysSinceDismissed < 7) {
      return;
    }

    // iOS: show manual install instructions after a short delay
    if (platform === 'ios') {
      const timer = setTimeout(() => {
        setShowPrompt(true);
      }, 3000);
      return () => clearTimeout(timer);
    }

    // Android: show the APK download banner after a short delay.
    // We don't rely on beforeinstallprompt here — that's for PWA install.
    // The Android branch shows a native APK download prompt instead.
    if (platform === 'android') {
      const timer = setTimeout(() => {
        setShowPrompt(true);
      }, 3000);
      return () => clearTimeout(timer);
    }

    // Desktop: listen for browser's beforeinstallprompt (fires on HTTPS only).
    // Also show a manual-instructions banner after a delay as a fallback for
    // HTTP deployments where beforeinstallprompt never fires (e.g. LAN IP).
    let fallbackTimer: ReturnType<typeof setTimeout> | null = null;

    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
      // Native prompt available — cancel the fallback timer, use the real prompt
      if (fallbackTimer) clearTimeout(fallbackTimer);
      setShowPrompt(true);
    };

    const handleAppInstalled = () => {
      setIsInstalled(true);
      setShowPrompt(false);
      setDeferredPrompt(null);
      localStorage.removeItem('pwa-install-prompt-dismissed');
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    window.addEventListener('appinstalled', handleAppInstalled);

    // Fallback: show manual instructions banner after 4s if native prompt hasn't fired
    fallbackTimer = setTimeout(() => {
      setShowPrompt(true);
    }, 4000);

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
      window.removeEventListener('appinstalled', handleAppInstalled);
      if (fallbackTimer) clearTimeout(fallbackTimer);
    };
  }, []);

  const handleInstallClick = async () => {
    if (deferredPrompt) {
      // Native browser install prompt available (HTTPS) — use it
      deferredPrompt.prompt();
      const { outcome } = await deferredPrompt.userChoice;
      if (outcome === 'accepted') {
        setShowPrompt(false);
      } else {
        handleDismiss();
      }
      setDeferredPrompt(null);
    }
    // No else — when there's no native prompt the banner already shows
    // step-by-step instructions inline, so no alert needed
  };

  const handleDismiss = () => {
    setShowPrompt(false);
    localStorage.setItem('pwa-install-prompt-dismissed', Date.now().toString());
  };

  // Don't show if already installed, prompt is hidden, or not on member portal
  if (isInstalled || !showPrompt || !isMemberPortal) {
    return null;
  }

  // iOS-specific manual installation instructions
  if (platform === 'ios') {
    return (
      <div className="fixed bottom-4 left-4 right-4 z-50 animate-in slide-in-from-bottom duration-500">
        <Card className="bg-gradient-to-r from-red-500 to-red-600 text-white shadow-lg border-0">
          <div className="p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-start gap-3 flex-1">
                <div className="bg-white/20 p-2 rounded-lg">
                  <Smartphone className="h-6 w-6" />
                </div>
                <div className="flex-1">
                  <h3 className="font-semibold text-lg mb-1">
                    Install Minet SACCO App
                  </h3>
                  <p className="text-sm text-white/90 mb-3">
                    Add to your home screen for quick access and notifications
                  </p>
                  <div className="text-sm space-y-1 bg-white/10 p-3 rounded-lg">
                    <p className="font-medium">How to install on iOS:</p>
                    <ol className="list-decimal list-inside space-y-1 text-white/90">
                      <li>Tap the Share button (
                        <svg className="inline w-4 h-4 mx-1" viewBox="0 0 24 24" fill="currentColor">
                          <path d="M16 5l-1.42 1.42-1.59-1.59V16h-1.98V4.83L9.42 6.42 8 5l4-4 4 4zm4 5v11c0 1.1-.9 2-2 2H6c-1.11 0-2-.9-2-2V10c0-1.11.89-2 2-2h3v2H6v11h12V10h-3V8h3c1.1 0 2 .89 2 2z"/>
                        </svg>
                        ) at the bottom of your screen
                      </li>
                      <li>Scroll down and tap "Add to Home Screen"</li>
                      <li>Tap "Add" in the top right corner</li>
                    </ol>
                  </div>
                </div>
              </div>
              <Button
                variant="ghost"
                size="icon"
                className="text-white hover:bg-white/20"
                onClick={handleDismiss}
              >
                <X className="h-5 w-5" />
              </Button>
            </div>
          </div>
        </Card>
      </div>
    );
  }

  // Android/Desktop installation
  // Android: Download APK
  // Desktop: Install PWA
  if (platform === 'android') {
    return (
      <div className="fixed bottom-4 left-4 right-4 md:left-auto md:right-4 md:max-w-md z-50 animate-in slide-in-from-bottom duration-500">
        <Card className="bg-gradient-to-r from-red-500 to-red-600 text-white shadow-lg border-0">
          <div className="p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-start gap-3 flex-1">
                <div className="bg-white/20 p-2 rounded-lg flex-shrink-0">
                  <Smartphone className="h-6 w-6" />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-lg mb-1">
                    Install Mobile App
                  </h3>
                  <p className="text-sm text-white/90 mb-2">
                    To install the Android app, open this link in Chrome and allow the download when prompted:
                  </p>
                  <div className="text-sm bg-white/10 p-2 rounded-lg mb-3 break-all font-mono">
                    http://10.39.60.15:8090/minet-sacco.apk
                  </div>
                  <div className="text-xs text-white/80 mb-3 bg-white/10 p-2 rounded-lg">
                    <p className="font-medium mb-1">If Chrome blocks the download:</p>
                    <ol className="list-decimal list-inside space-y-1">
                      <li>Tap the download notification → <strong>Download anyway</strong></li>
                      <li>Or open Chrome menu → Downloads → tap the file</li>
                      <li>Enable <strong>Install unknown apps</strong> in Android Settings if prompted</li>
                    </ol>
                  </div>
                  <div className="flex gap-2">
                    <a
                      href="/minet-sacco.apk"
                      download="minet-sacco.apk"
                      className="inline-flex"
                    >
                      <Button
                        className="bg-white text-red-600 hover:bg-white/90 font-semibold"
                        size="sm"
                      >
                        <Download className="h-4 w-4 mr-2" />
                        Download App
                      </Button>
                    </a>
                    <Button
                      variant="ghost"
                      onClick={handleDismiss}
                      className="text-white hover:bg-white/20"
                      size="sm"
                    >
                      Maybe Later
                    </Button>
                  </div>
                </div>
              </div>
              <Button
                variant="ghost"
                size="icon"
                className="text-white hover:bg-white/20 flex-shrink-0"
                onClick={handleDismiss}
              >
                <X className="h-5 w-5" />
              </Button>
            </div>
          </div>
        </Card>
      </div>
    );
  }

  // Desktop: Install PWA
  return (
    <div className="fixed bottom-4 left-4 right-4 md:left-auto md:right-4 md:max-w-md z-50 animate-in slide-in-from-bottom duration-500">
      <Card className="bg-gradient-to-r from-red-500 to-red-600 text-white shadow-lg border-0">
        <div className="p-4">
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-start gap-3 flex-1">
              <div className="bg-white/20 p-2 rounded-lg flex-shrink-0">
                <Download className="h-6 w-6" />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="font-semibold text-lg mb-1">
                  Install Minet SACCO
                </h3>
                {deferredPrompt ? (
                  // Native install available (HTTPS) — single click install
                  <>
                    <p className="text-sm text-white/90 mb-3">
                      Install the app for faster access, offline support, and notifications
                    </p>
                    <div className="flex gap-2">
                      <Button
                        onClick={handleInstallClick}
                        className="bg-white text-red-600 hover:bg-white/90 font-semibold"
                        size="sm"
                      >
                        <Download className="h-4 w-4 mr-2" />
                        Install Now
                      </Button>
                      <Button
                        variant="ghost"
                        onClick={handleDismiss}
                        className="text-white hover:bg-white/20"
                        size="sm"
                      >
                        Maybe Later
                      </Button>
                    </div>
                  </>
                ) : (
                  // No native prompt (HTTP deployment) — show manual steps inline
                  <>
                    <p className="text-sm text-white/90 mb-2">
                      Install this app directly from your browser:
                    </p>
                    <div className="text-sm space-y-1 bg-white/10 p-3 rounded-lg mb-3">
                      <p className="font-medium mb-1">Chrome / Edge:</p>
                      <ol className="list-decimal list-inside space-y-1 text-white/90">
                        <li>Click the <strong>⊕</strong> icon in the address bar, OR</li>
                        <li>Open the browser menu <strong>⋮</strong></li>
                        <li>Select <strong>"Install Minet SACCO"</strong></li>
                      </ol>
                    </div>
                    <Button
                      variant="ghost"
                      onClick={handleDismiss}
                      className="text-white hover:bg-white/20"
                      size="sm"
                    >
                      Dismiss
                    </Button>
                  </>
                )}
              </div>
            </div>
            <Button
              variant="ghost"
              size="icon"
              className="text-white hover:bg-white/20 flex-shrink-0"
              onClick={handleDismiss}
            >
              <X className="h-5 w-5" />
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default InstallPrompt;
