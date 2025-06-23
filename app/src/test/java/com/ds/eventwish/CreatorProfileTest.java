package com.ds.eventwish;

import com.ds.eventwish.data.model.Template;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Unit tests for Template Creator Profile Display functionality
 * 
 * Tests the logic implemented in RecommendedTemplateAdapter.setCreatorProfile()
 * to ensure proper fallback behavior and creator information display.
 */
public class CreatorProfileTest {
    
    private List<Template> testTemplates;
    
    @Before
    public void setUp() {
        testTemplates = createTestTemplates();
    }
    
    /**
     * Create test templates with various creator information scenarios
     */
    private List<Template> createTestTemplates() {
        List<Template> templates = new ArrayList<>();
        
        // Test Case 1: Complete creator information
        Template template1 = new Template("test_template_001", "Happy Birthday Celebration", "Birthday", "https://example.com/birthday.jpg");
        template1.setLikeCount(25);
        template1.setFavoriteCount(12);
        template1.setShareCount(8);
        template1.setCreatedAt(new Date());
        // Complete creator info
        template1.setCreatorId("user_12345");
        template1.setCreatorName("Sarah Johnson");
        template1.setCreatorProfilePhoto("https://i.pravatar.cc/150?img=1");
        template1.setGeneratedByUser(null);
        templates.add(template1);
        
        // Test Case 2: Only generatedByUser (fallback scenario)
        Template template2 = new Template("test_template_002", "Wedding Anniversary Wishes", "Anniversary", "https://example.com/anniversary.jpg");
        template2.setLikeCount(45);
        template2.setFavoriteCount(23);
        template2.setShareCount(15);
        template2.setCreatedAt(new Date());
        // Fallback only
        template2.setCreatorId(null);
        template2.setCreatorName(null);
        template2.setCreatorProfilePhoto(null);
        template2.setGeneratedByUser("Mike Chen");
        templates.add(template2);
        
        // Test Case 3: No creator information (default fallback)
        Template template3 = new Template("test_template_003", "Good Morning Greetings", "Good Morning", "https://example.com/morning.jpg");
        template3.setLikeCount(67);
        template3.setFavoriteCount(34);
        template3.setShareCount(22);
        template3.setCreatedAt(new Date());
        // No creator info
        template3.setCreatorId(null);
        template3.setCreatorName(null);
        template3.setCreatorProfilePhoto(null);
        template3.setGeneratedByUser(null);
        templates.add(template3);
        
        // Test Case 4: Empty strings (edge case)
        Template template4 = new Template("test_template_004", "Thank You Card", "Thank You", "https://example.com/thankyou.jpg");
        template4.setLikeCount(18);
        template4.setFavoriteCount(9);
        template4.setShareCount(5);
        template4.setCreatedAt(new Date());
        // Empty strings
        template4.setCreatorId("");
        template4.setCreatorName("");
        template4.setCreatorProfilePhoto("");
        template4.setGeneratedByUser("");
        templates.add(template4);
        
        // Test Case 5: Creator with invalid image URL
        Template template5 = new Template("test_template_005", "Get Well Soon", "Get Well", "https://example.com/getwell.jpg");
        template5.setLikeCount(33);
        template5.setFavoriteCount(16);
        template5.setShareCount(11);
        template5.setCreatedAt(new Date());
        // Invalid image URL
        template5.setCreatorId("user_67890");
        template5.setCreatorName("Alex Rodriguez");
        template5.setCreatorProfilePhoto("https://invalid-url.com/nonexistent.jpg");
        template5.setGeneratedByUser(null);
        templates.add(template5);
        
        return templates;
    }
    
    /**
     * Simulate the Android adapter logic for determining display name
     */
    private String getDisplayName(Template template) {
        String creatorName = template.getCreatorName();
        String generatedByUser = template.getGeneratedByUser();
        
        // Priority 1: Use creatorName if available
        if (creatorName != null && !creatorName.trim().isEmpty()) {
            return creatorName.trim();
        }
        // Priority 2: Use generatedByUser if available
        else if (generatedByUser != null && !generatedByUser.trim().isEmpty()) {
            return generatedByUser.trim();
        }
        // Priority 3: Default fallback
        else {
            return "eventwish";
        }
    }
    
    /**
     * Simulate the Android adapter logic for determining image source
     */
    private String getImageSource(Template template) {
        String creatorName = template.getCreatorName();
        String creatorProfilePhoto = template.getCreatorProfilePhoto();
        
        // Only use profile photo if we're using creator name (not fallback)
        if (creatorName != null && !creatorName.trim().isEmpty()) {
            if (creatorProfilePhoto != null && !creatorProfilePhoto.trim().isEmpty()) {
                return "url";
            }
        }
        return "app_logo";
    }
    
