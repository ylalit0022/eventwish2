# EventWish - Technical Documentation

## Architecture Overview

### 1. Data Flow Architecture
```
UI Layer (Fragments) ←→ ViewModels ←→ Repository ←→ API Service ←→ Backend
```

### 2. Component Details

#### 2.1 Data Models
##### Template Model
```java
public class Template {
    private String id;
    private String title;
    private String category;
    private String htmlContent;
    private String cssContent;
    private String jsContent;
    private String previewUrl;
    private boolean status;
    private String categoryIcon;
    // ... getters and setters
}
```

##### SharedWish Model
```java
public class SharedWish {
    private String shortCode;
    private String templateId;
    private String recipientName;
    private String senderName;
    private String customizedHtml;
    private int views;
    // ... getters and setters
}
```

#### 2.2 API Integration
```java
public interface ApiService {
    @GET("templates")
    Call<TemplateResponse> getTemplates(
        @Query("page") int page,
        @Query("limit") int limit
    );

    @GET("templates/category/{categoryId}")
    Call<TemplateResponse> getTemplatesByCategory(
        @Path("categoryId") String categoryId,
        @Query("page") int page,
        @Query("limit") int limit
    );

    @GET("wishes/{shortCode}")
    Call<SharedWish> getSharedWish(@Path("shortCode") String shortCode);
}
```

### 3. Key Features Implementation

#### 3.1 Template Rendering
- WebView for HTML/CSS/JS rendering
- JavaScript interface for name customization
- Custom WebViewClient for image loading
- Error handling for failed renders

#### 3.2 Infinite Scroll
```java
private void setupRecyclerView() {
    recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            if (!isLoading && !isLastPage) {
                if (layoutManager.findLastCompletelyVisibleItemPosition() == templates.size() - 1) {
                    loadMoreTemplates();
                }
            }
        }
    });
}
```

#### 3.3 Category Filtering
```java
private void filterByCategory(String category) {
    currentPage = 1;
    isLastPage = false;
    templates.clear();
    adapter.notifyDataSetChanged();
    loadTemplatesByCategory(category);
}
```

### 4. Backend Architecture

#### 4.1 Node.js Server Structure
```
backend/
├── server.js           # Entry point
├── config/
│   └── db.js          # MongoDB connection
├── models/
│   ├── Template.js    # Template schema
│   └── SharedWish.js  # SharedWish schema
├── routes/
│   ├── templates.js   # Template routes
│   └── wishes.js      # SharedWish routes
└── controllers/
    ├── templateController.js
    └── wishController.js
```

#### 4.2 MongoDB Schemas

##### Template Schema
```javascript
const templateSchema = new Schema({
    title: { type: String, required: true },
    category: { type: String, required: true },
    htmlContent: { type: String, required: true },
    cssContent: String,
    jsContent: String,
    previewUrl: String,
    status: { type: Boolean, default: true },
    categoryIcon: String,
    createdAt: { type: Date, default: Date.now },
    updatedAt: { type: Date, default: Date.now }
});
```

##### SharedWish Schema
```javascript
const sharedWishSchema = new Schema({
    shortCode: { type: String, required: true, unique: true },
    templateId: { type: Schema.Types.ObjectId, ref: 'Template' },
    recipientName: String,
    senderName: String,
    customizedHtml: { type: String, required: true },
    views: { type: Number, default: 0 },
    createdAt: { type: Date, default: Date.now },
    lastSharedAt: { type: Date, default: Date.now }
});
```

### 5. Security Considerations

#### 5.1 API Security
- Rate limiting implementation
- Input validation
- XSS prevention in templates
- CORS configuration

#### 5.2 Data Validation
- Server-side validation for all inputs
- Client-side validation for immediate feedback
- Sanitization of HTML content

### 6. Performance Optimization

#### 6.1 Caching Strategy
- In-memory template caching
- Image caching using Glide
- API response caching

#### 6.2 Lazy Loading
- Image lazy loading
- Template content lazy loading
- Category-wise data loading

### 7. Testing Strategy

#### 7.1 Unit Tests
- Repository tests
- ViewModel tests
- API service tests

#### 7.2 Integration Tests
- Template rendering tests
- API integration tests
- Database operation tests

### 8. Deployment

#### 8.1 Android App
- ProGuard rules
- API endpoint configuration
- Version management

#### 8.2 Backend
- Environment variables
- PM2 process management
- MongoDB connection pooling

## API Documentation

### Templates API

#### Get Templates
```http
GET /api/templates
Query Parameters:
- page (default: 1)
- limit (default: 20)
- category (optional)
```

#### Get Template by ID
```http
GET /api/templates/:id
```

### Shared Wishes API

#### Create Shared Wish
```http
POST /api/wishes/create
Body:
{
    "templateId": "string",
    "recipientName": "string",
    "senderName": "string",
    "customizedHtml": "string"
}
```

#### Get Shared Wish
```http
GET /api/wishes/:shortCode
```

## Architecture Overview

The app follows the MVVM (Model-View-ViewModel) architecture pattern with the following components:

### 1. UI Layer (View)

#### Activities
- `MainActivity`: Single activity that hosts all fragments
  ```java
  public class MainActivity extends AppCompatActivity {
      private ActivityMainBinding binding;
      private NavController navController;

      @Override
      protected void onCreate(Bundle savedInstanceState) {
          binding = ActivityMainBinding.inflate(getLayoutInflater());
          setContentView(binding.getRoot());
          setupNavigation();
      }
  }
  ```

#### Fragments
- `HomeFragment`: Displays template grid
- `HistoryFragment`: Shows shared wishes history
- `TemplateDetailFragment`: Template preview and customization
- `SharedWishFragment`: Displays shared wish

### 2. ViewModel Layer

#### ViewModels
- `HomeViewModel`: Manages template data and pagination
- `TemplateDetailViewModel`: Handles template customization
- `SharedWishViewModel`: Manages shared wish data

### 3. Repository Layer

#### Repositories
- `TemplateRepository`: Handles template data operations
- `SharedWishRepository`: Manages shared wish operations

### 4. Data Layer

#### API Service
- Retrofit interface for backend communication
- Response models and data classes
- Error handling and response parsing

#### Local Storage
- Room database for offline caching
- SharedPreferences for app settings
- File storage for cached images

### 5. Utils

#### Network Utils
- API client configuration
- Network state monitoring
- Response caching

#### UI Utils
- Custom views and animations
- Resource management
- Theme handling

### 6. Dependencies

#### Core Dependencies
- AndroidX Core
- Material Design
- Navigation Component
- Lifecycle Components

#### Network Dependencies
- Retrofit
- OkHttp
- Gson

#### UI Dependencies
- Glide
- Shimmer
- SwipeRefreshLayout

#### Database Dependencies
- Room
- DiskLruCache

### 7. Build Configuration

#### Gradle Setup
- Version management
- Dependency management
- Build variants

#### ProGuard Rules
- Keep rules for libraries
- Custom rules for app classes
- Resource shrinking

### 8. Testing

#### Unit Tests
- Repository tests
- ViewModel tests
- Utility class tests

#### Integration Tests
- API integration tests
- Database tests
- UI tests

### 9. Deployment

#### Release Process
- Version management
- Signing configuration
- ProGuard optimization

#### Monitoring
- Crash reporting
- Analytics
- Performance monitoring
