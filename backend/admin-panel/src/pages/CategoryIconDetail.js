import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Divider,
  FormHelperText,
  Snackbar
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Save as SaveIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  Cancel as CancelIcon
} from '@mui/icons-material';
import { getCategoryIconById, updateCategoryIcon, deleteCategoryIcon } from '../api';

const CategoryIconDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  
  const [categoryIcon, setCategoryIcon] = useState(null);
  const [editedCategoryIcon, setEditedCategoryIcon] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [editMode, setEditMode] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [validationErrors, setValidationErrors] = useState({});
  const [previewError, setPreviewError] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [showSuccess, setShowSuccess] = useState(false);

  // Fetch category icon data
  useEffect(() => {
    const fetchCategoryIcon = async () => {
      try {
        setLoading(true);
        setError(null);
        
        if (!id) {
          throw new Error('Category icon ID is missing');
        }
        
        const response = await getCategoryIconById(id);
        
        if (response.success && response.categoryIcon) {
          setCategoryIcon(response.categoryIcon);
          setEditedCategoryIcon(response.categoryIcon);
        } else {
          throw new Error(response.message || 'Failed to fetch category icon');
        }
      } catch (err) {
        console.error(`Error fetching category icon ${id}:`, err);
        setError('Failed to load category icon: ' + (err.message || 'Unknown error'));
      } finally {
        setLoading(false);
      }
    };
    
    fetchCategoryIcon();
  }, [id]);

  // Handle field change
  const handleFieldChange = (field, value) => {
    setEditedCategoryIcon(prev => ({
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

  // Validate form
  const validateForm = () => {
    const errors = {};
    
    if (!editedCategoryIcon.category) {
      errors.category = 'Category is required';
    }
    
    if (!editedCategoryIcon.categoryIcon) {
      errors.categoryIcon = 'Icon URL is required';
    } else if (!/^https?:\/\/.+/.test(editedCategoryIcon.categoryIcon)) {
      errors.categoryIcon = 'Icon URL must be a valid URL starting with http:// or https://';
    }
    
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Handle edit mode toggle
  const handleEditToggle = () => {
    if (editMode) {
      // Cancel edit - revert changes
      setEditedCategoryIcon(categoryIcon);
      setValidationErrors({});
    }
    setEditMode(!editMode);
  };

  // Handle save
  const handleSave = async () => {
    try {
      if (!validateForm()) {
        return;
      }
      
      setSaveLoading(true);
      setError(null);
      
      // Create a copy of the data without the id field to prevent overwriting it
      const dataToUpdate = { ...editedCategoryIcon };
      delete dataToUpdate.id; // Remove id field from update data
      
      const response = await updateCategoryIcon(id, dataToUpdate);
      
      if (response.success && response.categoryIcon) {
        setCategoryIcon(response.categoryIcon);
        setEditMode(false);
        // Show success message
        setSuccessMessage('Category icon updated successfully!');
        setShowSuccess(true);
      } else {
        throw new Error(response.message || 'Failed to update category icon');
      }
    } catch (err) {
      console.error(`Error updating category icon ${id}:`, err);
      setError('Failed to save changes: ' + (err.message || 'Unknown error'));
    } finally {
      setSaveLoading(false);
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
      setDeleteLoading(true);
      setError(null);
      
      const response = await deleteCategoryIcon(id);
      
      if (response.success) {
        // Show success message and navigate back after a short delay
        setSuccessMessage('Category icon deleted successfully!');
        setShowSuccess(true);
        setDeleteDialogOpen(false);
        
        setTimeout(() => {
          navigate('/category-icons');
        }, 1500);
      } else {
        throw new Error(response.message || 'Failed to delete category icon');
      }
    } catch (err) {
      console.error(`Error deleting category icon ${id}:`, err);
      setError('Failed to delete category icon: ' + (err.message || 'Unknown error'));
      setDeleteDialogOpen(false);
    } finally {
      setDeleteLoading(false);
    }
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

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ width: '100%' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <IconButton onClick={handleBack} sx={{ mr: 1 }}>
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h4" component="h1">
          {editMode ? 'Edit Category Icon' : 'Category Icon Details'}
        </Typography>
        <Box sx={{ flexGrow: 1 }} />
        {!editMode ? (
          <Button
            variant="contained"
            color="primary"
            startIcon={<EditIcon />}
            onClick={handleEditToggle}
            sx={{ mr: 1 }}
          >
            Edit
          </Button>
        ) : (
          <>
            <Button
              variant="outlined"
              color="secondary"
              startIcon={<CancelIcon />}
              onClick={handleEditToggle}
              sx={{ mr: 1 }}
            >
              Cancel
            </Button>
            <Button
              variant="contained"
              color="primary"
              startIcon={saveLoading ? <CircularProgress size={20} /> : <SaveIcon />}
              onClick={handleSave}
              disabled={saveLoading}
            >
              Save
            </Button>
          </>
        )}
        <Button
          variant="outlined"
          color="error"
          startIcon={<DeleteIcon />}
          onClick={handleDeleteClick}
          sx={{ ml: 1 }}
        >
          Delete
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
              value={editedCategoryIcon?.id || ''}
              disabled={true}
              margin="normal"
              helperText="Auto-generated, cannot be modified"
              InputProps={{
                readOnly: true,
              }}
            />
            
            <TextField
              fullWidth
              label="Category"
              value={editedCategoryIcon?.category || ''}
              onChange={(e) => handleFieldChange('category', e.target.value)}
              disabled={!editMode}
              error={!!validationErrors.category}
              helperText={validationErrors.category}
              margin="normal"
            />
            
            <FormControl fullWidth margin="normal">
              <InputLabel>Icon Type</InputLabel>
              <Select
                value={editedCategoryIcon?.iconType || 'URL'}
                onChange={(e) => handleFieldChange('iconType', e.target.value)}
                disabled={!editMode}
                label="Icon Type"
              >
                <MenuItem value="URL">URL</MenuItem>
                <MenuItem value="RESOURCE">RESOURCE</MenuItem>
              </Select>
            </FormControl>
            
            {editedCategoryIcon?.iconType === 'RESOURCE' && (
              <TextField
                fullWidth
                label="Resource Name"
                value={editedCategoryIcon?.resourceName || ''}
                onChange={(e) => handleFieldChange('resourceName', e.target.value)}
                disabled={!editMode}
                margin="normal"
              />
            )}
            
            <TextField
              fullWidth
              label="Icon URL"
              value={editedCategoryIcon?.categoryIcon || ''}
              onChange={(e) => handleFieldChange('categoryIcon', e.target.value)}
              disabled={!editMode}
              error={!!validationErrors.categoryIcon}
              helperText={validationErrors.categoryIcon}
              margin="normal"
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
              {editedCategoryIcon?.categoryIcon && !previewError ? (
                <Box
                  component="img"
                  src={editedCategoryIcon.categoryIcon}
                  alt={editedCategoryIcon.category}
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
              <Typography variant="subtitle2" gutterBottom>
                Created: {categoryIcon?.createdAt ? new Date(categoryIcon.createdAt).toLocaleString() : 'Unknown'}
              </Typography>
              <Typography variant="subtitle2" gutterBottom>
                Updated: {categoryIcon?.updatedAt ? new Date(categoryIcon.updatedAt).toLocaleString() : 'Unknown'}
              </Typography>
              <Typography variant="subtitle2" gutterBottom>
                Status: {categoryIcon?.status !== false ? 'Active' : 'Inactive'}
              </Typography>
            </Box>
          </Grid>
        </Grid>
      </Paper>

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
      >
        <DialogTitle id="alert-dialog-title">
          Confirm Delete
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="alert-dialog-description">
            Are you sure you want to delete this category icon? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel} disabled={deleteLoading}>
            Cancel
          </Button>
          <Button 
            onClick={handleDeleteConfirm} 
            color="error" 
            autoFocus
            disabled={deleteLoading}
            startIcon={deleteLoading ? <CircularProgress size={20} /> : null}
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default CategoryIconDetail; 