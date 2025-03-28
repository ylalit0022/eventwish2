# Flutter Migration Guide for EventWish

## Testing Strategy

### 1. Unit Tests

Create unit tests for repositories, use cases, and utilities:

```dart
// test/unit/repositories/template_repository_test.dart
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

class MockTemplateApi extends Mock implements TemplateApi {}

void main() {
  late TemplateRepository repository;
  late MockTemplateApi mockApi;

  setUp(() {
    mockApi = MockTemplateApi();
    repository = TemplateRepository(mockApi);
  });

  group('TemplateRepository', () {
    test('getTemplates returns list of templates', () async {
      // Arrange
      final templates = [
        Template(
          id: '1',
          name: 'Test Template',
          description: 'Test Description',
          category: 'Test Category',
          thumbnailUrl: 'https://example.com/thumb.jpg',
          htmlContent: '<div>Test</div>',
          cssContent: '.test {}',
          jsContent: 'console.log("test")',
          type: 'html',
          createdAt: DateTime.now(),
          updatedAt: DateTime.now(),
        ),
      ];
      
      when(() => mockApi.getTemplates(
        category: any(named: 'category'),
        sortBy: any(named: 'sortBy'),
        timeFilter: any(named: 'timeFilter'),
      )).thenAnswer((_) async => templates);

      // Act
      final result = await repository.getTemplates();

      // Assert
      expect(result, equals(templates));
      verify(() => mockApi.getTemplates()).called(1);
    });

    test('getTemplateById returns template', () async {
      // Arrange
      final template = Template(
        id: '1',
        name: 'Test Template',
        description: 'Test Description',
        category: 'Test Category',
        thumbnailUrl: 'https://example.com/thumb.jpg',
        htmlContent: '<div>Test</div>',
        cssContent: '.test {}',
        jsContent: 'console.log("test")',
        type: 'html',
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );
      
      when(() => mockApi.getTemplateById('1'))
          .thenAnswer((_) async => template);

      // Act
      final result = await repository.getTemplateById('1');

      // Assert
      expect(result, equals(template));
      verify(() => mockApi.getTemplateById('1')).called(1);
    });
  });
}
```

### 2. Widget Tests

Create widget tests for UI components:

```dart
// test/widget/template_card_test.dart
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('TemplateCard displays template info', (tester) async {
    // Arrange
    final template = Template(
      id: '1',
      name: 'Test Template',
      description: 'Test Description',
      category: 'Test Category',
      thumbnailUrl: 'https://example.com/thumb.jpg',
      htmlContent: '<div>Test</div>',
      cssContent: '.test {}',
      jsContent: 'console.log("test")',
      type: 'html',
      createdAt: DateTime.now(),
      updatedAt: DateTime.now(),
    );

    // Act
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: TemplateCard(
            template: template,
            onTap: () {},
          ),
        ),
      ),
    );

    // Assert
    expect(find.text('Test Template'), findsOneWidget);
    expect(find.text('Test Category'), findsOneWidget);
  });

  testWidgets('TemplateCard handles tap', (tester) async {
    // Arrange
    bool tapped = false;
    final template = Template(
      id: '1',
      name: 'Test Template',
      description: 'Test Description',
      category: 'Test Category',
      thumbnailUrl: 'https://example.com/thumb.jpg',
      htmlContent: '<div>Test</div>',
      cssContent: '.test {}',
      jsContent: 'console.log("test")',
      type: 'html',
      createdAt: DateTime.now(),
      updatedAt: DateTime.now(),
    );

    // Act
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: TemplateCard(
            template: template,
            onTap: () => tapped = true,
          ),
        ),
      ),
    );

    await tester.tap(find.byType(TemplateCard));
    await tester.pump();

    // Assert
    expect(tapped, isTrue);
  });
}
```

### 3. Integration Tests

Create integration tests for full user flows:

```dart
// integration_test/app_test.dart
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('End-to-end test', () {
    testWidgets('Create and share wish flow', (tester) async {
      // Start app
      await tester.pumpWidget(EventWishApp());
      await tester.pumpAndSettle();

      // Navigate to template detail
      await tester.tap(find.byType(TemplateCard).first);
      await tester.pumpAndSettle();

      // Enter recipient and sender names
      await tester.enterText(
        find.byKey(Key('recipient_input')),
        'John Doe',
      );
      await tester.enterText(
        find.byKey(Key('sender_input')),
        'Jane Doe',
      );
      await tester.pumpAndSettle();

      // Share wish
      await tester.tap(find.byIcon(Icons.share));
      await tester.pumpAndSettle();

      // Verify navigation to shared wish screen
      expect(find.byType(SharedWishScreen), findsOneWidget);
    });

    testWidgets('Unlock premium feature flow', (tester) async {
      // Start app
      await tester.pumpWidget(EventWishApp());
      await tester.pumpAndSettle();

      // Navigate to template detail
      await tester.tap(find.byType(TemplateCard).first);
      await tester.pumpAndSettle();

      // Try to use premium feature
      await tester.tap(find.byIcon(Icons.edit));
      await tester.pumpAndSettle();

      // Verify ad reward dialog appears
      expect(find.byType(AdRewardDialog), findsOneWidget);

      // Watch ad and earn coins
      await tester.tap(find.text('Watch Ad'));
      await tester.pumpAndSettle();

      // Unlock feature
      await tester.tap(find.text('Unlock Feature'));
      await tester.pumpAndSettle();

      // Verify feature is unlocked
      expect(find.byType(HtmlEditor), findsOneWidget);
    });
  });
}
```

