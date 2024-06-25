/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.PolicyEvaluationHelper;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataSummaryDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataSummaryListDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataTestUtil;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentListDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.SbomStatus;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;
import org.xmlunit.assertj.XmlAssert;

import static com.sonatype.insight.brain.api.v2.service.ApiSbomService.SBOM_VALIDATED_HEADER;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.CYCLONEDX_IGNORE_ATTRIBS;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.cycloneDxIgnoreNodesFilter;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.readFileToString;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.setupScenarioWithMetadataComponentSecurityLicenseAndVex;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.spdxDxIgnoreNodesFilter;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

public class ApiSbomServiceTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(1, ApiSbomServiceTest.class, ApiSbomService.class);

  private static final String DUMMY_USER_AGENT = RandomStringUtils.random(10, true, true);

  @Inject
  private ApiSbomService service;

  @Inject
  private ThirdPartySbomMetadataDAO dao;

  @Inject
  private InsightWork insightWork;

  @Inject
  private PolicyEvaluationHelper policyEvaluationHelper;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private HdsClient mockHdsClient;

  @Mock
  private BaseUrl mockBaseUrl;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    binder.bind(BaseUrl.class).toInstance(mockBaseUrl);
    super.configure(binder);
  }

  @Before
  public void before() {
    lenient().when(mockBaseUrl.get()).thenReturn("http://localhost:8070/");
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
        .withStatus(SbomStatus.ACTIVE.name())
        .build();

    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withStatus(SbomStatus.PENDING.name())
        .build();

    Response response = service.getSbomVersion(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(),
        ApiSbomService.SBOM_STATE_ORIGINAL, "cyclonedx1.5", "application/xml");
    String actualContent = new String((byte[]) response.getEntity());
    XmlAssert.assertThat(actualContent).and(expectedContentIn("third-party-simple-bom.xml"))
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
        .withStatus(SbomStatus.ACTIVE.name())
        .build();

    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withStatus(SbomStatus.PENDING.name())
        .build();

    Response response = service.getSbomVersion(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(),
        ApiSbomService.SBOM_STATE_ORIGINAL, "", "");
    String actualContent = new String((byte[]) response.getEntity());
    assertThatJson(actualContent).isEqualTo(expectedContentIn("spdx.json"));
  }

  @Test
  public void testGetSbomVersion_Current_CycloneDxToCycloneDx_Xml() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path zippedBom = mockOriginalSbom(this.getClass(), "sboms/valid-cyclonedx-bom.xml",
        insightWork.getSbomDir(app.getId()).toPath());

    String sbomVersion = tempEntity.newRandomHash();
    setupScenarioWithMetadataComponentSecurityLicenseAndVex(tempEntity, app, zippedBom, sbomVersion,
        "CycloneDx", "1.5", SbomFormat.XML);

    Response response =
        service.getSbomVersion(app.getId(), sbomVersion, ApiSbomService.SBOM_STATE_CURRENT,
            "cyclonedx1.5", MediaType.APPLICATION_XML);

    assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    assertThat(response.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_XML);
    assertThat(response.getHeaderString(SBOM_VALIDATED_HEADER)).isEqualTo("true");

    assertContentHeader(response, app, sbomVersion, ".xml");
    String sbomContent = new String((byte []) response.getEntity());
    XmlAssert.assertThat(sbomContent).and(expectedContentIn("sboms/valid-cyclonedx-result-bom.xml"))
        .withNodeFilter(cycloneDxIgnoreNodesFilter())
        .withAttributeFilter(attr -> !CYCLONEDX_IGNORE_ATTRIBS.contains(attr.getName()))
        .ignoreWhitespace()
        .areIdentical();
  }

  @Test
  public void testGetSbomVersion_Current_CycloneDxToCycloneDx_Json() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path zippedBom = mockOriginalSbom(this.getClass(), "sboms/valid-cyclonedx-bom.xml",
        insightWork.getSbomDir(app.getId()).toPath());

    String sbomVersion = tempEntity.newRandomHash();
    setupScenarioWithMetadataComponentSecurityLicenseAndVex(tempEntity, app, zippedBom, sbomVersion,
        "CycloneDx", "1.5", SbomFormat.XML);

    Response response =
        service.getSbomVersion(app.getId(), sbomVersion, ApiSbomService.SBOM_STATE_CURRENT,
            "cyclonedx1.5", MediaType.APPLICATION_JSON);

    assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    assertThat(response.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(response.getHeaderString(SBOM_VALIDATED_HEADER)).isEqualTo("true");

    String sbomContent = new String((byte []) response.getEntity());
    assertContentHeader(response, app, sbomVersion, ".json");
    assertThatJson(sbomContent)
        .isEqualTo(expectedContentIn("sboms/valid-cyclonedx-result-bom.json"));
  }

  @Test
  public void testGetSbomVersion_Current_SpdxToSpdx_Xml() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path zippedBom = mockOriginalSbom(this.getClass(), "sboms/valid-spdx-bom.xml",
        insightWork.getSbomDir(app.getId()).toPath());
    String sbomVersion = tempEntity.newRandomHash();
    setupScenarioWithMetadataComponentSecurityLicenseAndVex(tempEntity, app, zippedBom, sbomVersion,
        "SPDX", "2.3", SbomFormat.XML);

    Response response =
        service.getSbomVersion(app.getId(), sbomVersion, ApiSbomService.SBOM_STATE_CURRENT,
            "spdx2.3", MediaType.APPLICATION_XML);

    assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    assertThat(response.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_XML);
    assertThat(response.getHeaderString(SBOM_VALIDATED_HEADER)).isEqualTo("true");

    String sbomContent = new String((byte[]) response.getEntity());
    assertContentHeader(response, app, sbomVersion, ".xml");
    XmlAssert.assertThat(sbomContent).and(expectedContentIn("sboms/valid-spdx-result-bom.xml"))
        .withNodeFilter(spdxDxIgnoreNodesFilter())
        .ignoreWhitespace()
        .areIdentical();
  }

  @Test
  public void testGetSbomVersion_Current_SpdxToSpdx_Json() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path zippedBom = mockOriginalSbom(this.getClass(), "sboms/valid-spdx-bom.xml",
        insightWork.getSbomDir(app.getId()).toPath());
    String sbomVersion = tempEntity.newRandomHash();
    setupScenarioWithMetadataComponentSecurityLicenseAndVex(tempEntity, app, zippedBom, sbomVersion,
        "SPDX", "2.3", SbomFormat.XML);

    Response response =
        service.getSbomVersion(app.getId(), sbomVersion, ApiSbomService.SBOM_STATE_CURRENT,
            "spdx2.3", MediaType.APPLICATION_JSON);

    assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    assertThat(response.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(response.getHeaderString(SBOM_VALIDATED_HEADER)).isEqualTo("true");

    String sbomContent = new String((byte []) response.getEntity());
    assertContentHeader(response, app, sbomVersion, ".json");
    assertThatJson(sbomContent)
        .whenIgnoringPaths("creationInfo.created", "creationInfo.creators[0]", "documentNamespace", "name")
        .isEqualTo(expectedContentIn("sboms/valid-spdx-result-bom.json"));
  }

  @Test
  public void testValidateAndLogAnyErrors_Invalid() throws Exception {
    String content = "<?xml version=\"1.0\"?>\n" +
        "<bom serialNumber=\"urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79\" version=\"1\"\n" +
        "     xmlns=\"http://cyclonedx.org/schema/bom/1.1\"\n" +
        "     xmlns:v=\"http://cyclonedx.org/schema/ext/vulnerability/1.0\">\n" +
        "  <components>\n" +
        "    <component type=\"library\" bom-ref=\"pkg:maven/com.jackson.core/jackson-databind@2.9.9?type=jar\">\n" +
        "      <group>com.fasterxml.jackson.core</group>\n" +
        "      <name>jackson-databind</name>\n" +
        "      <version>2.9.9</version>\n" +
        "      <licenses>\n" +
        "        <license>\n" +
        "          <id></id>\n" +
        "        </license>\n" +
        "      </licenses>\n" +
        "      <purl>pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar</purl>\n" +
        "    </component>\n" +
        "  </components>\n" +
        "</bom>";
    boolean validated =
        service.validateAndLogAnyErrors(content, "appId", "v1", ExportSpecification.CYCLONEDX_15, SbomFormat.XML);
    assertThat(validated).isFalse();
    logOutput.assertThat()
        .contains("Invalid SBOM generated for application appId, version v1, spec CycloneDx, format xml")
        .atDebugLevel();
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
  public void testGetSbomVersion_NoActiveSboms() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus(SbomStatus.PENDING.name())
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
  @PostgresTest
  public void testGetSbomMetadataSummaryForApplication_Successful() {
    Application application = tempEntity.newApplicationWithParent();

    ThirdPartyFile file1 = tempEntity.newThirdPartyFile("CycloneDX-bom.xml");
    ThirdPartyFile file2 = tempEntity.newThirdPartyFile("SPDX-spdx.json");

    tempEntity.newThirdPartySbomMetadata(file1.getId(), application.getId(), "ACTIVE", file1.getFilename());
    tempEntity.newThirdPartySbomMetadata(file2.getId(), application.getId(), "ACTIVE", file2.getFilename());

    ThirdPartyFileCoordinate c1 = tempEntity.newThirdPartyFileCoordinate(file1, "s1", "f1", "n1", "v1");
    ThirdPartyFileCoordinate c2 = tempEntity.newThirdPartyFileCoordinate(file2, "s2", "f2", "n2", "v2");

    tempEntity.newThirdPartyCoordinateSecurity(c1, "r1", "d1", "l1", 3.5F, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(c1, "r2", "d2", "l2", 7.5F, "sd2", "f2");
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r3", "d3", "l3", 1.5F, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r4", "d4", "l4", 0.5F, "sd4", "f4");

    ThirdPartySbomMetadataSummaryListDTO resultList =
        service.getSbomMetadataSummaryForApplication(application.getId(), "asc", 5, 1);
    assertThat(resultList).isNotNull();
    assertThat(resultList.getTotalResultsCount()).isEqualTo(2);

    List<ThirdPartySbomMetadataSummaryDTO> results = resultList.getResults();
    assertThat(results).hasSize(2);

    Collections.sort(results, Comparator.comparingInt(ThirdPartySbomMetadataSummaryDTO::getHigh));

    assertThat(results).hasSize(2);
    assertThat(results.get(0).getLow()).isEqualTo(2);
    assertThat(results.get(1).getLow()).isEqualTo(1);
    assertThat(results.get(1).getHigh()).isEqualTo(1);
  }

  @Test
  public void testGetSbomComponents_InvalidPagination() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getSbomComponents("appId", "version", null, null, null, true, -1, 2))
        .withMessage("pageSize must not be less than one!");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.getSbomComponents("appId", "version", null, null, null, true, 1, -2))
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
            null, null, true, 3, 1))
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
            () -> service.getSbomComponents(sbomMetadata.getApplicationId(), fakeVersion, null, null, null, true, 3, 1))
        .withMessage(
            "Cannot find version " + fakeVersion + " for application with ID " + sbomMetadata.getApplicationId() + ".");
  }

  @Test
  @PostgresTest
  public void testGetSbomComponents_NoResults() {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    SbomComponentListDTO result = service.getSbomComponents(sbomMetadata.getApplicationId(),
        sbomMetadata.getSbomVersion(), null, null, null, true, 3, 1);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isZero();
    assertThat(result.getResults()).isEmpty();
  }

  @Test
  @PostgresTest
  public void testGetSbomComponents_WithResults() {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata =
        ThirdPartySbomMetadataTestUtil.createSbomMetadata("ACTIVE", application.getId(), thirdPartyFile.getId());
    dao.insert(sbomMetadata);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1",
        packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(), "h1",
        packageUrlIdentifier1.getPackageUrl());

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s2",
        packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(), "h2",
        packageUrlIdentifier2.getPackageUrl());
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-1", "License 1", "http://license-1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-2", "License 2", "http://license-2");

    SbomComponentListDTO result = service.getSbomComponents(sbomMetadata.getApplicationId(),
        sbomMetadata.getSbomVersion(), null, null, null, true, 3, 1);

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
              .extracting(License::getLicenseId).containsExactlyInAnyOrder("license-1", "license-2");
          assertThat(component.getLicenses())
              .extracting(License::getLicenseName).containsExactlyInAnyOrder("License 1", "License 2");
        });
  }

  @Test
  @PostgresTest
  public void testGetSbomComponents_WithResults_EmptyPackageUrl() {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata =
        ThirdPartySbomMetadataTestUtil.createSbomMetadata("ACTIVE", application.getId(), thirdPartyFile.getId());
    dao.insert(sbomMetadata);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1",
        componentIdentifier1.getFormat(), componentIdentifier1.get(ComponentIdentifier.NPM_PACKAGE_ID),
        componentIdentifier1.get(ComponentIdentifier.VERSION), "h1", null);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s2",
        componentIdentifier2.getFormat(), componentIdentifier2.get(ComponentIdentifier.NPM_PACKAGE_ID),
        componentIdentifier2.get(ComponentIdentifier.VERSION), "h2", null);
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-1", "License 1", "http://license-1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-2", "License 2", "http://license-2");

    SbomComponentListDTO result = service.getSbomComponents(sbomMetadata.getApplicationId(),
        sbomMetadata.getSbomVersion(), null, null, null, true, 3, 1);

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
          assertThat(component.getDisplayName()).isEqualTo(componentIdentifier1.get(ComponentIdentifier.NPM_PACKAGE_ID)
              + ":" + componentIdentifier1.get(ComponentIdentifier.VERSION));
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
          assertThat(component.getDisplayName())
              .isEqualTo(componentIdentifier2.get(ComponentIdentifier.NPM_PACKAGE_ID)
                  + ":" + componentIdentifier2.get(ComponentIdentifier.VERSION));
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
          assertThat(component.getLicenses())
              .extracting(License::getLicenseId).containsExactlyInAnyOrder("license-1", "license-2");
          assertThat(component.getLicenses())
              .extracting(License::getLicenseName).containsExactlyInAnyOrder("License 1", "License 2");
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
        .withStatus(SbomStatus.PENDING.name())
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
  public void testImportSbom_InvalidFile() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    Files.createDirectories(insightWork.getSbomDir(app.getId()).toPath());

    try (InputStream inputStream = getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/index.html")) {
      assertThatExceptionOfType(BadRequestException.class)
          .isThrownBy(
              () -> service.importSbom(app.getId(), inputStream, DUMMY_USER_AGENT))
          .withMessage("provided file type is not a supported SBOM file type");
    }
  }

  @Test
  public void testImportSbom_ValidFile_SPDX() throws IOException {
    mockHdsForImportWithDelayedReportDownload(50);

    Application app = tempEntity.newApplicationWithParent();
    Files.createDirectories(insightWork.getSbomDir(app.getId()).toPath());

    try (InputStream inputStream = getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/spdx.json")) {
      Response response = service.importSbom(app.getId(), inputStream,
          DUMMY_USER_AGENT);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);
    }
  }

  @Test
  public void testImportSbom_ValidFile_CycloneDX() throws IOException {
    mockHdsForImportWithDelayedReportDownload(50);

    Application app = tempEntity.newApplicationWithParent();
    Files.createDirectories(insightWork.getSbomDir(app.getId()).toPath());

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml")) {
      Response response = service.importSbom(app.getId(), inputStream, DUMMY_USER_AGENT);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      assertThat(ticketDTO).isNotNull();
      assertThat(ticketDTO.statusUrl).isNotEmpty()
          .startsWith("api/v2/sbom/applications/" + app.getId() + "/status/");

      policyEvaluationHelper.awaitEvaluationCompleted(app.getId(), ticketDTO.requestId);
    }
  }

  @Test
  public void testImportSbom_ValidFile_MaxSbomLimitHasBeenReached() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    Files.createDirectories(insightWork.getSbomDir(app.getId()).toPath());
    testProductLicense.setMaxSbom(0);
    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml")) {
      assertThatExceptionOfType(PaymentRequiredException.class)
          .isThrownBy(
              () -> service.importSbom(app.getId(), inputStream, DUMMY_USER_AGENT))
          .withMessage("You have exceeded the licensed limit of 0 sboms.");
    }
    testProductLicense.reset();
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
    Files.createDirectories(insightWork.getSbomDir(app.getId()).toPath());

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml")) {
      Response response = service.importSbom(app.getId(), inputStream, DUMMY_USER_AGENT);
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
    Files.createDirectories(insightWork.getSbomDir(app.getId()).toPath());

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml")) {
      Response response = service.importSbom(app.getId(), inputStream, DUMMY_USER_AGENT);
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
    Files.createDirectories(insightWork.getSbomDir(app.getId()).toPath());

    try (InputStream inputStream = getClass().getResourceAsStream(
        "/" + getClass().getSimpleName() + "/third-party-simple-bom.xml")) {
      Response response = service.importSbom(app.getId(), inputStream, DUMMY_USER_AGENT);
      ApiThirdPartyScanTicketDTO ticketDTO = (ApiThirdPartyScanTicketDTO) response.getEntity();
      String importRequestId = ticketDTO.statusUrl.substring(ticketDTO.statusUrl.lastIndexOf("/") + 1);

      policyEvaluationHelper.awaitEvaluationFailed(app.getId(), importRequestId);

      ApiSbomStatusDTO apiSbomStatusDTO = service.getImportStatus(app.getId(), importRequestId);
      assertThat(apiSbomStatusDTO.isError).isTrue();
      assertThat(apiSbomStatusDTO.errorMessage).isNotEmpty();
    }
  }

  private void mockHdsForImportWithDelayedReportDownload(long delayInMs) throws IOException {
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("SCAN-ID");
    doReturn(scanReceipt).when(mockHdsClient).put(any(), eq(ScanReceipt.class), eq(DUMMY_USER_AGENT),
        eq(ScanUploader.HDS_PATH), any(File.class), any());

    doAnswer(new AnswersWithDelay(delayInMs,
        new Returns(getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/small-report.zip"))))
        .when(mockHdsClient).get(any(Retry.class), eq(InputStream.class), eq("rest/application/analysis/{scanId}"),
            isNull(), eq("SCAN-ID"));
  }

  private void mockHdsForImportWithError() throws IOException {
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("SCAN-ID");
    doReturn(scanReceipt).when(mockHdsClient).put(any(), eq(ScanReceipt.class), eq(DUMMY_USER_AGENT),
        eq(ScanUploader.HDS_PATH), any(File.class), any());

    doThrow(new RuntimeException("Test error")).when(mockHdsClient).get(any(Retry.class), eq(InputStream.class),
        eq("rest/application/analysis/{scanId}"),
        isNull(), eq("SCAN-ID"));
  }

  private static void assertContentHeader(
      final Response response,
      final Application app,
      final String sbomVersion,
      final String x)
  {
    String contentHeader = response.getHeaderString("Content-Disposition");
    String actualFilename = contentHeader.substring(contentHeader.indexOf("=") + 1).split(";")[0].replaceAll("\"", "");
    assertThat(actualFilename).isEqualTo(app.getName() + "_" + sbomVersion + x);
  }

  private String expectedContentIn(String fileName) throws Exception {
    return readFileToString(this.getClass(), fileName);
  }
}
