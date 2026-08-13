/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlConfigurationDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2ApiSourceControlConfigurationResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
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

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.SOURCE_CONTROL_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  void testSetConfiguration() throws Exception {
    ApiSourceControlConfigurationDTO dto = new ApiSourceControlConfigurationDTO();
    dto.cloneDirectory = "some-clone-directory";
    dto.gitImplementation = GitImplementation.JAVA;
    dto.prCommentPurgeWindow = 1;
    dto.prEventPurgeWindow = 2;
    dto.gitExecutable = "some-git-executable";
    dto.gitTimeoutSeconds = 3;
    dto.commitUsername = "some-commit-username";
    dto.commitEmail = "some-commit-email@d";
    dto.useUsernameInRepositoryCloneUrl = true;
    dto.defaultBranchMonitoringStartTime = "1:11";
    dto.defaultBranchMonitoringIntervalHours = 4;
    dto.pullRequestMonitoringIntervalSeconds = 60;

    HttpResponse response = restRequest().body(dto).put();

    ctx.assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_SOURCE_CONTROL_CONFIGURATION, null);
    assertAuditData(auditDTO, dto.cloneDirectory, dto.gitImplementation, dto.prCommentPurgeWindow,
        dto.prEventPurgeWindow, dto.gitExecutable, dto.gitTimeoutSeconds, dto.commitUsername, dto.commitEmail,
        dto.useUsernameInRepositoryCloneUrl, dto.defaultBranchMonitoringStartTime,
        dto.defaultBranchMonitoringIntervalHours, dto.pullRequestMonitoringIntervalSeconds);
  }

  @Test
  void testSetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().put();

    ctx.assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.CONFIGURE_SOURCE_CONTROL_CONFIGURATION, "bad-request");
  }

  @Test
  void testDeleteConfiguration() throws Exception {
    SourceControlConfiguration config = ctx.tempEntity().newSourceControlConfiguration();

    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_SOURCE_CONTROL_CONFIGURATION, null);
    assertAuditData(auditDTO, config.getCloneDirectory(), config.getGitImplementation(),
        config.getPrCommentPurgeWindow(), config.getPrEventPurgeWindow(), config.getGitExecutable(),
        config.getGitTimeoutSeconds(), config.getCommitUsername(), config.getCommitEmail(),
        config.isUseUsernameInRepositoryCloneUrl(), config.getDefaultBranchMonitoringStartTimeString(),
        config.getDefaultBranchMonitoringIntervalHours(), config.getPullRequestMonitoringIntervalSeconds());
  }

  @Test
  void testDeleteConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    ctx.assertResponseStatus(404, response);
    assertAuditLog(AuditEvent.DELETE_SOURCE_CONTROL_CONFIGURATION, "not-found");
  }

  private void assertAuditData(
      AuditDTO auditDTO,
      String cloneDirectory,
      GitImplementation gitImplementation,
      Integer prCommentPurgeWindow,
      Integer prEventPurgeWindow,
      String gitExecutable,
      int gitTimeoutSeconds,
      String commitUsername,
      String commitEmail,
      boolean useUsernameInRepositoryCloneUrl,
      String defaultBranchMonitoringStartTime,
      int defaultBranchMonitoringIntervalHours,
      int pullRequestMonitoringIntervalSeconds)
  {
    assertCustomData(auditDTO, "cloneDirectory", cloneDirectory);
    assertCustomData(auditDTO, "gitImplementation", gitImplementation == null ? null : gitImplementation.toString());
    assertCustomData(auditDTO, "prCommentPurgeWindow", prCommentPurgeWindow);
    assertCustomData(auditDTO, "prEventPurgeWindow", prEventPurgeWindow);
    assertCustomData(auditDTO, "gitExecutable", gitExecutable);
    assertCustomData(auditDTO, "gitTimeoutSeconds", gitTimeoutSeconds);
    assertCustomData(auditDTO, "commitUsername", commitUsername);
    assertCustomData(auditDTO, "commitEmail", commitEmail);
    assertCustomData(auditDTO, "useUsernameInRepositoryCloneUrl", useUsernameInRepositoryCloneUrl);
    assertCustomData(auditDTO, "defaultBranchMonitoringStartTime", defaultBranchMonitoringStartTime);
    assertCustomData(auditDTO, "defaultBranchMonitoringIntervalHours", defaultBranchMonitoringIntervalHours);
    assertCustomData(auditDTO, "pullRequestMonitoringIntervalSeconds", pullRequestMonitoringIntervalSeconds);
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
