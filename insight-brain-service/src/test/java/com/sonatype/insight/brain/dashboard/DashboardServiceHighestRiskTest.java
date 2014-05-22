/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.StageTypeService;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class DashboardServiceHighestRiskTest
{

  private DashboardService dashboardService;

  @Mock
  private ApplicationService applicationService;

  @Mock
  private ApplicationDAO applicationDAO;

  @Mock
  private PolicyEvaluationDAO policyEvaluationDAO;

  //no need to mock if pure
  private PolicyViolationAdapter policyViolationAdapter = new PolicyViolationAdapter();

  @Mock
  private PolicyDAO policyDAO;

  @Mock
  private PolicyViolationDAO policyViolationDAO;

  @Mock
  private StageTypeService stageTypeService;

  @Mock
  private DashboardFilterDAO dashboardFilterDAO;


  @Before
  public void init() {

    dashboardService = new DashboardService(applicationDAO, applicationService, policyDAO,
        policyEvaluationDAO, policyViolationAdapter, policyViolationDAO, stageTypeService, dashboardFilterDAO);
  }

  private StageType buildStage = StageTypes.getById(Stage.ID_BUILD);

  private StageType releaseStage = StageTypes.getById(Stage.ID_RELEASE);

  private String appId1 = "wagarbl";

  private String appPublicId1 = "pubbobl";

  private String appName = "myApp";

  private String orgId = "org123";

  private Application application1 = new Application(appPublicId1, appName, orgId);

  {
    application1.setId(appId1);
  }

  private String scanId = "scan1";

  private String policyEvalId1 = "polEval1";

  private PolicyEvaluation policyEvaluation1 = new PolicyEvaluation(appId1, buildStage.getId(), scanId);

  {
    policyEvaluation1.setId(policyEvalId1);
    policyEvaluation1.setTime(new Date(1000L));
  }

  private String policyName = "firstPolicy";

  private String groupId1 = "group1";

  private String artifactId1 = "artifact1";

  private String versionId1 = "1.0";

  private String vioHash1 = "bargobl";

  private PolicyViolation vio1 = new PolicyViolation(policyEvaluation1, "policyW", policyName, 5,
      PolicyThreatCategory.LICENSE, vioHash1, groupId1, artifactId1, versionId1, "[]", "");


  private String appId2 = "wagarbl2";

  private String appPublicId2 = "pubbobl2";

  private String appName2 = "myApp2";

  private String orgId2 = "org1234";

  private Application application2 = new Application(appPublicId2, appName2, orgId2);

  {
    application2.setId(appId2);
  }

  private String policyEvalId4 = "polEval4";

  private String policyName4 = "fourthPolicy";

  private PolicyEvaluation policyEvaluation4 = new PolicyEvaluation(appId2, buildStage.getId(), scanId);

  {
    policyEvaluation4.setId(policyEvalId4);
    policyEvaluation4.setTime(new Date(4000L));
  }

  private PolicyViolation vio4 = new PolicyViolation(policyEvaluation4, "policyFour", policyName4, 3,
      PolicyThreatCategory.LICENSE, vioHash1, groupId1, artifactId1, versionId1, "[]", "");


  private void assertRisk(RiskDTO risk, int criticalRisk, int severeRisk, int moderateRisk,
      int lowRisk, int netRisk)
  {
    assertNotNull("Stage risk is set", risk);
    assertEquals(criticalRisk, risk.criticalRisk);
    assertEquals(severeRisk, risk.severeRisk);
    assertEquals(moderateRisk, risk.moderateRisk);
    assertEquals(lowRisk, risk.lowRisk);
    assertEquals(netRisk, risk.totalRisk);
  }

  private List<ApplicationRiskScoreDTO> doTest(List<StageType> stages, List<Application> returnApps,
      List<PolicyEvaluation> policyEvals, List<PolicyViolation> policyViolations, int limit)
  {

    Set<String> stageIds = Sets.newHashSet();
    for (StageType stage : stages) {
      stageIds.add(stage.getId());
    }

    Set<String> returnAppIds = Sets.newHashSet();
    for (Application app : returnApps) {
      returnAppIds.add(app.getId());
    }

    Set<String> policyEvaluationIds = Sets.newHashSet();
    for (PolicyEvaluation eval : policyEvals) {
      policyEvaluationIds.add(eval.getId());
    }

    when(stageTypeService.getLicensedStageTypes()).thenReturn(stages);
    when(applicationService.getApplicationsByPublicIdsAndTagIds(returnAppIds, null)).thenReturn(returnApps);
    when(policyEvaluationDAO.getLastByApplicationIdsAndStageIds(returnAppIds, stageIds)).thenReturn(policyEvals);
    when(policyViolationDAO.getByEvaluationIds(policyEvaluationIds)).thenReturn(policyViolations);

    return dashboardService
        .getApplicationRisks(returnAppIds, stageIds, null, null, null, limit);
  }

  @Test
  public void testGetAllApplicationRisksSimple() {
    List<ApplicationRiskScoreDTO> result = doTest(Lists.newArrayList(buildStage), Lists.newArrayList(application1),
        Lists.newArrayList(policyEvaluation1), Lists.newArrayList(vio1), Integer.MAX_VALUE);

    assertThat(result, hasSize(1));
    assertRisk(result.get(0).totalApplicationRisk, 0, 5, 0, 0, 5);
    assertEquals("Risk name was set right", appName, result.get(0).applicationName);
    assertEquals("Risk public appID was set right", appPublicId1, result.get(0).applicationId);
    assertThat(result.get(0).stageRisks, hasSize(1));

    StageRiskScoreDTO buildStageRisk = result.get(0).getStageRiskScore(buildStage.getId());
    assertRisk(buildStageRisk.risk, 0, 5, 0, 0, 5);
    assertThat(buildStageRisk.scanId, is(policyEvaluation1.getScanId()));
    assertThat(buildStageRisk.stageTypeName, is(buildStage.getName()));
  }


  @Test
  public void testGetAllApplicationRisksTwoStages() {
    String policyEvalId2 = "polEval2";
    String policyName2 = "secondPolicy";
    PolicyEvaluation policyEvaluation2 = new PolicyEvaluation(appId1, releaseStage.getId(), scanId);
    policyEvaluation2.setId(policyEvalId2);
    policyEvaluation2.setTime(new Date(5000L));
    PolicyViolation vio2 = new PolicyViolation(policyEvaluation2, "policyHam", policyName2, 7,
        PolicyThreatCategory.LICENSE, vioHash1, groupId1, artifactId1, versionId1, "[]", "");

    List<ApplicationRiskScoreDTO> result = doTest(Lists.newArrayList(buildStage, releaseStage),
        Lists.newArrayList(application1),
        Lists.newArrayList(policyEvaluation1, policyEvaluation2), Lists.newArrayList(vio1, vio2), Integer.MAX_VALUE);

    assertThat(result, hasSize(1));
    assertRisk(result.get(0).totalApplicationRisk, 0, 12, 0, 0, 12);
    assertEquals("Risk name was set right", appName, result.get(0).applicationName);
    assertEquals("Risk public appID was set right", appPublicId1, result.get(0).applicationId);
    assertThat(result.get(0).stageRisks, hasSize(2));

    StageRiskScoreDTO buildStageRisk = result.get(0).getStageRiskScore(buildStage.getId());
    assertRisk(buildStageRisk.risk, 0, 5, 0, 0, 5);
    assertThat(buildStageRisk.scanId, is(policyEvaluation1.getScanId()));

    StageRiskScoreDTO releaseStageRisk = result.get(0).getStageRiskScore(releaseStage.getId());
    assertRisk(releaseStageRisk.risk, 0, 7, 0, 0, 7);
    assertThat(releaseStageRisk.scanId, is(policyEvaluation2.getScanId()));
  }

  @Test
  public void testGetAllApplicationRisksTwoStagesAppOneOneStageAppTwo() {
    String policyEvalId2 = "polEval2";
    String policyName2 = "secondPolicy";
    PolicyEvaluation policyEvaluation2 = new PolicyEvaluation(appId1, releaseStage.getId(), scanId);
    policyEvaluation2.setId(policyEvalId2);
    policyEvaluation2.setTime(new Date(5000L));
    PolicyViolation vio2 = new PolicyViolation(policyEvaluation2, "policyHam", policyName2, 7,
        PolicyThreatCategory.LICENSE, vioHash1, groupId1, artifactId1, versionId1, "[]", "");

    List<ApplicationRiskScoreDTO> result = doTest(Lists.newArrayList(buildStage, releaseStage),
        Lists.newArrayList(application1, application2),
        Lists.newArrayList(policyEvaluation1, policyEvaluation2, policyEvaluation4),
        Lists.newArrayList(vio1, vio2, vio4), Integer.MAX_VALUE);

    assertThat(result, hasSize(2));
    assertRisk(result.get(0).totalApplicationRisk, 0, 12, 0, 0, 12);
    assertEquals("Risk name was set right", appName, result.get(0).applicationName);
    assertEquals("Risk public appID was set right", appPublicId1, result.get(0).applicationId);
    assertThat(result.get(0).stageRisks, hasSize(2));

    //application one
    StageRiskScoreDTO buildStageRisk = result.get(0).getStageRiskScore(buildStage.getId());
    assertRisk(buildStageRisk.risk, 0, 5, 0, 0, 5);
    assertThat(buildStageRisk.scanId, is(policyEvaluation1.getScanId()));

    StageRiskScoreDTO releaseStageRisk = result.get(0).getStageRiskScore(releaseStage.getId());
    assertRisk(releaseStageRisk.risk, 0, 7, 0, 0, 7);
    assertThat(releaseStageRisk.scanId, is(policyEvaluation2.getScanId()));

    //application two
    assertRisk(result.get(1).totalApplicationRisk, 0, 0, 3, 0, 3);
    assertEquals("Risk name was set right", appName2, result.get(1).applicationName);
    assertEquals("Risk public appID was set right", appPublicId2, result.get(1).applicationId);
    assertThat(result.get(1).stageRisks, hasSize(1));

    StageRiskScoreDTO appTwoBuildStageRisk = result.get(1).getStageRiskScore(buildStage.getId());
    assertRisk(appTwoBuildStageRisk.risk, 0, 0, 3, 0, 3);
    assertThat(appTwoBuildStageRisk.scanId, is(policyEvaluation4.getScanId()));
  }


  @Test
  public void testGetAllApplicationRisksSortedByRiskThenAppId() {
    String appId3 = "zagarbl2";
    String appPublicId3 = "pubbobl3";
    String appName3 = "myApp3";
    String orgId3 = "org12345";
    Application application3 = new Application(appPublicId3, appName3, orgId3);
    application3.setId(appId3);

    String policyEvalId3 = "polEval3";
    String policyName3 = "thirdPolicy";
    PolicyEvaluation policyEvaluation3 = new PolicyEvaluation(appId3, releaseStage.getId(), scanId);
    policyEvaluation3.setId(policyEvalId3);
    policyEvaluation3.setTime(new Date(5000L));
    PolicyViolation vio3 = new PolicyViolation(policyEvaluation3, "policyCheese", policyName3, 3,
        PolicyThreatCategory.LICENSE, vioHash1, groupId1, artifactId1, versionId1, "[]", "");

    List<ApplicationRiskScoreDTO> result = doTest(Lists.newArrayList(buildStage, releaseStage),
        Lists.newArrayList(application1, application2, application3),
        Lists.newArrayList(policyEvaluation1, policyEvaluation3, policyEvaluation4),
        Lists.newArrayList(vio1, vio3, vio4), Integer.MAX_VALUE);

    assertThat(result, hasSize(3));

    assertEquals("App1 name was set", appName, result.get(0).applicationName);
    assertEquals("App1 appId1 was set", appPublicId1, result.get(0).applicationId);
    assertEquals("App1 was correct", 5, result.get(0).totalApplicationRisk.totalRisk);

    assertEquals("App2 name was set", appName2, result.get(1).applicationName);
    assertEquals("App2 appId1 was set", appPublicId2, result.get(1).applicationId);
    assertEquals("App2 was correct", 3, result.get(1).totalApplicationRisk.totalRisk);

    assertEquals("App3 name was set", appName3, result.get(2).applicationName);
    assertEquals("App3 appId1 was set", appPublicId3, result.get(2).applicationId);
    assertEquals("App3 was correct", 3, result.get(2).totalApplicationRisk.totalRisk);
  }

  @Test
  public void testGetAllApplicationsLimitResults() {
    List<ApplicationRiskScoreDTO> result = doTest(Lists.newArrayList(buildStage),
        Lists.newArrayList(application1, application2),
        Lists.newArrayList(policyEvaluation1, policyEvaluation4), Lists.newArrayList(vio1, vio4), 1);


    assertThat(result, hasSize(1));

    assertEquals("App1 name was set", appName, result.get(0).applicationName);
    assertEquals("App1 appId1 was set", appPublicId1, result.get(0).applicationId);
    assertEquals("App1 was correct", 5, result.get(0).totalApplicationRisk.totalRisk);
  }

  @Test
  public void testGetAllApplicationNetRiskIsSumOfLevels() {
    String policyEvalId5 = "polEval5";
    String policyName5 = "fifthPolicy";
    PolicyEvaluation policyEvaluation5 = new PolicyEvaluation(appId1, buildStage.getId(), scanId);
    policyEvaluation5.setId(policyEvalId5);
    policyEvaluation5.setTime(new Date(4000L));
    PolicyViolation vio5 = new PolicyViolation(policyEvaluation5, "policyBacon", policyName5, 3,
        PolicyThreatCategory.LICENSE, vioHash1, groupId1, artifactId1, versionId1, "[]", "");
    String groupId2 = "group2";
    String artifactId2 = "artifact2";
    String versionId2 = "1.2";
    String vioHash2 = "bargoblyh";
    PolicyViolation vio51 = new PolicyViolation(policyEvaluation5, "policyw", policyName5, 9,
        PolicyThreatCategory.LICENSE, vioHash2, groupId2, artifactId2, versionId2, "[]", "");


    List<ApplicationRiskScoreDTO> result = doTest(Lists.newArrayList(buildStage), Lists.newArrayList(application1),
        Lists.newArrayList(policyEvaluation5), Lists.newArrayList(vio5, vio51), Integer.MAX_VALUE);

    assertThat(result, hasSize(1));
    assertRisk(result.get(0).totalApplicationRisk, 9, 0, 3, 0, 12);
    assertEquals("Risk name was set right", appName, result.get(0).applicationName);
    assertEquals("Risk public appID was set right", appPublicId1, result.get(0).applicationId);
    assertThat(result.get(0).stageRisks, hasSize(1));

    StageRiskScoreDTO buildStageRisk = result.get(0).getStageRiskScore(buildStage.getId());
    assertRisk(buildStageRisk.risk, 9, 0, 3, 0, 12);

  }

  @Test
  public void testGetAllApplicationTotalApplicationRiskDeDupesAcrossStages() {
    String sharedPolicyId = "sharedPolicyId";
    String policyEvalId7 = "policyEvalId";
    String policyName7 = "seventhPolicy";
    PolicyEvaluation policyEvaluation7 = new PolicyEvaluation(appId1, buildStage.getId(), scanId);
    policyEvaluation7.setId(policyEvalId7);
    policyEvaluation7.setTime(new Date(4000L));
    PolicyViolation vio7 = new PolicyViolation(policyEvaluation7, sharedPolicyId, policyName7, 5,
        PolicyThreatCategory.LICENSE, vioHash1, groupId1, artifactId1, versionId1, "[]", "");

    String policyEvalId8 = "polEval8";
    String policyName8 = "eightPolicy";
    PolicyEvaluation policyEvaluation8 = new PolicyEvaluation(appId1, releaseStage.getId(), scanId);
    policyEvaluation8.setId(policyEvalId8);
    policyEvaluation8.setTime(new Date(4000L));
    PolicyViolation vio8 = new PolicyViolation(policyEvaluation8, sharedPolicyId, policyName8, 5,
        PolicyThreatCategory.LICENSE, vioHash1, groupId1, artifactId1, versionId1, "[]", "");


    List<ApplicationRiskScoreDTO> result = doTest(Lists.newArrayList(buildStage, releaseStage),
        Lists.newArrayList(application1),
        Lists.newArrayList(policyEvaluation7, policyEvaluation8), Lists.newArrayList(vio7, vio8), Integer.MAX_VALUE);

    assertThat(result, hasSize(1));
    assertRisk(result.get(0).totalApplicationRisk, 0, 5, 0, 0, 5);
    assertEquals("Risk name was set right", appName, result.get(0).applicationName);
    assertEquals("Risk public appID was set right", appPublicId1, result.get(0).applicationId);
    assertThat(result.get(0).stageRisks, hasSize(2));

    StageRiskScoreDTO buildStageRisk = result.get(0).getStageRiskScore(buildStage.getId());
    assertRisk(buildStageRisk.risk, 0, 5, 0, 0, 5);

    StageRiskScoreDTO releaseStageRisk = result.get(0).getStageRiskScore(releaseStage.getId());
    assertRisk(releaseStageRisk.risk, 0, 5, 0, 0, 5);
  }

  @Test
  public void testGetAllApplicationExcludesRisksOfZero() {
    String policyEvalId5 = "polEval5";
    String policyName5 = "fifthPolicy";
    PolicyEvaluation policyEvaluation5 = new PolicyEvaluation(appId1, buildStage.getId(), scanId);
    policyEvaluation5.setId(policyEvalId5);
    policyEvaluation5.setTime(new Date(4000L));
    PolicyViolation vio5 = new PolicyViolation(policyEvaluation5, "policy5", policyName5, 0,
        PolicyThreatCategory.LICENSE, vioHash1, groupId1, artifactId1, versionId1, "[]", "");

    List<ApplicationRiskScoreDTO> result = doTest(Lists.newArrayList(buildStage), Lists.newArrayList(application1),
        Lists.newArrayList(policyEvaluation5), Lists.newArrayList(vio5), Integer.MAX_VALUE);

    assertThat(result, hasSize(0));
  }


}
