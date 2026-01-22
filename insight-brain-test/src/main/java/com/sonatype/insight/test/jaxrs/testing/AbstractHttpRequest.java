/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-jaxrs-testing
package com.sonatype.insight.test.jaxrs.testing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpCookie;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Consumer;
import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.request.body.multipart.ByteArrayPart;
import org.asynchttpclient.request.body.multipart.Part;
import org.asynchttpclient.request.body.multipart.StringPart;

/**
 * Base class to support extending the HTTP request building.
 *
 * @param <T> The type of the request subclass.
 * @param <R> The type of the response subclass.
 */
public abstract class AbstractHttpRequest<T extends AbstractHttpRequest<T, R>, R extends HttpResponse>
{
  private static class Auth
  {
    final String username;

    final String password;

    public Auth(String username, String password) {
      this.username = username;
      this.password = password;
    }
  }

  private static class CsrfToken
  {
    final String cookieValue;

    final String headerValue;

    final String formValue;

    public CsrfToken(String cookieValue, String headerValue, String formValue) {
      this.cookieValue = cookieValue;
      this.headerValue = headerValue;
      this.formValue = formValue;
    }
  }

  private static final ObjectMapper JSON = new ObjectMapper();

  private static final DefaultAsyncHttpClient CLIENT = new DefaultAsyncHttpClient(
          new DefaultAsyncHttpClientConfig.Builder()
                  .setCookieStore(null)
                  .setUseInsecureTrustManager(true)
                  .build());

  private Url url;

  private boolean redirects;

  private Auth auth;

  private CsrfToken csrfToken;

  private Map<String, String> headers;

  private Map<String, HttpCookie> cookies;

  private List<Part> parts;

  private byte[] body;

  private String contentType;

  protected AbstractHttpRequest(String url) {
    this.url = new Url(url);
    auth = new Auth(null, null);
    csrfToken = new CsrfToken("nonce", "nonce", null);
    headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    cookies = new TreeMap<>();
  }

  protected AbstractHttpRequest(AbstractHttpRequest<T, R> parent) {
    this.url = new Url(parent.getUrl());
    redirects = parent.redirects;
    auth = parent.auth;
    csrfToken = parent.csrfToken;
    headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    headers.putAll(parent.headers);
    cookies = new TreeMap<>(parent.cookies);
    // sub request usually processes different body so don't clone that
  }

  protected abstract T newSubRequest(T parent);

  protected abstract R newResponse(Object delegate);

  protected String getCsrfHeaderName() {
    return "X-CSRF-TOKEN";
  }

  protected String getCsrfCookieName() {
    return "CSRF-TOKEN";
  }

  @SuppressWarnings("unchecked")
  private T getThis() {
    return (T) this;
  }

  public T subpath(String... paths) {
    return newSubRequest(getThis()).path(paths);
  }

  public T path(String... paths) {
    url.path(paths);
    return getThis();
  }

