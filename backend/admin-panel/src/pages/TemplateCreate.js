import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
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
  Autocomplete,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Radio,
  RadioGroup,
  FormLabel,
  FormHelperText,
  Divider,
  Card,
  CardMedia,
  CardContent,
  CardActions,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Pagination,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Checkbox
} from '@mui/material';
import {
  Save as SaveIcon,
  ArrowBack as ArrowBackIcon,
  Cancel as CancelIcon,
  Image as ImageIcon,
  Visibility as VisibilityIcon,
  SelectAll as SelectAllIcon,
  ClearAll as ClearAllIcon,
  ContentCopy as ContentCopyIcon
} from '@mui/icons-material';
import { createTemplate, getCategoryIcons, getLanguages, getRegions, getTemplates, getTemplate } from '../api';
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

const TemplateCreate = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { showSnackbar } = useSnackbar();
  
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
    categoryIcon: '',
    // New fields
    styleTags: [],
    searchKeywords: [],
    templateType: 'html',
    price: 0,
    isFeatured: false,
    isTrending: false,
    isFlagged: false,
    moderationStatus: 'pending',
    language: '',
    region: '',
    experimentTag: '',
    variationOf: '',
    relatedTemplates: []
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [tabValue, setTabValue] = useState(0);
  const [categoryIcons, setCategoryIcons] = useState([]);
  const [selectedCategoryIconId, setSelectedCategoryIconId] = useState('');
  
  // New states for dropdown data
  const [languages, setLanguages] = useState([]);
  const [regions, setRegions] = useState([]);
  const [loadingLanguages, setLoadingLanguages] = useState(false);
  const [loadingRegions, setLoadingRegions] = useState(false);

  // New state variables for template references
  const [templates, setTemplates] = useState([]);
  const [templatesPage, setTemplatesPage] = useState(1);
  const [templatesLoading, setTemplatesLoading] = useState(false);
  const [templatesTotal, setTemplatesTotal] = useState(0);
  const [selectedTemplateId, setSelectedTemplateId] = useState('');
  const [templatePreviewDialogOpen, setTemplatePreviewDialogOpen] = useState(false);
  const [previewTemplate, setPreviewTemplate] = useState(null);
  
  // Check for copy parameter in URL
  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    const copyTemplateId = searchParams.get('copy');
    
    if (copyTemplateId) {
      const fetchTemplateForCopy = async () => {
        try {
          setLoading(true);
          setError(null);
          
          const response = await getTemplate(copyTemplateId);
          
          if (response.success) {
            // Create a copy of the template with a modified title to indicate it's a copy
            const templateData = response.template;
            setTemplate({
              ...templateData,
              title: `${templateData.title} (Copy)`,
              _id: undefined // Remove ID to ensure a new template is created
            });
            
            showSnackbar('Template data loaded for copying. Please update details as needed.', 'info');
          } else {
            throw new Error(response.message || 'Failed to fetch template for copying');
          }
        } catch (err) {
          console.error('Error fetching template for copy:', err);
          setError('Failed to load template for copying: ' + (err.message || 'Unknown error'));
          showSnackbar('Failed to load template for copying', 'error');
        } finally {
          setLoading(false);
        }
      };
      
      fetchTemplateForCopy();
    }
  }, [location.search, showSnackbar]);
  
  // Fetch category icons for dropdown
  useEffect(() => {
    const fetchCategoryIcons = async () => {
      try {
        const response = await getCategoryIcons(1, 100);
        if (response.success) {
          setCategoryIcons(response.data);
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
  }, [showSnackbar]);
  
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
  
  // Handle style tags change
  const handleStyleTagsChange = (event, newValue) => {
    setTemplate(prev => ({
      ...prev,
      styleTags: newValue
    }));
  };
  
  // Handle search keywords change
  const handleSearchKeywordsChange = (event, newValue) => {
    setTemplate(prev => ({
      ...prev,
      searchKeywords: newValue
    }));
  };
  
  // Handle related templates change
  const handleRelatedTemplatesChange = (event, newValue) => {
    setTemplate(prev => ({
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
    setTemplate(prev => ({
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
    setTemplate(prev => ({
      ...prev,
      relatedTemplates: templates.map(t => t._id)
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
    setTemplate(prev => ({
      ...prev,
      relatedTemplates: []
    }));
      showSnackbar('All related templates deselected', 'success');
    } catch (err) {
      console.error('Error deselecting all templates:', err);
      showSnackbar(`Failed to deselect templates: ${err.message || 'Unknown error'}`, 'error');
    }
  };

  // Handle save
  const handleSave = async () => {
    // Validate required fields
    const validationErrors = {};
    
    if (!template.title) {
      validationErrors.title = 'Title is required';
    }

    if (!template.category) {
      validationErrors.category = 'Category is required';
    }

    if (!template.htmlContent && template.templateType === 'html') {
      validationErrors.htmlContent = 'HTML Content is required for HTML templates';
    }
    
    if (template.price < 0) {
      validationErrors.price = 'Price cannot be negative';
    }
    
    if (template.isPremium && template.price <= 0) {
      validationErrors.price = 'Premium templates must have a price greater than 0';
    }

    // If there are validation errors, show them and stop
    if (Object.keys(validationErrors).length > 0) {
      // Set the first error as the main error message
      const firstErrorField = Object.keys(validationErrors)[0];
      const firstErrorMessage = validationErrors[firstErrorField];
      setError(firstErrorMessage);
      
      // Show the validation error in a snackbar
      showSnackbar(`Validation error: ${firstErrorMessage}`, 'error');
      
      // Switch to the appropriate tab for the first error
      if (firstErrorField === 'title' || firstErrorField === 'category' || firstErrorField === 'price') {
        setTabValue(0); // Basic Info tab
      } else if (firstErrorField === 'htmlContent') {
        setTabValue(1); // Content tab
      }
      
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      // Format ObjectId references
      const formattedTemplate = {
        ...template,
        language: template.language ? String(template.language) : null,
        region: template.region ? String(template.region) : null,
        variationOf: template.variationOf ? String(template.variationOf) : null,
        // Ensure numeric fields are numbers
        price: Number(template.price),
        relatedTemplates: template.relatedTemplates.filter(id => id && id.trim() !== '').map(id => String(id))
      };
      
      console.log('Sending template data:', formattedTemplate);
      console.log('Language field type:', typeof formattedTemplate.language, 'Value:', formattedTemplate.language);
      
      const response = await createTemplate(formattedTemplate);
      
      if (response.success) {
        showSnackbar('Template created successfully', 'success');
        // Navigate to the detail page of the newly created template
        navigate(`/templates/${response.template._id}`);
      } else {
        throw new Error(response.message || 'Failed to create template');
      }
    } catch (err) {
      console.error('Error creating template:', err);
      
      // Extract detailed error message
      let errorMessage = 'Failed to create template';
      let errorField = null;
      
      if (err.response) {
        console.error('Error response data:', err.response.data);
        
        if (err.response.status === 500) {
          // Check for specific MongoDB errors
          const errorText = err.response.data?.error || '';
          
          if (errorText.includes('language override field')) {
            errorMessage = 'Error with language field: Must be a valid string ID';
            errorField = 'language';
            setTabValue(5); // References tab
          } else if (errorText.includes('region')) {
            errorMessage = 'Error with region field: Must be a valid string ID';
            errorField = 'region';
            setTabValue(5); // References tab
          } else if (errorText.includes('variationOf')) {
            errorMessage = 'Error with variation template: Must be a valid string ID';
            errorField = 'variationOf';
            setTabValue(5); // References tab
          } else {
            errorMessage = 'Server error while creating template. Please try again later or contact support.';
          }
        } else if (err.response.status === 400) {
          // Handle validation errors from the server
          if (err.response.data && err.response.data.validationErrors) {
            const validationErrors = err.response.data.validationErrors;
            const errorFields = Object.keys(validationErrors);
            
            if (errorFields.length > 0) {
              errorField = errorFields[0];
              errorMessage = `Validation error: ${validationErrors[errorField]}`;
              
              // Switch to the appropriate tab for the error
              if (['title', 'category', 'price'].includes(errorField)) {
                setTabValue(0); // Basic Info tab
              } else if (['htmlContent', 'cssContent', 'jsContent'].includes(errorField)) {
                setTabValue(1); // Content tab
              } else if (['language', 'region', 'variationOf'].includes(errorField)) {
                setTabValue(5); // References tab
              }
            } else {
              errorMessage = err.response.data.message || 'Validation error';
            }
          } else if (err.response.data && err.response.data.message) {
            errorMessage = err.response.data.message;
          }
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
      
      // If we identified a specific field with an error, highlight it
      if (errorField) {
        console.error(`Field error in ${errorField}:`, template[errorField]);
      }
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
              {location.search.includes('copy=') ? 'Create Template Copy' : 'Create New Template'}
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
          <Tab label="Moderation" />
          <Tab label="References" />
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
              <FormControl fullWidth margin="normal">
                <FormLabel id="template-type-label">Template Type</FormLabel>
                <RadioGroup
                  row
                  aria-labelledby="template-type-label"
                  name="template-type"
                  value={template.templateType}
                  onChange={(e) => handleFieldChange('templateType', e.target.value)}
                >
                  {TEMPLATE_TYPES.map(type => (
                    <FormControlLabel 
                      key={type} 
                      value={type} 
                      control={<Radio />} 
                      label={type.charAt(0).toUpperCase() + type.slice(1)} 
                    />
                  ))}
                </RadioGroup>
              </FormControl>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                label="Price"
                type="number"
                value={template.price}
                onChange={(e) => handleFieldChange('price', e.target.value)}
                fullWidth
                margin="normal"
                InputProps={{ inputProps: { min: 0, step: 0.01 } }}
                helperText={template.isPremium ? "Required for premium templates" : "Set to 0 for free templates"}
                error={template.isPremium && template.price <= 0}
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
                required={template.templateType === 'html'}
                margin="normal"
                multiline
                rows={10}
                error={!template.htmlContent && template.templateType === 'html'}
                helperText={!template.htmlContent && template.templateType === 'html' ? 'HTML Content is required for HTML templates' : ''}
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
              <FormControl fullWidth margin="normal">
                <InputLabel id="category-icon-label">Category Icon</InputLabel>
                <Select
                  labelId="category-icon-label"
                  value={selectedCategoryIconId}
                  onChange={(e) => {
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
            
            <Grid item xs={12}>
              <Autocomplete
                multiple
                freeSolo
                options={[]}
                value={template.styleTags}
                onChange={handleStyleTagsChange}
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
                value={template.searchKeywords}
                onChange={handleSearchKeywordsChange}
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
              <FormControl fullWidth margin="normal">
                <FormLabel id="moderation-status-label">Moderation Status</FormLabel>
                <RadioGroup
                  aria-labelledby="moderation-status-label"
                  name="moderation-status"
                  value={template.moderationStatus}
                  onChange={(e) => handleFieldChange('moderationStatus', e.target.value)}
                >
                  {MODERATION_STATUSES.map(status => (
                    <FormControlLabel 
                      key={status} 
                      value={status} 
                      control={<Radio />} 
                      label={status.charAt(0).toUpperCase() + status.slice(1)} 
                    />
                  ))}
                </RadioGroup>
                <FormHelperText>
                  New templates are set to "pending" by default and need to be approved
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
                      checked={template.isFeatured}
                      onChange={(e) => handleFieldChange('isFeatured', e.target.checked)}
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
                      checked={template.isTrending}
                      onChange={(e) => handleFieldChange('isTrending', e.target.checked)}
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
                      checked={template.isFlagged}
                      onChange={(e) => handleFieldChange('isFlagged', e.target.checked)}
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
              <FormControl fullWidth margin="normal" disabled={loadingLanguages}>
                <InputLabel id="language-label">Language</InputLabel>
                <Select
                  labelId="language-label"
                  value={template.language}
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
              <FormControl fullWidth margin="normal" disabled={loadingRegions}>
                <InputLabel id="region-label">Region</InputLabel>
                <Select
                  labelId="region-label"
                  value={template.region}
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
                value={template.experimentTag}
                onChange={(e) => handleFieldChange('experimentTag', e.target.value)}
                fullWidth
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
                Select a template that this template is a variation of
              </Typography>
              
              {templatesLoading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
                  <CircularProgress />
                </Box>
              ) : (
                <>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mb: 2 }}>
                    {templates.map((tmpl) => (
                      <Card 
                        key={tmpl._id} 
                        sx={{ 
                          width: 200, 
                          border: template.variationOf === tmpl._id ? '2px solid #1976d2' : 'none',
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
              )}
              
              {template.variationOf && (
                <TextField
                  label="Variation Template ID"
                  value={template.variationOf}
                  onChange={(e) => handleFieldChange('variationOf', e.target.value)}
                  fullWidth
                  margin="normal"
                  helperText="ID of the template this is a variation of"
                />
              )}
            </Grid>
            
            <Grid item xs={12}>
              <Divider sx={{ my: 2 }} />
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">
                  Related Templates
                </Typography>
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
              </Box>
              
              <Typography variant="body2" color="textSecondary" paragraph>
                Select templates that are related to this template
              </Typography>
              
              {templatesLoading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
                  <CircularProgress />
                </Box>
              ) : (
                <List sx={{ width: '100%', bgcolor: 'background.paper' }}>
                  {templates.map((tmpl) => (
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
                          checked={template.relatedTemplates.includes(tmpl._id)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              handleFieldChange('relatedTemplates', [...template.relatedTemplates, tmpl._id]);
                            } else {
                              handleFieldChange('relatedTemplates', 
                                template.relatedTemplates.filter(id => id !== tmpl._id)
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
              )}
              
              <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
                <Pagination 
                  count={Math.ceil(templatesTotal / 10)} 
                  page={templatesPage} 
                  onChange={handleTemplatesPageChange} 
                  color="primary" 
                />
              </Box>
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
              {previewTemplate && (
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
                      const isSelected = template.relatedTemplates.includes(previewTemplate._id);
                      if (isSelected) {
                        handleFieldChange('relatedTemplates', 
                          template.relatedTemplates.filter(id => id !== previewTemplate._id)
                        );
                      } else {
                        handleFieldChange('relatedTemplates', 
                          [...template.relatedTemplates, previewTemplate._id]
                        );
                      }
                      handleClosePreviewDialog();
                    }}
                  >
                    {template.relatedTemplates.includes(previewTemplate._id) 
                      ? 'Remove from Related' 
                      : 'Add to Related'
                    }
                  </Button>
                </>
              )}
            </DialogActions>
          </Dialog>
        </TabPanel>
      </Paper>
    </Box>
  );
};

export default TemplateCreate; 