package com.ds.eventwish.data.remote;

import com.ds.eventwish.data.local.entity.AdMobEntity;
import com.ds.eventwish.data.model.CategoryIcon;
import com.ds.eventwish.data.model.Festival;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.response.BaseResponse;
import com.ds.eventwish.data.model.response.CategoryIconResponse;
import com.ds.eventwish.data.model.response.TemplateResponse;
import com.ds.eventwish.data.model.response.WishResponse;
import com.ds.eventwish.data.model.ServerTimeResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;

public interface ApiService {
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

    @GET("wishes/my")
    Call<List<SharedWish>> getMyWishes();

    @DELETE("wishes/clear")
    Call<Void> clearHistory();
    
    // Festival endpoints
    // The backend only has /api/festivals/upcoming endpoint
    @GET("festivals/upcoming")
    Call<List<Festival>> getAllFestivals();
    
    // Using the same endpoint for upcoming festivals
    @GET("festivals/upcoming")
    Call<List<Festival>> getUpcomingFestivals();
    
    @GET("festivals/category/{category}")
    Call<List<Festival>> getFestivalsByCategory(@Path("category") String category);
    
    @GET("festivals/{id}")
    Call<Festival> getFestivalById(@Path("id") String id);

    @GET("categoryIcons")
    Call<CategoryIconResponse> getCategoryIcons();

    @GET("server/time")
    Call<ServerTimeResponse> getServerTime();

    @GET("festivals/upcoming")
    Call<List<Festival>> getFestivals();

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
    Call<List<JsonObject>> getTemplates(@HeaderMap Map<String, String> headers);
    
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

    // AdMob endpoints for client
    @GET("admob/units")
    Call<List<AdMobEntity>> getAdMobData();

    @GET("admob/byType/{adType}")
    Call<List<Map<String, Object>>> getAdMobByType(@Path("adType") String adType);

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

    // Rename for clarity and avoid duplication
    @POST("coins/unlock/report")
    Call<JsonObject> reportUnlockStatus(@Body Map<String, Object> payload);

    // Track ad rewards
    @POST("coins/reward")
    Call<JsonObject> trackAdReward(@Body JsonObject payload);

    /**
     * Report security violation
     * @param payload Security violation details
     * @return Response
     */
    @POST("security/violation")
    Call<JsonObject> reportSecurityViolation(@Body Map<String, Object> payload);

    // Old authentication methods (deprecated, use the Object versions below)
    @POST("coins/register")  // Change this from auth/register
    Call<JsonObject> registerNewUser(@Body Map<String, Object> payload);
    
    @GET("auth/validate")
    Call<JsonObject> validateAppSignature(@HeaderMap Map<String, String> headers);
    
    /**
     * Change user password
     */
    @POST("auth/change-password")
    Call<JsonObject> changePassword(@Body Map<String, String> passwordRequest);

    /**
     * Get the OkHttpClient for this service
     * @return OkHttpClient instance
     */
    OkHttpClient getClient();

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
    
    // Authentication endpoints
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
}
