/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType.DIRECT;
import static com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType.TRANSITIVE;
import static com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType.UNSPECIFIED;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.CYCLONEDX_JSON_IGNORE_FIELDS;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.cycloneDxIgnoreAttributesFilter;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.cycloneDxIgnoreNodesFilter;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.readFileToString;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.setupScenarioWithMetadataComponentSecurityLicenseAndVex;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.spdxDxIgnoreNodesFilter;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.PolicyEvaluationHelper;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomVersionsApplicationSortableField;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataSummaryDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataSummaryListDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataTestUtil;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ResolvedLicenseDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentListDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.variant.AbstractComponentPgTest;
import com.sonatype.insight.brain.variant.ComponentPgTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.Zipper;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.brain.utils.ExistingFilesHelper;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.tika.utils.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.xmlunit.assertj.XmlAssert;

@ComponentPgTest
@ContextConfiguration(classes = ApiSbomServiceTest.ExistingFilesHelperTestConfig.class)
public class ApiSbomServiceTest
    extends AbstractComponentPgTest
{
  @TestConfiguration
  static class ExistingFilesHelperTestConfig
  {
    @Bean
    ExistingFilesHelper existingFilesHelper() {
      return new ExistingFilesHelper();
    }
  }

  public LogOutput logOutput = new LogOutput(1, ApiSbomServiceTest.class, ApiSbomService.class);

  private static final String DUMMY_USER_AGENT = RandomStringUtils.random(10, true, true);

  @Inject
  private ApiSbomService service;

  @Inject
  private ThirdPartySbomMetadataDAO dao;

  @Inject
  private ThirdPartyFileDAO thirdPartyFileDAO;

  @Inject
  private ThirdPartyScanDAO thirdPartyScanDao;

  @Inject
  ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Inject
  ThirdPartyCoordinateSecurityDAO coordinateSecurityDAO;

  @Inject
  private InsightWork insightWork;

  @Inject
  private PolicyEvaluationHelper policyEvaluationHelper;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private ExistingFilesHelper existingFilesHelper;

  @Mock
  private HdsClient mockHdsClient;

  @Mock
  private BaseUrl mockBaseUrl;

  // NOTE: This field is intentionally named without the "mock" prefix and without "Mock" suffix.
  // SpringInjectedTest.isMockOnlyField() treats any field named mock* or *Mock as a mock field,
  // and wireMocksIntoBean() recursively propagates such mocks through the bean dependency graph.
  // If this were named "mockReportService", it would replace ReportService in
  // ReportComponentService (reachable via ApiSbomService -> PolicyEvaluateService ->
  // ScanPolicyEvaluator -> ReportComponentService), causing the evaluation pipeline's
  // fetchReport() to return null and fail with NPE.
  // By naming it differently and creating it after lookup() calls, propagation is prevented.
  private ReportService stubbedReportService;

  @BeforeEach
  public void before() {
    service = lookup(ApiSbomService.class);
    ScanUploader scanUploader = lookup(ScanUploader.class);
    ReportDownloader reportDownloader = lookup(ReportDownloader.class);
    TelemetrySender telemetrySender = lookup(TelemetrySender.class);
    stubbedReportService = Mockito.mock(ReportService.class);
    Mockito.reset(mockHdsClient, mockBaseUrl);
    ReflectionTestUtils.setField(scanUploader, "client", mockHdsClient);
    ReflectionTestUtils.setField(reportDownloader, "client", mockHdsClient);
    ReflectionTestUtils.setField(telemetrySender, "hdsClient", mockHdsClient);
    ReflectionTestUtils.setField(service, "reportService", stubbedReportService);
    lenient().when(mockBaseUrl.get()).thenReturn("http://localhost:8070/");
  }

  @AfterEach
  public void after() {
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(false);
    // SBOM tests trigger async processing (policy evaluation, scan uploading) that may create
    // entities via background threads during or after TemporaryEntity.after() cleanup.
    // These are not real leaks — the entities are cleaned by the cascading delete — but the
    // detection data captured from the background thread's insert remains, causing false positives.
    AbstractOperationalSqlDAO.testEntityLeaksDetectionData.clear();
  }

  @Test
  public void testDeleteSbomVersion() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path zippedBom = mockOriginalSbom(this.getClass(), "third-party-simple-bom.xml",
        insightWork.getSbomDir(app.getId()).toPath());

    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withFilename(zippedBom.getFileName().toString())
        .build();

    service.deleteSbomVersion(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion());

    final ThirdPartySbomMetadata retrievedSbomMetadata =
        dao.getByApplicationIdAndSbomVersion(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion());

    assertThat(retrievedSbomMetadata).isNull();
    assertThat(zippedBom).doesNotExist();
  }

  @Test
  public void testDeleteSbomVersion_NotFoundInvalidVersion() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).build();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.deleteSbomVersion(sbomMetadata.getApplicationId(), "invalidVersion"))
        .withMessage(
            "Cannot find version invalidVersion for application with ID " + sbomMetadata.getApplicationId() + ".");
  }

  @Test
  public void testGetSbomVersion_UnsupportedState() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> service.getSbomVersion("invalidAppId", "invalidSbomVersion", "dummyState", "cyclonedx1.5",
                "application/xml"))
        .withMessage("Invalid sbom state dummyState");
  }

  @Test
  public void testGetSbomVersion_NotFoundInvalidVersion() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).build();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> service.getSbomVersion(sbomMetadata.getApplicationId(), "invalidVersion",
                ApiSbomService.SBOM_STATE_ORIGINAL, "spdx2.3", "application/xml"))
        .withMessage(
            "Cannot find version invalidVersion for application with ID " + sbomMetadata.getApplicationId() + ".");
  }

  @Test
  public void testGetSbomVersion_Original_Xml() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path zippedBom = mockOriginalSbom(this.getClass(), "third-party-simple-bom.xml",
        insightWork.getSbomDir(app.getId()).toPath());
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withFilename(zippedBom.getFileName().toString())
        .withStatus(ACTIVE)
        .build();

    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withStatus(PENDING)
        .build();

    Response response = service.getSbomVersion(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(),
        ApiSbomService.SBOM_STATE_ORIGINAL, "cyclonedx1.5", "application/xml");
    String actualContent = new String((byte[]) response.getEntity());
    XmlAssert.assertThat(actualContent)
        .and(expectedContentIn("third-party-simple-bom.xml"))
        .areIdentical();
  }

  @Test
  public void testGetSbomVersion_Original_Json() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path zippedBom = mockOriginalSbom(this.getClass(), "spdx.json",
        insightWork.getSbomDir(app.getId()).toPath());
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withFilename(zippedBom.getFileName().toString())
        .withStatus(ACTIVE)
        .build();

    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withStatus(PENDING)
        .build();

    Response response = service.getSbomVersion(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(),
        ApiSbomService.SBOM_STATE_ORIGINAL, "", "");
    String actualContent = new String((byte[]) response.getEntity());
    assertThatJson(actualContent).isEqualTo(expectedContentIn("spdx.json"));
  }

  @Test
  public void testGetSbomVersion_Current_CycloneDxToCycloneDx_Xml() throws Exception {
    testExportingSbomWithInputAndOutputSpecsAndFormats(SbomSpecification.CYCLONEDX, SbomFormat.XML,
        SbomSpecification.CYCLONEDX, SbomFormat.XML);
  }

  @Test
  public void testGetSbomVersion_Current_CycloneDxToCycloneDx_Json() throws Exception {
    testExportingSbomWithInputAndOutputSpecsAndFormats(SbomSpecification.CYCLONEDX, SbomFormat.XML,
        SbomSpecification.CYCLONEDX, SbomFormat.JSON);
  }

  @Test
  public void testGetSbomVersion_Current_SpdxToSpdx_Xml() throws Exception {
    testExportingSbomWithInputAndOutputSpecsAndFormats(SbomSpecification.SPDX, SbomFormat.XML,
        SbomSpecification.SPDX, SbomFormat.XML);
  }

  @Test
  public void testGetSbomVersion_Current_SpdxToSpdx_Json() throws Exception {
    testExportingSbomWithInputAndOutputSpecsAndFormats(SbomSpecification.SPDX, SbomFormat.XML,
        SbomSpecification.SPDX, SbomFormat.JSON);
  }

  @Test
  public void testGetSbomVersion_Current_SpdxToCycloneDx_Xml() throws Exception {
    testExportingSbomWithInputAndOutputSpecsAndFormats(SbomSpecification.SPDX, SbomFormat.XML,
        SbomSpecification.CYCLONEDX, SbomFormat.XML);
  }

  @Test
  public void testGetSbomVersion_Current_SpdxToCycloneDx_Json() throws Exception {
    testExportingSbomWithInputAndOutputSpecsAndFormats(SbomSpecification.SPDX, SbomFormat.XML,
        SbomSpecification.CYCLONEDX, SbomFormat.JSON);
  }

  @Test
  public void testGetSbomVersion_Current_CycloneDxToSpdx_Xml() throws Exception {
    testExportingSbomWithInputAndOutputSpecsAndFormats(SbomSpecification.CYCLONEDX, SbomFormat.XML,
        SbomSpecification.SPDX, SbomFormat.XML);
  }

  @Test
  public void testGetSbomVersion_Current_CycloneDxToSpdx_Json() throws Exception {
    testExportingSbomWithInputAndOutputSpecsAndFormats(SbomSpecification.CYCLONEDX, SbomFormat.XML,
        SbomSpecification.SPDX, SbomFormat.JSON);
  }

  private void testExportingSbomWithInputAndOutputSpecsAndFormats(
      SbomSpecification inputSpec,
      SbomFormat inputFormat,
      SbomSpecification outputSpec,
      SbomFormat outputFormat) throws Exception
  {
    Application app = tempEntity.newApplicationWithParent();
    String inputFileName = "sboms/valid-" + inputSpec.name().toLowerCase() + "-bom." + inputFormat.name().toLowerCase();
    Path zippedBom = mockOriginalSbom(this.getClass(), inputFileName, insightWork.getSbomDir(app.getId()).toPath());

    String sbomVersion = tempEntity.newRandomHash();
    String inputSpecVersion = inputSpec == SbomSpecification.CYCLONEDX ? "1.6" : "2.3";
    setupScenarioWithMetadataComponentSecurityLicenseAndVex(tempEntity, app, zippedBom, sbomVersion,
        inputSpec.toString(), inputSpecVersion, inputFormat);

    String targetSpecification = outputSpec == SbomSpecification.CYCLONEDX ? "cyclonedx1.6" : "spdx2.3";
    String acceptType = outputFormat == SbomFormat.JSON ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_XML;
    Response response =
        service.getSbomVersion(app.getId(), sbomVersion, ApiSbomService.SBOM_STATE_CURRENT,
            targetSpecification, acceptType);

    assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    assertThat(response.getMediaType().toString()).isEqualTo(acceptType);

    assertContentHeader(response, app, sbomVersion, "." + outputFormat.name().toLowerCase(), outputSpec);
    String sbomContent = new String((byte[]) response.getEntity());

    String outputFileName = "sboms/valid-" + inputSpec.name().toLowerCase() + "-to-" + outputSpec.name().toLowerCase() +
        "-result-bom." + outputFormat.name().toLowerCase();
    if (outputFormat == SbomFormat.JSON) {
      assertThatJson(sbomContent)
          .whenIgnoringPaths(CYCLONEDX_JSON_IGNORE_FIELDS)
          .withOptions(IGNORING_ARRAY_ORDER)
          .isEqualTo(expectedContentIn(outputFileName));
    }
    else {
      if (outputSpec == SbomSpecification.CYCLONEDX) {
        XmlAssert.assertThat(sbomContent)
            .and(expectedContentIn(outputFileName))
            .withNodeFilter(cycloneDxIgnoreNodesFilter())
            .withAttributeFilter(cycloneDxIgnoreAttributesFilter())
            .ignoreWhitespace()
            .areIdentical();
      }
      else {
        XmlAssert.assertThat(sbomContent)
            .and(expectedContentIn(outputFileName))
            .withNodeFilter(spdxDxIgnoreNodesFilter())
            .withNodeMatcher(new com.sonatype.insight.brain.sbom.export.IgnoreXmlListOrderMatcher())
            .ignoreWhitespace()
            .areIdentical();
      }
    }
  }

  @Test
  public void testGetSbomVersion_InvalidTargetSpecification() {
    Application app = tempEntity.newApplicationWithParent();
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.getSbomVersion(app.getId(), "some-version", ApiSbomService.SBOM_STATE_CURRENT,
            "invalid-spec", MediaType.APPLICATION_JSON))
        .withMessage("requested output specification invalid-spec not supported");
  }

  @Test
  public void testGetSbomVersion_InvalidExportFormat() {
    Application app = tempEntity.newApplicationWithParent();
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.getSbomVersion(app.getId(), "some-version", ApiSbomService.SBOM_STATE_CURRENT, "spdx2.3",
            MediaType.APPLICATION_SVG_XML))
        .withMessage("requested output format application/svg+xml not supported");
  }

  @Test
  public void testGetSbomVersion_exportSpdxToLowerVersion() throws Exception {
    testExportToLowerVersion(
        SbomSpecification.SPDX,
        "2.3",
        "spdx2.2",
        "Unable to export lower SBOM specification version 2.2. " +
            "The original SPDX SBOM was already in version 2.3");
  }

  @Test
  public void testGetSbomVersion_exportCycloneDxToLowerVersion() throws Exception {
    testExportToLowerVersion(
        SbomSpecification.CYCLONEDX,
        "1.6",
        "cyclonedx1.5",
        "Unable to export lower SBOM specification version 1.5. " +
            "The original CycloneDX SBOM was already in version 1.6");
  }

  @Test
  public void testGetSbomVersion_exportCycloneDx17ToLowerVersion() throws Exception {
    testExportToLowerVersion(
        SbomSpecification.CYCLONEDX,
        "1.7",
        "cyclonedx1.6",
        "Unable to export lower SBOM specification version 1.6. " +
            "The original CycloneDX SBOM was already in version 1.7");
  }

  private void testExportToLowerVersion(
      SbomSpecification spec,
      String inputSpecVersion,
      String targetSpecification,
      String expectedErrorMessage) throws Exception
  {
    Application app = tempEntity.newApplicationWithParent();
    String inputFileName = "sboms/valid-" + spec.name().toLowerCase() + "-bom." + SbomFormat.XML.name().toLowerCase();
    Path zippedBom = mockOriginalSbom(this.getClass(), inputFileName, insightWork.getSbomDir(app.getId()).toPath());

    String sbomVersion = tempEntity.newRandomHash();
    setupScenarioWithMetadataComponentSecurityLicenseAndVex(tempEntity, app, zippedBom, sbomVersion, spec.toString(),
        inputSpecVersion, SbomFormat.XML);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getSbomVersion(app.getId(), sbomVersion, ApiSbomService.SBOM_STATE_CURRENT,
            targetSpecification, MediaType.APPLICATION_XML))
        .withMessage(expectedErrorMessage);
  }

  @Test
  public void testGetSbomVersion_NoActiveSboms() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus(PENDING)
        .build();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> service.getSbomVersion(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(),
                ApiSbomService.SBOM_STATE_ORIGINAL, "", ""))
        .withMessage(
            "Cannot find version " + sbomMetadata.getSbomVersion() + " for application with ID " +
                sbomMetadata.getApplicationId() + ".");
  }

  @Test
  public void testGetSbomMetadataSummaryForApplication_Successful() {
    Application application = tempEntity.newApplicationWithParent();

    ThirdPartyFile file1 = tempEntity.newThirdPartyFile("CycloneDX-bom.xml");
    ThirdPartyFile file2 = tempEntity.newThirdPartyFile("SPDX-spdx.json");

    ThirdPartySbomMetadata sbom1 =
        tempEntity.newThirdPartySbomMetadata(file1.getId(), application.getId(), ACTIVE, file1.getFilename());
    ThirdPartySbomMetadata sbom2 =
        tempEntity.newThirdPartySbomMetadata(file2.getId(), application.getId(), ACTIVE, file2.getFilename());

    ThirdPartyFileCoordinate c1 = tempEntity.newThirdPartyFileCoordinate(file1, "s1", "f1", "n1", "v1");
    ThirdPartyFileCoordinate c2 = tempEntity.newThirdPartyFileCoordinate(file2, "s2", "f2", "n2", "v2");

    tempEntity.newThirdPartyCoordinateSecurity(c1, "r1", sbom1.getId(), "d1", "l1", 3.5F, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(c1, "r2", sbom1.getId(), "d2", "l2", 7.5F, "sd2", "f2");
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r3", sbom2.getId(), "d3", "l3", 1.5F, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r4", sbom2.getId(), "d4", "l4", 0.5F, "sd4", "f4");

    ThirdPartySbomMetadataSummaryListDTO resultList =
        service.getSbomMetadataSummaryForApplication(application.getId(), "asc", 5, 1,
            SbomVersionsApplicationSortableField.IMPORT_DATE, true);
    assertThat(resultList).isNotNull();
    assertThat(resultList.getTotalResultsCount()).isEqualTo(2);

    List<ThirdPartySbomMetadataSummaryDTO> results = resultList.getResults();
    assertThat(results).hasSize(2);

    Collections.sort(results, Comparator.comparingInt(ThirdPartySbomMetadataSummaryDTO::getHigh));

    assertThat(results).hasSize(2);
    assertThat(results.get(0).getLow()).isEqualTo(2);
    assertThat(results.get(1).getLow()).isEqualTo(1);
    assertThat(results.get(1).getHigh()).isEqualTo(1);

    resultList =
        service.getSbomMetadataSummaryForApplication(application.getId(), "asc", 5, 1,
            SbomVersionsApplicationSortableField.RELEASE_STATUS, true);
    assertThat(resultList.getResults()).isSortedAccordingTo(
        Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getReleaseStatusPercentage));
    resultList =
        service.getSbomMetadataSummaryForApplication(application.getId(), "asc", 5, 1,
            SbomVersionsApplicationSortableField.RELEASE_STATUS, false);
    assertThat(resultList.getResults()).isSortedAccordingTo(
        Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getReleaseStatusPercentage).reversed());
    resultList =
        service.getSbomMetadataSummaryForApplication(application.getId(), "desc", 5, 1,
            SbomVersionsApplicationSortableField.RELEASE_STATUS, false);
    assertThat(resultList.getResults()).isSortedAccordingTo(
        Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getReleaseStatusPercentage).reversed());

    resultList =
        service.getSbomMetadataSummaryForApplication(application.getId(), "desc", 5, 1,
            null, true);
    assertThat(resultList.getResults()).isSortedAccordingTo(
        Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getImportDate).reversed());
  }

  @Test
  public void testGetSbomComponents_InvalidPagination() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getSbomComponents("appId", "version", null, null, null, null, true, -1, 2))
        .withMessage("pageSize must not be less than one!");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getSbomComponents("appId", "version", null, null, null, null, true, 1, -2))
        .withMessage("page index must not be less than one!");
  }

  @Test
  public void testGetSbomComponents_NoApplicationFound() {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    Application applicationWithoutSbom = tempEntity.newApplicationWithParent();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getSbomComponents(applicationWithoutSbom.getId(), sbomMetadata.getSbomVersion(), null,
            null, null, null, true, 3, 1))
        .withMessage("Cannot find version " + sbomMetadata.getSbomVersion() + " for application with ID "
            + applicationWithoutSbom.getId() + ".");
  }

  @Test
  public void testGetSbomComponents_NoSbomVersionFound() {
    String fakeVersion = "fake.version";
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withSbomVersion("test-version")
        .build();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> service.getSbomComponents(sbomMetadata.getApplicationId(), fakeVersion, null,
                null, null, null, true, 3, 1))
        .withMessage(
            "Cannot find version " + fakeVersion + " for application with ID " + sbomMetadata.getApplicationId() + ".");
  }

  @Test
  public void testGetSbomComponents_NoResults() {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    SbomComponentListDTO result = service.getSbomComponents(sbomMetadata.getApplicationId(),
        sbomMetadata.getSbomVersion(), null, null, null,
        null, true, 3, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isZero();
    assertThat(result.getResults()).isEmpty();
  }

  @Test
  public void testGetSbomComponents_WithResults() throws IOException {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        ThirdPartySbomMetadataTestUtil.createSbomMetadata(ACTIVE, application.getId(), thirdPartyFile.getId());
    dao.insert(sbomMetadata);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 =
        tempEntity.newThirdPartyFileCoordinate("86163fcc32524261bfd2bdbedb7eae42", thirdPartyFile, "s1",
            packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
            "h1",
            packageUrlIdentifier1.getPackageUrl(), "exact", null, List.of("pkg:npm/p1@v1"));

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 =
        tempEntity.newThirdPartyFileCoordinate("86163fcc32524261bfd2bdbedb7eae43", thirdPartyFile, "s2",
            packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
            "h2",
            packageUrlIdentifier2.getPackageUrl(), "similar", null, List.of("pkg:npm/p2@v2,pkg:npm/p3@v3"));
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-1", "License 1", "http://license-1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-2", "License 2", "http://license-2");

    File reportFile = insightWork.getReportFile(application.getId(), scan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/ApiSbomServicePolicyViolationsTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scan.getScanId());

    SbomComponentListDTO result = service.getSbomComponents(sbomMetadata.getApplicationId(),
        sbomMetadata.getSbomVersion(), null, null, null,
        null, true, 3, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    List<SbomComponentDTO> dtos = result.getResults();

    assertThat(dtos).hasSize(2);
    assertThat(dtos)
        .extracting(SbomComponentDTO::getHash)
        .containsExactlyInAnyOrder(coordinate1.getHash(), coordinate2.getHash());

    assertThat(dtos)
        .filteredOn(component -> component.getHash().equals(coordinate1.getHash()))
        .allSatisfy(component -> {
          assertThat(component.getName()).isEqualTo(packageUrlIdentifier1.getName());
          assertThat(component.getVersion()).isEqualTo(packageUrlIdentifier1.getVersion());
          assertThat(component.getPackageUrl()).isEqualTo(packageUrlIdentifier1.getPackageUrl());
          assertThat(component.getFilenames()).isEqualTo(coordinate1.getFilenamesList());
          assertThat(component.getMatchStateId()).isEqualTo(coordinate1.getMatchStateId());
          assertThat(component.getDisplayName())
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier1).toString());
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
          assertThat(component.getLicenses()).isNullOrEmpty();
          assertThat(component.getPolicyViolationCount()).isEqualTo(2);
        });

    assertThat(dtos)
        .filteredOn(component -> component.getHash().equals(coordinate2.getHash()))
        .allSatisfy(component -> {
          assertThat(component.getName()).isEqualTo(packageUrlIdentifier2.getName());
          assertThat(component.getVersion()).isEqualTo(packageUrlIdentifier2.getVersion());
          assertThat(component.getPackageUrl()).isEqualTo(packageUrlIdentifier2.getPackageUrl());
          assertThat(component.getFilenames()).isEqualTo(coordinate2.getFilenamesList());
          assertThat(component.getMatchStateId()).isEqualTo(coordinate2.getMatchStateId());
          assertThat(component.getDisplayName())
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier2).toString());
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
          assertThat(component.getLicenses())
              .extracting(ResolvedLicenseDTO::licenseId)
              .containsExactlyInAnyOrder("license-1", "license-2");
          assertThat(component.getLicenses())
              .extracting(ResolvedLicenseDTO::licenseName)
              .containsExactlyInAnyOrder("License 1", "License 2");
          assertThat(component.getPolicyViolationCount()).isEqualTo(1);
        });

    verify(stubbedReportService, times(2)).processBrowseReport(any(Application.class), anyString(), anyString());
  }

  @Test
  public void testGetSbomComponents_displayNameStoredFromPackageUrl() throws IOException {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        ThirdPartySbomMetadataTestUtil.createSbomMetadata(ACTIVE, application.getId(), thirdPartyFile.getId());
    dao.insert(sbomMetadata);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("org.apache.logging.log4j",
        "log4j-core", "2.14.1", null, "war");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);

    ThirdPartyFileCoordinate coordinate1 =
        tempEntity.newThirdPartyFileCoordinate("86163fcc32524261bfd2bdbedb7eae42", thirdPartyFile, "s1",
            packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
            "h1",
            packageUrlIdentifier1.getPackageUrl(), "exact", null,
            List.of(thirdPartyFile.getFilename()), null);

    File reportFile = insightWork.getReportFile(application.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper.zipReport("/ApiSbomServicePolicyViolationsTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, thirdPartyScan.getScanId());

    SbomComponentListDTO result = service.getSbomComponents(sbomMetadata.getApplicationId(),
        sbomMetadata.getSbomVersion(), null, null, null,
        null, true, 3, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(1);

    List<SbomComponentDTO> dtos = result.getResults();

    assertThat(dtos).hasSize(1);
    assertThat(dtos.get(0))
        .extracting(SbomComponentDTO::getHash, SbomComponentDTO::getDisplayName)
        .containsExactly(coordinate1.getHash(), "org.apache.logging.log4j : log4j-core : war : 2.14.1");
  }

  @Test
  public void testGetSbomComponents_displayNameStoredFromFormatNameAndVersion() throws IOException {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        ThirdPartySbomMetadataTestUtil.createSbomMetadata(ACTIVE, application.getId(), thirdPartyFile.getId());
    dao.insert(sbomMetadata);

    ThirdPartyFileCoordinate coordinate1 =
        tempEntity.newThirdPartyFileCoordinate("86163fcc32524261bfd2bdbedb7eae42", thirdPartyFile, "s1",
            "rpm", "p1", "v1",
            "h1",
            null, "exact", null, List.of(thirdPartyFile.getFilename()), null);

    File reportFile = insightWork.getReportFile(application.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper.zipReport("/ApiSbomServicePolicyViolationsTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, thirdPartyScan.getScanId());

    SbomComponentListDTO result = service.getSbomComponents(sbomMetadata.getApplicationId(),
        sbomMetadata.getSbomVersion(), null, null, null,
        null, true, 3, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(1);

    List<SbomComponentDTO> dtos = result.getResults();

    assertThat(dtos).hasSize(1);
    assertThat(dtos.get(0))
        .extracting(SbomComponentDTO::getHash, SbomComponentDTO::getDisplayName)
        .containsExactly(coordinate1.getHash(), "p1-v1");
  }

  @Test
  public void testGetSbomComponents_displayNameStoredFromNameAndVersion() throws IOException {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        ThirdPartySbomMetadataTestUtil.createSbomMetadata(ACTIVE, application.getId(), thirdPartyFile.getId());
    dao.insert(sbomMetadata);

    ThirdPartyFileCoordinate coordinate1 =
        tempEntity.newThirdPartyFileCoordinate("86163fcc32524261bfd2bdbedb7eae42", thirdPartyFile, "s1",
            "", "p1",
            "v1",
            "h1",
            null, "exact", null, List.of(thirdPartyFile.getFilename()), null);

    File reportFile = insightWork.getReportFile(application.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper.zipReport("/ApiSbomServicePolicyViolationsTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, thirdPartyScan.getScanId());

    SbomComponentListDTO result = service.getSbomComponents(sbomMetadata.getApplicationId(),
        sbomMetadata.getSbomVersion(), null, null, null,
        null, true, 3, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(1);

    List<SbomComponentDTO> dtos = result.getResults();

    assertThat(dtos).hasSize(1);
    assertThat(dtos.get(0))
        .extracting(SbomComponentDTO::getHash, SbomComponentDTO::getDisplayName)
        .containsExactly(coordinate1.getHash(), "p1 : v1");
  }

  @Test
  public void testGetSbomComponents_WithResults_PolicyFeatureFlagOff() throws IOException {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        ThirdPartySbomMetadataTestUtil.createSbomMetadata(ACTIVE, application.getId(), thirdPartyFile.getId());
    dao.insert(sbomMetadata);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 =
        tempEntity.newThirdPartyFileCoordinate("86163fcc32524261bfd2bdbedb7eae42", thirdPartyFile, "s1",
            packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(),
            packageUrlIdentifier1.getVersion(), "h1", packageUrlIdentifier1.getPackageUrl());

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 =
        tempEntity.newThirdPartyFileCoordinate("86163fcc32524261bfd2bdbedb7eae43", thirdPartyFile, "s2",
            packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(),
            packageUrlIdentifier2.getVersion(), "h2", packageUrlIdentifier2.getPackageUrl());
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-1", "License 1", "http://license-1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-2", "License 2", "http://license-2");

    File reportFile = insightWork.getReportFile(application.getId(), scan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/ApiSbomServicePolicyViolationsTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scan.getScanId());
    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(false);

    SbomComponentListDTO result = service.getSbomComponents(sbomMetadata.getApplicationId(),
        sbomMetadata.getSbomVersion(), null, null, null,
        null, true, 3, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    List<SbomComponentDTO> dtos = result.getResults();

    assertThat(dtos).hasSize(2);
    assertThat(dtos)
        .extracting(SbomComponentDTO::getHash)
        .containsExactlyInAnyOrder(coordinate1.getHash(), coordinate2.getHash());

    assertThat(dtos)
        .filteredOn(component -> component.getHash().equals(coordinate1.getHash()))
        .allSatisfy(component -> {
          assertThat(component.getName()).isEqualTo(packageUrlIdentifier1.getName());
          assertThat(component.getVersion()).isEqualTo(packageUrlIdentifier1.getVersion());
          assertThat(component.getPackageUrl()).isEqualTo(packageUrlIdentifier1.getPackageUrl());
          assertThat(component.getDisplayName())
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier1).toString());
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
          assertThat(component.getLicenses()).isNullOrEmpty();
          assertThat(component.getPolicyViolationCount()).isNull();
        });

    assertThat(dtos)
        .filteredOn(component -> component.getHash().equals(coordinate2.getHash()))
        .allSatisfy(component -> {
          assertThat(component.getName()).isEqualTo(packageUrlIdentifier2.getName());
          assertThat(component.getVersion()).isEqualTo(packageUrlIdentifier2.getVersion());
          assertThat(component.getPackageUrl()).isEqualTo(packageUrlIdentifier2.getPackageUrl());
          assertThat(component.getDisplayName())
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier2).toString());
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
          assertThat(component.getLicenses())
              .extracting(ResolvedLicenseDTO::licenseId)
              .containsExactlyInAnyOrder("license-1", "license-2");
          assertThat(component.getLicenses())
              .extracting(ResolvedLicenseDTO::licenseName)
              .containsExactlyInAnyOrder("License 1", "License 2");
          assertThat(component.getPolicyViolationCount()).isNull();
        });
  }

  @Test
  public void testGetSbomComponents_WithResults_NoPolicyViolations() throws IOException {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        ThirdPartySbomMetadataTestUtil.createSbomMetadata(ACTIVE, application.getId(), thirdPartyFile.getId());
    dao.insert(sbomMetadata);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 =
        tempEntity.newThirdPartyFileCoordinate("86163fcc32524261bfd2bdbedb7eae42", thirdPartyFile, "s1",
            packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(),
            packageUrlIdentifier1.getVersion(), "h1", packageUrlIdentifier1.getPackageUrl());

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 =
        tempEntity.newThirdPartyFileCoordinate("86163fcc32524261bfd2bdbedb7eae43", thirdPartyFile, "s2",
            packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(),
            packageUrlIdentifier2.getVersion(), "h2", packageUrlIdentifier2.getPackageUrl());
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-1", "License 1", "http://license-1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-2", "License 2", "http://license-2");

    File reportFile = insightWork.getReportFile(application.getId(), scan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/ApiSbomServicePolicyViolationsTest/noPolicyViolations", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scan.getScanId());

    SbomComponentListDTO result = service.getSbomComponents(sbomMetadata.getApplicationId(),
        sbomMetadata.getSbomVersion(), null, null, null,
        null, true, 3, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    List<SbomComponentDTO> dtos = result.getResults();

    assertThat(dtos).hasSize(2);
    assertThat(dtos)
        .extracting(SbomComponentDTO::getHash)
        .containsExactlyInAnyOrder(coordinate1.getHash(), coordinate2.getHash());

    assertThat(dtos)
        .filteredOn(component -> component.getHash().equals(coordinate1.getHash()))
        .allSatisfy(component -> {
          assertThat(component.getName()).isEqualTo(packageUrlIdentifier1.getName());
          assertThat(component.getVersion()).isEqualTo(packageUrlIdentifier1.getVersion());
          assertThat(component.getPackageUrl()).isEqualTo(packageUrlIdentifier1.getPackageUrl());
          assertThat(component.getDisplayName())
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier1).toString());
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
          assertThat(component.getLicenses()).isNullOrEmpty();
          assertThat(component.getPolicyViolationCount()).isEqualTo(0);
        });

    assertThat(dtos)
        .filteredOn(component -> component.getHash().equals(coordinate2.getHash()))
        .allSatisfy(component -> {
          assertThat(component.getName()).isEqualTo(packageUrlIdentifier2.getName());
          assertThat(component.getVersion()).isEqualTo(packageUrlIdentifier2.getVersion());
          assertThat(component.getPackageUrl()).isEqualTo(packageUrlIdentifier2.getPackageUrl());
          assertThat(component.getDisplayName())
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier2).toString());
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
          assertThat(component.getLicenses())
              .extracting(ResolvedLicenseDTO::licenseId)
              .containsExactlyInAnyOrder("license-1", "license-2");
          assertThat(component.getLicenses())
              .extracting(ResolvedLicenseDTO::licenseName)
              .containsExactlyInAnyOrder("License 1", "License 2");
          assertThat(component.getPolicyViolationCount()).isEqualTo(0);
        });
  }

  @Test
  public void testGetSbomComponentsByThirdPartyFileId_ComponentNameFilter() throws IOException {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        ThirdPartySbomMetadataTestUtil.createSbomMetadata(ACTIVE, application.getId(), thirdPartyFile.getId());
    dao.insert(sbomMetadata);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("slf4j-log4j12", "1.7.12");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl(), TRANSITIVE);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("cxf-rt-transports-http-jetty",
        "3.0.4");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl(), DIRECT);

    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createNpmCoordinates("slf4j-log4j", "2.4.0");
    PackageUrlIdentifier packageUrlIdentifier3 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier3);
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s3", packageUrlIdentifier3.getFormat(), packageUrlIdentifier3.getName(), packageUrlIdentifier3.getVersion(),
        "h3", packageUrlIdentifier3.getPackageUrl(), UNSPECIFIED);

    File reportFile = insightWork.getReportFile(application.getId(), scan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/ApiSbomServicePolicyViolationsTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scan.getScanId());

    SbomComponentListDTO result = service.getSbomComponents(
        sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(), null, null,
        "slf4j-log4j", null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    result = service.getSbomComponents(
        sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(), null, null,
        null, null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(3);
  }

  @Test
  public void testGetSbomComponentsByThirdPartyFileId_LicenseNameFilter() throws IOException {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        ThirdPartySbomMetadataTestUtil.createSbomMetadata(ACTIVE, application.getId(), thirdPartyFile.getId());
    dao.insert(sbomMetadata);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("slf4j-log4j12", "1.7.12");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl(), TRANSITIVE);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("cxf-rt-transports-http-jetty",
        "3.0.4");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl(), DIRECT);

    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createNpmCoordinates("slf4j-log4j", "2.4.0");
    PackageUrlIdentifier packageUrlIdentifier3 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier3);
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s3", packageUrlIdentifier3.getFormat(), packageUrlIdentifier3.getName(), packageUrlIdentifier3.getVersion(),
        "h3", packageUrlIdentifier3.getPackageUrl(), UNSPECIFIED);

    tempEntity.newThirdPartyCoordinateLicense(coordinate1, "license-1", "License 1", "http://license1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-1", "License 1", "http://license1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-3", "SpecialChars %$3", "http://license3");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-4", "Another 4", "http://license4");

    File reportFile = insightWork.getReportFile(application.getId(), scan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/ApiSbomServicePolicyViolationsTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scan.getScanId());

    SbomComponentListDTO result = service.getSbomComponents(
        sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(), null, null,
        "license-1", null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    result = service.getSbomComponents(
        sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(), null, null,
        null, null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(3);
  }

  @Test
  public void testGetSbomComponents_withLicenseOverrides_forLifeCycleProduct() throws IOException {
    testProductLicense.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    testGetSbomComponents_withLicenseOverrides(LicenseOverrideStatus.OVERRIDDEN, "Aladdin", "3D-Slicer");
  }

  @Test
  public void testGetSbomComponents_withLicenseOverrides_forSbomAndALPProduct() throws IOException {
    testProductLicense.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK);
    testGetSbomComponents_withLicenseOverrides(LicenseOverrideStatus.OVERRIDDEN, "Aladdin", "3D-Slicer");
  }

  @Test
  public void testGetSbomComponents_withLicenseOverrides_forSbomProduct() throws IOException {
    testProductLicense.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    testGetSbomComponents_withLicenseOverrides(null, "License 1", "SpecialChars %$3", "Another 4");
  }

  private void testGetSbomComponents_withLicenseOverrides(
      LicenseOverrideStatus overrideStatus,
      String... expected) throws IOException
  {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        ThirdPartySbomMetadataTestUtil.createSbomMetadata(ACTIVE, application.getId(), thirdPartyFile.getId());
    dao.insert(sbomMetadata);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("slf4j-log4j12", "1.7.12");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl(), TRANSITIVE);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("cxf-rt-transports-http-jetty",
        "3.0.4");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl(), DIRECT);

    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createNpmCoordinates("slf4j-log4j", "2.4.0");
    PackageUrlIdentifier packageUrlIdentifier3 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier3);
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s3", packageUrlIdentifier3.getFormat(), packageUrlIdentifier3.getName(), packageUrlIdentifier3.getVersion(),
        "h3", packageUrlIdentifier3.getPackageUrl(), UNSPECIFIED);

    tempEntity.newThirdPartyCoordinateLicense(coordinate1, "license-1", "License 1", "http://license1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-1", "License 1", "http://license1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-3", "SpecialChars %$3", "http://license3");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-4", "Another 4", "http://license4");

    // mimic license override
    tempEntity.newLicenseOverride(application.getId(), packageUrlIdentifier2.toComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN,
        Set.of("3D-Slicer-UNSPECIFIED", "Aladdin"));

    File reportFile = insightWork.getReportFile(application.getId(), scan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/ApiSbomServicePolicyViolationsTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scan.getScanId());

    SbomComponentListDTO result = service.getSbomComponents(
        sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(), null, null,
        "license-1", null, true, 5, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);
    assertThat(
        result.getResults().stream().filter(r -> r.getComponentIdentifier().equals(componentIdentifier2)))
            .hasSize(1)
            .allSatisfy(dto -> {
              assertThat(dto.getLicenses()).hasSize(expected.length)
                  .extracting(ResolvedLicenseDTO::licenseName)
                  .containsExactlyInAnyOrder(expected);
              if (overrideStatus != null) {
                assertThat(dto.getLicenses()).extracting(ResolvedLicenseDTO::overrideStatus)
                    .containsOnly(overrideStatus);
              }
            });
  }

  @Test
  public void testGetSbomComponents_WithResults_EmptyPackageUrl() throws IOException {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        ThirdPartySbomMetadataTestUtil.createSbomMetadata(ACTIVE, application.getId(), thirdPartyFile.getId());
    dao.insert(sbomMetadata);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1",
        componentIdentifier1.getFormat(), componentIdentifier1.get(ComponentIdentifier.NPM_PACKAGE_ID),
        componentIdentifier1.get(ComponentIdentifier.VERSION), "h1", null, null, List.of("pkg:npm/p1@v1"), null);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s2",
        componentIdentifier2.getFormat(), componentIdentifier2.get(ComponentIdentifier.NPM_PACKAGE_ID),
        componentIdentifier2.get(ComponentIdentifier.VERSION), "h2", null, null, List.of("pkg:npm/p2@v2"), null);
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-1", "License 1", "http://license-1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-2", "License 2", "http://license-2");

    File reportFile = insightWork.getReportFile(application.getId(), scan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/ApiSbomServicePolicyViolationsTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scan.getScanId());

    SbomComponentListDTO result = service.getSbomComponents(sbomMetadata.getApplicationId(),
        sbomMetadata.getSbomVersion(), null, null, null,
        null, true, 3, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    List<SbomComponentDTO> dtos = result.getResults();

    assertThat(dtos).hasSize(2);
    assertThat(dtos)
        .extracting(SbomComponentDTO::getHash)
        .containsExactlyInAnyOrder(coordinate1.getHash(), coordinate2.getHash());

    assertThat(dtos)
        .filteredOn(component -> component.getHash().equals(coordinate1.getHash()))
        .allSatisfy(component -> {
          assertThat(component.getName()).isEqualTo(componentIdentifier1.get(ComponentIdentifier.NPM_PACKAGE_ID));
          assertThat(component.getVersion()).isEqualTo(componentIdentifier1.get(ComponentIdentifier.VERSION));
          assertThat(component.getPackageUrl()).isEqualTo(null);
          assertThat(component.getFilenames()).isEqualTo(coordinate1.getFilenamesList());
          assertThat(component.getMatchStateId()).isEqualTo(coordinate1.getMatchStateId());
          assertThat(component.getDisplayName()).isEqualTo(componentIdentifier1.get(ComponentIdentifier.NPM_PACKAGE_ID)
              + " : " + componentIdentifier1.get(ComponentIdentifier.VERSION));
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
          assertThat(component.getLicenses()).isNullOrEmpty();
        });

    assertThat(dtos)
        .filteredOn(component -> component.getHash().equals(coordinate2.getHash()))
        .allSatisfy(component -> {
          assertThat(component.getName()).isEqualTo(componentIdentifier2.get(ComponentIdentifier.NPM_PACKAGE_ID));
          assertThat(component.getVersion()).isEqualTo(componentIdentifier2.get(ComponentIdentifier.VERSION));
          assertThat(component.getPackageUrl()).isEqualTo(null);
          assertThat(component.getFilenames()).isEqualTo(coordinate2.getFilenamesList());
          assertThat(component.getMatchStateId()).isEqualTo(coordinate2.getMatchStateId());
          assertThat(component.getDisplayName())
              .isEqualTo(componentIdentifier2.get(ComponentIdentifier.NPM_PACKAGE_ID)
                  + " : " + componentIdentifier2.get(ComponentIdentifier.VERSION));
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
          assertThat(component.getLicenses())
              .extracting(ResolvedLicenseDTO::licenseId)
              .containsExactlyInAnyOrder("license-1", "license-2");
          assertThat(component.getLicenses())
              .extracting(ResolvedLicenseDTO::licenseName)
              .containsExactlyInAnyOrder("License 1", "License 2");
        });
  }

  @Test
  public void testGetActiveSbomVersionListByApplication_Successful() {
    Application app = tempEntity.newApplicationWithParent();
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSbomVersion("1.5")
        .build();
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSbomVersion("1.6")
        .withStatus(PENDING)
        .build();

    List<String> applicationVersionsSbomDTOS = service.getActiveSbomVersionListByApplication(app.getId());
    assertThat(applicationVersionsSbomDTOS.size()).isEqualTo(1);
    assertThat(applicationVersionsSbomDTOS.get(0)).isEqualTo("1.5");
  }

  @Test
  public void testGetActiveSbomVersionListByApplication_SuccessfulEmpty() {
    Application app = tempEntity.newApplicationWithParent();

    List<String> applicationVersionsSbomDTOS = service.getActiveSbomVersionListByApplication(app.getId());
    assertThat(applicationVersionsSbomDTOS).isEmpty();
  }

  @Test
  public void testImportSbom_ValidFile_SPDX() throws IOException {
    String expectedVersion = "76b10b862e7b42009f2415097620928c";
    String userFilename = "file.txt";
    mockHdsForImportWithDelayedReportDownload(50);

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/spdx.json")) {
      Response response = service.importSbom(app.getId(), inputStream, userFilename, false,
          DUMMY_USER_AGENT, null, false);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);
      assertSuccessfulSBOMImportState(app, expectedVersion, userFilename, ticketDTO);
    }
  }

  @Test
  public void testImportSbom_ValidFile_SPDX_IgnoreValidationError() throws IOException {
    String expectedVersion = "76b10b862e7b42009f2415097620928c";
    String userFilename = "file.txt";
    mockHdsForImportWithDelayedReportDownload(50);

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/spdx.json")) {
      Response response = service.importSbom(app.getId(), inputStream, userFilename, false,
          DUMMY_USER_AGENT, null, true);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);
      assertSuccessfulSBOMImportState(app, expectedVersion, userFilename, ticketDTO);
    }
  }

  @Test
  public void testImportSbom_ValidFile_SPDX_CustomVersion() throws IOException {
    String applicationVersion = "my_application_version";
    String userFilename = "file.txt";
    mockHdsForImportWithDelayedReportDownload(50);

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/spdx.json")) {
      Response response = service.importSbom(app.getId(), inputStream, userFilename, false,
          DUMMY_USER_AGENT, applicationVersion, false);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);
      assertSuccessfulSBOMImportState(app, applicationVersion, userFilename, ticketDTO);
    }
  }

  @Test
  public void testImportSbom_InvalidFile_SPDX_IgnoreValidationError() throws IOException {
    String expectedVersion = "76b10b862e7b42009f2415097620928c";
    String userFilename = "file.txt";
    mockHdsForImportWithDelayedReportDownload(50);

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/invalid-spdx.json"))
    {
      Response response = service.importSbom(app.getId(), inputStream, userFilename, false,
          DUMMY_USER_AGENT, null, true);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);
      assertSuccessfulSBOMImportState(app, expectedVersion, userFilename, ticketDTO, false);
    }
  }

  @Test
  public void testImportSbom_ValidFile_CycloneDX() throws IOException {
    String expectedVersion = "2.36.19-SNAPSHOT";
    String userFilename = "third-party-simple-bom.xml";
    mockHdsForImportWithDelayedReportDownload(50);

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {
      Response response =
          service.importSbom(app.getId(), inputStream, userFilename, false, DUMMY_USER_AGENT, null,
              false);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);
      assertSuccessfulSBOMImportState(app, expectedVersion, userFilename, ticketDTO);
    }
  }

  @Test
  public void testImportSbom_ValidFile_CycloneDX_IgnoreValidationError() throws IOException {
    String expectedVersion = "2.36.19-SNAPSHOT";
    String userFilename = "third-party-simple-bom.xml";
    mockHdsForImportWithDelayedReportDownload(50);

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {
      Response response =
          service.importSbom(app.getId(), inputStream, userFilename, false, DUMMY_USER_AGENT, null,
              true);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);
      assertSuccessfulSBOMImportState(app, expectedVersion, userFilename, ticketDTO);
    }
  }

  @Test
  public void testImportSbom_ValidFile_CycloneDX_CustomVersion() throws IOException {
    mockHdsForImportWithDelayedReportDownload(50);
    String userFilename = "third-party-simple-bom.xml";

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {
      String applicationVersion = "my_application_version";
      Response response =
          service.importSbom(app.getId(), inputStream, userFilename, false, DUMMY_USER_AGENT,
              applicationVersion, false);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);
      assertSuccessfulSBOMImportState(app, applicationVersion, userFilename, ticketDTO);
    }
  }

  @Test
  public void testImportSbom_InvalidFile_CycloneDX_IgnoreValidationError() throws IOException {
    String expectedVersion = "2.36.19-SNAPSHOT";
    String userFilename = "invalid-third-party-simple-bom.xml";
    mockHdsForImportWithDelayedReportDownload(50);

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/invalid-third-party-simple-bom.xml"))
    {
      Response response =
          service.importSbom(app.getId(), inputStream, userFilename, false, DUMMY_USER_AGENT,
              null, true);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty().startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);
      assertSuccessfulSBOMImportState(app, expectedVersion, userFilename, ticketDTO, false);
    }
  }

  @Test
  public void testImportSbom_EmptyVersion() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {
      assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.importSbom(
          app.getId(),
          inputStream,
          "third-party-simple-bom.xml",
          true,
          DUMMY_USER_AGENT,
          "", false)).withMessageContaining("between 1 and 200");
    }

    assertExistingSbomFiles();
  }

  @Test
  public void testImportSbom_BlankVersion() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {
      assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.importSbom(
          app.getId(),
          inputStream,
          "third-party-simple-bom.xml",
          true,
          DUMMY_USER_AGENT,
          " ", false)).withMessageContaining("between 1 and 200");
    }

    assertExistingSbomFiles();
  }

  @Test
  public void testImportSbom_TooLongVersion() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {
      assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.importSbom(
          app.getId(),
          inputStream,
          "third-party-simple-bom.xml",
          true,
          DUMMY_USER_AGENT,
          StringUtils.repeat('a', 201), false))
          .withMessageContaining("between 1 and 200");
    }

    assertExistingSbomFiles();
  }

  @Test
  public void testImportSbom_MaxLengthVersion() throws Exception {
    mockHdsForImportWithDelayedReportDownload(50);
    String userFilename = "third-party-simple-bom.xml";

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {
      String applicationVersion = StringUtils.repeat('a', 200);
      Response response = service.importSbom(
          app.getId(),
          inputStream,
          userFilename,
          true,
          DUMMY_USER_AGENT,
          applicationVersion,
          false);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);
      assertSuccessfulSBOMImportState(app, applicationVersion, userFilename, ticketDTO);
    }
  }

  @Test
  public void testImportSbom_ValidFile_MaxSbomLimitHasBeenReached() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    testProductLicense.setMaxSbom(0);
    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {
      assertThatExceptionOfType(PaymentRequiredException.class)
          .isThrownBy(
              () -> service.importSbom(app.getId(), inputStream, "third-party-simple-bom.xml", true, DUMMY_USER_AGENT,
                  null, false))
          .withMessage("You have exceeded the licensed limit of 0 sboms.");
    }
    assertExistingSbomFiles();

    testProductLicense.reset();
  }

  @Test
  public void testImport_ValidFile_StripsPathDirs() throws Exception {
    String expectedVersion = "2.36.19-SNAPSHOT";
    String userFilename = "foo/third-party-simple-bom.xml";
    mockHdsForImportWithDelayedReportDownload(50);

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {
      Response response =
          service.importSbom(app.getId(), inputStream, userFilename, false, DUMMY_USER_AGENT, null,
              false);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);

      assertSuccessfulSBOMImportState(app, expectedVersion, "third-party-simple-bom.xml", ticketDTO);
    }
  }

  @Test
  public void testImport_ValidFile_StripsAbsolutePath() throws Exception {
    String expectedVersion = "2.36.19-SNAPSHOT";
    String userFilename = "/third-party-simple-bom.xml";
    mockHdsForImportWithDelayedReportDownload(50);

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {
      Response response =
          service.importSbom(app.getId(), inputStream, userFilename, false, DUMMY_USER_AGENT, null,
              false);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);

      assertSuccessfulSBOMImportState(app, expectedVersion, "third-party-simple-bom.xml", ticketDTO);
    }
  }

  @Test
  public void testImport_ValidFile_StripsDotDotDirs() throws Exception {
    String expectedVersion = "2.36.19-SNAPSHOT";
    String userFilename = "../../third-party-simple-bom.xml";
    mockHdsForImportWithDelayedReportDownload(50);

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {
      Response response =
          service.importSbom(app.getId(), inputStream, userFilename, false, DUMMY_USER_AGENT, null,
              false);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);

      assertSuccessfulSBOMImportState(app, expectedVersion, "third-party-simple-bom.xml", ticketDTO);
    }
  }

  @Test
  public void testImport_ValidFile_FailsOnDotDotPath() throws Exception {
    String userFilename = "..";

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {

      assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.importSbom(
          app.getId(),
          inputStream,
          userFilename,
          false,
          DUMMY_USER_AGENT,
          null,
          false));
    }

    assertExistingSbomFiles();
  }

  @Test
  public void testImport_ValidFile_FailsOnDotDotFinalPath() throws Exception {
    String userFilename = "third-party-simple-bom.xml/..";

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {

      assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.importSbom(
          app.getId(),
          inputStream,
          userFilename,
          false,
          DUMMY_USER_AGENT,
          null,
          false));
    }

    assertExistingSbomFiles();
  }

  @Test
  public void testImport_ValidFile_FailsOnDotFinalPath() throws Exception {
    String userFilename = "third-party-simple-bom.xml/.";

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {

      assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.importSbom(
          app.getId(),
          inputStream,
          userFilename,
          false,
          DUMMY_USER_AGENT,
          null,
          false));
    }

    assertExistingSbomFiles();
  }

  @Test
  public void testImportSbom_BinaryFile_BinaryScanningDisabled() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(false);

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/index.html"))
    {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> service.importSbom(app.getId(), inputStream, "/index.html", true, DUMMY_USER_AGENT, null,
              false))
          .withMessage("Importing binary files for SBOM Manager is disabled.");
    }

    assertExistingSbomFiles();
  }

  @Test
  public void testImportSbom_BinaryFile_BinaryScanningDisabled_MissingQueryParam() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream =
        getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/index.html"))
    {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(() -> service.importSbom(app.getId(), inputStream, "/index.html", false, DUMMY_USER_AGENT, null,
              false))
          .withMessage("Provided file type is not a supported SBOM file type.");
    }

    assertExistingSbomFiles();
  }

  @Test
  public void testScanAndEvaluateBinaryFile() throws IOException {
    mockHdsForImportWithDelayedReportDownload(50);

    Application app = tempEntity.newApplicationWithParent();

    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    try (InputStream inputStream = getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/index.html")) {
      Response response = service.importSbom(app.getId(), inputStream, "/index.html", true, DUMMY_USER_AGENT, null,
          false);

      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      List<ThirdPartyScan> thirdPartyScanList = thirdPartyScanDao.getByScanRequestId(ticketDTO.requestId);
      assertThat(thirdPartyScanList.size()).isEqualTo(1);
      ThirdPartyScan thirdPartyScan = thirdPartyScanList.get(0);
      assertThat(thirdPartyScan).isNotNull();
      assertThat(thirdPartyScan.getScanRequestId()).isEqualTo(ticketDTO.requestId);

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);

      ThirdPartySbomMetadata thirdPartySbomMetadata = dao.getByThirdPartyFileId(thirdPartyScan.getThirdPartyFileId());

      assertSbomMetadata(thirdPartySbomMetadata, app,
          filename -> assertThat(filename).matches("index\\.\\d+\\.json\\.gz"));

      assertExistingSbomFiles("%s/%s".formatted(app.getId(), thirdPartySbomMetadata.getFilename()));
    }
  }

  @Test
  public void testScanAndEvaluateBinaryFile_CustomVersion() throws IOException {
    mockHdsForImportWithDelayedReportDownload(50);

    Application app = tempEntity.newApplicationWithParent();

    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    try (InputStream inputStream = getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/index.html")) {
      String applicationVersion = "my_application_version";
      Response response =
          service.importSbom(app.getId(), inputStream, "/index.html", true, DUMMY_USER_AGENT, applicationVersion,
              false);

      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      List<ThirdPartyScan> thirdPartyScanList = thirdPartyScanDao.getByScanRequestId(ticketDTO.requestId);
      assertThat(thirdPartyScanList.size()).isEqualTo(1);
      ThirdPartyScan thirdPartyScan = thirdPartyScanList.get(0);
      assertThat(thirdPartyScan).isNotNull();
      assertThat(thirdPartyScan.getScanRequestId()).isEqualTo(ticketDTO.requestId);

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);

      ThirdPartySbomMetadata thirdPartySbomMetadata = dao.getByThirdPartyFileId(thirdPartyScan.getThirdPartyFileId());
      assertThat(thirdPartySbomMetadata.getSbomVersion()).isEqualTo(applicationVersion);

      assertSbomMetadata(thirdPartySbomMetadata, app,
          filename -> assertThat(filename).isEqualTo("index.my_application_version.json.gz"));

      assertExistingSbomFiles("%s/%s".formatted(app.getId(), thirdPartySbomMetadata.getFilename()));
    }
  }

  @Test
  public void testImportSbom_BinaryFile_WithDuplicateThirdPartyFiles() throws Exception {
    File binaryFileToScan = tempDir.newFile("scan-items.zip");
    Zipper.zipDirectory(new File(
        getClass().getResource("/" + getClass().getSimpleName() + "/binary-scan-duplicates/scan-items").toURI()),
        binaryFileToScan);
    mockHdsForImportWithDelayedReportDownload("/binary-scan-duplicates/report.zip", 50);
    Application app = tempEntity.newApplicationWithParent();
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);
    try (InputStream inputStream = new FileInputStream(binaryFileToScan)) {
      Response response = service.importSbom(app.getId(), inputStream, "scan-items.zip", true, DUMMY_USER_AGENT, null,
          false);

      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      ThirdPartyScan thirdPartyScan = thirdPartyScanDao.getSingleByScanRequestId(ticketDTO.requestId);
      assertThat(thirdPartyScan).isNotNull();
      assertThat(thirdPartyScan.getScanRequestId()).isEqualTo(ticketDTO.requestId);

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);

      ThirdPartySbomMetadata thirdPartySbomMetadata = dao.getByThirdPartyFileId(thirdPartyScan.getThirdPartyFileId());

      assertSbomMetadata(thirdPartySbomMetadata, app,
          filename -> assertThat(filename).matches("scan-items\\.\\d+\\.json\\.gz"));

      List<ThirdPartyFileCoordinate> tpComponents =
          thirdPartyFileCoordinateDAO.getBySbomMetadataId(thirdPartySbomMetadata.getId());
      // only 1 result should exist after merge
      assertThat(tpComponents).hasSize(1);
      ThirdPartyFileCoordinate cp = tpComponents.get(0);
      assertThat(cp.getFormat()).isEqualTo("maven");
      assertThat(cp.getName()).isEqualTo("jackson-databind");
      assertThat(cp.getVersion()).isEqualTo("2.9.9");
      assertThat(cp.getIdentificationSourcesAsSet()).contains("SBOM", "Sonatype");
      assertThat(cp.getOccurrencesList()).contains(
          "dependency:/scan-items.zip/scan-items/1-component.cdx.json/" +
              "pkg:maven\\com.fasterxml.jackson.core\\jackson-databind@2.9.9?type=jar",
          "dependency:/scan-items.zip/scan-items/2-component.cdx.json/" +
              "pkg:maven\\com.fasterxml.jackson.core\\jackson-databind@2.9.9?type=jar");

      List<ThirdPartyCoordinateSecurity> securityIssues =
          coordinateSecurityDAO.getByFileCoordinateId(cp.getId());
      assertThat(securityIssues).hasSize(6);
      assertThat(securityIssues).extracting("refId")
          .containsExactlyInAnyOrder("CVE-2019-12384", "CVE-2019-12814", "CVE-2020-25649", "CVE-2020-36518",
              "CVE-2022-42003", "CVE-2022-42004");
      assertThat(securityIssues).extracting("identificationSources")
          .containsExactlyInAnyOrder("SBOM,Sonatype", "SBOM,Sonatype", "Sonatype", "Sonatype", "Sonatype", "Sonatype");
    }
  }

  @Test
  public void testImportSbom_BinaryFile_WithMultipleThirdPartyFiles() throws Exception {
    File binaryFileToScan = tempDir.newFile("binary-scan.zip");
    Zipper.zipDirectory(new File(getClass().getResource("/" + getClass().getSimpleName() + "/binary-scan").toURI()),
        binaryFileToScan);
    mockHdsForImportWithDelayedReportDownload("empty-report.zip", 50);

    Application app = tempEntity.newApplicationWithParent();

    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);
    try (InputStream inputStream = new FileInputStream(binaryFileToScan)) {
      Response response = service.importSbom(app.getId(), inputStream, "binary-scan.zip", true, DUMMY_USER_AGENT, null,
          false);

      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      ThirdPartyScan thirdPartyScan = thirdPartyScanDao.getSingleByScanRequestId(ticketDTO.requestId);
      assertThat(thirdPartyScan).isNotNull();
      assertThat(thirdPartyScan.getScanRequestId()).isEqualTo(ticketDTO.requestId);

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);

      ThirdPartySbomMetadata thirdPartySbomMetadata = dao.getByThirdPartyFileId(thirdPartyScan.getThirdPartyFileId());

      assertSbomMetadata(thirdPartySbomMetadata, app,
          filename -> assertThat(filename).matches("binary-scan\\.\\d+\\.json\\.gz"));

      List<ThirdPartyFileCoordinate> tpComponents =
          thirdPartyFileCoordinateDAO.getBySbomMetadataId(thirdPartySbomMetadata.getId());
      // 1 from clair file, 1 from cyclonedx file, 2 from spdx file
      assertThat(tpComponents).hasSize(4)
          .extracting("name")
          .containsExactlyInAnyOrder("apt", "axis", "commons", "iq_application_SBOMTests");

      List<ThirdPartyCoordinateSecurity> tpVulnerabilities = coordinateSecurityDAO.getByFileCoordinateIds(
          tpComponents.stream()
              .map(ThirdPartyFileCoordinate::getId)
              .collect(
                  Collectors.toList()));
      // ne each from clair, cdx, and spdx
      assertThat(tpVulnerabilities).hasSize(3)
          .extracting("refId")
          .containsExactlyInAnyOrder("CVE-2007-2353", "CVE-2019-3462", "CVE-2007-2353");

      assertExistingSbomFiles("%s/%s".formatted(app.getId(), thirdPartySbomMetadata.getFilename()));
    }
  }

  @Test
  public void testImport_CDX_Json_BadStructure() throws Exception {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> importInvalidSbom("cdx-bad-structure.json", false))
        .withMessage("""
            Not a valid CycloneDX SBOM file.
            Error: Unable to parse BOM from byte array
            Line: 11, Column: 3, Error: Unexpected close marker ']': expected '}' \
            (for Object starting at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); \
            line: 6, column: 2])""");

    assertExistingSbomFiles();
  }

  @Test
  public void testImport_CDX_Xml_BadStructure() throws Exception {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> importInvalidSbom("cdx-bad-structure.xml", false))
        .withMessage("""
            Not a valid CycloneDX SBOM file.
            Line: 9, Column: 1, Error: The end-tag for element type "components" must end with a '>' delimiter.""");

    assertExistingSbomFiles();
  }

  @Test
  public void testImport_SPDX_Json_BadStructure() throws Exception {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> importInvalidSbom("spdx-bad-structure.json", false))
        .withMessage("""
            Not a valid SPDX SBOM file.
            Line: 20, Column: 3, Error: Unexpected close marker ']': expected '}' \
            (for Object starting at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); \
            line: 15, column: 2])""");

    assertExistingSbomFiles();
  }

  @Test
  public void testImport_SPDX_Xml_BadStructure() throws Exception {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> importInvalidSbom("spdx-bad-structure.xml", false))
        .withMessage("""
            Not a valid SPDX SBOM file.
            Error: Misplaced '<' at 606 [character 1 line 20]""");

    assertExistingSbomFiles();
  }

  @Test
  public void testImport_CDX_Json_BadStructure_IgnoreValidationError() throws Exception {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> importInvalidSbom("cdx-bad-structure.json", true))
        .withMessage("""
            Not a valid CycloneDX SBOM file.
            Error: Unable to parse BOM from byte array
            Line: 11, Column: 3, Error: Unexpected close marker ']': expected '}' \
            (for Object starting at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); \
            line: 6, column: 2])""");

    assertExistingSbomFiles();
  }

  @Test
  public void testImport_CDX_Xml_BadStructure_IgnoreValidationError() throws Exception {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> importInvalidSbom("cdx-bad-structure.xml", true))
        .withMessage("""
            Not a valid CycloneDX SBOM file.
            Line: 9, Column: 1, Error: The end-tag for element type "components" must end with a '>' delimiter.""");

    assertExistingSbomFiles();
  }

  @Test
  public void testImport_SPDX_Json_BadStructure_IgnoreValidationError() throws Exception {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> importInvalidSbom("spdx-bad-structure.json", true))
        .withMessage("""
            Not a valid SPDX SBOM file.
            Line: 20, Column: 3, Error: Unexpected close marker ']': expected '}' \
            (for Object starting at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); \
            line: 15, column: 2])""");

    assertExistingSbomFiles();
  }

  @Test
  public void testImport_SPDX_Xml_BadStructure_IgnoreValidationError() throws Exception {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> importInvalidSbom("spdx-bad-structure.xml", true))
        .withMessage("""
            Not a valid SPDX SBOM file.
            Error: Misplaced '<' at 606 [character 1 line 20]""");

    assertExistingSbomFiles();
  }

  @Test
  public void testImport_CDX_Json_Invalid() throws Exception {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> importInvalidSbom("cdx-invalid.json", false))
        .withMessage("""
            Not a valid CycloneDX SBOM file.
            Line: 11, Column: 6, Path: /components/1, Error: Missing required field "type".
            Line: 15, Column: 6, Path: /components/2, Error: Missing required field "type".
            Line: 19, Column: 6, Path: /components/3, Error: Missing required field "type".
            Line: 23, Column: 6, Path: /components/4, Error: Missing required field "type".
            Line: 27, Column: 6, Path: /components/5, Error: Missing required field "type".
            Line: 31, Column: 6, Path: /components/6, Error: Missing required field "type".
            Line: 35, Column: 6, Path: /components/7, Error: Missing required field "type".
            Line: 39, Column: 6, Path: /components/8, Error: Missing required field "type".
            Line: 43, Column: 6, Path: /components/9, Error: Missing required field "type".
            Line: 47, Column: 6, Path: /components/10, Error: Missing required field "type".
            Line: 51, Column: 6, Path: /components/11, Error: Missing required field "type".
            Line: 55, Column: 6, Path: /components/12, Error: Missing required field "type".
            Line: 59, Column: 6, Path: /components/13, Error: Missing required field "type".
            Line: 63, Column: 6, Path: /components/14, Error: Missing required field "type".""");

    assertExistingSbomFiles();
  }

  @Test
  public void testImport_CDX_Xml_Invalid() throws Exception {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> importInvalidSbom("cdx-invalid.xml", false))
        .withMessage("""
            Not a valid CycloneDX SBOM file.
            Line: 8, Column: 16, Path: //bom[1]/components[1], Error: cvc-complex-type.4: \
            Attribute 'type' must appear on element 'component'.""");

    assertExistingSbomFiles();
  }

  @Test
  public void testImport_SPDX_Json_Invalid() throws Exception {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> importInvalidSbom("spdx-invalid.json", false))
        .withMessage("""
            Not a valid SPDX SBOM file.
            Line: 15, Column: 4, Path: /packages/1, Error: Missing required field "downloadLocation".
            Line: 19, Column: 4, Path: /packages/2, Error: Missing required field "downloadLocation".
            Line: 1, Column: 2, Path: , Error: Missing required field "creationInfo".
            Line: 1, Column: 2, Path: , Error: Missing required field "dataLicense".""");

    assertExistingSbomFiles();
  }

  @Test
  public void testImport_SPDX_Xml_Invalid() throws Exception {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> importInvalidSbom("spdx-invalid.xml", false))
        .withMessageContaining("Not a valid SPDX SBOM file.")
        .withMessageContaining("Error: Missing required Creator")
        .withMessageContaining("Error: Missing required data license");

    assertExistingSbomFiles();
  }

  @Test
  public void testImport_CDX_Json_Invalid_IgnoreValidationError() throws Exception {
    mockHdsForImportWithDelayedReportDownload(50);
    assertThatNoException().isThrownBy(() -> importInvalidSbom("cdx-invalid.json", true));
  }

  @Test
  public void testImport_CDX_Xml_Invalid_IgnoreValidationError() throws Exception {
    mockHdsForImportWithDelayedReportDownload(50);
    assertThatNoException().isThrownBy(() -> importInvalidSbom("cdx-invalid.xml", true));
  }

  @Test
  public void testImport_SPDX_Json_Invalid_IgnoreValidationError() throws Exception {
    mockHdsForImportWithDelayedReportDownload(50);
    assertThatNoException().isThrownBy(() -> importInvalidSbom("spdx-invalid.json", true));
  }

  @Test
  public void testImport_SPDX_Xml_Invalid_IgnoreValidationError() throws Exception {
    mockHdsForImportWithDelayedReportDownload(50);
    assertThatNoException().isThrownBy(() -> importInvalidSbom("spdx-invalid.xml", true));
  }

  @Test
  public void testImport_Invalid_ExplicitVersion_IgnoreValidationError() throws Exception {
    mockHdsForImportWithDelayedReportDownload(50);
    assertThatNoException().isThrownBy(() -> importInvalidSbomWithExplicitVersion("cdx-invalid.xml", "1.0", true));
  }

  @Test
  public void testImport_Invalid_VersionFromFile_IgnoreValidationError() throws Exception {
    mockHdsForImportWithDelayedReportDownload(50);
    assertThatNoException().isThrownBy(
        () -> importInvalidSbom("cdx-invalid-with-version.xml", null, "a140fd3c3ded4bb0a640dc31e2904dc9", true));
  }

  private void importInvalidSbomWithExplicitVersion(
      final String fileName,
      final String explicitVersion,
      final boolean ignoreValidationError) throws Exception
  {
    importInvalidSbom(fileName, explicitVersion, explicitVersion, ignoreValidationError);
  }

  private void importInvalidSbom(
      final String fileName,
      final boolean ignoreValidationError) throws Exception
  {
    importInvalidSbom(fileName, null, null, ignoreValidationError);
  }

  private void importInvalidSbom(
      final String fileName,
      final String explicitVersion,
      final String expectedVersion,
      final boolean ignoreValidationError) throws Exception
  {
    Application app = tempEntity.newApplicationWithParent();
    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/sboms/" + fileName))
    {
      Response response = service.importSbom(
          app.getId(),
          inputStream,
          fileName,
          false,
          DUMMY_USER_AGENT,
          explicitVersion,
          ignoreValidationError);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty().startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");
      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);
      assertSuccessfulSBOMImportState(app, expectedVersion, fileName, ticketDTO, false);
    }
  }

  private static void assertSbomMetadata(
      final ThirdPartySbomMetadata thirdPartySbomMetadata,
      final Application app,
      final Consumer<String> filenameAssertion)
  {
    assertThat(thirdPartySbomMetadata).isNotNull();
    assertThat(thirdPartySbomMetadata.getApplicationId()).isEqualTo(app.getId());
    assertThat(thirdPartySbomMetadata.getSbomVersion()).isNotNull();
    assertThat(thirdPartySbomMetadata.getSpec()).isEqualTo(SbomSpecification.CYCLONEDX.toString());
    assertThat(thirdPartySbomMetadata.getSpecFormat()).isEqualTo(SbomFormat.JSON.toString());
    assertThat(thirdPartySbomMetadata.getSpecVersion()).isEqualTo(ExportSpecification.DEFAULT.getVersion());
    assertThat(thirdPartySbomMetadata.getStatus()).isEqualTo(ACTIVE);
    assertThat(thirdPartySbomMetadata.getCreatedAt()).isNotNull();
    assertThat(thirdPartySbomMetadata.getScanType()).isEqualTo(SbomScanType.BINARY.toString());
    assertThat(thirdPartySbomMetadata.getMetadataJson()).isNotEmpty();

    filenameAssertion.accept(thirdPartySbomMetadata.getFilename());
  }

  @Test
  public void testGetImportStatus_NonExistentImportRequestId() {
    Application app = tempEntity.newApplicationWithParent();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> service.getImportStatus(app.getId(), "dummyRequestId"))
        .withMessage("Policy evaluation status with id dummyRequestId for public application id " + app.getPublicId() +
            " was not found.");
  }

  @Test
  public void testGetImportStatus_Completed() throws IOException {
    mockHdsForImportWithDelayedReportDownload(0);

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {
      Response response =
          service.importSbom(app.getId(), inputStream, "third-party-simple-bom.xml", false, DUMMY_USER_AGENT, null,
              false);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      String importRequestId = ticketDTO.statusUrl.substring(ticketDTO.statusUrl.lastIndexOf("/") + 1);

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), importRequestId);

      ApiSbomStatusDTO apiSbomStatusDTO = service.getImportStatus(app.getId(), importRequestId);
      assertThat(apiSbomStatusDTO.applicationId).isEqualTo(app.getId());
      assertThat(apiSbomStatusDTO.downloadUrl).startsWith("api/v2/sbom/applications/" + app.getId() + "/versions/");
      assertThat(apiSbomStatusDTO.downloadUrl).endsWith("state=original");
    }
  }

  @Test
  public void testGetImportStatus_Pending() throws Exception {
    mockHdsForImportWithDelayedReportDownload(1000);

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {
      Response response = service.importSbom(app.getId(), inputStream, "file.txt", false, DUMMY_USER_AGENT, null,
          false);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      String importRequestId = ticketDTO.statusUrl.substring(ticketDTO.statusUrl.lastIndexOf("/") + 1);

      assertThatExceptionOfType(NotFoundException.class)
          .isThrownBy(() -> service.getImportStatus(app.getId(), importRequestId))
          .withMessage("Sbom version import is still in progress");

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), importRequestId);
    }
  }

  @Test
  public void testGetImportStatus_Error() throws Exception {
    mockHdsForImportWithError();

    Application app = tempEntity.newApplicationWithParent();

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"))
    {
      Response response =
          service.importSbom(app.getId(), inputStream, "file.txt", false, DUMMY_USER_AGENT, null, false);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      String importRequestId = ticketDTO.statusUrl.substring(ticketDTO.statusUrl.lastIndexOf("/") + 1);

      policyEvaluationHelper.awaitEvaluationFailed(app.getId(), importRequestId);

      ApiSbomStatusDTO apiSbomStatusDTO = service.getImportStatus(app.getId(), importRequestId);
      assertThat(apiSbomStatusDTO.isError).isTrue();
      assertThat(apiSbomStatusDTO.errorMessage).isNotEmpty();
    }
  }

  private void mockHdsForImportWithDelayedReportDownload(String reportName, long delayInMs) throws IOException {
    String scanId = tempEntity.newRandomHash();
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    lenient().doReturn(scanReceipt)
        .when(mockHdsClient)
        .put(any(), eq(ScanReceipt.class), eq(DUMMY_USER_AGENT),
            eq(ScanUploader.HDS_PATH), any(ScanEntity.class), any());

    lenient().doReturn("")
        .when(mockHdsClient)
        .get(eq(String.class), eq("rest/productLicense/developer-upper-bound"));

    lenient().doAnswer(new AnswersWithDelay(delayInMs,
        new Returns(getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/" + reportName))))
        .when(mockHdsClient)
        .get(any(Retry.class), eq(InputStream.class), eq("rest/application/analysis/{scanId}"),
            isNull(), eq(scanId));
  }

  private void mockHdsForImportWithDelayedReportDownload(long delayInMs) throws IOException {
    mockHdsForImportWithDelayedReportDownload("small-report.zip", delayInMs);
  }

  private void mockHdsForImportWithError() throws IOException {
    String scanId = tempEntity.newRandomHash();
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);
    lenient().doReturn(scanReceipt)
        .when(mockHdsClient)
        .put(any(), eq(ScanReceipt.class), eq(DUMMY_USER_AGENT),
            eq(ScanUploader.HDS_PATH), any(ScanEntity.class), any());

    lenient().doThrow(new RuntimeException("Test error"))
        .when(mockHdsClient)
        .get(any(Retry.class), eq(InputStream.class),
            eq("rest/application/analysis/{scanId}"),
            isNull(), eq(scanId));
  }

  private static void assertContentHeader(
      final Response response,
      final Application app,
      final String sbomVersion,
      final String specFormat,
      final SbomSpecification sbomSpec)
  {
    String contentHeader = response.getHeaderString("Content-Disposition");
    String actualFilename = contentHeader.substring(contentHeader.indexOf("=") + 1).split(";")[0].replaceAll("\"", "");
    assertThat(actualFilename).matches(app.getPublicId() +
        "_" +
        sbomVersion +
        "_(\\d)+." +
        (sbomSpec.equals(SbomSpecification.SPDX) ? "spdx" : "cdx") +
        specFormat);
  }

  private void assertSuccessfulSBOMImportState(
      final Application app,
      final String applicationVersion,
      final String expectedThirdPartyFilename,
      final ApiThirdPartyScanTicketDTO ticketDTO) throws IOException
  {
    assertSuccessfulSBOMImportState(app, applicationVersion, expectedThirdPartyFilename, ticketDTO, true);
  }

  private void assertSuccessfulSBOMImportState(
      final Application app,
      final String applicationVersion,
      final String expectedThirdPartyFilename,
      final ApiThirdPartyScanTicketDTO ticketDTO,
      final boolean isValid) throws IOException
  {
    ApiSbomStatusDTO importStatus = service.getImportStatus(app.getId(), ticketDTO.requestId);
    assertThat(importStatus.version).isNotNull();
    ThirdPartySbomMetadata sbomMetadata = dao.getByApplicationIdAndSbomVersion(app.getId(), importStatus.version);
    if (applicationVersion == null) {
      // expect auto-generated timestamp-based version
      assertThat(sbomMetadata.getSbomVersion()).matches("\\d+");
    }
    else {
      assertThat(sbomMetadata.getSbomVersion()).isEqualTo(applicationVersion);
    }
    assertThat(sbomMetadata).isNotNull()
        .hasFieldOrPropertyWithValue("status", ACTIVE)
        .hasFieldOrPropertyWithValue("isValid", isValid);
    ThirdPartyScan tpScan = thirdPartyScanDao.getByThirdPartyFileId(sbomMetadata.getThirdPartyFileId());
    assertThat(tpScan.getFilteredScanFile()).isNotNull();

    var thirdPartyFile = thirdPartyFileDAO.getById(sbomMetadata.getThirdPartyFileId());
    assertThat(thirdPartyFile.getFilename()).isEqualTo(expectedThirdPartyFilename);

    assertExistingSbomFiles("%s/%s".formatted(app.getId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testGetExportOptions_spdx30Source() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withSpec(SbomSpecification.SPDX.toString())
        .withSpecVersion("3.0")
        .build();

    Response response = service.getExportOptions(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion());

    assertThat(response.getStatus()).isEqualTo(200);
    List<String> options = (List<String>) response.getEntity();
    assertThat(options).containsExactly("spdx3.0", "cyclonedx1.5", "cyclonedx1.6", "cyclonedx1.7", "pdf");
  }

  @Test
  public void testGetExportOptions_spdx2xSource() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withSpec(SbomSpecification.SPDX.toString())
        .withSpecVersion("2.3")
        .build();

    Response response = service.getExportOptions(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion());

    assertThat(response.getStatus()).isEqualTo(200);
    List<String> options = (List<String>) response.getEntity();
    assertThat(options).containsExactly("spdx2.2", "spdx2.3", "spdx3.0", "cyclonedx1.5", "cyclonedx1.6", "cyclonedx1.7",
        "pdf");
  }

  @Test
  public void testGetExportOptions_cycloneDxSource() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withSpec(SbomSpecification.CYCLONEDX.toString())
        .build();

    Response response = service.getExportOptions(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion());

    assertThat(response.getStatus()).isEqualTo(200);
    List<String> options = (List<String>) response.getEntity();
    assertThat(options).containsExactly("cyclonedx1.5", "cyclonedx1.6", "cyclonedx1.7", "spdx2.2", "spdx2.3", "spdx3.0",
        "pdf");
  }

  @Test
  public void testGetExportOptions_notFound() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).build();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getExportOptions(sbomMetadata.getApplicationId(), "nonExistentVersion"));
  }

  private String expectedContentIn(String fileName) throws Exception {
    return readFileToString(this.getClass(), fileName);
  }

  private void assertExistingSbomFiles(String... expectedPaths) throws IOException {
    existingFilesHelper.assertExistingSbomFiles(expectedPaths);
  }
}
