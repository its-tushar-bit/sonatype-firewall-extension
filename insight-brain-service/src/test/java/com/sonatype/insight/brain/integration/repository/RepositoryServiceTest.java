/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.hds.FirewallAuditHdsClient;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.integration.repository.RepositoryService.RepositoryDTO;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
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
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.repository.PendingRepositoryPolicyNotifications;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.RepositoryPolicyThreatDTO;
import com.sonatype.insight.brain.repository.RepositoryPolicyViolationDTO;
import com.sonatype.insight.brain.repository.RepositoryReportDetail;
import com.sonatype.insight.brain.repository.RepositoryReportResource.RepositoryReportSummary;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.apache.commons.lang.time.DateUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

/**
 * @since 1.17
 */
public class RepositoryServiceTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput policyViolationLoggerOutput = new LogOutput(
      AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  private static final String MANUAL_REPO_MAN_INSTANCE_ID = "manualDeleteRepoManagerInstanceId";

  private static final String REPO_MAN_INSTANCE_ID = "repoManagerInstanceId";

  private static final String REPO_PUBLIC_ID = "repoPublicId";

  @Inject
  private RepositoryService repositoryService;

  @Inject
  private CLMLicenseManager clmLicenseManager;

  @Inject
  private TestProductLicenseManager productLicenseManager;

  @Inject
  private PendingRepositoryPolicyNotifications pendingRepositoryPolicyNotifications;

  private RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  private RepositoryDAO repositoryDAO = new RepositoryDAO();

  private RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = new RepositoryPolicyViolationDAO();

  @Mock
  private HdsClient hdsClient;

  @Mock
  private FirewallAuditHdsClient auditHdsClient;

  @Mock
  private FirewallQuarantineHdsClient quarantineHdsClient;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClient);
    binder.bind(FirewallAuditHdsClient.class).toInstance(auditHdsClient);
    binder.bind(FirewallQuarantineHdsClient.class).toInstance(quarantineHdsClient);
    super.configure(binder);
  }

  @After
  public void cleanup() {
    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(MANUAL_REPO_MAN_INSTANCE_ID);
    if (repositoryManager != null) {
      repositoryManagerDAO.delete(repositoryManager);
    }
  }

  @Test
  public void testUnquarantineComponent_WasQuarantined() throws Exception {
    String pathname = "path";
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), pathname,
        new Date(), null);

    mockHdsRequestForComponent(repositoryComponent, true);

    repositoryService.unquarantineComponent(repository.getId(), pathname, null);
    repositoryComponent = repositoryComponentDAO.getById(repositoryComponent.getId());

    assertThat(repositoryComponent.isQuarantined()).isFalse();
  }

  @Test
  public void testUnquarantineComponent_WasNotQuarantined() throws Exception {
    String pathname = "path";
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), pathname, null,
        null);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      repositoryService.unquarantineComponent(repository.getId(), pathname, null);
    }).withMessage("Component " + pathname + " in repository " + repository.getId() + " is not quarantined.");
    repositoryComponent = repositoryComponentDAO.getById(repositoryComponent.getId());

    assertThat(repositoryComponent.isQuarantined()).isFalse();
  }

  @Test
  public void testUnquarantineComponent_WithViolations() throws Exception {
    String pathname = "path";
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), pathname,
        new Date(), null);

    createQuarantiningPolicy(repository);
    mockHdsRequestForComponent(repositoryComponent, true);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      repositoryService.unquarantineComponent(repository.getId(), pathname, null);
    }).withMessage("Component " + pathname + " in repository " + repository.getId() + " has policy violations.");
  }

  @Test
  public void testUnquarantineComponent_WithViolationsNotFailed() throws Exception {
    String pathname = "path";
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), pathname,
        new Date(), null);

    createQuarantiningPolicy(repository);
    mockHdsRequestForComponent(repositoryComponent, false);

    repositoryService.unquarantineComponent(repository.getId(), pathname, null);
    repositoryComponent = repositoryComponentDAO.getById(repositoryComponent.getId());

    assertThat(repositoryComponent.isQuarantined()).isFalse();
  }

  private void mockHdsRequestForComponent(RepositoryComponent repositoryComponent, boolean withSecurityVulnerabilities)
      throws Exception
  {
    // Prepare request and mock the HDS request
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest("maven2", repositoryComponent.getPathname(),
            repositoryComponent.getHash());
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList(RepositoryComponentEvaluationDataRequestList.REEVALUATION);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);

    List<SecurityVulnerability> securityVulnerabilities = new ArrayList<>();
    if (withSecurityVulnerabilities) {
      securityVulnerabilities = createSecurityVulnerabilities();
    }

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, repositoryComponent.getHash(),
        MatchState.EXACT, 0 /* index */, Collections.emptySet(), Collections.emptySet(),
        securityVulnerabilities, 0 /* popularity */));

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);
  }

  @Test
  public void testGetPolicyThreats() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    String pathname = "path1";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    RepositoryPolicyViolation repositoryPolicyViolation1 = tempEntity.newRepositoryPolicyViolation(repository.getId(),
        8, pathname, false, true, "policyId1", "policyName1", componentIdentifier);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 7, pathname, true, true, "policyId2", "policyName2",
        componentIdentifier);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, pathname, false, false, "policyId3", "policyName3",
        componentIdentifier);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "path4", false, true, Action.ID_FAIL, "policyId4",
        "policyName4", componentIdentifier);

    repositoryPolicyViolation1.setConstraintFacts(Collections.singletonList(new ConstraintFact("id", "name", "op")));
    new RepositoryPolicyViolationDAO().update(repositoryPolicyViolation1);

    tempEntity.newRepositoryComponent(repository.getId(), pathname, new Date(), null);
    tempEntity.newRepositoryComponent(repository.getId(), "path4", new Date(), null);

    RepositoryPolicyThreatDTO repositoryPolicyThreatDTO = repositoryService.getPolicyThreats(repository.getId(),
        pathname);

    assertThat(repositoryPolicyThreatDTO.activePolicyViolations).hasSize(1);
    RepositoryPolicyViolationDTO repositoryViolationDTO = repositoryPolicyThreatDTO.activePolicyViolations.get(0);
    assertThat(repositoryViolationDTO.policyId).isEqualTo("policyId1");
    assertThat(repositoryViolationDTO.policyName).isEqualTo("policyName1");
    assertThat(repositoryViolationDTO.policyThreatLevel).isEqualTo(8);
    assertThat(repositoryViolationDTO.constraintFactsJson)
        .isEqualTo(repositoryPolicyViolation1.getConstraintFactsJson());
    assertThat(repositoryViolationDTO.blocksUnquarantine).isFalse();

    repositoryViolationDTO = repositoryService.getPolicyThreats(repository.getId(), "path4").activePolicyViolations
        .get(0);
    assertThat(repositoryViolationDTO.blocksUnquarantine).isTrue();
  }

  @Test
  public void testGetPolicyThreats_RepositoryComponentDoesNotExist() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.getPolicyThreats(repository.getId(), "pathDoesNotExist");
    }).withMessage(
        "Cannot find a component with path pathDoesNotExist in repository with ID " + repository.getId() + ".");
  }

  @Test
  public void testGetPolicyThreats_RepositoryDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.getPolicyThreats("RepositoryIdDoesNotExist", null);
    }).withMessage("Cannot find a repository with ID RepositoryIdDoesNotExist.");
  }

  @Test
  public void testSetEnabled_NoRepositoryManager() throws Exception {
    repositoryService.setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);

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

    repositoryService.setEnabled(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertThat(repositories).hasSize(1);
    assertThat(repositories.get(0).getPublicId()).isEqualTo(REPO_PUBLIC_ID);
    assertThat(repositories.get(0).isEnabled()).isTrue();
  }

  @Test
  public void testSetEnabled_TrueExistingRepository() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    repositoryService.setEnabled(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertThat(repositories).hasSize(1);
    assertThat(repositories.get(0).getPublicId()).isEqualTo(REPO_PUBLIC_ID);
    assertThat(repositories.get(0).isEnabled()).isTrue();
  }

  @Test
  public void testSetEnabled_MissingLicenseFeature() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      repositoryService.setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);
    }).withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testSetEnabled_FalseExistingRepository() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    repositoryService.setEnabled(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, false);

    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(repositoryManager.getId());

    assertThat(repositories).hasSize(1);
    assertThat(repositories.get(0).getPublicId()).isEqualTo(REPO_PUBLIC_ID);
    assertThat(repositories.get(0).isEnabled()).isFalse();
  }

  @Test
  public void testSetQuarantine_RepositoryDoesNotExist() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID));
  }

  @Test
  public void testSetQuarantine_EnabledWhenRepositoryNotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      repositoryService.setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);
    }).withMessage("Cannot enable quarantine when repository " + REPO_PUBLIC_ID + " is disabled.");
  }

  @Test
  public void testSetQuarantine_DisabledWhenRepositoryNotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false, true);

    // Check initial state
    assertThat(repository.isEnabled()).isFalse();
    assertThat(repository.isQuarantineEnabled()).isFalse();

    repositoryService.setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, false);
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

    repositoryService.setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, true);
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

    repositoryService.setQuarantine(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, false);
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
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1", true, true, "policyId1", "policyName1",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    // Now add an obsolete one that should not show up in the test
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1", false, false, "policyId2", "policyName2",
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    // And one not in the range that should not show up in the test
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "path4",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));

    // And a quarantined component
    tempEntity.newRepositoryComponent(repository.getId(), "/quarantined", new Date(), null);

    RepositoryPolicyEvaluationSummary policyEvaluationSummary = repositoryService.getPolicyEvaluationSummary(
        REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
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

    RepositoryPolicyEvaluationSummary policyEvaluationSummary = repositoryService.getPolicyEvaluationSummary(
        REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(1);
  }

  @Test
  public void testGetPolicyEvaluationSummary_SameComponentDifferentPolicy() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1", false, true, "policyId1", "policyName1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1", false, true, "policyId2", "policyName2",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));

    RepositoryPolicyEvaluationSummary policyEvaluationSummary = repositoryService.getPolicyEvaluationSummary(
        REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(1);
  }

  @Test
  public void testGetPolicyEvaluationSummary_MissingLicenseFeature() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      repositoryService.getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    }).withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_RepositoryDoesNotExist() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null, true, null);
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID));
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_NullRequest() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponentEvaluationDataList componentEvaluationResultList = repositoryService.evaluateComponents(
        REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null, true, null);
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
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
          true, null);
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
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
          true, null);
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
        new RepositoryComponentEvaluationDataRequest("maven2", pathname, hash);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // Call the service
    Date before = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = repositoryService
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
    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations).hasSize(1);

    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
        pathname);
    assertRepositoryComponent(repository.getId(), pathname, before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), before, after, after, repositoryComponent);

    RepositoryPolicyViolation policyViolation = repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(
        repository.getId(), pathname).get(0);
    assertPolicyViolation(repository.getId(), pathname, policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolation);
  }

  @Test
  public void testEvaluateComponents_WithQuarantine_QuarantinedUnchangedComponentRemainsQuarantined()
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
        new RepositoryComponentEvaluationDataRequest("maven2", pathname, hash);
    
    // prepare hds response with violation
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        Collections.emptySet(), Collections.emptySet(), securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // call the service
    Date timeBeforeEvaluation = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = repositoryService
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

    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
        pathname);
    assertRepositoryComponent(repository.getId(), pathname, timeBeforeEvaluation, timeAfterEvaluation, hash,
        componentIdentifier, MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), timeBeforeEvaluation,
        timeAfterEvaluation, timeAfterEvaluation, repositoryComponent);
    assertThat(repositoryComponent.isQuarantined()).isTrue();

    RepositoryPolicyViolation policyViolation = repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(
        repository.getId(), pathname).get(0);
    assertPolicyViolation(repository.getId(), pathname, policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, timeBeforeEvaluation, timeAfterEvaluation,
        policyViolation);

    // prepare a hds request with no violations
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // evaluate and confirm quarantine state
    repositoryComponentEvaluationResultList = repositoryService
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex).isEqualTo(0);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isTrue();
    
    List<RepositoryPolicyViolation> currentRepositoryPolicyViolations = repositoryPolicyViolationDAO
        .getActiveByRepositoryIdAndPathname(repository.getId(), pathname);
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
        new RepositoryComponentEvaluationDataRequest("maven2", pathname, hash);

    // prepare hds response with no violations
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // call to evaluate
    Date timeBeforeEvaluation = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = repositoryService
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex).isEqualTo(0);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isFalse();

    List<RepositoryPolicyViolation> currentRepositoryPolicyViolations = repositoryPolicyViolationDAO
        .getActiveByRepositoryIdAndPathname(repository.getId(), pathname);
    assertThat(currentRepositoryPolicyViolations.isEmpty()).isTrue();

    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
        pathname);
    assertThat(repositoryComponent.isQuarantined()).isFalse();

    // prepare hds result with violations
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        Collections.emptySet(), Collections.emptySet(), securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // call to evaluate
    repositoryComponentEvaluationResultList = repositoryService
        .evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
    Date after = new Date();
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).requestIndex).isEqualTo(0);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isFalse();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations).hasSize(1);

    repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), pathname);
    assertRepositoryComponent(repository.getId(), pathname, timeBeforeEvaluation, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), timeBeforeEvaluation, after, null,
        repositoryComponent);
    assertThat(repositoryComponent.isQuarantined()).isFalse();

    RepositoryPolicyViolation policyViolation = repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(
        repository.getId(), pathname).get(0);
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
        new RepositoryComponentEvaluationDataRequest("maven2", "/" + pathname, hash);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // Call the service
    Date before = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = repositoryService
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
    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations).hasSize(1);

    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
        pathname);
    assertRepositoryComponent(repository.getId(), pathname, before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), before, after, after, repositoryComponent);

    RepositoryPolicyViolation policyViolation = repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(
        repository.getId(), pathname).get(0);
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
        new RepositoryComponentEvaluationDataRequest("maven2", pathname, hash);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // Call the service
    Date before = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = repositoryService
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

    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
        pathname);
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
        new RepositoryComponentEvaluationDataRequest("maven2", pathname, hash);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);

    // Call the service
    Date before = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = repositoryService
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
    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations).hasSize(1);

    RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
        pathname);
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
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("nuget", pathname,
        hash));

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0,
        Collections.singleton(new License("EPL-1.0", "EPL-2.0")),
        Collections.singleton(new License("EPL-1.0", "EPL-2.0")), createSecurityVulnerabilities(), 80));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // initial evaluation of component, audit-only
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList = repositoryService
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
    repositoryComponentEvaluationResultList = repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID,
        REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
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
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("nuget", pathname,
        hash));

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0,
        Collections.singleton(new License("EPL-1.0", "EPL-2.0")),
        Collections.singleton(new License("EPL-1.0", "EPL-2.0")), createSecurityVulnerabilities(), 80));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true /* quarantine */);

    // Initial evaluation of component, quarantine enabled
    Date before = new Date();
    RepositoryComponentEvaluationDataList repositoryComponentEvaluationResultList =
        repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
            true /* withQuarantine */, null);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults).hasSize(1);
    assertThat(repositoryComponentEvaluationResultList.componentEvalResults.get(0).quarantine).isTrue();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertThat(repositoryComponent.isQuarantined()).isTrue();

    // Unquarantine the component
    repositoryComponent.setUnquarantineTime(new Date());
    repositoryComponentDAO.update(repositoryComponent);
    repositoryComponent = repositoryComponentDAO.getById(repositoryComponent.getId());
    assertThat(repositoryComponent.isQuarantined()).isFalse();

    // Re-evaluation of component, quarantine enabled
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, true);
    repositoryComponentEvaluationResultList = repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID,
        REPO_PUBLIC_ID, componentEvaluationDataRequestList, true, null);
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
    assertThat(repositoryComponent.getQuarantineTime()).isAfterOrEqualsTo(before).isBeforeOrEqualsTo(after);
    assertThat(repositoryComponent.isQuarantined()).isFalse();
  }

  @Test
  public void testEvaluateComponents_RepositoryDoesNotExist() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
          null /* componentEvaluationDataRequestList */, false, null);
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID));
  }

  @Test
  public void testEvaluateComponents_ExistingRepository_NotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false, false);

    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
        null /* componentEvaluationDataRequestList */, false, null);

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isFalse();
  }

  @Test
  public void testEvaluateComponents_ExistingRepository_QuarantineNotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true, false);

    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
        null /* componentEvaluationDataRequestList */, true, null);

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();
  }

  @Test
  public void testEvaluateComponents_ExistingRepository_RepositoryAndQuarantineNotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false, false);

    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID,
        null /* componentEvaluationDataRequestList */, true, null);

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
      ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g" + i, "a" + i, "v" + i,
          "c" + i, "e" + i);
      componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path"
          + i, "h" + i));
      hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h" + i, MatchState.EXACT,
          i /* index */, declaredLicenseSet, observedLicenseSet, securityVulnerabilities, i /* popularity */));
    }
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false, null);
    Date after = new Date();

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.isEnabled()).isTrue();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(2);
    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations).hasSize(2);

    for (int i = 0; i < componentCount; i++) {
      String pathname = "path" + i;
      ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g" + i, "a" + i, "v" + i,
          "c" + i, "e" + i);
      String hash = "h" + i;

      RepositoryComponent repositoryComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
          pathname);
      assertRepositoryComponent(repository.getId(), pathname, before, after, hash, componentIdentifier,
          MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);

      RepositoryPolicyViolation policyViolation = repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(
          repository.getId(), pathname).get(0);
      assertPolicyViolation(repository.getId(), pathname, policy.getId(), policy.getName(), policy.getThreatLevel(),
          policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolation);
    }
  }

  @Test
  public void testEvaluateComponents_NewComponentViolationNotifications() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    tempEntity.newPolicy(repository.getParentOwnerId(), "Test Policy", 10, null, null, new Notifications(
        new UserNotification("test@sonatype.com", Stage.ID_PROXY)));
    Policy waivedPolicy = tempEntity.newPolicy(repository.getParentOwnerId(), "Waived Policy", 10, null, null,
        new Notifications(new UserNotification("waived@sonatype.com", Stage.ID_PROXY)));
    tempEntity.newWaiver(waivedPolicy.getId(), repository.getId());

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList(RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT);

    String hash1 = "hash1";
    String hash2 = "hash2";

    // Prepare request and mock the HDS request
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();

    hdsResult.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2",
        "pathname1", hash1));
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2",
        "pathname2", hash2));
    hdsResult.components.add(createComponentEvaluationData(
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), hash1, MatchState.EXACT, 0, null, null,
        securityVulnerabilities, 80));
    hdsResult.components.add(createComponentEvaluationData(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), hash2, MatchState.EXACT, 1, null, null,
        securityVulnerabilities, 80));

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false, null);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(4);

    List<PolicyNotification> policyNotifications = pendingRepositoryPolicyNotifications.remove()
        .get(repository.getId());
    assertThat(policyNotifications).hasSize(2);
    for (PolicyNotification policyNotification : policyNotifications) {
      assertThat(policyNotification.getPolicyFact().getPolicyName()).isEqualTo("Test Policy");

      Notifications notifications = policyNotification.getNotifications();
      assertThat(notifications.getUserNotifications()).hasSize(1);
      assertThat(notifications.getRoleNotifications()).isEmpty();
      assertThat(notifications.getJiraNotifications()).isEmpty();

      UserNotification userNotification = notifications.getUserNotifications().get(0);
      assertThat(userNotification.getEmailAddress()).isEqualTo("test@sonatype.com");
    }
  }

  @Test
  public void testEvaluateComponents_ReevaluationViolationNotifications() throws Exception {
    // This test ensures that there are no notifications for the evaluation cause other than "new component"
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    tempEntity.newPolicy(repository.getParentOwnerId(), "Test Policy", 10, null, null, new Notifications(
        new UserNotification("test@sonatype.com", Stage.ID_PROXY)));

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList(RepositoryComponentEvaluationDataRequestList.REEVALUATION);

    String hash = "hash";

    // Prepare request and mock the HDS request
    List<SecurityVulnerability> securityVulnerabilities = createSecurityVulnerabilities();
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");

    hdsResult.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven", "pathname",
        hash));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0, null, null,
        securityVulnerabilities, 80));

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false, null);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO
        .getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(1);

    List<PolicyNotification> policyNotifications = pendingRepositoryPolicyNotifications.remove()
        .get(repository.getId());
    assertThat(policyNotifications).isNull();
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
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path",
        hash));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service first time
    Date before1 = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false, null);
    Date after1 = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before1, after1, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations).hasSize(1);
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before1, after1, policyViolations.get(0));

    // Call the service second time
    String updatedHash = "h1";
    ComponentIdentifier updatedComponentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1",
        "e1");
    componentEvaluationDataRequestList = new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path",
        updatedHash));
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(updatedComponentIdentifier, updatedHash, MatchState.EXACT,
        0 /* index */, declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);
    Date before2 = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false, null);
    Date after2 = new Date();

    repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before2, after2, updatedHash, updatedComponentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), before2, after2, null, repositoryComponent);

    policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(policyViolations).hasSize(2);
    for (RepositoryPolicyViolation policyViolation : policyViolations) {
      if (policyViolation.isActive()) {
        assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
            policy.getThreatCategory(), updatedHash, updatedComponentIdentifier, before2, after2, policyViolation);
      }
      else {
        assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
            policy.getThreatCategory(), hash, componentIdentifier, before1, after1, policyViolation);
      }
    }
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
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path",
        "h"));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false, null);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before, after, "h", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
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
    List<SecurityVulnerability> securityVulnerabilities = Collections.singletonList(new SecurityVulnerability(
        referenceId, source, 2.9F));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path",
        hash));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        null /* declaredLicenses */, null /* observedLicenses */, securityVulnerabilities, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false, null);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
    assertThat(policyViolations).hasSize(1);
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), policy.getThreatLevel(),
        policy.getThreatCategory(), hash, componentIdentifier, before, after, policyViolations.get(0));
  }

  @Test
  public void testEvaluateComponents_ClaimedComponent() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Condition condition =
        new Condition(IdentificationSourceConditionType.ID, "is", IdentificationSource.MANUAL.getId());
    Constraint constraint = new Constraint("id", "name", LogicalOperator.AND);
    constraint.addCondition(condition);
    Policy policy = new Policy("id", "name");
    policy.setOwnerId(repository.getParentOwnerId());
    policy.addConstraint(constraint);
    tempEntity.newPolicy(policy);

    ComponentIdentifier claimedComponentIdentifier = ComponentIdentifier.createMavenCoordinates("cg", "ca", "cv", "cc",
        "ce");
    tempEntity.newClaimedComponent("h", claimedComponentIdentifier);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();

    // Prepare request and mock the HDS request
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path",
        "h"));
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, "h", MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false, null);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    assertRepositoryComponent(repository.getId(), "path", before, after, "h", claimedComponentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.MANUAL.getId(), repositoryComponents.get(0));

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
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
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path",
        longHash));
    Set<License> declaredLicenseSet = Collections.singleton(new License("Apache-2.0", "Apache-2.0"));
    Set<License> observedLicenseSet = Collections.singleton(new License("ATT", "ATT"));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.EXACT, 0 /* index */,
        declaredLicenseSet, observedLicenseSet, null /* securityVulnerabilities */, 0 /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false, null);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before, after, hash, componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
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
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven", "path",
        hash));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(createComponentEvaluationData(componentIdentifier, hash, MatchState.UNKNOWN,
        0 /* index */, Collections.emptySet(), Collections.emptySet(),
        null /* securityVulnerabilities */, null /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false, null);
    Date after = new Date();

    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).hasSize(1);
    RepositoryComponent repositoryComponent = repositoryComponents.get(0);
    assertRepositoryComponent(repository.getId(), "path", before, after, hash, componentIdentifier,
        MatchState.UNKNOWN.getId(), IdentificationSource.SONATYPE.getId(), repositoryComponent);

    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(repository
        .getId());
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
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "/path",
        hash));
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components
        .add(createComponentEvaluationData(componentIdentifier1, hash, MatchState.EXACT, 0 /* index */,
            Collections.emptySet(), Collections.emptySet(), null /* securityVulnerabilities */,
            null /* popularity */));
    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);

    // Call the service
    Date before = new Date();
    repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
        false, null);
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
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", null,
        "hash"));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
          false, null);
    }).withMessage("The pathname cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_EmptyPathname() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", " ",
        "hash"));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
          false, null);
    }).withMessage("The pathname cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_NullFormat() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest(null, "pathname",
        "hash"));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
          false, null);
    }).withMessage("The format cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_EmptyFormat() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest(" ", "pathname",
        "hash"));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
          false, null);
    }).withMessage("The format cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_NullHash() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    String hash = null;

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path",
        hash));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
          false, null);
    }).withMessage("The hash cannot be null or empty.");
  }

  @Test
  public void testEvaluateComponents_EmptyHash() throws Exception {
    tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList();
    componentEvaluationDataRequestList.components.add(new RepositoryComponentEvaluationDataRequest("maven2", "path",
        " "));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, componentEvaluationDataRequestList,
          false, null);
    }).withMessage("The hash cannot be null or empty.");
  }

  @Test
  public void testGetReportSummary() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repo = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);
    RepositoryComponent component1 = tempEntity.newRepositoryComponent(repo.getId(), "1");
    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repo.getId(), "2");
    RepositoryComponent component3 = tempEntity.newRepositoryComponent(repo.getId(), "3");
    RepositoryComponent component4 = tempEntity.newRepositoryComponent(repo.getId(), "4");
    tempEntity.newRepositoryComponent(repo.getId(), MatchState.UNKNOWN, null);

    tempEntity.newRepositoryPolicyViolation(repo.getId(), 1, component1.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 5, component2.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 6, component3.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 9, component4.getPathname(), null);

    tempEntity.newRepositoryComponent(repo.getId(), "/quarantined", new Date(), null);

    RepositoryReportSummary summary = repositoryService.getReportSummary(repo.getId());

    assertThat(summary.knownComponentCount).isEqualTo(5);
    assertThat(summary.totalComponentCount).isEqualTo(6);
    assertThat(summary.criticalComponentCount).isEqualTo(1);
    assertThat(summary.severeComponentCount).isEqualTo(2);
    assertThat(summary.moderateComponentCount).isEqualTo(0);
    assertThat(summary.affectedComponentCount).isEqualTo(3);
    assertThat(summary.quarantinedComponentCount).isEqualTo(1);
  }

  @Test
  public void testEvaluateComponents_MissingLicenseFeature() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      repositoryService.evaluateComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, null, false, null);
    }).withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  private void mockHdsRequest(RepositoryComponentEvaluationDataRequestList serviceRequest,
                              ComponentEvaluationDataList hdsResult,
                              boolean quarantine) throws IOException
  {
    RepositoryComponentEvaluationDataRequestList hdsRequest = new RepositoryComponentEvaluationDataRequestList();
    hdsRequest.cause = serviceRequest.cause;
    hdsRequest.components = new ArrayList<>();
    for (RepositoryComponentEvaluationDataRequest componentEvaluationDataRequest : serviceRequest.components) {
      String hash = HashHelper.truncateHash(componentEvaluationDataRequest.hash);
      String pathname = componentEvaluationDataRequest.pathname.substring(componentEvaluationDataRequest.pathname
          .startsWith("/") ? 1 : 0);
      hdsRequest.components.add(new RepositoryComponentEvaluationDataRequest(componentEvaluationDataRequest.format,
          pathname, hash));
    }
    when((quarantine ? quarantineHdsClient : auditHdsClient).post(any(), eq(ComponentEvaluationDataList.class),
        eq(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH), isNull(), eq(hdsRequest))).thenReturn(hdsResult);
  }

  private ComponentEvaluationData createComponentEvaluationData(ComponentIdentifier componentIdentifier,
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

  private List<SecurityVulnerability> createSecurityVulnerabilities() {
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
    assertThat(actual.getTime()).isAfterOrEqualsTo(beforeCreate).isBeforeOrEqualsTo(afterCreate);
    assertThat(actual.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(actual.getMatchStateId()).isEqualTo(matchStateId);
    assertThat(actual.getIdentificationSourceId()).isEqualTo(identificationSourceId);
    assertThat(actual.getLastEvaluationTime()).isAfterOrEqualsTo(beforeLastEvaluation)
        .isBeforeOrEqualsTo(afterLastEvaluation);
    if (afterQuarantineTime != null) {
      assertThat(actual.getQuarantineTime()).isBeforeOrEqualsTo(afterQuarantineTime);
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
    assertThat(actual.getTime()).isAfterOrEqualsTo(before).isBeforeOrEqualsTo(after);
  }

  @Test
  public void testRemoveComponent_RepositoryDoesNotExist() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "somepath");
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID));
  }

  @Test
  public void testRemoveComponent_RepositoryNotEnabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false /* enabled */);

    repositoryService.removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "somepath");

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

    repositoryService.removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, pathname1);

    assertThat(repositoryComponentDAO.getById(repositoryComponent1.getId())).isNull();
    assertThat(repositoryComponentDAO.getById(repositoryComponent2.getId())).isNotNull();
    assertThat(repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), pathname1)).isNull();
    policyViolation1 = repositoryPolicyViolationDAO.getById(policyViolation1.getId());
    assertThat(policyViolation1.isActive()).isFalse();
    policyViolation2 = repositoryPolicyViolationDAO.getById(policyViolation2.getId());
    assertThat(policyViolation2.isActive()).isTrue();
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

    repositoryService.removeComponent(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "/" + pathname1);

    assertThat(repositoryComponentDAO.getById(repositoryComponent1.getId())).isNull();
    assertThat(repositoryComponentDAO.getById(repositoryComponent2.getId())).isNotNull();
    assertThat(repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(), pathname1)).isNull();
    policyViolation1 = repositoryPolicyViolationDAO.getById(policyViolation1.getId());
    assertThat(policyViolation1.isActive()).isFalse();
    policyViolation2 = repositoryPolicyViolationDAO.getById(policyViolation2.getId());
    assertThat(policyViolation2.isActive()).isTrue();
  }

  @Test
  public void testTHREAT_LEVEL_DESC_PATHNAME_ASC() throws Exception {
    final RepositoryReportDetail detail1 = RepositoryReportDetail.create(new RepositoryComponent(null, "z", null, null,
        null, null, null, null));
    final RepositoryReportDetail detail2 = RepositoryReportDetail.create(new RepositoryComponent(null, "a", null, null,
        null, null, null, null), new RepositoryPolicyViolation(null, null, null, null, null, 9, null, null,
        null, "[]" /* constraintFacts */), false);
    assertThat(RepositoryService.THREAT_LEVEL_DESC_PATHNAME_ASC.compare(detail1, detail2))
        .as("Should sort ThreatLevel Descending").isPositive();

    final RepositoryReportDetail detail3 = RepositoryReportDetail.create(new RepositoryComponent(null, "a", null, null,
        null, null, null, null), new RepositoryPolicyViolation(null, null, null, null, null, 0, null, null,
        null, "[]" /* constraintFacts */), false);
    assertThat(RepositoryService.THREAT_LEVEL_DESC_PATHNAME_ASC.compare(detail1, detail3))
        .as("Should sort Pathname Ascending").isPositive();

    final RepositoryReportDetail detail4 = RepositoryReportDetail.create(new RepositoryComponent(null, "z", null, null,
        null, null, null, null), new RepositoryPolicyViolation(null, null, null, null, null, 0, null, null,
        null, "[]" /* constraintFacts */), false);
    assertThat(RepositoryService.THREAT_LEVEL_DESC_PATHNAME_ASC.compare(detail1, detail4))
        .as("Equal ThreatLevel and pathname").isZero();
  }

  @Test
  public void testGetReportDetails() throws Exception {
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    final Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);

    // component with 1 violation
    final String pathname1 = "pathname1";
    createRepositoryPolicyViolation(repository, pathname1, 5);

    // component with no violation
    final String pathname2 = "pathname2";
    createRepositoryPolicyViolation(repository, pathname2);

    // component with 2 violations
    final String pathname3 = "pathname3";
    createRepositoryPolicyViolation(repository, pathname3, 5, 9);

    // component with 1 violation that is waived
    final String pathname4 = "pathname4";
    createRepositoryPolicyViolation(repository, pathname4, true, 1);

    // add violations for a different repository, which should not be included in current repo details
    final Repository repositoryOther = tempEntity.newRepository(repositoryManager, "otherRepoPublicId");
    createRepositoryPolicyViolation(repositoryOther, pathname1, 6);

    final List<RepositoryReportDetail> reportDetails = repositoryService.getReportDetails(repository.getId(), null,
        null);

    assertThat(reportDetails).hasSize(6);

    int idx = 0;
    // list should be sorted by 'threadLevel DESC', 'pathname ASC'
    assertRepositoryReportDetail(reportDetails.get(idx++), pathname3, "policyName", 9, true, false);
    assertRepositoryReportDetail(reportDetails.get(idx++), pathname1, "policyName", 5, true, false);
    assertRepositoryReportDetail(reportDetails.get(idx++), pathname3, "policyName", 5, false, false);
    assertRepositoryReportDetail(reportDetails.get(idx++), pathname4, "policyName", 1, true, true);
    assertRepositoryReportDetail(reportDetails.get(idx++), pathname2, null, 0, true, false);
    assertRepositoryReportDetail(reportDetails.get(idx), pathname4, null, 0, true, false);
  }

  @Test
  public void testGetReportDetails_ByHash() throws Exception {
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    final Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);

    RepositoryComponent component1 = tempEntity.newRepositoryComponent(repository, "hash1");
    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repository, "hash1");
    tempEntity.newRepositoryComponent(repository, "hash2");

    // Add a component for a different repository, which should not be included in current repo details
    final Repository repositoryOther = tempEntity.newRepository(repositoryManager, "otherRepoPublicId");
    tempEntity.newRepositoryComponent(repositoryOther, "hash1");

    final List<RepositoryReportDetail> reportDetails = repositoryService.getReportDetails(repository.getId(), "hash1",
        null);

    assertThat(reportDetails).hasSize(2);

    for (RepositoryReportDetail detail : reportDetails) {
      assertThat(detail.getHash()).isEqualTo("hash1");
      assertThat(detail.getPathname()).isIn(component1.getPathname(), component2.getPathname());
    }
  }

  @Test
  public void testGetReportDetails_ByPathname() throws Exception {
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPO_MAN_INSTANCE_ID);
    final Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);

    RepositoryComponent component = tempEntity.newRepositoryComponent(repository, "hash1");
    tempEntity.newRepositoryComponent(repository, "hash1");

    // Add a component for a different repository, which should not be included in current repo details
    final Repository repositoryOther = tempEntity.newRepository(repositoryManager, "otherRepoPublicId");
    tempEntity.newRepositoryComponent(repositoryOther.getId(), component.getPathname());

    final List<RepositoryReportDetail> reportDetails = repositoryService.getReportDetails(repository.getId(), null,
        component.getPathname());

    assertThat(reportDetails).hasSize(1);
    assertThat(reportDetails.get(0).getPathname()).isEqualTo(component.getPathname());
  }

  @Test
  public void testGetRepositoryById() {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), new Date());

    RepositoryDTO actual = repositoryService.getRepositoryById(repository.getId());
    assertThat(actual.repository).isNotNull();
    assertThat(actual.repository.getPublicId()).isEqualTo(repository.getPublicId());
    assertThat(actual.oldestEvalTimestamp).isEqualTo(repositoryComponent.getLastEvaluationTime().getTime());
  }

  @Test
  public void testGetRepositoryById_NoEvaluation() {
    Repository repository = tempEntity.newRepository();

    RepositoryDTO actual = repositoryService.getRepositoryById(repository.getId());
    assertThat(actual.repository).isNotNull();
    assertThat(actual.repository.getPublicId()).isEqualTo(repository.getPublicId());
    assertThat(actual.oldestEvalTimestamp).isNull();
  }

  @Test
  public void testGetRepositoryById_UnknownId() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.getRepositoryById("foobar");
    }).withMessage("Cannot find a repository with ID foobar.");
  }

  @Test
  public void testReevaluateRepository() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(),
        DateUtils.addDays(new Date(), -1));

    ComponentEvaluationDataList response = new ComponentEvaluationDataList();
    ComponentEvaluationData component = new ComponentEvaluationData();
    component.hash = repositoryComponent.getHash();
    component.observedLicenses = Collections.emptySet();
    component.declaredLicenses = Collections.emptySet();
    component.matchState = MatchState.UNKNOWN.getId();
    response.components.add(component);
    when(auditHdsClient.post(any(), eq(ComponentEvaluationDataList.class),
        eq(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH), isNull(),
        any(RepositoryComponentEvaluationDataRequestList.class))).thenReturn(response);

    Date beforeEvaluation = new Date();
    repositoryService.reevaluateRepository(repository.getId());

    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      Date lastEvaluationTime = repositoryComponentDAO.getByRepositoryId(repository.getId()).get(0)
          .getLastEvaluationTime();
      assertThat(lastEvaluationTime).isAfterOrEqualsTo(beforeEvaluation);
    });
  }

  @Test
  public void testReevaluateRepository_UnknownId() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.reevaluateRepository("foobar");
    }).withMessage("Cannot find a repository with ID foobar.");
  }

  @Test
  public void testDeleteRepository() throws Exception {
    Repository repository = tempEntity.newRepository();
    repositoryService.deleteRepository(repository.getId());
    assertThat(repositoryDAO.getById(repository.getId())).isNull();
  }

  @Test
  public void testDeleteRepository_UnknownId() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.deleteRepository("foobar");
    }).withMessage("Cannot find a repository with ID foobar.");
  }

  @Test
  public void testReevaluateComponent() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(),
        DateUtils.addDays(new Date(), -1));

    ComponentEvaluationDataList response = new ComponentEvaluationDataList();
    ComponentEvaluationData component = new ComponentEvaluationData();
    component.hash = repositoryComponent.getHash();
    component.observedLicenses = Collections.emptySet();
    component.declaredLicenses = Collections.emptySet();
    component.matchState = MatchState.UNKNOWN.getId();
    response.components.add(component);
    when(auditHdsClient.post(any(), eq(ComponentEvaluationDataList.class),
        eq(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH), isNull(),
        any(RepositoryComponentEvaluationDataRequestList.class))).thenReturn(response);

    repositoryService.reevaluateComponent(repository.getId(), repositoryComponent.getHash(), null);

    RepositoryComponent actualComponent = repositoryComponentDAO.getByRepositoryIdAndPathname(repository.getId(),
        repositoryComponent.getPathname());

    assertThat(actualComponent.getLastEvaluationTime()).isAfter(repositoryComponent.getLastEvaluationTime());

  }

  @Test
  public void testReevaluateComponent_UnknownHash() throws Exception {
    Repository repo = tempEntity.newRepository();
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.reevaluateComponent(repo.getId(), "missing-hash", null);
    }).withMessage("Cannot find a repository component for hash missing-hash in " + repo.getPublicId() + ".");
  }

  private RepositoryComponent createRepositoryPolicyViolation(final Repository repository,
                                                              final String pathname,
                                                              int... threatLevels)
  {
    return createRepositoryPolicyViolation(repository, pathname, false, threatLevels);
  }

  private RepositoryComponent createRepositoryPolicyViolation(final Repository repository,
                                                              final String pathname,
                                                              final boolean waived,
                                                              int... threatLevels)
  {
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), pathname);
    for (final int threatLevel : threatLevels) {
      tempEntity.newRepositoryPolicyViolation(repository.getId(), threatLevel, pathname, waived, null);
    }
    return component;
  }

  private void assertRepositoryReportDetail(final RepositoryReportDetail actualReportDetail,
                                            final String expectedPathname,
                                            final String expectedPolicyName,
                                            final int expectedThreatLevel,
                                            final boolean expectedHighestThreatLevel,
                                            final boolean isWaived)
  {
    assertThat(actualReportDetail.getPathname()).isEqualTo(expectedPathname);
    assertThat(actualReportDetail.getPolicyName()).isEqualTo(expectedPolicyName);
    assertThat(actualReportDetail.getThreatLevel()).isEqualTo(expectedThreatLevel);
    assertThat(actualReportDetail.isHighestThreatLevel()).isEqualTo(expectedHighestThreatLevel);

    assertThat(actualReportDetail.getHash()).isEqualTo("hash");
    assertThat(actualReportDetail.getMatchState()).isEqualTo("exact");
    assertThat(actualReportDetail.getComponentDisplayText()).isEqualTo("g : a : v");
    assertThat(actualReportDetail.getComponentIdentifier().getFormat()).isEqualTo("maven");
    assertThat(actualReportDetail.isQuarantined()).isFalse();
    assertThat(actualReportDetail.isWaived()).isEqualTo(isWaived);
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
    tempEntity.newRepositoryComponent(repository.getId(), "pathnameQuarantined",
        new Date(since + 1) /* quarantineTime */, null /* unquarantineTime */);
    // Component un-quarantined before the "since" time
    tempEntity.newRepositoryComponent(repository.getId(), "pathnameUnquarantinedBefore",
        new Date(since - 1) /* quarantineTime */, new Date(since - 1) /* unquarantineTime */);
    // Component un-quarantined after the "since" time
    tempEntity.newRepositoryComponent(repository.getId(), "pathnameUnquarantinedAfter",
        new Date(since) /* quarantineTime */, new Date(since) /* unquarantineTime */);
    UnquarantinedComponentList result = repositoryService.getUnquarantinedComponents(REPO_MAN_INSTANCE_ID,
        REPO_PUBLIC_ID, since);
    assertThat(result.pathnames).containsExactly("pathnameUnquarantinedAfter");
  }

  @Test
  public void testGetUnquarantinedComponents_RepositoryDoesNotExist() throws Exception {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repositoryService.getUnquarantinedComponents(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, 0);
    }).withMessage(RepositoryDAO.getErrMsgMissingRepo(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID));
  }

  @Test
  public void testGetIgnorePatterns() throws Exception {
    // Prepare request and mock the HDS request
    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put("foo", Collections.singletonList("bar"));
    when(hdsClient.get(eq(FirewallIgnorePatterns.class), eq(RepositoryService.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(hdsResult);

    // Call the service
    FirewallIgnorePatterns firewallIgnorePatterns = repositoryService.getIgnorePatterns();

    assertThat(firewallIgnorePatterns).isEqualTo(hdsResult);
  }

  @Test
  public void testRemoveComponent_SetsPolicyViolationsInactive() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), "pathname",
        new Date() /* quarantineTime */, null /* unquarantineTime */);
    RepositoryPolicyViolation policyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId(), "pathname");

    repositoryService.removeComponent(repository, repositoryComponent.getPathname());

    policyViolation = new RepositoryPolicyViolationDAO().getById(policyViolation.getId());
    assertThat(policyViolation.isActive()).isFalse();
  }

  @Test
  public void testRemoveComponent_PolicyViolationLogger_LogsFixEventForEachInactivatedViolation() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), "path1");
    RepositoryPolicyViolation activeRepositoryPolicyViolation1 = tempEntity
        .newRepositoryPolicyViolation(repository.getId(), repositoryComponent.getPathname());
    RepositoryPolicyViolation activeRepositoryPolicyViolation2 = tempEntity
        .newRepositoryPolicyViolation(repository.getId(), repositoryComponent.getPathname());
    RepositoryPolicyViolation inactiveRepositoryPolicyViolation = tempEntity
        .newRepositoryPolicyViolation(repository.getId(), repositoryComponent.getPathname());
    inactiveRepositoryPolicyViolation.setActive(false);
    repositoryPolicyViolationDAO.update(inactiveRepositoryPolicyViolation);
    RepositoryComponent otherRepositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), "path2");
    tempEntity.newRepositoryPolicyViolation(repository.getId(), otherRepositoryComponent.getPathname());

    Date before = new Date();
    repositoryService.removeComponent(repository, repositoryComponent.getPathname());
    Date after = new Date();

    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(policyViolationLoggerOutput, 2);
    PolicyViolationLogDTOAssert
        .assertRepositoryPolicyViolationData(policyViolationLogDTOs, PolicyViolationLogEvent.FIX, repository, before,
            after, Arrays.asList(activeRepositoryPolicyViolation1, activeRepositoryPolicyViolation2));
  }

  @Test
  public void testDeleteRepository_PolicyViolationLogger_RepositoryEnabled() throws Exception {
    Repository repository = tempEntity.newRepository();

    Date before = new Date();
    repositoryService.deleteRepository(repository.getId());
    Date after = new Date();

    List<PolicyViolationLogDTO> policyViolationLogDTOs =
        PolicyViolationLogDTOAssert.assertPolicyViolationLogDTOs(policyViolationLoggerOutput, 1);
    PolicyViolationLogDTOAssert.assertRepositoryPolicyViolationData(policyViolationLogDTOs.get(0),
        PolicyViolationLogEvent.CLEAR, repository, before, after);
  }

  @Test
  public void testDeleteRepository_PolicyViolationLogger_RepositoryDisabled() throws Exception {
    Repository repository = tempEntity.newRepository();
    repository.setEnabled(false);
    repositoryDAO.update(repository);

    repositoryService.deleteRepository(repository.getId());

    PolicyViolationLogDTOAssert.assertPolicyViolationLogDTOs(policyViolationLoggerOutput, 0);
  }

  @Test
  public void testSetEnabled_PolicyViolationLogger_DisabledLogsClearEvent() throws Exception {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    Date before = new Date();
    repositoryService.setEnabled(REPO_MAN_INSTANCE_ID, repository.getPublicId(), false);
    Date after = new Date();

    List<PolicyViolationLogDTO> policyViolationLogDTOs =
        PolicyViolationLogDTOAssert.assertPolicyViolationLogDTOs(policyViolationLoggerOutput, 1);
    PolicyViolationLogDTOAssert.assertRepositoryPolicyViolationData(policyViolationLogDTOs.get(0),
        PolicyViolationLogEvent.CLEAR, repository, before, after);
  }

  @Test
  public void testSetEnabled_PolicyViolationLogger_EnabledDoesNotLogClearEvent() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);

    repositoryService.setEnabled(REPO_MAN_INSTANCE_ID, repository.getPublicId(), true);

    assertThat(policyViolationLoggerOutput.getInfoMessages(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME))
        .isEmpty();
  }
}
