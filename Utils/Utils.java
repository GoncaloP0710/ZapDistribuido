package utils;

import java.security.SecureRandom;

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
}
