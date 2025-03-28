# Flutter Conversion Guide for EventWish

This guide provides a comprehensive plan for converting the EventWish application from Java/Android to Flutter while maintaining the existing backend API integration and monetization system.

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture & State Management](#architecture--state-management)
3. [Backend API Integration](#backend-api-integration)
4. [Database & Local Storage](#database--local-storage)
5. [Monetization System](#monetization-system)
6. [UI Components](#ui-components)
7. [Deep Linking & Navigation](#deep-linking--navigation)
8. [Firebase Integration](#firebase-integration)
9. [Testing](#testing)
10. [Deployment](#deployment)

## Project Overview

EventWish is a greeting card creation app with the following key features:
- Template browsing and customization
- HTML-based greeting card rendering
- Sharing capabilities
- Monetization through coins and ads
- Feature unlocking (premium features like HTML editing)
- Festival notifications and reminders
- Deep linking support
- Analytics tracking

## Architecture & State Management

### Suggested Architecture

For the Flutter version, we'll implement a clean architecture with:

1. **Presentation Layer**: Flutter UI components and screens
2. **Domain Layer**: Business logic and use cases
3. **Data Layer**: Repositories, models, and data sources

### State Management

Recommended approach: **Riverpod** (a modern alternative to Provider)
- Provides reactive state management
- Easy dependency injection
- Testable and maintainable

Alternatives: GetX, Bloc, or Provider depending on team preference

### Folder Structure

```
lib/
├── core/
│   ├── constants/              # App constants
│   ├── errors/                 # Error handling
│   ├── network/               # Network helpers
│   └── utils/                 # Utility functions
├── data/
│   ├── datasources/
│   │   ├── local/             # Local storage/database
│   │   └── remote/            # API clients
│   ├── models/                # Data models
│   └── repositories/          # Repository implementations
├── domain/
│   ├── entities/              # Business entities
│   ├── repositories/          # Repository interfaces
│   └── usecases/             # Business logic
├── presentation/
│   ├── common/                # Shared UI components
│   ├── pages/                 # App screens
│   │   ├── home/
│   │   ├── customize/
│   │   ├── coins/            # Monetization UI
│   │   ├── festival/         # Festival notifications
│   │   ├── reminder/         # Reminders
│   │   └── shared_wish/      # Shared wish view
│   └── providers/            # State management
└── main.dart
```

### Key Components

1. **Core Layer**
   - Network client configuration
   - Error handling utilities
   - Common utilities and extensions
   - Constants and configurations

2. **Data Layer**
   - API clients for backend communication
   - Local database implementations
   - Repository implementations
   - Data models and DTOs

3. **Domain Layer**
   - Business entities
   - Repository interfaces
   - Use cases for business logic
   - Value objects and failures

4. **Presentation Layer**
   - UI components and screens
   - State management providers
   - Navigation logic
   - Common widgets

### State Management with Riverpod

Example provider setup:

```dart
// Template provider
final templateProvider = StateNotifierProvider<TemplateNotifier, TemplateState>((ref) {
  return TemplateNotifier(ref.watch(templateRepositoryProvider));
});

class TemplateNotifier extends StateNotifier<TemplateState> {
  final TemplateRepository _repository;
  
  TemplateNotifier(this._repository) : super(TemplateState.initial());
  
  Future<void> loadTemplates({
    String? category,
    String? sortBy,
    String? timeFilter,
  }) async {
    state = state.copyWith(isLoading: true);
    
    try {
      final templates = await _repository.getTemplates(
        category: category,
        sortBy: sortBy,
        timeFilter: timeFilter,
      );
      state = state.copyWith(
        templates: templates,
        isLoading: false,
        error: null,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString(),
      );
    }
  }
  
  void setCategory(String? category) {
    if (category != state.selectedCategory) {
      state = state.copyWith(selectedCategory: category);
      loadTemplates(category: category);
    }
  }
  
  void setSortOption(String sortBy) {
    state = state.copyWith(sortOption: sortBy);
    loadTemplates(
      category: state.selectedCategory,
      sortBy: sortBy,
    );
  }
}

// Template state
class TemplateState {
  final List<Template> templates;
  final bool isLoading;
  final String? error;
  final String? selectedCategory;
  final String? sortOption;
  final String? timeFilter;
  
  TemplateState({
    required this.templates,
    required this.isLoading,
    this.error,
    this.selectedCategory,
    this.sortOption,
    this.timeFilter,
  });
  
  factory TemplateState.initial() {
    return TemplateState(
      templates: [],
      isLoading: false,
    );
  }
  
  TemplateState copyWith({
    List<Template>? templates,
    bool? isLoading,
    String? error,
    String? selectedCategory,
    String? sortOption,
    String? timeFilter,
  }) {
    return TemplateState(
      templates: templates ?? this.templates,
      isLoading: isLoading ?? this.isLoading,
      error: error,
      selectedCategory: selectedCategory ?? this.selectedCategory,
      sortOption: sortOption ?? this.sortOption,
      timeFilter: timeFilter ?? this.timeFilter,
    );
  }
}
```

## UI Components

### Screen Implementations

#### 1. Home Screen (HomeFragment)

The home screen is the main entry point of the application, displaying template categories and a grid of templates.

```dart
class HomeScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Consumer2<TemplateProvider, CoinProvider>(
      builder: (context, templateProvider, coinProvider, _) {
        return Scaffold(
          appBar: AppBar(
            title: Text('EventWish'),
            actions: [
              IconButton(
                icon: Badge(
                  label: Text('${coinProvider.coins}'),
                  child: Icon(Icons.monetization_on),
                ),
                onPressed: () => showDialog(
                  context: context,
                  builder: (_) => AdRewardDialog(),
                ),
              ),
            ],
          ),
          body: Column(
            children: [
              // Categories list
              CategoryList(
                categories: templateProvider.categories,
                selectedCategory: templateProvider.selectedCategory,
                onCategorySelected: (category) {
                  templateProvider.setCategory(category);
                },
              ),
              
              // Filter chips
              FilterChips(
                sortOption: templateProvider.sortOption,
                timeFilter: templateProvider.timeFilter,
                onSortOptionChanged: templateProvider.setSortOption,
                onTimeFilterChanged: templateProvider.setTimeFilter,
              ),
              
              // Templates grid
              Expanded(
                child: templateProvider.isLoading
                    ? ShimmerTemplateGrid()
                    : RefreshIndicator(
                        onRefresh: () => templateProvider.refreshTemplates(),
                        child: TemplateGrid(
                          templates: templateProvider.templates,
                          onTemplateSelected: (template) {
                            context.pushNamed(
                              'template_detail',
                              params: {'id': template.id},
                            );
                          },
                        ),
                      ),
              ),
            ],
          ),
        );
      },
    );
  }
}

// Category list with horizontal scrolling
class CategoryList extends StatelessWidget {
  final List<Category> categories;
  final Category? selectedCategory;
  final ValueChanged<Category> onCategorySelected;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 50,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        itemCount: categories.length,
        itemBuilder: (context, index) {
          final category = categories[index];
          return CategoryChip(
            category: category,
            isSelected: category == selectedCategory,
            onSelected: () => onCategorySelected(category),
          );
        },
      ),
    );
  }
}

// Template grid with responsive layout
class TemplateGrid extends StatelessWidget {
  final List<Template> templates;
  final ValueChanged<Template> onTemplateSelected;

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        childAspectRatio: 0.75,
        mainAxisSpacing: 8,
        crossAxisSpacing: 8,
      ),
      padding: EdgeInsets.all(8),
      itemCount: templates.length,
      itemBuilder: (context, index) {
        final template = templates[index];
        return TemplateCard(
          template: template,
          onTap: () => onTemplateSelected(template),
        );
      },
    );
  }
}

// Template card with shimmer loading effect
class TemplateCard extends StatelessWidget {
  final Template template;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Expanded(
              child: CachedNetworkImage(
                imageUrl: template.thumbnailUrl,
                fit: BoxFit.cover,
                placeholder: (context, url) => ShimmerLoading(),
                errorWidget: (context, url, error) => Icon(Icons.error),
              ),
            ),
            Padding(
              padding: EdgeInsets.all(8),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    template.name,
                    style: Theme.of(context).textTheme.titleMedium,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  if (template.category != null)
                    Text(
                      template.category!,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// Shimmer loading effect for templates
class ShimmerTemplateGrid extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        childAspectRatio: 0.75,
        mainAxisSpacing: 8,
        crossAxisSpacing: 8,
      ),
      padding: EdgeInsets.all(8),
      itemCount: 6, // Show 6 shimmer items
      itemBuilder: (context, index) {
        return Card(
          clipBehavior: Clip.antiAlias,
          child: Shimmer.fromColors(
            baseColor: Colors.grey[300]!,
            highlightColor: Colors.grey[100]!,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(
                  child: Container(color: Colors.white),
                ),
                Padding(
                  padding: EdgeInsets.all(8),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        height: 16,
                        width: double.infinity,
                        color: Colors.white,
                      ),
                      SizedBox(height: 4),
                      Container(
                        height: 12,
                        width: 80,
                        color: Colors.white,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

#### 2. Template Detail Screen (TemplateDetailFragment)

The template detail screen allows users to customize and preview templates.

```dart
class TemplateDetailScreen extends StatefulWidget {
  final String templateId;

  @override
  State<TemplateDetailScreen> createState() => _TemplateDetailScreenState();
}

class _TemplateDetailScreenState extends State<TemplateDetailScreen> {
  final TextEditingController recipientController = TextEditingController();
  final TextEditingController senderController = TextEditingController();
  final WebViewController webViewController = WebViewController();
  
  @override
  void initState() {
    super.initState();
    context.read<TemplateDetailProvider>().loadTemplate(widget.templateId);
    setupWebView();
  }
  
  void setupWebView() {
    webViewController
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(Colors.transparent)
      ..addJavaScriptChannel(
        'Flutter',
        onMessageReceived: (message) {
          // Handle messages from WebView
          print('Received message from WebView: ${message.message}');
        },
      );
  }
  
  @override
  Widget build(BuildContext context) {
    return Consumer<TemplateDetailProvider>(
      builder: (context, provider, _) {
        return Scaffold(
          appBar: AppBar(
            title: Text('Customize Template'),
            actions: [
              IconButton(
                icon: Icon(Icons.share),
                onPressed: provider.canShare ? _shareTemplate : null,
              ),
            ],
          ),
          body: provider.isLoading
              ? Center(child: CircularProgressIndicator())
              : Column(
                  children: [
                    // Template preview
                    Expanded(
                      child: WebViewWidget(
                        controller: webViewController,
                      ),
                    ),
                    
                    // Input fields
                    Card(
                      margin: EdgeInsets.all(8),
                      child: Padding(
                        padding: EdgeInsets.all(16),
                        child: Column(
                          children: [
                            TextField(
                              controller: recipientController,
                              decoration: InputDecoration(
                                labelText: 'Recipient Name',
                                prefixIcon: Icon(Icons.person_outline),
                                border: OutlineInputBorder(),
                              ),
                              onChanged: (value) => _updatePreview(),
                            ),
                            SizedBox(height: 8),
                            TextField(
                              controller: senderController,
                              decoration: InputDecoration(
                                labelText: 'Sender Name',
                                prefixIcon: Icon(Icons.person),
                                border: OutlineInputBorder(),
                              ),
                              onChanged: (value) => _updatePreview(),
                            ),
                            SizedBox(height: 16),
                            ElevatedButton.icon(
                              icon: Icon(Icons.share),
                              label: Text('Share Wish'),
                              onPressed: provider.canShare ? _shareTemplate : null,
                              style: ElevatedButton.styleFrom(
                                minimumSize: Size(double.infinity, 48),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
        );
      },
    );
  }
  
  void _updatePreview() {
    final provider = context.read<TemplateDetailProvider>();
    provider.updateNames(
      recipientName: recipientController.text,
      senderName: senderController.text,
    );
    
    final html = provider.getCustomizedHtml();
    webViewController.loadHtmlString(html);
  }
  
  void _shareTemplate() async {
    final provider = context.read<TemplateDetailProvider>();
    final shortCode = await provider.saveWish();
    if (shortCode != null) {
      context.pushNamed('shared_wish', params: {'code': shortCode});
    }
  }
  
  @override
  void dispose() {
    recipientController.dispose();
    senderController.dispose();
    super.dispose();
  }
}
```

#### 3. Shared Wish Screen (SharedWishFragment)

The shared wish screen displays a shared greeting card and provides sharing options.

```dart
class SharedWishScreen extends StatefulWidget {
  final String shortCode;

  @override
  State<SharedWishScreen> createState() => _SharedWishScreenState();
}

class _SharedWishScreenState extends State<SharedWishScreen> {
  final WebViewController webViewController = WebViewController();
  
  @override
  void initState() {
    super.initState();
    setupWebView();
    loadWish();
  }
  
  void setupWebView() {
    webViewController
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(Colors.transparent);
  }
  
  void loadWish() {
    context.read<SharedWishProvider>().loadWish(widget.shortCode);
  }
  
  @override
  Widget build(BuildContext context) {
    return Consumer<SharedWishProvider>(
      builder: (context, provider, _) {
        return Scaffold(
          appBar: AppBar(
            title: Text('Shared Wish'),
            actions: [
              IconButton(
                icon: Icon(Icons.share),
                onPressed: () => _showShareOptions(context),
              ),
              IconButton(
                icon: Icon(Icons.analytics),
                onPressed: provider.analytics != null
                    ? () => _showAnalytics(context, provider.analytics!)
                    : null,
              ),
            ],
          ),
          body: provider.isLoading
              ? Center(child: CircularProgressIndicator())
              : Column(
                  children: [
                    Expanded(
                      child: WebViewWidget(
                        controller: webViewController,
                      ),
                    ),
                    if (provider.analytics != null)
                      AnalyticsBar(analytics: provider.analytics!),
                  ],
                ),
        );
      },
    );
  }
  
  void _showShareOptions(BuildContext context) {
    showModalBottomSheet(
      context: context,
      builder: (context) => ShareOptionsSheet(
        shortCode: widget.shortCode,
        onPlatformSelected: (platform) {
          context.read<SharedWishProvider>().trackShare(platform);
        },
      ),
    );
  }
  
  void _showAnalytics(BuildContext context, Analytics analytics) {
    showModalBottomSheet(
      context: context,
      builder: (context) => AnalyticsSheet(analytics: analytics),
    );
  }
}

class ShareOptionsSheet extends StatelessWidget {
  final String shortCode;
  final ValueChanged<String> onPlatformSelected;
  
  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        ListTile(
          leading: Icon(Icons.whatsapp),
          title: Text('WhatsApp'),
          onTap: () => _shareVia('whatsapp'),
        ),
        ListTile(
          leading: Icon(Icons.facebook),
          title: Text('Facebook'),
          onTap: () => _shareVia('facebook'),
        ),
        ListTile(
          leading: Icon(Icons.link),
          title: Text('Copy Link'),
          onTap: () => _copyLink(context),
        ),
      ],
    );
  }
  
  void _shareVia(String platform) {
    onPlatformSelected(platform);
    // Implement platform-specific sharing
  }
  
  void _copyLink(BuildContext context) {
    final url = 'https://eventwishes.onrender.com/wish/$shortCode';
    Clipboard.setData(ClipboardData(text: url));
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Link copied to clipboard')),
    );
    onPlatformSelected('clipboard');
  }
}

class AnalyticsSheet extends StatelessWidget {
  final Analytics analytics;
  
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.all(16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            'Analytics',
            style: Theme.of(context).textTheme.titleLarge,
          ),
          SizedBox(height: 16),
          _buildStatRow(
            context,
            'Views',
            analytics.views.toString(),
            Icons.visibility,
          ),
          _buildStatRow(
            context,
            'Unique Views',
            analytics.uniqueViews.toString(),
            Icons.person,
          ),
          _buildStatRow(
            context,
            'Shares',
            analytics.shareCount.toString(),
            Icons.share,
          ),
          if (analytics.platformBreakdown.isNotEmpty) ...[
            SizedBox(height: 16),
            Text(
              'Platform Breakdown',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            SizedBox(height: 8),
            ...analytics.platformBreakdown.entries.map(
              (entry) => _buildStatRow(
                context,
                entry.key,
                entry.value.toString(),
                _getPlatformIcon(entry.key),
              ),
            ),
          ],
        ],
      ),
    );
  }
  
  Widget _buildStatRow(
    BuildContext context,
    String label,
    String value,
    IconData icon,
  ) {
    return Padding(
      padding: EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Icon(icon),
          SizedBox(width: 8),
          Text(label),
          Spacer(),
          Text(
            value,
            style: Theme.of(context).textTheme.titleMedium,
          ),
        ],
      ),
    );
  }
  
  IconData _getPlatformIcon(String platform) {
    switch (platform.toLowerCase()) {
      case 'whatsapp':
        return Icons.whatsapp;
      case 'facebook':
        return Icons.facebook;
      case 'twitter':
        return Icons.twitter;
      case 'email':
        return Icons.email;
      case 'sms':
        return Icons.sms;
      case 'clipboard':
        return Icons.link;
      default:
        return Icons.share;
    }
  }
}

#### 4. Resource Screen (ResourceFragment)

The resource screen provides a full-screen view of templates and resources.

```dart
class ResourceScreen extends StatefulWidget {
  final String shortCode;

  @override
  State<ResourceScreen> createState() => _ResourceScreenState();
}

class _ResourceScreenState extends State<ResourceScreen> {
  final WebViewController webViewController = WebViewController();
  bool isFullScreen = true;
  
  @override
  void initState() {
    super.initState();
    setupWebView();
    loadResource();
  }
  
  void setupWebView() {
    webViewController
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(Colors.transparent);
  }
  
  void loadResource() {
    context.read<ResourceProvider>().loadResource(widget.shortCode);
  }
  
  @override
  Widget build(BuildContext context) {
    return Consumer<ResourceProvider>(
      builder: (context, provider, _) {
        return Scaffold(
          appBar: isFullScreen ? null : AppBar(
            title: Text('Resource'),
            backgroundColor: Colors.transparent,
            elevation: 0,
          ),
          extendBodyBehindAppBar: true,
          body: GestureDetector(
            onTap: _toggleUI,
            child: Stack(
              children: [
                WebViewWidget(
                  controller: webViewController,
                ),
                if (!isFullScreen)
                  SafeArea(
                    child: Align(
                      alignment: Alignment.topRight,
                      child: Padding(
                        padding: EdgeInsets.all(8),
                        child: IconButton(
                          icon: Icon(
                            isFullScreen
                                ? Icons.fullscreen_exit
                                : Icons.fullscreen,
                            color: Colors.white,
                          ),
                          onPressed: _toggleFullScreen,
                        ),
                      ),
                    ),
                  ),
                if (provider.isLoading)
                  Center(
                    child: CircularProgressIndicator(),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }
  
  void _toggleUI() {
    setState(() {
      isFullScreen = !isFullScreen;
    });
  }
  
  void _toggleFullScreen() {
    SystemChrome.setEnabledSystemUIMode(
      isFullScreen ? SystemUiMode.immersive : SystemUiMode.edgeToEdge,
    );
    setState(() {
      isFullScreen = !isFullScreen;
    });
  }
  
  @override
  void dispose() {
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    super.dispose();
  }
}
```

#### 5. Festival Notification Screen (FestivalNotificationFragment)

The festival notification screen displays upcoming festivals and their templates.

```dart
class FestivalNotificationScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Consumer<FestivalProvider>(
      builder: (context, provider, _) {
        return Scaffold(
          appBar: AppBar(
            title: Text('Festivals'),
          ),
          body: provider.isLoading
              ? ShimmerFestivalList()
              : RefreshIndicator(
                  onRefresh: () => provider.refreshFestivals(),
                  child: ListView.builder(
                    itemCount: provider.festivals.length,
                    itemBuilder: (context, index) {
                      final festival = provider.festivals[index];
                      return FestivalCard(
                        festival: festival,
                        onTemplateSelected: (template) {
                          context.pushNamed(
                            'template_detail',
                            params: {'id': template.id},
                          );
                        },
                      );
                    },
                  ),
                ),
        );
      },
    );
  }
}

class FestivalCard extends StatelessWidget {
  final Festival festival;
  final ValueChanged<Template> onTemplateSelected;
  
  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          ListTile(
            leading: CategoryIcon(category: festival.category),
            title: Text(
              festival.name,
              style: Theme.of(context).textTheme.titleLarge,
            ),
            subtitle: Text(festival.description),
          ),
          Padding(
            padding: EdgeInsets.symmetric(horizontal: 16),
            child: CountdownTimer(date: festival.date),
          ),
          if (festival.templates.isNotEmpty) ...[
            Padding(
              padding: EdgeInsets.all(16),
              child: Text(
                'Templates',
                style: Theme.of(context).textTheme.titleMedium,
              ),
            ),
            SizedBox(
              height: 180,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                padding: EdgeInsets.symmetric(horizontal: 16),
                itemCount: festival.templates.length,
                itemBuilder: (context, index) {
                  final template = festival.templates[index];
                  return Padding(
                    padding: EdgeInsets.only(right: 8),
                    child: AspectRatio(
                      aspectRatio: 0.75,
                      child: TemplateCard(
                        template: template,
                        onTap: () => onTemplateSelected(template),
                      ),
                    ),
                  );
                },
              ),
            ),
            SizedBox(height: 16),
          ],
        ],
      ),
    );
  }
}

class CountdownTimer extends StatefulWidget {
  final DateTime date;
  
  @override
  State<CountdownTimer> createState() => _CountdownTimerState();
}

class _CountdownTimerState extends State<CountdownTimer> {
  Timer? timer;
  Duration? remaining;
  
  @override
  void initState() {
    super.initState();
    _startTimer();
  }
  
  void _startTimer() {
    timer = Timer.periodic(Duration(seconds: 1), (_) {
      setState(() {
        remaining = widget.date.difference(DateTime.now());
      });
    });
  }
  
  @override
  Widget build(BuildContext context) {
    if (remaining == null || remaining!.isNegative) {
      return Text(
        'Event has passed',
        style: TextStyle(color: Colors.red),
      );
    }
    
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Coming in:',
          style: Theme.of(context).textTheme.bodyMedium,
        ),
        SizedBox(height: 4),
        Row(
          children: [
            _buildTimeUnit('${remaining!.inDays}', 'DAYS'),
            SizedBox(width: 8),
            _buildTimeUnit('${remaining!.inHours % 24}', 'HOURS'),
            SizedBox(width: 8),
            _buildTimeUnit('${remaining!.inMinutes % 60}', 'MINS'),
          ],
        ),
      ],
    );
  }
  
  Widget _buildTimeUnit(String value, String label) {
    return Column(
      children: [
        Container(
          padding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          decoration: BoxDecoration(
            color: Theme.of(context).primaryColor,
            borderRadius: BorderRadius.circular(4),
          ),
          child: Text(
            value,
            style: TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
        SizedBox(height: 4),
        Text(
          label,
          style: Theme.of(context).textTheme.bodySmall,
        ),
      ],
    );
  }
  
  @override
  void dispose() {
    timer?.cancel();
    super.dispose();
  }
}

#### 6. Reminder Screen (ReminderFragment)

The reminder screen manages user-created reminders with notifications.

```dart
class ReminderScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Consumer<ReminderProvider>(
      builder: (context, provider, _) {
        return Scaffold(
          appBar: AppBar(
            title: Text('Reminders'),
            actions: [
              IconButton(
                icon: Icon(Icons.delete_sweep),
                onPressed: () => _showClearAllDialog(context),
              ),
            ],
          ),
          body: Column(
            children: [
              ReminderStats(
                todayCount: provider.todayCount,
                upcomingCount: provider.upcomingCount,
              ),
              Expanded(
                child: provider.reminders.isEmpty
                    ? EmptyReminders()
                    : ReminderList(
                        reminders: provider.reminders,
                        onReminderTapped: (reminder) {
                          _showReminderDetails(context, reminder);
                        },
                        onReminderCompleted: provider.toggleReminderCompleted,
                        onReminderDeleted: provider.deleteReminder,
                      ),
              ),
            ],
          ),
          floatingActionButton: FloatingActionButton(
            child: Icon(Icons.add),
            onPressed: () => _showAddReminderDialog(context),
          ),
        );
      },
    );
  }
  
  void _showAddReminderDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AddReminderDialog(),
    );
  }
  
  void _showClearAllDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Clear All Reminders'),
        content: Text('Are you sure you want to delete all reminders?'),
        actions: [
          TextButton(
            child: Text('Cancel'),
            onPressed: () => Navigator.pop(context),
          ),
          TextButton(
            child: Text('Clear All'),
            onPressed: () {
              context.read<ReminderProvider>().clearAllReminders();
              Navigator.pop(context);
            },
          ),
        ],
      ),
    );
  }
}

class ReminderList extends StatelessWidget {
  final List<Reminder> reminders;
  final ValueChanged<Reminder> onReminderTapped;
  final ValueChanged<Reminder> onReminderCompleted;
  final ValueChanged<Reminder> onReminderDeleted;
  
  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      itemCount: reminders.length,
      itemBuilder: (context, index) {
        final reminder = reminders[index];
        return Dismissible(
          key: Key(reminder.id.toString()),
          background: Container(
            color: Colors.green,
            alignment: Alignment.centerLeft,
            padding: EdgeInsets.only(left: 16),
            child: Icon(Icons.check, color: Colors.white),
          ),
          secondaryBackground: Container(
            color: Colors.red,
            alignment: Alignment.centerRight,
            padding: EdgeInsets.only(right: 16),
            child: Icon(Icons.delete, color: Colors.white),
          ),
          onDismissed: (direction) {
            if (direction == DismissDirection.startToEnd) {
              onReminderCompleted(reminder);
            } else {
              onReminderDeleted(reminder);
            }
          },
          child: ReminderTile(
            reminder: reminder,
            onTap: () => onReminderTapped(reminder),
          ),
        );
      },
    );
  }
}

