import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  CardHeader,
  CircularProgress,
  Divider,
  FormControlLabel,
  Grid,
  InputAdornment,
  Switch,
  TextField,
  Typography,
  Snackbar,
  Alert,
  Paper
} from '@mui/material';
import { 
  getConfig, 
  updateConfig,
  triggerManually
} from '../utils/inactivityNotifications';

// Default configuration to use when all else fails
const FALLBACK_CONFIG = {
  inactivityThresholdDays: 3,
  isAutomaticSendEnabled: true,
  automaticSendHour: 10,
  notificationTitle: 'Hey {{displayName}}, we miss you!',
  notificationBody: 'Check out new content since your last visit {{lastOnline}}'
};

const InactivityNotificationSettings = () => {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [triggering, setTriggering] = useState(false);
  const [config, setConfig] = useState(FALLBACK_CONFIG);
  const [manualTriggerDays, setManualTriggerDays] = useState(3);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success'
  });
  const [loadError, setLoadError] = useState(false);
  const [connectionError, setConnectionError] = useState(false);
  const [retryCount, setRetryCount] = useState(0);

  // Load config on mount and when retry is clicked
  useEffect(() => {
    loadConfig();
  }, [retryCount]);

  const loadConfig = async () => {
    setLoading(true);
    setLoadError(false);
    setConnectionError(false);
    
    try {
      const response = await getConfig();
      
      if (response.success && response.config) {
        setConfig(response.config);
        setManualTriggerDays(response.config.inactivityThresholdDays || 3);
      } else {
        // Handle unsuccessful response
        console.error('Failed to load config:', response.message);
        setLoadError(true);
        
        // Check if it's a connection error
        if (response.message && response.message.includes('connect ECONNREFUSED')) {
          setConnectionError(true);
        }
        
        setSnackbar({
          open: true,
          message: response.message || 'Failed to load configuration',
          severity: 'error'
        });
        
        // Still use the fallback config from the response
        if (response.config) {
          setConfig(response.config);
          setManualTriggerDays(response.config.inactivityThresholdDays || 3);
        } else {
          // Use our local fallback if no config in response
          setConfig(FALLBACK_CONFIG);
          setManualTriggerDays(FALLBACK_CONFIG.inactivityThresholdDays);
        }
      }
    } catch (error) {
      console.error('Error loading config:', error);
      setLoadError(true);
      
      // Check if it's a connection error
      if (error.message && error.message.includes('connect ECONNREFUSED')) {
        setConnectionError(true);
      }
      
      setSnackbar({
        open: true,
        message: 'Failed to load configuration: ' + (error.message || 'Unknown error'),
        severity: 'error'
      });
      
      // Use fallback config
      setConfig(FALLBACK_CONFIG);
      setManualTriggerDays(FALLBACK_CONFIG.inactivityThresholdDays);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value, checked } = e.target;
    
    if (name === 'isAutomaticSendEnabled') {
      setConfig({ ...config, [name]: checked });
    } else if (['inactivityThresholdDays', 'automaticSendHour'].includes(name)) {
      setConfig({ ...config, [name]: parseInt(value, 10) });
    } else {
      setConfig({ ...config, [name]: value });
    }
  };

  const handleManualTriggerDaysChange = (e) => {
    setManualTriggerDays(parseInt(e.target.value, 10));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const response = await updateConfig(config);
      if (response.success) {
        setSnackbar({
          open: true,
          message: 'Configuration saved successfully',
          severity: 'success'
        });
        // Refresh config to ensure we have the latest data
        loadConfig();
      } else {
        setSnackbar({
          open: true,
          message: response.message || 'Failed to save configuration',
          severity: 'error'
        });
        
        // If we have a config in the response, update our state
        if (response.config) {
          setConfig(response.config);
        }
      }
    } catch (error) {
      console.error('Error saving config:', error);
      setSnackbar({
        open: true,
        message: 'Failed to save configuration: ' + (error.message || 'Unknown error'),
        severity: 'error'
      });
    } finally {
      setSaving(false);
    }
  };

  const handleTriggerManually = async () => {
    setTriggering(true);
    try {
      const response = await triggerManually(manualTriggerDays);
      if (response.success) {
        setSnackbar({
          open: true,
          message: 'Inactivity notifications triggered successfully',
          severity: 'success'
        });
      } else {
        setSnackbar({
          open: true,
          message: response.message || 'Failed to trigger notifications',
          severity: 'error'
        });
      }
    } catch (error) {
      console.error('Error triggering notifications:', error);
      setSnackbar({
        open: true,
        message: 'Failed to trigger notifications: ' + (error.message || 'Unknown error'),
        severity: 'error'
      });
    } finally {
      setTriggering(false);
    }
  };

  const handleCloseSnackbar = () => {
    setSnackbar({ ...snackbar, open: false });
  };

  const handleRetry = () => {
    setRetryCount(prev => prev + 1);
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (connectionError) {
    return (
      <Box display="flex" flexDirection="column" justifyContent="center" alignItems="center" minHeight="400px">
        <Paper elevation={3} sx={{ p: 4, maxWidth: 500, textAlign: 'center' }}>
          <Typography variant="h5" color="error" gutterBottom>
            Database Connection Error
          </Typography>
          <Typography variant="body1" paragraph>
            Unable to connect to the database. This could be because:
          </Typography>
          <Typography variant="body2" component="ul" sx={{ textAlign: 'left', mb: 3 }}>
            <li>MongoDB is not running</li>
            <li>The connection string is incorrect</li>
            <li>There's a network issue</li>
          </Typography>
          <Typography variant="body2" paragraph>
            You can continue with default settings, but changes won't be saved to the database.
          </Typography>
          <Box mt={3}>
            <Button variant="contained" onClick={handleRetry} sx={{ mr: 2 }}>
              Retry Connection
            </Button>
            <Button variant="outlined" onClick={() => setConnectionError(false)}>
              Continue with Defaults
            </Button>
          </Box>
        </Paper>
      </Box>
    );
  }

  // Add a check to ensure config is properly initialized
  if (!config || typeof config !== 'object') {
    return (
      <Box display="flex" flexDirection="column" justifyContent="center" alignItems="center" minHeight="400px">
        <Typography variant="h6" color="error" gutterBottom>
          Error loading configuration
        </Typography>
        <Button variant="contained" onClick={handleRetry}>
          Retry
        </Button>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Inactivity Notification Settings
      </Typography>
      
      {loadError && (
        <Alert severity="warning" sx={{ mb: 3 }}>
          Using default configuration values. Some settings may not be saved until you fix the database connection.
        </Alert>
      )}
      
      <Card sx={{ mb: 3 }}>
        <CardHeader title="Automatic Notifications" />
        <Divider />
        <CardContent>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={config.isAutomaticSendEnabled}
                    onChange={handleChange}
                    name="isAutomaticSendEnabled"
                    color="primary"
                  />
                }
                label="Enable automatic inactivity notifications"
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Inactivity Threshold (Days)"
                name="inactivityThresholdDays"
                type="number"
                value={config.inactivityThresholdDays}
                onChange={handleChange}
                InputProps={{
                  endAdornment: <InputAdornment position="end">days</InputAdornment>,
                }}
                disabled={!config.isAutomaticSendEnabled}
                helperText="Send notifications to users inactive for this many days"
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Daily Send Time (Hour)"
                name="automaticSendHour"
                type="number"
                value={config.automaticSendHour}
                onChange={handleChange}
                InputProps={{
                  endAdornment: <InputAdornment position="end">:00</InputAdornment>,
                }}
                disabled={!config.isAutomaticSendEnabled}
                helperText="Hour of day to send notifications (0-23, server time)"
                inputProps={{ min: 0, max: 23 }}
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>
      
      <Card sx={{ mb: 3 }}>
        <CardHeader title="Notification Content" />
        <Divider />
        <CardContent>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Notification Title"
                name="notificationTitle"
                value={config.notificationTitle}
                onChange={handleChange}
                helperText="You can use placeholders like {{displayName}}"
              />
            </Grid>
            
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Notification Body"
                name="notificationBody"
                value={config.notificationBody}
                onChange={handleChange}
                multiline
                rows={3}
                helperText="You can use placeholders like {{displayName}}, {{lastOnline}}"
              />
            </Grid>
            
            <Grid item xs={12}>
              <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                Available Placeholders:
              </Typography>
              <Typography variant="body2" color="textSecondary">
                <code>{{displayName}}</code> - User's display name<br />
                <code>{{lastOnline}}</code> - User's last online date (formatted)<br />
                <code>{{preferredLanguage}}</code> - User's preferred language<br />
                <code>{{email}}</code> - User's email address
              </Typography>
            </Grid>
          </Grid>
        </CardContent>
      </Card>
      
      <Box display="flex" justifyContent="flex-end" mb={3}>
        <Button
          variant="contained"
          color="primary"
          onClick={handleSave}
          disabled={saving || connectionError}
          startIcon={saving ? <CircularProgress size={20} /> : null}
        >
          {saving ? 'Saving...' : 'Save Configuration'}
        </Button>
      </Box>
      
      <Card>
        <CardHeader title="Manual Trigger" />
        <Divider />
        <CardContent>
          <Grid container spacing={3} alignItems="center">
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Inactivity Threshold (Days)"
                type="number"
                value={manualTriggerDays}
                onChange={handleManualTriggerDaysChange}
                InputProps={{
                  endAdornment: <InputAdornment position="end">days</InputAdornment>,
                }}
                helperText="Send to users inactive for this many days"
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Button
                variant="contained"
                color="secondary"
                onClick={handleTriggerManually}
                disabled={triggering || connectionError}
                startIcon={triggering ? <CircularProgress size={20} /> : null}
              >
                {triggering ? 'Sending...' : 'Send Notifications Now'}
              </Button>
            </Grid>
            
            <Grid item xs={12}>
              <Typography variant="body2" color="textSecondary">
                This will immediately send notifications to all users who haven't been active 
                for the specified number of days. The notification will use the content configured above.
              </Typography>
            </Grid>
          </Grid>
        </CardContent>
      </Card>
      
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert onClose={handleCloseSnackbar} severity={snackbar.severity}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default InactivityNotificationSettings; 