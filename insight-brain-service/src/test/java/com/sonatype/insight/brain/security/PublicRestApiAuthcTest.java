/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;

import com.ning.http.client.Cookie;
import com.ning.http.client.Response;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests authentication aspects of the public REST API in general.
 */
public class PublicRestApiAuthcTest
    extends AbstractBrainServiceTest
{
  @Test
  public void testSessionCookieInsufficientForAuthentication() throws Exception {
    Response response = restRequest().path(UserSessionResource.SERVICE_PATH).post();
    assertResponseStatus(204, response);

    Cookie sessionCookie = getSessionCookie(response);
    assertThat(sessionCookie, is(notNullValue()));

    HttpRequest request = restRequest().path(PublicApiPaths.BASE_PATH, "any/thing").anon().cookie(sessionCookie);
    response = request.get();
    assertResponseStatus(401, response);

    response = request.put();
    assertResponseStatus(401, response);

    response = request.post();
    assertResponseStatus(401, response);

    response = request.delete();
    assertResponseStatus(401, response);
  }

  @Test
  public void testExplicitCredentialsSufficientForAuthentication() throws Exception {
    HttpRequest request = restRequest().path(PublicApiPaths.BASE_PATH, "any/thing");
    Response response = request.get();
    assertResponseStatus(404, response);

    response = request.put();
    assertResponseStatus(404, response);

    response = request.post();
    assertResponseStatus(404, response);

    response = request.delete();
    assertResponseStatus(404, response);
  }

  @Test
  public void testRequestsDoNotCreateSession() throws Exception {
    HttpRequest request = restRequest().path(PublicApiPaths.BASE_PATH, "any/thing");
    Response response = request.get();
    assertResponseStatus(404, response);
    assertThat(getSessionCookie(response), is(nullValue()));

    response = request.put();
    assertResponseStatus(404, response);
    assertThat(getSessionCookie(response), is(nullValue()));

    response = request.post();
    assertResponseStatus(404, response);
    assertThat(getSessionCookie(response), is(nullValue()));

    response = request.delete();
    assertResponseStatus(404, response);
    assertThat(getSessionCookie(response), is(nullValue()));
  }
}
