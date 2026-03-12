package org.example.jackson.updated;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;

import java.io.DataInput;
import java.io.IOException;

public class UTF8DataInputWithMaxDocLenJsonParser extends UTF8DataInputJsonParser {

    private long _bytesRead = 0;

    public UTF8DataInputWithMaxDocLenJsonParser(IOContext ctxt, int features, DataInput inputData,
                                                ObjectCodec codec, ByteQuadsCanonicalizer sym,
                                                int firstByte) {
        super(ctxt, features, inputData, codec, sym, firstByte);
    }

    @Override
    protected int readUnsignedByte() throws IOException {
        ++_bytesRead;
        return _inputData.readUnsignedByte();
    }

    @Override
    public JsonToken nextToken() throws IOException {
        JsonToken token = super.nextToken();
        _streamReadConstraints.validateDocumentLength(_bytesRead);
        return token;
    }

    @Override
    public String nextFieldName() throws IOException {
        String name = super.nextFieldName();
        _streamReadConstraints.validateDocumentLength(_bytesRead);
        return name;
    }
}
