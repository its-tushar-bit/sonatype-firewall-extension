/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationDashboardResultDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDashboardDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentDashboardResultDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalObligationDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentCopyrightWithOwnerDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentLegalFileDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ComponentObligationAttributionDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightFilePathDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.CopyrightFilePathsDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalApplicationComponentsFilterDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseLegalFilterDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.LicenseObligationReviewStatus;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.legal.ComponentLegalFileDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
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
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.dto.model.ComponentLegalCommentFilePathsDTO;
import com.sonatype.insight.license.dto.model.LegalCommentFilesDTO;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiLicenseLegalResourceTest
    extends AbstractResourceTest
{
  private ComponentLegalFileDAO componentLegalFileDAO;

  private ComponentObligationDAO componentObligationDAO;

  private ComponentObligationAttributionDAO componentObligationAttributionDAO;

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.LICENSE_LEGAL_RESOURCE_PATH);
  }

  @Before
  public void setup() throws Exception {
    componentLegalFileDAO = lookup(ComponentLegalFileDAO.class);
    componentObligationDAO = lookup(ComponentObligationDAO.class);
    componentObligationAttributionDAO = lookup(ComponentObligationAttributionDAO.class);
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
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());

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
  public void testGetLicenseLegalComponentsDashboard_NoResults() throws Exception {
    LicenseLegalFilterDTO filter = new LicenseLegalFilterDTO();
    filter.page = 1;
    filter.pageSize = 10;

    HttpResponse response =
        restRequest().path(ApiLicenseLegalResource.DASHBOARD_COMPONENTS_PATH).body(filter).auth().post();

    assertResponseStatus(200, response);
    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        response.getBody(ApiLicenseLegalComponentDashboardResultDTO.class);
    assertThat(resultDto).isNotNull();
    assertThat(resultDto.results).isEmpty();
    assertThat(resultDto.totalResultsCount).isZero();
  }

  @Test
  public void testGetLicenseLegalComponentsDashboard() throws Exception {
    LicenseLegalFilterDTO filter = new LicenseLegalFilterDTO();
    filter.page = 1;
    filter.pageSize = 10;

    Application application = tempEntity.newApplicationWithParent();
    Tag tag = tempEntity.newTag(application.getOrganizationId(), "Test-Tag");
    tempEntity.newApplicationTag(application.getId(), tag.getId());
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    mockReport(policyEvaluation, getClass().getSimpleName());
    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.METADATA_URL);
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ApplicationComponent applicationComponent =
        tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, "hash1", componentIdentifier);
    tempEntity.newApplicationComponentLicense(applicationComponent.getId(), "MIT");

    HttpResponse response =
        restRequest().path(ApiLicenseLegalResource.DASHBOARD_COMPONENTS_PATH).body(filter).auth().post();

    assertResponseStatus(200, response);
    ApiLicenseLegalComponentDashboardResultDTO resultDto =
        response.getBody(ApiLicenseLegalComponentDashboardResultDTO.class);
    assertThat(resultDto).isNotNull();
    assertThat(resultDto.results).isNotEmpty();
    assertThat(resultDto.totalResultsCount).isEqualTo(1);

    ApiLicenseLegalComponentDashboardDTO dto = resultDto.results.get(0);
    assertThat(dto.applicationOccurrences).isEqualTo(1);
    assertThat(dto.displayName).isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
    assertThat(dto.hash).isEqualTo("hash1");
    assertThat(dto.licenses).extracting(dt -> dt.licenseName).containsExactly("MIT");
    assertThat(dto.reviewCompletedCount).isZero();
    assertThat(dto.reviewTotalCount).isZero();
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_ApplicationNotFound() throws Exception {
    LicenseLegalApplicationComponentsFilterDTO filter = new LicenseLegalApplicationComponentsFilterDTO();

    HttpResponse response = restRequest().path(ApiLicenseLegalResource.DASHBOARD_APPLICATION_PATH)
        .parameter("fake-app-id")
        .body(filter)
        .post();

    assertResponseStatus(404, response);
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_NoResults() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    LicenseLegalApplicationComponentsFilterDTO filter = new LicenseLegalApplicationComponentsFilterDTO();

    HttpResponse response =
        restRequest().path(ApiLicenseLegalResource.DASHBOARD_APPLICATION_PATH)
            .parameter(application.getPublicId())
            .body(filter)
            .post();

    assertResponseStatus(200, response);
    List<ApiLicenseLegalApplicationComponentDTO> result =
        Arrays.asList(response.getBody(ApiLicenseLegalApplicationComponentDTO[].class));
    assertThat(result).isEmpty();
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard() throws Exception {
    doTestGetLicenseLegalApplicationDashboard(new LicenseLegalApplicationComponentsFilterDTO());
  }

  @Test
  public void testGetLicenseLegalApplicationDashboard_WithoutBody() throws Exception {
    doTestGetLicenseLegalApplicationDashboard(null);
  }

  private void doTestGetLicenseLegalApplicationDashboard(
      LicenseLegalApplicationComponentsFilterDTO filter) throws Exception
  {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ApplicationComponent applicationComponent =
        tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, "hash", componentIdentifier);
    tempEntity.newApplicationComponentLicense(applicationComponent.getId(), "MIT");

    hdsRespondWith("[]").atUri(ApiLicenseLegalHdsService.METADATA_URL);

    HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.DASHBOARD_APPLICATION_PATH)
        .parameter(application.getPublicId())
        .body(filter)
        .auth()
        .post();

    assertResponseStatus(200, response);
    List<ApiLicenseLegalApplicationComponentDTO> result =
        Arrays.asList(response.getBody(ApiLicenseLegalApplicationComponentDTO[].class));
    assertThat(result).hasSize(1);

    ApiLicenseLegalApplicationComponentDTO dto = result.get(0);
    assertThat(dto.hash).isEqualTo(applicationComponent.getHash());
    assertThat(dto.displayName).isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
    assertThat(dto.licenses.iterator().next().licenseId).isEqualTo("MIT");
    assertThat(dto.reviewCompletedCount).isZero();
    assertThat(dto.reviewTotalCount).isZero();
    assertThat(dto.reviewStatus).isEqualTo(LicenseObligationReviewStatus.COMPLETED);
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
    assertThat(responseDto.getPackageUrl())
        .isEqualTo(PackageUrlIdentifier.toPackageUrl(componentIdentifier));
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
    assertThat(componentLegalFileDAO.getById(responseDto.getId())).isNotNull();
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
    assertThat(componentObligationAttributionDAO.getById(responseDto.getId())).isNotNull();
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
    assertThat(componentObligationAttributionDAO.getById(componentObligationAttribution.getId())).isNull();
  }

  @Test
  public void testGetComponentObligationAttribution() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), application.getId(),
        "obligationName", "content", ComponentLegalService.NOT_IMPLEMENTED);

    HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_ATTRIBUTION_PATH)
        .parameter(application.getType(), application.getPublicId())
        .query("componentIdentifier", componentObligationAttribution.getComponentIdentifier())
        .query("obligationName", componentObligationAttribution.getObligationName())
        .get();

    String packageUrl = PackageUrlIdentifier.toPackageUrl(componentObligationAttribution.getComponentIdentifier());

    HttpResponse response2 = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_ATTRIBUTION_PATH)
        .parameter(application.getType(), application.getPublicId())
        .query("packageUrl", packageUrl)
        .query("obligationName", componentObligationAttribution.getObligationName())
        .get();

    assertResponseStatus(200, response);
    assertResponseStatus(200, response2);
    List<ComponentObligationAttributionDTO> responseBody =
        Arrays.asList(response.getBody(ComponentObligationAttributionDTO[].class));
    List<ComponentObligationAttributionDTO> responseBody2 =
        Arrays.asList(response2.getBody(ComponentObligationAttributionDTO[].class));
    assertThat(responseBody).extracting(ComponentObligationAttributionDTO::getId)
        .containsExactly(componentObligationAttribution.getId());
    assertThat(responseBody).isEqualTo(responseBody2);
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
    assertThat(componentObligationDAO.getById(responseDto.getId())).isNotNull();
  }

  @Test
  public void testDeleteComponentObligation() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), application.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);

    HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_DELETE_PATH)
        .query("componentObligationId", componentObligation.getId())
        .delete();

    assertResponseStatus(204, response);
    assertThat(componentObligationDAO.getById(componentObligation.getId())).isNull();
  }

  @Test
  public void testDeleteComponentObligation_multiple() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligation componentObligation1 = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), application.getId(), "obligationName", null,
        ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);

    ComponentObligation componentObligation2 = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), application.getId(), "otherObligationName", null,
        ObligationStatus.FULFILLED, ComponentLegalService.NOT_IMPLEMENTED);

    HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_DELETE_PATH)
        .query("componentObligationId", componentObligation1.getId(), componentObligation2.getId())
        .delete();

    assertResponseStatus(204, response);
    assertThat(componentObligationDAO.getById(componentObligation1.getId())).isNull();
    assertThat(componentObligationDAO.getById(componentObligation2.getId())).isNull();
  }

  @Test
  public void testGetComponentObligation() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentObligation componentObligation = tempEntity.newComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), application.getId(),
        "obligationName", null, ObligationStatus.OPEN, ComponentLegalService.NOT_IMPLEMENTED);

    HttpResponse response = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_PATH)
        .parameter(application.getType(), application.getPublicId())
        .query("componentIdentifier", componentObligation.getComponentIdentifier())
        .query("obligationName", componentObligation.getObligationName())
        .get();

    String packageUrl = PackageUrlIdentifier.toPackageUrl(componentObligation.getComponentIdentifier());

    HttpResponse response2 = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_OBLIGATION_PATH)
        .parameter(application.getType(), application.getPublicId())
        .query("packageUrl", packageUrl)
        .query("obligationName", componentObligation.getObligationName())
        .get();

    assertResponseStatus(200, response);
    assertResponseStatus(200, response2);
    ApiLicenseLegalObligationDTO responseDto = response.getBody(ApiLicenseLegalObligationDTO.class);
    ApiLicenseLegalObligationDTO responseDto2 = response2.getBody(ApiLicenseLegalObligationDTO.class);
    assertThat(responseDto).isNotNull();
    assertThat(responseDto).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .isEqualTo(responseDto2);
    assertThat(responseDto.getPackageUrl()).isEqualTo(packageUrl);
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

    String packageUrl = PackageUrlIdentifier.toPackageUrl(componentCopyright.getComponentIdentifier());

    HttpResponse response2 = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_COPYRIGHT_PATH)
        .parameter(application.getType(), application.getPublicId())
        .query("packageUrl", packageUrl)
        .get();

    assertResponseStatus(200, response);
    assertResponseStatus(200, response2);
    ComponentCopyrightWithOwnerDTO componentCopyrightWithOwnerDTO =
        response.getBody(ComponentCopyrightWithOwnerDTO.class);
    ComponentCopyrightWithOwnerDTO componentCopyrightWithOwnerDTO2 =
        response.getBody(ComponentCopyrightWithOwnerDTO.class);
    assertThat(componentCopyrightWithOwnerDTO).isNotNull();
    assertThat(componentCopyrightWithOwnerDTO).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .isEqualTo(componentCopyrightWithOwnerDTO2);
    assertThat(componentCopyrightWithOwnerDTO.getComponentCopyrightDTO().getId()).isEqualTo(componentCopyright.getId());
    assertThat(componentCopyrightWithOwnerDTO.getComponentCopyrightDTO().getId())
        .isEqualTo(componentCopyrightWithOwnerDTO2.getComponentCopyrightDTO().getId());
    assertThat(componentCopyrightWithOwnerDTO.getOwnerId()).isEqualTo(organization.getId());
  }

  @Test
  public void testGetComponentLegalFile_License() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");
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

    String packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);

    HttpResponse response2 = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_LEGAL_FILE_PATH)
        .parameter(app.getType(), app.getPublicId())
        .query("packageUrl", packageUrl)
        .query("legalFileType", LegalFileType.LICENSE.toString())
        .get();

    assertResponseStatus(200, response);
    assertResponseStatus(200, response2);
    ComponentLegalFileDTO componentLegalFileDTO = response.getBody(ComponentLegalFileDTO.class);
    ComponentLegalFileDTO componentLegalFileDTO2 = response.getBody(ComponentLegalFileDTO.class);
    assertThat(componentLegalFileDTO).isNotNull();
    assertThat(componentLegalFileDTO).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .isEqualTo(componentLegalFileDTO2);
    assertThat(componentLegalFile.getId()).isEqualTo(componentLegalFile.getId());
    assertThat(componentLegalFileDTO.getLegalFileOverrides()).hasSize(1);
    assertThat(componentLegalFileDTO.getLegalFileOverrides().get(0).getId()).isEqualTo(licenseOverride.getId());
    assertThat(componentLegalFileDTO.getPackageUrl()).isEqualTo(packageUrl);
  }

  @Test
  public void testGetComponentLegalFile_Notice() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");
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

    String packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);

    HttpResponse response2 = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_LEGAL_FILE_PATH)
        .parameter(app.getType(), app.getPublicId(), LegalFileType.NOTICE.toString())
        .query("packageUrl", packageUrl)
        .query("legalFileType", LegalFileType.NOTICE.toString())
        .get();

    assertResponseStatus(200, response);
    assertResponseStatus(200, response2);
    ComponentLegalFileDTO componentLegalFileDTO = response.getBody(ComponentLegalFileDTO.class);
    ComponentLegalFileDTO componentLegalFileDTO2 = response.getBody(ComponentLegalFileDTO.class);
    assertThat(componentLegalFileDTO).isNotNull();
    assertThat(componentLegalFileDTO).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .isEqualTo(componentLegalFileDTO2);
    assertThat(componentLegalFile.getId()).isEqualTo(componentLegalFile.getId());
    assertThat(componentLegalFileDTO.getLegalFileOverrides()).hasSize(1);
    assertThat(componentLegalFileDTO.getLegalFileOverrides().get(0).getId()).isEqualTo(noticeOverride.getId());
    assertThat(componentLegalFileDTO.getPackageUrl()).isEqualTo(packageUrl);
  }

  @Test
  public void testGetCopyrightFilePaths() throws Exception {
    final ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");

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

    String packageUrl = PackageUrlIdentifier.toPackageUrl(mavenIdentifier);

    final HttpResponse response2 = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_COPYRIGHT_FILEPATHS)
        .parameter(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, "hash", "copyright hash 2")
        .query("packageUrl", packageUrl)
        .query("pageStart", 0)
        .query("pageLength", 15)
        .get();

    assertResponseStatus(200, response);
    assertResponseStatus(200, response2);
    final CopyrightFilePathsDTO filePaths = response.getBody(CopyrightFilePathsDTO.class);
    final CopyrightFilePathsDTO filePaths2 = response.getBody(CopyrightFilePathsDTO.class);

    assertThat(filePaths.getFilePaths()).hasSize(3)
        .containsExactly(
            new CopyrightFilePathDTO("path1/file1", 2),
            new CopyrightFilePathDTO("path2/file1", 2),
            new CopyrightFilePathDTO("path2/file2", 1));

    assertThat(filePaths2.getFilePaths()).hasSize(3)
        .containsExactly(
            new CopyrightFilePathDTO("path1/file1", 2),
            new CopyrightFilePathDTO("path2/file1", 2),
            new CopyrightFilePathDTO("path2/file2", 1));
  }

  @Test
  public void testGetCopyrightContextContent() throws Exception {
    final ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");

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
        .parameter(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, "hash", "copyright hash 2")
        .query("componentIdentifier", mavenIdentifier)
        .query("filePath", "path2/file1")
        .get();

    String packageUrl = PackageUrlIdentifier.toPackageUrl(mavenIdentifier);

    final HttpResponse response2 = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_COPYRIGHT_FILEPATH_CONTEXT)
        .parameter(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, "hash", "copyright hash 2")
        .query("packageUrl", packageUrl)
        .query("filePath", "path2/file1")
        .get();

    assertResponseStatus(200, response);
    assertResponseStatus(200, response2);
    final Collection<String> contents = response.getBodySet(String.class);
    final Collection<String> contents2 = response2.getBodySet(String.class);

    assertThat(contents).containsExactlyInAnyOrder("Content 1", "Content 2");
    assertThat(contents2).containsExactlyInAnyOrder("Content 1", "Content 2");
  }

  @Test
  public void testGetCopyrightFileCount() throws Exception {
    final ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");

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
        .path(ApiLicenseLegalResource.COMPONENT_COPYRIGHT_FILE_COUNT)
        .parameter(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, "hash")
        .query("componentIdentifier", mavenIdentifier)
        .get();

    String packageUrl = PackageUrlIdentifier.toPackageUrl(mavenIdentifier);

    final HttpResponse response2 = restRequest()
        .path(ApiLicenseLegalResource.COMPONENT_COPYRIGHT_FILE_COUNT)
        .parameter(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, "hash")
        .query("packageUrl", packageUrl)
        .get();

    assertResponseStatus(200, response);
    assertResponseStatus(200, response2);
    final Map<String, Integer> fileCounts = response.getBody(Map.class);
    final Map<String, Integer> fileCounts2 = response2.getBody(Map.class);

    assertThat(fileCounts).hasSize(3)
        .hasEntrySatisfying("copyright hash 1", new Condition<>(Predicate.isEqual(2), "hash 1"))
        .hasEntrySatisfying("copyright hash 2", new Condition<>(Predicate.isEqual(5), "hash 2"))
        .hasEntrySatisfying("copyright hash 3", new Condition<>(Predicate.isEqual(3), "hash 3"));

    assertThat(fileCounts2).hasSize(3)
        .hasEntrySatisfying("copyright hash 1", new Condition<>(Predicate.isEqual(2), "hash 1"))
        .hasEntrySatisfying("copyright hash 2", new Condition<>(Predicate.isEqual(5), "hash 2"))
        .hasEntrySatisfying("copyright hash 3", new Condition<>(Predicate.isEqual(3), "hash 3"));
  }
}
