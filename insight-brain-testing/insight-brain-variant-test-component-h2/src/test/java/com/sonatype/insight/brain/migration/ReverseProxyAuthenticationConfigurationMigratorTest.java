/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.api.v2.service.ReverseProxyAuthenticationConfigurationListener;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO;
import com.sonatype.insight.brain.migration.ReverseProxyAuthenticationConfigurationMigrator.ReverseProxyAuthenticationConfig;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ReverseProxyAuthenticationConfigurationMigratorTest
    extends AbstractComponentH2Test
{
  public LogOutput logOutput = new LogOutput(ReverseProxyAuthenticationConfigurationMigrator.class);

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private ReverseProxyAuthenticationConfigurationDAO reverseProxyAuthenticationConfigurationDAO;

  @Mock
  private ReverseProxyAuthenticationConfigurationListener mockReverseProxyAuthenticationConfigurationListener;

  @Inject
  private ReverseProxyAuthenticationConfigurationMigrator reverseProxyAuthenticationConfigurationMigrator;

  @BeforeEach
  @AfterEach
  public void clear() {
    migrationTrackerDAO.deleteById(ReverseProxyAuthenticationConfigurationMigrator.MIGRATION_ID);
  }

  @Test
  public void testMigrate_FirstRun_InvalidConfig() {
    ReverseProxyAuthenticationConfig reverseProxyAuthenticationConfig = new ReverseProxyAuthenticationConfig();
    reverseProxyAuthenticationConfig.setUsernameHeader("");
    insightConfig.setReverseProxyAuthentication(reverseProxyAuthenticationConfig);

    reverseProxyAuthenticationConfigurationMigrator.migrate();

    assertThat(
        migrationTrackerDAO.isTrackerPresent(ReverseProxyAuthenticationConfigurationMigrator.MIGRATION_ID)).isTrue();
    assertThat(reverseProxyAuthenticationConfigurationDAO.get()).isNull();
    assertThat(logOutput).atWarnLevel()
        .contains(ReverseProxyAuthenticationConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_DefaultConfig() {
    reverseProxyAuthenticationConfigurationMigrator.migrate();

    assertThat(
        migrationTrackerDAO.isTrackerPresent(ReverseProxyAuthenticationConfigurationMigrator.MIGRATION_ID)).isTrue();
    ReverseProxyAuthenticationConfiguration reverseProxyAuthenticationConfiguration =
        reverseProxyAuthenticationConfigurationDAO.get();
    assertThat(reverseProxyAuthenticationConfiguration.isEnabled()).isFalse();
    assertThat(reverseProxyAuthenticationConfiguration.getUsernameHeader()).isEqualTo(
        ReverseProxyAuthenticationConfiguration.DEFAULT_USERNAME_HEADER);
    assertThat(reverseProxyAuthenticationConfiguration.isCsrfProtectionDisabled()).isFalse();
    assertThat(reverseProxyAuthenticationConfiguration.getLogoutUrl()).isNull();
    verify(mockReverseProxyAuthenticationConfigurationListener).reverseProxyAuthenticationConfigurationChanged();
    assertThat(logOutput).doesNotContain(ReverseProxyAuthenticationConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_Full() {
    ReverseProxyAuthenticationConfig reverseProxyAuthenticationConfig = new ReverseProxyAuthenticationConfig();
    reverseProxyAuthenticationConfig.setEnabled(true);
    reverseProxyAuthenticationConfig.setUsernameHeader("MY_USERNAME_HEADER");
    reverseProxyAuthenticationConfig.setCsrfProtectionDisabled(true);
    reverseProxyAuthenticationConfig.setLogoutUrl(URI.create("myLogoutUrl"));

    testMigrate_FirstRun_CustomConfig(reverseProxyAuthenticationConfig);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_OnlyEnabled() {
    ReverseProxyAuthenticationConfig reverseProxyAuthenticationConfig = new ReverseProxyAuthenticationConfig();
    reverseProxyAuthenticationConfig.setEnabled(true);

    testMigrate_FirstRun_CustomConfig(reverseProxyAuthenticationConfig);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_OnlyUsernameHeader() {
    ReverseProxyAuthenticationConfig reverseProxyAuthenticationConfig = new ReverseProxyAuthenticationConfig();
    reverseProxyAuthenticationConfig.setUsernameHeader("MY_USERNAME_HEADER");

    testMigrate_FirstRun_CustomConfig(reverseProxyAuthenticationConfig);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_OnlyCsrfProtectionDisabled() {
    ReverseProxyAuthenticationConfig reverseProxyAuthenticationConfig = new ReverseProxyAuthenticationConfig();
    reverseProxyAuthenticationConfig.setCsrfProtectionDisabled(true);

    testMigrate_FirstRun_CustomConfig(reverseProxyAuthenticationConfig);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_OnlyLogoutUrl() {
    ReverseProxyAuthenticationConfig reverseProxyAuthenticationConfig = new ReverseProxyAuthenticationConfig();
    reverseProxyAuthenticationConfig.setLogoutUrl(URI.create("myLogoutUrl"));

    testMigrate_FirstRun_CustomConfig(reverseProxyAuthenticationConfig);
  }

  private void testMigrate_FirstRun_CustomConfig(ReverseProxyAuthenticationConfig reverseProxyAuthenticationConfig) {
    insightConfig.setReverseProxyAuthentication(reverseProxyAuthenticationConfig);

    reverseProxyAuthenticationConfigurationMigrator.migrate();

    assertThat(
        migrationTrackerDAO.isTrackerPresent(ReverseProxyAuthenticationConfigurationMigrator.MIGRATION_ID)).isTrue();
    ReverseProxyAuthenticationConfiguration reverseProxyAuthenticationConfiguration =
        reverseProxyAuthenticationConfigurationDAO.get();
    assertThat(reverseProxyAuthenticationConfiguration.isEnabled()).isEqualTo(
        reverseProxyAuthenticationConfig.isEnabled());
    assertThat(reverseProxyAuthenticationConfiguration.getUsernameHeader()).isEqualTo(
        reverseProxyAuthenticationConfig.getUsernameHeader());
    assertThat(reverseProxyAuthenticationConfiguration.isCsrfProtectionDisabled()).isEqualTo(
        reverseProxyAuthenticationConfig.isCsrfProtectionDisabled());
    assertThat(reverseProxyAuthenticationConfiguration.getLogoutUrl()).isEqualTo(
        reverseProxyAuthenticationConfig.getLogoutUrl() == null
            ? null
            : reverseProxyAuthenticationConfig.getLogoutUrl()
                .toString());
    verify(mockReverseProxyAuthenticationConfigurationListener).reverseProxyAuthenticationConfigurationChanged();
    assertThat(logOutput).atWarnLevel()
        .contains(ReverseProxyAuthenticationConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_AlreadyMigrated_DefaultConfig() {
    ReverseProxyAuthenticationConfiguration reverseProxyAuthenticationConfiguration =
        tempEntity.newReverseProxyAuthenticationConfiguration();
    migrationTrackerDAO.insert(new MigrationTracker(ReverseProxyAuthenticationConfigurationMigrator.MIGRATION_ID));

    reverseProxyAuthenticationConfigurationMigrator.migrate();

    assertThat(reverseProxyAuthenticationConfigurationDAO.get()).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(reverseProxyAuthenticationConfiguration);
    assertThat(logOutput).doesNotContain(ReverseProxyAuthenticationConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_AlreadyMigrated_CustomConfig() {
    ReverseProxyAuthenticationConfig reverseProxyAuthenticationConfig = new ReverseProxyAuthenticationConfig();
    reverseProxyAuthenticationConfig.setEnabled(true);
    reverseProxyAuthenticationConfig.setUsernameHeader("MY_USERNAME_HEADER");
    reverseProxyAuthenticationConfig.setCsrfProtectionDisabled(true);
    reverseProxyAuthenticationConfig.setLogoutUrl(URI.create("myLogoutUrl"));
    insightConfig.setReverseProxyAuthentication(reverseProxyAuthenticationConfig);
    ReverseProxyAuthenticationConfiguration reverseProxyAuthenticationConfiguration =
        tempEntity.newReverseProxyAuthenticationConfiguration();
    migrationTrackerDAO.insert(new MigrationTracker(ReverseProxyAuthenticationConfigurationMigrator.MIGRATION_ID));

    reverseProxyAuthenticationConfigurationMigrator.migrate();

    assertThat(reverseProxyAuthenticationConfigurationDAO.get()).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(reverseProxyAuthenticationConfiguration);
    assertThat(logOutput).atWarnLevel()
        .contains(ReverseProxyAuthenticationConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }
}
