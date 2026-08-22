package org.example.jackson;

import org.example.jackson3.updated.SwarProtobufFactory;
import org.example.jackson3.updated.SwarProtobufGenerator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.dataformat.protobuf.ProtobufMapper;
import tools.jackson.dataformat.protobuf.schema.ProtobufSchema;

/**
 * Measures protobuf <i>generation</i> throughput when writing a large array of doubles;
 * the mirror image of {@link ProtobufDoubleArrayBench}.
 * <p>
 * As there, protobuf's root value must be a message, so the {@code double[]} is wrapped in
 * a one-field message and written in two encodings that take different paths through the
 * generator:
 * <ul>
 *   <li><b>unpacked</b> ({@code repeated double values = 1;}): a tag plus an 8-byte payload
 *       per element, i.e. {@code _writeInt64()}</li>
 *   <li><b>packed</b> ({@code [packed=true]}): one length-prefixed block of 8-byte payloads,
 *       i.e. {@code _writeInt64NoTag()}</li>
 * </ul>
 * Each is written by three generators:
 * <ul>
 *   <li>the stock {@code tools.jackson.dataformat.protobuf.ProtobufGenerator} of the
 *       released jar</li>
 *   <li>{@link SwarProtobufGenerator}, a local copy carrying the VarHandle-based
 *       little-endian writes of
 *       <a href="https://github.com/FasterXML/jackson-dataformats-binary/pull/763">
 *       jackson-dataformats-binary#763</a></li>
 *   <li>the same local copy forked with {@link SwarProtobufGenerator#DISABLE_VARHANDLE_PROP}
 *       set, i.e. the byte-shifting fallback, as a control for the copy itself</li>
 * </ul>
 * The control arm is run over the packed encoding only, to keep the arm count down; packed
 * is the one with the least per-element overhead around the 8-byte write, so it is where
 * the change has the best chance of showing.
 */
public class ProtobufDoubleArrayGenBench extends BenchmarkLauncher {

    private static final int DOUBLE_COUNT = 100_000;

    private static final ProtobufMapper MAPPER = new ProtobufMapper();

    /** Same mapper, but its factory hands out {@link SwarProtobufGenerator}. */
    private static final ProtobufMapper SWAR_MAPPER =
            ProtobufMapper.builder(new SwarProtobufFactory()).build();

    /** Generated schema: {@code repeated double values = 1;} (unpacked). */
    private static final ProtobufSchema UNPACKED_SCHEMA =
            MAPPER.generateSchemaFor(ProtobufDoubleArrayBench.Doubles.class);

    /** Same message, but with the repeated field marked {@code [packed=true]}. */
    private static final ProtobufSchema PACKED_SCHEMA = parse(
            "message Doubles {\n"
            + "  repeated double values = 1 [packed=true];\n"
            + "}\n");

    private static final ObjectWriter UNPACKED_WRITER = MAPPER.writer(UNPACKED_SCHEMA);
    private static final ObjectWriter PACKED_WRITER = MAPPER.writer(PACKED_SCHEMA);
    private static final ObjectWriter SWAR_UNPACKED_WRITER = SWAR_MAPPER.writer(UNPACKED_SCHEMA);
    private static final ObjectWriter SWAR_PACKED_WRITER = SWAR_MAPPER.writer(PACKED_SCHEMA);

    /** The value written by every iteration; built once so only encoding is measured. */
    static final ProtobufDoubleArrayBench.Doubles VALUE =
            new ProtobufDoubleArrayBench.Doubles(
                    ProtobufDoubleArrayBench.generateDoubles(DOUBLE_COUNT));

    @Benchmark
    public void benchGenerateToByteArray(Blackhole blackhole) {
        blackhole.consume(UNPACKED_WRITER.writeValueAsBytes(VALUE));
    }

    @Benchmark
    public void benchGenerateToByteArraySwar(Blackhole blackhole) {
        blackhole.consume(SWAR_UNPACKED_WRITER.writeValueAsBytes(VALUE));
    }

    @Benchmark
    public void benchGenerateToByteArrayPacked(Blackhole blackhole) {
        blackhole.consume(PACKED_WRITER.writeValueAsBytes(VALUE));
    }

    @Benchmark
    public void benchGenerateToByteArrayPackedSwar(Blackhole blackhole) {
        blackhole.consume(SWAR_PACKED_WRITER.writeValueAsBytes(VALUE));
    }

    /**
     * Control arm: the same local copy, forked with the VarHandle path switched off, so the
     * only difference from {@link #benchGenerateToByteArrayPackedSwar} is the write strategy
     * (not the fact that the generator is a local copy).
     */
    @Benchmark
    @Fork(value = 1,
          jvmArgsAppend = "-D" + SwarProtobufGenerator.DISABLE_VARHANDLE_PROP + "=true")
    public void benchGenerateToByteArrayPackedSwarDisabled(Blackhole blackhole) {
        blackhole.consume(SWAR_PACKED_WRITER.writeValueAsBytes(VALUE));
    }

    private static ProtobufSchema parse(String protoDefinition) {
        try {
            return MAPPER.schemaLoader().parse(protoDefinition);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to parse protobuf schema", e);
        }
    }
}
