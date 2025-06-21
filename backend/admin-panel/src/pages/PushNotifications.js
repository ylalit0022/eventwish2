import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Divider,
  FormControl,
  Grid,
  IconButton,
  InputAdornment,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Snackbar,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
  Alert,
  Tooltip,
  AlertTitle,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle
} from '@mui/material';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  Search as SearchIcon,
  Send as SendIcon,
  Refresh as RefreshIcon,
  FilterList as FilterListIcon,
  Warning as WarningIcon,
  Check as CheckIcon,
  Clear as ClearIcon,
  DataUsage as DataUsageIcon
} from '@mui/icons-material';
import { getPushNotifications, getPushNotificationStats, sendPushNotification, getTopicsWithCounts, checkMongoDBStatus } from '../api';

const PushNotifications = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [statsLoading, setStatsLoading] = useState(true);
  const [topicsLoading, setTopicsLoading] = useState(true);
  const [notifications, setNotifications] = useState([]);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalNotifications, setTotalNotifications] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [typeFilter, setTypeFilter] = useState('all');
  const [sortField, setSortField] = useState('createdAt');
  const [sortOrder, setSortOrder] = useState('desc');
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [stats, setStats] = useState({
    total: 0,
    sent: 0,
    pending: 0,
    failed: 0,
    successRate: 0
  });
  const [topicStats, setTopicStats] = useState([]);
  const [sendLoading, setSendLoading] = useState({});
  const [usingMockData, setUsingMockData] = useState(false);
  const [mongodbStatus, setMongodbStatus] = useState(null);
  const [mongodbDialogOpen, setMongodbDialogOpen] = useState(false);
  const [mongodbLoading, setMongodbLoading] = useState(false);
  const [error, setError] = useState(null);

  // Fetch notifications
  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const filters = {};
      
      if (searchQuery) {
        filters.q = searchQuery;
      }
      
      if (statusFilter !== 'all') {
        filters.isSent = statusFilter === 'sent';
      }
      
      if (typeFilter !== 'all') {
        filters.type = typeFilter;
      }
      
      const data = await getPushNotifications(
        page + 1,
        rowsPerPage,
        sortField,
        sortOrder,
        filters
      );
      
      // Reset using mock data flag if we got real data
      setUsingMockData(false);
      
      setNotifications(data.notifications || []);
      setTotalNotifications(data.pagination?.total || 0);
    } catch (err) {
      console.error('Error fetching notifications:', err);
      setError('Failed to load notifications: ' + (err.message || 'Unknown error'));
      
      // Check if this is a MongoDB connection error
      const isMongoDBError = err.message?.includes('ECONNREFUSED') || 
                            err.response?.data?.message?.includes('MongoDB');
      
      if (isMongoDBError) {
        console.log('MongoDB appears to be unavailable, using mock data for notifications');
        setUsingMockData(true);
        
        // Use mock data as fallback
        try {
          const mockData = await import('../utils/mockData').then(module => module.default);
          setNotifications(mockData.pushNotifications.notifications || []);
          setTotalNotifications(mockData.pushNotifications.pagination?.total || 0);
        } catch (mockError) {
          console.error('Error loading mock data:', mockError);
          setNotifications([]);
          setTotalNotifications(0);
        }
      } else {
        // For other errors, just clear the data
        setNotifications([]);
        setTotalNotifications(0);
      }
    } finally {
      setLoading(false);
    }
  };

  // Fetch notification statistics
  const fetchStats = async () => {
    setStatsLoading(true);
    try {
      const data = await getPushNotificationStats();
      
      // Reset using mock data flag if we got real data
      setUsingMockData(false);
      
      setStats(data.stats || {
        total: 0,
        sent: 0,
        pending: 0,
        failed: 0,
        successRate: 0
      });
    } catch (err) {
      console.error('Error fetching stats:', err);
      
      // Check if this is a MongoDB connection error
      const isMongoDBError = err.message?.includes('ECONNREFUSED') || 
                            err.response?.data?.message?.includes('MongoDB');
      
      if (isMongoDBError) {
        console.log('MongoDB appears to be unavailable, using mock data for stats');
        setUsingMockData(true);
        
        // Use mock data as fallback
        try {
          const mockData = await import('../utils/mockData').then(module => module.default);
          setStats(mockData.pushNotificationStats.stats || {
            total: 0,
            sent: 0,
            pending: 0,
            failed: 0,
            successRate: 0
          });
        } catch (mockError) {
          console.error('Error loading mock data:', mockError);
          setStats({
            total: 0,
            sent: 0,
            pending: 0,
            failed: 0,
            successRate: 0
          });
        }
      } else {
        // For other errors, just use empty stats
        setStats({
          total: 0,
          sent: 0,
          pending: 0,
          failed: 0,
          successRate: 0
        });
      }
    } finally {
      setStatsLoading(false);
    }
  };

  // Fetch topics with counts
  const fetchTopics = async () => {
    setTopicsLoading(true);
    try {
      const data = await getTopicsWithCounts();
      
      // Reset using mock data flag if we got real data
      setUsingMockData(false);
      
      setTopicStats(data.topics || []);
    } catch (err) {
      console.error('Error fetching topics:', err);
      
      // Check if this is a MongoDB connection error
      const isMongoDBError = err.message?.includes('ECONNREFUSED') || 
                            err.response?.data?.message?.includes('MongoDB');
      
      if (isMongoDBError) {
        console.log('MongoDB appears to be unavailable, using mock data for topics');
        setUsingMockData(true);
        
        // Use mock data as fallback
        try {
          const mockData = await import('../utils/mockData').then(module => module.default);
          setTopicStats(mockData.topicsWithCounts.topics || []);
        } catch (mockError) {
          console.error('Error loading mock data:', mockError);
          setTopicStats([]);
        }
      } else {
        // For other errors, just use empty array
        setTopicStats([]);
      }
    } finally {
      setTopicsLoading(false);
    }
  };

  // Check MongoDB connection
  const checkMongoConnection = async () => {
    try {
      const result = await checkMongoDBStatus();
      console.log('MongoDB connection check result:', result);
      
      // Set MongoDB connected state based on the response
      const isConnected = result.success && result.mongodb?.connectionState === 1;
      setMongodbStatus(result);
      setMongodbDialogOpen(true);
      
      if (!isConnected) {
        console.warn('MongoDB is not connected. Using mock data.');
      }
      
      return isConnected;
    } catch (error) {
      console.error('Error checking MongoDB connection:', error);
      setMongodbStatus(null);
      setUsingMockData(true);
      return false;
    }
  };

  // Fetch all data
  const fetchAllData = async () => {
    setLoading(true);
    setStatsLoading(true);
    setTopicsLoading(true);
    setError(null);
    
    try {
      // Check MongoDB connection first
      const isConnected = await checkMongoConnection();
      
      // Create a promise that rejects after 10 seconds
      const timeout = new Promise((_, reject) => {
        setTimeout(() => reject(new Error('Request timeout after 10 seconds')), 10000);
      });
      
      // Use Promise.allSettled instead of Promise.all to handle individual failures
      const results = await Promise.allSettled([
        Promise.race([getPushNotifications(), timeout]),
        Promise.race([getTopicsWithCounts(), timeout]),
        Promise.race([getPushNotificationStats(), timeout])
      ]);
      
      // Process notifications
      if (results[0].status === 'fulfilled' && results[0].value) {
        setNotifications(results[0].value.notifications || []);
      } else {
        console.error('Error fetching notifications:', results[0].reason);
        setUsingMockData(true);
        
        // Import mockData only when needed
        const mockData = await import('../utils/mockData').then(module => module.default);
        setNotifications(mockData.pushNotifications.notifications || []);
      }
      
      // Process topics
      if (results[1].status === 'fulfilled' && results[1].value) {
        setTopicStats(results[1].value.topics || []);
      } else {
        console.error('Error fetching topics:', results[1].reason);
        setUsingMockData(true);
        
        // Import mockData only when needed
        const mockData = await import('../utils/mockData').then(module => module.default);
        setTopicStats(mockData.topicsWithCounts.topics || []);
      }
      
      // Process stats
      if (results[2].status === 'fulfilled' && results[2].value) {
        setStats(results[2].value.stats || {});
      } else {
        console.error('Error fetching stats:', results[2].reason);
        setUsingMockData(true);
        
        // Import mockData only when needed
        const mockData = await import('../utils/mockData').then(module => module.default);
        setStats(mockData.pushNotificationStats.stats || {});
      }
    } catch (error) {
      console.error('Error fetching data:', error);
      setError('Failed to load data. Please try again later.');
      
      // Import mockData only when needed
      try {
        const mockData = await import('../utils/mockData').then(module => module.default);
        
        // Fall back to mock data
        setNotifications(mockData.pushNotifications.notifications || []);
        setTopicStats(mockData.topicsWithCounts.topics || []);
        setStats(mockData.pushNotificationStats.stats || {});
        setUsingMockData(true);
      } catch (mockError) {
        console.error('Error loading mock data:', mockError);
      }
    } finally {
      setLoading(false);
      setStatsLoading(false);
      setTopicsLoading(false);
    }
  };

  // Initial load
  useEffect(() => {
    fetchAllData();
  }, [page, rowsPerPage, sortField, sortOrder, statusFilter, typeFilter]);

  // Handle search when user presses Enter
  const handleSearch = (event) => {
    if (event.key === 'Enter') {
      fetchNotifications();
    }
  };

  // Handle page change
  const handleChangePage = (event, newPage) => {
    setPage(newPage);
  };

  // Handle rows per page change
  const handleChangeRowsPerPage = (event) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  // Handle status filter change
  const handleStatusFilterChange = (event) => {
    setStatusFilter(event.target.value);
    setPage(0);
  };

  // Handle type filter change
  const handleTypeFilterChange = (event) => {
    setTypeFilter(event.target.value);
    setPage(0);
  };

  // Handle sort change
  const handleSortChange = (field) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('desc');
    }
    setPage(0);
  };

  // Handle send notification
  const handleSendNotification = async (id) => {
    setSendLoading((prev) => ({ ...prev, [id]: true }));
    try {
      await sendPushNotification(id);
      setSnackbar({
        open: true,
        message: 'Notification sent successfully',
        severity: 'success'
      });
      fetchNotifications();
      fetchStats();
    } catch (error) {
      console.error('Error sending notification:', error);
      setSnackbar({
        open: true,
        message: 'Failed to send notification',
        severity: 'error'
      });
    } finally {
      setSendLoading((prev) => ({ ...prev, [id]: false }));
    }
  };

  // Handle refresh button click
  const handleRefresh = () => {
    // Reset filters
    setSearchQuery('');
    setStatusFilter('all');
    setTypeFilter('all');
    setSortField('createdAt');
    setSortOrder('desc');
    setPage(0);
    
    // Fetch all data
    fetchAllData();
    
    setSnackbar({
      open: true,
      message: 'Data refreshed successfully',
      severity: 'success'
    });
  };

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return 'Not scheduled';
    const date = new Date(dateString);
    return date.toLocaleString();
  };

  // Close snackbar
  const handleCloseSnackbar = () => {
    setSnackbar({ ...snackbar, open: false });
  };

  // Render the MongoDB status dialog
  const renderMongoDBDialog = () => {
    if (!mongodbStatus) return null;
    
    // Check if we have error data
    if (mongodbStatus.success === false) {
      return (
        <Dialog
          open={mongodbDialogOpen}
          onClose={() => setMongodbDialogOpen(false)}
          maxWidth="md"
          fullWidth
        >
          <DialogTitle>MongoDB Connection Error</DialogTitle>
          <DialogContent>
            <Box sx={{ mb: 2 }}>
              <Typography variant="h6" color="error">Connection Failed</Typography>
              <Typography>
                Error: {mongodbStatus.error || mongodbStatus.message || 'Unknown error'}
              </Typography>
              <Typography sx={{ mt: 2 }}>
                The connection to MongoDB timed out or failed. This could be due to:
              </Typography>
              <ul>
                <li>MongoDB server is not running</li>
                <li>Network connectivity issues</li>
                <li>Incorrect MongoDB connection string</li>
                <li>Firewall blocking the connection</li>
              </ul>
              <Typography sx={{ mt: 2 }}>
                The application is currently using mock data instead of real database data.
              </Typography>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setMongodbDialogOpen(false)}>Close</Button>
            <Button 
              onClick={fetchAllData} 
              variant="contained" 
              color="primary"
              disabled={mongodbLoading}
            >
              {mongodbLoading ? <CircularProgress size={24} /> : 'Try Again'}
            </Button>
          </DialogActions>
        </Dialog>
      );
    }
    
    // Check if mongodb data exists
    if (!mongodbStatus.mongodb) {
      return (
        <Dialog
          open={mongodbDialogOpen}
          onClose={() => setMongodbDialogOpen(false)}
          maxWidth="md"
          fullWidth
        >
          <DialogTitle>Invalid MongoDB Data</DialogTitle>
          <DialogContent>
            <Typography color="error">
              The MongoDB diagnostic data is invalid or incomplete.
            </Typography>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setMongodbDialogOpen(false)}>Close</Button>
          </DialogActions>
        </Dialog>
      );
    }
    
    const { mongodb, models } = mongodbStatus;
    
    return (
      <Dialog
        open={mongodbDialogOpen}
        onClose={() => setMongodbDialogOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>MongoDB Connection Status</DialogTitle>
        <DialogContent>
          <Box sx={{ mb: 2 }}>
            <Typography variant="h6">Connection</Typography>
            <Typography>
              Status: <Chip 
                label={mongodb.connectionStateText || 'Unknown'} 
                color={mongodb.connectionState === 1 ? 'success' : 'error'} 
                size="small" 
              />
            </Typography>
            <Typography>Database: {mongodb.dbName || 'Unknown'}</Typography>
            <Typography>Host: {mongodb.dbHost || 'Unknown'}</Typography>
          </Box>
          
          {mongodb.collections && mongodb.collectionStatus && (
            <Box sx={{ mb: 2 }}>
              <Typography variant="h6">Collections</Typography>
              <TableContainer component={Paper} sx={{ maxHeight: 300 }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Collection</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Document Count</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {Object.keys(mongodb.collectionStatus).map(collection => (
                      <TableRow key={collection}>
                        <TableCell>{collection}</TableCell>
                        <TableCell>
                          {mongodb.collectionStatus[collection] ? 
                            <CheckIcon color="success" /> : 
                            <ClearIcon color="error" />
                          }
                        </TableCell>
                        <TableCell>{mongodb.documentCounts && mongodb.documentCounts[collection]}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          )}
          
          {models && (
            <Box>
              <Typography variant="h6">Models</Typography>
              <Typography>
                PushNotification Model: {models.pushNotification && models.pushNotification.exists ? 
                  <CheckIcon color="success" /> : 
                  <ClearIcon color="error" />
                }
              </Typography>
              <Typography>
                User Model: {models.user && models.user.exists ? 
                  <CheckIcon color="success" /> : 
                  <ClearIcon color="error" />
                }
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setMongodbDialogOpen(false)}>Close</Button>
          <Button 
            onClick={fetchAllData} 
            variant="contained" 
            color="primary"
            disabled={mongodbLoading}
          >
            {mongodbLoading ? <CircularProgress size={24} /> : 'Refresh'}
          </Button>
        </DialogActions>
      </Dialog>
    );
  };

  return (
    <Container maxWidth="xl">
      <Box mb={4}>
        <Typography variant="h4" gutterBottom>
          Push Notifications
        </Typography>
        <Typography variant="body1" color="textSecondary">
          Create and manage push notifications to users
        </Typography>
      </Box>

      {/* Mock Data Warning */}
      {usingMockData && (
        <Alert 
          severity="warning" 
          sx={{ mb: 3 }}
          icon={<WarningIcon />}
          action={
            <Button 
              color="inherit" 
              size="small" 
              onClick={fetchAllData}
              disabled={mongodbLoading}
            >
              {mongodbLoading ? <CircularProgress size={16} /> : 'Check Connection'}
            </Button>
          }
        >
          <AlertTitle>Using Demo Data</AlertTitle>
          MongoDB connection is unavailable. Showing demo data for preview purposes. 
          Real push notifications will not be sent or received until the database connection is restored.
          <Typography variant="caption" component="div" sx={{ mt: 1 }}>
            Possible causes: MongoDB server is not running, network connectivity issues, incorrect connection string, or firewall blocking the connection.
          </Typography>
        </Alert>
      )}

      {/* Action buttons */}
      <Box display="flex" justifyContent="space-between" mb={3}>
        <Box>
          <Tooltip title="Check MongoDB Connection">
            <IconButton 
              color="primary" 
              onClick={fetchAllData}
              disabled={mongodbLoading}
              sx={{ mr: 1 }}
            >
              {mongodbLoading ? <CircularProgress size={24} /> : <DataUsageIcon />}
            </IconButton>
          </Tooltip>
          <Button
            variant="outlined"
            color="secondary"
            onClick={() => navigate('/push-notifications/inactivity-settings')}
            sx={{ mr: 2 }}
          >
            Inactivity Notification Settings
          </Button>
          
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={handleRefresh}
          >
            Refresh
          </Button>
        </Box>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={3} mb={4}>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Total</Typography>
              <Typography variant="h3">{stats.total}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Sent</Typography>
              <Typography variant="h3">{stats.sent}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Pending</Typography>
              <Typography variant="h3">{stats.pending}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Success Rate</Typography>
              <Typography variant="h3">{stats.successRate}%</Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Topic Stats */}
      <Card sx={{ mb: 4 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Topics Distribution
          </Typography>
          <Divider sx={{ mb: 2 }} />
          <Grid container spacing={1}>
            {topicStats.map((topic) => (
              <Grid item key={topic.name}>
                <Chip 
                  label={`${topic.name}: ${topic.count}`} 
                  color="primary" 
                  variant="outlined" 
                  sx={{ m: 0.5 }}
                />
              </Grid>
            ))}
          </Grid>
        </CardContent>
      </Card>

      {/* Filters and Actions */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} md={4}>
            <TextField
              fullWidth
              variant="outlined"
              placeholder="Search notifications..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyPress={handleSearch}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
            />
          </Grid>
          <Grid item xs={12} md={2}>
            <FormControl fullWidth variant="outlined">
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                onChange={handleStatusFilterChange}
                label="Status"
              >
                <MenuItem value="all">All</MenuItem>
                <MenuItem value="sent">Sent</MenuItem>
                <MenuItem value="pending">Pending</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} md={2}>
            <FormControl fullWidth variant="outlined">
              <InputLabel>Type</InputLabel>
              <Select
                value={typeFilter}
                onChange={handleTypeFilterChange}
                label="Type"
              >
                <MenuItem value="all">All</MenuItem>
                <MenuItem value="BULK">Bulk</MenuItem>
                <MenuItem value="TOPIC">Topic</MenuItem>
                <MenuItem value="PERSONALIZED">Personalized</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} md={4}>
            <Stack direction="row" spacing={2} justifyContent="flex-end">
              <Button
                variant="outlined"
                startIcon={<RefreshIcon />}
                onClick={handleRefresh}
              >
                Refresh
              </Button>
              <Button
                variant="contained"
                color="primary"
                startIcon={<AddIcon />}
                onClick={() => navigate('/push-notifications/create')}
              >
                Create Notification
              </Button>
            </Stack>
          </Grid>
        </Grid>
      </Paper>

      {/* Notifications Table */}
      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell 
                  onClick={() => handleSortChange('title')}
                  style={{ cursor: 'pointer' }}
                >
                  Title
                  {sortField === 'title' && (
                    <FilterListIcon 
                      fontSize="small" 
                      style={{ 
                        transform: sortOrder === 'asc' ? 'rotate(180deg)' : 'none',
                        verticalAlign: 'middle'
                      }} 
                    />
                  )}
                </TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Target</TableCell>
                <TableCell>Audience Size</TableCell>
                <TableCell 
                  onClick={() => handleSortChange('scheduledAt')}
                  style={{ cursor: 'pointer' }}
                >
                  Schedule
                  {sortField === 'scheduledAt' && (
                    <FilterListIcon 
                      fontSize="small" 
                      style={{ 
                        transform: sortOrder === 'asc' ? 'rotate(180deg)' : 'none',
                        verticalAlign: 'middle'
                      }} 
                    />
                  )}
                </TableCell>
                <TableCell>Status</TableCell>
                <TableCell 
                  onClick={() => handleSortChange('createdAt')}
                  style={{ cursor: 'pointer' }}
                >
                  Created
                  {sortField === 'createdAt' && (
                    <FilterListIcon 
                      fontSize="small" 
                      style={{ 
                        transform: sortOrder === 'asc' ? 'rotate(180deg)' : 'none',
                        verticalAlign: 'middle'
                      }} 
                    />
                  )}
                </TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 3 }}>
                    <CircularProgress />
                  </TableCell>
                </TableRow>
              ) : notifications.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 3 }}>
                    <Typography variant="body1">No notifications found</Typography>
                    <Button 
                      variant="outlined" 
                      color="primary" 
                      size="small" 
                      onClick={() => navigate('/push-notifications/create')}
                      sx={{ mt: 1 }}
                    >
                      Create New Notification
                    </Button>
                  </TableCell>
                </TableRow>
              ) : (
                notifications.map((notification) => (
                  <TableRow key={notification._id}>
                    <TableCell>{notification.title}</TableCell>
                    <TableCell>
                      <Chip 
                        label={notification.type} 
                        color={
                          notification.type === 'BULK' ? 'error' : 
                          notification.type === 'TOPIC' ? 'primary' : 
                          'secondary'
                        }
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      {notification.type === 'TOPIC' ? (
                        <Tooltip title={notification.topics ? notification.topics.join(', ') : 'No topics'}>
                          <span>{notification.topics ? notification.topics.length : 0} topics</span>
                        </Tooltip>
                      ) : notification.type === 'PERSONALIZED' ? (
                        <Tooltip title="Targeted users">
                          <span>{notification.targetUsers ? notification.targetUsers.length : 0} users</span>
                        </Tooltip>
                      ) : (
                        'All Users'
                      )}
                    </TableCell>
                    <TableCell>{notification.audienceSize || 'N/A'}</TableCell>
                    <TableCell>{formatDate(notification.scheduledAt)}</TableCell>
                    <TableCell>
                      <Chip 
                        label={notification.isSent ? 'Sent' : 'Pending'} 
                        color={notification.isSent ? 'success' : 'warning'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>{notification.createdAt ? new Date(notification.createdAt).toLocaleDateString() : 'N/A'}</TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={1} justifyContent="flex-end">
                        {!notification.isSent && (
                          <Tooltip title="Send Now">
                            <IconButton 
                              color="primary"
                              onClick={() => handleSendNotification(notification._id)}
                              disabled={sendLoading[notification._id]}
                            >
                              {sendLoading[notification._id] ? (
                                <CircularProgress size={24} />
                              ) : (
                                <SendIcon />
                              )}
                            </IconButton>
                          </Tooltip>
                        )}
                        <Tooltip title="Edit">
                          <IconButton 
                            color="primary"
                            onClick={() => navigate(`/push-notifications/${notification._id}`)}
                          >
                            <EditIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete">
                          <IconButton 
                            color="error"
                            onClick={() => {
                              // Delete functionality will be implemented in detail view
                              navigate(`/push-notifications/${notification._id}`);
                            }}
                          >
                            <DeleteIcon />
                          </IconButton>
                        </Tooltip>
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          rowsPerPageOptions={[5, 10, 25]}
          component="div"
          count={totalNotifications}
          rowsPerPage={rowsPerPage}
          page={page}
          onPageChange={handleChangePage}
          onRowsPerPageChange={handleChangeRowsPerPage}
        />
      </Paper>

      {/* MongoDB status dialog */}
      {renderMongoDBDialog()}
      
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={handleCloseSnackbar} severity={snackbar.severity} sx={{ width: '100%' }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default PushNotifications;
 