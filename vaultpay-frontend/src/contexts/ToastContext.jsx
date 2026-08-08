// contexts/ToastContext.jsx
// Global toast notification system.
//
// How it works:
// 1. ToastProvider holds a list of active toasts in state
// 2. showToast(message, type) adds a toast to the list
// 3. Each toast auto-removes itself after 4 seconds
// 4. The ToastContainer renders all active toasts as an overlay
// 5. Any child component can call useToast() to access showToast()
//
// Types supported: 'success' | 'error' | 'warning' | 'info'

import { createContext, useContext, useState, useCallback } from 'react';
import './Toast.css';

// ── Context ────────────────────────────────────────────────────────────────
const ToastContext = createContext(null);

// ── Custom Hook ────────────────────────────────────────────────────────────
// Usage in any component:
//   const { showToast } = useToast();
//   showToast('Deposit successful!', 'success');
export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used inside <ToastProvider>');
  return ctx;
}

// ── Icons per toast type ───────────────────────────────────────────────────
const ICONS = {
  success: '✓',
  error:   '✕',
  warning: '⚠',
  info:    'ℹ',
};

// ── Individual Toast Component ─────────────────────────────────────────────
function ToastItem({ toast, onRemove }) {
  return (
    <div
      className={`toast toast-${toast.type}`}
      role="alert"
      aria-live="polite"
    >
      <div className="toast-icon">{ICONS[toast.type]}</div>
      <div className="toast-body">
        <span className="toast-message">{toast.message}</span>
      </div>
      <button
        className="toast-close"
        onClick={() => onRemove(toast.id)}
        aria-label="Dismiss notification"
      >
        ✕
      </button>
      {/* Progress bar that shrinks over toast duration */}
      <div className="toast-progress" />
    </div>
  );
}

// ── Provider ────────────────────────────────────────────────────────────────
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const removeToast = useCallback((id) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  const showToast = useCallback((message, type = 'info', duration = 4000) => {
    const id = Date.now() + Math.random(); // unique ID
    setToasts(prev => [...prev, { id, message, type }]);

    // Auto-remove after duration
    setTimeout(() => removeToast(id), duration);
  }, [removeToast]);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}

      {/* Toast Container — fixed overlay, renders all active toasts */}
      {toasts.length > 0 && (
        <div className="toast-container" aria-label="Notifications">
          {toasts.map(toast => (
            <ToastItem key={toast.id} toast={toast} onRemove={removeToast} />
          ))}
        </div>
      )}
    </ToastContext.Provider>
  );
}
