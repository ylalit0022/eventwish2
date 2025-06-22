import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { SnackbarProvider as NotiStackProvider } from 'notistack';

// Contexts
import { AuthProvider } from './contexts/AuthContext';
import { SnackbarProvider } from './contexts/SnackbarContext';

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
import PushNotifications from './pages/PushNotifications';
import PushNotificationDetail from './pages/PushNotificationDetail';
import PushNotificationCreate from './pages/PushNotificationCreate';
import InactivityNotificationSettings from './pages/InactivityNotificationSettings';
import Languages from './pages/Languages';
import LanguageCreate from './pages/LanguageCreate';
import LanguageDetail from './pages/LanguageDetail';
import Regions from './pages/Regions';
import RegionCreate from './pages/RegionCreate';
import RegionDetail from './pages/RegionDetail';

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
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <NotiStackProvider maxSnack={3}>
        <SnackbarProvider>
          <AuthProvider>
            <Router basename="/admin">
              <Routes>
                {/* Public routes */}
                <Route path="/login" element={<Login />} />
                
                {/* Protected routes - ALL admin pages use MainLayout with sidebar */}
                <Route element={<PrivateRoute />}>
                  <Route element={<MainLayout />}>
                    {/* Dashboard routes */}
                    <Route path="/" element={<Navigate to="/dashboard" replace />} />
                    <Route path="/dashboard" element={<Dashboard />} />
                    
                    {/* User management */}
                    <Route path="/users" element={<Users />} />
                    <Route path="/users/:uid" element={<UserDetail />} />
                    
                    {/* Template management */}
                    <Route path="/templates" element={<Templates />} />
                    <Route path="/templates/create" element={<TemplateCreate />} />
                    <Route path="/templates/:id" element={<TemplateDetail />} />
                    
                    {/* Shared wishes */}
                    <Route path="/shared-wishes" element={<SharedWishes />} />
                    <Route path="/shared-wishes/:id" element={<SharedWishDetail />} />
                    
                    {/* AdMob management */}
                    <Route path="/admob" element={<AdMobs />} />
                    <Route path="/admob/create" element={<AdMobCreate />} />
                    <Route path="/admob/:id" element={<AdMobDetail />} />
                    
                    {/* Category icons */}
                    <Route path="/category-icons" element={<CategoryIcons />} />
                    <Route path="/category-icons/create" element={<CategoryIconCreate />} />
                    <Route path="/category-icons/:id" element={<CategoryIconDetail />} />
                    
                    {/* Festivals */}
                    <Route path="/upcoming-festivals" element={<UpcomingFestivals />} />
                    <Route path="/upcoming-festivals/create" element={<UpcomingFestivalCreate />} />
                    <Route path="/upcoming-festivals/:id" element={<UpcomingFestivalDetail />} />
                    
                    {/* Sponsored Ads */}
                    <Route path="/sponsored-ads" element={<SponsoredAds />} />
                    <Route path="/sponsored-ads/create" element={<SponsoredAdCreate />} />
                    <Route path="/sponsored-ads/:id" element={<SponsoredAdDetail />} />
                    
                    {/* Push Notifications */}
                    <Route path="/push-notifications" element={<PushNotifications />} />
                    <Route path="/push-notifications/create" element={<PushNotificationCreate />} />
                    <Route path="/push-notifications/:id" element={<PushNotificationDetail />} />
                    <Route path="/push-notifications/inactivity-settings" element={<InactivityNotificationSettings />} />
                    
                    {/* About pages */}
                    <Route path="/about" element={<Abouts />} />
                    <Route path="/about/create" element={<AboutCreate />} />
                    <Route path="/about/:id" element={<AboutDetail />} />
                    
                    {/* Contacts */}
                    <Route path="/contacts" element={<Contacts />} />
                    <Route path="/contacts/create" element={<ContactCreate />} />
                    <Route path="/contacts/:id" element={<ContactDetail />} />
                    
                    {/* Languages */}
                    <Route path="/languages" element={<Languages />} />
                    <Route path="/languages/create" element={<LanguageCreate />} />
                    <Route path="/languages/:code" element={<LanguageDetail />} />
                    
                    {/* Regions */}
                    <Route path="/regions" element={<Regions />} />
                    <Route path="/regions/create" element={<RegionCreate />} />
                    <Route path="/regions/:code" element={<RegionDetail />} />
                    
                    {/* Settings */}
                    <Route path="/settings" element={<h1>Settings Page</h1>} />
                  </Route>
                </Route>
                
                {/* Fallback routes */}
                <Route path="*" element={<Navigate to="/login" />} />
              </Routes>
            </Router>
          </AuthProvider>
        </SnackbarProvider>
      </NotiStackProvider>
    </ThemeProvider>
  );
}

export default App; 
export default App; 
