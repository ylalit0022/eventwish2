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
  Tooltip
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Image as ImageIcon
} from '@mui/icons-material';
import { getCategoryIcons, deleteCategoryIcon, toggleCategoryIconStatus } from '../api';

const CategoryIcons = () => {
  const navigate = useNavigate();
  const [categoryIcons, setCategoryIcons] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalItems, setTotalItems] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [categoryIconToDelete, setCategoryIconToDelete] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [sortField, setSortField] = useState('category');
  const [sortOrder, setSortOrder] = useState('asc');
  const [statusLoading, setStatusLoading] = useState({});

  // Fetch category icons
  const fetchCategoryIcons = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const filters = {};
      if (searchQuery) {
        filters.q = searchQuery;
      }
      
      const response = await getCategoryIcons(
        page + 1,
        rowsPerPage,
        sortField,
        sortOrder,
        filters
      );
      
      if (response.success) {
        setCategoryIcons(response.data);
        setTotalItems(response.totalItems);
      } else {
        throw new Error(response.message || 'Failed to fetch category icons');
      }
    } catch (err) {
      console.error('Error fetching category icons:', err);
      setError('Failed to load category icons: ' + (err.message || 'Unknown error'));
    } finally {
      setLoading(false);
    }
  };

  // Initial fetch
  useEffect(() => {
    fetchCategoryIcons();
  }, [page, rowsPerPage, sortField, sortOrder, searchQuery]);

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

  // Handle delete dialog
  const handleDeleteClick = (categoryIcon) => {
    setCategoryIconToDelete(categoryIcon);
    setDeleteDialogOpen(true);
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
    setCategoryIconToDelete(null);
  };

  const handleDeleteConfirm = async () => {
    if (!categoryIconToDelete) return;
    
    try {
      setDeleteLoading(true);
      const response = await deleteCategoryIcon(categoryIconToDelete._id);
      
      if (response.success) {
        // Remove from list
        setCategoryIcons(categoryIcons.filter(icon => icon._id !== categoryIconToDelete._id));
        setDeleteDialogOpen(false);
        setCategoryIconToDelete(null);
      } else {
        throw new Error(response.message || 'Failed to delete category icon');
      }
    } catch (err) {
      console.error(`Error deleting category icon ${categoryIconToDelete._id}:`, err);
      setError('Failed to delete category icon: ' + (err.message || 'Unknown error'));
    } finally {
      setDeleteLoading(false);
    }
  };

  // Handle status toggle
  const handleStatusToggle = async (id) => {
    try {
      // Log the ID we're trying to toggle
      console.log("handleStatusToggle called with ID:", id);
      
      if (!id) {
        console.error("handleStatusToggle called with undefined ID");
        setError('Failed to toggle status: Invalid category icon ID');
        return;
      }
      
      setStatusLoading(prev => ({ ...prev, [id]: true }));
      
      const response = await toggleCategoryIconStatus(id);
      
      if (response.success) {
        // Update category icon status in the list
        setCategoryIcons(categoryIcons.map(icon => 
          // Match by either _id or id since the transform function might have changed the structure
          (icon._id === id || icon.id === id)
            ? { ...icon, status: response.categoryIcon.status } 
            : icon
        ));
      } else {
        console.error("Toggle status API returned error:", response.message, response.error);
        throw new Error(response.message || 'Failed to toggle status');
      }
    } catch (err) {
      console.error(`Error toggling category icon status for ID [${id}]:`, err);
      setError('Failed to toggle status: ' + (err.message || 'Unknown error'));
    } finally {
      setStatusLoading(prev => ({ ...prev, [id]: false }));
    }
  };

  // Handle refresh
  const handleRefresh = () => {
    fetchCategoryIcons();
  };

  // Handle create click
  const handleCreateClick = () => {
    navigate('/category-icons/create');
  };

  // Handle edit click
  const handleEditClick = (id) => {
    if (!id) {
      console.error('Invalid category icon ID');
      return;
    }
    navigate(`/category-icons/${id}`);
  };

  return (
    <Box sx={{ width: '100%' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Category Icons
        </Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={handleCreateClick}
        >
          Add New
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Paper sx={{ width: '100%', mb: 2 }}>
        <Box sx={{ p: 2 }}>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                variant="outlined"
                placeholder="Search by ID or category"
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
            <Grid item xs={12} md={6} sx={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                variant="outlined"
                startIcon={<RefreshIcon />}
                onClick={handleRefresh}
                sx={{ ml: 1 }}
              >
                Refresh
              </Button>
            </Grid>
          </Grid>
        </Box>

        <TableContainer>
          <Table sx={{ minWidth: 750 }} aria-labelledby="categoryIconsTable">
            <TableHead>
              <TableRow>
                <TableCell 
                  onClick={() => handleSortChange('id')}
                  sx={{ cursor: 'pointer' }}
                >
                  ID {sortField === 'id' && (sortOrder === 'asc' ? '↑' : '↓')}
                </TableCell>
                <TableCell 
                  onClick={() => handleSortChange('category')}
                  sx={{ cursor: 'pointer' }}
                >
                  Category {sortField === 'category' && (sortOrder === 'asc' ? '↑' : '↓')}
                </TableCell>
                <TableCell>Icon</TableCell>
                <TableCell 
                  onClick={() => handleSortChange('iconType')}
                  sx={{ cursor: 'pointer' }}
                >
                  Icon Type {sortField === 'iconType' && (sortOrder === 'asc' ? '↑' : '↓')}
                </TableCell>
                <TableCell>Status</TableCell>
                <TableCell 
                  onClick={() => handleSortChange('createdAt')}
                  sx={{ cursor: 'pointer' }}
                >
                  Created {sortField === 'createdAt' && (sortOrder === 'asc' ? '↑' : '↓')}
                </TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    <CircularProgress />
                  </TableCell>
                </TableRow>
              ) : categoryIcons.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    No category icons found
                  </TableCell>
                </TableRow>
              ) : (
                categoryIcons.map((icon) => (
                  <TableRow key={icon._id || icon.id}>
                    <TableCell>{icon.id}</TableCell>
                    <TableCell>{icon.category}</TableCell>
                    <TableCell>
                      {icon.categoryIcon ? (
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                          <Box
                            component="img"
                            src={icon.categoryIcon}
                            alt={icon.category}
                            sx={{ width: 40, height: 40, mr: 1, objectFit: 'contain' }}
                            onError={(e) => {
                              e.target.onerror = null;
                              e.target.src = 'data:image/svg+xml;charset=utf-8,%3Csvg xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22 width%3D%2240%22 height%3D%2240%22 viewBox%3D%220 0 40 40%22%3E%3Crect width%3D%2240%22 height%3D%2240%22 fill%3D%22%23f5f5f5%22%2F%3E%3Ctext x%3D%2250%25%22 y%3D%2250%25%22 font-size%3D%226%22 text-anchor%3D%22middle%22 alignment-baseline%3D%22middle%22 font-family%3D%22Arial%2C sans-serif%22 fill%3D%22%23aaa%22%3EError%3C%2Ftext%3E%3C%2Fsvg%3E';
                            }}
                          />
                        </Box>
                      ) : (
                        <ImageIcon color="disabled" />
                      )}
                    </TableCell>
                    <TableCell>
                      <Chip 
                        label={icon.iconType || 'URL'} 
                        color={icon.iconType === 'RESOURCE' ? 'secondary' : 'primary'} 
                        variant="outlined" 
                        size="small" 
                      />
                    </TableCell>
                    <TableCell>
                      <Switch
                        checked={icon.status !== false}
                        onChange={() => handleStatusToggle(icon._id || icon.id)}
                        disabled={statusLoading[icon._id] || statusLoading[icon.id]}
                        color="primary"
                      />
                    </TableCell>
                    <TableCell>
                      {new Date(icon.createdAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell>
                      <Tooltip title="Edit">
                        <IconButton 
                          onClick={() => handleEditClick(icon._id || icon.id)}
                          size="small"
                        >
                          <EditIcon />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete">
                        <IconButton 
                          onClick={() => handleDeleteClick(icon)}
                          size="small"
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

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
      >
        <DialogTitle id="alert-dialog-title">
          Confirm Delete
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="alert-dialog-description">
            Are you sure you want to delete the category icon "{categoryIconToDelete?.category}"?
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
            autoFocus
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

export default CategoryIcons; 