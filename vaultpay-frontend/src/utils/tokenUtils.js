// utils/tokenUtils.js
// Centralises all JWT token storage operations.
//
// Why localStorage (not sessionStorage or cookies)?
// - localStorage persists across browser tabs and page refreshes
// - sessionStorage clears when the tab closes (bad UX)
// - HttpOnly cookies are more secure but require backend support
// For this project, localStorage is the standard approach.
//
// Why a separate utility file?
// If we ever switch from localStorage to cookies or sessionStorage,
// we only change THIS file — not every component that uses tokens.

const TOKEN_KEY = 'vaultpay_token';
const ROLE_KEY  = 'vaultpay_role';

/**
 * Saves the JWT token to localStorage after a successful login.
 * @param {string} token - The JWT string returned by the backend
 */
export const saveToken = (token) => {
  localStorage.setItem(TOKEN_KEY, token);
};

/**
 * Retrieves the JWT token from localStorage.
 * Returns null if no token is stored (user not logged in).
 * @returns {string|null}
 */
export const getToken = () => {
  return localStorage.getItem(TOKEN_KEY);
};

/**
 * Removes the JWT token from localStorage.
 * Call this on logout.
 */
export const removeToken = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(ROLE_KEY); // Always clear role on logout too
};

/**
 * Checks whether the user currently has a stored token.
 * Note: this only checks EXISTENCE, not validity.
 * Token expiry is handled by the backend (401 response).
 * @returns {boolean}
 */
export const isLoggedIn = () => {
  return !!getToken();
};

// ─── Role Helpers ────────────────────────────────────────────────────────────
// The backend JWT does not embed the role as a claim.
// Instead we persist the role string from the LoginResponse body ("USER" or "ADMIN")
// in localStorage so AdminRoute can read it without an extra API call.

/**
 * Saves the user role to localStorage after a successful login.
 * @param {string} role - "USER" or "ADMIN"
 */
export const saveRole = (role) => {
  localStorage.setItem(ROLE_KEY, role);
};

/**
 * Retrieves the stored user role.
 * @returns {string|null} "USER", "ADMIN", or null
 */
export const getRole = () => {
  return localStorage.getItem(ROLE_KEY);
};
