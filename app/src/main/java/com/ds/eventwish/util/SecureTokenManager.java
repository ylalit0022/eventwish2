package com.ds.eventwish.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

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
    
    private static SecureTokenManager instance;
    private final SharedPreferences securePrefs;
    private final SharedPreferences authPrefs;
    private final Context context;
    
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
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
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
    
    private SecretKey getSecretKey() throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
    }
    
    private String encrypt(String value) {
        if (value == null) return null;
        
        try {
            SecretKey secretKey = getSecretKey();
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            
            byte[] encryptedBytes = cipher.doFinal(value.getBytes());
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);
            
            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting value", e);
            return null;
        }
    }
    
    private String decrypt(String encrypted) {
        if (encrypted == null) return null;
        
        try {
            byte[] encryptedData = Base64.decode(encrypted, Base64.NO_WRAP);
            if (encryptedData.length < 12) return null;
            
            byte[] iv = new byte[12];
            byte[] encryptedBytes = new byte[encryptedData.length - 12];
            System.arraycopy(encryptedData, 0, iv, 0, 12);
            System.arraycopy(encryptedData, 12, encryptedBytes, 0, encryptedBytes.length);
            
            SecretKey secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            
            return new String(cipher.doFinal(encryptedBytes));
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