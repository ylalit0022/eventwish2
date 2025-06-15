import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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
  Tabs,
  Tab,
  Divider,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Snackbar
} from '@mui/material';
import {
  Save as SaveIcon,
  Edit as EditIcon,
  ArrowBack as ArrowBackIcon,
  Delete as DeleteIcon,
  Add as AddIcon,
  Remove as RemoveIcon
} from '@mui/icons-material';
import { getFestivalById, updateFestival, deleteFestival, getCategoryIcons, getTemplates } from '../api';
import { format } from 'date-fns';

// Tab Panel component
function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`festival-tabpanel-${index}`}
      aria-labelledby={`festival-tab-${index}`}
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

const UpcomingFestivalDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [festival, setFestival] = useState(null);
  const [editedFestival, setEditedFestival] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [editMode, setEditMode] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [tabValue, setTabValue] = useState(0);
  const [categoryIcons, setCategoryIcons] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success'
  });

  // Fetch festival data
  useEffect(() => {
    const fetchFestivalData = async () => {
      try {
        setLoading(true);
        setError(null);
        
        const response = await getFestivalById(id);
        
        if (response.success) {
          setFestival(response.festival);
          setEditedFestival(response.festival);
        } else {
          throw new Error(response.message || 'Failed to fetch festival');
        }
      } catch (err) {
        console.error(`Error fetching festival ${id}:`, err);
        setError('Failed to load festival: ' + (err.message || 'Unknown error'));
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      fetchFestivalData();
    }
  }, [id]);

  // Fetch category icons and templates for dropdowns
  useEffect(() => {
    const fetchReferenceData = async () => {
      try {
        // Fetch category icons
        const iconsResponse = await getCategoryIcons(1, 100);
        if (iconsResponse.success) {
          setCategoryIcons(iconsResponse.data);
        }
        
        // Fetch templates
        const templatesResponse = await getTemplates(1, 100);
        if (templatesResponse.success) {
          setTemplates(templatesResponse.data);
        }
      } catch (err) {
        console.error('Error fetching reference data:', err);
      }
    };
    
    fetchReferenceData();
  }, []);

  // Handle tab change
  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  // Handle edit mode toggle
  const handleEditToggle = () => {
    if (editMode) {
      // Discard changes
      setEditedFestival(festival);
    }
    setEditMode(!editMode);
  };

  // Handle field change
  const handleFieldChange = (field, value) => {
    setEditedFestival({
      ...editedFestival,
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
      return format(date, "yyyy-MM-dd'T'HH:mm");
    } catch (error) {
      return '';
    }
  };

  // Handle map field change (for localizedNames and themeColors)
  const handleMapFieldChange = (field, key, value) => {
    const updatedMap = { ...(editedFestival[field] || {}) };
    updatedMap[key] = value;
    
    setEditedFestival({
      ...editedFestival,
      [field]: updatedMap
    });
  };

  // Handle map field delete
  const handleMapFieldDelete = (field, key) => {
    const updatedMap = { ...(editedFestival[field] || {}) };
    delete updatedMap[key];
    
    setEditedFestival({
      ...editedFestival,
      [field]: updatedMap
    });
  };

  // Handle map field add
  const handleMapFieldAdd = (field) => {
    const updatedMap = { ...(editedFestival[field] || {}) };
    const newKey = field === 'localizedNames' ? 'en' : 'primary';
    const newValue = '';
    
    // Check if key already exists
    if (updatedMap[newKey]) {
      let i = 1;
      while (updatedMap[`${newKey}${i}`]) {
        i++;
      }
      updatedMap[`${newKey}${i}`] = newValue;
    } else {
      updatedMap[newKey] = newValue;
    }
    
    setEditedFestival({
      ...editedFestival,
      [field]: updatedMap
    });
  };

  // Handle template selection
  const handleTemplateChange = (event) => {
    const selectedTemplates = event.target.value;
    setEditedFestival({
      ...editedFestival,
      templates: selectedTemplates
    });
  };

  // Handle save
  const handleSave = async () => {
    try {
      setSaveLoading(true);
      setError(null);
      
      const response = await updateFestival(id, editedFestival);
      
      if (response.success) {
        setFestival(response.festival);
        setEditedFestival(response.festival);
        setEditMode(false);
        
        // Show success message
        setSnackbar({
          open: true,
          message: 'Festival updated successfully',
          severity: 'success'
        });
      } else {
        throw new Error(response.message || 'Failed to update festival');
      }
    } catch (err) {
      console.error(`Error updating festival ${id}:`, err);
      setError('Failed to update festival: ' + (err.message || 'Unknown error'));
      
      // Show error message
      setSnackbar({
        open: true,
        message: `Failed to update festival: ${err.message || 'Unknown error'}`,
        severity: 'error'
      });
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
      const response = await deleteFestival(id);
      
      if (response.success) {
        // Navigate back to festivals list
        navigate('/upcoming-festivals');
      } else {
        throw new Error(response.message || 'Failed to delete festival');
      }
    } catch (err) {
      console.error(`Error deleting festival ${id}:`, err);
      setError('Failed to delete festival: ' + (err.message || 'Unknown error'));
      setDeleteDialogOpen(false);
      
      // Show error message
      setSnackbar({
        open: true,
        message: `Failed to delete festival: ${err.message || 'Unknown error'}`,
        severity: 'error'
      });
    } finally {
      setDeleteLoading(false);
    }
  };

  // Handle snackbar close
  const handleSnackbarClose = (event, reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setSnackbar({ ...snackbar, open: false });
  };

  // Render map fields (localizedNames and themeColors)
  const renderMapFields = (field, label) => {
    const mapData = editedFestival[field] || {};
    const entries = Object.entries(mapData);
    
    return (
      <Box sx={{ mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography variant="subtitle1">{label}</Typography>
          {editMode && (
            <Button
              startIcon={<AddIcon />}
              onClick={() => handleMapFieldAdd(field)}
              size="small"
            >
              Add Entry
            </Button>
          )}
        </Box>
        
        {entries.length === 0 ? (
          <Typography variant="body2" color="text.secondary">No entries</Typography>
        ) : (
          <Grid container spacing={2}>
            {entries.map(([key, value]) => (
              <Grid item xs={12} sm={6} key={key}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <TextField
                    label={`Key (${key})`}
                    value={value}
                    onChange={(e) => handleMapFieldChange(field, key, e.target.value)}
                    disabled={!editMode}
                    fullWidth
                    size="small"
                    sx={{ mr: 1 }}
                  />
                  {editMode && (
                    <IconButton 
                      color="error" 
                      onClick={() => handleMapFieldDelete(field, key)}
                      size="small"
                    >
                      <RemoveIcon />
                    </IconButton>
                  )}
                </Box>
              </Grid>
            ))}
          </Grid>
        )}
      </Box>
    );
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error && !festival) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error}</Alert>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate('/upcoming-festivals')}
          sx={{ mt: 2 }}
        >
          Back to Festivals
        </Button>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          <IconButton onClick={() => navigate('/upcoming-festivals')} sx={{ mr: 1 }}>
            <ArrowBackIcon />
          </IconButton>
          <Typography variant="h4" component="h1">
            {editedFestival?.name || 'Festival Details'}
          </Typography>
        </Box>
        <Box>
          <Button
            variant={editMode ? 'outlined' : 'contained'}
            color={editMode ? 'secondary' : 'primary'}
            startIcon={editMode ? null : <EditIcon />}
            onClick={handleEditToggle}
            sx={{ mr: 1 }}
          >
            {editMode ? 'Cancel' : 'Edit'}
          </Button>
          {editMode && (
            <Button
              variant="contained"
              color="primary"
              startIcon={saveLoading ? <CircularProgress size={20} /> : <SaveIcon />}
              onClick={handleSave}
              disabled={saveLoading}
            >
              Save
            </Button>
          )}
          {!editMode && (
            <Button
              variant="outlined"
              color="error"
              startIcon={<DeleteIcon />}
              onClick={handleDeleteClick}
            >
              Delete
            </Button>
          )}
        </Box>
      </Box>

      {/* Error alert */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Tabs */}
      <Paper sx={{ mb: 3 }}>
        <Tabs value={tabValue} onChange={handleTabChange} aria-label="festival tabs">
          <Tab label="Basic Info" id="festival-tab-0" aria-controls="festival-tabpanel-0" />
          <Tab label="Dates" id="festival-tab-1" aria-controls="festival-tabpanel-1" />
          <Tab label="Display" id="festival-tab-2" aria-controls="festival-tabpanel-2" />
          <Tab label="Templates" id="festival-tab-3" aria-controls="festival-tabpanel-3" />
          <Tab label="Notifications" id="festival-tab-4" aria-controls="festival-tabpanel-4" />
          <Tab label="Localization" id="festival-tab-5" aria-controls="festival-tabpanel-5" />
        </Tabs>

        {/* Basic Info Tab */}
        <TabPanel value={tabValue} index={0}>
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Name"
                value={editedFestival?.name || ''}
                onChange={(e) => handleFieldChange('name', e.target.value)}
                disabled={!editMode}
                fullWidth
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Slug"
                value={editedFestival?.slug || ''}
                onChange={(e) => handleFieldChange('slug', e.target.value)}
                disabled={!editMode}
                fullWidth
                helperText="URL-friendly identifier (must be unique)"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Category"
                value={editedFestival?.category || ''}
                onChange={(e) => handleFieldChange('category', e.target.value)}
                disabled={!editMode}
                fullWidth
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth disabled={!editMode}>
                <InputLabel id="category-icon-label">Category Icon</InputLabel>
                <Select
                  labelId="category-icon-label"
                  value={editedFestival?.categoryIcon?._id || editedFestival?.categoryIcon || ''}
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
                value={editedFestival?.priority || 0}
                onChange={(e) => handleFieldChange('priority', parseInt(e.target.value) || 0)}
                disabled={!editMode}
                fullWidth
                InputProps={{ inputProps: { min: 0 } }}
                helperText="Higher priority festivals appear first"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth disabled={!editMode}>
                <InputLabel id="status-label">Status</InputLabel>
                <Select
                  labelId="status-label"
                  value={editedFestival?.status || 'UPCOMING'}
                  onChange={(e) => handleFieldChange('status', e.target.value)}
                  label="Status"
                >
                  <MenuItem value="UPCOMING">Upcoming</MenuItem>
                  <MenuItem value="ONGOING">Ongoing</MenuItem>
                  <MenuItem value="ENDED">Ended</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={editedFestival?.isActive || false}
                    onChange={(e) => handleFieldChange('isActive', e.target.checked)}
                    disabled={!editMode}
                    color="primary"
                  />
                }
                label="Active"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Description"
                value={editedFestival?.description || ''}
                onChange={(e) => handleFieldChange('description', e.target.value)}
                disabled={!editMode}
                fullWidth
                multiline
                rows={4}
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Dates Tab */}
        <TabPanel value={tabValue} index={1}>
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Date"
                type="datetime-local"
                value={formatDateForInput(editedFestival?.date)}
                onChange={(e) => handleDateChange('date', e.target.value)}
                disabled={!editMode}
                fullWidth
                required
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Start Date (for multi-day festivals)"
                type="datetime-local"
                value={formatDateForInput(editedFestival?.startDate)}
                onChange={(e) => handleDateChange('startDate', e.target.value)}
                disabled={!editMode}
                fullWidth
                InputLabelProps={{ shrink: true }}
                helperText="Optional, for multi-day festivals"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="End Date (for multi-day festivals)"
                type="datetime-local"
                value={formatDateForInput(editedFestival?.endDate)}
                onChange={(e) => handleDateChange('endDate', e.target.value)}
                disabled={!editMode}
                fullWidth
                InputLabelProps={{ shrink: true }}
                helperText="Optional, for multi-day festivals"
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Display Tab */}
        <TabPanel value={tabValue} index={2}>
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Image URL"
                value={editedFestival?.imageUrl || ''}
                onChange={(e) => handleFieldChange('imageUrl', e.target.value)}
                disabled={!editMode}
                fullWidth
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Banner URL"
                value={editedFestival?.bannerUrl || ''}
                onChange={(e) => handleFieldChange('bannerUrl', e.target.value)}
                disabled={!editMode}
                fullWidth
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Deep Link"
                value={editedFestival?.deepLink || ''}
                onChange={(e) => handleFieldChange('deepLink', e.target.value)}
                disabled={!editMode}
                fullWidth
                helperText="App deep link URL for this festival"
              />
            </Grid>
            <Grid item xs={12}>
              <Divider sx={{ my: 2 }} />
              <Typography variant="subtitle1" gutterBottom>Theme Colors</Typography>
              {renderMapFields('themeColors', 'Theme Colors')}
            </Grid>
          </Grid>
        </TabPanel>

        {/* Templates Tab */}
        <TabPanel value={tabValue} index={3}>
          <FormControl fullWidth disabled={!editMode}>
            <InputLabel id="templates-label">Templates</InputLabel>
            <Select
              labelId="templates-label"
              multiple
              value={editedFestival?.templates?.map(t => typeof t === 'object' ? t._id : t) || []}
              onChange={handleTemplateChange}
              label="Templates"
              renderValue={(selected) => (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {selected.map((value) => {
                    const template = templates.find(t => t._id === value);
                    return (
                      <Chip key={value} label={template ? template.title : value} />
                    );
                  })}
                </Box>
              )}
            >
              {templates.map((template) => (
                <MenuItem key={template._id} value={template._id}>
                  {template.title}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </TabPanel>

        {/* Notifications Tab */}
        <TabPanel value={tabValue} index={4}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={editedFestival?.pushEnabled || false}
                    onChange={(e) => handleFieldChange('pushEnabled', e.target.checked)}
                    disabled={!editMode}
                    color="primary"
                  />
                }
                label="Push Notifications Enabled"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Personalized Push Template"
                value={editedFestival?.personalizedPushTemplate || ''}
                onChange={(e) => handleFieldChange('personalizedPushTemplate', e.target.value)}
                disabled={!editMode}
                fullWidth
                multiline
                helperText="Use {{name}} for user name placeholder"
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={editedFestival?.notifyCountdown || false}
                    onChange={(e) => handleFieldChange('notifyCountdown', e.target.checked)}
                    disabled={!editMode}
                    color="primary"
                  />
                }
                label="Enable Countdown Notifications"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Countdown Days"
                type="number"
                value={editedFestival?.countdownDays || 3}
                onChange={(e) => handleFieldChange('countdownDays', parseInt(e.target.value) || 3)}
                disabled={!editMode || !editedFestival?.notifyCountdown}
                fullWidth
                InputProps={{ inputProps: { min: 1, max: 10 } }}
                helperText="How many days before to show countdown"
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Localization Tab */}
        <TabPanel value={tabValue} index={5}>
          {renderMapFields('localizedNames', 'Localized Names')}
        </TabPanel>
      </Paper>

      {/* Delete confirmation dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
      >
        <DialogTitle>Delete Festival</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete the festival "{festival?.name}"? This action cannot be undone.
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

export default UpcomingFestivalDetail;
 