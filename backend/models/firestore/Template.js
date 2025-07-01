const { admin } = require('../../config/firebase');
const logger = require('../../config/logger');
const db = admin.firestore();

// Collection name for templates
const TEMPLATES_COLLECTION = 'templates';

/**
 * Template Model for Firestore
 * Handles all template-related operations using Firestore's document structure
 */
class Template {
    /**
     * Create a new template document in Firestore
     * @param {Object} templateData Initial template data
     * @returns {Promise<Object>} Created template document
     */
    static async create(templateData) {
        try {
            // Generate a new document ID
            const templateRef = db.collection(TEMPLATES_COLLECTION).doc();
            const templateId = templateRef.id;

            // Initialize template document with default values
            const templateDoc = {
                id: templateId,
                title: templateData.title || '',
                category: templateData.category || '',
                htmlContent: templateData.htmlContent || '',
                cssContent: templateData.cssContent || '',
                jsContent: templateData.jsContent || '',
                previewUrl: templateData.previewUrl || '',
                videoUrl: templateData.videoUrl || '',
                imageUrl: templateData.imageUrl || '',

                // Access / Monetization
                status: templateData.status !== undefined ? templateData.status : true,
                isPremium: templateData.isPremium || false,
                isFeatured: templateData.isFeatured || false,
                isTrending: templateData.isTrending || false,
                isFlagged: templateData.isFlagged || false,
                isLowPerforming: templateData.isLowPerforming || false,
                price: templateData.price || 0,

                // Engagement Metrics
                usageCount: templateData.usageCount || 0,
                likeCount: templateData.likeCount || 0,
                favoriteCount: templateData.favoriteCount || 0,
                viewCount: templateData.viewCount || 0,
                sharedCount: templateData.sharedCount || 0,
                downloadCount: templateData.downloadCount || 0,
                reportCount: templateData.reportCount || 0,
                rating: templateData.rating || 0,
                ratingCount: templateData.ratingCount || 0,

                // Weekly Engagement Metrics
                weeklyUsageCount: templateData.weeklyUsageCount || 0,
                weeklyLikes: templateData.weeklyLikes || 0,
                weeklyFavorites: templateData.weeklyFavorites || 0,
                weeklyViewCount: templateData.weeklyViewCount || 0,
                weeklySharedCount: templateData.weeklySharedCount || 0,
                weeklyDownloadCount: templateData.weeklyDownloadCount || 0,
                weeklyReportCount: templateData.weeklyReportCount || 0,
                weeklyScoreLastReset: templateData.weeklyScoreLastReset || admin.firestore.FieldValue.serverTimestamp(),

                // Trending Score
                trendingScore: templateData.trendingScore || 0,

                // Customization Settings
                customizationOptions: {
                    allowNameEdit: templateData.customizationOptions?.allowNameEdit !== undefined ? 
                        templateData.customizationOptions.allowNameEdit : true,
                    allowPhotoEdit: templateData.customizationOptions?.allowPhotoEdit || false,
                    allowBackgroundChange: templateData.customizationOptions?.allowBackgroundChange || false,
                    allowMusic: templateData.customizationOptions?.allowMusic || false,
                    allowThemeCustomization: templateData.customizationOptions?.allowThemeCustomization || false
                },

                // AI Metadata
                isAIGenerated: templateData.isAIGenerated || false,
                aiPrompt: templateData.aiPrompt || '',
                aiModel: templateData.aiModel || '',
                aiStyle: templateData.aiStyle || '',
                aiGenerationStage: templateData.aiGenerationStage || 'initial',
                generatedByUser: templateData.generatedByUser || null,
                generatedByUserUid: templateData.generatedByUserUid || null,
                generationMetadata: templateData.generationMetadata || {
                    resolution: '1024x1024',
                    seed: null,
                    guidanceScale: null,
                    timestamp: null
                },

                // Categorization
                creatorId: templateData.creatorId || null,
                creatorUid: templateData.creatorUid || null,
                festivalTag: templateData.festivalTag || '',
                tags: templateData.tags || [],
                searchKeywords: templateData.searchKeywords || [],
                variationOf: templateData.variationOf || null,
                relatedTemplates: templateData.relatedTemplates || [],

                // Timestamps
                created: admin.firestore.FieldValue.serverTimestamp(),
                lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
                lastModified: admin.firestore.FieldValue.serverTimestamp()
            };

            // Create the template document
            await templateRef.set(templateDoc);

            return { id: templateId, ...templateDoc };
        } catch (error) {
            logger.error(`Error creating template: ${error.message}`);
            throw error;
        }
    }

