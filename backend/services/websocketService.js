const { Server } = require('socket.io');
const admin = require('firebase-admin');
const logger = require('../utils/logger');
const User = require('../models/User');
const feedService = require('./feedService');

/**
 * WebSocket Service for Real-time Feed Updates
 * Handles live feed updates, user activity, and content notifications
 */
class WebSocketService {
    constructor() {
        this.io = null;
        this.connectedUsers = new Map(); // userId -> socket details
        this.userRooms = new Map(); // userId -> set of rooms
        this.feedUpdateQueue = new Map(); // userId -> pending updates
        this.rateLimitMap = new Map(); // userId -> last update timestamp
        
        // Configuration
        this.config = {
            feedUpdateInterval: 30000, // 30 seconds minimum between feed updates
            maxPendingUpdates: 10, // Maximum pending updates per user
            heartbeatInterval: 25000, // 25 seconds heartbeat
            connectionTimeout: 60000, // 1 minute connection timeout
            maxConnections: 1000 // Maximum concurrent connections
        };
    }

    /**
     * Initialize WebSocket server
     * @param {Object} server - HTTP server instance
     */
    initialize(server) {
        this.io = new Server(server, {
            cors: {
                origin: process.env.CLIENT_ORIGINS ? process.env.CLIENT_ORIGINS.split(',') : ["*"],
                methods: ["GET", "POST"],
                credentials: true
            },
            pingTimeout: this.config.connectionTimeout,
            pingInterval: this.config.heartbeatInterval,
            maxHttpBufferSize: 1e6, // 1MB
            transports: ['websocket', 'polling']
        });

        this.setupEventHandlers();
        this.startCleanupInterval();
        
        logger.info('WebSocket service initialized');
    }

    /**
     * Setup Socket.IO event handlers
     */
    setupEventHandlers() {
        this.io.on('connection', (socket) => {
            logger.info(`New WebSocket connection: ${socket.id}`);
            
            // Handle authentication
            socket.on('authenticate', async (data) => {
                await this.handleAuthentication(socket, data);
            });

            // Handle feed subscription
            socket.on('subscribe_feed', async (data) => {
                await this.handleFeedSubscription(socket, data);
            });

            // Handle user activity updates
            socket.on('user_activity', async (data) => {
                await this.handleUserActivity(socket, data);
            });

            // Handle preference updates
            socket.on('preferences_updated', async (data) => {
                await this.handlePreferencesUpdate(socket, data);
            });

            // Handle template interactions
            socket.on('template_interaction', async (data) => {
                await this.handleTemplateInteraction(socket, data);
            });

            // Handle disconnection
            socket.on('disconnect', (reason) => {
                this.handleDisconnection(socket, reason);
            });

            // Handle errors
            socket.on('error', (error) => {
                logger.error(`WebSocket error for ${socket.id}:`, error);
            });
        });
    }

    /**
     * Handle user authentication
     * @param {Object} socket - Socket instance
     * @param {Object} data - Authentication data
     */
    async handleAuthentication(socket, data) {
        try {
            const { firebaseToken, userId } = data;
            
            if (!firebaseToken || !userId) {
                socket.emit('auth_error', { message: 'Firebase token and user ID required' });
                return;
            }

            // Verify Firebase token
            const decodedToken = await admin.auth().verifyIdToken(firebaseToken);
            
            if (decodedToken.uid !== userId) {
                socket.emit('auth_error', { message: 'Token user ID mismatch' });
                return;
            }

            // Check connection limit
            if (this.connectedUsers.size >= this.config.maxConnections) {
                socket.emit('auth_error', { message: 'Server at capacity' });
                socket.disconnect();
                return;
            }

            // Store user connection
            this.connectedUsers.set(userId, {
                socketId: socket.id,
                socket: socket,
                userId: userId,
                connectedAt: Date.now(),
                lastActivity: Date.now(),
                subscribedFeeds: new Set(),
                preferences: {}
            });

            // Store userId in socket for easy access
            socket.userId = userId;

            // Join user-specific room
            socket.join(`user_${userId}`);
            
            // Initialize user rooms tracking
            if (!this.userRooms.has(userId)) {
                this.userRooms.set(userId, new Set());
            }
            this.userRooms.get(userId).add(`user_${userId}`);

            // Update user's last online status
            await this.updateUserOnlineStatus(userId);

            socket.emit('authenticated', { 
                userId: userId,
                connectedAt: Date.now(),
                serverTime: Date.now()
            });

            logger.info(`User ${userId} authenticated via WebSocket`);

        } catch (error) {
            logger.error('WebSocket authentication error:', error);
            socket.emit('auth_error', { message: 'Authentication failed' });
        }
    }

