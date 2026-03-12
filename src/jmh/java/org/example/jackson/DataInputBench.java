package org.example.jackson;

import com.fasterxml.jackson.core.JsonParser;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;

public class DataInputBench extends BenchmarkLauncher {

    private final String json = generateJSON(2000);
    private final ExtendedJsonFactory JSON_FACTORY = new ExtendedJsonFactory();

    @Benchmark
    public void benchDataInput(Blackhole blackhole) throws IOException {
        try (JsonParser jp = JSON_FACTORY.createParser(new MockDataInput(json))) {
            while(jp.hasCurrentToken()) {
                blackhole.consume(jp.nextToken());
            }
        }
    }

    @Benchmark
    public void benchDataInputNew(Blackhole blackhole) throws IOException {
        try (JsonParser jp = JSON_FACTORY.createParserNew(new MockDataInput(json))) {
            while(jp.hasCurrentToken()) {
                blackhole.consume(jp.nextToken());
            }
        }
    }

    @Benchmark
    public void benchDataInputLimited(Blackhole blackhole) throws IOException {
        try (JsonParser jp = JSON_FACTORY.createParserLimited(new MockDataInput(json))) {
            while(jp.hasCurrentToken()) {
                blackhole.consume(jp.nextToken());
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
