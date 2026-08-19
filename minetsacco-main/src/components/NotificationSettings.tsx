/**
 * Notification Settings Component
 * Allows users to enable/disable push notifications
 */

import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { Bell, BellOff, Smartphone, CheckCircle, XCircle, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { 
  pushNotificationService, 
  isPushSupported, 
  subscribeToPush, 
  unsubscribeFromPush,
  sendTestPushNotification
} from '@/services/pushNotificationService';

export const NotificationSettings = () => {
  const [isSupported, setIsSupported] = useState(false);
  const [isSubscribed, setIsSubscribed] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSending, setIsSending] = useState(false);
  const [permission, setPermission] = useState<NotificationPermission>('default');

  useEffect(() => {
    checkNotificationStatus();
  }, []);

  const checkNotificationStatus = async () => {
    setIsLoading(true);
    
    try {
      // Check if push notifications are supported
      const supported = isPushSupported();
      setIsSupported(supported);

      if (!supported) {
        console.log('[Notification Settings] Push notifications not supported');
        setIsLoading(false);
        return;
      }

      // Check permission status
      const perm = pushNotificationService.getPermission();
      setPermission(perm);

      // Check subscription status
      const subscribed = await pushNotificationService.isSubscribed();
      setIsSubscribed(subscribed);

      console.log('[Notification Settings] Status:', { supported, permission: perm, subscribed });
    } catch (error) {
      console.error('[Notification Settings] Error checking status:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleToggleNotifications = async (enabled: boolean) => {
    if (!isSupported) {
      toast.error('Push notifications are not supported in your browser');
      return;
    }

    setIsLoading(true);

    try {
      if (enabled) {
        // Subscribe to push notifications
        const result = await subscribeToPush();
        
        if (result.success) {
          setIsSubscribed(true);
          setPermission('granted');
          toast.success('Push notifications enabled successfully!', {
            description: 'You will now receive important updates',
            icon: <CheckCircle className="h-5 w-5 text-green-500" />
          });
        } else {
          toast.error('Failed to enable notifications', {
            description: result.message,
            icon: <XCircle className="h-5 w-5 text-red-500" />
          });
        }
      } else {
        // Unsubscribe from push notifications
        const result = await unsubscribeFromPush();
        
        if (result.success) {
          setIsSubscribed(false);
          toast.success('Push notifications disabled', {
            description: 'You will no longer receive push notifications',
            icon: <BellOff className="h-5 w-5" />
          });
        } else {
          toast.error('Failed to disable notifications', {
            description: result.message,
            icon: <XCircle className="h-5 w-5 text-red-500" />
          });
        }
      }
    } catch (error) {
      console.error('[Notification Settings] Toggle error:', error);
      toast.error('An error occurred', {
        description: error instanceof Error ? error.message : 'Please try again',
        icon: <XCircle className="h-5 w-5 text-red-500" />
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleSendTestNotification = async () => {
    if (!isSubscribed) {
      toast.error('Please enable notifications first');
      return;
    }

    setIsSending(true);

    try {
      const result = await sendTestPushNotification();
      
      if (result.success) {
        toast.success('Test notification sent!', {
          description: 'Check your device for the notification',
          icon: <CheckCircle className="h-5 w-5 text-green-500" />
        });
      } else {
        toast.error('Failed to send test notification', {
          description: result.message,
          icon: <XCircle className="h-5 w-5 text-red-500" />
        });
      }
    } catch (error) {
      console.error('[Notification Settings] Test notification error:', error);
      toast.error('Failed to send test notification', {
        icon: <XCircle className="h-5 w-5 text-red-500" />
      });
    } finally {
      setIsSending(false);
    }
  };

  if (!isSupported) {
    const isHTTPS = window.location.protocol === 'https:';
    const isLocalhost = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
    const needsHTTPS = !isHTTPS && !isLocalhost;

    return (
      <Card className="border-amber-200 bg-amber-50">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-amber-900">
            <BellOff className="h-5 w-5" />
            Push Notifications Not Available
          </CardTitle>
          <CardDescription className="text-amber-700 space-y-2">
            {needsHTTPS ? (
              <>
                <p>Push notifications require a secure connection (HTTPS).</p>
                <p className="text-sm">
                  <strong>Current URL:</strong> {window.location.protocol}//{window.location.host}
                </p>
                <p className="text-sm">
                  <strong>Quick fix:</strong> Access via <code className="bg-amber-100 px-1 rounded">http://localhost:{window.location.port || '5173'}</code> instead.
                </p>
              </>
            ) : (
              <>
                <p>Your browser does not support push notifications.</p>
                <p className="text-sm">Please use Chrome, Firefox, Edge, or Safari.</p>
              </>
            )}
          </CardDescription>
        </CardHeader>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Bell className="h-5 w-5" />
          Push Notifications
        </CardTitle>
        <CardDescription>
          Receive real-time alerts about your loans, deposits, and account activity
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Main Toggle */}
        <div className="flex items-center justify-between space-x-4 p-4 bg-muted rounded-lg">
          <div className="flex-1 space-y-1">
            <Label htmlFor="push-notifications" className="text-base font-medium">
              Enable Push Notifications
            </Label>
            <p className="text-sm text-muted-foreground">
              {isSubscribed 
                ? 'You are receiving push notifications' 
                : 'Get notified about important updates'}
            </p>
          </div>
          <div className="flex items-center gap-3">
            {isLoading && <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />}
            <Switch
              id="push-notifications"
              checked={isSubscribed}
              onCheckedChange={handleToggleNotifications}
              disabled={isLoading || permission === 'denied'}
            />
          </div>
        </div>

        {/* Permission Denied Warning */}
        {permission === 'denied' && (
          <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
            <div className="flex items-start gap-3">
              <XCircle className="h-5 w-5 text-red-500 flex-shrink-0 mt-0.5" />
              <div className="flex-1 space-y-2">
                <p className="text-sm font-medium text-red-900">
                  Notifications are blocked
                </p>
                <p className="text-sm text-red-700">
                  You have blocked notifications for this site. To enable them:
                </p>
                <ol className="text-sm text-red-700 list-decimal list-inside space-y-1 ml-2">
                  <li>Click the lock icon in your browser's address bar</li>
                  <li>Find "Notifications" in the permissions list</li>
                  <li>Change the setting to "Allow"</li>
                  <li>Reload this page</li>
                </ol>
              </div>
            </div>
          </div>
        )}

        {/* Status Indicator */}
        {isSubscribed && permission === 'granted' && (
          <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
            <div className="flex items-center gap-3">
              <CheckCircle className="h-5 w-5 text-green-500" />
              <div className="flex-1">
                <p className="text-sm font-medium text-green-900">
                  Notifications are active
                </p>
                <p className="text-sm text-green-700">
                  You will receive alerts about loans, deposits, and important updates
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Test Notification Button */}
        {isSubscribed && (
          <div className="pt-4 border-t">
            <Button
              onClick={handleSendTestNotification}
              disabled={isSending}
              variant="outline"
              className="w-full"
            >
              {isSending ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Sending...
                </>
              ) : (
                <>
                  <Smartphone className="h-4 w-4 mr-2" />
                  Send Test Notification
                </>
              )}
            </Button>
            <p className="text-xs text-muted-foreground text-center mt-2">
              Test your notification settings with a sample alert
            </p>
          </div>
        )}

        {/* What you'll receive */}
        <div className="pt-4 border-t space-y-3">
          <h4 className="text-sm font-medium">You'll be notified about:</h4>
          <ul className="space-y-2 text-sm text-muted-foreground">
            <li className="flex items-center gap-2">
              <div className="h-1.5 w-1.5 rounded-full bg-primary" />
              Loan approvals and rejections
            </li>
            <li className="flex items-center gap-2">
              <div className="h-1.5 w-1.5 rounded-full bg-primary" />
              Loan disbursements
            </li>
            <li className="flex items-center gap-2">
              <div className="h-1.5 w-1.5 rounded-full bg-primary" />
              Deposit confirmations
            </li>
            <li className="flex items-center gap-2">
              <div className="h-1.5 w-1.5 rounded-full bg-primary" />
              Guarantor requests
            </li>
            <li className="flex items-center gap-2">
              <div className="h-1.5 w-1.5 rounded-full bg-primary" />
              Payment reminders
            </li>
            <li className="flex items-center gap-2">
              <div className="h-1.5 w-1.5 rounded-full bg-primary" />
              Security alerts
            </li>
          </ul>
        </div>
      </CardContent>
    </Card>
  );
};

export default NotificationSettings;
