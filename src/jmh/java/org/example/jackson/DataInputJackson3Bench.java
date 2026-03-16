package org.example.jackson;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;

/**
 * Compares the parse time of 3 variants of UTF8DataInputJsonParser.
 */
public class DataInputJackson3Bench extends BenchmarkLauncher {

    private final String json = DataInputJackson2Bench.generateJSON(20000);
    private final ExtendedJsonFactory3 JSON_FACTORY = new ExtendedJsonFactory3();

    // this tests the jackson-core 3.1.0 UTF8DataInputJsonParser
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

    // this tests the 3.1.0 UTF8DataInputJsonParser modified to have an overrideable
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
}
