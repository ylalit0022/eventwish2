# EventWish App Structure and Development Status

## Overview
EventWish is a modern Android application that allows users to create and share customizable HTML-based greeting cards. The app features a clean, intuitive UI with smooth navigation and engaging visual elements.

## Architecture
The project follows a client-server architecture:
1. **Mobile App**: Android application built with Java
2. **Backend**: Node.js/Express server with MongoDB database

## Mobile App Structure

### Architecture Pattern
- MVVM (Model-View-ViewModel) architecture
- Android Jetpack components (Navigation Component, LiveData, ViewModel)
- Repository pattern for data management

### Package Structure
```
com.ds.eventwish/
├── data/
│   ├── converter/         # Type converters for Room database
│   ├── local/             # Local database implementation
│   ├── model/             # Data models
│   │   ├── CategoryIcon.java
│   │   ├── Festival.java
│   │   ├── SharedWish.java
│   │   ├── Template.java
│   │   └── response/      # API response models
│   │       ├── CategoryIconResponse.java  # Wrapper for category icon API responses
│   │       ├── TemplateResponse.java
│   │       └── WishResponse.java
│   ├── remote/            # API client implementation
│   │   ├── ApiClient.java
│   │   └── ApiService.java
│   └── repository/        # Repositories for data management
│       ├── CategoryIconRepository.java
│       ├── TemplateRepository.java
│       └── ...
├── ui/
│   ├── about/             # About screen
│   ├── base/              # Base classes for fragments/activities
│   ├── customize/         # Template customization
│   ├── detail/            # Template detail view
│   ├── festival/          # Festival notifications
│   ├── help/              # Help screens
│   ├── history/           # Wish history
│   ├── home/              # Home screen with template grid
│   │   ├── CategoriesAdapter.java
│   │   ├── HomeFragment.java
│   │   ├── HomeViewModel.java
│   │   └── TemplateAdapter.java
│   ├── more/              # More options screen
│   ├── reminder/          # Event reminders
│   ├── render/            # HTML rendering
│   ├── template/          # Template browsing
│   └── wish/              # Shared wish display
├── utils/                 # Utility classes
│   ├── DeepLinkUtil.java
│   ├── DeepLinkHandler.java
│   ├── LinkChooserUtil.java
│   ├── WebViewLinkHandler.java
│   ├── FirebaseTokenLogger.java        # Firebase token logging utility
│   ├── FirebaseInAppMessagingHandler.java  # Firebase In-App Messaging handler
│   ├── FlashyMessageManager.java       # Flashy message management
│   └── ...
├── workers/               # Background workers
├── receivers/             # Broadcast receivers
├── MainActivity.java      # Main activity
├── TestFlashyMessageActivity.java      # Test activity for flashy messages
└── EventWishApplication.java  # Application class
```

### Key Components

#### Activities
- `MainActivity`: Main entry point handling navigation and deep links
- `TestFlashyMessageActivity`: For testing notification and flashy message functionality

#### Application Class
- `EventWishApplication`: Handles app-wide initialization
  - Properly initializes WorkManager using Configuration.Provider interface
  - Sets up notification channels
  - Initializes repositories and loads initial data
  - Initializes Firebase services and logs Firebase project information

#### UI Components
- **Home Screen**: Template grid with category filtering and advanced sorting options
- **Template Detail**: Preview and customization options
- **Shared Wish**: Display and sharing of wishes
- **History**: Past greetings and shared wishes
- **Reminder**: Event reminder management
- **More**: Additional settings and options
- **Test Flashy Message**: Testing interface for flashy messages and Firebase features

#### Navigation
- Single activity, multiple fragments architecture
- Navigation Component with nav_graph.xml
- Bottom navigation with 4 main sections

#### Data Management
- Room database for local storage
- Retrofit for API communication
- Repository pattern for data access

## Backend Structure

### Server Components
```
backend/
├── config/                # Configuration files
│   └── db.js              # MongoDB connection
├── controllers/           # API controllers
│   ├── categoryIconController.js
│   ├── festivalController.js
│   ├── templateController.js
│   └── wishController.js
├── models/                # MongoDB models
│   ├── CategoryIcon.js
│   ├── Festival.js
│   ├── SharedWish.js
│   └── Template.js
├── routes/                # API routes
│   ├── categoryIcons.js
│   ├── festivals.js
│   ├── templates.js
│   └── wishes.js
├── .well-known/           # App Links verification
├── backendUi/             # Static web UI
├── scripts/               # Utility scripts
├── server.js              # Main Express server
└── categoryIconServer.js  # Dedicated category icon server
```

### API Endpoints

