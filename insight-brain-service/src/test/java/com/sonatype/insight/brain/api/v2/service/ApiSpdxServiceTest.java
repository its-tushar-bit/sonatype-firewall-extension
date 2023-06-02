/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.jacksonstore.MultiFormatStore.Verbose;
import org.spdx.library.DefaultModelStore;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.SpdxDocument;
import org.spdx.storage.IModelStore;
import org.spdx.storage.simple.InMemSpdxStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

public class ApiSpdxServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiSpdxService service;

  @Inject
  private InsightWork work;

  private Application application;

  private String scanId;

  @Mock
  private VersionService versionService;

  @Override
  public void configure(Binder binder) {
    binder.bind(VersionService.class).toInstance(versionService);
    super.configure(binder);
  }

  @Before
  public void setup() throws IOException {
    scanId = tempEntity.uuid();
    application = tempEntity.newApplication(tempEntity.newOrganization().getId());
    setBaseUrl("http://localhost:8070/");
    createReportAndPolicyEvaluation("report");

    SystemConfigurationPropertyFeature.SPDX_EXPORT.setEnabled(true);
  }

  @After
  public void teardown() {
    SystemConfigurationPropertyFeature.SPDX_EXPORT.setEnabled(false);
  }

  private void createReportAndPolicyEvaluation(String folder) throws IOException {
    File reportFile = work.getReportFile(application.getId(), scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/" + getClass().getSimpleName() + "/" + folder, tempDir),
        reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scanId);
  }

  @Test
  public void testGetByScanId_unknownApplicationId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getByScanId("fake-app", "fake-scan-id", "json", false, "2.3"))
        .withMessageContaining("Could not find an application with ID fake-app");
  }

  @Test
  public void testGetByScanId_json() throws Exception {
    testGetByScanId("json", false, "2.3");
  }

  @Test
  public void testGetByScanId_xml() throws Exception {
    testGetByScanId("xml", false, "2.3");
  }

  @Test
  public void testGetByScanId_invalidFormat() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getByScanId(application.getId(), scanId, "yaml", false, "2.3"))
        .withMessageContaining("Invalid format: yaml. Supported formats: [json, xml]");
  }

  @Test
  public void testGetByScanId_invalidSpdxVersion() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getByScanId(application.getId(), scanId, "xml", false, "2.0"))
        .withMessageContaining("Invalid SPDX version: 2.0. Supported SPDX versions: [2.3]");
  }

  @Test
  public void testGetByScanId_spdxExportDisabled() {
    SystemConfigurationPropertyFeature.SPDX_EXPORT.setEnabled(false);

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> service.getByScanId(application.getId(), scanId, "xml", false, "2.3"))
        .withMessageContaining("This API endpoint is currently disabled.");
  }

  private void testGetByScanId(
      String format,
      boolean generateCycloneDx,
      String spdxVersion) throws Exception
  {
    when(versionService.getFullVersion()).thenReturn("1.0");

    Response response = service.getByScanId(application.getId(), scanId, format, generateCycloneDx, spdxVersion);
    SpdxDocument document = deserialize(response, format);

    assertMetadata(document, spdxVersion);
  }

  @Test
  public void testGetLatestForStage_json() throws Exception {
    testGetLatest("json", false, "2.3");
  }

  @Test
  public void testGetLatestForStage_xml() throws Exception {
    testGetLatest("xml", false, "2.3");
  }

  @Test
  public void testGetLatestForStage_invalidFormat() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getLatestForStage(application.getId(), BuildStageType.ID, "yaml", false, "2.0"))
        .withMessageContaining("Invalid format: yaml. Supported formats: [json, xml]");
  }

  @Test
  public void testGetLatestForStage_invalidSpdxVersion() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getLatestForStage(application.getId(), BuildStageType.ID, "xml", false, "2.1"))
        .withMessageContaining("Invalid SPDX version: 2.1. Supported SPDX versions: [2.3]");
  }

  @Test
  public void testGetLatestForStage_spdxExportDisabled() {
    SystemConfigurationPropertyFeature.SPDX_EXPORT.setEnabled(false);

    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> service.getLatestForStage(application.getId(), BuildStageType.ID, "xml", false, "2.3"))
        .withMessageContaining("This API endpoint is currently disabled.");
  }

  private void testGetLatest(
      String format,
      boolean generateCycloneDx,
      String spdxVersion) throws Exception
  {
    when(versionService.getFullVersion()).thenReturn("1.0");

    Response response =
        service.getLatestForStage(application.getId(), BuildStageType.ID, format, generateCycloneDx, spdxVersion);
    SpdxDocument document = deserialize(response, format);

    assertMetadata(document, spdxVersion);
  }

  private void assertMetadata(SpdxDocument document, String spdxVersion) throws Exception {
    assertThat(document.getSpecVersion()).isEqualTo("SPDX-" + spdxVersion);
    assertThat(document.getCreationInfo().getCreated()).isNotNull();
    assertThat(document.getCreationInfo().getCreators().stream().findFirst().get()).isEqualTo(
        "Tool: Sonatype IQ Server - 1.0");
  }

  private SpdxDocument deserialize(Response response, String format)
      throws IOException, InvalidSPDXAnalysisException
  {
    String uri;
    IModelStore modelStore = new InMemSpdxStore();
    MultiFormatStore multiFormatStore =
        new MultiFormatStore(modelStore, "json".equals(format) ? Format.JSON : Format.XML, Verbose.COMPACT);
    try (InputStream in = new ByteArrayInputStream(response.getEntity().toString().getBytes(StandardCharsets.UTF_8))) {
      uri = multiFormatStore.deSerialize(in, true);
    }
    return new SpdxDocument(modelStore, uri, DefaultModelStore.getDefaultCopyManager(), true);
  }
}
