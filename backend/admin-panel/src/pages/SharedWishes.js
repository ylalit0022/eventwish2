import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  IconButton,
  Chip,
  TextField,
  InputAdornment,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Grid,
  Alert,
  CircularProgress,
  Tooltip,
  Card,
  CardContent,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Divider
} from '@mui/material';
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Visibility as ViewsIcon,
  Share as ShareIcon,
  Email as EmailIcon,
  TrendingUp as TrendingUpIcon,
  DateRange as DateRangeIcon
} from '@mui/icons-material';
import { getSharedWishes, deleteSharedWish, getSharedWishAnalytics } from '../api';

const SharedWishes = () => {
  const navigate = useNavigate();
  const [sharedWishes, setSharedWishes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalItems, setTotalItems] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortField, setSortField] = useState('createdAt');
  const [sortOrder, setSortOrder] = useState('desc');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [wishToDelete, setWishToDelete] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [timeFilter, setTimeFilter] = useState('');
  const [analyticsData, setAnalyticsData] = useState(null);
  const [analyticsLoading, setAnalyticsLoading] = useState(false);

  // Fetch shared wishes
  const fetchSharedWishes = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const filters = {};
      if (searchQuery) {
        filters.q = searchQuery;
      }
      
      if (timeFilter) {
        filters.timeFilter = timeFilter;
      }
      
      const response = await getSharedWishes(
        page + 1,
        rowsPerPage,
        sortField,
        sortOrder,
        filters
      );
      
      if (response.success) {
        setSharedWishes(response.data);
        setTotalItems(response.totalItems);
      } else {
        throw new Error(response.message || 'Failed to fetch shared wishes');
      }
    } catch (err) {
      console.error('Error fetching shared wishes:', err);
      setError('Failed to load shared wishes: ' + (err.message || 'Unknown error'));
    } finally {
      setLoading(false);
    }
  };

  // Fetch analytics data
  const fetchAnalytics = async () => {
    try {
      setAnalyticsLoading(true);
      
      const filters = {};
      if (timeFilter) {
        filters.timeFilter = timeFilter;
      }
      
      const response = await getSharedWishAnalytics(filters);
      
      if (response.success) {
        setAnalyticsData(response.analytics);
      } else {
        console.error('Failed to fetch analytics:', response.message);
      }
    } catch (err) {
      console.error('Error fetching analytics:', err);
    } finally {
      setAnalyticsLoading(false);
    }
  };

  // Initial fetch
  useEffect(() => {
    fetchSharedWishes();
    fetchAnalytics();
  }, [page, rowsPerPage, sortField, sortOrder, searchQuery, timeFilter]);

  // Handle page change
  const handleChangePage = (event, newPage) => {
    setPage(newPage);
  };

  // Handle rows per page change
  const handleChangeRowsPerPage = (event) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  // Handle search
  const handleSearchChange = (event) => {
    setSearchQuery(event.target.value);
    setPage(0);
  };

  // Handle time filter change
  const handleTimeFilterChange = (event) => {
    setTimeFilter(event.target.value);
    setPage(0);
  };

  // Handle sort change
  const handleSortChange = (field) => {
    if (field === sortField) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('asc');
    }
    setPage(0);
  };

  // Handle delete dialog
  const handleDeleteClick = (wish) => {
    setWishToDelete(wish);
    setDeleteDialogOpen(true);
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
    setWishToDelete(null);
  };

  const handleDeleteConfirm = async () => {
    if (!wishToDelete) return;
    
    try {
      setDeleteLoading(true);
      const response = await deleteSharedWish(wishToDelete._id);
      
      if (response.success) {
        // Remove from list
        setSharedWishes(sharedWishes.filter(w => w._id !== wishToDelete._id));
        setDeleteDialogOpen(false);
        setWishToDelete(null);
        
        // Refresh analytics
        fetchAnalytics();
      } else {
        throw new Error(response.message || 'Failed to delete shared wish');
      }
    } catch (err) {
      console.error(`Error deleting shared wish ${wishToDelete._id}:`, err);
      setError('Failed to delete shared wish: ' + (err.message || 'Unknown error'));
    } finally {
      setDeleteLoading(false);
    }
  };

  // Handle refresh
  const handleRefresh = () => {
    fetchSharedWishes();
    fetchAnalytics();
  };

  // Handle edit click
  const handleEditClick = (id) => {
    navigate(`/shared-wishes/${id}`);
  };

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString();
  };

  // Truncate text
  const truncateText = (text, maxLength = 30) => {
    if (!text) return 'N/A';
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  };

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" component="h1">
          Shared Wishes
        </Typography>
        <Box>
          <Button
            variant="contained"
            color="primary"
            startIcon={<RefreshIcon />}
            onClick={handleRefresh}
            sx={{ ml: 1 }}
          >
            Refresh
          </Button>
        </Box>
      </Box>

      {/* Error alert */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Analytics Dashboard */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">Analytics Dashboard</Typography>
          <FormControl variant="outlined" size="small" sx={{ minWidth: 150 }}>
            <InputLabel>Time Period</InputLabel>
            <Select
              value={timeFilter}
              onChange={handleTimeFilterChange}
              label="Time Period"
            >
              <MenuItem value="">All Time</MenuItem>
              <MenuItem value="today">Today</MenuItem>
              <MenuItem value="yesterday">Yesterday</MenuItem>
              <MenuItem value="last7days">Last 7 Days</MenuItem>
              <MenuItem value="last30days">Last 30 Days</MenuItem>
              <MenuItem value="thisMonth">This Month</MenuItem>
              <MenuItem value="lastMonth">Last Month</MenuItem>
            </Select>
          </FormControl>
        </Box>
        
        {analyticsLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
            <CircularProgress />
          </Box>
        ) : (
          <>
            <Grid container spacing={3}>
              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="subtitle2" color="textSecondary">Total Shares</Typography>
                    <Typography variant="h4">{analyticsData?.totalShares || 0}</Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="subtitle2" color="textSecondary">Total Views</Typography>
                    <Typography variant="h4">{analyticsData?.totalViews || 0}</Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="subtitle2" color="textSecondary">Unique Views</Typography>
                    <Typography variant="h4">{analyticsData?.totalUniqueViews || 0}</Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Card>
                  <CardContent>
                    <Typography variant="subtitle2" color="textSecondary">Avg. Views per Share</Typography>
                    <Typography variant="h4">
                      {analyticsData?.totalShares ? (analyticsData.totalViews / analyticsData.totalShares).toFixed(1) : '0'}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>
            
            <Divider sx={{ my: 2 }} />
            
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle1" gutterBottom>Top Templates</Typography>
                {analyticsData?.topTemplates?.length > 0 ? (
                  <TableContainer>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Template</TableCell>
                          <TableCell align="right">Shares</TableCell>
                          <TableCell align="right">Views</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {analyticsData.topTemplates.map((template) => (
                          <TableRow key={template.templateId}>
                            <TableCell>{truncateText(template.title || 'Unknown', 20)}</TableCell>
                            <TableCell align="right">{template.shareCount}</TableCell>
                            <TableCell align="right">{template.viewCount}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                ) : (
                  <Typography variant="body2">No template data available</Typography>
                )}
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle1" gutterBottom>Sharing by Platform</Typography>
                {analyticsData?.sharingByPlatform?.length > 0 ? (
                  <TableContainer>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Platform</TableCell>
                          <TableCell align="right">Count</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {analyticsData.sharingByPlatform.map((platform) => (
                          <TableRow key={platform.platform}>
                            <TableCell>{platform.platform || 'Unknown'}</TableCell>
                            <TableCell align="right">{platform.count}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                ) : (
                  <Typography variant="body2">No platform data available</Typography>
                )}
              </Grid>
            </Grid>
          </>
        )}
      </Paper>

      {/* Filters */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label="Search"
              variant="outlined"
              value={searchQuery}
              onChange={handleSearchChange}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
              placeholder="Search by short code, recipient, sender..."
            />
          </Grid>
        </Grid>
      </Paper>

      {/* Table */}
      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell 
                  onClick={() => handleSortChange('shortCode')}
                  sx={{ cursor: 'pointer' }}
                >
                  Short Code
                  {sortField === 'shortCode' && (
                    <span>{sortOrder === 'asc' ? ' ↑' : ' ↓'}</span>
                  )}
                </TableCell>
                <TableCell>Title</TableCell>
                <TableCell>Recipient</TableCell>
                <TableCell>Sender</TableCell>
                <TableCell>Email</TableCell>
                <TableCell 
                  onClick={() => handleSortChange('views')}
                  sx={{ cursor: 'pointer' }}
                >
                  Views
                  {sortField === 'views' && (
                    <span>{sortOrder === 'asc' ? ' ↑' : ' ↓'}</span>
                  )}
                </TableCell>
                <TableCell 
                  onClick={() => handleSortChange('createdAt')}
                  sx={{ cursor: 'pointer' }}
                >
                  Created
                  {sortField === 'createdAt' && (
                    <span>{sortOrder === 'asc' ? ' ↑' : ' ↓'}</span>
                  )}
                </TableCell>
                <TableCell>Shared Via</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={9} align="center">
                    <CircularProgress />
                  </TableCell>
                </TableRow>
              ) : sharedWishes.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={9} align="center">
                    No shared wishes found
                  </TableCell>
                </TableRow>
              ) : (
                sharedWishes.map((wish) => (
                  <TableRow key={wish._id}>
                    <TableCell>{wish.shortCode}</TableCell>
                    <TableCell>{truncateText(wish.title)}</TableCell>
                    <TableCell>{wish.recipientName}</TableCell>
                    <TableCell>{wish.senderName}</TableCell>
                    <TableCell>
                      {wish.viewerEngagement && wish.viewerEngagement.length > 0 && 
                       wish.viewerEngagement[0].userId && 
                       wish.viewerEngagement[0].userId.email ? (
                        <Chip 
                          icon={<EmailIcon />} 
                          label={truncateText(wish.viewerEngagement[0].userId.email, 20)} 
                          size="small" 
                          color="primary" 
                          variant="outlined" 
                        />
                      ) : (
                        'N/A'
                      )}
                    </TableCell>
                    <TableCell>
                      <Tooltip title={`${wish.uniqueViews} unique views`}>
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                          <ViewsIcon fontSize="small" sx={{ mr: 1 }} />
                          {wish.views}
                        </Box>
                      </Tooltip>
                    </TableCell>
                    <TableCell>{formatDate(wish.createdAt)}</TableCell>
                    <TableCell>
                      <Chip 
                        icon={<ShareIcon />} 
                        label={wish.sharedVia} 
                        size="small" 
                        color="primary" 
                        variant="outlined" 
                      />
                    </TableCell>
                    <TableCell>
                      <IconButton
                        color="primary"
                        onClick={() => handleEditClick(wish._id)}
                        size="small"
                      >
                        <EditIcon />
                      </IconButton>
                      <IconButton
                        color="error"
                        onClick={() => handleDeleteClick(wish)}
                        size="small"
                      >
                        <DeleteIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
        
        {/* Pagination */}
        <TablePagination
          rowsPerPageOptions={[5, 10, 25, 50]}
          component="div"
          count={totalItems}
          rowsPerPage={rowsPerPage}
          page={page}
          onPageChange={handleChangePage}
          onRowsPerPageChange={handleChangeRowsPerPage}
        />
      </Paper>

      {/* Delete Dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
      >
        <DialogTitle>Delete Shared Wish</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete the shared wish with short code{' '}
            <strong>{wishToDelete?.shortCode}</strong>?
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

export default SharedWishes;
