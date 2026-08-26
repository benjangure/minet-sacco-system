import React, { useState, useEffect } from 'react';
import { Bell, X, Check, CheckCheck } from 'lucide-react';
import { notificationService, Notification } from '../services/notificationService';
import { useAuth } from '@/contexts/AuthContext';
import { useNavigate } from 'react-router-dom';

export const NotificationBell: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Track consecutive failures — stop polling after 3 to avoid console noise
  const failureCount = React.useRef(0);
  const pollingDisabled = React.useRef(false);
  const { session, role } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    // Check member_session first, then staff session, then raw token
    let token: string | null = null;
    try {
      const memberSession = localStorage.getItem('member_session');
      if (memberSession) token = JSON.parse(memberSession)?.token;
    } catch (_) {}
    if (!token) {
      try {
        const staffSession = localStorage.getItem('session');
        if (staffSession) token = JSON.parse(staffSession)?.token;
      } catch (_) {}
    }
    if (!token) token = localStorage.getItem('token');

    if (token) {
      // Reset failure tracking on session change
      failureCount.current = 0;
      pollingDisabled.current = false;

      loadUnreadCount();
      const interval = setInterval(() => {
        if (!pollingDisabled.current) {
          loadUnreadCount();
        }
      }, 10000);
      return () => clearInterval(interval);
    }
  }, [session, role]);

  const loadUnreadCount = async () => {
    try {
      const count = await notificationService.getUnreadCount();
      setUnreadCount(count);
      // Reset failure counter on success
      failureCount.current = 0;
    } catch (error) {
      failureCount.current += 1;
      // After 3 consecutive failures, stop polling — avoids console spam
      // when the staff user account doesn't exist in the notifications DB
      if (failureCount.current >= 3) {
        pollingDisabled.current = true;
      }
    }
  };

  
  const loadNotifications = async () => {
    setLoading(true);
    setError(null);
    try {
      // Load only unread notifications to match the bell badge count
      const data = await notificationService.getUnreadNotifications();
      setNotifications(data);
    } catch (error) {
      console.error('Failed to load unread notifications:', error);
      setNotifications([]);
      setError('Unable to load notifications. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleBellClick = () => {
    if (!isOpen) {
      setError(null);
      loadNotifications();
      loadUnreadCount();
    }
    setIsOpen(!isOpen);
  };

  const handleMarkAsRead = async (notificationId: number) => {
    try {
      await notificationService.markAsRead(notificationId);
      setNotifications(
        notifications.map((n) =>
          n.id === notificationId ? { ...n, read: true } : n
        )
      );
      await loadUnreadCount();
    } catch (error) {
      console.error('Failed to mark notification as read:', error);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications(notifications.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (error) {
      console.error('Failed to mark all as read:', error);
    }
  };

  const handleDelete = async (notificationId: number) => {
    try {
      await notificationService.deleteNotification(notificationId);
      setNotifications(notifications.filter((n) => n.id !== notificationId));
      await loadUnreadCount();
    } catch (error) {
      console.error('Failed to delete notification:', error);
    }
  };

  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  };

  // Determine navigation path based on notification type and context
  const getNavigationPath = (notification: Notification): string | null => {
    const type = notification.type?.toUpperCase();
    const category = notification.category?.toUpperCase();

    // Loan-related notifications
    if (type?.includes('LOAN') || category?.includes('LOAN')) {
      if (type?.includes('APPROVAL') || category === 'LOAN_APPROVAL') {
        return '/loans'; // Staff view loans page
      }
      if (type?.includes('DISBURSED') || type?.includes('DISBURSEMENT')) {
        return '/loans';
      }
      if (type?.includes('REPAYMENT')) {
        return '/loan-repayments';
      }
      if (type?.includes('TOPUP') || type?.includes('TOP_UP')) {
        return '/loans'; // Could be a specific topup page if it exists
      }
      return '/loans'; // Default for loan notifications
    }

    // Guarantor-related notifications
    if (type?.includes('GUARANTOR') || category?.includes('GUARANTOR')) {
      if (role === 'MEMBER') {
        return '/member/guarantor-approvals'; // Member guarantor page
      }
      return '/loans'; // Staff can view from loans page
    }

    // Deposit-related notifications
    if (type?.includes('DEPOSIT') || category?.includes('DEPOSIT')) {
      return '/savings';
    }

    // Repayment-related notifications
    if (type?.includes('REPAYMENT') || category?.includes('REPAYMENT')) {
      return '/loan-repayments';
    }

    // Member-related notifications
    if (type?.includes('MEMBER') || category?.includes('MEMBER')) {
      return '/members';
    }

    // Bulk processing notifications
    if (type?.includes('BULK') || category?.includes('BULK')) {
      return '/bulk-processing';
    }

    // Transaction notifications
    if (type?.includes('TRANSACTION') || category?.includes('TRANSACTION')) {
      return '/member-transactions';
    }

    // Default: no navigation (notification is informational only)
    return null;
  };

  const handleNotificationClick = async (notification: Notification) => {
    // Mark as read
    if (!notification.read) {
      await handleMarkAsRead(notification.id);
    }

    // Navigate to relevant page
    const path = getNavigationPath(notification);
    if (path) {
      setIsOpen(false); // Close the dropdown
      navigate(path);
    }
  };

  const isClickable = (notification: Notification): boolean => {
    return getNavigationPath(notification) !== null;
  };

  // Don't render if no token (check member_session, staff session, and raw token)
  let token: string | null = null;
  try {
    const memberSession = localStorage.getItem('member_session');
    if (memberSession) token = JSON.parse(memberSession)?.token;
  } catch (_) {}
  if (!token) {
    try {
      const staffSession = localStorage.getItem('session');
      if (staffSession) token = JSON.parse(staffSession)?.token;
    } catch (_) {}
  }
  if (!token) token = localStorage.getItem('token');

  if (!token) {
    return null;
  }

  const totalBadgeCount = unreadCount;

  return (
    <div className="relative py-1">
      <button
        onClick={handleBellClick}
        className="relative p-2 text-gray-600 hover:text-gray-900 focus:outline-none transition-colors duration-200"
        title="Notifications"
      >
        <Bell size={24} />
        {totalBadgeCount > 0 && (
          <span className="absolute -top-1 -right-1 inline-flex items-center justify-center min-w-[20px] h-5 px-1.5 text-[10px] font-bold leading-none text-white bg-red-600 rounded-full">
            {totalBadgeCount > 99 ? '99+' : totalBadgeCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-96 bg-white rounded-lg shadow-xl z-50 max-h-96 flex flex-col">
          {/* Header */}
          <div className="flex items-center justify-between p-4 border-b">
            <h3 className="text-lg font-semibold">Notifications</h3>
            <button
              onClick={() => setIsOpen(false)}
              className="text-gray-400 hover:text-gray-600"
            >
              <X size={20} />
            </button>
          </div>

          {/* Notifications List */}
          <div className="flex-1 overflow-y-auto">
            {loading ? (
              <div className="flex items-center justify-center h-32">
                <p className="text-gray-500">Loading...</p>
              </div>
            ) : error ? (
              <div className="flex items-center justify-center h-32 p-4">
                <p className="text-red-600 text-center text-sm">{error}</p>
              </div>
            ) : notifications.length === 0 ? (
              <div className="flex items-center justify-center h-32">
                <p className="text-gray-500">No notifications</p>
              </div>
            ) : (
              <div className="divide-y">
                {notifications.map((notification) => (
                  <div
                    key={notification.id}
                    onClick={() => isClickable(notification) && handleNotificationClick(notification)}
                    className={`p-4 transition ${
                      !notification.read ? 'bg-blue-50' : ''
                    } ${
                      isClickable(notification) 
                        ? 'hover:bg-gray-100 cursor-pointer' 
                        : 'hover:bg-gray-50'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1">
                        <p className={`text-sm text-gray-900 ${
                          isClickable(notification) ? 'font-medium' : ''
                        }`}>
                          {notification.message}
                          {isClickable(notification) && (
                            <span className="text-blue-600 text-xs ml-2">→ View</span>
                          )}
                        </p>
                        <p className="text-xs text-gray-500 mt-1">
                          {formatTime(notification.createdAt)}
                        </p>
                      </div>
                      <div className="flex gap-1" onClick={(e) => e.stopPropagation()}>
                        {!notification.read && (
                          <button
                            onClick={() => handleMarkAsRead(notification.id)}
                            className="p-1 text-blue-600 hover:bg-blue-100 rounded"
                            title="Mark as read"
                          >
                            <Check size={16} />
                          </button>
                        )}
                        <button
                          onClick={() => handleDelete(notification.id)}
                          className="p-1 text-red-600 hover:bg-red-100 rounded"
                          title="Delete"
                        >
                          <X size={16} />
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Footer */}
          {notifications.length > 0 && unreadCount > 0 && (
            <div className="border-t p-3 bg-gray-50">
              <button
                onClick={handleMarkAllAsRead}
                className="w-full text-sm text-blue-600 hover:text-blue-800 font-medium flex items-center justify-center gap-1"
              >
                <CheckCheck size={16} />
                Mark all as read
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
