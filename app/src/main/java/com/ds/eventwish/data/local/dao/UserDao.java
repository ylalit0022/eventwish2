package com.ds.eventwish.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.ds.eventwish.data.local.entity.UserEntity;

/**
 * Data Access Object for User entity
 */
@Dao
public interface UserDao {
    
    /**
     * Insert a new user
     * @param user User to insert
     * @return rowId of inserted item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserEntity user);
    
    /**
     * Update an existing user
     * @param user User to update
     * @return Number of rows updated
     */
    @Update
    int update(UserEntity user);
    
    /**
     * Delete a user
     * @param user User to delete
     */
    @Delete
    void delete(UserEntity user);
    
    /**
     * Get user by UID
     * @param uid User's unique ID
     * @return User entity
     */
    @Query("SELECT * FROM users WHERE uid = :uid")
    UserEntity getUserByUid(String uid);
    
    /**
     * Get user by UID as LiveData
     * @param uid User's unique ID
     * @return LiveData of User entity
     */
    @Query("SELECT * FROM users WHERE uid = :uid")
    LiveData<UserEntity> getUserByUidLive(String uid);
    
    /**
     * Get user by phone number
     * @param phoneNumber Phone number
     * @return User entity
     */
    @Query("SELECT * FROM users WHERE phoneNumber = :phoneNumber")
    UserEntity getUserByPhoneNumber(String phoneNumber);
    
    /**
     * Get current authenticated user
     * @return User entity
     */
    @Query("SELECT * FROM users WHERE isAuthenticated = 1 LIMIT 1")
    UserEntity getCurrentUser();
    
    /**
     * Get current authenticated user as LiveData
     * @return LiveData of User entity
     */
    @Query("SELECT * FROM users WHERE isAuthenticated = 1 LIMIT 1")
    LiveData<UserEntity> getCurrentUserLive();
    
    /**
     * Check if a user exists by UID
     * @param uid User's unique ID
     * @return true if user exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE uid = :uid LIMIT 1)")
    boolean exists(String uid);
    
    /**
     * Delete all users
     */
    @Query("DELETE FROM users")
    void deleteAll();
    
    /**
     * Clear authenticated status for all users
     * @return Number of rows updated
     */
    @Query("UPDATE users SET isAuthenticated = 0")
    int clearAuthentication();
    
    /**
     * Set user as authenticated
     * @param uid User's unique ID
     * @param timestamp Current timestamp 
     * @return Number of rows updated
     */
    @Query("UPDATE users SET isAuthenticated = 1, lastLoginTime = :timestamp, updatedAt = :timestamp WHERE uid = :uid")
    int authenticateUser(String uid, long timestamp);

    /**
     * Find user by UID
     * @param uid User's unique ID
     * @return UserEntity if found, null otherwise
     */
    @Query("SELECT * FROM users WHERE uid = :uid")
    UserEntity findByUid(String uid);

    /**
     * Delete user by UID
     * @param uid User's unique ID
     */
    @Query("DELETE FROM users WHERE uid = :uid")
    void deleteByUid(String uid);

    /**
     * Check if user exists by UID
     * @param uid User's unique ID
     * @return True if user exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE uid = :uid LIMIT 1)")
    boolean existsByUid(String uid);
} 