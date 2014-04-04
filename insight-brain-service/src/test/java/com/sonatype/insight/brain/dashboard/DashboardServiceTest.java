/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.PolicyViolationDTOTestUtils.assertPolicyViolationDTO;
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
    while (System.currentTimeMillis() <= start) {
      // just spinning until next policy eval time is guaranteed to be greater than time for the evals created above
    }
  }

  @Test
  public void testGetPolicyViolationsWithBadStageTypeId() {
    String badStageTypeId = "not a real stage type id";
    try {
      dashboardService.getPolicyViolations(badStageTypeId);
      fail("Expected BadRequestException to be thrown.");
    }
    catch (BadRequestException e) {
      assertEquals(e.getMessage(), "Unknown stage type: " + badStageTypeId + ".");
    }
  }

  @Test
  public void testGetPolicyViolationsWithNullApplicationPublicIds() {
    List<String> nullApplicationPublicId = null;
    try {
      dashboardService.getPolicyViolationsByApplicationIds(nullApplicationPublicId, null);
      fail("Expected BadRequestException to be thrown.");
    }
    catch (BadRequestException e) {
      assertEquals(e.getMessage(), "Unable to get policy violations for null or empty application public IDs.");
    }
  }

  @Test
  public void testGetPolicyViolationsWithEmptyApplicationPublicIds() {
    List<String> emptyApplicationPublicId = new ArrayList<>();
    try {
      dashboardService.getPolicyViolationsByApplicationIds(emptyApplicationPublicId, null);
      fail("Expected BadRequestException to be thrown.");
    }
    catch (BadRequestException e) {
      assertEquals(e.getMessage(), "Unable to get policy violations for null or empty application public IDs.");
    }
  }

  @Test
  public void testGetPolicyViolations() {
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsReturnsLatest() {
    // Create a new evaluation for app1 that does not include any violations.
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "re-scan app1");
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolations(null);

    // The violations count should be 1, all of which come from app2.
    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationId() {
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationId(app1.getPublicId(), null);
    assertThat(policyViolationDTOs, hasSize(2));

    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);

    policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationId(app2.getPublicId(), null);
    assertThat(policyViolationDTOs, hasSize(1));

    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationIdReturnsLatest() {
    // Re-scan app1 and give it one violation.
    PolicyEvaluation newApp1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID,
        "re-scan app1");
    PolicyViolation sameOldApp1PolicyViolation = tempEntity.newPolicyViolation(newApp1PolicyEvaluation.getId(),
        app1Policy);
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationId(
        app1.getPublicId(), null);

    // Now only returns the 1 application violation.
    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, sameOldApp1PolicyViolation, app1, app1Policy);

    policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationId(app2.getPublicId(), null);
    assertThat(policyViolationDTOs, hasSize(1));

    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationIds() {
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(
        Lists.newArrayList(app1.getPublicId(), app2.getPublicId()), null);
    assertThat(policyViolationDTOs, hasSize(3));
    assertPolicyViolationDTO(policyViolationDTOs, orgPolicyViolation, app1, orgPolicy);
    assertPolicyViolationDTO(policyViolationDTOs, app1PolicyViolation, app1, app1Policy);
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationIdsReturnsLatest() {
    // Create a new evaluation for app1 that does not include any violations.
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "re-scan app1");
    List<PolicyViolationDTO> policyViolationDTOs = dashboardService.getPolicyViolationsByApplicationIds(
        Lists.newArrayList(app1.getPublicId(), app2.getPublicId()), null);

    // The violations count should be 1, all of which come from app2.
    assertThat(policyViolationDTOs, hasSize(1));
    assertPolicyViolationDTO(policyViolationDTOs, app2PolicyViolation, app2, orgPolicy);
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
    List<PolicyViolationDTO> sorted = DashboardService.sort(unsorted);
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
