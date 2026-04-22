/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiRoleMembershipResource;
import com.sonatype.insight.brain.api.v2.ApiUserResource;
import com.sonatype.insight.brain.dashboard.ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.DashboardComponentRiskService;
import com.sonatype.insight.brain.dashboard.DashboardViolationRiskService;
import com.sonatype.insight.brain.dashboard.H2ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.H2ComponentRiskService;
import com.sonatype.insight.brain.dashboard.H2DashboardViolationRiskService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.organization.ApplicationTelemetryCollector;
import com.sonatype.insight.brain.organization.SampleDataCreator;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.FIPSModeDetector;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.telemetry.ClusterIdentificationService;
import com.sonatype.insight.brain.telemetry.ClusterTelemetryTask;
import com.sonatype.insight.brain.telemetry.DatabaseTelemetryCollector;
import com.sonatype.insight.brain.telemetry.DefaultTelemetryScheduler;
import com.sonatype.insight.brain.telemetry.HierarchyMetricsTelemetryCollector;
import com.sonatype.insight.brain.telemetry.PolicyStatusOverrideTelemetryCollector;
import com.sonatype.insight.brain.telemetry.PropertiesTelemetryCollector;
import com.sonatype.insight.brain.telemetry.RealmTelemetryCollector;
import com.sonatype.insight.brain.telemetry.RestEndpointTelemetry;
import com.sonatype.insight.brain.telemetry.SourceControlMetricsTelemetryCollector;
import com.sonatype.insight.brain.telemetry.SourceControlRateLimitTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetryContainerRequestFilter;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.brain.testing.DefaultInsightBrainServiceFactory;
import com.sonatype.insight.brain.version.DefaultVersionService;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.mock.hds.HttpResponseProcessor;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryHeader;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationParsingException;
import io.dropwizard.configuration.ConfigurationValidationException;
import io.dropwizard.core.server.AbstractServerFactory;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.logging.common.AppenderFactory;
import io.dropwizard.logging.common.ConsoleAppenderFactory;
import io.dropwizard.logging.common.FileAppenderFactory;
import io.dropwizard.logging.common.SyslogAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.MockedStatic;
import org.quartz.JobPersistenceException;

