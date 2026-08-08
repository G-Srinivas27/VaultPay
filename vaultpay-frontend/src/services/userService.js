// services/userService.js
// All user-related API calls.

import api from './api';

/**
 * Fetches the currently logged-in user's profile.
 * Uses GET /api/users/me — returns the user tied to the JWT.
 *
 * Spring Boot response:
 * { "success": true, "data": { "id": 1, "firstName": "Srinivas", "email": "..." } }
 */
export const getMyProfile = () => {
  return api.get('/api/users/me');
};

export const updateMyProfile = (profileData) => {
  return api.put('/api/users/me', profileData);
};

/**
 * Fetches all users (ADMIN only).
 * @param {number} page - 0-indexed page number
 * @param {number} size - items per page
 */
export const getAllUsers = (page = 0, size = 10) => {
  return api.get(`/api/users?page=${page}&size=${size}`);
};
