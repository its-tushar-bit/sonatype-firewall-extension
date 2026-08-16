/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.function.Consumer;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.test.LogOutput;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;

public abstract class AbstractAuditTest
    extends AbstractResourceTest
    implements AuditTestSupport
{
  @Rule
  public LogOutput logOutput = new LogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private User unauthorizedUser;

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Before
  @BeforeEach
  public void setupCommonFixture() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = tempEntity.newUser();
  }

  protected Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return lookup(PolicyDAO.class);
  }
}
