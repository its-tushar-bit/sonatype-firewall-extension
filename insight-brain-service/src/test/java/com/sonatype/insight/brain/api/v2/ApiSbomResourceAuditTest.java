/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

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
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ApiSbomResourceAuditTest
    extends AbstractAuditTest
{
  @Inject
  private InsightWork insightWork;

  @Before
  public void setUp() throws Exception {
    insightWork = lookup(InsightWork.class);
    setFeatures(LicensedFeature.SBOM_MANAGER);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SBOM_RESOURCE_PATH);
  }

  @Test
  public void testDeleteSbomVersion_Authorized() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Path zippedBom = mockOriginalSbom(this.getClass(), "third-party-simple-bom.xml",
        insightWork.getSbomDir(app.getId()).toPath());

    ThirdPartySbomMetadata thirdPartySbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withFilename(zippedBom.getFileName().toString())
        .build();

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VERSION_PATH)
        .parameter(thirdPartySbomMetadata.getApplicationId(), thirdPartySbomMetadata.getSbomVersion()).delete();
    assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_SBOM_VERSION, null);
    assertThat(auditDTO.data).containsEntry("applicationId", thirdPartySbomMetadata.getApplicationId());
    assertCustomData(auditDTO, "sbomVersion", thirdPartySbomMetadata.getSbomVersion());
  }

  @Test
  public void testDeleteSbomVersion_Unauthorized() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata thirdPartySbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .build();
    HttpResponse response = restRequest().with(unauthorizedUser()).path(ApiSbomResource.SBOM_VERSION_PATH)
        .parameter(thirdPartySbomMetadata.getApplicationId(), thirdPartySbomMetadata.getSbomVersion()).delete();
    assertResponseStatus(403, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_SBOM_VERSION, "unauthorized");
    assertThat(auditDTO.data).containsEntry("applicationId", thirdPartySbomMetadata.getApplicationId());
  }

  @Test
  public void testImportSbom_Authorized() throws Exception {
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.APPLICATION_EVALUATION);
    Application app = tempEntity.newApplicationWithParent();

    mockReport("SCAN-ID", "/" + getClass().getSimpleName() + "/report");

    byte[] sbomFile = loadFileFromAssets("/" + getClass().getSimpleName() + "/third-party-simple-bom.xml");
    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_IMPORT_PATH)
        .part("file", "third-party-simple-bom.xml", sbomFile)
        .part("applicationId", app.getId())
        .post();

    assertResponseStatus(Status.OK.getStatusCode(), response);
    ApiThirdPartyScanTicketDTO apiThirdPartyScanTicketDTO = response.getBody(ApiThirdPartyScanTicketDTO.class);
    assertThat(apiThirdPartyScanTicketDTO.statusUrl).startsWith(
        "api/v2/sbom/applications/" + app.getId() + "/status/");

    ApiSbomStatusDTO resultDTO = getStatusResponse(apiThirdPartyScanTicketDTO.statusUrl);
    assertThat(resultDTO.errorMessage).isNull();
    assertThat(resultDTO.isError).isFalse();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT_SBOM_VERSION, null);
    assertThat(auditDTO.data).containsEntry("applicationId", app.getId());
  }

  @Test
  public void testSaveVulnerabilityAnalysis_Authorized() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(thirdPartyFile);
    String refId = "CVE-123";
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ThirdPartySbomMetadataStatus.ACTIVE,
            "file.tgz");
    ThirdPartyFileCoordinate
        component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "ThirdParty", "npm", "bloom", "1.0", "hash001",
            "pkg:npm/bloom@1.0");
    tempEntity.newThirdPartyCoordinateSecurity(component, refId, "description", "link", 8.1, "Critical",
        "1.2.0");

    ApiSbomVulnerabilityAnalysisRequestDTO dto = new ApiSbomVulnerabilityAnalysisRequestDTO();
    dto.setComponentLocator(new ComponentLocator(component.getHash(), component.getPackageUrl()));
    dto.setVulnerabilityAnalysis(mockAnalysisRequest());

    restRequest().path(ApiSbomResource.SBOM_VULNERABILITY_ANALYSIS_ANNOTATION_PATH)
        .parameter(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(), refId)
        .body(dto)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_SBOM_VULNERABILITY_ANALYSIS, null);
    assertVulnerabilityAnalysisAuditData(auditDTO, app, component.getHash(), component.getPackageUrl(), refId);
  }

  @Test
  public void testSaveVulnerabilityAnalysis_Unauthorized() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String refId = "CVE-123";

    ApiSbomVulnerabilityAnalysisRequestDTO dto = new ApiSbomVulnerabilityAnalysisRequestDTO();
    dto.setComponentLocator(new ComponentLocator("hash001", "purl"));
    dto.setVulnerabilityAnalysis(mockAnalysisRequest());

    restRequest().path(ApiSbomResource.SBOM_VULNERABILITY_ANALYSIS_ANNOTATION_PATH)
        .with(unauthorizedUser())
        .parameter(app.getId(), "v1", refId)
        .body(dto)
        .put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_SBOM_VULNERABILITY_ANALYSIS, "unauthorized");
    assertVulnerabilityAnalysisAuditData(auditDTO, app, "hash001", "purl", refId);
  }

  @Test
  public void testDeleteVulnerabilityAnalysis_Authorized() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(thirdPartyFile);
    String refId = "CVE-123";
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ThirdPartySbomMetadataStatus.ACTIVE,
            "file.tgz");
    ThirdPartyFileCoordinate component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "ThirdParty", "npm", "bloom", "1.0", "hash001",
            "pkg:npm/bloom@1.0");
    ThirdPartyCoordinateSecurity security =
        tempEntity.newThirdPartyCoordinateSecurity(component, refId, "description", "link", 8.1, "Critical",
            "1.2.0");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(security, security.getRefId(),
        State.EXPLOITABLE.toString(), Justification.REQUIRES_DEPENDENCY.toString(), Response.WILL_NOT_FIX.toString(),
        "some detail");

    restRequest().path(ApiSbomResource.SBOM_VULNERABILITY_ANALYSIS_ANNOTATION_PATH)
        .parameter(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(), refId)
        .body(new ComponentLocator(component.getHash(), component.getPackageUrl()))
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_SBOM_VULNERABILITY_ANALYSIS, null);
    assertVulnerabilityAnalysisAuditData(auditDTO, app, component.getHash(), component.getPackageUrl(), refId);
  }

  @Test
  public void testDeleteVulnerabilityAnalysis_Unauthorized() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    String refId = "CVE-123";

    restRequest().path(ApiSbomResource.SBOM_VULNERABILITY_ANALYSIS_ANNOTATION_PATH)
        .with(unauthorizedUser())
        .parameter(app.getId(), "v1", refId)
        .body(new ComponentLocator("hash001", "purl"))
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_SBOM_VULNERABILITY_ANALYSIS, "unauthorized");
    assertVulnerabilityAnalysisAuditData(auditDTO, app, "hash001", "purl", refId);
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

  private ApiSbomStatusDTO getStatusResponse(String statusUrl) {
    HttpResponse response = await().atMost(10, TimeUnit.SECONDS).until(() -> super.restRequest().path(statusUrl).get(),
        resp -> resp.getStatusCode() == 200);
    return response.getBody(ApiSbomStatusDTO.class);
  }

  private void assertVulnerabilityAnalysisAuditData(
      AuditDTO auditDTO, Application app, String componentHash,
      String componentPurl, String refId)
  {
    assertThat(auditDTO.data).containsEntry("applicationId", app.getId());
    assertThat(auditDTO.data).containsEntry("applicationPublicId", app.getPublicId());
    assertThat(auditDTO.data).containsEntry("applicationName", app.getName());
    assertThat(auditDTO.data).containsEntry("componentHash", componentHash);
    assertThat(auditDTO.data).containsEntry("packageUrl", componentPurl);
    assertThat(auditDTO.data).containsEntry("vulnerabilityReference", refId);
  }
}
