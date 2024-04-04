/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.sbom.utils.SbomSummary;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

public class SbomImportServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SbomImportService sbomImportService;

  @Inject
  private InsightWork insightWork;

  private Application application;

  @Before
  public void before() {
    application = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testDetectSbom_Success_CycloneDx() throws IOException {
    SbomDetectionResult expected = new SbomDetectionResult();
    expected.isSbom = true;
    expected.mimeType = MediaType.APPLICATION_XML;
    SbomSummary summary = new SbomSummary();
    summary.specification = "CycloneDx";
    summary.format = "xml";
    summary.version = "1.5";
    summary.componentCount = 1;
    summary.vulnerabilityCount = 1;
    summary.applicationName = "iq_application_vuln";
    summary.applicationVersion = "a140fd3c3ded4bb0a640dc31e2904dc9";
    expected.summary = summary;
    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/valid-cyclonedx-bom.xml");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    SbomDetectionResultDTO actual = sbomImportService
        .detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(sbom.toPath())));
    assertThat(actual.getRequestId()).isNotEmpty();
    assertThat(actual.getErrorMessage()).isNullOrEmpty();
    assertThat(actual.getSbomSummary().specification).isEqualTo(expected.summary.specification);
    assertThat(actual.getSbomSummary().format).isEqualTo(expected.summary.format);
    assertThat(actual.getSbomSummary().version).isEqualTo(expected.summary.version);
    assertThat(actual.getSbomSummary().componentCount).isEqualTo(expected.summary.componentCount);
    assertThat(actual.getSbomSummary().vulnerabilityCount).isEqualTo(expected.summary.vulnerabilityCount);
    assertThat(actual.getSbomSummary().applicationName).isEqualTo(expected.summary.applicationName);
    assertThat(actual.getSbomSummary().applicationVersion).isEqualTo(expected.summary.applicationVersion);
    assertTempSbomFile(actual.getRequestId().split("-")[0], true);
  }

  @Test
  public void testDetectSbom_Success_SPDX() throws IOException {
    SbomDetectionResult expected = new SbomDetectionResult();
    expected.isSbom = true;
    expected.mimeType = MediaType.APPLICATION_XML;
    SbomSummary summary = new SbomSummary();
    summary.specification = "SPDX";
    summary.format = "json";
    summary.version = "2.3";
    summary.componentCount = 2;
    summary.vulnerabilityCount = 1;
    summary.applicationName = "sonatype:iq_application_vuln";
    summary.applicationVersion = "a140fd3c3ded4bb0a640dc31e2904dc9";
    expected.summary = summary;
    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/valid-spdx-bom.json");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    SbomDetectionResultDTO actual = sbomImportService
        .detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(sbom.toPath())));
    assertThat(actual.getRequestId()).isNotEmpty();
    assertThat(actual.getErrorMessage()).isNullOrEmpty();
    assertThat(actual.getSbomSummary().specification).isEqualTo(expected.summary.specification);
    assertThat(actual.getSbomSummary().format).isEqualTo(expected.summary.format);
    assertThat(actual.getSbomSummary().version).isEqualTo(expected.summary.version);
    assertThat(actual.getSbomSummary().componentCount).isEqualTo(expected.summary.componentCount);
    assertThat(actual.getSbomSummary().vulnerabilityCount).isEqualTo(expected.summary.vulnerabilityCount);
    assertThat(actual.getSbomSummary().applicationName).isEqualTo(expected.summary.applicationName);
    assertThat(actual.getSbomSummary().applicationVersion).isEqualTo(expected.summary.applicationVersion);
    assertTempSbomFile(actual.getRequestId().split("-")[0], true);
  }

  @Test
  public void testDetectSbom_Failure_InvalidSbomFormat() {
    SbomDetectionResultDTO actual = sbomImportService.detectSbom(application.getId(),
        new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
    assertThat(actual.getRequestId()).isNotEmpty();
    assertThat(actual.getSbomSummary()).isNull();
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid/supported sbom file");
    assertTempSbomFile(actual.getRequestId(), false);
  }

  @Test
  public void testDetectSbom_Failure_InvalidSbomFile() {
    SbomDetectionResultDTO actual =
        sbomImportService.detectSbom(application.getId(), new ByteArrayInputStream(new byte[0]));
    assertThat(actual.getRequestId()).isNotEmpty();
    assertThat(actual.getSbomSummary()).isNull();
    assertThat(actual.getErrorMessage()).isEqualTo("provided file type is not a supported SBOM file type");
    assertTempSbomFile(actual.getRequestId(), false);
  }

  @Test
  public void testDetectSbom_Failure_InvalidApplicationId() {
    assertThrows("Application with id applicationId does not exist", NotFoundException.class,
        () ->
            sbomImportService.detectSbom("applicationId", new ByteArrayInputStream(new byte[0])));
  }

  @Test
  public void testImportDetectedSbom_Success() {
    InputStream sbom = SbomImportServiceTest.class
        .getResourceAsStream("/SbomImportServiceTest/valid-cyclonedx-bom.xml");
    String fileName = UUID.randomUUID().toString().replace("-", "");
    String mimeType = "application/xml";
    String contentType = "CycloneDx";
    String requestId =
        Base64.getEncoder().encodeToString(String.format("%s-%s-%s", fileName, mimeType, contentType).getBytes());
    File tempFile = createTemporarySbomFile(fileName, sbom);
    Response response = sbomImportService
        .importDetectedSbom(application.getId(), requestId, "clientUserAgent");
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
    assertThat(response.getEntity()).isNotNull();
    ApiThirdPartyScanTicketDTO status = (ApiThirdPartyScanTicketDTO) response.getEntity();
    assertThat(status.statusUrl).isNotEmpty()
            .startsWith("api/v2/sbom/" + application.getId() + "/status/");
    assertThat(Files.exists(tempFile.toPath())).isFalse();
  }

  @Test
  public void testImportDetectedSbom_Failure_InvalidApplicationId() {
    assertThrows("Application with id applicationId does not exist", NotFoundException.class,
        () ->
            sbomImportService.importDetectedSbom("applicationId",
                "OTExZDYxOTUxZTk0NDI5NGJhNjA0YjhhOWZkYmQzY2YtYXBwbGljYXRpb24veG1sLUN5Y2xvbmVEeA==", "userAgent"));
  }

  @Test
  public void testImportDetectedSbom_Failure_RequestIdDoesNotExist() {
    assertThrows("Request with id requestId does not exist", NotFoundException.class,
        () ->
            sbomImportService.importDetectedSbom(application.getId(),
                "OTExZDYxOTUxZTk0NDI5NGJhNjA0YjhhOWZkYmQzY2YtYXBwbGljYXRpb24veG1sLUN5Y2xvbmVEeA==", "userAgent"));
  }

  @Test
  public void testImportDetectedSbom_Failure_InvalidRequestId() {
    String fileName = UUID.randomUUID().toString().replace("-", "");
    String mimeType = "";
    String requestId = Base64.getEncoder().encodeToString(String.format("%s-%s", fileName, mimeType).getBytes());
    assertThrows("The provided requestId " + requestId + " is not valid.", BadRequestException.class,
        () ->
            sbomImportService.importDetectedSbom(application.getId(), requestId, "userAgent"));
  }

  private void assertTempSbomFile(String requestId, boolean success) {
    String[] decodedRequestId = new String(Base64.getDecoder().decode(requestId)).split("-");
    File tempSbomFile = new File(insightWork.getSbomTempDir(), decodedRequestId[0] + ".tmp");
    assertThat(Files.exists(tempSbomFile.toPath())).isEqualTo(success);
  }

  private File createTemporarySbomFile(String fileName, InputStream sbom) {
    File tempSbomFile = new File(insightWork.getSbomTempDir(), fileName + ".tmp");
    try (OutputStream outputStream = Files.newOutputStream(tempSbomFile.toPath())) {
      IOUtils.copy(sbom, outputStream);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    return tempSbomFile;
  }
}
