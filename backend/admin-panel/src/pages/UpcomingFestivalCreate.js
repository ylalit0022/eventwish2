import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  Paper,
  Grid,
  TextField,
  Switch,
  FormControlLabel,
  CircularProgress,
  Alert,
  IconButton,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Snackbar
} from '@mui/material';
import {
  Save as SaveIcon,
  ArrowBack as ArrowBackIcon,
  Cancel as CancelIcon,
  ContentCopy as ContentCopyIcon
} from '@mui/icons-material';
import { createFestival, getCategoryIcons, getUpcomingFestival } from '../api';
import { useSnackbar } from '../contexts/SnackbarContext';

const UpcomingFestivalCreate = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { showSnackbar } = useSnackbar();
  const [festival, setFestival] = useState({
    name: '',
    slug: '',
    date: new Date().toISOString(),
    category: '',
    description: '',
    isActive: true,
    status: 'UPCOMING',
    priority: 0,
    templates: []
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [categoryIcons, setCategoryIcons] = useState([]);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success'
  });

  // Check for copy parameter in URL
  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    const copyFestivalId = searchParams.get('copy');
    
    if (copyFestivalId) {
      const fetchFestivalForCopy = async () => {
        try {
          setLoading(true);
          setError(null);
          
          const response = await getUpcomingFestival(copyFestivalId);
          
          if (response.success) {
            // Create a copy of the festival with a modified name to indicate it's a copy
            const festivalData = response.festival;
            setFestival({
              ...festivalData,
              name: `${festivalData.name} (Copy)`,
              _id: undefined // Remove ID to ensure a new festival is created
            });
            
            showSnackbar('Festival data loaded for copying. Please update details as needed.', 'info');
          } else {
            throw new Error(response.message || 'Failed to fetch festival for copying');
          }
        } catch (err) {
          console.error('Error fetching festival for copy:', err);
          setError('Failed to load festival for copying: ' + (err.message || 'Unknown error'));
          showSnackbar('Failed to load festival for copying', 'error');
        } finally {
          setLoading(false);
        }
      };
      
      fetchFestivalForCopy();
    }
  }, [location.search, showSnackbar]);

  // Fetch category icons for dropdown
  useEffect(() => {
    const fetchCategoryIcons = async () => {
      try {
        const response = await getCategoryIcons(1, 100);
        if (response.success) {
          setCategoryIcons(response.data);
        }
      } catch (err) {
        console.error('Error fetching category icons:', err);
      }
    };
    
    fetchCategoryIcons();
  }, []);

  // Handle field change
  const handleFieldChange = (field, value) => {
    setFestival({
      ...festival,
      [field]: value
    });
  };

  // Handle date field change
  const handleDateChange = (field, value) => {
    // Convert from HTML datetime-local format to ISO string
    const date = value ? new Date(value).toISOString() : null;
    handleFieldChange(field, date);
  };

  // Format date for input
  const formatDateForInput = (dateString) => {
    if (!dateString) return '';
    try {
      // Format as YYYY-MM-DDThh:mm
      const date = new Date(dateString);
      return date.toISOString().slice(0, 16);
    } catch (error) {
      return '';
    }
  };

  // Generate slug from name
  const generateSlug = (name) => {
    return name
      .toLowerCase()
      .replace(/[^\w\s-]/g, '') // Remove special characters
      .replace(/\s+/g, '-')     // Replace spaces with hyphens
      .replace(/-+/g, '-');     // Replace multiple hyphens with single hyphen
  };

  // Auto-generate slug when name changes
  useEffect(() => {
    if (festival.name && !festival.slug) {
      const slug = generateSlug(festival.name);
      setFestival(prev => ({ ...prev, slug }));
    }
  }, [festival.name]);

  // Handle save
  const handleSave = async () => {
    // Validate required fields
    if (!festival.name || !festival.date || !festival.category) {
      setError('Please fill in all required fields (Name, Date, Category)');
      return;
    }
    
    try {
      setLoading(true);
      setError(null);
      
      const response = await createFestival(festival);
      
      if (response.success) {
        // Show success message
        setSnackbar({
          open: true,
          message: 'Festival created successfully',
          severity: 'success'
        });
        
        // Navigate to the detail page
        setTimeout(() => {
          navigate(`/upcoming-festivals/${response.festival._id}`);
        }, 1000);
      } else {
        throw new Error(response.message || 'Failed to create festival');
      }
    } catch (err) {
      console.error('Error creating festival:', err);
      setError('Failed to create festival: ' + (err.message || 'Unknown error'));
      
      // Show error message
      setSnackbar({
        open: true,
        message: `Failed to create festival: ${err.message || 'Unknown error'}`,
        severity: 'error'
      });
    } finally {
      setLoading(false);
    }
  };

  // Handle snackbar close
  const handleSnackbarClose = (event, reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setSnackbar({ ...snackbar, open: false });
  };

  // Handle cancel
  const handleCancel = () => {
    navigate('/upcoming-festivals');
  };

  return (
    <Box sx={{ flexGrow: 1, p: 3 }}>
      {/* Header */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item>
            <IconButton onClick={handleCancel}>
              <ArrowBackIcon />
            </IconButton>
          </Grid>
          <Grid item xs>
            <Typography variant="h5">
              {location.search.includes('copy=') ? 'Create Festival Copy' : 'Create New Festival'}
            </Typography>
          </Grid>
          <Grid item>
            <Button
              variant="outlined"
              color="secondary"
              startIcon={<CancelIcon />}
              onClick={handleCancel}
              sx={{ mr: 1 }}
              disabled={loading}
            >
              Cancel
            </Button>
            <Button
              variant="contained"
              color="primary"
              startIcon={loading ? <CircularProgress size={20} /> : <SaveIcon />}
              onClick={handleSave}
              disabled={loading}
            >
              Create Festival
            </Button>
          </Grid>
        </Grid>
      </Paper>

      {/* Error alert */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Form */}
      <Paper sx={{ p: 3 }}>
        <Grid container spacing={3}>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Name"
              value={festival.name}
              onChange={(e) => handleFieldChange('name', e.target.value)}
              fullWidth
              required
              error={!festival.name}
              helperText={!festival.name ? 'Name is required' : ''}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Slug"
              value={festival.slug}
              onChange={(e) => handleFieldChange('slug', e.target.value)}
              fullWidth
              helperText="URL-friendly identifier (auto-generated from name)"
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Date"
              type="datetime-local"
              value={formatDateForInput(festival.date)}
              onChange={(e) => handleDateChange('date', e.target.value)}
              fullWidth
              required
              InputLabelProps={{ shrink: true }}
              error={!festival.date}
              helperText={!festival.date ? 'Date is required' : ''}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Category"
              value={festival.category}
              onChange={(e) => handleFieldChange('category', e.target.value)}
              fullWidth
              required
              error={!festival.category}
              helperText={!festival.category ? 'Category is required' : ''}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth>
              <InputLabel id="category-icon-label">Category Icon</InputLabel>
              <Select
                labelId="category-icon-label"
                value={festival.categoryIcon || ''}
                onChange={(e) => handleFieldChange('categoryIcon', e.target.value)}
                label="Category Icon"
              >
                <MenuItem value="">None</MenuItem>
                {categoryIcons.map((icon) => (
                  <MenuItem key={icon._id} value={icon._id}>
                    {icon.category}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Priority"
              type="number"
              value={festival.priority}
              onChange={(e) => handleFieldChange('priority', parseInt(e.target.value) || 0)}
              fullWidth
              InputProps={{ inputProps: { min: 0 } }}
              helperText="Higher priority festivals appear first"
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth>
              <InputLabel id="status-label">Status</InputLabel>
              <Select
                labelId="status-label"
                value={festival.status}
                onChange={(e) => handleFieldChange('status', e.target.value)}
                label="Status"
              >
                <MenuItem value="UPCOMING">Upcoming</MenuItem>
                <MenuItem value="ONGOING">Ongoing</MenuItem>
                <MenuItem value="ENDED">Ended</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={6}>
            <FormControlLabel
              control={
                <Switch
                  checked={festival.isActive}
                  onChange={(e) => handleFieldChange('isActive', e.target.checked)}
                  color="primary"
                />
              }
              label="Active"
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              label="Description"
              value={festival.description}
              onChange={(e) => handleFieldChange('description', e.target.value)}
              fullWidth
              multiline
              rows={4}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Image URL"
              value={festival.imageUrl || ''}
              onChange={(e) => handleFieldChange('imageUrl', e.target.value)}
              fullWidth
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label="Banner URL"
              value={festival.bannerUrl || ''}
              onChange={(e) => handleFieldChange('bannerUrl', e.target.value)}
              fullWidth
            />
          </Grid>
        </Grid>
      </Paper>

      {/* Snackbar for notifications */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleSnackbarClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert 
          onClose={handleSnackbarClose} 
          severity={snackbar.severity} 
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default UpcomingFestivalCreate;
 