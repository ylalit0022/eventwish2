package com.ds.eventwish.util;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import java.util.ArrayList;

/**
 * Utility class for securely storing authentication tokens using Android KeyStore.
 * This class uses AES encryption in GCM mode to protect the tokens.
 */
public class SecureTokenManager {
    private static final String TAG = "SecureTokenManager";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "EventWishAuthKey";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final Context context;
    private final KeyStore keyStore;
    
    private static volatile SecureTokenManager instance;
    
    /**
     * Initialize the SecureTokenManager
     * @param context Application context
     */
    public static void init(Context context) {
        getInstance(context);
    }
    
    private SecureTokenManager(Context context) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        this.context = context.getApplicationContext();
        keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        createKeyIfNeeded();
    }
    
    /**
     * Get instance of SecureTokenManager
     * @param context Application context
     * @return Instance of SecureTokenManager
     */
    public static SecureTokenManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SecureTokenManager.class) {
                if (instance == null) {
                    try {
                        instance = new SecureTokenManager(context);
                    } catch (Exception e) {
                        Log.e(TAG, "Error initializing SecureTokenManager", e);
                        throw new RuntimeException("Failed to initialize SecureTokenManager", e);
                    }
                }
            }
        }
        return instance;
    }
    
    /**
     * Create encryption key if it doesn't exist
     */
    private void createKeyIfNeeded() {
        try {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                createEncryptionKey();
            } else {
                Log.d(TAG, "Key already exists");
            }
        } catch (KeyStoreException e) {
            Log.e(TAG, "Error creating key", e);
            throw new RuntimeException("Failed to create encryption key", e);
        }
    }
    
    /**
     * Encrypts a token string using the Android KeyStore.
     * 
     * @param token The token string to encrypt
     * @return Base64 encoded encrypted token or null if encryption fails
     */
    public static String encryptToken(String token) {
        try {
            final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            
            // Create or get the encryption key
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                createEncryptionKey();
            }
            
            final KeyStore.SecretKeyEntry secretKeyEntry = 
                    (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            final SecretKey secretKey = secretKeyEntry.getSecretKey();
            
            // Initialize cipher for encryption
            final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            // Get the IV that was generated
            byte[] iv = cipher.getIV();
            
            // Encrypt the token
            byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedBytes = cipher.doFinal(tokenBytes);
            
            // Combine IV and encrypted token (IV is needed for decryption)
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);
            
            // Encode as Base64 for storage
            return Base64.encodeToString(combined, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting token", e);
            return null;
        }
    }
    
    /**
     * Decrypts a previously encrypted token.
     * 
     * @param encryptedToken Base64 encoded encrypted token
     * @return Decrypted token or null if decryption fails
     */
    public static String decryptToken(String encryptedToken) {
        try {
            // Decode the Base64 string
            byte[] combined = Base64.decode(encryptedToken, Base64.DEFAULT);
            
            // Extract IV and encrypted token
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedBytes = new byte[combined.length - GCM_IV_LENGTH];
            
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encryptedBytes, 0, encryptedBytes.length);
            
            // Get the secret key from KeyStore
            final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                Log.e(TAG, "Encryption key not found");
                return null;
            }
            
            final KeyStore.SecretKeyEntry secretKeyEntry = 
                    (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            final SecretKey secretKey = secretKeyEntry.getSecretKey();
            
            // Initialize cipher for decryption with the IV used during encryption
            final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            final GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            
            // Decrypt
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting token", e);
            return null;
        }
    }
    
    /**
     * Creates a new encryption key in the Android KeyStore.
     */
    private static void createEncryptionKey() {
        try {
            final KeyGenerator keyGenerator = 
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            
            final KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build();
            
            keyGenerator.init(keyGenParameterSpec);
            keyGenerator.generateKey();
        } catch (Exception e) {
            Log.e(TAG, "Error creating encryption key", e);
        }
    }
    
    /**
     * Removes the encryption key from the Android KeyStore.
     * 
     * @return true if key was successfully cleared, false otherwise
     */
    public static boolean clearKeys() {
        try {
            final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error clearing encryption keys", e);
            return false;
        }
    }
    
    /**
     * Lists all keys in the Android KeyStore (for debugging purposes).
     * 
     * @return List of key aliases as strings
     */
    public static ArrayList<String> listKeys() {
        ArrayList<String> keys = new ArrayList<>();
        try {
            final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            
            java.util.Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                keys.add(aliases.nextElement());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error listing keys", e);
        }
        return keys;
    }
}