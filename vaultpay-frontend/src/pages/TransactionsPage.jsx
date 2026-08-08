// pages/TransactionsPage.jsx
// Full transaction history page with search, type filters, and pagination.

import { useState, useEffect, useMemo } from 'react';
import Navbar from '../components/Navbar';
import Skeleton from '../components/Skeleton';
import { getMyProfile } from '../services/userService';
import { getWalletByUserId, getTransactionHistory } from '../services/walletService';
import './TransactionsPage.css';

const formatCurrency = (amount, currency = 'INR') => {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(amount);
};

const formatDate = (dateStr) => {
  return new Date(dateStr).toLocaleString('en-IN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

// Filter options
const FILTERS = ['ALL', 'CREDIT', 'DEBIT'];

export default function TransactionsPage() {
  const [user,         setUser]         = useState(null);
  const [wallet,       setWallet]       = useState(null);
  const [transactions, setTransactions] = useState([]);

  // Pagination
  const [page,          setPage]          = useState(0);
  const [totalPages,    setTotalPages]    = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filters
  const [activeFilter,  setActiveFilter]  = useState('ALL');
  const [searchQuery,   setSearchQuery]   = useState('');

  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');

  // ── Data Fetching ────────────────────────────────────────────────────────
  useEffect(() => {
    const fetchHistory = async () => {
      try {
        setLoading(true);
        let currentUser   = user;
        let currentWallet = wallet;

        // Fetch user and wallet only once
        if (!currentUser || !currentWallet) {
          const userRes   = await getMyProfile();
          currentUser     = userRes.data.data;
          setUser(currentUser);

          const walletRes  = await getWalletByUserId(currentUser.id);
          const walletData = walletRes.data.data;
          currentWallet    = Array.isArray(walletData) ? walletData[0] : walletData;
          setWallet(currentWallet);
        }

        // Fetch paginated transactions (10 per page)
        const txRes    = await getTransactionHistory(currentWallet.id, page, 10);
        const pagedData = txRes.data.data;

        setTransactions(pagedData.content    || []);
        setTotalPages(pagedData.totalPages   || 0);
        setTotalElements(pagedData.totalElements || 0);

      } catch (err) {
        setError('Failed to load transaction history.');
      } finally {
        setLoading(false);
      }
    };

    fetchHistory();
  }, [page]); // Re-fetch only when page changes

  // ── Client-side Filtering ────────────────────────────────────────────────
  // Filter and search are applied in-memory on the current page's data.
  // This avoids extra API calls and keeps the UI instant.
  const filteredTransactions = useMemo(() => {
    let result = transactions;

    // Type filter
    if (activeFilter !== 'ALL') {
      result = result.filter(tx => tx.type === activeFilter);
    }

    // Description search (case-insensitive)
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      result = result.filter(tx =>
        (tx.description || '').toLowerCase().includes(q)
      );
    }

    return result;
  }, [transactions, activeFilter, searchQuery]);

  // ── Summary Stats (for current page's filtered view) ────────────────────
  const stats = useMemo(() => {
    const credits = filteredTransactions
      .filter(tx => tx.type === 'CREDIT')
      .reduce((sum, tx) => sum + Number(tx.amount), 0);
    const debits = filteredTransactions
      .filter(tx => tx.type === 'DEBIT')
      .reduce((sum, tx) => sum + Number(tx.amount), 0);
    return { credits, debits };
  }, [filteredTransactions]);

  // Reset to page 0 when filter or search changes
  const handleFilterChange = (filter) => {
    setActiveFilter(filter);
    setPage(0);
  };

  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
    setPage(0);
  };

  return (
    <div className="transactions-page">
      <Navbar user={user} />

      <main className="transactions-main">

        {/* ── Page Header ── */}
        <div className="page-header">
          <h1>Transaction <span className="gradient-text">History</span></h1>
          <p>View and filter all your past deposits, withdrawals, and transfers.</p>
        </div>

        {error && (
          <div className="alert-error" style={{ marginBottom: 20 }}>⚠ {error}</div>
        )}

        {/* ── Filters & Search Bar ── */}
        <div className="glass-card filters-card">
          {/* Type Toggle Buttons */}
          <div className="filter-toggle-group">
            {FILTERS.map(f => (
              <button
                key={f}
                className={`filter-btn ${activeFilter === f ? `filter-btn-active filter-btn-${f.toLowerCase()}` : ''}`}
                onClick={() => handleFilterChange(f)}
              >
                {f === 'CREDIT' && '↑ '}
                {f === 'DEBIT'  && '↓ '}
                {f}
              </button>
            ))}
          </div>

          {/* Description Search */}
          <div className="search-input-wrapper">
            <span className="search-icon">🔍</span>
            <input
              type="text"
              className="form-input search-input"
              placeholder="Search by description..."
              value={searchQuery}
              onChange={handleSearchChange}
            />
            {searchQuery && (
              <button className="search-clear-btn" onClick={() => setSearchQuery('')}>
                ✕
              </button>
            )}
          </div>
        </div>

        {/* ── Summary Stats ── */}
        {!loading && filteredTransactions.length > 0 && (
          <div className="stats-row">
            <div className="stat-pill stat-credit">
              <span className="stat-label">Credits this page</span>
              <span className="stat-value">+{formatCurrency(stats.credits, wallet?.currency)}</span>
            </div>
            <div className="stat-pill stat-debit">
              <span className="stat-label">Debits this page</span>
              <span className="stat-value">−{formatCurrency(stats.debits, wallet?.currency)}</span>
            </div>
            <div className="stat-pill stat-count">
              <span className="stat-label">Showing</span>
              <span className="stat-value">{filteredTransactions.length} transactions</span>
            </div>
          </div>
        )}

        {/* ── Transactions Table ── */}
        <div className="glass-card table-card">
          {loading && transactions.length === 0 ? (
            <div className="table-responsive">
              <table className="transactions-table">
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>Date</th>
                    <th>Description</th>
                    <th className="text-right">Amount</th>
                    <th className="text-right">Balance After</th>
                  </tr>
                </thead>
                <tbody>
                  {[...Array(5)].map((_, i) => (
                    <tr key={i}>
                      <td data-label="Type"><Skeleton width="80px" height="28px" borderRadius="14px" /></td>
                      <td data-label="Date"><Skeleton width="120px" /></td>
                      <td data-label="Description"><Skeleton width="180px" /></td>
                      <td className="text-right" data-label="Amount"><Skeleton width="100px" style={{ display: 'inline-block' }} /></td>
                      <td className="text-right" data-label="Balance After"><Skeleton width="100px" style={{ display: 'inline-block' }} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : filteredTransactions.length === 0 ? (
            <div className="empty-state">
              {searchQuery || activeFilter !== 'ALL'
                ? `No ${activeFilter !== 'ALL' ? activeFilter.toLowerCase() + ' ' : ''}transactions found${searchQuery ? ` for "${searchQuery}"` : ''}.`
                : 'No transactions yet.'}
            </div>
          ) : (
            <>
              <div className="table-responsive">
                <table className="transactions-table">
                  <thead>
                    <tr>
                      <th>Type</th>
                      <th>Date</th>
                      <th>Description</th>
                      <th className="text-right">Amount</th>
                      <th className="text-right">Balance After</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredTransactions.map((tx) => (
                      <tr key={tx.id}>
                        <td data-label="Type">
                          <div className={`tx-type-badge ${tx.type === 'CREDIT' ? 'badge-credit' : 'badge-debit'}`}>
                            {tx.type === 'CREDIT' ? '↑' : '↓'} {tx.type}
                          </div>
                        </td>
                        <td className="tx-date-cell" data-label="Date">{formatDate(tx.createdAt)}</td>
                        <td className="tx-desc-cell" data-label="Description">{tx.description || '—'}</td>
                        <td className="text-right" data-label="Amount">
                          <span className={`tx-amount-cell ${tx.type === 'CREDIT' ? 'text-success' : 'text-danger'}`}>
                            {tx.type === 'CREDIT' ? '+' : '−'}
                            {formatCurrency(tx.amount, wallet?.currency)}
                          </span>
                        </td>
                        <td className="text-right text-muted" data-label="Balance After">
                          {formatCurrency(tx.balanceAfter, wallet?.currency)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Pagination Controls */}
              {totalPages > 1 && (
                <div className="pagination">
                  <span className="pagination-info">
                    Page {page + 1} of {totalPages} &nbsp;·&nbsp; {totalElements} total transactions
                  </span>
                  <div className="pagination-buttons">
                    <button
                      className="btn-page"
                      onClick={() => setPage(p => p - 1)}
                      disabled={page === 0 || loading}
                    >
                      ← Prev
                    </button>
                    <button
                      className="btn-page"
                      onClick={() => setPage(p => p + 1)}
                      disabled={page >= totalPages - 1 || loading}
                    >
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
