package org.example.jackson;

import org.example.jackson3.updated.PR1674Base64Variant;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.core.Base64Variant;
import tools.jackson.core.Base64Variants;

import java.util.Base64;
import java.util.Random;

/**
 * Measures {@link Base64Variant#encode(byte[], boolean)} throughput.
 * <p>
 * The method builds its output in a {@link StringBuilder}, appending one character at a time
 * via {@code encodeBase64Chunk(StringBuilder, int)}, and sizes that builder at
 * {@code len + (len >> 2) + (len >> 3)} — 1.375x the input, where base64 needs 1.334x plus
 * padding, quotes and linefeeds, so line-wrapping variants can still grow the builder. Both
 * of those are what an optimisation of this method would target, so the arms below vary the
 * two inputs that drive them: payload size and variant line length.
 * <p>
 * Note this is the 2-arg overload, which hard-codes the linefeed as the two-character JSON
 * escape {@code \n} (backslash + 'n'), not a real newline; the 3-arg overload takes a custom
 * linefeed and is not covered here.
 * <p>
 * Variant arms:
 * <ul>
 *   <li><b>MIME-NO-LINEFEEDS</b> – Jackson's default; max line length is
 *       {@link Integer#MAX_VALUE}, so the {@code chunksBeforeLF} branch never fires. The
 *       straight-line baseline.</li>
 *   <li><b>MIME</b> – wraps at 76 chars, i.e. a linefeed every 19 chunks / 57 input bytes.</li>
 *   <li><b>PEM</b> – wraps at 64 chars (every 16 chunks / 48 input bytes); the most
 *       linefeed-dense of the standard variants.</li>
 *   <li><b>MODIFIED-FOR-URL</b> – no linefeeds and no padding, so the trailing-partial path
 *       emits fewer characters than the padded variants.</li>
 * </ul>
 * Size arms: {@code 3} (one full chunk, no leftover), {@code 47} (below every variant's line
 * length, with a 2-byte leftover), {@code 1024} (typical embedded-binary property) and
 * {@code 65536} (large enough for StringBuilder growth to dominate).
 * <p>
 * {@link #benchEncodePR1674} runs {@link PR1674Base64Variant}, the local copy of
 * <a href="https://github.com/FasterXML/jackson-core/pull/1674">jackson-core PR #1674</a>,
 * which sizes the output exactly and encodes into a {@code char[]}. It is the arm this class
 * exists to measure: the PR's own numbers came from a crude timing loop on a noisy machine
 * and are explicitly provisional. {@code Base64EncodeTest} asserts its output is identical
 * to the shipped method's, so the two arms do the same work.
 * <p>
 * {@link #benchEncodeJdk} is a reference arm only, not an equivalent: the JDK encoder fills a
 * {@code byte[]} and wraps it as a latin-1 String, cannot add quotes, and for the wrapping
 * variants emits a real one-byte newline where Jackson emits the two-character escape — so
 * its output differs. It is here as an outside yardstick for both Jackson arms, not a claim
 * that the three produce the same result.
 */
public class Base64EncodeBench extends BenchmarkLauncher {

    /** Fixed seed: the same bytes every fork, so runs are comparable. */
    private static final long SEED = 20260824L;

    /** Values are {@link Base64Variant#getName()}, as {@link Base64Variants#valueOf} matches on those. */
    //@Param({"MIME-NO-LINEFEEDS", "MIME", "PEM", "MODIFIED-FOR-URL"})
    @Param({"PEM"})
    public String variantName;

    //@Param({"3", "47", "1024", "65536"})
    @Param({"65536"})
    public int inputSize;

    private Base64Variant variant;
    private PR1674Base64Variant pr1674Variant;
    private Base64.Encoder jdkEncoder;
    private byte[] input;

    @Setup
    public void setup() {
        variant = Base64Variants.valueOf(variantName);
        pr1674Variant = new PR1674Base64Variant(variant);
        jdkEncoder = jdkEquivalent(variant);
        input = generateInput(inputSize);
    }

    //@Benchmark
    public void benchEncode(Blackhole blackhole) {
        blackhole.consume(variant.encode(input, false));
    }

    /** Same work plus the surrounding JSON quotes, the way a generator writes a binary value. */
    @Benchmark
    public void benchEncodeWithQuotes(Blackhole blackhole) {
        blackhole.consume(variant.encode(input, true));
    }

    //@Benchmark
    public void benchEncodePR1674(Blackhole blackhole) {
        blackhole.consume(pr1674Variant.encode(input, false));
    }

    @Benchmark
    public void benchEncodePR1674WithQuotes(Blackhole blackhole) {
        blackhole.consume(pr1674Variant.encode(input, true));
    }

    /** Reference arm; see the class javadoc for why this is not an apples-to-apples comparison. */
    //@Benchmark
    public void benchEncodeJdk(Blackhole blackhole) {
        blackhole.consume(jdkEncoder.encodeToString(input));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Closest {@link Base64.Encoder} for the given variant. The line separator is a real
     * {@code \n} rather than Jackson's escaped form because the JDK rejects any separator
     * byte that appears in the base64 alphabet, and {@code 'n'} does.
     */
    static Base64.Encoder jdkEquivalent(Base64Variant variant) {
        if (variant == Base64Variants.MODIFIED_FOR_URL) {
            return Base64.getUrlEncoder().withoutPadding();
        }
        int lineLength = variant.getMaxLineLength();
        if (lineLength <= 0 || lineLength == Integer.MAX_VALUE) {
            return Base64.getEncoder();
        }
        return Base64.getMimeEncoder(lineLength, new byte[] { '\n' });
    }

    /**
     * Pseudo-random bytes: base64 encoding is data-independent, but random input keeps the
     * JIT from folding away anything that a run of identical bytes might allow.
     */
    static byte[] generateInput(int size) {
        byte[] bytes = new byte[size];
        new Random(SEED).nextBytes(bytes);
        return bytes;
    }
}
