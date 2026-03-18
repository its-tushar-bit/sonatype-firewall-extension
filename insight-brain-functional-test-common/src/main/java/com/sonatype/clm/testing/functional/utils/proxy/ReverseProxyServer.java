/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils.proxy;

import java.io.IOException;

import com.sonatype.insight.test.reverseproxy.IRequestHandler;

/**
 * Allows support for HDS ReverseProxyServer by bridging between Javax and Jakarta
 */
public class ReverseProxyServer
    extends com.sonatype.insight.test.reverseproxy.ReverseProxyServer
{
  public ReverseProxyServer(final int proxiedServerPort) {
    super(proxiedServerPort);
  }

  public void addHandler(final RequestHandler handler) {
    super.addHandler(new IRequestHandler()
    {
      @Override
      public boolean matches(final javax.servlet.http.HttpServletRequest httpServletRequest) {
        return handler.matches(new JavaxToJakartaBridge.RequestAdapter(httpServletRequest));
      }

      @Override
      public void handle(
          final javax.servlet.http.HttpServletRequest httpServletRequest,
          final javax.servlet.http.HttpServletResponse httpServletResponse) throws IOException
      {
        handler.handle(new JavaxToJakartaBridge.RequestAdapter(httpServletRequest),
            new JavaxToJakartaBridge.ResponseAdapter(httpServletResponse));
      }
    });
  }
}
