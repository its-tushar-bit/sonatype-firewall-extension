/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.PolicyViolationDTOTestUtils.assertPolicyViolationDTO;
import static com.sonatype.insight.brain.utils.DisplayFieldValueAssertionUtil.assertDisplayFieldValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ComponentRiskServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ComponentRiskService componentRiskService;

  @Inject
  private CLMLicenseManager clmLicenseManager;

  @Inject
  private TestProductLicenseManager productLicenseManager;
  private Organization org;
  private Application app1;
  private Application app2;
  private Policy orgPolicy;
  private Policy app1Policy;
  private PolicyEvaluation app1PolicyEvaluation;
  private PolicyEvaluation app2PolicyEvaluation;
  private PolicyViolation orgPolicyViolation;
  private PolicyViolation app1PolicyViolation;
  private PolicyViolation app2PolicyViolation;
  private Tag tag1;
  private Tag tag2;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("app1", "app1", org.getId());
    app2 = tempEntity.newApplicationWithParent("app2", "app2");
    orgPolicy = tempEntity.newPolicy(org.getParentOwnerId(), "org owned policy", 3);
    app1Policy = tempEntity.newPolicy(app1.getId(), "app owned policy", 5);
    long time = System.currentTimeMillis() - 1000;
    app1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id",
        new Date(time));
    app2PolicyEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "test scan app2 id",
        new Date(time + 1));
    orgPolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy);
    app1PolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);
    app2PolicyViolation = tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-3", MatchState.SIMILAR, false);
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-4", MatchState.UNKNOWN, false);
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"));
    tag1 = tempEntity.newTag(org.getParentOwnerId());
    tag2 = tempEntity.newTag(org.getParentOwnerId());
    tempEntity.newApplicationTag(app1.getId(), tag1.getId());
    tempEntity.newApplicationTag(app1.getId(), tag2.getId());
  }

  private void fixViolations(PolicyEvaluation evaluation) {
    PolicyViolationDAO violationDAO = new PolicyViolationDAO();
    for (PolicyViolation fixedViolation : violationDAO
        .getUnfixedByApplicationIdAndStageId(evaluation.getApplicationId(), evaluation.getStageTypeId())) {
      fixedViolation.setFixTime(evaluation.getTime());
      violationDAO.update(fixedViolation);
    }
  }

  @Test
  public void testGetPolicyViolationsWithBadStageTypeId() {
    String badStageTypeId = "not a real stage type id";
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      componentRiskService.getPolicyViolations(null, null, Sets.newHashSet(badStageTypeId), null, null, null, null);
    }).withMessage("Invalid stage type: " + badStageTypeId + ".");
  }

  @Test
  public void testGetPolicyViolationsWithUnlicensedStageTypeIds() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);

    // Since we are not licensed for the build stage existing violations will not be returned.
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null, null,
        null, null, null);
    assertThat(policyViolationDTOs).isEmpty();

    Set<String> stageTypeIds = Sets.newHashSet(BuildStageType.ID);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      componentRiskService.getPolicyViolations(null, null, stageTypeIds, null, null, null, null);
    }).withMessage("Current license does not support stage type: " + BuildStageType.ID + ".");
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

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      componentRiskService.getPolicyViolations(null, null, Collections.singleton(DevelopStageType.ID), null, null, null,
          null);
    }).withMessage("Invalid stage type: develop.");
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
  public void testGetPolicyViolations_FilterByPolicyViolationState() throws Exception {
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", app1Policy.getId(), app1.getId(), "Some comments here");
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluation, app1Policy,
        ComponentIdentifier.createMavenCoordinates("gid", "aid", "1"), "hash1", policyWaiver);
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService
        .getPolicyViolations(null, null, null, null, null, null,
            new PolicyViolationStateFilter(PolicyViolationState.WAIVED));
    assertThat(policyViolationDTOs).hasSize(1);
    assertPolicyViolationDTO(policyViolationDTOs, waivedViolation, app1, app1PolicyEvaluation, app1Policy);

    Policy app1GrandfatherPolicy = tempEntity.newPolicy(app1.getId(), "policy Grandfather", 5);
    PolicyViolation grandfatherViolation = tempEntity
        .newGrandfatheredPolicyViolation(app1PolicyEvaluation, app1GrandfatherPolicy,
            ComponentIdentifier.createMavenCoordinates("gid", "aid", "1"), "hash1");
    policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.GRANDFATHERED));
    assertThat(policyViolationDTOs).hasSize(1);
    assertPolicyViolationDTO(policyViolationDTOs, grandfatherViolation, app1, app1PolicyEvaluation,
        app1GrandfatherPolicy);


    policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.OPEN));
    assertThat(policyViolationDTOs).hasSize(3);
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, app1PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1PolicyEvaluation, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, app2PolicyEvaluation, orgPolicy);

    policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.WAIVED, PolicyViolationState.GRANDFATHERED,
            PolicyViolationState.OPEN));
    assertThat(policyViolationDTOs).hasSize(5);
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, app1PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1PolicyEvaluation, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, app2PolicyEvaluation, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, waivedViolation, app1, app1PolicyEvaluation, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, grandfatherViolation, app1, app1PolicyEvaluation,
        app1GrandfatherPolicy);
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
        new HashSet<String>(), null, null, null);
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

  @Test
  public void testGetComponentRisks_DedupViolationsForSameAppAndPolicyByPickingMostRecentViolationAcrossStages()
      throws Exception
  {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanIdApp1");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, app1Policy, app1Policy.getThreatLevel() + 1,
        PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1");

    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 1000);

    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(violation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, violation);
    assertThat(riskDTO.score).isEqualTo(violation.getThreatLevel() + orgPolicy.getThreatLevel() * 2);
    assertThat(riskDTO.affectedApplications).isEqualTo(2);
  }

  @Test
  public void testGetComponentRisks_FilterByApplication() throws Exception {
    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(null, Collections.singleton(app2.getId()), null, null, null, null, null, "-TOTAL_RISK", 1000);

    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(app2PolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, app2PolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_FilterByOrganization() throws Exception {
    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(Collections.singleton(app2.getParentOwnerId()), null, null, null, null, null, null,
            "-TOTAL_RISK", 1000);

    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(app2PolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, app2PolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_FilterByEmptyOrganization() throws Exception {
    Organization emptyOrg = tempEntity.newOrganization();
    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(Collections.singleton(emptyOrg.getId()), null, null, null, null, null, null, "-TOTAL_RISK", 
            1000);

    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
  }

  @Test
  public void testGetComponentRisks_FilterByStage() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanIdApp1");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, app1Policy);

    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(null, null, Collections.singleton(ReleaseStageType.ID), null, null, null, null, "-TOTAL_RISK",
            1000);

    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(violation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, violation);
    assertThat(riskDTO.score).isEqualTo(app1Policy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_FilterByStage_ExcludesDevelop() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), DevelopStageType.ID, "newScanIdApp1");
    tempEntity.newPolicyViolation(evaluation, app1Policy, app1Policy.getThreatLevel(), app1Policy.getThreatCategory(),
        "g", "a", "v", "somehash");

    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    assertThat(result.dashboardResults.get(0).hash).isEqualTo(app1PolicyViolation.getHash());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      componentRiskService.getComponentRisks(null, null, Collections.singleton(DevelopStageType.ID), null, null, null,
          null, "-TOTAL_RISK", 1000);
    }).withMessage("Invalid stage type: develop.");
  }

  @Test
  public void testGetComponentRisks_FilterByTag() throws Exception {
    Tag app2Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(null, null, null, Collections.singleton(app2Tag.getId()), null, null, null, "-TOTAL_RISK", 
            1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(app2PolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, app2PolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_FilterPolicyThreatCategory() throws Exception {
    Policy licensePolicy =
        tempEntity.newPolicy(app1, 5, LogicalOperator.AND, new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    PolicyViolation violation =
        tempEntity.newPolicyViolation(app1PolicyEvaluation, licensePolicy, "gid", "aid", "1", "hash");

    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(null, null, null, null, new PolicyThreatCategoryFilter(violation.getThreatCategory()), null,
            null, "-TOTAL_RISK", 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(violation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, violation);
    assertThat(riskDTO.score).isEqualTo(app1Policy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_FilterPolicyThreatLevel() throws Exception {
    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(null, null, null, null, null, new PolicyThreatLevelFilter(3, 3), null,
            "-TOTAL_RISK", 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(orgPolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, orgPolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicy.getThreatLevel() * 2);
    assertThat(riskDTO.affectedApplications).isEqualTo(2);
  }

  @Test
  public void testGetComponentRisks_FilterByPolicyViolationState() throws Exception {
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", app1Policy.getId(), app1.getId(), "Some comments here");
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluation, app1Policy,
        ComponentIdentifier.createMavenCoordinates("gid", "aid", "1"), "hash1", policyWaiver);
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyViolation policyViolation = policyViolationDAO.getById(waivedViolation.getId());
    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(null, null, null, null, null, null,
            new PolicyViolationStateFilter(PolicyViolationState.WAIVED), "-TOTAL_RISK", 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(policyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, policyViolation);
    assertThat(riskDTO.score).isEqualTo(app1Policy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);

    result = componentRiskService.getComponentRisks(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.OPEN), "-TOTAL_RISK", 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(orgPolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, orgPolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicyViolation.getThreatLevel() + app1PolicyViolation.getThreatLevel() +
        app2PolicyViolation.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(2);

    result = componentRiskService.getComponentRisks(null, null, null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.WAIVED, PolicyViolationState.OPEN), "-TOTAL_RISK", 1000);
    assertThat(result.dashboardResults).hasSize(2);
    assertThat(result.numResults).isEqualTo(2);
    riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(orgPolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, orgPolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicyViolation.getThreatLevel() + app1PolicyViolation.getThreatLevel() +
        app2PolicyViolation.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(2);
    riskDTO = result.dashboardResults.get(1);
    assertThat(riskDTO.hash).isEqualTo(policyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, policyViolation);
    assertThat(riskDTO.score).isEqualTo(app1Policy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_FilterByApplicationAndPolicyViolationState() throws Exception {
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", app1Policy.getId(), app1.getId(), "Some comments here");
    PolicyViolation waivedViolation = tempEntity.newWaivedPolicyViolation(app1PolicyEvaluation, app1Policy,
        ComponentIdentifier.createMavenCoordinates("gid", "aid", "1"), "hash1", policyWaiver);
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    PolicyViolation policyViolation = policyViolationDAO.getById(waivedViolation.getId());
    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(null, Collections.singleton(app1.getId()), null, null, null, null,
            new PolicyViolationStateFilter(PolicyViolationState.WAIVED), "-TOTAL_RISK", 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(policyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, policyViolation);
    assertThat(riskDTO.score).isEqualTo(app1Policy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);

    result = componentRiskService.getComponentRisks(null, Collections.singleton(app1.getId()), null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.OPEN), "-TOTAL_RISK", 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(orgPolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, orgPolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicyViolation.getThreatLevel() + app1PolicyViolation.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);

    result = componentRiskService.getComponentRisks(null, Collections.singleton(app1.getId()), null, null, null, null,
        new PolicyViolationStateFilter(PolicyViolationState.WAIVED, PolicyViolationState.OPEN), "-TOTAL_RISK", 1000);
    assertThat(result.dashboardResults).hasSize(2);
    assertThat(result.numResults).isEqualTo(2);
    riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(orgPolicyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, orgPolicyViolation);
    assertThat(riskDTO.score).isEqualTo(orgPolicyViolation.getThreatLevel() + app1PolicyViolation.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
    riskDTO = result.dashboardResults.get(1);
    assertThat(riskDTO.hash).isEqualTo(policyViolation.getHash());
    assertDisplayFieldValues(riskDTO.displayName.parts, policyViolation);
    assertThat(riskDTO.score).isEqualTo(app1Policy.getThreatLevel());
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_ScoreBreakdown() throws Exception {
    for (int i = 0; i <= 10; i++) {
      if (i == app2PolicyViolation.getThreatLevel()) {
        continue;
      }
      Policy orgPolicy = tempEntity.newPolicy(org.getId(), "policy " + i, i);
      tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy);
    }

    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(null, Collections.singleton(app2.getId()), null, null, null, null, null, "-TOTAL_RISK", 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.scoreCritical).isEqualTo(27);
    assertThat(riskDTO.scoreSevere).isEqualTo(22);
    assertThat(riskDTO.scoreModerate).isEqualTo(5);
    assertThat(riskDTO.scoreLow).isEqualTo(1);
    assertThat(riskDTO.score).isEqualTo(55);
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_ScoreBreakdown_Org() throws Exception {
    for (int i = 0; i <= 10; i++) {
      if (i == app2PolicyViolation.getThreatLevel()) {
        continue;
      }
      Policy orgPolicy = tempEntity.newPolicy(org.getId(), "policy " + i, i);
      tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy);
    }

    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(Collections.singleton(app2.getParentOwnerId()), null, null, null, null, null, null,
            "-TOTAL_RISK", 1000);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.scoreCritical).isEqualTo(27);
    assertThat(riskDTO.scoreSevere).isEqualTo(22);
    assertThat(riskDTO.scoreModerate).isEqualTo(5);
    assertThat(riskDTO.scoreLow).isEqualTo(1);
    assertThat(riskDTO.score).isEqualTo(55);
    assertThat(riskDTO.affectedApplications).isEqualTo(1);
  }

  @Test
  public void testGetComponentRisks_ResultCapping() throws Exception {
    String gid = "gid", aid = "aid", ver = "1", hash = "somehash";
    tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy, 4, PolicyThreatCategory.SECURITY, gid, aid, ver,
        hash);
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, 4, PolicyThreatCategory.SECURITY, gid, aid, ver,
        hash);
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy, 4, PolicyThreatCategory.SECURITY, gid, aid, ver,
        hash);

    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 1);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(2);
    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.hash).isEqualTo(hash);
    assertThat(riskDTO.score).isEqualTo(12);
    assertThat(riskDTO.affectedApplications).isEqualTo(2);
  }

  @Test
  public void testGetComponentRisks_Unknown() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanIdApp1");
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "pathnames-hash", null, "a.zip/b.zip",
        MatchState.UNKNOWN, false, evaluation.getTime());

    // Create 2 violations without component identifiers: one with a pathname and one without. 
    ComponentIdentifier nullComponentIdentifier = null;
    tempEntity.newPolicyViolation(evaluation, app1Policy, nullComponentIdentifier, "hash-4", "unknown");
    tempEntity.newPolicyViolation(evaluation, app1Policy, nullComponentIdentifier, "filename-hash", "unknown2",
        "b.zip");

    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService.getComponentRisks(null, null,
        Collections.singleton(ReleaseStageType.ID), null, null, null, null, "NAME", 2);
    assertThat(result.dashboardResults).hasSize(2);
    assertThat(result.numResults).isEqualTo(2);

    ComponentRiskDTO riskDTO = result.dashboardResults.get(0);
    assertThat(riskDTO.filename).isEqualTo("b.zip");
    assertThat(riskDTO.derivedComponentName).isEqualTo("b.zip"); // we use the last file of the first path name

    riskDTO = result.dashboardResults.get(1);
    assertThat(riskDTO.derivedComponentName).isEqualTo("Unknown");
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
