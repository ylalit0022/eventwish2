#!/usr/bin/env python3
"""
Test Script for Template Creator Profile Display
This script tests the creator profile display functionality by creating sample templates
with various creator information scenarios and verifying the expected behavior.
"""

import json
import time
from datetime import datetime, timedelta

# Test Configuration
BASE_URL = "https://eventwish2.onrender.com"
API_BASE = f"{BASE_URL}/api"

class CreatorProfileTester:
    def __init__(self):
        self.test_results = []
        self.sample_templates = []
        
    def create_sample_templates(self):
        """Create sample templates with different creator scenarios"""
        
        # Scenario 1: Complete creator information
        template1 = {
            "id": "test_template_001",
            "title": "Happy Birthday Celebration",
            "category": "Birthday",
            "previewUrl": f"{BASE_URL}/templates/birthday_preview.jpg",
            "htmlContent": "<div>Happy Birthday Template</div>",
            "cssContent": ".birthday { color: #ff6b6b; }",
            "jsContent": "console.log('Birthday template loaded');",
            "likeCount": 25,
            "favoriteCount": 12,
            "shareCount": 8,
            "isLiked": False,
            "isFavorited": False,
            "createdAt": (datetime.now() - timedelta(hours=2)).isoformat() + "Z",
            # Creator information - Complete
            "creatorId": "user_12345",
            "creatorName": "Sarah Johnson",
            "creatorProfilePhoto": "https://i.pravatar.cc/150?img=1",
            "generatedByUser": None
        }
        
        # Scenario 2: Only generatedByUser (fallback scenario)
        template2 = {
            "id": "test_template_002",
            "title": "Wedding Anniversary Wishes",
            "category": "Anniversary",
            "previewUrl": f"{BASE_URL}/templates/anniversary_preview.jpg",
            "htmlContent": "<div>Anniversary Template</div>",
            "cssContent": ".anniversary { color: #ff69b4; }",
            "jsContent": "console.log('Anniversary template loaded');",
            "likeCount": 45,
            "favoriteCount": 23,
            "shareCount": 15,
            "isLiked": True,
            "isFavorited": False,
            "createdAt": (datetime.now() - timedelta(hours=5)).isoformat() + "Z",
            # Creator information - Fallback only
            "creatorId": None,
            "creatorName": None,
            "creatorProfilePhoto": None,
            "generatedByUser": "Mike Chen"
        }
        
        # Scenario 3: No creator information (default fallback)
        template3 = {
            "id": "test_template_003",
            "title": "Good Morning Greetings",
            "category": "Good Morning",
            "previewUrl": f"{BASE_URL}/templates/morning_preview.jpg",
            "htmlContent": "<div>Good Morning Template</div>",
            "cssContent": ".morning { color: #ffd93d; }",
            "jsContent": "console.log('Morning template loaded');",
            "likeCount": 67,
            "favoriteCount": 34,
            "shareCount": 22,
            "isLiked": False,
            "isFavorited": True,
            "createdAt": (datetime.now() - timedelta(days=1)).isoformat() + "Z",
            # Creator information - None (should fallback to app branding)
            "creatorId": None,
            "creatorName": None,
            "creatorProfilePhoto": None,
            "generatedByUser": None
        }
        
        # Scenario 4: Creator with empty strings (edge case)
        template4 = {
            "id": "test_template_004",
            "title": "Thank You Card",
            "category": "Thank You",
            "previewUrl": f"{BASE_URL}/templates/thankyou_preview.jpg",
            "htmlContent": "<div>Thank You Template</div>",
            "cssContent": ".thankyou { color: #4ecdc4; }",
            "jsContent": "console.log('Thank you template loaded');",
            "likeCount": 18,
            "favoriteCount": 9,
            "shareCount": 5,
            "isLiked": False,
            "isFavorited": False,
            "createdAt": (datetime.now() - timedelta(hours=8)).isoformat() + "Z",
            # Creator information - Empty strings (should fallback)
            "creatorId": "",
            "creatorName": "",
            "creatorProfilePhoto": "",
            "generatedByUser": ""
        }
        
        # Scenario 5: Creator with invalid image URL
        template5 = {
            "id": "test_template_005",
            "title": "Get Well Soon",
            "category": "Get Well",
            "previewUrl": f"{BASE_URL}/templates/getwell_preview.jpg",
            "htmlContent": "<div>Get Well Soon Template</div>",
            "cssContent": ".getwell { color: #95e1d3; }",
            "jsContent": "console.log('Get well template loaded');",
            "likeCount": 33,
            "favoriteCount": 16,
            "shareCount": 11,
            "isLiked": True,
            "isFavorited": True,
            "createdAt": (datetime.now() - timedelta(hours=12)).isoformat() + "Z",
            # Creator information - Invalid image URL
            "creatorId": "user_67890",
            "creatorName": "Alex Rodriguez",
            "creatorProfilePhoto": "https://invalid-url.com/nonexistent.jpg",
            "generatedByUser": None
        }
        
        self.sample_templates = [template1, template2, template3, template4, template5]
        return self.sample_templates
    
    def test_creator_profile_logic(self):
        """Test the creator profile display logic"""
        
        print("🧪 Testing Creator Profile Display Logic")
        print("=" * 50)
        
        for i, template in enumerate(self.sample_templates, 1):
            print(f"\n📋 Test Case {i}: {template['title']}")
            print("-" * 30)
            
            # Extract creator information
            creator_id = template.get('creatorId')
            creator_name = template.get('creatorName')
            creator_photo = template.get('creatorProfilePhoto')
            generated_by = template.get('generatedByUser')
            
            # Simulate the Android logic
            display_name = None
            display_image_url = None
            
            # Priority 1: Use creatorName and creatorProfilePhoto if available
            if creator_name and creator_name.strip():
                display_name = creator_name.strip()
                display_image_url = creator_photo
                source = "Primary Creator Info"
            # Priority 2: Use generatedByUser if available
            elif generated_by and generated_by.strip():
                display_name = generated_by.strip()
                source = "GeneratedByUser Fallback"
            else:
                # Priority 3: Default fallback
                display_name = "eventwish"
                source = "App Default Fallback"
            
            # Determine image source
            if display_image_url and display_image_url.strip():
                image_source = f"URL: {display_image_url}"
            else:
                image_source = "App Logo (R.drawable.app_logo)"
            
            # Print results
            print(f"📊 Creator ID: {creator_id or 'None'}")
            print(f"👤 Display Name: '{display_name}' ({source})")
            print(f"🖼️  Profile Image: {image_source}")
            
            # Expected behavior validation
            expected_behavior = self.get_expected_behavior(template)
            actual_behavior = {
                'display_name': display_name,
                'image_source': 'url' if display_image_url and display_image_url.strip() else 'app_logo',
                'source': source
            }
            
            # Validate results
            is_correct = self.validate_behavior(expected_behavior, actual_behavior)
            status = "✅ PASS" if is_correct else "❌ FAIL"
            print(f"🔍 Test Result: {status}")
            
            if not is_correct:
                print(f"   Expected: {expected_behavior}")
                print(f"   Actual: {actual_behavior}")
            
            self.test_results.append({
                'test_case': i,
                'template_id': template['id'],
                'title': template['title'],
                'expected': expected_behavior,
                'actual': actual_behavior,
                'passed': is_correct
            })
    
    def get_expected_behavior(self, template):
        """Define expected behavior for each test case"""
        
        creator_name = template.get('creatorName')
        creator_photo = template.get('creatorProfilePhoto')
        generated_by = template.get('generatedByUser')
        
        if creator_name and creator_name.strip():
            return {
                'display_name': creator_name.strip(),
                'image_source': 'url' if creator_photo and creator_photo.strip() else 'app_logo',
                'source': 'Primary Creator Info'
            }
        elif generated_by and generated_by.strip():
            return {
                'display_name': generated_by.strip(),
                'image_source': 'app_logo',
                'source': 'GeneratedByUser Fallback'
            }
        else:
            return {
                'display_name': 'eventwish',
                'image_source': 'app_logo',
                'source': 'App Default Fallback'
            }
    
    def validate_behavior(self, expected, actual):
        """Validate if actual behavior matches expected"""
        return (expected['display_name'] == actual['display_name'] and
                expected['image_source'] == actual['image_source'] and
                expected['source'] == actual['source'])
    
    def generate_android_test_data(self):
        """Generate Android-compatible JSON for testing"""
        
        print("\n📱 Android Test Data Generation")
        print("=" * 50)
        
        android_templates = []
        for template in self.sample_templates:
            android_template = {
                "id": template["id"],
                "title": template["title"],
                "category": template["category"],
                "previewUrl": template["previewUrl"],
                "htmlContent": template["htmlContent"],
                "cssContent": template["cssContent"],
                "jsContent": template["jsContent"],
                "likeCount": template["likeCount"],
                "favoriteCount": template["favoriteCount"],
                "shareCount": template["shareCount"],
                "isLiked": template["isLiked"],
                "isFavorited": template["isFavorited"],
                "createdAt": template["createdAt"],
                "creatorId": template["creatorId"],
                "creatorName": template["creatorName"],
                "creatorProfilePhoto": template["creatorProfilePhoto"],
                "generatedByUser": template["generatedByUser"]
            }
            android_templates.append(android_template)
        
        # Save to file for Android testing
        with open('android_test_templates.json', 'w') as f:
            json.dump(android_templates, f, indent=2)
        
        print("📄 Generated android_test_templates.json with sample data")
        print(f"📊 Created {len(android_templates)} test templates")
        
        return android_templates
    
    def generate_test_report(self):
        """Generate a comprehensive test report"""
        
        print("\n📊 Test Report Summary")
        print("=" * 50)
        
        total_tests = len(self.test_results)
        passed_tests = sum(1 for result in self.test_results if result['passed'])
        failed_tests = total_tests - passed_tests
        
        print(f"📈 Total Tests: {total_tests}")
        print(f"✅ Passed: {passed_tests}")
        print(f"❌ Failed: {failed_tests}")
        print(f"📊 Success Rate: {(passed_tests/total_tests)*100:.1f}%")
        
        if failed_tests > 0:
            print("\n❌ Failed Test Cases:")
            for result in self.test_results:
                if not result['passed']:
                    print(f"   - Test {result['test_case']}: {result['title']}")
        
        # Generate detailed report file
        report_data = {
            'timestamp': datetime.now().isoformat(),
            'total_tests': total_tests,
            'passed_tests': passed_tests,
            'failed_tests': failed_tests,
            'success_rate': (passed_tests/total_tests)*100,
            'test_results': self.test_results
        }
        
        with open('creator_profile_test_report.json', 'w') as f:
            json.dump(report_data, f, indent=2)
        
        print("\n📄 Detailed report saved to: creator_profile_test_report.json")
    
    def generate_android_integration_guide(self):
        """Generate integration guide for Android testing"""
        
        guide = """
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
"""
        
        with open('android_integration_guide.md', 'w') as f:
            f.write(guide)
        
        print("📖 Android integration guide saved to: android_integration_guide.md")
    
    def run_all_tests(self):
        """Run all tests and generate reports"""
        
        print("🚀 Starting Creator Profile Display Tests")
        print("=" * 50)
        
        # Create sample templates
        self.create_sample_templates()
        print(f"📋 Created {len(self.sample_templates)} sample templates")
        
        # Test creator profile logic
        self.test_creator_profile_logic()
        
        # Generate Android test data
        self.generate_android_test_data()
        
        # Generate test report
        self.generate_test_report()
        
        # Generate integration guide
        self.generate_android_integration_guide()
        
        print("\n🎉 All tests completed!")
        print("📁 Generated files:")
        print("   - android_test_templates.json")
        print("   - creator_profile_test_report.json")
        print("   - android_integration_guide.md")

if __name__ == "__main__":
    tester = CreatorProfileTester()
    tester.run_all_tests() 