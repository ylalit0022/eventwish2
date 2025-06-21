import React, { useState, useEffect } from 'react';
import { 
  Typography, 
  Box, 
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
  CircularProgress,
  Tooltip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Avatar
} from '@mui/material';
import { 
  Add as AddIcon, 
  Edit as EditIcon, 
  Delete as DeleteIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Public as GlobalIcon
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { getRegions, deleteRegion } from '../api';
import { useSnackbar } from '../contexts/SnackbarContext';

const Regions = () => {
  const navigate = useNavigate();
  const { showSnackbar } = useSnackbar();
  
  // State
  const [regions, setRegions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalCount, setTotalCount] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [regionToDelete, setRegionToDelete] = useState(null);
  
  // Fetch regions
  const fetchRegions = async () => {
    try {
      setLoading(true);
      const response = await getRegions(
        page + 1,
        rowsPerPage,
        'displayOrder',
        'asc',
        { search: searchQuery }
      );
      
      if (response.success) {
        setRegions(response.data);
        setTotalCount(response.data.length); // Update when pagination is implemented in the API
      } else {
        showSnackbar('Failed to fetch regions', 'error');
      }
    } catch (error) {
      console.error('Error fetching regions:', error);
      showSnackbar('Error fetching regions: ' + (error.message || 'Unknown error'), 'error');
    } finally {
      setLoading(false);
    }
  };
  
  useEffect(() => {
    fetchRegions();
  }, [page, rowsPerPage, searchQuery]);
  
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
  
  // Open delete confirmation dialog
  const handleDeleteClick = (region) => {
    setRegionToDelete(region);
    setDeleteDialogOpen(true);
  };
  
  // Close delete confirmation dialog
  const handleCloseDeleteDialog = () => {
    setDeleteDialogOpen(false);
    setRegionToDelete(null);
  };
  
  // Confirm delete
  const handleConfirmDelete = async () => {
    if (!regionToDelete) return;
    
    try {
      const response = await deleteRegion(regionToDelete.code);
      if (response.success) {
        showSnackbar(`Region "${regionToDelete.name}" deleted successfully`, 'success');
        fetchRegions();
      } else {
        showSnackbar(`Failed to delete region: ${response.message}`, 'error');
      }
    } catch (error) {
      console.error('Error deleting region:', error);
      showSnackbar('Error deleting region: ' + (error.message || 'Unknown error'), 'error');
    } finally {
      handleCloseDeleteDialog();
    }
  };
  
  return (
    <Box sx={{ p: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" component="h1" gutterBottom>
          Regions
        </Typography>
        
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => navigate('/regions/create')}
        >
          Add Region
        </Button>
      </Box>
      
      <Paper sx={{ width: '100%', mb: 2 }}>
        <Box p={2} display="flex" alignItems="center" justifyContent="space-between">
          <TextField
            label="Search Regions"
            variant="outlined"
            size="small"
            value={searchQuery}
            onChange={handleSearchChange}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            }}
            sx={{ width: '300px' }}
          />
          
          <IconButton onClick={fetchRegions} disabled={loading}>
            <RefreshIcon />
          </IconButton>
        </Box>
        
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Flag</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Continent</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Display Order</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    <CircularProgress size={40} />
                  </TableCell>
                </TableRow>
              ) : regions.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    No regions found
                  </TableCell>
                </TableRow>
              ) : (
                regions.map((region) => (
                  <TableRow key={region.code}>
                    <TableCell>{region.code}</TableCell>
                    <TableCell>
                      {region.flagIcon ? (
                        <Avatar 
                          src={region.flagIcon} 
                          alt={region.name}
                          sx={{ width: 30, height: 20 }}
                          variant="rounded"
                        />
                      ) : (
                        <GlobalIcon fontSize="small" color="action" />
                      )}
                    </TableCell>
                    <TableCell>{region.name}</TableCell>
                    <TableCell>{region.continent}</TableCell>
                    <TableCell>
                      <Chip 
                        label={region.isActive ? 'Active' : 'Inactive'} 
                        color={region.isActive ? 'success' : 'default'} 
                        size="small" 
                      />
                    </TableCell>
                    <TableCell>{region.displayOrder}</TableCell>
                    <TableCell align="center">
                      <Tooltip title="Edit">
                        <IconButton 
                          color="primary" 
                          onClick={() => navigate(`/regions/${region.code}`)}
                        >
                          <EditIcon />
                        </IconButton>
                      </Tooltip>
                      
                      <Tooltip title="Delete">
                        <IconButton 
                          color="error" 
                          onClick={() => handleDeleteClick(region)}
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
        
        <TablePagination
          rowsPerPageOptions={[5, 10, 25, 50]}
          component="div"
          count={totalCount}
          rowsPerPage={rowsPerPage}
          page={page}
          onPageChange={handleChangePage}
          onRowsPerPageChange={handleChangeRowsPerPage}
        />
      </Paper>
      
      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialogOpen} onClose={handleCloseDeleteDialog}>
        <DialogTitle>Delete Region</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete the region "{regionToDelete?.name}"? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDeleteDialog}>Cancel</Button>
          <Button onClick={handleConfirmDelete} color="error" variant="contained">
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Regions;
