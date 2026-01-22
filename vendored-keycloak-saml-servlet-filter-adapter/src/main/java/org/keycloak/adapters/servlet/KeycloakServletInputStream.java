/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.keycloak.adapters.servlet;

import java.io.IOException;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

/**
 * Original Keycloak implementation was tied to do different servlet spec and didn't implement all methods.
 * This class was added to make the implementation more clear and use the supported apis.
 */
public class KeycloakServletInputStream
    extends ServletInputStream
{
  private final byte[] bytes;

  public KeycloakServletInputStream(final byte[] bytes) {
    this.bytes = bytes;
  }

  private int lastIndexRetrieved = -1;

  private ReadListener readListener = null;

  @Override
  public boolean isFinished() {
    return (lastIndexRetrieved == bytes.length - 1);
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void setReadListener(ReadListener readListener) {
    this.readListener = readListener;
    if (!isFinished()) {
      try {
        readListener.onDataAvailable();
      }
      catch (IOException e) {
        readListener.onError(e);
      }
    }
    else {
      try {
        readListener.onAllDataRead();
      }
      catch (IOException e) {
        readListener.onError(e);
      }
    }
  }

  @Override
  public int read() throws IOException {
    int i;
    if (!isFinished()) {
      i = bytes[lastIndexRetrieved + 1];
      lastIndexRetrieved++;
      if (isFinished() && (readListener != null)) {
        try {
          readListener.onAllDataRead();
        }
        catch (IOException ex) {
          readListener.onError(ex);
          throw ex;
        }
      }
      return i;
    }
    else {
      return -1;
    }
  }
}
