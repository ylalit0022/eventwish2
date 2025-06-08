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
            .update({
                likeCount: admin.firestore.FieldValue.increment(increment),
                lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            });
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
            .update({
                favoriteCount: admin.firestore.FieldValue.increment(increment),
                lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            });
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