package com.ds.eventwish.data.remote;

import com.ds.eventwish.data.model.About;
import com.ds.eventwish.data.model.Contact;
import com.ds.eventwish.data.model.CategoryIcon;
import com.ds.eventwish.data.model.Festival;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.User;
import com.ds.eventwish.data.model.response.BaseResponse;
import com.ds.eventwish.data.model.response.CategoryIconResponse;
import com.ds.eventwish.data.model.response.TemplateResponse;
import com.ds.eventwish.data.model.response.WishResponse;
import com.ds.eventwish.data.model.ServerTimeResponse;
import com.ds.eventwish.data.model.response.AdMobResponse;
import com.ds.eventwish.data.model.response.SponsoredAdResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

import com.google.gson.JsonObject;

public interface ApiService {
    // Template endpoints
    @GET("templates")
    Call<TemplateResponse> getTemplates(
        @Query("page") int page,
        @Query("limit") int limit
    );

    @GET("templates/category/{category}")
    Call<TemplateResponse> getTemplatesByCategory(
        @Path("category") String category,
        @Query("page") int page,
        @Query("limit") int limit
    );

    @GET("templates/{id}")
    Call<Template> getTemplateById(@Path("id") String id);
    
    // Wish sharing endpoints
    @POST("share")
    Call<JsonObject> createSharedWish(@Body Map<String, Object> sharedWish);

    @GET("wishes/{shortCode}")
    Call<BaseResponse<WishResponse>> getSharedWish(@Path("shortCode") String shortCode);

    @GET("wishes/{shortCode}")
    Call<JsonObject> getSharedWishJsonByShortCode(@Path("shortCode") String shortCode);

    @POST("wishes/{shortCode}/share")
    Call<JsonObject> updateSharedWishPlatform(@Path("shortCode") String shortCode, @Body JsonObject platform);

    @GET("wishes/{shortCode}/analytics")
    Call<JsonObject> getWishAnalytics(@Path("shortCode") String shortCode);

    /**
     * Track sharing of wish via specific platform
     * @param shortCode The shortcode of the wish
     * @param platform The platform used for sharing
     * @return Response
     */
    @POST("wishes/{shortCode}/track-share")
    Call<BaseResponse<Void>> trackWishShare(
        @Path("shortCode") String shortCode, 
        @Body String platform
    );

    @GET("wishes/my")
    Call<List<SharedWish>> getMyWishes();

    @DELETE("wishes/clear")
    Call<Void> clearHistory();
    
    // Festival endpoints
    @GET("festivals/upcoming")
    Call<List<Festival>> getUpcomingFestivals();
    
    @GET("festivals/category/{category}")
    Call<List<Festival>> getFestivalsByCategory(@Path("category") String category);
    
    @GET("festivals/{id}")
    Call<Festival> getFestivalById(@Path("id") String id);

    // Category icon endpoints
    @GET("categoryIcons")
    Call<CategoryIconResponse> getCategoryIcons();

    // Get a single category icon by ID
    @GET("categoryIcons/{id}")
    Call<CategoryIcon> getCategoryIconById(@Path("id") String id);

    // Server time endpoint
    @GET("server/time")
    Call<ServerTimeResponse> getServerTime();

    // FCM token registration
    @POST("tokens/register")
    Call<Void> registerToken(@Body JsonObject token);
    
    // Resource loading methods
    @GET("{resourceType}/{resourceId}")
    Call<JsonObject> getResource(
        @Path("resourceType") String resourceType,
        @Path("resourceId") String resourceId,
        @HeaderMap Map<String, String> headers
    );
    
    @GET("templates")
    Call<List<JsonObject>> getTemplatesJson(@HeaderMap Map<String, String> headers);
    
    @GET("categories")
    Call<List<JsonObject>> getCategories(@HeaderMap Map<String, String> headers);
    
    @GET("icons")
    Call<List<JsonObject>> getIcons(@HeaderMap Map<String, String> headers);
    
