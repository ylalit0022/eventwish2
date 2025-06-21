import React, { useState, useEffect } from 'react';
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
  Breadcrumbs,
  FormHelperText,
  MenuItem,
  CircularProgress
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Save as SaveIcon
} from '@mui/icons-material';
import { Link } from 'react-router-dom';
import { createRegion, getLanguages } from '../api';
import { useSnackbar } from '../contexts/SnackbarContext';

const continents = [
  'Africa',
  'Asia',
  'Europe',
  'North America',
  'South America',
  'Oceania',
  'Antarctica',
  'Global'
];

const RegionCreate = () => {
  const navigate = useNavigate();
  const { showSnackbar } = useSnackbar();
  
  const [languages, setLanguages] = useState([]);
  const [loadingLanguages, setLoadingLanguages] = useState(true);
  const [saving, setSaving] = useState(false);
  const [formErrors, setFormErrors] = useState({});
  
  // Form state
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [continent, setContinent] = useState('');
  const [flagIcon, setFlagIcon] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [displayOrder, setDisplayOrder] = useState(0);
  const [localization, setLocalization] = useState({});
  
  useEffect(() => {
    const fetchLanguages = async () => {
      try {
        setLoadingLanguages(true);
        const response = await getLanguages();
        
        if (response.success) {
          setLanguages(response.data || []);
        } else {
          showSnackbar('Failed to load languages for localization', 'warning');
        }
      } catch (error) {
        console.error('Error fetching languages:', error);
        showSnackbar(`Error: ${error.message || 'Failed to load languages'}`, 'error');
      } finally {
        setLoadingLanguages(false);
      }
    };
    
    fetchLanguages();
  }, [showSnackbar]);
  
  const validateForm = () => {
    const errors = {};
    
    if (!code.trim()) {
      errors.code = 'Region code is required';
    } else if (!/^[a-z]{2,3}(-[A-Z]{2})?$/.test(code)) {
      errors.code = 'Invalid code format. Use ISO format like "us" or "gb"';
    }
    
    if (!name.trim()) {
      errors.name = 'Name is required';
    }
    
    if (!continent) {
      errors.continent = 'Continent is required';
    }
    
    if (displayOrder < 0) {
      errors.displayOrder = 'Display order cannot be negative';
    }
    
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };
  
  const handleLocalizationChange = (langCode, value) => {
    setLocalization(prev => ({
      ...prev,
      [langCode]: value
    }));
  };
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      showSnackbar('Please correct the errors in the form', 'error');
      return;
    }
    
    try {
      setSaving(true);
      
      const regionData = {
        code,
        name,
        continent,
        flagIcon,
        isActive,
        displayOrder: Number(displayOrder),
        localization
      };
      
      const response = await createRegion(regionData);
      
      if (response.success) {
        showSnackbar('Region created successfully', 'success');
        navigate('/regions');
      } else {
        showSnackbar(response.message || 'Failed to create region', 'error');
      }
    } catch (error) {
      console.error('Error creating region:', error);
      showSnackbar(`Error: ${error.message || 'Failed to create region'}`, 'error');
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
          <Link to="/regions" style={{ textDecoration: 'none', color: 'inherit' }}>
            Regions
          </Link>
          <Typography color="text.primary">Create New</Typography>
        </Breadcrumbs>
        
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate('/regions')}
          variant="outlined"
          color="primary"
        >
          Back to Regions
        </Button>
      </Box>
      
      <Paper sx={{ p: 3, mb: 3 }}>
        <Box sx={{ mb: 2 }}>
          <Typography variant="h5" component="h1" gutterBottom>
            Create New Region
          </Typography>
        </Box>
        
        <Divider sx={{ mb: 3 }} />
        
        <form onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                label="Region Code"
                value={code}
                onChange={(e) => setCode(e.target.value.toLowerCase())}
                fullWidth
                required
                margin="normal"
                error={!!formErrors.code}
                helperText={formErrors.code || "ISO code like 'us' for United States or 'gb' for United Kingdom"}
                placeholder="us"
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
                placeholder="United States"
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                select
                label="Continent"
                value={continent}
                onChange={(e) => setContinent(e.target.value)}
                fullWidth
                required
                margin="normal"
                error={!!formErrors.continent}
                helperText={formErrors.continent}
              >
                {continents.map((option) => (
                  <MenuItem key={option} value={option}>
                    {option}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                label="Flag Icon URL"
                value={flagIcon}
                onChange={(e) => setFlagIcon(e.target.value)}
                fullWidth
                margin="normal"
                helperText="URL to the flag icon image. Leave empty for global regions"
                placeholder="https://example.com/flags/us.png"
              />
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
                Inactive regions won't appear in region selection menus
              </FormHelperText>
            </Grid>
            
            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                Localized Names
              </Typography>
              <Divider sx={{ mb: 2 }} />
              
              {loadingLanguages ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
                  <CircularProgress size={24} />
                </Box>
              ) : (
                <Grid container spacing={2}>
                  {languages.filter(lang => lang.isActive).map((lang) => (
                    <Grid item xs={12} md={6} key={lang.code}>
                      <TextField
                        label={`Name in ${lang.name} (${lang.code})`}
                        value={localization[lang.code] || ''}
                        onChange={(e) => handleLocalizationChange(lang.code, e.target.value)}
                        fullWidth
                        margin="normal"
                        dir={lang.isRTL ? 'rtl' : 'ltr'}
                        InputProps={{
                          startAdornment: (
                            <Box component="span" sx={{ mr: 1, color: 'text.secondary' }}>
                              {lang.code}:
                            </Box>
                          ),
                        }}
                      />
                    </Grid>
                  ))}
                </Grid>
              )}
            </Grid>
            
            <Grid item xs={12}>
              <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
                <Button
                  variant="outlined"
                  color="secondary"
                  onClick={() => navigate('/regions')}
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
                  {saving ? 'Creating...' : 'Create Region'}
                </Button>
              </Box>
            </Grid>
          </Grid>
        </form>
      </Paper>
    </Box>
  );
};

export default RegionCreate;
