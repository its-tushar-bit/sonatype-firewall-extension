/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
  private PolicyViolationDAO policyViolationDAO;

  @Mock
  private StageTypeService stageTypeService;

  @Mock
  private DashboardFilterDAO dashboardFilterDAO;


  @Before
  public void init() {

    dashboardService = new DashboardService(applicationDAO, applicationService,
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

  private PolicyViolation vio1 = new PolicyViolation(policyEvaluation1, policyEvalId1, policyName, 5,
      PolicyThreatCategory.LICENSE, vioHash1, groupId1, artifactId1, versionId1, "[]", "");

  private List<PolicyViolation> violations1 = Lists.newArrayList(vio1);


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

  private PolicyViolation vio4 = new PolicyViolation(policyEvaluation4, policyEvalId4, policyName4, 3,
      PolicyThreatCategory.LICENSE, vioHash1, groupId1, artifactId1, versionId1, "[]", "");

  private List<PolicyViolation> violations4 = Lists.newArrayList(vio4);

  private List<StageType> stages = Lists.newArrayList(buildStage);

  private Set<String> stageIds = Sets.newHashSet(buildStage.getId());

  private void mockGetByPublicId(final List<StageType> stages, String inputPublicAppId, Application returnApp) {
    when(stageTypeService.getLicensedStageTypes()).thenReturn(stages);
    when(applicationService.getApplicationByPublicIdNotNull(inputPublicAppId)).thenReturn(returnApp);
    when(applicationDAO.getByPublicIdNotNull(inputPublicAppId)).thenReturn(returnApp);
  }

  private void mockPolicyEval(final String givenAppId, final String stageId, final PolicyEvaluation policyEval,
      final List<PolicyViolation> resultViolation)
  {
    when(policyEvaluationDAO.getLastByApplicationIdAndStageId(givenAppId, stageId)).thenReturn(policyEval);
    when(policyViolationDAO.getByEvaluationId(policyEval.getId())).thenReturn(resultViolation);
  }

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

  @Test
  public void testGetAllApplicationRisksSimple() {
    mockGetByPublicId(Lists.newArrayList(buildStage), appPublicId1, application1);
    mockPolicyEval(appId1, buildStage.getId(), policyEvaluation1, violations1);

    List<ApplicationRiskScoreDTO> result = dashboardService
        .getApplicationRisks(Collections.singleton(appPublicId1), Collections.singleton(buildStage.getId()),
            Collections.<String>emptySet(), null, null, Integer.MAX_VALUE);

    assertEquals("One result expected", 1, result.size());
    assertRisk(result.get(0).totalApplicationRisk, 0, 5, 0, 0, 5);
    assertEquals("Risk name was set right", appName, result.get(0).applicationName);
    assertEquals("Risk public appID was set right", appPublicId1, result.get(0).applicationId);
    assertEquals("One stage risk in map", 1, result.get(0).stageRisks.size());

    StageRiskScoreDTO buildStageRisk = result.get(0).getStageRiskScore(buildStage.getId());
    assertRisk(buildStageRisk.risk, 0, 5, 0, 0, 5);
  }


  private void doTwoStageTestWithStageIds(final Set<String> inputStageIds, int maxResults) {
    String policyEvalId2 = "polEval2";
    String policyName2 = "secondPolicy";
    PolicyEvaluation policyEvaluation2 = new PolicyEvaluation(appId1, releaseStage.getId(), scanId);
    policyEvaluation2.setId(policyEvalId2);
    policyEvaluation2.setTime(new Date(5000L));
    PolicyViolation vio2 = new PolicyViolation(policyEvaluation2, policyEvalId2, policyName2, 7,
        PolicyThreatCategory.LICENSE, vioHash1, groupId1, artifactId1, versionId1, "[]", "");
    List<PolicyViolation> violations2 = Lists.newArrayList(vio2);

    List<StageType> licensedStages = Lists.newArrayList(buildStage, releaseStage);

    mockGetByPublicId(licensedStages, appPublicId1, application1);
    mockPolicyEval(appId1, buildStage.getId(), policyEvaluation1, violations1);
    mockPolicyEval(appId1, releaseStage.getId(), policyEvaluation2, violations2);

    List<ApplicationRiskScoreDTO> result = dashboardService.getApplicationRisks(Collections.singleton(appPublicId1),
        inputStageIds, Collections.<String>emptySet(), null, null, maxResults);

    assertEquals("One result expected", 1, result.size());
    assertRisk(result.get(0).totalApplicationRisk, 0, 12, 0, 0, 12);
    assertEquals("Risk name was set right", appName, result.get(0).applicationName);
    assertEquals("Risk public appID was set right", appPublicId1, result.get(0).applicationId);
    assertEquals("Two stage risk in map", 2, result.get(0).stageRisks.size());

    StageRiskScoreDTO buildStageRisk = result.get(0).getStageRiskScore(buildStage.getId());
    assertRisk(buildStageRisk.risk, 0, 5, 0, 0, 5);

    StageRiskScoreDTO releaseStageRisk = result.get(0).getStageRiskScore(releaseStage.getId());
    assertRisk(releaseStageRisk.risk, 0, 7, 0, 0, 7);
  }

  @Test
  public void testGetAllApplicationRisksTwoStages() {
    doTwoStageTestWithStageIds(Sets.newHashSet(buildStage.getId(), releaseStage.getId()), 2);
  }

  @Test
  public void testGetAllApplicationRIsksNoStagedPickedGetsAllLicensedStages() {
    doTwoStageTestWithStageIds(new HashSet<String>(), 2);
  }


  @Test
  public void testGetAllApplicationRisksSortedByRiskThenAppId() {

    String appId3 = "zagarbl2";
    String appPublicId3 = "pubbobl3";
    String appName3 = "myApp3";
    String orgId3 = "org12345";
    Application application3 = new Application(appPublicId3, appName3, orgId3);
    application3.setId(appId3);

    mockGetByPublicId(stages, appPublicId1, application1);
    mockGetByPublicId(stages, appPublicId2, application2);
    mockGetByPublicId(stages, appPublicId3, application3);
    mockPolicyEval(appId1, buildStage.getId(), policyEvaluation1, violations1);
    mockPolicyEval(appId2, buildStage.getId(), policyEvaluation4, violations4);
    mockPolicyEval(appId3, buildStage.getId(), policyEvaluation4, violations4);

    List<ApplicationRiskScoreDTO> result = dashboardService.getApplicationRisks(
        Sets.newHashSet(appPublicId1, appPublicId2, appPublicId3),
        stageIds, Collections.<String>emptySet(), null, null, Integer.MAX_VALUE);

    assertEquals("Three results expected", 3, result.size());

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

    mockGetByPublicId(stages, appPublicId1, application1);
    mockGetByPublicId(stages, appPublicId2, application2);
    mockPolicyEval(appId1, buildStage.getId(), policyEvaluation1, violations1);
    mockPolicyEval(appId2, buildStage.getId(), policyEvaluation4, violations4);

    List<ApplicationRiskScoreDTO> result = dashboardService.getApplicationRisks(
        Sets.newHashSet(appPublicId1, appPublicId2),
        stageIds, Collections.<String>emptySet(), null, null, 1);

    assertEquals("One result expected", 1, result.size());

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
    PolicyViolation vio5 = new PolicyViolation(policyEvaluation5, policyEvalId5, policyName5, 3,
        PolicyThreatCategory.LICENSE, vioHash1, groupId1, artifactId1, versionId1, "[]", "");
    String groupId2 = "group2";
    String artifactId2 = "artifact2";
    String versionId2 = "1.2";
    String vioHash2 = "bargoblyh";
    PolicyViolation vio51 = new PolicyViolation(policyEvaluation5, policyEvalId5, policyName5, 9,
        PolicyThreatCategory.LICENSE, vioHash2, groupId2, artifactId2, versionId2, "[]", "");
    List<PolicyViolation> violations5 = Lists.newArrayList(vio5, vio51);

    List<StageType> stages = Lists.newArrayList(buildStage);
    Set<String> stageIds = Sets.newHashSet(buildStage.getId());

    mockGetByPublicId(stages, appPublicId1, application1);
    mockPolicyEval(appId1, buildStage.getId(), policyEvaluation5, violations5);

    List<ApplicationRiskScoreDTO> result = dashboardService.getApplicationRisks(Collections.singleton(appPublicId1),
        stageIds, Collections.<String>emptySet(), null, null, Integer.MAX_VALUE);

    assertEquals("One result expected", 1, result.size());
    assertRisk(result.get(0).totalApplicationRisk, 9, 0, 3, 0, 12);
    assertEquals("Risk name was set right", appName, result.get(0).applicationName);
    assertEquals("Risk public appID was set right", appPublicId1, result.get(0).applicationId);
    assertEquals("One stage risk in map", 1, result.get(0).stageRisks.size());

    StageRiskScoreDTO buildStageRisk = result.get(0).getStageRiskScore(buildStage.getId());
    assertRisk(buildStageRisk.risk, 9, 0, 3, 0, 12);

  }

  @Test
  public void testGetAllApplicationTotalApplicationRiskDeDupesAcrossStages() {
    List<StageType> stages = Lists.newArrayList(buildStage, releaseStage);
    Set<String> stageIds = Sets.newHashSet(buildStage.getId(), releaseStage.getId());

    mockGetByPublicId(stages, appPublicId1, application1);
    mockPolicyEval(appId1, buildStage.getId(), policyEvaluation1, violations1);
    mockPolicyEval(appId1, releaseStage.getId(), policyEvaluation1, violations1);

    List<ApplicationRiskScoreDTO> result = dashboardService.getApplicationRisks(Collections.singleton(appPublicId1),
        stageIds, Collections.<String>emptySet(), null, null, Integer.MAX_VALUE);

    assertEquals("One result expected", 1, result.size());
    assertRisk(result.get(0).totalApplicationRisk, 0, 5, 0, 0, 5);
    assertEquals("Risk name was set right", appName, result.get(0).applicationName);
    assertEquals("Risk public appID was set right", appPublicId1, result.get(0).applicationId);
    assertEquals("Two stage risk in map", 2, result.get(0).stageRisks.size());

    StageRiskScoreDTO buildStageRisk = result.get(0).getStageRiskScore(buildStage.getId());
    assertRisk(buildStageRisk.risk, 0, 5, 0, 0, 5);

    StageRiskScoreDTO releaseStageRisk = result.get(0).getStageRiskScore(releaseStage.getId());
    assertRisk(releaseStageRisk.risk, 0, 5, 0, 0, 5);
  }

  @Test
  public void testGetAllApplicationExcludesRisksOfZero() {
    List<StageType> stages = Lists.newArrayList(buildStage);
    Set<String> stageIds = Sets.newHashSet(buildStage.getId());

    String policyEvalId5 = "polEval5";
    String policyName5 = "fifthPolicy";
    PolicyEvaluation policyEvaluation5 = new PolicyEvaluation(appId1, buildStage.getId(), scanId);
    policyEvaluation5.setId(policyEvalId5);
    policyEvaluation5.setTime(new Date(4000L));
    PolicyViolation vio5 = new PolicyViolation(policyEvaluation5, policyEvalId5, policyName5, 0,
        PolicyThreatCategory.LICENSE, vioHash1, groupId1, artifactId1, versionId1, "[]", "");
    List<PolicyViolation> violations5 = Lists.newArrayList(vio5);


    mockGetByPublicId(stages, appPublicId1, application1);
    mockPolicyEval(appId1, buildStage.getId(), policyEvaluation1, violations5);

    List<ApplicationRiskScoreDTO> result = dashboardService.getApplicationRisks(Collections.singleton(appPublicId1),
        stageIds, Collections.<String>emptySet(), null, null, Integer.MAX_VALUE);

    assertEquals("No result expected", 0, result.size());
  }


}
