/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import static com.sonatype.insight.brain.Assert.assertNotifications;
import static com.sonatype.insight.brain.integration.repository.AbstractRepositoryService.REPOSITORY_COMPONENT_METADATA_EVALUATION_TIME;
import static com.sonatype.insight.brain.integration.repository.AbstractRepositoryService.REPOSITORY_COMPONENT_POLICY_COMPLIANT_VERSION_COUNT;
import static com.sonatype.insight.brain.integration.repository.AbstractRepositoryService.REPOSITORY_COMPONENT_REQUESTED_VERSION_COUNT;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.clm.dto.model.repository.QuarantinedComponentReport;
import com.sonatype.clm.dto.model.repository.RepositoryDTO;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
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
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
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
import com.sonatype.insight.brain.repository.RequestSafeComponentsMetricEventService;
import com.sonatype.insight.brain.repository.component.DbQuarantinedComponentAccessManager;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.test.MailboxTestUtil;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import jakarta.mail.Message;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

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

  @Rule
  public LogOutput repositoryServiceLogOutput = new LogOutput(AbstractRepositoryService.class);

  @Inject
  protected TestProductLicense testProductLicense;

  @Inject
  private RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  private RepositoryDAO repositoryDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private RepositoryComponentDAO repositoryComponentDAO;

  @Inject
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Inject
  private ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO;

  @Inject
  private MailConfigurationDAO mailConfigurationDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Mock
  private FirewallAuditHdsClient auditHdsClient;

  @Mock
  protected HdsClient hdsClient;

  @Mock
  private FirewallQuarantineHdsClient quarantineHdsClient;

  @Mock
  private RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator;

  protected abstract AbstractRepositoryService getRepositoryService();

  protected abstract String getUserAgent();

  protected abstract ConfigureRepositoriesRequest createConfigureRepositoriesRequest(
      List<RepositoryDTO> repositoryDTOs);

  @Mock
  private DbQuarantinedComponentAccessManager quarantinedComponentAccessManager;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Mock
  private RequestSafeComponentsMetricEventService requestSafeComponentsMetricEventServiceMock;

  @Mock
  private CurrentUser currentUser;

  @Mock
  private TaskScheduler mockTaskScheduler;

  private PolicyViolationLogDTOAssert policyViolationLogDTOAssert;

  @Before
  public void before() {
    policyViolationLogDTOAssert = new PolicyViolationLogDTOAssert(repositoryManagerDAO);

    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = Map.of("npm", List.of("random_ignore_pattern"));
    lenient().when(
        hdsClient.get(eq(FirewallIgnorePatterns.class), eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(hdsResult);
    setBaseUrl("http://localhost");

    MailConfiguration mailConfiguration = new MailConfiguration();
    mailConfiguration.setHostname("127.0.0.1");
    mailConfiguration.setPort(587);
    mailConfiguration.setSystemEmail("NexusIQServer@localhost");
    mailConfigurationDAO.set(mailConfiguration);
  }

  @Test
  public void testSetAuditEnabled_NoRepositoryManager() {
    getRepositoryService().setAuditEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true, null);

    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(MANUAL_REPO_MAN_INSTANCE_ID);

    assertThat(repositoryManager).isNotNull();
    assertThat(repositoryManager.isConfigured()).isTrue();

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertThat(repositories).hasSize(1);
    assertThat(repositories.get(0).getPublicId()).isEqualTo(REPO_PUBLIC_ID);
    assertThat(repositories.get(0).isAuditEnabled()).isTrue();
  }

  @Test
  public void testSetAuditEnabled_ExistingRepositoryManager() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);

    getRepositoryService().setAuditEnabled(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true, null);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertThat(repositories).hasSize(1);
    assertThat(repositories.get(0).getPublicId()).isEqualTo(REPO_PUBLIC_ID);
    assertThat(repositories.get(0).isAuditEnabled()).isTrue();
  }

  @Test
  public void testSetAuditEnabled_NotProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().setAuditEnabled(repoManager.getInstanceId(), repo.getPublicId(), true, null);
    }).withMessage("Repository " + repo.getPublicId() + " (" + repo.getId() + ") is not a proxy repository");
  }

  @Test
  public void testSetAuditEnabled_TrueExistingRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    ApiRepositoryDTO repositoryDTO =
        getRepositoryService().setAuditEnabled(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true, null);
    assertThat(repositoryDTO).isNotNull();
    assertThat(repositoryDTO.publicId).isEqualTo(REPO_PUBLIC_ID);
    assertThat(repositoryDTO.repositoryId).isNotBlank();

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertThat(repositories).hasSize(1);
    assertThat(repositories.get(0).getPublicId()).isEqualTo(REPO_PUBLIC_ID);
    assertThat(repositories.get(0).isAuditEnabled()).isTrue();
  }

  @Test
  public void testSetAuditEnabled_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(
            () -> getRepositoryService().setAuditEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true, null))
        .withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testSetAuditEnabled_FalseExistingRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    getRepositoryService().setAuditEnabled(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, false, null);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertThat(repositories).hasSize(1);
    assertThat(repositories.get(0).getPublicId()).isEqualTo(REPO_PUBLIC_ID);
    assertThat(repositories.get(0).isAuditEnabled()).isFalse();
  }

  @Test
  public void testSetQuarantine_RepositoryDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> getRepositoryService().setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true, null))
        .withMessage(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID));
  }

  @Test
  public void testSetQuarantine_EnabledWhenRepositoryNotEnabled() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true, null))
        .withMessage("Cannot enable quarantine when repository " + REPO_PUBLIC_ID + " is disabled.");
  }

  @Test
  public void testSetQuarantine_DisabledWhenRepositoryNotEnabled() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false, true);

    // Check initial state
    assertThat(repository.isAuditEnabled()).isFalse();
    assertThat(repository.isQuarantineEnabled()).isFalse();

    getRepositoryService().setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, false, null);
    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isAuditEnabled()).isFalse();
    assertThat(repository.isQuarantineEnabled()).isFalse();
  }

  @Test
  public void testSetQuarantine_EnabledWhenRepositoryEnabled() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    // Check that the initial value is false
    assertThat(repository.isQuarantineEnabled()).isFalse();

    getRepositoryService().setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true, null);
    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isQuarantineEnabled()).isTrue();
  }

  @Test
  public void testSetQuarantine_DisabledWhenRepositoryEnabled() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    // Check that initial value is true
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();

    getRepositoryService().setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, false, null);
    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isFalse();
  }

  @Test
  public void testSetQuarantine_NotProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().setQuarantine(repoManager.getInstanceId(), repo.getPublicId(), false, null);
    }).withMessage("Repository " + repo.getPublicId() + " (" + repo.getId() + ") is not a proxy repository");
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
        .getPolicyEvaluationSummary(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null);
    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getReportUrl())
        .isEqualTo("ui/links/repository/" + repository.getId() + "/result");
    assertThat(policyEvaluationSummary.getQuarantinedComponentCount()).isEqualTo(1);
  }

  @Test
  public void testGetPolicyEvaluationSummary_dockerProxyRepository() {
    Organization organization = tempEntity.newOrganization();

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    repository.setQuarantineEnabled(true);
    repository.setRelatedOrganizationId(organization.getId());
    organization.setRelatedRepositoryId(repository.getId());

    repositoryDAO.update(repository);
    organizationDAO.update(organization);

    // Container Image applications
    Application application1 = tempEntity.newApplication("app1", "appPublicId1", organization.getId());
    Application application2 = tempEntity.newApplication("app2", "appPublicId2", organization.getId());

    // policy evaluation
    PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(application1.getId(), Stage.ID_PROXY, "scanId1");
    PolicyEvaluation policyEvaluation2 =
        tempEntity.newPolicyEvaluation(application2.getId(), Stage.ID_PROXY, "scanId2");

    // policy for policy violation
    Policy policy1 = tempEntity.newPolicy(application1.getId(), "policy1", 10);
    Policy policy2 = tempEntity.newPolicy(application1.getId(), "policy2", 8);
    Policy policy3 = tempEntity.newPolicy(application1.getId(), "policy3", 10);
    Policy policy4 = tempEntity.newPolicy(application1.getId(), "policy4", 5);

    Policy policy5 = tempEntity.newPolicy(application2.getId(), "policy5", 10);
    Policy policy6 = tempEntity.newPolicy(application2.getId(), "policy6", 2);

    // create policy violations app 1
    tempEntity.newPolicyViolation(policyEvaluation1, policy1, 10, PolicyThreatCategory.SECURITY, "g", "a", "v", "h",
        Action.ID_FAIL);
    tempEntity.newPolicyViolation(policyEvaluation1, policy2);
    tempEntity.newPolicyViolation(policyEvaluation1, policy3);
    tempEntity.newPolicyViolation(policyEvaluation1, policy4);

    // create policy violations app 2
    tempEntity.newPolicyViolation(policyEvaluation2, policy5);
    tempEntity.newPolicyViolation(policyEvaluation2, policy6);

    RepositoryPolicyEvaluationSummary policyEvaluationSummary = getRepositoryService()
        .getPolicyEvaluationSummary(repositoryManager.getInstanceId(), repository.getPublicId(), null);

    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(4);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(2);
    assertThat(policyEvaluationSummary.getReportUrl())
        .isEqualTo("ui/links/repository/" + repository.getId() + "/result");
    assertThat(policyEvaluationSummary.getQuarantinedComponentCount()).isEqualTo(1);
  }

  @Test
  public void testGetPolicyEvaluationSummary_NotProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().getPolicyEvaluationSummary(repoManager.getInstanceId(), repo.getPublicId(), null);
    }).withMessage("Repository " + repo.getPublicId() + " (" + repo.getId() + ") is not a proxy repository");
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
        .getPolicyEvaluationSummary(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null);
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
        .getPolicyEvaluationSummary(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null);
    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(1);
  }

  @Test
  public void testGetPolicyEvaluationSummary_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(
            () -> getRepositoryService().getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null))
        .withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testGetRepositoryResultsUrl() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    String repositoryResultsUrl = getRepositoryService()
        .getRepositoryResultsUrl(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null);
    assertThat(repositoryResultsUrl).isEqualTo("ui/links/repository/" + repository.getId() + "/result");
  }

  @Test
  public void testGetRepositoryResultsUrl_NotProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().getRepositoryResultsUrl(repoManager.getInstanceId(), repo.getPublicId(), null);
    }).withMessage("Repository " + repo.getPublicId() + " (" + repo.getId() + ") is not a proxy repository");
  }

  @Test
  public void testGetRepositoryResultsUrl_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(
            () -> getRepositoryService().getRepositoryResultsUrl(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null))
        .withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_RepositoryDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> getRepositoryService().evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null, true, null))
        .withMessage(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID));
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_NullRequest() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataList componentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null, true, null);
    assertThat(componentEvaluationResultList.componentEvalResults).isEmpty();
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_EmptyPathname() {
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

    RepositoryComponentEvaluationDataList result = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);

    // Verify empty result since the only component was invalid
    assertThat(result).isNotNull();
    assertThat(result.componentEvalResults).isEmpty();
    assertThat(componentEvaluationDataRequestList.components)
        .as("Invalid component should be filtered out")
        .isEmpty();
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_EmptyHash() {
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

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService()
            .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null))
        .withMessage("The hash cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_WithQuarantine() {
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
    assertThat(repository.isAuditEnabled()).isTrue();
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
        .getByRepositoryIdAndPathname(repository.getId(), pathname)
        .get(0);
    assertPolicyViolation(repository.getId(), pathname, policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolation);
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_QuarantineAndUnquarantineComponent() {
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

    // evaluate
    Date timeBeforeEvaluation1 = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    Date timeAfterEvaluation1 = new Date();
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex).isEqualTo(0);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isTrue();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);

    RepositoryComponent repositoryComponent = repositoryComponentDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname);
    assertRepositoryComponent(repository.getId(), pathname, timeBeforeEvaluation1, timeAfterEvaluation1, hash,
        componentIdentifier, MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), timeBeforeEvaluation1,
        timeAfterEvaluation1, timeAfterEvaluation1, repositoryComponent);
    assertThat(repositoryComponent.isQuarantined()).isTrue();

    RepositoryPolicyViolation policyViolation = repositoryPolicyViolationDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname)
        .get(0);
    assertPolicyViolation(repository.getId(), pathname, policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, timeBeforeEvaluation1, timeAfterEvaluation1,
        policyViolation);

    // prepare a hds request with no violations
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // evaluate and confirm unquarantined state
    Date timeBeforeEvaluation2 = new Date();
    repositoryComponentEvaluationResultList = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    Date timeAfterEvaluation2 = new Date();
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex).isEqualTo(0);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isFalse();

    List<RepositoryPolicyViolation> currentRepositoryPolicyViolations = repositoryPolicyViolationDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname);
    assertThat(currentRepositoryPolicyViolations).isEmpty();
    repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), pathname);
    assertThat(repositoryComponent.isQuarantined()).isFalse();
    assertThat(repositoryComponent.getQuarantineTime()).isBetween(timeBeforeEvaluation1, timeAfterEvaluation1, true,
        true);
    assertThat(repositoryComponent.getUnquarantineTime()).isBetween(timeBeforeEvaluation2, timeAfterEvaluation2, true,
        true);
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_NotQuarantinedUnchangedComponentRemainsNotQuarantined() {
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
        .getByRepositoryIdAndPathname(repository.getId(), pathname)
        .get(0);
    assertPolicyViolation(repository.getId(), pathname, policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, timeBeforeEvaluation, after, policyViolation);
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_PathnameSlashPrefix() {
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
    assertThat(repository.isAuditEnabled()).isTrue();
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
        .getByRepositoryIdAndPathname(repository.getId(), pathname)
        .get(0);
    assertPolicyViolation(repository.getId(), pathname, policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolation);
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_NoViolations() {
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
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);

    RepositoryComponent repositoryComponent = repositoryComponentDAO
        .getByRepositoryIdAndPathname(repository.getId(), pathname);
    assertRepositoryComponent(repository.getId(), pathname, before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_Waived() {
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
    assertThat(repository.isAuditEnabled()).isTrue();
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
  public void testEvaluateComponents_WithQuarantine_QuarantineRequestAfterAuditWithoutExplicitRemoval() {
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
    assertThat(repository.isAuditEnabled()).isTrue();
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
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();

    repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    assertThat(repositoryComponents.get(0).getPathname()).isEqualTo(pathname);
    assertThat(repositoryComponents.get(0).isQuarantined()).isFalse();
    assertThat(repositoryComponents.get(0).getQuarantineTime()).isNull();
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_QuarantineRequestAfterUnquarantineWithoutExplicitRemoval() {
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
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();

    repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    repositoryComponent = repositoryComponents.get(0);
    assertThat(repositoryComponent.getPathname()).isEqualTo(pathname);
    assertThat(repositoryComponent.getQuarantineTime()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    assertThat(repositoryComponent.isQuarantined()).isFalse();
  }

  @Test
  public void testEvaluateComponents_RepositoryDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null /* componentEvaluationDataRequestList */,
            false, null))
        .withMessage(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID));
  }

  @Test
  public void testEvaluateComponents_ExistingRepository_NotEnabled() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false, false);

    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null /* componentEvaluationDataRequestList */, false,
            null);

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isFalse();
  }

  @Test
  public void testEvaluateComponents_ExistingRepository_QuarantineNotEnabled() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, false);

    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null /* componentEvaluationDataRequestList */, true,
            null);

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();
  }

  @Test
  public void testEvaluateComponents_ExistingRepository_RepositoryAndQuarantineNotEnabled() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false, false);

    getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null /* componentEvaluationDataRequestList */, true,
            null);

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();
  }

  @Test
  public void testEvaluateComponents_MultipleComponents() {
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
    assertThat(repository.isAuditEnabled()).isTrue();

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
          .getByRepositoryIdAndPathname(repository.getId(), pathname)
          .get(0);
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

    List<Message> notificationsUser1 = MailboxTestUtil.get(user1EmailAddress);
    notificationsUser1.clear();
    List<Message> notificationsUser2 = MailboxTestUtil.get(user2EmailAddress);
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
  public void testEvaluateComponents_NotificationFailuresDoNotFailTheEvaluation() {
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
    mailConfigurationDAO.delete();

    // Call the service
    getRepositoryService().evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false, null);

    await().atMost(Duration.ofMillis(5000))
        .untilAsserted(() -> assertThat(emailerLogOutput).atErrorLevel()
            .contains(
                "Unable to send notification email to " + userEmailAddress + " for repository "
                    + repository.getPublicId()));
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

    List<Message> notifications = MailboxTestUtil.get(userEmailAddress);
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
  public void testEvaluateComponents_Reevaluation() {
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
  public void testEvaluateComponents_LicenseOverridden() {
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
  public void testEvaluateComponents_SecurityVulnerabilityOverridden() {
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
  public void testEvaluateComponents_ClaimedComponent() {
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
  public void testEvaluateComponents_NotProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().evaluateComponents(repoManager.getInstanceId(), repo.getPublicId(),
          componentEvaluationDataRequestList, false, null);
    }).withMessage("Repository " + repo.getPublicId() + " (" + repo.getId() + ") is not a proxy repository");
  }

  @Test
  public void testEvaluateComponents_LongHash() {
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
  public void testEvaluateComponents_UnknownComponent() {
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
  public void testEvaluateComponents_pathnameSlashPrefix() {
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
  public void testEvaluateComponents_NullPathname() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", null, "hash"));

    RepositoryComponentEvaluationDataList result = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);

    // Verify empty result since the only component was invalid
    assertThat(result).isNotNull();
    assertThat(result.componentEvalResults).isEmpty();
    assertThat(componentEvaluationDataRequestList.components)
        .as("Invalid component should be filtered out")
        .isEmpty();
  }

  @Test
  public void testEvaluateComponents_EmptyPathname() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", " ", "hash"));

    RepositoryComponentEvaluationDataList result = getRepositoryService()
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, false, null);

    // Verify empty result since the only component was invalid
    assertThat(result).isNotNull();
    assertThat(result.componentEvalResults).isEmpty();
    assertThat(componentEvaluationDataRequestList.components)
        .as("Invalid component should be filtered out")
        .isEmpty();
  }

  @Test
  public void testEvaluateComponents_NullFormat() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest(null, "pathname", "hash"));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, false, null))
        .withMessage("The format cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_EmptyFormat() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest(" ", "pathname", "hash"));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, false, null))
        .withMessage("The format cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_NullHash() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    String hash = null;

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path", hash));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, false, null))
        .withMessage("The hash cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_EmptyHash() {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path", " "));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, false, null))
        .withMessage("The hash cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(
            () -> getRepositoryService().evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null, false, null))
        .withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
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

  void mockHdsRequestForMetadata(ComponentEvaluationDataList hdsResult) {
    doReturn(hdsResult).when(quarantineHdsClient)
        .get( //
            eq(ComponentEvaluationDataList.class), //
            eq(AbstractRepositoryService.HDS_COMPONENT_METADATA_PATH), //
            anyString(), //
            anyMap());
  }

  void mockHdsRequestForMetadataWithoutUserAgent(ComponentEvaluationDataList hdsResult) {
    doReturn(hdsResult).when(quarantineHdsClient)
        .get(eq(ComponentEvaluationDataList.class),
            eq(AbstractRepositoryService.HDS_COMPONENT_METADATA_PATH), isNull(), anyMap());
  }

  protected ComponentEvaluationData createComponentEvaluationData(
      ComponentIdentifier componentIdentifier,
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

  protected ComponentEvaluationData createComponentEvaluationData(
      ComponentIdentifier componentIdentifier,
      String hash,
      MatchState matchState,
      int index,
      String filename,
      Set<License> declaredLicenses,
      Set<License> observedLicenses,
      List<SecurityVulnerability> securityVulnerabilities,
      Integer relativePopularity)
  {
    ComponentEvaluationData componentEvaluationData = createComponentEvaluationData(componentIdentifier, hash,
        matchState, index, declaredLicenses, observedLicenses, securityVulnerabilities, relativePopularity);
    componentEvaluationData.filename = filename;

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

  private void assertRepositoryComponent(
      String repositoryId,
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

  private void assertRepositoryComponent(
      String repositoryId,
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

  private void assertPolicyViolation(
      String repositoryId,
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
  public void testRemoveComponent_RepositoryDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> getRepositoryService().removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "somepath", null))
        .withMessage(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID));
    verifyNoInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testRemoveComponent_RepositoryNotEnabled() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false /* enabled */);

    getRepositoryService().removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "somepath", null);

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isAuditEnabled()).isTrue();
    verifyNoInteractions(repositoryComponentTelemetryCreator);
  }

  @Test
  public void testRemoveComponent() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);
    String pathname1 = "pathname1";
    String pathname2 = "pathname2";
    RepositoryComponent repositoryComponent1 = tempEntity.newRepositoryComponent(repository.getId(), pathname1);
    RepositoryComponent repositoryComponent2 = tempEntity.newRepositoryComponent(repository.getId(), pathname2);
    RepositoryPolicyViolation policyViolation1 = tempEntity.newRepositoryPolicyViolation(repository.getId(), pathname1);
    RepositoryPolicyViolation policyViolation2 = tempEntity.newRepositoryPolicyViolation(repository.getId(), pathname2);

    getRepositoryService().removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, pathname1, null);

    assertThat(repositoryComponentDAO.getById(repositoryComponent1.getId())).isNull();
    assertThat(repositoryComponentDAO.getById(repositoryComponent2.getId())).isNotNull();
    assertThat(repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), pathname1)).isNull();
    policyViolation1 = repositoryPolicyViolationDAO.getById(policyViolation1.getId());
    assertThat(policyViolation1).isNull();
    policyViolation2 = repositoryPolicyViolationDAO.getById(policyViolation2.getId());
    assertThat(policyViolation2).isNotNull();

    verify(repositoryComponentTelemetryCreator)
        .sendRepositoryComponentTelemetry(any(), any(), eq(repositoryManager.getId()),
            eq(RepositoryComponentTelemetryEventType.DELETE));
  }

  @Test
  public void testRemoveComponent_NotProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().removeComponent(repoManager.getInstanceId(), repo.getPublicId(), "testpathname", null);
    }).withMessage("Repository " + repo.getPublicId() + " (" + repo.getId() + ") is not a proxy repository");
  }

  @Test
  public void testRemoveComponent_pathnameSlashPrefix() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);
    String pathname1 = "pathname1";
    String pathname2 = "pathname2";
    RepositoryComponent repositoryComponent1 = tempEntity.newRepositoryComponent(repository.getId(), pathname1);
    RepositoryComponent repositoryComponent2 = tempEntity.newRepositoryComponent(repository.getId(), pathname2);
    RepositoryPolicyViolation policyViolation1 = tempEntity.newRepositoryPolicyViolation(repository.getId(), pathname1);
    RepositoryPolicyViolation policyViolation2 = tempEntity.newRepositoryPolicyViolation(repository.getId(), pathname2);

    getRepositoryService().removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "/" + pathname1, null);

    assertThat(repositoryComponentDAO.getById(repositoryComponent1.getId())).isNull();
    assertThat(repositoryComponentDAO.getById(repositoryComponent2.getId())).isNotNull();
    assertThat(repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), pathname1)).isNull();
    policyViolation1 = repositoryPolicyViolationDAO.getById(policyViolation1.getId());
    assertThat(policyViolation1).isNull();
    policyViolation2 = repositoryPolicyViolationDAO.getById(policyViolation2.getId());
    assertThat(policyViolation2).isNotNull();

    verify(repositoryComponentTelemetryCreator)
        .sendRepositoryComponentTelemetry(any(), any(), eq(repositoryManager.getId()),
            eq(RepositoryComponentTelemetryEventType.DELETE));
  }

  @Test
  public void testRemoveComponent_containerImageApplication() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);
    repository.setRepositoryType(RepositoryType.proxy);
    repository.setFormat("docker");

    String pathname1 = "pathname1";
    String pathname2 = "pathname2";
    Application application1 = tempEntity.newApplicationWithParent(pathname1);
    Application application2 = tempEntity.newApplicationWithParent(pathname2);

    repository.setRelatedOrganizationId(application1.getOrganizationId());
    repositoryDAO.update(repository);

    getRepositoryService().removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, pathname1, null);

    assertThat(applicationDAO.getById(application1.getId())).isNull();
    assertThat(applicationDAO.getById(application2.getId())).isNotNull();
  }

  private Policy createQuarantiningPolicy(Repository repository) {
    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);
    return policy;
  }

  @Test
  public void testGetUnquarantinedComponents() {
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
        .getUnquarantinedComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, since, null);
    assertThat(result.pathnames).containsExactly("pathnameUnquarantinedAfter");
  }

  @Test
  public void testGetUnquarantinedComponents_NotProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().getUnquarantinedComponents(repoManager.getInstanceId(), repo.getPublicId(), 0, null);
    }).withMessage("Repository " + repo.getPublicId() + " (" + repo.getId() + ") is not a proxy repository");
  }

  @Test
  public void testGetUnquarantinedComponents_RepositoryDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> getRepositoryService().getUnquarantinedComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, 0, null))
        .withMessage(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID));
  }

  @Test
  public void testRemoveComponent_DeletesPolicyViolations() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), "pathname", new Date() /* quarantineTime */,
            null /* unquarantineTime */);
    RepositoryPolicyViolation policyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId(), "pathname");

    getRepositoryService().removeComponent(repository, repositoryComponent.getPathname());

    policyViolation = repositoryPolicyViolationDAO.getById(policyViolation.getId());
    assertThat(policyViolation).isNull();

    verify(repositoryComponentTelemetryCreator)
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(RepositoryComponentTelemetryEventType.DELETE));
  }

  @Test
  public void testRemoveComponent_PolicyViolationLogger_LogsFixEventForEachDeletedViolation() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
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
    policyViolationLogDTOAssert
        .assertRepositoryPolicyViolationData(policyViolationLogDTOs, PolicyViolationLogEvent.FIX, repository, before,
            after, Arrays.asList(activeRepositoryPolicyViolation1, activeRepositoryPolicyViolation2),
            currentUser.getUsernameOrSystem());

    verify(repositoryComponentTelemetryCreator)
        .sendRepositoryComponentTelemetry(any(), any(), eq(repository.getRepositoryManagerId()),
            eq(RepositoryComponentTelemetryEventType.DELETE));
  }

  @Test
  public void testSetAuditEnabled_PolicyViolationLogger_DisabledLogsClearEvent() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    Date before = new Date();
    getRepositoryService().setAuditEnabled(REPO_MAN_INSTANCE_ID, repository.getPublicId(), false, null);
    Date after = new Date();

    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(policyViolationLoggerOutput, 1);
    policyViolationLogDTOAssert
        .assertRepositoryPolicyViolationData(policyViolationLogDTOs.get(0), PolicyViolationLogEvent.CLEAR, repository,
            before, after);
  }

  @Test
  public void testSetAuditEnabled_PolicyViolationLogger_EnabledDoesNotLogClearEvent() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    getRepositoryService().setAuditEnabled(REPO_MAN_INSTANCE_ID, repository.getPublicId(), true, null);

    assertThat(policyViolationLoggerOutput.getInfoMessages(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME))
        .isEmpty();
  }

  @Test
  public void testAddProprietaryNamespaceNames() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    getRepositoryService().addProprietaryNamespaceNames(repoManager.getInstanceId(), repo.getPublicId(),
        "npm", List.of("@sonatype"));

    List<ProprietaryComponentNamePattern> patterns =
        proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM);
    assertThat(patterns).allSatisfy(pattern -> {
      assertThat(pattern.getFormat()).isEqualTo(ComponentIdentifier.FORMAT_NPM);
      assertThat(pattern.getRepositoryId()).isEqualTo(repo.getId());
    })
        .extracting(ProprietaryComponentNamePattern::getNamespacePattern,
            ProprietaryComponentNamePattern::getNamePattern)
        .containsExactly(tuple("@sonatype", null));
  }

  @Test
  public void testAddProprietaryNamespaceNames_ValidatesNamespacesPresent() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().addProprietaryNamespaceNames(repoManager.getInstanceId(), repo.getPublicId(),
          "npm", List.of());
    }).withMessage("namespaces must be provided.");
  }

  @Test
  public void testAddProprietaryNamespaceNames_ValidatesFormatPresent() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().addProprietaryNamespaceNames(repoManager.getInstanceId(), repo.getPublicId(),
          null, List.of("@sonatype"));
    }).withMessage("format must be provided.");
  }

  @Test
  public void testAddProprietaryNamespaceNames_ValidatesFormatIsValid() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().addProprietaryNamespaceNames(repoManager.getInstanceId(), repo.getPublicId(),
          "notnpm", List.of("@sonatype"));
    }).withMessage("'notnpm' format is not supported.");
  }

  @Test
  public void testAddProprietaryComponentNames() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    ProprietaryComponentNames proprietaryComponentNames =
        new ProprietaryComponentNames("npm").addNames("sonatype*").addNamespaces("@sonatype");

    getRepositoryService().addProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId(),
        proprietaryComponentNames);

    List<ProprietaryComponentNamePattern> patterns =
        proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM);
    assertThat(patterns).allSatisfy(pattern -> {
      assertThat(pattern.getFormat()).isEqualTo(ComponentIdentifier.FORMAT_NPM);
      assertThat(pattern.getRepositoryId()).isEqualTo(repo.getId());
    })
        .extracting(ProprietaryComponentNamePattern::getNamespacePattern,
            ProprietaryComponentNamePattern::getNamePattern)
        .containsExactlyInAnyOrder(tuple("@sonatype", null), tuple(null, "sonatype*"));
  }

  @Test
  public void testAddProprietaryComponentNames_NotHostedRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.proxy, ComponentIdentifier.FORMAT_NPM);
    ProprietaryComponentNames proprietaryComponentNames =
        new ProprietaryComponentNames("npm").addNames("sonatype*").addNamespaces("@sonatype");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().addProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId(),
          proprietaryComponentNames);
    }).withMessage("Repository " + repo.getPublicId() + " (" + repo.getId() + ") is not a hosted repository");
  }

  @Test
  public void testAddProprietaryComponentNames_DoesNotChangePatternEnabledStatus() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    String namespacePattern = "testNamespacePattern";
    String namePattern = "testNamePattern";
    // Existing disabled patterns
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(repo,
        null /* namespacePattern */, namePattern, false /* enabled */);
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(repo, namespacePattern,
        null /* namePattern */, false /* enabled */);
    assertThat(proprietaryComponentNamePatternDAO.getEnabledByFormat(ComponentIdentifier.FORMAT_NPM)).isEmpty();

    // Add the existing patterns
    ProprietaryComponentNames proprietaryComponentNames =
        new ProprietaryComponentNames(ComponentIdentifier.FORMAT_NPM).addNames(namePattern)
            .addNamespaces(namespacePattern);
    getRepositoryService().addProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId(),
        proprietaryComponentNames);

    pattern1 = proprietaryComponentNamePatternDAO.getById(pattern1.getId());
    assertThat(pattern1.isEnabled()).isFalse();
    pattern2 = proprietaryComponentNamePatternDAO.getById(pattern2.getId());
    assertThat(pattern2.isEnabled()).isFalse();
    assertThat(proprietaryComponentNamePatternDAO.getByFormat(ComponentIdentifier.FORMAT_NPM)).hasSize(2);
  }

  @Test
  public void testAddProprietaryComponentNames_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(
            () -> getRepositoryService().addProprietaryComponentNames(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
                new ProprietaryComponentNames()))
        .withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testAddProprietaryComponentNames_RepositoryDoesNotExist() {
    String repoManagerInstanceId = tempEntity.newRepositoryManager().getInstanceId();
    String repoPublicId = "hosted-repo";
    ProprietaryComponentNames proprietaryComponentNames =
        new ProprietaryComponentNames(ComponentIdentifier.FORMAT_NPM, "name");

    getRepositoryService().addProprietaryComponentNames(repoManagerInstanceId, repoPublicId, proprietaryComponentNames);

    Repository repo = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repoManagerInstanceId, repoPublicId);
    assertThat(repo.getRepositoryType()).isEqualTo(RepositoryType.hosted);
    assertThat(repo.getFormat()).isEqualTo(ComponentIdentifier.FORMAT_NPM);
  }

  @Test
  public void testAddProprietaryComponentNames_RepositoryManagerDoesNotExist() {
    String repoManagerInstanceId = MANUAL_REPO_MAN_INSTANCE_ID;
    String repoPublicId = "hosted-repo";
    ProprietaryComponentNames proprietaryComponentNames =
        new ProprietaryComponentNames(ComponentIdentifier.FORMAT_NPM, "name");

    getRepositoryService().addProprietaryComponentNames(repoManagerInstanceId, repoPublicId, proprietaryComponentNames);

    RepositoryManager repoManager = repositoryManagerDAO.getByInstanceId(repoManagerInstanceId);
    assertThat(repoManager).isNotNull();
    Repository repo = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repoManagerInstanceId, repoPublicId);
    assertThat(repo.getRepositoryType()).isEqualTo(RepositoryType.hosted);
    assertThat(repo.getFormat()).isEqualTo(ComponentIdentifier.FORMAT_NPM);
  }

  @Test
  public void testAddProprietaryComponentNames_NullDto() {
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().addProprietaryComponentNames(repoManId, repoId, null))
        .withMessageContaining("No component name patterns specified");
  }

  @Test
  public void testAddProprietaryComponentNames_NoPatterns() {
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";
    ProprietaryComponentNames proprietaryComponentNames = new ProprietaryComponentNames("npm");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> getRepositoryService().addProprietaryComponentNames(repoManId, repoId, proprietaryComponentNames))
        .withMessageContaining("No component name patterns specified");
  }

  @Test
  public void testAddProprietaryComponentNames_NoFormat() {
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";
    ProprietaryComponentNames proprietaryComponentNames = new ProprietaryComponentNames(null, "name");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> getRepositoryService().addProprietaryComponentNames(repoManId, repoId, proprietaryComponentNames))
        .withMessageContaining("No component format specified");
  }

  @Test
  public void testAddProprietaryComponentNames_BadPattern() {
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().addProprietaryComponentNames(repoManId, repoId,
            new ProprietaryComponentNames("npm").addNames("")))
        .withMessageContaining("Empty component name pattern");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().addProprietaryComponentNames(repoManId, repoId,
            new ProprietaryComponentNames("npm").addNames("*")))
        .withMessageContaining("Invalid component name pattern: *");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().addProprietaryComponentNames(repoManId, repoId,
            new ProprietaryComponentNames("npm").addNamespaces("foo*bar")))
        .withMessageContaining("Invalid component namespace pattern: foo*bar");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().addProprietaryComponentNames(repoManId, repoId,
            new ProprietaryComponentNames("npm").addNamespaces("*foo*")))
        .withMessageContaining("Invalid component namespace pattern: *foo*");
  }

  @Test
  public void testAddProprietaryComponentNames_TooLongNameSpace() {
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";
    String namespace = "a".repeat(301);
    ProprietaryComponentNames proprietaryComponentNames =
        new ProprietaryComponentNames("npm").addNames("sonatype*").addNamespaces(namespace);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> getRepositoryService().addProprietaryComponentNames(repoManId, repoId, proprietaryComponentNames))
        .withMessageContaining("Component " + namespace + " is too long. Maximum length is 300 characters.");
  }

  @Test
  public void testAddProprietaryComponentNames_TooLongName() {
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";
    String name = "a".repeat(301);
    ProprietaryComponentNames proprietaryComponentNames =
        new ProprietaryComponentNames("npm").addNames(name).addNamespaces("@sonatype");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> getRepositoryService().addProprietaryComponentNames(repoManId, repoId, proprietaryComponentNames))
        .withMessageContaining("Component " + name + " is too long. Maximum length is 300 characters.");
  }

  @Test
  public void testAddProprietaryComponentNames_ProprietaryComponentNamesEmptyOrNull() {
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> getRepositoryService().addProprietaryComponentNames(repoManId, repoId, null))
        .withMessageContaining("No component name patterns specified");

    ProprietaryComponentNames proprietaryComponentNames = new ProprietaryComponentNames("format");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> getRepositoryService().addProprietaryComponentNames(repoManId, repoId, proprietaryComponentNames))
        .withMessageContaining("No component name patterns specified");
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

  @Test
  public void testAddProprietaryComponentNames_FormatTranslation_Yum() {
    testAddProprietaryComponentNames_FormatTranslation("yum", "rpm");
  }

  @Test
  public void testGetQuarantinedComponentReportUrl() {
    // setup
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    final Repository repository = tempEntity.newRepository(repositoryManager, "repo");
    tempEntity.newRepositoryComponent(repository.getId());

    when(quarantinedComponentAccessManager.createToken(any())).thenReturn("token");

    // when
    final QuarantinedComponentReport quarantinedComponentReport = getRepositoryService()
        .getQuarantinedComponentReportUrl(repositoryManager.getInstanceId(), repository.getPublicId(), "path", null);

    // then
    assertThat(quarantinedComponentReport.getReportUrl())
        .isEqualTo("ui/links/firewall/repositories/quarantinedComponent/token");
  }

  @Test
  public void testGetQuarantinedComponentReportUrl_NotProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().getQuarantinedComponentReportUrl(repoManager.getInstanceId(), repo.getPublicId(),
          "testpathname", null);
    }).withMessage("Repository " + repo.getPublicId() + " (" + repo.getId() + ") is not a proxy repository");
  }

  @Test
  public void testGetQuarantinedComponentReportUrl_ComponentNotExists() {
    // setup
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    final Repository repository = tempEntity.newRepository(repositoryManager, "repo");

    // when
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> getRepositoryService()
        .getQuarantinedComponentReportUrl(repositoryManager.getInstanceId(), repository.getPublicId(), "", null));
  }

  @Test
  public void testGetQuarantinedComponentReportUrl_RepositoryNotExists() {
    // when
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> getRepositoryService()
        .getQuarantinedComponentReportUrl("repmanid", "repid", "", null));
  }

  public void testEvaluateComponentMetadata_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(
            () -> getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null, null))
        .withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
    verify(requestSafeComponentsMetricEventServiceMock, never()).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_ClaimedComponent() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    // Create a policy that fails for claimed components
    Condition condition =
        new Condition(IdentificationSourceConditionType.ID, "is", IdentificationSource.MANUAL.getId());
    Constraint constraint = new Constraint("id", "name", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy = new Policy("id", "name");
    policy.setOwnerId(repository.getParentOwnerId());
    policy.addConstraint(constraint);
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    tempEntity.newPolicy(policy);

    ComponentIdentifier claimedComponentIdentifier =
        ComponentIdentifier.createNpmCoordinates("claimedPackageId", "claimedVersion");
    tempEntity.newClaimedComponent("testHash", claimedComponentIdentifier);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("testPackageId", "testVersion");
    componentEvaluationDataRequestList.cause = "metadata";
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("npm",
        "/testPackageId/-/testPackageId-testVersion.tgz", "testHash"));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, null, MatchState.EXACT, 0 /* index */,
        null /* declaredLicenseSet */, null /* observedLicenseSet */, null /* securityVulnerabilities */,
        0 /* popularity */));
    mockHdsRequestForMetadata(hdsResult);

    // Call the service
    long start = System.currentTimeMillis();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = getRepositoryService()
        .evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
            getUserAgent());

    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    RepositoryComponentEvaluationData repositoryComponentEvaluationData =
        repositoryComponentEvaluationResultList.componentEvalResults.get(0);
    assertThat(repositoryComponentEvaluationData.requestIndex).isEqualTo(0);
    assertThat(repositoryComponentEvaluationData.quarantine).isTrue();

    assertThat(repositoryComponentDAO.getByRepositoryId(repository.getId())).isEmpty();
    assertTelemetry(componentEvaluationDataRequestList.components.size(), 0, System.currentTimeMillis() - start);
    verify(requestSafeComponentsMetricEventServiceMock).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_EmptyFormat() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = "metadata";
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest(" " /* format */, "pathname", "hash"));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null))
        .withMessage("The format cannot be null or empty.");
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
    verify(requestSafeComponentsMetricEventServiceMock, never()).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_NullFormat() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = "metadata";
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest(null /* format */, "pathname", "hash"));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null))
        .withMessage("The format cannot be null or empty.");
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
    verify(requestSafeComponentsMetricEventServiceMock, never()).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_NotProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = "metadata";
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest(null /* format */, "pathname", "hash"));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().evaluateComponentMetadata(repoManager.getInstanceId(), repo.getPublicId(),
          componentEvaluationDataRequestList, null);
    }).withMessage("Repository " + repo.getPublicId() + " (" + repo.getId() + ") is not a proxy repository");

    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
    verify(requestSafeComponentsMetricEventServiceMock, never()).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_EmptyHash() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = "metadata";
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("npm", "path", " " /* hash */));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null))
        .withMessage("The hash cannot be null or empty.");
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
    verify(requestSafeComponentsMetricEventServiceMock, never()).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_NullHash_npm() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = "metadata";
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("npm", "path", null /* hash */));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null))
        .withMessage("The hash cannot be null or empty.");
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
    verify(requestSafeComponentsMetricEventServiceMock, never()).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_EmptyPathname() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = "metadata";
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("npm", " "/* pathname */, "hash"));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null))
        .withMessage("The pathname cannot be null or empty.");
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
    verify(requestSafeComponentsMetricEventServiceMock, never()).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_NullPathname() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = "metadata";
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("npm", null /* pathname */, "hash"));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null))
        .withMessage("The pathname cannot be null or empty.");
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
    verify(requestSafeComponentsMetricEventServiceMock, never()).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_ExistingRepository_NotEnabled() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false, false);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            null /* componentEvaluationDataRequestList */, null))
        .withMessage("The repository must be enabled in quarantine mode.");
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
    verify(requestSafeComponentsMetricEventServiceMock, never()).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_ExistingRepository_QuarantineNotEnabled() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, false);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            null /* componentEvaluationDataRequestList */, null))
        .withMessage("The repository must be enabled in quarantine mode.");
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
    verify(requestSafeComponentsMetricEventServiceMock, never()).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_FormatIsNpmOrPypi() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.cause = "metadata";
    componentEvaluationDataRequestList.components
        .add(new RepositoryComponentEvaluationDataRequest("maven2", "path", "hash"));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, null))
        .withMessage("The repository format must be npm or pypi.");
    verify(telemetrySenderMock, never()).send(any(TelemetryData.class));
    verify(requestSafeComponentsMetricEventServiceMock, never()).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_MultipleComponents_npm() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    int componentCount = 2;
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    String packageId = "testPackageId";
    for (int i = 0; i < componentCount; i++) {
      String version = "testVersion" + i;
      String hash = "testHash" + i;
      ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates(packageId, version);
      String filename = packageId + "-" + version + ".tgz";
      componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("npm",
          "/" + packageId + "/-/" + filename, hash));
      List<SecurityVulnerability> securityVulnerabilities = null;
      // Add security vulnerabilities only to the first version/component,
      // so only the first one should be quarantined.
      if (i == 0) {
        securityVulnerabilities = createSecurityVulnerabilities();
      }
      hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT,
          i /* index */, filename, null, null, securityVulnerabilities, 0 /* popularity */));
    }
    mockHdsRequestForMetadata(hdsResult);

    // Call the service
    long start = System.currentTimeMillis();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList =
        getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, getUserAgent());

    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(2);

    for (int i = 0; i < componentCount; i++) {
      RepositoryComponentEvaluationData repositoryComponentEvaluationData =
          repositoryComponentEvaluationResultList.componentEvalResults.get(i);
      assertThat(repositoryComponentEvaluationData.requestIndex).isEqualTo(i);
      if (i == 0) {
        assertThat(repositoryComponentEvaluationData.quarantine).isTrue();
      }
      else {
        assertThat(repositoryComponentEvaluationData.quarantine).isFalse();
      }
    }

    assertThat(repositoryComponentDAO.getByRepositoryId(repository.getId())).isEmpty();
    assertTelemetry(componentEvaluationDataRequestList.components.size(), 1, System.currentTimeMillis() - start);
    verify(requestSafeComponentsMetricEventServiceMock).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_MultipleComponents_PyPI() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    int componentCount = 2;
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    String packageName = "testName";
    for (int i = 0; i < componentCount; i++) {
      String version = "testVersion" + i;
      ComponentIdentifier componentIdentifier =
          ComponentIdentifier.createPypiCoordinates(packageName, version, "testQualifier", "whl");
      String filename = packageName + "-" + version + "-testQualifier.whl";
      // The hash is null for PyPI
      componentEvaluationDataRequestList.components
          .add(new RepositoryComponentEvaluationDataRequest(ComponentIdentifier.FORMAT_PYPI,
              "/" + packageName + "/" + version + "/" + filename, null /* hash */));
      List<SecurityVulnerability> securityVulnerabilities = null;
      // Add security vulnerabilities only to the first version/component,
      // so only the first one should be quarantined.
      if (i == 0) {
        securityVulnerabilities = createSecurityVulnerabilities();
      }
      hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "testHash" + i, MatchState.EXACT,
          i /* index */, filename, null, null, securityVulnerabilities, 0 /* popularity */));
    }
    mockHdsRequestForMetadata(hdsResult);

    // Call the service
    long start = System.currentTimeMillis();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList =
        getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, "testClientUserAgent");

    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(2);

    for (int i = 0; i < componentCount; i++) {
      RepositoryComponentEvaluationData repositoryComponentEvaluationData =
          repositoryComponentEvaluationResultList.componentEvalResults.get(i);
      assertThat(repositoryComponentEvaluationData.requestIndex).isEqualTo(i);
      if (i == 0) {
        assertThat(repositoryComponentEvaluationData.quarantine).isTrue();
      }
      else {
        assertThat(repositoryComponentEvaluationData.quarantine).isFalse();
      }
    }

    assertThat(repositoryComponentDAO.getByRepositoryId(repository.getId())).isEmpty();
    assertTelemetry(componentEvaluationDataRequestList.components.size(), 1, System.currentTimeMillis() - start);
    verify(requestSafeComponentsMetricEventServiceMock).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_PyPI_MultipleComponentsSameHashAndFilename() {
    // At least for PyPI, there are binaries with the same hash and filename published under different coordinates.
    // See https://sonatype.atlassian.net/browse/CLM-27246
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    int componentCount = 2;
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    String packageName = "testName";
    String filename = packageName + "-testQualifier.whl";
    String hash = "testHash";
    for (int i = 0; i < componentCount; i++) {
      String version = "testVersion" + i;
      ComponentIdentifier componentIdentifier =
          ComponentIdentifier.createPypiCoordinates(packageName, version, "testQualifier", "whl");
      List<SecurityVulnerability> securityVulnerabilities = null;
      securityVulnerabilities = createSecurityVulnerabilities();
      hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT,
          i /* index */, filename, null, null, securityVulnerabilities, 0 /* popularity */));
    }
    mockHdsRequestForMetadata(hdsResult);
    // The hash is null for PyPI
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest(
        ComponentIdentifier.FORMAT_PYPI, "/" + packageName + "/testVersion/" + filename, null /* hash */));

    // Call the service
    long start = System.currentTimeMillis();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList =
        getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, "testClientUserAgent");

    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);

    RepositoryComponentEvaluationData repositoryComponentEvaluationData =
        repositoryComponentEvaluationResultList.componentEvalResults.get(0);
    assertThat(repositoryComponentEvaluationData.requestIndex).isEqualTo(0);
    assertThat(repositoryComponentEvaluationData.quarantine).isTrue();

    assertThat(repositoryComponentDAO.getByRepositoryId(repository.getId())).isEmpty();
    assertTelemetry(componentEvaluationDataRequestList.components.size(), 0, System.currentTimeMillis() - start);
    verify(requestSafeComponentsMetricEventServiceMock).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_MultipleComponents_ScopedNpmComponents() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    int componentCount = 2;
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    String packageId = "@testScope/testPackageId";
    for (int i = 0; i < componentCount; i++) {
      String version = "testVersion" + i;
      String hash = "testHash" + i;
      ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates(packageId, version);
      String filename = "testPackageId-" + version + ".tgz";
      componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("npm",
          "/" + packageId + "/-/" + filename, hash));
      List<SecurityVulnerability> securityVulnerabilities = null;
      // Add security vulnerabilities only to the first version/component,
      // so only the first one should be quarantined.
      if (i == 0) {
        securityVulnerabilities = createSecurityVulnerabilities();
      }
      hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, i /* index */,
          filename, null, null, securityVulnerabilities, 0 /* popularity */));
    }
    mockHdsRequestForMetadata(hdsResult);

    // Call the service
    long start = System.currentTimeMillis();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList =
        getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, getUserAgent());

    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(2);

    for (int i = 0; i < componentCount; i++) {
      RepositoryComponentEvaluationData repositoryComponentEvaluationData =
          repositoryComponentEvaluationResultList.componentEvalResults.get(i);
      assertThat(repositoryComponentEvaluationData.requestIndex).isEqualTo(i);
      if (i == 0) {
        assertThat(repositoryComponentEvaluationData.quarantine).isTrue();
      }
      else {
        assertThat(repositoryComponentEvaluationData.quarantine).isFalse();
      }
    }

    assertThat(repositoryComponentDAO.getByRepositoryId(repository.getId())).isEmpty();
    assertTelemetry(componentEvaluationDataRequestList.components.size(), 1, System.currentTimeMillis() - start);
    verify(requestSafeComponentsMetricEventServiceMock).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testEvaluateComponentMetadata_UnknownComponent() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    Condition condition = new Condition(MatchStateConditionType.ID, "is", MatchState.UNKNOWN.getId());
    Constraint constraint = new Constraint("id", "name", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy = new Policy("id", "name");
    policy.setOwnerId(repository.getParentOwnerId());
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request.
    // HDS only knows about the first version, so the second version should be quarantined.
    int componentCount = 2;
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    String packageId = "testPackageId";
    String version = "testVersion";
    String hash = "testHash";
    String filename = packageId + "-" + version + ".tgz";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates(packageId, version);
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("npm",
        "/" + packageId + "/-/" + filename, hash));
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("npm",
        "/" + packageId + "/-/" + packageId + "-UnknownVersion.tgz", hash));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        filename, null, null, null, 0 /* popularity */));
    mockHdsRequestForMetadata(hdsResult);

    // Call the service
    long start = System.currentTimeMillis();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList =
        getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, getUserAgent());

    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(2);

    for (int i = 0; i < componentCount; i++) {
      RepositoryComponentEvaluationData repositoryComponentEvaluationData =
          repositoryComponentEvaluationResultList.componentEvalResults.get(i);
      assertThat(repositoryComponentEvaluationData.requestIndex).isEqualTo(i);
      if (i == 0) {
        assertThat(repositoryComponentEvaluationData.quarantine).isFalse();
      }
      else {
        assertThat(repositoryComponentEvaluationData.quarantine).isTrue();
      }
    }

    assertThat(repositoryComponentDAO.getByRepositoryId(repository.getId())).isEmpty();
    assertTelemetry(componentEvaluationDataRequestList.components.size(), 1, System.currentTimeMillis() - start);
    verify(requestSafeComponentsMetricEventServiceMock).postRequestSafeComponentsMetricEvent();
  }

  @Test
  public void testConfigureRepositories_NewRepositoryManager() {
    String clientUserAgent = getUserAgent();

    // Call the service
    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.emptyList());
    getRepositoryService().configureRepositories(MANUAL_REPO_MAN_INSTANCE_ID, configureRepositoriesRequest,
        clientUserAgent);

    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(MANUAL_REPO_MAN_INSTANCE_ID);
    assertThat(repositoryManager.getUserAgent()).isEqualTo(clientUserAgent);
    assertThat(repositoryManager.getProductName()).isEqualTo(configureRepositoriesRequest.repositoryManagerProductName);
    assertThat(repositoryManager.getProductVersion())
        .isEqualTo(configureRepositoriesRequest.repositoryManagerProductVersion);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());
    assertThat(repositories).isEmpty();
  }

  @Test
  public void testConfigureRepositories_ExistingRepositoryManager() {
    String clientUserAgent = getUserAgent();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    // Sanity checks
    assertThat(repositoryManager.getUserAgent()).isNull();
    assertThat(repositoryManager.getProductName()).isNull();
    assertThat(repositoryManager.getProductVersion()).isNull();

    // Call the service
    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.emptyList());
    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        clientUserAgent);

    repositoryManager = repositoryManagerDAO.getByInstanceId(repositoryManager.getInstanceId());
    assertThat(repositoryManager.getUserAgent()).isEqualTo(clientUserAgent);
    assertThat(repositoryManager.getProductName()).isEqualTo(configureRepositoriesRequest.repositoryManagerProductName);
    assertThat(repositoryManager.getProductVersion())
        .isEqualTo(configureRepositoriesRequest.repositoryManagerProductVersion);
  }

  @Test
  public void testConfigureRepositories_NewRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.name = "testRepoName";
    repositoryDTO.format = "npm";
    repositoryDTO.type = RepositoryType.hosted;
    repositoryDTO.auditEnabled = false;
    repositoryDTO.quarantineEnabled = false;
    repositoryDTO.policyCompliantComponentSelectionEnabled = false;
    repositoryDTO.namespaceConfusionProtectionEnabled = true;
    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.singletonList(repositoryDTO));

    // Call the service
    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        "testClientUserAgent");

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());
    assertThat(repositories).hasSize(1);
    Repository repository = repositories.get(0);
    assertThat(repository.getName()).isEqualTo("testRepoName");
    assertThat(repository.getFormat()).isEqualTo("npm");
    assertThat(repository.getRepositoryType()).isEqualTo(RepositoryType.hosted);
    assertThat(repository.isAuditEnabled()).isFalse();
    assertThat(repository.isQuarantineEnabled()).isFalse();
    assertThat(repository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(repository.isNamespaceConfusionProtectionEnabled()).isTrue();
  }

  @Test
  public void testConfigureRepositories_ExistingRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testRepoName");
    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.name = repository.getName();
    repositoryDTO.format = "npm";
    repositoryDTO.type = RepositoryType.proxy;
    repositoryDTO.auditEnabled = true;
    repositoryDTO.quarantineEnabled = true;
    repositoryDTO.policyCompliantComponentSelectionEnabled = true;
    repositoryDTO.namespaceConfusionProtectionEnabled = false;
    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.singletonList(repositoryDTO));

    // Call the service
    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        getUserAgent());

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());
    assertThat(repositories).hasSize(1);
    repository = repositories.get(0);
    assertThat(repository.getName()).isEqualTo("testRepoName");
    assertThat(repository.getFormat()).isEqualTo("npm");
    assertThat(repository.getRepositoryType()).isEqualTo(RepositoryType.proxy);
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();
    assertThat(repository.isPolicyCompliantComponentSelectionEnabled()).isTrue();
    assertThat(repository.isNamespaceConfusionProtectionEnabled()).isFalse();
  }

  @Test
  public void testConfigureRepositories_RepositoryWithErrorDoesNotStopProcessingOfOtherRepositories() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testRepoName");
    // Bad repository because it has a different type, so it cannot be processed
    RepositoryDTO badRepositoryDTO = new RepositoryDTO();
    badRepositoryDTO.name = repository.getName();
    badRepositoryDTO.format = "npm";
    badRepositoryDTO.type = RepositoryType.hosted;
    badRepositoryDTO.auditEnabled = true;
    badRepositoryDTO.quarantineEnabled = true;
    badRepositoryDTO.policyCompliantComponentSelectionEnabled = true;
    badRepositoryDTO.namespaceConfusionProtectionEnabled = true;
    RepositoryDTO goodRepositoryDTO = new RepositoryDTO();
    goodRepositoryDTO.name = "Good Repo";
    goodRepositoryDTO.format = "npm";
    goodRepositoryDTO.type = RepositoryType.proxy;
    goodRepositoryDTO.auditEnabled = true;
    goodRepositoryDTO.quarantineEnabled = true;
    goodRepositoryDTO.policyCompliantComponentSelectionEnabled = true;
    goodRepositoryDTO.namespaceConfusionProtectionEnabled = false;
    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Arrays.asList(badRepositoryDTO, goodRepositoryDTO));

    // Call the service
    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        getUserAgent());

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());
    assertThat(repositories).hasSize(2);

    await().atMost(Duration.ofMillis(5000))
        .untilAsserted(
            () -> assertThat(repositoryServiceLogOutput).atErrorLevel()
                .contains("Error updating repository " + repository.getName() + " (" + repository.getId()
                    + "): Cannot change the repository type."));

    Repository existingRepository =
        repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManager.getInstanceId(),
            repository.getName());
    assertThat(existingRepository.getId()).isEqualTo(repository.getId());
    assertThat(existingRepository.getFormat()).isEqualTo(repository.getFormat());
    assertThat(existingRepository.getRepositoryType()).isEqualTo(RepositoryType.proxy);
    assertThat(existingRepository.isAuditEnabled()).isTrue();
    assertThat(existingRepository.isQuarantineEnabled()).isFalse();
    assertThat(existingRepository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(existingRepository.isNamespaceConfusionProtectionEnabled()).isFalse();

    Repository newRepository =
        repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManager.getInstanceId(), "Good Repo");
    assertThat(newRepository.getFormat()).isEqualTo("npm");
    assertThat(newRepository.getRepositoryType()).isEqualTo(RepositoryType.proxy);
    assertThat(newRepository.isAuditEnabled()).isTrue();
    assertThat(newRepository.isQuarantineEnabled()).isTrue();
    assertThat(newRepository.isPolicyCompliantComponentSelectionEnabled()).isTrue();
    assertThat(newRepository.isNamespaceConfusionProtectionEnabled()).isFalse();
  }

  @Test
  public void testConfigureRepositories_CannotChangeRepositoryType() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testRepoName");
    // Bad repository because it has a different type, so it cannot be processed
    RepositoryDTO badRepositoryDTO = new RepositoryDTO();
    badRepositoryDTO.name = repository.getName();
    badRepositoryDTO.format = "npm";
    badRepositoryDTO.type = RepositoryType.hosted;
    badRepositoryDTO.auditEnabled = true;
    badRepositoryDTO.quarantineEnabled = true;
    badRepositoryDTO.policyCompliantComponentSelectionEnabled = true;
    badRepositoryDTO.namespaceConfusionProtectionEnabled = true;
    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.singletonList(badRepositoryDTO));

    // Call the service
    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        getUserAgent());

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());
    assertThat(repositories).hasSize(1);

    await().atMost(Duration.ofMillis(5000))
        .untilAsserted(
            () -> assertThat(repositoryServiceLogOutput).atErrorLevel()
                .contains("Error updating repository " + repository.getName() + " (" + repository.getId()
                    + "): Cannot change the repository type."));

    Repository existingRepository = repositoryDAO.getById(repository.getId());
    assertThat(existingRepository.getName()).isEqualTo(repository.getName());
    assertThat(existingRepository.getFormat()).isEqualTo(repository.getFormat());
    assertThat(existingRepository.getRepositoryType()).isEqualTo(RepositoryType.proxy);
    assertThat(existingRepository.isAuditEnabled()).isTrue();
    assertThat(existingRepository.isQuarantineEnabled()).isFalse();
    assertThat(existingRepository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(existingRepository.isNamespaceConfusionProtectionEnabled()).isFalse();
  }

  @Test
  public void testConfigureRepositories_CannotChangeRepositoryFormat() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testRepoName");
    repository.setFormat("npm");
    repositoryDAO.update(repository);
    // Bad repository because it has a different format, so it cannot be processed
    RepositoryDTO badRepositoryDTO = new RepositoryDTO();
    badRepositoryDTO.name = repository.getName();
    badRepositoryDTO.format = "testFormat";
    badRepositoryDTO.type = RepositoryType.proxy;
    badRepositoryDTO.auditEnabled = true;
    badRepositoryDTO.quarantineEnabled = true;
    badRepositoryDTO.policyCompliantComponentSelectionEnabled = true;
    badRepositoryDTO.namespaceConfusionProtectionEnabled = true;
    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.singletonList(badRepositoryDTO));

    // Call the service
    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        getUserAgent());

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());
    assertThat(repositories).hasSize(1);

    await().atMost(Duration.ofMillis(5000))
        .untilAsserted(() -> assertThat(repositoryServiceLogOutput).atErrorLevel()
            .contains("Error updating repository "
                + repository.getName() + " (" + repository.getId() + "): Cannot change the repository format."));

    Repository existingRepository = repositoryDAO.getById(repository.getId());
    assertThat(existingRepository.getName()).isEqualTo(repository.getName());
    assertThat(existingRepository.getFormat()).isEqualTo(repository.getFormat());
    assertThat(existingRepository.getRepositoryType()).isEqualTo(RepositoryType.proxy);
    assertThat(existingRepository.isAuditEnabled()).isTrue();
    assertThat(existingRepository.isQuarantineEnabled()).isFalse();
    assertThat(existingRepository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(existingRepository.isNamespaceConfusionProtectionEnabled()).isFalse();
  }

  @Test
  public void testConfigureRepositories_UpdatesRepositoryFormatIfFormatIsMissing() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testRepoName");
    assertThat(repository.getFormat()).isNull();
    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.name = repository.getName();
    repositoryDTO.format = ComponentIdentifier.FORMAT_NPM;
    repositoryDTO.type = RepositoryType.proxy;
    repositoryDTO.auditEnabled = true;
    repositoryDTO.quarantineEnabled = true;
    repositoryDTO.policyCompliantComponentSelectionEnabled = true;
    repositoryDTO.namespaceConfusionProtectionEnabled = false;
    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.singletonList(repositoryDTO));

    // Call the service
    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        getUserAgent());

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());
    assertThat(repositories).hasSize(1);

    Repository existingRepository = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicId(repositoryManager.getInstanceId(), repository.getName());
    assertThat(existingRepository.getId()).isEqualTo(repository.getId());
    assertThat(existingRepository.getFormat()).isEqualTo(repositoryDTO.format);
  }

  @Test
  public void testConfigureRepositories_Unlicensed() {
    testProductLicense.setMissingFeatures(getRepositoryService().requiredFeature);
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    // Call the service
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      getRepositoryService().configureRepositories(repositoryManager.getInstanceId(),
          null /* configureRepositoriesRequest */, getUserAgent());
    }).withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testConfigureRepositories_NullRequestParameter() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    // Call the service
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().configureRepositories(repositoryManager.getInstanceId(),
          null /* configureRepositoriesRequest */, getUserAgent());
    }).withMessage("The configureRepositoriesRequest parameter is required.");
  }

  @Test
  public void testConfigureRepositories_NullRepositoryManagerProductName() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.emptyList());
    configureRepositoriesRequest.repositoryManagerProductName = null;
    // Call the service
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
          getUserAgent());
    }).withMessage("The repositoryManagerProductName parameter is required.");
  }

  @Test
  public void testConfigureRepositories_EmptyRepositoryManagerProductName() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.emptyList());
    configureRepositoriesRequest.repositoryManagerProductName = " ";
    // Call the service
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
          getUserAgent());
    }).withMessage("The repositoryManagerProductName parameter is required.");
  }

  @Test
  public void testConfigureRepositories_NullRepositoryManagerProductVersion() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.emptyList());
    configureRepositoriesRequest.repositoryManagerProductVersion = null;
    // Call the service
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
          getUserAgent());
    }).withMessage("The repositoryManagerProductVersion parameter is required.");
  }

  @Test
  public void testConfigureRepositories_EmptyRepositoryManagerProductVersion() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.emptyList());
    configureRepositoriesRequest.repositoryManagerProductVersion = " ";
    // Call the service
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
          getUserAgent());
    }).withMessage("The repositoryManagerProductVersion parameter is required.");
  }

  @Test
  public void testConfigureRepositories_UpdatesRepositoryManagerProductNameAndVersion() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    // Sanity checks
    assertThat(repositoryManager.getProductName()).isNull();
    assertThat(repositoryManager.getProductVersion()).isNull();

    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.emptyList());
    // Call the service
    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        getUserAgent());
    repositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());
    assertThat(repositoryManager.getProductName()).isEqualTo(configureRepositoriesRequest.repositoryManagerProductName);
    assertThat(repositoryManager.getProductVersion())
        .isEqualTo(configureRepositoriesRequest.repositoryManagerProductVersion);

    // Change the product version
    configureRepositoriesRequest.repositoryManagerProductVersion =
        configureRepositoriesRequest.repositoryManagerProductVersion + "otherVersion";
    // Call the service
    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        getUserAgent());
    repositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());
    assertThat(repositoryManager.getProductName()).isEqualTo(configureRepositoriesRequest.repositoryManagerProductName);
    assertThat(repositoryManager.getProductVersion())
        .isEqualTo(configureRepositoriesRequest.repositoryManagerProductVersion);
  }

  @Test
  public void testConfigureRepositories_WithInstanceName_PersistsNameOnNewRepositoryManager() {
    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.emptyList());

    getRepositoryService().configureRepositories(MANUAL_REPO_MAN_INSTANCE_ID, configureRepositoriesRequest,
        getUserAgent(), "My NXRM Instance");

    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(MANUAL_REPO_MAN_INSTANCE_ID);
    assertThat(repositoryManager.getRawName()).isEqualTo("My NXRM Instance");
  }

  @Test
  public void testConfigureRepositories_WithInstanceName_UpdatesNameOnExistingRepositoryManager() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.emptyList());
    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        getUserAgent(), "Updated Name");

    repositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());
    assertThat(repositoryManager.getRawName()).isEqualTo("Updated Name");
  }

  @Test
  public void testConfigureRepositories_WithBlankInstanceName_ClearsName() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    repositoryManager.setName("Original Name");
    repositoryManagerDAO.update(repositoryManager);

    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.emptyList());
    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        getUserAgent(), "   ");

    repositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());
    assertThat(repositoryManager.getRawName()).isNull();
  }

  @Test
  public void testConfigureRepositories_WithEmptyInstanceName_ClearsName() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    repositoryManager.setName("Original Name");
    repositoryManagerDAO.update(repositoryManager);

    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.emptyList());
    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        getUserAgent(), "");

    repositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());
    assertThat(repositoryManager.getRawName()).isNull();
  }

  @Test
  public void testConfigureRepositories_WithInstanceNameTooLong_ThrowsBadRequest() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    String tooLongName = "a".repeat(201);

    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.emptyList());
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> getRepositoryService().configureRepositories(repositoryManager.getInstanceId(),
            configureRepositoriesRequest,
            getUserAgent(), tooLongName))
        .withMessage("Repository manager name must not exceed 200 characters.");
  }

  @Test
  public void testConfigureRepositories_WithDuplicateInstanceName_ThrowsInvalidNameException() {
    RepositoryManager rm1 = tempEntity.newRepositoryManager();
    rm1.setName("Shared Name");
    repositoryManagerDAO.update(rm1);

    RepositoryManager rm2 = tempEntity.newRepositoryManager();

    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.emptyList());
    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(
            () -> getRepositoryService().configureRepositories(rm2.getInstanceId(), configureRepositoriesRequest,
                getUserAgent(), "Shared Name"))
        .withMessageContaining("already used as a name");
  }

  @Test
  public void testConfigureRepositories_WithDuplicateInstanceName_NewManager_ThrowsInvalidNameException() {
    RepositoryManager rm1 = tempEntity.newRepositoryManager();
    rm1.setName("Shared Name");
    repositoryManagerDAO.update(rm1);

    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.emptyList());
    assertThatExceptionOfType(InvalidNameException.class)
        .isThrownBy(
            () -> getRepositoryService().configureRepositories("brand-new-instance-id", configureRepositoriesRequest,
                getUserAgent(), "Shared Name"))
        .withMessageContaining("already used as a name");
  }

  @Test
  public void testConfigureRepositories_NewRepo_WithMonitoringEnabled_SetsLastManualConfigureTime() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.name = "monitored-repo";
    repositoryDTO.format = "npm";
    repositoryDTO.type = RepositoryType.hosted;
    repositoryDTO.monitoringEnabled = true;
    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.singletonList(repositoryDTO));

    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        getUserAgent());

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());
    assertThat(repositories).hasSize(1);
    assertThat(repositories.get(0).getLastManualConfigureTime()).isNotNull();
  }

  @Test
  public void testConfigureRepositories_NewRepo_WithMonitoringDisabled_DoesNotSetLastManualConfigureTime() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.name = "unmonitored-repo";
    repositoryDTO.format = "npm";
    repositoryDTO.type = RepositoryType.hosted;
    repositoryDTO.monitoringEnabled = false;
    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.singletonList(repositoryDTO));

    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        getUserAgent());

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());
    assertThat(repositories).hasSize(1);
    assertThat(repositories.get(0).getLastManualConfigureTime()).isNull();
  }

  @Test
  public void testConfigureRepositories_ExistingRepo_MonitoringChanged_SetsLastManualConfigureTime() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository existing = tempEntity.newRepository(repositoryManager, "monitored-repo");
    assertThat(existing.getLastManualConfigureTime()).isNull();

    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.name = existing.getName();
    repositoryDTO.format = "npm";
    repositoryDTO.type = RepositoryType.proxy;
    repositoryDTO.monitoringEnabled = true;
    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.singletonList(repositoryDTO));

    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        getUserAgent());

    Repository updated = repositoryDAO.getById(existing.getId());
    assertThat(updated.getLastManualConfigureTime()).isNotNull();
  }

  @Test
  public void testConfigureRepositories_ExistingRepo_NoMonitoringChange_DoesNotSetLastManualConfigureTime() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository existing = tempEntity.newRepository(repositoryManager, "unmonitored-repo");
    assertThat(existing.getLastManualConfigureTime()).isNull();

    RepositoryDTO repositoryDTO = new RepositoryDTO();
    repositoryDTO.name = existing.getName();
    repositoryDTO.format = "npm";
    repositoryDTO.type = RepositoryType.proxy;
    repositoryDTO.monitoringEnabled = false;
    ConfigureRepositoriesRequest configureRepositoriesRequest =
        createConfigureRepositoriesRequest(Collections.singletonList(repositoryDTO));

    getRepositoryService().configureRepositories(repositoryManager.getInstanceId(), configureRepositoriesRequest,
        getUserAgent());

    Repository updated = repositoryDAO.getById(existing.getId());
    assertThat(updated.getLastManualConfigureTime()).isNull();
  }

  @Test
  public void testRemoveProprietaryComponentNames_NotHostedRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.proxy, ComponentIdentifier.FORMAT_NPM);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().removeProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId());
    }).withMessage("Repository " + repo.getPublicId() + " (" + repo.getId() + ") is not a hosted repository");
  }

  @Test
  public void testRemoveProprietaryNamespaceNames_NotHostedRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.proxy, ComponentIdentifier.FORMAT_NPM);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      getRepositoryService().removeProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId());
    }).withMessage("Repository " + repo.getPublicId() + " (" + repo.getId() + ") is not a hosted repository");
  }

  @Test
  public void testRemoveRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testRepoMaven", "maven2");

    getRepositoryService().removeRepository(repositoryManager.getInstanceId(), repository.getPublicId());

    Repository repositoryFound = repositoryDAO.getById(repository.getId());
    assertThat(repositoryFound).isNull();
  }

  @Test
  public void testRemoveRepository_WithRelatedOrganization() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testRepoMaven", "maven2");
    Organization organization = tempEntity.newOrganization();

    repository.setRelatedOrganizationId(organization.getId());
    repositoryDAO.update(repository);

    getRepositoryService().removeRepository(repositoryManager.getInstanceId(), repository.getPublicId());

    Repository repositoryFound = repositoryDAO.getById(repository.getId());
    assertThat(repositoryFound).isNull();
    assertThat(organizationDAO.getById(organization.getId())).isNull();
  }

  private void testAddProprietaryComponentNames_FormatTranslation(String repoFormat, String componentFormat) {
    // The repository does not exist. It will be created when proprietaryComponentNames are added.
    String repoManId = tempEntity.newRepositoryManager().getInstanceId();
    String repoId = "hosted-repo";
    ProprietaryComponentNames proprietaryComponentNames = new ProprietaryComponentNames(repoFormat, "format-test");

    getRepositoryService().addProprietaryComponentNames(repoManId, repoId, proprietaryComponentNames);

    assertThat(proprietaryComponentNamePatternDAO.getByFormat(componentFormat))
        .extracting(ProprietaryComponentNamePattern::getNamePattern)
        .containsExactlyInAnyOrder("format-test");
    assertThat(repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repoManId, repoId).getFormat())
        .isEqualTo(repoFormat);

    // The repository exists.
    proprietaryComponentNames = new ProprietaryComponentNames(repoFormat, "format-test1");

    getRepositoryService().addProprietaryComponentNames(repoManId, repoId, proprietaryComponentNames);

    assertThat(proprietaryComponentNamePatternDAO.getByFormat(componentFormat))
        .extracting(ProprietaryComponentNamePattern::getNamePattern)
        .containsExactlyInAnyOrder("format-test", "format-test1");
    assertThat(repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repoManId, repoId).getFormat())
        .isEqualTo(repoFormat);
  }

  private void assertTelemetry(
      final int requestedVersionCount,
      final int policyCompliantVersionCount,
      final long evaluationTime)
  {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(1)).send(telemetryDataArgumentCaptor.capture());
    final TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REPOSITORY_COMPONENT_METADATA_EVALUATION);

    assertThat(telemetryData.getAttributes()).hasSize(3);
    assertThat(telemetryData.getAttributes()).containsEntry(REPOSITORY_COMPONENT_REQUESTED_VERSION_COUNT,
        requestedVersionCount);
    assertThat(telemetryData.getAttributes()).containsEntry(REPOSITORY_COMPONENT_POLICY_COMPLIANT_VERSION_COUNT,
        policyCompliantVersionCount);
    assertThat((Long) telemetryData.getAttributes().get(REPOSITORY_COMPONENT_METADATA_EVALUATION_TIME))
        .isGreaterThanOrEqualTo(0)
        .isLessThanOrEqualTo(evaluationTime);
  }

  @Test
  public void testRemoveRepository_RepositoryDoesNotExist() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(
        repositoryManager.getId(), "NotExistingId");

    assertThat(repository).isNull();

    getRepositoryService().removeRepository(repositoryManager.getInstanceId(), "NotExistingId");
  }

  @Test
  public void testGetConfiguredRepositories() {
    Date may5th20239AM = Date.from(LocalDateTime.of(2023, 5, 1, 9, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
    Date may5th202310AM = Date.from(LocalDateTime.of(2023, 5, 1, 10, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
    Date may5th202311AM = Date.from(LocalDateTime.of(2023, 5, 1, 11, 0, 0).atZone(ZoneId.systemDefault()).toInstant());

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    tempEntity.newRepository(repositoryManager, "testRepoNpm", RepositoryType.proxy, "npm",
        may5th20239AM);
    Repository repository =
        tempEntity.newRepository(repositoryManager, "testRepoMaven", RepositoryType.proxy, "maven", may5th202311AM);
    String clientUserAgent = getUserAgent();

    List<RepositoryDTO> repositoryDTOS =
        getRepositoryService().getConfiguredRepositories(repositoryManager.getInstanceId(), may5th202310AM.getTime(),
            clientUserAgent);
    assertThat(repositoryDTOS).hasSize(1);
    RepositoryDTO repositoryDTO = repositoryDTOS.get(0);
    assertThat(repositoryDTO.name).isEqualTo(repository.getName());
    assertThat(repositoryDTO.format).isEqualTo(repository.getFormat());
    assertThat(repositoryDTO.type).isEqualTo(repository.getRepositoryType());
    assertThat(repositoryDTO.auditEnabled).isEqualTo(repository.isAuditEnabled());
    assertThat(repositoryDTO.quarantineEnabled).isEqualTo(repository.isQuarantineEnabled());
    assertThat(repositoryDTO.policyCompliantComponentSelectionEnabled).isEqualTo(
        repository.isPolicyCompliantComponentSelectionEnabled());
    assertThat(repositoryDTO.namespaceConfusionProtectionEnabled).isEqualTo(
        repository.isNamespaceConfusionProtectionEnabled());
  }

  @Test
  public void testGetConfiguredRepositories_NullTimestamp() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository1 = tempEntity.newRepository(repositoryManager, TemporaryEntity.uuid());
    repository1.setLastManualConfigureTime(new Date(0));
    repositoryDAO.update(repository1);
    Repository repository2 = tempEntity.newRepository(repositoryManager, TemporaryEntity.uuid());
    repository2.setLastManualConfigureTime(new Date(1));
    repositoryDAO.update(repository2);
    Repository repository3 = tempEntity.newRepository(repositoryManager, TemporaryEntity.uuid());
    String clientUserAgent = getUserAgent();

    List<RepositoryDTO> repositoryDTOS =
        getRepositoryService().getConfiguredRepositories(repositoryManager.getInstanceId(), null, clientUserAgent);

    assertThat(repositoryDTOS).extracting(r -> r.name)
        .containsExactlyInAnyOrder(repository1.getName(), repository2.getName(), repository3.getName());
  }

  @Test
  public void testGetConfiguredRepositories_UpdateUserAgent() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    String clientUserAgent = getUserAgent();

    getRepositoryService().getConfiguredRepositories(repositoryManager.getInstanceId(), 0L, clientUserAgent);

    assertThat(repositoryManagerDAO.getById(repositoryManager.getId()).getUserAgent()).isEqualTo(clientUserAgent);
  }

  @Test
  public void testGetConfiguredRepositories_NotExistingRepositoryManager() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      getRepositoryService().getConfiguredRepositories(MANUAL_REPO_MAN_INSTANCE_ID, 0L, null);
    }).withMessage("Cannot find a repository manager with instance ID " + MANUAL_REPO_MAN_INSTANCE_ID + ".");
  }

  @Test
  public void testEvaluateComponentMetadata_WaivedComponents_ExactComponentWaiver_PyPI() {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Test Age");
    Condition condition = new Condition(AgeInDaysConditionType.ID, "older than", "1");
    Constraint constraint = new Constraint(policy.getId(), "age>1day", null);
    constraint.addCondition(condition);
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    // mock data for 2 components
    ComponentIdentifier quarantinedComponentIdentifier = null;
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    for (int i = 1; i < 3; i++) {
      String filename = "testname-" + i + "-testqualifier.whl";
      String pathname = "/testname/" + i + "/" + filename;
      ComponentIdentifier componentIdentifier =
          ComponentIdentifier.createPypiCoordinates("testname", Integer.toString(i), "testqualifier", "whl");
      componentEvaluationDataRequestList.components
          .add(
              new RepositoryComponentEvaluationDataRequest(ComponentIdentifier.FORMAT_PYPI, pathname, null /* hash */));
      long catalogDateLong = Instant.now().minusSeconds(60 * 60).toEpochMilli();
      if (i == 1) {
        quarantinedComponentIdentifier = componentIdentifier;
        catalogDateLong = Instant.now().minusSeconds(2 * 24 * 60 * 60).toEpochMilli();
      }
      hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "hash" + i, MatchState.EXACT,
          0 /* index */, filename, catalogDateLong));
    }
    mockHdsRequestForMetadata(hdsResult);

    // call to service method
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList =
        getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, "testClientUserAgent");

    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(2);
    for (int i = 0; i < 2; i++) {
      RepositoryComponentEvaluationData repositoryComponentEvaluationData =
          repositoryComponentEvaluationResultList.componentEvalResults.get(i);
      assertThat(repositoryComponentEvaluationData.requestIndex).isEqualTo(i);
      if (i == 0) {
        assertThat(repositoryComponentEvaluationData.quarantine).isTrue();
      }
      else {
        assertThat(repositoryComponentEvaluationData.quarantine).isFalse();
      }
    }

    // mock data for policy waiver
    Date date = new Date();
    ConstraintFact constraintFact =
        new ConstraintFact(constraint.getId(), constraint.getName(), constraint.getOperator().name());
    constraintFact.addConditionFact(
        new ConditionFact(condition.getConditionTypeId(), condition.getConditionIndex(), "summary", "reason"));
    tempEntity.newWaiver("hash1", policy.getId(), Organization.ROOT_ORGANIZATION_ID,
        Collections.singletonList(constraintFact),
        PackageUrlIdentifier.toPackageUrl(quarantinedComponentIdentifier), EXACT_COMPONENT, "test comment", date,
        null);

    // call to service method
    repositoryComponentEvaluationResultList =
        getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, "testClientUserAgent");

    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(2);
    for (int i = 0; i < 2; i++) {
      RepositoryComponentEvaluationData repositoryComponentEvaluationData =
          repositoryComponentEvaluationResultList.componentEvalResults.get(i);
      assertThat(repositoryComponentEvaluationData.requestIndex).isEqualTo(i);
      if (i == 0) {
        assertThat(repositoryComponentEvaluationData.quarantine).isFalse();
      }
      else {
        assertThat(repositoryComponentEvaluationData.quarantine).isFalse();
      }
    }
  }

  @Test
  public void testEvaluateComponentMetadata_WaivedComponents_AllVersionsWaiver_PyPI() {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Test Age");
    Condition condition = new Condition(AgeInDaysConditionType.ID, "older than", "1");
    Constraint constraint = new Constraint(policy.getId(), "age>1day", null);
    constraint.addCondition(condition);
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    // mock data for 2 components
    ComponentIdentifier quarantinedComponentIdentifier = null;
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    for (int i = 1; i < 3; i++) {
      String filename = "testname-" + i + "-testqualifier.whl";
      String pathname = "/testname/" + i + "/" + filename;
      ComponentIdentifier componentIdentifier =
          ComponentIdentifier.createPypiCoordinates("testname", Integer.toString(i), "testqualifier", "whl");
      componentEvaluationDataRequestList.components
          .add(
              new RepositoryComponentEvaluationDataRequest(ComponentIdentifier.FORMAT_PYPI, pathname, null /* hash */));
      if (i == 1) {
        quarantinedComponentIdentifier = componentIdentifier;
      }
      hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "hash" + i, MatchState.EXACT,
          0 /* index */, filename,
          Instant.now().minusSeconds(2 * 24 * 60 * 60).toEpochMilli()));
    }
    mockHdsRequestForMetadata(hdsResult);

    // call to service method
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList =
        getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, "testClientUserAgent");

    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(2);
    for (int i = 0; i < 2; i++) {
      RepositoryComponentEvaluationData repositoryComponentEvaluationData =
          repositoryComponentEvaluationResultList.componentEvalResults.get(i);
      assertThat(repositoryComponentEvaluationData.requestIndex).isEqualTo(i);
      if (i == 0) {
        assertThat(repositoryComponentEvaluationData.quarantine).isTrue();
      }
      else {
        assertThat(repositoryComponentEvaluationData.quarantine).isTrue();
      }
    }

    // mock data for policy waiver
    Date date = new Date();
    ConstraintFact constraintFact =
        new ConstraintFact(constraint.getId(), constraint.getName(), constraint.getOperator().name());
    constraintFact.addConditionFact(
        new ConditionFact(condition.getConditionTypeId(), condition.getConditionIndex(), "summary", "reason"));
    tempEntity.newWaiver(null, policy.getId(), Organization.ROOT_ORGANIZATION_ID,
        Collections.singletonList(constraintFact),
        PackageUrlIdentifier.toPackageUrl(quarantinedComponentIdentifier), ALL_VERSIONS, "test comment", date,
        null);

    // call to service method
    repositoryComponentEvaluationResultList =
        getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, "testClientUserAgent");

    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(2);
    for (int i = 0; i < 2; i++) {
      RepositoryComponentEvaluationData repositoryComponentEvaluationData =
          repositoryComponentEvaluationResultList.componentEvalResults.get(i);
      assertThat(repositoryComponentEvaluationData.requestIndex).isEqualTo(i);
      if (i == 0) {
        assertThat(repositoryComponentEvaluationData.quarantine).isFalse();
      }
      else {
        assertThat(repositoryComponentEvaluationData.quarantine).isFalse();
      }
    }
  }

  @Test
  public void testEvaluateComponentMetadata_WaivedComponents_AllComponentsWaiver_PyPI() {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Test Age");
    Condition condition = new Condition(AgeInDaysConditionType.ID, "older than", "1");
    Constraint constraint = new Constraint(policy.getId(), "age>1day", null);
    constraint.addCondition(condition);
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, true);

    // mock data for 2 components
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    for (int i = 1; i < 3; i++) {
      String filename = "testname-" + i + "-testqualifier.whl";
      String pathname = "/testname/" + i + "/" + filename;
      ComponentIdentifier componentIdentifier =
          ComponentIdentifier.createPypiCoordinates("testname", Integer.toString(i), "testqualifier", "whl");
      componentEvaluationDataRequestList.components
          .add(
              new RepositoryComponentEvaluationDataRequest(ComponentIdentifier.FORMAT_PYPI, pathname, null /* hash */));
      hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "hash" + i, MatchState.EXACT,
          0 /* index */, filename,
          Instant.now().minusSeconds(2 * 24 * 60 * 60).toEpochMilli()));
    }
    mockHdsRequestForMetadata(hdsResult);

    // call to service method
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList =
        getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, "testClientUserAgent");

    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(2);
    for (int i = 0; i < 2; i++) {
      RepositoryComponentEvaluationData repositoryComponentEvaluationData =
          repositoryComponentEvaluationResultList.componentEvalResults.get(i);
      assertThat(repositoryComponentEvaluationData.requestIndex).isEqualTo(i);
      if (i == 0) {
        assertThat(repositoryComponentEvaluationData.quarantine).isTrue();
      }
      else {
        assertThat(repositoryComponentEvaluationData.quarantine).isTrue();
      }
    }

    // mock data for policy waiver
    Date date = new Date();
    ConstraintFact constraintFact =
        new ConstraintFact(constraint.getId(), constraint.getName(), constraint.getOperator().name());
    constraintFact.addConditionFact(
        new ConditionFact(condition.getConditionTypeId(), condition.getConditionIndex(), "summary", "reason"));
    tempEntity.newWaiver(null, policy.getId(), Organization.ROOT_ORGANIZATION_ID,
        Collections.singletonList(constraintFact), null, ALL_COMPONENTS, "test comment", date,
        null);

    // call to service method
    repositoryComponentEvaluationResultList =
        getRepositoryService().evaluateComponentMetadata(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
            componentEvaluationDataRequestList, "testClientUserAgent");

    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(2);
    for (int i = 0; i < 2; i++) {
      RepositoryComponentEvaluationData repositoryComponentEvaluationData =
          repositoryComponentEvaluationResultList.componentEvalResults.get(i);
      assertThat(repositoryComponentEvaluationData.requestIndex).isEqualTo(i);
      if (i == 0) {
        assertThat(repositoryComponentEvaluationData.quarantine).isFalse();
      }
      else {
        assertThat(repositoryComponentEvaluationData.quarantine).isFalse();
      }
    }
  }

  protected ComponentEvaluationData createComponentEvaluationData(
      ComponentIdentifier componentIdentifier,
      String hash,
      MatchState matchState,
      int index,
      String filename,
      long catalogDate)
  {
    ComponentEvaluationData componentEvaluationData =
        createComponentEvaluationData(componentIdentifier, hash, matchState,
            index, filename, null, null, null, 0);
    componentEvaluationData.catalogDate = catalogDate;

    return componentEvaluationData;
  }
}
