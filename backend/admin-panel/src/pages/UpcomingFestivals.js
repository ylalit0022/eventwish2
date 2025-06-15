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
  Switch,
  Tooltip,
  FormControl,
  InputLabel,
  Select,
  MenuItem as SelectItem,
  Snackbar
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Celebration as FestivalIcon
} from '@mui/icons-material';
import { getFestivals, deleteFestival, toggleFestivalStatus } from '../api';
import { format } from 'date-fns';

const UpcomingFestivals = () => {
  const navigate = useNavigate();
  const [festivals, setFestivals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalItems, setTotalItems] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [festivalToDelete, setFestivalToDelete] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [sortField, setSortField] = useState('date');
  const [sortOrder, setSortOrder] = useState('asc');
  const [statusLoading, setStatusLoading] = useState({});
  const [statusFilter, setStatusFilter] = useState('');
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success'
  });

  // Fetch festivals
  const fetchFestivals = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const filters = {};
      if (searchQuery) {
        filters.q = searchQuery;
      }
      
      if (statusFilter) {
        filters.status = statusFilter;
      }
      
      const response = await getFestivals(
        page + 1,
        rowsPerPage,
        sortField,
        sortOrder,
        filters
      );
      
      if (response.success) {
        setFestivals(response.data);
        setTotalItems(response.totalItems);
      } else {
        throw new Error(response.message || 'Failed to fetch festivals');
      }
    } catch (err) {
      console.error('Error fetching festivals:', err);
      setError('Failed to load festivals: ' + (err.message || 'Unknown error'));
    } finally {
      setLoading(false);
    }
  };

  // Initial fetch
  useEffect(() => {
    fetchFestivals();
  }, [page, rowsPerPage, sortField, sortOrder, searchQuery, statusFilter]);

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

  // Handle status filter change
  const handleStatusFilterChange = (event) => {
    setStatusFilter(event.target.value);
    setPage(0);
  };

  // Handle delete dialog
  const handleDeleteClick = (festival) => {
    setFestivalToDelete(festival);
    setDeleteDialogOpen(true);
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
    setFestivalToDelete(null);
  };

  const handleDeleteConfirm = async () => {
    if (!festivalToDelete) return;
    
    try {
      setDeleteLoading(true);
      const response = await deleteFestival(festivalToDelete._id);
      
      if (response.success) {
        // Remove from list
        setFestivals(festivals.filter(festival => festival._id !== festivalToDelete._id));
        setDeleteDialogOpen(false);
        setFestivalToDelete(null);
        
        // Show success message
        setSnackbar({
          open: true,
          message: 'Festival deleted successfully',
          severity: 'success'
        });
      } else {
        throw new Error(response.message || 'Failed to delete festival');
      }
    } catch (err) {
      console.error(`Error deleting festival ${festivalToDelete._id}:`, err);
      setError('Failed to delete festival: ' + (err.message || 'Unknown error'));
      
      // Show error message
      setSnackbar({
        open: true,
        message: `Failed to delete festival: ${err.message || 'Unknown error'}`,
        severity: 'error'
      });
    } finally {
      setDeleteLoading(false);
    }
  };

  // Handle status toggle
  const handleStatusToggle = async (id) => {
    try {
      if (!id) {
        console.error("handleStatusToggle called with undefined ID");
        setError('Failed to toggle status: Invalid festival ID');
        return;
      }
      
      setStatusLoading(prev => ({ ...prev, [id]: true }));
      
      const response = await toggleFestivalStatus(id);
      
      if (response.success) {
        // Update festival status in the list
        setFestivals(festivals.map(festival => 
          festival._id === id ? { ...festival, isActive: response.festival.isActive } : festival
        ));
        
        // Show success message
        setSnackbar({
          open: true,
          message: `Festival ${response.festival.isActive ? 'activated' : 'deactivated'} successfully`,
          severity: 'success'
        });
      } else {
        throw new Error(response.message || 'Failed to toggle status');
      }
    } catch (err) {
      console.error(`Error toggling festival status ${id}:`, err);
      setError('Failed to toggle status: ' + (err.message || 'Unknown error'));
      
      // Show error message
      setSnackbar({
        open: true,
        message: `Failed to toggle status: ${err.message || 'Unknown error'}`,
        severity: 'error'
      });
    } finally {
      setStatusLoading(prev => ({ ...prev, [id]: false }));
    }
  };

  // Handle refresh
  const handleRefresh = () => {
    fetchFestivals();
  };

  // Handle create click
  const handleCreateClick = () => {
    navigate('/upcoming-festivals/create');
  };

  // Handle edit click
  const handleEditClick = (id) => {
    if (!id) {
      console.error("handleEditClick called with undefined ID");
      setError('Failed to edit: Invalid festival ID');
      return;
    }
    
    navigate(`/upcoming-festivals/${id}`);
  };

  // Handle snackbar close
  const handleSnackbarClose = (event, reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setSnackbar({ ...snackbar, open: false });
  };

  // Format date for display
  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    try {
      return format(new Date(dateString), 'MMM dd, yyyy');
    } catch (error) {
      return 'Invalid Date';
    }
  };

  // Get status chip color
  const getStatusColor = (status) => {
    switch (status) {
      case 'UPCOMING':
        return 'primary';
      case 'ONGOING':
        return 'success';
      case 'ENDED':
        return 'error';
      default:
        return 'default';
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Upcoming Festivals
        </Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={handleCreateClick}
        >
          Add Festival
        </Button>
      </Box>

      {/* Filters and search */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={6} md={4}>
            <TextField
              fullWidth
              label="Search Festivals"
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
            />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <FormControl fullWidth variant="outlined">
              <InputLabel id="status-filter-label">Status</InputLabel>
              <Select
                labelId="status-filter-label"
                id="status-filter"
                value={statusFilter}
                onChange={handleStatusFilterChange}
                label="Status"
              >
                <SelectItem value="">All</SelectItem>
                <SelectItem value="UPCOMING">Upcoming</SelectItem>
                <SelectItem value="ONGOING">Ongoing</SelectItem>
                <SelectItem value="ENDED">Ended</SelectItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <Button
              fullWidth
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={handleRefresh}
            >
              Refresh
            </Button>
          </Grid>
        </Grid>
      </Paper>

      {/* Error alert */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Festivals table */}
      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell 
                  onClick={() => handleSortChange('name')}
                  sx={{ cursor: 'pointer', fontWeight: 'bold' }}
                >
                  Name {sortField === 'name' && (sortOrder === 'asc' ? '↑' : '↓')}
                </TableCell>
                <TableCell 
                  onClick={() => handleSortChange('date')}
                  sx={{ cursor: 'pointer', fontWeight: 'bold' }}
                >
                  Date {sortField === 'date' && (sortOrder === 'asc' ? '↑' : '↓')}
                </TableCell>
                <TableCell 
                  onClick={() => handleSortChange('category')}
                  sx={{ cursor: 'pointer', fontWeight: 'bold' }}
                >
                  Category {sortField === 'category' && (sortOrder === 'asc' ? '↑' : '↓')}
                </TableCell>
                <TableCell 
                  onClick={() => handleSortChange('status')}
                  sx={{ cursor: 'pointer', fontWeight: 'bold' }}
                >
                  Status {sortField === 'status' && (sortOrder === 'asc' ? '↑' : '↓')}
                </TableCell>
                <TableCell 
                  onClick={() => handleSortChange('priority')}
                  sx={{ cursor: 'pointer', fontWeight: 'bold' }}
                >
                  Priority {sortField === 'priority' && (sortOrder === 'asc' ? '↑' : '↓')}
                </TableCell>
                <TableCell align="center">Active</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    <CircularProgress />
                  </TableCell>
                </TableRow>
              ) : festivals.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    No festivals found
                  </TableCell>
                </TableRow>
              ) : (
                festivals.map((festival) => (
                  <TableRow key={festival._id}>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <FestivalIcon sx={{ mr: 1, color: 'primary.main' }} />
                        {festival.name}
                      </Box>
                    </TableCell>
                    <TableCell>{formatDate(festival.date)}</TableCell>
                    <TableCell>{festival.category}</TableCell>
                    <TableCell>
                      <Chip 
                        label={festival.status} 
                        color={getStatusColor(festival.status)} 
                        size="small" 
                      />
                    </TableCell>
                    <TableCell>{festival.priority}</TableCell>
                    <TableCell align="center">
                      <Switch
                        checked={festival.isActive}
                        onChange={() => handleStatusToggle(festival._id)}
                        disabled={statusLoading[festival._id]}
                        color="primary"
                      />
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title="Edit">
                        <IconButton 
                          onClick={() => handleEditClick(festival._id)}
                          color="primary"
                        >
                          <EditIcon />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete">
                        <IconButton 
                          onClick={() => handleDeleteClick(festival)}
                          color="error"
                        >
                          <DeleteIcon />
                        </IconButton>
                      </Tooltip>
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

      {/* Delete confirmation dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
      >
        <DialogTitle>Delete Festival</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete the festival "{festivalToDelete?.name}"? This action cannot be undone.
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

      {/* Snackbar for notifications */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleSnackbarClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert 
          onClose={handleSnackbarClose} 
          severity={snackbar.severity} 
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default UpcomingFestivals; 