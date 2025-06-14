import axios from 'axios';
import { getAuthToken } from './firebase';

const API_URL = '/api';

// Create axios instance
const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Check if we're in development mode
const isDevelopment = window.location.hostname === 'localhost' || 
                     window.location.hostname === '127.0.0.1';

// Add auth token to requests
api.interceptors.request.use(async (config) => {
  try {
    const token = await getAuthToken();
    if (token) {
      console.log("Adding auth token to request");
      config.headers.Authorization = `Bearer ${token}`;
    } else if (isDevelopment) {
      // In development mode, add a special header
      console.log("Adding development auth header");
      
      // Check if we're using dev mode from localStorage
      if (localStorage.getItem('devMode') === 'true') {
        // Use a special dev token that the backend will recognize
        config.headers.Authorization = 'Bearer dev-token';
        config.headers['X-Dev-Email'] = 'ylalit0022@gmail.com'; // Your admin email
        config.headers['X-Dev-Admin'] = 'true'; // Signal this is a dev admin request
      } else {
        console.warn("No auth token available and not in dev mode");
      }
    } else {
      console.warn("No auth token available for request");
    }
    return config;
  } catch (error) {
    console.error("Error in request interceptor:", error);
    return Promise.reject(error);
  }
}, (error) => {
  console.error("Request interceptor error:", error);
  return Promise.reject(error);
});

// Add response interceptor for debugging
api.interceptors.response.use((response) => {
  console.log(`API Response [${response.config.method.toUpperCase()}] ${response.config.url}:`, response.status);
  return response;
}, (error) => {
  if (error.response) {
    console.error(`API Error [${error.config.method.toUpperCase()}] ${error.config.url}:`, error.response.status, error.response.data);
  } else {
    console.error("API Error:", error.message || error);
  }
  return Promise.reject(error);
});

// Admin verification
export const verifyAdmin = async () => {
  try {
    console.log('Verifying admin status...');
    
    // In development mode with dev login, return mock admin data
    if (isDevelopment && localStorage.getItem('devMode') === 'true') {
      console.log('Using development admin verification');
      return {
        isAdmin: true,
        role: 'superAdmin',
        email: 'ylalit0022@gmail.com'
      };
    }
    
    const response = await api.get('/admin/verify');
    console.log('Admin verification response:', response.data);
    return response.data;
  } catch (error) {
    console.error('Error verifying admin status:', error);
    if (error.response && error.response.status === 401) {
      console.error('Authentication error: Not authorized');
    } else if (error.response) {
      console.error('Server error:', error.response.status, error.response.data);
    } else if (error.request) {
      console.error('No response received from server');
    } else {
      console.error('Error setting up request:', error.message);
    }
    throw error;
  }
};

// User management
export const getUsers = async (page = 1, limit = 10, sort = 'createdAt', order = 'desc', filters = {}) => {
  try {
    const response = await api.get('/admin/users', {
      params: {
        page,
        limit,
        sort,
        order,
        ...filters
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching users:', error);
    throw error;
  }
};

export const getUserById = async (uid) => {
  try {
    const response = await api.get(`/admin/users/${uid}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching user ${uid}:`, error);
    throw error;
  }
};

export const updateUser = async (uid, userData) => {
  try {
    const response = await api.put(`/admin/users/${uid}`, userData);
    return response.data;
  } catch (error) {
    console.error(`Error updating user ${uid}:`, error);
    throw error;
  }
};

export const blockUser = async (uid, reason) => {
  try {
    const response = await api.post(`/admin/users/${uid}/block`, { reason });
    return response.data;
  } catch (error) {
    console.error(`Error blocking user ${uid}:`, error);
    throw error;
  }
};

export const unblockUser = async (uid) => {
  try {
    const response = await api.post(`/admin/users/${uid}/unblock`);
    return response.data;
  } catch (error) {
    console.error(`Error unblocking user ${uid}:`, error);
    throw error;
  }
};

// Dashboard statistics
export const getDashboardStats = async () => {
  try {
    const response = await api.get('/admin/dashboard/stats');
    return response.data;
  } catch (error) {
    console.error('Error fetching dashboard stats:', error);
    throw error;
  }
};

export default api; 