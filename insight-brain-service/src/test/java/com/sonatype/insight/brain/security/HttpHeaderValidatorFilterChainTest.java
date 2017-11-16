/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @since 1.21
 */
public class HttpHeaderValidatorFilterChainTest
    extends AbstractBrainServiceTest
{

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(UserSessionResource.RESOURCE_PATH).auth(User.ADMIN_USERNAME, "admin123");
  }

  @Test
  public void testValidHeader() throws Exception {
    assertResponseStatus(204, restRequest().header("Host", "localhost").post());
  }

  @Test
  public void testInvalidHeader_Proto() throws Exception {
    HttpResponse response = restRequest().header("X-Forwarded-Proto", "http\"><script>alert(document.domain)</script>")
        .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), is("Illegal header value detected in 'X-Forwarded-Proto'"));
  }

  @Test
  public void testInvalidHeader_Host() throws Exception {
    HttpResponse response = restRequest().header("X-Forwarded-Host", "\"><script>alert(document.domain)</script>")
        .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), is("Illegal header value detected in 'Host'"));
  }
}
