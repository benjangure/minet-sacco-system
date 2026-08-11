import { useEffect, useCallback, useRef } from 'react';
import websocketService from '@/utils/websocket';

type MessageHandler = (message: any) => void;

/**
 * React hook for WebSocket subscriptions with automatic cleanup
 */
export function useWebSocket() {
  const isConnected = useRef(false);

  useEffect(() => {
    // Connect on mount if not already connected
    if (!isConnected.current && !websocketService.isConnected()) {
      const userId = localStorage.getItem('memberId') || localStorage.getItem('userId');
      websocketService.connect(userId || undefined).catch((error) => {
        console.error('Failed to connect to WebSocket:', error);
      });
      isConnected.current = true;
    }

    // Cleanup on unmount
    return () => {
      // Don't disconnect on unmount - keep connection alive for the session
      // Only disconnect when user logs out
    };
  }, []);

  return {
    isConnected: websocketService.isConnected(),
    subscribe: websocketService.subscribe.bind(websocketService),
    subscribeToLoans: websocketService.subscribeToLoans.bind(websocketService),
    subscribeToTransactions: websocketService.subscribeToTransactions.bind(websocketService),
    subscribeToRepayments: websocketService.subscribeToRepayments.bind(websocketService),
    subscribeToTopUps: websocketService.subscribeToTopUps.bind(websocketService),
    subscribeToMembers: websocketService.subscribeToMembers.bind(websocketService),
    subscribeToUserNotifications: websocketService.subscribeToUserNotifications.bind(websocketService),
    subscribeToSystemNotifications: websocketService.subscribeToSystemNotifications.bind(websocketService),
  };
}

/**
 * Hook to subscribe to loans updates with automatic cleanup
 */
export function useLoansSubscription(callback: MessageHandler) {
  const { subscribeToLoans } = useWebSocket();

  useEffect(() => {
    const unsubscribe = subscribeToLoans(callback);
    return unsubscribe;
  }, [callback, subscribeToLoans]);
}

/**
 * Hook to subscribe to transactions with automatic cleanup
 */
export function useTransactionsSubscription(callback: MessageHandler) {
  const { subscribeToTransactions } = useWebSocket();

  useEffect(() => {
    const unsubscribe = subscribeToTransactions(callback);
    return unsubscribe;
  }, [callback, subscribeToTransactions]);
}

/**
 * Hook to subscribe to repayments with automatic cleanup
 */
export function useRepaymentsSubscription(callback: MessageHandler) {
  const { subscribeToRepayments } = useWebSocket();

  useEffect(() => {
    const unsubscribe = subscribeToRepayments(callback);
    return unsubscribe;
  }, [callback, subscribeToRepayments]);
}

/**
 * Hook to subscribe to top-ups with automatic cleanup
 */
export function useTopUpsSubscription(callback: MessageHandler) {
  const { subscribeToTopUps } = useWebSocket();

  useEffect(() => {
    const unsubscribe = subscribeToTopUps(callback);
    return unsubscribe;
  }, [callback, subscribeToTopUps]);
}

/**
 * Hook to subscribe to user-specific notifications
 */
export function useUserNotifications(userId: string, callback: MessageHandler) {
  const { subscribeToUserNotifications } = useWebSocket();

  useEffect(() => {
    if (!userId) return;
    const unsubscribe = subscribeToUserNotifications(userId, callback);
    return unsubscribe;
  }, [userId, callback, subscribeToUserNotifications]);
}

/**
 * Hook to subscribe to system-wide notifications
 */
export function useSystemNotifications(callback: MessageHandler) {
  const { subscribeToSystemNotifications } = useWebSocket();

  useEffect(() => {
    const unsubscribe = subscribeToSystemNotifications(callback);
    return unsubscribe;
  }, [callback, subscribeToSystemNotifications]);
}
