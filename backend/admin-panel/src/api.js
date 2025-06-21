import axios from 'axios';
import { getAuthToken } from './firebase';
import mockData from './utils/mockData';

// Check if we're in development mode
const isDevelopment = window.location.hostname === 'localhost' || 
                     window.location.hostname === '127.0.0.1';

// Get the port from the current URL or default to 3000
const currentPort = window.location.port || '3000';
// Backend port - in development, use 3001 as specified in the backend server.js
const backendPort = '3001';

// Set API base URL - use absolute URL for development, relative for production
// In production, the API is served from the same origin, so we use a relative path
// In development, we need to specify the full URL
const API_URL = isDevelopment 
  ? `http://${window.location.hostname}:${backendPort}/api` 
  : '/api';
console.log('API base URL set to:', API_URL);

// Create axios instance
export const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true, // Include credentials with requests
  timeout: 10000, // Reduce timeout to 10 seconds (from 60 seconds)
});

// Add auth token to requests
api.interceptors.request.use(async (config) => {
  try {
    // Log the request URL for debugging
    console.log(`Making ${config.method?.toUpperCase() || 'GET'} request to: ${config.baseURL}${config.url}`);
    
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
        // For development, always set dev mode to true if not set
        localStorage.setItem('devMode', 'true');
        config.headers.Authorization = 'Bearer dev-token';
        config.headers['X-Dev-Email'] = 'ylalit0022@gmail.com';
        config.headers['X-Dev-Admin'] = 'true';
        console.log("Dev mode automatically enabled for development environment");
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
    console.error(`API Error [${error.config?.method?.toUpperCase()}] ${error.config?.url}:`, error.response.status, error.response.data);
  } else if (error.code === 'ECONNABORTED') {
    console.error(`Request timeout: ${error.message}`);
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

// New function to get user by MongoDB ObjectId
export const getUserByObjectId = async (id) => {
  try {
    if (!id) {
      console.error('Invalid user ID provided');
      return { success: false, message: 'Invalid user ID' };
    }
    
    const response = await api.get(`/admin/users/by-id/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching user by ObjectId ${id}:`, error);
    return { success: false, message: `Error fetching user: ${error.message}` };
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

// Template management
export const getTemplates = async (page = 1, limit = 10, sort = 'createdAt', order = 'desc', filters = {}) => {
  try {
    const response = await api.get('/admin/templates', {
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
    console.error('Error fetching templates:', error);
    throw error;
  }
};

export const getTemplateById = async (id) => {
  try {
    const response = await api.get(`/admin/templates/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching template ${id}:`, error);
    throw error;
  }
};

export const createTemplate = async (templateData) => {
  try {
    // Create a sanitized copy of the template data
    const sanitizedData = { ...templateData };
    
    // Handle ObjectId references - remove null/empty values instead of converting to empty strings
    if (sanitizedData.language === null || sanitizedData.language === '') {
      console.log('Removing null/empty language field');
      delete sanitizedData.language;
    } else if (sanitizedData.language !== undefined && typeof sanitizedData.language !== 'string') {
      sanitizedData.language = String(sanitizedData.language);
    }
    
    if (sanitizedData.region === null || sanitizedData.region === '') {
      console.log('Removing null/empty region field');
      delete sanitizedData.region;
    } else if (sanitizedData.region !== undefined && typeof sanitizedData.region !== 'string') {
      sanitizedData.region = String(sanitizedData.region);
    }
    
    if (sanitizedData.variationOf === null || sanitizedData.variationOf === '') {
      console.log('Removing null/empty variationOf field');
      delete sanitizedData.variationOf;
    } else if (sanitizedData.variationOf !== undefined && typeof sanitizedData.variationOf !== 'string') {
      sanitizedData.variationOf = String(sanitizedData.variationOf);
    }
    
    // Ensure relatedTemplates are strings and remove null/empty values
    if (sanitizedData.relatedTemplates && Array.isArray(sanitizedData.relatedTemplates)) {
      sanitizedData.relatedTemplates = sanitizedData.relatedTemplates
        .filter(id => id !== null && id !== undefined && id !== '')
        .map(id => typeof id === 'string' ? id : String(id));
    }
    
    console.log('Sending sanitized template data:', {
      language: sanitizedData.language,
      languageType: typeof sanitizedData.language
    });
    
    const response = await api.post('/admin/templates', sanitizedData);
    return response.data;
  } catch (error) {
    console.error('Error creating template:', error);
    
    // Enhanced error logging
    if (error.response && error.response.data) {
      console.error('Error response data:', error.response.data);
    }
    
    throw error;
  }
};

export const updateTemplate = async (id, templateData) => {
  try {
    // Create a sanitized copy of the template data
    const sanitizedData = { ...templateData };
    
    // Handle ObjectId references - remove null/empty values instead of converting to empty strings
    if (sanitizedData.language === null || sanitizedData.language === '') {
      console.log('Removing null/empty language field');
      delete sanitizedData.language;
    } else if (sanitizedData.language !== undefined && typeof sanitizedData.language !== 'string') {
      sanitizedData.language = String(sanitizedData.language);
      console.log('Sanitized language field:', sanitizedData.language, typeof sanitizedData.language);
    }
    
    if (sanitizedData.region === null || sanitizedData.region === '') {
      console.log('Removing null/empty region field');
      delete sanitizedData.region;
    } else if (sanitizedData.region !== undefined && typeof sanitizedData.region !== 'string') {
      sanitizedData.region = String(sanitizedData.region);
    }
    
    if (sanitizedData.variationOf === null || sanitizedData.variationOf === '') {
      console.log('Removing null/empty variationOf field');
      delete sanitizedData.variationOf;
    } else if (sanitizedData.variationOf !== undefined && typeof sanitizedData.variationOf !== 'string') {
      sanitizedData.variationOf = String(sanitizedData.variationOf);
    }
    
    // Ensure relatedTemplates are strings and remove null/empty values
    if (sanitizedData.relatedTemplates && Array.isArray(sanitizedData.relatedTemplates)) {
      sanitizedData.relatedTemplates = sanitizedData.relatedTemplates
        .filter(id => id !== null && id !== undefined && id !== '')
        .map(id => typeof id === 'string' ? id : String(id));
    }
    
    console.log(`Sending sanitized template update for ${id}:`, {
      language: sanitizedData.language,
      languageType: typeof sanitizedData.language
    });
    
    const response = await api.put(`/admin/templates/${id}`, sanitizedData);
    return response.data;
  } catch (error) {
    console.error(`Error updating template ${id}:`, error);
    
    // Enhanced error logging
    if (error.response && error.response.data) {
      console.error('Error response data:', error.response.data);
      
      // Check for specific MongoDB error about language field
      const errorMessage = error.response.data.error || '';
      if (errorMessage.includes('language override field') && errorMessage.includes('non-string type')) {
        console.error('Field error in language:', templateData.language);
      }
    }
    
    throw error;
  }
};

export const deleteTemplate = async (id) => {
  try {
    const response = await api.delete(`/admin/templates/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error deleting template ${id}:`, error);
    throw error;
  }
};

export const toggleTemplateStatus = async (id) => {
  try {
    const response = await api.patch(`/admin/templates/${id}/toggle-status`);
    return response.data;
  } catch (error) {
    console.error(`Error toggling template status ${id}:`, error);
    throw error;
  }
};

export const exportTemplatesCSV = async (filters = {}) => {
  try {
    console.log('Exporting templates as CSV with filters:', filters);
    
    // Use a direct fetch instead of axios to avoid potential issues with blob handling
    const queryParams = new URLSearchParams();
    
    // Add filters to query params
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        queryParams.append(key, value);
      }
    });
    
    const queryString = queryParams.toString();
    const url = `/api/admin/templates/export-csv${queryString ? `?${queryString}` : ''}`;
    
    console.log('Fetching CSV from URL:', url);
    
    // Get auth token using the same method as other API calls
    const token = await getAuthToken();
    const headers = {
      'Authorization': token ? `Bearer ${token}` : ''
    };
    
    // Add development mode headers if needed
    if (localStorage.getItem('devMode') === 'true') {
      headers['X-Dev-Email'] = 'ylalit0022@gmail.com';
      headers['X-Dev-Admin'] = 'true';
    }
    
    const response = await fetch(url, {
      method: 'GET',
      headers: headers
    });
    
    if (!response.ok) {
      // Handle error response
      const errorText = await response.text();
      console.error('CSV export error response:', errorText);
      try {
        const errorJson = JSON.parse(errorText);
        throw new Error(errorJson.message || errorJson.error || 'Error exporting CSV');
      } catch (parseError) {
        throw new Error(`Error exporting CSV: ${response.status} ${response.statusText}`);
      }
    }
    
    // Get the blob from the response
    const blob = await response.blob();
    
    // Create a download link for the CSV file
    const url2 = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.style.display = 'none';
    a.href = url2;
    a.download = `templates_export_${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url2);
    document.body.removeChild(a);
    
    return { success: true };
  } catch (error) {
    console.error('Error exporting templates CSV:', error);
    throw error;
  }
};

export const importTemplatesCSV = async (file) => {
  try {
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await api.post('/admin/templates/import-csv', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    
    // Store import results in localStorage for dashboard notification
    if (response.data.success) {
      const importResults = {
        timestamp: Date.now(),
        created: response.data.created,
        updated: response.data.updated,
        errors: response.data.errors || 0,
        total: response.data.total
      };
      localStorage.setItem('templateImportResults', JSON.stringify(importResults));
    }
    
    return response.data;
  } catch (error) {
    console.error('Error importing templates CSV:', error);
    throw error;
  }
};

// AdMob management
export const getAdMobs = async (page = 1, limit = 10, sort = 'createdAt', order = 'desc', filters = {}) => {
  try {
    const response = await api.get('/admin/admob', {
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
    console.error('Error fetching AdMob ads:', error);
    throw error;
  }
};

export const getAdMobById = async (id) => {
  try {
    console.log(`Fetching AdMob ad with ID: "${id}"`, typeof id);
    
    if (!id) {
      console.error('Invalid AdMob ID: ID is undefined or empty');
      return {
        success: false,
        message: 'Invalid AdMob ID',
        error: 'ID is undefined or empty'
      };
    }
    
    // Ensure we're using the correct URL format
    // API_URL already contains '/api' so we don't need to add it again
    const url = `/admin/admob/${id}`;
    console.log(`Making GET request to: ${API_URL}${url}`);
    
    // Add additional logging to debug the request
    console.log('Full request URL:', `${window.location.origin}${API_URL}${url}`);
    console.log('API base URL:', API_URL);
    
    const response = await api.get(url);
    console.log('AdMob ad fetch response:', response.data);
    
    return response.data;
  } catch (error) {
    console.error(`Error fetching AdMob ad ${id}:`, error);
    console.error('Error details:', error.response?.data || error.message);
    throw error;
  }
};

export const createAdMob = async (adMobData) => {
  try {
    const response = await api.post('/admin/admob', adMobData);
    return response.data;
  } catch (error) {
    console.error('Error creating AdMob ad:', error);
    throw error;
  }
};

export const updateAdMob = async (id, adMobData) => {
  try {
    if (!id) {
      return {
        success: false,
        message: 'Invalid AdMob ID',
        error: 'ID is undefined or empty'
      };
    }
    
    const response = await api.put(`/admin/admob/${id}`, adMobData);
    return response.data;
  } catch (error) {
    console.error(`Error updating AdMob ad ${id}:`, error);
    throw error;
  }
};

export const deleteAdMob = async (id) => {
  try {
    if (!id) {
      return {
        success: false,
        message: 'Invalid AdMob ID',
        error: 'ID is undefined or empty'
      };
    }
    
    const response = await api.delete(`/admin/admob/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error deleting AdMob ad ${id}:`, error);
    throw error;
  }
};

export const toggleAdMobStatus = async (id) => {
  try {
    if (!id) {
      return {
        success: false,
        message: 'Invalid AdMob ID',
        error: 'ID is undefined or empty'
      };
    }
    
    const response = await api.patch(`/admin/admob/${id}/toggle-status`);
    return response.data;
  } catch (error) {
    console.error(`Error toggling AdMob ad status ${id}:`, error);
    throw error;
  }
};

// SharedWish management
export const getSharedWishes = async (page = 1, limit = 10, sort = 'createdAt', order = 'desc', filters = {}) => {
  try {
    const response = await api.get('/admin/shared-wishes', {
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
    console.error('Error fetching shared wishes:', error);
    throw error;
  }
};

export const getSharedWishById = async (id) => {
  if (!id) {
    console.error('Invalid shared wish ID provided');
    throw new Error('Invalid shared wish ID');
  }
  
  try {
    const response = await api.get(`/admin/shared-wishes/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching shared wish ${id}:`, error);
    throw error;
  }
};

export const getSharedWishAnalytics = async (filters = {}) => {
  try {
    const response = await api.get('/admin/shared-wishes/analytics', {
      params: filters
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching shared wish analytics:', error);
    return {
      success: false,
      message: `Error fetching analytics: ${error.message}`
    };
  }
};

export const updateSharedWish = async (id, sharedWishData) => {
  if (!id) {
    console.error('Invalid shared wish ID provided');
    throw new Error('Invalid shared wish ID');
  }
  
  try {
    const response = await api.put(`/admin/shared-wishes/${id}`, sharedWishData);
    return response.data;
  } catch (error) {
    console.error(`Error updating shared wish ${id}:`, error);
    throw error;
  }
};

export const deleteSharedWish = async (id) => {
  if (!id) {
    console.error('Invalid shared wish ID provided');
    throw new Error('Invalid shared wish ID');
  }
  
  try {
    const response = await api.delete(`/admin/shared-wishes/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error deleting shared wish ${id}:`, error);
    throw error;
  }
};

// CategoryIcon management
export const getCategoryIcons = async (page = 1, limit = 10, sort = 'category', order = 'asc', filters = {}) => {
  try {
    const response = await api.get('/admin/category-icons', {
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
    console.error('Error fetching category icons:', error);
    throw error;
  }
};

export const getCategoryIconById = async (id) => {
  try {
    if (!id) {
      console.error('Invalid category icon ID provided');
      return {
        success: false,
        message: 'Invalid category icon ID',
        error: 'ID is undefined or empty'
      };
    }
    
    const response = await api.get(`/admin/category-icons/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching category icon ${id}:`, error);
    throw error;
  }
};

export const createCategoryIcon = async (categoryIconData) => {
  try {
    const response = await api.post('/admin/category-icons', categoryIconData);
    return response.data;
  } catch (error) {
    console.error('Error creating category icon:', error);
    throw error;
  }
};

export const updateCategoryIcon = async (id, categoryIconData) => {
  try {
    if (!id) {
      return {
        success: false,
        message: 'Invalid category icon ID',
        error: 'ID is undefined or empty'
      };
    }
    
    const response = await api.put(`/admin/category-icons/${id}`, categoryIconData);
    return response.data;
  } catch (error) {
    console.error(`Error updating category icon ${id}:`, error);
    throw error;
  }
};

export const deleteCategoryIcon = async (id) => {
  try {
    if (!id) {
      return {
        success: false,
        message: 'Invalid category icon ID',
        error: 'ID is undefined or empty'
      };
    }
    
    const response = await api.delete(`/admin/category-icons/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error deleting category icon ${id}:`, error);
    throw error;
  }
};

export const toggleCategoryIconStatus = async (id) => {
  try {
    console.log("Toggling status for CategoryIcon with ID:", id);
    
    if (!id) {
      console.error("toggleCategoryIconStatus called with undefined or empty ID");
      return {
        success: false,
        message: 'Invalid category icon ID',
        error: 'ID is undefined or empty'
      };
    }
    
    const response = await api.patch(`/admin/category-icons/${id}/toggle-status`);
    return response.data;
  } catch (error) {
    console.error(`Error toggling category icon status ${id}:`, error);
    // Return a structured error object instead of throwing
    return {
      success: false,
      message: 'Error toggling category icon status',
      error: error.message
    };
  }
};

// Festival management
export const getFestivals = async (page = 1, limit = 10, sort = 'createdAt', order = 'desc', filters = {}) => {
  try {
    const response = await api.get('/admin/festivals', {
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
    console.error('Error fetching festivals:', error);
    throw error;
  }
};

export const getFestivalById = async (id) => {
  try {
    if (!id || id === 'undefined' || id === 'null') {
      console.error('Invalid festival ID provided');
      return { success: false, message: 'Invalid festival ID' };
    }
    
    const response = await api.get(`/admin/festivals/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching festival ${id}:`, error);
    return { success: false, message: `Error fetching festival: ${error.message}` };
  }
};

export const createFestival = async (festivalData) => {
  try {
    const response = await api.post('/admin/festivals', festivalData);
    return response.data;
  } catch (error) {
    console.error('Error creating festival:', error);
    throw error;
  }
};

export const updateFestival = async (id, festivalData) => {
  try {
    if (!id || id === 'undefined' || id === 'null') {
      console.error('Invalid festival ID provided');
      return { success: false, message: 'Invalid festival ID' };
    }
    
    const response = await api.put(`/admin/festivals/${id}`, festivalData);
    return response.data;
  } catch (error) {
    console.error(`Error updating festival ${id}:`, error);
    return { success: false, message: `Error updating festival: ${error.message}` };
  }
};

export const deleteFestival = async (id) => {
  try {
    if (!id || id === 'undefined' || id === 'null') {
      console.error('Invalid festival ID provided');
      return { success: false, message: 'Invalid festival ID' };
    }
    
    const response = await api.delete(`/admin/festivals/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error deleting festival ${id}:`, error);
    return { success: false, message: `Error deleting festival: ${error.message}` };
  }
};

export const toggleFestivalStatus = async (id) => {
  try {
    if (!id || id === 'undefined' || id === 'null') {
      console.error('Invalid festival ID provided');
      return { success: false, message: 'Invalid festival ID' };
    }
    
    const response = await api.patch(`/admin/festivals/${id}/toggle-status`);
    return response.data;
  } catch (error) {
    console.error(`Error toggling festival status ${id}:`, error);
    return { success: false, message: `Error toggling festival status: ${error.message}` };
  }
};

// About management
export const getAbouts = async (page = 1, limit = 10, sort = 'createdAt', order = 'desc', filters = {}) => {
  try {
    // Build query parameters
    const params = new URLSearchParams();
    params.append('page', page);
    params.append('limit', limit);
    params.append('sort', sort);
    params.append('order', order);
    
    // Add text search if provided
    if (filters.q) {
      params.append('q', filters.q);
    }

    // Add isActive filter if provided
    if (filters.isActive !== undefined && filters.isActive !== 'all') {
      params.append('isActive', filters.isActive);
    }

    const response = await api.get(`/admin/about?${params.toString()}`);
    return response.data;
  } catch (error) {
    console.error('Error fetching abouts:', error);
    throw error;
  }
};

export const getAboutById = async (id) => {
  try {
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Invalid about ID provided');
    }
    
    const response = await api.get(`/admin/about/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching about ${id}:`, error);
    throw error;
  }
};

export const createAbout = async (aboutData) => {
  try {
    const response = await api.post('/admin/about', aboutData);
    return response.data;
  } catch (error) {
    console.error('Error creating about:', error);
    throw error;
  }
};

export const updateAbout = async (id, aboutData) => {
  try {
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Invalid about ID provided');
    }
    
    const response = await api.put(`/admin/about/${id}`, aboutData);
    return response.data;
  } catch (error) {
    console.error(`Error updating about ${id}:`, error);
    throw error;
  }
};

export const deleteAbout = async (id) => {
  try {
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Invalid about ID provided');
    }
    
    const response = await api.delete(`/admin/about/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error deleting about ${id}:`, error);
    throw error;
  }
};

export const toggleAboutStatus = async (id) => {
  try {
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Invalid about ID provided');
    }
    
    const response = await api.patch(`/admin/about/${id}/toggle-status`);
    return response.data;
  } catch (error) {
    console.error(`Error toggling about status ${id}:`, error);
    throw error;
  }
};

// Contact Management
export const getContacts = async (page = 1, limit = 10, sort = 'createdAt', order = 'desc', filters = {}) => {
  try {
    // Build query parameters
    const params = new URLSearchParams();
    params.append('page', page);
    params.append('limit', limit);
    params.append('sort', sort);
    params.append('order', order);
    
    // Add search query if provided
    if (filters.q) {
      params.append('q', filters.q);
    }
    
    // Add isActive filter if provided
    if (filters.isActive !== undefined) {
      params.append('isActive', filters.isActive);
    }
    
    console.log(`Making contact request to: ${api.defaults.baseURL}/admin/contact`);
    const response = await api.get(`/admin/contact?${params.toString()}`);
    return response.data;
  } catch (error) {
    console.error('Error fetching contacts:', error);
    throw error;
  }
};

export const getContactById = async (id) => {
  try {
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Invalid contact ID');
    }
    
    const response = await api.get(`/admin/contact/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching contact with ID ${id}:`, error);
    throw error;
  }
};

export const createContact = async (contactData) => {
  try {
    const response = await api.post('/admin/contact', contactData);
    return response.data;
  } catch (error) {
    console.error('Error creating contact:', error);
    throw error;
  }
};

export const updateContact = async (id, contactData) => {
  try {
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Invalid contact ID');
    }
    
    const response = await api.put(`/admin/contact/${id}`, contactData);
    return response.data;
  } catch (error) {
    console.error(`Error updating contact with ID ${id}:`, error);
    throw error;
  }
};

export const deleteContact = async (id) => {
  try {
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Invalid contact ID');
    }
    
    const response = await api.delete(`/admin/contact/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error deleting contact with ID ${id}:`, error);
    throw error;
  }
};

export const toggleContactStatus = async (id) => {
  try {
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      throw new Error('Invalid contact ID');
    }
    
    const response = await api.patch(`/admin/contact/${id}/toggle-status`, {});
    return response.data;
  } catch (error) {
    console.error(`Error toggling contact status with ID ${id}:`, error);
    throw error;
  }
};

// SponsoredAd management
export const getSponsoredAds = async (page = 1, limit = 10, sort = 'createdAt', order = 'desc', filters = {}) => {
  try {
    const response = await api.get('/admin/sponsored-ads', {
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
    console.error('Error fetching sponsored ads:', error);
    throw error;
  }
};

export const getSponsoredAdById = async (id) => {
  try {
    if (!id) {
      console.error('Invalid sponsored ad ID provided');
      return { success: false, message: 'Invalid sponsored ad ID' };
    }
    
    console.log(`Fetching sponsored ad with ID: ${id}`);
    
    const response = await api.get(`/admin/sponsored-ads/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching sponsored ad ${id}:`, error);
    throw error;
  }
};

export const createSponsoredAd = async (sponsoredAdData) => {
  try {
    const response = await api.post('/admin/sponsored-ads', sponsoredAdData);
    return response.data;
  } catch (error) {
    console.error('Error creating sponsored ad:', error);
    throw error;
  }
};

export const updateSponsoredAd = async (id, sponsoredAdData) => {
  try {
    if (!id) {
      return {
        success: false,
        message: 'Invalid sponsored ad ID',
        error: 'ID is undefined or empty'
      };
    }
    
    console.log(`Updating sponsored ad with ID: ${id}`);
    console.log('Data being sent:', JSON.stringify(sponsoredAdData, null, 2));
    
    try {
      // First try the normal update route
      const response = await api.put(`/admin/sponsored-ads/${id}`, sponsoredAdData);
      return response.data;
    } catch (updateError) {
      // If there's a validation error about dates, use the force-update route
      if (updateError.response && 
          updateError.response.data && 
          updateError.response.data.error && 
          updateError.response.data.error.includes('End date must be after start date')) {
        
        console.log('Date validation error detected, using force-update route');
        const forceUpdateResponse = await api.put(`/admin/sponsored-ads/${id}/force-update`, sponsoredAdData);
        return forceUpdateResponse.data;
      }
      
      // Re-throw other errors
      throw updateError;
    }
  } catch (error) {
    console.error(`Error updating sponsored ad ${id}:`, error);
    
    // Extract validation error message if available
    if (error.response && error.response.data) {
      console.log('Server error response:', error.response.data);
      
      // Return the server's error response for the component to display
      return {
        success: false,
        message: error.response.data.message || 'Error updating sponsored ad',
        error: error.response.data.error || error.message
      };
    }
    
    throw error;
  }
};

export const deleteSponsoredAd = async (id) => {
  try {
    if (!id) {
      return {
        success: false,
        message: 'Invalid sponsored ad ID',
        error: 'ID is undefined or empty'
      };
    }
    
    console.log(`Deleting sponsored ad with ID: ${id}`);
    
    const response = await api.delete(`/admin/sponsored-ads/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error deleting sponsored ad ${id}:`, error);
    throw error;
  }
};

export const toggleSponsoredAdStatus = async (id) => {
  try {
    if (!id) {
      return {
        success: false,
        message: 'Invalid sponsored ad ID',
        error: 'ID is undefined or empty'
      };
    }
    
    console.log(`Toggling sponsored ad status with ID: ${id}`);
    
    const response = await api.patch(`/admin/sponsored-ads/${id}/toggle-status`);
    return response.data;
  } catch (error) {
    console.error(`Error toggling sponsored ad status ${id}:`, error);
    throw error;
  }
};

// Push Notification management
export const getPushNotifications = async (page = 1, limit = 10, sort = 'createdAt', order = 'desc', filters = {}) => {
  try {
    if (!page || page < 1) page = 1;
    if (!limit || limit < 1) limit = 10;
    
    console.log('Fetching push notifications with params:', { page, limit, sort, order, ...filters });
    
    const response = await api.get('/admin/push-notifications', {
      params: {
        page,
        limit,
        sort,
        order,
        ...filters
      },
      timeout: 5000 // 5 second timeout for push notifications
    });
    
    console.log('Push notifications API response status:', response.status);
    console.log('Push notifications count:', response.data?.notifications?.length || 0);
    
    return response.data;
  } catch (error) {
    console.error('Error fetching push notifications:', error);
    
    // Check if this is a timeout error
    if (error.code === 'ECONNABORTED') {
      console.warn('MongoDB connection timeout. Using mock data for push notifications.');
      return mockData.pushNotifications;
    }
    
    // Let the component handle other errors
    throw error;
  }
};

export const getPushNotificationById = async (id) => {
  try {
    if (!id) {
      console.error('Invalid push notification ID');
      throw new Error('Invalid push notification ID');
    }
    
    const response = await api.get(`/admin/push-notifications/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching push notification ${id}:`, error);
    throw error;
  }
};

export const createPushNotification = async (notificationData) => {
  try {
    const response = await api.post('/admin/push-notifications', notificationData);
    return response.data;
  } catch (error) {
    console.error('Error creating push notification:', error);
    throw error;
  }
};

export const updatePushNotification = async (id, notificationData) => {
  try {
    if (!id) {
      console.error('Invalid push notification ID');
      throw new Error('Invalid push notification ID');
    }
    
    const response = await api.put(`/admin/push-notifications/${id}`, notificationData);
    return response.data;
  } catch (error) {
    console.error(`Error updating push notification ${id}:`, error);
    throw error;
  }
};

export const deletePushNotification = async (id) => {
  try {
    if (!id) {
      console.error('Invalid push notification ID');
      throw new Error('Invalid push notification ID');
    }
    
    const response = await api.delete(`/admin/push-notifications/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Error deleting push notification ${id}:`, error);
    throw error;
  }
};

export const sendPushNotification = async (id) => {
  try {
    if (!id) {
      console.error('Invalid push notification ID');
      throw new Error('Invalid push notification ID');
    }
    
    const response = await api.post(`/admin/push-notifications/${id}/send`);
    return response.data;
  } catch (error) {
    console.error(`Error sending push notification ${id}:`, error);
    throw error;
  }
};

export const getUsersByTopic = async (topic) => {
  try {
    if (!topic) {
      console.error('Invalid topic');
      throw new Error('Invalid topic');
    }
    
    const response = await api.get(`/users/by-topic/${topic}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching users by topic ${topic}:`, error);
    throw error;
  }
};

export const getTopicsWithCounts = async () => {
  try {
    console.log('Fetching topics with counts...');
    
    const response = await api.get('/admin/push-notifications/topics', {
      timeout: 5000 // 5 second timeout for topics
    });
    
    console.log('Topics API response status:', response.status);
    console.log('Topics count:', response.data?.topics?.length || 0);
    
    return response.data;
  } catch (error) {
    console.error('Error fetching topics with counts:', error);
    
    // Check if this is a timeout error
    if (error.code === 'ECONNABORTED') {
      console.warn('MongoDB connection timeout. Using mock data for topics with counts.');
      return mockData.topicsWithCounts;
    }
    
    // Let the component handle other errors
    throw error;
  }
};

export const getPushNotificationStats = async () => {
  try {
    console.log('Fetching push notification stats...');
    
    const response = await api.get('/admin/push-notifications/stats', {
      timeout: 5000 // 5 second timeout for stats
    });
    
    console.log('Stats API response status:', response.status);
    console.log('Stats data:', response.data?.stats);
    
    return response.data;
  } catch (error) {
    console.error('Error fetching push notification stats:', error);
    
    // Check if this is a timeout error
    if (error.code === 'ECONNABORTED') {
      console.warn('MongoDB connection timeout. Using mock data for push notification stats.');
      return mockData.pushNotificationStats;
    }
    
    // Let the component handle other errors
    throw error;
  }
};

export const createPersonalizedNotification = async (notificationData) => {
  try {
    const response = await api.post('/admin/push-notifications/personalized', notificationData);
    return response.data;
  } catch (error) {
    console.error('Error creating personalized notification:', error);
    throw error;
  }
};

export const previewPersonalizedNotification = async (notificationData) => {
  try {
    const response = await api.post('/admin/push-notifications/preview-personalized', notificationData);
    return response.data;
  } catch (error) {
    console.error('Error previewing personalized notification:', error);
    throw error;
  }
};

export const triggerInactivityNotifications = async (inactiveDays = 3) => {
  try {
    const response = await api.post('/admin/push-notifications/inactivity', { inactiveDays });
    return response.data;
  } catch (error) {
    console.error('Error triggering inactivity notifications:', error);
    throw error;
  }
};

export const getUserSegments = async () => {
  try {
    console.log('Fetching user segments...');
    
    const response = await api.get('/admin/push-notifications/user-segments', {
      timeout: 5000 // 5 second timeout for user segments
    });
    
    console.log('User segments API response status:', response.status);
    console.log('User segments count:', response.data?.segments?.length || 0);
    
    return response.data;
  } catch (error) {
    console.error('Error fetching user segments:', error);
    
    // Check if this is a timeout error
    if (error.code === 'ECONNABORTED') {
      console.warn('MongoDB connection timeout. Using mock data for user segments.');
      return mockData.userSegments;
    }
    
    // Let the component handle other errors
    throw error;
  }
};

/**
 * Get inactivity notification configuration
 * @returns {Promise<Object>} Configuration object
 */
export const getInactivityNotificationConfig = async () => {
  try {
    const response = await api.get('/admin/push-notifications/inactivity-config');
    return response.data;
  } catch (error) {
    console.error('Error getting inactivity notification config:', error);
    throw error;
  }
};

/**
 * Update inactivity notification configuration
 * @param {Object} configData - Configuration data to update
 * @returns {Promise<Object>} Updated configuration
 */
export const updateInactivityNotificationConfig = async (configData) => {
  try {
    const response = await api.put('/admin/push-notifications/inactivity-config', configData);
    return response.data;
  } catch (error) {
    console.error('Error updating inactivity notification config:', error);
    throw error;
  }
};

/**
 * Trigger inactivity notifications manually
 * @param {number} inactivityThresholdDays - Days of inactivity to trigger notifications for
 * @returns {Promise<Object>} Result object
 */
export const triggerInactivityNotificationsManual = async (inactivityThresholdDays) => {
  try {
    const response = await api.post('/admin/push-notifications/inactivity', {
      inactivityThresholdDays
    });
    
    return response.data;
  } catch (error) {
    console.error('Error triggering inactivity notifications:', error);
    throw error;
  }
};

/**
 * Check MongoDB connection status and collections
 * @returns {Promise<Object>} MongoDB diagnostics information
 */
export const checkMongoDBStatus = async () => {
  try {
    console.log('Checking MongoDB connection status...');
    
    // Use a shorter timeout for diagnostics to avoid long waits
    const response = await api.get('/admin/diagnostics/mongodb', {
      timeout: 3000 // 3 second timeout
    });
    
    console.log('MongoDB connection state:', response.data.mongodb?.connectionStateText);
    console.log('Available collections:', response.data.mongodb?.collections?.length || 0);
    
    return response.data;
  } catch (error) {
    console.error('Error checking MongoDB status:', error);
    
    // Provide more detailed error information
    let errorMessage = error.message || 'Unknown error';
    let errorType = 'unknown';
    let errorDetails = {};
    
    if (error.code === 'ECONNABORTED') {
      errorMessage = 'Connection timed out. The MongoDB server might be down or unreachable.';
      errorType = 'timeout';
    } else if (error.message?.includes('Network Error')) {
      errorMessage = 'Network error. Unable to connect to the server.';
      errorType = 'network';
    } else if (error.response) {
      // The server responded with a status code outside the 2xx range
      errorMessage = `Server responded with error: ${error.response.status} ${error.response.statusText}`;
      errorType = 'server';
      errorDetails = {
        status: error.response.status,
        statusText: error.response.statusText,
        data: error.response.data
      };
    } else if (error.request) {
      // The request was made but no response was received
      errorMessage = 'No response received from server. The server might be down or unreachable.';
      errorType = 'no_response';
    }
    
    return {
      success: false,
      message: 'Error checking MongoDB status',
      error: errorMessage,
      errorType: errorType,
      errorDetails: errorDetails,
      usingMockData: true
    };
  }
};

// Language management
export const getLanguages = async (page = 1, limit = 10, sort = 'displayOrder', order = 'asc', filters = {}) => {
  try {
    // Log the request for debugging
    console.log('Fetching languages with params:', { page, limit, sort, order, ...filters });
    
    const response = await api.get('/admin/languages', {
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
    console.error('Error fetching languages:', error);
    throw error;
  }
};

export const getLanguageByCode = async (code) => {
  try {
    const response = await api.get(`/admin/languages/${code}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching language ${code}:`, error);
    throw error;
  }
};

export const createLanguage = async (languageData) => {
  try {
    const response = await api.post('/admin/languages', languageData);
    return response.data;
  } catch (error) {
    console.error('Error creating language:', error);
    throw error;
  }
};

export const updateLanguage = async (code, languageData) => {
  try {
    const response = await api.put(`/admin/languages/${code}`, languageData);
    return response.data;
  } catch (error) {
    console.error(`Error updating language ${code}:`, error);
    throw error;
  }
};

export const deleteLanguage = async (code) => {
  try {
    const response = await api.delete(`/admin/languages/${code}`);
    return response.data;
  } catch (error) {
    console.error(`Error deleting language ${code}:`, error);
    throw error;
  }
};

// Region management
export const getRegions = async (page = 1, limit = 10, sort = 'displayOrder', order = 'asc', filters = {}) => {
  try {
    // Log the request for debugging
    console.log('Fetching regions with params:', { page, limit, sort, order, ...filters });
    
    const response = await api.get('/admin/regions', {
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
    console.error('Error fetching regions:', error);
    throw error;
  }
};

export const getRegionsByContinent = async (continent) => {
  try {
    const response = await api.get(`/admin/regions/continent/${continent}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching regions for continent ${continent}:`, error);
    throw error;
  }
};

export const getRegionByCode = async (code) => {
  try {
    const response = await api.get(`/admin/regions/${code}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching region ${code}:`, error);
    throw error;
  }
};

export const createRegion = async (regionData) => {
  try {
    const response = await api.post('/admin/regions', regionData);
    return response.data;
  } catch (error) {
    console.error('Error creating region:', error);
    throw error;
  }
};

export const updateRegion = async (code, regionData) => {
  try {
    const response = await api.put(`/admin/regions/${code}`, regionData);
    return response.data;
  } catch (error) {
    console.error(`Error updating region ${code}:`, error);
    throw error;
  }
};

export const deleteRegion = async (code) => {
  try {
    const response = await api.delete(`/admin/regions/${code}`);
    return response.data;
  } catch (error) {
    console.error(`Error deleting region ${code}:`, error);
    throw error;
  }
};

// Template duplication
export const duplicateTemplate = async (id) => {
  try {
    const response = await api.post(`/templates/${id}/duplicate`);
    return response.data;
  } catch (error) {
    console.error(`Error duplicating template ${id}:`, error);
    throw error;
  }
};

// Category Icon duplication
export const duplicateCategoryIcon = async (id) => {
  try {
    const response = await api.post(`/admin/category-icons/${id}/duplicate`);
    return response.data;
  } catch (error) {
    console.error(`Error duplicating category icon ${id}:`, error);
    throw error;
  }
};

// Sponsored Ad duplication
export const duplicateSponsoredAd = async (id) => {
  try {
    const response = await api.post(`/admin/sponsored-ads/${id}/duplicate`);
    return response.data;
  } catch (error) {
    console.error(`Error duplicating sponsored ad ${id}:`, error);
    throw error;
  }
};

// Festival duplication
export const duplicateFestival = async (id) => {
  try {
    const response = await api.post(`/festivals/${id}/duplicate`);
    return response.data;
  } catch (error) {
    console.error(`Error duplicating festival ${id}:`, error);
    throw error;
  }
};

// Export all API functions
export default {
  verifyAdmin,
  getUsers,
  getUserById,
  getUserByObjectId,
  updateUser,
  blockUser,
  unblockUser,
  getDashboardStats,
  getTemplates,
  getTemplateById,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  toggleTemplateStatus,
  exportTemplatesCSV,
  importTemplatesCSV,
  getAdMobs,
  getAdMobById,
  createAdMob,
  updateAdMob,
  deleteAdMob,
  toggleAdMobStatus,
  getSharedWishes,
  getSharedWishById,
  getSharedWishAnalytics,
  updateSharedWish,
  deleteSharedWish,
  getCategoryIcons,
  getCategoryIconById,
  createCategoryIcon,
  updateCategoryIcon,
  deleteCategoryIcon,
  toggleCategoryIconStatus,
  getFestivals,
  getFestivalById,
  createFestival,
  updateFestival,
  deleteFestival,
  toggleFestivalStatus,
  getAbouts,
  getAboutById,
  createAbout,
  updateAbout,
  deleteAbout,
  toggleAboutStatus,
  getContacts,
  getContactById,
  createContact,
  updateContact,
  deleteContact,
  toggleContactStatus,
  getSponsoredAds,
  getSponsoredAdById,
  createSponsoredAd,
  updateSponsoredAd,
  deleteSponsoredAd,
  toggleSponsoredAdStatus,
  getPushNotifications,
  getPushNotificationById,
  createPushNotification,
  updatePushNotification,
  deletePushNotification,
  sendPushNotification,
  getUsersByTopic,
  getTopicsWithCounts,
  getPushNotificationStats,
  createPersonalizedNotification,
  previewPersonalizedNotification,
  triggerInactivityNotifications,
  getUserSegments,
  getInactivityNotificationConfig,
  updateInactivityNotificationConfig,
  triggerInactivityNotificationsManual,
  checkMongoDBStatus,
  getLanguages,
  getLanguageByCode,
  createLanguage,
  updateLanguage,
  deleteLanguage,
  getRegions,
  getRegionsByContinent,
  getRegionByCode,
  createRegion,
  updateRegion,
  deleteRegion,
  duplicateTemplate,
  duplicateCategoryIcon,
  duplicateSponsoredAd,
  duplicateFestival
}; 