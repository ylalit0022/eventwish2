package com.ds.eventwish.ui.render;

import android.annotation.SuppressLint;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import com.ds.eventwish.data.model.Template;

public class TemplateRenderer {
    private static final String TAG = "TemplateRenderer";
    private final WebView webView;
    private TemplateRenderListener listener;
    private String recipientName = "";
    private String senderName = "";
    private String currentHtml = "";
    private String currentCss = "";
    private String currentJs = "";

    public interface TemplateRenderListener {
        void onRenderComplete();
        void onRenderError(String error);
        void onLoadingStateChanged(boolean isLoading);
    }

    public TemplateRenderer(WebView webView, TemplateRenderListener listener) {
        this.webView = webView;
        this.listener = listener;
        setupWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                updateNames();  // Ensure names update when WebView finishes loading
            }
        });
    }

    public void renderTemplate(Template template) {
        if (template == null) {
            if (listener != null) {
                listener.onRenderError("Template is null");
            }
            return;
        }
        
        // Get content from template
        String html = template.getHtmlContent();
        String css = template.getCssContent();
        String js = template.getJsContent();
        
        // Check if HTML content is available
        if (html == null || html.isEmpty()) {
            if (listener != null) {
                listener.onRenderError("Template HTML content is empty");
            }
            return;
        }

        // Store content for future reference
        this.currentHtml = html;
        this.currentCss = css != null ? css : getDefaultCSS();
        this.currentJs = js != null ? js : getDefaultJS();

        // Use default CSS if not provided
        if (css == null || css.isEmpty()) {
            css = getDefaultCSS();
        }

        // Use default JS if not provided
        if (js == null || js.isEmpty()) {
            js = getDefaultJS();
        }
        
        // Create enhanced HTML with responsive meta tags and better DOM structure
        String fullHtml = String.format(
                "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">" +
                "  <style>" +
                "    * { box-sizing: border-box; }" +
                "    body { margin: 0; padding: 8px; font-family: Arial, sans-serif; line-height: 1.4; }" +
                "    img { max-width: 100%%; height: auto; display: block; }" +
                "    table { width: 100%%; max-width: 100%%; table-layout: fixed; }" +
                "    div { max-width: 100%%; }" +
                "    .recipient-name, .sender-name { font-weight: bold; }" +
                "    %s" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div id=\"template-content\">%s</div>" +
                "  <script>" +
                "    // Safe DOMContentLoaded handler" +
                "    function domReady(fn) {" +
                "      if (document.readyState === 'complete' || document.readyState === 'interactive') {" +
                "        setTimeout(fn, 1);" +
                "      } else {" +
                "        document.addEventListener('DOMContentLoaded', fn);" +
                "      }" +
                "    }" +
                "    " +
                "    domReady(function() {" +
                "      // Notify that template is loaded" + 
                "      if (window.Android) {" +
                "        try {" +
                "          window.Android.onRenderComplete();" +
                "        } catch(e) {" +
                "          console.error('Error notifying render complete:', e);" +
                "        }" +
                "      }" +
                "    });" +
                "    " +
                "    // Setup error handling" +
                "    window.onerror = function(message, source, lineno, colno, error) {" +
                "      console.error('Template error:', message);" +
                "      if (window.Android) {" +
                "        try {" +
                "          window.Android.onRenderError(message);" +
                "        } catch(e) {" +
                "          console.error('Error notifying error:', e);" +
                "        }" +
                "      }" +
                "      return true;" +
                "    };" +
                "    " +
                "    // Fix viewport tag if needed" +
                "    function fixViewport() {" +
                "      try {" +
                "        var found = false;" +
                "        var head = document.head;" +
                "        if (!head) return;" +
                "        var metas = head.getElementsByTagName('meta');" +
                "        for (var i = 0; i < metas.length; i++) {" +
                "          if (metas[i].name === 'viewport') {" +
                "            found = true;" +
                "            metas[i].content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0';" +
                "          }" +
                "        }" +
                "        if (!found) {" +
                "          var meta = document.createElement('meta');" +
                "          meta.name = 'viewport';" +
                "          meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0';" +
                "          head.appendChild(meta);" +
                "        }" +
                "      } catch (e) {" +
                "        console.error('Error fixing viewport:', e);" +
                "      }" +
                "    }" +
                "    " +
                "    domReady(fixViewport);" +
                "    " +
                "    // Wrap user JS in try/catch to prevent errors" +
                "    try {" +
                "      %s" +
                "    } catch (e) {" +
                "      console.error('Error in template JS:', e);" +
                "    }" +
                "  </script>" +
                "</body>" +
                "</html>",
                css, html, js
        );

        // Signal loading state
        if (listener != null) {
            listener.onLoadingStateChanged(true);
        }
        
        // Load the content with proper encoding
        webView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null);
        
        // Update names after a short delay to ensure DOM is ready
        webView.postDelayed(this::updateNames, 300);
    }

    public void setRecipientName(String name) {
        this.recipientName = name;
        updateNames();
    }

    public void setSenderName(String name) {
        this.senderName = name;
        updateNames();
    }

    private void updateNames() {
        String js = String.format(
            "javascript:(function() {" +
            "  try {" +
            "    console.log('Updating names: %s, %s');" +
            "    var recipientEls = document.getElementsByClassName('recipient-name');" +
            "    var senderEls = document.getElementsByClassName('sender-name');" +
            "    console.log('Found elements:', recipientEls.length, senderEls.length);" +
            "    " +
            "    for (var i = 0; i < recipientEls.length; i++) {" +
            "      recipientEls[i].textContent = '%s';" +
            "    }" +
            "    " +
            "    for (var i = 0; i < senderEls.length; i++) {" +
            "      senderEls[i].textContent = '%s';" +
            "    }" +
            "  } catch (e) {" +
            "    console.error('Error updating names:', e);" +
            "  }" +
            "})()",
            recipientName, senderName,
            recipientName.isEmpty() ? "[Recipient]" : recipientName,
            senderName.isEmpty() ? "[Your Name]" : senderName
        );
        
        webView.evaluateJavascript(js, result -> {
            if (listener != null) {
                listener.onRenderComplete();
            }
        });
    }
    
    /**
     * Get the current HTML content with names replaced
     * @return The customized HTML content
     */
    public String getCustomizedHtml() {
        // Create a copy of the current HTML with names replaced
        String customHtml = currentHtml;
        if (customHtml != null) {
            customHtml = customHtml.replace("[Recipient]", 
                "<span class=\"recipient-name\">" + recipientName + "</span>");
            customHtml = customHtml.replace("{recipient}", 
                "<span class=\"recipient-name\">" + recipientName + "</span>");
            customHtml = customHtml.replace("[Your Name]", 
                "<span class=\"sender-name\">" + senderName + "</span>");
            customHtml = customHtml.replace("{sender}", 
                "<span class=\"sender-name\">" + senderName + "</span>");
            
            Log.d(TAG, "Generated customized HTML with recipient: " + recipientName + 
                  ", sender: " + senderName);
        }
        return customHtml;
    }
    
    /**
     * Get the current CSS content
     * @return The CSS content
     */
    public String getCssContent() {
        return currentCss;
    }
    
    /**
     * Get the current JS content
     * @return The JS content
     */
    public String getJsContent() {
        return currentJs;
    }

    private String getDefaultCSS() {
        return "body {" +
                "  margin: 0;" +
                "  padding: 16px;" +
                "  font-family: Arial, sans-serif;" +
                "  text-align: center;" +
                "  background-color: #f5f5f5;" +
                "}" +
                "h1, h2 {" +
                "  color: #333;" +
                "  margin-bottom: 16px;" +
                "}" +
                ".recipient-name, .sender-name {" +
                "  color: #2196F3;" +
                "  font-weight: bold;" +
                "}";
    }

    private String getDefaultJS() {
        return "function updateNames(recipient, sender) {" +
                "  document.querySelectorAll('.recipient-name').forEach(function(el) {" +
                "    el.textContent = recipient || '[Recipient]';" +
                "  });" +
                "  document.querySelectorAll('.sender-name').forEach(function(el) {" +
                "    el.textContent = sender || '[Sender]';" +
                "  });" +
                "}";
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void onRenderComplete() {
            if (listener != null) {
                listener.onRenderComplete();
            }
        }

        @JavascriptInterface
        public void onRenderError(String error) {
            if (listener != null) {
                listener.onRenderError(error);
            }
        }
    }
}
