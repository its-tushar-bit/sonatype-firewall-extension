/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock.hds;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.proxy.ConnectHandler;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class HdsMockServer
{
  private int httpPort = 0;

  private int httpsPort = -1;

  private String keyStoreLocation;

  private String keyStorePassword;

  private String username;

  private String password;

  private String proxyUsername;

  private String proxyPassword;

  private Server server;

  private Deque<HdsMockResponse> responses = new ConcurrentLinkedDeque<>();

  private Map<String, Map<String, String>> capturedRequestHttpHeadersByUri = new HashMap<>();

  private Map<String, String> capturedRequestBodyByUri = new HashMap<>();

  public void reset() {
    responses.clear();
    capturedRequestHttpHeadersByUri.clear();
    capturedRequestBodyByUri.clear();
  }

  public Map<String, String> getCapturedRequestHttpHeaders(String uri) {
    if (!uri.startsWith("/")) {
      uri = "/" + uri;
    }
    return capturedRequestHttpHeadersByUri.get(uri);
  }

  public String getCapturedRequestBody(String uri) {
    if (!uri.startsWith("/")) {
      uri = "/" + uri;
    }
    return capturedRequestBodyByUri.get(uri);
  }

  public HdsMockResponse respondWith(Object body) {
    HdsMockResponse response = new HdsMockResponse(body);
    responses.addFirst(response);
    return response;
  }

  private HdsMockResponse getMockResponse(String method, ParsedUri uri) {
    for (HdsMockResponse response : responses) {
      if (response.matches(method, uri)) {
        return response;
      }
    }
    return null;
  }

  public HdsMockServer setHttpPort(int httpPort) {
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

  public HdsMockServer setHttpsPort(int httpsPort) {
    this.httpsPort = httpsPort;
    return this;
  }

  public int getHttpsPort() {
    if (httpsPort >= 0 && server != null && server.isRunning()) {
      return ((NetworkConnector) server.getConnectors()[(httpPort < 0) ? 0 : 1]).getLocalPort();
    }
    return httpsPort;
  }

  public String getHttpsUrl() {
    return "https://localhost:" + getHttpsPort();
  }

  public HdsMockServer setAuthentication(String username, String password) {
    this.username = username;
    this.password = password;
    return this;
  }

  public HdsMockServer setProxyAuthentication(String username, String password) {
    this.proxyUsername = username;
    this.proxyPassword = password;
    return this;
  }

  public HdsMockServer setKeyStore(String path, String password) {
    keyStoreLocation = path;
    keyStorePassword = password;
    return this;
  }

  private Connector newHttpConnector() {
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(httpPort);
    return connector;
  }

  private Connector newHttpsConnector() {
    SslContextFactory.Server ssl = new SslContextFactory.Server();
    ssl.setKeyStorePath(new File(keyStoreLocation).getAbsolutePath());
    ssl.setKeyStorePassword(keyStorePassword);
    ssl.setKeyManagerPassword(keyStorePassword);
    HttpConfiguration httpsConfiguration = new HttpConfiguration();
    httpsConfiguration.setSecureScheme("https");
    httpsConfiguration.addCustomizer(new SecureRequestCustomizer());
    ServerConnector connector = new ServerConnector(server,
        new SslConnectionFactory(ssl, HttpVersion.HTTP_1_1.asString()), new HttpConnectionFactory(httpsConfiguration));
    connector.setPort(httpsPort);
    return connector;
  }

  public HdsMockServer start() throws Exception {
    if (server != null) {
      return this;
    }

    server = new Server();

    if (httpPort >= 0) {
      server.addConnector(newHttpConnector());
    }
    if (httpsPort >= 0 && keyStoreLocation != null) {
      server.addConnector(newHttpsConnector());
    }

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
      secHandler.setConstraintMappings(new ConstraintMapping[]{constraintMapping});
      secHandler.setHandler(mainHandler);

      mainHandler = secHandler;
    }

    HandlerList handlers = new HandlerList();

    if (proxyUsername != null) {
      handlers.addHandler(new ProxyHandler());
    }

    handlers.addHandler(new ConnectHandler());
    handlers.addHandler(mainHandler);
    server.setHandler(handlers);
    server.setRequestLog(new HdsRequestLog());
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

  private void consume(Request request) throws IOException {
    request.setHandled(true);
    IO.copy(request.getInputStream(), IO.getNullStream());
  }

  private void sendError(HttpServletResponse response, int status, String message) throws IOException {
    response.setStatus(status);
    send(response, "text/plain; charset=UTF-8", message);
  }

  private void sendJson(HttpServletResponse response, String json) throws IOException {
    send(response, "application/json; charset=UTF-8", json);
  }

  private void send(HttpServletResponse response, String contentType, String content) throws IOException {
    response.setContentType(contentType);
    try (PrintWriter writer = response.getWriter()) {
      writer.print(content);
    }
  }

  public class RestHandler
      extends AbstractHandler
  {
    private static final String REPORT_PATH_PREFIX = "/rest/application/analysis/";

    public static final String SCAN_ID = "SCAN-ID";

    private void validateLicense(HttpServletRequest request) throws RequestException {
      String licenseFingerprint = request.getHeader("X-CLM-Token");
      if (licenseFingerprint == null || licenseFingerprint.isEmpty()) {
        throw new RequestException(HttpServletResponse.SC_PAYMENT_REQUIRED, "license fingerprint required");
      }
    }

    private void captureRequestHttpHeaders(HttpServletRequest request) {
      Map<String, String> httpHeaders = new HashMap<>();
      Enumeration<String> httpHeaderNames = request.getHeaderNames();
      while (httpHeaderNames.hasMoreElements()) {
        String httpHeaderName = httpHeaderNames.nextElement();
        httpHeaders.put(httpHeaderName, request.getHeader(httpHeaderName));
      }
      capturedRequestHttpHeadersByUri.put(request.getRequestURI(), httpHeaders);
    }

    private boolean captureRequestHttpBody(Request baseRequest, HttpServletRequest request) {
      boolean isPostRequestWithJsonContent =
          "POST".equalsIgnoreCase(request.getMethod()) && request.getContentType() != null
              && request.getContentType().contains("application/json");
      if (isPostRequestWithJsonContent) {
        try {
          String bodyAsString = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
          capturedRequestBodyByUri.put(request.getRequestURI(), bodyAsString);
          baseRequest.setHandled(true);
          return true;
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      return false;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
    {
      captureRequestHttpHeaders(request);
      boolean bodyWasCaptured = captureRequestHttpBody(baseRequest, request);

      String uri = request.getRequestURI();
      String uriWithParams = uri;
      if (request.getQueryString() != null) {
        // remove timestamp parameters that are generated at runtime
        uriWithParams += '?' + request.getQueryString().replaceAll("&(ts|timestamp)=[0-9]*", "");
      }

      try {
        HdsMockResponse mockResponse = getMockResponse(request.getMethod(), new ParsedUri(uriWithParams));
        if (mockResponse == null && uri.equals("/rest/productLicense/v1") && "POST".equals(request.getMethod())) {
          mockResponse = new HdsMockResponse(getClass().getResource("productLicenseDetails.json")).withoutLicense();
        }
        if (mockResponse != null) {
          mockResponse.render(request, response);
          if (!bodyWasCaptured) {
            consume(baseRequest);
          }
        }
        else if (uri.equals("/rest/license") && "GET".equals(request.getMethod())) {
          consume(baseRequest);
          sendJson(response, "{\"licenses\": [], \"multiLicenses\": []}");
        }
        else if (uri.equals("/rest/productNotifications") && "GET".equals(request.getMethod())) {
          consume(baseRequest);
          sendJson(response, "{}");
        }
        else if (uri.equals("/rest/environment/stats")) {
          if ("GET".equals(request.getMethod())) {
            consume(baseRequest);
            sendJson(response, "{}");
          }
          else if ("POST".equals(request.getMethod())) {
            if (!bodyWasCaptured) {
              consume(baseRequest);
            }
          }
        }
        else if (uri.equals("/user-telemetry.js") && "GET".equals(request.getMethod())) {
          consume(baseRequest);
          send(response, "application/javascript", "function noop() {}");
        }
        else if (uri.equals("/rest/application/analysis") && "PUT".equals(request.getMethod())) {
          if (!bodyWasCaptured) {
            consume(baseRequest);
          }
          validateLicense(request);
          sendJson(response, "{\"scanId\": \"" + SCAN_ID + "\", \"timeToReport\": 0}");
        }
        else if (uri.startsWith(REPORT_PATH_PREFIX) && "GET".equals(request.getMethod())) {
          consume(baseRequest);
          validateLicense(request);
          String scanId = request.getRequestURI().substring(REPORT_PATH_PREFIX.length());
          throw new RequestException(HttpServletResponse.SC_BAD_REQUEST,
              scanId.isEmpty() ? "scan id missing" : "bad scan id: " + scanId);
        }
        else if (uri.equals("/rest/component/details/firewall/ignorePatterns") && "GET".equals(request.getMethod())) {
          consume(baseRequest);
          sendJson(response, "{\"regexpsByRepositoryFormat\":{ \"maven2\":[], \"npm\":[], \"nuget\":[], \"pypi\":[]}}");
        }
        else if (uri.equals("/rest/componentCategories") && "GET".equals(request.getMethod())) {
          consume(baseRequest);
          sendJson(response, "{\"componentCategories\": []}");
        }
        else if (uri.equals("/rest/enterpriseReporting/config") && "GET".equals(request.getMethod())) {
          consume(baseRequest);
          sendJson(response, "{}");
        }
        else if (uri.equals("/rest/productLicense/developer-upper-bound") && "GET".equals(request.getMethod())) {
          consume(baseRequest);
          send(response, "text/plain", "");
        }
        else if (uri.equals("/rest/maliciousUrls/active/maven")) {
          consume(baseRequest);
          sendJson(response, "{\"activeThreatUrls\": [\"https://malicious.com/malicious.jar\"]}");
        }
        else if (uri.equals("/rest/maliciousUrls/active/npm")) {
          consume(baseRequest);
          sendJson(response, "{\"activeThreatUrls\": [\"https://malicious.com/npm.tgz\"]}");
        }
        else if (uri.equals("/rest/maliciousUrls/active/pypi")) {
          consume(baseRequest);
          sendJson(response, "{\"activeThreatUrls\": [\"https://malicious.com/pypi.zip\"]}");
        }
        else if (uri.equals("/rest/maliciousUrls/active/nuget")) {
          consume(baseRequest);
          sendJson(response, "{\"activeThreatUrls\": [\"https://malicious.com/nuget.pkg\"]}");
        }
      }
      catch (RequestException e) {
        if (!bodyWasCaptured) {
          consume(baseRequest);
        }
        sendError(response, e.statusCode, e.errorMsg);
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
        auth = new String(Base64.getDecoder().decode(auth), StandardCharsets.ISO_8859_1);
      }

      if (!(proxyUsername + ':' + proxyPassword).equals(auth)) {
        consume(baseRequest);
        response.setHeader("Proxy-Authenticate", "Basic realm=\"TestRealm\"");
        sendError(response, HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED,
            "Proxy authentication required, got " + auth);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    HdsMockServer server = new HdsMockServer();
    server.setHttpPort(9000);
    server.setProxyAuthentication("proxyuser", "proxypass");
    // server.setKeyStore(System.getProperty("javax.net.ssl.trustStore"), "server-pwd");
    // server.setAuthentication("testuser", "testpass");
    // server.setProxyAuthentication("proxyuser", "proxypass");
    server.start();
  }
}
