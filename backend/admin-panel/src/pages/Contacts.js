import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Container,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  TextField,
  IconButton,
  Switch,
  FormControlLabel,
  Checkbox,
  TablePagination,
  CircularProgress,
  Snackbar,
  Alert,
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
  Refresh as RefreshIcon
} from '@mui/icons-material';
import { getContacts, deleteContact, toggleContactStatus } from '../api';
import { formatDate } from '../utils/dateUtils2';

const Contacts = () => {
  const navigate = useNavigate();
  
  // State variables
  const [contacts, setContacts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalItems, setTotalItems] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [showActiveOnly, setShowActiveOnly] = useState(false);
  const [statusLoading, setStatusLoading] = useState({});
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [contactToDelete, setContactToDelete] = useState(null);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success'
  });
  
  // Fetch contacts on component mount and when filters change
  useEffect(() => {
    fetchContacts();
  }, [page, rowsPerPage, searchQuery, showActiveOnly]);
  
  // Fetch contacts from API
  const fetchContacts = async () => {
    setLoading(true);
    try {
      const filters = {};
      if (searchQuery) filters.q = searchQuery;
      if (showActiveOnly) filters.isActive = true;
      
      const response = await getContacts(
        page + 1,
        rowsPerPage,
        'createdAt',
        'desc',
        filters
      );
      
      setContacts(response.data || []);
      setTotalItems(response.totalItems || 0);
      setError(null);
    } catch (err) {
      console.error('Error fetching contacts:', err);
      setError('Failed to load contacts. Please try again.');
      setSnackbar({
        open: true,
        message: 'Failed to load contacts: ' + (err.message || 'Unknown error'),
        severity: 'error'
      });
    } finally {
      setLoading(false);
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
  
  // Handle search input change
  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
    setPage(0);
  };
  
  // Handle search form submit
  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchContacts();
  };
  
  // Handle active filter change
  const handleActiveFilterChange = (e) => {
    setShowActiveOnly(e.target.checked);
    setPage(0);
  };
  
  // Handle status toggle
  const handleStatusToggle = async (id) => {
    if (!id) {
      setSnackbar({
        open: true,
        message: 'Error toggling status: Invalid contact ID',
        severity: 'error'
      });
      return;
    }
    
    setStatusLoading((prev) => ({ ...prev, [id]: true }));
    
    try {
      const response = await toggleContactStatus(id);
      
      // Update the contact in the list
      setContacts((prevContacts) =>
        prevContacts.map((contact) =>
          contact._id === id ? { ...contact, isActive: !contact.isActive } : contact
        )
      );
      
      setSnackbar({
        open: true,
        message: response.message || 'Status toggled successfully',
        severity: 'success'
      });
    } catch (err) {
      console.error(`Error toggling contact status ${id}:`, err);
      setSnackbar({
        open: true,
        message: `Error toggling contact status: ${err.message || 'Unknown error'}`,
        severity: 'error'
      });
    } finally {
      setStatusLoading((prev) => ({ ...prev, [id]: false }));
    }
  };
  
  // Handle delete button click
  const handleDeleteClick = (contact) => {
    setContactToDelete(contact);
    setDeleteDialogOpen(true);
  };
  
  // Handle delete confirmation
  const handleDeleteConfirm = async () => {
    if (!contactToDelete || !contactToDelete._id) {
      setSnackbar({
        open: true,
        message: 'Error deleting contact: Invalid contact ID',
        severity: 'error'
      });
      setDeleteDialogOpen(false);
      return;
    }
    
    try {
      await deleteContact(contactToDelete._id);
      
      // Remove the contact from the list
      setContacts((prevContacts) =>
        prevContacts.filter((contact) => contact._id !== contactToDelete._id)
      );
      
      setSnackbar({
        open: true,
        message: 'Contact deleted successfully',
        severity: 'success'
      });
      
      // Refresh the list if we deleted the last item on a page
      if (contacts.length === 1 && page > 0) {
        setPage(page - 1);
      } else {
        fetchContacts();
      }
    } catch (err) {
      console.error(`Error deleting contact ${contactToDelete._id}:`, err);
      setSnackbar({
        open: true,
        message: `Error deleting contact: ${err.message || 'Unknown error'}`,
        severity: 'error'
      });
    } finally {
      setDeleteDialogOpen(false);
      setContactToDelete(null);
    }
  };
  
  // Handle delete dialog close
  const handleDeleteDialogClose = () => {
    setDeleteDialogOpen(false);
    setContactToDelete(null);
  };
  
  // Handle snackbar close
  const handleSnackbarClose = (event, reason) => {
    if (reason === 'clickaway') return;
    setSnackbar((prev) => ({ ...prev, open: false }));
  };
  
  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h4" component="h1" gutterBottom>
            Contact Management
          </Typography>
          <Button
            variant="contained"
            color="primary"
            startIcon={<AddIcon />}
            component={Link}
            to="/contacts/create"
          >
            Add New Contact
          </Button>
        </Box>
        
        {/* Search and filter */}
        <Paper sx={{ p: 2, mb: 2 }}>
          <Box component="form" onSubmit={handleSearchSubmit} sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <TextField
              label="Search by title"
              variant="outlined"
              size="small"
              value={searchQuery}
              onChange={handleSearchChange}
              sx={{ flexGrow: 1, mr: 2 }}
            />
            <IconButton type="submit" color="primary" aria-label="search">
              <SearchIcon />
            </IconButton>
            <IconButton color="secondary" aria-label="refresh" onClick={fetchContacts}>
              <RefreshIcon />
            </IconButton>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={showActiveOnly}
                  onChange={handleActiveFilterChange}
                  color="primary"
                />
              }
              label="Show active only"
            />
          </Box>
        </Paper>
        
        {/* Contacts table */}
        <Paper sx={{ width: '100%', overflow: 'hidden' }}>
          <TableContainer>
            <Table stickyHeader aria-label="contacts table">
              <TableHead>
                <TableRow>
                  <TableCell>Title</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Created At</TableCell>
                  <TableCell>Updated At</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 3 }}>
                      <CircularProgress />
                    </TableCell>
                  </TableRow>
                ) : error ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 3 }}>
                      <Typography color="error">{error}</Typography>
                    </TableCell>
                  </TableRow>
                ) : contacts.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 3 }}>
                      <Typography>No contacts found.</Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  contacts.map((contact) => (
                    <TableRow key={contact._id} hover>
                      <TableCell>{contact.title}</TableCell>
                      <TableCell>
                        <Switch
                          checked={contact.isActive}
                          onChange={() => handleStatusToggle(contact._id)}
                          disabled={statusLoading[contact._id]}
                          color="primary"
                        />
                        {statusLoading[contact._id] && (
                          <CircularProgress size={24} sx={{ ml: 1 }} />
                        )}
                      </TableCell>
                      <TableCell>{formatDate(contact.createdAt)}</TableCell>
                      <TableCell>{formatDate(contact.updatedAt)}</TableCell>
                      <TableCell align="right">
                        <IconButton
                          color="primary"
                          aria-label="edit"
                          onClick={() => navigate(`/contacts/${contact._id}`)}
                        >
                          <EditIcon />
                        </IconButton>
                        <IconButton
                          color="error"
                          aria-label="delete"
                          onClick={() => handleDeleteClick(contact)}
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
      
      {/* Delete confirmation dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteDialogClose}
        aria-labelledby="delete-dialog-title"
        aria-describedby="delete-dialog-description"
      >
        <DialogTitle id="delete-dialog-title">Delete Contact</DialogTitle>
        <DialogContent>
          <DialogContentText id="delete-dialog-description">
            Are you sure you want to delete the contact "{contactToDelete?.title}"? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteDialogClose} color="primary">
            Cancel
          </Button>
          <Button onClick={handleDeleteConfirm} color="error" autoFocus>
            Delete
          </Button>
        </DialogActions>
      </Dialog>
      
      {/* Snackbar for notifications */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleSnackbarClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          onClose={handleSnackbarClose}
          severity={snackbar.severity}
          variant="filled"
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default Contacts; 