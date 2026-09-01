/**
 * API Configuration
 * This file manages the backend API URL for different environments
 */

import axios from 'axios';
import { Capacitor } from '@capacitor/core';
import { getPlatformAdapter } from '@/utils/capacitorAxiosAdapter';

// The one true backend URL for this deployment
const SERVER_BACKEND_URL = 'http://10.39.60.15:9090';

export const getBackendUrl = (): string => {
  // Native Capacitor app (APK) — always use server backend
  // Check isNativePlatform first, then fall back to hostname detection
  if (Capacitor.isNativePlatform()) {
    const stored = localStorage.getItem('backendUrl');
    return stored || SERVER_BACKEND_URL;
  }

  // Capacitor WebView may report hostname as 'localhost' internally but
  // the user-agent contains 'wv' (WebView). Also catch empty/blank hostnames.
  const hostname = window.location.hostname;
  const isWebView = /wv/.test(navigator.userAgent) || 
                    hostname === '' || 
                    hostname === 'null';

  if (isWebView) {
    return SERVER_BACKEND_URL;
  }

  // Web app on the production server (non-localhost)
  if (hostname !== 'localhost' && hostname !== '127.0.0.1') {
    return SERVER_BACKEND_URL;
  }

  // Local development only
  return import.meta.env.VITE_API_URL || 'http://localhost:9090';
};

export const setBackendUrl = (url: string): void => {
  localStorage.setItem('backendUrl', url);
  api.defaults.baseURL = `${url}/api`;
};

export const getApiBaseUrl = (): string => {
  return `${getBackendUrl()}/api`;
};

export const API_BASE_URL = getApiBaseUrl();

const api = axios.create({
  baseURL: getApiBaseUrl(),
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
  // On Android use CapacitorHttp to bypass WebView CORS; elsewhere use default
  adapter: getPlatformAdapter(),
});

export const getAuthToken = (): string | null => {
  // Staff session always takes priority over member_session.
  // A TREASURER or LOAN_OFFICER logging in on native should not pick up a
  // stale member_session that happens to exist in localStorage.
  try {
    const staffStr = localStorage.getItem('session');
    if (staffStr) {
      const staffSession = JSON.parse(staffStr);
      const role: string = staffSession.role || staffSession.user?.role || '';
      // If the stored session is a real staff session (not MEMBER), use it.
      if (role && role !== 'MEMBER' && staffSession.token) {
        return staffSession.token;
      }
    }
  } catch (_) {}

  // Member session — used when on /member/* path or on native with no staff session
  const isMemberContext = window.location.pathname.startsWith('/member');
  // On native, fall through to member_session if no staff session found above
  const tryMemberSession = isMemberContext || Capacitor.isNativePlatform();
  if (tryMemberSession) {
    try {
      const memberStr = localStorage.getItem('member_session');
      if (memberStr) {
        const memberSession = JSON.parse(memberStr);
        if (memberSession.token && typeof memberSession.token === 'string') {
          return memberSession.token;
        }
      }
    } catch (_) {}
  }

  // Fallback: raw 'token' key
  return localStorage.getItem('token');
};

// Update the axios instance baseURL dynamically in case the module loaded
// before Capacitor/window was fully ready
api.defaults.baseURL = getApiBaseUrl();

api.interceptors.request.use((config) => {
  const token = getAuthToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Track if we're already redirecting to prevent multiple redirects
let isRedirecting = false;

// Response interceptor to handle session expiry
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    
    // Check if error is 401 (Unauthorized) - 403 can be permission issue, not session expiry
    if (error.response && error.response.status === 401) {
      // Only redirect if user was actually logged in (has a token) and we're not already redirecting
      const hasToken = getAuthToken();
      
      if (hasToken && !isRedirecting) {
        // Get current path to determine portal type
        const currentPath = window.location.pathname;
        
        // Don't redirect if already on login page
        if (currentPath === '/login' || currentPath === '/member/login') {
          return Promise.reject(error);
        }
        
        const isMemberPortal = currentPath.startsWith('/member');
        const isStaffPortal = !isMemberPortal && (
          currentPath.startsWith('/dashboard') ||
          currentPath.startsWith('/loans') ||
          currentPath.startsWith('/members') ||
          currentPath.startsWith('/transactions') ||
          currentPath.startsWith('/reports') ||
          currentPath.startsWith('/settings') ||
          currentPath.startsWith('/user-management') ||
          currentPath.startsWith('/gl') ||
          currentPath.startsWith('/administration')
        );

        // Only redirect on truly protected routes, and only for session expiry
        // Check if the error message indicates session expiry (not just authorization failure)
        const isSessionExpiry = error.response.data?.message?.toLowerCase().includes('token') ||
                               error.response.data?.message?.toLowerCase().includes('session') ||
                               error.response.data?.message?.toLowerCase().includes('expired') ||
                               error.response.data?.message?.toLowerCase().includes('invalid token');
        
        if ((isMemberPortal || isStaffPortal) && isSessionExpiry) {
          isRedirecting = true;
          
          // Clear the appropriate session key
          const sessionKey = isMemberPortal ? 'member_session' : 'session';
          localStorage.removeItem(sessionKey);
          localStorage.removeItem('token'); // Legacy cleanup

          // Redirect to appropriate login page
          if (isMemberPortal) {
            window.location.href = '/member/login';
          } else if (isStaffPortal) {
            window.location.href = '/login';
          }
          
          // Reset flag after redirect
          setTimeout(() => {
            isRedirecting = false;
          }, 1000);
          
          // Log the session expiry
          console.warn('Session expired. Redirecting to login page.');
        }
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;

export const getApiUrl = (endpoint: string): string => {
  return `${getApiBaseUrl()}${endpoint}`;
};

export const API_ENDPOINTS = {
  LOGIN: '/auth/login',
  LOGOUT: '/auth/logout',
  REFRESH_TOKEN: '/auth/refresh-token',
  MEMBER_DASHBOARD: '/member/dashboard',
  MEMBER_PROFILE: '/member/profile',
  MEMBER_LOANS: '/member/loans',
  MEMBER_ACCOUNTS: '/member/accounts',
  MEMBER_TRANSACTIONS: '/member/transactions',
  REQUEST_LOAN_REPAYMENT: '/member/request-loan-repayment',
  LOAN_REPAYMENT_REQUESTS: '/member/loan-repayment-requests',
  LOAN_REPAYMENT_REJECTION_DETAILS: '/member/loan-repayment-requests/{requestId}/rejection-details',
  LOAN_REPAYMENT_RESUBMIT: '/member/loan-repayment-requests/{requestId}/resubmit',
  NOTIFICATIONS: '/member/notifications',
  NOTIFICATIONS_UNREAD: '/member/notifications/unread',
  NOTIFICATIONS_UNREAD_COUNT: '/member/notifications/unread-count',
};