import React, { useState, useEffect } from 'react';
import axiosInstance from '../services/axiosInstance';
import { getUserRoles } from '../services/authService';

function StoragesTable() {
  const [storages, setStorages] = useState([]);
  const [userRoles, setUserRoles] = useState([]);
  const [newStorage, setNewStorage] = useState({ storageType: '', bucket: '', path: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const roles = getUserRoles();
    setUserRoles(roles);
    fetchStorages();
  }, []);

  const fetchStorages = () => {
    axiosInstance.get('/storages')
      .then(res => {
        setStorages(res.data);
        setLoading(false);
      })
      .catch(err => {
        setError('Failed to load storages');
        setLoading(false);
      });
  };

  const handleAddStorage = async (e) => {
    e.preventDefault();
    try {
      const response = await axiosInstance.post('/storages', newStorage);
      setStorages(prev => [...prev, response.data]);
      setNewStorage({ storageType: '', bucket: '', path: '' });
    } catch (err) {
      setError('Failed to add storage');
    }
  };

  const handleDeleteStorage = async (id) => {
    try {
      await axiosInstance.delete(`/storages/${id}`);
      setStorages(prev => prev.filter(s => s.id !== id));
    } catch (err) {
      setError('Failed to delete storage');
    }
  };

  const isAdmin = userRoles.some(r => r === 'ADMIN' || r === 'Admin');

  if (loading) {
    return <div className="loading">Loading storages...</div>;
  }

  return (
    <div>
      {isAdmin && (
        <form className="add-storage-form" onSubmit={handleAddStorage}>
          <input
            type="text"
            placeholder="Storage Type"
            value={newStorage.storageType}
            onChange={(e) => setNewStorage({ ...newStorage, storageType: e.target.value })}
            required
          />
          <input
            type="text"
            placeholder="Bucket"
            value={newStorage.bucket}
            onChange={(e) => setNewStorage({ ...newStorage, bucket: e.target.value })}
            required
          />
          <input
            type="text"
            placeholder="Path"
            value={newStorage.path}
            onChange={(e) => setNewStorage({ ...newStorage, path: e.target.value })}
            required
          />
          <button type="submit" className="btn">Add Storage</button>
        </form>
      )}

      {error && <p className="error-message">{error}</p>}

      <table className="storages-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Storage Type</th>
            <th>Bucket</th>
            <th>Path</th>
            {isAdmin && <th>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {storages.map(storage => (
            <tr key={storage.id}>
              <td>{storage.id}</td>
              <td>{storage.storageType}</td>
              <td>{storage.bucket}</td>
              <td>{storage.path}</td>
              {isAdmin && (
                <td>
                  <button
                    className="admin-btn"
                    onClick={() => handleDeleteStorage(storage.id)}
                  >
                    Delete
                  </button>
                </td>
              )}
            </tr>
          ))}
          {storages.length === 0 && (
            <tr>
              <td colSpan={isAdmin ? 5 : 4} style={{ textAlign: 'center', padding: '24px' }}>
                No storages found
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

export default StoragesTable;
