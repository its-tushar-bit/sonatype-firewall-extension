/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.NewestPolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.PolicyViolationDTOTestUtils.assertPolicyViolationDTO;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
    orgPolicy = tempEntity.newPolicy(org.getId(), "org owned policy");
    app1Policy = tempEntity.newPolicy(app1.getId(), "app owned policy");
    app1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id");
    app2PolicyEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "test scan app2 id");
    long start = System.currentTimeMillis();
    orgPolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation.getId(), orgPolicy);
    app1PolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation.getId(), app1Policy);
    app2PolicyViolation = tempEntity.newPolicyViolation(app2PolicyEvaluation.getId(), orgPolicy);
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
      dashboardService.getPolicyViolations(Sets.newHashSet(badStageTypeId), null, null, false);
      fail("Expected BadRequestException to be thrown.");
    }
    catch (BadRequestException e) {
      assertEquals(e.getMessage(), "Unknown stage type: " + badStageTypeId + ".");
    }
  }

  @Test
  public void testGetPolicyViolationsWithNullApplicationPublicIds() {
    Set<String> nullApplicationPublicId = null;
    try {
      dashboardService.getPolicyViolationsByApplicationIds(nullApplicationPublicId, null, null, null, false);
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
      dashboardService.getPolicyViolationsByApplicationIds(emptyApplicationPublicId, null, null, null, false);
      fail("Expected BadRequestException to be thrown.");
    }
    catch (BadRequestException e) {
      assertEquals(e.getMessage(), "Unable to get policy violations for null or empty application public IDs.");
    }
  }

  @Test
  public void testGetNewestPolicyViolations() {
    // Initial scan should have all the violations as 'newest'.
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null, true);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);

    // Force a violation to no longer be new.
    NewestPolicyViolationDAO newestPolicyViolationDAO = new NewestPolicyViolationDAO();
    newestPolicyViolationDAO.delete(newestPolicyViolationDAO.getById(orgPolicyViolation.getId()));
    policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null, true);

    assertThat(policyViolationDTOs, hasSize(2));
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);

    // Ensure that there are still 3 violations in the system.
    assertThat(dashboardService.getPolicyViolations(null, null, null, false), hasSize(3));
  }

  @Test
  public void testGetPolicyViolations() {
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null, false);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsWithMultipleStages() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation.getId(), app1Policy);

    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(
        Sets.newHashSet(BuildStageType.ID, ReleaseStageType.ID), null, null, false);
    assertThat(policyViolationDTOs, hasSize(4));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);
  }

  @Test
  public void testGetPolicyViolationsWithPolicyThreatCategories() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation.getId(), app1Policy);

    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(
        Sets.newHashSet(ReleaseStageType.ID),
        new PolicyThreatCategoryFilter(Lists.newArrayList(PolicyThreatCategory.OTHER)), null, false);

    assertThat(policyViolationDTOs, hasSize(0));

    policyViolationDTOs = dashboardService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID),
        new PolicyThreatCategoryFilter(
        Lists.newArrayList(PolicyThreatCategory.LICENSE)), null, false);

    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);
  }

  @Test
  public void testGetPolicyViolationsWithPolicyThreatLevel() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation.getId(), app1Policy);

    // Violation out of threat level range.
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(
        Sets.newHashSet(ReleaseStageType.ID),
        new PolicyThreatLevelFilter(6, 7), null, false);

    assertThat(policyViolationDTOs, hasSize(0));

    // Violation in range.
    policyViolationDTOs = dashboardService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID),
        new PolicyThreatLevelFilter(5, 7),
        null, false);

    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);
  }

  @Test
  public void testGetPolicyViolationsWithPolicyThreatLevelAndPolicyThreatCategories() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation.getId(), app1Policy);

    // Violation out of threat level range and wrong threat category.
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(Sets
        .newHashSet(ReleaseStageType.ID), Predicates
        .and(new PolicyThreatCategoryFilter(Lists.newArrayList(PolicyThreatCategory.OTHER)),
            new PolicyThreatLevelFilter(6, 7)), null, false);
    assertThat(policyViolationDTOs, hasSize(0));

    // Violation out of threat level range and correct threat category.
    policyViolationDTOs = dashboardService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), Predicates.and(
        new PolicyThreatCategoryFilter(Lists.newArrayList(PolicyThreatCategory.LICENSE)), new PolicyThreatLevelFilter(
            6, 7)), null, false);
    assertThat(policyViolationDTOs, hasSize(0));

    // Violation in range, but wrong threat category.
    policyViolationDTOs = dashboardService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), Predicates.and(
        new PolicyThreatCategoryFilter(Lists.newArrayList(PolicyThreatCategory.OTHER)), new PolicyThreatLevelFilter(5,
            7)), null, false);
    assertThat(policyViolationDTOs, hasSize(0));

    // Violation in range and in the correct category.
    policyViolationDTOs = dashboardService.getPolicyViolations(Sets.newHashSet(ReleaseStageType.ID), Predicates.and(
        new PolicyThreatCategoryFilter(Lists.newArrayList(PolicyThreatCategory.LICENSE)), new PolicyThreatLevelFilter(
            5, 7)), null, false);

    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, violation, app1, app1Policy);
  }

  @Test
  public void testGetPolicyViolationsReturnsLatest() {
    // Create a new evaluation for app1 that does not include any violations.
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "re-scan app1");
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null, false);

    // The violations count should be 1, all of which come from app2.
    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationIds() {
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app1.getPublicId(), app2.getPublicId()), null, null, null, false);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationIdsWithMultipleStages() {
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID,
        "re-scan app1");
    PolicyViolation violation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation.getId(), app1Policy);

    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app1.getPublicId(), app2.getPublicId()),
        Sets.newHashSet(BuildStageType.ID, ReleaseStageType.ID), null, null, false);
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
        null, null, null, true);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);

    // Force a violation to no longer be new.
    NewestPolicyViolationDAO newestPolicyViolationDAO = new NewestPolicyViolationDAO();
    newestPolicyViolationDAO.delete(newestPolicyViolationDAO.getById(orgPolicyViolation.getId()));
    policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(applicationIds, null, null, null, true);

    assertThat(policyViolationDTOs, hasSize(2));
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);

    // Ensure that there are still 3 violations in the system.
    assertThat(dashboardService.getPolicyViolationsByApplicationIds(applicationIds, null, null, null, false),
        hasSize(3));
  }

  @Test
  public void testGetPolicyViolationsByApplicationIdsReturnsLatest() {
    // Create a new evaluation for app1 that does not include any violations.
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "re-scan app1");
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app1.getPublicId(), app2.getPublicId()), null, null, null, false);

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
    filtered = dashboardService.filter(violations, new PolicyThreatLevelFilter(3, null));
    assertThat(filtered, contains(v3, v4, v5, v6));

    // Test maximum range.
    filtered = dashboardService.filter(violations, new PolicyThreatLevelFilter(null, 3));
    assertThat(filtered, contains(v1, v2, v3));

    // Test minimum and maximum range.
    filtered = dashboardService.filter(violations, new PolicyThreatLevelFilter(3, 3));
    assertThat(filtered, contains(v3));

    // Test single policy threat category.
    filtered = dashboardService.filter(violations,
        new PolicyThreatCategoryFilter(Lists.newArrayList(PolicyThreatCategory.LICENSE)));
    assertThat(filtered, contains(v1, v2));

    // Test multiple policy threat category.
    filtered = dashboardService
        .filter(
            violations,
            new PolicyThreatCategoryFilter(Lists.newArrayList(PolicyThreatCategory.LICENSE,
                PolicyThreatCategory.SECURITY)));
    assertThat(filtered, contains(v1, v2, v5, v6));

    // Test multiple policy threat category and threat range.
    filtered = dashboardService.filter(violations,
        Predicates.and(
            new PolicyThreatCategoryFilter(Lists.newArrayList(PolicyThreatCategory.LICENSE,
                PolicyThreatCategory.SECURITY)), new PolicyThreatLevelFilter(2, 5)));
    assertThat(filtered, contains(v2, v5));
  }

  @Test
  public void testGetPolicyViolations_Limit() {
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null, null, null, false);
    assertThat(policyViolationDTOs, hasSize(3));

    policyViolationDTOs = dashboardService.getPolicyViolations(null, null, 1, false);
    assertThat(policyViolationDTOs, hasSize(1));
  }

  @Test
  public void testGetPolicyViolationsByApplicationId_Limit() {
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app1.getPublicId(), app2.getPublicId()), null, null, null, false);
    assertThat(policyViolationDTOs, hasSize(3));

    policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(
        Sets.newHashSet(app1.getPublicId(), app2.getPublicId()), null, null, 1, false);
    assertThat(policyViolationDTOs, hasSize(1));
  }

  @Test
  public void testPolicyViolationDTOSort() {
    PolicyViolationDTO dto9AA = buildPolicyViolationDTO(9, "A", "A", null, null, null);
    PolicyViolationDTO dto8AA = buildPolicyViolationDTO(8, "A", "A", null, null, null);
    PolicyViolationDTO dto8BA = buildPolicyViolationDTO(8, "B", "A", null, null, null);
    PolicyViolationDTO dto8BB = buildPolicyViolationDTO(8, "B", "B", null, null, null);
    PolicyViolationDTO dto7AAAAA = buildPolicyViolationDTO(7, "A", "A", "A", "A", "A");
    PolicyViolationDTO dto7AAAAB = buildPolicyViolationDTO(7, "A", "A", "A", "A", "B");
    PolicyViolationDTO dto7AAA_NULL_A = buildPolicyViolationDTO(7, "A", "A", "A", null, "A");
    PolicyViolationDTO dto7AAA_NULL_B = buildPolicyViolationDTO(7, "A", "A", "A", null, "B");
    PolicyViolationDTO dto7BAAAB = buildPolicyViolationDTO(7, "B", "A", "A", "A", "B");
    PolicyViolationDTO dto7BAACA = buildPolicyViolationDTO(7, "B", "A", "A", "C", "A");

    List<PolicyViolationDTO> unsorted = Lists.newArrayList(dto7AAA_NULL_A, dto7BAACA, dto8BA, dto7BAAAB, dto9AA,
        dto7AAAAB, dto7AAA_NULL_B, dto8AA, dto8BB, dto7AAAAA);
    List<PolicyViolationDTO> sorted = dashboardService.sort(unsorted);
    List<PolicyViolationDTO> expected = Lists.newArrayList(dto9AA, dto8AA, dto8BA, dto8BB, dto7AAAAA, dto7AAAAB,
        dto7AAA_NULL_A, dto7AAA_NULL_B, dto7BAAAB, dto7BAACA);

    assertThat(sorted, is(expected));
  }

  private PolicyViolationDTO buildPolicyViolationDTO(int threatLevel, String policyName, String applicationName,
      String groupId, String artifactId, String version)
  {
    PolicyViolationDTO dto = new PolicyViolationDTO();
    dto.id = UUID.randomUUID().toString();
    dto.policyName = policyName;
    dto.applicationName = applicationName;
    dto.threatLevel = threatLevel;
    dto.groupId = groupId;
    dto.artifactId = artifactId;
    dto.version = version;
    return dto;
  }
}
