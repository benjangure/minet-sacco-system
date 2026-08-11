/**
 * Push Notification Service
 * Handles Web Push API subscriptions and push notification management
 */

import { getApiBaseUrl } from '../config/api';

export interface PushSubscriptionData {
  endpoint: string;
  keys: {
    p256dh: string;
    auth: string;
  };
}

export interface PushSubscriptionResponse {
  success: boolean;
  message: string;
  subscription?: PushSubscriptionData;
}

/**
 * Handle authentication errors (401/403) by redirecting to appropriate login
 */
const handleAuthError = (response: Response): void => {
  if (response.status === 401 || response.status === 403) {
    const currentPath = window.location.pathname;
    const isMemberPortal = currentPath.startsWith('/member');
    
    // Clear authentication data
    localStorage.removeItem('token');
    localStorage.removeItem('session');
    localStorage.removeItem('userRole');
    
    // Redirect to appropriate login page
    if (isMemberPortal) {
      window.location.href = '/member/login';
    } else {
      window.location.href = '/login';
    }
    
    console.warn('[Push Service] Session expired. Redirecting to login page.');
  }
};

class PushNotificationService {
  private static instance: PushNotificationService;
  private vapidPublicKey: string = '';
  private subscription: PushSubscription | null = null;

  private constructor() {
    this.initializeVapidKey();
  }

  static getInstance(): PushNotificationService {
    if (!PushNotificationService.instance) {
      PushNotificationService.instance = new PushNotificationService();
    }
    return PushNotificationService.instance;
  }

  /**
   * Initialize VAPID public key from environment
   */
  private initializeVapidKey() {
    // Get VAPID public key from environment variable
    this.vapidPublicKey = import.meta.env.VITE_VAPID_PUBLIC_KEY || '';
    
    if (!this.vapidPublicKey) {
      console.warn('[Push Service] VAPID public key not configured. Push notifications will not work.');
    }
  }

  /**
   * Check if push notifications are supported in this browser
   */
  isSupported(): boolean {
    return (
      'serviceWorker' in navigator &&
      'PushManager' in window &&
      'Notification' in window
    );
  }

  /**
   * Check if user has granted notification permission
   */
  hasPermission(): boolean {
    return this.isSupported() && Notification.permission === 'granted';
  }

  /**
   * Get current notification permission status
   */
  getPermission(): NotificationPermission {
    if (!this.isSupported()) {
      return 'denied';
    }
    return Notification.permission;
  }

  /**
   * Request notification permission from user
   */
  async requestPermission(): Promise<NotificationPermission> {
    if (!this.isSupported()) {
      console.warn('[Push Service] Push notifications are not supported in this browser');
      return 'denied';
    }

    if (Notification.permission === 'granted') {
      console.log('[Push Service] Permission already granted');
      return 'granted';
    }

    try {
      const permission = await Notification.requestPermission();
      console.log('[Push Service] Permission status:', permission);
      return permission;
    } catch (error) {
      console.error('[Push Service] Error requesting permission:', error);
      return 'denied';
    }
  }

  /**
   * Get the current push subscription if it exists
   */
  async getCurrentSubscription(): Promise<PushSubscription | null> {
    if (!this.isSupported()) {
      return null;
    }

    try {
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();
      this.subscription = subscription;
      return subscription;
    } catch (error) {
      console.error('[Push Service] Error getting subscription:', error);
      return null;
    }
  }

  /**
   * Check if user is currently subscribed to push notifications
   */
  async isSubscribed(): Promise<boolean> {
    const subscription = await this.getCurrentSubscription();
    return subscription !== null;
  }

