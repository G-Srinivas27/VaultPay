// services/walletService.js
// All wallet-related API calls.

import api from './api';

/**
 * Fetches the wallet for the given user ID.
 * GET /api/wallets/user/{userId}
 *
 * Spring Boot response:
 * { "success": true, "data": { "id": 1, "balance": 5000.00, "currency": "INR", "status": "ACTIVE" } }
 *
 * @param {number} userId
 */
export const getWalletByUserId = (userId) => {
  return api.get(`/api/wallets/user/${userId}`);
};

/**
 * Deposits money into a wallet.
 * POST /api/transactions/{walletId}/deposit
 *
 * @param {number} walletId
 * @param {number} amount
 * @param {string} description
 */
export const deposit = (walletId, amount, description = '') => {
  return api.post(`/api/transactions/${walletId}/deposit`, { amount, description });
};

/**
 * Withdraws money from a wallet.
 * POST /api/transactions/{walletId}/withdraw
 *
 * @param {number} walletId
 * @param {number} amount
 * @param {string} description
 */
export const withdraw = (walletId, amount, description = '') => {
  return api.post(`/api/transactions/${walletId}/withdraw`, { amount, description });
};

/**
 * Transfers money to another wallet.
 * POST /api/transactions/{walletId}/transfer
 *
 * @param {number} walletId - sender's wallet ID
 * @param {number} toWalletId - receiver's wallet ID
 * @param {number} amount
 * @param {string} description
 */
export const transfer = (walletId, toWalletId, amount, description = '') => {
  return api.post(`/api/transactions/${walletId}/transfer`, { toWalletId, amount, description });
};

/**
 * Fetches transaction history for a wallet.
 * GET /api/transactions/{walletId}/history
 *
 * @param {number} walletId
 * @param {number} page
 * @param {number} size
 */
export const getTransactionHistory = (walletId, page = 0, size = 5) => {
  return api.get(`/api/transactions/${walletId}/history?page=${page}&size=${size}`);
};
