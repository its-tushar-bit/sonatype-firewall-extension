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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.PolicyEvaluationHelper;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.sbom.utils.SbomSummary;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;

import com.google.inject.Binder;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.thymeleaf.util.StringUtils;

import static com.sonatype.insight.brain.hds.ScanUploader.HDS_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

public class SbomImportServiceTest
    extends AbstractComponentTest
{
  private static final String TEST_FILENAME_XML = "test-filename.xml";

  private static final String TEST_FILENAME_JSON = "test-filename.json";

  @Mock
  private HdsClient mockHdsClient;

  @Inject
  private SbomImportService sbomImportService;

  @Inject
  private InsightWork insightWork;

  @Inject
  private TestProductLicense productLicense;

  @Inject
  private PolicyEvaluationHelper policyEvaluationHelper;

  @Inject
  private SbomMetadataUtils sbomMetadataUtils;

  private Application application;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    super.configure(binder);
  }

  @Before
  public void before() {
    application = tempEntity.newApplicationWithParent();
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(false);
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
        .detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(sbom.toPath())),
            "valid-cyclonedx-bom.xml"
        );
    assertThat(actual.getRequestId()).isNotEmpty();
    assertThat(actual.getErrorMessage()).isNullOrEmpty();
    assertThat(actual.getValidationErrors()).isNullOrEmpty();
    assertThat(actual.getSbomSummary().specification).isEqualTo(expected.summary.specification);
    assertThat(actual.getSbomSummary().format).isEqualTo(expected.summary.format);
    assertThat(actual.getSbomSummary().version).isEqualTo(expected.summary.version);
    assertThat(actual.getSbomSummary().componentCount).isEqualTo(expected.summary.componentCount);
    assertThat(actual.getSbomSummary().vulnerabilityCount).isEqualTo(expected.summary.vulnerabilityCount);
    assertThat(actual.getSbomSummary().applicationName).isEqualTo(expected.summary.applicationName);
    assertThat(actual.getSbomSummary().applicationVersion).isEqualTo(expected.summary.applicationVersion);
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertTempSbomFile(actual.getRequestId(), true);
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
        .detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(sbom.toPath())),
            "valid-spdx-bom.json");
    assertThat(actual.getRequestId()).isNotEmpty();
    assertThat(actual.getErrorMessage()).isNullOrEmpty();
    assertThat(actual.getValidationErrors()).isNullOrEmpty();
    assertThat(actual.getSbomSummary().specification).isEqualTo(expected.summary.specification);
    assertThat(actual.getSbomSummary().format).isEqualTo(expected.summary.format);
    assertThat(actual.getSbomSummary().version).isEqualTo(expected.summary.version);
    assertThat(actual.getSbomSummary().componentCount).isEqualTo(expected.summary.componentCount);
    assertThat(actual.getSbomSummary().vulnerabilityCount).isEqualTo(expected.summary.vulnerabilityCount);
    assertThat(actual.getSbomSummary().applicationName).isEqualTo(expected.summary.applicationName);
    assertThat(actual.getSbomSummary().applicationVersion).isEqualTo(expected.summary.applicationVersion);
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertTempSbomFile(actual.getRequestId(), true);
  }

  @Test
  public void testDetectBinary_Success() throws IOException {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/binary.jar");
    File binary = new File(Objects.requireNonNull(resource).getFile());
    SbomDetectionResultDTO actual = sbomImportService
        .detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(binary.toPath())), "binary.jar"
        );
    assertThat(actual.getRequestId()).isNotEmpty();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.BINARY);
    assertThat(actual.getErrorMessage()).isEqualTo("Provided file type is not a supported SBOM file type.");
    assertThat(actual.getValidationErrors()).isNullOrEmpty();
    assertThat(actual.getSbomSummary()).isNull();
    assertTempSbomFile(actual.getRequestId(), true);

    SbomRequestIdElements sbomRequestIdElements = sbomMetadataUtils.decodeRequestId(actual.getRequestId());
    assertThat(sbomRequestIdElements.getScanType()).isEqualTo(SbomScanType.BINARY);
    assertThat(sbomRequestIdElements.getFilename()).endsWith("binary.jar");
  }

  @Test
  public void testDetectBinary_Disabled() {
    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/binary.jar");
    File binary = new File(Objects.requireNonNull(resource).getFile());
    assertThrows("Importing binary files for SBOM Manager is disabled", BadRequestException.class,
        () -> sbomImportService
            .detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(binary.toPath())),
                "binary.jar"
            ));
  }

  @Test
  public void testDetectSbom_Failure_Invalid_CDX_JSON() {
    String sbom = """
        {
          "bomFormat": "CycloneDX",
          "specVersion": "1.4",
          "version": 1,
          "components": [
            {
              "type": "library",
              "name": "example-library-1",
              "version": "1.0.0"
            },
            {
              "name": "example-library-2",
              "version": "1.0.0"
            },
            {
              "name": "example-library-3",
              "version": "1.0.0"
            }
          ]
        }
        """;
    SbomDetectionResultDTO actual = sbomImportService.detectSbom(application.getId(),
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_JSON);
    assertThat(actual.getRequestId()).isEmpty();
    assertThat(actual.getSbomSummary()).isNull();
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid CycloneDX SBOM file.");
    assertThat(actual.getValidationErrors()).containsExactly(
        "Line: 11, Column: 6, Path: $.components[1], Error: required property 'type' not found",
        "Line: 15, Column: 6, Path: $.components[2], Error: required property 'type' not found"
    );
  }

  @Test
  public void testDetectSbom_Failure_Invalid_CDX_XML() {
    String sbom = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bom xmlns="http://cyclonedx.org/schema/bom/1.4" version="1">
          <components>
            <component type="library">
              <name>example-library1</name>
              <version>1.0.0</version>
            </component>
            <component>
              <name>example-library2</name>
              <version>1.0.0</version>
            </component>
            <component>
              <name>example-library3</name>
              <version>1.0.0</version>
            </component>
          </components>
        </bom>
        """;
    SbomDetectionResultDTO actual = sbomImportService.detectSbom(application.getId(),
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_XML);
    assertThat(actual.getRequestId()).isEmpty();
    assertThat(actual.getSbomSummary()).isNull();
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid CycloneDX SBOM file.");
    assertThat(actual.getValidationErrors()).containsExactly(
        "Line: 8, Column: 16, Path: //bom[1]/components[1], Error: cvc-complex-type.4: Attribute 'type' must appear " +
            "on element 'component'.",
        "Line: 12, Column: 16, Path: //bom[1]/components[1], Error: cvc-complex-type.4: Attribute 'type' must appear " +
            "on element 'component'."
    );
  }

  @Test
  public void testDetectSbom_Failure_Invalid_CDX_XML_MissingXmlns() {
    String sbom = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bom version="1">
          <components>
            <component type="library">
              <name>example-library1</name>
              <version>1.0.0</version>
            </component>
            <component>
              <name>example-library2</name>
              <version>1.0.0</version>
            </component>
            <component>
              <name>example-library3</name>
              <version>1.0.0</version>
            </component>
          </components>
        </bom>
        """;

    SbomDetectionResultDTO sbomDetectionResultDTO = sbomImportService.detectSbom(application.getId(),
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_XML);
    assertThat(sbomDetectionResultDTO).isNotNull();
    assertThat(sbomDetectionResultDTO.getErrorMessage()).isEqualTo("CycloneDX XML null version is not supported");
  }

  @Test
  public void testDetectSbom_Failure_Invalid_SPDX_JSON() {
    String sbom = """
        {
          "spdxVersion": "SPDX-2.3",
          "SPDXID": "SPDXRef-DOCUMENT",
          "name": "DummySPDXFile",
          "documentNamespace": "http://spdx.org/spdxdocs/DummySPDXFile",
          "documentDescribes" : [ "SPDXRef-Package1" ],
          "packages": [
            {
              "name": "DummyComponent1",
              "SPDXID": "SPDXRef-Package1",
              "downloadLocation" : "http://some-download-1"
            },
            {
              "name": "DummyComponent2",
              "SPDXID": "SPDXRef-Package2"
            },
            {
              "name": "DummyComponent3",
              "SPDXID": "SPDXRef-Package3"
            }
          ]
        }
        """;
    SbomDetectionResultDTO actual = sbomImportService.detectSbom(application.getId(),
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_JSON);
    assertThat(actual.getRequestId()).isEmpty();
    assertThat(actual.getSbomSummary()).isNull();
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid SPDX SBOM file.");
    assertThat(actual.getValidationErrors()).containsExactly(
        "Line: 13, Column: 6, Path: $.packages[1], Error: required property 'downloadLocation' not found",
        "Line: 17, Column: 6, Path: $.packages[2], Error: required property 'downloadLocation' not found",
        "Line: 1, Column: 2, Path: $, Error: required property 'creationInfo' not found",
        "Line: 1, Column: 2, Path: $, Error: required property 'dataLicense' not found"
    );
  }

  @Test
  public void testDetectSbom_Failure_Invalid_SPDX_XML() {
    String sbom = """
        <?xml version="1.0" encoding="UTF-8"?>
        <Document>
          <spdxVersion>SPDX-2.3</spdxVersion>
          <SPDXID>SPDXRef-DOCUMENT</SPDXID>
          <name>DummySPDXFile</name>
          <documentNamespace>http://spdx.org/spdxdocs/DummySPDXFile</documentNamespace>
          <documentDescribes>SPDXRef-Package1</documentDescribes>
          <packages>
            <name>DummyComponent1</name>
            <SPDXID>SPDXRef-Package1</SPDXID>
            <downloadLocation>http://some-download-1</downloadLocation>
          </packages>
          <packages>
            <name>DummyComponent2</name>
            <SPDXID>SPDXRef-Package2</SPDXID>
          </packages>
          <packages>
            <name>DummyComponent3</name>
            <SPDXID>SPDXRef-Package3</SPDXID>
          </packages>
        </Document>
        """;
    SbomDetectionResultDTO actual = sbomImportService.detectSbom(application.getId(),
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_XML);
    assertThat(actual.getRequestId()).isEmpty();
    assertThat(actual.getSbomSummary()).isNull();
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid SPDX SBOM file.");
    assertThat(actual.getValidationErrors()).containsExactly(
        "Missing required Creator",
        "Missing required data license"
    );
  }

  @Test
  public void testDetectSbom_Failure_InvalidApplicationId() {
    assertThrows("Application with id applicationId does not exist", NotFoundException.class,
        () ->
            sbomImportService.detectSbom("applicationId", new ByteArrayInputStream(new byte[0]), TEST_FILENAME_XML));
  }

  @Test
  public void testImportDetectedSbom_Binary_Success() throws Exception {
    testImportDetectedSbom_Success("binary.jar", SbomScanType.BINARY, null, null);
  }

  @Test
  public void testImportDetectedSbom_SBOM_Success() throws Exception {
    testImportDetectedSbom_Success("valid-cyclonedx-bom.xml", SbomScanType.SBOM, "application/xml", "CycloneDx");
  }

  public void testImportDetectedSbom_Success(
      String fileName,
      SbomScanType scanType,
      String mimeType,
      String contentType) throws Exception
  {
    mockHdsReportDownload();
    InputStream file = SbomImportServiceTest.class
        .getResourceAsStream("/SbomImportServiceTest/" + fileName);
    String uuid = UUID.randomUUID().toString().replace("-", "");
    String storedFileName = StringUtils.concat(uuid, "-", fileName);

    StringBuilder sb = new StringBuilder();
    sb.append(scanType.name());
    if (mimeType != null && contentType != null) {
      sb.append("-").append(mimeType).append("-").append(contentType);
    }
    sb.append("-").append(uuid).append("-").append(fileName);

    String requestId = Base64.getEncoder().encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
    File tempFile = createTemporarySbomFile(storedFileName, file);
    Response response = sbomImportService
        .importDetectedSbom(application.getId(), requestId, "clientUserAgent");
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(Status.ACCEPTED.getStatusCode());
    assertThat(response.getEntity()).isNotNull();
    ApiThirdPartyScanTicketDTO status = (ApiThirdPartyScanTicketDTO) response.getEntity();
    assertThat(status.statusUrl).isNotEmpty()
        .startsWith("api/v2/sbom/applications/" + application.getId() + "/status/");
    assertThat(Files.exists(tempFile.toPath())).isFalse();

    policyEvaluationHelper.awaitEvaluationCompleted(application.getId(), status.requestId);
  }

  @Test
  public void testImportDetectedSbom_Failure_InvalidApplicationId() {
    assertThrows("Application with id applicationId does not exist", NotFoundException.class,
        () ->
            sbomImportService.importDetectedSbom("applicationId",
                "U0JPTS1hcHBsaWNhdGlvbi94bWwtQ3ljbG9uZUR4LTkxMWQ2MTk1MWU5NDQyOTRiYTYwNGI4YTlmZGJkM2NmLWZpbGUuemlw",
                "userAgent"));
  }

  @Test
  public void testImportDetectedSbom_Failure_RequestIdDoesNotExist() {
    assertThrows("Request with id requestId does not exist", NotFoundException.class,
        () ->
            sbomImportService.importDetectedSbom(application.getId(),
                "U0JPTS1hcHBsaWNhdGlvbi94bWwtQ3ljbG9uZUR4LTkxMWQ2MTk1MWU5NDQyOTRiYTYwNGI4YTlmZGJkM2NmLWZpbGUuemlw",
                "userAgent"));
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

  @Test
  public void testImportDetectedSbom_Failure_PathTraversalInFileName() {
    String subpathBinaryRequestId = Base64.getEncoder().encodeToString("BINARY-fo-o/bar.jar".getBytes());
    String parentDirBinaryRequestId = Base64.getEncoder().encodeToString("BINARY-../as-df/passwd".getBytes());

    String subpathSbomRequestId = Base64.getEncoder()
        .encodeToString("SBOM-application/json-SPDX-fo-o/bar.spdx.json".getBytes());
    String parentDirSbomRequestId = Base64.getEncoder()
        .encodeToString("SBOM-application/xml-CycloneDX-../as-df/passwd".getBytes());

    var requestIds =
        List.of(subpathBinaryRequestId, parentDirBinaryRequestId, subpathSbomRequestId, parentDirSbomRequestId);

    for (String requestId : requestIds) {
      assertThrows(
          "The provided requestId " + requestId + " is not valid.",
          BadRequestException.class,
          () -> sbomImportService.importDetectedSbom(application.getId(), requestId, "userAgent")
      );
    }
  }

  @Test
  public void testImportDetectedSbom_MaxSbomLimitHasBeenReached() {
    productLicense.setMaxSbom(0);
    assertThrows("You have exceeded the licensed limit of " + productLicense.getMaxSboms() + " sboms.",
        PaymentRequiredException.class, () ->
            sbomImportService.importDetectedSbom(application.getId(),
                "OTExZDYxOTUxZTk0NDI5NGJhNjA0YjhhOWZkYmQzY2YtYXBwbGljYXRpb24veG1sLUN5Y2xvbmVEeA==", "userAgent"));
    productLicense.reset();
  }

  @Test
  public void testDetectSbom_Failure_MaxSbomLimitHasBeenReached() {
    productLicense.setMaxSbom(0);
    assertThrows("You have exceeded the licensed limit of " + productLicense.getMaxSboms() + " sboms.",
        PaymentRequiredException.class,
        () -> sbomImportService.detectSbom(application.getId(), new ByteArrayInputStream(new byte[0]), TEST_FILENAME_XML
        ));
    productLicense.reset();
  }

  private void mockHdsReportDownload() throws IOException, URISyntaxException {
    final File reportZip =
        Paths.get(ReportHelper.zipReport("/ReportServiceTest/report-with-dependencies", tempDir).toURI()).toFile();
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("scanId");
    when(mockHdsClient.put(any(), eq(ScanReceipt.class), eq("clientUserAgent"), eq(HDS_PATH), any(File.class), any()))
        .thenReturn(scanReceipt);
    when(mockHdsClient.get(any(Retry.class), eq(InputStream.class), anyString(), isNull(), anyString()))
        .thenReturn(Files.newInputStream(reportZip.toPath()));
  }

  private void assertTempSbomFile(String requestId, boolean success) {
    SbomRequestIdElements sbomRequestIdElements = sbomMetadataUtils.decodeRequestId(requestId);
    File tempSbomFile =
        new File(insightWork.getSbomTempDir(), sbomRequestIdElements.getFilename());
    assertThat(Files.exists(tempSbomFile.toPath())).isEqualTo(success);
  }

  private File createTemporarySbomFile(String fileName, InputStream sbom) {
    File tempSbomFile = new File(insightWork.getSbomTempDir(), fileName);
    try (OutputStream outputStream = Files.newOutputStream(tempSbomFile.toPath())) {
      IOUtils.copy(sbom, outputStream);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    return tempSbomFile;
  }
}
