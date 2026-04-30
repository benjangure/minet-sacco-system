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
  // Token is stored in the session object in localStorage
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
    console.warn('No token found in localStorage');
  }
  
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
};

// Flag to prevent multiple redirect attempts
let isRedirecting = false;

const handleAuthError = (response: Response) => {
  if (response.status === 401) {
    // Only redirect if not already on login page and if we have a token to clear
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
      // Token expired - clear session and redirect to login
      isRedirecting = true;
      localStorage.removeItem('token');
      localStorage.removeItem('session');
      localStorage.removeItem('userRole');
      window.location.href = '/login';
      throw new Error('Session expired. Please login again.');
    } else if (!hasToken) {
      // No token - just throw error without redirect
      throw new Error('No authentication token found');
    }
  }
  return response;
};

const getApiBaseUrlDynamic = (): string => {
  return getApiBaseUrl();
};

const getNotificationsPath = (): string => {
  let userRole = null;
  
  try {
    const session = localStorage.getItem('session');
    if (session) {
      const parsedSession = JSON.parse(session);
      userRole = parsedSession.user?.role;
    }
  } catch (e) {
    console.warn('Failed to parse session from localStorage');
  }
  
  // Staff roles use /api/notifications, members use /api/member/notifications
  if (userRole && userRole === 'MEMBER') {
    return '/member/notifications';
  }
  return '/notifications';
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
