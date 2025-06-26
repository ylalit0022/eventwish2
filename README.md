# Profile Photo System

## Overview
A responsive profile photo system that provides dynamic, consistent photo displays across all screen sizes. The system includes support for placeholders, loading states, and various visual effects.

## Documentation
- [Profile Photo Styling Guide](docs/profile_photo_styling.md) - Comprehensive documentation of the styling system

## Key Features
- Dynamic sizing with maximum dimensions
- Circular photo display with border effects
- Consistent placeholder handling
- Loading state animations
- Hover effects
- Mobile-responsive adjustments

## Quick Start

### Basic Usage
```html
<div class="user-profile">
    <img class="user-photo" src="user_photo.jpg" alt="User Photo">
    <div class="user-name">John Doe</div>
</div>
```

### With Placeholder
```html
<div class="user-profile">
    <div class="user-photo-placeholder">
        <!-- Placeholder content -->
    </div>
    <div class="user-name">Loading...</div>
</div>
```

## Browser Support
- Modern browsers (Chrome, Firefox, Safari, Edge)
- IE11+ (with some limitations)

## Contributing
Please read our [Contributing Guide](docs/CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
