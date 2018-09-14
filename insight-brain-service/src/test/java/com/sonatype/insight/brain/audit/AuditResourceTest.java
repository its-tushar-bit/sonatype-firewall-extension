/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.List;

import com.sonatype.insight.brain.organization.ApplicationResource;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.LogOutput;

import org.apache.http.HttpStatus;
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

  @Rule
  public LogOutput logOutput = new LogOutput("audit");

  @Before
  public void before() {
    logOutput.before();
  }

  @Test
  public void testNoAuthenticationHeadersOrCookies() throws Exception {
    HttpGet httpGet = new HttpGet(getRestBaseUrl() + RESTRICTED_PATH);

    HttpClientBuilder.create().build().execute(httpGet);

    List<String> auditAuthenticationMessages = logOutput.getInfoMessages("audit.authentication");
    assertThat(auditAuthenticationMessages, notNullValue());
    assertThat(auditAuthenticationMessages, hasSize(1));
    AuditDTO auditDTO = JsonUtils.asPojo(JsonUtils.parse(auditAuthenticationMessages.get(0)), AuditDTO.class);
    assertThat(auditDTO.method, is("GET"));
    assertThat(auditDTO.path, is(RESTRICTED_PATH));
    assertThat(auditDTO.logger, is("audit.authentication"));
    assertThat(auditDTO.event, is(AuditEvent.AUTHENTICATION_FAILURE.name()));
    assertThat(auditDTO.httpStatus, is(HttpStatus.SC_UNAUTHORIZED));
    assertThat(auditDTO.error, is("unauthenticated"));
  }
}
