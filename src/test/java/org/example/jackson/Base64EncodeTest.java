package org.example.jackson;

import org.example.jackson3.updated.PR1674Base64Variant;
import org.junit.jupiter.api.Test;
import tools.jackson.core.Base64Variant;
import tools.jackson.core.Base64Variants;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Differential check that {@link PR1674Base64Variant} produces exactly what the shipped
 * {@link Base64Variant#encode(byte[], boolean)} produces -- otherwise the benchmark would be
 * comparing two different amounts of work.
 */
class Base64EncodeTest {

    private static final Base64Variant[] VARIANTS = {
            Base64Variants.MIME,
            Base64Variants.MIME_NO_LINEFEEDS,
            Base64Variants.PEM,
            Base64Variants.MODIFIED_FOR_URL,
    };

    /** Input lengths 0..400 cover every leftover count and several line boundaries per variant. */
    @Test
    void matchesShippedEncode() {
        Random random = new Random(1234L);
        for (Base64Variant variant : VARIANTS) {
            PR1674Base64Variant pr1674 = new PR1674Base64Variant(variant);
            for (int len = 0; len <= 400; len++) {
                byte[] input = new byte[len];
                random.nextBytes(input);
                for (boolean addQuotes : new boolean[] { false, true }) {
                    assertEquals(variant.encode(input, addQuotes),
                            pr1674.encode(input, addQuotes),
                            () -> variant.getName() + ", len=" + input.length + ", quotes=" + addQuotes);
                }
            }
        }
    }

    /** The 3-arg overload, including the empty linefeed and a multi-char one. */
    @Test
    void matchesShippedEncodeWithLinefeed() {
        Random random = new Random(5678L);
        for (Base64Variant variant : VARIANTS) {
            PR1674Base64Variant pr1674 = new PR1674Base64Variant(variant);
            for (String linefeed : new String[] { "", "\n", "\r\n", "<%>" }) {
                for (int len = 0; len <= 200; len++) {
                    byte[] input = new byte[len];
                    random.nextBytes(input);
                    assertEquals(variant.encode(input, true, linefeed),
                            pr1674.encode(input, true, linefeed),
                            () -> variant.getName() + ", len=" + input.length + ", lf='" + linefeed + "'");
                }
            }
        }
    }
}
