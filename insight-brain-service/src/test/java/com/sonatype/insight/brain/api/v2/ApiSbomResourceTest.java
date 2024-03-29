/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response.Status;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSbomService;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataSummaryDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataSummaryListDTO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.brain.utils.SbomTestsHelper;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ApiSbomResourceTest
    extends AbstractResourceTest
{
  private ThirdPartySbomMetadataDAO dao;

  private InsightWork insightWork;

  @Before
  public void setUp() throws Exception {
    dao = lookup(ThirdPartySbomMetadataDAO.class);
    insightWork = lookup(InsightWork.class);

    setFeatures(LicensedFeature.SBOM_MANAGER);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SBOM_RESOURCE_PATH);
  }

  @Test
  public void testDeleteSbomVersion() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path fileInWorkDirPath =
        SbomTestsHelper.createTestFileForSbomMetadata(insightWork.getSbomDir(app.getId()),
            getClass().getResource("/" + getClass().getSimpleName() + "/third-party-simple-bom.xml"));
    ThirdPartySbomMetadata thirdPartySbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withFilename(fileInWorkDirPath.getFileName().toString())
        .build();

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VERSION_PATH)
        .parameter(thirdPartySbomMetadata.getApplicationId(), thirdPartySbomMetadata.getSbomVersion()).delete();
    assertResponseStatus(204, response);

    ThirdPartySbomMetadata retrievedSbomMetadata =
        dao.getByApplicationIdAndSbomVersion(thirdPartySbomMetadata.getApplicationId(),
            thirdPartySbomMetadata.getSbomVersion());
    assertThat(retrievedSbomMetadata).isNull();
  }

  @Test
  public void testGetSbomVersion_Xml() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path fileInWorkDirPath =
        SbomTestsHelper.createTestFileForSbomMetadata(insightWork.getSbomDir(app.getId()),
            getClass().getResource(
                "/" + getClass().getSimpleName() + "/cb4e10e0f3a94fd98bee955b53f9474c7343830902282944835.xml.gz"));
    ThirdPartySbomMetadata thirdPartySbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withFilename(fileInWorkDirPath.getFileName().toString())
        .build();

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VERSION_PATH)
        .parameter(thirdPartySbomMetadata.getApplicationId(), thirdPartySbomMetadata.getSbomVersion())
        .query("state=" + ApiSbomService.SBOM_STATE_ORIGINAL)
        .get();
    assertResponseStatus(Status.OK.getStatusCode(), response);
    assertThat(response.getContentType()).isEqualTo("application/xml");
    assertThat(response.getBodyBytes()).hasSizeGreaterThan(0);

    String contentHeader = response.getHeader("Content-Disposition");
    String actualFilename = contentHeader.substring(contentHeader.indexOf("=") + 1).split(";")[0].replaceAll("\"", "");
    assertThat(actualFilename).isEqualTo(app.getName() + "_" + thirdPartySbomMetadata.getSbomVersion() + ".xml");
  }

  @Test
  public void testGetSbomVersion_Json() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path fileInWorkDirPath =
        SbomTestsHelper.createTestFileForSbomMetadata(insightWork.getSbomDir(app.getId()),
            getClass().getResource(
                "/" + getClass().getSimpleName() + "/668bbb2087354637b030de2bc1a3faf76935110932971722768.json.gz"));
    ThirdPartySbomMetadata thirdPartySbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withJsonSpecFormat()
        .withFilename(fileInWorkDirPath.getFileName().toString())
        .build();

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VERSION_PATH)
        .parameter(thirdPartySbomMetadata.getApplicationId(), thirdPartySbomMetadata.getSbomVersion())
        .query("state=" + ApiSbomService.SBOM_STATE_ORIGINAL)
        .get();
    assertResponseStatus(Status.OK.getStatusCode(), response);
    assertThat(response.getContentType()).isEqualTo("application/json");
    assertThat(response.getBodyBytes()).hasSizeGreaterThan(0);

    String contentHeader = response.getHeader("Content-Disposition");
    String actualFilename = contentHeader.substring(contentHeader.indexOf("=") + 1).split(";")[0].replaceAll("\"", "");
    assertThat(actualFilename).isEqualTo(app.getName() + "_" + thirdPartySbomMetadata.getSbomVersion() + ".json");
  }

  @Test
  @PostgresTest
  public void testGetListOfSbomsForApplicationId() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    ThirdPartyFile file1 = tempEntity.newThirdPartyFile("CycloneDX -bom.xml");
    ThirdPartyFile file2 = tempEntity.newThirdPartyFile("SPDX .spdx.json");

    tempEntity.newThirdPartySbomMetadata(file1.getId(), app.getId(), "ACTIVE", file1.getFilename());
    tempEntity.newThirdPartySbomMetadata(file2.getId(), app.getId(), "ACTIVE", file2.getFilename());

    ThirdPartyFileCoordinate c1 = tempEntity.newThirdPartyFileCoordinate(file1, "s1", "f1", "n1", "v1");
    ThirdPartyFileCoordinate c2 = tempEntity.newThirdPartyFileCoordinate(file2, "s2", "f2", "n2", "v2");

    tempEntity.newThirdPartyCoordinateSecurity(c1, "r1", "d1", "l1", 3.5F, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(c1, "r2", "d2", "l2", 7.5F, "sd2", "f2");
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r3", "d3", "l3", 1.5F, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r4", "d4", "l4", 0.5F, "sd4", "f4");

    HttpResponse response = restRequest().path(ApiSbomResource.SBOMS_BY_APPLICATION_ID_PATH)
        .parameter(app.getId()).get();
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
  }

  @Test
  @PostgresTest
  public void testGetSbomComponents() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .build();

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    PackageUrlIdentifier packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "source",
        packageUrlIdentifier.getFormat(), packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion(), "hash",
        packageUrlIdentifier.getPackageUrl());

    HttpResponse response = restRequest()
        .path(ApiSbomResource.SBOM_COMPONENTS_PATH)
        .parameter(app.getId(), sbomMetadata.getSbomVersion())
        .get();

    assertResponseStatus(200, response);
    SbomComponentDTO[] result = response.getBody(SbomComponentDTO[].class);

    assertThat(result)
        .hasOnlyOneElementSatisfying(component -> {
          assertThat(component.getPackageUrl()).isEqualTo(packageUrlIdentifier.getPackageUrl());
          assertThat(component.getDisplayName())
              .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
          assertThat(component.getVulnerabilitySeverityNoneCount()).isZero();
          assertThat(component.getVulnerabilitySeverityLowCount()).isZero();
          assertThat(component.getVulnerabilitySeverityMediumCount()).isZero();
          assertThat(component.getVulnerabilitySeverityHighCount()).isZero();
          assertThat(component.getVulnerabilitySeverityCriticalCount()).isZero();
        });
  }

  @Test
  @PostgresTest
  public void testGetSbomComponents_VersionNotExists() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    HttpResponse response = restRequest()
        .path(ApiSbomResource.SBOM_COMPONENTS_PATH)
        .parameter(app.getId(), "fake-version")
        .get();

    assertResponseStatus(Status.NOT_FOUND.getStatusCode(), response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find version fake-version for application with ID " + app.getId() + ".");
  }

  @Test
  @PostgresTest
  public void testGetSbomComponents_EmptyResults() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .build();

    HttpResponse response = restRequest()
        .path(ApiSbomResource.SBOM_COMPONENTS_PATH)
        .parameter(app.getId(), sbomMetadata.getSbomVersion())
        .get();

    assertResponseStatus(200, response);
    assertThat(response.getBody(SbomComponentDTO[].class)).isEmpty();
  }

  @Test
  public void testGetSbomVersionListByAppId_Successful() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSbomVersion("1.5")
        .build();

    HttpRequest request = restRequest().path(ApiSbomResource.SBOM_VERSIONS_BY_APPLICATION_ID_PATH)
        .parameter(app.getId());

    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualTo("application/json");
    List<String> applicationVersionsSbomDTOS = response.getBody(List.class);
    assertThat(applicationVersionsSbomDTOS).hasSize(1);
    assertThat(applicationVersionsSbomDTOS.get(0)).isEqualTo("1.5");
  }

  @Test
  public void testGetSbomVersionListByAppId_Empty() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    HttpRequest request = restRequest().path(ApiSbomResource.SBOM_VERSIONS_BY_APPLICATION_ID_PATH)
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
    Files.createDirectories(insightWork.getSbomDir(app.getId()).toPath());

    mockReport("SCAN-ID", "/" + getClass().getSimpleName() + "/report");

    byte[] sbomFile = loadFileFromAssets("/" + getClass().getSimpleName() + "/spdx.json");
    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_IMPORT_PATH)
        .part("file", "spdx.json", sbomFile)
        .part("applicationId", app.getId())
        .post();

    assertResponseStatus(Status.OK.getStatusCode(), response);
    ApiThirdPartyScanTicketDTO apiThirdPartyScanTicketDTO = response.getBody(ApiThirdPartyScanTicketDTO.class);
    assertThat(apiThirdPartyScanTicketDTO.statusUrl).startsWith(
        "api/v2/sbom/" + app.getId() + "/status/");

    ApiSbomStatusDTO resultDTO = getSbomStatusDTO(apiThirdPartyScanTicketDTO.statusUrl);
    assertThat(resultDTO.errorMessage).isNull();
    assertThat(resultDTO.isError).isFalse();
  }

  @Test
  public void testImportSbom_CycloneDX() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Files.createDirectories(insightWork.getSbomDir(app.getId()).toPath());

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
        "api/v2/sbom/" + app.getId() + "/status/");

    ApiSbomStatusDTO resultDTO = getSbomStatusDTO(apiThirdPartyScanTicketDTO.statusUrl);
    assertThat(resultDTO.errorMessage).isNull();
    assertThat(resultDTO.isError).isFalse();
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

  private byte[] loadFileFromAssets(String fileName) throws IOException {
    try (InputStream inputStream = getClass().getResourceAsStream(fileName)) {
      assertThat(inputStream).as("Missing resource: " + fileName).isNotNull();
      return IOUtils.toByteArray(inputStream);
    }
  }

  private ApiSbomStatusDTO getSbomStatusDTO(String statusUrl) {
    HttpResponse response = await().atMost(10, TimeUnit.SECONDS).until(() -> super.restRequest().path(statusUrl).get(),
        resp -> resp.getStatusCode() == 200);
    return response.getBody(ApiSbomStatusDTO.class);
  }
}
