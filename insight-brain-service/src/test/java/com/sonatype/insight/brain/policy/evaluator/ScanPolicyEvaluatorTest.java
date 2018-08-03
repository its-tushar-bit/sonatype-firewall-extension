/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.policy.PolicyViolationPersistenceLocks;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEventService;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Injector;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ScanPolicyEvaluatorTest
    extends AbstractBrainServiceTest
{
  private Application application;

  private ScanPolicyEvaluator scanPolicyEvaluator;

  private AsyncEventBus asyncEventBus;

  private TestEventHandler<ApplicationEvaluationEvent> handler;

  private InsightWork insightWork;
  
  private ReportService reportService;
  
  private PolicyThreatsAdapter policyThreatsAdapter;
  
  private ComponentPolicyEvaluator componentPolicyEvaluator;
  
  private ApplicationEvaluationEventService applicationEvaluationEventService;

  @After
  public void after() {
    if (handler != null) {
      asyncEventBus.unregister(handler);
    }
  }

  @Before
  public void setup() throws Exception {
    Organization organization = tempEntity.newOrganization();
    application = tempEntity.newApplication("name", "publicId", organization.getId(), "admin");

    Injector injector = getCLMServer().getInjector();
    scanPolicyEvaluator = injector.getInstance(ScanPolicyEvaluator.class);
    asyncEventBus = injector.getInstance(AsyncEventBus.class);
    insightWork = injector.getInstance(InsightWork.class);
    reportService = injector.getInstance(ReportService.class);
    policyThreatsAdapter = injector.getInstance(PolicyThreatsAdapter.class);
    componentPolicyEvaluator = injector.getInstance(ComponentPolicyEvaluator.class);
    applicationEvaluationEventService = injector.getInstance(ApplicationEvaluationEventService.class);
  }

  @Test
  public void testEvaluate_Results_Evaluation() throws Exception {
    String scanId = "scanId";
    Stage stage = new Stage(Stage.ID_BUILD);

    mockReport(scanId, "/ScanPolicyEvaluatorTest/report.zip");

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);

    assertThat(results.evaluation, is(notNullValue()));
    assertThat(results.evaluation.getApplicationId(), is(application.getId()));
    assertThat(results.evaluation.getStageTypeId(), is(stage.getStageTypeId()));
    assertThat(results.evaluation.getScanId(), is(scanId));
  }

  @Test
  public void testEvaluate_Results_AllViolations() throws Exception {
    String scanId = "scanId";
    Stage stage = new Stage(Stage.ID_BUILD);
    mockReport(scanId, "/ScanPolicyEvaluatorTest/report.zip");
    newSecurityPolicy();

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);

    assertThat(results.allViolations, hasSize(36));
    assertThat(results.allViolations.stream().filter(PolicyViolation::isFixed).collect(toList()), hasSize(0));
    assertThat(results.activeViolations, hasSize(36));
  }

  @Test
  public void testEvaluate_Results_WaivedViolations() throws Exception {
    String scanId = "scanId";
    Stage stage = new Stage(Stage.ID_BUILD);
    mockReport(scanId, "/ScanPolicyEvaluatorTest/report.zip");
    Policy policy = newSecurityPolicy();
    PolicyWaiver waiver = tempEntity.newWaiver("f0776db1593e215146d2", policy.getId(), application.getId());

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);

    assertThat(results.activeViolations, hasSize(33));
    for (PolicyViolation violation : results.activeViolations) {
      assertThat(violation.getHash(), is(not(waiver.getHash())));
      assertThat(violation.getGrandfatherTime(), is(nullValue()));
      assertThat(violation.getWaiveTime(), is(nullValue()));
      assertThat(violation.getPolicyWaiverId(), is(nullValue()));
      assertThat(violation.getPolicyWaiverComment(), is(nullValue()));
    }
    List<PolicyViolation> inactiveViolations = getInactiveViolations(results);
    assertThat(inactiveViolations, hasSize(3));
    for (PolicyViolation inactiveViolation : inactiveViolations) {
      assertThat(inactiveViolation.getHash(), is(waiver.getHash()));
      assertThat(inactiveViolation.isGrandfathered(), is(false));
      assertThat(inactiveViolation.getWaiveTime(), is(not(nullValue())));
      assertThat(inactiveViolation.getPolicyWaiverId(), is(waiver.getId()));
      assertThat(inactiveViolation.getPolicyWaiverComment(), is(waiver.getComment()));
    }
  }

  @Test
  public void testEvaluate_Results_GrandfatheredViolations() throws Exception {
    application = tempEntity.newApplicationWithParent();
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);
    boolean grandfatherViolations = true;
    testEvaluate_GrandfatheredViolations(grandfatherViolations, true);
    
    application = tempEntity.newApplicationWithParent();
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);
    grandfatherViolations = false;
    testEvaluate_GrandfatheredViolations(grandfatherViolations, true);
  }

  private void testEvaluate_GrandfatheredViolations(boolean expectGrandfatheredViolations,
                                                    boolean grandfatheringEnabled) throws Exception
  {
    TelemetrySender mockTelemetrySender = mock(TelemetrySender.class);
    String scanId = "scanId";
    Stage stage = new Stage(Stage.ID_BUILD);
    mockReport(scanId, "/ScanPolicyEvaluatorTest/report.zip");
    PolicyViolationPersistenceLocks policyViolationPersistenceLocks = getCLMServer().getInjector()
        .getInstance(PolicyViolationPersistenceLocks.class);
    Policy policy = newSecurityPolicy(5);
    policy.setPolicyViolationGrandfatheringAllowed(expectGrandfatheredViolations);
    new PolicyDAO().update(policy);

    ScanPolicyEvaluator scanPolicyEvaluator = new ScanPolicyEvaluator(insightWork, reportService, policyThreatsAdapter,
        componentPolicyEvaluator, applicationEvaluationEventService, mockTelemetrySender,
        policyViolationPersistenceLocks);

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);

    if (expectGrandfatheredViolations) {
      assertThat(results.activeViolations, hasSize(0));
      List<PolicyViolation> inactiveViolations = getInactiveViolations(results);
      assertThat(inactiveViolations, hasSize(36));
      for (PolicyViolation inactiveViolation : inactiveViolations) {
        assertThat(inactiveViolation.getGrandfatherTime(), is(results.evaluation.getTime()));
        assertThat(inactiveViolation.isWaived(), is(false));
      }
    }
    else {
      assertThat(results.activeViolations, hasSize(36));
      for (PolicyViolation activeViolation : results.activeViolations) {
        assertThat(activeViolation.isGrandfathered(), is(false));
      }
    }

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender, times(2)).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate(application.getId()));
    expectedAttributes.put("grandfathering_enabled", String.valueOf(grandfatheringEnabled));
    expectedAttributes.put("number_of_grandfathered_violations", expectGrandfatheredViolations ? "36" : "0");
    if (expectGrandfatheredViolations) {
      expectedAttributes.put("number_of_grandfathered_violations_with_low_threat_level", "0");
      expectedAttributes.put("number_of_grandfathered_violations_with_moderate_threat_level", "0");
      expectedAttributes.put("number_of_grandfathered_violations_with_severe_threat_level", "36");
      expectedAttributes.put("number_of_grandfathered_violations_with_critical_threat_level", "0");
      expectedAttributes.put("number_of_grandfathered_violations_in_security_policy_threat_category", "36");
      expectedAttributes.put("number_of_grandfathered_violations_in_license_policy_threat_category", "0");
      expectedAttributes.put("number_of_grandfathered_violations_in_quality_policy_threat_category", "0");
      expectedAttributes.put("number_of_grandfathered_violations_in_other_policy_threat_category", "0");
    }
    assertGrandfatheredViolationAttributes(telemetryDataArgumentCaptor.getAllValues().get(1), expectedAttributes);
  }

  @Test
  public void testEvaluate_GrandfatherOnlyOnFirstEvaluation() throws Exception {
    application = tempEntity.newApplicationWithParent();
    Policy policy = newSecurityPolicy();
    policy.setPolicyViolationGrandfatheringAllowed(true);
    new PolicyDAO().update(policy);
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);

    // This is the first evaluation. All policy violations should be grandfathered.
    String scanId1 = "scanId1";
    Stage stage1 = new Stage(Stage.ID_BUILD);
    mockReport(scanId1, "/ScanPolicyEvaluatorTest/report.zip");
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId1, stage1);
    assertThat(results.activeViolations, hasSize(0));
    List<PolicyViolation> inactiveViolations = getInactiveViolations(results);
    assertThat(inactiveViolations, hasSize(36));
    for (PolicyViolation inactiveViolation : inactiveViolations) {
      assertThat(inactiveViolation.getGrandfatherTime(), is(results.evaluation.getTime()));
      assertThat(inactiveViolation.isWaived(), is(false));
    }

    // Delete all violations
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    inactiveViolations.forEach(inactiveViolation -> policyViolationDAO.delete(inactiveViolation));

    // Evaluate again. No policy violations should be grandfathered.
    String scanId2 = "scanId2";
    mockReport(scanId2, "/ScanPolicyEvaluatorTest/report.zip");
    results = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId2, stage1);
    assertThat(results.activeViolations, hasSize(36));

    // Evaluate for a different stage. No policy violations should be grandfathered.
    String scanId3 = "scanId3";
    Stage stage2 = new Stage(Stage.ID_RELEASE);
    mockReport(scanId3, "/ScanPolicyEvaluatorTest/report.zip");
    results = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId3, stage2);
    assertThat(results.activeViolations, hasSize(36));
  }

  @Test
  public void testEvaluate_Results_NotifiableViolations() throws Exception {
    newRelativePopularityPolicy();

    String scanId = "scanId";
    Stage stage = new Stage(Stage.ID_BUILD);

    // 1st evaluation. The report contains one component that triggers the policy, so there is one notifiable violation.
    mockReport(scanId, "/ScanPolicyEvaluatorTest/testEvaluate_Results_NotifiableViolations/before");
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);
    assertThat(results.activeViolations, hasSize(1));
    assertThat(results.notifiableViolations, hasSize(1));

    // 2nd evaluation. Nothing changed, so there are no new violations, so no notifiable violations.
    results = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);
    assertThat(results.activeViolations, hasSize(1));
    assertThat(results.notifiableViolations, is(nullValue()));

    // 3rd evaluation. The report contains a new component that triggers the policy, so there is one new notifiable
    // violation.
    scanId = "newScanId";
    mockReport(scanId, "/ScanPolicyEvaluatorTest/testEvaluate_Results_NotifiableViolations/after");
    results = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);
    assertThat(results.activeViolations, hasSize(2));
    assertThat(results.notifiableViolations, hasSize(1));
  }

  @Test
  public void testEvaluate_EmitsApplicationEvaluationEvent() throws IOException, InterruptedException {
    handler = new TestEventHandler<>(new CountDownLatch(1));

    newSecurityPolicy(5);
    String scanId = "scanId";
    Stage stage = new Stage(Stage.ID_BUILD);

    // Simulate that the report is available
    mockReport(scanId, "/ScanPolicyEvaluatorTest/report.zip");

    asyncEventBus.register(handler);

    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS), is(true));
    ApplicationEvaluationEvent event = handler.getEvent();
    assertThat(event, is(notNullValue()));
    assertThat(event.stageTypeId, is(Stage.ID_BUILD));
    assertThat(event.ownerId, is(application.getId()));
    assertThat(event.initiator, is("system"));
    assertThat(event.policyEvaluationId, is(scanPolicyEvaluatorResults.evaluation.getId()));
    assertThat(event.evaluationDate, is(scanPolicyEvaluatorResults.evaluation.getTime()));
    assertThat(event.affectedComponentCount, is(7));
    assertThat(event.criticalComponentCount, is(0));
    assertThat(event.severeComponentCount, is(7));
    assertThat(event.moderateComponentCount, is(0));
    assertThat(event.outcome, is(ApplicationEvaluationEvent.ACTION_ID_NONE));
  }

  @Test
  public void testEvaluate_DeletesPreviousScanFile() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId1 = "scanId1";
    File scanFile1 = createScanFile(application, scanId1);
    mockReport(scanId1, "/ScanPolicyEvaluatorTest/report.zip");
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId1, stage);
    assertThat(scanFile1.exists(), is(true));

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    String scanId2 = "scanId2";
    File scanFile2 = createScanFile(application, scanId2);
    mockReport(scanId2, "/ScanPolicyEvaluatorTest/report.zip");
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId2, stage);
    assertThat(scanFile1.exists(), is(false));
    assertThat(scanFile2.exists(), is(true));
  }

  @Test
  public void testEvaluate_ReEvaluationDoesNotDeleteScanFile() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = "scanId";
    File scanFile = createScanFile(application, scanId);
    mockReport(scanId, "/ScanPolicyEvaluatorTest/report.zip");
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);
    assertThat(scanFile.exists(), is(true));

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);
    assertThat(scanFile.exists(), is(true));
  }

  @Test
  public void testEvaluate_DoesNotDeleteScanFileForDifferentStage() throws Exception {
    Stage stage1 = new Stage(Stage.ID_BUILD);

    String scanId1 = "scanId1";
    File scanFile1 = createScanFile(application, scanId1);
    mockReport(scanId1, "/ScanPolicyEvaluatorTest/report.zip");
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId1, stage1);
    assertThat(scanFile1.exists(), is(true));

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    Stage stage2 = new Stage(Stage.ID_RELEASE);
    String scanId2 = "scanId2";
    File scanFile2 = createScanFile(application, scanId2);
    mockReport(scanId2, "/ScanPolicyEvaluatorTest/report.zip");
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId2, stage2);
    assertThat(scanFile1.exists(), is(true));
    assertThat(scanFile2.exists(), is(true));
  }

  @Test
  public void testEvaluate_CanReEvaluatePreviousScan() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId1 = "scanId1";
    File scanFile1 = createScanFile(application, scanId1);
    mockReport(scanId1, "/ScanPolicyEvaluatorTest/report.zip");
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId1, stage);

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    String scanId2 = "scanId2";
    createScanFile(application, scanId2);
    mockReport(scanId2, "/ScanPolicyEvaluatorTest/report.zip");
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId2, stage);

    // The first scan file was deleted by the second policy evaluation.
    // A re-evaluation of the first scan doesn't need the scan so it should succeed.
    assertThat(scanFile1.exists(), is(false));
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId1, stage);
  }

  @Test
  public void testEvaluate_UpdateFixedViolations() throws Exception {
    String scanId = "scanId";
    Stage stage = new Stage(Stage.ID_BUILD);
    mockReport(scanId, "/ScanPolicyEvaluatorTest/report.zip");
    Policy policy = newSecurityPolicy();

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);

    assertThat(results.allViolations, hasSize(36));

    new PolicyDAO().delete(policy);

    results = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);

    assertThat(results.allViolations, hasSize(0));
    List<PolicyViolation> allViolations = new PolicyViolationDAO().getByApplicationId(application.getId());
    assertThat(allViolations, hasSize(36));
    List<PolicyViolation> fixedViolations = allViolations.stream().filter(PolicyViolation::isFixed).collect(toList());
    assertThat(fixedViolations, hasSize(36));
    for (PolicyViolation violation : fixedViolations) {
      assertThat(violation.toString(), violation.getFixTime(), is(results.evaluation.getTime()));
    }
  }

  @Test
  public void testEvaluate_UpdateWaivedViolations() throws Exception {
    String scanId = "scanId";
    Stage stage = new Stage(Stage.ID_BUILD);
    mockReport(scanId, "/ScanPolicyEvaluatorTest/report.zip");
    Policy policy = newSecurityPolicy();

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);
    Date openTime = results.evaluation.getTime();

    assertThat(results.activeViolations, hasSize(36));

    PolicyWaiver waiver = tempEntity.newWaiver("f0776db1593e215146d2", policy.getId(), application.getId());

    results = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);

    assertThat(results.activeViolations, hasSize(33));
    List<PolicyViolation> waivedViolations = new PolicyViolationDAO()
        .getUnfixedByApplicationIdAndStageId(application.getId(), stage.getStageTypeId()).stream()
        .filter(PolicyViolation::isWaived).collect(toList());
    assertThat(waivedViolations, hasSize(3));
    for (PolicyViolation waivedViolation : waivedViolations) {
      assertThat(waivedViolation.getHash(), is(waiver.getHash()));
      assertThat(waivedViolation.getOpenTime(), is(openTime));
      assertThat(waivedViolation.getFixTime(), is(nullValue()));
      assertThat(waivedViolation.getWaiveTime(), is(results.evaluation.getTime()));
      assertThat(waivedViolation.getPolicyWaiverId(), is(waiver.getId()));
      assertThat(waivedViolation.getPolicyWaiverComment(), is(waiver.getComment()));
    }

    new PolicyWaiverDAO().delete(waiver);

    results = scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);

    assertThat(results.activeViolations, hasSize(36));
    List<PolicyViolation> unfixedViolations = new PolicyViolationDAO()
        .getUnfixedByApplicationIdAndStageId(application.getId(), stage.getStageTypeId());
    assertThat(unfixedViolations.stream().filter(PolicyViolation::isWaived).collect(toList()), hasSize(0));
    List<PolicyViolation> unwaivedViolations = unfixedViolations.stream()
        .filter(violation -> violation.getHash().equals(waiver.getHash())).collect(toList());
    assertThat(unwaivedViolations, hasSize(3));
    for (PolicyViolation unwaivedViolation : unwaivedViolations) {
      assertThat(unwaivedViolation.getHash(), is(waiver.getHash()));
      assertThat(unwaivedViolation.getOpenTime(), is(results.evaluation.getTime()));
      assertThat(unwaivedViolation.getFixTime(), is(nullValue()));
      assertThat(unwaivedViolation.getWaiveTime(), is(nullValue()));
      assertThat(unwaivedViolation.getPolicyWaiverId(), is(nullValue()));
      assertThat(unwaivedViolation.getPolicyWaiverComment(), is(nullValue()));
    }

    assertThat(new PolicyViolationDAO().getByApplicationId(application.getId()), hasSize(39));
  }

  @Test
  public void testEvaluate_ApplicationStageComponentCounts() throws Exception {
    TelemetrySender mockTelemetrySender = mock(TelemetrySender.class);
    String scanId = "scanId";
    Stage stage = new Stage(Stage.ID_BUILD);
    mockReport(scanId, "/ScanPolicyEvaluatorTest/report.zip");
    PolicyViolationPersistenceLocks policyViolationPersistenceLocks = getCLMServer().getInjector()
        .getInstance(PolicyViolationPersistenceLocks.class);
    ScanPolicyEvaluator scanPolicyEvaluator = new ScanPolicyEvaluator(insightWork, reportService, policyThreatsAdapter,
        componentPolicyEvaluator, applicationEvaluationEventService, mockTelemetrySender,
        policyViolationPersistenceLocks);

    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender, times(2)).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate(application.getId()));
    expectedAttributes.put("stage_id", Stage.ID_BUILD);
    expectedAttributes.put("number_of_maven_components", "28");
    expectedAttributes.put("number_of_components", "28");
    assertApplicationStageAttributes(telemetryDataArgumentCaptor.getAllValues().get(0), expectedAttributes);
  }

  @Test
  public void testSendApplicationStageComponentCounts_NoComponents() {
    TelemetrySender mockTelemetrySender = mock(TelemetrySender.class);
    ScanPolicyEvaluator scanPolicyEvaluator = new ScanPolicyEvaluator(null, null, null, null, null, mockTelemetrySender,
        null);

    scanPolicyEvaluator.sendApplicationStageComponentCounts("applicationId", "stageId", new ArrayList<>());

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate("applicationId"));
    expectedAttributes.put("stage_id", "stageId");
    expectedAttributes.put("number_of_components", "0");
    assertApplicationStageAttributes(telemetryDataArgumentCaptor.getValue(), expectedAttributes);
  }

  @Test
  public void testSendApplicationStageComponentCounts() {
    TelemetrySender mockTelemetrySender = mock(TelemetrySender.class);
    ScanPolicyEvaluator scanPolicyEvaluator = new ScanPolicyEvaluator(null, null, null, null, null, mockTelemetrySender,
        null);
    Object[] formatsAndCounts = new Object[]{
        "unknown", 1, ComponentIdentifier.FORMAT_MAVEN, 2, ComponentIdentifier.FORMAT_NPM, 3,
        ComponentIdentifier.FORMAT_NUGET, 4, ComponentIdentifier.FORMAT_ANAME, 5, ComponentIdentifier.FORMAT_PYPI, 6,
        ComponentIdentifier.FORMAT_RPM, 7, ComponentIdentifier.FORMAT_RUBYGEMS, 8
    };
    
    scanPolicyEvaluator.sendApplicationStageComponentCounts("applicationId", "stageId", components(formatsAndCounts));

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate("applicationId"));
    expectedAttributes.put("stage_id", "stageId");
    expectedAttributes.put("number_of_unknown_components", "1");
    expectedAttributes.put("number_of_maven_components", "2");
    expectedAttributes.put("number_of_npm_components", "3");
    expectedAttributes.put("number_of_nuget_components", "4");
    expectedAttributes.put("number_of_aname_components", "5");
    expectedAttributes.put("number_of_pypi_components", "6");
    expectedAttributes.put("number_of_rpm_components", "7");
    expectedAttributes.put("number_of_gem_components", "8");
    expectedAttributes.put("number_of_components", "36");
    assertApplicationStageAttributes(telemetryDataArgumentCaptor.getValue(), expectedAttributes);
  }

  @Test
  public void testSendGrandfatheredViolationCounts_NoGrandfatheredViolations() {
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);

    TelemetrySender mockTelemetrySender = mock(TelemetrySender.class);
    ScanPolicyEvaluator scanPolicyEvaluator = new ScanPolicyEvaluator(null, null, null, null, null, mockTelemetrySender,
        null);

    PolicyEvaluation policyEvaluation = new PolicyEvaluation(application.getId(), "stageId", "scanId");
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(policyViolation(policyEvaluation, 1, PolicyThreatCategory.LICENSE, false));
    policyViolations.add(policyViolation(policyEvaluation, 3, PolicyThreatCategory.SECURITY, false));
    policyViolations.add(policyViolation(policyEvaluation, 5, PolicyThreatCategory.QUALITY, false));
    policyViolations.add(policyViolation(policyEvaluation, 7, PolicyThreatCategory.OTHER, false));

    scanPolicyEvaluator.sendGrandfatheredViolationTelemetryData(application.getId(), policyViolations);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate(application.getId()));
    expectedAttributes.put("grandfathering_enabled", "true");
    expectedAttributes.put("number_of_grandfathered_violations", "0");
    assertGrandfatheredViolationAttributes(telemetryDataArgumentCaptor.getValue(), expectedAttributes);
  }

  @Test
  public void testSendGrandfatheredViolationCounts() {
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);

    TelemetrySender mockTelemetrySender = mock(TelemetrySender.class);
    ScanPolicyEvaluator scanPolicyEvaluator = new ScanPolicyEvaluator(null, null, null, null, null, mockTelemetrySender,
        null);

    PolicyEvaluation policyEvaluation = new PolicyEvaluation(application.getId(), "stageId", "scanId");
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(policyViolation(policyEvaluation, 1, PolicyThreatCategory.LICENSE, true));
    policyViolations.add(policyViolation(policyEvaluation, 3, PolicyThreatCategory.SECURITY, true));
    policyViolations.add(policyViolation(policyEvaluation, 5, PolicyThreatCategory.QUALITY, true));
    policyViolations.add(policyViolation(policyEvaluation, 7, PolicyThreatCategory.OTHER, true));
    policyViolations.add(policyViolation(policyEvaluation, 9, PolicyThreatCategory.LICENSE, true));
    policyViolations.add(policyViolation(policyEvaluation, 1, PolicyThreatCategory.LICENSE, false));
    policyViolations.add(policyViolation(policyEvaluation, 3, PolicyThreatCategory.SECURITY, false));
    policyViolations.add(policyViolation(policyEvaluation, 5, PolicyThreatCategory.QUALITY, false));
    policyViolations.add(policyViolation(policyEvaluation, 7, PolicyThreatCategory.OTHER, false));

    scanPolicyEvaluator.sendGrandfatheredViolationTelemetryData(application.getId(), policyViolations);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate(application.getId()));
    expectedAttributes.put("grandfathering_enabled", "true");
    expectedAttributes.put("number_of_grandfathered_violations", "5");
    expectedAttributes.put("number_of_grandfathered_violations_with_low_threat_level", "1");
    expectedAttributes.put("number_of_grandfathered_violations_with_moderate_threat_level", "1");
    expectedAttributes.put("number_of_grandfathered_violations_with_severe_threat_level", "2");
    expectedAttributes.put("number_of_grandfathered_violations_with_critical_threat_level", "1");
    expectedAttributes.put("number_of_grandfathered_violations_in_security_policy_threat_category", "1");
    expectedAttributes.put("number_of_grandfathered_violations_in_license_policy_threat_category", "2");
    expectedAttributes.put("number_of_grandfathered_violations_in_quality_policy_threat_category", "1");
    expectedAttributes.put("number_of_grandfathered_violations_in_other_policy_threat_category", "1");
    assertGrandfatheredViolationAttributes(telemetryDataArgumentCaptor.getValue(), expectedAttributes);
  }

  private PolicyViolation policyViolation(PolicyEvaluation policyEvaluation,
                                          int threatLevel,
                                          PolicyThreatCategory policyThreatCategory,
                                          boolean grandfathered)
  {
    PolicyViolation policyViolation = new PolicyViolation(policyEvaluation, "policyId", "policyName", threatLevel,
        policyThreatCategory, "hash", null, "json", "filename");
    if (grandfathered) {
      policyViolation.setGrandfatherTime(new Date());
    }
    return policyViolation;
  }

  private List<Component> components(Object[] formatsAndCounts) {
    List<Component> components = new ArrayList<>();
    for (int formatAndCountIndex = 0; formatAndCountIndex < formatsAndCounts.length; formatAndCountIndex += 2) {
      for (int count = 0; count < (int) formatsAndCounts[formatAndCountIndex + 1]; count++) {
        components.add(component((String) formatsAndCounts[formatAndCountIndex]));
      }
    }
    return components;
  }

  private Component component(String format) {
    switch (format) {
      case ComponentIdentifier.FORMAT_MAVEN:
        return new Component(ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
      case ComponentIdentifier.FORMAT_NPM:
        return new Component(ComponentIdentifier.createNpmCoordinates("p", "v"));
      case ComponentIdentifier.FORMAT_NUGET:
        return new Component(ComponentIdentifier.createNugetCoordinates("p", "v"));
      case ComponentIdentifier.FORMAT_ANAME:
        return new Component(ComponentIdentifier.createAnameCoordinates("n", "q", "v"));
      case ComponentIdentifier.FORMAT_PYPI:
        return new Component(ComponentIdentifier.createPypiCoordinates("n", "v", "q", "e"));
      case ComponentIdentifier.FORMAT_RPM:
        return new Component(ComponentIdentifier.createRpmCoordinates("n", "v", "a"));
      case ComponentIdentifier.FORMAT_RUBYGEMS:
        return new Component(ComponentIdentifier.createRubyGemsCoordinates("n", "v", "p"));
      default:
        return new Component();
    }
  }

  private void assertApplicationStageAttributes(TelemetryData telemetryData, Map<String, Object> expectedAttributes) {
    assertThat(telemetryData, is(notNullValue()));
    assertThat(telemetryData.getPurpose(), is(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS));
    assertThat(telemetryData.getTimestamp(), is(lessThanOrEqualTo(System.currentTimeMillis())));
    assertThat(telemetryData.getAttributes(), is(expectedAttributes));
  }

  private void assertGrandfatheredViolationAttributes(TelemetryData telemetryData,
                                                      Map<String, Object> expectedAttributes)
  {
    assertThat(telemetryData, is(notNullValue()));
    assertThat(telemetryData.getPurpose(), is(TelemetryPurpose.APPLICATION_EVALUATION_GRANDFATHERED_VIOLATION_COUNTS));
    assertThat(telemetryData.getTimestamp(), is(lessThanOrEqualTo(System.currentTimeMillis())));
    assertThat(telemetryData.getAttributes().keySet(), containsInAnyOrder(expectedAttributes.keySet().toArray()));
    for (Entry<String, Object> entry : expectedAttributes.entrySet()) {
      assertThat(telemetryData.getAttributes(), hasEntry(entry.getKey(), entry.getValue()));
    }
  }
  
  @Test
  public void testEvaluate_BeforeAndAfterAddingConditionTriggerData() throws Exception {
    // Add a policy
    Policy policy = new Policy(null, "Test Policy");
    policy.setThreatLevel(5);
    policy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "7"));
    constraint.addCondition(new Condition(LicenseThreatGroupLevelConditionType.ID, ">=", "2"));
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    // Add a legacy policy violation - i.e. the way it used to be stored before we added condition trigger data.
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("commons-httpclient",
        "commons-httpclient", "3.1", "", "jar");
    Stage stage = new Stage(Stage.ID_BUILD);
    Date beforeTime = new Date(System.currentTimeMillis() - 2000);
    PolicyEvaluation policyEvaluationBefore = tempEntity.newPolicyEvaluation(application.getId(),
        stage.getStageTypeId(), "scanIdBefore", beforeTime);
    String constraintFactsJson = IOUtils.toString(getClass().getResource(
        "/ScanPolicyEvaluatorTest/testEvaluate_BeforeAndAfterAddingConditionTriggerData/policy-violation-constraint-facts.json"),
        "UTF-8");
    constraintFactsJson = constraintFactsJson.replace("TestConstraintId", policy.getConstraints().get(0).getId());
    PolicyViolation policyViolationBefore = new PolicyViolation(policyEvaluationBefore, policy.getId(),
        policy.getName(), policy.getThreatLevel(), policy.getThreatCategory(), "964cd74171f427720480",
        componentIdentifier, constraintFactsJson, "commons-httpclient-3.1.jar");
    new PolicyViolationDAO().insert(policyViolationBefore);
    assertThat(policyViolationBefore.getOpenTime(), is(beforeTime));

    // Evaluate the policy.
    String scanId = "scanId";
    mockReport(scanId, "/ScanPolicyEvaluatorTest/testEvaluate_BeforeAndAfterAddingConditionTriggerData/report");
    scanPolicyEvaluator.evaluate(application.getPublicId(), scanId, stage);

    // There should be only one policy violation (the existing one).
    List<PolicyViolation> policyViolationsAfter = new PolicyViolationDAO().getByApplicationId(application.getId());
    assertThat(policyViolationsAfter, hasSize(1));
    PolicyViolation policyViolationAfter = policyViolationsAfter.get(0);
    assertThat(policyViolationAfter.getId(), is(policyViolationBefore.getId()));
    assertThat(policyViolationAfter.getOpenTime(), is(beforeTime));
    assertThat(policyViolationAfter.getConstraintFacts(), hasSize(1));
    ConstraintFact constraintFact = policyViolationAfter.getConstraintFacts().get(0);
    assertThat(constraintFact.getConditionFacts(), hasSize(2));
    assertThat(constraintFact.getConditionFacts().get(0).getConditionIndex(), is(0));
    assertThat(constraintFact.getConditionFacts().get(1).getConditionIndex(), is(1));
  }

  @Test
  public void testEvaluate_UpdateGrandfatheredViolations() throws Exception {
    newSecurityPolicy();

    // Evaluate policies for two stages.
    String scanIdStage1 = "scanIdStage1";
    Stage stage1 = new Stage(Stage.ID_BUILD);
    mockReport(scanIdStage1, "/ScanPolicyEvaluatorTest/report.zip");
    ScanPolicyEvaluatorResults resultsStage1 = scanPolicyEvaluator.evaluate(application.getPublicId(), scanIdStage1,
        stage1);
    Date evaluationTimeStage1 = resultsStage1.evaluation.getTime();
    assertThat(resultsStage1.activeViolations, hasSize(36));

    String scanIdStage2 = "scanIdStage2";
    Stage stage2 = new Stage(Stage.ID_RELEASE);
    mockReport(scanIdStage2, "/ScanPolicyEvaluatorTest/report.zip");
    ScanPolicyEvaluatorResults resultsStage2 = scanPolicyEvaluator.evaluate(application.getPublicId(), scanIdStage2,
        stage2);
    Date evaluationTimeStage2 = resultsStage2.evaluation.getTime();
    assertThat(resultsStage1.activeViolations, hasSize(36));

    // Grandfather a violation in one stage.
    PolicyViolation grandfatheredViolation = resultsStage1.activeViolations.get(0);
    Date grandfatherTime = new Date();
    grandfatheredViolation.setGrandfatherTime(grandfatherTime);
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    policyViolationDAO.update(grandfatheredViolation);

    // Evaluate again for the same stage. The violation should remain grandfathered.
    String scanIdStage1New = "scanIdStage1New";
    mockReport(scanIdStage1New, "/ScanPolicyEvaluatorTest/report.zip");
    ScanPolicyEvaluatorResults resultsStage1New = scanPolicyEvaluator.evaluate(application.getPublicId(),
        scanIdStage1New, stage1);
    assertThat(resultsStage1New.activeViolations, hasSize(35));
    List<PolicyViolation> grandfatheredViolations1 = getInactiveViolations(resultsStage1New);
    assertThat(grandfatheredViolations1, hasSize(1));
    assertThat(grandfatheredViolations1.get(0).getId(), is(grandfatheredViolation.getId()));
    assertThat(grandfatheredViolations1.get(0).getGrandfatherTime(), is(grandfatherTime));
    assertThat(grandfatheredViolations1.get(0).getOpenTime(), is(evaluationTimeStage1));
    assertThat(grandfatheredViolations1.get(0).getFixTime(), is(nullValue()));

    // Evaluate again for the other stage.
    // The violation should be grandfathered (because grandfathering works across stages).
    String scanIdStage2New = "scanIdStage2New";
    mockReport(scanIdStage2New, "/ScanPolicyEvaluatorTest/report.zip");
    ScanPolicyEvaluatorResults resultsStage2New = scanPolicyEvaluator.evaluate(application.getPublicId(),
        scanIdStage2New, stage2);
    assertThat(resultsStage2New.activeViolations, hasSize(35));
    List<PolicyViolation> grandfatheredViolations2 = getInactiveViolations(resultsStage2New);
    assertThat(grandfatheredViolations2, hasSize(1));
    assertThat(PolicyViolationComparator.COMPARATOR.compare(grandfatheredViolations2.get(0), grandfatheredViolation),
        is(0));
    assertThat(grandfatheredViolations2.get(0).getId(), is(not(grandfatheredViolation.getId())));
    assertThat(grandfatheredViolations2.get(0).getId(), is(resultsStage2.activeViolations.get(0).getId()));
    assertThat(grandfatheredViolations2.get(0).getGrandfatherTime(), is(grandfatherTime));
    assertThat(grandfatheredViolations2.get(0).getOpenTime(), is(evaluationTimeStage2));
    assertThat(grandfatheredViolations2.get(0).getFixTime(), is(nullValue()));

    // Disable grandfathering and evaluate policies again for the first stage.
    // The grandfathered violation should remain grandfathered.
    application.setPolicyViolationGrandfatheringEnabled(false);
    new ApplicationDAO().update(application);
    String scanIdStage1New1 = "scanIdStage1New1";
    mockReport(scanIdStage1New1, "/ScanPolicyEvaluatorTest/report.zip");
    ScanPolicyEvaluatorResults resultsStage1New1 = scanPolicyEvaluator.evaluate(application.getPublicId(),
        scanIdStage1New1, stage1);
    assertThat(resultsStage1New1.activeViolations, hasSize(35));
    List<PolicyViolation> grandfatheredViolations3 = getInactiveViolations(resultsStage1New1);
    assertThat(grandfatheredViolations3, hasSize(1));
    assertThat(grandfatheredViolations3.get(0).getId(), is(grandfatheredViolation.getId()));
    grandfatheredViolation = new PolicyViolationDAO().getById(grandfatheredViolation.getId());
    assertThat(grandfatheredViolation.isFixed(), is(false));
    assertThat(grandfatheredViolation.getGrandfatherTime(), is(grandfatherTime));
  }

  @Test
  public void testEvaluate_PolicyViolationGrandfatheringEnabledAtDifferentLevels() throws Exception {
    OrganizationDAO organizationDAO = new OrganizationDAO();
    ApplicationDAO applicationDAO = new ApplicationDAO();
    Organization organization = tempEntity.newOrganization();

    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      // Grandfathering is not configured for app or org.
      organization.setPolicyViolationGrandfatheringEnabled(null);
      organization.setAllowPolicyViolationGrandfatheringOverride(true);
      organizationDAO.update(organization);
      application = tempEntity.newApplication(organization.getId());
      application.setPolicyViolationGrandfatheringEnabled(null);
      applicationDAO.update(application);
      testEvaluate_GrandfatheredViolations(false, false);

      // The app can override grandfathering and grandfathering is enabled for app.
      application = tempEntity.newApplication(organization.getId());
      application.setPolicyViolationGrandfatheringEnabled(true);
      applicationDAO.update(application);
      testEvaluate_GrandfatheredViolations(true, true);

      // The app cannot override grandfathering and grandfathering is disabled for org.
      organization.setAllowPolicyViolationGrandfatheringOverride(false);
      organizationDAO.update(organization);
      application = tempEntity.newApplication(organization.getId());
      application.setPolicyViolationGrandfatheringEnabled(true);
      applicationDAO.update(application);
      testEvaluate_GrandfatheredViolations(false, false);

      // The app cannot override grandfathering and grandfathering is enabled for org.
      organization.setPolicyViolationGrandfatheringEnabled(true);
      organization.setAllowPolicyViolationGrandfatheringOverride(false);
      organizationDAO.update(organization);
      application = tempEntity.newApplication(organization.getId());
      application.setPolicyViolationGrandfatheringEnabled(false);
      applicationDAO.update(application);
      testEvaluate_GrandfatheredViolations(true, true);
    }
  }

  private File createScanFile(Application app, String scanId) {
    File scanFile = insightWork.getScanFile(app.getId(), scanId);
    scanFile.delete();
    URL testScanFileUrl = getClass().getResource("/ScanPolicyEvaluatorTest/scan.xml.gz");
    try {
      FileUtils.copyFile(new File(testScanFileUrl.getFile()), scanFile);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    return scanFile;
  }

  private void waitForTimeAdvance() {
    for (long start = System.currentTimeMillis(); System.currentTimeMillis() <= start;) {
    }
  }

  private Policy newSecurityPolicy() {
    return newSecurityPolicy(5);
  }

  private Policy newSecurityPolicy(int policyThreatLevel) {
    Policy policy = new Policy(null, "Test Policy");
    policy.setThreatLevel(policyThreatLevel);
    policy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    new PolicyDAO().insert(policy);
    return policy;
  }

  private Policy newRelativePopularityPolicy() {
    Policy policy = new Policy(null, "Test Policy Age");
    policy.setThreatLevel(5);
    policy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(RelativePopularityConditionType.ID, ">=", "0"));
    policy.addConstraint(constraint);
    new PolicyDAO().insert(policy);
    return policy;
  }

  private List<PolicyViolation> getInactiveViolations(ScanPolicyEvaluatorResults scanPolicyEvaluatorResults) {
    List<PolicyViolation> inactiveViolations = new ArrayList<>(scanPolicyEvaluatorResults.allViolations);
    inactiveViolations.removeAll(scanPolicyEvaluatorResults.activeViolations);
    return inactiveViolations;
  }
}
