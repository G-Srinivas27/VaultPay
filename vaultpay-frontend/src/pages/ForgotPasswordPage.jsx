import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { forgotPassword } from '../services/authService';
import './LoginPage.css'; // Reusing LoginPage CSS for consistent branding

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);

    try {
      const response = await forgotPassword(email);
      setMessage(response.data.message || 'If an account exists, a reset link has been sent.');
    } catch (err) {
      if (err.response) {
        setError(err.response.data?.message || 'Failed to process request.');
      } else {
        setError('Cannot connect to server. Please check your connection.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="bg-orb bg-orb-1" />
      <div className="bg-orb bg-orb-2" />

      <div className="login-container">
        <div className="login-brand" onClick={() => navigate('/login')} style={{ cursor: 'pointer' }}>
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

        <div className="glass-card login-card">
          <div className="login-header">
            <h2>Forgot Password</h2>
            <p>Enter your email to receive a reset link</p>
          </div>

          <form onSubmit={handleSubmit} className="login-form">
            {error && (
              <div className="alert-error" role="alert">
                <span>⚠</span> {error}
              </div>
            )}
            {message && (
              <div className="alert-success" role="alert" style={{ backgroundColor: 'rgba(52, 211, 153, 0.1)', color: '#34d399', padding: '12px', borderRadius: '8px', marginBottom: '20px', border: '1px solid rgba(52, 211, 153, 0.2)' }}>
                <span>✓</span> {message}
              </div>
            )}

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
                autoFocus
              />
            </div>

            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? (
                <span className="btn-loading"><span className="spinner" /> Sending...</span>
              ) : 'Send Reset Link'}
            </button>
          </form>

          <div className="login-footer" style={{ marginTop: '20px' }}>
            <p>
              Remembered your password? <Link to="/login">Sign in</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
