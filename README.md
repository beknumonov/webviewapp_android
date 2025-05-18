# WebViewApp - Enhanced Android WebView Application

A modern, feature-rich Android WebView application built with Kotlin that provides seamless web-native integration with advanced file upload capabilities, permission management, and optimal performance.

## 🌟 Features

### 📱 Core WebView Functionality
- **Modern WebView Implementation** - Hardware-accelerated rendering with optimal performance
- **Multi-Window Support** - Handle popup windows and child WebViews seamlessly
- **JavaScript Integration** - Bidirectional communication between web and native code
- **Session Management** - User login/logout handling with persistent preferences
- **Smart Navigation** - Intelligent back button handling with WebView history

### 📷 File Upload System
- **Camera Integration** - Direct camera capture for file uploads
- **Gallery Selection** - Choose images from device gallery
- **System Camera Detection** - Automatically detects and uses the best available camera app
- **Secure File Handling** - Uses FileProvider for secure file sharing
- **Multiple Upload Support** - Handles both single and multiple file uploads

### 🔒 Security & Permissions
- **Runtime Permissions** - Modern permission handling using Activity Result API
- **Secure WebView Configuration** - Prevents XSS attacks and unauthorized file access
- **HTTPS Enforcement** - Configurable mixed content handling
- **URL Scheme Validation** - Safe handling of deep links and custom schemes

### ⚡ Performance Optimizations
- **Hardware Acceleration** - GPU-accelerated rendering for smooth scrolling
- **Memory Management** - Proper cleanup to prevent memory leaks
- **Efficient Caching** - Optimized cache strategy for better loading times
- **Background Task Handling** - Non-blocking UI operations

## 📋 System Requirements

- **Minimum SDK**: API 21 (Android 5.0 Lollipop)
- **Target SDK**: API 34 (Android 14)
- **Architecture Support**: ARM64, ARMv7, x86, x86_64
- **Required Storage**: ~50MB for app installation
- **RAM**: Minimum 2GB recommended

## 🚀 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/beknumonov/webviewapp.git
cd webviewapp
```

### 2. Android Studio Setup

1. Open the project in Android Studio Arctic Fox or newer
2. Sync project with Gradle files
3. Ensure you have the latest Android SDK installed

### 3. Dependencies Configuration

The app uses AndroidX libraries. Add to your `gradle.properties`:

```properties
android.useAndroidX=true
android.enableJetifier=true
```

### 4. Build Configuration

Minimum `build.gradle (Module: app)` configuration:

```gradle
android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.beknumonov.webviewapp"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = '1.8'
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'androidx.core:core-ktx:1.13.0'
    implementation 'androidx.activity:activity-ktx:1.9.1'
    implementation 'androidx.webkit:webkit:1.11.0'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'com.afollestad.material-dialogs:core:3.3.0'
}
```

## 🎯 Usage Guide

### Basic Implementation

Launch the app with a custom URL:

```kotlin
val intent = Intent(this, MainActivity::class.java)
intent.putExtra("url", "https://your-web-app.com")
startActivity(intent)
```

### JavaScript Bridge

The app exposes a JavaScript interface for web-native communication:

```javascript
// Notify native app when user logs in
CallbackWebInterface.loginFinished("user123");

// Notify native app when user logs out
CallbackWebInterface.logoutFinished("user123");
```

### File Upload Integration

The WebView automatically handles HTML file input elements:

```html
<!-- Camera capture -->
<input type="file" accept="image/*" capture="camera" />

<!-- Gallery selection -->
<input type="file" accept="image/*" />

<!-- Multiple files -->
<input type="file" accept="image/*" multiple />
```

### Deep Link Support

The app handles various URL schemes:

- **HTTP/HTTPS**: Standard web URLs
- **Intent Schemes**: `intent://` for launching other apps
- **Custom Schemes**: `myapp://` for deep linking

## 🏗️ Architecture Overview

### Core Components

```
MainActivity
├── WebViewClient (URL handling, page events)
├── WebChromeClient (file uploads, console logs)
├── WebAppInterface (JavaScript bridge)
├── Activity Result Launchers (camera, gallery, permissions)
└── Permission Manager (runtime permissions)
```

### Key Classes

- **`MainActivity`**: Main activity hosting the WebView
- **`WebViewClient`**: Handles URL interception and page lifecycle
- **`WebChromeClient`**: Manages file choosers, console messages, and new windows
- **`WebAppInterface`**: JavaScript ↔ Native communication bridge
- **`IImagePickerLister`**: Interface for image selection callbacks

### State Management

- **User Session**: Managed through `PreferencesManager`
- **File Uploads**: Handled via `ValueCallback` system
- **Navigation**: Smart back button with WebView history
- **Permissions**: Dynamic runtime permission requests

## 🔧 Configuration Options

### WebView Settings

