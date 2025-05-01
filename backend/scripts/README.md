# Backend Tests

This directory contains test scripts to verify various aspects of the application.

## Template ID Tests

These tests verify that template IDs are properly handled throughout the application:

- **testTemplateId.js**: Verifies that Template objects have proper id fields in both direct objects and JSON representations.
- **testTemplateApiResponse.js**: Simulates API responses to ensure they include proper string ID fields.
- **testAndroidCompatibility.js**: Simulates Android navigation bundle handling to ensure IDs are compatible.
- **testCategoryIcons.js**: Tests the relationship between Templates and CategoryIcons.
- **runAllTemplateTests.js**: Runs all template ID tests in sequence.

## Field Update Tests

These tests verify that fields update correctly when models are modified:

- **testTemplateFieldUpdates.js**: Tests creating and updating Template fields directly.
- **testTemplateApiUpdates.js**: Tests Template updates via API calls.
- **testClientCompatibility.js**: Simulates Android client processing of templates.
- **testSponsoredAdFieldUpdates.js**: Tests SponsoredAd field updates and tracking.
- **runAllFieldUpdateTests.js**: Runs all field update tests in sequence.

## SponsoredAd Tests

These tests verify SponsoredAd functionality:

- **testSponsoredAdTracking.js**: Tests impression and click tracking with specific device IDs.
- **testDailyImpressions.js**: Tests daily impression tracking and frequency capping.

## Running Tests

Run any test script with Node.js:

```bash
node scripts/testTemplateId.js
```

Or run all tests in a category:

```bash
node scripts/runAllTemplateTests.js
node scripts/runAllFieldUpdateTests.js
```

## Key Requirements

1. Template IDs must be strings, not ObjectIds, in all API responses.
2. The `id` field must always be present and properly set from `_id`.
3. When a template is serialized to JSON (API response), the `_id` field should be removed.
4. CategoryIcon references must be properly handled in both Template objects and API responses.
5. Field updates must persist correctly, especially tracking data for SponsoredAds.

## Android Compatibility

The Android app expects all template IDs to be strings when passing them in navigation bundles. Any non-string ID will cause the error:

```
Wrong argument type for 'templateId' in argument bundle. string expected
```

This is why we ensure models' `toJSON` transform always converts `_id` to a string for the `id` field. 