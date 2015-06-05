/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.security.UserSessionResource;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.test.RestAccess;

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
    Response response = AuthedRestAccess.post(getRestBaseUrl() + UserSessionResource.SERVICE_PATH, null);
    assertResponseStatus(204, response);

    Cookie sessionCookie = getSessionCookie(response);
    assertThat(sessionCookie, is(notNullValue()));

    response = RestAccess.get(getRestUrl(PublicApiPaths.BASE_PATH + "/any/thing"), sessionCookie);
    assertResponseStatus(401, response);

    response = RestAccess.put(getRestUrl(PublicApiPaths.BASE_PATH + "/any/thing"), null, null, null, null,
        sessionCookie, "body", null, null);
    assertResponseStatus(401, response);

    response = RestAccess.post(getRestUrl(PublicApiPaths.BASE_PATH + "/any/thing"), sessionCookie);
    assertResponseStatus(401, response);

    response = RestAccess.delete(getRestUrl(PublicApiPaths.BASE_PATH + "/any/thing"), null, null, null, sessionCookie);
    assertResponseStatus(401, response);
  }

  @Test
  public void testExplicitCredentialsSufficientForAuthentication() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl(PublicApiPaths.BASE_PATH + "/any/thing"));
    assertResponseStatus(404, response);

    response = AuthedRestAccess.put(getRestUrl(PublicApiPaths.BASE_PATH + "/any/thing"), "body");
    assertResponseStatus(404, response);

    response = AuthedRestAccess.post(getRestUrl(PublicApiPaths.BASE_PATH + "/any/thing"), "body");
    assertResponseStatus(404, response);

    response = AuthedRestAccess.delete(getRestUrl(PublicApiPaths.BASE_PATH + "/any/thing"));
    assertResponseStatus(404, response);
  }

  @Test
  public void testRequestsDoNotCreateSession() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl(PublicApiPaths.BASE_PATH + "/any/thing"));
    assertResponseStatus(404, response);
    assertThat(getSessionCookie(response), is(nullValue()));

    response = AuthedRestAccess.put(getRestUrl(PublicApiPaths.BASE_PATH + "/any/thing"), "body");
    assertResponseStatus(404, response);
    assertThat(getSessionCookie(response), is(nullValue()));

    response = AuthedRestAccess.post(getRestUrl(PublicApiPaths.BASE_PATH + "/any/thing"), "body");
    assertResponseStatus(404, response);
    assertThat(getSessionCookie(response), is(nullValue()));

    response = AuthedRestAccess.delete(getRestUrl(PublicApiPaths.BASE_PATH + "/any/thing"));
    assertResponseStatus(404, response);
    assertThat(getSessionCookie(response), is(nullValue()));
  }
}
