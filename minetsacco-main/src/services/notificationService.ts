import { getApiBaseUrl } from '../config/api';

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
  let token = null;

  try {
    const session = localStorage.getItem('session');
    if (session) {
      const parsedSession = JSON.parse(session);
      token = parsedSession.token;
    }
  } catch (e) {
    console.warn('Failed to parse session from localStorage');
  }

  if (!token) {
    token = localStorage.getItem('token');
  }

  if (!token) {
    console.warn('No token found in localStorage');
  }

  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
};

let isRedirecting = false;

const handleAuthError = (response: Response) => {
  if (response.status === 401) {
    const currentPath = window.location.pathname;
    let hasToken = false;

    try {
      const session = localStorage.getItem('session');
      if (session) {
        const parsedSession = JSON.parse(session);
        hasToken = !!parsedSession.token;
      }
    } catch (e) {
      // Failed to parse session
    }

    if (hasToken && currentPath !== '/login' && !isRedirecting) {
      isRedirecting = true;
      localStorage.removeItem('token');
      localStorage.removeItem('session');
      localStorage.removeItem('userRole');
      window.location.href = '/login';
      throw new Error('Session expired. Please login again.');
    } else if (!hasToken) {
      throw new Error('No authentication token found');
    }
  }
  return response;
};

const getApiBaseUrlDynamic = (): string => {
  return getApiBaseUrl();
};

// Reliable path detection based on current URL instead of localStorage state
const getNotificationsPath = (): string => {
  const isMemberPortal = window.location.pathname.startsWith('/member');
  return isMemberPortal ? '/member/notifications' : '/notifications';
};

export const notificationService = {
  getNotifications: async () => {
    const response = await fetch(`${getApiBaseUrlDynamic()}${getNotificationsPath()}`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });
    const handledResponse = handleAuthError(response);
    if (!handledResponse.ok) throw new Error('Failed to fetch notifications');
    const data = await handledResponse.json();
    return Array.isArray(data.data) ? data.data : [];
  },

  getUnreadNotifications: async () => {
    const response = await fetch(`${getApiBaseUrlDynamic()}${getNotificationsPath()}/unread`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });
    const handledResponse = handleAuthError(response);
    if (!handledResponse.ok) throw new Error('Failed to fetch unread notifications');
    const data = await handledResponse.json();
    return Array.isArray(data.data) ? data.data : [];
  },

  getUnreadCount: async () => {
    const response = await fetch(`${getApiBaseUrlDynamic()}${getNotificationsPath()}/unread-count`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });
    const handledResponse = handleAuthError(response);
    if (!handledResponse.ok) throw new Error('Failed to fetch unread count');
    const data = await handledResponse.json();
    return typeof data.data === 'number' ? data.data : 0;
  },

  markAsRead: async (notificationId: number) => {
    const response = await fetch(`${getApiBaseUrlDynamic()}${getNotificationsPath()}/${notificationId}/read`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({}),
    });
    const handledResponse = handleAuthError(response);
    if (!handledResponse.ok) throw new Error('Failed to mark notification as read');
    return await handledResponse.json();
  },

  markAllAsRead: async () => {
    const response = await fetch(`${getApiBaseUrlDynamic()}${getNotificationsPath()}/read-all`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({}),
    });
    const handledResponse = handleAuthError(response);
    if (!handledResponse.ok) throw new Error('Failed to mark all as read');
    return await handledResponse.json();
  },

  deleteNotification: async (notificationId: number) => {
    const response = await fetch(`${getApiBaseUrlDynamic()}${getNotificationsPath()}/${notificationId}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });
    const handledResponse = handleAuthError(response);
    if (!handledResponse.ok) throw new Error('Failed to delete notification');
    return await handledResponse.json();
  },
};