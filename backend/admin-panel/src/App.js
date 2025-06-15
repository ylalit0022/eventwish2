import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';

// Contexts
import { AuthProvider } from './contexts/AuthContext';

// Layouts
import MainLayout from './layouts/MainLayout';

// Components
import PrivateRoute from './components/PrivateRoute';

// Pages
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Users from './pages/Users';
import UserDetail from './pages/UserDetail';
import Templates from './pages/Templates';
import TemplateDetail from './pages/TemplateDetail';
import TemplateCreate from './pages/TemplateCreate';
import SharedWishes from './pages/SharedWishes';
import SharedWishDetail from './pages/SharedWishDetail';
import AdMobs from './pages/AdMobs';
import AdMobDetail from './pages/AdMobDetail';
import AdMobCreate from './pages/AdMobCreate';

// Create theme
const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
    background: {
      default: '#f5f5f5',
    },
  },
});

function App() {
  // Use basename for correct routing when served from /admin/
  console.log('App: Router basename set to /admin');
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>
        <Router basename="/admin">
          <Routes>
            {/* Public routes */}
            <Route path="/login" element={<Login />} />
            
            {/* Protected routes */}
            <Route element={<PrivateRoute />}>
              <Route element={<MainLayout />}>
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/users" element={<Users />} />
                <Route path="/users/:uid" element={<UserDetail />} />
                <Route path="/templates" element={<Templates />} />
                <Route path="/templates/create" element={<TemplateCreate />} />
                <Route path="/templates/:id" element={<TemplateDetail />} />
                <Route path="/shared-wishes" element={<SharedWishes />} />
                <Route path="/shared-wishes/:id" element={<SharedWishDetail />} />
                <Route path="/admob" element={<AdMobs />} />
                <Route path="/admob/create" element={<AdMobCreate />} />
                <Route path="/admob/:id" element={<AdMobDetail />} />
                <Route path="/settings" element={<h1>Settings Page</h1>} />
                <Route path="/categories" element={<h1>Categories Page</h1>} />
              </Route>
            </Route>
            
            {/* Default route */}
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </Router>
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App; 