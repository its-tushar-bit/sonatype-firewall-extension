/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock.twistlock;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.IO;

public class TwistlockMockServer
{
  private int httpPort;

  private Server server;

  private Map<RequestMatcher, ResponseProvider> responseProviders = new LinkedHashMap<>();

  public void reset() {
    responseProviders.clear();
  }

  public void setResponseForURI(String uri, Object body, int status) {
    ResponseProvider responseProvider;
    if (body == null) {
      throw new IllegalArgumentException("response body missing for " + uri);
    }
    else if (body instanceof URL) {
      responseProvider = new UrlResponseProvider(status, (URL) body);
    }
    else {
      throw new IllegalStateException("No response provider");
    }
    responseProviders.put(new SimpleRequestMatcher(uri), responseProvider);
  }

  private ResponseProvider getResponseProvider(String uri) {
    for (Map.Entry<RequestMatcher, ResponseProvider> entry : responseProviders.entrySet()) {
      if (entry.getKey().matches(uri)) {
        return entry.getValue();
      }
    }
    return null;
  }

  public TwistlockMockServer setHttpPort(int httpPort) {
    this.httpPort = httpPort;
    return this;
  }

  public int getHttpPort() {
    if (httpPort >= 0 && server != null && server.isRunning()) {
      return ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
    }
    return httpPort;
  }

  public String getHttpUrl() {
    return "http://localhost:" + getHttpPort();
  }

  private Connector newHttpConnector() {
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(httpPort);
    return connector;
  }

  public TwistlockMockServer start() throws Exception {
    if (server != null) {
      return this;
    }

    server = new Server();
    server.addConnector(newHttpConnector());
    server.setHandler(new HandlerList(new RestHandler()));
    server.start();

    return this;
  }

  public void stop() {
    if (server != null) {
      try {
        server.stop();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      server = null;
    }
  }

  class RestHandler
      extends AbstractHandler
  {
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
    {
      String uri = request.getRequestURI();
      String uriWithParams = uri;

      ResponseProvider responseProvider = getResponseProvider(uriWithParams);
      if (responseProvider != null) {
        IO.copy(request.getInputStream(), IO.getNullStream());
        responseProvider.render(response);
        baseRequest.setHandled(true);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    TwistlockMockServer server = new TwistlockMockServer();
    server.setHttpPort(9000);
    server.start();
  }
}
