import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Box,
  Button,
  Container,
  Paper,
  Typography,
  CircularProgress,
  Snackbar,
  Alert,
  Stack,
  IconButton,
  Grid,
  Divider,
  Chip,
  Card,
  CardContent,
  LinearProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Send as SendIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  Schedule as ScheduleIcon
} from '@mui/icons-material';
import { getPushNotificationById, sendPushNotification, deletePushNotification } from '../api';
// date-fns import removed due to compatibility issues

const PushNotificationDetail = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const [loading, setLoading] = useState(true);
  const [notification, setNotification] = useState(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [sendDialog, setSendDialog] = useState(false);
  const [deleteDialog, setDeleteDialog] = useState(false);
  const [actionInProgress, setActionInProgress] = useState(false);

  useEffect(() => {
    const fetchNotification = async () => {
      try {
        const data = await getPushNotificationById(id);
        if (data.success) {
          setNotification(data.notification);
        } else {
          throw new Error(data.message || 'Failed to load notification');
        }
      } catch (error) {
        console.error('Error fetching notification:', error);
        setSnackbar({
          open: true,
          message: 'Failed to load notification details',
          severity: 'error'
        });
      } finally {
        setLoading(false);
      }
    };

    fetchNotification();
  }, [id]);

  const handleSendNotification = async () => {
    setActionInProgress(true);
    try {
      const response = await sendPushNotification(id);
      if (response.success) {
        setNotification(response.notification);
        setSnackbar({
          open: true,
          message: 'Notification sending started',
          severity: 'success'
        });
      } else {
        throw new Error(response.message || 'Failed to send notification');
      }
    } catch (error) {
      console.error('Error sending notification:', error);
      setSnackbar({
        open: true,
        message: `Error sending notification: ${error.message}`,
        severity: 'error'
      });
    } finally {
      setSendDialog(false);
      setActionInProgress(false);
    }
  };

  const handleDeleteNotification = async () => {
    setActionInProgress(true);
    try {
      const response = await deletePushNotification(id);
      if (response.success) {
        setSnackbar({
          open: true,
          message: 'Notification deleted successfully',
          severity: 'success'
        });
        setTimeout(() => navigate('/push-notifications'), 1500);
      } else {
        throw new Error(response.message || 'Failed to delete notification');
      }
    } catch (error) {
      console.error('Error deleting notification:', error);
      setSnackbar({
        open: true,
        message: `Error deleting notification: ${error.message}`,
        severity: 'error'
      });
    } finally {
      setDeleteDialog(false);
      setActionInProgress(false);
    }
  };

  const getStatusChip = (status) => {
    switch (status) {
      case 'DRAFT':
        return <Chip label="Draft" color="default" size="small" />;
      case 'SCHEDULED':
        return <Chip label="Scheduled" color="primary" size="small" icon={<ScheduleIcon />} />;
      case 'SENDING':
        return <Chip label="Sending" color="warning" size="small" />;
      case 'SENT':
        return <Chip label="Sent" color="success" size="small" icon={<CheckCircleIcon />} />;
      case 'FAILED':
        return <Chip label="Failed" color="error" size="small" icon={<ErrorIcon />} />;
      default:
        return <Chip label={status} size="small" />;
    }
  };

  if (loading) {
    return (
      <Container>
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="60vh">
          <CircularProgress />
        </Box>
      </Container>
    );
  }

  if (!notification) {
    return (
      <Container>
        <Box mb={4}>
          <Stack direction="row" alignItems="center" spacing={1} mb={2}>
            <IconButton onClick={() => navigate('/push-notifications')}>
              <ArrowBackIcon />
            </IconButton>
            <Typography variant="h4">Notification Not Found</Typography>
          </Stack>
        </Box>
        <Paper sx={{ p: 3, textAlign: 'center' }}>
          <Typography>
            The notification you are looking for does not exist or has been deleted.
          </Typography>
          <Button
            variant="contained"
            onClick={() => navigate('/push-notifications')}
            sx={{ mt: 2 }}
          >
            Back to Notifications
          </Button>
        </Paper>
      </Container>
    );
  }

  const canSend = ['DRAFT', 'SCHEDULED'].includes(notification.status);
  const canEdit = ['DRAFT', 'SCHEDULED', 'FAILED'].includes(notification.status);
  const canDelete = ['DRAFT', 'SCHEDULED', 'FAILED'].includes(notification.status);
  
  const successRate = notification.stats?.total > 0
    ? Math.round((notification.stats.success / notification.stats.total) * 100)
    : 0;

  return (
    <Container>
      <Box mb={4}>
        <Stack direction="row" alignItems="center" spacing={1} mb={2}>
          <IconButton onClick={() => navigate('/push-notifications')}>
            <ArrowBackIcon />
          </IconButton>
          <Typography variant="h4">Notification Details</Typography>
          <Box sx={{ flexGrow: 1 }} />
          {getStatusChip(notification.status)}
        </Stack>
      </Box>
      
      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h5" gutterBottom>{notification.title}</Typography>
            <Typography variant="body1" paragraph>{notification.body}</Typography>
            
            <Divider sx={{ my: 2 }} />
            
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="textSecondary">Type</Typography>
                <Typography variant="body1">{notification.type}</Typography>
              </Grid>
              
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="textSecondary">Created By</Typography>
                <Typography variant="body1">{notification.createdBy || 'Unknown'}</Typography>
              </Grid>
              
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="textSecondary">Created At</Typography>
                <Typography variant="body1">
                  {notification.createdAt ? new Date(notification.createdAt).toLocaleString() : 'N/A'}
                </Typography>
              </Grid>
              
              <Grid item xs={12} sm={6}>
                <Typography variant="subtitle2" color="textSecondary">Updated At</Typography>
                <Typography variant="body1">
                  {notification.updatedAt ? new Date(notification.updatedAt).toLocaleString() : 'N/A'}
                </Typography>
              </Grid>
              
              {notification.scheduledFor && (
                <Grid item xs={12} sm={6}>
                  <Typography variant="subtitle2" color="textSecondary">Scheduled For</Typography>
                  <Typography variant="body1">
                    {new Date(notification.scheduledFor).toLocaleString()}
                  </Typography>
                </Grid>
              )}
              
              {notification.sentAt && (
                <Grid item xs={12} sm={6}>
                  <Typography variant="subtitle2" color="textSecondary">Sent At</Typography>
                  <Typography variant="body1">
                    {new Date(notification.sentAt).toLocaleString()}
                  </Typography>
                </Grid>
              )}
              
              {notification.type === 'TOPIC' && (
                <Grid item xs={12} sm={6}>
                  <Typography variant="subtitle2" color="textSecondary">Topic</Typography>
                  <Typography variant="body1">{notification.topic || 'N/A'}</Typography>
                </Grid>
              )}
              
              {notification.imageUrl && (
                <Grid item xs={12}>
                  <Typography variant="subtitle2" color="textSecondary">Image</Typography>
                  <Box sx={{ mt: 1, textAlign: 'center' }}>
                    <img 
                      src={notification.imageUrl} 
                      alt="Notification" 
                      style={{ maxWidth: '100%', maxHeight: '200px', objectFit: 'contain' }}
                      onError={(e) => {
                        e.target.onerror = null;
                        e.target.src = 'https://via.placeholder.com/400x200?text=Invalid+Image+URL';
                      }}
                    />
                  </Box>
                </Grid>
              )}
            </Grid>
            
            <Divider sx={{ my: 2 }} />
            
            <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
              <Button 
                variant="contained" 
                onClick={() => navigate('/push-notifications')}
              >
                Back to List
              </Button>
              
              {canSend && (
                <Button
                  variant="contained"
                  color="primary"
                  startIcon={<SendIcon />}
                  onClick={() => setSendDialog(true)}
                >
                  Send Now
                </Button>
              )}
              
              {canEdit && (
                <Button
                  variant="outlined"
                  startIcon={<EditIcon />}
                  onClick={() => navigate(`/push-notifications/edit/${notification._id}`)}
                >
                  Edit
                </Button>
              )}
              
              {canDelete && (
                <Button
                  variant="outlined"
                  color="error"
                  startIcon={<DeleteIcon />}
                  onClick={() => setDeleteDialog(true)}
                >
                  Delete
                </Button>
              )}
            </Stack>
          </Paper>
        </Grid>
        
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Delivery Stats</Typography>
              
              {notification.status === 'SENT' || notification.status === 'FAILED' ? (
                <>
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="body2" color="textSecondary">
                      Success Rate: {successRate}%
                    </Typography>
                    <LinearProgress 
                      variant="determinate" 
                      value={successRate} 
                      color={successRate > 90 ? 'success' : successRate > 50 ? 'warning' : 'error'}
                      sx={{ mt: 1, height: 10, borderRadius: 5 }}
                    />
                  </Box>
                  
                  <Grid container spacing={2} sx={{ mt: 1 }}>
                    <Grid item xs={4}>
                      <Typography variant="body2" color="textSecondary">Total</Typography>
                      <Typography variant="h6">{notification.stats?.total || 0}</Typography>
                    </Grid>
                    <Grid item xs={4}>
                      <Typography variant="body2" color="textSecondary">Success</Typography>
                      <Typography variant="h6" color="success.main">{notification.stats?.success || 0}</Typography>
                    </Grid>
                    <Grid item xs={4}>
                      <Typography variant="body2" color="textSecondary">Failed</Typography>
                      <Typography variant="h6" color="error.main">{notification.stats?.failure || 0}</Typography>
                    </Grid>
                  </Grid>
                </>
              ) : (
                <Typography variant="body2" color="textSecondary">
                  {notification.status === 'SENDING' 
                    ? 'Notification is currently being sent...' 
                    : 'Notification has not been sent yet'}
                </Typography>
              )}
            </CardContent>
          </Card>
          
          {notification.type === 'PERSONALIZED' && notification.targetUserIds && notification.targetUserIds.length > 0 && (
            <Card sx={{ mt: 2 }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>Target Users</Typography>
                <Typography variant="body2">
                  This notification targets {notification.targetUserIds.length} specific users.
                </Typography>
              </CardContent>
            </Card>
          )}
        </Grid>
      </Grid>
      
      {/* Send Confirmation Dialog */}
      <Dialog
        open={sendDialog}
        onClose={() => !actionInProgress && setSendDialog(false)}
        aria-labelledby="send-dialog-title"
      >
        <DialogTitle id="send-dialog-title">Send Notification</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to send this notification now? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button 
            onClick={() => setSendDialog(false)} 
            disabled={actionInProgress}
          >
            Cancel
          </Button>
          <Button 
            onClick={handleSendNotification} 
            color="primary" 
            disabled={actionInProgress}
            startIcon={actionInProgress ? <CircularProgress size={20} /> : <SendIcon />}
          >
            {actionInProgress ? 'Sending...' : 'Send'}
          </Button>
        </DialogActions>
      </Dialog>
      
      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteDialog}
        onClose={() => !actionInProgress && setDeleteDialog(false)}
        aria-labelledby="delete-dialog-title"
      >
        <DialogTitle id="delete-dialog-title">Delete Notification</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete this notification? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button 
            onClick={() => setDeleteDialog(false)} 
            disabled={actionInProgress}
          >
            Cancel
          </Button>
          <Button 
            onClick={handleDeleteNotification} 
            color="error" 
            disabled={actionInProgress}
            startIcon={actionInProgress ? <CircularProgress size={20} /> : <DeleteIcon />}
          >
            {actionInProgress ? 'Deleting...' : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>
      
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={() => setSnackbar({...snackbar, open: false})}
      >
        <Alert severity={snackbar.severity} sx={{ width: '100%' }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default PushNotificationDetail;
 