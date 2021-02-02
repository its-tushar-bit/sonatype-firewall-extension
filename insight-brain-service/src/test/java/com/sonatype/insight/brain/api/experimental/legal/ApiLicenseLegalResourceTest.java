/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalFilterDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiLicenseLegalResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH);
  }

  @Before
  public void setup() throws Exception {
    setFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard_NoResults() throws Exception {
    HttpResponse response =
        restRequest().path(ApiLicenseLegalResource.DASHBOARD_APPLICATIONS_PATH).body(new LicenseLegalFilterDTO()).auth()
            .post();

    assertResponseStatus(200, response);
    List<ApiLicenseLegalApplicationDashboardDTO> result =
        Arrays.asList(response.getBody(ApiLicenseLegalApplicationDashboardDTO[].class));
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard() throws Exception {
    LicenseLegalFilterDTO filter = new LicenseLegalFilterDTO();

    Application application = tempEntity.newApplicationWithParent();
    Tag tag = tempEntity.newTag(application.getOrganizationId(), "Test-Tag");
    tempEntity.newApplicationTag(application.getId(), tag.getId());
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation);

    HttpResponse response =
        restRequest().path(ApiLicenseLegalResource.DASHBOARD_APPLICATIONS_PATH).body(filter).auth().post();

    assertResponseStatus(200, response);
    List<ApiLicenseLegalApplicationDashboardDTO> result =
        Arrays.asList(response.getBody(ApiLicenseLegalApplicationDashboardDTO[].class));
    assertThat(result).isNotEmpty();

    ApiLicenseLegalApplicationDashboardDTO dto = result.get(0);
    assertThat(dto.applicationId).isEqualTo(application.getId());
    assertThat(dto.applicationName).isEqualTo(application.getName());
    assertThat(dto.applicationPublicId).isEqualTo(application.getPublicId());
    assertThat(dto.applicationTagNames).containsExactly(tag.getName());
    assertThat(dto.lastScanTime).isEqualTo(policyEvaluation.getTime().getTime());
    assertThat(dto.reviewCompletedCount).isZero();
    assertThat(dto.reviewTotalCount).isZero();
  }

  @Test
  public void testGetLicenseLegalApplicationReport() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);

    HttpResponse response =
        restRequest().path(ApiLicenseLegalResource.APPLICATION_PATH).parameter(application.getPublicId()).get();

    assertResponseStatus(200, response);
    ApiLicenseLegalApplicationReportDTO
        apiLicenseLegalApplicationReportDTO = response.getBody(ApiLicenseLegalApplicationReportDTO.class);
    assertThat(apiLicenseLegalApplicationReportDTO).isNotNull();
    assertThat(apiLicenseLegalApplicationReportDTO.components).hasSize(14);
    assertThat(apiLicenseLegalApplicationReportDTO.licenseLegalMetadata).hasSize(8);
  }

  @Test
  public void testGetLicenseLegalApplicationReport_NotFound() throws Exception {
    String applicationPublicId = "doesNotExist";

    HttpResponse response =
        restRequest().path(ApiLicenseLegalResource.APPLICATION_PATH).parameter(applicationPublicId).get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Could not find an application with public ID " + applicationPublicId + ".");
  }

  @Test
  public void testGetLicenseLegalComponentReport_ComponentIdentifier() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.METADATA_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_URL);
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.LEGAL_FILE_URL);

    HttpResponse response = restRequest().path(ApiLicenseLegalResource.COMPONENT_PATH)
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

    HttpResponse response = restRequest().path(ApiLicenseLegalResource.COMPONENT_PATH)
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

    HttpResponse response = restRequest().path(ApiLicenseLegalResource.COMPONENT_PATH)
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

    HttpResponse response = restRequest().path(ApiLicenseLegalResource.COMPONENT_PATH)
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

  @Test
  public void testSaveComponentCopyright() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentCopyright componentCopyright1 =
        tempEntity.newComponentCopyright(componentIdentifier, owner.getPublicId(), "legalContentHash1");
    HttpResponse response =
        restRequest().path(ApiLicenseLegalResource.COMPONENT_COPYRIGHT_PATH)
            .parameter(owner.getType().toString(), owner.getPublicId())
            .body(ComponentCopyrightDTO.fromComponentCopyright(componentCopyright1, new ArrayList<>()))
            .post();
    assertResponseStatus(200, response);
    ComponentCopyrightDTO responseDto =
        response.getBody(ComponentCopyrightDTO.class);
    assertThat(responseDto).isNotNull();
    assertThat(responseDto.getComponentIdentifier().toComponentIdentifier())
        .isEqualTo(componentIdentifier);
  }

  private void mockReport(PolicyEvaluation evaluation) {
    try {
      Path reportDir = getCLMServer().getInstance(InsightWork.class)
          .getReportDir(evaluation.getApplicationId(), evaluation.getScanId()).toPath();
      Files.createDirectories(reportDir);
      Files.write(reportDir.resolve("report.zip"), Collections.singletonList("report.zip"));
      File reportFile = reportDir.resolve("report.zip").toFile();
      try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(reportFile))) {
        zos.putNextEntry(new ZipEntry("index.html"));
      }
      String[] filenames = {
          Report.BOM_JSON_FILENAME, Report.SECURITY_JSON_FILENAME, Report.LICENSES_JSON_FILENAME,
          Report.DATA_JSON_FILENAME, Report.DEPENDENCIES_JSON_FILENAME
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
