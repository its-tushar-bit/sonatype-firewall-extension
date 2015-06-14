/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.net.MalformedURLException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ning.http.client.Cookie;
import com.ning.http.client.Response;

public class HttpResponse
{
  private static final ObjectMapper JSON = new ObjectMapper();

  private final Response response;

  HttpResponse(Response response) {
    this.response = response;
  }

  public int getStatusCode() {
    return response.getStatusCode();
  }

  public String getStatusText() {
    return response.getStatusText();
  }

  public String getUrl() {
    try {
      return response.getUri().toString();
    }
    catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
  }

  public String getHeader(String name) {
    return response.getHeader(name);
  }

  public String getContentType() {
    return response.getContentType();
  }

  public HttpCookie getCookie(String name) {
    for (Cookie cookie : response.getCookies()) {
      if (name.equals(cookie.getName())) {
        HttpCookie httpCookie = new HttpCookie(cookie.getName(), cookie.getValue());
        httpCookie.setDomain(cookie.getDomain());
        httpCookie.setPath(cookie.getPath());
        httpCookie.setSecure(cookie.isSecure());
        return httpCookie;
      }
    }
    return null;
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
    try {
      return response.getResponseBody();
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public InputStream getBodyStream() {
    try {
      return response.getResponseBodyAsStream();
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public byte[] getBodyBytes() {
    try {
      return response.getResponseBodyAsBytes();
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
