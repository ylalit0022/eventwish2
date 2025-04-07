package com.ds.eventwish.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

public class SecureTokenManager {
    private static final String TAG = "SecureTokenManager";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "EventWishSecureKey";
    private static final String SHARED_PREFS_NAME = "EventWishSecurePrefs";
    private static final String API_KEY_PREF = "api_key";
    private static final String DEVICE_ID_PREF = "device_id";
    private static final String AUTH_PREFS_NAME = "auth_prefs";
    private static final String ACCESS_TOKEN_PREF = "access_token";
    private static final String REFRESH_TOKEN_PREF = "refresh_token";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128; // in bits
    
    private static SecureTokenManager instance;
    private final SharedPreferences securePrefs;
    private final SharedPreferences authPrefs;
    private final Context context;
    private KeyStore keyStore;
    
    private SecureTokenManager(Context context) {
        this.context = context.getApplicationContext();
        securePrefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        authPrefs = context.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE);
        try {
            createKeyIfNotExists();
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Error initializing security key", e);
        }
    }
    
    public static synchronized SecureTokenManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SecureTokenManager must be initialized with init(Context) before first use");
        }
        return instance;
    }
    
    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new SecureTokenManager(context.getApplicationContext());
        }
    }
    
    private void createKeyIfNotExists() throws GeneralSecurityException {
        keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        try {
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build();
                
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
                keyGenerator.init(spec);
                keyGenerator.generateKey();
                Log.d(TAG, "Created new security key");
            }
        } catch (IOException e) {
            throw new GeneralSecurityException("Error loading keystore", e);
        }
    }
    
    /**
     * Get or create the encryption key
     * @return SecretKey object
     * @throws GeneralSecurityException if key creation fails
     */
    private SecretKey getOrCreateKey() throws GeneralSecurityException {
        try {
            // Check if key exists
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            
            if (keyStore.containsAlias(KEY_ALIAS)) {
                return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
            } else {
                // Create key if it doesn't exist
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
                
                KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(true) // System will handle IV
                        .build();
                
                keyGenerator.init(keySpec);
                return keyGenerator.generateKey();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error accessing keystore", e);
            throw new GeneralSecurityException("Error accessing keystore", e);
        }
    }
    
    /**
     * Encrypt data
     * @param data Data to encrypt
     * @return Encrypted data
     */
    private String encrypt(String data) {
        try {
            if (keyStore == null || data == null || data.isEmpty()) {
                Log.e(TAG, "KeyStore or data is null/empty");
                return null;
            }

            SecretKey key = getOrCreateKey();
            if (key == null) {
                Log.e(TAG, "Failed to get encryption key");
                return null;
            }

            // Let the system generate the IV instead of providing our own
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            
            // Get the IV that was automatically generated
            byte[] iv = cipher.getIV();
            
            // Perform encryption
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and encrypted data
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);
            
            return Base64.encodeToString(combined, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting value", e);
            return null;
        }
    }
    
    /**
     * Decrypt data
     * @param encryptedData Encrypted data
     * @return Decrypted data
     */
    private String decrypt(String encryptedData) {
        try {
            if (keyStore == null || encryptedData == null || encryptedData.isEmpty()) {
                Log.e(TAG, "KeyStore or encrypted data is null/empty");
                return null;
            }
            
            SecretKey key = getOrCreateKey();
            if (key == null) {
                Log.e(TAG, "Failed to get encryption key");
                return null;
            }
            
            // Decode the base64 string
            byte[] combined = Base64.decode(encryptedData, Base64.DEFAULT);
            if (combined.length < GCM_IV_LENGTH) {
                Log.e(TAG, "Invalid encrypted data format");
                return null;
            }
            
            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedBytes = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);
            
            // Initialize cipher with extracted IV using GCMParameterSpec
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec specs = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, specs);
            
            // Decrypt the data
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting value", e);
            return null;
        }
    }
    
    public String getApiKey() {
        return decrypt(securePrefs.getString(API_KEY_PREF, null));
    }
    
    public void saveApiKey(String apiKey) {
        String encryptedValue = encrypt(apiKey);
        if (encryptedValue != null) {
            securePrefs.edit().putString(API_KEY_PREF, encryptedValue).apply();
        }
    }
    
    public void clearApiKey() {
        securePrefs.edit().remove(API_KEY_PREF).apply();
    }
    
    public String getDeviceId() {
        return decrypt(securePrefs.getString(DEVICE_ID_PREF, null));
    }
    
    public void saveDeviceId(String deviceId) {
        String encryptedValue = encrypt(deviceId);
        if (encryptedValue != null) {
            securePrefs.edit().putString(DEVICE_ID_PREF, encryptedValue).apply();
        }
    }
    
    public void clearDeviceId() {
        securePrefs.edit().remove(DEVICE_ID_PREF).apply();
    }
    
    /**
     * Get the access token from storage
     * @return The access token or null if not found
     */
    public String getAccessToken() {
        String encryptedToken = authPrefs.getString(ACCESS_TOKEN_PREF, null);
        if (encryptedToken == null) {
            return null;
        }
        return decrypt(encryptedToken);
    }
    
    /**
     * Save the access token
     * @param token The token to save
     */
    public void saveAccessToken(String token) {
        String encryptedToken = encrypt(token);
        if (encryptedToken != null) {
            authPrefs.edit().putString(ACCESS_TOKEN_PREF, encryptedToken).apply();
        }
    }
    
    /**
     * Get the refresh token from storage
     * @return The refresh token or null if not found
     */
    public String getRefreshToken() {
        String encryptedToken = authPrefs.getString(REFRESH_TOKEN_PREF, null);
        if (encryptedToken == null) {
            return null;
        }
        return decrypt(encryptedToken);
    }
    
    /**
     * Save the refresh token
     * @param token The token to save
     */
    public void saveRefreshToken(String token) {
        String encryptedToken = encrypt(token);
        if (encryptedToken != null) {
            authPrefs.edit().putString(REFRESH_TOKEN_PREF, encryptedToken).apply();
        }
    }
    
    /**
     * Check if both access and refresh tokens are available
     * @return true if both tokens are available
     */
    public boolean hasTokens() {
        String accessToken = getAccessToken();
        String refreshToken = getRefreshToken();
        return accessToken != null && !accessToken.isEmpty() && 
               refreshToken != null && !refreshToken.isEmpty();
    }
    
    /**
     * Clear all authentication tokens
     */
    public void clearTokens() {
        authPrefs.edit()
            .remove(ACCESS_TOKEN_PREF)
            .remove(REFRESH_TOKEN_PREF)
            .apply();
    }
}