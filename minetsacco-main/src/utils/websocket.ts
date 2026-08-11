import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { API_BASE_URL } from '@/config/api';
import { desktopNotificationService } from '@/services/desktopNotificationService';

type MessageHandler = (message: any) => void;

class WebSocketService {
  private client: Client | null = null;
  private connected: boolean = false;
  private reconnectAttempts: number = 0;
  private maxReconnectAttempts: number = 5;
  private reconnectDelay: number = 3000;
  private subscriptions: Map<string, string> = new Map();
  private messageHandlers: Map<string, Set<MessageHandler>> = new Map();
  private userId: string | null = null;

  /**
   * Connect to WebSocket server
   */
  connect(userId?: string): Promise<void> {
    if (this.connected) {
      console.log('WebSocket already connected');
      return Promise.resolve();
    }

    if (userId) {
      this.userId = userId;
    }

    return new Promise((resolve, reject) => {
      try {
        // Extract base URL without /api
        const wsBaseUrl = API_BASE_URL.replace('/api', '');
        
        this.client = new Client({
          webSocketFactory: () => new SockJS(`${wsBaseUrl}/ws`),
          
          connectHeaders: {
            // Add auth headers if needed
          },
          
          debug: (str) => {
            console.log('[WebSocket Debug]:', str);
          },
          
          reconnectDelay: this.reconnectDelay,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,

          onConnect: () => {
            console.log('✅ WebSocket connected');
            this.connected = true;
            this.reconnectAttempts = 0;
            
            // Re-establish all subscriptions after reconnect
            this.resubscribeAll();
            
            resolve();
          },

          onStompError: (frame) => {
            console.error('❌ WebSocket STOMP error:', frame.headers['message']);
            console.error('Additional details:', frame.body);
            this.connected = false;
            reject(new Error(frame.headers['message']));
          },

          onWebSocketError: (error) => {
            console.error('❌ WebSocket error:', error);
            this.connected = false;
          },

          onDisconnect: () => {
            console.log('🔌 WebSocket disconnected');
            this.connected = false;
            this.attemptReconnect();
          },
        });

        this.client.activate();
      } catch (error) {
        console.error('Failed to create WebSocket connection:', error);
        reject(error);
      }
    });
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.connected = false;
      this.subscriptions.clear();
      this.messageHandlers.clear();
      console.log('WebSocket disconnected manually');
    }
  }

  /**
   * Attempt to reconnect with exponential backoff
   */
  private attemptReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
      
      console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts}) in ${delay}ms`);
      
      setTimeout(() => {
        this.connect(this.userId || undefined);
      }, delay);
    } else {
      console.error('Max reconnection attempts reached. Please refresh the page.');
    }
  }

  /**
   * Re-establish all subscriptions after reconnect
   */
  private resubscribeAll(): void {
    const topics = Array.from(this.subscriptions.keys());
    this.subscriptions.clear();
    
    topics.forEach(topic => {
      const handlers = this.messageHandlers.get(topic);
      if (handlers) {
        handlers.forEach(handler => {
          this.subscribe(topic, handler);
        });
      }
    });
  }

  /**
   * Handle desktop notifications based on topic and payload
   */
  private handleDesktopNotification(topic: string, payload: any): void {
    // Only show notifications if enabled
    if (!desktopNotificationService.isEnabled()) {
      return;
    }

    try {
      // Parse notification based on topic
      if (topic === '/topic/loans' || topic.includes('/queue/notifications')) {
        // Loan notifications
        if (payload.type === 'LOAN_APPROVED' || payload.message?.toLowerCase().includes('approved')) {
          desktopNotificationService.showLoanNotification(
            payload.message || 'Your loan application has been approved!',
            payload.loanId
          );
        } else if (payload.type === 'LOAN_REJECTED' || payload.message?.toLowerCase().includes('rejected')) {
          desktopNotificationService.showLoanNotification(
            payload.message || 'Your loan application needs attention',
            payload.loanId
          );
        } else if (payload.type === 'LOAN_DISBURSED') {
          desktopNotificationService.showLoanNotification(
            payload.message || 'Your loan has been disbursed!',
            payload.loanId
          );
        } else if (payload.type?.includes('LOAN')) {
          desktopNotificationService.showLoanNotification(
            payload.message || 'Loan update received',
            payload.loanId
          );
        }
      }

      // Transaction/Deposit notifications
      if (topic === '/topic/transactions' || payload.type?.includes('DEPOSIT')) {
        desktopNotificationService.showDepositNotification(
          payload.message || 'Transaction processed successfully',
          payload.transactionId || payload.depositId
        );
      }

      // Guarantor notifications
      if (payload.type === 'GUARANTOR_REQUEST' || payload.message?.toLowerCase().includes('guarantor')) {
        desktopNotificationService.showGuarantorNotification(
          payload.message || 'You have a new guarantor request',
          payload.requestId
        );
      }

      // Approval notifications
      if (payload.type?.includes('APPROVAL') || payload.message?.toLowerCase().includes('approval')) {
        desktopNotificationService.showApprovalNotification(
          payload.message || 'Approval status updated',
          payload.id
        );
      }

      // System notifications
      if (topic === '/topic/notifications' && !payload.type) {
        desktopNotificationService.showSystemNotification(
          payload.message || 'System notification received'
        );
      }

      // Generic notification for unhandled types
      if (payload.message && !payload.type) {
        desktopNotificationService.showGenericNotification(
          'Minet SACCO',
          payload.message
        );
      }
    } catch (error) {
      console.error('Error handling desktop notification:', error);
    }
  }

  /**
   * Subscribe to a topic
   */
  subscribe(topic: string, callback: MessageHandler): () => void {
    if (!this.client || !this.connected) {
      console.warn('Cannot subscribe - WebSocket not connected');
      return () => {};
    }

    // Add handler to our tracking
    if (!this.messageHandlers.has(topic)) {
      this.messageHandlers.set(topic, new Set());
    }
    this.messageHandlers.get(topic)!.add(callback);

    // Only create subscription if it doesn't exist
    if (!this.subscriptions.has(topic)) {
      const subscription = this.client.subscribe(topic, (message: IMessage) => {
        try {
          const payload = JSON.parse(message.body);
          console.log(`📨 Message received on ${topic}:`, payload);
          
          // Trigger desktop notification based on topic
          this.handleDesktopNotification(topic, payload);
          
          // Call all handlers for this topic
          const handlers = this.messageHandlers.get(topic);
          if (handlers) {
            handlers.forEach(handler => handler(payload));
          }
        } catch (error) {
          console.error('Error parsing WebSocket message:', error);
        }
      });

      this.subscriptions.set(topic, subscription.id);
      console.log(`📡 Subscribed to ${topic}`);
    }

    // Return unsubscribe function
    return () => {
      const handlers = this.messageHandlers.get(topic);
      if (handlers) {
        handlers.delete(callback);
        
        // If no more handlers, unsubscribe from topic
        if (handlers.size === 0) {
          const subId = this.subscriptions.get(topic);
          if (subId && this.client) {
            this.client.unsubscribe(subId);
            this.subscriptions.delete(topic);
            this.messageHandlers.delete(topic);
            console.log(`📴 Unsubscribed from ${topic}`);
          }
        }
      }
    };
  }

  /**
   * Subscribe to loans topic for real-time loan updates
   */
  subscribeToLoans(callback: MessageHandler): () => void {
    return this.subscribe('/topic/loans', callback);
  }

  /**
   * Subscribe to transactions topic
   */
  subscribeToTransactions(callback: MessageHandler): () => void {
    return this.subscribe('/topic/transactions', callback);
  }

  /**
   * Subscribe to repayments topic
   */
  subscribeToRepayments(callback: MessageHandler): () => void {
    return this.subscribe('/topic/repayments', callback);
  }

  /**
   * Subscribe to top-ups topic
   */
  subscribeToTopUps(callback: MessageHandler): () => void {
    return this.subscribe('/topic/topups', callback);
  }

  /**
   * Subscribe to members topic
   */
  subscribeToMembers(callback: MessageHandler): () => void {
    return this.subscribe('/topic/members', callback);
  }

  /**
   * Subscribe to personal notifications for a specific user
   */
  subscribeToUserNotifications(userId: string, callback: MessageHandler): () => void {
    return this.subscribe(`/user/${userId}/queue/notifications`, callback);
  }

  /**
   * Subscribe to general system notifications
   */
  subscribeToSystemNotifications(callback: MessageHandler): () => void {
    return this.subscribe('/topic/notifications', callback);
  }

  /**
   * Check if WebSocket is connected
   */
  isConnected(): boolean {
    return this.connected;
  }

  /**
   * Send a message to a destination
   */
  send(destination: string, body: any): void {
    if (this.client && this.connected) {
      this.client.publish({
        destination,
        body: JSON.stringify(body),
      });
    } else {
      console.warn('Cannot send message - WebSocket not connected');
    }
  }
}

// Export singleton instance
export const websocketService = new WebSocketService();
export default websocketService;
