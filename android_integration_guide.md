
# Android Integration Testing Guide

## 1. Load Test Data in Android

Add this method to your test class or adapter:

```java
private List<Template> loadTestTemplates() {
    // Load from android_test_templates.json or create manually
    List<Template> testTemplates = new ArrayList<>();
    
    // Test Case 1: Complete creator info
    Template template1 = new Template();
    template1.setId("test_template_001");
    template1.setTitle("Happy Birthday Celebration");
    template1.setCreatorId("user_12345");
    template1.setCreatorName("Sarah Johnson");
    template1.setCreatorProfilePhoto("https://i.pravatar.cc/150?img=1");
    template1.setGeneratedByUser(null);
    testTemplates.add(template1);
    
    // Test Case 2: GeneratedByUser fallback
    Template template2 = new Template();
    template2.setId("test_template_002");
    template2.setTitle("Wedding Anniversary Wishes");
    template2.setCreatorId(null);
    template2.setCreatorName(null);
    template2.setCreatorProfilePhoto(null);
    template2.setGeneratedByUser("Mike Chen");
    testTemplates.add(template2);
    
    // Test Case 3: Default fallback
    Template template3 = new Template();
    template3.setId("test_template_003");
    template3.setTitle("Good Morning Greetings");
    template3.setCreatorId(null);
    template3.setCreatorName(null);
    template3.setCreatorProfilePhoto(null);
    template3.setGeneratedByUser(null);
    testTemplates.add(template3);
    
    return testTemplates;
}
```

## 2. Test in HomeFragment

```java
// In HomeFragment.java
private void testCreatorProfiles() {
    List<Template> testTemplates = loadTestTemplates();
    recommendedTemplateAdapter.updateTemplates(testTemplates);
    
    Log.d("CreatorProfileTest", "Loaded " + testTemplates.size() + " test templates");
}
```

## 3. Expected Results

- Template 1: Shows "Sarah Johnson" with profile photo
- Template 2: Shows "Mike Chen" with app logo
- Template 3: Shows "eventwish" with app logo

## 4. Verify in Logs

Look for these log messages:
- "Template [ID] creator info - Name: [name], Photo: [url]"
- "Setting username to: [name]"
- "Loading creator profile image: [url]" or "Using fallback app logo"

## 5. Visual Verification

Check that:
- Profile pictures are circular
- Names display correctly
- Fallbacks work when data is missing
- Images load properly or show app logo on failure
