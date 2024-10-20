package Handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.*;
import java.util.concurrent.TimeUnit;
import java.security.cert.Certificate;

// import sun.security.x509.*; // Internal package for creating self-signed certificates

public class KeyHandler {

    private static KeyHandler instance; // Singleton instance

    private KeyStore keyStore;
    private String keyStorePassword;
    private String keyStoreString;
    private File keystoreFile;
    private Certificate certificate;
    private File certificateFile;
    private KeyStore trustStore;
    private File trustStoreFile;

    private KeyHandler(String keyStorePassword, String keystoreString) throws Exception {
        this.keyStorePassword = keyStorePassword;
        this.keyStoreString = keystoreString;
        this.keystoreFile = new File("/files/"+keystoreString+".jks"); 
        initialize(); 
        this.certificate = getCertificate(keystoreString);

    }

    public void initialize() throws Exception{
        if(!isFirstTimeUser(keyStoreString)){
            System.out.println("Not first time user");
            loadKeyStore();
            loadTrustStore();
        } else{
            System.out.println("First time user");
            firstTimeUser();
        }
    }

    public static KeyHandler getInstance(String keyStorePassword, String keystoreString) throws Exception {
        if (instance == null) {
            instance = new KeyHandler(keyStorePassword, keystoreString);
        }
        return instance;
    }

    public Boolean isFirstTimeUser(String userName) {
        File userFile = new File("/files/" + userName + ".jks");
        return !userFile.exists();
    }

    public Boolean existsStore(String userName) throws Exception {
        File keyStoreFile = new File("/files/" + userName + ".jks");
        File trustStoreFile = new File("/files/" + userName + "_TrustStore.jks");
        File certificateFile = new File("/files/" + userName + ".cer");

        if (keyStoreFile.exists() && trustStoreFile.exists()) {
            if (!certificateFile.exists()) {
                createCertificate();
            }
        } else {
            return false;
        } 
    
        return true;
    }

    public void loadKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            ks.load(fis, keyStorePassword.toCharArray());
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            if (e.getCause() instanceof UnrecoverableKeyException) {
                System.err.println("Error initializing KeyStore: Incorrect password");
            } else {
                System.err.println("Error initializing KeyStore: " + e.getMessage());
            }
            throw new Exception("Failed to initialize KeyStore", e);
        }
        this.keyStore = ks;
    }

    public void loadTrustStore() throws Exception {
        KeyStore ts = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(trustStoreFile)) {
            ts.load(fis, keyStorePassword.toCharArray());
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            if (e.getCause() instanceof UnrecoverableKeyException) {
                System.err.println("Error initializing TrustStore: Incorrect password");
            } else {
                System.err.println("Error initializing TrustStore: " + e.getMessage());
            }
            throw new Exception("Failed to initialize TrustStore", e);
        }

        this.trustStore = ts;
    }

    public void firstTimeUser() throws Exception{

        keyStore = KeyStore.getInstance("JKS");

        //load keystore
        keyStore.load(null, keyStorePassword.toCharArray());

        FileOutputStream fos = new FileOutputStream(keystoreFile);
        keyStore.store(fos, keyStorePassword.toCharArray());
        fos.close();
        keyStore.load(null);

        //create keystore
        String[] args = new String[]{//"/bin/bash", "-c",
            "keytool", "-genkeypair", "-alias", keyStoreString, "-keyalg", "RSA", "-keysize", "2048",
            "-validity", "365", "-keystore", keyStoreString + ".jks", "-storepass", keyStorePassword,
            "-dname", "CN=a OU=a O=a L=a ST=a C=a", "-storetype", "JKS" //ainda sussy
        };
        Process proc = new ProcessBuilder(args).start(); 
        proc.waitFor(10, TimeUnit.SECONDS); //precisamos?
          
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            keyStore.load(fis, keyStorePassword.toCharArray());
            fis.close();
        }

        this.keystoreFile = new File("/files/"+keyStoreString+".jks"); 

        //create certificate File
        createCertificate();

        //load trustStore
        trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null, keyStorePassword.toCharArray());

        FileOutputStream fos2 = new FileOutputStream(trustStoreFile);
        trustStore.store(fos2, keyStorePassword.toCharArray());
        fos2.close();
        trustStore.load(null);

        //create truststore File
        String[] argsTrust = new String[]{
            "keytool", "-import", "-alias", keyStoreString, "-file", keyStoreString + ".cer", 
            "-storetype", "JKS","-keystore", keyStoreString + "_TrustStore" + ".jks" //ainda sussy
        };
        Process procTrust = new ProcessBuilder(argsTrust).start(); 
        procTrust.waitFor(10, TimeUnit.SECONDS); //precisamos?
        this.trustStoreFile = new File("/files/"+keyStoreString + "_TrustStore" + ".jks");

        try (FileInputStream fis2 = new FileInputStream(trustStoreFile)) {
            trustStore.load(fis2, keyStorePassword.toCharArray());
            fis2.close();
        }
        
    }

    public void createCertificate() throws Exception{
       String[] argsCert = new String[]{
            "keytool", "-exportcert", "-alias", keyStoreString, "-storetype", "JKS", "-keystore", 
            keyStoreString + ".jks", "-file", keyStoreString + ".cer" //ainda sussy
        };
        Process procCert = new ProcessBuilder(argsCert).start(); 
        procCert.waitFor(10, TimeUnit.SECONDS); //precisamos?
        this.certificateFile = new File("/files/"+keyStoreString+".cer");
    }


    public void addCertificateToTrustStore(String username, Certificate cer) throws Exception{
        trustStore.setCertificateEntry(username, cer);

        try (FileInputStream fis2 = new FileInputStream(trustStoreFile)) {
            trustStore.load(fis2, keyStorePassword.toCharArray());
            fis2.close();
        }
        
    }

    public PublicKey getPublicKey(String alias) throws Exception {
        return keyStore.getCertificate(alias).getPublicKey();
    }

    public PrivateKey getPrivateKey(String alias, String keyPassword) throws Exception {
        return (PrivateKey) keyStore.getKey(alias, keyPassword.toCharArray());
    }

    public Certificate getCertificate(String alias) throws Exception {
        return keyStore.getCertificate(alias);
    }

    public PublicKey getPublicKey() throws Exception {
        return keyStore.getCertificate(keyStoreString).getPublicKey();
    }

    public PrivateKey getPrivateKey() throws Exception {
        return (PrivateKey) keyStore.getKey(keyStoreString, keyStorePassword.toCharArray());
    }

    public Certificate getCertificate() throws Exception {
        return keyStore.getCertificate(keyStoreString);
    }

    public String getKeystoString(){
        return keyStore.toString();
    }

    public String getUsername() {
        return keyStoreString;
    }

    public String getKeyStorePassword(){
        return keyStorePassword;
    }

    public String getTrustStoreString(){
        return trustStore.toString();
    }

    public File getCertificateFile(){
        return certificateFile;
    }

    public File getTrustStoreFile(){
        return trustStoreFile;
    }

    public File getKeystoreFile(){
        return keystoreFile;
    }

    public KeyStore getTruStore(){
        return trustStore;
    }

    public KeyStore getKeyStore(){
        return keyStore;
    }

}

