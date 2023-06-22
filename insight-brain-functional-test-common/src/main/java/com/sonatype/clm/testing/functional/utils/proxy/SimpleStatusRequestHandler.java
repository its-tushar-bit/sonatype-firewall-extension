/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils.proxy;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.test.reverseproxy.IRequestHandler;

public class SimpleStatusRequestHandler
    implements IRequestHandler
{
  private final int status;

  private final String message;

  private final String url;

  public SimpleStatusRequestHandler(int status, String message, String url) {
    this.status = status;
    this.message = message;
    this.url = url;
  }

  @Override
  public boolean matches(HttpServletRequest request) {
    return request.getRequestURI().equals(url);
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.sendError(status, message);
  }
}
