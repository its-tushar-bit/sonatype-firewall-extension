/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.LegacyViolationService.LegacyViolationStatusDTO;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LegacyViolationResourceTest
    extends AbstractResourceTest
{
  private ApplicationDAO applicationDAO;

  private PolicyDAO policyDAO;

  private OrganizationDAO organizationDAO;

  private PolicyViolationDAO policyViolationDAO;

  @Before
  public void setUp() {
    policyDAO = lookup(PolicyDAO.class);
    applicationDAO = lookup(ApplicationDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LegacyViolationResource.RESOURCE_PATH);
  }

  @Test
  public void testRevokeLegacyViolationStatus() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scanId");
    Policy policy = tempEntity.newPolicy();
    PolicyViolation policyViolation = tempEntity.newLegacyPolicyViolation(policyEvaluation, policy);

    HttpResponse response = restRequest().path(LegacyViolationResource.REVOKE_PATH)
        .parameter(application.getPublicId())
        .put();
    assertResponseStatus(204, response);
    policyViolation = policyViolationDAO.getById(policyViolation.getId());
    assertThat(policyViolation.isLegacyViolation()).isFalse();
  }

  @Test
  public void testGrantLegacyViolationStatus() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scanId");
    Policy policy = tempEntity.newPolicy();
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    HttpResponse response = restRequest().path(LegacyViolationResource.GRANT_PATH)
        .parameter(application.getPublicId())
        .put();
    assertResponseStatus(204, response);
    policyViolation = policyViolationDAO.getById(policyViolation.getId());
    assertThat(policyViolation.isLegacyViolation()).isTrue();
  }

  @Test
  public void testGetLegacyViolationsStatus_Application() throws Exception {
    Organization org = tempEntity.newOrganization();
    org.setAllowLegacyViolationOverride(true);
    organizationDAO.update(org);
    Application app = tempEntity.newApplication(org.getId());
    app.setLegacyViolationEnabled(true);
    applicationDAO.update(app);

    HttpResponse response = restRequest().path(LegacyViolationResource.GET_PATH)
        .parameter(OwnerType.APPLICATION, app.getPublicId())
        .get();
    assertResponseStatus(200, response);

    LegacyViolationStatusDTO legacyViolationStatusDTO = response.getBody(LegacyViolationStatusDTO.class);
    assertThat(legacyViolationStatusDTO.enabled).isTrue();
    assertThat(legacyViolationStatusDTO.inheritedFromOrganizationName).isNull();
    assertThat(legacyViolationStatusDTO.allowOverride).isFalse();
    assertThat(legacyViolationStatusDTO.allowChange).isTrue();
  }

  @Test
  public void testGetLegacyViolationsStatus_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    org.setLegacyViolationEnabled(true);
    org.setAllowLegacyViolationOverride(false);
    organizationDAO.update(org);

    HttpResponse response = restRequest().path(LegacyViolationResource.GET_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .get();
    assertResponseStatus(200, response);

    LegacyViolationStatusDTO legacyViolationStatusDTO = response.getBody(LegacyViolationStatusDTO.class);
    assertThat(legacyViolationStatusDTO.enabled).isTrue();
    assertThat(legacyViolationStatusDTO.inheritedFromOrganizationName).isNull();
    assertThat(legacyViolationStatusDTO.allowOverride).isFalse();
    assertThat(legacyViolationStatusDTO.allowChange).isTrue();
  }

  @Test
  public void testSetLegacyViolationStatus_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    app.setLegacyViolationEnabled(false);
    applicationDAO.update(app);

    LegacyViolationStatusDTO legacyViolationStatusDTO = new LegacyViolationStatusDTO();
    legacyViolationStatusDTO.enabled = true;
    HttpResponse response = restRequest().path(LegacyViolationResource.GET_PATH)
        .parameter(OwnerType.APPLICATION, app.getPublicId())
        .body(legacyViolationStatusDTO)
        .put();
    assertResponseStatus(200, response);

    assertThat(applicationDAO.getById(app.getId()).isLegacyViolationEnabled()).isTrue();
    legacyViolationStatusDTO = response.getBody(LegacyViolationStatusDTO.class);
    assertThat(legacyViolationStatusDTO.enabled).isTrue();
  }

  @Test
  public void testSetLegacyViolationStatus_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    org.setLegacyViolationEnabled(false);
    org.setAllowLegacyViolationOverride(false);
    organizationDAO.update(org);

    LegacyViolationStatusDTO legacyViolationStatusDTO = new LegacyViolationStatusDTO();
    legacyViolationStatusDTO.enabled = true;
    legacyViolationStatusDTO.allowOverride = true;
    HttpResponse response = restRequest().path(LegacyViolationResource.GET_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId())
        .body(legacyViolationStatusDTO)
        .put();
    assertResponseStatus(200, response);

    org = organizationDAO.getById(org.getId());
    assertThat(org.isLegacyViolationEnabled()).isTrue();
    assertThat(org.isAllowLegacyViolationOverride()).isTrue();
    legacyViolationStatusDTO = response.getBody(LegacyViolationStatusDTO.class);
    assertThat(legacyViolationStatusDTO.enabled).isTrue();
    assertThat(legacyViolationStatusDTO.allowOverride).isTrue();
  }
}
