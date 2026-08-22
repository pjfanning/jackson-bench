package org.example.jackson3.updated;

import tools.jackson.core.JacksonException;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.io.IOContext;
import tools.jackson.dataformat.avro.AvroFactory;
import tools.jackson.dataformat.avro.AvroParser;
import tools.jackson.dataformat.avro.AvroSchema;

/**
 * {@link AvroFactory} that hands out {@link SwarAvroParser} for {@code byte[]} input,
 * so a benchmark can compare it against the released parser over identical input.
 * <p>
 * Only the {@code byte[]} entry point is overridden - that is what the benchmarks use;
 * {@code InputStream} and {@code DataInput} keep the stock behaviour.
 */
public class SwarAvroFactory extends AvroFactory
{
    private static final long serialVersionUID = 1L;

    public SwarAvroFactory() {
        super();
    }

    protected SwarAvroFactory(SwarAvroFactory src) {
        super(src);
    }

    @Override
    public SwarAvroFactory copy() {
        return new SwarAvroFactory(this);
    }

    @Override
    public SwarAvroFactory snapshot() {
        return this;
    }

    @Override
    protected AvroParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
            byte[] data, int offset, int len)
        throws JacksonException
    {
        // The Apache-lib decoder has no SWAR variant, so fall back to stock for it
        if (_useApacheLibDecoder) {
            return super._createParser(readCtxt, ioCtxt, data, offset, len);
        }
        return new SwarAvroParser(readCtxt, ioCtxt,
                readCtxt.getStreamReadFeatures(_streamReadFeatures),
                readCtxt.getFormatReadFeatures(_formatReadFeatures),
                (AvroSchema) readCtxt.getSchema(),
                data, offset, len);
    }
}