import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.telemetry.PaginatedTelemetryCollectorImpl.DATA_LIST;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class InsightBrainServiceTest
    extends AbstractBrainServiceIntegrationTest
{
  private static final TelemetryPurpose[] EXPECTED_TELEMETRY_PURPOSES = {
    TelemetryPurpose.HIERARCHY_METRICS, TelemetryPurpose.POLICY_STATUS_OVERRIDE, TelemetryPurpose.DATABASE,
    TelemetryPurpose.CONFIGURATION_PROPERTIES, TelemetryPurpose.REALM, TelemetryPurpose.SOURCE_CONTROL_METRICS,
    TelemetryPurpose.SOURCE_CONTROL_RATE_LIMITS, TelemetryPurpose.ROLE_USAGE, TelemetryPurpose.RUNTIME_ENVIRONMENT,
    TelemetryPurpose.REPOSITORY_CONFIGURATION, TelemetryPurpose.CLUSTER_USAGE, TelemetryPurpose.REAL_OWNER_IDS,
    TelemetryPurpose.REAL_OWNER_IDS, // One for apps and another for organizations
    TelemetryPurpose.APPLICATION_CATEGORY,
    TelemetryPurpose.CLUSTER_IDENTITY
  };

  @Rule
  public LogOutput logOutput = new LogOutput(InsightBrainService.class);

  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule
  public LogOutput logOutputFIPSModeDetector = new LogOutput(FIPSModeDetector.class);

  private OrganizationDAO organizationDAO;

  private ApplicationDAO applicationDAO;

  private QuartzJobStoreTX quartzJobStoreTX;

  @Before
  public void before() throws JobPersistenceException {
    organizationDAO = lookup(OrganizationDAO.class);
    applicationDAO = lookup(ApplicationDAO.class);

    quartzJobStoreTX = mock(QuartzJobStoreTX.class);
    when(quartzJobStoreTX.getSchedulerStateRecords()).thenReturn(Collections.nCopies(2, null));
  }

  @After
  public void after() {
    removeBouncyCastleFipsProvider();
  }

  @Test
  @ManualIqServerInit
  public void testCreateSampleData_Enabled() throws Exception {
    startIqTestServer(config -> config.setCreateSampleData(true));

    Organization sampleOrg = organizationDAO.getByName(SampleDataCreator.SAMPLE_ORGANIZATION_NAME);
    Application sampleApp = applicationDAO.getByName(SampleDataCreator.SAMPLE_APPLICATION_NAME);

    assertThat(sampleOrg).isNotNull();
    assertThat(sampleApp).isNotNull();
  }

  @Test
  public void testCreateSampleData_Disabled() {
    // The creation of the sample data is disabled by default.
    Organization sampleOrg = organizationDAO.getByName(SampleDataCreator.SAMPLE_ORGANIZATION_NAME);
    assertThat(sampleOrg).isNull();
    Application sampleApp = applicationDAO.getByName(SampleDataCreator.SAMPLE_APPLICATION_NAME);
    assertThat(sampleApp).isNull();
  }

  @Test
  @ManualIqServerInit
  public void testRun_TelemetryIsCalled() throws Exception {
    final Map<ByteArrayDataSource, Integer> responses = new ConcurrentHashMap<>();

    Date expectedMinCreateTime = new Date();
    startIqTestServer(config -> getHdsServer().respondWith((HttpResponseProcessor) (request, response) -> responses.put(
        new ByteArrayDataSource(request.getInputStream(), "multipart/form-data"), response.getStatus()))
        .andStatus(204)
        .atUri(TelemetrySender.RESOURCE_PATH));
    temporarilyEnableQuartzTelemetry();
    await().atMost(5, SECONDS).untilAsserted(() -> assertThat(responses).hasSize(EXPECTED_TELEMETRY_PURPOSES.length));
    Date expectedMaxCreateTime = new Date();
    Collection<TelemetryData> allTelemetryData =
        assertTelemetry(responses, expectedMinCreateTime, expectedMaxCreateTime);
    List<TelemetryPurpose> telemetryPurposes = new ArrayList<>();
    for (TelemetryData telemetryDataReceived : allTelemetryData) {
      TelemetryPurpose telemetryPurpose = telemetryDataReceived.getPurpose();
      telemetryPurposes.add(telemetryPurpose);
      switch (telemetryPurpose) {
        case HIERARCHY_METRICS:
          assertThat(telemetryDataReceived.getAttributes())
              .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_ORGS, "0")
              .containsEntry(HierarchyMetricsTelemetryCollector.NUMBER_OF_APPS, "0")
              .containsEntry(HierarchyMetricsTelemetryCollector.MAX_APPS_PER_ORG, "0")
              .containsEntry(HierarchyMetricsTelemetryCollector.MIN_APPS_PER_ORG, "0")
              .containsEntry(HierarchyMetricsTelemetryCollector.P90_APPS_PER_ORG, "0");
          break;
        case SOURCE_CONTROL_METRICS:
          assertThat(telemetryDataReceived.getAttributes())
              .containsEntry(SourceControlMetricsTelemetryCollector.TOTAL_SC_WITH_REMEDIATION_PRS_ENABLED, "0")
              .containsEntry(SourceControlMetricsTelemetryCollector.TOTAL_APPLICATION_SC_ENTRIES, "0")
              .containsEntry(SourceControlMetricsTelemetryCollector.TOTAL_APPLICATIONS, "0");
          break;
        case SOURCE_CONTROL_RATE_LIMITS:
          assertThat(telemetryDataReceived.getAttributes())
              .containsKey(SourceControlRateLimitTelemetry.SOURCE_CONTROL_RATE_LIMITS);
          break;
        case POLICY_STATUS_OVERRIDE:
          assertThat(telemetryDataReceived.getAttributes())
              .containsEntry(PolicyStatusOverrideTelemetryCollector.SECURITY_VULNERABILITY_OVERRIDE_COUNT, "0")
              .containsEntry(PolicyStatusOverrideTelemetryCollector.POLICY_WAIVER_COUNT, "0");
          break;
        case DATABASE:
          // The database is in memory, so the reported size is null.
          assertThat(telemetryDataReceived.getAttributes()).containsEntry(DatabaseTelemetryCollector.ODS_SIZE_BYTES,
              null);
          break;
        case CONFIGURATION_PROPERTIES:
          assertThat(telemetryDataReceived.getAttributes())
              .containsEntry(PropertiesTelemetryCollector.REPORT_TIMEOUT_SECONDS, 2100);
          break;
        case REALM:
          assertThat(telemetryDataReceived.getAttributes())
              .containsEntry(RealmTelemetryCollector.SAML_CONFIGURED, "false");
          break;
        case ROLE_USAGE:
        case RUNTIME_ENVIRONMENT:
        case REPOSITORY_CONFIGURATION:
        case CLUSTER_USAGE:
          assertThat(telemetryDataReceived.getAttributes()).isNotEmpty();
          break;
        case REAL_OWNER_IDS:
          assertThat(telemetryDataReceived.getAttributes())
              .containsKey(ApplicationTelemetryCollector.ALL_OWNER_IDS_NAMES);
          break;
        case APPLICATION_CATEGORY:
          assertThat(telemetryDataReceived.getAttributes())
              .containsKey(DATA_LIST);
          break;
        case CLUSTER_IDENTITY:
          assertThat(telemetryDataReceived.getAttributes())
              .containsKey(ClusterIdentificationService.RESOLUTION_OUTCOME);
          break;
        default:
          fail("Unexpected telemetry purpose: " + telemetryPurpose);
          break;
      }
    }
    assertThat(telemetryPurposes).containsOnly(EXPECTED_TELEMETRY_PURPOSES);
  }

  private Collection<TelemetryData> assertTelemetry(
      Map<ByteArrayDataSource, Integer> responses,
      Date expectedMinCreateTime,
      Date expectedMaxCreateTime) throws Exception
  {
    Collection<TelemetryData> allTelemetryData = new ArrayList<>();
    VersionService versionService = getCLMServer().getInstance(VersionService.class);
    TelemetryId telemetryId = getCLMServer().getInstance(TelemetryId.class);
    ObjectMapper objectMapper = new ObjectMapper().disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
    for (Map.Entry<ByteArrayDataSource, Integer> response : responses.entrySet()) {
      Integer status = response.getValue();
      MimeMultipart multipart = new MimeMultipart(response.getKey());
      BodyPart bodyPart = multipart.getBodyPart(0);
      String filename = bodyPart.getFileName();
      assertThat(TelemetrySender.ZIP_FILENAME).isEqualTo(filename);
      assertThat(status).isEqualTo(204);
      try (ZipInputStream zipInputStream = new ZipInputStream(bodyPart.getInputStream())) {
        ZipEntry zipEntryHeader = zipInputStream.getNextEntry();
        assertThat(zipEntryHeader.getName()).isEqualTo(TelemetrySender.HEADER_ENTRY_NAME);
        TelemetryHeader telemetryHeaderReceived = objectMapper.readValue(zipInputStream, TelemetryHeader.class);
        assertThat(telemetryHeaderReceived.getCreateTime()).isAfterOrEqualTo(expectedMinCreateTime)
            .isBeforeOrEqualTo(expectedMaxCreateTime);
        assertThat(telemetryHeaderReceived.getTelemetryId()).isEqualTo(telemetryId.getId());
        assertThat(telemetryHeaderReceived.getProduct())
            .isEqualTo(TelemetrySender.PRODUCT_PREFIX + "/" + versionService.getVersion());
        assertThat(telemetryHeaderReceived.getFormat()).isEqualTo(TelemetrySender.FILE_FORMAT);

        ZipEntry zipEntryData = zipInputStream.getNextEntry();
        assertThat(zipEntryData.getName()).isEqualTo(TelemetrySender.DATA_ENTRY_NAME);
        List<TelemetryData> telemetryDataReceived =
            objectMapper.readValue(zipInputStream, new TypeReference<List<TelemetryData>>()
            {
            });
        allTelemetryData.addAll(telemetryDataReceived);
      }
    }
    return allTelemetryData;
  }

  @Test
  @ManualIqServerInit
  public void testRun_TelemetryFail() throws Exception {
    final HttpServletResponse[] responses = new HttpServletResponse[1];
    startIqTestServer(config -> getHdsServer().respondWith((HttpResponseProcessor) (request, response) -> {
      responses[0] = response;
      throw new RuntimeException();
    }).atUri(TelemetrySender.RESOURCE_PATH));
    await().atMost(5, SECONDS).until(() -> responses[0] != null);
    HttpResponse response = adminRequest().path("/healthcheck").get();
    assertResponseStatus(200, response);
  }

  @Test
  @ManualIqServerInit
  public void testRestEndpointTelemetry() throws Exception {
    Map<ByteArrayDataSource, Integer> responses = new ConcurrentHashMap<>();
    Date expectedMinCreateTime = new Date();
    startIqTestServer(config -> getHdsServer().respondWith((HttpResponseProcessor) (request, response) -> responses.put(
        new ByteArrayDataSource(request.getInputStream(), "multipart/form-data"), response.getStatus()))
        .andStatus(204)
        .atUri(TelemetrySender.RESOURCE_PATH));

    assertResponseStatus(404,
        restRequest().path(PublicApiPaths.USER_RESOURCE_PATH_V2, ApiUserResource.USERNAME_PATH)
            .parameter("sensitiveUsername")
            .get());
    assertResponseStatus(404,
        restRequest().path(PublicApiPaths.USER_RESOURCE_PATH_V2, ApiUserResource.USERNAME_PATH)
            .parameter("otherUsername")
            .get());
    assertResponseStatus(404, restRequest()
        .path(PublicApiPaths.ROLE_MEMBERSHIP_PATH_V2, ApiRoleMembershipResource.APPLICATION_OR_ORGANIZATION)
        .parameter("organization", "orgId", "roleId", "user", "sensitiveUsername")
        .put());

    DefaultTelemetryScheduler defaultTelemetryScheduler = getCLMServer().getInstance(DefaultTelemetryScheduler.class);
    responses.clear();
    defaultTelemetryScheduler.getTelemetryRunnable().run();
    temporarilyEnableQuartzTelemetry();
    await().atMost(5, SECONDS).untilAsserted(() -> assertThat(responses).hasSize(15));
    Date expectedMaxCreateTime = new Date();
    Collection<TelemetryData> allTelemetryData =
        assertTelemetry(responses, expectedMinCreateTime, expectedMaxCreateTime);
    Collection<TelemetryData> restEndpointUsageTelemetryData =
        allTelemetryData.stream()
            .filter(t -> t.getPurpose().equals(TelemetryPurpose.REST_ENDPOINT_USAGE))
            .collect(Collectors.toList());
    RestEndpointTelemetry[] expected = new RestEndpointTelemetry[]{
      new RestEndpointTelemetry("GET", "/api/v2/users/{username}", 2),
      new RestEndpointTelemetry("PUT",
          "/api/v2/roleMemberships/{ownerType}/{internalOwnerId}/role/{roleId}/{memberType}/{memberName}", 1)
    };
    assertThat(restEndpointUsageTelemetryData)
        .extracting(t -> JsonUtils
            .asPojo(JsonUtils.asTree(t.getAttributes().get(TelemetryContainerRequestFilter.REST_ENDPOINT_TELEMETRY)),
                RestEndpointTelemetry.class))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(expected);
  }

  @Test
  @ManualIqServerInit
  public void testConfigWithHttp_SuggestsUpdateConfig() {
    assertThatThrownBy(() -> startIqTestServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class.getResource("/InsightBrainServiceTest/config-with-http.yml").getFile();
      }
    })).isInstanceOf(RuntimeException.class)
        .hasStackTraceContaining(InsightConfigurationFactory.SUGGEST_UPDATE_CONFIG_EXCEPTION_MESSAGE);
  }

  @Test
  @ManualIqServerInit
  public void testConfigWithoutLogFormats_UsesOurDefaultRequestLogFormat() throws Exception {
    startIqTestServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class.getResource("/InsightBrainServiceTest/config-without-request-log-formats.yml")
            .getFile();
      }
    });
    InsightConfig insightConfig = getCLMServer().getConfiguration();

    LogbackAccessRequestLogFactory logbackAccessRequestLogFactory =
        (LogbackAccessRequestLogFactory) ((AbstractServerFactory) insightConfig.getServerFactory())
            .getRequestLogFactory();
    List<? extends AppenderFactory<?>> accessAppenders = logbackAccessRequestLogFactory.getAppenders();
    assertThat(((ConsoleAppenderFactory<?>) accessAppenders.get(0)).getLogFormat())
        .isEqualTo(InsightConfigurationFactory.DEFAULT_REQUEST_LOG_FORMAT);
    assertThat(((FileAppenderFactory<?>) accessAppenders.get(1)).getLogFormat())
        .isEqualTo(InsightConfigurationFactory.DEFAULT_REQUEST_LOG_FORMAT);
    assertThat(((SyslogAppenderFactory) accessAppenders.get(2)).getLogFormat())
        .isEqualTo(InsightConfigurationFactory.DEFAULT_REQUEST_LOG_FORMAT);
  }

  @Test
  @ManualIqServerInit
  public void testPrintVersion() throws Exception {
    // Manually initialize server with custom configurator to ensure it gets restarted if already running
    startIqTestServer(config -> {
    });

    assertThat(logOutput).atInfoLevel()
        .contains("Initializing Nexus IQ Server 1 release " + new DefaultVersionService().getLogDisplayVersion());
  }

  @Test
  @ManualIqServerInit
  public void testStartupWithoutLicense() throws Exception {
    getTestProductLicenseManager().uninstallLicense();
    // Manually initialize server with custom configurator to ensure it gets restarted if already running
    startIqTestServer(config -> {
    });
  }

  @Test
  public void testStartupFailsIfSonatypeWorkIsInUse() {
    TestCLMServer testCLMServerTwo =
        new TestCLMServer(new DefaultInsightBrainServiceFactory(), false, null, null,
            databaseContainerRule.getDatabaseContainer());
    try {
      assertThatExceptionOfType(IllegalStateException.class).isThrownBy(testCLMServerTwo::start)
          .withStackTraceContaining(
              "Work directory " + getCLMServer().getConfiguration().getSonatypeWork().getAbsolutePath() +
                  " is already in use.");
    }
    finally {
      testCLMServerTwo.stop();
    }
  }

  @Test
  @ManualIqServerInit
  public void testReleasesLockOnFailedRun() throws Exception {
    Configurator configurator = config -> {
      DefaultServerFactory mockDefaultServerFactory = mock(DefaultServerFactory.class);
      when(mockDefaultServerFactory.getApplicationConnectors()).thenThrow(new RuntimeException());
      config.setServerFactory(mockDefaultServerFactory);
    };
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> startIqTestServer(configurator));
    startIqTestServer(null);
  }

  @Test
  public void testEnsureBouncyCastleProviderIsLowestPreference_BouncyCastleProviderDoesNotExist() {
    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);

    InsightBrainService.ensureBouncyCastleProviderIsLowestPreference();

    assertThat(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)).isNotNull();
    Provider[] providers = Security.getProviders();
    assertThat(providers[providers.length - 1].getName()).isEqualTo(BouncyCastleProvider.PROVIDER_NAME);
  }

  @Test
  public void testEnsureBouncyCastleProviderIsLowestPreference_BouncyCastleProviderIsNotLowestPreference() {
    insertBouncyCastleFipsProvider();

    InsightBrainService.ensureBouncyCastleProviderIsLowestPreference();

    assertThat(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)).isNotNull();
    Provider[] providers = Security.getProviders();
    assertThat(providers[providers.length - 1].getName()).isEqualTo(BouncyCastleProvider.PROVIDER_NAME);
  }

  @Test
  @ManualIqServerInit
  public void testHttpsConnector_SNI() throws Exception {
    startIqTestServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class.getResource("/InsightBrainServiceTest/config-with-https-sni.yml").getFile();
      }
    });
  }

  @Test
  @ManualIqServerInit
  public void testFeatures() throws Exception {
    startIqTestServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class
            .getResource("/InsightBrainServiceTest/config-with-feature-flags.yml")
            .getFile();
      }
    });
    InsightConfig config = getCLMServer().getConfiguration();
    assertThat(config.isFeatureEnabled("unspecifiedFeature")).isTrue();
    assertThat(config.isFeatureEnabled("enabledFeature")).isTrue();
    assertThat(config.isFeatureEnabled("disabledFeature")).isFalse();
  }

  private void temporarilyEnableQuartzTelemetry() throws Exception {
    TaskScheduler taskScheduler = getCLMServer().getInstance(TaskScheduler.class);
    ClusterTelemetryTask clusterTelemetryTask = getCLMServer().getInstance(ClusterTelemetryTask.class);

    taskScheduler.disableForTesting = false;
    clusterTelemetryTask.disableForTesting = false;

    taskScheduler.start();
    clusterTelemetryTask.register();

    // Note: this should be disabled again in AbstractBrainServiceTest.cleanupTest()
  }

  @Test
  public void testInitialize_correctDbBasedInstancesInjected() {
    assertThat(getCLMServer().getInstance(DashboardViolationRiskService.class)).isInstanceOf(
        H2DashboardViolationRiskService.class);
    assertThat(getCLMServer().getInstance(DashboardComponentRiskService.class))
        .isInstanceOf(H2ComponentRiskService.class);
    assertThat(getCLMServer().getInstance(ApplicationRiskService.class)).isInstanceOf(H2ApplicationRiskService.class);
  }

  @Test
  @ManualIqServerInit
  public void testInitialize_MaxConnections_BelowMin() {
    Configurator configurator = new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class
            .getResource("/InsightBrainServiceTest/config-with-max-connections-below-min.yml")
            .getFile();
      }
    };
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> startIqTestServer(configurator))
        .withCauseInstanceOf(ConfigurationValidationException.class)
        .withStackTraceContaining("database.maxConnections must be greater than or equal to 1");
  }

  @Test
  @ManualIqServerInit
  public void testInitialize_MaxConnections_AboveMax() {
    Configurator configurator = new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
      }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class
            .getResource("/InsightBrainServiceTest/config-with-max-connections-above-max.yml")
            .getFile();
      }
    };
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> startIqTestServer(configurator))
        .withCauseInstanceOf(ConfigurationParsingException.class)
        .withStackTraceContaining(
            "database.maxConnections; Numeric value (2147483648) out of range of int (-2147483648 - 2147483647)");
  }

  @Test
  @ManualIqServerInit
  public void testInitialize_FIPSEnabled_LogInfo() throws Exception {
    try (MockedStatic<FIPSModeDetector> mocked = mockStatic(FIPSModeDetector.class, CALLS_REAL_METHODS)) {
      insertBouncyCastleFipsProvider();

      when(FIPSModeDetector.isEnabled()).thenReturn(true);
      startIqTestServer(config -> {
      });

      assertThat(logOutput)
          .atInfoLevel()
          .contains("FIPS mode is enabled for Nexus IQ Server.");
    }
  }

  @Test
  @ManualIqServerInit
  public void testInitialize_FIPSDisabled_NoLogInfo() throws Exception {
    try (MockedStatic<FIPSModeDetector> mocked = mockStatic(FIPSModeDetector.class, CALLS_REAL_METHODS)) {
      when(FIPSModeDetector.isEnabled()).thenReturn(false);

      startIqTestServer(config -> {
      });

      assertThat(logOutput)
          .atInfoLevel()
          .doesNotContain("FIPS mode is enabled for Nexus IQ Server.");
    }
  }

  @Test
  @ManualIqServerInit
  public void testInitialize_FIPSEnabledByEnvironmentVariable_LogTrace() throws Exception {
    insertBouncyCastleFipsProvider();

    environmentVariables.set("FIPS_MODE_ENABLED", "true");

    startIqTestServer(config -> {
    });

    assertThat(logOutputFIPSModeDetector)
        .atTraceLevel()
        .contains("FIPS mode is enabled through environment variable FIPS_MODE_ENABLED");

    // still assert that the error log is printed
    assertThat(logOutput)
        .atInfoLevel()
        .contains("FIPS mode is enabled for Nexus IQ Server.");
  }

  @Test
  @ManualIqServerInit
  public void testInitialize_FIPSDisabledByEnvironmentVariable_LogTrace() throws Exception {
    environmentVariables.set("FIPS_MODE_ENABLED", "false");

    startIqTestServer(config -> {
    });

    assertThat(logOutputFIPSModeDetector)
        .atTraceLevel()
        .contains("FIPS mode is disabled through environment variable FIPS_MODE_ENABLED");

    assertThat(logOutput)
        .atErrorLevel()
        .doesNotContain("FIPS mode is enabled for Nexus IQ Server.");
  }

  @Test
  @ManualIqServerInit
  public void testInitialize_FIPSInvalidEnvironmentVariable_NoLogs() throws Exception {
    environmentVariables.set("FIPS_MODE_ENABLED", "notaboolean");

    startIqTestServer(config -> {
    });

    assertThat(logOutputFIPSModeDetector)
        .atDebugLevel()
        .doesNotContain("FIPS mode is disabled through environment variable FIPS_MODE_ENABLED");

    assertThat(logOutput)
        .atInfoLevel()
        .doesNotContain("FIPS mode is enabled for Nexus IQ Server.");
  }
}
