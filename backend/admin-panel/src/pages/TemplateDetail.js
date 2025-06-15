import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Grid,
  TextField,
  Button,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
  FormControlLabel,
  Switch,
  Divider,
  IconButton,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Chip,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Autocomplete
} from '@mui/material';
import {
  Save as SaveIcon,
  Delete as DeleteIcon,
  ArrowBack as ArrowBackIcon,
  Edit as EditIcon,
  Cancel as CancelIcon
} from '@mui/icons-material';
import { getTemplateById, updateTemplate, deleteTemplate } from '../api';

// TabPanel component for tab content
function TabPanel({ children, value, index, ...other }) {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`template-tabpanel-${index}`}
      aria-labelledby={`template-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: 3 }}>
          {children}
        </Box>
      )}
    </div>
  );
}

const TemplateDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [template, setTemplate] = useState(null);
  const [editedTemplate, setEditedTemplate] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saveLoading, setSaveLoading] = useState(false);
  const [error, setError] = useState(null);
  const [tabValue, setTabValue] = useState(0);
  const [editMode, setEditMode] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);

  // Fetch template data
  useEffect(() => {
    const fetchTemplate = async () => {
      try {
        setLoading(true);
        setError(null);
        
        const response = await getTemplateById(id);
        
        if (response.success) {
          setTemplate(response.template);
          setEditedTemplate(response.template);
        } else {
          throw new Error(response.message || 'Failed to load template details');
        }
      } catch (err) {
        console.error(`Error fetching template ${id}:`, err);
        setError('Failed to load template details: ' + (err.message || 'Unknown error'));
      } finally {
        setLoading(false);
      }
    };

    fetchTemplate();
  }, [id]);

  // Handle tab change
  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  // Handle edit mode toggle
  const handleEditToggle = () => {
    if (editMode) {
      setEditedTemplate(template); // Reset changes
    }
    setEditMode(!editMode);
  };

  // Handle field change
  const handleFieldChange = (field, value) => {
    setEditedTemplate(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Handle tags change
  const handleTagsChange = (event, newValue) => {
    setEditedTemplate(prev => ({
      ...prev,
      tags: newValue
    }));
  };

  // Handle save changes
  const handleSaveChanges = async () => {
    try {
      setSaveLoading(true);
      setError(null);
      
      const response = await updateTemplate(id, editedTemplate);
      
      if (response.success) {
        setTemplate(response.template);
        setEditMode(false);
      } else {
        throw new Error(response.message || 'Failed to update template');
      }
    } catch (err) {
      console.error(`Error updating template ${id}:`, err);
      setError('Failed to update template: ' + (err.message || 'Unknown error'));
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
      
      const response = await deleteTemplate(id);
      
      if (response.success) {
        navigate('/templates');
      } else {
        throw new Error(response.message || 'Failed to delete template');
      }
    } catch (err) {
      console.error(`Error deleting template ${id}:`, err);
      setError('Failed to delete template: ' + (err.message || 'Unknown error'));
      setDeleteDialogOpen(false);
    } finally {
      setDeleteLoading(false);
    }
  };

  // Handle back button
  const handleBackClick = () => {
    navigate('/templates');
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error && !template) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error}</Alert>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={handleBackClick}
          sx={{ mt: 2 }}
        >
          Back to Templates
        </Button>
      </Box>
    );
  }

  if (!template) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="warning">Template not found</Alert>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={handleBackClick}
          sx={{ mt: 2 }}
        >
          Back to Templates
        </Button>
      </Box>
    );
  }

  return (
    <Box sx={{ flexGrow: 1, p: 3 }}>
      {/* Header */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item>
            <IconButton onClick={handleBackClick}>
              <ArrowBackIcon />
            </IconButton>
          </Grid>
          <Grid item xs>
            <Typography variant="h5">
              {editMode ? 'Edit Template' : template.title}
            </Typography>
            <Typography variant="body2" color="textSecondary">
              ID: {template._id}
            </Typography>
          </Grid>
          <Grid item>
            {editMode ? (
              <>
                <Button
                  variant="outlined"
                  color="secondary"
                  startIcon={<CancelIcon />}
                  onClick={handleEditToggle}
                  sx={{ mr: 1 }}
                  disabled={saveLoading}
                >
                  Cancel
                </Button>
                <Button
                  variant="contained"
                  color="primary"
                  startIcon={saveLoading ? <CircularProgress size={20} /> : <SaveIcon />}
                  onClick={handleSaveChanges}
                  disabled={saveLoading}
                >
                  Save Changes
                </Button>
              </>
            ) : (
              <>
                <Button
                  variant="outlined"
                  color="error"
                  startIcon={<DeleteIcon />}
                  onClick={handleDeleteClick}
                  sx={{ mr: 1 }}
                >
                  Delete
                </Button>
                <Button
                  variant="contained"
                  color="primary"
                  startIcon={<EditIcon />}
                  onClick={handleEditToggle}
                >
                  Edit
                </Button>
              </>
            )}
          </Grid>
        </Grid>
      </Paper>

      {/* Error message */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Tabs */}
      <Paper sx={{ mb: 3 }}>
        <Tabs
          value={tabValue}
          onChange={handleTabChange}
          indicatorColor="primary"
          textColor="primary"
          variant="scrollable"
          scrollButtons="auto"
        >
          <Tab label="Basic Info" />
          <Tab label="Content" />
          <Tab label="Display" />
          <Tab label="Metadata" />
          <Tab label="Analytics" />
        </Tabs>

        {/* Basic Info Tab */}
        <TabPanel value={tabValue} index={0}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                label="Title"
                value={editMode ? editedTemplate.title : template.title}
                onChange={(e) => handleFieldChange('title', e.target.value)}
                fullWidth
                required
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                label="Category"
                value={editMode ? editedTemplate.category : template.category}
                onChange={(e) => handleFieldChange('category', e.target.value)}
                fullWidth
                required
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={editMode ? editedTemplate.status : template.status}
                    onChange={(e) => handleFieldChange('status', e.target.checked)}
                    disabled={!editMode}
                  />
                }
                label="Active"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={editMode ? editedTemplate.isPremium : template.isPremium}
                    onChange={(e) => handleFieldChange('isPremium', e.target.checked)}
                    disabled={!editMode}
                  />
                }
                label="Premium Template"
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Content Tab */}
        <TabPanel value={tabValue} index={1}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <TextField
                label="HTML Content"
                value={editMode ? editedTemplate.htmlContent : template.htmlContent}
                onChange={(e) => handleFieldChange('htmlContent', e.target.value)}
                fullWidth
                required
                disabled={!editMode}
                margin="normal"
                multiline
                rows={10}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="CSS Content"
                value={editMode ? editedTemplate.cssContent : template.cssContent}
                onChange={(e) => handleFieldChange('cssContent', e.target.value)}
                fullWidth
                disabled={!editMode}
                margin="normal"
                multiline
                rows={6}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="JavaScript Content"
                value={editMode ? editedTemplate.jsContent : template.jsContent}
                onChange={(e) => handleFieldChange('jsContent', e.target.value)}
                fullWidth
                disabled={!editMode}
                margin="normal"
                multiline
                rows={6}
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Display Tab */}
        <TabPanel value={tabValue} index={2}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                label="Preview URL"
                value={editMode ? editedTemplate.previewUrl : template.previewUrl}
                onChange={(e) => handleFieldChange('previewUrl', e.target.value)}
                fullWidth
                disabled={!editMode}
                margin="normal"
              />
              {template.previewUrl && (
                <Box sx={{ mt: 2, textAlign: 'center' }}>
                  <img 
                    src={template.previewUrl} 
                    alt="Template Preview" 
                    style={{ maxWidth: '100%', maxHeight: '300px', objectFit: 'contain' }}
                  />
                </Box>
              )}
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                label="Category Icon URL"
                value={editMode ? editedTemplate.categoryIcon : template.categoryIcon}
                onChange={(e) => handleFieldChange('categoryIcon', e.target.value)}
                fullWidth
                disabled={!editMode}
                margin="normal"
                helperText="Enter a valid URL for the category icon"
              />
              {template.categoryIcon && (
                <Box sx={{ mt: 2, textAlign: 'center' }}>
                  <img 
                    src={template.categoryIcon} 
                    alt="Category Icon" 
                    style={{ maxWidth: '100px', maxHeight: '100px', objectFit: 'contain' }}
                  />
                </Box>
              )}
            </Grid>
          </Grid>
        </TabPanel>

        {/* Metadata Tab */}
        <TabPanel value={tabValue} index={3}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                label="Festival Tag"
                value={editMode ? editedTemplate.festivalTag : template.festivalTag}
                onChange={(e) => handleFieldChange('festivalTag', e.target.value)}
                fullWidth
                disabled={!editMode}
                margin="normal"
                helperText="e.g., 'diwali', 'holi', 'christmas'"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                label="Creator ID"
                value={editMode ? editedTemplate.creatorId : template.creatorId}
                onChange={(e) => handleFieldChange('creatorId', e.target.value)}
                fullWidth
                disabled={!editMode}
                margin="normal"
                helperText="User ID of the template creator"
              />
            </Grid>
            <Grid item xs={12}>
              <Autocomplete
                multiple
                freeSolo
                options={[]}
                value={editMode ? editedTemplate.tags || [] : template.tags || []}
                onChange={handleTagsChange}
                disabled={!editMode}
                renderTags={(value, getTagProps) =>
                  value.map((option, index) => (
                    <Chip
                      variant="outlined"
                      label={option}
                      {...getTagProps({ index })}
                    />
                  ))
                }
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Tags"
                    helperText="Press Enter to add a tag"
                    fullWidth
                    margin="normal"
                  />
                )}
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Analytics Tab */}
        <TabPanel value={tabValue} index={4}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={4}>
              <TextField
                label="Usage Count"
                value={editMode ? editedTemplate.usageCount : template.usageCount || 0}
                onChange={(e) => handleFieldChange('usageCount', parseInt(e.target.value) || 0)}
                fullWidth
                type="number"
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                label="Likes"
                value={editMode ? editedTemplate.likes : template.likes || 0}
                onChange={(e) => handleFieldChange('likes', parseInt(e.target.value) || 0)}
                fullWidth
                type="number"
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                label="Favorites"
                value={editMode ? editedTemplate.favorites : template.favorites || 0}
                onChange={(e) => handleFieldChange('favorites', parseInt(e.target.value) || 0)}
                fullWidth
                type="number"
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12}>
              <Divider sx={{ my: 2 }} />
              <Typography variant="subtitle1" gutterBottom>
                Timestamps
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <TextField
                    label="Created At"
                    value={new Date(template.createdAt).toLocaleString()}
                    fullWidth
                    disabled
                    margin="normal"
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    label="Updated At"
                    value={new Date(template.updatedAt).toLocaleString()}
                    fullWidth
                    disabled
                    margin="normal"
                  />
                </Grid>
              </Grid>
            </Grid>
          </Grid>
        </TabPanel>
      </Paper>

      {/* Delete confirmation dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
      >
        <DialogTitle>Delete Template</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete the template "{template.title}"? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel} disabled={deleteLoading}>
            Cancel
          </Button>
          <Button 
            onClick={handleDeleteConfirm} 
            color="error" 
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

export default TemplateDetail; 