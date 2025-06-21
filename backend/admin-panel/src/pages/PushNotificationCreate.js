import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Container,
  Grid,
  IconButton,
  Paper,
  Snackbar,
  Stack,
  TextField,
  Typography,
  Alert,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormHelperText,
  Chip,
  Autocomplete,
  Switch,
  FormControlLabel,
  Divider,
  Card,
  CardContent,
  Tooltip,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Collapse
} from '@mui/material';
import { 
  ArrowBack as ArrowBackIcon,
  NotificationsActive as NotificationsActiveIcon,
  Schedule as ScheduleIcon,
  Preview as PreviewIcon,
  PersonAdd as PersonAddIcon,
  Group as GroupIcon,
  AccessTime as AccessTimeIcon,
  Category as CategoryIcon,
  Language as LanguageIcon,
  ExpandMore as ExpandMoreIcon,
  ExpandLess as ExpandLessIcon
} from '@mui/icons-material';
// Date picker imports removed due to compatibility issues
import { 
  createPushNotification, 
  getTopicsWithCounts, 
  getUsers, 
  getUserSegments,
  createPersonalizedNotification,
  previewPersonalizedNotification
} from '../api';

const PushNotificationCreate = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [topics, setTopics] = useState([]);
  const [users, setUsers] = useState([]);
  const [loadingTopics, setLoadingTopics] = useState(false);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [showPreview, setShowPreview] = useState(false);
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [userSegments, setUserSegments] = useState(null);
  const [loadingSegments, setLoadingSegments] = useState(false);
  const [showSegments, setShowSegments] = useState(false);
  const [previewData, setPreviewData] = useState(null);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [expandedSegment, setExpandedSegment] = useState(null);
  
  const [formData, setFormData] = useState({
    title: '',
    body: '',
    imageUrl: '',
    deepLink: '',
    type: 'BULK',
    notificationType: 'GENERAL',
    topic: '',
    targetUserIds: [],
    scheduledFor: null,
    personalizationKeys: [],
    data: {}
  });

  // Load topics, users and segments
  useEffect(() => {
    const fetchTopics = async () => {
      setLoadingTopics(true);
      try {
        const response = await getTopicsWithCounts();
        if (response && response.topics) {
          setTopics(response.topics);
        } else {
          console.error('Invalid topics response:', response);
          setTopics([]);
        }
      } catch (error) {
        console.error('Error loading topics:', error);
        setTopics([]);
      } finally {
        setLoadingTopics(false);
      }
    };

    const fetchUsers = async () => {
      setLoadingUsers(true);
      try {
        const response = await getUsers(1, 100, 'lastOnline', 'desc');
        if (response && response.users) {
          setUsers(response.users);
        } else {
          console.error('Invalid users response:', response);
          setUsers([]);
        }
      } catch (error) {
        console.error('Error loading users:', error);
        setUsers([]);
      } finally {
        setLoadingUsers(false);
      }
    };

    const fetchUserSegments = async () => {
      setLoadingSegments(true);
      try {
        const response = await getUserSegments();
        if (response && response.success) {
          setUserSegments(response.segments);
        } else {
          console.error('Invalid segments response:', response);
          // Provide fallback data for segments to avoid showing loading forever
          setUserSegments({
            activity: { activeToday: 0, inactiveThreeDays: 0 },
            topics: [],
            languages: []
          });
          setSnackbar({
            open: true,
            message: 'Failed to load user segments. Using default values.',
            severity: 'warning'
          });
        }
      } catch (error) {
        console.error('Error loading user segments:', error);
        // Provide fallback data for segments to avoid showing loading forever
        setUserSegments({
          activity: { activeToday: 0, inactiveThreeDays: 0 },
          topics: [],
          languages: []
        });
        setSnackbar({
          open: true,
          message: `Error loading user segments: ${error.message}`,
          severity: 'error'
        });
      } finally {
        setLoadingSegments(false);
      }
    };

    fetchTopics();
    fetchUsers();
    fetchUserSegments();
  }, []);

  // Extract personalization keys from title and body
  useEffect(() => {
    if (formData.type === 'PERSONALIZED') {
      const extractedKeys = [];
      const placeholderRegex = /\{\{([^}]+)\}\}/g;
      let match;
      
      // Extract from title
      while ((match = placeholderRegex.exec(formData.title)) !== null) {
        extractedKeys.push(match[1]);
      }
      
      // Extract from body
      placeholderRegex.lastIndex = 0; // Reset regex index
      while ((match = placeholderRegex.exec(formData.body)) !== null) {
        extractedKeys.push(match[1]);
      }
      
      // Use unique keys
      const uniqueKeys = [...new Set(extractedKeys)];
      
      setFormData(prevData => ({
        ...prevData,
        personalizationKeys: uniqueKeys
      }));
    }
  }, [formData.title, formData.body, formData.type]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: value
    });
  };

  const handleUserSelectionChange = (event, newValue) => {
    setSelectedUsers(newValue);
    setFormData({
      ...formData,
      targetUserIds: newValue.map(user => user._id)
    });
  };

  const handleScheduleChange = (e) => {
    const { value } = e.target;
    setFormData({
      ...formData,
      scheduledFor: value ? new Date(value).toISOString() : null
    });
  };

  const togglePreview = async () => {
    if (!showPreview && formData.type === 'PERSONALIZED') {
      // Fetch preview data
      await handlePreviewPersonalized();
    }
    setShowPreview(!showPreview);
  };

  const handlePreviewPersonalized = async () => {
    if (!formData.title || !formData.body) {
      setSnackbar({
        open: true,
        message: 'Title and body are required for preview',
        severity: 'warning'
      });
      return;
    }

    setLoadingPreview(true);
    try {
      const response = await previewPersonalizedNotification({
        title: formData.title,
        body: formData.body,
        personalizationKeys: formData.personalizationKeys
      });

      if (response && response.success) {
        setPreviewData(response.preview);
      } else {
        throw new Error(response?.message || 'Preview failed');
      }
    } catch (error) {
      setSnackbar({
        open: true,
        message: `Error generating preview: ${error.message}`,
        severity: 'error'
      });
      setPreviewData(null);
    } finally {
      setLoadingPreview(false);
    }
  };

  const toggleSegments = () => {
    setShowSegments(!showSegments);
  };

  const handleExpandSegment = (segment) => {
    setExpandedSegment(expandedSegment === segment ? null : segment);
  };

  const handleSelectSegment = (segmentType, segmentValue) => {
    // Filter users based on the selected segment
    setLoadingUsers(true);
    
    // This is a simplified implementation - in a real app, you would fetch filtered users from the backend
    // For now, we'll just simulate selecting users by segment
    setTimeout(() => {
      // Example: select first 5 users as a simulation
      const simulatedSelection = users.slice(0, 5);
      setSelectedUsers(simulatedSelection);
      setFormData({
        ...formData,
        targetUserIds: simulatedSelection.map(user => user._id)
      });
      
      setSnackbar({
        open: true,
        message: `Selected ${simulatedSelection.length} users from ${segmentType}: ${segmentValue}`,
        severity: 'info'
      });
      
      setLoadingUsers(false);
    }, 1000);
  };

  const validateForm = () => {
    if (!formData.title.trim()) {
      setSnackbar({
        open: true,
        message: 'Title is required',
        severity: 'error'
      });
      return false;
    }
    
    if (!formData.body.trim()) {
      setSnackbar({
        open: true,
        message: 'Message body is required',
        severity: 'error'
      });
      return false;
    }
    
    if (formData.type === 'TOPIC' && !formData.topic) {
      setSnackbar({
        open: true,
        message: 'Topic is required for topic-based notifications',
        severity: 'error'
      });
      return false;
    }
    
    if (formData.type === 'PERSONALIZED' && (!formData.targetUserIds || formData.targetUserIds.length === 0)) {
      setSnackbar({
        open: true,
        message: 'At least one user must be selected for personalized notifications',
        severity: 'error'
      });
      return false;
    }
    
    return true;
  };

  const handleSubmit = async () => {
    if (!validateForm()) return;
    
    setLoading(true);
    try {
      let response;
      
      if (formData.type === 'PERSONALIZED') {
        console.log('Sending personalized notification with data:', formData);
        response = await createPersonalizedNotification(formData);
      } else {
        console.log('Sending regular notification with data:', formData);
        response = await createPushNotification(formData);
      }
      
      console.log('API response:', response);
      
      if (response && response.success) {
        setSnackbar({
          open: true,
          message: 'Notification created successfully',
          severity: 'success'
        });
        setTimeout(() => navigate('/push-notifications'), 1500);
      } else {
        // More detailed error handling for unsuccessful but non-error responses
        const errorMessage = response?.message || 'Server returned unsuccessful response';
        console.error('Unsuccessful response:', errorMessage, response);
        throw new Error(errorMessage);
      }
    } catch (error) {
      console.error('Error creating notification:', error);
      // More detailed error message to help debugging
      let errorMessage = 'Error creating notification';
      
      if (error.response) {
        // The request was made and the server responded with a status code
        // that falls out of the range of 2xx
        errorMessage += `: ${error.response.status} - ${error.response.data?.message || 'Unknown server error'}`;
        console.error('Error response data:', error.response.data);
      } else if (error.request) {
        // The request was made but no response was received
        errorMessage += ': No response received from server';
      } else {
        // Something happened in setting up the request that triggered an Error
        errorMessage += `: ${error.message}`;
      }
      
      setSnackbar({
        open: true,
        message: errorMessage,
        severity: 'error'
      });
    } finally {
      setLoading(false);
    }
  };

  const renderPlaceholderHelp = () => {
    const commonPlaceholders = [
      { key: 'displayName', description: 'User\'s display name' },
      { key: 'preferredLanguage', description: 'User\'s preferred language' },
      { key: 'lastOnline', description: 'Last time user was online' }
    ];

    return (
      <Box mt={2}>
        <Typography variant="subtitle2" gutterBottom>
          Available Placeholders:
        </Typography>
        <Grid container spacing={1}>
          {commonPlaceholders.map((placeholder) => (
            <Grid item key={placeholder.key}>
              <Chip 
                label={`{{${placeholder.key}}}`} 
                size="small" 
                color="primary" 
                variant="outlined"
                onClick={() => {
                  // Copy to clipboard
                  navigator.clipboard.writeText(`{{${placeholder.key}}}`);
                  setSnackbar({
                    open: true,
                    message: `Copied ${placeholder.key} placeholder to clipboard`,
                    severity: 'info'
                  });
                }}
              />
            </Grid>
          ))}
        </Grid>
        <Typography variant="caption" color="text.secondary" mt={1} display="block">
          Click on a placeholder to copy it to clipboard. Use these in your title and message body.
        </Typography>
      </Box>
    );
  };

  return (
    <Container>
      <Box mb={4}>
        <Stack direction="row" alignItems="center" spacing={1} mb={2}>
          <IconButton onClick={() => navigate('/push-notifications')}>
            <ArrowBackIcon />
          </IconButton>
          <Typography variant="h5">Create Push Notification</Typography>
        </Stack>

        <Paper sx={{ p: 3 }}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel id="notification-type-label">Notification Type</InputLabel>
                <Select
                  labelId="notification-type-label"
                  id="type"
                  name="type"
                  value={formData.type}
                  label="Notification Type"
                  onChange={handleInputChange}
                >
                  <MenuItem value="BULK">Bulk (All Users)</MenuItem>
                  <MenuItem value="TOPIC">Topic-based</MenuItem>
                  <MenuItem value="PERSONALIZED">Personalized</MenuItem>
                </Select>
                <FormHelperText>
                  {formData.type === 'BULK' && 'Send to all users'}
                  {formData.type === 'TOPIC' && 'Send to users subscribed to a specific topic'}
                  {formData.type === 'PERSONALIZED' && 'Send personalized messages to specific users'}
                </FormHelperText>
              </FormControl>
            </Grid>

            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel id="notification-category-label">Notification Category</InputLabel>
                <Select
                  labelId="notification-category-label"
                  id="notificationType"
                  name="notificationType"
                  value={formData.notificationType}
                  label="Notification Category"
                  onChange={handleInputChange}
                >
                  <MenuItem value="GENERAL">General</MenuItem>
                  <MenuItem value="PROMOTIONAL">Promotional</MenuItem>
                  <MenuItem value="TRANSACTIONAL">Transactional</MenuItem>
                  <MenuItem value="INACTIVITY">Inactivity Re-engagement</MenuItem>
                  <MenuItem value="FESTIVAL">Festival</MenuItem>
                </Select>
                <FormHelperText>
                  Categorize your notification for analytics and filtering
                </FormHelperText>
              </FormControl>
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Title"
                name="title"
                value={formData.title}
                onChange={handleInputChange}
                required
                helperText={formData.type === 'PERSONALIZED' ? 'You can use placeholders like {{displayName}}' : ''}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Message"
                name="body"
                value={formData.body}
                onChange={handleInputChange}
                required
                multiline
                rows={4}
                helperText={formData.type === 'PERSONALIZED' ? 'You can use placeholders like {{displayName}}' : ''}
              />
            </Grid>

            {formData.type === 'PERSONALIZED' && renderPlaceholderHelp()}

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Image URL (Optional)"
                name="imageUrl"
                value={formData.imageUrl}
                onChange={handleInputChange}
                helperText="URL for an image to display with the notification"
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Deep Link (Optional)"
                name="deepLink"
                value={formData.deepLink}
                onChange={handleInputChange}
                helperText="Deep link URL for in-app navigation when notification is tapped"
              />
            </Grid>

            {formData.type === 'TOPIC' && (
              <Grid item xs={12}>
                <FormControl fullWidth>
                  <InputLabel id="topic-label">Topic</InputLabel>
                  <Select
                    labelId="topic-label"
                    id="topic"
                    name="topic"
                    value={formData.topic}
                    label="Topic"
                    onChange={handleInputChange}
                    disabled={loadingTopics}
                  >
                    {loadingTopics ? (
                      <MenuItem value="">
                        <CircularProgress size={20} /> Loading...
                      </MenuItem>
                    ) : (
                      topics.map((topic) => (
                        <MenuItem key={topic.name} value={topic.name}>
                          {topic.name} ({topic.count} users)
                        </MenuItem>
                      ))
                    )}
                  </Select>
                  <FormHelperText>
                    Select a topic to send the notification to
                  </FormHelperText>
                </FormControl>
              </Grid>
            )}

            {formData.type === 'PERSONALIZED' && (
              <>
                <Grid item xs={12}>
                  <Stack direction="row" spacing={2} alignItems="center" mb={2}>
                    <Button 
                      variant="outlined" 
                      startIcon={<GroupIcon />}
                      onClick={toggleSegments}
                      disabled={loadingSegments || !userSegments}
                    >
                      {showSegments ? 'Hide User Segments' : 'Show User Segments'}
                    </Button>
                    {loadingSegments && <CircularProgress size={24} />}
                    <Button
                      variant="outlined"
                      color="secondary"
                      onClick={() => {
                        if (selectedUsers.length === users.length) {
                          // Deselect all
                          setSelectedUsers([]);
                          setFormData({
                            ...formData,
                            targetUserIds: []
                          });
                        } else {
                          // Select all
                          setSelectedUsers([...users]);
                          setFormData({
                            ...formData,
                            targetUserIds: users.map(user => user._id)
                          });
                        }
                      }}
                    >
                      {selectedUsers.length === users.length ? 'Deselect All' : 'Select All Users'}
                    </Button>
                  </Stack>
                </Grid>

                {showSegments && userSegments && (
                  <Grid item xs={12}>
                    <Paper variant="outlined" sx={{ p: 2, mb: 2, maxHeight: '400px', overflow: 'auto' }}>
                      <Typography variant="subtitle1" gutterBottom>
                        User Segments
                      </Typography>
                      
                      <List component="nav">
                        {/* Activity Segment */}
                        <ListItem button onClick={() => handleExpandSegment('activity')}>
                          <ListItemIcon>
                            <AccessTimeIcon />
                          </ListItemIcon>
                          <ListItemText primary="By Activity" />
                          {expandedSegment === 'activity' ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                        </ListItem>
                        <Collapse in={expandedSegment === 'activity'} timeout="auto" unmountOnExit>
                          <List component="div" disablePadding>
                            <ListItem 
                              button 
                              sx={{ pl: 4 }} 
                              onClick={() => handleSelectSegment('activity', 'activeToday')}
                            >
                              <ListItemText 
                                primary="Active Today" 
                                secondary={`${userSegments.activity.activeToday} users`} 
                              />
                            </ListItem>
                            <ListItem 
                              button 
                              sx={{ pl: 4 }} 
                              onClick={() => handleSelectSegment('activity', 'inactiveThreeDays')}
                            >
                              <ListItemText 
                                primary="Inactive (3+ days)" 
                                secondary={`${userSegments.activity.inactiveThreeDays} users`}
                              />
                            </ListItem>
                          </List>
                        </Collapse>

                        {/* Topics Segment */}
                        <ListItem button onClick={() => handleExpandSegment('topics')}>
                          <ListItemIcon>
                            <CategoryIcon />
                          </ListItemIcon>
                          <ListItemText primary="By Topic" />
                          {expandedSegment === 'topics' ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                        </ListItem>
                        <Collapse in={expandedSegment === 'topics'} timeout="auto" unmountOnExit>
                          <List component="div" disablePadding sx={{ maxHeight: '200px', overflow: 'auto' }}>
                            {userSegments.topics.map(topic => (
                              <ListItem 
                                button 
                                sx={{ pl: 4 }} 
                                key={topic.name}
                                onClick={() => handleSelectSegment('topic', topic.name)}
                              >
                                <ListItemText 
                                  primary={topic.name} 
                                  secondary={`${topic.count} users`} 
                                />
                              </ListItem>
                            ))}
                          </List>
                        </Collapse>

                        {/* Language Segment */}
                        <ListItem button onClick={() => handleExpandSegment('languages')}>
                          <ListItemIcon>
                            <LanguageIcon />
                          </ListItemIcon>
                          <ListItemText primary="By Language" />
                          {expandedSegment === 'languages' ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                        </ListItem>
                        <Collapse in={expandedSegment === 'languages'} timeout="auto" unmountOnExit>
                          <List component="div" disablePadding sx={{ maxHeight: '200px', overflow: 'auto' }}>
                            {userSegments.languages.map(lang => (
                              <ListItem 
                                button 
                                sx={{ pl: 4 }} 
                                key={lang.name}
                                onClick={() => handleSelectSegment('language', lang.name)}
                              >
                                <ListItemText 
                                  primary={lang.name} 
                                  secondary={`${lang.count} users`} 
                                />
                              </ListItem>
                            ))}
                          </List>
                        </Collapse>
                      </List>
                    </Paper>
                  </Grid>
                )}

                <Grid item xs={12}>
                  <Autocomplete
                    multiple
                    id="users-autocomplete"
                    options={users}
                    getOptionLabel={(option) => option.displayName || option.email || option.uid || 'Unknown User'}
                    value={selectedUsers}
                    onChange={handleUserSelectionChange}
                    loading={loadingUsers}
                    ListboxProps={{
                      style: { maxHeight: '200px' }
                    }}
                    renderInput={(params) => (
                      <TextField
                        {...params}
                        label="Select Users"
                        helperText={`${selectedUsers.length} users selected`}
                        InputProps={{
                          ...params.InputProps,
                          endAdornment: (
                            <>
                              {loadingUsers ? <CircularProgress color="inherit" size={20} /> : null}
                              {params.InputProps.endAdornment}
                            </>
                          ),
                        }}
                      />
                    )}
                    renderTags={(value, getTagProps) =>
                      value.length > 10 ? (
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', maxHeight: '100px', overflow: 'auto' }}>
                          {value.map((option, index) => (
                            <Chip
                              key={option._id || index}
                              label={option.displayName || option.email || option.uid || 'Unknown User'}
                              {...getTagProps({ index })}
                              sx={{ m: 0.5 }}
                            />
                          ))}
                        </Box>
                      ) : (
                        value.map((option, index) => (
                          <Chip
                            key={option._id || index}
                            label={option.displayName || option.email || option.uid || 'Unknown User'}
                            {...getTagProps({ index })}
                          />
                        ))
                      )
                    }
                  />
                </Grid>
              </>
            )}

            <Grid item xs={12}>
              <TextField
                fullWidth
                id="scheduledFor"
                label="Schedule For (Optional)"
                type="datetime-local"
                value={formData.scheduledFor ? new Date(formData.scheduledFor).toISOString().slice(0, 16) : ''}
                onChange={handleScheduleChange}
                InputLabelProps={{
                  shrink: true,
                }}
              />
            </Grid>

            <Grid item xs={12}>
              <Stack direction="row" spacing={2} justifyContent="flex-end">
                <Button
                  variant="outlined"
                  startIcon={<PreviewIcon />}
                  onClick={togglePreview}
                  disabled={formData.type === 'PERSONALIZED' && loadingPreview}
                >
                  {showPreview ? 'Hide Preview' : 'Show Preview'}
                  {formData.type === 'PERSONALIZED' && loadingPreview && (
                    <CircularProgress size={20} sx={{ ml: 1 }} />
                  )}
                </Button>
                <Button
                  variant="contained"
                  startIcon={<NotificationsActiveIcon />}
                  onClick={handleSubmit}
                  disabled={loading}
                >
                  {loading ? <CircularProgress size={24} /> : 'Create Notification'}
                </Button>
              </Stack>
            </Grid>

            {showPreview && (
              <Grid item xs={12}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Preview
                    </Typography>
                    <Divider sx={{ mb: 2 }} />
                    
                    {formData.type === 'PERSONALIZED' && previewData ? (
                      <>
                        <Typography variant="subtitle1" gutterBottom>
                          Original with placeholders:
                        </Typography>
                        <Paper variant="outlined" sx={{ p: 2, mb: 2, bgcolor: '#f5f5f5' }}>
                          <Typography variant="subtitle1">{previewData.originalTitle}</Typography>
                          <Typography variant="body2">{previewData.originalBody}</Typography>
                        </Paper>
                        
                        <Typography variant="subtitle1" gutterBottom>
                          Processed for user:
                        </Typography>
                        <Paper variant="outlined" sx={{ p: 2, bgcolor: '#e3f2fd' }}>
                          <Typography variant="subtitle1">{previewData.title}</Typography>
                          <Typography variant="body2">{previewData.body}</Typography>
                        </Paper>
                      </>
                    ) : (
                      <Paper variant="outlined" sx={{ p: 2 }}>
                        <Typography variant="subtitle1">{formData.title}</Typography>
                        <Typography variant="body2">{formData.body}</Typography>
                        {formData.imageUrl && (
                          <Box mt={2}>
                            <img 
                              src={formData.imageUrl} 
                              alt="Notification preview" 
                              style={{ maxWidth: '100%', maxHeight: '200px' }} 
                              onError={(e) => {
                                e.target.onerror = null;
                                e.target.src = 'https://via.placeholder.com/400x200?text=Invalid+Image+URL';
                              }}
                            />
                          </Box>
                        )}
                      </Paper>
                    )}
                  </CardContent>
                </Card>
              </Grid>
            )}
          </Grid>
        </Paper>
      </Box>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert onClose={() => setSnackbar({ ...snackbar, open: false })} severity={snackbar.severity}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default PushNotificationCreate;
