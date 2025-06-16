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
import CategoryIcons from './pages/CategoryIcons';
import CategoryIconDetail from './pages/CategoryIconDetail';
import CategoryIconCreate from './pages/CategoryIconCreate';
import UpcomingFestivals from './pages/UpcomingFestivals';
import UpcomingFestivalDetail from './pages/UpcomingFestivalDetail';
import UpcomingFestivalCreate from './pages/UpcomingFestivalCreate';
import Abouts from './pages/Abouts';
import AboutDetail from './pages/AboutDetail';
import AboutCreate from './pages/AboutCreate';
import Contacts from './pages/Contacts';
import ContactDetail from './pages/ContactDetail';
import ContactCreate from './pages/ContactCreate';
import SponsoredAds from './pages/SponsoredAds';
import SponsoredAdDetail from './pages/SponsoredAdDetail';
import SponsoredAdCreate from './pages/SponsoredAdCreate';

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
                <Route path="/category-icons" element={<CategoryIcons />} />
                <Route path="/category-icons/create" element={<CategoryIconCreate />} />
                <Route path="/category-icons/:id" element={<CategoryIconDetail />} />
                <Route path="/upcoming-festivals" element={<UpcomingFestivals />} />
                <Route path="/upcoming-festivals/create" element={<UpcomingFestivalCreate />} />
                <Route path="/upcoming-festivals/:id" element={<UpcomingFestivalDetail />} />
                <Route path="/settings" element={<h1>Settings Page</h1>} />
                <Route path="/about" element={<Abouts />} />
                <Route path="/about/create" element={<AboutCreate />} />
                <Route path="/about/:id" element={<AboutDetail />} />
                <Route path="/contacts" element={<Contacts />} />
                <Route path="/contacts/create" element={<ContactCreate />} />
                <Route path="/contacts/:id" element={<ContactDetail />} />
                <Route path="/sponsored-ads" element={<SponsoredAds />} />
                <Route path="/sponsored-ads/create" element={<SponsoredAdCreate />} />
                <Route path="/sponsored-ads/:id" element={<SponsoredAdDetail />} />
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