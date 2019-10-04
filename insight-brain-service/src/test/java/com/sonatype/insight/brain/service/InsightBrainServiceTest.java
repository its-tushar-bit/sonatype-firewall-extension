/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.organization.SampleDataCreator;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.telemetry.DatabaseTelemetryCollector;
import com.sonatype.insight.brain.telemetry.HierarchyMetricsTelemetryCollector;
import com.sonatype.insight.brain.telemetry.PolicyStatusOverrideTelemetryCollector;
import com.sonatype.insight.brain.telemetry.PropertiesTelemetryCollector;
import com.sonatype.insight.brain.telemetry.RealmTelemetryCollector;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.mock.hds.HttpResponseProcessor;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryHeader;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;

import io.dropwizard.logging.AppenderFactory;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.logging.FileAppenderFactory;
import io.dropwizard.logging.SyslogAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.server.AbstractServerFactory;
import io.dropwizard.server.DefaultServerFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Rule;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InsightBrainServiceTest
    extends AbstractBrainServiceTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(InsightBrainService.class);

  @Test
  @ManualServerInit
  public void testCreateSampleData_Enabled() throws Exception {
    initServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
        config.setCreateSampleData(true);
      }
    });

    Organization sampleOrg = new OrganizationDAO().getByName(SampleDataCreator.SAMPLE_ORGANIZATION_NAME);
    tempEntity.register(sampleOrg);
    Application sampleApp = new ApplicationDAO().getByName(SampleDataCreator.SAMPLE_APPLICATION_NAME);

    assertThat(sampleOrg).isNotNull();
    assertThat(sampleApp).isNotNull();
  }

  @Test
  public void testCreateSampleData_Disabled() {
    // The creation of the sample data is disabled by default.
    Organization sampleOrg = new OrganizationDAO().getByName(SampleDataCreator.SAMPLE_ORGANIZATION_NAME);
    assertThat(sampleOrg).isNull();
    Application sampleApp = new ApplicationDAO().getByName(SampleDataCreator.SAMPLE_APPLICATION_NAME);
    assertThat(sampleApp).isNull();
  }

  @Test
  @ManualServerInit
  public void testRun_TelemetryIsCalled() throws Exception {
    final Map<ByteArrayDataSource, Integer> responses = Collections.synchronizedMap(new LinkedHashMap<>());

    Date expectedMinCreateTime = new Date();
    initServer(config -> {
      getHdsServer().respondWith((HttpResponseProcessor) (request, response) -> {
        responses.put(new ByteArrayDataSource(request.getInputStream(), "multipart/form-data"), response.getStatus());
      }).andStatus(204).atUri(TelemetrySender.RESOURCE_PATH);
    });
    await().atMost(5, SECONDS).untilAsserted(() -> assertThat(responses).hasSize(5));
    Date expectedMaxCreateTime = new Date();

    VersionService versionService = getCLMServer().getInstance(VersionService.class);
    TelemetryId telemetryId = getCLMServer().getInstance(TelemetryId.class);
    List<TelemetryPurpose> telemetryPurposes = new ArrayList<>();
    for (Map.Entry<ByteArrayDataSource, Integer> response : responses.entrySet()) {
      Integer status = response.getValue();
      MimeMultipart multipart = new MimeMultipart(response.getKey());
      BodyPart bodyPart = multipart.getBodyPart(0);
      String filename = bodyPart.getFileName();
      assertThat(TelemetrySender.ZIP_FILENAME).isEqualTo(filename);
      assertThat(status).isEqualTo(204);
      try (ZipInputStream zipInputStream = new ZipInputStream(bodyPart.getInputStream())) {
        byte[] buffer = new byte[1024];

        ZipEntry zipEntryHeader = zipInputStream.getNextEntry();
        assertThat(zipEntryHeader.getName()).isEqualTo(TelemetrySender.HEADER_ENTRY_NAME);
        zipInputStream.read(buffer);
        TelemetryHeader telemetryHeaderReceived = JsonUtils.parse(buffer, TelemetryHeader.class);
        assertThat(telemetryHeaderReceived.getCreateTime()).isAfterOrEqualTo(expectedMinCreateTime)
            .isBeforeOrEqualTo(expectedMaxCreateTime);
        assertThat(telemetryHeaderReceived.getTelemetryId()).isEqualTo(telemetryId.getId());
        assertThat(telemetryHeaderReceived.getProduct())
            .isEqualTo(TelemetrySender.PRODUCT_PREFIX + "/" + versionService.getVersion());
        assertThat(telemetryHeaderReceived.getFormat()).isEqualTo(TelemetrySender.FILE_FORMAT);

        ZipEntry zipEntryData = zipInputStream.getNextEntry();
        assertThat(zipEntryData.getName()).isEqualTo(TelemetrySender.DATA_ENTRY_NAME);
        zipInputStream.read(buffer);
        TelemetryData telemetryDataReceived = JsonUtils.parse(buffer, TelemetryData.class);
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
          default:
            fail("Unexpected telemetry purpose: " + telemetryPurpose);
            break;
        }
      }
    }

    assertThat(telemetryPurposes).containsExactlyInAnyOrder(TelemetryPurpose.HIERARCHY_METRICS,
        TelemetryPurpose.POLICY_STATUS_OVERRIDE, TelemetryPurpose.DATABASE, TelemetryPurpose.CONFIGURATION_PROPERTIES,
        TelemetryPurpose.REALM);
  }

  @Test
  @ManualServerInit
  public void testRun_TelemetryFail() throws Exception {
    final HttpServletResponse[] responses = new HttpServletResponse[1];
    initServer(config -> {
      getHdsServer().respondWith((HttpResponseProcessor) (request, response) -> {
        responses[0] = response;
        throw new RuntimeException();
      }).atUri(TelemetrySender.RESOURCE_PATH);
    });
    await().atMost(5, SECONDS).until(() -> responses[0] != null);
    HttpResponse response = adminRequest().path("/healthcheck").get();
    assertResponseStatus(200, response);
  }

  @Test
  @ManualServerInit
  public void testConfigWithHttp_SuggestsUpdateConfig() throws Exception {
    assertThatThrownBy(() -> {
      initServer(new Configurator()
      {
        @Override
        public void configure(final InsightConfig config) {
        }

        @Override
        public String getConfigFilePath() {
          return InsightBrainService.class.getResource("/InsightBrainServiceTest/config-with-http.yml").getFile();
        }
      });
    }).isInstanceOf(RuntimeException.class).hasMessage(ConfigurationChecker.SUGGEST_UPDATE_CONFIG_EXCEPTION_MESSAGE);
  }

  @Test
  @ManualServerInit
  public void testConfigWithoutLogFormats_UsesOurDefaultRequestLogFormat() throws Exception {
    initServer(new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) { }

      @Override
      public String getConfigFilePath() {
        return InsightBrainService.class.getResource("/InsightBrainServiceTest/config-without-request-log-formats.yml")
            .getFile();
      }
    });
    InsightConfig insightConfig = getCLMServer().getConfiguration();

    LogbackAccessRequestLogFactory logbackAccessRequestLogFactory = (LogbackAccessRequestLogFactory) 
        ((AbstractServerFactory) insightConfig.getServerFactory()).getRequestLogFactory();
    List<? extends AppenderFactory<?>> accessAppenders = logbackAccessRequestLogFactory.getAppenders();
    assertThat(((ConsoleAppenderFactory<?>) accessAppenders.get(0)).getLogFormat())
        .isEqualTo(InsightConfigurationFactory.DEFAULT_REQUEST_LOG_FORMAT);
    assertThat(((FileAppenderFactory<?>) accessAppenders.get(1)).getLogFormat())
        .isEqualTo(InsightConfigurationFactory.DEFAULT_REQUEST_LOG_FORMAT);
    assertThat(((SyslogAppenderFactory) accessAppenders.get(2)).getLogFormat())
        .isEqualTo(InsightConfigurationFactory.DEFAULT_REQUEST_LOG_FORMAT);
  }

  @Test
  @ManualServerInit
  public void testPrintVersion() throws Exception {
    // Manually initialize server with custom configurator to ensure it gets restarted if already running
    initServer(config -> {
    });
    assertThat(logOutput).atInfoLevel()
        .contains("Initializing Nexus IQ Server 1 release " + new VersionService().getLogDisplayVersion());
  }

  @Test
  @ManualServerInit
  public void testStartupWithoutLicense() throws Exception {
    getTestProductLicenseManager().uninstallLicense();
    // Manually initialize server with custom configurator to ensure it gets restarted if already running
    initServer(config -> {
    });
  }

  @Test
  public void testStartupFailsIfSonatypeWorkIsInUse() {
    TestCLMServer testCLMServerTwo = new TestCLMServer(false, null, null);
    try {
      assertThatExceptionOfType(IllegalStateException.class).isThrownBy(testCLMServerTwo::start)
          .withStackTraceContaining(
              "Work directory " + getCLMServer().getWorkDir().getAbsolutePath() + " is already in use.");
    }
    finally {
      testCLMServerTwo.stop();
    }
  }

  @Test
  @ManualServerInit
  public void testReleasesLockOnFailedRun() throws Exception {
    Configurator configurator = config -> {
      DefaultServerFactory mockDefaultServerFactory = mock(DefaultServerFactory.class);
      when(mockDefaultServerFactory.getApplicationConnectors()).thenThrow(new RuntimeException());
      config.setServerFactory(mockDefaultServerFactory);
    };
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> initServer(configurator));
    initServer(null);
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
    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    Security.insertProviderAt(new BouncyCastleProvider(), 1);

    InsightBrainService.ensureBouncyCastleProviderIsLowestPreference();

    assertThat(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)).isNotNull();
    Provider[] providers = Security.getProviders();
    assertThat(providers[providers.length - 1].getName()).isEqualTo(BouncyCastleProvider.PROVIDER_NAME);
  }
}
