const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

// Handle like count updates
exports.onLikeChange = functions.firestore
    .document('users/{userId}/likes/{templateId}')
    .onWrite((change, context) => {
        const templateId = context.params.templateId;
        const increment = change.after.exists ? 1 : -1;
        
        return admin.firestore()
            .collection('templates')
            .doc(templateId)
            .set({
                likeCount: admin.firestore.FieldValue.increment(increment),
                lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            }, { merge: true });
    });

// Handle favorite count updates
exports.onFavoriteChange = functions.firestore
    .document('users/{userId}/favorites/{templateId}')
    .onWrite((change, context) => {
        const templateId = context.params.templateId;
        const increment = change.after.exists ? 1 : -1;
        
        return admin.firestore()
            .collection('templates')
            .doc(templateId)
            .set({
                favoriteCount: admin.firestore.FieldValue.increment(increment),
                lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            }, { merge: true });
    });

// Handle error tracking
exports.onInteractionError = functions.firestore
    .document('analytics/{docId}')
    .onCreate((snap, context) => {
        const data = snap.data();
        if (data.type === 'interaction_error') {
            console.error('Template interaction error:', {
                templateId: data.templateId,
                userId: data.userId,
                error: data.error,
                timestamp: data.timestamp
            });
        }
        return null;
    });

// Scheduled function to sync template counts daily
exports.syncTemplateCounts = functions.pubsub.schedule('every 24 hours').onRun(async (context) => {
    console.log('Starting scheduled template count sync');
    
    // First ensure templates collection exists
    const templatesRef = admin.firestore().collection('templates');
    const templatesSnapshot = await templatesRef.limit(1).get();
    
    if (templatesSnapshot.empty) {
        console.log('Templates collection is empty, creating placeholder');
        await templatesRef.doc('placeholder').set({
            id: 'placeholder',
            name: 'Placeholder Template',
            likeCount: 0,
            favoriteCount: 0,
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        });
        console.log('Created placeholder template');
    }
    
    // Get all template IDs from user likes and favorites
    const [likesSnapshot, favoritesSnapshot] = await Promise.all([
        admin.firestore().collectionGroup('likes').get(),
        admin.firestore().collectionGroup('favorites').get()
    ]);
    
    // Build set of unique template IDs
    const templateIds = new Set();
    
    likesSnapshot.forEach(doc => {
        const templateId = doc.data().templateId;
        if (templateId) {
            templateIds.add(templateId);
        }
    });
    
    favoritesSnapshot.forEach(doc => {
        const templateId = doc.data().templateId;
        if (templateId) {
            templateIds.add(templateId);
        }
    });
    
    console.log(`Found ${templateIds.size} unique templates to sync`);
    
    // Process each template
    const syncPromises = [...templateIds].map(async (templateId) => {
        try {
            // Count users who liked this template
            const likeCount = (await admin.firestore()
                .collectionGroup('likes')
                .where('templateId', '==', templateId)
                .get()).size;
            
            // Count users who favorited this template
            const favoriteCount = (await admin.firestore()
                .collectionGroup('favorites')
                .where('templateId', '==', templateId)
                .get()).size;
            
            // Update template document
            await admin.firestore()
                .collection('templates')
                .doc(templateId)
                .set({
                    likeCount,
                    favoriteCount,
                    lastUpdated: admin.firestore.FieldValue.serverTimestamp()
                }, { merge: true });
            
            console.log(`Synced template ${templateId}: ${likeCount} likes, ${favoriteCount} favorites`);
            return true;
        } catch (error) {
            console.error(`Error syncing template ${templateId}:`, error);
            return false;
        }
    });
    
    await Promise.all(syncPromises);
    console.log('Template count sync completed');
    
    return null;
}); 