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

import com.ning.http.client.Cookie;
import com.ning.http.client.Response;

public class HttpResponse
{
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

  public String getResponseBody() throws IOException {
    return response.getResponseBody();
  }

  public InputStream getResponseBodyAsStream() throws IOException {
    return response.getResponseBodyAsStream();
  }

  public byte[] getResponseBodyAsBytes() throws IOException {
    return response.getResponseBodyAsBytes();
  }

  public String getResponseBodyExcerpt(int maxLength, String charset) throws IOException {
    return response.getResponseBodyExcerpt(maxLength, charset);
  }
}
