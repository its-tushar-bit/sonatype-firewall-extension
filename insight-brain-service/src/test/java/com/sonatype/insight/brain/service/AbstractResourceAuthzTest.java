/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;

import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Before;
import org.junit.Rule;

/**
 * Provides boilerplate fixture for authorization tests.
 */
public abstract class AbstractResourceAuthzTest
    extends AbstractResourceTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  protected Organization org;

  protected Application app;

  protected User unauthorized;

  protected User authorized;

  @Before
  public void createEntities() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    unauthorized = tempEntity.newUser();
    authorized = tempEntity.newUser();
  }

  protected String getRestUrl(String templateUrl, Object... paramValues) {
    return getRestBaseUrl() + expandRestUrl(templateUrl, paramValues);
  }

  protected String toJson(Object object) {
    try {
      return JsonHelpers.asJson(object);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  protected <T> T fromJson(Response response, Class<T> type) {
    try {
      return JsonHelpers.fromJson(response.getResponseBody(), type);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  protected void testAuthzGet(String url) throws Exception {
    testAuthzGet(url, 200);
  }

  protected void testAuthzGet(String url, int expectedSuccessStatus) throws Exception {
    Response response = RestAccess.get(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.get(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(expectedSuccessStatus, response);
  }

  protected void testAuthzPut(String url, String body) throws Exception {
    testAuthzPut(url, body, 200);
  }

  protected void testAuthzPut(String url, String body, int expectedSuccessStatus) throws Exception {
    Response response = RestAccess.put(url, unauthorized.getUsername(), unauthorized.getPassword(), body);
    assertResponseStatus(403, response);

    response = RestAccess.put(url, authorized.getUsername(), authorized.getPassword(), body);
    assertResponseStatus(expectedSuccessStatus, response);
  }

  protected Response testAuthzPost(String url, String body) throws Exception {
    return testAuthzPost(url, body, 200);
  }

  protected Response testAuthzPost(String url, String body, int expectedSuccessStatus) throws Exception {
    Response response = RestAccess.post(url, unauthorized.getUsername(), unauthorized.getPassword(), body);
    assertResponseStatus(403, response);

    response = RestAccess.post(url, authorized.getUsername(), authorized.getPassword(), body);
    assertResponseStatus(expectedSuccessStatus, response);

    return response;
  }

  protected Response testAuthzDelete(String url) throws Exception {
    Response response = RestAccess.delete(url, unauthorized.getUsername(), unauthorized.getPassword());
    assertResponseStatus(403, response);

    response = RestAccess.delete(url, authorized.getUsername(), authorized.getPassword());
    assertResponseStatus(204, response);

    return response;
  }
}
