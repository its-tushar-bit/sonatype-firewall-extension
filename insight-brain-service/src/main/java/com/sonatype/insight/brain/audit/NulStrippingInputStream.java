/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps an {@link InputStream} to drop 0x00 (NUL) bytes on the way through (CLM-40845).
 *
 * <p>
 * Audit records are JSON. A JSON serializer that encounters a U+0000 code point in a
 * string field escapes it as a six-character ASCII escape sequence (backslash-u-0000);
 * a raw 0x00 byte on the wire is therefore always corruption injected below the
 * serializer, not legitimate data. Dropping raw NUL bytes cannot damage otherwise-valid
 * records.
 *
 * <p>
 * This stream is intended for sequential bulk reads by {@code IOUtils.copy}-style
 * callers. {@link #available()} and {@link #skip(long)} inherit their {@link FilterInputStream}
 * defaults and do not account for stripped bytes; callers that rely on precise pre-read
 * byte counts or skip-past semantics should not use this wrapper.
 */
public final class NulStrippingInputStream
    extends FilterInputStream
{
  /**
   * Upper bound on how many all-NUL bytes {@link #read(byte[], int, int)} will consume
   * from the underlying stream in one call before aborting. Guards against very large
   * contiguous NUL regions silencing the HTTP response long enough to trigger an upstream
   * idle timeout (CLM-38045 pattern). If this cap is hit, an {@link IOException} is
   * thrown so the pathological corruption case surfaces as an error rather than an
   * indefinite stall.
   */
  static final int MAX_CONSUMED_WITHOUT_OUTPUT = 1 << 20; // 1 MiB

  public NulStrippingInputStream(final InputStream in) {
    super(in);
  }

  @Override
  public int read() throws IOException {
    int b;
    while ((b = super.read()) == 0) {
      // NUL — keep reading
    }
    return b;
  }

  @Override
  public int read(final byte[] buf, final int off, final int len) throws IOException {
    if (len == 0) {
      return 0;
    }
    int w = off;
    int consumedAllNul = 0;
    while (w == off) {
      int n = super.read(buf, off, len);
      if (n < 0) {
        return n; // EOF
      }
      if (n == 0) {
        // Spurious zero from the underlying stream. Contract for read(byte[], int, int)
        // with len > 0 requires >=1 byte or EOF, so retry rather than propagate 0 upward
        // (which would busy-spin any IOUtils.copy-style caller).
        continue;
      }

      // Fast path: scan for the first NUL. If the chunk is clean, avoid the compacting
      // self-copy that would otherwise defeat JIT vectorization for the common case.
      int firstNul = -1;
      for (int i = off; i < off + n; i++) {
        if (buf[i] == 0) {
          firstNul = i;
          break;
        }
      }
      if (firstNul < 0) {
        return n;
      }

      // Compact from the first NUL onward, keeping earlier bytes in place.
      w = firstNul;
      for (int i = firstNul + 1; i < off + n; i++) {
        if (buf[i] != 0) {
          buf[w++] = buf[i];
        }
      }

      if (w == off) {
        // Every byte in the chunk was NUL. Cap how much all-NUL we consume before
        // aborting so a large NUL region doesn't stall the HTTP response.
        consumedAllNul += n;
        if (consumedAllNul >= MAX_CONSUMED_WITHOUT_OUTPUT) {
          throw new IOException(
              "Aborting audit log stream: consumed " + consumedAllNul
                  + " consecutive NUL bytes with no output (source file is severely corrupted)");
        }
      }
    }
    return w - off;
  }
}
