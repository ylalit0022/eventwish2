# Sample Data for About and Contact Sections

This directory contains scripts to populate the database with sample data for the About and Contact sections of the EventWish application.

## Files Overview

- `aboutSample.js` - Sample data for the About section with external links
- `contactSample.js` - Sample data for the Contact section with external links
- `insertSampleData.js` - Combined script to insert both About and Contact sample data

## HTML Content Features

The sample HTML content includes:

1. **Responsive Design** - Looks good on both mobile and desktop
2. **External Links** - Links to third-party websites that will open in a browser when clicked
3. **Email Links** - `mailto:` links that will open the default email client
4. **Styled Components** - CSS styling for a professional appearance
5. **Organized Sections** - Content is divided into logical sections

## External Links

The sample data includes various external links that will redirect to a browser when clicked:

### About Section Links
- Wikipedia (History of Greeting Cards)
- Canva (Design Inspiration)
- Pexels (Free Stock Photos)
- GitHub
- Privacy Policy Generator

### Contact Section Links
- Email links (support, bugs, business, privacy)
- Social media (Facebook, Twitter, Instagram, Pinterest)
- Google Forms
- GitHub Issues
- FAQ page

## How to Use

### Insert All Sample Data

To insert both About and Contact sample data at once:

```bash
cd backend
node data/insertSampleData.js
```

### Insert Individual Sample Data

To insert only About sample data:

```bash
cd backend
node data/aboutSample.js
```

To insert only Contact sample data:

```bash
cd backend
node data/contactSample.js
```

## Notes on Link Handling in Android

When implementing link handling in the Android app:

1. **WebView Configuration**

```java
webView.setWebViewClient(new WebViewClient() {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith("mailto:")) {
            // Handle email links
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse(url));
            startActivity(intent);
            return true;
        } else if (url.startsWith("tel:")) {
            // Handle phone links
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse(url));
            startActivity(intent);
            return true;
        } else if (url.startsWith("http:") || url.startsWith("https:")) {
            // Handle web links - open in external browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
        return false;
    }
});

// Enable JavaScript
WebSettings webSettings = webView.getSettings();
webSettings.setJavaScriptEnabled(true);
```

2. **Add Required Permissions**

Add these permissions to your AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Customization

Feel free to modify the HTML content in the sample files to match your application's branding and requirements. The HTML includes inline CSS for styling, which can be adjusted as needed. 