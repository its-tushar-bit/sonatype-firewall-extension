/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock.hds;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.mock.hds.InsightMockServer.ResponseProvider;

class BytesResponseProvider
    implements ResponseProvider
{
  private final int status;

  private final String contentType;

  private final byte[] body;

  public BytesResponseProvider(int status, String contentType, byte[] body) {
    this.status = status;
    this.contentType = contentType;
    this.body = body;
  }

  public BytesResponseProvider(int status, byte[] body) {
    this(status, CONTENT_TYPE_OCTET_STREAM, body);
  }

  public BytesResponseProvider(int status, String body) {
    this(status, CONTENT_TYPE_JSON, body.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public void render(HttpServletResponse response) throws IOException {
    response.setStatus(status);
    response.setContentType(contentType);
    try (OutputStream os = response.getOutputStream()) {
      os.write(body);
    }
  }
}
