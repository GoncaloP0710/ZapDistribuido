package utils;

import java.security.SecureRandom;

public final class Utils {

    private static final SecureRandom SECRANDOM = new SecureRandom();
    /**
     * Empty constructor to prevent initialization
     */
    private Utils() {
    }

    public static void CreateFile() {

    }

    public static Long nonceGenarator() {
        return SECRANDOM.nextLong();
    }

    public static boolean isPowerOfTwo(int n) {
        return (n > 0) && ((n & (n - 1)) == 0);
    }
}
