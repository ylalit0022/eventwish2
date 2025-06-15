import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
  ArrowBack as ArrowBackIcon
} from '@mui/icons-material';
import { createAbout } from '../api';

// Optional: Import React Quill for rich text editing
// If you don't have React Quill installed, you can install it with:
// npm install react-quill
// import ReactQuill from 'react-quill';
// import 'react-quill/dist/quill.snow.css';

const AboutCreate = () => {
  // State management
  const [newAbout, setNewAbout] = useState({
    title: '',
    htmlCode: '',
    isActive: true
  });
  const [saving, setSaving] = useState(false);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success'
  });
  
  const navigate = useNavigate();
  
  // Handle field change
  const handleFieldChange = (field, value) => {
    setNewAbout(prev => ({
      ...prev,
      [field]: value
    }));
  };
  
  // Validate form
  const validateForm = () => {
    if (!newAbout.title.trim()) {
      setSnackbar({
        open: true,
        message: 'Title is required',
        severity: 'error'
      });
      return false;
    }
    
    if (!newAbout.htmlCode.trim()) {
      setSnackbar({
        open: true,
        message: 'HTML Content is required',
        severity: 'error'
      });
      return false;
    }
    
    return true;
  };
  
  // Handle save
  const handleSave = async () => {
    if (!validateForm()) {
      return;
    }
    
    setSaving(true);
    try {
      const response = await createAbout(newAbout);
      setSnackbar({
        open: true,
        message: 'About entry created successfully',
        severity: 'success'
      });
      
      // Navigate to the detail page of the new about entry
      navigate(`/about/${response.about._id}`);
    } catch (error) {
      console.error('Error creating about entry:', error);
      setSnackbar({
        open: true,
        message: `Error creating about entry: ${error.message}`,
        severity: 'error'
      });
      setSaving(false);
    }
  };
  
  // Handle back button
  const handleBack = () => {
    if (newAbout.title.trim() || newAbout.htmlCode.trim()) {
      if (!window.confirm('Discard unsaved changes?')) {
        return;
      }
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
  
  return (
    <Container maxWidth="lg">
      <Box sx={{ my: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h4">
            Create New About Entry
          </Typography>
          <Box sx={{ display: 'flex', gap: 2 }}>
            <Button
              variant="outlined"
              startIcon={<ArrowBackIcon />}
              onClick={handleBack}
            >
              Back
            </Button>
            <Button
              variant="contained"
              color="primary"
              startIcon={<SaveIcon />}
              onClick={handleSave}
              disabled={saving}
            >
              {saving ? <CircularProgress size={24} /> : 'Save'}
            </Button>
          </Box>
        </Box>
        
        <Paper sx={{ p: 3 }}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <TextField
                label="Title"
                variant="outlined"
                fullWidth
                value={newAbout.title}
                onChange={(e) => handleFieldChange('title', e.target.value)}
                required
                margin="normal"
              />
            </Grid>
            
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={newAbout.isActive}
                    onChange={(e) => handleFieldChange('isActive', e.target.checked)}
                  />
                }
                label="Active"
              />
              <Typography variant="caption" color="textSecondary" display="block">
                Note: If active, this will make all other about entries inactive.
              </Typography>
            </Grid>
            
            <Grid item xs={12}>
              <Divider sx={{ my: 2 }} />
              <Typography variant="h6" gutterBottom>HTML Content</Typography>
              
              {/* If you're using React Quill, uncomment this: */}
              {/* <ReactQuill
                value={newAbout.htmlCode}
                onChange={(value) => handleFieldChange('htmlCode', value)}
                style={{ height: '300px', marginBottom: '50px' }}
              /> */}
              
              {/* If not using React Quill, use a regular TextField: */}
              <TextField
                label="HTML Content"
                variant="outlined"
                fullWidth
                multiline
                rows={10}
                value={newAbout.htmlCode}
                onChange={(e) => handleFieldChange('htmlCode', e.target.value)}
                required
                margin="normal"
              />
              
              {/* HTML Preview */}
              {newAbout.htmlCode && (
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
                    dangerouslySetInnerHTML={{ __html: newAbout.htmlCode }}
                  />
                </Box>
              )}
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

export default AboutCreate; 