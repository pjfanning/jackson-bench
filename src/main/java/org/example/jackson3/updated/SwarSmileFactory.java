package org.example.jackson3.updated;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.IOContext;
import tools.jackson.dataformat.smile.SmileConstants;
import tools.jackson.dataformat.smile.SmileFactory;
import tools.jackson.dataformat.smile.SmileReadFeature;

/**
 * {@link SmileFactory} variant that hands out {@link SwarSmileParser} instances (the
 * VarHandle/SWAR flavour of {@code SmileParser}) for {@code byte[]} input.
 * <p>
 * Only the {@code byte[]} entry point is redirected; that is the one databind uses for
 * {@code readValue(byte[], ...)}, which is what the benchmarks exercise. Other input
 * sources fall through to the stock factory behaviour.
 * <p>
 * The header handling below mirrors {@code SmileParserBootstrapper.constructParser()};
 * that bootstrapper cannot be reused because it is hard-wired to {@code SmileParser}.
 */
public class SwarSmileFactory extends SmileFactory {
    private static final long serialVersionUID = 1L;

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt,
            byte[] data, int offset, int len) throws JacksonException
    {
        _streamReadConstraints.validateDocumentLength(len);
        IOContext ioCtxt = _createContext(_createContentReference(data, offset, len), true);
        final int smileFeatures = readCtxt.getFormatReadFeatures(_formatReadFeatures);
        final int end = offset + len;
        SwarSmileParser p = new SwarSmileParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                smileFeatures,
                _byteSymbolCanonicalizer.makeChildOrPlaceholder(_factoryFeatures),
                null, data, offset, end, false);
        if (offset >= end) { // only the case for empty doc
            return p;
        }
        boolean hadSig = false;
        final byte firstByte = data[offset];
        if (firstByte == SmileConstants.HEADER_BYTE_1) {
            hadSig = p.handleSignature(true, true);
        }
        if (!hadSig && SmileReadFeature.REQUIRE_HEADER.enabledIn(smileFeatures)) {
            throw new StreamReadException(p,
                    "Input does not start with Smile format header (first byte = 0x"
                    + Integer.toHexString(firstByte & 0xFF)
                    + ") and parser has REQUIRE_HEADER enabled: can not parse");
        }
        return p;
    }
}
