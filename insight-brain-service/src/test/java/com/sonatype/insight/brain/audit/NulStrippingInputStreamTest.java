/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class NulStrippingInputStreamTest
{
  @Test
  public void singleByteRead_skipsNuls() throws IOException {
    byte[] source = new byte[]{0, 0, 'a', 0, 'b', 0, 0, 0, 'c', 0};
    try (NulStrippingInputStream in = new NulStrippingInputStream(new ByteArrayInputStream(source))) {
      assertThat(in.read()).isEqualTo((int) 'a');
      assertThat(in.read()).isEqualTo((int) 'b');
      assertThat(in.read()).isEqualTo((int) 'c');
      assertThat(in.read()).isEqualTo(-1);
    }
  }

  @Test
  public void singleByteRead_allNulsReachesEof() throws IOException {
    byte[] source = new byte[]{0, 0, 0, 0, 0};
    try (NulStrippingInputStream in = new NulStrippingInputStream(new ByteArrayInputStream(source))) {
      assertThat(in.read()).isEqualTo(-1);
    }
  }

  @Test
  public void singleByteRead_emptyStream() throws IOException {
    try (NulStrippingInputStream in = new NulStrippingInputStream(new ByteArrayInputStream(new byte[0]))) {
      assertThat(in.read()).isEqualTo(-1);
    }
  }

  @Test
  public void bulkRead_cleanChunkReturnedUnchanged() throws IOException {
    byte[] source = "hello world\n".getBytes(StandardCharsets.UTF_8);
    byte[] buf = new byte[64];
    try (NulStrippingInputStream in = new NulStrippingInputStream(new ByteArrayInputStream(source))) {
      int n = in.read(buf, 0, buf.length);
      assertThat(n).isEqualTo(source.length);
      assertThat(new String(buf, 0, n, StandardCharsets.UTF_8)).isEqualTo("hello world\n");
    }
  }

  @Test
  public void bulkRead_stripsNulsFromMixedChunk() throws IOException {
    byte[] source = new byte[]{'a', 0, 'b', 0, 0, 'c'};
    byte[] buf = new byte[64];
    try (NulStrippingInputStream in = new NulStrippingInputStream(new ByteArrayInputStream(source))) {
      int n = in.read(buf, 0, buf.length);
      assertThat(new String(buf, 0, n, StandardCharsets.UTF_8)).isEqualTo("abc");
    }
  }

  @Test
  public void bulkRead_zeroLenReturnsZero() throws IOException {
    try (NulStrippingInputStream in = new NulStrippingInputStream(new ByteArrayInputStream(new byte[]{'a'}))) {
      assertThat(in.read(new byte[4], 0, 0)).isEqualTo(0);
    }
  }

  @Test
  public void bulkRead_allNulChunkContinuesUntilRealByte() throws IOException {
    // 5000 NULs followed by "OK\n"
    byte[] source = new byte[5003];
    source[5000] = 'O';
    source[5001] = 'K';
    source[5002] = '\n';
    byte[] buf = new byte[128];
    try (NulStrippingInputStream in = new NulStrippingInputStream(new ByteArrayInputStream(source))) {
      int n = in.read(buf, 0, buf.length);
      assertThat(n).isEqualTo(3);
      assertThat(new String(buf, 0, n, StandardCharsets.UTF_8)).isEqualTo("OK\n");
      assertThat(in.read(buf, 0, buf.length)).isEqualTo(-1);
    }
  }

  @Test
  public void bulkRead_allNulStreamReachesEof() throws IOException {
    byte[] source = new byte[8192];
    byte[] buf = new byte[4096];
    try (NulStrippingInputStream in = new NulStrippingInputStream(new ByteArrayInputStream(source))) {
      assertThat(in.read(buf, 0, buf.length)).isEqualTo(-1);
    }
  }

  @Test
  public void bulkRead_abortsAfterConsumingCapOfConsecutiveNuls() {
    // The cap counts bytes consumed from the underlying stream; give it slightly more
    // than the cap of all-NULs so the abort fires.
    int size = NulStrippingInputStream.MAX_CONSUMED_WITHOUT_OUTPUT + 4096;
    byte[] source = new byte[size];
    byte[] buf = new byte[4096];
    try (NulStrippingInputStream in = new NulStrippingInputStream(new ByteArrayInputStream(source))) {
      assertThatThrownBy(() -> in.read(buf, 0, buf.length))
          .isInstanceOf(IOException.class)
          .hasMessageContaining("consecutive NUL");
    }
    catch (IOException e) {
      // close() may or may not throw depending on JDK version; ignore
    }
  }

  @Test
  public void bulkRead_returnsMinusOneAtEndOfStream() throws IOException {
    try (NulStrippingInputStream in = new NulStrippingInputStream(new ByteArrayInputStream(new byte[]{'x'}))) {
      byte[] buf = new byte[4];
      assertThat(in.read(buf, 0, buf.length)).isEqualTo(1);
      assertThat(in.read(buf, 0, buf.length)).isEqualTo(-1);
    }
  }

  @Test
  public void bulkRead_preservesUtf8MultibyteCharacters() throws IOException {
    // "café" = 63 61 66 c3 a9 in UTF-8; add a NUL between "af" and "é" to check that
    // NUL stripping does not disturb the multibyte sequence bytes on either side.
    byte[] source = new byte[]{'c', 'a', 'f', 0, (byte) 0xc3, (byte) 0xa9};
    byte[] buf = new byte[64];
    try (NulStrippingInputStream in = new NulStrippingInputStream(new ByteArrayInputStream(source))) {
      int n = in.read(buf, 0, buf.length);
      assertThat(new String(buf, 0, n, StandardCharsets.UTF_8)).isEqualTo("café");
    }
  }

  @Test
  public void bulkRead_respectsOffAndLenArguments() throws IOException {
    byte[] source = new byte[]{'a', 0, 'b', 'c', 'd'};
    byte[] buf = new byte[16];
    // Sentinel bytes that must not be overwritten by read
    for (int i = 0; i < buf.length; i++) {
      buf[i] = (byte) '.';
    }
    try (NulStrippingInputStream in = new NulStrippingInputStream(new ByteArrayInputStream(source))) {
      int n = in.read(buf, 4, 3);
      // read is allowed to return anywhere from 1..3 bytes; assert what did land is inside the [4, 4+3) window
      assertThat(n).isBetween(1, 3);
      // sentinels outside the window untouched
      assertThat(buf[0]).isEqualTo((byte) '.');
      assertThat(buf[1]).isEqualTo((byte) '.');
      assertThat(buf[2]).isEqualTo((byte) '.');
      assertThat(buf[3]).isEqualTo((byte) '.');
      assertThat(buf[7]).isEqualTo((byte) '.');
      assertThat(buf[15]).isEqualTo((byte) '.');
    }
  }

  @Test
  public void constructor_wrapsInputStream() throws IOException {
    InputStream underlying = new ByteArrayInputStream(new byte[]{'x', 'y'});
    NulStrippingInputStream wrapper = new NulStrippingInputStream(underlying);
    assertThat(wrapper.read()).isEqualTo((int) 'x');
    assertThat(wrapper.read()).isEqualTo((int) 'y');
    assertThat(wrapper.read()).isEqualTo(-1);
    wrapper.close();
  }
}
