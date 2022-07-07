/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class PolicyViolationGrandfatheringServiceTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput policyViolationLoggerOutput =
      new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Inject
  private PolicyViolationGrandfatheringService policyViolationGrandfatheringService;

  @Inject
  private TestProductLicense testProductLicense;

  @Test
  public void testRevokeGrandfathering() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();
    Policy policyGrandfathered = tempEntity.newPolicy();
    Policy policyFixed = tempEntity.newPolicy();
    PolicyEvaluation policyEvaluationApp1 = tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scanId1");
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyViolation fixedGrandfatheredPolicyViolation =
        tempEntity.newGrandfatheredPolicyViolation(policyEvaluationApp1, policyFixed);
    fixedGrandfatheredPolicyViolation.setFixTime(new Date());
    policyViolationDAO.update(fixedGrandfatheredPolicyViolation);
    PolicyViolation grandfatheredPolicyViolation1 =
        tempEntity.newGrandfatheredPolicyViolation(policyEvaluationApp1, policyGrandfathered);
    PolicyEvaluation policyEvaluationApp2 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, "scanId2");
    PolicyViolation grandfatheredPolicyViolation2 =
        tempEntity.newGrandfatheredPolicyViolation(policyEvaluationApp2, policyGrandfathered);

    Date before = new Date();
    policyViolationGrandfatheringService.revokeGrandfathering(app1.getPublicId());
    Date after = new Date();

    assertThat(policyViolationDAO.getById(fixedGrandfatheredPolicyViolation.getId()).isGrandfathered()).isTrue();
    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation1.getId()).isGrandfathered()).isFalse();
    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation2.getId()).isGrandfathered()).isTrue();

    assertPolicyViolationsLogged(PolicyViolationLogEvent.UNGRANDFATHER, app1, before, after,
        Collections.singletonList(grandfatheredPolicyViolation1));
  }

  @Test
  public void testRevokeGrandfathering_MissingLicenseFeature() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        policyViolationGrandfatheringService.revokeGrandfathering("APPID")
    );
  }
  
  private Policy newPolicyAllowingGrandfathering() {
    Policy policy = tempEntity.newPolicy();
    policy.setPolicyViolationGrandfatheringAllowed(true);
    new PolicyDAO().update(policy);
    return policy;
  }

  private void testGrandfather(Application app, boolean grandfatheringAllowed) throws Exception {
    Policy policyFixed = newPolicyAllowingGrandfathering();
    Policy policyUnfixed = newPolicyAllowingGrandfathering();
    Policy policyGrandfathered = newPolicyAllowingGrandfathering();
    Policy policyWaived = newPolicyAllowingGrandfathering();
    Policy policyDoesNotExist = tempEntity.newPolicy();

    PolicyEvaluation policyEvaluationApp1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId1");
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyViolation unfixedPolicyViolation1 = tempEntity.newPolicyViolation(policyEvaluationApp1, policyUnfixed);
    PolicyViolation fixedPolicyViolation1 = tempEntity.newPolicyViolation(policyEvaluationApp1, policyFixed);
    fixedPolicyViolation1.setFixTime(new Date());
    policyViolationDAO.update(fixedPolicyViolation1);
    PolicyViolation grandfatheredPolicyViolation1 =
        tempEntity.newPolicyViolation(policyEvaluationApp1, policyGrandfathered);
    Date inThePast = new Date(System.currentTimeMillis() - 1);
    grandfatheredPolicyViolation1.setGrandfatherTime(inThePast);
    policyViolationDAO.update(grandfatheredPolicyViolation1);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policyWaived.getId(), app.getId());
    PolicyViolation waivedPolicyViolation1 =
        tempEntity.newWaivedPolicyViolation(policyEvaluationApp1, policyWaived, policyWaiver);
    PolicyViolation unfixedPolicyViolation1PolicyDoesNotExist =
        tempEntity.newPolicyViolation(policyEvaluationApp1, policyDoesNotExist);
    new PolicyDAO().delete(policyDoesNotExist);

    Application app2 = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluationApp2 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, "scanId2");
    PolicyViolation unfixedPolicyViolation2 = tempEntity.newPolicyViolation(policyEvaluationApp2, policyUnfixed);

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
      assertThat(e.getMessage())
          .isEqualTo("Policy violation grandfathering is not enabled for application '" + app.getName() + "'.");
    }
    Date after = new Date();

    assertThat(policyViolationDAO.getById(fixedPolicyViolation1.getId()).isGrandfathered()).isFalse();
    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation1.getId()).getGrandfatherTime())
        .isEqualTo(inThePast);
    unfixedPolicyViolation1 = policyViolationDAO.getById(unfixedPolicyViolation1.getId());
    waivedPolicyViolation1 = policyViolationDAO.getById(waivedPolicyViolation1.getId());
    unfixedPolicyViolation1PolicyDoesNotExist =
        policyViolationDAO.getById(unfixedPolicyViolation1PolicyDoesNotExist.getId());
    if (grandfatheringAllowed) {
      assertPolicyViolationGrandfatherTime(unfixedPolicyViolation1, before, after);
      assertPolicyViolationGrandfatherTime(waivedPolicyViolation1, before, after);
      assertPolicyViolationGrandfatherTime(unfixedPolicyViolation1PolicyDoesNotExist, before, after);

      Date grandfatherTime = unfixedPolicyViolation1.getGrandfatherTime();
      assertPolicyViolationsLogged(PolicyViolationLogEvent.GRANDFATHER, app, grandfatherTime, grandfatherTime,
          Arrays.asList(unfixedPolicyViolation1, waivedPolicyViolation1, unfixedPolicyViolation1PolicyDoesNotExist));
    }
    else {
      assertThat(unfixedPolicyViolation1.isGrandfathered()).isFalse();
      assertThat(waivedPolicyViolation1.isGrandfathered()).isFalse();
      assertThat(unfixedPolicyViolation1PolicyDoesNotExist.isGrandfathered()).isFalse();
    }

    assertThat(policyViolationDAO.getById(unfixedPolicyViolation2.getId()).isGrandfathered()).isFalse();
  }

  private void assertPolicyViolationsLogged(PolicyViolationLogEvent policyViolationLogEvent,
                                            Application app,
                                            Date before,
                                            Date after,
                                            List<PolicyViolation> policyViolations) throws Exception
  {
    Organization org = new OrganizationDAO().getById(app.getOrganizationId());
    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(policyViolationLoggerOutput, policyViolationLogEvent, policyViolations.size());
    PolicyViolationLogDTOAssert.assertApplicationPolicyViolationData(policyViolationLogDTOs, policyViolationLogEvent,
        org, app, before, after, policyViolations);
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

  @Test
  public void testGrandfather_MissingLicenseFeature() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> policyViolationGrandfatheringService.grandfather("APPID"));
  }

  private void assertPolicyViolationGrandfatherTime(PolicyViolation policyViolation, Date before, Date after) {
    assertThat(policyViolation.getGrandfatherTime()).isAfterOrEqualTo(before);
    assertThat(policyViolation.getGrandfatherTime()).isBeforeOrEqualTo(after);
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
    assertThat(actual.enabled).as("enabled").isEqualTo(expectedEnabled);
    assertThat(actual.inheritedFromOrganizationName).as("inheritedFromOrganizationName")
        .isEqualTo(expectedInheritedFromOrganizationName);
    assertThat(actual.allowOverride).as("allowOverride").isEqualTo(expectedAllowOverride);
    assertThat(actual.allowChange).as("allowChange").isEqualTo(expectedAllowChange);
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

    assertThat(new ApplicationDAO().getById(app.getId()).isPolicyViolationGrandfatheringEnabled()).isTrue();

    // Set to null value
    policyViolationGrandfatheringDTO.enabled = null;
    policyViolationGrandfatheringService.setGrandfathering(OwnerType.APPLICATION, app.getPublicId(),
        policyViolationGrandfatheringDTO);

    assertThat(new ApplicationDAO().getById(app.getId()).isPolicyViolationGrandfatheringEnabled()).isNull();
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
    assertThat(org.isPolicyViolationGrandfatheringEnabled()).isTrue();
    assertThat(org.isAllowPolicyViolationGrandfatheringOverride()).isTrue();

    // Set to null value
    policyViolationGrandfatheringDTO.enabled = null;
    policyViolationGrandfatheringService.setGrandfathering(OwnerType.ORGANIZATION, org.getId(),
        policyViolationGrandfatheringDTO);

    org = new OrganizationDAO().getById(org.getId());
    assertThat(org.isPolicyViolationGrandfatheringEnabled()).isNull();
    assertThat(org.isAllowPolicyViolationGrandfatheringOverride()).isTrue();
  }

  @Test
  public void testSetGrandfathering_MissingLicenseFeature() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);
    Organization org = tempEntity.newOrganization();
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        policyViolationGrandfatheringService.setGrandfathering(OwnerType.ORGANIZATION, org.getId(),
            new PolicyViolationGrandfatheringDTO())
    );
  }
}
