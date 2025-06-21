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
  DialogTitle
} from '@mui/material';
import { 
  Add as AddIcon, 
  Edit as EditIcon, 
  Delete as DeleteIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
  FormatTextdirectionRToL as RTLIcon,
  FormatTextdirectionLToR as LTRIcon
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { getLanguages, deleteLanguage } from '../api';
import { useSnackbar } from '../contexts/SnackbarContext';

const Languages = () => {
  const navigate = useNavigate();
  const { showSnackbar } = useSnackbar();
  
  // State
  const [languages, setLanguages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalCount, setTotalCount] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [languageToDelete, setLanguageToDelete] = useState(null);
  
  // Fetch languages
  const fetchLanguages = async () => {
    try {
      setLoading(true);
      const response = await getLanguages(
        page + 1,
        rowsPerPage,
        'displayOrder',
        'asc',
        { search: searchQuery }
      );
      
      if (response.success) {
        setLanguages(response.data);
        setTotalCount(response.data.length); // Update when pagination is implemented in the API
      } else {
        showSnackbar('Failed to fetch languages', 'error');
      }
    } catch (error) {
      console.error('Error fetching languages:', error);
      showSnackbar('Error fetching languages: ' + (error.message || 'Unknown error'), 'error');
    } finally {
      setLoading(false);
    }
  };
  
  useEffect(() => {
    fetchLanguages();
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
  const handleDeleteClick = (language) => {
    setLanguageToDelete(language);
    setDeleteDialogOpen(true);
  };
  
  // Close delete confirmation dialog
  const handleCloseDeleteDialog = () => {
    setDeleteDialogOpen(false);
    setLanguageToDelete(null);
  };
  
  // Confirm delete
  const handleConfirmDelete = async () => {
    if (!languageToDelete) return;
    
    try {
      const response = await deleteLanguage(languageToDelete.code);
      if (response.success) {
        showSnackbar(`Language "${languageToDelete.name}" deleted successfully`, 'success');
        fetchLanguages();
      } else {
        showSnackbar(`Failed to delete language: ${response.message}`, 'error');
      }
    } catch (error) {
      console.error('Error deleting language:', error);
      showSnackbar('Error deleting language: ' + (error.message || 'Unknown error'), 'error');
    } finally {
      handleCloseDeleteDialog();
    }
  };
  
  return (
    <Box sx={{ p: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" component="h1" gutterBottom>
          Languages
        </Typography>
        
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => navigate('/languages/create')}
        >
          Add Language
        </Button>
      </Box>
      
      <Paper sx={{ width: '100%', mb: 2 }}>
        <Box p={2} display="flex" alignItems="center" justifyContent="space-between">
          <TextField
            label="Search Languages"
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
          
          <IconButton onClick={fetchLanguages} disabled={loading}>
            <RefreshIcon />
          </IconButton>
        </Box>
        
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Native Name</TableCell>
                <TableCell>RTL</TableCell>
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
              ) : languages.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    No languages found
                  </TableCell>
                </TableRow>
              ) : (
                languages.map((language) => (
                  <TableRow key={language.code}>
                    <TableCell>{language.code}</TableCell>
                    <TableCell>{language.name}</TableCell>
                    <TableCell>{language.nativeName}</TableCell>
                    <TableCell>
                      {language.isRTL ? (
                        <Tooltip title="Right to Left">
                          <RTLIcon color="primary" />
                        </Tooltip>
                      ) : (
                        <Tooltip title="Left to Right">
                          <LTRIcon />
                        </Tooltip>
                      )}
                    </TableCell>
                    <TableCell>
                      <Chip 
                        label={language.isActive ? 'Active' : 'Inactive'} 
                        color={language.isActive ? 'success' : 'default'} 
                        size="small" 
                      />
                    </TableCell>
                    <TableCell>{language.displayOrder}</TableCell>
                    <TableCell align="center">
                      <Tooltip title="Edit">
                        <IconButton 
                          color="primary" 
                          onClick={() => navigate(`/languages/${language.code}`)}
                        >
                          <EditIcon />
                        </IconButton>
                      </Tooltip>
                      
                      <Tooltip title="Delete">
                        <IconButton 
                          color="error" 
                          onClick={() => handleDeleteClick(language)}
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
        <DialogTitle>Delete Language</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete the language "{languageToDelete?.name}"? This action cannot be undone.
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

export default Languages;
