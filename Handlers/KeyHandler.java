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
        System.out.println("KeyHandler constructor");
        this.keyStorePassword = keyStorePassword;
        this.keyStoreString = keystoreString;

        String keystoreFilePath = "files/" + keystoreString + ".jks";
        String certificateFilePath = "files/" + keystoreString + ".cer";
        String trustStoreFilePath = "files/" + keystoreString + "_TrustStore.jks";

        // Initialize File objects using the paths
        this.keystoreFile = new File(keystoreFilePath);
        this.certificateFile = new File(certificateFilePath);
        this.trustStoreFile = new File(trustStoreFilePath);

        initialize(); 

        System.out.println("certificate trustsore: "+ trustStore.getCertificate(keystoreString));
        System.out.println("certificate keystore: "+ keyStore.getCertificate(keystoreString));
        System.out.println("keystore aliases: "+ keyStore.aliases());
        System.out.println("keyStore: "+ keyStore);
        System.out.println("keyStore key: "+ keyStore.getKey(keystoreString, keyStorePassword.toCharArray()));
        System.out.println("keystoreFile: "+ keystoreFile);

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

    public void firstTimeUser() throws Exception {
        keyStore = KeyStore.getInstance("JKS");

        // Load keystore
        keyStore.load(null, keyStorePassword.toCharArray());

        File filesDir = new File("files");
        if (!filesDir.exists()) {
            filesDir.mkdirs();
        }

        // Create keystore file path
        String keystoreFilePath = "files/" + keyStoreString + ".jks";
        this.keystoreFile = new File(keystoreFilePath);

        // Create keystore
        String[] args = new String[]{
            "keytool", "-genkeypair", "-alias", keyStoreString, "-keyalg", "RSA", "-keysize", "2048",
            "-validity", "365", "-keystore", keystoreFilePath, "-storepass", keyStorePassword,
            "-dname", "CN=a OU=a O=a L=a ST=a C=a", "-storetype", "JKS"
        };

        Process proc = new ProcessBuilder(args).start();
        int exitCode = proc.waitFor(); // Wait for the process to complete

        if (exitCode != 0) {
            throw new RuntimeException("Keystore creation process failed with exit code: " + exitCode);
        }

        System.out.println("Keystore created");
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            System.out.println("Loading keystore");
            keyStore.load(fis, keyStorePassword.toCharArray());
            System.out.println("Keystore loaded");
        }

        // Create certificate file
        createCertificate();
        String certificateFilePath = "files/" + keyStoreString + ".cer";

        // Load trustStore
        trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null, keyStorePassword.toCharArray());

        String trustStoreFilePath = "files/" + keyStoreString + "_TrustStore.jks";
        this.trustStoreFile = new File(trustStoreFilePath);

        // Save the initial truststore
        try (FileOutputStream fos2 = new FileOutputStream(trustStoreFile)) {
            trustStore.store(fos2, keyStorePassword.toCharArray());
        }

        System.out.println("Truststore created");

        // Import certificate into truststore
        String[] argsTrust = new String[]{
            "keytool", "-import", "-alias", keyStoreString, "-file", certificateFilePath,
            "-storetype", "JKS", "-keystore", trustStoreFilePath, "-noprompt", "-storepass", keyStorePassword
        };

        Process procTrust = new ProcessBuilder(argsTrust).start();
        int exitCodeTrust = procTrust.waitFor(); // Wait for the process to complete

        if (exitCodeTrust != 0) {
            throw new RuntimeException("Truststore import process failed with exit code: " + exitCodeTrust);
        }

        System.out.println("Truststore loaded");

        // Load the updated truststore
        try (FileInputStream fis2 = new FileInputStream(trustStoreFile)) {
            System.out.println("Loading truststore");
            trustStore.load(fis2, keyStorePassword.toCharArray());
            System.out.println("Truststore loaded 2");
        }

        addCertificateToTrustStore(keyStoreString, keyStore.getCertificate(keyStoreString), this.trustStoreFile);
    }

    public void createCertificate() throws Exception {
        String certificateFilePath = "files/" + keyStoreString + ".cer";

        String[] argsCert = new String[]{
            "keytool", "-exportcert", "-alias", keyStoreString, "-storetype", "JKS", "-keystore",
            "files/" + keyStoreString + ".jks", "-file", certificateFilePath
        };

        Process procCert = new ProcessBuilder(argsCert).start();
        int exitCodeCert = procCert.waitFor(); // Wait for the process to complete

        if (exitCodeCert != 0) {
            throw new RuntimeException("Certificate export process failed with exit code: " + exitCodeCert);
        }

        this.certificateFile = new File(certificateFilePath);

        if (!this.certificateFile.exists()) {
            throw new RuntimeException("Certificate file was not created: " + certificateFilePath);
        }

        System.out.println("Certificate created at: " + certificateFilePath);
    }

    public void addCertificateToTrustStore(String username, Certificate cer, File trustStoreFile) throws Exception {
        System.out.println("Adding certificate to truststore");

        // Load the truststore
        try (FileInputStream fis2 = new FileInputStream(trustStoreFile)) {
            trustStore.load(fis2, keyStorePassword.toCharArray());
            System.out.println("Truststore loaded");
        }

        // Add the certificate to the truststore
        trustStore.setCertificateEntry(username, cer);
        System.out.println("Certificate added to truststore");

        // Save the updated truststore
        try (FileOutputStream fos = new FileOutputStream(trustStoreFile)) {
            trustStore.store(fos, keyStorePassword.toCharArray());
            System.out.println("Truststore saved");
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