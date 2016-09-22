/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils.proxy;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.service.PortAllocator;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class ReverseProxyServer
{
  private final int proxyPort = PortAllocator.findFreePort(9999);

  private final String proxyBasePath = System.getProperty("brain.baseUrl", "");

  private final Server jettyServer;

  private final InternalHandler handler;

  public ReverseProxyServer(int brainPort) {
    this.jettyServer = new Server(proxyPort);

    this.handler = new InternalHandler(brainPort, proxyBasePath);
  }

  public String getUrl() {
    return "http://localhost:" + proxyPort + "/" + proxyBasePath;
  }

  public void start() throws Exception {
    jettyServer.setHandler(handler);
    try {
      jettyServer.start();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }

    while (!jettyServer.isStarted()) {
      Thread.sleep(100);
    }
  }

  public void addHandler(IRequestHandler handler) {
    this.handler.add(handler);
  }

  public void reset() {
    handler.reset();
  }

  private static class InternalHandler
      extends AbstractHandler
  {
    private List<IRequestHandler> handlers = new LinkedList<>();

    private final ReverseProxyHandler reverseProxy;

    InternalHandler(int brainPort, String proxyBasePath) {
      reverseProxy = new ReverseProxyHandler(brainPort, proxyBasePath);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
    {
      for (IRequestHandler candidate : handlers) {
        if (handle(candidate, request, response)) {
          return;
        }
      }
      if (handle(reverseProxy, request, response)) {
        return;
      }
      else {
        response.setStatus(HttpStatus.NOT_FOUND_404);
      }
    }

    private boolean handle(IRequestHandler handler, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
    {
      if (handler.matches(request)) {
        handler.handle(request, response);
        return true;
      }
      return false;
    }

    public void reset() {
      handlers = new LinkedList<>();
    }

    public void add(IRequestHandler handler) {
      handlers.add(handler);
    }
  }
}
