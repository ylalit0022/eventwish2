const admin = require('firebase-admin');
const path = require('path');
const logger = require('./logger');

/**
 * Firebase Admin SDK initialization
 * Used for verifying Firebase ID tokens during authentication
 */
const initializeFirebaseAdmin = () => {
    try {
        // Check if firebase admin is already initialized
        if (admin.apps.length) {
            logger.info('Firebase Admin SDK already initialized');
            return;
        }
        
        // Path to service account key file
        const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH || 
                                   path.join(__dirname, '../config/firebase-service-account.json');
        
        // Initialize with service account or application default credentials
        if (process.env.NODE_ENV === 'production') {
            // In production, use application default credentials
            admin.initializeApp({
                credential: admin.credential.applicationDefault()
            });
            logger.info('Firebase Admin SDK initialized with application default credentials');
        } else {
            // In development or testing, use service account file
            try {
                const serviceAccount = require(serviceAccountPath);
                admin.initializeApp({
                    credential: admin.credential.cert(serviceAccount)
                });
                logger.info(`Firebase Admin SDK initialized with service account: ${serviceAccountPath}`);
            } catch (error) {
                logger.error(`Failed to load service account file: ${error.message}`);
                throw new Error('Firebase service account file not found or invalid');
            }
        }
    } catch (error) {
        logger.error(`Error initializing Firebase Admin SDK: ${error.message}`);
        throw error;
    }
};

/**
 * Verify Firebase ID token
 * @param {string} idToken - Firebase ID token to verify
 * @returns {Promise<DecodedIdToken>} - Decoded token with user information
 */
const verifyIdToken = async (idToken) => {
    try {
        // Verify the ID token
        const decodedToken = await admin.auth().verifyIdToken(idToken);
        return decodedToken;
    } catch (error) {
        logger.error(`Error verifying Firebase ID token: ${error.message}`);
        throw error;
    }
};

module.exports = {
    initializeFirebaseAdmin,
    verifyIdToken,
    admin
}; 