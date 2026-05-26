/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.eventbus.EventBusConfig;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.service.SupportConfig;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class SimpleConfigurationMigratorTest
    extends AbstractComponentTest
{
  private static final String DISABLE_CONAN_NAMESPACE_MATCHING = "disableConanNamespaceMatching";

  private static final String ENABLE_CPE_DATA_MATCHING = "enableCpeDataMatching";

  private static final String EXPECTED_OBSOLETE_SUFFIX = " is now configured using the REST API. "
      + "The configuration in the config.yml or via system properties is obsolete.";

  @Rule
  public LogOutput logOutput = new LogOutput(SimpleConfigurationMigrator.class);

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private SimpleConfigurationMigrator simpleConfigurationMigrator;

  @Inject
  private ApiConfigurationService configurationService;

  private ApiConfigurationService configurationServiceSpy;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Mock
  private ApiConfigFeaturesService mockConfigFeaturesService;

  @Before
  @After
  public void clear() {
    reset(mockConfigFeaturesService);
    if (configurationServiceSpy != null) {
      clearInvocations(configurationServiceSpy);
    }
    migrationTrackerDAO.deleteById(SimpleConfigurationMigrator.MIGRATION_ID);
    resetInsightConfig();
    SystemConfigurationPropertyFeature.CODE_INSIGHTS.setEnabled(true);
  }

  @Test
  public void testMigrate_AlreadyMigrated() {
    Map<String, Object> expectedProperties = setAll();
    clearInvocations(configurationServiceSpy);
    migrationTrackerDAO.insertTracker(SimpleConfigurationMigrator.MIGRATION_ID);

    simpleConfigurationMigrator.migrate();

    assertCombinedMigrationWarningContains(expectedProperties.keySet().toArray(String[]::new));
    logOutput.assertThat().atDebugLevel().contains("configuration already migrated.");
    verify(configurationServiceSpy, never()).applyConfigurationToClients(ArgumentMatchers.<Set<String>>any());
    verifyNoInteractions(mockConfigFeaturesService);
  }

  @Test
  public void testMigrate_AllNull() {
    simpleConfigurationMigrator.migrate();

    logOutput.assertThat().doesNotContain(EXPECTED_OBSOLETE_SUFFIX);
    logOutput.assertThat().doesNotContain("configuration already migrated.");
    verify(configurationServiceSpy, never()).applyConfigurationToClients(ArgumentMatchers.<Set<String>>any());
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_All() {
    Map<String, Object> expectedProperties = setAll();
    clearInvocations(configurationServiceSpy);

    simpleConfigurationMigrator.migrate();

    assertCombinedMigrationWarningContains(expectedProperties.keySet().toArray(String[]::new));
    expectedProperties
        .forEach((propertyName, expectedValue) -> assertThat(configurationService.getConfigurationNoAuthz(propertyName))
            .isEqualTo(expectedValue));
    Set<String> expectedApplyConfigurationToClients =
        new HashSet<>(SimpleConfigurationMigrator.NAME_TO_GETTER.keySet());
    expectedApplyConfigurationToClients.removeAll(SimpleConfigurationMigrator.FEATURE_FLAGS);
    verify(configurationServiceSpy).applyConfigurationToClients(expectedApplyConfigurationToClients);
    Arrays.stream(Feature.values())
        .forEach(feature -> verify(mockConfigFeaturesService)
            .disableFeatureNoAuthz(feature.getFlag()));
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_SingleConfiguration_UsesCombinedWarningFormat() {
    insightConfig.setCdnUrl("http://cdnUrl/");

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService
        .getConfigurationNoAuthz(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.CDN_URL))
            .isEqualTo(insightConfig.getCdnUrl());
    assertCombinedMigrationWarningContains(
        com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.CDN_URL);
    verify(configurationServiceSpy).applyConfigurationToClients(
        Set.of(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.CDN_URL));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_IndependentConfigurationsAreMigratedTogether() {
    insightConfig.setHdsUrl("badUrl");
    insightConfig.setCdnUrl("http://cdnUrl/");
    Object defaultHdsUrl = configurationService
        .getConfigurationNoAuthz(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.HDS_URL);

    simpleConfigurationMigrator.migrate();

    assertThat(configurationService
        .getConfigurationNoAuthz(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.HDS_URL))
            .isEqualTo(defaultHdsUrl);
    assertThat(configurationService
        .getConfigurationNoAuthz(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.CDN_URL))
            .isEqualTo(insightConfig.getCdnUrl());
    assertCombinedMigrationWarningContains(
        com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.HDS_URL,
        com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.CDN_URL);
    logOutput.assertThat().atWarnLevel().contains("cannot be migrated");
    verify(configurationServiceSpy).applyConfigurationToClients(
        Set.of(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.CDN_URL));
    verifyNoInteractions(mockConfigFeaturesService);
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Feature_DisablesUsingApiConfigFeaturesService() {
    insightConfig.setFeatures(Map.of(Feature.CODE_INSIGHTS.getFlag(), false));
    SystemConfigurationPropertyFeature.CODE_INSIGHTS.setEnabled(true);

    simpleConfigurationMigrator.migrate();

    assertCombinedMigrationWarningContains(Feature.CODE_INSIGHTS.getFlag());
    verify(mockConfigFeaturesService).disableFeatureNoAuthz(Feature.CODE_INSIGHTS.getFlag());
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Feature_AlreadyEnabled() {
    insightConfig.setFeatures(Map.of(Feature.CODE_INSIGHTS.getFlag(), true));

    simpleConfigurationMigrator.migrate();

    assertCombinedMigrationWarningContains(Feature.CODE_INSIGHTS.getFlag());
    verify(mockConfigFeaturesService).enableFeatureNoAuthz(Feature.CODE_INSIGHTS.getFlag());
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_Feature_AlreadyDisabled() {
    insightConfig.setFeatures(Map.of(Feature.CODE_INSIGHTS.getFlag(), false));
    SystemConfigurationPropertyFeature.CODE_INSIGHTS.setEnabled(false);

    simpleConfigurationMigrator.migrate();

    assertCombinedMigrationWarningContains(Feature.CODE_INSIGHTS.getFlag());
    verify(mockConfigFeaturesService).disableFeatureNoAuthz(Feature.CODE_INSIGHTS.getFlag());
    assertThat(migrationTrackerDAO.isTrackerPresent(SimpleConfigurationMigrator.MIGRATION_ID)).isTrue();
  }

  private void assertCombinedMigrationWarningContains(final String... propertyNames) {
    logOutput.assertThat().atWarnLevel().contains(EXPECTED_OBSOLETE_SUFFIX);
    Arrays.stream(propertyNames).forEach(propertyName -> logOutput.assertThat().atWarnLevel().contains(propertyName));
  }

  private Map<String, Object> setAll() {
    Map<String, Object> expected = new LinkedHashMap<>();

    insightConfig.setHdsUrl("http://hdsUrl/");
    expected.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.HDS_URL,
        insightConfig.getHdsUrl());

    insightConfig.setCdnUrl("http://cdnUrl/");
    expected.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.CDN_URL,
        insightConfig.getCdnUrl());

    SupportConfig supportConfig = new SupportConfig();
    supportConfig.setReadLimitBytes(supportConfig.getReadLimitBytes() + 1);
    insightConfig.setSupportConfig(supportConfig);
    expected.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES,
        insightConfig.getSupportConfig().getReadLimitBytes());

    EventBusConfig eventBusConfig = new EventBusConfig();
    eventBusConfig.setMaxPoolSize(eventBusConfig.getMaxPoolSize() + 1);
    insightConfig.setEventBusConfig(eventBusConfig);
    expected.put(
        com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE,
        insightConfig.getEventBusConfig().getMaxPoolSize());

    insightConfig.setCsrfProtection(
        !(boolean) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.CSRF_PROTECTION));
    expected.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.CSRF_PROTECTION,
        insightConfig.isCsrfProtection());

    insightConfig.setUserAgentSuffix("userAgentSuffix");
    expected.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.USER_AGENT_SUFFIX,
        insightConfig.getUserAgentSuffix());

    insightConfig.setCspEnabled(
        !(boolean) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.CSP_ENABLED));
    expected.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.CSP_ENABLED,
        insightConfig.isCspEnabled());

    insightConfig.setBlockSemicolonInPath(
        !(boolean) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH));
    expected.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH,
        insightConfig.isBlockSemicolonInPath());

    insightConfig.setBlockBackslashInPath(
        !(boolean) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH));
    expected.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH,
        insightConfig.isBlockBackslashInPath());

    insightConfig.setBlockNonAsciiInPath(
        !(boolean) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH));
    expected.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH,
        insightConfig.isBlockNonAsciiInPath());

    insightConfig.setReleaseGraphCacheSize(
        ((int) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE)) + 1);
    expected.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE,
        insightConfig.getReleaseGraphCacheSize());

    insightConfig.setLicenseLegalHdsRequestLimit(
        ((int) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT))
            + 1);
    expected.put(
        com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT,
        insightConfig.getLicenseLegalHdsRequestLimit());

    insightConfig.setMaxApplicationsToQueryOnDashboard(
        ((int) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD))
            + 1);
    expected.put(
        com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD,
        insightConfig.getMaxApplicationsToQueryOnDashboard());

    insightConfig.setMaxAdvancedSearchClauseCount(
        ((int) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT))
            + 1);
    expected.put(
        com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT,
        insightConfig.getMaxAdvancedSearchClauseCount());

    insightConfig.setAdvancedSearchCSVExportDelimiter(
        configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER)
            + "custom");
    expected.put(
        com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER,
        insightConfig.getAdvancedSearchCSVExportDelimiter());

    insightConfig.setConnectTimeoutInSeconds(
        ((int) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS))
            + 1);
    expected.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS,
        insightConfig.getConnectTimeoutInSeconds());

    insightConfig.setSocketTimeoutInSeconds(
        ((int) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS)) + 1);
    expected.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS,
        insightConfig.getSocketTimeoutInSeconds());

    insightConfig.setReportTimeoutInSeconds(
        ((int) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS)) + 1);
    expected.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS,
        insightConfig.getReportTimeoutInSeconds());

    insightConfig.setNeedsAcknowledgementOfInitialDashboardFilter(
        !(boolean) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER));
    expected.put(
        com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER,
        insightConfig.isNeedsAcknowledgementOfInitialDashboardFilter());

    insightConfig.setEnableDefaultPasswordWarning(
        !(boolean) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING));
    expected.put(
        com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING,
        insightConfig.isEnableDefaultPasswordWarning());

    insightConfig.setPolicyMonitoringHour(
        ((int) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.POLICY_MONITORING_HOUR)) + 1);
    expected.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.POLICY_MONITORING_HOUR,
        insightConfig.getPolicyMonitoringHour());

    insightConfig.setDbBackupDir(
        configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DB_BACKUP_DIR)
            + File.separator + "2");
    expected.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DB_BACKUP_DIR,
        insightConfig.getDbBackupDir());

    insightConfig.setWebhookSecretPassphrase(
        configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE)
            + "2");
    expected.put(
        com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE,
        insightConfig.getWebhookSecretPassphrase());

    insightConfig.setWebhookSecretPassphraseFips(
        configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE_FIPS)
            + "2");
    expected.put(
        com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE_FIPS,
        insightConfig.getWebhookSecretPassphraseFips());

    insightConfig.setExternalHyperlinksAllowed(
        !(boolean) configurationService.getConfigurationNoAuthz(
            com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED));
    expected.put(
        com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED,
        insightConfig.isExternalHyperlinksAllowed());

    Map<String, String> matcherConfiguration = new HashMap<>();
    matcherConfiguration.put(DISABLE_CONAN_NAMESPACE_MATCHING, "true");
    matcherConfiguration.put(ENABLE_CPE_DATA_MATCHING, "true");
    insightConfig.setMatcherConfiguration(matcherConfiguration);
    expected.put(
        com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING,
        true);

    insightConfig.setFeatures(
        Arrays.stream(Feature.values()).collect(Collectors.toMap(Feature::getFlag, feature -> false)));

    return expected;
  }

  private void resetInsightConfig() {
    InsightConfig defaults = new InsightConfig();
    insightConfig.setHdsUrl(defaults.getHdsUrl());
    insightConfig.setCdnUrl(defaults.getCdnUrl());
    insightConfig.setSupportConfig(defaults.getSupportConfig());
    insightConfig.setEventBusConfig(defaults.getEventBusConfig());
    insightConfig.setCsrfProtection(defaults.isCsrfProtection());
    insightConfig.setUserAgentSuffix(defaults.getUserAgentSuffix());
    insightConfig.setCspEnabled(defaults.isCspEnabled());
    insightConfig.setBlockSemicolonInPath(defaults.isBlockSemicolonInPath());
    insightConfig.setBlockBackslashInPath(defaults.isBlockBackslashInPath());
    insightConfig.setBlockNonAsciiInPath(defaults.isBlockNonAsciiInPath());
    insightConfig.setReleaseGraphCacheSize(defaults.getReleaseGraphCacheSize());
    insightConfig.setLicenseLegalHdsRequestLimit(defaults.getLicenseLegalHdsRequestLimit());
    insightConfig.setMaxApplicationsToQueryOnDashboard(defaults.getMaxApplicationsToQueryOnDashboard());
    insightConfig.setMaxAdvancedSearchClauseCount(defaults.getMaxAdvancedSearchClauseCount());
    insightConfig.setAdvancedSearchCSVExportDelimiter(defaults.getAdvancedSearchCSVExportDelimiter());
    insightConfig.setConnectTimeoutInSeconds(defaults.getConnectTimeoutInSeconds());
    insightConfig.setSocketTimeoutInSeconds(defaults.getSocketTimeoutInSeconds());
    insightConfig.setReportTimeoutInSeconds(defaults.getReportTimeoutInSeconds());
    insightConfig
        .setNeedsAcknowledgementOfInitialDashboardFilter(defaults.isNeedsAcknowledgementOfInitialDashboardFilter());
    insightConfig.setEnableDefaultPasswordWarning(defaults.isEnableDefaultPasswordWarning());
    insightConfig.setPolicyMonitoringHour(defaults.getPolicyMonitoringHour());
    insightConfig.setDbBackupDir(defaults.getDbBackupDir());
    insightConfig.setWebhookSecretPassphrase(defaults.getWebhookSecretPassphrase());
    insightConfig.setWebhookSecretPassphraseFips(defaults.getWebhookSecretPassphraseFips());
    insightConfig.setExternalHyperlinksAllowed(defaults.isExternalHyperlinksAllowed());
    insightConfig.setMatcherConfiguration(defaults.getMatcherConfiguration());
    insightConfig.setFeatures(defaults.getFeatures());
  }
}
