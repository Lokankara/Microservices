import React from 'react';
import { isAuthenticated } from './services/authService';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
  const path = window.location.pathname;
  const authenticated = isAuthenticated();

  if (path === '/login' && !authenticated) {
    return (
      <Login onLoginSuccess={() => { window.location.href = '/dashboard'; }} />
    );
  }

  if (!authenticated) {
    return (
      <Login onLoginSuccess={() => { window.location.href = '/dashboard'; }} />
    );
  }

  if (path === '/dashboard') {
    return (
      <ProtectedRoute>
        <Dashboard />
      </ProtectedRoute>
    );
  }

  return (
    <ProtectedRoute>
      <Dashboard />
    </ProtectedRoute>
  );
}

export default App;
