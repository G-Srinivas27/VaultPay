// pages/LoginPage.jsx
// The login page — first thing users see.
//
// Flow:
//   User fills form → calls authService.login() → gets JWT token
//   → saves to localStorage via tokenUtils → redirects to /dashboard
//
// Why useNavigate instead of window.location.href?
//   useNavigate is React Router's way to navigate — it doesn't reload
//   the page, preserving React state. window.location.href causes a
//   full page reload (we only use that in the Axios interceptor for 401s).

import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { login } from '../services/authService';
import { saveToken, saveRole } from '../utils/tokenUtils';
import './LoginPage.css';

export default function LoginPage() {
  // Form field values
  const [email, setEmail]       = useState('');
  const [password, setPassword] = useState('');

  // UI state
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState('');

  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();   // Prevent default browser form submission
    setError('');         // Clear any previous error
    setLoading(true);

    try {
      // Call the authService (which calls POST /api/auth/login)
      const response = await login(email, password);

      // Our Spring Boot ApiResponse wraps data:
      // response.data = { success: true, message: "...", data: { token: "eyJ...", role: "ADMIN" } }
      const { token, role } = response.data.data;

      // Persist the token and role so they survive page refreshes
      saveToken(token);
      saveRole(role); // "USER" or "ADMIN" — used by AdminRoute

      // Navigate to the dashboard (replace: true removes /login from history
      // so the back button doesn't take the user back to login)
      navigate('/dashboard', { replace: true });

    } catch (err) {
      // err.response exists if the server returned an error response (4xx, 5xx)
      // err.response is undefined for network errors (server down, no internet)
      if (err.response) {
        const message = err.response.data?.message || 'Login failed. Please try again.';
        setError(message);
      } else {
        setError('Cannot connect to server. Please check your connection.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      {/* Animated background orbs for depth effect */}
      <div className="bg-orb bg-orb-1" />
      <div className="bg-orb bg-orb-2" />

      <div className="login-container">
        {/* Brand Header */}
        <div className="login-brand">
          <div className="brand-icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"
                    stroke="url(#brandGrad)" strokeWidth="2"
                    strokeLinecap="round" strokeLinejoin="round"/>
              <defs>
                <linearGradient id="brandGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#6366f1"/>
                  <stop offset="100%" stopColor="#8b5cf6"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h1 className="brand-name">
            Vault<span className="gradient-text">Pay</span>
          </h1>
        </div>

        {/* Glass Card */}
        <div className="glass-card login-card">
          <div className="login-header">
            <h2>Welcome back</h2>
            <p>Sign in to your account to continue</p>
          </div>

          <form onSubmit={handleSubmit} className="login-form">
            {/* Error Alert */}
            {error && (
              <div className="alert-error" role="alert">
                <span>⚠</span>
                {error}
              </div>
            )}

            {/* Email Field */}
            <div className="form-group">
              <label htmlFor="email" className="form-label">Email address</label>
              <input
                id="email"
                type="email"
                className="form-input"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
                autoFocus
              />
            </div>

            {/* Password Field */}
            <div className="form-group">
              <label htmlFor="password" className="form-label">Password</label>
              <input
                id="password"
                type="password"
                className="form-input"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
              />
            </div>
            
            <div style={{ textAlign: 'right', marginBottom: '1.5rem', marginTop: '-0.5rem' }}>
              <Link to="/forgot-password" style={{ fontSize: '13px', color: '#a5b4fc', textDecoration: 'none' }}>
                Forgot Password?
              </Link>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              className="btn-primary"
              disabled={loading}
            >
              {loading ? (
                <span className="btn-loading">
                  <span className="spinner" /> Signing in...
                </span>
              ) : (
                'Sign In'
              )}
            </button>
          </form>

          {/* Footer */}
          <div className="login-footer">
            <p>
              Don&apos;t have an account?{' '}
              <Link to="/register">Create one</Link>
            </p>
          </div>
        </div>

        {/* Security Note */}
        <p className="security-note">
          🔒 Secured with JWT Authentication
        </p>
      </div>
    </div>
  );
}
