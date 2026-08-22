package org.example.jackson;

import org.example.jackson3.updated.SwarCBORFactory;
import org.example.jackson3.updated.SwarCBORParser;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.dataformat.cbor.CBORMapper;

/**
 * Measures CBOR parsing throughput when databinding a large array of doubles.
 * <p>
 * The CBOR document is generated once in the static initializer from a deterministic
 * {@code double[]} of {@value #DOUBLE_COUNT} values, so every benchmark iteration parses
 * exactly the same valid input.
 * <p>
 * Two parsers are compared over identical input:
 * <ul>
 *   <li>the stock {@code tools.jackson.dataformat.cbor.CBORParser} of the released jar</li>
 *   <li>{@link SwarCBORParser}, a local copy carrying the VarHandle-based multi-byte reads
 *       of <a href="https://github.com/FasterXML/jackson-dataformats-binary/pull/758">
 *       jackson-dataformats-binary#758</a>; a double is a 1-byte header plus an 8-byte
 *       big-endian payload, so every element goes through {@code _decode64Bits()}</li>
 *   <li>the same local copy forked with {@link SwarCBORParser#DISABLE_VARHANDLE_PROP} set,
 *       i.e. the byte-shifting fallback, as a control for the copy itself</li>
 * </ul>
 */
public class CBORDoubleArrayBench extends BenchmarkLauncher {

    private static final int DOUBLE_COUNT = 100_000;

    private static final CBORMapper MAPPER = new CBORMapper();

    /** Same mapper, but its factory hands out {@link SwarCBORParser} for byte[] input. */
    private static final CBORMapper SWAR_MAPPER = CBORMapper.builder(new SwarCBORFactory()).build();

    /** The values the encoded document holds; kept for verification/sanity checks. */
    static final double[] DOUBLES;

    /** Valid CBOR encoding of {@link #DOUBLES} (an array of doubles). */
    static final byte[] CBOR_DOC;

    static {
        DOUBLES = generateDoubles(DOUBLE_COUNT);
        CBOR_DOC = MAPPER.writeValueAsBytes(DOUBLES);
    }

    @Benchmark
    public void benchParseFromByteArray(Blackhole blackhole) {
        blackhole.consume(MAPPER.readValue(CBOR_DOC, double[].class));
    }

    @Benchmark
    public void benchParseFromByteArraySwar(Blackhole blackhole) {
        blackhole.consume(SWAR_MAPPER.readValue(CBOR_DOC, double[].class));
    }

    /**
     * Control arm: the very same local copy, but forked with the VarHandle path switched
     * off, so the only difference from {@link #benchParseFromByteArraySwar} is the read
     * strategy (not the fact that the parser is a local copy).
     */
    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-D" + SwarCBORParser.DISABLE_VARHANDLE_PROP + "=true")
    public void benchParseFromByteArraySwarDisabled(Blackhole blackhole) {
        blackhole.consume(SWAR_MAPPER.readValue(CBOR_DOC, double[].class));
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
