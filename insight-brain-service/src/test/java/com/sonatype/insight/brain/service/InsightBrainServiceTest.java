/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Date;
import java.util.List;
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
import com.sonatype.insight.brain.telemetry.TelemetryCollector;
import com.sonatype.insight.brain.telemetry.TelemetryData;
import com.sonatype.insight.brain.telemetry.TelemetryHeader;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.mock.hds.HttpResponseProcessor;

import io.dropwizard.logging.AppenderFactory;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.logging.FileAppenderFactory;
import io.dropwizard.logging.SyslogAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.server.AbstractServerFactory;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class InsightBrainServiceTest
    extends AbstractBrainServiceTest
{
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
    tempEntity.register(sampleApp);

    assertThat(sampleOrg, is(notNullValue()));
    assertThat(sampleApp, is(notNullValue()));
  }

  @Test
  public void testCreateSampleData_Disabled() {
    // The creation of the sample data is disabled by default.
    Organization sampleOrg = new OrganizationDAO().getByName(SampleDataCreator.SAMPLE_ORGANIZATION_NAME);
    assertThat(sampleOrg, is(nullValue()));
    Application sampleApp = new ApplicationDAO().getByName(SampleDataCreator.SAMPLE_APPLICATION_NAME);
    assertThat(sampleApp, is(nullValue()));
  }

  @Test
  public void testRun_TelemetryIsCalled() throws Exception {
    final int[] status = new int[1];
    final ByteArrayDataSource[] multipartDataSources = new ByteArrayDataSource[1];
    VersionService versionService = getCLMServer().getInjector().getInstance(VersionService.class);
    TelemetryId telemetryId = getCLMServer().getInjector().getInstance(TelemetryId.class);

    Date expectedMinCreateTime = new Date();
    getHdsServer().setResponseForURI(TelemetrySender.RESOURCE_PATH, (HttpResponseProcessor) (request, response) -> {
      status[0] = response.getStatus();
      multipartDataSources[0] = new ByteArrayDataSource(request.getInputStream(), "multipart/form-data");
    }, 204);
    getCLMServer().stop();
    getCLMServer().start();
    await().atMost(5, SECONDS).until(() -> status[0] != 0);
    Date expectedMaxCreateTime = new Date();
    MimeMultipart multipart = new MimeMultipart(multipartDataSources[0]);
    BodyPart bodyPart = multipart.getBodyPart(0);
    String filename = bodyPart.getFileName();
    assertThat(TelemetrySender.ZIP_FILENAME, is(filename));
    assertThat(status[0], is(204));
    try (ZipInputStream zipInputStream = new ZipInputStream(bodyPart.getInputStream())) {
      byte[] buffer = new byte[1024];

      ZipEntry zipEntryHeader = zipInputStream.getNextEntry();
      assertThat(zipEntryHeader.getName(), is(TelemetrySender.HEADER_ENTRY_NAME));
      zipInputStream.read(buffer);
      TelemetryHeader telemetryHeaderReceived = JsonUtils.parse(buffer, TelemetryHeader.class);
      assertThat(telemetryHeaderReceived.getCreateTime(), greaterThanOrEqualTo(expectedMinCreateTime));
      assertThat(telemetryHeaderReceived.getCreateTime(), lessThanOrEqualTo(expectedMaxCreateTime));
      assertThat(telemetryHeaderReceived.getTelemetryId(), is(telemetryId.getId()));
      assertThat(telemetryHeaderReceived.getProduct(),
          is(TelemetrySender.PRODUCT_PREFIX + "/" + versionService.getVersion()));
      assertThat(telemetryHeaderReceived.getFormat(), is(TelemetrySender.FILE_FORMAT));

      ZipEntry zipEntryData = zipInputStream.getNextEntry();
      assertThat(zipEntryData.getName(), is(TelemetrySender.DATA_ENTRY_NAME));
      zipInputStream.read(buffer);
      TelemetryData telemetryDataReceived = JsonUtils.parse(buffer, TelemetryData.class);
      assertThat(telemetryDataReceived.getAttributes().get(TelemetryCollector.NUMBER_OF_ORGS), is("0"));
      assertThat(telemetryDataReceived.getAttributes().get(TelemetryCollector.NUMBER_OF_APPS), is("0"));
      assertThat(telemetryDataReceived.getAttributes().get(TelemetryCollector.MAX_APPS_PER_ORG), is("0"));
      assertThat(telemetryDataReceived.getAttributes().get(TelemetryCollector.MIN_APPS_PER_ORG), is("0"));
      assertThat(telemetryDataReceived.getAttributes().get(TelemetryCollector.P90_APPS_PER_ORG), is("0"));
    }
  }

  @Test
  public void testRun_TelemetryFail() throws Exception {
    final HttpServletResponse[] responses = new HttpServletResponse[1];
    getHdsServer().setResponseForURI(TelemetrySender.RESOURCE_PATH, (HttpResponseProcessor) (request, response) -> {
      responses[0] = response;
      throw new RuntimeException();
    }, 204);
    getCLMServer().stop();
    getCLMServer().start();
    await().atMost(5, SECONDS).until(() -> responses[0] != null);
    HttpResponse response = adminRequest().path("/healthcheck").get();
    assertResponseStatus(200, response);
  }

  @Test
  @ManualServerInit
  public void testConfigWithHttp_SuggestsUpdateConfig() throws Exception {
    try {
      initServer(new Configurator()
      {
        @Override
        public void configure(final InsightConfig config) { }

        @Override
        public String getConfigFilePath() {
          return InsightBrainService.class.getResource("/InsightBrainServiceTest/config-with-http.yml").getFile();
        }
      });
      fail("Expected exception");
    }
    catch (RuntimeException ex) {
      assertThat(ex.getMessage(), is(ConfigurationChecker.SUGGEST_UPDATE_CONFIG_EXCEPTION_MESSAGE));
    }
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
    assertThat(((ConsoleAppenderFactory<?>) accessAppenders.get(0)).getLogFormat(),
        is(InsightConfigurationFactory.DEFAULT_REQUEST_LOG_FORMAT));
    assertThat(((FileAppenderFactory<?>) accessAppenders.get(1)).getLogFormat(),
        is(InsightConfigurationFactory.DEFAULT_REQUEST_LOG_FORMAT));
    assertThat(((SyslogAppenderFactory) accessAppenders.get(2)).getLogFormat(),
        is(InsightConfigurationFactory.DEFAULT_REQUEST_LOG_FORMAT));
  }
}
