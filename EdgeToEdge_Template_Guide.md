# 🎨 EventWish Edge-to-Edge Template Guide

## 🌟 Overview

This guide provides comprehensive instructions for creating and implementing edge-to-edge HTML/CSS templates for the EventWish greeting card app. These templates are optimized for WebView display with Material 3 festive theming, picture-in-picture support, and premium user experience.

## 🎯 Key Features

### ✅ **Edge-to-Edge Display**
- Full viewport utilization (`100vw` x `100vh/100dvh`)
- Safe area support for notches and cutouts
- Dynamic viewport height for mobile browsers
- Proper overflow handling

### ✅ **Material 3 Festive Design**
- EventWish brand colors (Pink Red, Warm Yellow, Teal Mint)
- Dynamic light/dark theme support
- Material 3 elevation and shadows
- Festive typography and animations

### ✅ **Picture-in-Picture Optimization**
- Responsive design for PiP mode (320x240px)
- Simplified layouts for small screens
- Optimized touch targets and typography
- Graceful degradation of animations

### ✅ **Premium User Experience**
- Smooth animations and transitions
- Touch-friendly interactions
- Accessibility compliance (WCAG AA)
- Performance optimizations

## 🏗️ Template Structure

### Basic HTML Structure
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
    <title>EventWish Greeting Card</title>
    <style>
        /* Edge-to-edge CSS styles */
    </style>
</head>
<body>
    <div class="greeting-container">
        <!-- Decorative elements -->
        <div class="decoration">🎉</div>
        
        <!-- Main content -->
        <div class="greeting-card">
            <h1 class="card-title">Greeting Title</h1>
            <p class="card-subtitle">Subtitle</p>
            <p class="card-message">Main message content</p>
            
            <div class="action-buttons">
                <button class="btn btn-primary">Share</button>
                <button class="btn btn-secondary">Save</button>
            </div>
        </div>
    </div>
    
    <script>
        /* JavaScript for interactivity */
    </script>
</body>
</html>
```

## 🎨 CSS Implementation

### 1. Edge-to-Edge Foundation
```css
/* Essential edge-to-edge setup */
html, body {
    width: 100vw;
    height: 100vh;
    height: 100dvh; /* Dynamic viewport height for mobile */
    margin: 0;
    padding: 0;
    overflow-x: hidden;
    
    /* Safe area support for notches/cutouts */
    padding: env(safe-area-inset-top) env(safe-area-inset-right) 
             env(safe-area-inset-bottom) env(safe-area-inset-left);
}

.greeting-container {
    width: 100vw;
    min-height: 100vh;
    min-height: 100dvh;
    position: relative;
    overflow: hidden;
}
```

### 2. Material 3 Festive Colors
```css
:root {
    /* Light theme colors */
    --primary: #D64F7D;           /* Pink Red - Love/Celebration */
    --on-primary: #FFFFFF;
    --primary-container: #FFD6E4;
    
    --secondary: #FBB13C;         /* Warm Yellow - Joy/Birthday */
    --secondary-container: #FFE8C7;
    
    --tertiary: #80CBC4;          /* Teal Mint - Calm/Gratitude */
    --tertiary-container: #B2DFDB;
    
    --surface: #FFFFFF;
    --on-surface: #1C1B1F;
    --background: #FFF8F6;        /* Light paper feel */
}

/* Dark theme support */
@media (prefers-color-scheme: dark) {
    :root {
        --primary: #FFB1C5;
        --on-primary: #3B001C;
        --surface: #121212;
        --on-surface: #E6E1E5;
        --background: #1A1A1A;      /* Dark cozy feel */
    }
}
```

### 3. Responsive Design
```css
/* Mobile optimizations */
@media (max-width: 480px) {
    .greeting-card {
        margin: 0.5rem;
        padding: 1.5rem;
        border-radius: 16px;
    }
    
    .card-title {
        font-size: 1.5rem;
    }
}

/* Picture-in-Picture optimizations */
@media (max-width: 320px) and (max-height: 240px) {
    .greeting-card {
        padding: 1rem;
        margin: 0.25rem;
    }
    
    .card-title {
        font-size: 1rem;
    }
    
    .btn {
        padding: 0.5rem 1rem;
        font-size: 0.7rem;
        min-height: 32px;
    }
    
    .decoration {
        display: none; /* Hide decorations in PiP */
    }
}
```

### 4. Accessibility Features
```css
/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
    * {
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
        transition-duration: 0.01ms !important;
    }
}

