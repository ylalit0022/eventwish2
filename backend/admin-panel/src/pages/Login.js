import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Container, Typography, Box, Paper, CircularProgress, Divider } from '@mui/material';
import GoogleIcon from '@mui/icons-material/Google';
import DeveloperModeIcon from '@mui/icons-material/DeveloperMode';
import { useAuth } from '../contexts/AuthContext';

const Login = () => {
  const { currentUser, adminInfo, loading, error, login, devLogin } = useAuth();
  const [isDevMode, setIsDevMode] = useState(false);
  const navigate = useNavigate();

  // Check if we're in development mode (localhost)
  useEffect(() => {
    const isLocalhost = window.location.hostname === 'localhost' || 
                        window.location.hostname === '127.0.0.1';
    setIsDevMode(isLocalhost);
  }, []);

  // Redirect if already logged in and is admin
  useEffect(() => {
    console.log("Login page - Current user:", currentUser?.email);
    console.log("Login page - Admin info:", adminInfo);
    
    if (currentUser && adminInfo?.isAdmin) {
      console.log("User is authenticated and admin, redirecting to dashboard");
      navigate('/dashboard');
    }
  }, [currentUser, adminInfo, navigate]);

  const handleGoogleSignIn = async () => {
    try {
      console.log("Login page - Initiating Google sign-in");
      await login();
      // Redirect will happen automatically
    } catch (error) {
      console.error('Login page - Login error:', error);
    }
  };

  const handleDevLogin = async () => {
    try {
      console.log("Login page - Using development login");
      await devLogin();
      navigate('/dashboard');
    } catch (error) {
      console.error('Login page - Dev login error:', error);
    }
  };

  return (
    <Container maxWidth="sm">
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
        }}
      >
        <Paper 
          elevation={3}
          sx={{
            p: 4,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            width: '100%',
          }}
        >
          <Typography variant="h4" component="h1" gutterBottom>
            EventWish Admin
          </Typography>
          
          <Typography variant="body1" color="textSecondary" align="center" sx={{ mb: 3 }}>
            Sign in with your Google account to access the admin panel
          </Typography>

          {loading ? (
            <>
              <CircularProgress />
              <Typography variant="body2" sx={{ mt: 2 }}>
                Authenticating...
              </Typography>
            </>
          ) : (
            <>
              <Button
                variant="contained"
                color="primary"
                startIcon={<GoogleIcon />}
                onClick={handleGoogleSignIn}
                fullWidth
                size="large"
                sx={{ mt: 2 }}
              >
                Sign in with Google
              </Button>

              {isDevMode && (
                <>
                  <Divider sx={{ width: '100%', my: 3 }}>OR</Divider>
                  <Button
                    variant="outlined"
                    color="secondary"
                    startIcon={<DeveloperModeIcon />}
                    onClick={handleDevLogin}
                    fullWidth
                    size="large"
                  >
                    Development Login
                  </Button>
                  <Typography variant="caption" color="textSecondary" sx={{ mt: 1 }}>
                    (Only available on localhost)
                  </Typography>
                </>
              )}
            </>
          )}

          {error && (
            <Typography color="error" sx={{ mt: 2 }}>
              {error}
            </Typography>
          )}
          
          {currentUser && !adminInfo?.isAdmin && (
            <Typography color="error" sx={{ mt: 2 }}>
              Your account does not have admin privileges.
            </Typography>
          )}
        </Paper>
      </Box>
    </Container>
  );
};

export default Login; 