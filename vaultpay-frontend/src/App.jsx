// App.jsx — Root component with all routes

import { Routes, Route, Navigate } from 'react-router-dom';
import LoginPage          from './pages/LoginPage';
import RegisterPage       from './pages/RegisterPage';
import DashboardPage      from './pages/DashboardPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage  from './pages/ResetPasswordPage';
import TransactionsPage   from './pages/TransactionsPage';
import ProfilePage        from './pages/ProfilePage';
import AdminDashboardPage from './pages/AdminDashboardPage';
import NotFoundPage       from './pages/NotFoundPage';
import PrivateRoute       from './components/PrivateRoute';
import AdminRoute         from './components/AdminRoute';

function App() {
  return (
    <Routes>
      {/* Default → login */}
      <Route path="/" element={<Navigate to="/login" replace />} />

      {/* Public routes */}
      <Route path="/login"    element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />

      {/* Protected routes — PrivateRoute redirects to /login if no token */}
      <Route path="/dashboard" element={
        <PrivateRoute>
          <DashboardPage />
        </PrivateRoute>
      } />

      <Route path="/profile" element={
        <PrivateRoute>
          <ProfilePage />
        </PrivateRoute>
      } />

      <Route path="/transactions" element={
        <PrivateRoute>
          <TransactionsPage />
        </PrivateRoute>
      } />

      {/* Admin-only route — AdminRoute checks JWT role */}
      <Route path="/admin" element={
        <AdminRoute>
          <AdminDashboardPage />
        </AdminRoute>
      } />

      {/* Catch-all */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}

export default App;
