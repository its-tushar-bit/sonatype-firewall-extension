/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock.hds;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.mock.hds.HdsMockServer.ResponseProvider;

class HttpResponseProvider
    implements ResponseProvider
{
  private final HttpResponseProcessor httpResponseProcessor;

  private final int status;

  public HttpResponseProvider(int status, HttpResponseProcessor httpResponseProcessor) {
    this.status = status;
    this.httpResponseProcessor = httpResponseProcessor;
  }

  @Override
  public void render(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setStatus(status);
    httpResponseProcessor.process(request, response);
  }
}
