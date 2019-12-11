/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MailConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MailConfigurationMigratorTest
    extends AbstractComponentTest
{
  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private MailConfigurationDAO mailConfigurationDAO;

  @Inject
  private MailConfigurationMigrator mailConfigurationMigrator;

  @Inject
  private InsightConfig insightConfig;

  @Before
  @After
  public void clear() {
    migrationTrackerDAO.deleteById(MailConfigurationMigrator.MIGRATION_ID);
    mailConfigurationDAO.delete();
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
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig() {
    MailConfig fileConfig = new MailConfig();
    fileConfig.setHostname("testhost");
    fileConfig.setPort(12345);
    fileConfig.setUsername("testuser");
    fileConfig.setPassword("testpass");
    fileConfig.setSsl(true);
    fileConfig.setSystemEmail("nxiq@localhost");
    insightConfig.setMailConfig(fileConfig);

    mailConfigurationMigrator.migrate();

    MailConfiguration dbConfig = mailConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(fileConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(fileConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo(fileConfig.getUsername());
    assertThat(dbConfig.getPassword()).isEqualTo(fileConfig.getPassword());
    assertThat(dbConfig.isSslEnabled()).isEqualTo(fileConfig.isSsl());
    assertThat(dbConfig.isStartTlsEnabled()).isEqualTo(fileConfig.isTls());
    assertThat(dbConfig.getSystemEmail()).isEqualTo(fileConfig.getSystemEmail());
  }

  @Test
  public void testMigrate_AlreadyMigrated() {
    MailConfiguration currentConfig = new MailConfiguration();
    currentConfig.setHostname("testhost");
    currentConfig.setPort(12345);
    currentConfig.setUsername("testuser");
    currentConfig.setPassword("testpass");
    currentConfig.setSslEnabled(true);
    currentConfig.setSystemEmail("nxiq@localhost");
    mailConfigurationDAO.set(currentConfig);
    migrationTrackerDAO.insert(new MigrationTracker(MailConfigurationMigrator.MIGRATION_ID));

    MailConfig fileConfig = new MailConfig();
    fileConfig.setHostname("ignored");
    fileConfig.setPort(0);
    fileConfig.setUsername("ignored");
    fileConfig.setPassword("ignored");
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
  }
}