class ReminderTile extends StatelessWidget {
  final Reminder reminder;
  final VoidCallback onTap;
  
  @override
  Widget build(BuildContext context) {
    final isOverdue = reminder.dateTime.isBefore(DateTime.now()) &&
        !reminder.isCompleted;
    
    return ListTile(
      onTap: onTap,
      leading: CircleAvatar(
        backgroundColor: _getPriorityColor(reminder.priority),
        child: Icon(
          reminder.isCompleted ? Icons.check : Icons.notifications,
          color: Colors.white,
        ),
      ),
      title: Text(
        reminder.title,
        style: TextStyle(
          decoration: reminder.isCompleted
              ? TextDecoration.lineThrough
              : null,
        ),
      ),
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (reminder.description.isNotEmpty)
            Text(
              reminder.description,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          Text(
            DateFormat('MMM dd, yyyy HH:mm').format(reminder.dateTime),
            style: TextStyle(
              color: isOverdue ? Colors.red : null,
            ),
          ),
        ],
      ),
      trailing: reminder.isRepeating
          ? Icon(Icons.repeat)
          : null,
    );
  }
  
  Color _getPriorityColor(Priority priority) {
    switch (priority) {
      case Priority.high:
        return Colors.red;
      case Priority.medium:
        return Colors.orange;
      case Priority.low:
        return Colors.green;
    }
  }
}