```kotlin
webView.settings.apply {
    javaScriptEnabled = true
    loadWithOverviewMode = true
    useWideViewPort = true
    setSupportZoom(true)
    builtInZoomControls = true
    displayZoomControls = false
    cacheMode = WebSettings.LOAD_DEFAULT
    
    // Security settings
    allowFileAccess = false
    allowContentAccess = false
    allowFileAccessFromFileURLs = false
    allowUniversalAccessFromFileURLs = false
}
```

### File Provider Configuration

Create `res/xml/file_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-files-path 
        name="images" 
        path="webviewapp/" />
</paths>
```

Update `AndroidManifest.xml`:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.provider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### Required Permissions

Add to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## 🛡️ Security Features

### WebView Security
- Disabled unnecessary file access permissions
- XSS protection through content security policies
- Secure JavaScript interface with `@JavascriptInterface` annotation
- HTTPS enforcement with configurable mixed content handling

### File Security
- FileProvider for secure file URI sharing
- Scoped storage compliance for Android 10+
- Secure temporary file creation and cleanup
- Permission-based file access control

### Intent Security
- Validated intent URL parsing
- Safe activity resolution checking
- Sandboxed custom scheme handling
- Market redirect for unavailable apps

## 📱 Platform Support

### Android Versions
- ✅ Android 5.0+ (API 21)
- ✅ Android 6.0+ (API 23) - Runtime permissions
- ✅ Android 7.0+ (API 24) - FileProvider mandatory
- ✅ Android 10+ (API 29) - Scoped storage
- ✅ Android 13+ (API 33) - Notification permissions

### Device Features
- 📷 Camera (auto-detects best available)
- 🖼️ Gallery access
- 📁 External storage (scoped)
- 🔔 Push notifications
- 🌐 Network connectivity
- 🎯 Hardware acceleration
- 📱 Multiple window sizes

## 🐛 Troubleshooting

### Common Issues

#### 1. **Build Errors - Dependency Conflicts**
```bash
# Solution: Clean and rebuild
./gradlew clean
./gradlew build
```

#### 2. **File Upload Not Working**
- Verify FileProvider configuration in manifest
- Check file_paths.xml contains correct paths
- Ensure camera permissions are granted
- Confirm system camera app is available

#### 3. **JavaScript Bridge Issues**
- Ensure `javaScriptEnabled = true`
- Verify `@JavascriptInterface` annotations
- Check JavaScript calls use exact method names
- Enable WebView debugging: `WebView.setWebContentsDebuggingEnabled(true)`

#### 4. **Permission Denied Errors**
- Check runtime permission implementation
- Verify permission declarations in manifest
- Handle permission denial gracefully
- Test on different Android versions

#### 5. **WebView Loading Issues**
- Check network connectivity
- Verify URL format (include https://)
- Enable mixed content if needed
- Check WebView console for JavaScript errors

### Debug Mode

Enable debugging in development:

```kotlin
if (BuildConfig.DEBUG) {
    WebView.setWebContentsDebuggingEnabled(true)
}
```

Access WebView debugging:
1. Open Chrome browser
2. Navigate to `chrome://inspect`
3. Look for your app's WebView

## 📊 Performance Tips

### Memory Optimization
- Call `webView.destroy()` in `onDestroy()`
- Remove JavaScript interfaces when not needed
- Clear WebView cache periodically
- Use hardware acceleration

### Loading Performance
- Enable WebView caching
- Optimize web content (minify JS/CSS)
- Use WebP images for better compression
- Implement progressive loading for large pages

### Battery Optimization
- Pause WebView when app is backgrounded
- Disable auto-playing media
- Use efficient image formats
- Minimize JavaScript execution in background

## 🔄 Migration Notes

### From Support Library to AndroidX

If migrating from older versions:

1. Update `gradle.properties`:
```properties
android.useAndroidX=true
android.enableJetifier=true
```

2. Replace imports:
```kotlin
// OLD
import android.support.v7.app.AppCompatActivity
import android.support.v4.content.ContextCompat

// NEW
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
```

3. Update dependencies to AndroidX equivalents

## 📈 Roadmap

### Planned Features
- [ ] Dark mode support
- [ ] Offline mode with cache management
- [ ] Advanced security headers configuration
- [ ] Progressive Web App (PWA) support
- [ ] Biometric authentication integration
- [ ] Crash reporting integration
- [ ] Analytics integration
- [ ] WebRTC support for video calls

### Performance Improvements
- [ ] Lazy loading optimization
- [ ] Image compression pipeline
- [ ] Background sync capabilities
- [ ] Memory usage profiling
- [ ] Battery usage optimization

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/beknumonov/webviewapp/issues)
- **Email**: support@beknumonov.com

---

## 🙏 Acknowledgments

- [Material Dialogs](https://github.com/afollestad/material-dialogs) for beautiful dialog implementation
- [AndroidX](https://developer.android.com/jetpack/androidx) for modern Android development
- [WebView](https://developer.android.com/guide/webapps/webview) documentation and best practices

---

*Built with ❤️ by [Sardorbek Numonov](https://github.com/beknumonov)*