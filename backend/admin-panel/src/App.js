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
import AdminLayout from './layouts/AdminLayout';

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
                    <Route path="/push-notifications" element={<PushNotifications />} />
                    <Route path="/push-notifications/create" element={<PushNotificationCreate />} />
                    <Route path="/push-notifications/:id" element={<PushNotificationDetail />} />
                    <Route path="/push-notifications/inactivity-settings" element={<InactivityNotificationSettings />} />
                    
                    {/* Language Routes */}
                    <Route path="/languages" element={<Languages />} />
                    <Route path="/languages/create" element={<LanguageCreate />} />
                    <Route path="/languages/:code" element={<LanguageDetail />} />
                    
                    {/* Region Routes */}
                    <Route path="/regions" element={<Regions />} />
                    <Route path="/regions/create" element={<RegionCreate />} />
                    <Route path="/regions/:code" element={<RegionDetail />} />
                  </Route>
                </Route>
                
                {/* Admin routes */}
                <Route path="/" element={<PrivateRoute><AdminLayout /></PrivateRoute>}>
                  <Route index element={<Dashboard />} />
                  <Route path="dashboard" element={<Dashboard />} />
                </Route>
                
                {/* Default route */}
                <Route path="/" element={<Navigate to="/dashboard" replace />} />
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