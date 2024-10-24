package Utils;

import java.math.BigInteger;
import java.security.SecureRandom;

import java.io.FileInputStream;
import javax.net.ssl.SSLContext;

import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;


public final class Utils {

    private static final SecureRandom SECRANDOM = new SecureRandom();

    /**
     * Empty constructor to prevent initialization
     */
    private Utils() {
    }

    public static Long nonceGenarator() {
        return SECRANDOM.nextLong();
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        SECRANDOM.nextBytes(salt);
        return salt;
    }

    public static int generateIterations() {
        return SECRANDOM.nextInt(1000) + 1000;
    }

    public static boolean isPowerOfTwo(int n) {
        return (n > 0) && ((n & (n - 1)) == 0);
    }

    public static int getDistance(BigInteger start, BigInteger end, int ringSize) {
        return ((end.intValue() - start.intValue()+ ringSize) % ringSize);
    }

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

            System.out.println("Truststore has been reloaded and SSLContext has been updated.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
