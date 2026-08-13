/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-jaxrs-testing
package com.sonatype.insight.test.jaxrs.testing;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.netty.handler.codec.http.cookie.Cookie;
import org.asynchttpclient.Response;

public class HttpResponse
{
  private static final ObjectMapper JSON = new ObjectMapper().registerModule(new ParameterNamesModule());

  private final Response response;

  protected HttpResponse(Object response) {
    this.response = (Response) response;
  }

  public int getStatusCode() {
    return response.getStatusCode();
  }

  public String getStatusText() {
    return response.getStatusText();
  }

  public String getUrl() {
    return response.getUri().toString();
  }

  public String getHeader(String name) {
    return response.getHeader(name);
  }

  public String getContentType() {
    return response.getContentType();
  }

  public HttpCookie getCookie(String name) {
    HttpCookie result = null;
    HttpCookie deleteMe = null;
    for (Cookie cookie : response.getCookies()) {
      if (name.equals(cookie.name())) {
        HttpCookie httpCookie = new HttpCookie(cookie.name(), cookie.value());
        httpCookie.setDomain(cookie.domain());
        httpCookie.setPath(cookie.path());
        httpCookie.setSecure(cookie.isSecure());
        // Shiro sets cookie value to "deleteMe" (Cookie.DELETED_COOKIE_VALUE) to signal deletion; prefer the real value
        if ("deleteMe".equals(cookie.value())) {
          deleteMe = httpCookie;
        }
        else {
          result = httpCookie;
        }
      }
    }
    return result != null ? result : deleteMe;
  }

  public <T> T getBody(Class<T> type) {
    try {
      return JSON.readValue(response.getResponseBodyAsBytes(), type);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public String getBodyText() {
    return response.getResponseBody();
  }

  public InputStream getBodyStream() {
    return response.getResponseBodyAsStream();
  }

  public byte[] getBodyBytes() {
    return response.getResponseBodyAsBytes();
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> getBodyList() {
    try {
      return JSON.readValue(response.getResponseBodyAsBytes(), List.class);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public <T> List<T> getBodyList(Class<T> itemType) {
    try {
      return JSON.readValue(response.getResponseBodyAsBytes(),
          JSON.getTypeFactory().constructCollectionType(List.class, itemType));
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public <T> Set<T> getBodySet(Class<T> itemType) {
    try {
      return JSON.readValue(response.getResponseBodyAsBytes(),
          JSON.getTypeFactory().constructCollectionType(Set.class, itemType));
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
