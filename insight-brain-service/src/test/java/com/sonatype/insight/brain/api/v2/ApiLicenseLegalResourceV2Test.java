/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalHdsService;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiLicenseLegalResourceV2Test
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH_V2);
  }

  @Test
  public void testGetLicenseLegalApplicationReport() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);

    HttpResponse response =
        restRequest().path(DefaultApiLicenseLegalResourceV2.APPLICATION_PATH)
            .parameter(application.getId())
            .get();

    assertResponseStatus(200, response);
    ApiLicenseLegalApplicationReportDTO
        apiLicenseLegalApplicationReportDTO = response.getBody(ApiLicenseLegalApplicationReportDTO.class);
    assertThat(apiLicenseLegalApplicationReportDTO).isNotNull();
    assertThat(apiLicenseLegalApplicationReportDTO.components).hasSize(14);
    assertThat(apiLicenseLegalApplicationReportDTO.licenseLegalMetadata).hasSize(8);
  }

  @Test
  public void testGetLicenseLegalApplicationHTMLReport() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);

    HttpResponse response =
        restRequest().path(DefaultApiLicenseLegalResourceV2.APPLICATION_REPORT_PATH)
            .parameter(application.getId(), BuildStageType.ID)
            .get();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains(application.getPublicId());
  }

  @Test
  public void testGetLicenseLegalApplicationHTMLReport_NoStage() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);

    HttpResponse response =
        restRequest().path(DefaultApiLicenseLegalResourceV2.APPLICATION_REPORT_PATH)
            .parameter(application.getId(), ReleaseStageType.ID)
            .get();

    assertResponseStatus(404, response);
  }

  @Test
  public void testGetLicenseLegalApplicationReport_NotFound() throws Exception {
    String applicationPublicId = "doesNotExist";

    HttpResponse response =
        restRequest().path(DefaultApiLicenseLegalResourceV2.APPLICATION_PATH)
            .parameter(applicationPublicId)
            .get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Could not find an application with ID " + applicationPublicId + ".");
  }

  @Test
  public void testGetLicenseLegalComponentReport_ComponentIdentifier() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);

    HttpResponse response = restRequest().path(DefaultApiLicenseLegalResourceV2.COMPONENT_PATH)
        .parameter(owner.getType().toString(), owner.getPublicId())
        .query("componentIdentifier", componentIdentifier)
        .get();

    assertResponseStatus(200, response);
    ApiLicenseLegalComponentReportDTO apiLicenseLegalComponentDTO =
        response.getBody(ApiLicenseLegalComponentReportDTO.class);
    assertThat(apiLicenseLegalComponentDTO).isNotNull();
    assertThat(apiLicenseLegalComponentDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(componentIdentifier);
  }

  @Test
  public void testGetLicenseLegalComponentReport_PackageUrl() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);

    HttpResponse response = restRequest().path(DefaultApiLicenseLegalResourceV2.COMPONENT_PATH)
        .parameter(owner.getType().toString(), owner.getPublicId())
        .query("packageUrl", PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl())
        .get();

    assertResponseStatus(200, response);
    ApiLicenseLegalComponentReportDTO apiLicenseLegalComponentDTO =
        response.getBody(ApiLicenseLegalComponentReportDTO.class);
    assertThat(apiLicenseLegalComponentDTO).isNotNull();
    assertThat(apiLicenseLegalComponentDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(componentIdentifier);
  }

  @Test
  public void testGetLicenseLegalComponentReport_Hash() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String hash = "hash";
    tempEntity.newApplicationComponent(owner.getId(), BuildStageType.ID, hash, componentIdentifier);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);

    HttpResponse response = restRequest().path(DefaultApiLicenseLegalResourceV2.COMPONENT_PATH)
        .parameter(owner.getType().toString(), owner.getPublicId())
        .query("hash", hash)
        .get();

    assertResponseStatus(200, response);
    ApiLicenseLegalComponentReportDTO apiLicenseLegalComponentDTO =
        response.getBody(ApiLicenseLegalComponentReportDTO.class);
    assertThat(apiLicenseLegalComponentDTO).isNotNull();
    assertThat(apiLicenseLegalComponentDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(componentIdentifier);
  }

  @Test
  public void testGetLicenseLegalComponentReport_ThirdParty() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("name", "glibc");
    coordinates.put(ComponentIdentifier.VERSION, "2.24-11+deb9u3");
    ComponentIdentifier componentIdentifier = new ComponentIdentifier("debian-9", coordinates);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);
    String scanId = "scanId";
    mockThirdPartyReport(owner.getId(), scanId);

    HttpResponse response = restRequest().path(DefaultApiLicenseLegalResourceV2.COMPONENT_PATH)
        .parameter(owner.getType().toString(), owner.getPublicId())
        .query("componentIdentifier", componentIdentifier)
        .query("identificationSource", IdentificationSource.CLAIR.getId())
        .query("scanId", scanId)
        .get();

    assertResponseStatus(200, response);
    ApiLicenseLegalComponentReportDTO apiLicenseLegalComponentDTO =
        response.getBody(ApiLicenseLegalComponentReportDTO.class);
    assertThat(apiLicenseLegalComponentDTO).isNotNull();
    assertThat(apiLicenseLegalComponentDTO.component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(componentIdentifier);
  }

  private void mockThirdPartyReport(String applicationId, String scanId) {
    try {
      Path reportDir = getCLMServer().getInstance(InsightWork.class).getReportDir(applicationId, scanId).toPath();
      Files.createDirectories(reportDir);
      Files.write(reportDir.resolve("report.zip"), Collections.singletonList("report.zip"));
      File reportFile = reportDir.resolve("report.zip").toFile();
      String[] filenames = {
          ThirdPartyComponentDAO.THIRD_PARTY_BOM_JSON_FILENAME,
          ThirdPartyComponentDAO.THIRD_PARTY_LICENSE_JSON_FILENAME,
          ThirdPartyComponentDAO.THIRD_PARTY_SECURITY_JSON_FILENAME
      };
      for (String filename : filenames) {
        File file = Report.getCacheFile(reportFile, filename);
        FileUtils.copyURLToFile(getClass().getResource("/" + getClass().getSimpleName() + "/report/" + filename), file);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
