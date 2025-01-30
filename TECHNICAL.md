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
          navController = Navigation.findNavController(this, R.id.nav_host_fragment);
          NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
      }
  }
  ```

#### Fragments
1. `HomeFragment`
   ```java
   public class HomeFragment extends Fragment {
       private FragmentHomeBinding binding;
       private GreetingsAdapter greetingsAdapter;
       private CategoriesAdapter categoriesAdapter;

       // Key methods
       private void setupRecyclerViews() {
           binding.categoriesRecyclerView.setLayoutManager(
               new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
           binding.greetingsRecyclerView.setLayoutManager(
               new GridLayoutManager(requireContext(), 2));
       }

       private void setupSearchView() {
           binding.searchEditText.addTextChangedListener(new TextWatcher() {
               // Implement search filtering
           });
       }
   }
   ```

2. `GreetingDetailFragment`
   ```java
   public class GreetingDetailFragment extends Fragment {
       private FragmentGreetingDetailBinding binding;

       private void setupWebView() {
           String htmlContent = getGreetingHtml();
           binding.greetingPreview.loadData(htmlContent, "text/html", "UTF-8");
       }

       private void shareGreeting(String senderName, String recipientName) {
           Intent shareIntent = new Intent(Intent.ACTION_SEND);
           shareIntent.setType("text/plain");
           // Configure share intent
       }
   }
   ```

### 2. Adapters and ViewHolders

#### GreetingsAdapter
```java
public class GreetingsAdapter extends RecyclerView.Adapter<GreetingsAdapter.GreetingViewHolder> {
    private List<GreetingItem> greetings;
    private List<GreetingItem> filteredGreetings;

    public void filter(String query) {
        filteredGreetings.clear();
        if (query.isEmpty()) {
            filteredGreetings.addAll(greetings);
        } else {
            // Implement filtering logic
        }
        notifyDataSetChanged();
    }

    static class GreetingViewHolder extends RecyclerView.ViewHolder {
        private final ItemGreetingBinding binding;

        void bind(GreetingItem greeting) {
            binding.greetingTitle.setText(greeting.getTitle());
            Glide.with(binding.getRoot().getContext())
                .load(greeting.getImageUrl())
                .centerCrop()
                .into(binding.greetingImage);
        }
    }
}
```

#### CategoriesAdapter
```java
public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder> {
    private List<String> categories;
    private int selectedPosition = 0;

    public void loadMore() {
        int startPosition = categories.size();
        List<String> newCategories = getMoreCategories();
        categories.addAll(newCategories);
        notifyItemRangeInserted(startPosition, newCategories.size());
    }
}
```

### 3. Layout Implementation

#### Navigation Graph
```xml
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_graph"
    app:startDestination="@id/navigation_home">

    <fragment android:id="@+id/navigation_home"
        android:name="com.ds.eventwish.ui.home.HomeFragment">
        <action android:id="@+id/action_home_to_detail"
            app:destination="@id/navigation_greeting_detail" />
    </fragment>
</navigation>
```

#### Material Design Components
1. Search Bar
```xml
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
    android:hint="Search greetings..."
    app:startIconDrawable="@drawable/ic_search">
    <com.google.android.material.textfield.TextInputEditText
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
</com.google.android.material.textfield.TextInputLayout>
```

2. Category Chips
```xml
<com.google.android.material.chip.Chip
    style="@style/Widget.MaterialComponents.Chip.Filter"
    app:chipBackgroundColor="@color/chip_background_color"
    app:chipStrokeWidth="1dp"
    app:chipStrokeColor="@color/chip_stroke_color" />
```

3. Greeting Cards
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="12dp"
    app:cardElevation="4dp">
    <!-- Card content -->
</com.google.android.material.card.MaterialCardView>
```

### 4. Data Models

#### GreetingItem
```java
public class GreetingItem {
    private int id;
    private String title;
    private String category;
    private String imageUrl;

    // Constructor and getters
}
```

### 5. Resource Organization

#### Colors
```xml
<resources>
    <color name="primary">#FF6200EE</color>
    <color name="primary_variant">#FF3700B3</color>
    <color name="accent">#FF03DAC5</color>
    <color name="background">#FAFAFA</color>
    <color name="surface">#FFFFFF</color>
    <color name="chip_background_color">#E0E0E0</color>
    <color name="chip_stroke_color">#BDBDBD</color>
</resources>
```

#### Vector Drawables
- `ic_home.xml`
- `ic_history.xml`
- `ic_reminder.xml`
- `ic_more.xml`
- `ic_search.xml`
- `ic_back.xml`
- `ic_share.xml`
- `ic_add.xml`

### 6. Best Practices Implementation

#### View Binding
```java
// In Fragment
private FragmentHomeBinding binding;

@Override
public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, 
                        Bundle savedInstanceState) {
    binding = FragmentHomeBinding.inflate(inflater, container, false);
    return binding.getRoot();
}

@Override
public void onDestroyView() {
    super.onDestroyView();
    binding = null;
}
```

#### RecyclerView Optimization
```java
// In HomeFragment
private void setupRecyclerViews() {
    binding.greetingsRecyclerView.setHasFixedSize(true);
    binding.greetingsRecyclerView.setItemViewCacheSize(20);
    binding.greetingsRecyclerView.setDrawingCacheEnabled(true);
    binding.greetingsRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
}
```

### 7. Testing Guidelines

#### Unit Tests
```java
@Test
public void filterGreetings_withEmptyQuery_showsAllItems() {
    GreetingsAdapter adapter = new GreetingsAdapter();
    adapter.filter("");
    assertEquals(adapter.getItemCount(), adapter.getAllItems().size());
}
```

#### UI Tests
```java
@Test
public void clickGreeting_navigatesToDetail() {
    onView(withId(R.id.greetingsRecyclerView))
        .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
    intended(hasComponent(GreetingDetailFragment.class.getName()));
}
```

### 8. Security Considerations

1. Input Validation
```java
private boolean validateInput() {
    String senderName = binding.senderNameEdit.getText().toString();
    String recipientName = binding.recipientNameEdit.getText().toString();
    
    if (senderName.isEmpty()) {
        binding.senderNameLayout.setError("Please enter your name");
        return false;
    }
    // More validation...
    return true;
}
```

2. WebView Security
```java
private void setupWebView() {
    WebSettings settings = binding.greetingPreview.getSettings();
    settings.setJavaScriptEnabled(false);
    settings.setAllowFileAccess(false);
    settings.setSaveFormData(false);
}
```

### 9. Performance Optimization

1. Image Loading
```java
Glide.with(context)
    .load(imageUrl)
    .diskCacheStrategy(DiskCacheStrategy.ALL)
    .placeholder(R.drawable.placeholder)
    .error(R.drawable.error)
    .into(imageView);
```

2. Layout Optimization
```xml
<merge xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <!-- Merged layouts for better performance -->
</merge>
```

### 10. Deployment Checklist

1. ProGuard Rules
```proguard
-keep class com.ds.eventwish.models.** { *; }
-keepclassmembers class com.ds.eventwish.ui.** {
    public <init>();
}
```

2. Version Management
```gradle
android {
    defaultConfig {
        versionCode 1
        versionName "1.0"
    }
}
```

## Implementation Guidelines

### 1. Fragment Creation
```java
public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
}
```

### 2. RecyclerView Setup
```java
private void setupGreetingsRecyclerView() {
    GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
    binding.greetingsRecyclerView.setLayoutManager(layoutManager);
    // Add item decoration for spacing
    int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
    binding.greetingsRecyclerView.addItemDecoration(new GridSpacingItemDecoration(2, spacing, true));
}
```

### 3. Navigation Implementation
```java
private void navigateToDetail(int greetingId) {
    HomeFragmentDirections.ActionHomeToDetail action = 
        HomeFragmentDirections.actionHomeToDetail(greetingId);
    Navigation.findNavController(view).navigate(action);
}
```

### 4. View Binding Usage
```java
// In Activity
private ActivityMainBinding binding;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
}
```

## Best Practices

### 1. Resource Naming
- Layouts: `fragment_*`, `activity_*`, `item_*`
- IDs: `<component_type>_<description>`
- Drawables: `ic_*`, `bg_*`
- Colors: Descriptive names like `primary`, `surface`

### 2. Code Organization
- Keep fragments focused on UI logic
- Use ViewModels for business logic
- Separate data operations into repositories
- Use dependency injection when possible

### 3. Performance Considerations
- Use ViewBinding instead of findViewById
- Implement view holder pattern in adapters
- Use proper image loading with Glide
- Implement pagination for large lists

### 4. Error Handling
- Implement proper error states
- Show meaningful error messages
- Handle configuration changes
- Implement proper lifecycle management

## Testing Guidelines

### 1. Unit Tests
- Test ViewModels
- Test data transformations
- Test business logic

### 2. UI Tests
- Test navigation flows
- Test user interactions
- Test error states

### 3. Integration Tests
- Test database operations
- Test network calls
- Test component interactions

## Security Considerations

1. Input Validation
2. Data Encryption
3. Secure File Storage
4. Network Security
5. Content Provider Security

## Deployment Checklist

1. ProGuard Rules
2. API Keys Management
3. Version Code/Name
4. Build Variants
5. Release Signing
