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

import jakarta.inject.Inject;

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
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class LegacyViolationServiceTest
    extends AbstractComponentH2Test
{
  public LogOutput policyViolationLoggerOutput =
      new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Inject
  private LegacyViolationService legacyViolationService;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

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
    PolicyViolation fixedLegacyViolation =
        tempEntity.newLegacyPolicyViolation(policyEvaluationApp1, policyFixed);
    fixedLegacyViolation.setFixTime(new Date());
    policyViolationDAO.update(fixedLegacyViolation);
    PolicyViolation legacyViolation1 =
        tempEntity.newLegacyPolicyViolation(policyEvaluationApp1, policyForLegacyViolation);
    PolicyEvaluation policyEvaluationApp2 = tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, "scanId2");
    PolicyViolation legacyViolation2 =
        tempEntity.newLegacyPolicyViolation(policyEvaluationApp2, policyForLegacyViolation);

    Date before = new Date();
    legacyViolationService.revokeLegacyViolationStatus(app1.getPublicId());
    Date after = new Date();

    assertThat(policyViolationDAO.getById(fixedLegacyViolation.getId()).isLegacyViolation()).isTrue();
    assertThat(policyViolationDAO.getById(legacyViolation1.getId()).isLegacyViolation()).isFalse();
    assertThat(policyViolationDAO.getById(legacyViolation2.getId()).isLegacyViolation()).isTrue();

    assertPolicyViolationsLogged(PolicyViolationLogEvent.UNGRANDFATHER, app1, before, after,
        Collections.singletonList(legacyViolation1), currentUser.getUsernameOrSystem());
    assertPolicyViolationsLogged(PolicyViolationLogEvent.REVOKE_LEGACY_STATUS, app1, before, after,
        Collections.singletonList(legacyViolation1), currentUser.getUsernameOrSystem());
  }

  @Test
  public void testRevokeLegacyViolationStatus_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> legacyViolationService.revokeLegacyViolationStatus("APPID"));
  }

  private Policy newPolicyAllowingLegacyViolations() {
    Policy policy = tempEntity.newPolicy();
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);
    return policy;
  }

  private void testGrantLegacyViolationStatus(Application app, boolean legacyViolationsAllowed) throws Exception {
    Policy policyFixed = newPolicyAllowingLegacyViolations();
    Policy policyUnfixed = newPolicyAllowingLegacyViolations();
    Policy policyForLegacyViolation = newPolicyAllowingLegacyViolations();
    Policy policyWaived = newPolicyAllowingLegacyViolations();
    Policy policyDoesNotExist = tempEntity.newPolicy();

    PolicyEvaluation policyEvaluationApp1 = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId1");
    PolicyViolation unfixedPolicyViolation1 = tempEntity.newPolicyViolation(policyEvaluationApp1, policyUnfixed);
    PolicyViolation fixedPolicyViolation1 = tempEntity.newPolicyViolation(policyEvaluationApp1, policyFixed);
    fixedPolicyViolation1.setFixTime(new Date());
    policyViolationDAO.update(fixedPolicyViolation1);
    PolicyViolation legacyViolation1 = tempEntity.newPolicyViolation(policyEvaluationApp1, policyForLegacyViolation);
    Date inThePast = new Date(System.currentTimeMillis() - 1);
    legacyViolation1.setLegacyViolationTime(inThePast);
    policyViolationDAO.update(legacyViolation1);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policyWaived.getId(), app.getId());
    PolicyViolation waivedPolicyViolation1 =
        tempEntity.newWaivedPolicyViolation(policyEvaluationApp1, policyWaived, policyWaiver);
    PolicyViolation unfixedPolicyViolation1PolicyDoesNotExist =
        tempEntity.newPolicyViolation(policyEvaluationApp1, policyDoesNotExist);
    policyDAO.delete(policyDoesNotExist);

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

    assertThat(policyViolationDAO.getById(fixedPolicyViolation1.getId()).isLegacyViolation()).isFalse();
    assertThat(policyViolationDAO.getById(legacyViolation1.getId()).getLegacyViolationTime())
        .isEqualTo(inThePast);
    unfixedPolicyViolation1 = policyViolationDAO.getById(unfixedPolicyViolation1.getId());
    waivedPolicyViolation1 = policyViolationDAO.getById(waivedPolicyViolation1.getId());
    unfixedPolicyViolation1PolicyDoesNotExist =
        policyViolationDAO.getById(unfixedPolicyViolation1PolicyDoesNotExist.getId());
    if (legacyViolationsAllowed) {
      assertPolicyLegacyViolationTime(unfixedPolicyViolation1, before, after);
      assertPolicyLegacyViolationTime(waivedPolicyViolation1, before, after);
      assertPolicyLegacyViolationTime(unfixedPolicyViolation1PolicyDoesNotExist, before, after);

      Date legacyViolationTime = unfixedPolicyViolation1.getLegacyViolationTime();
      assertPolicyViolationsLogged(PolicyViolationLogEvent.GRANDFATHER, app, legacyViolationTime,
          legacyViolationTime,
          Arrays.asList(unfixedPolicyViolation1, waivedPolicyViolation1, unfixedPolicyViolation1PolicyDoesNotExist),
          currentUser.getUsernameOrSystem());
      assertPolicyViolationsLogged(PolicyViolationLogEvent.GRANT_LEGACY_STATUS, app, legacyViolationTime,
          legacyViolationTime,
          Arrays.asList(unfixedPolicyViolation1, waivedPolicyViolation1, unfixedPolicyViolation1PolicyDoesNotExist),
          currentUser.getUsernameOrSystem());
    }
    else {
      assertThat(unfixedPolicyViolation1.isLegacyViolation()).isFalse();
      assertThat(unfixedPolicyViolation1PolicyDoesNotExist.isLegacyViolation()).isFalse();
    }

    assertThat(policyViolationDAO.getById(unfixedPolicyViolation2.getId()).isLegacyViolation()).isFalse();
  }

  private void assertPolicyViolationsLogged(
      PolicyViolationLogEvent policyViolationLogEvent,
      Application app,
      Date before,
      Date after,
      List<PolicyViolation> policyViolations,
      String userName) throws Exception
  {
    policyViolationDAO.loadConstraintFacts(policyViolations);
    Organization org = organizationDAO.getById(app.getOrganizationId());
    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(policyViolationLoggerOutput, policyViolationLogEvent, policyViolations.size());
    PolicyViolationLogDTOAssert.assertApplicationPolicyViolationData(policyViolationLogDTOs, policyViolationLogEvent,
        org, app, before, after, policyViolations, userName);
  }

  @Test
  public void testGrantLegacyViolationStatus_LegacyViolationsNotConfiguredForAppOrOrg() throws Exception {
    Organization organization = tempEntity.newOrganization();
    organization.setLegacyViolationEnabled(null);
    organization.setAllowLegacyViolationOverride(true);
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setLegacyViolationEnabled(null);
    applicationDAO.update(application);
    testGrantLegacyViolationStatus(application, false);
  }

  @Test
  public void testGrantLegacyViolationStatus_LegacyViolationsEnabledForApp_AppCanOverrideLegacyViolations() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Organization organization = tempEntity.newOrganization();
    organization.setLegacyViolationEnabled(false);
    organization.setAllowLegacyViolationOverride(true);
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);
    testGrantLegacyViolationStatus(application, true);
  }

  @Test
  public void testGrantLegacyViolationStatus_LegacyViolationsDisabledForApp_AppCanOverrideLegacyViolations() throws Exception {
    Organization organization = tempEntity.newOrganization();
    organization.setLegacyViolationEnabled(true);
    organization.setAllowLegacyViolationOverride(true);
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setLegacyViolationEnabled(false);
    applicationDAO.update(application);
    testGrantLegacyViolationStatus(application, false);
  }

  @Test
  public void testGrantLegacyViolationStatus_LegacyViolationsEnabledForApp_DisabledForOrg_AppCannotOverrideLegacy() throws Exception {
    Organization organization = tempEntity.newOrganization();
    organization.setLegacyViolationEnabled(false);
    organization.setAllowLegacyViolationOverride(false);
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);
    testGrantLegacyViolationStatus(application, false);
  }

  @Test
  public void testGrantLegacyViolationStatus_LegacyViolationDisabledForApp_EnabledForOrg_AppCannotOverrideLegacyStatus() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Organization organization = tempEntity.newOrganization();
    organization.setLegacyViolationEnabled(true);
    organization.setAllowLegacyViolationOverride(false);
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setLegacyViolationEnabled(false);
    applicationDAO.update(application);
    testGrantLegacyViolationStatus(application, true);
  }

  @Test
  public void testGrantLegacyViolationStatus_LegacyViolationsEnabledForApp_EnabledForOrg_AppCannotOverrideLegacyStatus() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Organization organization = tempEntity.newOrganization();
    organization.setLegacyViolationEnabled(true);
    organization.setLegacyViolationEnabled(false);
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);
    testGrantLegacyViolationStatus(application, true);
  }

  @Test
  public void testGrantLegacyViolationStatus_LegacyViolationsDisabledForApp_DisabledForOrg_AppCannotOverrideLegacy() throws Exception {
    Organization organization = tempEntity.newOrganization();
    organization.setLegacyViolationEnabled(false);
    organization.setAllowLegacyViolationOverride(false);
    organizationDAO.update(organization);
    Application application = tempEntity.newApplication(organization.getId());
    application.setLegacyViolationEnabled(false);
    applicationDAO.update(application);
    testGrantLegacyViolationStatus(application, false);
  }

  @Test
  public void testGrantLegacyViolationStatus_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> legacyViolationService.grantLegacyViolationStatus("APPID"));
  }

  private void assertPolicyLegacyViolationTime(PolicyViolation policyViolation, Date before, Date after) {
    assertThat(policyViolation.getLegacyViolationTime()).isAfterOrEqualTo(before);
    assertThat(policyViolation.getLegacyViolationTime()).isBeforeOrEqualTo(after);
  }

  @Test
  public void testGetLegacyViolationsStatus_Application() {
    // The parent org doesn't allow override, legacy violations are not specified at any level.
    Organization org = tempEntity.newOrganization();
    org.setAllowLegacyViolationOverride(false);
    organizationDAO.update(org);
    Application app = tempEntity.newApplication(org.getId());
    LegacyViolationStatusDTO result =
        legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
    assertLegacyViolationDTO(result, null, null, "Root Organization", false, false);

    // The parent org allows override, legacy violations are not specified at any level.
    org.setAllowLegacyViolationOverride(true);
    organizationDAO.update(org);
    result = legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
    assertLegacyViolationDTO(result, null, null, "Root Organization", false, true);

    // The parent org allows override and legacy violations are specified at org level as true.
    org.setLegacyViolationEnabled(true);
    organizationDAO.update(org);
    result = legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
    assertLegacyViolationDTO(result, true, true, org.getName(), false, true);

    // The parent org allows override and legacy violations are specified at org level as false.
    app.setLegacyViolationEnabled(true);
    applicationDAO.update(app);
    org.setLegacyViolationEnabled(false);
    organizationDAO.update(org);
    result = legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
    assertLegacyViolationDTO(result, true, false, null, false, true);

    // The parent org allows override and legacy violations are specified at app and org level.
    org.setLegacyViolationEnabled(null);
    organizationDAO.update(org);
    app.setLegacyViolationEnabled(false);
    applicationDAO.update(app);
    result = legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
    assertLegacyViolationDTO(result, false, null, null, false, true);

    // The parent org doesn't allow override, legacy violations are specified at app level.
    org.setLegacyViolationEnabled(null);
    org.setAllowLegacyViolationOverride(false);
    organizationDAO.update(org);
    app.setLegacyViolationEnabled(true);
    applicationDAO.update(app);
    result = legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
    assertLegacyViolationDTO(result, null, null, "Root Organization", false, false);
  }

  @Test
  public void testgetLegacyViolationsStatus_Organization() {
    Organization rootOrganization = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    Boolean legacyViolationsEnabled = rootOrganization.isLegacyViolationEnabled();
    boolean legacyViolationsOverrideEnabled = rootOrganization.isAllowLegacyViolationOverride();
    try {
      // The parent org doesn't allow override, legacy violations are not specified at any level.
      rootOrganization.setAllowLegacyViolationOverride(false);
      organizationDAO.update(rootOrganization);
      Organization org = tempEntity.newOrganization();
      org.setAllowLegacyViolationOverride(false);
      organizationDAO.update(org);
      LegacyViolationStatusDTO result =
          legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
      assertLegacyViolationDTO(result, null, null, "Root Organization", false, false);

      // The parent org allows override, legacy violations are not specified at any level.
      rootOrganization.setAllowLegacyViolationOverride(true);
      organizationDAO.update(rootOrganization);
      result = legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
      assertLegacyViolationDTO(result, null, null, "Root Organization", false, true);

      // The parent org allows override and legacy violations are specified at parent org level.
      rootOrganization.setLegacyViolationEnabled(true);
      organizationDAO.update(rootOrganization);
      result = legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
      assertLegacyViolationDTO(result, true, true, rootOrganization.getName(), false, true);

      // The parent org allows override and legacy violations are specified at this org level.
      org.setLegacyViolationEnabled(false);
      organizationDAO.update(org);
      result = legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
      assertLegacyViolationDTO(result, false, true, null, false, true);

      // The parent org doesn't allow override, legacy violations are specified at this org level.
      rootOrganization.setLegacyViolationEnabled(null);
      rootOrganization.setAllowLegacyViolationOverride(false);
      organizationDAO.update(rootOrganization);
      result = legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
      assertLegacyViolationDTO(result, null, null, rootOrganization.getName(), false, false);

      // The parent org doesn't allow override, this org allows override.
      org.setAllowLegacyViolationOverride(true);
      organizationDAO.update(org);
      result = legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
      assertLegacyViolationDTO(result, null, null, rootOrganization.getName(), true, false);
    }
    finally {
      rootOrganization.setLegacyViolationEnabled(legacyViolationsEnabled);
      rootOrganization.setAllowLegacyViolationOverride(legacyViolationsOverrideEnabled);
      organizationDAO.update(rootOrganization);
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
    applicationDAO.update(app);

    // Set to not null value
    LegacyViolationStatusDTO legacyViolationStatusDTO = new LegacyViolationStatusDTO();
    legacyViolationStatusDTO.enabled = true;
    legacyViolationService.setLegacyViolationStatus(OwnerType.APPLICATION, app.getPublicId(),
        legacyViolationStatusDTO);

    assertThat(applicationDAO.getById(app.getId()).isLegacyViolationEnabled()).isTrue();

    // Set to null value
    legacyViolationStatusDTO.enabled = null;
    legacyViolationService.setLegacyViolationStatus(OwnerType.APPLICATION, app.getPublicId(),
        legacyViolationStatusDTO);

    assertThat(applicationDAO.getById(app.getId()).isLegacyViolationEnabled()).isNull();
  }

  @Test
  public void testSetLegacyViolationStatus_Application_ComplianceStage() {
    Application app = tempEntity.newApplicationWithParent();
    app.setLegacyViolationEnabled(true);
    applicationDAO.update(app);
    boolean isLegacyViolation = legacyViolationService.isLegacyViolationEnabled(app.getId(), "compliance");

    assertThat(isLegacyViolation).isFalse();
  }

  @Test
  public void testSetLegacyViolationStatus_Organization() {
    Organization org = tempEntity.newOrganization();
    org.setAllowLegacyViolationOverride(false);
    org.setAllowLegacyViolationOverride(false);
    organizationDAO.update(org);

    // Set to not null value
    LegacyViolationStatusDTO legacyViolationStatusDTO = new LegacyViolationStatusDTO();
    legacyViolationStatusDTO.enabled = true;
    legacyViolationStatusDTO.allowOverride = true;
    legacyViolationService.setLegacyViolationStatus(OwnerType.ORGANIZATION, org.getId(),
        legacyViolationStatusDTO);

    org = organizationDAO.getById(org.getId());
    assertThat(org.isLegacyViolationEnabled()).isTrue();
    assertThat(org.isAllowLegacyViolationOverride()).isTrue();

    // Set to null value
    legacyViolationStatusDTO.enabled = null;
    legacyViolationService.setLegacyViolationStatus(OwnerType.ORGANIZATION, org.getId(),
        legacyViolationStatusDTO);

    org = organizationDAO.getById(org.getId());
    assertThat(org.isLegacyViolationEnabled()).isNull();
    assertThat(org.isAllowLegacyViolationOverride()).isTrue();
  }

  @Test
  public void testSetLegacyViolationStatus_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);
    Organization org = tempEntity.newOrganization();
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> legacyViolationService.setLegacyViolationStatus(OwnerType.ORGANIZATION, org.getId(),
            new LegacyViolationStatusDTO()));
  }

  @Test
  public void testGrantLegacyViolationStatus_ProxyStageViolationsNotMarkedAsLegacy() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    app.setLegacyViolationEnabled(true);
    applicationDAO.update(app);

    Policy buildStagePolicy = newPolicyAllowingLegacyViolations();
    Policy proxyStagePolicy = newPolicyAllowingLegacyViolations();

    PolicyEvaluation buildEvaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanId1");
    PolicyViolation buildViolation = tempEntity.newPolicyViolation(buildEvaluation, buildStagePolicy);

    PolicyEvaluation proxyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_PROXY, "scanId2");
    PolicyViolation proxyViolation = tempEntity.newPolicyViolation(proxyEvaluation, proxyStagePolicy);

    legacyViolationService.grantLegacyViolationStatus(app.getPublicId());

    buildViolation = policyViolationDAO.getById(buildViolation.getId());
    proxyViolation = policyViolationDAO.getById(proxyViolation.getId());

    assertThat(buildViolation.getLegacyViolationTime()).isNotNull();
    assertThat(proxyViolation.getLegacyViolationTime()).isNull();
  }
}
