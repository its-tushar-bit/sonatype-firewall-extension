/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationChangeResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLegacyViolationStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApiLegacyViolationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiLegacyViolationService service;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private TestProductLicense testProductLicense;

  @Test
  public void validateLicense_passesWhenFeatureEnabled() {
    service.validateLicense();
  }

  @Test
  public void validateLicense_throwsWhenFeatureDisabled() {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);
    assertThatThrownBy(() -> service.validateLicense())
        .isInstanceOf(InvalidLicenseException.class);
  }

  @Test
  public void listLegacyViolations_returnsEmptyWhenNoViolations() {
    Application app = tempEntity.newApplicationWithParent();

    List<ApiPolicyViolationDTOV2> result = service.listLegacyViolations(app.getPublicId(), null, null);

    assertThat(result).isEmpty();
  }

  @Test
  public void listLegacyViolations_throwsWhenLicenseInvalid() {
    Application app = tempEntity.newApplicationWithParent();
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);

    assertThatThrownBy(() -> service.listLegacyViolations(app.getPublicId(), null, null))
        .isInstanceOf(InvalidLicenseException.class);
  }

  @Test
  public void listLegacyViolations_filtersByPolicyId() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policyA = tempEntity.newPolicy();
    Policy policyB = tempEntity.newPolicy();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    PolicyViolation pvA = tempEntity.newLegacyPolicyViolation(eval, policyA);
    tempEntity.newLegacyPolicyViolation(eval, policyB);

    List<ApiPolicyViolationDTOV2> result = service.listLegacyViolations(app.getPublicId(), policyA.getId(), null);

    assertThat(result).extracting(dto -> dto.policyViolationId).containsExactly(pvA.getId());
  }

  @Test
  public void listLegacyViolations_filtersByComponentIdentifier() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    ComponentIdentifier wantedCid = ComponentIdentifier.createMavenCoordinates("org.apache", "commons", "1.0");
    ComponentIdentifier otherCid = ComponentIdentifier.createMavenCoordinates("org.other", "lib", "2.0");
    PolicyViolation matching =
        tempEntity.newLegacyPolicyViolation(eval, policy, wantedCid, tempEntity.newRandomHash());
    tempEntity.newLegacyPolicyViolation(eval, policy, otherCid, tempEntity.newRandomHash());

    List<ApiPolicyViolationDTOV2> result = service.listLegacyViolations(app.getPublicId(), null, wantedCid);

    assertThat(result).extracting(dto -> dto.policyViolationId).containsExactly(matching.getId());
  }

  @Test
  public void listLegacyViolations_sortsByLegacyViolationTimeDescThenId() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    PolicyViolation older = tempEntity.newLegacyPolicyViolation(eval, policy);
    PolicyViolation newer = tempEntity.newLegacyPolicyViolation(eval, policy);
    older.setLegacyViolationTime(new Date(1_000L));
    newer.setLegacyViolationTime(new Date(2_000L));
    policyViolationDAO.update(older);
    policyViolationDAO.update(newer);

    List<ApiPolicyViolationDTOV2> result = service.listLegacyViolations(app.getPublicId(), null, null);

    assertThat(result).extracting(dto -> dto.policyViolationId).containsExactly(newer.getId(), older.getId());
  }

  @Test
  public void listLegacyViolations_nullApplicationPublicIdThrows() {
    assertThatThrownBy(() -> service.listLegacyViolations(null, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void revoke_clearsLegacyTimeAndReturnsCount() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "scanA");
    PolicyViolation pv1 = tempEntity.newLegacyPolicyViolation(eval, policy);
    PolicyViolation pv2 = tempEntity.newLegacyPolicyViolation(eval, policy);

    ApiLegacyViolationChangeResponseDTO result = service.revoke(app.getPublicId());

    assertThat(result.changedPolicyViolationCount).isEqualTo(2);
    assertThat(policyViolationDAO.getById(pv1.getId()).isLegacyViolation()).isFalse();
    assertThat(policyViolationDAO.getById(pv2.getId()).isLegacyViolation()).isFalse();
  }

  @Test
  public void revoke_returnsZeroWhenNoLegacyViolations() {
    Application app = tempEntity.newApplicationWithParent();

    ApiLegacyViolationChangeResponseDTO result = service.revoke(app.getPublicId());

    assertThat(result.changedPolicyViolationCount).isEqualTo(0);
  }

  @Test
  public void revoke_throwsWhenLicenseInvalid() {
    Application app = tempEntity.newApplicationWithParent();
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);

    assertThatThrownBy(() -> service.revoke(app.getPublicId()))
        .isInstanceOf(InvalidLicenseException.class);
  }

  @Test
  public void revoke_nullApplicationPublicIdThrows() {
    assertThatThrownBy(() -> service.revoke(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  public void grant_throwsWhenLicenseInvalid() {
    Application app = tempEntity.newApplicationWithParent();
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);

    assertThatThrownBy(() -> service.grant(app.getPublicId()))
        .isInstanceOf(InvalidLicenseException.class);
  }

  @Test
  public void grant_nullApplicationPublicIdThrows() {
    assertThatThrownBy(() -> service.grant(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  public void getConfig_application_returnsEnabledStateFromDb() {
    Organization parent = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent(parent);
    app.setLegacyViolationEnabled(true);
    applicationDAO.update(app);
    parent.setLegacyViolationEnabled(false);
    parent.setAllowLegacyViolationOverride(true);
    organizationDAO.update(parent);

    ApiLegacyViolationStatusDTO result = service.getConfig(OwnerType.APPLICATION, app.getPublicId());

    assertThat(result.enabled).isTrue();
    assertThat(result.enabledInParent).isFalse();
    assertThat(result.allowChange).isTrue();
  }

  @Test
  public void getConfig_organization_returnsEnabledAndOverride() {
    Organization org = tempEntity.newOrganization();
    org.setLegacyViolationEnabled(true);
    org.setAllowLegacyViolationOverride(false);
    organizationDAO.update(org);

    ApiLegacyViolationStatusDTO result = service.getConfig(OwnerType.ORGANIZATION, org.getId());

    assertThat(result.enabled).isTrue();
    assertThat(result.allowOverride).isFalse();
  }

  @Test
  public void getConfig_throwsWhenLicenseInvalid() {
    Application app = tempEntity.newApplicationWithParent();
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);

    assertThatThrownBy(() -> service.getConfig(OwnerType.APPLICATION, app.getPublicId()))
        .isInstanceOf(InvalidLicenseException.class);
  }

  @Test
  public void getConfig_nullArgumentsThrow() {
    assertThatThrownBy(() -> service.getConfig(null, "id")).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> service.getConfig(OwnerType.APPLICATION, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void setConfig_organization_persistsAndReturnsAuthoritativeState() {
    Organization org = tempEntity.newOrganization();
    ApiLegacyViolationStatusDTO request = new ApiLegacyViolationStatusDTO();
    request.enabled = true;
    request.allowOverride = true;

    ApiLegacyViolationStatusDTO result = service.setConfig(OwnerType.ORGANIZATION, org.getId(), request);

    assertThat(result.enabled).isTrue();
    assertThat(result.allowOverride).isTrue();
    Organization reloaded = organizationDAO.getByIdNotNull(org.getId());
    assertThat(reloaded.isLegacyViolationEnabled()).isTrue();
    assertThat(reloaded.isAllowLegacyViolationOverride()).isTrue();
  }

  @Test
  public void setConfig_application_persistsEnabledFlag() {
    Application app = tempEntity.newApplicationWithParent();
    ApiLegacyViolationStatusDTO request = new ApiLegacyViolationStatusDTO();
    request.enabled = true;

    ApiLegacyViolationStatusDTO result = service.setConfig(OwnerType.APPLICATION, app.getPublicId(), request);

    assertThat(result.enabled).isTrue();
  }

  @Test
  public void setConfig_throwsWhenLicenseInvalid() {
    Organization org = tempEntity.newOrganization();
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);
    ApiLegacyViolationStatusDTO request = new ApiLegacyViolationStatusDTO();
    request.enabled = true;

    assertThatThrownBy(() -> service.setConfig(OwnerType.ORGANIZATION, org.getId(), request))
        .isInstanceOf(InvalidLicenseException.class);
  }

  @Test
  public void setConfig_nullArgumentsThrow() {
    ApiLegacyViolationStatusDTO request = new ApiLegacyViolationStatusDTO();
    assertThatThrownBy(() -> service.setConfig(null, "id", request)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> service.setConfig(OwnerType.APPLICATION, null, request))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> service.setConfig(OwnerType.APPLICATION, "id", null))
        .isInstanceOf(NullPointerException.class);
  }
}
