import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { resetPassword } from '../services/authService';
import './LoginPage.css';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const navigate = useNavigate();

  useEffect(() => {
    if (!token) {
      setError('Invalid or missing password reset token.');
    }
  }, [token]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (newPassword.length < 8) {
      setError('Password must be at least 8 characters long.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setLoading(true);

    try {
      const response = await resetPassword(token, newPassword);
      setSuccess(response.data.message || 'Password has been successfully reset.');
      setTimeout(() => {
        navigate('/login');
      }, 3000);
    } catch (err) {
      if (err.response) {
        setError(err.response.data?.message || 'Failed to reset password.');
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
            <h2>Reset Password</h2>
            <p>Please enter your new password below</p>
          </div>

          <form onSubmit={handleSubmit} className="login-form">
            {error && (
              <div className="alert-error" role="alert">
                <span>⚠</span> {error}
              </div>
            )}
            {success && (
              <div className="alert-success" role="alert" style={{ backgroundColor: 'rgba(52, 211, 153, 0.1)', color: '#34d399', padding: '12px', borderRadius: '8px', marginBottom: '20px', border: '1px solid rgba(52, 211, 153, 0.2)' }}>
                <span>✓</span> {success} Redirecting to login...
              </div>
            )}

            <div className="form-group">
              <label htmlFor="newPassword" className="form-label">New Password</label>
              <input
                id="newPassword"
                type="password"
                className="form-input"
                placeholder="Must be at least 8 characters"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                disabled={!token || success}
              />
            </div>

            <div className="form-group">
              <label htmlFor="confirmPassword" className="form-label">Confirm Password</label>
              <input
                id="confirmPassword"
                type="password"
                className="form-input"
                placeholder="Re-enter new password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                disabled={!token || success}
              />
            </div>

            <button type="submit" className="btn-primary" disabled={loading || !token || success}>
              {loading ? (
                <span className="btn-loading"><span className="spinner" /> Resetting...</span>
              ) : 'Reset Password'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
