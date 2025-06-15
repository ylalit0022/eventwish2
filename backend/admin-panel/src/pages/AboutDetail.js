import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Container,
  Typography,
  Paper,
  TextField,
  Button,
  Box,
  FormControlLabel,
  Switch,
  CircularProgress,
  Snackbar,
  Alert,
  Divider,
  Grid
} from '@mui/material';
import {
  Save as SaveIcon,
  ArrowBack as ArrowBackIcon,
  Edit as EditIcon,
  Delete as DeleteIcon
} from '@mui/icons-material';
import { getAboutById, updateAbout, deleteAbout } from '../api';
import { formatDate } from '../utils/dateUtils2';

// Optional: Import React Quill for rich text editing
// If you don't have React Quill installed, you can install it with:
// npm install react-quill
// import ReactQuill from 'react-quill';
// import 'react-quill/dist/quill.snow.css';

const AboutDetail = () => {
  // State management
  const [about, setAbout] = useState(null);
  const [editedAbout, setEditedAbout] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success'
  });
  
  const { id } = useParams();
  const navigate = useNavigate();
  
  // Fetch about on initial load
  useEffect(() => {
    fetchAbout();
  }, [id]);
  
  // Handle fetch about
  const fetchAbout = async () => {
    setLoading(true);
    try {
      const response = await getAboutById(id);
      setAbout(response.about);
      setEditedAbout(response.about);
    } catch (error) {
      console.error(`Error fetching about ${id}:`, error);
      setSnackbar({
        open: true,
        message: `Error fetching about: ${error.message}`,
        severity: 'error'
      });
    } finally {
      setLoading(false);
    }
  };
  
  // Handle field change
  const handleFieldChange = (field, value) => {
    setEditedAbout(prev => ({
      ...prev,
      [field]: value
    }));
  };
  
  // Handle save
  const handleSave = async () => {
    setSaving(true);
    try {
      await updateAbout(id, editedAbout);
      setSnackbar({
        open: true,
        message: 'About updated successfully',
        severity: 'success'
      });
      setAbout(editedAbout);
      setEditMode(false);
    } catch (error) {
      console.error(`Error updating about ${id}:`, error);
      setSnackbar({
        open: true,
        message: `Error updating about: ${error.message}`,
        severity: 'error'
      });
    } finally {
      setSaving(false);
    }
  };
  
  // Handle delete
  const handleDelete = async () => {
    if (window.confirm('Are you sure you want to delete this about entry?')) {
      try {
        await deleteAbout(id);
        setSnackbar({
          open: true,
          message: 'About deleted successfully',
          severity: 'success'
        });
        navigate('/about');
      } catch (error) {
        console.error(`Error deleting about ${id}:`, error);
        setSnackbar({
          open: true,
          message: `Error deleting about: ${error.message}`,
          severity: 'error'
        });
      }
    }
  };
  
  // Handle edit mode toggle
  const handleEditToggle = () => {
    if (editMode) {
      // Discard changes
      setEditedAbout(about);
    }
    setEditMode(!editMode);
  };
  
  // Handle back button
  const handleBack = () => {
    if (editMode && !window.confirm('Discard unsaved changes?')) {
      return;
    }
    navigate('/about');
  };
  
  // Handle snackbar close
  const handleSnackbarClose = (event, reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setSnackbar(prev => ({ ...prev, open: false }));
  };
  
  if (loading) {
    return (
      <Container maxWidth="lg" sx={{ mt: 4, display: 'flex', justifyContent: 'center' }}>
        <CircularProgress />
      </Container>
    );
  }
  
  if (!about) {
    return (
      <Container maxWidth="lg" sx={{ mt: 4 }}>
        <Typography variant="h6" color="error">About entry not found</Typography>
        <Button
          variant="contained"
          startIcon={<ArrowBackIcon />}
          onClick={handleBack}
          sx={{ mt: 2 }}
        >
          Back to List
        </Button>
      </Container>
    );
  }
  
  return (
    <Container maxWidth="lg">
      <Box sx={{ my: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h4">
            {editMode ? 'Edit About Entry' : 'About Entry Details'}
          </Typography>
          <Box sx={{ display: 'flex', gap: 2 }}>
            <Button
              variant="outlined"
              startIcon={<ArrowBackIcon />}
              onClick={handleBack}
            >
              Back
            </Button>
            {!editMode ? (
              <>
                <Button
                  variant="contained"
                  color="primary"
                  startIcon={<EditIcon />}
                  onClick={handleEditToggle}
                >
                  Edit
                </Button>
                <Button
                  variant="contained"
                  color="error"
                  startIcon={<DeleteIcon />}
                  onClick={handleDelete}
                >
                  Delete
                </Button>
              </>
            ) : (
              <>
                <Button
                  variant="contained"
                  color="primary"
                  startIcon={<SaveIcon />}
                  onClick={handleSave}
                  disabled={saving}
                >
                  {saving ? <CircularProgress size={24} /> : 'Save'}
                </Button>
                <Button
                  variant="outlined"
                  color="secondary"
                  onClick={handleEditToggle}
                >
                  Cancel
                </Button>
              </>
            )}
          </Box>
        </Box>
        
        <Paper sx={{ p: 3 }}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <TextField
                label="Title"
                variant="outlined"
                fullWidth
                value={editMode ? editedAbout.title : about.title}
                onChange={(e) => handleFieldChange('title', e.target.value)}
                InputProps={{
                  readOnly: !editMode,
                }}
                required
                margin="normal"
              />
            </Grid>
            
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={editMode ? editedAbout.isActive : about.isActive}
                    onChange={(e) => handleFieldChange('isActive', e.target.checked)}
                    disabled={!editMode}
                  />
                }
                label="Active"
              />
              {editMode && about.isActive && (
                <Typography variant="caption" color="textSecondary" display="block">
                  Note: Making this inactive will require another about entry to be active.
                </Typography>
              )}
            </Grid>
            
            <Grid item xs={12}>
              <Divider sx={{ my: 2 }} />
              <Typography variant="h6" gutterBottom>HTML Content</Typography>
              
              {/* If you're using React Quill, uncomment this: */}
              {/* {editMode ? (
                <ReactQuill
                  value={editedAbout.htmlCode}
                  onChange={(value) => handleFieldChange('htmlCode', value)}
                  style={{ height: '300px', marginBottom: '50px' }}
                />
              ) : (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                    HTML Preview:
                  </Typography>
                  <Box
                    sx={{
                      border: '1px solid #e0e0e0',
                      borderRadius: 1,
                      p: 2,
                      bgcolor: '#f9f9f9',
                      minHeight: '200px'
                    }}
                    dangerouslySetInnerHTML={{ __html: about.htmlCode }}
                  />
                </Box>
              )} */}
              
              {/* If not using React Quill, use a regular TextField: */}
              <TextField
                label="HTML Content"
                variant="outlined"
                fullWidth
                multiline
                rows={10}
                value={editMode ? editedAbout.htmlCode : about.htmlCode}
                onChange={(e) => handleFieldChange('htmlCode', e.target.value)}
                InputProps={{
                  readOnly: !editMode,
                }}
                required
                margin="normal"
              />
              
              {/* HTML Preview */}
              {!editMode && (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                    HTML Preview:
                  </Typography>
                  <Box
                    sx={{
                      border: '1px solid #e0e0e0',
                      borderRadius: 1,
                      p: 2,
                      bgcolor: '#f9f9f9',
                      minHeight: '200px'
                    }}
                    dangerouslySetInnerHTML={{ __html: about.htmlCode }}
                  />
                </Box>
              )}
            </Grid>
            
            <Grid item xs={12}>
              <Box sx={{ mt: 2 }}>
                <Typography variant="caption" color="textSecondary">
                  Created: {formatDate(about.createdAt)}
                  {about.updatedAt && ` | Last updated: ${formatDate(about.updatedAt)}`}
                </Typography>
              </Box>
            </Grid>
          </Grid>
        </Paper>
      </Box>
      
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleSnackbarClose}
      >
        <Alert onClose={handleSnackbarClose} severity={snackbar.severity}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default AboutDetail; 