#### Templates
- `GET /api/templates`: Paginated templates with category counts
- `GET /api/templates/category/:category`: Templates by category
- `GET /api/templates/:id`: Specific template by ID
- `GET /api/templates/filter`: Templates with filtering options (sort, time, category)

#### Category Icons
- `GET /api/categoryIcons`: All category icons (returns a JSON object with data array)

#### Festivals
- `GET /api/festivals/upcoming`: Upcoming festivals

#### Wishes
- `POST /api/share`: Create shared wish
- `GET /api/wish/:shortCode`: Get shared wish by short code
- `GET /api/wishes/my`: Get user's wish history
- `DELETE /api/wishes/clear`: Clear wish history
- `POST /api/wish/{shortCode}/share`: Update sharing platform information

### Deep Linking
- Custom URI scheme: `eventwish://wish/{shortCode}`, `eventwish://festival/{id}`, `eventwish://template/{id}`
- HTTP/HTTPS links: 
  - `https://eventwishes.onrender.com/wish/{shortCode}`
  - `https://eventwishes.onrender.com/festival/{id}`
  - `https://eventwishes.onrender.com/template/{id}`
- App Links implementation for seamless opening
- Comprehensive deep link handling with `DeepLinkHandler` utility class

## Features Implementation Status

### Implemented Features
- ✅ Home screen with template grid
- ✅ Category filtering with horizontal scrolling chips
- ✅ Advanced filtering system (sort options, time filters)
- ✅ Template detail view
- ✅ Template customization
- ✅ Wish sharing via multiple channels
- ✅ Deep linking for shared wishes, festivals, and templates
- ✅ History tracking for past wishes
- ✅ Festival notifications
- ✅ Category icons with fallback mechanism
- ✅ External link handling with chooser dialog
- ✅ WebView link handling
- ✅ Firebase Cloud Messaging integration
- ✅ Firebase In-App Messaging integration
- ✅ Flashy message system for notifications
- ✅ Firebase token logging and verification
- ✅ Share platform tracking (WhatsApp, Facebook, etc.)
- ✅ Full-screen mode for resource viewing

### In Progress Features
- 🔄 Event reminder system
- 🔄 Offline support for templates
- 🔄 User preferences
- 🔄 Real-time notifications
- 🔄 Enhanced caching mechanism
- 🔄 Improved Firebase In-App Messaging handling
- 🔄 MongoDB integration for share platform tracking

### Planned Features
- 📝 Enhanced sharing options
- 📝 User accounts and cloud sync
- 📝 Template favorites
- 📝 Custom template creation
- 📝 AI-powered recommendations
- 📝 Location-based features
- 📝 Premium templates and subscription model
- 📝 In-app purchases for special features

## Technical Implementation Details

### Dependencies
- **UI Components**: Material Design, RecyclerView, CardView, ConstraintLayout
- **Navigation**: Navigation Component
- **Networking**: Retrofit, OkHttp, Gson
- **Image Loading**: Glide
- **UI Effects**: Facebook Shimmer
- **Background Processing**: WorkManager
- **Local Storage**: Room Database
- **Notification Badges**: ShortcutBadger
- **Cloud Messaging**: Firebase Cloud Messaging (FCM)
- **In-App Messaging**: Firebase In-App Messaging
- **Analytics**: Firebase Analytics
- **Installation ID**: Firebase Installations
- **Monetization**: Google Play Billing Library, AdMob

### Android Features Used
- Permissions handling for notifications and alarms
- Deep linking with App Links
- Custom notifications with actions
- Background work scheduling
- Boot completed receiver for persistence
- WebView link handling
- Intent chooser for external links
- Firebase integration for messaging and analytics
- In-app purchases and subscriptions

## Development Guidelines

### Resource Naming
- Drawables: `ic_*` for icons
- Layouts: `fragment_*`, `item_*`
- Strings: Categorized by feature

### Vector Assets
- 24dp base size
- Material Design style
- Adaptive colors

### Code Style
- Java 17 compatibility
- MVVM architecture pattern
- Repository pattern for data access
- LiveData for reactive UI updates

## Performance Considerations
- Pagination for large data sets
- Image caching with Glide
- Connection optimizations in OkHttp client
- Background processing with WorkManager
- Efficient filtering implementation

## Security Considerations
- URL validation for external resources
- Input validation on all API endpoints
- Proper error handling to prevent information leakage
- Safe handling of deep links
- Secure storage of purchase information
- Validation of subscription status

## Recent Changes and Fixes

### 1. WorkManager Initialization
- Fixed double initialization issue by:
  - Properly implementing Configuration.Provider interface
  - Disabling default WorkManagerInitializer in AndroidManifest.xml
  - Using the schedule method from ReminderCheckWorker

