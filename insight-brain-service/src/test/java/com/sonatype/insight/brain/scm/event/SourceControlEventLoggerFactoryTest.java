/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scm.event;

import java.util.Date;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.test.LogOutput;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static ch.qos.logback.classic.Level.OFF;
import static com.sonatype.insight.brain.scm.event.AbstractSourceControlEventLogger.SCM_EVENT_LOGGER_NAME;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.resetAfterTest;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlEventLoggerFactoryTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(SCM_EVENT_LOGGER_NAME);

  @Inject
  private SourceControlEventLoggerFactory scmEventLoggerFactory;

  private Organization organization;

  private Application application;

  private GitRepositoryInfo gitRepositoryInfo;

  @Before
  public void before() {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());
    gitRepositoryInfo =
        new GitRepositoryInfo("https://github.com/test/repo", null, "test-user", "test-token", GITHUB, "main", true,
            true, false, false, true, true, false, null);
  }

  @After
  public void after() {
    resetAfterTest();
  }

  @Test
  public void testNewLogger_WhenMultiTenant_ShouldBeEnabled() {
    testAsNewTenant(testName, tenant -> {
      PullRequestCommentingLogger logger = createLogger();
      assertThat(logger.isEnabled()).isTrue();
    });
  }

  @Test
  public void testNewLogger_WhenLoggerDisabled_ShouldBeDisabled() {
    testAsNewTenant(testName, tenant -> {
      Logger logger = getScmEventLogger();
      Level level = logger.getLevel();
      try {
        logger.setLevel(OFF);
        PullRequestCommentingLogger scmLogger = createLogger();
        assertThat(scmLogger.isEnabled()).isFalse();
      }
      finally {
        logger.setLevel(level);
      }
    });
  }

  @Test
  public void testNewLogger_WithNullApplication() {
    testAsNewTenant(testName, tenant -> {
      PullRequestCommentingLogger logger = scmEventLoggerFactory.newLogger(
          new Date(), null, organization, gitRepositoryInfo);
      assertThat(logger.isEnabled()).isTrue();
    });
  }

  @Test
  public void testNewLogger_WithNullOrganization() {
    testAsNewTenant(testName, tenant -> {
      PullRequestCommentingLogger logger = scmEventLoggerFactory.newLogger(
          new Date(), application, null, gitRepositoryInfo);
      assertThat(logger.isEnabled()).isTrue();
    });
  }

  @Test
  public void testNewLogger_WithNullGitRepositoryInfo() {
    testAsNewTenant(testName, tenant -> {
      PullRequestCommentingLogger logger = scmEventLoggerFactory.newLogger(
          new Date(), application, organization, null);
      assertThat(logger.isEnabled()).isTrue();
    });
  }

  @Test
  public void testNewLogger_WhenNotMultiTenant_ShouldBeDisabled() {
    PullRequestCommentingLogger logger = createLogger();
    assertThat(logger.isEnabled()).isFalse();
  }

  private Logger getScmEventLogger() {
    return (Logger) LoggerFactory.getLogger(SCM_EVENT_LOGGER_NAME);
  }

  private PullRequestCommentingLogger createLogger() {
    return scmEventLoggerFactory.newLogger(new Date(), application, organization, gitRepositoryInfo);
  }
}