    /**
     * Get a template by its ID
     * @param {string} templateId Template ID
     * @returns {Promise<Object>} Template document
     */
    static async getById(templateId) {
        try {
            const templateDoc = await db.collection(TEMPLATES_COLLECTION).doc(templateId).get();
            
            if (!templateDoc.exists) {
                return null;
            }

            return { id: templateDoc.id, ...templateDoc.data() };
        } catch (error) {
            logger.error(`Error getting template by ID: ${error.message}`);
            throw error;
        }
    }

    /**
     * Update a template document
     * @param {string} templateId Template ID
     * @param {Object} updateData Data to update
     * @returns {Promise<Object>} Updated template document
     */
    static async update(templateId, updateData) {
        try {
            const templateRef = db.collection(TEMPLATES_COLLECTION).doc(templateId);
            
            // Update the document
            await templateRef.update({
                ...updateData,
                lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
                lastModified: admin.firestore.FieldValue.serverTimestamp()
            });

            // Get and return the updated document
            const updatedDoc = await templateRef.get();
            return { id: updatedDoc.id, ...updatedDoc.data() };
        } catch (error) {
            logger.error(`Error updating template: ${error.message}`);
            throw error;
        }
    }

    /**
     * Get templates by category
     * @param {string} category Category name
     * @param {number} limit Number of templates to return
     * @param {Object} lastDoc Last document for pagination
     * @returns {Promise<Object>} Templates and pagination info
     */
    static async getByCategory(category, limit = 20, lastDoc = null) {
        try {
            let query = db.collection(TEMPLATES_COLLECTION)
                .where('category', '==', category)
                .where('status', '==', true)
                .orderBy('created', 'desc')
                .limit(limit);

            if (lastDoc) {
                query = query.startAfter(lastDoc);
            }

            const snapshot = await query.get();
            const templates = [];
            let lastDocument = null;

            snapshot.forEach(doc => {
                templates.push({ id: doc.id, ...doc.data() });
                lastDocument = doc;
            });

            return {
                templates,
                hasMore: snapshot.size === limit,
                lastDoc: lastDocument
            };
        } catch (error) {
            logger.error(`Error getting templates by category: ${error.message}`);
            throw error;
        }
    }

    /**
     * Get templates with pagination
     * @param {number} page Page number (1-based)
     * @param {number} limit Number of templates per page
     * @param {Object} filters Additional filters
     * @returns {Promise<Object>} Templates with pagination info
     */
    static async getPaginated(page = 1, limit = 20, filters = {}) {
        try {
            const offset = (page - 1) * limit;
            
            let query = db.collection(TEMPLATES_COLLECTION)
                .where('status', '==', true);

            // Apply filters
            if (filters.category) {
                query = query.where('category', '==', filters.category);
            }

            if (filters.isPremium !== undefined) {
                query = query.where('isPremium', '==', filters.isPremium);
            }

            if (filters.isFeatured !== undefined) {
                query = query.where('isFeatured', '==', filters.isFeatured);
            }

            // Apply ordering and pagination
            query = query.orderBy('created', 'desc');

            if (offset > 0) {
                query = query.offset(offset);
            }

            query = query.limit(limit);

            const snapshot = await query.get();
            const templates = [];

            snapshot.forEach(doc => {
                templates.push({ id: doc.id, ...doc.data() });
            });

            return {
                templates,
                page,
                limit,
                hasMore: snapshot.size === limit,
                total: null // Firestore doesn't provide efficient count
            };
        } catch (error) {
            logger.error(`Error getting paginated templates: ${error.message}`);
            throw error;
        }
    }

    /**
     * Get all categories with template counts
     * @returns {Promise<Object>} Categories with counts
     */
    static async getCategories() {
        try {
            const snapshot = await db.collection(TEMPLATES_COLLECTION)
                .where('status', '==', true)
                .get();

            const categories = {};
            
            snapshot.forEach(doc => {
                const category = doc.data().category;
                if (category) {
                    categories[category] = (categories[category] || 0) + 1;
                }
            });

            return categories;
        } catch (error) {
            logger.error(`Error getting categories: ${error.message}`);
            throw error;
        }
    }

    /**
     * Increment template counter atomically
     * @param {string} templateId Template ID
     * @param {string} counterField Field to increment
     * @param {number} increment Increment value (can be negative)
     * @returns {Promise<void>}
     */
    static async incrementCounter(templateId, counterField, increment = 1) {
        try {
            const templateRef = db.collection(TEMPLATES_COLLECTION).doc(templateId);
            
            await db.runTransaction(async (transaction) => {
                const templateDoc = await transaction.get(templateRef);
                
                if (!templateDoc.exists) {
                    // Create template with initial counter
                    await transaction.set(templateRef, {
                        id: templateId,
                        [counterField]: increment,
                        created: admin.firestore.FieldValue.serverTimestamp(),
                        lastUpdated: admin.firestore.FieldValue.serverTimestamp()
                    });
                } else {
                    // Update existing counter
                    const currentValue = templateDoc.data()[counterField] || 0;
                    const newValue = Math.max(0, currentValue + increment); // Prevent negative values
                    
                    await transaction.update(templateRef, {
                        [counterField]: newValue,
                        lastUpdated: admin.firestore.FieldValue.serverTimestamp()
                    });
                }
            });
        } catch (error) {
            logger.error(`Error incrementing counter: ${error.message}`);
            throw error;
        }
    }

