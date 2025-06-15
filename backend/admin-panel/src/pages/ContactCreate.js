import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Container,
  Paper,
  Typography,
  TextField,
  Switch,
  FormControlLabel,
  CircularProgress,
  Snackbar,
  Alert,
  Tabs,
  Tab,
  IconButton
} from '@mui/material';
import {
  Save as SaveIcon,
  ArrowBack as ArrowBackIcon,
  Cancel as CancelIcon
} from '@mui/icons-material';
import { createContact } from '../api';

// Custom TabPanel component
function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`contact-tabpanel-${index}`}
      aria-labelledby={`contact-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const ContactCreate = () => {
  const navigate = useNavigate();
  
  // State variables
  const [contact, setContact] = useState({
    title: '',
    htmlCode: '',
    isActive: true
  });
  const [saving, setSaving] = useState(false);
  const [tabValue, setTabValue] = useState(0);
  const [errors, setErrors] = useState({});
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success'
  });
  
  // Handle tab change
  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };
  
  // Handle field change
  const handleFieldChange = (field, value) => {
    setContact((prev) => ({ ...prev, [field]: value }));
    // Clear error for this field if it exists
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: null }));
    }
  };
  
  // Validate form
  const validateForm = () => {
    const newErrors = {};
    
    if (!contact.title.trim()) {
      newErrors.title = 'Title is required';
    }
    
    if (!contact.htmlCode.trim()) {
      newErrors.htmlCode = 'HTML content is required';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };
  
  // Handle save
  const handleSave = async () => {
    if (!validateForm()) {
      setSnackbar({
        open: true,
        message: 'Please fix the errors before saving',
        severity: 'error'
      });
      return;
    }
    
    setSaving(true);
    try {
      const response = await createContact(contact);
      setSnackbar({
        open: true,
        message: 'Contact created successfully',
        severity: 'success'
      });
      
      // Navigate to the newly created contact after a short delay
      setTimeout(() => {
        navigate(`/contacts/${response.contact._id}`);
      }, 1500);
    } catch (err) {
      console.error('Error creating contact:', err);
      setSnackbar({
        open: true,
        message: `Error creating contact: ${err.message || 'Unknown error'}`,
        severity: 'error'
      });
      setSaving(false);
    }
  };
  
  // Handle cancel
  const handleCancel = () => {
    navigate('/contacts');
  };
  
  // Handle snackbar close
  const handleSnackbarClose = (event, reason) => {
    if (reason === 'clickaway') return;
    setSnackbar((prev) => ({ ...prev, open: false }));
  };
  
  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 4 }}>
        {/* Header */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <IconButton
              color="primary"
              aria-label="back"
              onClick={() => navigate('/contacts')}
              sx={{ mr: 1 }}
            >
              <ArrowBackIcon />
            </IconButton>
            <Typography variant="h4" component="h1">
              Create New Contact
            </Typography>
          </Box>
          <Box>
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
              startIcon={<SaveIcon />}
              onClick={handleSave}
              disabled={saving}
            >
              {saving ? 'Saving...' : 'Save'}
            </Button>
          </Box>
        </Box>
        
        {/* Tabs */}
        <Paper sx={{ width: '100%', mb: 2 }}>
          <Tabs
            value={tabValue}
            onChange={handleTabChange}
            indicatorColor="primary"
            textColor="primary"
            aria-label="contact tabs"
          >
            <Tab label="Basic Info" />
            <Tab label="Content" />
            <Tab label="Preview" />
          </Tabs>
          
          {/* Basic Info Tab */}
          <TabPanel value={tabValue} index={0}>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <TextField
                label="Title"
                fullWidth
                value={contact.title}
                onChange={(e) => handleFieldChange('title', e.target.value)}
                required
                error={!!errors.title}
                helperText={errors.title}
              />
              
              <FormControlLabel
                control={
                  <Switch
                    checked={contact.isActive}
                    onChange={(e) => handleFieldChange('isActive', e.target.checked)}
                    color="primary"
                  />
                }
                label="Active"
              />
              
              {contact.isActive && (
                <Typography variant="body2" color="primary">
                  Note: Activating this contact will automatically deactivate all other contacts.
                </Typography>
              )}
            </Box>
          </TabPanel>
          
          {/* Content Tab */}
          <TabPanel value={tabValue} index={1}>
            <Box>
              <TextField
                label="HTML Content"
                variant="outlined"
                fullWidth
                multiline
                rows={15}
                value={contact.htmlCode}
                onChange={(e) => handleFieldChange('htmlCode', e.target.value)}
                required
                error={!!errors.htmlCode}
                helperText={errors.htmlCode}
              />
            </Box>
          </TabPanel>
          
          {/* Preview Tab */}
          <TabPanel value={tabValue} index={2}>
            <Paper
              variant="outlined"
              sx={{ p: 2, minHeight: '500px', overflow: 'auto' }}
            >
              <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                Preview
              </Typography>
              {contact.htmlCode ? (
                <Box
                  sx={{ mt: 2 }}
                  dangerouslySetInnerHTML={{
                    __html: contact.htmlCode
                  }}
                />
              ) : (
                <Typography color="textSecondary" sx={{ mt: 2 }}>
                  No content to preview. Add HTML content in the Content tab.
                </Typography>
              )}
            </Paper>
          </TabPanel>
        </Paper>
      </Box>
      
      {/* Snackbar for notifications */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleSnackbarClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          onClose={handleSnackbarClose}
          severity={snackbar.severity}
          variant="filled"
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
      
      {/* Loading overlay */}
      {saving && (
        <Box
          sx={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            zIndex: 9999
          }}
        >
          <CircularProgress color="primary" />
        </Box>
      )}
    </Container>
  );
};

export default ContactCreate; 