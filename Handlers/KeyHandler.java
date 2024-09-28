package handlers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.NoSuchAlgorithmException;

public class KeyHandler {
    
    private KeyStore keyStore;
    private String keyStorePassword;
    private String keyStoreFile;

    public KeyHandler(String keyStoreFile, String keyStorePassword) throws Exception {
        this.keyStoreFile = keyStoreFile;
        this.keyStorePassword = keyStorePassword;
        this.keyStore = loadKeyStore();
    }

    private KeyStore loadKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS"); 
        try (FileInputStream in = new FileInputStream(keyStoreFile)) {
            ks.load(in, keyStorePassword.toCharArray());
        } catch (IOException e) { // If the file does not exist, create a new one
            ks.load(null, keyStorePassword.toCharArray());
        }
        return ks;
    }

    public void generateAndStoreKey (String user_id, String keyPassword) throws Exception {
        KeyPair keyPair = generateKeyPair();
        saveKeyStore();
    }

    private void saveKeyStore() throws Exception {
        try (FileOutputStream out = new FileOutputStream(keyStoreFile)) {
            keyStore.store(out, keyStorePassword.toCharArray());
        }
    }


    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }


    public PrivateKey getPrivateKey(String user_id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPrivateKey'");
    }

    public PublicKey getPublicKey(String user_id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPublicKey'");
    }

    public void initializeKeyStore() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'initializeKeyStore'");
    }

    public void storeKeys(String user_id, KeyPair keyPair) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'storeKeys'");
    }

}
