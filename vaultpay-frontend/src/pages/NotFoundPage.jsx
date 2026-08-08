// pages/NotFoundPage.jsx
// Custom 404 page for unmatched routes.

import { useNavigate } from 'react-router-dom';
import './NotFoundPage.css';

export default function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div className="not-found-page">
      <div className="not-found-card glass-card">
        <div className="not-found-code gradient-text">404</div>
        <h1 className="not-found-title">Page Not Found</h1>
        <p className="not-found-desc">
          We couldn't find the page you're looking for. It might have been moved, deleted, or never existed.
        </p>
        
        <div className="not-found-actions">
          <button 
            className="btn-secondary" 
            onClick={() => navigate(-1)}
          >
            ← Go Back
          </button>
          <button 
            className="btn-primary" 
            onClick={() => navigate('/dashboard')}
          >
            Dashboard
          </button>
        </div>
      </div>
      
      {/* Decorative background glow */}
      <div className="not-found-glow" />
    </div>
  );
}
