import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Bell, X } from 'lucide-react';
import { useDesktopNotifications } from '@/hooks/useDesktopNotifications';

interface NotificationPromptProps {
  onClose?: () => void;
}

/**
 * Prompt component to request notification permissions
 * Shows once per session when user logs in
 */
export default function NotificationPrompt({ onClose }: NotificationPromptProps) {
  const { isSupported, isEnabled, permission, requestPermission } = useDesktopNotifications();
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    // Check if we should show the prompt
    const hasSeenPrompt = sessionStorage.getItem('notification-prompt-seen');
    
    // Show if: supported, not enabled, not denied, and hasn't seen prompt this session
    if (isSupported && !isEnabled && permission !== 'denied' && !hasSeenPrompt) {
      // Delay showing prompt by 2 seconds after login
      const timer = setTimeout(() => {
        setIsVisible(true);
      }, 2000);

      return () => clearTimeout(timer);
    }
  }, [isSupported, isEnabled, permission]);

  const handleEnable = async () => {
    const result = await requestPermission();
    
    if (result === 'granted' || result === 'denied') {
      sessionStorage.setItem('notification-prompt-seen', 'true');
      setIsVisible(false);
      onClose?.();
    }
  };

  const handleDismiss = () => {
    sessionStorage.setItem('notification-prompt-seen', 'true');
    setIsVisible(false);
    onClose?.();
  };

  if (!isVisible) {
    return null;
  }

  return (
    <div className="fixed bottom-4 right-4 z-50 animate-in slide-in-from-bottom-4">
      <Card className="w-[380px] shadow-lg border-2">
        <CardHeader className="relative">
          <Button
            variant="ghost"
            size="icon"
            className="absolute right-2 top-2 h-6 w-6"
            onClick={handleDismiss}
          >
            <X className="h-4 w-4" />
          </Button>
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-full bg-gradient-to-br from-red-500 to-red-600 flex items-center justify-center">
              <Bell className="h-5 w-5 text-white" />
            </div>
            <div>
              <CardTitle className="text-lg">Enable Notifications</CardTitle>
              <CardDescription>Stay updated in real-time</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-2">
          <p className="text-sm text-muted-foreground">
            Get instant desktop notifications for:
          </p>
          <ul className="text-sm text-muted-foreground space-y-1 ml-4">
            <li>• Loan approvals and updates</li>
            <li>• Deposit confirmations</li>
            <li>• Guarantor requests</li>
            <li>• Important announcements</li>
          </ul>
        </CardContent>
        <CardFooter className="flex gap-2">
          <Button
            onClick={handleEnable}
            className="flex-1 bg-gradient-to-r from-red-500 to-red-600 hover:from-red-600 hover:to-red-700"
          >
            <Bell className="h-4 w-4 mr-2" />
            Enable Notifications
          </Button>
          <Button
            variant="outline"
            onClick={handleDismiss}
            className="flex-1"
          >
            Maybe Later
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
