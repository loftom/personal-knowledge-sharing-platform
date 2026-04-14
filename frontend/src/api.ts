import axios from 'axios';
import { clearAdminAuth, clearUserAuth, getActiveAuth, isAdminPath } from './utils/auth';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api'
});

api.interceptors.request.use((config) => {
  const token = getActiveAuth().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => {
    const payload = response.data;
    if (payload && typeof payload.code !== 'undefined' && payload.code !== 0) {
      return Promise.reject({
        response: {
          data: payload,
          status: response.status
        },
        message: payload.message || 'Request failed'
      });
    }
    return response;
  },
  (error) => {
    if (error?.response?.status === 401) {
      if (isAdminPath()) {
        clearAdminAuth();
        window.dispatchEvent(new Event('admin-auth-change'));
      } else {
        clearUserAuth();
        window.dispatchEvent(new Event('auth-change'));
      }
    }
    return Promise.reject(error);
  }
);

export default api;
