/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.ImmutableList;

public class ApiAccessControlFilterTest
    extends AbstractBrainServiceIntegrationTest
{
  private static final String REST_PATH = PublicApiPaths.APP_RESOURCE_PATH;

  private User allowedUser;

  private User forbiddenUser;

  @Before
  public void before() {
    allowedUser = tempEntity.newUser("allowedUser");
    forbiddenUser = tempEntity.newUser("forbiddenUser");
  }

  @Test
  public void testApiAccessControlFilter_AnonymousAccessDenied() throws Exception {
    // given: apiAccessAllowList is not set

    // when: an anonymous request is received
    HttpResponse response = restRequest().anon().get();

    // then: anonymous access is not allowed
    assertResponseStatus(HttpServletResponse.SC_UNAUTHORIZED, response);
  }

  @Test
  public void testApiAccessControlFilter_AccessAllowedWhenListIsNull() throws Exception {
    // given: apiAccessAllowList is not set

    // when: an authenticated request is received
    HttpResponse response = restRequest().auth(forbiddenUser).get();

    // then: access is unrestricted
    assertResponseStatus(HttpServletResponse.SC_OK, response);
  }

  @Test
  public void testApiAccessControlFilter_AccessAllowed() throws Exception {
    // given: apiAccessAllowList is set to ["allowedUser"]
    setApiAccessAllowList(ImmutableList.of(allowedUser.getUsername()));

    // when: an authenticated request is received
    HttpResponse response = restRequest().auth(allowedUser).get();

    // then: access is unrestricted
    assertResponseStatus(HttpServletResponse.SC_OK, response);
  }

  @Test
  public void testApiAccessControlFilter_AccessAllowedForUiRequests() throws Exception {
    // given: apiAccessAllowList is set to ["allowedUser"]
    setApiAccessAllowList(ImmutableList.of(allowedUser.getUsername()));

    // when: a UI request is received
    HttpResponse response = restRequest().header(AntiCsrfFilter.CSRF_HEADER_NAME, "x").auth(forbiddenUser).get();

    // then: access is unrestricted
    assertResponseStatus(HttpServletResponse.SC_OK, response);
  }

  @Test
  public void testApiAccessControlFilter_AccessAllowedForSysAdmins() throws Exception {
    // given: apiAccessAllowList is set to ["allowedUser"]
    setApiAccessAllowList(ImmutableList.of(allowedUser.getUsername()));

    // when: a sys admin request is received
    HttpResponse response = restRequest().auth().get();

    // then: access is unrestricted
    assertResponseStatus(HttpServletResponse.SC_OK, response);
  }

  @Test
  public void testApiAccessControlFilter_AccessDenied() throws Exception {
    // given: apiAccessAllowList is set to ["allowedUser"]
    setApiAccessAllowList(ImmutableList.of(allowedUser.getUsername()));

    // when: an authenticated request is received from a user not in the apiAccessAllowList
    HttpResponse response = restRequest().auth(forbiddenUser).get();

    // then: access is denied
    assertResponseStatus(HttpServletResponse.SC_FORBIDDEN, response);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(REST_PATH).noCsrfToken();
  }

  private void setApiAccessAllowList(List<String> allowList) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.API_ACCESS_ALLOW_LIST, allowList);

    setProperties(properties);
  }
}