class AddReminderDialog extends StatefulWidget {
  @override
  State<AddReminderDialog> createState() => _AddReminderDialogState();
}

class _AddReminderDialogState extends State<AddReminderDialog> {
  final titleController = TextEditingController();
  final descriptionController = TextEditingController();
  final repeatIntervalController = TextEditingController();
  DateTime selectedDate = DateTime.now();
  TimeOfDay selectedTime = TimeOfDay.now();
  Priority selectedPriority = Priority.medium;
  bool isRepeating = false;
  
  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text('Add Reminder'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: titleController,
              decoration: InputDecoration(
                labelText: 'Title',
                border: OutlineInputBorder(),
              ),
            ),
            SizedBox(height: 8),
            TextField(
              controller: descriptionController,
              decoration: InputDecoration(
                labelText: 'Description',
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
            ),
            SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    icon: Icon(Icons.calendar_today),
                    label: Text(DateFormat('MMM dd, yyyy')
                        .format(selectedDate)),
                    onPressed: () => _selectDate(context),
                  ),
                ),
                SizedBox(width: 8),
                Expanded(
                  child: OutlinedButton.icon(
                    icon: Icon(Icons.access_time),
                    label: Text(selectedTime.format(context)),
                    onPressed: () => _selectTime(context),
                  ),
                ),
              ],
            ),
            SizedBox(height: 16),
            Row(
              children: Priority.values.map((priority) {
                return Expanded(
                  child: RadioListTile<Priority>(
                    title: Text(priority.name),
                    value: priority,
                    groupValue: selectedPriority,
                    onChanged: (value) {
                      setState(() {
                        selectedPriority = value!;
                      });
                    },
                  ),
                );
              }).toList(),
            ),
            SwitchListTile(
              title: Text('Repeat'),
              value: isRepeating,
              onChanged: (value) {
                setState(() {
                  isRepeating = value;
                });
              },
            ),
            if (isRepeating)
              TextField(
                controller: repeatIntervalController,
                decoration: InputDecoration(
                  labelText: 'Repeat Interval (days)',
                  border: OutlineInputBorder(),
                ),
                keyboardType: TextInputType.number,
              ),
          ],
        ),
      ),
      actions: [
        TextButton(
          child: Text('Cancel'),
          onPressed: () => Navigator.pop(context),
        ),
        ElevatedButton(
          child: Text('Save'),
          onPressed: () => _saveReminder(context),
        ),
      ],
    );
  }
  
  Future<void> _selectDate(BuildContext context) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: selectedDate,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(Duration(days: 365)),
    );
    if (picked != null) {
      setState(() {
        selectedDate = picked;
      });
    }
  }
  
  Future<void> _selectTime(BuildContext context) async {
    final picked = await showTimePicker(
      context: context,
      initialTime: selectedTime,
    );
    if (picked != null) {
      setState(() {
        selectedTime = picked;
      });
    }
  }
  
  void _saveReminder(BuildContext context) {
    if (titleController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Title is required')),
      );
      return;
    }
    
    final dateTime = DateTime(
      selectedDate.year,
      selectedDate.month,
      selectedDate.day,
      selectedTime.hour,
      selectedTime.minute,
    );
    
    if (dateTime.isBefore(DateTime.now())) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Please select a future date and time')),
      );
      return;
    }
    
    int? repeatInterval;
    if (isRepeating) {
      repeatInterval = int.tryParse(repeatIntervalController.text);
      if (repeatInterval == null || repeatInterval <= 0) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Please enter a valid repeat interval')),
        );
        return;
      }
    }
    
    final reminder = Reminder(
      title: titleController.text,
      description: descriptionController.text,
      dateTime: dateTime,
      priority: selectedPriority,
      isRepeating: isRepeating,
      repeatInterval: repeatInterval ?? 0,
    );
    
    context.read<ReminderProvider>().addReminder(reminder);
    Navigator.pop(context);
  }
  
  @override
  void dispose() {
    titleController.dispose();
    descriptionController.dispose();
    repeatIntervalController.dispose();
    super.dispose();
  }
}
```

### Deep Linking Implementation

```dart
class DeepLinkService {
  static final DeepLinkService _instance = DeepLinkService._();
  factory DeepLinkService() => _instance;
  DeepLinkService._();
  
