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
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Kept in the original {@code com.sonatype.insight.brain.security} package because it references
 * the package-private {@link AntiCsrfFilter#CSRF_HEADER_NAME}.
 */
@IqH2Test
class IqH2ApiAccessControlFilterTest
{
  private static final String REST_PATH = PublicApiPaths.APP_RESOURCE_PATH;

  private IqTestContext ctx;

  private User allowedUser;

  private User forbiddenUser;

  @BeforeEach
  void before() {
    allowedUser = ctx.tempEntity().newUser("allowedUser");
    forbiddenUser = ctx.tempEntity().newUser("forbiddenUser");
  }

  @Test
  void testApiAccessControlFilter_AnonymousAccessDenied() throws Exception {
    // given: apiAccessAllowList is not set

    // when: an anonymous request is received
    HttpResponse response = restRequest().anon().get();

    // then: anonymous access is not allowed
    ctx.assertResponseStatus(HttpServletResponse.SC_UNAUTHORIZED, response);
  }

  @Test
  void testApiAccessControlFilter_AccessAllowedWhenListIsNull() throws Exception {
    // given: apiAccessAllowList is not set

    // when: an authenticated request is received
    HttpResponse response = restRequest().auth(forbiddenUser).get();

    // then: access is unrestricted
    ctx.assertResponseStatus(HttpServletResponse.SC_OK, response);
  }

  @Test
  void testApiAccessControlFilter_AccessAllowed() throws Exception {
    // given: apiAccessAllowList is set to ["allowedUser"]
    setApiAccessAllowList(ImmutableList.of(allowedUser.getUsername()));

    // when: an authenticated request is received
    HttpResponse response = restRequest().auth(allowedUser).get();

    // then: access is unrestricted
    ctx.assertResponseStatus(HttpServletResponse.SC_OK, response);
  }

  @Test
  void testApiAccessControlFilter_AccessAllowedForUiRequests() throws Exception {
    // given: apiAccessAllowList is set to ["allowedUser"]
    setApiAccessAllowList(ImmutableList.of(allowedUser.getUsername()));

    // when: a UI request is received
    HttpResponse response = restRequest().header(AntiCsrfFilter.CSRF_HEADER_NAME, "x").auth(forbiddenUser).get();

    // then: access is unrestricted
    ctx.assertResponseStatus(HttpServletResponse.SC_OK, response);
  }

  @Test
  void testApiAccessControlFilter_AccessAllowedForSysAdmins() throws Exception {
    // given: apiAccessAllowList is set to ["allowedUser"]
    setApiAccessAllowList(ImmutableList.of(allowedUser.getUsername()));

    // when: a sys admin request is received
    HttpResponse response = restRequest().auth().get();

    // then: access is unrestricted
    ctx.assertResponseStatus(HttpServletResponse.SC_OK, response);
  }

  @Test
  void testApiAccessControlFilter_AccessDenied() throws Exception {
    // given: apiAccessAllowList is set to ["allowedUser"]
    setApiAccessAllowList(ImmutableList.of(allowedUser.getUsername()));

    // when: an authenticated request is received from a user not in the apiAccessAllowList
    HttpResponse response = restRequest().auth(forbiddenUser).get();

    // then: access is denied
    ctx.assertResponseStatus(HttpServletResponse.SC_FORBIDDEN, response);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(REST_PATH).noCsrfToken();
  }

  private void setApiAccessAllowList(List<String> allowList) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.API_ACCESS_ALLOW_LIST, allowList);

    ctx.setProperties(properties);
  }
}
