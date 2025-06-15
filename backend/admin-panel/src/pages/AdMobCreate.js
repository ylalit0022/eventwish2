import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  Paper,
  Grid,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  Alert,
  CircularProgress,
  IconButton
} from '@mui/material';
import {
  Save as SaveIcon,
  ArrowBack as BackIcon
} from '@mui/icons-material';
import { createAdMob } from '../api';

const AdMobCreate = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    adName: '',
    adUnitCode: '',
    adType: '',
    status: true,
    targetingPriority: 1,
    displaySettings: {
      maxImpressionsPerDay: 10,
      minIntervalBetweenAds: 60,
      cooldownPeriod: 15
    },
    parameters: {},
    targetingCriteria: {}
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [adTypes] = useState(['Banner', 'Interstitial', 'Rewarded', 'Native', 'App Open', 'Video']);

  // Handle form field change
  const handleFieldChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
    
    // Clear field error when user makes changes
    if (fieldErrors[field]) {
      setFieldErrors(prev => ({
        ...prev,
        [field]: null
      }));
    }
  };

  // Handle nested field change (for displaySettings)
  const handleNestedFieldChange = (parent, field, value) => {
    setFormData(prev => ({
      ...prev,
      [parent]: {
        ...prev[parent],
        [field]: value
      }
    }));
  };
  
  // Validate ad unit code format
  const validateAdUnitCode = (code) => {
    const regex = /^ca-app-pub-\d{16}\/\d{10}$/;
    return regex.test(code);
  };
  
  // Validate form
  const validateForm = () => {
    const errors = {};
    
    if (!formData.adName) {
      errors.adName = 'Ad name is required';
    }
    
    if (!formData.adUnitCode) {
      errors.adUnitCode = 'Ad unit code is required';
    } else if (!validateAdUnitCode(formData.adUnitCode)) {
      errors.adUnitCode = 'Invalid format. Should be: ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX';
    }
    
    if (!formData.adType) {
      errors.adType = 'Ad type is required';
    }
    
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Handle save
  const handleSave = async () => {
    try {
      // Reset error state
      setError(null);
      
      // Validate form
      if (!validateForm()) {
        return;
      }
      
      setSaving(true);
      
      const response = await createAdMob(formData);
      
      if (response.success) {
        // Check if response.data exists and has _id or id property
        if (response.data) {
          const adId = response.data._id || response.data.id;
          if (adId) {
            console.log('Successfully created AdMob ad with ID:', adId);
            navigate(`/admob/${adId}`);
            return;
          }
        }
        
        // If we can't get the ID, just go back to the list
        console.log('Created AdMob ad but could not get ID, redirecting to list');
        navigate('/admob');
      } else {
        throw new Error(response.message || 'Failed to create AdMob ad');
      }
    } catch (err) {
      console.error('Error creating AdMob ad:', err);
      
      // Check for specific error messages
      if (err.response && err.response.data && err.response.data.error === 'Ad unit code already exists') {
        setFieldErrors(prev => ({
          ...prev,
          adUnitCode: 'This Ad Unit Code already exists. Please use a different code.'
        }));
        setError('This Ad Unit Code already exists. Please use a different code.');
      } else {
        setError('Failed to create AdMob ad: ' + (err.message || 'Unknown error'));
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <Box sx={{ flexGrow: 1, p: 3 }}>
      {/* Header */}
      <Grid container spacing={2} alignItems="center" sx={{ mb: 3 }}>
        <Grid item>
          <IconButton onClick={() => navigate('/admob')} color="primary">
            <BackIcon />
          </IconButton>
        </Grid>
        <Grid item xs>
          <Typography variant="h4" component="h1">
            Create AdMob Ad
          </Typography>
        </Grid>
        <Grid item>
          <Button
            variant="contained"
            color="primary"
            startIcon={<SaveIcon />}
            onClick={handleSave}
            disabled={saving}
          >
            {saving ? <CircularProgress size={24} /> : 'Save'}
          </Button>
        </Grid>
      </Grid>

      {/* Error message */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Form */}
      <Paper sx={{ p: 3 }}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Ad Name"
              value={formData.adName}
              onChange={(e) => handleFieldChange('adName', e.target.value)}
              required
              error={!!fieldErrors.adName}
              helperText={fieldErrors.adName || ''}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <FormControl fullWidth required error={!!fieldErrors.adType}>
              <InputLabel>Ad Type</InputLabel>
              <Select
                value={formData.adType}
                label="Ad Type"
                onChange={(e) => handleFieldChange('adType', e.target.value)}
              >
                {adTypes.map((type) => (
                  <MenuItem key={type} value={type}>
                    {type}
                  </MenuItem>
                ))}
              </Select>
              {fieldErrors.adType && (
                <Typography variant="caption" color="error">
                  {fieldErrors.adType}
                </Typography>
              )}
            </FormControl>
          </Grid>
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Ad Unit Code"
              value={formData.adUnitCode}
              onChange={(e) => handleFieldChange('adUnitCode', e.target.value)}
              required
              error={!!fieldErrors.adUnitCode}
              helperText={
                fieldErrors.adUnitCode || 
                'Format: ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX'
              }
            />
          </Grid>
          <Grid item xs={12}>
            <FormControlLabel
              control={
                <Switch
                  checked={formData.status}
                  onChange={(e) => handleFieldChange('status', e.target.checked)}
                  color="primary"
                />
              }
              label="Active"
            />
          </Grid>
          <Grid item xs={12}>
            <Typography variant="h6" gutterBottom>
              Display Settings
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={4}>
                <TextField
                  fullWidth
                  type="number"
                  label="Max Impressions Per Day"
                  value={formData.displaySettings.maxImpressionsPerDay}
                  onChange={(e) => handleNestedFieldChange(
                    'displaySettings', 
                    'maxImpressionsPerDay', 
                    parseInt(e.target.value) || 1
                  )}
                  InputProps={{ inputProps: { min: 1 } }}
                />
              </Grid>
              <Grid item xs={12} md={4}>
                <TextField
                  fullWidth
                  type="number"
                  label="Min Interval Between Ads (seconds)"
                  value={formData.displaySettings.minIntervalBetweenAds}
                  onChange={(e) => handleNestedFieldChange(
                    'displaySettings', 
                    'minIntervalBetweenAds', 
                    parseInt(e.target.value) || 30
                  )}
                  InputProps={{ inputProps: { min: 30 } }}
                />
              </Grid>
              <Grid item xs={12} md={4}>
                <TextField
                  fullWidth
                  type="number"
                  label="Cooldown Period (days)"
                  value={formData.displaySettings.cooldownPeriod}
                  onChange={(e) => handleNestedFieldChange(
                    'displaySettings', 
                    'cooldownPeriod', 
                    parseInt(e.target.value) || 1
                  )}
                  InputProps={{ inputProps: { min: 1, max: 30 } }}
                />
              </Grid>
            </Grid>
          </Grid>
          <Grid item xs={12}>
            <Typography variant="h6" gutterBottom>
              Targeting
            </Typography>
            <TextField
              fullWidth
              type="number"
              label="Targeting Priority"
              value={formData.targetingPriority}
              onChange={(e) => handleFieldChange('targetingPriority', parseInt(e.target.value) || 1)}
              InputProps={{ inputProps: { min: 1, max: 10 } }}
              helperText="Higher priority ads are shown first (1-10)"
              sx={{ mb: 2 }}
            />
          </Grid>
        </Grid>
      </Paper>
    </Box>
  );
};

export default AdMobCreate; 