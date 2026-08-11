/**
 * Install Prompt Component
 * Displays a banner prompting users to install the PWA on their device
 */

import { useState, useEffect } from 'react';
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
  const [platform, setPlatform] = useState<'ios' | 'android' | 'desktop' | 'unknown'>('unknown');

  useEffect(() => {
    // Check if app is already installed
    const checkInstalled = () => {
      // Check if running in standalone mode (installed)
      if (window.matchMedia('(display-mode: standalone)').matches) {
        setIsInstalled(true);
        return true;
      }
      
      // Check navigator.standalone for iOS
      if ((window.navigator as any).standalone === true) {
        setIsInstalled(true);
        return true;
      }
      
      return false;
    };

    // Detect platform
    const detectPlatform = () => {
      const userAgent = window.navigator.userAgent.toLowerCase();
      const isIOS = /iphone|ipad|ipod/.test(userAgent);
      const isAndroid = /android/.test(userAgent);
      const isDesktop = !isIOS && !isAndroid;

      if (isIOS) {
        setPlatform('ios');
      } else if (isAndroid) {
        setPlatform('android');
      } else if (isDesktop) {
        setPlatform('desktop');
      }
    };

    // Check if already installed
    if (checkInstalled()) {
      console.log('[Install Prompt] App is already installed');
      return;
    }

    detectPlatform();

    // Check if prompt was previously dismissed
    const promptDismissed = localStorage.getItem('pwa-install-prompt-dismissed');
    const dismissedAt = promptDismissed ? parseInt(promptDismissed) : 0;
    const daysSinceDismissed = (Date.now() - dismissedAt) / (1000 * 60 * 60 * 24);

    // Don't show prompt if dismissed within last 7 days
    if (promptDismissed && daysSinceDismissed < 7) {
      // Silently skip - don't log to console
      return;
    }

    // Listen for the beforeinstallprompt event
    const handleBeforeInstallPrompt = (e: Event) => {
      console.log('[Install Prompt] beforeinstallprompt event fired');
      
      // Prevent the mini-infobar from appearing on mobile
      e.preventDefault();
      
      // Store the event so it can be triggered later
      setDeferredPrompt(e as BeforeInstallPromptEvent);
      
      // Show the custom install prompt
      setShowPrompt(true);
    };

    // Listen for app installed event
    const handleAppInstalled = () => {
      console.log('[Install Prompt] App was installed');
      setIsInstalled(true);
      setShowPrompt(false);
      setDeferredPrompt(null);
      
      // Clear dismissed flag
      localStorage.removeItem('pwa-install-prompt-dismissed');
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    window.addEventListener('appinstalled', handleAppInstalled);

    // For iOS, show manual install instructions after a delay
    if (platform === 'ios' && !isInstalled) {
      const timer = setTimeout(() => {
        setShowPrompt(true);
      }, 3000); // Show after 3 seconds
      
      return () => clearTimeout(timer);
    }

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
      window.removeEventListener('appinstalled', handleAppInstalled);
    };
  }, [platform]);

  const handleInstallClick = async () => {
    if (!deferredPrompt) {
      console.log('[Install Prompt] No deferred prompt available');
      return;
    }

    console.log('[Install Prompt] Showing install prompt');

    // Show the install prompt
    deferredPrompt.prompt();

    // Wait for the user to respond to the prompt
    const { outcome } = await deferredPrompt.userChoice;
    
    console.log(`[Install Prompt] User response: ${outcome}`);

    if (outcome === 'accepted') {
      console.log('[Install Prompt] User accepted the install prompt');
      setShowPrompt(false);
    } else {
      console.log('[Install Prompt] User dismissed the install prompt');
      handleDismiss();
    }

    // Clear the deferred prompt
    setDeferredPrompt(null);
  };

  const handleDismiss = () => {
    console.log('[Install Prompt] User dismissed the prompt');
    setShowPrompt(false);
    
    // Store dismissal timestamp
    localStorage.setItem('pwa-install-prompt-dismissed', Date.now().toString());
  };

  // Don't show if already installed or prompt is hidden
  if (isInstalled || !showPrompt) {
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

  // Android/Desktop installation with native prompt
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
                <p className="text-sm text-white/90 mb-3">
                  Install our app for faster access, offline support, and push notifications
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
