import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormHelperText,
  Grid,
  Alert,
  CircularProgress,
  Divider,
  Tabs,
  Tab,
  FormControlLabel,
  Switch,
  Slider
} from '@mui/material';
import { createSponsoredAd, getUsers } from '../api';

// Tab panel component
function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`ad-tabpanel-${index}`}
      aria-labelledby={`ad-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const SponsoredAdCreate = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  const [tabValue, setTabValue] = useState(0);
  const [users, setUsers] = useState([]);
  const [loadingUsers, setLoadingUsers] = useState(false);

  // Form state
  const [adData, setAdData] = useState({
    uid: '',
    image_url: '',
    redirect_url: '',
    status: true,
    start_date: new Date().toISOString().split('T')[0],
    end_date: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0], // 30 days from now
    location: 'category_below',
    priority: 5,
    title: 'Sponsored Ad',
    description: ''
  });

  // Form validation
  const [errors, setErrors] = useState({});

  // Location options based on enum in SponsoredAd model
  const locationOptions = [
    'home_top',
    'home_bottom',
    'category_below',
    'details_top',
    'details_bottom'
  ];

  // Format location display
  const formatLocation = (location) => {
    return location
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  };

  // Fetch users for dropdown
  useEffect(() => {
    const fetchUsers = async () => {
      try {
        setLoadingUsers(true);
        const response = await getUsers(1, 100, 'email', 'asc');
        if (response.success) {
          setUsers(response.users);
        }
      } catch (err) {
        console.error('Error fetching users:', err);
      } finally {
        setLoadingUsers(false);
      }
    };

    fetchUsers();
  }, []);

  // Handle tab change
  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  // Handle form change
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setAdData({
      ...adData,
      [name]: type === 'checkbox' ? checked : value
    });
    
    // Clear error when field is updated
    if (errors[name]) {
      setErrors({
        ...errors,
        [name]: null
      });
    }
  };

  // Handle priority slider change
  const handlePriorityChange = (event, newValue) => {
    setAdData({
      ...adData,
      priority: newValue
    });
  };

  // Validate form
  const validateForm = () => {
    const newErrors = {};
    
    // Required fields
    if (!adData.uid) newErrors.uid = 'User is required';
    if (!adData.image_url) newErrors.image_url = 'Image URL is required';
    if (!adData.redirect_url) newErrors.redirect_url = 'Redirect URL is required';
    if (!adData.start_date) newErrors.start_date = 'Start date is required';
    if (!adData.end_date) newErrors.end_date = 'End date is required';
    
    // URL validation
    const urlRegex = /^https?:\/\/.+/;
    if (adData.image_url && !urlRegex.test(adData.image_url)) {
      newErrors.image_url = 'Please enter a valid URL starting with http:// or https://';
    }
    if (adData.redirect_url && !urlRegex.test(adData.redirect_url)) {
      newErrors.redirect_url = 'Please enter a valid URL starting with http:// or https://';
    }
    
    // Date validation
    if (adData.start_date && adData.end_date) {
      const startDate = new Date(adData.start_date);
      const endDate = new Date(adData.end_date);
      if (endDate <= startDate) {
        newErrors.end_date = 'End date must be after start date';
      }
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      // Switch to the tab with errors
      if (errors.uid || errors.title || errors.description) {
        setTabValue(0); // Basic Info tab
      } else if (errors.image_url || errors.redirect_url) {
        setTabValue(1); // Content tab
      } else if (errors.start_date || errors.end_date || errors.location || errors.priority) {
        setTabValue(2); // Settings tab
      }
      return;
    }
    
    try {
      setLoading(true);
      setError(null);
      
      const response = await createSponsoredAd(adData);
      
      if (response.success) {
        setSuccess(true);
        
        // Redirect to the ad detail page after a brief delay
        setTimeout(() => {
          navigate(`/sponsored-ads/${response.sponsoredAd.id}`);
        }, 1500);
      } else {
        throw new Error(response.message || 'Failed to create sponsored ad');
      }
    } catch (err) {
      console.error('Error creating sponsored ad:', err);
      setError('Failed to create sponsored ad: ' + (err.message || 'Unknown error'));
      window.scrollTo(0, 0); // Scroll to top to show error
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ flexGrow: 1, p: 3 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Create Sponsored Ad
      </Typography>
      
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}
      
      {success && (
        <Alert severity="success" sx={{ mb: 3 }}>
          Sponsored ad created successfully! Redirecting...
        </Alert>
      )}
      
      <Paper sx={{ mb: 3 }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={handleTabChange} aria-label="ad tabs">
            <Tab label="Basic Info" />
            <Tab label="Content" />
            <Tab label="Settings" />
          </Tabs>
        </Box>
        
        <form onSubmit={handleSubmit}>
          {/* Basic Info Tab */}
          <TabPanel value={tabValue} index={0}>
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <FormControl fullWidth error={!!errors.uid}>
                  <InputLabel>User</InputLabel>
                  <Select
                    name="uid"
                    value={adData.uid}
                    label="User"
                    onChange={handleChange}
                    disabled={loadingUsers}
                  >
                    {loadingUsers ? (
                      <MenuItem value="">Loading users...</MenuItem>
                    ) : (
                      users.map((user) => (
                        <MenuItem key={user._id} value={user._id}>
                          {user.email || user.displayName || user.uid}
                        </MenuItem>
                      ))
                    )}
                  </Select>
                  {errors.uid && <FormHelperText>{errors.uid}</FormHelperText>}
                </FormControl>
              </Grid>
              
              <Grid item xs={12} md={6}>
                <FormControlLabel
                  control={
                    <Switch
                      name="status"
                      checked={adData.status}
                      onChange={handleChange}
                      color="primary"
                    />
                  }
                  label="Active"
                />
              </Grid>
              
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Title"
                  name="title"
                  value={adData.title}
                  onChange={handleChange}
                  error={!!errors.title}
                  helperText={errors.title}
                />
              </Grid>
              
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Description"
                  name="description"
                  value={adData.description}
                  onChange={handleChange}
                  multiline
                  rows={4}
                  error={!!errors.description}
                  helperText={errors.description}
                />
              </Grid>
            </Grid>
          </TabPanel>
          
          {/* Content Tab */}
          <TabPanel value={tabValue} index={1}>
            <Grid container spacing={3}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Image URL"
                  name="image_url"
                  value={adData.image_url}
                  onChange={handleChange}
                  placeholder="https://example.com/image.jpg"
                  error={!!errors.image_url}
                  helperText={errors.image_url || 'URL to the advertisement image'}
                  required
                />
              </Grid>
              
              {adData.image_url && (
                <Grid item xs={12}>
                  <Typography variant="subtitle1" gutterBottom>
                    Image Preview
                  </Typography>
                  <Box sx={{ maxWidth: 300, maxHeight: 300, overflow: 'hidden' }}>
                    <img 
                      src={adData.image_url} 
                      alt="Ad Preview" 
                      style={{ maxWidth: '100%', maxHeight: '100%' }} 
                      onError={(e) => {
                        e.target.onerror = null;
                        e.target.src = 'https://via.placeholder.com/300x200?text=Invalid+Image+URL';
                      }}
                    />
                  </Box>
                </Grid>
              )}
              
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Redirect URL"
                  name="redirect_url"
                  value={adData.redirect_url}
                  onChange={handleChange}
                  placeholder="https://example.com/landing-page"
                  error={!!errors.redirect_url}
                  helperText={errors.redirect_url || 'URL where users will be redirected when clicking the ad'}
                  required
                />
              </Grid>
            </Grid>
          </TabPanel>
          
          {/* Settings Tab */}
          <TabPanel value={tabValue} index={2}>
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Start Date"
                  name="start_date"
                  type="date"
                  value={adData.start_date}
                  onChange={handleChange}
                  error={!!errors.start_date}
                  helperText={errors.start_date}
                  InputLabelProps={{ shrink: true }}
                  required
                />
              </Grid>
              
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="End Date"
                  name="end_date"
                  type="date"
                  value={adData.end_date}
                  onChange={handleChange}
                  error={!!errors.end_date}
                  helperText={errors.end_date}
                  InputLabelProps={{ shrink: true }}
                  required
                />
              </Grid>
              
              <Grid item xs={12} md={6}>
                <FormControl fullWidth error={!!errors.location}>
                  <InputLabel>Location</InputLabel>
                  <Select
                    name="location"
                    value={adData.location}
                    label="Location"
                    onChange={handleChange}
                  >
                    {locationOptions.map((location) => (
                      <MenuItem key={location} value={location}>
                        {formatLocation(location)}
                      </MenuItem>
                    ))}
                  </Select>
                  {errors.location && <FormHelperText>{errors.location}</FormHelperText>}
                </FormControl>
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Typography id="priority-slider" gutterBottom>
                  Priority: {adData.priority}
                </Typography>
                <Slider
                  aria-labelledby="priority-slider"
                  value={adData.priority}
                  onChange={handlePriorityChange}
                  step={1}
                  marks
                  min={1}
                  max={10}
                  valueLabelDisplay="auto"
                />
                <Typography variant="caption" color="text.secondary">
                  Higher priority ads are shown more frequently (1-10)
                </Typography>
              </Grid>
            </Grid>
          </TabPanel>
          
          <Divider sx={{ my: 3 }} />
          
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', p: 2 }}>
            <Button
              variant="outlined"
              onClick={() => navigate('/sponsored-ads')}
              sx={{ mr: 1 }}
              disabled={loading}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              color="primary"
              disabled={loading || success}
              startIcon={loading ? <CircularProgress size={20} /> : null}
            >
              {loading ? 'Creating...' : 'Create Sponsored Ad'}
            </Button>
          </Box>
        </form>
      </Paper>
    </Box>
  );
};

export default SponsoredAdCreate; 