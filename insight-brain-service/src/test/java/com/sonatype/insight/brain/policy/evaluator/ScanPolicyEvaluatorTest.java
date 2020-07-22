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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ComponentCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.DataSourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.DependencyTypeConditionType;
import com.sonatype.insight.brain.model.policy.conditions.HygieneRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.IdentificationSourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.PackageUrlConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.WebhookNotification;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.MockReportDownloader;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.pdf.PdfGenerator;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.brain.webhook.FilteringTestEventHandler;
import com.sonatype.insight.brain.webhook.PolicyAlertEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Binder;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.COUNT;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ScanPolicyEvaluatorTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput policyViolationLoggerOutput =
      new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
  
  private Organization organization;

  private Application application;

  @Inject
  private ScanPolicyEvaluator scanPolicyEvaluator;

  @Inject
  private InsightWork insightWork;

  @Inject
  private AsyncEventBus asyncEventBus;

  private TestEventHandler<ApplicationEvaluationEvent> handler;

  private FilteringTestEventHandler<PolicyAlertEvent> policyAlertHandler;

  private MockReportDownloader mockReportDownloader;

  private TelemetrySender mockTelemetrySender;

  @Inject
  private TestProductLicense testProductLicense;

  @Override
  public void configure(Binder binder) {
    mockReportDownloader = new MockReportDownloader();
    binder.bind(ReportDownloader.class).toInstance(mockReportDownloader.getMock());
    mockTelemetrySender = mock(TelemetrySender.class);
    binder.bind(TelemetrySender.class).toInstance(mockTelemetrySender);

    super.configure(binder);
  }

  @After
  public void after() {
    if (handler != null) {
      asyncEventBus.unregister(handler);
    }
    if (policyAlertHandler != null) {
      asyncEventBus.unregister(policyAlertHandler);
    }
  }

  @Before
  public void setup() throws Exception {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());
  }

  @Test
  public void testEvaluate_Results_Evaluation() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("report");

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(results.evaluation).isNotNull();
    assertThat(results.evaluation.getApplicationId()).isEqualTo(application.getId());
    assertThat(results.evaluation.getStageTypeId()).isEqualTo(stage.getStageTypeId());
    assertThat(results.evaluation.getScanId()).isEqualTo(scanId);
    assertThat(results.evaluation.getCommitHash()).isEqualTo("testCommitHash");
  }

  @Test
  public void testEvaluate_Results_AllViolations() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    newSecurityPolicy();

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(results.allViolations).hasSize(36).filteredOn(PolicyViolation::isFixed).isEmpty();
    assertThat(results.activeViolations).hasSize(36);
  }

  @Test
  public void testEvaluate_PolicyNameChange() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    Policy policy = newSecurityPolicy();

    ScanPolicyEvaluatorResults results1 = scanPolicyEvaluator.evaluate(application, scanId, stage);
    for (PolicyViolation violation : results1.activeViolations) {
      assertThat(violation.getPolicyName()).isEqualTo(policy.getName());
    }
    List<PolicyViolation> persistedViolations1 = new PolicyViolationDAO().getByApplicationId(application.getId());
    assertThat(persistedViolations1).allSatisfy(violation -> {
      assertThat(violation.getPolicyName()).isEqualTo(policy.getName());
    });

    policy.setName("PolicyName1");
    new PolicyDAO().update(policy);

    String scanId2 = simulateReportIsAvailable("report");
    ScanPolicyEvaluatorResults results2 = scanPolicyEvaluator.evaluate(application, scanId2, stage);
    assertThat(results2.activeViolations).allSatisfy(violation -> {
      assertThat(violation.getPolicyName()).isEqualTo(policy.getName());
    });
    List<PolicyViolation> persistedViolations2 = new PolicyViolationDAO().getByApplicationId(application.getId());
    assertThat(persistedViolations2).allSatisfy(violation -> {
      assertThat(violation.getPolicyName()).isEqualTo(policy.getName());
    });
  }

  @Test
  public void testEvaluate_Results_WaivedViolations() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    Policy policy = newSecurityPolicy();
    PolicyWaiver waiver = tempEntity.newWaiver("f0776db1593e215146d2", policy.getId(), application.getId());

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(results.activeViolations).hasSize(33).allSatisfy(violation -> {
      assertThat(violation.getHash()).isNotEqualTo(waiver.getHash());
      assertThat(violation.getGrandfatherTime()).isNull();
      assertThat(violation.getWaiveTime()).isNull();
      assertThat(violation.getPolicyWaiverId()).isNull();
      assertThat(violation.getPolicyWaiverComment()).isNull();
    });
    List<PolicyViolation> inactiveViolations = getInactiveViolations(results);
    assertThat(inactiveViolations).hasSize(3).allSatisfy(inactiveViolation -> {
      assertThat(inactiveViolation.getHash()).isEqualTo(waiver.getHash());
      assertThat(inactiveViolation.isGrandfathered()).isFalse();
      assertThat(inactiveViolation.getWaiveTime()).isNotNull();
      assertThat(inactiveViolation.getPolicyWaiverId()).isEqualTo(waiver.getId());
      assertThat(inactiveViolation.getPolicyWaiverComment()).isEqualTo(waiver.getComment());
    });
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
    reset(mockTelemetrySender);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    Policy policy = newSecurityPolicy();
    policy.setPolicyViolationGrandfatheringAllowed(expectGrandfatheredViolations);
    new PolicyDAO().update(policy);

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage);

    if (expectGrandfatheredViolations) {
      assertThat(results.activeViolations).hasSize(0);
      List<PolicyViolation> inactiveViolations = getInactiveViolations(results);
      assertThat(inactiveViolations).hasSize(36).allSatisfy(inactiveViolation -> {
        assertThat(inactiveViolation.getGrandfatherTime()).isEqualTo(results.evaluation.getTime());
        assertThat(inactiveViolation.isWaived()).isFalse();
      });
    }
    else {
      assertThat(results.activeViolations).hasSize(36).allSatisfy(activeViolation -> {
        assertThat(activeViolation.isGrandfathered()).isFalse();
      });
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
    String scanId1 = simulateReportIsAvailable("report");
    Stage stage1 = new Stage(Stage.ID_BUILD);
    ScanPolicyEvaluatorResults results1 = scanPolicyEvaluator.evaluate(application, scanId1, stage1);
    assertThat(results1.activeViolations).hasSize(0);
    List<PolicyViolation> inactiveViolations = getInactiveViolations(results1);
    assertThat(inactiveViolations).hasSize(36).allSatisfy(inactiveViolation -> {
      assertThat(inactiveViolation.getGrandfatherTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(inactiveViolation.isWaived()).isFalse();
    });

    // Delete all violations
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    inactiveViolations.forEach(inactiveViolation -> policyViolationDAO.delete(inactiveViolation));

    // Evaluate again. No policy violations should be grandfathered.
    String scanId2 = simulateReportIsAvailable("report");
    ScanPolicyEvaluatorResults results2 = scanPolicyEvaluator.evaluate(application, scanId2, stage1);
    assertThat(results2.activeViolations).hasSize(36);

    // Evaluate for a different stage. No policy violations should be grandfathered.
    String scanId3 = simulateReportIsAvailable("report");
    Stage stage2 = new Stage(Stage.ID_RELEASE);
    ScanPolicyEvaluatorResults results3 = scanPolicyEvaluator.evaluate(application, scanId3, stage2);
    assertThat(results3.activeViolations).hasSize(36);
  }

  @Test
  public void testEvaluate_GrandfatherIgnoredOnFirstEvaluation_MissingLicenseFeature() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);

    application = tempEntity.newApplicationWithParent();
    Policy policy = newSecurityPolicy();
    policy.setPolicyViolationGrandfatheringAllowed(true);
    new PolicyDAO().update(policy);
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);

    // This is the first evaluation. No policy violations should be grandfathered given the license doesn't allow it.
    String scanId1 = simulateReportIsAvailable("report");
    Stage stage1 = new Stage(Stage.ID_BUILD);
    ScanPolicyEvaluatorResults results1 = scanPolicyEvaluator.evaluate(application, scanId1, stage1);
    assertThat(results1.activeViolations).hasSize(36);
  }

  @Test
  public void testEvaluate_GrandfatherContinuesAfterFirstEvaluation_MissingLicenseFeature() throws Exception {
    application = tempEntity.newApplicationWithParent();
    Policy policy = newSecurityPolicy();
    policy.setPolicyViolationGrandfatheringAllowed(true);
    new PolicyDAO().update(policy);
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);

    // This is the first evaluation. All policy violations should be grandfathered.
    String scanId1 = simulateReportIsAvailable("report");
    Stage stage1 = new Stage(Stage.ID_BUILD);
    ScanPolicyEvaluatorResults results1 = scanPolicyEvaluator.evaluate(application, scanId1, stage1);
    assertThat(results1.activeViolations).hasSize(0);
    List<PolicyViolation> inactiveViolations = getInactiveViolations(results1);
    assertThat(inactiveViolations).hasSize(36).allSatisfy(inactiveViolation -> {
      assertThat(inactiveViolation.getGrandfatherTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(inactiveViolation.isWaived()).isFalse();
    });

    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);

    // Evaluate again with license without grandfathering. Policy violations continue to be grandfathered.
    String scanId2 = simulateReportIsAvailable("report");
    ScanPolicyEvaluatorResults results2 = scanPolicyEvaluator.evaluate(application, scanId2, stage1);
    assertThat(results2.activeViolations).hasSize(0);
    inactiveViolations = getInactiveViolations(results2);
    assertThat(inactiveViolations).hasSize(36).allSatisfy(inactiveViolation -> {
      assertThat(inactiveViolation.getGrandfatherTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(inactiveViolation.isWaived()).isFalse();
    });
  }

  @Test
  public void testEvaluate_Results_NotifiableViolations() throws Exception {
    newRelativePopularityPolicy();

    Stage stage = new Stage(Stage.ID_BUILD);

    // 1st evaluation. The report contains one component that triggers the policy, so there is one notifiable violation.
    String scanId = simulateReportIsAvailable("testEvaluate_Results_NotifiableViolations/before");
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage);
    assertThat(results.activeViolations).hasSize(1);
    assertThat(results.notifiableViolations).hasSize(1);

    // 2nd evaluation. Nothing changed, so there are no new violations, so no notifiable violations.
    results = scanPolicyEvaluator.evaluate(application, scanId, stage);
    assertThat(results.activeViolations).hasSize(1);
    assertThat(results.notifiableViolations).isEmpty();

    // 3rd evaluation. The report contains a new component that triggers the policy, so there is one new notifiable
    // violation.
    scanId = simulateReportIsAvailable("testEvaluate_Results_NotifiableViolations/after");
    results = scanPolicyEvaluator.evaluate(application, scanId, stage);
    assertThat(results.activeViolations).hasSize(2);
    assertThat(results.notifiableViolations).hasSize(1);
    assertThat(results.evaluation.getCommitHash()).isNull();
  }

  @Test
  public void testEvaluate_EmitsApplicationEvaluationEvent() throws IOException, InterruptedException {
    handler = new TestEventHandler<>(new CountDownLatch(1));

    newSecurityPolicy();
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("report");

    asyncEventBus.register(handler);

    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    ApplicationEvaluationEvent event = handler.getEvent();
    assertThat(event).isNotNull();
    assertThat(event.stageTypeId).isEqualTo(Stage.ID_BUILD);
    assertThat(event.ownerId).isEqualTo(application.getId());
    assertThat(event.initiator).isEqualTo("testuser");
    assertThat(event.policyEvaluationId).isEqualTo(scanPolicyEvaluatorResults.evaluation.getId());
    assertThat(event.evaluationDate).isEqualTo(scanPolicyEvaluatorResults.evaluation.getTime());
    assertThat(event.affectedComponentCount).isEqualTo(7);
    assertThat(event.criticalComponentCount).isEqualTo(0);
    assertThat(event.severeComponentCount).isEqualTo(7);
    assertThat(event.moderateComponentCount).isEqualTo(0);
    assertThat(event.outcome).isEqualTo(Action.ID_FAIL);
    assertThat(event.commitHash).isEqualTo("testCommitHash");
  }

  @Test
  public void testEvaluate_DoesNot_EmitPolicyAlertEvent_WithoutWebhooks() throws IOException, InterruptedException {
    policyAlertHandler = new FilteringTestEventHandler<>(new CountDownLatch(1), PolicyAlertEvent.class::isInstance);
    newSecurityPolicy();
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    asyncEventBus.register(policyAlertHandler);

    scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(policyAlertHandler.waitForEvent(ofSeconds(5)).isPresent()).isFalse();
  }

  @Test
  public void testEvaluate_EmitsPolicyAlertEvent() throws IOException, InterruptedException {
    policyAlertHandler = new FilteringTestEventHandler<>(new CountDownLatch(1), PolicyAlertEvent.class::isInstance);
    tempEntity.newPolicy(application.getId(), "Test Policy", 10, Action.ID_WARN, Stage.ID_BUILD,
        new Notifications(new WebhookNotification("id", Stage.ID_BUILD)));

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    asyncEventBus.register(policyAlertHandler);

    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(policyAlertHandler.waitForEvent(ofSeconds(5)).isPresent()).isTrue();
    PolicyAlertEvent event = policyAlertHandler.getEvent();
    assertThat(event).isNotNull();
    assertThat(event.applicationEvaluation.stageTypeId).isEqualTo(Stage.ID_BUILD);
    assertThat(event.applicationEvaluation.ownerId).isEqualTo(application.getId());
    assertThat(event.initiator).isEqualTo("testuser");
    assertThat(event.applicationEvaluation.policyEvaluationId).isEqualTo(scanPolicyEvaluatorResults.evaluation.getId());
    assertThat(event.applicationEvaluation.evaluationDate).isEqualTo(scanPolicyEvaluatorResults.evaluation.getTime());
    assertThat(event.applicationEvaluation.affectedComponentCount).isEqualTo(7);
    assertThat(event.applicationEvaluation.criticalComponentCount).isEqualTo(7);
    assertThat(event.applicationEvaluation.severeComponentCount).isEqualTo(0);
    assertThat(event.applicationEvaluation.moderateComponentCount).isEqualTo(0);
    assertThat(event.applicationEvaluation.outcome).isEqualTo(Action.ID_WARN);
    assertThat(event.application.id).isEqualTo(application.getId());
    assertThat(event.application.name).isEqualTo(application.getName());
    assertThat(event.application.organizationId).isEqualTo(organization.getId());
    assertThat(event.policyFacts).hasSize(36);
  }

  @Test
  public void testEvaluate_DeletesPreviousScanFile() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId1 = simulateReportIsAvailable("report");
    File scanFile1 = createScanFile(application, scanId1);
    scanPolicyEvaluator.evaluate(application, scanId1, stage);
    assertThat(scanFile1).isFile();

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    String scanId2 = simulateReportIsAvailable("report");
    File scanFile2 = createScanFile(application, scanId2);
    scanPolicyEvaluator.evaluate(application, scanId2, stage);
    assertThat(scanFile1).doesNotExist();
    assertThat(scanFile2).isFile();
  }

  @Test
  public void testEvaluate_ReEvaluationDoesNotDeleteScanFile() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("report");
    File scanFile = createScanFile(application, scanId);
    scanPolicyEvaluator.evaluate(application, scanId, stage);
    assertThat(scanFile).isFile();

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    scanPolicyEvaluator.evaluate(application, scanId, stage);
    assertThat(scanFile).isFile();
  }

  @Test
  public void testEvaluate_ReEvaluationDeletesPdfReport() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    // Evaluate a scan.
    String scanId = simulateReportIsAvailable("report");
    createScanFile(application, scanId);
    scanPolicyEvaluator.evaluate(application, scanId, stage);

    // Create a fake PDF report.
    File reportFile = insightWork.getReportFile(application.getId(), scanId);
    File pdfReportFile = PdfGenerator.getPdfFile(reportFile);
    pdfReportFile.createNewFile();
    assertThat(pdfReportFile).isFile();

    // Make sure we don't have two evaluations at exactly the same time.
    waitForTimeAdvance();

    // Re-evaluate and check that the PDF report was deleted.
    scanPolicyEvaluator.evaluate(application, scanId, stage);
    assertThat(pdfReportFile).doesNotExist();
  }

  @Test
  public void testEvaluate_DoesNotDeleteScanFileForDifferentStage() throws Exception {
    Stage stage1 = new Stage(Stage.ID_BUILD);

    String scanId1 = simulateReportIsAvailable("report");
    File scanFile1 = createScanFile(application, scanId1);
    scanPolicyEvaluator.evaluate(application, scanId1, stage1);
    assertThat(scanFile1).isFile();

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    Stage stage2 = new Stage(Stage.ID_RELEASE);
    String scanId2 = simulateReportIsAvailable("report");
    File scanFile2 = createScanFile(application, scanId2);
    scanPolicyEvaluator.evaluate(application, scanId2, stage2);
    assertThat(scanFile1).isFile();
    assertThat(scanFile2).isFile();
  }

  @Test
  public void testEvaluate_CanReEvaluatePreviousScan() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId1 = simulateReportIsAvailable("report");
    File scanFile1 = createScanFile(application, scanId1);
    scanPolicyEvaluator.evaluate(application, scanId1, stage);

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    String scanId2 = simulateReportIsAvailable("report");
    createScanFile(application, scanId2);
    scanPolicyEvaluator.evaluate(application, scanId2, stage);

    // The first scan file was deleted by the second policy evaluation.
    // A re-evaluation of the first scan doesn't need the scan so it should succeed.
    assertThat(scanFile1.exists()).isFalse();
    scanPolicyEvaluator.evaluate(application, scanId1, stage);
  }

  @Test
  public void testEvaluate_UpdateFixedViolations() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    Policy policy = newSecurityPolicy();

    ScanPolicyEvaluatorResults results1 = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(results1.allViolations).hasSize(36);

    new PolicyDAO().delete(policy);

    ScanPolicyEvaluatorResults results2 = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(results2.allViolations).hasSize(0);
    List<PolicyViolation> allViolations = new PolicyViolationDAO().getByApplicationId(application.getId());
    assertThat(allViolations).hasSize(36);
    List<PolicyViolation> fixedViolations = allViolations.stream().filter(PolicyViolation::isFixed).collect(toList());
    assertThat(fixedViolations).hasSize(36).allSatisfy(violation -> {
      assertThat(violation.getFixTime()).isEqualTo(results2.evaluation.getTime());
    });
  }

  @Test
  public void testEvaluate_UpdateWaivedViolations() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    Policy policy = newSecurityPolicy();

    ScanPolicyEvaluatorResults results1 = scanPolicyEvaluator.evaluate(application, scanId, stage);
    Date openTime = results1.evaluation.getTime();

    assertThat(results1.activeViolations).hasSize(36);

    PolicyWaiver waiver = tempEntity.newWaiver("f0776db1593e215146d2", policy.getId(), application.getId());

    ScanPolicyEvaluatorResults results2 = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(results2.activeViolations).hasSize(33);
    List<PolicyViolation> waivedViolations = new PolicyViolationDAO()
        .getUnfixedByApplicationIdAndStageId(application.getId(), stage.getStageTypeId()).stream()
        .filter(PolicyViolation::isWaived).collect(toList());
    assertThat(waivedViolations).hasSize(3).allSatisfy(waivedViolation -> {
      assertThat(waivedViolation.getHash()).isEqualTo(waiver.getHash());
      assertThat(waivedViolation.getOpenTime()).isEqualTo(openTime);
      assertThat(waivedViolation.getFixTime()).isNull();
      assertThat(waivedViolation.getWaiveTime()).isEqualTo(results2.evaluation.getTime());
      assertThat(waivedViolation.getPolicyWaiverId()).isEqualTo(waiver.getId());
      assertThat(waivedViolation.getPolicyWaiverComment()).isEqualTo(waiver.getComment());
    });

    new PolicyWaiverDAO().delete(waiver);

    ScanPolicyEvaluatorResults results3 = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(results3.activeViolations).hasSize(36);
    List<PolicyViolation> unfixedViolations = new PolicyViolationDAO()
        .getUnfixedByApplicationIdAndStageId(application.getId(), stage.getStageTypeId());
    assertThat(unfixedViolations.stream().filter(PolicyViolation::isWaived).collect(toList())).hasSize(0);
    List<PolicyViolation> unwaivedViolations = unfixedViolations.stream()
        .filter(violation -> violation.getHash().equals(waiver.getHash())).collect(toList());
    assertThat(unwaivedViolations).hasSize(3).allSatisfy(unwaivedViolation -> {
      assertThat(unwaivedViolation.getHash()).isEqualTo(waiver.getHash());
      assertThat(unwaivedViolation.getOpenTime()).isEqualTo(results3.evaluation.getTime());
      assertThat(unwaivedViolation.getFixTime()).isNull();
      assertThat(unwaivedViolation.getWaiveTime()).isNull();
      assertThat(unwaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(unwaivedViolation.getPolicyWaiverComment()).isNull();
    });

    assertThat(new PolicyViolationDAO().getByApplicationId(application.getId())).hasSize(39);
  }

  @Test
  public void testEvaluate_ApplicationStageComponentCounts() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    scanPolicyEvaluator.evaluate(application, scanId, stage);

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
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }

  private void assertGrandfatheredViolationAttributes(TelemetryData telemetryData,
                                                      Map<String, Object> expectedAttributes)
  {
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose())
        .isEqualTo(TelemetryPurpose.APPLICATION_EVALUATION_GRANDFATHERED_VIOLATION_COUNTS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
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
    String constraintFactsJson = IOUtils.toString(
        getClass().getResource("/ScanPolicyEvaluatorTest/testEvaluate_BeforeAndAfterAddingConditionTriggerData/"
            + "policy-violation-constraint-facts.json"),
        "UTF-8");
    constraintFactsJson = constraintFactsJson.replace("TestConstraintId", policy.getConstraints().get(0).getId());
    PolicyViolation policyViolationBefore = new PolicyViolation(policyEvaluationBefore, policy.getId(),
        policy.getName(), policy.getThreatLevel(), policy.getThreatCategory(), "964cd74171f427720480",
        componentIdentifier, constraintFactsJson, "commons-httpclient-3.1.jar");
    new PolicyViolationDAO().insert(policyViolationBefore);
    assertThat(policyViolationBefore.getOpenTime()).isEqualTo(beforeTime);

    // Evaluate the policy.
    String scanId = simulateReportIsAvailable("testEvaluate_BeforeAndAfterAddingConditionTriggerData/report");
    scanPolicyEvaluator.evaluate(application, scanId, stage);

    // There should be only one policy violation (the existing one).
    List<PolicyViolation> policyViolationsAfter = new PolicyViolationDAO().getByApplicationId(application.getId());
    assertThat(policyViolationsAfter).hasSize(1);
    PolicyViolation policyViolationAfter = policyViolationsAfter.get(0);
    assertThat(policyViolationAfter.getId()).isEqualTo(policyViolationBefore.getId());
    assertThat(policyViolationAfter.getOpenTime()).isEqualTo(beforeTime);
    assertThat(policyViolationAfter.getConstraintFacts()).hasSize(1);
    ConstraintFact constraintFact = policyViolationAfter.getConstraintFacts().get(0);
    assertThat(constraintFact.getConditionFacts()).hasSize(2);
    assertThat(constraintFact.getConditionFacts().get(0).getConditionIndex()).isEqualTo(0);
    assertThat(constraintFact.getConditionFacts().get(1).getConditionIndex()).isEqualTo(1);
  }

  @Test
  public void testEvaluate_GrandfatheringNotConfiguredForAppOrOrg() throws Exception {
    organization.setPolicyViolationGrandfatheringEnabled(null);
    organization.setAllowPolicyViolationGrandfatheringOverride(true);
    new OrganizationDAO().update(organization);
    application.setPolicyViolationGrandfatheringEnabled(null);
    new ApplicationDAO().update(application);

    testEvaluate_GrandfatheredViolations(false, false);
  }

  @Test
  public void testEvaluate_GrandfatheringEnabledForApp_AppCanOverrideGrandfathering() throws Exception {
    organization.setPolicyViolationGrandfatheringEnabled(false);
    organization.setAllowPolicyViolationGrandfatheringOverride(true);
    new OrganizationDAO().update(organization);
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);

    testEvaluate_GrandfatheredViolations(true, true);
  }

  @Test
  public void testEvaluate_GrandfatheringDisabledForApp_AppCanOverrideGrandfathering() throws Exception {
    organization.setPolicyViolationGrandfatheringEnabled(true);
    organization.setAllowPolicyViolationGrandfatheringOverride(true);
    new OrganizationDAO().update(organization);
    application.setPolicyViolationGrandfatheringEnabled(false);
    new ApplicationDAO().update(application);

    testEvaluate_GrandfatheredViolations(false, false);
  }

  @Test
  public void testEvaluate_GrandfatheringEnabledForApp_DisabledForOrg_AppCannotOverrideGrandfathering()
      throws Exception
  {
    organization.setPolicyViolationGrandfatheringEnabled(false);
    organization.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(organization);
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);

    testEvaluate_GrandfatheredViolations(false, false);
  }

  @Test
  public void testEvaluate_GrandfatheringDisabledForApp_DisabledForOrg_AppCannotOverrideGrandfathering()
      throws Exception
  {
    organization.setPolicyViolationGrandfatheringEnabled(false);
    organization.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(organization);
    application.setPolicyViolationGrandfatheringEnabled(false);
    new ApplicationDAO().update(application);

    testEvaluate_GrandfatheredViolations(false, false);
  }

  @Test
  public void testEvaluate_GrandfatheringEnabledForApp_EnabledForOrg_AppCannotOverrideGrandfathering()
      throws Exception
  {
    organization.setPolicyViolationGrandfatheringEnabled(true);
    organization.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(organization);
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);

    testEvaluate_GrandfatheredViolations(true, true);
  }

  @Test
  public void testEvaluate_GrandfatheringDisabledForApp_EnabledForOrg_AppCannotOverrideGrandfathering()
      throws Exception
  {
    organization.setPolicyViolationGrandfatheringEnabled(true);
    organization.setAllowPolicyViolationGrandfatheringOverride(false);
    new OrganizationDAO().update(organization);
    application.setPolicyViolationGrandfatheringEnabled(false);
    new ApplicationDAO().update(application);

    testEvaluate_GrandfatheredViolations(true, true);
  }

  @Test
  public void testEvaluate_Label_DefinedAtAppLevel() throws Exception {
    testEvaluate_Label(false, false);
  }

  @Test
  public void testEvaluate_Label_DefinedAtOrgLevel_AppliedAtOrgLevel() throws Exception {
    testEvaluate_Label(true, true);
  }

  @Test
  public void testEvaluate_Label_DefinedAtOrgLevel_AppliedAtAppLevel() throws Exception {
    testEvaluate_Label(true, false);
  }

  private void testEvaluate_Label(boolean orgLabel, boolean orgComponentLabel) throws Exception {
    String hash = "1249e25aebb15358bedd";
    Label label = tempEntity.newLabel(orgLabel ? application.getOrganizationId() : application.getId(), "red");
    tempEntity.newComponentLabel(orgComponentLabel ? application.getOrganizationId() : application.getId(),
        label.getId(), hash);

    Policy policy = newPolicy(new Condition(LabelConditionType.ID, "is", label.getId()));
    Constraint constraint = policy.getConstraints().get(0);

    Stage stage = new Stage(Stage.ID_BUILD);

    String groupId = "tomcat";
    String artifactId = "tomcat-util";
    String version = "5.5.23";

    String scanId = simulateReportIsAvailable("report");

    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(1);
    assertContainsPolicyViolation(ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version), hash,
        policy, constraint, Action.ID_FAIL, LabelConditionType.ID, scanPolicyEvaluatorResults.activeViolations);
  }

  @Test
  public void testEvaluate_LicenseOverride() throws Exception {
    Constraint constraint1 = new Constraint(null /* constraintId */, "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(LicenseConditionType.ID, "is", "ZPL-2.0");
    constraint1.addCondition(condition1);
    Constraint constraint2 = new Constraint(null /* constraintId */, "Constraint 2", LogicalOperator.AND);
    Condition condition2 = new Condition(LicenseStatusConditionType.ID, "is", "OVERRIDDEN");
    constraint2.addCondition(condition2);

    Policy policy = new Policy(null /* policyId */, "Policy 1");
    policy.setThreatLevel(5);
    policy.addConstraint(constraint1);
    policy.addConstraint(constraint2);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy.setOwnerId(application.getId());
    tempEntity.newPolicy(policy);
    constraint1 = policy.getConstraints().get(0);
    constraint2 = policy.getConstraints().get(1);

    Stage stage = new Stage(Stage.ID_BUILD);

    // Override the license at org level
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("commons-pool", "commons-pool",
        "1.4");
    String hash = "1a667c9d419dc4f185c9";
    tempEntity.newLicenseOverride(application.getOrganizationId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "ZPL-2.0", " My comment");

    String scanId = simulateReportIsAvailable("report");

    // Evaluate policy
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(2);
    assertContainsPolicyViolation(componentIdentifier, hash, policy, constraint1, Action.ID_FAIL,
        LicenseConditionType.ID, scanPolicyEvaluatorResults.activeViolations);
    assertContainsPolicyViolation(componentIdentifier, hash, policy, constraint2, Action.ID_FAIL,
        LicenseStatusConditionType.ID, scanPolicyEvaluatorResults.activeViolations);

    // Override the license at app level. This must supersede the override at org level, so the policy should not
    // trigger any alerts.
    tempEntity.newLicenseOverride(application.getId(), componentIdentifier, LicenseOverrideStatus.ACKNOWLEDGED,
        (String) null /* licenseId */, " My comment");

    scanId = simulateReportIsAvailable("report");

    // Evaluate policy
    scanPolicyEvaluatorResults = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(0);
  }

  @Test
  public void testEvaluate_SecurityVulnerabilityOverride() throws Exception {
    Policy policy = newPolicy(new Condition(SecurityVulnerabilityStatusConditionType.ID, "is", "CONFIRMED"));
    Constraint constraint = policy.getConstraints().get(0);

    Stage stage = new Stage(Stage.ID_BUILD);

    // Override the security vulnerability
    String hash = "494308fc2d433720c778";
    tempEntity.newSecurityVulnerabilityOverride(application.getId(), hash, "cve", "CVE-2009-1524",
        SecurityVulnerabilityOverrideStatus.CONFIRMED, " My comment");

    String scanId = simulateReportIsAvailable("report");

    // Evaluate policy
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(1);
    assertContainsPolicyViolation(ComponentIdentifier.createMavenCoordinates("org.mortbay.jetty", "jetty", "6.1.15"),
        hash, policy, constraint, Action.ID_FAIL, SecurityVulnerabilityStatusConditionType.ID,
        scanPolicyEvaluatorResults.activeViolations);
  }

  @Test
  public void testEvaluate_WaivedPolicyViolations() throws Exception {
    Policy policy = newPolicy(new Condition(LicenseConditionType.ID, "is", "GPL-2.0"));

    String componentHash = "f2e35e4a21f07d25710f";
    PolicyWaiver policyWaiver = tempEntity.newWaiver(componentHash, policy.getId(), application.getId(),
        "Waiver comment here");

    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("report");

    // Evaluate policy
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(scanPolicyEvaluatorResults.allViolations).hasSize(3);
    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(2);

    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    List<PolicyViolation> policyViolations = policyViolationDAO.getUnfixedByApplicationIdAndStageId(application.getId(),
        stage.getStageTypeId());
    assertThat(policyViolations).hasSize(3);
    assertThat(policyViolations).filteredOn(violation -> componentHash.equals(violation.getHash())).hasSize(1)
        .allSatisfy(policyViolation -> {
          assertThat(policyViolation.isWaived()).isTrue();
          assertThat(policyViolation.getPolicyWaiverId()).isEqualTo(policyWaiver.getId());
          assertThat(policyViolation.getPolicyWaiverComment()).isEqualTo(policyWaiver.getComment());
        });
    assertThat(policyViolations).filteredOn(violation -> !componentHash.equals(violation.getHash()))
        .allSatisfy(policyViolation -> {
          assertThat(policyViolation.isWaived()).isFalse();
        });
  }

  @Test
  public void testEvaluate_InvalidStage() throws Exception {
    assertThatExceptionOfType(InvalidStageException.class).isThrownBy(() -> {
      scanPolicyEvaluator.evaluate(application, "scanid", new Stage("foobar"));
    }).withMessage("Invalid stage id=foobar");
  }

  @Test
  public void testEvaluate_MissingReport() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      scanPolicyEvaluator.evaluate(application, "scanId", new Stage(Stage.ID_BUILD));
    }).withMessage("Could not download the report for scan ID scanId");

    PolicyEvaluation eval = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(application.getId(),
        Stage.ID_BUILD);
    assertThat(eval).isNull();
  }

  @Test
  public void testEvaluate_ErrorReport() throws Exception {
    String scanId = simulateReportIsAvailable("empty_report");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD));
    }).withMessage("Unable to evaluate policy, the scan " + scanId + " could not be processed.");

    PolicyEvaluation eval = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(application.getId(),
        Stage.ID_BUILD);
    assertThat(eval).isNull();
  }

  @Test
  public void testEvaluate_ManuallyIdentifiedComponent() throws Exception {
    Policy policy = newPolicy(new Condition(MatchStateConditionType.ID, "is", "exact"));
    Constraint constraint = policy.getConstraints().get(0);

    Stage stage = new Stage(Stage.ID_BUILD);

    String hash = "5801a1a27a36f88e2089";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("G", "A", "V");
    tempEntity.newClaimedComponent(hash, componentIdentifier);

    String scanId = simulateReportIsAvailable("ManuallyIdentifiedComponent/report");

    // Evaluate policy
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(1);
    assertContainsPolicyViolation(componentIdentifier, hash, policy, constraint, Action.ID_FAIL,
        MatchStateConditionType.ID, scanPolicyEvaluatorResults.activeViolations);
  }

  @Test
  public void testEvaluate_MultiLicense() throws Exception {
    Policy policy = newPolicy(new Condition(LicenseConditionType.ID, "is", "GPL-2.0"));
    Constraint constraint = policy.getConstraints().get(0);

    String hash = "f2e35e4a21f07d25710f";

    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("report");

    // Evaluate policy
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(3);
    assertContainsPolicyViolation(ComponentIdentifier.createMavenCoordinates("org.webjars", "select2", "3.2"), hash,
        policy, constraint, Action.ID_FAIL, LicenseConditionType.ID, scanPolicyEvaluatorResults.activeViolations);
  }

  @Test
  public void testEvaluate_MultipleMatchesForSameGAV() throws Exception {
    Constraint constraintLicense = new Constraint(null /* constraintId */, "Constraint License", LogicalOperator.AND);
    Condition condition1 = new Condition(LicenseConditionType.ID, "is", "UNSPECIFIED");
    constraintLicense.addCondition(condition1);
    Constraint constraintSV = new Constraint(null /* constraintId */, "Constraint SV", LogicalOperator.AND);
    Condition condition2 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    constraintSV.addCondition(condition2);

    Policy policy = new Policy(null /* policyId */, "Policy 1");
    policy.setThreatLevel(5);
    policy.addConstraint(constraintLicense);
    policy.addConstraint(constraintSV);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy.setOwnerId(application.getId());
    tempEntity.newPolicy(policy);
    constraintLicense = policy.getConstraints().get(0);
    constraintSV = policy.getConstraints().get(1);

    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("MultipleMatchesForSameGAV/report");

    // Evaluate policy
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(9);
    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("tomcat", "tomcat-util", "5.0.28");
    String hashExact = "3102cdd0edd5a05afe00";
    String hashSimilar1 = "d29a75f9056e0b040f09";
    assertContainsPolicyViolation(componentIdentifier, hashExact,
        policy, constraintLicense, Action.ID_FAIL, LicenseConditionType.ID,
        scanPolicyEvaluatorResults.activeViolations);
    assertContainsPolicyViolation(componentIdentifier, hashExact, policy, constraintSV, Action.ID_FAIL,
        SecurityVulnerabilitySeverityConditionType.ID, scanPolicyEvaluatorResults.activeViolations);
    assertContainsPolicyViolation(componentIdentifier, hashSimilar1, policy, constraintLicense, Action.ID_FAIL,
        LicenseConditionType.ID, scanPolicyEvaluatorResults.activeViolations);

    String hashSimilar2 = "707df42012875442b9df";
    // Verify that the SVs are associated with components by hash, not by component identifier.
    // If SVs were associated with components by component identifier, this component would have a policy violation for
    // an SV because it has the same identifier as expectedComponentExact, which has a violation for an SV.
    assertNotContainsPolicyViolation(componentIdentifier, hashSimilar1, policy, constraintSV, Action.ID_FAIL,
        SecurityVulnerabilitySeverityConditionType.ID, scanPolicyEvaluatorResults.activeViolations);
    assertContainsPolicyViolation(componentIdentifier, hashSimilar2, policy, constraintLicense, Action.ID_FAIL,
        LicenseConditionType.ID, scanPolicyEvaluatorResults.activeViolations);
    // Verify that the SVs are associated with components by hash, not by component identifier.
    // If SVs were associated with components by component identifier, this component would have a policy violation for
    // an SV because it has the same identifier as expectedComponentExact, which has a violation for an SV.
    assertNotContainsPolicyViolation(componentIdentifier, hashSimilar2, policy, constraintSV, Action.ID_FAIL,
        SecurityVulnerabilitySeverityConditionType.ID, scanPolicyEvaluatorResults.activeViolations);
  }

  @Test
  public void testEvaluate_OneStage() throws Exception {
    Policy policy = newPolicyOR(new Condition(CoordinatesConditionType.ID, "match", "maven:tomcat:tomcat-util:5.5.23"),
        new Condition(CoordinatesConditionType.ID, "match", "maven:commons-pool:commons-pool:1.4"));

    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("report");

    // Evaluate policy
    scanPolicyEvaluator.evaluate(application, scanId, stage);
    PolicyEvaluation policyEvaluation1 = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(application.getId(),
        stage.getStageTypeId());
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    List<PolicyViolation> policyViolations1 = policyViolationDAO.getActiveByApplicationIdAndStageId(application.getId(),
        stage.getStageTypeId());
    assertThat(policyViolations1).hasSize(2);
    policyViolations1 = sort(policyViolations1);
    assertThat(policyViolations1.get(0).getComponentIdentifier().get(ComponentIdentifier.MAVEN_GROUP_ID))
        .isEqualTo("tomcat");
    assertThat(policyViolations1.get(0).getOpenTime()).isEqualTo(policyEvaluation1.getTime());
    assertThat(policyViolations1.get(1).getComponentIdentifier().get(ComponentIdentifier.MAVEN_GROUP_ID))
        .isEqualTo("commons-pool");
    assertThat(policyViolations1.get(1).getOpenTime()).isEqualTo(policyEvaluation1.getTime());

    // Change one of the policy conditions and re-evaluate the policy.
    // This should cause a policy violation to be cleared and a new policy violation to appear.
    policy.getConstraints().get(0).getConditions().get(0).setValue("maven:commons-dbcp:commons-dbcp:1.4");
    new PolicyDAO().update(policy);
    // Evaluate policy again for the same scan
    scanPolicyEvaluator.evaluate(application, scanId, stage);
    PolicyEvaluation policyEvaluation2 = new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(application.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation1.getId()).isNotEqualTo(policyEvaluation2.getId());
    List<PolicyViolation> policyViolations2 = policyViolationDAO.getActiveByApplicationIdAndStageId(application.getId(),
        stage.getStageTypeId());
    assertThat(policyViolations2).hasSize(2);
    policyViolations2 = sort(policyViolations2);
    assertThat(policyViolations2.get(0).getComponentIdentifier().get(ComponentIdentifier.MAVEN_GROUP_ID))
        .isEqualTo("commons-pool");
    assertThat(policyViolations2.get(0).getOpenTime()).isEqualTo(policyEvaluation1.getTime());
    assertThat(policyViolations2.get(1).getComponentIdentifier().get(ComponentIdentifier.MAVEN_GROUP_ID))
        .isEqualTo("commons-dbcp");
    assertThat(policyViolations2.get(1).getOpenTime()).isEqualTo(policyEvaluation2.getTime());
  }

  @Test
  public void testEvaluate_TwoStages() throws Exception {
    newPolicy(new Condition(CoordinatesConditionType.ID, "match", "maven:commons-pool:commons-pool:1.4"));

    // Evaluate policy for the Build stage
    String scanBuildId = simulateReportIsAvailable("report");
    scanPolicyEvaluator.evaluate(application, scanBuildId, new Stage(Stage.ID_BUILD));
    PolicyEvaluation policyEvaluationBuild = new PolicyEvaluationDAO()
        .getLastByApplicationIdAndStageId(application.getId(), Stage.ID_BUILD);
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    List<PolicyViolation> policyViolationsBuild = policyViolationDAO
        .getActiveByApplicationIdAndStageId(application.getId(), Stage.ID_BUILD);
    assertThat(policyViolationsBuild).hasSize(1);
    assertThat(policyViolationsBuild.get(0).getComponentIdentifier().get(ComponentIdentifier.MAVEN_GROUP_ID))
        .isEqualTo("commons-pool");
    assertThat(policyViolationsBuild.get(0).getOpenTime()).isEqualTo(policyEvaluationBuild.getTime());

    // Evaluate policy for the Release stage
    String scanReleaseId = simulateReportIsAvailable("report");
    scanPolicyEvaluator.evaluate(application, scanReleaseId, new Stage(Stage.ID_RELEASE));
    PolicyEvaluation policyEvaluationRelease = new PolicyEvaluationDAO()
        .getLastByApplicationIdAndStageId(application.getId(), Stage.ID_RELEASE);
    List<PolicyViolation> policyViolationsRelease = policyViolationDAO
        .getActiveByApplicationIdAndStageId(application.getId(), Stage.ID_RELEASE);
    assertThat(policyViolationsRelease).hasSize(1);
    assertThat(policyViolationsRelease.get(0).getComponentIdentifier().get(ComponentIdentifier.MAVEN_GROUP_ID))
        .isEqualTo("commons-pool");
    assertThat(policyViolationsRelease.get(0).getOpenTime()).isEqualTo(policyEvaluationRelease.getTime());

    policyViolationsBuild = policyViolationDAO.getActiveByApplicationIdAndStageId(application.getId(), Stage.ID_BUILD);
    assertThat(policyViolationsBuild).hasSize(1);
    assertThat(policyViolationsBuild.get(0).getOpenTime()).isEqualTo(policyEvaluationBuild.getTime());
  }

  @Test
  public void testEvaluate_ReEvaluateObsoleteScan() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    // Evaluate policy for scanId1
    String scanId1 = simulateReportIsAvailable("report");
    scanPolicyEvaluator.evaluate(application, scanId1, stage);
    assertPolicyEvaluation(scanId1, false /* isReevaluation */);

    // Make sure we don't have two evaluations at exactly the same time
    Thread.sleep(1);

    // Evaluate policy for scanId2
    String scanId2 = simulateReportIsAvailable("report");
    scanPolicyEvaluator.evaluate(application, scanId2, stage);
    assertPolicyEvaluation(scanId2, false /* isReevaluation */);

    // Evaluate policy again for scanId1
    scanPolicyEvaluator.evaluate(application, scanId1, stage);
    assertPolicyEvaluation(scanId1, true /* isReevaluation */, true /* isForObsoleteScan */);
  }

  @Test
  public void testEvaluate_PersistApplicationComponents() throws Exception {
    Stage stage1 = new Stage(Stage.ID_BUILD);
    Stage stage2 = new Stage(Stage.ID_RELEASE);

    // Evaluate policy
    ApplicationComponentDAO appComponentDAO = new ApplicationComponentDAO();
    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(application.getId(), stage1.getStageTypeId()))
        .isEmpty();
    String scanId1 = simulateReportIsAvailable("PersistApplicationComponents/report1");
    scanPolicyEvaluator.evaluate(application, scanId1, stage1);
    List<ApplicationComponent> appComponents1 = appComponentDAO.getByApplicationIdAndStageTypeId(application.getId(),
        stage1.getStageTypeId());
    assertThat(appComponents1).hasSize(1);
    ApplicationComponent appComponent1 = appComponents1.get(0);
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    PolicyEvaluation policyEvaluation1 = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(),
        stage1.getStageTypeId());
    ComponentIdentifier commonsDbcpComponentIdentifier = ComponentIdentifier.createMavenCoordinates("commons-dbcp",
        "commons-dbcp", "1.4");
    assertApplicationComponent(commonsDbcpComponentIdentifier, policyEvaluation1.getTime(), appComponent1);

    // Evaluate policy for a different stage. It should not touch the app<->component assocs for the first stage.
    assertThat(appComponentDAO.getByApplicationIdAndStageTypeId(application.getId(), stage2.getStageTypeId()))
        .isEmpty();
    String scanId2 = simulateReportIsAvailable("PersistApplicationComponents/report2");
    scanPolicyEvaluator.evaluate(application, scanId2, stage2);
    List<ApplicationComponent> appComponents2 = appComponentDAO.getByApplicationIdAndStageTypeId(application.getId(),
        stage2.getStageTypeId());
    assertThat(appComponents2).hasSize(1);
    ApplicationComponent appComponent2 = appComponents2.get(0);
    PolicyEvaluation policyEvaluation2 = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(),
        stage2.getStageTypeId());
    ComponentIdentifier geronimoTomcatComponentIdentifier = ComponentIdentifier.createMavenCoordinates("geronimo",
        "geronimo-tomcat", "1.0");
    assertApplicationComponent(geronimoTomcatComponentIdentifier, policyEvaluation2.getTime(), appComponent2);
    appComponents1 = appComponentDAO.getByApplicationIdAndStageTypeId(application.getId(), stage1.getStageTypeId());
    assertThat(appComponents1).hasSize(1);
    assertApplicationComponent(commonsDbcpComponentIdentifier, policyEvaluation1.getTime(), appComponents1.get(0));
    assertThat(appComponents1.get(0).getId()).isEqualTo(appComponent1.getId());

    // Evaluate again for the first stage. It should replace the app<->component assocs for the first stage and it
    // should not touch the app<->component assocs for the second stage.
    String scanId3 = simulateReportIsAvailable("PersistApplicationComponents/report3");
    scanPolicyEvaluator.evaluate(application, scanId3, stage1);
    List<ApplicationComponent> appComponents3 = appComponentDAO.getByApplicationIdAndStageTypeId(application.getId(),
        stage1.getStageTypeId());
    assertThat(appComponents3).hasSize(1);
    ApplicationComponent appComponent3 = appComponents3.get(0);
    policyEvaluation1 = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(),
        stage1.getStageTypeId());
    ComponentIdentifier tomcatUtilCOmponentIdentifier = ComponentIdentifier.createMavenCoordinates("tomcat",
        "tomcat-util", "5.5.23");
    assertApplicationComponent(tomcatUtilCOmponentIdentifier, policyEvaluation1.getTime(), appComponent3);
    appComponents2 = appComponentDAO.getByApplicationIdAndStageTypeId(application.getId(), stage2.getStageTypeId());
    assertThat(appComponents2).hasSize(1);
    assertApplicationComponent(geronimoTomcatComponentIdentifier, policyEvaluation2.getTime(), appComponents2.get(0));
    assertThat(appComponents2.get(0).getId()).isEqualTo(appComponent2.getId());
  }

  @Test
  public void testEvaluate_UpdatesReportFiles() throws Exception {
    // The policy will cause three policy violations.
    Policy policy = newPolicy(new Condition(LicenseConditionType.ID, "is", "GPL-2.0"));
    // The waiver will waive one policy violation, leaving two active policy violations.
    tempEntity.newWaiver("f2e35e4a21f07d25710f", policy.getId(), application.getId(), "Waiver comment here");
    String scanId = simulateReportIsAvailable("report");

    // Evaluate policy
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = scanPolicyEvaluator
        .evaluate(application, scanId, new Stage(Stage.ID_BUILD));

    assertThat(scanPolicyEvaluatorResults.allViolations).hasSize(3);
    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(2);

    File reportFile = insightWork.getReportFile(application.getId(), scanId);
    // Verify the policyalerts.json report file
    ReportEntry policyAlertsReportEntry = Report.getEntry(reportFile, ScanPolicyEvaluator.POLICY_ALERTS_FILENAME);
    List<PolicyAlert> policyAlerts = Arrays.asList(JsonUtils.parse(policyAlertsReportEntry.buf, PolicyAlert[].class));
    assertThat(policyAlerts).extracting(PolicyAlert::getTrigger) //
        .flatExtracting(PolicyFact::getComponentFacts) //
        .extracting(ComponentFact::getHash) //
        .containsExactlyInAnyOrder("3e1470773021fde54f51", "e93e551d738e9f4d1aae");
    assertThat(policyAlerts).flatExtracting(PolicyAlert::getActions).isNotEmpty();
    // Verify the policythreats.json report file
    ReportEntry policyThreatsReportEntry = Report.getEntry(reportFile, ScanPolicyEvaluator.POLICY_THREATS_FILENAME);
    PolicyThreats policyThreats = JsonUtils.parse(policyThreatsReportEntry.buf, PolicyThreats.class);
    assertThat(policyThreats.aaData) //
        .extracting(component -> component.hash) //
        .containsExactlyInAnyOrder("3e1470773021fde54f51", "e93e551d738e9f4d1aae", "f2e35e4a21f07d25710f");
    // Verify the data.json report file
    ReportEntry dataReportEntry = Report.getEntry(reportFile, Report.DATA_JSON_FILENAME);
    ObjectNode data = JsonUtils.parse(dataReportEntry.buf);
    assertThat(data.get("policyCounts").toString()).isEqualTo("[1,0,0,0,0,2,0,0,0,0,0]");
    assertThat(data.get("policyComponentCount").asInt()).isEqualTo(2);
    assertThat(data.get("grandfatheredPolicyViolationCount").asInt()).isEqualTo(0);
  }

  @Test
  public void testEvaluate_UpdatesReportFiles_GrandfatheredViolations() throws Exception {
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);
    // The policy will cause three policy violations.
    Policy policy = newPolicy(new Condition(LicenseConditionType.ID, "is", "GPL-2.0"));
    policy.setPolicyViolationGrandfatheringAllowed(true);
    new PolicyDAO().update(policy);
    String scanId = simulateReportIsAvailable("report");

    // Evaluate policy
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD));

    assertThat(scanPolicyEvaluatorResults.allViolations).hasSize(3);
    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(0);

    File reportFile = insightWork.getReportFile(application.getId(), scanId);
    // Verify the policyalerts.json report file
    ReportEntry policyAlertsReportEntry = Report.getEntry(reportFile, ScanPolicyEvaluator.POLICY_ALERTS_FILENAME);
    List<PolicyAlert> policyAlerts = Arrays.asList(JsonUtils.parse(policyAlertsReportEntry.buf, PolicyAlert[].class));
    assertThat(policyAlerts).isEmpty();
    // Verify the policythreats.json report file
    ReportEntry policyThreatsReportEntry = Report.getEntry(reportFile, ScanPolicyEvaluator.POLICY_THREATS_FILENAME);
    PolicyThreats policyThreats = JsonUtils.parse(policyThreatsReportEntry.buf, PolicyThreats.class);
    assertThat(policyThreats.aaData) //
        .extracting(component -> component.hash) //
        .containsExactlyInAnyOrder("3e1470773021fde54f51", "e93e551d738e9f4d1aae", "f2e35e4a21f07d25710f");
    // Verify the data.json report file
    ReportEntry dataReportEntry = Report.getEntry(reportFile, Report.DATA_JSON_FILENAME);
    ObjectNode data = JsonUtils.parse(dataReportEntry.buf);
    // All three policy violations are grandfathered and so each of the three components has a policyThreatLevel of 0
    assertThat(data.get("policyCounts").toString()).isEqualTo("[3,0,0,0,0,0,0,0,0,0,0]");
    // Since each component has a policyThreatLevel of 0, there are 0 affected components
    assertThat(data.get("policyComponentCount").asInt()).isEqualTo(0);
    assertThat(data.get("grandfatheredPolicyViolationCount").asInt()).isEqualTo(3);
  }

  @Test
  public void testEvaluate_UpdatesReportFiles_MissingLicenseFeature() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.ENFORCEMENT);
    // The policy will cause three policy violations.
    Policy policy = newPolicy(new Condition(LicenseConditionType.ID, "is", "GPL-2.0"));
    // The waiver will waive one policy violation, leaving two active policy violations.
    tempEntity.newWaiver("f2e35e4a21f07d25710f", policy.getId(), application.getId(), "Waiver comment here");
    String scanId = simulateReportIsAvailable("report");

    // Evaluate policy
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = scanPolicyEvaluator
        .evaluate(application, scanId, new Stage(Stage.ID_BUILD));

    assertThat(scanPolicyEvaluatorResults.allViolations).hasSize(3);
    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(2);

    File reportFile = insightWork.getReportFile(application.getId(), scanId);
    // Verify the policyalerts.json report file
    ReportEntry policyAlertsReportEntry = Report.getEntry(reportFile, ScanPolicyEvaluator.POLICY_ALERTS_FILENAME);
    List<PolicyAlert> policyAlerts = Arrays.asList(JsonUtils.parse(policyAlertsReportEntry.buf, PolicyAlert[].class));
    assertThat(policyAlerts).extracting(PolicyAlert::getTrigger) //
        .flatExtracting(PolicyFact::getComponentFacts) //
        .extracting(ComponentFact::getHash) //
        .containsExactlyInAnyOrder("3e1470773021fde54f51", "e93e551d738e9f4d1aae");
    assertThat(policyAlerts).flatExtracting(PolicyAlert::getActions).isEmpty();
    // Verify the policythreats.json report file
    ReportEntry policyThreatsReportEntry = Report.getEntry(reportFile, ScanPolicyEvaluator.POLICY_THREATS_FILENAME);
    PolicyThreats policyThreats = JsonUtils.parse(policyThreatsReportEntry.buf, PolicyThreats.class);
    assertThat(policyThreats.aaData) //
        .extracting(component -> component.hash) //
        .containsExactlyInAnyOrder("3e1470773021fde54f51", "e93e551d738e9f4d1aae", "f2e35e4a21f07d25710f");
  }

  @Test
  public void testEvaluate() throws Exception {
    newPolicy(new Condition(CoordinatesConditionType.ID, "match", "maven:commons-pool:commons-pool:1.4"));

    String scanBuildId = simulateReportIsAvailable("report");
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator
        .evaluate(application, scanBuildId, new Stage(Stage.ID_BUILD));
    PolicyEvaluationResult evaluationResult = scanPolicyEvaluator.createPolicyEvaluationResult(results.evaluation,
        results.allViolations, true);
    assertThat(evaluationResult.getAlerts()).hasSize(1);
    PolicyAlert alert = evaluationResult.getAlerts().get(0);
    assertThat(alert.getActions().get(0).getActionTypeId()).isEqualTo(Action.ID_FAIL);
  }

  @Test
  public void testEvaluate_MissingLicenseFeature() throws Exception {
    newPolicy(new Condition(CoordinatesConditionType.ID, "match", "maven:commons-pool:commons-pool:1.4"));

    testProductLicense.setMissingFeatures(LicensedFeature.ENFORCEMENT);
    String scanBuildId = simulateReportIsAvailable("report");
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator
        .evaluate(application, scanBuildId, new Stage(Stage.ID_BUILD));
    PolicyEvaluationResult evaluationResult = scanPolicyEvaluator.createPolicyEvaluationResult(results.evaluation,
        results.allViolations, true);
    assertThat(evaluationResult.getAlerts()).hasSize(1);
    PolicyAlert alert = evaluationResult.getAlerts().get(0);
    assertThat(alert.getActions()).isEmpty();
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_CreateAndFixPolicyViolations() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    Policy policy = newSecurityPolicy();

    // First evaluation, all policy violations are new, all logged
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, results.evaluation.getTime(), results.allViolations);
    policyViolationLoggerOutput.clear();

    // Second evaluation, all policy violations are the same, none logged
    scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertPolicyViolationLogDTOs(0);

    new PolicyDAO().delete(policy);
    // Third evaluation, all policy violations are fixed, all logged
    results = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertPolicyViolationsLogged(PolicyViolationLogEvent.FIX, results.evaluation.getTime(),
        new PolicyViolationDAO().getByApplicationId(application.getId()));
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_WaiveAndUnwaivePolicyViolations() throws Exception {
    // Create two policies that will cause policy violations and waive one policy.
    Policy securityPolicy = newSecurityPolicy();
    tempEntity.newWaiver(securityPolicy.getId(), application.getId());
    Policy licensePolicy = newPolicy(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));

    // Evaluate policies. There should be two policy violations, one active and one waived.
    // Both violations should have a CREATE event logged. Only one should have a WAIVE event.
    String scanId = simulateReportIsAvailable("testEvaluate_PolicyViolationLogger_WaivePolicyViolations/report");
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD));
    assertThat(results.allViolations).hasSize(2);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, results.evaluation.getTime(), results.allViolations);
    List<PolicyViolation> waivedViolations =
        filterPolicyViolationsByPolicyId(results.allViolations, securityPolicy.getId());
    assertThat(waivedViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.WAIVE, results.evaluation.getTime(), waivedViolations);

    policyViolationLoggerOutput.clear();

    // Waive the other policy and evaluate policies again.
    // There should be two waived policy violations, one already waived and one newly waived.
    // Only one should have a WAIVE event logged.
    PolicyWaiver licensePolicyWaiver = tempEntity.newWaiver(licensePolicy.getId(), application.getId());
    scanId = simulateReportIsAvailable("testEvaluate_PolicyViolationLogger_WaivePolicyViolations/report");
    results = scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD));
    assertThat(results.allViolations).hasSize(2);
    List<PolicyViolation> newWaivedViolations =
        filterPolicyViolationsByPolicyId(results.allViolations, licensePolicy.getId());
    assertThat(newWaivedViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.WAIVE, results.evaluation.getTime(), newWaivedViolations);

    policyViolationLoggerOutput.clear();

    // Remove the waiver for one of the policies and evaluate policies again.
    // There should be an active policy violation again. The unwaived violation should have an UNWAIVE event logged.
    new PolicyWaiverDAO().delete(licensePolicyWaiver);
    scanId = simulateReportIsAvailable("testEvaluate_PolicyViolationLogger_WaivePolicyViolations/report");
    results = scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD));
    assertThat(results.allViolations).hasSize(2);
    assertThat(results.activeViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.UNWAIVE, results.evaluation.getTime(),
        Collections.singletonList(results.activeViolations.get(0)));
  }

  private List<PolicyViolation> filterPolicyViolationsByPolicyId(List<PolicyViolation> policyViolations,
                                                                 String policyId)
  {
    return policyViolations.stream().filter(policyViolation -> policyViolation.getPolicyId().equals(policyId))
        .collect(Collectors.toList());
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_GrandfatherPolicyViolations() throws Exception {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());
    application.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(application);
    // Create two policies that will cause policy violations and allow grandfathering for one policy.
    Policy securityPolicy = newSecurityPolicy();
    securityPolicy.setPolicyViolationGrandfatheringAllowed(true);
    new PolicyDAO().update(securityPolicy);
    newPolicy(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));

    // Evaluate policies. There should be two policy violations, one active and one grandfathered.
    // Both violations should have a CREATE event logged. Only one should have a GRANDFATHER event.
    String scanId = simulateReportIsAvailable(
        "testEvaluate_PolicyViolationLogger_GrandfatherAndUngrandfatherPolicyViolations/report");
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD));
    assertThat(results.allViolations).hasSize(2);
    assertThat(results.activeViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, results.evaluation.getTime(), results.allViolations);
    List<PolicyViolation> grandfatheredViolations =
        filterPolicyViolationsByPolicyId(results.allViolations, securityPolicy.getId());
    assertThat(grandfatheredViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.GRANDFATHER, results.evaluation.getTime(),
        grandfatheredViolations);
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_DoesNotLogPolicyViolationsForNonLatestScan() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    scanPolicyEvaluator.evaluate(application, scanId, stage);
    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();
    scanPolicyEvaluator.evaluate(application, simulateReportIsAvailable("report"), stage);
    newSecurityPolicy();

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage);

    assertThat(results.allViolations).isNotEmpty();
    assertPolicyViolationLogDTOs(0);
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_LogsPolicyConditionTriggersForAllConditionTypes() throws Exception {
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newSecurityVulnerabilityOverride(application.getId(), "964cd74171f427720480", "sonatype",
        "sonatype-2007-0004", SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);

    Condition ageCondition = new Condition(AgeInDaysConditionType.ID, "older than", "1");
    Condition coordinatesCondition = new Condition(CoordinatesConditionType.ID, "match", "maven:*:*:*:*:*");
    Condition identificationSourceCondition = new Condition(IdentificationSourceConditionType.ID, "is", "Sonatype");
    Condition labelCondition = new Condition(LabelConditionType.ID, "is not", label.getId());
    Condition licenseCondition = new Condition(LicenseConditionType.ID, "is not", "Beerware");
    Condition licenseStatusCondition = new Condition(LicenseStatusConditionType.ID, "is not", "ACKNOWLEDGED");
    Condition licenseThreatGroupCondition = new Condition(LicenseThreatGroupConditionType.ID, "is not",
        licenseThreatGroup.getId());
    Condition licenseThreatGroupLevelCondition = new Condition(LicenseThreatGroupLevelConditionType.ID, "<=", "0");
    Condition matchStateCondition = new Condition(MatchStateConditionType.ID, "is not", "unknown");
    Condition proprietaryCondition = new Condition(ProprietaryConditionType.ID, "is false");
    Condition relativePopularityCondition = new Condition(RelativePopularityConditionType.ID, ">=", "0");
    Condition securityVulnerabilitySeverityCondition = new Condition(SecurityVulnerabilitySeverityConditionType.ID,
        ">=", "7");
    Condition securityVulnerabilityStatusCondition = new Condition(SecurityVulnerabilityStatusConditionType.ID, "is",
        "ACKNOWLEDGED");
    Condition packageUrlCondition = new Condition(PackageUrlConditionType.ID, "matches", "pkg:maven/*/*@*");
    Condition componentCategoryCondition = new Condition(ComponentCategoryConditionType.ID, "is not", "113");
    Condition hygieneCondition = new Condition(HygieneRatingConditionType.ID, "is not", "1");
    Condition dataSourceCondition = new Condition(DataSourceConditionType.ID, "has support for", "identity");
    Condition dependencyCondition = new Condition(DependencyTypeConditionType.ID, "is", "direct");
    Condition vulnerabilityCategoryCondition =
        new Condition(SecurityVulnerabilityCategoryConditionType.ID, "is", "malicious_code");

    List<Condition> conditions = Arrays.asList(ageCondition, coordinatesCondition, identificationSourceCondition,
        labelCondition, licenseCondition, licenseStatusCondition, licenseThreatGroupCondition,
        licenseThreatGroupLevelCondition, matchStateCondition, proprietaryCondition, relativePopularityCondition,
        securityVulnerabilitySeverityCondition, securityVulnerabilityStatusCondition, packageUrlCondition,
        componentCategoryCondition, hygieneCondition, dataSourceCondition, dependencyCondition,
        vulnerabilityCategoryCondition);
    ConditionTypes.enableConditionType(ConditionTypes.HygieneRatingConditionType);
    try {
      Set<String> expectedConditionTypeIds = ConditionTypes.getAll().stream().map(ConditionType::getId)
          .collect(Collectors.toSet());
      assertThat(conditions.stream().map(Condition::getConditionTypeId).collect(toSet()))
          .isEqualTo(expectedConditionTypeIds);

      Constraint constraint = new Constraint(null, "constraintName", LogicalOperator.OR);
      constraint.setConditions(conditions);

      tempEntity.newPolicy("policyName", constraint);

      ScanPolicyEvaluatorResults results = scanPolicyEvaluator
          .evaluate(application, simulateReportIsAvailable("LogPolicyViolationPolicyConditionTriggers"),
              new Stage(Stage.ID_BUILD));

      assertThat(results.allViolations).hasSize(conditions.size());
      assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, results.evaluation.getTime(), results.allViolations);
    }
    finally {
      ConditionTypes.disableConditionType(ConditionTypes.HygieneRatingConditionType);
    }
  }

  @Test
  public void testEvaluate_PolicyViolationTelemetryCollector_ValidTelemetryForConditionTypes() throws Exception {
    // Setup hygiene conditions and one "invalid" condition.
    Condition packageUrlCondition = new Condition(PackageUrlConditionType.ID, "matches", "pkg:maven/*/*@*");
    Condition componentCategoryCondition = new Condition(ComponentCategoryConditionType.ID, "is not", "113");
    Condition hygieneCondition = new Condition(HygieneRatingConditionType.ID, "is not", "1");
    Condition dependencyCondition = new Condition(DependencyTypeConditionType.ID, "is", "direct");
    Condition vulnerabilityCategoryCondition =
        new Condition(SecurityVulnerabilityCategoryConditionType.ID, "is", "malicious_code");

    List<Condition> conditions = Arrays.asList(packageUrlCondition, componentCategoryCondition, hygieneCondition,
        dependencyCondition, vulnerabilityCategoryCondition);

    Constraint constraint = new Constraint(null, "constraintName", LogicalOperator.OR);
    constraint.setConditions(conditions);

    tempEntity.newPolicy("policyName", constraint);

    String scanId = simulateReportIsAvailable("LogPolicyViolationPolicyConditionTriggers");
    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    clearInvocations(mockTelemetrySender);

    // When evaluate policies
    scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD));

    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getValue();
    // excluding the packageUrl condition, which is not included in telemetry
    assertThat(telemetryDataList).hasSize(conditions.size() - 1);

    boolean hasHygieneViolation = telemetryDataList.stream().anyMatch(telemetryData ->
        telemetryData.getAttributes().get(
            PolicyViolationTelemetryCollector.CONDITION_TYPE).equals(HygieneRatingConditionType.ID));

    boolean hasComponentCategoryViolation = telemetryDataList.stream().anyMatch(telemetryData ->
        telemetryData.getAttributes().get(
            PolicyViolationTelemetryCollector.CONDITION_TYPE).equals(ComponentCategoryConditionType.ID));

    boolean hasDependencyTypeViolation = telemetryDataList.stream().anyMatch(telemetryData ->
        telemetryData.getAttributes().get(
            PolicyViolationTelemetryCollector.CONDITION_TYPE).equals(DependencyTypeConditionType.ID));

    boolean hasSVCategoryTypeViolation = telemetryDataList.stream().anyMatch(telemetryData ->
        telemetryData.getAttributes().get(
            PolicyViolationTelemetryCollector.CONDITION_TYPE).equals(SecurityVulnerabilityCategoryConditionType.ID));

    assertThat(hasHygieneViolation).isTrue();
    assertThat(hasComponentCategoryViolation).isTrue();
    assertThat(hasDependencyTypeViolation).isTrue();
    assertThat(hasSVCategoryTypeViolation).isTrue();
    clearInvocations(mockTelemetrySender);
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_LogsPolicyConditionTriggersForMultipleConstraintsConditions()
      throws Exception
  {
    Condition ageCondition1 = new Condition(AgeInDaysConditionType.ID, "older than", "1");
    Condition ageCondition2 = new Condition(AgeInDaysConditionType.ID, "younger than", "999999");
    Constraint constraint1 = new Constraint(null, "constraintName1", LogicalOperator.AND);
    constraint1.setConditions(Arrays.asList(ageCondition1, ageCondition2));

    Condition relativePopularityCondition1 = new Condition(RelativePopularityConditionType.ID, ">=", "0");
    Condition relativePopularityCondition2 = new Condition(RelativePopularityConditionType.ID, "<=", "100");
    Constraint constraint2 = new Constraint(null, "constraintName2", LogicalOperator.AND);
    constraint2.setConditions(Arrays.asList(relativePopularityCondition1, relativePopularityCondition2));

    tempEntity.newPolicy("policyName", constraint1, constraint2);

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator
        .evaluate(application, simulateReportIsAvailable("LogPolicyViolationPolicyConditionTriggers"),
            new Stage(Stage.ID_BUILD));

    assertThat(results.allViolations).isNotEmpty();
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, results.evaluation.getTime(), results.allViolations);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testEvaluate_PolicyViolationTelemetryCollector_CreateAndFixPolicyViolations() throws Exception {
    // Given
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    Policy policy = newSecurityPolicy();
    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    clearInvocations(mockTelemetrySender);

    // When running the first evaluation
    scanPolicyEvaluator.evaluate(application, scanId, stage);

    // Then no telemetry data is collected
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getValue();
    assertThat(telemetryDataList).hasSize(0);

    // When removing the policy
    new PolicyDAO().delete(policy);
    clearInvocations(mockTelemetrySender);
    // And running the second evaluation to have all policy violations fixed
    scanPolicyEvaluator.evaluate(application, scanId, stage);

    // Then all policy violations are collected for telemetry
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    telemetryDataList = telemetryDataArgumentCaptor.getValue();
    assertThat(telemetryDataList).hasSize(36);
    for (TelemetryData telemetryData : telemetryDataList) {
      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.TIME_TO_REMEDIATE_POLICY_VIOLATION);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testEvaluate_PolicyViolationTelemetryCollector_WaiveAndUnwaivePolicyViolations() throws Exception {
    // Create two policies that will cause policy violations and waive one policy.
    Policy securityPolicy = newSecurityPolicy();
    tempEntity.newWaiver(securityPolicy.getId(), application.getId());
    Policy licensePolicy = newPolicy(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    String scanId = simulateReportIsAvailable("testEvaluate_PolicyViolationLogger_WaivePolicyViolations/report");
    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    clearInvocations(mockTelemetrySender);

    // When evaluate policies
    scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD));

    // Then there should be two policy violations, of which one is waived.
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getValue();
    assertThat(telemetryDataList).hasSize(1);
    assertThat(telemetryDataList.get(0).getPurpose()).isEqualTo(TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION);
    assertThat(telemetryDataList.get(0).getAttributes().get(COUNT)).isEqualTo(1);
    clearInvocations(mockTelemetrySender);

    // When waive the other policy and evaluate policies again
    PolicyWaiver licensePolicyWaiver = tempEntity.newWaiver(licensePolicy.getId(), application.getId());
    scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD));

    // Then there should be two waived policy violations, one already waived and one newly waived.
    // Only one should be collected for telemetry
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    telemetryDataList = telemetryDataArgumentCaptor.getValue();
    assertThat(telemetryDataList).hasSize(1);
    assertThat(telemetryDataList.get(0).getPurpose()).isEqualTo(TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION);
    assertThat(telemetryDataList.get(0).getAttributes().get(COUNT)).isEqualTo(1);
    clearInvocations(mockTelemetrySender);

    // When remove the waiver for one of the policies and evaluate policies again.
    new PolicyWaiverDAO().delete(licensePolicyWaiver);
    scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD));
    // Then there should be an unwaived violation collected for telemetry
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    telemetryDataList = telemetryDataArgumentCaptor.getValue();
    assertThat(telemetryDataList).hasSize(1);
    assertThat(telemetryDataList.get(0).getPurpose()).isEqualTo(TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION);
    assertThat(telemetryDataList.get(0).getAttributes().get(COUNT)).isEqualTo(-1);
    clearInvocations(mockTelemetrySender);
  }

  private void assertPolicyViolationsLogged(PolicyViolationLogEvent policyViolationLogEvent,
                                            Date evaluationTime,
                                            List<PolicyViolation> policyViolations) throws Exception
  {
    List<PolicyViolationLogDTO> policyViolationLogDTOs =
        PolicyViolationLogDTOAssert.assertPolicyViolationLogDTOs(policyViolationLoggerOutput, policyViolationLogEvent,
            policyViolations.size());
    PolicyViolationLogDTOAssert.assertApplicationPolicyViolationData(policyViolationLogDTOs, policyViolationLogEvent,
        organization, application, evaluationTime, policyViolations);
  }

  private List<PolicyViolationLogDTO> assertPolicyViolationLogDTOs(int expected) throws Exception {
    return PolicyViolationLogDTOAssert.assertPolicyViolationLogDTOs(policyViolationLoggerOutput, expected);
  }

  private static void assertContainsPolicyViolation(ComponentIdentifier expectedComponentIdentifier,
                                                    String expectedHash,
                                                    Policy expectedPolicy,
                                                    Constraint expectedConstraint,
                                                    String expectedActionTypeId,
                                                    String expectedConditionTypeId,
                                                    List<PolicyViolation> actualPolicyViolations)
  {
    assertThat(findPolicyViolation(expectedComponentIdentifier, expectedHash, expectedPolicy, expectedConstraint,
        expectedActionTypeId, expectedConditionTypeId, actualPolicyViolations))
            .as("Cannot find expected policy violation.").isNotNull();
  }

  private static void assertNotContainsPolicyViolation(ComponentIdentifier expectedComponentIdentifier,
                                                       String expectedHash,
                                                       Policy expectedPolicy,
                                                       Constraint expectedConstraint,
                                                       String expectedActionTypeId,
                                                       String expectedConditionTypeId,
                                                       List<PolicyViolation> actualPolicyViolations)
  {
    assertThat(findPolicyViolation(expectedComponentIdentifier, expectedHash, expectedPolicy, expectedConstraint,
        expectedActionTypeId, expectedConditionTypeId, actualPolicyViolations)).as("Found unexpected policy violation.")
            .isNull();
  }

  private static PolicyViolation findPolicyViolation(ComponentIdentifier expectedComponentIdentifier,
                                                     String expectedHash,
                                                     Policy expectedPolicy,
                                                     Constraint expectedConstraint,
                                                     String expectedActionTypeId,
                                                     String expectedConditionTypeId,
                                                     List<PolicyViolation> actualPolicyViolations)
  {
    for (PolicyViolation actualPolicyViolation : actualPolicyViolations) {
      if (actualPolicyViolation.getPolicyId().equals(expectedPolicy.getId())
          && actualPolicyViolation.getPolicyName().equals(expectedPolicy.getName())
          && actualPolicyViolation.getComponentIdentifier().equals(expectedComponentIdentifier)
          && actualPolicyViolation.getHash().equals(expectedHash)
          && actualPolicyViolation.getActionTypeId().equals(expectedActionTypeId)) {
        assertThat(actualPolicyViolation.getConstraintFacts()).hasSize(1);
        ConstraintFact actualConstraintFact = actualPolicyViolation.getConstraintFacts().get(0);
        if (actualConstraintFact.getConstraintId().equals(expectedConstraint.getId())
            && actualConstraintFact.getConstraintName().equals(expectedConstraint.getName())) {
          assertThat(actualConstraintFact.getConditionFacts()).hasSize(1);
          ConditionFact actualConditionFact = actualConstraintFact.getConditionFacts().get(0);
          if (actualConditionFact.getConditionTypeId().equals(expectedConditionTypeId)) {
            return actualPolicyViolation;
          }
        }
      }
    }

    return null;
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
    return newPolicy(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
  }

  private Policy newRelativePopularityPolicy() {
    return newPolicy(new Condition(RelativePopularityConditionType.ID, ">=", "0"));
  }

  private Policy newPolicy(Condition... conditions) {
    return newPolicyAND(conditions);
  }

  private Policy newPolicyAND(Condition... conditions) {
    return newPolicy(LogicalOperator.AND, conditions);
  }

  private Policy newPolicyOR(Condition... conditions) {
    return newPolicy(LogicalOperator.OR, conditions);
  }

  private Policy newPolicy(LogicalOperator conditionOperator, Condition... conditions) {
    return tempEntity.newPolicy(application, 5, conditionOperator, conditions);
  }

  private List<PolicyViolation> getInactiveViolations(ScanPolicyEvaluatorResults scanPolicyEvaluatorResults) {
    List<PolicyViolation> inactiveViolations = new ArrayList<>(scanPolicyEvaluatorResults.allViolations);
    inactiveViolations.removeAll(scanPolicyEvaluatorResults.activeViolations);
    return inactiveViolations;
  }

  private List<PolicyViolation> sort(List<PolicyViolation> policyViolations) {
    List<PolicyViolation> result = new ArrayList<>(policyViolations);
    result.sort(PolicyViolationComparator.COMPARATOR);
    return result;
  }

  private void assertPolicyEvaluation(String scanId, boolean isReevaluation) {
    assertPolicyEvaluation(scanId, isReevaluation, false /* isForObsoleteScan */);
  }

  private void assertPolicyEvaluation(String scanId,
                                      boolean isReevaluation,
                                      boolean isForObsoleteScan)
  {
    PolicyEvaluation policyEvaluation = new PolicyEvaluationDAO().getLastByApplicationIdAndScanId(application.getId(),
        scanId);
    assertThat(policyEvaluation.isReevaluation()).isEqualTo(isReevaluation);
    assertThat(policyEvaluation.isForObsoleteScan()).isEqualTo(isForObsoleteScan);
  }

  private void assertApplicationComponent(ComponentIdentifier componentIdentifier,
                                          Date time,
                                          ApplicationComponent actual)
  {
    assertThat(actual.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(actual.getTime()).isEqualTo(time);
  }

  /**
   * Simulates that a report (based on the specified resource) exists.
   * 
   * @param reportResourceName can be a report.zip file or a directory that will be zipped up into a report.
   * 
   * @return A generated scan ID that can be used in subsequent calls to evaluate policies.
   */
  private String simulateReportIsAvailable(String reportResourceName) {
    return mockReportDownloader.mockDownloadReport("/" + getClass().getSimpleName() + "/" + reportResourceName);
  }
}
