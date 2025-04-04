package com.ds.eventwish.network;

import com.ds.eventwish.data.model.ServerTimeResponse;
import com.ds.eventwish.data.model.response.BaseResponse;
import com.ds.eventwish.data.model.response.CategoryIconResponse;
import com.ds.eventwish.data.model.response.TemplateResponse;
import com.ds.eventwish.data.model.response.WishResponse;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit interface for API calls
 */
public interface ApiService {
    @GET("templates")
    Call<TemplateResponse> getTemplates(@Query("page") int page, @Query("limit") int limit);

    @GET("templates/category/{category}")
    Call<TemplateResponse> getTemplatesByCategory(@Path("category") String category, @Query("page") int page, @Query("limit") int limit);

    @GET("templates/{id}")
    Call<JsonObject> getTemplateById(@Path("id") String id);

    @GET("categories")
    Call<List<JsonObject>> getCategories();

    @GET("icons")
    Call<CategoryIconResponse> getCategoryIcons();

    @POST("share")
    Call<JsonObject> createSharedWish(@Body Map<String, Object> sharedWish);

    @GET("wishes/{shortCode}")
    Call<BaseResponse<WishResponse>> getSharedWish(@Path("shortCode") String shortCode);

    @GET("wishes/{shortCode}")
    Call<JsonObject> getSharedWishJsonByShortCode(@Path("shortCode") String shortCode);

    @POST("wishes/{shortCode}/share")
    Call<JsonObject> updateSharedWishPlatform(@Path("shortCode") String shortCode, @Body JsonObject platform);

    @GET("server/time")
    Call<ServerTimeResponse> getServerTime();

    // Coins endpoints
    @GET("coins/{deviceId}")
    Call<JsonObject> getCoins(@Path("deviceId") String deviceId);

    @POST("coins/add")
    Call<JsonObject> addCoins(@Body Map<String, Object> requestBody);

    // User and registration
    @POST("device/register")
    Call<JsonObject> registerDevice(@Body JsonObject deviceInfo);

    @POST("auth/validate")
    Call<JsonObject> validateAppSignature(@HeaderMap Map<String, String> headers);

    @POST("auth/register")
    Call<JsonObject> registerUser(@Body Map<String, Object> user);

    @POST("auth/login")
    Call<JsonObject> login(@Body Map<String, Object> credentials);

    @POST("auth/refresh")
    Call<JsonObject> refreshToken(@Body Map<String, Object> refreshToken);

    @POST("security/violation")
    Call<JsonObject> reportSecurity(@Body Map<String, Object> securityData);

    // Festivals
    @GET("festivals/upcoming")
    Call<JsonObject> getUpcomingFestivals();

    @GET("festivals/category/{category}")
    Call<JsonObject> getFestivalsByCategory(@Path("category") String category);
} 