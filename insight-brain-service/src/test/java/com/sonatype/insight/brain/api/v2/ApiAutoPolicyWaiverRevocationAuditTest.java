/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationResource.BY_AUTO_POLICY_WAIVER_REVOCATION_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationResource.OWNERS_PATH;
import static org.mockito.Mockito.when;

public class ApiAutoPolicyWaiverRevocationAuditTest
    extends AbstractAuditTest
{
  @Before
  public void setup() {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(true);
    testProductLicense.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);
  }

  @After
  public void cleanup() {
    licenseManager.reset();
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverRevocationDTO revocation = new ApiAutoPolicyWaiverRevocationDTO();
    revocation.ownerId = application.getId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();
    revocation.hash = "hash";
    revocation.scanId = "scanId";

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(revocation)
        .post();

    assertResponseStatus(200, response);
    ApiAutoPolicyWaiverRevocationDTO responseDTO = response.getBody(ApiAutoPolicyWaiverRevocationDTO.class);
    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_AUTO_WAIVER_REVOCATION, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "autoPolicyWaiverRevocationId", responseDTO.autoPolicyWaiverRevocationId);
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(org.getId());

    ApiAutoPolicyWaiverRevocationDTO revocation = new ApiAutoPolicyWaiverRevocationDTO();
    revocation.ownerId = org.getId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();
    revocation.hash = "hash";
    revocation.scanId = "scanId";

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(revocation)
        .post();

    assertResponseStatus(200, response);
    ApiAutoPolicyWaiverRevocationDTO responseDTO = response.getBody(ApiAutoPolicyWaiverRevocationDTO.class);
    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_AUTO_WAIVER_REVOCATION, null);
    assertOrganizationData(auditDTO, org);
    assertCustomData(auditDTO, "autoPolicyWaiverRevocationId", responseDTO.autoPolicyWaiverRevocationId);
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_Unauthorized() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverRevocationDTO revocation = new ApiAutoPolicyWaiverRevocationDTO();
    revocation.ownerId = application.getId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();
    revocation.hash = "hash";
    revocation.scanId = "scanId";
    
    restRequest().with(unauthorizedUser())
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(revocation)
        .post();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_AUTO_WAIVER_REVOCATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation =
        tempEntity.newAutoPolicyWaiverRevocation(app.getId(), autoPolicyWaiver.getId());

    restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_REVOCATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), revocation.getId())
        .delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_AUTO_WAIVER_REVOCATION, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "autoPolicyWaiverRevocationId", revocation.getId());
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());
    AutoPolicyWaiverRevocation revocation =
        tempEntity.newAutoPolicyWaiverRevocation(organization.getId(), autoPolicyWaiver.getId());

    restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_REVOCATION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId(), revocation.getId())
        .delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_AUTO_WAIVER_REVOCATION, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "autoPolicyWaiverRevocationId", revocation.getId());
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_Unauthorized() throws Exception {
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(organization.getId());
    AutoPolicyWaiverRevocation revocation = 
        tempEntity.newAutoPolicyWaiverRevocation(organization.getId(), autoPolicyWaiver.getId());
    
    restRequest().with(unauthorizedUser())
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_REVOCATION_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId(), revocation.getId())
        .delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_AUTO_WAIVER_REVOCATION, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }
}
