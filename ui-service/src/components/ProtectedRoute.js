import { isAuthenticated } from '../services/authService';

function ProtectedRoute({ children }) {
  if (!isAuthenticated()) {
    window.location.href = '/login';
    return null;
  }
  return children;
}

export default ProtectedRoute;