  static const String webUrlBase = 'https://eventwishes.onrender.com';
  static const String appScheme = 'eventwish';
  
  Future<void> initDeepLinks() async {
    // Handle app links
    uriLinkStream.listen((Uri? uri) {
      if (uri != null) {
        handleDeepLink(uri);
      }
    }, onError: (err) {
      print('Deep link error: $err');
    });
    
    // Get initial link
    try {
      final initialUri = await getInitialUri();
      if (initialUri != null) {
        handleDeepLink(initialUri);
      }
    } catch (e) {
      print('Error getting initial URI: $e');
    }
  }
  
  void handleDeepLink(Uri uri) {
    final pathSegments = uri.pathSegments;
    if (pathSegments.isEmpty) return;
    
    switch (pathSegments[0]) {
      case 'wish':
        if (pathSegments.length > 1) {
          final shortCode = pathSegments[1];
          router.pushNamed('shared_wish', params: {'code': shortCode});
        }
        break;
      case 'template':
        if (pathSegments.length > 1) {
          final templateId = pathSegments[1];
          router.pushNamed('template_detail', params: {'id': templateId});
        }
        break;
      case 'festival':
        if (pathSegments.length > 1) {
          final festivalId = pathSegments[1];
          router.pushNamed('festival_detail', params: {'id': festivalId});
        }
        break;
    }
  }
  
