/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-jaxrs-testing
package com.sonatype.insight.test.jaxrs.testing;

/**
 * Builder-style utility to execute HTTP requests.
 */
public class HttpRequest
    extends AbstractHttpRequest<HttpRequest, HttpResponse>
{
  public static HttpRequest to(String url) {
    return new HttpRequest(url);
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
}
