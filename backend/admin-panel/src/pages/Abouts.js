import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Container,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Button,
  Box,
  TextField,
  IconButton,
  Switch,
  Toolbar,
  Tooltip,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  CircularProgress,
  Snackbar,
  Alert
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon
} from '@mui/icons-material';
import { getAbouts, deleteAbout, toggleAboutStatus } from '../api';
import { formatDate } from '../utils/dateUtils2';

const Abouts = () => {
  // State management
  const [abouts, setAbouts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalItems, setTotalItems] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [statusLoading, setStatusLoading] = useState({});
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success'
  });
  
  const navigate = useNavigate();
  
  // Fetch abouts on initial load and when filters change
  useEffect(() => {
    fetchAbouts();
  }, [page, rowsPerPage, statusFilter]);
  
  // Handle fetch abouts
  const fetchAbouts = async () => {
    setLoading(true);
    try {
      const filters = {
        q: searchQuery,
        isActive: statusFilter
      };
      
      const response = await getAbouts(
        page + 1,
        rowsPerPage,
        'createdAt',
        'desc',
        filters
      );
      
      // Ensure data is always an array
      const aboutsData = Array.isArray(response.data) ? response.data : [];
      setAbouts(aboutsData);
      setTotalItems(response.totalItems || 0);
    } catch (error) {
      console.error('Error fetching about entries:', error);
      setSnackbar({
        open: true,
        message: `Error fetching about entries: ${error.message}`,
        severity: 'error'
      });
      // Set abouts to empty array on error
      setAbouts([]);
    } finally {
      setLoading(false);
    }
  };
  
  // Handle search query change
  const handleSearchChange = (event) => {
    setSearchQuery(event.target.value);
  };
  
  // Handle search submit
  const handleSearch = (event) => {
    event.preventDefault();
    setPage(0); // Reset to first page
    fetchAbouts();
  };
  
  // Handle status filter change
  const handleStatusFilterChange = (event) => {
    setStatusFilter(event.target.value);
    setPage(0); // Reset to first page
  };
  
  // Handle page change
  const handleChangePage = (event, newPage) => {
    setPage(newPage);
  };
  
  // Handle rows per page change
  const handleChangeRowsPerPage = (event) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0); // Reset to first page
  };
  
  // Handle create button click
  const handleCreateClick = () => {
    navigate('/about/create');
  };
  
  // Handle edit button click
  const handleEditClick = (id) => {
    if (!id || id === 'undefined' || id === 'null') {
      setSnackbar({
        open: true,
        message: 'Invalid about ID',
        severity: 'error'
      });
      return;
    }
    navigate(`/about/${id}`);
  };
  
  // Handle delete button click
  const handleDeleteClick = async (id) => {
    if (!id || id === 'undefined' || id === 'null') {
      setSnackbar({
        open: true,
        message: 'Invalid about ID',
        severity: 'error'
      });
      return;
    }
    
    if (window.confirm('Are you sure you want to delete this about entry?')) {
      try {
        await deleteAbout(id);
        setSnackbar({
          open: true,
          message: 'About entry deleted successfully',
          severity: 'success'
        });
        fetchAbouts(); // Refresh the list
      } catch (error) {
        console.error(`Error deleting about entry ${id}:`, error);
        setSnackbar({
          open: true,
          message: `Error deleting about entry: ${error.message}`,
          severity: 'error'
        });
      }
    }
  };
  
  // Handle status toggle
  const handleStatusToggle = async (id, currentStatus) => {
    if (!id || id === 'undefined' || id === 'null') {
      setSnackbar({
        open: true,
        message: 'Invalid about ID',
        severity: 'error'
      });
      return;
    }
    
    // Update local loading state
    setStatusLoading(prev => ({ ...prev, [id]: true }));
    
    try {
      const result = await toggleAboutStatus(id);
      
      // Update the status in the local state
      setAbouts(prevAbouts =>
        prevAbouts.map(about =>
          about._id === id ? { ...about, isActive: !currentStatus } : about
        )
      );
      
      setSnackbar({
        open: true,
        message: result.message || 'Status updated successfully',
        severity: 'success'
      });
    } catch (error) {
      console.error(`Error toggling about status ${id}:`, error);
      setSnackbar({
        open: true,
        message: `Error toggling about status: ${error.message}`,
        severity: 'error'
      });
    } finally {
      // Clear loading state
      setStatusLoading(prev => ({ ...prev, [id]: false }));
    }
  };
  
  // Handle snackbar close
  const handleSnackbarClose = (event, reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setSnackbar(prev => ({ ...prev, open: false }));
  };
  
  // Handle refresh button click
  const handleRefresh = () => {
    fetchAbouts();
  };
  
  return (
    <Container maxWidth="lg">
      <Box sx={{ my: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h4" gutterBottom>
            About Management
          </Typography>
          <Button
            variant="contained"
            color="primary"
            startIcon={<AddIcon />}
            onClick={handleCreateClick}
          >
            Add About
          </Button>
        </Box>
        
        <Paper sx={{ mb: 2, p: 2 }}>
          <Toolbar disableGutters sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
            <Box component="form" onSubmit={handleSearch} sx={{ display: 'flex', flexGrow: 1 }}>
              <TextField
                label="Search"
                variant="outlined"
                size="small"
                value={searchQuery}
                onChange={handleSearchChange}
                sx={{ flexGrow: 1, mr: 1 }}
              />
              <Button
                type="submit"
                variant="contained"
                startIcon={<SearchIcon />}
                sx={{ mr: 1 }}
              >
                Search
              </Button>
              <Tooltip title="Refresh">
                <IconButton onClick={handleRefresh}>
                  <RefreshIcon />
                </IconButton>
              </Tooltip>
            </Box>
            <FormControl variant="outlined" size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                onChange={handleStatusFilterChange}
                label="Status"
              >
                <MenuItem value="all">All</MenuItem>
                <MenuItem value="true">Active</MenuItem>
                <MenuItem value="false">Inactive</MenuItem>
              </Select>
            </FormControl>
          </Toolbar>
        </Paper>
        
        <Paper>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Title</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Created At</TableCell>
                  <TableCell>Updated At</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center">
                      <CircularProgress />
                    </TableCell>
                  </TableRow>
                ) : abouts.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center">
                      No about entries found
                    </TableCell>
                  </TableRow>
                ) : (
                  abouts.map((about) => (
                    <TableRow key={about._id}>
                      <TableCell>{about.title}</TableCell>
                      <TableCell>
                        <Switch
                          checked={about.isActive}
                          onChange={() => handleStatusToggle(about._id, about.isActive)}
                          disabled={statusLoading[about._id]}
                        />
                        {statusLoading[about._id] && (
                          <CircularProgress size={20} sx={{ ml: 1 }} />
                        )}
                      </TableCell>
                      <TableCell>{formatDate(about.createdAt)}</TableCell>
                      <TableCell>{formatDate(about.updatedAt)}</TableCell>
                      <TableCell>
                        <Tooltip title="Edit">
                          <IconButton onClick={() => handleEditClick(about._id)}>
                            <EditIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete">
                          <IconButton onClick={() => handleDeleteClick(about._id)}>
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
          <TablePagination
            rowsPerPageOptions={[5, 10, 25]}
            component="div"
            count={totalItems}
            rowsPerPage={rowsPerPage}
            page={page}
            onPageChange={handleChangePage}
            onRowsPerPageChange={handleChangeRowsPerPage}
          />
        </Paper>
      </Box>
      
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleSnackbarClose}
      >
        <Alert onClose={handleSnackbarClose} severity={snackbar.severity}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default Abouts; 