    @Test
    public void testCompleteCreatorInfo() {
        Template template = testTemplates.get(0); // Sarah Johnson
        
        String displayName = getDisplayName(template);
        String imageSource = getImageSource(template);
        
        assertEquals("Should display creator name", "Sarah Johnson", displayName);
        assertEquals("Should use profile photo URL", "url", imageSource);
        assertEquals("Creator ID should be preserved", "user_12345", template.getCreatorId());
        assertNotNull("Profile photo URL should not be null", template.getCreatorProfilePhoto());
    }
    
    @Test
    public void testGeneratedByUserFallback() {
        Template template = testTemplates.get(1); // Mike Chen
        
        String displayName = getDisplayName(template);
        String imageSource = getImageSource(template);
        
        assertEquals("Should display generatedByUser name", "Mike Chen", displayName);
        assertEquals("Should use app logo", "app_logo", imageSource);
        assertNull("Creator name should be null", template.getCreatorName());
        assertEquals("GeneratedByUser should be preserved", "Mike Chen", template.getGeneratedByUser());
    }
    
    @Test
    public void testDefaultFallback() {
        Template template = testTemplates.get(2); // No creator info
        
        String displayName = getDisplayName(template);
        String imageSource = getImageSource(template);
        
        assertEquals("Should display app name", "eventwish", displayName);
        assertEquals("Should use app logo", "app_logo", imageSource);
        assertNull("Creator name should be null", template.getCreatorName());
        assertNull("GeneratedByUser should be null", template.getGeneratedByUser());
    }
    
    @Test
    public void testEmptyStringsFallback() {
        Template template = testTemplates.get(3); // Empty strings
        
        String displayName = getDisplayName(template);
        String imageSource = getImageSource(template);
        
        assertEquals("Should display app name for empty strings", "eventwish", displayName);
        assertEquals("Should use app logo for empty strings", "app_logo", imageSource);
        assertEquals("Creator name should be empty string", "", template.getCreatorName());
        assertEquals("GeneratedByUser should be empty string", "", template.getGeneratedByUser());
    }
    
    @Test
    public void testInvalidImageUrl() {
        Template template = testTemplates.get(4); // Alex Rodriguez with invalid URL
        
        String displayName = getDisplayName(template);
        String imageSource = getImageSource(template);
        
        assertEquals("Should display creator name", "Alex Rodriguez", displayName);
        assertEquals("Should attempt to use URL (error handling in Glide)", "url", imageSource);
        assertEquals("Creator ID should be preserved", "user_67890", template.getCreatorId());
        assertTrue("Invalid URL should still be attempted", 
                  template.getCreatorProfilePhoto().contains("invalid-url.com"));
    }
    
    @Test
    public void testAllTemplatesHaveRequiredFields() {
        for (Template template : testTemplates) {
            assertNotNull("Template ID should not be null", template.getId());
            assertNotNull("Template title should not be null", template.getTitle());
            assertNotNull("Template category should not be null", template.getCategory());
            assertNotNull("Template preview URL should not be null", template.getPreviewUrl());
            assertNotNull("Template created date should not be null", template.getCreatedAt());
            
            // Creator fields can be null, but getters should not throw exceptions
            try {
                template.getCreatorId();
                template.getCreatorName();
                template.getCreatorProfilePhoto();
                template.getGeneratedByUser();
            } catch (Exception e) {
                fail("Creator field getters should not throw exceptions: " + e.getMessage());
            }
        }
    }
    
    @Test
    public void testCreatorPriorityLogic() {
        // Test that creatorName takes priority over generatedByUser
        Template template = new Template("test_priority", "Priority Test", "Test", "https://example.com/test.jpg");
        template.setCreatorName("Primary Creator");
        template.setGeneratedByUser("Fallback Creator");
        
        String displayName = getDisplayName(template);
        assertEquals("CreatorName should take priority", "Primary Creator", displayName);
        
        // Test that generatedByUser is used when creatorName is null
        template.setCreatorName(null);
        displayName = getDisplayName(template);
        assertEquals("GeneratedByUser should be used as fallback", "Fallback Creator", displayName);
        
        // Test that app name is used when both are null
        template.setGeneratedByUser(null);
        displayName = getDisplayName(template);
        assertEquals("App name should be used as final fallback", "eventwish", displayName);
    }
    
    @Test
    public void testImageUrlLogic() {
        Template template = new Template("test_image", "Image Test", "Test", "https://example.com/test.jpg");
        
        // Test with valid creator name and photo
        template.setCreatorName("Test Creator");
        template.setCreatorProfilePhoto("https://example.com/photo.jpg");
        assertEquals("Should use URL when both name and photo are available", "url", getImageSource(template));
        
        // Test with creator name but no photo
        template.setCreatorProfilePhoto(null);
        assertEquals("Should use app logo when photo is null", "app_logo", getImageSource(template));
        
        // Test with no creator name (even if photo exists)
        template.setCreatorName(null);
        template.setCreatorProfilePhoto("https://example.com/photo.jpg");
        assertEquals("Should use app logo when creator name is null", "app_logo", getImageSource(template));
    }
} 