  String generateWebUrl(String type, String id) {
    return '$webUrlBase/$type/$id';
  }
  
  String generateAppUrl(String type, String id) {
    return '$appScheme://$type/$id';
  }
  
  void shareWish(String shortCode) {
    final url = generateWebUrl('wish', shortCode);
    Share.share(url);
  }
  
  void copyToClipboard(String text) {
    Clipboard.setData(ClipboardData(text: text));
  }
}
```

### Models

#### 1. Category Icon Model

```dart
class CategoryIcon {
  final String id;
  final String category;
  final String categoryIcon;
  
  CategoryIcon({
    required this.id,
    required this.category,
    required this.categoryIcon,
  });
  
  factory CategoryIcon.fromJson(Map<String, dynamic> json) {
    if (json is String) {
      // Handle case where json is just the icon URL
      return CategoryIcon(
        id: '',
        category: '',
        categoryIcon: json,
      );
    }
    
    return CategoryIcon(
      id: json['_id'] ?? '',
      category: json['category'] ?? '',
      categoryIcon: json['categoryIcon'] ?? '',
    );
  }
  
  Map<String, dynamic> toJson() => {
    '_id': id,
    'category': category,
    'categoryIcon': categoryIcon,
  };
}
```

#### 2. Template Model

```dart
class Template {
  final String id;
  final String name;
  final String description;
  final String category;
  final String thumbnailUrl;
  final String htmlContent;
  final String cssContent;
  final String jsContent;
  final String type;
  final bool isPremium;
  final DateTime createdAt;
  final DateTime updatedAt;
  