    /**
     * Get trending templates based on trending score
     * @param {number} limit Number of templates to return
     * @returns {Promise<Array>} Array of trending templates
     */
    static async getTrending(limit = 20) {
        try {
            const snapshot = await db.collection(TEMPLATES_COLLECTION)
                .where('status', '==', true)
                .where('isTrending', '==', true)
                .orderBy('trendingScore', 'desc')
                .limit(limit)
                .get();

            const templates = [];
            snapshot.forEach(doc => {
                templates.push({ id: doc.id, ...doc.data() });
            });

            return templates;
        } catch (error) {
            logger.error(`Error getting trending templates: ${error.message}`);
            // Fallback to recent templates if trending query fails
            return await this.getRecent(limit);
        }
    }

    /**
     * Get featured templates
     * @param {number} limit Number of templates to return
     * @returns {Promise<Array>} Array of featured templates
     */
    static async getFeatured(limit = 20) {
        try {
            const snapshot = await db.collection(TEMPLATES_COLLECTION)
                .where('status', '==', true)
                .where('isFeatured', '==', true)
                .orderBy('created', 'desc')
                .limit(limit)
                .get();

            const templates = [];
            snapshot.forEach(doc => {
                templates.push({ id: doc.id, ...doc.data() });
            });

            return templates;
        } catch (error) {
            logger.error(`Error getting featured templates: ${error.message}`);
            // Fallback to recent templates if featured query fails
            return await this.getRecent(limit);
        }
    }

    /**
     * Get recent templates (fallback method)
     * @param {number} limit Number of templates to return
     * @returns {Promise<Array>} Array of recent templates
     */
    static async getRecent(limit = 20) {
        try {
            const snapshot = await db.collection(TEMPLATES_COLLECTION)
                .where('status', '==', true)
                .orderBy('created', 'desc')
                .limit(limit)
                .get();

            const templates = [];
            snapshot.forEach(doc => {
                templates.push({ id: doc.id, ...doc.data() });
            });

            return templates;
        } catch (error) {
            logger.error(`Error getting recent templates: ${error.message}`);
            return [];
        }
    }

    /**
     * Search templates by query and filters
     * @param {Object} searchParams Search parameters
     * @returns {Promise<Array>} Array of matching templates
     */
    static async search(searchParams) {
        try {
            const {
                query,
                category,
                tags,
                isAIGenerated,
                isPremium,
                isFeatured,
                limit = 20,
                offset = 0
            } = searchParams;

            let firestoreQuery = db.collection(TEMPLATES_COLLECTION)
                .where('status', '==', true);

            // Apply filters
            if (category) {
                firestoreQuery = firestoreQuery.where('category', '==', category);
            }

            if (isPremium !== undefined) {
                firestoreQuery = firestoreQuery.where('isPremium', '==', isPremium);
            }

            if (isFeatured !== undefined) {
                firestoreQuery = firestoreQuery.where('isFeatured', '==', isFeatured);
            }

            if (isAIGenerated !== undefined) {
                firestoreQuery = firestoreQuery.where('isAIGenerated', '==', isAIGenerated);
            }

            // Apply ordering and pagination
            firestoreQuery = firestoreQuery.orderBy('created', 'desc');

            if (offset > 0) {
                firestoreQuery = firestoreQuery.offset(offset);
            }

            firestoreQuery = firestoreQuery.limit(limit);

            const snapshot = await firestoreQuery.get();
            let templates = [];

            snapshot.forEach(doc => {
                templates.push({ id: doc.id, ...doc.data() });
            });

            // Apply text search and tag filtering in memory (Firestore doesn't support full-text search)
            if (query) {
                const searchTerm = query.toLowerCase();
                templates = templates.filter(template => 
                    template.title?.toLowerCase().includes(searchTerm) ||
                    template.category?.toLowerCase().includes(searchTerm) ||
                    template.searchKeywords?.some(keyword => 
                        keyword.toLowerCase().includes(searchTerm)
                    )
                );
            }

            if (tags && tags.length > 0) {
                templates = templates.filter(template =>
                    template.tags?.some(tag => tags.includes(tag))
                );
            }

            return templates;
        } catch (error) {
            logger.error(`Error searching templates: ${error.message}`);
            return [];
        }
    }
}

module.exports = Template;