  /**
   * Convert base64 VAPID key to Uint8Array
   */
  private urlBase64ToUint8Array(base64String: string): Uint8Array {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding)
      .replace(/\-/g, '+')
      .replace(/_/g, '/');

    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);

    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
  }

  /**
   * Subscribe to push notifications
   */
  async subscribe(): Promise<PushSubscriptionResponse> {
    if (!this.isSupported()) {
      return {
        success: false,
        message: 'Push notifications are not supported in this browser'
      };
    }

    if (!this.vapidPublicKey) {
      return {
        success: false,
        message: 'VAPID public key is not configured'
      };
    }

    try {
      // Request permission first
      const permission = await this.requestPermission();
      if (permission !== 'granted') {
        return {
          success: false,
          message: 'Notification permission denied'
        };
      }

      // Get service worker registration
      const registration = await navigator.serviceWorker.ready;
      console.log('[Push Service] Service worker ready:', registration);

      // Check for existing subscription
      let subscription = await registration.pushManager.getSubscription();
      
      if (subscription) {
        console.log('[Push Service] Existing subscription found');
      } else {
        // Subscribe to push notifications
        console.log('[Push Service] Creating new subscription...');
        subscription = await registration.pushManager.subscribe({
          userVisibleOnly: true,
          applicationServerKey: this.urlBase64ToUint8Array(this.vapidPublicKey)
        });
        console.log('[Push Service] New subscription created');
      }

      this.subscription = subscription;

      // Convert subscription to plain object for backend
      const subscriptionData = this.subscriptionToJson(subscription);

      // Send subscription to backend
      const response = await this.sendSubscriptionToBackend(subscriptionData);

      if (response.success) {
        console.log('[Push Service] ✅ Subscription saved to backend');
        return {
          success: true,
          message: 'Successfully subscribed to push notifications',
          subscription: subscriptionData
        };
      } else {
        throw new Error(response.message || 'Failed to save subscription to backend');
      }

    } catch (error) {
      console.error('[Push Service] ❌ Subscription failed:', error);
      return {
        success: false,
        message: error instanceof Error ? error.message : 'Failed to subscribe to push notifications'
      };
    }
  }

  /**
   * Unsubscribe from push notifications
   */
  async unsubscribe(): Promise<PushSubscriptionResponse> {
    if (!this.isSupported()) {
      return {
        success: false,
        message: 'Push notifications are not supported'
      };
    }

    try {
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();

      if (!subscription) {
        return {
          success: true,
          message: 'No active subscription found'
        };
      }

      // Convert subscription to JSON before unsubscribing (we need it for backend)
      const subscriptionData = this.subscriptionToJson(subscription);

      // Unsubscribe from push service
      const unsubscribed = await subscription.unsubscribe();

      if (unsubscribed) {
        console.log('[Push Service] Unsubscribed from push service');
        
        // Remove subscription from backend
        await this.removeSubscriptionFromBackend(subscriptionData);
        
        this.subscription = null;

        return {
          success: true,
          message: 'Successfully unsubscribed from push notifications'
        };
      } else {
        throw new Error('Failed to unsubscribe from push service');
      }

    } catch (error) {
      console.error('[Push Service] ❌ Unsubscribe failed:', error);
      return {
        success: false,
        message: error instanceof Error ? error.message : 'Failed to unsubscribe from push notifications'
      };
    }
  }

  /**
   * Convert PushSubscription to JSON format for backend
   */
  private subscriptionToJson(subscription: PushSubscription): PushSubscriptionData {
    const keys = subscription.toJSON().keys;
    
    if (!keys || !keys.p256dh || !keys.auth) {
      throw new Error('Invalid subscription keys');
    }

    return {
      endpoint: subscription.endpoint,
      keys: {
        p256dh: keys.p256dh,
        auth: keys.auth
      }
    };
  }

  /**
   * Get authentication headers for API requests
   */
  private getAuthHeaders(): HeadersInit {
    let token = null;

    try {
      const session = localStorage.getItem('session');
      if (session) {
        const parsedSession = JSON.parse(session);
        token = parsedSession.token;
      }
    } catch (e) {
      console.warn('[Push Service] Failed to parse session from localStorage');
    }

    if (!token) {
      token = localStorage.getItem('token');
    }

    return {
      'Authorization': token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json',
    };
  }

  /**
   * Send subscription to backend server
   */
  private async sendSubscriptionToBackend(subscriptionData: PushSubscriptionData): Promise<any> {
    try {
      const response = await fetch(`${getApiBaseUrl()}/member/push/subscribe`, {
        method: 'POST',
        headers: this.getAuthHeaders(),
        body: JSON.stringify(subscriptionData)
      });

      handleAuthError(response);

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      return {
        success: true,
        message: data.message || 'Subscription saved successfully',
        data: data.data
      };

    } catch (error) {
      console.error('[Push Service] Error sending subscription to backend:', error);
      throw error;
    }
  }

  /**
   * Remove subscription from backend server
   */
  private async removeSubscriptionFromBackend(subscriptionData: PushSubscriptionData): Promise<any> {
    try {
      const response = await fetch(`${getApiBaseUrl()}/member/push/unsubscribe`, {
        method: 'POST',
        headers: this.getAuthHeaders(),
        body: JSON.stringify(subscriptionData)
      });

      handleAuthError(response);

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      return {
        success: true,
        message: data.message || 'Subscription removed successfully'
      };

    } catch (error) {
      console.error('[Push Service] Error removing subscription from backend:', error);
      throw error;
    }
  }

  /**
   * Get subscription status from backend
   */
  async getSubscriptionStatus(): Promise<{ subscribed: boolean; subscription?: PushSubscriptionData }> {
    try {
      const response = await fetch(`${getApiBaseUrl()}/member/push/status`, {
        method: 'GET',
        headers: this.getAuthHeaders()
      });

      handleAuthError(response);

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      return {
        subscribed: data.data?.subscribed || false,
        subscription: data.data?.subscription
      };

    } catch (error) {
      console.error('[Push Service] Error getting subscription status:', error);
      return { subscribed: false };
    }
  }

  /**
   * Request a test notification from backend
   */
  async sendTestNotification(): Promise<{ success: boolean; message: string }> {
    try {
      const response = await fetch(`${getApiBaseUrl()}/member/push/test`, {
        method: 'POST',
        headers: this.getAuthHeaders(),
        body: JSON.stringify({})
      });

      handleAuthError(response);

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      return {
        success: true,
        message: data.message || 'Test notification sent successfully'
      };

    } catch (error) {
      console.error('[Push Service] Error sending test notification:', error);
      return {
        success: false,
        message: error instanceof Error ? error.message : 'Failed to send test notification'
      };
    }
  }

  /**
   * Update service worker
   */
  async updateServiceWorker(): Promise<void> {
    if (!this.isSupported()) {
      return;
    }

    try {
      const registration = await navigator.serviceWorker.ready;
      await registration.update();
      console.log('[Push Service] Service worker updated');
    } catch (error) {
      console.error('[Push Service] Error updating service worker:', error);
    }
  }

  /**
   * Re-subscribe if subscription has expired or changed
   */
  async resubscribe(): Promise<PushSubscriptionResponse> {
    console.log('[Push Service] Re-subscribing...');
    
    // First unsubscribe
    await this.unsubscribe();
    
    // Then subscribe again
    return await this.subscribe();
  }

  /**
   * Initialize push notifications for the app
   * Call this when user logs in or app starts
   */
  async initialize(): Promise<void> {
    if (!this.isSupported()) {
      console.log('[Push Service] Push notifications not supported');
      return;
    }

    console.log('[Push Service] Initializing...');

    try {
      // Check if service worker is registered
      const registration = await navigator.serviceWorker.ready;
      console.log('[Push Service] Service worker ready');

      // Check for existing subscription
      const subscription = await registration.pushManager.getSubscription();
      this.subscription = subscription;

      if (subscription) {
        console.log('[Push Service] Existing subscription found');
        // Verify subscription with backend
        const status = await this.getSubscriptionStatus();
        if (!status.subscribed) {
          console.log('[Push Service] Subscription not found on backend, re-subscribing...');
          await this.resubscribe();
        }
      } else {
        console.log('[Push Service] No subscription found');
      }

    } catch (error) {
      console.error('[Push Service] Initialization error:', error);
    }
  }
}

// Export singleton instance
export const pushNotificationService = PushNotificationService.getInstance();

// Helper functions for easy access
export const isPushSupported = () => pushNotificationService.isSupported();
export const hasPushPermission = () => pushNotificationService.hasPermission();
export const subscribeToPush = () => pushNotificationService.subscribe();
export const unsubscribeFromPush = () => pushNotificationService.unsubscribe();
export const isPushSubscribed = () => pushNotificationService.isSubscribed();
export const initializePush = () => pushNotificationService.initialize();
export const sendTestPushNotification = () => pushNotificationService.sendTestNotification();
