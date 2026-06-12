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
  // For web: uses .env.development (localhost:8080) or .env.production (10.39.60.15:9090)
  return import.meta.env.VITE_API_URL || 'http://localhost:8080';
};

const DEFAULT_BACKEND_URL = getDefaultBackendUrl();

export const getBackendUrl = (): string => {
  if (!Capacitor.isNativePlatform()) {
    return import.meta.env.VITE_API_URL || 'http://localhost:8080';
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
});

api.interceptors.request.use((config) => {
  const isMemberRoute = window.location.pathname.startsWith('/member');
  let token = null;

  if (isMemberRoute) {
    token = localStorage.getItem('token');
  } else {
    const sessionStr = localStorage.getItem('session');
    if (sessionStr) {
      try {
        const session = JSON.parse(sessionStr);
        token = session.token;
      } catch (e) {
        token = localStorage.getItem('token');
      }
    } else {
      token = localStorage.getItem('token');
    }
  }

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

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