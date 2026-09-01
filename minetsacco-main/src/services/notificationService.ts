import { getApiBaseUrl } from '../config/api';
import { nativeFetch } from '../utils/nativeHttp';

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

/**
 * Determine whether the current active session is a member session.
 * 
 * Logic (in priority order):
 * 1. If `session` key has a staff role → staff, regardless of member_session
 * 2. If `member_session` key has a valid token and no staff session → member
 * 3. URL path /member/* → member
 * 4. Default → staff
 */
const isActiveMemberSession = (): boolean => {
  // Staff session takes priority — if it exists and has a non-MEMBER role, this is staff
  try {
    const staffSession = localStorage.getItem('session');
    if (staffSession) {
      const parsed = JSON.parse(staffSession);
      const role: string = parsed.role || parsed.user?.role || '';
      if (role && role !== 'MEMBER') {
        return false; // Definitely staff
      }
    }
  } catch (_) {}

  // Member session present and no overriding staff role → member
  try {
    const memberSession = localStorage.getItem('member_session');
    if (memberSession) {
      const parsed = JSON.parse(memberSession);
      if (parsed.token) return true;
    }
  } catch (_) {}

  // URL fallback
  return window.location.pathname.startsWith('/member');
};

// Reliable path detection based on active session role
const getNotificationsPath = (): string =>
  isActiveMemberSession() ? '/member/notifications' : '/notifications';

const getAuthHeaders = () => {
  // Pick the correct session key based on the active session role
  const sessionKey = isActiveMemberSession() ? 'member_session' : 'session';
  let token: string | null = null;

  try {
    const raw = localStorage.getItem(sessionKey);
    if (raw) token = JSON.parse(raw)?.token ?? null;
  } catch (_) {}

  // Fallback: raw token key
  if (!token) token = localStorage.getItem('token');

  if (!token) console.warn('No token found in localStorage');

  return {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
};

let isRedirecting = false;

const handleAuthError = (response: { ok: boolean; status: number }) => {
  // Only treat 401 as session expiry — 403 is a permissions error, not auth failure
  if (response.status === 401) {
    const currentPath = window.location.pathname;
    const isMemberPortal = currentPath.startsWith('/member');

    // Don't redirect if already on a login page
    const isLoginPage = currentPath === '/login' || currentPath === '/member/login';
    if (isLoginPage) {
      throw new Error('Authentication required');
    }

    let hasToken = false;

    try {
      const memberSession = localStorage.getItem('member_session');
      if (memberSession) {
        const parsedSession = JSON.parse(memberSession);
        hasToken = !!parsedSession.token;
      }
    } catch (e) {
      // Failed to parse member_session
    }

    if (!hasToken) {
      try {
        const session = localStorage.getItem('session');
        if (session) {
          const parsedSession = JSON.parse(session);
          hasToken = !!parsedSession.token;
        }
      } catch (e) {
        // Failed to parse session
      }
    }

    if (!hasToken) {
      hasToken = !!localStorage.getItem('token');
    }

    // Only redirect if we have a token (session expired) and not already redirecting
    if (hasToken && !isRedirecting) {
      isRedirecting = true;
      localStorage.removeItem('token');
      localStorage.removeItem('session');
      localStorage.removeItem('member_session');
      localStorage.removeItem('userRole');

      // Redirect to appropriate login page
      if (isMemberPortal) {
        window.location.href = '/member/login';
      } else {
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

export const notificationService = {
  getNotifications: async () => {
    const response = await nativeFetch(`${getApiBaseUrlDynamic()}${getNotificationsPath()}`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });
    const handledResponse = handleAuthError(response);
    if (!handledResponse.ok) throw new Error('Failed to fetch notifications');
    const data = await response.json();
    return Array.isArray(data.data) ? data.data : [];
  },

  getUnreadNotifications: async () => {
    const response = await nativeFetch(`${getApiBaseUrlDynamic()}${getNotificationsPath()}/unread`, {
      method: 'GET',
      headers: getAuthHeaders(),
    });
    const handledResponse = handleAuthError(response);
    if (!handledResponse.ok) throw new Error('Failed to fetch unread notifications');
    const data = await response.json();
    return Array.isArray(data.data) ? data.data : [];
  },

  getUnreadCount: async () => {
    try {
      const response = await nativeFetch(`${getApiBaseUrlDynamic()}${getNotificationsPath()}/unread-count`, {
        method: 'GET',
        headers: getAuthHeaders(),
      });
      if (response.status === 400) return 0;
      const handledResponse = handleAuthError(response);
      if (!handledResponse.ok) return 0;
      const data = await response.json();
      return typeof data.data === 'number' ? data.data : 0;
    } catch (error) {
      return 0;
    }
  },

  markAsRead: async (notificationId: number) => {
    const response = await nativeFetch(`${getApiBaseUrlDynamic()}${getNotificationsPath()}/${notificationId}/read`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({}),
    });
    const handledResponse = handleAuthError(response);
    if (!handledResponse.ok) throw new Error('Failed to mark notification as read');
    return await response.json();
  },

  markAllAsRead: async () => {
    const response = await nativeFetch(`${getApiBaseUrlDynamic()}${getNotificationsPath()}/read-all`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({}),
    });
    const handledResponse = handleAuthError(response);
    if (!handledResponse.ok) throw new Error('Failed to mark all as read');
    return await response.json();
  },

  deleteNotification: async (notificationId: number) => {
    const response = await nativeFetch(`${getApiBaseUrlDynamic()}${getNotificationsPath()}/${notificationId}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });
    const handledResponse = handleAuthError(response);
    if (!handledResponse.ok) throw new Error('Failed to delete notification');
    return await response.json();
  },
};