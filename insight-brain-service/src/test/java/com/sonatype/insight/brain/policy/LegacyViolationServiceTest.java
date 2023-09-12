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
import com.sonatype.insight.brain.policy.LegacyViolationService.LegacyViolationStatusDTO;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

public class LegacyViolationServiceTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput policyViolationLoggerOutput =
      new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Inject
  private LegacyViolationService legacyViolationService;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private CurrentUser currentUser;

  @Test
  public void testRevokeLegacyViolationStatus() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();
    Policy policyForLegacyViolation = tempEntity.newPolicy();
    Policy policyFixed = tempEntity.newPolicy();
    PolicyEvaluation policyEvaluationApp1 = tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scanId1");
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyViolation fixedLegacyViolation =
        tempEntity.newGrandfatheredPolicyViolation(policyEvaluationApp1, policyFixed);
    fixedLegacyViolation.setFixTime(new Date());
    policyViolationDAO.update(fixedLegacyViolation);
    PolicyViolation legacyViolation1 =
        tempEntity.newGrandfatheredPolicyViolation(policyEvaluationApp1, policyForLegacyViolation);
    PolicyEvaluation policyEvaluationApp2 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, "scanId2");
    PolicyViolation legacyViolation2 =
        tempEntity.newGrandfatheredPolicyViolation(policyEvaluationApp2, policyForLegacyViolation);

    Date before = new Date();
    legacyViolationService.revokeLegacyViolationStatus(app1.getPublicId());
    Date after = new Date();

    assertThat(policyViolationDAO.getById(fixedLegacyViolation.getId()).isGrandfathered()).isTrue();
    assertThat(policyViolationDAO.getById(legacyViolation1.getId()).isGrandfathered()).isFalse();
    assertThat(policyViolationDAO.getById(legacyViolation2.getId()).isGrandfathered()).isTrue();

    assertPolicyViolationsLogged(PolicyViolationLogEvent.UNGRANDFATHER, app1, before, after,
        Collections.singletonList(legacyViolation1), currentUser.getUsernameOrSystem());
  }

  @Test
  public void testRevokeLegacyViolationStatus_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        legacyViolationService.revokeLegacyViolationStatus("APPID")
    );
  }
  
  private Policy newPolicyAllowingLegacyViolations() {
    Policy policy = tempEntity.newPolicy();
    policy.setPolicyViolationGrandfatheringAllowed(true);
    new PolicyDAO().update(policy);
    return policy;
  }

  private void testGrantLegacyViolationStatus(Application app, boolean legacyViolationsAllowed) throws Exception {
    Policy policyFixed = newPolicyAllowingLegacyViolations();
    Policy policyUnfixed = newPolicyAllowingLegacyViolations();
    Policy policyForLegacyViolation = newPolicyAllowingLegacyViolations();
    Policy policyWaived = newPolicyAllowingLegacyViolations();
    Policy policyDoesNotExist = tempEntity.newPolicy();

    PolicyEvaluation policyEvaluationApp1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId1");
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyViolation unfixedPolicyViolation1 = tempEntity.newPolicyViolation(policyEvaluationApp1, policyUnfixed);
    PolicyViolation fixedPolicyViolation1 = tempEntity.newPolicyViolation(policyEvaluationApp1, policyFixed);
    fixedPolicyViolation1.setFixTime(new Date());
    policyViolationDAO.update(fixedPolicyViolation1);
    PolicyViolation legacyViolation1 = tempEntity.newPolicyViolation(policyEvaluationApp1, policyForLegacyViolation);
    Date inThePast = new Date(System.currentTimeMillis() - 1);
    legacyViolation1.setGrandfatherTime(inThePast);
    policyViolationDAO.update(legacyViolation1);
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
      legacyViolationService.grantLegacyViolationStatus(app.getPublicId());
      if (!legacyViolationsAllowed) {
        fail("Expected exception");
      }
    }
    catch (BadRequestException e) {
      if (legacyViolationsAllowed) {
        throw e;
      }
      assertThat(e.getMessage())
          .isEqualTo("Legacy violations are not enabled for application '" + app.getName() + "'.");
    }
    Date after = new Date();

    assertThat(policyViolationDAO.getById(fixedPolicyViolation1.getId()).isGrandfathered()).isFalse();
    assertThat(policyViolationDAO.getById(legacyViolation1.getId()).getGrandfatherTime())
        .isEqualTo(inThePast);
    unfixedPolicyViolation1 = policyViolationDAO.getById(unfixedPolicyViolation1.getId());
    waivedPolicyViolation1 = policyViolationDAO.getById(waivedPolicyViolation1.getId());
    unfixedPolicyViolation1PolicyDoesNotExist =
        policyViolationDAO.getById(unfixedPolicyViolation1PolicyDoesNotExist.getId());
    if (legacyViolationsAllowed) {
      assertPolicyViolationGrandfatherTime(unfixedPolicyViolation1, before, after);
      assertPolicyViolationGrandfatherTime(waivedPolicyViolation1, before, after);
      assertPolicyViolationGrandfatherTime(unfixedPolicyViolation1PolicyDoesNotExist, before, after);

      Date grandfatherTime = unfixedPolicyViolation1.getGrandfatherTime();
      assertPolicyViolationsLogged(PolicyViolationLogEvent.GRANDFATHER, app, grandfatherTime, grandfatherTime,
          Arrays.asList(unfixedPolicyViolation1, waivedPolicyViolation1, unfixedPolicyViolation1PolicyDoesNotExist),
          currentUser.getUsernameOrSystem());
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
                                            List<PolicyViolation> policyViolations,
                                            String userName) throws Exception
  {
    Organization org = new OrganizationDAO().getById(app.getOrganizationId());
    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(policyViolationLoggerOutput, policyViolationLogEvent, policyViolations.size());
    PolicyViolationLogDTOAssert.assertApplicationPolicyViolationData(policyViolationLogDTOs, policyViolationLogEvent,
        org, app, before, after, policyViolations, userName);
  }

  @Test
  public void testGrantLegacyViolationStatus_LegacyViolationsNotConfiguredForAppOrOrg() throws Exception {
    Organization organization = tempEntity.newOrganization();
    organization.setPolicyViolationGrandfatheringEnabled(null);
    organization.setAllowPolicyViolationGrandfatheringOverride(true);
    new OrganizationDAO().update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setPolicyViolationGrandfatheringEnabled(null);
    new ApplicationDAO().update(application);
    testGrantLegacyViolationStatus(application, false);
  }

  @Test
  public void testGrantLegacyViolationStatus_LegacyViolationsEnabledForApp_AppCanOverrideLegacyViolations()
      throws Exception
  {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Organization organization = tempEntity.newOrganization();
    organization.setPolicyViolationGrandfatheringEnabled(false);
    organization.setAllowPolicyViolationGrandfatheringOverride(true);
    new OrganizationDAO().update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);
    testGrantLegacyViolationStatus(application, true);
  }

  @Test
  public void testGrantLegacyViolationStatus_LegacyViolationsDisabledForApp_AppCanOverrideLegacyViolations()
      throws Exception
  {
    Organization organization = tempEntity.newOrganization();
    organization.setPolicyViolationGrandfatheringEnabled(true);
    organization.setAllowPolicyViolationGrandfatheringOverride(true);
    new OrganizationDAO().update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setPolicyViolationGrandfatheringEnabled(false);
    new ApplicationDAO().update(application);
    testGrantLegacyViolationStatus(application, false);
  }

  @Test
  public void testGrantLegacyViolationStatus_LegacyViolationsEnabledForApp_DisabledForOrg_AppCannotOverrideLegacy()
      throws Exception
  {
    Organization organization = tempEntity.newOrganization();
    organization.setPolicyViolationGrandfatheringEnabled(false);
    organization.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);
    testGrantLegacyViolationStatus(application, false);
  }

  @Test
  public void testGrantLegacyViolationStatus_LegacyViolationDisabledForApp_EnabledForOrg_AppCannotOverrideLegacyStatus()
      throws Exception
  {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Organization organization = tempEntity.newOrganization();
    organization.setPolicyViolationGrandfatheringEnabled(true);
    organization.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setPolicyViolationGrandfatheringEnabled(false);
    new ApplicationDAO().update(application);
    testGrantLegacyViolationStatus(application, true);
  }

  @Test
  public void testGrantLegacyViolationStatus_LegacyViolationsEnabledForApp_EnabledForOrg_AppCannotOverrideLegacyStatus()
      throws Exception
  {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Organization organization = tempEntity.newOrganization();
    organization.setPolicyViolationGrandfatheringEnabled(true);
    organization.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);
    testGrantLegacyViolationStatus(application, true);
  }

  @Test
  public void testGrantLegacyViolationStatus_LegacyViolationsDisabledForApp_DisabledForOrg_AppCannotOverrideLegacy()
      throws Exception
  {
    Organization organization = tempEntity.newOrganization();
    organization.setPolicyViolationGrandfatheringEnabled(false);
    organization.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setPolicyViolationGrandfatheringEnabled(false);
    new ApplicationDAO().update(application);
    testGrantLegacyViolationStatus(application, false);
  }

  @Test
  public void testGrantLegacyViolationStatus_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> legacyViolationService.grantLegacyViolationStatus("APPID"));
  }

  private void assertPolicyViolationGrandfatherTime(PolicyViolation policyViolation, Date before, Date after) {
    assertThat(policyViolation.getGrandfatherTime()).isAfterOrEqualTo(before);
    assertThat(policyViolation.getGrandfatherTime()).isBeforeOrEqualTo(after);
  }

  @Test
  public void testGetLegacyViolationsStatus_Application() {
    // The parent org doesn't allow override, legacy violations are not specified at any level.
    Organization org = tempEntity.newOrganization();
    org.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(org);
    Application app = tempEntity.newApplication(org.getId());
    LegacyViolationStatusDTO result =
        legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
    assertLegacyViolationDTO(result, null, null, "Root Organization", false, false);

    // The parent org allows override, legacy violations are not specified at any level.
    org.setAllowPolicyViolationGrandfatheringOverride(true);
    new OrganizationDAO().update(org);
    result = legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
    assertLegacyViolationDTO(result, null, null, "Root Organization", false, true);

    // The parent org allows override and legacy violations are specified at org level as true.
    org.setPolicyViolationGrandfatheringEnabled(true);
    new OrganizationDAO().update(org);
    result = legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
    assertLegacyViolationDTO(result, true, true, org.getName(), false, true);

    // The parent org allows override and legacy violations are specified at org level as false.
    app.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(app);
    org.setPolicyViolationGrandfatheringEnabled(false);
    new OrganizationDAO().update(org);
    result = legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
    assertLegacyViolationDTO(result, true, false, null, false, true);

    // The parent org allows override and legacy violations are specified at app and org level.
    org.setPolicyViolationGrandfatheringEnabled(null);
    new OrganizationDAO().update(org);
    app.setPolicyViolationGrandfatheringEnabled(false);
    new ApplicationDAO().update(app);
    result = legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
    assertLegacyViolationDTO(result, false, null, null, false, true);

    // The parent org doesn't allow override, legacy violations are specified at app level.
    org.setPolicyViolationGrandfatheringEnabled(null);
    org.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(org);
    app.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(app);
    result = legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
    assertLegacyViolationDTO(result, null, null, "Root Organization", false, false);
  }

  @Test
  public void testgetLegacyViolationsStatus_Organization() {
    Organization rootOrganization = new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID);
    Boolean legacyViolationsEnabled = rootOrganization.isPolicyViolationGrandfatheringEnabled();
    boolean legacyViolationsOverrideEnabled = rootOrganization.isAllowPolicyViolationGrandfatheringOverride();
    try {
      // The parent org doesn't allow override, legacy violations are not specified at any level.
      rootOrganization.setAllowPolicyViolationGrandfatheringOverride(false);
      new OrganizationDAO().update(rootOrganization);
      Organization org = tempEntity.newOrganization();
      org.setAllowPolicyViolationGrandfatheringOverride(false);
      new OrganizationDAO().update(org);
      LegacyViolationStatusDTO result =
          legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
      assertLegacyViolationDTO(result, null, null, "Root Organization", false, false);
  
      // The parent org allows override, legacy violations are not specified at any level.
      rootOrganization.setAllowPolicyViolationGrandfatheringOverride(true);
      new OrganizationDAO().update(rootOrganization);
      result = legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
      assertLegacyViolationDTO(result, null, null, "Root Organization", false, true);
  
      // The parent org allows override and legacy violations are specified at parent org level.
      rootOrganization.setPolicyViolationGrandfatheringEnabled(true);
      new OrganizationDAO().update(rootOrganization);
      result = legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
      assertLegacyViolationDTO(result, true, true, rootOrganization.getName(), false, true);
  
      // The parent org allows override and legacy violations are specified at this org level.
      org.setPolicyViolationGrandfatheringEnabled(false);
      new OrganizationDAO().update(org);
      result = legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
      assertLegacyViolationDTO(result, false, true, null, false, true);

      // The parent org doesn't allow override, legacy violations are specified at this org level.
      rootOrganization.setPolicyViolationGrandfatheringEnabled(null);
      rootOrganization.setAllowPolicyViolationGrandfatheringOverride(false);
      new OrganizationDAO().update(rootOrganization);
      result = legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
      assertLegacyViolationDTO(result, null, null, rootOrganization.getName(), false, false);

      // The parent org doesn't allow override, this org allows override.
      org.setAllowPolicyViolationGrandfatheringOverride(true);
      new OrganizationDAO().update(org);
      result = legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
      assertLegacyViolationDTO(result, null, null, rootOrganization.getName(), true, false);
    }
    finally {
      rootOrganization.setPolicyViolationGrandfatheringEnabled(legacyViolationsEnabled);
      rootOrganization.setAllowPolicyViolationGrandfatheringOverride(legacyViolationsOverrideEnabled);
      new OrganizationDAO().update(rootOrganization);
    }
  }

  private void assertLegacyViolationDTO(
      LegacyViolationStatusDTO actual,
      Boolean expectedEnabled,
      Boolean expectedEnabledInParent,
      String expectedInheritedFromOrganizationName,
      boolean expectedAllowOverride,
      boolean expectedAllowChange)
  {
    assertThat(actual.enabled).as("enabled").isEqualTo(expectedEnabled);
    assertThat(actual.enabledInParent).as("enabledInParent").isEqualTo(expectedEnabledInParent);
    assertThat(actual.inheritedFromOrganizationName).as("inheritedFromOrganizationName")
        .isEqualTo(expectedInheritedFromOrganizationName);
    assertThat(actual.allowOverride).as("allowOverride").isEqualTo(expectedAllowOverride);
    assertThat(actual.allowChange).as("allowChange").isEqualTo(expectedAllowChange);
  }

  @Test
  public void testSetLegacyViolationStatus_Application() {
    Application app = tempEntity.newApplicationWithParent();
    app.setPolicyViolationGrandfatheringEnabled(false);
    new ApplicationDAO().update(app);

    // Set to not null value
    LegacyViolationStatusDTO legacyViolationStatusDTO = new LegacyViolationStatusDTO();
    legacyViolationStatusDTO.enabled = true;
    legacyViolationService.setLegacyViolationStatus(OwnerType.APPLICATION, app.getPublicId(),
        legacyViolationStatusDTO);

    assertThat(new ApplicationDAO().getById(app.getId()).isPolicyViolationGrandfatheringEnabled()).isTrue();

    // Set to null value
    legacyViolationStatusDTO.enabled = null;
    legacyViolationService.setLegacyViolationStatus(OwnerType.APPLICATION, app.getPublicId(),
        legacyViolationStatusDTO);

    assertThat(new ApplicationDAO().getById(app.getId()).isPolicyViolationGrandfatheringEnabled()).isNull();
  }

  @Test
  public void testSetLegacyViolationStatus_Organization() {
    Organization org = tempEntity.newOrganization();
    org.setPolicyViolationGrandfatheringEnabled(false);
    org.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(org);

    // Set to not null value
    LegacyViolationStatusDTO legacyViolationStatusDTO = new LegacyViolationStatusDTO();
    legacyViolationStatusDTO.enabled = true;
    legacyViolationStatusDTO.allowOverride = true;
    legacyViolationService.setLegacyViolationStatus(OwnerType.ORGANIZATION, org.getId(),
        legacyViolationStatusDTO);

    org = new OrganizationDAO().getById(org.getId());
    assertThat(org.isPolicyViolationGrandfatheringEnabled()).isTrue();
    assertThat(org.isAllowPolicyViolationGrandfatheringOverride()).isTrue();

    // Set to null value
    legacyViolationStatusDTO.enabled = null;
    legacyViolationService.setLegacyViolationStatus(OwnerType.ORGANIZATION, org.getId(),
        legacyViolationStatusDTO);

    org = new OrganizationDAO().getById(org.getId());
    assertThat(org.isPolicyViolationGrandfatheringEnabled()).isNull();
    assertThat(org.isAllowPolicyViolationGrandfatheringOverride()).isTrue();
  }

  @Test
  public void testSetLegacyViolationStatus_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);
    Organization org = tempEntity.newOrganization();
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() ->
        legacyViolationService.setLegacyViolationStatus(OwnerType.ORGANIZATION, org.getId(),
            new LegacyViolationStatusDTO())
    );
  }
}
