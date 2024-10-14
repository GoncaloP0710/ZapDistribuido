package Handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Scanner;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
// import sun.security.x509.*; // Internal package for creating self-signed certificates

public class KeyHandler {

    private KeyStore keyStore;
    private String keyStorePassword;
    private File keystoreFile;

    public KeyHandler(String keyStorePassword, String keystoreString) throws Exception {
        this.keyStorePassword = keyStorePassword;
        this.keystoreFile = new File("/files/"+keystoreString); 
        this.keyStore = initializeKeyStore(keystoreString);
        Certificate cer = (Certificate) keyStore.getCertificate("user");
        PrivateKey privK = (PrivateKey) keyStore.getKey("user", keystoreString.toCharArray());
        PublicKey pubK = cer.getPublicKey();

    }

    private KeyStore initializeKeyStore(String keystoreString) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        
        if (keystoreFile.exists()) {
            try (FileInputStream fis = new FileInputStream(keystoreString)) {
                ks.load(fis, keyStorePassword.toCharArray());
            }
        } else {
            Exception e = new Exception("Keystore file not found");
            e.printStackTrace();
            //ks.load(null, keyStorePassword.toCharArray());
            
        }
        return ks;
    }

    public PublicKey getPublicKey(String alias) throws Exception {
        return keyStore.getCertificate(alias).getPublicKey();
    }

    public PrivateKey getPrivateKey(String alias, String keyPassword) throws Exception {
        return (PrivateKey) keyStore.getKey(alias, keyPassword.toCharArray());
    }

}

    
    // private KeyStore keyStore;
    // private String keyStorePassword;
    // //private String keyStoreFile;
    // private File keystoreFile;

    // public KeyHandler(String keyStorePassword, String user_name) throws Exception {
    //     this.keyStorePassword = keyStorePassword;

    //     this.keystoreFile = new File("../files/keystores");

    //     String info = findUser(keystoreFile, user_name);

    //     if(info == null){
    //         //nao encontra userName -> cria e guarda keystore
    //         KeyStore ks = createKeyStoreForUser(user_name); //cria keystore
            
    //         //guarda keystore
    //         FileWriter fw = new FileWriter(keystoreFile, true);
    //         String linha = "\n" + user_name + " " + ks.toString();
    //         fw.append(linha);
    //         fw.close();

    //     }
        
    //     //encontra userName no ficheiro -> apanha a keystore
    //     this.keyStore = initializeKeyStore();
    // }

    // private static String findUser(File file, String user) throws FileNotFoundException{
    //     Scanner sc = new Scanner(file);
    //     while(sc.hasNextLine()){
    //         String s = sc.nextLine();
    //         if(s.startsWith(user + " ")){
    //             sc.close();
    //             return s;
    //         }
    //     }
    //     sc.close();
    //     return null;

    // }

    // private KeyStore createKeyStoreForUser(String userName) throws Exception {
    //     KeyStore ks = initializeKeyStore();
    //     KeyPair keyPair = generateKeyPair();
    //     PrivateKey privateKey = keyPair.getPrivate();
    //     PublicKey publicKey = keyPair.getPublic();
    //     char[] passwordBytes = keyStorePassword.toCharArray();

    //     // Create a self-signed certificate
    //     X509Certificate certificate = generateSelfSignedCertificate(userName, keyPair);

    //     // Store the key pair in the keystore
    //     ks.setKeyEntry(userName, privateKey, passwordBytes, new Certificate[]{certificate});
        
    //     return ks;
    // }

    // //this dont work 
    


    // private KeyStore initializeKeyStore() throws Exception {
    //     KeyStore ks = KeyStore.getInstance("JKS"); 
    //     try (FileInputStream in = new FileInputStream(keystoreFile)) {
    //         ks.load(in, keyStorePassword.toCharArray());
    //     } catch (IOException e) { // If the file does not exist, create a new one
    //         ks.load(null, keyStorePassword.toCharArray());
    //     }
        
    //     return ks;
    // }

    // private void saveKeyStore() throws Exception {
    //     try (FileOutputStream out = new FileOutputStream(keystoreFile)) {
    //         keyStore.store(out, keyStorePassword.toCharArray());
    //     }
    // }

    // public PublicKey getPublicKey(String alias) throws Exception {
    //     return keyStore.getCertificate(alias).getPublicKey();
    // }

    // public PrivateKey getPrivateKey(String alias, String keyPassword) throws Exception {
    //     return (PrivateKey) keyStore.getKey(alias, keyPassword.toCharArray());
    // }


    // public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
    //     KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    //     keyPairGenerator.initialize(2048);
    //     return keyPairGenerator.generateKeyPair();
    // }


    // public boolean containsAlias(String alias) throws KeyStoreException {
    //     return keyStore.containsAlias(alias);
    // }

    // public void deleteEntry(String alias) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    //     keyStore.deleteEntry(alias);
    //     try {
    //         saveKeyStore();
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }

    // public KeyStore loadKeyStore() throws Exception {
    //     this.keyStore = KeyStore.getInstance("JCEKS");
    //     try (InputStream keystoreStream = new FileInputStream(keystoreFile)) {
    //         keyStore.load(keystoreStream, keystorePassword.toCharArray());
    //         return keyStore;
    //     } catch (IOException e) {
    //         return null;
    //     }

    // }

