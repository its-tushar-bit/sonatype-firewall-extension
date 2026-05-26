/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.api.v2.service.JiraConfigurationListener;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.jira.JiraConfigurationDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import org.assertj.core.util.Maps;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

public class JiraConfigurationMigratorTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(JiraConfigurationMigrator.class);

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private JiraConfigurationDAO jiraConfigurationDAO;

  @Inject
  private PasswordHandler passwordHandler;

  @Mock
  private JiraConfigurationListener mockJiraConfigurationListener;

  @Inject
  private JiraConfigurationMigrator jiraConfigurationMigrator;

  @Before
  @After
  public void clear() {
    migrationTrackerDAO.deleteById(JiraConfigurationMigrator.MIGRATION_ID);
  }

  @Test
  public void testMigrate_FirstRun_InvalidConfig() {
    JiraConfigurationMigrator.JiraConfig jiraConfig = new JiraConfigurationMigrator.JiraConfig();
    insightConfig.setJiraConfig(jiraConfig);

    jiraConfigurationMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(JiraConfigurationMigrator.MIGRATION_ID)).isTrue();
    assertThat(jiraConfigurationDAO.get()).isNull();
    assertThat(logOutput).atWarnLevel().contains(JiraConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_DefaultConfig() {
    jiraConfigurationMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(JiraConfigurationMigrator.MIGRATION_ID)).isTrue();
    assertThat(jiraConfigurationDAO.get()).isNull();
    verify(mockJiraConfigurationListener).jiraConfigurationChanged();
    assertThat(logOutput).doesNotContain(JiraConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_Full() {
    JiraConfigurationMigrator.JiraConfig jiraConfig = new JiraConfigurationMigrator.JiraConfig();
    jiraConfig.setUrl("http://url");
    jiraConfig.setUsername("username");
    jiraConfig.setPassword("password".toCharArray());
    jiraConfig.setCustomFields(Maps.newHashMap("field", "value"));
    insightConfig.setJiraConfig(jiraConfig);

    jiraConfigurationMigrator.migrate();

    JiraConfiguration jiraConfiguration = jiraConfigurationDAO.get();
    assertThat(jiraConfiguration).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .ignoringFields("password")
        .isEqualTo(jiraConfig);
    assertThat(passwordHandler.decryptPassword(jiraConfiguration.getPassword())).isEqualTo(jiraConfig.getPassword());
    verify(mockJiraConfigurationListener).jiraConfigurationChanged();
    assertThat(logOutput).atWarnLevel().contains(JiraConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_AlreadyMigrated_DefaultConfig() {
    JiraConfiguration jiraConfiguration = tempEntity.newJiraConfiguration();
    migrationTrackerDAO.insert(new MigrationTracker(JiraConfigurationMigrator.MIGRATION_ID));

    jiraConfigurationMigrator.migrate();

    assertThat(jiraConfigurationDAO.get()).usingRecursiveComparison().isEqualTo(jiraConfiguration);
    assertThat(logOutput).doesNotContain(JiraConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_AlreadyMigrated_CustomConfig() {
    JiraConfigurationMigrator.JiraConfig jiraConfig = new JiraConfigurationMigrator.JiraConfig();
    jiraConfig.setUrl("http://url");
    jiraConfig.setUsername("username");
    jiraConfig.setPassword("password".toCharArray());
    jiraConfig.setCustomFields(Maps.newHashMap("field", "value"));
    insightConfig.setJiraConfig(jiraConfig);
    migrationTrackerDAO.insert(new MigrationTracker(JiraConfigurationMigrator.MIGRATION_ID));

    jiraConfigurationMigrator.migrate();

    assertThat(jiraConfigurationDAO.get()).isNull();
    assertThat(logOutput).atWarnLevel().contains(JiraConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }
}
