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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardResultDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightWithOwnerDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightFilePathDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightFilePathsDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalFilterDTO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentLegalFileDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentFilePathsDTO;
import com.sonatype.insight.license.dto.model.LegalCommentFilesDTO;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.collect.ImmutableSet;
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
    LicenseLegalFilterDTO filter = new LicenseLegalFilterDTO();
    filter.page = 1;
    filter.pageSize = 10;

    HttpResponse response =
        restRequest().path(ApiLicenseLegalResource.DASHBOARD_APPLICATIONS_PATH).body(filter).auth().post();

    assertResponseStatus(200, response);
    ApiLicenseLegalApplicationDashboardResultDTO result =
        response.getBody(ApiLicenseLegalApplicationDashboardResultDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.results).isEmpty();
    assertThat(result.totalResultsCount).isZero();
  }

  @Test
  public void testGetLicenseLegalApplicationsDashboard() throws Exception {
    LicenseLegalFilterDTO filter = new LicenseLegalFilterDTO();
    filter.page = 1;
    filter.pageSize = 10;

    Application application = tempEntity.newApplicationWithParent();
    Tag tag = tempEntity.newTag(application.getOrganizationId(), "Test-Tag");
    tempEntity.newApplicationTag(application.getId(), tag.getId());
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, tempEntity.uuid());
    mockReport(policyEvaluation);

    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.METADATA_URL);

    HttpResponse response =
        restRequest().path(ApiLicenseLegalResource.DASHBOARD_APPLICATIONS_PATH).body(filter).auth().post();

    assertResponseStatus(200, response);
    ApiLicenseLegalApplicationDashboardResultDTO result =
        response.getBody(ApiLicenseLegalApplicationDashboardResultDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.results).isNotEmpty();
    assertThat(result.totalResultsCount).isEqualTo(1);

    ApiLicenseLegalApplicationDashboardDTO dto = result.results.get(0);
    assertThat(dto.applicationId).isEqualTo(application.getId());
    assertThat(dto.applicationName).isEqualTo(application.getName());
    assertThat(dto.applicationPublicId).isEqualTo(application.getPublicId());
    assertThat(dto.applicationTagNames).containsExactly(tag.getName());
    assertThat(dto.lastScanTime).isEqualTo(policyEvaluation.getTime().getTime());
    assertThat(dto.componentsReviewedCount).isZero();
    assertThat(dto.componentsTotalCount).isZero();
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
        tempEntity.newComponentCopyright(componentIdentifier, owner.getId(), "legalContentHash1");
    HttpResponse response =
        restRequest().path(ApiLicenseLegalResource.COMPONENT_COPYRIGHT_PATH)
            .parameter(owner.getType(), owner.getPublicId())
            .body(ComponentCopyrightDTO.fromComponentCopyright(componentCopyright1, new ArrayList<>()))
            .post();
    assertResponseStatus(200, response);
    ComponentCopyrightDTO responseDto =
        response.getBody(ComponentCopyrightDTO.class);
    assertThat(responseDto).isNotNull();
    assertThat(responseDto.getComponentIdentifier().toComponentIdentifier())
        .isEqualTo(componentIdentifier);
  }

  @Test
  public void testSaveComponentLegalFile() throws Exception {
    Owner owner = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFile componentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, owner.getId(), LegalFileType.NOTICE, "legalContentHash");
    ComponentLegalFileDTO bodyDto = new ComponentLegalFileDTO(componentLegalFile, Collections.emptyList());
    bodyDto.setLastUpdatedByUsername(null);
    bodyDto.setLastUpdatedAt(null);
    Date now = new Date();

    HttpResponse response = restRequest().path(ApiLicenseLegalResource.COMPONENT_LEGAL_FILE_PATH)
        .parameter(owner.getType(), owner.getPublicId())
        .body(bodyDto)
        .post();

    assertResponseStatus(200, response);
    ComponentLegalFileDTO responseDto = response.getBody(ComponentLegalFileDTO.class);
    assertThat(responseDto).usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(bodyDto);
    assertThat(responseDto.getLastUpdatedAt()).isAfterOrEqualTo(now);
    assertThat(responseDto.getLastUpdatedByUsername()).isEqualTo(User.ADMIN_USERNAME);
    assertThat(responseDto.getId()).isNotNull();
    assertThat(new ComponentLegalFileDAO().getById(responseDto.getId())).isNotNull();
  }

  @Test
  public void testSaveComponentObligationAttribution() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttributionDTO bodyDto = new ComponentObligationAttributionDTO();
    bodyDto.setComponentIdentifier(
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    bodyDto.setContent("content");

    HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_ATTRIBUTION_PATH)
        .parameter(application.getType(), application.getPublicId())
        .body(bodyDto)
        .post();

    assertResponseStatus(200, response);
    ComponentObligationAttributionDTO responseDto = response.getBody(ComponentObligationAttributionDTO.class);
    assertThat(responseDto).usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(bodyDto);
    assertThat(responseDto.getId()).isNotNull();
    assertThat(new ComponentObligationAttributionDAO().getById(responseDto.getId())).isNotNull();
  }

  @Test
  public void testDeleteComponentObligationAttribution() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), application.getId(), null, "content",
        ComponentLegalService.NOT_IMPLEMENTED);

    HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_ATTRIBUTION_DELETE_PATH)
        .parameter(componentObligationAttribution.getId())
        .delete();

    assertResponseStatus(204, response);
    assertThat(new ComponentObligationAttributionDAO().getById(componentObligationAttribution.getId())).isNull();
  }

  @Test
  public void testGetComponentObligationAttribution() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), application.getId(), "obligationName", "content",
        ComponentLegalService.NOT_IMPLEMENTED);

    HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_ATTRIBUTION_PATH)
        .parameter(application.getType(), application.getPublicId())
        .query("componentIdentifier", componentObligationAttribution.getComponentIdentifier())
        .query("obligationName", componentObligationAttribution.getObligationName())
        .get();

    assertResponseStatus(200, response);
    List<ComponentObligationAttributionDTO> responseBody =
        Arrays.asList(response.getBody(ComponentObligationAttributionDTO[].class));
    assertThat(responseBody).extracting(ComponentObligationAttributionDTO::getId)
        .containsExactly(componentObligationAttribution.getId());
  }

  @Test
  public void testSaveComponentObligation() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ApiLicenseLegalObligationDTO bodyDto = new ApiLicenseLegalObligationDTO();
    bodyDto.setComponentIdentifier(ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v")));
    bodyDto.setName("obligationName");
    bodyDto.setStatus(ObligationStatus.OPEN);

    HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_PATH)
        .parameter(application.getType(), application.getPublicId())
        .body(bodyDto)
        .post();

    assertResponseStatus(200, response);
    ApiLicenseLegalObligationDTO responseDto = response.getBody(ApiLicenseLegalObligationDTO.class);
    assertThat(responseDto).usingRecursiveComparison().ignoringExpectedNullFields().isEqualTo(bodyDto);
    assertThat(responseDto.getId()).isNotNull();
    assertThat(new ComponentObligationDAO().getById(responseDto.getId())).isNotNull();
  }

  @Test
  public void testDeleteComponentObligation() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), application.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);

    HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_DELETE_PATH)
        .parameter(componentObligation.getId())
        .delete();

    assertResponseStatus(204, response);
    assertThat(new ComponentObligationDAO().getById(componentObligation.getId())).isNull();
  }

  @Test
  public void testGetComponentObligation() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), application.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);

    HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_PATH)
        .parameter(application.getType(), application.getPublicId())
        .query("componentIdentifier", componentObligation.getComponentIdentifier())
        .query("obligationName", componentObligation.getObligationName())
        .get();

    assertResponseStatus(200, response);
    ApiLicenseLegalObligationDTO responseDto = response.getBody(ApiLicenseLegalObligationDTO.class);
    assertThat(responseDto).isNotNull();
    assertThat(responseDto.getId()).isEqualTo(componentObligation.getId());
  }

  @Test
  public void testGetComponentCopyright() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    ComponentCopyright componentCopyright =
        tempEntity.newComponentCopyright(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"),
            organization.getId(), "lch");

    HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_COPYRIGHT_PATH)
        .parameter(application.getType(), application.getPublicId())
        .query("componentIdentifier", componentCopyright.getComponentIdentifier())
        .get();

    assertResponseStatus(200, response);
    ComponentCopyrightWithOwnerDTO componentCopyrightWithOwnerDTO =
        response.getBody(ComponentCopyrightWithOwnerDTO.class);
    assertThat(componentCopyrightWithOwnerDTO).isNotNull();
    assertThat(componentCopyrightWithOwnerDTO.getComponentCopyrightDTO().getId()).isEqualTo(componentCopyright.getId());
    assertThat(componentCopyrightWithOwnerDTO.getOwnerId()).isEqualTo(organization.getId());
  }

  @Test
  public void testGetComponentLegalFile_License() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFile componentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, app.getId(), LegalFileType.LICENSE, "legalContentHash");
    LegalFileOverride licenseOverride = tempEntity.newLegalFileOverride(null, "hash1",
        "content1", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());

    HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_LEGAL_FILE_PATH)
        .parameter(app.getType(), app.getPublicId())
        .query("componentIdentifier", componentIdentifier)
        .query("legalFileType", LegalFileType.LICENSE.toString())
        .get();

    assertResponseStatus(200, response);
    ComponentLegalFileDTO componentLegalFileDTO = response.getBody(ComponentLegalFileDTO.class);
    assertThat(componentLegalFileDTO).isNotNull();
    assertThat(componentLegalFile.getId()).isEqualTo(componentLegalFile.getId());
    assertThat(componentLegalFileDTO.getLegalFileOverrides()).hasSize(1);
    assertThat(componentLegalFileDTO.getLegalFileOverrides().get(0).getId()).isEqualTo(licenseOverride.getId());
  }

  @Test
  public void testGetComponentLegalFile_Notice() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFile componentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, app.getId(), LegalFileType.NOTICE, "legalContentHash");
    LegalFileOverride noticeOverride = tempEntity.newLegalFileOverride(null, "hash1", "content1",
        ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());

    HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_LEGAL_FILE_PATH)
        .parameter(app.getType(), app.getPublicId(), LegalFileType.NOTICE.toString())
        .query("componentIdentifier", componentIdentifier)
        .query("legalFileType", LegalFileType.NOTICE.toString())
        .get();

    assertResponseStatus(200, response);
    ComponentLegalFileDTO componentLegalFileDTO = response.getBody(ComponentLegalFileDTO.class);
    assertThat(componentLegalFileDTO).isNotNull();
    assertThat(componentLegalFile.getId()).isEqualTo(componentLegalFile.getId());
    assertThat(componentLegalFileDTO.getLegalFileOverrides()).hasSize(1);
    assertThat(componentLegalFileDTO.getLegalFileOverrides().get(0).getId()).isEqualTo(noticeOverride.getId());
  }

  @Test
  public void testGetCopyrightFilePaths() throws Exception {
    final ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    final LegalCommentFilesDTO commentFilesDTO1 = new LegalCommentFilesDTO();
    commentFilesDTO1.setContent("Content 1");
    commentFilesDTO1.setCopyrightContentHashes(ImmutableSet.of("copyright hash 1", "copyright hash 2"));
    commentFilesDTO1.setFilePaths(ImmutableSet.of("path1/file1", "path2/file1"));

    final LegalCommentFilesDTO commentFilesDTO2 = new LegalCommentFilesDTO();
    commentFilesDTO2.setContent("Content 2");
    commentFilesDTO2.setCopyrightContentHashes(ImmutableSet.of("copyright hash 3", "copyright hash 2"));
    commentFilesDTO2.setFilePaths(ImmutableSet.of("path2/file2", "path1/file1", "path2/file1"));

    final ComponentLegalCommentFilePathsDTO hdsResponse = new ComponentLegalCommentFilePathsDTO();
    hdsResponse.setHash("hash");
    hdsResponse.setComponentIdentifier(mavenIdentifier);
    hdsResponse.setComments(ImmutableSet.of(commentFilesDTO1, commentFilesDTO2));

    hdsRespondWith(ImmutableSet.of(hdsResponse)).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_FILE_PATHS_URL);

    final HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_COPYRIGHT_FILEPATHS)
        .parameter(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, "hash", "copyright hash 2")
        .query("componentIdentifier", mavenIdentifier)
        .query("pageStart", 0)
        .query("pageLength", 15)
        .get();

    assertResponseStatus(200, response);
    final CopyrightFilePathsDTO filePaths = response.getBody(CopyrightFilePathsDTO.class);

    assertThat(filePaths.getFilePaths()).hasSize(3).containsExactly(
        new CopyrightFilePathDTO("path1/file1", 2),
        new CopyrightFilePathDTO("path2/file1", 2),
        new CopyrightFilePathDTO("path2/file2", 1));
  }

  @Test
  public void testGetCopyrightContextContent() throws Exception {
    final ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    final LegalCommentFilesDTO commentFilesDTO1 = new LegalCommentFilesDTO();
    commentFilesDTO1.setContent("Content 1");
    commentFilesDTO1.setCopyrightContentHashes(ImmutableSet.of("copyright hash 1", "copyright hash 2"));
    commentFilesDTO1.setFilePaths(ImmutableSet.of("path1/file1", "path2/file1"));

    final LegalCommentFilesDTO commentFilesDTO2 = new LegalCommentFilesDTO();
    commentFilesDTO2.setContent("Content 2");
    commentFilesDTO2.setCopyrightContentHashes(ImmutableSet.of("copyright hash 3", "copyright hash 2"));
    commentFilesDTO2.setFilePaths(ImmutableSet.of("path2/file2", "path1/file1", "path2/file1"));

    final ComponentLegalCommentFilePathsDTO hdsResponse = new ComponentLegalCommentFilePathsDTO();
    hdsResponse.setHash("hash");
    hdsResponse.setComponentIdentifier(mavenIdentifier);
    hdsResponse.setComments(ImmutableSet.of(commentFilesDTO1, commentFilesDTO2));

    hdsRespondWith(ImmutableSet.of(hdsResponse)).atUri(ApiLicenseLegalHdsService.LEGAL_COMMENT_FILE_PATHS_URL);

    final HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_COPYRIGHT_FILEPATH_CONTEXT)
        .parameter(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, "hash", "copyright hash 2",
            "path2/file1")
        .query("componentIdentifier", mavenIdentifier)
        .get();

    assertResponseStatus(200, response);
    final Collection<String> contents = response.getBodySet(String.class);

    assertThat(contents).containsExactlyInAnyOrder("Content 1", "Content 2");
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
