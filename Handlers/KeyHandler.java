package Handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.*;
import java.security.cert.Certificate;
import java.io.BufferedReader;
import java.io.InputStreamReader;


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

    public static KeyHandler getInstance(String keyStorePassword, String keystoreString) throws Exception {
        if (instance == null) {
            instance = new KeyHandler(keyStorePassword, keystoreString);
        }
        return instance;
    }

    private KeyHandler(String keyStorePassword, String keystoreString) throws Exception {
        System.out.println("KeyHandler constructor");
        this.keyStorePassword = keyStorePassword;
        this.keyStoreString = keystoreString;
        this.keystoreFile = new File("files/"+keystoreString+".jks"); 
        this.certificateFile = new File("files/"+keystoreString +".cer");
        this.trustStoreFile = new File("files/"+keystoreString+"_TrustStore.jks");

        initialize(); 
        this.certificate = getCertificate(keystoreString);


        System.out.println("certificate trustsore: "+ trustStore.getCertificate(keystoreString));
        System.out.println("certificate keystore: "+ keyStore.getCertificate(keystoreString));
        System.out.println("keystore aliases: "+ keyStore.aliases());
        System.out.println("keyStore: "+ keyStore);
        System.out.println("keyStore key: "+ keyStore.getKey(keystoreString, keyStorePassword.toCharArray()));
        System.out.println("keystoreFile: "+ keystoreFile);
        System.out.println("certificate File: "+certificateFile.toString());
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
            // ------------------ DEBUG ------------------
    
            // Print debug information
            System.out.println("Keystore String - " + keyStoreString);
            System.out.println("Keystore - " + keyStore);
            System.out.println("KeystoreFile - " + keystoreFile);
        
            // Print aliases
            System.out.println("Alias: " + keyStore.aliases());
        
            // Print certificate
            Certificate cert = keyStore.getCertificate(keyStoreString);
            if (cert != null) {
                System.out.println("Certificate: " + cert.toString());
            } else {
                System.out.println("No certificate found for alias: " + keyStoreString);
            }
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

    public void loadTrustStore() throws Exception { // TODO: Is it suposed to be KeyStore.getDefaultType()?
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
        createKeyStore();
        createCertificate();
        createTrustStore();
        addCertificateToTrustStore(keyStoreString, keyStore.getCertificate(keyStoreString));
    }

    public static void createFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs(); // Create parent directories if they don't exist
        }
        if (file.createNewFile()) {
            System.out.println("File created: " + filePath);
        } else {
            System.out.println("File already exists: " + filePath);
        }
    }

    public void createKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InterruptedException {
        keyStore = KeyStore.getInstance("JKS");
        String keystoreFilePath = "files/" + keyStoreString + ".jks";

        //load keystore -
        keyStore.load(null, keyStorePassword.toCharArray());

        FileOutputStream fos = new FileOutputStream(keystoreFile);
        keyStore.store(fos, keyStorePassword.toCharArray());
        fos.close();
        keyStore.load(null);
        // --------------
        
        // Create keystore
        String[] args = new String[]{
            "keytool", "-genkeypair", "-alias", keyStoreString, "-keyalg", "RSA", "-keysize", "2048",
            "-validity", "365", "-keystore", keystoreFilePath, "-storepass", keyStorePassword,
            "-keypass", keyStorePassword, "-dname", "CN="+keyStoreString+", OU=a, O=a, L=a, ST=a, C=a", 
            "-storetype", "JKS", "-noprompt"
        };
        
        // Execute the keytool command
        Process proc = new ProcessBuilder(args).start();
        proc.waitFor(); // Wait without a timeout
    
        if (proc.exitValue() != 0) {
            // Capture and print the error stream
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    System.err.println("ERROR: " + errorLine);
                }
            }
            // Capture and print the standard output stream
            try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String inputLine;
                while ((inputLine = inputReader.readLine()) != null) {
                    System.out.println("OUTPUT: " + inputLine);
                }
            }
        }

        // Load the keystore
        keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystoreFilePath)) {
            keyStore.load(fis, keyStorePassword.toCharArray());
        }

        // ------------------ DEBUG ------------------
    
        // Print debug information
        System.out.println("Keystore String - " + keyStoreString);
        System.out.println("Keystore - " + keyStore);
        System.out.println("KeystoreFile - " + keystoreFile);
    
        // // Print aliases
        // System.out.println("Alias: " + keyStore.aliases());
    
        // // Print certificate
        // Certificate cert = keyStore.getCertificate(keyStoreString);
        // if (cert != null) {
        //     System.out.println("Certificate: " + cert.toString());
        // } else {
        //     System.out.println("No certificate found for alias: " + keyStoreString);
        // }
    }

    public void createTrustStore () throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InterruptedException {
        String trustStoreFilePath = "files/" + keyStoreString + "_TrustStore" + ".jks";
        String certificateFilePath = "files/" + keyStoreString + ".cer";
        
        trustStore = KeyStore.getInstance("JKS");
        
        //load trustStore
        trustStore.load(null, keyStorePassword.toCharArray());

        FileOutputStream fos = new FileOutputStream(trustStoreFile);
        trustStore.store(fos, keyStorePassword.toCharArray());
        fos.close();
        trustStore.load(null);

        //create truststore File
        String[] argsTrust = new String[]{
            "keytool", "-import", "-alias", keyStoreString, "-file", certificateFilePath, 
            "-storetype", "JKS","-keystore", trustStoreFilePath, "-storepass", keyStorePassword,
            "-noprompt" //ainda sussy
        };
        Process procTrust = new ProcessBuilder(argsTrust).start(); 
        procTrust.waitFor();

        if (procTrust.exitValue() != 0) {
            // Capture and print the error stream
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(procTrust.getErrorStream()))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    System.err.println("ERROR: " + errorLine);
                }
            }
            // Capture and print the standard output stream
            try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(procTrust.getInputStream()))) {
                String inputLine;
                while ((inputLine = inputReader.readLine()) != null) {
                    System.out.println("OUTPUT: " + inputLine);
                }
            }
        }

        trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(trustStoreFilePath)) {
            trustStore.load(fis, keyStorePassword.toCharArray());
            fis.close();
        }

        // try{
        //     addCertificateToTrustStore(keyStoreString, keyStore.getCertificate(keyStoreString));
        // } catch(Exception e){
        //     System.out.println("deu merda");
        // }
        

        // System.out.println("Keystore created " + trustStore.toString());
        System.out.println("Keystore created " + trustStore);
    }

    public void createCertificate() throws Exception{
        String certificateFilePath = "files/" + keyStoreString + ".cer";

        String[] argsCert = new String[]{
            "keytool", "-exportcert", "-alias", keyStoreString, "-keystore", 
            "files/"+keyStoreString + ".jks", "-file", certificateFilePath,
            "-storepass", keyStorePassword 
        };

        Process procCert = new ProcessBuilder(argsCert).start(); 
        procCert.waitFor(); 

        System.out.println("Certificate created" + certificateFile);
    }


    public void addCertificateToTrustStore(String username, Certificate cer) throws Exception{
        String trustStoreFilePath = "files/" + keyStoreString + "_TrustStore" + ".jks";
        // String certificateFilePath = "files/" + keyStoreString + ".cer";
        System.out.println("Loading truststore");
        try (FileInputStream fis = new FileInputStream(trustStoreFilePath)) {
            trustStore.load(fis, keyStorePassword.toCharArray());
        }
        System.out.println("Truststore loaded");

        System.out.println("Adding certificate to truststore");
        trustStore.setCertificateEntry(username, cer);
        System.out.println("Certificate added to truststore");

        // Save the updated truststore
        try (FileOutputStream fos = new FileOutputStream(trustStoreFilePath)) {
            trustStore.store(fos, keyStorePassword.toCharArray());
        }
        System.out.println("Truststore saved");
    }

    //------------------------------------ GETTERS -------------------------------------------

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