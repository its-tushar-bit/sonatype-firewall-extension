/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static com.sonatype.insight.brain.api.v2.service.ConfigurationUtils.WITH_REPORTS;
import static com.sonatype.insight.brain.jooq.generated.ods.Tables.POLICY_VIOLATION;
import static com.sonatype.insight.brain.model.policy.ReachabilityStatus.NON_REACHABLE;
import static com.sonatype.insight.brain.model.policy.ReachabilityStatus.REACHABLE;
import static com.sonatype.insight.brain.model.policy.conditions.ConditionTypes.SecurityVulnerabilityEpssScoreConditionType;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.COUNT;
import static com.sonatype.insight.brain.policy.evaluator.PolicyViolationTelemetryCollector.LEGACY_VIOLATION_TIME;
import static com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator.REEVALUATE_NOT_ALLOWED_FOR_OUT_OF_DATE_SCAN_MESSAGE;
import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.DATA_JSON;
import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.POLICY_ALERTS;
import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.POLICY_THREATS;
import static com.sonatype.insight.brain.utils.VulnerabilitySignatureAnalysisDTOHelper.createTestAnalysisDTO;
import static com.sonatype.insight.brain.utils.VulnerabilitySignatureAnalysisDTOHelper.findPolicyViolationByVulnerabilityIdentifier;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.sonatype.clm.dto.model.EpssData;
import com.sonatype.clm.dto.model.component.AiModelContentType;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.signature.VulnerabilitySignatureAnalysisDTO;
import com.sonatype.insight.brain.api.experimental.ApiVulnerabilityReachabilityStatusService;
import com.sonatype.insight.brain.api.experimental.PurlIdentifiersWithVulnerabilities;
import com.sonatype.insight.brain.api.experimental.ReachableComponentVulnerabilities;
import com.sonatype.insight.brain.api.experimental.ReachableComponentVulnerabilities.PresentReachableComponentVulnerabilities;
import com.sonatype.insight.brain.dataaccess.AggregateFileDAO;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.OwnerComponentLicenseDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationConstraintFactsDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.OwnerComponentLicense;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilitySource;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFacts;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.AiModelContentConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ComponentCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ComponentEndOfLifeConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ComponentFormatConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.DataSourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.DependencyTypeConditionType;
import com.sonatype.insight.brain.model.policy.conditions.DerivativeAiModelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.HygieneRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.IacControlConditionType;
import com.sonatype.insight.brain.model.policy.conditions.IdentificationSourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.KevStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.PackageUrlConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryNameConflictConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCustomCVSSVectorStringConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCustomRemediationConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCweConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityDetectionConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityResearchConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.VulnerabilityGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.SecurityVulnerabilityResearch;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.WebhookNotification;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroup;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.LifecycleReport;
import com.sonatype.insight.brain.report.MockReportDownloader;
import com.sonatype.insight.brain.report.ReportDataStore;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfigDTO;
import com.sonatype.insight.brain.scanhealth.ScanHealthService;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.variant.AbstractComponentPgTest;
import com.sonatype.insight.brain.variant.ComponentPgTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEvent;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEventService;
import com.sonatype.insight.brain.webhook.PolicyAlertEvent;
import com.sonatype.insight.brain.webhook.PolicyAlertEventService;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;

