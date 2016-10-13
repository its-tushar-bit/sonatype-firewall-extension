/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils.proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class ResponseCopyHandler
    implements IRequestHandler
{
  private final String url;

  private final ReverseProxyHandler reverseProxy;

  private HttpServletResponseCopier responseCopier;

  public ResponseCopyHandler(int brainPort, String url) {
    this.url = url;
    this.reverseProxy = new ReverseProxyHandler(brainPort, url);
  }

  public byte[] getResponseCopy() {
    return responseCopier.getCopy();
  }

  @Override
  public boolean matches(HttpServletRequest request) {
    return request.getRequestURI().equals(url);
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    this.responseCopier = new HttpServletResponseCopier(response);
    reverseProxy.handle(request, responseCopier);
    responseCopier.flushBuffer();
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
  private OutputStream outputStream;

  private ByteArrayOutputStream copy;

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
}
