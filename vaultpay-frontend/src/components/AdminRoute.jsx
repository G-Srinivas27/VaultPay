// components/AdminRoute.jsx
// A special route guard that ONLY lets ADMIN users through.
//
// How it works:
// 1. Checks if a JWT token exists (user is logged in)
// 2. Reads the role saved to localStorage at login time (saveRole in tokenUtils)
// 3. If role is not ADMIN → redirects to /dashboard
//
// Note: The backend JWT does not embed the role as a claim.
// The role is returned in the LoginResponse body and saved to localStorage.
// The backend STILL enforces 403 independently — this is just a UI layer guard.

import { Navigate } from 'react-router-dom';
import { getToken, getRole } from '../utils/tokenUtils';

export default function AdminRoute({ children }) {
  const token = getToken();

  // Not logged in at all → go to login
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  const role = getRole(); // "ADMIN", "USER", or null

  // Logged in but not an admin → back to dashboard
  if (role !== 'ADMIN') {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}
