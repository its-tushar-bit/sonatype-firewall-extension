/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
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
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.policy.PolicyViolationGrandfatheringService.PolicyViolationGrandfatheringDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

public class PolicyViolationGrandfatheringServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyViolationGrandfatheringService policyViolationGrandfatheringService;

  @Test
  public void testRevokeGrandfathering() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy();
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scanId1");
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyViolation fixedGrandfatheredPolicyViolation = tempEntity.newGrandfatheredPolicyViolation(policyEvaluation1,
        policy);
    fixedGrandfatheredPolicyViolation.setFixTime(new Date());
    policyViolationDAO.update(fixedGrandfatheredPolicyViolation);
    PolicyViolation grandfatheredPolicyViolation1 = tempEntity.newGrandfatheredPolicyViolation(policyEvaluation1,
        policy);
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, "scanId2");
    PolicyViolation grandfatheredPolicyViolation2 = tempEntity.newGrandfatheredPolicyViolation(policyEvaluation2,
        policy);

    policyViolationGrandfatheringService.revokeGrandfathering(app1.getPublicId());

    assertThat(policyViolationDAO.getById(fixedGrandfatheredPolicyViolation.getId()).isGrandfathered(), is(true));
    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation1.getId()).isGrandfathered(), is(false));
    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation2.getId()).isGrandfathered(), is(true));
  }

  private void testGrandfather(Application app, boolean grandfatheringAllowed) throws Exception {
    Policy policy1 = tempEntity.newPolicy();
    policy1.setPolicyViolationGrandfatheringAllowed(true);
    new PolicyDAO().update(policy1);
    Policy policy2 = tempEntity.newPolicy();

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId1");
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyViolation unfixedPolicyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    PolicyViolation fixedPolicyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    fixedPolicyViolation1.setFixTime(new Date());
    policyViolationDAO.update(fixedPolicyViolation1);
    PolicyViolation grandfatheredPolicyViolation1 = tempEntity.newPolicyViolation(policyEvaluation1, policy1);
    Date inThePast = new Date(System.currentTimeMillis() - 1);
    grandfatheredPolicyViolation1.setGrandfatherTime(inThePast);
    policyViolationDAO.update(grandfatheredPolicyViolation1);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy1.getId(), app.getId());
    PolicyViolation waivedPolicyViolation1 = tempEntity.newWaivedPolicyViolation(policyEvaluation1, policy1,
        policyWaiver);
    PolicyViolation unfixedPolicyViolation1PolicyDoesNotExist = tempEntity.newPolicyViolation(policyEvaluation1,
        policy2);
    new PolicyDAO().delete(policy2);

    Application app2 = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, "scanId2");
    PolicyViolation unfixedPolicyViolation2 = tempEntity.newPolicyViolation(policyEvaluation2, policy1);

    Date before = new Date();
    try {
      policyViolationGrandfatheringService.grandfather(app.getPublicId());
      if (!grandfatheringAllowed) {
        fail("Expected exception");
      }
    }
    catch (BadRequestException e) {
      if (grandfatheringAllowed) {
        throw e;
      }
      assertThat(e.getMessage(),
          is("Policy violation grandfathering is not enabled for application '" + app.getName() + "'."));
    }
    Date after = new Date();

    assertThat(policyViolationDAO.getById(fixedPolicyViolation1.getId()).isGrandfathered(), is(false));
    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation1.getId()).getGrandfatherTime(),
        is(inThePast));
    if (grandfatheringAllowed) {
      assertPolicyViolationGrandfatherTime(unfixedPolicyViolation1, before, after);
      assertPolicyViolationGrandfatherTime(waivedPolicyViolation1, before, after);
      assertPolicyViolationGrandfatherTime(unfixedPolicyViolation1PolicyDoesNotExist, before, after);
    }
    else {
      assertThat(policyViolationDAO.getById(unfixedPolicyViolation1.getId()).isGrandfathered(), is(false));
      assertThat(policyViolationDAO.getById(waivedPolicyViolation1.getId()).isGrandfathered(), is(false));
      assertThat(policyViolationDAO.getById(unfixedPolicyViolation1PolicyDoesNotExist.getId()).isGrandfathered(),
          is(false));
    }

    assertThat(policyViolationDAO.getById(unfixedPolicyViolation2.getId()).isGrandfathered(), is(false));
  }

  @Test
  public void testGrandfather_GrandfatheringNotConfiguredForAppOrOrg() throws Exception {
    Organization organization = tempEntity.newOrganization();
    organization.setPolicyViolationGrandfatheringEnabled(null);
    organization.setAllowPolicyViolationGrandfatheringOverride(true);
    new OrganizationDAO().update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setPolicyViolationGrandfatheringEnabled(null);
    new ApplicationDAO().update(application);
    testGrandfather(application, false);
  }

  @Test
  public void testGrandfather_GrandfatheringEnabledForApp_AppCanOverrideGrandfathering() throws Exception {
    Organization organization = tempEntity.newOrganization();
    organization.setPolicyViolationGrandfatheringEnabled(false);
    organization.setAllowPolicyViolationGrandfatheringOverride(true);
    new OrganizationDAO().update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);
    testGrandfather(application, true);
  }

  @Test
  public void testGrandfather_GrandfatheringDisabledForApp_AppCanOverrideGrandfathering() throws Exception {
    Organization organization = tempEntity.newOrganization();
    organization.setPolicyViolationGrandfatheringEnabled(true);
    organization.setAllowPolicyViolationGrandfatheringOverride(true);
    new OrganizationDAO().update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setPolicyViolationGrandfatheringEnabled(false);
    new ApplicationDAO().update(application);
    testGrandfather(application, false);
  }

  @Test
  public void testGrandfather_GrandfatheringEnabledForApp_DisabledForOrg_AppCannotOverrideGrandfathering()
      throws Exception
  {
    Organization organization = tempEntity.newOrganization();
    organization.setPolicyViolationGrandfatheringEnabled(false);
    organization.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);
    testGrandfather(application, false);
  }

  @Test
  public void testGrandfather_GrandfatheringDisabledForApp_EnabledForOrg_AppCannotOverrideGrandfathering()
      throws Exception
  {
    Organization organization = tempEntity.newOrganization();
    organization.setPolicyViolationGrandfatheringEnabled(true);
    organization.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setPolicyViolationGrandfatheringEnabled(false);
    new ApplicationDAO().update(application);
    testGrandfather(application, true);
  }

  @Test
  public void testGrandfather_GrandfatheringEnabledForApp_EnabledForOrg_AppCannotOverrideGrandfathering()
      throws Exception
  {
    Organization organization = tempEntity.newOrganization();
    organization.setPolicyViolationGrandfatheringEnabled(true);
    organization.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);
    testGrandfather(application, true);
  }

  @Test
  public void testGrandfather_GrandfatheringDisabledForApp_DisabledForOrg_AppCannotOverrideGrandfathering()
      throws Exception
  {
    Organization organization = tempEntity.newOrganization();
    organization.setPolicyViolationGrandfatheringEnabled(false);
    organization.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setPolicyViolationGrandfatheringEnabled(false);
    new ApplicationDAO().update(application);
    testGrandfather(application, false);
  }

  private void assertPolicyViolationGrandfatherTime(PolicyViolation policyViolation, Date before, Date after) {
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    assertThat(policyViolationDAO.getById(policyViolation.getId()).getGrandfatherTime(), greaterThanOrEqualTo(before));
    assertThat(policyViolationDAO.getById(policyViolation.getId()).getGrandfatherTime(), lessThanOrEqualTo(after));
  }

  @Test
  public void testGetGrandfathering_Application() throws Exception {
    // The parent org doesn't allow override, grandfathering is not specified at any level.
    Organization org = tempEntity.newOrganization();
    org.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(org);
    Application app = tempEntity.newApplication(org.getId());
    PolicyViolationGrandfatheringDTO result = policyViolationGrandfatheringService
        .getGrandfathering(OwnerType.APPLICATION, app.getPublicId());
    assertPolicyViolationGrandfatheringDTO(result, null, "Root Organization", false, false);

    // The parent org allows override, grandfathering is not specified at any level.
    org.setAllowPolicyViolationGrandfatheringOverride(true);
    new OrganizationDAO().update(org);
    result = policyViolationGrandfatheringService.getGrandfathering(OwnerType.APPLICATION, app.getPublicId());
    assertPolicyViolationGrandfatheringDTO(result, null, "Root Organization", false, true);

    // The parent org allows override and grandfathering is specified at org level.
    org.setPolicyViolationGrandfatheringEnabled(true);
    new OrganizationDAO().update(org);
    result = policyViolationGrandfatheringService.getGrandfathering(OwnerType.APPLICATION, app.getPublicId());
    assertPolicyViolationGrandfatheringDTO(result, true, org.getName(), false, true);

    // The parent org allows override and grandfathering is specified at app and org level.
    app.setPolicyViolationGrandfatheringEnabled(false);
    new ApplicationDAO().update(app);
    result = policyViolationGrandfatheringService.getGrandfathering(OwnerType.APPLICATION, app.getPublicId());
    assertPolicyViolationGrandfatheringDTO(result, false, null, false, true);

    // The parent org doesn't allow override, grandfathering is specified at app level.
    org.setPolicyViolationGrandfatheringEnabled(null);
    org.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(org);
    app.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(app);
    result = policyViolationGrandfatheringService.getGrandfathering(OwnerType.APPLICATION, app.getPublicId());
    assertPolicyViolationGrandfatheringDTO(result, null, "Root Organization", false, false);
  }

  @Test
  public void testGetGrandfathering_Organization() throws Exception {
    Organization rootOrganization = new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID);
    Boolean grandfatheringEnabled = rootOrganization.isPolicyViolationGrandfatheringEnabled();
    boolean grandfatheringOverrideEnabled = rootOrganization.isAllowPolicyViolationGrandfatheringOverride();
    try {
      // The parent org doesn't allow override, grandfathering is not specified at any level.
      rootOrganization.setAllowPolicyViolationGrandfatheringOverride(false);
      new OrganizationDAO().update(rootOrganization);
      Organization org = tempEntity.newOrganization();
      org.setAllowPolicyViolationGrandfatheringOverride(false);
      new OrganizationDAO().update(org);
      PolicyViolationGrandfatheringDTO result = policyViolationGrandfatheringService
          .getGrandfathering(OwnerType.ORGANIZATION, org.getId());
      assertPolicyViolationGrandfatheringDTO(result, null, "Root Organization", false, false);
  
      // The parent org allows override, grandfathering is not specified at any level.
      rootOrganization.setAllowPolicyViolationGrandfatheringOverride(true);
      new OrganizationDAO().update(rootOrganization);
      result = policyViolationGrandfatheringService.getGrandfathering(OwnerType.ORGANIZATION, org.getId());
      assertPolicyViolationGrandfatheringDTO(result, null, "Root Organization", false, true);
  
      // The parent org allows override and grandfathering is specified at parent org level.
      rootOrganization.setPolicyViolationGrandfatheringEnabled(true);
      new OrganizationDAO().update(rootOrganization);
      result = policyViolationGrandfatheringService.getGrandfathering(OwnerType.ORGANIZATION, org.getId());
      assertPolicyViolationGrandfatheringDTO(result, true, rootOrganization.getName(), false, true);
  
      // The parent org allows override and grandfathering is specified at this org level.
      org.setPolicyViolationGrandfatheringEnabled(false);
      new OrganizationDAO().update(org);
      result = policyViolationGrandfatheringService.getGrandfathering(OwnerType.ORGANIZATION, org.getId());
      assertPolicyViolationGrandfatheringDTO(result, false, null, false, true);

      // The parent org doesn't allow override, grandfathering is specified at this org level.
      rootOrganization.setPolicyViolationGrandfatheringEnabled(null);
      rootOrganization.setAllowPolicyViolationGrandfatheringOverride(false);
      new OrganizationDAO().update(rootOrganization);
      result = policyViolationGrandfatheringService.getGrandfathering(OwnerType.ORGANIZATION, org.getId());
      assertPolicyViolationGrandfatheringDTO(result, null, rootOrganization.getName(), false, false);

      // The parent org doesn't allow override, this org allows override.
      org.setAllowPolicyViolationGrandfatheringOverride(true);
      new OrganizationDAO().update(org);
      result = policyViolationGrandfatheringService.getGrandfathering(OwnerType.ORGANIZATION, org.getId());
      assertPolicyViolationGrandfatheringDTO(result, null, rootOrganization.getName(), true, false);
    }
    finally {
      rootOrganization.setPolicyViolationGrandfatheringEnabled(grandfatheringEnabled);
      rootOrganization.setAllowPolicyViolationGrandfatheringOverride(grandfatheringOverrideEnabled);
      new OrganizationDAO().update(rootOrganization);
    }
  }

  private void assertPolicyViolationGrandfatheringDTO(PolicyViolationGrandfatheringDTO actual,
                                                      Boolean expectedEnabled,
                                                      String expectedInheritedFromOrganizationName,
                                                      boolean expectedAllowOverride,
                                                      boolean expectedAllowChange)
  {
    assertThat("enabled", actual.enabled, is(expectedEnabled));
    assertThat("inheritedFromOrganizationName", actual.inheritedFromOrganizationName,
        is(expectedInheritedFromOrganizationName));
    assertThat("allowOverride", actual.allowOverride, is(expectedAllowOverride));
    assertThat("allowChange", actual.allowChange, is(expectedAllowChange));
  }

  @Test
  public void testSetGrandfathering_Application() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    app.setPolicyViolationGrandfatheringEnabled(false);
    new ApplicationDAO().update(app);

    // Set to not null value
    PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO = new PolicyViolationGrandfatheringDTO();
    policyViolationGrandfatheringDTO.enabled = true;
    policyViolationGrandfatheringService.setGrandfathering(OwnerType.APPLICATION, app.getPublicId(),
        policyViolationGrandfatheringDTO);

    assertThat(new ApplicationDAO().getById(app.getId()).isPolicyViolationGrandfatheringEnabled(), is(true));

    // Set to null value
    policyViolationGrandfatheringDTO.enabled = null;
    policyViolationGrandfatheringService.setGrandfathering(OwnerType.APPLICATION, app.getPublicId(),
        policyViolationGrandfatheringDTO);

    assertThat(new ApplicationDAO().getById(app.getId()).isPolicyViolationGrandfatheringEnabled(), is(nullValue()));
  }

  @Test
  public void testSetGrandfathering_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    org.setPolicyViolationGrandfatheringEnabled(false);
    org.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(org);

    // Set to not null value
    PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO = new PolicyViolationGrandfatheringDTO();
    policyViolationGrandfatheringDTO.enabled = true;
    policyViolationGrandfatheringDTO.allowOverride = true;
    policyViolationGrandfatheringService.setGrandfathering(OwnerType.ORGANIZATION, org.getId(),
        policyViolationGrandfatheringDTO);

    org = new OrganizationDAO().getById(org.getId());
    assertThat(org.isPolicyViolationGrandfatheringEnabled(), is(true));
    assertThat(org.isAllowPolicyViolationGrandfatheringOverride(), is(true));

    // Set to null value
    policyViolationGrandfatheringDTO.enabled = null;
    policyViolationGrandfatheringService.setGrandfathering(OwnerType.ORGANIZATION, org.getId(),
        policyViolationGrandfatheringDTO);

    org = new OrganizationDAO().getById(org.getId());
    assertThat(org.isPolicyViolationGrandfatheringEnabled(), is(nullValue()));
    assertThat(org.isAllowPolicyViolationGrandfatheringOverride(), is(true));
  }
}
