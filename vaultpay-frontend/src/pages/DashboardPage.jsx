// pages/DashboardPage.jsx
// Main dashboard — shows wallet balance, quick actions, recent transactions.
//
// Data flow:
//   1. On mount → fetch user profile (to get userId + name)
//   2. With userId → fetch wallet (to get walletId + balance)
//   3. With walletId → fetch recent transactions
//
// Why sequential fetches (not parallel)?
//   Each fetch depends on data from the previous one:
//   userId (from profile) → walletId (from wallet) → transactions
//   We can't fetch all three at once because we don't know
//   userId or walletId until the previous call responds.

import { useState, useEffect, useCallback, useMemo } from 'react';
import Navbar from '../components/Navbar';
import TransactionModal from '../components/TransactionModal';
import Skeleton from '../components/Skeleton';
import TransactionChart from '../components/TransactionChart';
import { getMyProfile } from '../services/userService';
import { getWalletByUserId, getTransactionHistory } from '../services/walletService';
import './DashboardPage.css';

// Format a number as Indian currency: 5000.5 → "₹5,000.50"
const formatCurrency = (amount, currency = 'INR') => {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(amount);
};

// Format date: "2024-01-15T10:30:00" → "Jan 15, 10:30 AM"
const formatDate = (dateStr) => {
  return new Date(dateStr).toLocaleDateString('en-IN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export default function DashboardPage() {
  const [user,         setUser]         = useState(null);
  const [wallet,       setWallet]       = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading,      setLoading]      = useState(true);
  const [error,        setError]        = useState('');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalType, setModalType] = useState('DEPOSIT');

  const loadDashboard = useCallback(async () => {
    try {
        // Step 1: Get logged-in user profile
        let userRes;
        try {
          userRes = await getMyProfile();
        } catch (err) {
          const status = err.response?.status ?? 'network error';
          throw new Error(`Step 1 failed — GET /api/users/me → ${status}: ${err.response?.data?.message ?? err.message}`);
        }

        const userData = userRes.data.data;
        setUser(userData);

        // Step 2: Get their wallet using userId
        let walletRes;
        try {
          walletRes = await getWalletByUserId(userData.id);
        } catch (err) {
          const status = err.response?.status ?? 'network error';
          if (status === 404) {
            throw new Error(`No wallet found for your account (userId=${userData.id}). Please create a wallet first via Swagger: POST /api/wallets/${userData.id}`);
          }
          throw new Error(`Step 2 failed — GET /api/wallets/user/${userData.id} → ${status}: ${err.response?.data?.message ?? err.message}`);
        }

        const walletData = walletRes.data.data;
        setWallet(walletData);

        // Step 3: Get recent transactions using walletId (fetch 50 for the chart)
        try {
          const txRes = await getTransactionHistory(walletData.id, 0, 50);
          setTransactions(txRes.data.data?.content ?? []);
        } catch (err) {
          // Transactions failing is non-fatal — just show empty list
          console.warn('Could not load transactions:', err.message);
          setTransactions([]);
        }

      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
  }, []);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  const handleOpenModal = (type) => {
    setModalType(type);
    setIsModalOpen(true);
  };

  const statusColor = {
    ACTIVE: 'status-active',
    FROZEN: 'status-frozen',
    CLOSED: 'status-closed',
  };

  const analytics = useMemo(() => {
    const credits = transactions.filter(t => t.type === 'CREDIT');
    const debits = transactions.filter(t => t.type === 'DEBIT' || t.type === 'TRANSFER');
    
    const largestDeposit = credits.length > 0 
      ? Math.max(...credits.map(t => Number(t.amount)))
      : 0;

    const totalSpend = debits.reduce((sum, t) => sum + Number(t.amount), 0);
    const averageSpend = debits.length > 0 ? totalSpend / debits.length : 0;

    return { largestDeposit, averageSpend, creditsCount: credits.length, debitsCount: debits.length };
  }, [transactions]);

  if (loading) {
    return (
      <div className="dashboard-page">
        <Navbar user={user} />
        <main className="dashboard-main">
          <div className="dashboard-greeting" style={{ marginBottom: '24px' }}>
            <Skeleton width="300px" height="40px" />
            <Skeleton width="200px" height="20px" style={{ marginTop: '8px' }} />
          </div>
          <div className="dashboard-grid">
            <div className="glass-card balance-card">
              <Skeleton width="100px" height="20px" />
              <Skeleton width="200px" height="48px" style={{ margin: '16px 0' }} />
              <Skeleton width="150px" height="16px" />
            </div>
            
            <div className="glass-card actions-card">
              <Skeleton width="150px" height="24px" style={{ marginBottom: '16px' }} />
              <div className="actions-grid">
                <Skeleton width="100%" height="80px" borderRadius="12px" />
                <Skeleton width="100%" height="80px" borderRadius="12px" />
                <Skeleton width="100%" height="80px" borderRadius="12px" />
              </div>
            </div>

            <div className="glass-card analytics-card">
              <Skeleton width="150px" height="24px" style={{ marginBottom: '20px' }} />
              <Skeleton width="100%" height="250px" borderRadius="12px" />
            </div>

            <div className="glass-card transactions-card">
              <Skeleton width="200px" height="24px" style={{ marginBottom: '20px' }} />
              <div className="transaction-list">
                {[...Array(4)].map((_, i) => (
                  <div key={i} className="transaction-item" style={{ gap: '16px' }}>
                    <Skeleton width="40px" height="40px" borderRadius="12px" />
                    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '8px' }}>
                      <Skeleton width="60%" height="16px" />
                      <Skeleton width="40%" height="12px" />
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', alignItems: 'flex-end' }}>
                      <Skeleton width="80px" height="16px" />
                      <Skeleton width="100px" height="12px" />
                    </div>
                  </div>
                ))}
              </div>
            </div>
            
            <div className="stats-col">
              <div className="glass-card stat-card">
                <Skeleton width="100%" height="60px" />
              </div>
              <div className="glass-card stat-card">
                <Skeleton width="100%" height="60px" />
              </div>
            </div>
          </div>
        </main>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard-error">
        <Navbar user={user} />
        <div className="alert-error" style={{ maxWidth: 500, margin: '80px auto' }}>
          ⚠ {error}
        </div>
      </div>
    );
  }



  return (
    <div className="dashboard-page">
      <Navbar user={user} />

      <main className="dashboard-main">
        {/* ── Greeting ─────────────────────────────────────────────── */}
        <div className="dashboard-greeting">
          <h1>Good day, <span className="gradient-text">{user?.firstName}</span> 👋</h1>
          <p>Here&apos;s your financial overview</p>
        </div>

        <div className="dashboard-grid">
          {/* ── ROW 1: Balance & Actions ── */}
          <div className="glass-card balance-card">
            <div className="balance-header">
              <span className="balance-label">Total Balance</span>
              <span className={`wallet-status ${statusColor[wallet?.status] ?? ''}`}>
                {wallet?.status}
              </span>
            </div>
            <div className="balance-amount">
              {formatCurrency(wallet?.balance ?? 0, wallet?.currency)}
            </div>
            <div className="balance-footer">
              <span className="wallet-currency">
                {wallet?.currency} Wallet
              </span>
              <span className="wallet-id">ID #{wallet?.id}</span>
            </div>
            <div className="balance-orb" />
          </div>

          <div className="glass-card actions-card">
            <h3 className="card-title">Quick Actions</h3>
            <div className="actions-grid">
              <button className="action-btn action-deposit" onClick={() => handleOpenModal('DEPOSIT')}>
                <span className="action-icon">+</span>
                <span>Deposit</span>
              </button>
              <button className="action-btn action-withdraw" onClick={() => handleOpenModal('WITHDRAW')}>
                <span className="action-icon">−</span>
                <span>Withdraw</span>
              </button>
              <button className="action-btn action-transfer" onClick={() => handleOpenModal('TRANSFER')}>
                <span className="action-icon">⇄</span>
                <span>Transfer</span>
              </button>
            </div>
          </div>

          {/* ── ROW 2: Analytics Chart ── */}
          <div className="glass-card analytics-card">
            <h3 className="card-title">Income & Expenses</h3>
            <TransactionChart transactions={transactions} />
          </div>

          {/* ── ROW 3: Transactions & Stats ── */}
          <div className="glass-card transactions-card">
            <div className="card-header">
              <h3 className="card-title">Recent Transactions</h3>
              <a href="/transactions" className="view-all-link">View all →</a>
            </div>

            {transactions.length === 0 ? (
              <div className="empty-state">
                <p>No transactions yet.</p>
                <p>Make your first deposit to get started!</p>
              </div>
            ) : (
              <div className="transaction-list">
                {transactions.slice(0, 5).map((tx) => (
                  <div key={tx.id} className="transaction-item">
                    <div className={`tx-type-badge ${tx.type === 'CREDIT' ? 'badge-credit' : 'badge-debit'}`}>
                      {tx.type === 'CREDIT' ? '↓' : '↑'}
                    </div>
                    <div className="tx-details">
                      <span className="tx-description">
                        {tx.description || tx.type}
                      </span>
                      <span className="tx-date">{formatDate(tx.createdAt)}</span>
                    </div>
                    <div className="tx-amount-col">
                      <span className={`tx-amount ${tx.type === 'CREDIT' ? 'amount-credit' : 'amount-debit'}`}>
                        {tx.type === 'CREDIT' ? '+' : '-'}
                        {formatCurrency(tx.amount, wallet?.currency)}
                      </span>
                      <span className="tx-balance">
                        Bal: {formatCurrency(tx.balanceAfter, wallet?.currency)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="stats-col">
            <div className="glass-card stat-card">
              <div className="stat-icon stat-icon-green">↓</div>
              <div className="stat-info">
                <span className="stat-label">Total Credits</span>
                <span className="stat-value" style={{ color: 'var(--color-success)' }}>
                  {analytics.creditsCount} txns
                </span>
              </div>
            </div>

            <div className="glass-card stat-card">
              <div className="stat-icon stat-icon-red">↑</div>
              <div className="stat-info">
                <span className="stat-label">Total Debits</span>
                <span className="stat-value" style={{ color: 'var(--color-danger)' }}>
                  {analytics.debitsCount} txns
                </span>
              </div>
            </div>

            <div className="glass-card stat-card">
              <div className="stat-icon" style={{ background: 'rgba(99,102,241,0.15)', color: 'var(--color-primary-light)' }}>💎</div>
              <div className="stat-info">
                <span className="stat-label">Largest Deposit</span>
                <span className="stat-value">
                  {formatCurrency(analytics.largestDeposit, wallet?.currency)}
                </span>
              </div>
            </div>

            <div className="glass-card stat-card">
              <div className="stat-icon" style={{ background: 'rgba(245,158,11,0.15)', color: '#fcd34d' }}>📊</div>
              <div className="stat-info">
                <span className="stat-label">Average Spend</span>
                <span className="stat-value">
                  {formatCurrency(analytics.averageSpend, wallet?.currency)}
                </span>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Transaction Modal */}
      {wallet && (
        <TransactionModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          type={modalType}
          walletId={wallet.id}
          onSuccess={loadDashboard}
        />
      )}
    </div>
  );
}
