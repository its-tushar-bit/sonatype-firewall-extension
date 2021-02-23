/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.mail.Message;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.hds.FirewallAuditHdsClient;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.IdentificationSourceConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.repository.RepositoryPolicyAlertEmailer;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;
import org.mockito.Mock;

import static com.sonatype.insight.brain.Assert.assertNotifications;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

public abstract class AbstractRepositoryServiceTest
    extends AbstractComponentTest
{
  protected static final String MANUAL_REPO_MAN_INSTANCE_ID = "manualDeleteRepoManagerInstanceId";

  protected static final String REPO_MAN_INSTANCE_ID = "repoManagerInstanceId";

  protected static final String REPO_PUBLIC_ID = "repoPublicId";

  @Rule
  public LogOutput policyViolationLoggerOutput = new LogOutput(
      AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Rule
  public LogOutput emailerLogOutput = new LogOutput(RepositoryPolicyAlertEmailer.class);

  @Inject
  protected TestProductLicense testProductLicense;

  private RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  private RepositoryDAO repositoryDAO = new RepositoryDAO();

  private RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = new RepositoryPolicyViolationDAO();

  private ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO =
      new ProprietaryComponentNamePatternDAO();

  @Mock
  private FirewallAuditHdsClient auditHdsClient;

  @Mock
  protected HdsClient hdsClient;

  @Mock
  private FirewallQuarantineHdsClient quarantineHdsClient;

  protected abstract AbstractRepositoryService getRepositoryService();

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClient);
    binder.bind(FirewallAuditHdsClient.class).toInstance(auditHdsClient);
    binder.bind(FirewallQuarantineHdsClient.class).toInstance(quarantineHdsClient);
    super.configure(binder);
  }

  @Before
  public void before() {
    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    lenient().when(
        hdsClient.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(hdsResult);

    InsightConfig insightConfig = lookup(InsightConfig.class);
    insightConfig.setBaseUrl("http://localhost");

    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("127.0.0.1");
    mailConfiguration.setPort(587);
    mailConfiguration.setSystemEmail("NexusIQServer@localhost");
    new MailConfigurationDAO().set(mailConfiguration);
  }

  @After
  public void cleanup() {
    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(MANUAL_REPO_MAN_INSTANCE_ID);
    if (repositoryManager != null) {
      repositoryManagerDAO.delete(repositoryManager);
    }
  }

  @Test
  public void testSetEnabled_NoRepositoryManager() throws Exception {
    getRepositoryService().setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);

    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(MANUAL_REPO_MAN_INSTANCE_ID);

    assertThat(repositoryManager).isNotNull();

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertThat(repositories).hasSize(1);
    assertThat(repositories.get(0).getPublicId()).isEqualTo(REPO_PUBLIC_ID);
    assertThat(repositories.get(0).isEnabled()).isTrue();
  }

  @Test
  public void testSetEnabled_ExistingRepositoryManager() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);

    getRepositoryService().setEnabled(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertThat(repositories).hasSize(1);
    assertThat(repositories.get(0).getPublicId()).isEqualTo(REPO_PUBLIC_ID);
    assertThat(repositories.get(0).isEnabled()).isTrue();
  }

  @Test
  public void testSetEnabled_TrueExistingRepository() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    getRepositoryService().setEnabled(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertThat(repositories).hasSize(1);
    assertThat(repositories.get(0).getPublicId()).isEqualTo(REPO_PUBLIC_ID);
    assertThat(repositories.get(0).isEnabled()).isTrue();
  }

  @Test
  public void testSetEnabled_MissingLicenseFeature() throws Exception {
    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      getRepositoryService().setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);
    }).withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testSetEnabled_FalseExistingRepository() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    getRepositoryService().setEnabled(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, false);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertThat(repositories).hasSize(1);
    assertThat(repositories.get(0).getPublicId()).isEqualTo(REPO_PUBLIC_ID);
    assertThat(repositories.get(0).isEnabled()).isFalse();
  }

  @Test
  public void testSetQuarantine_RepositoryDoesNotExist() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      getRepositoryService().setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID));
  }

  @Test
  public void testSetQuarantine_EnabledWhenRepositoryNotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);
    }).withMessage("Cannot enable quarantine when repository " + REPO_PUBLIC_ID + " is disabled.");
  }

  @Test
  public void testSetQuarantine_DisabledWhenRepositoryNotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false, true);

    // Check initial state
    assertThat(repository.isEnabled()).isFalse();
    assertThat(repository.isQuarantineEnabled()).isFalse();

    getRepositoryService().setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, false);
    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isFalse();
    assertThat(repository.isQuarantineEnabled()).isFalse();
  }

  @Test
  public void testSetQuarantine_EnabledWhenRepositoryEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    // Check that the initial value is false
    assertThat(repository.isQuarantineEnabled()).isFalse();

    getRepositoryService().setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);
    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isQuarantineEnabled()).isTrue();
  }

  @Test
  public void testSetQuarantine_DisabledWhenRepositoryEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    // Check that initial value is true
    assertThat(repository.isEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();

    getRepositoryService().setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, false);
    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isFalse();
  }

  @Test
  public void testGetPolicyEvaluationSummary() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    // Now add a waived one that should not show up in the test
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1", true, "policyId1", "policyName1",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    // And one not in the range that should not show up in the test
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "path4",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));

    // And a quarantined component
    tempEntity.newRepositoryComponent(repository.getId(), "/quarantined", new Date(), null);

    RepositoryPolicyEvaluationSummary policyEvaluationSummary = getRepositoryService()
        .getPolicyEvaluationSummary(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getReportUrl())
        .isEqualTo("ui/links/repository/" + repository.getId() + "/result");
    assertThat(policyEvaluationSummary.getQuarantinedComponentCount()).isEqualTo(1);
  }

  @Test
  public void testGetPolicyEvaluationSummary_ComponentIsCriticalAndSevereAndModerate() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 4, "path1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 2, "path1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));

    RepositoryPolicyEvaluationSummary policyEvaluationSummary = getRepositoryService()
        .getPolicyEvaluationSummary(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(1);
  }

  @Test
  public void testGetPolicyEvaluationSummary_SameComponentDifferentPolicy() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1", false, "policyId1", "policyName1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1", false, "policyId2", "policyName2",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));

    RepositoryPolicyEvaluationSummary policyEvaluationSummary = getRepositoryService()
        .getPolicyEvaluationSummary(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(1);
  }

  @Test
  public void testGetPolicyEvaluationSummary_MissingLicenseFeature() throws Exception {
    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      getRepositoryService().getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    }).withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_RepositoryDoesNotExist() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      getRepositoryService().evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null, true, null);
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID));
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_NullRequest() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataList componentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null, true, null);
    assertThat(componentEvaluationResultList.componentEvalResults).isEmpty();
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_EmptyPathname() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest();
    repositoryComponentEvaluationDataRequest.format = "maven";
    repositoryComponentEvaluationDataRequest.hash = "hash";
    repositoryComponentEvaluationDataRequest.pathname = "";

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService()
          .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    }).withMessage("The pathname cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_EmptyHash() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest();
    repositoryComponentEvaluationDataRequest.format = "maven";
    repositoryComponentEvaluationDataRequest.hash = "";
    repositoryComponentEvaluationDataRequest.pathname = "path";
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService()
          .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    }).withMessage("The hash cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_WithQuarantine() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    Policy policy = createQuarantiningPolicy(repository);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    String pathname = "path";
    String hash = "h";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest(
            "maven2", pathname, hash);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(
        createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */, declaredLicenseSet,
            observedLicenseSet, securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // Call the service
    Date before = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex).isEqualTo(0);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isTrue();
    Date after = new Date();

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);

    RepositoryComponent repositoryComponent = repositoryComponentDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname);
    assertRepositoryComponent(repository.getId(), pathname, before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), before, after, after, repositoryComponent);

    RepositoryPolicyViolation policyViolation = repositoryPolicyViolationDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname).get(0);
    assertPolicyViolation(repository.getId(), pathname, policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolation);
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_QuarantinedUnchangedComponentRemainsQuarantined() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Policy policy = createQuarantiningPolicy(repository);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    String pathname = "path";
    String hash = "h";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest(
            "maven2", pathname, hash);

    // prepare hds response with violation
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        Collections.emptySet(), Collections.emptySet(), securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // call the service
    Date timeBeforeEvaluation = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex).isEqualTo(0);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isTrue();
    Date timeAfterEvaluation = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);

    RepositoryComponent repositoryComponent = repositoryComponentDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname);
    assertRepositoryComponent(repository.getId(), pathname, timeBeforeEvaluation, timeAfterEvaluation, hash,
        componentIdentifier, MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), timeBeforeEvaluation,
        timeAfterEvaluation, timeAfterEvaluation, repositoryComponent);
    assertThat(repositoryComponent.isQuarantined()).isTrue();

    RepositoryPolicyViolation policyViolation = repositoryPolicyViolationDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname).get(0);
    assertPolicyViolation(repository.getId(), pathname, policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, timeBeforeEvaluation, timeAfterEvaluation,
        policyViolation);

    // prepare a hds request with no violations
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // evaluate and confirm quarantine state
    repositoryComponentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex).isEqualTo(0);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isTrue();

    List<RepositoryPolicyViolation> currentRepositoryPolicyViolations = repositoryPolicyViolationDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname);
    assertThat(currentRepositoryPolicyViolations.isEmpty()).isTrue();
    repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), pathname);
    assertThat(repositoryComponent.isQuarantined()).isTrue();
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_NotQuarantinedUnchangedComponentRemainsNotQuarantined()
      throws Exception
  {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Policy policy = createQuarantiningPolicy(repository);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    String pathname = "path";
    String hash = "h";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest(
            "maven2", pathname, hash);

    // prepare hds response with no violations
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // call to evaluate
    Date timeBeforeEvaluation = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex).isEqualTo(0);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isFalse();

    List<RepositoryPolicyViolation> currentRepositoryPolicyViolations = repositoryPolicyViolationDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname);
    assertThat(currentRepositoryPolicyViolations.isEmpty()).isTrue();

    RepositoryComponent repositoryComponent = repositoryComponentDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname);
    assertThat(repositoryComponent.isQuarantined()).isFalse();

    // prepare hds result with violations
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        Collections.emptySet(), Collections.emptySet(), securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // call to evaluate
    repositoryComponentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    Date after = new Date();
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex).isEqualTo(0);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isFalse();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);

    repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), pathname);
    assertRepositoryComponent(repository.getId(), pathname, timeBeforeEvaluation, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), timeBeforeEvaluation, after, null,
        repositoryComponent);
    assertThat(repositoryComponent.isQuarantined()).isFalse();

    RepositoryPolicyViolation policyViolation = repositoryPolicyViolationDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname).get(0);
    assertPolicyViolation(repository.getId(), pathname, policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, timeBeforeEvaluation, after, policyViolation);
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_PathnameSlashPrefix() throws Exception {
    String pathname = "path";
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    Policy policy = createQuarantiningPolicy(repository);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    String hash = "h";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest(
            "maven2", "/" + pathname, hash);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(
        createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */, declaredLicenseSet,
            observedLicenseSet, securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // Call the service
    Date before = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex).isEqualTo(0);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isTrue();
    Date after = new Date();

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);

    RepositoryComponent repositoryComponent = repositoryComponentDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname);
    assertRepositoryComponent(repository.getId(), pathname, before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), before, after, after, repositoryComponent);

    RepositoryPolicyViolation policyViolation = repositoryPolicyViolationDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname).get(0);
    assertPolicyViolation(repository.getId(), pathname, policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolation);
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_NoViolations() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    List<SecurityVulnerability> securityVulnerabilities = new ArrayList<>();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    String pathname = "path";
    String hash = "h";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest(
            "maven2", pathname, hash);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(
        createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */, declaredLicenseSet,
            observedLicenseSet, securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // Call the service
    Date before = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex).isEqualTo(0);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isFalse();
    Date after = new Date();

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);

    RepositoryComponent repositoryComponent = repositoryComponentDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname);
    assertRepositoryComponent(repository.getId(), pathname, before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_Waived() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    String hash = "h";

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());
    tempEntity.newWaiver(hash, policy.getId(), repository.getId());

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    String pathname = "path";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest(
            "maven2", pathname, hash);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(
        createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */, declaredLicenseSet,
            observedLicenseSet, securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // Call the service
    Date before = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex).isEqualTo(0);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isFalse();

    Date after = new Date();

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);

    RepositoryComponent repositoryComponent = repositoryComponentDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname);
    assertRepositoryComponent(repository.getId(), pathname, before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_QuarantineRequestAfterAuditWithoutExplicitRemoval()
      throws Exception
  {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    createQuarantiningPolicy(repository);

    String hash = "hash";
    String pathname = "pathname";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNugetCoordinates("p", "1");

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("nuget", pathname, hash));

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0,
        Collections.singleton(new License("EPL-1.0", "EPL-2.0")),
        Collections.singleton(new License("EPL-1.0", "EPL-2.0")), createSecurityVulnerabilities(), 80));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // initial evaluation of component, audit-only
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isFalse();

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isFalse();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    assertThat(repositoryComponents.get(0).getPathname()).isEqualTo(pathname);
    assertThat(repositoryComponents.get(0).getQuarantineTime()).isNull();

    // re-evaluation of component, this time with quarantine enabled
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);
    repositoryComponentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isFalse();

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();

    repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    assertThat(repositoryComponents.get(0).getPathname()).isEqualTo(pathname);
    assertThat(repositoryComponents.get(0).isQuarantined()).isFalse();
    assertThat(repositoryComponents.get(0).getQuarantineTime()).isNull();
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_QuarantineRequestAfterUnquarantineWithoutExplicitRemoval()
      throws Exception
  {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    createQuarantiningPolicy(repository);

    String hash = "hash";
    String pathname = "pathname";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNugetCoordinates("p", "1");

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("nuget", pathname, hash));

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0,
        Collections.singleton(new License("EPL-1.0", "EPL-2.0")),
        Collections.singleton(new License("EPL-1.0", "EPL-2.0")), createSecurityVulnerabilities(), 80));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true /* quarantine */);

    // Initial evaluation of component, quarantine enabled
    Date before = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
            true /* withQuarantine */, null);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isTrue();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertThat(repositoryComponent.isQuarantined()).isTrue();

    // Unquarantine the component
    repositoryComponent.setUnquarantineTimeForManualRelease(new Date());
    repositoryComponentDAO.update(repositoryComponent);
    repositoryComponent = repositoryComponentDAO.getById(repositoryComponent.getId());
    assertThat(repositoryComponent.isQuarantined()).isFalse();

    // Re-evaluation of component, quarantine enabled
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);
    repositoryComponentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    Date after = new Date();
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isFalse();

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();

    repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    repositoryComponent = repositoryComponents.get(0);
    assertThat(repositoryComponent.getPathname()).isEqualTo(pathname);
    assertThat(repositoryComponent.getQuarantineTime()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    assertThat(repositoryComponent.isQuarantined()).isFalse();
  }

  @Test
  public void testEvaluateComponents_RepositoryDoesNotExist() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      getRepositoryService()
          .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null /* componentEvaluationDataRequestList */,
              false, null);
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID));
  }

  @Test
  public void testEvaluateComponents_ExistingRepository_NotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false, false);

    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null /* componentEvaluationDataRequestList */, false,
            null);

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isFalse();
  }

  @Test
  public void testEvaluateComponents_ExistingRepository_QuarantineNotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, false);

    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null /* componentEvaluationDataRequestList */, true,
            null);

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();
  }

  @Test
  public void testEvaluateComponents_ExistingRepository_RepositoryAndQuarantineNotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false, false);

    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null /* componentEvaluationDataRequestList */, true,
            null);

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();
  }

  @Test
  public void testEvaluateComponents_MultipleComponents() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    int componentCount = 2;
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    for (int i = 0; i < componentCount; i++) {
      ComponentIdentifier componentIdentifier = ComponentIdentifier
          .createMavenCoordinates("g" + i, "a" + i, "v" + i, "c" + i, "e" + i);
      componentEvaluationDataRequestList.components
          .add(new RepositoryComponentEvaluationDataRequest("maven2", "path" + i, "h" + i));
      hdsResult.components.add(
          createComponentEvaluationData(componentIdentifier, "h" + i, MatchState.EXACT, i /* index */,
              declaredLicenseSet, observedLicenseSet, securityVulnerabilities, i /* popularity */));
    }
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    Date after = new Date();

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(2);
    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(2);

    for (int i = 0; i < componentCount; i++) {
      String pathname = "path" + i;
      ComponentIdentifier componentIdentifier = ComponentIdentifier
          .createMavenCoordinates("g" + i, "a" + i, "v" + i, "c" + i, "e" + i);
      String hash = "h" + i;

      RepositoryComponent repositoryComponent = repositoryComponentDAO
          .getByRepositoryIdAndPathname(repository.getId(), pathname);
      assertRepositoryComponent(repository.getId(), pathname, before, after, hash, componentIdentifier,
          MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);

      RepositoryPolicyViolation policyViolation = repositoryPolicyViolationDAO
          .getByRepositoryIdAndPathname(repository.getId(), pathname).get(0);
      assertPolicyViolation(repository.getId(), pathname, policy.getId(), policy.getName(), policy.getThreatLevel(),
          policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolation);
    }
  }

  @Test
  public void testEvaluateComponents_NewComponentViolationNotifications() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    String user1EmailAddress = "user1@sonatype.com";
    String user2EmailAddress = "user2@sonatype.com";
    tempEntity.newPolicy(repository.getParentOwnerId(), "Test Policy", 10, null, null,
        new Notifications(new UserNotification(user1EmailAddress, Stage.ID_PROXY)));
    Policy waivedPolicy = tempEntity.newPolicy(repository.getParentOwnerId(), "Waived Policy", 10, null, null,
        new Notifications(new UserNotification(user2EmailAddress, Stage.ID_PROXY)));
    tempEntity.newWaiver(waivedPolicy.getId(), repository.getId());

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList(
            RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT);

    String hash1 = "hash1";
    String hash2 = "hash2";

    // Prepare request and mock the HDS request
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();

    hdsResult.components = new ArrayList<>();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "pathname1", hash1));
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "pathname2", hash2));
    hdsResult.components.add(
        createComponentEvaluationData(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), hash1,
            MatchState.EXACT, 0, null, null, securityVulnerabilities, 80));
    hdsResult.components.add(
        createComponentEvaluationData(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), hash2,
            MatchState.EXACT, 1, null, null, securityVulnerabilities, 80));

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    List<Message> notificationsUser1 = Mailbox.get(user1EmailAddress);
    notificationsUser1.clear();
    List<Message> notificationsUser2 = Mailbox.get(user2EmailAddress);
    notificationsUser2.clear();

    // Call the service
    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(4);

    // Notification message should have been sent
    assertNotifications(notificationsUser1, 1, 5000);
    assertNotifications(notificationsUser2, 0, 1000);
  }

  @Test
  public void testEvaluateComponents_NotificationFailuresDoNotFailTheEvaluation() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    String userEmailAddress = "user@sonatype.com";
    tempEntity.newPolicy(repository.getParentOwnerId(), "Test Policy", 10, null, null,
        new Notifications(new UserNotification(userEmailAddress, Stage.ID_PROXY)));

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList(RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT);

    // Prepare request and mock the HDS request
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();

    hdsResult.components = new ArrayList<>();
    String hash = "hash";
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "pathname1", hash));
    hdsResult.components.add(createComponentEvaluationData(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"),
        hash, MatchState.EXACT, 0, null, null, securityVulnerabilities, 80));

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Remove the mail server configuration to trigger an error when notifications are sent.
    new MailConfigurationDAO().delete();

    // Call the service
    getRepositoryService().evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false, null);

    await().atMost(Duration.ofMillis(5000)).untilAsserted(() -> {
      assertThat(emailerLogOutput).atErrorLevel().contains(
          "Unable to send notification email to " + userEmailAddress + " for repository " + repository.getPublicId());
    });
  }

  @Test
  public void testEvaluateComponents_ReevaluationViolationNotifications() throws Exception {
    // This test ensures that there are no notifications for the evaluation cause other than "new component"
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    String userEmailAddress = "test@sonatype.com";
    tempEntity.newPolicy(repository.getParentOwnerId(), "Test Policy", 10, null, null,
        new Notifications(new UserNotification(userEmailAddress, Stage.ID_PROXY)));

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList(
            RepositoryComponentEvaluationDataRequestList.REEVALUATION);

    String hash = "hash";

    // Prepare request and mock the HDS request
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");

    hdsResult.components = new ArrayList<>();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven", "pathname", hash));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0, null, null,
        securityVulnerabilities, 80));

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    List<Message> notifications = Mailbox.get(userEmailAddress);
    notifications.clear();

    // Call the service
    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);

    assertNotifications(notifications, 0, 2000);
  }

  @Test
  public void testEvaluateComponents_Reevaluation() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Condition condition = new Condition(LicenseConditionType.ID, "is", "Apache-2.0");
    Constraint constraint = new Constraint("id", "name", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy = new Policy("id", "name");
    policy.setOwnerId(repository.getParentOwnerId());
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    String hash = "h";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path", hash));
    hdsResult.components.add(
        createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */, declaredLicenseSet,
            observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service first time
    Date before1 = new Date();
    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    Date after1 = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before1, after1, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before1, after1, policyViolations.get(0));

    // Call the service second time
    String updatedHash = "h1";
    ComponentIdentifier updatedComponentIdentifier = ComponentIdentifier
        .createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path", updatedHash));
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(
        createComponentEvaluationData(updatedComponentIdentifier, updatedHash, MatchState.EXACT, 0 /* index */,
            declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);
    Date before2 = new Date();
    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    Date after2 = new Date();

    repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before2, after2, updatedHash, updatedComponentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), before2, after2, null, repositoryComponent);

    policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);
    RepositoryPolicyViolation policyViolation = policyViolations.get(0);
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), updatedHash, updatedComponentIdentifier, before2, after2, policyViolation);
  }

  @Test
  public void testEvaluateComponents_LicenseOverridden() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Condition condition = new Condition(LicenseConditionType.ID, "is", "GPL-2.0");
    Constraint constraint = new Constraint("id", "name", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy = new Policy("id", "name");
    policy.setOwnerId(repository.getParentOwnerId());
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    tempEntity.newLicenseOverride(repository.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0");

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path", "h"));
    hdsResult.components.add(
        createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */, declaredLicenseSet,
            observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before, after, "h", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), "h", componentIdentifier, before, after, policyViolations.get(0));
  }

  @Test
  public void testEvaluateComponents_SecurityVulnerabilityOverridden() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Condition condition = new Condition(SecurityVulnerabilityStatusConditionType.ID, "is",
        SecurityVulnerabilityOverrideStatus.CONFIRMED.getId());
    Constraint constraint = new Constraint("id", "name", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy = new Policy("id", "name");
    policy.setOwnerId(repository.getParentOwnerId());
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    String hash = "ababababa";
    String source = "cve";
    String referenceId = "CVE-2009-1523";
    tempEntity.newSecurityVulnerabilityOverride(repository.getId(), hash, source, referenceId,
        SecurityVulnerabilityOverrideStatus.CONFIRMED);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    List<SecurityVulnerability> securityVulnerabilities = Collections
        .singletonList(new SecurityVulnerability(referenceId, source, 2.9F));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path", hash));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        null /* declaredLicenses */, null /* observedLicenses */, securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolations.get(0));
  }

  @Test
  public void testEvaluateComponents_ClaimedComponent() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Condition condition = new Condition(IdentificationSourceConditionType.ID, "is",
        IdentificationSource.MANUAL.getId());
    Constraint constraint = new Constraint("id", "name", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy = new Policy("id", "name");
    policy.setOwnerId(repository.getParentOwnerId());
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    ComponentIdentifier claimedComponentIdentifier = ComponentIdentifier
        .createMavenCoordinates("cg", "ca", "cv", "cc", "ce");
    tempEntity.newClaimedComponent("h", claimedComponentIdentifier);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path", "h"));
    hdsResult.components.add(
        createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */, declaredLicenseSet,
            observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    assertRepositoryComponent(repository.getId(), "path", before, after, "h", claimedComponentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.MANUAL.getId(), repositoryComponents.get(0));

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), "h", claimedComponentIdentifier, before, after, policyViolations.get(0));
  }

  @Test
  public void testEvaluateComponents_LongHash() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Condition condition = new Condition(LicenseConditionType.ID, "is", "Apache-2.0");
    Constraint constraint = new Constraint("id", "name", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy = new Policy("id", "name");
    policy.setOwnerId(repository.getParentOwnerId());
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    String hash = "01234567890123456789";
    String longHash = hash + "1";
    // Sanity check
    assertThat(longHash.length()).isGreaterThan(HashHelper.MAX_LENGTH);
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    // Prepare request and mock the HDS request
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path", longHash));
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(
        createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */, declaredLicenseSet,
            observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolations.get(0));
  }

  @Test
  public void testEvaluateComponents_UnknownComponent() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Condition condition = new Condition(MatchStateConditionType.ID, "is", MatchState.UNKNOWN.getId());
    Constraint constraint = new Constraint("id", "name", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy = new Policy("id", "name");
    policy.setOwnerId(repository.getParentOwnerId());
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    String hash = "hash";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");

    // Prepare request and mock the HDS request
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven", "path", hash));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.UNKNOWN, 0 /* index */,
        Collections.emptySet(), Collections.emptySet(), null /* securityVulnerabilities */, null /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before, after, hash, componentIdentifier,
        MatchState.UNKNOWN.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolations.get(0));
  }

  @Test
  public void testEvaluateComponents_pathnameSlashPrefix() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    String hash = "hash";
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNugetCoordinates("p", "v1");

    // Prepare request and mock the HDS request
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "/path", hash));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier1, hash, MatchState.EXACT, 0 /* index */,
        Collections.emptySet(), Collections.emptySet(), null /* securityVulnerabilities */, null /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before, after, hash, componentIdentifier1,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);
  }

  @Test
  public void testEvaluateComponents_NullPathname() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", null, "hash"));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService()
          .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    }).withMessage("The pathname cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_EmptyPathname() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", " ", "hash"));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService()
          .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    }).withMessage("The pathname cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_NullFormat() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest(null, "pathname", "hash"));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService()
          .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    }).withMessage("The format cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_EmptyFormat() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest(" ", "pathname", "hash"));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService()
          .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    }).withMessage("The format cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_NullHash() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    String hash = null;

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path", hash));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService()
          .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    }).withMessage("The hash cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_EmptyHash() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path", " "));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService()
          .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);
    }).withMessage("The hash cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_MissingLicenseFeature() throws Exception {
    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      getRepositoryService().evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null, false, null);
    }).withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  protected void mockHdsRequest(
      RepositoryComponentEvaluationDataRequestList serviceRequest,
      ComponentEvaluationDataList hdsResult,
      boolean quarantine)
  {
    RepositoryComponentEvaluationDataRequestList hdsRequest = new RepositoryComponentEvaluationDataRequestList();
    hdsRequest.cause = serviceRequest.cause;
    hdsRequest.components = new ArrayList<>();
    for (RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest : serviceRequest.components) {
      String hash = HashHelper.truncateHash(componentEvaluationDataRequest.hash);
      String pathname = componentEvaluationDataRequest.pathname
          .substring(componentEvaluationDataRequest.pathname.startsWith("/") ? 1 : 0);
      hdsRequest.components
          .add(new RepositoryComponentEvaluationDataRequest(componentEvaluationDataRequest.format, pathname, hash));
    }
    when((quarantine ? quarantineHdsClient : auditHdsClient)
        .post(any(), eq(ComponentEvaluationDataList.class), eq(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH),
            isNull(), eq(hdsRequest))).thenReturn(hdsResult);
  }

  protected ComponentEvaluationData createComponentEvaluationData(ComponentIdentifier componentIdentifier,
                                                                String hash,
                                                                MatchState matchState,
                                                                int index,
                                                                Set<License> declaredLicenses,
                                                                Set<License> observedLicenses,
                                                                List<SecurityVulnerability> securityVulnerabilities,
                                                                Integer relativePopularity)
  {
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.requestIndex = index;
    componentEvaluationData.hash = hash;
    componentEvaluationData.componentIdentifier = componentIdentifier;
    componentEvaluationData.matchState = matchState.getId();
    componentEvaluationData.declaredLicenses = declaredLicenses == null ? Collections.emptySet() : declaredLicenses;
    componentEvaluationData.observedLicenses = observedLicenses == null ? Collections.emptySet() : observedLicenses;
    componentEvaluationData.catalogDate = (long) index;
    componentEvaluationData.securityVulnerabilities = securityVulnerabilities;
    componentEvaluationData.relativePopularity = relativePopularity;

    return componentEvaluationData;
  }

  protected List<SecurityVulnerability> createSecurityVulnerabilities() {
    List<SecurityVulnerability> securityVulnerabilities = new ArrayList<>();
    SecurityVulnerability securityVulnerability = new SecurityVulnerability();
    securityVulnerability.setRefId("refId");
    securityVulnerability.setSeverity(5.0F);
    securityVulnerability.setSource("source");
    securityVulnerability.setUrl("test-url");
    securityVulnerabilities.add(securityVulnerability);
    return securityVulnerabilities;
  }

  private void assertRepositoryComponent(String repositoryId,
                                         String pathname,
                                         Date beforeCreate,
                                         Date afterCreate,
                                         String hash,
                                         ComponentIdentifier componentIdentifier,
                                         String matchStateId,
                                         String identificationSourceId,
                                         Date beforeLastEvaluation,
                                         Date afterLastEvaluation,
                                         Date afterQuarantineTime,
                                         RepositoryComponent actual)
  {
    assertThat(actual.getRepositoryId()).isEqualTo(repositoryId);
    assertThat(actual.getPathname()).isEqualTo(pathname);
    assertThat(actual.getHash()).isEqualTo(hash);
    assertThat(actual.getTime()).isAfterOrEqualTo(beforeCreate).isBeforeOrEqualTo(afterCreate);
    assertThat(actual.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(actual.getMatchStateId()).isEqualTo(matchStateId);
    assertThat(actual.getIdentificationSourceId()).isEqualTo(identificationSourceId);
    assertThat(actual.getLastEvaluationTime()).isAfterOrEqualTo(beforeLastEvaluation)
        .isBeforeOrEqualTo(afterLastEvaluation);
    if (afterQuarantineTime != null) {
      assertThat(actual.getQuarantineTime()).isBeforeOrEqualTo(afterQuarantineTime);
    }
    else {
      assertThat(actual.getQuarantineTime()).isNull();
    }
  }

  private void assertRepositoryComponent(String repositoryId,
                                         String pathname,
                                         Date beforeCreate,
                                         Date afterCreate,
                                         String hash,
                                         ComponentIdentifier componentIdentifier,
                                         String matchStateId,
                                         String identificationSourceId,
                                         RepositoryComponent actual)
  {
    assertRepositoryComponent(repositoryId, pathname, beforeCreate, afterCreate, hash, componentIdentifier,
        matchStateId, identificationSourceId, beforeCreate, afterCreate, null, actual);
  }

  private void assertPolicyViolation(String repositoryId,
                                     String pathname,
                                     String policyId,
                                     String policyName,
                                     int threatLevel,
                                     PolicyThreatCategory threatCategory,
                                     String hash,
                                     ComponentIdentifier componentIdentifier,
                                     Date before,
                                     Date after,
                                     RepositoryPolicyViolation actual)
  {
    assertThat(actual.getRepositoryId()).isEqualTo(repositoryId);
    assertThat(actual.getPathname()).isEqualTo(pathname);
    assertThat(actual.getPolicyId()).isEqualTo(policyId);
    assertThat(actual.getPolicyName()).isEqualTo(policyName);
    assertThat(actual.getThreatLevel()).isEqualTo(threatLevel);
    assertThat(actual.getThreatCategory()).isEqualTo(threatCategory);
    assertThat(actual.getHash()).isEqualTo(hash);
    assertThat(actual.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(actual.getTime()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
  }

  @Test
  public void testRemoveComponent_RepositoryDoesNotExist() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      getRepositoryService().removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "somepath");
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID));
  }

  @Test
  public void testRemoveComponent_RepositoryNotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false /* enabled */);

    getRepositoryService().removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "somepath");

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();
  }

  @Test
  public void testRemoveComponent() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);
    String pathname1 = "pathname1";
    String pathname2 = "pathname2";
    RepositoryComponent repositoryComponent1 = tempEntity.newRepositoryComponent(repository.getId(), pathname1);
    RepositoryComponent repositoryComponent2 = tempEntity.newRepositoryComponent(repository.getId(), pathname2);
    RepositoryPolicyViolation policyViolation1 = tempEntity.newRepositoryPolicyViolation(repository.getId(), pathname1);
    RepositoryPolicyViolation policyViolation2 = tempEntity.newRepositoryPolicyViolation(repository.getId(), pathname2);

    getRepositoryService().removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, pathname1);

    assertThat(repositoryComponentDAO.getById(repositoryComponent1.getId())).isNull();
    assertThat(repositoryComponentDAO.getById(repositoryComponent2.getId())).isNotNull();
    assertThat(repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), pathname1)).isNull();
    policyViolation1 = repositoryPolicyViolationDAO.getById(policyViolation1.getId());
    assertThat(policyViolation1).isNull();
    policyViolation2 = repositoryPolicyViolationDAO.getById(policyViolation2.getId());
    assertThat(policyViolation2).isNotNull();
  }

  @Test
  public void testRemoveComponent_pathnameSlashPrefix() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);
    String pathname1 = "pathname1";
    String pathname2 = "pathname2";
    RepositoryComponent repositoryComponent1 = tempEntity.newRepositoryComponent(repository.getId(), pathname1);
    RepositoryComponent repositoryComponent2 = tempEntity.newRepositoryComponent(repository.getId(), pathname2);
    RepositoryPolicyViolation policyViolation1 = tempEntity.newRepositoryPolicyViolation(repository.getId(), pathname1);
    RepositoryPolicyViolation policyViolation2 = tempEntity.newRepositoryPolicyViolation(repository.getId(), pathname2);

    getRepositoryService().removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "/" + pathname1);

    assertThat(repositoryComponentDAO.getById(repositoryComponent1.getId())).isNull();
    assertThat(repositoryComponentDAO.getById(repositoryComponent2.getId())).isNotNull();
    assertThat(repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), pathname1)).isNull();
    policyViolation1 = repositoryPolicyViolationDAO.getById(policyViolation1.getId());
    assertThat(policyViolation1).isNull();
    policyViolation2 = repositoryPolicyViolationDAO.getById(policyViolation2.getId());
    assertThat(policyViolation2).isNotNull();
  }

  private Policy createQuarantiningPolicy(Repository repository) {
    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    new PolicyDAO().update(policy);
    return policy;
  }

  @Test
  public void testGetUnquarantinedComponents() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    long since = System.currentTimeMillis();
    // Component that was never quarantined
    tempEntity.newRepositoryComponent(repository.getId(), "pathnameNotQuarantined");
    // Component quarantined after the "since" time and not un-quarantined
    tempEntity
        .newRepositoryComponent(repository.getId(), "pathnameQuarantined", new Date(since + 1) /* quarantineTime */,
            null /* unquarantineTime */);
    // Component un-quarantined before the "since" time
    tempEntity.newRepositoryComponent(repository.getId(), "pathnameUnquarantinedBefore",
        new Date(since - 1) /* quarantineTime */, new Date(since - 1) /* unquarantineTime */);
    // Component un-quarantined after the "since" time
    tempEntity
        .newRepositoryComponent(repository.getId(), "pathnameUnquarantinedAfter", new Date(since) /* quarantineTime */,
            new Date(since) /* unquarantineTime */);
    UnquarantinedComponentList result = getRepositoryService()
        .getUnquarantinedComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, since);
    assertThat(result.pathnames).containsExactly("pathnameUnquarantinedAfter");
  }

  @Test
  public void testGetUnquarantinedComponents_RepositoryDoesNotExist() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      getRepositoryService().getUnquarantinedComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, 0);
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID));
  }

  @Test
  public void testRemoveComponent_DeletesPolicyViolations() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), "pathname", new Date() /* quarantineTime */,
            null /* unquarantineTime */);
    RepositoryPolicyViolation policyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId(), "pathname");

    getRepositoryService().removeComponent(repository, repositoryComponent.getPathname());

    policyViolation = new RepositoryPolicyViolationDAO().getById(policyViolation.getId());
    assertThat(policyViolation).isNull();
  }

  @Test
  public void testRemoveComponent_PolicyViolationLogger_LogsFixEventForEachDeletedViolation() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), "path1");
    RepositoryPolicyViolation activeRepositoryPolicyViolation1 = tempEntity
        .newRepositoryPolicyViolation(repository.getId(), repositoryComponent.getPathname());
    RepositoryPolicyViolation activeRepositoryPolicyViolation2 = tempEntity
        .newRepositoryPolicyViolation(repository.getId(), repositoryComponent.getPathname());
    RepositoryComponent otherRepositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), "path2");
    tempEntity.newRepositoryPolicyViolation(repository.getId(), otherRepositoryComponent.getPathname());

    Date before = new Date();
    getRepositoryService().removeComponent(repository, repositoryComponent.getPathname());
    Date after = new Date();

    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(policyViolationLoggerOutput, 2);
    PolicyViolationLogDTOAssert
        .assertRepositoryPolicyViolationData(policyViolationLogDTOs, PolicyViolationLogEvent.FIX, repository, before,
            after, Arrays.asList(activeRepositoryPolicyViolation1, activeRepositoryPolicyViolation2));
  }

  @Test
  public void testSetEnabled_PolicyViolationLogger_DisabledLogsClearEvent() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    Date before = new Date();
    getRepositoryService().setEnabled(REPO_MAN_INSTANCE_ID, repository.getPublicId(), false);
    Date after = new Date();

    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(policyViolationLoggerOutput, 1);
    PolicyViolationLogDTOAssert
        .assertRepositoryPolicyViolationData(policyViolationLogDTOs.get(0), PolicyViolationLogEvent.CLEAR, repository,
            before, after);
  }

  @Test
  public void testSetEnabled_PolicyViolationLogger_EnabledDoesNotLogClearEvent() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    getRepositoryService().setEnabled(REPO_MAN_INSTANCE_ID, repository.getPublicId(), true);

    assertThat(policyViolationLoggerOutput.getInfoMessages(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME))
        .isEmpty();
  }

  @Test
  public void testAddProprietaryComponentNames() {
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";
    ProprietaryComponentNames proprietaryComponentNames =
        new ProprietaryComponentNames("npm").addNames("sonatype*").addNamespaces("@sonatype");

    getRepositoryService().addProprietaryComponentNames(repoManId, repoId, proprietaryComponentNames);

    List<ProprietaryComponentNamePattern> patterns = proprietaryComponentNamePatternDAO.getByFormat("npm");
    assertThat(patterns).allSatisfy(pattern -> {
      assertThat(pattern.getFormat()).isEqualTo("npm");
      assertThat(pattern.getRepositoryManagerInstanceId()).isEqualTo(repoManId);
      assertThat(pattern.getRepositoryPublicId()).isEqualTo(repoId);
    }).extracting(ProprietaryComponentNamePattern::getNamespacePattern, ProprietaryComponentNamePattern::getNamePattern)
        .containsExactlyInAnyOrder(tuple("@sonatype", null), tuple(null, "sonatype*"));
  }

  @Test
  public void testAddProprietaryComponentNames_MissingLicenseFeature() throws Exception {
    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      getRepositoryService().addProprietaryComponentNames(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
          new ProprietaryComponentNames());
    }).withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testAddProprietaryComponentNames_NoFirewallRepositoryRegistered() {
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";
    ProprietaryComponentNames proprietaryComponentNames = new ProprietaryComponentNames("npm", "name");

    getRepositoryService().addProprietaryComponentNames(repoManId, repoId, proprietaryComponentNames);

    assertThat(repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repoManId, repoId)).isNull();
  }

  @Test
  public void testAddProprietaryComponentNames_NullDto() {
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().addProprietaryComponentNames(repoManId, repoId, null);
    }).withMessageContaining("No component name patterns specified");
  }

  @Test
  public void testAddProprietaryComponentNames_NoPatterns() {
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";
    ProprietaryComponentNames proprietaryComponentNames = new ProprietaryComponentNames("npm");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().addProprietaryComponentNames(repoManId, repoId, proprietaryComponentNames);
    }).withMessageContaining("No component name patterns specified");
  }

  @Test
  public void testAddProprietaryComponentNames_NoFormat() {
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";
    ProprietaryComponentNames proprietaryComponentNames = new ProprietaryComponentNames(null, "name");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().addProprietaryComponentNames(repoManId, repoId, proprietaryComponentNames);
    }).withMessageContaining("No component format specified");
  }

  @Test
  public void testAddProprietaryComponentNames_BadPattern() {
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().addProprietaryComponentNames(repoManId, repoId,
          new ProprietaryComponentNames("npm").addNames(""));
    }).withMessageContaining("Empty component name pattern");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().addProprietaryComponentNames(repoManId, repoId,
          new ProprietaryComponentNames("npm").addNames("*"));
    }).withMessageContaining("Invalid component name pattern: *");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().addProprietaryComponentNames(repoManId, repoId,
          new ProprietaryComponentNames("npm").addNamespaces("foo*bar"));
    }).withMessageContaining("Invalid component namespace pattern: foo*bar");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().addProprietaryComponentNames(repoManId, repoId,
          new ProprietaryComponentNames("npm").addNamespaces("*foo*"));
    }).withMessageContaining("Invalid component namespace pattern: *foo*");
  }

  @Test
  public void testAddProprietaryComponentNames_FormatTranslation_Maven2() {
    testAddProprietaryComponentNames_FormatTranslation("maven2", "maven");
  }

  @Test
  public void testAddProprietaryComponentNames_FormatTranslation_Apk() {
    testAddProprietaryComponentNames_FormatTranslation("apk", "alpine");
  }

  @Test
  public void testAddProprietaryComponentNames_FormatTranslation_Apt() {
    testAddProprietaryComponentNames_FormatTranslation("apt", "deb");
  }

  @Test
  public void testAddProprietaryComponentNames_FormatTranslation_Go() {
    testAddProprietaryComponentNames_FormatTranslation("go", "golang");
  }

  @Test
  public void testAddProprietaryComponentNames_FormatTranslation_R() {
    testAddProprietaryComponentNames_FormatTranslation("r", "cran");
  }

  @Test
  public void testAddProprietaryComponentNames_FormatTranslation_Rubygems() {
    testAddProprietaryComponentNames_FormatTranslation("rubygems", "gem");
  }

  private void testAddProprietaryComponentNames_FormatTranslation(String repoFormat, String componentFormat) {
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";
    ProprietaryComponentNames proprietaryComponentNames = new ProprietaryComponentNames(repoFormat, "format-test");

    getRepositoryService().addProprietaryComponentNames(repoManId, repoId, proprietaryComponentNames);

    assertThat(proprietaryComponentNamePatternDAO.getByFormat(componentFormat))
        .extracting(ProprietaryComponentNamePattern::getNamePattern).containsExactlyInAnyOrder("format-test");
  }
}
