package org.example.jackson3.updated;

import java.io.IOException;

import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.io.IOContext;
import tools.jackson.dataformat.avro.AvroSchema;
import tools.jackson.dataformat.avro.deser.JacksonAvroParserImpl;

/**
 * Local copy of the {@code float}/{@code double} decoding changes of
 * <a href="https://github.com/FasterXML/jackson-dataformats-binary/pull/762">
 * jackson-dataformats-binary#762</a>, for benchmarking against the released parser.
 * <p>
 * Unlike the CBOR and Smile equivalents in this package, this one does NOT need to be a
 * full copy of the upstream parser: {@code JacksonAvroParserImpl.decodeFloat()} and
 * {@code decodeDouble()} are {@code public} and non-{@code final}, and every field they
 * touch is {@code protected}, so overriding the two methods is enough. That also makes
 * the comparison exact - everything other than these two method bodies is the released
 * 3.2.2 code, shared with the control mapper.
 * <p>
 * Avro encodes {@code float} and {@code double} as LITTLE-endian IEEE-754, so the wide
 * reads here are little-endian, unlike the big-endian ones CBOR and Smile need.
 */
public class SwarAvroParser extends JacksonAvroParserImpl
{
    /**
     * Benchmark-only escape hatch (not part of PR #762): set to {@code true} to force the
     * byte-shifting fallback, so a fork of this very same class can act as the control
     * arm and the VarHandle path is the single variable under test.
     */
    public final static String DISABLE_VARHANDLE_PROP = "org.example.swar.disable";

    /**
     * Whether VarHandles are usable on this runtime; probed once at class load.
     * Being {@code static final} lets the branches in {@link #decodeFloat()} and
     * {@link #decodeDouble()} fold away at JIT time.
     */
    private final static boolean _VARHANDLE_AVAILABLE = _checkVarHandleAvailable();

    private static boolean _checkVarHandleAvailable() {
        // Benchmark-only hatch; upstream has no equivalent
        if (Boolean.getBoolean(DISABLE_VARHANDLE_PROP)) {
            return false;
        }
        // NOTE: this call is what first loads `AvroVarHandleUtil`, and that class
        // names `VarHandle` in its field/method signatures. On a runtime lacking
        // `java.lang.invoke.VarHandle` (some Android builds) loading it raises
        // `NoClassDefFoundError` -- an Error, not an Exception -- so `Throwable`
        // is what has to be caught here.
        try {
            return AvroVarHandleUtil.isAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    public SwarAvroParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            int parserFeatures, int avroFeatures,
            AvroSchema schema,
            byte[] inputBuffer, int start, int end)
    {
        super(readCtxt, ioCtxt, parserFeatures, avroFeatures, schema,
                inputBuffer, start, end);
    }

    /*
    /**********************************************************************
    /* Overrides: decoding float/double (the PR #762 change)
    /**********************************************************************
     */

    @Override
    public JsonToken decodeFloat() throws IOException {
        int ptr = _inputPtr;
        if ((_inputEnd - ptr) < 4) {
            _loadToHaveAtLeast(4);
            ptr = _inputPtr;
        }
        final byte[] buf = _inputBuffer;
        _inputPtr = ptr+4;
        // Avro encodes float as little-endian IEEE-754
        final int i;
        if (_VARHANDLE_AVAILABLE) {
            i = AvroVarHandleUtil.getIntLE(buf, ptr);
        } else {
            i = (buf[ptr] & 0xff) | ((buf[ptr+1] & 0xff) << 8)
                    | ((buf[ptr+2] & 0xff) << 16) | (buf[ptr+3] << 24);
        }
        _numberFloat = Float.intBitsToFloat(i);
        _numTypesValid = NR_FLOAT;
        return JsonToken.VALUE_NUMBER_FLOAT;
    }

    @Override
    public JsonToken decodeDouble() throws IOException {
        int ptr = _inputPtr;
        if ((_inputEnd - ptr) < 8) {
            _loadToHaveAtLeast(8);
            ptr = _inputPtr;
        }
        final byte[] buf = _inputBuffer;
        // Avro encodes double as little-endian IEEE-754; the two 32-bit halves
        // below combine to exactly a little-endian 8-byte read
        final long l;
        if (_VARHANDLE_AVAILABLE) {
            l = AvroVarHandleUtil.getLongLE(buf, ptr);
            ptr += 4;
        } else {
            int i = (buf[ptr] & 0xff) | ((buf[ptr+1] & 0xff) << 8)
                    | ((buf[ptr+2] & 0xff) << 16) | (buf[ptr+3] << 24);
            ptr += 4;
            int i2 = (buf[ptr] & 0xff) | ((buf[ptr+1] & 0xff) << 8)
                    | ((buf[ptr+2] & 0xff) << 16) | (buf[ptr+3] << 24);
            l = (((long) i) & 0xffffffffL) | (((long) i2) << 32);
        }
        _inputPtr = ptr+4;
        _numberDouble = Double.longBitsToDouble(l);
        _numTypesValid = NR_DOUBLE;
        return JsonToken.VALUE_NUMBER_FLOAT;
    }
}
