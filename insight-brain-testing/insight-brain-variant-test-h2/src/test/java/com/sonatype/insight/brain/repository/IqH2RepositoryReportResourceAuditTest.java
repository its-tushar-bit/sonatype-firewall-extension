/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.function.Consumer;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Kept in the {@code com.sonatype.insight.brain.repository} package because
 * {@link RepositoryReportResource#RESOURCE_PATH} and {@link RepositoryReportResource#SUMMARY}
 * are package-private. Reproduces the {@code AbstractAuditTest}/{@code AbstractComponentInfoResourceAuditTest}
 * scaffolding that the legacy {@code RepositoryReportResourceAuditTest} inherited.
 */
@IqH2Test
class IqH2RepositoryReportResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private Repository repository;

  private User unauthorizedUser;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    repository = ctx.tempEntity().newRepository("repoPublicId");
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
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  @Test
  void testGetRepositorySummary() throws Exception {
    repositoryResourceRequest().path(RepositoryReportResource.SUMMARY).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, null);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testGetRepositorySummary_Unauthorized() throws Exception {
    repositoryResourceRequest().path(RepositoryReportResource.SUMMARY).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  private HttpRequest repositoryResourceRequest() {
    return ctx.restRequest().path(RepositoryReportResource.RESOURCE_PATH).parameter(repository.getId());
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
