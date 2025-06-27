package com.ds.eventwish.services;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.data.model.response.FeedResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket Service for Real-time Feed Updates
 * Handles Socket.IO connection, authentication, and real-time events
 */
public class WebSocketService {
    private static final String TAG = "WebSocketService";
    private static final String SERVER_URL = "https://eventwish2.onrender.com";
    
    private static WebSocketService instance;
    private Socket socket;
    private FirebaseAuth firebaseAuth;
    private Gson gson;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private AtomicBoolean isAuthenticated = new AtomicBoolean(false);
    
    // LiveData for observing real-time updates
    private MutableLiveData<FeedResponse> feedUpdateLiveData = new MutableLiveData<>();
    private MutableLiveData<TemplateInteraction> templateInteractionLiveData = new MutableLiveData<>();
    private MutableLiveData<SystemNotification> notificationLiveData = new MutableLiveData<>();
    private MutableLiveData<ConnectionStatus> connectionStatusLiveData = new MutableLiveData<>();
    private MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    
    // Connection status enum
    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        AUTHENTICATED,
        ERROR
    }
    
    // Template interaction model
    public static class TemplateInteraction {
        public String templateId;
        public String action; // liked, favorited, shared
        public String category;
        public long timestamp;
        
        public TemplateInteraction(String templateId, String action, String category, long timestamp) {
            this.templateId = templateId;
            this.action = action;
            this.category = category;
            this.timestamp = timestamp;
        }
    }
    
    // System notification model
    public static class SystemNotification {
        public String title;
        public String message;
        public String type; // info, warning, error, announcement
        public long timestamp;
        
        public SystemNotification(String title, String message, String type, long timestamp) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
        }
    }
    
    private WebSocketService() {
        firebaseAuth = FirebaseAuth.getInstance();
        gson = new Gson();
        initializeSocket();
    }
    
    public static synchronized WebSocketService getInstance() {
        if (instance == null) {
            instance = new WebSocketService();
        }
        return instance;
    }
    
    /**
     * Initialize Socket.IO connection
     */
    private void initializeSocket() {
        try {
            IO.Options options = IO.Options.builder()
                    .setTransports(new String[] {"websocket", "polling"})
                    .setTimeout(30000)
                    .setReconnection(true)
                    .setReconnectionAttempts(5)
                    .setReconnectionDelay(1000)
                    .build();
                    
            socket = IO.socket(SERVER_URL, options);
            setupEventListeners();
            
            Log.d(TAG, "Socket.IO initialized with server: " + SERVER_URL);
            
        } catch (URISyntaxException e) {
            Log.e(TAG, "Failed to initialize Socket.IO", e);
            connectionStatusLiveData.postValue(ConnectionStatus.ERROR);
            errorLiveData.postValue("Failed to initialize WebSocket connection");
        }
    }
    
    /**
     * Setup Socket.IO event listeners
     */
    private void setupEventListeners() {
        // Connection events
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Socket connected");
                isConnected.set(true);
                connectionStatusLiveData.postValue(ConnectionStatus.CONNECTED);
                
                // Authenticate immediately after connection
                authenticateUser();
            }
        });
        
        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Socket disconnected: " + (args.length > 0 ? args[0] : "unknown reason"));
                isConnected.set(false);
                isAuthenticated.set(false);
                connectionStatusLiveData.postValue(ConnectionStatus.DISCONNECTED);
            }
        });
        
        // Authentication events
        socket.on("authenticated", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Socket authenticated successfully");
                isAuthenticated.set(true);
                connectionStatusLiveData.postValue(ConnectionStatus.AUTHENTICATED);
                
                // Subscribe to feed updates after authentication
                subscribeFeedUpdates();
            }
        });
        
        // Feed update events
        socket.on("feed_update", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (args.length > 0) {
                    try {
                        String jsonData = args[0].toString();
                        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
                        
                        // Parse feed response
                        FeedResponse feedResponse = gson.fromJson(jsonObject, FeedResponse.class);
                        feedUpdateLiveData.postValue(feedResponse);
                        
                        Log.d(TAG, "Feed update received: " + (feedResponse.getSections() != null ? 
                            feedResponse.getSections().size() + " sections" : "no sections"));
                            
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing feed update", e);
                    }
                }
            }
        });
        
        // Template interaction events
        socket.on("template_interaction", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (args.length > 0) {
                    try {
                        String jsonData = args[0].toString();
                        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
                        
                        String templateId = jsonObject.get("templateId").getAsString();
                        String action = jsonObject.get("action").getAsString();
                        String category = jsonObject.has("category") ? jsonObject.get("category").getAsString() : "";
                        long timestamp = jsonObject.get("timestamp").getAsLong();
                        
                        TemplateInteraction interaction = new TemplateInteraction(templateId, action, category, timestamp);
                        templateInteractionLiveData.postValue(interaction);
                        
                        Log.d(TAG, "Template interaction received: " + action + " on " + templateId);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing template interaction", e);
                    }
                }
            }
        });
    }
    
    /**
     * Connect to WebSocket server
     */
    public void connect() {
        if (socket != null && !isConnected.get()) {
            Log.d(TAG, "Connecting to WebSocket server...");
            connectionStatusLiveData.postValue(ConnectionStatus.CONNECTING);
            socket.connect();
        }
    }
    
    /**
     * Disconnect from WebSocket server
     */
    public void disconnect() {
        if (socket != null && isConnected.get()) {
            Log.d(TAG, "Disconnecting from WebSocket server...");
            socket.disconnect();
            isConnected.set(false);
            isAuthenticated.set(false);
            connectionStatusLiveData.postValue(ConnectionStatus.DISCONNECTED);
        }
    }
    
    /**
     * Authenticate user with Firebase token
     */
    private void authenticateUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            user.getIdToken(false).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String token = task.getResult().getToken();
                    
                    JsonObject authData = new JsonObject();
                    authData.addProperty("firebaseToken", token);
                    authData.addProperty("userId", user.getUid());
                    
                    socket.emit("authenticate", authData);
                    Log.d(TAG, "Authentication request sent");
                    
                } else {
                    Log.e(TAG, "Failed to get Firebase token");
                    errorLiveData.postValue("Failed to authenticate");
                }
            });
        }
    }
    
    /**
     * Subscribe to feed updates
     */
    private void subscribeFeedUpdates() {
        if (isAuthenticated.get()) {
            JsonObject subscriptionData = new JsonObject();
            subscriptionData.addProperty("feedType", "general");
            subscriptionData.addProperty("includePersonalized", true);
            
            socket.emit("subscribe_feed", subscriptionData);
            Log.d(TAG, "Subscribed to feed updates");
        }
    }
    
    /**
     * Send template interaction
     */
    public void sendTemplateInteraction(String templateId, String action, String category) {
        if (isAuthenticated.get()) {
            JsonObject interactionData = new JsonObject();
            interactionData.addProperty("templateId", templateId);
            interactionData.addProperty("action", action);
            interactionData.addProperty("category", category);
            interactionData.addProperty("timestamp", System.currentTimeMillis());
            
            socket.emit("template_interaction", interactionData);
            Log.d(TAG, "Template interaction sent: " + action + " on " + templateId);
        }
    }
    
    // LiveData getters for observing real-time updates
    public LiveData<FeedResponse> getFeedUpdateLiveData() {
        return feedUpdateLiveData;
    }
    
    public LiveData<TemplateInteraction> getTemplateInteractionLiveData() {
        return templateInteractionLiveData;
    }
    
    public LiveData<SystemNotification> getNotificationLiveData() {
        return notificationLiveData;
    }
    
    public LiveData<ConnectionStatus> getConnectionStatusLiveData() {
        return connectionStatusLiveData;
    }
    
    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }
    
    // Status getters
    public boolean isConnected() {
        return isConnected.get();
    }
    
    public boolean isAuthenticated() {
        return isAuthenticated.get();
    }
}
