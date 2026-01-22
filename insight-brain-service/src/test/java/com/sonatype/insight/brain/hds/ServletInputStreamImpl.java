/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

class ServletInputStreamImpl
    extends ServletInputStream
{
  // ByteArrayInputStream.close is a noop, so we don't need to close this stream
  private final ByteArrayInputStream wrappedInputStream;

  public ServletInputStreamImpl(String data) {
    this(data.getBytes(StandardCharsets.UTF_8));
  }

  public ServletInputStreamImpl(byte[] data) {
    wrappedInputStream = new ByteArrayInputStream(data);
  }

  @Override
  public int read() {
    return wrappedInputStream.read();
  }

  @Override
  public boolean isFinished() {
    return false;
  }

  @Override
  public boolean isReady() {
    return false;
  }

  @Override
  public void setReadListener(final ReadListener readListener) {
    // No implementation necessary
  }
}
