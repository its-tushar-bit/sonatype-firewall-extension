/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.component.DisplayFieldValueAssertionUtil.assertDisplayFieldValues;
import static com.sonatype.insight.brain.dashboard.PolicyViolationDTOTestUtils.assertPolicyViolationDTO;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DashboardServiceTest
    extends AbstractComponentTest
{
  @Inject
  private DashboardService dashboardService;

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
    app2 = tempEntity.newApplication("app2", "app2", org.getId());
    orgPolicy = tempEntity.newPolicy(org.getId(), "org owned policy", 3);
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
    tag1 = tempEntity.newTag(org.getId());
    tag2 = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app1.getId(), tag1.getId());
    tempEntity.newApplicationTag(app1.getId(), tag2.getId());
  }

  @Test
  public void testGetPolicyViolationsWithBadStageTypeId() {
    String badStageTypeId = "not a real stage type id";
    try {
      dashboardService.getPolicyViolations(Sets.newHashSet(badStageTypeId), null, null);
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
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null);
    assertThat(policyViolationDTOs, hasSize(0));

    Set<String> stageTypeIds = Sets.newHashSet(BuildStageType.ID);
    try {
      policyViolationDTOs = dashboardService.getPolicyViolations(stageTypeIds, null, null);
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
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null);

    assertThat(policyViolationDTOs, hasSize(4));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);

    Set<String> stageTypeIds = Collections.emptySet();
    policyViolationDTOs = dashboardService.getPolicyViolations(stageTypeIds, null, null);

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
      dashboardService.getPolicyViolationsByApplicationIds(nullApplicationId, null, null, null);
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
      dashboardService.getPolicyViolationsByApplicationIds(emptyApplicationId, null, null, null);
      fail("Expected BadRequestException to be thrown.");
    }
    catch (BadRequestException e) {
      assertEquals(e.getMessage(), "Unable to get policy violations for null or empty application IDs.");
    }
  }

  @Test
  public void testGetPolicyViolations() {
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null);
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

    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(
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

    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null);
    assertThat(policyViolationDTOs, hasSize(3));

    try {
      dashboardService.getPolicyViolations(Collections.singleton(DevelopStageType.ID), null, null);
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

    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(
        Sets.newHashSet(ReleaseStageType.ID), null,
        new PolicyThreatCategoryFilter(PolicyThreatCategory.OTHER).asPolicyViolationPredicate());

    assertThat(policyViolationDTOs, hasSize(0));

    policyViolationDTOs = dashboardService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), null,
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
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(
        Sets.newHashSet(ReleaseStageType.ID), null, new PolicyThreatLevelFilter(6, 7).asPolicyViolationPredicate());

    assertThat(policyViolationDTOs, hasSize(0));

    // Violation in range.
    policyViolationDTOs = dashboardService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), null,
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
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(Sets
        .newHashSet(ReleaseStageType.ID), null, Predicates.and(new PolicyThreatCategoryFilter(
        PolicyThreatCategory.OTHER).asPolicyViolationPredicate(), new PolicyThreatLevelFilter(6, 7)
        .asPolicyViolationPredicate()));
    assertThat(policyViolationDTOs, hasSize(0));

    // Violation out of threat level range and correct threat category.
    policyViolationDTOs = dashboardService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), null, Predicates
        .and(new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE).asPolicyViolationPredicate(),
            new PolicyThreatLevelFilter(6, 7).asPolicyViolationPredicate()));
    assertThat(policyViolationDTOs, hasSize(0));

    // Violation in range, but wrong threat category.
    policyViolationDTOs = dashboardService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), null, Predicates
        .and(new PolicyThreatCategoryFilter(PolicyThreatCategory.OTHER).asPolicyViolationPredicate(),
            new PolicyThreatLevelFilter(5, 7).asPolicyViolationPredicate()));
    assertThat(policyViolationDTOs, hasSize(0));

    // Violation in range and in the correct category.
    policyViolationDTOs = dashboardService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), null, Predicates
        .and(new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE).asPolicyViolationPredicate(),
            new PolicyThreatLevelFilter(5, 7).asPolicyViolationPredicate()));

    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);
  }

  @Test
  public void testGetPolicyViolationsReturnsLatest() {
    // Create a new evaluation for app1 that does not include any violations.
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "re-scan app1");
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null);

    // The violations count should be 1, all of which come from app2.
    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationIds() {
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(
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

    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app1.getId(), app2.getId()),
        Sets.newHashSet(BuildStageType.ID, ReleaseStageType.ID), null, null);
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
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app1.getId(), app2.getId()), null, null, null);

    // The violations count should be 1, all of which come from app2.
    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsByTags() {
    Tag app1Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app1.getId(), app1Tag.getId());

    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null,
        Sets.newHashSet(app1Tag.getId()), null);
    assertThat(policyViolationDTOs, hasSize(2));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);

    Tag app2Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    policyViolationDTOs = dashboardService.getPolicyViolations(null, Sets.newHashSet(app1Tag.getId(), app2Tag.getId()),
        null);
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

    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, new HashSet<String>(),
        null);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);

    policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null);
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
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, app1Policy,
        app1Policy.getThreatLevel() + 1, PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1");

    List<ComponentRiskDTO> riskDTOs = dashboardService.getComponentRisks(null, null, null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.hash, is(violation.getHash()));
    assertDisplayFieldValues(riskDTO.displayName.parts, violation);
    assertThat(riskDTO.score, is(violation.getThreatLevel() + orgPolicy.getThreatLevel() * 2));
    assertThat(riskDTO.affectedApplications, is(2));
  }

  @Test
  public void testGetComponentRisks_FilterByApplication() throws Exception {
    List<ComponentRiskDTO> riskDTOs = dashboardService.getComponentRisks(Collections.singleton(app2.getId()),
        null, null, null, null, 1000);
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

    List<ComponentRiskDTO> riskDTOs = dashboardService.getComponentRisks(null,
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

    List<ComponentRiskDTO> riskDTOs = dashboardService.getComponentRisks(null, null, null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    assertThat(riskDTOs.get(0).hash, is(app1PolicyViolation.getHash()));

    try {
      dashboardService.getComponentRisks(null, Collections.singleton(DevelopStageType.ID), null, null, null, 1000);
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

    List<ComponentRiskDTO> riskDTOs = dashboardService.getComponentRisks(null, null,
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

    List<ComponentRiskDTO> riskDTOs = dashboardService.getComponentRisks(null, null, null,
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
    List<ComponentRiskDTO> riskDTOs = dashboardService.getComponentRisks(null, null, null, null,
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

    List<ComponentRiskDTO> riskDTOs = dashboardService.getComponentRisks(Collections.singleton(app2.getId()),
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
  public void testGetComponentRisks_ResultCapping() throws Exception {
    String gid = "gid", aid = "aid", ver = "1", hash = "somehash";
    tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy, 4, PolicyThreatCategory.SECURITY, gid, aid, ver,
        hash);
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, 4, PolicyThreatCategory.SECURITY, gid, aid, ver,
        hash);
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy, 4, PolicyThreatCategory.SECURITY, gid, aid, ver,
        hash);

    List<ComponentRiskDTO> riskDTOs = dashboardService.getComponentRisks(null, null, null, null, null, 1);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.hash, is(hash));
    assertThat(riskDTO.score, is(12));
    assertThat(riskDTO.affectedApplications, is(2));
  }

  @Test
  public void testGetApplicationRisks_FilterByStage_ExcludesDevelop() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), DevelopStageType.ID, "newScanIdApp1");
    tempEntity.newPolicyViolation(evaluation, app1Policy, app1Policy.getThreatLevel(), app1Policy.getThreatCategory(),
        "g", "a", "v", "somehash");

    List<ApplicationRiskScoreDTO> riskDTOs = dashboardService.getApplicationRisks(null, null, null, null, null, 100);
    assertThat(riskDTOs, hasSize(2));
    assertThat(riskDTOs.get(0).getStageRiskScore(DevelopStageType.ID), is(nullValue()));
    assertThat(riskDTOs.get(1).getStageRiskScore(DevelopStageType.ID), is(nullValue()));

    try {
      dashboardService.getApplicationRisks(null, Collections.singleton(DevelopStageType.ID), null, null, null, 100);
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Invalid stage type: develop."));
    }
  }

  @Test
  public void testGetApplicationRisks_StagesInChronologicalOrder() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), OperateStageType.ID, "scan app1 id");
    tempEntity.newPolicyViolation(evaluation, app1Policy);
    evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "scan app1 id");
    tempEntity.newPolicyViolation(evaluation, app1Policy);
    evaluation = tempEntity.newPolicyEvaluation(app1.getId(), StageReleaseStageType.ID, "scan app1 id");
    tempEntity.newPolicyViolation(evaluation, app1Policy);

    List<ApplicationRiskScoreDTO> riskDTOs = dashboardService.getApplicationRisks(
        Collections.singleton(app1.getId()),
        new LinkedHashSet<>(Arrays.asList(ReleaseStageType.ID, OperateStageType.ID, BuildStageType.ID,
            StageReleaseStageType.ID)), null, null, null, 100);
    assertThat(riskDTOs, hasSize(1));
    ApplicationRiskScoreDTO appDTO = riskDTOs.get(0);
    assertThat(appDTO.stageRisks, hasSize(4));
    assertThat(appDTO.stageRisks.get(0).stageTypeId, is(BuildStageType.ID));
    assertThat(appDTO.stageRisks.get(1).stageTypeId, is(StageReleaseStageType.ID));
    assertThat(appDTO.stageRisks.get(2).stageTypeId, is(ReleaseStageType.ID));
    assertThat(appDTO.stageRisks.get(3).stageTypeId, is(OperateStageType.ID));
  }

  @Test
  public void testGetApplicationRisks_ViolationForComponentWithoutHash() throws Exception {
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, null, null, null, null, "unknown");

    List<ApplicationRiskScoreDTO> riskDTOs = dashboardService.getApplicationRisks(Collections.singleton(app1.getId()),
        null, null, null, null, 100);
    assertThat(riskDTOs, hasSize(1));
    ApplicationRiskScoreDTO appDTO = riskDTOs.get(0);
    assertThat(appDTO.stageRisks, hasSize(1));
    assertThat(appDTO.stageRisks.get(0).stageTypeId, is(BuildStageType.ID));
    assertThat(appDTO.stageRisks.get(0).risk.totalRisk,
        is(orgPolicy.getThreatLevel() + app1Policy.getThreatLevel() * 2));
  }

  @Test
  public void testGetFilterSummary_NoFilter() throws Exception {
    FilterSummaryDTO summary = dashboardService.getFilterSummary(null, null, null, null, null);
    assertThat(summary.totalApplications, is(2));
    assertThat(summary.matchedApplications, is(2));
    assertThat(summary.totalPolicies, is(2));
    assertThat(summary.matchedPolicies, is(2));
    assertThat(summary.totalComponents, is(4));
    assertThat(summary.matchedComponents, is(4));
  }

  @Test
  public void testGetFilterSummary_FilterByApp() throws Exception {
    FilterSummaryDTO summary = dashboardService.getFilterSummary(Collections.singleton(app2.getId()), null, null,
        null, null);
    assertThat(summary.totalApplications, is(2));
    assertThat(summary.matchedApplications, is(1));
    assertThat(summary.totalPolicies, is(2));
    assertThat(summary.matchedPolicies, is(1));
    assertThat(summary.totalComponents, is(4));
    assertThat(summary.matchedComponents, is(1));
  }

  @Test
  public void testGetFilterSummary_FilterByTag() throws Exception {
    Tag app2Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    FilterSummaryDTO summary = dashboardService.getFilterSummary(null, null, Collections.singleton(app2Tag.getId()),
        null, null);
    assertThat(summary.totalApplications, is(2));
    assertThat(summary.matchedApplications, is(1));
    assertThat(summary.totalPolicies, is(2));
    assertThat(summary.matchedPolicies, is(1));
    assertThat(summary.totalComponents, is(4));
    assertThat(summary.matchedComponents, is(1));
  }

  @Test
  public void testGetFilterSummary_FilterByPolicyThreatLevel() throws Exception {
    FilterSummaryDTO summary = dashboardService.getFilterSummary(null, null, null, null, new PolicyThreatLevelFilter(
        orgPolicy.getThreatLevel(), orgPolicy.getThreatLevel()));
    assertThat(summary.totalApplications, is(2));
    assertThat(summary.matchedApplications, is(2));
    assertThat(summary.totalPolicies, is(2));
    assertThat(summary.matchedPolicies, is(1));
    assertThat(summary.totalComponents, is(4));
    assertThat(summary.matchedComponents, is(4));
  }

  @Test
  public void testGetFilterSummary_FilterByPolicyThreatCategory() throws Exception {
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(LicenseConditionType.ID, "is", "GPL-2.0"));
    orgPolicy.setConstraints(Collections.singletonList(constraint));
    new PolicyDAO().update(orgPolicy);

    FilterSummaryDTO summary = dashboardService.getFilterSummary(null, null, null, new PolicyThreatCategoryFilter(
        PolicyThreatCategory.LICENSE), null);
    assertThat(summary.totalApplications, is(2));
    assertThat(summary.matchedApplications, is(2));
    assertThat(summary.totalPolicies, is(2));
    assertThat(summary.matchedPolicies, is(1));
    assertThat(summary.totalComponents, is(4));
    assertThat(summary.matchedComponents, is(4));
  }

  @Test
  public void testGetFilterSummary_FilterByStage() throws Exception {
    FilterSummaryDTO summary = dashboardService.getFilterSummary(null, Collections.singleton(ReleaseStageType.ID),
        null, null, null);
    assertThat(summary.totalApplications, is(2));
    assertThat(summary.matchedApplications, is(2));
    assertThat(summary.totalPolicies, is(2));
    assertThat(summary.matchedPolicies, is(2));
    assertThat(summary.totalComponents, is(4));
    assertThat(summary.matchedComponents, is(2));
  }

  private void assertNewestRiskDTO(NewestRiskDTO actual, Application app, PolicyViolation policyViolation, Date time) {
    assertThat(actual.applicationName, is(app.getName()));
    assertThat(actual.applicationPublicId, is(app.getPublicId()));
    assertThat(actual.threatLevel, is(policyViolation.getThreatLevel()));
    assertThat(actual.time, is(time.getTime()));
    assertThat(actual.policyName, is(policyViolation.getPolicyName()));
    assertThat(actual.policyId, is(policyViolation.getPolicyId()));
    assertThat(actual.hash, is(policyViolation.getHash()));
    if (policyViolation.getComponentIdentifier() != null) {
      assertDisplayFieldValues(actual.displayName.parts, policyViolation);
    }
    else {
      assertThat(actual.displayName, is(nullValue()));
    }
    assertThat(actual.pathnames, is(policyViolation.getPathnames()));
  }

  private void assertNewestRiskDTOContainsStageDetails(NewestRiskDTO actual, String stageTypeId, String scanId,
      String actionTypeId, Date time)
  {
    for (StageDetailDTO stageDetailDTO : actual.stageDetails) {
      if (stageTypeId.equals(stageDetailDTO.stageTypeId)) {
        assertThat(stageDetailDTO.actionTypeId, is(actionTypeId));
        assertThat(stageDetailDTO.time, is(time.getTime()));
        assertThat(stageDetailDTO.scanId, is(scanId));
        return;
      }
    }
    fail("NewestRiskDTO does not contain details for stage " + stageTypeId);
  }

  private void assertNewestRiskDTOContainsEmptyStageDetails(NewestRiskDTO actual, String stageTypeId)
  {
    for (StageDetailDTO stageDetailDTO : actual.stageDetails) {
      if (stageTypeId.equals(stageDetailDTO.stageTypeId)) {
        assertThat(stageDetailDTO.actionTypeId, nullValue());
        assertThat(stageDetailDTO.time, is(nullValue()));
        assertThat(stageDetailDTO.scanId, nullValue());
        return;
      }
    }
    fail("NewestRiskDTO does not contain details for stage " + stageTypeId);
  }

  @Test
  public void testGetNewestRisks_FilterByApplication() throws Exception {
    List<NewestRiskDTO> riskDTOs = dashboardService.getNewestRisks(Collections.singleton(app2.getId()), null,
        null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO, app2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByStage() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanIdApp1");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, app1Policy);
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation.getId(), app1.getId(), ReleaseStageType.ID);

    List<NewestRiskDTO> riskDTOs = dashboardService.getNewestRisks(null, Collections.singleton(ReleaseStageType.ID),
        null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO, app1, policyViolation, evaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByStage_ExcludesDevelop() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app2.getId(), DevelopStageType.ID, "newScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation.getId(), app2.getId(), DevelopStageType.ID);

    List<NewestRiskDTO> riskDTOs = dashboardService.getNewestRisks(Collections.singleton(app2.getId()), null,
        null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO, app2, app2PolicyViolation, app2PolicyEvaluation.getTime());
    for (StageDetailDTO stageDetailDTO : riskDTO.stageDetails) {
      assertThat(stageDetailDTO.stageTypeId, is(not(DevelopStageType.ID)));
    }

    try {
      dashboardService.getNewestRisks(null, Collections.singleton(DevelopStageType.ID), null, null, null, 1000);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Invalid stage type: develop."));
    }
  }

  @Test
  public void testGetNewestRisks_FilterByTag() throws Exception {
    Tag app2Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    List<NewestRiskDTO> riskDTOs = dashboardService.getNewestRisks(null, null, Collections.singleton(app2Tag.getId()),
        null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO, app2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByPolicyThreatCategory() throws Exception {
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, 5,
        PolicyThreatCategory.OTHER, "gid", "aid", "1", "hash1");
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation.getId(), app1.getId(), BuildStageType.ID);

    List<NewestRiskDTO> riskDTOs = dashboardService.getNewestRisks(null, null, null, new PolicyThreatCategoryFilter(
        PolicyThreatCategory.OTHER), null, 1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO, app1, policyViolation, app1PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_FilterByPolicyThreatLevel() throws Exception {
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, 7,
        PolicyThreatCategory.OTHER, "gid", "aid", "1", "hash1");
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation.getId(), app1.getId(), BuildStageType.ID);
    List<NewestRiskDTO> riskDTOs = dashboardService.getNewestRisks(null, null, null, null, new PolicyThreatLevelFilter(
        7, 7), 1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTOs, hasSize(1));
    assertNewestRiskDTO(riskDTO, app1, policyViolation, app1PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_SortAndResultCapping() throws Exception {
    // Limit to high value
    List<NewestRiskDTO> riskDTOs = dashboardService.getNewestRisks(null, null, null, null, null, 100);
    assertThat(riskDTOs, hasSize(3));
    assertNewestRiskDTO(riskDTOs.get(0), app2, app2PolicyViolation, app2PolicyEvaluation.getTime());
    assertNewestRiskDTO(riskDTOs.get(1), app1, app1PolicyViolation, app1PolicyEvaluation.getTime());
    assertNewestRiskDTO(riskDTOs.get(2), app1, orgPolicyViolation, app1PolicyEvaluation.getTime());

    // Limit to 1
    riskDTOs = dashboardService.getNewestRisks(null, null, null, null, null, 1);
    assertThat(riskDTOs, hasSize(1));
    assertNewestRiskDTO(riskDTOs.get(0), app2, app2PolicyViolation, app2PolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks() throws Exception {
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "test scan app2 release id", new Date(app1PolicyEvaluation.getTime().getTime() + 1));
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, app1Policy,
        app1PolicyViolation.getThreatLevel(), app1PolicyViolation.getThreatCategory(),
        app1PolicyViolation.getComponentIdentifier(), app1PolicyViolation.getHash());
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation.getId(), app1.getId(), ReleaseStageType.ID);

    List<NewestRiskDTO> riskDTOs = dashboardService.getNewestRisks(null, null, null, null, null, 100);
    assertThat(riskDTOs, hasSize(3));

    NewestRiskDTO riskDTO0 = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO0, app1, policyViolation, policyEvaluation.getTime());
    assertThat(riskDTO0.stageDetails, hasSize(4));
    assertNewestRiskDTOContainsStageDetails(riskDTO0, BuildStageType.ID, app1PolicyEvaluation.getScanId(),
        app1PolicyViolation.getActionTypeId(), app1PolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetails(riskDTO0, ReleaseStageType.ID, policyEvaluation.getScanId(),
        policyViolation.getActionTypeId(), policyEvaluation.getTime());
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO0, StageReleaseStageType.ID);
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO0, OperateStageType.ID);

    NewestRiskDTO riskDTO1 = riskDTOs.get(1);
    assertNewestRiskDTO(riskDTO1, app2, app2PolicyViolation, app2PolicyEvaluation.getTime());
    assertThat(riskDTO1.stageDetails, hasSize(4));
    assertNewestRiskDTOContainsStageDetails(riskDTO1, BuildStageType.ID, app2PolicyEvaluation.getScanId(),
        app2PolicyViolation.getActionTypeId(), app2PolicyEvaluation.getTime());
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO1, StageReleaseStageType.ID);
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO1, ReleaseStageType.ID);
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO1, OperateStageType.ID);

    NewestRiskDTO riskDTO2 = riskDTOs.get(2);
    assertNewestRiskDTO(riskDTO2, app1, orgPolicyViolation, app1PolicyEvaluation.getTime());
    assertThat(riskDTO2.stageDetails, hasSize(4));
    assertNewestRiskDTOContainsStageDetails(riskDTO2, BuildStageType.ID, app1PolicyEvaluation.getScanId(),
        app1PolicyViolation.getActionTypeId(), app1PolicyEvaluation.getTime());
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO2, StageReleaseStageType.ID);
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO2, ReleaseStageType.ID);
    assertNewestRiskDTOContainsEmptyStageDetails(riskDTO2, OperateStageType.ID);
  }

  @Test
  public void testGetNewestRisks_NewerThanNDays() throws Exception {
    Application app = tempEntity.newApplication("myapp", "myapp", org.getId());

    Date beforeNDays = new DateTime().minusDays(DashboardService.NEWEST_RISK_TIME_RANGE_IN_DAYS + 1).toDate();
    String oldScanId = "test old scan id";
    PolicyEvaluation oldPolicyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, oldScanId,
        beforeNDays);
    PolicyViolation oldPolicyViolation = tempEntity.newPolicyViolation(oldPolicyEvaluation, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(oldPolicyViolation.getId(), app.getId(), BuildStageType.ID);

    List<NewestRiskDTO> riskDTOs = dashboardService.getNewestRisks(Collections.singleton(app.getId()), null,
        null, null, null, 100);
    assertThat(riskDTOs, hasSize(0));

    Date afterNDays = new DateTime().minusDays(DashboardService.NEWEST_RISK_TIME_RANGE_IN_DAYS - 1).toDate();
    String newScanId = "test new scan id";
    PolicyEvaluation newPolicyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, newScanId,
        afterNDays);
    PolicyViolation newPolicyViolation = tempEntity.newPolicyViolation(newPolicyEvaluation, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(newPolicyViolation.getId(), app.getId(), ReleaseStageType.ID);

    riskDTOs = dashboardService.getNewestRisks(Collections.singleton(app.getId()), null, null, null, null, 100);
    assertThat(riskDTOs, hasSize(1));
    assertNewestRiskDTO(riskDTOs.get(0), app, newPolicyViolation, newPolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetails(riskDTOs.get(0), BuildStageType.ID, oldScanId,
        orgPolicyViolation.getActionTypeId(), oldPolicyEvaluation.getTime());
    assertNewestRiskDTOContainsStageDetails(riskDTOs.get(0), ReleaseStageType.ID, newScanId,
        newPolicyViolation.getActionTypeId(), newPolicyEvaluation.getTime());
  }

  @Test
  public void testGetNewestRisks_LastViolationNotFirstOccurrence() throws Exception {
    Application app = tempEntity.newApplication("myapp", "myapp", org.getId());

    DateTime time1 = new DateTime().minusDays(1);
    String scanId1 = "scanId1";
    PolicyEvaluation policyEval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId1,
        time1.toDate());
    PolicyViolation policyViolation1 = tempEntity.newPolicyViolation(policyEval1, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation1.getId(), app.getId(), BuildStageType.ID);

    DateTime time2 = time1.plusHours(1);
    String scanId2 = "scanId2";
    PolicyEvaluation policyEval2 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId2,
        time2.toDate());
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEval2, orgPolicy);

    DateTime time3 = time2.plusHours(1);
    String scanId3 = "scanId3";
    PolicyEvaluation policyEval3 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId3,
        time3.toDate());
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEval3, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation3.getId(), app.getId(), ReleaseStageType.ID);

    DateTime time4 = time3.plusHours(1);
    String scanId4 = "scanId4";
    PolicyEvaluation policyEval4 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId4,
        time4.toDate());
    PolicyViolation policyViolation4 = tempEntity.newPolicyViolation(policyEval4, orgPolicy);

    List<NewestRiskDTO> riskDTOs = dashboardService.getNewestRisks(Collections.singleton(app.getId()), null,
        null, null, null, 100);
    assertThat(riskDTOs, hasSize(1));
    assertNewestRiskDTO(riskDTOs.get(0), app, policyViolation1, policyEval3.getTime());
    assertNewestRiskDTOContainsStageDetails(riskDTOs.get(0), BuildStageType.ID, scanId2,
        policyViolation2.getActionTypeId(), policyEval1.getTime());
    assertNewestRiskDTOContainsStageDetails(riskDTOs.get(0), ReleaseStageType.ID, scanId4,
        policyViolation4.getActionTypeId(), policyEval3.getTime());
  }

  @Test
  public void testGetNewestRisks_ViolationWithoutHash() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "newScanId");
    PolicyViolation policyViolation = tempEntity
        .newPolicyViolation(evaluation, app1Policy, "g", "a", "v", null /* hash */, "reason");

    List<NewestRiskDTO> riskDTOs = dashboardService.getNewestRisks(null, Collections.singleton(ReleaseStageType.ID),
        null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    NewestRiskDTO riskDTO = riskDTOs.get(0);
    assertNewestRiskDTO(riskDTO, app1, policyViolation, evaluation.getTime());
  }

  @Test
  public void testGetComponentSummary_NoFilter() throws Exception {
    ComponentSummaryDTO summary = dashboardService.getComponentSummary(null, null, null);
    assertThat(summary.total, is(4));
    assertThat(summary.exact, is(2));
    assertThat(summary.similar, is(1));
    assertThat(summary.unknown, is(1));
  }

  @Test
  public void testGetComponentSummary_FilterByApp() throws Exception {
    ComponentSummaryDTO summary = dashboardService.getComponentSummary(Collections.singleton(app1.getId()), null,
        null);
    assertThat(summary.total, is(3));
    assertThat(summary.exact, is(1));
    assertThat(summary.similar, is(1));
    assertThat(summary.unknown, is(1));
  }

  @Test
  public void testGetComponentSummary_FilterByTag() throws Exception {
    Tag app2Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    ComponentSummaryDTO summary = dashboardService.getComponentSummary(null, null,
        Collections.singleton(app2Tag.getId()));
    assertThat(summary.total, is(1));
    assertThat(summary.exact, is(1));
    assertThat(summary.similar, is(0));
    assertThat(summary.unknown, is(0));
  }

  @Test
  public void testGetComponentSummary_FilterByStage() throws Exception {
    ComponentSummaryDTO summary = dashboardService.getComponentSummary(null,
        Collections.singleton(ReleaseStageType.ID), null);
    assertThat(summary.total, is(2));
    assertThat(summary.exact, is(0));
    assertThat(summary.similar, is(1));
    assertThat(summary.unknown, is(1));
  }

  @Test
  public void testGetComponentSummary_ExcludesProprietaryComponents() throws Exception {
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-x", MatchState.SIMILAR, true);

    ComponentSummaryDTO summary = dashboardService.getComponentSummary(null, null, null);
    assertThat(summary.total, is(4));
    assertThat(summary.exact, is(2));
    assertThat(summary.similar, is(1));
    assertThat(summary.unknown, is(1));
  }

  @Test
  public void testGetComponentSummary_UsesMostRecentMatchState() throws Exception {
    tempEntity.newApplicationComponent(app2.getId(), ReleaseStageType.ID, "hash-1", MatchState.SIMILAR, false);

    ComponentSummaryDTO summary = dashboardService.getComponentSummary(null, null, null);
    assertThat(summary.total, is(4));
    assertThat(summary.exact, is(1));
    assertThat(summary.similar, is(2));
    assertThat(summary.unknown, is(1));
  }

  @Test
  public void testDashboardFilterDefaultFilter() throws Exception {
    DashboardFilterDTO actual = dashboardService.getDashboardFilterForCurrentUser();
    // Register to make sure the the filter is deleted after the test
    tempEntity.register(new DashboardFilterDAO().getByUsername(USERNAME));
    assertThat(actual, notNullValue());

    assertThat(actual.minPolicyThreatLevel, is(2));
    assertThat(actual.maxPolicyThreatLevel, is(10));
    assertThat(actual.applicationFilters, hasSize(0));
    assertThat(actual.tagFilters, hasSize(0));
    assertThat(actual.policyThreatCategoryFilters, hasSize(0));
    assertThat(actual.stageTypeFilters, hasSize(0));
  }

  /**
   * Tests elimination of duplicates in query results for Application with multiple Tags that are present in a filter
   * Prior to the fix for this issue, the same call resulted in IAE when trying to Map duplicate Applications by their
   * IDs.
   * https://issues.sonatype.org/browse/CLM-3385
   */
  @Test
  public void testGetPolicyViolationsForApplicationWithMultipleTags() {
    List<PolicyViolationDTO> policyViolations = dashboardService
        .getPolicyViolations(null, Sets.newHashSet(tag1.getId(), tag2.getId()), null);
    assertThat(policyViolations, hasSize(2));
    assertPolicyViolationDTO(policyViolations, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolations, app1PolicyViolation, app1, app1Policy);
  }
}