### 4. Test Coverage

Set up test coverage reporting:

1. Add coverage package:
```yaml
dev_dependencies:
  coverage: ^1.6.3
```

2. Run tests with coverage:
```bash
flutter test --coverage
```

3. Generate coverage report:
```bash
genhtml coverage/lcov.info -o coverage/html
```

4. Configure CI/CD to track coverage:
```yaml
- name: Run tests with coverage
  run: flutter test --coverage

- name: Upload coverage to Codecov
  uses: codecov/codecov-action@v3
  with:
    file: coverage/lcov.info
```

### 5. Test Organization

Organize tests by feature and type:

```
test/
├── unit/
│   ├── repositories/
│   │   ├── coins_repository_test.dart
│   │   ├── template_repository_test.dart
│   │   └── festival_repository_test.dart
│   ├── providers/
│   │   ├── coins_provider_test.dart
│   │   ├── template_provider_test.dart
│   │   └── festival_provider_test.dart
│   └── utils/
│       ├── analytics_utils_test.dart
│       ├── date_utils_test.dart
│       └── share_utils_test.dart
├── widget/
│   ├── common/
│   │   ├── category_chip_test.dart
│   │   ├── shimmer_loading_test.dart
│   │   └── template_card_test.dart
│   └── pages/
│       ├── home_page_test.dart
│       ├── template_detail_page_test.dart
│       └── shared_wish_page_test.dart
└── integration/
    ├── app_test.dart
    ├── monetization_test.dart
    └── reminder_test.dart
```

### 6. Test Data

Create test data factories:

```dart
// test/utils/test_data.dart
class TestData {
  static Template createTemplate({
    String? id,
    String? name,
    String? description,
    String? category,
  }) {
    return Template(
      id: id ?? '1',
      name: name ?? 'Test Template',
      description: description ?? 'Test Description',
      category: category ?? 'Test Category',
      thumbnailUrl: 'https://example.com/thumb.jpg',
      htmlContent: '<div>Test</div>',
      cssContent: '.test {}',
      jsContent: 'console.log("test")',
      type: 'html',
      createdAt: DateTime.now(),
      updatedAt: DateTime.now(),
    );
  }

  static Festival createFestival({
    String? id,
    String? name,
    String? description,
    String? category,
    DateTime? date,
  }) {
    return Festival(
      id: id ?? '1',
      name: name ?? 'Test Festival',
      description: description ?? 'Test Description',
      category: category ?? 'Test Category',
      date: date ?? DateTime.now().add(Duration(days: 7)),
      templates: [createTemplate()],
    );
  }

  static Reminder createReminder({
    int? id,
    String? title,
    String? description,
    DateTime? dateTime,
  }) {
    return Reminder(
      id: id ?? 1,
      title: title ?? 'Test Reminder',
      description: description ?? 'Test Description',
      dateTime: dateTime ?? DateTime.now().add(Duration(days: 1)),
    );
  }
}
```

### 7. Test Best Practices

1. **Arrange-Act-Assert**: Follow the AAA pattern in tests
2. **Descriptive Names**: Use clear test names that describe the scenario
3. **Test Independence**: Each test should be independent and not rely on other tests
4. **Mock External Dependencies**: Use mocks for APIs, databases, etc.
5. **Test Edge Cases**: Include error cases and boundary conditions
6. **Keep Tests Simple**: One assertion per test when possible
7. **Use Test Groups**: Organize related tests together
8. **Avoid Test Duplication**: Use helper functions for common setup
9. **Test Real Scenarios**: Base tests on actual user flows
10. **Maintain Tests**: Update tests when code changes

## Deployment

### 1. Android Release

1. Update `android/app/build.gradle`:

```gradle
android {
    defaultConfig {
        applicationId "com.ds.eventwish"
        minSdkVersion 21
        targetSdkVersion 33
        versionCode flutterVersionCode.toInteger()
        versionName flutterVersionName
    }

    signingConfigs {
        release {
            keyAlias keystoreProperties['keyAlias']
            keyPassword keystoreProperties['keyPassword']
            storeFile keystoreProperties['storeFile'] ? file(keystoreProperties['storeFile']) : null
            storePassword keystoreProperties['storePassword']
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}
```

