#!/bin/bash

echo "Deploying backend fix for subscription validation errors..."

# Navigate to backend directory
cd backend

# Add all changes
git add -A

# Commit the changes
git commit -m "Fix: Remove planLevel field from subscription schema to eliminate validation errors

- Removed planLevel field from User.subscription schema
- Updated cleanupSubscriptionData function to remove deprecated planLevel field from existing documents
- Applied cleanup before saving user documents in profile, register, and session update endpoints
- This completely eliminates MongoDB validation errors for the planLevel field"

# Push to main branch (this will trigger Render deployment)
git push origin main

echo "Backend fix deployed! The changes will be live on Render in a few minutes."
echo ""
echo "Summary of changes:"
echo "- Removed planLevel field from User.subscription schema"
echo "- Updated cleanupSubscriptionData() function to remove deprecated planLevel field"
echo "- Applied cleanup in POST /api/users/profile endpoint"
echo "- Applied cleanup in POST /api/users/:uid/sessions/update endpoint"
echo "- Applied cleanup in POST /api/users/register endpoint"
echo ""
echo "This completely eliminates the MongoDB validation errors for subscription.planLevel field." 