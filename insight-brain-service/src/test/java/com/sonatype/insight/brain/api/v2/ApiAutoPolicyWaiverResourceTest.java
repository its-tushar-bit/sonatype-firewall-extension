/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverStatusDTO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiAutoPolicyExclusionResource.AUTO_WAIVER_STATUS_PATH;
import static com.sonatype.insight.brain.api.v2.ApiAutoPolicyExclusionResource.BY_AUTO_POLICY_WAIVER_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiAutoPolicyExclusionResource.OWNERS_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ApiAutoPolicyWaiverResourceTest
    extends AbstractResourceTest
{
  private AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  @Before
  public void setUp() {
    autoPolicyWaiverDAO = lookup(AutoPolicyWaiverDAO.class);
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(true);
    licenseManager.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
  }

  @After
  public void cleanup() {
    licenseManager.reset();
  }

  @Test
  public void testDeleteAutoPolicyWaiver() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
        .delete();

    assertResponseStatus(204, response);
    assertThat(autoPolicyWaiverDAO.getById(autoPolicyWaiver.getId())).isNull();
  }

  @Test
  public void testDeleteAutoPolicyWaiver_FeatureFlag() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
        .delete();

    assertResponseStatus(204, response);
    assertThat(autoPolicyWaiverDAO.getById(autoPolicyWaiver.getId())).isNull();

    //when feature flag is disabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);

    response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
        .delete();

    assertResponseStatus(403, response);
  }

  @Test
  public void testGetAutoPolicyWaivers_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    HttpResponse response = restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId()).get();

    assertResponseStatus(200, response);

    List<ApiAutoPolicyWaiverDTO> autoPolicyWaiverDtoList =
        Arrays.asList(response.getBody(ApiAutoPolicyWaiverDTO[].class));
    assertThat(autoPolicyWaiverDtoList).hasSize(1);

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO = autoPolicyWaiverDtoList.get(0);
    assertThat(apiAutoPolicyWaiverDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(apiAutoPolicyWaiverDTO.ownerId).isEqualTo(autoPolicyWaiver.getOwnerId());
    assertThat(apiAutoPolicyWaiverDTO.ownerName).isEqualTo(application.getName());
    assertThat(apiAutoPolicyWaiverDTO.ownerType).isEqualTo(OwnerType.APPLICATION.toString());
    assertThat(apiAutoPolicyWaiverDTO.publicId).isEqualTo(application.getPublicId());
    assertThat(apiAutoPolicyWaiverDTO.threatLevel).isEqualTo(autoPolicyWaiver.getThreatLevel());
    assertThat(apiAutoPolicyWaiverDTO.reachable).isEqualTo(autoPolicyWaiver.isReachable());
    assertThat(apiAutoPolicyWaiverDTO.pathForward).isEqualTo(autoPolicyWaiver.hasPathForward());
    assertThat(apiAutoPolicyWaiverDTO.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(apiAutoPolicyWaiverDTO.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(apiAutoPolicyWaiverDTO.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());
  }

  @Test
  public void testGetAutoPolicyWaivers_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());

    HttpResponse response =
        restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
            .parameter(OwnerType.ORGANIZATION, organization.getId()).get();

    assertResponseStatus(200, response);

    List<ApiAutoPolicyWaiverDTO> autoPolicyWaiverDtoList =
        Arrays.asList(response.getBody(ApiAutoPolicyWaiverDTO[].class));
    assertThat(autoPolicyWaiverDtoList).hasSize(1);

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO = autoPolicyWaiverDtoList.get(0);
    assertThat(apiAutoPolicyWaiverDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(apiAutoPolicyWaiverDTO.ownerId).isEqualTo(autoPolicyWaiver.getOwnerId());
    assertThat(apiAutoPolicyWaiverDTO.ownerName).isEqualTo(organization.getName());
    assertThat(apiAutoPolicyWaiverDTO.ownerType).isEqualTo(OwnerType.ORGANIZATION.toString());
    assertThat(apiAutoPolicyWaiverDTO.publicId).isEqualTo(organization.getPublicId());
    assertThat(apiAutoPolicyWaiverDTO.threatLevel).isEqualTo(autoPolicyWaiver.getThreatLevel());
    assertThat(apiAutoPolicyWaiverDTO.reachable).isEqualTo(autoPolicyWaiver.isReachable());
    assertThat(apiAutoPolicyWaiverDTO.pathForward).isEqualTo(autoPolicyWaiver.hasPathForward());
    assertThat(apiAutoPolicyWaiverDTO.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(apiAutoPolicyWaiverDTO.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(apiAutoPolicyWaiverDTO.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());
  }

  @Test
  public void testAddAutoPolicyWaiver_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = "ownerId";
    dto.threatLevel = 2;
    dto.reachable = false;
    dto.pathForward = true;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    HttpResponse response = restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();

    assertResponseStatus(200, response);
    List<AutoPolicyWaiver> autoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(app.getId());
    assertThat(autoPolicyWaivers.size()).isEqualTo(1);
  }

  @Test
  public void testAddAutoPolicyWaiver_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = "ownerId";
    dto.threatLevel = 2;
    dto.reachable = false;
    dto.pathForward = true;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    HttpResponse response = restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();

    assertResponseStatus(200, response);
    List<AutoPolicyWaiver> autoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(organization.getId());
    assertThat(autoPolicyWaivers.size()).isEqualTo(1);
  }

  @Test
  public void testGetAutoPolicyWaiver_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    HttpResponse response =
        restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
            .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId()).get();

    assertResponseStatus(200, response);

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO = response.getBody(ApiAutoPolicyWaiverDTO.class);
    assertThat(apiAutoPolicyWaiverDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(apiAutoPolicyWaiverDTO.ownerId).isEqualTo(autoPolicyWaiver.getOwnerId());
    assertThat(apiAutoPolicyWaiverDTO.ownerName).isEqualTo(application.getName());
    assertThat(apiAutoPolicyWaiverDTO.ownerType).isEqualTo(OwnerType.APPLICATION.toString());
    assertThat(apiAutoPolicyWaiverDTO.publicId).isEqualTo(application.getPublicId());
    assertThat(apiAutoPolicyWaiverDTO.threatLevel).isEqualTo(autoPolicyWaiver.getThreatLevel());
    assertThat(apiAutoPolicyWaiverDTO.reachable).isEqualTo(autoPolicyWaiver.isReachable());
    assertThat(apiAutoPolicyWaiverDTO.pathForward).isEqualTo(autoPolicyWaiver.hasPathForward());
    assertThat(apiAutoPolicyWaiverDTO.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(apiAutoPolicyWaiverDTO.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(apiAutoPolicyWaiverDTO.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());
  }

  @Test
  public void testGetAutoPolicyWaiver_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());

    HttpResponse response =
        restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
            .parameter(OwnerType.ORGANIZATION, organization.getId(), autoPolicyWaiver.getId()).get();

    assertResponseStatus(200, response);

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO = response.getBody(ApiAutoPolicyWaiverDTO.class);
    assertThat(apiAutoPolicyWaiverDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(apiAutoPolicyWaiverDTO.ownerId).isEqualTo(autoPolicyWaiver.getOwnerId());
    assertThat(apiAutoPolicyWaiverDTO.ownerName).isEqualTo(organization.getName());
    assertThat(apiAutoPolicyWaiverDTO.ownerType).isEqualTo(OwnerType.ORGANIZATION.toString());
    assertThat(apiAutoPolicyWaiverDTO.publicId).isEqualTo(organization.getPublicId());
    assertThat(apiAutoPolicyWaiverDTO.threatLevel).isEqualTo(autoPolicyWaiver.getThreatLevel());
    assertThat(apiAutoPolicyWaiverDTO.reachable).isEqualTo(autoPolicyWaiver.isReachable());
    assertThat(apiAutoPolicyWaiverDTO.pathForward).isEqualTo(autoPolicyWaiver.hasPathForward());
    assertThat(apiAutoPolicyWaiverDTO.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(apiAutoPolicyWaiverDTO.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(apiAutoPolicyWaiverDTO.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());
  }

  @Test
  public void testUpdateAutoPolicyWaiver_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = new ApiAutoPolicyWaiverDTO();
    autoPolicyWaiverDTO.autoPolicyWaiverId = autoPolicyWaiver.getId();
    autoPolicyWaiverDTO.threatLevel = 1;
    autoPolicyWaiverDTO.reachable = true;
    autoPolicyWaiverDTO.pathForward = false;

    HttpResponse response =
        restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
            .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
            .body(autoPolicyWaiverDTO, MediaType.APPLICATION_JSON)
            .put();

    assertResponseStatus(200, response);

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO = response.getBody(ApiAutoPolicyWaiverDTO.class);
    assertThat(apiAutoPolicyWaiverDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiverDTO.autoPolicyWaiverId);
    assertThat(apiAutoPolicyWaiverDTO.threatLevel).isEqualTo(1);
    assertThat(apiAutoPolicyWaiverDTO.reachable).isTrue();
    assertThat(apiAutoPolicyWaiverDTO.pathForward).isFalse();
  }

  @Test
  public void testUpdateAutoPolicyWaiver_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());

    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = new ApiAutoPolicyWaiverDTO();
    autoPolicyWaiverDTO.autoPolicyWaiverId = autoPolicyWaiver.getId();
    autoPolicyWaiverDTO.threatLevel = 1;
    autoPolicyWaiverDTO.reachable = true;
    autoPolicyWaiverDTO.pathForward = false;

    HttpResponse response =
        restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
            .parameter(OwnerType.ORGANIZATION, organization.getId(), autoPolicyWaiver.getId())
            .body(autoPolicyWaiverDTO, MediaType.APPLICATION_JSON)
            .put();

    assertResponseStatus(200, response);

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO = response.getBody(ApiAutoPolicyWaiverDTO.class);
    assertThat(apiAutoPolicyWaiverDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiverDTO.autoPolicyWaiverId);
    assertThat(apiAutoPolicyWaiverDTO.threatLevel).isEqualTo(1);
    assertThat(apiAutoPolicyWaiverDTO.reachable).isTrue();
    assertThat(apiAutoPolicyWaiverDTO.pathForward).isFalse();
  }

  @Test
  public void testGetAutoPolicyWaiver_MissingDeveloperDashboardFeature() throws Exception {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(false);
    setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    HttpResponse response =
        restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
            .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId()).get();

    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void testGetAutoPolicyWaivers_MissingDeveloperDashboardFeature() throws Exception {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(false);

    setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = tempEntity.newApplicationWithParent();

    HttpResponse response = restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId()).get();

    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void testAddAutoPolicyWaiver_BothOptionsAreFalse() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = "ownerId";
    dto.threatLevel = 2;
    dto.reachable = false;
    dto.pathForward = false;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    HttpResponse response = restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();

    assertResponseStatus(HttpStatus.BAD_REQUEST_400, response);

  }

  @Test
  public void testAddAutoPolicyWaiver_MissingDeveloperDashboardFeature() throws Exception {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(false);
    setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application app = tempEntity.newApplicationWithParent();

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = "ownerId";
    dto.threatLevel = 2;
    dto.reachable = false;
    dto.pathForward = true;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    HttpResponse response = restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();

    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void testUpdateAutoPolicyWaiver_MissingDeveloperDashboardFeature() throws Exception {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(false);
    setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = new ApiAutoPolicyWaiverDTO();
    autoPolicyWaiverDTO.autoPolicyWaiverId = autoPolicyWaiver.getId();
    autoPolicyWaiverDTO.threatLevel = 1;
    autoPolicyWaiverDTO.reachable = true;
    autoPolicyWaiverDTO.pathForward = false;

    HttpResponse response =
        restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
            .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
            .body(autoPolicyWaiverDTO, MediaType.APPLICATION_JSON)
            .put();

    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void testDeleteAutoPolicyWaiver_MissingDeveloperDashboardFeature() throws Exception {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(false);
    setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
        .delete();

    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void testGetAutoPolicyWaiverStatus_MissingDeveloperDashboardFeature() throws Exception {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(false);
    setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newAutoPolicyWaiver(application.getId());

    HttpResponse response =
        restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH, AUTO_WAIVER_STATUS_PATH)
            .parameter(OwnerType.APPLICATION, application.getId())
            .get();

    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void test_getAutoPolicyWaiverStatus_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    HttpResponse response =
        restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH, AUTO_WAIVER_STATUS_PATH)
            .parameter(OwnerType.APPLICATION, application.getId())
            .get();

    assertResponseStatus(200, response);

    ApiAutoPolicyWaiverStatusDTO dto = response.getBody(ApiAutoPolicyWaiverStatusDTO.class);
    assertThat(dto.isAutoWaiverEnabled).isTrue();
    assertThat(dto.isInherited).isFalse();
    assertThat(dto.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(dto.autoPolicyWaiverOwnerId).isEqualTo(application.getId());
    assertThat(dto.autoPolicyWaiverOwnerName).isEqualTo(application.getName());
  }

  @Test
  public void test_getAutoPolicyWaiverMetdata_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());

    HttpResponse response =
        restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH, AUTO_WAIVER_STATUS_PATH)
            .parameter(OwnerType.ORGANIZATION, organization.getId())
            .get();

    assertResponseStatus(200, response);

    ApiAutoPolicyWaiverStatusDTO dto = response.getBody(ApiAutoPolicyWaiverStatusDTO.class);
    assertThat(dto.isAutoWaiverEnabled).isTrue();
    assertThat(dto.isInherited).isFalse();
    assertThat(dto.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(dto.autoPolicyWaiverOwnerId).isEqualTo(organization.getId());
    assertThat(dto.autoPolicyWaiverOwnerName).isEqualTo(organization.getName());
  }
}
