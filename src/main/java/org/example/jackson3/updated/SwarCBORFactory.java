package org.example.jackson3.updated;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.io.IOContext;
import tools.jackson.dataformat.cbor.CBORFactory;

/**
 * {@link CBORFactory} variant that hands out {@link SwarCBORParser} instances (the
 * VarHandle/SWAR flavour of {@code CBORParser}) for {@code byte[]} input.
 * <p>
 * Only the {@code byte[]} entry point is redirected; that is the one databind uses for
 * {@code readValue(byte[], ...)}, which is what the benchmarks exercise. Other input
 * sources fall through to the stock factory behaviour.
 */
public class SwarCBORFactory extends CBORFactory {
    private static final long serialVersionUID = 1L;

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt,
            byte[] data, int offset, int len) throws JacksonException
    {
        _streamReadConstraints.validateDocumentLength(len);
        IOContext ioCtxt = _createContext(_createContentReference(data, offset, len), true);
        return new SwarCBORParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                _byteSymbolCanonicalizer.makeChildOrPlaceholder(_factoryFeatures),
                null, data, offset, offset + len, false);
    }
}
