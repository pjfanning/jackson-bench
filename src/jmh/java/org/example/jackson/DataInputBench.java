package org.example.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;

/**
 * Compares the parse time of 3 variants of UTF8DataInputJsonParser.
 */
public class DataInputBench extends BenchmarkLauncher {

    private final String json = generateJSON(20000);
    private final ExtendedJsonFactory JSON_FACTORY = new ExtendedJsonFactory();

    // this tests the jackson-core 2.21.1 UTF8DataInputJsonParser
    @Benchmark
    public void benchDataInput(Blackhole blackhole) throws IOException {
        try (JsonParser jp = JSON_FACTORY.createParser(new MockDataInput(json))) {
            JsonToken token = jp.nextToken();
            while(token != null) {
                blackhole.consume(token);
                token = jp.nextToken();
            }
        }
    }

    // this tests the 2.21.1 UTF8DataInputJsonParser modified to have an overrideable
    // readUnsignedByte method (aka new UTF8DataInputJsonParser)
    @Benchmark
    public void benchDataInputNew(Blackhole blackhole) throws IOException {
        try (JsonParser jp = JSON_FACTORY.createParserNew(new MockDataInput(json))) {
            JsonToken token = jp.nextToken();
            while(token != null) {
                blackhole.consume(token);
                token = jp.nextToken();
            }
        }
    }

    // this tests a subclass of the new UTF8DataInputJsonParser that tracks the data len
    // and validates it against a limit
    @Benchmark
    public void benchDataInputLimited(Blackhole blackhole) throws IOException {
        try (JsonParser jp = JSON_FACTORY.createParserLimited(new MockDataInput(json))) {
            JsonToken token = jp.nextToken();
            while(token != null) {
                blackhole.consume(token);
                token = jp.nextToken();
            }
        }
    }

    private String generateJSON(final int docLen) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");

        int i = 0;
        while (docLen > sb.length()) {
            sb.append(++i).append(",\n");
        }
        sb.append("true ] ");
        return sb.toString();
    }
}