### 2. Category Icon Repository
- Fixed JSON parsing error by:
  - Creating CategoryIconResponse wrapper class
  - Updating ApiService to use the wrapper
  - Adding robust error handling and fallbacks
  - Implementing proper null checks

### 3. Navigation Issues
- Fixed navigation to/from HomeFragment:
  - **Issue**: When navigating from another fragment back to HomeFragment, the UI is not properly refreshed
  - **Solution**: 
    - Override onResume() in HomeFragment to refresh data when returning
    - Ensure proper lifecycle handling for data loading
    - Add explicit refresh call when navigating back to HomeFragment
    - Implement proper state restoration in HomeFragment

```java
// In HomeFragment.java
@Override
public void onResume() {
    super.onResume();
    
    // Check if we're returning from another fragment
    if (wasInBackground) {
        Log.d(TAG, "Returning to HomeFragment, refreshing data");
        
        // Refresh category icons
        categoryIconRepository.refreshCategoryIcons();
        
        // Refresh templates if needed
        if (viewModel.shouldRefreshOnReturn()) {
            viewModel.loadTemplates(true);
        }
        
        wasInBackground = false;
    }
}

@Override
public void onPause() {
    super.onPause();
    wasInBackground = true;
}
```

### 4. Comprehensive Filtering System
- Implemented a robust filtering system in HomeFragment:
  - Added sort options (Trending, Newest, Oldest, Most Used)
  - Added time filters (Today, This Week, This Month, All Time)
  - Created filter chips for quick access to filtering options
  - Implemented a bottom sheet for advanced filtering
  - Updated HomeViewModel to handle filter application

### 5. Deep Link Handling
- Implemented a comprehensive deep link handling system:
  - Created DeepLinkHandler utility class to centralize deep link processing
  - Updated DeepLinkUtil to support all content types (wishes, festivals, templates)
  - Added intent filters in AndroidManifest.xml for all deep link types
  - Updated MainActivity to use the new DeepLinkHandler

### 6. External Link Handling
- Implemented a robust external link handling system:
  - Created LinkChooserUtil for opening URLs with a chooser dialog
  - Added WebViewLinkHandler for handling links in WebViews
  - Implemented proper MIME type detection for file handling
  - Added fallback mechanisms for when no app can handle a link

### 7. Firebase Integration
- Implemented Firebase Cloud Messaging (FCM) for push notifications:
  - Added Firebase dependencies to build.gradle
  - Created FirebaseTokenLogger utility to verify FCM token generation
  - Added logging for Firebase project ID and application ID
  - Created test interface for verifying Firebase integration

### 8. Firebase In-App Messaging
- Implemented Firebase In-App Messaging for in-app notifications:
  - Created FirebaseInAppMessagingHandler to handle in-app messages
  - Integrated with FlashyMessageManager to convert in-app messages to flashy messages
  - Added test functionality to trigger in-app message events
  - Fixed API compatibility issues with the Firebase In-App Messaging library

### 9. Flashy Message System
- Enhanced the flashy message system:
  - Created TestFlashyMessageActivity for testing flashy messages
  - Added Firebase verification buttons to test activity
  - Implemented FlashyMessageManager for managing flashy messages
  - Added support for converting Firebase In-App Messages to flashy messages

### 10. Resource Fragment Enhancement
- Improved the ResourceFragment for better user experience:
  - Added full-screen mode with adaptive background color
  - Implemented auto-hide for bottom navigation
  - Added touch listener to toggle UI visibility
  - Extracted dominant color from WebView content for background
  - Added fullscreen toggle button

### 11. Share Platform Tracking
- Implemented tracking of sharing platforms:
  - Added constants for different sharing platforms (WhatsApp, Facebook, etc.)
  - Created method to save the sharing platform in local database
  - Added API endpoint for updating sharing platform in MongoDB
  - Enhanced SharedWishFragment to detect which app was used for sharing
  - Added visual indicators for different sharing platforms in history view

## Real-Time Notification Implementation

### Current Implementation
- Firebase Cloud Messaging (FCM) for push notifications
- Firebase In-App Messaging for in-app notifications
- Custom flashy message system for displaying notifications within the app
- Notification channels for different types of notifications
- Test interface for verifying notification functionality

### Firebase Integration
1. **Firebase Cloud Messaging (FCM)**
   - Device token registration and logging
   - Topic-based subscriptions for user preferences
   - Background message handling
   - Foreground message handling with conversion to flashy messages

