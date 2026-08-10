/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sonatype.insight.brain.api.v2.service.ProxyServerConfigurationListener;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.migration.ProxyServerConfigurationMigrator.ProxyConfig;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ProxyServerConfigurationMigratorTest
    extends AbstractComponentH2Test
{
  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  @Inject
  private ProxyServerConfigurationMigrator proxyServerConfigurationMigrator;

  @Inject
  private InsightConfig insightConfig;

  @Rule
  public LogOutput logOutput = new LogOutput(ProxyServerConfigurationMigrator.class);

  @Mock
  private ProxyServerConfigurationListener proxyServerConfigurationListener;

  @BeforeEach
  public void clear() {
    migrationTrackerDAO.deleteById(ProxyServerConfigurationMigrator.MIGRATION_ID);
  }

  @Test
  public void testMigrate_FirstRun_NoConfig() {
    systemConfigurationPropertyDAO
        .insert(new SystemConfigurationProperty(ProxyServerConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME, ""));
    insightConfig.setProxyConfig(null);

    proxyServerConfigurationMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(ProxyServerConfigurationMigrator.MIGRATION_ID)).isTrue();
    assertThat(proxyServerConfigurationDAO.get()).isNull();
    assertThat(
        systemConfigurationPropertyDAO.getByName(ProxyServerConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME))
            .isNull();
    assertThat(logOutput).doesNotContain(ProxyServerConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
    verifyNoInteractions(proxyServerConfigurationListener);
  }

  @Test
  public void testMigrate_FirstRun_InvalidConfig() {
    systemConfigurationPropertyDAO
        .insert(new SystemConfigurationProperty(ProxyServerConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME, ""));
    ProxyConfig proxyConfig = new ProxyConfig();
    insightConfig.setProxyConfig(proxyConfig);

    proxyServerConfigurationMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(ProxyServerConfigurationMigrator.MIGRATION_ID)).isTrue();
    assertThat(proxyServerConfigurationDAO.get()).isNull();
    assertThat(
        systemConfigurationPropertyDAO.getByName(ProxyServerConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME))
            .isNull();
    assertThat(logOutput).atWarnLevel().contains(ProxyServerConfigurationMigrator.INVALID_CONFIG_MESSAGE);
    verifyNoInteractions(proxyServerConfigurationListener);
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig() {
    systemConfigurationPropertyDAO.insert(new SystemConfigurationProperty(
        ProxyServerConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME, "host1, host2"));
    char[] password = "testpass".toCharArray();
    ProxyServerConfigurationMigrator.ProxyConfig fileConfig = new ProxyServerConfigurationMigrator.ProxyConfig();
    fileConfig.setHostname("testhost");
    fileConfig.setPort(12345);
    fileConfig.setUsername("testuser");
    fileConfig.setPassword(password.clone());
    insightConfig.setProxyConfig(fileConfig);

    proxyServerConfigurationMigrator.migrate();

    assertThat(fileConfig.getPassword()).containsOnly('0');

    ProxyServerConfiguration dbConfig = proxyServerConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(fileConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(fileConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo(fileConfig.getUsername());
    assertThat(passwordHandler.decryptPassword(dbConfig.getPassword())).isEqualTo(password);
    assertThat(dbConfig.getExcludeHosts()).isEqualTo("host1, host2");
    assertThat(
        systemConfigurationPropertyDAO.getByName(ProxyServerConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME))
            .isNull();

    assertThat(logOutput).atWarnLevel().contains(ProxyServerConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_NoExcludeHosts() {
    systemConfigurationPropertyDAO
        .insert(new SystemConfigurationProperty(ProxyServerConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME, ""));
    char[] password = "testpass".toCharArray();
    ProxyServerConfigurationMigrator.ProxyConfig fileConfig = new ProxyServerConfigurationMigrator.ProxyConfig();
    fileConfig.setHostname("testhost");
    fileConfig.setPort(12345);
    fileConfig.setUsername("testuser");
    fileConfig.setPassword(password.clone());
    insightConfig.setProxyConfig(fileConfig);

    proxyServerConfigurationMigrator.migrate();

    assertThat(fileConfig.getPassword()).containsOnly('0');

    ProxyServerConfiguration dbConfig = proxyServerConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(fileConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(fileConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo(fileConfig.getUsername());
    assertThat(passwordHandler.decryptPassword(dbConfig.getPassword())).isEqualTo(password);
    assertThat(dbConfig.getExcludeHosts()).isNull();
    assertThat(
        systemConfigurationPropertyDAO.getByName(ProxyServerConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME))
            .isNull();

    assertThat(logOutput).atWarnLevel().contains(ProxyServerConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  @Test
  public void testMigrate_FirstRun_CustomConfig_NoPassword() {
    systemConfigurationPropertyDAO.insert(new SystemConfigurationProperty(
        ProxyServerConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME, "host1, host2"));
    ProxyServerConfigurationMigrator.ProxyConfig fileConfig = new ProxyServerConfigurationMigrator.ProxyConfig();
    fileConfig.setHostname("testhost");
    fileConfig.setPort(12345);
    fileConfig.setUsername("testuser");
    fileConfig.setPassword(null);
    insightConfig.setProxyConfig(fileConfig);

    proxyServerConfigurationMigrator.migrate();

    ProxyServerConfiguration dbConfig = proxyServerConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(fileConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(fileConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo(fileConfig.getUsername());
    assertThat(dbConfig.getPassword()).isNull();
    assertThat(dbConfig.getExcludeHosts()).isEqualTo("host1, host2");
    assertThat(
        systemConfigurationPropertyDAO.getByName(ProxyServerConfigurationMigrator.PROXY_EXCLUDE_HOSTS_PROP_NAME))
            .isNull();

    assertThat(logOutput).atWarnLevel().contains(ProxyServerConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }

  @Test
  public void testMigrate_AlreadyMigrated_CustomConfig() {
    ProxyServerConfiguration currentConfig = new ProxyServerConfiguration();
    currentConfig.setHostname("testhost");
    currentConfig.setPort(12345);
    currentConfig.setUsername("testuser");
    currentConfig.setPassword("testpass".toCharArray());
    currentConfig.setExcludeHosts("host1, host2");
    proxyServerConfigurationDAO.set(currentConfig);
    migrationTrackerDAO.insert(new MigrationTracker(ProxyServerConfigurationMigrator.MIGRATION_ID));

    ProxyServerConfigurationMigrator.ProxyConfig fileConfig = new ProxyServerConfigurationMigrator.ProxyConfig();
    fileConfig.setHostname("ignored");
    fileConfig.setPort(0);
    fileConfig.setUsername("ignored");
    fileConfig.setPassword("ignored".toCharArray());
    insightConfig.setProxyConfig(fileConfig);

    proxyServerConfigurationMigrator.migrate();

    ProxyServerConfiguration dbConfig = proxyServerConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(currentConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(currentConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo(currentConfig.getUsername());
    assertThat(dbConfig.getPassword()).isEqualTo(currentConfig.getPassword());
    assertThat(dbConfig.getExcludeHosts()).isEqualTo(currentConfig.getExcludeHosts());

    assertThat(logOutput).atWarnLevel().contains(ProxyServerConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
    verifyNoInteractions(proxyServerConfigurationListener);
  }

  @Test
  public void testMigrate_AlreadyMigrated_DefaultConfig() {
    ProxyServerConfiguration currentConfig = new ProxyServerConfiguration();
    currentConfig.setHostname("testhost");
    currentConfig.setPort(12345);
    currentConfig.setUsername("testuser");
    currentConfig.setPassword("testpass".toCharArray());
    proxyServerConfigurationDAO.set(currentConfig);
    migrationTrackerDAO.insert(new MigrationTracker(ProxyServerConfigurationMigrator.MIGRATION_ID));

    proxyServerConfigurationMigrator.migrate();

    ProxyServerConfiguration dbConfig = proxyServerConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo(currentConfig.getHostname());
    assertThat(dbConfig.getPort()).isEqualTo(currentConfig.getPort());
    assertThat(dbConfig.getUsername()).isEqualTo(currentConfig.getUsername());
    assertThat(dbConfig.getPassword()).isEqualTo(currentConfig.getPassword());
    assertThat(dbConfig.getExcludeHosts()).isNull();

    assertThat(logOutput).doesNotContain(ProxyServerConfigurationMigrator.OBSOLETE_CONFIG_MESSAGE);
    verifyNoInteractions(proxyServerConfigurationListener);
  }

  @Test
  public void testMigrate_FirstRun_EmptyUsername_EmptyPassword() {
    ProxyServerConfigurationMigrator.ProxyConfig fileConfig = new ProxyServerConfigurationMigrator.ProxyConfig();
    fileConfig.setHostname("host");
    fileConfig.setPort(12345);
    fileConfig.setUsername("");
    fileConfig.setPassword("".toCharArray());
    insightConfig.setProxyConfig(fileConfig);

    proxyServerConfigurationMigrator.migrate();

    ProxyServerConfiguration dbConfig = proxyServerConfigurationDAO.get();
    assertThat(dbConfig).isNotNull();
    assertThat(dbConfig.getHostname()).isEqualTo("host");
    assertThat(dbConfig.getPort()).isEqualTo(12345);
    assertThat(dbConfig.getUsername()).isEqualTo("");
    assertThat(String.valueOf(passwordHandler.decryptPassword(dbConfig.getPassword()))).isEqualTo("");
    assertThat(dbConfig.getExcludeHosts()).isNull();

    verify(proxyServerConfigurationListener).proxyServerConfigurationChanged();
  }
}
