# EventWish Notification Documentation

This directory contains documentation and tools for the EventWish notification system.

## Overview

The EventWish notification system allows for scheduled push notifications about upcoming festivals and events. Notifications are configured through Firebase Remote Config, which provides flexibility to update notification content without requiring app updates.

## Documentation Files

1. **notification_system.md**: Comprehensive documentation of the notification system
2. **notification_json_reference.md**: Quick reference guide for the JSON configuration format
3. **notification_templates.json**: Example JSON templates for different notification scenarios
4. **generate_festivals.py**: Python script to generate festival notification JSON

## Quick Start

### Setting Up Notifications

1. Choose a notification format (single event or multiple events)
2. Configure the notification parameters in Firebase Remote Config
3. Set the appropriate deep link (`eventwish://open/festival_notification`)
4. Test the notification using the EventNotificationTestActivity

### JSON Configuration Example

```json
{
  "events": [
    {
      "id": "diwali_2023",
      "title": "Diwali Festival 2023",
      "body": "Diwali celebration is coming soon! Only {{countdown}} left!",
      "status": true,
      "startdate": "2023-10-01T00:00:00Z",
      "enddate": "2023-11-12T23:59:59Z",
      "deeplinksupport": "eventwish://open/festival_notification",
      "showTime": "18:30"
    },
    {
      "id": "christmas_2023",
      "title": "Christmas 2023",
      "body": "Christmas is almost here! Only {{countdown}} left!",
      "status": true,
      "startdate": "2023-11-25T00:00:00Z",
      "enddate": "2023-12-25T23:59:59Z",
      "deeplinksupport": "eventwish://open/festival_notification",
      "showTime": "09:00"
    }
  ]
}
```

### Generating Festival JSON

Use the provided Python script to generate festival notification JSON:

```bash
python generate_festivals.py --year 2023 --format firebase --output festivals.json
```

This will generate a JSON file with festival notifications for the specified year in the Firebase Remote Config format.

## Deep Linking

When a user taps on a notification, the app opens to the FestivalNotificationFragment using the deep link specified in the `deeplinksupport` field. The recommended deep link format is:

```
eventwish://open/festival_notification
```

This deep link will navigate directly to the festival notification screen showing all upcoming festivals.

## Testing

The notification system includes a test activity (`EventNotificationTestActivity`) that allows for:

1. Viewing current notification configuration
2. Testing notification display
3. Setting custom notification configuration for testing
4. Viewing scheduled notifications

## Additional Resources

For more detailed information, refer to the comprehensive documentation in `notification_system.md`.

## Troubleshooting

Common issues and solutions:

1. **Notifications not showing**: Check that the current date is between `startdate` and `enddate`
2. **Incorrect countdown**: Verify the `enddate` format is correct (ISO 8601)
3. **Deep links not working**: Ensure the app has proper deep link handling configured
4. **Multiple notifications**: Check for duplicate event IDs
5. **Notification permission**: Ensure the app has notification permission 