  Template({
    required this.id,
    required this.name,
    required this.description,
    required this.category,
    required this.thumbnailUrl,
    required this.htmlContent,
    required this.cssContent,
    required this.jsContent,
    required this.type,
    this.isPremium = false,
    required this.createdAt,
    required this.updatedAt,
  });
  
  factory Template.fromJson(Map<String, dynamic> json) {
    return Template(
      id: json['_id'],
      name: json['name'],
      description: json['description'],
      category: json['category'],
      thumbnailUrl: json['thumbnailUrl'],
      htmlContent: json['htmlContent'],
      cssContent: json['cssContent'],
      jsContent: json['jsContent'],
      type: json['type'],
      isPremium: json['isPremium'] ?? false,
      createdAt: DateTime.parse(json['createdAt']),
      updatedAt: DateTime.parse(json['updatedAt']),
    );
  }
  
  Map<String, dynamic> toJson() => {
    '_id': id,
    'name': name,
    'description': description,
    'category': category,
    'thumbnailUrl': thumbnailUrl,
    'htmlContent': htmlContent,
    'cssContent': cssContent,
    'jsContent': jsContent,
    'type': type,
    'isPremium': isPremium,
    'createdAt': createdAt.toIso8601String(),
    'updatedAt': updatedAt.toIso8601String(),
  };
}
```

#### 3. Festival Model

```dart
class Festival {
  final String id;
  final String name;
  final String description;
  final String category;
  final DateTime date;
  final CategoryIcon? categoryIcon;
  final List<Template> templates;
  