/* High contrast support */
@media (prefers-contrast: high) {
    .greeting-card {
        border: 2px solid var(--outline);
    }
    
    .btn {
        border: 2px solid currentColor;
    }
}

/* Focus management */
.btn:focus-visible {
    outline: 2px solid var(--primary);
    outline-offset: 2px;
}

/* Touch targets */
.btn {
    min-height: 48px; /* Minimum touch target size */
    min-width: 48px;
}
```

## 🚀 JavaScript Integration

### 1. WebView Communication
```javascript
// Communication with Android WebView
window.addEventListener('message', (event) => {
    const { type, data } = event.data;
    
    switch (type) {
        case 'updateContent':
            updateCardContent(data);
            break;
        case 'setTheme':
            setCustomTheme(data);
            break;
        case 'enterPiP':
            document.body.classList.add('pip-mode');
            break;
        case 'exitPiP':
            document.body.classList.remove('pip-mode');
            break;
    }
});

function updateCardContent(data) {
    if (data.title) document.querySelector('.card-title').textContent = data.title;
    if (data.subtitle) document.querySelector('.card-subtitle').textContent = data.subtitle;
    if (data.message) document.querySelector('.card-message').textContent = data.message;
}
```

### 2. Android Integration
```javascript
// Save functionality
function saveCard() {
    if (window.Android && window.Android.saveCard) {
        window.Android.saveCard();
    } else {
        showToast('Card saved to favorites!');
    }
}

// Share functionality
function shareCard() {
    if (navigator.share) {
        navigator.share({
            title: 'EventWish Greeting',
            text: 'Check out this greeting card!',
            url: window.location.href
        }).catch(console.error);
    } else if (window.Android && window.Android.shareCard) {
        window.Android.shareCard();
    }
}
```

### 3. Theme Adaptation
```javascript
// Dynamic theme adaptation
function adaptToSystemTheme() {
    const isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
}

// Listen for theme changes
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', adaptToSystemTheme);
```

## 📱 Android WebView Integration

### 1. WebView Setup in Fragment
```java
// Enable edge-to-edge in WebView
webView.getSettings().setJavaScriptEnabled(true);
webView.getSettings().setDomStorageEnabled(true);
webView.getSettings().setLoadWithOverviewMode(true);
webView.getSettings().setUseWideViewPort(true);

// Add JavaScript interface
webView.addJavascriptInterface(new WebAppInterface(this), "Android");

// Load template
webView.loadUrl("file:///android_asset/sample_edge_to_edge_template.html");
```

### 2. JavaScript Interface
```java
public class WebAppInterface {
    Context context;

    WebAppInterface(Context c) {
        context = c;
    }

    @JavascriptInterface
    public void saveCard() {
        // Handle save functionality
    }

    @JavascriptInterface
    public void shareCard() {
        // Handle share functionality
    }
}
```

### 3. Picture-in-Picture Integration
```java
// Update WebView for PiP mode
private void updateWebViewForPiP(boolean isInPiP) {
    if (webView != null) {
        String script = isInPiP ? 
            "window.postMessage({type: 'enterPiP'}, '*');" :
            "window.postMessage({type: 'exitPiP'}, '*');";
        webView.evaluateJavascript(script, null);
    }
}
```

## 🎪 Festive Design Guidelines

### 1. Color Usage
- **Primary (Pink Red)**: Use for love, celebration, romance cards
- **Secondary (Warm Yellow)**: Use for birthday, joy, friendship cards  
- **Tertiary (Teal Mint)**: Use for thank you, get well, sympathy cards

### 2. Typography Hierarchy
- **Display Large (2.5rem)**: Main greeting titles
- **Headline Medium (1.75rem)**: Card subtitles
- **Body Large (1rem)**: Message content
- **Label Large (0.875rem)**: Button text

### 3. Animation Guidelines
- **Entrance**: Slide up with fade (0.8s ease-out)
- **Hover**: Subtle lift with shadow (0.3s ease)
- **Active**: Quick press feedback (0.1s ease)
- **Background**: Gentle floating animations (6-8s ease-in-out)

### 4. Spacing System
- **Extra Large**: 2rem (32px) - Card padding
- **Large**: 1.5rem (24px) - Section spacing
- **Medium**: 1rem (16px) - Element spacing
- **Small**: 0.5rem (8px) - Tight spacing

## 🔧 Performance Optimization

### 1. CSS Optimizations
```css
/* Use transform for animations (GPU accelerated) */
.card-content {
    transform: translateZ(0); /* Force hardware acceleration */
    will-change: transform; /* Hint to browser */
}

