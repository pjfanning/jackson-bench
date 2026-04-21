package org.example.jackson;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import tools.jackson.core.ErrorReportConfiguration;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.util.BufferRecycler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Compares the gen time of 2 variants of UTF8JsonGenerator.
 * <p>
 * Benchmark variants:
 * <ul>
 *   <li><b>LongAscii</b>  – 1,000 fields each with a 1,100-char pure-ASCII value; this is the
 *       primary workload that exercises the 4-char loop-unrolling fast path in the updated
 *       generator.</li>
 *   <li><b>ShortAscii</b> – 1,000 fields each with a 20-char pure-ASCII value; shows behaviour
 *       when strings are too short to amortise unrolling overhead.</li>
 *   <li><b>Unicode</b>    – 1,000 fields each with a value that mixes ASCII and non-ASCII (Latin
 *       Supplement) characters; exercises the non-fast-path UTF-8 encoding branch.</li>
 * </ul>
 */
public class UTF8GenBench extends BenchmarkLauncher {

    private static final int FIELD_COUNT = 1000;

    // Pre-computed to eliminate String-concatenation GC noise from the hot loop.
    private String[] fieldNames;

    private IOContext ioc;
    private String longAsciiValue;
    private String shortAsciiValue;
    private String unicodeValue;

    @Setup
    public void setup() {
        ioc = testIOContext();
        fieldNames = generateFieldNames(FIELD_COUNT);
        longAsciiValue = generateLongAsciiValue();
        shortAsciiValue = generateShortAsciiValue();
        unicodeValue = generateUnicodeValue();
    }

    // -----------------------------------------------------------------------
    // Long ASCII value (1 100 chars) – primary fast-path benchmark
    // -----------------------------------------------------------------------

    @Benchmark
    public void benchExistingGeneratorLongAscii(Blackhole blackhole) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(2 * 1024 * 1024);
        JsonGenerator generator = new tools.jackson.core.json.UTF8JsonGenerator(
                ObjectWriteContext.empty(), ioc, 0, 0, bytes,
                JsonFactory.DEFAULT_ROOT_VALUE_SEPARATOR, null, null,
                0, '"');
        generator.writeStartObject();
        for (int i = 0; i < FIELD_COUNT; i++) {
            generator.writeName(fieldNames[i]);
            generator.writeString(longAsciiValue);
        }
        generator.writeEndObject();
        generator.flush();
        generator.close();
        blackhole.consume(bytes.toByteArray());
    }

    @Benchmark
    public void benchUpdatedGeneratorLongAscii(Blackhole blackhole) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(2 * 1024 * 1024);
        JsonGenerator generator = new org.example.jackson3.updated.UTF8JsonGenerator(
                ObjectWriteContext.empty(), ioc, 0, 0, bytes,
                JsonFactory.DEFAULT_ROOT_VALUE_SEPARATOR, null, null,
                0, '"');
        generator.writeStartObject();
        for (int i = 0; i < FIELD_COUNT; i++) {
            generator.writeName(fieldNames[i]);
            generator.writeString(longAsciiValue);
        }
        generator.writeEndObject();
        generator.flush();
        generator.close();
        blackhole.consume(bytes.toByteArray());
    }

    // -----------------------------------------------------------------------
    // Short ASCII value (20 chars) – shows behaviour with small strings
    // -----------------------------------------------------------------------

    @Benchmark
    public void benchExistingGeneratorShortAscii(Blackhole blackhole) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(64 * 1024);
        JsonGenerator generator = new tools.jackson.core.json.UTF8JsonGenerator(
                ObjectWriteContext.empty(), ioc, 0, 0, bytes,
                JsonFactory.DEFAULT_ROOT_VALUE_SEPARATOR, null, null,
                0, '"');
        generator.writeStartObject();
        for (int i = 0; i < FIELD_COUNT; i++) {
            generator.writeName(fieldNames[i]);
            generator.writeString(shortAsciiValue);
        }
        generator.writeEndObject();
        generator.flush();
        generator.close();
        blackhole.consume(bytes.toByteArray());
    }

    @Benchmark
    public void benchUpdatedGeneratorShortAscii(Blackhole blackhole) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(64 * 1024);
        JsonGenerator generator = new org.example.jackson3.updated.UTF8JsonGenerator(
                ObjectWriteContext.empty(), ioc, 0, 0, bytes,
                JsonFactory.DEFAULT_ROOT_VALUE_SEPARATOR, null, null,
                0, '"');
        generator.writeStartObject();
        for (int i = 0; i < FIELD_COUNT; i++) {
            generator.writeName(fieldNames[i]);
            generator.writeString(shortAsciiValue);
        }
        generator.writeEndObject();
        generator.flush();
        generator.close();
        blackhole.consume(bytes.toByteArray());
    }

    // -----------------------------------------------------------------------
    // Unicode value – exercises multi-byte UTF-8 encoding (non-fast-path)
    // -----------------------------------------------------------------------

    @Benchmark
    public void benchExistingGeneratorUnicode(Blackhole blackhole) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(2 * 1024 * 1024);
        JsonGenerator generator = new tools.jackson.core.json.UTF8JsonGenerator(
                ObjectWriteContext.empty(), ioc, 0, 0, bytes,
                JsonFactory.DEFAULT_ROOT_VALUE_SEPARATOR, null, null,
                0, '"');
        generator.writeStartObject();
        for (int i = 0; i < FIELD_COUNT; i++) {
            generator.writeName(fieldNames[i]);
            generator.writeString(unicodeValue);
        }
        generator.writeEndObject();
        generator.flush();
        generator.close();
        blackhole.consume(bytes.toByteArray());
    }

    @Benchmark
    public void benchUpdatedGeneratorUnicode(Blackhole blackhole) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(2 * 1024 * 1024);
        JsonGenerator generator = new org.example.jackson3.updated.UTF8JsonGenerator(
                ObjectWriteContext.empty(), ioc, 0, 0, bytes,
                JsonFactory.DEFAULT_ROOT_VALUE_SEPARATOR, null, null,
                0, '"');
        generator.writeStartObject();
        for (int i = 0; i < FIELD_COUNT; i++) {
            generator.writeName(fieldNames[i]);
            generator.writeString(unicodeValue);
        }
        generator.writeEndObject();
        generator.flush();
        generator.close();
        blackhole.consume(bytes.toByteArray());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    static IOContext testIOContext() {
        return testIOContext(StreamReadConstraints.defaults(),
                StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults());
    }

    static IOContext testIOContext(StreamReadConstraints src,
                                  StreamWriteConstraints swc,
                                  ErrorReportConfiguration erc) {
        return new IOContext(src, swc, erc,
                new BufferRecycler(), ContentReference.unknown(), false,
                JsonEncoding.UTF8);
    }

    static String[] generateFieldNames(int count) {
        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = "name" + i;
        }
        return names;
    }

    static String generateLongAsciiValue() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("abcdefghijk");
        }
        return sb.toString(); // 1,100 ASCII chars
    }

    static String generateShortAsciiValue() {
        return "abcdefghijklmnopqrst"; // 20 ASCII chars
    }

    static String generateUnicodeValue() {
        // Mix of ASCII and non-ASCII (Latin Supplement, > 0x7F) characters;
        // the non-ASCII chars bypass the fast path and exercise multi-byte UTF-8 encoding.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("caf\u00e9 r\u00e9sum\u00e9 na\u00efve "); // café résumé naïve
        }
        return sb.toString();
    }
}
