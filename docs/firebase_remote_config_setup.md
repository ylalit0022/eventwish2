# Firebase Remote Config Setup Guide for EventWish Notification System

## Overview
This guide provides step-by-step instructions for setting up Firebase Remote Config parameters for the EventWish app's centralized notification system, including production-ready sample values.

## Firebase Console Configuration

### 1. Access Firebase Remote Config
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your EventWish project
3. Navigate to **Engage** > **Remote Config**

### 2. Create notification_config Parameter

#### Parameter Details
- **Parameter key**: `notification_config`
- **Data type**: JSON
- **Description**: Centralized notification configuration for EventWish app

#### Production-Ready Sample Value

```json
{
  "version": "2.1",
  "lastUpdated": "2024-12-20T10:00:00Z",
  "notificationTypes": {
    "dailyReminder": {
      "enabled": true,
      "schedule": {
        "type": "daily",
        "utcHour": 9,
        "utcMinute": 0
      },
      "messages": {
        "title": "✨ Daily Inspiration",
        "body": "Start your day with beautiful greetings! Create and share joy today. 🌟"
      }
    },
    "festivalAlert": {
      "enabled": true,
      "schedule": {
        "type": "dynamic",
        "daysBeforeFestival": [0, 1, 3, 7]
      },
      "messages": {
        "title": "🎉 {{festivalName}} is Here!",
        "body": "{{festivalName}} is {{timeMessage}}! Create stunning greeting cards to celebrate with your loved ones. 💝"
      }
    },
    "inactivityNudge": {
      "enabled": true,
      "schedule": {
        "type": "inactivity",
        "thresholdDays": 5
      },
      "messages": {
        "title": "We Miss You! 💙",
        "body": "Your creativity is missed! Come back and create beautiful greetings to brighten someone's day. ✨"
      }
    },
    "subscriptionExpiry": {
      "enabled": true,
      "schedule": {
        "type": "expiry",
        "daysBeforeExpiry": [1, 3, 7, 14]
      },
      "messages": {
        "title": "⚡ Premium Features Expiring",
        "body": "Your premium subscription expires in {{daysUntil}} days. Renew now to keep creating unlimited greetings! 🎨"
      }
    }
  },
  "globalSettings": {
    "maxNotificationsPerDay": 4,
    "quietHoursStart": 22,
    "quietHoursEnd": 7,
    "batchingIntervalMinutes": 30,
    "enableDuplicateProtection": true,
    "respectSystemDnd": true,
    "enableVibration": true,
    "priority": "default"
  }
}
```

## Implementation Steps

### 1. Create Parameters
1. In Firebase Console, click **"Add parameter"**
2. Enter parameter key: `notification_config`
3. Add default value using the JSON above
4. Set description: "Centralized notification configuration"

### 2. Publish Configuration
1. Review all parameters
2. Click **"Publish changes"**
3. Add change description: "Initial notification system setup"

### 3. Verify Implementation
1. Test configuration fetch in app
2. Monitor notification behavior
3. Check app logs for success

## Testing Checklist

- [ ] Verify JSON syntax in Remote Config
- [ ] Test notification delivery
- [ ] Validate quiet hours functionality
- [ ] Test notification limits
- [ ] Monitor user engagement metrics

## Monitoring and Analytics

### Key Metrics to Track
- **Notification Delivery Rate**: Percentage of notifications successfully delivered
- **Engagement Rate**: Click-through rate on notifications
- **User Retention**: 7-day and 30-day retention rates
- **Conversion Rate**: Premium subscription conversions from notifications
- **Uninstall Rate**: App uninstalls correlated with notification frequency

### Firebase Analytics Events
```javascript
// Track notification events
analytics.logEvent('notification_delivered', {
  notification_type: 'dailyReminder',
  user_segment: 'new_users'
});

analytics.logEvent('notification_clicked', {
  notification_type: 'festivalAlert',
  festival_name: 'Diwali'
});

analytics.logEvent('notification_dismissed', {
  notification_type: 'inactivityNudge'
});
```

## Troubleshooting

### Common Issues
1. **Notifications not showing**: Check notification permissions and channel settings
2. **Wrong timing**: Verify UTC time conversion and timezone handling
3. **Duplicate notifications**: Check duplicate protection logic
4. **Poor engagement**: Review message content and timing
5. **High uninstall rate**: Reduce notification frequency

### Debug Commands
```bash
# Check Remote Config fetch status
adb logcat | grep "RemoteConfig"

# Monitor notification delivery
adb logcat | grep "EventNotificationManager"

# Check WorkManager status
adb logcat | grep "NotificationSyncWorker"
```

## Security Considerations

### Data Protection
- No sensitive user data in notification content
- Secure template variable replacement
- Validate all Remote Config values
- Implement rate limiting to prevent abuse

### Privacy Compliance
- Respect user notification preferences
- Honor system Do Not Disturb settings
- Provide easy opt-out mechanisms
- Comply with GDPR and regional privacy laws

## Maintenance Schedule

### Weekly Tasks
- Review notification performance metrics
- Update seasonal content as needed
- Monitor user feedback and ratings

### Monthly Tasks
- Analyze A/B test results
- Update notification content for freshness
- Review and optimize delivery times

### Quarterly Tasks
- Comprehensive performance review
- User survey on notification preferences
- Feature updates and improvements