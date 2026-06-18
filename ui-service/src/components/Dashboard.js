import React from 'react';
import { getUserRoles, decodeToken, logout } from '../services/authService';
import StoragesTable from './StoragesTable';

function Dashboard() {
  const decoded = decodeToken();
  const roles = getUserRoles();
  const username = decoded ? decoded.sub : '';

  const handleLogout = () => {
    logout();
    window.location.href = '/login';
  };

  return (
    <div>
      <nav className="navbar">
        <h3>Storages Management</h3>
        <button className="logout-btn" onClick={handleLogout}>Logout</button>
      </nav>
      <div className="container">
        <div className="user-info">
          <p><strong>User:</strong> {username}</p>
          <p><strong>Roles:</strong> {roles.join(', ') || 'None'}</p>
        </div>
        <StoragesTable />
      </div>
    </div>
  );
}

export default Dashboard;
