package org.example.jackson;

import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.ByteSourceJsonBootstrapper;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.sym.ByteQuadsCanonicalizer;

import java.io.DataInput;

public class ExtendedJsonFactory3 extends JsonFactory {
    private ObjectReadContext readCtxt = ObjectReadContext.empty();

    public JsonParser createParser(DataInput input) {
        IOContext ioCtxt = _createContext(_createContentReference(input), false);
        int firstByte = ByteSourceJsonBootstrapper.skipUTF8BOM(input);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChildOrPlaceholder(_factoryFeatures);
        return new tools.jackson.core.json.UTF8DataInputJsonParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                input, can, firstByte);
    }

    public JsonParser createParserNew(DataInput input) {
        IOContext ioCtxt = _createContext(_createContentReference(input), false);
        int firstByte = ByteSourceJsonBootstrapper.skipUTF8BOM(input);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChildOrPlaceholder(_factoryFeatures);
        return new org.example.jackson3.updated.UTF8DataInputJsonParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                input, can, firstByte);
    }

    public JsonParser createParserLimited(DataInput input) {
        IOContext ioCtxt = _createContext(_createContentReference(input), false);
        int firstByte = ByteSourceJsonBootstrapper.skipUTF8BOM(input);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChildOrPlaceholder(_factoryFeatures);
        return new org.example.jackson3.updated.UTF8DataInputWithMaxDocLenJsonParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                input, can, firstByte);
    }
}
