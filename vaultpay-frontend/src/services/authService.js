// services/authService.js
// Handles all authentication-related API calls.
//
// Design rule: Pages call authService functions, NEVER axios directly.
// This means if the backend URL or request format changes,
// only this file needs to change — not every page that uses login.

import api from './api';

/**
 * Calls POST /api/auth/login with email and password.
 * Returns the full Axios response (data.data contains the token).
 *
 * Spring Boot response shape:
 * {
 *   "success": true,
 *   "message": "Login successful",
 *   "data": { "token": "eyJhbGci..." }
 * }
 *
 * @param {string} email
 * @param {string} password
 * @returns {Promise<AxiosResponse>}
 */
export const login = (email, password) => {
  return api.post('/api/auth/login', { email, password });
};

/**
 * Calls POST /api/users/register with user registration data.
 *
 * @param {Object} userData - { firstName, lastName, email, password }
 * @returns {Promise<AxiosResponse>}
 */
export const register = (userData) => {
  return api.post('/api/users/register', userData);
};

/**
 * Calls POST /api/auth/forgot-password with email.
 */
export const forgotPassword = (email) => {
  return api.post('/api/auth/forgot-password', { email });
};

/**
 * Calls POST /api/auth/reset-password with token and new password.
 */
export const resetPassword = (token, newPassword) => {
  return api.post('/api/auth/reset-password', { token, newPassword });
};
