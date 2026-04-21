package org.example.jackson;

import org.openjdk.jmh.annotations.Benchmark;
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
 */
public class UTF8GenBench extends BenchmarkLauncher {

    final IOContext ioc = testIOContext();
    final String value = generateValue();

    // this tests the jackson-core 3.1.2 UTF8JsonGenerator
    @Benchmark
    public void benchExistingGenerator(Blackhole blackhole) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(16 * 1024);
        JsonGenerator generator = new tools.jackson.core.json.UTF8JsonGenerator(
                ObjectWriteContext.empty(), ioc, 0, 0, bytes,
                JsonFactory.DEFAULT_ROOT_VALUE_SEPARATOR, null, null,
                0, '"');
        generator.writeStartObject();  // Start of JSON object: {

        // Generate 1000 name/value pairs
        for (int i = 0; i < 1000; i++) {
            generator.writeName("name" + i);
            generator.writeString(value);
        }

        generator.writeEndObject();    // End of JSON object: }

        generator.flush();
        generator.close();
        blackhole.consume(bytes.toByteArray());
    }

    // this tests the jackson-core 3.1.2 UTF8JsonGenerator
    @Benchmark
    public void benchUpdatedGenerator(Blackhole blackhole) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(16 * 1024);
        JsonGenerator generator = new org.example.jackson3.updated.UTF8JsonGenerator(
                ObjectWriteContext.empty(), ioc, 0, 0, bytes,
                JsonFactory.DEFAULT_ROOT_VALUE_SEPARATOR, null, null,
                0, '"');
        generator.writeStartObject();  // Start of JSON object: {

        // Generate 1000 name/value pairs
        for (int i = 0; i < 1000; i++) {
            generator.writeName("name" + i);
            generator.writeString(value);
        }

        generator.writeEndObject();    // End of JSON object: }

        generator.flush();
        generator.close();
        blackhole.consume(bytes.toByteArray());
    }

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

    static String generateValue() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("abcdefghijk");
        }
        return sb.toString();
    }
}
