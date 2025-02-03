package com.ds.eventwish.data.remote;

import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.response.TemplateResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

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

    @GET("share/{shortCode}")
    Call<SharedWish> getSharedWish(@Path("shortCode") String shortCode);

    @GET("wishes/my")
    Call<List<SharedWish>> getMyWishes();
}
