/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.File;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomVulnerabilityAnalysisRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomVulnerabilityAnalysisRequestDTO.ComponentLocator;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomVulnerabilityAnalysisRequestDTO.VulnerabilityAnalysis;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomVulnerabilityAnalysisRequestDTO.VulnerabilityAnalysis.Justification;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomVulnerabilityAnalysisRequestDTO.VulnerabilityAnalysis.Response;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomVulnerabilityAnalysisRequestDTO.VulnerabilityAnalysis.State;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.api.v2.dto.SecurityVulnerabilityDataDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSbomService;
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomComponentSortableField;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomVersionsApplicationSortableField;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataSummaryDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataSummaryListDTO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ResolvedLicenseDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentListDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.Zipper;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.xmlunit.assertj.XmlAssert;

import static com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType.DIRECT;
import static com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType.TRANSITIVE;
import static com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType.UNSPECIFIED;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.cycloneDxIgnoreAttributesFilter;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.cycloneDxIgnoreNodesFilter;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.readFileToString;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.setupScenarioWithMetadataComponentSecurityLicenseAndVex;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Category(SlowTest.class)
public class ApiSbomResourceTest
    extends AbstractResourceTest
{
  private ThirdPartySbomMetadataDAO dao;

  private InsightWork insightWork;

  @Before
  public void setUp() throws Exception {
    dao = lookup(ThirdPartySbomMetadataDAO.class);
    insightWork = lookup(InsightWork.class);

    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.APPLICATION_EVALUATION);
  }

  @After
  public void clearLeakDetectionData() {
    // SBOM tests trigger async processing (policy evaluation, search indexing) that may create
    // entities via background threads during or after TemporaryEntity.after() cleanup.
    // These are not real leaks — the entities are cleaned by the cascading delete — but the
    // detection data captured from the background thread's insert remains, causing false positives.
    AbstractOperationalSqlDAO.testEntityLeaksDetectionData.clear();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SBOM_RESOURCE_PATH);
  }

  @Test
  public void testDeleteSbomVersion_Successful() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path zippedBom = mockOriginalSbom(this.getClass(), "third-party-simple-bom.xml",
        insightWork.getSbomDir(app.getId()).toPath());
    ThirdPartySbomMetadata thirdPartySbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withFilename(zippedBom.getFileName().toString())
        .build();

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VERSION_PATH)
        .parameter(thirdPartySbomMetadata.getApplicationId(), thirdPartySbomMetadata.getSbomVersion())
        .delete();
    assertResponseStatus(204, response);

    ThirdPartySbomMetadata retrievedSbomMetadata =
        dao.getByApplicationIdAndSbomVersion(thirdPartySbomMetadata.getApplicationId(),
            thirdPartySbomMetadata.getSbomVersion());
    assertThat(retrievedSbomMetadata).isNull();
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
        .withSpec(SbomSpecification.CYCLONEDX.toString())
        .build();

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VERSION_PATH)
        .parameter(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion())
        .query("state=" + ApiSbomService.SBOM_STATE_ORIGINAL)
        .get();
    assertResponseStatus(Status.OK.getStatusCode(), response);
    assertThat(response.getContentType()).isEqualTo("application/xml");
    assertContentHeader(response, app, sbomMetadata.getSbomVersion(), ".xml", SbomSpecification.CYCLONEDX, true);
    String actualContent = new String(response.getBodyBytes());
    XmlAssert.assertThat(actualContent)
        .and(expectedContentIn("third-party-simple-bom.xml"))
        .areIdentical();
  }

  @Test
  public void testGetSbomVersion_Current_CycloneDx() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path zippedBom = mockOriginalSbom(this.getClass(), "sboms/valid-cyclonedx-bom.xml",
        insightWork.getSbomDir(app.getId()).toPath());
    String sbomVersion = tempEntity.newRandomHash();
    setupScenarioWithMetadataComponentSecurityLicenseAndVex(tempEntity, app, zippedBom, sbomVersion,
        "CycloneDx", "1.5", SbomFormat.XML);

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VERSION_PATH)
        .parameter(app.getId(), sbomVersion)
        .query("specification=" + "cyclonedx1.5")
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML)
        .get();
    assertResponseStatus(Status.OK.getStatusCode(), response);
    assertThat(response.getContentType()).isEqualTo("application/xml");

    assertContentHeader(response, app, sbomVersion, ".xml", SbomSpecification.CYCLONEDX, false);
    String sbomContent = new String(response.getBodyBytes());
    XmlAssert.assertThat(sbomContent)
        .and(expectedContentIn("sboms/valid-cyclonedx-result-bom.xml"))
        .withNodeFilter(cycloneDxIgnoreNodesFilter())
        .withAttributeFilter(cycloneDxIgnoreAttributesFilter())
        .ignoreWhitespace()
        .areIdentical();
  }

  @Test
  public void testGetSbomVersion_Current_Spdx() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path zippedBom = mockOriginalSbom(this.getClass(), "sboms/valid-spdx-bom.xml",
        insightWork.getSbomDir(app.getId()).toPath());
    String sbomVersion = tempEntity.newRandomHash();
    setupScenarioWithMetadataComponentSecurityLicenseAndVex(tempEntity, app, zippedBom, sbomVersion,
        "SPDX", "2.3", SbomFormat.XML);

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VERSION_PATH)
        .parameter(app.getId(), sbomVersion)
        .query("specification=" + "spdx2.3")
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
        .get();
    assertResponseStatus(Status.OK.getStatusCode(), response);
    assertThat(response.getContentType()).isEqualTo("application/json");

    assertContentHeader(response, app, sbomVersion, ".json", SbomSpecification.SPDX, false);
    String sbomContent = new String(response.getBodyBytes());
    assertThatJson(sbomContent)
        .whenIgnoringPaths("creationInfo.created", "creationInfo.creators[0]", "documentNamespace", "name")
        .isEqualTo(expectedContentIn("sboms/valid-spdx-result-bom.json"));
  }

  @Test
  public void testGetSbomVersion_Original_Json() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path zippedBom = mockOriginalSbom(this.getClass(), "spdx.json",
        insightWork.getSbomDir(app.getId()).toPath());
    ThirdPartySbomMetadata thirdPartySbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withJsonSpecFormat()
        .withFilename(zippedBom.getFileName().toString())
        .withSpec(SbomSpecification.SPDX.toString())
        .build();

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VERSION_PATH)
        .parameter(thirdPartySbomMetadata.getApplicationId(), thirdPartySbomMetadata.getSbomVersion())
        .query("state=" + ApiSbomService.SBOM_STATE_ORIGINAL)
        .get();
    assertResponseStatus(Status.OK.getStatusCode(), response);
    assertThat(response.getContentType()).isEqualTo("application/json");
    assertThat(response.getBodyBytes()).hasSizeGreaterThan(0);

    assertContentHeader(response, app, thirdPartySbomMetadata.getSbomVersion(), ".json", SbomSpecification.SPDX, true);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomMetadataSummaryForApplication_Successful() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    ThirdPartyFile file1 = tempEntity.newThirdPartyFile("CycloneDX -bom.xml");
    ThirdPartyFile file2 = tempEntity.newThirdPartyFile("SPDX .spdx.json");

    ThirdPartySbomMetadata sbom1 =
        tempEntity.newThirdPartySbomMetadata(file1.getId(), app.getId(), ACTIVE, file1.getFilename());
    ThirdPartySbomMetadata sbom2 =
        tempEntity.newThirdPartySbomMetadata(file2.getId(), app.getId(), ACTIVE, file2.getFilename());

    ThirdPartyFileCoordinate c1 = tempEntity.newThirdPartyFileCoordinate(file1, "s1", "f1", "n1", "v1");
    ThirdPartyFileCoordinate c2 = tempEntity.newThirdPartyFileCoordinate(file2, "s2", "f2", "n2", "v2");

    ThirdPartyCoordinateSecurity cs1 =
        tempEntity.newThirdPartyCoordinateSecurity(c1, "r1", sbom1.getId(), "d1", "l1", 3.5F, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(c1, "r2", sbom1.getId(), "d2", "l2", 7.5F, "sd2", "f2");
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r3", sbom2.getId(), "d3", "l3", 1.5F, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r4", sbom2.getId(), "d4", "l4", 0.5F, "sd4", "f4");

    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(cs1, cs1.getRefId(),
        "state", "justification", "response", "detail");

    HttpResponse response = restRequest().path(ApiSbomResource.SBOMS_APPLICATION_PATH)
        .parameter(app.getId())
        .get();
    assertResponseStatus(200, response);

    ThirdPartySbomMetadataSummaryListDTO result = response.getBody(ThirdPartySbomMetadataSummaryListDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);

    List<ThirdPartySbomMetadataSummaryDTO> thirdPartySbomMetadataSummaryDTOList = result.getResults();
    assertThat(thirdPartySbomMetadataSummaryDTOList).hasSize(2);

    ObjectMapper mapper = new ObjectMapper();

    ThirdPartySbomMetadataSummaryDTO thirdPartySbomMetadataSummaryDTO1 =
        mapper.convertValue(thirdPartySbomMetadataSummaryDTOList.get(0), ThirdPartySbomMetadataSummaryDTO.class);
    ThirdPartySbomMetadataSummaryDTO thirdPartySbomMetadataSummaryDTO2 =
        mapper.convertValue(thirdPartySbomMetadataSummaryDTOList.get(1), ThirdPartySbomMetadataSummaryDTO.class);

    List<ThirdPartySbomMetadataSummaryDTO> thirdPartySbomMetadataSummaryDTOListOrdered = new ArrayList<>();
    thirdPartySbomMetadataSummaryDTOListOrdered.add(thirdPartySbomMetadataSummaryDTO1);
    thirdPartySbomMetadataSummaryDTOListOrdered.add(thirdPartySbomMetadataSummaryDTO2);

    Collections.sort(thirdPartySbomMetadataSummaryDTOListOrdered,
        Comparator.comparingInt(ThirdPartySbomMetadataSummaryDTO::getHigh));

    assertThat(thirdPartySbomMetadataSummaryDTOListOrdered.get(0).getLow()).isEqualTo(2);
    assertThat(thirdPartySbomMetadataSummaryDTOListOrdered.get(1).getLow()).isEqualTo(1);
    assertThat(thirdPartySbomMetadataSummaryDTOListOrdered.get(1).getHigh()).isEqualTo(1);

    response = restRequest().path(ApiSbomResource.SBOMS_APPLICATION_PATH)
        .parameter(app.getId())
        .query("sortBy", SbomVersionsApplicationSortableField.RELEASE_STATUS)
        .get();
    assertResponseStatus(200, response);
    result = response.getBody(ThirdPartySbomMetadataSummaryListDTO.class);
    assertThat(result.getTotalResultsCount()).isEqualTo(2);
    assertThat(result.getResults().get(0).getSpecVersion()).isEqualTo(sbom1.getSpecVersion());
    assertThat(result.getResults()).isSortedAccordingTo(
        Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getReleaseStatusPercentage));

    response = restRequest().path(ApiSbomResource.SBOMS_APPLICATION_PATH)
        .parameter(app.getId())
        .query("sortBy", SbomVersionsApplicationSortableField.RELEASE_STATUS)
        .query("asc", false)
        .get();
    assertResponseStatus(200, response);
    result = response.getBody(ThirdPartySbomMetadataSummaryListDTO.class);
    assertThat(result.getTotalResultsCount()).isEqualTo(2);
    assertThat(result.getResults().get(1).getSpecVersion()).isEqualTo(sbom2.getSpecVersion());
    assertThat(result.getResults()).isSortedAccordingTo(
        Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getReleaseStatusPercentage).reversed());
    response = restRequest().path(ApiSbomResource.SBOMS_APPLICATION_PATH)
        .parameter(app.getId())
        .query("sortByDate", "desc")
        .get();
    assertResponseStatus(200, response);
    assertThat(result.getTotalResultsCount()).isEqualTo(2);
    assertThat(result.getResults().get(1).getSpecVersion()).isEqualTo(sbom2.getSpecVersion());
    assertThat(result.getResults()).isSortedAccordingTo(
        Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getImportDate).reversed());
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponents_Successful() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyFile.getId())
        .build();

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    PackageUrlIdentifier packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    ThirdPartyFileCoordinate coordinate = tempEntity.newThirdPartyFileCoordinate("86163fcc32524261bfd2bdbedb7eae42",
        thirdPartyFile, "source", packageUrlIdentifier.getFormat(), packageUrlIdentifier.getName(),
        packageUrlIdentifier.getVersion(), "hash", packageUrlIdentifier.getPackageUrl());
    tempEntity.newThirdPartyCoordinateSecurity(coordinate, "refId", "description", "link",
        CvssV3Severity.NONE.getStartScoreRange(), CvssV3Severity.NONE.getDisplayName(), "fix");

    File reportFile = insightWork.getReportFile(app.getId(), scan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/ApiSbomServicePolicyViolationsTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scan.getScanId());

    HttpResponse response = restRequest()
        .path(ApiSbomResource.SBOM_COMPONENTS_PATH)
        .parameter(app.getId(), sbomMetadata.getSbomVersion())
        .query("vulnerabilityThreatLevels", CvssV3Severity.NONE)
        .query("dependencyTypes", ThirdPartyDependencyType.UNSPECIFIED)
        .query("sortBy", SbomComponentSortableField.VULNERABILITIES)
        .query("asc", true)
        .query("page", 1)
        .query("pageSize", "3")
        .get();

    assertResponseStatus(200, response);
    SbomComponentListDTO result = response.getBody(SbomComponentListDTO.class);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isOne();

    assertThat(result.getResults())
        .singleElement()
        .satisfies(component -> {
          assertThat(component.getPackageUrl()).isEqualTo(packageUrlIdentifier.getPackageUrl());
          assertThat(component.getDisplayName())
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
          assertThat(component.getVulnerabilitySeverityNoneCount()).isOne();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
          assertThat(component.getPolicyViolationCount()).isEqualTo(2);
        });
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponents_SuccessfulWithDefaultValues() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
            .withApplicationId(app.getId())
            .withThirdPartyFileId(thirdPartyFile.getId())
            .build();

    int totalCountOfComponents = 51;

    for (int i = 0; i < totalCountOfComponents; i++) {
      ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p" + i, "v" + i);
      PackageUrlIdentifier packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
      ThirdPartyFileCoordinate coordinate = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
          "source", packageUrlIdentifier.getFormat(), packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion(),
          "hash" + i, packageUrlIdentifier.getPackageUrl());

      CvssV3Severity cvssV3Severity;
      if (i < 10) {
        cvssV3Severity = CvssV3Severity.NONE;
      }
      else if (i < 20) {
        cvssV3Severity = CvssV3Severity.LOW;
      }
      else if (i < 30) {
        cvssV3Severity = CvssV3Severity.MEDIUM;
      }
      else if (i < 40) {
        cvssV3Severity = CvssV3Severity.HIGH;
      }
      else {
        // 40 until 50, that's 11 records using this severity
        cvssV3Severity = CvssV3Severity.CRITICAL;
      }

      tempEntity.newThirdPartyCoordinateSecurity(coordinate, "refId" + i, "description" + i, "link" + i,
          cvssV3Severity.getStartScoreRange(), cvssV3Severity.getDisplayName(), "fix" + i);
    }

    File reportFile = insightWork.getReportFile(app.getId(), scan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/ApiSbomServicePolicyViolationsTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scan.getScanId());

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_COMPONENTS_PATH)
        .parameter(app.getId(), sbomMetadata.getSbomVersion())
        .get();

    assertResponseStatus(200, response);
    SbomComponentListDTO result = response.getBody(SbomComponentListDTO.class);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(totalCountOfComponents);
    assertThat(result.getResults()).hasSize(totalCountOfComponents - 1);

    assertThat(result.getResults().subList(0, 11))
        .allSatisfy(component -> {
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isOne();
        });

    assertThat(result.getResults().subList(11, 21))
        .allSatisfy(component -> {
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isOne();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
        });

    assertThat(result.getResults().subList(21, 31))
        .allSatisfy(component -> {
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isOne();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
        });

    assertThat(result.getResults().subList(31, 41))
        .allSatisfy(component -> {
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isOne();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
        });

    assertThat(result.getResults().subList(41, totalCountOfComponents - 1))
        .allSatisfy(component -> {
          assertThat(component.getVulnerabilitySeverityNoneCount()).isOne();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
        });
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponents_VersionNotExists() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    HttpResponse response = restRequest()
        .path(ApiSbomResource.SBOM_COMPONENTS_PATH)
        .parameter(app.getId(), "fake-version")
        .query("sortBy", SbomComponentSortableField.VULNERABILITIES)
        .query("asc", true)
        .query("page", 1)
        .query("pageSize", "3")
        .get();

    assertResponseStatus(Status.NOT_FOUND.getStatusCode(), response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find version fake-version for application with ID " + app.getId() + ".");
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponents_EmptyResults() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .build();

    HttpResponse response = restRequest()
        .path(ApiSbomResource.SBOM_COMPONENTS_PATH)
        .parameter(app.getId(), sbomMetadata.getSbomVersion())
        .query("sortBy", SbomComponentSortableField.VULNERABILITIES)
        .query("asc", true)
        .query("page", 1)
        .query("pageSize", "3")
        .get();

    assertResponseStatus(200, response);

    SbomComponentListDTO result = response.getBody(SbomComponentListDTO.class);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isZero();
    assertThat(result.getResults()).isEmpty();
  }

  @Test
  public void testGetSbomVersionListByApplication_Successful() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSbomVersion("1.5")
        .build();

    HttpRequest request = restRequest().path(ApiSbomResource.SBOM_VERSIONS_PATH)
        .parameter(app.getId());

    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualTo("application/json");
    List<String> applicationVersionsSbomDTOS = response.getBody(List.class);
    assertThat(applicationVersionsSbomDTOS).hasSize(1);
    assertThat(applicationVersionsSbomDTOS.get(0)).isEqualTo("1.5");
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_ComponentNameFilter() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), application.getId(), ACTIVE, "bom.xml");

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

    File reportFile = insightWork.getReportFile(application.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper.zipReport("/ApiSbomServicePolicyViolationsTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, thirdPartyScan.getScanId());

    HttpResponse response = restRequest()
        .path(ApiSbomResource.SBOM_COMPONENTS_PATH)
        .parameter(application.getId(), sbomMetadata.getSbomVersion())
        .query("vulnerabilityThreatLevels")
        .query("dependencyTypes")
        .query("sortBy")
        .query("asc", true)
        .query("page", 1)
        .query("pageSize", "3")
        .query("filter", "slf4j-log4j")
        .get();

    assertResponseStatus(200, response);
    SbomComponentListDTO result = response.getBody(SbomComponentListDTO.class);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponentsByThirdPartyFileId_LicenseNameFilter() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);

    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), application.getId(), ACTIVE, "bom.xml");

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

    ComponentIdentifier componentIdentifier4 = ComponentIdentifier.createNpmCoordinates("d-license-blah", "3.5.0");
    PackageUrlIdentifier packageUrlIdentifier4 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier4);
    ThirdPartyFileCoordinate coordinate4 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s4", packageUrlIdentifier4.getFormat(), packageUrlIdentifier4.getName(), packageUrlIdentifier4.getVersion(),
        "h4", packageUrlIdentifier4.getPackageUrl(), UNSPECIFIED);

    tempEntity.newThirdPartyCoordinateLicense(coordinate1, "license-1", "License 1", "http://license1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-1", "License 1", "http://license1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-3", "SpecialChars %$3", "http://license3");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-4", "Another 4", "http://license4");
    tempEntity.newThirdPartyCoordinateLicense(coordinate4, "some-5", "Some 5", "http://some5");

    File reportFile = insightWork.getReportFile(application.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper.zipReport("/ApiSbomServicePolicyViolationsTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, thirdPartyScan.getScanId());

    HttpResponse response = restRequest()
        .path(ApiSbomResource.SBOM_COMPONENTS_PATH)
        .parameter(application.getId(), sbomMetadata.getSbomVersion())
        .query("vulnerabilityThreatLevels")
        .query("dependencyTypes")
        .query("sortBy")
        .query("asc", true)
        .query("page", 1)
        .query("pageSize", "3")
        .query("filter", "license")
        .get();

    assertResponseStatus(200, response);
    SbomComponentListDTO result = response.getBody(SbomComponentListDTO.class);

    assertThat(result).isNotNull();
    // componentIdentifier1, componentIdentifier2 match based on license text (license-x)
    // while componentIdentifier4 is matched based on component name (d-license-blah)
    assertThat(result.getTotalResultsCount()).isEqualTo(3);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomComponents_withLicenseOverrides() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);

    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), application.getId(), ACTIVE, "bom.xml");

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

    tempEntity.newThirdPartyCoordinateLicense(coordinate1, "license-1", "License 1", "http://license1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-1", "License 1", "http://license1");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-3", "SpecialChars %$3", "http://license3");
    tempEntity.newThirdPartyCoordinateLicense(coordinate2, "license-4", "Another 4", "http://license4");
    // mock license override
    tempEntity.newLicenseOverride(application.getId(), componentIdentifier2, LicenseOverrideStatus.SELECTED,
        Set.of("Aladdin", "MIT"));

    File reportFile = insightWork.getReportFile(application.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper.zipReport("/ApiSbomServicePolicyViolationsTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, thirdPartyScan.getScanId());

    HttpResponse response = restRequest()
        .path(ApiSbomResource.SBOM_COMPONENTS_PATH)
        .parameter(application.getId(), sbomMetadata.getSbomVersion())
        .query("vulnerabilityThreatLevels")
        .query("dependencyTypes")
        .query("sortBy")
        .query("asc", true)
        .query("page", 1)
        .query("pageSize", "3")
        .query("filter", "license")
        .get();

    assertResponseStatus(200, response);
    SbomComponentListDTO result = response.getBody(SbomComponentListDTO.class);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(2);
    assertThat(result.getResults().stream().filter(r -> r.getComponentIdentifier().equals(componentIdentifier2)))
        .hasSize(1)
        .allSatisfy(dto -> {
          assertThat(dto.getLicenses()).extracting(ResolvedLicenseDTO::licenseId)
              .containsExactlyInAnyOrder("Aladdin", "MIT");
          assertThat(dto.getLicenses()).extracting(ResolvedLicenseDTO::overrideStatus)
              .containsOnly(LicenseOverrideStatus.SELECTED);
        });
  }

  @Test
  public void testGetSbomVersionListByApplication_Empty() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    HttpRequest request = restRequest().path(ApiSbomResource.SBOM_VERSIONS_PATH)
        .parameter(app.getId());

    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualTo("application/json");
    List<String> applicationVersionsSbomDTOS = response.getBody(List.class);
    assertThat(applicationVersionsSbomDTOS).isEmpty();
  }

  @Test
  public void testImportSbom_SPDX() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    mockReport("SCAN-ID", "/" + getClass().getSimpleName() + "/report");

    byte[] sbomFile = loadFileFromAssets("/" + getClass().getSimpleName() + "/spdx.json");
    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_IMPORT_PATH)
        .part("file", "spdx.json", sbomFile)
        .part("applicationId", app.getId())
        .post();

    assertResponseStatus(Status.OK.getStatusCode(), response);
    ApiThirdPartyScanTicketDTO apiThirdPartyScanTicketDTO = response.getBody(ApiThirdPartyScanTicketDTO.class);
    assertThat(apiThirdPartyScanTicketDTO.statusUrl).startsWith(
        String.format("%s%s/%s/status", PublicApiPaths.SBOM_RESOURCE_PATH, ApiSbomResource.SBOMS_APPLICATIONS_PATH,
            app.getId()));

    ApiSbomStatusDTO resultDTO = getSbomStatusDTO(apiThirdPartyScanTicketDTO.statusUrl);
    assertThat(resultDTO.errorMessage).isNull();
    assertThat(resultDTO.isError).isFalse();
    assertSbomMetadataIdIsSetOnThirdPartyCoordinateSecurityEntities(resultDTO);
  }

  @Test
  public void testImportSbom_SPDX_CustomVersion() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    mockReport("SCAN-ID", "/" + getClass().getSimpleName() + "/report");

    String applicationVersion = "my_application_version";

    byte[] sbomFile = loadFileFromAssets("/" + getClass().getSimpleName() + "/spdx.json");
    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_IMPORT_PATH)
        .part("file", "spdx.json", sbomFile)
        .part("applicationId", app.getId())
        .part("applicationVersion", applicationVersion)
        .post();

    assertResponseStatus(Status.OK.getStatusCode(), response);
    ApiThirdPartyScanTicketDTO apiThirdPartyScanTicketDTO = response.getBody(ApiThirdPartyScanTicketDTO.class);
    assertThat(apiThirdPartyScanTicketDTO.statusUrl).startsWith(
        String.format("%s%s/%s/status", PublicApiPaths.SBOM_RESOURCE_PATH, ApiSbomResource.SBOMS_APPLICATIONS_PATH,
            app.getId()));

    ApiSbomStatusDTO resultDTO = getSbomStatusDTO(apiThirdPartyScanTicketDTO.statusUrl);
    assertThat(resultDTO.errorMessage).isNull();
    assertThat(resultDTO.isError).isFalse();
    assertThat(resultDTO.version).isEqualTo(applicationVersion);
    assertSbomMetadataIdIsSetOnThirdPartyCoordinateSecurityEntities(resultDTO);
  }

  @Test
  public void testImportSbom_InvalidSPDX_IgnoreValidationError() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    mockReport("SCAN-ID", "/" + getClass().getSimpleName() + "/report");

    byte[] sbomFile = loadFileFromAssets("/" + getClass().getSimpleName() + "/invalid-spdx.json");
    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_IMPORT_PATH)
        .query("ignoreValidationError", true)
        .part("file", "spdx.json", sbomFile)
        .part("applicationId", app.getId())
        .post();

    assertResponseStatus(Status.OK.getStatusCode(), response);
    ApiThirdPartyScanTicketDTO apiThirdPartyScanTicketDTO = response.getBody(ApiThirdPartyScanTicketDTO.class);
    assertThat(apiThirdPartyScanTicketDTO.statusUrl).startsWith(
        String.format("%s%s/%s/status", PublicApiPaths.SBOM_RESOURCE_PATH, ApiSbomResource.SBOMS_APPLICATIONS_PATH,
            app.getId()));

    ApiSbomStatusDTO resultDTO = getSbomStatusDTO(apiThirdPartyScanTicketDTO.statusUrl);
    assertThat(resultDTO.errorMessage).isNull();
    assertThat(resultDTO.isError).isFalse();
    assertSbomMetadataIdIsSetOnThirdPartyCoordinateSecurityEntities(resultDTO);
  }

  @Test
  public void testImportSbom_CycloneDX() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    mockReport("SCAN-ID", "/" + getClass().getSimpleName() + "/report");

    byte[] sbomFile = loadFileFromAssets("/" + getClass().getSimpleName() + "/third-party-simple-bom.xml");
    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_IMPORT_PATH)
        .parameter(app.getId())
        .part("file", "third-party-simple-bom.xml", sbomFile)
        .part("applicationId", app.getId())
        .post();

    assertResponseStatus(Status.OK.getStatusCode(), response);
    ApiThirdPartyScanTicketDTO apiThirdPartyScanTicketDTO = response.getBody(ApiThirdPartyScanTicketDTO.class);
    assertThat(apiThirdPartyScanTicketDTO.statusUrl).startsWith(
        String.format("%s%s/%s/status", PublicApiPaths.SBOM_RESOURCE_PATH, ApiSbomResource.SBOMS_APPLICATIONS_PATH,
            app.getId()));

    ApiSbomStatusDTO resultDTO = getSbomStatusDTO(apiThirdPartyScanTicketDTO.statusUrl);
    assertThat(resultDTO.errorMessage).isNull();
    assertThat(resultDTO.isError).isFalse();
    assertSbomMetadataIdIsSetOnThirdPartyCoordinateSecurityEntities(resultDTO);
  }

  @Test
  public void testImportSbom_CycloneDX_CustomVersion() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    mockReport("SCAN-ID", "/" + getClass().getSimpleName() + "/report");

    String applicationVersion = "my_application_version";

    byte[] sbomFile = loadFileFromAssets("/" + getClass().getSimpleName() + "/third-party-simple-bom.xml");
    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_IMPORT_PATH)
        .parameter(app.getId())
        .part("file", "third-party-simple-bom.xml", sbomFile)
        .part("applicationId", app.getId())
        .part("applicationVersion", applicationVersion)
        .post();

    assertResponseStatus(Status.OK.getStatusCode(), response);
    ApiThirdPartyScanTicketDTO apiThirdPartyScanTicketDTO = response.getBody(ApiThirdPartyScanTicketDTO.class);
    assertThat(apiThirdPartyScanTicketDTO.statusUrl).startsWith(
        String.format("%s%s/%s/status", PublicApiPaths.SBOM_RESOURCE_PATH, ApiSbomResource.SBOMS_APPLICATIONS_PATH,
            app.getId()));

    ApiSbomStatusDTO resultDTO = getSbomStatusDTO(apiThirdPartyScanTicketDTO.statusUrl);
    assertThat(resultDTO.errorMessage).isNull();
    assertThat(resultDTO.isError).isFalse();
    assertThat(resultDTO.version).isEqualTo(applicationVersion);
    assertSbomMetadataIdIsSetOnThirdPartyCoordinateSecurityEntities(resultDTO);
  }

  @Test
  public void testImportSbom_InvalidCycloneDX_IgnoreValidationError() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    mockReport("SCAN-ID", "/" + getClass().getSimpleName() + "/report");

    byte[] sbomFile = loadFileFromAssets("/" + getClass().getSimpleName() + "/invalid-third-party-simple-bom.xml");
    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_IMPORT_PATH)
        .parameter(app.getId())
        .query("ignoreValidationError", true)
        .part("file", "third-party-simple-bom.xml", sbomFile)
        .part("applicationId", app.getId())
        .post();

    assertResponseStatus(Status.OK.getStatusCode(), response);
    ApiThirdPartyScanTicketDTO apiThirdPartyScanTicketDTO = response.getBody(ApiThirdPartyScanTicketDTO.class);
    assertThat(apiThirdPartyScanTicketDTO.statusUrl).startsWith(
        String.format("%s%s/%s/status", PublicApiPaths.SBOM_RESOURCE_PATH, ApiSbomResource.SBOMS_APPLICATIONS_PATH,
            app.getId()));

    ApiSbomStatusDTO resultDTO = getSbomStatusDTO(apiThirdPartyScanTicketDTO.statusUrl);
    assertThat(resultDTO.errorMessage).isNull();
    assertThat(resultDTO.isError).isFalse();
    assertSbomMetadataIdIsSetOnThirdPartyCoordinateSecurityEntities(resultDTO);
  }

  @Test
  public void testImportSbom_Binary() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    SystemConfigurationPropertyFeature.SBOM_BINARY_SCANNING.setEnabled(true);

    mockReport("SCAN-ID", "/" + getClass().getSimpleName() + "/report");

    File binaryFile = tempDir.newFile("binary-scan");
    Zipper.zipDirectory(new File(getClass().getResource("/ApiSbomServiceTest/binary-scan").toURI()),
        binaryFile);
    try (FileInputStream binaryInputStream = new FileInputStream(binaryFile)) {
      byte[] inputFile = IOUtils.toByteArray(binaryInputStream);
      HttpResponse response = restRequest().path(ApiSbomResource.SBOM_IMPORT_PATH)
          .parameter(app.getId())
          .part("file", "binary-scan.zip", inputFile)
          .part("applicationId", app.getId())
          .query("enableBinaryImport", "true")
          .post();

      assertResponseStatus(Status.OK.getStatusCode(), response);

      ApiThirdPartyScanTicketDTO ticketDTO = response.getBody(ApiThirdPartyScanTicketDTO.class);
      assertThat(ticketDTO.statusUrl).startsWith(
          String.format("%s%s/%s/status", PublicApiPaths.SBOM_RESOURCE_PATH, ApiSbomResource.SBOMS_APPLICATIONS_PATH,
              app.getId()));

      ApiSbomStatusDTO resultDTO = getSbomStatusDTO(ticketDTO.statusUrl);
      assertThat(resultDTO.errorMessage).isNull();
      assertThat(resultDTO.isError).isFalse();

      ThirdPartySbomMetadata sbomMetadata =
          dao.getByApplicationIdAndSbomVersion(app.getId(), resultDTO.version);
      assertThat(sbomMetadata).isNotNull();
    }
  }

  @Test
  public void testImportSbom_EmptyApplicationId() throws Exception {
    byte[] sbomFile = loadFileFromAssets("/" + getClass().getSimpleName() + "/third-party-simple-bom.xml");
    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_IMPORT_PATH)
        .part("file", "third-party-simple-bom.xml", sbomFile)
        .post();

    assertResponseStatus(Status.BAD_REQUEST.getStatusCode(), response);
    assertThat(response.getBodyText()).isEqualTo("Missing required parameter [applicationId]");
  }

  @Test
  public void testSaveVulnerabilityAnalysis() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(thirdPartyFile);
    String refId = "CVE-123";
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, "file.tgz");
    ThirdPartyFileCoordinate component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "ThirdParty", "npm", "bloom", "1.0", "hash001",
            "pkg:npm/bloom@1.0");
    tempEntity.newThirdPartyCoordinateSecurity(component, refId, "description", "link", 8.1, "Critical", "1.2.0");

    ApiSbomVulnerabilityAnalysisRequestDTO dto = new ApiSbomVulnerabilityAnalysisRequestDTO();
    dto.setComponentLocator(new ComponentLocator(component.getHash(), null));
    dto.setVulnerabilityAnalysis(mockAnalysisRequest());

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VULNERABILITY_ANALYSIS_ANNOTATION_PATH)
        .parameter(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(), refId)
        .body(dto)
        .put();

    assertResponseStatus(Status.OK.getStatusCode(), response);
    JsonNode result = new ObjectMapper().readTree(response.getBodyText());
    assertThat(result.get("state").asText()).isEqualTo(State.EXPLOITABLE.toString());
    assertThat(result.get("justification").asText()).isEqualTo(Justification.REQUIRES_DEPENDENCY.toString());
    assertThat(result.get("response").asText()).isEqualTo(Response.WILL_NOT_FIX.toString());
    assertThat(result.get("detail").asText()).isEqualTo("detail");
    assertThat(result.get("createdOn").asText()).isNotNull();
    assertThat(result.get("lastUpdatedOn").asText()).isNotNull();
    assertThat(result.get("lastUpdatedBy").asText()).isEqualTo("admin");
  }

  @Test
  public void testDeleteVulnerabilityAnalysis() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(thirdPartyFile);
    String refId = "CVE-123";
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, "file.tgz");
    ThirdPartyFileCoordinate component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "ThirdParty", "npm", "bloom", "1.0", "hash001",
            "pkg:npm/bloom@1.0");
    ThirdPartyCoordinateSecurity security =
        tempEntity.newThirdPartyCoordinateSecurity(component, refId, "description", "link", 8.1, "Critical", "1.2.0");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(security, security.getRefId(),
        State.EXPLOITABLE.toString(), Justification.REQUIRES_DEPENDENCY.toString(), Response.WILL_NOT_FIX.toString(),
        "some detail");

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VULNERABILITY_ANALYSIS_ANNOTATION_PATH)
        .parameter(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(), refId)
        .body(new ComponentLocator(component.getHash(), null))
        .delete();

    assertResponseStatus(Status.NO_CONTENT.getStatusCode(), response);
  }

  @Test
  public void testGetVulnerabilityDetails_ByComponentHash() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(thirdPartyFile);
    String refId = "CVE-123";
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, "file.tgz");
    ThirdPartyFileCoordinate component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "ThirdParty", "npm", "bloom", "1.0", "hash001",
            "pkg:npm/bloom@1.0");
    tempEntity.newThirdPartyCoordinateSecurity(component, refId, "description", "link", 8.1, "Critical", "1.2.0");

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VULNERABILITY_PATH)
        .parameter(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(), refId)
        .query("componentHash", component.getHash())
        .get();

    assertResponseStatus(Status.OK.getStatusCode(), response);
    SecurityVulnerabilityDataDTO vulnerabilityDetails = response.getBody(SecurityVulnerabilityDataDTO.class);
    assertThat(vulnerabilityDetails.identifier).isEqualTo(refId);
    assertThat(vulnerabilityDetails.mainSeverity.score).isEqualTo(8.1f);
  }

  @Test
  public void testGetVulnerabilityDetails_ByPackageUrl() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(thirdPartyFile);
    String refId = "CVE-123";
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, "file.tgz");
    ThirdPartyFileCoordinate component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "ThirdParty", "npm", "bloom", "1.0", "hash001",
            "pkg:npm/bloom@1.0");
    tempEntity.newThirdPartyCoordinateSecurity(component, refId, "description", "link", 8.1, "Critical", "1.2.0");

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VULNERABILITY_PATH)
        .parameter(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(), refId)
        .query("packageUrl", component.getPackageUrl())
        .get();

    assertResponseStatus(Status.OK.getStatusCode(), response);
    SecurityVulnerabilityDataDTO vulnerabilityDetails = response.getBody(SecurityVulnerabilityDataDTO.class);
    assertThat(vulnerabilityDetails.identifier).isEqualTo(refId);
    assertThat(vulnerabilityDetails.mainSeverity.score).isEqualTo(8.1f);
  }

  @Test
  public void testGetVulnerabilityDetails_NotFound() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VULNERABILITY_PATH)
        .parameter(app.getId(), "v1", "CVE-123")
        .query("componentHash", "hash")
        .get();

    assertResponseStatus(Status.NOT_FOUND.getStatusCode(), response);
  }

  private static VulnerabilityAnalysis mockAnalysisRequest() {
    VulnerabilityAnalysis analysis = new VulnerabilityAnalysis();
    analysis.setState(State.EXPLOITABLE);
    analysis.setJustification(Justification.REQUIRES_DEPENDENCY);
    analysis.setResponse(Response.WILL_NOT_FIX);
    analysis.setDetail("detail");
    return analysis;
  }

  private byte[] loadFileFromAssets(String fileName) throws IOException {
    try (InputStream inputStream = getClass().getResourceAsStream(fileName)) {
      assertThat(inputStream).as("Missing resource: " + fileName).isNotNull();
      return IOUtils.toByteArray(inputStream);
    }
  }

  private ApiSbomStatusDTO getSbomStatusDTO(String statusUrl) {
    HttpResponse response = await().atMost(10, TimeUnit.SECONDS)
        .until(() -> super.restRequest().path(statusUrl).get(),
            resp -> resp.getStatusCode() == 200);
    return response.getBody(ApiSbomStatusDTO.class);
  }

  private String expectedContentIn(String filePath) throws Exception {
    return readFileToString(this.getClass(), filePath);
  }

  private static void assertContentHeader(
      final HttpResponse response,
      final Application app,
      final String sbomVersion,
      final String specFormat,
      final SbomSpecification sbomSpec,
      final boolean isOriginal)
  {
    String contentHeader = response.getHeader("Content-Disposition");
    String actualFilename = contentHeader.substring(contentHeader.indexOf("=") + 1).split(";")[0].replaceAll("\"", "");
    assertThat(actualFilename).matches(
        (isOriginal ? "Original_" : "") +
            app.getPublicId() +
            "_" +
            sbomVersion +
            "_(\\d)+." +
            (sbomSpec.equals(SbomSpecification.SPDX) ? "spdx" : "cdx") +
            specFormat);
  }

  private void assertSbomMetadataIdIsSetOnThirdPartyCoordinateSecurityEntities(ApiSbomStatusDTO resultDTO) {
    ThirdPartySbomMetadata thirdPartySbomMetadata = getCLMServer()
        .getInstance(ThirdPartySbomMetadataDAO.class)
        .getByApplicationIdAndSbomVersion(resultDTO.applicationId, resultDTO.version);
    assertThat(thirdPartySbomMetadata).isNotNull();
    List<ThirdPartyFileCoordinate> thirdPartyFileCoordinates = getCLMServer()
        .getInstance(ThirdPartyFileCoordinateDAO.class)
        .getByThirdPartyFileId(thirdPartySbomMetadata.getThirdPartyFileId());
    List<String> thirdPartyFileCoordinateIds = thirdPartyFileCoordinates.stream()
        .map(ThirdPartyFileCoordinate::getId)
        .collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(thirdPartyFileCoordinateIds)) {
      List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurities = getCLMServer()
          .getInstance(ThirdPartyCoordinateSecurityDAO.class)
          .getByFileCoordinateIds(thirdPartyFileCoordinateIds);
      assertThat(thirdPartyCoordinateSecurities)
          .allMatch(s -> s.getSbomMetadataId().equals(thirdPartySbomMetadata.getId()));
    }
  }
}
