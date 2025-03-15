# AdMob Test Page

A comprehensive React-based test page to visually check all AdMob-related features including analytics, fraud detection, and A/B testing functionality.

## Features

- **Ad Configuration Display**: View and test different ad configurations
- **Ad Display Testing**: Test different ad formats (Banner, Interstitial, Rewarded, Native)
- **Analytics Dashboard**: Visualize ad performance metrics
- **Fraud Detection Testing**: Test fraud detection mechanisms
- **A/B Testing**: Create and monitor A/B tests for ad configurations
- **Settings Management**: Configure API keys, app signatures, and other settings
- **Dark Mode Support**: Toggle between light and dark themes

## Prerequisites

- Node.js (v14.0.0 or higher)
- npm (v6.0.0 or higher)

## Installation

1. Clone the repository:
   ```
   git clone <repository-url>
   cd admob-test-page
   ```

2. Install dependencies:
   ```
   npm install
   ```

## Running the Application

### Development Mode

To start the development server:

```
npm run dev
```

Or use the provided start script with the dev flag:

```
./start.sh --dev
```

The application will be available at [http://localhost:3000](http://localhost:3000).

### Production Mode

To run the application in production mode:

```
npm start
```

Or use the provided start script:

```
./start.sh
```

This will build the application if needed and serve it using Express.

### Production Build

To build the application for production:

```
npm run build
```

Or use the provided build script:

```
./build.sh
```

The production files will be available in the `dist` directory.

## Deployment

### Local Deployment

To build the application for production:

```
npm run build
```

Or use the provided build script:

```
./build.sh
```

The production files will be available in the `dist` directory.

### Deploying to Render

This application is configured to be deployed to Render.com. For detailed deployment instructions, see [RENDER_DEPLOYMENT.md](./RENDER_DEPLOYMENT.md).

Quick steps:
1. Run `./build.sh` to prepare the application for deployment
2. Create a new Web Service on Render
3. Set the environment variables
4. Deploy the service

Once deployed, the application will be available at the URL provided by Render (e.g., `https://admob-test-page.onrender.com`).

### Server Configuration

By default, the application is configured to use the API server at `https://eventwish2.onrender.com`. You can change this in the Settings page of the application.

## Usage

### Dashboard

The dashboard provides an overview of key metrics and quick access to all features.

### Ad Configuration

View and test different ad configurations. You can:
- View active ad configurations
- Test configurations with different user contexts
- View performance metrics for each configuration

### Ad Display

Test different ad formats:
- Banner ads
- Interstitial ads
- Rewarded ads
- Native ads

You can customize ad settings and track impressions and clicks.

### Analytics

The analytics dashboard provides visualizations of:
- Impressions over time
- Clicks over time
- CTR (Click-Through Rate) over time
- Revenue over time
- Ad performance by type

### Fraud Detection

Test fraud detection mechanisms:
- Rapid clicks simulation
- Pattern clicks simulation
- Multi-device simulation

### A/B Testing

Create and monitor A/B tests for ad configurations:
- Create new tests with multiple variants
- Start, stop, and monitor tests
- View test results with visualizations
- Declare winners based on performance metrics

### Settings

Configure application settings:
- API key and app signature
- Server endpoint
- Ad types to enable/disable
- Feature toggles
- Log level

## API Integration

The application integrates with the AdMob API through the following endpoints:

- `/api/admob/config`: Get ad configuration
- `/api/admob/impression`: Track ad impression
- `/api/admob/click`: Track ad click
- `/api/admob/types`: Get ad types
- `/api/admob-ads`: Get all active ads
- `/api/analytics/ad/:id`: Get analytics data for a specific ad
- `/api/analytics/summary`: Get summary analytics for all ads
- `/api/monitoring/metrics`: Get current monitoring metrics
- `/api/monitoring/ad-metrics`: Get ad-specific monitoring metrics
- `/api/testing/simulate-fraud`: Simulate fraud detection
- `/api/fraud-detection/status`: Get fraud detection status
- `/api/abtests`: Get A/B test configurations
- `/api/abtests/:id/results`: Get A/B test results

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details. 