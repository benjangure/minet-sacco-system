/**
 * API Configuration
 * This file manages the backend API URL for different environments
 */

import axios from 'axios';
import { Capacitor } from '@capacitor/core';

// Determine backend URL based on platform
const getDefaultBackendUrl = (): string => {
  const isNative = Capacitor.isNativePlatform();
  
  if (isNative) {
    // For APK: use laptop IP (can be changed in settings)
    return 'http://192.168.0.50:8080';
  } else {
    // For web: use localhost
    return 'http://localhost:8080';
  }
};

const DEFAULT_BACKEND_URL = getDefaultBackendUrl();

// Get backend URL from localStorage or use default
export const getBackendUrl = (): string => {
  const stored = localStorage.getItem('backendUrl');
  return stored || DEFAULT_BACKEND_URL;
};

// Set backend URL in localStorage (without reload - axios instance will pick it up)
export const setBackendUrl = (url: string): void => {
  localStorage.setItem('backendUrl', url);
  // Update axios instance base URL
  api.defaults.baseURL = `${url}/api`;
};

// Get API base URL (for fetch calls)
export const getApiBaseUrl = (): string => {
  return `${getBackendUrl()}/api`;
};

// Export API_BASE_URL for backward compatibility
export const API_BASE_URL = getApiBaseUrl();

// Create axios instance with dynamic base URL
const api = axios.create({
  baseURL: getApiBaseUrl(),
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Export default axios instance
export default api;

// Get full API endpoint
export const getApiUrl = (endpoint: string): string => {
  return `${getApiBaseUrl()}${endpoint}`;
};

// Common API endpoints
export const API_ENDPOINTS = {
  // Auth
  LOGIN: '/auth/login',
  LOGOUT: '/auth/logout',
  REFRESH_TOKEN: '/auth/refresh-token',
  
  // Member Portal
  MEMBER_DASHBOARD: '/member/dashboard',
  MEMBER_PROFILE: '/member/profile',
  MEMBER_LOANS: '/member/loans',
  MEMBER_ACCOUNTS: '/member/accounts',
  MEMBER_TRANSACTIONS: '/member/transactions',
  
  // Loan Repayment
  REQUEST_LOAN_REPAYMENT: '/member/request-loan-repayment',
  LOAN_REPAYMENT_REQUESTS: '/member/loan-repayment-requests',
  LOAN_REPAYMENT_REJECTION_DETAILS: '/member/loan-repayment-requests/{requestId}/rejection-details',
  LOAN_REPAYMENT_RESUBMIT: '/member/loan-repayment-requests/{requestId}/resubmit',
  
  // Notifications
  NOTIFICATIONS: '/member/notifications',
  NOTIFICATIONS_UNREAD: '/member/notifications/unread',
  NOTIFICATIONS_UNREAD_COUNT: '/member/notifications/unread-count',
};
