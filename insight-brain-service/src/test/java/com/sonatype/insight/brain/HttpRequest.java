/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.AntiCsrfFilter;
import com.sonatype.insight.test.jaxrs.testing.AbstractHttpRequest;

/**
 * Builder-style utility to execute HTTP requests.
 */
public class HttpRequest
    extends AbstractHttpRequest<HttpRequest, HttpResponse>
{
  public static HttpRequest to(String url) {
    return new HttpRequest(stripTrailingSlash(url)).auth();
  }

  private HttpRequest(String url) {
    super(url);
  }

  private HttpRequest(HttpRequest parent) {
    super(parent);
  }

  @Override
  protected HttpRequest newSubRequest(HttpRequest parent) {
    return new HttpRequest(parent);
  }

  @Override
  protected HttpResponse newResponse(Object delegate) {
    return new HttpResponse(delegate);
  }

  @Override
  public HttpRequest path(String... paths) {
    if (paths == null || paths.length == 0) {
      return super.path(paths);
    }

    String[] normalizedPaths = new String[paths.length];
    for (int i = 0; i < paths.length; i++) {
      normalizedPaths[i] = stripLeadingSlashes(paths[i]);
    }
    return super.path(normalizedPaths);
  }

  @Override
  protected String getCsrfCookieName() {
    return AntiCsrfFilter.CSRF_COOKIE_NAME;
  }

  @Override
  protected String getCsrfHeaderName() {
    return AntiCsrfFilter.CSRF_HEADER_NAME;
  }

  public HttpRequest auth() {
    return auth(User.ADMIN_USERNAME, "admin123");
  }

  public HttpRequest auth(User user) {
    return auth(user.getUsername(), user.getPassword());
  }

  private static String stripTrailingSlash(String url) {
    if (url == null) {
      return null;
    }

    String normalized = url;
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private String stripLeadingSlashes(String path) {
    if (path == null) {
      return null;
    }

    String normalized = path;
    while (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    return normalized;
  }
}
