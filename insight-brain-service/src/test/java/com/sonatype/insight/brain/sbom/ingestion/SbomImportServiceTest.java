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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.PolicyEvaluationHelper;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.sbom.SbomComponentInfoTelemetry;
import com.sonatype.insight.brain.sbom.utils.SbomSummary;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.brain.utils.ExistingFilesHelper;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.hds.ScanUploader.HDS_PATH;
import static com.sonatype.insight.brain.sbom.SbomSpecification.CYCLONEDX;
import static com.sonatype.insight.brain.sbom.SbomSpecification.SPDX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class SbomImportServiceTest
    extends AbstractComponentTest
{
  private static final String TEST_FILENAME_XML = "test-filename.xml";

  private static final String TEST_FILENAME_JSON = "test-filename.json";

  private static final int CONCURRENCY = 8;

  @Mock
  private HdsClient mockHdsClient;

  @Inject
  private SbomImportService sbomImportService;

  @Inject
  private TestProductLicense productLicense;

  @Inject
  private PolicyEvaluationHelper policyEvaluationHelper;

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  private ThirdPartyFileDAO thirdPartyFileDAO;

  @Inject
  private ExistingFilesHelper existingFilesHelper;

  @Mock
  private TelemetrySender mockTelemetrySender;

  private Application application;

  private ThreadPoolExecutor executor;

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(mockTelemetrySender);
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    super.configure(binder);
  }

  @Before
  public void before() {
    application = tempEntity.newApplicationWithParent();
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(false);
  }

  @After
  public void shutdownThreadPool() {
    if (executor != null) {
      executor.shutdown();
    }
  }

  @Test
  public void testDetectSbom_Success_CycloneDx() throws IOException {
    String expectedApplicationVersion = "a140fd3c3ded4bb0a640dc31e2904dc9";
    SbomSummary expectedSummary = new SbomSummary();
    expectedSummary.specification = "CycloneDx";
    expectedSummary.format = "xml";
    expectedSummary.version = "1.5";
    expectedSummary.componentCount = 1;
    expectedSummary.vulnerabilityCount = 1;
    expectedSummary.applicationName = "iq_application_vuln";
    expectedSummary.applicationVersion = expectedApplicationVersion;
    expectedSummary.creationDetails = "{\"type\":\"application\",\"created\":\"2024-02-29T23:41:22Z\",\"tools\"" +
        ":[{\"name\":\"Nexus IQ Server\",\"version\":\"1.174.0-SNAPSHOT\"}]}";
    expectedSummary.serialNumber = "urn:uuid:a140fd3c-3ded-4bb0-a640-dc31e2904dc9";

    URL resource = SbomImportServiceTest.class.getResource("/SbomImportServiceTest/valid-cyclonedx-bom.xml");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    SbomDetectionResultDTO actual = sbomImportService
        .detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(sbom.toPath())),
            "valid-cyclonedx-bom.xml", false);

    assertThat(actual.getErrorMessage()).isNull();
    assertThat(actual.getValidationErrors()).isNull();
    assertThat(actual.getIsValid()).isTrue();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(actual.getSbomSummary()).usingRecursiveComparison().isEqualTo(expectedSummary);
    assertThat(actual.getSavedVersion()).isEqualTo(expectedApplicationVersion);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        expectedApplicationVersion);
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testDetectSbom_Success_SPDX() throws IOException {
    String expectedApplicationVersion = "a140fd3c3ded4bb0a640dc31e2904dc9";
    SbomSummary expectedSummary = new SbomSummary();
    expectedSummary.specification = "SPDX";
    expectedSummary.format = "json";
    expectedSummary.version = "2.3";
    expectedSummary.componentCount = 2;
    expectedSummary.vulnerabilityCount = 1;
    expectedSummary.applicationName = "sonatype:iq_application_vuln";
    expectedSummary.applicationVersion = expectedApplicationVersion;
    expectedSummary.creationDetails = "{\"created\":\"2024-02-29T23:42:28Z\",\"tools\":" +
        "[{\"name\":\"Sonatype IQ Server\",\"version\":\"1.174.0-SNAPSHOT\"}]}";
    expectedSummary.serialNumber =
        "http://localhost:8070/ui/links/application/vuln/report/a140fd3c3ded4bb0a640dc31e2904dc9";

    URL resource = SbomImportServiceTest.class.getResource("/SbomImportServiceTest/valid-spdx-bom.json");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    SbomDetectionResultDTO actual =
        sbomImportService.detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(sbom.toPath())),
            "valid-spdx-bom.json", false);

    assertThat(actual.getErrorMessage()).isNull();
    assertThat(actual.getValidationErrors()).isNull();
    assertThat(actual.getIsValid()).isTrue();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(actual.getSbomSummary()).usingRecursiveComparison().isEqualTo(expectedSummary);
    assertThat(actual.getSavedVersion()).isEqualTo(expectedApplicationVersion);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        expectedApplicationVersion);
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testDetectSbom_Success_IgnoreValidationError_ValidCycloneDx() throws IOException {
    String expectedApplicationVersion = "a140fd3c3ded4bb0a640dc31e2904dc9";
    SbomSummary expectedSummary = new SbomSummary();
    expectedSummary.specification = "CycloneDx";
    expectedSummary.format = "xml";
    expectedSummary.version = "1.5";
    expectedSummary.componentCount = 1;
    expectedSummary.vulnerabilityCount = 1;
    expectedSummary.applicationName = "iq_application_vuln";
    expectedSummary.applicationVersion = expectedApplicationVersion;
    expectedSummary.creationDetails = "{\"type\":\"application\",\"created\":\"2024-02-29T23:41:22Z\",\"tools\":" +
        "[{\"name\":\"Nexus IQ Server\",\"version\":\"1.174.0-SNAPSHOT\"}]}";
    expectedSummary.serialNumber = "urn:uuid:a140fd3c-3ded-4bb0-a640-dc31e2904dc9";

    URL resource = SbomImportServiceTest.class.getResource("/SbomImportServiceTest/valid-cyclonedx-bom.xml");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    SbomDetectionResultDTO actual =
        sbomImportService.detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(sbom.toPath())),
            "valid-cyclonedx-bom.xml", true);

    assertThat(actual.getErrorMessage()).isNull();
    assertThat(actual.getValidationErrors()).isNull();
    assertThat(actual.getIsValid()).isTrue();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(actual.getSbomSummary()).usingRecursiveComparison().isEqualTo(expectedSummary);
    assertThat(actual.getSavedVersion()).isEqualTo(expectedApplicationVersion);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        expectedApplicationVersion);
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testDetectSbom_Success_IgnoreValidationError_ValidSPDX() throws IOException {
    String expectedApplicationVersion = "a140fd3c3ded4bb0a640dc31e2904dc9";
    SbomSummary expectedSummary = new SbomSummary();
    expectedSummary.specification = "SPDX";
    expectedSummary.format = "json";
    expectedSummary.version = "2.3";
    expectedSummary.componentCount = 2;
    expectedSummary.vulnerabilityCount = 1;
    expectedSummary.applicationName = "sonatype:iq_application_vuln";
    expectedSummary.applicationVersion = expectedApplicationVersion;
    expectedSummary.creationDetails = "{\"created\":\"2024-02-29T23:42:28Z\",\"tools\":" +
        "[{\"name\":\"Sonatype IQ Server\",\"version\":\"1.174.0-SNAPSHOT\"}]}";
    expectedSummary.serialNumber =
        "http://localhost:8070/ui/links/application/vuln/report/a140fd3c3ded4bb0a640dc31e2904dc9";

    URL resource = SbomImportServiceTest.class.getResource("/SbomImportServiceTest/valid-spdx-bom.json");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    SbomDetectionResultDTO actual =
        sbomImportService.detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(sbom.toPath())),
            "valid-spdx-bom.json", true);

    assertThat(actual.getErrorMessage()).isNull();
    assertThat(actual.getValidationErrors()).isNull();
    assertThat(actual.getIsValid()).isTrue();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(actual.getSbomSummary()).usingRecursiveComparison().isEqualTo(expectedSummary);
    assertThat(actual.getSavedVersion()).isEqualTo(expectedApplicationVersion);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        expectedApplicationVersion);
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testDetectSbom_Success_IgnoreValidationError_InvalidCycloneDx() throws IOException {
    String expectedApplicationVersion = "a140fd3c3ded4bb0a640dc31e2904dc9";
    SbomSummary expectedSummary = new SbomSummary();
    expectedSummary.specification = "CycloneDx";
    expectedSummary.format = "json";
    expectedSummary.version = "1.4";
    expectedSummary.componentCount = 3;
    expectedSummary.vulnerabilityCount = 0;
    expectedSummary.applicationName = "iq_application_vuln";
    expectedSummary.applicationVersion = expectedApplicationVersion;
    expectedSummary.creationDetails = "{\"type\":\"application\",\"created\":\"2024-10-29T17:55:28Z\"}";

    String sbom = """
        {
          "bomFormat": "CycloneDX",
          "specVersion": "1.4",
          "version": 1,
          "metadata" : {
            "timestamp" : "2024-10-29T17:55:28Z",
            "component" : {
              "type" : "application",
              "bom-ref" : "3908ff2f-1662-4e2f-a48e-9e4ffca960e6",
              "name" : "iq_application_vuln",
              "version" : "a140fd3c3ded4bb0a640dc31e2904dc9"
            }
          },
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
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_JSON, true);

    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getErrorMessage()).isNull();
    assertThat(actual.getValidationErrors()).isNull();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(actual.getSbomSummary()).usingRecursiveComparison()
        .ignoringFields("serialNumber")
        .isEqualTo(expectedSummary);
    assertThat(actual.getSavedVersion()).isEqualTo(expectedApplicationVersion);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        expectedApplicationVersion);
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testDetectSbom_Success_IgnoreValidationError_InvalidSPDX() throws IOException {
    String expectedApplicationVersion = "2.11.1";
    SbomSummary expectedSummary = new SbomSummary();
    expectedSummary.specification = "SPDX";
    expectedSummary.format = "xml";
    expectedSummary.version = "2.3";
    expectedSummary.componentCount = 3;
    expectedSummary.vulnerabilityCount = 0;
    expectedSummary.applicationName = "DummyComponent1";
    expectedSummary.applicationVersion = expectedApplicationVersion;
    expectedSummary.creationDetails = "{}";
    expectedSummary.serialNumber = "http://spdx.org/spdxdocs/DummySPDXFile";
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
            <versionInfo>2.11.1</versionInfo>
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
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_XML, true);

    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getErrorMessage()).isNull();
    assertThat(actual.getValidationErrors()).isNull();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(actual.getSbomSummary()).usingRecursiveComparison().isEqualTo(expectedSummary);
    assertThat(actual.getSavedVersion()).isEqualTo(expectedApplicationVersion);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        expectedApplicationVersion);
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testDetectSbom_Success_VersionDeduplication() throws IOException {
    String expectedApplicationVersion = "a140fd3c3ded4bb0a640dc31e2904dc9";

    URL resource = SbomImportServiceTest.class.getResource("/SbomImportServiceTest/valid-cyclonedx-bom.xml");
    File sbom = new File(Objects.requireNonNull(resource).getFile());
    SbomDetectionResultDTO actual1 = sbomImportService
        .detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(sbom.toPath())),
            "valid-cyclonedx-bom.xml", false);
    SbomDetectionResultDTO actual2 = sbomImportService
        .detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(sbom.toPath())),
            "valid-cyclonedx-bom.xml", false);

    assertThat(actual1.getSavedVersion()).isEqualTo(expectedApplicationVersion);
    assertThat(actual2.getSavedVersion())
        .isNotEqualTo(expectedApplicationVersion)
        .startsWith(expectedApplicationVersion);
  }

  @Test
  public void testDetectSbom_Success_VersionDeconflicting() throws Exception {
    String expectedApplicationVersion = "a140fd3c3ded4bb0a640dc31e2904dc9";

    URL resource = SbomImportServiceTest.class.getResource("/SbomImportServiceTest/valid-cyclonedx-bom.xml");
    byte[] sbom = Files.readAllBytes(new File(Objects.requireNonNull(resource).getFile()).toPath());

    // try to save the same SBOM 8 times at the same time and ensure that they all deconflict uniquely
    List<Callable<SbomDetectionResultDTO>> calls = Stream.<Callable<SbomDetectionResultDTO>>generate(() -> () -> {
      return sbomImportService.detectSbom(
          application.getId(),
          new ByteArrayInputStream(sbom),
          "valid-cyclonedx-bom.xml",
          false);
    }).limit(CONCURRENCY).toList();

    executor = new ThreadPoolExecutor(CONCURRENCY, CONCURRENCY, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    executor.prestartAllCoreThreads();
    Set<String> versions = executor.invokeAll(calls, 1, TimeUnit.SECONDS)
        .stream()
        .map(future -> {
          try {
            return future.get();
          }
          catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
          }
        })
        .map(SbomDetectionResultDTO::getSavedVersion)
        .collect(Collectors.toSet());

    assertThat(versions).hasSize(CONCURRENCY);
    assertThat(versions).allMatch(v -> v.startsWith(expectedApplicationVersion));
  }

  @Test
  public void testDetectSbom_Success_VersionDeconflicting_noBuiltInVersion() throws Exception {
    URL resource = SbomImportServiceTest.class.getResource("/SbomImportServiceTest/no-version-cyclonedx-bom.xml");
    byte[] sbom = Files.readAllBytes(new File(Objects.requireNonNull(resource).getFile()).toPath());

    // try to save the same SBOM 8 times at the same time and ensure that they all deconflict uniquely
    List<Callable<SbomDetectionResultDTO>> calls = Stream.<Callable<SbomDetectionResultDTO>>generate(() -> () -> {
      return sbomImportService.detectSbom(
          application.getId(),
          new ByteArrayInputStream(sbom),
          "no-version-cyclonedx-bom.xml",
          false);
    }).limit(CONCURRENCY).toList();

    executor = new ThreadPoolExecutor(CONCURRENCY, CONCURRENCY, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    executor.prestartAllCoreThreads();
    Set<String> versions = executor.invokeAll(calls, 1, TimeUnit.SECONDS)
        .stream()
        .map(future -> {
          try {
            return future.get();
          }
          catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
          }
        })
        .map(SbomDetectionResultDTO::getSavedVersion)
        .collect(Collectors.toSet());

    assertThat(versions).hasSize(CONCURRENCY);
    assertThat(versions).allMatch(v -> v.matches("^\\d+$"));
  }

  @Test
  public void testDetectBinary_Success() throws IOException {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/binary.jar");
    File binary = new File(Objects.requireNonNull(resource).getFile());
    SbomDetectionResultDTO actual = sbomImportService
        .detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(binary.toPath())), "binary.jar",
            false);
    assertThat(actual.getIsValid()).isNull();
    assertThat(actual.getIsValidationErrorIgnorable()).isNull();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.BINARY);
    assertThat(actual.getErrorMessage()).isEqualTo("Provided file type is not a supported SBOM file type.");
    assertThat(actual.getValidationErrors()).isNull();
    assertThat(actual.getSbomSummary()).isNull();
    assertThat(actual.getSavedVersion()).matches("^\\d+$");

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        actual.getSavedVersion());
    assertThat(sbomMetadata.getOriginalBinaryFileName()).isEqualTo("binary.jar");
    assertExistingSbomFiles("temp/persistent/%s/binary.jar".formatted(sbomMetadata.getId()));
  }

  @Test
  public void testDetectBinary_StripsPathDirs() throws IOException {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/binary.jar");
    File binary = new File(Objects.requireNonNull(resource).getFile());
    SbomDetectionResultDTO actual = sbomImportService
        .detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(binary.toPath())),
            "foo/binary.jar", false);
    assertThat(actual.getIsValid()).isNull();
    assertThat(actual.getIsValidationErrorIgnorable()).isNull();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.BINARY);
    assertThat(actual.getErrorMessage()).isEqualTo("Provided file type is not a supported SBOM file type.");
    assertThat(actual.getValidationErrors()).isNull();
    assertThat(actual.getSbomSummary()).isNull();
    assertThat(actual.getSavedVersion()).matches("^\\d+$");

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        actual.getSavedVersion());
    assertThat(sbomMetadata.getOriginalBinaryFileName()).isEqualTo("binary.jar");
    assertExistingSbomFiles("temp/persistent/%s/binary.jar".formatted(sbomMetadata.getId()));
  }

  @Test
  public void testDetectBinary_StripsAbsolutePath() throws IOException {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/binary.jar");
    File binary = new File(Objects.requireNonNull(resource).getFile());
    SbomDetectionResultDTO actual = sbomImportService
        .detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(binary.toPath())),
            "/binary.jar", false);
    assertThat(actual.getIsValid()).isNull();
    assertThat(actual.getIsValidationErrorIgnorable()).isNull();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.BINARY);
    assertThat(actual.getErrorMessage()).isEqualTo("Provided file type is not a supported SBOM file type.");
    assertThat(actual.getValidationErrors()).isNull();
    assertThat(actual.getSbomSummary()).isNull();
    assertThat(actual.getSavedVersion()).matches("^\\d+$");

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        actual.getSavedVersion());
    assertThat(sbomMetadata.getOriginalBinaryFileName()).isEqualTo("binary.jar");
    assertExistingSbomFiles("temp/persistent/%s/binary.jar".formatted(sbomMetadata.getId()));
  }

  @Test
  public void testDetectBinary_StripsDotDotDirs() throws IOException {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/binary.jar");
    File binary = new File(Objects.requireNonNull(resource).getFile());
    SbomDetectionResultDTO actual = sbomImportService
        .detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(binary.toPath())),
            "../../binary.jar", false);
    assertThat(actual.getIsValid()).isNull();
    assertThat(actual.getIsValidationErrorIgnorable()).isNull();
    assertThat(actual.getScanType()).isEqualTo(SbomScanType.BINARY);
    assertThat(actual.getErrorMessage()).isEqualTo("Provided file type is not a supported SBOM file type.");
    assertThat(actual.getValidationErrors()).isNull();
    assertThat(actual.getSbomSummary()).isNull();
    assertThat(actual.getSavedVersion()).matches("^\\d+$");

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        actual.getSavedVersion());
    assertThat(sbomMetadata.getOriginalBinaryFileName()).isEqualTo("binary.jar");
    assertExistingSbomFiles("temp/persistent/%s/binary.jar".formatted(sbomMetadata.getId()));
  }

  @Test
  public void testDetectBinary_FailsOnDotDotPath() throws IOException {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/binary.jar");
    File binary = new File(Objects.requireNonNull(resource).getFile());
    assertThatThrownBy(() -> sbomImportService.detectSbom(
        application.getId(),
        new ByteArrayInputStream(Files.readAllBytes(binary.toPath())),
        "..",
        false)).isInstanceOf(BadRequestException.class);

    assertExistingSbomFiles();
  }

  @Test
  public void testDetectBinary_FailsOnDotDotFinalPath() throws IOException {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/binary.jar");
    File binary = new File(Objects.requireNonNull(resource).getFile());
    assertThatThrownBy(() -> sbomImportService.detectSbom(
        application.getId(),
        new ByteArrayInputStream(Files.readAllBytes(binary.toPath())),
        "binary.jar/..",
        false)).isInstanceOf(BadRequestException.class);

    assertExistingSbomFiles();
  }

  @Test
  public void testDetectBinary_FailsOnDotFinalPath() throws IOException {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/binary.jar");
    File binary = new File(Objects.requireNonNull(resource).getFile());

    assertThatThrownBy(() -> sbomImportService.detectSbom(
        application.getId(),
        new ByteArrayInputStream(Files.readAllBytes(binary.toPath())),
        "binary.jar/.",
        false)).isInstanceOf(BadRequestException.class);

    assertExistingSbomFiles();
  }

  @Test
  public void testDetectSbom_StripsPathDirs() throws IOException {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/valid-cyclonedx-bom.xml");
    File binary = new File(Objects.requireNonNull(resource).getFile());
    SbomDetectionResultDTO actual = sbomImportService
        .detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(binary.toPath())),
            "foo/valid-cyclonedx-bom.xml", false);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        actual.getSavedVersion());
    var thirdPartyFile = thirdPartyFileDAO.getById(sbomMetadata.getThirdPartyFileId());
    assertThat(thirdPartyFile.getFilename()).isEqualTo("valid-cyclonedx-bom.xml");
    assertThat(sbomMetadata.getFilename()).matches("\\p{XDigit}+\\.xml\\.gz");
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testDetectSbom_StripsDotDotDirs() throws IOException {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/valid-cyclonedx-bom.xml");
    File binary = new File(Objects.requireNonNull(resource).getFile());
    SbomDetectionResultDTO actual = sbomImportService
        .detectSbom(application.getId(), new ByteArrayInputStream(Files.readAllBytes(binary.toPath())),
            "../../valid-cyclonedx-bom.xml", false);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        actual.getSavedVersion());
    var thirdPartyFile = thirdPartyFileDAO.getById(sbomMetadata.getThirdPartyFileId());
    assertThat(thirdPartyFile.getFilename()).isEqualTo("valid-cyclonedx-bom.xml");
    assertThat(sbomMetadata.getFilename()).matches("\\p{XDigit}+\\.xml\\.gz");
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testDetectSbom_FailsOnDotDotPath() throws IOException {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/valid-cyclonedx-bom.xml");
    File binary = new File(Objects.requireNonNull(resource).getFile());
    assertThatThrownBy(() -> sbomImportService.detectSbom(
        application.getId(),
        new ByteArrayInputStream(Files.readAllBytes(binary.toPath())),
        "..",
        false)).isInstanceOf(BadRequestException.class);

    assertExistingSbomFiles();
  }

  @Test
  public void testDetectSbom_FailsOnDotDotFinalPath() throws IOException {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/valid-cyclonedx-bom.xml");
    File binary = new File(Objects.requireNonNull(resource).getFile());
    assertThatThrownBy(() -> sbomImportService.detectSbom(
        application.getId(),
        new ByteArrayInputStream(Files.readAllBytes(binary.toPath())),
        "valid-cyclonedx-bom.xml/..",
        false)).isInstanceOf(BadRequestException.class);

    assertExistingSbomFiles();
  }

  @Test
  public void testDetectSbom_FailsOnDotFinalPath() throws IOException {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/valid-cyclonedx-bom.xml");
    File binary = new File(Objects.requireNonNull(resource).getFile());

    assertThatThrownBy(() -> sbomImportService.detectSbom(
        application.getId(),
        new ByteArrayInputStream(Files.readAllBytes(binary.toPath())),
        "valid-cyclonedx-bom.xml/.",
        false)).isInstanceOf(BadRequestException.class);

    assertExistingSbomFiles();
  }

  @Test
  public void testDetectBinary_Disabled() throws IOException {
    URL resource = SbomImportServiceTest.class
        .getResource("/SbomImportServiceTest/binary.jar");
    File binary = new File(Objects.requireNonNull(resource).getFile());
    assertThrows("Importing binary files for SBOM Manager is disabled", BadRequestException.class,
        () -> sbomImportService.detectSbom(application.getId(),
            new ByteArrayInputStream(Files.readAllBytes(binary.toPath())), "binary.jar", false));

    assertExistingSbomFiles();
  }

  @Test
  public void testDetectSbom_Failure_Invalid_CDX_JSON() throws IOException {
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
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_JSON, false);
    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getSavedVersion()).matches("^\\d+$");
    assertThat(actual.getIsValidationErrorIgnorable()).isTrue();
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getSbomSummary().format).isEqualTo("json");
    assertThat(actual.getSbomSummary().specification).isEqualTo("CycloneDx");
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid CycloneDX SBOM file.");
    assertThat(actual.getValidationErrors()).containsExactly(
        "Line: 11, Column: 6, Path: $.components[1], Error: required property 'type' not found",
        "Line: 15, Column: 6, Path: $.components[2], Error: required property 'type' not found");

    assertTelemetryData("json", CYCLONEDX.toString(), "1.4", 2, false, false);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        actual.getSavedVersion());
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testDetectSbom_Failure_Invalid_CDX_XML() throws IOException {
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
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_XML, false);
    assertThat(actual.getSavedVersion()).matches("^\\d+$");
    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getIsValidationErrorIgnorable()).isTrue();
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getSbomSummary().format).isEqualTo("xml");
    assertThat(actual.getSbomSummary().specification).isEqualTo("CycloneDx");
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid CycloneDX SBOM file.");
    assertThat(actual.getValidationErrors()).containsExactly(
        "Line: 8, Column: 16, Path: //bom[1]/components[1], Error: cvc-complex-type.4: Attribute 'type' must appear " +
            "on element 'component'.",
        "Line: 12, Column: 16, Path: //bom[1]/components[1], Error: cvc-complex-type.4: Attribute 'type' must appear " +
            "on element 'component'.");

    assertTelemetryData("xml", CYCLONEDX.toString(), "1.4", 2, false, false);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        actual.getSavedVersion());
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testDetectSbom_Failure_Invalid_CDX_XML_MissingXmlns() throws IOException {
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
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_XML, false);
    assertThat(sbomDetectionResultDTO).isNotNull();
    assertThat(sbomDetectionResultDTO.getIsValid()).isFalse();
    assertThat(sbomDetectionResultDTO.getIsValidationErrorIgnorable()).isFalse();
    assertThat(sbomDetectionResultDTO.getErrorMessage()).isEqualTo("CycloneDX XML null version is not supported");

    assertExistingSbomFiles();
  }

  @Test
  public void testDetectSbom_Failure_Invalid_SPDX_JSON() throws IOException {
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
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_JSON, false);
    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getSavedVersion()).matches("^\\d+$");
    assertThat(actual.getIsValidationErrorIgnorable()).isTrue();
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getSbomSummary().format).isEqualTo("json");
    assertThat(actual.getSbomSummary().specification).isEqualTo("SPDX");
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid SPDX SBOM file.");
    assertThat(actual.getValidationErrors()).containsExactly(
        "Line: 13, Column: 6, Path: $.packages[1], Error: required property 'downloadLocation' not found",
        "Line: 17, Column: 6, Path: $.packages[2], Error: required property 'downloadLocation' not found",
        "Line: 1, Column: 2, Path: $, Error: required property 'creationInfo' not found",
        "Line: 1, Column: 2, Path: $, Error: required property 'dataLicense' not found");

    assertTelemetryData("json", SPDX.toString(), "2.3", 4, false, false);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        actual.getSavedVersion());
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testDetectSbom_Failure_Invalid_SPDX_XML() throws IOException {
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
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_XML, false);
    assertThat(actual.getSavedVersion()).matches("^\\d+$");
    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getIsValidationErrorIgnorable()).isTrue();
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getSbomSummary().format).isEqualTo("xml");
    assertThat(actual.getSbomSummary().specification).isEqualTo("SPDX");
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid SPDX SBOM file.");
    assertThat(actual.getValidationErrors()).containsExactly(
        "Error: Missing required Creator",
        "Error: Missing required data license");

    assertTelemetryData("xml", SPDX.toString(), "2.3", 2, false, false);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        actual.getSavedVersion());
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testDetectSbom_Failure_Invalid_CDX_JSON_Structure() throws IOException {
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
          ]
        }
        """;
    SbomDetectionResultDTO actual = sbomImportService.detectSbom(application.getId(),
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_JSON, false);
    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getIsValidationErrorIgnorable()).isFalse();
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getSbomSummary().format).isEqualTo("json");
    assertThat(actual.getSbomSummary().specification).isEqualTo("CycloneDx");
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid CycloneDX SBOM file.");
    assertThat(actual.getValidationErrors()).containsExactly("Error: Unable to parse BOM from byte array",
        "Line: 11, Column: 3, Error: Unexpected character (']' (code 93)): expected a value");

    assertTelemetryData("json", CYCLONEDX.toString(), null, 2, false, false);

    assertExistingSbomFiles();
  }

  @Test
  public void testDetectSbom_Failure_Invalid_CDX_XML_Structure() throws IOException {
    String sbom = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bom xmlns="http://cyclonedx.org/schema/bom/1.4" version="1">
          <components>
            <component type="library">
              <name>example-library1</name>
              <version>1.0.0</version>
            </component>
            <component
          </components>
        </bom>
        """;
    SbomDetectionResultDTO actual = sbomImportService.detectSbom(application.getId(),
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_XML, false);
    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getIsValidationErrorIgnorable()).isFalse();
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getSbomSummary().format).isEqualTo("xml");
    assertThat(actual.getSbomSummary().specification).isEqualTo("CycloneDx");
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid CycloneDX SBOM file.");
    assertThat(actual.getValidationErrors()).containsExactly(
        "Line: 9, Column: 3, Error: Element type \"component\" " +
            "must be followed by either attribute specifications, \">\" or \"/>\".");

    assertTelemetryData("xml", CYCLONEDX.toString(), null, 1, false, false);

    assertExistingSbomFiles();
  }

  @Test
  public void testDetectSbom_Failure_Invalid_SPDX_JSON_Structure() throws IOException {
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
          ]
        }
        """;

    SbomDetectionResultDTO actual = sbomImportService.detectSbom(application.getId(),
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_XML, false);
    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getIsValidationErrorIgnorable()).isFalse();
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getSbomSummary().format).isEqualTo("xml");
    assertThat(actual.getSbomSummary().specification).isEqualTo("SPDX");
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid SPDX SBOM file.");
    assertThat(actual.getValidationErrors()).containsExactly("Error: Missing SPDX Document");

    assertTelemetryData("xml", SPDX.toString(), null, 1, false, false);

    assertExistingSbomFiles();
  }

  @Test
  public void testDetectSbom_Failure_Invalid_SPDX_XML_Structure() throws IOException {
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
          <packages
        </Document>
        """;

    SbomDetectionResultDTO actual = sbomImportService.detectSbom(application.getId(),
        new ByteArrayInputStream(sbom.getBytes(StandardCharsets.UTF_8)), TEST_FILENAME_XML, false);
    assertThat(actual.getIsValid()).isFalse();
    assertThat(actual.getIsValidationErrorIgnorable()).isFalse();
    assertThat(actual.getSbomSummary()).isNotNull();
    assertThat(actual.getSbomSummary().format).isEqualTo("xml");
    assertThat(actual.getSbomSummary().specification).isEqualTo("SPDX");
    assertThat(actual.getErrorMessage()).isEqualTo("Not a valid SPDX SBOM file.");
    assertThat(actual.getValidationErrors()).containsExactly("Error: Misplaced '<' at 466 [character 1 line 14]");

    assertTelemetryData("xml", SPDX.toString(), null, 1, false, false);

    assertExistingSbomFiles();
  }

  @Test
  public void testDetectSbom_Failure_InvalidApplicationId() throws IOException {
    assertThrows("Application with id applicationId does not exist", NotFoundException.class,
        () -> sbomImportService.detectSbom("applicationId", new ByteArrayInputStream(new byte[0]), TEST_FILENAME_XML,
            false));

    assertExistingSbomFiles();
  }

  @Test
  public void testImportDetectedSbom_Binary_Success() throws Exception {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);
    var actual = testImportDetectedSbom_Success("binary.jar", SbomScanType.BINARY, null, null, false);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        actual.getSavedVersion());
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testImportDetectedSbom_SBOM_Success() throws Exception {
    var actual = testImportDetectedSbom_Success("valid-cyclonedx-bom.xml", SbomScanType.SBOM,
        SbomFormat.forMimeType("application/xml"), ItemContentType.SBOM, false);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        actual.getSavedVersion());
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testImportDetectedSbom_ValidationSkippedSBOM_Success() throws Exception {
    var actual = testImportDetectedSbom_Success("invalid-cyclonedx-bom.xml", SbomScanType.SBOM,
        SbomFormat.forMimeType("application/xml"), ItemContentType.SBOM, false);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(
        application.getId(),
        actual.getSavedVersion());
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testImportDetectedSbom_SBOM_Success_Version_Override() throws Exception {
    mockHdsReportDownload();
    InputStream file =
        SbomImportServiceTest.class.getResourceAsStream("/SbomImportServiceTest/valid-cyclonedx-bom.xml");

    var detectionResult =
        sbomImportService.detectSbom(application.getId(), file, "valid-cyclonedx-bom.xml", false);

    assertThat(detectionResult.getSavedVersion()).isNotEqualTo("1.2.3.4");

    Response response = sbomImportService.importDetectedSbom(
        application.getId(),
        detectionResult.getSavedVersion(),
        "1.2.3.4",
        "clientUserAgent");
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(Status.ACCEPTED.getStatusCode());
    assertThat(response.getEntity()).isNotNull();
    ApiThirdPartyScanTicketDTO status = (ApiThirdPartyScanTicketDTO) response.getEntity();
    assertThat(status.statusUrl).isNotEmpty()
        .startsWith("api/v2/sbom/applications/" + application.getId() + "/status/");
    policyEvaluationHelper.awaitEvaluationCompleted(application.getId(), status.requestId);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(application.getId(), "1.2.3.4");
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testImportDetectedSbom_SBOM_Success_Version_Override_Blank() throws Exception {
    InputStream file =
        SbomImportServiceTest.class.getResourceAsStream("/SbomImportServiceTest/valid-cyclonedx-bom.xml");

    var detectionResult =
        sbomImportService.detectSbom(application.getId(), file, "valid-cyclonedx-bom.xml", false);

    assertThatThrownBy(() -> sbomImportService.importDetectedSbom(
        application.getId(),
        detectionResult.getSavedVersion(),
        "   ",
        "clientUserAgent")).isInstanceOf(BadRequestException.class);
  }

  @Test
  public void testImportDetectedSbom_SBOM_Success_Version_Override_Conflict() throws Exception {
    mockHdsReportDownload();
    InputStream file1 =
        SbomImportServiceTest.class.getResourceAsStream("/SbomImportServiceTest/valid-cyclonedx-bom.xml");

    var detectionResult1 =
        sbomImportService.detectSbom(application.getId(), file1, "valid-cyclonedx-bom.xml", false);

    Response response = sbomImportService.importDetectedSbom(
        application.getId(),
        detectionResult1.getSavedVersion(),
        "1.2.3.4",
        "clientUserAgent");
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(Status.ACCEPTED.getStatusCode());
    assertThat(response.getEntity()).isNotNull();
    ApiThirdPartyScanTicketDTO status = (ApiThirdPartyScanTicketDTO) response.getEntity();
    assertThat(status.statusUrl).isNotEmpty()
        .startsWith("api/v2/sbom/applications/" + application.getId() + "/status/");
    policyEvaluationHelper.awaitEvaluationCompleted(application.getId(), status.requestId);

    InputStream file2 =
        SbomImportServiceTest.class.getResourceAsStream("/SbomImportServiceTest/valid-cyclonedx-bom.xml");

    var detectionResult2 =
        sbomImportService.detectSbom(application.getId(), file2, "valid-cyclonedx-bom.xml", false);

    assertThatThrownBy(() -> sbomImportService.importDetectedSbom(
        application.getId(),
        detectionResult2.getSavedVersion(),
        "1.2.3.4",
        "clientUserAgent")).isInstanceOf(ConflictException.class);
  }

  public SbomDetectionResultDTO testImportDetectedSbom_Success(
      String fileName,
      SbomScanType scanType,
      SbomFormat sbomFormat,
      ItemContentType contentType,
      boolean skipValidation) throws Exception
  {
    mockHdsReportDownload();
    InputStream file = SbomImportServiceTest.class
        .getResourceAsStream("/SbomImportServiceTest/" + fileName);

    var detectionResult = sbomImportService.detectSbom(application.getId(), file, fileName, skipValidation);

    Response response = sbomImportService.importDetectedSbom(
        application.getId(),
        detectionResult.getSavedVersion(),
        null,
        "clientUserAgent");
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(Status.ACCEPTED.getStatusCode());
    assertThat(response.getEntity()).isNotNull();
    ApiThirdPartyScanTicketDTO status = (ApiThirdPartyScanTicketDTO) response.getEntity();
    assertThat(status.statusUrl).isNotEmpty()
        .startsWith("api/v2/sbom/applications/" + application.getId() + "/status/");
    policyEvaluationHelper.awaitEvaluationCompleted(application.getId(), status.requestId);

    return detectionResult;
  }

  @Test
  public void testImportDetectedSbom_Failure_InvalidApplicationId() throws IOException {
    assertThrows("Application with id applicationId does not exist", NotFoundException.class,
        () -> sbomImportService.importDetectedSbom("notAnApplicationId", "1", null, "userAgent"));

    assertExistingSbomFiles();
  }

  @Test
  public void testImportDetectedSbom_Failure_InvalidVersion() throws IOException {
    String invalidVersion = "invalidVersion";

    assertThrows(
        "SBOM with applicationId %s and version %s does not exist".formatted(application.getId(), invalidVersion),
        NotFoundException.class,
        () -> sbomImportService.importDetectedSbom(application.getId(), invalidVersion, null, "userAgent"));

    assertExistingSbomFiles();
  }

  @Test
  public void testDetectSbom_Failure_MaxSbomLimitHasBeenReached() throws IOException {
    productLicense.setMaxSbom(0);
    assertThrows("You have exceeded the licensed limit of " + productLicense.getMaxSboms() + " sboms.",
        PaymentRequiredException.class,
        () -> sbomImportService.detectSbom(application.getId(), new ByteArrayInputStream(new byte[0]),
            TEST_FILENAME_XML, false));
    productLicense.reset();

    assertExistingSbomFiles();
  }

  @Test
  public void testImportDetectedSbom_SBOM_VersionConflict_StaysInUploadedState() throws Exception {
    mockHdsReportDownload();

    InputStream file1 =
        SbomImportServiceTest.class.getResourceAsStream("/SbomImportServiceTest/valid-cyclonedx-bom.xml");
    var detectionResult1 = sbomImportService.detectSbom(application.getId(), file1, "valid-cyclonedx-bom.xml", false);
    Response response = sbomImportService.importDetectedSbom(
        application.getId(),
        detectionResult1.getSavedVersion(),
        "1.2.3.4",
        "clientUserAgent");
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(Status.ACCEPTED.getStatusCode());
    assertThat(response.getEntity()).isNotNull();
    ApiThirdPartyScanTicketDTO status = (ApiThirdPartyScanTicketDTO) response.getEntity();
    assertThat(status.statusUrl).isNotEmpty()
        .startsWith("api/v2/sbom/applications/" + application.getId() + "/status/");
    policyEvaluationHelper.awaitEvaluationCompleted(application.getId(), status.requestId);

    InputStream file2 =
        SbomImportServiceTest.class.getResourceAsStream("/SbomImportServiceTest/valid-cyclonedx-bom.xml");
    var detectionResult2 = sbomImportService.detectSbom(application.getId(), file2, "valid-cyclonedx-bom.xml", false);

    assertThatThrownBy(() -> sbomImportService.importDetectedSbom(
        application.getId(),
        detectionResult2.getSavedVersion(),
        "1.2.3.4", // Try to use same version
        "clientUserAgent"))
            .isInstanceOf(ConflictException.class);

    ThirdPartySbomMetadata metadata =
        thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(application.getId(),
            detectionResult2.getSavedVersion());
    assertThat(metadata.getStatus()).isEqualTo(ThirdPartySbomMetadataStatus.UPLOADED);
  }

  private void assertTelemetryData(
      final String format,
      final String spec,
      final String specVersion,
      final int validationErrorsCount,
      final boolean isSkipSbomValidationFeatureFlagEnabled,
      final boolean isSbomValid)
  {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender, times(1)).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);
    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    assertThat(telemetryAttributes).isNotNull();
    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");
    assertThat(componentInfoTelemetry).isNotNull();
    assertThat(componentInfoTelemetry.getSpec()).isEqualTo(spec);
    assertThat(componentInfoTelemetry.getContentType()).isEqualTo(format);
    assertThat(componentInfoTelemetry.getSpecVersion()).isEqualTo(specVersion);
    assertThat(componentInfoTelemetry.getValidationErrorsCount()).isEqualTo(validationErrorsCount);

    assertThat(telemetryAttributes.get("is_skip_sbom_validation_feature_flag_enabled"))
        .isEqualTo(isSkipSbomValidationFeatureFlagEnabled);
    assertThat(telemetryAttributes.get("is_sbom_valid"))
        .isEqualTo(isSbomValid);
  }

  private void mockHdsReportDownload() throws IOException, URISyntaxException {
    final File reportZip =
        Paths.get(ReportHelper.zipReport("/ReportServiceTest/report-with-dependencies", tempDir).toURI()).toFile();
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("scanId");
    when(mockHdsClient.put(any(), eq(ScanReceipt.class), eq("clientUserAgent"), eq(HDS_PATH), any(ScanEntity.class),
        any())).thenReturn(scanReceipt);
    when(mockHdsClient.get(any(Retry.class), eq(InputStream.class), anyString(), isNull(), anyString()))
        .thenReturn(Files.newInputStream(reportZip.toPath()));
  }

  private void assertExistingSbomFiles(String... expectedPaths) throws IOException {
    existingFilesHelper.assertExistingSbomFiles(expectedPaths);
  }
}
