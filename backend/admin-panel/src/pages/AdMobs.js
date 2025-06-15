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
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  Tooltip
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  MonetizationOn as AdIcon
} from '@mui/icons-material';
import { getAdMobs, deleteAdMob, toggleAdMobStatus } from '../api';

const AdMobs = () => {
  const navigate = useNavigate();
  const [adMobs, setAdMobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalItems, setTotalItems] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [adTypeFilter, setAdTypeFilter] = useState('');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [adMobToDelete, setAdMobToDelete] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [sortField, setSortField] = useState('createdAt');
  const [sortOrder, setSortOrder] = useState('desc');
  const [statusLoading, setStatusLoading] = useState({});
  const [adTypes, setAdTypes] = useState([]);

  // Fetch AdMob ads
  const fetchAdMobs = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const filters = {};
      if (searchQuery) {
        filters.q = searchQuery;
      }
      if (adTypeFilter) {
        filters.adType = adTypeFilter;
      }
      
      const response = await getAdMobs(
        page + 1,
        rowsPerPage,
        sortField,
        sortOrder,
        filters
      );
      
      if (response.success) {
        setAdMobs(response.data);
        setTotalItems(response.totalItems);
        setAdTypes(response.adTypes || []);
      } else {
        throw new Error(response.message || 'Failed to fetch AdMob ads');
      }
    } catch (err) {
      console.error('Error fetching AdMob ads:', err);
      setError('Failed to load AdMob ads: ' + (err.message || 'Unknown error'));
    } finally {
      setLoading(false);
    }
  };

  // Initial fetch
  useEffect(() => {
    fetchAdMobs();
  }, [page, rowsPerPage, sortField, sortOrder, searchQuery, adTypeFilter]);

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

  // Handle ad type filter change
  const handleAdTypeFilterChange = (event) => {
    setAdTypeFilter(event.target.value);
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
  const handleDeleteClick = (adMob) => {
    // Make sure we have a valid ID before showing delete dialog
    if (!adMob || (!adMob._id && !adMob.id)) {
      setError('Invalid AdMob ID. Cannot delete this ad.');
      return;
    }
    
    setAdMobToDelete(adMob);
    setDeleteDialogOpen(true);
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
    setAdMobToDelete(null);
  };

  const handleDeleteConfirm = async () => {
    if (!adMobToDelete) return;
    
    const adId = adMobToDelete._id || adMobToDelete.id;
    
    if (!adId) {
      setError('Invalid AdMob ID. Cannot delete this ad.');
      setDeleteDialogOpen(false);
      return;
    }
    
    try {
      setDeleteLoading(true);
      const response = await deleteAdMob(adId);
      
      if (response.success) {
        // Remove from list
        setAdMobs(adMobs.filter(ad => {
          const currentId = ad._id || ad.id;
          return currentId !== adId;
        }));
        setDeleteDialogOpen(false);
        setAdMobToDelete(null);
      } else {
        throw new Error(response.message || 'Failed to delete AdMob ad');
      }
    } catch (err) {
      console.error(`Error deleting AdMob ad ${adId}:`, err);
      setError('Failed to delete AdMob ad: ' + (err.message || 'Unknown error'));
    } finally {
      setDeleteLoading(false);
    }
  };

  // Handle status toggle
  const handleStatusToggle = async (id) => {
    try {
      // Validate ID
      if (!id) {
        setError('Invalid AdMob ID');
        return;
      }
      
      console.log('Toggling status for AdMob ID:', id);
      
      // Set loading state for this specific ad
      setStatusLoading(prev => ({ ...prev, [id]: true }));
      
      // Call API to toggle status
      const response = await toggleAdMobStatus(id);
      
      if (response.success) {
        // Update ad status in the local state
        setAdMobs(prevAdMobs => 
          prevAdMobs.map(ad => {
            const adId = ad._id || ad.id;
            return adId === id 
              ? { ...ad, status: response.adMob.status } 
              : ad;
          })
        );
        
        // Clear any previous errors
        setError(null);
      } else {
        throw new Error(response.message || 'Failed to toggle AdMob status');
      }
    } catch (err) {
      console.error(`Error toggling AdMob status ${id}:`, err);
      setError('Failed to toggle AdMob status: ' + (err.message || 'Unknown error'));
    } finally {
      // Always clear loading state
      setStatusLoading(prev => ({ ...prev, [id]: false }));
    }
  };

  // Handle refresh
  const handleRefresh = () => {
    fetchAdMobs();
  };

  // Handle create
  const handleCreateClick = () => {
    navigate('/admob/create');
  };

  // Handle edit
  const handleEditClick = (id) => {
    console.log('Edit button clicked with ID:', id, typeof id);
    
    if (!id || id === 'undefined' || id === 'null') {
      console.error('Invalid AdMob ID in handleEditClick');
      setError('Invalid AdMob ID. Cannot edit this ad.');
      return;
    }
    
    // Simple navigation - similar to how Templates.js does it
    navigate(`/admob/${id}`);
  };

  if (loading && adMobs.length === 0) {
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
            AdMob Ads
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
              label="Search AdMob Ads"
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
          <Grid item xs={12} sm={6} md={3}>
            <FormControl fullWidth>
              <InputLabel>Ad Type</InputLabel>
              <Select
                value={adTypeFilter}
                label="Ad Type"
                onChange={handleAdTypeFilterChange}
              >
                <MenuItem value="">All Types</MenuItem>
                {adTypes.map((type) => (
                  <MenuItem key={type} value={type}>
                    {type}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      </Paper>

      {/* AdMob ads table */}
      <Paper sx={{ width: '100%', overflow: 'hidden' }}>
        <TableContainer>
          <Table stickyHeader aria-label="AdMob ads table">
            <TableHead>
              <TableRow>
                <TableCell 
                  onClick={() => handleSortChange('adName')}
                  sx={{ cursor: 'pointer' }}
                >
                  Ad Name
                  {sortField === 'adName' && (
                    <span>{sortOrder === 'asc' ? ' ▲' : ' ▼'}</span>
                  )}
                </TableCell>
                <TableCell 
                  onClick={() => handleSortChange('adType')}
                  sx={{ cursor: 'pointer' }}
                >
                  Ad Type
                  {sortField === 'adType' && (
                    <span>{sortOrder === 'asc' ? ' ▲' : ' ▼'}</span>
                  )}
                </TableCell>
                <TableCell>Ad Unit Code</TableCell>
                <TableCell align="center">Status</TableCell>
                <TableCell align="center">Impressions</TableCell>
                <TableCell align="center">Clicks</TableCell>
                <TableCell align="center">CTR</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {adMobs.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} align="center">
                    No AdMob ads found
                  </TableCell>
                </TableRow>
              ) : (
                adMobs.map((ad) => (
                  <TableRow key={ad._id || ad.id} hover>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <AdIcon sx={{ mr: 1, color: 'primary.main' }} />
                        {ad.adName}
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip 
                        label={ad.adType} 
                        color="primary" 
                        variant="outlined" 
                        size="small" 
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                        {ad.adUnitCode}
                      </Typography>
                    </TableCell>
                    <TableCell align="center">
                      {(ad._id || ad.id) ? (
                        <>
                          <Tooltip title={ad.status ? 'Active' : 'Inactive'}>
                            <Box component="span">
                              <Switch
                                checked={ad.status}
                                onChange={() => handleStatusToggle(ad._id || ad.id)}
                                disabled={statusLoading[ad._id || ad.id]}
                                color="primary"
                              />
                            </Box>
                          </Tooltip>
                          {statusLoading[ad._id || ad.id] && (
                            <CircularProgress size={24} sx={{ ml: 1 }} />
                          )}
                        </>
                      ) : (
                        <Typography color="error" variant="body2">Invalid ID</Typography>
                      )}
                    </TableCell>
                    <TableCell align="center">{ad.impressions}</TableCell>
                    <TableCell align="center">{ad.clicks}</TableCell>
                    <TableCell align="center">{ad.ctr ? `${ad.ctr.toFixed(2)}%` : '0.00%'}</TableCell>
                    <TableCell align="right">
                      {(ad._id || ad.id) ? (
                        <>
                          <IconButton
                            color="primary"
                            onClick={() => handleEditClick(ad._id || ad.id)}
                          >
                            <EditIcon />
                          </IconButton>
                          <IconButton
                            color="error"
                            onClick={() => handleDeleteClick(ad)}
                          >
                            <DeleteIcon />
                          </IconButton>
                        </>
                      ) : (
                        <Typography color="error" variant="body2">Invalid ID</Typography>
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
        <DialogTitle>Delete AdMob Ad</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete the AdMob ad "{adMobToDelete?.adName}"? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel} disabled={deleteLoading}>
            Cancel
          </Button>
          <Button 
            onClick={handleDeleteConfirm} 
            color="error" 
            variant="contained"
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

export default AdMobs; 