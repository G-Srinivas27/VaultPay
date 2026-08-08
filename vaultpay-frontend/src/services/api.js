// services/api.js
// Shared Axios instance for all API calls to the VaultPay backend.
//
// Why create a custom Axios instance instead of using axios directly?
//
// 1. Base URL: Set once here, not repeated in every service file
// 2. Request Interceptor: Automatically attaches the JWT Bearer token
//    to every outgoing request — no need to pass it manually each time
// 3. Response Interceptor: Handles 401 Unauthorized globally —
//    if the token expires, the user is redirected to login automatically
// 4. Single place to change backend URL (dev vs prod)

import axios from 'axios';
import { getToken, removeToken } from '../utils/tokenUtils';

// The base URL of our Spring Boot backend.
// In production, this would be an environment variable: import.meta.env.VITE_API_URL
const BASE_URL = 'http://localhost:8080';

// Create a custom Axios instance with default configuration
const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ─── Request Interceptor ──────────────────────────────────────────────────────
// Runs BEFORE every outgoing request.
// Reads the JWT from localStorage and attaches it as a Bearer token header.
//
// Without this, you'd need to write:
//   Authorization: `Bearer ${getToken()}`
// in every single API call. This DRYs it up completely.
api.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ─── Response Interceptor ─────────────────────────────────────────────────────
// Runs AFTER every incoming response.
// Handles 401 Unauthorized globally:
//   - Token expired? Clear it and redirect to login.
//   - This prevents users from being stuck on a page with an expired token.
api.interceptors.response.use(
  (response) => response, // Pass successful responses through unchanged
  (error) => {
    if (error.response?.status === 401) {
      // Token is expired or invalid — force logout
      removeToken();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
