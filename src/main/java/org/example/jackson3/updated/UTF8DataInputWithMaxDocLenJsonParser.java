package org.example.jackson3.updated;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.sym.ByteQuadsCanonicalizer;

import java.io.DataInput;
import java.io.IOException;

public class UTF8DataInputWithMaxDocLenJsonParser extends UTF8DataInputJsonParser {

    private long _bytesRead = 0;

    public UTF8DataInputWithMaxDocLenJsonParser(ObjectReadContext readCtxt, IOContext ctxt,
                                                int stdFeatures, int formatFeatures, DataInput inputData,
                                                ByteQuadsCanonicalizer sym,
                                                int firstByte) {
        super(readCtxt, ctxt, stdFeatures, formatFeatures, inputData, sym, firstByte);
    }

    @Override
    protected int readUnsignedByte() throws IOException {
        ++_bytesRead;
        return _inputData.readUnsignedByte();
    }

    @Override
    public JsonToken nextToken() throws JacksonException {
        JsonToken token = super.nextToken();
        _streamReadConstraints.validateDocumentLength(_bytesRead);
        return token;
    }

    @Override
    public String nextName() throws JacksonException {
        String name = super.nextName();
        _streamReadConstraints.validateDocumentLength(_bytesRead);
        return name;
    }
}
