// pages/ProfilePage.jsx
// Displays the authenticated user's profile and wallet information.

import { useState, useEffect } from 'react';
import Navbar from '../components/Navbar';
import Skeleton from '../components/Skeleton';
import { getMyProfile, updateMyProfile } from '../services/userService';
import { getWalletByUserId } from '../services/walletService';
import { useToast } from '../contexts/ToastContext';
import './ProfilePage.css';

// Format date: "2024-01-15T10:30:00" → "Jan 15, 2024"
const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A';
  return new Date(dateStr).toLocaleDateString('en-IN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

export default function ProfilePage() {
  const { showToast } = useToast();
  const [user, setUser] = useState(null);
  const [wallet, setWallet] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    phoneNumber: ''
  });

  useEffect(() => {
    const loadProfileData = async () => {
      try {
        setLoading(true);
        // Fetch user profile
        const userRes = await getMyProfile();
        const userData = userRes.data.data;
        setUser(userData);
        setFormData({
          firstName: userData.firstName || '',
          lastName: userData.lastName || '',
          phoneNumber: userData.phoneNumber || ''
        });

        // Fetch user's wallet
        try {
          const walletRes = await getWalletByUserId(userData.id);
          // The API may return a single wallet object or an array — handle both
          const walletData = walletRes.data.data;
          const wallet = Array.isArray(walletData) ? walletData[0] : walletData;
          setWallet(wallet);
        } catch (walletErr) {
          console.warn("Wallet not found for user", walletErr);
          // Non-fatal error, user might not have a wallet yet
        }

      } catch (err) {
        setError('Failed to load profile information.');
      } finally {
        setLoading(false);
      }
    };

    loadProfileData();
  }, []);

  const handleSaveProfile = async () => {
    try {
      setIsSaving(true);
      setError('');
      
      const res = await updateMyProfile(formData);
      const updatedUser = res.data.data;
      setUser(updatedUser);
      setFormData({
        firstName: updatedUser.firstName || '',
        lastName: updatedUser.lastName || '',
        phoneNumber: updatedUser.phoneNumber || ''
      });
      setIsEditing(false);
      showToast('Profile updated successfully!', 'success');
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Failed to update profile.');
      showToast('Failed to update profile.', 'error');
    } finally {
      setIsSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="profile-page">
        <Navbar user={null} />
        <main className="profile-main">
          <div className="page-header">
            <Skeleton width="200px" height="40px" />
            <Skeleton width="300px" height="20px" style={{ marginTop: '8px' }} />
          </div>
          <div className="profile-grid">
            <div className="glass-card profile-card" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <Skeleton className="skeleton-avatar" width="80px" height="80px" />
              <Skeleton width="150px" height="28px" style={{ marginTop: '16px' }} />
              <Skeleton width="100px" height="16px" style={{ marginTop: '8px' }} />
              
              <div className="profile-details-list" style={{ width: '100%', marginTop: '24px' }}>
                <div className="detail-row">
                  <Skeleton width="100px" height="16px" />
                  <Skeleton width="150px" height="16px" />
                </div>
                <div className="detail-row">
                  <Skeleton width="100px" height="16px" />
                  <Skeleton width="80px" height="24px" borderRadius="12px" />
                </div>
                <div className="detail-row">
                  <Skeleton width="100px" height="16px" />
                  <Skeleton width="120px" height="16px" />
                </div>
              </div>
            </div>
            
            <div className="glass-card wallet-info-card">
              <Skeleton width="180px" height="24px" />
              <Skeleton width="280px" height="16px" style={{ marginTop: '8px' }} />
              
              <div className="wallet-details" style={{ marginTop: '24px' }}>
                <div className="wallet-id-box" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
                  <Skeleton width="120px" height="14px" />
                  <Skeleton width="80px" height="32px" />
                </div>
                <div className="wallet-status-row" style={{ marginTop: '24px', display: 'flex', justifyContent: 'space-between' }}>
                  <Skeleton width="100px" height="16px" />
                  <Skeleton width="80px" height="24px" borderRadius="12px" />
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="profile-page">
      <Navbar user={user} />

      <main className="profile-main">
        <div className="page-header">
          <h1>My <span className="gradient-text">Profile</span></h1>
          <p>Manage your personal information and account details</p>
        </div>

        {error && (
          <div className="alert-error" style={{ marginBottom: 20 }}>
            ⚠ {error}
          </div>
        )}

        {user && (
          <div className="profile-grid">
            {/* Identity Card */}
            <div className="glass-card profile-card">
              <div className="profile-avatar-large">
                {user.firstName?.charAt(0).toUpperCase()}
              </div>
              
              {!isEditing ? (
                <>
                  <h2 className="profile-name">{user.firstName} {user.lastName}</h2>
                  <p className="profile-role">{user.role}</p>
                </>
              ) : (
                <div className="profile-edit-inputs">
                  <div className="input-group">
                    <label>First Name</label>
                    <input 
                      type="text" 
                      className="profile-input"
                      value={formData.firstName}
                      onChange={(e) => setFormData({...formData, firstName: e.target.value})}
                    />
                  </div>
                  <div className="input-group">
                    <label>Last Name</label>
                    <input 
                      type="text" 
                      className="profile-input"
                      value={formData.lastName}
                      onChange={(e) => setFormData({...formData, lastName: e.target.value})}
                    />
                  </div>
                  <p className="profile-role">{user.role}</p>
                </div>
              )}

              <div className="profile-details-list">
                <div className="detail-row">
                  <span className="detail-label">Email Address</span>
                  <span className="detail-value text-muted">{user.email}</span>
                </div>
                
                <div className="detail-row">
                  <span className="detail-label">Phone Number</span>
                  {!isEditing ? (
                    <span className="detail-value">{user.phoneNumber || 'Not provided'}</span>
                  ) : (
                    <input 
                      type="text" 
                      className="profile-input profile-input-sm"
                      value={formData.phoneNumber}
                      onChange={(e) => setFormData({...formData, phoneNumber: e.target.value})}
                      placeholder="e.g. +1 555-0100"
                    />
                  )}
                </div>
                <div className="detail-row">
                  <span className="detail-label">Account Status</span>
                  <span className="detail-value">
                     <span className={`status-badge ${user.active ? 'status-active' : 'status-frozen'}`}>
                        {user.active ? 'ACTIVE' : 'INACTIVE'}
                     </span>
                  </span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Member Since</span>
                  <span className="detail-value">{formatDate(user.createdAt)}</span>
                </div>
              </div>

              <div className="profile-actions" style={{ marginTop: '32px', width: '100%', display: 'flex', gap: '12px' }}>
                {!isEditing ? (
                  <button 
                    className="btn-primary" 
                    style={{ width: '100%' }}
                    onClick={() => setIsEditing(true)}
                  >
                    Edit Profile
                  </button>
                ) : (
                  <>
                    <button 
                      className="btn-secondary" 
                      style={{ flex: 1 }}
                      onClick={() => {
                        setIsEditing(false);
                        setFormData({
                          firstName: user.firstName || '',
                          lastName: user.lastName || '',
                          phoneNumber: user.phoneNumber || ''
                        });
                        setError('');
                      }}
                      disabled={isSaving}
                    >
                      Cancel
                    </button>
                    <button 
                      className="btn-primary" 
                      style={{ flex: 1 }}
                      onClick={handleSaveProfile}
                      disabled={isSaving || !formData.firstName || !formData.lastName}
                    >
                      {isSaving ? 'Saving...' : 'Save Changes'}
                    </button>
                  </>
                )}
              </div>
            </div>

            {/* Wallet Info Card */}
            <div className="glass-card wallet-info-card">
              <h3 className="card-title">Wallet Information</h3>
              <p className="card-subtitle">Share your Wallet ID to receive funds from others.</p>
              
              {wallet ? (
                <div className="wallet-details">
                  <div className="wallet-id-box">
                    <span className="wallet-id-label">Your Wallet ID</span>
                    <span className="wallet-id-value">#{wallet.id}</span>
                  </div>
                  <div className="wallet-status-row">
                    <span className="detail-label">Wallet Status</span>
                    <span className={`status-badge ${wallet.status === 'ACTIVE' ? 'status-active' : 'status-frozen'}`}>
                      {wallet.status}
                    </span>
                  </div>
                </div>
              ) : (
                <div className="empty-wallet">
                  <p>No wallet found for this account.</p>
                </div>
              )}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
