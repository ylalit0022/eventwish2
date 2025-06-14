# EventWish Admin Panel

A React-based admin panel for the EventWish application with Firebase Google Sign-In authentication.

## Features

- Firebase Google Sign-In authentication
- Role-based access control
- Responsive design using Material UI
- User management
- Dashboard with statistics
- Integration with EventWish backend API

## Setup

### Prerequisites

- Node.js (v14+)
- npm or yarn
- EventWish backend server running

### Installation

1. Install dependencies:

```bash
cd backend/admin-panel
npm install
```

2. Create a `.env` file in the `admin-panel` directory with the following content:

```
REACT_APP_API_URL=/api
REACT_APP_FIREBASE_API_KEY=your_firebase_api_key
REACT_APP_FIREBASE_AUTH_DOMAIN=your_firebase_auth_domain
REACT_APP_FIREBASE_PROJECT_ID=your_firebase_project_id
REACT_APP_FIREBASE_STORAGE_BUCKET=your_firebase_storage_bucket
REACT_APP_FIREBASE_MESSAGING_SENDER_ID=your_firebase_messaging_sender_id
REACT_APP_FIREBASE_APP_ID=your_firebase_app_id
```

Replace the placeholder values with your actual Firebase configuration.

### Development

To start the development server:

```bash
npm start
```

This will start the development server on [http://localhost:3000](http://localhost:3000).

### Production Build

To create a production build:

```bash
npm run build
```

This will create a `build` directory with optimized production files.

## Authentication

The admin panel uses Firebase Google Sign-In for authentication. Only users with admin roles can access the panel. Admin roles are defined in the `adminConfig.js` file in the backend.

## Deployment

1. Build the admin panel:

```bash
npm run build
```

2. The backend server is configured to serve the admin panel at `/admin` path.

3. Access the admin panel at `http://your-server-url/admin`.

## Project Structure

- `src/` - Source code
  - `components/` - Reusable components
  - `contexts/` - React contexts for state management
  - `layouts/` - Layout components
  - `pages/` - Page components
  - `App.js` - Main application component
  - `index.js` - Entry point
  - `firebase.js` - Firebase configuration
  - `api.js` - API service functions

## Available Scripts

- `npm start` - Start development server
- `npm run build` - Create production build

## License

This project is proprietary and confidential. Unauthorized copying or distribution is prohibited. 