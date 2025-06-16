import axios from 'axios';
import { getAuthToken } from './firebase';

// Set API base URL
const API_URL = '/api';
console.log('API base URL set to:', API_URL);

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
    const response = await api.post('/admin/templates', templateData);
    return response.data;
  } catch (error) {
    console.error('Error creating template:', error);
    throw error;
  }
};

export const updateTemplate = async (id, templateData) => {
  try {
    const response = await api.put(`/admin/templates/${id}`, templateData);
    return response.data;
  } catch (error) {
    console.error(`Error updating template ${id}:`, error);
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
    
    const response = await api.put(`/admin/sponsored-ads/${id}`, sponsoredAdData);
    return response.data;
  } catch (error) {
    console.error(`Error updating sponsored ad ${id}:`, error);
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

export default api; 