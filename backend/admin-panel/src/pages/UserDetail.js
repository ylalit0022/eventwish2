import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  Typography,
  Grid,
  Avatar,
  Chip,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  TextField,
  List,
  ListItem,
  ListItemText,
  Tab,
  Tabs,
  Alert,
  Card,
  CardContent,
  Switch,
  MenuItem,
  FormControlLabel,
  FormGroup,
  InputLabel,
  Select,
  FormControl,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Divider,
  IconButton,
  Tooltip,
  Badge
} from '@mui/material';
import {
  Block as BlockIcon,
  CheckCircle,
  Edit as EditIcon,
  Save as SaveIcon,
  Cancel as CancelIcon,
  Favorite as FavoriteIcon,
  FavoriteBorder as FavoriteBorderIcon,
  ThumbUp as ThumbUpIcon,
  ThumbDown as ThumbDownIcon,
  Visibility as ViewIcon,
  Share as ShareIcon,
  FilterList as FilterListIcon
} from '@mui/icons-material';
import { getUserById, blockUser, unblockUser, updateUser } from '../api';

// TabPanel component for tab content
function TabPanel({ children, value, index, ...other }) {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`user-tabpanel-${index}`}
      aria-labelledby={`user-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: 3 }}>
          {children}
        </Box>
      )}
    </div>
  );
}

const UserDetail = () => {
  const { uid } = useParams();
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [tabValue, setTabValue] = useState(0);
  const [blockDialogOpen, setBlockDialogOpen] = useState(false);
  const [blockReason, setBlockReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [editedUser, setEditedUser] = useState(null);
  
  // Activity section pagination state
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [activeFilter, setActiveFilter] = useState('ALL');

  // Fetch user data
  useEffect(() => {
    const fetchUser = async () => {
      try {
        setLoading(true);
        const response = await getUserById(uid);
        
        if (response.success) {
          setUser(response.user);
          setEditedUser(response.user);
          setError(null);
        } else {
          throw new Error(response.message || 'Failed to load user details');
        }
      } catch (err) {
        console.error('Error fetching user:', err);
        setError('Failed to load user details: ' + (err.message || 'Unknown error'));
      } finally {
        setLoading(false);
      }
    };

    fetchUser();
  }, [uid]);

  // Handle tab change
  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  // Handle block user
  const handleBlockUser = () => {
    setBlockDialogOpen(true);
  };

  // Handle confirm block
  const handleConfirmBlock = async () => {
    try {
      setActionLoading(true);
      const response = await blockUser(uid, blockReason);
      
      if (response.success) {
        // Update local state
        setUser(prev => ({
          ...prev,
          isBlocked: true,
          blockInfo: {
            reason: blockReason,
            blockedAt: new Date().toISOString(),
            blockedBy: 'admin@eventwish.com'
          }
        }));
        
        setBlockDialogOpen(false);
        setBlockReason('');
      } else {
        throw new Error(response.message || 'Failed to block user');
      }
    } catch (err) {
      console.error('Error blocking user:', err);
      setError('Failed to block user: ' + (err.message || 'Unknown error'));
    } finally {
      setActionLoading(false);
    }
  };

  // Handle unblock user
  const handleUnblockUser = async () => {
    try {
      setActionLoading(true);
      const response = await unblockUser(uid);
      
      if (response.success) {
        // Update local state
        setUser(prev => ({
          ...prev,
          isBlocked: false,
          blockInfo: null
        }));
      } else {
        throw new Error(response.message || 'Failed to unblock user');
      }
    } catch (err) {
      console.error('Error unblocking user:', err);
      setError('Failed to unblock user: ' + (err.message || 'Unknown error'));
    } finally {
      setActionLoading(false);
    }
  };

  // Handle edit mode toggle
  const handleEditToggle = () => {
    if (editMode) {
      setEditedUser(user); // Reset changes
    }
    setEditMode(!editMode);
  };

  // Handle save changes
  const handleSaveChanges = async () => {
    try {
      setActionLoading(true);
      const response = await updateUser(uid, editedUser);
      
      if (response.success) {
        setUser(response.user);
        setEditMode(false);
        setError(null);
      } else {
        throw new Error(response.message || 'Failed to update user details');
      }
    } catch (err) {
      console.error('Error updating user:', err);
      setError('Failed to update user details: ' + (err.message || 'Unknown error'));
    } finally {
      setActionLoading(false);
    }
  };

  // Handle field change in edit mode
  const handleFieldChange = (field, value) => {
    setEditedUser(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Handle nested object field change
  const handleNestedFieldChange = (parent, field, value) => {
    setEditedUser(prev => ({
      ...prev,
      [parent]: {
        ...prev[parent],
        [field]: value
      }
    }));
  };

  // Handle topic subscriptions change (comma-separated string to array)
  const handleTopicSubscriptionsChange = (value) => {
    // Convert comma-separated string to array
    const topicsArray = value.split(',')
      .map(topic => topic.trim())
      .filter(topic => topic.length > 0);
    
    setEditedUser(prev => ({
      ...prev,
      topicSubscriptions: topicsArray
    }));
  };

  // Format date for input fields
  const formatDateForInput = (dateString) => {
    if (!dateString) return '';
    
    try {
      const date = new Date(dateString);
      // Format as YYYY-MM-DDTHH:MM
      return date.toISOString().slice(0, 16);
    } catch (err) {
      console.error('Error formatting date:', err);
      return '';
    }
  };

  // Parse date from input fields
  const parseDateFromInput = (dateString) => {
    if (!dateString) return null;
    
    try {
      return new Date(dateString).toISOString();
    } catch (err) {
      console.error('Error parsing date:', err);
      return null;
    }
  };

  // Handle page change for activity table
  const handleChangePage = (event, newPage) => {
    setPage(newPage);
  };

  // Handle rows per page change for activity table
  const handleChangeRowsPerPage = (event) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  // Handle filter change for activity table
  const handleFilterChange = (filter) => {
    setActiveFilter(filter);
    setPage(0);
  };

  // Group engagement logs by action type
  const getGroupedEngagementLogs = () => {
    if (!user?.engagementLog || user.engagementLog.length === 0) {
      return {
        LIKE: [],
        UNLIKE: [],
        FAV: [],
        UNFAV: [],
        VIEW: [],
        SHARE: []
      };
    }

    return user.engagementLog.reduce((groups, entry) => {
      const action = entry.action;
      if (!groups[action]) {
        groups[action] = [];
      }
      groups[action].push(entry);
      return groups;
    }, {
      LIKE: [],
      UNLIKE: [],
      FAV: [],
      UNFAV: [],
      VIEW: [],
      SHARE: []
    });
  };

  // Get filtered engagement logs based on active filter
  const getFilteredEngagementLogs = () => {
    const grouped = getGroupedEngagementLogs();
    
    if (activeFilter === 'ALL') {
      return user?.engagementLog || [];
    }
    
    return grouped[activeFilter] || [];
  };

  // Get action icon based on action type
  const getActionIcon = (action) => {
    switch (action) {
      case 'LIKE':
        return <ThumbUpIcon color="primary" />;
      case 'UNLIKE':
        return <ThumbDownIcon color="error" />;
      case 'FAV':
        return <FavoriteIcon color="secondary" />;
      case 'UNFAV':
        return <FavoriteBorderIcon color="error" />;
      case 'VIEW':
        return <ViewIcon color="action" />;
      case 'SHARE':
        return <ShareIcon color="success" />;
      default:
        return null;
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error}</Alert>
      </Box>
    );
  }

  if (!user) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="warning">User not found</Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ flexGrow: 1, p: 3 }}>
      {/* Header */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item>
            <Avatar
              src={user.profilePhoto}
              alt={user.displayName}
              sx={{ width: 64, height: 64 }}
            />
          </Grid>
          <Grid item xs>
            <Typography variant="h5">{user.displayName}</Typography>
            <Typography variant="body2" color="textSecondary">{user.email}</Typography>
            <Box sx={{ mt: 1 }}>
              {user.isBlocked ? (
                <Chip
                  label="Blocked"
                  color="error"
                  icon={<BlockIcon />}
                  sx={{ mr: 1 }}
                />
              ) : (
                <Chip
                  label="Active"
                  color="success"
                  icon={<CheckCircle />}
                  sx={{ mr: 1 }}
                />
              )}
              <Chip
                label={`ID: ${user.uid}`}
                variant="outlined"
                sx={{ mr: 1 }}
              />
            </Box>
          </Grid>
          <Grid item>
            {editMode ? (
              <>
                <Button
                  variant="contained"
                  color="primary"
                  onClick={handleSaveChanges}
                  disabled={actionLoading}
                  startIcon={<SaveIcon />}
                  sx={{ mr: 1 }}
                >
                  Save
                </Button>
                <Button
                  variant="outlined"
                  onClick={handleEditToggle}
                  disabled={actionLoading}
                  startIcon={<CancelIcon />}
                >
                  Cancel
                </Button>
              </>
            ) : (
              <>
                <Button
                  variant="contained"
                  onClick={handleEditToggle}
                  disabled={actionLoading}
                  startIcon={<EditIcon />}
                  sx={{ mr: 1 }}
                >
                  Edit
                </Button>
                {user.isBlocked ? (
                  <Button
                    variant="contained"
                    color="success"
                    onClick={handleUnblockUser}
                    disabled={actionLoading}
                    startIcon={<CheckCircle />}
                  >
                    Unblock
                  </Button>
                ) : (
                  <Button
                    variant="contained"
                    color="error"
                    onClick={handleBlockUser}
                    disabled={actionLoading}
                    startIcon={<BlockIcon />}
                  >
                    Block
                  </Button>
                )}
              </>
            )}
          </Grid>
        </Grid>
      </Paper>

      {/* Tabs */}
      <Paper sx={{ mb: 2 }}>
        <Tabs value={tabValue} onChange={handleTabChange}>
          <Tab label="Profile" />
          <Tab label="Preferences" />
          <Tab label="Subscription" />
          <Tab label="Templates" />
          <Tab label="Categories" />
          <Tab label="Activity" />
        </Tabs>
      </Paper>

      {/* Tab Panels */}
      <TabPanel value={tabValue} index={0}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>Basic Information</Typography>
                <List>
                  <ListItem>
                    <ListItemText
                      primary="Display Name"
                      secondary={
                        editMode ? (
                          <TextField
                            value={editedUser.displayName || ''}
                            onChange={(e) => handleFieldChange('displayName', e.target.value)}
                            size="small"
                            fullWidth
                          />
                        ) : user.displayName
                      }
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="Email"
                      secondary={user.email}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="Device ID"
                      secondary={user.deviceId || 'Not set'}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="Created"
                      secondary={new Date(user.created).toLocaleString()}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="Last Online"
                      secondary={new Date(user.lastOnline).toLocaleString()}
                    />
                  </ListItem>
                </List>
              </CardContent>
            </Card>
          </Grid>
          
          {user.isBlocked && (
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>Block Information</Typography>
                  <List>
                    <ListItem>
                      <ListItemText
                        primary="Blocked By"
                        secondary={user.blockInfo.blockedBy}
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemText
                        primary="Block Reason"
                        secondary={user.blockInfo.reason}
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemText
                        primary="Blocked At"
                        secondary={new Date(user.blockInfo.blockedAt).toLocaleString()}
                      />
                    </ListItem>
                  </List>
                </CardContent>
              </Card>
            </Grid>
          )}

          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>Ads & Monetization</Typography>
                <List>
                  <ListItem>
                    <ListItemText
                      primary="Ads Allowed"
                      secondary={
                        editMode ? (
                          <FormGroup>
                            <FormControlLabel 
                              control={
                                <Switch
                                  checked={editedUser.adsAllowed !== false}
                                  onChange={(e) => handleFieldChange('adsAllowed', e.target.checked)}
                                />
                              } 
                              label={editedUser.adsAllowed !== false ? "Enabled" : "Disabled"}
                            />
                            <Typography variant="caption" color="textSecondary">
                              When disabled, user won't see ads. Typically disabled for premium users.
                            </Typography>
                          </FormGroup>
                        ) : (user.adsAllowed !== false ? 'Enabled' : 'Disabled')
                      }
                    />
                  </ListItem>
                </List>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>Referral Information</Typography>
                <List>
                  <ListItem>
                    <ListItemText
                      primary="Referred By"
                      secondary={user.referredBy?.referredBy || 'Not referred'}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="Referral Code"
                      secondary={user.referredBy?.referralCode || 'No referral code'}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="User's Referral Code"
                      secondary={user.referralCode || 'No referral code assigned'}
                    />
                  </ListItem>
                </List>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>Topic Subscriptions</Typography>
                {editMode ? (
                  <>
                    <TextField
                      label="Topic Subscriptions (comma-separated)"
                      value={(editedUser.topicSubscriptions || []).join(', ')}
                      onChange={(e) => handleTopicSubscriptionsChange(e.target.value)}
                      fullWidth
                      multiline
                      rows={2}
                      helperText="Enter topics separated by commas (e.g., 'diwali, holi, christmas')"
                    />
                  </>
                ) : (
                  <List>
                    {(user.topicSubscriptions && user.topicSubscriptions.length > 0) ? (
                      user.topicSubscriptions.map((topic, index) => (
                        <Chip 
                          key={index} 
                          label={topic} 
                          variant="outlined" 
                          sx={{ m: 0.5 }} 
                        />
                      ))
                    ) : (
                      <ListItem>
                        <ListItemText secondary="No topic subscriptions" />
                      </ListItem>
                    )}
                  </List>
                )}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </TabPanel>

      <TabPanel value={tabValue} index={1}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>User Preferences</Typography>
                <List>
                  <ListItem>
                    <ListItemText
                      primary="Theme"
                      secondary={
                        editMode ? (
                          <TextField
                            select
                            value={editedUser.preferredTheme || 'light'}
                            onChange={(e) => handleFieldChange('preferredTheme', e.target.value)}
                            size="small"
                            fullWidth
                          >
                            <MenuItem value="light">Light</MenuItem>
                            <MenuItem value="dark">Dark</MenuItem>
                          </TextField>
                        ) : user.preferredTheme
                      }
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="Language"
                      secondary={
                        editMode ? (
                          <TextField
                            value={editedUser.preferredLanguage || ''}
                            onChange={(e) => handleFieldChange('preferredLanguage', e.target.value)}
                            size="small"
                            fullWidth
                          />
                        ) : user.preferredLanguage
                      }
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="Timezone"
                      secondary={
                        editMode ? (
                          <TextField
                            value={editedUser.timezone || ''}
                            onChange={(e) => handleFieldChange('timezone', e.target.value)}
                            size="small"
                            fullWidth
                          />
                        ) : user.timezone
                      }
                    />
                  </ListItem>
                </List>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>Push Notification Preferences</Typography>
                <List>
                  <ListItem>
                    <ListItemText
                      primary="Festival Notifications"
                      secondary={
                        editMode ? (
                          <Switch
                            checked={editedUser.pushPreferences?.allowFestivalPush || false}
                            onChange={(e) => handleNestedFieldChange('pushPreferences', 'allowFestivalPush', e.target.checked)}
                          />
                        ) : (user.pushPreferences?.allowFestivalPush ? 'Enabled' : 'Disabled')
                      }
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="Personal Notifications"
                      secondary={
                        editMode ? (
                          <Switch
                            checked={editedUser.pushPreferences?.allowPersonalPush || false}
                            onChange={(e) => handleNestedFieldChange('pushPreferences', 'allowPersonalPush', e.target.checked)}
                          />
                        ) : (user.pushPreferences?.allowPersonalPush ? 'Enabled' : 'Disabled')
                      }
                    />
                  </ListItem>
                </List>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </TabPanel>

      <TabPanel value={tabValue} index={2}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>Subscription Details</Typography>
            <List>
              <ListItem>
                <ListItemText
                  primary="Status"
                  secondary={
                    editMode ? (
                      <FormGroup>
                        <FormControlLabel 
                          control={
                            <Switch
                              checked={editedUser.subscription?.isActive || false}
                              onChange={(e) => handleNestedFieldChange('subscription', 'isActive', e.target.checked)}
                            />
                          } 
                          label={editedUser.subscription?.isActive ? "Active" : "Inactive"}
                        />
                      </FormGroup>
                    ) : (user.subscription?.isActive ? 'Active' : 'Inactive')
                  }
                />
              </ListItem>
              <ListItem>
                <ListItemText
                  primary="Plan"
                  secondary={
                    editMode ? (
                      <FormControl fullWidth size="small">
                        <InputLabel id="subscription-plan-label">Subscription Plan</InputLabel>
                        <Select
                          labelId="subscription-plan-label"
                          value={editedUser.subscription?.plan || ''}
                          onChange={(e) => handleNestedFieldChange('subscription', 'plan', e.target.value)}
                          label="Subscription Plan"
                        >
                          <MenuItem value="">No Plan</MenuItem>
                          <MenuItem value="MONTHLY">Monthly</MenuItem>
                          <MenuItem value="QUARTERLY">Quarterly</MenuItem>
                          <MenuItem value="HALF_YEARLY">Half Yearly</MenuItem>
                          <MenuItem value="YEARLY">Yearly</MenuItem>
                        </Select>
                      </FormControl>
                    ) : (user.subscription?.plan || 'No plan')
                  }
                />
              </ListItem>
              <ListItem>
                <ListItemText
                  primary="Started At"
                  secondary={
                    editMode ? (
                      <TextField
                        type="datetime-local"
                        value={formatDateForInput(editedUser.subscription?.startedAt)}
                        onChange={(e) => handleNestedFieldChange('subscription', 'startedAt', parseDateFromInput(e.target.value))}
                        fullWidth
                        size="small"
                        InputLabelProps={{ shrink: true }}
                      />
                    ) : (user.subscription?.startedAt ? new Date(user.subscription.startedAt).toLocaleString() : 'Not set')
                  }
                />
              </ListItem>
              <ListItem>
                <ListItemText
                  primary="Expires At"
                  secondary={
                    editMode ? (
                      <TextField
                        type="datetime-local"
                        value={formatDateForInput(editedUser.subscription?.expiresAt)}
                        onChange={(e) => handleNestedFieldChange('subscription', 'expiresAt', parseDateFromInput(e.target.value))}
                        fullWidth
                        size="small"
                        InputLabelProps={{ shrink: true }}
                      />
                    ) : (user.subscription?.expiresAt ? new Date(user.subscription.expiresAt).toLocaleString() : 'Not set')
                  }
                />
              </ListItem>
            </List>
          </CardContent>
        </Card>
      </TabPanel>

      <TabPanel value={tabValue} index={3}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>Recent Templates</Typography>
                <List>
                  {user.recentTemplatesUsed?.map((templateId) => (
                    <ListItem key={templateId}>
                      <ListItemText
                        primary={`Template ID: ${templateId}`}
                      />
                    </ListItem>
                  ))}
                  {(!user.recentTemplatesUsed || user.recentTemplatesUsed.length === 0) && (
                    <ListItem>
                      <ListItemText secondary="No recent templates" />
                    </ListItem>
                  )}
                </List>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>Favorites & Likes</Typography>
                <List>
                  <ListItem>
                    <ListItemText
                      primary="Favorite Templates"
                      secondary={`${user.favorites?.length || 0} templates`}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText
                      primary="Liked Templates"
                      secondary={`${user.likes?.length || 0} templates`}
                    />
                  </ListItem>
                </List>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </TabPanel>

      <TabPanel value={tabValue} index={4}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>Category Visits</Typography>
            <List>
              {user.categories?.map((visit, index) => (
                <ListItem key={index}>
                  <ListItemText
                    primary={visit.category}
                    secondary={`Visited ${visit.visitCount} times • Last visit: ${new Date(visit.visitDate).toLocaleString()}`}
                  />
                  <Chip
                    label={visit.source}
                    size="small"
                    variant="outlined"
                  />
                </ListItem>
              ))}
              {(!user.categories || user.categories.length === 0) && (
                <ListItem>
                  <ListItemText secondary="No category visits recorded" />
                </ListItem>
              )}
            </List>
          </CardContent>
        </Card>
      </TabPanel>

      <TabPanel value={tabValue} index={5}>
        <Card>
          <CardContent>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6">Engagement Log</Typography>
              <Box>
                <Tooltip title="Filter by Action">
                  <IconButton 
                    aria-label="filter" 
                    color={activeFilter !== 'ALL' ? 'primary' : 'default'}
                    onClick={(e) => {
                      const anchorEl = e.currentTarget;
                      const menu = document.getElementById('action-filter-menu');
                      menu.style.display = menu.style.display === 'block' ? 'none' : 'block';
                      menu.style.position = 'absolute';
                      menu.style.top = `${anchorEl.getBoundingClientRect().bottom}px`;
                      menu.style.left = `${anchorEl.getBoundingClientRect().left}px`;
                    }}
                  >
                    <FilterListIcon />
                  </IconButton>
                </Tooltip>
                <div 
                  id="action-filter-menu" 
                  style={{ 
                    display: 'none', 
                    position: 'absolute', 
                    backgroundColor: 'white',
                    boxShadow: '0px 2px 10px rgba(0,0,0,0.2)',
                    borderRadius: '4px',
                    zIndex: 1000
                  }}
                >
                  <List dense>
                    <ListItem button onClick={() => handleFilterChange('ALL')}>
                      <ListItemText primary="All Actions" />
                      {activeFilter === 'ALL' && <CheckCircle fontSize="small" color="primary" />}
                    </ListItem>
                    <Divider />
                    <ListItem button onClick={() => handleFilterChange('LIKE')}>
                      <ThumbUpIcon fontSize="small" sx={{ mr: 1 }} color="primary" />
                      <ListItemText primary="Likes" />
                      <Badge badgeContent={getGroupedEngagementLogs().LIKE.length} color="primary" />
                    </ListItem>
                    <ListItem button onClick={() => handleFilterChange('UNLIKE')}>
                      <ThumbDownIcon fontSize="small" sx={{ mr: 1 }} color="error" />
                      <ListItemText primary="Unlikes" />
                      <Badge badgeContent={getGroupedEngagementLogs().UNLIKE.length} color="error" />
                    </ListItem>
                    <ListItem button onClick={() => handleFilterChange('FAV')}>
                      <FavoriteIcon fontSize="small" sx={{ mr: 1 }} color="secondary" />
                      <ListItemText primary="Favorites" />
                      <Badge badgeContent={getGroupedEngagementLogs().FAV.length} color="secondary" />
                    </ListItem>
                    <ListItem button onClick={() => handleFilterChange('UNFAV')}>
                      <FavoriteBorderIcon fontSize="small" sx={{ mr: 1 }} color="error" />
                      <ListItemText primary="Unfavorites" />
                      <Badge badgeContent={getGroupedEngagementLogs().UNFAV.length} color="error" />
                    </ListItem>
                    <ListItem button onClick={() => handleFilterChange('VIEW')}>
                      <ViewIcon fontSize="small" sx={{ mr: 1 }} color="action" />
                      <ListItemText primary="Views" />
                      <Badge badgeContent={getGroupedEngagementLogs().VIEW.length} color="default" />
                    </ListItem>
                    <ListItem button onClick={() => handleFilterChange('SHARE')}>
                      <ShareIcon fontSize="small" sx={{ mr: 1 }} color="success" />
                      <ListItemText primary="Shares" />
                      <Badge badgeContent={getGroupedEngagementLogs().SHARE.length} color="success" />
                    </ListItem>
                  </List>
                </div>
              </Box>
            </Box>
            
            {user?.engagementLog && user.engagementLog.length > 0 ? (
              <>
                <TableContainer>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Action</TableCell>
                        <TableCell>Template ID</TableCell>
                        <TableCell>Timestamp</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {getFilteredEngagementLogs()
                        .slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage)
                        .map((entry, index) => (
                          <TableRow key={index} hover>
                            <TableCell>
                              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                {getActionIcon(entry.action)}
                                <Typography variant="body2" sx={{ ml: 1 }}>
                                  {entry.action}
                                </Typography>
                              </Box>
                            </TableCell>
                            <TableCell>{entry.templateId}</TableCell>
                            <TableCell>{new Date(entry.timestamp).toLocaleString()}</TableCell>
                          </TableRow>
                        ))}
                    </TableBody>
                  </Table>
                </TableContainer>
                <TablePagination
                  rowsPerPageOptions={[5, 10, 25, 50]}
                  component="div"
                  count={getFilteredEngagementLogs().length}
                  rowsPerPage={rowsPerPage}
                  page={page}
                  onPageChange={handleChangePage}
                  onRowsPerPageChange={handleChangeRowsPerPage}
                />
                
                {/* Summary Cards */}
                <Box sx={{ mt: 3 }}>
                  <Typography variant="h6" gutterBottom>Activity Summary</Typography>
                  <Grid container spacing={2}>
                    <Grid item xs={6} sm={4} md={2}>
                      <Card variant="outlined">
                        <CardContent sx={{ textAlign: 'center', py: 1 }}>
                          <ThumbUpIcon color="primary" />
                          <Typography variant="h6">{getGroupedEngagementLogs().LIKE.length}</Typography>
                          <Typography variant="body2" color="textSecondary">Likes</Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={6} sm={4} md={2}>
                      <Card variant="outlined">
                        <CardContent sx={{ textAlign: 'center', py: 1 }}>
                          <FavoriteIcon color="secondary" />
                          <Typography variant="h6">{getGroupedEngagementLogs().FAV.length}</Typography>
                          <Typography variant="body2" color="textSecondary">Favorites</Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={6} sm={4} md={2}>
                      <Card variant="outlined">
                        <CardContent sx={{ textAlign: 'center', py: 1 }}>
                          <ViewIcon color="action" />
                          <Typography variant="h6">{getGroupedEngagementLogs().VIEW.length}</Typography>
                          <Typography variant="body2" color="textSecondary">Views</Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={6} sm={4} md={2}>
                      <Card variant="outlined">
                        <CardContent sx={{ textAlign: 'center', py: 1 }}>
                          <ShareIcon color="success" />
                          <Typography variant="h6">{getGroupedEngagementLogs().SHARE.length}</Typography>
                          <Typography variant="body2" color="textSecondary">Shares</Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={6} sm={4} md={2}>
                      <Card variant="outlined">
                        <CardContent sx={{ textAlign: 'center', py: 1 }}>
                          <ThumbDownIcon color="error" />
                          <Typography variant="h6">{getGroupedEngagementLogs().UNLIKE.length}</Typography>
                          <Typography variant="body2" color="textSecondary">Unlikes</Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                    <Grid item xs={6} sm={4} md={2}>
                      <Card variant="outlined">
                        <CardContent sx={{ textAlign: 'center', py: 1 }}>
                          <FavoriteBorderIcon color="error" />
                          <Typography variant="h6">{getGroupedEngagementLogs().UNFAV.length}</Typography>
                          <Typography variant="body2" color="textSecondary">Unfavorites</Typography>
                        </CardContent>
                      </Card>
                    </Grid>
                  </Grid>
                </Box>
              </>
            ) : (
              <Typography color="textSecondary" align="center" sx={{ py: 4 }}>
                No engagement activity recorded
              </Typography>
            )}
          </CardContent>
        </Card>
      </TabPanel>

      {/* Block User Dialog */}
      <Dialog open={blockDialogOpen} onClose={() => setBlockDialogOpen(false)}>
        <DialogTitle>Block User</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to block {user.displayName}? This will prevent them from using the app.
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

export default UserDetail; 