2. **Firebase In-App Messaging**
   - Custom message display component
   - Event-based triggering of in-app messages
   - Conversion of in-app messages to flashy messages
   - Test functionality for triggering events

3. **Firebase Analytics**
   - Event tracking for user actions
   - Screen tracking for navigation
   - User property tracking for personalization
   - Campaign tracking for marketing

4. **Firebase Installations**
   - Unique installation ID for device identification
   - Logging of installation ID for debugging
   - Verification of Firebase integration

### Flashy Message System
1. **Message Management**
   - Storage of messages in SharedPreferences
   - Unique message IDs for tracking
   - Message expiration handling
   - Message priority handling

2. **Message Display**
   - Custom UI for displaying messages
   - Animation for message appearance and disappearance
   - Action buttons for user interaction
   - Automatic dismissal after timeout

3. **Message Testing**
   - Test interface for creating and clearing messages
   - Default message templates for quick testing
   - Firebase integration testing
   - Event triggering for in-app messages

## App Monetization Strategy

### Monetization Models

#### 1. Freemium Model
- **Basic Features (Free)**:
  - Access to limited template categories
  - Basic customization options
  - Standard sharing options
  - Ad-supported experience
  
- **Premium Features (Paid)**:
  - Access to all template categories
  - Advanced customization options
  - Premium templates and effects
  - Ad-free experience
  - Priority support

#### 2. Subscription Tiers
- **Free Tier**:
  - Limited templates per month
  - Basic features
  - Ad-supported
  
- **Basic Subscription ($2.99/month)**:
  - Unlimited templates
  - No ads
  - Premium templates
  
- **Pro Subscription ($4.99/month)**:
  - All Basic features
  - Custom template creation
  - Advanced effects and animations
  - Priority support
  - Early access to new features

#### 3. In-App Purchases
- **Template Packs**: Themed collections of premium templates
- **Special Effects**: Advanced animations and transitions
- **Custom Elements**: Special decorative elements for templates
- **Fonts and Styles**: Premium typography options
- **Seasonal Content**: Holiday-specific templates and effects

#### 4. Advertising
- **Banner Ads**: Non-intrusive banner ads in free version
- **Interstitial Ads**: Full-screen ads at natural transition points
- **Rewarded Ads**: Watch ads to unlock premium templates temporarily
- **Native Ads**: Integrated ads that match the app's look and feel

### Implementation Plan

#### Phase 1: Foundation (1-2 months)
1. **Integrate Google Play Billing Library**:
   - Add dependency to build.gradle
   - Create BillingManager class to handle purchases
   - Implement purchase verification
   
2. **Create Subscription Management**:
   - Design subscription tiers
   - Implement subscription status checking
   - Create UI for subscription management
   
3. **Basic AdMob Integration**:
   - Add AdMob SDK
   - Implement banner ads in free version
   - Create ad-free experience for premium users

#### Phase 2: Enhanced Monetization (2-3 months)
1. **Template Marketplace**:
   - Create backend for template marketplace
   - Implement template browsing and preview
   - Add purchase flow for individual templates
   
2. **Advanced Ad Strategy**:
   - Implement interstitial ads at natural breaks
   - Add rewarded ads for temporary premium access
   - Optimize ad placement for better user experience
   
3. **Analytics Enhancement**:
   - Track purchase funnel
   - Analyze user behavior for monetization optimization
   - Implement A/B testing for pricing and offers

#### Phase 3: Optimization and Expansion (3+ months)
1. **Personalized Offers**:
   - Implement user segmentation
   - Create targeted promotions based on usage patterns
   - Develop seasonal and event-based offers
   
2. **Referral Program**:
   - Create referral system with rewards
   - Implement tracking for referral conversions
   - Design referral UI and sharing flow
   
3. **Enterprise Solutions**:
   - Develop business accounts for corporate use
   - Create bulk purchase options for businesses
   - Implement custom branding options

### Technical Implementation

#### 1. Google Play Billing Integration
```java
// BillingManager.java
public class BillingManager {
    private BillingClient billingClient;
    private final Context context;
    private final BillingUpdatesListener listener;
    
    public BillingManager(Context context, BillingUpdatesListener listener) {
        this.context = context;
        this.listener = listener;
        billingClient = BillingClient.newBuilder(context)
                .setListener(this::onPurchasesUpdated)
                .enablePendingPurchases()
                .build();
        startConnection();
    }
    
    private void startConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready
                    listener.onBillingClientReady();
                    queryPurchases();
                }
            }
            
            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request
                listener.onBillingServiceDisconnected();
            }
        });
    }
    
    // Additional methods for querying products, handling purchases, etc.
}
```

