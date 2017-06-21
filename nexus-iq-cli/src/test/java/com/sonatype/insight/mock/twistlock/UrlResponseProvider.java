/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock.twistlock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.IO;

public class UrlResponseProvider
    implements ResponseProvider
{
  private final int status;

  private final String contentType;

  private final URL body;

  public UrlResponseProvider(int status, URL body) {
    this.status = status;
    this.contentType = body.getPath().endsWith(".json") ? CONTENT_TYPE_JSON : CONTENT_TYPE_OCTET_STREAM;
    this.body = body;
  }

  public UrlResponseProvider(int status, File body) {
    this(status, toUrl(body));
  }

  private static URL toUrl(File file) {
    try {
      return file.toURI().toURL();
    }
    catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public void render(HttpServletResponse response) throws IOException {
    response.setStatus(status);
    response.setContentType(contentType);
    try (OutputStream os = response.getOutputStream(); InputStream is = body.openStream()) {
      IO.copy(is, os);
    }
  }
}
