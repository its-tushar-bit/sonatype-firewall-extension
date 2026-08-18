/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightMail;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class MailConfigurationMigratorTest
    extends AbstractComponentH2Test
{
  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private MailConfigurationDAO mailConfigurationDAO;

  @Inject
  private MailConfigurationMigrator mailConfigurationMigrator;

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private InsightMail insightMail;

  public LogOutput logOutput = new LogOutput(MailConfigurationMigrator.class);

  @BeforeEach
  @AfterEach
  public void clear() {
    migrationTrackerDAO.deleteById(MailConfigurationMigrator.MIGRATION_ID);
    mailConfigurationDAO.delete();
  }

  @Test
  public void testMigrate_FirstRun_InvalidConfig() {
    MailConfigurationMigrator.MailConfig fileConfig = new MailConfigurationMigrator.MailConfig();
    fileConfig.setHostname(null);
    insightConfig.setMailConfig(fileConfig);

    mailConfigurationMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(MailConfigurationMigrator.MIGRATION_ID)).isTrue();
    assertThat(mailConfigurationDAO.get()).isNull();
  }

  @Test
  public void testMigrate_FirstRun_DefaultConfig() {
    mailConfigurationMigrator.migrate();

    MailConfiguration dbConfig = mailConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo("127.0.0.1");
    assertThat(dbConfig.getPort()).isEqualTo(587);
    assertThat(dbConfig.getUsername()).isNull();
    assertThat(dbConfig.getPassword()).isNull();
    assertThat(dbConfig.isSslEnabled()).isFalse();
    assertThat(dbConfig.isStartTlsEnabled()).isFalse();
    assertThat(dbConfig.getSystemEmail()).isEqualTo("NexusIQServer@localhost");

    assertThat(logOutput).doesNotContain(MailConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig() {
    char[] password = "testpass".toCharArray();
    MailConfigurationMigrator.MailConfig fileConfig = new MailConfigurationMigrator.MailConfig();
    fileConfig.setHostname("testhost");
    fileConfig.setPort(12345);
    fileConfig.setUsername("testuser");
    fileConfig.setPassword(password.clone());
    fileConfig.setSsl(true);
    fileConfig.setSystemEmail("nxiq@localhost");
    insightConfig.setMailConfig(fileConfig);

    mailConfigurationMigrator.migrate();

    assertThat(fileConfig.getPassword()).containsOnly('0');

    MailConfiguration dbConfig = mailConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(fileConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(fileConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo(fileConfig.getUsername());
    assertThat(insightMail.decryptPassword(dbConfig.getPassword())).isEqualTo(password);
    assertThat(dbConfig.isSslEnabled()).isEqualTo(fileConfig.isSsl());
    assertThat(dbConfig.isStartTlsEnabled()).isEqualTo(fileConfig.isTls());
    assertThat(dbConfig.getSystemEmail()).isEqualTo(fileConfig.getSystemEmail());

    assertThat(logOutput).atWarnLevel().contains(MailConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_NoPassword() {
    MailConfigurationMigrator.MailConfig fileConfig = new MailConfigurationMigrator.MailConfig();
    fileConfig.setHostname("testhost");
    fileConfig.setPort(12345);
    fileConfig.setUsername("testuser");
    fileConfig.setPassword(null);
    fileConfig.setSsl(true);
    fileConfig.setSystemEmail("nxiq@localhost");
    insightConfig.setMailConfig(fileConfig);

    mailConfigurationMigrator.migrate();

    MailConfiguration dbConfig = mailConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(fileConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(fileConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo(fileConfig.getUsername());
    assertThat(dbConfig.getPassword()).isNull();
    assertThat(dbConfig.isSslEnabled()).isEqualTo(fileConfig.isSsl());
    assertThat(dbConfig.isStartTlsEnabled()).isEqualTo(fileConfig.isTls());
    assertThat(dbConfig.getSystemEmail()).isEqualTo(fileConfig.getSystemEmail());

    assertThat(logOutput).atWarnLevel().contains(MailConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_AlreadyMigrated_CustomConfig() {
    MailConfiguration currentConfig = new MailConfiguration();
    currentConfig.setHostname("testhost");
    currentConfig.setPort(12345);
    currentConfig.setUsername("testuser");
    currentConfig.setPassword("testpass".toCharArray());
    currentConfig.setSslEnabled(true);
    currentConfig.setSystemEmail("nxiq@localhost");
    mailConfigurationDAO.set(currentConfig);
    migrationTrackerDAO.insert(new MigrationTracker(MailConfigurationMigrator.MIGRATION_ID));

    MailConfigurationMigrator.MailConfig fileConfig = new MailConfigurationMigrator.MailConfig();
    fileConfig.setHostname("ignored");
    fileConfig.setPort(0);
    fileConfig.setUsername("ignored");
    fileConfig.setPassword("ignored".toCharArray());
    fileConfig.setSsl(false);
    fileConfig.setTls(true);
    fileConfig.setSystemEmail("ignored");
    insightConfig.setMailConfig(fileConfig);

    mailConfigurationMigrator.migrate();

    MailConfiguration dbConfig = mailConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(currentConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(currentConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo(currentConfig.getUsername());
    assertThat(dbConfig.getPassword()).isEqualTo(currentConfig.getPassword());
    assertThat(dbConfig.isSslEnabled()).isEqualTo(currentConfig.isSslEnabled());
    assertThat(dbConfig.isStartTlsEnabled()).isEqualTo(currentConfig.isStartTlsEnabled());
    assertThat(dbConfig.getSystemEmail()).isEqualTo(currentConfig.getSystemEmail());

    assertThat(logOutput).atWarnLevel().contains(MailConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_AlreadyMigrated_DefaultConfig() {
    MailConfiguration currentConfig = new MailConfiguration();
    currentConfig.setHostname("testhost");
    currentConfig.setPort(12345);
    currentConfig.setUsername("testuser");
    currentConfig.setPassword("testpass".toCharArray());
    currentConfig.setSslEnabled(true);
    currentConfig.setSystemEmail("nxiq@localhost");
    mailConfigurationDAO.set(currentConfig);
    migrationTrackerDAO.insert(new MigrationTracker(MailConfigurationMigrator.MIGRATION_ID));

    mailConfigurationMigrator.migrate();

    MailConfiguration dbConfig = mailConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(currentConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(currentConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo(currentConfig.getUsername());
    assertThat(dbConfig.getPassword()).isEqualTo(currentConfig.getPassword());
    assertThat(dbConfig.isSslEnabled()).isEqualTo(currentConfig.isSslEnabled());
    assertThat(dbConfig.isStartTlsEnabled()).isEqualTo(currentConfig.isStartTlsEnabled());
    assertThat(dbConfig.getSystemEmail()).isEqualTo(currentConfig.getSystemEmail());

    assertThat(logOutput).doesNotContain(MailConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_EmptyUsername_EmptyPassword() {
    MailConfigurationMigrator.MailConfig fileConfig = new MailConfigurationMigrator.MailConfig();
    fileConfig.setHostname("testhost");
    fileConfig.setPort(12345);
    fileConfig.setUsername("");
    fileConfig.setPassword("".toCharArray());
    fileConfig.setSystemEmail("system@email");
    insightConfig.setMailConfig(fileConfig);

    mailConfigurationMigrator.migrate();

    MailConfiguration dbConfig = mailConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(fileConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(fileConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo("");
    assertThat(String.valueOf(insightMail.decryptPassword(dbConfig.getPassword()))).isEqualTo("");
    assertThat(dbConfig.isSslEnabled()).isFalse();
    assertThat(dbConfig.isStartTlsEnabled()).isFalse();
    assertThat(dbConfig.getSystemEmail()).isEqualTo("system@email");

    assertThat(logOutput).atWarnLevel().contains(MailConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }
}
