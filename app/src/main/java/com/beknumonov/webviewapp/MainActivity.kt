package com.beknumonov.webviewapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.afollestad.materialdialogs.BuildConfig
import com.afollestad.materialdialogs.MaterialDialog
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.util.*

class MainActivity : AppCompatActivity(), IImagePickerLister {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val BACK_PRESS_DELAY = 2000L
        private const val DEFAULT_URL = "https://www.linkedin.com/"
        private const val PROVIDER_AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider"
        private const val IMAGE_STORAGE_DIR = "webviewapp"
        
        // Permission request codes
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
    }
    
    // Views
    private lateinit var webView: WebView
    private lateinit var childWebView: WebView
    private lateinit var splashImage: ImageView
    
    // File handling
    private var imageUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var mUploadCallbackAboveL: ValueCallback<Array<Uri>>? = null
    private var mUploadCallbackBelow: ValueCallback<Uri>? = null
    
    // State management
    private var isOptionSelected = false
    private var singleBack = false
    private val backPressHandler = Handler(Looper.getMainLooper())
    private val resetBackPressRunnable = Runnable { singleBack = false }
    
    // Activity result launchers using new API
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupActivityResultLaunchers()
        setupUI()
        configureWebView()
        requestPermissions()
        loadInitialUrl()
    }
    
    private fun setupActivityResultLaunchers() {
        // Camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleCameraResult(result.resultCode, result.data)
        }
        
        // Gallery launcher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            handleGalleryResult(uri)
        }
        
        // Permission launcher
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionResults(permissions)
        }
    }
    
    private fun setupUI() {
        setContentView(R.layout.activity_main)
        
        // Handle edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize views
        webView = findViewById(R.id.webView)
        childWebView = findViewById(R.id.childWebview)
        splashImage = findViewById(R.id.splashImage)
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.apply {
            settings.apply {
                javaScriptEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false // Hide zoom controls UI
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // Security settings
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                
                // Performance optimizations
                setRenderPriority(WebSettings.RenderPriority.HIGH)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
            }
            
            // Hardware acceleration
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            webViewClient = createWebViewClient()
            webChromeClient = createWebChromeClient()
            
            addJavascriptInterface(WebAppInterface(this@MainActivity), "CallbackWebInterface")
        }
        
        // Configure child webview
        childWebView.settings.useWideViewPort = true
        
        // Configure cookies
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(webView, true)
            }
        }
    }
    
    private fun createWebViewClient() = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false
            return handleUrlLoading(url)
        }
        
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            return url?.let { handleUrlLoading(it) } ?: false
        }
        
        override fun onPageFinished(view: WebView?, url: String?) {
            webView.visibility = View.VISIBLE
            splashImage.visibility = View.GONE
        }
        
        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            Log.e(TAG, "WebView error: ${error?.description}")
        }
    }
    
    private fun createWebChromeClient() = object : WebChromeClient() {
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
            
            childWebView.apply {
                settings.useWideViewPort = true
                webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView?) {
                        if (window == childWebView) {
                            webView.visibility = View.VISIBLE
                            childWebView.visibility = View.GONE
                        }
                    }
                }
                visibility = View.VISIBLE
                transport.webView = this
            }
            
            webView.visibility = View.GONE
            resultMsg.sendToTarget()
            return true
        }
        
        override fun onShowFileChooser(
            webView: WebView?,
            valueCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            mUploadCallbackAboveL = valueCallback
            
            if (fileChooserParams?.isCaptureEnabled == true) {
                takePhoto()
            } else {
                showImagePickerDialog()
            }
            return true
        }
        
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            Log.d(TAG, "Console: ${consoleMessage?.message()}")
            return true
        }
    }
    
    private fun handleUrlLoading(url: String): Boolean {
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> false
            url.startsWith("intent://") -> handleIntentUrl(url)
            else -> handleCustomScheme(url)
        }
    }
    
    private fun handleIntentUrl(url: String): Boolean {
        return try {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            
            if (packageManager.resolveActivity(intent, 0) != null) {
                startActivity(intent)
                true
            } else {
                intent.`package`?.let { packageName ->
                    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                    startActivity(marketIntent)
                    true
                } ?: false
            }
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Failed to parse intent URL: $url", e)
            false
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No activity found for intent URL: $url", e)
            false
        }
    }
    
    private fun handleCustomScheme(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle custom scheme: $url", e)
            Toast.makeText(this, "Failed to open link", Toast.LENGTH_SHORT).show()
            false
        }
    }
    
    private fun showImagePickerDialog() {
        MaterialDialog.Builder(this)
            .items(R.array.imagePicker)
            .title(R.string.app_name)
            .canceledOnTouchOutside(true)
            .itemsCallback { dialog, _, position, _ ->
                when (position) {
                    0 -> onOptionSelected(ImagePickerEnum.FROM_GALLERY)
                    1 -> onOptionSelected(ImagePickerEnum.FROM_CAMERA)
                }
                dialog.dismiss()
            }
            .dismissListener {
                if (!isOptionSelected) {
                    mUploadCallbackAboveL?.onReceiveValue(null)
                    mUploadCallbackAboveL = null
                }
            }
            .show()
    }

    
    private fun takePhoto() {
        if (!hasRequiredPermissions()) {
            requestPermissions()
            return
        }
        
        val fileName = "IMG_${DateFormat.format("yyyyMMdd_hhmmss", Calendar.getInstance(Locale.getDefault()))}.jpg"
        val file = createImageFile(fileName)
        
        imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(this, PROVIDER_AUTHORITY, file)
        } else {
            Uri.fromFile(file)
        }
        
        val systemCamera = findSystemCamera()
        if (systemCamera != null) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                `package` = systemCamera
                putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            }
            cameraLauncher.launch(intent)
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }
    
    private fun createImageFile(fileName: String): File {
        val storageDir = File(getExternalFilesDir(null), IMAGE_STORAGE_DIR)
        
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        return try {
            File.createTempFile(fileName, ".jpg", storageDir).also { file ->
                currentPhotoPath = "file:${file.absolutePath}"
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create image file", e)
            throw RuntimeException("Failed to create image file", e)
        }
    }
    
    private fun handleCameraResult(resultCode: Int, data: Intent?) {
        isOptionSelected = false
        
        if (resultCode == Activity.RESULT_OK) {
            updatePhotos()
            val resultUri = imageUri
            
            if (resultUri != null) {
                mUploadCallbackAboveL?.onReceiveValue(arrayOf(resultUri))
            } else {
                mUploadCallbackAboveL?.onReceiveValue(null)
            }
        } else {
            mUploadCallbackAboveL?.onReceiveValue(null)
        }
        
        mUploadCallbackAboveL = null
    }
    
    private fun handleGalleryResult(uri: Uri?) {
        isOptionSelected = false
        
        if (uri != null) {
            imageUri = uri
            mUploadCallbackAboveL?.onReceiveValue(arrayOf(uri))
        } else {
            mUploadCallbackAboveL?.onReceiveValue(null)
        }
        
        mUploadCallbackAboveL = null
    }
    
    private fun updatePhotos() {
        imageUri?.let { uri ->
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
            sendBroadcast(intent)
        }
    }
    
    private fun findSystemCamera(): String? {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val cameraApps = packageManager.queryIntentActivities(intent, 0)
        
        // Prefer Google Camera if available
        val googleCamera = cameraApps.find { it.activityInfo.packageName == "com.google.android.camera" }
        if (googleCamera != null) {
            return googleCamera.activityInfo.packageName
        }
        
        // Otherwise, find any system camera app
        return cameraApps.find { isSystemApp(it.activityInfo.packageName) }?.activityInfo?.packageName
    }
    
    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val systemFlags = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
            (appInfo.flags and systemFlags) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        
        return permissions.all { 
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED 
        }
    }
    
    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val deniedPermissions = permissions.filter { !it.value }.keys
        
        if (deniedPermissions.isNotEmpty()) {
            Log.w(TAG, "Permissions denied: $deniedPermissions")
            // Handle denied permissions (show explanation, disable features, etc.)
        }
    }
    
    private fun loadInitialUrl() {
        val url = intent.getStringExtra("url") ?: DEFAULT_URL
        webView.loadUrl(url)
        
        // Initialize user session if logged in
        if (PreferencesManager.get(this).isLoggedIn) {
            PreferencesManager.get(this).userId.let { userId ->
                subscribeTopic(userId!!)
            }
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            childWebView.visibility == View.VISIBLE -> {
                webView.visibility = View.VISIBLE
                childWebView.visibility = View.GONE
            }
            webView.canGoBack() -> webView.goBack()
            singleBack -> super.onBackPressed()
            else -> {
                singleBack = true
                Toast.makeText(this, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다", Toast.LENGTH_SHORT).show()
                backPressHandler.postDelayed(resetBackPressRunnable, BACK_PRESS_DELAY)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        backPressHandler.removeCallbacks(resetBackPressRunnable)
        webView.destroy()
    }
    
    inner class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun loginFinished(userId: String?) {
            if (!userId.isNullOrEmpty()) {
                PreferencesManager.get(context).loginFinished(userId)
                subscribeTopic(userId)
            }
        }
        
        @JavascriptInterface
        fun logoutFinished(userId: String) {
            unsubscribeTopic(userId)
            PreferencesManager.get(context).setLogout()
        }
    }
    
    private fun subscribeTopic(userId: String) {
        // TODO: Implement Firebase messaging subscription
        Log.d(TAG, "Subscribe to topic: users-$userId")
    }
    
    private fun unsubscribeTopic(userId: String) {
        // TODO: Implement Firebase messaging unsubscription
        Log.d(TAG, "Unsubscribe from topic: users-$userId")
    }

    override fun onOptionSelected(imagePickerEnum: ImagePickerEnum?) {
        isOptionSelected = true
        when (imagePickerEnum) {
            ImagePickerEnum.FROM_CAMERA -> takePhoto()
            ImagePickerEnum.FROM_GALLERY -> openGallery()
            else -> {}
        }
    }
}