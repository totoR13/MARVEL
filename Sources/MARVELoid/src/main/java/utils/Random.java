package utils;

import java.util.UUID;

public class Random {
    private static final java.util.Random random;
    static {
        random = new java.util.Random(System.currentTimeMillis());
    }

    public static boolean checkChance(int chance) {
        if (chance < 0 || chance > 100) {
            throw new IllegalArgumentException("The \"chance\" argument must be between 0 and 100");
        }

        return random.nextInt(100) > (100 - chance);
    }

    public static int getRandomFromTo(int from, int to) {
        return from + random.nextInt(to);
    }

    public static String getRandomString() { // Update this functions
        return UUID.randomUUID().toString().replace("-", "");
    }

}
