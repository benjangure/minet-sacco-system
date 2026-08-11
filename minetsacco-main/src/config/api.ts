/**
 * API Configuration
 * This file manages the backend API URL for different environments
 */

import axios from 'axios';
import { Capacitor } from '@capacitor/core';

// For APK: server IP for UAT, change to domain when going live
const DEFAULT_NATIVE_BACKEND_URL =
  import.meta.env.VITE_NATIVE_BACKEND_URL || 'http://10.39.60.15:9090';

const getDefaultBackendUrl = (): string => {
  if (Capacitor.isNativePlatform()) {
    return DEFAULT_NATIVE_BACKEND_URL;
  }
  // For web: uses .env.development (localhost:9090) or .env.production (10.39.60.15:9090)
  return import.meta.env.VITE_API_URL || 'http://localhost:9090';
};

const DEFAULT_BACKEND_URL = getDefaultBackendUrl();

export const getBackendUrl = (): string => {
  if (!Capacitor.isNativePlatform()) {
    return import.meta.env.VITE_API_URL || 'http://localhost:9090';
  }
  const stored = localStorage.getItem('backendUrl');
  return stored || DEFAULT_BACKEND_URL;
};

export const setBackendUrl = (url: string): void => {
  if (!Capacitor.isNativePlatform()) {
    console.warn('Backend URL can only be changed for native APK platform.');
    return;
  }
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
  timeout: 15000, // 15 second timeout to prevent 30s+ hangs
});

export const getAuthToken = (): string | null => {
  // Try session object first (where AuthContext stores token)
  let token = localStorage.getItem('token');
  if (!token) {
    const sessionStr = localStorage.getItem('session');
    if (sessionStr) {
      try {
        const session = JSON.parse(sessionStr);
        if (session.token && typeof session.token === 'string') {
          token = session.token;
        }
      } catch (e) {
        console.error('Failed to parse session:', e);
      }
    }
  }
  return token;
};

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
  (response) => response,
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
          localStorage.removeItem('token');
          localStorage.removeItem('session');

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