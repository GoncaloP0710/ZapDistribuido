package psd.group4.utils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.Signature;
import java.io.FileInputStream;
import java.io.IOException;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import psd.group4.handlers.EncryptionHandler;
import psd.group4.handlers.InterfaceHandler;

import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Random;
import java.security.MessageDigest;
import javax.crypto.Mac;

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

    /**
     * Generate a random byte array to serve as an attribute for ABE
     * 
     * @param length the length of the byte array
     * @return a random byte array
     */
    public static byte[] generateRandomAttribute(int length) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        return randomBytes;
    }

    public static <T extends Serializable> byte[] serialize(T object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        }
    }

    public static <T extends Serializable> T deserialize(byte[] bytes, Class<T> clazz) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            return clazz.cast(in.readObject());
        }
    }

    /**
     * Generates an HMAC for a given message using the specified secret key and algorithm.
     *
     * @param algorithm the HMAC algorithm (e.g., HmacSHA256, HmacSHA1)
     * @param key       the secret key as a byte array
     * @param message   the message as a byte array
     * @return the computed HMAC as a hexadecimal string
     * @throws Exception if any error occurs during HMAC computation
     */
    public static byte[] generateHMAC(String algorithm, byte[] key, byte[] message) throws Exception {
        try {
            // Create a secret key specification based on the provided key
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, algorithm);

            // Initialize the Mac instance with the specified algorithm
            Mac mac = Mac.getInstance(algorithm);
            mac.init(secretKeySpec);

            // Compute the HMAC
            byte[] hmacBytes = mac.doFinal(message);
            return hmacBytes;
        } catch (Exception e) {
            throw new Exception("Error while generating HMAC", e);
        }
    }

    // Encode a byte array to a group element
    public static Element encodeBytesToGroup(Pairing pairing, byte[] data) {
        // Convert the byte array to a BigInteger
        java.math.BigInteger bigInteger = new java.math.BigInteger(1, data);
        return pairing.getGT().newElement(bigInteger).getImmutable();
    }

    // Decode a group element back to a byte array
    public static byte[] decodeGroupToBytes(Element element) {
        // Convert the group element to a BigInteger
        java.math.BigInteger bigInteger = element.toBigInteger();
        // Convert the BigInteger to a byte array
        return bigInteger.toByteArray();
    }

    public static String[] generateAttributesForPolicy(String policy) {
        // Extract numbers from the policy
        String[] tokens = policy.split("[^0-9]+");
        return Arrays.stream(tokens)
                .filter(token -> !token.isEmpty())
                .distinct()
                .toArray(String[]::new);
    }

    public static String generateRandomPolicy() {
        Random random = new Random();
        int numClauses = random.nextInt(3) + 1; // Number of clauses in the policy
        StringBuilder policy = new StringBuilder();
    
        for (int i = 0; i < numClauses; i++) {
            if (i > 0) {
                policy.append(" and ");
            }
            int numTerms = random.nextInt(2) + 1; // Number of terms in the clause
            if (numTerms == 1) {
                policy.append(random.nextInt(50));
            } else {
                policy.append("(");
                for (int j = 0; j < numTerms; j++) {
                    if (j > 0) {
                        policy.append(" or ");
                    }
                    policy.append(random.nextInt(50));
                }
                policy.append(")");
            }
        }
    
        return policy.toString();
    }

    /**
     * Verifies the signature of the message with the public key of the sender
     * 
     * @param senderPubKey
     * @param messageEncrypted
     * @param hashSigned
     * @return
     * @throws Exception
     */
    public static boolean verifySignature(PublicKey senderPubKey, byte[] messageEncrypted, byte[] hashSigned) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(senderPubKey);
        signature.update(EncryptionHandler.createMessageHash(messageEncrypted));
        return signature.verify(hashSigned);
    }
}