#### 2. Subscription Management
```java
// SubscriptionManager.java
public class SubscriptionManager {
    private static final String BASIC_SUBSCRIPTION = "com.ds.eventwish.subscription.basic";
    private static final String PRO_SUBSCRIPTION = "com.ds.eventwish.subscription.pro";
    
    private final BillingManager billingManager;
    private final MutableLiveData<SubscriptionStatus> subscriptionStatus = new MutableLiveData<>();
    
    public SubscriptionManager(Context context) {
        billingManager = new BillingManager(context, new BillingUpdatesListener() {
            @Override
            public void onBillingClientReady() {
                querySubscriptionStatus();
            }
            
            @Override
            public void onPurchasesUpdated(List<Purchase> purchases) {
                processPurchases(purchases);
            }
            
            @Override
            public void onBillingServiceDisconnected() {
                // Handle disconnection
            }
        });
    }
    
    // Methods for subscription management
}
```

#### 3. AdMob Integration
```java
// AdManager.java
public class AdManager {
    private static final String TAG = "AdManager";
    private final Context context;
    private final SubscriptionManager subscriptionManager;
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    
    public AdManager(Context context, SubscriptionManager subscriptionManager) {
        this.context = context;
        this.subscriptionManager = subscriptionManager;
        
        // Initialize the Mobile Ads SDK
        MobileAds.initialize(context, initializationStatus -> {
            Log.d(TAG, "AdMob SDK initialized");
        });
        
        // Load ads
        loadInterstitialAd();
        loadRewardedAd();
    }
    
    // Methods for ad management
}
```

### Revenue Projection and Analysis

#### Estimated Revenue Streams
1. **Subscriptions**: 60-70% of total revenue
2. **In-App Purchases**: 20-25% of total revenue
3. **Advertising**: 10-15% of total revenue

#### Key Performance Indicators (KPIs)
1. **Average Revenue Per User (ARPU)**
2. **Customer Lifetime Value (CLV)**
3. **Conversion Rate** (free to paid)
4. **Subscription Renewal Rate**
5. **Churn Rate**
6. **Ad Engagement Rate**

#### Analytics Implementation
- Firebase Analytics for user behavior tracking
- Google Play Console for purchase analytics
- Custom events for monetization funnel analysis
- A/B testing framework for optimizing pricing and offers

## Known Issues

1. Category icon loading may fail on slow connections
2. Deep links may not work consistently on all Android versions
3. Reminder notifications may be delayed on some devices
4. Template loading performance could be improved
5. ~~Navigation to HomeFragment from other fragments doesn't refresh data~~ (Fixed)
6. ~~WorkManager initialization error~~ (Fixed)
7. ~~Category icon JSON parsing error~~ (Fixed)
8. **Flashy Message Persistence Issue**: When first creating an in-app message or on new app installation, flashy messages display correctly. However, subsequent messages don't appear until the app data and cache are cleared. This suggests a potential issue with message storage or display logic that needs investigation.

## Troubleshooting Flashy Messages

### Current Behavior
- First message after installation or clearing app data displays correctly
- Subsequent messages are created but not displayed
- Clearing app data and cache resolves the issue temporarily

### Potential Causes
1. **Message Storage**: Messages may not be properly stored or retrieved from SharedPreferences
2. **Display Logic**: The display component may not be properly refreshed after the first message
3. **Message Queue**: There might be issues with the message queue management
4. **Firebase Integration**: Firebase In-App Messaging might be interfering with the flashy message system
5. **Lifecycle Issues**: Activity or fragment lifecycle methods might not be properly handling message display

### Recommended Fixes
1. **Improve Message Storage**:
   - Ensure proper synchronization when accessing SharedPreferences
   - Implement a more robust storage mechanism (e.g., Room database)
   - Add logging to verify message storage and retrieval

2. **Enhance Display Logic**:
   - Implement a message observer pattern to react to new messages
   - Ensure UI is updated when new messages are available
   - Add refresh mechanisms for the message display component

3. **Review Message Queue**:
   - Implement proper queue management for messages
   - Ensure messages are properly prioritized and displayed in order
   - Add mechanisms to prevent message loss

4. **Firebase Integration**:
   - Ensure Firebase In-App Messaging is properly initialized
   - Verify that the custom display component is working correctly
   - Add more robust error handling for Firebase integration

5. **Lifecycle Management**:
   - Review activity and fragment lifecycle methods
   - Ensure messages are checked and displayed at appropriate lifecycle points
   - Implement proper state restoration for message display

## Development Environment
- Android Studio Arctic Fox or newer
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Java 17
- Node.js 14+ for backend development 