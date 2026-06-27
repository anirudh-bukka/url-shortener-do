package com.example.urlshortener.util;

/**
 * Stateless Base62 codec ({@code [0-9A-Za-z]}).
 *
 * <p>Encoding a monotonically increasing numeric id into Base62 produces short,
 * URL-safe, human-typeable aliases (e.g. id 1,000,000 -> "4c92"). Paired with a
 * database sequence, this guarantees uniqueness without any coordination between
 * concurrent requests.
 */
public final class Base62 {

    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();

    private Base62() {
    }

    /** Encode a non-negative number into its Base62 string representation. */
    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Cannot encode a negative value: " + value);
        }
        if (value == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(ALPHABET.charAt((int) (value % BASE)));
            value /= BASE;
        }
        return sb.reverse().toString();
    }

    /** Decode a Base62 string back into its numeric value. */
    public static long decode(String code) {
        long value = 0;
        for (int i = 0; i < code.length(); i++) {
            int digit = ALPHABET.indexOf(code.charAt(i));
            if (digit < 0) {
                throw new IllegalArgumentException("Illegal Base62 character: " + code.charAt(i));
            }
            value = value * BASE + digit;
        }
        return value;
    }
}
