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
│   └── ...
├── workers/               # Background workers
├── receivers/             # Broadcast receivers
├── MainActivity.java      # Main activity
└── EventWishApplication.java  # Application class
```

### Key Components

#### Activities
- `MainActivity`: Main entry point handling navigation and deep links
- `TestNotificationActivity`: For testing notification functionality

#### Application Class
- `EventWishApplication`: Handles app-wide initialization
  - Properly initializes WorkManager using Configuration.Provider interface
  - Sets up notification channels
  - Initializes repositories and loads initial data

#### UI Components
- **Home Screen**: Template grid with category filtering and advanced sorting options
- **Template Detail**: Preview and customization options
- **Shared Wish**: Display and sharing of wishes
- **History**: Past greetings and shared wishes
- **Reminder**: Event reminder management
- **More**: Additional settings and options

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

### In Progress Features
- 🔄 Event reminder system
- 🔄 Offline support for templates
- 🔄 User preferences
- 🔄 Real-time notifications
- 🔄 Enhanced caching mechanism

### Planned Features
- 📝 Enhanced sharing options
- 📝 User accounts and cloud sync
- 📝 Template favorites
- 📝 Custom template creation
- 📝 AI-powered recommendations
- 📝 Location-based features

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

### Android Features Used
- Permissions handling for notifications and alarms
- Deep linking with App Links
- Custom notifications with actions
- Background work scheduling
- Boot completed receiver for persistence
- WebView link handling
- Intent chooser for external links

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

## Real-Time Notification Plans

### Current Implementation
- Basic notification system using AlarmManager and BroadcastReceivers
- Festival notifications based on predefined dates
- Reminder notifications with snooze and complete actions
- Notification channels for different types of notifications

### Planned Enhancements
1. **Firebase Cloud Messaging (FCM) Integration**
   - Real-time push notifications for new content
   - Silent notifications for background data sync
   - Topic-based subscriptions for user preferences

2. **Notification Categories**
   - **Content Updates**: New templates, festivals, features
   - **Social Notifications**: Shares, likes, comments
   - **Reminders**: Event reminders, scheduled wishes
   - **System**: App updates, maintenance alerts

3. **Smart Notification Scheduling**
   - Time-zone aware notifications
   - User activity pattern recognition
   - Batching of notifications to reduce interruptions
   - Priority-based delivery

4. **Rich Notifications**
   - Image previews for new templates
   - Action buttons for quick responses
   - Expandable notifications with more details
   - Progress indicators for background tasks

5. **Notification Analytics**
   - Tracking of notification engagement
   - A/B testing of notification content
   - User preference learning

## Recommended Features

### Personalization
1. **AI-Powered Recommendations**
   - Personalized template suggestions based on user history
   - Smart categorization of templates
   - Predictive text for wish messages

2. **Custom Templates**
   - User-created templates with custom layouts
   - Template editor with drag-and-drop interface
   - Custom color schemes and fonts

3. **Favorites System**
   - Save favorite templates for quick access
   - Organize favorites into collections
   - Smart suggestions based on favorites

### Social Features
1. **Collaborative Wishes**
   - Multiple users contributing to a single wish
   - Group signatures and messages
   - Shared editing capabilities

2. **Social Sharing Integration**
   - Deep integration with social platforms
   - Share progress and achievements
   - Friend connections and recommendations

3. **Community Templates**
   - User-submitted templates
   - Voting and ranking system
   - Featured community creations

### Enhanced Filtering and Discovery
1. **Location-Based Filters**
   - Region-specific templates and festivals
   - Local event integration
   - Cultural customization

2. **Trending by Region**
   - See what's popular in different areas
   - Regional trending categories
   - Cultural event highlights

3. **Advanced Search Capabilities**
   - Natural language search queries
   - Visual search (search by image)
   - Voice search integration

### Content Enrichment
1. **Rich Media Templates**
   - Video backgrounds
   - Animated elements
   - Audio messages and music

2. **Advanced Template Customization**
   - Layer-based editing
   - Advanced text formatting
   - Photo filters and effects

3. **Seasonal Collections**
   - Curated template collections for seasons
   - Limited-time special templates
   - Themed user interface changes

### Engagement Features
1. **Gamification Elements**
   - Achievement badges for app usage
   - Points for creating and sharing wishes
   - Leaderboards and challenges

2. **Daily Challenges**
   - Creative prompts for wish creation
   - Daily template highlights
   - Timed exclusive content

3. **Streaks and Rewards**
   - Consecutive day usage rewards
   - Milestone celebrations
   - Premium content unlocks

### Technical Improvements
1. **Offline Mode**
   - Full functionality without internet
   - Background sync when connection returns
   - Bandwidth-saving options

2. **Performance Optimization**
   - Reduced app size
   - Faster loading times
   - Battery usage optimization

3. **Accessibility Enhancements**
   - Screen reader compatibility
   - Dynamic text sizing
   - Color contrast options

### Monetization Opportunities
1. **Premium Templates**
   - Exclusive high-quality templates
   - Early access to new designs
   - Premium effects and animations

2. **Subscription Tiers**
   - Basic (free) with ads
   - Premium (paid) with additional features
   - Pro (paid) with all features and no ads

3. **In-App Purchases**
   - Template packs
   - Special effects
   - Custom fonts and styles

### Integration and Expansion
1. **Calendar Integration**
   - Automatic event detection
   - Reminder scheduling
   - Birthday and anniversary tracking

2. **Smart Home Integration**
   - Display wishes on smart displays
   - Voice assistant integration
   - IoT device notifications

3. **Cross-Platform Support**
   - Web application
   - iOS version
   - Desktop companion

## Known Issues
1. Category icon loading may fail on slow connections
2. Deep links may not work consistently on all Android versions
3. Reminder notifications may be delayed on some devices
4. Template loading performance could be improved
5. ~~Navigation to HomeFragment from other fragments doesn't refresh data~~ (Fixed)
6. ~~WorkManager initialization error~~ (Fixed)
7. ~~Category icon JSON parsing error~~ (Fixed)

## Development Environment
- Android Studio Arctic Fox or newer
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Java 17
- Node.js 14+ for backend development 