    @GET("templates/{id}")
    Call<JsonObject> getTemplate(@Path("id") String id, @HeaderMap Map<String, String> headers);
    
    @GET("categories/{id}")
    Call<JsonObject> getCategory(@Path("id") String id, @HeaderMap Map<String, String> headers);
    
    @GET("icons/{id}")
    Call<JsonObject> getIcon(@Path("id") String id, @HeaderMap Map<String, String> headers);

    // Coins endpoints
    @GET("coins/{deviceId}")
    Call<JsonObject> getCoins(@Path("deviceId") String deviceId);

    @POST("coins/add")
    Call<JsonObject> addCoins(@Body Map<String, Object> requestBody);

    @POST("coins/{deviceId}/unlock")
    Call<JsonObject> unlockFeature(@Path("deviceId") String deviceId);

    // Unlock validation endpoints
    @POST("coins/validate")
    Call<JsonObject> validateUnlock(@Body Map<String, Object> requestBody);

    @POST("coins/unlock/report")
    Call<JsonObject> reportUnlock(@Body Map<String, Object> payload);

    /**
     * Report security violation
     * @param payload Security violation details
     * @return Response
     */
    @POST("security/violation")
    Call<JsonObject> reportSecurityViolation(@Body Map<String, Object> payload);

    // Authentication methods
    @POST("coins/register")
    Call<JsonObject> registerNewUser(@Body Map<String, Object> payload);
    
    @GET("auth/validate")
    Call<JsonObject> validateAppSignature(@HeaderMap Map<String, String> headers);
    
    /**
     * Change user password
     */
    @POST("auth/change-password")
    Call<JsonObject> changePassword(@Body Map<String, String> passwordRequest);

    // Remove non-HTTP method from interface

    // SMS verification endpoints
    /**
     * Send SMS verification code to phone number
     * @param body Request body containing phoneNumber
     * @return Response
     */
    @POST("auth/send-verification-code")
    Call<Object> sendVerificationCode(@Body Map<String, Object> body);
    
    /**
     * Verify SMS code
     * @param body Request body containing phoneNumber and verificationCode
     * @return Response
     */
    @POST("auth/verify-code")
    Call<Object> verifyCode(@Body Map<String, Object> body);
    
    // User endpoints
    /**
     * Register a new user
     * @param body User data including phoneNumber, password, etc.
     * @return API response with user data
     */
    @POST("users/register")
    Call<JsonObject> registerUser(@Body Map<String, Object> body);
    
    /**
     * Login with phone number and password
     * @param body Login credentials including phoneNumber and password
     * @return Response with tokens and user data
     */
    @POST("auth/login")
    Call<Object> loginUser(@Body Map<String, Object> body);
    
    /**
     * Refresh access token
     * @param body Request body containing refreshToken
     * @return Response with new tokens
     */
    @POST("auth/refresh")
    Call<Object> refreshToken(@Body Map<String, Object> body);
    
    /**
     * Logout user
     * @return Response
     */
    @POST("auth/logout")
    Call<Object> logout();
    
    /**
     * Send password reset code via SMS
     * @param body Request body containing phoneNumber
     * @return Response
     */
    @POST("auth/password/reset/send-code")
    Call<Object> sendPasswordResetCode(@Body Map<String, Object> body);
    
    /**
     * Reset password using verification code
     * @param body Request containing phoneNumber, verificationCode, and newPassword
     * @return Response
     */
    @POST("auth/password/reset")
    Call<Object> resetPassword(@Body Map<String, Object> body);
    
    /**
     * Get current user info
     * @return Response with user data
     */
    @GET("auth/me")
    Call<Object> getCurrentUser();

    /**
     * Update user profile in MongoDB after Firebase authentication
     * @param userData Map containing user profile data (uid, deviceId, displayName, email, profilePhoto, lastOnline)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @POST("users/profile")
    Call<JsonObject> updateUserProfile(
        @Body Map<String, Object> userData,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Register device with the server
     * @param requestBody Device registration request data
     * @return Response
     */
    @POST("device/register")
    Call<JsonObject> registerDevice(@Body JsonObject requestBody);

