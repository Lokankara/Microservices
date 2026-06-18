import axios from 'axios';
import { getToken, logout } from './authService';

const axiosInstance = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || ''
});

axiosInstance.interceptors.request.use(config => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosInstance.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      logout();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