2. Create keystore and `key.properties`:

```properties
storePassword=<password>
keyPassword=<password>
keyAlias=upload
storeFile=../upload-keystore.jks
```

3. Build release APK:

```bash
flutter build appbundle
```

### 2. iOS Release

1. Update `ios/Runner/Info.plist`:

```xml
<key>CFBundleShortVersionString</key>
<string>$(FLUTTER_BUILD_NAME)</string>
<key>CFBundleVersion</key>
<string>$(FLUTTER_BUILD_NUMBER)</string>
```

2. Set up signing in Xcode:
   - Open `ios/Runner.xcworkspace`
   - Configure signing certificates
   - Set up provisioning profiles

3. Build release IPA:

```bash
flutter build ipa
```

### 3. CI/CD Setup

Create GitHub Actions workflow:

```yaml
name: Build and Deploy

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: subosito/flutter-action@v2
      - run: flutter pub get
      - run: flutter test

  build-android:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v2
        with:
          distribution: 'zulu'
          java-version: '11'
      - uses: subosito/flutter-action@v2
      - run: flutter pub get
      - run: flutter build appbundle

  build-ios:
    needs: test
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - uses: subosito/flutter-action@v2
      - run: flutter pub get
      - run: flutter build ios --release --no-codesign
```

### 4. App Store Submission

1. Android:
   - Create app in Google Play Console
   - Upload app bundle
   - Fill store listing
   - Submit for review

2. iOS:
   - Create app in App Store Connect
   - Upload build through Xcode
   - Fill store listing
   - Submit for review

### 5. Release Management

1. Version Control:
   - Use semantic versioning (MAJOR.MINOR.PATCH)
   - Update version in `pubspec.yaml`
   - Create git tags for releases

2. Release Notes:
   - Document changes in CHANGELOG.md
   - Include bug fixes and new features
   - Note breaking changes

3. Staged Rollout:
   - Use staged rollout in Play Store
   - Use TestFlight for iOS beta testing
   - Monitor crash reports and feedback

4. Post-Release:
   - Monitor analytics
   - Track user feedback
   - Plan next release

### 6. Security

1. Code Signing:
   - Secure keystore files
   - Use CI/CD secrets
   - Rotate keys periodically

2. API Security:
   - Use HTTPS
   - Implement API key rotation
   - Add request signing

3. Data Protection:
   - Encrypt sensitive data
   - Use secure storage
   - Implement app lock

### 7. Monitoring

1. Firebase Crashlytics:
```dart
void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  FlutterError.onError = FirebaseCrashlytics.instance.recordFlutterError;
  
  runApp(EventWishApp());
}
```

2. Analytics Events:
```dart
Future<void> trackEvent(String name, Map<String, dynamic> parameters) async {
  await FirebaseAnalytics.instance.logEvent(
    name: name,
    parameters: parameters,
  );
}
```

3. Performance Monitoring:
```dart
final trace = FirebasePerformance.instance.newTrace('template_load');
await trace.start();
try {
  await loadTemplate();
} finally {
  await trace.stop();
}
```

### 8. App Updates

1. In-App Updates (Android):
```dart
Future<void> checkForUpdate() async {
  final info = await PackageInfo.fromPlatform();
  final response = await http.get(Uri.parse(
    'https://eventwishes.onrender.com/api/version',
  ));
  
  final data = json.decode(response.body);
  if (data['latest_version'] != info.version) {
    showUpdateDialog();
  }
}
```

2. Force Update:
```dart
void checkForceUpdate() {
  if (needsForceUpdate) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => AlertDialog(
        title: Text('Update Required'),
        content: Text('Please update to continue using the app.'),
        actions: [
          TextButton(
            child: Text('Update Now'),
            onPressed: () => openStore(),
          ),
        ],
      ),
    );
  }
}
```

### Conclusion

This guide provides a comprehensive roadmap for converting the EventWish application from Java/Android to Flutter. By following this approach, you'll maintain the existing monetization system and backend API integration while leveraging Flutter's cross-platform capabilities for future development.

Key benefits of the Flutter conversion:
1. Cross-platform support (Android and iOS)
2. Faster development with hot reload
3. Rich widget ecosystem
4. Strong type system and null safety
5. Modern state management with Riverpod
6. Improved testing capabilities
7. Simplified deployment process

Remember to:
1. Test thoroughly on both platforms
2. Handle platform-specific features appropriately
3. Maintain backward compatibility with existing APIs
4. Follow Flutter best practices and patterns
5. Keep dependencies up to date
6. Monitor app performance and crash reports 