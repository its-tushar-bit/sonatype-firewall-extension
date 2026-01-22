/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.eventbus.EventBusConfig;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.service.SupportConfig;
import com.sonatype.insight.test.LogOutput;

import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.matcher.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class SimpleConfigurationMigratorTest
    extends AbstractComponentTest
{
  private static final String DISABLE_CONAN_NAMESPACE_MATCHING = "disableConanNamespaceMatching";

  private static final String ENABLE_CPE_DATA_MATCHING = "enableCpeDataMatching";

  private static final String EXPECTED_OBSOLETE_SUFFIX = " is now configured using the REST API. " +
      "The configuration in the config.yml or via system properties is obsolete.";

  @Rule
  public LogOutput logOutput = new LogOutput(SimpleConfigurationMigrator.class);

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private SimpleConfigurationMigrator simpleConfigurationMigrator;

  @Inject
  private ApiConfigurationService configurationService;

  @Mock
  private ApiConfigurationService mockConfigurationService;

  @Mock
  private ApiConfigFeaturesService mockConfigFeaturesService;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Override
  public void configure(Binder binder) {
    binder.bindInterceptor(Matchers.subclassesOf(ApiConfigurationService.class), Matchers.any(), invocation -> {
      if (invocation.getMethod().getModifiers() == Modifier.PUBLIC) {
        invocation.getMethod().invoke(mockConfigurationService, invocation.getArguments());
      }
      return invocation.proceed();
    });
    binder.bindInterceptor(Matchers.subclassesOf(ApiConfigFeaturesService.class), Matchers.any(), invocation -> {
      if (invocation.getMethod().getModifiers() == Modifier.PUBLIC) {
        invocation.getMethod().invoke(mockConfigFeaturesService, invocation.getArguments());
      }
      return invocation.proceed();
    });
    super.configure(binder);
  }

  @Before
  @After
  public void clear() {
    reset(mockConfigurationService);
    migrationTrackerDAO.deleteById(SimpleConfigurationMigrator.MIGRATION_ID);
  }

  @Test
  public void testMigrate_AlreadyMigrated() {
    setAll();
    reset(mockConfigurationService);
    migrationTrackerDAO.insertTracker(SimpleConfigurationMigrator.MIGRATION_ID);

    simpleConfigurationMigrator.migrate();

    String allPropertyNames = String.join(", ", SimpleConfigurationMigrator.NAME_TO_GETTER.keySet());
    logOutput.assertThat().atWarnLevel().contains(allPropertyNames + EXPECTED_OBSOLETE_SUFFIX);
    logOutput.assertThat().atDebugLevel().contains(allPropertyNames + " configuration already migrated.");
    verifyNoInteractions(mockConfigurationService);
    verifyNoInteractions(mockConfigFeaturesService);
  }

  @Test
  public void testMigrate_AllNull() {
    simpleConfigurationMigrator.migrate();

    logOutput.assertThat().doesNotContain(EXPECTED_OBSOLETE_SUFFIX);
    logOutput.assertThat().doesNotContain("configuration already migrated.");
    verifyNoInteractions(mockConfigurationService);
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_All() {
    setAll();

    simpleConfigurationMigrator.migrate();

    String allPropertyNames = String.join(", ", SimpleConfigurationMigrator.NAME_TO_GETTER.keySet());
    logOutput.assertThat().atWarnLevel().contains(allPropertyNames + EXPECTED_OBSOLETE_SUFFIX);
    logOutput.assertThat().doesNotContain("configuration already migrated.");
    Set<String> expectedApplyConfigurationToClients =
        new HashSet<>(SimpleConfigurationMigrator.NAME_TO_GETTER.keySet());
    expectedApplyConfigurationToClients.removeAll(SimpleConfigurationMigrator.FEATURE_FLAGS);
    verify(mockConfigurationService).applyConfigurationToClients(expectedApplyConfigurationToClients);
    Arrays.stream(Feature.values()).forEach(f -> {
      verify(mockConfigFeaturesService).disableFeatureNoAuthz(f.getFlag());
      assertThat(
          Arrays.stream(SystemConfigurationPropertyFeature.values()).filter(p -> p.name().equals(f.name())).findFirst()
              .orElseThrow(RuntimeException::new).isEnabled()).isFalse();
    });
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Independent() {
    insightConfig.setHdsUrl("badUrl");
    insightConfig.setCdnUrl("http://cdnUrl/");

    simpleConfigurationMigrator.migrate();

    logOutput.assertThat().atWarnLevel()
        .contains("The current hdsUrl configuration is invalid and cannot be migrated.");
    assertThat(configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.HDS_URL)).isEqualTo(
        "https://clm-staging.sonatype.com/");
    assertThat(configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.CDN_URL)).isEqualTo(
        insightConfig.getCdnUrl());
    verify(mockConfigurationService).setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.HDS_URL,
        insightConfig.getHdsUrl());
    verify(mockConfigurationService).setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.CDN_URL,
        insightConfig.getCdnUrl());
    verify(mockConfigurationService).applyConfigurationToClients(Sets.newHashSet(SystemConfigurationProperty.CDN_URL));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_HdsUrl() {
    insightConfig.setHdsUrl("http://hdsUrl/");

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.HDS_URL)).isEqualTo(
        insightConfig.getHdsUrl());
    logOutput.assertThat().atWarnLevel().contains(SystemConfigurationProperty.HDS_URL + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(Sets.newHashSet(SystemConfigurationProperty.HDS_URL));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_CdnUrl() {
    insightConfig.setCdnUrl("http://cdnUrl/");

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.CDN_URL)).isEqualTo(
        insightConfig.getCdnUrl());
    logOutput.assertThat().atWarnLevel().contains(SystemConfigurationProperty.CDN_URL + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(Sets.newHashSet(SystemConfigurationProperty.CDN_URL));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_SupportReadLimitBytes() {
    SupportConfig supportConfig = new SupportConfig();
    supportConfig.setReadLimitBytes(supportConfig.getReadLimitBytes() + 1);
    insightConfig.setSupportConfig(supportConfig);

    simpleConfigurationMigrator.migrate();

    assertThat(
        configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES)).isEqualTo(
        insightConfig.getSupportConfig().getReadLimitBytes());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_EventBusMaxThreadPoolSize() {
    EventBusConfig eventBusConfig = new EventBusConfig();
    eventBusConfig.setMaxPoolSize(eventBusConfig.getMaxPoolSize() + 1);
    insightConfig.setEventBusConfig(eventBusConfig);

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE)).isEqualTo(
        insightConfig.getEventBusConfig().getMaxPoolSize());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_CsrfProtection() {
    insightConfig.setCsrfProtection(
        !(boolean) configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.CSRF_PROTECTION));

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.CSRF_PROTECTION)).isEqualTo(
        insightConfig.isCsrfProtection());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.CSRF_PROTECTION + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.CSRF_PROTECTION));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_UserAgentSuffix() {
    insightConfig.setUserAgentSuffix("userAgentSuffix");

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.USER_AGENT_SUFFIX)).isEqualTo(
        insightConfig.getUserAgentSuffix());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.USER_AGENT_SUFFIX + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.USER_AGENT_SUFFIX));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_CspEnabled() {
    insightConfig.setCspEnabled(
        !(boolean) configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.CSP_ENABLED));

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.CSP_ENABLED)).isEqualTo(
        insightConfig.isCspEnabled());
    logOutput.assertThat().atWarnLevel().contains(SystemConfigurationProperty.CSP_ENABLED + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.CSP_ENABLED));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_BlockSemicolonInPath() {
    insightConfig.setBlockSemicolonInPath(
        !(boolean) configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH));

    simpleConfigurationMigrator.migrate();

    assertThat(
        configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH)).isEqualTo(
        insightConfig.isBlockSemicolonInPath());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_BlockBackslashInPath() {
    insightConfig.setBlockBackslashInPath(
        !(boolean) configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH));

    simpleConfigurationMigrator.migrate();

    assertThat(
        configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH)).isEqualTo(
        insightConfig.isBlockBackslashInPath());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_BlockNonAsciiInPath() {
    insightConfig.setBlockNonAsciiInPath(
        !(boolean) configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH));

    simpleConfigurationMigrator.migrate();

    assertThat(
        configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH)).isEqualTo(
        insightConfig.isBlockNonAsciiInPath());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_ReleaseGraphCacheSize() {
    insightConfig.setReleaseGraphCacheSize(
        ((int) configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE)) + 1);

    simpleConfigurationMigrator.migrate();

    assertThat(
        configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE)).isEqualTo(
        insightConfig.getReleaseGraphCacheSize());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_LicenseLegalHdsRequestLimit() {
    insightConfig.setLicenseLegalHdsRequestLimit(((int) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT)) + 1);

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT)).isEqualTo(
        insightConfig.getLicenseLegalHdsRequestLimit());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_MaxApplicationsToQueryOnDashboard() {
    insightConfig.setMaxApplicationsToQueryOnDashboard(((int) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD)) + 1);

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD)).isEqualTo(
        insightConfig.getMaxApplicationsToQueryOnDashboard());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_MaxAdvancedSearchClauseCount() {
    insightConfig.setMaxAdvancedSearchClauseCount(((int) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT)) + 1);

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT)).isEqualTo(
        insightConfig.getMaxAdvancedSearchClauseCount());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_AdvancedSearchCSVExportDelimiter() {
    insightConfig.setAdvancedSearchCSVExportDelimiter(configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER) + "custom");

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER)).isEqualTo(
        insightConfig.getAdvancedSearchCSVExportDelimiter());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_ConnectTimeoutInSeconds() {
    insightConfig.setConnectTimeoutInSeconds(((int) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS)) + 1);

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS)).isEqualTo(
        insightConfig.getConnectTimeoutInSeconds());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_SocketTimeoutInSeconds() {
    insightConfig.setSocketTimeoutInSeconds(((int) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS)) + 1);

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS)).isEqualTo(
        insightConfig.getSocketTimeoutInSeconds());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_ReportTimeoutInSeconds() {
    insightConfig.setReportTimeoutInSeconds(((int) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS)) + 1);

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS)).isEqualTo(
        insightConfig.getReportTimeoutInSeconds());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_NeedsAcknowledgementOfInitialDashboardFilter() {
    insightConfig.setNeedsAcknowledgementOfInitialDashboardFilter(
        !(boolean) configurationService.getConfigurationNoAuthz(
            SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER));

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER)).isEqualTo(
        insightConfig.isNeedsAcknowledgementOfInitialDashboardFilter());
    logOutput.assertThat().atWarnLevel().contains(
        SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_EnableDefaultPasswordWarning() {
    insightConfig.setEnableDefaultPasswordWarning(!(boolean) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING));

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING)).isEqualTo(
        insightConfig.isEnableDefaultPasswordWarning());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_PolicyMonitoringHour() {
    insightConfig.setPolicyMonitoringHour(((int) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.POLICY_MONITORING_HOUR)) + 1);

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.POLICY_MONITORING_HOUR)).isEqualTo(
        insightConfig.getPolicyMonitoringHour());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.POLICY_MONITORING_HOUR + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.POLICY_MONITORING_HOUR));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_DbBackupDir() {
    insightConfig.setDbBackupDir(
        configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.DB_BACKUP_DIR) + File.separator + "2");

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.DB_BACKUP_DIR)).isEqualTo(
        insightConfig.getDbBackupDir());
    logOutput.assertThat().atWarnLevel().contains(SystemConfigurationProperty.DB_BACKUP_DIR + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.DB_BACKUP_DIR));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_WebhookSecretPassphrase() {
    insightConfig.setWebhookSecretPassphrase(
        configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE) + "2");

    simpleConfigurationMigrator.migrate();

    assertThat(
        configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE)).isEqualTo(
        insightConfig.getWebhookSecretPassphrase());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_WebhookSecretPassphraseFips() {
    insightConfig.setWebhookSecretPassphraseFips(
        configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE_FIPS) + "2");

    simpleConfigurationMigrator.migrate();

    assertThat(
        configurationService.getConfigurationNoAuthz(
            SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE_FIPS)
        ).isEqualTo(insightConfig.getWebhookSecretPassphraseFips());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE_FIPS + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE_FIPS));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_ExternalHyperlinksAllowed() {
    insightConfig.setExternalHyperlinksAllowed(!(boolean) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED));

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED)).isEqualTo(
        insightConfig.isExternalHyperlinksAllowed());
    logOutput.assertThat().atWarnLevel()
        .contains(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_MatcherConfiguration_DisableConanNamespaceMatching() {
    Map<String, String> matcherConfiguration = new HashMap<>();
    matcherConfiguration.put(DISABLE_CONAN_NAMESPACE_MATCHING, "true");
    insightConfig.setMatcherConfiguration(matcherConfiguration);

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING)).isEqualTo(true);
    logOutput.assertThat().atWarnLevel().contains(
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING + EXPECTED_OBSOLETE_SUFFIX);
    verify(mockConfigurationService).applyConfigurationToClients(
        Sets.newHashSet(SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Feature_CodeInsights() {
    Map<String, Boolean> features = new HashMap<>();
    features.put(Feature.CODE_INSIGHTS.getFlag(), false);
    insightConfig.setFeatures(features);
    assertThat(SystemConfigurationPropertyFeature.CODE_INSIGHTS.isEnabled()).isTrue();

    simpleConfigurationMigrator.migrate();

    assertThat(SystemConfigurationPropertyFeature.CODE_INSIGHTS.isEnabled()).isFalse();
    logOutput.assertThat().atWarnLevel().contains(Feature.CODE_INSIGHTS.getFlag() + EXPECTED_OBSOLETE_SUFFIX);
    verifyNoInteractions(mockConfigurationService);
    verify(mockConfigFeaturesService).disableFeatureNoAuthz(Feature.CODE_INSIGHTS.getFlag());
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Feature_ComponentSearchApiWithInnerSource() {
    Map<String, Boolean> features = new HashMap<>();
    features.put(Feature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.getFlag(), false);
    insightConfig.setFeatures(features);
    assertThat(SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.isEnabled()).isTrue();

    simpleConfigurationMigrator.migrate();

    assertThat(SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.isEnabled()).isFalse();
    logOutput.assertThat().atWarnLevel()
        .contains(Feature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.getFlag() + EXPECTED_OBSOLETE_SUFFIX);
    verifyNoInteractions(mockConfigurationService);
    verify(mockConfigFeaturesService).disableFeatureNoAuthz(Feature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.getFlag());
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Feature_DefaultBranchMonitoring() {
    Map<String, Boolean> features = new HashMap<>();
    features.put(Feature.DEFAULT_BRANCH_MONITORING.getFlag(), false);
    insightConfig.setFeatures(features);
    assertThat(SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.isEnabled()).isTrue();

    simpleConfigurationMigrator.migrate();

    assertThat(SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.isEnabled()).isFalse();
    logOutput.assertThat().atWarnLevel()
        .contains(Feature.DEFAULT_BRANCH_MONITORING.getFlag() + EXPECTED_OBSOLETE_SUFFIX);
    verifyNoInteractions(mockConfigurationService);
    verify(mockConfigFeaturesService).disableFeatureNoAuthz(Feature.DEFAULT_BRANCH_MONITORING.getFlag());
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Feature_DependencyDataInApi() {
    Map<String, Boolean> features = new HashMap<>();
    features.put(Feature.DEPENDENCY_DATA_IN_API.getFlag(), false);
    insightConfig.setFeatures(features);
    assertThat(SystemConfigurationPropertyFeature.DEPENDENCY_DATA_IN_API.isEnabled()).isTrue();

    simpleConfigurationMigrator.migrate();

    assertThat(SystemConfigurationPropertyFeature.DEPENDENCY_DATA_IN_API.isEnabled()).isFalse();
    logOutput.assertThat().atWarnLevel()
        .contains(Feature.DEPENDENCY_DATA_IN_API.getFlag() + EXPECTED_OBSOLETE_SUFFIX);
    verifyNoInteractions(mockConfigurationService);
    verify(mockConfigFeaturesService).disableFeatureNoAuthz(Feature.DEPENDENCY_DATA_IN_API.getFlag());
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Feature_InnerSourceTransitiveWaiver() {
    Map<String, Boolean> features = new HashMap<>();
    features.put(Feature.INNER_SOURCE_TRANSITIVE_WAIVER.getFlag(), false);
    insightConfig.setFeatures(features);
    assertThat(SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER.isEnabled()).isTrue();

    simpleConfigurationMigrator.migrate();

    assertThat(SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER.isEnabled()).isFalse();
    logOutput.assertThat().atWarnLevel()
        .contains(Feature.INNER_SOURCE_TRANSITIVE_WAIVER.getFlag() + EXPECTED_OBSOLETE_SUFFIX);
    verifyNoInteractions(mockConfigurationService);
    verify(mockConfigFeaturesService).disableFeatureNoAuthz(Feature.INNER_SOURCE_TRANSITIVE_WAIVER.getFlag());
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Feature_InnerSourceRepositoryIntegration() {
    Map<String, Boolean> features = new HashMap<>();
    features.put(Feature.INNER_SOURCE_REPOSITORY_INTEGRATION.getFlag(), false);
    insightConfig.setFeatures(features);
    assertThat(SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.isEnabled()).isTrue();

    simpleConfigurationMigrator.migrate();

    assertThat(SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.isEnabled()).isFalse();
    logOutput.assertThat().atWarnLevel()
        .contains(Feature.INNER_SOURCE_REPOSITORY_INTEGRATION.getFlag() + EXPECTED_OBSOLETE_SUFFIX);
    verifyNoInteractions(mockConfigurationService);
    verify(mockConfigFeaturesService).disableFeatureNoAuthz(Feature.INNER_SOURCE_REPOSITORY_INTEGRATION.getFlag());
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Feature_PrCommenting() {
    Map<String, Boolean> features = new HashMap<>();
    features.put(Feature.PR_COMMENTING.getFlag(), false);
    insightConfig.setFeatures(features);
    assertThat(SystemConfigurationPropertyFeature.PR_COMMENTING.isEnabled()).isTrue();

    simpleConfigurationMigrator.migrate();

    assertThat(SystemConfigurationPropertyFeature.PR_COMMENTING.isEnabled()).isFalse();
    logOutput.assertThat().atWarnLevel().contains(Feature.PR_COMMENTING.getFlag() + EXPECTED_OBSOLETE_SUFFIX);
    verifyNoInteractions(mockConfigurationService);
    verify(mockConfigFeaturesService).disableFeatureNoAuthz(Feature.PR_COMMENTING.getFlag());
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Feature_PrLineCommenting() {
    Map<String, Boolean> features = new HashMap<>();
    features.put(Feature.PR_LINE_COMMENTING.getFlag(), false);
    insightConfig.setFeatures(features);
    assertThat(SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.isEnabled()).isTrue();

    simpleConfigurationMigrator.migrate();

    assertThat(SystemConfigurationPropertyFeature.PR_LINE_COMMENTING.isEnabled()).isFalse();
    logOutput.assertThat().atWarnLevel().contains(Feature.PR_LINE_COMMENTING.getFlag() + EXPECTED_OBSOLETE_SUFFIX);
    verifyNoInteractions(mockConfigurationService);
    verify(mockConfigFeaturesService).disableFeatureNoAuthz(Feature.PR_LINE_COMMENTING.getFlag());
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Feature_EnableUnauthenticatedPages() {
    Map<String, Boolean> features = new HashMap<>();
    features.put(Feature.ENABLE_UNAUTHENTICATED_PAGES.getFlag(), false);
    insightConfig.setFeatures(features);
    assertThat(SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES.isEnabled()).isTrue();

    simpleConfigurationMigrator.migrate();

    assertThat(SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES.isEnabled()).isFalse();
    logOutput.assertThat().atWarnLevel()
        .contains(Feature.ENABLE_UNAUTHENTICATED_PAGES.getFlag() + EXPECTED_OBSOLETE_SUFFIX);
    verifyNoInteractions(mockConfigurationService);
    verify(mockConfigFeaturesService).disableFeatureNoAuthz(Feature.ENABLE_UNAUTHENTICATED_PAGES.getFlag());
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Feature_InternalSourceControlPolicyEvaluations() {
    Map<String, Boolean> features = new HashMap<>();
    features.put(Feature.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS.getFlag(), false);
    insightConfig.setFeatures(features);
    assertThat(SystemConfigurationPropertyFeature.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS.isEnabled()).isTrue();

    simpleConfigurationMigrator.migrate();

    assertThat(SystemConfigurationPropertyFeature.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS.isEnabled()).isFalse();
    logOutput.assertThat().atWarnLevel()
        .contains(Feature.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS.getFlag() + EXPECTED_OBSOLETE_SUFFIX);
    verifyNoInteractions(mockConfigurationService);
    verify(mockConfigFeaturesService).disableFeatureNoAuthz(
        Feature.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS.getFlag());
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Feature_AlreadyEnabled() {
    Map<String, Boolean> features = new HashMap<>();
    features.put(Feature.CODE_INSIGHTS.getFlag(), true);
    insightConfig.setFeatures(features);
    assertThat(SystemConfigurationPropertyFeature.CODE_INSIGHTS.isEnabled()).isTrue();

    simpleConfigurationMigrator.migrate();

    assertThat(SystemConfigurationPropertyFeature.CODE_INSIGHTS.isEnabled()).isTrue();
    logOutput.assertThat().atWarnLevel().contains(Feature.CODE_INSIGHTS.getFlag() + EXPECTED_OBSOLETE_SUFFIX);
    verifyNoInteractions(mockConfigurationService);
    verify(mockConfigFeaturesService).enableFeatureNoAuthz(Feature.CODE_INSIGHTS.getFlag());
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Feature_AlreadyDisabled() {
    Map<String, Boolean> features = new HashMap<>();
    features.put(Feature.CODE_INSIGHTS.getFlag(), false);
    insightConfig.setFeatures(features);
    SystemConfigurationPropertyFeature.CODE_INSIGHTS.setEnabled(false);
    assertThat(SystemConfigurationPropertyFeature.CODE_INSIGHTS.isEnabled()).isFalse();

    simpleConfigurationMigrator.migrate();

    assertThat(SystemConfigurationPropertyFeature.CODE_INSIGHTS.isEnabled()).isFalse();
    logOutput.assertThat().atWarnLevel().contains(Feature.CODE_INSIGHTS.getFlag() + EXPECTED_OBSOLETE_SUFFIX);
    verifyNoInteractions(mockConfigurationService);
    verify(mockConfigFeaturesService).disableFeatureNoAuthz(Feature.CODE_INSIGHTS.getFlag());
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  private void setAll() {
    insightConfig.setHdsUrl("http://hdsUrl/");
    insightConfig.setCdnUrl("http://cdnUrl/");
    SupportConfig supportConfig = new SupportConfig();
    supportConfig.setReadLimitBytes(supportConfig.getReadLimitBytes() + 1);
    insightConfig.setSupportConfig(supportConfig);
    EventBusConfig eventBusConfig = new EventBusConfig();
    eventBusConfig.setMaxPoolSize(eventBusConfig.getMaxPoolSize() + 1);
    insightConfig.setEventBusConfig(eventBusConfig);
    insightConfig.setCsrfProtection(
        !(boolean) configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.CSRF_PROTECTION));
    insightConfig.setUserAgentSuffix("userAgentSuffix");
    insightConfig.setCspEnabled(
        !(boolean) configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.CSP_ENABLED));
    insightConfig.setBlockSemicolonInPath(
        !(boolean) configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH));
    insightConfig.setBlockBackslashInPath(
        !(boolean) configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH));
    insightConfig.setBlockNonAsciiInPath(
        !(boolean) configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH));
    insightConfig.setReleaseGraphCacheSize(
        ((int) configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE)) + 1);
    insightConfig.setLicenseLegalHdsRequestLimit(((int) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT)) + 1);
    insightConfig.setMaxApplicationsToQueryOnDashboard(((int) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD)) + 1);
    insightConfig.setMaxAdvancedSearchClauseCount(((int) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT)) + 1);
    insightConfig.setAdvancedSearchCSVExportDelimiter(configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER) + "custom");
    insightConfig.setConnectTimeoutInSeconds(((int) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS)) + 1);
    insightConfig.setSocketTimeoutInSeconds(((int) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS)) + 1);
    insightConfig.setReportTimeoutInSeconds(((int) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS)) + 1);
    insightConfig.setNeedsAcknowledgementOfInitialDashboardFilter(
        !(boolean) configurationService.getConfigurationNoAuthz(
            SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER));
    insightConfig.setEnableDefaultPasswordWarning(!(boolean) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING));
    insightConfig.setPolicyMonitoringHour(((int) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.POLICY_MONITORING_HOUR)) + 1);
    insightConfig.setDbBackupDir(
        configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.DB_BACKUP_DIR) + File.separator + "2");
    insightConfig.setWebhookSecretPassphrase(
        configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE) + "2");
    insightConfig.setWebhookSecretPassphraseFips(
        configurationService.getConfigurationNoAuthz(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE_FIPS) + "2");
    insightConfig.setExternalHyperlinksAllowed(!(boolean) configurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED));
    Map<String, String> matcherConfiguration = new HashMap<>();
    matcherConfiguration.put(DISABLE_CONAN_NAMESPACE_MATCHING, "true");
    matcherConfiguration.put(ENABLE_CPE_DATA_MATCHING, "true");
    insightConfig.setMatcherConfiguration(matcherConfiguration);
    insightConfig.setFeatures(Arrays.stream(Feature.values()).collect(Collectors.toMap(Feature::getFlag, f -> false)));
  }
}
