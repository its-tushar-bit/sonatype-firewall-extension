/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static com.sonatype.insight.scan.model.ItemContentType.IAC_FILE;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataTestUtil;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.sbom.SbomTestHelper;
import com.sonatype.insight.brain.sbom.utils.SbomFileDetector;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.scan.datastore.FileScanEntity;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.ExistingFilesHelper;
import com.sonatype.insight.brain.utils.Xpp3Util;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.file.ContainerFileSonatypeProcessor;
import com.sonatype.insight.scan.manifest.ClairScannerResult;
import com.sonatype.insight.scan.manifest.ClairScannerVulnerability;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.scan.model.ScanFileNames;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.w3c.dom.Document;

@Category(SlowTest.class)
@ContextConfiguration(classes = ThirdPartyScanResultsProcessorTest.ExistingFilesHelperTestConfig.class)
public class ThirdPartyScanResultsProcessorTest
    extends AbstractComponentTest
{
  @TestConfiguration
  static class ExistingFilesHelperTestConfig
  {
    @Bean
    ExistingFilesHelper existingFilesHelper() {
      return new ExistingFilesHelper();
    }
  }

  private final String loggerName = ThirdPartyScanResultsProcessor.class.getName();

  private static final String DUMMY_APP_ID = UUID.randomUUID().toString().replace("-", "");

  @Rule
  public LogOutput logOutput = new LogOutput(loggerName);

  @Inject
  private ThirdPartyScanDAO thirdPartyScanDAO;

  @Inject
  private ThirdPartyFileDAO thirdPartyFileDAO;

  @Inject
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  @Inject
  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  @Inject
  private ThirdPartyResultHandlerFactory thirdPartyResultHandlerFactory;

  @Mock
  private TelemetrySender telemetrySender;

  @Inject
  private InsightWork insightWork;

  @Inject
  private SbomFileDetector sbomFileDetector;

  @Mock
  private ProductLicense productLicense;

  @Mock
  private ThirdPartyScanResultsProcessor thirdPartyScanResultsProcessorSpy;

  @Inject
  private SbomMetadataUtils sbomMetadataUtils;

  @Inject
  private ThirdPartyPersistenceService thirdPartyPersistenceService;

  @Inject
  private ExistingFilesHelper existingFilesHelper;

  private static final Gson GSON = new Gson();

  private static final String DEFAULT_STAGE_TYPE = StageTypes.DEVELOP.getName();

  @Before
  public void before() {
    thirdPartyScanResultsProcessorSpy = spy(new ThirdPartyScanResultsProcessor(
        thirdPartyScanDAO,
        thirdPartyFileDAO,
        thirdPartySbomMetadataDAO,
        telemetrySender,
        thirdPartyResultHandlerFactory,
        productLicense,
        sbomFileDetector,
        sbomMetadataUtils,
        thirdPartyPersistenceService));
  }

  @Test
  public void testHandle_EmptyItemElement() throws Exception {
    File scanFile = getScanFile("scan-with-empty-item-data.xml");
    File tempScanFile = tempDir.newFile();
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()),
        mockContext(scanFile), null);
    verify(thirdPartyScanResultsProcessorSpy, times(0)).createHandler(any(ItemContentType.class),
        any(ThirdPartyScanContext.class));
    assertEmptyItemElement(tempScanFile);
  }

  @Test
  public void testHandle_thirdPartyWithOtherContent() throws Exception {
    File scanFile = getScanFile("scan-thirdparty-and-other-content.xml");
    File tempScanFile = tempDir.newFile();

    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()),
        mockContext(scanFile), null);
    verify(thirdPartyScanResultsProcessorSpy, times(2))
        .createHandler(any(ItemContentType.class), any(ThirdPartyScanContext.class));

    URL resource = getTestResource("scan-thirdparty-and-other-content-expected.xml");
    DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document expectedScan = db.parse(new File(resource.toURI()));
    String expectedSbom = getSbomNodeAsString(expectedScan, "/scan/item[4]/content");
    Document actualScan = db.parse(getScanXMLFile(tempScanFile));
    String actualSbom = getSbomNodeAsString(actualScan, "/scan/item[4]/content");

    assertThatJson(actualSbom).whenIgnoringPaths("components[*].properties[*].value")
        .isEqualTo(expectedSbom);

    String hasErrorAttr = getSbomNodeAsString(actualScan, "/scan/item[4]/@hasError");
    assertThat(hasErrorAttr).isEmpty();
  }

  @Test
  public void testHandle_spdx_api() throws Exception {
    File scanFile = getScanFile("scan-with-spdx-data-api.xml");
    File tempScanFile = tempDir.newFile();

    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()),
        mockContext(scanFile), null);
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.SPDX),
        any(ThirdPartyScanContext.class));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.SPDX, true, 6);
  }

  @Test
  public void testHandle_spdx_api_sbomManagerDisabled() throws Exception {
    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Test Application", "TEST", organization.getId());
    File scanFile = getScanFile("scan-with-spdx-data-api.xml");
    File tempScanFile = tempDir.newFile();

    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()),
        mockContext(scanFile), null);
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.SPDX),
        any(ThirdPartyScanContext.class));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.SPDX, true, 6);

    File sbomDir = insightWork.getSbomDir(application.getId());
    assertThat(sbomDir).isEmptyDirectory();
  }

  @Test
  public void testHandle_spdx_api_sbomManagerEnabled() throws Exception {
    mockValidSbomManagerLicense();

    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Test Application", "TEST", organization.getId());
    File scanFile = getScanFile("scan-with-spdx-data-api.xml");
    String scanId = TemporaryEntity.uuid();
    File tempScanFile = tempDir.newFile();
    Files.createDirectories(insightWork.getScanDir(application.getId()).toPath());

    ThirdPartyScanContext context =
        new ThirdPartyScanContext("scanRequestId", application.getId(), SbomScanType.SBOM,
            new FileScanEntity(scanFile.toPath()),
            StageTypes.COMPLIANCE.getName());
    String scanRequestId = thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()), context, null);
    thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanId);
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.SPDX),
        any(ThirdPartyScanContext.class));

    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.SPDX, true, 6);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(application.getId(), "1.0-SNAPSHOT");
    var thirdPartyFile = thirdPartyFileDAO.getById(sbomMetadata.getThirdPartyFileId());

    ThirdPartySbomMetadata expectedSbomMetadata = new ThirdPartySbomMetadata(thirdPartyFile.getId(),
        application.getId(),
        null,
        null,
        "http://localhost:8070/ui/links/application/local-iq-app/report/d6ffc430f2594d2480c7af837eb2a5b6",
        "SPDX",
        "json",
        "2.3",
        PENDING,
        new Date(),
        creationDetailsToolsOnlyJson(),
        "SBOM",
        false, null);
    assertThirdPartySbomMetadata(thirdPartyFile, true, expectedSbomMetadata);
    assertThat(sbomMetadata.getSerialNumber()).isEqualTo(expectedSbomMetadata.getSerialNumber());
    assertThat(sbomMetadata.getFilename()).matches("\\p{XDigit}+\\.json\\.gz");
    assertThat(thirdPartyFile.getFilename()).isEqualTo("spdx.spdx.json");
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testHandle_spdx_api_TelemetryData() throws Exception {
    File scanFile = getScanFile("scan-with-spdx-data-api.xml");
    TelemetryData telemetryData = buildThirdPartyScanTelemetryData();
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempDir.newFile().toPath()), mockContext(scanFile), telemetryData);
    verify(telemetrySender).send(telemetryData);
    assertTelemetryData(telemetryData, List.of("SPDX"));
  }

  @Test
  public void testHandle_sbom_api() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-data-api.xml");
    File tempScanFile = tempDir.newFile();

    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()),
        mockContext(scanFile), null);
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.SBOM),
        any(ThirdPartyScanContext.class));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.SBOM, true, 3);

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_cyclonedx_api_sbomManagerDisabled() throws Exception {
    final Organization organization = tempEntity.newOrganization("Test Org");
    tempEntity.newApplication("Test Application", "TEST", organization.getId());
    File scanFile = getScanFile("sbom/scan-with-sbom-data-api.xml");
    File tempScanFile = tempDir.newFile();

    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()),
        mockContext(scanFile), null);
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.SBOM),
        any(ThirdPartyScanContext.class));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.SBOM, true, 3);

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_cyclonedx_api_sbomManagerEnabled() throws Exception {
    mockValidSbomManagerLicense();

    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Test Application", "TEST", organization.getId());
    File scanFile = getScanFile("sbom/scan-with-sbom-data-api.xml");
    String scanId = TemporaryEntity.uuid();
    File tempScanFile = tempDir.newFile();

    ThirdPartyScanContext scanContext =
        new ThirdPartyScanContext("scanRequestId", application.getId(), SbomScanType.SBOM,
            new FileScanEntity(scanFile.toPath()),
            StageTypes.COMPLIANCE.getName());
    scanContext.setStageType(StageTypes.COMPLIANCE.getName());
    String scanRequestId = thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()), scanContext, null);
    thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanId);
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.SBOM),
        any(ThirdPartyScanContext.class));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.SBOM, true, 3);

    var sbomMetadatas = thirdPartySbomMetadataDAO.getByApplicationId(application.getId());
    assertThat(sbomMetadatas).hasSize(1);
    var sbomMetadata = sbomMetadatas.get(0);
    var thirdPartyFile = thirdPartyFileDAO.getById(sbomMetadata.getThirdPartyFileId());

    ThirdPartySbomMetadata expectedSbomMetadata = new ThirdPartySbomMetadata(thirdPartyFile.getId(),
        application.getId(),
        null,
        null,
        "urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79",
        "CycloneDx",
        "xml",
        "1.5",
        PENDING,
        new Date(),
        null,
        "SBOM",
        false, null);
    assertThirdPartySbomMetadata(thirdPartyFile, true, expectedSbomMetadata);
    assertThat(sbomMetadata.getSbomVersion()).matches("\\d+");
    assertThat(sbomMetadata.getFilename()).matches("\\p{XDigit}+\\.xml\\.gz");
    assertThat(thirdPartyFile.getFilename()).isEqualTo("test-bom.xml");
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testHandle_sbom_api_TelemetryData() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-data-api.xml");
    TelemetryData telemetryData = buildThirdPartyScanTelemetryData();
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempDir.newFile().toPath()), mockContext(scanFile), telemetryData);
    verify(telemetrySender).send(telemetryData);
    assertTelemetryData(telemetryData, List.of("SBOM"));
  }

  @Test
  public void testHandle_sbom_cli() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-data-cli.xml");
    File tempScanFile = tempDir.newFile();

    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()),
        mockContext(scanFile), null);

    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(eq(ItemContentType.SBOM),
        any(ThirdPartyScanContext.class));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.SBOM, true, 2);

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_sbom_cli_sbomManagerEnabled() throws Exception {
    mockValidSbomManagerLicense();

    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Test Application", "TEST", organization.getId());
    File scanFile = getScanFile("sbom/scan-with-sbom-data-cli.xml");
    String scanId = TemporaryEntity.uuid();
    File tempScanFile = tempDir.newFile();
    Files.createDirectories(insightWork.getScanDir(application.getId()).toPath());

    ThirdPartyScanContext scanContext =
        new ThirdPartyScanContext("scanRequestId", application.getId(), SbomScanType.SBOM,
            new FileScanEntity(scanFile.toPath()),
            StageTypes.COMPLIANCE.getName());
    scanContext.setStageType(StageTypes.COMPLIANCE.getName());
    String scanRequestId = thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()), scanContext, null);
    thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanId);

    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(eq(ItemContentType.SBOM),
        any(ThirdPartyScanContext.class));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.SBOM, true, 2);

    var sbomMetadatas = thirdPartySbomMetadataDAO.getByApplicationId(application.getId());
    assertThat(sbomMetadatas).hasSize(1);
    var sbomMetadata = sbomMetadatas.get(0);
    var thirdPartyFile = thirdPartyFileDAO.getById(sbomMetadata.getThirdPartyFileId());

    ThirdPartySbomMetadata expectedSbomMetadata = new ThirdPartySbomMetadata(thirdPartyFile.getId(),
        application.getId(),
        null,
        null,
        "urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79",
        "CycloneDx",
        "xml",
        "1.1",
        PENDING,
        new Date(),
        null,
        "SBOM",
        false, null);
    assertThirdPartySbomMetadata(thirdPartyFile, true, expectedSbomMetadata);
    assertThat(sbomMetadata.getSbomVersion()).matches("\\d+");
    assertThat(sbomMetadata.getFilename()).matches("\\p{XDigit}+\\.xml\\.gz");
    assertThat(thirdPartyFile.getFilename()).isEqualTo("path1/iq-scan-sbom.xml");
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testHandle_container_neuvector_content() throws Exception {
    mockValidSbomManagerLicense();

    File scanFile = getScanFile("container/scan-with-container-neuvector-content.xml");
    File tempScanFile = tempDir.newFile();
    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Test Application", "TEST", organization.getId());
    ThirdPartyScanContext scanContext =
        new ThirdPartyScanContext("scanRequestId", application.getId(), SbomScanType.SBOM,
            new FileScanEntity(scanFile.toPath()),
            StageTypes.COMPLIANCE.getName());

    TelemetryData telemetryData = buildThirdPartyScanTelemetryData();
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()),
        scanContext, telemetryData);

    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.CONTAINER_URI),
        any(ThirdPartyScanContext.class));
    assertThat(scanContext.getContainerUriPaths()).hasSize(1);
    assertThat(scanContext.getContainerUriPaths().get(0)).isEqualTo("container:alpine:3.6");
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.CONTAINER_URI, true, 9);
    verify(telemetrySender, times(1)).send(telemetryData);
    assertTelemetryData(telemetryData, List.of(ItemContentType.CONTAINER_URI.name()));

    var sbomMetadatas = thirdPartySbomMetadataDAO.getByApplicationId(application.getId());
    assertThat(sbomMetadatas).hasSize(1);
    var sbomMetadata = sbomMetadatas.get(0);
    var thirdPartyFile = thirdPartyFileDAO.getById(sbomMetadata.getThirdPartyFileId());
    assertThat(sbomMetadata.getFilename()).isNull(); // gets set outside of ThirdPartyScanResultsProcessor
    assertThat(thirdPartyFile.getFilename()).isEqualTo("container:alpine:3.6");
    assertThat(scanContext.getApplicationVersion()).isNotNull();
    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_container_sonatype_content() throws Exception {
    mockValidSbomManagerLicense();
    when(productLicense.hasFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION)).thenReturn(true);

    File scanFile = getScanFile("container/scan-with-container-sonatype-content.xml");
    File tempScanFile = tempDir.newFile();
    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Test Application", "TEST", organization.getId());
    ThirdPartyScanContext scanContext =
        new ThirdPartyScanContext("scanRequestId", application.getId(), SbomScanType.SBOM,
            new FileScanEntity(scanFile.toPath()),
            StageTypes.COMPLIANCE.getName());

    TelemetryData telemetryData = buildThirdPartyScanTelemetryData();
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()),
        scanContext, telemetryData);

    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.CONTAINER_URI_SONATYPE),
        any(ThirdPartyScanContext.class));
    assertThat(scanContext.getContainerUriPaths()).hasSize(1);
    assertThat(scanContext.getContainerUriPaths().get(0))
        .isEqualTo(ContainerFileSonatypeProcessor.CONTAINER_FILE_PREFIX + "alpine:3.6");
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.CONTAINER_URI_SONATYPE, true, 9);
    verify(telemetrySender, times(1)).send(telemetryData);
    assertTelemetryData(telemetryData, List.of(ItemContentType.CONTAINER_URI_SONATYPE.name()));

    var sbomMetadatas = thirdPartySbomMetadataDAO.getByApplicationId(application.getId());
    assertThat(sbomMetadatas).hasSize(1);
    var sbomMetadata = sbomMetadatas.get(0);
    var thirdPartyFile = thirdPartyFileDAO.getById(sbomMetadata.getThirdPartyFileId());
    assertThat(sbomMetadata.getFilename()).isNull(); // gets set outside of ThirdPartyScanResultsProcessor
    assertThat(thirdPartyFile.getFilename())
        .isEqualTo(ContainerFileSonatypeProcessor.CONTAINER_FILE_PREFIX + "alpine:3.6");
    assertThat(scanContext.getApplicationVersion()).isNotNull();
    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_sbomUsingSameSbomFileRepeatedContent() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-repeated-content.xml");

    File tempScanFile = tempDir.newFile();
    TelemetryData telemetryData = buildThirdPartyScanTelemetryData();
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()),
        mockContext(scanFile), telemetryData);

    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(any(ItemContentType.class),
        any(ThirdPartyScanContext.class));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.SBOM, true, 2);
    verify(telemetrySender, times(1)).send(telemetryData);
    assertTelemetryData(telemetryData, List.of("SBOM", "SBOM"));

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_InvalidJson() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-data-invalid-json.xml");
    String scanId = TemporaryEntity.uuid();

    try {
      String scanRequestId = thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
          new FileScanEntity(tempDir.newFile().toPath()), mockContext(scanFile), null);
      thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanId);
    }
    catch (RuntimeException e) {
      verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(any(ItemContentType.class),
          any(ThirdPartyScanContext.class));
      assertThat(thirdPartyFileDAO.getByScanId(scanId)).isEmpty();
      assertExistingSbomFiles();
    }
  }

  @Test
  public void testHandle_ClairScanner_TelemetryData() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-repeated-content.xml");
    TelemetryData telemetryData = buildThirdPartyScanTelemetryData();
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempDir.newFile().toPath()), mockContext(scanFile), telemetryData);
    verify(telemetrySender, times(1)).send(telemetryData);
    assertTelemetryData(telemetryData, List.of("CLAIR_SCANNER", "CLAIR_SCANNER"));
  }

  @Test
  public void testHandle_ClairScanner() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-data.xml");
    String scanId = TemporaryEntity.uuid();

    File tempScanFile = tempDir.newFile();
    String scanRequestId = thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()), mockContext(scanFile), null);
    thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanId);
    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(eq(ItemContentType.CLAIR_SCANNER),
        any(ThirdPartyScanContext.class));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.CLAIR_SCANNER, false, 3);

    List<ThirdPartyFile> thirdPartyFileList = thirdPartyFileDAO.getByScanId(scanId);
    assertThirdPartyFile(thirdPartyFileList, "clair-scanner-out/clair-scanner-output.json",
        "clair-scanner-out/other/clair-scanner-output.json");

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_ClairScanner_sbomManagerEnabled() throws Exception {
    when(productLicense.hasFeature(LicensedFeature.SBOM_MANAGER)).thenReturn(true);
    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Test Application", "TEST", organization.getId());
    File scanFile = getScanFile("scan-with-clair-scanner-data.xml");
    String scanId = TemporaryEntity.uuid();

    File tempScanFile = tempDir.newFile();
    TelemetryData telemetryData = buildThirdPartyScanTelemetryData();
    ThirdPartyScanContext context =
        new ThirdPartyScanContext("scanRequestId", application.getId(), SbomScanType.SBOM,
            new FileScanEntity(scanFile.toPath()),
            DEFAULT_STAGE_TYPE);
    String scanRequestId = thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()), context, telemetryData);
    thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanId);

    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(eq(ItemContentType.CLAIR_SCANNER),
        any(ThirdPartyScanContext.class));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.CLAIR_SCANNER, false, 3);

    List<ThirdPartyFile> thirdPartyFileList = thirdPartyFileDAO.getByScanId(scanId);
    assertThirdPartyFile(thirdPartyFileList, "clair-scanner-out/clair-scanner-output.json",
        "clair-scanner-out/other/clair-scanner-output.json");

    thirdPartyFileList.forEach(thirdPartyFile -> assertThirdPartySbomMetadata(thirdPartyFile, false, null));
    verify(telemetrySender, times(1)).send(telemetryData);
    assertTelemetryData(telemetryData, List.of("CLAIR_SCANNER", "CLAIR_SCANNER", "CLAIR_SCANNER"));

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_ClairScanner_sbomManagerDisabled() throws Exception {
    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Test Application", "TEST", organization.getId());
    File scanFile = getScanFile("scan-with-clair-scanner-data.xml");
    String scanId = TemporaryEntity.uuid();

    File tempScanFile = tempDir.newFile();
    ThirdPartyScanContext context =
        new ThirdPartyScanContext("scanRequestId", application.getId(), SbomScanType.SBOM,
            new FileScanEntity(scanFile.toPath()),
            DEFAULT_STAGE_TYPE);

    String scanRequestId = thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()), context, null);
    thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanId);

    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(eq(ItemContentType.CLAIR_SCANNER),
        any(ThirdPartyScanContext.class));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.CLAIR_SCANNER, false, 3);

    List<ThirdPartyFile> thirdPartyFileList = thirdPartyFileDAO.getByScanId(scanId);
    assertThirdPartyFile(thirdPartyFileList, "clair-scanner-out/clair-scanner-output.json",
        "clair-scanner-out/other/clair-scanner-output.json");

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_ClairScannerUsingSameClairFileRepeatedContent() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-repeated-content.xml");
    String scanId = TemporaryEntity.uuid();

    File tempScanFile = tempDir.newFile();
    String scanRequestId = thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()), mockContext(scanFile), null);
    thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanId);

    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(any(ItemContentType.class),
        any(ThirdPartyScanContext.class));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.CLAIR_SCANNER, false, 3);

    assertThat(scanId).isNotNull();
    List<ThirdPartyFile> thirdPartyFileList = thirdPartyFileDAO.getByScanId(scanId);
    assertThirdPartyFile(thirdPartyFileList, "clair-scanner-output.json", "clair-scanner-output.json");

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_iac_content() throws Exception {
    TelemetryData telemetryData = buildThirdPartyScanTelemetryData();
    File scanFile = getScanFile("iac/scan-with-iac-content.xml");
    File tempScanFile = tempDir.newFile();
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()),
        mockContext(scanFile), telemetryData);

    verify(telemetrySender, times(1)).send(telemetryData);
    assertTelemetryData(telemetryData, List.of("IAC_FILE"));
    assertFilteredThirdPartyScanContentFile(tempScanFile, IAC_FILE, true, 0);

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_ClairCorruptFile() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-data-corrupted.xml");
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
        () -> thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
            new FileScanEntity(tempDir.newFile().toPath()), mockContext(scanFile), null))
        .withMessage("Error reading/processing third party scan content from scan file");

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_corruptSbomFile() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-data-corrupted.xml");
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
        () -> thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
            new FileScanEntity(tempDir.newFile().toPath()), mockContext(scanFile), null))
        .withMessage("Error reading/processing third party scan content from scan file");

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_noSbomContent() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-empty-sbom-content.xml");
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempDir.newFile().toPath()), mockContext(scanFile), null);
    verify(thirdPartyScanResultsProcessorSpy, times(0)).createHandler(any(ItemContentType.class),
        any(ThirdPartyScanContext.class));

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_SbomDependencyTree_basic() throws Exception {
    testHandle_SbomDependencyTree("DependencyGraph/scan-sbom-dependencies-basic.xml",
        "DependencyGraph/scan-sbom-dependencies-basic-expected.xml");
  }

  @Test
  public void testHandle_SbomDependencyTree_cyclic() throws Exception {
    testHandle_SbomDependencyTree("DependencyGraph/scan-sbom-dependencies-cyclic.xml",
        "DependencyGraph/scan-sbom-dependencies-cyclic-expected.xml");
  }

  @Test
  public void testHandle_SbomDependencyTree_multiformats() throws Exception {
    testHandle_SbomDependencyTree("DependencyGraph/scan-sbom-dependencies-multiformat.xml",
        "DependencyGraph/scan-sbom-dependencies-multiformat-expected.xml");
  }

  @Test
  public void testHandle_SbomDependencyTree_multiformats_uuidRef() throws Exception {
    testHandle_SbomDependencyTree("DependencyGraph/scan-sbom-dependencies-multiformat-uuid-ref.xml",
        "DependencyGraph/scan-sbom-dependencies-multiformat-uuid-bom-ref-expected.xml");
  }

  @Test
  public void testHandle_SbomDependencyTree_uuidRef_incomplete() throws Exception {
    testHandle_SbomDependencyTree("DependencyGraph/scan-sbom-dependencies-uuid-ref-incomplete.xml",
        "DependencyGraph/scan-sbom-dependencies-uuid-bom-ref-incomplete-expected.xml");
  }

  @Test
  public void testHandle_cyclonedx_api_sbomManagerEnabled_creationDetails() throws Exception {
    mockValidSbomManagerLicense();

    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Test Application", "TEST", organization.getId());
    File scanFile = getScanFile("sbom/scan-with-sbom-data-creation-details.xml");
    String scanId = TemporaryEntity.uuid();
    File tempScanFile = tempDir.newFile();
    Files.createDirectories(insightWork.getScanDir(application.getId()).toPath());

    ThirdPartyScanContext context =
        new ThirdPartyScanContext("scanRequestId", application.getId(), SbomScanType.SBOM,
            new FileScanEntity(scanFile.toPath()),
            StageTypes.COMPLIANCE.getName());
    String scanRequestId = thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()), context, null);
    thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanId);
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.SBOM),
        any(ThirdPartyScanContext.class));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.SBOM, true, 2);

    var sbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(application.getId(),
        "a140fd3c3ded4bb0a640dc31e2904dc9");
    var thirdPartyFile = thirdPartyFileDAO.getById(sbomMetadata.getThirdPartyFileId());

    ThirdPartySbomMetadata expectedSbomMetadata = new ThirdPartySbomMetadata(thirdPartyFile.getId(),
        application.getId(),
        null,
        null,
        "urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79",
        "CycloneDx",
        "xml",
        "1.5",
        PENDING,
        new Date(),
        creationDetailsJson(),
        "SBOM",
        false, null);
    assertThirdPartySbomMetadata(thirdPartyFile, true, expectedSbomMetadata);
    assertThat(sbomMetadata.getFilename()).matches("\\p{XDigit}+\\.xml\\.gz");
    assertThat(thirdPartyFile.getFilename()).isEqualTo("test-bom.xml");
    assertExistingSbomFiles("%s/%s".formatted(application.getId(), sbomMetadata.getFilename()));
  }

  private void testHandle_SbomDependencyTree(final String s, final String s2) throws Exception {
    File scanFile = getScanFile(s);
    File tempScanFile = tempDir.newFile();

    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()),
        mockContext(scanFile), null);
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.SBOM),
        any(ThirdPartyScanContext.class));

    URL resource = getTestResource(s2);
    DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document expectedScan = db.parse(new File(resource.toURI()));
    String expectedSbom = getSbomNodeAsString(expectedScan, "/scan/item[1]/content");
    Document actualScan = db.parse(getScanXMLFile(tempScanFile));
    String actualSbom = getSbomNodeAsString(actualScan, "/scan/item[1]/content");

    assertThatJson(actualSbom).whenIgnoringPaths("components[*].properties[*].value")
        .isEqualTo(expectedSbom);

    assertExistingSbomFiles();
  }

  public String getSbomNodeAsString(Document rootDocument, String xPathString) throws Exception {
    XPath xPath = XPathFactory.newInstance().newXPath();
    return (String) xPath.compile(xPathString).evaluate(rootDocument, XPathConstants.STRING);
  }

  private File getScanXMLFile(File scanFile) throws Exception {
    File output = tempDir.newFile("scan-test.xml");
    try (GZIPInputStream gis = new GZIPInputStream(Files.newInputStream(scanFile.toPath()))) {
      IOUtils.copy(gis, Files.newOutputStream(output.toPath()));
    }
    return output;
  }

  private void assertEmptyItemElement(File scanFile) throws Exception {
    try (GZIPInputStream gis = new GZIPInputStream(Files.newInputStream(scanFile.toPath()))) {
      XmlPullParser parser = new MXParser();
      parser.setInput(new XmlStreamReader(gis));

      int eventType = parser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if ("item".equals(parser.getName())) {
          String content = parser.getAttributeValue(null, "contentType");
          Xpp3Dom itemElement = Xpp3Util.loadElement("item", parser);
          assertThat(itemElement.getChildren()).isEmpty();
          assertThat(content).isNull();
          assertThat(logOutput.getErrorMessages(loggerName)).isNullOrEmpty();
        }
        eventType = parser.next();
      }
    }
  }

  @Test
  public void testHandle_NoThirdPartyContent() throws Exception {
    File scanFile = getScanFile("scan-without-thirdparty-content.xml");
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempDir.newFile().toPath()), mockContext(scanFile), null);
    verify(thirdPartyScanResultsProcessorSpy, times(0)).createHandler(any(ItemContentType.class),
        any(ThirdPartyScanContext.class));

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_InvalidFile() throws Exception {
    File scanFile = getScanFile("empty-scan.xml");
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
        () -> thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
            new FileScanEntity(tempDir.newFile().toPath()), mockContext(scanFile), null))
        .withMessage("Error reading/processing third party scan content from scan file");
    verify(thirdPartyScanResultsProcessorSpy, times(0)).createHandler(any(ItemContentType.class),
        any(ThirdPartyScanContext.class));

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_cyclonedx_api_sbomManagerEnabled_maxSbom_reached() throws Exception {
    when(productLicense.hasFeature(LicensedFeature.SBOM_MANAGER)).thenReturn(true);
    when(productLicense.getMaxSboms()).thenReturn(0);
    IntStream.rangeClosed(1, 2).forEach(i -> createSbomMetadata());
    final Organization organization = tempEntity.newOrganization("Test Org");
    tempEntity.newApplication("Test Application", "TEST", organization.getId());
    File scanFile = getScanFile("sbom/scan-with-sbom-data-api.xml");
    File tempScan = tempDir.newFile();
    ThirdPartyScanContext context =
        new ThirdPartyScanContext("scanRequestId", DUMMY_APP_ID, SbomScanType.SBOM,
            new FileScanEntity(scanFile.toPath()),
            DEFAULT_STAGE_TYPE);
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScan.toPath()),
        context, null);

    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.SBOM),
        any(ThirdPartyScanContext.class));

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_SbomManagerEnabled_BuildStage_noSbomSaved() throws Exception {
    when(productLicense.hasFeature(LicensedFeature.SBOM_MANAGER)).thenReturn(true);
    createSbomMetadata();
    final Organization organization = tempEntity.newOrganization("Test Org");
    tempEntity.newApplication("Test Application", "TEST", organization.getId());
    File scanFile = getScanFile("sbom/scan-with-sbom-data-api.xml");
    File tempScan = tempDir.newFile();
    FileScanEntity fileScanEntity = new FileScanEntity(scanFile.toPath());
    ThirdPartyScanContext context =
        new ThirdPartyScanContext("scanRequestId", DUMMY_APP_ID, SbomScanType.SBOM, fileScanEntity,
            StageTypes.BUILD.getId());
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(fileScanEntity, new FileScanEntity(tempScan.toPath()), context,
        null);

    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.SBOM),
        any(ThirdPartyScanContext.class));

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_cdx_invalidFile() throws Exception {
    File scanFile = getScanFile("scan-invalid-cdx.xml");
    String scanId = TemporaryEntity.uuid();
    File tempScanFile = tempDir.newFile();

    ThirdPartyScanContext context =
        new ThirdPartyScanContext("scanRequestId", DUMMY_APP_ID, SbomScanType.SBOM,
            new FileScanEntity(scanFile.toPath()),
            StageTypes.RELEASE.getName());
    context.setIsValid(false);

    String scanRequestId = thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()), context, null);
    thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanId);
    List<ThirdPartyFile> thirdPartyFiles = thirdPartyFileDAO.getByScanId(scanId);
    assertThirdPartyFile(thirdPartyFiles, "third-party-simple-invalid-bom.xml");
    List<ThirdPartyFileCoordinate> fileCoordinate =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFiles.get(0).getId());
    assertThat(fileCoordinate).isNotEmpty();
    List<ThirdPartyCoordinateSecurity> coordinateSecurities =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(fileCoordinate.get(0).getId());
    List<ThirdPartyCoordinateLicense> coordinateLicenses =
        thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(fileCoordinate.get(0).getId());
    assertThat(coordinateSecurities).isEmpty();
    assertThat(coordinateLicenses).isEmpty();

    DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document actualScan = db.parse(getScanXMLFile(tempScanFile));
    String hasErrorProperty = getSbomNodeAsString(actualScan, "/scan/item[1]/@hasError");
    assertThat(hasErrorProperty).isEqualTo("true");

    assertExistingSbomFiles();
  }

  @Test
  public void testHandle_cdx_invalidFile_isValidTrue() throws Exception {
    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Test Application", "TEST", organization.getId());
    File scanFile = getScanFile("scan-invalid-cdx.xml");
    String scanId = TemporaryEntity.uuid();
    File tempScanFile = tempDir.newFile();

    ThirdPartyScanContext context =
        new ThirdPartyScanContext("scanRequestId", application.getId(), SbomScanType.SBOM,
            new FileScanEntity(scanFile.toPath()),
            StageTypes.RELEASE.getName());
    context.setIsValid(true);

    try {
      String scanRequestId = thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
          new FileScanEntity(tempScanFile.toPath()), context, null);
      thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanId);
    }
    catch (RuntimeException e) {
      assertThat(thirdPartyFileDAO.getByScanId(scanId)).isEmpty();
      assertExistingSbomFiles();
    }
  }

  @Test
  public void testHandle_cdx_validFile_isValidTrue() throws Exception {
    final Organization organization = tempEntity.newOrganization("Test Org");
    final Application application = tempEntity.newApplication("Test Application", "TEST", organization.getId());
    File scanFile = getScanFile("scan-valid-cdx.xml");
    String scanId = TemporaryEntity.uuid();
    File tempScanFile = tempDir.newFile();

    ThirdPartyScanContext context =
        new ThirdPartyScanContext("scanRequestId", application.getId(), SbomScanType.SBOM,
            new FileScanEntity(scanFile.toPath()),
            StageTypes.RELEASE.getName());
    context.setIsValid(true);

    String scanRequestId = thirdPartyScanResultsProcessorSpy.filterAndSaveData(new FileScanEntity(scanFile.toPath()),
        new FileScanEntity(tempScanFile.toPath()), context, null);
    thirdPartyScanDAO.updateScanIdForScanRequest(scanRequestId, scanId);
    List<ThirdPartyFile> thirdPartyFiles = thirdPartyFileDAO.getByScanId(scanId);
    assertThirdPartyFile(thirdPartyFiles, "third-party-simple-bom.xml");
    List<ThirdPartyFileCoordinate> fileCoordinate =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFiles.get(0).getId());
    assertThat(fileCoordinate).isNotEmpty();
    List<ThirdPartyCoordinateSecurity> coordinateSecurities =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(fileCoordinate.get(0).getId());
    List<ThirdPartyCoordinateLicense> coordinateLicenses =
        thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(fileCoordinate.get(0).getId());
    assertThat(coordinateSecurities).hasSize(1);
    assertThat(coordinateLicenses).hasSize(2);

    assertExistingSbomFiles();
  }

  private void assertLogOutput(final String message) {
    assertThat(logOutput.getErrorMessages(loggerName)).containsOnly(message);
  }

  private File getScanFile(final String fileName) throws Exception {
    URL resource = getTestResource(fileName);
    // Gzip the Third Party scan file
    File sonatypeScanGzipFile = tempDir.newFile(ScanFileNames.SONATYPE_SCAN_FILENAME);
    try (GZIPOutputStream gzipStream = new GZIPOutputStream(Files.newOutputStream(sonatypeScanGzipFile.toPath()))) {
      FileUtils.copyFile(new File(resource.toURI()), gzipStream);
    }
    return sonatypeScanGzipFile;
  }

  private URL getTestResource(final String fileName) {
    return getClass().getResource("/ThirdPartyResultsProcessorTest/" + fileName);
  }

  private void assertFilteredThirdPartyScanContentFile(
      File scanFile,
      ItemContentType itemContentType,
      boolean optionalValuesPresent,
      int expectedComponentCount) throws Exception
  {
    try (GZIPInputStream gis = new GZIPInputStream(Files.newInputStream(scanFile.toPath()))) {
      XmlPullParser parser = new MXParser();
      parser.setInput(new XmlStreamReader(gis));

      int eventType = parser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG) {
          String elementName = parser.getName();
          if ("item".equals(elementName)) {
            String contentType = parser.getAttributeValue(null, "contentType");
            assertThat(contentType).isNotNull();
            Xpp3Dom itemElement = Xpp3Util.loadElement("item", parser);
            // Item element is empty, so it's skipped
            if (itemElement.getChildCount() > 0) {
              Xpp3Dom contentElement = itemElement.getChild("content");
              assertThat(contentElement.getValue()).isNotNull();
              if (ItemContentType.CLAIR_SCANNER == itemContentType) {
                assertFilteredClairScanContentFile(contentElement.getValue(), contentType, expectedComponentCount);
              }
              // the SPDX content is converted to CycloneDx during content handling and filtering,
              // so the contentType for the filtered content is SBOM not SPDX
              else if (ItemContentType.SPDX == itemContentType) {
                assertFilteredScanContentFile(contentElement.getValue(), contentType, optionalValuesPresent,
                    expectedComponentCount, ItemContentType.SBOM);
              }
              else if (ItemContentType.SBOM == itemContentType) {
                assertFilteredScanContentFile(contentElement.getValue(), contentType, optionalValuesPresent,
                    expectedComponentCount, ItemContentType.SBOM);
              }
              else if (ItemContentType.CONTAINER_URI == itemContentType) {
                assertFilteredScanContentFile(contentElement.getValue(), contentType, optionalValuesPresent,
                    expectedComponentCount, ItemContentType.CONTAINER_URI);
              }
              else if (ItemContentType.CONTAINER_URI_SONATYPE == itemContentType) {
                assertFilteredScanContentFile(contentElement.getValue(), contentType, optionalValuesPresent,
                    expectedComponentCount, ItemContentType.CONTAINER_URI_SONATYPE);
              }
              else if (IAC_FILE == itemContentType) {
                final String expectedIacXmlContent = "terraform";
                assertThat(contentElement.getValue()).isEqualTo(expectedIacXmlContent);
              }
            }
            else {
              assertLogOutput("scan file scan.xml.gz contained a third party scan item " + itemContentType
                  + " without any content");
            }
          }
        }
        eventType = parser.next();
      }
    }
  }

  private void assertFilteredScanContentFile(
      final String json,
      final String contentType,
      final boolean optionalValuesPresent,
      final int expectedComponentCount,
      final ItemContentType itemContentType) throws ParseException
  {
    assertThat(contentType).isEqualTo(itemContentType.name());
    Bom bom = SbomTestHelper.parseToCycloneDxBom(json);
    assertThat(bom).isNotNull();
    assertThat(bom.getComponents()).hasSize(expectedComponentCount);
    assertThat(bom.getSerialNumber()).isNull();
    assertThat(bom.getVersion()).isNotNull();
    assertThat(bom.getExternalReferences()).isNull();
    assertThat(bom.getExternalReferences()).isNull();

    for (Component component : bom.getComponents()) {
      assertThat(component.getComponents()).isNull();
      assertThat(component.getName()).isNotNull();
      assertThat(component.getVersion()).isNotNull();
      assertThat(component.getType()).isNotNull();
      if (optionalValuesPresent) {
        assertThat(component.getPurl()).isNotNull();
      }
      else {
        assertThat(component.getPurl()).isNull();
        assertThat(component.getHashes()).isNull();
      }

      assertThat(component.getComponents()).isNull();
      assertThat(component.getCopyright()).isNull();
      assertThat(component.getDescription()).isNull();
      assertThat(component.getExternalReferences()).isNull();
      assertThat(component.getExtensibleTypes()).isNull();
      if (component.getSwid() != null) {
        assertThat(component.getAuthor()).isNull();
      }
      else {
        assertThat(component.getGroup()).isNotNull();
      }
      assertThat(component.getLicenses()).isNull();
      assertThat(component.getPedigree()).isNull();
      assertThat(component.getPublisher()).isNull();
    }
  }

  private void assertFilteredClairScanContentFile(String json, String contentType, int expectedComponentCount) {
    assertThat(contentType).isEqualTo(ItemContentType.CLAIR_SCANNER.name());
    ClairScannerResult clairScannerResult =
        GSON.fromJson(StringEscapeUtils.unescapeXml(json), ClairScannerResult.class);
    assertThat(clairScannerResult).isNotNull();
    assertThat(clairScannerResult.getImage()).isNull();
    assertThat(clairScannerResult.getVulnerabilities()).isNotNull();
    assertThat(clairScannerResult.getVulnerabilities()).hasSize(expectedComponentCount);

    for (ClairScannerVulnerability vulnerability : clairScannerResult.getVulnerabilities()) {
      assertThat(vulnerability).isNotNull();
      assertThat(vulnerability.getFeatureName()).isNotNull();
      assertThat(vulnerability.getFeatureVersion()).isNotNull();
      assertThat(vulnerability.getNamespace()).isNotNull();

      assertThat(vulnerability.getDescription()).isNull();
      assertThat(vulnerability.getVulnerability()).isNull();
      assertThat(vulnerability.getLink()).isNull();
      assertThat(vulnerability.getSeverity()).isNull();
    }
  }

  private void assertThirdPartyFile(List<ThirdPartyFile> thirdPartyFileList, String... expectedFilenames) {
    assertThat(thirdPartyFileList).extracting(ThirdPartyFile::getFilename)
        .containsExactlyInAnyOrder(expectedFilenames);
  }

  private void assertThirdPartySbomMetadata(
      ThirdPartyFile thirdPartyFile,
      boolean expectRecord,
      ThirdPartySbomMetadata expectedSbomMetadata)
  {
    ThirdPartySbomMetadata thirdPartySbomMetadata =
        thirdPartySbomMetadataDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    if (expectRecord) {
      assertThat(thirdPartySbomMetadata).isNotNull();
      assertThat(thirdPartySbomMetadata.getId()).isNotNull();
      assertThat(thirdPartySbomMetadata.getCreatedAt()).isNotNull();
      assertThat(thirdPartySbomMetadata.getSbomVersion()).isNotNull();
      assertThat(thirdPartySbomMetadata.getFilename()).isNotNull();
      assertThat(thirdPartySbomMetadata.getStatus()).isEqualTo(expectedSbomMetadata.getStatus());
      assertThat(thirdPartySbomMetadata.getThirdPartyFileId()).isEqualTo(expectedSbomMetadata.getThirdPartyFileId());
      assertThat(thirdPartySbomMetadata.getApplicationId()).isEqualTo(expectedSbomMetadata.getApplicationId());
      assertThat(thirdPartySbomMetadata.getSpec()).isEqualTo(expectedSbomMetadata.getSpec());
      assertThat(thirdPartySbomMetadata.getSpecFormat()).isEqualTo(expectedSbomMetadata.getSpecFormat());
      assertThat(thirdPartySbomMetadata.getSpecVersion()).isEqualTo(expectedSbomMetadata.getSpecVersion());
      assertThat(thirdPartySbomMetadata.getMetadataJson()).isEqualTo(expectedSbomMetadata.getMetadataJson());
      assertThat(thirdPartySbomMetadata.getScanType()).isEqualTo(expectedSbomMetadata.getScanType());
    }
    else {
      assertThat(thirdPartySbomMetadata).isNull();
    }
  }

  private void assertTelemetryData(TelemetryData telemetryData, List<String> expectedContentTypes) {
    assertThat(telemetryData.getAttributes()).hasSize(5);
    assertThat(telemetryData.getAttributes())
        .contains(entry("application_id", "appId"),
            entry("content_type_list", expectedContentTypes),
            entry("stage_id", "build"),
            entry("source", "api"),
            entry("user_agent", "agent"));
  }

  private TelemetryData buildThirdPartyScanTelemetryData() {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("application_id", "appId");
    attributes.put("stage_id", "build");
    attributes.put("source", "api");
    attributes.put("user_agent", "agent");

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.THIRD_PARTY_SCAN_USAGE);
    telemetryData.setAttributes(attributes);
    return telemetryData;
  }

  private void createSbomMetadata() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata metadata =
        ThirdPartySbomMetadataTestUtil.createSbomMetadata(ACTIVE, application.getId(), thirdPartyFile.getId());

    thirdPartySbomMetadataDAO.insert(metadata);
  }

  private String creationDetailsToolsOnlyJson() {
    return "{\"created\":\"2023-07-20T16:39:54Z\",\"tools\":[{\"name\":\"Sonatype IQ Server\",\"version\"" +
        ":\"1.166.0-SNAPSHOT\"}]}";
  }

  private String creationDetailsJson() {
    return "{\"type\":\"application\",\"created\":\"2024-02-29T23:41:22Z\",\"creators\":[{\"type\":\"Author\"," +
        "\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"},{\"type\":" +
        "\"Manufacturer\",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"," +
        "\"url\":\"example.com,example2.com,example3.com\"},{\"type\":\"Manufacturer\",\"name\":\"Jane Doe\"," +
        "\"email\":\"Jane.doe@example.com\",\"phone\":\"1-800-222-2222\",\"url\":\"example.com,example2.com," +
        "example3.com\"},{\"type\":\"Supplier\",\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\"" +
        ":\"1-800-111-1111\",\"url\":\"example.com,example2.com,example3.com\"},{\"type\":\"Supplier\",\"name\":" +
        "\"Jane Doe\",\"email\":\"Jane.doe@example.com\",\"phone\":\"1-800-222-2222\",\"url\":\"example.com," +
        "example2.com,example3.com\"}],\"tools\":[{\"type\":\"application\",\"name\":\"Tool\",\"version\"" +
        ":\"1.0-RELEASE\"}]}";
  }

  private void mockValidSbomManagerLicense() {
    when(productLicense.hasFeature(LicensedFeature.SBOM_MANAGER)).thenReturn(true);
    when(productLicense.getMaxSboms()).thenReturn(50);
    when(productLicense.getStageTypes()).thenReturn(new HashSet<>(Arrays.asList(StageTypes.COMPLIANCE)));
  }

  private static ThirdPartyScanContext mockContext(final File scanFile) {
    ThirdPartyScanContext thirdPartyScanContext =
        new ThirdPartyScanContext("scanRequestId", DUMMY_APP_ID, SbomScanType.SBOM,
            new FileScanEntity(scanFile.toPath()),
            DEFAULT_STAGE_TYPE);
    return thirdPartyScanContext;
  }

  private void assertExistingSbomFiles(String... expectedPaths) throws IOException {
    existingFilesHelper.assertExistingSbomFiles(expectedPaths);
  }
}
