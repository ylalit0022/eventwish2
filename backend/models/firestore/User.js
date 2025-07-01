const { admin } = require('../../config/firebase');
const logger = require('../../config/logger');
const db = admin.firestore();

// Collection name for users
const USERS_COLLECTION = 'users';

/**
 * User Model for Firestore
 * Handles all user-related operations using Firestore's document structure
 */
class User {
    /**
     * Create a new user document in Firestore
     * @param {Object} userData Initial user data
     * @returns {Promise<Object>} Created user document
     */
    static async create(userData) {
        try {
            const { uid, ...otherData } = userData;
            
            if (!uid) {
                throw new Error('uid is required for user creation');
            }

            // Initialize user document with default values
            const userDoc = {
                uid,
                deviceId: null,
                deviceModel: null,
                deviceName: null,
                appVersion: null,
                osVersion: null,
                loginTimestamp: admin.firestore.FieldValue.serverTimestamp(),
                displayName: null,
                email: null,
                profilePhoto: null,
                
                // Subscription & Access
                subscription: {
                    isActive: false,
                    plan: 'NONE',
                    planLevel: 'NONE',
                    startedAt: null,
                    expiresAt: null,
                    features: {
                        allowNameEdit: false,
                        allowPhotoEdit: false,
                        allowAdFree: false,
                        allowDraftSave: false,
                        allowAnalytics: false,
                        allowMusic: false,
                        allowThemeCustomization: false,
                        allowPrioritySupport: false
                    },
                    appliedPricingRuleId: null,
                    basePrice: 0,
                    finalPrice: 0,
                    currency: 'INR'
                },
                
                subscriptionOffer: null,
                subscriptionHistory: [],
                lastSubscriptionEndedAt: null,
                monthsWithoutPayment: 0,
                adsAllowed: true,

                // AI Usage
                aiUsage: {
                    monthlyGenerationCount: 0,
                    lastGenerationAt: null,
                    quota: 5,
                    recentPrompts: [],
                    stylePreferences: []
                },

                // Activity & Status
                lastOnline: admin.firestore.FieldValue.serverTimestamp(),
                lastActive: admin.firestore.FieldValue.serverTimestamp(),
                lastInactivityNotification: null,
                created: admin.firestore.FieldValue.serverTimestamp(),
                isBlocked: false,
                blockInfo: null,

                // Push Preferences
                pushPreferences: {
                    allowFestivalPush: true,
                    allowPersonalPush: true
                },

                // Device & Notifications
                fcmTokens: [],
                topicSubscriptions: [],
                activeSessions: {},

                // User Preferences
                preferredTheme: 'light',
                preferredLanguage: 'en',
                timezone: 'Asia/Kolkata',
                muteNotificationsUntil: null,

                // Referrals
                referredBy: {
                    referredBy: null,
                    referralCode: null
                },
                referralCode: null,

                // Engagement
                likes: [],
                favorites: [],
                recentTemplatesUsed: [],
                lastActiveTemplate: null,
                lastActionOnTemplate: null,
                engagementLog: [],
                templateAffinity: [],
                categories: [],
                ignoredTemplates: [],
                viewedTemplatesMap: {},
                drafts: [],

                // Feed
                cachedHomeFeed: [],
                homeFeedLastGeneratedAt: null,

                // Merge any provided data
                ...otherData
            };

            // Create the user document
            await db.collection(USERS_COLLECTION).doc(uid).set(userDoc);

            return userDoc;
        } catch (error) {
            logger.error(`Error creating user: ${error.message}`);
            throw error;
        }
    }

    /**
     * Get a user by their UID
     * @param {string} uid User's Firebase UID
     * @returns {Promise<Object>} User document
     */
    static async getByUid(uid) {
        try {
            const userDoc = await db.collection(USERS_COLLECTION).doc(uid).get();
            
            if (!userDoc.exists) {
                return null;
            }

            return userDoc.data();
        } catch (error) {
            logger.error(`Error getting user by UID: ${error.message}`);
            throw error;
        }
    }

    /**
     * Get a user by their device ID
     * @param {string} deviceId Device ID
     * @returns {Promise<Object>} User document
     */
    static async getByDeviceId(deviceId) {
        try {
            const snapshot = await db.collection(USERS_COLLECTION)
                .where('deviceId', '==', deviceId)
                .limit(1)
                .get();

            if (snapshot.empty) {
                return null;
            }

            return snapshot.docs[0].data();
        } catch (error) {
            logger.error(`Error getting user by device ID: ${error.message}`);
            throw error;
        }
    }

