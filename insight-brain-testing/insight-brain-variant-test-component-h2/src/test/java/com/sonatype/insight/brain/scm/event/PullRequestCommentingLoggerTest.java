/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scm.event;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.test.LogOutput;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;

import static ch.qos.logback.classic.Level.OFF;
import static com.sonatype.insight.brain.scm.event.AbstractSourceControlEventLogger.SCM_EVENT_LOGGER_NAME;
import static com.sonatype.insight.brain.scm.event.AbstractSourceControlEventLogger.SourceControlEventData.forComment;
import static com.sonatype.insight.brain.scm.event.AbstractSourceControlEventLogger.SourceControlEventData.forError;
import static com.sonatype.insight.brain.scm.event.SourceControlEventType.API_ERROR;
import static com.sonatype.insight.brain.scm.event.SourceControlEventType.PR_COMMENT_CREATED;
import static com.sonatype.insight.brain.scm.event.SourceControlEventType.PR_COMMENT_UPDATED;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.resetAfterTest;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ComponentH2Test
public class PullRequestCommentingLoggerTest
    extends AbstractComponentH2Test
{
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Mock
  private CurrentUser currentUser;

  @Rule
  public LogOutput logOutput = new LogOutput(SCM_EVENT_LOGGER_NAME);

  private Organization organization;

  private Application application;

  private GitRepositoryInfo gitRepositoryInfo;

  @BeforeEach
  public void before() {
    lenient().when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());
    gitRepositoryInfo =
        new GitRepositoryInfo("https://github.com/test/repo", null, "test-user", "test-token", GITHUB, "main", true,
            true, false, false, true, true, false, null);
  }

  @AfterEach
  public void after() {
    resetAfterTest();
  }

  @Test
  public void testLog_PrCommentCreated() {
    testAsNewTenant(testName, tenant -> {
      PullRequestCommentingLogger logger = createCommentingLogger();

      logger.add(PR_COMMENT_CREATED, forComment("123", 5, 2));
      logger.log();

      List<SourceControlEventLogDTO> logDTOs = assertLogDTOs(1);
      SourceControlEventLogDTO dto = logDTOs.get(0);
      assertThat(dto.eventType).isEqualTo(PR_COMMENT_CREATED.name().toLowerCase());
      assertThat(dto.userName).isEqualTo(USERNAME);
      assertThat(dto.applicationId).isEqualTo(application.getId());
      assertThat(dto.applicationPublicId).isEqualTo(application.getPublicId());
      assertThat(dto.applicationName).isEqualTo(application.getName());
      assertThat(dto.organizationId).isEqualTo(organization.getId());
      assertThat(dto.organizationName).isEqualTo(organization.getName());
      assertThat(dto.scmProvider).isEqualTo(GITHUB.name());
      assertThat(dto.repositoryUrl).isEqualTo("https://github.com/test/repo");
      assertThat(dto.pullRequestNumber).isEqualTo("123");
      assertThat(dto.violationsAppeared).isEqualTo(5);
      assertThat(dto.violationsCleared).isEqualTo(2);
      assertThat(dto.errorMessage).isNull();
      assertThat(dto.eventTimestamp).isNotNull();
      assertThat(dto.eventTimestamp).matches("\\d{4}-\\d{2}-\\d{2}T.*");
      assertThat(dto.tenant).isEqualTo(tenant.tenantSlug);
    });
  }

  @Test
  public void testLog_PrCommentUpdated() {
    testAsNewTenant(testName, tenant -> {
      PullRequestCommentingLogger logger = createCommentingLogger();

      logger.add(PR_COMMENT_UPDATED, forComment("456", 3, 7));
      logger.log();

      List<SourceControlEventLogDTO> logDTOs = assertLogDTOs(1);
      SourceControlEventLogDTO dto = logDTOs.get(0);
      assertThat(dto.eventType).isEqualTo(PR_COMMENT_UPDATED.name().toLowerCase());
      assertThat(dto.pullRequestNumber).isEqualTo("456");
      assertThat(dto.violationsAppeared).isEqualTo(3);
      assertThat(dto.violationsCleared).isEqualTo(7);
      assertThat(dto.eventTimestamp).isNotNull();
    });
  }

  @Test
  public void testLog_ApiError() {
    testAsNewTenant(testName, tenant -> {
      PullRequestCommentingLogger logger = createCommentingLogger();

      logger.add(API_ERROR, forError("Authentication failed"));
      logger.log();

      List<SourceControlEventLogDTO> logDTOs = assertLogDTOs(1);
      SourceControlEventLogDTO dto = logDTOs.get(0);
      assertThat(dto.eventType).isEqualTo(API_ERROR.name().toLowerCase());
      assertThat(dto.errorMessage).isEqualTo("Authentication failed");
      assertThat(dto.pullRequestNumber).isNull();
      assertThat(dto.violationsAppeared).isNull();
      assertThat(dto.violationsCleared).isNull();
      assertThat(dto.eventTimestamp).isNotNull();
    });
  }

  @Test
  public void testLog_MultipleEvents() {
    testAsNewTenant(testName, tenant -> {
      PullRequestCommentingLogger logger = createCommentingLogger();

      logger.add(PR_COMMENT_CREATED, forComment("123", 5, 2));
      logger.add(PR_COMMENT_UPDATED, forComment("123", 3, 4));
      logger.log();

      List<SourceControlEventLogDTO> logDTOs = assertLogDTOs(2);
      assertThat(logDTOs.get(0).eventType).isEqualTo(PR_COMMENT_CREATED.name().toLowerCase());
      assertThat(logDTOs.get(0).eventTimestamp).isNotNull();
      assertThat(logDTOs.get(1).eventType).isEqualTo(PR_COMMENT_UPDATED.name().toLowerCase());
      assertThat(logDTOs.get(1).eventTimestamp).isNotNull();
    });
  }

  @Test
  public void testLog_WhenLoggerDisabled_ShouldNotLogMessages() {
    Logger logger = getScmEventLogger();
    Level level = logger.getLevel();
    try {
      logger.setLevel(OFF);

      PullRequestCommentingLogger commentingLogger = createCommentingLogger();

      commentingLogger.add(PR_COMMENT_CREATED, forComment("123", 5, 2));
      commentingLogger.log();

      assertThat(logOutput.getInfoMessages(SCM_EVENT_LOGGER_NAME)).isEmpty();
    }
    finally {
      logger.setLevel(level);
    }
  }

  @Test
  public void testIsEnabled_WhenMultiTenant_ShouldReturnTrue() {
    testAsNewTenant(testName, tenant -> {
      PullRequestCommentingLogger logger = createCommentingLogger();
      assertThat(logger.isEnabled()).isTrue();
    });
  }

  @Test
  public void testIsEnabled_WhenLoggerDisabled_ShouldReturnFalse() {
    testAsNewTenant(testName, tenant -> {
      Logger logger = getScmEventLogger();
      Level level = logger.getLevel();
      try {
        logger.setLevel(OFF);
        PullRequestCommentingLogger scmLogger = createCommentingLogger();
        assertThat(scmLogger.isEnabled()).isFalse();
      }
      finally {
        logger.setLevel(level);
      }
    });
  }

  @Test
  public void testIsEnabled_WhenNotMultiTenant_ShouldReturnFalse() {
    PullRequestCommentingLogger logger = createCommentingLogger();
    assertThat(logger.isEnabled()).isFalse();
  }

  @Test
  public void testAdd_WhenDisabled_ShouldNotAddEvents() {
    // In non-MTIQ context, logger should be disabled
    PullRequestCommentingLogger logger = createCommentingLogger();
    assertThat(logger.isEnabled()).isFalse();

    logger.add(PR_COMMENT_CREATED, forComment("123", 5, 2));
    logger.log();

    // Should produce NO log output
    assertThat(logOutput.getInfoMessages(SCM_EVENT_LOGGER_NAME)).isEmpty();
  }

  @Test
  public void testLog_CalledMultipleTimes_ShouldClearAfterEachCall() {
    testAsNewTenant(testName, tenant -> {
      PullRequestCommentingLogger logger = createCommentingLogger();

      // First batch
      logger.add(PR_COMMENT_CREATED, forComment("123", 5, 2));
      logger.log();
      assertLogDTOs(1);

      // Second batch - should only log new events
      logger.add(PR_COMMENT_UPDATED, forComment("456", 3, 1));
      logger.log();

      // Should have 2 messages total (1 from first call, 1 from second call)
      List<String> allMessages = logOutput.getInfoMessages(SCM_EVENT_LOGGER_NAME);
      assertThat(allMessages).hasSize(2);

      // Third call without adding anything - should produce nothing new
      logger.log();
      assertThat(logOutput.getInfoMessages(SCM_EVENT_LOGGER_NAME)).hasSize(2);
    });
  }

  @Test
  public void testLog_PrCreated() {
    testAsNewTenant(testName, tenant -> {
      PullRequestCommentingLogger logger = createCommentingLogger();

      logger.add(SourceControlEventType.PR_CREATED,
          AbstractSourceControlEventLogger.SourceControlEventData.forPullRequest("789"));
      logger.log();

      List<SourceControlEventLogDTO> logDTOs = assertLogDTOs(1);
      SourceControlEventLogDTO dto = logDTOs.get(0);
      assertThat(dto.eventType).isEqualTo("pr_created");
      assertThat(dto.pullRequestNumber).isEqualTo("789");
      assertThat(dto.violationsAppeared).isNull();
      assertThat(dto.violationsCleared).isNull();
      assertThat(dto.errorMessage).isNull();
      assertThat(dto.eventTimestamp).isNotNull();
      assertThat(dto.tenant).isEqualTo(tenant.tenantSlug);
    });
  }

  @Test
  public void testLog_PrCreated_WithTraceContextEmitsAllFields() {
    testAsNewTenant(testName, tenant -> {
      PullRequestCommentingLogger logger = createCommentingLogger();

      logger.add(SourceControlEventType.PR_CREATED,
          AbstractSourceControlEventLogger.SourceControlEventData.forPullRequest("789")
              .withTraceContext("GITHUB_APP", "owner-O", "12345", "99999", "SUCCESS", null));
      logger.log();

      SourceControlEventLogDTO dto = assertLogDTOs(1).get(0);
      assertThat(dto.authenticationType).isEqualTo("GITHUB_APP");
      assertThat(dto.authOwnerId).isEqualTo("owner-O");
      assertThat(dto.githubAppId).isEqualTo("12345");
      assertThat(dto.installationId).isEqualTo("99999");
      assertThat(dto.outcome).isEqualTo("SUCCESS");
      assertThat(dto.failureReason).isNull();
    });
  }

  @Test
  public void testLog_ApiError_WithTraceContextEmitsCategoricalReason() {
    testAsNewTenant(testName, tenant -> {
      PullRequestCommentingLogger logger = createCommentingLogger();

      logger.add(API_ERROR,
          forError("Pull request creation failed: anything")
              .withTraceContext("PAT", "owner-A", null, null, "FAILURE", "auth_invalid"));
      logger.log();

      SourceControlEventLogDTO dto = assertLogDTOs(1).get(0);
      assertThat(dto.authenticationType).isEqualTo("PAT");
      assertThat(dto.authOwnerId).isEqualTo("owner-A");
      assertThat(dto.githubAppId).isNull();
      assertThat(dto.installationId).isNull();
      assertThat(dto.outcome).isEqualTo("FAILURE");
      assertThat(dto.failureReason).isEqualTo("auth_invalid");
    });
  }

  @Test
  public void testLog_WithAllNullParameters_ProducesValidJson() {
    testAsNewTenant(testName, tenant -> {
      PullRequestCommentingLogger logger = new PullRequestCommentingLogger(
          new Date(), null, null, null, currentUser);

      logger.add(API_ERROR, forError("test error"));
      logger.log();

      List<SourceControlEventLogDTO> logDTOs = assertLogDTOs(1);
      SourceControlEventLogDTO dto = logDTOs.get(0);

      // Should produce valid JSON with minimal content
      assertThat(dto.eventType).isEqualTo("api_error");
      assertThat(dto.errorMessage).isEqualTo("test error");
      assertThat(dto.userName).isNotNull();
      assertThat(dto.tenant).isEqualTo(tenant.tenantSlug);
      assertThat(dto.eventTimestamp).isNotNull();

      // All optional fields should be null
      assertThat(dto.applicationId).isNull();
      assertThat(dto.organizationId).isNull();
      assertThat(dto.scmProvider).isNull();
      assertThat(dto.repositoryUrl).isNull();
    });
  }

  private Logger getScmEventLogger() {
    return (Logger) LoggerFactory.getLogger(SCM_EVENT_LOGGER_NAME);
  }

  private PullRequestCommentingLogger createCommentingLogger() {
    return new PullRequestCommentingLogger(new Date(), application, organization, gitRepositoryInfo, currentUser);
  }

  private List<SourceControlEventLogDTO> assertLogDTOs(int expectedCount) {
    List<String> infoMessages = logOutput.getInfoMessages(SCM_EVENT_LOGGER_NAME);
    assertThat(infoMessages).hasSize(expectedCount);
    return infoMessages.stream()
        .map(message -> {
          try {
            return OBJECT_MAPPER.readValue(message, SourceControlEventLogDTO.class);
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        })
        .toList();
  }
}
