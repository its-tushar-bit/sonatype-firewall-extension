/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.mail.Message;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssSeverityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupVulnerabilityDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.hds.FirewallAuditHdsClient;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternUpdater;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.ComponentDataSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityCategory;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.DataSourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryNameConflictConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCustomCVSSVectorStringConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCustomRemediationConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCweConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.VulnerabilityGroupConditionType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssSeverity;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssVector;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCwe;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomRemediation;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroup;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroupVulnerability;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDigester;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.ReleaseQuarantineType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.ReleaseReason;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import com.sonatype.insight.brain.test.MailboxTestUtil;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.hamcrest.MockitoHamcrest;

import static com.sonatype.insight.brain.Assert.assertNotifications;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.extractProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class RepositoryPolicyEvaluatorTest
    extends AbstractComponentTest
{
  @Inject
  private RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  @Inject
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Inject
  private RepositoryComponentDAO repositoryComponentDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private VulnerabilityCustomCweDAO vulnerabilityCustomCweDAO;

  @Inject
  private VulnerabilityCustomRemediationDAO vulnerabilityCustomRemediationDAO;

  @Inject
  private VulnerabilityCustomCvssVectorDAO vulnerabilityCustomCvssVectorDAO;

  @Inject
  private VulnerabilityCustomCvssSeverityDAO vulnerabilityCustomCvssSeverityDAO;

  @Inject
  private VulnerabilityGroupDAO vulnerabilityGroupDAO;

  @Inject
  private VulnerabilityGroupVulnerabilityDAO vulnerabilityGroupVulnerabilityDAO;

  @Inject
  private MailConfigurationDAO mailConfigurationDAO;

  @Inject
  private RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  private AsyncEventBus mockEventBus;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private FirewallAuditHdsClient auditHdsClient;

  @Mock
  private FirewallQuarantineHdsClient quarantineHdsClient;

  @Mock
  private HdsClient mockHdsClient;

  @Mock
  private RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator;

  @Mock
  private CurrentUser currentUser;

  @Rule
  public LogOutput policyViolationLoggerOutput = new LogOutput(
      AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  private PolicyViolationLogDTOAssert policyViolationLogDTOAssert;

  @Override
  public void configure(Binder binder) {
    binder.bind(FirewallAuditHdsClient.class).toInstance(auditHdsClient);
    binder.bind(FirewallQuarantineHdsClient.class).toInstance(quarantineHdsClient);
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    binder.bind(RepositoryComponentTelemetryCreator.class).toInstance(repositoryComponentTelemetryCreator);
    super.configure(binder);
  }

  @Before
  public void before() {
    policyViolationLogDTOAssert = new PolicyViolationLogDTOAssert(repositoryManagerDAO);

    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat = new HashMap<>();
    lenient().when(mockHdsClient.get(eq(FirewallIgnorePatterns.class),
        eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH))).thenReturn(firewallIgnorePatterns);
    setBaseUrl("http://localhost");

    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("127.0.0.1");
    mailConfiguration.setPort(587);
    mailConfiguration.setSystemEmail("NexusIQServer@localhost");
    mailConfigurationDAO.set(mailConfiguration);
  }

  private void mockHdsRequest(
      RepositoryComponentEvaluationDataRequestList serviceRequest,
      ComponentEvaluationDataList hdsResult,
      boolean quarantine)
  {
    RepositoryComponentEvaluationDataRequestList hdsRequest = new RepositoryComponentEvaluationDataRequestList();
    hdsRequest.cause = serviceRequest.cause;
    for (RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest : serviceRequest.components) {
      String hash = HashHelper.truncateHash(componentEvaluationDataRequest.hash);
      String pathname = componentEvaluationDataRequest.pathname
          .substring(componentEvaluationDataRequest.pathname.startsWith("/") ? 1 : 0);
      hdsRequest.components
          .add(new RepositoryComponentEvaluationDataRequest(componentEvaluationDataRequest.format, pathname, hash));
    }
    when((quarantine ? quarantineHdsClient : auditHdsClient).post(any(), eq(ComponentEvaluationDataList.class),
        eq(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH), isNull(), eq(hdsRequest))).thenReturn(hdsResult);
  }

  private ComponentEvaluationData createComponentEvaluationData(
      ComponentIdentifier componentIdentifier,
      String hash,
      MatchState matchState,
      int index,
      Set<License> declaredLicenses,
      Set<License> observedLicenses,
      List<SecurityVulnerability> securityVulnerabilities,
      Integer relativePopularity)
  {
    AnalyzerFeatures analyzerFeatures = new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.HASH, "client");
    return createComponentEvaluationData(componentIdentifier, hash, matchState, index, declaredLicenses,
        observedLicenses, securityVulnerabilities, relativePopularity, analyzerFeatures);
  }

  private ComponentEvaluationData createComponentEvaluationData(
      ComponentIdentifier componentIdentifier,
      String hash,
      MatchState matchState,
      int index,
      Set<License> declaredLicenses,
      Set<License> observedLicenses,
      List<SecurityVulnerability> securityVulnerabilities,
      Integer relativePopularity,
      AnalyzerFeatures analyzerFeatures)
  {
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.requestIndex = index;
    componentEvaluationData.hash = hash;
    componentEvaluationData.componentIdentifier = componentIdentifier;
    componentEvaluationData.matchState = matchState.getId();
    componentEvaluationData.declaredLicenses = declaredLicenses == null ? Collections.emptySet() : declaredLicenses;
    componentEvaluationData.observedLicenses = observedLicenses == null ? Collections.emptySet() : observedLicenses;
    componentEvaluationData.catalogDate = System.currentTimeMillis();
    componentEvaluationData.securityVulnerabilities = securityVulnerabilities;
    componentEvaluationData.relativePopularity = relativePopularity;
    componentEvaluationData.analyzerFeatures = analyzerFeatures;
    return componentEvaluationData;
  }

  private List<SecurityVulnerability> createSecurityVulnerabilities() {
    return Collections.singletonList(new SecurityVulnerability("cve-2019-1234", "sonatype", 5.0f, ""));
  }

  private List<SecurityVulnerability> createUniqueSecurityVulnerabilities() {
    List<SecurityVulnerability> list = new ArrayList<>();
    list.add(new SecurityVulnerability("cve-2019-1234", "sonatype", 5.0f, ""));
    list.add(new SecurityVulnerability("cve-2019-5678", "sonatype", 5.0f, ""));
    return list;
  }

  private void assertPolicyViolationsLogged(
      PolicyViolationLogEvent policyViolationLogEvent,
      Repository repository,
      Date before,
      Date after,
      List<RepositoryPolicyViolation> policyViolations)
      throws Exception
  {
    repositoryPolicyViolationDAO.loadConstraintFacts(policyViolations);
    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(policyViolationLoggerOutput, policyViolationLogEvent, policyViolations.size());
    policyViolationLogDTOAssert.assertRepositoryPolicyViolationData(policyViolationLogDTOs, policyViolationLogEvent,
        repository, before, after, policyViolations, currentUser.getUsernameOrSystem());
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_CreatePolicyViolations() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Repository repository = tempEntity.newRepository();

    tempEntity.newPolicy(repository.getId());

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    for (int i = 0; i < 2; i++) {
      componentEvaluationDataRequestList.components
          .add(new RepositoryComponentEvaluationDataRequest("maven2", "path" + i, "h" + i));
      hdsResult.components.add(createComponentEvaluationData(
          ComponentIdentifier.createMavenCoordinates("g" + i, "a" + i, "v" + i, "c" + i, "e" + i), "h" + i,
          MatchState.EXACT, i /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
          createSecurityVulnerabilities(), i /* popularity */));
    }
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Evaluate policies. All policy violations should be logged.
    Date before1 = new Date();
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);
    final Date after1 = new Date();
    List<RepositoryPolicyViolation> policyViolations =
        repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(2);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, repository, before1, after1, policyViolations);
    policyViolationLoggerOutput.clear();

    // Add a new policy and evaluate again. Only the new policy violations should be logged.
    Awaitility.await().until(() -> System.currentTimeMillis() > after1.getTime());
    Policy newPolicy = tempEntity.newPolicy(repository.getId());
    Date before2 = new Date();
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);
    final Date after2 = new Date();
    policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(4);
    List<RepositoryPolicyViolation> newPolicyViolations =
        policyViolations.stream().filter(policyViolation -> policyViolation.getPolicyId().equals(newPolicy.getId()))
            .collect(Collectors.toList());
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, repository, before2, after2, newPolicyViolations);
    assertRepositoryComponent(repository, 2);

    verify(repositoryComponentTelemetryCreator, times(4))
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(repository.getPublicId()), eq(RepositoryComponentTelemetryEventType.AUDIT),
            eq(Collections.emptyList()), any());
    verifyNoMoreInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testEvaluate_postCreateRepositoryPolicyViolationEvent() throws Exception {
    TestEventHandler<CreateRepositoryPolicyViolationsEvent> handler =
        new TestEventHandler<>(new CountDownLatch(1), CreateRepositoryPolicyViolationsEvent.class);
    mockEventBus.register(handler);

    Repository repository = tempEntity.newRepository();

    tempEntity.newPolicy(repository.getId());

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();

    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven", "path0", "h0"));
    hdsResult.components.add(createComponentEvaluationData(
        ComponentIdentifier.createMavenCoordinates("g0", "a0", "v0", "c0", "e0"), "h0",
        MatchState.EXACT, 0 /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
        createUniqueSecurityVulnerabilities(), 0 /* popularity */));

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);

    try {
      assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
      CreateRepositoryPolicyViolationsEvent event = handler.getEvent();
      assertThat(event.repositoryPolicyViolations).hasSize(2);
      assertThat(event.repositoryPolicyViolations)
          .extracting(RepositoryPolicyViolation::getHash)
          .containsOnly("h0");
      // Check component has given security vulnerabilities
      assertThat(event.repositoryPolicyViolations)
          .flatExtracting(RepositoryPolicyViolation::getConstraintFacts)
          .flatExtracting(ConstraintFact::getConditionFacts)
          .extracting(ConditionFact::getReference)
          .extracting(TriggerReference::getValue)
          .containsExactlyInAnyOrder("cve-2019-1234","cve-2019-5678");
    }
    finally {
      mockEventBus.unregister(handler);
    }
  }

  @Test
  public void testEvaluate_doNotPostCreateRepositoryPolicyViolationEventWhenLicenseFails() throws Exception {
    // Intentionally fail license check
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    TestEventHandler<CreateRepositoryPolicyViolationsEvent> handler =
        new TestEventHandler<>(new CountDownLatch(1), CreateRepositoryPolicyViolationsEvent.class);
    mockEventBus.register(handler);

    Repository repository = tempEntity.newRepository();

    tempEntity.newPolicy(repository.getId());

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();

    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven", "path0", "h0"));
    hdsResult.components.add(createComponentEvaluationData(
        ComponentIdentifier.createMavenCoordinates("g0", "a0", "v0", "c0", "e0"), "h0",
        MatchState.EXACT, 0 /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
        createUniqueSecurityVulnerabilities(), 0 /* popularity */));

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);

    try {
      assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isFalse();
    }
    finally {
      mockEventBus.unregister(handler);
    }
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_FixPolicyViolations() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Repository repository = tempEntity.newRepository();

    Policy policy = tempEntity.newPolicy(repository.getId());

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    for (int i = 0; i < 2; i++) {
      componentEvaluationDataRequestList.components
          .add(new RepositoryComponentEvaluationDataRequest("maven2", "path" + i, "h" + i));
      hdsResult.components.add(createComponentEvaluationData(
          ComponentIdentifier.createMavenCoordinates("g" + i, "a" + i, "v" + i, "c" + i, "e" + i), "h" + i,
          MatchState.EXACT, i /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
          createSecurityVulnerabilities(), i /* popularity */));
    }
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Evaluate policies.
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);
    final Date after1 = new Date();
    List<RepositoryPolicyViolation> policyViolations =
        repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(2);

    policyViolationLoggerOutput.clear();

    // Delete the policy and evaluate again. All policy violations should be logged as fixed.
    Awaitility.await().until(() -> System.currentTimeMillis() > after1.getTime());
    policyDAO.delete(policy);
    Date before2 = new Date();
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);
    Date after2 = new Date();
    assertThat(repositoryPolicyViolationDAO.getByRepositoryId(repository.getId())).hasSize(0);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.FIX, repository, before2, after2, policyViolations);
    assertRepositoryComponent(repository, 2);

    verify(repositoryComponentTelemetryCreator, times(4))
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(repository.getPublicId()), eq(RepositoryComponentTelemetryEventType.AUDIT),
            eq(Collections.emptyList()), any());
    verifyNoMoreInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_WaiveAndUnwaivePolicyViolations() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Repository repository = tempEntity.newRepository();

    Policy policy1 = tempEntity.newPolicy(repository.getId());
    Policy policy2 = tempEntity.newPolicy(repository.getId());
    PolicyWaiver policy2Waiver = tempEntity.newWaiver(policy2.getId(), repository.getId());

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    for (int i = 0; i < 1; i++) {
      componentEvaluationDataRequestList.components
          .add(new RepositoryComponentEvaluationDataRequest("maven2", "path" + i, "h" + i));
      hdsResult.components.add(createComponentEvaluationData(
          ComponentIdentifier.createMavenCoordinates("g" + i, "a" + i, "v" + i, "c" + i, "e" + i), "h" + i,
          MatchState.EXACT, i /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
          createSecurityVulnerabilities(), i /* popularity */));
    }
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // perform initial evaluation
    Date before1 = new Date();
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);
    final Date after1 = new Date();
    // ... yielding two active violations, both of which logged as new
    List<RepositoryPolicyViolation> activeViolations =
        repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(activeViolations).hasSize(2);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, repository, before1, after1, activeViolations);
    // ... and one logged as waived
    List<RepositoryPolicyViolation> waivedViolations = activeViolations.stream()
        .filter(RepositoryPolicyViolation::isWaived).collect(toList());
    assertThat(waivedViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.WAIVE, repository, before1, after1, waivedViolations);

    policyViolationLoggerOutput.clear();

    // remove the original waiver, add a waiver for the other policy and re-evaluate
    policyWaiverDAO.delete(policy2Waiver);
    tempEntity.newWaiver(policy1.getId(), repository.getId());
    Date before2 = new Date();
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);
    final Date after2 = new Date();
    // ... yielding again two violations, none of which logged as new
    activeViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(activeViolations).hasSize(2);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, repository, before2, after2, Collections.emptyList());
    // ... but one logged as unwaived
    List<RepositoryPolicyViolation> unwaivedViolations = activeViolations.stream()
        .filter(violation -> policy2.getId().equals(violation.getPolicyId())).collect(toList());
    assertThat(unwaivedViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.UNWAIVE, repository, before2, after2, unwaivedViolations);
    // ... and one logged as freshly waived
    waivedViolations = activeViolations.stream()
        .filter(violation -> policy1.getId().equals(violation.getPolicyId())).collect(toList());
    assertThat(waivedViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.WAIVE, repository, before2, after2, waivedViolations);
    assertRepositoryComponent(repository, 1);
  }

  @Test
  public void testEvaluate_WaiverDetails() {
    Repository repository = tempEntity.newRepository();

    Policy policy1 = tempEntity.newPolicy(repository.getId());
    Policy policy2 = tempEntity.newPolicy(repository.getId());

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path", "h"));
    hdsResult.components.add(
        createComponentEvaluationData(ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), "h",
            MatchState.EXACT, 0 /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
            createSecurityVulnerabilities(), 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // waive the first policy
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver(policy1.getId(), repository.getId());

    // perform initial evaluation
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);

    // ... yielding two active violations
    List<RepositoryPolicyViolation> activeViolations =
        repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(activeViolations).hasSize(2);

    // ... and one as waived
    List<RepositoryPolicyViolation> waivedViolations =
        activeViolations.stream().filter(RepositoryPolicyViolation::isWaived).collect(toList());
    assertThat(waivedViolations).hasSize(1);
    RepositoryComponent repositoryComponent =
        repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), waivedViolations.get(0).getPathname());
    Date policy1ViolationWaiveTime = repositoryComponent.getTime();
    assertViolationWaiverDetails(waivedViolations.get(0), policyWaiver1, policy1ViolationWaiveTime);

    // waive the second policy
    PolicyWaiver policyWaiver2 = tempEntity.newWaiver(policy2.getId(), repository.getId());

    // re-evaluate
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);

    // ... yielding again two violations
    activeViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(activeViolations).hasSize(2);

    // ... and two ARE waived
    waivedViolations = activeViolations.stream().filter(RepositoryPolicyViolation::isWaived)
        .collect(toList());
    assertThat(waivedViolations).hasSize(2);

    RepositoryPolicyViolation waivedViolation1 = waivedViolations.stream()
        .filter(violation -> violation.getPolicyId().equals(policy1.getId())).findFirst().orElse(null);
    assertThat(waivedViolation1).isNotNull();

    RepositoryPolicyViolation waivedViolation2 = waivedViolations.stream()
        .filter(violation -> violation.getPolicyId().equals(policy2.getId())).findFirst().orElse(null);
    assertThat(waivedViolation2).isNotNull();

    // first waived violation should still use the original evaluation time
    assertViolationWaiverDetails(waivedViolation1, policyWaiver1, policy1ViolationWaiveTime);

    // second waived violation should use the most recent evaluation time
    repositoryComponent =
        repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), waivedViolation2.getPathname());
    Date policy2ViolationWaiveTime = repositoryComponent.getLastEvaluationTime();
    assertViolationWaiverDetails(waivedViolation2, policyWaiver2, policy2ViolationWaiveTime);

    // remove the original waiver re-evaluate
    policyWaiverDAO.delete(policyWaiver1);

    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);

    activeViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(activeViolations).hasSize(2);

    // first violation is no longer waived
    List<RepositoryPolicyViolation> unwaivedViolations =
        activeViolations.stream().filter(violation -> policy1.getId().equals(violation.getPolicyId()))
            .collect(toList());
    assertThat(unwaivedViolations).hasSize(1);
    assertThat(unwaivedViolations.get(0).getPolicyWaiverId()).isNull();
    assertThat(unwaivedViolations.get(0).getPolicyWaiverComment()).isNull();
    assertThat(unwaivedViolations.get(0).getWaiveTime()).isNull();

    // ... second violation is still waived... waive time should be preserved
    waivedViolations = activeViolations.stream().filter(violation -> policy2.getId().equals(violation.getPolicyId()))
        .collect(toList());
    assertThat(waivedViolations).hasSize(1);
    assertViolationWaiverDetails(waivedViolations.get(0), policyWaiver2, policy2ViolationWaiveTime);
  }

  @Test
  public void testEvaluate_WaiverDetails_MigrateExistingRecordMissingWaiveTime() {
    Repository repository = tempEntity.newRepository();

    Policy policy = new Policy(null, "test");
    policy.setOwnerId(repository.getId());
    Constraint constraint = new Constraint(null, "constraint", LogicalOperator.AND);
    com.sonatype.insight.brain.model.policy.Condition condition =
        new com.sonatype.insight.brain.model.policy.Condition(MatchStateConditionType.ID, "is",
            MatchState.EXACT.toString());
    constraint.setConditions(Collections.singletonList(condition));
    policy.setConstraints(Collections.singletonList(constraint));
    tempEntity.newPolicy(policy);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    // simulate an older record that does not have the policy waiver details, specifically no waive time
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "path", "hash", componentIdentifier, new Date(System.currentTimeMillis() - 1000), null);
    ConstraintFact constraintFact =
        new ConstraintFact(constraint.getId(), constraint.getName(), constraint.getOperator().name());

    Component c = new Component(repositoryComponent.getComponentIdentifier());
    constraintFact.addConditionFact(ComponentPolicyEvaluator
        .createConditionFact(condition, new MatchFact(c,
            policy.getId(), constraint.getId(), Collections.emptyList() /* conditionTriggers */)));

    RepositoryPolicyViolation existingPolicyViolation = tempEntity
        .newRepositoryPolicyViolation(repositoryComponent.getRepositoryId(), policy.getThreatLevel(),
            repositoryComponent.getPathname(), "hash", Collections.singletonList(constraintFact), true, null,
            policy.getId(), policy.getName(), repositoryComponent.getComponentIdentifier(),
            repositoryComponent.getTime(), null, null, null);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path", "hash"));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "hash", MatchState.EXACT, 0 /* index */,
        null /* declaredLicenseSet */, null /* observedLicenseSet */, createSecurityVulnerabilities(),
        0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // waive the policy
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver(policy.getId(), repository.getId());

    // perform the evaluation
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);

    // ... yielding one active/waived violation
    List<RepositoryPolicyViolation> policyViolations =
        repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    repositoryPolicyViolationDAO.loadConstraintFacts(policyViolations);

    assertThat(policyViolations).hasSize(1);

    RepositoryPolicyViolation policyViolation = policyViolations.get(0);
    assertThat(policyViolation.isWaived()).isTrue();

    // sanity check to ensure we are dealing with the same violation
    PolicyViolationDiff<RepositoryPolicyViolation> policyViolationDiff = PolicyViolationDigester
        .digestPolicyViolations(Collections.singletonList(existingPolicyViolation), policyViolations);
    assertThat(policyViolationDiff.getSame()).hasSize(1);

    repositoryComponent =
        repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), policyViolation.getPathname());

    // Another sanity check ensuring the original evaluation time is different than the last evaluation time
    assertThat(repositoryComponent.getTime()).isNotEqualTo(repositoryComponent.getLastEvaluationTime());

    // For older violations (violations that existed before adding waive time) we are ok to use the last evaluation time
    assertViolationWaiverDetails(policyViolation, policyWaiver1, repositoryComponent.getLastEvaluationTime());
  }

  @Test
  public void testEvaluate_IgnorableRepositoryComponent_DoesNotEvaluateOrPersist() {
    Repository repository = tempEntity.newRepository(tempEntity.newRepositoryManager().getId(), "my_repo", "maven2");
    RepositoryComponent ignorableComponent = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "some/path/sha", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"), false);
    tempEntity.newPolicy(repository.getId(), "some_policy", 9, Action.ID_FAIL, Stage.ID_PROXY, null);
    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat = new HashMap<>();
    firewallIgnorePatterns.regexpsByRepositoryFormat.put(repository.getFormat(), Collections.singletonList(".*sha"));
    firewallIgnorePatterns.regexpsByRepositoryFormat
        .put(repository.getFormat() + "other", Collections.singletonList(".*"));
    when(mockHdsClient.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(firewallIgnorePatterns);

    RepositoryComponentEvaluationDataRequestList requestList =
        new RepositoryComponentEvaluationDataRequestList();
    RepositoryComponentEvaluationDataRequest ignorableRequest = new RepositoryComponentEvaluationDataRequest(
        repository.getFormat(), ignorableComponent.getPathname(), ignorableComponent.getHash());
    RepositoryComponentEvaluationDataRequest unignorableRequest =
        new RepositoryComponentEvaluationDataRequest(repository.getFormat(), "some/path/other", "hash2");
    requestList.components.add(ignorableRequest);
    requestList.components.add(unignorableRequest);

    Date now = new Date();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components.add(createComponentEvaluationData(
        ignorableComponent.getComponentIdentifier(), ignorableComponent.getHash(),
        MatchState.UNKNOWN, 0 /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
        null, 0 /* popularity */));
    hdsResult.components.add(createComponentEvaluationData(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"), "hash2",
        MatchState.UNKNOWN, 1 /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
        createSecurityVulnerabilities(), 1 /* popularity */));

    mockHdsRequest(requestList, hdsResult, true);

    RepositoryComponentEvaluationDataList resultList = repositoryPolicyEvaluator.evaluate(repository, requestList,
        true /* withQuarantine */, null /* clientUserAgent */);

    assertThat(resultList.componentEvalResults).hasSize(2);
    // Ignored component is not evaluated and cannot have security vulnerabilities and so should not be quarantined
    assertThat(resultList.componentEvalResults.get(0).requestIndex).isEqualTo(0);
    assertThat(resultList.componentEvalResults.get(0).quarantine).isFalse();
    // Catalog date is not available from an ignored component
    assertThat(resultList.componentEvalResults.get(0).catalogDate).isNull();
    // Unignored component is evaluated and has a security vulnerability and so should be quarantined
    assertThat(resultList.componentEvalResults.get(1).requestIndex).isEqualTo(1);
    assertThat(resultList.componentEvalResults.get(1).quarantine).isTrue();
    assertThat(resultList.componentEvalResults.get(1).catalogDate).isAfterOrEqualTo(now);

    assertThat(repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), ignorableRequest.pathname))
        .isNull();
    assertThat(repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), unignorableRequest.pathname))
        .isNotNull();
    assertThat(repositoryPolicyViolationDAO.getByRepositoryId(repository.getId()))
        .extracting(RepositoryPolicyViolation::getPathname).containsExactly(unignorableRequest.pathname);

    verify(repositoryComponentTelemetryCreator, times(1))
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(repository.getPublicId()), eq(RepositoryComponentTelemetryEventType.QUARANTINE),
            eq(Collections.emptyList()), any());
    verifyNoMoreInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testEvaluate_PolicyViolationLogger_metadata() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Repository repository = tempEntity.newRepository();
    createPolicyDataSourceFeature(repository);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path1", "h1"));
    hdsResult.components.add(createdComponentMetadata());
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Evaluate policies. All policy violations should be logged.
    Date before1 = new Date();
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);
    final Date after1 = new Date();
    List<RepositoryPolicyViolation> policyViolations =
        repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);
    assertPolicyViolationsLogged(PolicyViolationLogEvent.CREATE, repository, before1, after1, policyViolations);
    policyViolationLoggerOutput.clear();
  }

  private void createPolicyDataSourceFeature(Repository repository) {
    tempEntity.newPolicy(repository.getId(), 5, LogicalOperator.AND, new Condition(
        DataSourceConditionType.ID, DataSourceConditionType.HAS_SUPPORT_FOR, ComponentDataSource.IDENTITY.getId()));
  }

  private ComponentEvaluationData createdComponentMetadata() {
    return createComponentEvaluationData(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), "h1",
        MatchState.EXACT, 0 /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
        createSecurityVulnerabilities(), 1 /* popularity */, fromHds());
  }

  private AnalyzerFeatures fromHds() {
    return new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.COORDINATE, "CLI", true, true, true);
  }

  private void assertViolationWaiverDetails(
      RepositoryPolicyViolation repositoryPolicyViolation,
      PolicyWaiver policyWaiver,
      Date waiveTime)
  {
    assertThat(repositoryPolicyViolation.getPolicyWaiverId()).isEqualTo(policyWaiver.getId());
    assertThat(repositoryPolicyViolation.getPolicyWaiverComment()).isEqualTo(policyWaiver.getComment());
    assertThat(repositoryPolicyViolation.getWaiveTime()).isEqualTo(waiveTime);
  }

  private void assertRepositoryComponent(final Repository repository, int size) {
    List<RepositoryComponent> components = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(components).hasSize(size);
    AnalyzerFeatures analyzerFeatures = new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.HASH, "client");
    assertThat(extractProperty("repositoryId").from(components)).containsOnly(repository.getId());
    assertThat(extractProperty("identificationSourceId").from(components)).containsOnly("Sonatype");
    assertThat(extractProperty("matchStateId").from(components)).containsOnly("exact");
    assertThat(extractProperty("analyzerFeaturesJson").from(components))
        .containsOnly(JsonUtils.format(analyzerFeatures));
    if (size == 2) {
      assertThat(extractProperty("pathname").from(components)).containsOnly("path0", "path1");
      assertThat(extractProperty("hash").from(components)).containsOnly("h0", "h1");
    }
    else {
      assertThat(extractProperty("pathname").from(components)).containsOnly("path0");
      assertThat(extractProperty("hash").from(components)).containsOnly("h0");
    }
  }

  @Test
  public void testEvaluate_SupportsProprietaryNameConflictCondition() {
    RepositoryManager repoMan = tempEntity.newRepositoryManager();
    Repository repoHosted =
        tempEntity.newRepository(repoMan, "hosted-repo", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    tempEntity.newProprietaryComponentNamePattern(repoHosted, "@sonatype", null);
    Repository repo = tempEntity.newRepository(repoMan, "proxy-repo");

    Policy policy = new Policy(null, "Namespace Confusion");
    policy.setAction(Stage.ID_PROXY, Action.ID_FAIL);
    policy.setThreatLevel(10);
    policy.setOwnerId(repo.getId());
    Constraint constraint = new Constraint(null, "No Conflicting Name", LogicalOperator.OR);
    constraint.addCondition(
        new Condition(ProprietaryNameConflictConditionType.ID, ProprietaryNameConflictConditionType.OP_IS_PRESENT));
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    Date now = new Date();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    for (int i = 0; i < 2; i++) {
      componentEvaluationDataRequestList.components
          .add(new RepositoryComponentEvaluationDataRequest("npm", i == 0 ? "@sonatype/cli" : "cli-" + i, "h" + i));
      hdsResult.components.add(createComponentEvaluationData(
          ComponentIdentifier.createNpmCoordinates(i == 0 ? "@sonatype/cli" : "cli-" + i, "999"), "h" + i,
          MatchState.EXACT, i, null, null, null, null));
    }
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    RepositoryComponentEvaluationDataList resultList =
        repositoryPolicyEvaluator.evaluate(repo, componentEvaluationDataRequestList, true /* withQuarantine */,
            null /* clientUserAgent */);

    assertThat(resultList.componentEvalResults).hasSize(2);
    assertThat(resultList.componentEvalResults.get(0).quarantine).isTrue();
    assertThat(resultList.componentEvalResults.get(0).catalogDate).isAfterOrEqualTo(now);
    assertThat(resultList.componentEvalResults.get(1).quarantine).isFalse();
    assertThat(resultList.componentEvalResults.get(1).catalogDate).isAfterOrEqualTo(now);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repo.getId());
    assertThat(policyViolations).hasSize(1);
    assertThat(policyViolations.get(0).getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyViolations.get(0).getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createNpmCoordinates("@sonatype/cli", "999"));
  }

  @Test
  public void testEvaluate_Telemetry_SendNotificationsForNewComponent() {
    Repository repository = tempEntity.newRepository();

    Policy policy = tempEntity.newPolicy(repository.getId());
    policy.setAction("proxy", "fail");
    policyDAO.update(policy);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT;

    // Prepare request and mock the HDS request
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    for (int i = 0; i < 2; i++) {
      componentEvaluationDataRequestList.components
          .add(new RepositoryComponentEvaluationDataRequest("maven2", "path" + i, "h" + i));
      hdsResult.components.add(createComponentEvaluationData(
          ComponentIdentifier.createMavenCoordinates("g" + i, "a" + i, "v" + i, "c" + i, "e" + i), "h" + i,
          MatchState.EXACT, i /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
          createSecurityVulnerabilities(), i /* popularity */));
    }
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, true /* withQuarantine */,
        null /* clientUserAgent */);

    verify(repositoryComponentTelemetryCreator, times(2))
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(repository.getPublicId()), eq(RepositoryComponentTelemetryEventType.QUARANTINE),
            (List) MockitoHamcrest.argThat(hasSize(2)), any());
    verifyNoMoreInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testEvaluate_Telemetry_DontSendNotificationsForExistingComponent() {
    Repository repository = tempEntity.newRepository();

    tempEntity.newPolicy(repository.getId());

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = RepositoryComponentEvaluationDataRequestList.ADHOC;

    // Prepare request and mock the HDS request
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    for (int i = 0; i < 2; i++) {
      componentEvaluationDataRequestList.components
          .add(new RepositoryComponentEvaluationDataRequest("maven2", "path" + i, "h" + i));
      hdsResult.components.add(createComponentEvaluationData(
          ComponentIdentifier.createMavenCoordinates("g" + i, "a" + i, "v" + i, "c" + i, "e" + i), "h" + i,
          MatchState.EXACT, i /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
          createSecurityVulnerabilities(), i /* popularity */));
    }
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);

    verify(repositoryComponentTelemetryCreator, times(2))
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(repository.getPublicId()), eq(RepositoryComponentTelemetryEventType.AUDIT),
            eq(Collections.emptyList()), any());
    verifyNoMoreInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testEvaluate_PyPI_ForPCCS() {
    int requestIndexForVersion1 = 0;
    int requestIndexForVersion2 = 1;

    Repository repository = tempEntity.newRepository();

    RepositoryComponent component1 =
        tempEntity.newRepositoryComponent(repository.getId(), "scipy-1.0.0.tar.gz", new Date());
    RepositoryComponent component2 =
        tempEntity.newRepositoryComponent(repository.getId(), "scipy-2.0.0.tar.gz", new Date());

    // State that a component has been un-quarantine
    component1.setQuarantineTime(new Date());
    component1.setUnquarantineTimeForManualRelease(new Date());
    repositoryComponentDAO.update(component1);
    component2.setQuarantineTime(new Date());
    repositoryComponentDAO.update(component2);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("pypi", "scipy-1.0.0.tar.gz", "hash"));
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("pypi", "scipy-2.0.0.tar.gz", "hash"));

    hdsResult.components
        .add(createComponentEvaluationData(
            ComponentIdentifier.createPypiCoordinates("scipy", "1.0.0", null, "tar.gz"),
            null, MatchState.EXACT, requestIndexForVersion1, null /* declaredLicenseSet */,
            null /* observedLicenseSet */, createSecurityVulnerabilities(), 2 /* popularity */));
    hdsResult.components
        .add(createComponentEvaluationData(
            ComponentIdentifier.createPypiCoordinates("scipy", "2.0.0", null, "tar.gz"),
            null, MatchState.EXACT, requestIndexForVersion2, null /* declaredLicenseSet */,
            null /* observedLicenseSet */, createSecurityVulnerabilities(), 2 /* popularity */));

    // Evaluate policies. The component that has been un-quarantined should have the quarantined flag set to false.
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResult =
        repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, hdsResult,
            true /* withQuarantine */, false /* persistEvaluationResults */, false /* forMonitoring */);
    assertThat(
        getQuarantineStatusOfRequestIndex(repositoryComponentEvaluationResult, requestIndexForVersion1)).isFalse();
    assertThat(
        getQuarantineStatusOfRequestIndex(repositoryComponentEvaluationResult, requestIndexForVersion2)).isTrue();
  }

  private boolean getQuarantineStatusOfRequestIndex(
      RepositoryComponentEvaluationDataList repositoryComponentEvaluationResult, int requestIndex)
  {
    return repositoryComponentEvaluationResult.componentEvalResults.stream()
        .filter(component -> component.requestIndex == requestIndex).findFirst().get().quarantine;
  }

  @Test
  public void testEvaluate_UnquarantinesComponent() {
    Repository repository = tempEntity.newRepository();

    Policy policy = tempEntity.newPolicy(repository.getId());
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path", "hash"));
    hdsResult.components
        .add(createComponentEvaluationData(ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e"), "hash",
            MatchState.EXACT, 0 /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
            createSecurityVulnerabilities(), 2 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true /* quarantine */);

    // Evaluate policies. The component should be quarantined.
    Date before1 = new Date();
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, true /* withQuarantine */,
        null /* clientUserAgent */);
    Date after1 = new Date();
    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    assertThat(repositoryComponents.get(0).isQuarantined()).isEqualTo(true);
    assertThat(repositoryComponents.get(0).getQuarantineTime()).isBetween(before1, after1, true, true);
    assertThat(repositoryComponents.get(0).getUnquarantineTime()).isNull();

    // Evaluate policies again. The component should still be quarantined.
    Awaitility.await().until(() -> System.currentTimeMillis() > after1.getTime());
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, true /* withQuarantine */,
        null /* clientUserAgent */);
    repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    assertThat(repositoryComponents.get(0).isQuarantined()).isEqualTo(true);
    assertThat(repositoryComponents.get(0).getQuarantineTime()).isBetween(before1, after1, true, true);
    assertThat(repositoryComponents.get(0).getUnquarantineTime()).isNull();

    // Remove policy and evaluate again. The component should still be unquarantined.
    policyDAO.delete(policy);
    Date before2 = new Date();
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, true /* withQuarantine */,
        null /* clientUserAgent */);
    Date after2 = new Date();

    repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    assertThat(repositoryComponents.get(0).isQuarantined()).isEqualTo(false);
    assertThat(repositoryComponents.get(0).getQuarantineTime()).isBetween(before1, after1, true, true);
    assertThat(repositoryComponents.get(0).getUnquarantineTime()).isBetween(before2, after2, true, true);
  }

  @Test
  public void testEvaluate_ExistingComponent() {
    Repository repository = tempEntity.newRepository();

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1",
        "v1", "c1", "e1");
    Date createTime = new Date();
    RepositoryComponent repositoryComponent = new RepositoryComponent(repository.getId(), "path1", createTime, "h1",
        componentIdentifier, MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), createTime);

    repositoryComponentDAO.insert(repositoryComponent);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT;

    // Prepare request and mock the HDS request
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("maven2", "path1", "h1");

    ComponentEvaluationData componentEvaluationData = createComponentEvaluationData(
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"), "h1",
        MatchState.EXACT, 0, null, null, createSecurityVulnerabilities(), 1);

    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(componentEvaluationData);

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, true, null);

    verify(repositoryComponentTelemetryCreator, times(1))
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(repository.getPublicId()), any(), (List) MockitoHamcrest.argThat(hasSize(0)), any());

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());

    assertThat(repositoryComponents).hasSize(1);
  }

  @Test
  public void testEvaluate_SecurityVulnerabilityCweConditionType() {
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    String componentHash = "testHash";
    String cweId = "500";
    Condition condition = new Condition(SecurityVulnerabilityCweConditionType.ID, "is", cweId);
    SecurityVulnerability securityVulnerability = new SecurityVulnerability("cve-2019-1234", "sonatype", 5.0f);
    securityVulnerability.setCwe(cweId);

    testEvaluate_SecurityCondition(repository, componentIdentifier, componentHash, condition, securityVulnerability);
  }

  @Test
  public void testEvaluate_SecurityVulnerabilityCweConditionType_CustomCwe() {
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    String componentHash = "testHash";
    String svCweId = "500";
    String customCweId = "600";
    Condition condition = new Condition(SecurityVulnerabilityCweConditionType.ID, "is", customCweId);
    SecurityVulnerability securityVulnerability = new SecurityVulnerability("cve-2019-1234", "sonatype", 5.0f);
    securityVulnerability.setCwe(svCweId);
    VulnerabilityCustomCwe vulnerabilityCustomCwe = new VulnerabilityCustomCwe();
    vulnerabilityCustomCwe.setOwnerId(repository.getId());
    vulnerabilityCustomCwe.setRefId(securityVulnerability.getRefId());
    vulnerabilityCustomCwe.setComponentIdentifier(componentIdentifier);
    vulnerabilityCustomCwe.setLastUpdatedAt(new Date());
    vulnerabilityCustomCwe.setLastUpdatedByUsername("testUser");
    vulnerabilityCustomCwe.setCwe(customCweId);
    vulnerabilityCustomCweDAO.insert(vulnerabilityCustomCwe);

    testEvaluate_SecurityCondition(repository, componentIdentifier, componentHash, condition, securityVulnerability);
  }

  @Test
  public void testEvaluate_SecurityVulnerabilityStatusConditionType() {
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    String componentHash = "testHash";
    Condition condition = new Condition(SecurityVulnerabilityStatusConditionType.ID, "is",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED.getId());
    SecurityVulnerability securityVulnerability = new SecurityVulnerability("cve-2019-1234", "sonatype", 5.0f);
    tempEntity.newSecurityVulnerabilityOverride(repository.getId(), componentHash, securityVulnerability.getSource(),
        securityVulnerability.getRefId(), SecurityVulnerabilityOverrideStatus.valueOf(condition.getValue()));

    testEvaluate_SecurityCondition(repository, componentIdentifier, componentHash, condition, securityVulnerability);
  }

  @Test
  public void testEvaluate_SecurityVulnerabilityCustomRemediationConditionType() {
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    String componentHash = "testHash";
    Condition condition = new Condition(SecurityVulnerabilityCustomRemediationConditionType.ID, "exists",
        "testRemediation");
    SecurityVulnerability securityVulnerability = new SecurityVulnerability("cve-2019-1234", "sonatype", 5.0f);
    VulnerabilityCustomRemediation vulnerabilityCustomRemediation = new VulnerabilityCustomRemediation();
    vulnerabilityCustomRemediation.setRemediation(condition.getValue());
    vulnerabilityCustomRemediation.setRefId(securityVulnerability.getRefId());
    vulnerabilityCustomRemediation.setOwnerId(repository.getId());
    vulnerabilityCustomRemediation.setLastUpdatedByUsername("testUser");
    vulnerabilityCustomRemediationDAO.insert(vulnerabilityCustomRemediation);

    testEvaluate_SecurityCondition(repository, componentIdentifier, componentHash, condition, securityVulnerability);
  }

  @Test
  public void testEvaluate_SecurityVulnerabilityCustomCVSSVectorStringConditionType() {
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    String componentHash = "testHash";
    Condition condition =
        new Condition(SecurityVulnerabilityCustomCVSSVectorStringConditionType.ID, "matches", "testCVSSVectorString");
    SecurityVulnerability securityVulnerability = new SecurityVulnerability("cve-2019-1234", "sonatype", 5.0f);
    VulnerabilityCustomCvssVector customCvssVector = new VulnerabilityCustomCvssVector();
    customCvssVector.setOwnerId(repository.getId());
    customCvssVector.setRefId(securityVulnerability.getRefId());
    customCvssVector.setComponentIdentifier(componentIdentifier);
    customCvssVector.setLastUpdatedByUsername("testUser");
    customCvssVector.setLastUpdatedAt(new Date());
    customCvssVector.setVector(condition.getValue());
    vulnerabilityCustomCvssVectorDAO.insert(customCvssVector);

    testEvaluate_SecurityCondition(repository, componentIdentifier, componentHash, condition, securityVulnerability);
  }

  @Test
  public void testEvaluate_SecurityVulnerabilityCategoryConditionType() {
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    String componentHash = "testHash";
    Condition condition = new Condition(SecurityVulnerabilityCategoryConditionType.ID, "is",
        SecurityVulnerabilityCategory.CONFIGURATION.getId());
    SecurityVulnerability securityVulnerability = new SecurityVulnerability("cve-2019-1234", "sonatype", 5.0f);
    securityVulnerability
        .setVulnerabilityCategories(Collections.singletonList(SecurityVulnerabilityCategory.CONFIGURATION.getId()));

    testEvaluate_SecurityCondition(repository, componentIdentifier, componentHash, condition, securityVulnerability);
  }

  @Test
  public void testEvaluate_SecurityVulnerabilitySeverityConditionType() {
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    String componentHash = "testHash";
    Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, "=", "5");
    SecurityVulnerability securityVulnerability = new SecurityVulnerability("cve-2019-1234", "sonatype", 5.0f);

    testEvaluate_SecurityCondition(repository, componentIdentifier, componentHash, condition, securityVulnerability);
  }

  @Test
  public void testEvaluate_SecurityVulnerabilitySeverityConditionType_CustomSeverity() {
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    String componentHash = "testHash";
    String customSeverity = "7";
    Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, "=", customSeverity);
    SecurityVulnerability securityVulnerability = new SecurityVulnerability("cve-2019-1234", "sonatype", 5.0f);
    VulnerabilityCustomCvssSeverity vulnerabilityCustomCvssSeverity = new VulnerabilityCustomCvssSeverity();
    vulnerabilityCustomCvssSeverity.setOwnerId(repository.getId());
    vulnerabilityCustomCvssSeverity.setRefId(securityVulnerability.getRefId());
    vulnerabilityCustomCvssSeverity.setLastUpdatedByUsername("testUser");
    vulnerabilityCustomCvssSeverity.setSeverity(Float.valueOf(customSeverity));
    vulnerabilityCustomCvssSeverityDAO.insert(vulnerabilityCustomCvssSeverity);

    testEvaluate_SecurityCondition(repository, componentIdentifier, componentHash, condition, securityVulnerability);
  }

  @Test
  public void testEvaluate_SecurityVulnerabilitySourceConditionType() {
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    String componentHash = "testHash";
    Condition condition = new Condition(SecurityVulnerabilitySourceConditionType.ID, "is", "sonatype");
    SecurityVulnerability securityVulnerability = new SecurityVulnerability("cve-2019-1234", "sonatype", 5.0f);

    testEvaluate_SecurityCondition(repository, componentIdentifier, componentHash, condition, securityVulnerability);
  }

  @Test
  public void testEvaluate_VulnerabilityGroupConditionType() {
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    String componentHash = "testHash";
    Condition condition = new Condition(VulnerabilityGroupConditionType.ID, "is", "testVulnerabilityGroupId");
    SecurityVulnerability securityVulnerability = new SecurityVulnerability("cve-2019-1234", "sonatype", 5.0f);
    VulnerabilityGroup vulnerabilityGroup = new VulnerabilityGroup("testGroupName", Organization.ROOT_ORGANIZATION_ID);
    vulnerabilityGroup.setId(condition.getValue());
    vulnerabilityGroupDAO.insert(vulnerabilityGroup);
    VulnerabilityGroupVulnerability vulnerabilityGroupVulnerability =
        new VulnerabilityGroupVulnerability(vulnerabilityGroup.getId(), securityVulnerability.getRefId());
    vulnerabilityGroupVulnerabilityDAO.insert(vulnerabilityGroupVulnerability);

    testEvaluate_SecurityCondition(repository, componentIdentifier, componentHash, condition, securityVulnerability);
  }

  private void testEvaluate_SecurityCondition(
      Repository repository,
      ComponentIdentifier componentIdentifier,
      String componentHash,
      Condition policyCondition,
      SecurityVulnerability securityVulnerability)
  {
    String componentPath = "testPath";

    Policy policy = tempEntity.newPolicy(repository.getId(), 5 /* threatLevel */,
        LogicalOperator.AND, policyCondition);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT;

    // Prepare request and mock the HDS request
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("maven2", componentPath, componentHash);
    ComponentEvaluationData componentEvaluationData = createComponentEvaluationData(componentIdentifier, componentHash,
        MatchState.EXACT, 0, null, null, Collections.singletonList(securityVulnerability), 1);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(componentEvaluationData);
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, true,
        null /* clientUserAgent */);

    List<RepositoryPolicyViolation> policyViolations =
        repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(repository.getId(), componentPath);

    assertThat(policyViolations).hasSize(1);
    RepositoryPolicyViolation policyViolation = policyViolations.get(0);
    assertThat(policyViolation.getPolicyId()).isEqualTo(policy.getId());
  }

  @Test
  public void testEvaluate_PolicyAtRootOrgLevel() {
    Repository repository = tempEntity.newRepository();

    testEvaluate(repository, Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testEvaluate_RepositoryContainerLevel() {
    Repository repository = tempEntity.newRepository();

    testEvaluate(repository, RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testEvaluate_PolicyAtRepositoryManager() {
    Repository repository = tempEntity.newRepository();

    testEvaluate(repository, repository.getRepositoryManagerId());
  }

  @Test
  public void testEvaluate_PolicyAtRepository() {
    Repository repository = tempEntity.newRepository();

    testEvaluate(repository, repository.getId());
  }

  private void testEvaluate(Repository repository, String policyOwnerId) {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    String componentHash = "testHash";
    String componentPath = "testPath";
    SecurityVulnerability securityVulnerability = new SecurityVulnerability("cve-2019-1234", "sonatype", 5.0f);

    Policy policy = tempEntity.newPolicy(policyOwnerId);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT;

    // Prepare request and mock the HDS request
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("maven2", componentPath, componentHash);
    ComponentEvaluationData componentEvaluationData = createComponentEvaluationData(componentIdentifier, componentHash,
        MatchState.EXACT, 0, null, null, Collections.singletonList(securityVulnerability), 1);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(componentEvaluationData);
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, true,
        null /* clientUserAgent */);

    List<RepositoryPolicyViolation> policyViolations =
        repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(repository.getId(), componentPath);

    assertThat(policyViolations).hasSize(1);
    RepositoryPolicyViolation policyViolation = policyViolations.get(0);
    assertThat(policyViolation.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyViolation.getActionTypeId()).isNull();
  }

  @Test
  public void testEvaluate_ActionAndNotificationOverrides_AtRepositoryContainerLevel() throws Exception {
    Repository repository = tempEntity.newRepository();

    testEvaluate_ActionAndNotificationOverrides(repository, RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testEvaluate_ActionAndNotificationOverrides_AtRepositoryManagerLevel() throws Exception {
    Repository repository = tempEntity.newRepository();

    testEvaluate_ActionAndNotificationOverrides(repository, repository.getRepositoryManagerId());
  }

  @Test
  public void testEvaluate_ActionAndNotificationOverrides_AtRepositoryLevel() throws Exception {
    Repository repository = tempEntity.newRepository();

    testEvaluate_ActionAndNotificationOverrides(repository, repository.getId());
  }

  private void testEvaluate_ActionAndNotificationOverrides(
      Repository repository,
      String ownerIdForOverrides) throws Exception
  {
    String userEmailAddress = "testuser@sonatype.com";

    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    policy.setPolicyActionsOverrideAllowed(true);
    Map<String, String> actionsOverride = new LinkedHashMap<>();
    actionsOverride.put(ProxyStageType.ID, FailActionType.ID);
    policy.addPolicyActionsOverride(ownerIdForOverrides, actionsOverride);

    policy.setPolicyNotificationsOverrideAllowed(true);
    Notifications notificationsOverride = new Notifications();
    notificationsOverride.add(new UserNotification(userEmailAddress, ProxyStageType.ID));
    policy.addPolicyNotificationsOverride(ownerIdForOverrides, notificationsOverride);
    policyDAO.update(policy);

    // Prepare request and mock the HDS request
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList(RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    String componentHash = "testHash";
    String componentPath = "testPath";
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("maven2", componentPath, componentHash);
    ComponentEvaluationData componentEvaluationData = createComponentEvaluationData(componentIdentifier, componentHash,
        MatchState.EXACT, 0, null, null, securityVulnerabilities, 1);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(componentEvaluationData);
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    List<Message> notificationsUser = MailboxTestUtil.get(userEmailAddress);
    notificationsUser.clear();

    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, true,
        null /* clientUserAgent */);

    List<RepositoryPolicyViolation> policyViolations =
        repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(repository.getId(), componentPath);

    assertThat(policyViolations).hasSize(1);
    RepositoryPolicyViolation policyViolation = policyViolations.get(0);
    assertThat(policyViolation.getPolicyId()).isEqualTo(policy.getId());
    assertThat(policyViolation.getActionTypeId()).isEqualTo(FailActionType.ID);
    assertNotifications(notificationsUser, 1, 5000);
  }

  @Test
  public void testEvaluate_NewComponentViolationNotifications() throws Exception {
    Repository repository = tempEntity.newRepository();
    String user1EmailAddress = "user1@sonatype.com";
    String user2EmailAddress = "user2@sonatype.com";
    tempEntity.newPolicy(repository.getId(), "Test Policy", 10, null, null,
        new Notifications(new UserNotification(user1EmailAddress, Stage.ID_PROXY)));
    Policy waivedPolicy = tempEntity.newPolicy(repository.getId(), "Waived Policy", 10, null, null,
        new Notifications(new UserNotification(user2EmailAddress, Stage.ID_PROXY)));
    tempEntity.newWaiver(waivedPolicy.getId(), repository.getId());

    // Prepare request and mock the HDS request
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList(RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT);

    String hash1 = "hash1";
    String hash2 = "hash2";
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "pathname1", hash1));
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "pathname2", hash2));
    hdsResult.components
        .add(createComponentEvaluationData(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"),
            hash1, MatchState.EXACT, 0, null, null, securityVulnerabilities, 80));
    hdsResult.components
        .add(createComponentEvaluationData(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2"),
            hash2, MatchState.EXACT, 1, null, null, securityVulnerabilities, 80));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    List<Message> notificationsUser1 = MailboxTestUtil.get(user1EmailAddress);
    notificationsUser1.clear();
    List<Message> notificationsUser2 = MailboxTestUtil.get(user2EmailAddress);
    notificationsUser2.clear();

    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, true,
        null /* clientUserAgent */);

    List<RepositoryPolicyViolation> policyViolations =
        repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(4);

    // Notification message should have been sent
    assertNotifications(notificationsUser1, 1, 5000);
    assertNotifications(notificationsUser2, 0, 1000);
  }

  @Test
  public void testEvaluate_ManualMavenPathnameParser_PopulatesComponentFields() {
    Repository repository = tempEntity.newRepository();

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Test Maven pathname that requires manual parsing
    String mavenPathname = "org/apache/logging/log4j/log4j-core/2.14.1/log4j-core-2.14.1.jar";
    String hash = "testHash";

    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven", mavenPathname, hash));

    // Mock HDS response without componentIdentifier (simulates parser failure scenario)
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData = createComponentEvaluationData(
        null, HashHelper.truncateHash(hash), MatchState.EXACT, 0, null, null, createSecurityVulnerabilities(), 1);
    hdsResult.components.add(componentEvaluationData);

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Evaluate policies
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);

    // Verify component was created with correct identifier parsed from pathname
    RepositoryComponent repositoryComponent =
        repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), mavenPathname);
    assertThat(repositoryComponent).isNotNull();

    ComponentIdentifier identifier = repositoryComponent.getComponentIdentifier();
    assertThat(identifier).isNotNull();
    assertThat(identifier.getFormat()).isEqualTo("maven");

    Map<String, String> coordinates = identifier.getCoordinates();
    assertThat(coordinates.get("groupId")).isEqualTo("org.apache.logging.log4j");
    assertThat(coordinates.get("artifactId")).isEqualTo("log4j-core");
    assertThat(coordinates.get("version")).isEqualTo("2.14.1");
  }

  @Test
  public void testEvaluate_ManualMavenPathnameParser_WithoutLeadingSlash() {
    Repository repository = tempEntity.newRepository();

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Test Maven pathname without leading slash
    String mavenPathname = "com/google/guava/guava/30.1-jre/guava-30.1-jre.jar";
    String hash = "testHash";

    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven", mavenPathname, hash));

    // Mock HDS response without componentIdentifier
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData = createComponentEvaluationData(
        null, HashHelper.truncateHash(hash), MatchState.EXACT, 0, null, null, createSecurityVulnerabilities(), 1);
    hdsResult.components.add(componentEvaluationData);

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Evaluate policies
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);

    // Verify component was created with correct identifier
    RepositoryComponent repositoryComponent =
        repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), mavenPathname);
    assertThat(repositoryComponent).isNotNull();

    ComponentIdentifier identifier = repositoryComponent.getComponentIdentifier();
    assertThat(identifier).isNotNull();
    assertThat(identifier.getFormat()).isEqualTo("maven");

    Map<String, String> coordinates = identifier.getCoordinates();
    assertThat(coordinates.get("groupId")).isEqualTo("com.google.guava");
    assertThat(coordinates.get("artifactId")).isEqualTo("guava");
    assertThat(coordinates.get("version")).isEqualTo("30.1-jre");
  }

  @Test
  public void testEvaluate_ManualMavenPathnameParser_PackageUrlFormat() {
    Repository repository = tempEntity.newRepository();

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Test with packageUrl format (starts with "pkg:")
    String packageUrl = "pkg:maven/org.springframework/spring-core@5.3.23";
    String hash = "testHash";

    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven", packageUrl, hash));

    // Mock HDS response without componentIdentifier
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData = createComponentEvaluationData(
        null, HashHelper.truncateHash(hash), MatchState.EXACT, 0, null, null, Collections.emptyList(), 1);
    hdsResult.components.add(componentEvaluationData);

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Evaluate policies
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);

    // Verify component was created with correct identifier from packageUrl
    RepositoryComponent repositoryComponent =
        repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), packageUrl);
    assertThat(repositoryComponent).isNotNull();

    ComponentIdentifier identifier = repositoryComponent.getComponentIdentifier();
    assertThat(identifier).isNotNull();
    assertThat(identifier.getFormat()).isEqualTo("maven");

    Map<String, String> coordinates = identifier.getCoordinates();
    assertThat(coordinates.get("groupId")).isEqualTo("org.springframework");
    assertThat(coordinates.get("artifactId")).isEqualTo("spring-core");
    assertThat(coordinates.get("version")).isEqualTo("5.3.23");
  }

  @Test
  public void testEvaluate_ManualMavenPathnameParser_NpmFormat() {
    Repository repository = tempEntity.newRepository();

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Test npm component pathname
    String npmPathname = "lodash/-/lodash-4.17.20.tgz";
    String hash = "testHash";

    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("npm", npmPathname, hash));

    // Mock HDS response with npm componentIdentifier (npm parser should work)
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentEvaluationData componentEvaluationData = createComponentEvaluationData(
        ComponentIdentifier.createNpmCoordinates("lodash", "4.17.20"), hash,
        MatchState.EXACT, 0, null, null, Collections.emptyList(), 1);
    hdsResult.components.add(componentEvaluationData);

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Evaluate policies
    repositoryPolicyEvaluator.evaluate(repository, componentEvaluationDataRequestList, false /* withQuarantine */,
        null /* clientUserAgent */);

    // Verify component was created with npm identifier
    RepositoryComponent repositoryComponent =
        repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), npmPathname);
    assertThat(repositoryComponent).isNotNull();

    ComponentIdentifier identifier = repositoryComponent.getComponentIdentifier();
    assertThat(identifier).isNotNull();
    assertThat(identifier.getFormat()).isEqualTo("npm");

    Map<String, String> coordinates = identifier.getCoordinates();
    assertThat(coordinates.get("packageId")).isEqualTo("lodash");
    assertThat(coordinates.get("version")).isEqualTo("4.17.20");
  }

  @Test
  public void testEvaluateForAutomaticRelease() {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(repository.getId());

    // Create a component that was previously quarantined
    RepositoryComponent quarantinedComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "testPath", new Date());
    quarantinedComponent.setQuarantineTime(new Date());
    repositoryComponentDAO.update(quarantinedComponent);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = RepositoryComponentEvaluationDataRequestList.ADHOC;

    // Prepare request and mock the HDS request - component now has no violations
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", quarantinedComponent.getPathname(),
            quarantinedComponent.getHash()));
    hdsResult.components.add(createComponentEvaluationData(
        quarantinedComponent.getComponentIdentifier(), quarantinedComponent.getHash(),
        MatchState.EXACT, 0 /* index */, null /* declaredLicenseSet */, null /* observedLicenseSet */,
        Collections.emptyList(), 1 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Delete the policy so the component will be automatically released
    policyDAO.delete(policy);

    // Call evaluateForAutomaticRelease
    repositoryPolicyEvaluator.evaluateForAutomaticRelease(repository, componentEvaluationDataRequestList);

    // Verify component is no longer quarantined
    RepositoryComponent updatedComponent =
        repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), quarantinedComponent.getPathname());
    assertThat(updatedComponent).isNotNull();
    assertThat(updatedComponent.isQuarantined()).isFalse();
    assertThat(updatedComponent.getUnquarantineTime()).isNotNull();

    // Verify telemetry calls in order
    InOrder inOrder = inOrder(repositoryComponentTelemetryCreator);

    // First call: RELEASE_QUARANTINE telemetry with ReleaseReason.AUTO_RELEASED
    inOrder.verify(repositoryComponentTelemetryCreator)
        .sendRepositoryComponentTelemetry(any(RepositoryComponent.class), eq(Collections.emptyList()),
            eq(repository.getRepositoryManagerId()), eq(repository.getPublicId()),
            eq(RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE),
            eq(ReleaseQuarantineType.AUTO), eq(ReleaseReason.AUTO_RELEASED.getDescription()),
            eq(Collections.emptyList()));

    // Second call: AUDIT telemetry after evaluation (component is no longer quarantined)
    inOrder.verify(repositoryComponentTelemetryCreator)
        .sendRepositoryComponentTelemetry(any(RepositoryComponent.class), eq(Collections.emptyList()),
            eq(repository.getRepositoryManagerId()), eq(repository.getPublicId()),
            eq(RepositoryComponentTelemetryEventType.AUDIT),
            eq(Collections.emptyList()), any(Component.class));

    verifyNoMoreInteractions(repositoryComponentTelemetryCreator);
  }
}
