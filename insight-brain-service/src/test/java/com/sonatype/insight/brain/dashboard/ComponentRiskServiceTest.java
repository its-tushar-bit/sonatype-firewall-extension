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
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.component.DisplayFieldValueAssertionUtil.assertDisplayFieldValues;
import static com.sonatype.insight.brain.dashboard.PolicyViolationDTOTestUtils.assertPolicyViolationDTO;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
    tempEntity.newFirstOccurrencePolicyViolation(orgPolicyViolation.getId(), app1.getId(), BuildStageType.ID);
    tempEntity.newFirstOccurrencePolicyViolation(app1PolicyViolation.getId(), app1.getId(), BuildStageType.ID);
    tempEntity.newFirstOccurrencePolicyViolation(app2PolicyViolation.getId(), app2.getId(), BuildStageType.ID);
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

  @Test
  public void testGetPolicyViolationsWithBadStageTypeId() {
    String badStageTypeId = "not a real stage type id";
    try {
      componentRiskService.getPolicyViolations(Sets.newHashSet(badStageTypeId), null, null);
      fail("Expected BadRequestException to be thrown.");
    }
    catch (BadRequestException e) {
      assertEquals(e.getMessage(), "Invalid stage type: " + badStageTypeId + ".");
    }
  }

  @Test
  public void testGetPolicyViolationsWithUnlicensedStageTypeIds() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);

    // Since we are not licensed for the build stage existing violations will not be returned.
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null);
    assertThat(policyViolationDTOs, hasSize(0));

    Set<String> stageTypeIds = Sets.newHashSet(BuildStageType.ID);
    try {
      policyViolationDTOs = componentRiskService.getPolicyViolations(stageTypeIds, null, null);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertEquals(e.getMessage(), "Current license does not support stage type: " + BuildStageType.ID + ".");
    }
  }

  @Test
  public void testGetPolicyViolationsWithNullOrEmptyStageTypeIds() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation, app1Policy);

    // If no stages are given return violations for all stages.
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null);

    assertThat(policyViolationDTOs, hasSize(4));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);

    Set<String> stageTypeIds = Collections.emptySet();
    policyViolationDTOs = componentRiskService.getPolicyViolations(stageTypeIds, null, null);

    assertThat(policyViolationDTOs, hasSize(4));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);
  }

  @Test
  public void testGetPolicyViolationsWithNullApplicationIds() {
    Set<String> nullApplicationId = null;
    try {
      componentRiskService.getPolicyViolationsByApplicationIds(nullApplicationId, null, null, null);
      fail("Expected BadRequestException to be thrown.");
    }
    catch (BadRequestException e) {
      assertEquals(e.getMessage(), "Unable to get policy violations for null or empty application IDs.");
    }
  }

  @Test
  public void testGetPolicyViolationsWithEmptyApplicationIds() {
    Set<String> emptyApplicationId = new HashSet<>();
    try {
      componentRiskService.getPolicyViolationsByApplicationIds(emptyApplicationId, null, null, null);
      fail("Expected BadRequestException to be thrown.");
    }
    catch (BadRequestException e) {
      assertEquals(e.getMessage(), "Unable to get policy violations for null or empty application IDs.");
    }
  }

  @Test
  public void testGetPolicyViolations() {
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsWithMultipleStages() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation, app1Policy);

    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(
        Sets.newHashSet(BuildStageType.ID, ReleaseStageType.ID), null, null);
    assertThat(policyViolationDTOs, hasSize(4));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);
  }

  @Test
  public void testGetPolicyViolations_FilterByStage_ExcludesDevelop() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), DevelopStageType.ID, "newScanIdApp1");
    tempEntity.newPolicyViolation(evaluation, app1Policy);

    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null);
    assertThat(policyViolationDTOs, hasSize(3));

    try {
      componentRiskService.getPolicyViolations(Collections.singleton(DevelopStageType.ID), null, null);
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Invalid stage type: develop."));
    }
  }

  @Test
  public void testGetPolicyViolationsWithPolicyThreatCategories() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation, app1Policy);

    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(
        Sets.newHashSet(ReleaseStageType.ID), null,
        new PolicyThreatCategoryFilter(PolicyThreatCategory.OTHER).asPolicyViolationPredicate());

    assertThat(policyViolationDTOs, hasSize(0));

    policyViolationDTOs = componentRiskService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), null,
        new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE).asPolicyViolationPredicate());

    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);
  }

  @Test
  public void testGetPolicyViolationsWithPolicyThreatLevel() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation, app1Policy);

    // Violation out of threat level range.
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(
        Sets.newHashSet(ReleaseStageType.ID), null, new PolicyThreatLevelFilter(6, 7).asPolicyViolationPredicate());

    assertThat(policyViolationDTOs, hasSize(0));

    // Violation in range.
    policyViolationDTOs = componentRiskService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), null,
        new PolicyThreatLevelFilter(5, 7).asPolicyViolationPredicate());

    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);
  }

  @Test
  public void testGetPolicyViolationsWithPolicyThreatLevelAndPolicyThreatCategories() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation, app1Policy);

    // Violation out of threat level range and wrong threat category.
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(Sets
        .newHashSet(ReleaseStageType.ID), null, Predicates.and(new PolicyThreatCategoryFilter(
        PolicyThreatCategory.OTHER).asPolicyViolationPredicate(), new PolicyThreatLevelFilter(6, 7)
        .asPolicyViolationPredicate()));
    assertThat(policyViolationDTOs, hasSize(0));

    // Violation out of threat level range and correct threat category.
    policyViolationDTOs = componentRiskService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), null,
        Predicates.and(new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE).asPolicyViolationPredicate(),
            new PolicyThreatLevelFilter(6, 7).asPolicyViolationPredicate()));
    assertThat(policyViolationDTOs, hasSize(0));

    // Violation in range, but wrong threat category.
    policyViolationDTOs = componentRiskService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), null,
        Predicates.and(new PolicyThreatCategoryFilter(PolicyThreatCategory.OTHER).asPolicyViolationPredicate(),
            new PolicyThreatLevelFilter(5, 7).asPolicyViolationPredicate()));
    assertThat(policyViolationDTOs, hasSize(0));

    // Violation in range and in the correct category.
    policyViolationDTOs = componentRiskService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), null,
        Predicates.and(new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE).asPolicyViolationPredicate(),
            new PolicyThreatLevelFilter(5, 7).asPolicyViolationPredicate()));

    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);
  }

  @Test
  public void testGetPolicyViolationsReturnsLatest() {
    // Create a new evaluation for app1 that does not include any violations.
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "re-scan app1");
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null);

    // The violations count should be 1, all of which come from app2.
    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationIds() {
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app1.getId(), app2.getId()), null, null, null);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationIdsWithMultipleStages() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation, app1Policy);

    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app1.getId(), app2.getId()), Sets.newHashSet(BuildStageType.ID, ReleaseStageType.ID), null,
        null);
    assertThat(policyViolationDTOs, hasSize(4));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationIdsReturnsLatest() {
    // Create a new evaluation for app1 that does not include any violations.
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "re-scan app1");
    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app1.getId(), app2.getId()), null, null, null);

    // The violations count should be 1, all of which come from app2.
    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsByTags() {
    Tag app1Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app1.getId(), app1Tag.getId());

    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null,
        Sets.newHashSet(app1Tag.getId()), null);
    assertThat(policyViolationDTOs, hasSize(2));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);

    Tag app2Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    policyViolationDTOs = componentRiskService.getPolicyViolations(null,
        Sets.newHashSet(app1Tag.getId(), app2Tag.getId()), null);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationWithNullAndEmptyTags() {
    Tag app1Tag = tempEntity.newTag(org.getId());
    // The tag filter uses Application Tags to filter the result by tag id. To ensure the results are not filtered
    // an entry for Application Tag must exist
    tempEntity.newApplicationTag(app1.getId(), app1Tag.getId());

    List<PolicyViolationDTO> policyViolationDTOs = componentRiskService.getPolicyViolations(null,
        new HashSet<String>(), null);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);

    policyViolationDTOs = componentRiskService.getPolicyViolations(null, null, null);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetComponentRisks_DedupViolationsForSameAppAndPolicyByPickingMostRecentViolationAcrossStages()
      throws Exception
  {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanIdApp1");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, app1Policy, app1Policy.getThreatLevel() + 1,
        PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1");

    List<ComponentRiskDTO> riskDTOs = componentRiskService.getComponentRisks(null, null, null, null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.hash, is(violation.getHash()));
    assertDisplayFieldValues(riskDTO.displayName.parts, violation);
    assertThat(riskDTO.score, is(violation.getThreatLevel() + orgPolicy.getThreatLevel() * 2));
    assertThat(riskDTO.affectedApplications, is(2));
  }

  @Test
  public void testGetComponentRisks_FilterByApplication() throws Exception {
    List<ComponentRiskDTO> riskDTOs = componentRiskService.getComponentRisks(null, Collections.singleton(app2.getId()),
        null,
        null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.hash, is(app2PolicyViolation.getHash()));
    assertDisplayFieldValues(riskDTO.displayName.parts, app2PolicyViolation);
    assertThat(riskDTO.score, is(orgPolicy.getThreatLevel()));
    assertThat(riskDTO.affectedApplications, is(1));
  }

  @Test
  public void testGetComponentRisks_FilterByOrganization() throws Exception {
    List<ComponentRiskDTO> riskDTOs = componentRiskService
        .getComponentRisks(Collections.singleton(app2.getParentOwnerId()), null, null, null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.hash, is(app2PolicyViolation.getHash()));
    assertDisplayFieldValues(riskDTO.displayName.parts, app2PolicyViolation);
    assertThat(riskDTO.score, is(orgPolicy.getThreatLevel()));
    assertThat(riskDTO.affectedApplications, is(1));
  }

  @Test
  public void testGetComponentRisks_FilterByStage() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanIdApp1");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, app1Policy);

    List<ComponentRiskDTO> riskDTOs = componentRiskService.getComponentRisks(null, null,
        Collections.singleton(ReleaseStageType.ID), null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.hash, is(violation.getHash()));
    assertDisplayFieldValues(riskDTO.displayName.parts, violation);
    assertThat(riskDTO.score, is(app1Policy.getThreatLevel()));
    assertThat(riskDTO.affectedApplications, is(1));
  }

  @Test
  public void testGetComponentRisks_FilterByStage_ExcludesDevelop() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), DevelopStageType.ID, "newScanIdApp1");
    tempEntity.newPolicyViolation(evaluation, app1Policy, app1Policy.getThreatLevel(), app1Policy.getThreatCategory(),
        "g", "a", "v", "somehash");

    List<ComponentRiskDTO> riskDTOs = componentRiskService.getComponentRisks(null, null, null, null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    assertThat(riskDTOs.get(0).hash, is(app1PolicyViolation.getHash()));

    try {
      componentRiskService.getComponentRisks(null, null, Collections.singleton(DevelopStageType.ID), null, null, null,
          1000);
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Invalid stage type: develop."));
    }
  }

  @Test
  public void testGetComponentRisks_FilterByTag() throws Exception {
    Tag app2Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    List<ComponentRiskDTO> riskDTOs = componentRiskService.getComponentRisks(null, null, null,
        Collections.singleton(app2Tag.getId()), null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.hash, is(app2PolicyViolation.getHash()));
    assertDisplayFieldValues(riskDTO.displayName.parts, app2PolicyViolation);
    assertThat(riskDTO.score, is(orgPolicy.getThreatLevel()));
    assertThat(riskDTO.affectedApplications, is(1));
  }

  @Test
  public void testGetComponentRisks_FilterPolicyThreatCategory() throws Exception {
    PolicyViolation violation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, 5,
        PolicyThreatCategory.SECURITY, "gid", "aid", "1");

    List<ComponentRiskDTO> riskDTOs = componentRiskService.getComponentRisks(null, null, null, null,
        new PolicyThreatCategoryFilter(PolicyThreatCategory.SECURITY), null, 1000);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.hash, is(violation.getHash()));
    assertDisplayFieldValues(riskDTO.displayName.parts, violation);
    assertThat(riskDTO.score, is(app1Policy.getThreatLevel()));
    assertThat(riskDTO.affectedApplications, is(1));
  }

  @Test
  public void testGetComponentRisks_FilterPolicyThreatLevel() throws Exception {
    List<ComponentRiskDTO> riskDTOs = componentRiskService.getComponentRisks(null, null, null, null, null,
        new PolicyThreatLevelFilter(3, 3), 1000);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.hash, is(orgPolicyViolation.getHash()));
    assertDisplayFieldValues(riskDTO.displayName.parts, orgPolicyViolation);
    assertThat(riskDTO.score, is(orgPolicy.getThreatLevel() * 2));
    assertThat(riskDTO.affectedApplications, is(2));
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

    List<ComponentRiskDTO> riskDTOs = componentRiskService.getComponentRisks(null, Collections.singleton(app2.getId()),
        null, null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.scoreCritical, is(27));
    assertThat(riskDTO.scoreSevere, is(22));
    assertThat(riskDTO.scoreModerate, is(5));
    assertThat(riskDTO.scoreLow, is(1));
    assertThat(riskDTO.score, is(55));
    assertThat(riskDTO.affectedApplications, is(1));
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

    List<ComponentRiskDTO> riskDTOs = componentRiskService
        .getComponentRisks(Collections.singleton(app2.getParentOwnerId()), null, null, null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.scoreCritical, is(27));
    assertThat(riskDTO.scoreSevere, is(22));
    assertThat(riskDTO.scoreModerate, is(5));
    assertThat(riskDTO.scoreLow, is(1));
    assertThat(riskDTO.score, is(55));
    assertThat(riskDTO.affectedApplications, is(1));
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

    List<ComponentRiskDTO> riskDTOs = componentRiskService.getComponentRisks(null, null, null, null, null, null, 1);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.hash, is(hash));
    assertThat(riskDTO.score, is(12));
    assertThat(riskDTO.affectedApplications, is(2));
  }

  /**
   * Tests elimination of duplicates in query results for Application with multiple Tags that are present in a filter
   * Prior to the fix for this issue, the same call resulted in IAE when trying to Map duplicate Applications by their
   * IDs.
   * https://issues.sonatype.org/browse/CLM-3385
   */
  @Test
  public void testGetPolicyViolationsForApplicationWithMultipleTags() {
    List<PolicyViolationDTO> policyViolations = componentRiskService.getPolicyViolations(null,
        Sets.newHashSet(tag1.getId(), tag2.getId()), null);
    assertThat(policyViolations, hasSize(2));
    assertPolicyViolationDTO(policyViolations, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolations, app1PolicyViolation, app1, app1Policy);
  }
}
