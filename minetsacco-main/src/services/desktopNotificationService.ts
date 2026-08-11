/**
 * Desktop Notification Service
 * Handles browser push notifications for real-time alerts
 */

export interface DesktopNotificationOptions {
  title: string;
  body: string;
  icon?: string;
  badge?: string;
  tag?: string;
  requireInteraction?: boolean;
  silent?: boolean;
  data?: any;
  actions?: Array<{
    action: string;
    title: string;
    icon?: string;
  }>;
}

class DesktopNotificationService {
  private static instance: DesktopNotificationService;
  private permission: NotificationPermission = 'default';
  private defaultIcon = '/icon-512x512.png';
  private defaultBadge = '/icon-192x192.png';

  private constructor() {
    this.initializePermission();
    this.setupNotificationClickHandler();
  }

  static getInstance(): DesktopNotificationService {
    if (!DesktopNotificationService.instance) {
      DesktopNotificationService.instance = new DesktopNotificationService();
    }
    return DesktopNotificationService.instance;
  }

  /**
   * Initialize notification permission status
   */
  private initializePermission() {
    if ('Notification' in window) {
      this.permission = Notification.permission;
    }
  }

  /**
   * Check if notifications are supported
   */
  isSupported(): boolean {
    return 'Notification' in window;
  }

  /**
   * Check if notifications are enabled
   */
  isEnabled(): boolean {
    return this.isSupported() && this.permission === 'granted';
  }

  /**
   * Get current permission status
   */
  getPermission(): NotificationPermission {
    return this.permission;
  }

  /**
   * Request notification permission from user
   */
  async requestPermission(): Promise<NotificationPermission> {
    if (!this.isSupported()) {
      console.warn('Desktop notifications are not supported in this browser');
      return 'denied';
    }

    if (this.permission === 'granted') {
      return 'granted';
    }

    try {
      this.permission = await Notification.requestPermission();
      
      if (this.permission === 'granted') {
        console.log('✅ Desktop notification permission granted');
        this.showWelcomeNotification();
      } else {
        console.log('❌ Desktop notification permission denied');
      }

      return this.permission;
    } catch (error) {
      console.error('Error requesting notification permission:', error);
      return 'denied';
    }
  }

  /**
   * Show a welcome notification after permission is granted
   */
  private showWelcomeNotification() {
    this.show({
      title: '🎉 Notifications Enabled!',
      body: 'You will now receive real-time updates from Minet SACCO',
      icon: this.defaultIcon,
      tag: 'welcome',
      requireInteraction: false,
    });
  }

  /**
   * Show a desktop notification
   */
  async show(options: DesktopNotificationOptions): Promise<Notification | null> {
    if (!this.isSupported()) {
      console.warn('Desktop notifications are not supported');
      return null;
    }

    if (this.permission !== 'granted') {
      console.warn('Desktop notification permission not granted');
      return null;
    }

    try {
      const notification = new Notification(options.title, {
        body: options.body,
        icon: options.icon || this.defaultIcon,
        badge: options.badge || this.defaultBadge,
        tag: options.tag,
        requireInteraction: options.requireInteraction || false,
        silent: options.silent || false,
        data: options.data,
        // @ts-ignore - actions are supported but not in all TypeScript definitions
        actions: options.actions,
      });

      // Add click handler to bring user to the app
      notification.onclick = (event) => {
        event.preventDefault();
        window.focus();
        
        // Navigate based on notification data
        if (options.data) {
          this.handleNotificationClick(options.data);
        } else {
          // Default: go to member dashboard
          window.location.href = '/member/dashboard?tab=notifications';
        }
        
        notification.close();
      };

      // Auto close after 10 seconds if not requireInteraction
      if (!options.requireInteraction) {
        setTimeout(() => {
          notification.close();
        }, 10000);
      }

      return notification;
    } catch (error) {
      console.error('Error showing desktop notification:', error);
      return null;
    }
  }

