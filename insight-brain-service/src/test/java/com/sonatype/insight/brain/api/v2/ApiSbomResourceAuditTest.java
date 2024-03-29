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
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.brain.utils.SbomTestsHelper;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

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
    Application app = tempEntity.newApplicationWithParent();
    Files.createDirectories(insightWork.getSbomDir(app.getId()).toPath());

    mockReport("SCAN-ID", "/" + getClass().getSimpleName() + "/report");

    byte[] sbomFile = loadFileFromAssets("/" + getClass().getSimpleName() + "/third-party-simple-bom.xml");
    HttpResponse response = restRequest().path(ApiSbomResource.SBOM_IMPORT_PATH)
        .part("file", "third-party-simple-bom.xml", sbomFile)
        .part("applicationId", app.getId())
        .post();

    assertResponseStatus(Status.OK.getStatusCode(), response);
    ApiThirdPartyScanTicketDTO apiThirdPartyScanTicketDTO = response.getBody(ApiThirdPartyScanTicketDTO.class);
    assertThat(apiThirdPartyScanTicketDTO.statusUrl).startsWith(
        "api/v2/sbom/" + app.getId() + "/status/");

    ApiSbomStatusDTO resultDTO = getStatusResponse(apiThirdPartyScanTicketDTO.statusUrl);
    assertThat(resultDTO.errorMessage).isNull();
    assertThat(resultDTO.isError).isFalse();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.IMPORT_SBOM_VERSION, null);
    assertThat(auditDTO.data).containsEntry("applicationId", app.getId());
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
}
