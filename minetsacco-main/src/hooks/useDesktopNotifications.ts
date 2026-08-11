import { useState, useEffect, useCallback } from 'react';
import { desktopNotificationService } from '@/services/desktopNotificationService';

/**
 * Hook for managing desktop notifications
 */
export const useDesktopNotifications = () => {
  const [isSupported, setIsSupported] = useState(false);
  const [isEnabled, setIsEnabled] = useState(false);
  const [permission, setPermission] = useState<NotificationPermission>('default');

  useEffect(() => {
    setIsSupported(desktopNotificationService.isSupported());
    setIsEnabled(desktopNotificationService.isEnabled());
    setPermission(desktopNotificationService.getPermission());
  }, []);

  const requestPermission = useCallback(async () => {
    const result = await desktopNotificationService.requestPermission();
    setPermission(result);
    setIsEnabled(result === 'granted');
    return result;
  }, []);

  const showNotification = useCallback(
    (title: string, body: string, options?: any) => {
      return desktopNotificationService.show({
        title,
        body,
        ...options,
      });
    },
    []
  );

  const showLoanNotification = useCallback((message: string, loanId?: number) => {
    return desktopNotificationService.showLoanNotification(message, loanId);
  }, []);

  const showDepositNotification = useCallback((message: string, depositId?: number) => {
    return desktopNotificationService.showDepositNotification(message, depositId);
  }, []);

  const showGuarantorNotification = useCallback((message: string, requestId?: number) => {
    return desktopNotificationService.showGuarantorNotification(message, requestId);
  }, []);

  const showApprovalNotification = useCallback((message: string, itemId?: number) => {
    return desktopNotificationService.showApprovalNotification(message, itemId);
  }, []);

  const showSystemNotification = useCallback((message: string) => {
    return desktopNotificationService.showSystemNotification(message);
  }, []);

  return {
    isSupported,
    isEnabled,
    permission,
    requestPermission,
    showNotification,
    showLoanNotification,
    showDepositNotification,
    showGuarantorNotification,
    showApprovalNotification,
    showSystemNotification,
  };
};
