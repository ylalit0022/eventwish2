import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
  IconButton,
  Chip,
  Autocomplete
} from '@mui/material';
import {
  Save as SaveIcon,
  ArrowBack as ArrowBackIcon,
  Cancel as CancelIcon
} from '@mui/icons-material';
import { createTemplate } from '../api';

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

const TemplateCreate = () => {
  const navigate = useNavigate();
  const [template, setTemplate] = useState({
    title: '',
    category: '',
    htmlContent: '',
    cssContent: '',
    jsContent: '',
    previewUrl: '',
    status: true,
    isPremium: false,
    festivalTag: '',
    tags: [],
    categoryIcon: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [tabValue, setTabValue] = useState(0);

  // Handle tab change
  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  // Handle field change
  const handleFieldChange = (field, value) => {
    setTemplate(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Handle tags change
  const handleTagsChange = (event, newValue) => {
    setTemplate(prev => ({
      ...prev,
      tags: newValue
    }));
  };

  // Handle save
  const handleSave = async () => {
    // Validate required fields
    if (!template.title) {
      setError('Title is required');
      setTabValue(0); // Switch to Basic Info tab
      return;
    }

    if (!template.category) {
      setError('Category is required');
      setTabValue(0); // Switch to Basic Info tab
      return;
    }

    if (!template.htmlContent) {
      setError('HTML Content is required');
      setTabValue(1); // Switch to Content tab
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      const response = await createTemplate(template);
      
      if (response.success) {
        // Navigate to the detail page of the newly created template
        navigate(`/templates/${response.template._id}`);
      } else {
        throw new Error(response.message || 'Failed to create template');
      }
    } catch (err) {
      console.error('Error creating template:', err);
      setError('Failed to create template: ' + (err.message || 'Unknown error'));
    } finally {
      setLoading(false);
    }
  };

  // Handle cancel
  const handleCancel = () => {
    navigate('/templates');
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
              Create New Template
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
              Create Template
            </Button>
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
        </Tabs>

        {/* Basic Info Tab */}
        <TabPanel value={tabValue} index={0}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                label="Title"
                value={template.title}
                onChange={(e) => handleFieldChange('title', e.target.value)}
                fullWidth
                required
                margin="normal"
                error={!template.title}
                helperText={!template.title ? 'Title is required' : ''}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                label="Category"
                value={template.category}
                onChange={(e) => handleFieldChange('category', e.target.value)}
                fullWidth
                required
                margin="normal"
                error={!template.category}
                helperText={!template.category ? 'Category is required' : ''}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={template.status}
                    onChange={(e) => handleFieldChange('status', e.target.checked)}
                  />
                }
                label="Active"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={template.isPremium}
                    onChange={(e) => handleFieldChange('isPremium', e.target.checked)}
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
                value={template.htmlContent}
                onChange={(e) => handleFieldChange('htmlContent', e.target.value)}
                fullWidth
                required
                margin="normal"
                multiline
                rows={10}
                error={!template.htmlContent}
                helperText={!template.htmlContent ? 'HTML Content is required' : ''}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="CSS Content"
                value={template.cssContent}
                onChange={(e) => handleFieldChange('cssContent', e.target.value)}
                fullWidth
                margin="normal"
                multiline
                rows={6}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="JavaScript Content"
                value={template.jsContent}
                onChange={(e) => handleFieldChange('jsContent', e.target.value)}
                fullWidth
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
                value={template.previewUrl}
                onChange={(e) => handleFieldChange('previewUrl', e.target.value)}
                fullWidth
                margin="normal"
                helperText="URL to a preview image of the template"
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
                value={template.categoryIcon}
                onChange={(e) => handleFieldChange('categoryIcon', e.target.value)}
                fullWidth
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
                value={template.festivalTag}
                onChange={(e) => handleFieldChange('festivalTag', e.target.value)}
                fullWidth
                margin="normal"
                helperText="e.g., 'diwali', 'holi', 'christmas'"
              />
            </Grid>
            <Grid item xs={12}>
              <Autocomplete
                multiple
                freeSolo
                options={[]}
                value={template.tags}
                onChange={handleTagsChange}
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
      </Paper>
    </Box>
  );
};

export default TemplateCreate; 