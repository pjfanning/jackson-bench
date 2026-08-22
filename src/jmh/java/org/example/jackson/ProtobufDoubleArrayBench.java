package org.example.jackson;

import org.example.jackson3.updated.SwarProtobufFactory;
import org.example.jackson3.updated.SwarProtobufParser;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.databind.ObjectReader;
import tools.jackson.dataformat.protobuf.ProtobufMapper;
import tools.jackson.dataformat.protobuf.schema.ProtobufSchema;

/**
 * Measures protobuf parsing throughput when databinding a large array of doubles.
 * <p>
 * Protobuf is schema-driven and its root value must be a message, so unlike the Smile and
 * CBOR benchmarks the {@code double[]} is wrapped in a one-field message
 * ({@link Doubles}) rather than written as a bare root array.
 * <p>
 * Two encodings of that same array are parsed, because they take different decoding paths:
 * <ul>
 *   <li><b>unpacked</b> ({@code repeated double values = 1;}, what
 *       {@code ProtobufMapper.generateSchemaFor()} emits): one tag byte plus an 8-byte
 *       little-endian payload per element</li>
 *   <li><b>packed</b> ({@code [packed=true]}): a single length-prefixed block of 8-byte
 *       payloads, so the per-element tag read disappears</li>
 * </ul>
 * Each encoding is parsed by three parsers:
 * <ul>
 *   <li>the stock {@code tools.jackson.dataformat.protobuf.ProtobufParser} of the released
 *       jar</li>
 *   <li>{@link SwarProtobufParser}, a local copy carrying the VarHandle-based little-endian
 *       reads of <a href="https://github.com/FasterXML/jackson-dataformats-binary/pull/763">
 *       jackson-dataformats-binary#763</a>; a {@code double} is a protobuf {@code fixed64},
 *       so every element goes through {@code _decode64Bits()}</li>
 *   <li>the same local copy forked with {@link SwarProtobufParser#DISABLE_VARHANDLE_PROP}
 *       set, i.e. the byte-shifting fallback, as a control for the copy itself</li>
 * </ul>
 * The control arm is run over the packed encoding only, to keep the arm count down; packed
 * is the one with the least per-element overhead around the 8-byte read, so it is where the
 * change has the best chance of showing.
 */
public class ProtobufDoubleArrayBench extends BenchmarkLauncher {

    private static final int DOUBLE_COUNT = 100_000;

    private static final ProtobufMapper MAPPER = new ProtobufMapper();

    /** Generated schema: {@code repeated double values = 1;} (unpacked). */
    private static final ProtobufSchema UNPACKED_SCHEMA = MAPPER.generateSchemaFor(Doubles.class);

    /** Same message, but with the repeated field marked {@code [packed=true]}. */
    private static final ProtobufSchema PACKED_SCHEMA = parse(
            "message Doubles {\n"
            + "  repeated double values = 1 [packed=true];\n"
            + "}\n");

    private static final ObjectReader UNPACKED_READER =
            MAPPER.readerFor(Doubles.class).with(UNPACKED_SCHEMA);
    private static final ObjectReader PACKED_READER =
            MAPPER.readerFor(Doubles.class).with(PACKED_SCHEMA);

    /** Same mapper, but its factory hands out {@link SwarProtobufParser} for byte[] input. */
    private static final ProtobufMapper SWAR_MAPPER =
            ProtobufMapper.builder(new SwarProtobufFactory()).build();

    private static final ObjectReader SWAR_UNPACKED_READER =
            SWAR_MAPPER.readerFor(Doubles.class).with(UNPACKED_SCHEMA);
    private static final ObjectReader SWAR_PACKED_READER =
            SWAR_MAPPER.readerFor(Doubles.class).with(PACKED_SCHEMA);

    /** The values the encoded documents hold; kept for verification/sanity checks. */
    static final double[] DOUBLES;

    /** Valid unpacked protobuf encoding of {@link #DOUBLES}. */
    static final byte[] UNPACKED_DOC;

    /** Valid packed protobuf encoding of the same values. */
    static final byte[] PACKED_DOC;

    static {
        DOUBLES = generateDoubles(DOUBLE_COUNT);
        Doubles value = new Doubles(DOUBLES);
        UNPACKED_DOC = MAPPER.writer(UNPACKED_SCHEMA).writeValueAsBytes(value);
        PACKED_DOC = MAPPER.writer(PACKED_SCHEMA).writeValueAsBytes(value);
    }

    @Benchmark
    public void benchParseFromByteArray(Blackhole blackhole) {
        blackhole.consume(UNPACKED_READER.readValue(UNPACKED_DOC));
    }

    @Benchmark
    public void benchParseFromByteArraySwar(Blackhole blackhole) {
        blackhole.consume(SWAR_UNPACKED_READER.readValue(UNPACKED_DOC));
    }

    @Benchmark
    public void benchParseFromByteArrayPacked(Blackhole blackhole) {
        blackhole.consume(PACKED_READER.readValue(PACKED_DOC));
    }

    @Benchmark
    public void benchParseFromByteArrayPackedSwar(Blackhole blackhole) {
        blackhole.consume(SWAR_PACKED_READER.readValue(PACKED_DOC));
    }

    /**
     * Control arm: the same local copy, forked with the VarHandle path switched off, so the
     * only difference from {@link #benchParseFromByteArrayPackedSwar} is the read strategy
     * (not the fact that the parser is a local copy).
     */
    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-D" + SwarProtobufParser.DISABLE_VARHANDLE_PROP + "=true")
    public void benchParseFromByteArrayPackedSwarDisabled(Blackhole blackhole) {
        blackhole.consume(SWAR_PACKED_READER.readValue(PACKED_DOC));
    }

    private static ProtobufSchema parse(String protoDefinition) {
        try {
            return MAPPER.schemaLoader().parse(protoDefinition);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to parse protobuf schema", e);
        }
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

    /** Root message; protobuf cannot have a bare array as its root value. */
    public static class Doubles {
        public double[] values;

        public Doubles() { }

        public Doubles(double[] values) {
            this.values = values;
        }
    }
}
