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
  - History: View past greetings and shared wishes
  - Reminder: Set event reminders (Coming Soon)
  - More: Additional options and settings

### 3. Greeting Card Features
- **Card Preview**: 
  - Visual preview with GIF support
  - Responsive layout
  - HTML-based rendering
- **Customization**:
  - Sender name input with validation
  - Recipient name input with validation
  - Message customization
- **Sharing**: 
  - Social media sharing functionality
  - WhatsApp direct sharing
  - Copy link functionality
  - Custom share intent with preview

### 4. Deep Linking
- **URL Formats**:
  - Web: `https://eventwish2.onrender.com/wish/{shortCode}`
  - App: `eventwish://wish/{shortCode}`
- **Features**:
  - Direct app opening
  - Play Store fallback
  - Rich link previews
  - Analytics tracking

## Implementation Details

### 1. Resource Organization
#### 1.1 Drawables
- **Action Icons**:
  - `ic_close.xml`: Close/dismiss action
  - `ic_edit.xml`: Edit functionality
  - `ic_preview.xml`: Preview mode
  - `ic_save.xml`: Save changes
  - `ic_share.xml`: General sharing
  - `ic_whatsapp.xml`: WhatsApp sharing
  - `ic_copy.xml`: Copy to clipboard

#### 1.2 Layouts
- **Fragments**:
  - `fragment_shared_wish.xml`: Wish viewing screen
  - `fragment_template_customize.xml`: Template editing
  - `fragment_template_detail.xml`: Template preview
- **Components**:
  - `bottom_sheet_share.xml`: Sharing options sheet
  - `item_history.xml`: History list item

#### 1.3 Values
- **Strings**:
  - Action strings (share, reply, edit)
  - Template customization text
  - Sharing messages
  - UI feedback messages
- **Colors**:
  - Brand colors
  - UI element colors
  - State colors

### 2. Code Structure
- **Activities**:
  - `MainActivity`: Navigation and deep link handling
- **Fragments**:
  - `SharedWishFragment`: Wish display and sharing
  - `TemplateCustomizeFragment`: Wish editing
- **Utils**:
  - `DeepLinkUtil`: Deep link and sharing handling

### 3. Development Guidelines
- **Resource Naming**:
  - Drawables: `ic_*` for icons
  - Layouts: `fragment_*`, `item_*`
  - Strings: Categorized by feature
- **Vector Assets**:
  - 24dp base size
  - Material Design style
  - Adaptive colors

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
    
    // Networking
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    
    // SwipeRefreshLayout
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
}
```

## Future Enhancements
1. Implement reminder functionality
2. Add wish categories and filtering
3. Enhanced sharing options
4. User preferences
5. Offline support
