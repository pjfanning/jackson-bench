package org.example.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.ByteSourceJsonBootstrapper;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;

import java.io.DataInput;
import java.io.IOException;

public class ExtendedJsonFactory extends JsonFactory {
    public JsonParser createParser(DataInput input) throws IOException {
        IOContext ctxt = _createContext(_createContentReference(input), false);
        // Also: while we can't do full bootstrapping (due to read-ahead limitations), should
        // at least handle possible UTF-8 BOM
        int firstByte = ByteSourceJsonBootstrapper.skipUTF8BOM(input);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChildOrPlaceholder(_factoryFeatures);
        return new org.example.jackson.UTF8DataInputJsonParser(
                ctxt, _parserFeatures, input,
                _objectCodec, can, firstByte);
    }

    public JsonParser createParserNew(DataInput input) throws IOException
    {
        IOContext ctxt = _createContext(_createContentReference(input), false);
        // Also: while we can't do full bootstrapping (due to read-ahead limitations), should
        // at least handle possible UTF-8 BOM
        int firstByte = ByteSourceJsonBootstrapper.skipUTF8BOM(input);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChildOrPlaceholder(_factoryFeatures);
        return new org.example.jackson.updated.UTF8DataInputJsonParser(
                ctxt, _parserFeatures, input,
                _objectCodec, can, firstByte);
    }

    public JsonParser createParserLimited(DataInput input) throws IOException
    {
        IOContext ctxt = _createContext(_createContentReference(input), false);
        // Also: while we can't do full bootstrapping (due to read-ahead limitations), should
        // at least handle possible UTF-8 BOM
        int firstByte = ByteSourceJsonBootstrapper.skipUTF8BOM(input);
        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChildOrPlaceholder(_factoryFeatures);
        return new org.example.jackson.updated.UTF8DataInputWithMaxDocLenJsonParser(
                ctxt, _parserFeatures, input,
                _objectCodec, can, firstByte);
    }
}
