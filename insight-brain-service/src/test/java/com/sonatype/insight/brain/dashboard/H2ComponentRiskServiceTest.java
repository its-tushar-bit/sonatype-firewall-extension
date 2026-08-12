/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2InMemoryTest;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.Sets;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.PolicyViolationDTOTestUtils.assertPolicyViolationDTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@H2InMemoryTest
public class H2ComponentRiskServiceTest
    extends AbstractComponentRiskServiceTest
{
  @Inject
  private H2ComponentRiskService componentRiskService;

  @Override
  protected DashboardComponentRiskService getComponentRiskService() {
    return componentRiskService;
  }

  private void fixViolations(PolicyEvaluation evaluation) {
    List<PolicyViolation> policyViolations =
        violationDAO.getUnfixedByOwnerIdAndStageId(evaluation.getOwnerId(), evaluation.getStageTypeId());
    violationDAO.loadConstraintFacts(policyViolations);
    for (PolicyViolation fixedViolation : policyViolations) {
      fixedViolation.setFixTime(evaluation.getTime());
      violationDAO.update(fixedViolation);
    }
  }

  @Test
  public void testGetPolicyViolationsWithBadStageTypeId() {
    String badStageTypeId = "not a real stage type id";
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> componentRiskService.getPolicyViolations(null, null, Sets.newHashSet(badStageTypeId), null, null,
                null, null))
        .withMessage("Invalid stage type: " + badStageTypeId + ".");
  }

  @Test
  public void testGetPolicyViolationsWithUnlicensedStageTypeIds() {
    testProductLicense.setStageTypes(StageTypes.RELEASE);

    // Since we are not licensed for the build stage existing violations will not be returned.
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null, null,
        null, null, null);
    assertThat(policyViolationDTOs).isEmpty();

    Set<String> stageTypeIds = Sets.newHashSet(BuildStageType.ID);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> componentRiskService.getPolicyViolations(null, null, stageTypeIds, null, null, null, null))
        .withMessage("Current license does not support stage type: " + BuildStageType.ID + ".");
  }

  @Test
  public void testGetPolicyViolationsWithNullOrEmptyStageTypeIds() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation, app1Policy);

    // If no stages are given return violations for all stages.
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null, null,
        null, null, null);

    assertThat(policyViolationDTOs).hasSize(4);
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, app1PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1PolicyEvaluation, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, app2PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, newApp1PolicyEvaluation, app1Policy);

    Set<String> stageTypeIds = Collections.emptySet();
    policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, stageTypeIds, null, null, null, null);

    assertThat(policyViolationDTOs).hasSize(4);
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, app1PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1PolicyEvaluation, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, app2PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, newApp1PolicyEvaluation, app1Policy);
  }

  @Test
  public void testGetPolicyViolations() {
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null, null,
        null, null, null);
    assertThat(policyViolationDTOs).hasSize(3);
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, app1PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1PolicyEvaluation, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, app2PolicyEvaluation, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsWithMultipleStages() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation, app1Policy);

    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null,
        Sets.newHashSet(BuildStageType.ID, ReleaseStageType.ID), null, null, null, null);
    assertThat(policyViolationDTOs).hasSize(4);
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, app1PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1PolicyEvaluation, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, app2PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, newApp1PolicyEvaluation, app1Policy);
  }

  @Test
  public void testGetPolicyViolations_FilterByStage_ExcludesDevelop() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), DevelopStageType.ID, "newScanIdApp1");
    tempEntity.newPolicyViolation(evaluation, app1Policy);

    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null, null,
        null, null, null);
    assertThat(policyViolationDTOs).hasSize(3);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> componentRiskService.getPolicyViolations(null, null, Collections.singleton(DevelopStageType.ID), null,
                null, null, null))
        .withMessage("Invalid stage type: develop.");
  }

  @Test
  public void testGetPolicyViolationsWithPolicyThreatCategories() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation, app1Policy);

    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null,
        Sets.newHashSet(ReleaseStageType.ID), null,
        new PolicyThreatCategoryFilter(PolicyThreatCategory.OTHER), null, null);

    assertThat(policyViolationDTOs).isEmpty();

    policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, Sets.newHashSet(ReleaseStageType.ID),
        null, new PolicyThreatCategoryFilter(violation.getThreatCategory()), null, null);

    assertThat(policyViolationDTOs).hasSize(1);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, newApp1PolicyEvaluation, app1Policy);
  }

  @Test
  public void testGetPolicyViolationsWithPolicyThreatLevel() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation, app1Policy);

    // Violation out of threat level range.
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null,
        Sets.newHashSet(ReleaseStageType.ID), null, null, new PolicyThreatLevelFilter(6, 7), null);

    assertThat(policyViolationDTOs).isEmpty();

    // Violation in range.
    policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, Sets.newHashSet(ReleaseStageType.ID),
        null, null, new PolicyThreatLevelFilter(5, 7), null);

    assertThat(policyViolationDTOs).hasSize(1);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, newApp1PolicyEvaluation, app1Policy);
  }

  @Test
  public void testGetPolicyViolationsWithPolicyThreatLevelAndPolicyThreatCategories() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation, app1Policy);

    // Violation out of threat level range and wrong threat category.
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null,
        Sets.newHashSet(ReleaseStageType.ID), null, new PolicyThreatCategoryFilter(PolicyThreatCategory.OTHER),
        new PolicyThreatLevelFilter(6, 7), null);
    assertThat(policyViolationDTOs).isEmpty();

    // Violation out of threat level range and correct threat category.
    policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, Sets.newHashSet(ReleaseStageType.ID),
        null, new PolicyThreatCategoryFilter(violation.getThreatCategory()), new PolicyThreatLevelFilter(6, 7), null);
    assertThat(policyViolationDTOs).isEmpty();

    // Violation in range, but wrong threat category.
    policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, Sets.newHashSet(ReleaseStageType.ID),
        null, new PolicyThreatCategoryFilter(PolicyThreatCategory.OTHER), new PolicyThreatLevelFilter(5, 7), null);
    assertThat(policyViolationDTOs).isEmpty();

    // Violation in range and in the correct category.
    policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, Sets.newHashSet(ReleaseStageType.ID),
        null, new PolicyThreatCategoryFilter(violation.getThreatCategory()), new PolicyThreatLevelFilter(5, 7), null);

    assertThat(policyViolationDTOs).hasSize(1);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, newApp1PolicyEvaluation, app1Policy);
  }

  @Test
  public void testGetPolicyViolations_FilterByPolicyViolationState() {
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", app1Policy.getId(), app1.getId(), "Some comments here");
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluation, app1Policy,
        ComponentIdentifier.createMavenCoordinates("gid", "aid", "1"), "hash1", policyWaiver);
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService
        .getPolicyViolations(null, null, null, null, null, null,
            new PolicyViolationStateFilter(PolicyViolationState.WAIVED));
    assertThat(policyViolationDTOs).hasSize(1);
    assertPolicyViolationDTO(policyViolationDTOs, waivedViolation, app1, app1PolicyEvaluation, app1Policy);

    Policy app1LegacyViolationPolicy = tempEntity.newPolicy(app1.getId(), "Legacy Violation Policy", 5);
    PolicyViolation legacyViolation = tempEntity
        .newLegacyPolicyViolation(app1PolicyEvaluation, app1LegacyViolationPolicy,
            ComponentIdentifier.createMavenCoordinates("gid", "aid", "1"), "hash1");
    policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.LEGACY_VIOLATION));
    assertThat(policyViolationDTOs).hasSize(1);
    assertPolicyViolationDTO(policyViolationDTOs, legacyViolation, app1, app1PolicyEvaluation,
        app1LegacyViolationPolicy);

    policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.OPEN));
    assertThat(policyViolationDTOs).hasSize(3);
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, app1PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1PolicyEvaluation, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, app2PolicyEvaluation, orgPolicy);

    policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.WAIVED, PolicyViolationState.LEGACY_VIOLATION,
            PolicyViolationState.OPEN));
    assertThat(policyViolationDTOs).hasSize(5);
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, app1PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1PolicyEvaluation, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, app2PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, waivedViolation, app1, app1PolicyEvaluation, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, legacyViolation, app1, app1PolicyEvaluation,
        app1LegacyViolationPolicy);
  }

  @Test
  public void testGetPolicyViolationsReturnsLatest() {
    // Create a new evaluation for app1 that does not include any violations.
    PolicyEvaluation newEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "re-scan app1");
    fixViolations(newEvaluation);

    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null, null,
        null, null, null);

    // The violations count should be 1, all of which come from app2.
    assertThat(policyViolationDTOs).hasSize(1);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, app2PolicyEvaluation, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationIds() {
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null,
        Sets.newHashSet(app1.getId(), app2.getId()), null, null, null, null, null);
    assertThat(policyViolationDTOs).hasSize(3);
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, app1PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1PolicyEvaluation, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, app2PolicyEvaluation, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationIdsWithMultipleStages() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation, app1Policy);

    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null,
        Sets.newHashSet(app1.getId(), app2.getId()), Sets.newHashSet(BuildStageType.ID, ReleaseStageType.ID), null,
        null, null, null);
    assertThat(policyViolationDTOs).hasSize(4);
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, app1PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1PolicyEvaluation, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, app2PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, newApp1PolicyEvaluation, app1Policy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationIdsReturnsLatest() {
    // Create a new evaluation for app1 that does not include any violations.
    PolicyEvaluation newEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "re-scan app1");
    fixViolations(newEvaluation);

    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null,
        Sets.newHashSet(app1.getId(), app2.getId()), null, null, null, null, null);

    // The violations count should be 1, all of which come from app2.
    assertThat(policyViolationDTOs).hasSize(1);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, app2PolicyEvaluation, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsByTags() {
    Tag app1Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app1.getId(), app1Tag.getId());

    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null,
        Sets.newHashSet(app1Tag.getId()), null, null, null);
    assertThat(policyViolationDTOs).hasSize(2);
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, app1PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1PolicyEvaluation, app1Policy);

    Tag app2Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null,
        Sets.newHashSet(app1Tag.getId(), app2Tag.getId()), null, null, null);
    assertThat(policyViolationDTOs).hasSize(3);
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, app1PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1PolicyEvaluation, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, app2PolicyEvaluation, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationWithNullAndEmptyTags() {
    Tag app1Tag = tempEntity.newTag(org.getId());
    // The tag filter uses Application Tags to filter the result by tag id. To ensure the results are not filtered
    // an entry for Application Tag must exist
    tempEntity.newApplicationTag(app1.getId(), app1Tag.getId());

    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null,
        new HashSet<>(), null, null, null);
    assertThat(policyViolationDTOs).hasSize(3);
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, app1PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1PolicyEvaluation, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, app2PolicyEvaluation, orgPolicy);

    policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null, null, null, null, null);
    assertThat(policyViolationDTOs).hasSize(3);
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, app1PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1PolicyEvaluation, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, app2PolicyEvaluation, orgPolicy);
  }

  /**
   * Tests elimination of duplicates in query results for Application with multiple Tags that are present in a filter
   * Prior to the fix for this issue, the same call resulted in IAE when trying to Map duplicate Applications by their
   * IDs.
   * https://issues.sonatype.org/browse/CLM-3385
   */
  @Test
  public void testGetPolicyViolationsForApplicationWithMultipleTags() {
    List<PolicyViolationDTO> policyViolations = componentRiskService.getPolicyViolations(null, null, null,
        Sets.newHashSet(tag1.getId(), tag2.getId()), null, null, null);
    assertThat(policyViolations).hasSize(2);
    assertPolicyViolationDTO(policyViolations, orgPolicyViolation, app1, app1PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolations, app1PolicyViolation, app1, app1PolicyEvaluation, app1Policy);
  }
}
