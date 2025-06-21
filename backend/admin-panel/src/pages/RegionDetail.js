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
  Breadcrumbs,
  FormHelperText,
  MenuItem,
  Avatar
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Save as SaveIcon,
  Public as GlobalIcon
} from '@mui/icons-material';
import { Link } from 'react-router-dom';
import { getRegionByCode, updateRegion, getLanguages } from '../api';
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

const RegionDetail = () => {
  const { code } = useParams();
  const navigate = useNavigate();
  const { showSnackbar } = useSnackbar();
  
  const [region, setRegion] = useState(null);
  const [languages, setLanguages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [formErrors, setFormErrors] = useState({});
  
  // Form state
  const [name, setName] = useState('');
  const [continent, setContinent] = useState('');
  const [flagIcon, setFlagIcon] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [displayOrder, setDisplayOrder] = useState(0);
  const [localization, setLocalization] = useState({});
  
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        
        // Fetch languages for localization
        const languagesResponse = await getLanguages();
        if (languagesResponse.success) {
          setLanguages(languagesResponse.data || []);
        }
        
        // Fetch region details
        const regionResponse = await getRegionByCode(code);
        
        if (regionResponse.success && regionResponse.data) {
          const regionData = regionResponse.data;
          setRegion(regionData);
          
          // Initialize form values
          setName(regionData.name || '');
          setContinent(regionData.continent || '');
          setFlagIcon(regionData.flagIcon || '');
          setIsActive(regionData.isActive !== undefined ? regionData.isActive : true);
          setDisplayOrder(regionData.displayOrder || 0);
          setLocalization(regionData.localization || {});
        } else {
          showSnackbar('Failed to load region details', 'error');
          navigate('/regions');
        }
      } catch (error) {
        console.error('Error fetching data:', error);
        showSnackbar(`Error: ${error.message || 'Failed to load region details'}`, 'error');
        navigate('/regions');
      } finally {
        setLoading(false);
      }
    };
    
    fetchData();
  }, [code, navigate, showSnackbar]);
  
  const validateForm = () => {
    const errors = {};
    
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
        name,
        continent,
        flagIcon,
        isActive,
        displayOrder: Number(displayOrder),
        localization
      };
      
      const response = await updateRegion(code, regionData);
      
      if (response.success) {
        showSnackbar('Region updated successfully', 'success');
        // Update local state with the updated data
        setRegion(response.data);
      } else {
        showSnackbar(response.message || 'Failed to update region', 'error');
      }
    } catch (error) {
      console.error('Error updating region:', error);
      showSnackbar(`Error: ${error.message || 'Failed to update region'}`, 'error');
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
          <Link to="/regions" style={{ textDecoration: 'none', color: 'inherit' }}>
            Regions
          </Link>
          <Typography color="text.primary">{region?.code || code}</Typography>
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
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <Typography variant="h5" component="h1" gutterBottom sx={{ flexGrow: 1 }}>
            Edit Region: {region?.code}
          </Typography>
          
          {flagIcon && (
            <Avatar
              src={flagIcon}
              alt={name}
              sx={{ mr: 2 }}
              variant="rounded"
            >
              <GlobalIcon />
            </Avatar>
          )}
        </Box>
        
        <Divider sx={{ mb: 3 }} />
        
        <form onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                label="Region Code"
                value={region?.code || ''}
                fullWidth
                disabled
                margin="normal"
                helperText="Region code cannot be changed"
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

export default RegionDetail;
