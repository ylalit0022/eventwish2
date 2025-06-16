import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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
  Slider,
  IconButton,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Chip,
  Card,
  CardContent,
  CardMedia,
  Link
} from '@mui/material';
import {
  Edit as EditIcon,
  Save as SaveIcon,
  Delete as DeleteIcon,
  Cancel as CancelIcon,
  Link as LinkIcon,
  InsertChart as AnalyticsIcon
} from '@mui/icons-material';
import { getSponsoredAdById, updateSponsoredAd, deleteSponsoredAd, getUsers } from '../api';

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

const SponsoredAdDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [tabValue, setTabValue] = useState(0);
  const [editMode, setEditMode] = useState(false);
  const [sponsoredAd, setSponsoredAd] = useState(null);
  const [editedAd, setEditedAd] = useState(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [users, setUsers] = useState([]);
  const [loadingUsers, setLoadingUsers] = useState(false);
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

  // Fetch sponsored ad and users
  useEffect(() => {
    const fetchSponsoredAd = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // Fetch users for dropdown
        try {
          setLoadingUsers(true);
          const usersResponse = await getUsers(1, 100, 'email', 'asc');
          if (usersResponse.success) {
            setUsers(usersResponse.users);
          }
        } catch (err) {
          console.error('Error fetching users:', err);
        } finally {
          setLoadingUsers(false);
        }
        
        // Fetch sponsored ad details
        const response = await getSponsoredAdById(id);
        
        if (response.success) {
          setSponsoredAd(response.sponsoredAd);
          setEditedAd(response.sponsoredAd);
        } else {
          throw new Error(response.message || 'Failed to fetch sponsored ad');
        }
      } catch (err) {
        console.error(`Error fetching sponsored ad ${id}:`, err);
        setError('Failed to load sponsored ad: ' + (err.message || 'Unknown error'));
      } finally {
        setLoading(false);
      }
    };

    fetchSponsoredAd();
  }, [id]);

  // Handle tab change
  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  // Toggle edit mode
  const handleEditToggle = () => {
    if (editMode) {
      // Discard changes
      setEditedAd(sponsoredAd);
      setErrors({});
    }
    setEditMode(!editMode);
  };

  // Handle form change
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setEditedAd({
      ...editedAd,
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
    setEditedAd({
      ...editedAd,
      priority: newValue
    });
  };

  // Validate form
  const validateForm = () => {
    const newErrors = {};
    
    // Required fields
    if (!editedAd.uid) newErrors.uid = 'User is required';
    if (!editedAd.image_url) newErrors.image_url = 'Image URL is required';
    if (!editedAd.redirect_url) newErrors.redirect_url = 'Redirect URL is required';
    if (!editedAd.start_date) newErrors.start_date = 'Start date is required';
    if (!editedAd.end_date) newErrors.end_date = 'End date is required';
    
    // URL validation
    const urlRegex = /^https?:\/\/.+/;
    if (editedAd.image_url && !urlRegex.test(editedAd.image_url)) {
      newErrors.image_url = 'Please enter a valid URL starting with http:// or https://';
    }
    if (editedAd.redirect_url && !urlRegex.test(editedAd.redirect_url)) {
      newErrors.redirect_url = 'Please enter a valid URL starting with http:// or https://';
    }
    
    // Date validation
    if (editedAd.start_date && editedAd.end_date) {
      const startDate = new Date(editedAd.start_date);
      const endDate = new Date(editedAd.end_date);
      if (endDate <= startDate) {
        newErrors.end_date = 'End date must be after start date';
      }
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle save
  const handleSave = async () => {
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
      setSaving(true);
      setError(null);
      
      // Prepare dates
      const updatedAd = {
        ...editedAd,
        // Ensure dates are properly formatted
        start_date: new Date(editedAd.start_date).toISOString(),
        end_date: new Date(editedAd.end_date).toISOString()
      };
      
      const response = await updateSponsoredAd(id, updatedAd);
      
      if (response.success) {
        setSponsoredAd(response.sponsoredAd);
        setEditedAd(response.sponsoredAd);
        setSuccess('Sponsored ad updated successfully');
        setEditMode(false);
        
        // Clear success message after 3 seconds
        setTimeout(() => {
          setSuccess(null);
        }, 3000);
      } else {
        throw new Error(response.message || 'Failed to update sponsored ad');
      }
    } catch (err) {
      console.error(`Error updating sponsored ad ${id}:`, err);
      setError('Failed to update sponsored ad: ' + (err.message || 'Unknown error'));
      window.scrollTo(0, 0); // Scroll to top to show error
    } finally {
      setSaving(false);
    }
  };

  // Handle delete dialog
  const handleDeleteClick = () => {
    setDeleteDialogOpen(true);
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
  };

  const handleDeleteConfirm = async () => {
    try {
      setSaving(true);
      
      const response = await deleteSponsoredAd(id);
      
      if (response.success) {
        // Navigate back to list view
        navigate('/sponsored-ads');
      } else {
        throw new Error(response.message || 'Failed to delete sponsored ad');
      }
    } catch (err) {
      console.error(`Error deleting sponsored ad ${id}:`, err);
      setError('Failed to delete sponsored ad: ' + (err.message || 'Unknown error'));
      setDeleteDialogOpen(false);
    } finally {
      setSaving(false);
    }
  };

  // Format date for display
  const formatDate = (dateString) => {
    if (!dateString) return '';
    
    const date = new Date(dateString);
    return date.toLocaleDateString(undefined, { 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric'
    });
  };

  // Format date for input
  const formatDateForInput = (dateString) => {
    if (!dateString) return '';
    
    const date = new Date(dateString);
    return date.toISOString().split('T')[0];
  };

  // Render loading state
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  // Render error state
  if (error && !sponsoredAd) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
        <Button variant="contained" onClick={() => navigate('/sponsored-ads')}>
          Back to Sponsored Ads
        </Button>
      </Box>
    );
  }

  return (
    <Box sx={{ flexGrow: 1, p: 3 }}>
      {/* Header */}
      <Grid container spacing={2} alignItems="center" sx={{ mb: 3 }}>
        <Grid item xs>
          <Typography variant="h4" component="h1">
            {editMode ? 'Edit Sponsored Ad' : 'Sponsored Ad Details'}
          </Typography>
        </Grid>
        <Grid item>
          <Button
            variant={editMode ? 'outlined' : 'contained'}
            color={editMode ? 'secondary' : 'primary'}
            startIcon={editMode ? <CancelIcon /> : <EditIcon />}
            onClick={handleEditToggle}
            disabled={saving}
            sx={{ mr: 1 }}
          >
            {editMode ? 'Cancel' : 'Edit'}
          </Button>
          
          {editMode ? (
            <Button
              variant="contained"
              color="primary"
              startIcon={saving ? <CircularProgress size={20} /> : <SaveIcon />}
              onClick={handleSave}
              disabled={saving}
            >
              Save
            </Button>
          ) : (
            <Button
              variant="outlined"
              color="error"
              startIcon={<DeleteIcon />}
              onClick={handleDeleteClick}
              disabled={saving}
            >
              Delete
            </Button>
          )}
        </Grid>
      </Grid>

      {/* Messages */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}
      
      {success && (
        <Alert severity="success" sx={{ mb: 3 }}>
          {success}
        </Alert>
      )}
      
      {/* Preview Card */}
      {!editMode && (
        <Card sx={{ maxWidth: 400, mb: 3, boxShadow: 3 }}>
          {sponsoredAd.image_url && (
            <CardMedia
              component="img"
              height="200"
              image={sponsoredAd.image_url}
              alt={sponsoredAd.title}
              onError={(e) => {
                e.target.onerror = null;
                e.target.src = 'https://via.placeholder.com/400x200?text=Invalid+Image+URL';
              }}
            />
          )}
          <CardContent>
            <Typography gutterBottom variant="h5" component="div">
              {sponsoredAd.title}
            </Typography>
            {sponsoredAd.description && (
              <Typography variant="body2" color="text.secondary" gutterBottom>
                {sponsoredAd.description}
              </Typography>
            )}
            <Grid container spacing={1} sx={{ mt: 1 }}>
              <Grid item>
                <Chip 
                  label={sponsoredAd.status ? 'Active' : 'Inactive'} 
                  color={sponsoredAd.status ? 'success' : 'default'} 
                  size="small"
                />
              </Grid>
              <Grid item>
                <Chip 
                  label={formatLocation(sponsoredAd.location)} 
                  color="primary" 
                  variant="outlined" 
                  size="small"
                />
              </Grid>
              <Grid item>
                <Chip 
                  label={`Priority: ${sponsoredAd.priority}`} 
                  variant="outlined" 
                  size="small"
                />
              </Grid>
            </Grid>
            <Box sx={{ mt: 2 }}>
              <Link href={sponsoredAd.redirect_url} target="_blank" rel="noopener noreferrer">
                <Button 
                  variant="text" 
                  startIcon={<LinkIcon />}
                  size="small"
                >
                  Visit Redirect URL
                </Button>
              </Link>
            </Box>
          </CardContent>
        </Card>
      )}
      
      {/* Tabs */}
      <Paper sx={{ mb: 3 }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={handleTabChange} aria-label="ad tabs">
            <Tab label="Basic Info" />
            <Tab label="Content" />
            <Tab label="Settings" />
            <Tab label="Analytics" />
          </Tabs>
        </Box>
        
        {/* Basic Info Tab */}
        <TabPanel value={tabValue} index={0}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth error={!!errors.uid} disabled={!editMode}>
                <InputLabel>User</InputLabel>
                <Select
                  name="uid"
                  value={editedAd?.uid || ''}
                  label="User"
                  onChange={handleChange}
                  disabled={!editMode || loadingUsers}
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
                    checked={editedAd?.status || false}
                    onChange={handleChange}
                    color="primary"
                    disabled={!editMode}
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
                value={editedAd?.title || ''}
                onChange={handleChange}
                error={!!errors.title}
                helperText={errors.title}
                disabled={!editMode}
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Description"
                name="description"
                value={editedAd?.description || ''}
                onChange={handleChange}
                multiline
                rows={4}
                error={!!errors.description}
                helperText={errors.description}
                disabled={!editMode}
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
                value={editedAd?.image_url || ''}
                onChange={handleChange}
                placeholder="https://example.com/image.jpg"
                error={!!errors.image_url}
                helperText={errors.image_url || 'URL to the advertisement image'}
                required
                disabled={!editMode}
              />
            </Grid>
            
            {editedAd?.image_url && (
              <Grid item xs={12}>
                <Typography variant="subtitle1" gutterBottom>
                  Image Preview
                </Typography>
                <Box sx={{ maxWidth: 400, maxHeight: 300, overflow: 'hidden' }}>
                  <img 
                    src={editedAd.image_url} 
                    alt="Ad Preview" 
                    style={{ maxWidth: '100%', maxHeight: '100%' }} 
                    onError={(e) => {
                      e.target.onerror = null;
                      e.target.src = 'https://via.placeholder.com/400x200?text=Invalid+Image+URL';
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
                value={editedAd?.redirect_url || ''}
                onChange={handleChange}
                placeholder="https://example.com/landing-page"
                error={!!errors.redirect_url}
                helperText={errors.redirect_url || 'URL where users will be redirected when clicking the ad'}
                required
                disabled={!editMode}
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
                value={editMode ? editedAd?.start_date ? formatDateForInput(editedAd.start_date) : '' : ''}
                onChange={handleChange}
                error={!!errors.start_date}
                helperText={errors.start_date}
                InputLabelProps={{ shrink: true }}
                required
                disabled={!editMode}
              />
              {!editMode && editedAd?.start_date && (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                  Start Date: {formatDate(editedAd.start_date)}
                </Typography>
              )}
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="End Date"
                name="end_date"
                type="date"
                value={editMode ? editedAd?.end_date ? formatDateForInput(editedAd.end_date) : '' : ''}
                onChange={handleChange}
                error={!!errors.end_date}
                helperText={errors.end_date}
                InputLabelProps={{ shrink: true }}
                required
                disabled={!editMode}
              />
              {!editMode && editedAd?.end_date && (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                  End Date: {formatDate(editedAd.end_date)}
                </Typography>
              )}
            </Grid>
            
            <Grid item xs={12} md={6}>
              <FormControl fullWidth error={!!errors.location} disabled={!editMode}>
                <InputLabel>Location</InputLabel>
                <Select
                  name="location"
                  value={editedAd?.location || ''}
                  label="Location"
                  onChange={handleChange}
                  disabled={!editMode}
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
                Priority: {editedAd?.priority || 1}
              </Typography>
              <Slider
                aria-labelledby="priority-slider"
                value={editedAd?.priority || 1}
                onChange={handlePriorityChange}
                step={1}
                marks
                min={1}
                max={10}
                valueLabelDisplay="auto"
                disabled={!editMode}
              />
              <Typography variant="caption" color="text.secondary">
                Higher priority ads are shown more frequently (1-10)
              </Typography>
            </Grid>
          </Grid>
        </TabPanel>
        
        {/* Analytics Tab */}
        <TabPanel value={tabValue} index={3}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="h6" gutterBottom>
                  Performance
                </Typography>
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <Typography variant="subtitle2">Impressions</Typography>
                    <Typography variant="h4">{sponsoredAd?.impression_count || 0}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="subtitle2">Clicks</Typography>
                    <Typography variant="h4">{sponsoredAd?.click_count || 0}</Typography>
                  </Grid>
                  <Grid item xs={12}>
                    <Typography variant="subtitle2">Click Through Rate (CTR)</Typography>
                    <Typography variant="h5">
                      {sponsoredAd?.impression_count ? 
                        ((sponsoredAd.click_count / sponsoredAd.impression_count) * 100).toFixed(2) : 
                        0}%
                    </Typography>
                  </Grid>
                </Grid>
              </Paper>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="h6" gutterBottom>
                  Timeline
                </Typography>
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <Typography variant="subtitle2">Start Date</Typography>
                    <Typography variant="body1">{formatDate(sponsoredAd?.start_date)}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="subtitle2">End Date</Typography>
                    <Typography variant="body1">{formatDate(sponsoredAd?.end_date)}</Typography>
                  </Grid>
                  <Grid item xs={12}>
                    <Typography variant="subtitle2">Status</Typography>
                    <Chip 
                      label={sponsoredAd?.status ? 'Active' : 'Inactive'} 
                      color={sponsoredAd?.status ? 'success' : 'default'} 
                      size="small"
                    />
                  </Grid>
                </Grid>
              </Paper>
            </Grid>
            
            {sponsoredAd?.device_impressions && Object.keys(sponsoredAd.device_impressions).length > 0 && (
              <Grid item xs={12}>
                <Paper sx={{ p: 2 }}>
                  <Typography variant="h6" gutterBottom>
                    Device Impressions
                  </Typography>
                  <Grid container spacing={2}>
                    {Object.entries(sponsoredAd.device_impressions).map(([device, count]) => (
                      <Grid item xs={4} key={device}>
                        <Typography variant="subtitle2">{device}</Typography>
                        <Typography variant="body1">{count}</Typography>
                      </Grid>
                    ))}
                  </Grid>
                </Paper>
              </Grid>
            )}
            
            {sponsoredAd?.device_clicks && Object.keys(sponsoredAd.device_clicks).length > 0 && (
              <Grid item xs={12}>
                <Paper sx={{ p: 2 }}>
                  <Typography variant="h6" gutterBottom>
                    Device Clicks
                  </Typography>
                  <Grid container spacing={2}>
                    {Object.entries(sponsoredAd.device_clicks).map(([device, count]) => (
                      <Grid item xs={4} key={device}>
                        <Typography variant="subtitle2">{device}</Typography>
                        <Typography variant="body1">{count}</Typography>
                      </Grid>
                    ))}
                  </Grid>
                </Paper>
              </Grid>
            )}
          </Grid>
        </TabPanel>
      </Paper>
      
      {/* Delete confirmation dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
      >
        <DialogTitle>Delete Sponsored Ad</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete this sponsored ad? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel} disabled={saving}>
            Cancel
          </Button>
          <Button 
            onClick={handleDeleteConfirm} 
            color="error" 
            disabled={saving}
            startIcon={saving ? <CircularProgress size={20} /> : null}
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SponsoredAdDetail; 