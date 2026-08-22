package org.example.jackson;

import org.example.jackson3.updated.SwarSmileFactory;
import org.example.jackson3.updated.SwarSmileParser;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.smile.SmileFactory;
import tools.jackson.dataformat.smile.SmileMapper;
import tools.jackson.dataformat.smile.SmileWriteFeature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Measures Smile parsing throughput on a property-name-heavy document.
 * <p>
 * <a href="https://github.com/FasterXML/jackson-dataformats-binary/pull/757">PR #757</a>
 * only replaces the byte-shifting in the <i>property name</i> quad decoding
 * ({@code _nextNameOptimized}, {@code _nextNameFromSymbolsLong}, {@code _findDecodedFromSymbols},
 * {@code _findDecodedFixed12}); Smile number decoding is untouched, and in any case Smile
 * splits numbers across 7-bit bytes, so there is nothing there for a wide load to read.
 * That is why the SWAR arms live here rather than in {@link SmileDoubleArrayBench} — an
 * array of doubles carries no property names and would exercise none of the changed code.
 * <p>
 * The document is written with {@link SmileWriteFeature#CHECK_SHARED_NAMES} disabled, so
 * names are NOT written as back-references: every one of the {@value #OBJECT_COUNT} x
 * {@value #NAMES_PER_OBJECT} occurrences is decoded through the quad path. Name lengths
 * straddle the branches of {@code _findDecodedFromSymbols}: 5-8 bytes (one quad), 9-12
 * (two quads) and 13+ (the {@code _quadBuffer} loop).
 * <p>
 * Note this covers the {@code _findDecodedFromSymbols} path used by {@code Map} binding;
 * the {@code PropertyNameMatcher} path ({@code _nextNameOptimized}) is what POJO binding
 * uses and is not exercised here.
 */
public class SmileNameDecodeBench extends BenchmarkLauncher {

    private static final int OBJECT_COUNT = 500;
    private static final int NAMES_PER_OBJECT = 21;

    private static final SmileMapper MAPPER = new SmileMapper();

    /** Same mapper, but its factory hands out {@link SwarSmileParser} for byte[] input. */
    private static final SmileMapper SWAR_MAPPER = SmileMapper.builder(new SwarSmileFactory()).build();

    private static final TypeReference<List<Map<String, Integer>>> TYPE =
            new TypeReference<List<Map<String, Integer>>>() { };

    /** Valid Smile encoding of {@value #OBJECT_COUNT} objects, names written out in full. */
    static final byte[] SMILE_DOC;

    static {
        SmileMapper writer = SmileMapper.builder(
                        SmileFactory.builder()
                                .disable(SmileWriteFeature.CHECK_SHARED_NAMES)
                                .build())
                .build();
        SMILE_DOC = writer.writeValueAsBytes(generateObjects());
    }

    @Benchmark
    public void benchParseFromByteArray(Blackhole blackhole) {
        blackhole.consume(MAPPER.readValue(SMILE_DOC, TYPE));
    }

    @Benchmark
    public void benchParseFromByteArraySwar(Blackhole blackhole) {
        blackhole.consume(SWAR_MAPPER.readValue(SMILE_DOC, TYPE));
    }

    /**
     * Control arm: the same local copy, forked with the VarHandle path switched off, so the
     * only difference from {@link #benchParseFromByteArraySwar} is the read strategy.
     */
    @Benchmark
    @Fork(value = 1, jvmArgsAppend = "-D" + SwarSmileParser.DISABLE_VARHANDLE_PROP + "=true")
    public void benchParseFromByteArraySwarDisabled(Blackhole blackhole) {
        blackhole.consume(SWAR_MAPPER.readValue(SMILE_DOC, TYPE));
    }

    /**
     * {@value #OBJECT_COUNT} objects sharing one set of property names, so the parse is
     * dominated by repeated name decoding rather than by symbol-table population.
     */
    static List<Map<String, Integer>> generateObjects() {
        String[] names = generateNames();
        List<Map<String, Integer>> objects = new ArrayList<>(OBJECT_COUNT);
        for (int i = 0; i < OBJECT_COUNT; i++) {
            Map<String, Integer> object = new LinkedHashMap<>();
            for (int j = 0; j < names.length; j++) {
                object.put(names[j], i + j);
            }
            objects.add(object);
        }
        return objects;
    }

    /** ASCII names of 5..25 bytes: one per length, covering all three quad branches. */
    static String[] generateNames() {
        String[] names = new String[NAMES_PER_OBJECT];
        StringBuilder sb = new StringBuilder("abcd");
        for (int i = 0; i < NAMES_PER_OBJECT; i++) {
            sb.append((char) ('a' + (i % 26)));
            names[i] = sb.toString(); // lengths 5, 6, ... 25
        }
        return names;
    }
}
