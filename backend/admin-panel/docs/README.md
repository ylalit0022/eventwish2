# EventWish Admin Panel Documentation

This document provides a comprehensive guide to the EventWish Admin Panel, including its architecture, components, routes, APIs, and features. It serves as a reference for developers working on the project.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Server Configuration](#server-configuration)
4. [Dependencies](#dependencies)
5. [Authentication](#authentication)
6. [API Structure](#api-structure)
7. [Components](#components)
8. [Routes](#routes)
9. [Models](#models)
10. [Features](#features)
11. [Workflow Examples](#workflow-examples)
12. [Development Guidelines](#development-guidelines)

## Overview

The EventWish Admin Panel is a React-based web application that provides an interface for administrators to manage the EventWish platform. It allows administrators to manage templates, users, shared wishes, category icons, festivals, and other content.

## Architecture

The admin panel follows a client-server architecture:

- **Frontend**: React application with Material-UI components
- **Backend**: Node.js/Express API server connected to MongoDB
- **Authentication**: Firebase Authentication with custom token verification

### Directory Structure

```
backend/admin-panel/
├── public/                 # Static assets
├── src/
│   ├── components/         # Reusable UI components
│   ├── pages/              # Page components
│   ├── layouts/            # Layout components
│   ├── App.js              # Main application component
│   ├── api.js              # API client functions
│   ├── firebase.js         # Firebase configuration
│   ├── index.js            # Application entry point
│   └── theme.js            # Material-UI theme configuration
├── package.json            # Dependencies and scripts
└── webpack.config.js       # Webpack configuration
```

## Server Configuration

The admin panel is served by an Express server that also handles API requests. In development mode, webpack-dev-server is used with a proxy configuration to forward API requests to the backend server.

### Development Server

The development server is configured in `webpack.config.js` with the following key settings:

```javascript
devServer: {
  port: 3000,
  historyApiFallback: true,
  proxy: {
    '/api': 'http://localhost:5000'
  }
}
```

### Production Server

In production, the admin panel is built as static files and served by the Express server. The server is configured to handle API requests and serve the admin panel from the `build` directory.

## Dependencies

### Frontend Dependencies

Key dependencies used in the admin panel:

```json
{
  "dependencies": {
    "@emotion/react": "^11.10.5",
    "@emotion/styled": "^11.10.5",
    "@mui/icons-material": "^5.11.0",
    "@mui/material": "^5.11.0",
    "axios": "^1.2.1",
    "firebase": "^9.15.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.4.5",
    "react-quill": "^2.0.0"
  },
  "devDependencies": {
    "@babel/core": "^7.20.5",
    "@babel/preset-env": "^7.20.2",
    "@babel/preset-react": "^7.18.6",
    "babel-loader": "^9.1.0",
    "css-loader": "^6.7.3",
    "html-webpack-plugin": "^5.5.0",
    "style-loader": "^3.3.1",
    "webpack": "^5.75.0",
    "webpack-cli": "^5.0.1",
    "webpack-dev-server": "^4.11.1"
  }
}
```

### Backend Dependencies

Key backend dependencies:

```json
{
  "dependencies": {
    "bcryptjs": "^2.4.3",
    "cors": "^2.8.5",
    "dotenv": "^16.0.3",
    "express": "^4.18.2",
    "firebase-admin": "^11.3.0",
    "mongoose": "^6.8.0",
    "multer": "^1.4.5-lts.1",
    "winston": "^3.8.2",
    "json2csv": "^5.0.7"
  }
}
```

## Authentication

The admin panel uses Firebase Authentication for user authentication. The authentication flow is as follows:

1. User logs in with email/password through Firebase Authentication
2. Frontend receives Firebase ID token
3. Token is sent with each API request in the Authorization header
4. Backend verifies the token using Firebase Admin SDK
5. Backend checks if the user has admin privileges
6. If authorized, the request is processed

### Development Mode Authentication

For development, there's a special development mode that can be enabled by setting `localStorage.setItem('devMode', 'true')`. This allows bypassing Firebase Authentication during development.

## API Structure

The API client is defined in `src/api.js` and uses Axios for making HTTP requests. All API functions follow a consistent pattern:

```javascript
export const someApiFunction = async (params) => {
  try {
    const response = await api.method(`/endpoint`, data);
    return response.data;
  } catch (error) {
    console.error('Error message:', error);
    throw error;
  }
};
```

The API base URL is configured to `/api` and is automatically prefixed to all API requests.

## Components

### Layout Components

- **MainLayout**: Main layout with navigation sidebar, app bar, and content area

### Page Components

- **Dashboard**: Dashboard with statistics and notifications
- **Users**: User management
- **Templates**: Template management
- **SharedWishes**: Shared wishes management
- **CategoryIcons**: Category icon management
- **UpcomingFestivals**: Festival management
- **Abouts**: About page content management
- **Contacts**: Contact page content management
- **AdMobs**: AdMob management

### Detail/Create Components

For each main entity, there are corresponding detail and create components:

- **TemplateDetail/TemplateCreate**: View/edit/create templates
- **UserDetail**: View/edit user details
- **SharedWishDetail**: View/edit shared wishes
- **CategoryIconDetail/CategoryIconCreate**: View/edit/create category icons
- **UpcomingFestivalDetail/UpcomingFestivalCreate**: View/edit/create festivals
- **AboutDetail/AboutCreate**: View/edit/create about content
- **ContactDetail/ContactCreate**: View/edit/create contact content
- **AdMobDetail/AdMobCreate**: View/edit/create AdMob entries

## Routes

The application routes are defined in `App.js`:

```javascript
<Routes>
  <Route path="/" element={<MainLayout />}>
    <Route index element={<Dashboard />} />
    <Route path="users" element={<Users />} />
    <Route path="users/:id" element={<UserDetail />} />
    <Route path="templates" element={<Templates />} />
    <Route path="templates/create" element={<TemplateCreate />} />
    <Route path="templates/:id" element={<TemplateDetail />} />
    <Route path="shared-wishes" element={<SharedWishes />} />
    <Route path="shared-wishes/:id" element={<SharedWishDetail />} />
    <Route path="category-icons" element={<CategoryIcons />} />
    <Route path="category-icons/create" element={<CategoryIconCreate />} />
    <Route path="category-icons/:id" element={<CategoryIconDetail />} />
    <Route path="upcoming-festivals" element={<UpcomingFestivals />} />
    <Route path="upcoming-festivals/create" element={<UpcomingFestivalCreate />} />
    <Route path="upcoming-festivals/:id" element={<UpcomingFestivalDetail />} />
    <Route path="about" element={<Abouts />} />
    <Route path="about/create" element={<AboutCreate />} />
    <Route path="about/:id" element={<AboutDetail />} />
    <Route path="contacts" element={<Contacts />} />
    <Route path="contacts/create" element={<ContactCreate />} />
    <Route path="contacts/:id" element={<ContactDetail />} />
    <Route path="admob" element={<AdMobs />} />
    <Route path="admob/create" element={<AdMobCreate />} />
    <Route path="admob/:id" element={<AdMobDetail />} />
  </Route>
  <Route path="/login" element={<Login />} />
</Routes>
```

## Models

The admin panel interacts with the following MongoDB models:

### Template

Represents a template for creating wishes:

```javascript
{
  title: String,
  category: String,
  htmlContent: String,
  cssContent: String,
  jsContent: String,
  previewUrl: String,
  status: Boolean,
  isPremium: Boolean,
  creatorId: ObjectId (ref: 'User'),
  festivalTag: String,
  tags: [String],
  usageCount: Number,
  likes: Number,
  favorites: Number,
  categoryIcon: String (URL)
}
```

### SharedWish

Represents a wish shared by a user:

```javascript
{
  shortCode: String,
  template: ObjectId (ref: 'Template'),
  title: String,
  description: String,
  recipientName: String,
  senderName: String,
  customizedHtml: String,
  cssContent: String,
  jsContent: String,
  previewUrl: String,
  sharedVia: String,
  views: Number,
  uniqueViews: Number,
  viewerIps: [String],
  shareCount: Number,
  shareHistory: [{ platform: String, timestamp: Date }],
  lastSharedAt: Date,
  conversionSource: String,
  referrer: String,
  deviceInfo: String,
  deeplink: String,
  viewerEngagement: [{
    userId: ObjectId (ref: 'User'),
    action: String,
    timestamp: Date
  }],
  isPremiumShared: Boolean
}
```

### CategoryIcon

Represents an icon for a category:

```javascript
{
  id: String,
  category: String,
  categoryIcon: String (URL),
  iconType: String,
  resourceName: String
}
```

### Festival

Represents a festival:

```javascript
{
  name: String,
  slug: String,
  date: Date,
  startDate: Date,
  endDate: Date,
  description: String,
  category: String,
  categoryIcon: ObjectId (ref: 'CategoryIcon'),
  imageUrl: String,
  bannerUrl: String,
  templates: [ObjectId (ref: 'Template')],
  isActive: Boolean,
  status: String,
  priority: Number,
  deepLink: String,
  pushEnabled: Boolean,
  personalizedPushTemplate: String,
  notifyCountdown: Boolean,
  countdownDays: Number,
  localizedNames: Map,
  themeColors: Map
}
```

### About

Represents content for the About page:

```javascript
{
  title: String,
  htmlCode: String,
  isActive: Boolean
}
```

### Contact

Represents content for the Contact page:

```javascript
{
  title: String,
  htmlCode: String,
  isActive: Boolean
}
```

### AdMob

Represents an AdMob ad:

```javascript
{
  adName: String,
  adUnitCode: String,
  adType: String,
  status: Boolean,
  targetingCriteria: Map,
  targetSegments: [ObjectId],
  targetingPriority: Number,
  parameters: Map,
  impressions: Number,
  clicks: Number,
  ctr: Number,
  revenue: Number,
  impressionData: Array,
  clickData: Array,
  revenueData: Array,
  segmentPerformance: Map,
  displaySettings: {
    maxImpressionsPerDay: Number,
    minIntervalBetweenAds: Number,
    cooldownPeriod: Number
  },
  deviceSettings: Map
}
```

## Features

### Template Management

- List templates with filtering, sorting, and pagination
- View, create, edit, and delete templates
- Toggle template status
- Import/export templates via CSV
- Set template as premium
- Add tags for searchability
- Associate templates with festivals
- Track usage metrics (usage count, likes, favorites)

#### CategoryIcon Integration

Templates can be associated with category icons. The CategoryIcon field in the template form allows selecting from available icons and displays a preview of the selected icon.

### User Management

- List users with filtering, sorting, and pagination
- View and edit user details
- Manage user subscriptions
- Track user activity
- Control ad settings
- Manage topic subscriptions
- View referral information

### Shared Wish Management

- List shared wishes with filtering, sorting, and pagination
- View and edit shared wish details
- Track engagement metrics (views, shares)
- View analytics dashboard with filtering options
- Display user email for engagement records

### Category Icon Management

- List category icons with filtering, sorting, and pagination
- View, create, edit, and delete category icons
- Toggle category icon status
- Preview category icons

### Festival Management

- List festivals with filtering, sorting, and pagination
- View, create, edit, and delete festivals
- Toggle festival status
- Set festival dates, description, and images
- Associate festivals with templates and category icons
- Configure notification settings
- Set localized names and theme colors

### About/Contact Management

- List about/contact entries
- View, create, edit, and delete about/contact entries
- Toggle active status (only one can be active at a time)
- Edit HTML content with preview

### AdMob Management

- List AdMob ads with filtering, sorting, and pagination
- View, create, edit, and delete AdMob ads
- Toggle ad status
- Configure targeting criteria and display settings
- Track performance metrics

## Workflow Examples

### Template Management Workflow

1. **List Templates**:
   - Navigate to `/templates`
   - View list of templates with filtering options
   - Use status toggle to activate/deactivate templates

2. **Create Template**:
   - Click "Add Template" button
   - Fill in template details across tabs (Basic Info, Content, Display, Metadata)
   - Select category icon from dropdown
   - Click "Create Template"

3. **Edit Template**:
   - Click edit icon for a template
   - Toggle edit mode with "Edit" button
   - Make changes across tabs
   - Click "Save Changes"

4. **Delete Template**:
   - Click delete icon for a template
   - Confirm deletion in dialog

5. **Import/Export Templates**:
   - Click "Export CSV" to download templates as CSV
   - Click "Import CSV" to upload templates from CSV

### CategoryIcon Integration

1. **In TemplateDetail.js**:
   - The Display tab includes a CategoryIcon dropdown
   - Icons are fetched using `getCategoryIcons` API
   - When an icon is selected, its URL is stored in the template

2. **In TemplateCreate.js**:
   - Similar implementation to TemplateDetail.js
   - Icons are displayed with small previews in the dropdown
   - Selected icon URL is stored in the template

## Development Guidelines

### Adding a New Feature

1. **Update Models**:
   - Add new fields to the relevant MongoDB model
   - Update validation rules if needed

2. **Add API Functions**:
   - Add new functions to `api.js` for the feature
   - Follow the existing pattern for consistency

3. **Add Backend Routes**:
   - Add new routes to `adminRoutes.js`
   - Implement proper authentication and validation

4. **Create/Update Components**:
   - Create new components or update existing ones
   - Follow Material-UI patterns for consistency

5. **Update Routes**:
   - Add new routes to `App.js` if needed

### Best Practices

1. **Error Handling**:
   - Always include proper error handling in API functions
   - Display meaningful error messages to users

2. **Loading States**:
   - Show loading indicators during API calls
   - Disable buttons during loading

3. **Validation**:
   - Validate inputs on both client and server
   - Show validation errors to users

4. **Authentication**:
   - Ensure all API routes are protected
   - Check admin privileges for sensitive operations

5. **Code Style**:
   - Follow consistent naming conventions
   - Use meaningful variable and function names
   - Add comments for complex logic

### Running the Application

1. **Development Mode**:
   ```bash
   npm run dev
   ```

2. **Production Build**:
   ```bash
   npm run build
   ```

3. **Start Production Server**:
   ```bash
   npm start
   ```

## Conclusion

This documentation provides a comprehensive overview of the EventWish Admin Panel. For specific implementation details, refer to the source code and comments within the files. 