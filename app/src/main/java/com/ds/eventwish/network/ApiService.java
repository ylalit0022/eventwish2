package com.ds.eventwish.network;

import com.ds.eventwish.data.local.entity.AdMobEntity;
import com.google.gson.JsonObject;
import com.ds.eventwish.data.model.Festival;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.model.Template;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;
import java.util.Map;

/**
 * API service interface for Retrofit
 */
public interface ApiService {
    
    // Template endpoints
    @GET("templates")
    Call<List<Template>> getTemplates();
    
    @GET("templates/{id}")
    Call<Template> getTemplateById(@Path("id") String id);
    
    // Festival endpoints
    @GET("festivals")
    Call<List<Festival>> getFestivals();
    
    // Shared wish endpoints
    @POST("wishes/create")
    Call<SharedWish> createSharedWish(@Body Map<String, Object> requestBody);
    
    @GET("wishes/{shortCode}")
    Call<SharedWish> getSharedWishByShortCode(@Path("shortCode") String shortCode);
    
    @GET("wishes/shared/{shortCode}")
    Call<JsonObject> getSharedWishJsonByShortCode(@Path("shortCode") String shortCode);
    
    // AdMob endpoints for client
    @GET("admob")
    Call<List<Map<String, Object>>> getAdMobConfig();
    
    @GET("admob/byType/{adType}")
    Call<List<Map<String, Object>>> getAdMobByType(@Path("adType") String adType);
    
    /**
     * Get all active AdMob units
     * @return List of AdMob entities
     */
    @GET("admob/units")
    Call<List<AdMobEntity>> getAdMobData();
    
    // Time and security endpoints
    @GET("test/time")
    Call<JSONObject> getServerTime();
    
    // Plan configuration endpoint
    @GET("coins/plan")
    Call<JsonObject> getPlanConfiguration();
    
    /**
     * Get coins for a device
     * @param deviceId Device ID
     * @return Coins data
     */
    @GET("coins/{deviceId}")
    Call<JsonObject> getCoins(@Path("deviceId") String deviceId);
    
    /**
     * Add coins for watching a rewarded ad
     * @param deviceId Device ID
     * @param requestBody Body containing ad unit ID
     * @return Updated coins data
     */
    @POST("coins/{deviceId}")
    Call<JsonObject> addCoins(@Path("deviceId") String deviceId, @Body Map<String, Object> requestBody);
    
    /**
     * Unlock HTML editing feature
     * @param deviceId Device ID
     * @return Unlock result
     */
    @POST("coins/{deviceId}/unlock")
    Call<JsonObject> unlockFeature(@Path("deviceId") String deviceId);
    
    // Unlock validation endpoints
    @POST("coins/validate")
    Call<JsonObject> validateUnlock(@Body Map<String, Object> requestBody);
    
    @POST("coins/report")
    Call<JsonObject> reportUnlock(@Body Map<String, Object> requestBody);
    
    // Track ad rewards
    @POST("coins/reward")
    Call<JSONObject> trackAdReward(@Body JSONObject payload);
} 