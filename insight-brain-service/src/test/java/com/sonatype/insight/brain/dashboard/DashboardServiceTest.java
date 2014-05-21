/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.NewestPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
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
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.PolicyViolationDTOTestUtils.assertPolicyViolationDTO;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DashboardServiceTest
    extends AbstractComponentTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

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

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication(org.getId());
    app2 = tempEntity.newApplication(org.getId());
    orgPolicy = tempEntity.newPolicy(org.getId(), "org owned policy", 3);
    app1Policy = tempEntity.newPolicy(app1.getId(), "app owned policy", 5);
    app1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id");
    app2PolicyEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "test scan app2 id");
    long start = System.currentTimeMillis();
    orgPolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy);
    app1PolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);
    app2PolicyViolation = tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy);
    tempEntity.newNewestPolicyViolation(orgPolicyViolation.getId(), app1.getId(), BuildStageType.ID);
    tempEntity.newNewestPolicyViolation(app1PolicyViolation.getId(), app1.getId(), BuildStageType.ID);
    tempEntity.newNewestPolicyViolation(app2PolicyViolation.getId(), app2.getId(), BuildStageType.ID);
    while (System.currentTimeMillis() <= start) {
      // just spinning until next policy eval time is guaranteed to be greater than time for the evals created above
    }
  }

  @Test
  public void testGetPolicyViolationsWithBadStageTypeId() {
    String badStageTypeId = "not a real stage type id";
    try {
      dashboardService.getPolicyViolations(Sets.newHashSet(badStageTypeId), null, null, null, false);
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
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null, null, false);
    assertThat(policyViolationDTOs, hasSize(0));

    Set<String> stageTypeIds = Sets.newHashSet(BuildStageType.ID);
    try {
      policyViolationDTOs = dashboardService.getPolicyViolations(stageTypeIds, null, null, null, false);
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
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null, null, false);

    assertThat(policyViolationDTOs, hasSize(4));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);

    Set<String> stageTypeIds = Collections.emptySet();
    policyViolationDTOs = dashboardService.getPolicyViolations(stageTypeIds, null, null, null, false);

    assertThat(policyViolationDTOs, hasSize(4));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);
  }

  @Test
  public void testGetPolicyViolationsWithNullApplicationPublicIds() {
    Set<String> nullApplicationPublicId = null;
    try {
      dashboardService.getPolicyViolationsByApplicationIds(nullApplicationPublicId, null, null, null, null, false);
      fail("Expected BadRequestException to be thrown.");
    }
    catch (BadRequestException e) {
      assertEquals(e.getMessage(), "Unable to get policy violations for null or empty application public IDs.");
    }
  }

  @Test
  public void testGetPolicyViolationsWithEmptyApplicationPublicIds() {
    Set<String> emptyApplicationPublicId = new HashSet<>();
    try {
      dashboardService.getPolicyViolationsByApplicationIds(emptyApplicationPublicId, null, null, null, null, false);
      fail("Expected BadRequestException to be thrown.");
    }
    catch (BadRequestException e) {
      assertEquals(e.getMessage(), "Unable to get policy violations for null or empty application public IDs.");
    }
  }

  @Test
  public void testGetNewestPolicyViolations() {
    // Initial scan should have all the violations as 'newest'.
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null, null, true);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);

    // Force a violation to no longer be new.
    NewestPolicyViolationDAO newestPolicyViolationDAO = new NewestPolicyViolationDAO();
    newestPolicyViolationDAO.delete(newestPolicyViolationDAO.getById(orgPolicyViolation.getId()));
    policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null, null, true);

    assertThat(policyViolationDTOs, hasSize(2));
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);

    // Ensure that there are still 3 violations in the system.
    assertThat(dashboardService.getPolicyViolations(null, null, null, null, false), hasSize(3));
  }

  @Test
  public void testGetPolicyViolations() {
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null, null, false);
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
        Sets.newHashSet(BuildStageType.ID, ReleaseStageType.ID), null, null, null, false);
    assertThat(policyViolationDTOs, hasSize(4));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);
  }

  @Test
  public void testGetPolicyViolations_FilterByStage_ExcludesDevelop() {
    PolicyEvaluation evaluation = tempEntity
        .newPolicyEvaluation(app1.getId(), DevelopStageType.ID, "test scan app1 id");
    tempEntity.newPolicyViolation(evaluation, app1Policy);

    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null, null, false);
    assertThat(policyViolationDTOs, hasSize(3));

    try {
      dashboardService.getPolicyViolations(Collections.singleton(DevelopStageType.ID), null, null, null, false);
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
        new PolicyThreatCategoryFilter(PolicyThreatCategory.OTHER).asPolicyViolationPredicate(), null, false);

    assertThat(policyViolationDTOs, hasSize(0));

    policyViolationDTOs = dashboardService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), null,
        new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE).asPolicyViolationPredicate(), null, false);

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
        Sets.newHashSet(ReleaseStageType.ID), null,
        new PolicyThreatLevelFilter(6, 7).asPolicyViolationPredicate(), null, false);

    assertThat(policyViolationDTOs, hasSize(0));

    // Violation in range.
    policyViolationDTOs = dashboardService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), null,
        new PolicyThreatLevelFilter(5, 7).asPolicyViolationPredicate(),
        null, false);

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
        .newHashSet(ReleaseStageType.ID), null, Predicates
        .and(new PolicyThreatCategoryFilter(PolicyThreatCategory.OTHER).asPolicyViolationPredicate(),
            new PolicyThreatLevelFilter(6, 7).asPolicyViolationPredicate()), null, false);
    assertThat(policyViolationDTOs, hasSize(0));

    // Violation out of threat level range and correct threat category.
    policyViolationDTOs = dashboardService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), null, Predicates
        .and(new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE).asPolicyViolationPredicate(),
            new PolicyThreatLevelFilter(6, 7).asPolicyViolationPredicate()), null, false);
    assertThat(policyViolationDTOs, hasSize(0));

    // Violation in range, but wrong threat category.
    policyViolationDTOs = dashboardService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), null, Predicates
        .and(new PolicyThreatCategoryFilter(PolicyThreatCategory.OTHER).asPolicyViolationPredicate(),
            new PolicyThreatLevelFilter(5, 7).asPolicyViolationPredicate()), null, false);
    assertThat(policyViolationDTOs, hasSize(0));

    // Violation in range and in the correct category.
    policyViolationDTOs = dashboardService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), null, Predicates
        .and(new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE).asPolicyViolationPredicate(),
            new PolicyThreatLevelFilter(5, 7).asPolicyViolationPredicate()), null, false);

    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);
  }

  @Test
  public void testGetPolicyViolationsReturnsLatest() {
    // Create a new evaluation for app1 that does not include any violations.
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "re-scan app1");
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null, null, false);

    // The violations count should be 1, all of which come from app2.
    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationIds() {
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app1.getPublicId(), app2.getPublicId()), null, null, null, null, false);
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
        Sets.newHashSet(app1.getPublicId(), app2.getPublicId()),
        Sets.newHashSet(BuildStageType.ID, ReleaseStageType.ID), null, null, null, false);
    assertThat(policyViolationDTOs, hasSize(4));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);
  }

  @Test
  public void testGetNewestPolicyViolationsByApplicationIds() {
    Set<String> applicationIds = Sets.newHashSet(app1.getPublicId(), app2.getPublicId());
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(applicationIds,
        null, null, null, null, true);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);

    // Force a violation to no longer be new.
    NewestPolicyViolationDAO newestPolicyViolationDAO = new NewestPolicyViolationDAO();
    newestPolicyViolationDAO.delete(newestPolicyViolationDAO.getById(orgPolicyViolation.getId()));
    policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(applicationIds, null, null, null, null, true);

    assertThat(policyViolationDTOs, hasSize(2));
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);

    // Ensure that there are still 3 violations in the system.
    assertThat(dashboardService.getPolicyViolationsByApplicationIds(applicationIds, null, null, null, null, false),
        hasSize(3));
  }

  @Test
  public void testGetPolicyViolationsByApplicationIdsReturnsLatest() {
    // Create a new evaluation for app1 that does not include any violations.
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "re-scan app1");
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app1.getPublicId(), app2.getPublicId()), null, null, null, null, false);

    // The violations count should be 1, all of which come from app2.
    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testPolicyViolationFilter() {
    PolicyViolation v1 = new PolicyViolation();
    PolicyViolation v2 = new PolicyViolation();
    PolicyViolation v3 = new PolicyViolation();
    PolicyViolation v4 = new PolicyViolation();
    PolicyViolation v5 = new PolicyViolation();
    PolicyViolation v6 = new PolicyViolation();
    v1.setThreatCategory(PolicyThreatCategory.LICENSE);
    v2.setThreatCategory(PolicyThreatCategory.LICENSE);
    v3.setThreatCategory(PolicyThreatCategory.OTHER);
    v4.setThreatCategory(PolicyThreatCategory.QUALITY);
    v5.setThreatCategory(PolicyThreatCategory.SECURITY);
    v6.setThreatCategory(PolicyThreatCategory.SECURITY);
    v1.setThreatLevel(1);
    v2.setThreatLevel(2);
    v3.setThreatLevel(3);
    v4.setThreatLevel(4);
    v5.setThreatLevel(5);
    v6.setThreatLevel(6);

    List<PolicyViolation> violations = Lists.newArrayList(v1, v2, v3, v4, v5, v6);
    List<PolicyViolation> filtered;

    // Test minimum range.
    filtered = dashboardService.filter(violations, new PolicyThreatLevelFilter(3, null).asPolicyViolationPredicate());
    assertThat(filtered, contains(v3, v4, v5, v6));

    // Test maximum range.
    filtered = dashboardService.filter(violations, new PolicyThreatLevelFilter(null, 3).asPolicyViolationPredicate());
    assertThat(filtered, contains(v1, v2, v3));

    // Test minimum and maximum range.
    filtered = dashboardService.filter(violations, new PolicyThreatLevelFilter(3, 3).asPolicyViolationPredicate());
    assertThat(filtered, contains(v3));

    // Test single policy threat category.
    filtered = dashboardService.filter(violations,
        new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE).asPolicyViolationPredicate());
    assertThat(filtered, contains(v1, v2));

    // Test multiple policy threat category.
    filtered = dashboardService
        .filter(
            violations,
            new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE,
                PolicyThreatCategory.SECURITY).asPolicyViolationPredicate());
    assertThat(filtered, contains(v1, v2, v5, v6));

    // Test multiple policy threat category and threat range.
    filtered = dashboardService.filter(violations, Predicates.and(new PolicyThreatCategoryFilter(
        PolicyThreatCategory.LICENSE, PolicyThreatCategory.SECURITY).asPolicyViolationPredicate(),
        new PolicyThreatLevelFilter(2, 5).asPolicyViolationPredicate()));
    assertThat(filtered, contains(v2, v5));
  }

  @Test
  public void testGetPolicyViolations_Limit() {
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null, null, false);
    assertThat(policyViolationDTOs, hasSize(3));

    policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null, 1, false);
    assertThat(policyViolationDTOs, hasSize(1));
  }

  @Test
  public void testGetPolicyViolationsByApplicationId_Limit() {
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app1.getPublicId(), app2.getPublicId()), null, null, null, null, false);
    assertThat(policyViolationDTOs, hasSize(3));

    policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app1.getPublicId(), app2.getPublicId()), null, null, null, 1, false);
    assertThat(policyViolationDTOs, hasSize(1));
  }

  @Test
  public void testGetPolicyViolationsByTags() {
    Tag app1Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app1.getId(), app1Tag.getId());

    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null,
        Sets.newHashSet(app1Tag.getId()), null, null, false);
    assertThat(policyViolationDTOs, hasSize(2));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);

    Tag app2Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    policyViolationDTOs = dashboardService.getPolicyViolations(null, Sets.newHashSet(app1Tag.getId(), app2Tag.getId()),
        null, null, false);
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
        null, null, false);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);

    policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null, null, false);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetComponentRisks_DedupViolationsForSameAppAndPolicyByPickingMostRecentViolationAcrossStages()
      throws Exception
  {
    PolicyEvaluation evaluation = tempEntity
        .newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "test scan app1 id");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, app1Policy,
        app1Policy.getThreatLevel() + 1, PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1");

    List<ComponentRiskDTO> riskDTOs = dashboardService.getComponentRisks(null, null, null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.hash, is(violation.getHash()));
    assertThat(
        riskDTO.gavs,
        containsInAnyOrder(new ComponentRiskDTO.GavDTO(violation.getGroupId(), violation.getArtifactId(), violation
            .getVersion())));
    assertThat(riskDTO.score, is(violation.getThreatLevel() + orgPolicy.getThreatLevel() * 2));
  }

  @Test
  public void testGetComponentRisks_FilterByApplication() throws Exception {
    List<ComponentRiskDTO> riskDTOs = dashboardService.getComponentRisks(Collections.singleton(app2.getPublicId()),
        null, null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.hash, is(app2PolicyViolation.getHash()));
    assertThat(riskDTO.gavs, containsInAnyOrder(new ComponentRiskDTO.GavDTO(app2PolicyViolation.getGroupId(),
        app2PolicyViolation.getArtifactId(), app2PolicyViolation.getVersion())));
    assertThat(riskDTO.score, is(orgPolicy.getThreatLevel()));
  }

  @Test
  public void testGetComponentRisks_FilterByStage() throws Exception {
    PolicyEvaluation evaluation = tempEntity
        .newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "test scan app1 id");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, app1Policy);

    List<ComponentRiskDTO> riskDTOs = dashboardService.getComponentRisks(null,
        Collections.singleton(ReleaseStageType.ID), null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.hash, is(violation.getHash()));
    assertThat(
        riskDTO.gavs,
        containsInAnyOrder(new ComponentRiskDTO.GavDTO(violation.getGroupId(), violation.getArtifactId(), violation
            .getVersion())));
    assertThat(riskDTO.score, is(app1Policy.getThreatLevel()));
  }

  @Test
  public void testGetComponentRisks_FilterByStage_ExcludesDevelop() throws Exception {
    PolicyEvaluation evaluation = tempEntity
        .newPolicyEvaluation(app1.getId(), DevelopStageType.ID, "test scan app1 id");
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
    assertThat(riskDTO.gavs, containsInAnyOrder(new ComponentRiskDTO.GavDTO(app2PolicyViolation.getGroupId(),
        app2PolicyViolation.getArtifactId(), app2PolicyViolation.getVersion())));
    assertThat(riskDTO.score, is(orgPolicy.getThreatLevel()));
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
    assertThat(
        riskDTO.gavs,
        containsInAnyOrder(new ComponentRiskDTO.GavDTO(violation.getGroupId(), violation.getArtifactId(), violation
            .getVersion())));
    assertThat(riskDTO.score, is(app1Policy.getThreatLevel()));
  }

  @Test
  public void testGetComponentRisks_FilterPolicyThreatLevel() throws Exception {
    List<ComponentRiskDTO> riskDTOs = dashboardService.getComponentRisks(null, null, null, null,
        new PolicyThreatLevelFilter(3, 3), 1000);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.hash, is(orgPolicyViolation.getHash()));
    assertThat(riskDTO.gavs, containsInAnyOrder(new ComponentRiskDTO.GavDTO(orgPolicyViolation.getGroupId(),
        orgPolicyViolation.getArtifactId(), orgPolicyViolation.getVersion())));
    assertThat(riskDTO.score, is(orgPolicy.getThreatLevel() * 2));
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

    List<ComponentRiskDTO> riskDTOs = dashboardService.getComponentRisks(Collections.singleton(app2.getPublicId()),
        null, null, null, null, 1000);
    assertThat(riskDTOs, hasSize(1));
    ComponentRiskDTO riskDTO = riskDTOs.get(0);
    assertThat(riskDTO.scoreCritical, is(27));
    assertThat(riskDTO.scoreSevere, is(22));
    assertThat(riskDTO.scoreModerate, is(5));
    assertThat(riskDTO.scoreLow, is(1));
    assertThat(riskDTO.score, is(55));
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
  }

  @Test
  public void testGetApplicationRisks_FilterByStage_ExcludesDevelop() throws Exception {
    PolicyEvaluation evaluation = tempEntity
        .newPolicyEvaluation(app1.getId(), DevelopStageType.ID, "test scan app1 id");
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
  public void testGetFilterSummary_NoFilter() throws Exception {
    FilterSummaryDTO summary = dashboardService.getFilterSummary(null, null, null, null, null);
    assertThat(summary.totalApplications, is(2));
    assertThat(summary.matchedApplications, is(2));
    assertThat(summary.totalPolicies, is(2));
    assertThat(summary.matchedPolicies, is(2));
  }

  @Test
  public void testGetFilterSummary_FilterByApp() throws Exception {
    FilterSummaryDTO summary = dashboardService.getFilterSummary(Collections.singleton(app2.getPublicId()), null, null,
        null, null);
    assertThat(summary.totalApplications, is(2));
    assertThat(summary.matchedApplications, is(1));
    assertThat(summary.totalPolicies, is(2));
    assertThat(summary.matchedPolicies, is(1));
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
  }

  @Test
  public void testGetFilterSummary_FilterByPolicyThreatLevel() throws Exception {
    FilterSummaryDTO summary = dashboardService.getFilterSummary(null, null, null, null, new PolicyThreatLevelFilter(
        orgPolicy.getThreatLevel(), orgPolicy.getThreatLevel()));
    assertThat(summary.totalApplications, is(2));
    assertThat(summary.matchedApplications, is(2));
    assertThat(summary.totalPolicies, is(2));
    assertThat(summary.matchedPolicies, is(1));
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
  }
}
