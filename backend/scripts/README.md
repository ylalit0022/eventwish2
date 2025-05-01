# Template ID Tests

This directory contains test scripts to verify that template IDs are properly handled throughout the application, especially for Android compatibility.

## Test Scripts

- **testTemplateId.js**: Verifies that Template objects have proper id fields in both direct objects and JSON representations.
- **testTemplateApiResponse.js**: Simulates API responses to ensure they include proper string ID fields.
- **testAndroidCompatibility.js**: Simulates Android navigation bundle handling to ensure IDs are compatible.
- **testCategoryIcons.js**: Tests the relationship between Templates and CategoryIcons.

## Running Tests

Run any test script with Node.js:

```bash
node scripts/testTemplateId.js
```

## Key Requirements

1. Template IDs must be strings, not ObjectIds, in all API responses.
2. The `id` field must always be present and properly set from `_id`.
3. When a template is serialized to JSON (API response), the `_id` field should be removed.
4. CategoryIcon references must be properly handled in both Template objects and API responses.

## Android Compatibility

The Android app expects all template IDs to be strings when passing them in navigation bundles. Any non-string ID will cause the error:

```
Wrong argument type for 'templateId' in argument bundle. string expected
```

This is why we ensure the Template model's `toJSON` transform always converts `_id` to a string for the `id` field. 