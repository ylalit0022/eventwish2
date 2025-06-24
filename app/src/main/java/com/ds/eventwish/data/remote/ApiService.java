package com.ds.eventwish.data.remote;

import com.ds.eventwish.data.model.About;
import com.ds.eventwish.data.model.Contact;
import com.ds.eventwish.data.model.CategoryIcon;
import com.ds.eventwish.data.model.Festival;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.User;
import com.ds.eventwish.data.model.response.ApiResponse;
import com.ds.eventwish.data.model.response.BaseResponse;
import com.ds.eventwish.data.model.response.CategoryIconResponse;
import com.ds.eventwish.data.model.response.SessionsResponse;
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
    
    // Session management endpoints
    
    /**
     * Get user's active sessions
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with active sessions
     */
    @GET("users/{uid}/sessions")
    Call<ApiResponse<SessionsResponse>> getUserSessions(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Invalidate all other sessions except current one
     * @param uid User ID (Firebase UID)
     * @param body Request body containing deviceId
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @POST("users/{uid}/sessions/invalidate")
    Call<ApiResponse<Void>> invalidateOtherSessions(
        @Path("uid") String uid,
        @Body Map<String, String> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    

    
    /**
     * Update activity timestamp for a device session
     * @param uid User ID (Firebase UID)
     * @param body Request body containing deviceId
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @POST("users/{uid}/sessions/update")
    Call<ApiResponse<Void>> updateSessionActivity(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    // =============================================================================
    // COMPREHENSIVE USER CRUD OPERATIONS BASED ON SCHEMA FIELDS
    // =============================================================================

    /**
     * Get complete user profile with all fields
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with complete user data
     */
    @GET("users/{uid}/complete")
    Call<ApiResponse<User>> getCompleteUserProfile(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );

    // =============================================================================
    // DEVICE & SESSION MANAGEMENT
    // =============================================================================

    /**
     * Add or update device session
     * @param uid User ID (Firebase UID)
     * @param body Device session data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with active sessions
     */
    @POST("users/{uid}/device-sessions")
    Call<JsonObject> addDeviceSession(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Remove device session
     * @param uid User ID (Firebase UID)
     * @param deviceId Device ID to remove
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("users/{uid}/device-sessions/{deviceId}")
    Call<JsonObject> removeDeviceSession(
        @Path("uid") String uid,
        @Path("deviceId") String deviceId,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Update device session activity
     * @param uid User ID (Firebase UID)
     * @param deviceId Device ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @PUT("users/{uid}/device-sessions/{deviceId}/activity")
    Call<JsonObject> updateDeviceSessionActivity(
        @Path("uid") String uid,
        @Path("deviceId") String deviceId,
        @retrofit2.http.Header("Authorization") String authToken
    );

    // =============================================================================
    // SUBSCRIPTION MANAGEMENT
    // =============================================================================

    /**
     * Update user subscription details
     * @param uid User ID (Firebase UID)
     * @param subscriptionData Subscription details
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated subscription
     */
    @PUT("users/{uid}/subscription")
    Call<JsonObject> updateSubscription(
        @Path("uid") String uid,
        @Body Map<String, Object> subscriptionData,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Add subscription history entry
     * @param uid User ID (Firebase UID)
     * @param body Subscription history data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with subscription history
     */
    @POST("users/{uid}/subscription-history")
    Call<JsonObject> addSubscriptionHistory(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Update user subscription offer
     * @param uid User ID (Firebase UID)
     * @param offerData Subscription offer details
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated offer
     */
    @PUT("users/{uid}/subscription-offer")
    Call<JsonObject> updateSubscriptionOffer(
        @Path("uid") String uid,
        @Body Map<String, Object> offerData,
        @retrofit2.http.Header("Authorization") String authToken
    );

    // =============================================================================
    // AI USAGE MANAGEMENT
    // =============================================================================

    /**
     * Update AI usage data
     * @param uid User ID (Firebase UID)
     * @param body AI usage data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated AI usage
     */
    @PUT("users/{uid}/ai-usage")
    Call<JsonObject> updateAIUsage(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Increment AI generation count
     * @param uid User ID (Firebase UID)
     * @param body Request body containing prompt and style
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated AI usage and remaining quota
     */
    @POST("users/{uid}/ai-usage/increment")
    Call<JsonObject> incrementAIUsage(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    // =============================================================================
    // CATEGORY VISITS MANAGEMENT
    // =============================================================================

    /**
     * Record category visit
     * @param uid User ID (Firebase UID)
     * @param body Category visit data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated categories
     */
    @POST("users/{uid}/categories/visit")
    Call<JsonObject> recordCategoryVisit(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Get category visit statistics
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with category statistics
     */
    @GET("users/{uid}/categories/stats")
    Call<JsonObject> getCategoryStats(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );

    // =============================================================================
    // TEMPLATE AFFINITY MANAGEMENT
    // =============================================================================

    /**
     * Update template affinity scores
     * @param uid User ID (Firebase UID)
     * @param body Template affinity data (tag, score)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated template affinity
     */
    @PUT("users/{uid}/template-affinity")
    Call<JsonObject> updateTemplateAffinity(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Get top template affinities
     * @param uid User ID (Firebase UID)
     * @param limit Number of top affinities to return
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with top template affinities
     */
    @GET("users/{uid}/template-affinity/top")
    Call<JsonObject> getTopTemplateAffinities(
        @Path("uid") String uid,
        @Query("limit") int limit,
        @retrofit2.http.Header("Authorization") String authToken
    );

    // =============================================================================
    // IGNORED TEMPLATES MANAGEMENT
    // =============================================================================

    /**
     * Add template to ignored list
     * @param uid User ID (Firebase UID)
     * @param body Template ignore data (templateId, score)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated ignored templates
     */
    @POST("users/{uid}/ignored-templates")
    Call<JsonObject> addIgnoredTemplate(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Remove template from ignored list
     * @param uid User ID (Firebase UID)
     * @param templateId Template ID to remove from ignored list
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("users/{uid}/ignored-templates/{templateId}")
    Call<JsonObject> removeIgnoredTemplate(
        @Path("uid") String uid,
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );

    // =============================================================================
    // DRAFTS MANAGEMENT
    // =============================================================================

    /**
     * Save or update draft
     * @param uid User ID (Firebase UID)
     * @param body Draft data (templateId, html)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated drafts
     */
    @POST("users/{uid}/drafts")
    Call<JsonObject> saveDraft(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Get user's drafts
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with user's drafts
     */
    @GET("users/{uid}/drafts")
    Call<JsonObject> getUserDrafts(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Delete specific draft
     * @param uid User ID (Firebase UID)
     * @param templateId Template ID of the draft to delete
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("users/{uid}/drafts/{templateId}")
    Call<JsonObject> deleteDraft(
        @Path("uid") String uid,
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );

    // =============================================================================
    // FCM TOKENS MANAGEMENT
    // =============================================================================

    /**
     * Add or update FCM token
     * @param uid User ID (Firebase UID)
     * @param body FCM token data (token, platform)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated FCM tokens
     */
    @POST("users/{uid}/fcm-tokens")
    Call<JsonObject> addFCMToken(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Remove FCM token
     * @param uid User ID (Firebase UID)
     * @param token FCM token to remove
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("users/{uid}/fcm-tokens/{token}")
    Call<JsonObject> removeFCMToken(
        @Path("uid") String uid,
        @Path("token") String token,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Subscribe FCM token to topics
     * @param uid User ID (Firebase UID)
     * @param token FCM token
     * @param body Topics data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated FCM tokens
     */
    @POST("users/{uid}/fcm-tokens/{token}/topics/subscribe")
    Call<JsonObject> subscribeFCMTokenToTopics(
        @Path("uid") String uid,
        @Path("token") String token,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    // =============================================================================
    // CACHED HOME FEED MANAGEMENT
    // =============================================================================

    /**
     * Update cached home feed
     * @param uid User ID (Firebase UID)
     * @param body Feed data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated cached feed
     */
    @PUT("users/{uid}/cached-feed")
    Call<JsonObject> updateCachedHomeFeed(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Get cached home feed
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with cached feed data
     */
    @GET("users/{uid}/cached-feed")
    Call<JsonObject> getCachedHomeFeed(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );

    // =============================================================================
    // BLOCK MANAGEMENT (Admin Operations)
    // =============================================================================

    /**
     * Block a user (Admin only)
     * @param uid User ID (Firebase UID)
     * @param body Block data (reason, expiresAt, notes)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with block info
     */
    @PUT("users/{uid}/block")
    Call<JsonObject> blockUser(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Unblock a user (Admin only)
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @PUT("users/{uid}/unblock")
    Call<JsonObject> unblockUser(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );

    // =============================================================================
    // BULK OPERATIONS
    // =============================================================================

    /**
     * Bulk update multiple user fields
     * @param uid User ID (Firebase UID)
     * @param updateData Multiple fields to update
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated user data
     */
    @POST("users/{uid}/bulk-update")
    Call<JsonObject> bulkUpdateUser(
        @Path("uid") String uid,
        @Body Map<String, Object> updateData,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Delete user account (GDPR compliance)
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("users/{uid}")
    Call<JsonObject> deleteUserAccount(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );

    // =============================================================================
    // ADVANCED USER QUERIES
    // =============================================================================

    /**
     * Get user analytics data
     * @param uid User ID (Firebase UID)
     * @param startDate Start date for analytics (optional)
     * @param endDate End date for analytics (optional)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with user analytics
     */
    @GET("users/{uid}/analytics")
    Call<JsonObject> getUserAnalytics(
        @Path("uid") String uid,
        @Query("startDate") String startDate,
        @Query("endDate") String endDate,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Get user activity timeline
     * @param uid User ID (Firebase UID)
     * @param limit Number of activities to return
     * @param offset Offset for pagination
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with activity timeline
     */
    @GET("users/{uid}/activity-timeline")
    Call<JsonObject> getUserActivityTimeline(
        @Path("uid") String uid,
        @Query("limit") int limit,
        @Query("offset") int offset,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Search users (Admin only)
     * @param query Search query
     * @param filters Search filters
     * @param page Page number
     * @param limit Items per page
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with search results
     */
    @GET("users/search")
    Call<JsonObject> searchUsers(
        @Query("q") String query,
        @QueryMap Map<String, Object> filters,
        @Query("page") int page,
        @Query("limit") int limit,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Export user data (GDPR compliance)
     * @param uid User ID (Firebase UID)
     * @param format Export format (json, csv, xml)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with exported data
     */
    @GET("users/{uid}/export")
    Call<JsonObject> exportUserData(
        @Path("uid") String uid,
        @Query("format") String format,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Get user subscription eligibility
     * @param uid User ID (Firebase UID)
     * @param planLevel Plan level to check (BASIC, PREMIUM, PRO)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with eligibility information
     */
    @GET("users/{uid}/subscription/eligibility")
    Call<JsonObject> getSubscriptionEligibility(
        @Path("uid") String uid,
        @Query("planLevel") String planLevel,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Apply pricing rule to user
     * @param uid User ID (Firebase UID)
     * @param body Pricing rule application data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with applied pricing
     */
    @POST("users/{uid}/pricing-rule/apply")
    Call<JsonObject> applyPricingRule(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Get user's viewed templates map
     * @param uid User ID (Firebase UID)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with viewed templates map
     */
    @GET("users/{uid}/viewed-templates")
    Call<JsonObject> getViewedTemplatesMap(
        @Path("uid") String uid,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Update user's viewed templates map
     * @param uid User ID (Firebase UID)
     * @param body Viewed templates data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @PUT("users/{uid}/viewed-templates")
    Call<JsonObject> updateViewedTemplatesMap(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    // =============================================================================
    // TEMPLATE INTERACTION TRACKING WITH COUNTS
    // =============================================================================

    /**
     * Record template share and increment share count
     * @param uid User ID (Firebase UID)
     * @param templateId Template ID to share
     * @param body Share data (shareMethod, category)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated template counts and user data
     */
    @POST("users/{uid}/templates/{templateId}/share")
    Call<JsonObject> recordTemplateShare(
        @Path("uid") String uid,
        @Path("templateId") String templateId,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Get template interaction status for a user
     * @param uid User ID (Firebase UID)
     * @param templateId Template ID to check
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with interaction status (liked, favorited, shared)
     */
    @GET("users/{uid}/templates/{templateId}/interaction-status")
    Call<JsonObject> getTemplateInteractionStatus(
        @Path("uid") String uid,
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );

    /**
     * Get bulk template interaction status for multiple templates
     * @param uid User ID (Firebase UID)
     * @param body Request containing templateIds array
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with interaction status for all templates
     */
    @POST("users/{uid}/templates/interaction-status/bulk")
    Call<JsonObject> getBulkTemplateInteractionStatus(
        @Path("uid") String uid,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );

    // =============================================================================
    // CREATOR PROFILE ENDPOINTS
    // =============================================================================

    /**
     * Get creator profile information for a single template
     * @param templateId The ID of the template
     * @return Creator profile information with fallback logic applied server-side
     */
    @GET("templates/{templateId}/creator")
    Call<JsonObject> getTemplateCreatorProfile(@Path("templateId") String templateId);

    /**
     * Get creator profiles for multiple templates in a single request
     * @param body Request body containing templateIds array
     * @return Map of template IDs to creator profile information
     */
    @POST("templates/creators/batch")
    Call<JsonObject> getBatchCreatorProfiles(@Body Map<String, Object> body);

    /**
     * Get detailed statistics for a specific creator
     * @param userId The creator's user ID
     * @return Creator statistics including template count, total likes, etc.
     */
    @GET("templates/creators/{userId}/stats")
    Call<JsonObject> getCreatorStats(@Path("userId") String userId);

    /**
     * Health check endpoint for template interactions
     * @return Health status of template interaction system
     */
    @GET("templates/health/template-interactions")
    Call<JsonObject> getTemplateInteractionsHealth();

    // =============================================================================
    // TEMPLATE CORE CONTENT API - Phase 2 Implementation
    // =============================================================================
    
    /**
     * Create new template with content
     * @param templateData Template creation data (title, category, htmlContent, etc.)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with created template
     */
    @POST("templates")
    Call<JsonObject> createTemplate(
        @Body Map<String, Object> templateData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update template content
     * @param templateId Template ID to update
     * @param updateData Template update data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated template
     */
    @PUT("templates/{templateId}")
    Call<JsonObject> updateTemplate(
        @Path("templateId") String templateId,
        @Body Map<String, Object> updateData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Delete template
     * @param templateId Template ID to delete
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/{templateId}")
    Call<JsonObject> deleteTemplate(
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Duplicate template
     * @param templateId Template ID to duplicate
     * @param body Duplication options (newTitle, modifications)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with duplicated template
     */
    @POST("templates/{templateId}/duplicate")
    Call<JsonObject> duplicateTemplate(
        @Path("templateId") String templateId,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Validate template content
     * @param templateData Template data to validate
     * @return Response with validation results
     */
    @POST("templates/validate")
    Call<JsonObject> validateTemplate(@Body Map<String, Object> templateData);
    
    /**
     * Get template content with version history
     * @param templateId Template ID
     * @param version Version number (optional)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with template content and version info
     */
    @GET("templates/{templateId}/content")
    Call<JsonObject> getTemplateContent(
        @Path("templateId") String templateId,
        @Query("version") String version,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update template content with versioning
     * @param templateId Template ID
     * @param contentData Content update data (htmlContent, cssContent, jsContent)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated content and new version
     */
    @PUT("templates/{templateId}/content")
    Call<JsonObject> updateTemplateContent(
        @Path("templateId") String templateId,
        @Body Map<String, Object> contentData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template preview with rendering
     * @param templateId Template ID
     * @param previewData Preview customization data
     * @return Response with rendered preview
     */
    @POST("templates/{templateId}/preview")
    Call<JsonObject> getTemplatePreview(
        @Path("templateId") String templateId,
        @Body Map<String, Object> previewData
    );
    
    /**
     * Bulk update template properties
     * @param templateId Template ID
     * @param updateData Bulk update data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated template
     */
    @PUT("templates/{templateId}/bulk-update")
    Call<JsonObject> bulkUpdateTemplate(
        @Path("templateId") String templateId,
        @Body Map<String, Object> updateData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template metadata only (no content)
     * @param templateId Template ID
     * @return Response with template metadata
     */
    @GET("templates/{templateId}/metadata")
    Call<JsonObject> getTemplateMetadata(@Path("templateId") String templateId);
    
    /**
     * Update template metadata
     * @param templateId Template ID
     * @param metadataData Metadata update data (title, description, tags, etc.)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated metadata
     */
    @PUT("templates/{templateId}/metadata")
    Call<JsonObject> updateTemplateMetadata(
        @Path("templateId") String templateId,
        @Body Map<String, Object> metadataData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Search templates with advanced filters
     * @param queryMap Search parameters (query, category, tags, creator, etc.)
     * @return Response with search results
     */
    @GET("templates/search")
    Call<JsonObject> searchTemplates(@QueryMap Map<String, Object> queryMap);
    
    /**
     * Get templates by multiple criteria
     * @param filters Filter criteria
     * @param page Page number
     * @param limit Items per page
     * @param sortBy Sort field
     * @param sortOrder Sort order (asc/desc)
     * @return Response with filtered templates
     */
    @POST("templates/filter")
    Call<JsonObject> filterTemplates(
        @Body Map<String, Object> filters,
        @Query("page") int page,
        @Query("limit") int limit,
        @Query("sortBy") String sortBy,
        @Query("sortOrder") String sortOrder
    );
    
    /**
     * Get template version history
     * @param templateId Template ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with version history
     */
    @GET("templates/{templateId}/versions")
    Call<JsonObject> getTemplateVersions(
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    // =============================================================================
    // TEMPLATE MONETIZATION API - Phase 2 Implementation
    // =============================================================================
    
    /**
     * Set template as premium
     * @param templateId Template ID
     * @param pricingData Pricing data (price, currency, planLevel)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated monetization status
     */
    @PUT("templates/{templateId}/premium")
    Call<JsonObject> setTemplatePremium(
        @Path("templateId") String templateId,
        @Body Map<String, Object> pricingData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Remove premium status from template
     * @param templateId Template ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/{templateId}/premium")
    Call<JsonObject> removeTemplatePremium(
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update template pricing
     * @param templateId Template ID
     * @param pricingData New pricing data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated pricing
     */
    @PUT("templates/{templateId}/pricing")
    Call<JsonObject> updateTemplatePricing(
        @Path("templateId") String templateId,
        @Body Map<String, Object> pricingData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template monetization status
     * @param templateId Template ID
     * @return Response with monetization details
     */
    @GET("templates/{templateId}/monetization")
    Call<JsonObject> getTemplateMonetization(@Path("templateId") String templateId);
    
    /**
     * Set template as featured
     * @param templateId Template ID
     * @param featuredData Featured status data (priority, duration, placement)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated featured status
     */
    @PUT("templates/{templateId}/featured")
    Call<JsonObject> setTemplateFeatured(
        @Path("templateId") String templateId,
        @Body Map<String, Object> featuredData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Remove featured status from template
     * @param templateId Template ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/{templateId}/featured")
    Call<JsonObject> removeTemplateFeatured(
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Set template as trending
     * @param templateId Template ID
     * @param trendingData Trending status data (score, duration, category)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated trending status
     */
    @PUT("templates/{templateId}/trending")
    Call<JsonObject> setTemplateTrending(
        @Path("templateId") String templateId,
        @Body Map<String, Object> trendingData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Remove trending status from template
     * @param templateId Template ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/{templateId}/trending")
    Call<JsonObject> removeTemplateTrending(
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Flag template for review
     * @param templateId Template ID
     * @param flagData Flag data (reason, severity, reportedBy)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with flag status
     */
    @PUT("templates/{templateId}/flag")
    Call<JsonObject> flagTemplate(
        @Path("templateId") String templateId,
        @Body Map<String, Object> flagData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Remove flag from template
     * @param templateId Template ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/{templateId}/flag")
    Call<JsonObject> unflagTemplate(
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Mark template as low performing
     * @param templateId Template ID
     * @param performanceData Performance data (metrics, thresholds)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with performance status
     */
    @PUT("templates/{templateId}/low-performing")
    Call<JsonObject> markTemplateLowPerforming(
        @Path("templateId") String templateId,
        @Body Map<String, Object> performanceData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Remove low performing status
     * @param templateId Template ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/{templateId}/low-performing")
    Call<JsonObject> removeTemplateLowPerforming(
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template moderation history
     * @param templateId Template ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with moderation history
     */
    @GET("templates/{templateId}/moderation/history")
    Call<JsonObject> getTemplateModerationHistory(
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Bulk update template monetization status
     * @param monetizationData Bulk monetization update data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with update results
     */
    @POST("templates/monetization/bulk-update")
    Call<JsonObject> bulkUpdateMonetization(
        @Body Map<String, Object> monetizationData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get monetization analytics
     * @param period Analytics period (daily, weekly, monthly)
     * @param startDate Start date (optional)
     * @param endDate End date (optional)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with monetization analytics
     */
    @GET("templates/monetization/analytics")
    Call<JsonObject> getMonetizationAnalytics(
        @Query("period") String period,
        @Query("startDate") String startDate,
        @Query("endDate") String endDate,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get revenue reports
     * @param templateId Template ID (optional for specific template)
     * @param period Report period
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with revenue data
     */
    @GET("templates/monetization/revenue")
    Call<JsonObject> getRevenueReports(
        @Query("templateId") String templateId,
        @Query("period") String period,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    // =============================================================================
    // TEMPLATE METRICS API - Phase 2 Implementation
    // =============================================================================
    
    /**
     * Get template engagement metrics
     * @param templateId Template ID
     * @param period Metrics period (daily, weekly, monthly, all-time)
     * @return Response with engagement metrics
     */
    @GET("templates/{templateId}/metrics")
    Call<JsonObject> getTemplateMetrics(
        @Path("templateId") String templateId,
        @Query("period") String period
    );
    
    /**
     * Update template usage count
     * @param templateId Template ID
     * @param body Usage data (increment, category, userId)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated usage count
     */
    @PUT("templates/{templateId}/metrics/usage")
    Call<JsonObject> updateTemplateUsage(
        @Path("templateId") String templateId,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update template view count
     * @param templateId Template ID
     * @param body View data (increment, source, userId)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated view count
     */
    @PUT("templates/{templateId}/metrics/views")
    Call<JsonObject> updateTemplateViews(
        @Path("templateId") String templateId,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update template download count
     * @param templateId Template ID
     * @param body Download data (increment, format, userId)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated download count
     */
    @PUT("templates/{templateId}/metrics/downloads")
    Call<JsonObject> updateTemplateDownloads(
        @Path("templateId") String templateId,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update template share count
     * @param templateId Template ID
     * @param body Share data (increment, platform, userId)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated share count
     */
    @PUT("templates/{templateId}/metrics/shares")
    Call<JsonObject> updateTemplateShares(
        @Path("templateId") String templateId,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update template like count
     * @param templateId Template ID
     * @param body Like data (increment, userId)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated like count
     */
    @PUT("templates/{templateId}/metrics/likes")
    Call<JsonObject> updateTemplateLikes(
        @Path("templateId") String templateId,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update template favorite count
     * @param templateId Template ID
     * @param body Favorite data (increment, userId)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated favorite count
     */
    @PUT("templates/{templateId}/metrics/favorites")
    Call<JsonObject> updateTemplateFavorites(
        @Path("templateId") String templateId,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update template report count
     * @param templateId Template ID
     * @param body Report data (reason, severity, userId)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated report count
     */
    @PUT("templates/{templateId}/metrics/reports")
    Call<JsonObject> updateTemplateReports(
        @Path("templateId") String templateId,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update template rating
     * @param templateId Template ID
     * @param body Rating data (rating, userId, review)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated rating
     */
    @PUT("templates/{templateId}/metrics/rating")
    Call<JsonObject> updateTemplateRating(
        @Path("templateId") String templateId,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template weekly metrics
     * @param templateId Template ID
     * @param week Week identifier (YYYY-WW format)
     * @return Response with weekly metrics
     */
    @GET("templates/{templateId}/metrics/weekly")
    Call<JsonObject> getTemplateWeeklyMetrics(
        @Path("templateId") String templateId,
        @Query("week") String week
    );
    
    /**
     * Update template weekly metrics
     * @param templateId Template ID
     * @param body Weekly metrics data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated weekly metrics
     */
    @PUT("templates/{templateId}/metrics/weekly")
    Call<JsonObject> updateTemplateWeeklyMetrics(
        @Path("templateId") String templateId,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Reset template weekly metrics
     * @param templateId Template ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @POST("templates/{templateId}/metrics/weekly/reset")
    Call<JsonObject> resetTemplateWeeklyMetrics(
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template trending score
     * @param templateId Template ID
     * @param period Score period (current, weekly, monthly)
     * @return Response with trending score
     */
    @GET("templates/{templateId}/metrics/trending-score")
    Call<JsonObject> getTemplateTrendingScore(
        @Path("templateId") String templateId,
        @Query("period") String period
    );
    
    /**
     * Update template trending score
     * @param templateId Template ID
     * @param body Score data (score, factors, period)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated trending score
     */
    @PUT("templates/{templateId}/metrics/trending-score")
    Call<JsonObject> updateTemplateTrendingScore(
        @Path("templateId") String templateId,
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template performance analytics
     * @param templateId Template ID
     * @param period Analytics period
     * @param metrics Specific metrics to include
     * @return Response with performance analytics
     */
    @GET("templates/{templateId}/metrics/analytics")
    Call<JsonObject> getTemplatePerformanceAnalytics(
        @Path("templateId") String templateId,
        @Query("period") String period,
        @Query("metrics") String metrics
    );
    
    /**
     * Get template comparison metrics
     * @param templateId Template ID
     * @param compareWith Template IDs to compare with
     * @param metrics Metrics to compare
     * @return Response with comparison data
     */
    @GET("templates/{templateId}/metrics/compare")
    Call<JsonObject> getTemplateComparisonMetrics(
        @Path("templateId") String templateId,
        @Query("compareWith") String compareWith,
        @Query("metrics") String metrics
    );
    
    /**
     * Get template engagement timeline
     * @param templateId Template ID
     * @param startDate Start date for timeline
     * @param endDate End date for timeline
     * @param granularity Timeline granularity (hour, day, week)
     * @return Response with engagement timeline
     */
    @GET("templates/{templateId}/metrics/timeline")
    Call<JsonObject> getTemplateEngagementTimeline(
        @Path("templateId") String templateId,
        @Query("startDate") String startDate,
        @Query("endDate") String endDate,
        @Query("granularity") String granularity
    );
    
    /**
     * Bulk update template metrics
     * @param body Bulk metrics update data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with update results
     */
    @POST("templates/metrics/bulk-update")
    Call<JsonObject> bulkUpdateTemplateMetrics(
        @Body Map<String, Object> body,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get top performing templates by metrics
     * @param metric Metric to sort by (usage, views, likes, shares, etc.)
     * @param period Time period for metrics
     * @param limit Number of templates to return
     * @param category Category filter (optional)
     * @return Response with top performing templates
     */
    @GET("templates/metrics/top-performing")
    Call<JsonObject> getTopPerformingTemplates(
        @Query("metric") String metric,
        @Query("period") String period,
        @Query("limit") int limit,
        @Query("category") String category
    );
    
    /**
     * Get trending templates
     * @param period Trending period
     * @param limit Number of templates to return
     * @param category Category filter (optional)
     * @return Response with trending templates
     */
    @GET("templates/metrics/trending")
    Call<JsonObject> getTrendingTemplates(
        @Query("period") String period,
        @Query("limit") int limit,
        @Query("category") String category
    );
    
    /**
     * Get template metrics summary
     * @param templateIds List of template IDs
     * @param metrics Specific metrics to include
     * @return Response with metrics summary
     */
    @POST("templates/metrics/summary")
    Call<JsonObject> getTemplateMetricsSummary(
        @Body Map<String, Object> body
    );
    
    /**
     * Export template metrics
     * @param templateId Template ID
     * @param format Export format (json, csv, excel)
     * @param period Metrics period
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with exported metrics
     */
    @GET("templates/{templateId}/metrics/export")
    Call<JsonObject> exportTemplateMetrics(
        @Path("templateId") String templateId,
        @Query("format") String format,
        @Query("period") String period,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    // =============================================================================
    // TEMPLATE AI METADATA API - Phase 2 Implementation
    // =============================================================================
    
    /**
     * Get template AI metadata
     * @param templateId Template ID
     * @return Response with AI metadata
     */
    @GET("templates/{templateId}/ai")
    Call<JsonObject> getTemplateAIMetadata(@Path("templateId") String templateId);
    
    /**
     * Update template AI metadata
     * @param templateId Template ID
     * @param aiData AI metadata (isAIGenerated, aiModel, aiPrompt, etc.)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated AI metadata
     */
    @PUT("templates/{templateId}/ai")
    Call<JsonObject> updateTemplateAIMetadata(
        @Path("templateId") String templateId,
        @Body Map<String, Object> aiData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Mark template as AI generated
     * @param templateId Template ID
     * @param aiData AI generation data (model, prompt, style, stage)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated AI status
     */
    @PUT("templates/{templateId}/ai/generated")
    Call<JsonObject> markTemplateAsAIGenerated(
        @Path("templateId") String templateId,
        @Body Map<String, Object> aiData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Remove AI generated status from template
     * @param templateId Template ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/{templateId}/ai/generated")
    Call<JsonObject> removeAIGeneratedStatus(
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update AI prompt for template
     * @param templateId Template ID
     * @param promptData Prompt data (prompt, parameters, version)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated prompt
     */
    @PUT("templates/{templateId}/ai/prompt")
    Call<JsonObject> updateTemplateAIPrompt(
        @Path("templateId") String templateId,
        @Body Map<String, Object> promptData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get AI prompt history for template
     * @param templateId Template ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with prompt history
     */
    @GET("templates/{templateId}/ai/prompt/history")
    Call<JsonObject> getTemplateAIPromptHistory(
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update AI model information
     * @param templateId Template ID
     * @param modelData Model data (name, version, parameters)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated model info
     */
    @PUT("templates/{templateId}/ai/model")
    Call<JsonObject> updateTemplateAIModel(
        @Path("templateId") String templateId,
        @Body Map<String, Object> modelData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update AI style information
     * @param templateId Template ID
     * @param styleData Style data (style, parameters, settings)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated style info
     */
    @PUT("templates/{templateId}/ai/style")
    Call<JsonObject> updateTemplateAIStyle(
        @Path("templateId") String templateId,
        @Body Map<String, Object> styleData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update AI generation stage
     * @param templateId Template ID
     * @param stageData Stage data (stage, progress, status)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated generation stage
     */
    @PUT("templates/{templateId}/ai/stage")
    Call<JsonObject> updateTemplateAIStage(
        @Path("templateId") String templateId,
        @Body Map<String, Object> stageData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update AI generation metadata
     * @param templateId Template ID
     * @param metadataData Generation metadata (tokens, cost, duration, etc.)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated generation metadata
     */
    @PUT("templates/{templateId}/ai/metadata")
    Call<JsonObject> updateTemplateAIGenerationMetadata(
        @Path("templateId") String templateId,
        @Body Map<String, Object> metadataData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get AI-generated templates
     * @param limit Number of templates to return
     * @param model AI model filter (optional)
     * @param style AI style filter (optional)
     * @param page Page number for pagination
     * @return Response with AI-generated templates
     */
    @GET("templates/ai/generated")
    Call<JsonObject> getAIGeneratedTemplates(
        @Query("limit") int limit,
        @Query("model") String model,
        @Query("style") String style,
        @Query("page") int page
    );
    
    /**
     * Get templates by AI model
     * @param model AI model name
     * @param limit Number of templates to return
     * @param page Page number for pagination
     * @return Response with templates by model
     */
    @GET("templates/ai/model/{model}")
    Call<JsonObject> getTemplatesByAIModel(
        @Path("model") String model,
        @Query("limit") int limit,
        @Query("page") int page
    );
    
    /**
     * Get templates by AI style
     * @param style AI style name
     * @param limit Number of templates to return
     * @param page Page number for pagination
     * @return Response with templates by style
     */
    @GET("templates/ai/style/{style}")
    Call<JsonObject> getTemplatesByAIStyle(
        @Path("style") String style,
        @Query("limit") int limit,
        @Query("page") int page
    );
    
    /**
     * Search AI prompts
     * @param query Search query
     * @param model Model filter (optional)
     * @param limit Number of results to return
     * @return Response with matching prompts
     */
    @GET("templates/ai/prompts/search")
    Call<JsonObject> searchAIPrompts(
        @Query("q") String query,
        @Query("model") String model,
        @Query("limit") int limit
    );
    
    /**
     * Get popular AI prompts
     * @param limit Number of prompts to return
     * @param period Time period (daily, weekly, monthly)
     * @return Response with popular prompts
     */
    @GET("templates/ai/prompts/popular")
    Call<JsonObject> getPopularAIPrompts(
        @Query("limit") int limit,
        @Query("period") String period
    );
    
    /**
     * Generate template using AI
     * @param generationData Generation request data (prompt, model, style, parameters)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with generation job ID or generated template
     */
    @POST("templates/ai/generate")
    Call<JsonObject> generateTemplateWithAI(
        @Body Map<String, Object> generationData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get AI generation job status
     * @param jobId Generation job ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with job status and progress
     */
    @GET("templates/ai/generate/{jobId}/status")
    Call<JsonObject> getAIGenerationJobStatus(
        @Path("jobId") String jobId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Cancel AI generation job
     * @param jobId Generation job ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/ai/generate/{jobId}")
    Call<JsonObject> cancelAIGenerationJob(
        @Path("jobId") String jobId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Enhance template using AI
     * @param templateId Template ID to enhance
     * @param enhancementData Enhancement parameters (style, improvements, model)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with enhanced template or job ID
     */
    @POST("templates/{templateId}/ai/enhance")
    Call<JsonObject> enhanceTemplateWithAI(
        @Path("templateId") String templateId,
        @Body Map<String, Object> enhancementData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get AI enhancement suggestions
     * @param templateId Template ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with enhancement suggestions
     */
    @GET("templates/{templateId}/ai/suggestions")
    Call<JsonObject> getAIEnhancementSuggestions(
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Apply AI enhancement suggestion
     * @param templateId Template ID
     * @param suggestionId Suggestion ID to apply
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with applied enhancement
     */
    @POST("templates/{templateId}/ai/suggestions/{suggestionId}/apply")
    Call<JsonObject> applyAIEnhancementSuggestion(
        @Path("templateId") String templateId,
        @Path("suggestionId") String suggestionId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get AI models list
     * @param category Model category filter (optional)
     * @param capability Capability filter (optional)
     * @return Response with available AI models
     */
    @GET("templates/ai/models")
    Call<JsonObject> getAIModels(
        @Query("category") String category,
        @Query("capability") String capability
    );
    
    /**
     * Get AI model details
     * @param modelId Model ID
     * @return Response with model details and capabilities
     */
    @GET("templates/ai/models/{modelId}")
    Call<JsonObject> getAIModelDetails(@Path("modelId") String modelId);
    
    /**
     * Get AI styles list
     * @param category Style category filter (optional)
     * @param model Compatible model filter (optional)
     * @return Response with available AI styles
     */
    @GET("templates/ai/styles")
    Call<JsonObject> getAIStyles(
        @Query("category") String category,
        @Query("model") String model
    );
    
    /**
     * Get AI style details
     * @param styleId Style ID
     * @return Response with style details and parameters
     */
    @GET("templates/ai/styles/{styleId}")
    Call<JsonObject> getAIStyleDetails(@Path("styleId") String styleId);
    
    /**
     * Get AI generation analytics
     * @param period Analytics period (daily, weekly, monthly)
     * @param model Model filter (optional)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with generation analytics
     */
    @GET("templates/ai/analytics")
    Call<JsonObject> getAIGenerationAnalytics(
        @Query("period") String period,
        @Query("model") String model,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get AI usage statistics
     * @param userId User ID filter (optional)
     * @param period Statistics period
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with usage statistics
     */
    @GET("templates/ai/usage")
    Call<JsonObject> getAIUsageStatistics(
        @Query("userId") String userId,
        @Query("period") String period,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Validate AI prompt
     * @param promptData Prompt validation data (prompt, model, parameters)
     * @return Response with validation results and suggestions
     */
    @POST("templates/ai/prompts/validate")
    Call<JsonObject> validateAIPrompt(@Body Map<String, Object> promptData);
    
    /**
     * Optimize AI prompt
     * @param promptData Prompt optimization data (prompt, target, model)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with optimized prompt
     */
    @POST("templates/ai/prompts/optimize")
    Call<JsonObject> optimizeAIPrompt(
        @Body Map<String, Object> promptData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get AI generation cost estimate
     * @param estimateData Estimation data (prompt, model, parameters)
     * @return Response with cost estimate
     */
    @POST("templates/ai/estimate")
    Call<JsonObject> getAIGenerationCostEstimate(@Body Map<String, Object> estimateData);
    
    /**
     * Bulk update AI metadata
     * @param bulkData Bulk AI metadata update data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with update results
     */
    @POST("templates/ai/bulk-update")
    Call<JsonObject> bulkUpdateAIMetadata(
        @Body Map<String, Object> bulkData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Export AI generation data
     * @param format Export format (json, csv, excel)
     * @param period Export period
     * @param filters Export filters (model, style, user)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with exported data
     */
    @GET("templates/ai/export")
    Call<JsonObject> exportAIGenerationData(
        @Query("format") String format,
        @Query("period") String period,
        @Query("filters") String filters,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get AI feature availability
     * @param userId User ID to check features for
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with available AI features
     */
    @GET("templates/ai/features")
    Call<JsonObject> getAIFeatureAvailability(
        @Query("userId") String userId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    // =============================================================================
    // TEMPLATE CATEGORIZATION API - Phase 2 Implementation (20 key endpoints)
    // =============================================================================
    
    /**
     * Get template categories
     * @param includeCount Include template count per category
     * @param activeOnly Only active categories
     * @return Response with categories list
     */
    @GET("templates/categories")
    Call<JsonObject> getTemplateCategories(
        @Query("includeCount") boolean includeCount,
        @Query("activeOnly") boolean activeOnly
    );
    
    /**
     * Update template tags
     * @param templateId Template ID
     * @param tagsData Tags data (tags array, operation: add/remove/replace)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated tags
     */
    @PUT("templates/{templateId}/tags")
    Call<JsonObject> updateTemplateTags(
        @Path("templateId") String templateId,
        @Body Map<String, Object> tagsData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get templates by tag
     * @param tag Tag name
     * @param page Page number for pagination
     * @param limit Items per page
     * @param sortBy Sort field
     * @param sortOrder Sort order
     * @return Response with tagged templates
     */
    @GET("templates/tags/{tag}")
    Call<JsonObject> getTemplatesByTag(
        @Path("tag") String tag,
        @Query("page") int page,
        @Query("limit") int limit,
        @Query("sortBy") String sortBy,
        @Query("sortOrder") String sortOrder
    );
    
    /**
     * Get popular tags
     * @param limit Number of tags to return
     * @param period Time period for popularity
     * @param category Category filter (optional)
     * @return Response with popular tags
     */
    @GET("templates/tags/popular")
    Call<JsonObject> getPopularTags(
        @Query("limit") int limit,
        @Query("period") String period,
        @Query("category") String category
    );
    
    /**
     * Search tags
     * @param query Search query
     * @param limit Number of results
     * @param category Category filter (optional)
     * @return Response with matching tags
     */
    @GET("templates/tags/search")
    Call<JsonObject> searchTags(
        @Query("q") String query,
        @Query("limit") int limit,
        @Query("category") String category
    );
    
    /**
     * Update template festival tag
     * @param templateId Template ID
     * @param festivalData Festival tag data (festivalTag, year, region)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated festival tag
     */
    @PUT("templates/{templateId}/festival-tag")
    Call<JsonObject> updateTemplateFestivalTag(
        @Path("templateId") String templateId,
        @Body Map<String, Object> festivalData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get templates by festival
     * @param festival Festival name
     * @param year Festival year (optional)
     * @param page Page number
     * @param limit Items per page
     * @return Response with festival templates
     */
    @GET("templates/festivals/{festival}")
    Call<JsonObject> getTemplatesByFestival(
        @Path("festival") String festival,
        @Query("year") String year,
        @Query("page") int page,
        @Query("limit") int limit
    );
    
    /**
     * Update template search keywords
     * @param templateId Template ID
     * @param keywordsData Keywords data (keywords array, operation)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated keywords
     */
    @PUT("templates/{templateId}/keywords")
    Call<JsonObject> updateTemplateSearchKeywords(
        @Path("templateId") String templateId,
        @Body Map<String, Object> keywordsData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Search templates by keywords
     * @param keywords Search keywords
     * @param page Page number
     * @param limit Items per page
     * @param filters Additional filters
     * @return Response with search results
     */
    @GET("templates/search/keywords")
    Call<JsonObject> searchTemplatesByKeywords(
        @Query("keywords") String keywords,
        @Query("page") int page,
        @Query("limit") int limit,
        @Query("filters") String filters
    );
    
    /**
     * Set template as variation of another
     * @param templateId Template ID
     * @param variationData Variation data (originalTemplateId, variationType)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with variation relationship
     */
    @PUT("templates/{templateId}/variation-of")
    Call<JsonObject> setTemplateVariation(
        @Path("templateId") String templateId,
        @Body Map<String, Object> variationData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template variations
     * @param templateId Template ID
     * @param includeParent Include parent template info
     * @return Response with template variations
     */
    @GET("templates/{templateId}/variations")
    Call<JsonObject> getTemplateVariations(
        @Path("templateId") String templateId,
        @Query("includeParent") boolean includeParent
    );
    
    /**
     * Update related templates
     * @param templateId Template ID
     * @param relatedData Related templates data (templateIds, relationshipType)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated relationships
     */
    @PUT("templates/{templateId}/related")
    Call<JsonObject> updateRelatedTemplates(
        @Path("templateId") String templateId,
        @Body Map<String, Object> relatedData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get related templates
     * @param templateId Template ID
     * @param relationshipType Filter by relationship type (optional)
     * @param limit Number of related templates
     * @return Response with related templates
     */
    @GET("templates/{templateId}/related")
    Call<JsonObject> getRelatedTemplates(
        @Path("templateId") String templateId,
        @Query("type") String relationshipType,
        @Query("limit") int limit
    );
    
    /**
     * Get similar templates based on categorization
     * @param templateId Template ID
     * @param algorithm Similarity algorithm (tags, category, ai, hybrid)
     * @param limit Number of similar templates
     * @return Response with similar templates
     */
    @GET("templates/{templateId}/similar")
    Call<JsonObject> getSimilarTemplates(
        @Path("templateId") String templateId,
        @Query("algorithm") String algorithm,
        @Query("limit") int limit
    );
    
    /**
     * Bulk update template categorization
     * @param bulkData Bulk categorization update data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with update results
     */
    @POST("templates/categorization/bulk-update")
    Call<JsonObject> bulkUpdateCategorization(
        @Body Map<String, Object> bulkData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Auto-categorize template
     * @param templateId Template ID
     * @param algorithm Categorization algorithm (content, ai, hybrid)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with suggested categorization
     */
    @POST("templates/{templateId}/auto-categorize")
    Call<JsonObject> autoCategorizeTemplate(
        @Path("templateId") String templateId,
        @Query("algorithm") String algorithm,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get categorization suggestions
     * @param templateId Template ID
     * @param includeConfidence Include confidence scores
     * @return Response with categorization suggestions
     */
    @GET("templates/{templateId}/categorization/suggestions")
    Call<JsonObject> getCategorizationSuggestions(
        @Path("templateId") String templateId,
        @Query("includeConfidence") boolean includeConfidence
    );
    
    /**
     * Get category trends
     * @param period Trend period (daily, weekly, monthly)
     * @param limit Number of trending categories
     * @return Response with category trends
     */
    @GET("templates/categories/trends")
    Call<JsonObject> getCategoryTrends(
        @Query("period") String period,
        @Query("limit") int limit
    );
    
    /**
     * Get tag trends
     * @param period Trend period
     * @param limit Number of trending tags
     * @param category Category filter (optional)
     * @return Response with tag trends
     */
    @GET("templates/tags/trends")
    Call<JsonObject> getTagTrends(
        @Query("period") String period,
        @Query("limit") int limit,
        @Query("category") String category
    );
    
    /**
     * Get categorization analytics
     * @param period Analytics period
     * @param categoryId Category filter (optional)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with categorization analytics
     */
    @GET("templates/categorization/analytics")
    Call<JsonObject> getCategorizationAnalytics(
        @Query("period") String period,
        @Query("categoryId") String categoryId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    // =============================================================================
    // TEMPLATE VISIBILITY API - Phase 2 Implementation (25 endpoints)
    // =============================================================================
    
    /**
     * Get template visibility score
     * @param templateId Template ID
     * @return Response with visibility score and factors
     */
    @GET("templates/{templateId}/visibility/score")
    Call<JsonObject> getTemplateVisibilityScore(@Path("templateId") String templateId);
    
    /**
     * Update template visibility score
     * @param templateId Template ID
     * @param scoreData Score data (score, factors, algorithm)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated visibility score
     */
    @PUT("templates/{templateId}/visibility/score")
    Call<JsonObject> updateTemplateVisibilityScore(
        @Path("templateId") String templateId,
        @Body Map<String, Object> scoreData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template ranking position
     * @param templateId Template ID
     * @param context Ranking context (category, search, trending, etc.)
     * @param filters Ranking filters (optional)
     * @return Response with ranking position and details
     */
    @GET("templates/{templateId}/visibility/ranking")
    Call<JsonObject> getTemplateRanking(
        @Path("templateId") String templateId,
        @Query("context") String context,
        @Query("filters") String filters
    );
    
    /**
     * Update template ranking factors
     * @param templateId Template ID
     * @param rankingData Ranking factors data (quality, engagement, recency, etc.)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated ranking factors
     */
    @PUT("templates/{templateId}/visibility/ranking")
    Call<JsonObject> updateTemplateRankingFactors(
        @Path("templateId") String templateId,
        @Body Map<String, Object> rankingData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Boost template visibility
     * @param templateId Template ID
     * @param boostData Boost data (priority, duration, target_contexts)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with boost status
     */
    @POST("templates/{templateId}/visibility/boost")
    Call<JsonObject> boostTemplateVisibility(
        @Path("templateId") String templateId,
        @Body Map<String, Object> boostData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update template boost priority
     * @param templateId Template ID
     * @param priorityData Priority data (priority, context, expiration)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated boost priority
     */
    @PUT("templates/{templateId}/visibility/boost-priority")
    Call<JsonObject> updateTemplateBoostPriority(
        @Path("templateId") String templateId,
        @Body Map<String, Object> priorityData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Remove template boost
     * @param templateId Template ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/{templateId}/visibility/boost")
    Call<JsonObject> removeTemplateBoost(
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template boost status
     * @param templateId Template ID
     * @return Response with current boost status and details
     */
    @GET("templates/{templateId}/visibility/boost-status")
    Call<JsonObject> getTemplateBoostStatus(@Path("templateId") String templateId);
    
    /**
     * Get boosted templates
     * @param context Boost context filter (optional)
     * @param priority Priority filter (optional)
     * @param limit Number of templates to return
     * @param page Page number for pagination
     * @return Response with boosted templates
     */
    @GET("templates/visibility/boosted")
    Call<JsonObject> getBoostedTemplates(
        @Query("context") String context,
        @Query("priority") String priority,
        @Query("limit") int limit,
        @Query("page") int page
    );
    
    /**
     * Update template type
     * @param templateId Template ID
     * @param typeData Type data (templateType, subType, classification)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated template type
     */
    @PUT("templates/{templateId}/visibility/type")
    Call<JsonObject> updateTemplateType(
        @Path("templateId") String templateId,
        @Body Map<String, Object> typeData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get templates by type
     * @param templateType Template type
     * @param subType Sub-type filter (optional)
     * @param page Page number for pagination
     * @param limit Items per page
     * @param sortBy Sort field
     * @return Response with templates of specified type
     */
    @GET("templates/visibility/type/{templateType}")
    Call<JsonObject> getTemplatesByType(
        @Path("templateType") String templateType,
        @Query("subType") String subType,
        @Query("page") int page,
        @Query("limit") int limit,
        @Query("sortBy") String sortBy
    );
    
    /**
     * Hide template from user
     * @param templateId Template ID
     * @param userId User ID to hide template from
     * @param hideData Hide reason and settings
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @POST("templates/{templateId}/visibility/hide")
    Call<JsonObject> hideTemplateFromUser(
        @Path("templateId") String templateId,
        @Query("userId") String userId,
        @Body Map<String, Object> hideData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Unhide template for user
     * @param templateId Template ID
     * @param userId User ID to unhide template for
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/{templateId}/visibility/hide")
    Call<JsonObject> unhideTemplateForUser(
        @Path("templateId") String templateId,
        @Query("userId") String userId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Block template globally
     * @param templateId Template ID
     * @param blockData Block reason and settings
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @POST("templates/{templateId}/visibility/block")
    Call<JsonObject> blockTemplate(
        @Path("templateId") String templateId,
        @Body Map<String, Object> blockData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Unblock template
     * @param templateId Template ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/{templateId}/visibility/block")
    Call<JsonObject> unblockTemplate(
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template visibility settings
     * @param templateId Template ID
     * @return Response with visibility settings
     */
    @GET("templates/{templateId}/visibility/settings")
    Call<JsonObject> getTemplateVisibilitySettings(@Path("templateId") String templateId);
    
    /**
     * Update template visibility settings
     * @param templateId Template ID
     * @param settingsData Visibility settings (regions, demographics, time_restrictions)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated settings
     */
    @PUT("templates/{templateId}/visibility/settings")
    Call<JsonObject> updateTemplateVisibilitySettings(
        @Path("templateId") String templateId,
        @Body Map<String, Object> settingsData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template visibility analytics
     * @param templateId Template ID
     * @param period Analytics period
     * @param metrics Specific visibility metrics to include
     * @return Response with visibility analytics
     */
    @GET("templates/{templateId}/visibility/analytics")
    Call<JsonObject> getTemplateVisibilityAnalytics(
        @Path("templateId") String templateId,
        @Query("period") String period,
        @Query("metrics") String metrics
    );
    
    /**
     * Get visibility performance comparison
     * @param templateId Template ID
     * @param compareWith Template IDs to compare with
     * @param metrics Visibility metrics to compare
     * @param period Comparison period
     * @return Response with visibility comparison data
     */
    @GET("templates/{templateId}/visibility/compare")
    Call<JsonObject> getVisibilityPerformanceComparison(
        @Path("templateId") String templateId,
        @Query("compareWith") String compareWith,
        @Query("metrics") String metrics,
        @Query("period") String period
    );
    
    /**
     * Bulk update template visibility
     * @param bulkData Bulk visibility update data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with update results
     */
    @POST("templates/visibility/bulk-update")
    Call<JsonObject> bulkUpdateTemplateVisibility(
        @Body Map<String, Object> bulkData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get visibility ranking factors
     * @param algorithm Ranking algorithm (default, trending, personalized)
     * @return Response with ranking factors and weights
     */
    @GET("templates/visibility/ranking-factors")
    Call<JsonObject> getVisibilityRankingFactors(@Query("algorithm") String algorithm);
    
    /**
     * Update visibility ranking algorithm
     * @param algorithmData Algorithm configuration and weights
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated algorithm settings
     */
    @PUT("templates/visibility/ranking-algorithm")
    Call<JsonObject> updateVisibilityRankingAlgorithm(
        @Body Map<String, Object> algorithmData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get visibility trends
     * @param period Trend period (daily, weekly, monthly)
     * @param metric Visibility metric (score, ranking, boost_usage)
     * @param limit Number of trending items
     * @return Response with visibility trends
     */
    @GET("templates/visibility/trends")
    Call<JsonObject> getVisibilityTrends(
        @Query("period") String period,
        @Query("metric") String metric,
        @Query("limit") int limit
    );
    
    /**
     * Export visibility data
     * @param format Export format (json, csv, excel)
     * @param period Export period
     * @param templateIds Template IDs to export (optional)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with exported visibility data
     */
    @GET("templates/visibility/export")
    Call<JsonObject> exportVisibilityData(
        @Query("format") String format,
        @Query("period") String period,
        @Query("templateIds") String templateIds,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    // =============================================================================
    // TEMPLATE CUSTOMIZATION API - Phase 2 Implementation (14 endpoints)
    // =============================================================================
    
    /**
     * Get template customization options
     * @param templateId Template ID
     * @return Response with customization options
     */
    @GET("templates/{templateId}/customization")
    Call<JsonObject> getTemplateCustomizationOptions(@Path("templateId") String templateId);
    
    /**
     * Update template customization options
     * @param templateId Template ID
     * @param optionsData Customization options data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated customization options
     */
    @PUT("templates/{templateId}/customization")
    Call<JsonObject> updateTemplateCustomizationOptions(
        @Path("templateId") String templateId,
        @Body Map<String, Object> optionsData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Enable specific customization option
     * @param templateId Template ID
     * @param option Customization option name
     * @param optionData Option configuration data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated option status
     */
    @PUT("templates/{templateId}/customization/{option}/enable")
    Call<JsonObject> enableCustomizationOption(
        @Path("templateId") String templateId,
        @Path("option") String option,
        @Body Map<String, Object> optionData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Disable specific customization option
     * @param templateId Template ID
     * @param option Customization option name
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated option status
     */
    @DELETE("templates/{templateId}/customization/{option}")
    Call<JsonObject> disableCustomizationOption(
        @Path("templateId") String templateId,
        @Path("option") String option,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get customization permissions
     * @param templateId Template ID
     * @param userId User ID to check permissions for
     * @return Response with customization permissions
     */
    @GET("templates/{templateId}/customization/permissions")
    Call<JsonObject> getCustomizationPermissions(
        @Path("templateId") String templateId,
        @Query("userId") String userId
    );
    
    /**
     * Update customization permissions
     * @param templateId Template ID
     * @param permissionsData Permissions data (users, roles, restrictions)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated permissions
     */
    @PUT("templates/{templateId}/customization/permissions")
    Call<JsonObject> updateCustomizationPermissions(
        @Path("templateId") String templateId,
        @Body Map<String, Object> permissionsData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get customization presets
     * @param templateId Template ID
     * @param category Preset category filter (optional)
     * @return Response with available customization presets
     */
    @GET("templates/{templateId}/customization/presets")
    Call<JsonObject> getCustomizationPresets(
        @Path("templateId") String templateId,
        @Query("category") String category
    );
    
    /**
     * Create customization preset
     * @param templateId Template ID
     * @param presetData Preset data (name, settings, category)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with created preset
     */
    @POST("templates/{templateId}/customization/presets")
    Call<JsonObject> createCustomizationPreset(
        @Path("templateId") String templateId,
        @Body Map<String, Object> presetData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Apply customization preset
     * @param templateId Template ID
     * @param presetId Preset ID to apply
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with applied customization
     */
    @POST("templates/{templateId}/customization/presets/{presetId}/apply")
    Call<JsonObject> applyCustomizationPreset(
        @Path("templateId") String templateId,
        @Path("presetId") String presetId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Delete customization preset
     * @param templateId Template ID
     * @param presetId Preset ID to delete
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/{templateId}/customization/presets/{presetId}")
    Call<JsonObject> deleteCustomizationPreset(
        @Path("templateId") String templateId,
        @Path("presetId") String presetId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get customization analytics
     * @param templateId Template ID
     * @param period Analytics period
     * @param option Specific option to analyze (optional)
     * @return Response with customization analytics
     */
    @GET("templates/{templateId}/customization/analytics")
    Call<JsonObject> getCustomizationAnalytics(
        @Path("templateId") String templateId,
        @Query("period") String period,
        @Query("option") String option
    );
    
    /**
     * Bulk update customization options
     * @param bulkData Bulk customization update data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with update results
     */
    @POST("templates/customization/bulk-update")
    Call<JsonObject> bulkUpdateCustomizationOptions(
        @Body Map<String, Object> bulkData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get customization trends
     * @param period Trend period (daily, weekly, monthly)
     * @param option Customization option filter (optional)
     * @param limit Number of trending items
     * @return Response with customization trends
     */
    @GET("templates/customization/trends")
    Call<JsonObject> getCustomizationTrends(
        @Query("period") String period,
        @Query("option") String option,
        @Query("limit") int limit
    );
    
    /**
     * Export customization data
     * @param format Export format (json, csv, excel)
     * @param period Export period
     * @param templateIds Template IDs to export (optional)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with exported customization data
     */
    @GET("templates/customization/export")
    Call<JsonObject> exportCustomizationData(
        @Query("format") String format,
        @Query("period") String period,
        @Query("templateIds") String templateIds,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    // =============================================================================
    // TEMPLATE LEGACY API - Phase 2 Implementation (30 endpoints)
    // =============================================================================
    
    /**
     * Get legacy category icon for template
     * @param templateId Template ID
     * @return Response with legacy category icon data
     */
    @GET("templates/{templateId}/legacy/category-icon")
    Call<JsonObject> getLegacyCategoryIcon(@Path("templateId") String templateId);
    
    /**
     * Update legacy category icon
     * @param templateId Template ID
     * @param iconData Legacy icon data (categoryIcon, iconUrl, iconName)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated legacy icon
     */
    @PUT("templates/{templateId}/legacy/category-icon")
    Call<JsonObject> updateLegacyCategoryIcon(
        @Path("templateId") String templateId,
        @Body Map<String, Object> iconData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template moderation status
     * @param templateId Template ID
     * @return Response with moderation status and history
     */
    @GET("templates/{templateId}/legacy/moderation-status")
    Call<JsonObject> getTemplateModerationStatus(@Path("templateId") String templateId);
    
    /**
     * Update template legacy moderation status
     * @param templateId Template ID
     * @param moderationData Moderation data (status, reason, moderator, timestamp)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated legacy moderation status
     */
    @PUT("templates/{templateId}/legacy/moderation-status")
    Call<JsonObject> updateTemplateLegacyModerationStatus(
        @Path("templateId") String templateId,
        @Body Map<String, Object> moderationData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template language and region settings
     * @param templateId Template ID
     * @return Response with language and region data
     */
    @GET("templates/{templateId}/legacy/language-region")
    Call<JsonObject> getTemplateLanguageRegion(@Path("templateId") String templateId);
    
    /**
     * Update template language and region
     * @param templateId Template ID
     * @param localeData Language and region data (language, region, localization)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated locale settings
     */
    @PUT("templates/{templateId}/legacy/language-region")
    Call<JsonObject> updateTemplateLanguageRegion(
        @Path("templateId") String templateId,
        @Body Map<String, Object> localeData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template experiment tag
     * @param templateId Template ID
     * @return Response with experiment tag and configuration
     */
    @GET("templates/{templateId}/legacy/experiment-tag")
    Call<JsonObject> getTemplateExperimentTag(@Path("templateId") String templateId);
    
    /**
     * Set template experiment tag
     * @param templateId Template ID
     * @param experimentData Experiment data (experimentTag, variant, configuration)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with experiment assignment
     */
    @PUT("templates/{templateId}/legacy/experiment-tag")
    Call<JsonObject> setTemplateExperimentTag(
        @Path("templateId") String templateId,
        @Body Map<String, Object> experimentData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Remove template experiment tag
     * @param templateId Template ID
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/{templateId}/legacy/experiment-tag")
    Call<JsonObject> removeTemplateExperimentTag(
        @Path("templateId") String templateId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get template performance log
     * @param templateId Template ID
     * @param period Log period filter (optional)
     * @param metrics Specific metrics to include (optional)
     * @return Response with performance log data
     */
    @GET("templates/{templateId}/legacy/performance-log")
    Call<JsonObject> getTemplatePerformanceLog(
        @Path("templateId") String templateId,
        @Query("period") String period,
        @Query("metrics") String metrics
    );
    
    /**
     * Add template performance log entry
     * @param templateId Template ID
     * @param logData Performance log data (metrics, timestamp, context)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with logged entry
     */
    @POST("templates/{templateId}/legacy/performance-log")
    Call<JsonObject> addTemplatePerformanceLogEntry(
        @Path("templateId") String templateId,
        @Body Map<String, Object> logData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Clear template performance log
     * @param templateId Template ID
     * @param beforeDate Clear entries before this date (optional)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/{templateId}/legacy/performance-log")
    Call<JsonObject> clearTemplatePerformanceLog(
        @Path("templateId") String templateId,
        @Query("beforeDate") String beforeDate,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get legacy API compatibility info
     * @param version API version to check compatibility for
     * @return Response with compatibility information
     */
    @GET("templates/legacy/compatibility")
    Call<JsonObject> getLegacyAPICompatibility(@Query("version") String version);
    
    /**
     * Get deprecated field mappings
     * @param fromVersion Source API version
     * @param toVersion Target API version
     * @return Response with field mapping information
     */
    @GET("templates/legacy/field-mappings")
    Call<JsonObject> getDeprecatedFieldMappings(
        @Query("fromVersion") String fromVersion,
        @Query("toVersion") String toVersion
    );
    
    /**
     * Convert legacy template format
     * @param templateId Template ID
     * @param conversionData Conversion parameters (targetFormat, preserveMetadata)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with converted template data
     */
    @POST("templates/{templateId}/legacy/convert")
    Call<JsonObject> convertLegacyTemplateFormat(
        @Path("templateId") String templateId,
        @Body Map<String, Object> conversionData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Migrate template to new format
     * @param templateId Template ID
     * @param migrationData Migration parameters (targetVersion, preserveLegacy)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with migration status
     */
    @POST("templates/{templateId}/legacy/migrate")
    Call<JsonObject> migrateTemplateFormat(
        @Path("templateId") String templateId,
        @Body Map<String, Object> migrationData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get migration status
     * @param templateId Template ID
     * @return Response with migration status and progress
     */
    @GET("templates/{templateId}/legacy/migration-status")
    Call<JsonObject> getTemplateMigrationStatus(@Path("templateId") String templateId);
    
    /**
     * Rollback template migration
     * @param templateId Template ID
     * @param rollbackData Rollback parameters (targetVersion, preserveChanges)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with rollback status
     */
    @POST("templates/{templateId}/legacy/rollback")
    Call<JsonObject> rollbackTemplateMigration(
        @Path("templateId") String templateId,
        @Body Map<String, Object> rollbackData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get legacy experiment results
     * @param experimentTag Experiment tag filter
     * @param period Results period
     * @param variant Experiment variant filter (optional)
     * @return Response with experiment results
     */
    @GET("templates/legacy/experiments/{experimentTag}/results")
    Call<JsonObject> getLegacyExperimentResults(
        @Path("experimentTag") String experimentTag,
        @Query("period") String period,
        @Query("variant") String variant
    );
    
    /**
     * Get active legacy experiments
     * @param includeExpired Include expired experiments
     * @return Response with active experiments list
     */
    @GET("templates/legacy/experiments")
    Call<JsonObject> getActiveLegacyExperiments(@Query("includeExpired") boolean includeExpired);
    
    /**
     * Create legacy experiment
     * @param experimentData Experiment configuration (name, variants, allocation)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with created experiment
     */
    @POST("templates/legacy/experiments")
    Call<JsonObject> createLegacyExperiment(
        @Body Map<String, Object> experimentData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update legacy experiment
     * @param experimentTag Experiment tag
     * @param experimentData Updated experiment configuration
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated experiment
     */
    @PUT("templates/legacy/experiments/{experimentTag}")
    Call<JsonObject> updateLegacyExperiment(
        @Path("experimentTag") String experimentTag,
        @Body Map<String, Object> experimentData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * End legacy experiment
     * @param experimentTag Experiment tag
     * @param endData End experiment data (reason, preserveData)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/legacy/experiments/{experimentTag}")
    Call<JsonObject> endLegacyExperiment(
        @Path("experimentTag") String experimentTag,
        @Body Map<String, Object> endData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get legacy reference mappings
     * @param referenceType Type of reference (template, category, user)
     * @param legacyId Legacy identifier
     * @return Response with current reference mapping
     */
    @GET("templates/legacy/references/{referenceType}/{legacyId}")
    Call<JsonObject> getLegacyReferenceMapping(
        @Path("referenceType") String referenceType,
        @Path("legacyId") String legacyId
    );
    
    /**
     * Create legacy reference mapping
     * @param mappingData Reference mapping data (legacyId, currentId, type)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with created mapping
     */
    @POST("templates/legacy/references")
    Call<JsonObject> createLegacyReferenceMapping(
        @Body Map<String, Object> mappingData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Update legacy reference mapping
     * @param referenceType Type of reference
     * @param legacyId Legacy identifier
     * @param mappingData Updated mapping data
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with updated mapping
     */
    @PUT("templates/legacy/references/{referenceType}/{legacyId}")
    Call<JsonObject> updateLegacyReferenceMapping(
        @Path("referenceType") String referenceType,
        @Path("legacyId") String legacyId,
        @Body Map<String, Object> mappingData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Delete legacy reference mapping
     * @param referenceType Type of reference
     * @param legacyId Legacy identifier
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response indicating success or failure
     */
    @DELETE("templates/legacy/references/{referenceType}/{legacyId}")
    Call<JsonObject> deleteLegacyReferenceMapping(
        @Path("referenceType") String referenceType,
        @Path("legacyId") String legacyId,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Bulk migrate legacy references
     * @param migrationData Bulk migration data (mappings, validation, dryRun)
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with migration results
     */
    @POST("templates/legacy/references/bulk-migrate")
    Call<JsonObject> bulkMigrateLegacyReferences(
        @Body Map<String, Object> migrationData,
        @retrofit2.http.Header("Authorization") String authToken
    );
    
    /**
     * Get legacy system health check
     * @param includeMetrics Include performance metrics
     * @param checkReferences Check reference integrity
     * @return Response with legacy system health status
     */
    @GET("templates/legacy/health")
    Call<JsonObject> getLegacySystemHealth(
        @Query("includeMetrics") boolean includeMetrics,
        @Query("checkReferences") boolean checkReferences
    );
    
    /**
     * Export legacy data
     * @param format Export format (json, csv, xml)
     * @param dataType Type of legacy data to export
     * @param period Export period
     * @param authToken Firebase authentication token (for Authorization header)
     * @return Response with exported legacy data
     */
    @GET("templates/legacy/export")
    Call<JsonObject> exportLegacyData(
        @Query("format") String format,
        @Query("dataType") String dataType,
        @Query("period") String period,
        @retrofit2.http.Header("Authorization") String authToken
    );
}
