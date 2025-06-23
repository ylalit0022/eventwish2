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
}
