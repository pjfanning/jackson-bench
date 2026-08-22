package org.example.jackson;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.dataformat.smile.SmileMapper;

import java.io.ByteArrayInputStream;

/**
 * Measures Smile parsing throughput when databinding a large array of doubles.
 * <p>
 * The Smile document is generated once in the static initializer from a deterministic
 * {@code double[]} of {@value #DOUBLE_COUNT} values, so every benchmark iteration parses
 * exactly the same valid input.
 */
public class SmileDoubleArrayBench extends BenchmarkLauncher {

    private static final int DOUBLE_COUNT = 1_000;

    private static final SmileMapper MAPPER = new SmileMapper();

    /** The values the encoded document holds; kept for verification/sanity checks. */
    static final double[] DOUBLES;

    /** Valid Smile encoding of {@link #DOUBLES} (an array of doubles). */
    static final byte[] SMILE_DOC;

    static {
        DOUBLES = generateDoubles(DOUBLE_COUNT);
        SMILE_DOC = MAPPER.writeValueAsBytes(DOUBLES);
    }

    @Benchmark
    public void benchParseFromByteArray(Blackhole blackhole) {
        blackhole.consume(MAPPER.readValue(SMILE_DOC, double[].class));
    }

    @Benchmark
    public void benchParseFromInputStream(Blackhole blackhole) {
        blackhole.consume(MAPPER.readValue(new ByteArrayInputStream(SMILE_DOC), double[].class));
    }

    /**
     * Deterministic (seeded LCG) mix of small, large and fractional values so the encoder
     * cannot collapse the array into a single repeated value.
     */
    static double[] generateDoubles(int count) {
        double[] values = new double[count];
        long seed = 0x5DEECE66DL;
        for (int i = 0; i < count; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            // spread values over several orders of magnitude
            values[i] = (seed >>> 11) / (double) (1L << 53) * Math.pow(10, i % 12 - 6);
        }
        return values;
    }
}