    /**
     * Handle feed subscription
     * @param {Object} socket - Socket instance
     * @param {Object} data - Subscription data
     */
    async handleFeedSubscription(socket, data) {
        try {
            if (!socket.userId) {
                socket.emit('error', { message: 'Not authenticated' });
                return;
            }

            const { feedType, categories, includePersonalized = true } = data;
            const userId = socket.userId;
            
            const userConnection = this.connectedUsers.get(userId);
            if (!userConnection) {
                socket.emit('error', { message: 'User connection not found' });
                return;
            }

            // Subscribe to feed updates
            const feedKey = `feed_${feedType || 'general'}`;
            userConnection.subscribedFeeds.add(feedKey);
            
            // Join feed-specific room
            socket.join(feedKey);
            this.userRooms.get(userId).add(feedKey);

            // Subscribe to category-specific feeds if specified
            if (categories && Array.isArray(categories)) {
                categories.forEach(category => {
                    const categoryKey = `feed_category_${category}`;
                    userConnection.subscribedFeeds.add(categoryKey);
                    socket.join(categoryKey);
                    this.userRooms.get(userId).add(categoryKey);
                });
            }

            // Send initial feed data if requested
            if (includePersonalized) {
                await this.sendPersonalizedFeedUpdate(userId);
            }

            socket.emit('feed_subscribed', {
                feedType,
                categories,
                subscribedAt: Date.now()
            });

            logger.info(`User ${userId} subscribed to feed: ${feedKey}`);

        } catch (error) {
            logger.error('Feed subscription error:', error);
            socket.emit('error', { message: 'Feed subscription failed' });
        }
    }

    /**
     * Handle user activity updates
     * @param {Object} socket - Socket instance
     * @param {Object} data - Activity data
     */
    async handleUserActivity(socket, data) {
        try {
            if (!socket.userId) return;

            const userId = socket.userId;
            const userConnection = this.connectedUsers.get(userId);
            
            if (userConnection) {
                userConnection.lastActivity = Date.now();
                
                // Update activity in database periodically (not every event)
                const timeSinceLastUpdate = Date.now() - (userConnection.lastDbUpdate || 0);
                if (timeSinceLastUpdate > 60000) { // Update DB every minute
                    await this.updateUserOnlineStatus(userId);
                    userConnection.lastDbUpdate = Date.now();
                }
            }

            // Broadcast activity to relevant rooms if needed
            if (data.broadcastActivity) {
                this.io.to(`user_${userId}`).emit('user_activity_update', {
                    userId,
                    activity: data.activity,
                    timestamp: Date.now()
                });
            }

        } catch (error) {
            logger.error('User activity update error:', error);
        }
    }

    /**
     * Handle preference updates
     * @param {Object} socket - Socket instance
     * @param {Object} data - Preference data
     */
    async handlePreferencesUpdate(socket, data) {
        try {
            if (!socket.userId) return;

            const userId = socket.userId;
            const userConnection = this.connectedUsers.get(userId);
            
            if (userConnection) {
                userConnection.preferences = { ...userConnection.preferences, ...data.preferences };
                
                // Trigger feed refresh due to preference changes
                await this.queueFeedUpdate(userId, 'preferences_changed');
            }

            logger.info(`Preferences updated for user ${userId} via WebSocket`);

        } catch (error) {
            logger.error('Preferences update error:', error);
        }
    }

