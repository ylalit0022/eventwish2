# Update Analytics Dashboards

## Implementation Status

✅ **COMPLETE** - All dashboards have been designed and documented. Ready for implementation in Firebase Analytics.

## Overview

Tracking update metrics helps understand user behavior around app updates and can inform decisions about update frequency, messaging, and whether to make updates mandatory. The dashboards below use Firebase Analytics events tracked by the RemoteConfigManager and UpdateDialogHelper classes.

## Dashboard 1: Update Check Overview

This dashboard provides a high-level overview of update check performance.

### Setup Instructions

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Select the EventWish project
3. Navigate to Analytics → Dashboards → Create dashboard
4. Name the dashboard "Update Check Overview"
5. Add the following cards:

#### Card 1: Update Check Volume
- Type: Time series chart
- Metric: Event count
- Dimension: Day
- Filter: Event name equals `update_check`

#### Card 2: Update Availability Rate
- Type: Scorecard
- Metric: Parameter value
- Parameter: is_update_available
- Aggregation: Average
- Filter: Event name equals `update_check`
- Format as percentage

#### Card 3: Update Prompt Actions
- Type: Pie chart
- Metric: Event count
- Dimension: action
- Filter: Event name equals `update_prompt_action`

#### Card 4: Update Check by App Version
- Type: Bar chart
- Metric: Event count
- Dimension: current_version_name
- Filter: Event name equals `update_check`

## Dashboard 2: Update Funnel Analysis

This dashboard tracks the conversion through the update funnel.

### Setup Instructions

1. Go to the Firebase Console
2. Navigate to Analytics → Dashboards → Create dashboard
3. Name the dashboard "Update Funnel Analysis"
4. Add the following cards:

#### Card 1: Update Funnel
- Type: Funnel chart
- Steps:
  - Step 1: Event name equals `update_check`
  - Step 2: Event name equals `update_available_impression`
  - Step 3: Event name equals `update_dialog_shown`
  - Step 4: Event name equals `update_prompt_action` AND action equals "accepted"

#### Card 2: Update Funnel Conversion Rate
- Type: Scorecard
- Metric: Custom formula
- Formula: (Count where Event name equals `update_prompt_action` AND action equals "accepted") / (Count where Event name equals `update_available_impression`)
- Format as percentage

#### Card 3: Time to Update
- Type: Time series chart
- Metric: Parameter value
- Parameter: timestamp
- Aggregation: Average
- Filter: Event name equals `update_prompt_action` AND action equals "accepted"
- Subtract from: Parameter value where Event name equals `update_available_impression`
- Format as duration

## Dashboard 3: Force Update Effectiveness

This dashboard analyzes the effectiveness of force updates.

### Setup Instructions

1. Go to the Firebase Console
2. Navigate to Analytics → Dashboards → Create dashboard
3. Name the dashboard "Force Update Effectiveness"
4. Add the following cards:

#### Card 1: Force Update vs Optional Update Acceptance
- Type: Bar chart
- Metric: Event count
- Dimension: action
- Filter: Event name equals `update_prompt_action`
- Breakdown: is_force_update

#### Card 2: Force Update Compliance Rate
- Type: Scorecard
- Metric: Custom formula
- Formula: (Count where Event name equals `update_prompt_action` AND action equals "accepted" AND is_force_update equals true) / (Count where Event name equals `update_dialog_shown` AND is_force_update equals true)
- Format as percentage

#### Card 3: Force Update Abandonment Rate
- Type: Scorecard
- Metric: Custom formula
- Formula: (Count where Event name equals `update_prompt_action` AND action equals "declined" AND is_force_update equals true) / (Count where Event name equals `update_dialog_shown` AND is_force_update equals true)
- Format as percentage

## Dashboard 4: Version Distribution

This dashboard tracks the distribution of app versions across your user base.

### Setup Instructions

