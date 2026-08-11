/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.autowaivers;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverStatusDTO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverResource.APPLICABLE_WAIVERS_PATH;
import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverResource.AUTO_WAIVER_STATUS_PATH;
import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverResource.BY_AUTO_POLICY_WAIVER_ID_PATH;
import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverResource.OWNERS_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * IQ Server on PostgreSQL — converted from the legacy {@code ApiAutoPolicyWaiverResourceTest}
 * ({@code insight-brain-service}), reusing the shared, cached server via {@link IqTestContext}.
 */
@IqPostgresTest
class IqPostgresApiAutoPolicyWaiverResourceTest
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  @BeforeEach
  void setUp() throws Exception {
    autoPolicyWaiverDAO = ctx.lookup(AutoPolicyWaiverDAO.class);
    when(ctx.lookup(DeveloperEnablementService.class).shouldEnableDeveloperProduct()).thenReturn(true);
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.AUTO_WAIVER_MANAGEMENT);
  }

  @AfterEach
  void cleanup() {
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);
  }

  @Test
  void testDeleteAutoPolicyWaiver() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
        .delete();

    ctx.assertResponseStatus(204, response);
    assertThat(autoPolicyWaiverDAO.getById(autoPolicyWaiver.getId())).isNull();
  }

  @Test
  void testDeleteAutoPolicyWaiver_FeatureFlag() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
        .delete();

    ctx.assertResponseStatus(204, response);
    assertThat(autoPolicyWaiverDAO.getById(autoPolicyWaiver.getId())).isNull();

    // when feature flag is disabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);

    response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
        .delete();

    ctx.assertResponseStatus(403, response);
  }

  @Test
  void testGetAutoPolicyWaivers_Application() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .get();

    ctx.assertResponseStatus(200, response);

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
    assertThat(apiAutoPolicyWaiverDTO.reachability).isEqualTo(autoPolicyWaiver.hasReachability());
    assertThat(apiAutoPolicyWaiverDTO.pathForward).isEqualTo(autoPolicyWaiver.hasPathForward());
    assertThat(apiAutoPolicyWaiverDTO.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(apiAutoPolicyWaiverDTO.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(apiAutoPolicyWaiverDTO.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());
  }

  @Test
  void testGetAutoPolicyWaivers_Organization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(organization.getId());

    HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
            .parameter(OwnerType.ORGANIZATION, organization.getId())
            .get();

    ctx.assertResponseStatus(200, response);

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
    assertThat(apiAutoPolicyWaiverDTO.reachability).isEqualTo(autoPolicyWaiver.hasReachability());
    assertThat(apiAutoPolicyWaiverDTO.pathForward).isEqualTo(autoPolicyWaiver.hasPathForward());
    assertThat(apiAutoPolicyWaiverDTO.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(apiAutoPolicyWaiverDTO.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(apiAutoPolicyWaiverDTO.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());
  }

  @Test
  void testAddAutoPolicyWaiver_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = "ownerId";
    dto.threatLevel = 2;
    dto.reachability = false;
    dto.pathForward = true;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(200, response);
    List<AutoPolicyWaiver> autoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(app.getId());
    assertThat(autoPolicyWaivers.size()).isEqualTo(1);
  }

  @Test
  void testAddAutoPolicyWaivers_Application() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver1 = new ApiAutoPolicyWaiverDTO();
    apiAutoPolicyWaiver1.threatLevel = 2;
    apiAutoPolicyWaiver1.reachability = false;
    apiAutoPolicyWaiver1.pathForward = true;

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver2 = new ApiAutoPolicyWaiverDTO();
    apiAutoPolicyWaiver2.threatLevel = 2;
    apiAutoPolicyWaiver2.reachability = true;
    apiAutoPolicyWaiver2.pathForward = false;

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/v2/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(List.of(apiAutoPolicyWaiver1, apiAutoPolicyWaiver2), MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(200, response);
    List<AutoPolicyWaiver> autoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(app.getId());
    assertThat(autoPolicyWaivers.size()).isEqualTo(2);

    // try to add the same waivers again
    response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/v2/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(List.of(apiAutoPolicyWaiver1, apiAutoPolicyWaiver2), MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(400, response);
    autoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(app.getId());
    assertThat(autoPolicyWaivers.size()).isEqualTo(2);

    // try to add the same waivers again with different values that are not allowed
    apiAutoPolicyWaiver1.reachability = true;
    apiAutoPolicyWaiver1.pathForward = true;
    apiAutoPolicyWaiver2.reachability = true;
    apiAutoPolicyWaiver2.pathForward = true;

    response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/v2/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(List.of(apiAutoPolicyWaiver1, apiAutoPolicyWaiver2), MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(400, response);
    autoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(app.getId());
    assertThat(autoPolicyWaivers.size()).isEqualTo(2);
  }

  @Test
  void testAddAutoPolicyWaivers_OrganizationWithApplication() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver1 = new ApiAutoPolicyWaiverDTO();
    apiAutoPolicyWaiver1.threatLevel = 2;
    apiAutoPolicyWaiver1.reachability = false;
    apiAutoPolicyWaiver1.pathForward = true;

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/v2/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, app.getOrganizationId())
        .body(List.of(apiAutoPolicyWaiver1), MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(200, response);
    List<AutoPolicyWaiver> autoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(app.getOrganizationId());
    assertThat(autoPolicyWaivers.size()).isEqualTo(1);

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver2 = new ApiAutoPolicyWaiverDTO();
    apiAutoPolicyWaiver2.threatLevel = 2;
    apiAutoPolicyWaiver2.reachability = true;
    apiAutoPolicyWaiver2.pathForward = false;

    // try to add the same type of waiver as the org, and a new one.
    response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/v2/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(List.of(apiAutoPolicyWaiver1, apiAutoPolicyWaiver2), MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(200, response);
    autoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(app.getId());
    assertThat(autoPolicyWaivers.size()).isEqualTo(2);
  }

  @Test
  void testAddAutoPolicyWaiver_Organization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = "ownerId";
    dto.threatLevel = 2;
    dto.reachability = false;
    dto.pathForward = true;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(200, response);
    List<AutoPolicyWaiver> autoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(organization.getId());
    assertThat(autoPolicyWaivers.size()).isEqualTo(1);
  }

  @Test
  void testAddAutoPolicyWaivers_Organization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver1 = new ApiAutoPolicyWaiverDTO();
    apiAutoPolicyWaiver1.threatLevel = 2;
    apiAutoPolicyWaiver1.reachability = false;
    apiAutoPolicyWaiver1.pathForward = true;

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiver2 = new ApiAutoPolicyWaiverDTO();
    apiAutoPolicyWaiver2.threatLevel = 2;
    apiAutoPolicyWaiver2.reachability = true;
    apiAutoPolicyWaiver2.pathForward = false;

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/v2/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .body(List.of(apiAutoPolicyWaiver1, apiAutoPolicyWaiver2), MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(200, response);
    List<AutoPolicyWaiver> autoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(organization.getId());
    assertThat(autoPolicyWaivers.size()).isEqualTo(2);

    // try to add the same waivers again
    response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/v2/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .body(List.of(apiAutoPolicyWaiver1, apiAutoPolicyWaiver2), MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(400, response);
    autoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(organization.getId());
    assertThat(autoPolicyWaivers.size()).isEqualTo(2);

    // try to add the same waivers again with different values that are not allowed
    apiAutoPolicyWaiver1.reachability = true;
    apiAutoPolicyWaiver1.pathForward = true;
    apiAutoPolicyWaiver2.reachability = true;
    apiAutoPolicyWaiver2.pathForward = true;

    response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/v2/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .body(List.of(apiAutoPolicyWaiver1, apiAutoPolicyWaiver2), MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(400, response);
    autoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(organization.getId());
    assertThat(autoPolicyWaivers.size()).isEqualTo(2);
  }

  @Test
  void testGetAutoPolicyWaiver_Application() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());

    HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
            .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
            .get();

    ctx.assertResponseStatus(200, response);

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO = response.getBody(ApiAutoPolicyWaiverDTO.class);
    assertThat(apiAutoPolicyWaiverDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(apiAutoPolicyWaiverDTO.ownerId).isEqualTo(autoPolicyWaiver.getOwnerId());
    assertThat(apiAutoPolicyWaiverDTO.ownerName).isEqualTo(application.getName());
    assertThat(apiAutoPolicyWaiverDTO.ownerType).isEqualTo(OwnerType.APPLICATION.toString());
    assertThat(apiAutoPolicyWaiverDTO.publicId).isEqualTo(application.getPublicId());
    assertThat(apiAutoPolicyWaiverDTO.threatLevel).isEqualTo(autoPolicyWaiver.getThreatLevel());
    assertThat(apiAutoPolicyWaiverDTO.reachability).isEqualTo(autoPolicyWaiver.hasReachability());
    assertThat(apiAutoPolicyWaiverDTO.pathForward).isEqualTo(autoPolicyWaiver.hasPathForward());
    assertThat(apiAutoPolicyWaiverDTO.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(apiAutoPolicyWaiverDTO.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(apiAutoPolicyWaiverDTO.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());
  }

  @Test
  void testGetAutoPolicyWaiver_Organization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(organization.getId());

    HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
            .parameter(OwnerType.ORGANIZATION, organization.getId(), autoPolicyWaiver.getId())
            .get();

    ctx.assertResponseStatus(200, response);

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO = response.getBody(ApiAutoPolicyWaiverDTO.class);
    assertThat(apiAutoPolicyWaiverDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(apiAutoPolicyWaiverDTO.ownerId).isEqualTo(autoPolicyWaiver.getOwnerId());
    assertThat(apiAutoPolicyWaiverDTO.ownerName).isEqualTo(organization.getName());
    assertThat(apiAutoPolicyWaiverDTO.ownerType).isEqualTo(OwnerType.ORGANIZATION.toString());
    assertThat(apiAutoPolicyWaiverDTO.publicId).isEqualTo(organization.getPublicId());
    assertThat(apiAutoPolicyWaiverDTO.threatLevel).isEqualTo(autoPolicyWaiver.getThreatLevel());
    assertThat(apiAutoPolicyWaiverDTO.reachability).isEqualTo(autoPolicyWaiver.hasReachability());
    assertThat(apiAutoPolicyWaiverDTO.pathForward).isEqualTo(autoPolicyWaiver.hasPathForward());
    assertThat(apiAutoPolicyWaiverDTO.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(apiAutoPolicyWaiverDTO.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(apiAutoPolicyWaiverDTO.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());
  }

  @Test
  void testUpdateAutoPolicyWaiver_Application() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = new ApiAutoPolicyWaiverDTO();
    autoPolicyWaiverDTO.autoPolicyWaiverId = autoPolicyWaiver.getId();
    autoPolicyWaiverDTO.threatLevel = 1;
    autoPolicyWaiverDTO.reachability = true;
    autoPolicyWaiverDTO.pathForward = false;

    HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
            .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
            .body(autoPolicyWaiverDTO, MediaType.APPLICATION_JSON)
            .put();

    ctx.assertResponseStatus(200, response);

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO = response.getBody(ApiAutoPolicyWaiverDTO.class);
    assertThat(apiAutoPolicyWaiverDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiverDTO.autoPolicyWaiverId);
    assertThat(apiAutoPolicyWaiverDTO.threatLevel).isEqualTo(1);
    assertThat(apiAutoPolicyWaiverDTO.reachability).isTrue();
    assertThat(apiAutoPolicyWaiverDTO.pathForward).isFalse();
  }

  @Test
  void testUpdateAutoPolicyWaiver_Organization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(organization.getId());

    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = new ApiAutoPolicyWaiverDTO();
    autoPolicyWaiverDTO.autoPolicyWaiverId = autoPolicyWaiver.getId();
    autoPolicyWaiverDTO.threatLevel = 1;
    autoPolicyWaiverDTO.reachability = true;
    autoPolicyWaiverDTO.pathForward = false;

    HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
            .parameter(OwnerType.ORGANIZATION, organization.getId(), autoPolicyWaiver.getId())
            .body(autoPolicyWaiverDTO, MediaType.APPLICATION_JSON)
            .put();

    ctx.assertResponseStatus(200, response);

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO = response.getBody(ApiAutoPolicyWaiverDTO.class);
    assertThat(apiAutoPolicyWaiverDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiverDTO.autoPolicyWaiverId);
    assertThat(apiAutoPolicyWaiverDTO.threatLevel).isEqualTo(1);
    assertThat(apiAutoPolicyWaiverDTO.reachability).isTrue();
    assertThat(apiAutoPolicyWaiverDTO.pathForward).isFalse();
  }

  @Test
  void testGetAutoPolicyWaiver_MissingDeveloperDashboardFeature() throws Exception {
    when(ctx.lookup(DeveloperEnablementService.class).shouldEnableDeveloperProduct()).thenReturn(false);
    ctx.setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());

    HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
            .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
            .get();

    ctx.assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  void testGetAutoPolicyWaivers_MissingDeveloperDashboardFeature() throws Exception {
    when(ctx.lookup(DeveloperEnablementService.class).shouldEnableDeveloperProduct()).thenReturn(false);

    ctx.setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = ctx.tempEntity().newApplicationWithParent();

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .get();

    ctx.assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  void testAddAutoPolicyWaiver_BothOptionsAreFalse() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = "ownerId";
    dto.threatLevel = 2;
    dto.reachability = false;
    dto.pathForward = false;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(HttpStatus.BAD_REQUEST_400, response);

  }

  @Test
  void testAddAutoPolicyWaiver_MissingDeveloperDashboardFeature() throws Exception {
    when(ctx.lookup(DeveloperEnablementService.class).shouldEnableDeveloperProduct()).thenReturn(false);
    ctx.setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application app = ctx.tempEntity().newApplicationWithParent();

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = "ownerId";
    dto.threatLevel = 2;
    dto.reachability = false;
    dto.pathForward = true;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();

    ctx.assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  void testUpdateAutoPolicyWaiver_MissingDeveloperDashboardFeature() throws Exception {
    when(ctx.lookup(DeveloperEnablementService.class).shouldEnableDeveloperProduct()).thenReturn(false);
    ctx.setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = new ApiAutoPolicyWaiverDTO();
    autoPolicyWaiverDTO.autoPolicyWaiverId = autoPolicyWaiver.getId();
    autoPolicyWaiverDTO.threatLevel = 1;
    autoPolicyWaiverDTO.reachability = true;
    autoPolicyWaiverDTO.pathForward = false;

    HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
            .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
            .body(autoPolicyWaiverDTO, MediaType.APPLICATION_JSON)
            .put();

    ctx.assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  void testDeleteAutoPolicyWaiver_MissingDeveloperDashboardFeature() throws Exception {
    when(ctx.lookup(DeveloperEnablementService.class).shouldEnableDeveloperProduct()).thenReturn(false);
    ctx.setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
        .delete();

    ctx.assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  void testGetAutoPolicyWaiverStatus_MissingDeveloperDashboardFeature() throws Exception {
    when(ctx.lookup(DeveloperEnablementService.class).shouldEnableDeveloperProduct()).thenReturn(false);
    ctx.setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = ctx.tempEntity().newApplicationWithParent();
    ctx.tempEntity().newAutoPolicyWaiver(application.getId());

    HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH, AUTO_WAIVER_STATUS_PATH)
            .parameter(OwnerType.APPLICATION, application.getId())
            .get();

    ctx.assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  void test_getAutoPolicyWaiverStatus_Application() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());

    HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH, AUTO_WAIVER_STATUS_PATH)
            .parameter(OwnerType.APPLICATION, application.getId())
            .get();

    ctx.assertResponseStatus(200, response);

    ApiAutoPolicyWaiverStatusDTO dto = response.getBody(ApiAutoPolicyWaiverStatusDTO.class);
    assertThat(dto.isAutoWaiverEnabled).isTrue();
    assertThat(dto.isInherited).isFalse();
    assertThat(dto.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(dto.autoPolicyWaiverOwnerId).isEqualTo(application.getId());
    assertThat(dto.autoPolicyWaiverOwnerName).isEqualTo(application.getName());
  }

  @Test
  void test_getAutoPolicyWaiverMetdata_Organization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(organization.getId());

    HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH, AUTO_WAIVER_STATUS_PATH)
            .parameter(OwnerType.ORGANIZATION, organization.getId())
            .get();

    ctx.assertResponseStatus(200, response);

    ApiAutoPolicyWaiverStatusDTO dto = response.getBody(ApiAutoPolicyWaiverStatusDTO.class);
    assertThat(dto.isAutoWaiverEnabled).isTrue();
    assertThat(dto.isInherited).isFalse();
    assertThat(dto.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(dto.autoPolicyWaiverOwnerId).isEqualTo(organization.getId());
    assertThat(dto.autoPolicyWaiverOwnerName).isEqualTo(organization.getName());
  }

  @Test
  void testGetApplicableAutoWaivers_MissingDeveloperDashboardFeature() throws Exception {
    when(ctx.lookup(DeveloperEnablementService.class).shouldEnableDeveloperProduct()).thenReturn(false);
    ctx.setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    final Application application = ctx.tempEntity().newApplicationWithParent();
    ctx.tempEntity().newAutoPolicyWaiver(application.getId());

    final HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH, APPLICABLE_WAIVERS_PATH)
            .parameter(OwnerType.APPLICATION, application.getId())
            .get();

    ctx.assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  void testGetApplicableAutoWaivers_Application() throws Exception {
    final Application app = ctx.tempEntity().newApplicationWithParent();
    final AutoPolicyWaiver autoWaiver = ctx.tempEntity().newAutoPolicyWaiver(app.getId());

    final HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH, APPLICABLE_WAIVERS_PATH)
            .parameter(OwnerType.APPLICATION, app.getId())
            .get();

    ctx.assertResponseStatus(200, response);

    final List<ApiAutoPolicyWaiverStatusDTO> applicableWaivers =
        Arrays.asList(response.getBody(ApiAutoPolicyWaiverStatusDTO[].class));
    assertThat(applicableWaivers).hasSize(1);
    final ApiAutoPolicyWaiverStatusDTO dto = applicableWaivers.get(0);
    assertThat(dto.isAutoWaiverEnabled).isTrue();
    assertThat(dto.isInherited).isFalse();
    assertThat(dto.autoPolicyWaiverId).isEqualTo(autoWaiver.getId());
    assertThat(dto.autoPolicyWaiverOwnerId).isEqualTo(app.getId());
    assertThat(dto.autoPolicyWaiverOwnerName).isEqualTo(app.getName());
  }

  @Test
  void testGetApplicableAutoWaivers_Organization() throws Exception {
    final Organization org = ctx.tempEntity().newOrganization();
    final AutoPolicyWaiver autoWaiver = ctx.tempEntity().newAutoPolicyWaiver(org.getId());

    final HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH, APPLICABLE_WAIVERS_PATH)
            .parameter(OwnerType.ORGANIZATION, org.getId())
            .get();

    ctx.assertResponseStatus(200, response);

    final List<ApiAutoPolicyWaiverStatusDTO> applicableWaivers =
        Arrays.asList(response.getBody(ApiAutoPolicyWaiverStatusDTO[].class));
    assertThat(applicableWaivers).hasSize(1);
    final ApiAutoPolicyWaiverStatusDTO dto = applicableWaivers.get(0);
    assertThat(dto.isAutoWaiverEnabled).isTrue();
    assertThat(dto.isInherited).isFalse();
    assertThat(dto.autoPolicyWaiverId).isEqualTo(autoWaiver.getId());
    assertThat(dto.autoPolicyWaiverOwnerId).isEqualTo(org.getId());
    assertThat(dto.autoPolicyWaiverOwnerName).isEqualTo(org.getName());
  }

  @Test
  void testGetApplicableAutoWaivers_NoAutoWaivers() throws Exception {
    final Organization org = ctx.tempEntity().newOrganization();

    final HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH, APPLICABLE_WAIVERS_PATH)
            .parameter(OwnerType.ORGANIZATION, org.getId())
            .get();

    ctx.assertResponseStatus(200, response);

    final List<ApiAutoPolicyWaiverStatusDTO> applicableWaivers =
        Arrays.asList(response.getBody(ApiAutoPolicyWaiverStatusDTO[].class));
    assertThat(applicableWaivers).isEmpty();
  }

  @Test
  void testGetApplicableAutoWaivers_AutoWaiversDisabled() throws Exception {
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);
    final Organization org = ctx.tempEntity().newOrganization();

    final HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH, APPLICABLE_WAIVERS_PATH)
            .parameter(OwnerType.ORGANIZATION, org.getId())
            .get();
    ctx.assertResponseStatus(403, response);
  }

  @Test
  void testGetApplicableAutoWaivers_InvalidOwnerType() throws Exception {
    final Organization org = ctx.tempEntity().newOrganization();

    final HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH, APPLICABLE_WAIVERS_PATH)
            .parameter(OwnerType.REPOSITORY, org.getId())
            .get();
    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testGetApplicableAutoWaivers_NonexistentOwnerId() throws Exception {
    final HttpResponse response =
        ctx.restRequest()
            .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH, APPLICABLE_WAIVERS_PATH)
            .parameter(OwnerType.ORGANIZATION, "does-not-exist")
            .get();
    ctx.assertResponseStatus(404, response);
  }
}
