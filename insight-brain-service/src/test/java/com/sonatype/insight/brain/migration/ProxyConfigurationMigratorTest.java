/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.migration.ProxyConfigurationMigrator.ProxyConfig;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.configuration.ProxyConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.test.LogOutput;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProxyConfigurationMigratorTest
    extends AbstractComponentTest
{
  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private ProxyConfigurationDAO proxyConfigurationDAO;

  @Inject
  private ProxyConfigurationMigrator proxyConfigurationMigrator;

  @Inject
  private InsightConfig insightConfig;

  @Rule
  public LogOutput logOutput = new LogOutput(ProxyConfigurationMigrator.class);

  @Before
  @After
  public void clear() {
    migrationTrackerDAO.deleteById(ProxyConfigurationMigrator.MIGRATION_ID);
    proxyConfigurationDAO.delete();
    SystemConfigurationProperty proxyExcludeHostsProperty =
        new SystemConfigurationPropertyDAO().getByName(ProxyConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME);
    if (proxyExcludeHostsProperty != null) {
      new SystemConfigurationPropertyDAO().delete(proxyExcludeHostsProperty);
    }
  }

  @Test
  public void testMigrate_FirstRun_NoConfig() {
    new SystemConfigurationPropertyDAO()
        .insert(new SystemConfigurationProperty(ProxyConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME, ""));
    insightConfig.setProxyConfig(null);

    proxyConfigurationMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(ProxyConfigurationMigrator.MIGRATION_ID)).isTrue();
    assertThat(proxyConfigurationDAO.get()).isNull();
    assertThat(new SystemConfigurationPropertyDAO().getByName(ProxyConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME))
        .isNull();
    assertThat(logOutput).doesNotContain(ProxyConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_InvalidConfig() {
    new SystemConfigurationPropertyDAO()
        .insert(new SystemConfigurationProperty(ProxyConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME, ""));
    ProxyConfig proxyConfig = new ProxyConfig();
    insightConfig.setProxyConfig(proxyConfig);

    proxyConfigurationMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(ProxyConfigurationMigrator.MIGRATION_ID)).isTrue();
    assertThat(proxyConfigurationDAO.get()).isNull();
    assertThat(new SystemConfigurationPropertyDAO().getByName(ProxyConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME))
        .isNull();
    assertThat(logOutput).atWarnLevel().contains(ProxyConfigurationMigrator.INVALID_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig() {
    new SystemConfigurationPropertyDAO().insert(
        new SystemConfigurationProperty(ProxyConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME, "host1, host2"));
    char[] password = "testpass".toCharArray();
    ProxyConfigurationMigrator.ProxyConfig fileConfig = new ProxyConfigurationMigrator.ProxyConfig();
    fileConfig.setHostname("testhost");
    fileConfig.setPort(12345);
    fileConfig.setUsername("testuser");
    fileConfig.setPassword(password.clone());
    insightConfig.setProxyConfig(fileConfig);

    proxyConfigurationMigrator.migrate();

    assertThat(fileConfig.getPassword()).containsOnly('0');

    ProxyConfiguration dbConfig = proxyConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(fileConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(fileConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo(fileConfig.getUsername());
    assertThat(passwordHandler.decryptPassword(dbConfig.getPassword())).isEqualTo(password);
    assertThat(dbConfig.getExcludeHosts()).isEqualTo("host1, host2");
    assertThat(new SystemConfigurationPropertyDAO().getByName(ProxyConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME))
        .isNull();

    assertThat(logOutput).atWarnLevel().contains(ProxyConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_NoExcludeHosts() {
    new SystemConfigurationPropertyDAO()
        .insert(new SystemConfigurationProperty(ProxyConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME, ""));
    char[] password = "testpass".toCharArray();
    ProxyConfigurationMigrator.ProxyConfig fileConfig = new ProxyConfigurationMigrator.ProxyConfig();
    fileConfig.setHostname("testhost");
    fileConfig.setPort(12345);
    fileConfig.setUsername("testuser");
    fileConfig.setPassword(password.clone());
    insightConfig.setProxyConfig(fileConfig);

    proxyConfigurationMigrator.migrate();

    assertThat(fileConfig.getPassword()).containsOnly('0');

    ProxyConfiguration dbConfig = proxyConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(fileConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(fileConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo(fileConfig.getUsername());
    assertThat(passwordHandler.decryptPassword(dbConfig.getPassword())).isEqualTo(password);
    assertThat(dbConfig.getExcludeHosts()).isNull();
    assertThat(new SystemConfigurationPropertyDAO().getByName(ProxyConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME))
        .isNull();

    assertThat(logOutput).atWarnLevel().contains(ProxyConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_NoPassword() {
    new SystemConfigurationPropertyDAO().insert(
        new SystemConfigurationProperty(ProxyConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME, "host1, host2"));
    ProxyConfigurationMigrator.ProxyConfig fileConfig = new ProxyConfigurationMigrator.ProxyConfig();
    fileConfig.setHostname("testhost");
    fileConfig.setPort(12345);
    fileConfig.setUsername("testuser");
    fileConfig.setPassword(null);
    insightConfig.setProxyConfig(fileConfig);

    proxyConfigurationMigrator.migrate();

    ProxyConfiguration dbConfig = proxyConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(fileConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(fileConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo(fileConfig.getUsername());
    assertThat(dbConfig.getPassword()).isNull();
    assertThat(dbConfig.getExcludeHosts()).isEqualTo("host1, host2");
    assertThat(new SystemConfigurationPropertyDAO().getByName(ProxyConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME))
        .isNull();

    assertThat(logOutput).atWarnLevel().contains(ProxyConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_AlreadyMigrated_CustomConfig() {
    ProxyConfiguration currentConfig = new ProxyConfiguration();
    currentConfig.setHostname("testhost");
    currentConfig.setPort(12345);
    currentConfig.setUsername("testuser");
    currentConfig.setPassword("testpass".toCharArray());
    currentConfig.setExcludeHosts("host1, host2");
    proxyConfigurationDAO.set(currentConfig);
    migrationTrackerDAO.insert(new MigrationTracker(ProxyConfigurationMigrator.MIGRATION_ID));

    ProxyConfigurationMigrator.ProxyConfig fileConfig = new ProxyConfigurationMigrator.ProxyConfig();
    fileConfig.setHostname("ignored");
    fileConfig.setPort(0);
    fileConfig.setUsername("ignored");
    fileConfig.setPassword("ignored".toCharArray());
    insightConfig.setProxyConfig(fileConfig);

    proxyConfigurationMigrator.migrate();

    ProxyConfiguration dbConfig = proxyConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(currentConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(currentConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo(currentConfig.getUsername());
    assertThat(dbConfig.getPassword()).isEqualTo(currentConfig.getPassword());
    assertThat(dbConfig.getExcludeHosts()).isEqualTo(currentConfig.getExcludeHosts());

    assertThat(logOutput).atWarnLevel().contains(ProxyConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }

  @Test
  public void testMigrate_AlreadyMigrated_DefaultConfig() {
    ProxyConfiguration currentConfig = new ProxyConfiguration();
    currentConfig.setHostname("testhost");
    currentConfig.setPort(12345);
    currentConfig.setUsername("testuser");
    currentConfig.setPassword("testpass".toCharArray());
    proxyConfigurationDAO.set(currentConfig);
    migrationTrackerDAO.insert(new MigrationTracker(ProxyConfigurationMigrator.MIGRATION_ID));

    proxyConfigurationMigrator.migrate();

    ProxyConfiguration dbConfig = proxyConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(currentConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(currentConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo(currentConfig.getUsername());
    assertThat(dbConfig.getPassword()).isEqualTo(currentConfig.getPassword());
    assertThat(dbConfig.getExcludeHosts()).isNull();

    assertThat(logOutput).doesNotContain(ProxyConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
  }
}
