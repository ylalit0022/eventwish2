# EventWish

A mobile application for creating and sharing event wishes and greetings.

## Project Overview

EventWish is an Android application that allows users to:
- Browse various greeting templates organized by categories
- Customize greetings with personal messages
- Share wishes via messaging apps and social media
- Save a history of shared wishes
- Set reminders for important events

## Development Setup

### Prerequisites
- Android Studio
- JDK 11 or higher
- Gradle 8.0 or higher

### Building the project
1. Clone the repository
2. Open in Android Studio
3. Build using Gradle: `./gradlew assembleDebug`

### Configuration
Create a `secrets.properties` file in the root directory with your API keys:
```
API_BASE_URL="your_api_base_url"
```

## Backend Services

The application uses a Node.js backend for:
- Template management
- User activity tracking
- Analytics
- Shared wish storage

## License

This project is private and confidential.
