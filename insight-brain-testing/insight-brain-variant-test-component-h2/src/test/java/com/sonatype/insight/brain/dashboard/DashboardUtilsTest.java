/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class DashboardUtilsTest
    extends AbstractComponentH2Test
{
  @Inject
  private DashboardUtils dashboardUtils;

  @Inject
  private TestProductLicense testProductLicense;

  @Test
  public void testGetStageTypes_StageTypeIdsNull() {
    assertThat(dashboardUtils.getStageTypes(null)).containsExactly(StageTypes.SOURCE, StageTypes.BUILD,
        StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE);
  }

  @Test
  public void testGetStageTypes_StageTypeIdsEmpty() {
    assertThat(dashboardUtils.getStageTypes(Collections.emptySet())).containsExactly(StageTypes.SOURCE,
        StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE, StageTypes.OPERATE);
  }

  @Test
  public void testGetStageTypes_InvalidStageTypeId() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dashboardUtils.getStageTypes(Collections.singleton("invalid-stage-type-id")))
        .withMessage("Invalid stage type: invalid-stage-type-id.");
  }

  @Test
  public void testGetStageTypes_UnlicensedStageTypeId() {
    testProductLicense.setStageTypes(StageTypes.RELEASE);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dashboardUtils.getStageTypes(Collections.singleton(StageTypes.BUILD.getId())))
        .withMessage("Current license does not support stage type: build.");
  }

  @Test
  public void testHasExistingAutoWaiverExclusion() {
    final String scanId = "scan-id";
    final Organization org = tempEntity.newOrganization();
    final Application app = tempEntity.newApplication(org.getId());
    final Policy policy = tempEntity.newPolicy(org.getId());
    final PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(),
        scanId);
    final PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

    // No waiver or exclusion exists
    final AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(org.getId());
    boolean result = dashboardUtils.hasExistingAutoWaiverExclusion(app.getId(), waiver.getId(),
        policyViolation.getId());
    assertThat(result).isFalse();

    // The waiver and exclusion exist at the app level, so the org hierarchy will not be traversed to check for it
    final AutoPolicyWaiver appWaiver = tempEntity.newAutoPolicyWaiver(app.getId());
    tempEntity.newAutoPolicyWaiverExclusion(app.getId(), "", "", appWaiver.getId(), scanId, policyViolation);

    result = dashboardUtils.hasExistingAutoWaiverExclusion(app.getId(), appWaiver.getId(), policyViolation.getId());
    assertThat(result).isTrue();

    // The waiver and exclusion exist at the org level, so the org hierarchy will be traversed to check for it
    final AutoPolicyWaiver orgWaiver = tempEntity.newAutoPolicyWaiver(org.getId());
    tempEntity.newAutoPolicyWaiverExclusion(org.getId(), "", "", orgWaiver.getId(), scanId, policyViolation);

    result = dashboardUtils.hasExistingAutoWaiverExclusion(app.getId(), orgWaiver.getId(), policyViolation.getId());
    assertThat(result).isTrue();
  }

  @Test
  public void testShouldOnlyShowWaivedViolations() {
    // Null filter
    PolicyViolationStateFilter policyViolationStateFilter = null;
    boolean result = DashboardUtils.shouldOnlyShowWaivedViolations(policyViolationStateFilter);
    assertThat(result).isFalse();

    // Empty filter
    policyViolationStateFilter = new PolicyViolationStateFilter(Set.of());
    result = DashboardUtils.shouldOnlyShowWaivedViolations(policyViolationStateFilter);
    assertThat(result).isFalse();

    // Filter with only waived
    policyViolationStateFilter = new PolicyViolationStateFilter(Set.of(PolicyViolationState.WAIVED));
    result = DashboardUtils.shouldOnlyShowWaivedViolations(policyViolationStateFilter);
    assertThat(result).isTrue();

    // Filter with only non-waived
    policyViolationStateFilter = new PolicyViolationStateFilter(Set.of(PolicyViolationState.OPEN,
        PolicyViolationState.LEGACY_VIOLATION));
    result = DashboardUtils.shouldOnlyShowWaivedViolations(policyViolationStateFilter);
    assertThat(result).isFalse();

    // Filter with waived and non-waived
    policyViolationStateFilter = new PolicyViolationStateFilter(Set.of(PolicyViolationState.WAIVED,
        PolicyViolationState.OPEN));
    result = DashboardUtils.shouldOnlyShowWaivedViolations(policyViolationStateFilter);
    assertThat(result).isFalse();
  }
}
