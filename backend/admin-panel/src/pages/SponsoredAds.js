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
  TextField,
  InputAdornment,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Switch,
  Grid,
  Alert,
  CircularProgress,
  Chip,
  Tooltip,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TableSortLabel
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Link as LinkIcon,
  Image as ImageIcon
} from '@mui/icons-material';
import { getSponsoredAds, deleteSponsoredAd, toggleSponsoredAdStatus } from '../api';

const SponsoredAds = () => {
  const navigate = useNavigate();
  const [ads, setAds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalItems, setTotalItems] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [locationFilter, setLocationFilter] = useState('');
  const [sortField, setSortField] = useState('createdAt');
  const [sortOrder, setSortOrder] = useState('desc');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [adToDelete, setAdToDelete] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [statusLoading, setStatusLoading] = useState({});
  const [successMessage, setSuccessMessage] = useState('');

  // Location options based on enum in SponsoredAd model
  const locationOptions = [
    'home_top',
    'home_bottom',
    'category_below',
    'details_top',
    'details_bottom'
  ];

  // Format location display
  const formatLocation = (location) => {
    return location
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  };

  // Fetch sponsored ads
  const fetchAds = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const filters = {};
      if (searchQuery) {
        filters.q = searchQuery;
      }
      if (locationFilter) {
        filters.location = locationFilter;
      }
      
      console.log('Fetching sponsored ads with params:', {
        page: page + 1,
        rowsPerPage,
        sortField,
        sortOrder,
        filters
      });
      
      // Check if we're in dev mode
      const isDevelopment = window.location.hostname === 'localhost' || 
                           window.location.hostname === '127.0.0.1';
      
      // Enable dev mode in local storage if in development environment
      if (isDevelopment && localStorage.getItem('devMode') !== 'true') {
        console.log('Enabling dev mode for authentication');
        localStorage.setItem('devMode', 'true');
      }
      
      const response = await getSponsoredAds(
        page + 1,
        rowsPerPage,
        sortField,
        sortOrder,
        filters
      );
      
      console.log('Sponsored ads API response:', response);
      
      if (response && response.success) {
        console.log('Setting ads state with:', response.sponsoredAds || []);
        setAds(response.sponsoredAds || []);
        setTotalItems(response.pagination?.total || 0);
      } else if (response) {
        throw new Error(response.message || 'Failed to fetch sponsored ads');
      } else {
        throw new Error('No response received from the server');
      }
    } catch (err) {
      console.error('Error fetching sponsored ads:', err);
      
      // Handle authentication errors
      if (err.response && err.response.status === 401) {
        setError('Authentication error: Please log in again to continue');
      } else if (err.response && err.response.status === 403) {
        setError('Permission denied: You do not have access to view sponsored ads');
      } else {
        setError('Failed to load sponsored ads: ' + (err.message || 'Unknown error'));
      }
      
      // Set empty ads to avoid infinite loading
      setAds([]);
      setTotalItems(0);
    } finally {
      setLoading(false);
    }
  };

  // Initial fetch
  useEffect(() => {
    fetchAds();
  }, [page, rowsPerPage, sortField, sortOrder, searchQuery, locationFilter]);

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

  // Handle location filter change
  const handleLocationFilterChange = (event) => {
    setLocationFilter(event.target.value);
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
  const handleDeleteClick = (ad) => {
    setAdToDelete(ad);
    setDeleteDialogOpen(true);
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
    setAdToDelete(null);
  };

  const handleDeleteConfirm = async () => {
    if (!adToDelete) return;
    
    try {
      setDeleteLoading(true);
      const response = await deleteSponsoredAd(adToDelete.id);
      
      if (response.success) {
        // Remove from list
        setAds(ads.filter(ad => ad.id !== adToDelete.id));
        setDeleteDialogOpen(false);
        setAdToDelete(null);
        setSuccessMessage('Sponsored ad deleted successfully');
        
        // Hide success message after 3 seconds
        setTimeout(() => {
          setSuccessMessage('');
        }, 3000);
      } else {
        throw new Error(response.message || 'Failed to delete sponsored ad');
      }
    } catch (err) {
      console.error(`Error deleting sponsored ad ${adToDelete.id}:`, err);
      setError('Failed to delete sponsored ad: ' + (err.message || 'Unknown error'));
    } finally {
      setDeleteLoading(false);
    }
  };

  // Handle status toggle
  const handleStatusToggle = async (id) => {
    try {
      setStatusLoading(prev => ({ ...prev, [id]: true }));
      
      const response = await toggleSponsoredAdStatus(id);
      
      if (response.success) {
        // Update ad status in the list
        setAds(ads.map(ad => 
          ad.id === id 
            ? { ...ad, status: response.status } 
            : ad
        ));
        
        setSuccessMessage('Status updated successfully');
        
        // Hide success message after 3 seconds
        setTimeout(() => {
          setSuccessMessage('');
        }, 3000);
      } else {
        throw new Error(response.message || 'Failed to toggle sponsored ad status');
      }
    } catch (err) {
      console.error(`Error toggling sponsored ad status ${id}:`, err);
      setError('Failed to toggle sponsored ad status: ' + (err.message || 'Unknown error'));
    } finally {
      setStatusLoading(prev => ({ ...prev, [id]: false }));
    }
  };

  // Handle refresh
  const handleRefresh = () => {
    fetchAds();
  };

  // Handle create
  const handleCreateClick = () => {
    navigate('/sponsored-ads/create');
  };

  // Handle edit
  const handleEditClick = (id) => {
    navigate(`/sponsored-ads/${id}`);
  };

  // Render loading state
  if (loading && ads.length === 0) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ flexGrow: 1, p: 3 }}>
      {/* Header */}
      <Grid container spacing={2} alignItems="center" sx={{ mb: 3 }}>
        <Grid item xs>
          <Typography variant="h4" component="h1">
            Sponsored Ads
          </Typography>
        </Grid>
        <Grid item>
          <Button
            variant="contained"
            color="primary"
            startIcon={<AddIcon />}
            onClick={handleCreateClick}
          >
            Create Ad
          </Button>
        </Grid>
        <Grid item>
          <IconButton onClick={handleRefresh} color="primary">
            <RefreshIcon />
          </IconButton>
        </Grid>
      </Grid>

      {/* Success message */}
      {successMessage && (
        <Alert severity="success" sx={{ mb: 3 }}>
          {successMessage}
        </Alert>
      )}

      {/* Error message */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Filters */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={6} md={4}>
            <TextField
              fullWidth
              label="Search Ads"
              value={searchQuery}
              onChange={handleSearchChange}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={4}>
            <FormControl fullWidth>
              <InputLabel>Location</InputLabel>
              <Select
                value={locationFilter}
                label="Location"
                onChange={handleLocationFilterChange}
              >
                <MenuItem value="">All Locations</MenuItem>
                {locationOptions.map((location) => (
                  <MenuItem key={location} value={location}>
                    {formatLocation(location)}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      </Paper>

      {/* Ads table */}
      <Paper>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell width="5%">#</TableCell>
                <TableCell width="15%">
                  <TableSortLabel
                    active={sortField === 'title'}
                    direction={sortField === 'title' ? sortOrder : 'asc'}
                    onClick={() => handleSortChange('title')}
                  >
                    Title
                  </TableSortLabel>
                </TableCell>
                <TableCell width="15%">Image</TableCell>
                <TableCell width="10%">
                  <TableSortLabel
                    active={sortField === 'location'}
                    direction={sortField === 'location' ? sortOrder : 'asc'}
                    onClick={() => handleSortChange('location')}
                  >
                    Location
                  </TableSortLabel>
                </TableCell>
                <TableCell width="10%">
                  <TableSortLabel
                    active={sortField === 'priority'}
                    direction={sortField === 'priority' ? sortOrder : 'asc'}
                    onClick={() => handleSortChange('priority')}
                  >
                    Priority
                  </TableSortLabel>
                </TableCell>
                <TableCell width="10%">Status</TableCell>
                <TableCell width="15%">
                  <TableSortLabel
                    active={sortField === 'createdAt'}
                    direction={sortField === 'createdAt' ? sortOrder : 'asc'}
                    onClick={() => handleSortChange('createdAt')}
                  >
                    Created
                  </TableSortLabel>
                </TableCell>
                <TableCell width="20%" align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {ads.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} align="center">
                    No sponsored ads found.
                  </TableCell>
                </TableRow>
              ) : (
                ads.map((ad, index) => (
                  <TableRow key={ad.id}>
                    <TableCell>{page * rowsPerPage + index + 1}</TableCell>
                    <TableCell>
                      <Typography variant="body2" noWrap>
                        {ad.title}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      {ad.image_url ? (
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                          <Box
                            component="img"
                            sx={{ width: 60, height: 40, objectFit: 'cover', borderRadius: 1, mr: 1 }}
                            src={ad.image_url}
                            alt={ad.title}
                            onError={(e) => { e.target.src = 'https://via.placeholder.com/60x40?text=Error'; }}
                          />
                          <IconButton 
                            size="small" 
                            color="primary" 
                            component="a" 
                            href={ad.image_url} 
                            target="_blank"
                          >
                            <ImageIcon fontSize="small" />
                          </IconButton>
                        </Box>
                      ) : (
                        <Typography variant="body2" color="text.secondary">
                          No image
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      <Chip 
                        label={formatLocation(ad.location)} 
                        size="small" 
                        color="primary" 
                        variant="outlined" 
                      />
                    </TableCell>
                    <TableCell>{ad.priority || 0}</TableCell>
                    <TableCell>
                      <Switch
                        checked={ad.status}
                        onChange={() => handleStatusToggle(ad.id)}
                        disabled={statusLoading[ad.id]}
                        color={ad.status ? 'success' : 'default'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      {new Date(ad.createdAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell align="right">
                      <IconButton 
                        color="primary" 
                        size="small"
                        onClick={() => handleEditClick(ad.id)}
                      >
                        <EditIcon fontSize="small" />
                      </IconButton>
                      <IconButton 
                        color="error" 
                        size="small"
                        onClick={() => handleDeleteClick(ad)}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                      {ad.redirect_url && (
                        <IconButton 
                          color="info" 
                          size="small" 
                          component="a" 
                          href={ad.redirect_url} 
                          target="_blank"
                        >
                          <LinkIcon fontSize="small" />
                        </IconButton>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
        
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

      {/* Delete confirmation dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
      >
        <DialogTitle>Delete Sponsored Ad</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete the sponsored ad "{adToDelete?.title}"? This action cannot be undone.
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
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SponsoredAds; 