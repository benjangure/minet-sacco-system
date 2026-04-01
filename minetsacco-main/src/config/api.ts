import axios from 'axios';

export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

// Create axios instance with interceptors
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - attaches fresh token to EVERY request
api.interceptors.request.use(
  (config) => {
    // Try to get token from multiple sources for compatibility
    let token = localStorage.getItem('token');
    
    if (!token) {
      // Fallback to session storage
      const session = localStorage.getItem('session');
      if (session) {
        try {
          const parsedSession = JSON.parse(session);
          token = parsedSession.token;
        } catch (e) {
          console.error('Failed to parse session:', e);
        }
      }
    }
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - catches 401s globally
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid - clear storage and redirect to login
      localStorage.removeItem('token');
      localStorage.removeItem('session');
      localStorage.removeItem('user');
      window.location.href = '/member/login';
    }
    return Promise.reject(error);
  }
);

export default api;
