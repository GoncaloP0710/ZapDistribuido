package handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.NoSuchAlgorithmException;

public class KeyHandler {
    
    private KeyStore keyStore;
    private String keyStorePassword;
    //private String keyStoreFile;
    private File userFile;

    public KeyHandler(String keyStorePassword, String user_id) throws Exception {
        this.keyStorePassword = keyStorePassword;

        this.userFile = new File("../files/"+user_id);
        
        if(!userFile.createNewFile()){
            
        } else {

        }

        




        this.keyStore = initializeKeyStore();
    }

    private KeyStore initializeKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS"); 
        try (FileInputStream in = new FileInputStream(keyStoreFile)) {
            ks.load(in, keyStorePassword.toCharArray());
        } catch (IOException e) { // If the file does not exist, create a new one
            ks.load(null, keyStorePassword.toCharArray());
        }
        
        return ks;
    }

    private void saveKeyStore() throws Exception {
        try (FileOutputStream out = new FileOutputStream(keyStoreFile)) {
            keyStore.store(out, keyStorePassword.toCharArray());
        }
    }

    public PublicKey getPublicKey(String alias) throws Exception {
        return keyStore.getCertificate(alias).getPublicKey();
    }

    public PrivateKey getPrivateKey(String alias, String keyPassword) throws Exception {
        return (PrivateKey) keyStore.getKey(alias, keyPassword.toCharArray());
    }


    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }


    public boolean containsAlias(String alias) throws KeyStoreException {
        return keyStore.containsAlias(alias);
    }

    public void deleteEntry(String alias) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        keyStore.deleteEntry(alias);
        try {
            saveKeyStore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