  Festival({
    required this.id,
    required this.name,
    required this.description,
    required this.category,
    required this.date,
    this.categoryIcon,
    required this.templates,
  });
  
  factory Festival.fromJson(Map<String, dynamic> json) {
    return Festival(
      id: json['_id'],
      name: json['name'],
      description: json['description'],
      category: json['category'],
      date: DateTime.parse(json['date']),
      categoryIcon: json['categoryIcon'] != null
          ? CategoryIcon.fromJson(json['categoryIcon'])
          : null,
      templates: (json['templates'] as List?)
          ?.map((e) => Template.fromJson(e))
          .toList() ?? [],
    );
  }
  
  Map<String, dynamic> toJson() => {
    '_id': id,
    'name': name,
    'description': description,
    'category': category,
    'date': date.toIso8601String(),
    'categoryIcon': categoryIcon?.toJson(),
    'templates': templates.map((e) => e.toJson()).toList(),
  };
}
```

#### 4. Reminder Model

```dart
enum Priority { low, medium, high }

class Reminder {
  final int id;
  final String title;
  final String description;
  final DateTime dateTime;
  final Priority priority;
  final bool isRepeating;
  final int repeatInterval;
  final bool isCompleted;
  
  Reminder({
    required this.id,
    required this.title,
    required this.description,
    required this.dateTime,
    this.priority = Priority.medium,
    this.isRepeating = false,
    this.repeatInterval = 0,
    this.isCompleted = false,
  });
  
  factory Reminder.fromJson(Map<String, dynamic> json) {
    return Reminder(
      id: json['id'],
      title: json['title'],
      description: json['description'],
      dateTime: DateTime.parse(json['dateTime']),
      priority: Priority.values[json['priority']],
      isRepeating: json['isRepeating'],
      repeatInterval: json['repeatInterval'],
      isCompleted: json['isCompleted'],
    );
  }
  
  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'description': description,
    'dateTime': dateTime.toIso8601String(),
    'priority': priority.index,
    'isRepeating': isRepeating,
    'repeatInterval': repeatInterval,
    'isCompleted': isCompleted,
  };
  
  Reminder copyWith({
    int? id,
    String? title,
    String? description,
    DateTime? dateTime,
    Priority? priority,
    bool? isRepeating,
    int? repeatInterval,
    bool? isCompleted,
  }) {
    return Reminder(
      id: id ?? this.id,
      title: title ?? this.title,
      description: description ?? this.description,
      dateTime: dateTime ?? this.dateTime,
      priority: priority ?? this.priority,
      isRepeating: isRepeating ?? this.isRepeating,
      repeatInterval: repeatInterval ?? this.repeatInterval,
      isCompleted: isCompleted ?? this.isCompleted,
    );
  }
}
```

#### 5. Analytics Model

```dart
class Analytics {
  final int views;
  final int uniqueViews;
  final int shareCount;
  final Map<String, int> platformBreakdown;
  
  Analytics({
    required this.views,
    required this.uniqueViews,
    required this.shareCount,
    required this.platformBreakdown,
  });
  
  factory Analytics.fromJson(Map<String, dynamic> json) {
    return Analytics(
      views: json['views'] ?? 0,
      uniqueViews: json['uniqueViews'] ?? 0,
      shareCount: json['shareCount'] ?? 0,
      platformBreakdown: Map<String, int>.from(
        json['platformBreakdown'] ?? {},
      ),
    );
  }
  
  Map<String, dynamic> toJson() => {
    'views': views,
    'uniqueViews': uniqueViews,
    'shareCount': shareCount,
    'platformBreakdown': platformBreakdown,
  };
}
```

### Platform Configuration

#### Android Configuration

Update `android/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Permissions -->
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>
    <uses-permission android:name="android.permission.VIBRATE"/>
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    
    <application
        android:label="EventWish"
        android:name="${applicationName}"
        android:icon="@mipmap/ic_launcher">
        
        <!-- AdMob App ID -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-3940256099942544~3347511713"/>
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:theme="@style/LaunchTheme"
            android:configChanges="orientation|keyboardHidden|keyboard|screenSize|smallestScreenSize|locale|layoutDirection|fontScale|screenLayout|density|uiMode"
            android:hardwareAccelerated="true"
            android:windowSoftInputMode="adjustResize">
            
            <meta-data
                android:name="io.flutter.embedding.android.NormalTheme"
                android:resource="@style/NormalTheme"/>
            
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
            
            <!-- Deep Links -->
            <intent-filter android:autoVerify="true">
                <action android:name="android.intent.action.VIEW"/>
                <category android:name="android.intent.category.DEFAULT"/>
                <category android:name="android.intent.category.BROWSABLE"/>
                <data
                    android:scheme="https"
                    android:host="eventwishes.onrender.com"
                    android:pathPrefix="/wish/"/>
            </intent-filter>
            
            <!-- Custom Scheme -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW"/>
                <category android:name="android.intent.category.DEFAULT"/>
                <category android:name="android.intent.category.BROWSABLE"/>
                <data
                    android:scheme="eventwish"
                    android:host="wish"/>
            </intent-filter>
        </activity>
        
        <!-- Don't delete the meta-data below.
             This is used by the Flutter tool to generate GeneratedPluginRegistrant.java -->
        <meta-data
            android:name="flutterEmbedding"
            android:value="2"/>
    </application>