  public T query(String name, Object... values) {
    if (values == null) {
      values = new Object[0];
    }
    List<Object> vals = new ArrayList<>(Arrays.asList(values));
    for (int i = vals.size() - 1; i >= 0; i--) {
      Object val = vals.get(i);
      if (val == null) {
        vals.remove(i);
      }
      else if (!val.getClass().getName().startsWith("java.lang.") && !(val instanceof Enum)) {
        try {
          vals.set(i, JSON.writeValueAsString(val));
        }
        catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    }
    url.query(name, vals.toArray(new Object[vals.size()]));
    return getThis();
  }

  public T query(String query) {
    url.query(query);
    return getThis();
  }

  public T parameter(Object... parameters) {
    url.parameter(parameters);
    return getThis();
  }

  public String getUrl() {
    return url.build();
  }

  public T followRedirects() {
    redirects = true;
    return getThis();
  }

  public T ignoreRedirects() {
    redirects = false;
    return getThis();
  }

  public T csrfToken(String cookieValue, String headerValue) {
    return csrfToken(cookieValue, headerValue, null);
  }

  public T csrfToken(String cookieValue, String headerValue, String formValue) {
    csrfToken = new CsrfToken(cookieValue, headerValue, formValue);
    return getThis();
  }

  public T noCsrfToken() {
    csrfToken = new CsrfToken(null, null, null);
    return getThis();
  }

  public T anon() {
    return auth(null, null);
  }

  public T auth(String username, String password) {
    auth = new Auth(username, password);
    return getThis();
  }

  public T header(String name, String value) {
    if (value != null) {
      headers.put(name, value);
    }
    else {
      headers.remove(name);
    }
    return getThis();
  }

  public T headers(Map<String, String> headers) {
    if (headers != null) {
      for (Entry<String, String> entry : headers.entrySet()) {
        header(entry.getKey(), entry.getValue());
      }
    }
    return getThis();
  }

  public T userAgent(String userAgent) {
    return header(HttpHeaderNames.USER_AGENT.toString(), userAgent);
  }

  public T cookie(String name, String value) {
    if (value != null) {
      cookies.put(name, new HttpCookie(name, value));
    }
    else {
      cookies.remove(name);
    }
    return getThis();
  }

  public T cookie(HttpCookie... cookies) {
    for (HttpCookie cookie : cookies) {
      if (cookie != null) {
        this.cookies.put(cookie.getName(), cookie);
      }
    }
    return getThis();
  }

  public T with(Consumer<T> configurator) {
    if (configurator != null) {
      configurator.accept(getThis());
    }
    return getThis();
  }

  public T body(Object body) {
    return body(body, MediaType.APPLICATION_JSON);
  }

  public T body(Object body, String contentType) {
    this.body = (body != null) ? toBytes(body) : null;
    this.contentType = contentType;
    parts = null;
    return getThis();
  }

  public T part(String name, Object value) {
    // Before sisu was removed we could send json or plain text and Jackson would still correctly convert JSON to a POJO
    // however since the removal it will now only convert json to a POJO if the contentType is application/json.
    // That suggests the version of jackson has changed but actually sending the correct type is something we should do.
    String contentType = isComplexObject(value) ? MediaType.APPLICATION_JSON : "text/plain";
    return part(new StringPart(name, new String(toBytes(value), StandardCharsets.UTF_8), contentType));
  }

  private boolean isComplexObject(Object value) {
    if (value == null) {
      return false;
    }
    Class<?> clazz = value.getClass();
    // Primitives, strings, files, and common simple types should use text/plain
    return !clazz.isPrimitive()
        && !String.class.isAssignableFrom(clazz)
        && !Number.class.isAssignableFrom(clazz)
        && !Boolean.class.isAssignableFrom(clazz)
        && !clazz.isEnum()
        && !java.io.File.class.isAssignableFrom(clazz);
  }

  public T part(String name, String filename, Object part) {
    return part(name, filename, part, MediaType.APPLICATION_OCTET_STREAM);
  }

  public T part(String name, String filename, Object part, String contentType) {
    return part(new ByteArrayPart(name, toBytes(part), contentType, StandardCharsets.UTF_8, filename));
  }

  private T part(Part part) {
    if (parts == null) {
      parts = new ArrayList<>();
    }
    parts.add(part);
    body = null;
    contentType = MediaType.MULTIPART_FORM_DATA;
    return getThis();
  }

  private byte[] toBytes(Object payload) {
    try {
      if (payload instanceof String) {
        return payload.toString().getBytes(StandardCharsets.UTF_8);
      }
      else if (payload instanceof byte[]) {
        return (byte[]) payload;
      }
      else if (payload instanceof File) {
        try (InputStream is = new FileInputStream((File) payload)) {
          return toBytes(is);
        }
      }
      else if (payload instanceof Path) {
        try (InputStream is = Files.newInputStream((Path) payload)) {
          return toBytes(is);
        }
      }
      else if (payload instanceof URL) {
        try (InputStream is = ((URL) payload).openStream()) {
          return toBytes(is);
        }
      }
      else {
        return JSON.writeValueAsBytes(payload);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private byte[] toBytes(InputStream is) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 64);
    byte[] buffer = new byte[1024 * 64];
    for (int read = is.read(buffer); read >= 0; read = is.read(buffer)) {
      baos.write(buffer, 0, read);
    }
    return baos.toByteArray();
  }

  private R execute(BoundRequestBuilder builder, boolean noBody) throws Exception {
    builder.setFollowRedirect(redirects);

    for (HttpCookie cookie : cookies.values()) {
      builder.addCookie(new DefaultCookie(cookie.getName(), cookie.getValue()));
    }

    if (csrfToken.cookieValue != null && !cookies.containsKey(getCsrfCookieName())) {
      builder.addCookie(new DefaultCookie(getCsrfCookieName(), csrfToken.cookieValue));
    }
    if (csrfToken.headerValue != null) {
      builder.setHeader(getCsrfHeaderName(), csrfToken.headerValue);
    }
    if (csrfToken.formValue != null && parts != null && !noBody) {
      builder.addBodyPart(new StringPart(getCsrfHeaderName(), csrfToken.formValue, "text/plain"));
    }

    if (auth.username != null) {
      builder.setHeader(HttpHeaderNames.AUTHORIZATION.toString(),
          "Basic " + Base64.getEncoder()
                  .encodeToString((auth.username + ":" + auth.password).getBytes(StandardCharsets.UTF_8)));
    }

    for (Entry<String, String> header : headers.entrySet()) {
      builder.setHeader(header.getKey(), header.getValue());
    }

    if (noBody) {
      // request doesn't support body
    }
    else if (body != null) {
      builder.setHeader(HttpHeaderNames.CONTENT_TYPE.toString(), contentType);
      builder.setBody(body);
    }
    else if (parts != null) {
      builder.setHeader(HttpHeaderNames.CONTENT_TYPE.toString(), contentType);
      for (Part part : parts) {
        builder.addBodyPart(part);
      }
    }

    return newResponse(builder.execute().get());
  }

  public R head() throws Exception {
    return execute(CLIENT.prepareHead(getUrl()), true);
  }

  public R get() throws Exception {
    return execute(CLIENT.prepareGet(getUrl()), true);
  }

  public R put() throws Exception {
    return execute(CLIENT.preparePut(getUrl()), false);
  }

  public R post() throws Exception {
    return execute(CLIENT.preparePost(getUrl()), false);
  }

  public R patch() throws Exception {
    return execute(CLIENT.prepare("PATCH", getUrl()), false);
  }

  public R delete() throws Exception {
    return execute(CLIENT.prepareDelete(getUrl()), false);
  }

  public R send(String method) throws Exception {
    return execute(CLIENT.prepare(method, getUrl()), "GET".equals(method) || "HEAD".equals(method));
  }
}
