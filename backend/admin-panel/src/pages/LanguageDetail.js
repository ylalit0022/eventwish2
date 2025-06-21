import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Grid,
  FormControlLabel,
  Switch,
  CircularProgress,
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
import { getLanguageByCode, updateLanguage } from '../api';
import { useSnackbar } from '../contexts/SnackbarContext';

const LanguageDetail = () => {
  const { code } = useParams();
  const navigate = useNavigate();
  const { showSnackbar } = useSnackbar();
  
  const [language, setLanguage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [formErrors, setFormErrors] = useState({});
  
  // Form state
  const [name, setName] = useState('');
  const [nativeName, setNativeName] = useState('');
  const [isRTL, setIsRTL] = useState(false);
  const [isActive, setIsActive] = useState(true);
  const [displayOrder, setDisplayOrder] = useState(0);
  
  useEffect(() => {
    const fetchLanguage = async () => {
      try {
        setLoading(true);
        const response = await getLanguageByCode(code);
        
        if (response.success && response.data) {
          setLanguage(response.data);
          
          // Initialize form values
          setName(response.data.name || '');
          setNativeName(response.data.nativeName || '');
          setIsRTL(response.data.isRTL || false);
          setIsActive(response.data.isActive !== undefined ? response.data.isActive : true);
          setDisplayOrder(response.data.displayOrder || 0);
        } else {
          showSnackbar('Failed to load language details', 'error');
          navigate('/languages');
        }
      } catch (error) {
        console.error('Error fetching language:', error);
        showSnackbar(`Error: ${error.message || 'Failed to load language details'}`, 'error');
        navigate('/languages');
      } finally {
        setLoading(false);
      }
    };
    
    fetchLanguage();
  }, [code, navigate, showSnackbar]);
  
  const validateForm = () => {
    const errors = {};
    
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
        name,
        nativeName,
        isRTL,
        isActive,
        displayOrder: Number(displayOrder)
      };
      
      const response = await updateLanguage(code, languageData);
      
      if (response.success) {
        showSnackbar('Language updated successfully', 'success');
        // Update local state with the updated data
        setLanguage(response.data);
      } else {
        showSnackbar(response.message || 'Failed to update language', 'error');
      }
    } catch (error) {
      console.error('Error updating language:', error);
      showSnackbar(`Error: ${error.message || 'Failed to update language'}`, 'error');
    } finally {
      setSaving(false);
    }
  };
  
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }
  
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
          <Typography color="text.primary">{language?.code || code}</Typography>
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
            Edit Language: {language?.code}
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
                value={language?.code || ''}
                fullWidth
                disabled
                margin="normal"
                helperText="Language code cannot be changed"
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
                  {saving ? 'Saving...' : 'Save Changes'}
                </Button>
              </Box>
            </Grid>
          </Grid>
        </form>
      </Paper>
    </Box>
  );
};

export default LanguageDetail;
