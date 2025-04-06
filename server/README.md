# EventWish Backend API

Backend server for the EventWish Android application, providing API endpoints for authentication, templates, and wishes.

## Features

- Firebase Phone Authentication integration
- JWT-based authentication for API requests
- User management with MongoDB
- Template and wish management APIs
- Security with Helmet, CORS, and proper input validation

## Prerequisites

- Node.js 14+ and npm
- MongoDB 4.4+
- Firebase project with Phone Authentication enabled
- Firebase Admin SDK service account credentials

## Installation

1. Clone the repository
2. Install dependencies

```bash
cd server
npm install
```

3. Copy `.env.example` to `.env` and update the environment variables:

```bash
cp .env.example .env
```

4. Add your Firebase service account credentials to `config/firebase-service-account.json`:

You can download the service account JSON file from the Firebase Console:
- Go to Project Settings > Service Accounts
- Click "Generate new private key"
- Save the file to `config/firebase-service-account.json`

## Running the Server

Development mode with nodemon:

```bash
npm run dev
```

Production mode:

```bash
npm start
```

## API Routes

### Authentication

- `POST /api/auth/register` - Register a new user with Firebase phone authentication
- `POST /api/auth/login` - Login with phone number and password
- `POST /api/auth/refresh-token` - Refresh JWT token
- `POST /api/auth/logout` - Logout user (requires authentication)
- `POST /api/auth/change-password` - Change user password (requires authentication)
- `GET /api/auth/me` - Get current user info (requires authentication)

### Templates

- `GET /api/templates` - Get all templates
- `GET /api/templates/:id` - Get template by ID
- `GET /api/templates/category/:categoryId` - Get templates by category

### Wishes

- `POST /api/wishes/create` - Create a new wish
- `GET /api/wishes/:shortCode` - Get wish by short code

## Error Handling

The API returns appropriate HTTP status codes and error messages in JSON format:

```json
{
  "error": {
    "message": "Error message",
    "status": 400
  }
}
```

## Security

- JWT tokens expire after 1 hour by default
- Refresh tokens expire after 7 days by default
- Firebase ID tokens are verified on registration
- Passwords are hashed with bcrypt
- CORS and Helmet are configured for security
- Input validation for all requests 