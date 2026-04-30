import React, { useState, useEffect } from 'react';
import { Bell, X, Check, CheckCheck } from 'lucide-react';
import { notificationService, Notification } from '../services/notificationService';
import { useAuth } from '@/contexts/AuthContext';
import { API_BASE_URL } from '@/config/api';

export const NotificationBell: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const { session, role } = useAuth();

  useEffect(() => {
    // Check for staff session or member token
    const staffSession = localStorage.getItem('session');
    const memberToken = localStorage.getItem('token');
    const token = staffSession ? JSON.parse(staffSession)?.token : memberToken;
    
    if (token) {
      loadUnreadCount();
      // Poll for unread count every 10 seconds
      const interval = setInterval(() => {
        loadUnreadCount();
      }, 10000);
      return () => clearInterval(interval);
    }
  }, [session, role]);

  const loadUnreadCount = async () => {
    try {
      const count = await notificationService.getUnreadCount();
      setUnreadCount(count);
    } catch (error) {
      console.error('Failed to load unread count:', error);
    }
  };

  
  const loadNotifications = async () => {
    setLoading(true);
    try {
      const data = await notificationService.getNotifications();
      setNotifications(data);
    } catch (error) {
      console.error('Failed to load notifications:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleBellClick = () => {
    if (!isOpen) {
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

  // Don't render if no token (check both staff session and member token)
  const staffSession = localStorage.getItem('session');
  const memberToken = localStorage.getItem('token');
  const token = staffSession ? JSON.parse(staffSession)?.token : memberToken;
  
  if (!token) {
    return null;
  }

  const totalBadgeCount = unreadCount;

  return (
    <div className="relative">
      <button
        onClick={handleBellClick}
        className="relative p-2 text-gray-600 hover:text-gray-900 focus:outline-none"
        title="Notifications"
      >
        <Bell size={24} />
        {totalBadgeCount > 0 && (
          <span className="absolute top-0 right-0 inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-white transform translate-x-1/2 -translate-y-1/2 bg-red-600 rounded-full">
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
            ) : notifications.length === 0 ? (
              <div className="flex items-center justify-center h-32">
                <p className="text-gray-500">No notifications</p>
              </div>
            ) : (
              <div className="divide-y">
                {notifications.map((notification) => (
                  <div
                    key={notification.id}
                    className={`p-4 hover:bg-gray-50 transition ${
                      !notification.read ? 'bg-blue-50' : ''
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1">
                        <p className="text-sm text-gray-900">
                          {notification.message}
                        </p>
                        <p className="text-xs text-gray-500 mt-1">
                          {formatTime(notification.createdAt)}
                        </p>
                      </div>
                      <div className="flex gap-1">
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
