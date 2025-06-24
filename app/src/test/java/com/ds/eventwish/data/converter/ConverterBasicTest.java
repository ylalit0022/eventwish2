package com.ds.eventwish.data.converter;

import com.ds.eventwish.data.converter.StringListConverter;
import com.ds.eventwish.data.converter.DateConverter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Basic test suite for Template Type Converters
 */
@RunWith(MockitoJUnitRunner.class)
public class ConverterBasicTest {
    private static final String TAG = "ConverterBasicTest";
    
    @Test
    public void testStringListConverter() {
        System.out.println(TAG + ": Testing StringListConverter...");
        
        List<String> testList = Arrays.asList("tag1", "tag2", "tag3");
        String json = StringListConverter.fromStringList(testList);
        List<String> result = StringListConverter.toStringList(json);
        
        assertNotNull("Result should not be null", result);
        assertEquals("List size should be preserved", testList.size(), result.size());
        
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
} 