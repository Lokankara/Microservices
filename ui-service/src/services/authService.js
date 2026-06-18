import { jwtDecode } from 'jwt-decode';

const AUTH_URL = process.env.REACT_APP_AUTH_BASE_URL || 'http://localhost:9000/auth';

export function login(username, password) {
  const params = new URLSearchParams();
  params.append('grant_type', 'password');
  params.append('username', username);
  params.append('password', password);
  params.append('client_id', 'gateway');

  return fetch(`${AUTH_URL}/oauth2/token`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: params
  }).then(response => {
    if (!response.ok) {
      throw new Error('Invalid credentials');
    }
    return response.json();
  }).then(data => {
    localStorage.setItem('token', data.access_token);
    if (data.refresh_token) {
      localStorage.setItem('refresh_token', data.refresh_token);
    }
    return data;
  });
}

export function logout() {
  localStorage.removeItem('token');
  localStorage.removeItem('refresh_token');
}

export function getToken() {
  return localStorage.getItem('token');
}

export function isAuthenticated() {
  const token = getToken();
  if (!token) return false;
  try {
    const decoded = jwtDecode(token);
    const now = Math.floor(Date.now() / 1000);
    return decoded.exp > now;
  } catch {
    return false;
  }
}

export function decodeToken() {
  const token = getToken();
  if (!token) return null;
  try {
    return jwtDecode(token);
  } catch {
    return null;
  }
}

export function getUserRoles() {
  const decoded = decodeToken();
  if (!decoded) return [];
  const roles = decoded.roles || decoded.authorities || [];
  return roles.map(r => typeof r === 'string' ? r.replace('ROLE_', '') : r);
}