/* Optimize animations */
@keyframes slideUp {
    from {
        opacity: 0;
        transform: translate3d(0, 30px, 0);
    }
    to {
        opacity: 1;
        transform: translate3d(0, 0, 0);
    }
}
```

### 2. JavaScript Optimizations
```javascript
// Use requestAnimationFrame for smooth animations
function animateElement() {
    requestAnimationFrame(() => {
        // Animation logic here
    });
}

// Debounce resize events
const debouncedResize = debounce(() => {
    // Resize handling
}, 100);

window.addEventListener('resize', debouncedResize);
```

### 3. Image Optimizations
```css
/* Use CSS for decorative elements instead of images */
.decoration::before {
    content: '🎉';
    font-size: 2rem;
    /* Better performance than background images */
}
```

## 🧪 Testing Guidelines

### 1. Device Testing
- **Mobile**: iPhone SE, Galaxy S21, Pixel 6
- **Tablet**: iPad Air, Galaxy Tab S8
- **Desktop**: Chrome, Firefox, Safari
- **WebView**: Android WebView, iOS WKWebView

### 2. Orientation Testing
- **Portrait**: Primary orientation
- **Landscape**: Ensure proper layout adaptation
- **Rotation**: Smooth transitions between orientations

### 3. Accessibility Testing
- **Screen Reader**: VoiceOver, TalkBack compatibility
- **High Contrast**: Ensure visibility in high contrast mode
- **Reduced Motion**: Verify animations respect user preferences
- **Keyboard Navigation**: Tab order and focus management

### 4. Performance Testing
- **Load Time**: < 2 seconds for template loading
- **Animation**: 60fps smooth animations
- **Memory**: No memory leaks in long sessions
- **Battery**: Minimal battery impact

## 📋 Checklist for New Templates

### ✅ **Design Requirements**
- [ ] Uses EventWish festive color palette
- [ ] Material 3 design principles followed
- [ ] Responsive design for all screen sizes
- [ ] Picture-in-Picture optimizations included
- [ ] Dark theme support implemented

### ✅ **Technical Requirements**
- [ ] Edge-to-edge layout implemented
- [ ] Safe area support for notches
- [ ] WebView communication ready
- [ ] JavaScript interface methods included
- [ ] Performance optimizations applied

### ✅ **Accessibility Requirements**
- [ ] WCAG AA compliance verified
- [ ] Screen reader compatibility tested
- [ ] Keyboard navigation working
- [ ] High contrast support included
- [ ] Reduced motion support added

### ✅ **Testing Requirements**
- [ ] Multiple device sizes tested
- [ ] Both orientations verified
- [ ] Light and dark themes tested
- [ ] Picture-in-Picture mode verified
- [ ] Performance benchmarks met

## 🚀 Deployment

### 1. File Organization
```
app/src/main/assets/
├── templates/
│   ├── birthday/
│   │   ├── template_1.html
│   │   └── template_1.css
│   ├── anniversary/
│   │   ├── template_2.html
│   │   └── template_2.css
│   └── general/
│       ├── template_3.html
│       └── template_3.css
├── shared/
│   ├── edge_to_edge_base.css
│   └── festive_animations.css
└── examples/
    └── sample_edge_to_edge_template.html
```

### 2. Version Management
- Use semantic versioning for template updates
- Maintain backward compatibility
- Document breaking changes
- Test with existing user data

### 3. A/B Testing
- Create template variants for testing
- Monitor user engagement metrics
- Collect feedback on design preferences
- Iterate based on performance data

## 📚 Resources

### **Material 3 Documentation**
- [Material Design 3](https://m3.material.io/)
- [Color System](https://m3.material.io/styles/color/overview)
- [Typography](https://m3.material.io/styles/typography/overview)

### **Web Standards**
- [CSS Viewport Units](https://developer.mozilla.org/en-US/docs/Web/CSS/viewport)
- [Safe Area Insets](https://developer.mozilla.org/en-US/docs/Web/CSS/env)
- [Picture-in-Picture API](https://developer.mozilla.org/en-US/docs/Web/API/Picture-in-Picture_API)

### **Accessibility Guidelines**
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [Mobile Accessibility](https://www.w3.org/WAI/mobile/)
- [Touch Target Guidelines](https://www.w3.org/WAI/WCAG21/Understanding/target-size.html)

---

**🎉 Happy template creating! Make every greeting card a delightful, edge-to-edge experience! 🎉** 