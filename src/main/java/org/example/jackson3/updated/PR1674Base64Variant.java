package org.example.jackson3.updated;

import tools.jackson.core.Base64Variant;

/**
 * Local copy of the base64 encoding path proposed in
 * <a href="https://github.com/FasterXML/jackson-core/pull/1674">jackson-core PR #1674</a>,
 * so it can be benchmarked against the shipped {@link Base64Variant#encode(byte[], boolean)}
 * without building a patched jackson-core.
 * <p>
 * The shipped method appends char-at-a-time to a {@link StringBuilder} sized at an estimate
 * of {@code inputEnd + (inputEnd >> 2) + (inputEnd >> 3)} (1.375x input). The PR instead
 * computes the exact output length up front and encodes straight into a {@code char[]},
 * reusing the already-public {@code encodeBase64Chunk(int, char[], int)} and
 * {@code encodeBase64Partial(int, int, char[], int)} helpers.
 * <p>
 * {@link Base64Variant} is {@code final}, so this cannot extend it. It wraps one instead and
 * calls through to those public helpers — which is exactly what the PR's private
 * {@code _encodeToString} does from inside the class, so the encoding work is the same; the
 * only difference from the real patch is the extra field indirection on each helper call.
 * <p>
 * Behaviour is meant to be identical to the shipped method, including the trailing linefeed
 * emitted when the last chunk completes a line; {@code Base64EncodeTest} asserts that
 * against the shipped implementation.
 */
public final class PR1674Base64Variant {

    /**
     * Linefeed used by {@link #encode(byte[], boolean)}: the 2-character JSON
     * (and Java source) escape sequence of backslash + {@code n}.
     */
    private final static String JSON_ESCAPED_LF = "\\n";

    private final Base64Variant _variant;

    public PR1674Base64Variant(Base64Variant variant) {
        _variant = variant;
    }

    public Base64Variant delegate() {
        return _variant;
    }

    public String encode(byte[] input, boolean addQuotes) {
        return _encodeToString(input, addQuotes, JSON_ESCAPED_LF);
    }

    public String encode(byte[] input, boolean addQuotes, String linefeed) {
        return _encodeToString(input, addQuotes, linefeed);
    }

    // 23-Aug-2026, pjfanning: Encodes straight into an exactly-sized char[] rather
    //   than appending char-at-a-time to a StringBuilder: measured 2-4x faster,
    //   the win coming from the bulk array stores (merely fixing the old 1.375x
    //   capacity estimate, which under-shoots for shorter line lengths, recovers
    //   much less).
    private String _encodeToString(byte[] input, boolean addQuotes, String linefeed)
    {
        final int inputEnd = input.length;
        final int lfLen = linefeed.length();
        // Guard degenerate/unvalidated line lengths: loop below emits a linefeed
        // after every chunk once the counter cannot stay positive
        final int chunksPerLine = Math.max(1, _variant.getMaxLineLength() >> 2);

        final char[] buffer = new char[_encodedLength(inputEnd, addQuotes, lfLen, chunksPerLine)];
        int outPtr = 0;
        if (addQuotes) {
            buffer[outPtr++] = '"';
        }

        int chunksBeforeLF = chunksPerLine;

        // Ok, first we loop through all full triplets of data:
        int inputPtr = 0;
        final int safeInputEnd = inputEnd-3; // to get only full triplets

        while (inputPtr <= safeInputEnd) {
            // First, mash 3 bytes into lsb of 32-bit int
            int b24 = (input[inputPtr++]) << 8;
            b24 |= (input[inputPtr++]) & 0xFF;
            b24 = (b24 << 8) | ((input[inputPtr++]) & 0xFF);
            outPtr = _variant.encodeBase64Chunk(b24, buffer, outPtr);
            if (--chunksBeforeLF <= 0) {
                linefeed.getChars(0, lfLen, buffer, outPtr);
                outPtr += lfLen;
                chunksBeforeLF = chunksPerLine;
            }
        }

        // And then we may have 1 or 2 leftover bytes to encode
        final int inputLeft = inputEnd - inputPtr; // 0, 1 or 2
        if (inputLeft > 0) {
            int b24 = (input[inputPtr++]) << 16;
            if (inputLeft == 2) {
                b24 |= ((input[inputPtr]) & 0xFF) << 8;
            }
            outPtr = _variant.encodeBase64Partial(b24, inputLeft, buffer, outPtr);
        }

        if (addQuotes) {
            buffer[outPtr++] = '"';
        }
        return new String(buffer, 0, outPtr);
    }

    // Exact number of characters {@link #_encodeToString} will produce.
    private int _encodedLength(int inputLength, boolean addQuotes,
            int linefeedLength, int chunksPerLine)
    {
        final int fullChunks = inputLength / 3;
        long len = 4L * fullChunks;
        // Note: linefeed is written whenever a chunk completes a line, including
        // the last chunk -- so a trailing linefeed is possible (existing behavior)
        len += (long) linefeedLength * (fullChunks / chunksPerLine);
        final int leftover = inputLength - (fullChunks * 3);
        if (leftover > 0) {
            // 4 chars when padded; otherwise 2 chars for 1 byte, 3 for 2 bytes
            len += _variant.usesPadding() ? 4 : (leftover + 1);
        }
        if (addQuotes) {
            len += 2;
        }
        // Note: caller gets NegativeArraySizeException on overflow, same as the
        // `new StringBuilder(...)` this replaced
        return (int) len;
    }
}
