/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.ApplicationCategoryAuditDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.tag.ApplicationTagResource;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2ApplicationTagResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private Application application;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    application = ctx.tempEntity().newApplicationWithParent("appPubId", "appName");
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public com.sonatype.insight.brain.dataaccess.policy.PolicyDAO getPolicyDAO() {
    return ctx.lookup(com.sonatype.insight.brain.dataaccess.policy.PolicyDAO.class);
  }

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private void updateApplicationTags(Consumer<HttpRequest> user, List<Tag> newTags) throws Exception {
    ctx.restRequest()
        .with(user)
        .path(ApplicationTagResource.RESOURCE_PATH)
        .parameter(application.getPublicId())
        .body(newTags)
        .put();
  }

  @Test
  void testUpdateApplicationTags() throws Exception {
    List<Tag> tags = new ArrayList<>();
    tags.add(ctx.tempEntity().newTag(application.getOrganizationId(), "tag name 1"));
    tags.add(ctx.tempEntity().newTag(application.getOrganizationId(), "tag name 2"));
    updateApplicationTags(null, tags);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_APPLICATION_CATEGORY, null);
    assertApplicationData(auditDTO, application);
    assertCustomObject(auditDTO, "applicationCategories", ApplicationCategoryAuditDTO.transcribe(tags));
  }

  @Test
  void testUpdateApplicationTags_NoTags() throws Exception {
    updateApplicationTags(null, new ArrayList<>());

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_APPLICATION_CATEGORY, null);
    assertApplicationData(auditDTO, application);
    assertCustomObject(auditDTO, "applicationCategories", new ArrayList<>());
  }

  @Test
  void testUpdateApplicationTags_Unauthorized() throws Exception {
    updateApplicationTags(unauthorizedUser(), new ArrayList<>());

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_APPLICATION_CATEGORY, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
