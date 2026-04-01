const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export interface Notification {
  id: number;
  user: {
    id: number;
    username: string;
  };
  message: string;
  type: string;
  read: boolean;
  createdAt: string;
}

const getAuthHeaders = () => {
  // Member portal stores token directly in localStorage
  const token = localStorage.getItem('token');
  
  if (!token) {
    console.warn('No token found in localStorage');
  }
  
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
};

export const notificationService = {
  getNotifications: async () => {
    const response = await fetch(`${API_BASE_URL}/member/notifications`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Failed to fetch notifications');
    const data = await response.json();
    return Array.isArray(data.data) ? data.data : [];
  },

  getUnreadNotifications: async () => {
    const response = await fetch(`${API_BASE_URL}/member/notifications/unread`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Failed to fetch unread notifications');
    const data = await response.json();
    return Array.isArray(data.data) ? data.data : [];
  },

  getUnreadCount: async () => {
    const response = await fetch(`${API_BASE_URL}/member/notifications/unread-count`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Failed to fetch unread count');
    const data = await response.json();
    return typeof data.data === 'number' ? data.data : 0;
  },

  markAsRead: async (notificationId: number) => {
    const response = await fetch(`${API_BASE_URL}/member/notifications/${notificationId}/read`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({}),
    });
    if (!response.ok) throw new Error('Failed to mark notification as read');
    return await response.json();
  },

  markAllAsRead: async () => {
    const response = await fetch(`${API_BASE_URL}/member/notifications/read-all`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({}),
    });
    if (!response.ok) throw new Error('Failed to mark all as read');
    return await response.json();
  },

  deleteNotification: async (notificationId: number) => {
    const response = await fetch(`${API_BASE_URL}/member/notifications/${notificationId}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });
    if (!response.ok) throw new Error('Failed to delete notification');
    return await response.json();
  },
};
