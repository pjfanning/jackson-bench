package org.example.jackson;

import org.example.jackson3.updated.SwarAvroFactory;
import org.example.jackson3.updated.SwarAvroParser;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.databind.ObjectReader;
import tools.jackson.dataformat.avro.AvroMapper;
import tools.jackson.dataformat.avro.AvroSchema;

/**
 * Measures Avro parsing throughput when databinding a large array of doubles.
 * <p>
 * The Avro document is generated once in the static initializer from the same deterministic
 * {@code double[]} of {@value #DOUBLE_COUNT} values used by the Smile and CBOR benchmarks,
 * so every benchmark iteration parses exactly the same valid input.
 * <p>
 * Avro is schema-driven; an array is a legal root type, so the document binds straight to
 * {@code double[]} with the schema {@code {"type":"array","items":"double"}}. On the wire
 * that is a block count followed by the doubles as fixed 8-byte little-endian payloads,
 * with no per-element tag and no property names.
 * <p>
 * Three parsers are compared over identical input:
 * <ul>
 *   <li>the stock {@code tools.jackson.dataformat.avro.deser.JacksonAvroParserImpl} of the
 *       released jar</li>
 *   <li>{@link SwarAvroParser}, a local subclass carrying the VarHandle-based little-endian
 *       reads of <a href="https://github.com/FasterXML/jackson-dataformats-binary/pull/762">
 *       jackson-dataformats-binary#762</a>; every element of the array goes through the
 *       overridden {@code decodeDouble()}</li>
 *   <li>the same local subclass forked with {@link SwarAvroParser#DISABLE_VARHANDLE_PROP}
 *       set, i.e. the byte-shifting fallback, as a control</li>
 * </ul>
 * Because {@link SwarAvroParser} only overrides {@code decodeFloat()}/{@code decodeDouble()}
 * and inherits everything else from the released class, the SWAR arm differs from the stock
 * arm in exactly those two method bodies.
 */
public class AvroDoubleArrayBench extends BenchmarkLauncher {

    private static final int DOUBLE_COUNT = 100_000;

    private static final AvroMapper MAPPER = new AvroMapper();

    private static final AvroSchema SCHEMA =
            MAPPER.schemaFrom("{\"type\":\"array\",\"items\":\"double\"}");

    private static final ObjectReader READER = MAPPER.readerFor(double[].class).with(SCHEMA);

    /** Same mapper, but its factory hands out {@link SwarAvroParser} for byte[] input. */
    private static final AvroMapper SWAR_MAPPER = AvroMapper.builder(new SwarAvroFactory()).build();

    private static final ObjectReader SWAR_READER =
            SWAR_MAPPER.readerFor(double[].class).with(SCHEMA);

    /** The values the encoded document holds; kept for verification/sanity checks. */
    static final double[] DOUBLES;

    /** Valid Avro encoding of {@link #DOUBLES} (an array of doubles). */
    static final byte[] AVRO_DOC;

    static {
        DOUBLES = generateDoubles(DOUBLE_COUNT);
        AVRO_DOC = MAPPER.writer(SCHEMA).writeValueAsBytes(DOUBLES);
    }

    @Benchmark
    public void benchParseFromByteArray(Blackhole blackhole) {
        blackhole.consume(READER.readValue(AVRO_DOC));
    }

    @Benchmark
    public void benchParseFromByteArraySwar(Blackhole blackhole) {
        blackhole.consume(SWAR_READER.readValue(AVRO_DOC));
    }

    /**
     * Control arm: the same local subclass, forked with the VarHandle path switched off, so
     * the only difference from {@link #benchParseFromByteArraySwar} is the read strategy.
     */
    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-D" + SwarAvroParser.DISABLE_VARHANDLE_PROP + "=true")
    public void benchParseFromByteArraySwarDisabled(Blackhole blackhole) {
        blackhole.consume(SWAR_READER.readValue(AVRO_DOC));
    }

    /**
     * Deterministic (seeded LCG) mix of small, large and fractional values so the encoder
     * cannot collapse the array into a single repeated value. Same generator as the Smile
     * and CBOR benchmarks, so the three formats encode identical values.
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
