/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.migration.ScanFileCleaner;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.AllowedIp;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Maps;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

public class ApiConfigurationServiceTest
    extends AbstractComponentTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule
  public LogOutput logOutput = new LogOutput(ConfigurationUtils.class);

  @Inject
  private ApiConfigurationService service;

  @Inject
  private SystemConfigurationPropertyDAO dao;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private ConfigurationListener mockConfigurationListener;

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private TestProductLicense testProductLicense;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    binder.bind(ConfigurationListener.class).toInstance(mockConfigurationListener);
    super.configure(binder);
  }

  @Test
  public void testGetConfiguration_Null() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.getConfiguration(null)).withMessageContaining(
        ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  public void testGetConfiguration_Empty() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.getConfiguration(Collections.emptySet())).withMessageContaining(
        ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  public void testGetConfiguration_InvalidPropertyName() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.getConfiguration(SetUtils.hashSet("invalidPropertyName"))).withMessageContaining(
        String.format(ApiConfigurationService.INVALID_PROPERTY_NAME_ERROR_MSG, "invalidPropertyName"));
  }

  @Test
  public void testGetConfiguration() {
    dao.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    dao.set(SystemConfigurationProperty.FORCE_BASE_URL, String.valueOf(Boolean.TRUE));

    Map<String, Object> configuration = service.getConfiguration(
        SetUtils.hashSet(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL));

    assertThat(configuration).hasSize(2).containsEntry(SystemConfigurationProperty.BASE_URL, "http://baseUrl/")
        .containsEntry(SystemConfigurationProperty.FORCE_BASE_URL, true);
  }

  @Test
  public void testSetConfiguration_Null() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.setConfiguration(null)).withMessageContaining(
        ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  public void testSetConfiguration_Empty() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.setConfiguration(Collections.emptyMap())).withMessageContaining(
        ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  public void testSetConfiguration_InvalidPropertyName() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("invalidPropertyName", null);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.setConfiguration(payload)).withMessageContaining(
        String.format(ApiConfigurationService.INVALID_PROPERTY_NAME_ERROR_MSG, "invalidPropertyName"));
  }

  @Test
  public void testSetConfiguration_InvalidPropertyValue() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(SystemConfigurationProperty.BASE_URL, true);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.setConfiguration(payload)).withMessageContaining(
        String.format(ApiConfigurationService.INVALID_PROPERTY_VALUE_TYPE_ERROR_MSG,
            SystemConfigurationProperty.BASE_URL, String.class, Boolean.class));
  }

  @Test
  public void testSetConfiguration_InvalidUrl() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(SystemConfigurationProperty.BASE_URL, "invalid");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.setConfiguration(payload))
        .withMessageContaining("Invalid URL: invalid/");
  }

  @Test
  public void testSetConfiguration_NullValues() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(SystemConfigurationProperty.BASE_URL, null);
    payload.put(SystemConfigurationProperty.FORCE_BASE_URL, null);

    service.setConfiguration(payload);

    assertThat(dao.getByName(SystemConfigurationProperty.BASE_URL)).isNull();
    assertThat(dao.getByName(SystemConfigurationProperty.FORCE_BASE_URL)).isNull();
  }

  @Test
  public void testSetConfiguration() {
    Map<String, Object> payload = new HashMap<>();
    payload.put(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    payload.put(SystemConfigurationProperty.FORCE_BASE_URL, true);

    service.setConfiguration(payload);

    assertThat(dao.get(SystemConfigurationProperty.BASE_URL)).isEqualTo("http://baseUrl/");
    assertThat(dao.get(SystemConfigurationProperty.FORCE_BASE_URL)).isEqualTo(String.valueOf(Boolean.TRUE));
  }

  @Test
  public void testDeleteConfiguration_Null() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.deleteConfiguration(null)).withMessageContaining(
        ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  public void testDeleteConfiguration_Empty() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.deleteConfiguration(Collections.emptySet())).withMessageContaining(
        ApiConfigurationService.NO_PROPERTIES_ERROR_MSG);
  }

  @Test
  public void testDeleteConfiguration_InvalidPropertyName() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.deleteConfiguration(SetUtils.hashSet("invalidPropertyName"))).withMessageContaining(
        String.format(ApiConfigurationService.INVALID_PROPERTY_NAME_ERROR_MSG, "invalidPropertyName"));
  }

  @Test
  public void testDeleteConfiguration_NullValues() {
    service.deleteConfiguration(
        SetUtils.hashSet(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL));

    assertThat(dao.getByName(SystemConfigurationProperty.BASE_URL)).isNull();
    assertThat(dao.getByName(SystemConfigurationProperty.FORCE_BASE_URL)).isNull();
  }

  @Test
  public void testDeleteConfiguration() {
    dao.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    dao.set(SystemConfigurationProperty.FORCE_BASE_URL, String.valueOf(Boolean.TRUE));

    service.deleteConfiguration(
        SetUtils.hashSet(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL));

    assertThat(dao.getByName(SystemConfigurationProperty.BASE_URL)).isNull();
    assertThat(dao.getByName(SystemConfigurationProperty.FORCE_BASE_URL)).isNull();
  }

  @Test
  public void testDeleteConfiguration_PurgeScanFiles_Deleted() {
    dao.set(SystemConfigurationProperty.PURGE_SCAN_FILES, "withReports");

    service.deleteConfiguration(SetUtils.hashSet(SystemConfigurationProperty.PURGE_SCAN_FILES));

    assertThat(dao.getByName(SystemConfigurationProperty.PURGE_SCAN_FILES)).isNull();
    verify(mockTaskScheduler).scheduleOneTimeTask(any(ScanFileCleaner.class));
  }

  @Test
  public void testUpdateAllClusterNodesFromConfiguration() {
    ApiConfigurationService spy = spy(service);
    Set<String> propertyNames =
        SetUtils.hashSet(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL);

    spy.updateAllClusterNodesFromConfiguration(propertyNames);

    verify(spy).applyConfigurationToClients(propertyNames);
    Map<String, String> parameters = new HashMap<>();
    parameters.put(ApiConfigurationService.TASK_PARAM_PROPERTIES,
        StringUtils.join(propertyNames, ApiConfigurationService.TASK_PARAM_PROPERTIES_DELIMITER));
    verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(spy, parameters);
  }

  @Test
  public void testApplyConfigurationToClients() {
    Set<String> propertyNames =
        SetUtils.hashSet(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL);

    service.applyConfigurationToClients(propertyNames);

    verify(mockConfigurationListener).configurationChanged(propertyNames);
  }

  @Test
  public void testGetConfiguration_ForceBaseUrlNotSet_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(SetUtils.hashSet(SystemConfigurationProperty.FORCE_BASE_URL))).containsEntry(
        SystemConfigurationProperty.FORCE_BASE_URL, false);
  }

  @Test
  public void testGetConfiguration_HdsUrlNotSet_ReturnsDefault() {
    setHdsUrl(null);

    assertThat(service.getConfigurationNoAuthz(SetUtils.hashSet(SystemConfigurationProperty.HDS_URL))).containsEntry(
        SystemConfigurationProperty.HDS_URL, "https://clm.sonatype.com/");
  }

  @Test
  public void testGetConfiguration_CdnUrlNotSet_ReturnsDefault() {
    assertThat(service.getConfigurationNoAuthz(SetUtils.hashSet(SystemConfigurationProperty.CDN_URL))).containsEntry(
        SystemConfigurationProperty.CDN_URL, "https://cdn.sonatype.com/");
  }

  @Test
  public void testGetConfiguration_SupportReadLimitBytesNotSet_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES))).containsEntry(
        SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES, 31457280L);
  }

  @Test
  public void testGetConfiguration_SupportClusterLogFileRegexNotSet_ReturnsDefault() {
    assertThat(service.getConfigurationNoAuthz(SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX)).isEqualTo(
        InsightConfig.DEFAULT_SUPPORT_CLUSTER_LOG_FILE_REGEX);
  }

  @Test
  public void testGetConfiguration_SupportClusterLogFileRegex_OnlyEnv() {
    String supportClusterLogFileRegex = ".*other/" + InsightConfig.DEFAULT_SUPPORT_CLUSTER_LOG_FILE_REGEX;
    environmentVariables.set(InsightConfig.NXIQ_SUPPORT_CLUSTER_LOG_FILE_REGEX, supportClusterLogFileRegex);

    assertThat(service.getConfigurationNoAuthz(SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX)).isEqualTo(
        supportClusterLogFileRegex);
  }

  @Test
  public void testGetConfiguration_SupportClusterLogFileRegex_OnlyDb() {
    String supportClusterLogFileRegex = ".*other/" + InsightConfig.DEFAULT_SUPPORT_CLUSTER_LOG_FILE_REGEX;
    dao.set(SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX, supportClusterLogFileRegex);

    assertThat(service.getConfigurationNoAuthz(SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX)).isEqualTo(
        supportClusterLogFileRegex);
  }

  @Test
  public void testGetConfiguration_SupportClusterLogFileRegex_EnvAndDb() {
    String supportClusterLogFileRegex1 = ".*other1/" + InsightConfig.DEFAULT_SUPPORT_CLUSTER_LOG_FILE_REGEX;
    environmentVariables.set(InsightConfig.NXIQ_SUPPORT_CLUSTER_LOG_FILE_REGEX, supportClusterLogFileRegex1);
    String supportClusterLogFileRegex2 = ".*other2/" + InsightConfig.DEFAULT_SUPPORT_CLUSTER_LOG_FILE_REGEX;
    dao.set(SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX, supportClusterLogFileRegex2);

    assertThat(service.getConfigurationNoAuthz(SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX)).isEqualTo(
        supportClusterLogFileRegex1);
  }

  @Test
  public void testGetConfiguration_EventBusMaxThreadPoolSizeNotSet_ReturnsDefault() {
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE))).containsEntry(
        SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE, AsyncEventBus.DEFAULT_MAX_POOL_SIZE);
  }

  @Test
  public void testGetConfiguration_CsrfProtectionNotSet_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(SetUtils.hashSet(SystemConfigurationProperty.CSRF_PROTECTION))).containsEntry(
        SystemConfigurationProperty.CSRF_PROTECTION, true);
  }

  @Test
  public void testGetConfiguration_CspEnabledNotSet_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(SetUtils.hashSet(SystemConfigurationProperty.CSP_ENABLED))).containsEntry(
        SystemConfigurationProperty.CSP_ENABLED, true);
  }

  @Test
  public void testGetConfiguration_BlockSemicolonInPathNotSet_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH))).containsEntry(
        SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH, true);
  }

  @Test
  public void testGetConfiguration_BlockBackslashInPathNotSet_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH))).containsEntry(
        SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH, true);
  }

  @Test
  public void testGetConfiguration_BlockNonAsciiInPathNotSet_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH))).containsEntry(
        SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH, false);
  }

  @Test
  public void testGetConfiguration_ReleaseGraphCacheSizeNotSet_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE))).containsEntry(
        SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE, 1000);
  }

  @Test
  public void testGetConfiguration_LicenseLegalHdsRequestLimitNotSet_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT))).containsEntry(
        SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT, 50);
  }

  @Test
  public void testGetConfiguration_MaxApplicationsToQueryOnDashboardNotSet_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD))).containsEntry(
        SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD, 0);
  }

  @Test
  public void testGetConfiguration_MaxAdvancedSearchClauseCountNotSet_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT))).containsEntry(
        SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT, 2048);
  }

  @Test
  public void testGetConfiguration_AdvancedSearchCSVExportDelimiterNotSet_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER))).containsEntry(
        SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER, ",");
  }

  @Test
  public void testGetConfiguration_ConnectTimeoutInSecondsNotSet_ReturnsDefault() {
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS))).containsEntry(
        SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS, 20);
  }

  @Test
  public void testGetConfiguration_SocketTimeoutInSecondsNotSet_ReturnsDefault() {
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS))).containsEntry(
        SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS, 3 * 60);
  }

  @Test
  public void testGetConfiguration_ReportTimeoutInSecondsNotSet_ReturnsDefault() {
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS))).containsEntry(
        SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS, 35 * 60);
  }

  @Test
  public void testGetConfiguration_NeedsAcknowledgementOfInitialDashboardFilterNotSet_ReturnsDefault() {
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER))).containsEntry(
        SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER, false);
  }

  @Test
  public void testGetConfiguration_EnableDefaultPasswordWarningNotSet_ReturnsDefault() {
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING))).containsEntry(
        SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING, true);
  }

  @Test
  public void testGetConfiguration_PolicyMonitoringHourNotSet_ReturnsDefault() {
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.POLICY_MONITORING_HOUR))).containsEntry(
        SystemConfigurationProperty.POLICY_MONITORING_HOUR, 0);
  }

  @Test
  public void testGetConfiguration_DbBackupDirNotSet_ReturnsDefault() {
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.DB_BACKUP_DIR))).containsEntry(
        SystemConfigurationProperty.DB_BACKUP_DIR,
        new File(insightConfig.getSonatypeWork(), InsightConfig.DEFAULT_BACKUP_DIR).getAbsolutePath());
  }

  @Test
  public void testGetConfiguration_WebhookSecretPassphraseNotSet_ReturnsDefault() {
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE))).containsEntry(
        SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE, "^d1swM!FF&qQ");
  }

  @Test
  public void testGetConfiguration_ExternalHyperlinksAllowedNotSet_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED))).containsEntry(
        SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED, true);
  }

  @Test
  public void testGetConfiguration_MatcherConfiguration_DisableConanNamespaceMatchingNotSet_ReturnsDefault() {
    assertThat(service.getConfigurationNoAuthz(SetUtils.hashSet(
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING))).containsEntry(
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING, false);
  }

  @Test
  public void testGetConfiguration_BfsArtifactoryExpiredTokenRegexNotSet_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_REGEX))).containsEntry(
        SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_REGEX, "(?i)(?s).*token[\\s\\w:]+expired.*");
  }

  @Test
  public void testGetConfiguration_BfsArtifactoryExpiredTokenEmailNotSet_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL))).containsEntry(
        SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL, null);
  }

  @Test
  public void testGetConfiguration_BfsComponentLimit_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT)))
        .containsEntry(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT, null);
  }

  @Test
  public void testGetConfiguration_BfsQueryRepositoriesList_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.BFS_REPOSITORIES)))
        .containsEntry(SystemConfigurationProperty.BFS_REPOSITORIES, null);
  }

  @Test
  public void testGetConfiguration_AccessAllowlist_ReturnsDefault() {
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.ACCESS_ALLOWLIST)))
        .containsEntry(SystemConfigurationProperty.ACCESS_ALLOWLIST, null);
  }

  @Test
  public void testGetConfiguration_SchemaMigrationEnabled_NullDb_NullEnv() {
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED))).containsEntry(
        SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, true);
  }

  @Test
  public void testGetConfiguration_SchemaMigrationEnabled_Db_NullEnv() {
    service.setConfigurationNoAuthz(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, false);

    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED))).containsEntry(
        SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, false);
  }

  @Test
  public void testGetConfiguration_SchemaMigrationEnabled_NullDb_Env() {
    environmentVariables.set(DatabaseMigrator.NXIQ_SCHEMA_MIGRATION, "false");

    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED))).containsEntry(
        SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, false);
  }

  @Test
  public void testGetConfiguration_SchemaMigrationEnabled_Db_Env() {
    environmentVariables.set(DatabaseMigrator.NXIQ_SCHEMA_MIGRATION, "false");
    service.setConfigurationNoAuthz(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, true);

    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED))).containsEntry(
        SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, false);
  }

  @Test
  public void testExecute() throws Exception {
    Set<String> propertyNames =
        SetUtils.hashSet(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL);
    Map<String, String> parameters = new HashMap<>();
    parameters.put(ApiConfigurationService.TASK_PARAM_PROPERTIES,
        StringUtils.join(propertyNames, ApiConfigurationService.TASK_PARAM_PROPERTIES_DELIMITER));
    JobDataMap jobDataMap = new JobDataMap(parameters);
    ApiConfigurationService spy = spy(service);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(spy)
        .applyConfigurationToClients(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL);
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      spy.execute(mockJobExecutionContext);
    }

    verify(spy).applyConfigurationToClients(SystemConfigurationProperty.BASE_URL,
        SystemConfigurationProperty.FORCE_BASE_URL);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(ApiConfigurationService.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testSetConfiguration_NarrowerType_To_WiderType() {
    Map<String, Object> properties = Maps.newHashMap(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES, 500);

    service.setConfiguration(properties);

    assertThat(dao.get(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES)).isEqualTo("500");
  }

  @Test
  public void testSetConfiguration_SameType() {
    Map<String, Object> properties = Maps.newHashMap(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES, 500L);

    service.setConfiguration(properties);

    assertThat(dao.get(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES)).isEqualTo("500");
  }

  @Test
  public void testSetConfiguration_WiderType_To_NarrowerType() {
    Map<String, Object> properties = Maps.newHashMap(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE, 500L);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.setConfiguration(properties))
        .withMessageContaining(String.format(ApiConfigurationService.INVALID_PROPERTY_VALUE_TYPE_ERROR_MSG,
            SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE, int.class, long.class));
  }

  @Test
  public void testSetConfiguration_BaseUrlAndForceBaseUrl_Null() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, null);
    properties.put(SystemConfigurationProperty.FORCE_BASE_URL, null);

    service.setConfiguration(properties);

    assertThat(dao.get(SystemConfigurationProperty.BASE_URL)).isNull();
    assertThat(dao.get(SystemConfigurationProperty.FORCE_BASE_URL)).isNull();
    assertThat(service.getConfiguration(
        SetUtils.hashSet(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL)))
        .containsEntry(SystemConfigurationProperty.BASE_URL, null)
        .containsEntry(SystemConfigurationProperty.FORCE_BASE_URL, false);
    logOutput.assertThat().atErrorLevel().doesNotContain("DEPRECATION NOTICE");
  }

  @Test
  public void testSetConfiguration_BaseUrlAndForceBaseUrl() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, "http://my-base-url/");
    properties.put(SystemConfigurationProperty.FORCE_BASE_URL, true);

    service.setConfiguration(properties);

    assertThat(dao.get(SystemConfigurationProperty.BASE_URL)).isEqualTo("http://my-base-url/");
    assertThat(dao.get(SystemConfigurationProperty.FORCE_BASE_URL)).isEqualTo("true");
    assertThat(service.getConfiguration(
        SetUtils.hashSet(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL)))
        .containsEntry(SystemConfigurationProperty.BASE_URL, "http://my-base-url/")
        .containsEntry(SystemConfigurationProperty.FORCE_BASE_URL, true);
    logOutput.assertThat().atErrorLevel().contains("http://my-base-url/");
  }

  @Test
  public void testSetConfiguration_NoDatabaseHdsUrl_NoInsightConfigHdsUrl() {
    insightConfig.setHdsUrl(null);

    setHdsUrl(null);

    assertThat(dao.get(SystemConfigurationProperty.HDS_URL)).isNull();
    assertThat(service.getConfiguration(SetUtils.hashSet(SystemConfigurationProperty.HDS_URL))).containsEntry(
        SystemConfigurationProperty.HDS_URL, "https://clm.sonatype.com/");
  }

  @Test
  public void testSetConfiguration_NoDatabaseHdsUrl_InsightConfigHdsUrl() {
    String expected = "http://my-config-hds-url/";
    insightConfig.setHdsUrl(expected);

    setHdsUrl(null);

    assertThat(dao.get(SystemConfigurationProperty.HDS_URL)).isNull();
    assertThat(service.getConfiguration(SetUtils.hashSet(SystemConfigurationProperty.HDS_URL))).containsEntry(
        SystemConfigurationProperty.HDS_URL, expected);
  }

  @Test
  public void testSetConfiguration_DatabaseHdsUrl_NoInsightConfigHdsUrl() {
    String expected = "http://my-db-hds-url/";
    insightConfig.setHdsUrl(null);

    setHdsUrl(expected);

    assertThat(dao.get(SystemConfigurationProperty.HDS_URL)).isEqualTo(expected);
    assertThat(service.getConfiguration(SetUtils.hashSet(SystemConfigurationProperty.HDS_URL))).containsEntry(
        SystemConfigurationProperty.HDS_URL, expected);
  }

  @Test
  public void testSetConfiguration_DatabaseHdsUrl_InsightConfigHdsUrl() {
    String expected = "http://my-db-hds-url/";
    insightConfig.setHdsUrl("http://my-config-hds-url");

    setHdsUrl(expected);

    assertThat(dao.get(SystemConfigurationProperty.HDS_URL)).isEqualTo(expected);
    assertThat(service.getConfiguration(SetUtils.hashSet(SystemConfigurationProperty.HDS_URL))).containsEntry(
        SystemConfigurationProperty.HDS_URL, expected);
  }

  @Test
  public void testSetConfiguration_CdnUrl_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.CDN_URL, null));

    assertThat(dao.get(SystemConfigurationProperty.CDN_URL)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.CDN_URL))).containsEntry(
        SystemConfigurationProperty.CDN_URL, "https://cdn.sonatype.com/");
  }

  @Test
  public void testSetConfiguration_CdnUrl() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.CDN_URL, "http://my-cdn-url/"));

    assertThat(dao.get(SystemConfigurationProperty.CDN_URL)).isEqualTo("http://my-cdn-url/");
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.CDN_URL))).containsEntry(
        SystemConfigurationProperty.CDN_URL, "http://my-cdn-url/");
  }

  @Test
  public void testSetConfiguration_SupportReadLimitBytes_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES, null));

    assertThat(dao.get(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES))).containsEntry(
        SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES, 31457280L);
  }

  @Test
  public void testSetConfiguration_SupportReadLimitBytes() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES, 10L));

    assertThat(dao.get(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES)).isEqualTo("10");
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES))).containsEntry(
        SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES, 10L);
  }

  @Test
  public void testSetConfiguration_SupportClusterFileRegex_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX, null));

    assertThat(dao.get(SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX)).isNull();
    assertThat(service.getConfigurationNoAuthz(SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX)).isEqualTo(
        InsightConfig.DEFAULT_SUPPORT_CLUSTER_LOG_FILE_REGEX);
  }

  @Test
  public void testSetConfiguration_SupportClusterLogFileRegex() {
    String supportClusterLogFileRegex = ".*other/" + InsightConfig.DEFAULT_SUPPORT_CLUSTER_LOG_FILE_REGEX;

    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX, supportClusterLogFileRegex));

    assertThat(dao.get(SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX)).isEqualTo(
        supportClusterLogFileRegex);
    assertThat(service.getConfigurationNoAuthz(SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX)).isEqualTo(
        supportClusterLogFileRegex);
  }

  @Test
  public void testSetConfiguration_EventBusMaxThreadPoolSize_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE, null));

    assertThat(dao.get(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE))).containsEntry(
        SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE, AsyncEventBus.DEFAULT_MAX_POOL_SIZE);
  }

  @Test
  public void testSetConfiguration_EventBusMaxThreadPoolSize() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE, 10));

    assertThat(dao.get(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE)).isEqualTo("10");
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE))).containsEntry(
        SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE, 10);
  }

  @Test
  public void testSetConfiguration_CsrfProtection_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.CSRF_PROTECTION, null));

    assertThat(dao.get(SystemConfigurationProperty.CSRF_PROTECTION)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(SetUtils.hashSet(SystemConfigurationProperty.CSRF_PROTECTION))).containsEntry(
        SystemConfigurationProperty.CSRF_PROTECTION, true);
  }

  @Test
  public void testSetConfiguration_CsrfProtection() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.CSRF_PROTECTION, false));

    assertThat(dao.get(SystemConfigurationProperty.CSRF_PROTECTION)).isEqualTo("false");
    assertThat(
        service.getConfigurationNoAuthz(SetUtils.hashSet(SystemConfigurationProperty.CSRF_PROTECTION))).containsEntry(
        SystemConfigurationProperty.CSRF_PROTECTION, false);
  }

  @Test
  public void testSetConfiguration_UserAgentSuffix_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.USER_AGENT_SUFFIX, null));

    assertThat(dao.get(SystemConfigurationProperty.USER_AGENT_SUFFIX)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(SetUtils.hashSet(SystemConfigurationProperty.USER_AGENT_SUFFIX))).containsEntry(
        SystemConfigurationProperty.USER_AGENT_SUFFIX, null);
  }

  @Test
  public void testSetConfiguration_UserAgentSuffix() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.USER_AGENT_SUFFIX, "test"));

    assertThat(dao.get(SystemConfigurationProperty.USER_AGENT_SUFFIX)).isEqualTo("test");
    assertThat(
        service.getConfigurationNoAuthz(SetUtils.hashSet(SystemConfigurationProperty.USER_AGENT_SUFFIX))).containsEntry(
        SystemConfigurationProperty.USER_AGENT_SUFFIX, "test");
  }

  @Test
  public void testSetConfiguration_CspEnabled_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.CSP_ENABLED, null));

    assertThat(dao.get(SystemConfigurationProperty.CSP_ENABLED)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(SetUtils.hashSet(SystemConfigurationProperty.CSP_ENABLED))).containsEntry(
        SystemConfigurationProperty.CSP_ENABLED, true);
  }

  @Test
  public void testSetConfiguration_CspEnabled() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.CSP_ENABLED, false));

    assertThat(dao.get(SystemConfigurationProperty.CSP_ENABLED)).isEqualTo("false");
    assertThat(
        service.getConfigurationNoAuthz(SetUtils.hashSet(SystemConfigurationProperty.CSP_ENABLED))).containsEntry(
        SystemConfigurationProperty.CSP_ENABLED, false);
  }

  @Test
  public void testSetConfiguration_AccessAllowlist_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.ACCESS_ALLOWLIST, null));

    assertThat(dao.get(SystemConfigurationProperty.ACCESS_ALLOWLIST)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(SetUtils.hashSet(SystemConfigurationProperty.ACCESS_ALLOWLIST))).containsEntry(
        SystemConfigurationProperty.ACCESS_ALLOWLIST, null);
  }

  @Test
  public void testSetConfiguration_AccessAllowlist() {
    Map<String, String> values  = new HashMap<>();
    values.put("ipAddress", "192.168.33.10");
    values.put("description", "Test IPv4 address");
    List<Map<String, String>> allowlist = Collections.singletonList(values);

    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.ACCESS_ALLOWLIST, allowlist));

    assertThat(dao.get(SystemConfigurationProperty.ACCESS_ALLOWLIST))
        .isEqualTo("[{\"ipAddress\":\"192.168.33.10\",\"description\":\"Test IPv4 address\"}]");

    List<AllowedIp> allowlistIPs = (List<AllowedIp>) service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.ACCESS_ALLOWLIST))
        .get(SystemConfigurationProperty.ACCESS_ALLOWLIST);

    assertThat(allowlistIPs).extracting(AllowedIp::getIpAddress)
        .containsExactlyInAnyOrder("192.168.33.10");
    assertThat(allowlistIPs).extracting(AllowedIp::getDescription)
        .containsExactlyInAnyOrder("Test IPv4 address");
  }

  @Test
  public void testSetConfiguration_AccessAllowlistIsNotAllowedWithoutLicence() {
    Map<String, String> values  = new HashMap<>();
    values.put("ipAddress", "192.168.33.10");
    values.put("description", "Test IPv4 address");
    List<Map<String, String>> allowlist = Collections.singletonList(values);

    // Remove the PRODUCT_LIFECYCLE_CLOUD feature flag LicensedFeature.IP_ALLOWLIST
    testProductLicense.setFeatures(LicensedFeature.DASHBOARD);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
            service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.ACCESS_ALLOWLIST, allowlist)))
        .withMessageContaining(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testSetConfiguration_BlockSemicolonInPath_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH, null));

    assertThat(dao.get(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH))).containsEntry(
        SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH, true);
  }

  @Test
  public void testSetConfiguration_BlockSemicolonInPath() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH, false));

    assertThat(dao.get(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH)).isEqualTo("false");
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH))).containsEntry(
        SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH, false);
  }

  @Test
  public void testSetConfiguration_BlockBackslashInPath_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH, null));

    assertThat(dao.get(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH))).containsEntry(
        SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH, true);
  }

  @Test
  public void testSetConfiguration_BlockBackslashInPath() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH, false));

    assertThat(dao.get(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH)).isEqualTo("false");
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH))).containsEntry(
        SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH, false);
  }

  @Test
  public void testSetConfiguration_BlockNonAsciiInPath_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH, null));

    assertThat(dao.get(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH))).containsEntry(
        SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH, false);
  }

  @Test
  public void testSetConfiguration_BlockNonAsciiInPath() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH, true));

    assertThat(dao.get(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH)).isEqualTo("true");
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH))).containsEntry(
        SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH, true);
  }

  @Test
  public void testSetConfiguration_ReleaseGraphCacheSize_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE, null));

    assertThat(dao.get(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE))).containsEntry(
        SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE, 1000);
  }

  @Test
  public void testSetConfiguration_ReleaseGraphCacheSize() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE, 10));

    assertThat(dao.get(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE)).isEqualTo("10");
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE))).containsEntry(
        SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE, 10);
  }

  @Test
  public void testSetConfiguration_LicenseLegalHdsRequestLimit_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT, null));

    assertThat(dao.get(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT))).containsEntry(
        SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT, 50);
  }

  @Test
  public void testSetConfiguration_LicenseLegalHdsRequestLimit() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT, 10));

    assertThat(dao.get(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT)).isEqualTo("10");
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT))).containsEntry(
        SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT, 10);
  }

  @Test
  public void testSetConfiguration_MaxApplicationsToQueryOnDashboard_Null() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD, null));

    assertThat(dao.get(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD))).containsEntry(
        SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD, 0);
  }

  @Test
  public void testSetConfiguration_MaxApplicationsToQueryOnDashboard() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD, 10));

    assertThat(dao.get(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD)).isEqualTo("10");
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD))).containsEntry(
        SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD, 10);
  }

  @Test
  public void testSetConfiguration_MaxAdvancedSearchClauseCount_Null() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT, null));

    assertThat(dao.get(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT))).containsEntry(
        SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT, 2048);
  }

  @Test
  public void testSetConfiguration_MaxAdvancedSearchClauseCount() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT, 10));

    assertThat(dao.get(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT)).isEqualTo("10");
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT))).containsEntry(
        SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT, 10);
  }

  @Test
  public void testSetConfiguration_AdvancedSearchCSVExportDelimiter_Null() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER, null));

    assertThat(dao.get(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER))).containsEntry(
        SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER, ",");
  }

  @Test
  public void testSetConfiguration_AdvancedSearchCSVExportDelimiter() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER, ";"));

    assertThat(dao.get(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER)).isEqualTo(";");
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER))).containsEntry(
        SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER, ";");
  }

  @Test
  public void testSetConfiguration_ConnectTimeoutInSeconds_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS, null));

    assertThat(dao.get(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS)).isNull();
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS))).containsEntry(
        SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS, 20);
  }

  @Test
  public void testSetConfiguration_ConnectTimeoutInSeconds() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS, 10));

    assertThat(dao.get(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS)).isEqualTo("10");
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS))).containsEntry(
        SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS, 10);
    assertMinAndMax(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS, 5, 60 * 60);
  }

  @Test
  public void testSetConfiguration_SocketTimeoutInSeconds_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS, null));

    assertThat(dao.get(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS)).isNull();
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS))).containsEntry(
        SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS, 3 * 60);
  }

  @Test
  public void testSetConfiguration_SocketTimeoutInSeconds() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS, 10));

    assertThat(dao.get(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS)).isEqualTo("10");
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS))).containsEntry(
        SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS, 10);
    assertMinAndMax(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS, 5, 60 * 60);
  }

  @Test
  public void testSetConfiguration_ReportTimeoutInSeconds_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS, null));

    assertThat(dao.get(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS)).isNull();
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS))).containsEntry(
        SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS, 35 * 60);
  }

  @Test
  public void testSetConfiguration_ReportTimeoutInSeconds() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS, 35));

    assertThat(dao.get(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS)).isEqualTo("35");
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS))).containsEntry(
        SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS, 35);
    assertMinAndMax(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS, 30, 60 * 60);
  }

  @Test
  public void testSetConfiguration_NeedsAcknowledgementOfInitialDashboardFilter_Null() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER, null));

    assertThat(dao.get(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER)).isNull();
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER))).containsEntry(
        SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER, false);
  }

  @Test
  public void testSetConfiguration_NeedsAcknowledgementOfInitialDashboardFilter() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER, true));

    assertThat(dao.get(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER)).isEqualTo(
        "true");
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER))).containsEntry(
        SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER, true);
  }

  @Test
  public void testSetConfiguration_EnableDefaultPasswordWarning_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING, null));

    assertThat(dao.get(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING)).isNull();
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING))).containsEntry(
        SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING, true);
  }

  @Test
  public void testSetConfiguration_EnableDefaultPasswordWarning() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING, false));

    assertThat(dao.get(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING)).isEqualTo("false");
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING))).containsEntry(
        SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING, false);
  }

  @Test
  public void testSetConfiguration_PolicyMonitoringHour_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.POLICY_MONITORING_HOUR, null));

    assertThat(dao.get(SystemConfigurationProperty.POLICY_MONITORING_HOUR)).isNull();
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.POLICY_MONITORING_HOUR))).containsEntry(
        SystemConfigurationProperty.POLICY_MONITORING_HOUR, 0);
  }

  @Test
  public void testSetConfiguration_PolicyMonitoringHour() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.POLICY_MONITORING_HOUR, 22));

    assertThat(dao.get(SystemConfigurationProperty.POLICY_MONITORING_HOUR)).isEqualTo("22");
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.POLICY_MONITORING_HOUR))).containsEntry(
        SystemConfigurationProperty.POLICY_MONITORING_HOUR, 22);
    assertMinAndMax(SystemConfigurationProperty.POLICY_MONITORING_HOUR, 0, 23);
  }

  @Test
  public void testSetConfiguration_DbBackupDir_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.DB_BACKUP_DIR, null));

    assertThat(dao.get(SystemConfigurationProperty.DB_BACKUP_DIR)).isNull();
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.DB_BACKUP_DIR))).containsEntry(
        SystemConfigurationProperty.DB_BACKUP_DIR,
        new File(insightConfig.getSonatypeWork(), InsightConfig.DEFAULT_BACKUP_DIR).getAbsolutePath());
  }

  @Test
  public void testSetConfiguration_DbBackupDir() throws Exception {
    String dbBackupDir = InsightConfig.DEFAULT_BACKUP_DIR + "-2";
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.DB_BACKUP_DIR, dbBackupDir));

    assertThat(dao.get(SystemConfigurationProperty.DB_BACKUP_DIR)).isEqualTo(dbBackupDir);
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.DB_BACKUP_DIR))).containsEntry(
        SystemConfigurationProperty.DB_BACKUP_DIR,
        new File(insightConfig.getSonatypeWork(), dbBackupDir).getAbsolutePath());

    String absolutePath = tempDir.newFolder().getAbsolutePath();
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.DB_BACKUP_DIR, absolutePath));
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.DB_BACKUP_DIR))).containsEntry(
        SystemConfigurationProperty.DB_BACKUP_DIR, absolutePath);
  }

  @Test
  public void testSetConfiguration_WebhookSecretPassphrase_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE, null));

    assertThat(dao.get(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE)).isNull();
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE))).containsEntry(
        SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE, "^d1swM!FF&qQ");
  }

  @Test
  public void testSetConfiguration_WebhookSecretPassphrase() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE, "custom"));

    assertThat(dao.get(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE)).isEqualTo("custom");
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE))).containsEntry(
        SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE, "custom");
  }

  @Test
  public void testSetConfiguration_ExternalHyperlinksAllowed_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED, null));

    assertThat(dao.get(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED)).isNull();
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED))).containsEntry(
        SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED, true);
  }

  @Test
  public void testSetConfiguration_ExternalHyperlinksAllowed() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED, false));

    assertThat(dao.get(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED)).isEqualTo("false");
    assertThat(
        service.getConfigurationNoAuthz(
            SetUtils.hashSet(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED))).containsEntry(
        SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED, false);
  }

  @Test
  public void testSetConfiguration_MatcherConfiguration_DisableConanNamespaceMatching_Null() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING, null));

    assertThat(dao.get(SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING)).isNull();
    assertThat(service.getConfigurationNoAuthz(SetUtils.hashSet(
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING))).containsEntry(
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING, false);
  }

  @Test
  public void testSetConfiguration_MatcherConfiguration_DisableConanNamespaceMatching() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING, true));

    assertThat(dao.get(SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING)).isEqualTo(
        "true");
    assertThat(service.getConfigurationNoAuthz(SetUtils.hashSet(
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING))).containsEntry(
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING, true);
  }

  @Test
  public void testSetConfiguration_SchemaMigrationEnabled_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, null));

    assertThat(dao.get(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED)).isNull();
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED))).containsEntry(
        SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, true);
  }

  @Test
  public void testSetConfiguration_SchemaMigrationEnabled() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, false));

    assertThat(dao.get(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED)).isEqualTo("false");
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED))).containsEntry(
        SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED, false);
  }

  @Test
  public void testSetConfiguration_BfsArtifactoryExpiredTokenRegex_Null() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_REGEX, null));

    assertThat(dao.get(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_REGEX)).isNull();
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_REGEX))).containsEntry(
        SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_REGEX, "(?i)(?s).*token[\\s\\w:]+expired.*");
  }

  @Test
  public void testSetConfiguration_BfsArtifactoryExpiredTokenRegex() {
    String regex = "(?s).*token[\\s\\w:]+outdated.*";
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_REGEX, regex));

    assertThat(dao.get(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_REGEX)).isEqualTo(regex);
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_REGEX))).containsEntry(
        SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_REGEX, regex);
  }

  @Test
  public void testSetConfiguration_BfsArtifactoryExpiredTokenEmail_Null() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL, null));

    assertThat(dao.get(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL)).isNull();
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL))).containsEntry(
        SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL, null);
  }

  @Test
  public void testSetConfiguration_BfsArtifactoryExpiredTokenEmail() {
    String email = "username@domain";
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL, email));

    assertThat(dao.get(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL)).isEqualTo(email);
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL))).containsEntry(
        SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL, email);
  }

  @Test
  public void testSetConfiguration_BfsComponentQueryLimit_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT, null));

    assertThat(dao.get(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT)).isNull();
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT)))
        .containsEntry(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT, null);
  }

  @Test
  public void testSetConfiguration_BfsComponentQueryLimit() {
    Integer limit = 10;
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT, limit));

    assertThat(dao.get(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT)).isEqualTo(limit.toString());
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT)))
        .containsEntry(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT, limit);
    assertMinAndMax(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT, 0, Integer.MAX_VALUE);
  }

  @Test
  public void testSetConfiguration_BfsQueryRepositoriesList_Null() {
    service.setConfigurationNoAuthz(Maps.newHashMap(SystemConfigurationProperty.BFS_REPOSITORIES, null));

    assertThat(dao.get(SystemConfigurationProperty.BFS_REPOSITORIES)).isNull();
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.BFS_REPOSITORIES)))
        .containsEntry(SystemConfigurationProperty.BFS_REPOSITORIES, null);
  }

  @Test
  public void testSetConfiguration_BfsQueryRepositoriesList() {
    String repos = "repo1,repo2,repo3";
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.BFS_REPOSITORIES, repos));
    assertThat(dao.get(SystemConfigurationProperty.BFS_REPOSITORIES)).isEqualTo(repos);
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(SystemConfigurationProperty.BFS_REPOSITORIES)))
        .containsEntry(SystemConfigurationProperty.BFS_REPOSITORIES, repos);
  }

  private void assertMinAndMax(String name, int min, int max) {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.setConfigurationNoAuthz(name, min - 1))
        .withMessageContaining(String.format(ConfigurationUtils.OUTSIDE_RANGE_ERROR_MSG, name, min, max));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.setConfigurationNoAuthz(name, max + 1))
        .withMessageContaining(String.format(ConfigurationUtils.OUTSIDE_RANGE_ERROR_MSG, name, min, max));
    service.setConfigurationNoAuthz(name, min);
    assertThat(dao.get(name)).isEqualTo(Integer.toString(min));
    assertThat(service.getConfigurationNoAuthz(SetUtils.hashSet(name))).containsEntry(name, min);
    service.setConfigurationNoAuthz(name, max);
    assertThat(dao.get(name)).isEqualTo(Integer.toString(max));
    assertThat(service.getConfigurationNoAuthz(SetUtils.hashSet(name))).containsEntry(name, max);
  }

  @Test
  public void testSetConfiguration_AutomaticQuarantineReleaseTimeIntervalInMinutes() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES, 35));

    assertThat(dao.get(SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES)).isEqualTo(
        "35");
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(
            SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES))).containsEntry(
        SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES, 35);
    assertMinAndMax(SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES, 30,
        Integer.MAX_VALUE);
  }

  @Test
  public void testSetConfiguration_AutomaticQuarantineReleaseTimeIntervalInMinutes_Null() {
    service.setConfigurationNoAuthz(
        Maps.newHashMap(SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES, null));

    assertThat(dao.get(SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES)).isNull();
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(
            SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES))).containsEntry(
        SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES, 60);
  }

  @Test
  public void testGetConfiguration_AutomaticQuarantineReleaseTimeIntervalInMinutesNotSet_ReturnsDefault() {
    assertThat(service.getConfigurationNoAuthz(
        SetUtils.hashSet(
            SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES))).containsEntry(
        SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES, 60);
  }
}
