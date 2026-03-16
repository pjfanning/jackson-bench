package org.example.jackson;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.ByteSourceJsonBootstrapper;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.sym.ByteQuadsCanonicalizer;

import java.io.DataInput;
import java.io.IOException;

public class ExtendedJsonFactory3 extends JsonFactory {
    public JsonParser createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
                                   DataInput input) {
        int firstByte = ByteSourceJsonBootstrapper.skipUTF8BOM(input);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChildOrPlaceholder(_factoryFeatures);
        return new tools.jackson.core.json.UTF8DataInputJsonParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                input, can, firstByte);
    }

    public JsonParser createParserNew(ObjectReadContext readCtxt, IOContext ioCtxt,
                                      DataInput input) {
        int firstByte = ByteSourceJsonBootstrapper.skipUTF8BOM(input);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChildOrPlaceholder(_factoryFeatures);
        return new org.example.jackson3.updated.UTF8DataInputJsonParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                input, can, firstByte);
    }

    public JsonParser createParserLimited(ObjectReadContext readCtxt, IOContext ioCtxt,
                                          DataInput input) {
        int firstByte = ByteSourceJsonBootstrapper.skipUTF8BOM(input);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChildOrPlaceholder(_factoryFeatures);
        return new org.example.jackson3.updated.UTF8DataInputWithMaxDocLenJsonParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                input, can, firstByte);
    }
}
