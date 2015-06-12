/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.security.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Cookie;
import com.ning.http.client.Part;
import com.ning.http.client.Response;
import com.ning.http.multipart.ByteArrayPartSource;
import com.ning.http.multipart.FilePart;
import com.ning.http.multipart.StringPart;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.codehaus.plexus.util.IOUtil;

/**
 * Builder-style utility to execute HTTP requests.
 */
public class HttpRequest
{
  private static class AsyncHttpClientEx
      extends AsyncHttpClient
  {
    BoundRequestBuilder prepare(String method, String url) {
      return requestBuilder(method, url);
    }
  }

  private static final String ADMIN_USERNAME = User.ADMIN_USERNAME;

  private static final String ADMIN_PASSWORD = "admin123";

  private static final ObjectMapper JSON = new ObjectMapper();

  private static final AsyncHttpClientEx CLIENT = new AsyncHttpClientEx();

  private Url url;

  private String username;

  private String password;

  private Map<String, String> headers;

  private Map<String, Cookie> cookies;

  private List<Part> parts;

  private byte[] body;

  private String contentType;

  private HttpRequest(Url url) {
    this.url = url;
    username = ADMIN_USERNAME;
    password = ADMIN_PASSWORD;
    headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    cookies = new TreeMap<>();
  }

  private HttpRequest(HttpRequest parent) {
    this.url = new Url(parent.getUrl());
    username = parent.username;
    password = parent.password;
    headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    headers.putAll(parent.headers);
    cookies = new TreeMap<>(parent.cookies);
    // subpath usually processes different body so don't clone that
  }

  public static HttpRequest to(String url) {
    return new HttpRequest(new Url(url));
  }

  public HttpRequest subpath(String... paths) {
    return new HttpRequest(this).path(paths);
  }

  public HttpRequest path(String... paths) {
    url.path(paths);
    return this;
  }

  public HttpRequest query(String name, Object value) {
    if (value != null && !value.getClass().getName().startsWith("java.lang.")) {
      try {
        value = JSON.writeValueAsString(value);
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    url.query(name, value);
    return this;
  }

  public HttpRequest query(String query) {
    url.query(query);
    return this;
  }

  public HttpRequest parameter(Object... parameters) {
    url.parameter(parameters);
    return this;
  }

  public String getUrl() {
    return url.build();
  }

  public HttpRequest anon() {
    return auth(null, null);
  }

  public HttpRequest auth() {
    return auth(ADMIN_USERNAME, ADMIN_PASSWORD);
  }

  public HttpRequest auth(String username, String password) {
    this.username = username;
    this.password = password;
    return this;
  }

  public HttpRequest header(String name, String value) {
    if (value != null) {
      headers.put(name, value);
    }
    else {
      headers.remove(name);
    }
    return this;
  }

  public HttpRequest cookie(String name, String value) {
    if (value != null) {
      cookies.put(name, new Cookie(null, name, value, null, 0, false, 0));
    }
    else {
      cookies.remove(name);
    }
    return this;
  }

  public HttpRequest cookie(Cookie cookie) {
    if (cookie != null) {
      cookies.put(cookie.getName(), cookie);
    }
    return this;
  }

  public HttpRequest body(Object body) {
    return body(body, MediaType.APPLICATION_JSON);
  }

  public HttpRequest body(Object body, String contentType) {
    this.body = (body != null) ? toBytes(body) : null;
    this.contentType = contentType;
    parts = null;
    return this;
  }

  public HttpRequest part(String name, String value) {
    return part(new StringPart(name, value, "UTF-8"));
  }

  public HttpRequest part(String name, String filename, Object part) {
    return part(new FilePart(name, new ByteArrayPartSource(filename, toBytes(part))));
  }

  private HttpRequest part(Part part) {
    if (parts == null) {
      parts = new ArrayList<>();
    }
    parts.add(part);
    body = null;
    contentType = MediaType.MULTIPART_FORM_DATA;
    return this;
  }

  private byte[] toBytes(Object payload) {
    try {
      if (payload instanceof String) {
        return payload.toString().getBytes(StandardCharsets.UTF_8);
      }
      else if (payload instanceof byte[]) {
        return (byte[]) payload;
      }
      else if (payload instanceof URL) {
        try (InputStream is = ((URL) payload).openStream()) {
          return IOUtil.toByteArray(is);
        }
      }
      else {
        return JSON.writeValueAsBytes(payload);
      }
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private Response execute(BoundRequestBuilder builder, boolean noBody) throws Exception {
    for (Cookie cookie : cookies.values()) {
      builder.addCookie(cookie);
    }

    if (username != null) {
      builder.setHeader(HttpHeaders.AUTHORIZATION,
          "Basic " + new Base64(0).encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
    }

    for (Entry<String, String> header : headers.entrySet()) {
      builder.setHeader(header.getKey(), header.getValue());
    }

    if (noBody) {
      // request doesn't support body
    }
    else if (body != null) {
      builder.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
      builder.setBody(body);
    }
    else if (parts != null) {
      builder.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
      for (Part part : parts) {
        builder.addBodyPart(part);
      }
    }

    return builder.execute().get();
  }

  public Response get() throws Exception {
    return execute(CLIENT.prepareGet(getUrl()), true);
  }

  public Response put() throws Exception {
    return execute(CLIENT.preparePut(getUrl()), false);
  }

  public Response post() throws Exception {
    return execute(CLIENT.preparePost(getUrl()), false);
  }

  public Response delete() throws Exception {
    return execute(CLIENT.prepareDelete(getUrl()), false);
  }

  public Response send(String method) throws Exception {
    return execute(CLIENT.prepare(method, getUrl()), "GET".equals(method) || "HEAD".equals(method));
  }
}
