/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.HttpCookie;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;

import com.google.common.net.HttpHeaders;
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
    HttpResponse response = restRequest().path(UserSessionResource.RESOURCE_PATH).post();
    assertResponseStatus(204, response);

    HttpCookie sessionCookie = response.getSessionCookie();
    assertThat(sessionCookie, is(notNullValue()));

    HttpRequest request = restRequest().path(PublicApiPaths.BASE_PATH, "any/thing").anon().cookie(sessionCookie);
    response = request.get();
    assertResponse401(response, BasicHttpAuthenticationMandatoryFilter.SESSION_COOKIE_MESSAGE);

    response = request.put();
    assertResponse401(response, BasicHttpAuthenticationMandatoryFilter.SESSION_COOKIE_MESSAGE);

    response = request.post();
    assertResponse401(response, BasicHttpAuthenticationMandatoryFilter.SESSION_COOKIE_MESSAGE);

    response = request.delete();
    assertResponse401(response, BasicHttpAuthenticationMandatoryFilter.SESSION_COOKIE_MESSAGE);
  }

  @Test
  public void testNoAuthentication() throws Exception {
    testBadAuthentication(null, null, BasicHttpAuthenticationMandatoryFilter.INVALID_AUTHENTICATION_MESSAGE);
  }

  @Test
  public void testInvalidUser() throws Exception {
    testBadAuthentication("yeti", "password", ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT);
  }

  @Test
  public void testInvalidPassword() throws Exception {
    testBadAuthentication("admin", "wrong password", ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT);
  }

  private void testBadAuthentication(String username, String password, String expectedMessage) throws Exception {
    HttpRequest request = restRequest().path(PublicApiPaths.BASE_PATH, "any/thing").auth(username, password);
    HttpResponse response = request.get();
    assertResponseStatus(401, response);
    assertResponse401(response, expectedMessage);

    response = request.put();
    assertResponse401(response, expectedMessage);

    response = request.post();
    assertResponse401(response, expectedMessage);

    response = request.delete();
    assertResponse401(response, expectedMessage);
  }

  @Test
  public void testExplicitCredentialsSufficientForAuthentication() throws Exception {
    HttpRequest request = restRequest().path(PublicApiPaths.BASE_PATH, "any/thing");
    HttpResponse response = request.get();
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
    HttpResponse response = request.get();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie(), is(nullValue()));

    response = request.put();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie(), is(nullValue()));

    response = request.post();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie(), is(nullValue()));

    response = request.delete();
    assertResponseStatus(404, response);
    assertThat(response.getSessionCookie(), is(nullValue()));
  }

  private void assertResponse401(HttpResponse response, String expectedMessage) {
    assertResponseStatus(401, response);
    assertThat(response.getBodyText(), is(expectedMessage));
    assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE), is(nullValue()));
  }
}
