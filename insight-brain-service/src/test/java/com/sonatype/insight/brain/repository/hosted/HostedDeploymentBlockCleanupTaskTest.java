/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.repository.HostedDeploymentBlockDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.repository.HostedDeploymentBlock;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ContextConfiguration(classes = HostedDeploymentBlockCleanupTaskTest.TestConfig.class)
public class HostedDeploymentBlockCleanupTaskTest
    extends AbstractComponentTest
{
  @TestConfiguration
  static class TestConfig
  {
    @Bean("testTaskScheduler")
    @Primary
    TaskScheduler taskScheduler() {
      return mock(TaskScheduler.class);
    }
  }

  @Inject
  private TaskScheduler taskSchedulerMock;

  @Inject
  private HostedDeploymentBlockCleanupTask cleanupTask;

  @Inject
  private HostedDeploymentBlockDAO blockDAO;

  @Test
  public void register_schedulesPeriodicTaskEvery24Hours() {
    cleanupTask.register();
    verify(taskSchedulerMock).schedulePeriodicTask(cleanupTask, Duration.ofHours(24));
  }

  @Test
  public void execute_quartzJob_runsCleanupAgainstRealDb_usingDefaultRetention() {
    // Default retention property is 24h. Insert a row 25h old (should be deleted) and a row
    // 1h old (should survive). Wire-test that the Quartz entry-point ends in real DB delete.
    Repository repo = tempEntity.newRepository("repo-task-default");
    String old = insertBlock(repo.getId(), Instant.now().minus(Duration.ofHours(25)));
    String recent = insertBlock(repo.getId(), Instant.now().minus(Duration.ofHours(1)));

    cleanupTask.execute(mock(JobExecutionContext.class));

    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      assertThat(blockDAO.getById(tx, old)).isNull();
      assertThat(blockDAO.getById(tx, recent)).isNotNull();
    }
  }

  @Test
  public void execute_quartzJob_honoursOverriddenRetentionProperty() throws Exception {
    // Override the system_config retention to 1 hour. Insert one row 2h old (should be deleted)
    // and one 30 min old (should survive).
    setRetentionHours(1);
    try {
      Repository repo = tempEntity.newRepository("repo-task-override");
      String old = insertBlock(repo.getId(), Instant.now().minus(Duration.ofHours(2)));
      String recent = insertBlock(repo.getId(), Instant.now().minus(Duration.ofMinutes(30)));

      cleanupTask.execute(mock(JobExecutionContext.class));

      try (TransactionContext tx = blockDAO.createTransactionContext()) {
        assertThat(blockDAO.getById(tx, old)).isNull();
        assertThat(blockDAO.getById(tx, recent)).isNotNull();
      }
    }
    finally {
      // Restore the default for any subsequent tests sharing this database fixture.
      setRetentionHours(24);
    }
  }

  @Test
  public void execute_adminTask_triggersTaskScheduler() {
    cleanupTask.execute(null, new PrintWriter(new StringWriter()));
    verify(taskSchedulerMock).triggerTaskNow(cleanupTask, null);
  }

  @Test
  public void disallowConcurrentExecution_isSet() {
    assertThat(JobBuilder.newJob(HostedDeploymentBlockCleanupTask.class).build().isConcurrentExectionDisallowed())
        .isTrue();
  }

  @Test
  public void getJobName_returnsClassName() {
    assertThat(cleanupTask.getJobName()).isEqualTo(HostedDeploymentBlockCleanupTask.NAME);
  }

  // --- helpers ---

  private void setRetentionHours(final int hours) {
    lookup(ApiConfigurationService.class)
        .setConfigurationInDatabaseNoAuthz(
            SystemConfigurationProperty.HOSTED_DEPLOYMENT_BLOCK_RETENTION_HOURS, Integer.valueOf(hours));
    lookup(ApiConfigurationService.class)
        .applyConfigurationToClients(SystemConfigurationProperty.HOSTED_DEPLOYMENT_BLOCK_RETENTION_HOURS);
  }

  private String insertBlock(final String repoId, final Instant blockedAt) {
    String id = UUID.randomUUID().toString();
    HostedDeploymentBlock block = new HostedDeploymentBlock();
    block.setId(id);
    block.setRepositoryId(repoId);
    block.setPathname("com/example/lib-" + id.substring(0, 8) + ".jar");
    block.setHash("hash" + id.substring(0, 12));
    block.setComponentIdFormat("maven2");
    block.setDisplayName("pkg:maven/com.example/lib@" + id.substring(0, 4));
    block.setPolicyAction("FAIL");
    block.setHighestThreatLevel(9);
    block.setEvaluationUrl("https://iq.example.com/report/" + id);
    block.setCorrelationId("corr-" + id.substring(0, 8));
    block.setRequestedBy("test@example.com");
    block.setBlockedTime(Date.from(blockedAt));

    try (TransactionContext tx = blockDAO.createTransactionContext()) {
      tx.begin();
      blockDAO.insertWithViolations(tx, block, List.of());
      tx.commit();
    }
    return id;
  }
}
