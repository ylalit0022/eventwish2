import React, { useState, useEffect, useRef } from 'react';
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
  FormControlLabel,
  Tooltip
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Star as StarIcon,
  FileUpload as FileUploadIcon,
  FileDownload as FileDownloadIcon
} from '@mui/icons-material';
import { getTemplates, deleteTemplate, toggleTemplateStatus, exportTemplatesCSV, importTemplatesCSV } from '../api';

const Templates = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalItems, setTotalItems] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [premiumFilter, setPremiumFilter] = useState(false);
  const [categories, setCategories] = useState({});
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [templateToDelete, setTemplateToDelete] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [sortField, setSortField] = useState('createdAt');
  const [sortOrder, setSortOrder] = useState('desc');
  const [statusLoading, setStatusLoading] = useState({});
  const [importDialogOpen, setImportDialogOpen] = useState(false);
  const [importLoading, setImportLoading] = useState(false);
  const [importResults, setImportResults] = useState(null);

  // Fetch templates
  const fetchTemplates = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const filters = {};
      if (searchQuery) {
        filters.q = searchQuery;
      }
      if (categoryFilter) {
        filters.category = categoryFilter;
      }
      if (premiumFilter) {
        filters.isPremium = true;
      }
      
      const response = await getTemplates(
        page + 1,
        rowsPerPage,
        sortField,
        sortOrder,
        filters
      );
      
      if (response.success) {
        setTemplates(response.data);
        setTotalItems(response.totalItems);
        setCategories(response.categories || {});
      } else {
        throw new Error(response.message || 'Failed to fetch templates');
      }
    } catch (err) {
      console.error('Error fetching templates:', err);
      setError('Failed to load templates: ' + (err.message || 'Unknown error'));
    } finally {
      setLoading(false);
    }
  };

  // Initial fetch
  useEffect(() => {
    fetchTemplates();
  }, [page, rowsPerPage, sortField, sortOrder, searchQuery, categoryFilter, premiumFilter]);

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

  // Handle category filter change
  const handleCategoryFilterChange = (event) => {
    setCategoryFilter(event.target.value);
    setPage(0);
  };

  // Handle premium filter change
  const handlePremiumFilterChange = (event) => {
    setPremiumFilter(event.target.checked);
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
  const handleDeleteClick = (template) => {
    setTemplateToDelete(template);
    setDeleteDialogOpen(true);
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
    setTemplateToDelete(null);
  };

  const handleDeleteConfirm = async () => {
    if (!templateToDelete) return;
    
    try {
      setDeleteLoading(true);
      const response = await deleteTemplate(templateToDelete._id);
      
      if (response.success) {
        // Remove from list
        setTemplates(templates.filter(t => t._id !== templateToDelete._id));
        setDeleteDialogOpen(false);
        setTemplateToDelete(null);
      } else {
        throw new Error(response.message || 'Failed to delete template');
      }
    } catch (err) {
      console.error(`Error deleting template ${templateToDelete._id}:`, err);
      setError('Failed to delete template: ' + (err.message || 'Unknown error'));
    } finally {
      setDeleteLoading(false);
    }
  };

  // Handle status toggle
  const handleStatusToggle = async (id) => {
    try {
      setStatusLoading(prev => ({ ...prev, [id]: true }));
      
      const response = await toggleTemplateStatus(id);
      
      if (response.success) {
        // Update template status in the list
        setTemplates(templates.map(template => 
          template._id === id 
            ? { ...template, status: response.template.status } 
            : template
        ));
      } else {
        throw new Error(response.message || 'Failed to toggle template status');
      }
    } catch (err) {
      console.error(`Error toggling template status ${id}:`, err);
      setError('Failed to toggle template status: ' + (err.message || 'Unknown error'));
    } finally {
      setStatusLoading(prev => ({ ...prev, [id]: false }));
    }
  };

  // Handle CSV export
  const handleExportCSV = async () => {
    try {
      setError(null);
      
      // Show loading state
      setLoading(true);
      
      const filters = {};
      if (searchQuery) filters.q = searchQuery;
      if (categoryFilter) filters.category = categoryFilter;
      if (premiumFilter) filters.isPremium = true;
      
      console.log('Starting CSV export with filters:', filters);
      
      // Add try-catch for more detailed error logging
      try {
        await exportTemplatesCSV(filters);
        console.log('CSV export completed successfully');
      } catch (exportError) {
        console.error('Error exporting CSV:', exportError);
        throw new Error(`Error exporting CSV: ${exportError.message || 'Unknown error'}`);
      }
    } catch (err) {
      console.error('Error exporting CSV:', err);
      setError('Failed to export CSV: ' + (err.message || 'Unknown error'));
    } finally {
      setLoading(false);
    }
  };

  // Handle CSV import dialog
  const handleImportClick = () => {
    setImportDialogOpen(true);
  };

  const handleImportCancel = () => {
    setImportDialogOpen(false);
    setImportResults(null);
  };

  const handleFileSelect = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    
    try {
      setImportLoading(true);
      setImportResults(null);
      
      const response = await importTemplatesCSV(file);
      
      if (response.success) {
        setImportResults({
          created: response.created,
          updated: response.updated,
          errors: response.errors || 0,
          total: response.total
        });
        
        // Refresh templates list
        fetchTemplates();
      } else {
        throw new Error(response.message || 'Failed to import templates');
      }
    } catch (err) {
      console.error('Error importing CSV:', err);
      setError('Failed to import CSV: ' + (err.message || 'Unknown error'));
    } finally {
      setImportLoading(false);
      // Reset file input
      event.target.value = '';
    }
  };

  // Handle refresh
  const handleRefresh = () => {
    fetchTemplates();
  };

  // Handle create
  const handleCreateClick = () => {
    navigate('/templates/create');
  };

  // Handle edit
  const handleEditClick = (id) => {
    navigate(`/templates/${id}`);
  };

  if (loading && templates.length === 0) {
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
            Templates
          </Typography>
        </Grid>
        <Grid item>
          <Button
            variant="outlined"
            color="primary"
            startIcon={<FileUploadIcon />}
            onClick={handleImportClick}
            sx={{ mr: 1 }}
          >
            Import CSV
          </Button>
        </Grid>
        <Grid item>
          <Button
            variant="outlined"
            color="primary"
            startIcon={<FileDownloadIcon />}
            onClick={handleExportCSV}
            sx={{ mr: 1 }}
          >
            Export CSV
          </Button>
        </Grid>
        <Grid item>
          <Button
            variant="contained"
            color="primary"
            startIcon={<AddIcon />}
            onClick={handleCreateClick}
          >
            Create Template
          </Button>
        </Grid>
        <Grid item>
          <IconButton onClick={handleRefresh} color="primary">
            <RefreshIcon />
          </IconButton>
        </Grid>
      </Grid>

      {/* Filters */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={6} md={4}>
            <TextField
              fullWidth
              label="Search Templates"
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
              <InputLabel>Category</InputLabel>
              <Select
                value={categoryFilter}
                label="Category"
                onChange={handleCategoryFilterChange}
              >
                <MenuItem value="">All Categories</MenuItem>
                {Object.keys(categories).map((category) => (
                  <MenuItem key={category} value={category}>
                    {category} ({categories[category]})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <FormControlLabel
              control={
                <Switch
                  checked={premiumFilter}
                  onChange={handlePremiumFilterChange}
                  color="warning"
                />
              }
              label={
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <StarIcon sx={{ color: 'warning.main', mr: 0.5 }} fontSize="small" />
                  Premium Only
                </Box>
              }
            />
          </Grid>
        </Grid>
      </Paper>

      {/* Error message */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Templates table */}
      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Title</TableCell>
                <TableCell>Category</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Premium</TableCell>
                <TableCell>Usage</TableCell>
                <TableCell>Created</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {templates.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    {loading ? 'Loading templates...' : 'No templates found.'}
                  </TableCell>
                </TableRow>
              ) : (
                templates.map((template) => (
                  <TableRow key={template._id}>
                    <TableCell>
                      {template.title}
                    </TableCell>
                    <TableCell>{template.category}</TableCell>
                    <TableCell>
                      <Tooltip title="Click to toggle status">
                        <Switch
                          checked={template.status}
                          onChange={() => handleStatusToggle(template._id)}
                          disabled={statusLoading[template._id]}
                          color={template.status ? 'success' : 'default'}
                          size="small"
                        />
                      </Tooltip>
                    </TableCell>
                    <TableCell>
                      {template.isPremium && (
                        <Chip 
                          icon={<StarIcon />}
                          label="Premium" 
                          color="warning" 
                          size="small" 
                        />
                      )}
                    </TableCell>
                    <TableCell>{template.usageCount || 0}</TableCell>
                    <TableCell>
                      {new Date(template.createdAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell>
                      <IconButton 
                        size="small" 
                        color="primary"
                        onClick={() => handleEditClick(template._id)}
                      >
                        <EditIcon fontSize="small" />
                      </IconButton>
                      <IconButton 
                        size="small" 
                        color="error"
                        onClick={() => handleDeleteClick(template)}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
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
        <DialogTitle>Delete Template</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete the template "{templateToDelete?.title}"? This action cannot be undone.
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

      {/* Import CSV dialog */}
      <Dialog
        open={importDialogOpen}
        onClose={importLoading ? undefined : handleImportCancel}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Import Templates from CSV</DialogTitle>
        <DialogContent>
          {importResults ? (
            <Box sx={{ textAlign: 'center', py: 2 }}>
              <Typography variant="h6" color="primary" gutterBottom>
                Import Completed
              </Typography>
              <Typography variant="body1" paragraph>
                Total templates processed: {importResults.total}
              </Typography>
              <Grid container spacing={2} justifyContent="center">
                <Grid item xs={4}>
                  <Paper sx={{ p: 2, bgcolor: 'success.light', color: 'success.contrastText' }}>
                    <Typography variant="h5">{importResults.created}</Typography>
                    <Typography variant="body2">Created</Typography>
                  </Paper>
                </Grid>
                <Grid item xs={4}>
                  <Paper sx={{ p: 2, bgcolor: 'info.light', color: 'info.contrastText' }}>
                    <Typography variant="h5">{importResults.updated}</Typography>
                    <Typography variant="body2">Updated</Typography>
                  </Paper>
                </Grid>
                <Grid item xs={4}>
                  <Paper sx={{ p: 2, bgcolor: 'error.light', color: 'error.contrastText' }}>
                    <Typography variant="h5">{importResults.errors}</Typography>
                    <Typography variant="body2">Errors</Typography>
                  </Paper>
                </Grid>
              </Grid>
            </Box>
          ) : (
            <>
              <DialogContentText sx={{ mb: 2 }}>
                Upload a CSV file to import templates. The CSV should have the following columns:
                title, category, htmlContent, cssContent, jsContent, previewUrl, status, isPremium, festivalTag, tags, categoryIcon.
              </DialogContentText>
              <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
                <input
                  type="file"
                  accept=".csv"
                  hidden
                  ref={fileInputRef}
                  onChange={handleFileChange}
                />
                <Button
                  variant="contained"
                  onClick={handleFileSelect}
                  startIcon={importLoading ? <CircularProgress size={20} /> : <FileUploadIcon />}
                  disabled={importLoading}
                >
                  {importLoading ? 'Importing...' : 'Select CSV File'}
                </Button>
              </Box>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button 
            onClick={handleImportCancel} 
            disabled={importLoading}
          >
            {importResults ? 'Close' : 'Cancel'}
          </Button>
        </DialogActions>
      </Dialog>
      
      {/* Hidden file input for CSV import */}
      <input
        type="file"
        accept=".csv"
        hidden
        ref={fileInputRef}
        onChange={handleFileChange}
      />
    </Box>
  );
};

export default Templates; 