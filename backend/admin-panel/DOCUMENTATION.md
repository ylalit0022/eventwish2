# EventWish Admin Panel Documentation

## Overview

The EventWish Admin Panel is a web-based application built with React and Material UI that provides administrative capabilities for managing the EventWish platform. It allows administrators to view and manage users, monitor engagement, and perform administrative actions.

## Table of Contents

1. [Architecture](#architecture)
2. [File Structure](#file-structure)
3. [Authentication](#authentication)
4. [Key Features](#key-features)
5. [Pages and Components](#pages-and-components)
6. [API Integration](#api-integration)
7. [Development Guide](#development-guide)
8. [Deployment](#deployment)

## Architecture

The admin panel follows a modern React architecture with the following key technologies:

- **React**: Frontend library for building the user interface
- **Material UI**: Component library for consistent design
- **React Router**: For navigation between different sections
- **Axios**: For API communication
- **Firebase**: For authentication
- **Chart.js**: For data visualization

The application uses a component-based architecture with reusable UI elements and follows a centralized state management approach for authentication and global state.

## File Structure

```
backend/admin-panel/
├── public/                 # Static assets
├── src/
│   ├── api.js              # API client and endpoints
│   ├── App.js              # Main application component
│   ├── firebase.js         # Firebase configuration and auth utilities
│   ├── index.js            # Application entry point
│   ├── contexts/           # React contexts for state management
│   │   └── AuthContext.js  # Authentication context
│   ├── components/         # Reusable UI components
│   │   ├── Header.js       # Application header
│   │   ├── Sidebar.js      # Navigation sidebar
│   │   ├── PrivateRoute.js # Route protection component
│   │   └── ...
│   └── pages/              # Page components
│       ├── Login.js        # Authentication page
│       ├── Dashboard.js    # Main dashboard
│       ├── Users.js        # Users listing page
│       ├── UserDetail.js   # User details page
│       └── ...
├── package.json            # Dependencies and scripts
└── webpack.config.js       # Build configuration
```

## Authentication

The admin panel uses Firebase Authentication for secure access:

1. **Login Flow**: Administrators log in using their email and password
2. **Token Management**: Firebase tokens are used for API authentication
3. **Route Protection**: Private routes ensure only authenticated users can access admin features
4. **Development Mode**: Special development authentication is available for local testing

Authentication state is managed through the AuthContext, which provides the current user and authentication methods to all components.

## Key Features

### User Management

- View a paginated list of all users
- Search and filter users by various criteria
- View detailed user information
- Edit user profiles and preferences
- Block/unblock users with reason tracking

### User Detail View

The UserDetail page provides comprehensive information about users, organized in tabs:

1. **Profile Tab**: Basic user information and status
   - Display name, email, device ID, creation date, last online
   - Block/unblock functionality
   - Referral information
   - Topic subscriptions
   - Ads control settings

2. **Preferences Tab**: User preferences and settings
   - Theme preference
   - Language preference
   - Timezone
   - Push notification settings

3. **Subscription Tab**: Subscription details
   - Subscription status (active/inactive)
   - Plan type (Monthly, Quarterly, Half Yearly, Yearly)
   - Start and expiration dates

4. **Templates Tab**: Template usage information
   - Recent templates used
   - Favorite templates count
   - Liked templates count

5. **Categories Tab**: Category visit tracking
   - Categories visited
   - Visit counts
   - Last visit dates
   - Visit sources

6. **Activity Tab**: User engagement tracking
   - Table of all engagement activities with pagination
   - Filtering by action type (LIKE, UNLIKE, FAV, UNFAV, VIEW, SHARE)
   - Visual indicators for different action types
   - Activity summary with counts by action type

### Dashboard

- Overview of platform statistics
- User growth metrics
- Engagement analytics
- Visual charts and graphs

## Pages and Components

### UserDetail.js

The UserDetail page is a comprehensive view of a single user's information and provides administrative capabilities:

#### State Management
- User data fetching and storage
- Edit mode toggling
- Tab navigation
- Pagination for activity logs
- Action filtering

#### Key Components
1. **User Header**: Profile photo, name, email, and status indicators
2. **Action Buttons**: Edit, Save, Block/Unblock
3. **Tab Navigation**: For organizing different aspects of user data
4. **Editable Fields**: Text fields, dropdowns, switches for data editing
5. **Activity Table**: Paginated table of user engagement with filtering
6. **Block Dialog**: Confirmation dialog for blocking users

#### Features
- **Edit Mode**: Toggle between view and edit modes
- **Field Validation**: Ensure data integrity during edits
- **Complex Data Handling**: Support for nested objects, arrays, and dates
- **Responsive Design**: Works on various screen sizes
- **Visual Feedback**: Loading indicators, error messages, success notifications

## API Integration

The admin panel communicates with the backend through a centralized API client (`api.js`):

### Key Endpoints

- `GET /api/admin/users`: Fetch paginated user list
- `GET /api/admin/users/:uid`: Fetch single user details
- `PUT /api/admin/users/:uid`: Update user information
- `POST /api/admin/users/:uid/block`: Block a user
- `POST /api/admin/users/:uid/unblock`: Unblock a user
- `GET /api/admin/dashboard/stats`: Fetch dashboard statistics

### Authentication

All API requests include the Firebase authentication token in the Authorization header:

```javascript
// Example from api.js
api.interceptors.request.use(async (config) => {
  const token = await getAuthToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

## Development Guide

### Prerequisites

- Node.js (v14+)
- npm or yarn
- Firebase project with authentication configured

### Setup

1. Clone the repository
2. Navigate to the admin panel directory: `cd backend/admin-panel`
3. Install dependencies: `npm install`
4. Configure Firebase: Update `firebase.js` with your Firebase project credentials
5. Start the development server: `npm start`

### Adding New Features

1. **New Page**:
   - Create a new file in the `pages` directory
   - Add the route in `App.js`
   - Ensure proper authentication checks

2. **New API Endpoint**:
   - Add the endpoint function in `api.js`
   - Follow the established pattern for error handling and authentication

3. **New Component**:
   - Create reusable components in the `components` directory
   - Use Material UI for consistent styling

### Best Practices

- Use functional components with hooks
- Implement proper error handling for API calls
- Follow Material UI design patterns
- Implement responsive designs for all screen sizes
- Add loading states and error messages for better UX

## Deployment

The admin panel can be deployed using the following steps:

1. Build the production bundle: `npm run build`
2. Deploy the contents of the `dist` directory to your hosting provider
3. Ensure proper environment variables are set for production

### Security Considerations

- Ensure Firebase security rules are properly configured
- Implement IP restrictions for admin access if possible
- Use environment variables for sensitive configuration
- Regularly audit admin access and permissions

## User Detail Page Implementation Details

The UserDetail page (`UserDetail.js`) is one of the most complex components in the admin panel. It provides a comprehensive view of user data with editing capabilities.

### Component Structure

```jsx
const UserDetail = () => {
  // State variables for user data, loading, errors, etc.
  
  // Data fetching effect
  useEffect(() => {
    fetchUser();
  }, [uid]);
  
  // Helper functions for data manipulation
  
  // Event handlers for user interactions
  
  // Render different sections based on state
  return (
    <Box>
      {/* Header section with user info and actions */}
      <Paper>...</Paper>
      
      {/* Tab navigation */}
      <Tabs>...</Tabs>
      
      {/* Tab panels for different data sections */}
      <TabPanel>...</TabPanel>
      
      {/* Dialogs for confirmations */}
      <Dialog>...</Dialog>
    </Box>
  );
};
```

### Key Implementation Features

#### 1. Edit Mode

The page implements a toggle between view and edit modes:

```javascript
const handleEditToggle = () => {
  if (editMode) {
    setEditedUser(user); // Reset changes
  }
  setEditMode(!editMode);
};
```

In edit mode, fields are rendered as form inputs; in view mode, they're displayed as text.

#### 2. Field Change Handling

The page handles different types of field changes:

```javascript
// Simple field changes
const handleFieldChange = (field, value) => {
  setEditedUser(prev => ({
    ...prev,
    [field]: value
  }));
};

// Nested object field changes
const handleNestedFieldChange = (parent, field, value) => {
  setEditedUser(prev => ({
    ...prev,
    [parent]: {
      ...prev[parent],
      [field]: value
    }
  }));
};

// Array field changes (e.g., topic subscriptions)
const handleTopicSubscriptionsChange = (value) => {
  const topicsArray = value.split(',')
    .map(topic => topic.trim())
    .filter(topic => topic.length > 0);
  
  setEditedUser(prev => ({
    ...prev,
    topicSubscriptions: topicsArray
  }));
};
```

#### 3. Date Handling

The page includes utilities for formatting and parsing dates:

```javascript
// Format date for input fields
const formatDateForInput = (dateString) => {
  if (!dateString) return '';
  
  try {
    const date = new Date(dateString);
    return date.toISOString().slice(0, 16); // YYYY-MM-DDTHH:MM
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
```

#### 4. Activity Log Grouping and Filtering

The Activity tab implements grouping and filtering of engagement logs:

```javascript
// Group engagement logs by action type
const getGroupedEngagementLogs = () => {
  if (!user?.engagementLog || user.engagementLog.length === 0) {
    return { LIKE: [], UNLIKE: [], FAV: [], UNFAV: [], VIEW: [], SHARE: [] };
  }

  return user.engagementLog.reduce((groups, entry) => {
    const action = entry.action;
    if (!groups[action]) {
      groups[action] = [];
    }
    groups[action].push(entry);
    return groups;
  }, { LIKE: [], UNLIKE: [], FAV: [], UNFAV: [], VIEW: [], SHARE: [] });
};

// Get filtered engagement logs based on active filter
const getFilteredEngagementLogs = () => {
  const grouped = getGroupedEngagementLogs();
  
  if (activeFilter === 'ALL') {
    return user?.engagementLog || [];
  }
  
  return grouped[activeFilter] || [];
};
```

#### 5. Pagination Implementation

The Activity tab implements pagination for potentially large engagement logs:

```javascript
// Handle page change for activity table
const handleChangePage = (event, newPage) => {
  setPage(newPage);
};

// Handle rows per page change for activity table
const handleChangeRowsPerPage = (event) => {
  setRowsPerPage(parseInt(event.target.value, 10));
  setPage(0);
};

// In the render:
<TablePagination
  rowsPerPageOptions={[5, 10, 25, 50]}
  component="div"
  count={getFilteredEngagementLogs().length}
  rowsPerPage={rowsPerPage}
  page={page}
  onPageChange={handleChangePage}
  onRowsPerPageChange={handleChangeRowsPerPage}
/>
```

This documentation provides a comprehensive overview of the EventWish Admin Panel, with special focus on the UserDetail page implementation. It covers the architecture, file structure, key features, and development guidelines to help developers understand and extend the admin panel functionality. 