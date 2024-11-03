package psd.group4.utils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.io.FileInputStream;
import javax.crypto.KeyAgreement;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import psd.group4.handlers.InterfaceHandler;

import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.MessageDigest;

public final class Utils {

    private static final SecureRandom SECRANDOM = new SecureRandom();

    /**
     * Empty constructor to prevent initialization
     */
    private Utils() {
    }

    /**
     * Create a directory if it does not exist
     * 
     * @param path
     */
    public static void createDir(String path) {
        File serverFilesDir = new File(path);
        if (!serverFilesDir.exists()) {
            serverFilesDir.mkdirs();
        }
    }

    /**
     * Generate a random nonce
     * 
     * @return nonce
     */
    public static Long nonceGenarator() {
        return SECRANDOM.nextLong();
    }

    /**
     * Generate a random salt
     * 
     * @return salt
     */
    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        SECRANDOM.nextBytes(salt);
        return salt;
    }

    /**
     * Generate a random number of iterations
     * 
     * @return iterations
     */
    public static int generateIterations() {
        return SECRANDOM.nextInt(1000) + 1000;
    }

    /**
     * Check if a number is a power of two
     * 
     * @param n number to check
     * @return true if n is a power of two, false otherwise
     */
    public static boolean isPowerOfTwo(int n) {
        return (n > 0) && ((n & (n - 1)) == 0);
    }

    public static int getDistance(BigInteger start, BigInteger end, int ringSize) {
        return ((end.intValue() - start.intValue()+ ringSize) % ringSize);
    }

    /**
     * Instead of the ip and port we use name to be easier to find the user in the ring
     * 
     * @param name 
     * @return hashNumber
     * @throws NoSuchAlgorithmException
     */
    public static BigInteger calculateHash(String name) throws NoSuchAlgorithmException {
        // Instead of the ip and port we use name to be easier to find the user in the ring
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = md.digest(name.getBytes());

        // Convert the hash bytes directly to a BigInteger
        BigInteger hashNumber = new BigInteger(1, hashBytes);
        return hashNumber;
    }

    /**
     * Update the context of the SSL connection - truststore changed
     * 
     * @param trustStorePath
     * @param trustStorePassword
     */
    public static void loadTrustStore(String trustStorePath, String trustStorePassword) {
        try {
            // Step 1: Set the new truststore system properties
            System.setProperty("javax.net.ssl.trustStore", trustStorePath);
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);

            // Step 2: Load the new truststore
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (FileInputStream trustStoreStream = new FileInputStream(trustStorePath)) {
                trustStore.load(trustStoreStream, trustStorePassword.toCharArray());
            }

            // Step 3: Initialize a new TrustManagerFactory with the new truststore
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            // Step 4: Create a new SSLContext with the updated truststore
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            // Step 5: Set the new SSLContext as the default for future connections
            SSLContext.setDefault(sslContext);

            InterfaceHandler.success(trustStorePath + " loaded successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate a key pair for the Diffie-Hellman key exchange
     * 
     * @return key pair
     * @throws NoSuchAlgorithmException
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        // Step 1: Generate parameters
        int primeLength = 2048;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(primeLength);
        
        // Step 2: generates  key pair
        KeyPair userKeyPair = keyPairGenerator.generateKeyPair();
        return userKeyPair;
    }

    /**
     * Compute the shared secret key
     * 
     * @param userPrivateKey
     * @param pubK
     * @return shared secret key
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public static byte[] computeSKey(PrivateKey userPrivateKey, PublicKey pubK) throws NoSuchAlgorithmException, InvalidKeyException {
        // Step 4: Compute the shared secret
        KeyAgreement userKeyAgreement = KeyAgreement.getInstance("DH");
        userKeyAgreement.init(userPrivateKey);
        userKeyAgreement.doPhase(pubK, true);
        byte[] userSharedSecret = userKeyAgreement.generateSecret();

        // Ensure the shared secret is 256 bits (32 bytes)
        byte[] aesKey = new byte[32];
        System.arraycopy(userSharedSecret, 0, aesKey, 0, Math.min(userSharedSecret.length, 32));
        return aesKey;
    }

    /**
     * Convert a byte array to a certificate
     * 
     * @param bytes
     * @return certificate
     */
    public static Certificate byteArrToCertificate(byte[] bytes){
        try{
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            InputStream inStream = new ByteArrayInputStream(bytes);
            Certificate cert = certFactory.generateCertificate(inStream);
            inStream.close();
            return cert;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
