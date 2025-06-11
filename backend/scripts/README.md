# User Model Test Scripts

This directory contains scripts for testing the User model and API routes with the new Firebase UID integration.

## Available Scripts

1. `sample-users.js` - Creates sample users with various configurations including Firebase UIDs, preferences, likes, and favorites.
2. `test-user-model.js` - Tests the User model methods and properties directly.
3. `test-user-routes.js` - Tests the User API routes with both Firebase UID and device ID.
4. `run-user-tests.sh` - Shell script to run all tests in sequence (Unix/Linux/Mac only).

## Running the Tests

### Prerequisites

- MongoDB connection string in your `.env` file
- Node.js installed
- Server running (for route tests)

### For Windows Users

Run each script individually:

```bash
# Create sample users
node backend/scripts/sample-users.js

# Test user model
node backend/scripts/test-user-model.js

# Test user routes (requires server to be running)
node backend/scripts/test-user-routes.js
```

### For Unix/Linux/Mac Users

You can run all tests in sequence:

```bash
# Make the script executable
chmod +x backend/scripts/run-user-tests.sh

# Run all tests
./backend/scripts/run-user-tests.sh
```

## Sample Data

The `sample-users.js` script creates three users:

1. **Full Firebase User** - Has Firebase UID, device ID, and all profile fields
2. **Device-Only User** - Has only device ID, no Firebase authentication
3. **Minimal Firebase User** - Has Firebase UID and device ID with minimal profile

These users have various combinations of:
- Preferences (theme, language, timezone)
- Push notification settings
- Template interactions (likes, favorites, recent templates)
- Category visit history
- Engagement logs

## Test Coverage

The tests cover:

1. **User Creation** - Creating users with both Firebase UID and device ID
2. **User Retrieval** - Finding users by both Firebase UID and device ID
3. **Preference Updates** - Updating user preferences and settings
4. **Template Interactions** - Recording likes, favorites, and template usage
5. **Category Tracking** - Recording category visits and sources
6. **API Integration** - Testing all API endpoints with both identifier types

## Manual Verification

After running the tests, you can manually verify the data in your MongoDB database:

```javascript
// Connect to your MongoDB and run:
db.users.find({ 
  $or: [
    { deviceId: /test_device_id/ }, 
    { uid: /test_firebase_uid/ }
  ]
}).pretty()
```

This will show all test users created by the scripts.