// services/adminService.js
// All admin-only API calls.
// These endpoints return 403 Forbidden for non-ADMIN users (enforced by Spring Security).

import api from './api';

/**
 * Fetches all users in a paginated list.
 * GET /api/users?page=0&size=10
 * @param {number} page - zero-indexed page number
 * @param {number} size - number of users per page
 */
export const getAllUsers = (page = 0, size = 10) => {
  return api.get(`/api/users?page=${page}&size=${size}&sort=createdAt,desc`);
};

/**
 * Updates a user's role.
 * PATCH /api/users/{id}/role
 * @param {number} userId
 * @param {string} role - "ADMIN" or "USER"
 */
export const updateUserRole = (userId, role) => {
  return api.patch(`/api/users/${userId}/role`, { role });
};

/**
 * Activates or deactivates a user account.
 * PATCH /api/users/{id}/status?active=true|false
 * @param {number} userId
 * @param {boolean} active
 */
export const updateUserStatus = (userId, active) => {
  return api.patch(`/api/users/${userId}/status?active=${active}`);
};