1. Go to the Firebase Console
2. Navigate to Analytics → Dashboards → Create dashboard
3. Name the dashboard "Version Distribution"
4. Add the following cards:

#### Card 1: Current Version Distribution
- Type: Pie chart
- Metric: User count
- Dimension: current_version_name
- Filter: Event name equals `update_check`

#### Card 2: Version Adoption Over Time
- Type: Time series chart
- Metric: Event count
- Dimension: Day
- Breakdown: current_version_name
- Filter: Event name equals `update_check`

#### Card 3: Latest Version Adoption Rate
- Type: Scorecard
- Metric: Custom formula
- Formula: (Count where Event name equals `update_check` AND current_version_name equals [latest_version]) / (Count where Event name equals `update_check`)
- Format as percentage
- Note: Replace [latest_version] with your actual latest version

## Dashboard 5: Update Messaging Effectiveness

This dashboard analyzes how different update messages affect user behavior.

### Setup Instructions

1. Go to the Firebase Console
2. Navigate to Analytics → Dashboards → Create dashboard
3. Name the dashboard "Update Messaging Effectiveness"
4. Add the following cards:

#### Card 1: Update Message Conversion Rate
- Type: Table
- Rows: update_message
- Columns: 
  - Metric 1: Count where Event name equals `update_dialog_shown`
  - Metric 2: Count where Event name equals `update_prompt_action` AND action equals "accepted"
  - Metric 3: Custom formula (Metric 2 / Metric 1), format as percentage

## Implementation in BigQuery

For more advanced analysis, you can use BigQuery with Firebase Analytics data:

```sql
-- Update funnel analysis
SELECT
  COUNT(DISTINCT CASE WHEN event_name = 'update_check' THEN user_pseudo_id END) AS check_count,
  COUNT(DISTINCT CASE WHEN event_name = 'update_available_impression' THEN user_pseudo_id END) AS impression_count,
  COUNT(DISTINCT CASE WHEN event_name = 'update_dialog_shown' THEN user_pseudo_id END) AS dialog_count,
  COUNT(DISTINCT CASE WHEN event_name = 'update_prompt_action' AND params.value.string_value = 'accepted' THEN user_pseudo_id END) AS accepted_count
FROM
  `your-project-id.analytics_XXXXXXXXX.events_*`
WHERE
  _TABLE_SUFFIX BETWEEN FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY))
  AND FORMAT_DATE('%Y%m%d', CURRENT_DATE())
  AND (event_name = 'update_check' 
       OR event_name = 'update_available_impression' 
       OR event_name = 'update_dialog_shown' 
       OR event_name = 'update_prompt_action')
```

## Automated Reporting

To receive regular updates on these metrics:

1. Go to the Firebase Console
2. Navigate to Analytics → Dashboards
3. Select your dashboard
4. Click "Share" and select "Schedule email reports"
5. Choose frequency (daily, weekly, monthly)
6. Add recipient email addresses
7. Click "Save"

## Key Metrics to Monitor

1. **Update Check Rate**: % of sessions where update check occurs
2. **Update Available Rate**: % of checks where update is available
3. **Update Prompt Impression Rate**: % of users who see update dialog
4. **Update Acceptance Rate**: % of users who accept the update
5. **Update Deferral Rate**: % of users who defer the update
6. **Update Completion Rate**: % of users who complete the update process
7. **Time-to-Update**: Average time between update availability and acceptance
8. **Version Distribution**: % of users on each app version
9. **Force Update Compliance**: % of users who update when forced

## Troubleshooting

If metrics are not appearing in your dashboards:

1. Verify events are being logged correctly by checking DebugView in Firebase Analytics
2. Ensure RemoteConfigManager and UpdateDialogHelper are properly initialized
3. Check that parameter names match exactly (case-sensitive)
4. Allow 24-48 hours for data to populate in BigQuery
5. For real-time debugging, use the Firebase Analytics DebugView 