  /**
   * Handle notification click and navigate to appropriate page
   */
  private handleNotificationClick(data: any) {
    const baseUrl = window.location.origin;
    
    switch (data.type) {
      case 'LOAN':
      case 'LOAN_APPROVED':
      case 'LOAN_REJECTED':
      case 'LOAN_DISBURSED':
      case 'LOAN_STATUS_CHANGED':
        if (data.loanId) {
          window.location.href = `${baseUrl}/member/dashboard?tab=loans`;
        } else {
          window.location.href = `${baseUrl}/member/dashboard?tab=loans`;
        }
        break;
        
      case 'DEPOSIT':
      case 'DEPOSIT_STATUS_CHANGED':
        if (data.depositId) {
          window.location.href = `${baseUrl}/member/dashboard?tab=deposits`;
        } else {
          window.location.href = `${baseUrl}/member/dashboard?tab=deposits`;
        }
        break;
        
      case 'GUARANTOR':
      case 'GUARANTOR_REQUEST':
        window.location.href = `${baseUrl}/member/guarantor-approvals`;
        break;
        
      case 'APPROVAL':
        window.location.href = `${baseUrl}/member/dashboard?tab=notifications`;
        break;
        
      case 'SYSTEM':
      case 'SECURITY_ALERT':
      case 'NEW_DEVICE_LOGIN':
        window.location.href = `${baseUrl}/member/dashboard?tab=notifications`;
        break;
        
      default:
        window.location.href = `${baseUrl}/member/dashboard?tab=notifications`;
    }
  }

  /**
   * Setup notification click handler
   */
  private setupNotificationClickHandler() {
    if (!this.isSupported()) return;

    // Handle notification clicks - bring window to focus and navigate
    // This works for both desktop and mobile notifications
  }

  /**
   * Show loan-related notification
   */
  showLoanNotification(message: string, loanId?: number) {
    this.show({
      title: '💰 Loan Update',
      body: message + '\n\nClick to view details',
      icon: this.defaultIcon,
      tag: loanId ? `loan-${loanId}` : 'loan-notification',
      requireInteraction: true,
      data: { type: 'LOAN', loanId },
    });
  }

  /**
   * Show deposit-related notification
   */
  showDepositNotification(message: string, depositId?: number) {
    this.show({
      title: '💵 Deposit Update',
      body: message + '\n\nClick to view details',
      icon: this.defaultIcon,
      tag: depositId ? `deposit-${depositId}` : 'deposit-notification',
      data: { type: 'DEPOSIT', depositId },
    });
  }

  /**
   * Show guarantor request notification
   */
  showGuarantorNotification(message: string, requestId?: number) {
    this.show({
      title: '🤝 Guarantor Request',
      body: message + '\n\nClick to view request',
      icon: this.defaultIcon,
      tag: requestId ? `guarantor-${requestId}` : 'guarantor-notification',
      requireInteraction: true,
      data: { type: 'GUARANTOR', requestId },
    });
  }

  /**
   * Show approval notification
   */
  showApprovalNotification(message: string, itemId?: number) {
    this.show({
      title: '✅ Approval Update',
      body: message + '\n\nClick to view details',
      icon: this.defaultIcon,
      tag: itemId ? `approval-${itemId}` : 'approval-notification',
      data: { type: 'APPROVAL', itemId },
    });
  }

  /**
   * Show system notification
   */
  showSystemNotification(message: string) {
    this.show({
      title: '🔔 System Notification',
      body: message + '\n\nClick to view',
      icon: this.defaultIcon,
      tag: 'system-notification',
      data: { type: 'SYSTEM' },
    });
  }

  /**
   * Show generic notification
   */
  showGenericNotification(title: string, message: string, type?: string) {
    this.show({
      title,
      body: message,
      icon: this.defaultIcon,
      tag: type || 'generic-notification',
      data: { type: type || 'GENERIC' },
    });
  }

  /**
   * Clear all notifications with a specific tag
   */
  clearNotificationsByTag(tag: string) {
    // Note: This only works with service worker notifications
    if ('serviceWorker' in navigator && 'getNotifications' in ServiceWorkerRegistration.prototype) {
      navigator.serviceWorker.ready.then((registration) => {
        registration.getNotifications({ tag }).then((notifications) => {
          notifications.forEach((notification) => notification.close());
        });
      });
    }
  }

  /**
   * Clear all notifications
   */
  clearAllNotifications() {
    if ('serviceWorker' in navigator && 'getNotifications' in ServiceWorkerRegistration.prototype) {
      navigator.serviceWorker.ready.then((registration) => {
        registration.getNotifications().then((notifications) => {
          notifications.forEach((notification) => notification.close());
        });
      });
    }
  }
}

// Export singleton instance
export const desktopNotificationService = DesktopNotificationService.getInstance();

// Helper function to check if notifications are available
export const areNotificationsAvailable = () => {
  return desktopNotificationService.isSupported();
};

// Helper function to check if notifications are enabled
export const areNotificationsEnabled = () => {
  return desktopNotificationService.isEnabled();
};

// Helper function to request permission
export const requestNotificationPermission = () => {
  return desktopNotificationService.requestPermission();
};
