// pages/AdminDashboardPage.jsx
// Admin-only page to manage all registered users.
//
// Features:
// - Paginated user list (10 per page)
// - Client-side search/filter by name or email
// - Toggle user status (activate / deactivate)
// - Toggle user role (USER ↔ ADMIN)
// - Real-time row updates after each action

import { useState, useEffect, useMemo } from 'react';
import Navbar from '../components/Navbar';
import Skeleton from '../components/Skeleton';
import { getMyProfile } from '../services/userService';
import { getAllUsers, updateUserRole, updateUserStatus } from '../services/adminService';
import { useToast } from '../contexts/ToastContext';
import './AdminDashboardPage.css';

const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A';
  return new Date(dateStr).toLocaleDateString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric',
  });
};

export default function AdminDashboardPage() {
  const { showToast } = useToast();
  const [adminUser, setAdminUser]     = useState(null);
  const [users, setUsers]             = useState([]);
  const [page, setPage]               = useState(0);
  const [totalPages, setTotalPages]   = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading]         = useState(true);
  const [actionLoading, setActionLoading] = useState(null); // userId being actioned
  const [error, setError]             = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Load current admin's profile for the Navbar
  useEffect(() => {
    getMyProfile()
      .then(res => setAdminUser(res.data.data))
      .catch(() => {});
  }, []);

  // Load users whenever page changes
  useEffect(() => {
    const fetchUsers = async () => {
      try {
        setLoading(true);
        setError('');
        const res = await getAllUsers(page, 10);
        const paged = res.data.data;
        setUsers(paged.content || []);
        setTotalPages(paged.totalPages || 0);
        setTotalElements(paged.totalElements || 0);
      } catch (err) {
        setError('Failed to load users. Make sure you are logged in as ADMIN.');
      } finally {
        setLoading(false);
      }
    };
    fetchUsers();
  }, [page]);

  // Client-side search: filter the current page's users by name or email
  const filteredUsers = useMemo(() => {
    if (!searchQuery.trim()) return users;
    const q = searchQuery.toLowerCase();
    return users.filter(u =>
      `${u.firstName} ${u.lastName}`.toLowerCase().includes(q) ||
      u.email.toLowerCase().includes(q)
    );
  }, [users, searchQuery]);

  const handleToggleStatus = async (user) => {
    setActionLoading(user.id);
    try {
      const res = await updateUserStatus(user.id, !user.active);
      const updated = res.data.data;
      setUsers(prev => prev.map(u => u.id === updated.id ? updated : u));
      showToast(
        `${updated.firstName}'s account has been ${updated.active ? 'activated' : 'deactivated'}.`,
        updated.active ? 'success' : 'warning'
      );
    } catch {
      showToast(`Failed to update status for ${user.firstName}.`, 'error');
    } finally {
      setActionLoading(null);
    }
  };

  const handleToggleRole = async (user) => {
    const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN';
    setActionLoading(user.id);
    try {
      const res = await updateUserRole(user.id, newRole);
      const updated = res.data.data;
      setUsers(prev => prev.map(u => u.id === updated.id ? updated : u));
      showToast(
        `${updated.firstName}'s role updated to ${updated.role}.`,
        'success'
      );
    } catch {
      showToast(`Failed to update role for ${user.firstName}.`, 'error');
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className="admin-page">
      <Navbar user={adminUser} />

      <main className="admin-main">
        {/* ── Page Header ── */}
        <div className="page-header">
          <div>
            <h1>Admin <span className="gradient-text">Dashboard</span></h1>
            <p>Manage all registered users — {totalElements} total accounts</p>
          </div>
        </div>

        {error && (
          <div className="alert-error" style={{ marginBottom: 20 }}>⚠ {error}</div>
        )}

        {/* ── Search Bar ── */}
        <div className="glass-card search-bar-card">
          <div className="search-input-wrapper">
            <span className="search-icon">🔍</span>
            <input
              type="text"
              className="form-input search-input"
              placeholder="Search by name or email..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
            />
          </div>
        </div>

        {/* ── Users Table ── */}
        <div className="glass-card table-card">
          {loading ? (
            <div className="table-responsive">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>User</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Joined</th>
                    <th className="text-center">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {[...Array(5)].map((_, i) => (
                    <tr key={i}>
                      <td>
                        <div className="user-cell">
                          <Skeleton className="skeleton-avatar" width="36px" height="36px" />
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                            <Skeleton width="100px" />
                            <Skeleton width="60px" height="12px" />
                          </div>
                        </div>
                      </td>
                      <td><Skeleton width="150px" /></td>
                      <td><Skeleton width="60px" height="24px" borderRadius="12px" /></td>
                      <td><Skeleton width="70px" height="24px" borderRadius="12px" /></td>
                      <td><Skeleton width="90px" /></td>
                      <td>
                        <div className="action-buttons" style={{ justifyContent: 'center' }}>
                          <Skeleton width="80px" height="30px" borderRadius="4px" />
                          <Skeleton width="80px" height="30px" borderRadius="4px" />
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : filteredUsers.length === 0 ? (
            <div className="empty-state">
              {searchQuery ? `No users found for "${searchQuery}"` : 'No users found.'}
            </div>
          ) : (
            <>
              <div className="table-responsive">
                <table className="admin-table">
                  <thead>
                    <tr>
                      <th>User</th>
                      <th>Email</th>
                      <th>Role</th>
                      <th>Status</th>
                      <th>Joined</th>
                      <th className="text-center">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredUsers.map(user => {
                      const isActioning = actionLoading === user.id;
                      return (
                        <tr key={user.id} className={isActioning ? 'row-loading' : ''}>
                          <td>
                            <div className="user-cell">
                              <div className="user-avatar-sm">
                                {user.firstName?.charAt(0).toUpperCase()}
                              </div>
                              <div>
                                <div className="user-cell-name">
                                  {user.firstName} {user.lastName}
                                </div>
                                <div className="user-cell-id">ID #{user.id}</div>
                              </div>
                            </div>
                          </td>
                          <td className="text-muted">{user.email}</td>
                          <td>
                            <span className={`role-badge ${user.role === 'ADMIN' ? 'role-admin' : 'role-user'}`}>
                              {user.role}
                            </span>
                          </td>
                          <td>
                            <span className={`status-badge ${user.active ? 'status-active' : 'status-frozen'}`}>
                              {user.active ? 'ACTIVE' : 'INACTIVE'}
                            </span>
                          </td>
                          <td className="text-muted">{formatDate(user.createdAt)}</td>
                          <td>
                            <div className="action-buttons">
                              <button
                                className={`btn-action ${user.active ? 'btn-freeze' : 'btn-activate'}`}
                                onClick={() => handleToggleStatus(user)}
                                disabled={isActioning}
                              >
                                {isActioning ? '...' : user.active ? 'Deactivate' : 'Activate'}
                              </button>
                              <button
                                className={`btn-action ${user.role === 'ADMIN' ? 'btn-demote' : 'btn-promote'}`}
                                onClick={() => handleToggleRole(user)}
                                disabled={isActioning}
                              >
                                {isActioning ? '...' : user.role === 'ADMIN' ? '→ USER' : '→ ADMIN'}
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="pagination">
                  <span className="pagination-info">
                    Page {page + 1} of {totalPages}
                  </span>
                  <div className="pagination-buttons">
                    <button className="btn-page" onClick={() => setPage(p => p - 1)} disabled={page === 0 || loading}>
                      ← Prev
                    </button>
                    <button className="btn-page" onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1 || loading}>
                      Next →
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </main>
    </div>
  );
}
