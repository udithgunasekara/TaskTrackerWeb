import axios from 'axios';
import { useAuthStore } from '../stores/authStore';

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082',
  headers: {
    'Content-Type': 'application/json',
  },
});

axiosClient.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

axiosClient.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body && Array.isArray(body.results)) {
      response.data = body.results[0];
    }
    return response;
  },
  (error) => {
    const body = error.response?.data;
    if (body && Array.isArray(body.results)) {
      error.response.data = body.results[0];
    }
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
