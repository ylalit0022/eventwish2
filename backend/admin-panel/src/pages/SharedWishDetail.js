import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Grid,
  TextField,
  Button,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
  FormControlLabel,
  Switch,
  Divider,
  IconButton,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Chip,
  List,
  ListItem,
  ListItemText,
  ListItemIcon
} from '@mui/material';
import {
  Save as SaveIcon,
  Delete as DeleteIcon,
  ArrowBack as ArrowBackIcon,
  Edit as EditIcon,
  Cancel as CancelIcon,
  Visibility as ViewsIcon,
  Share as ShareIcon,
  Link as LinkIcon,
  Code as CodeIcon,
  Person as PersonIcon,
  Description as DescriptionIcon,
  Timeline as TimelineIcon,
  Email as EmailIcon
} from '@mui/icons-material';
import { getSharedWishById, updateSharedWish, deleteSharedWish, getUserByObjectId } from '../api';

// TabPanel component for tab content
function TabPanel({ children, value, index, ...other }) {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`sharedwish-tabpanel-${index}`}
      aria-labelledby={`sharedwish-tab-${index}`}
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

const SharedWishDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [sharedWish, setSharedWish] = useState(null);
  const [editedSharedWish, setEditedSharedWish] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saveLoading, setSaveLoading] = useState(false);
  const [error, setError] = useState(null);
  const [tabValue, setTabValue] = useState(0);
  const [editMode, setEditMode] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);

  // Fetch shared wish data
  useEffect(() => {
    const fetchSharedWish = async () => {
      try {
        setLoading(true);
        setError(null);
        
        const response = await getSharedWishById(id);
        
        if (response.success) {
          setSharedWish(response.sharedWish);
          setEditedSharedWish(response.sharedWish);
        } else {
          throw new Error(response.message || 'Failed to load shared wish details');
        }
      } catch (err) {
        console.error(`Error fetching shared wish ${id}:`, err);
        setError('Failed to load shared wish details: ' + (err.message || 'Unknown error'));
      } finally {
        setLoading(false);
      }
    };

    fetchSharedWish();
  }, [id]);

  // Handle tab change
  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  // Handle edit mode toggle
  const handleEditToggle = () => {
    if (editMode) {
      setEditedSharedWish(sharedWish); // Reset changes
    }
    setEditMode(!editMode);
  };

  // Handle field change
  const handleFieldChange = (field, value) => {
    setEditedSharedWish(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Handle save changes
  const handleSaveChanges = async () => {
    try {
      setSaveLoading(true);
      setError(null);
      
      const response = await updateSharedWish(id, editedSharedWish);
      
      if (response.success) {
        setSharedWish(response.sharedWish);
        setEditMode(false);
      } else {
        throw new Error(response.message || 'Failed to update shared wish');
      }
    } catch (err) {
      console.error(`Error updating shared wish ${id}:`, err);
      setError('Failed to update shared wish: ' + (err.message || 'Unknown error'));
    } finally {
      setSaveLoading(false);
    }
  };

  // Handle delete dialog
  const handleDeleteClick = () => {
    setDeleteDialogOpen(true);
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
  };

  const handleDeleteConfirm = async () => {
    try {
      setDeleteLoading(true);
      
      const response = await deleteSharedWish(id);
      
      if (response.success) {
        navigate('/shared-wishes');
      } else {
        throw new Error(response.message || 'Failed to delete shared wish');
      }
    } catch (err) {
      console.error(`Error deleting shared wish ${id}:`, err);
      setError('Failed to delete shared wish: ' + (err.message || 'Unknown error'));
      setDeleteDialogOpen(false);
    } finally {
      setDeleteLoading(false);
    }
  };

  // Handle back button
  const handleBackClick = () => {
    navigate('/shared-wishes');
  };

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString();
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error && !sharedWish) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error}</Alert>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={handleBackClick}
          sx={{ mt: 2 }}
        >
          Back to Shared Wishes
        </Button>
      </Box>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          <IconButton onClick={handleBackClick} sx={{ mr: 1 }}>
            <ArrowBackIcon />
          </IconButton>
          <Typography variant="h4" component="h1">
            Shared Wish: {sharedWish?.shortCode}
          </Typography>
        </Box>
        <Box>
          {editMode ? (
            <>
              <Button
                variant="outlined"
                color="secondary"
                startIcon={<CancelIcon />}
                onClick={handleEditToggle}
                sx={{ mr: 1 }}
                disabled={saveLoading}
              >
                Cancel
              </Button>
              <Button
                variant="contained"
                color="primary"
                startIcon={saveLoading ? <CircularProgress size={24} /> : <SaveIcon />}
                onClick={handleSaveChanges}
                disabled={saveLoading}
              >
                {saveLoading ? 'Saving...' : 'Save Changes'}
              </Button>
            </>
          ) : (
            <>
              <Button
                variant="outlined"
                color="error"
                startIcon={<DeleteIcon />}
                onClick={handleDeleteClick}
                sx={{ mr: 1 }}
              >
                Delete
              </Button>
              <Button
                variant="contained"
                color="primary"
                startIcon={<EditIcon />}
                onClick={handleEditToggle}
              >
                Edit
              </Button>
            </>
          )}
        </Box>
      </Box>

      {/* Error alert */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Tabs */}
      <Paper sx={{ mb: 3 }}>
        <Tabs
          value={tabValue}
          onChange={handleTabChange}
          indicatorColor="primary"
          textColor="primary"
          variant="scrollable"
          scrollButtons="auto"
        >
          <Tab label="Basic Info" icon={<DescriptionIcon />} iconPosition="start" />
          <Tab label="Content" icon={<CodeIcon />} iconPosition="start" />
          <Tab label="Sharing" icon={<ShareIcon />} iconPosition="start" />
          <Tab label="Analytics" icon={<TimelineIcon />} iconPosition="start" />
        </Tabs>
      </Paper>

      {/* Basic Info Tab */}
      <TabPanel value={tabValue} index={0}>
        <Paper sx={{ p: 3 }}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Short Code"
                value={editedSharedWish?.shortCode || ''}
                onChange={(e) => handleFieldChange('shortCode', e.target.value)}
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Template ID"
                value={editedSharedWish?.template?._id || editedSharedWish?.template || ''}
                disabled={true} // Template ID should not be editable
                margin="normal"
                helperText="Template ID cannot be changed"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Title"
                value={editedSharedWish?.title || ''}
                onChange={(e) => handleFieldChange('title', e.target.value)}
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Description"
                value={editedSharedWish?.description || ''}
                onChange={(e) => handleFieldChange('description', e.target.value)}
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Recipient Name"
                value={editedSharedWish?.recipientName || ''}
                onChange={(e) => handleFieldChange('recipientName', e.target.value)}
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Sender Name"
                value={editedSharedWish?.senderName || ''}
                onChange={(e) => handleFieldChange('senderName', e.target.value)}
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Preview URL"
                value={editedSharedWish?.previewUrl || ''}
                onChange={(e) => handleFieldChange('previewUrl', e.target.value)}
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Deep Link"
                value={editedSharedWish?.deeplink || ''}
                onChange={(e) => handleFieldChange('deeplink', e.target.value)}
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={editedSharedWish?.isPremiumShared || false}
                    onChange={(e) => handleFieldChange('isPremiumShared', e.target.checked)}
                    disabled={!editMode}
                  />
                }
                label="Premium Shared"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="subtitle2" gutterBottom>
                Created At: {formatDate(sharedWish?.createdAt)}
              </Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="subtitle2" gutterBottom>
                Updated At: {formatDate(sharedWish?.updatedAt)}
              </Typography>
            </Grid>
          </Grid>
        </Paper>
      </TabPanel>

      {/* Content Tab */}
      <TabPanel value={tabValue} index={1}>
        <Paper sx={{ p: 3 }}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Customized HTML"
                value={editedSharedWish?.customizedHtml || ''}
                onChange={(e) => handleFieldChange('customizedHtml', e.target.value)}
                disabled={!editMode}
                margin="normal"
                multiline
                rows={10}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="CSS Content"
                value={editedSharedWish?.cssContent || ''}
                onChange={(e) => handleFieldChange('cssContent', e.target.value)}
                disabled={!editMode}
                margin="normal"
                multiline
                rows={6}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="JS Content"
                value={editedSharedWish?.jsContent || ''}
                onChange={(e) => handleFieldChange('jsContent', e.target.value)}
                disabled={!editMode}
                margin="normal"
                multiline
                rows={6}
              />
            </Grid>
          </Grid>
        </Paper>
      </TabPanel>

      {/* Sharing Tab */}
      <TabPanel value={tabValue} index={2}>
        <Paper sx={{ p: 3 }}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Shared Via"
                value={editedSharedWish?.sharedVia || ''}
                onChange={(e) => handleFieldChange('sharedVia', e.target.value)}
                disabled={!editMode}
                margin="normal"
                select
                SelectProps={{
                  native: true,
                }}
              >
                <option value="LINK">Link</option>
                <option value="WHATSAPP">WhatsApp</option>
                <option value="FACEBOOK">Facebook</option>
                <option value="TWITTER">Twitter</option>
                <option value="INSTAGRAM">Instagram</option>
                <option value="EMAIL">Email</option>
                <option value="SMS">SMS</option>
                <option value="OTHER">Other</option>
              </TextField>
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Last Shared At"
                value={editedSharedWish?.lastSharedAt ? new Date(editedSharedWish.lastSharedAt).toISOString().slice(0, 16) : ''}
                onChange={(e) => handleFieldChange('lastSharedAt', new Date(e.target.value).toISOString())}
                disabled={!editMode}
                margin="normal"
                type="datetime-local"
                InputLabelProps={{
                  shrink: true,
                }}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Views"
                value={editedSharedWish?.views || 0}
                onChange={(e) => handleFieldChange('views', parseInt(e.target.value) || 0)}
                disabled={!editMode}
                margin="normal"
                type="number"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Unique Views"
                value={editedSharedWish?.uniqueViews || 0}
                onChange={(e) => handleFieldChange('uniqueViews', parseInt(e.target.value) || 0)}
                disabled={!editMode}
                margin="normal"
                type="number"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Share Count"
                value={editedSharedWish?.shareCount || 0}
                onChange={(e) => handleFieldChange('shareCount', parseInt(e.target.value) || 0)}
                disabled={!editMode}
                margin="normal"
                type="number"
              />
            </Grid>
            <Grid item xs={12}>
              <Typography variant="subtitle1" gutterBottom>
                Viewer IPs
              </Typography>
              <TextField
                fullWidth
                value={editedSharedWish?.viewerIps?.join(', ') || ''}
                onChange={(e) => handleFieldChange('viewerIps', e.target.value.split(',').map(ip => ip.trim()).filter(ip => ip))}
                disabled={!editMode}
                margin="normal"
                helperText="Comma-separated list of IPs"
              />
            </Grid>
            <Grid item xs={12}>
              <Typography variant="subtitle1" gutterBottom>
                Share History
              </Typography>
              <List>
                {sharedWish?.shareHistory?.map((share, index) => (
                  <ListItem key={index} divider>
                    <ListItemIcon>
                      <ShareIcon />
                    </ListItemIcon>
                    <ListItemText
                      primary={share.platform}
                      secondary={formatDate(share.timestamp)}
                    />
                  </ListItem>
                )) || <Typography variant="body2">No share history</Typography>}
              </List>
            </Grid>
          </Grid>
        </Paper>
      </TabPanel>

      {/* Analytics Tab */}
      <TabPanel value={tabValue} index={3}>
        <Paper sx={{ p: 3 }}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Conversion Source"
                value={editedSharedWish?.conversionSource || ''}
                onChange={(e) => handleFieldChange('conversionSource', e.target.value)}
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Referrer"
                value={editedSharedWish?.referrer || ''}
                onChange={(e) => handleFieldChange('referrer', e.target.value)}
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Device Info"
                value={editedSharedWish?.deviceInfo || ''}
                onChange={(e) => handleFieldChange('deviceInfo', e.target.value)}
                disabled={!editMode}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12}>
              <Typography variant="subtitle1" gutterBottom>
                Viewer Engagement
              </Typography>
              {editMode ? (
                <Box sx={{ mb: 2 }}>
                  <Button 
                    variant="outlined" 
                    color="primary" 
                    size="small" 
                    onClick={() => {
                      const newEngagement = {
                        userId: '',
                        action: 'VIEWED',
                        timestamp: new Date().toISOString()
                      };
                      setEditedSharedWish(prev => ({
                        ...prev,
                        viewerEngagement: [...(prev.viewerEngagement || []), newEngagement]
                      }));
                    }}
                    sx={{ mb: 2 }}
                  >
                    Add Engagement Record
                  </Button>
                  {editedSharedWish?.viewerEngagement?.map((engagement, index) => (
                    <Paper key={index} sx={{ p: 2, mb: 2, border: '1px solid #e0e0e0' }}>
                      <Grid container spacing={2}>
                        <Grid item xs={12} md={4}>
                          <TextField
                            fullWidth
                            label="User ID"
                            value={engagement.userId || ''}
                            onChange={(e) => {
                              const newEngagements = [...editedSharedWish.viewerEngagement];
                              newEngagements[index].userId = e.target.value;
                              handleFieldChange('viewerEngagement', newEngagements);
                            }}
                            margin="dense"
                          />
                        </Grid>
                        <Grid item xs={12} md={4}>
                          <TextField
                            fullWidth
                            label="Action"
                            value={engagement.action || 'VIEWED'}
                            onChange={(e) => {
                              const newEngagements = [...editedSharedWish.viewerEngagement];
                              newEngagements[index].action = e.target.value;
                              handleFieldChange('viewerEngagement', newEngagements);
                            }}
                            select
                            SelectProps={{
                              native: true,
                            }}
                            margin="dense"
                          >
                            <option value="VIEWED">Viewed</option>
                            <option value="LIKED">Liked</option>
                            <option value="FAVORITED">Favorited</option>
                            <option value="SHARED">Shared</option>
                          </TextField>
                        </Grid>
                        <Grid item xs={12} md={4}>
                          <TextField
                            fullWidth
                            label="Timestamp"
                            type="datetime-local"
                            value={engagement.timestamp ? new Date(engagement.timestamp).toISOString().slice(0, 16) : ''}
                            onChange={(e) => {
                              const newEngagements = [...editedSharedWish.viewerEngagement];
                              newEngagements[index].timestamp = new Date(e.target.value).toISOString();
                              handleFieldChange('viewerEngagement', newEngagements);
                            }}
                            InputLabelProps={{
                              shrink: true,
                            }}
                            margin="dense"
                          />
                        </Grid>
                        <Grid item xs={12} display="flex" justifyContent="flex-end">
                          <Button 
                            variant="outlined" 
                            color="error" 
                            size="small"
                            onClick={() => {
                              const newEngagements = editedSharedWish.viewerEngagement.filter((_, i) => i !== index);
                              handleFieldChange('viewerEngagement', newEngagements);
                            }}
                          >
                            Remove
                          </Button>
                        </Grid>
                      </Grid>
                    </Paper>
                  ))}
                </Box>
              ) : (
                <List>
                  {sharedWish?.viewerEngagement?.length > 0 ? (
                    sharedWish.viewerEngagement.map((engagement, index) => (
                      <ListItem key={index} divider>
                        <ListItemIcon>
                          {engagement.action === 'VIEWED' && <ViewsIcon />}
                          {engagement.action === 'LIKED' && <PersonIcon />}
                          {engagement.action === 'FAVORITED' && <PersonIcon />}
                          {engagement.action === 'SHARED' && <ShareIcon />}
                        </ListItemIcon>
                        <ListItemText
                          primary={
                            <Typography>
                              <strong>{engagement.action}</strong> by {
                                engagement.userId && typeof engagement.userId === 'object' && engagement.userId.email ? (
                                  <Chip 
                                    label={engagement.userId.email} 
                                    size="small" 
                                    color="primary" 
                                    variant="outlined"
                                    icon={<EmailIcon />}
                                  />
                                ) : (
                                  'Anonymous'
                                )
                              }
                            </Typography>
                          }
                          secondary={formatDate(engagement.timestamp)}
                        />
                      </ListItem>
                    ))
                  ) : (
                    <Typography variant="body2">No engagement data</Typography>
                  )}
                </List>
              )}
            </Grid>
          </Grid>
        </Paper>
      </TabPanel>

      {/* Delete Dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
      >
        <DialogTitle>Delete Shared Wish</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete this shared wish with short code{' '}
            <strong>{sharedWish?.shortCode}</strong>?
            This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel} disabled={deleteLoading}>
            Cancel
          </Button>
          <Button 
            onClick={handleDeleteConfirm} 
            color="error" 
            disabled={deleteLoading}
            startIcon={deleteLoading ? <CircularProgress size={20} /> : null}
          >
            {deleteLoading ? 'Deleting...' : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SharedWishDetail;