    /**
     * Handle template interactions
     * @param {Object} socket - Socket instance
     * @param {Object} data - Interaction data
     */
    async handleTemplateInteraction(socket, data) {
        try {
            if (!socket.userId) return;

            const { templateId, action, category } = data;
            const userId = socket.userId;

            // Broadcast interaction to relevant feeds
            if (action === 'like' || action === 'favorite' || action === 'share') {
                // Notify trending feed subscribers
                this.io.to('feed_trending').emit('template_interaction', {
                    templateId,
                    action,
                    category,
                    timestamp: Date.now()
                });

                // Notify category-specific subscribers
                if (category) {
                    this.io.to(`feed_category_${category}`).emit('template_interaction', {
                        templateId,
                        action,
                        category,
                        timestamp: Date.now()
                    });
                }
            }

            // Queue feed update for the user
            await this.queueFeedUpdate(userId, 'interaction');

        } catch (error) {
            logger.error('Template interaction handling error:', error);
        }
    }

    /**
     * Handle disconnection
     * @param {Object} socket - Socket instance
     * @param {String} reason - Disconnection reason
     */
    handleDisconnection(socket, reason) {
        const userId = socket.userId;
        
        if (userId) {
            // Clean up user connection
            this.connectedUsers.delete(userId);
            this.userRooms.delete(userId);
            this.feedUpdateQueue.delete(userId);
            
            logger.info(`User ${userId} disconnected: ${reason}`);
        } else {
            logger.info(`Anonymous socket ${socket.id} disconnected: ${reason}`);
        }
    }

    /**
     * Queue feed update for a user
     * @param {String} userId - User ID
     * @param {String} reason - Update reason
     */
    async queueFeedUpdate(userId, reason) {
        try {
            // Check rate limiting
            const lastUpdate = this.rateLimitMap.get(userId) || 0;
            const timeSinceLastUpdate = Date.now() - lastUpdate;
            
            if (timeSinceLastUpdate < this.config.feedUpdateInterval) {
                // Queue the update for later
                if (!this.feedUpdateQueue.has(userId)) {
                    this.feedUpdateQueue.set(userId, []);
                }
                
                const queue = this.feedUpdateQueue.get(userId);
                if (queue.length < this.config.maxPendingUpdates) {
                    queue.push({ reason, timestamp: Date.now() });
                }
                return;
            }

            // Send immediate update
            await this.sendPersonalizedFeedUpdate(userId, reason);
            this.rateLimitMap.set(userId, Date.now());

        } catch (error) {
            logger.error('Feed update queueing error:', error);
        }
    }

    /**
     * Send personalized feed update to user
     * @param {String} userId - User ID
     * @param {String} reason - Update reason
     */
    async sendPersonalizedFeedUpdate(userId, reason = 'manual') {
        try {
            const userConnection = this.connectedUsers.get(userId);
            if (!userConnection) return;

            // Generate fresh feed
            const feedResponse = await feedService.generateMultiSectionFeed(userId, {
                include: 'personalized,trending,fresh,categories',
                forceRefresh: true
            });

            if (feedResponse.success) {
                userConnection.socket.emit('feed_update', {
                    sections: feedResponse.sections,
                    metadata: feedResponse.metadata,
                    reason,
                    timestamp: Date.now()
                });

                logger.info(`Feed update sent to user ${userId}, reason: ${reason}`);
            }

        } catch (error) {
            logger.error('Personalized feed update error:', error);
        }
    }

    /**
     * Broadcast feed update to all subscribers
     * @param {String} feedType - Feed type (trending, fresh, etc.)
     * @param {Object} updateData - Update data
     */
    broadcastFeedUpdate(feedType, updateData) {
        try {
            const room = `feed_${feedType}`;
            this.io.to(room).emit('feed_update', {
                feedType,
                ...updateData,
                timestamp: Date.now()
            });

            logger.info(`Broadcast feed update to ${room}:`, updateData);

        } catch (error) {
            logger.error('Broadcast feed update error:', error);
        }
    }

