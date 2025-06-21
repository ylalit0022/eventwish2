import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Grid,
  FormControlLabel,
  Switch,
  Divider,
  IconButton,
  Breadcrumbs,
  FormHelperText
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Save as SaveIcon,
  FormatTextdirectionRToL as RTLIcon,
  FormatTextdirectionLToR as LTRIcon
} from '@mui/icons-material';
import { Link } from 'react-router-dom';
import { createLanguage } from '../api';
import { useSnackbar } from '../contexts/SnackbarContext';

const LanguageCreate = () => {
  const navigate = useNavigate();
  const { showSnackbar } = useSnackbar();
  
  const [saving, setSaving] = useState(false);
  const [formErrors, setFormErrors] = useState({});
  
  // Form state
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [nativeName, setNativeName] = useState('');
  const [isRTL, setIsRTL] = useState(false);
  const [isActive, setIsActive] = useState(true);
  const [displayOrder, setDisplayOrder] = useState(0);
  
  const validateForm = () => {
    const errors = {};
    
    if (!code.trim()) {
      errors.code = 'Language code is required';
    } else if (!/^[a-z]{2,3}(-[A-Z]{2})?$/.test(code)) {
      errors.code = 'Invalid code format. Use ISO format like "en" or "en-US"';
    }
    
    if (!name.trim()) {
      errors.name = 'Name is required';
    }
    
    if (!nativeName.trim()) {
      errors.nativeName = 'Native name is required';
    }
    
    if (displayOrder < 0) {
      errors.displayOrder = 'Display order cannot be negative';
    }
    
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      showSnackbar('Please correct the errors in the form', 'error');
      return;
    }
    
    try {
      setSaving(true);
      
      const languageData = {
        code,
        name,
        nativeName,
        isRTL,
        isActive,
        displayOrder: Number(displayOrder)
      };
      
      const response = await createLanguage(languageData);
      
      if (response.success) {
        showSnackbar('Language created successfully', 'success');
        navigate('/languages');
      } else {
        showSnackbar(response.message || 'Failed to create language', 'error');
      }
    } catch (error) {
      console.error('Error creating language:', error);
      showSnackbar(`Error: ${error.message || 'Failed to create language'}`, 'error');
    } finally {
      setSaving(false);
    }
  };
  
  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Breadcrumbs aria-label="breadcrumb">
          <Link to="/dashboard" style={{ textDecoration: 'none', color: 'inherit' }}>
            Dashboard
          </Link>
          <Link to="/languages" style={{ textDecoration: 'none', color: 'inherit' }}>
            Languages
          </Link>
          <Typography color="text.primary">Create New</Typography>
        </Breadcrumbs>
        
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate('/languages')}
          variant="outlined"
          color="primary"
        >
          Back to Languages
        </Button>
      </Box>
      
      <Paper sx={{ p: 3, mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <Typography variant="h5" component="h1" gutterBottom sx={{ flexGrow: 1 }}>
            Create New Language
          </Typography>
          
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <IconButton
              color={isRTL ? "primary" : "default"}
              onClick={() => setIsRTL(true)}
              aria-label="right to left text direction"
            >
              <RTLIcon />
            </IconButton>
            <IconButton
              color={!isRTL ? "primary" : "default"}
              onClick={() => setIsRTL(false)}
              aria-label="left to right text direction"
            >
              <LTRIcon />
            </IconButton>
          </Box>
        </Box>
        
        <Divider sx={{ mb: 3 }} />
        
        <form onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                label="Language Code"
                value={code}
                onChange={(e) => setCode(e.target.value.toLowerCase())}
                fullWidth
                required
                margin="normal"
                error={!!formErrors.code}
                helperText={formErrors.code || "ISO code like 'en' for English or 'fr' for French"}
                placeholder="en"
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                label="Display Order"
                type="number"
                value={displayOrder}
                onChange={(e) => setDisplayOrder(e.target.value)}
                fullWidth
                margin="normal"
                error={!!formErrors.displayOrder}
                helperText={formErrors.displayOrder || "Lower values appear first in lists"}
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                label="Name (English)"
                value={name}
                onChange={(e) => setName(e.target.value)}
                fullWidth
                required
                margin="normal"
                error={!!formErrors.name}
                helperText={formErrors.name}
                placeholder="English"
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                label="Native Name"
                value={nativeName}
                onChange={(e) => setNativeName(e.target.value)}
                fullWidth
                required
                margin="normal"
                error={!!formErrors.nativeName}
                helperText={formErrors.nativeName}
                dir={isRTL ? 'rtl' : 'ltr'}
                placeholder="English"
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={isRTL}
                    onChange={(e) => setIsRTL(e.target.checked)}
                    color="primary"
                  />
                }
                label="Right-to-Left (RTL) Language"
              />
              <FormHelperText>
                Enable for languages that read from right to left (e.g., Arabic, Hebrew)
              </FormHelperText>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={isActive}
                    onChange={(e) => setIsActive(e.target.checked)}
                    color="primary"
                  />
                }
                label={isActive ? "Active" : "Inactive"}
              />
              <FormHelperText>
                Inactive languages won't appear in language selection menus
              </FormHelperText>
            </Grid>
            
            <Grid item xs={12}>
              <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
                <Button
                  variant="outlined"
                  color="secondary"
                  onClick={() => navigate('/languages')}
                  sx={{ mr: 2 }}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="contained"
                  color="primary"
                  startIcon={<SaveIcon />}
                  disabled={saving}
                >
                  {saving ? 'Creating...' : 'Create Language'}
                </Button>
              </Box>
            </Grid>
          </Grid>
        </form>
      </Paper>
    </Box>
  );
};

export default LanguageCreate;
