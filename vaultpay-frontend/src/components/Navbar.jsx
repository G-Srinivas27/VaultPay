// components/Navbar.jsx
// Top navigation bar for authenticated pages.

import { useState } from 'react';
import { useNavigate, NavLink, Link } from 'react-router-dom';
import { removeToken } from '../utils/tokenUtils';
import './Navbar.css';

export default function Navbar({ user }) {
  const navigate = useNavigate();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    removeToken();                        // Clear JWT from localStorage
    navigate('/login', { replace: true }); // Redirect to login
  };

  const toggleMobileMenu = () => {
    setIsMobileMenuOpen(!isMobileMenuOpen);
  };

  return (
    <header className="navbar">
      {/* Brand */}
      <div className="navbar-brand">
        <div className="navbar-logo">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"
                  stroke="url(#navGrad)" strokeWidth="2"
                  strokeLinecap="round" strokeLinejoin="round"/>
            <defs>
              <linearGradient id="navGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#6366f1"/>
                <stop offset="100%" stopColor="#8b5cf6"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <span className="navbar-brand-name">Vault<span className="gradient-text">Pay</span></span>
      </div>

      {/* Hamburger Icon for Mobile */}
      <button className="mobile-menu-btn" onClick={toggleMobileMenu}>
        {isMobileMenuOpen ? '✕' : '☰'}
      </button>

      {/* Navigation Links & User Info */}
      <div className={`navbar-collapse ${isMobileMenuOpen ? 'show' : ''}`}>
        <nav className="navbar-links">
          <NavLink to="/dashboard" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'} onClick={() => setIsMobileMenuOpen(false)}>
            Dashboard
          </NavLink>
          <NavLink to="/transactions" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'} onClick={() => setIsMobileMenuOpen(false)}>
            Transactions
          </NavLink>
          {user?.role === 'ADMIN' && (
            <NavLink to="/admin" className={({ isActive }) => isActive ? 'nav-link active nav-link-admin' : 'nav-link nav-link-admin'} onClick={() => setIsMobileMenuOpen(false)}>
              ⚙ Admin
            </NavLink>
          )}
        </nav>

        <div className="navbar-right">
          {user && (
            <Link to="/profile" className="navbar-user" style={{ textDecoration: 'none' }} onClick={() => setIsMobileMenuOpen(false)}>
              <div className="user-avatar">
                {user.firstName?.[0]?.toUpperCase() ?? '?'}
              </div>
              <span className="user-name">{user.firstName}</span>
            </Link>
          )}
          <button className="btn-logout" onClick={handleLogout}>
            Sign Out
          </button>
        </div>
      </div>
    </header>
  );
}
