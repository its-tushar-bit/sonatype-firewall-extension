/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import com.sonatype.insight.brain.spring.config.SecurityConfiguration;
import java.net.HttpCookie;

public class HttpResponse
    extends com.sonatype.insight.test.jaxrs.testing.HttpResponse
{
  HttpResponse(Object delegate) {
    super(delegate);
  }

  public HttpCookie getSessionCookie() {
    return getCookie(SecurityConfiguration.SESSION_COOKIE_NAME);
  }
}