    /**
     * Broadcast template update
     * @param {Object} template - Template data
     * @param {String} action - Action type (created, updated, deleted)
     */
    broadcastTemplateUpdate(template, action) {
        try {
            const updateData = {
                template,
                action,
                timestamp: Date.now()
            };

            // Broadcast to general feed
            this.io.to('feed_general').emit('template_update', updateData);

            // Broadcast to category-specific feed
            if (template.category) {
                this.io.to(`feed_category_${template.category}`).emit('template_update', updateData);
            }

            logger.info(`Broadcast template ${action}:`, template.id);

        } catch (error) {
            logger.error('Broadcast template update error:', error);
        }
    }

    /**
     * Update user's online status in database
     * @param {String} userId - User ID
     */
    async updateUserOnlineStatus(userId) {
        try {
            await User.findOneAndUpdate(
                { uid: userId },
                { 
                    lastOnline: new Date(),
                    lastActive: new Date()
                },
                { upsert: false }
            );
        } catch (error) {
            logger.error('Error updating user online status:', error);
        }
    }

    /**
     * Process pending feed updates
     */
    async processPendingUpdates() {
        for (const [userId, updates] of this.feedUpdateQueue.entries()) {
            if (updates.length === 0) continue;

            const lastUpdate = this.rateLimitMap.get(userId) || 0;
            const timeSinceLastUpdate = Date.now() - lastUpdate;

            if (timeSinceLastUpdate >= this.config.feedUpdateInterval) {
                // Send update for the most recent reason
                const latestUpdate = updates[updates.length - 1];
                await this.sendPersonalizedFeedUpdate(userId, latestUpdate.reason);
                
                // Clear the queue and update rate limit
                this.feedUpdateQueue.set(userId, []);
                this.rateLimitMap.set(userId, Date.now());
            }
        }
    }

    /**
     * Start cleanup interval for expired connections and pending updates
     */
    startCleanupInterval() {
        setInterval(() => {
            this.processPendingUpdates();
            this.cleanupExpiredConnections();
        }, 30000); // Run every 30 seconds
    }

    /**
     * Clean up expired connections
     */
    cleanupExpiredConnections() {
        const now = Date.now();
        const expiredConnections = [];

        for (const [userId, connection] of this.connectedUsers.entries()) {
            const timeSinceActivity = now - connection.lastActivity;
            
            if (timeSinceActivity > this.config.connectionTimeout * 2) {
                expiredConnections.push(userId);
            }
        }

        expiredConnections.forEach(userId => {
            const connection = this.connectedUsers.get(userId);
            if (connection && connection.socket) {
                connection.socket.disconnect();
            }
            this.connectedUsers.delete(userId);
            this.userRooms.delete(userId);
            this.feedUpdateQueue.delete(userId);
        });

        if (expiredConnections.length > 0) {
            logger.info(`Cleaned up ${expiredConnections.length} expired connections`);
        }
    }

    /**
     * Get connection statistics
     * @returns {Object} Connection stats
     */
    getStats() {
        return {
            connectedUsers: this.connectedUsers.size,
            totalRooms: Array.from(this.userRooms.values()).reduce((total, rooms) => total + rooms.size, 0),
            pendingUpdates: Array.from(this.feedUpdateQueue.values()).reduce((total, queue) => total + queue.length, 0),
            uptime: process.uptime(),
            memoryUsage: process.memoryUsage()
        };
    }

    /**
     * Send notification to specific user
     * @param {String} userId - User ID
     * @param {Object} notification - Notification data
     */
    sendNotificationToUser(userId, notification) {
        try {
            const userConnection = this.connectedUsers.get(userId);
            if (userConnection) {
                userConnection.socket.emit('notification', {
                    ...notification,
                    timestamp: Date.now()
                });
                
                logger.info(`Notification sent to user ${userId}:`, notification);
            }
        } catch (error) {
            logger.error('Send notification error:', error);
        }
    }

    /**
     * Broadcast system announcement
     * @param {Object} announcement - Announcement data
     */
    broadcastAnnouncement(announcement) {
        try {
            this.io.emit('system_announcement', {
                ...announcement,
                timestamp: Date.now()
            });

            logger.info('System announcement broadcasted:', announcement);

        } catch (error) {
            logger.error('Broadcast announcement error:', error);
        }
    }
}

// Export singleton instance
module.exports = new WebSocketService();
