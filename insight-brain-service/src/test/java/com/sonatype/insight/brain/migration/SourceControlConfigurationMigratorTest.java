/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.api.v2.service.SourceControlConfigurationListener;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.io.File;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

public class SourceControlConfigurationMigratorTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(SourceControlConfigurationMigrator.class);

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private SourceControlConfigurationDAO sourceControlConfigurationDAO;

  @Mock
  private SourceControlConfigurationListener mockSourceControlConfigurationListener;

  @Inject
  private SourceControlConfigurationMigrator sourceControlConfigurationMigrator;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Before
  @After
  public void clear() {
    migrationTrackerDAO.deleteById(SourceControlConfigurationMigrator.MIGRATION_ID);
  }

  @Test
  public void testMigrate_FirstRun_InvalidConfig() {
    SourceControlConfigurationMigrator.SourceControlConfig sourceControlConfig =
        new SourceControlConfigurationMigrator.SourceControlConfig();
    sourceControlConfig.setCommitEmail("invalid");
    insightConfig.setSourceControl(sourceControlConfig);

    sourceControlConfigurationMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(SourceControlConfigurationMigrator.MIGRATION_ID)).isTrue();
    assertThat(sourceControlConfigurationDAO.get()).isNull();
    assertThat(logOutput).atWarnLevel().contains(SourceControlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_DefaultConfig() {
    sourceControlConfigurationMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(SourceControlConfigurationMigrator.MIGRATION_ID)).isTrue();
    assertThat(sourceControlConfigurationDAO.get()).isNull();
    verify(mockSourceControlConfigurationListener).sourceControlConfigurationChanged();
    assertThat(logOutput).doesNotContain(SourceControlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_Full() {
    SourceControlConfigurationMigrator.SourceControlConfig sourceControlConfig =
        new SourceControlConfigurationMigrator.SourceControlConfig();
    sourceControlConfig.setCloneDirectory("some-clone-directory");
    sourceControlConfig.setSonatypeWorkDir(new File("some-sonatype-work-directory"));
    sourceControlConfig.setGitImplementation("native");
    sourceControlConfig.setPrCommentPurgeWindow(1);
    sourceControlConfig.setPrEventPurgeWindow(2);
    sourceControlConfig.setGitExecutable("some-git-executable-path");
    sourceControlConfig.setGitTimeoutSeconds(3);
    sourceControlConfig.setCommitUsername("some-commit-username");
    sourceControlConfig.setCommitEmail("some-commit-email@d");
    sourceControlConfig.setUseUsernameInRepositoryCloneUrl(true);
    insightConfig.setSourceControl(sourceControlConfig);
    SourceControlConfigurationMigrator.DefaultBranchMonitoringConfig defaultBranchMonitoringConfig =
        new SourceControlConfigurationMigrator.DefaultBranchMonitoringConfig();
    defaultBranchMonitoringConfig.setStartTime("01:11");
    defaultBranchMonitoringConfig.setIntervalInHours(2);
    insightConfig.setDefaultBranchMonitoring(defaultBranchMonitoringConfig);
    insightConfig.setPullRequestMonitoringIntervalInSeconds(60);

    sourceControlConfigurationMigrator.migrate();

    SourceControlConfiguration sourceControlConfiguration = sourceControlConfigurationDAO.get();
    assertThat(sourceControlConfiguration).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .ignoringFields("cloneDirectory", "gitImplementation")
        .isEqualTo(sourceControlConfig);
    assertThat(sourceControlConfiguration.getCloneDirectory()).isEqualTo(
        sourceControlConfig.getCloneDirectory().getAbsolutePath());
    assertThat(sourceControlConfiguration.getGitImplementation()).hasToString(
        sourceControlConfig.getGitImplementation());
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTimeString()).isEqualTo(
        defaultBranchMonitoringConfig.getStartTime());
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringIntervalHours()).isEqualTo(
        defaultBranchMonitoringConfig.getIntervalInHours());
    assertThat(sourceControlConfiguration.getPullRequestMonitoringIntervalSeconds()).isEqualTo(60);
    verify(mockSourceControlConfigurationListener).sourceControlConfigurationChanged();
    assertThat(logOutput).atWarnLevel().contains(SourceControlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_OnlySourceControlConfiguration() {
    SourceControlConfigurationMigrator.SourceControlConfig sourceControlConfig =
        new SourceControlConfigurationMigrator.SourceControlConfig();
    sourceControlConfig.setCloneDirectory("some-clone-directory");
    sourceControlConfig.setSonatypeWorkDir(new File("some-sonatype-work-directory"));
    sourceControlConfig.setGitImplementation("native");
    sourceControlConfig.setPrCommentPurgeWindow(1);
    sourceControlConfig.setPrEventPurgeWindow(2);
    sourceControlConfig.setGitExecutable("some-git-executable-path");
    sourceControlConfig.setGitTimeoutSeconds(3);
    sourceControlConfig.setCommitUsername("some-commit-username");
    sourceControlConfig.setCommitEmail("some-commit-email@d");
    sourceControlConfig.setUseUsernameInRepositoryCloneUrl(true);
    insightConfig.setSourceControl(sourceControlConfig);

    sourceControlConfigurationMigrator.migrate();

    SourceControlConfiguration sourceControlConfiguration = sourceControlConfigurationDAO.get();
    assertThat(sourceControlConfiguration).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .ignoringFields("cloneDirectory", "gitImplementation")
        .isEqualTo(sourceControlConfig);
    assertThat(sourceControlConfiguration.getCloneDirectory()).isEqualTo(
        sourceControlConfig.getCloneDirectory().getAbsolutePath());
    assertThat(sourceControlConfiguration.getGitImplementation()).hasToString(
        sourceControlConfig.getGitImplementation());
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTimeString()).isNull();
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringIntervalHours()).isEqualTo(
        SourceControlConfiguration.DEFAULT_BRANCH_MONITORING_INTERVAL_HOURS);
    verify(mockSourceControlConfigurationListener).sourceControlConfigurationChanged();
    assertThat(logOutput).atWarnLevel().contains(SourceControlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_OnlyDefaultBranchMonitoringConfig() {
    SourceControlConfigurationMigrator.DefaultBranchMonitoringConfig defaultBranchMonitoringConfig =
        new SourceControlConfigurationMigrator.DefaultBranchMonitoringConfig();
    defaultBranchMonitoringConfig.setStartTime("01:11");
    defaultBranchMonitoringConfig.setIntervalInHours(2);
    insightConfig.setDefaultBranchMonitoring(defaultBranchMonitoringConfig);

    sourceControlConfigurationMigrator.migrate();

    SourceControlConfiguration sourceControlConfiguration = sourceControlConfigurationDAO.get();
    assertThat(sourceControlConfiguration).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .ignoringFields("defaultBranchMonitoringStartTimeString", "defaultBranchMonitoringStartTime",
            "defaultBranchMonitoringIntervalHours")
        .isEqualTo(new SourceControlConfiguration());
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTimeString()).isEqualTo(
        defaultBranchMonitoringConfig.getStartTime());
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringIntervalHours()).isEqualTo(
        defaultBranchMonitoringConfig.getIntervalInHours());
    verify(mockSourceControlConfigurationListener).sourceControlConfigurationChanged();
    assertThat(logOutput).atWarnLevel().contains(SourceControlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_OnlyPullRequestMonitoringIntervalInSeconds() {
    insightConfig.setPullRequestMonitoringIntervalInSeconds(500);

    sourceControlConfigurationMigrator.migrate();

    SourceControlConfiguration sourceControlConfiguration = sourceControlConfigurationDAO.get();
    assertThat(sourceControlConfiguration.getPullRequestMonitoringIntervalSeconds()).isEqualTo(500);
    verify(mockSourceControlConfigurationListener).sourceControlConfigurationChanged();
    assertThat(logOutput).atWarnLevel().contains(SourceControlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_PullRequestMonitoringIntervalBelowMinimum() {
    insightConfig.setPullRequestMonitoringIntervalInSeconds(30);

    sourceControlConfigurationMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(SourceControlConfigurationMigrator.MIGRATION_ID)).isTrue();
    SourceControlConfiguration sourceControlConfiguration = sourceControlConfigurationDAO.get();

    assertThat(sourceControlConfiguration).isNotNull();
    assertThat(sourceControlConfiguration.getPullRequestMonitoringIntervalSeconds()).isEqualTo(60);
    verify(mockSourceControlConfigurationListener).sourceControlConfigurationChanged();
    assertThat(logOutput).atWarnLevel().contains(SourceControlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_PullRequestMonitoringIntervalZero() {
    insightConfig.setPullRequestMonitoringIntervalInSeconds(0);

    sourceControlConfigurationMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(SourceControlConfigurationMigrator.MIGRATION_ID)).isTrue();
    SourceControlConfiguration sourceControlConfiguration = sourceControlConfigurationDAO.get();

    assertThat(sourceControlConfiguration).isNotNull();
    assertThat(sourceControlConfiguration.getPullRequestMonitoringIntervalSeconds()).isEqualTo(60);
    verify(mockSourceControlConfigurationListener).sourceControlConfigurationChanged();
    assertThat(logOutput).atWarnLevel().contains(SourceControlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_PullRequestMonitoringIntervalNegative() {
    insightConfig.setPullRequestMonitoringIntervalInSeconds(-10);

    sourceControlConfigurationMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(SourceControlConfigurationMigrator.MIGRATION_ID)).isTrue();
    SourceControlConfiguration sourceControlConfiguration = sourceControlConfigurationDAO.get();

    assertThat(sourceControlConfiguration).isNotNull();
    assertThat(sourceControlConfiguration.getPullRequestMonitoringIntervalSeconds()).isEqualTo(60);
    verify(mockSourceControlConfigurationListener).sourceControlConfigurationChanged();
    assertThat(logOutput).atWarnLevel().contains(SourceControlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_PullRequestMonitoringIntervalJustBelowMinimum() {
    insightConfig.setPullRequestMonitoringIntervalInSeconds(59);

    sourceControlConfigurationMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(SourceControlConfigurationMigrator.MIGRATION_ID)).isTrue();
    SourceControlConfiguration sourceControlConfiguration = sourceControlConfigurationDAO.get();

    assertThat(sourceControlConfiguration).isNotNull();
    assertThat(sourceControlConfiguration.getPullRequestMonitoringIntervalSeconds()).isEqualTo(60);
    verify(mockSourceControlConfigurationListener).sourceControlConfigurationChanged();
    assertThat(logOutput).atWarnLevel().contains(SourceControlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_PullRequestMonitoringIntervalExactlyMinimum() {
    insightConfig.setPullRequestMonitoringIntervalInSeconds(60);

    sourceControlConfigurationMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(SourceControlConfigurationMigrator.MIGRATION_ID)).isTrue();
    SourceControlConfiguration sourceControlConfiguration = sourceControlConfigurationDAO.get();

    assertThat(sourceControlConfiguration).isNotNull();
    assertThat(sourceControlConfiguration.getPullRequestMonitoringIntervalSeconds()).isEqualTo(60);
    verify(mockSourceControlConfigurationListener).sourceControlConfigurationChanged();
    assertThat(logOutput).atWarnLevel().contains(SourceControlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_AlreadyMigrated_DefaultConfig() {
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    migrationTrackerDAO.insert(new MigrationTracker(SourceControlConfigurationMigrator.MIGRATION_ID));

    sourceControlConfigurationMigrator.migrate();

    assertThat(sourceControlConfigurationDAO.get()).usingRecursiveComparison().isEqualTo(sourceControlConfiguration);
    assertThat(logOutput).doesNotContain(SourceControlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_AlreadyMigrated_CustomConfig() {
    SourceControlConfigurationMigrator.SourceControlConfig sourceControlConfig =
        new SourceControlConfigurationMigrator.SourceControlConfig();
    sourceControlConfig.setCloneDirectory("some-clone-directory");
    sourceControlConfig.setSonatypeWorkDir(new File("some-sonatype-work-directory"));
    sourceControlConfig.setGitImplementation("native");
    sourceControlConfig.setPrCommentPurgeWindow(1);
    sourceControlConfig.setPrEventPurgeWindow(2);
    sourceControlConfig.setGitExecutable("some-git-executable-path");
    sourceControlConfig.setGitTimeoutSeconds(3);
    sourceControlConfig.setCommitUsername("some-commit-username");
    sourceControlConfig.setCommitEmail("some-commit-email@d");
    sourceControlConfig.setUseUsernameInRepositoryCloneUrl(true);
    insightConfig.setSourceControl(sourceControlConfig);
    migrationTrackerDAO.insert(new MigrationTracker(SourceControlConfigurationMigrator.MIGRATION_ID));
    SourceControlConfigurationMigrator.DefaultBranchMonitoringConfig defaultBranchMonitoringConfig =
        new SourceControlConfigurationMigrator.DefaultBranchMonitoringConfig();
    defaultBranchMonitoringConfig.setStartTime("01:11");
    defaultBranchMonitoringConfig.setIntervalInHours(2);
    insightConfig.setDefaultBranchMonitoring(defaultBranchMonitoringConfig);
    insightConfig.setPullRequestMonitoringIntervalInSeconds(1);

    sourceControlConfigurationMigrator.migrate();

    assertThat(sourceControlConfigurationDAO.get()).isNull();
    assertThat(logOutput).atWarnLevel().contains(SourceControlConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testGetCloneDirectory() {
    File sonatypeWork = new File("sonatype-work");

    SourceControlConfigurationMigrator.SourceControlConfig sourceControlConfig =
        new SourceControlConfigurationMigrator.SourceControlConfig();
    sourceControlConfig.setSonatypeWorkDir(sonatypeWork);

    assertThat(sourceControlConfig.getCloneDirectory())
        .isEqualTo(new File(sonatypeWork, SourceControlConfiguration.DEFAULT_SOURCE_CONTROL_CLONE_DIR));

    sourceControlConfig.setCloneDirectory("");
    assertThat(sourceControlConfig.getCloneDirectory())
        .isEqualTo(new File(sonatypeWork, SourceControlConfiguration.DEFAULT_SOURCE_CONTROL_CLONE_DIR));

    String relativePath = "abc";
    assertThat(new File(relativePath)).isRelative();
    sourceControlConfig.setCloneDirectory(relativePath);
    assertThat(sourceControlConfig.getCloneDirectory()).isEqualTo(new File(sonatypeWork, relativePath));

    String absolutePath = new File("abc").getAbsolutePath();
    sourceControlConfig.setCloneDirectory(absolutePath);
    assertThat(sourceControlConfig.getCloneDirectory()).isEqualTo(new File(absolutePath));
  }
}
