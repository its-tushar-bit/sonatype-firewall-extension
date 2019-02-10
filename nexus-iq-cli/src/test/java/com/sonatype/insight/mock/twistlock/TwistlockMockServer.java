/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock.twistlock;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.test.SslProperties;

import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;

public class TwistlockMockServer
{
  static {
    SslProperties.use();
  }

  private int httpPort = 0;

  private String username;

  private String password;

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

  public TwistlockMockServer setAuthentication(String username, String password) {
    this.username = username;
    this.password = password;
    return this;
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

    Handler mainHandler = new RestHandler();

    if (username != null && username.length() > 0) {
      UserStore userStore = new UserStore();
      userStore.addUser(username, new Password(password), new String[]{"uploader"});
      HashLoginService loginService = new HashLoginService("TestRealm");
      loginService.setUserStore(userStore);
      server.addBean(loginService);

      Constraint constraint = new Constraint("auth", "uploader");
      constraint.setAuthenticate(true);

      ConstraintMapping constraintMapping = new ConstraintMapping();
      constraintMapping.setPathSpec("/*");
      constraintMapping.setConstraint(constraint);

      ConstraintSecurityHandler secHandler = new ConstraintSecurityHandler();
      secHandler.setAuthenticator(new BasicAuthenticator());
      secHandler.setLoginService(loginService);
      secHandler.setConstraintMappings(new ConstraintMapping[] { constraintMapping });
      secHandler.setHandler(mainHandler);

      mainHandler = secHandler;
    }

    HandlerList handlers = new HandlerList();

    handlers.addHandler(new ConnectHandler());
    handlers.addHandler(mainHandler);
    server.setHandler(handlers);
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
    private static final String REPORT_PATH_PREFIX = "/rest/application/analysis/";

    private static final String SCAN_ID = "SCAN-ID";

    private void handleMatchedRequest(HttpServletRequest request) throws IOException {
      IO.copy(request.getInputStream(), IO.getNullStream());
    }

    private void handleScanUpload(Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
      handleMatchedRequest(request);

      response.setContentType(ResponseProvider.CONTENT_TYPE_JSON);

      try (PrintWriter writer = response.getWriter()) {
        writer.println("{");
        writer.println("\"scanId\" : \"" + SCAN_ID + "\", ");
        writer.println("\"timeToReport\" : " + 1);
        writer.println("}");
      }

      baseRequest.setHandled(true);
    }

    private void handleReportDownload(HttpServletRequest request) throws IOException {
      handleMatchedRequest(request);

      String scanId = request.getRequestURI().substring(REPORT_PATH_PREFIX.length());
      if (scanId.length() <= 0) {
        throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "scan id missing");
      }

      throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "bad scan id");
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
    {
      String uri = request.getRequestURI();
      String uriWithParams = uri;

      try {
        ResponseProvider responseProvider = getResponseProvider(uriWithParams);
        if (responseProvider != null) {
          handleMatchedRequest(request);
          responseProvider.render(response);
          baseRequest.setHandled(true);
        }
        else if (uri.equals("/rest/application/analysis") && "PUT".equals(request.getMethod())) {
          handleScanUpload(baseRequest, request, response);
        }
        else if (uri.startsWith(REPORT_PATH_PREFIX) && "GET".equals(request.getMethod())) {
          handleReportDownload(request);
        }
      }
      catch (RequestException e) {
        response.sendError(e.statusCode, e.errorMsg);
        baseRequest.setHandled(true);
      }
    }
  }

  static class RequestException
      extends RuntimeException
  {
    private static final long serialVersionUID = -5203188602692541540L;

    final int statusCode;

    final String errorMsg;

    public RequestException(int statusCode, String errorMsg) {
      this.statusCode = statusCode;
      this.errorMsg = errorMsg;
    }
  }

  class ProxyHandler
      extends AbstractHandler
  {
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
    {
      if ("https".equalsIgnoreCase(request.getScheme())) {
        return;
      }

      String auth = request.getHeader("Proxy-Authorization");
      if (auth != null) {
        auth = auth.substring(auth.indexOf(' ') + 1).trim();
        auth = B64Code.decode(auth, "ISO-8859-1");
      }
    }
  }

  public static void main(String[] args) throws Exception {
    TwistlockMockServer server = new TwistlockMockServer();
    server.setHttpPort(9000);
    server.start();
  }
}
