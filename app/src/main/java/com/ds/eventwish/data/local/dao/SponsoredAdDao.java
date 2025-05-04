package com.ds.eventwish.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.ds.eventwish.data.local.entity.SponsoredAdEntity;

import java.util.List;

/**
 * Room DAO for sponsored ads.
 */
@Dao
public interface SponsoredAdDao {
    
    /**
     * Insert a new sponsored ad into the cache
     * @param ad The sponsored ad entity to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SponsoredAdEntity ad);
    
    /**
     * Insert multiple sponsored ads into the cache
     * @param ads List of sponsored ad entities to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SponsoredAdEntity> ads);
    
    /**
     * Update an existing sponsored ad in the cache
     * @param ad The sponsored ad entity to update
     */
    @Update
    void update(SponsoredAdEntity ad);
    
    /**
     * Delete a sponsored ad from the cache
     * @param ad The sponsored ad entity to delete
     */
    @Delete
    void delete(SponsoredAdEntity ad);
    
    /**
     * Delete all expired sponsored ads from the cache
     * @param currentTime Current timestamp in milliseconds
     * @return Number of deleted ads
     */
    @Query("DELETE FROM sponsored_ads WHERE expiresAt < :currentTime")
    int deleteExpired(long currentTime);
    
    /**
     * Delete all sponsored ads from the cache
     */
    @Query("DELETE FROM sponsored_ads")
    void deleteAll();
    
    /**
     * Get a sponsored ad by ID
     * @param id ID of the sponsored ad
     * @return Matching sponsored ad entity
     */
    @Query("SELECT * FROM sponsored_ads WHERE id = :id")
    SponsoredAdEntity getById(String id);
    
    /**
     * Get all sponsored ads from the cache
     * @return LiveData list of all sponsored ad entities
     */
    @Query("SELECT * FROM sponsored_ads")
    LiveData<List<SponsoredAdEntity>> getAll();
    
    /**
     * Get all non-expired sponsored ads
     * @param currentTime Current timestamp in milliseconds
     * @return List of non-expired sponsored ad entities
     */
    @Query("SELECT * FROM sponsored_ads WHERE expiresAt > :currentTime")
    List<SponsoredAdEntity> getValidAds(long currentTime);
    
    /**
     * Get all valid sponsored ads as LiveData
     * @param currentTime Current timestamp in milliseconds
     * @return LiveData list of non-expired ad entities
     */
    @Query("SELECT * FROM sponsored_ads WHERE expiresAt > :currentTime")
    LiveData<List<SponsoredAdEntity>> observeValidAds(long currentTime);
    
    /**
     * Get all valid sponsored ads for a specific location
     * @param location The location to filter by
     * @param currentTime Current timestamp in milliseconds
     * @return List of sponsored ad entities for the location
     */
    @Query("SELECT * FROM sponsored_ads WHERE location = :location AND expiresAt > :currentTime AND status = 1 ORDER BY priority DESC")
    List<SponsoredAdEntity> getAdsForLocation(String location, long currentTime);
    
    /**
     * Get all valid sponsored ads for a specific location as LiveData
     * @param location The location to filter by
     * @param currentTime Current timestamp in milliseconds
     * @return LiveData list of sponsored ad entities for the location
     */
    @Query("SELECT * FROM sponsored_ads WHERE location = :location AND expiresAt > :currentTime AND status = 1 ORDER BY priority DESC")
    LiveData<List<SponsoredAdEntity>> observeAdsForLocation(String location, long currentTime);
    
    /**
     * Count all sponsored ads in the cache
     * @return Number of sponsored ads
     */
    @Query("SELECT COUNT(*) FROM sponsored_ads")
    int count();
    
    /**
     * Count valid sponsored ads in the cache
     * @param currentTime Current timestamp in milliseconds
     * @return Number of valid sponsored ads
     */
    @Query("SELECT COUNT(*) FROM sponsored_ads WHERE expiresAt > :currentTime")
    int countValid(long currentTime);
    
    /**
     * Check if the cache has valid ads for a location
     * @param location The location to check
     * @param currentTime Current timestamp in milliseconds
     * @return Number of valid ads for the location
     */
    @Query("SELECT COUNT(*) FROM sponsored_ads WHERE location = :location AND expiresAt > :currentTime AND status = 1")
    int countValidForLocation(String location, long currentTime);
    
    /**
     * Check if any cached ads match the given location (case-insensitive)
     * @param location The location to find
     * @param currentTime Current timestamp in milliseconds
     * @return Number of valid ads with that location
     */
    @Query("SELECT COUNT(*) FROM sponsored_ads WHERE location LIKE :location AND expiresAt > :currentTime AND status = 1")
    int countValidForLocationLike(String location, long currentTime);
    
    /**
     * Reset impression and click counts for all ads (for testing)
     */
    @Query("UPDATE sponsored_ads SET impressionCount = 0, clickCount = 0")
    void resetCounts();
    
    /**
     * Transaction to replace all ads with new ones
     * @param ads List of sponsored ad entities
     */
    @Transaction
    default void replaceAll(List<SponsoredAdEntity> ads) {
        deleteAll();
        insertAll(ads);
    }
    
    /**
     * Get active ads by location, excluding specific IDs
     * @param location The location to filter by
     * @param excludeIds List of ad IDs to exclude
     * @param currentTime Current timestamp in milliseconds
     * @return List of sponsored ad entities matching criteria
     */
    @Query("SELECT * FROM sponsored_ads WHERE location = :location AND " +
           "expiresAt > :currentTime AND status = 1 AND " +
           "id NOT IN (:excludeIds) " +
           "ORDER BY priority DESC")
    List<SponsoredAdEntity> getActiveAdsByLocationExcluding(
        String location, 
        List<String> excludeIds, 
        long currentTime
    );
    
    /**
     * Get active ads by location matching status and date validity
     * @param location The location to filter by
     * @param currentTime Current timestamp in milliseconds
     * @return List of valid sponsored ad entities for the location
     */
    @Query("SELECT * FROM sponsored_ads WHERE location = :location AND " +
           "expiresAt > :currentTime AND status = 1 " +
           "ORDER BY priority DESC")
    List<SponsoredAdEntity> getActiveAdsByLocation(
        String location, 
        long currentTime
    );
    
    /**
     * Get ads by location synchronously for preloading purposes
     * @param location The location to filter by
     * @return List of sponsored ad entities for the location
     */
    @Query("SELECT * FROM sponsored_ads WHERE location = :location AND status = 1 ORDER BY priority DESC")
    List<SponsoredAdEntity> getAdsByLocationSync(String location);
    
    /**
     * Get all ads from the database without LiveData wrapper
     * @return List of all sponsored ad entities
     */
    @Query("SELECT * FROM sponsored_ads")
    List<SponsoredAdEntity> getAllAds();

    /**
     * Get a specific ad by ID - direct non-LiveData access
     * @param id ID of the sponsored ad
     * @return Sponsored ad entity or null if not found
     */
    @Query("SELECT * FROM sponsored_ads WHERE id = :id")
    SponsoredAdEntity getAdById(String id);

    /**
     * Get a random ad for testing purposes
     * @return A random sponsored ad entity or null if none exist
     */
    @Query("SELECT * FROM sponsored_ads LIMIT 1")
    SponsoredAdEntity getRandomAd();
} 