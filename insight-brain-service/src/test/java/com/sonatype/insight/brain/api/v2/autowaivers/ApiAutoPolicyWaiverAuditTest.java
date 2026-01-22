/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.autowaivers;

import java.util.Date;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverResource.BY_AUTO_POLICY_WAIVER_ID_PATH;
import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverResource.OWNERS_PATH;
import static org.mockito.Mockito.when;

public class ApiAutoPolicyWaiverAuditTest
    extends AbstractAuditTest
{
  @Before
  public void setup() {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(true);
  }

  @After
  public void cleanup() {
    licenseManager.reset();
  }

  @Test
  public void testAddApiAutoPolicyWaiver_Application() throws Exception {
    testProductLicense.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    Application app = tempEntity.newApplicationWithParent();

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = app.getId();
    dto.threatLevel = 2;
    dto.reachability = false;
    dto.pathForward = true;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    HttpResponse response = restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();

    ApiAutoPolicyWaiverDTO responseDTO = response.getBody(ApiAutoPolicyWaiverDTO.class);

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_AUTO_WAIVER, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "autoPolicyWaiverId", responseDTO.autoPolicyWaiverId);
  }

  @Test
  public void testAddApiAutoPolicyWaiver_Organization() throws Exception {
    testProductLicense.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    Organization organization = tempEntity.newOrganization();

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = organization.getId();
    dto.threatLevel = 2;
    dto.reachability = false;
    dto.pathForward = true;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    HttpResponse response = restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();

    ApiAutoPolicyWaiverDTO responseDTO = response.getBody(ApiAutoPolicyWaiverDTO.class);

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_AUTO_WAIVER, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "autoPolicyWaiverId", responseDTO.autoPolicyWaiverId);
  }

  @Test
  public void testAddApiAutoPolicyWaiver_Unauthorized() throws Exception {
    testProductLicense.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    Application app = tempEntity.newApplicationWithParent();

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = app.getId();
    dto.threatLevel = 2;
    dto.reachability = false;
    dto.pathForward = true;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    restRequest()
        .with(unauthorizedUser())
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_AUTO_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testUpdateApiAutoPolicyWaiver_Application() throws Exception {
    testProductLicense.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = new ApiAutoPolicyWaiverDTO();
    autoPolicyWaiverDTO.autoPolicyWaiverId = autoPolicyWaiver.getId();
    autoPolicyWaiverDTO.threatLevel = 1;
    autoPolicyWaiverDTO.reachability = true;
    autoPolicyWaiverDTO.pathForward = false;

    restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
        .body(autoPolicyWaiverDTO, MediaType.APPLICATION_JSON)
        .put();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_AUTO_WAIVER, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "autoPolicyWaiverId", autoPolicyWaiver.getId());
  }

  @Test
  public void testUpdateApiAutoPolicyWaiver_Organization() throws Exception {
    testProductLicense.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());

    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = new ApiAutoPolicyWaiverDTO();
    autoPolicyWaiverDTO.autoPolicyWaiverId = autoPolicyWaiver.getId();
    autoPolicyWaiverDTO.threatLevel = 1;
    autoPolicyWaiverDTO.reachability = true;
    autoPolicyWaiverDTO.pathForward = false;

    restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId(), autoPolicyWaiver.getId())
        .body(autoPolicyWaiverDTO, MediaType.APPLICATION_JSON)
        .put();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_AUTO_WAIVER, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "autoPolicyWaiverId", autoPolicyWaiver.getId());
  }

  @Test
  public void testUpdateApiAutoPolicyWaiver_Unauthorized() throws Exception {
    testProductLicense.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = new ApiAutoPolicyWaiverDTO();
    autoPolicyWaiverDTO.autoPolicyWaiverId = autoPolicyWaiver.getId();
    autoPolicyWaiverDTO.threatLevel = 1;
    autoPolicyWaiverDTO.reachability = true;
    autoPolicyWaiverDTO.pathForward = false;

    restRequest().path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .with(unauthorizedUser())
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
        .body(autoPolicyWaiverDTO, MediaType.APPLICATION_JSON)
        .put();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_AUTO_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testDeleteApiAutoPolicyWaiver_Application() throws Exception {
    testProductLicense.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
        .delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_AUTO_WAIVER, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "autoPolicyWaiverId", autoPolicyWaiver.getId());
  }

  @Test
  public void testDeleteApiAutoPolicyWaiver_Organization() throws Exception {
    testProductLicense.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());

    restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId(), autoPolicyWaiver.getId())
        .delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_AUTO_WAIVER, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "autoPolicyWaiverId", autoPolicyWaiver.getId());
  }

  @Test
  public void testDeleteApiAutoPolicyWaiver_Unauthorized() throws Exception {
    testProductLicense.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());
    restRequest().with(unauthorizedUser())
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId(), autoPolicyWaiver.getId())
        .delete();
    final AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_AUTO_WAIVER, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }
}
