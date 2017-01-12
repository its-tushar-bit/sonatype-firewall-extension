/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpMethods;

public class ReverseProxyHandler
    implements IRequestHandler
{
  private final int brainPort;

  private final String proxyBasePath;

  private HttpClient client;

  ReverseProxyHandler(int brainPort, String proxyBasePath) {
    this.brainPort = brainPort;
    this.proxyBasePath = normalizeBasePath(proxyBasePath);
    client = HttpClientBuilder.create().disableCookieManagement().disableRedirectHandling().build();
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

    RequestBuilder builder;
    switch (request.getMethod()) {
      case HttpMethods.GET:
        builder = RequestBuilder.get();
        break;
      case HttpMethods.POST:
        builder = RequestBuilder.post();
        break;
      case HttpMethods.PUT:
        builder = RequestBuilder.put();
        break;
      case HttpMethods.DELETE:
        builder = RequestBuilder.delete();
        break;
      default:
        throw new IOException("Unsupported method");
    }
    builder.setEntity(new InputStreamEntity(request.getInputStream()));
    builder.setUri(translateUrl(request));

    Enumeration<String> iter = request.getHeaderNames();
    while (iter.hasMoreElements()) {
      String headerName = iter.nextElement();
      if (!HttpHeaders.CONTENT_LENGTH.equals(headerName)) {
        builder.setHeader(headerName, request.getHeader(headerName));
      }
    }

    HttpResponse brainResponse = client.execute(builder.build());
    for (Header header : brainResponse.getAllHeaders()) {
      response.setHeader(header.getName(), header.getValue());
    }
    response.setStatus(brainResponse.getStatusLine().getStatusCode());

    if (brainResponse.getEntity() != null && brainResponse.getEntity().isStreaming()) {
      try (InputStream entity = brainResponse.getEntity().getContent();
          OutputStream out = response.getOutputStream();) {
        IOUtils.copy(entity, out);
      }
    }
    else {
      response.getOutputStream().close();
    }
  }

  private String translateUrl(HttpServletRequest request) {
    try {
      URIBuilder builder = new URIBuilder();
      builder.setScheme("http");
      builder.setHost("localhost");
      builder.setPort(brainPort);
      builder.setPath(URLDecoder.decode(request.getRequestURI(), "UTF-8").substring(proxyBasePath.length()));
      for (Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
        for (String value : parameter.getValue()) {
          builder.addParameter(parameter.getKey(), value);
        }
      }
      return builder.build().toString();
    }
    catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public boolean matches(HttpServletRequest request) {
    return URI.create(request.getRequestURI()).getPath().startsWith(proxyBasePath);
  }

  private static String normalizeBasePath(String proxyBasePath) {
    if (!proxyBasePath.startsWith("/")) {
      proxyBasePath = "/" + proxyBasePath;
    }
    if (proxyBasePath.endsWith("/")) {
      proxyBasePath = proxyBasePath.substring(0, proxyBasePath.length() - 1);
    }
    return proxyBasePath;
  }
}
