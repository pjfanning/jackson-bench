package org.example.jackson3.updated;

import java.io.OutputStream;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.io.IOContext;
import tools.jackson.dataformat.protobuf.ProtobufFactory;
import tools.jackson.dataformat.protobuf.schema.ProtobufSchema;

/**
 * {@link ProtobufFactory} variant that hands out {@link SwarProtobufParser} for
 * {@code byte[]} input and {@link SwarProtobufGenerator} for {@code OutputStream} output -
 * the VarHandle flavours of {@code ProtobufParser} and {@code ProtobufGenerator}.
 * <p>
 * The protected {@code _createParser()}/{@code _createGenerator()} hooks cannot be used
 * here: they are declared to return {@code ProtobufParser}/{@code ProtobufGenerator}, and
 * the Swar classes are sibling copies rather than subclasses. So the public entry points
 * are overridden instead, mirroring what {@code ProtobufFactory} and {@code BinaryTSFactory}
 * do between them. Other input and output targets fall through to stock behaviour.
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

    @Override
    public JsonGenerator createGenerator(ObjectWriteContext writeCtxt,
            OutputStream out, JsonEncoding enc) throws JacksonException
    {
        // false -> we won't manage the stream unless explicitly directed to
        IOContext ioCtxt = _createContext(_createContentReference(out), false, enc);
        return _decorate(new SwarProtobufGenerator(writeCtxt, ioCtxt,
                writeCtxt.getStreamWriteFeatures(_streamWriteFeatures),
                (ProtobufSchema) writeCtxt.getSchema(),
                _decorate(ioCtxt, out)));
    }
}
