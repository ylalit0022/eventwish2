import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Tabs,
  Tab,
  IconButton,
  Divider
} from '@mui/material';
import {
  Save as SaveIcon,
  Delete as DeleteIcon,
  ArrowBack as ArrowBackIcon,
  Edit as EditIcon,
  Cancel as CancelIcon
} from '@mui/icons-material';
import { getContactById, updateContact, deleteContact } from '../api';
import { formatDate } from '../utils/dateUtils2';

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

const ContactDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  
  // State variables
  const [contact, setContact] = useState(null);
  const [editedContact, setEditedContact] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [tabValue, setTabValue] = useState(0);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success'
  });
  
  // Fetch contact on component mount
  useEffect(() => {
    fetchContact();
  }, [id]);
  
  // Fetch contact from API
  const fetchContact = async () => {
    setLoading(true);
    try {
      const data = await getContactById(id);
      console.log('API response data:', data);
      // Check if data.contact exists (API might be returning data in a nested object)
      const contactData = data.contact || data;
      console.log('Contact data to be set in state:', contactData);
      setContact(contactData);
      setEditedContact(contactData);
      setLoading(false);
    } catch (err) {
      console.error('Error fetching contact:', err);
      setSnackbar({
        open: true,
        message: `Error loading contact: ${err.message || 'Unknown error'}`,
        severity: 'error'
      });
      setLoading(false);
    }
  };
  
  // Handle tab change
  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };
  
  // Handle edit mode toggle
  const handleEditToggle = () => {
    if (editMode) {
      // Cancel editing, reset to original data
      setEditedContact(contact);
    }
    setEditMode(!editMode);
  };
  
  // Handle field change
  const handleFieldChange = (field, value) => {
    setEditedContact((prev) => ({ ...prev, [field]: value }));
  };
  
  // Handle save
  const handleSave = async () => {
    if (!editedContact) return;
    
    setSaving(true);
    try {
      const response = await updateContact(id, editedContact);
      setContact(response.contact);
      setEditedContact(response.contact);
      setEditMode(false);
      setSnackbar({
        open: true,
        message: 'Contact updated successfully',
        severity: 'success'
      });
    } catch (err) {
      console.error('Error updating contact:', err);
      setSnackbar({
        open: true,
        message: `Error updating contact: ${err.message || 'Unknown error'}`,
        severity: 'error'
      });
    } finally {
      setSaving(false);
    }
  };
  
  // Handle delete button click
  const handleDeleteClick = () => {
    setDeleteDialogOpen(true);
  };
  
  // Handle delete confirmation
  const handleDeleteConfirm = async () => {
    try {
      await deleteContact(id);
      setSnackbar({
        open: true,
        message: 'Contact deleted successfully',
        severity: 'success'
      });
      // Navigate back to contacts list after a short delay
      setTimeout(() => {
        navigate('/contacts');
      }, 1500);
    } catch (err) {
      console.error('Error deleting contact:', err);
      setSnackbar({
        open: true,
        message: `Error deleting contact: ${err.message || 'Unknown error'}`,
        severity: 'error'
      });
    } finally {
      setDeleteDialogOpen(false);
    }
  };
  
  // Handle delete dialog close
  const handleDeleteDialogClose = () => {
    setDeleteDialogOpen(false);
  };
  
  // Handle snackbar close
  const handleSnackbarClose = (event, reason) => {
    if (reason === 'clickaway') return;
    setSnackbar((prev) => ({ ...prev, open: false }));
  };
  
  if (loading) {
    return (
      <Container maxWidth="lg">
        <Box sx={{ mt: 4, display: 'flex', justifyContent: 'center' }}>
          <CircularProgress />
        </Box>
      </Container>
    );
  }
  
  if (!contact) {
    return (
      <Container maxWidth="lg">
        <Box sx={{ mt: 4 }}>
          <Typography variant="h5" color="error">
            Contact not found
          </Typography>
          <Button
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate('/contacts')}
            sx={{ mt: 2 }}
          >
            Back to Contacts
          </Button>
        </Box>
      </Container>
    );
  }
  
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
              {editMode ? 'Edit Contact' : 'Contact Details'}
            </Typography>
          </Box>
          <Box>
            <Button
              variant={editMode ? 'outlined' : 'contained'}
              color={editMode ? 'secondary' : 'primary'}
              startIcon={editMode ? <CancelIcon /> : <EditIcon />}
              onClick={handleEditToggle}
              sx={{ mr: 1 }}
            >
              {editMode ? 'Cancel' : 'Edit'}
            </Button>
            {editMode && (
              <Button
                variant="contained"
                color="primary"
                startIcon={<SaveIcon />}
                onClick={handleSave}
                disabled={saving}
              >
                {saving ? 'Saving...' : 'Save'}
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
                value={editedContact?.title || ''}
                onChange={(e) => handleFieldChange('title', e.target.value)}
                disabled={!editMode}
                required
              />
              
              <FormControlLabel
                control={
                  <Switch
                    checked={editedContact?.isActive || false}
                    onChange={(e) => handleFieldChange('isActive', e.target.checked)}
                    disabled={!editMode}
                    color="primary"
                  />
                }
                label="Active"
              />
              
              {!editMode && (
                <>
                  <Divider />
                  <Box sx={{ display: 'flex', gap: 2 }}>
                    <Box>
                      <Typography variant="subtitle2" color="textSecondary">
                        Created At
                      </Typography>
                      <Typography>{formatDate(contact.createdAt)}</Typography>
                    </Box>
                    <Box>
                      <Typography variant="subtitle2" color="textSecondary">
                        Updated At
                      </Typography>
                      <Typography>{formatDate(contact.updatedAt)}</Typography>
                    </Box>
                  </Box>
                </>
              )}
            </Box>
          </TabPanel>
          
          {/* Content Tab */}
          <TabPanel value={tabValue} index={1}>
            <Box>
              {console.log('Edit mode:', editMode)}
              {console.log('EditedContact in render:', editedContact)}
              {console.log('Contact in render:', contact)}
              {editMode ? (
                <TextField
                  label="HTML Content"
                  variant="outlined"
                  fullWidth
                  multiline
                  rows={15}
                  value={editedContact?.htmlCode || ''}
                  onChange={(e) => handleFieldChange('htmlCode', e.target.value)}
                  required
                />
              ) : (
                <Paper
                  variant="outlined"
                  sx={{ p: 2, height: '500px', overflow: 'auto' }}
                >
                  <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                    HTML Content
                  </Typography>
                  <pre
                    style={{
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                      fontFamily: 'monospace',
                      fontSize: '0.875rem'
                    }}
                  >
                    {contact.htmlCode}
                  </pre>
                </Paper>
              )}
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
              <Box
                sx={{ mt: 2 }}
                dangerouslySetInnerHTML={{
                  __html: editedContact?.htmlCode || ''
                }}
              />
            </Paper>
          </TabPanel>
        </Paper>
      </Box>
      
      {/* Delete confirmation dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteDialogClose}
        aria-labelledby="delete-dialog-title"
        aria-describedby="delete-dialog-description"
      >
        <DialogTitle id="delete-dialog-title">Delete Contact</DialogTitle>
        <DialogContent>
          <DialogContentText id="delete-dialog-description">
            Are you sure you want to delete this contact? This action cannot be undone.
            {contact.isActive && (
              <Typography color="error" sx={{ mt: 1 }}>
                Warning: This is currently the active contact. Deleting it may affect the application.
              </Typography>
            )}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteDialogClose} color="primary">
            Cancel
          </Button>
          <Button onClick={handleDeleteConfirm} color="error" autoFocus>
            Delete
          </Button>
        </DialogActions>
      </Dialog>
      
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
    </Container>
  );
};

export default ContactDetail; 