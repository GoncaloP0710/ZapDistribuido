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
import Utils.Utils;

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

    private String keyStorePath;
    private String trustStorePath;
    private String certificatePath;

    public static KeyHandler getInstance(String keyStorePassword, String keystoreString) throws Exception {
        if (instance == null) {
            instance = new KeyHandler(keyStorePassword, keystoreString);
        }
        return instance;
    }

    public static KeyHandler newInstance(String keyStorePassword, String keystoreString) throws Exception {
        instance = new KeyHandler(keyStorePassword, keystoreString);
        return instance;
    }
    

    private KeyHandler(String keyStorePassword, String keystoreString) throws Exception {
        this.keyStorePassword = keyStorePassword;
        this.keyStoreString = keystoreString;

        this.keyStorePath = "files/" + keystoreString + "/" + keystoreString + ".jks";
        this.trustStorePath = "files/" + keystoreString + "/" + keystoreString + "_TrustStore.jks";
        this.certificatePath = "files/" + keystoreString + "/" + keystoreString + ".cer";

        // Create the 'files' directory
        Utils.createDir("files");

        // Create a subdirectory named by the username inside 'files'
        String userDirPath = "files/" + keystoreString;
        Utils.createDir(userDirPath);

        this.keystoreFile = new File(userDirPath + "/" + keystoreString + ".jks"); 
        this.certificateFile = new File(userDirPath + "/" + keystoreString + ".cer");
        this.trustStoreFile = new File(userDirPath + "/" + keystoreString + "_TrustStore.jks");

        initialize(); 
        
    }

    public void initialize() throws Exception{
        if(!isFirstTimeUser(keyStoreString)){ //existing user
            try {
                loadKeyStore();
                loadTrustStore();
            } catch (Exception e) {
                if (!(e.getCause() instanceof UnrecoverableKeyException)) {
                    System.err.println("Error initializing TrustStore: " + e.getMessage());
                }
                return;
            }
        } else{ //new user
            firstTimeUser();
        }
        this.certificate = getCertificate(keyStoreString);
    }

    public Boolean isFirstTimeUser(String userName) {
        File userFile = new File("files/" + userName + "/" + userName + ".jks");
        return !userFile.exists();
    }

    public Boolean existsStore(String userName) throws Exception {
        File keyStoreFile = new File(keyStorePath);
        File trustStoreFile = new File(trustStorePath);
        File certificateFile = new File(certificatePath);

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
        keyStore = KeyStore.getInstance("JKS");
        try(FileInputStream fis = new FileInputStream(keyStorePath)){
            keyStore.load(fis, keyStorePassword.toCharArray());
        }catch(Exception e){
            throw e;
        }
        
 
    }

    public void loadTrustStore() throws Exception { 
        trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(trustStoreFile)) {
            trustStore.load(fis, keyStorePassword.toCharArray());
        }catch(Exception e){
            throw e;
        }

    }

    public void firstTimeUser() throws Exception{
        createKeyStore();
        createCertificate();
        createTrustStore();
        addCertificateToTrustStore(keyStoreString, keyStore.getCertificate(keyStoreString));
    }

    public void createKeyStore() 
    throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InterruptedException {

        keyStore = KeyStore.getInstance("JKS");
        // String keystoreFilePath = "files/" + keyStoreString + ".jks";

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
            "-validity", "365", "-keystore", keyStorePath, "-storepass", keyStorePassword,
            "-keypass", keyStorePassword, "-dname", "CN="+keyStoreString+", OU=a, O=a, L=a, ST=a, C=a", 
            "-storetype", "JKS", "-noprompt"
        };
        
        // Execute the keytool command
        Process proc = new ProcessBuilder(args).start();
        proc.waitFor(); // Wait without a timeout
    
        printKeytoolError(proc);

        // Load the keystore
        keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keyStorePath)) {
            keyStore.load(fis, keyStorePassword.toCharArray());
        }
    }

    public void createTrustStore () 
    throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InterruptedException {
        // String trustStoreFilePath = "files/" + keyStoreString + "_TrustStore" + ".jks";
        // String certificateFilePath = "files/" + keyStoreString + ".cer";
        
        trustStore = KeyStore.getInstance("JKS");
        
        //load trustStore
        trustStore.load(null, keyStorePassword.toCharArray());

        FileOutputStream fos = new FileOutputStream(trustStoreFile);
        trustStore.store(fos, keyStorePassword.toCharArray());
        fos.close();
        trustStore.load(null);

        //create truststore File
        String[] argsTrust = new String[]{
            "keytool", "-import", "-alias", keyStoreString, "-file", certificatePath, 
            "-storetype", "JKS","-keystore", trustStorePath, "-storepass", keyStorePassword,
            "-noprompt" //ainda sussy
        };
        Process procTrust = new ProcessBuilder(argsTrust).start(); 
        procTrust.waitFor();
        printKeytoolError(procTrust);
        

        trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(trustStorePath)) {
            trustStore.load(fis, keyStorePassword.toCharArray());
            fis.close();
        }
    }

    public void createCertificate() throws Exception{
        // String certificateFilePath = "files/" + keyStoreString + ".cer";

        String[] argsCert = new String[]{
            "keytool", "-exportcert", "-alias", keyStoreString, "-keystore", 
           keyStorePath, "-file", certificatePath,
            "-storepass", keyStorePassword 
        };

        Process procCert = new ProcessBuilder(argsCert).start(); 
        procCert.waitFor(); 
        printKeytoolError(procCert);
        
    }

    public void printKeytoolError(Process proc) 
    throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InterruptedException {
        if (proc.exitValue() != 0) {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    System.err.println("ERROR: " + errorLine);
                }
            }
            try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String inputLine;
                while ((inputLine = inputReader.readLine()) != null) {
                    System.out.println("OUTPUT: " + inputLine);
                }
            }
        }
    }


    public void addCertificateToTrustStore(String username, Certificate cer) throws Exception {
        // String trustStoreFilePath = "files/" + keyStoreString + "_TrustStore" + ".jks";

        // Load the truststore
        try (FileInputStream fis = new FileInputStream(trustStorePath)) {
            trustStore.load(fis, keyStorePassword.toCharArray());
        } catch (Exception e) {
            System.err.println("Error loading truststore: " + e.getMessage());
            throw new Exception("Failed to load truststore", e);
        }

        // Add the certificate to the truststore
        try {
            trustStore.setCertificateEntry(username, cer);
        } catch (Exception e) {
            System.err.println("Error adding certificate to truststore: " + e.getMessage());
            throw new Exception("Failed to add certificate to truststore", e);
        }

        // Save the updated truststore
        try (FileOutputStream fos = new FileOutputStream(trustStorePath)) {
            trustStore.store(fos, keyStorePassword.toCharArray());
        } catch (Exception e) {
            System.err.println("Error saving truststore: " + e.getMessage());
            throw new Exception("Failed to save truststore", e);
        }
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

    public String getTruststorePath() {
        return this.trustStoreFile.getAbsolutePath();
    }

    public String getKeystorePath() {
        return this.keystoreFile.getAbsolutePath();
    }

    public String getCertificatePath() {
        return this.certificateFile.getAbsolutePath();
    }

}