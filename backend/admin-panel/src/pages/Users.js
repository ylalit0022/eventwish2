import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  TextField,
  Button,
  IconButton,
  Chip,
  Avatar,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Tooltip,
  Alert
} from '@mui/material';
import {
  Search as SearchIcon,
  Block as BlockIcon,
  CheckCircle as UnblockIcon,
  Visibility as ViewIcon
} from '@mui/icons-material';
import { getUsers, blockUser, unblockUser } from '../api';

const Users = () => {
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalUsers, setTotalUsers] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterBlocked, setFilterBlocked] = useState('all');
  const [sortField, setSortField] = useState('lastOnline');
  const [sortOrder, setSortOrder] = useState('desc');
  const [selectedUser, setSelectedUser] = useState(null);
  const [blockDialogOpen, setBlockDialogOpen] = useState(false);
  const [blockReason, setBlockReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  // Fetch users
  useEffect(() => {
    const fetchUsers = async () => {
      try {
        setLoading(true);
        // Fetch real users from the API
        const response = await getUsers(page + 1, rowsPerPage, sortField, sortOrder, {
          q: searchQuery,
          blocked: filterBlocked !== 'all' ? filterBlocked === 'blocked' : undefined
        });
        
        if (response.success) {
          setUsers(response.users);
          setTotalUsers(response.pagination.total);
          setTotalPages(response.pagination.totalPages);
          setError(null);
        } else {
          throw new Error(response.message || 'Failed to load users');
        }
      } catch (err) {
        console.error('Error fetching users:', err);
        setError('Failed to load users: ' + (err.message || 'Unknown error'));
      } finally {
        setLoading(false);
      }
    };

    fetchUsers();
  }, [page, rowsPerPage, searchQuery, filterBlocked, sortField, sortOrder]);

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
  const handleSearch = (event) => {
    setSearchQuery(event.target.value);
    setPage(0);
  };

  // Handle filter change
  const handleFilterChange = (event) => {
    setFilterBlocked(event.target.value);
    setPage(0);
  };

  // Handle sort change
  const handleSortChange = (event) => {
    setSortField(event.target.value);
  };

  const handleSortOrderChange = (event) => {
    setSortOrder(event.target.value);
  };

  // Handle view user
  const handleViewUser = (uid) => {
    navigate(`/users/${uid}`);
  };

  // Handle block user
  const handleBlockUser = (user) => {
    setSelectedUser(user);
    setBlockDialogOpen(true);
  };

  // Handle unblock user
  const handleUnblockUser = async (user) => {
    try {
      setActionLoading(true);
      const response = await unblockUser(user.uid);
      
      if (response.success) {
        // Update the user in the list
        setUsers(users.map(u => 
          u.uid === user.uid 
            ? { ...u, isBlocked: false, blockInfo: null } 
            : u
        ));
        
        // Show success message
        alert(`User ${user.displayName} has been unblocked`);
      } else {
        throw new Error(response.message || 'Failed to unblock user');
      }
    } catch (err) {
      console.error('Error unblocking user:', err);
      alert('Failed to unblock user: ' + (err.message || 'Unknown error'));
    } finally {
      setActionLoading(false);
    }
  };

  // Handle confirm block
  const handleConfirmBlock = async () => {
    try {
      setActionLoading(true);
      const response = await blockUser(selectedUser.uid, blockReason);
      
      if (response.success) {
        // Update the user in the list
        setUsers(users.map(u => 
          u.uid === selectedUser.uid 
            ? { 
                ...u, 
                isBlocked: true, 
                blockInfo: {
                  reason: blockReason,
                  blockedAt: new Date().toISOString(),
                  blockedBy: 'admin@eventwish.com'
                } 
              } 
            : u
        ));
        
        // Close dialog and reset state
        setBlockDialogOpen(false);
        setSelectedUser(null);
        setBlockReason('');
        
        // Show success message
        alert(`User ${selectedUser.displayName} has been blocked`);
      } else {
        throw new Error(response.message || 'Failed to block user');
      }
    } catch (err) {
      console.error('Error blocking user:', err);
      alert('Failed to block user: ' + (err.message || 'Unknown error'));
    } finally {
      setActionLoading(false);
    }
  };

  if (loading && users.length === 0) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error && users.length === 0) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error}</Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ flexGrow: 1 }}>
      <Typography variant="h4" gutterBottom>
        Users
      </Typography>
      
      {/* Filters */}
      <Box sx={{ mb: 3, display: 'flex', flexWrap: 'wrap', gap: 2 }}>
        <TextField
          label="Search"
          variant="outlined"
          size="small"
          value={searchQuery}
          onChange={handleSearch}
          InputProps={{
            endAdornment: <SearchIcon />
          }}
          sx={{ flexGrow: 1, minWidth: '200px' }}
        />
        
        <FormControl size="small" sx={{ minWidth: '150px' }}>
          <InputLabel>Status</InputLabel>
          <Select
            value={filterBlocked}
            label="Status"
            onChange={handleFilterChange}
          >
            <MenuItem value="all">All Users</MenuItem>
            <MenuItem value="active">Active Only</MenuItem>
            <MenuItem value="blocked">Blocked Only</MenuItem>
          </Select>
        </FormControl>
        
        <FormControl size="small" sx={{ minWidth: '150px' }}>
          <InputLabel>Sort By</InputLabel>
          <Select
            value={sortField}
            label="Sort By"
            onChange={handleSortChange}
          >
            <MenuItem value="lastOnline">Last Online</MenuItem>
            <MenuItem value="displayName">Name</MenuItem>
            <MenuItem value="email">Email</MenuItem>
            <MenuItem value="created">Join Date</MenuItem>
          </Select>
        </FormControl>
        
        <FormControl size="small" sx={{ minWidth: '150px' }}>
          <InputLabel>Order</InputLabel>
          <Select
            value={sortOrder}
            label="Order"
            onChange={handleSortOrderChange}
          >
            <MenuItem value="asc">Ascending</MenuItem>
            <MenuItem value="desc">Descending</MenuItem>
          </Select>
        </FormControl>
      </Box>
      
      {/* Users Table */}
      <TableContainer component={Paper}>
        <Table sx={{ minWidth: 650 }} aria-label="users table">
          <TableHead>
            <TableRow>
              <TableCell>User</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Last Online</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {users.map((user) => (
              <TableRow
                key={user.uid}
                sx={{ '&:last-child td, &:last-child th': { border: 0 }, cursor: 'pointer' }}
                hover
                onClick={() => handleViewUser(user.uid)}
              >
                <TableCell component="th" scope="row">
                  <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <Avatar src={user.profilePhoto} alt={user.displayName} sx={{ mr: 2 }} />
                    <Typography>{user.displayName}</Typography>
                  </Box>
                </TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>
                  {user.isBlocked ? (
                    <Chip 
                      label="Blocked" 
                      color="error" 
                      size="small" 
                      title={user.blockInfo?.reason || 'User is blocked'}
                    />
                  ) : (
                    <Chip 
                      label="Active" 
                      color="success" 
                      size="small" 
                    />
                  )}
                </TableCell>
                <TableCell>
                  {new Date(user.lastOnline).toLocaleString()}
                </TableCell>
                <TableCell align="right">
                  <Tooltip title="View Details">
                    <IconButton
                      onClick={(e) => {
                        e.stopPropagation();
                        handleViewUser(user.uid);
                      }}
                    >
                      <ViewIcon />
                    </IconButton>
                  </Tooltip>
                  
                  {user.isBlocked ? (
                    <Tooltip title="Unblock User">
                      <IconButton
                        onClick={(e) => {
                          e.stopPropagation();
                          handleUnblockUser(user);
                        }}
                        color="success"
                      >
                        <UnblockIcon />
                      </IconButton>
                    </Tooltip>
                  ) : (
                    <Tooltip title="Block User">
                      <IconButton
                        onClick={(e) => {
                          e.stopPropagation();
                          handleBlockUser(user);
                        }}
                        color="error"
                      >
                        <BlockIcon />
                      </IconButton>
                    </Tooltip>
                  )}
                </TableCell>
              </TableRow>
            ))}
            {users.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  <Typography variant="body1" sx={{ py: 2 }}>
                    No users found
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
      
      {/* Pagination */}
      <TablePagination
        rowsPerPageOptions={[5, 10, 25]}
        component="div"
        count={totalUsers}
        rowsPerPage={rowsPerPage}
        page={page}
        onPageChange={handleChangePage}
        onRowsPerPageChange={handleChangeRowsPerPage}
      />
      
      {/* Block User Dialog */}
      <Dialog open={blockDialogOpen} onClose={() => setBlockDialogOpen(false)}>
        <DialogTitle>Block User</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to block {selectedUser?.displayName}? This will prevent them from using the app.
          </DialogContentText>
          <TextField
            autoFocus
            margin="dense"
            label="Reason for blocking"
            type="text"
            fullWidth
            variant="outlined"
            value={blockReason}
            onChange={(e) => setBlockReason(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setBlockDialogOpen(false)} disabled={actionLoading}>
            Cancel
          </Button>
          <Button 
            onClick={handleConfirmBlock} 
            color="error" 
            disabled={actionLoading || !blockReason.trim()}
          >
            {actionLoading ? <CircularProgress size={24} /> : 'Block User'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Users; 