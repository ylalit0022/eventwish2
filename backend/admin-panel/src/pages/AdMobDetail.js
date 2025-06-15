import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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
  Divider,
  IconButton,
  Tabs,
  Tab,
  Chip,
  Card,
  CardContent,
  Tooltip,
  List,
  ListItem,
  ListItemText
} from '@mui/material';
import {
  Save as SaveIcon,
  ArrowBack as BackIcon,
  Delete as DeleteIcon,
  Add as AddIcon,
  Remove as RemoveIcon
} from '@mui/icons-material';
import { getAdMobById, updateAdMob, deleteAdMob } from '../api';

// Tab panel component
function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`admob-tabpanel-${index}`}
      aria-labelledby={`admob-tab-${index}`}
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

const AdMobDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [adMob, setAdMob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [editMode, setEditMode] = useState(false);
  const [tabValue, setTabValue] = useState(0);
  const [adTypes, setAdTypes] = useState(['Banner', 'Interstitial', 'Rewarded', 'Native', 'App Open', 'Video']);
  
  // Form state
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

  // Fetch AdMob ad
  useEffect(() => {
    const fetchAdMob = async () => {
      try {
        console.log('AdMobDetail: useEffect triggered with ID:', id, typeof id);
        
        setLoading(true);
        setError(null);
        
        if (!id || id === 'undefined') {
          console.error('AdMobDetail: ID parameter is undefined or empty');
          setError('Invalid AdMob ID. Please go back to the AdMob list and try again.');
          setLoading(false);
          return;
        }
        
        console.log('AdMobDetail: Calling getAdMobById with ID:', id);
        const response = await getAdMobById(id);
        console.log('AdMobDetail: Response from getAdMobById:', response);
        
        if (response.success) {
          setAdMob(response.data);
          setFormData({
            adName: response.data.adName || '',
            adUnitCode: response.data.adUnitCode || '',
            adType: response.data.adType || '',
            status: response.data.status !== undefined ? response.data.status : true,
            targetingPriority: response.data.targetingPriority || 1,
            displaySettings: {
              maxImpressionsPerDay: response.data.displaySettings?.maxImpressionsPerDay || 10,
              minIntervalBetweenAds: response.data.displaySettings?.minIntervalBetweenAds || 60,
              cooldownPeriod: response.data.displaySettings?.cooldownPeriod || 15
            },
            parameters: response.data.parameters || {},
            targetingCriteria: response.data.targetingCriteria || {}
          });
          setAdTypes(response.adTypes || ['Banner', 'Interstitial', 'Rewarded', 'Native', 'App Open', 'Video']);
        } else {
          console.error('AdMobDetail: API returned error:', response.message || 'Failed to fetch AdMob ad');
          throw new Error(response.message || 'Failed to fetch AdMob ad');
        }
      } catch (err) {
        console.error(`AdMobDetail: Error fetching AdMob ad ${id}:`, err);
        setError('Failed to load AdMob ad: ' + (err.message || 'Unknown error'));
      } finally {
        setLoading(false);
      }
    };

    fetchAdMob();
  }, [id]);

  // Handle tab change
  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  // Handle form field change
  const handleFieldChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
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

  // Handle map field change (for parameters and targetingCriteria)
  const handleMapFieldChange = (mapName, key, value) => {
    setFormData(prev => ({
      ...prev,
      [mapName]: {
        ...prev[mapName],
        [key]: value
      }
    }));
  };

  // Handle map field remove
  const handleMapFieldRemove = (mapName, key) => {
    setFormData(prev => {
      const newMap = { ...prev[mapName] };
      delete newMap[key];
      return {
        ...prev,
        [mapName]: newMap
      };
    });
  };

  // Handle map field add
  const handleMapFieldAdd = (mapName) => {
    const newKey = `key${Object.keys(formData[mapName]).length + 1}`;
    handleMapFieldChange(mapName, newKey, '');
  };

  // Handle save
  const handleSave = async () => {
    try {
      setSaving(true);
      setError(null);
      
      // Validate form data
      if (!formData.adName) {
        throw new Error('Ad name is required');
      }
      if (!formData.adUnitCode) {
        throw new Error('Ad unit code is required');
      }
      if (!formData.adType) {
        throw new Error('Ad type is required');
      }
      
      // Check if ID is valid
      if (!id) {
        throw new Error('Invalid AdMob ID');
      }
      
      // Format data for API
      const adMobData = {
        ...formData
      };
      
      // Update or create
      const response = await updateAdMob(id, adMobData);
      
      if (response.success) {
        setAdMob(response.data);
        setEditMode(false);
      } else {
        throw new Error(response.message || 'Failed to save AdMob ad');
      }
    } catch (err) {
      console.error('Error saving AdMob ad:', err);
      setError('Failed to save AdMob ad: ' + (err.message || 'Unknown error'));
    } finally {
      setSaving(false);
    }
  };

  // Handle delete
  const handleDelete = async () => {
    if (!window.confirm('Are you sure you want to delete this AdMob ad? This action cannot be undone.')) {
      return;
    }
    
    try {
      setSaving(true);
      setError(null);
      
      // Check if ID is valid
      if (!id) {
        throw new Error('Invalid AdMob ID');
      }
      
      const response = await deleteAdMob(id);
      
      if (response.success) {
        navigate('/admob');
      } else {
        throw new Error(response.message || 'Failed to delete AdMob ad');
      }
    } catch (err) {
      console.error('Error deleting AdMob ad:', err);
      setError('Failed to delete AdMob ad: ' + (err.message || 'Unknown error'));
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error && !adMob) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error}</Alert>
        <Button
          startIcon={<BackIcon />}
          onClick={() => navigate('/admob')}
          sx={{ mt: 2 }}
        >
          Back to AdMob Ads
        </Button>
      </Box>
    );
  }

  if (!adMob) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="warning">AdMob ad not found</Alert>
        <Button
          startIcon={<BackIcon />}
          onClick={() => navigate('/admob')}
          sx={{ mt: 2 }}
        >
          Back to AdMob Ads
        </Button>
      </Box>
    );
  }

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
            {id ? (editMode ? 'Edit AdMob Ad' : 'AdMob Ad Details') : 'Create AdMob Ad'}
          </Typography>
        </Grid>
        <Grid item>
          {!editMode && id && (
            <Button
              variant="outlined"
              color="primary"
              onClick={() => setEditMode(true)}
              sx={{ mr: 1 }}
            >
              Edit
            </Button>
          )}
          {editMode && (
            <Button
              variant="contained"
              color="primary"
              startIcon={<SaveIcon />}
              onClick={handleSave}
              disabled={saving}
              sx={{ mr: 1 }}
            >
              Save
            </Button>
          )}
          {id && (
            <Button
              variant="outlined"
              color="error"
              startIcon={<DeleteIcon />}
              onClick={handleDelete}
              disabled={saving}
            >
              Delete
            </Button>
          )}
        </Grid>
      </Grid>

      {/* Error message */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* AdMob ad details */}
      <Paper sx={{ mb: 3 }}>
        <Tabs
          value={tabValue}
          onChange={handleTabChange}
          indicatorColor="primary"
          textColor="primary"
          variant="scrollable"
          scrollButtons="auto"
        >
          <Tab label="Basic Info" />
          <Tab label="Display Settings" />
          <Tab label="Targeting" />
          <Tab label="Parameters" />
          {!editMode && <Tab label="Analytics" />}
        </Tabs>

        {/* Basic Info Tab */}
        <TabPanel value={tabValue} index={0}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Ad Name"
                value={formData.adName}
                onChange={(e) => handleFieldChange('adName', e.target.value)}
                disabled={!editMode}
                required
                error={editMode && !formData.adName}
                helperText={editMode && !formData.adName ? 'Ad name is required' : ''}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth required error={editMode && !formData.adType}>
                <InputLabel>Ad Type</InputLabel>
                <Select
                  value={formData.adType}
                  label="Ad Type"
                  onChange={(e) => handleFieldChange('adType', e.target.value)}
                  disabled={!editMode}
                >
                  {adTypes.map((type) => (
                    <MenuItem key={type} value={type}>
                      {type}
                    </MenuItem>
                  ))}
                </Select>
                {editMode && !formData.adType && (
                  <Typography variant="caption" color="error">
                    Ad type is required
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
                disabled={!editMode}
                required
                error={editMode && !formData.adUnitCode}
                helperText={
                  editMode && !formData.adUnitCode 
                    ? 'Ad unit code is required' 
                    : 'Format: ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX'
                }
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.status}
                    onChange={(e) => handleFieldChange('status', e.target.checked)}
                    disabled={!editMode}
                    color="primary"
                  />
                }
                label="Active"
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Display Settings Tab */}
        <TabPanel value={tabValue} index={1}>
          <Grid container spacing={3}>
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
                disabled={!editMode}
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
                disabled={!editMode}
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
                disabled={!editMode}
                InputProps={{ inputProps: { min: 1, max: 30 } }}
              />
            </Grid>
            <Grid item xs={12}>
              <Typography variant="body2" color="text.secondary">
                Display settings control how often ads are shown to users. 
                Max impressions per day limits the number of times an ad is shown to a user in a single day.
                Min interval ensures a minimum time between ad displays.
                Cooldown period prevents the ad from being shown to a user for the specified number of days after they've interacted with it.
              </Typography>
            </Grid>
          </Grid>
        </TabPanel>

        {/* Targeting Tab */}
        <TabPanel value={tabValue} index={2}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                type="number"
                label="Targeting Priority"
                value={formData.targetingPriority}
                onChange={(e) => handleFieldChange('targetingPriority', parseInt(e.target.value) || 1)}
                disabled={!editMode}
                InputProps={{ inputProps: { min: 1, max: 10 } }}
                helperText="Higher priority ads are shown first (1-10)"
              />
            </Grid>
            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom>
                Targeting Criteria
              </Typography>
              <Divider sx={{ mb: 2 }} />
              
              {Object.entries(formData.targetingCriteria).map(([key, value]) => (
                <Grid container spacing={2} key={key} sx={{ mb: 2 }}>
                  <Grid item xs={5}>
                    <TextField
                      fullWidth
                      label="Criterion"
                      value={key}
                      disabled
                    />
                  </Grid>
                  <Grid item xs={5}>
                    <TextField
                      fullWidth
                      label="Value"
                      value={value}
                      onChange={(e) => handleMapFieldChange('targetingCriteria', key, e.target.value)}
                      disabled={!editMode}
                    />
                  </Grid>
                  <Grid item xs={2}>
                    {editMode && (
                      <IconButton 
                        color="error" 
                        onClick={() => handleMapFieldRemove('targetingCriteria', key)}
                      >
                        <RemoveIcon />
                      </IconButton>
                    )}
                  </Grid>
                </Grid>
              ))}
              
              {editMode && (
                <Button
                  variant="outlined"
                  startIcon={<AddIcon />}
                  onClick={() => handleMapFieldAdd('targetingCriteria')}
                  sx={{ mt: 1 }}
                >
                  Add Targeting Criterion
                </Button>
              )}
            </Grid>
          </Grid>
        </TabPanel>

        {/* Parameters Tab */}
        <TabPanel value={tabValue} index={3}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom>
                Ad Parameters
              </Typography>
              <Divider sx={{ mb: 2 }} />
              
              {Object.entries(formData.parameters).map(([key, value]) => (
                <Grid container spacing={2} key={key} sx={{ mb: 2 }}>
                  <Grid item xs={5}>
                    <TextField
                      fullWidth
                      label="Parameter"
                      value={key}
                      disabled
                    />
                  </Grid>
                  <Grid item xs={5}>
                    <TextField
                      fullWidth
                      label="Value"
                      value={value}
                      onChange={(e) => handleMapFieldChange('parameters', key, e.target.value)}
                      disabled={!editMode}
                    />
                  </Grid>
                  <Grid item xs={2}>
                    {editMode && (
                      <IconButton 
                        color="error" 
                        onClick={() => handleMapFieldRemove('parameters', key)}
                      >
                        <RemoveIcon />
                      </IconButton>
                    )}
                  </Grid>
                </Grid>
              ))}
              
              {editMode && (
                <Button
                  variant="outlined"
                  startIcon={<AddIcon />}
                  onClick={() => handleMapFieldAdd('parameters')}
                  sx={{ mt: 1 }}
                >
                  Add Parameter
                </Button>
              )}
            </Grid>
          </Grid>
        </TabPanel>

        {/* Analytics Tab - Only shown in view mode */}
        {!editMode && (
          <TabPanel value={tabValue} index={4}>
            <Grid container spacing={3}>
              <Grid item xs={12} md={4}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Impressions
                    </Typography>
                    <Typography variant="h3">
                      {adMob?.impressions || 0}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} md={4}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Clicks
                    </Typography>
                    <Typography variant="h3">
                      {adMob?.clicks || 0}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} md={4}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      CTR
                    </Typography>
                    <Typography variant="h3">
                      {adMob?.ctr ? `${adMob.ctr.toFixed(2)}%` : '0.00%'}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} md={6}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Revenue
                    </Typography>
                    <Typography variant="h3">
                      ${adMob?.revenue ? adMob.revenue.toFixed(2) : '0.00'}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} md={6}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Recent Activity
                    </Typography>
                    <List dense>
                      {adMob?.impressionData && adMob.impressionData.length > 0 ? (
                        adMob.impressionData.slice(0, 5).map((impression, index) => (
                          <ListItem key={index}>
                            <ListItemText 
                              primary={`Impression at ${new Date(impression.timestamp).toLocaleString()}`}
                              secondary={`Context: ${JSON.stringify(impression.context)}`}
                            />
                          </ListItem>
                        ))
                      ) : (
                        <ListItem>
                          <ListItemText primary="No recent activity" />
                        </ListItem>
                      )}
                    </List>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>
          </TabPanel>
        )}
      </Paper>
    </Box>
  );
};

export default AdMobDetail; 