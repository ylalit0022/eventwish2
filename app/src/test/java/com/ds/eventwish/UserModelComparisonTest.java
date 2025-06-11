package com.ds.eventwish;

import com.ds.eventwish.data.model.User;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * Test class to verify Android User model compatibility with backend User model
 */
public class UserModelComparisonTest {

    /**
     * Verify that the Android User model contains all essential fields needed for MongoDB sync
     */
    @Test
    public void testAndroidUserModelContainsRequiredFields() {
        // Essential fields for MongoDB sync
        Set<String> requiredFields = new HashSet<>(Arrays.asList(
            "uid",
            "deviceId",
            "displayName",
            "email",
            "profilePhoto",
            "lastActive"
        ));
        
        // Get all fields from User model
        Field[] fields = User.class.getDeclaredFields();
        Set<String> actualFields = new HashSet<>();
        
        for (Field field : fields) {
            actualFields.add(field.getName());
        }
        
        // Verify all required fields exist
        for (String requiredField : requiredFields) {
            assertTrue("User model is missing required field: " + requiredField, 
                actualFields.contains(requiredField));
        }
    }
    
    /**
     * Verify that User model contains social interaction fields (likes, favorites)
     */
    @Test
    public void testUserModelContainsSocialInteractionFields() {
        // Social interaction fields
        Set<String> socialFields = new HashSet<>(Arrays.asList(
            "favorites",
            "likes"
        ));
        
        // Get all fields from User model
        Field[] fields = User.class.getDeclaredFields();
        Set<String> actualFields = new HashSet<>();
        
        for (Field field : fields) {
            actualFields.add(field.getName());
        }
        
        // Verify social fields exist
        for (String socialField : socialFields) {
            assertTrue("User model is missing social interaction field: " + socialField, 
                actualFields.contains(socialField));
        }
    }
    
    /**
     * Verify that methods exist for social interactions
     */
    @Test
    public void testUserModelContainsSocialInteractionMethods() {
        // Essential methods for social interactions
        Set<String> requiredMethods = new HashSet<>(Arrays.asList(
            "addFavorite",
            "removeFavorite",
            "isFavorite",
            "addLike",
            "removeLike",
            "isLiked"
        ));
        
        // Get all methods from User class
        java.lang.reflect.Method[] methods = User.class.getDeclaredMethods();
        Set<String> actualMethods = new HashSet<>();
        
        for (java.lang.reflect.Method method : methods) {
            actualMethods.add(method.getName());
        }
        
        // Verify all required methods exist
        for (String requiredMethod : requiredMethods) {
            assertTrue("User model is missing required method: " + requiredMethod, 
                actualMethods.contains(requiredMethod));
        }
    }
} 