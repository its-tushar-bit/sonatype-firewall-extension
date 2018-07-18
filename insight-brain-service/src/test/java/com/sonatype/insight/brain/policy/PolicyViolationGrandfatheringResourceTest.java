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
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.PolicyViolationGrandfatheringService.PolicyViolationGrandfatheringDTO;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PolicyViolationGrandfatheringResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PolicyViolationGrandfatheringResource.RESOURCE_PATH);
  }

  @Test
  public void testRevokeGrandfathering() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scanId");
    Policy policy = tempEntity.newPolicy("test");
    PolicyViolation policyViolation = tempEntity.newGrandfatheredPolicyViolation(policyEvaluation, policy);

    HttpResponse response = restRequest().path(PolicyViolationGrandfatheringResource.REVOKE_PATH)
        .parameter(application.getPublicId()).put();
    assertResponseStatus(204, response);
    policyViolation = new PolicyViolationDAO().getById(policyViolation.getId());
    assertThat(policyViolation.isGrandfathered(), is(false));
  }

  @Test
  public void testGetGrandfathering_Application() throws Exception {
    Organization org = tempEntity.newOrganization();
    org.setAllowPolicyViolationGrandfatheringOverride(true);
    new OrganizationDAO().update(org);
    Application app = tempEntity.newApplication(org.getId());
    app.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(app);

    HttpResponse response = restRequest().path(PolicyViolationGrandfatheringResource.GET_PATH)
        .parameter(OwnerType.APPLICATION, app.getPublicId()).get();
    assertResponseStatus(200, response);

    PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO = response
        .getBody(PolicyViolationGrandfatheringDTO.class);
    assertThat(policyViolationGrandfatheringDTO.enabled, is(true));
    assertThat(policyViolationGrandfatheringDTO.inheritedFromOrganizationName, is(nullValue()));
    assertThat(policyViolationGrandfatheringDTO.allowOverride, is(false));
    assertThat(policyViolationGrandfatheringDTO.allowChange, is(true));
  }

  @Test
  public void testGetGrandfathering_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    org.setPolicyViolationGrandfatheringEnabled(true);
    org.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(org);

    HttpResponse response = restRequest().path(PolicyViolationGrandfatheringResource.GET_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId()).get();
    assertResponseStatus(200, response);

    PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO = response
        .getBody(PolicyViolationGrandfatheringDTO.class);
    assertThat(policyViolationGrandfatheringDTO.enabled, is(true));
    assertThat(policyViolationGrandfatheringDTO.inheritedFromOrganizationName, is(nullValue()));
    assertThat(policyViolationGrandfatheringDTO.allowOverride, is(false));
    assertThat(policyViolationGrandfatheringDTO.allowChange, is(true));
  }

  @Test
  public void testSetGrandfathering_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    app.setPolicyViolationGrandfatheringEnabled(false);
    new ApplicationDAO().update(app);

    PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO = new PolicyViolationGrandfatheringDTO();
    policyViolationGrandfatheringDTO.enabled = true;
    HttpResponse response = restRequest().path(PolicyViolationGrandfatheringResource.GET_PATH)
        .parameter(OwnerType.APPLICATION, app.getPublicId()).body(policyViolationGrandfatheringDTO).put();
    assertResponseStatus(200, response);

    assertThat(new ApplicationDAO().getById(app.getId()).isPolicyViolationGrandfatheringEnabled(), is(true));
    policyViolationGrandfatheringDTO = response.getBody(PolicyViolationGrandfatheringDTO.class);
    assertThat(policyViolationGrandfatheringDTO.enabled, is(true));
  }

  @Test
  public void testSetGrandfathering_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    org.setPolicyViolationGrandfatheringEnabled(false);
    org.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(org);

    PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO = new PolicyViolationGrandfatheringDTO();
    policyViolationGrandfatheringDTO.enabled = true;
    policyViolationGrandfatheringDTO.allowOverride = true;
    HttpResponse response = restRequest().path(PolicyViolationGrandfatheringResource.GET_PATH)
        .parameter(OwnerType.ORGANIZATION, org.getId()).body(policyViolationGrandfatheringDTO).put();
    assertResponseStatus(200, response);

    org = new OrganizationDAO().getById(org.getId());
    assertThat(org.isPolicyViolationGrandfatheringEnabled(), is(true));
    assertThat(org.isAllowPolicyViolationGrandfatheringOverride(), is(true));
    policyViolationGrandfatheringDTO = response.getBody(PolicyViolationGrandfatheringDTO.class);
    assertThat(policyViolationGrandfatheringDTO.enabled, is(true));
    assertThat(policyViolationGrandfatheringDTO.allowOverride, is(true));
  }
}
