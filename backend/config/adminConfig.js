/**
 * Admin Configuration
 * 
 * This file contains configuration for admin functionality including:
 * - List of authorized admin users
 * - Role definitions and permissions
 */

// Admin user email whitelists by role
const adminUsers = {
  // Super admins have full access to all functionality
  superAdmins: [
    'admin@eventwish.com',
    'superadmin@example.com',
    'hpw@gmail.com',
    'ylalit0022@gmail.com' // Ensure user's email is here
  ],
  
  // Content admins can manage content but not system settings or users
  contentAdmins: [
    'content@eventwish.com',
    'editor@example.com'
  ],
  
  // User admins can manage user accounts but not content or system settings
  userAdmins: [
    'useradmin@eventwish.com',
    'usermanager@example.com'
  ],
  
  // Analytics admins can view stats but can't modify anything
  analyticsAdmins: [
    'analytics@eventwish.com',
    'stats@example.com'
  ]
};

// Role-based permissions
const rolePermissions = {
  superAdmin: [
    'users.view', 'users.edit', 'users.block', 'users.delete',
    'content.view', 'content.edit', 'content.create', 'content.delete',
    'system.view', 'system.edit',
    'analytics.view'
  ],
  
  contentAdmin: [
    'content.view', 'content.edit', 'content.create', 'content.delete',
    'analytics.view'
  ],
  
  userAdmin: [
    'users.view', 'users.edit', 'users.block',
    'analytics.view'
  ],
  
  analyticsAdmin: [
    'analytics.view'
  ]
};

/**
 * Get admin role for a given email
 * @param {string} email - User email
 * @returns {string|null} - Admin role or null if not an admin
 */
const getAdminRole = (email) => {
  if (!email) {
    console.log("No email provided to getAdminRole");
    return null;
  }
  
  const normalizedEmail = email.toLowerCase();
  console.log(`Checking admin role for email: ${normalizedEmail}`);

  // Check super admins
  if (adminUsers.superAdmins.some(admin => admin.toLowerCase() === normalizedEmail)) {
    console.log(`${normalizedEmail} is a superAdmin`);
    return 'superAdmin';
  }
  
  // Check content admins
  if (adminUsers.contentAdmins.some(admin => admin.toLowerCase() === normalizedEmail)) {
    console.log(`${normalizedEmail} is a contentAdmin`);
    return 'contentAdmin';
  }
  
  // Check user admins
  if (adminUsers.userAdmins.some(admin => admin.toLowerCase() === normalizedEmail)) {
    console.log(`${normalizedEmail} is a userAdmin`);
    return 'userAdmin';
  }
  
  // Check analytics admins
  if (adminUsers.analyticsAdmins.some(admin => admin.toLowerCase() === normalizedEmail)) {
    console.log(`${normalizedEmail} is an analyticsAdmin`);
    return 'analyticsAdmin';
  }
  
  // Not an admin
  console.log(`${normalizedEmail} is not an admin`);
  return null;
};

/**
 * Check if a role has a specific permission
 * @param {string} role - Admin role
 * @param {string} permission - Permission to check
 * @returns {boolean} - Whether the role has the permission
 */
const hasPermission = (role, permission) => {
  if (!role || !permission) return false;
  
  const permissions = rolePermissions[role];
  
  if (!permissions) return false;
  
  return permissions.includes(permission);
};

module.exports = {
  getAdminRole,
  hasPermission,
  rolePermissions
};