    /**
     * Update a user's document
     * @param {string} uid User's Firebase UID
     * @param {Object} updateData Data to update
     * @returns {Promise<Object>} Updated user document
     */
    static async update(uid, updateData) {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            
            // Update the document
            await userRef.update({
                ...updateData,
                lastActive: admin.firestore.FieldValue.serverTimestamp()
            });

            // Get and return the updated document
            const updatedDoc = await userRef.get();
            return updatedDoc.data();
        } catch (error) {
            logger.error(`Error updating user: ${error.message}`);
            throw error;
        }
    }

    /**
     * Update a user's last online timestamp
     * @param {string} uid User's Firebase UID
     * @returns {Promise<void>}
     */
    static async updateLastOnline(uid) {
        try {
            await db.collection(USERS_COLLECTION).doc(uid).update({
                lastOnline: admin.firestore.FieldValue.serverTimestamp(),
                lastActive: admin.firestore.FieldValue.serverTimestamp()
            });
        } catch (error) {
            logger.error(`Error updating last online: ${error.message}`);
            throw error;
        }
    }

    /**
     * Record a category visit for a user
     * @param {string} uid User's Firebase UID
     * @param {string} categoryName Category name
     * @param {string} source Source of the visit ('direct' or 'template')
     * @returns {Promise<void>}
     */
    static async visitCategory(uid, categoryName, source = 'direct') {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            const userDoc = await userRef.get();
            
            if (!userDoc.exists) {
                throw new Error('User not found');
            }

            const userData = userDoc.data();
            const categories = userData.categories || [];
            
            // Find existing category
            const existingCategoryIndex = categories.findIndex(
                c => c.category.toLowerCase() === categoryName.toLowerCase()
            );

            if (existingCategoryIndex !== -1) {
                // Update existing category
                categories[existingCategoryIndex] = {
                    ...categories[existingCategoryIndex],
                    visitDate: admin.firestore.FieldValue.serverTimestamp(),
                    visitCount: categories[existingCategoryIndex].visitCount + 1,
                    source
                };
            } else {
                // Add new category
                categories.push({
                    category: categoryName,
                    visitDate: admin.firestore.FieldValue.serverTimestamp(),
                    visitCount: 1,
                    source
                });
            }

            // Update the document
            await userRef.update({
                categories,
                lastOnline: admin.firestore.FieldValue.serverTimestamp(),
                lastActive: admin.firestore.FieldValue.serverTimestamp()
            });
        } catch (error) {
            logger.error(`Error recording category visit: ${error.message}`);
            throw error;
        }
    }

    /**
     * Block a user
     * @param {string} uid User's Firebase UID
     * @param {string} adminUid Admin's UID
     * @param {string} reason Reason for blocking
     * @param {Date} expiresAt Block expiration date
     * @param {string} notes Additional notes
     * @returns {Promise<Object>} Updated user document
     */
    static async blockUser(uid, adminUid, reason, expiresAt = null, notes = '') {
        try {
            const blockInfo = {
                blockedBy: adminUid,
                reason: reason || 'Blocked by administrator',
                blockedAt: admin.firestore.FieldValue.serverTimestamp(),
                blockExpiresAt: expiresAt,
                notes: notes
            };

            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            await userRef.update({
                isBlocked: true,
                blockInfo
            });

            const updatedDoc = await userRef.get();
            return updatedDoc.data();
        } catch (error) {
            logger.error(`Error blocking user: ${error.message}`);
            throw error;
        }
    }

    /**
     * Unblock a user
     * @param {string} uid User's Firebase UID
     * @returns {Promise<Object>} Updated user document
     */
    static async unblockUser(uid) {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            await userRef.update({
                isBlocked: false,
                blockInfo: null
            });

            const updatedDoc = await userRef.get();
            return updatedDoc.data();
        } catch (error) {
            logger.error(`Error unblocking user: ${error.message}`);
            throw error;
        }
    }

    /**
     * Check if a user is currently blocked
     * @param {string} uid User's Firebase UID
     * @returns {Promise<boolean>} Whether the user is blocked
     */
    static async isCurrentlyBlocked(uid) {
        try {
            const userDoc = await db.collection(USERS_COLLECTION).doc(uid).get();
            
            if (!userDoc.exists) {
                throw new Error('User not found');
            }

            const userData = userDoc.data();
            
            if (!userData.isBlocked) {
                return false;
            }

            // Check if block has expired
            if (userData.blockInfo && userData.blockInfo.blockExpiresAt) {
                const now = new Date();
                const expiresAt = userData.blockInfo.blockExpiresAt.toDate();
                
                if (now > expiresAt) {
                    // Auto-unblock
                    await this.unblockUser(uid);
                    return false;
                }
            }

            return true;
        } catch (error) {
            logger.error(`Error checking block status: ${error.message}`);
            throw error;
        }
    }

    /**
     * Add or update an FCM token for a user
     * @param {string} uid User's Firebase UID
     * @param {string} token FCM token
     * @param {string} platform Device platform
     * @returns {Promise<void>}
     */
    static async addFcmToken(uid, token, platform = 'android') {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            const userDoc = await userRef.get();
            
            if (!userDoc.exists) {
                throw new Error('User not found');
            }

            const userData = userDoc.data();
            const fcmTokens = userData.fcmTokens || [];
            
            // Find existing token
            const tokenIndex = fcmTokens.findIndex(t => t.token === token);
            
            if (tokenIndex !== -1) {
                // Update existing token
                fcmTokens[tokenIndex] = {
                    ...fcmTokens[tokenIndex],
                    platform,
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                };
            } else {
                // Add new token
                fcmTokens.push({
                    token,
                    platform,
                    subscribedTopics: [],
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                });
            }

            // Update the document
            await userRef.update({ fcmTokens });
        } catch (error) {
            logger.error(`Error adding FCM token: ${error.message}`);
            throw error;
        }
    }

    /**
     * Remove an FCM token from a user
     * @param {string} uid User's Firebase UID
     * @param {string} token FCM token to remove
     * @returns {Promise<void>}
     */
    static async removeFcmToken(uid, token) {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            const userDoc = await userRef.get();
            
            if (!userDoc.exists) {
                throw new Error('User not found');
            }

            const userData = userDoc.data();
            const fcmTokens = userData.fcmTokens || [];
            
            // Filter out the token
            const updatedTokens = fcmTokens.filter(t => t.token !== token);

            // Update the document
            await userRef.update({ fcmTokens: updatedTokens });
        } catch (error) {
            logger.error(`Error removing FCM token: ${error.message}`);
            throw error;
        }
    }

    /**
     * Add a device session for a user
     * @param {string} uid User's Firebase UID
     * @param {Object} deviceInfo Device information
     * @returns {Promise<void>}
     */
    static async addDeviceSession(uid, deviceInfo) {
        try {
            const { deviceId, deviceModel, deviceName, appVersion, osVersion } = deviceInfo;

            if (!deviceId) {
                throw new Error('deviceId is required for tracking sessions');
            }

            const sessionData = {
                deviceId,
                deviceModel: deviceModel || 'Unknown',
                deviceName: deviceName || 'Unknown Device',
                appVersion: appVersion || 'Unknown',
                osVersion: osVersion || 'Unknown',
                loginTimestamp: admin.firestore.FieldValue.serverTimestamp(),
                lastActiveTimestamp: admin.firestore.FieldValue.serverTimestamp()
            };

            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            await userRef.update({
                [`activeSessions.${deviceId}`]: sessionData
            });
        } catch (error) {
            logger.error(`Error adding device session: ${error.message}`);
            throw error;
        }
    }

    /**
     * Subscribe a user to notification topics
     * @param {string} uid User's Firebase UID
     * @param {string[]} topics Topics to subscribe to
     * @returns {Promise<string[]>} Updated list of subscribed topics
     */
    static async subscribeToTopics(uid, topics) {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            const userDoc = await userRef.get();
            
            if (!userDoc.exists) {
                throw new Error('User not found');
            }

            const userData = userDoc.data();
            const currentTopics = userData.topicSubscriptions || [];
            
            // Add new topics
            const updatedTopics = [...new Set([...currentTopics, ...topics])];
            
            // Update the document
            await userRef.update({ topicSubscriptions: updatedTopics });
            
            return updatedTopics;
        } catch (error) {
            logger.error(`Error subscribing to topics: ${error.message}`);
            throw error;
        }
    }

    /**
     * Unsubscribe a user from notification topics
     * @param {string} uid User's Firebase UID
     * @param {string[]} topics Topics to unsubscribe from
     * @returns {Promise<string[]>} Updated list of subscribed topics
     */
    static async unsubscribeFromTopics(uid, topics) {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            const userDoc = await userRef.get();
            
            if (!userDoc.exists) {
                throw new Error('User not found');
            }

            const userData = userDoc.data();
            const currentTopics = userData.topicSubscriptions || [];
            
            // Remove specified topics
            const updatedTopics = currentTopics.filter(topic => !topics.includes(topic));
            
            // Update the document
            await userRef.update({ topicSubscriptions: updatedTopics });
            
            return updatedTopics;
        } catch (error) {
            logger.error(`Error unsubscribing from topics: ${error.message}`);
            throw error;
        }
    }

    /**
     * Add a template to user's likes
     * @param {string} uid User's Firebase UID
     * @param {string} templateId Template ID to like
     * @returns {Promise<void>}
     */
    static async addToLikes(uid, templateId) {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            const likeRef = userRef.collection('likes').doc(templateId);
            
            await likeRef.set({
                templateId,
                timestamp: admin.firestore.FieldValue.serverTimestamp()
            });
            
            logger.info(`User ${uid} liked template ${templateId}`);
        } catch (error) {
            logger.error(`Error adding to likes: ${error.message}`);
            throw error;
        }
    }

    /**
     * Remove a template from user's likes
     * @param {string} uid User's Firebase UID
     * @param {string} templateId Template ID to unlike
     * @returns {Promise<void>}
     */
    static async removeFromLikes(uid, templateId) {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            const likeRef = userRef.collection('likes').doc(templateId);
            
            await likeRef.delete();
            
            logger.info(`User ${uid} unliked template ${templateId}`);
        } catch (error) {
            logger.error(`Error removing from likes: ${error.message}`);
            throw error;
        }
    }

    /**
     * Add a template to user's favorites
     * @param {string} uid User's Firebase UID
     * @param {string} templateId Template ID to favorite
     * @returns {Promise<void>}
     */
    static async addToFavorites(uid, templateId) {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            const favoriteRef = userRef.collection('favorites').doc(templateId);
            
            await favoriteRef.set({
                templateId,
                timestamp: admin.firestore.FieldValue.serverTimestamp()
            });
            
            logger.info(`User ${uid} favorited template ${templateId}`);
        } catch (error) {
            logger.error(`Error adding to favorites: ${error.message}`);
            throw error;
        }
    }

    /**
     * Remove a template from user's favorites
     * @param {string} uid User's Firebase UID
     * @param {string} templateId Template ID to unfavorite
     * @returns {Promise<void>}
     */
    static async removeFromFavorites(uid, templateId) {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            const favoriteRef = userRef.collection('favorites').doc(templateId);
            
            await favoriteRef.delete();
            
            logger.info(`User ${uid} unfavorited template ${templateId}`);
        } catch (error) {
            logger.error(`Error removing from favorites: ${error.message}`);
            throw error;
        }
    }

    /**
     * Record a template view for a user
     * @param {string} uid User's Firebase UID
     * @param {string} templateId Template ID viewed
     * @returns {Promise<void>}
     */
    static async recordTemplateView(uid, templateId) {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            const viewRef = userRef.collection('views').doc(templateId);
            
            // Update or create view record
            await viewRef.set({
                templateId,
                viewCount: admin.firestore.FieldValue.increment(1),
                lastViewedAt: admin.firestore.FieldValue.serverTimestamp()
            }, { merge: true });
            
            logger.info(`User ${uid} viewed template ${templateId}`);
        } catch (error) {
            logger.error(`Error recording template view: ${error.message}`);
            throw error;
        }
    }

    /**
     * Get user's liked templates
     * @param {string} uid User's Firebase UID
     * @param {number} limit Number of templates to return
     * @returns {Promise<Array>} Array of liked template IDs
     */
    static async getUserLikes(uid, limit = 50) {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            const likesSnapshot = await userRef.collection('likes')
                .orderBy('timestamp', 'desc')
                .limit(limit)
                .get();
            
            const likes = [];
            likesSnapshot.forEach(doc => {
                likes.push(doc.data().templateId);
            });
            
            return likes;
        } catch (error) {
            logger.error(`Error getting user likes: ${error.message}`);
            throw error;
        }
    }

    /**
     * Get user's favorite templates
     * @param {string} uid User's Firebase UID
     * @param {number} limit Number of templates to return
     * @returns {Promise<Array>} Array of favorite template IDs
     */
    static async getUserFavorites(uid, limit = 50) {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            const favoritesSnapshot = await userRef.collection('favorites')
                .orderBy('timestamp', 'desc')
                .limit(limit)
                .get();
            
            const favorites = [];
            favoritesSnapshot.forEach(doc => {
                favorites.push(doc.data().templateId);
            });
            
            return favorites;
        } catch (error) {
            logger.error(`Error getting user favorites: ${error.message}`);
            throw error;
        }
    }

    /**
     * Check if user has liked a template
     * @param {string} uid User's Firebase UID
     * @param {string} templateId Template ID to check
     * @returns {Promise<boolean>} True if liked
     */
    static async hasLiked(uid, templateId) {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            const likeDoc = await userRef.collection('likes').doc(templateId).get();
            
            return likeDoc.exists;
        } catch (error) {
            logger.error(`Error checking like status: ${error.message}`);
            throw error;
        }
    }

    /**
     * Check if user has favorited a template
     * @param {string} uid User's Firebase UID
     * @param {string} templateId Template ID to check
     * @returns {Promise<boolean>} True if favorited
     */
    static async hasFavorited(uid, templateId) {
        try {
            const userRef = db.collection(USERS_COLLECTION).doc(uid);
            const favoriteDoc = await userRef.collection('favorites').doc(templateId).get();
            
            return favoriteDoc.exists;
        } catch (error) {
            logger.error(`Error checking favorite status: ${error.message}`);
            throw error;
        }
    }
}

module.exports = User;