@ComponentPgTest
public class ScanPolicyEvaluatorTest
    extends AbstractComponentPgTest
{
  private static final Logger log = LoggerFactory.getLogger(ScanPolicyEvaluatorTest.class);

  @Rule
  public LogOutput policyViolationLoggerOutput =
      new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  private AggregateFileDAO aggregateFileDAO;

  @Inject
  private PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO;

  @Inject
  private OwnerComponentLicenseDAO applicationComponentLicenseDAO;

  @Inject
  private OwnerComponentDAO applicationComponentDAO;

  @Inject
  private ThirdPartySbomMetadataDAO sbomMetadataDAO;

  @Inject
  private ScanPolicyEvaluator scanPolicyEvaluator;

  @Inject
  private InsightWork insightWork;

  @Inject
  private AsyncEventBus asyncEventBus;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private Configuration configuration;

  @Inject
  private ReportComponentService reportComponentService;

  @Inject
  private ReportService reportService;

  @Inject
  private ScanHealthService scanHealthService;

  @Mock
  private CurrentUser currentUser;

  @Mock
  private ComponentInfoService mockComponentInfoService;

  @Mock
  private ApiVulnerabilityReachabilityStatusService apiVulnerabilityReachabilityStatusService;

  private Organization organization;

  private Application application;

  private TestEventHandler<ApplicationEvaluationEvent> handler;

  private TestEventHandler<PolicyAlertEvent> policyAlertHandler;

  private MockReportDownloader mockReportDownloader;

  private TelemetrySender mockTelemetrySender;

  @AfterEach
  public void after() {
    try {
      if (handler != null) {
        asyncEventBus.unregister(handler);
      }
      if (policyAlertHandler != null) {
        asyncEventBus.unregister(policyAlertHandler);
      }
    }
    finally {
      // Reset every feature flag toggled by individual tests back to its default so it cannot leak into
      // sibling classes: this module runs the whole cohort in one fork with a single reused Spring context.
      SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);
      SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    }
  }

  @BeforeEach
  public void setup() {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());
    mockReportDownloader = new MockReportDownloader(tempDir);
    mockReportDownloader.setInsightWork(insightWork);
    applyBeanFieldOverride(ReportDataStore.class, "reportDownloader", mockReportDownloader.getMock());
    applyBeanFieldOverride(ScanPolicyEvaluator.class, "currentUser", currentUser);
    applyBeanFieldOverride(ApplicationEvaluationEventService.class, "currentUser", currentUser);
    applyBeanFieldOverride(PolicyAlertEventService.class, "currentUser", currentUser);
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.PURGE_SCAN_FILES, null);
    configuration.configurationChanged(Sets.newHashSet(SystemConfigurationProperty.PURGE_SCAN_FILES));
    lenient().when(currentUser.getUsernameOrSystem()).thenReturn(CurrentUser.SYSTEM);
  }

  @Test
  public void testEvaluate_Results_Evaluation() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("report");

    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results.evaluation).isNotNull();
    assertThat(results.evaluation.getOwnerId()).isEqualTo(application.getId());
    assertThat(results.evaluation.getStageTypeId()).isEqualTo(stage.getStageTypeId());
    assertThat(results.evaluation.getScanId()).isEqualTo(scanId);
    assertThat(results.evaluation.getCommitHash()).isEqualTo("testCommitHash");
    assertThat(results.evaluation.getScanTriggerType()).isEqualTo(ScanTriggerType.CLI);
    assertThat(results.evaluation.getBranchName()).isEqualTo("testBranchName");
  }

  @Test
  public void testEvaluate_Results_ContainerImageEvaluation() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    testProductLicense.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);

    Stage stage = new Stage(Stage.ID_PROXY);
    ScanTriggerType scanTriggerType = ScanTriggerType.SONATYPE_CONTAINER_IMAGE_SCANNER_API;

    String scanId = simulateReportIsAvailable("report");

    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, scanTriggerType, ClientScanType.SONATYPE, false);

    assertThat(results.evaluation).isNotNull();
    assertThat(results.evaluation.getOwnerId()).isEqualTo(application.getId());
    assertThat(results.evaluation.getStageTypeId()).isEqualTo(stage.getStageTypeId());
    assertThat(results.evaluation.getScanId()).isEqualTo(scanId);
    assertThat(results.evaluation.getCommitHash()).isEqualTo("testCommitHash");
    assertThat(results.evaluation.getScanTriggerType()).isEqualTo(scanTriggerType);
    assertThat(results.evaluation.getBranchName()).isEqualTo("testBranchName");
  }

  @Test
  public void testEvaluate_Results_ContainerImageEvaluation_MissingLicense() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    testProductLicense.setMissingFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    Stage stage = new Stage(Stage.ID_PROXY);

    assertThatExceptionOfType(InvalidStageException.class)
        .isThrownBy(() -> scanPolicyEvaluator.evaluate(application, "test-scan", stage,
            ScanTriggerType.CLI, ClientScanType.SONATYPE, false))
        .withMessage("Invalid stage id=proxy");
  }

  @Test
  public void testEvaluate_Results_ContainerImageEvaluation_MissingFeature() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
    testProductLicense.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    Stage stage = new Stage(Stage.ID_PROXY);

    assertThatExceptionOfType(InvalidStageException.class)
        .isThrownBy(() -> scanPolicyEvaluator.evaluate(application, "test-scan", stage,
            ScanTriggerType.CLI, ClientScanType.SONATYPE, false))
        .withMessage("Invalid stage id=proxy");
  }

  @Test
  public void testEvaluate_Results_ContainerImageEvaluation_MissingLicenseAndFeature() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
    testProductLicense.setMissingFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    Stage stage = new Stage(Stage.ID_PROXY);

    assertThatExceptionOfType(InvalidStageException.class)
        .isThrownBy(() -> scanPolicyEvaluator.evaluate(application, "test-scan", stage,
            ScanTriggerType.CLI, ClientScanType.SONATYPE, false))
        .withMessage("Invalid stage id=proxy");
  }

  @Test
  public void testEvaluate_Results_AllViolations() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    newSecurityPolicy();

    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results.allViolations).hasSize(36).filteredOn(PolicyViolation::isFixed).isEmpty();
    assertThat(results.activeViolations).hasSize(36);
  }

  @Test
  public void testEvaluate_PolicyNameChange() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    Policy policy = newSecurityPolicy();

    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    for (PolicyViolation violation : results1.activeViolations) {
      assertThat(violation.getPolicyName()).isEqualTo(policy.getName());
    }
    List<PolicyViolation> persistedViolations1 = policyViolationDAO.getByOwnerId(application.getId());
    assertThat(persistedViolations1)
        .allSatisfy(violation -> assertThat(violation.getPolicyName()).isEqualTo(policy.getName()));

    policy.setName("PolicyName1");
    policyDAO.update(policy);

    String scanId2 = simulateReportIsAvailable("report");
    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId2, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results2.activeViolations)
        .allSatisfy(violation -> assertThat(violation.getPolicyName()).isEqualTo(policy.getName()));
    List<PolicyViolation> persistedViolations2 = policyViolationDAO.getByOwnerId(application.getId());
    assertThat(persistedViolations2)
        .allSatisfy(violation -> assertThat(violation.getPolicyName()).isEqualTo(policy.getName()));
  }

  @Test
  public void testEvaluate_Results_WaivedViolations() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    Policy policy = newSecurityPolicy();
    PolicyWaiver waiver = tempEntity.newWaiver("f0776db1593e215146d2", policy.getId(), application.getId());

    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results.activeViolations).hasSize(33).allSatisfy(violation -> {
      assertThat(violation.getHash()).isNotEqualTo(waiver.getHash());
      assertThat(violation.getLegacyViolationTime()).isNull();
      assertThat(violation.getWaiveTime()).isNull();
      assertThat(violation.getPolicyWaiverId()).isNull();
      assertThat(violation.getPolicyWaiverComment()).isNull();
    });
    List<PolicyViolation> inactiveViolations = getInactiveViolations(results);
    assertThat(inactiveViolations).hasSize(3).allSatisfy(inactiveViolation -> {
      assertThat(inactiveViolation.getHash()).isEqualTo(waiver.getHash());
      assertThat(inactiveViolation.isLegacyViolation()).isFalse();
      assertThat(inactiveViolation.getWaiveTime()).isNotNull();
      assertThat(inactiveViolation.getPolicyWaiverId()).isEqualTo(waiver.getId());
      assertThat(inactiveViolation.getPolicyWaiverComment()).isEqualTo(waiver.getComment());
    });
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_AutoWaiversFeatureMissing() throws Exception {
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    Policy licensePolicy = newPolicy(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));

    tempEntity.newAutoPolicyWaiver(application.getId(), 7, false, false);
    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    List<PolicyViolation> autoWaivedViolations = results.autoWaivedViolations;
    assertThat(autoWaivedViolations).isEmpty();

    assertThat(results.activeViolations).hasSize(20).allSatisfy(activeViolation -> {
      assertThat(activeViolation.getPolicyId()).isEqualTo(licensePolicy.getId());
      assertThat(activeViolation.getLegacyViolationTime()).isNull();
      assertThat(activeViolation.getWaiveTime()).isNull();
      assertThat(activeViolation.getAutoPolicyWaiverId()).isNull();
      assertThat(activeViolation.getPolicyWaiverId()).isNull();
      assertThat(activeViolation.getPolicyWaiverComment()).isNull();
    });

    List<PolicyViolation> inactiveViolations = getInactiveViolations(results);
    assertThat(inactiveViolations).isEmpty();
  }

  /**
   * Verifies that auto-waivers are NOT applied when the AUTO_WAIVER_MANAGEMENT entitlement is absent
   * (e.g., Lifecycle Pro tier, license expired). Even if auto-waiver rows exist in the database (left
   * over from a previous Enterprise entitlement or other source), scan evaluation must not apply them.
   *
   * <p>
   * This locks in the defense-in-depth check in {@code ScanPolicyEvaluator} that guards against
   * license expiry and misconfiguration scenarios (CLM-39600).
   */
  @Test
  public void testEvaluate_Results_AutoWaivedViolations_AutoWaiverManagementEntitlementMissing() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.AUTO_WAIVER_MANAGEMENT);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    Policy licensePolicy = newPolicy(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));

    tempEntity.newAutoPolicyWaiver(application.getId(), 7, false, false);
    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    List<PolicyViolation> autoWaivedViolations = results.autoWaivedViolations;
    assertThat(autoWaivedViolations).isEmpty();

    assertThat(results.activeViolations).hasSize(20).allSatisfy(activeViolation -> {
      assertThat(activeViolation.getPolicyId()).isEqualTo(licensePolicy.getId());
      assertThat(activeViolation.getLegacyViolationTime()).isNull();
      assertThat(activeViolation.getWaiveTime()).isNull();
      assertThat(activeViolation.getAutoPolicyWaiverId()).isNull();
      assertThat(activeViolation.getPolicyWaiverId()).isNull();
      assertThat(activeViolation.getPolicyWaiverComment()).isNull();
    });

    List<PolicyViolation> inactiveViolations = getInactiveViolations(results);
    assertThat(inactiveViolations).isEmpty();
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_PathForward_NoVersionChanges() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(4);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 7, false, true);
    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    List<PolicyViolation> autoWaivedViolations = results.autoWaivedViolations;
    assertThat(autoWaivedViolations).hasSize(36).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiver.getId());
      assertThat(autoWaivedViolation.getWaiveTime()).isNotNull();
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getPolicyId()).isEqualTo(securityPolicy.getId());
    });
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_PathForward_WithVersionChanges() throws Exception {
    ComponentDetailsDTO tomcatComponentDetailsDTOV1 = new ComponentDetailsDTO();
    tomcatComponentDetailsDTOV1.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    tomcatComponentDetailsDTOV1.violatedPolicyCount = 1;
    ComponentDetailsDTO tomcatComponentDetailsDTOV2 = new ComponentDetailsDTO();
    tomcatComponentDetailsDTOV2.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.25");
    tomcatComponentDetailsDTOV2.violatedPolicyCount = 0;

    doReturn(Pair.of(Arrays.asList(tomcatComponentDetailsDTOV1, tomcatComponentDetailsDTOV2), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), eq(tomcatComponentDetailsDTOV1.componentIdentifier), any(), any(), any(), any(), any(),
            anyBoolean());

    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), not(eq(tomcatComponentDetailsDTOV1.componentIdentifier)), any(), any(), any(), any(), any(),
            anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(4);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    tempEntity.newAutoPolicyWaiver(application.getId(), 7, false, true);
    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    List<PolicyViolation> autoWaivedViolations = results.autoWaivedViolations;
    assertThat(autoWaivedViolations).hasSize(27);
    // 9 violations from tomcat has pathForward version
    assertThat(results.activeViolations).hasSize(9);
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_PathForward_NonSecurityViolations() throws Exception {
    ComponentDetailsDTO tomcatComponentDetailsDTOV1 = new ComponentDetailsDTO();
    tomcatComponentDetailsDTOV1.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    tomcatComponentDetailsDTOV1.violatedPolicyCount = 1;
    ComponentDetailsDTO tomcatComponentDetailsDTOV2 = new ComponentDetailsDTO();
    tomcatComponentDetailsDTOV2.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.25");
    tomcatComponentDetailsDTOV2.violatedPolicyCount = 0;

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    newPolicy(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));

    tempEntity.newAutoPolicyWaiver(application.getId(), 10, false, true);
    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results.autoWaivedViolations).isEmpty();
    assertThat(results.activeViolations).hasSize(20);

    verify(mockComponentInfoService, times(0)).getComponentDetailsForAllVersionsNoAuth(
        any(), any(), any(), any(), any(), any(), any(), eq(true));
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_MultipleAutoWaivers() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    AutoPolicyWaiver appWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 8, false, true);
    tempEntity.newAutoPolicyWaiver(application.getOrganizationId(), 4, false, true);
    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results.autoWaivedViolations).hasSize(36).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(appWaiver.getId());
      assertThat(autoWaivedViolation.getWaiveTime()).isNotNull();
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getPolicyId()).isEqualTo(securityPolicy.getId());
    });
    assertThat(results.activeViolations).isEmpty();
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_ThreatLevelOnly_noPolicyWaiver() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    // License policy has a threat level of 5, but it is not security violation.
    Policy licensePolicy = newPolicy(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8); // should not be auto waived
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    // auto policy waiver threat level to 7
    tempEntity.newAutoPolicyWaiver(application.getId(), 7, false, false);
    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    List<PolicyViolation> autoWaivedViolations = results.autoWaivedViolations;
    assertThat(autoWaivedViolations).isEmpty();

    // auto waiver should not apply to security policy violations because their threat level > 7
    // also not apply to license type violations
    assertThat(results.activeViolations).allSatisfy(activeViolation -> {
      if (activeViolation.getThreatCategory().equals(PolicyThreatCategory.SECURITY)) {
        assertThat(activeViolation.getPolicyId()).isEqualTo(securityPolicy.getId());
      }
      else {
        assertThat(activeViolation.getPolicyId()).isEqualTo(licensePolicy.getId());
      }
      assertThat(activeViolation.getLegacyViolationTime()).isNull();
      assertThat(activeViolation.getWaiveTime()).isNull();
      assertThat(activeViolation.getAutoPolicyWaiverId()).isNull();
      assertThat(activeViolation.getPolicyWaiverId()).isNull();
      assertThat(activeViolation.getPolicyWaiverComment()).isNull();
    });
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_ThreatLevelOnly_withPolicyWaiver() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    // Basic setup
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    // There should be 36 violations per security policy
    Policy securityPolicyOne = new Policy(null, "Security Policy One");
    securityPolicyOne.setThreatLevel(5);
    securityPolicyOne.setOwnerId(application.getId());
    Constraint constraintOne = new Constraint(null, "TestConstraintOne", LogicalOperator.AND);
    constraintOne.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicyOne.addConstraint(constraintOne);
    tempEntity.newPolicy(securityPolicyOne);

    Policy securityPolicyTwo = new Policy(null, "Security Policy Two");
    securityPolicyTwo.setThreatLevel(9);
    securityPolicyTwo.setOwnerId(application.getId());
    Constraint constraintTwo = new Constraint(null, "TestConstraintTwo", LogicalOperator.AND);
    constraintTwo.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicyTwo.addConstraint(constraintTwo);
    tempEntity.newPolicy(securityPolicyTwo);

    // Create a policy waiver for one of them - will only waive 3 of the 36 violations found
    PolicyWaiver waiver = tempEntity.newWaiver("f0776db1593e215146d2", securityPolicyOne.getId(), application.getId());

    // Create an auto waiver - should only waive violations for policy 1 where the threat level is 5
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 7, false, true);

    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, new VulnerabilitySignatureAnalysisDTO(), false);

    assertThat(results.activeViolations).hasSize(36).allSatisfy(activeViolation -> {
      assertThat(activeViolation.getPolicyId()).isEqualTo(securityPolicyTwo.getId());
      assertThat(activeViolation.getLegacyViolationTime()).isNull();
      assertThat(activeViolation.getWaiveTime()).isNull();
      assertThat(activeViolation.getAutoPolicyWaiverId()).isNull();
      assertThat(activeViolation.getPolicyWaiverId()).isNull();
      assertThat(activeViolation.getPolicyWaiverComment()).isNull();
    });

    assertThat(results.waivedViolations).hasSize(3).allSatisfy(waivedViolation -> {
      assertThat(waivedViolation.getHash()).isEqualTo(waiver.getHash());
      assertThat(waivedViolation.isLegacyViolation()).isFalse();
      assertThat(waivedViolation.getWaiveTime()).isNotNull();
      assertThat(waivedViolation.getPolicyWaiverId()).isEqualTo(waiver.getId());
      assertThat(waivedViolation.getPolicyWaiverComment()).isEqualTo(waiver.getComment());
    });

    assertThat(results.autoWaivedViolations).hasSize(33).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.isLegacyViolation()).isFalse();
      assertThat(autoWaivedViolation.getWaiveTime()).isNotNull();
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiver.getId());
    });

    List<PolicyViolation> inactiveViolations = getInactiveViolations(results);
    assertThat(inactiveViolations).hasSize(36);
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_NoExclusions() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    tempEntity.newAutoPolicyWaiver(application.getId(), 10, false, true);

    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, new VulnerabilitySignatureAnalysisDTO(), false);

    assertThat(results.autoWaivedViolations).hasSize(36);
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_WithReachableVulnerability_NotReachable() throws Exception {
    // Mock the reachable vuln map
    Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> reachableVulnMap = new HashMap();

    List<String> unreachableComponentList = List.of(
        "pkg:maven/commons-httpclient/commons-httpclient@3.1",
        "pkg:maven/org.apache.geronimo.framework/geronimo-security@2.1",
        "pkg:maven/tomcat/catalina-host-manager@5.5.23",
        "pkg:maven/org.mortbay.jetty/jetty@6.1.15",
        "pkg:maven/tomcat/servlets-default@5.5.4",
        "pkg:maven/org.openid4java/openid4java@0.9.5",
        "pkg:maven/tomcat/tomcat-util@5.4.23",
        "pkg:maven/tomcat/tomcat-util@5.5.23");

    addReachabilityMap(unreachableComponentList, reachableVulnMap);

    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId", reachableVulnMap))
        .when(apiVulnerabilityReachabilityStatusService)
        .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(), any(VulnerabilitySignatureAnalysisDTO.class));

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    tempEntity.newAutoPolicyWaiver(application.getId(), 10, true, false);

    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    String vulnerabilityIdentifier = "CVE-2012-0022";

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        scanId,
        componentIdentifier,
        vulnerabilityIdentifier,
        insightWork);

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, analysisDTO, false);

    assertThat(results.autoWaivedViolations).hasSize(36);

    Optional<PolicyViolation> optionalPolicyViolation =
        findPolicyViolationByVulnerabilityIdentifier(results.autoWaivedViolations, vulnerabilityIdentifier);

    assertThat(optionalPolicyViolation).isPresent();
    assertThat(optionalPolicyViolation.get().getReachabilityStatus()).isEqualTo(NON_REACHABLE);
    assertThat(optionalPolicyViolation.get().getComponentIdentifier()).isEqualTo(componentIdentifier);

    results.autoWaivedViolations
        .forEach(policyViolation -> assertThat(policyViolation.getReachabilityStatus()).isNotNull());
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_WithReachableVulnerability_Reachable() throws Exception {
    // Mock the reachable vuln map
    Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> reachableVulnMap = new HashMap();

    // reachable vuln
    String reachableCVE = "CVE-2012-0022";
    reachableVulnMap.put(new PackageUrlIdentifier("pkg:maven/tomcat/tomcat-util@5.5.23"),
        new PresentReachableComponentVulnerabilities(Set.of(reachableCVE)));

    List<String> unreachableComponentList = List.of(
        "pkg:maven/commons-httpclient/commons-httpclient@3.1",
        "pkg:maven/org.apache.geronimo.framework/geronimo-security@2.1",
        "pkg:maven/tomcat/catalina-host-manager@5.5.23",
        "pkg:maven/org.mortbay.jetty/jetty@6.1.15",
        "pkg:maven/tomcat/servlets-default@5.5.4",
        "pkg:maven/org.openid4java/openid4java@0.9.5",
        "pkg:maven/tomcat/tomcat-util@5.4.23");

    addReachabilityMap(unreachableComponentList, reachableVulnMap);

    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId", reachableVulnMap))
        .when(apiVulnerabilityReachabilityStatusService)
        .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(), any(VulnerabilitySignatureAnalysisDTO.class));

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    tempEntity.newAutoPolicyWaiver(application.getId(), 10, true, false);

    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        scanId,
        componentIdentifier,
        reachableCVE,
        insightWork);

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, analysisDTO, false);

    // Total violations = 36
    // Security.REACHABLE violations = 1
    // Security.NON_REACHABLE violations = 35

    // Should not autowaive violations that are REACHABLE. Should only autowaive violations that are NON_REACHABLE
    assertThat(results.activeViolations).hasSize(1);
    assertThat(results.autoWaivedViolations).hasSize(35);
  }

  @Test
  public void testEvaluate_Results_WithoutReachableVulnerability_ThenWithReachableVulnerability_UsingExistingViolation() throws Exception {
    doReturn(null)
        .when(apiVulnerabilityReachabilityStatusService)
        .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(), any(VulnerabilitySignatureAnalysisDTO.class));

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    tempEntity.newAutoPolicyWaiver(application.getId(), 10, true, false);

    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        scanId,
        componentIdentifier,
        "",
        insightWork);

    // we run with all the details, but we have not data for reachability
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, analysisDTO, false);

    assertThat(results.activeViolations).hasSize(36)
        .allSatisfy(violation -> assertThat(violation.getReachabilityStatus()).isNull());
    assertThat(results.autoWaivedViolations).isEmpty();
    assertThat(results.waivedViolations).isEmpty();

    // Mock the reachable vuln map
    Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> reachableVulnMap = new HashMap();

    // reachable vuln
    String reachableCVE = "CVE-2012-0022";

    reachableVulnMap.put(new PackageUrlIdentifier("pkg:maven/tomcat/tomcat-util@5.5.23"),
        new PresentReachableComponentVulnerabilities(Set.of(reachableCVE)));

    List<String> unreachableComponentList = List.of(
        "pkg:maven/commons-httpclient/commons-httpclient@3.1",
        "pkg:maven/org.apache.geronimo.framework/geronimo-security@2.1",
        "pkg:maven/tomcat/catalina-host-manager@5.5.23",
        "pkg:maven/org.mortbay.jetty/jetty@6.1.15",
        "pkg:maven/tomcat/servlets-default@5.5.4",
        "pkg:maven/org.openid4java/openid4java@0.9.5",
        "pkg:maven/tomcat/tomcat-util@5.4.23");

    addReachabilityMap(unreachableComponentList, reachableVulnMap);

    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId", reachableVulnMap))
        .when(apiVulnerabilityReachabilityStatusService)
        .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(), any(VulnerabilitySignatureAnalysisDTO.class));

    // we run again with all the details, but now we have data for reachability
    results = scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, analysisDTO, false);

    // Total violations = 36
    // Security.REACHABLE violations = 1
    // Security.NON_REACHABLE violations = 35
    assertThat(results.activeViolations)
        .hasSize(1)
        .extracting(PolicyViolation::getReachabilityStatus)
        .containsOnly(REACHABLE);

    assertThat(results.autoWaivedViolations)
        .hasSize(35)
        .allSatisfy(violation -> assertThat(violation.getReachabilityStatus()).isEqualTo(NON_REACHABLE));
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_PathForward_WithVersionChanges_WithReachableVuln_Reachable() throws Exception {
    String knownCVE = "CVE-2011-4314";
    Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> reachableVulnMap = new HashMap();

    // reachable vuln
    reachableVulnMap.put(new PackageUrlIdentifier("pkg:maven/org.openid4java/openid4java@0.9.5"),
        new PresentReachableComponentVulnerabilities(Set.of(knownCVE)));

    List<String> unreachableComponentList = List.of(
        "pkg:maven/commons-httpclient/commons-httpclient@3.1",
        "pkg:maven/org.apache.geronimo.framework/geronimo-security@2.1",
        "pkg:maven/tomcat/catalina-host-manager@5.5.23",
        "pkg:maven/org.mortbay.jetty/jetty@6.1.15",
        "pkg:maven/tomcat/tomcat-util@5.5.23",
        "pkg:maven/tomcat/servlets-default@5.5.4");

    addReachabilityMap(unreachableComponentList, reachableVulnMap);

    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId", reachableVulnMap))
        .when(apiVulnerabilityReachabilityStatusService)
        .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(), any(VulnerabilitySignatureAnalysisDTO.class));

    ComponentDetailsDTO tomcatComponentDetailsDTOV1 = new ComponentDetailsDTO();
    tomcatComponentDetailsDTOV1.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    tomcatComponentDetailsDTOV1.violatedPolicyCount = 1;
    ComponentDetailsDTO tomcatComponentDetailsDTOV2 = new ComponentDetailsDTO();
    tomcatComponentDetailsDTOV2.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.25");
    tomcatComponentDetailsDTOV2.violatedPolicyCount = 0;

    doReturn(Pair.of(Arrays.asList(tomcatComponentDetailsDTOV1, tomcatComponentDetailsDTOV2), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), eq(tomcatComponentDetailsDTOV1.componentIdentifier), any(), any(), any(), any(), any(),
            anyBoolean());

    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), not(eq(tomcatComponentDetailsDTOV1.componentIdentifier)), any(), any(), any(), any(), any(),
            anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(4);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    tempEntity.newAutoPolicyWaiver(application.getId(), 7, true, true, false);

    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("org.openid4java", "openid4java", "0.9.5");

    String vulnerabilityIdentifier = "CVE-2011-4314";
    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        scanId,
        componentIdentifier,
        vulnerabilityIdentifier,
        insightWork);

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, analysisDTO, false);

    // Total violations = 36
    // Security.REACHABLE violations = 1
    // Security.NON_REACHABLE violations = 35
    // Components with pathForward = 9
    assertThat(results.autoWaivedViolations).hasSize(26);
    assertThat(results.activeViolations).hasSize(10);
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_PathForward_WithVersionChanges_Reachable() throws Exception {
    ComponentDetailsDTO tomcatComponentDetailsDTOV1 = new ComponentDetailsDTO();
    tomcatComponentDetailsDTOV1.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    tomcatComponentDetailsDTOV1.violatedPolicyCount = 1;
    ComponentDetailsDTO tomcatComponentDetailsDTOV2 = new ComponentDetailsDTO();
    tomcatComponentDetailsDTOV2.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.25");
    tomcatComponentDetailsDTOV2.violatedPolicyCount = 0;

    doReturn(Pair.of(Arrays.asList(tomcatComponentDetailsDTOV1, tomcatComponentDetailsDTOV2), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), eq(tomcatComponentDetailsDTOV1.componentIdentifier), any(), any(), any(), any(), any(),
            anyBoolean());

    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), not(eq(tomcatComponentDetailsDTOV1.componentIdentifier)), any(), any(), any(), any(), any(),
            anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(4);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    tempEntity.newAutoPolicyWaiver(application.getId(), 7, true, true);

    // With no reachability data, but reachable checked off in config
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, null, false);

    List<PolicyViolation> autoWaivedViolations = results.autoWaivedViolations;

    // Total violations = 36
    // Security.REACHABLE violations = 0
    // Security.NON_REACHABLE violations = 0
    // Components with pathForward = 9
    assertThat(autoWaivedViolations).hasSize(27);
    assertThat(results.activeViolations).hasSize(9);
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_ExclusionsApply_ExactComponent() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    // The exclusion will apply to tomcat-util version 5.5.23 only. The violation for v5.4.23 will be auto-waived
    String componentIdentifier = "maven: {artifactId=tomcat-util, groupId=tomcat, version=5.5.23}";
    String componentHash = "1249e25aebb15358bedd";

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 10, false, true);
    tempEntity.newAutoPolicyWaiverExclusion(
        application.getId(),
        "creatorId",
        "creatorName",
        new Date(),
        autoPolicyWaiver.getId(),
        scanId,
        componentHash,
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);

    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results.allViolations).hasSize(36);
    assertThat(results.autoWaivedViolations).hasSize(28);
    assertThat(results.activeViolations).hasSize(8).allSatisfy(activeViolation -> {
      assertThat(activeViolation.getHash()).isEqualTo(componentHash);
      assertThat(activeViolation.getComponentIdentifier()).hasToString(componentIdentifier);
    });
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_ExclusionsApply_AllVersions() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    // The exclusion will apply to all versions of tomcat-util. Report contains violations for 5.4.23 & 5.5.23
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 10, false, true);

    tempEntity.newAutoPolicyWaiverExclusion(
        application.getId(),
        "creatorId",
        "creatorName",
        new Date(),
        autoPolicyWaiver.getId(),
        scanId,
        null,
        ComponentMatcherStrategyForExclusion.ALL_VERSIONS,
        null,
        null,
        null,
        securityPolicy.getName(),
        null,
        null,
        componentIdentifier,
        null);

    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results.allViolations).hasSize(36);
    assertThat(results.autoWaivedViolations).hasSize(27);
    assertThat(results.activeViolations).hasSize(9).allSatisfy(activeViolation -> {
      assertThat(activeViolation.getComponentIdentifier().toString()).contains("tomcat-util");
    });
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_ExclusionsApply_PolicyViolation() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    Stage stageTwo = new Stage(Stage.ID_DEVELOP);
    String scanIdOne = simulateReportIsAvailable("AutoWaiverRevocationsAlternate");
    String scanIdTwo = simulateReportIsAvailable("AutoWaiverRevocationsAlternate");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 10, false, true);

    ScanPolicyEvaluatorResults evalOne =
        scanPolicyEvaluator.evaluate(application, scanIdOne, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    PolicyViolation targetViolation = evalOne.autoWaivedViolations.get(0);

    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(
        application.getId(),
        "fakeId",
        "fakeName",
        autoPolicyWaiver.getId(),
        scanIdOne,
        targetViolation);

    ScanPolicyEvaluatorResults evalTwo =
        scanPolicyEvaluator.evaluate(application, scanIdTwo, stageTwo, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(evalTwo.allViolations).hasSize(3);
    assertThat(evalTwo.autoWaivedViolations).hasSize(2);
    assertThat(evalTwo.activeViolations).hasSize(1).allSatisfy(violation -> {
      assertThat(violation.getPolicyId()).isEqualTo(exclusion.getPolicyId());
      assertThat(violation.getHash()).isEqualTo(exclusion.getHash());
      assertThat(violation.getThreatLevel()).isEqualTo(exclusion.getThreatLevel());
      assertThat(violation.getComponentIdentifier()).isEqualTo(exclusion.getComponentIdentifier());
    });
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_MultiExclusionsApply_PolicyViolation() throws Exception {
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    Stage stageTwo = new Stage(Stage.ID_DEVELOP);
    String scanIdOne = simulateReportIsAvailable("AutoWaiverRevocationsAlternateMultiSecurity");
    String scanIdTwo = simulateReportIsAvailable("AutoWaiverRevocationsAlternateMultiSecurity");

    Policy securityPolicyOne = new Policy(null, "Security Policy One");
    securityPolicyOne.setThreatLevel(9);
    securityPolicyOne.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    Constraint constraintOne = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraintOne.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicyOne.addConstraint(constraintOne);
    tempEntity.newPolicy(securityPolicyOne);

    Policy securityPolicyTwo = new Policy(null, "Security Policy Two");
    securityPolicyTwo.setThreatLevel(8);
    securityPolicyTwo.setOwnerId(application.getId());
    Constraint constraintTwo = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraintTwo.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicyTwo.addConstraint(constraintTwo);
    tempEntity.newPolicy(securityPolicyTwo);

    AutoPolicyWaiver rootAutoPolicyWaiver = tempEntity
        .newAutoPolicyWaiver(Organization.ROOT_ORGANIZATION_ID, 10, false, false);
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 9, false, true);

    ScanPolicyEvaluatorResults evalOne =
        scanPolicyEvaluator.evaluate(application, scanIdOne, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    ComponentIdentifier gsonComponentIdentifier = ComponentIdentifier
        .createMavenCoordinates("com.google.code.gson", "gson", "2.8.1", "", "jar");
    Optional<PolicyViolation> optionalRootTargetViolation =
        evalOne.autoWaivedViolations.stream()
            .filter(
                policyViolation -> policyViolation.getPolicyId().equals(securityPolicyOne.getId()) &&
                    policyViolation.getComponentIdentifier().compareTo(gsonComponentIdentifier) == 0)
            .findFirst();

    PolicyViolation rootTargetViolation = optionalRootTargetViolation.get();

    AutoPolicyWaiverExclusion rootExclusion = tempEntity.newAutoPolicyWaiverExclusion(
        Organization.ROOT_ORGANIZATION_ID,
        "fakeId",
        "fakeName",
        rootAutoPolicyWaiver.getId(),
        scanIdOne,
        rootTargetViolation);

    ComponentIdentifier jacksonDatabindComponentIdentifier = ComponentIdentifier
        .createMavenCoordinates("com.fasterxml.jackson.core", "jackson-databind", "2.9.8", "", "jar");
    Optional<PolicyViolation> optionalTargetViolation =
        evalOne.autoWaivedViolations.stream()
            .filter(
                policyViolation -> policyViolation.getPolicyId().equals(securityPolicyTwo.getId()) &&
                    policyViolation.getComponentIdentifier().compareTo(jacksonDatabindComponentIdentifier) == 0)
            .findFirst();

    PolicyViolation targetViolation = optionalTargetViolation.get();

    AutoPolicyWaiverExclusion exclusion = tempEntity.newAutoPolicyWaiverExclusion(
        application.getId(),
        "fakeId",
        "fakeName",
        autoPolicyWaiver.getId(),
        scanIdOne,
        targetViolation);

    ScanPolicyEvaluatorResults evalTwo =
        scanPolicyEvaluator.evaluate(application, scanIdTwo, stageTwo, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(evalTwo.allViolations).hasSize(8);

    // this will get auto waved violations, showing the lowest leave as the auto waiver
    assertThat(evalTwo.autoWaivedViolations)
        .hasSize(6)
        .allSatisfy(autoWaivedViolation -> {
          assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiver.getId());
        });

    // we end up here with 2 active violations, one for each policy and individual components
    assertThat(evalTwo.activeViolations)
        .hasSize(2)
        .satisfiesOnlyOnce(violation -> {
          assertThat(violation.getPolicyId()).isEqualTo(rootExclusion.getPolicyId());
          assertThat(violation.getHash()).isEqualTo(rootExclusion.getHash());
          assertThat(violation.getThreatLevel()).isEqualTo(rootExclusion.getThreatLevel());
          assertThat(violation.getComponentIdentifier()).isEqualTo(rootExclusion.getComponentIdentifier());
          assertThat(violation.getComponentIdentifier()).isEqualTo(gsonComponentIdentifier);
        })
        .satisfiesOnlyOnce(violation -> {
          assertThat(violation.getPolicyId()).isEqualTo(exclusion.getPolicyId());
          assertThat(violation.getHash()).isEqualTo(exclusion.getHash());
          assertThat(violation.getThreatLevel()).isEqualTo(exclusion.getThreatLevel());
          assertThat(violation.getComponentIdentifier()).isEqualTo(exclusion.getComponentIdentifier());
          assertThat(violation.getComponentIdentifier()).isEqualTo(jacksonDatabindComponentIdentifier);
        });
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_ExclusionsApply_PolicyViolation_incompleteComponent() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    Stage stageTwo = new Stage(Stage.ID_DEVELOP);
    String scanIdOne = simulateReportIsAvailable("AutoWaiverRevocations");
    String scanIdTwo = simulateReportIsAvailable("AutoWaiverRevocations");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 10, false, true);

    ScanPolicyEvaluatorResults evalOne =
        scanPolicyEvaluator.evaluate(application, scanIdOne, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    PolicyViolation targetViolation = evalOne.autoWaivedViolations.get(0);

    tempEntity.newAutoPolicyWaiverExclusion(
        application.getId(),
        "fakeId",
        "fakeName",
        autoPolicyWaiver.getId(),
        scanIdOne,
        targetViolation);

    ScanPolicyEvaluatorResults evalTwo =
        scanPolicyEvaluator.evaluate(application, scanIdTwo, stageTwo, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(evalTwo.allViolations).hasSize(36);
    assertThat(evalTwo.autoWaivedViolations).hasSize(36);
    assertThat(evalTwo.activeViolations).isEmpty();
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_ExclusionsDoNotApply_ExactComponent() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 10, false, true);

    tempEntity.newAutoPolicyWaiverExclusion(
        application.getId(),
        "creatorId",
        "creatorName",
        new Date(),
        autoPolicyWaiver.getId(),
        scanId,
        "someHash",
        ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);

    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    // The exclusion does not apply to the component with violations, so all violations should be auto-waived
    assertThat(results.autoWaivedViolations).hasSize(36);
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_ExclusionsDoNotApply_AllVersions() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("group", "artifact", "2.0");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 10, false, true);

    tempEntity.newAutoPolicyWaiverExclusion(
        application.getId(),
        "creatorId",
        "creatorName",
        new Date(),
        autoPolicyWaiver.getId(),
        scanId,
        null,
        ComponentMatcherStrategyForExclusion.ALL_VERSIONS,
        null,
        null,
        null,
        securityPolicy.getName(),
        null,
        null,
        componentIdentifier,
        null);

    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    // The exclusion does not apply to the component with violations, so all violations should be auto-waived
    assertThat(results.autoWaivedViolations).hasSize(36);
  }

  @Test
  public void testEvaluate_Results_AutoWaivedViolations_ExclusionsDoNotApply_PolicyViolation() throws Exception {
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    Stage stageTwo = new Stage(Stage.ID_DEVELOP);
    String scanIdOne = simulateReportIsAvailable("AutoWaiverRevocations");
    String scanIdTwo = simulateReportIsAvailable("AutoWaiverRevocations");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 10, false, true);

    ScanPolicyEvaluatorResults evalOne =
        scanPolicyEvaluator.evaluate(application, scanIdOne, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    PolicyViolation targetViolation = evalOne.autoWaivedViolations.get(0);

    tempEntity.newAutoPolicyWaiverExclusion(
        application.getId(),
        "fakeId",
        "fakeName",
        autoPolicyWaiver.getId(),
        scanIdOne,
        "someOtherHash",
        targetViolation);

    ScanPolicyEvaluatorResults evalTwo =
        scanPolicyEvaluator.evaluate(application, scanIdTwo, stageTwo, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(evalTwo.allViolations).hasSize(36);
    assertThat(evalTwo.autoWaivedViolations).hasSize(36);
    assertThat(evalTwo.activeViolations).isEmpty();
  }

  @Test
  public void testReEvaluate_Results_WithoutSkippingAutoWaivers() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    /*
     * Run policy evaluation for the first time
     */
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    tempEntity.newAutoPolicyWaiver(application.getId(), 10,
        false, true);

    ScanPolicyEvaluatorResults reevaluationResults = scanPolicyEvaluator.evaluate(application,
        scanId,
        stage,
        ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    /*
     * Auto-waivers were not skipped so they should be applied
     */
    assertThat(reevaluationResults.allViolations).hasSize(36);
    assertThat(reevaluationResults.autoWaivedViolations).hasSize(36);
    assertThat(reevaluationResults.activeViolations).isEmpty();
  }

  @Test
  public void testReEvaluate_Results_SkippingAutoWaivers() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    /*
     * Run policy evaluation for the first time
     */
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    tempEntity.newAutoPolicyWaiver(application.getId(), 10,
        false, false);

    ScanPolicyEvaluatorResults reevaluationResults = scanPolicyEvaluator.evaluate(application,
        scanId,
        stage,
        ScanTriggerType.CLI,
        ClientScanType.SONATYPE, true);

    /*
     * Auto-waivers were skipped so they should not be applied
     */
    assertThat(reevaluationResults.allViolations).hasSize(36);
    assertThat(reevaluationResults.autoWaivedViolations).isEmpty();
    assertThat(reevaluationResults.activeViolations).hasSize(36);
  }

  @Test
  public void testReEvaluate_Results_SkippingAutoWaiversWithoutReevaluation_ThrowsError() {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8); // should not be auto waived
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    /*
     * Run policy evaluation for the first time
     */
    assertThatThrownBy(() -> scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, true))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Auto-waivers can only be skipped in re-evaluations not primary policy evaluations.");
  }

  @Test
  public void testEvaluate_Results_LegacyViolations() throws Exception {
    application = tempEntity.newApplicationWithParent();
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);
    boolean legacyViolations = true;
    testEvaluate_LegacyViolations(legacyViolations, true);

    application = tempEntity.newApplicationWithParent();
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);
    legacyViolations = false;
    testEvaluate_LegacyViolations(legacyViolations, true);
  }

  @Test
  public void testEvaluate_Results_LegacyViolations_Compliance_Stage() throws Exception {
    application = tempEntity.newApplicationWithParent();
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    Stage stage = new Stage(Stage.ID_COMPLIANCE);
    String scanId = simulateReportIsAvailable("report");

    Policy policy = newSecurityPolicy();
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);

    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    results.activeViolations.forEach(policyViolation -> {
      assertThat(policyViolation.isLegacyViolationApplied()).isFalse();
      assertThat(policyViolation.getLegacyViolationTime()).isNull();
      assertThat(policyViolation.isLegacyViolation()).isFalse();
    });
  }

  private void testEvaluate_LegacyViolations(
      boolean expectLegacyViolations,
      boolean legacyViolationsEnabled) throws Exception
  {
    reset(mockTelemetrySender);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    Policy policy = newSecurityPolicy();
    policy.setLegacyViolationAllowed(expectLegacyViolations);
    policyDAO.update(policy);

    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    if (expectLegacyViolations) {
      assertThat(results.activeViolations).isEmpty();
      List<PolicyViolation> inactiveViolations = getInactiveViolations(results);
      assertThat(inactiveViolations).hasSize(36).allSatisfy(inactiveViolation -> {
        assertThat(inactiveViolation.getLegacyViolationTime()).isEqualTo(results.evaluation.getTime());
        assertThat(inactiveViolation.isLegacyViolationApplied()).isTrue();
        assertThat(inactiveViolation.isWaived()).isFalse();
      });
    }
    else {
      assertThat(results.activeViolations).hasSize(36).allSatisfy(activeViolation -> {
        assertThat(activeViolation.isLegacyViolation()).isFalse();
        assertThat(activeViolation.isLegacyViolationApplied()).isFalse();
      });
    }

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender, times(3)).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate(application.getId()));
    expectedAttributes.put("owner_type", application.getType().toString());
    expectedAttributes.put("real_application_id", application.getId());
    expectedAttributes.put("grandfathering_enabled", String.valueOf(legacyViolationsEnabled));
    expectedAttributes.put("number_of_grandfathered_violations", expectLegacyViolations ? "36" : "0");
    if (expectLegacyViolations) {
      expectedAttributes.put("number_of_grandfathered_violations_with_low_threat_level", "0");
      expectedAttributes.put("number_of_grandfathered_violations_with_moderate_threat_level", "0");
      expectedAttributes.put("number_of_grandfathered_violations_with_severe_threat_level", "36");
      expectedAttributes.put("number_of_grandfathered_violations_with_critical_threat_level", "0");
      expectedAttributes.put("number_of_grandfathered_violations_in_security_policy_threat_category", "36");
      expectedAttributes.put("number_of_grandfathered_violations_in_license_policy_threat_category", "0");
      expectedAttributes.put("number_of_grandfathered_violations_in_quality_policy_threat_category", "0");
      expectedAttributes.put("number_of_grandfathered_violations_in_other_policy_threat_category", "0");
    }
    assertLegacyViolationAttributes(telemetryDataArgumentCaptor.getAllValues().get(1), expectedAttributes);
  }

  @Test
  public void testEvaluate_LegacyOnlyOnFirstEvaluation() throws Exception {
    application = tempEntity.newApplicationWithParent();
    Policy policy = newSecurityPolicy();
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    // This is the first evaluation. All policy violations should be legacy.
    String scanId1 = simulateReportIsAvailable("report");
    Stage stage1 = new Stage(Stage.ID_BUILD);
    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId1, stage1, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results1.activeViolations).isEmpty();
    List<PolicyViolation> inactiveViolations = getInactiveViolations(results1);
    assertThat(inactiveViolations).hasSize(36).allSatisfy(inactiveViolation -> {
      assertThat(inactiveViolation.getLegacyViolationTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(inactiveViolation.isWaived()).isFalse();
    });

    // Delete all violations
    inactiveViolations.forEach(policyViolationDAO::delete);

    // Evaluate again. No policy violations should be legacy.
    String scanId2 = simulateReportIsAvailable("report");
    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId2, stage1, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results2.activeViolations).hasSize(36);

    // Evaluate for a different stage. No policy violations should be legacy.
    String scanId3 = simulateReportIsAvailable("report");
    Stage stage2 = new Stage(Stage.ID_RELEASE);
    ScanPolicyEvaluatorResults results3 =
        scanPolicyEvaluator.evaluate(application, scanId3, stage2, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results3.activeViolations).hasSize(36);
  }

  @Test
  public void testEvaluate_LegacyViolationEvaluation_ComplianceStage() throws Exception {
    application = tempEntity.newApplicationWithParent();
    Policy policy = newSecurityPolicy();

    // This is the first evaluation using compliance stage. No policy violations should be legacy.
    String scanId1 = simulateReportIsAvailable("report");
    Stage stage1 = new Stage(Stage.ID_COMPLIANCE);
    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId1, stage1, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results1.activeViolations).hasSize(36).allSatisfy(inactiveViolation -> {
      assertThat(inactiveViolation.getLegacyViolationTime()).isNull();
      assertThat(inactiveViolation.isLegacyViolationApplied()).isFalse();
    });

    // setting up legacy violation
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    // Evaluate again. No policy violations(old policy violations and new policy violations) should be legacy.
    String scanId2 = simulateReportIsAvailable("report");
    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId2, stage1, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results2.activeViolations).hasSize(36).allSatisfy(inactiveViolation -> {
      assertThat(inactiveViolation.getLegacyViolationTime()).isNull();
      assertThat(inactiveViolation.isLegacyViolationApplied()).isFalse();
    });
    // check old policy violations are still not set to legacy
    assertThat(results1.activeViolations).hasSize(36).allSatisfy(inactiveViolation -> {
      assertThat(inactiveViolation.getLegacyViolationTime()).isNull();
      assertThat(inactiveViolation.isLegacyViolationApplied()).isFalse();
    });
  }

  @Test
  public void testEvaluate_LegacyIgnoredOnFirstEvaluation_MissingLicenseFeature() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);

    application = tempEntity.newApplicationWithParent();
    Policy policy = newSecurityPolicy();
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    // This is the first evaluation. No policy violations should be legacy given the license doesn't allow it.
    String scanId1 = simulateReportIsAvailable("report");
    Stage stage1 = new Stage(Stage.ID_BUILD);
    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId1, stage1, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results1.activeViolations).hasSize(36);
  }

  @Test
  public void testEvaluate_LegacyContinuesAfterFirstEvaluation_MissingLicenseFeature() throws Exception {
    application = tempEntity.newApplicationWithParent();
    Policy policy = newSecurityPolicy();
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    // This is the first evaluation. All policy violations should be legacy.
    String scanId1 = simulateReportIsAvailable("report");
    Stage stage1 = new Stage(Stage.ID_BUILD);
    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId1, stage1, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results1.activeViolations).isEmpty();
    List<PolicyViolation> inactiveViolations = getInactiveViolations(results1);
    assertThat(inactiveViolations).hasSize(36).allSatisfy(inactiveViolation -> {
      assertThat(inactiveViolation.getLegacyViolationTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(inactiveViolation.isWaived()).isFalse();
    });

    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_GRANDFATHERING);

    // Evaluate again with license without legacy violations. Policy violations continue to be legacy.
    String scanId2 = simulateReportIsAvailable("report");
    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId2, stage1, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results2.activeViolations).isEmpty();
    inactiveViolations = getInactiveViolations(results2);
    assertThat(inactiveViolations).hasSize(36).allSatisfy(inactiveViolation -> {
      assertThat(inactiveViolation.getLegacyViolationTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(inactiveViolation.isWaived()).isFalse();
    });
  }

  @Test
  public void testEvaluate_Results_NotifiableViolations() throws Exception {
    newRelativePopularityPolicy();

    Stage stage = new Stage(Stage.ID_BUILD);

    // 1st evaluation. The report contains one component that triggers the policy, so there is one notifiable violation.
    String scanId = simulateReportIsAvailable("testEvaluate_Results_NotifiableViolations/before");
    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results.activeViolations).hasSize(1);
    assertThat(results.notifiableViolations).hasSize(1);

    // 2nd evaluation. Nothing changed, so there are no new violations, so no notifiable violations.
    results = scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    assertThat(results.activeViolations).hasSize(1);
    assertThat(results.notifiableViolations).isEmpty();

    // 3rd evaluation. The report contains a new component that triggers the policy, so there is one new notifiable
    // violation.
    scanId = simulateReportIsAvailable("testEvaluate_Results_NotifiableViolations/after");
    results = scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    assertThat(results.activeViolations).hasSize(2);
    assertThat(results.notifiableViolations).hasSize(1);
    assertThat(results.evaluation.getCommitHash()).isNull();
    assertThat(results.evaluation.getBranchName()).isNull();
  }

  @Test
  public void testEvaluate_Results_NotifiableViolations_WithAutoWaiver_ThreatLevelOnly() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    newSecurityPolicy();

    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results1.activeViolations).hasSize(36);
    assertThat(results1.notifiableViolations).hasSize(36);

    AutoPolicyWaiver autoWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 10, false, true);

    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    // auto waived all the security violations
    assertThat(results2.activeViolations).isEmpty();
    assertThat(results2.autoWaivedViolations).hasSize(36);
    assertThat(results2.notifiableViolations).isEmpty();

    autoPolicyWaiverDAO.delete(autoWaiver);

    ScanPolicyEvaluatorResults results3 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results3.activeViolations).hasSize(36);
    assertThat(results3.autoWaivedViolations).isEmpty();
    assertThat(results3.notifiableViolations).isEmpty();
  }

  @Test
  public void testEvaluate_EmitsApplicationEvaluationEvent() throws IOException, InterruptedException {
    handler = new TestEventHandler<>(new CountDownLatch(1), ApplicationEvaluationEvent.class);
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);

    newSecurityPolicy();
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("report");

    asyncEventBus.register(handler);

    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(handler.getLatch().await(1, SECONDS)).isTrue();
    ApplicationEvaluationEvent event = handler.getEvent();
    assertThat(event).isNotNull();
    assertThat(event.stageTypeId).isEqualTo(Stage.ID_BUILD);
    assertThat(event.ownerId).isEqualTo(application.getId());
    assertThat(event.initiator).isEqualTo(USERNAME);
    assertThat(event.policyEvaluationId).isEqualTo(scanPolicyEvaluatorResults.evaluation.getId());
    assertThat(event.evaluationDate).isEqualTo(scanPolicyEvaluatorResults.evaluation.getTime());
    assertThat(event.affectedComponentCount).isEqualTo(7);
    assertThat(event.criticalComponentCount).isZero();
    assertThat(event.severeComponentCount).isEqualTo(7);
    assertThat(event.moderateComponentCount).isZero();
    assertThat(event.outcome).isEqualTo(Action.ID_FAIL);
    assertThat(event.commitHash).isEqualTo("testCommitHash");
    assertThat(event.branchName).isEqualTo("testBranchName");
  }

  @Test
  public void testEvaluate_DoesNot_EmitPolicyAlertEvent_WithoutWebhooks() throws IOException, InterruptedException {
    policyAlertHandler = new TestEventHandler<>(new CountDownLatch(1), PolicyAlertEvent.class);
    newSecurityPolicy();
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    asyncEventBus.register(policyAlertHandler);

    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    assertThat(policyAlertHandler.getLatch().await(5, SECONDS)).isFalse();
  }

  @Test
  public void testEvaluate_EmitsPolicyAlertEvent() throws IOException, InterruptedException {
    policyAlertHandler = new TestEventHandler<>(new CountDownLatch(1), PolicyAlertEvent.class);
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    tempEntity.newPolicy(application.getId(), "Test Policy", 10, Action.ID_WARN, Stage.ID_BUILD,
        new Notifications(new WebhookNotification("id", Stage.ID_BUILD)));

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    asyncEventBus.register(policyAlertHandler);

    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(policyAlertHandler.getLatch().await(5, SECONDS)).isTrue();
    PolicyAlertEvent event = policyAlertHandler.getEvent();
    assertThat(event).isNotNull();
    assertThat(event.applicationEvaluation.stageTypeId).isEqualTo(Stage.ID_BUILD);
    assertThat(event.applicationEvaluation.ownerId).isEqualTo(application.getId());
    assertThat(event.initiator).isEqualTo(USERNAME);
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
    scanPolicyEvaluator.evaluate(application, scanId1, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    assertThat(scanFile1).isFile();

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    String scanId2 = simulateReportIsAvailable("report");
    File scanFile2 = createScanFile(application, scanId2);
    scanPolicyEvaluator.evaluate(application, scanId2, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    assertThat(scanFile1).doesNotExist();
    assertThat(scanFile2).isFile();
  }

  @Test
  public void testEvaluate_DoesNotDeletePreviousScanFile_PurgeScanFilesWithReports() throws Exception {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.PURGE_SCAN_FILES, WITH_REPORTS);
    configuration.configurationChanged(Sets.newHashSet(SystemConfigurationProperty.PURGE_SCAN_FILES));

    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId1 = simulateReportIsAvailable("report");
    File scanFile1 = createScanFile(application, scanId1);
    scanPolicyEvaluator.evaluate(application, scanId1, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    assertThat(scanFile1).isFile();

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    String scanId2 = simulateReportIsAvailable("report");
    File scanFile2 = createScanFile(application, scanId2);
    scanPolicyEvaluator.evaluate(application, scanId2, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    assertThat(scanFile1).isFile();
    assertThat(scanFile2).isFile();
  }

  @Test
  public void testEvaluate_ReEvaluationDoesNotDeleteScanFile() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    String scanId = simulateReportIsAvailable("report");
    File scanFile = createScanFile(application, scanId);
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    assertThat(scanFile).isFile();

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    assertThat(scanFile).isFile();
  }

  @Test
  public void testEvaluate_DoesNotDeleteScanFileForDifferentStage() throws Exception {
    Stage stage1 = new Stage(Stage.ID_BUILD);

    String scanId1 = simulateReportIsAvailable("report");
    File scanFile1 = createScanFile(application, scanId1);
    scanPolicyEvaluator.evaluate(application, scanId1, stage1, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    assertThat(scanFile1).isFile();

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    Stage stage2 = new Stage(Stage.ID_RELEASE);
    String scanId2 = simulateReportIsAvailable("report");
    File scanFile2 = createScanFile(application, scanId2);
    scanPolicyEvaluator.evaluate(application, scanId2, stage2, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    assertThat(scanFile1).isFile();
    assertThat(scanFile2).isFile();
  }

  @Test
  public void testEvaluate_UpdateFixedViolations() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    Policy policy = newSecurityPolicy();

    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results1.allViolations).hasSize(36);

    policyDAO.delete(policy);

    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results2.allViolations).isEmpty();
    List<PolicyViolation> allViolations = policyViolationDAO.getByOwnerId(application.getId());
    assertThat(allViolations).hasSize(36);
    List<PolicyViolation> fixedViolations = allViolations.stream().filter(PolicyViolation::isFixed).toList();
    assertThat(fixedViolations).hasSize(36)
        .allSatisfy(violation -> assertThat(violation.getFixTime()).isEqualTo(results2.evaluation.getTime()));
  }

  @Test
  public void testEvaluate_UpdateAutoWaivedViolations_MultiplePolicies_NoPolicyWaiver() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    // There should be 36 violations per security policy
    Policy securityPolicyOne = new Policy(null, "Security Policy One");
    securityPolicyOne.setThreatLevel(5);
    securityPolicyOne.setOwnerId(application.getId());
    Constraint constraintOne = new Constraint(null, "TestConstraintOne", LogicalOperator.AND);
    constraintOne.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicyOne.addConstraint(constraintOne);
    tempEntity.newPolicy(securityPolicyOne);

    Policy securityPolicyTwo = new Policy(null, "Security Policy Two");
    securityPolicyTwo.setThreatLevel(9);
    securityPolicyTwo.setOwnerId(application.getId());
    Constraint constraintTwo = new Constraint(null, "TestConstraintTwo", LogicalOperator.AND);
    constraintTwo.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicyTwo.addConstraint(constraintTwo);
    tempEntity.newPolicy(securityPolicyTwo);

    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    Date openTime = results1.evaluation.getTime();

    assertThat(results1.activeViolations).hasSize(72);

    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 7, false, true);

    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results2.activeViolations).hasSize(36).allSatisfy(activeViolation -> {
      assertThat(activeViolation.getAutoPolicyWaiverId()).isNull();
      assertThat(activeViolation.getWaiveTime()).isNull();
      assertThat(activeViolation.getPolicyId()).isEqualTo(securityPolicyTwo.getId());
    });

    List<PolicyViolation> autoWaivedViolations = results2.autoWaivedViolations;
    assertThat(autoWaivedViolations).hasSize(36).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getOpenTime()).isEqualTo(openTime);
      assertThat(autoWaivedViolation.getFixTime()).isNull();
      assertThat(autoWaivedViolation.getWaiveTime()).isEqualTo(results2.evaluation.getTime());
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiver.getId());
    });

    autoPolicyWaiverDAO.delete(autoPolicyWaiver);

    ScanPolicyEvaluatorResults results3 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results3.activeViolations).hasSize(72).allSatisfy(unwaivedViolation -> {
      assertThat(unwaivedViolation.getFixTime()).isNull();
      assertThat(unwaivedViolation.getWaiveTime()).isNull();
      assertThat(unwaivedViolation.getAutoPolicyWaiverId()).isNull();
    });
    assertThat(results3.autoWaivedViolations).isEmpty();
    // 36 waived violations + 72 unwaived violations = 108
    assertThat(policyViolationDAO.getByOwnerId(application.getId())).hasSize(108);
  }

  @Test
  public void testEvaluate_AddAndRemoveAutoWaiver_SinglePolicy_NoPolicyWaiver() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    // There should be 36 violations per security policy
    Policy securityPolicy = new Policy(null, "Security Policy One");
    securityPolicy.setThreatLevel(5);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraintOne = new Constraint(null, "TestConstraintOne", LogicalOperator.AND);
    constraintOne.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraintOne);
    tempEntity.newPolicy(securityPolicy);

    // add auto waiver and evaluate
    AutoPolicyWaiver autoPolicyWaiverOne = tempEntity.newAutoPolicyWaiver(application.getId(), 6, false, true);
    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    List<PolicyViolation> autoWaivedViolations = results1.autoWaivedViolations;
    assertThat(autoWaivedViolations).hasSize(36).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getOpenTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(autoWaivedViolation.getFixTime()).isNull();
      assertThat(autoWaivedViolation.getWaiveTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverOne.getId());
    });

    // delete auto waiver and evaluate
    autoPolicyWaiverDAO.delete(autoPolicyWaiverOne);
    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results2.autoWaivedViolations).isEmpty();
    assertThat(results2.activeViolations).hasSize(36).allSatisfy(activeViolation -> {
      assertThat(activeViolation.getAutoPolicyWaiverId()).isNull();
      assertThat(activeViolation.getWaiveTime()).isNull();
    });

    // add another auto waiver and evaluate
    AutoPolicyWaiver autoPolicyWaiverTwo = tempEntity.newAutoPolicyWaiver(application.getId(), 3, false, true);
    ScanPolicyEvaluatorResults results3 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results3.autoWaivedViolations).isEmpty();
    assertThat(results3.activeViolations).hasSize(36).allSatisfy(activeViolation -> {
      assertThat(activeViolation.getAutoPolicyWaiverId()).isNull();
      assertThat(activeViolation.getWaiveTime()).isNull();
    });

    // Add a final auto waiver and evaluate
    autoPolicyWaiverDAO.delete(autoPolicyWaiverTwo);
    AutoPolicyWaiver autoPolicyWaiverThree = tempEntity.newAutoPolicyWaiver(application.getId(), 8, false, true);
    ScanPolicyEvaluatorResults results4 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results4.autoWaivedViolations).hasSize(36).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getOpenTime()).isEqualTo(results2.evaluation.getTime());
      assertThat(autoWaivedViolation.getFixTime()).isNull();
      assertThat(autoWaivedViolation.getWaiveTime()).isEqualTo(results4.evaluation.getTime());
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverThree.getId());
    });
    assertThat(results4.activeViolations).isEmpty();
  }

  @Test
  public void testEvaluate_AddAndRemovePolicyWaiver_SinglePolicy_WithAutoWaiver() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(6);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    // add auto policy waiver and evaluate
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 8, false, true);
    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results1.activeViolations).isEmpty();
    List<PolicyViolation> autoWaivedViolations = results1.autoWaivedViolations;
    assertThat(autoWaivedViolations).hasSize(36).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getOpenTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(autoWaivedViolation.getFixTime()).isNull();
      assertThat(autoWaivedViolation.getWaiveTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiver.getId());
    });

    // add policy waiver and evaluate
    PolicyWaiver waiver = tempEntity.newWaiver("f0776db1593e215146d2", securityPolicy.getId(), application.getId());
    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results2.activeViolations).isEmpty();
    assertThat(results2.waivedViolations).hasSize(3).allSatisfy(waivedViolation -> {
      assertThat(waivedViolation.getHash()).isEqualTo(waiver.getHash());
      assertThat(waivedViolation.getOpenTime()).isEqualTo(results2.evaluation.getTime());
      assertThat(waivedViolation.getFixTime()).isNull();
      assertThat(waivedViolation.getWaiveTime()).isEqualTo(results2.evaluation.getTime());
      assertThat(waivedViolation.getPolicyWaiverId()).isEqualTo(waiver.getId());
      assertThat(waivedViolation.getPolicyWaiverComment()).isEqualTo(waiver.getComment());
      assertThat(waivedViolation.getAutoPolicyWaiverId()).isNull();
    });
    assertThat(results2.autoWaivedViolations).hasSize(33).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getOpenTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(autoWaivedViolation.getFixTime()).isNull();
      assertThat(autoWaivedViolation.getWaiveTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiver.getId());
    });

    // remove policy waiver and evaluate
    policyWaiverDAO.delete(waiver);
    ScanPolicyEvaluatorResults results3 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results3.activeViolations).isEmpty();
    assertThat(results3.waivedViolations).isEmpty();
    assertThat(results3.autoWaivedViolations).hasSize(36).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getFixTime()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiver.getId());
    });

    List<PolicyViolation> newlyAutoWaivedViolations = results3.autoWaivedViolations.stream()
        .filter(
            autoWaivedViolation -> !autoWaivedViolation.getOpenTime().equals(results1.evaluation.getTime()))
        .toList();

    assertThat(newlyAutoWaivedViolations).hasSize(3).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getOpenTime()).isEqualTo(results3.evaluation.getTime());
      assertThat(autoWaivedViolation.getWaiveTime()).isEqualTo(results3.evaluation.getTime());
    });
  }

  @Test
  public void testEvaluate_AddAndRemoveAutoWaiver_SinglePolicy_WithPolicyWaiver() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(6);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    // add policy waiver and evaluate
    PolicyWaiver waiver = tempEntity.newWaiver("f0776db1593e215146d2", securityPolicy.getId(), application.getId());
    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results1.activeViolations).hasSize(33);
    assertThat(results1.waivedViolations).hasSize(3).allSatisfy(waivedViolation -> {
      assertThat(waivedViolation.getHash()).isEqualTo(waiver.getHash());
      assertThat(waivedViolation.getOpenTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(waivedViolation.getFixTime()).isNull();
      assertThat(waivedViolation.getWaiveTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(waivedViolation.getPolicyWaiverId()).isEqualTo(waiver.getId());
      assertThat(waivedViolation.getPolicyWaiverComment()).isEqualTo(waiver.getComment());
      assertThat(waivedViolation.getAutoPolicyWaiverId()).isNull();
    });
    assertThat(results1.autoWaivedViolations).isEmpty();

    // add auto waiver and evaluate
    AutoPolicyWaiver autoPolicyWaiverOne = tempEntity.newAutoPolicyWaiver(application.getId(), 8, false, true);
    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results2.activeViolations).isEmpty();
    assertThat(results2.waivedViolations).isEmpty();
    assertThat(results2.autoWaivedViolations).hasSize(33).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getOpenTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(autoWaivedViolation.getFixTime()).isNull();
      assertThat(autoWaivedViolation.getWaiveTime()).isEqualTo(results2.evaluation.getTime());
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverOne.getId());
    });

    // delete auto waiver and evaluate
    autoPolicyWaiverDAO.delete(autoPolicyWaiverOne);
    ScanPolicyEvaluatorResults results3 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results3.activeViolations).hasSize(33);
    assertThat(results3.autoWaivedViolations).isEmpty();
    assertThat(results3.waivedViolations).isEmpty();

    // delete policy waiver and evaluate
    policyWaiverDAO.delete(waiver);
    ScanPolicyEvaluatorResults results4 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results4.autoWaivedViolations).isEmpty();
    assertThat(results4.waivedViolations).isEmpty();
    assertThat(results4.activeViolations).hasSize(36);

    // add auto waiver and evaluate
    AutoPolicyWaiver autoPolicyWaiverTwo = tempEntity.newAutoPolicyWaiver(application.getId(), 8, false, true);
    ScanPolicyEvaluatorResults results5 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results5.activeViolations).isEmpty();
    assertThat(results5.waivedViolations).isEmpty();
    assertThat(results5.autoWaivedViolations).hasSize(36).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getFixTime()).isNull();
      assertThat(autoWaivedViolation.getWaiveTime()).isEqualTo(results5.evaluation.getTime());
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverTwo.getId());
    });

    List<PolicyViolation> newlyAutoWaivedViolations = results5.autoWaivedViolations.stream()
        .filter(
            autoWaivedViolation -> autoWaivedViolation.getOpenTime().equals(results4.evaluation.getTime()))
        .toList();

    assertThat(newlyAutoWaivedViolations).hasSize(3).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getOpenTime()).isEqualTo(results4.evaluation.getTime());
      assertThat(autoWaivedViolation.getWaiveTime()).isEqualTo(results5.evaluation.getTime());
    });

    // add policy waiver and evaluate
    PolicyWaiver waiverTwo = tempEntity.newWaiver("f0776db1593e215146d2", securityPolicy.getId(), application.getId());
    ScanPolicyEvaluatorResults results6 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results6.activeViolations).isEmpty();
    assertThat(results6.waivedViolations).hasSize(3).allSatisfy(waivedViolation -> {
      assertThat(waivedViolation.getHash()).isEqualTo(waiverTwo.getHash());
      assertThat(waivedViolation.getFixTime()).isNull();
      assertThat(waivedViolation.getWaiveTime()).isEqualTo(results6.evaluation.getTime());
      assertThat(waivedViolation.getPolicyWaiverId()).isEqualTo(waiverTwo.getId());
      assertThat(waivedViolation.getPolicyWaiverComment()).isEqualTo(waiverTwo.getComment());
      assertThat(waivedViolation.getAutoPolicyWaiverId()).isNull();
    });
    assertThat(results6.autoWaivedViolations).hasSize(33).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getFixTime()).isNull();
      assertThat(autoWaivedViolation.getWaiveTime()).isEqualTo(results5.evaluation.getTime());
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverTwo.getId());
    });
  }

  @Test
  public void testEvaluate_AddAndRemoveAutoWaiver_MultiplePolicies_NoPolicyWaiver() throws Exception {
    // setup
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    // There should be 36 violations per security policy
    Policy securityPolicyOne = new Policy(null, "Security Policy One");
    securityPolicyOne.setThreatLevel(5);
    securityPolicyOne.setOwnerId(application.getId());
    Constraint constraintOne = new Constraint(null, "TestConstraintOne", LogicalOperator.AND);
    constraintOne.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicyOne.addConstraint(constraintOne);
    tempEntity.newPolicy(securityPolicyOne);

    Policy securityPolicyTwo = new Policy(null, "Security Policy Two");
    securityPolicyTwo.setThreatLevel(9);
    securityPolicyTwo.setOwnerId(application.getId());
    Constraint constraintTwo = new Constraint(null, "TestConstraintTwo", LogicalOperator.AND);
    constraintTwo.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicyTwo.addConstraint(constraintTwo);
    tempEntity.newPolicy(securityPolicyTwo);

    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results1.activeViolations).hasSize(72);

    // add auto waiver and evaluate
    AutoPolicyWaiver autoPolicyWaiverOne = tempEntity.newAutoPolicyWaiver(application.getId(), 5, false, true);
    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results2.activeViolations).hasSize(36);
    assertThat(results2.autoWaivedViolations).hasSize(36).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getOpenTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(autoWaivedViolation.getFixTime()).isNull();
      assertThat(autoWaivedViolation.getWaiveTime()).isEqualTo(results2.evaluation.getTime());
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverOne.getId());
    });

    // remove auto waive and evaluate
    autoPolicyWaiverDAO.delete(autoPolicyWaiverOne);
    ScanPolicyEvaluatorResults results3 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results3.autoWaivedViolations).isEmpty();
    assertThat(results3.activeViolations).hasSize(72);

    List<PolicyViolation> unAutoWaivedViolations = results3.activeViolations.stream()
        .filter(
            unwaivedViolation -> unwaivedViolation.getOpenTime().equals(results3.evaluation.getTime()))
        .toList();
    assertThat(unAutoWaivedViolations).hasSize(36);

    // add ineffective auto waiver and evaluate
    tempEntity.newAutoPolicyWaiver(application.getId(), 3, false, true);
    ScanPolicyEvaluatorResults results4 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results4.autoWaivedViolations).isEmpty();
    assertThat(results4.activeViolations).hasSize(72);
  }

  @Test
  public void testEvaluate_AddAndRemoveAutoWaiver_MultiplePolicies_WithPolicyWaiver() throws Exception {
    // setup
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    // There should be 20 violations for this policy
    newPolicy(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));

    // There should be 36 violations per security policy
    Policy securityPolicyOne = new Policy(null, "Security Policy One");
    securityPolicyOne.setThreatLevel(5);
    securityPolicyOne.setOwnerId(application.getId());
    Constraint constraintOne = new Constraint(null, "TestConstraintOne", LogicalOperator.AND);
    constraintOne.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicyOne.addConstraint(constraintOne);
    tempEntity.newPolicy(securityPolicyOne);

    Policy securityPolicyTwo = new Policy(null, "Security Policy Two");
    securityPolicyTwo.setThreatLevel(9);
    securityPolicyTwo.setOwnerId(application.getId());
    Constraint constraintTwo = new Constraint(null, "TestConstraintTwo", LogicalOperator.AND);
    constraintTwo.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicyTwo.addConstraint(constraintTwo);
    tempEntity.newPolicy(securityPolicyTwo);

    tempEntity.newWaiver("f0776db1593e215146d2", securityPolicyOne.getId(), application.getId());

    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results1.activeViolations).hasSize(89);
    assertThat(results1.waivedViolations).hasSize(3);

    // add auto waiver and evaluate
    AutoPolicyWaiver autoPolicyWaiverOne = tempEntity.newAutoPolicyWaiver(application.getId(), 8, false, true);
    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results2.activeViolations).hasSize(56);
    assertThat(results2.autoWaivedViolations).hasSize(33).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getOpenTime()).isEqualTo(results1.evaluation.getTime());
      assertThat(autoWaivedViolation.getFixTime()).isNull();
      assertThat(autoWaivedViolation.getWaiveTime()).isEqualTo(results2.evaluation.getTime());
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiverOne.getId());
    });
    assertThat(results2.activeViolations).hasSize(56);

    // remove auto waive and evaluate
    autoPolicyWaiverDAO.delete(autoPolicyWaiverOne);
    ScanPolicyEvaluatorResults results3 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results3.autoWaivedViolations).isEmpty();
    assertThat(results3.activeViolations).hasSize(89);
    assertThat(results3.waivedViolations).isEmpty();

    // add ineffective auto waiver and evaluate
    tempEntity.newAutoPolicyWaiver(application.getId(), 1, false, false);
    ScanPolicyEvaluatorResults results4 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results4.autoWaivedViolations).isEmpty();
    assertThat(results4.activeViolations).hasSize(89);
    assertThat(results3.waivedViolations).isEmpty();
  }

  @Test
  public void testEvaluate_UpdateAutoWaivedViolations_SinglePolicy_WithPolicyWaiver() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(6);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    Date openTime = results1.evaluation.getTime();

    assertThat(results1.activeViolations).hasSize(36);

    PolicyWaiver waiver = tempEntity.newWaiver("f0776db1593e215146d2", securityPolicy.getId(), application.getId());
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 7, false, true);

    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results2.activeViolations).isEmpty();

    assertThat(results2.waivedViolations).hasSize(3).allSatisfy(waivedViolation -> {
      assertThat(waivedViolation.getHash()).isEqualTo(waiver.getHash());
      assertThat(waivedViolation.getOpenTime()).isEqualTo(openTime);
      assertThat(waivedViolation.getFixTime()).isNull();
      assertThat(waivedViolation.getWaiveTime()).isEqualTo(results2.evaluation.getTime());
      assertThat(waivedViolation.getPolicyWaiverId()).isEqualTo(waiver.getId());
      assertThat(waivedViolation.getPolicyWaiverComment()).isEqualTo(waiver.getComment());
    });

    assertThat(results2.autoWaivedViolations).hasSize(33).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getOpenTime()).isEqualTo(openTime);
      assertThat(autoWaivedViolation.getFixTime()).isNull();
      assertThat(autoWaivedViolation.getWaiveTime()).isEqualTo(results2.evaluation.getTime());
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiver.getId());
    });

    policyWaiverDAO.delete(waiver);

    ScanPolicyEvaluatorResults results3 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results3.waivedViolations).isEmpty();
    assertThat(results3.autoWaivedViolations).hasSize(36).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getFixTime()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiver.getId());
    });

    List<PolicyViolation> newlyAutoWaivedViolations = results3.autoWaivedViolations.stream()
        .filter(
            autoWaivedViolation -> !autoWaivedViolation.getOpenTime().equals(openTime))
        .toList();

    assertThat(newlyAutoWaivedViolations).hasSize(3).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getOpenTime()).isEqualTo(results3.evaluation.getTime());
      assertThat(autoWaivedViolation.getWaiveTime()).isEqualTo(results3.evaluation.getTime());
    });
  }

  @Test
  public void testEvaluate_UpdateAutoWaivedViolations_SinglePolicy_NoPolicyWaiver() throws Exception {
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(6);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    Date openTime = results1.evaluation.getTime();

    assertThat(results1.activeViolations).hasSize(36);

    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 7, false, true);

    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results2.activeViolations).isEmpty();

    List<PolicyViolation> autoWaivedViolations = results2.autoWaivedViolations;
    assertThat(autoWaivedViolations).hasSize(36).allSatisfy(autoWaivedViolation -> {
      assertThat(autoWaivedViolation.getOpenTime()).isEqualTo(openTime);
      assertThat(autoWaivedViolation.getFixTime()).isNull();
      assertThat(autoWaivedViolation.getWaiveTime()).isEqualTo(results2.evaluation.getTime());
      assertThat(autoWaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(autoWaivedViolation.getPolicyWaiverComment()).isNull();
      assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiver.getId());
    });

    autoPolicyWaiverDAO.delete(autoPolicyWaiver);

    ScanPolicyEvaluatorResults results3 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results3.activeViolations).hasSize(36).allSatisfy(unwaivedViolation -> {
      assertThat(unwaivedViolation.getOpenTime()).isEqualTo(results3.evaluation.getTime());
      assertThat(unwaivedViolation.getFixTime()).isNull();
      assertThat(unwaivedViolation.getWaiveTime()).isNull();
      assertThat(unwaivedViolation.getAutoPolicyWaiverId()).isNull();
    });
    assertThat(results3.autoWaivedViolations).isEmpty();
    assertThat(policyViolationDAO.getByOwnerId(application.getId())).hasSize(72);
  }

  @Test
  public void testEvaluate_UpdateWaivedViolations() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    Policy policy = newSecurityPolicy();

    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    Date openTime = results1.evaluation.getTime();

    assertThat(results1.activeViolations).hasSize(36);

    PolicyWaiver waiver = tempEntity.newWaiver("f0776db1593e215146d2", policy.getId(), application.getId());

    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results2.activeViolations).hasSize(33);
    List<PolicyViolation> waivedViolations = policyViolationDAO
        .getUnfixedByOwnerIdAndStageId(application.getId(), stage.getStageTypeId())
        .stream()
        .filter(PolicyViolation::isWaived)
        .toList();
    assertThat(waivedViolations).hasSize(3).allSatisfy(waivedViolation -> {
      assertThat(waivedViolation.getHash()).isEqualTo(waiver.getHash());
      assertThat(waivedViolation.getOpenTime()).isEqualTo(openTime);
      assertThat(waivedViolation.getFixTime()).isNull();
      assertThat(waivedViolation.getWaiveTime()).isEqualTo(results2.evaluation.getTime());
      assertThat(waivedViolation.getPolicyWaiverId()).isEqualTo(waiver.getId());
      assertThat(waivedViolation.getPolicyWaiverComment()).isEqualTo(waiver.getComment());
    });

    policyWaiverDAO.delete(waiver);

    ScanPolicyEvaluatorResults results3 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results3.activeViolations).hasSize(36);
    List<PolicyViolation> unfixedViolations = policyViolationDAO
        .getUnfixedByOwnerIdAndStageId(application.getId(), stage.getStageTypeId());
    assertThat(unfixedViolations.stream().filter(PolicyViolation::isWaived).toList()).isEmpty();
    List<PolicyViolation> unwaivedViolations = unfixedViolations.stream()
        .filter(violation -> violation.getHash().equals(waiver.getHash()))
        .toList();
    assertThat(unwaivedViolations).hasSize(3).allSatisfy(unwaivedViolation -> {
      assertThat(unwaivedViolation.getHash()).isEqualTo(waiver.getHash());
      assertThat(unwaivedViolation.getOpenTime()).isEqualTo(results3.evaluation.getTime());
      assertThat(unwaivedViolation.getFixTime()).isNull();
      assertThat(unwaivedViolation.getWaiveTime()).isNull();
      assertThat(unwaivedViolation.getPolicyWaiverId()).isNull();
      assertThat(unwaivedViolation.getPolicyWaiverComment()).isNull();
    });

    assertThat(policyViolationDAO.getByOwnerId(application.getId())).hasSize(39);
  }

  /**
   * If waiver is deleted and new waiver is created before re-evaluation (CLM-19768), the violation should be updated
   * with new waiver ID and comment, but waiveTime should remain the same
   */
  @Test
  public void testEvaluate_UpdateWaivedViolations_waiverIdHasChanged() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    Policy policy = newSecurityPolicy();

    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    Date openTime = results1.evaluation.getTime();

    assertThat(results1.activeViolations).hasSize(36);

    PolicyWaiver waiver = tempEntity.newWaiver("f0776db1593e215146d2", policy.getId(), application.getId(), "waiver1");

    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results2.activeViolations).hasSize(33);
    List<PolicyViolation> waivedViolations = policyViolationDAO
        .getUnfixedByOwnerIdAndStageId(application.getId(), stage.getStageTypeId())
        .stream()
        .filter(PolicyViolation::isWaived)
        .toList();
    assertThat(waivedViolations).hasSize(3).allSatisfy(waivedViolation -> {
      assertThat(waivedViolation.getHash()).isEqualTo(waiver.getHash());
      assertThat(waivedViolation.getOpenTime()).isEqualTo(openTime);
      assertThat(waivedViolation.getFixTime()).isNull();
      assertThat(waivedViolation.getWaiveTime()).isEqualTo(results2.evaluation.getTime());
      assertThat(waivedViolation.getPolicyWaiverId()).isEqualTo(waiver.getId());
      assertThat(waivedViolation.getPolicyWaiverComment()).isEqualTo("waiver1");
    });

    policyWaiverDAO.delete(waiver);
    PolicyWaiver waiver2 = tempEntity.newWaiver("f0776db1593e215146d2", policy.getId(), application.getId(), "waiver2");

    ScanPolicyEvaluatorResults results3 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results3.activeViolations).hasSize(33);
    waivedViolations = policyViolationDAO
        .getUnfixedByOwnerIdAndStageId(application.getId(), stage.getStageTypeId())
        .stream()
        .filter(PolicyViolation::isWaived)
        .toList();
    assertThat(waivedViolations).hasSize(3).allSatisfy(waivedViolation -> {
      assertThat(waivedViolation.getHash()).isEqualTo(waiver2.getHash());
      assertThat(waivedViolation.getOpenTime()).isEqualTo(openTime);
      assertThat(waivedViolation.getFixTime()).isNull();
      // waiveTime should still hold the original waive time
      assertThat(waivedViolation.getWaiveTime()).isEqualTo(results2.evaluation.getTime());
      assertThat(waivedViolation.getPolicyWaiverId()).isEqualTo(waiver2.getId());
      assertThat(waivedViolation.getPolicyWaiverComment()).isEqualTo("waiver2");
    });
  }

  @Test
  public void testEvaluate_SendsEvaluationTelemetry() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.WEB_UI,
        ClientScanType.SONATYPE, false);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender, times(3)).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("scan_id", scanId);
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate(application.getId()));
    expectedAttributes.put("real_application_id", application.getId());
    expectedAttributes.put("stage_id", Stage.ID_BUILD);
    expectedAttributes.put("scan_trigger_type", "WEB_UI");
    expectedAttributes.put("number_of_maven_components", "28");
    expectedAttributes.put("number_of_components", "28");
    assertPolicyEvaluationTelemetryData(telemetryDataArgumentCaptor.getAllValues().get(0), expectedAttributes);
  }

  @Test
  public void testSendEvaluationTelemetry_NoComponents() {
    scanPolicyEvaluator.sendEvaluationTelemetry("scanId", "applicationId", "stageId", ScanTriggerType.THIRD_PARTY,
        new ArrayList<>(), null, null);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("scan_id", "scanId");
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate("applicationId"));
    expectedAttributes.put("real_application_id", "applicationId");
    expectedAttributes.put("stage_id", "stageId");
    expectedAttributes.put("scan_trigger_type", "THIRD_PARTY");
    expectedAttributes.put("number_of_components", "0");
    assertPolicyEvaluationTelemetryData(telemetryDataArgumentCaptor.getValue(), expectedAttributes);
  }

  @Test
  public void testSendEvaluationTelemetry_UA_InstanceId() {
    String userAgent = "client/1.0 (Java 1.8.0; Linux 5.14.30; Other info)";
    scanPolicyEvaluator.sendEvaluationTelemetry("scanId", "applicationId", "stageId", ScanTriggerType.THIRD_PARTY,
        new ArrayList<>(), userAgent, "instanceId");

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("scan_id", "scanId");
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate("applicationId"));
    expectedAttributes.put("real_application_id", "applicationId");
    expectedAttributes.put("stage_id", "stageId");
    expectedAttributes.put("scan_trigger_type", "THIRD_PARTY");
    expectedAttributes.put("number_of_components", "0");
    expectedAttributes.put("client_id", "client");
    expectedAttributes.put("client_version", "1.0");
    expectedAttributes.put("client_runtime", "Java");
    expectedAttributes.put("client_runtime_version", "1.8.0");
    expectedAttributes.put("client_os_name", "Linux");
    expectedAttributes.put("client_os_version", "5.14.30");
    expectedAttributes.put("client_other", "Other info");
    expectedAttributes.put("client_instance_id", "instanceId");
    assertPolicyEvaluationTelemetryData(telemetryDataArgumentCaptor.getValue(), expectedAttributes);
  }

  @Test
  public void testSendEvaluationTelemetry() {
    Object[] formatsAndCounts = new Object[]{
      "unknown", 1, ComponentIdentifier.FORMAT_MAVEN, 2, ComponentIdentifier.FORMAT_NPM, 3,
      ComponentIdentifier.FORMAT_NUGET, 4, ComponentIdentifier.FORMAT_ANAME, 5, ComponentIdentifier.FORMAT_PYPI, 6,
      ComponentIdentifier.FORMAT_RPM, 7, ComponentIdentifier.FORMAT_RUBYGEMS, 8
    };

    scanPolicyEvaluator.sendEvaluationTelemetry("scanId", "applicationId", "stageId",
        ScanTriggerType.CONTINUOUS_INTEGRATION, components(formatsAndCounts), null, null);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("scan_id", "scanId");
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate("applicationId"));
    expectedAttributes.put("real_application_id", "applicationId");
    expectedAttributes.put("stage_id", "stageId");
    expectedAttributes.put("scan_trigger_type", "CONTINUOUS_INTEGRATION");
    expectedAttributes.put("number_of_unknown_components", "1");
    expectedAttributes.put("number_of_maven_components", "2");
    expectedAttributes.put("number_of_npm_components", "3");
    expectedAttributes.put("number_of_nuget_components", "4");
    expectedAttributes.put("number_of_aname_components", "5");
    expectedAttributes.put("number_of_pypi_components", "6");
    expectedAttributes.put("number_of_rpm_components", "7");
    expectedAttributes.put("number_of_gem_components", "8");
    expectedAttributes.put("number_of_components", "36");
    assertPolicyEvaluationTelemetryData(telemetryDataArgumentCaptor.getValue(), expectedAttributes);
  }

  @Test
  public void testSendLegacyViolationCounts_NoLegacyViolations() {
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation(application.getId(), "stageId", "scanId", CurrentUser.SYSTEM, ScanTriggerType.CLI);
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolations.add(policyViolation(policyEvaluation, 1, PolicyThreatCategory.LICENSE, false));
    policyViolations.add(policyViolation(policyEvaluation, 3, PolicyThreatCategory.SECURITY, false));
    policyViolations.add(policyViolation(policyEvaluation, 5, PolicyThreatCategory.QUALITY, false));
    policyViolations.add(policyViolation(policyEvaluation, 7, PolicyThreatCategory.OTHER, false));

    scanPolicyEvaluator.sendLegacyViolationTelemetryData(application, policyViolations, Stage.ID_BUILD);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate(application.getId()));
    expectedAttributes.put("owner_type", application.getType().toString());
    expectedAttributes.put("real_application_id", application.getId());
    expectedAttributes.put("grandfathering_enabled", "true");
    expectedAttributes.put("number_of_grandfathered_violations", "0");
    assertLegacyViolationAttributes(telemetryDataArgumentCaptor.getValue(), expectedAttributes);
  }

  @Test
  public void testSendLegacyViolationCounts() {
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation(application.getId(), "stageId", "scanId", CurrentUser.SYSTEM, ScanTriggerType.CLI);
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

    scanPolicyEvaluator.sendLegacyViolationTelemetryData(application, policyViolations, Stage.ID_RELEASE);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate(application.getId()));
    expectedAttributes.put("owner_type", application.getType().toString());
    expectedAttributes.put("real_application_id", application.getId());
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
    assertLegacyViolationAttributes(telemetryDataArgumentCaptor.getValue(), expectedAttributes);
  }

  private PolicyViolation policyViolation(
      PolicyEvaluation policyEvaluation,
      int threatLevel,
      PolicyThreatCategory policyThreatCategory,
      boolean legacyViolation)
  {
    ConstraintFact constraintFact = new ConstraintFact("json", "json", "json");
    PolicyViolation policyViolation = new PolicyViolation(policyEvaluation, "policyId", "policyName", threatLevel,
        policyThreatCategory, "hash", null, List.of(constraintFact), "filename");
    if (legacyViolation) {
      policyViolation.setLegacyViolationTime(new Date());
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
    return switch (format) {
      case ComponentIdentifier.FORMAT_MAVEN -> new Component(ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
      case ComponentIdentifier.FORMAT_NPM -> new Component(ComponentIdentifier.createNpmCoordinates("p", "v"));
      case ComponentIdentifier.FORMAT_NUGET -> new Component(ComponentIdentifier.createNugetCoordinates("p", "v"));
      case ComponentIdentifier.FORMAT_ANAME -> new Component(ComponentIdentifier.createAnameCoordinates("n", "q", "v"));
      case ComponentIdentifier.FORMAT_PYPI -> new Component(
          ComponentIdentifier.createPypiCoordinates("n", "v", "q", "e"));
      case ComponentIdentifier.FORMAT_RPM -> new Component(ComponentIdentifier.createRpmCoordinates("n", "v", "a"));
      case ComponentIdentifier.FORMAT_RUBYGEMS -> new Component(
          ComponentIdentifier.createRubyGemsCoordinates("n", "v", "p"));
      default -> new Component();
    };
  }

  private void assertPolicyEvaluationTelemetryData(
      TelemetryData telemetryData,
      Map<String, Object> expectedAttributes)
  {
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }

  private void assertLegacyViolationAttributes(TelemetryData telemetryData, Map<String, Object> expectedAttributes) {
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose())
        .isEqualTo(TelemetryPurpose.APPLICATION_EVALUATION_LEGACY_VIOLATION_COUNTS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }

  private void assertStaleScanAttributes(TelemetryData telemetryData, Map<String, Object> expectedAttributes) {
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose())
        .isEqualTo(TelemetryPurpose.STALE_REPORT_REEVALUATION);
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
        StandardCharsets.UTF_8);
    constraintFactsJson = constraintFactsJson.replace("TestConstraintId", policy.getConstraints().get(0).getId());

    List<ConstraintFact> constraintFacts = Arrays.asList(JsonUtils.parse(constraintFactsJson, ConstraintFact[].class));
    PolicyViolation policyViolationBefore = new PolicyViolation(policyEvaluationBefore, policy.getId(),
        policy.getName(), policy.getThreatLevel(), policy.getThreatCategory(), "964cd74171f427720480",
        componentIdentifier, constraintFacts, "commons-httpclient-3.1.jar");
    policyViolationDAO.insert(policyViolationBefore);
    assertThat(policyViolationBefore.getOpenTime()).isEqualTo(beforeTime);

    // Evaluate the policy.
    String scanId = simulateReportIsAvailable("testEvaluate_BeforeAndAfterAddingConditionTriggerData/report");
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    // There should be only one policy violation (the existing one).
    List<PolicyViolation> policyViolationsAfter = policyViolationDAO.getByOwnerId(application.getId());
    policyViolationDAO.loadConstraintFacts(policyViolationsAfter);
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
  public void testEvaluate_LegacyViolationsNotConfiguredForAppOrOrg() throws Exception {
    organization.setLegacyViolationEnabled(null);
    organization.setAllowLegacyViolationOverride(true);
    organizationDAO.update(organization);
    application.setLegacyViolationEnabled(null);
    applicationDAO.update(application);

    testEvaluate_LegacyViolations(false, false);
  }

  @Test
  public void testEvaluate_LegacyViolationsEnabledForApp_AppCanOverrideLegacyViolations() throws Exception {
    organization.setLegacyViolationEnabled(false);
    organization.setAllowLegacyViolationOverride(true);
    organizationDAO.update(organization);
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    testEvaluate_LegacyViolations(true, true);
  }

  @Test
  public void testEvaluate_LegacyViolationsDisabledForApp_AppCanOverrideLegacyViolations() throws Exception {
    organization.setLegacyViolationEnabled(true);
    organization.setAllowLegacyViolationOverride(true);
    organizationDAO.update(organization);
    application.setLegacyViolationEnabled(false);
    applicationDAO.update(application);

    testEvaluate_LegacyViolations(false, false);
  }

  @Test
  public void testEvaluate_LegacyViolationsEnabledForApp_DisabledForOrg_AppCannotOverrideLegacyViolations() throws Exception {
    organization.setLegacyViolationEnabled(false);
    organization.setAllowLegacyViolationOverride(false);
    organizationDAO.update(organization);
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    testEvaluate_LegacyViolations(false, false);
  }

  @Test
  public void testEvaluate_LegacyViolationsDisabledForApp_DisabledForOrg_AppCannotOverrideLegacyViolations() throws Exception {
    organization.setLegacyViolationEnabled(false);
    organization.setAllowLegacyViolationOverride(false);
    organizationDAO.update(organization);
    application.setLegacyViolationEnabled(false);
    applicationDAO.update(application);

    testEvaluate_LegacyViolations(false, false);
  }

  @Test
  public void testEvaluate_LegacyViolationsEnabledForApp_EnabledForOrg_AppCannotOverrideLegacyViolations() throws Exception {
    organization.setLegacyViolationEnabled(true);
    organization.setAllowLegacyViolationOverride(false);
    organizationDAO.update(organization);
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    testEvaluate_LegacyViolations(true, true);
  }

  @Test
  public void testEvaluate_LegacyViolationsDisabledForApp_EnabledForOrg_AppCannotOverrideLegacyViolations() throws Exception {
    organization.setLegacyViolationEnabled(true);
    organization.setAllowLegacyViolationOverride(false);
    organizationDAO.update(organization);
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    testEvaluate_LegacyViolations(true, true);
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

    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

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
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

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
    scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(scanPolicyEvaluatorResults.activeViolations).isEmpty();
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
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

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
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(scanPolicyEvaluatorResults.allViolations).hasSize(3);
    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(2);

    List<PolicyViolation> policyViolations = policyViolationDAO.getUnfixedByOwnerIdAndStageId(application.getId(),
        stage.getStageTypeId());
    assertThat(policyViolations).hasSize(3);
    assertThat(policyViolations).filteredOn(violation -> componentHash.equals(violation.getHash()))
        .hasSize(1)
        .allSatisfy(policyViolation -> {
          assertThat(policyViolation.isWaived()).isTrue();
          assertThat(policyViolation.getPolicyWaiverId()).isEqualTo(policyWaiver.getId());
          assertThat(policyViolation.getPolicyWaiverComment()).isEqualTo(policyWaiver.getComment());
        });
    assertThat(policyViolations).filteredOn(violation -> !componentHash.equals(violation.getHash()))
        .allSatisfy(policyViolation -> assertThat(policyViolation.isWaived()).isFalse());
  }

  @Test
  public void testEvaluate_InvalidStage() {
    assertThatExceptionOfType(InvalidStageException.class)
        .isThrownBy(() -> scanPolicyEvaluator.evaluate(application, "scanid", new Stage("foobar"), ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false))
        .withMessage("Invalid stage id=foobar");
  }

  @Test
  public void testEvaluate_MissingReport() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> scanPolicyEvaluator.evaluate(application, "scanId", new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
                ClientScanType.SONATYPE, false))
        .withMessage("Could not download the report for scan ID scanId");

    PolicyEvaluation eval = policyEvaluationDAO.getLastByOwnerIdAndStageId(application.getId(),
        Stage.ID_BUILD);
    assertThat(eval).isNull();
  }

  @Test
  public void testEvaluate_ErrorReport() {
    String scanId = simulateReportIsAvailable("empty_report");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
                ClientScanType.SONATYPE, false))
        .withMessage("Unable to fetch report data, the scan " + scanId + " could not be processed.");

    PolicyEvaluation eval = policyEvaluationDAO.getLastByOwnerIdAndStageId(application.getId(),
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
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(1);
    assertContainsPolicyViolation(componentIdentifier, hash, policy, constraint, Action.ID_FAIL,
        MatchStateConditionType.ID, scanPolicyEvaluatorResults.activeViolations);
  }

  @Test
  public void testEvaluate_ClaimedComponentHasLegalNonePolicyViolation() throws Exception {
    Constraint constraintLicense1 =
        new Constraint(null /* constraintId */, "Constraint License No-Sources", LogicalOperator.AND);
    Condition condition1 = new Condition(LicenseConditionType.ID, "is", License.NO_SOURCES_ID);
    constraintLicense1.addCondition(condition1);
    Constraint constraintLicense2 =
        new Constraint(null /* constraintId */, "Constraint License Not-Declared", LogicalOperator.AND);
    Condition condition2 = new Condition(LicenseConditionType.ID, "is", License.NOT_DECLARED_ID);
    constraintLicense2.addCondition(condition2);
    Constraint constraintLicense3 =
        new Constraint(null /* constraintId */, "Constraint Unknown Component", LogicalOperator.AND);
    Condition condition3 = new Condition(MatchStateConditionType.ID, "is", "unknown");
    constraintLicense3.addCondition(condition3);

    Policy policy1 = new Policy(null /* policyId */, "License-No-Sources");
    policy1.setThreatLevel(9);
    policy1.addConstraint(constraintLicense1);
    policy1.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy1.setOwnerId(application.getId());
    tempEntity.newPolicy(policy1);

    Policy policy2 = new Policy(null /* policyId */, "License-Not-Declared");
    policy2.setThreatLevel(9);
    policy2.addConstraint(constraintLicense2);
    policy2.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy2.setOwnerId(application.getId());
    tempEntity.newPolicy(policy2);

    Policy policy3 = new Policy(null /* policyId */, "Unknown Component");
    policy3.setThreatLevel(2);
    policy3.addConstraint(constraintLicense3);
    policy3.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policy3.setOwnerId(application.getId());
    tempEntity.newPolicy(policy3);

    constraintLicense1 = policy1.getConstraints().get(0);
    constraintLicense2 = policy2.getConstraints().get(0);
    constraintLicense3 = policy3.getConstraints().get(0);

    Stage stage = new Stage(Stage.ID_BUILD);

    String hash = "5801a1a27a36f88e2089";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("G", "A", "V");

    String scanId = simulateReportIsAvailable("ManuallyIdentifiedComponent/report");
    tempEntity.newClaimedComponent(hash, componentIdentifier);
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(2);
    assertContainsPolicyViolation(componentIdentifier, hash, policy1, constraintLicense1, Action.ID_FAIL,
        LicenseConditionType.ID, scanPolicyEvaluatorResults.activeViolations);
    assertContainsPolicyViolation(componentIdentifier, hash, policy2, constraintLicense2, Action.ID_FAIL,
        LicenseConditionType.ID, scanPolicyEvaluatorResults.activeViolations);
    assertNotContainsPolicyViolation(componentIdentifier, hash, policy3, constraintLicense3, Action.ID_FAIL,
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
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

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
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

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
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    PolicyEvaluation policyEvaluation1 = policyEvaluationDAO.getLastByOwnerIdAndStageId(application.getId(),
        stage.getStageTypeId());
    List<PolicyViolation> policyViolations1 = policyViolationDAO.getActiveByOwnerIdAndStageId(application.getId(),
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
    policyDAO.update(policy);
    // Evaluate policy again for the same scan
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    PolicyEvaluation policyEvaluation2 = policyEvaluationDAO.getLastByOwnerIdAndStageId(application.getId(),
        stage.getStageTypeId());
    assertThat(policyEvaluation1.getId()).isNotEqualTo(policyEvaluation2.getId());
    List<PolicyViolation> policyViolations2 = policyViolationDAO.getActiveByOwnerIdAndStageId(application.getId(),
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
    scanPolicyEvaluator.evaluate(application, scanBuildId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    PolicyEvaluation policyEvaluationBuild = policyEvaluationDAO
        .getLastByOwnerIdAndStageId(application.getId(), Stage.ID_BUILD);
    List<PolicyViolation> policyViolationsBuild = policyViolationDAO
        .getActiveByOwnerIdAndStageId(application.getId(), Stage.ID_BUILD);
    assertThat(policyViolationsBuild).hasSize(1);
    assertThat(policyViolationsBuild.get(0).getComponentIdentifier().get(ComponentIdentifier.MAVEN_GROUP_ID))
        .isEqualTo("commons-pool");
    assertThat(policyViolationsBuild.get(0).getOpenTime()).isEqualTo(policyEvaluationBuild.getTime());

    // Evaluate policy for the Release stage
    String scanReleaseId = simulateReportIsAvailable("report");
    scanPolicyEvaluator.evaluate(application, scanReleaseId, new Stage(Stage.ID_RELEASE),
        ScanTriggerType.CLI, ClientScanType.SONATYPE, false);
    PolicyEvaluation policyEvaluationRelease = policyEvaluationDAO
        .getLastByOwnerIdAndStageId(application.getId(), Stage.ID_RELEASE);
    List<PolicyViolation> policyViolationsRelease = policyViolationDAO
        .getActiveByOwnerIdAndStageId(application.getId(), Stage.ID_RELEASE);
    assertThat(policyViolationsRelease).hasSize(1);
    assertThat(policyViolationsRelease.get(0).getComponentIdentifier().get(ComponentIdentifier.MAVEN_GROUP_ID))
        .isEqualTo("commons-pool");
    assertThat(policyViolationsRelease.get(0).getOpenTime()).isEqualTo(policyEvaluationRelease.getTime());

    policyViolationsBuild = policyViolationDAO.getActiveByOwnerIdAndStageId(application.getId(), Stage.ID_BUILD);
    assertThat(policyViolationsBuild).hasSize(1);
    assertThat(policyViolationsBuild.get(0).getOpenTime()).isEqualTo(policyEvaluationBuild.getTime());
  }

  @Test
  public void testEvaluate_PersistApplicationComponents() throws Exception {
    Stage stage1 = new Stage(Stage.ID_BUILD);
    Stage stage2 = new Stage(Stage.ID_RELEASE);

    // Evaluate policy
    assertThat(applicationComponentDAO.getByOwnerIdAndStageTypeId(application.getId(), stage1.getStageTypeId()))
        .isEmpty();
    String scanId1 = simulateReportIsAvailable("PersistApplicationComponents/report1");
    scanPolicyEvaluator.evaluate(application, scanId1, stage1, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    List<OwnerComponent> appComponents1 =
        applicationComponentDAO.getByOwnerIdAndStageTypeId(application.getId(),
            stage1.getStageTypeId());
    assertThat(appComponents1).hasSize(1);
    OwnerComponent appComponent1 = appComponents1.get(0);
    PolicyEvaluation policyEvaluation1 = policyEvaluationDAO.getLastByOwnerIdAndStageId(application.getId(),
        stage1.getStageTypeId());
    ComponentIdentifier commonsDbcpComponentIdentifier = ComponentIdentifier.createMavenCoordinates("commons-dbcp",
        "commons-dbcp", "1.4");
    assertApplicationComponent(commonsDbcpComponentIdentifier, policyEvaluation1.getTime(), appComponent1);

    // Evaluate policy for a different stage. It should not touch the app<->component assocs for the first stage.
    assertThat(applicationComponentDAO.getByOwnerIdAndStageTypeId(application.getId(), stage2.getStageTypeId()))
        .isEmpty();
    String scanId2 = simulateReportIsAvailable("PersistApplicationComponents/report2");
    scanPolicyEvaluator.evaluate(application, scanId2, stage2, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    List<OwnerComponent> appComponents2 =
        applicationComponentDAO.getByOwnerIdAndStageTypeId(application.getId(),
            stage2.getStageTypeId());
    assertThat(appComponents2).hasSize(1);
    OwnerComponent appComponent2 = appComponents2.get(0);
    PolicyEvaluation policyEvaluation2 = policyEvaluationDAO.getLastByOwnerIdAndStageId(application.getId(),
        stage2.getStageTypeId());
    ComponentIdentifier geronimoTomcatComponentIdentifier = ComponentIdentifier.createMavenCoordinates("geronimo",
        "geronimo-tomcat", "1.0");
    assertApplicationComponent(geronimoTomcatComponentIdentifier, policyEvaluation2.getTime(), appComponent2);
    appComponents1 =
        applicationComponentDAO.getByOwnerIdAndStageTypeId(application.getId(), stage1.getStageTypeId());
    assertThat(appComponents1).hasSize(1);
    assertApplicationComponent(commonsDbcpComponentIdentifier, policyEvaluation1.getTime(), appComponents1.get(0));
    assertThat(appComponents1.get(0).getId()).isEqualTo(appComponent1.getId());

    // Evaluate again for the first stage. It should replace the app<->component assocs for the first stage and it
    // should not touch the app<->component assocs for the second stage.
    String scanId3 = simulateReportIsAvailable("PersistApplicationComponents/report3");
    scanPolicyEvaluator.evaluate(application, scanId3, stage1, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    List<OwnerComponent> appComponents3 =
        applicationComponentDAO.getByOwnerIdAndStageTypeId(application.getId(),
            stage1.getStageTypeId());
    assertThat(appComponents3).hasSize(1);
    OwnerComponent appComponent3 = appComponents3.get(0);
    policyEvaluation1 = policyEvaluationDAO.getLastByOwnerIdAndStageId(application.getId(),
        stage1.getStageTypeId());
    ComponentIdentifier tomcatUtilCOmponentIdentifier = ComponentIdentifier.createMavenCoordinates("tomcat",
        "tomcat-util", "5.5.23");
    assertApplicationComponent(tomcatUtilCOmponentIdentifier, policyEvaluation1.getTime(), appComponent3);
    appComponents2 =
        applicationComponentDAO.getByOwnerIdAndStageTypeId(application.getId(), stage2.getStageTypeId());
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
        .evaluate(application, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(scanPolicyEvaluatorResults.allViolations).hasSize(3);
    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(2);

    LifecycleReport lifecycleReport = reportService.getReport(application.getId(), scanId);
    // Verify the policyalerts.json report file
    ReportEntry policyAlertsReportEntry = lifecycleReport.getEntry(POLICY_ALERTS.getName());
    List<PolicyAlert> policyAlerts = Arrays.asList(JsonUtils.parse(policyAlertsReportEntry.buf, PolicyAlert[].class));
    assertThat(policyAlerts).extracting(PolicyAlert::getTrigger) //
        .flatExtracting(PolicyFact::getComponentFacts) //
        .extracting(ComponentFact::getHash) //
        .containsExactlyInAnyOrder("3e1470773021fde54f51", "e93e551d738e9f4d1aae");
    assertThat(policyAlerts).flatExtracting(PolicyAlert::getActions).isNotEmpty();
    // Verify the policythreats.json report file
    ReportEntry policyThreatsReportEntry = lifecycleReport.getEntry(POLICY_THREATS.getName());
    PolicyThreats policyThreats = JsonUtils.parse(policyThreatsReportEntry.buf, PolicyThreats.class);
    assertThat(policyThreats.stageTypeId).isEqualTo("build");
    assertThat(policyThreats.aaData) //
        .extracting(component -> component.hash) //
        .containsExactlyInAnyOrder("3e1470773021fde54f51", "e93e551d738e9f4d1aae", "f2e35e4a21f07d25710f");
    // Verify the data.json report file
    ReportEntry dataReportEntry = lifecycleReport.getEntry(DATA_JSON.getName());
    ObjectNode data = JsonUtils.parse(dataReportEntry.buf);
    assertThat(data.get("policyCounts").toString()).isEqualTo("[1,0,0,0,0,2,0,0,0,0,0]");
    assertThat(data.get("policyComponentCount").asInt()).isEqualTo(2);
    assertThat(data.get("grandfatheredPolicyViolationCount").asInt()).isZero();
    assertThat(data.get("legacyViolationCount").asInt()).isZero();
    validatePolicyValidationOwner(policyThreats.aaData, application);
  }

  private void validatePolicyValidationOwner(List<PolicyThreats.Component> components, Owner owner) {
    String ownerId = owner.getId();
    String ownerType = owner.getType().toString();
    components.stream()
        .flatMap(component -> component.allViolations.stream())
        .forEach(policyViolation -> {
          assertThat(policyViolation.policyOwnerId).isEqualTo(ownerId);
          assertThat(policyViolation.policyOwnerType).isEqualTo(ownerType);
        });
  }

  @Test
  public void testEvaluate_UpdatesReportFiles_LegacyViolations() throws Exception {
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);
    // The policy will cause three policy violations.
    Policy policy = newPolicy(new Condition(LicenseConditionType.ID, "is", "GPL-2.0"));
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);
    String scanId = simulateReportIsAvailable("report");

    // Evaluate policy
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(scanPolicyEvaluatorResults.allViolations).hasSize(3);
    assertThat(scanPolicyEvaluatorResults.activeViolations).isEmpty();

    LifecycleReport lifecycleReport = reportService.getReport(application.getId(), scanId);
    // Verify the policyalerts.json report file
    ReportEntry policyAlertsReportEntry = lifecycleReport.getEntry(POLICY_ALERTS.getName());
    List<PolicyAlert> policyAlerts = Arrays.asList(JsonUtils.parse(policyAlertsReportEntry.buf, PolicyAlert[].class));
    assertThat(policyAlerts).isEmpty();
    // Verify the policythreats.json report file
    ReportEntry policyThreatsReportEntry = lifecycleReport.getEntry(POLICY_THREATS.getName());
    PolicyThreats policyThreats = JsonUtils.parse(policyThreatsReportEntry.buf, PolicyThreats.class);
    assertThat(policyThreats.aaData) //
        .extracting(component -> component.hash) //
        .containsExactlyInAnyOrder("3e1470773021fde54f51", "e93e551d738e9f4d1aae", "f2e35e4a21f07d25710f");
    // Verify the data.json report file
    ReportEntry dataReportEntry = lifecycleReport.getEntry(DATA_JSON.getName());
    ObjectNode data = JsonUtils.parse(dataReportEntry.buf);
    // All three policy violations are legacy and so each of the three components has a policyThreatLevel of 0
    assertThat(data.get("policyCounts").toString()).isEqualTo("[3,0,0,0,0,0,0,0,0,0,0]");
    // Since each component has a policyThreatLevel of 0, there are 0 affected components
    assertThat(data.get("policyComponentCount").asInt()).isEqualTo(0);
    assertThat(data.get("grandfatheredPolicyViolationCount").asInt()).isEqualTo(3);
    assertThat(data.get("legacyViolationCount").asInt()).isEqualTo(3);
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
        .evaluate(application, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(scanPolicyEvaluatorResults.allViolations).hasSize(3);
    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(2);

    LifecycleReport lifecycleReport = reportService.getReport(application.getId(), scanId);
    // Verify the policyalerts.json report file
    ReportEntry policyAlertsReportEntry = lifecycleReport.getEntry(POLICY_ALERTS.getName());
    List<PolicyAlert> policyAlerts = Arrays.asList(JsonUtils.parse(policyAlertsReportEntry.buf, PolicyAlert[].class));
    assertThat(policyAlerts).extracting(PolicyAlert::getTrigger) //
        .flatExtracting(PolicyFact::getComponentFacts) //
        .extracting(ComponentFact::getHash) //
        .containsExactlyInAnyOrder("3e1470773021fde54f51", "e93e551d738e9f4d1aae");
    assertThat(policyAlerts).flatExtracting(PolicyAlert::getActions).isEmpty();
    // Verify the policythreats.json report file
    ReportEntry policyThreatsReportEntry = lifecycleReport.getEntry(POLICY_THREATS.getName());
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
        .evaluate(application, scanBuildId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    PolicyEvaluationResult evaluationResult = scanPolicyEvaluator.createPolicyEvaluationResult(results.evaluation,
        results.allViolations, true);
    assertThat(evaluationResult.getAlerts()).hasSize(1);
    PolicyAlert alert = evaluationResult.getAlerts().get(0);
    assertThat(alert.getActions().get(0).getActionTypeId()).isEqualTo(Action.ID_FAIL);
  }

  @Test
  public void testEvaluate_ActionFailInFirewallForContainerImages() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.ENFORCEMENT);
    testProductLicense.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    // We need to create an actual repository and set it in the organization
    Repository repository = createTestRepository();
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    Policy policy =
        newPolicy(new Condition(CoordinatesConditionType.ID, "match", "maven:commons-pool:commons-pool:1.4"));
    policy.getActions().put(Stage.ID_PROXY, Action.ID_FAIL);
    policy.setOwnerId(repository.getId());
    policyDAO.update(policy);
    String scanBuildId = simulateReportIsAvailable("report");

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanBuildId,
        new Stage(Stage.ID_PROXY), ScanTriggerType.CLI, ClientScanType.SONATYPE, false);

    PolicyEvaluationResult evaluationResult =
        scanPolicyEvaluator.createPolicyEvaluationResult(results.evaluation, results.allViolations, true);

    assertThat(evaluationResult.getAlerts()).hasSize(1);
    PolicyAlert alert = evaluationResult.getAlerts().get(0);
    assertThat(alert.getActions().get(0).getActionTypeId()).isEqualTo(Action.ID_FAIL);
  }

  @Test
  public void testCreatePolicyEvaluationResult_ActionFailDisabledInFirewallForContainerImages() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.ENFORCEMENT);
    testProductLicense.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    // We need to create an actual repository and set it in the organization
    Repository repository = createTestRepository();
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    Policy policy =
        newPolicy(new Condition(CoordinatesConditionType.ID, "match", "maven:commons-pool:commons-pool:1.4"));
    policy.getActions().put(Stage.ID_PROXY, Action.ID_FAIL);
    policy.setOwnerId(repository.getId());
    policyDAO.update(policy);
    String scanBuildId = simulateReportIsAvailable("report");

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanBuildId,
        new Stage(Stage.ID_PROXY), ScanTriggerType.CLI, ClientScanType.SONATYPE, false);

    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);

    PolicyEvaluationResult evaluationResult =
        scanPolicyEvaluator.createPolicyEvaluationResult(results.evaluation, results.allViolations, true);

    assertThat(evaluationResult.getAlerts()).hasSize(1);
    assertThat(evaluationResult.getAlerts().get(0).getActions()).isEmpty();
  }

  private Repository createTestRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    return tempEntity.newRepository(repositoryManager, "test-repo-id");
  }

  @Test
  public void testEvaluate_MissingLicenseFeature() throws Exception {
    newPolicy(new Condition(CoordinatesConditionType.ID, "match", "maven:commons-pool:commons-pool:1.4"));

    testProductLicense.setMissingFeatures(LicensedFeature.ENFORCEMENT);
    String scanBuildId = simulateReportIsAvailable("report");
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator
        .evaluate(application, scanBuildId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    PolicyEvaluationResult evaluationResult = scanPolicyEvaluator.createPolicyEvaluationResult(results.evaluation,
        results.allViolations, true);
    assertThat(evaluationResult.getAlerts()).hasSize(1);
    PolicyAlert alert = evaluationResult.getAlerts().get(0);
    assertThat(alert.getActions()).isEmpty();
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_CreateAndFixPolicyViolations() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    Policy policy = newSecurityPolicy();

    // First evaluation, all policy violations are new, all logged
    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, results.evaluation.getTime(), results.allViolations,
        currentUser.getUsernameOrSystem());
    policyViolationLoggerOutput.clear();

    // Second evaluation, all policy violations are the same, none logged
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    assertPolicyViolationLogDTOs(0);

    policyDAO.delete(policy);
    // Third evaluation, all policy violations are fixed, all logged
    results = scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    assertPolicyViolationsLogged(PolicyViolationLogEvent.FIX, results.evaluation.getTime(),
        policyViolationDAO.getByOwnerId(application.getId()), currentUser.getUsernameOrSystem());
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_AutoWaiveAndUnAutoWaivePolicyViolations() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");

    // There should be 36 violations per security policy
    Policy securityPolicyOne = new Policy(null, "Security Policy One");
    securityPolicyOne.setThreatLevel(5);
    securityPolicyOne.setOwnerId(application.getId());
    Constraint constraintOne = new Constraint(null, "TestConstraintOne", LogicalOperator.AND);
    constraintOne.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicyOne.addConstraint(constraintOne);
    tempEntity.newPolicy(securityPolicyOne);

    Policy securityPolicyTwo = new Policy(null, "Security Policy Two");
    securityPolicyTwo.setThreatLevel(9);
    securityPolicyTwo.setOwnerId(application.getId());
    Constraint constraintTwo = new Constraint(null, "TestConstraintTwo", LogicalOperator.AND);
    constraintTwo.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicyTwo.addConstraint(constraintTwo);
    tempEntity.newPolicy(securityPolicyTwo);

    AutoPolicyWaiver autoWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 7, false, true);

    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    assertThat(results1.allViolations).hasSize(72);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, results1.evaluation.getTime(), results1.allViolations,
        currentUser.getUsernameOrSystem());
    List<PolicyViolation> autoWaivedViolations = results1.autoWaivedViolations;
    assertThat(autoWaivedViolations).hasSize(36);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.AUTOWAIVE, results1.evaluation.getTime(), autoWaivedViolations,
        currentUser.getUsernameOrSystem());

    policyViolationLoggerOutput.clear();

    autoPolicyWaiverDAO.delete(autoWaiver);
    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results2.allViolations).hasSize(72);
    assertThat(results2.activeViolations).hasSize(72);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.UNAUTOWAIVE, results2.evaluation.getTime(),
        results1.autoWaivedViolations, currentUser.getUsernameOrSystem());
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_WaiveAndUnwaivePolicyViolations() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    // Create two policies that will cause policy violations and waive one policy.
    Policy securityPolicy = newSecurityPolicy();
    tempEntity.newWaiver(securityPolicy.getId(), application.getId());
    Policy licensePolicy = newPolicy(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));

    // Evaluate policies. There should be two policy violations, one active and one waived.
    // Both violations should have a CREATE event logged. Only one should have a WAIVE event.
    String scanId = simulateReportIsAvailable("testEvaluate_PolicyViolationLogger_WaivePolicyViolations/report");
    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results.allViolations).hasSize(2);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, results.evaluation.getTime(), results.allViolations,
        currentUser.getUsernameOrSystem());
    List<PolicyViolation> waivedViolations =
        filterPolicyViolationsByPolicyId(results.allViolations, securityPolicy.getId());
    assertThat(waivedViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.WAIVE, results.evaluation.getTime(), waivedViolations,
        currentUser.getUsernameOrSystem());

    policyViolationLoggerOutput.clear();

    // Waive the other policy and evaluate policies again.
    // There should be two waived policy violations, one already waived and one newly waived.
    // Only one should have a WAIVE event logged.
    PolicyWaiver licensePolicyWaiver = tempEntity.newWaiver(licensePolicy.getId(), application.getId());
    scanId = simulateReportIsAvailable("testEvaluate_PolicyViolationLogger_WaivePolicyViolations/report");
    results = scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    assertThat(results.allViolations).hasSize(2);
    List<PolicyViolation> newWaivedViolations =
        filterPolicyViolationsByPolicyId(results.allViolations, licensePolicy.getId());
    assertThat(newWaivedViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.WAIVE, results.evaluation.getTime(), newWaivedViolations,
        currentUser.getUsernameOrSystem());

    policyViolationLoggerOutput.clear();

    // Remove the waiver for one of the policies and evaluate policies again.
    // There should be an active policy violation again. The unwaived violation should have an UNWAIVE event logged.
    policyWaiverDAO.delete(licensePolicyWaiver);
    scanId = simulateReportIsAvailable("testEvaluate_PolicyViolationLogger_WaivePolicyViolations/report");
    results = scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    assertThat(results.allViolations).hasSize(2);
    assertThat(results.activeViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.UNWAIVE, results.evaluation.getTime(),
        Collections.singletonList(results.activeViolations.get(0)), currentUser.getUsernameOrSystem());
  }

  private List<PolicyViolation> filterPolicyViolationsByPolicyId(
      List<PolicyViolation> policyViolations,
      String policyId)
  {
    return policyViolations.stream()
        .filter(policyViolation -> policyViolation.getPolicyId().equals(policyId))
        .toList();
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_LegacyViolations() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);
    // Create two policies that will cause policy violations and allow legacy status for one policy.
    Policy securityPolicy = newSecurityPolicy();
    securityPolicy.setLegacyViolationAllowed(true);
    policyDAO.update(securityPolicy);
    newPolicy(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));

    // Evaluate policies. There should be two policy violations, one active and one legacy.
    // Both violations should have a CREATE event logged. Only one should have a GRANT_LEGACY_STATUS event.
    String scanId = simulateReportIsAvailable(
        "testEvaluate_PolicyViolationLogger_LegacyGrantedAndRevokedPolicyViolations/report");
    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results.allViolations).hasSize(2);
    assertThat(results.activeViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, results.evaluation.getTime(), results.allViolations,
        currentUser.getUsernameOrSystem());
    List<PolicyViolation> legacyViolations =
        filterPolicyViolationsByPolicyId(results.allViolations, securityPolicy.getId());
    assertThat(legacyViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.GRANDFATHER, results.evaluation.getTime(), legacyViolations,
        currentUser.getUsernameOrSystem());
    assertPolicyViolationsLogged(PolicyViolationLogEvent.GRANT_LEGACY_STATUS, results.evaluation.getTime(),
        legacyViolations, currentUser.getUsernameOrSystem());
  }

  @Test
  public void testEvaluate_DoesNotAllowReevalutionForNonLatestScan() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();
    scanPolicyEvaluator.evaluate(application, simulateReportIsAvailable("report"), stage,
        ScanTriggerType.CLI, ClientScanType.SONATYPE, false);
    newSecurityPolicy();

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    clearInvocations(mockTelemetrySender);

    assertThatThrownBy(() -> scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false))
            .hasMessage(REEVALUATE_NOT_ALLOWED_FOR_OUT_OF_DATE_SCAN_MESSAGE);

    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate(application.getId()));
    expectedAttributes.put("owner_type", application.getType().toString());
    expectedAttributes.put("scan_id", HdsClientAnalytics.obfuscate(scanId));
    assertStaleScanAttributes(telemetryDataArgumentCaptor.getValue(), expectedAttributes);
    clearInvocations(mockTelemetrySender);
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_LogsPolicyConditionTriggersForAllConditionTypes() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Label label = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID);
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newSecurityVulnerabilityOverride(application.getId(), "964cd74171f427720480", "sonatype",
        "sonatype-2007-0004", SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);
    VulnerabilityGroup vg = tempEntity.newVulnerabilityGroup("Test Group Name 1", Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newVulnerabilityGroupVulnerability(vg.getId(), "sonatype-2007-0004");
    Tag tag = tempEntity.newTag(application.getOrganizationId());
    tempEntity.newApplicationTag(application.getId(), tag.getId());
    tempEntity.newVulnerabilityCustomData(application.getOrganizationId(), "sonatype-2007-0004", tag,
        "custom remediation", "770", "cvss", 8.0F);

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
    Condition endOfLifeCondition = new Condition(ComponentEndOfLifeConditionType.ID, "is false");
    Condition relativePopularityCondition = new Condition(RelativePopularityConditionType.ID, ">=", "0");
    Condition securityVulnerabilitySeverityCondition = new Condition(SecurityVulnerabilitySeverityConditionType.ID,
        ">=", "7");
    Condition securityVulnerabilityStatusCondition = new Condition(SecurityVulnerabilityStatusConditionType.ID, "is",
        "ACKNOWLEDGED");
    Condition securityVulnerabilityCweCondition = new Condition(SecurityVulnerabilityCweConditionType.ID, "is", "770");
    Condition vulnerabilityGroupCondition = new Condition(VulnerabilityGroupConditionType.ID, "is", vg.getId());
    Condition securityVulnerabilityResearchCondition = new Condition(SecurityVulnerabilityResearchConditionType.ID,
        "is", SecurityVulnerabilityResearch.DEEP_DIVE_RESEARCH.getId());
    Condition securityVulnerabilityDetectionTypeCondition = new Condition(
        SecurityVulnerabilityDetectionConditionType.ID, "is not", SecurityVulnerabilityDetectionType.OTHER.getId());

    Condition packageUrlCondition = new Condition(PackageUrlConditionType.ID, "matches", "pkg:maven/*/*@*");
    Condition componentCategoryCondition = new Condition(ComponentCategoryConditionType.ID, "is not", "113");
    Condition hygieneCondition = new Condition(HygieneRatingConditionType.ID, "is not", "1");
    Condition dataSourceCondition = new Condition(DataSourceConditionType.ID, "has support for", "identity");
    Condition dependencyCondition = new Condition(DependencyTypeConditionType.ID, "is", "direct");
    Condition componentFormatCondition = new Condition(ComponentFormatConditionType.ID, "is", "maven");
    Condition vulnerabilityCategoryCondition =
        new Condition(SecurityVulnerabilityCategoryConditionType.ID, "is", "malicious_code");
    Condition integrityCondition = new Condition(IntegrityRatingConditionType.ID, "is not", "0");
    Condition securityVulnerabilitySourceCondition = new Condition(SecurityVulnerabilitySourceConditionType.ID,
        "is not", SecurityVulnerabilitySource.NATIONAL_VULNERABILITY_DATABASE.getId());
    Condition securityVulnerabilityCustomCVSSVectorCondition =
        new Condition(SecurityVulnerabilityCustomCVSSVectorStringConditionType.ID, "matches", "cvss");
    Condition securityVulnerabilityCustomRemediationCondition =
        new Condition(SecurityVulnerabilityCustomRemediationConditionType.ID, "exists", null);
    Condition kevStatusCondition = new Condition(KevStatusConditionType.ID, "is", "known_to_be_exploited");
    Condition epssPercentCondition = new Condition(SecurityVulnerabilityEpssScoreConditionType.ID, ">=", "1");

    List<Condition> conditions = Arrays.asList(ageCondition, coordinatesCondition, identificationSourceCondition,
        labelCondition, licenseCondition, licenseStatusCondition, licenseThreatGroupCondition,
        licenseThreatGroupLevelCondition, matchStateCondition, proprietaryCondition, relativePopularityCondition,
        securityVulnerabilitySeverityCondition, securityVulnerabilityStatusCondition, securityVulnerabilityCweCondition,
        vulnerabilityGroupCondition, securityVulnerabilityResearchCondition, packageUrlCondition,
        componentCategoryCondition, hygieneCondition, dataSourceCondition, dependencyCondition,
        componentFormatCondition, vulnerabilityCategoryCondition, integrityCondition,
        securityVulnerabilitySourceCondition, securityVulnerabilityCustomCVSSVectorCondition,
        securityVulnerabilityCustomRemediationCondition, endOfLifeCondition,
        securityVulnerabilityDetectionTypeCondition, kevStatusCondition, epssPercentCondition);
    ConditionTypes.enableConditionType(ConditionTypes.HygieneRatingConditionType);
    ConditionTypes.enableConditionType(ConditionTypes.IntegrityRatingConditionType);
    ConditionTypes.enableConditionType(ConditionTypes.SecurityVulnerabilitySourceConditionType);
    try {
      Set<String> expectedConditionTypeIds = ConditionTypes.getAll()
          .stream()
          .map(ConditionType::getId)
          .filter(id -> !ProprietaryNameConflictConditionType.ID.equals(id))
          .filter(id -> !IacControlConditionType.ID.equals(id))
          // Tested in testEvaluate_PolicyViolationLogger_DerivativeAiModelConditionType
          .filter(id -> !DerivativeAiModelConditionType.ID.equals(id))
          // Tested in testEvaluate_PolicyViolationLogger_AiModelContentConditionType
          .filter(id -> !AiModelContentConditionType.ID.equals(id))
          .collect(toSet());
      assertThat(conditions.stream().map(Condition::getConditionTypeId).collect(toSet()))
          .isEqualTo(expectedConditionTypeIds);

      Constraint constraint = new Constraint(null, "constraintName", LogicalOperator.OR);
      constraint.setConditions(conditions);

      tempEntity.newPolicy("policyName", constraint);

      ScanPolicyEvaluatorResults results = scanPolicyEvaluator
          .evaluate(application, simulateReportIsAvailable("LogPolicyViolationPolicyConditionTriggers"),
              new Stage(Stage.ID_BUILD), ScanTriggerType.CLI, ClientScanType.SONATYPE, false);

      assertThat(results.allViolations).hasSize(conditions.size() - 1); // KEV Status does not trigger for non CVE vulns
      assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, results.evaluation.getTime(), results.allViolations,
          currentUser.getUsernameOrSystem());
    }
    finally {
      ConditionTypes.disableConditionType(ConditionTypes.HygieneRatingConditionType);
      ConditionTypes.disableConditionType(ConditionTypes.IntegrityRatingConditionType);
      ConditionTypes.disableConditionType(ConditionTypes.SecurityVulnerabilitySourceConditionType);
    }
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_DerivativeAiModelConditionType() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Condition derivativeAiModelCondition = new Condition(DerivativeAiModelConditionType.ID, "is false");

    List<Condition> conditions = List.of(derivativeAiModelCondition);
    Constraint constraint = new Constraint(null, "constraintName", LogicalOperator.OR);
    constraint.addCondition(derivativeAiModelCondition);
    tempEntity.newPolicy("policyName", constraint);

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application,
        simulateReportIsAvailable("LogPolicyViolationDerivativeAiModelConditionType"), new Stage(Stage.ID_BUILD),
        ScanTriggerType.CLI, ClientScanType.SONATYPE, false);

    assertThat(results.allViolations).hasSize(conditions.size());
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, results.evaluation.getTime(), results.allViolations,
        currentUser.getUsernameOrSystem());
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_AiModelContentConditionType() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Condition aiModelContentCondition =
        new Condition(AiModelContentConditionType.ID, "is not", AiModelContentType.OBJECTIONABLE.getId());

    List<Condition> conditions = List.of(aiModelContentCondition);
    Constraint constraint = new Constraint(null, "constraintName", LogicalOperator.OR);
    constraint.addCondition(aiModelContentCondition);
    tempEntity.newPolicy("policyName", constraint);

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application,
        simulateReportIsAvailable("LogPolicyViolationAiModelContentConditionType"), new Stage(Stage.ID_BUILD),
        ScanTriggerType.CLI, ClientScanType.SONATYPE, false);

    assertThat(results.allViolations).hasSize(conditions.size());
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, results.evaluation.getTime(), results.allViolations,
        currentUser.getUsernameOrSystem());
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
    Condition integrityCondition = new Condition(IntegrityRatingConditionType.ID, "is not", "0");
    Condition endOfLifeCondition = new Condition(ComponentEndOfLifeConditionType.ID, "is false");

    List<Condition> conditions = Arrays.asList(packageUrlCondition, componentCategoryCondition, hygieneCondition,
        dependencyCondition, vulnerabilityCategoryCondition, integrityCondition, endOfLifeCondition);

    Constraint constraint = new Constraint(null, "constraintName", LogicalOperator.OR);
    constraint.setConditions(conditions);

    tempEntity.newPolicy("policyName", constraint);

    String scanId = simulateReportIsAvailable("LogPolicyViolationPolicyConditionTriggers");
    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    clearInvocations(mockTelemetrySender);

    // When evaluate policies
    scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor
        .getValue()
        .stream()
        .filter(telemetryData -> telemetryData.getPurpose().equals(TelemetryPurpose.CONDITION_TYPE_VIOLATION))
        .toList();

    assertThat(telemetryDataList).hasSize(conditions.size());

    boolean hasHygieneViolation = assertConditionTypeExistsInTelemetryData(HygieneRatingConditionType.ID,
        telemetryDataList);

    boolean hasComponentCategoryViolation = assertConditionTypeExistsInTelemetryData(ComponentCategoryConditionType.ID,
        telemetryDataList);

    boolean hasDependencyTypeViolation = assertConditionTypeExistsInTelemetryData(DependencyTypeConditionType.ID,
        telemetryDataList);

    boolean hasSVCategoryTypeViolation = assertConditionTypeExistsInTelemetryData(
        SecurityVulnerabilityCategoryConditionType.ID, telemetryDataList);

    boolean hasIntegrityViolation = assertConditionTypeExistsInTelemetryData(
        IntegrityRatingConditionType.ID, telemetryDataList);

    boolean hasComponentEndOfLife = assertConditionTypeExistsInTelemetryData(
        ComponentEndOfLifeConditionType.ID, telemetryDataList);

    assertThat(hasHygieneViolation).isTrue();
    assertThat(hasComponentCategoryViolation).isTrue();
    assertThat(hasDependencyTypeViolation).isTrue();
    assertThat(hasSVCategoryTypeViolation).isTrue();
    assertThat(hasIntegrityViolation).isTrue();
    assertThat(hasComponentEndOfLife).isTrue();
    clearInvocations(mockTelemetrySender);
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_LogsPolicyConditionTriggersForMultipleConstraintsConditions() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
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
            new Stage(Stage.ID_BUILD), ScanTriggerType.CLI, ClientScanType.SONATYPE, false);

    assertThat(results.allViolations).isNotEmpty();
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, results.evaluation.getTime(), results.allViolations,
        currentUser.getUsernameOrSystem());
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
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor
        .getValue()
        .stream()
        .filter(telemetryData -> telemetryData.getPurpose().equals(TelemetryPurpose.CONDITION_TYPE_VIOLATION))
        .toList();

    assertThat(telemetryDataList).hasSize(36);

    // When removing the policy
    policyDAO.delete(policy);
    clearInvocations(mockTelemetrySender);
    // And running the second evaluation to have all policy violations fixed
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    // Then all policy violations are collected for telemetry
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    telemetryDataList = telemetryDataArgumentCaptor.getValue();
    assertThat(telemetryDataList).hasSize(36);
    for (TelemetryData telemetryData : telemetryDataList) {
      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.TIME_TO_REMEDIATE_POLICY_VIOLATION);
    }
  }

  @Test
  public void testEvaluate_StoresAggregateFiles() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("testEvaluate_StoresAggregateFiles");

    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    List<OwnerComponent> applicationComponents =
        applicationComponentDAO.getByOwnerId(application.getId());
    assertThat(applicationComponents).hasSize(2);
    OwnerComponent aggregateComponent = applicationComponents.stream()
        .filter(applicationComponent -> applicationComponent.getHash().equals("a567564b25bb307a55ef"))
        .findFirst()
        .orElse(null);
    assertThat(aggregateComponent).isNotNull();
    List<AggregateFile> aggregateFiles = aggregateFileDAO.getByOwnerComponentId(aggregateComponent.getId());
    assertThat(aggregateFiles).hasSize(2);
    assertThat(findAggregateFileByHash(aggregateFiles, "b688552e098a688d71ed").getPathnames())
        .containsExactlyInAnyOrder("hawkTest.zip/duplicate/index.js", "hawkTest.zip/index.js",
            "hawkTest.zip/nested/index/index.js", "hawkTest.zip/reversed/server.js");
    assertThat(findAggregateFileByHash(aggregateFiles, "d19dcac7f0ce105e3369").getPathnames())
        .containsExactlyInAnyOrder("hawkTest.zip/duplicate/server.js", "hawkTest.zip/nested/server/server.js",
            "hawkTest.zip/reversed/index.js", "hawkTest.zip/server.js");
    OwnerComponent nonAggregateComponent = applicationComponents.stream()
        .filter(applicationComponent -> applicationComponent.getHash().equals("ab4e6f3b97ec4831f018"))
        .findFirst()
        .orElse(null);
    assertThat(nonAggregateComponent).isNotNull();
    assertThat(aggregateFileDAO.getByOwnerComponentId(nonAggregateComponent.getId())).isEmpty();
  }

  @Test
  public void testEvaluate_StoresOwnerComponentLicenses() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("testEvaluate_StoresOwnerComponentLicenses");

    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    List<OwnerComponent> applicationComponents =
        applicationComponentDAO.getByOwnerId(application.getId());
    assertThat(applicationComponents).hasSize(1);

    OwnerComponent applicationComponent = applicationComponents.get(0);

    List<OwnerComponentLicense> applicationComponentLicenses =
        applicationComponentLicenseDAO.getByOwnerComponentId(applicationComponent.getId());
    assertThat(applicationComponentLicenses).hasSize(3);

    OwnerComponentLicense applicationComponentLicense1 =
        new OwnerComponentLicense(applicationComponent.getId(), "EPL-1.0");
    OwnerComponentLicense applicationComponentLicense2 =
        new OwnerComponentLicense(applicationComponent.getId(), "LGPL-2.1");
    OwnerComponentLicense applicationComponentLicense3 =
        new OwnerComponentLicense(applicationComponent.getId(), "EPL-1.0-LGPL-2.1");

    assertThat(applicationComponentLicenses)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(ArrayUtils.add(JPA.IGNORE_FIELDS, "id"))
        .containsExactlyInAnyOrder(applicationComponentLicense1, applicationComponentLicense2,
            applicationComponentLicense3);
  }

  private AggregateFile findAggregateFileByHash(List<AggregateFile> aggregateFiles, String hash) {
    AggregateFile result = aggregateFiles.stream()
        .filter(aggregateFile -> aggregateFile.getHash().equals(hash))
        .findFirst()
        .orElse(null);
    assertThat(result).isNotNull();
    return result;
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
    scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    // Then there should be two policy violations, of which one is waived.
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getValue();
    assertThat(telemetryDataList).hasSize(5);
    TelemetryData timeToWaiveTelemetryData = telemetryDataList.stream()
        .filter(telemetryData -> TIME_TO_WAIVE_POLICY_VIOLATION.equals(telemetryData.getPurpose()))
        .findFirst()
        .orElseThrow();
    assertThat(timeToWaiveTelemetryData.getAttributes().get(COUNT)).isEqualTo(1);
    clearInvocations(mockTelemetrySender);

    // When waive the other policy and evaluate policies again
    PolicyWaiver licensePolicyWaiver = tempEntity.newWaiver(licensePolicy.getId(), application.getId());
    scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    // Then there should be two waived policy violations, one already waived and one newly waived.
    // Only one should be collected for telemetry
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    telemetryDataList = telemetryDataArgumentCaptor
        .getValue()
        .stream()
        .filter(telemetryData -> telemetryData.getPurpose().equals(TIME_TO_WAIVE_POLICY_VIOLATION))
        .toList();

    assertThat(telemetryDataList).hasSize(1);
    assertThat(telemetryDataList.get(0).getPurpose()).isEqualTo(TIME_TO_WAIVE_POLICY_VIOLATION);
    assertThat(telemetryDataList.get(0).getAttributes().get(COUNT)).isEqualTo(1);
    clearInvocations(mockTelemetrySender);

    // When remove the waiver for one of the policies and evaluate policies again.
    policyWaiverDAO.delete(licensePolicyWaiver);
    scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    // Then there should be an unwaived violation collected for telemetry
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    telemetryDataList = telemetryDataArgumentCaptor
        .getValue()
        .stream()
        .filter(telemetryData -> telemetryData.getPurpose().equals(TIME_TO_WAIVE_POLICY_VIOLATION))
        .toList();

    assertThat(telemetryDataList).hasSize(1);
    assertThat(telemetryDataList.get(0).getPurpose()).isEqualTo(TIME_TO_WAIVE_POLICY_VIOLATION);
    assertThat(telemetryDataList.get(0).getAttributes().get(COUNT)).isEqualTo(-1);
    clearInvocations(mockTelemetrySender);
  }

  @Test
  public void testEvaluate_Results_FixedViolationsCount() throws Exception {
    // Given
    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("report");
    Policy policy = newSecurityPolicy();

    // When running the first evaluation
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    // Then new violations are created
    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(36);
    assertThat(scanPolicyEvaluatorResults.fixedViolations).isEmpty();

    // When removing the policy
    policyDAO.delete(policy);
    clearInvocations(mockTelemetrySender);
    // And running the second evaluation to have all policy violations fixed
    scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    // Then all policy violations are reported as fixed and no active
    assertThat(scanPolicyEvaluatorResults.activeViolations).isEmpty();
    assertThat(scanPolicyEvaluatorResults.fixedViolations).hasSize(36);
  }

  @Test
  public void testEvaluate_Results_WaivedViolationsCount() throws Exception {
    // Create two policies that will cause policy violations and waive one policy.
    Policy securityPolicy = newSecurityPolicy();
    tempEntity.newWaiver(securityPolicy.getId(), application.getId());
    Policy licensePolicy = newPolicy(new Condition(LicenseConditionType.ID, "is", "Apache-2.0"));
    String scanId = simulateReportIsAvailable("testEvaluate_PolicyViolationLogger_WaivePolicyViolations/report");

    // When evaluate policies
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    // There should be one waived violation
    assertThat(scanPolicyEvaluatorResults.waivedViolations).hasSize(1);
    assertThat(scanPolicyEvaluatorResults.waivedViolations.get(0).getPolicyId()).isEqualTo(securityPolicy.getId());

    // When waive the other policy and evaluate policies again
    tempEntity.newWaiver(licensePolicy.getId(), application.getId());
    scanPolicyEvaluatorResults =
        scanPolicyEvaluator.evaluate(application, scanId, new Stage(Stage.ID_BUILD), ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    // The second violation should be reported as waived
    assertThat(scanPolicyEvaluatorResults.waivedViolations).hasSize(1);
    assertThat(scanPolicyEvaluatorResults.waivedViolations.get(0).getPolicyId()).isEqualTo(licensePolicy.getId());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testEvaluate_replaceAutoWaiverWithManualWaiver() throws Exception {
    // Given a security policy that will cause policy violations
    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(6);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    // And an auto policy waiver
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 8, false, true);

    // And a report with violations
    String scanId = simulateReportIsAvailable("report");

    // Mock component info service to return empty results
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    // Capture telemetry data
    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    clearInvocations(mockTelemetrySender);

    // When evaluating policies for the first time
    Stage stage = new Stage(Stage.ID_BUILD);
    ScanPolicyEvaluatorResults results1 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    // Then violations should be auto-waived
    assertThat(results1.autoWaivedViolations).isNotEmpty();
    assertThat(results1.activeViolations).isEmpty();
    assertThat(results1.waivedViolations).isEmpty();

    // And auto-waive telemetry should be sent
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getValue();
    List<TelemetryData> autoWaiveTelemetry = telemetryDataList.stream()
        .filter(telemetryData -> TIME_TO_WAIVE_POLICY_VIOLATION.equals(telemetryData.getPurpose()))
        .toList();
    assertThat(autoWaiveTelemetry).isNotEmpty();

    // Select a specific violation to manually waive
    PolicyViolation autoWaivedViolation = results1.autoWaivedViolations.get(0);
    assertThat(autoWaivedViolation.isAutoWaived()).isTrue();
    assertThat(autoWaivedViolation.getAutoPolicyWaiverId()).isEqualTo(autoPolicyWaiver.getId());

    // When adding a manual waiver for the same component/policy
    clearInvocations(mockTelemetrySender);
    PolicyWaiver manualWaiver = tempEntity.newWaiver(
        autoWaivedViolation.getHash(),
        autoWaivedViolation.getPolicyId(),
        application.getId());

    // And evaluating policies again
    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);

    // Then the violation should now be manually waived instead of auto-waived
    assertThat(results2.waivedViolations).isNotEmpty();

    // Find the manually waived violation that replaced the auto-waived one
    Optional<PolicyViolation> manuallyWaivedViolationOpt = results2.waivedViolations.stream()
        .filter(v -> v.getHash().equals(autoWaivedViolation.getHash()) &&
            v.getPolicyId().equals(autoWaivedViolation.getPolicyId()))
        .findFirst();

    assertThat(manuallyWaivedViolationOpt).isPresent();
    PolicyViolation manuallyWaivedViolation = manuallyWaivedViolationOpt.get();

    // Verify the manually waived violation has the correct properties
    assertThat(manuallyWaivedViolation.isWaived()).isTrue();
    assertThat(manuallyWaivedViolation.isAutoWaived()).isFalse();
    assertThat(manuallyWaivedViolation.getPolicyWaiverId()).isEqualTo(manualWaiver.getId());
    assertThat(manuallyWaivedViolation.getAutoPolicyWaiverId()).isNull();

    // Verify telemetry was sent for both unwaving the auto-waived violation and waiving the new violation
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    telemetryDataList = telemetryDataArgumentCaptor.getValue();

    // Filter telemetry data for TIME_TO_WAIVE_POLICY_VIOLATION purpose
    List<TelemetryData> waiveTelemetry = telemetryDataList.stream()
        .filter(telemetryData -> TIME_TO_WAIVE_POLICY_VIOLATION.equals(telemetryData.getPurpose()))
        .toList();

    // Should have at least two telemetry entries: one for unwaving and one for waiving
    assertThat(waiveTelemetry).hasSizeGreaterThanOrEqualTo(2);

    // Find the unwaive telemetry (has COUNT = -1)
    Optional<TelemetryData> unwaiveTelemetryOpt = waiveTelemetry.stream()
        .filter(telemetryData -> telemetryData.getAttributes().containsKey(COUNT) &&
            telemetryData.getAttributes().get(COUNT).equals(-1))
        .findFirst();

    assertThat(unwaiveTelemetryOpt).isPresent();
    TelemetryData unwaiveTelemetry = unwaiveTelemetryOpt.get();

    // Verify unwaive telemetry has the auto policy waiver ID
    assertThat(unwaiveTelemetry.getAttributes()).containsKey("auto_policy_waiver_id");
    assertThat(unwaiveTelemetry.getAttributes().get("auto_policy_waiver_id")).isEqualTo(autoPolicyWaiver.getId());

    // Find the waive telemetry for the new manually waived violation
    Optional<TelemetryData> waiveTelemetryOpt = waiveTelemetry.stream()
        .filter(telemetryData -> telemetryData.getAttributes().containsKey("policy_waiver_id") &&
            telemetryData.getAttributes().get("policy_waiver_id").equals(manualWaiver.getId()))
        .findFirst();

    assertThat(waiveTelemetryOpt).isPresent();
    TelemetryData newWaiveTelemetry = waiveTelemetryOpt.get();

    // Verify waive telemetry has the policy waiver ID and waive time
    assertThat(newWaiveTelemetry.getAttributes()).containsKey("policy_waiver_id");
    assertThat(newWaiveTelemetry.getAttributes()).containsKey("waive_time");
  }

  @Test
  public void testEvaluate_EvaluationAfterEnablingLegacyViolations() throws Exception {
    testEvaluate_DoEvaluationAfterEnablingLegacyViolations(false);
  }

  @Test
  public void testEvaluate_ReEvaluationAfterEnablingLegacyViolations() throws Exception {
    testEvaluate_DoEvaluationAfterEnablingLegacyViolations(true);
  }

  @Test
  public void testEvaluate_RevokeLegacyViolations() throws Exception {
    applicationDAO.update(application);

    Stage stage = new Stage(Stage.ID_BUILD);
    Policy policy = newSecurityPolicy();
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);

    String scanId = simulateReportIsAvailable("report");
    File scanFile = createScanFile(application, scanId);
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    assertThat(scanFile).isFile();

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    application.setLegacyViolationEnabled(false);
    applicationDAO.update(application);

    // This is what disabling legacy violations performs for an application
    try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      tx.begin();
      policyViolationDAO.getByOwnerId(application.getId()).forEach(policyViolation -> {
        policyViolationDAO.loadConstraintFacts(Collections.singletonList(policyViolation));
        policyViolation.setLegacyViolationTime(null);
        policyViolationDAO.update(tx, policyViolation);
      });
      tx.commit();
    }

    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    policyViolationDAO.getByOwnerId(application.getId())
        .forEach(policyViolation -> assertThat(policyViolation.isLegacyViolationApplied()).isFalse());
  }

  @Test
  public void testEvaluate_constraintFactsNotMigrated() throws Exception {
    application = tempEntity.newApplicationWithParent();
    Policy policy = newSecurityPolicy();
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    // First evaluation creates legacy violation
    String scanId1 = simulateReportIsAvailable("report");
    Stage stage1 = new Stage(Stage.ID_BUILD);
    scanPolicyEvaluator.evaluate(application, scanId1, stage1, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    // Run second evaluation which will use violations from the first evaluation. This is where the constraint violation
    // was triggered
    String scanId2 = simulateReportIsAvailable("report");

    restoreConstraintFactsToPreMigratedState();

    ScanPolicyEvaluatorResults results2 =
        scanPolicyEvaluator.evaluate(application, scanId2, stage1, ScanTriggerType.CLI,
            ClientScanType.SONATYPE, false);
    assertThat(results2.activeViolations).isNotEmpty();
  }

  @Test
  public void testPerformPolicyEvaluation() throws Exception {
    String scanId = simulateReportIsAvailable("report");
    newSecurityPolicy();
    Stage stage = new Stage(Stage.ID_BUILD);

    ReportComponentData reportComponentData =
        reportComponentService.fetchReportAndComponents(application, scanId, stage.getStageTypeId());

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.performPolicyEvaluation(
        application, scanId, stage, ScanTriggerType.CLI, "testUserAgent", "testClientId",
        false, ClientScanType.SONATYPE, reportComponentData, false);

    assertNotNull(results);
    assertNotNull(results.evaluation);
    assertThat(results.allViolations).isNotEmpty();
  }

  @Test
  public void testPerformPolicyEvaluation_SbomUpdateStatus() throws Exception {
    String scanId = simulateReportIsAvailable("report");
    ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan("request", scanId, file);
    tempEntity.createSbomMetadata(application.getId(), scanId, file, PENDING);

    newSecurityPolicy();
    Stage stage = new Stage(Stage.ID_BUILD);

    ReportComponentData reportComponentData =
        reportComponentService.fetchReportAndComponents(application, scanId, stage.getStageTypeId());

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.performPolicyEvaluation(
        application, scanId, stage, ScanTriggerType.CLI, "testUserAgent", "testClientId",
        false, ClientScanType.SONATYPE, reportComponentData, false);

    assertNotNull(results);
    assertNotNull(results.evaluation);
    assertThat(results.allViolations).isNotEmpty();
    ThirdPartySbomMetadata updatedMetadata = sbomMetadataDAO.getByThirdPartyFileId(file.getId());

    assertThat(updatedMetadata.getStatus()).isEqualTo(ACTIVE);
  }

  @Test
  public void testPerformPolicyEvaluation_WithReachableVulnerability() throws Exception {

    // Set up reachable vulnerability:
    String reachableCVE = "CVE-2012-0022";

    // Mock the reachable vuln map
    Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> reachableVulnMap = new HashMap();

    reachableVulnMap.put(new PackageUrlIdentifier("pkg:maven/tomcat/tomcat-util@5.5.23"),
        new PresentReachableComponentVulnerabilities(Set.of(reachableCVE)));

    List<String> unreachableComponentList = List.of(
        "pkg:maven/commons-httpclient/commons-httpclient@3.1",
        "pkg:maven/org.apache.geronimo.framework/geronimo-security@2.1",
        "pkg:maven/tomcat/catalina-host-manager@5.5.23",
        "pkg:maven/org.mortbay.jetty/jetty@6.1.15",
        "pkg:maven/tomcat/servlets-default@5.5.4",
        "pkg:maven/org.openid4java/openid4java@0.9.5",
        "pkg:maven/tomcat/tomcat-util@5.4.23");

    addReachabilityMap(unreachableComponentList, reachableVulnMap);

    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId", reachableVulnMap))
        .when(apiVulnerabilityReachabilityStatusService)
        .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(), any(VulnerabilitySignatureAnalysisDTO.class));

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    tempEntity.newAutoPolicyWaiver(application.getId(), 10, true, false);

    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        scanId,
        componentIdentifier,
        reachableCVE,
        insightWork);

    ReportComponentData reportComponentData =
        reportComponentService.fetchReportAndComponents(application, scanId, stage.getStageTypeId());

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.performPolicyEvaluation(
        application, scanId, stage, ScanTriggerType.CLI, "testUserAgent", "testClientId",
        false, ClientScanType.SONATYPE, reportComponentData, analysisDTO, false);

    assertThat(results.autoWaivedViolations).hasSize(35);
    assertThat(results.activeViolations).hasSize(1);

    Optional<PolicyViolation> optionalPolicyViolation =
        findPolicyViolationByVulnerabilityIdentifier(results.activeViolations, reachableCVE);

    assertThat(optionalPolicyViolation).isPresent();
    assertThat(optionalPolicyViolation.get().getReachabilityStatus()).isEqualTo(REACHABLE);
    assertThat(optionalPolicyViolation.get().getComponentIdentifier()).isEqualTo(componentIdentifier);

    results.autoWaivedViolations
        .forEach(policyViolation -> assertThat(policyViolation.getReachabilityStatus()).isNotNull());
  }

  @Test
  public void testEvaluate_DerivativeAiModel() throws Exception {
    Policy policy = newPolicy(new Condition(DerivativeAiModelConditionType.ID, "is true"));
    Constraint constraint = policy.getConstraints().get(0);

    String scanId = simulateReportIsAvailable("DerivativeAiModel");
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = scanPolicyEvaluator.evaluate(application, scanId,
        new Stage(Stage.ID_BUILD), ScanTriggerType.CLI, ClientScanType.SONATYPE, false);

    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(1);
    assertContainsPolicyViolation(
        ComponentIdentifier.createHuggingfaceModelCoordinates("testRepoId", "testModel", "testVersion",
            "testModelFormat", "testExtension"),
        "a64cd74171f427720480", policy, constraint, Action.ID_FAIL, DerivativeAiModelConditionType.ID,
        scanPolicyEvaluatorResults.activeViolations);
  }

  @Test
  public void testEvaluate_AiModelContentConditionType() throws Exception {
    Policy policy =
        newPolicy(new Condition(AiModelContentConditionType.ID, "is", AiModelContentType.OBJECTIONABLE.getId()));
    Constraint constraint = policy.getConstraints().get(0);

    String scanId = simulateReportIsAvailable("AiModelContent");
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = scanPolicyEvaluator.evaluate(application, scanId,
        new Stage(Stage.ID_BUILD), ScanTriggerType.CLI, ClientScanType.SONATYPE, false);

    assertThat(scanPolicyEvaluatorResults.activeViolations).hasSize(1);
    assertContainsPolicyViolation(
        ComponentIdentifier.createHuggingfaceModelCoordinates("testRepoId", "testModel", "testVersion",
            "testModelFormat", "testExtension"),
        "a64cd74171f427720480", policy, constraint, Action.ID_FAIL, AiModelContentConditionType.ID,
        scanPolicyEvaluatorResults.activeViolations);
  }

  @Test
  public void testEvaluate_EvaluateAutoWaiverWithNotReachableAndNoPathForward_ByScopeOperator_And() throws Exception {
    // Set up reachable vulnerability:
    String vulnerabilityIdentifier = "CVE-2007-3385";
    String vulnerabilityIdentifier2 = "CVE-2007-3386";

    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId",
        Map.of(new PackageUrlIdentifier("pkg:maven/org.openid4java/openid4java@0.9.5"),
            new PresentReachableComponentVulnerabilities(Set.of(vulnerabilityIdentifier2)),
            new PackageUrlIdentifier("pkg:maven/tomcat/tomcat-util@5.5.23"),
            new PresentReachableComponentVulnerabilities(Set.of(vulnerabilityIdentifier)),
            new PackageUrlIdentifier("pkg:maven/org.mortbay.jetty/jetty@6.1.15"),
            new PresentReachableComponentVulnerabilities(Set.of(vulnerabilityIdentifier2)))))
                .when(apiVulnerabilityReachabilityStatusService)
                .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(),
                    any(VulnerabilitySignatureAnalysisDTO.class));

    // Set up path forward:
    ComponentDetailsDTO tomcatComponentDetailsDTO = new ComponentDetailsDTO();
    tomcatComponentDetailsDTO.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    tomcatComponentDetailsDTO.violatedPolicyCount = 1;
    ComponentDetailsDTO jettyComponentDetailsDTO = new ComponentDetailsDTO();
    jettyComponentDetailsDTO.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("org.mortbay.jetty", "jetty", "6.1.15");
    jettyComponentDetailsDTO.violatedPolicyCount = 0;
    doReturn(Pair.of(Arrays.asList(jettyComponentDetailsDTO, jettyComponentDetailsDTO), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), eq(jettyComponentDetailsDTO.componentIdentifier), any(), any(), any(), any(), any(),
            anyBoolean());
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), not(eq(jettyComponentDetailsDTO.componentIdentifier)), any(), any(), any(), any(), any(),
            anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverWithScopeOperator");
    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(4);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    // Set auto waiver to use AND logic to evaluate the scopes
    tempEntity.newAutoPolicyWaiver(application.getId(), 7, true, true, false);

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        scanId,
        tomcatComponentDetailsDTO.componentIdentifier,
        vulnerabilityIdentifier,
        insightWork);
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, analysisDTO, false);

    /*
     * Total violations = 7
     * Security.REACHABLE violations = 1
     * Security.NON_REACHABLE violations = 6
     * Components with pathForward = 1 with 3 vulnerabilities
     *
     * NOT AUTO WAIVED:
     * tomcat-util: 1/2 vulns with REACHABLE, no path forward
     * jetty: 3 vulns with NOT REACHABLE, has path forward
     *
     * AUTO WAIVED:
     * tomcat-util: 1/2 vulns with NOT REACHABLE, no path forward
     * openid: 2 vulns with NOT REACHABLE, no path forward
     */
    PackageUrlIdentifier openId4JavaIdentifier = PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier
        .createMavenCoordinates("org.openid4java", "openid4java", "0.9.5"));
    PackageUrlIdentifier tomcatIdentifier =
        PackageUrlIdentifier.fromComponentIdentifier(tomcatComponentDetailsDTO.componentIdentifier);
    PackageUrlIdentifier jettyIdentifier =
        PackageUrlIdentifier.fromComponentIdentifier(jettyComponentDetailsDTO.componentIdentifier);

    assertThat(results.autoWaivedViolations)
        .hasSize(3)
        .extracting(
            policyViolation -> PackageUrlIdentifier.fromComponentIdentifier(policyViolation.getComponentIdentifier()))
        .containsExactlyInAnyOrder(openId4JavaIdentifier, openId4JavaIdentifier, tomcatIdentifier);

    assertThat(results.activeViolations)
        .hasSize(4)
        .extracting(
            policyViolation -> PackageUrlIdentifier.fromComponentIdentifier(policyViolation.getComponentIdentifier()))
        .containsExactlyInAnyOrder(tomcatIdentifier, jettyIdentifier, jettyIdentifier, jettyIdentifier);
  }

  @Test
  public void testEvaluate_EvaluateAutoWaiverWithNotReachableAndNoPathForward_ByScopeOperator_Or() throws Exception {
    // Set up reachable vulnerabilities:
    String vulnerabilityIdentifier = "CVE-2007-3385";
    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId",
        Map.of(new PackageUrlIdentifier("pkg:maven/tomcat/tomcat-util@5.5.23"),
            new PresentReachableComponentVulnerabilities(Set.of(vulnerabilityIdentifier)),
            new PackageUrlIdentifier("pkg:maven/org.openid4java/openid4java@0.9.5"),
            new PresentReachableComponentVulnerabilities(Set.of("73737")),
            new PackageUrlIdentifier("pkg:maven/org.mortbay.jetty/jetty@6.1.15"),
            new PresentReachableComponentVulnerabilities(Set.of("73737")))))
                .when(apiVulnerabilityReachabilityStatusService)
                .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(),
                    any(VulnerabilitySignatureAnalysisDTO.class));
    // Set up path forward:
    ComponentDetailsDTO tomcatComponentDetailsDTO = new ComponentDetailsDTO();
    tomcatComponentDetailsDTO.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    tomcatComponentDetailsDTO.violatedPolicyCount = 0;
    ComponentDetailsDTO jettyComponentDetailsDTO = new ComponentDetailsDTO();
    jettyComponentDetailsDTO.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("org.mortbay.jetty", "jetty", "6.1.15");
    jettyComponentDetailsDTO.violatedPolicyCount = 0;
    ComponentIdentifier openidComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("org.openid4java", "openid4java", "0.9.5");
    doReturn(Pair.of(Arrays.asList(tomcatComponentDetailsDTO, tomcatComponentDetailsDTO), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), eq(tomcatComponentDetailsDTO.componentIdentifier), any(), any(), any(), any(), any(), anyBoolean());
    // Don't need to mock path forward for jetty because path forward evaluation will not be reached because it is
    // not reachable, which satisfies the auto waiver condition
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), eq(openidComponentIdentifier), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverWithScopeOperator");
    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(4);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    // Set auto waiver to use OR logic to evaluate the scopes (default behaviour)
    tempEntity.newAutoPolicyWaiver(application.getId(), 7, true, true);

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        scanId,
        tomcatComponentDetailsDTO.componentIdentifier,
        vulnerabilityIdentifier,
        insightWork);
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, analysisDTO, false);

    /*
     * Total violations = 7
     * Security.REACHABLE violations = 1
     * Security.NON_REACHABLE violations = 6
     * Components with pathForward = 2 with 5 combined vulnerabilities
     *
     * NOT AUTO WAIVED:
     * tomcat-util: 1/2 vulns with REACHABLE, has path forward
     *
     * AUTO WAIVED:
     * tomcat-util: 1/2 vulns with NOT REACHABLE, has path forward
     * jetty: 3 vulns with NOT REACHABLE, no path forward
     * openid: 1/2 vulns with NOT REACHABLE, no path forward
     * openid: 1/2 vulns with REACHABLE, no path forward
     */
    PackageUrlIdentifier openId4JavaIdentifier =
        PackageUrlIdentifier.fromComponentIdentifier(openidComponentIdentifier);
    PackageUrlIdentifier tomcatIdentifier =
        PackageUrlIdentifier.fromComponentIdentifier(tomcatComponentDetailsDTO.componentIdentifier);
    PackageUrlIdentifier jettyIdentifier =
        PackageUrlIdentifier.fromComponentIdentifier(jettyComponentDetailsDTO.componentIdentifier);
    assertThat(results.autoWaivedViolations)
        .hasSize(6)
        .extracting(
            policyViolation -> PackageUrlIdentifier.fromComponentIdentifier(policyViolation.getComponentIdentifier()))
        .containsExactlyInAnyOrder(openId4JavaIdentifier, openId4JavaIdentifier, tomcatIdentifier, jettyIdentifier,
            jettyIdentifier, jettyIdentifier);

    assertThat(results.activeViolations)
        .hasSize(1)
        .extracting(
            policyViolation -> PackageUrlIdentifier.fromComponentIdentifier(policyViolation.getComponentIdentifier()))
        .containsExactly(tomcatIdentifier);
  }

  @Test
  public void testEvaluate_ApplyFirstAutoWaiverThatMatches() throws Exception {
    // Set up no path forward:
    ComponentDetailsDTO tomcatComponentDetailsDTO = new ComponentDetailsDTO();
    tomcatComponentDetailsDTO.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    tomcatComponentDetailsDTO.violatedPolicyCount = 0;
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("MultiAutoWaivers");
    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(4);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        scanId,
        tomcatComponentDetailsDTO.componentIdentifier,
        "CVE-2007-3385",
        insightWork);

    final AutoPolicyWaiver notReachable =
        tempEntity.newAutoPolicyWaiver(application.getId(), 7, true, false);
    final AutoPolicyWaiver noPathForward =
        tempEntity.newAutoPolicyWaiver(application.getId(), 7, false, true);

    // Match the auto waiver with No Path Forward
    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId",
        Map.of(new PackageUrlIdentifier("pkg:maven/tomcat/tomcat-util@5.5.23"),
            new PresentReachableComponentVulnerabilities(Set.of("CVE-2007-3385")))))
                .when(apiVulnerabilityReachabilityStatusService)
                .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(),
                    any(VulnerabilitySignatureAnalysisDTO.class));
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, analysisDTO, false);
    assertThat(results.autoWaivedViolations)
        .singleElement()
        .extracting(PolicyViolation::getAutoPolicyWaiverId)
        .isEqualTo(noPathForward.getId());

    // Match the auto waiver with Unknown reachability then non-reachable auto-waiver doesn't apply
    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId", Map.of()))
        .when(apiVulnerabilityReachabilityStatusService)
        .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(),
            any(VulnerabilitySignatureAnalysisDTO.class));
    results = scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, analysisDTO, false);
    assertThat(results.autoWaivedViolations)
        .singleElement()
        .extracting(PolicyViolation::getAutoPolicyWaiverId)
        .isEqualTo(noPathForward.getId());

    // Match the auto waiver with Not Reachable
    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId",
        Map.of(new PackageUrlIdentifier("pkg:maven/tomcat/tomcat-util@5.5.23"),
            new PresentReachableComponentVulnerabilities(Set.of("CVE-2007-3382")))))
                .when(apiVulnerabilityReachabilityStatusService)
                .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(),
                    any(VulnerabilitySignatureAnalysisDTO.class));
    results = scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, analysisDTO, false);
    assertThat(results.autoWaivedViolations)
        .singleElement()
        .extracting(PolicyViolation::getAutoPolicyWaiverId)
        .isEqualTo(notReachable.getId());

    // Match the auto waiver with Not Reachable and No Path Forward
    final AutoPolicyWaiver notReachableAndNoPathForward =
        tempEntity.newAutoPolicyWaiver(application.getId(), 7, true, true, false);
    results = scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, analysisDTO, false);
    assertThat(results.autoWaivedViolations)
        .singleElement()
        .extracting(PolicyViolation::getAutoPolicyWaiverId)
        .isEqualTo(notReachableAndNoPathForward.getId());
  }

  @Test
  public void testEvaluate_CollectAppliedAutoWaiverTelemetry() throws Exception {
    // Set up no path forward:
    ComponentDetailsDTO tomcatComponentDetailsDTO = new ComponentDetailsDTO();
    tomcatComponentDetailsDTO.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    tomcatComponentDetailsDTO.violatedPolicyCount = 0;
    doReturn(Pair.of(Collections.emptyList(), null))
        .when(mockComponentInfoService)
        .getComponentDetailsForAllVersionsNoAuth(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean());

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("MultiAutoWaivers");
    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(4);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        scanId,
        tomcatComponentDetailsDTO.componentIdentifier,
        "CVE-2007-3385",
        insightWork);

    // Create NPF auto waiver
    tempEntity.newAutoPolicyWaiver(application.getId(), 7, false, true);

    // Match the auto waiver with No Path Forward
    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId",
        Map.of(new PackageUrlIdentifier("pkg:maven/tomcat/tomcat-util@5.5.23"),
            new PresentReachableComponentVulnerabilities(Set.of("CVE-2007-3385")))))
                .when(apiVulnerabilityReachabilityStatusService)
                .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(),
                    any(VulnerabilitySignatureAnalysisDTO.class));

    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI, ClientScanType.SONATYPE, analysisDTO,
        false);

    ArgumentCaptor<List<TelemetryData>> captor = ArgumentCaptor.forClass(List.class);
    verify(mockTelemetrySender, atLeastOnce()).send(captor.capture());
    boolean foundAutoWaiverApplyTelemetry = captor.getAllValues()
        .stream()
        .flatMap(List::stream)
        .anyMatch(td -> td.getPurpose() == TelemetryPurpose.AUTO_POLICY_WAIVER
            && "APPLY".equals(td.getAttributes().get("auto_policy_waiver_action")));
    assertThat(foundAutoWaiverApplyTelemetry)
        .as("Expected to find auto waiver APPLY telemetry")
        .isTrue();
  }

  @Test
  public void testGetPolicyOwnerIdForEvaluation_ReturnsApplicationId() {
    Stage buildStage = new Stage(Stage.ID_BUILD);

    String result = scanPolicyEvaluator.getPolicyOwnerIdForEvaluation(application, buildStage);

    assertThat(result).isEqualTo(application.getId());
  }

  @Test
  public void testGetPolicyOwnerIdForEvaluation_ReturnsRepositoryId() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    testProductLicense.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);

    Stage proxyStage = new Stage(Stage.ID_PROXY);

    organization.setRelatedRepositoryId("test-repo-id");
    organizationDAO.update(organization);

    String result = scanPolicyEvaluator.getPolicyOwnerIdForEvaluation(application, proxyStage);

    assertThat(result).isEqualTo("test-repo-id");
  }

  @Test
  public void testGetPolicyOwnerIdForEvaluation_ContainerImageNonApplication_Throws() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    testProductLicense.setFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);

    Stage proxyStage = new Stage(Stage.ID_PROXY);

    Owner owner = mock(Owner.class);
    when(owner.getType()).thenReturn(OwnerType.HOSTED_REPOSITORY_COMPONENT);

    assertThatThrownBy(() -> scanPolicyEvaluator.getPolicyOwnerIdForEvaluation(owner, proxyStage))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testGetPolicyOwnerIdForEvaluation_NonContainerImageNonApplication_ReturnsOwnerId() {
    Stage buildStage = new Stage(Stage.ID_BUILD);

    Owner owner = mock(Owner.class);
    when(owner.getId()).thenReturn("owner-id");

    String result = scanPolicyEvaluator.getPolicyOwnerIdForEvaluation(owner, buildStage);

    assertThat(result).isEqualTo("owner-id");
  }

  private void restoreConstraintFactsToPreMigratedState() {
    List<PolicyViolationConstraintFacts> constraintFacts = policyViolationConstraintFactsDAO.getAll();
    PolicyViolationConstraintFacts policyViolationConstraintFacts = constraintFacts.get(0);
    try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      tx.begin();

      tx.dsl()
          .update(POLICY_VIOLATION)
          .setNull(POLICY_VIOLATION.CONSTRAINT_FACTS_ID)
          .set(POLICY_VIOLATION.CONSTRAINT_FACTS_JSON, policyViolationConstraintFacts.getConstraintFactsJson())
          .execute();

      tx.commit();
    }

    for (PolicyViolationConstraintFacts constraintFact : constraintFacts) {
      policyViolationConstraintFactsDAO.delete(constraintFact);
    }
  }

  @SuppressWarnings("unchecked")
  private void testEvaluate_DoEvaluationAfterEnablingLegacyViolations(boolean isReevaluation) throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);
    Policy policy = newSecurityPolicy();
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);

    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);

    String scanId = simulateReportIsAvailable("report");
    File scanFile = createScanFile(application, scanId);
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);
    assertThat(scanFile).isFile();

    // Make sure we don't have two evaluations at exactly the same time
    waitForTimeAdvance();

    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    // This is what enabling legacy violations performs for an application
    Date currentDate = new Date();
    try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      tx.begin();
      policyViolationDAO.getByOwnerId(application.getId()).forEach(policyViolation -> {
        policyViolationDAO.loadConstraintFacts(Collections.singletonList(policyViolation));
        policyViolation.setLegacyViolationTime(currentDate);
        policyViolationDAO.update(tx, policyViolation);
      });
      tx.commit();
    }

    clearInvocations(mockTelemetrySender);
    if (!isReevaluation) {
      scanId = simulateReportIsAvailable("report");
      scanFile = createScanFile(application, scanId);
    }
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor
        .getValue()
        .stream()
        .filter(telemetryData -> telemetryData.getPurpose().equals(TelemetryPurpose.TIME_TO_LEGACY_VIOLATION))
        .toList();

    assertThat(telemetryDataList).hasSize(36);
    for (TelemetryData telemetryData : telemetryDataList) {
      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.TIME_TO_LEGACY_VIOLATION);
      assertThat(telemetryData.getAttributes()).containsEntry(COUNT, 1);
      assertThat(telemetryData.getAttributes().get(LEGACY_VIOLATION_TIME)).isNotNull();
    }

    policyViolationDAO.getByOwnerId(application.getId())
        .forEach(policyViolation -> assertThat(policyViolation.isLegacyViolationApplied()).isTrue());
  }

  private void assertPolicyViolationsLogged(
      PolicyViolationLogEvent policyViolationLogEvent,
      Date evaluationTime,
      List<PolicyViolation> policyViolations,
      String userName) throws Exception
  {
    policyViolationDAO.loadConstraintFacts(policyViolations);
    List<PolicyViolationLogDTO> policyViolationLogDTOs =
        PolicyViolationLogDTOAssert.assertPolicyViolationLogDTOs(policyViolationLoggerOutput, policyViolationLogEvent,
            policyViolations.size());
    PolicyViolationLogDTOAssert.assertApplicationPolicyViolationData(policyViolationLogDTOs, policyViolationLogEvent,
        organization, application, evaluationTime, policyViolations, userName);
  }

  private List<PolicyViolationLogDTO> assertPolicyViolationLogDTOs(int expected) throws Exception {
    return PolicyViolationLogDTOAssert.assertPolicyViolationLogDTOs(policyViolationLoggerOutput, expected);
  }

  private static void assertContainsPolicyViolation(
      ComponentIdentifier expectedComponentIdentifier,
      String expectedHash,
      Policy expectedPolicy,
      Constraint expectedConstraint,
      String expectedActionTypeId,
      String expectedConditionTypeId,
      List<PolicyViolation> actualPolicyViolations)
  {
    assertThat(findPolicyViolation(expectedComponentIdentifier, expectedHash, expectedPolicy, expectedConstraint,
        expectedActionTypeId, expectedConditionTypeId, actualPolicyViolations))
            .as("Cannot find expected policy violation.")
            .isNotNull();
  }

  private static void assertNotContainsPolicyViolation(
      ComponentIdentifier expectedComponentIdentifier,
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

  private static PolicyViolation findPolicyViolation(
      ComponentIdentifier expectedComponentIdentifier,
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
          && actualPolicyViolation.getActionTypeId().equals(expectedActionTypeId))
      {
        assertThat(actualPolicyViolation.getConstraintFacts()).hasSize(1);
        ConstraintFact actualConstraintFact = actualPolicyViolation.getConstraintFacts().get(0);
        if (actualConstraintFact.getConstraintId().equals(expectedConstraint.getId())
            && actualConstraintFact.getConstraintName().equals(expectedConstraint.getName()))
        {
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

  @Test
  public void testEvaluate_PreservesReachabilityStatus_DuringReevaluation() throws Exception {
    // Setup: Create security policy and auto-waiver
    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    tempEntity.newAutoPolicyWaiver(application.getId(), 10, true, false);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    String vulnerabilityIdentifier = "CVE-2012-0022";

    // Initial evaluation with reachability data
    Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> reachableVulnMap = new HashMap<>();
    List<String> unreachableComponentList = List.of(
        "pkg:maven/commons-httpclient/commons-httpclient@3.1",
        "pkg:maven/org.apache.geronimo.framework/geronimo-security@2.1",
        "pkg:maven/tomcat/catalina-host-manager@5.5.23",
        "pkg:maven/org.mortbay.jetty/jetty@6.1.15",
        "pkg:maven/tomcat/servlets-default@5.5.4",
        "pkg:maven/org.openid4java/openid4java@0.9.5",
        "pkg:maven/tomcat/tomcat-util@5.4.23",
        "pkg:maven/tomcat/tomcat-util@5.5.23");
    addReachabilityMap(unreachableComponentList, reachableVulnMap);

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        scanId,
        componentIdentifier,
        vulnerabilityIdentifier,
        insightWork);

    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId", reachableVulnMap))
        .when(apiVulnerabilityReachabilityStatusService)
        .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(), any(VulnerabilitySignatureAnalysisDTO.class));

    // First evaluation: with reachability data
    ScanPolicyEvaluatorResults initialResults = scanPolicyEvaluator.evaluate(
        application, scanId, stage, ScanTriggerType.CLI, ClientScanType.SONATYPE, analysisDTO, false);

    assertThat(initialResults.autoWaivedViolations).hasSize(36);
    Optional<PolicyViolation> initialViolation =
        findPolicyViolationByVulnerabilityIdentifier(initialResults.autoWaivedViolations, vulnerabilityIdentifier);
    assertThat(initialViolation).isPresent();
    assertThat(initialViolation.get().getReachabilityStatus()).isEqualTo(NON_REACHABLE);

    // Reevaluation WITHOUT reachability data (analysisDTO = null)
    // This simulates a policy reevaluation where no new scan data is available
    ScanPolicyEvaluatorResults reevaluationResults = scanPolicyEvaluator.evaluate(
        application, scanId, stage, ScanTriggerType.CLI, ClientScanType.SONATYPE, null, false);

    // Verify reachability status is preserved
    assertThat(reevaluationResults.autoWaivedViolations).hasSize(36);
    Optional<PolicyViolation> reevaluatedViolation =
        findPolicyViolationByVulnerabilityIdentifier(reevaluationResults.autoWaivedViolations, vulnerabilityIdentifier);
    assertThat(reevaluatedViolation).isPresent();
    assertThat(reevaluatedViolation.get().getReachabilityStatus()).isEqualTo(NON_REACHABLE);

    // Verify all auto-waived violations preserve their reachability status
    reevaluationResults.autoWaivedViolations
        .forEach(violation -> assertThat(violation.getReachabilityStatus()).isNotNull());
  }

  @Test
  public void testEvaluate_UsesNewReachabilityData_WhenAvailable() throws Exception {
    // Setup: Create security policy and auto-waiver
    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    tempEntity.newAutoPolicyWaiver(application.getId(), 10, true, false);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    String vulnerabilityIdentifier = "CVE-2012-0022";

    // Initial evaluation: vulnerability is NON_REACHABLE
    Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> initialReachableVulnMap = new HashMap<>();
    List<String> unreachableComponentList = List.of(
        "pkg:maven/commons-httpclient/commons-httpclient@3.1",
        "pkg:maven/org.apache.geronimo.framework/geronimo-security@2.1",
        "pkg:maven/tomcat/catalina-host-manager@5.5.23",
        "pkg:maven/org.mortbay.jetty/jetty@6.1.15",
        "pkg:maven/tomcat/servlets-default@5.5.4",
        "pkg:maven/org.openid4java/openid4java@0.9.5",
        "pkg:maven/tomcat/tomcat-util@5.4.23",
        "pkg:maven/tomcat/tomcat-util@5.5.23");
    addReachabilityMap(unreachableComponentList, initialReachableVulnMap);

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        scanId,
        componentIdentifier,
        vulnerabilityIdentifier,
        insightWork);

    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId", initialReachableVulnMap))
        .when(apiVulnerabilityReachabilityStatusService)
        .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(), any(VulnerabilitySignatureAnalysisDTO.class));

    // First evaluation
    ScanPolicyEvaluatorResults initialResults = scanPolicyEvaluator.evaluate(
        application, scanId, stage, ScanTriggerType.CLI, ClientScanType.SONATYPE, analysisDTO, false);

    assertThat(initialResults.autoWaivedViolations).hasSize(36);
    assertThat(initialResults.activeViolations).isEmpty();

    // Second evaluation: new reachability data shows vulnerability is REACHABLE
    Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> newReachableVulnMap = new HashMap<>();
    newReachableVulnMap.put(
        new PackageUrlIdentifier("pkg:maven/tomcat/tomcat-util@5.5.23"),
        new PresentReachableComponentVulnerabilities(Set.of(vulnerabilityIdentifier)));
    // Other components remain unreachable
    List<String> stillUnreachableComponents = List.of(
        "pkg:maven/commons-httpclient/commons-httpclient@3.1",
        "pkg:maven/org.apache.geronimo.framework/geronimo-security@2.1",
        "pkg:maven/tomcat/catalina-host-manager@5.5.23",
        "pkg:maven/org.mortbay.jetty/jetty@6.1.15",
        "pkg:maven/tomcat/servlets-default@5.5.4",
        "pkg:maven/org.openid4java/openid4java@0.9.5",
        "pkg:maven/tomcat/tomcat-util@5.4.23");
    addReachabilityMap(stillUnreachableComponents, newReachableVulnMap);

    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId", newReachableVulnMap))
        .when(apiVulnerabilityReachabilityStatusService)
        .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(), any(VulnerabilitySignatureAnalysisDTO.class));

    // Second evaluation with NEW reachability data
    ScanPolicyEvaluatorResults newResults = scanPolicyEvaluator.evaluate(
        application, scanId, stage, ScanTriggerType.CLI, ClientScanType.SONATYPE, analysisDTO, false);

    // New reachability data takes precedence
    // The previously auto-waived violation should now be active because it's REACHABLE
    assertThat(newResults.activeViolations).hasSize(1);
    assertThat(newResults.autoWaivedViolations).hasSize(35);

    Optional<PolicyViolation> activeViolation =
        findPolicyViolationByVulnerabilityIdentifier(newResults.activeViolations, vulnerabilityIdentifier);
    assertThat(activeViolation).isPresent();
    assertThat(activeViolation.get().getReachabilityStatus()).isEqualTo(REACHABLE);
  }

  @Test
  public void testEvaluate_PreservesReachabilityBasedAutoWaiversDuringReevaluation() throws Exception {
    // Setup: Create security policy and reachability-based auto-waiver
    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    // Create auto-waiver with reachability scope (non-reachable vulnerabilities)
    tempEntity.newAutoPolicyWaiver(application.getId(), 10, true, false);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    String vulnerabilityIdentifier = "CVE-2012-0022";

    // Initial evaluation with reachability data showing vulnerability is non-reachable
    Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> reachableVulnMap = new HashMap<>();
    List<String> unreachableComponentList = List.of(
        "pkg:maven/commons-httpclient/commons-httpclient@3.1",
        "pkg:maven/org.apache.geronimo.framework/geronimo-security@2.1",
        "pkg:maven/tomcat/catalina-host-manager@5.5.23",
        "pkg:maven/org.mortbay.jetty/jetty@6.1.15",
        "pkg:maven/tomcat/servlets-default@5.5.4",
        "pkg:maven/org.openid4java/openid4java@0.9.5",
        "pkg:maven/tomcat/tomcat-util@5.4.23",
        "pkg:maven/tomcat/tomcat-util@5.5.23");
    addReachabilityMap(unreachableComponentList, reachableVulnMap);

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        scanId,
        componentIdentifier,
        vulnerabilityIdentifier,
        insightWork);

    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId", reachableVulnMap))
        .when(apiVulnerabilityReachabilityStatusService)
        .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(), any(VulnerabilitySignatureAnalysisDTO.class));

    // First evaluation: violations should be auto-waived due to NON_REACHABLE status
    ScanPolicyEvaluatorResults initialResults = scanPolicyEvaluator.evaluate(
        application, scanId, stage, ScanTriggerType.CLI, ClientScanType.SONATYPE, analysisDTO, false);

    assertThat(initialResults.autoWaivedViolations).hasSize(36);
    assertThat(initialResults.activeViolations).isEmpty();
    Optional<PolicyViolation> autoWaivedViolation =
        findPolicyViolationByVulnerabilityIdentifier(initialResults.autoWaivedViolations, vulnerabilityIdentifier);
    assertThat(autoWaivedViolation).isPresent();
    assertThat(autoWaivedViolation.get().getReachabilityStatus()).isEqualTo(NON_REACHABLE);
    assertThat(autoWaivedViolation.get().isAutoWaived()).isTrue();

    // Reevaluation WITHOUT new reachability data
    // Auto-waiver should be maintained because reachability data is preserved
    ScanPolicyEvaluatorResults reevaluationResults = scanPolicyEvaluator.evaluate(
        application, scanId, stage, ScanTriggerType.CLI, ClientScanType.SONATYPE, null, false);

    // Verify auto-waiver is maintained due to preserved reachability data
    assertThat(reevaluationResults.autoWaivedViolations).hasSize(36);
    assertThat(reevaluationResults.activeViolations).isEmpty();
    Optional<PolicyViolation> stillAutoWaivedViolation =
        findPolicyViolationByVulnerabilityIdentifier(reevaluationResults.autoWaivedViolations, vulnerabilityIdentifier);
    assertThat(stillAutoWaivedViolation).isPresent();
    assertThat(stillAutoWaivedViolation.get().getReachabilityStatus()).isEqualTo(NON_REACHABLE);
    assertThat(stillAutoWaivedViolation.get().isAutoWaived()).isTrue();

    // Verify all auto-waived violations maintained their reachability status
    reevaluationResults.autoWaivedViolations
        .forEach(violation -> assertThat(violation.getReachabilityStatus()).isEqualTo(NON_REACHABLE));
  }

  @Test
  public void testEvaluate_DoesNotPreserveReachabilityStatus_ForNewScan() throws Exception {
    // Setup: Create security policy and auto-waiver
    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    tempEntity.newAutoPolicyWaiver(application.getId(), 10, true, false);

    Stage stage = new Stage(Stage.ID_BUILD);
    String firstScanId = simulateReportIsAvailable("AutoWaiverRevocations");

    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    String vulnerabilityIdentifier = "CVE-2012-0022";

    // First scan: with reachability data showing vulnerability is NON_REACHABLE
    Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> reachableVulnMap = new HashMap<>();
    List<String> unreachableComponentList = List.of(
        "pkg:maven/commons-httpclient/commons-httpclient@3.1",
        "pkg:maven/org.apache.geronimo.framework/geronimo-security@2.1",
        "pkg:maven/tomcat/catalina-host-manager@5.5.23",
        "pkg:maven/org.mortbay.jetty/jetty@6.1.15",
        "pkg:maven/tomcat/servlets-default@5.5.4",
        "pkg:maven/org.openid4java/openid4java@0.9.5",
        "pkg:maven/tomcat/tomcat-util@5.4.23",
        "pkg:maven/tomcat/tomcat-util@5.5.23");
    addReachabilityMap(unreachableComponentList, reachableVulnMap);

    VulnerabilitySignatureAnalysisDTO firstAnalysisDTO = createTestAnalysisDTO(
        application.getId(),
        firstScanId,
        componentIdentifier,
        vulnerabilityIdentifier,
        insightWork);

    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), firstScanId, reachableVulnMap))
        .when(apiVulnerabilityReachabilityStatusService)
        .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(), any(VulnerabilitySignatureAnalysisDTO.class));

    // First evaluation: violations should be auto-waived due to NON_REACHABLE status
    ScanPolicyEvaluatorResults firstResults = scanPolicyEvaluator.evaluate(
        application, firstScanId, stage, ScanTriggerType.CLI, ClientScanType.SONATYPE, firstAnalysisDTO, false);

    assertThat(firstResults.autoWaivedViolations).hasSize(36);
    Optional<PolicyViolation> firstViolation =
        findPolicyViolationByVulnerabilityIdentifier(firstResults.autoWaivedViolations, vulnerabilityIdentifier);
    assertThat(firstViolation).isPresent();
    assertThat(firstViolation.get().getReachabilityStatus()).isEqualTo(NON_REACHABLE);
    assertThat(firstViolation.get().isAutoWaived()).isTrue();

    // NEW scan with DIFFERENT scanId and NO reachability data
    // This simulates a fresh scan, NOT a reevaluation
    String secondScanId = simulateReportIsAvailable("AutoWaiverRevocations");
    assertThat(secondScanId).isNotEqualTo(firstScanId); // Verify different scan IDs

    // Second evaluation WITHOUT reachability data (analysisDTO = null)
    // This is a new primary scan, so it should not preserve old reachability data
    ScanPolicyEvaluatorResults secondResults = scanPolicyEvaluator.evaluate(
        application, secondScanId, stage, ScanTriggerType.CLI, ClientScanType.SONATYPE, null, false);

    // Reachability status should be UNKNOWN (or null), NOT preserved from first scan
    // Since there's no reachability data in the new scan, and reachability-based auto-waivers
    // require reachability data, the violations should be ACTIVE (not auto-waived)
    assertThat(secondResults.activeViolations).hasSize(36);
    assertThat(secondResults.autoWaivedViolations).isEmpty();

    Optional<PolicyViolation> secondViolation =
        findPolicyViolationByVulnerabilityIdentifier(secondResults.activeViolations, vulnerabilityIdentifier);
    assertThat(secondViolation).isPresent();

    // Reachability status should NOT be preserved from first scan
    // It should be null or UNKNOWN, NOT NON_REACHABLE
    ReachabilityStatus secondStatus = secondViolation.get().getReachabilityStatus();
    assertThat(secondStatus)
        .isIn(null, ReachabilityStatus.UNKNOWN)
        .isNotEqualTo(NON_REACHABLE);
  }

  @Test
  public void testEvaluateForMonitoring_PreservesReachabilityStatus() throws Exception {
    // CLM-38947: Test that Continuous Monitoring preserves reachability status
    // Setup: Create security policy and reachability-based auto-waiver
    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    tempEntity.newAutoPolicyWaiver(application.getId(), 10, true, false);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    String vulnerabilityIdentifier = "CVE-2012-0022";

    // Initial evaluation with reachability data showing vulnerability is NON_REACHABLE
    Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> reachableVulnMap = new HashMap<>();
    List<String> unreachableComponentList = List.of(
        "pkg:maven/commons-httpclient/commons-httpclient@3.1",
        "pkg:maven/org.apache.geronimo.framework/geronimo-security@2.1",
        "pkg:maven/tomcat/catalina-host-manager@5.5.23",
        "pkg:maven/org.mortbay.jetty/jetty@6.1.15",
        "pkg:maven/tomcat/servlets-default@5.5.4",
        "pkg:maven/org.openid4java/openid4java@0.9.5",
        "pkg:maven/tomcat/tomcat-util@5.4.23",
        "pkg:maven/tomcat/tomcat-util@5.5.23");
    addReachabilityMap(unreachableComponentList, reachableVulnMap);

    VulnerabilitySignatureAnalysisDTO analysisDTO = createTestAnalysisDTO(
        application.getId(),
        scanId,
        componentIdentifier,
        vulnerabilityIdentifier,
        insightWork);

    doReturn(new PurlIdentifiersWithVulnerabilities(application.getId(), "scanId", reachableVulnMap))
        .when(apiVulnerabilityReachabilityStatusService)
        .getPurlIdentifiersWithVulnerabilities(anyString(), anyString(), any(VulnerabilitySignatureAnalysisDTO.class));

    // First evaluation: with reachability data, violations should be auto-waived due to NON_REACHABLE status
    ScanPolicyEvaluatorResults initialResults = scanPolicyEvaluator.evaluate(
        application, scanId, stage, ScanTriggerType.CLI, ClientScanType.SONATYPE, analysisDTO, false);

    assertThat(initialResults.autoWaivedViolations).hasSize(36);
    assertThat(initialResults.activeViolations).isEmpty();
    Optional<PolicyViolation> initialViolation =
        findPolicyViolationByVulnerabilityIdentifier(initialResults.autoWaivedViolations, vulnerabilityIdentifier);
    assertThat(initialViolation).isPresent();
    assertThat(initialViolation.get().getReachabilityStatus()).isEqualTo(NON_REACHABLE);
    assertThat(initialViolation.get().isAutoWaived()).isTrue();

    // Continuous Monitoring with NEW scanId (CM creates new scan) and NO new reachability data
    // This is the key difference from re-evaluation: CM uses a different scanId
    String monitoringScanId = simulateReportIsAvailable("AutoWaiverRevocations");
    assertThat(monitoringScanId).isNotEqualTo(scanId); // Verify CM creates new scanId

    // CM evaluation WITHOUT new reachability data (analysisDTO = null)
    // Before CLM-38947 fix, this would lose reachability status because isReevaluation=false
    ScanPolicyEvaluatorResults monitoringResults = scanPolicyEvaluator.evaluateForMonitoring(
        application, monitoringScanId, stage, ScanTriggerType.CLI, ClientScanType.SONATYPE);

    // Verify reachability status is preserved during CM and auto-waivers are maintained
    assertThat(monitoringResults.autoWaivedViolations).hasSize(36);
    assertThat(monitoringResults.activeViolations).isEmpty();
    Optional<PolicyViolation> monitoredViolation =
        findPolicyViolationByVulnerabilityIdentifier(monitoringResults.autoWaivedViolations, vulnerabilityIdentifier);
    assertThat(monitoredViolation).isPresent();
    assertThat(monitoredViolation.get().getReachabilityStatus()).isEqualTo(NON_REACHABLE);
    assertThat(monitoredViolation.get().isAutoWaived()).isTrue();

    // Verify all auto-waived violations maintained their reachability status during CM
    monitoringResults.autoWaivedViolations
        .forEach(violation -> assertThat(violation.getReachabilityStatus()).isEqualTo(NON_REACHABLE));
  }

  @Test
  public void testEvaluateForMonitoring_DoesNotPreserveReachabilityStatus_WhenNoPriorData() throws Exception {
    // CLM-38947: Negative test - Verify CM does NOT retroactively add reachability status
    // when the prior scan never had reachability data
    Policy securityPolicy = new Policy(null, "Security Policy");
    securityPolicy.setThreatLevel(8);
    securityPolicy.setOwnerId(application.getId());
    Constraint constraint = new Constraint(null, "TestConstraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0"));
    securityPolicy.addConstraint(constraint);
    tempEntity.newPolicy(securityPolicy);

    // Create auto-waiver with "Not Reachable" scope
    tempEntity.newAutoPolicyWaiver(application.getId(), 10, true, false);

    Stage stage = new Stage(Stage.ID_BUILD);
    String scanId = simulateReportIsAvailable("AutoWaiverRevocations");

    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("tomcat", "tomcat-util", "5.5.23");
    String vulnerabilityIdentifier = "CVE-2012-0022";

    // Initial evaluation WITHOUT reachability data (no -ra flag)
    // This simulates a scan without callflow analysis
    ScanPolicyEvaluatorResults initialResults = scanPolicyEvaluator.evaluate(
        application, scanId, stage, ScanTriggerType.CLI, ClientScanType.SONATYPE, null, false);

    // Verify: All violations are ACTIVE (not auto-waived) because no reachability data
    assertThat(initialResults.activeViolations).hasSize(36);
    assertThat(initialResults.autoWaivedViolations).isEmpty();

    Optional<PolicyViolation> initialViolation =
        findPolicyViolationByVulnerabilityIdentifier(initialResults.activeViolations, vulnerabilityIdentifier);
    assertThat(initialViolation).isPresent();

    // Verify: No reachability status (null or UNKNOWN)
    ReachabilityStatus initialStatus = initialViolation.get().getReachabilityStatus();
    assertThat(initialStatus).isIn(null, ReachabilityStatus.UNKNOWN);

    // CM runs - also without new reachability data
    String monitoringScanId = simulateReportIsAvailable("AutoWaiverRevocations");
    assertThat(monitoringScanId).isNotEqualTo(scanId); // Verify CM creates new scanId

    ScanPolicyEvaluatorResults monitoringResults = scanPolicyEvaluator.evaluateForMonitoring(
        application, monitoringScanId, stage, ScanTriggerType.CLI, ClientScanType.SONATYPE);

    // Verify: Violations remain ACTIVE (not auto-waived) after CM
    // CM should NOT retroactively add reachability status from nowhere
    assertThat(monitoringResults.activeViolations).hasSize(36);
    assertThat(monitoringResults.autoWaivedViolations).isEmpty();

    Optional<PolicyViolation> monitoredViolation =
        findPolicyViolationByVulnerabilityIdentifier(monitoringResults.activeViolations, vulnerabilityIdentifier);
    assertThat(monitoredViolation).isPresent();

    // Verify: Still no reachability status after CM (should not be fabricated)
    ReachabilityStatus monitoredStatus = monitoredViolation.get().getReachabilityStatus();
    assertThat(monitoredStatus)
        .isIn(null, ReachabilityStatus.UNKNOWN)
        .isNotEqualTo(NON_REACHABLE); // Must NOT incorrectly set to NON_REACHABLE

    // Verify: All violations have null/UNKNOWN reachability (not incorrectly set to NON_REACHABLE)
    monitoringResults.activeViolations.forEach(violation -> {
      ReachabilityStatus status = violation.getReachabilityStatus();
      assertThat(status)
          .isIn(null, ReachabilityStatus.UNKNOWN)
          .describedAs("Violation should not have fabricated reachability status");
    });
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

  private void assertApplicationComponent(
      ComponentIdentifier componentIdentifier,
      Date time,
      OwnerComponent actual)
  {
    assertThat(actual.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(actual.getTime()).isEqualTo(time);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testEvaluate_AuditTelemetryForUnchangedViolations() throws Exception {
    // Given: A policy that will trigger violations
    Stage stage = new Stage(Stage.ID_BUILD);
    Condition securityCondition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "1");
    Constraint constraint = new Constraint(null, "Security Constraint", LogicalOperator.OR);
    constraint.setConditions(Arrays.asList(securityCondition));
    tempEntity.newPolicy("Security Policy", constraint);

    String scanId = simulateReportIsAvailable("report");
    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    clearInvocations(mockTelemetrySender);

    // When: Running the first evaluation to create violations
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    clearInvocations(mockTelemetrySender);

    // When: Running a second evaluation with the same scan (violations remain unchanged)
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    // Then: Verify CONDITION_TYPE_VIOLATION_AUDIT telemetry was collected for unchanged violations
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor
        .getValue()
        .stream()
        .filter(telemetryData -> telemetryData.getPurpose().equals(TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT))
        .toList();

    // Verify we have audit telemetry for the unchanged violations
    assertThat(telemetryDataList).isNotEmpty();

    // Verify the telemetry contains policy constraints and policy_violation_id
    for (TelemetryData telemetryData : telemetryDataList) {
      assertThat(telemetryData.getAttributes()).containsKey(PolicyViolationTelemetryCollector.POLICY_CONSTRAINTS);
      List<Constraint> constraints = (List<Constraint>) telemetryData.getAttributes()
          .get(PolicyViolationTelemetryCollector.POLICY_CONSTRAINTS);
      assertThat(constraints).isNotEmpty();

      // Verify the constraints have conditions
      for (Constraint c : constraints) {
        assertThat(c.getConditions()).isNotEmpty();
      }

      // Verify policy_violation_id is not null (regression test for bug fix)
      String policyViolationId = (String) telemetryData.getAttributes()
          .get(PolicyViolationTelemetryCollector.POLICY_VIOLATION_ID);
      assertThat(policyViolationId)
          .as("policy_violation_id must not be null for audit telemetry")
          .isNotNull();
    }
  }

  @Test
  public void testEvaluate_AuditTelemetryWithMultipleConstraints() throws Exception {
    // Given: A policy with multiple constraints
    Stage stage = new Stage(Stage.ID_BUILD);
    Condition condition1 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "7");
    Condition condition2 = new Condition(ComponentCategoryConditionType.ID, "is not", "113");
    Constraint constraint1 = new Constraint(null, "Constraint 1", LogicalOperator.AND);
    constraint1.setConditions(Arrays.asList(condition1));

    Constraint constraint2 = new Constraint(null, "Constraint 2", LogicalOperator.OR);
    constraint2.setConditions(Arrays.asList(condition2));

    tempEntity.newPolicy("Multi-Constraint Policy", constraint1, constraint2);

    String scanId = simulateReportIsAvailable("report");
    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);

    // When: Running first evaluation
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    clearInvocations(mockTelemetrySender);

    // When: Running second evaluation
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    // Then Verify audit telemetry contains all constraints
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> auditTelemetryList = telemetryDataArgumentCaptor
        .getValue()
        .stream()
        .filter(telemetryData -> telemetryData.getPurpose().equals(TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT))
        .toList();

    assertThat(auditTelemetryList).isNotEmpty();

    // Verify each telemetry entry contains constraint data
    // Note: When a policy has multiple constraints, each violation contains constraint facts
    // for the specific constraint that was violated, not all constraints from the policy
    auditTelemetryList.forEach(telemetryData -> {
      List<Constraint> constraints = (List<Constraint>) telemetryData.getAttributes()
          .get(PolicyViolationTelemetryCollector.POLICY_CONSTRAINTS);
      assertThat(constraints).isNotNull();
      assertThat(constraints).isNotEmpty();
    });
  }

  @Test
  public void testEvaluate_AuditTelemetryWithNullConstraintFacts() throws Exception {
    // Given: A policy that will trigger violations
    Stage stage = new Stage(Stage.ID_BUILD);
    newSecurityPolicy();
    String scanId = simulateReportIsAvailable("report");

    // When Running first evaluation
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    // Note: Cannot set constraint facts to null as AbstractPolicyViolation validates this.
    // Instead, we verify that audit telemetry is collected properly for existing violations.

    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    clearInvocations(mockTelemetrySender);

    // When Running second evaluation
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    // Then Verify audit telemetry was collected without errors
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> auditTelemetryList = telemetryDataArgumentCaptor
        .getValue()
        .stream()
        .filter(telemetryData -> telemetryData.getPurpose().equals(TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT))
        .toList();

    // Should collect telemetry for violations with valid constraint facts
    assertThat(auditTelemetryList).isNotEmpty();
  }

  @Test
  public void testEvaluate_AuditTelemetryForWaivedViolations() throws Exception {
    // Given: A policy with a waiver
    Stage stage = new Stage(Stage.ID_BUILD);
    Policy policy = newSecurityPolicy();
    tempEntity.newWaiver(policy.getId(), application.getId());

    String scanId = simulateReportIsAvailable("report");
    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);

    // When: Running first evaluation (creates waived violations)
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    clearInvocations(mockTelemetrySender);

    // When: Running second evaluation (waived violations remain unchanged)
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    // Then: Verify audit telemetry is collected for waived violations
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> auditTelemetryList = telemetryDataArgumentCaptor
        .getValue()
        .stream()
        .filter(telemetryData -> telemetryData.getPurpose().equals(TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT))
        .toList();

    // Should collect audit telemetry even for waived violations
    assertThat(auditTelemetryList).isNotEmpty();

    // Verify each telemetry entry has constraint data
    for (TelemetryData telemetryData : auditTelemetryList) {
      List<Constraint> constraints = (List<Constraint>) telemetryData.getAttributes()
          .get(PolicyViolationTelemetryCollector.POLICY_CONSTRAINTS);
      assertThat(constraints).isNotNull();
    }
  }

  @Test
  public void testEvaluate_AuditAndRegularTelemetryBothCollected() throws Exception {
    // Given: A policy that will trigger violations
    Stage stage = new Stage(Stage.ID_BUILD);
    newSecurityPolicy();
    String scanId = simulateReportIsAvailable("report");
    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    clearInvocations(mockTelemetrySender);

    // When: Running first evaluation (creates new violations)
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    // Then: Should have regular CONDITION_TYPE_VIOLATION telemetry for new violations
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> regularTelemetryList = telemetryDataArgumentCaptor
        .getValue()
        .stream()
        .filter(telemetryData -> telemetryData.getPurpose().equals(TelemetryPurpose.CONDITION_TYPE_VIOLATION))
        .toList();

    assertThat(regularTelemetryList).isNotEmpty();

    clearInvocations(mockTelemetrySender);

    // When: Running second evaluation (violations remain unchanged)
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    // Then: Should have AUDIT telemetry for unchanged violations
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> auditTelemetryList = telemetryDataArgumentCaptor
        .getValue()
        .stream()
        .filter(telemetryData -> telemetryData.getPurpose().equals(TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT))
        .toList();

    assertThat(auditTelemetryList).isNotEmpty();

    // Verify both use the same constraint structure (built by buildTelemetryConstraints)
    List<Constraint> regularConstraints = (List<Constraint>) regularTelemetryList.get(0)
        .getAttributes()
        .get(PolicyViolationTelemetryCollector.POLICY_CONSTRAINTS);
    List<Constraint> auditConstraints = (List<Constraint>) auditTelemetryList.get(0)
        .getAttributes()
        .get(PolicyViolationTelemetryCollector.POLICY_CONSTRAINTS);

    // Both should have constraint data
    assertThat(regularConstraints).isNotEmpty();
    assertThat(auditConstraints).isNotEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testEvaluate_AuditTelemetryContainsPolicyViolationId() throws Exception {
    // Given: A policy that will trigger violations
    Stage stage = new Stage(Stage.ID_BUILD);
    newSecurityPolicy();
    String scanId = simulateReportIsAvailable("report");

    // When: Running first evaluation to create violations
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    // Get the created policy violation IDs from database
    List<PolicyViolation> createdViolations = policyViolationDAO.getByOwnerId(application.getId());
    assertThat(createdViolations).isNotEmpty();

    ArgumentCaptor<List<TelemetryData>> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    clearInvocations(mockTelemetrySender);

    // When: Running second evaluation (unchanged violations should trigger audit telemetry)
    scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
        ClientScanType.SONATYPE, false);

    // Then: Verify CONDITION_TYPE_VIOLATION_AUDIT telemetry contains policy_violation_id
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    List<TelemetryData> auditTelemetryList = telemetryDataArgumentCaptor
        .getValue()
        .stream()
        .filter(telemetryData -> telemetryData.getPurpose().equals(TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT))
        .toList();

    // Verify audit telemetry was collected
    assertThat(auditTelemetryList).isNotEmpty();

    // CRITICAL: Verify each audit telemetry entry has a valid policy_violation_id from database
    Set<String> dbViolationIds = createdViolations.stream()
        .map(PolicyViolation::getId)
        .collect(toSet());

    for (TelemetryData telemetryData : auditTelemetryList) {
      String policyViolationId = (String) telemetryData.getAttributes()
          .get(PolicyViolationTelemetryCollector.POLICY_VIOLATION_ID);

      // Verify policy_violation_id is not null (this was the bug - it was null before fix)
      assertThat(policyViolationId)
          .as("policy_violation_id must not be null for CONDITION_TYPE_VIOLATION_AUDIT telemetry")
          .isNotNull();

      // Verify policy_violation_id matches one of the database IDs
      assertThat(dbViolationIds)
          .as("policy_violation_id must match an existing violation ID from database")
          .contains(policyViolationId);
    }
  }

  /**
   * Simulates that a report (based on the specified resource) exists.
   *
   * @param reportResourceName can be a report.zip file or a directory that will be zipped up into a report.
   * @return A generated scan ID that can be used in subsequent calls to evaluate policies.
   */
  private String simulateReportIsAvailable(String reportResourceName) {
    return mockReportDownloader.mockDownloadReport("/" + getClass().getSimpleName() + "/" + reportResourceName);
  }

  /**
   * Adds a reachable component vulnerability to the map for each package URL identifier.
   *
   * @param packageUrlIdentifiers the list of package URL identifiers
   * @param reachableComponentVulnerabilitiesMap the map to add the reachable component vulnerabilities to
   */
  private void addReachabilityMap(
      List<String> packageUrlIdentifiers,
      Map<PackageUrlIdentifier, ReachableComponentVulnerabilities> reachableComponentVulnerabilitiesMap)
  {
    for (String packageUrlIdentifier : packageUrlIdentifiers) {
      PackageUrlIdentifier packageUrl = new PackageUrlIdentifier(packageUrlIdentifier);
      reachableComponentVulnerabilitiesMap.put(packageUrl,
          new PresentReachableComponentVulnerabilities(Set.of("UNKNOWN_CVE")));
    }
  }

  private boolean assertConditionTypeExistsInTelemetryData(
      String conditionType,
      List<TelemetryData> telemetryDataList)
  {
    return telemetryDataList.stream()
        .anyMatch(
            telemetryData -> {
              List<Constraint> constraints = (List<Constraint>) telemetryData.getAttributes()
                  .get(PolicyViolationTelemetryCollector.POLICY_CONSTRAINTS);
              return constraints.stream()
                  .anyMatch(c -> c.getConditions()
                      .stream()
                      .anyMatch(cond -> cond.getConditionTypeId().equals(conditionType)));
            });
  }

  @Test
  public void testSendMissingEpssScoreTelemetry_noVulnerabilities() {
    // Components without any vulnerabilities
    List<Component> components = List.of(
        new Component(ComponentIdentifier.createMavenCoordinates("g", "a", "v")),
        new Component(ComponentIdentifier.createNpmCoordinates("p", "v")));

    scanPolicyEvaluator.sendMissingEpssScoreTelemetry("appId", "scanId", "stageId", components);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());

    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    assertThat(telemetryData.getAttributes())
        .containsKey("application_id")
        .containsKey("scan_id")
        .containsEntry("stage_id", "stageId")
        .containsEntry("vulnerabilities_with_missing_epss", "0");
  }

  @Test
  public void testSendMissingEpssScoreTelemetry_withMissingEpssData() {
    // Components with vulnerabilities but missing EPSS data
    Component component1 = new Component(ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    component1.addSecurityVulnerability(new SecurityVulnerability("CVE", "CVE-2021-1234", 7.5f));
    component1.addSecurityVulnerability(new SecurityVulnerability("CVE", "CVE-2021-5678", 5.0f));

    Component component2 = new Component(ComponentIdentifier.createNpmCoordinates("p", "v"));
    component2.addSecurityVulnerability(new SecurityVulnerability("CVE", "CVE-2022-9999", 9.0f));

    List<Component> components = List.of(component1, component2);

    scanPolicyEvaluator.sendMissingEpssScoreTelemetry("appId", "scanId", "stageId", components);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());

    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    assertThat(telemetryData.getAttributes())
        .containsKey("application_id")
        .containsKey("scan_id")
        .containsEntry("stage_id", "stageId")
        .containsEntry("vulnerabilities_with_missing_epss", "3");
  }

  @Test
  public void testSendMissingEpssScoreTelemetry_withMixedEpssData() {
    // Components with some vulnerabilities having EPSS data and some without
    Component component1 = new Component(ComponentIdentifier.createMavenCoordinates("g", "a", "v"));

    SecurityVulnerability vulnWithEpss = new SecurityVulnerability("CVE", "CVE-2021-1234", 7.5f);
    EpssData epssData = new EpssData(0.75);
    vulnWithEpss.setEpssData(epssData);
    component1.addSecurityVulnerability(vulnWithEpss);

    SecurityVulnerability vulnWithoutEpss = new SecurityVulnerability("CVE", "CVE-2021-5678", 5.0f);
    component1.addSecurityVulnerability(vulnWithoutEpss);

    Component component2 = new Component(ComponentIdentifier.createNpmCoordinates("p", "v"));
    SecurityVulnerability vulnWithNullScore = new SecurityVulnerability("CVE", "CVE-2022-1111", 8.0f);
    EpssData epssDataNullScore = new EpssData(null);
    vulnWithNullScore.setEpssData(epssDataNullScore);
    component2.addSecurityVulnerability(vulnWithNullScore);

    List<Component> components = List.of(component1, component2);

    scanPolicyEvaluator.sendMissingEpssScoreTelemetry("appId", "scanId", "stageId", components);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());

    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    // Should count: vulnWithoutEpss (1) + vulnWithNullScore (1) = 2
    assertThat(telemetryData.getAttributes())
        .containsKey("application_id")
        .containsKey("scan_id")
        .containsEntry("stage_id", "stageId")
        .containsEntry("vulnerabilities_with_missing_epss", "2");
  }

  @Test
  public void testCreatePolicyEvaluationResult_SkipLoadingTotalComponentCount() throws Exception {
    // Setup: Create a policy evaluation (report doesn't need to exist since we're skipping the read)
    String scanId = "test-scan-id";
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, scanId);
    List<PolicyViolation> violations = Collections.emptyList();

    // Execute: Call with loadTotalComponentCount=false to skip expensive I/O
    PolicyEvaluationResult result = scanPolicyEvaluator.createPolicyEvaluationResult(
        policyEvaluation,
        Collections.emptyList(),
        violations,
        false,
        null,
        false // Skip loading total component count - optimization for /rest/application/services/summary
    );

    // Verify: Total component count should be 0 (not loaded), but result is valid
    assertThat(result).isNotNull();
    assertThat(result.getTotalComponentCount()).isEqualTo(0);
  }

  @Test
  public void testEvaluate_FirewallContext_LegacyViolationsEnforced() throws Exception {
    // Firewall violations should NEVER be marked as legacy, and ALL violations should be enforced
    Stage proxyStage = new Stage(Stage.ID_PROXY);
    Repository repository = createTestRepository();
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    Policy policy = newSecurityPolicy();
    policy.setLegacyViolationAllowed(true);
    policy.getActions().put(Stage.ID_PROXY, Action.ID_FAIL);
    policy.setOwnerId(repository.getId());
    policyDAO.update(policy);

    String scanId = simulateReportIsAvailable("report");
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, proxyStage,
        ScanTriggerType.REPOSITORY_MANAGER, ClientScanType.SONATYPE, false);

    // Manually mark all violations as legacy to simulate pre-fix data
    List<PolicyViolation> allViolations = results.allViolations;
    try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      tx.begin();
      for (PolicyViolation violation : allViolations) {
        violation.setLegacyViolationTime(new Date());
        policyViolationDAO.update(tx, violation);
      }
      tx.commit();
    }

    // Re-evaluate - Guards should clear legacy time from proxy violations
    results = scanPolicyEvaluator.evaluate(application, scanId, proxyStage,
        ScanTriggerType.REPOSITORY_MANAGER, ClientScanType.SONATYPE, false);

    // Verify Firewall violations do NOT have legacy time (Guards cleared it)
    List<PolicyViolation> activeViolations = results.activeViolations;
    long legacyCount = activeViolations.stream()
        .filter(PolicyViolation::isLegacyViolation)
        .count();

    // Firewall violations should NEVER be legacy - legacy count should be 0
    assertThat(legacyCount).isEqualTo(0L);
    // ALL violations should still be active (enforced)
    assertThat(activeViolations.size()).isEqualTo(allViolations.size());
  }

  @Test
  public void testEvaluate_LifecycleContext_LegacyViolationsExcluded() throws Exception {
    // Lifecycle should exclude ALL legacy violations
    Stage buildStage = new Stage(Stage.ID_BUILD);
    Policy policy = newSecurityPolicy();
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);

    String scanId = simulateReportIsAvailable("report");
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, buildStage,
        ScanTriggerType.CLI, ClientScanType.SONATYPE, false);

    // Mark all violations as legacy
    List<PolicyViolation> allViolations = results.allViolations;
    try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      tx.begin();
      for (PolicyViolation violation : allViolations) {
        violation.setLegacyViolationTime(new Date());
        policyViolationDAO.update(tx, violation);
      }
      tx.commit();
    }

    // Re-evaluate
    results = scanPolicyEvaluator.evaluate(application, scanId, buildStage,
        ScanTriggerType.CLI, ClientScanType.SONATYPE, false);

    // Verify NO legacy violations are in active alerts (Lifecycle behavior)
    List<PolicyViolation> activeViolations = results.activeViolations;
    long legacyInActiveCount = activeViolations.stream()
        .filter(PolicyViolation::isLegacyViolation)
        .count();

    assertThat(legacyInActiveCount).isEqualTo(0);
  }

  @Test
  public void testEvaluate_ProxyStage_ViolationsNotMarkedAsLegacyOnFirstEvaluation() throws Exception {
    Repository repository = createTestRepository();
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    Policy policy = newSecurityPolicy();
    policy.setLegacyViolationAllowed(true);
    policy.getActions().put(Stage.ID_PROXY, Action.ID_FAIL);
    policy.setOwnerId(repository.getId());
    policyDAO.update(policy);

    String scanId = simulateReportIsAvailable("report");
    Stage proxyStage = new Stage(Stage.ID_PROXY);
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(
        application, scanId, proxyStage,
        ScanTriggerType.REPOSITORY_MANAGER, ClientScanType.SONATYPE, false);

    // Verify violations are not marked as legacy for proxy stage
    assertThat(results.allViolations).isNotEmpty();
    for (PolicyViolation violation : results.allViolations) {
      assertThat(violation.getLegacyViolationTime()).isNull();
      assertThat(violation.getStageTypeId()).isEqualTo(Stage.ID_PROXY);
    }
    assertThat(results.activeViolations).hasSize(results.allViolations.size());
  }

  @Test
  public void testEvaluate_ProxyStage_ViolationsDoNotInheritLegacyStatusOnSubsequentEvaluation() throws Exception {
    Repository repository = createTestRepository();
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    Policy policy = newSecurityPolicy();
    policy.setLegacyViolationAllowed(true);
    policy.getActions().put(Stage.ID_PROXY, Action.ID_FAIL);
    policy.setOwnerId(repository.getId());
    policyDAO.update(policy);

    String scanId = simulateReportIsAvailable("report");
    Stage proxyStage = new Stage(Stage.ID_PROXY);

    // First evaluation creates violations
    ScanPolicyEvaluatorResults firstResults = scanPolicyEvaluator.evaluate(
        application, scanId, proxyStage,
        ScanTriggerType.REPOSITORY_MANAGER, ClientScanType.SONATYPE, false);

    assertThat(firstResults.allViolations).isNotEmpty();

    // Manually mark violations as legacy to simulate pre-fix data
    Date legacyTime = new Date();
    try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      tx.begin();
      policyViolationDAO.getByOwnerId(application.getId()).forEach(policyViolation -> {
        policyViolationDAO.loadConstraintFacts(Collections.singletonList(policyViolation));
        policyViolation.setLegacyViolationTime(legacyTime);
        policyViolationDAO.update(tx, policyViolation);
      });
      tx.commit();
    }

    waitForTimeAdvance();

    // Second evaluation should not inherit legacy status for proxy stage
    ScanPolicyEvaluatorResults secondResults = scanPolicyEvaluator.evaluate(
        application, scanId, proxyStage,
        ScanTriggerType.REPOSITORY_MANAGER, ClientScanType.SONATYPE, false);

    // Verify violations are not marked as legacy
    assertThat(secondResults.allViolations).isNotEmpty();
    for (PolicyViolation violation : secondResults.allViolations) {
      assertThat(violation.getLegacyViolationTime()).isNull();
      assertThat(violation.getStageTypeId()).isEqualTo(Stage.ID_PROXY);
    }
    assertThat(secondResults.activeViolations).hasSize(secondResults.allViolations.size());
  }

  @Test
  public void testEvaluate_BuildStage_ViolationsStillMarkedAsLegacyOnFirstEvaluation() throws Exception {
    application = tempEntity.newApplicationWithParent();
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    Policy policy = newSecurityPolicy();
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);

    String scanId = simulateReportIsAvailable("report");
    Stage buildStage = new Stage(Stage.ID_BUILD);
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(
        application, scanId, buildStage,
        ScanTriggerType.CLI, ClientScanType.SONATYPE, false);

    // Verify Lifecycle stages still get legacy marking
    List<PolicyViolation> inactiveViolations = getInactiveViolations(results);
    assertThat(inactiveViolations).isNotEmpty().allSatisfy(violation -> {
      assertThat(violation.getLegacyViolationTime()).isEqualTo(results.evaluation.getTime());
    });
    assertThat(results.activeViolations).isEmpty();
  }

  @Test
  public void testEvaluate_failsOnZeroComponents_whenFeatureEnabled() {
    // Enable fail on zero components
    scanHealthService.saveConfiguration(
        APPLICATION,
        application.getId(),
        new ScanHealthConfigDTO(true));

    Policy policy = tempEntity.newPolicy(application);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policyDAO.update(policy);

    // Create a scan file with zero components
    String scanId = simulateReportIsAvailable("zero_components_report");

    // Evaluation should throw BadRequestException for zero components
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> scanPolicyEvaluator.evaluate(
            application, scanId, new Stage(Stage.ID_BUILD),
            ScanTriggerType.CLI, ClientScanType.SONATYPE, false))
        .withMessage(ScanHealthService.SCAN_FAILED_ZERO_COMPONENTS_DETECTED_MESSAGE);
  }

  @Test
  public void testEvaluate_succeedsWithZeroComponents_whenFeatureDisabled() throws Exception {
    // Do NOT enable fail on zero components (default is disabled)
    Policy policy = tempEntity.newPolicy(application);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policyDAO.update(policy);

    // Create a scan file with zero components
    String scanId = simulateReportIsAvailable("zero_components_report");

    // Evaluation should succeed (feature disabled)
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(
        application, scanId, new Stage(Stage.ID_BUILD),
        ScanTriggerType.CLI, ClientScanType.SONATYPE, false);

    assertThat(results).isNotNull();
    assertThat(results.allViolations).isEmpty();
  }

  @Test
  public void testEvaluate_succeedsWithComponents_whenFeatureEnabled() throws Exception {
    // Enable fail on zero components
    scanHealthService.saveConfiguration(
        APPLICATION,
        application.getId(),
        new ScanHealthConfigDTO(true));

    Policy policy = newSecurityPolicy();
    policyDAO.update(policy);

    // Create a scan file with components
    String scanId = simulateReportIsAvailable("report");

    // Evaluation should succeed (has components)
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(
        application, scanId, new Stage(Stage.ID_BUILD),
        ScanTriggerType.CLI, ClientScanType.SONATYPE, false);

    assertThat(results).isNotNull();
    assertThat(results.allViolations).isNotEmpty();
  }

  @Test
  public void testEvaluate_failsOnZeroComponents_whenOrgConfigEnabled() {
    // Enable fail on zero components at ORGANIZATION level
    scanHealthService.saveConfiguration(
        ORGANIZATION,
        application.getOrganizationId(),
        new ScanHealthConfigDTO(true));

    Policy policy = tempEntity.newPolicy(application);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policyDAO.update(policy);

    // Create a scan file with zero components
    String scanId = simulateReportIsAvailable("zero_components_report");

    // Evaluation should throw BadRequestException - org config inherits to app
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> scanPolicyEvaluator.evaluate(
            application, scanId, new Stage(Stage.ID_BUILD),
            ScanTriggerType.CLI, ClientScanType.SONATYPE, false))
        .withMessage(ScanHealthService.SCAN_FAILED_ZERO_COMPONENTS_DETECTED_MESSAGE);
  }

  @Test
  public void testEvaluate_succeedsWithOnlyUnknownComponents_whenFeatureEnabled() throws Exception {
    // Enable fail on zero components
    scanHealthService.saveConfiguration(
        APPLICATION,
        application.getId(),
        new ScanHealthConfigDTO(true));

    Policy policy = tempEntity.newPolicy(application);
    policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
    policyDAO.update(policy);

    // Create a scan file with ONLY unknown/unrecognized components (AC4: should NOT fail)
    String scanId = simulateReportIsAvailable("unknown_components_report");

    // Evaluation should succeed - unknown components are still components (AC4)
    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(
        application, scanId, new Stage(Stage.ID_BUILD),
        ScanTriggerType.CLI, ClientScanType.SONATYPE, false);

    assertThat(results).isNotNull();
    assertThat(results.allViolations).isEmpty();
  }

  @Disabled("On-demand performance benchmark for DB batching optimizations")
  @Test
  public void testPerformPolicyEvaluation_BatchingPerformance() throws Exception {
    int numPolicies = 20;
    int warmupIterations = 3;
    int measuredIterations = 10;

    Stage stage = new Stage(Stage.ID_BUILD);

    // Create many policies so each evaluation produces many violations
    for (int i = 0; i < numPolicies; i++) {
      newSecurityPolicy();
    }

    // Warmup: let JIT and caches stabilize
    for (int i = 0; i < warmupIterations; i++) {
      String scanId = simulateReportIsAvailable("report");
      scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
          ClientScanType.SONATYPE, false);
    }

    // Measure: first evaluations (inserts path)
    long[] firstEvalTimes = new long[measuredIterations];
    for (int i = 0; i < measuredIterations; i++) {
      String scanId = simulateReportIsAvailable("report");
      long start = System.nanoTime();
      scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
          ClientScanType.SONATYPE, false);
      firstEvalTimes[i] = System.nanoTime() - start;
    }

    // Measure: re-evaluations (updates path — same scanId re-evaluated)
    long[] reEvalTimes = new long[measuredIterations];
    for (int i = 0; i < measuredIterations; i++) {
      String scanId = simulateReportIsAvailable("report");
      // First eval to establish the scan
      scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
          ClientScanType.SONATYPE, false);
      // Re-evaluation of same scan
      long start = System.nanoTime();
      scanPolicyEvaluator.evaluate(application, scanId, stage, ScanTriggerType.CLI,
          ClientScanType.SONATYPE, false);
      reEvalTimes[i] = System.nanoTime() - start;
    }

    Arrays.sort(firstEvalTimes);
    Arrays.sort(reEvalTimes);

    log.info("=== Policy Evaluation Batching Performance ===");
    log.info("Policies: {}, Components in report: 28", numPolicies);
    log.info("First evaluation (insert path):");
    log.info("  min: {} ms", firstEvalTimes[0] / 1_000_000);
    log.info("  p50: {} ms", firstEvalTimes[measuredIterations / 2 - 1] / 1_000_000);
    log.info("  p90: {} ms", firstEvalTimes[(int) Math.ceil(measuredIterations * 0.9) - 1] / 1_000_000);
    log.info("  max: {} ms", firstEvalTimes[measuredIterations - 1] / 1_000_000);
    log.info("  avg: {} ms", Arrays.stream(firstEvalTimes).sum() / measuredIterations / 1_000_000);
    log.info("Re-evaluation (update path):");
    log.info("  min: {} ms", reEvalTimes[0] / 1_000_000);
    log.info("  p50: {} ms", reEvalTimes[measuredIterations / 2 - 1] / 1_000_000);
    log.info("  p90: {} ms", reEvalTimes[(int) Math.ceil(measuredIterations * 0.9) - 1] / 1_000_000);
    log.info("  max: {} ms", reEvalTimes[measuredIterations - 1] / 1_000_000);
    log.info("  avg: {} ms", Arrays.stream(reEvalTimes).sum() / measuredIterations / 1_000_000);
  }
}
