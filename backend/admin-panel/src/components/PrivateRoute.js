import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { CircularProgress, Box } from '@mui/material';
import { useAuth } from '../contexts/AuthContext';

// Check if we're in development mode
const isDevelopment = window.location.hostname === 'localhost' || 
                     window.location.hostname === '127.0.0.1';

/**
 * PrivateRoute component that requires authentication to access
 * Redirects to login page if not authenticated
 */
const PrivateRoute = () => {
  const { currentUser, adminInfo, loading, error } = useAuth();
  
  // Check for development mode
  const isDevMode = isDevelopment && localStorage.getItem('devMode') === 'true';
  
  // Show loading spinner while checking authentication
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }
  
  // If authenticated and is admin, or in dev mode, render the child routes
  if ((currentUser && adminInfo?.isAdmin) || isDevMode) {
    return <Outlet />;
  }
  
  // Otherwise redirect to login page
  return <Navigate to="/login" replace />;
};

export default PrivateRoute; 