import { useState, useEffect } from 'react';
import { X, Download } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

export default function InstallPrompt() {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [showPrompt, setShowPrompt] = useState(false);
  const [isLocalNetwork, setIsLocalNetwork] = useState(false);

  useEffect(() => {
    // Check if on local network (not HTTPS)
    if (window.location.protocol !== 'https:') {
      setIsLocalNetwork(true);
      setShowPrompt(true);
    }

    const handler = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
      setShowPrompt(true);
    };

    window.addEventListener('beforeinstallprompt', handler);
    return () => window.removeEventListener('beforeinstallprompt', handler);
  }, []);

  const handleInstall = async () => {
    if (deferredPrompt) {
      deferredPrompt.prompt();
      const { outcome } = await deferredPrompt.userChoice;
      
      if (outcome === 'accepted') {
        setShowPrompt(false);
        setDeferredPrompt(null);
      }
    }
  };

  const handleManualInstall = () => {
    alert(
      'To add this app to your home screen:\n\n' +
      '1. Tap the menu (⋮) in the top right\n' +
      '2. Tap "Add to home screen"\n' +
      '3. Confirm the app name\n' +
      '4. The app will appear on your home screen'
    );
  };

  if (!showPrompt) return null;

  return (
    <div className="fixed bottom-4 left-4 right-4 bg-primary text-white rounded-lg shadow-lg p-4 z-50 max-w-sm mx-auto">
      <div className="flex items-start gap-3">
        <Download className="h-5 w-5 flex-shrink-0 mt-1" />
        <div className="flex-1">
          <h3 className="font-semibold mb-1">Install Minet SACCO</h3>
          <p className="text-sm text-white/90 mb-3">Add the app to your home screen for quick access</p>
          <div className="flex gap-2">
            {deferredPrompt ? (
              <>
                <Button
                  size="sm"
                  onClick={handleInstall}
                  className="bg-white text-primary hover:bg-white/90"
                >
                  Install
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => setShowPrompt(false)}
                  className="text-white hover:bg-white/20"
                >
                  Later
                </Button>
              </>
            ) : (
              <>
                <Button
                  size="sm"
                  onClick={handleManualInstall}
                  className="bg-white text-primary hover:bg-white/90"
                >
                  How to Install
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => setShowPrompt(false)}
                  className="text-white hover:bg-white/20"
                >
                  Dismiss
                </Button>
              </>
            )}
          </div>
        </div>
        <button
          onClick={() => setShowPrompt(false)}
          className="text-white/80 hover:text-white"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
