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
  Autocomplete,
  FormLabel,
  RadioGroup,
  Radio,
  FormHelperText,
  Card,
  CardMedia,
  CardContent,
  CardActions,
  Pagination,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Checkbox
} from '@mui/material';
import {
  Save as SaveIcon,
  Delete as DeleteIcon,
  ArrowBack as ArrowBackIcon,
  Edit as EditIcon,
  Cancel as CancelIcon,
  Image as ImageIcon,
  Visibility as VisibilityIcon,
  SelectAll as SelectAllIcon,
  ClearAll as ClearAllIcon
} from '@mui/icons-material';
import { getTemplateById, updateTemplate, deleteTemplate, getCategoryIcons, getLanguages, getRegions, getTemplates } from '../api';
import { useSnackbar } from '../contexts/SnackbarContext';

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

const TEMPLATE_TYPES = ['html', 'image', 'video'];
const MODERATION_STATUSES = ['approved', 'pending', 'rejected'];

const TemplateDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showSnackbar } = useSnackbar();
  
  const [template, setTemplate] = useState(null);
  const [editedTemplate, setEditedTemplate] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saveLoading, setSaveLoading] = useState(false);
  const [error, setError] = useState(null);
  const [tabValue, setTabValue] = useState(0);
  const [editMode, setEditMode] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [categoryIcons, setCategoryIcons] = useState([]);
  const [selectedCategoryIconId, setSelectedCategoryIconId] = useState('');
  
  // New states for dropdown data
  const [languages, setLanguages] = useState([]);
  const [regions, setRegions] = useState([]);
  const [loadingLanguages, setLoadingLanguages] = useState(false);
  const [loadingRegions, setLoadingRegions] = useState(false);
  
  // Template reference states
  const [templates, setTemplates] = useState([]);
  const [templatesPage, setTemplatesPage] = useState(1);
  const [templatesLoading, setTemplatesLoading] = useState(false);
  const [templatesTotal, setTemplatesTotal] = useState(0);
  const [selectedTemplateId, setSelectedTemplateId] = useState('');
  const [templatePreviewDialogOpen, setTemplatePreviewDialogOpen] = useState(false);
  const [previewTemplate, setPreviewTemplate] = useState(null);

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
          // If variation template is set, remember it for UI
          if (response.template.variationOf) {
            setSelectedTemplateId(response.template.variationOf);
          }
        } else {
          throw new Error(response.message || 'Failed to load template details');
        }
      } catch (err) {
        console.error(`Error fetching template ${id}:`, err);
        
        // Extract detailed error message
        let errorMessage = 'Failed to load template details';
        if (err.response) {
          if (err.response.status === 500) {
            errorMessage = 'Server error while loading template. Please try again later or contact support.';
          } else if (err.response.status === 404) {
            errorMessage = 'Template not found. It may have been deleted.';
            // Navigate back to templates list after a delay
            setTimeout(() => navigate('/templates'), 3000);
          } else if (err.response.data && err.response.data.message) {
            errorMessage = err.response.data.message;
          } else {
            errorMessage = `Error (${err.response.status}): ${errorMessage}`;
          }
        } else if (err.message) {
          errorMessage = `${errorMessage}: ${err.message}`;
        }
        
        setError(errorMessage);
        showSnackbar(errorMessage, 'error');
      } finally {
        setLoading(false);
      }
    };

    fetchTemplate();
  }, [id, navigate, showSnackbar]);

  // Fetch category icons for dropdown
  useEffect(() => {
    const fetchCategoryIcons = async () => {
      try {
        const response = await getCategoryIcons(1, 100);
        if (response.success) {
          setCategoryIcons(response.data);
          
          // If template is loaded, find the matching icon ID
          if (template && template.categoryIcon) {
            const matchingIcon = response.data.find(icon => 
              icon.categoryIcon === template.categoryIcon
            );
            if (matchingIcon) {
              setSelectedCategoryIconId(matchingIcon._id);
            }
          }
        } else {
          console.error('Failed to fetch category icons:', response.message);
          showSnackbar('Failed to fetch category icons', 'error');
        }
      } catch (err) {
        console.error('Error fetching category icons:', err);
        
        // Extract detailed error message
        let errorMessage = 'Failed to fetch category icons';
        if (err.response) {
          if (err.response.status === 500) {
            errorMessage = 'Server error while fetching category icons. Please try again later.';
          } else if (err.response.data && err.response.data.message) {
            errorMessage = err.response.data.message;
          } else {
            errorMessage = `Error (${err.response.status}): ${errorMessage}`;
          }
        } else if (err.message) {
          errorMessage = `${errorMessage}: ${err.message}`;
        }
        
        showSnackbar(errorMessage, 'error');
      }
    };

    fetchCategoryIcons();
  }, [template?.categoryIcon, showSnackbar]);
  
  // Fetch languages for dropdown
  useEffect(() => {
    const fetchLanguages = async () => {
      try {
        setLoadingLanguages(true);
        const response = await getLanguages(1, 100);
        if (response.success) {
          setLanguages(response.data || []);
        } else {
          console.error('Failed to fetch languages:', response.message);
          showSnackbar('Failed to fetch languages', 'error');
        }
      } catch (err) {
        console.error('Error fetching languages:', err);
        
        // Extract detailed error message
        let errorMessage = 'Failed to fetch languages';
        if (err.response) {
          if (err.response.status === 500) {
            errorMessage = 'Server error while fetching languages. Please try again later.';
          } else if (err.response.data && err.response.data.message) {
            errorMessage = err.response.data.message;
          } else {
            errorMessage = `Error (${err.response.status}): ${errorMessage}`;
          }
        } else if (err.message) {
          errorMessage = `${errorMessage}: ${err.message}`;
        }
        
        showSnackbar(errorMessage, 'error');
      } finally {
        setLoadingLanguages(false);
      }
    };

    fetchLanguages();
  }, [showSnackbar]);
  
  // Fetch regions for dropdown
  useEffect(() => {
    const fetchRegions = async () => {
      try {
        setLoadingRegions(true);
        const response = await getRegions(1, 100);
        if (response.success) {
          setRegions(response.data || []);
        } else {
          console.error('Failed to fetch regions:', response.message);
          showSnackbar('Failed to fetch regions', 'error');
        }
      } catch (err) {
        console.error('Error fetching regions:', err);
        
        // Extract detailed error message
        let errorMessage = 'Failed to fetch regions';
        if (err.response) {
          if (err.response.status === 500) {
            errorMessage = 'Server error while fetching regions. Please try again later.';
          } else if (err.response.data && err.response.data.message) {
            errorMessage = err.response.data.message;
          } else {
            errorMessage = `Error (${err.response.status}): ${errorMessage}`;
          }
        } else if (err.message) {
          errorMessage = `${errorMessage}: ${err.message}`;
        }
        
        showSnackbar(errorMessage, 'error');
      } finally {
        setLoadingRegions(false);
      }
    };

    fetchRegions();
  }, [showSnackbar]);
  
  // Fetch templates for reference selection
  useEffect(() => {
    const fetchTemplates = async () => {
      try {
        setTemplatesLoading(true);
        const response = await getTemplates(templatesPage, 10, 'createdAt', 'desc');
        if (response.success) {
          setTemplates(response.templates || []);
          setTemplatesTotal(response.total || 0);
        } else {
          console.error('Failed to fetch templates:', response.message);
          showSnackbar('Failed to fetch templates', 'error');
        }
      } catch (err) {
        console.error('Error fetching templates:', err);
        
        // Extract detailed error message
        let errorMessage = 'Failed to fetch templates';
        if (err.response) {
          if (err.response.status === 500) {
            errorMessage = 'Server error while fetching templates. Please try again later.';
          } else if (err.response.data && err.response.data.message) {
            errorMessage = err.response.data.message;
          } else {
            errorMessage = `Error (${err.response.status}): ${errorMessage}`;
          }
        } else if (err.message) {
          errorMessage = `${errorMessage}: ${err.message}`;
        }
        
        showSnackbar(errorMessage, 'error');
      } finally {
        setTemplatesLoading(false);
      }
    };

    fetchTemplates();
  }, [templatesPage, showSnackbar]);

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
  
  // Handle style tags change
  const handleStyleTagsChange = (event, newValue) => {
    setEditedTemplate(prev => ({
      ...prev,
      styleTags: newValue
    }));
  };
  
  // Handle search keywords change
  const handleSearchKeywordsChange = (event, newValue) => {
    setEditedTemplate(prev => ({
      ...prev,
      searchKeywords: newValue
    }));
  };
  
  // Handle related templates change
  const handleRelatedTemplatesChange = (event, newValue) => {
    setEditedTemplate(prev => ({
      ...prev,
      relatedTemplates: newValue
    }));
  };
  
  // Handle pagination change
  const handleTemplatesPageChange = (event, value) => {
    setTemplatesPage(value);
  };
  
  // Handle preview dialog
  const handleOpenPreviewDialog = (template) => {
    setPreviewTemplate(template);
    setTemplatePreviewDialogOpen(true);
  };
  
  const handleClosePreviewDialog = () => {
    setTemplatePreviewDialogOpen(false);
  };
  
  // Handle selection of variation template
  const handleVariationTemplateSelect = (templateId) => {
    try {
    setEditedTemplate(prev => ({
      ...prev,
      variationOf: templateId
    }));
    setSelectedTemplateId(templateId);
      showSnackbar('Variation template selected', 'success');
    } catch (err) {
      console.error('Error selecting variation template:', err);
      showSnackbar(`Failed to select variation template: ${err.message || 'Unknown error'}`, 'error');
    }
  };
  
  // Handle selection of all templates
  const handleSelectAllTemplates = () => {
    try {
    setEditedTemplate(prev => ({
      ...prev,
      relatedTemplates: templates.map(t => t._id).filter(id => id !== template._id) // Don't include self
    }));
      showSnackbar('All templates selected as related', 'success');
    } catch (err) {
      console.error('Error selecting all templates:', err);
      showSnackbar(`Failed to select all templates: ${err.message || 'Unknown error'}`, 'error');
    }
  };
  
  // Handle deselection of all templates
  const handleDeselectAllTemplates = () => {
    try {
    setEditedTemplate(prev => ({
      ...prev,
      relatedTemplates: []
    }));
      showSnackbar('All related templates deselected', 'success');
    } catch (err) {
      console.error('Error deselecting all templates:', err);
      showSnackbar(`Failed to deselect templates: ${err.message || 'Unknown error'}`, 'error');
    }
  };

  // Handle save changes with improved error handling
  const handleSaveChanges = async () => {
    // Validate required fields
    const validationErrors = {};
    
    if (!editedTemplate.title) {
      validationErrors.title = 'Title is required';
    }

    if (!editedTemplate.category) {
      validationErrors.category = 'Category is required';
    }

    if (!editedTemplate.htmlContent && editedTemplate.templateType === 'html') {
      validationErrors.htmlContent = 'HTML Content is required for HTML templates';
    }
    
    if (editedTemplate.price < 0) {
      validationErrors.price = 'Price cannot be negative';
    }
    
    if (editedTemplate.isPremium && editedTemplate.price <= 0) {
      validationErrors.price = 'Premium templates must have a price greater than 0';
    }
    
    // If there are validation errors, show them and return
    if (Object.keys(validationErrors).length > 0) {
      setError(validationErrors);
      
      // Navigate to the tab with the first error
      if (validationErrors.title || validationErrors.category || validationErrors.price) {
        setTabValue(0); // Basic Info tab
      } else if (validationErrors.htmlContent) {
        setTabValue(1); // Content tab
      }
      
      return;
    }
    
      setSaveLoading(true);
      setError(null);
      
    try {
      // Format ObjectId references
      const formattedTemplate = {
        ...editedTemplate,
        language: editedTemplate.language === null ? '' : (editedTemplate.language ? String(editedTemplate.language) : ''),
        region: editedTemplate.region === null ? '' : (editedTemplate.region ? String(editedTemplate.region) : ''),
        variationOf: editedTemplate.variationOf === null ? '' : (editedTemplate.variationOf ? String(editedTemplate.variationOf) : ''),
        // Ensure numeric fields are numbers
        price: Number(editedTemplate.price),
        usageCount: Number(editedTemplate.usageCount || 0),
        likes: Number(editedTemplate.likes || 0),
        favorites: Number(editedTemplate.favorites || 0),
        viewCount: Number(editedTemplate.viewCount || 0),
        sharedCount: Number(editedTemplate.sharedCount || 0),
        downloadCount: Number(editedTemplate.downloadCount || 0),
        relatedTemplates: (editedTemplate.relatedTemplates || [])
          .filter(id => id !== null && id !== undefined)
          .map(id => id === null ? '' : String(id))
      };
      
      // Debug logging for language field
      console.log('Language field before submission:', {
        original: editedTemplate.language,
        originalType: typeof editedTemplate.language,
        formatted: formattedTemplate.language,
        formattedType: typeof formattedTemplate.language
      });
      
      console.log('Sending template data:', formattedTemplate);
      
      const response = await updateTemplate(id, formattedTemplate);
      
      if (response.success) {
        showSnackbar('Template updated successfully', 'success');
        setSaveLoading(false);
        
        // Refresh template data
        fetchTemplate();
      } else {
        showSnackbar(`Error: ${response.message || 'Failed to update template'}`, 'error');
        setSaveLoading(false);
      }
    } catch (error) {
      setSaveLoading(false);
      console.error(`Error updating template ${id}:`, error);
      
      // Enhanced error logging
      if (error.response && error.response.data) {
        console.error('Error response data:', error.response.data);
        
        // Check for specific MongoDB error about language field
        const errorMessage = error.response?.data?.error || '';
        if (errorMessage.includes('language override field') && errorMessage.includes('non-string type')) {
          console.error('Field error in language:', editedTemplate.language);
          
          // Show specific error for language field
          setError(prev => ({
            ...prev,
            language: 'Invalid language format. Must be a valid string ID.'
          }));
          
          // Navigate to the basic info tab where language field is
          setTabValue(0);
        }
      }
      
      showSnackbar(`Error updating template ${id}: ${error.message || 'Unknown error'}`, 'error');
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
      
      const response = await deleteTemplate(id);
      
      if (response.success) {
        showSnackbar(`Template "${template.title}" deleted successfully`, 'success');
        navigate('/templates');
      } else {
        throw new Error(response.message || 'Failed to delete template');
      }
    } catch (err) {
      console.error(`Error deleting template ${id}:`, err);
      
      // Extract detailed error message
      let errorMessage = 'Failed to delete template';
      if (err.response) {
        if (err.response.status === 500) {
          errorMessage = 'Server error while deleting template. Please try again later or contact support.';
        } else if (err.response.data && err.response.data.message) {
          errorMessage = err.response.data.message;
        } else {
          errorMessage = `Error (${err.response.status}): ${errorMessage}`;
        }
      } else if (err.message) {
        errorMessage = `${errorMessage}: ${err.message}`;
      }
      
      setError(errorMessage);
      showSnackbar(errorMessage, 'error');
      setDeleteDialogOpen(false);
    } finally {
      setDeleteLoading(false);
    }
  };

  // Handle back button
  const handleBackClick = () => {
    navigate('/templates');
  };

  // Get category icon URL for display
  const getCategoryIconUrl = () => {
    if (!template || !template.categoryIcon) return '';
    
    // If categoryIcon is already a URL string
    if (typeof template.categoryIcon === 'string' && template.categoryIcon.startsWith('http')) {
      return template.categoryIcon;
    }
    
    return '';
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
          <Tab label="Moderation" />
          <Tab label="References" />
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
              <FormControl fullWidth margin="normal" disabled={!editMode}>
                <FormLabel id="template-type-label">Template Type</FormLabel>
                <RadioGroup
                  row
                  aria-labelledby="template-type-label"
                  name="template-type"
                  value={editMode ? editedTemplate.templateType : template.templateType || 'html'}
                  onChange={(e) => handleFieldChange('templateType', e.target.value)}
                >
                  {TEMPLATE_TYPES.map(type => (
                    <FormControlLabel 
                      key={type} 
                      value={type} 
                      control={<Radio />} 
                      label={type.charAt(0).toUpperCase() + type.slice(1)} 
                      disabled={!editMode}
                    />
                  ))}
                </RadioGroup>
              </FormControl>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                label="Price"
                type="number"
                value={editMode ? editedTemplate.price : template.price || 0}
                onChange={(e) => handleFieldChange('price', e.target.value)}
                fullWidth
                disabled={!editMode}
                margin="normal"
                InputProps={{ inputProps: { min: 0, step: 0.01 } }}
                helperText={editMode && editedTemplate.isPremium ? "Required for premium templates" : "Set to 0 for free templates"}
                error={editMode && editedTemplate.isPremium && editedTemplate.price <= 0}
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
                required={editMode ? editedTemplate.templateType === 'html' : template.templateType === 'html'}
                disabled={!editMode}
                margin="normal"
                multiline
                rows={10}
                error={editMode && !editedTemplate.htmlContent && editedTemplate.templateType === 'html'}
                helperText={editMode && !editedTemplate.htmlContent && editedTemplate.templateType === 'html' 
                  ? 'HTML Content is required for HTML templates' 
                  : 'The HTML content of the template'}
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
              <FormControl fullWidth margin="normal" disabled={!editMode}>
                <InputLabel id="category-icon-label">Category Icon</InputLabel>
                <Select
                  labelId="category-icon-label"
                  value={editMode ? selectedCategoryIconId : selectedCategoryIconId}
                  onChange={(e) => {
                    // Find the selected icon and use its URL
                    const selectedId = e.target.value;
                    setSelectedCategoryIconId(selectedId);
                    
                    const selectedIcon = categoryIcons.find(icon => icon._id === selectedId);
                    handleFieldChange('categoryIcon', selectedIcon ? selectedIcon.categoryIcon : '');
                  }}
                  label="Category Icon"
                >
                  <MenuItem value="">None</MenuItem>
                  {categoryIcons.map((icon) => (
                    <MenuItem key={icon._id} value={icon._id}>
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        {icon.categoryIcon && (
                          <img 
                            src={icon.categoryIcon} 
                            alt={icon.category} 
                            style={{ width: 24, height: 24, marginRight: 8 }} 
                          />
                        )}
                        {icon.category}
                      </Box>
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              {(editMode ? editedTemplate.categoryIcon : template.categoryIcon) && (
                <Box sx={{ mt: 2, textAlign: 'center' }}>
                  <img 
                    src={editMode ? editedTemplate.categoryIcon : template.categoryIcon} 
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
            
            <Grid item xs={12}>
              <Autocomplete
                multiple
                freeSolo
                options={[]}
                value={editMode ? editedTemplate.styleTags || [] : template.styleTags || []}
                onChange={handleStyleTagsChange}
                disabled={!editMode}
                renderTags={(value, getTagProps) =>
                  value.map((option, index) => (
                    <Chip
                      variant="outlined"
                      label={option}
                      {...getTagProps({ index })}
                      color="primary"
                    />
                  ))
                }
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Style Tags"
                    helperText="Style-related tags like 'modern', 'retro', 'minimalist'"
                    fullWidth
                    margin="normal"
                  />
                )}
              />
            </Grid>
            
            <Grid item xs={12}>
              <Autocomplete
                multiple
                freeSolo
                options={[]}
                value={editMode ? editedTemplate.searchKeywords || [] : template.searchKeywords || []}
                onChange={handleSearchKeywordsChange}
                disabled={!editMode}
                renderTags={(value, getTagProps) =>
                  value.map((option, index) => (
                    <Chip
                      variant="outlined"
                      label={option}
                      {...getTagProps({ index })}
                      color="secondary"
                    />
                  ))
                }
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Search Keywords"
                    helperText="Keywords for improving search results"
                    fullWidth
                    margin="normal"
                  />
                )}
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Moderation Tab */}
        <TabPanel value={tabValue} index={4}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth margin="normal" disabled={!editMode}>
                <FormLabel id="moderation-status-label">Moderation Status</FormLabel>
                <RadioGroup
                  aria-labelledby="moderation-status-label"
                  name="moderation-status"
                  value={editMode ? editedTemplate.moderationStatus : template.moderationStatus || 'pending'}
                  onChange={(e) => handleFieldChange('moderationStatus', e.target.value)}
                >
                  {MODERATION_STATUSES.map(status => (
                    <FormControlLabel 
                      key={status} 
                      value={status} 
                      control={<Radio />} 
                      label={status.charAt(0).toUpperCase() + status.slice(1)} 
                      disabled={!editMode}
                    />
                  ))}
                </RadioGroup>
                <FormHelperText>
                  Templates need approval before appearing in search results
                </FormHelperText>
              </FormControl>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Typography variant="subtitle1" gutterBottom>
                Promotion Settings
              </Typography>
              <Box sx={{ ml: 1, mt: 2 }}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={editMode ? editedTemplate.isFeatured : template.isFeatured || false}
                      onChange={(e) => handleFieldChange('isFeatured', e.target.checked)}
                      disabled={!editMode}
                    />
                  }
                  label="Featured Template"
                />
                <FormHelperText sx={{ mt: -1, mb: 2, ml: 4 }}>
                  Featured templates are highlighted on the app homepage
                </FormHelperText>
                
                <FormControlLabel
                  control={
                    <Switch
                      checked={editMode ? editedTemplate.isTrending : template.isTrending || false}
                      onChange={(e) => handleFieldChange('isTrending', e.target.checked)}
                      disabled={!editMode}
                    />
                  }
                  label="Trending Template"
                />
                <FormHelperText sx={{ mt: -1, mb: 2, ml: 4 }}>
                  Manually mark as trending to show in trending section
                </FormHelperText>
                
                <FormControlLabel
                  control={
                    <Switch
                      checked={editMode ? editedTemplate.isFlagged : template.isFlagged || false}
                      onChange={(e) => handleFieldChange('isFlagged', e.target.checked)}
                      disabled={!editMode}
                    />
                  }
                  label="Flagged for Review"
                />
                <FormHelperText sx={{ mt: -1, ml: 4 }}>
                  Flag templates that need additional review
                </FormHelperText>
              </Box>
            </Grid>
          </Grid>
        </TabPanel>

        {/* References Tab */}
        <TabPanel value={tabValue} index={5}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth margin="normal" disabled={!editMode || loadingLanguages}>
                <InputLabel id="language-label">Language</InputLabel>
                <Select
                  labelId="language-label"
                  value={editMode ? editedTemplate.language || '' : template.language || ''}
                  onChange={(e) => handleFieldChange('language', e.target.value)}
                  label="Language"
                >
                  <MenuItem value="">None</MenuItem>
                  {languages.map((lang) => (
                    <MenuItem key={lang._id} value={lang._id}>
                      {lang.name} ({lang.code})
                    </MenuItem>
                  ))}
                </Select>
                <FormHelperText>
                  Language this template is designed for
                </FormHelperText>
              </FormControl>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <FormControl fullWidth margin="normal" disabled={!editMode || loadingRegions}>
                <InputLabel id="region-label">Region</InputLabel>
                <Select
                  labelId="region-label"
                  value={editMode ? editedTemplate.region || '' : template.region || ''}
                  onChange={(e) => handleFieldChange('region', e.target.value)}
                  label="Region"
                >
                  <MenuItem value="">None</MenuItem>
                  {regions.map((region) => (
                    <MenuItem key={region._id} value={region._id}>
                      {region.name} ({region.code})
                    </MenuItem>
                  ))}
                </Select>
                <FormHelperText>
                  Region this template is targeting
                </FormHelperText>
              </FormControl>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                label="Experiment Tag"
                value={editMode ? editedTemplate.experimentTag || '' : template.experimentTag || ''}
                onChange={(e) => handleFieldChange('experimentTag', e.target.value)}
                fullWidth
                disabled={!editMode}
                margin="normal"
                helperText="Tag for A/B testing or experiments"
              />
            </Grid>
            
            <Grid item xs={12}>
              <Divider sx={{ my: 3 }} />
              <Typography variant="h6" gutterBottom>
                Variation Template
              </Typography>
              <Typography variant="body2" color="textSecondary" paragraph>
                {editMode 
                  ? "Select a template that this template is a variation of" 
                  : "Template that this template is a variation of"}
              </Typography>
              
              {editMode ? (
                templatesLoading ? (
                  <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
                    <CircularProgress />
                  </Box>
                ) : (
                  <>
                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mb: 2 }}>
                      {templates
                        .filter(t => t._id !== template._id) // Don't show self
                        .map((tmpl) => (
                        <Card 
                          key={tmpl._id} 
                          sx={{ 
                            width: 200, 
                            border: editedTemplate.variationOf === tmpl._id ? '2px solid #1976d2' : 'none',
                            cursor: 'pointer'
                          }}
                          onClick={() => handleVariationTemplateSelect(tmpl._id)}
                        >
                          <CardMedia
                            component="img"
                            height="120"
                            image={tmpl.previewUrl || 'https://via.placeholder.com/150?text=No+Preview'}
                            alt={tmpl.title}
                            sx={{ objectFit: 'contain', bgcolor: '#f5f5f5' }}
                          />
                          <CardContent sx={{ p: 1, pb: 0 }}>
                            <Typography variant="subtitle2" noWrap>
                              {tmpl.title}
                            </Typography>
                            <Typography variant="caption" color="textSecondary" component="div">
                              ID: {tmpl._id.substring(0, 8)}...
                            </Typography>
                          </CardContent>
                          <CardActions sx={{ p: 1, pt: 0 }}>
                            <Button 
                              size="small" 
                              startIcon={<VisibilityIcon />} 
                              onClick={(e) => {
                                e.stopPropagation();
                                handleOpenPreviewDialog(tmpl);
                              }}
                            >
                              Preview
                            </Button>
                          </CardActions>
                        </Card>
                      ))}
                    </Box>
                    <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
                      <Pagination 
                        count={Math.ceil(templatesTotal / 10)} 
                        page={templatesPage} 
                        onChange={handleTemplatesPageChange} 
                        color="primary" 
                      />
                    </Box>
                  </>
                )
              ) : (
                template.variationOf ? (
                  <Box sx={{ 
                    p: 2, 
                    border: '1px solid #e0e0e0',
                    borderRadius: 1
                  }}>
                    <Typography variant="body1">
                      Variation of template ID: {template.variationOf}
                    </Typography>
                    <Button 
                      size="small" 
                      startIcon={<VisibilityIcon />}
                      sx={{ mt: 1 }}
                      onClick={() => {
                        // Find the template in the loaded templates
                        const variationTemplate = templates.find(t => t._id === template.variationOf);
                        if (variationTemplate) {
                          handleOpenPreviewDialog(variationTemplate);
                        } else {
                          showSnackbar('Template preview not available in current page', 'info');
                        }
                      }}
                    >
                      Preview Template
                    </Button>
                  </Box>
                ) : (
                  <Typography color="textSecondary">
                    No variation template selected
                  </Typography>
                )
              )}
              
              {editMode && editedTemplate.variationOf && (
                <TextField
                  label="Variation Template ID"
                  value={editedTemplate.variationOf}
                  onChange={(e) => handleFieldChange('variationOf', e.target.value)}
                  fullWidth
                  margin="normal"
                  helperText="ID of the template this is a variation of"
                />
              )}
            </Grid>
            
            <Grid item xs={12}>
              <Divider sx={{ my: 3 }} />
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">
                  Related Templates
                </Typography>
                {editMode && (
                  <Box>
                    <Button 
                      startIcon={<SelectAllIcon />} 
                      onClick={handleSelectAllTemplates}
                      size="small"
                      sx={{ mr: 1 }}
                    >
                      Select All
                    </Button>
                    <Button 
                      startIcon={<ClearAllIcon />} 
                      onClick={handleDeselectAllTemplates}
                      size="small"
                    >
                      Deselect All
                    </Button>
                  </Box>
                )}
              </Box>
              
              <Typography variant="body2" color="textSecondary" paragraph>
                {editMode 
                  ? "Select templates that are related to this template" 
                  : "Templates that are related to this template"}
              </Typography>
              
              {editMode ? (
                templatesLoading ? (
                  <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
                    <CircularProgress />
                  </Box>
                ) : (
                  <List sx={{ width: '100%', bgcolor: 'background.paper' }}>
                    {templates
                      .filter(t => t._id !== template._id) // Don't show self
                      .map((tmpl) => (
                      <ListItem
                        key={tmpl._id}
                        sx={{ 
                          border: '1px solid #e0e0e0', 
                          borderRadius: 1, 
                          mb: 1,
                          '&:hover': { bgcolor: '#f5f5f5' }
                        }}
                      >
                        <ListItemIcon>
                          <Checkbox
                            edge="start"
                            checked={editedTemplate.relatedTemplates?.includes(tmpl._id) || false}
                            onChange={(e) => {
                              if (e.target.checked) {
                                handleFieldChange('relatedTemplates', 
                                  [...(editedTemplate.relatedTemplates || []), tmpl._id]
                                );
                              } else {
                                handleFieldChange('relatedTemplates', 
                                  (editedTemplate.relatedTemplates || []).filter(id => id !== tmpl._id)
                                );
                              }
                            }}
                          />
                        </ListItemIcon>
                        <Box 
                          sx={{ 
                            width: 60, 
                            height: 60, 
                            mr: 2, 
                            bgcolor: '#f5f5f5',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                          }}
                        >
                          {tmpl.previewUrl ? (
                            <img 
                              src={tmpl.previewUrl} 
                              alt={tmpl.title} 
                              style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }}
                            />
                          ) : (
                            <ImageIcon color="disabled" />
                          )}
                        </Box>
                        <ListItemText 
                          primary={tmpl.title} 
                          secondary={`ID: ${tmpl._id} • Category: ${tmpl.category}`}
                        />
                        <Button 
                          size="small" 
                          startIcon={<VisibilityIcon />} 
                          onClick={() => handleOpenPreviewDialog(tmpl)}
                        >
                          Preview
                        </Button>
                      </ListItem>
                    ))}
                  </List>
                )
              ) : (
                template.relatedTemplates && template.relatedTemplates.length > 0 ? (
                  <List sx={{ width: '100%', bgcolor: 'background.paper' }}>
                    {template.relatedTemplates.map((relatedId, index) => {
                      // Find the template in the loaded templates
                      const relatedTemplate = templates.find(t => t._id === relatedId);
                      
                      return (
                        <ListItem
                          key={relatedId || index}
                          sx={{ 
                            border: '1px solid #e0e0e0', 
                            borderRadius: 1, 
                            mb: 1
                          }}
                        >
                          {relatedTemplate ? (
                            <>
                              <Box 
                                sx={{ 
                                  width: 60, 
                                  height: 60, 
                                  mr: 2, 
                                  bgcolor: '#f5f5f5',
                                  display: 'flex',
                                  alignItems: 'center',
                                  justifyContent: 'center'
                                }}
                              >
                                {relatedTemplate.previewUrl ? (
                                  <img 
                                    src={relatedTemplate.previewUrl} 
                                    alt={relatedTemplate.title} 
                                    style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }}
                                  />
                                ) : (
                                  <ImageIcon color="disabled" />
                                )}
                              </Box>
                              <ListItemText 
                                primary={relatedTemplate.title} 
                                secondary={`ID: ${relatedTemplate._id} • Category: ${relatedTemplate.category}`}
                              />
                              <Button 
                                size="small" 
                                startIcon={<VisibilityIcon />} 
                                onClick={() => handleOpenPreviewDialog(relatedTemplate)}
                              >
                                Preview
                              </Button>
                            </>
                          ) : (
                            <ListItemText primary={`Template ID: ${relatedId}`} secondary="Template details not available in current page" />
                          )}
                        </ListItem>
                      );
                    })}
                  </List>
                ) : (
                  <Typography color="textSecondary">
                    No related templates selected
                  </Typography>
                )
              )}
              
              {editMode && (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
                  <Pagination 
                    count={Math.ceil(templatesTotal / 10)} 
                    page={templatesPage} 
                    onChange={handleTemplatesPageChange} 
                    color="primary" 
                  />
                </Box>
              )}
            </Grid>
          </Grid>
          
          {/* Preview Dialog */}
          <Dialog
            open={templatePreviewDialogOpen}
            onClose={handleClosePreviewDialog}
            maxWidth="md"
            fullWidth
          >
            <DialogTitle>
              {previewTemplate?.title || 'Template Preview'}
            </DialogTitle>
            <DialogContent>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Box sx={{ textAlign: 'center' }}>
                    {previewTemplate?.previewUrl ? (
                      <img 
                        src={previewTemplate.previewUrl} 
                        alt={previewTemplate.title}
                        style={{ maxWidth: '100%', maxHeight: '300px', objectFit: 'contain' }}
                      />
                    ) : (
                      <Box 
                        sx={{ 
                          height: 200, 
                          bgcolor: '#f5f5f5', 
                          display: 'flex', 
                          alignItems: 'center', 
                          justifyContent: 'center' 
                        }}
                      >
                        <Typography color="textSecondary">No preview available</Typography>
                      </Box>
                    )}
                  </Box>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle1">Details</Typography>
                  <Typography variant="body2"><strong>ID:</strong> {previewTemplate?._id}</Typography>
                  <Typography variant="body2"><strong>Category:</strong> {previewTemplate?.category}</Typography>
                  <Typography variant="body2"><strong>Type:</strong> {previewTemplate?.templateType}</Typography>
                  <Typography variant="body2"><strong>Status:</strong> {previewTemplate?.status ? 'Active' : 'Inactive'}</Typography>
                  
                  <Divider sx={{ my: 2 }} />
                  <Typography variant="subtitle1">Tags</Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 1 }}>
                    {previewTemplate?.tags?.map((tag, index) => (
                      <Chip key={index} label={tag} size="small" />
                    )) || <Typography variant="body2">No tags</Typography>}
                  </Box>
                </Grid>
              </Grid>
            </DialogContent>
            <DialogActions>
              <Button onClick={handleClosePreviewDialog}>Close</Button>
              {previewTemplate && editMode && (
                <>
                  <Button 
                    color="primary"
                    onClick={() => {
                      handleVariationTemplateSelect(previewTemplate._id);
                      handleClosePreviewDialog();
                    }}
                  >
                    Select as Variation Template
                  </Button>
                  <Button 
                    color="primary"
                    onClick={() => {
                      const isSelected = editedTemplate.relatedTemplates?.includes(previewTemplate._id);
                      if (isSelected) {
                        handleFieldChange('relatedTemplates', 
                          (editedTemplate.relatedTemplates || []).filter(id => id !== previewTemplate._id)
                        );
                      } else {
                        handleFieldChange('relatedTemplates', 
                          [...(editedTemplate.relatedTemplates || []), previewTemplate._id]
                        );
                      }
                      handleClosePreviewDialog();
                    }}
                  >
                    {editedTemplate.relatedTemplates?.includes(previewTemplate._id) 
                      ? 'Remove from Related' 
                      : 'Add to Related'
                    }
                  </Button>
                </>
              )}
            </DialogActions>
          </Dialog>
        </TabPanel>

        {/* Analytics Tab */}
        <TabPanel value={tabValue} index={6}>
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
            
            <Grid item xs={12} md={4}>
              <TextField
                label="View Count"
                value={editMode ? editedTemplate.viewCount : template.viewCount || 0}
                onChange={(e) => handleFieldChange('viewCount', parseInt(e.target.value) || 0)}
                fullWidth
                type="number"
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                label="Shared Count"
                value={editMode ? editedTemplate.sharedCount : template.sharedCount || 0}
                onChange={(e) => handleFieldChange('sharedCount', parseInt(e.target.value) || 0)}
                fullWidth
                type="number"
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                label="Download Count"
                value={editMode ? editedTemplate.downloadCount : template.downloadCount || 0}
                onChange={(e) => handleFieldChange('downloadCount', parseInt(e.target.value) || 0)}
                fullWidth
                type="number"
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            
            <Grid item xs={12} md={4}>
              <TextField
                label="Report Count"
                value={editMode ? editedTemplate.reportCount : template.reportCount || 0}
                onChange={(e) => handleFieldChange('reportCount', parseInt(e.target.value) || 0)}
                fullWidth
                type="number"
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                label="Rating"
                value={editMode ? editedTemplate.rating : template.rating || 0}
                onChange={(e) => handleFieldChange('rating', parseFloat(e.target.value) || 0)}
                fullWidth
                type="number"
                disabled={!editMode}
                margin="normal"
                InputProps={{ inputProps: { min: 0, max: 5, step: 0.1 } }}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                label="Rating Count"
                value={editMode ? editedTemplate.ratingCount : template.ratingCount || 0}
                onChange={(e) => handleFieldChange('ratingCount', parseInt(e.target.value) || 0)}
                fullWidth
                type="number"
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            
            <Grid item xs={12}>
              <Divider sx={{ my: 2 }} />
              <Typography variant="subtitle1" gutterBottom>
                Performance Metrics
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} md={4}>
                  <TextField
                    label="Daily Usage"
                    value={editMode 
                      ? (editedTemplate.performanceLog?.dailyUsage || 0) 
                      : (template.performanceLog?.dailyUsage || 0)}
                    onChange={(e) => handleFieldChange('performanceLog', {
                      ...editedTemplate.performanceLog,
                      dailyUsage: parseInt(e.target.value) || 0
                    })}
                    fullWidth
                    type="number"
                    disabled={!editMode}
                    margin="normal"
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    label="Weekly Usage"
                    value={editMode 
                      ? (editedTemplate.performanceLog?.weeklyUsage || 0) 
                      : (template.performanceLog?.weeklyUsage || 0)}
                    onChange={(e) => handleFieldChange('performanceLog', {
                      ...editedTemplate.performanceLog,
                      weeklyUsage: parseInt(e.target.value) || 0
                    })}
                    fullWidth
                    type="number"
                    disabled={!editMode}
                    margin="normal"
                  />
                </Grid>
                <Grid item xs={12} md={4}>
                  <TextField
                    label="Last Used At"
                    value={editMode 
                      ? (editedTemplate.performanceLog?.lastUsedAt 
                        ? new Date(editedTemplate.performanceLog.lastUsedAt).toISOString().slice(0, 16) 
                        : '') 
                      : (template.performanceLog?.lastUsedAt 
                        ? new Date(template.performanceLog.lastUsedAt).toISOString().slice(0, 16) 
                        : '')}
                    onChange={(e) => handleFieldChange('performanceLog', {
                      ...editedTemplate.performanceLog,
                      lastUsedAt: e.target.value ? new Date(e.target.value).toISOString() : null
                    })}
                    fullWidth
                    type="datetime-local"
                    disabled={!editMode}
                    margin="normal"
                    InputLabelProps={{ shrink: true }}
                  />
                </Grid>
              </Grid>
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