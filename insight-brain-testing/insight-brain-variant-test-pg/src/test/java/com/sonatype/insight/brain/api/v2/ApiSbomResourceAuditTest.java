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
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IqPostgresTest
class ApiSbomResourceAuditTest
    implements AuditTestSupport
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  private User unauthorizedUser;

  @BeforeEach
  void setUp() throws Exception {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    ctx.setFeatures(LicensedFeature.SBOM_MANAGER);
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public com.sonatype.insight.brain.dataaccess.policy.PolicyDAO getPolicyDAO() {
    return ctx.lookup(com.sonatype.insight.brain.dataaccess.policy.PolicyDAO.class);
  }

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.SBOM_RESOURCE_PATH);
  }

  @Test
  void testDeleteSbomVersion_Authorized() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Path zippedBom = mockOriginalSbom(this.getClass(), "third-party-simple-bom.xml",
        ctx.insightWork().getSbomDir(app.getId()).toPath());

    ThirdPartySbomMetadata thirdPartySbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(ctx.daoFactory())
        .withApplicationId(app.getId())
        .withFilename(zippedBom.getFileName().toString())
        .build();

    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VERSION_PATH)
        .parameter(thirdPartySbomMetadata.getApplicationId(), thirdPartySbomMetadata.getSbomVersion())
        .delete();
    ctx.assertResponseStatus(204, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_SBOM_VERSION, null);
    assertThat(auditDTO.data).containsEntry("applicationId", thirdPartySbomMetadata.getApplicationId());
    assertCustomData(auditDTO, "sbomVersion", thirdPartySbomMetadata.getSbomVersion());
  }

  @Test
  void testDeleteSbomVersion_Unauthorized() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ThirdPartySbomMetadata thirdPartySbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(ctx.daoFactory())
        .withApplicationId(app.getId())
        .build();
    HttpResponse response = restRequest().with(unauthorizedUser())
        .path(ApiSbomResource.SBOM_VERSION_PATH)
        .parameter(thirdPartySbomMetadata.getApplicationId(), thirdPartySbomMetadata.getSbomVersion())
        .delete();
    ctx.assertResponseStatus(403, response);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_SBOM_VERSION, "unauthorized");
    assertThat(auditDTO.data).containsEntry("applicationId", thirdPartySbomMetadata.getApplicationId());
  }

  @Test
  void testImportSbom_Authorized() throws Exception {
    ctx.setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.APPLICATION_EVALUATION);
    Application app = ctx.tempEntity().newApplicationWithParent();

    ctx.mockReport("SCAN-ID", "/" + getClass().getSimpleName() + "/report");

    byte[] sbomFile = loadFileFromAssets("/" + getClass().getSimpleName() + "/third-party-simple-bom.xml");
    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_IMPORT_PATH)
        .part("file", "third-party-simple-bom.xml", sbomFile)
        .part("applicationId", app.getId())
        .post();

    ctx.assertResponseStatus(Status.OK.getStatusCode(), response);
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
  void testSaveVulnerabilityAnalysis_Authorized() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = ctx.tempEntity().newThirdPartyFile();
    ctx.tempEntity().newThirdPartyScan(thirdPartyFile);
    String refId = "CVE-123";
    ThirdPartySbomMetadata sbomMetadata =
        ctx.tempEntity()
            .newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(),
                ThirdPartySbomMetadataStatus.ACTIVE, "file.tgz");
    ThirdPartyFileCoordinate component =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyFile, "ThirdParty", "npm", "bloom", "1.0", "hash001",
                "pkg:npm/bloom@1.0");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(component, refId, "description", "link", 8.1, "Critical",
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
  void testSaveVulnerabilityAnalysis_Unauthorized() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
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
  void testDeleteVulnerabilityAnalysis_Authorized() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = ctx.tempEntity().newThirdPartyFile();
    ctx.tempEntity().newThirdPartyScan(thirdPartyFile);
    String refId = "CVE-123";
    ThirdPartySbomMetadata sbomMetadata =
        ctx.tempEntity()
            .newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(),
                ThirdPartySbomMetadataStatus.ACTIVE, "file.tgz");
    ThirdPartyFileCoordinate component =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyFile, "ThirdParty", "npm", "bloom", "1.0", "hash001",
                "pkg:npm/bloom@1.0");
    ThirdPartyCoordinateSecurity security =
        ctx.tempEntity()
            .newThirdPartyCoordinateSecurity(component, refId, "description", "link", 8.1, "Critical",
                "1.2.0");
    ctx.tempEntity()
        .newThirdPartyVulnerabilityExploitabilityExchange(security, security.getRefId(),
            State.EXPLOITABLE.toString(), Justification.REQUIRES_DEPENDENCY.toString(),
            Response.WILL_NOT_FIX.toString(),
            "some detail");

    restRequest().path(ApiSbomResource.SBOM_VULNERABILITY_ANALYSIS_ANNOTATION_PATH)
        .parameter(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion(), refId)
        .body(new ComponentLocator(component.getHash(), component.getPackageUrl()))
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_SBOM_VULNERABILITY_ANALYSIS, null);
    assertVulnerabilityAnalysisAuditData(auditDTO, app, component.getHash(), component.getPackageUrl(), refId);
  }

  @Test
  void testDeleteVulnerabilityAnalysis_Unauthorized() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
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
    HttpResponse response = await().atMost(10, TimeUnit.SECONDS)
        .until(() -> ctx.restRequest().path(statusUrl).get(),
            resp -> resp.getStatusCode() == 200);
    return response.getBody(ApiSbomStatusDTO.class);
  }

  private void assertVulnerabilityAnalysisAuditData(
      AuditDTO auditDTO,
      Application app,
      String componentHash,
      String componentPurl,
      String refId)
  {
    assertThat(auditDTO.data).containsEntry("applicationId", app.getId());
    assertThat(auditDTO.data).containsEntry("applicationPublicId", app.getPublicId());
    assertThat(auditDTO.data).containsEntry("applicationName", app.getName());
    assertThat(auditDTO.data).containsEntry("componentHash", componentHash);
    assertThat(auditDTO.data).containsEntry("packageUrl", componentPurl);
    assertThat(auditDTO.data).containsEntry("vulnerabilityReference", refId);
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