</manifest>
```

#### iOS Configuration

Update `ios/Runner/Info.plist`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- Add deep linking configuration -->
    <key>FlutterDeepLinkingEnabled</key>
    <true/>
    <key>CFBundleURLTypes</key>
    <array>
        <dict>
            <key>CFBundleURLSchemes</key>
            <array>
                <string>eventwish</string>
            </array>
        </dict>
    </array>
    
    <!-- Add Associated Domains for Universal Links -->
    <key>com.apple.developer.associated-domains</key>
    <array>
        <string>applinks:eventwishes.onrender.com</string>
    </array>
    
    <!-- Add GADApplicationIdentifier for AdMob -->
    <key>GADApplicationIdentifier</key>
    <string>ca-app-pub-3940256099942544~3347511713</string>
    
    <!-- Add permissions -->
    <key>NSUserNotificationsUsageDescription</key>
    <string>We need to send you notifications about upcoming events and reminders.</string>
</dict>
</plist>
```

### Dependencies

Add the following dependencies to `pubspec.yaml`:

```yaml
dependencies:
  flutter:
    sdk: flutter
  
  # State Management
  riverpod: ^2.4.0
  flutter_riverpod: ^2.4.0
  
  # Navigation
  go_router: ^12.0.0
  
  # Network & API
  dio: ^5.3.0
  retrofit: ^4.0.0
  json_annotation: ^4.8.0
  
  # Local Storage
  hive: ^2.2.3
  hive_flutter: ^1.1.0
  flutter_secure_storage: ^9.0.0
  
  # UI Components
  webview_flutter: ^4.4.0
  shimmer: ^3.0.0
  cached_network_image: ^3.3.0
  share_plus: ^7.2.0
  
  # Firebase
  firebase_core: ^2.20.0
  firebase_messaging: ^14.7.4
  firebase_analytics: ^10.6.4
  firebase_crashlytics: ^3.4.4
  
  # Ads
  google_mobile_ads: ^3.1.0
  
  # Utils
  device_info_plus: ^9.1.0
  package_info_plus: ^4.2.0
  uni_links: ^0.5.1
  path_provider: ^2.1.1
  intl: ^0.18.0

dev_dependencies:
  flutter_test:
    sdk: flutter
  build_runner: ^2.4.6
  json_serializable: ^6.7.1
  retrofit_generator: ^7.0.8
  hive_generator: ^2.0.1
```

### Project Structure

Create the following directory structure:

```
lib/
├── core/
│   ├── constants/
│   │   ├── api_constants.dart
│   │   ├── app_constants.dart
│   │   └── route_constants.dart
│   ├── errors/
│   │   ├── app_error.dart
│   │   └── network_error.dart
│   ├── network/
│   │   ├── api_client.dart
│   │   ├── dio_client.dart
│   │   └── interceptors/
│   └── utils/
│       ├── analytics_utils.dart
│       ├── date_utils.dart
│       └── share_utils.dart
├── data/
│   ├── datasources/
│   │   ├── local/
│   │   │   ├── database/
│   │   │   └── preferences/
│   │   └── remote/
│   │       ├── coins_api.dart
│   │       ├── template_api.dart
│   │       └── festival_api.dart
│   ├── models/
│   │   ├── analytics.dart
│   │   ├── category_icon.dart
│   │   ├── festival.dart
│   │   ├── reminder.dart
│   │   └── template.dart
│   └── repositories/
│       ├── coins_repository.dart
│       ├── template_repository.dart
│       └── festival_repository.dart
├── domain/
│   ├── entities/
│   │   ├── analytics.dart
│   │   ├── category.dart
│   │   ├── festival.dart
│   │   ├── reminder.dart
│   │   └── template.dart
│   ├── repositories/
│   │   ├── coins_repository.dart
│   │   ├── template_repository.dart
│   │   └── festival_repository.dart
│   └── usecases/
│       ├── get_templates.dart
│       ├── save_wish.dart
│       └── unlock_feature.dart
├── presentation/
│   ├── common/
│   │   ├── widgets/
│   │   │   ├── category_chip.dart
│   │   │   ├── shimmer_loading.dart
│   │   │   └── template_card.dart
│   │   └── theme/
│   │       ├── app_theme.dart
│   │       └── colors.dart
│   ├── pages/
│   │   ├── home/
│   │   │   ├── home_page.dart
│   │   │   └── widgets/
│   │   ├── template_detail/
│   │   │   ├── template_detail_page.dart
│   │   │   └── widgets/
│   │   ├── shared_wish/
│   │   │   ├── shared_wish_page.dart
│   │   │   └── widgets/
│   │   ├── festival/
│   │   │   ├── festival_page.dart
│   │   │   └── widgets/
│   │   └── reminder/
│   │       ├── reminder_page.dart
│   │       └── widgets/
│   └── providers/
│       ├── coins_provider.dart
│       ├── template_provider.dart
│       └── festival_provider.dart
└── main.dart
```

### Testing

#### 1. Unit Tests

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

#### 2. Widget Tests

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

#### 3. Integration Tests

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

### Deployment

#### 1. Android Release

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

#### 2. iOS Release

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

#### 3. CI/CD Setup

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

#### 4. App Store Submission

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