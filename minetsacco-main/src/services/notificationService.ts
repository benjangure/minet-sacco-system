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
  // Handle both 401 (Unauthorized) and 403 (Forbidden) as session expiry
  if (response.status === 401 || response.status === 403) {
    const currentPath = window.location.pathname;
    const isMemberPortal = currentPath.startsWith('/member');
    const isStaffPortal = !isMemberPortal && (
      currentPath.startsWith('/dashboard') ||
      currentPath.startsWith('/loans') ||
      currentPath.startsWith('/members') ||
      currentPath.startsWith('/transactions') ||
      currentPath.startsWith('/reports') ||
      currentPath.startsWith('/settings') ||
      currentPath.startsWith('/user-management')
    );
    
    // Don't redirect if already on a login page
    const isLoginPage = currentPath === '/login' || currentPath === '/member/login';
    if (isLoginPage) {
      throw new Error('Authentication required');
    }
    
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

    if (!hasToken) {
      hasToken = !!localStorage.getItem('token');
    }

    // Only redirect if we have a token (session expired) and not already redirecting
    if (hasToken && !isRedirecting) {
      isRedirecting = true;
      localStorage.removeItem('token');
      localStorage.removeItem('session');
      localStorage.removeItem('userRole');
      
      // Redirect to appropriate login page
      if (isMemberPortal) {
        window.location.href = '/member/login';
      } else if (isStaffPortal) {
        window.location.href = '/login';
      }
      
      // Reset redirect flag after a short delay
      setTimeout(() => {
        isRedirecting = false;
      }, 1000);
      
      console.warn('Session expired. Redirecting to login page.');
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

// Reliable path detection based on user role and current URL
const getNotificationsPath = (): string => {
  // Check if user is a member by looking at their role
  try {
    const session = localStorage.getItem('session');
    if (session) {
      const parsedSession = JSON.parse(session);
      const role = parsedSession.role || parsedSession.user?.role;
      // If role is MEMBER, use member endpoint
      if (role === 'MEMBER') {
        return '/member/notifications';
      }
    }
  } catch (e) {
    console.warn('Failed to parse session for role detection');
  }
  
  // Default to staff notifications endpoint
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
    try {
      const response = await fetch(`${getApiBaseUrlDynamic()}${getNotificationsPath()}/unread-count`, {
        method: 'GET',
        headers: getAuthHeaders(),
      });
      const handledResponse = handleAuthError(response);
      if (!handledResponse.ok) {
        console.warn('Notifications API not available or user not authenticated');
        return 0;
      }
      const data = await handledResponse.json();
      return typeof data.data === 'number' ? data.data : 0;
    } catch (error) {
      console.warn('Failed to fetch notification count:', error);
      return 0;
    }
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