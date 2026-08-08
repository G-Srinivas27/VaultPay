// components/PrivateRoute.jsx
// A route guard that protects authenticated pages.
//
// How it works:
//   If user has a JWT token → render the protected page (children)
//   If user has NO token    → redirect to /login
//
// Why do we need this?
//   Without PrivateRoute, anyone can type /dashboard in the browser
//   and see the page — even without logging in.
//   PrivateRoute checks for the token BEFORE rendering the page.
//
// Usage in App.jsx:
//   <Route path="/dashboard" element={
//     <PrivateRoute>
//       <DashboardPage />
//     </PrivateRoute>
//   } />
//
// Important: This is CLIENT-SIDE protection only.
// The backend ALSO validates the JWT on every API call.
// So even if someone bypasses PrivateRoute, they'll get 401 from the API.
// Client + Server protection = defence in depth.

import { Navigate } from 'react-router-dom';
import { isLoggedIn } from '../utils/tokenUtils';

export default function PrivateRoute({ children }) {
  // Check if a token exists in localStorage
  if (!isLoggedIn()) {
    // No token → redirect to login, replacing history so
    // the back button doesn't bring them back to the protected page
    return <Navigate to="/login" replace />;
  }

  // Token exists → render the actual page
  return children;
}
