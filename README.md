# EventWish - Modern Greeting Card App

EventWish is a modern Android application that allows users to create and share customizable HTML-based greeting cards. The app features a clean, intuitive UI with smooth navigation and engaging visual elements.

## Features

### 1. Home Screen
- **Search Bar**: Live search functionality with real-time filtering
- **Category Navigation**: 
  - Horizontal scrolling category chips
  - Interactive selection with visual feedback
  - "Load More" functionality for additional categories
- **Grid Layout**: 
  - 2-column grid displaying greeting cards
  - Each card shows image, title, and category
  - Smooth animations and transitions

### 2. Navigation
- **Bottom Navigation Bar** with 4 main sections:
  - Home: Main greeting cards grid
  - History: View past greetings (Coming Soon)
  - Reminder: Set event reminders (Coming Soon)
  - More: Additional options (Coming Soon)

### 3. Greeting Card Features
- **Card Preview**: 
  - Visual preview with GIF support
  - Responsive layout
  - HTML-based rendering
- **Customization**:
  - Sender name input with validation
  - Recipient name input with validation
- **Sharing**: 
  - Social media sharing functionality
  - Custom share intent
  - Preview before sharing

## Implementation Plan

### 1. Data Layer Implementation
#### 1.1 Models
- **Template.java**
  - Fields: id, title, category, htmlContent, cssContent, jsContent, previewUrl, status, categoryIcon
  - Methods: getters/setters, toString()

- **SharedWish.java**
  - Fields: shortCode, templateId, recipientName, senderName, customizedHtml, views
  - Methods: getters/setters, toString()

#### 1.2 API Integration
- **ApiService.java**
  ```java
  // Endpoints
  - getTemplates(page, limit)
  - getTemplatesByCategory(category, page, limit)
  - getTemplate(id)
  - createSharedWish(SharedWish)
  - getSharedWish(shortCode)
  ```

- **ApiClient.java**
  - Retrofit configuration
  - Error handling
  - Response interceptors

#### 1.3 Repository
- **TemplateRepository.java**
  - Template caching
  - Pagination handling
  - Category filtering
  - Error handling

### 2. UI Layer Implementation
#### 2.1 Home Screen
- **HomeFragment.java Updates**
  - Infinite scroll (20 items per page)
  - Category filtering
  - Template grid/list view
  - Search functionality

#### 2.2 Template Detail
- **GreetingDetailFragment.java Updates**
  - HTML/CSS/JS renderer
  - Name customization
  - Live preview
  - Share functionality

#### 2.3 Shared Wish
- **SharedWishFragment.java (New)**
  - Template rendering
  - View counter
  - Social sharing

### 3. Backend Implementation
#### 3.1 Database
```javascript
// Template Schema
{
  title: String,
  category: String,
  htmlContent: String,
  cssContent: String,
  jsContent: String,
  previewUrl: String,
  status: Boolean,
  categoryIcon: String
}

// SharedWish Schema
{
  shortCode: String,
  templateId: ObjectId,
  recipientName: String,
  senderName: String,
  customizedHtml: String,
  views: Number
}
```

#### 3.2 API Endpoints
```javascript
// Templates
GET    /api/templates?page=1&limit=20
GET    /api/templates/categories
GET    /api/templates/category/:categoryId
GET    /api/templates/:id

// Shared Wishes
POST   /api/wishes/create
GET    /api/wishes/:shortCode
```

### 4. Implementation Phases

#### Phase 1: Core Infrastructure
1. Set up API client and models
2. Implement repository layer
3. Basic template listing
4. Category filtering

#### Phase 2: Template Rendering
1. HTML/CSS/JS renderer
2. Template customization
3. Live preview
4. Image loading

#### Phase 3: Sharing System
1. Create sharing mechanism
2. Generate short URLs
3. View shared wishes
4. Social media integration

#### Phase 4: Optimization
1. Template caching
2. Image caching
3. Lazy loading
4. Error handling

### 5. Dependencies Required
```gradle
// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

// Image Loading
implementation 'com.github.bumptech.glide:glide:4.12.0'

// UI Components
implementation 'androidx.recyclerview:recyclerview:1.3.1'
implementation 'com.google.android.material:material:1.9.0'

// Lifecycle
implementation 'androidx.lifecycle:lifecycle-viewmodel:2.6.2'
implementation 'androidx.lifecycle:lifecycle-livedata:2.6.2'
```

### 6. Future Enhancements
- Offline mode support
- Template favorites
- Custom template creation
- Multiple language support
- Analytics integration
- Dark mode support

## Implementation Details

### 1. Core Components

#### Activities
- `MainActivity`: 
  - Manages navigation
  - Handles bottom navigation
  - Implements view binding

#### Fragments
1. `HomeFragment`:
   - Search functionality
   - Category filtering
   - Grid display management
   
2. `GreetingDetailFragment`:
   - Form validation
   - HTML preview
   - Share functionality

3. Placeholder Fragments:
   - `HistoryFragment`
   - `ReminderFragment`
   - `MoreFragment`

#### Adapters
1. `GreetingsAdapter`:
   - Grid layout management
   - Search filtering
   - Click handling
   
2. `CategoriesAdapter`:
   - Horizontal scrolling
   - Selection state management
   - Dynamic loading

### 2. UI Components

#### Layouts
1. `activity_main.xml`:
   - CoordinatorLayout base
   - Navigation host
   - Bottom navigation

2. `fragment_home.xml`:
   - Search bar with Material Design
   - Category RecyclerView
   - Greetings Grid RecyclerView
   - Load More FAB

3. `fragment_greeting_detail.xml`:
   - Material text inputs
   - WebView preview
   - Share button
   - Navigation handling

4. `item_greeting.xml`:
   - MaterialCardView design
   - Image preview
   - Title and category chips

5. `item_category.xml`:
   - Material Chip design
   - Selection states
   - Custom styling

### 3. Design Elements

#### Colors
```xml
Primary: #FF6200EE
Primary Variant: #FF3700B3
Accent: #FF03DAC5
Background: #FAFAFA
Surface: #FFFFFF
Text on Surface: #212121
```

#### Material Components
- TextInputLayout with outlined style
- MaterialCardView with elevation
- MaterialChips for categories
- ExtendedFloatingActionButton
- BottomNavigationView
- CoordinatorLayout behaviors

## Getting Started

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on an emulator or device

## Requirements
- Android Studio Arctic Fox or newer
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Java 11

## Dependencies
```gradle
dependencies {
    // Navigation Component
    implementation 'androidx.navigation:navigation-fragment:2.7.6'
    implementation 'androidx.navigation:navigation-ui:2.7.6'
    
    // Lifecycle components
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    
    // UI Components
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'com.google.android.material:material:1.11.0'
    
    // Image Loading
    implementation 'com.github.bumptech.glide:glide:4.16.0'
}

## Future Enhancements
1. User Authentication
2. Offline Support
3. Custom Template Creation
4. Advanced Sharing Options
5. Push Notifications
6. Event Reminders
7. Template Categories
8. Social Features
