import React, { createContext, useContext, useState, useEffect } from 'react';
import { auth, getCurrentUser, signInWithGoogle, getGoogleRedirectResult, logOut } from '../firebase';
import { verifyAdmin } from '../api';

// Create auth context
const AuthContext = createContext();

// Check if we're in development mode
const isDevelopment = window.location.hostname === 'localhost' || 
                     window.location.hostname === '127.0.0.1';

// Auth provider component
export const AuthProvider = ({ children }) => {
  const [currentUser, setCurrentUser] = useState(null);
  const [adminInfo, setAdminInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Check for development login on component mount
  useEffect(() => {
    const checkDevLogin = () => {
      if (isDevelopment && localStorage.getItem('devMode') === 'true') {
        console.log("Restoring development login session");
        
        // Create a mock user
        const mockUser = {
          uid: 'dev-user-id',
          email: 'ylalit0022@gmail.com', // Use your email
          displayName: 'Development User',
          photoURL: null,
          getIdToken: () => Promise.resolve('dev-token')
        };
        
        // Set the mock user
        setCurrentUser(mockUser);
        
        // Set admin info directly
        setAdminInfo({
          isAdmin: true,
          role: 'superAdmin'
        });
        
        console.log("Development login session restored");
        setLoading(false);
        return true;
      }
      return false;
    };
    
    // Only proceed with Firebase auth if not in dev mode
    if (!checkDevLogin()) {
      checkRedirectResult();
    }
  }, []);

  // Check for redirect result
  const checkRedirectResult = async () => {
    try {
      console.log("Checking for redirect result...");
      const result = await getGoogleRedirectResult();
      if (result) {
        console.log("Redirect sign-in successful", result.user);
        setCurrentUser(result.user);
        
        // Verify admin status immediately after redirect
        try {
          const adminData = await verifyAdmin();
          console.log("Admin verification response after redirect:", adminData);
          
          if (adminData && adminData.isAdmin) {
            console.log("Admin verification successful", adminData);
            setAdminInfo({
              isAdmin: true,
              role: adminData.role
            });
          } else {
            console.log("User is not an admin");
            setAdminInfo(null);
            setError('You do not have admin access');
          }
        } catch (err) {
          console.error('Error verifying admin status after redirect:', err);
          setAdminInfo(null);
          setError('Error verifying admin status: ' + (err.message || err));
        }
        
        setLoading(false);
      } else {
        console.log("No redirect result found");
      }
    } catch (err) {
      console.error('Error handling redirect result:', err);
      setError('Failed to complete Google sign-in: ' + (err.message || err));
      setLoading(false);
    }
  };

  // Listen for auth state changes
  useEffect(() => {
    // Skip Firebase auth listener if in dev mode
    if (isDevelopment && localStorage.getItem('devMode') === 'true') {
      console.log("Skipping Firebase auth listener in dev mode");
      return () => {};
    }
    
    console.log("Setting up auth state listener");
    const unsubscribe = auth.onAuthStateChanged(async (user) => {
      console.log("Auth state changed:", user ? `User logged in: ${user.email}` : "No user");
      
      if (user) {
        setCurrentUser(user);
        
        // Only verify admin status if we don't already have it
        if (!adminInfo) {
          try {
            console.log("Verifying admin status...");
            const adminData = await verifyAdmin();
            console.log("Admin verification response:", adminData);
            
            if (adminData && adminData.isAdmin) {
              console.log("Admin verification successful", adminData);
              setAdminInfo({
                isAdmin: true,
                role: adminData.role
              });
            } else {
              console.log("User is not an admin");
              setAdminInfo(null);
              setError('You do not have admin access');
            }
          } catch (err) {
            console.error('Error verifying admin status:', err);
            setAdminInfo(null);
            setError('Error verifying admin status: ' + (err.message || err));
          }
        }
      } else {
        // Don't clear user in dev mode
        if (!(isDevelopment && localStorage.getItem('devMode') === 'true')) {
          setCurrentUser(null);
          setAdminInfo(null);
        }
      }
      
      setLoading(false);
    });

    return unsubscribe;
  }, [adminInfo]);

  // Sign in with Google
  const login = async () => {
    try {
      setError(null);
      setLoading(true);
      console.log("Initiating Google sign-in redirect...");
      
      // Using redirect method
      await signInWithGoogle();
      // The redirect will happen now, and the result will be handled in the useEffect
      
      // Note: We won't reach this point as the page will redirect
    } catch (err) {
      console.error('Login error:', err);
      setError('Failed to sign in with Google: ' + (err.message || err));
      setLoading(false);
    }
  };

  // Development login (bypasses Firebase)
  const devLogin = async () => {
    try {
      setError(null);
      setLoading(true);
      console.log("Using development login...");
      
      // Set dev mode flag in localStorage
      localStorage.setItem('devMode', 'true');
      
      // Create a mock user
      const mockUser = {
        uid: 'dev-user-id',
        email: 'ylalit0022@gmail.com', // Use your email
        displayName: 'Development User',
        photoURL: null,
        getIdToken: () => Promise.resolve('dev-token')
      };
      
      // Set the mock user
      setCurrentUser(mockUser);
      
      // Set admin info directly
      setAdminInfo({
        isAdmin: true,
        role: 'superAdmin'
      });
      
      console.log("Development login successful");
      setLoading(false);
      return true;
    } catch (err) {
      console.error('Development login error:', err);
      setError('Failed to use development login: ' + (err.message || err));
      setLoading(false);
      return false;
    }
  };

  // Sign out
  const logout = async () => {
    try {
      setError(null);
      console.log("Signing out...");
      
      // Clear dev mode flag
      localStorage.removeItem('devMode');
      
      await logOut();
      setCurrentUser(null);
      setAdminInfo(null);
      console.log("Signed out successfully");
    } catch (err) {
      console.error('Logout error:', err);
      setError('Failed to sign out');
    }
  };

  // Check if user has a specific permission
  const hasPermission = (permission) => {
    if (!adminInfo || !adminInfo.role) return false;
    
    // This would need to be implemented based on your permission structure
    // For now, we'll assume superAdmin has all permissions
    if (adminInfo.role === 'superAdmin') return true;
    
    // Other role-based permissions would be checked here
    return false;
  };

  const value = {
    currentUser,
    adminInfo,
    loading,
    error,
    login,
    devLogin,
    logout,
    hasPermission
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

// Custom hook to use auth context
export const useAuth = () => {
  return useContext(AuthContext);
}; 