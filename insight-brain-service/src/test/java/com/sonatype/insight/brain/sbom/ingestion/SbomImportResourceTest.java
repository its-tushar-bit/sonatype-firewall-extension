/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SbomImportResourceTest
    extends AbstractResourceTest
{
  private Application application;

  @Before
  public void before() throws IOException {
    licenseManager.setFeatures(LicensedFeature.SBOM_MANAGER);
    application = tempEntity.newApplicationWithParent();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(SbomImportResource.RESOURCE_PATH);
  }

  @Test
  public void testDetectSbom_ValidCycloneDxSbom() throws Exception {
    URL resource = SbomImportResourceTest.class.getResource("/SbomImportResourceTest/valid-cyclonedx-bom.xml");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    HttpResponse response = restRequest()
        .parameter(application.getId())
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.DETECT_PATH)
        .post();
    assertResponseStatus(200, response);
    SbomDetectionResultDTO actual = response.getBody(SbomDetectionResultDTO.class);
    assertThat(actual.getRequestId()).isNotEmpty();
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getSbomSummary().applicationName).isEqualTo("iq_application_vuln");
    assertThat(actual.getSbomSummary().applicationVersion).isEqualTo("a140fd3c3ded4bb0a640dc31e2904dc9");
    assertThat(actual.getSbomSummary().componentCount).isEqualTo(1);
    assertThat(actual.getSbomSummary().vulnerabilityCount).isEqualTo(1);
    assertThat(actual.getSbomSummary().specification).isEqualTo("CycloneDx");
    assertThat(actual.getSbomSummary().format).isEqualTo("xml");
    assertThat(actual.getSbomSummary().version).isEqualTo("1.5");
    assertThat(actual.getErrorMessage()).isNullOrEmpty();
  }

  @Test
  public void testDetectSbom_ValidSpdxSbom() throws Exception {
    URL resource = SbomImportResourceTest.class.getResource("/SbomImportResourceTest/valid-spdx-bom.json");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    HttpResponse response = restRequest()
        .parameter(application.getId())
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.DETECT_PATH)
        .post();
    SbomDetectionResultDTO actual = response.getBody(SbomDetectionResultDTO.class);
    assertResponseStatus(200, response);
    assertThat(actual.getRequestId()).isNotEmpty();
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getSbomSummary().applicationName).isEqualTo("sonatype:iq_application_vuln");
    assertThat(actual.getSbomSummary().applicationVersion).isEqualTo("a140fd3c3ded4bb0a640dc31e2904dc9");
    assertThat(actual.getSbomSummary().componentCount).isEqualTo(2);
    assertThat(actual.getSbomSummary().vulnerabilityCount).isEqualTo(1);
    assertThat(actual.getSbomSummary().specification).isEqualTo("SPDX");
    assertThat(actual.getSbomSummary().format).isEqualTo("json");
    assertThat(actual.getSbomSummary().version).isEqualTo("2.3");
    assertThat(actual.getErrorMessage()).isNullOrEmpty();
  }

  @Test
  public void testDetectSbom_InvalidSbom() throws Exception {
    HttpResponse response = restRequest()
        .parameter(application.getId())
        .part("file", "sbom.xml", new byte[1])
        .path(SbomImportResource.DETECT_PATH)
        .post();
    SbomDetectionResultDTO actual = response.getBody(SbomDetectionResultDTO.class);
    assertResponseStatus(200, response);
    assertThat(actual.getSbomSummary()).isNull();
    assertThat(actual.getRequestId()).isNotEmpty();
    assertThat(actual.getErrorMessage()).isEqualTo("provided file type is not a supported SBOM file type");
  }

  @Test
  public void testDetectSbom_InvalidApplicationId() throws Exception {
    HttpResponse response = restRequest()
        .parameter("applicationId")
        .part("file", "sbom.xml", new Byte[1])
        .path(SbomImportResource.DETECT_PATH)
        .post();
    assertResponseStatus(404, response);
  }

  @Test
  public void testImportDetectedSbom_InvalidApplicationId() throws Exception {
    HttpResponse response = restRequest()
        .path(SbomImportResource.COMMIT_PATH)
        .parameter("applicationId", "requestId")
        .post();

    assertResponseStatus(404, response);
  }

  @Test
  public void testImportDetectedSbom_InvalidRequestId() throws Exception {
    HttpResponse response = restRequest()
        .path(SbomImportResource.COMMIT_PATH)
        .parameter(application.getId(), "requestId")
        .post();

    assertResponseStatus(400, response);
  }

  @Test
  public void testImportDetectedSbom_Success() throws Exception {
    URL resource = SbomImportResourceTest.class.getResource("/SbomImportResourceTest/valid-spdx-bom.json");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    HttpResponse responseDetect = restRequest()
        .parameter(application.getId())
        .part("file", sbom.getName(), Files.readAllBytes(sbom.toPath()))
        .path(SbomImportResource.DETECT_PATH)
        .post();
    SbomDetectionResultDTO actual = responseDetect.getBody(SbomDetectionResultDTO.class);

    HttpResponse responseCommit = restRequest()
        .path(SbomImportResource.COMMIT_PATH)
        .parameter(application.getId(), actual.getRequestId())
        .post();

    assertResponseStatus(201, responseCommit);
  }
}
