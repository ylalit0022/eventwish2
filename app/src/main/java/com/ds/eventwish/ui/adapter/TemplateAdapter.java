package com.ds.eventwish.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.remote.TemplateInteractionManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import androidx.core.content.ContextCompat;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.SubscriptSpan;
import android.text.style.TextAppearanceSpan;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.material.imageview.ShapeableImageView;
import com.ds.eventwish.utils.TemplateOverlayHelper;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import android.widget.FrameLayout;
import androidx.media3.ui.PlayerView;
import android.webkit.WebViewClient;
import android.graphics.Bitmap;
import android.view.Gravity;
import com.ds.eventwish.BuildConfig;
import android.os.Build;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.ViewHolder> {

    private final Context context;
    private ArrayList<Template> templates;
    private final TemplateInteractionManager interactionManager;
    private OnItemClickListener onItemClickListener;
    private OnTemplateInteractionListener onTemplateInteractionListener;
    private static final long CLICK_DEBOUNCE_TIME = 800; // ms - longer debounce time for network operations
    private final Map<String, Long> lastClickTimes = new HashMap<>();
    private static final String TAG = "TemplateAdapter";
    
    // Add debouncing for setTemplates to prevent excessive updates
    private long lastSetTemplatesTime = 0;
    private static final long SET_TEMPLATES_DEBOUNCE_TIME = 1000; // 1 second debounce
    
    // Video auto-play management
    private RecyclerView recyclerView;
    private ViewHolder currentPlayingVideoHolder;
    private String currentPlayingVideoId;
    private boolean isAutoPlayEnabled = true;
    private static final float VISIBILITY_THRESHOLD = 0.5f; // 50% visibility required for auto-play

    // Cache for user display name and profile photo
    private static String cachedUserName = null;
    private static String cachedUserPhotoUrl = null;
    private static long lastNameFetchTime = 0;
    private static long lastPhotoFetchTime = 0;
    private static final long NAME_CACHE_DURATION = 5 * 60 * 1000; // 5 minutes cache
    private static final long PHOTO_CACHE_DURATION = 10 * 60 * 1000; // 10 minutes cache for photos

    // Cache for creator profiles
    private static final Map<String, CreatorProfile> creatorProfileCache = new HashMap<>();
    private static final long CREATOR_CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

    private static class CreatorProfile {
        String displayName;
        String photoUrl;
        long timestamp;

        CreatorProfile(String displayName, String photoUrl) {
            this.displayName = displayName;
            this.photoUrl = photoUrl;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CREATOR_CACHE_DURATION;
        }
    }

    public TemplateAdapter(Context context) {
        this.context = context;
        this.templates = new ArrayList<>();
        this.interactionManager = TemplateInteractionManager.getInstance();
        
        // Get and log current user's name from Firebase Auth
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            String email = currentUser.getEmail();
            String uid = currentUser.getUid();
            
            Log.d(TAG, "Current user info from Firebase Auth:");
            Log.d(TAG, "Display Name: " + (displayName != null ? displayName : "Not set"));
            Log.d(TAG, "Email: " + (email != null ? email : "Not set"));
            Log.d(TAG, "UID: " + uid);
            
            // Check if user is signed in with Google
            boolean isGoogleUser = false;
            for (com.google.firebase.auth.UserInfo profile : currentUser.getProviderData()) {
                if ("google.com".equals(profile.getProviderId())) {
                    isGoogleUser = true;
                    Log.d(TAG, "User is signed in with Google");
                    break;
                }
            }
            if (!isGoogleUser) {
                Log.d(TAG, "User is not signed in with Google");
            }
        } else {
            Log.d(TAG, "No user currently signed in");
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_template, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        // Get template with validation
        if (templates == null || position >= templates.size()) {
            Log.e(TAG, "🚨 Invalid template list or position: " + position);
            holder.cardView.setVisibility(View.GONE);
            return;
        }

        Template template = templates.get(position);
        if (template == null) {
            Log.e(TAG, "🚨 Null template at position " + position);
            holder.cardView.setVisibility(View.GONE);
            return;
        }

        // Validate template ID
        String templateId = template.getId();
        if (templateId == null || templateId.trim().isEmpty()) {
            Log.e(TAG, "🚨 Template at position " + position + " has null/empty ID. Title: " + template.getTitle());
            holder.cardView.setVisibility(View.GONE);
            return;
        }

        try {
            // Show card and proceed with binding
            holder.cardView.setVisibility(View.VISIBLE);
            Log.d(TAG, "🎯 Binding template: " + templateId + " at position " + position);

            // Set up interaction listeners with validated template
            setupInteractionListeners(holder, template);

            // Render template content based on type
            renderTemplateByType(holder, template);

            // Load creator profile
            loadCreatorProfile(holder, template);

            // Update interaction states
            updateInteractionStates(holder, template);

            Log.d(TAG, "✅ Successfully bound template: " + templateId);

        } catch (Exception e) {
            Log.e(TAG, "🚨 Error binding template: " + templateId + " at position " + position, e);
            holder.cardView.setVisibility(View.GONE);
        }
    }
    
    private void loadCreatorProfile(@NonNull ViewHolder holder, @NonNull Template template) {
        // Get creator UID from template
        String creatorUid = template.getCreatorUid();
        
        // If no creator ID, show default app branding
        if (creatorUid == null || creatorUid.isEmpty()) {
            setDefaultProfile(holder);
            return;
        }

        // Get creator info from Firebase
        FirebaseFirestore.getInstance()
            .collection("users")
            .whereEqualTo("uid", creatorUid)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                    String displayName = userDoc.getString("displayName");
                    String profilePhoto = userDoc.getString("profilePhoto");
                    
                    // If either display name or photo is missing, use default
                    if (displayName == null || displayName.isEmpty() || profilePhoto == null || profilePhoto.isEmpty()) {
                        setDefaultProfile(holder);
                        return;
                    }
                    
                    // Set profile with creator info
                    holder.usernameText.setText(displayName);
                    Glide.with(holder.itemView.getContext())
                        .load(profilePhoto)
                        .placeholder(R.drawable.app_logo)
                        .error(R.drawable.app_logo)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.profileImage);
                } else {
                    // User not found, use default
                    setDefaultProfile(holder);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading creator profile", e);
                setDefaultProfile(holder);
            });
    }

    private void setDefaultProfile(@NonNull ViewHolder holder) {
        // Set app logo
        holder.profileImage.setImageResource(R.drawable.app_logo);
        // Set app name
        holder.usernameText.setText(R.string.app_name);
    }
    
    /**
     * 🎨 Render template based on its type (HTML, VIDEO, IMAGE)
     * This is the main method that handles type-based rendering
     */
    private void renderTemplateByType(@NonNull ViewHolder holder, @NonNull Template template) {
        String templateType = template.getTemplateType();
        if (templateType == null || templateType.isEmpty()) {
            templateType = "image"; // Default to image if not specified
        }
        
        Log.d(TAG, "🎨 RENDERING TEMPLATE TYPE: " + templateType + " for template: " + template.getId());
        
        // Hide all template containers first
        holder.imageTemplateContainer.setVisibility(View.GONE);
        holder.htmlTemplateContainer.setVisibility(View.GONE);
        holder.videoTemplateContainer.setVisibility(View.GONE);
        
        // Show template type badge
        holder.templateTypeBadge.setVisibility(View.VISIBLE);
        
        // Render based on template type
        switch (templateType.toLowerCase()) {
            case "html":
                renderHtmlTemplate(holder, template);
                break;
            case "video":
                renderVideoTemplate(holder, template);
                break;
            case "image":
            default:
                renderImageTemplate(holder, template);
                break;
        }
    }
    
    /**
     * 🖼️ Render IMAGE template with HTML/CSS overlay support
     */
    private void renderImageTemplate(@NonNull ViewHolder holder, @NonNull Template template) {
        try {
            Log.d(TAG, "🖼️ Starting image template rendering for template: " + template.getId());
            Log.d(TAG, "   • Template type: " + template.getTemplateType());
            Log.d(TAG, "   • Image URL: " + template.getImageUrl());
            Log.d(TAG, "   • Preview URL: " + template.getPreviewUrl());
            Log.d(TAG, "   • Title: " + template.getTitle());
            
            // Show image container and hide others
            if (holder.imageTemplateContainer != null) {
                holder.imageTemplateContainer.setVisibility(View.VISIBLE);
                Log.d(TAG, "✅ Image container made visible");
            }
            if (holder.htmlTemplateContainer != null) {
                holder.htmlTemplateContainer.setVisibility(View.GONE);
            }
            if (holder.videoTemplateContainer != null) {
                holder.videoTemplateContainer.setVisibility(View.GONE);
            }

            holder.templateTypeBadge.setText("IMAGE");
            
            // Load image with enhanced options
            String imageUrl = template.getPreviewUrl();
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = template.getImageUrl();
            }
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Log.d(TAG, "🖼️ Loading image from URL: " + imageUrl);
                Glide.with(context)
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(holder.templateImage);
            } else {
                Log.w(TAG, "🖼️ No image URL available for template: " + template.getId());
                holder.templateImage.setImageResource(R.drawable.placeholder_image);
            }

            // Get user information for overlay
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String userName = currentUser != null ? currentUser.getDisplayName() : null;
            String photoUrl = currentUser != null && currentUser.getPhotoUrl() != null ? 
                            currentUser.getPhotoUrl().toString() : null;
            String userId = currentUser != null ? currentUser.getUid() : null;

            Log.d(TAG, "👤 User info for image overlay:");
            Log.d(TAG, "   • User Name: " + (userName != null ? userName : "null"));
            Log.d(TAG, "   • Photo URL: " + (photoUrl != null ? photoUrl : "null"));
            Log.d(TAG, "   • User ID: " + (userId != null ? userId : "null"));

            // Check for HTML/CSS content for image overlay (use main content fields)
            String htmlContent = template.getHtmlContent();
            String cssContent = template.getCssContent();
            
            Log.d(TAG, "📄 Template content analysis:");
            Log.d(TAG, "   • HTML Content length: " + (htmlContent != null ? htmlContent.length() : 0));
            Log.d(TAG, "   • CSS Content length: " + (cssContent != null ? cssContent.length() : 0));
            Log.d(TAG, "   • HTML Content preview: " + (htmlContent != null && htmlContent.length() > 50 ? 
                  htmlContent.substring(0, 50) + "..." : htmlContent));
            Log.d(TAG, "   • CSS Content preview: " + (cssContent != null && cssContent.length() > 50 ? 
                  cssContent.substring(0, 50) + "..." : cssContent));

            // Also check overlay-specific fields for comparison
            String overlayHtml = template.getOverlayHtmlTemplate();
            String overlayCss = template.getOverlayCssTemplate();
            Log.d(TAG, "🎭 Overlay-specific fields:");
            Log.d(TAG, "   • Overlay HTML length: " + (overlayHtml != null ? overlayHtml.length() : 0));
            Log.d(TAG, "   • Overlay CSS length: " + (overlayCss != null ? overlayCss.length() : 0));

            // Create overlay using HTML/CSS content if available
            if (htmlContent != null && !htmlContent.trim().isEmpty() && 
                cssContent != null && !cssContent.trim().isEmpty()) {
                Log.d(TAG, "🎨 Found HTML/CSS content, rendering image overlay...");
                renderImageOverlayWithContent(holder, template, htmlContent, cssContent);
            } else if (overlayHtml != null && !overlayHtml.trim().isEmpty() && 
                      overlayCss != null && !overlayCss.trim().isEmpty()) {
                Log.d(TAG, "🎭 Found overlay templates, rendering image overlay...");
                renderImageOverlayWithContent(holder, template, overlayHtml, overlayCss);
            } else {
                Log.w(TAG, "❌ No valid HTML/CSS content found for image overlay");
                Log.w(TAG, "   • Main HTML Content: " + (htmlContent == null ? "null" : 
                          (htmlContent.trim().isEmpty() ? "empty" : "present (" + htmlContent.length() + " chars)")));
                Log.w(TAG, "   • Main CSS Content: " + (cssContent == null ? "null" : 
                          (cssContent.trim().isEmpty() ? "empty" : "present (" + cssContent.length() + " chars)")));
                Log.w(TAG, "   • Overlay HTML: " + (overlayHtml == null ? "null" : 
                          (overlayHtml.trim().isEmpty() ? "empty" : "present (" + overlayHtml.length() + " chars)")));
                Log.w(TAG, "   • Overlay CSS: " + (overlayCss == null ? "null" : 
                          (overlayCss.trim().isEmpty() ? "empty" : "present (" + overlayCss.length() + " chars)")));
                
                // Hide overlay container if no content
                if (holder.imageOverlayContainer != null) {
                    holder.imageOverlayContainer.setVisibility(View.GONE);
                    Log.d(TAG, "🚫 Image overlay container hidden due to no content");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error rendering image template", e);
            e.printStackTrace();
        }
    }
    
    /**
     * 🎨 Render image overlay with HTML/CSS content and enhanced debugging
     */
    private void renderImageOverlayWithContent(@NonNull ViewHolder holder, @NonNull Template template, 
                                             @NonNull String htmlContent, @NonNull String cssContent) {
        Log.d(TAG, "🎨 Starting image overlay rendering with content for template: " + template.getId());
        
        try {
            // Get user information
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.w(TAG, "⚠️ No user logged in for image overlay");
                return;
            }
            
            String userName = getUserDisplayName();
            String userPhoto = getUserPhotoUrl();
            String userId = currentUser.getUid();
            
            Log.d(TAG, "👤 User data for image overlay:");
            Log.d(TAG, "   • Name: " + userName);
            Log.d(TAG, "   • Photo: " + (userPhoto != null ? userPhoto : "null"));
            Log.d(TAG, "   • UID: " + userId);
            
            Log.d(TAG, "🎨 Content data for image overlay:");
            Log.d(TAG, "   • HTML Content length: " + htmlContent.length());
            Log.d(TAG, "   • CSS Content length: " + cssContent.length());
            Log.d(TAG, "   • HTML Content preview: " + (htmlContent.length() > 100 ? 
                  htmlContent.substring(0, 100) + "..." : htmlContent));
            
            // Create photo HTML with proper null checking and comprehensive fallback
            String photoHtml;
            if (userPhoto != null && !userPhoto.trim().isEmpty()) {
                photoHtml = "<img class='user-photo' src='" + userPhoto + "' alt='" + (userName != null ? userName : "User") + "'>";
                Log.d(TAG, "👤 Using actual user photo: " + userPhoto);
            } else {
                photoHtml = "<img class='user-photo' src='file:///android_res/drawable/app_logo' alt='User Photo'>";
                Log.d(TAG, "👤 No user photo available, using app logo");
            }
                
            // Replace placeholders with comprehensive format support
            Log.d(TAG, "🔄 Replacing placeholders in HTML content...");
            
            String processedHtml = htmlContent
                // Support multiple placeholder formats: [USER_*], {USER_*}, {{USER_*}}
                .replace("[USER_PHOTO]", photoHtml)
                .replace("[USER_NAME]", userName != null ? userName : "User")
                .replace("[USER_ID]", userId != null ? userId : "")
                .replace("{USER_PHOTO}", photoHtml)
                .replace("{USER_NAME}", userName != null ? userName : "User")
                .replace("{USER_ID}", userId != null ? userId : "")
                .replace("{{USER_PHOTO}}", photoHtml)
                .replace("{{USER_NAME}}", userName != null ? userName : "User")
                .replace("{{USER_ID}}", userId != null ? userId : "")
                // Additional common formats
                .replace("{{userPhoto}}", photoHtml)
                .replace("{{userName}}", userName != null ? userName : "User")
                .replace("{{userId}}", userId != null ? userId : "")
                .replace("[userPhoto]", photoHtml)
                .replace("[userName]", userName != null ? userName : "User")
                .replace("[userId]", userId != null ? userId : "");
                
            Log.d(TAG, "✅ Placeholders replaced. Processed HTML length: " + processedHtml.length());
            
            // Enhanced CSS with debugging styles and improved visibility
            String enhancedCss = cssContent;
                
            // Create complete HTML document
            String completeHtml = String.format(
                "<!DOCTYPE html><html><head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>%s</style>" +
                "</head><body>%s</body></html>",
                enhancedCss,
                processedHtml
            );
            
            Log.d(TAG, "📄 Complete HTML document created. Length: " + completeHtml.length());
            
            // Ensure overlay container is visible and properly configured
            if (holder.imageOverlayContainer != null) {
                holder.imageOverlayContainer.setVisibility(View.VISIBLE);
                holder.imageOverlayContainer.removeAllViews();
                
                Log.d(TAG, "📱 Image overlay container prepared:");
                Log.d(TAG, "   • Visibility: " + (holder.imageOverlayContainer.getVisibility() == View.VISIBLE ? "VISIBLE" : "HIDDEN"));
                Log.d(TAG, "   • Width: " + holder.imageOverlayContainer.getLayoutParams().width);
                Log.d(TAG, "   • Height: " + holder.imageOverlayContainer.getLayoutParams().height);
                
                // Create and configure WebView with enhanced settings
                WebView overlayWebView = new WebView(holder.itemView.getContext());
                
                // Enhanced WebView settings for overlay rendering
                WebSettings webSettings = overlayWebView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setLoadWithOverviewMode(true);
                webSettings.setUseWideViewPort(true);
                webSettings.setBuiltInZoomControls(false);
                webSettings.setDisplayZoomControls(false);
                webSettings.setSupportZoom(false);
                webSettings.setTextZoom(100);
                webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                
                // Enable debugging for WebView
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    WebView.setWebContentsDebuggingEnabled(true);
                }
                
                // Configure WebView layout parameters
                FrameLayout.LayoutParams webViewParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                );
                webViewParams.gravity = Gravity.BOTTOM;
                overlayWebView.setLayoutParams(webViewParams);
                
                // Set WebView background and styling
                overlayWebView.setBackgroundColor(Color.TRANSPARENT);
                overlayWebView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
                
                // Enhanced WebViewClient with comprehensive debugging
                overlayWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        super.onPageStarted(view, url, favicon);
                        Log.d(TAG, "🌐 Image overlay WebView page started loading: " + url);
                    }
                    
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        Log.d(TAG, "✅ Image overlay WebView page finished loading: " + url);
                        

                        
                        // Force container visibility check after page load
                        view.post(() -> {
                            Log.d(TAG, "📱 Post-load image overlay container check:");
                            Log.d(TAG, "   • Container visibility: " + 
                                  (holder.imageOverlayContainer.getVisibility() == View.VISIBLE ? "VISIBLE" : "HIDDEN"));
                            Log.d(TAG, "   • WebView visibility: " + 
                                  (view.getVisibility() == View.VISIBLE ? "VISIBLE" : "HIDDEN"));
                            Log.d(TAG, "   • Container child count: " + holder.imageOverlayContainer.getChildCount());
                        });
                    }
                    
                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        super.onReceivedError(view, errorCode, description, failingUrl);
                        Log.e(TAG, "❌ Image overlay WebView error: " + description + " (Code: " + errorCode + ")");
                        
                        // Show error overlay using CSS class instead of inline styles
                        String errorHtml = "<div class='error-message'>" +
                                          "❌ Image Overlay Error: " + description + "</div>";
                        view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
                    }
                    
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        Log.d(TAG, "🔗 Image overlay WebView URL loading: " + url);
                        return false; // Allow normal loading
                    }
                });
                
                // Add WebView to overlay container
                holder.imageOverlayContainer.addView(overlayWebView);
                
                Log.d(TAG, "📱 WebView added to image overlay container");
                
                // Load the HTML content
                Log.d(TAG, "🚀 Loading HTML content into image overlay WebView...");
                overlayWebView.loadDataWithBaseURL(
                    "https://eventwish.com/", // Base URL for relative resources
                    completeHtml,
                    "text/html",
                    "UTF-8",
                    null
                );
                
                Log.d(TAG, "✅ Image overlay rendering completed successfully!");
                
            } else {
                Log.e(TAG, "❌ Image overlay container is null - cannot render overlay");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in renderImageOverlayWithContent", e);
            e.printStackTrace();
            
            // Show error message in overlay container
            if (holder.imageOverlayContainer != null) {
                holder.imageOverlayContainer.removeAllViews();
                TextView errorView = new TextView(holder.itemView.getContext());
                errorView.setText("❌ Image Overlay Error: " + e.getMessage());
                errorView.setTextColor(Color.RED);
                errorView.setBackgroundColor(Color.WHITE);
                errorView.setPadding(16, 8, 16, 8);
                holder.imageOverlayContainer.addView(errorView);
            }
        }
    }
    
    /**
     * 🌐 Render HTML template with WebView preview
     */
    private void renderHtmlTemplate(@NonNull ViewHolder holder, @NonNull Template template) {
        Log.d(TAG, "🌐 RENDERING HTML TEMPLATE: " + template.getId());
        
        // Show HTML container
        holder.htmlTemplateContainer.setVisibility(View.VISIBLE);
        holder.templateTypeBadge.setText("HTML");
        
        // Set HTML template title
        holder.htmlTemplateTitle.setText(template.getTitle());
        
        // Configure WebView for enhanced HTML preview
        if (holder.htmlPreviewWebView != null) {
            WebSettings webSettings = holder.htmlPreviewWebView.getSettings();
            
            // Enhanced rendering settings
            webSettings.setDefaultTextEncodingName("UTF-8");
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setDisplayZoomControls(false);
            webSettings.setSupportZoom(false);
            webSettings.setBuiltInZoomControls(false);
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            webSettings.setDomStorageEnabled(true);
            webSettings.setTextZoom(100);
            
            // Security settings
            webSettings.setJavaScriptEnabled(false);
            webSettings.setAllowFileAccess(false);
            webSettings.setAllowContentAccess(false);
            webSettings.setAllowFileAccessFromFileURLs(false);
            webSettings.setAllowUniversalAccessFromFileURLs(false);
            
            // Allow network loads for images in preview
            webSettings.setBlockNetworkLoads(false);
            webSettings.setBlockNetworkImage(false);
            
            // Set WebView client for error handling and rendering completion
            holder.htmlPreviewWebView.setWebViewClient(new android.webkit.WebViewClient() {
                @Override
                public void onPageStarted(android.webkit.WebView view, String url, android.graphics.Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    Log.d(TAG, "🌐 Starting to load HTML for template: " + template.getId());
                }
                
                @Override
                public void onPageFinished(android.webkit.WebView view, String url) {
                    super.onPageFinished(view, url);
                    Log.d(TAG, "🌐 HTML page finished loading for template: " + template.getId());
                    
                    // Inject CSS for better text rendering
                    String css = "body { -webkit-text-size-adjust: none; }";
                    view.loadUrl("javascript:(function() {" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.innerHTML = '" + css + "';" +
                        "document.head.appendChild(style);" +
                    "})()");
                }
                
                @Override
                public void onReceivedError(android.webkit.WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    Log.e(TAG, "🌐 WebView error for template " + template.getId() + ": " + description);
                    renderHtmlPlaceholder(holder, template);
                }
            });
            
            // Enable hardware acceleration for better rendering
            holder.htmlPreviewWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            
            // Render HTML content
            renderHtmlContentInWebView(holder, template);
        } else {
            Log.e(TAG, "🌐 WebView is null for template: " + template.getId());
            renderHtmlPlaceholder(holder, template);
        }
        
        Log.d(TAG, "🌐 HTML template rendered with title: " + template.getTitle());
    }
    
    /**
     * Render HTML content in WebView with fallback handling
     */
    private void renderHtmlContentInWebView(@NonNull ViewHolder holder, @NonNull Template template) {
        String htmlContent = template.getHtmlContent();
        String cssContent = template.getCssContent();
        
        if (htmlContent != null && !htmlContent.trim().isEmpty()) {
            Log.d(TAG, "🌐 Rendering HTML content for template: " + template.getId());
            
            // Create responsive HTML with enhanced preview styling
            String responsiveHtml = createResponsiveHtmlForPreview(htmlContent, cssContent, template);
            
            try {
                holder.htmlPreviewWebView.loadDataWithBaseURL(
                    null, 
                    responsiveHtml, 
                    "text/html", 
                    "UTF-8", 
                    null
                );
                Log.d(TAG, "🌐 HTML content loaded successfully for template: " + template.getId());
            } catch (Exception e) {
                Log.e(TAG, "🌐 Error loading HTML content for template " + template.getId() + ": " + e.getMessage());
                renderHtmlPlaceholder(holder, template);
            }
        } else {
            Log.w(TAG, "🌐 No HTML content available for template: " + template.getId());
            renderHtmlPlaceholder(holder, template);
        }
    }
    
    /**
     * 📱 Create responsive HTML optimized for preview mode
     * This version is optimized for small preview containers in RecyclerView
     */
    private String createResponsiveHtmlForPreview(String htmlContent, String cssContent, Template template) {
        // Replace name placeholders with proper error handling
        htmlContent = replaceNamePlaceholders(htmlContent);
        
        // Replace user profile placeholders for video overlays and dynamic content
        htmlContent = replaceUserProfilePlaceholders(htmlContent);
        
        // Create complete HTML with responsive viewport and preview optimizations
        String responsiveHtml = 
            "<!DOCTYPE html>" +
            "<html lang='en'>" +
            "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
                "<meta http-equiv='X-UA-Compatible' content='ie=edge'>" +
                "<title>" + (template.getTitle() != null ? template.getTitle() : "Preview") + "</title>" +
                "<style>" +
                    // Use server-provided CSS only
                    cssContent +
                "</style>" +
            "</head>" +
            "<body>" +
                "<div class='preview-container'>" +
                    htmlContent +
                "</div>" +
                "<script>" +
                    "document.addEventListener('DOMContentLoaded', function() {" +
                        // Remove any scripts that might interfere with preview
                        "var scripts = document.querySelectorAll('script[src]');" +
                        "scripts.forEach(function(script) { script.remove(); });" +
                        
                        // Optimize images for preview
                        "var images = document.querySelectorAll('img');" +
                        "images.forEach(function(img) {" +
                            "img.style.maxWidth = '100%';" +
                            "img.style.height = 'auto';" +
                            "img.loading = 'lazy';" +
                        "});" +
                        
                        // Add preview mode class to body
                        "document.body.classList.add('preview-mode');" +
                        
                        // Disable all form elements in preview
                        "var forms = document.querySelectorAll('form, input, button, textarea, select');" +
                        "forms.forEach(function(el) { el.disabled = true; });" +
                        
                        "console.log('HTML preview optimized for RecyclerView');" +
                    "});" +
                "</script>" +
            "</body>" +
            "</html>";
        
        Log.d(TAG, "📱 Created responsive HTML for preview mode (length: " + responsiveHtml.length() + ")");
        return responsiveHtml;
    }
    
    /**
     * Get user's display name with caching and error handling
     * @return The user's display name or a default value
     */
    private String getUserDisplayName() {
        long currentTime = System.currentTimeMillis();
        
        // Check if we have a valid cached name
        if (cachedUserName != null && (currentTime - lastNameFetchTime) < NAME_CACHE_DURATION) {
            Log.d(TAG, "Using cached user name: " + cachedUserName);
            return cachedUserName;
        }
        
        String userName = null;
        
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                // Try display name first
                userName = currentUser.getDisplayName();
                
                // If no display name, try email
                if (userName == null || userName.trim().isEmpty()) {
                    String email = currentUser.getEmail();
                    if (email != null && !email.isEmpty()) {
                        userName = email.split("@")[0]; // Use part before @ as name
                        Log.d(TAG, "Using email username as fallback: " + userName);
                    }
                }
                
                // If still no name, use UID prefix
                if (userName == null || userName.trim().isEmpty()) {
                    userName = "User " + currentUser.getUid().substring(0, 4);
                    Log.d(TAG, "Using UID prefix as fallback: " + userName);
                }
                
                // Cache the resolved name
                cachedUserName = userName;
                lastNameFetchTime = currentTime;
                
                Log.d(TAG, "Updated cached user name: " + userName);
            } else {
                Log.w(TAG, "No user currently signed in");
                userName = "Guest User";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting user display name", e);
            // Use cached name as fallback during errors
            if (cachedUserName != null) {
                Log.d(TAG, "Using cached name during error: " + cachedUserName);
                return cachedUserName;
            }
            userName = "Guest User";
        }
        
        return userName;
    }
    
    /**
     * Get user's profile photo URL with caching and error handling
     * @return The user's profile photo URL or null for default
     */
    private String getUserPhotoUrl() {
        long currentTime = System.currentTimeMillis();
        
        // Check if we have a valid cached photo URL
        if (cachedUserPhotoUrl != null && (currentTime - lastPhotoFetchTime) < PHOTO_CACHE_DURATION) {
            Log.d(TAG, "Using cached user photo URL");
            return cachedUserPhotoUrl;
        }
        
        String photoUrl = null;
        
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                // Try to get photo URL
                if (currentUser.getPhotoUrl() != null) {
                    photoUrl = currentUser.getPhotoUrl().toString();
                    
                    // Validate URL format
                    if (!photoUrl.startsWith("http")) {
                        Log.w(TAG, "Invalid photo URL format, using default");
                        photoUrl = null;
                    } else {
                        Log.d(TAG, "Got valid user photo URL");
                    }
                } else {
                    Log.d(TAG, "No photo URL available for user");
                }
                
                // Cache the resolved photo URL (even if null)
                cachedUserPhotoUrl = photoUrl;
                lastPhotoFetchTime = currentTime;
            } else {
                Log.w(TAG, "No user currently signed in for photo URL");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting user photo URL", e);
            // Use cached photo URL as fallback during errors
            if (cachedUserPhotoUrl != null) {
                Log.d(TAG, "Using cached photo URL during error");
                return cachedUserPhotoUrl;
            }
        }
        
        return photoUrl;
    }
    
    /**
     * Set user profile information (name and photo) in ViewHolder with placeholders support
     * @param holder The ViewHolder to update
     * @param showPhoto Whether to show the profile photo (false = hide photo, show only name)
     * @param showName Whether to show the username (false = hide name, show only photo)
     */
    private void setUserProfile(@NonNull ViewHolder holder, boolean showPhoto, boolean showName) {
        try {
            // Handle username display
            if (showName && holder.usernameText != null) {
                String userName = getUserDisplayName();
                holder.usernameText.setText(userName);
                holder.usernameText.setVisibility(View.VISIBLE);
                Log.d(TAG, "👤 Set username: " + userName);
            } else if (holder.usernameText != null) {
                holder.usernameText.setVisibility(View.GONE);
                Log.d(TAG, "👤 Username hidden as requested");
            }
            
            // Handle profile photo display
            if (showPhoto && holder.profileImage != null) {
                String photoUrl = getUserPhotoUrl();
                
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    // Load user's actual profile photo
                    Glide.with(context)
                            .load(photoUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.app_logo)
                            .error(R.drawable.app_logo)
                            .circleCrop()
                            .into(holder.profileImage);
                    Log.d(TAG, "👤 Loading user profile photo from: " + photoUrl);
                } else {
                    // Use default app logo
                    holder.profileImage.setImageResource(R.drawable.app_logo);
                    Log.d(TAG, "👤 Using default app logo for profile photo");
                }
                holder.profileImage.setVisibility(View.VISIBLE);
            } else if (holder.profileImage != null) {
                holder.profileImage.setVisibility(View.GONE);
                Log.d(TAG, "👤 Profile photo hidden as requested");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "👤 Error setting user profile", e);
            
            // Fallback to safe defaults
            if (holder.usernameText != null && showName) {
                holder.usernameText.setText("User");
                holder.usernameText.setVisibility(View.VISIBLE);
            }
            if (holder.profileImage != null && showPhoto) {
                holder.profileImage.setImageResource(R.drawable.app_logo);
                holder.profileImage.setVisibility(View.VISIBLE);
            }
        }
    }
    
    /**
     * Replace user profile placeholders in HTML content for video overlays
     * @param htmlContent The HTML content to process
     * @return HTML content with replaced placeholders
     */
    private String replaceUserProfilePlaceholders(String htmlContent) {
        if (htmlContent == null) {
            Log.w(TAG, "HTML content is null for profile placeholder replacement");
            return "";
        }
        
        try {
            String userName = getUserDisplayName();
            String photoUrl = getUserPhotoUrl();
            
            // Replace [USER_NAME] placeholder
            String userNameSpan = "<span class=\"user-name\">" + userName + "</span>";
            htmlContent = htmlContent.replace("[USER_NAME]", userNameSpan);
            
            // Replace [USER_PHOTO] placeholder
            if (photoUrl != null && !photoUrl.isEmpty()) {
                String userPhotoImg = "<img src=\"" + photoUrl + "\" class=\"user-photo\" alt=\"User Photo\" />";
                htmlContent = htmlContent.replace("[USER_PHOTO]", userPhotoImg);
            } else {
                // Use app logo as placeholder
                String appLogoUrl = "file:///android_res/drawable/app_logo";
                String userPhotoImg = "<img src=\"" + appLogoUrl + "\" class=\"user-photo\" alt=\"User Photo\" />";
                htmlContent = htmlContent.replace("[USER_PHOTO]", userPhotoImg);
            }
            
            // Replace [USER_PROFILE] placeholder (both name and photo)
            String userProfileHtml = "<div class=\"user-profile\">";
            if (photoUrl != null && !photoUrl.isEmpty()) {
                userProfileHtml += "<img src=\"" + photoUrl + "\" class=\"user-photo\" alt=\"User Photo\" />";
            }
            userProfileHtml += "<span class=\"user-name\">" + userName + "</span>";
            userProfileHtml += "</div>";
            
            htmlContent = htmlContent.replace("[USER_PROFILE]", userProfileHtml);
            
            Log.d(TAG, "👤 Successfully replaced user profile placeholders");
            
        } catch (Exception e) {
            Log.e(TAG, "👤 Error replacing user profile placeholders", e);
            // Don't modify content if replacement fails
            return htmlContent;
        }
        
        return htmlContent;
    }
    
    /**
     * Replace name placeholders in HTML content with proper error handling
     */
    private String replaceNamePlaceholders(String htmlContent) {
        if (htmlContent == null) {
            Log.w(TAG, "HTML content is null");
            return "";
        }
        
        try {
            String userName = getUserDisplayName();
            
            // Create span with user name
            String userSpan = "<span class=\"sender-name\">" + userName + "</span>";
            
            // List of placeholder patterns to replace
            String[] placeholders = {
                "[Your Name]",
                "[SENDER_NAME]",
                "{sender}",
                "[sender]",
                "[Sender]",
                "{SENDER}",
                "{Your Name}",
                "[YOUR_NAME]"
            };
            
            // Replace all placeholder patterns
            for (String placeholder : placeholders) {
                htmlContent = htmlContent.replace(placeholder, userSpan);
            }
            
            Log.d(TAG, "Successfully replaced name placeholders with: " + userName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error replacing name placeholders", e);
            // Don't modify content if replacement fails
            return htmlContent;
        }
        
        return htmlContent;
    }
    
    /**
     * Render HTML placeholder when content is not available
     */
    private void renderHtmlPlaceholder(@NonNull ViewHolder holder, @NonNull Template template) {
        String placeholderHtml = "<!DOCTYPE html>" +
            "<html><head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<style>" +
            "body { margin: 0; padding: 20px; font-family: Arial, sans-serif; " +
            "background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); " +
            "color: white; text-align: center; display: flex; " +
            "flex-direction: column; justify-content: center; height: 100vh; }" +
            ".icon { font-size: 48px; margin-bottom: 16px; }" +
            ".title { font-size: 18px; font-weight: bold; margin-bottom: 8px; }" +
            ".subtitle { font-size: 14px; opacity: 0.8; }" +
            "</style>" +
            "</head><body>" +
            "<div class='icon'>🌐</div>" +
            "<div class='title'>HTML Template</div>" +
            "<div class='subtitle'>Interactive content preview</div>" +
            "</body></html>";
            
        try {
            holder.htmlPreviewWebView.loadDataWithBaseURL(
                null, 
                placeholderHtml, 
                "text/html", 
                "UTF-8", 
                null
            );
            Log.d(TAG, "🌐 HTML placeholder rendered for template: " + template.getId());
        } catch (Exception e) {
            Log.e(TAG, "🌐 Error rendering HTML placeholder for template: " + template.getId(), e);
        }
    }
    
    /**
     * 🎥 Render VIDEO template
     */
    private void renderVideoTemplate(@NonNull ViewHolder holder, @NonNull Template template) {
        try {
            Log.d(TAG, "🎥 Starting video template rendering for template: " + template.getId());
            Log.d(TAG, "   • Template type: " + template.getTemplateType());
            Log.d(TAG, "   • Video URL: " + template.getVideoUrl());
            Log.d(TAG, "   • Title: " + template.getTitle());
            
            // Show video container and hide others
            if (holder.videoTemplateContainer != null) {
                holder.videoTemplateContainer.setVisibility(View.VISIBLE);
                Log.d(TAG, "✅ Video container made visible");
            }
            if (holder.templateImage != null) {
                holder.templateImage.setVisibility(View.GONE);
            }
            if (holder.htmlTemplateContainer != null) {
                holder.htmlTemplateContainer.setVisibility(View.GONE);
            }

            // Get user information for overlay
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String userName = currentUser != null ? currentUser.getDisplayName() : null;
            String photoUrl = currentUser != null && currentUser.getPhotoUrl() != null ? 
                            currentUser.getPhotoUrl().toString() : null;
            String userId = currentUser != null ? currentUser.getUid() : null;

            Log.d(TAG, "👤 User info for video overlay:");
            Log.d(TAG, "   • User Name: " + (userName != null ? userName : "null"));
            Log.d(TAG, "   • Photo URL: " + (photoUrl != null ? photoUrl : "null"));
            Log.d(TAG, "   • User ID: " + (userId != null ? userId : "null"));

            // Check for HTML/CSS content for video overlay (use main content fields)
            String htmlContent = template.getHtmlContent();
            String cssContent = template.getCssContent();
            
            Log.d(TAG, "📄 Template content analysis:");
            Log.d(TAG, "   • HTML Content length: " + (htmlContent != null ? htmlContent.length() : 0));
            Log.d(TAG, "   • CSS Content length: " + (cssContent != null ? cssContent.length() : 0));
            Log.d(TAG, "   • HTML Content preview: " + (htmlContent != null && htmlContent.length() > 50 ? 
                  htmlContent.substring(0, 50) + "..." : htmlContent));
            Log.d(TAG, "   • CSS Content preview: " + (cssContent != null && cssContent.length() > 50 ? 
                  cssContent.substring(0, 50) + "..." : cssContent));

            // Also check overlay-specific fields for comparison
            String overlayHtml = template.getOverlayHtmlTemplate();
            String overlayCss = template.getOverlayCssTemplate();
            Log.d(TAG, "🎭 Overlay-specific fields:");
            Log.d(TAG, "   • Overlay HTML length: " + (overlayHtml != null ? overlayHtml.length() : 0));
            Log.d(TAG, "   • Overlay CSS length: " + (overlayCss != null ? overlayCss.length() : 0));

            // Create overlay using HTML/CSS content if available
            if (htmlContent != null && !htmlContent.trim().isEmpty() && 
                cssContent != null && !cssContent.trim().isEmpty()) {
                Log.d(TAG, "🎬 Found HTML/CSS content, rendering video overlay...");
                renderVideoOverlayWithContent(holder, template, htmlContent, cssContent);
            } else if (overlayHtml != null && !overlayHtml.trim().isEmpty() && 
                      overlayCss != null && !overlayCss.trim().isEmpty()) {
                Log.d(TAG, "🎭 Found overlay templates, rendering video overlay...");
                renderVideoOverlayWithContent(holder, template, overlayHtml, overlayCss);
            } else {
                Log.w(TAG, "❌ No valid HTML/CSS content found for video overlay");
                Log.w(TAG, "   • Main HTML Content: " + (htmlContent == null ? "null" : 
                          (htmlContent.trim().isEmpty() ? "empty" : "present (" + htmlContent.length() + " chars)")));
                Log.w(TAG, "   • Main CSS Content: " + (cssContent == null ? "null" : 
                          (cssContent.trim().isEmpty() ? "empty" : "present (" + cssContent.length() + " chars)")));
                Log.w(TAG, "   • Overlay HTML: " + (overlayHtml == null ? "null" : 
                          (overlayHtml.trim().isEmpty() ? "empty" : "present (" + overlayHtml.length() + " chars)")));
                Log.w(TAG, "   • Overlay CSS: " + (overlayCss == null ? "null" : 
                          (overlayCss.trim().isEmpty() ? "empty" : "present (" + overlayCss.length() + " chars)")));
                
                // Hide overlay container if no content
                if (holder.videoOverlayContainer != null) {
                    holder.videoOverlayContainer.setVisibility(View.GONE);
                    Log.d(TAG, "🚫 Video overlay container hidden due to no content");
                }
            }

            // Get video URL for validation
            String videoUrl = template.getVideoUrl();
            if (videoUrl == null || videoUrl.isEmpty()) {
                videoUrl = template.getPreviewUrl();
            }

            // Load video thumbnail with improved error handling
            if (holder.videoThumbnail != null && videoUrl != null) {
                Log.d(TAG, "🎥 Loading video thumbnail from URL: " + videoUrl);
                Glide.with(holder.itemView)
                    .load(videoUrl)
                    .thumbnail(0.1f)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.videoThumbnail);
            }

            // Initialize video controls
            if (holder.videoPlayButton != null) {
                holder.videoPlayButton.setVisibility(View.VISIBLE);
            }
            if (holder.videoMuteButton != null) {
                holder.videoMuteButton.setVisibility(View.GONE);
            }
            if (holder.videoLoadingIndicator != null) {
                holder.videoLoadingIndicator.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error rendering video template", e);
            e.printStackTrace();
        }
    }
    
    /**
     * 🎬 Render video overlay with HTML/CSS content and enhanced debugging
     */
    private void renderVideoOverlayWithContent(@NonNull ViewHolder holder, @NonNull Template template, 
                                             @NonNull String htmlContent, @NonNull String cssContent) {
        Log.d(TAG, "🎬 Starting video overlay rendering with content for template: " + template.getId());
        
        try {
            // Get user information
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.w(TAG, "⚠️ No user logged in for video overlay");
                return;
            }
            
            String userName = getUserDisplayName();
            String userPhoto = getUserPhotoUrl();
            String userId = currentUser.getUid();
            
            Log.d(TAG, "👤 User data for overlay:");
            Log.d(TAG, "   • Name: " + userName);
            Log.d(TAG, "   • Photo: " + (userPhoto != null ? userPhoto : "null"));
            Log.d(TAG, "   • UID: " + userId);
            
            Log.d(TAG, "🎬 Content data for overlay:");
            Log.d(TAG, "   • HTML Content length: " + htmlContent.length());
            Log.d(TAG, "   • CSS Content length: " + cssContent.length());
            Log.d(TAG, "   • HTML Content preview: " + (htmlContent.length() > 100 ? 
                  htmlContent.substring(0, 100) + "..." : htmlContent));
            
            // Create photo HTML with proper null checking and comprehensive fallback
            String photoHtml;
            if (userPhoto != null && !userPhoto.trim().isEmpty()) {
                photoHtml = "<img class='user-photo' src='" + userPhoto + "' alt='" + (userName != null ? userName : "User") + "'>";
                Log.d(TAG, "👤 Using actual user photo: " + userPhoto);
            } else {
                photoHtml = "<img class='user-photo' src='file:///android_res/drawable/app_logo' alt='User Photo'>";
                Log.d(TAG, "👤 No user photo available, using app logo");
            }
                
            // Replace placeholders with comprehensive format support
            Log.d(TAG, "🔄 Replacing placeholders in HTML content...");
            
            String processedHtml = htmlContent
                // Support multiple placeholder formats: [USER_*], {USER_*}, {{USER_*}}
                .replace("[USER_PHOTO]", photoHtml)
                .replace("[USER_NAME]", userName != null ? userName : "User")
                .replace("[USER_ID]", userId != null ? userId : "")
                .replace("{USER_PHOTO}", photoHtml)
                .replace("{USER_NAME}", userName != null ? userName : "User")
                .replace("{USER_ID}", userId != null ? userId : "")
                .replace("{{USER_PHOTO}}", photoHtml)
                .replace("{{USER_NAME}}", userName != null ? userName : "User")
                .replace("{{USER_ID}}", userId != null ? userId : "")
                // Additional common formats
                .replace("{{userPhoto}}", photoHtml)
                .replace("{{userName}}", userName != null ? userName : "User")
                .replace("{{userId}}", userId != null ? userId : "")
                .replace("[userPhoto]", photoHtml)
                .replace("[userName]", userName != null ? userName : "User")
                .replace("[userId]", userId != null ? userId : "");
                
            Log.d(TAG, "✅ Placeholders replaced. Processed HTML length: " + processedHtml.length());
            Log.d(TAG, "🔍 Processed HTML preview: " + (processedHtml.length() > 150 ? 
                  processedHtml.substring(0, 150) + "..." : processedHtml));
            
            // Use server-provided CSS directly
            String enhancedCss = cssContent;
                
            // Create clean HTML document without debug elements
            String completeHtml = String.format(
                "<!DOCTYPE html><html><head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>%s</style>" +
                "</head><body>%s</body></html>",
                enhancedCss,
                processedHtml
            );
            
            Log.d(TAG, "📄 Complete HTML document created. Length: " + completeHtml.length());
            
            // Ensure overlay container is visible and properly configured
            if (holder.videoOverlayContainer != null) {
                holder.videoOverlayContainer.setVisibility(View.VISIBLE);
                holder.videoOverlayContainer.removeAllViews();
                
                Log.d(TAG, "📱 Video overlay container prepared:");
                Log.d(TAG, "   • Visibility: " + (holder.videoOverlayContainer.getVisibility() == View.VISIBLE ? "VISIBLE" : "HIDDEN"));
                Log.d(TAG, "   • Width: " + holder.videoOverlayContainer.getLayoutParams().width);
                Log.d(TAG, "   • Height: " + holder.videoOverlayContainer.getLayoutParams().height);
                
                // Create and configure WebView with enhanced settings
                WebView overlayWebView = new WebView(holder.itemView.getContext());
                
                // Enhanced WebView settings for overlay rendering
                WebSettings webSettings = overlayWebView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setLoadWithOverviewMode(true);
                webSettings.setUseWideViewPort(true);
                webSettings.setBuiltInZoomControls(false);
                webSettings.setDisplayZoomControls(false);
                webSettings.setSupportZoom(false);
                webSettings.setTextZoom(100);
                webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                
                // Enable debugging for WebView
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    WebView.setWebContentsDebuggingEnabled(true);
                }
                
                // Configure WebView layout parameters
                FrameLayout.LayoutParams webViewParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                );
                webViewParams.gravity = Gravity.BOTTOM;
                overlayWebView.setLayoutParams(webViewParams);
                
                // Set WebView background and styling
                overlayWebView.setBackgroundColor(Color.TRANSPARENT);
                overlayWebView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
                
                // Enhanced WebViewClient with comprehensive debugging
                overlayWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        super.onPageStarted(view, url, favicon);
                        Log.d(TAG, "🌐 Overlay WebView page started loading: " + url);
                    }
                    
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        Log.d(TAG, "✅ Overlay WebView page finished loading: " + url);
                        
                        // Inject additional debugging script
                        String debugScript = 
                            "javascript:(function() {" +
                            "  console.log('🎯 Page loaded, body content:', document.body.innerHTML);" +
                            "  console.log('📏 Body size:', document.body.offsetWidth + 'x' + document.body.offsetHeight);" +
                            "  var userProfile = document.querySelector('.user-profile');" +
                            "  if (userProfile) {" +
                            "    console.log('👤 Found user profile element');" +
                            "  } else {" +
                            "    console.warn('❌ User profile element not found');" +
                            "  }" +
                            "})()";
                        
                        view.evaluateJavascript(debugScript, result -> 
                            Log.d(TAG, "📝 Debug script executed: " + result));
                        
                        // Force container visibility check after page load
                        view.post(() -> {
                            Log.d(TAG, "📱 Post-load container check:");
                            Log.d(TAG, "   • Container visibility: " + 
                                  (holder.videoOverlayContainer.getVisibility() == View.VISIBLE ? "VISIBLE" : "HIDDEN"));
                            Log.d(TAG, "   • WebView visibility: " + 
                                  (view.getVisibility() == View.VISIBLE ? "VISIBLE" : "HIDDEN"));
                            Log.d(TAG, "   • Container child count: " + holder.videoOverlayContainer.getChildCount());
                        });
                    }
                    
                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        super.onReceivedError(view, errorCode, description, failingUrl);
                        Log.e(TAG, "❌ Video overlay WebView error: " + description + " (Code: " + errorCode + ")");
                        
                        // Show error overlay using CSS class instead of inline styles
                        String errorHtml = "<div class='error-message'>" +
                                          "❌ Video Overlay Error: " + description + "</div>";
                        view.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
                    }
                    
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        Log.d(TAG, "🔗 Overlay WebView URL loading: " + url);
                        return false; // Allow normal loading
                    }
                });
                
                // Add WebView to overlay container
                holder.videoOverlayContainer.addView(overlayWebView);
                
                Log.d(TAG, "📱 WebView added to overlay container");
                
                // Load the HTML content
                Log.d(TAG, "🚀 Loading HTML content into overlay WebView...");
                overlayWebView.loadDataWithBaseURL(
                    "https://eventwish.com/", // Base URL for relative resources
                    completeHtml,
                    "text/html",
                    "UTF-8",
                    null
                );
                
                Log.d(TAG, "✅ Video overlay rendering completed successfully!");
                
            } else {
                Log.e(TAG, "❌ Video overlay container is null - cannot render overlay");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in renderVideoOverlayWithContent", e);
            e.printStackTrace();
            
            // Show error message in overlay container
            if (holder.videoOverlayContainer != null) {
                holder.videoOverlayContainer.removeAllViews();
                TextView errorView = new TextView(holder.itemView.getContext());
                errorView.setText("❌ Overlay Error: " + e.getMessage());
                errorView.setTextColor(Color.RED);
                errorView.setBackgroundColor(Color.WHITE);
                errorView.setPadding(16, 8, 16, 8);
                holder.videoOverlayContainer.addView(errorView);
            }
        }
    }
    
    /**
     * 🎯 Set up interaction listeners for template actions
     */
    private void setupInteractionListeners(@NonNull ViewHolder holder, @NonNull Template template) {
        // Validate template and its ID first
        if (template == null) {
            Log.e(TAG, "🚨 Cannot setup listeners for null template");
            return;
        }

        String templateId = template.getId();
        if (templateId == null || templateId.trim().isEmpty()) {
            Log.e(TAG, "🚨 Cannot setup listeners for template with null/empty ID. Title: " + template.getTitle());
            return;
        }

        Log.d(TAG, "🎯 Setting up interaction listeners for template: " + templateId + " (Type: " + template.getTemplateType() + ")");
        
        try {
            // Template click listener (works for all types)
            View.OnClickListener templateClickListener = v -> {
                try {
                    if (onItemClickListener != null) {
                        // Double-check template validity before click
                        if (template.getId() == null) {
                            Log.e(TAG, "🚨 Template ID became null before click");
                            showErrorToast("Error opening template");
                            return;
                        }
                        Log.d(TAG, "🎯 Template clicked: " + template.getId() + " (Type: " + template.getTemplateType() + ")");
                        Log.d(TAG, "🎯 Navigating to TemplateDetailFragment for template: " + template.getTitle());
                        onItemClickListener.onItemClick(template);
                    } else {
                        Log.w(TAG, "🎯 onItemClickListener is null - cannot navigate");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "🎯 ERROR handling template click for: " + template.getId(), e);
                    showErrorToast("Error opening template: " + template.getTitle());
                }
            };
            
            // Set click listener on appropriate view based on template type
            String templateType = template.getTemplateType();
            Log.d(TAG, "🎯 Template type for click handling: " + (templateType != null ? templateType : "null"));
            
            if (templateType != null) {
                switch (templateType.toLowerCase()) {
                    case "html":
                        Log.d(TAG, "🎯 Setting up HTML template click listener");
                        // Enhanced HTML WebView navigation
                        setupHtmlWebViewNavigation(holder, template, templateClickListener);
                        break;
                    case "video":
                        Log.d(TAG, "🎯 Setting up VIDEO template click listener");
                        // Video templates should navigate to TemplateDetailFragment for full video playback
                        holder.videoTemplateContainer.setOnClickListener(templateClickListener);
                        holder.videoThumbnail.setOnClickListener(templateClickListener);
                        // Add visual feedback for video clicks
                        holder.videoTemplateContainer.setOnTouchListener((v, event) -> {
                            switch (event.getAction()) {
                                case android.view.MotionEvent.ACTION_DOWN:
                                    holder.videoTemplateContainer.setAlpha(0.8f);
                                    Log.d(TAG, "🎯 Video template touch down");
                                    break;
                                case android.view.MotionEvent.ACTION_UP:
                                case android.view.MotionEvent.ACTION_CANCEL:
                                    holder.videoTemplateContainer.setAlpha(1.0f);
                                    Log.d(TAG, "🎯 Video template touch up/cancel");
                                    break;
                            }
                            return false; // Let click listener handle the actual click
                        });
                        break;
                    case "image":
                        Log.d(TAG, "🎯 Setting up IMAGE template click listener");
                        holder.templateImage.setOnClickListener(templateClickListener);
                        break;
                    default:
                        Log.d(TAG, "🎯 Setting up DEFAULT template click listener for type: " + templateType);
                        holder.templateImage.setOnClickListener(templateClickListener);
                        break;
                }
            } else {
                Log.w(TAG, "🎯 Template type is null for template: " + template.getId() + ", using fallback image click");
                // Fallback to image click
                holder.templateImage.setOnClickListener(templateClickListener);
            }
            
            // Like button listener with error handling
            holder.likeIcon.setOnClickListener(v -> {
                try {
                    // Validate template ID before like action
                    if (template.getId() == null) {
                        Log.e(TAG, "🚨 Cannot like template with null ID");
                        showErrorToast("Error liking template");
                        return;
                    }
                    Log.d(TAG, "🎯 Like button clicked for template: " + template.getId());
                    handleLikeClick(holder, template);
                } catch (Exception e) {
                    Log.e(TAG, "🎯 ERROR handling like click for: " + template.getId(), e);
                    showErrorToast("Error liking template");
                }
            });
            
            // Favorite button listener with error handling
            holder.favoriteIcon.setOnClickListener(v -> {
                try {
                    // Validate template ID before favorite action
                    if (template.getId() == null) {
                        Log.e(TAG, "🚨 Cannot favorite template with null ID");
                        showErrorToast("Error favoriting template");
                        return;
                    }
                    Log.d(TAG, "🎯 Favorite button clicked for template: " + template.getId());
                    handleFavoriteClick(holder, template);
                } catch (Exception e) {
                    Log.e(TAG, "🎯 ERROR handling favorite click for: " + template.getId(), e);
                    showErrorToast("Error favoriting template");
                }
            });
            
            Log.d(TAG, "🎯 Interaction listeners setup complete for template: " + template.getId());
            
        } catch (Exception e) {
            Log.e(TAG, "🎯 CRITICAL ERROR setting up interaction listeners for: " + template.getId(), e);
            showErrorToast("Error setting up template interactions");
        }
    }
    
    /**
     * 🚨 Show error toast without crashing the app
     */
    private void showErrorToast(String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "🚨 Showing error toast: " + message);
        } else {
            Log.e(TAG, "🚨 Cannot show error toast (null context): " + message);
        }
    }
    
    /**
     * 🌐 Setup enhanced HTML WebView navigation with visual feedback
     */
    private void setupHtmlWebViewNavigation(@NonNull ViewHolder holder, @NonNull Template template, View.OnClickListener templateClickListener) {
        // Set click listener on HTML container for better touch target
        holder.htmlTemplateContainer.setOnClickListener(templateClickListener);
        
        // Enhanced WebView click listener with visual feedback
        if (holder.htmlPreviewWebView != null) {
            holder.htmlPreviewWebView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        // Visual feedback on touch down
                        holder.htmlTemplateContainer.setAlpha(0.8f);
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        // Reset visual state
                        holder.htmlTemplateContainer.setAlpha(1.0f);
                        break;
                }
                return true; // Consume the touch event
            });
        }
        
        // Add ripple effect to HTML container for better UX
        android.util.TypedValue outValue = new android.util.TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        holder.htmlTemplateContainer.setBackground(
            androidx.core.content.ContextCompat.getDrawable(context, outValue.resourceId)
        );
        
        Log.d(TAG, "🌐 Enhanced HTML WebView navigation setup complete for template: " + template.getId());
    }
    
    /**
     * ❤️ Handle like button click with debouncing
     */
    private void handleLikeClick(@NonNull ViewHolder holder, @NonNull Template template) {
        Log.d(TAG, "❤️ LIKE BUTTON CLICKED for template: " + template.getId());
            Log.d(TAG, "Current liked state: " + template.isLiked());
            Log.d(TAG, "Current like count: " + template.getLikeCount());
            
            // Prevent rapid clicks
        if (!holder.likeIcon.isEnabled()) {
            Log.w(TAG, "❤️ Like button disabled - preventing rapid clicks");
                return;
            }
            
            // Disable button temporarily to prevent rapid clicks
        holder.likeIcon.setEnabled(false);
        holder.likeIcon.postDelayed(() -> holder.likeIcon.setEnabled(true), 1000);
            
            // Get current adapter position
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) {
            Log.e(TAG, "❤️ Invalid adapter position - aborting like action");
                return;
            }
            
        Log.d(TAG, "❤️ Performing optimistic UI update at position: " + currentPosition);
            
        // Optimistic UI update
            boolean newLikedState = !template.isLiked();
            long newLikeCount = template.getLikeCount() + (newLikedState ? 1 : -1);
            
        Log.d(TAG, "❤️ Optimistic update: liked " + template.isLiked() + " -> " + newLikedState);
        Log.d(TAG, "❤️ Optimistic update: count " + template.getLikeCount() + " -> " + newLikeCount);
            
            // Update template state immediately for UI responsiveness
            template.setLiked(newLikedState);
            template.setLikeCount(Math.max(0, newLikeCount));
            
            // Animate the like button
            animateLikeButton(holder.likeIcon, newLikedState);
            
            // Update UI state
            updateLikeState(holder, newLikedState);
            
            // Update like count display
            if (template.getLikeCount() > 0) {
                holder.likeCountText.setText(String.valueOf(template.getLikeCount()));
                holder.likeCountText.setVisibility(View.VISIBLE);
            Log.d(TAG, "❤️ Like count updated to: " + template.getLikeCount());
            } else {
                holder.likeCountText.setVisibility(View.GONE);
            Log.d(TAG, "❤️ Like count hidden (count is 0)");
            }
            
            // Show toast message
        String message = newLikedState ? "Liked!" : "Unliked!";
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        
        // Notify listener for network operation
            if (onTemplateInteractionListener != null) {
            Log.d(TAG, "❤️ Notifying interaction listener: liked=" + newLikedState);
            onTemplateInteractionListener.onTemplateLiked(template, newLikedState);
        }
    }
    
    /**
     * ⭐ Handle favorite button click with debouncing
     */
    private void handleFavoriteClick(@NonNull ViewHolder holder, @NonNull Template template) {
        Log.d(TAG, "⭐ FAVORITE BUTTON CLICKED for template: " + template.getId());
        Log.d(TAG, "Current favorited state: " + template.isFavorited());
        Log.d(TAG, "Current favorite count: " + template.getFavoriteCount());
        
        // Prevent rapid clicks
        if (!holder.favoriteIcon.isEnabled()) {
            Log.w(TAG, "⭐ Favorite button disabled - preventing rapid clicks");
            return;
        }
        
        // Disable button temporarily to prevent rapid clicks
        holder.favoriteIcon.setEnabled(false);
        holder.favoriteIcon.postDelayed(() -> holder.favoriteIcon.setEnabled(true), 1000);
        
        // Get current adapter position
        int currentPosition = holder.getAdapterPosition();
        if (currentPosition == RecyclerView.NO_POSITION) {
            Log.e(TAG, "⭐ Invalid adapter position - aborting favorite action");
            return;
        }
        
        Log.d(TAG, "⭐ Performing optimistic UI update at position: " + currentPosition);
        
        // Optimistic UI update
        boolean newFavoritedState = !template.isFavorited();
        long newFavoriteCount = template.getFavoriteCount() + (newFavoritedState ? 1 : -1);
        
        Log.d(TAG, "⭐ Optimistic update: favorited " + template.isFavorited() + " -> " + newFavoritedState);
        Log.d(TAG, "⭐ Optimistic update: count " + template.getFavoriteCount() + " -> " + newFavoriteCount);
        
        // Update template state immediately for UI responsiveness
        template.setFavorited(newFavoritedState);
        template.setFavoriteCount(Math.max(0, newFavoriteCount));
        
        // Animate the favorite button
        animateFavoriteButton(holder.favoriteIcon, newFavoritedState);
        
        // Update UI state
        updateFavoriteState(holder, newFavoritedState);
        
        // Update favorite count display - FIXED: Added proper count update logic
        if (template.getFavoriteCount() > 0) {
            holder.favoriteCountText.setText(String.valueOf(template.getFavoriteCount()));
            holder.favoriteCountText.setVisibility(View.VISIBLE);
            Log.d(TAG, "⭐ Favorite count updated to: " + template.getFavoriteCount());
        } else {
            holder.favoriteCountText.setVisibility(View.GONE);
            Log.d(TAG, "⭐ Favorite count hidden (count is 0)");
        }
        
        // Show toast message
        String message = newFavoritedState ? "Added to favorites!" : "Removed from favorites!";
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        
        // Notify listener for network operation
        if (onTemplateInteractionListener != null) {
            Log.d(TAG, "⭐ Notifying interaction listener: favorited=" + newFavoritedState);
            onTemplateInteractionListener.onTemplateFavorited(template, newFavoritedState);
        }
    }
    
    private void animateLikeButton(ImageView likeIcon, boolean liked) {
        // Animate scale
        likeIcon.animate()
                .scaleX(liked ? 1.2f : 1.0f)
                .scaleY(liked ? 1.2f : 1.0f)
                .setDuration(150)
                .withEndAction(() -> {
                    likeIcon.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }

    private void animateFavoriteButton(ImageView favoriteIcon, boolean favorited) {
        // Animate scale
        favoriteIcon.animate()
                .scaleX(favorited ? 1.2f : 1.0f)
                .scaleY(favorited ? 1.2f : 1.0f)
                .setDuration(150)
                .withEndAction(() -> {
                    favoriteIcon.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }

    private void updateLikeState(ViewHolder holder, boolean liked) {
        holder.likeIcon.setImageResource(liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        holder.likeIcon.setColorFilter(ContextCompat.getColor(context, liked ? R.color.like_icon_filled : R.color.like_icon_outline));
    }

    private void updateFavoriteState(ViewHolder holder, boolean favorited) {
        holder.favoriteIcon.setImageResource(favorited ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
        holder.favoriteIcon.setColorFilter(ContextCompat.getColor(context, favorited ? R.color.favorite_icon_filled : R.color.favorite_icon_outline));
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    /**
     * Set the list of templates and update the adapter
     */
    public void setTemplates(List<Template> templates) {
        Log.d(TAG, "=== SET TEMPLATES CALLED ===");
        Log.d(TAG, "New templates count: " + (templates != null ? templates.size() : 0));
        Log.d(TAG, "Current templates count: " + (this.templates != null ? this.templates.size() : 0));
        
        if (templates == null) {
            Log.w(TAG, "Received null templates list");
            return;
        }
        
        // Debounce rapid setTemplates calls to prevent excessive updates
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSetTemplatesTime < SET_TEMPLATES_DEBOUNCE_TIME) {
            Log.d(TAG, "Debouncing setTemplates call - too soon since last update (" + 
                  (currentTime - lastSetTemplatesTime) + "ms ago)");
            return;
        }
        lastSetTemplatesTime = currentTime;
        
        // Log first few template IDs for debugging
        if (!templates.isEmpty()) {
            Log.d(TAG, "First template ID: " + templates.get(0).getId());
            if (templates.size() > 1) {
                Log.d(TAG, "Second template ID: " + templates.get(1).getId());
            }
        }
        
        // Check if this is the same data
        boolean isSameData = false;
        if (this.templates != null && this.templates.size() == templates.size()) {
            isSameData = true;
            for (int i = 0; i < templates.size(); i++) {
                Template oldTemplate = this.templates.get(i);
                Template newTemplate = templates.get(i);
                
                // Add null checks to prevent NullPointerException
                if (oldTemplate == null || newTemplate == null || 
                    oldTemplate.getId() == null || newTemplate.getId() == null ||
                    !oldTemplate.getId().equals(newTemplate.getId())) {
                    isSameData = false;
                    break;
                }
            }
        }
        
        Log.d(TAG, "Is same data: " + isSameData);
        
        if (isSameData) {
            Log.d(TAG, "Same template data detected - checking for state changes");
            // Check if any template states have changed
            boolean hasStateChanges = false;
            for (int i = 0; i < templates.size(); i++) {
                Template oldTemplate = this.templates.get(i);
                Template newTemplate = templates.get(i);
                
                // Add null checks to prevent NullPointerException
                if (oldTemplate == null || newTemplate == null) {
                    Log.w(TAG, "Null template detected at position " + i + " - skipping state comparison");
                    continue;
                }
                
                if (oldTemplate.isLiked() != newTemplate.isLiked() ||
                    oldTemplate.isFavorited() != newTemplate.isFavorited() ||
                    oldTemplate.getLikeCount() != newTemplate.getLikeCount() ||
                    oldTemplate.getFavoriteCount() != newTemplate.getFavoriteCount()) {
                    
                    String templateId = newTemplate.getId() != null ? newTemplate.getId() : "null";
                    Log.d(TAG, "State change detected in template " + templateId + 
                          " at position " + i);
                    Log.d(TAG, "  Liked: " + oldTemplate.isLiked() + " -> " + newTemplate.isLiked());
                    Log.d(TAG, "  Like count: " + oldTemplate.getLikeCount() + " -> " + newTemplate.getLikeCount());
                    Log.d(TAG, "  Favorited: " + oldTemplate.isFavorited() + " -> " + newTemplate.isFavorited());
                    Log.d(TAG, "  Favorite count: " + oldTemplate.getFavoriteCount() + " -> " + newTemplate.getFavoriteCount());
                    
                    hasStateChanges = true;
                    
                    // Call notifyItemChanged for this specific position - THIS MIGHT CAUSE JUMPING!
                    Log.w(TAG, "Calling notifyItemChanged(" + i + ") - POTENTIAL JUMPING CAUSE!");
                    notifyItemChanged(i);
                }
            }
            
            if (hasStateChanges) {
                Log.w(TAG, "Template states updated with individual notifyItemChanged calls");
            } else {
                Log.d(TAG, "No state changes detected - skipping unnecessary updates");
            }
            
            // Update the data reference
            this.templates = new ArrayList<>(templates);
            return;
        }
        
        Log.d(TAG, "Different template data - performing full update");
        this.templates = new ArrayList<>(templates);
        
        Log.w(TAG, "Calling notifyDataSetChanged() - POTENTIAL JUMPING CAUSE!");
        notifyDataSetChanged();
        
        Log.d(TAG, "=== SET TEMPLATES COMPLETED ===");
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    public void setOnTemplateInteractionListener(OnTemplateInteractionListener listener) {
        this.onTemplateInteractionListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(Template template);
    }
    
    public interface OnTemplateInteractionListener {
        void onTemplateLiked(Template template, boolean liked);
        void onTemplateFavorited(Template template, boolean favorited);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final CardView cardView;
        final ImageView templateImage;
        final TextView titleText;
        final TextView categoryText;
        final ImageView categoryIcon;
        final ImageView likeIcon;
        final TextView likeCountText;
        final ImageView favoriteIcon;
        final TextView favoriteCountText;
        final TextView newBadge;
        final LinearLayout recommendedBadge;
        final TextView timeText;
        final TextView fallbackTimeText;
        final TextView htmlTemplateTitle;
        final LinearLayout htmlTemplateContainer;
        final WebView htmlPreviewWebView;
        final ImageView videoThumbnail;
        final TextView videoDuration;
        final LinearLayout videoTemplateContainer;
        final TextView templateTypeBadge;
        final PlayerView videoPlayerView;
        final ImageView videoPlayButton;
        final ImageView videoMuteButton;
        final ProgressBar videoLoadingIndicator;
        
        // User profile views for dynamic placeholders
        final ShapeableImageView profileImage;
        final TextView usernameText;
        final FrameLayout videoOverlayContainer;
        final FrameLayout imageOverlayContainer;
        final FrameLayout imageTemplateContainer;

        public ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            templateImage = itemView.findViewById(R.id.template_image);
            titleText = itemView.findViewById(R.id.titleText);
            categoryText = itemView.findViewById(R.id.categoryText);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            likeCountText = itemView.findViewById(R.id.likeCountText);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
            favoriteCountText = itemView.findViewById(R.id.favoriteCountText);
            newBadge = itemView.findViewById(R.id.newBadge);
            recommendedBadge = itemView.findViewById(R.id.recommendedBadge);
            timeText = itemView.findViewById(R.id.timeText);
            fallbackTimeText = itemView.findViewById(R.id.fallbackTimeText);
            htmlTemplateTitle = itemView.findViewById(R.id.html_template_title);
            htmlTemplateContainer = itemView.findViewById(R.id.html_template_container);
            htmlPreviewWebView = itemView.findViewById(R.id.html_preview_webview);
            videoTemplateContainer = itemView.findViewById(R.id.video_template_container);
            templateTypeBadge = itemView.findViewById(R.id.template_type_badge);
            
            // Initialize video-related views
            videoPlayerView = itemView.findViewById(R.id.video_player_view);
            videoPlayButton = itemView.findViewById(R.id.video_play_button);
            videoMuteButton = itemView.findViewById(R.id.video_mute_button);
            videoLoadingIndicator = itemView.findViewById(R.id.video_loading_indicator);
            videoThumbnail = itemView.findViewById(R.id.video_thumbnail);
            videoDuration = itemView.findViewById(R.id.video_duration);
            
            // Initialize user profile views for dynamic placeholders
            profileImage = itemView.findViewById(R.id.profileImage);
            usernameText = itemView.findViewById(R.id.usernameText);
            
            // Initialize video overlay container
            videoOverlayContainer = itemView.findViewById(R.id.video_overlay_container);
            
            // Initialize image overlay containers
            imageOverlayContainer = itemView.findViewById(R.id.image_overlay_container);
            imageTemplateContainer = itemView.findViewById(R.id.image_template_container);
            
            // Log view initialization
            Log.d(TAG, "🎥 ViewHolder initialized - PlayerView: " + (videoPlayerView != null ? "found" : "null") +
                      ", PlayButton: " + (videoPlayButton != null ? "found" : "null") +
                      ", MuteButton: " + (videoMuteButton != null ? "found" : "null"));
            Log.d(TAG, "👤 Profile views initialized - ProfileImage: " + (profileImage != null ? "found" : "null") +
                      ", UsernameText: " + (usernameText != null ? "found" : "null"));
            Log.d(TAG, "🖼️ Image overlay views initialized - ImageOverlayContainer: " + (imageOverlayContainer != null ? "found" : "null") +
                      ", ImageTemplateContainer: " + (imageTemplateContainer != null ? "found" : "null"));
        }
    }

    /**
     * Formats a date into a social media style time ago string
     * @param date The date to format
     * @return A string like "2h ago", "3d ago", etc.
     */
    private String formatTimeAgo(Date date) {
        if (date == null) {
            Log.d(TAG, "formatTimeAgo: date is null, returning 'recently added'");
            return "recently added";
        }
        
        long now = System.currentTimeMillis();
        long time = date.getTime();
        long diff = now - time;
        
        Log.d(TAG, "formatTimeAgo: date=" + date + ", now=" + new Date(now) + ", diff=" + diff + "ms (" + (diff/1000) + " seconds)");
        
        // Check if date is in the future (server time might be ahead)
        if (diff < 0) {
            Log.d(TAG, "formatTimeAgo: date is in the future, using 'recently added'");
            return "recently added";
        }
        
        // Convert to seconds
        long seconds = diff / 1000;
        if (seconds < 60) {
            Log.d(TAG, "formatTimeAgo: returning 'just now' for " + seconds + " seconds");
            return "just now";
        }
        
        // Convert to minutes
        long minutes = seconds / 60;
        if (minutes < 60) {
            String result = minutes + "m ago";
            Log.d(TAG, "formatTimeAgo: returning '" + result + "' for " + minutes + " minutes");
            return result;
        }
        
        // Convert to hours
        long hours = minutes / 60;
        if (hours < 24) {
            String result = hours + "h ago";
            Log.d(TAG, "formatTimeAgo: returning '" + result + "' for " + hours + " hours");
            return result;
        }
        
        // Convert to days
        long days = hours / 24;
        if (days < 7) {
            String result = days + "d ago";
            Log.d(TAG, "formatTimeAgo: returning '" + result + "' for " + days + " days");
            return result;
        }
        
        // Convert to weeks
        long weeks = days / 7;
        if (weeks < 4) {
            String result = weeks + "w ago";
            Log.d(TAG, "formatTimeAgo: returning '" + result + "' for " + weeks + " weeks");
            return result;
        }
        
        // Convert to months
        long months = days / 30;
        if (months < 12) {
            String result = months + "mo ago";
            Log.d(TAG, "formatTimeAgo: returning '" + result + "' for " + months + " months");
            return result;
        }
        
        // Convert to years
        long years = days / 365;
        String result = years + "y ago";
        Log.d(TAG, "formatTimeAgo: returning '" + result + "' for " + years + " years");
        return result;
    }

    private void verifyTimeTextVisibility(TextView timeText) {
        // Check if the view is visible and has valid dimensions
        boolean isVisible = timeText.getVisibility() == View.VISIBLE;
        int width = timeText.getWidth();
        int height = timeText.getHeight();
        String text = timeText.getText().toString();
        
        Log.d(TAG, "TimeText verification: " +
              "isVisible=" + isVisible + 
              ", width=" + width + 
              ", height=" + height + 
              ", text='" + text + "'" +
              ", parent=" + (timeText.getParent() != null ? timeText.getParent().getClass().getSimpleName() : "null"));
        
        // Check parent view
        if (timeText.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) timeText.getParent();
            Log.d(TAG, "Parent view: " +
                  "visibility=" + (parent.getVisibility() == View.VISIBLE ? "VISIBLE" : "NOT_VISIBLE") +
                  ", width=" + parent.getWidth() +
                  ", height=" + parent.getHeight() +
                  ", childCount=" + parent.getChildCount());
        }
        
        if (!isVisible) {
            Log.e(TAG, "TimeText is not visible!");
        } else if (width <= 0 || height <= 0) {
            Log.e(TAG, "TimeText has invalid dimensions: " + width + "x" + height);
        } else if (text.isEmpty()) {
            Log.e(TAG, "TimeText has empty text!");
        } else {
            Log.d(TAG, "TimeText appears to be displayed correctly");
        }
    }

    /**
     * Determines if a template is new based on its creation date
     * @param template The template to check
     * @return true if the template is new (less than 7 days old)
     */
    private boolean isNewTemplate(Template template) {
        if (template.getCreatedAt() == null) return false;
        
        long now = System.currentTimeMillis();
        long createdTime = template.getCreatedAt().getTime();
        long daysDiff = (now - createdTime) / (1000 * 60 * 60 * 24);
        
        return daysDiff < 7; // Template is new if less than 7 days old
    }

    /**
     * 🎥 Attach adapter to RecyclerView to enable video auto-play functionality
     */
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        setupVideoAutoPlayScrollListener();
        Log.d(TAG, "🎥 Adapter attached to RecyclerView - video auto-play enabled");
    }
    
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        pauseCurrentPlayingVideo();
        this.recyclerView = null;
        Log.d(TAG, "🎥 Adapter detached from RecyclerView - video auto-play disabled");
    }
    
    /**
     * 🎥 Setup scroll listener for video auto-play detection
     */
    private void setupVideoAutoPlayScrollListener() {
        if (recyclerView == null) return;
        
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                
                // Check video visibility when scroll stops
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    checkVideoVisibilityAndAutoPlay();
                }
            }
            
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                // Continuously check during scroll for smooth experience
                checkVideoVisibilityAndAutoPlay();
            }
        });
    }
    
    /**
     * 🎥 Check video visibility and manage auto-play
     */
    private void checkVideoVisibilityAndAutoPlay() {
        try {
            if (!isAutoPlayEnabled || recyclerView == null) {
                Log.d(TAG, "🎥 Auto-play disabled or RecyclerView null - skipping visibility check");
                return;
            }

            if (templates == null || templates.isEmpty()) {
                Log.d(TAG, "🎥 No templates available - skipping visibility check");
                return;
            }
            
            Log.d(TAG, "🎥 Checking video visibility for auto-play...");
            
            ViewHolder mostVisibleVideoHolder = null;
            float maxVisibilityPercentage = 0f;
            String mostVisibleVideoId = null;
            Template mostVisibleTemplate = null;
            
            int videoCount = 0;
            
            // Find the most visible video template
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View child = recyclerView.getChildAt(i);
                if (child == null) continue;

                ViewHolder holder = (ViewHolder) recyclerView.getChildViewHolder(child);
                if (holder == null) continue;

                int position = holder.getAdapterPosition();
                if (position < 0 || position >= templates.size()) continue;
                    
                Template template = templates.get(position);
                if (template == null) {
                    Log.w(TAG, "🎥 Null template at position " + position);
                    continue;
                }

                String templateId = template.getId();
                if (templateId == null || templateId.trim().isEmpty()) {
                    Log.w(TAG, "🎥 Template at position " + position + " has null/empty ID");
                    continue;
                }

                String templateType = template.getTemplateType();
                if (templateType == null) {
                    Log.w(TAG, "🎥 Template " + templateId + " has null type");
                    continue;
                }

                if ("video".equalsIgnoreCase(templateType)) {
                    // Validate video URL
                    String videoUrl = template.getVideoUrl();
                    if (videoUrl == null || videoUrl.trim().isEmpty()) {
                        videoUrl = template.getPreviewUrl();
                    }
                    
                    if (videoUrl == null || videoUrl.trim().isEmpty()) {
                        Log.w(TAG, "🎥 Video template " + templateId + " has no valid video URL");
                        continue;
                    }

                    videoCount++;
                    float visibilityPercentage = getVisibilityPercentage(child);
                    
                    Log.d(TAG, "🎥 Video template found: " + templateId + 
                          " (Position: " + position + 
                          ", Visibility: " + (visibilityPercentage * 100) + "%)");
                    
                    if (visibilityPercentage > maxVisibilityPercentage && 
                        visibilityPercentage >= VISIBILITY_THRESHOLD) {
                        maxVisibilityPercentage = visibilityPercentage;
                        mostVisibleVideoHolder = holder;
                        mostVisibleVideoId = templateId;
                        mostVisibleTemplate = template;
                        
                        Log.d(TAG, "🎥 New most visible video: " + templateId + 
                              " (" + (visibilityPercentage * 100) + "%)");
                    }
                }
            }
            
            if (videoCount == 0) {
                Log.d(TAG, "🎥 No valid video templates found");
                return;
            }
            
            Log.d(TAG, "🎥 Visibility check complete: " + videoCount + " video(s) found, " +
                  "most visible: " + (mostVisibleVideoId != null ? mostVisibleVideoId : "none") +
                  " (" + (maxVisibilityPercentage * 100) + "%)");
            
            // Manage video playback based on visibility
            if (mostVisibleVideoHolder != null && mostVisibleVideoId != null && 
                !mostVisibleVideoId.equals(currentPlayingVideoId)) {
                Log.d(TAG, "🎥 Switching video playback from " + currentPlayingVideoId + " to " + mostVisibleVideoId);
                
                // Pause current playing video
                pauseCurrentPlayingVideo();
                
                // Start playing the most visible video
                playVideo(mostVisibleVideoHolder, mostVisibleVideoId, mostVisibleTemplate);
                
            } else if (mostVisibleVideoHolder == null && currentPlayingVideoHolder != null) {
                Log.d(TAG, "🎥 No video sufficiently visible - pausing current video: " + currentPlayingVideoId);
                // No video is sufficiently visible, pause current playing video
                pauseCurrentPlayingVideo();
                
            } else if (mostVisibleVideoId != null && mostVisibleVideoId.equals(currentPlayingVideoId)) {
                Log.d(TAG, "🎥 Same video still most visible: " + currentPlayingVideoId + " - continuing playback");
                
            } else {
                Log.d(TAG, "🎥 No change in video visibility state");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "🎥 ERROR in checkVideoVisibilityAndAutoPlay", e);
            // Don't show error toast for visibility check errors as they are not critical
            Log.d(TAG, "🎥 Suppressing visibility check error toast to avoid user distraction");
        }
    }
    
    /**
     * 🎥 Calculate visibility percentage of a view
     */
    private float getVisibilityPercentage(View view) {
        if (recyclerView == null) return 0f;
        
        android.graphics.Rect scrollBounds = new android.graphics.Rect();
        recyclerView.getHitRect(scrollBounds);
        
        android.graphics.Rect viewBounds = new android.graphics.Rect();
        view.getHitRect(viewBounds);
        
        if (viewBounds.intersect(scrollBounds)) {
            int visibleArea = viewBounds.width() * viewBounds.height();
            int totalArea = view.getWidth() * view.getHeight();
            
            return totalArea > 0 ? (float) visibleArea / totalArea : 0f;
        }
        
        return 0f;
    }
    
    /**
     * 🎥 Start playing video with smooth animation
     */
    private void playVideo(ViewHolder holder, String videoId, Template template) {
        try {
            if (holder == null || holder.videoTemplateContainer == null) {
                Log.w(TAG, "🎥 Cannot play video - holder or container is null");
                return;
            }
            
            Log.d(TAG, "🎥 Starting video playback for template: " + videoId);
            
            // Get video URL
            String videoUrl = template != null ? template.getVideoUrl() : null;
            if (videoUrl == null || videoUrl.isEmpty()) {
                videoUrl = template != null ? template.getPreviewUrl() : null;
            }
            
            if (videoUrl == null || videoUrl.isEmpty()) {
                Log.w(TAG, "🎥 No video URL available for template: " + videoId + " - showing visual feedback only");
                // Continue with visual feedback even if no video URL
            } else {
                Log.d(TAG, "🎥 Video URL found: " + videoUrl);
            }
            
            currentPlayingVideoHolder = holder;
            currentPlayingVideoId = videoId;
            
            // Add play animation
            if (holder.videoThumbnail != null) {
                Log.d(TAG, "🎥 Starting play animation for thumbnail");
                holder.videoThumbnail.animate()
                    .alpha(0.7f)
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(300)
                    .start();
            }
            
            // Show visual indicator that video is playing
            if (holder.videoDuration != null) {
                holder.videoDuration.setText("▶ PLAYING");
                holder.videoDuration.setTextColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.holo_red_dark));
                Log.d(TAG, "🎥 Updated duration text to show PLAYING state");
            }
            
            // Try to integrate with VideoPlayerManager if available
            try {
                com.ds.eventwish.utils.VideoPlayerManager videoPlayerManager = 
                    com.ds.eventwish.utils.VideoPlayerManager.getInstance(context);
                
                if (videoPlayerManager != null && videoUrl != null && !videoUrl.isEmpty()) {
                    Log.d(TAG, "🎥 Attempting to play video with VideoPlayerManager: " + videoUrl);
                    
                    // Use the properly initialized PlayerView from ViewHolder
                    if (holder.videoPlayerView != null) {
                        Log.d(TAG, "🎥 PlayerView found in ViewHolder - starting actual video playback");
                        
                        // Show loading indicator
                        if (holder.videoLoadingIndicator != null) {
                            holder.videoLoadingIndicator.setVisibility(View.VISIBLE);
                        }
                        
                        // Hide thumbnail and show player view
                        if (holder.videoThumbnail != null) {
                            holder.videoThumbnail.setVisibility(View.GONE);
                        }
                        
                        // Hide play button
                        if (holder.videoPlayButton != null) {
                            holder.videoPlayButton.setVisibility(View.GONE);
                        }
                        
                        // Show player view
                        holder.videoPlayerView.setVisibility(View.VISIBLE);
                        
                        // Start video playback
                        videoPlayerManager.playVideo(holder.videoPlayerView, videoUrl, videoId, true);
                        
                        // Hide loading indicator after a short delay
                        holder.videoPlayerView.postDelayed(() -> {
                            if (holder.videoLoadingIndicator != null) {
                                holder.videoLoadingIndicator.setVisibility(View.GONE);
                            }
                        }, 1000);
                        
                    } else {
                        Log.w(TAG, "🎥 No PlayerView found in ViewHolder - showing visual feedback only");
                    }
                } else {
                    Log.w(TAG, "🎥 VideoPlayerManager unavailable or no video URL - visual feedback only");
                }
            } catch (Exception videoPlayerError) {
                Log.e(TAG, "🎥 Error with VideoPlayerManager - falling back to visual feedback", videoPlayerError);
                
                // Hide loading indicator on error
                if (holder.videoLoadingIndicator != null) {
                    holder.videoLoadingIndicator.setVisibility(View.GONE);
                }
            }
            
            Log.d(TAG, "🎥 Video playback setup completed for template: " + videoId);
            
        } catch (Exception e) {
            Log.e(TAG, "🎥 ERROR starting video playback for: " + videoId, e);
            showErrorToast("Error playing video: " + (template != null ? template.getTitle() : videoId));
            
            // Reset state on error
            currentPlayingVideoHolder = null;
            currentPlayingVideoId = null;
        }
    }
    
    /**
     * 🎥 Pause current playing video with smooth animation
     */
    private void pauseCurrentPlayingVideo() {
        try {
            if (currentPlayingVideoHolder == null) {
                Log.d(TAG, "🎥 No video currently playing - nothing to pause");
                return;
            }
            
            Log.d(TAG, "🎥 Pausing video playback for template: " + currentPlayingVideoId);
            
            ViewHolder holder = currentPlayingVideoHolder;
            
            // Try to pause with VideoPlayerManager first
            try {
                com.ds.eventwish.utils.VideoPlayerManager videoPlayerManager = 
                    com.ds.eventwish.utils.VideoPlayerManager.getInstance(context);
                
                if (videoPlayerManager != null) {
                    Log.d(TAG, "🎥 Pausing video with VideoPlayerManager");
                    videoPlayerManager.pauseCurrentVideo();
                } else {
                    Log.w(TAG, "🎥 VideoPlayerManager unavailable - visual pause only");
                }
            } catch (Exception videoPlayerError) {
                Log.e(TAG, "🎥 Error pausing with VideoPlayerManager", videoPlayerError);
            }
            
            // Reset video UI elements
            if (holder.videoPlayerView != null) {
                holder.videoPlayerView.setVisibility(View.GONE);
                Log.d(TAG, "🎥 PlayerView hidden");
            }
            
            if (holder.videoThumbnail != null) {
                holder.videoThumbnail.setVisibility(View.VISIBLE);
                Log.d(TAG, "🎥 Video thumbnail restored");
            }
            
            if (holder.videoPlayButton != null) {
                holder.videoPlayButton.setVisibility(View.VISIBLE);
                Log.d(TAG, "🎥 Play button restored");
            }
            
            if (holder.videoLoadingIndicator != null) {
                holder.videoLoadingIndicator.setVisibility(View.GONE);
                Log.d(TAG, "🎥 Loading indicator hidden");
            }
            
            // Restore normal appearance
            if (holder.videoThumbnail != null) {
                Log.d(TAG, "🎥 Restoring thumbnail animation");
                holder.videoThumbnail.animate()
                    .alpha(1.0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(300)
                    .start();
            }
            
            // Reset duration text with properly resolved theme color
            if (holder.videoDuration != null) {
                try {
                    holder.videoDuration.setText("0:30");
                    
                    // Properly resolve the textColorSecondary theme attribute
                    android.util.TypedValue typedValue = new android.util.TypedValue();
                    context.getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
                    
                    if (typedValue.resourceId != 0) {
                        holder.videoDuration.setTextColor(androidx.core.content.ContextCompat.getColor(context, typedValue.resourceId));
                    } else {
                        // Fallback to a default color
                        holder.videoDuration.setTextColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.darker_gray));
                    }
                    
                    Log.d(TAG, "🎥 Duration text reset to default state");
                } catch (Exception colorError) {
                    Log.e(TAG, "🎥 Error setting duration text color", colorError);
                    // Fallback to white text
                    holder.videoDuration.setTextColor(android.graphics.Color.WHITE);
                }
            }
            
            // Clear current playing state
            String pausedVideoId = currentPlayingVideoId;
            currentPlayingVideoHolder = null;
            currentPlayingVideoId = null;
            
            Log.d(TAG, "🎥 Video playback paused successfully for template: " + pausedVideoId);
            
        } catch (Exception e) {
            Log.e(TAG, "🎥 ERROR pausing video playback", e);
            
            // Force clear state even on error
            currentPlayingVideoHolder = null;
            currentPlayingVideoId = null;
            
            showErrorToast("Error pausing video");
        }
    }
    
    /**
     * 🎥 Enable or disable video auto-play
     */
    public void setAutoPlayEnabled(boolean enabled) {
        this.isAutoPlayEnabled = enabled;
        if (!enabled) {
            pauseCurrentPlayingVideo();
        }
        Log.d(TAG, "🎥 Video auto-play " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * 🎥 Check if auto-play is enabled
     */
    public boolean isAutoPlayEnabled() {
        return isAutoPlayEnabled;
    }
    
    /**
     * Initialize video player for the RecyclerView
     * This method is called from HomeFragment to set up video functionality
     */
    public void initializeVideoPlayer(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        setupVideoAutoPlayScrollListener();
        Log.d(TAG, "Video player initialized for RecyclerView");
    }
    
    /**
     * Handle video visibility changes (called from scroll listeners)
     * This method checks which videos are visible and manages auto-play
     */
    public void handleVideoVisibilityChange() {
        if (recyclerView != null) {
            checkVideoVisibilityAndAutoPlay();
        }
    }

    private void updateInteractionStates(@NonNull ViewHolder holder, @NonNull Template template) {
        try {
            // Set title and category
            holder.titleText.setText(template.getTitle());
            holder.categoryText.setText(template.getCategoryId());

            // Set creation time
            String timeAgoText = template.getCreatedAt() != null ? 
                formatTimeAgo(template.getCreatedAt()) : "recently added";
            
            // Create a SpannableString for the time
            SpannableString timeSpannable = new SpannableString("⏱️ " + timeAgoText.toUpperCase());
            timeSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, timeSpannable.length(), 0);
            timeSpannable.setSpan(new RelativeSizeSpan(1.2f), 0, timeSpannable.length(), 0);
            
            // Set time texts
            holder.timeText.setText(timeSpannable);
            holder.timeText.setVisibility(View.VISIBLE);
            holder.timeText.setTextColor(Color.WHITE);
            holder.fallbackTimeText.setText("⏱️ POSTED " + timeAgoText.toUpperCase());
            holder.fallbackTimeText.setVisibility(View.VISIBLE);
            
            // Update like state and count
            updateLikeState(holder, template.isLiked());
            if (template.getLikeCount() > 0) {
                holder.likeCountText.setVisibility(View.VISIBLE);
                holder.likeCountText.setText(String.valueOf(template.getLikeCount()));
            } else {
                holder.likeCountText.setVisibility(View.GONE);
            }
            
            // Update favorite state and count
            updateFavoriteState(holder, template.isFavorited());
            if (template.getFavoriteCount() > 0) {
                holder.favoriteCountText.setVisibility(View.VISIBLE);
                holder.favoriteCountText.setText(String.valueOf(template.getFavoriteCount()));
            } else {
                holder.favoriteCountText.setVisibility(View.GONE);
            }
            
            // Update badges
            holder.newBadge.setVisibility(isNewTemplate(template) ? View.VISIBLE : View.GONE);
            holder.recommendedBadge.setVisibility(View.GONE); // Recommendation system removed
            
            Log.d(TAG, "✅ Successfully updated interaction states for template: " + template.getId());
            
        } catch (Exception e) {
            Log.e(TAG, "🚨 Error updating interaction states for template: " + template.getId(), e);
            // Don't hide the card here, just log the error
        }
    }
} 