/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils.proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.sonatype.insight.test.reverseproxy.IRequestHandler;
import com.sonatype.insight.test.reverseproxy.ReverseProxyHandler;

import com.codeborne.selenide.Configuration;

public class ResponseCopyHandler
    implements IRequestHandler
{
  private final String url;

  private final ReverseProxyHandler reverseProxy;

  private volatile HttpServletResponseCopier responseCopier;

  public ResponseCopyHandler(String url, int brainPort) {
    this.url = url;
    this.reverseProxy = new ReverseProxyHandler(brainPort, System.getProperty("proxy.basePath", ""));
  }

  public byte[] consumeResponse() {
    for (long start = System.currentTimeMillis(); System.currentTimeMillis() - start <= Configuration.timeout;) {
      if (responseCopier != null) {
        byte[] response = responseCopier.getCopy();
        responseCopier = null;
        return response;
      }
    }
    throw new IllegalStateException("Timeout waiting for response after " + Configuration.timeout + "ms");
  }

  @Override
  public boolean matches(HttpServletRequest request) {
    return request.getRequestURI().endsWith(url);
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpServletResponseCopier copier = new HttpServletResponseCopier(response);
    reverseProxy.handle(request, copier);
    copier.flushBuffer();
    responseCopier = copier;
  }
}

class HttpServletResponseCopier
    extends HttpServletResponseWrapper
{
  private ServletOutputStream outputStream;

  private ServletOutputStreamCopier copier;

  HttpServletResponseCopier(HttpServletResponse response) {
    super(response);
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    if (outputStream == null) {
      outputStream = getResponse().getOutputStream();
      copier = new ServletOutputStreamCopier(outputStream);
    }
    return copier;
  }

  @Override
  public void flushBuffer() throws IOException {
    if (outputStream != null) {
      copier.flush();
    }
  }

  byte[] getCopy() {
    if (copier != null) {
      return copier.getCopy();
    }
    else {
      return new byte[0];
    }
  }
}

class ServletOutputStreamCopier
    extends ServletOutputStream
{
  private final OutputStream outputStream;

  private final ByteArrayOutputStream copy;

  ServletOutputStreamCopier(OutputStream outputStream) {
    this.outputStream = outputStream;
    this.copy = new ByteArrayOutputStream(1024);
  }

  @Override
  public void write(int b) throws IOException {
    outputStream.write(b);
    copy.write(b);
  }

  byte[] getCopy() {
    return copy.toByteArray();
  }

  @Override
  public void flush() throws IOException {
    outputStream.flush();
    copy.flush();
  }

  @Override
  public boolean isReady() {
    return false;
  }

  @Override
  public void setWriteListener(final WriteListener writeListener) {
    // No implementation necessary
  }
}
