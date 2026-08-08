// components/TransactionModal.jsx
import { useState } from 'react';
import Modal from './Modal';
import { deposit, withdraw, transfer } from '../services/walletService';
import { useToast } from '../contexts/ToastContext';

export default function TransactionModal({ isOpen, onClose, type, walletId, onSuccess }) {
  const { showToast } = useToast();
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [toWalletId, setToWalletId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Reset form when modal closes
  const handleClose = () => {
    setAmount('');
    setDescription('');
    setToWalletId('');
    setError('');
    onClose();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const numericAmount = parseFloat(amount);
      if (isNaN(numericAmount) || numericAmount <= 0) {
        throw new Error('Please enter a valid amount greater than 0');
      }
      if (numericAmount > 1000000) {
        throw new Error('Amount cannot exceed ₹10,00,000 per transaction');
      }

      if (type === 'DEPOSIT') {
        await deposit(walletId, numericAmount, description);
      } else if (type === 'WITHDRAW') {
        await withdraw(walletId, numericAmount, description);
      } else if (type === 'TRANSFER') {
        if (!toWalletId) throw new Error('Please enter the recipient Wallet ID');
        await transfer(walletId, parseInt(toWalletId), numericAmount, description);
      }

      // Build a human-readable success message
      const formattedAmount = new Intl.NumberFormat('en-IN', {
        style: 'currency', currency: 'INR'
      }).format(parseFloat(amount));

      const successMessages = {
        DEPOSIT:  `Deposit of ${formattedAmount} was successful! 🎉`,
        WITHDRAW: `Withdrawal of ${formattedAmount} was successful!`,
        TRANSFER: `Transfer of ${formattedAmount} to Wallet #${toWalletId} was successful!`,
      };

      showToast(successMessages[type] || 'Transaction successful!', 'success');
      onSuccess();
      handleClose();
    } catch (err) {
      const errMsg = err.response?.data?.message || err.message || 'Transaction failed';
      setError(errMsg);
      showToast(errMsg, 'error');
    } finally {
      setLoading(false);
    }
  };

  const getTitle = () => {
    switch (type) {
      case 'DEPOSIT': return 'Deposit Money';
      case 'WITHDRAW': return 'Withdraw Money';
      case 'TRANSFER': return 'Transfer Money';
      default: return 'Transaction';
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title={getTitle()}>
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        {error && (
          <div className="alert-error" role="alert">
            <span>⚠</span> {error}
          </div>
        )}

        {type === 'TRANSFER' && (
          <div className="form-group">
            <label className="form-label">Recipient Wallet ID</label>
            <input
              type="number"
              className="form-input"
              placeholder="e.g. 2"
              value={toWalletId}
              onChange={(e) => setToWalletId(e.target.value)}
              required
            />
          </div>
        )}

        <div className="form-group">
          <label className="form-label">Amount (₹)</label>
          <input
            type="number"
            className="form-input"
            placeholder="0.00"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            step="0.01"
            min="1"
            max="1000000"
            required
            autoFocus
          />
        </div>

        <div className="form-group">
          <label className="form-label">Description (Optional)</label>
          <input
            type="text"
            className="form-input"
            placeholder={type === 'TRANSFER' ? 'e.g. Rent payment' : 'e.g. Salary'}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>

        <button type="submit" className="btn-primary" disabled={loading} style={{ marginTop: '8px' }}>
          {loading ? 'Processing...' : `Confirm ${type.charAt(0) + type.slice(1).toLowerCase()}`}
        </button>
      </form>
    </Modal>
  );
}
