# EventWish Flutter App Structure

## Root Directory
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
```

## Data Layer
```
lib/data/
├── datasources/
│   ├── local/
│   │   ├── database/
│   │   │   ├── app_database.dart
│   │   │   ├── coins_dao.dart
│   │   │   └── reminder_dao.dart
│   │   └── preferences/
│   │       └── app_preferences.dart
│   └── remote/
│       ├── coins_api.dart
│       ├── template_api.dart
│       └── festival_api.dart
├── models/
│   ├── analytics.dart
│   ├── category_icon.dart
│   ├── festival.dart
│   ├── reminder.dart
│   └── template.dart
└── repositories/
    ├── coins_repository.dart
    ├── template_repository.dart
    └── festival_repository.dart
```

## Domain Layer
```
lib/domain/
├── entities/
│   ├── analytics.dart
│   ├── category.dart
│   ├── festival.dart
│   ├── reminder.dart
│   └── template.dart
├── repositories/
│   ├── coins_repository.dart
│   ├── template_repository.dart
│   └── festival_repository.dart
└── usecases/
    ├── get_templates.dart
    ├── save_wish.dart
    └── unlock_feature.dart
```

## Presentation Layer
```
lib/presentation/
├── common/
│   ├── widgets/
│   │   ├── category_chip.dart
│   │   ├── shimmer_loading.dart
│   │   └── template_card.dart
│   └── theme/
│       ├── app_theme.dart
│       └── colors.dart
├── pages/
│   ├── home/
│   │   ├── home_page.dart
│   │   └── widgets/
│   │       ├── category_list.dart
│   │       ├── template_grid.dart
│   │       └── filter_chips.dart
│   ├── template_detail/
│   │   ├── template_detail_page.dart
│   │   └── widgets/
│   │       ├── template_preview.dart
│   │       └── customization_panel.dart
│   ├── shared_wish/
│   │   ├── shared_wish_page.dart
│   │   └── widgets/
│   │       ├── share_options.dart
│   │       └── analytics_panel.dart
│   ├── festival/
│   │   ├── festival_page.dart
│   │   └── widgets/
│   │       ├── festival_card.dart
│   │       └── countdown_timer.dart
│   └── reminder/
│       ├── reminder_page.dart
│       └── widgets/
│           ├── reminder_list.dart
│           └── reminder_form.dart
└── providers/
    ├── coins_provider.dart
    ├── template_provider.dart
    └── festival_provider.dart
```

## Test Directory
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

## Configuration Files
```
├── pubspec.yaml
├── analysis_options.yaml
├── .gitignore
├── README.md
├── android/
│   └── app/
│       ├── build.gradle
│       └── src/
│           └── main/
│               ├── AndroidManifest.xml
│               └── kotlin/
│                   └── com/
│                       └── ds/
│                           └── eventwish/
│                               └── MainActivity.kt
└── ios/
    └── Runner/
        ├── Info.plist
        └── AppDelegate.swift
``` 