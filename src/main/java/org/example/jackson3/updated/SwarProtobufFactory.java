package org.example.jackson3.updated;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.io.IOContext;
import tools.jackson.dataformat.protobuf.ProtobufFactory;
import tools.jackson.dataformat.protobuf.schema.ProtobufSchema;

/**
 * {@link ProtobufFactory} variant that hands out {@link SwarProtobufParser} instances (the
 * VarHandle flavour of {@code ProtobufParser}) for {@code byte[]} input.
 * <p>
 * The protected {@code _createParser()} hook cannot be used here: it is declared to return
 * {@code ProtobufParser}, and {@link SwarProtobufParser} is a sibling copy rather than a
 * subclass of it. So the public {@code byte[]} entry point is overridden instead, mirroring
 * what {@code ProtobufFactory._createParser()} does. Other input sources fall through to
 * the stock factory behaviour.
 */
public class SwarProtobufFactory extends ProtobufFactory {
    private static final long serialVersionUID = 1L;

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt,
            byte[] data, int offset, int len) throws JacksonException
    {
        _streamReadConstraints.validateDocumentLength(len);
        IOContext ioCtxt = _createContext(_createContentReference(data, offset, len), true);
        return new SwarProtobufParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                (ProtobufSchema) readCtxt.getSchema(),
                null, data, offset, len, false);
    }
}
