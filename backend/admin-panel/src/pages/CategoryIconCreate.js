import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  Paper,
  TextField,
  Grid,
  Alert,
  CircularProgress,
  IconButton,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormHelperText,
  Snackbar,
  InputAdornment
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Save as SaveIcon,
  Cancel as CancelIcon,
  Refresh as RefreshIcon
} from '@mui/icons-material';
import { createCategoryIcon } from '../api';

const CategoryIconCreate = () => {
  const navigate = useNavigate();
  
  const [categoryIcon, setCategoryIcon] = useState({
    id: '',
    category: '',
    categoryIcon: '',
    iconType: 'URL',
    resourceName: '',
    status: true
  });
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [validationErrors, setValidationErrors] = useState({});
  const [previewError, setPreviewError] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [showSuccess, setShowSuccess] = useState(false);

  // Handle field change
  const handleFieldChange = (field, value) => {
    setCategoryIcon(prev => ({
      ...prev,
      [field]: value
    }));
    
    // Clear validation error for this field
    if (validationErrors[field]) {
      setValidationErrors(prev => ({
        ...prev,
        [field]: null
      }));
    }
  };

  // Generate a unique ID with "cat_" prefix and random MongoDB-like ObjectId
  const generateUniqueId = () => {
    // Generate a timestamp part (first 8 chars)
    const timestamp = Math.floor(Date.now() / 1000).toString(16);
    
    // Generate remaining 16 chars (random)
    let randomPart = '';
    const chars = '0123456789abcdef';
    for (let i = 0; i < 16; i++) {
      randomPart += chars[Math.floor(Math.random() * chars.length)];
    }
    
    return `cat_${timestamp}${randomPart}`;
  };

  // Handle ID generation
  const handleGenerateId = () => {
    const newId = generateUniqueId();
    handleFieldChange('id', newId);
  };

  // Validate form
  const validateForm = () => {
    const errors = {};
    
    if (!categoryIcon.id) {
      errors.id = 'ID is required';
    } else if (!categoryIcon.id.startsWith('cat_')) {
      errors.id = 'ID must start with "cat_" prefix';
    }
    
    if (!categoryIcon.category) {
      errors.category = 'Category is required';
    }
    
    if (!categoryIcon.categoryIcon) {
      errors.categoryIcon = 'Icon URL is required';
    } else if (!/^https?:\/\/.+/.test(categoryIcon.categoryIcon)) {
      errors.categoryIcon = 'Icon URL must be a valid URL starting with http:// or https://';
    }
    
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Handle save
  const handleSave = async () => {
    try {
      if (!validateForm()) {
        return;
      }
      
      setLoading(true);
      setError(null);
      
      const response = await createCategoryIcon(categoryIcon);
      
      if (response.success && response.categoryIcon) {
        // Show success message
        setSuccessMessage('Category icon created successfully!');
        setShowSuccess(true);
        
        // Navigate to the detail page of the newly created category icon after a short delay
        setTimeout(() => {
          navigate(`/category-icons/${response.categoryIcon._id || response.categoryIcon.id}`);
        }, 1500);
      } else {
        throw new Error(response.message || 'Failed to create category icon');
      }
    } catch (err) {
      console.error('Error creating category icon:', err);
      setError('Failed to create category icon: ' + (err.message || 'Unknown error'));
    } finally {
      setLoading(false);
    }
  };

  // Handle cancel
  const handleCancel = () => {
    navigate('/category-icons');
  };

  // Handle back button
  const handleBack = () => {
    navigate('/category-icons');
  };

  // Handle image error
  const handleImageError = () => {
    setPreviewError(true);
  };

  // Handle success message close
  const handleSuccessClose = () => {
    setShowSuccess(false);
  };

  return (
    <Box sx={{ width: '100%' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <IconButton onClick={handleBack} sx={{ mr: 1 }}>
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h4" component="h1">
          Create New Category Icon
        </Typography>
        <Box sx={{ flexGrow: 1 }} />
        <Button
          variant="outlined"
          color="secondary"
          startIcon={<CancelIcon />}
          onClick={handleCancel}
          sx={{ mr: 1 }}
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
          Create
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      <Snackbar
        open={showSuccess}
        autoHideDuration={3000}
        onClose={handleSuccessClose}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert onClose={handleSuccessClose} severity="success" sx={{ width: '100%' }}>
          {successMessage}
        </Alert>
      </Snackbar>

      <Paper sx={{ p: 3, mb: 3 }}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="ID"
              value={categoryIcon.id}
              onChange={(e) => handleFieldChange('id', e.target.value)}
              error={!!validationErrors.id}
              helperText={validationErrors.id || 'Must start with "cat_" prefix'}
              margin="normal"
              required
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={handleGenerateId}
                      edge="end"
                      title="Generate unique ID"
                    >
                      <RefreshIcon />
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
            
            <TextField
              fullWidth
              label="Category"
              value={categoryIcon.category}
              onChange={(e) => handleFieldChange('category', e.target.value)}
              error={!!validationErrors.category}
              helperText={validationErrors.category}
              margin="normal"
              required
            />
            
            <FormControl fullWidth margin="normal">
              <InputLabel>Icon Type</InputLabel>
              <Select
                value={categoryIcon.iconType}
                onChange={(e) => handleFieldChange('iconType', e.target.value)}
                label="Icon Type"
              >
                <MenuItem value="URL">URL</MenuItem>
                <MenuItem value="RESOURCE">RESOURCE</MenuItem>
              </Select>
            </FormControl>
            
            {categoryIcon.iconType === 'RESOURCE' && (
              <TextField
                fullWidth
                label="Resource Name"
                value={categoryIcon.resourceName}
                onChange={(e) => handleFieldChange('resourceName', e.target.value)}
                margin="normal"
              />
            )}
            
            <TextField
              fullWidth
              label="Icon URL"
              value={categoryIcon.categoryIcon}
              onChange={(e) => handleFieldChange('categoryIcon', e.target.value)}
              error={!!validationErrors.categoryIcon}
              helperText={validationErrors.categoryIcon || 'Must be a valid URL starting with http:// or https://'}
              margin="normal"
              required
            />
          </Grid>
          
          <Grid item xs={12} md={6}>
            <Typography variant="h6" gutterBottom>
              Icon Preview
            </Typography>
            
            <Box
              sx={{
                width: '100%',
                height: 200,
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                border: '1px solid #ddd',
                borderRadius: 1,
                overflow: 'hidden',
                p: 2
              }}
            >
              {categoryIcon.categoryIcon && !previewError ? (
                <Box
                  component="img"
                  src={categoryIcon.categoryIcon}
                  alt={categoryIcon.category}
                  onError={handleImageError}
                  sx={{
                    maxWidth: '100%',
                    maxHeight: '100%',
                    objectFit: 'contain'
                  }}
                />
              ) : (
                <Typography color="text.secondary">
                  {previewError ? 'Failed to load image' : 'No image URL provided'}
                </Typography>
              )}
            </Box>
            
            <Box sx={{ mt: 2 }}>
              <Alert severity="info">
                Enter the category icon details and click Create to save the new icon. Use the refresh button to generate a unique ID automatically.
              </Alert>
            </Box>
          </Grid>
        </Grid>
      </Paper>
    </Box>
  );
};

export default CategoryIconCreate; 