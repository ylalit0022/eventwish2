package com.ds.eventwish.data.remote;

import com.ds.eventwish.data.model.CategoryIcon;
import com.ds.eventwish.data.model.Festival;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.model.Template;
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
    Call<SharedWish> createSharedWish(@Body SharedWish sharedWish);

    @GET("wishes/{shortCode}")
    Call<WishResponse> getSharedWish(@Path("shortCode") String shortCode);

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

    @GET("test/time")
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
}
