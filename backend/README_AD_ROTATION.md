# Sponsored Ads Rotation and Fair Distribution

This document provides an overview of the ad rotation and fair distribution features implemented in the backend API.

## Overview

The ad rotation and fair distribution system enables the app to display different ads over time while ensuring all ads get fair exposure based on their priority and current impression counts. This improves user experience and maximizes ad effectiveness.

## API Endpoints

### 1. GET /api/sponsored-ads/rotation

Fetches ads for rotation with support for excluding previously shown ads.

**Query Parameters:**
- `location` (optional): Filter ads by location (e.g., "home_top", "category_below")
- `limit` (optional): Maximum number of ads to return (default: 10)
- `exclude` (optional): Array of ad IDs to exclude from results

**Example Request:**
```
GET /api/sponsored-ads/rotation?location=home_top&limit=3&exclude[]=60f8a3b4c1e2e30015d781a2&exclude[]=60f8a3b4c1e2e30015d781a3
```

**Example Response:**
```json
{
  "success": true,
  "ads": [
    {
      "id": "60f8a3b4c1e2e30015d781a4",
      "title": "Featured Product",
      "description": "Check out our featured product",
      "image_url": "https://example.com/image.jpg",
      "redirect_url": "https://example.com/product",
      "priority": 8,
      "location": "home_top",
      "start_date": "2023-01-01T00:00:00.000Z",
      "end_date": "2023-12-31T23:59:59.999Z",
      "status": true,
      "impression_count": 52,
      "click_count": 7
    },
    // More ads...
  ]
}
```

### 2. GET /api/sponsored-ads/fair-distribution

Fetches ads with fair distribution based on priority and impression counts.

**Query Parameters:**
- `location` (optional): Filter ads by location (e.g., "home_top", "category_below")
- `limit` (optional): Maximum number of ads to return (default: 10)

**Example Request:**
```
GET /api/sponsored-ads/fair-distribution?location=home_top&limit=5
```

**Example Response:**
```json
{
  "success": true,
  "ads": [
    // Ads ordered by weighted priority...
  ]
}
```

## Fair Distribution Algorithm

The fair distribution algorithm ensures that ads get exposure proportional to their priority, while also accounting for how often they've been shown.

The algorithm works as follows:

1. **Weight Calculation**: Each ad gets a weight calculated by:
   ```
   weight = priority * (1 / (1 + log(1 + impression_count)))
   ```
   This ensures that:
   - Higher priority ads get more exposure
   - As impressions increase, the weight gradually decreases
   - The logarithmic function prevents the weight from dropping too rapidly

2. **Sorting**: Ads are sorted by this weight in descending order

3. **Selection**: The top N ads (based on the requested limit) are returned

This approach balances the need to show high-priority ads more frequently while ensuring that all ads get some level of exposure.

## Client Usage Example

Here's a simplified example of how to use the rotation API from a client:

```javascript
const shownAds = []; // Persist this across app sessions

async function fetchAdForRotation(location) {
  const response = await fetch(`/api/sponsored-ads/rotation?location=${location}&exclude=${shownAds.join(',')}`);
  const data = await response.json();
  
  if (data.success && data.ads.length > 0) {
    const ad = data.ads[0];
    shownAds.push(ad.id); // Remember we've shown this ad
    return ad;
  }
  return null;
}

// Track impression when ad is shown
async function recordImpression(adId) {
  await fetch(`/api/sponsored-ads/viewed/${adId}`, { method: 'POST' });
}

// Track click when ad is interacted with
async function recordClick(adId) {
  await fetch(`/api/sponsored-ads/clicked/${adId}`, { method: 'POST' });
}
```

## Testing

The implementation includes tests that verify:
1. The exclusion functionality works correctly
2. The fair distribution algorithm properly weights ads by priority and impressions
3. The API endpoints return the expected results

For a full client example, see `client-examples/sponsored-ad-rotation.js`. 