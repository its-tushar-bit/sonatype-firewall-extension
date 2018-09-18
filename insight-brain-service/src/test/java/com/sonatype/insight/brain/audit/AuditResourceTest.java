/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.List;

import com.sonatype.insight.brain.organization.ApplicationResource;
import com.sonatype.insight.brain.security.UserSessionResource;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.LogOutput;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class AuditResourceTest
    extends AbstractBrainServiceTest
{
  private static final String RESTRICTED_PATH = "/" + ApplicationResource.RESOURCE_PATH;

  private static final String AUTH_RESOURCE_PATH = "/" + UserSessionResource.RESOURCE_PATH;

  @Rule
  public LogOutput logOutput = new LogOutput("com.sonatype.insight.audit");

  @Before
  public void before() {
    logOutput.before();
  }

  @Test
  public void testNoAuthenticationHeadersOrCookies() throws Exception {
    HttpGet httpGet = new HttpGet(getRestBaseUrl() + RESTRICTED_PATH);

    HttpClientBuilder.create().build().execute(httpGet);

    List<String> auditAuthenticationMessages = logOutput
        .getInfoMessages("com.sonatype.insight.audit.authentication");
    assertThat(auditAuthenticationMessages, notNullValue());
    assertThat(auditAuthenticationMessages, hasSize(1));
    assertAuditLog(auditAuthenticationMessages.get(0), "GET", RESTRICTED_PATH, "unauthenticated");
  }

  @Test
  public void testInvalidUserNamePassword() throws Exception {
    restRequest().auth("invalidUser", "invalidPassword").path(AUTH_RESOURCE_PATH).post();
    List<String> auditAuthenticationMessages = logOutput
        .getInfoMessages("com.sonatype.insight.audit.authentication");

    assertThat(auditAuthenticationMessages, notNullValue());
    assertThat(auditAuthenticationMessages, hasSize(1));
    assertAuditLog(auditAuthenticationMessages.get(0), "POST", AUTH_RESOURCE_PATH, "bad-authentication");
  }

  private void assertAuditLog(final String auditLogEntry,
                              final String method,
                              final String resourcePath,
                              final String error) throws Exception
  {
    AuditDTO auditDTO = JsonUtils.asPojo(JsonUtils.parse(auditLogEntry), AuditDTO.class);
    assertThat(auditDTO.method, is(method));
    assertThat(auditDTO.path, is(resourcePath));
    assertThat(auditDTO.domain, is("authentication"));
    assertThat(auditDTO.type, is("failure"));
    assertThat(auditDTO.error, is(error));
  }
}