    /**
     * Register user with device ID
     * @param requestBody Request body containing deviceId
     * @return Response with user data
     */
    @POST("users/register")
    Call<JsonObject> registerDeviceUser(@Body Map<String, Object> requestBody);

    /**
     * Update user activity (online status and category visit)
     * @param body Request body containing uid, category, and source
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @PUT("users/activity")
    Call<JsonObject> updateUserActivity(
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Record a template view with category
     * @param body Request body containing uid, templateId, and category
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @PUT("users/template-view")
    Call<JsonObject> recordTemplateView(
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Get personalized recommendations for a user
     * @param uid Firebase UID
     * @param authToken Firebase authentication token (for Authorization header, optional)
     * @return Response with recommendations data
     */
    @GET("users/{uid}/recommendations")
    Call<JsonObject> getUserRecommendations(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Get user data by deviceId (legacy method)
     * @param deviceId Device ID
     * @return Response with user data
     * @deprecated Use getUserByUid instead
     */
    @GET("users/{deviceId}")
    @Deprecated
    Call<JsonObject> getUserByDeviceId(@Path("deviceId") String deviceId);
    
    // User activity and engagement tracking
    
    /**
     * Record user engagement with detailed metrics
     * @param body Engagement data containing uid, type, templateId, category, etc.
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response
     */
    @POST("users/engagement")
    Call<JsonObject> recordEngagement(
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Sync multiple engagement records in a batch
     * @param body JSON object containing uid and array of engagement data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response
     */
    @POST("users/engagement/sync")
    Call<JsonObject> syncEngagementData(
        @Body JsonObject body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get personalized recommendations with detailed parameters
     * @param body Request containing deviceId and filtering options
     * @return Response with recommended templates
     */
    @POST("users/recommendations/advanced")
    Call<JsonObject> getAdvancedRecommendations(@Body Map<String, Object> body);

    // AdMob endpoints
    /**
     * Get ad units with required headers
     * Required headers:
     * - x-api-key
     * - x-app-signature
     * - x-device-id
     * 
     * @param adType Type of ad (app_open, banner, interstitial, rewarded)
     * @return Response containing ad units
     */
    @GET("admob/units")
    Call<JsonObject> getAdUnits(
        @Query("adType") String adType,
        @retrofit2.http.Header("x-api-key") String apiKey,
        @retrofit2.http.Header("x-app-signature") String appSignature,
        @retrofit2.http.Header("x-device-id") String deviceId
    );

    /**
     * Get all ad units with required headers
     * Required headers:
     * - x-api-key
     * - x-app-signature
     * - x-device-id
     * 
     * @return Response containing all ad units
     */
    @GET("admob/units")
    Call<JsonObject> getAllAdUnits(
        @retrofit2.http.Header("x-api-key") String apiKey,
        @retrofit2.http.Header("x-app-signature") String appSignature,
        @retrofit2.http.Header("x-device-id") String deviceId
    );

    /**
     * Get ad status
     * @param headers Headers with authentication data
     * @param adType Type of ads to filter by (optional)
     * @return AdMobResponse containing ad status
     */
    @GET("admob/status")
    Call<AdMobResponse> getAdStatus(
        @HeaderMap Map<String, String> headers,
        @Query("type") String adType
    );

    /**
     * Record impression for ad.
     */
    @POST("admob/impression")
    Call<JsonObject> recordImpression(@HeaderMap Map<String, String> headers, @Body JsonObject body);

    /**
     * Record click for ad.
     */
    @POST("admob/click")
    Call<JsonObject> recordClick(@HeaderMap Map<String, String> headers, @Body JsonObject body);

    /**
     * Process reward for rewarded ad.
     */
    @POST("admob/reward")
    Call<JsonObject> processReward(@HeaderMap Map<String, String> headers, @Body JsonObject body);

    /**
     * Track user engagement with ad.
     */
    @POST("admob/engagement")
    Call<JsonObject> trackEngagement(@HeaderMap Map<String, String> headers, @Body JsonObject body);
    
    // Sponsored Ads endpoints
    /**
     * Get active sponsored ads for display
     * @return Response containing list of sponsored ads
     */
    @GET("sponsored-ads")
    Call<SponsoredAdResponse> getSponsoredAds();
    
    /**
     * Get sponsored ads for rotation with exclusion support
     * @param queryMap Map of query parameters including 'location', 'limit', and 'exclude'
     * @return Response containing list of sponsored ads
     */
    @GET("sponsored-ads/rotation")
    Call<SponsoredAdResponse> getSponsoredAdsForRotation(@QueryMap Map<String, Object> queryMap);

    /**
     * Get sponsored ads with fair distribution based on priority and impressions
     * @param queryMap Map of query parameters including 'location' and 'limit'
     * @return Response containing list of sponsored ads
     */
    @GET("sponsored-ads/fair-distribution")
    Call<SponsoredAdResponse> getFairDistributedAds(@QueryMap Map<String, Object> queryMap);
    
    /**
     * Record impression when a sponsored ad is viewed
     * @param id The ID of the sponsored ad
     * @param deviceId The device ID for tracking
     * @return Response indicating success or failure
     */
    @POST("sponsored-ads/viewed/{id}")
    Call<JsonObject> recordSponsoredAdImpression(
        @Path("id") String id,
        @retrofit2.http.Header("x-device-id") String deviceId
    );
    
    /**
     * Record click when a sponsored ad is clicked
     * @param id The ID of the sponsored ad
     * @param deviceId The device ID for tracking
     * @return Response indicating success or failure
     */
    @POST("sponsored-ads/clicked/{id}")
    Call<JsonObject> recordSponsoredAdClick(
        @Path("id") String id,
        @retrofit2.http.Header("x-device-id") String deviceId
    );

    // About endpoints
    /**
     * Get active about content
     * @return Response containing about content
     */
    @GET("about")
    Call<BaseResponse<About>> getAbout();

    /**
     * Create new about content (admin only)
     * @param about About content
     * @return Response
     */
    @POST("about")
    Call<BaseResponse<About>> createAbout(@Body About about);

    /**
     * Update about content (admin only)
     * @param id About content ID
     * @param about Updated about content
     * @return Response
     */
    @PUT("about/{id}")
    Call<BaseResponse<About>> updateAbout(@Path("id") String id, @Body About about);

    // Contact endpoints
    /**
     * Get active contact content
     * @return Response containing contact content
     */
    @GET("contact")
    Call<BaseResponse<Contact>> getContact();

    /**
     * Create new contact content (admin only)
     * @param contact Contact content
     * @return Response
     */
    @POST("contact")
    Call<BaseResponse<Contact>> createContact(@Body Contact contact);

    /**
     * Update contact content (admin only)
     * @param id Contact content ID
     * @param contact Updated contact content
     * @return Response
     */
    @PUT("contact/{id}")
    Call<BaseResponse<Contact>> updateContact(@Path("id") String id, @Body Contact contact);
    
    // New User Preference endpoints
    /**
     * Update user preferences
     * @param body Request containing deviceId and preferences
     * @return Response with updated preferences
     */
    @PUT("users/preferences")
    Call<JsonObject> updateUserPreferences(@Body Map<String, Object> body);
    
    /**
     * Get user's favorite templates
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header, optional)
     * @return Response with favorite templates
     */
    @GET("users/{uid}/templates/favorites")
    Call<JsonObject> getUserFavorites(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get user's liked templates
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header, optional)
     * @return Response with liked templates
     */
    @GET("users/{uid}/templates/likes")
    Call<JsonObject> getUserLikes(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get user's recently used templates
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header, optional)
     * @return Response with recent templates
     */
    @GET("users/{uid}/templates/recent")
    Call<JsonObject> getUserRecentTemplates(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Link existing user with Firebase UID
     * @param deviceId Device ID of the existing user
     * @param body Request containing uid and optional profile info (displayName, email, photoUrl)
     * @return Response with user data
     */
    @PUT("users/{deviceId}/link-firebase")
    Call<JsonObject> linkFirebaseUser(
        @Path("deviceId") String deviceId,
        @Body Map<String, Object> body
    );
    
    /**
     * Update user subscription status
     * @param uid User ID (Firebase UID)
     * @param body Subscription details
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated user data
     */
    @PUT("users/{uid}/subscription")
    Call<JsonObject> updateUserSubscription(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update user push notification preferences
     * @param uid User ID (Firebase UID)
     * @param body Push preferences
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated preferences
     */
    @PUT("users/{uid}/push-preferences")
    Call<JsonObject> updatePushPreferences(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Subscribe to notification topics
     * @param uid User ID (Firebase UID)
     * @param body Topics to subscribe to
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated subscription status
     */
    @POST("users/{uid}/topics/subscribe")
    Call<JsonObject> subscribeToTopics(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Unsubscribe from notification topics
     * @param uid User ID (Firebase UID)
     * @param body Topics to unsubscribe from
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated subscription status
     */
    @POST("users/{uid}/topics/unsubscribe")
    Call<JsonObject> unsubscribeFromTopics(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Add a template to user's favorites
     * @param uid User ID (Firebase UID)
     * @param templateId Template ID to add to favorites
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated favorites
     */
    @PUT("users/{uid}/favorites/{templateId}")
    Call<JsonObject> addToFavorites(
        @Path("uid") String uid,
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Remove a template from user's favorites
     * @param uid User ID (Firebase UID)
     * @param templateId Template ID to remove from favorites
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated favorites
     */
    @DELETE("users/{uid}/favorites/{templateId}")
    Call<JsonObject> removeFromFavorites(
        @Path("uid") String uid,
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Add a template to user's likes
     * @param uid User ID (Firebase UID)
     * @param templateId Template ID to like
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated likes
     */
    @PUT("users/{uid}/likes/{templateId}")
    Call<JsonObject> likeTemplate(
        @Path("uid") String uid,
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Remove a template from user's likes
     * @param uid User ID (Firebase UID)
     * @param templateId Template ID to unlike
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated likes
     */
    @DELETE("users/{uid}/likes/{templateId}")
    Call<JsonObject> unlikeTemplate(
        @Path("uid") String uid,
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Generate or update referral code for a user
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with referral code
     */
    @POST("users/{uid}/referral")
    Call<JsonObject> generateReferralCode(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Apply a referral code to a user
     * @param uid User ID (Firebase UID)
     * @param body Request body containing referralCode
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @POST("users/{uid}/apply-referral")
    Call<JsonObject> applyReferralCode(
        @Path("uid") String uid,
        @Body Map<String, String> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get user's category visit history
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with category visit data
     */
    @GET("users/{uid}/categories")
    Call<JsonObject> getUserCategories(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get user engagement analytics
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with engagement analytics data
     */
    @GET("users/{uid}/analytics/engagement")
    Call<JsonObject> getUserEngagementAnalytics(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Mute notifications for a specified duration
     * @param uid User ID (Firebase UID)
     * @param body Request body containing duration in hours
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @PUT("users/{uid}/notifications/mute")
    Call<JsonObject> muteNotifications(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Unmute notifications
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @PUT("users/{uid}/notifications/unmute")
    Call<JsonObject> unmuteNotifications(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get user's notification status
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with notification status
     */
    @GET("users/{uid}/notifications/status")
    Call<JsonObject> getNotificationStatus(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * First-time authentication with Firebase
     * Determines if user exists and handles new user creation
     * @param body Request body containing uid and optional deviceId
     * @return Response with user data and isNewUser flag
     */
    @POST("users/auth")
    Call<JsonObject> authenticateWithFirebase(@Body Map<String, Object> body);
    
    /**
     * Get user data by Firebase UID
     * @param uid Firebase UID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with user data
     */
    @GET("users/{uid}")
    Call<JsonObject> getUserByUid(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );
}
