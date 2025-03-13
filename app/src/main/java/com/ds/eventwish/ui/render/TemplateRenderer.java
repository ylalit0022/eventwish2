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
        String html = template.getHtmlContent();
        String css = template.getCssContent();
        String js = template.getJsContent();

        this.currentHtml = html;
        this.currentCss = css != null ? css : getDefaultCSS();
        this.currentJs = js != null ? js : getDefaultJS();

        if (css == null || css.isEmpty()) {
            css = getDefaultCSS();
        }

        if (js == null || js.isEmpty()) {
            js = getDefaultJS();
        }

        String fullHtml = String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "<style>%s</style>" +
                        "</head>" +
                        "<body>" +
                        "%s" +
                        "<script>%s</script>" +
                        "</body>" +
                        "</html>",
                css, html, js
        );

        webView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null);
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
            "  console.log('Updating names: %s, %s');" +
            "  var recipientEls = document.getElementsByClassName('recipient-name');" +
            "  var senderEls = document.getElementsByClassName('sender-name');" +
            "  console.log('Found elements:', recipientEls.length, senderEls.length);" +
            "  Array.from(recipientEls).forEach(function(el) {" +
            "    el.textContent = '%s';" +
            "  });" +
            "  Array.from(senderEls).forEach(function(el) {" +
            "    el.textContent = '%s';" +
            "  });" +
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
