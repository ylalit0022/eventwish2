package com.ds.eventwish.template;

import com.ds.eventwish.data.converter.StringListConverter;
import com.ds.eventwish.data.converter.DateConverter;
import com.ds.eventwish.data.model.Template;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Basic test suite for Template API implementation
 * Tests Template model, converters, and basic functionality
 */
@RunWith(MockitoJUnitRunner.class)
public class TemplateBasicTest {
    private static final String TAG = "TemplateBasicTest";
    
    @Test
    public void testTemplateBasicFields() {
        System.out.println(TAG + ": Testing Template basic fields...");
        
        Template template = new Template();
        template.setId("test_123");
        template.setTitle("Test Template");
        template.setCategoryId("birthday");
        
        assertEquals("ID should be set correctly", "test_123", template.getId());
        assertEquals("Title should be set correctly", "Test Template", template.getTitle());
        assertEquals("Category should be set correctly", "birthday", template.getCategoryId());
        
        System.out.println(TAG + ": Template basic fields test passed");
    }
    
    @Test
    public void testTemplateMonetizationFields() {
        System.out.println(TAG + ": Testing Template monetization fields...");
        
        Template template = new Template();
        template.setIsPremium(true);
        template.setIsFeatured(true);
        template.setIsTrending(true);
        template.setPrice(9.99);
        
        assertTrue("Premium status should be true", template.isPremium());
        assertTrue("Featured status should be true", template.isFeatured());
        assertTrue("Trending status should be true", template.isTrending());
        assertEquals("Price should be set correctly", 9.99, template.getPrice(), 0.01);
        
        System.out.println(TAG + ": Template monetization fields test passed");
    }
    
    @Test
    public void testTemplateAIFields() {
        System.out.println(TAG + ": Testing Template AI fields...");
        
        Template template = new Template();
        template.setIsAIGenerated(true);
        template.setAiPrompt("Test prompt");
        template.setAiModel("gpt-4");
        
        assertTrue("AI generated flag should be true", template.isAIGenerated());
        assertEquals("AI prompt should be set correctly", "Test prompt", template.getAiPrompt());
        assertEquals("AI model should be set correctly", "gpt-4", template.getAiModel());
        
        System.out.println(TAG + ": Template AI fields test passed");
    }
    
    @Test
    public void testTemplateEngagementFields() {
        System.out.println(TAG + ": Testing Template engagement fields...");
        
        Template template = new Template();
        template.setUsageCount(100);
        template.setViewCount(1000);
        template.setLikeCount(50);
        template.setRating(4.5);
        
        assertEquals("Usage count should be set correctly", 100, template.getUsageCount());
        assertEquals("View count should be set correctly", 1000, template.getViewCount());
        assertEquals("Like count should be set correctly", 50, template.getLikeCount());
        assertEquals("Rating should be set correctly", 4.5, template.getRating(), 0.01);
        
        System.out.println(TAG + ": Template engagement fields test passed");
    }
    
    @Test
    public void testStringListConverter() {
        System.out.println(TAG + ": Testing StringListConverter...");
        
        List<String> testList = Arrays.asList("tag1", "tag2", "tag3");
        String json = StringListConverter.fromStringList(testList);
        List<String> result = StringListConverter.toStringList(json);
        
        assertNotNull("Result should not be null", result);
        assertEquals("List size should be preserved", testList.size(), result.size());
        assertTrue("Should contain tag1", result.contains("tag1"));
        assertTrue("Should contain tag2", result.contains("tag2"));
        assertTrue("Should contain tag3", result.contains("tag3"));
        
        System.out.println(TAG + ": StringListConverter test passed");
    }
    
    @Test
    public void testDateConverter() {
        System.out.println(TAG + ": Testing DateConverter...");
        
        Date testDate = new Date();
        Long timestamp = DateConverter.dateToTimestamp(testDate);
        Date result = DateConverter.fromTimestamp(timestamp);
        
        assertNotNull("Result should not be null", result);
        assertEquals("Date should be preserved", testDate.getTime(), result.getTime());
        
        System.out.println(TAG + ": DateConverter test passed");
    }
    
    @Test
    public void testNullHandling() {
        System.out.println(TAG + ": Testing null handling...");
        
        // Test StringListConverter with null
        assertNull("Null list should return null", StringListConverter.fromStringList(null));
        assertNull("Null string should return null", StringListConverter.toStringList(null));
        
        // Test DateConverter with null
        assertNull("Null date should return null", DateConverter.dateToTimestamp(null));
        assertNull("Null timestamp should return null", DateConverter.fromTimestamp(null));
        
        System.out.println(TAG + ": Null handling test passed");
    }
    
    @Test
    public void testTemplateCopyConstructor() {
        System.out.println(TAG + ": Testing Template copy constructor...");
        
        Template original = new Template();
        original.setId("original_123");
        original.setTitle("Original Template");
        original.setIsPremium(true);
        original.setUsageCount(100);
        
        Template copy = new Template(original);
        
        assertEquals("ID should be copied", original.getId(), copy.getId());
        assertEquals("Title should be copied", original.getTitle(), copy.getTitle());
        assertEquals("Premium status should be copied", original.isPremium(), copy.isPremium());
        assertEquals("Usage count should be copied", original.getUsageCount(), copy.getUsageCount());
        
        System.out.println(TAG + ": Template copy constructor test passed");
    }
    
    @Test
    public void testTemplateComputedProperties() {
        System.out.println(TAG + ": Testing Template computed properties...");
        
        Template template = new Template();
        template.setUsageCount(100);
        template.setViewCount(1000);
        template.setLikeCount(50);
        
        double trendingScore = template.getTrendingScore();
        assertTrue("Trending score should be positive", trendingScore >= 0);
        
        System.out.println(TAG + ": Template computed properties test passed");
    }
} 