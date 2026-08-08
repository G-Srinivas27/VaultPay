// pages/RegisterPage.jsx
// Handles new user registration.

import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { register, login } from '../services/authService';
import { saveToken } from '../utils/tokenUtils';
import './LoginPage.css'; // Reusing the same glassmorphism styles from login!

export default function RegisterPage() {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      // 1. Register the user
      await register(formData);
      
      // 2. Automatically log them in so they don't have to type it again
      const loginRes = await login(formData.email, formData.password);
      const token = loginRes.data.data.token;
      saveToken(token);
      
      // 3. Send them to the dashboard
      navigate('/dashboard', { replace: true });
      
    } catch (err) {
      if (err.response) {
        setError(err.response.data?.message || 'Registration failed. Please try again.');
      } else {
        setError('Cannot connect to server. Please check your connection.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      {/* Animated background orbs */}
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
            <h2>Create an account</h2>
            <p>Start managing your money today</p>
          </div>

          <form onSubmit={handleSubmit} className="login-form">
            {error && (
              <div className="alert-error" role="alert">
                <span>⚠</span> {error}
              </div>
            )}

            <div style={{ display: 'flex', gap: '16px' }}>
              <div className="form-group" style={{ flex: 1 }}>
                <label className="form-label">First name</label>
                <input
                  type="text"
                  name="firstName"
                  className="form-input"
                  placeholder="John"
                  value={formData.firstName}
                  onChange={handleChange}
                  required
                />
              </div>
              <div className="form-group" style={{ flex: 1 }}>
                <label className="form-label">Last name</label>
                <input
                  type="text"
                  name="lastName"
                  className="form-input"
                  placeholder="Doe"
                  value={formData.lastName}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Email address</label>
              <input
                type="email"
                name="email"
                className="form-input"
                placeholder="you@example.com"
                value={formData.email}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Password</label>
              <input
                type="password"
                name="password"
                className="form-input"
                placeholder="Create a strong password"
                value={formData.password}
                onChange={handleChange}
                required
                minLength="6"
              />
            </div>

            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? (
                <span className="btn-loading">
                  <span className="spinner" /> Creating account...
                </span>
              ) : (
                'Create Account'
              )}
            </button>
          </form>

          <div className="login-footer">
            <p>
              Already have an account?{' '}
              <Link to="/login">Sign in</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
