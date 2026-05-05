/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDTO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternFilter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dto.repository.RepositoriesDTO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.hds.FirewallAuditHdsClient;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternUpdater;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class RepositoryServiceTest
    extends AbstractComponentTest
{
  private static final String REPO_MAN_INSTANCE_ID = "repoManagerInstanceId";

  private static final String REPO_PUBLIC_ID = "repoPublicId";

  @Rule
  public LogOutput policyViolationLoggerOutput = new LogOutput(
      AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Inject
  private RepositoryService repositoryService;

  @Inject
  private ProprietaryComponentNameDetector proprietaryComponentNameDetector;

  @Inject
  private RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  private RepositoryDAO repositoryDAO;

  @Inject
  private RepositoryComponentDAO repositoryComponentDAO;

  @Inject
  private ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Mock
  private FirewallAuditHdsClient auditHdsClient;

  @Mock
  private HdsClient hdsClient;

  @Mock
  private FirewallQuarantineHdsClient quarantineHdsClient;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  private PolicyViolationLogDTOAssert policyViolationLogDTOAssert;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClient);
    binder.bind(FirewallAuditHdsClient.class).toInstance(auditHdsClient);
    binder.bind(FirewallQuarantineHdsClient.class).toInstance(quarantineHdsClient);
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    binder.bind(ShutdownHandler.class).toInstance(mockShutdownHandler);
    super.configure(binder);
  }

  @Before
  public void before() {
    policyViolationLogDTOAssert = new PolicyViolationLogDTOAssert(repositoryManagerDAO);

    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat = new HashMap<>();
    firewallIgnorePatterns.regexpsByRepositoryFormat.put("maven2", Collections.singletonList("a"));
    lenient().when(hdsClient.get(eq(FirewallIgnorePatterns.class),
        eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH))).thenReturn(firewallIgnorePatterns);
  }

  @After
  public void after() {
    // Restore the default value
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
  }

  @Test
  public void testUnquarantineComponent_WasQuarantined() throws Exception {
    String pathname = "path";
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), pathname, new Date(), null);

    mockHdsRequestForComponent(repositoryComponent, true);

    repositoryService.unquarantineComponent(repository.getId(), pathname, null);
    repositoryComponent = repositoryComponentDAO.getById(repositoryComponent.getId());

    assertThat(repositoryComponent.isQuarantined()).isFalse();
    assertThat(repositoryComponent.getAutoUnquarantined()).isFalse();
  }

  @Test
  public void testUnquarantineComponent_WasNotQuarantined() {
    String pathname = "path";
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), pathname, null, null);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryService.unquarantineComponent(repository.getId(), pathname, null))
        .withMessage("Component " + pathname + " in repository " + repository.getId() + " is not quarantined.");
    repositoryComponent = repositoryComponentDAO.getById(repositoryComponent.getId());

    assertThat(repositoryComponent.isQuarantined()).isFalse();
    assertThat(repositoryComponent.getAutoUnquarantined()).isNull();
  }

  @Test
  public void testUnquarantineComponent_WithViolations() throws Exception {
    String pathname = "path";
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), pathname, new Date(), null);

    createQuarantiningPolicy(repository);
    mockHdsRequestForComponent(repositoryComponent, true);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryService.unquarantineComponent(repository.getId(), pathname, null))
        .withMessage("Component " + pathname + " in repository " + repository.getId() + " has policy violations.");
  }

  @Test
  public void testUnquarantineComponent_WithViolationsNotFailed() throws Exception {
    String pathname = "path";
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID, "maven2");
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), pathname, new Date(), null);

    createQuarantiningPolicy(repository);
    mockHdsRequestForComponent(repositoryComponent, false);

    repositoryService.unquarantineComponent(repository.getId(), pathname, null);
    repositoryComponent = repositoryComponentDAO.getById(repositoryComponent.getId());

    assertThat(repositoryComponent.isQuarantined()).isFalse();
    assertThat(repositoryComponent.getAutoUnquarantined()).isFalse();
  }

  private void mockHdsRequestForComponent(
      RepositoryComponent repositoryComponent,
      boolean withSecurityVulnerabilities)
  {
    // Prepare request and mock the HDS request
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    RepositoryComponentEvaluationDataRequest repositoryComponentEvaluationDataRequest =
        new RepositoryComponentEvaluationDataRequest(
            "maven2", repositoryComponent.getPathname(), repositoryComponent.getHash());
    RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList =
        new RepositoryComponentEvaluationDataRequestList(
            RepositoryComponentEvaluationDataRequestList.REEVALUATION);
    componentEvaluationDataRequestList.components.add(repositoryComponentEvaluationDataRequest);

    List<SecurityVulnerability> securityVulnerabilities = new ArrayList<>();
    if (withSecurityVulnerabilities) {
      securityVulnerabilities = createSecurityVulnerabilities();
    }

    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(
        createComponentEvaluationData(componentIdentifier, repositoryComponent.getHash(), MatchState.EXACT,
            0 /* index */, Collections.emptySet(), Collections.emptySet(), securityVulnerabilities,
            0 /* popularity */));

    mockHdsRequest(componentEvaluationDataRequestList, hdsResult, false);
  }

  @Test
  public void testGetPolicyViolations() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Policy policy = tempEntity.newPolicy(RepositoryContainer.SINGLETON);
    String pathname = "path1";
    RepositoryComponent repositoryComponent1 = tempEntity.newRepositoryComponent(repository.getId(), pathname);
    RepositoryPolicyViolation repositoryPolicyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId(),
        8, pathname, true /* isWaived */, Action.ID_FAIL, policy.getId(), policy.getName(),
        repositoryComponent1.getComponentIdentifier());
    RepositoryComponent repositoryComponent2 = tempEntity.newRepositoryComponent(repository.getId(), "path2");
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "path2", false/* isWaived */, "policyId2",
        "policyName2", repositoryComponent2.getComponentIdentifier());

    List<RepositoryPolicyViolationDTO> repositoryPolicyViolationDTOs =
        repositoryService.getPolicyViolations(repository.getId(), pathname);

    assertThat(repositoryPolicyViolationDTOs).hasSize(1);
    RepositoryPolicyViolationDTO repositoryPolicyViolationDTO =
        repositoryPolicyViolationDTOs.get(0);
    assertThat(repositoryPolicyViolationDTO.policyViolationId).isEqualTo(repositoryPolicyViolation.getId());
    assertThat(repositoryPolicyViolationDTO.componentIdentifier.getFormat())
        .isEqualTo(repositoryComponent1.getComponentIdentifier().getFormat());
    assertThat(repositoryPolicyViolationDTO.componentIdentifier.getCoordinates())
        .isEqualTo(repositoryComponent1.getComponentIdentifier().getCoordinates());
    assertThat(repositoryPolicyViolationDTO.componentDisplayName.getName())
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(repositoryComponent1.getComponentIdentifier()).getName());
    assertThat(repositoryPolicyViolationDTO.hash).isEqualTo(repositoryPolicyViolation.getHash());
    assertThat(repositoryPolicyViolationDTO.policyId).isEqualTo(repositoryPolicyViolation.getPolicyId());
    assertThat(repositoryPolicyViolationDTO.policyName).isEqualTo(repositoryPolicyViolation.getPolicyName());
    assertThat(repositoryPolicyViolationDTO.policyOwner.ownerId).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(repositoryPolicyViolationDTO.policyOwner.ownerName).isEqualTo(RepositoryContainer.SINGLETON.getName());
    assertThat(repositoryPolicyViolationDTO.policyOwner.ownerType).isEqualTo(OwnerType.REPOSITORY_CONTAINER.toString());
    assertThat(repositoryPolicyViolationDTO.policyThreatLevel).isEqualTo(repositoryPolicyViolation.getThreatLevel());
    assertThat(repositoryPolicyViolationDTO.policyThreatCategory)
        .isEqualTo(repositoryPolicyViolation.getThreatCategory());
    assertThat(repositoryPolicyViolationDTO.constraintFactsJson)
        .isEqualTo(repositoryPolicyViolation.getConstraintFactsJson());
    assertThat(repositoryPolicyViolationDTO.waived).isEqualTo(repositoryPolicyViolation.isWaived());
    assertThat(repositoryPolicyViolationDTO.policyActionTypeId).isEqualTo(repositoryPolicyViolation.getActionTypeId());
    assertThat(repositoryPolicyViolationDTO.lastReported).isEqualTo(repositoryPolicyViolation.getTime());
  }

  @Test
  public void testGetPolicyViolations_PolicyDoesNotExist() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    String pathname = "path1";
    RepositoryComponent repositoryComponent1 = tempEntity.newRepositoryComponent(repository.getId(), pathname);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, pathname, false /* isWaived */, Action.ID_FAIL,
            "policyId1", "policyName1", repositoryComponent1.getComponentIdentifier());
    RepositoryComponent repositoryComponent2 = tempEntity.newRepositoryComponent(repository.getId(), "path2");
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "path2", false/* isWaived */, "policyId2",
        "policyName2", repositoryComponent2.getComponentIdentifier());

    List<RepositoryPolicyViolationDTO> repositoryPolicyViolationDTOs =
        repositoryService.getPolicyViolations(repository.getId(), pathname);

    assertThat(repositoryPolicyViolationDTOs).hasSize(1);
    RepositoryPolicyViolationDTO repositoryPolicyViolationDTO = repositoryPolicyViolationDTOs.get(0);
    assertThat(repositoryPolicyViolationDTO.policyViolationId).isEqualTo(repositoryPolicyViolation.getId());
    assertThat(repositoryPolicyViolationDTO.componentIdentifier.getFormat())
        .isEqualTo(repositoryComponent1.getComponentIdentifier().getFormat());
    assertThat(repositoryPolicyViolationDTO.componentIdentifier.getCoordinates())
        .isEqualTo(repositoryComponent1.getComponentIdentifier().getCoordinates());
    assertThat(repositoryPolicyViolationDTO.componentDisplayName.getName())
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(repositoryComponent1.getComponentIdentifier()).getName());
    assertThat(repositoryPolicyViolationDTO.hash).isEqualTo(repositoryPolicyViolation.getHash());
    assertThat(repositoryPolicyViolationDTO.policyId).isEqualTo(repositoryPolicyViolation.getPolicyId());
    assertThat(repositoryPolicyViolationDTO.policyName).isEqualTo(repositoryPolicyViolation.getPolicyName());
    assertThat(repositoryPolicyViolationDTO.policyOwner.ownerId).isNull();
    assertThat(repositoryPolicyViolationDTO.policyOwner.ownerName).isNull();
    assertThat(repositoryPolicyViolationDTO.policyOwner.ownerType).isNull();
    assertThat(repositoryPolicyViolationDTO.policyThreatLevel).isEqualTo(repositoryPolicyViolation.getThreatLevel());
    assertThat(repositoryPolicyViolationDTO.policyThreatCategory)
        .isEqualTo(repositoryPolicyViolation.getThreatCategory());
    assertThat(repositoryPolicyViolationDTO.constraintFactsJson)
        .isEqualTo(repositoryPolicyViolation.getConstraintFactsJson());
    assertThat(repositoryPolicyViolationDTO.waived).isEqualTo(repositoryPolicyViolation.isWaived());
    assertThat(repositoryPolicyViolationDTO.policyActionTypeId).isEqualTo(repositoryPolicyViolation.getActionTypeId());
    assertThat(repositoryPolicyViolationDTO.lastReported).isEqualTo(repositoryPolicyViolation.getTime());
  }

  @Test
  public void testGetPolicyViolations_RepositoryComponentDoesNotExist() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryService.getPolicyViolations(repository.getId(), "pathDoesNotExist"))
        .withMessage(
            "Cannot find a component with path pathDoesNotExist in repository with ID " + repository.getId() + ".");
  }

  @Test
  public void testGetPolicyViolation() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Policy policy = tempEntity.newPolicy(RepositoryContainer.SINGLETON);
    String pathname = "path1";
    RepositoryComponent repositoryComponent1 = tempEntity.newRepositoryComponent(repository.getId(), pathname);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, pathname, true /* isWaived */, Action.ID_FAIL,
            policy.getId(), policy.getName(), repositoryComponent1.getComponentIdentifier());

    RepositoryPolicyViolationDTO repositoryPolicyViolationDTO =
        repositoryService.getPolicyViolation(repository.getId(), repositoryPolicyViolation.getId());

    assertThat(repositoryPolicyViolationDTO.policyViolationId).isEqualTo(repositoryPolicyViolation.getId());
    assertThat(repositoryPolicyViolationDTO.componentIdentifier.getFormat())
        .isEqualTo(repositoryComponent1.getComponentIdentifier().getFormat());
    assertThat(repositoryPolicyViolationDTO.componentIdentifier.getCoordinates())
        .isEqualTo(repositoryComponent1.getComponentIdentifier().getCoordinates());
    assertThat(repositoryPolicyViolationDTO.componentDisplayName.getName())
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(repositoryComponent1.getComponentIdentifier()).getName());
    assertThat(repositoryPolicyViolationDTO.hash).isEqualTo(repositoryPolicyViolation.getHash());
    assertThat(repositoryPolicyViolationDTO.policyId).isEqualTo(repositoryPolicyViolation.getPolicyId());
    assertThat(repositoryPolicyViolationDTO.policyName).isEqualTo(repositoryPolicyViolation.getPolicyName());
    assertThat(repositoryPolicyViolationDTO.policyOwner.ownerId).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(repositoryPolicyViolationDTO.policyOwner.ownerName).isEqualTo(RepositoryContainer.SINGLETON.getName());
    assertThat(repositoryPolicyViolationDTO.policyOwner.ownerType).isEqualTo(OwnerType.REPOSITORY_CONTAINER.toString());
    assertThat(repositoryPolicyViolationDTO.policyThreatLevel).isEqualTo(repositoryPolicyViolation.getThreatLevel());
    assertThat(repositoryPolicyViolationDTO.policyThreatCategory)
        .isEqualTo(repositoryPolicyViolation.getThreatCategory());
    assertThat(repositoryPolicyViolationDTO.constraintFactsJson)
        .isEqualTo(repositoryPolicyViolation.getConstraintFactsJson());
    assertThat(repositoryPolicyViolationDTO.waived).isEqualTo(repositoryPolicyViolation.isWaived());
    assertThat(repositoryPolicyViolationDTO.policyActionTypeId).isEqualTo(repositoryPolicyViolation.getActionTypeId());
    assertThat(repositoryPolicyViolationDTO.lastReported).isEqualTo(repositoryPolicyViolation.getTime());
  }

  @Test
  public void testGetPolicyViolations_RepositoryPolicyViolationDoesNotExist() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryService.getPolicyViolation(repository.getId(), "fakeRepositoryPolicyViolationId"))
        .withMessage(
            "Cannot find a repository policy violation with ID fakeRepositoryPolicyViolationId in repository with ID "
                + repository.getId() + ".");
  }

  @Test
  public void testGetPolicyViolations_MismatchedIds() {
    Repository repository1 = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    Policy policy = tempEntity.newPolicy(RepositoryContainer.SINGLETON);
    String pathname = "path1";
    RepositoryComponent repositoryComponent1 = tempEntity.newRepositoryComponent(repository1.getId(), pathname);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository1.getId(), 8, pathname, false /* isWaived */, Action.ID_FAIL,
            policy.getId(), policy.getName(), repositoryComponent1.getComponentIdentifier());
    Repository repository2 = tempEntity.newRepository();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryService.getPolicyViolation(repository2.getId(), repositoryPolicyViolation.getId()))
        .withMessage("Cannot find a repository policy violation with ID " + repositoryPolicyViolation.getId()
            + " in repository with ID " + repository2.getId() + ".");
  }

  @Test
  public void testGetPolicyViolation_PolicyDoesNotExist() {
    Repository repository = tempEntity.newRepository(REPO_MAN_INSTANCE_ID, REPO_PUBLIC_ID);
    String pathname = "path1";
    RepositoryComponent repositoryComponent1 = tempEntity.newRepositoryComponent(repository.getId(), pathname);
    RepositoryPolicyViolation repositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, pathname, false /* isWaived */, Action.ID_FAIL,
            "policyId1", "policyName1", repositoryComponent1.getComponentIdentifier());

    RepositoryPolicyViolationDTO repositoryPolicyViolationDTO =
        repositoryService.getPolicyViolation(repository.getId(), repositoryPolicyViolation.getId());

    assertThat(repositoryPolicyViolationDTO.policyViolationId).isEqualTo(repositoryPolicyViolation.getId());
    assertThat(repositoryPolicyViolationDTO.componentIdentifier.getFormat())
        .isEqualTo(repositoryComponent1.getComponentIdentifier().getFormat());
    assertThat(repositoryPolicyViolationDTO.componentIdentifier.getCoordinates())
        .isEqualTo(repositoryComponent1.getComponentIdentifier().getCoordinates());
    assertThat(repositoryPolicyViolationDTO.componentDisplayName.getName())
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(repositoryComponent1.getComponentIdentifier()).getName());
    assertThat(repositoryPolicyViolationDTO.hash).isEqualTo(repositoryPolicyViolation.getHash());
    assertThat(repositoryPolicyViolationDTO.policyId).isEqualTo(repositoryPolicyViolation.getPolicyId());
    assertThat(repositoryPolicyViolationDTO.policyName).isEqualTo(repositoryPolicyViolation.getPolicyName());
    assertThat(repositoryPolicyViolationDTO.policyOwner.ownerId).isNull();
    assertThat(repositoryPolicyViolationDTO.policyOwner.ownerName).isNull();
    assertThat(repositoryPolicyViolationDTO.policyOwner.ownerType).isNull();
    assertThat(repositoryPolicyViolationDTO.policyThreatLevel).isEqualTo(repositoryPolicyViolation.getThreatLevel());
    assertThat(repositoryPolicyViolationDTO.policyThreatCategory)
        .isEqualTo(repositoryPolicyViolation.getThreatCategory());
    assertThat(repositoryPolicyViolationDTO.constraintFactsJson)
        .isEqualTo(repositoryPolicyViolation.getConstraintFactsJson());
    assertThat(repositoryPolicyViolationDTO.waived).isEqualTo(repositoryPolicyViolation.isWaived());
    assertThat(repositoryPolicyViolationDTO.policyActionTypeId).isEqualTo(repositoryPolicyViolation.getActionTypeId());
    assertThat(repositoryPolicyViolationDTO.lastReported).isEqualTo(repositoryPolicyViolation.getTime());
  }

  @Test
  public void testGetRepositorySummary() {
    Repository repo = tempEntity.newRepository();
    repo.setQuarantineEnabled(true);
    repositoryDAO.update(repo);

    // Component without violations
    tempEntity.newRepositoryComponent(repo.getId(), "no policy violations");
    // Components with active violations
    RepositoryComponent component1 = tempEntity.newRepositoryComponent(repo.getId(), "1");
    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repo.getId(), "2");
    RepositoryComponent component3 = tempEntity.newRepositoryComponent(repo.getId(), "3");
    RepositoryComponent component4 = tempEntity.newRepositoryComponent(repo.getId(), "4");
    // Component with waived violation
    RepositoryComponent component5 = tempEntity.newRepositoryComponent(repo.getId(), "5");
    // Unknown component
    tempEntity.newRepositoryComponent(repo.getId(), MatchState.UNKNOWN, null);
    // Quarantined component
    tempEntity.newRepositoryComponent(repo.getId(), "/quarantined", new Date(), null);

    // Threat level < 2 is not counted
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 1, component1.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 1, component2.getPathname(), null);
    // Moderate threat level
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 2, component2.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 3, component2.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 3, component3.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 3, component3.getPathname(), null);
    // Severe threat level
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 4, component3.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 5, component3.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 6, component3.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 6, component4.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 6, component4.getPathname(), null);
    // Critical threat level
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 8, component4.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 9, component4.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 9, component4.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 9, component4.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 9, component4.getPathname(), null);
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 10, component4.getPathname(), null);
    // Waived
    tempEntity.newRepositoryPolicyViolation(component5, 9, true /* waived */, "testPolicyName", FailActionType.ID);

    RepositorySummary summary = repositoryService.getRepositorySummary(repo.getId());

    assertThat(summary.knownComponentCount).isEqualTo(7);
    assertThat(summary.totalComponentCount).isEqualTo(8);
    assertThat(summary.criticalViolationCount).isEqualTo(6);
    assertThat(summary.severeViolationCount).isEqualTo(5);
    assertThat(summary.moderateViolationCount).isEqualTo(4);
    assertThat(summary.affectedComponentCount).isEqualTo(3);
    assertThat(summary.quarantinedComponentCount).isEqualTo(1);
  }

  /**
   * NEXUS-50206: Verify that quarantine count is 0 when quarantine is disabled,
   * even when components have quarantineTime set.
   */
  @Test
  public void testGetRepositorySummary_QuarantineDisabled() {
    Repository repo = tempEntity.newRepository();
    repo.setQuarantineEnabled(false);
    repositoryDAO.update(repo);

    // Component without violations
    tempEntity.newRepositoryComponent(repo.getId(), "no policy violations");
    // Component with violations
    RepositoryComponent component1 = tempEntity.newRepositoryComponent(repo.getId(), "1");
    // Quarantined component (has quarantineTime)
    tempEntity.newRepositoryComponent(repo.getId(), "/quarantined", new Date(), null);

    // Add some violations
    tempEntity.newRepositoryPolicyViolation(repo.getId(), 10, component1.getPathname(), null);

    RepositorySummary summary = repositoryService.getRepositorySummary(repo.getId());

    assertThat(summary.knownComponentCount).isEqualTo(3);
    assertThat(summary.totalComponentCount).isEqualTo(3);
    assertThat(summary.criticalViolationCount).isEqualTo(1);
    assertThat(summary.affectedComponentCount).isEqualTo(1);
    // Quarantine count should be 0 even though component has quarantineTime set
    assertThat(summary.quarantinedComponentCount).isEqualTo(0);
  }

  private void mockHdsRequest(
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
  public void testGetRepositoryById_UnknownId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryService.getRepositoryById("foobar"))
        .withMessage("Repository with ID foobar does not exist.");
  }

  @Test
  public void testReevaluateRepository() {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), DateUtils.addDays(new Date(), -1));

    ComponentEvaluationDataList response = new ComponentEvaluationDataList();
    ComponentEvaluationData component = new ComponentEvaluationData();
    component.hash = repositoryComponent.getHash();
    component.observedLicenses = Collections.emptySet();
    component.declaredLicenses = Collections.emptySet();
    component.matchState = MatchState.UNKNOWN.getId();
    response.components.add(component);
    when(auditHdsClient
        .post(any(), eq(ComponentEvaluationDataList.class), eq(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH),
            isNull(), any(RepositoryComponentEvaluationDataRequestList.class))).thenReturn(response);

    Date beforeEvaluation = new Date();
    repositoryService.reevaluateRepository(repository.getId());

    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      Date lastEvaluationTime = repositoryComponentDAO.getByRepositoryId(repository.getId())
          .get(0)
          .getLastEvaluationTime();
      assertThat(lastEvaluationTime).isAfterOrEqualTo(beforeEvaluation);
    });
  }

  @Test
  public void testReevaluateRepository_UnknownId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryService.reevaluateRepository("foobar"))
        .withMessage("Repository with ID foobar does not exist.");
  }

  @Test
  public void testDeleteRepository() {
    Repository repository = tempEntity.newRepository();
    repositoryService.deleteRepository(repository.getId());
    assertThat(repositoryDAO.getById(repository.getId())).isNull();
  }

  @Test
  public void testDeleteRepository_WithRelatedOrganization() {
    Repository repository = tempEntity.newRepository();
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    repository.setRelatedOrganizationId(organization.getId());
    repositoryDAO.update(repository);

    // Sanity check
    assertThat(repository.getRelatedOrganizationId()).isNotNull();

    repositoryService.deleteRepository(repository.getId());

    assertThat(repositoryDAO.getById(repository.getId())).isNull();
    assertThat(organizationDAO.getById(organization.getId())).isNull();
    assertThat(applicationDAO.getById(application.getId())).isNull();
  }

  @Test
  public void testDelete() {
    Repository repository = tempEntity.newRepository();
    repositoryService.delete(repository);
    assertThat(repositoryDAO.getById(repository.getId())).isNull();
  }

  @Test
  public void testDelete_WithRelatedOrganization() {
    Repository repository = tempEntity.newRepository();
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    repository.setRelatedOrganizationId(organization.getId());
    repositoryDAO.update(repository);

    // Sanity check
    assertThat(repository.getRelatedOrganizationId()).isNotNull();

    repositoryService.delete(repository);

    assertThat(repositoryDAO.getById(repository.getId())).isNull();
    assertThat(organizationDAO.getById(organization.getId())).isNull();
    assertThat(applicationDAO.getById(application.getId())).isNull();
  }

  @Test
  public void testDeleteRepository_UnknownId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryService.deleteRepository("foobar"))
        .withMessage("Repository with ID foobar does not exist.");
  }

  @Test
  public void testReevaluateComponent() {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), DateUtils.addDays(new Date(), -1));

    ComponentEvaluationDataList response = new ComponentEvaluationDataList();
    ComponentEvaluationData component = new ComponentEvaluationData();
    component.hash = repositoryComponent.getHash();
    component.observedLicenses = Collections.emptySet();
    component.declaredLicenses = Collections.emptySet();
    component.matchState = MatchState.UNKNOWN.getId();
    response.components.add(component);
    when(auditHdsClient
        .post(any(), eq(ComponentEvaluationDataList.class), eq(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH),
            isNull(), any(RepositoryComponentEvaluationDataRequestList.class))).thenReturn(response);

    repositoryService.reevaluateComponent(repository.getId(), repositoryComponent.getHash(), null);

    RepositoryComponent actualComponent = repositoryComponentDAO
        .getByRepositoryIdAndPathname(repository.getId(), repositoryComponent.getPathname());

    assertThat(actualComponent.getLastEvaluationTime()).isAfter(repositoryComponent.getLastEvaluationTime());
  }

  @Test
  public void testReevaluateComponent_UnknownHash() {
    Repository repo = tempEntity.newRepository();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryService.reevaluateComponent(repo.getId(), "missing-hash", null))
        .withMessage("Cannot find a repository component for hash missing-hash in " + repo.getPublicId() + ".");
  }

  @Test
  public void testGetRepositoriesWithReadPermissionByIds() {
    List<String> repoIds = IntStream.range(0, 10)
        .mapToObj(number -> tempEntity.newRepository().getId())
        .collect(Collectors.toList());

    String repoId1 = repoIds.get(0);
    String repoId2 = repoIds.get(1);

    List<Repository> actual =
        repositoryService.getRepositoriesWithReadPermissionByIds(new HashSet<>(Arrays.asList(repoId1, repoId2)));
    assertThat(actual).hasSize(2);

    List<String> actualIds = actual.stream().map(Repository::getId).collect(Collectors.toList());

    assertThat(repoId1).isIn(actualIds);
    assertThat(repoId2).isIn(actualIds);
  }

  @Test
  public void testGetRepositoriesWithReadPermissionByIds_Null() {
    List<String> repoIds = IntStream.range(0, 5)
        .mapToObj(number -> tempEntity.newRepository().getId())
        .collect(Collectors.toList());

    List<Repository> actual = repositoryService.getRepositoriesWithReadPermissionByIds(null);

    actual.forEach(repo -> assertThat(repo.getId()).isIn(repoIds));
  }

  @Test
  public void testGetRepositoriesWithReadPermissionByIds_Empty() {
    List<String> repoIds = IntStream.range(0, 5)
        .mapToObj(number -> tempEntity.newRepository().getId())
        .collect(Collectors.toList());

    List<Repository> actual = repositoryService.getRepositoriesWithReadPermissionByIds(Collections.emptySet());

    actual.forEach(repo -> assertThat(repo.getId()).isIn(repoIds));
  }

  private Policy createQuarantiningPolicy(Repository repository) {
    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);
    return policy;
  }

  @Test
  public void testDeleteRepository_PolicyViolationLogger_RepositoryEnabled() throws Exception {
    Repository repository = tempEntity.newRepository();

    Date before = new Date();
    repositoryService.deleteRepository(repository.getId());
    Date after = new Date();

    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(policyViolationLoggerOutput, 1);
    policyViolationLogDTOAssert
        .assertRepositoryPolicyViolationData(policyViolationLogDTOs.get(0), PolicyViolationLogEvent.CLEAR, repository,
            before, after);
  }

  @Test
  public void testDeleteRepository_PolicyViolationLogger_RepositoryDisabled() throws Exception {
    Repository repository = tempEntity.newRepository();
    repository.setAuditEnabled(false);
    repositoryDAO.update(repository);

    repositoryService.deleteRepository(repository.getId());

    PolicyViolationLogDTOAssert.assertPolicyViolationLogDTOs(policyViolationLoggerOutput, 0);
  }

  @Test
  public void testGetPolicyEvaluationTimestamps() {
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("testPackageId", "testVersion");
    Date firstPolicyEvaluationTime = new Date();
    Date lastEvaluationTime = new Date(System.currentTimeMillis() + 1000);
    Date quarantineTime = firstPolicyEvaluationTime;
    Date unquarantineTime = new Date(System.currentTimeMillis() + 2000);
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "testPathname", "testHash", componentIdentifier, firstPolicyEvaluationTime, quarantineTime, unquarantineTime);
    repositoryComponent.setLastEvaluationTime(lastEvaluationTime);
    repositoryComponent.setUnquarantineTimeForMonitoring(unquarantineTime);
    repositoryComponentDAO.update(repositoryComponent);

    PolicyEvaluationTimestampsDTO policyEvaluationTimestampsDTO =
        repositoryService.getPolicyEvaluationTimestamps(repository.getId(), componentIdentifier);

    assertThat(policyEvaluationTimestampsDTO.firstPolicyEvaluationTime).isEqualTo(firstPolicyEvaluationTime);
    assertThat(policyEvaluationTimestampsDTO.latestPolicyEvaluationTime).isEqualTo(lastEvaluationTime);
    assertThat(policyEvaluationTimestampsDTO.quarantineTime).isEqualTo(quarantineTime);
    assertThat(policyEvaluationTimestampsDTO.unquarantineTime).isEqualTo(unquarantineTime);
    assertThat(policyEvaluationTimestampsDTO.autoUnquarantined).isTrue();
  }

  @Test
  public void testGetPolicyEvaluationTimestamps_ComponentDoesNotExist() {
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("testPackageId", "testVersion");

    PolicyEvaluationTimestampsDTO policyEvaluationTimestampsDTO =
        repositoryService.getPolicyEvaluationTimestamps(repository.getId(), componentIdentifier);

    assertThat(policyEvaluationTimestampsDTO.firstPolicyEvaluationTime).isNull();
    assertThat(policyEvaluationTimestampsDTO.latestPolicyEvaluationTime).isNull();
    assertThat(policyEvaluationTimestampsDTO.quarantineTime).isNull();
    assertThat(policyEvaluationTimestampsDTO.unquarantineTime).isNull();
    assertThat(policyEvaluationTimestampsDTO.autoUnquarantined).isNull();
  }

  @Test
  public void testGetPolicyEvaluationTimestamps_MultipleComponents() {
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createConanCoordinates("libxml2", "2.9.14", null, null);

    Date firstPolicyEvaluationTime1 = new Date(0);
    Date lastEvaluationTime1 = new Date(firstPolicyEvaluationTime1.getTime() + 1000);
    Date quarantineTime1 = firstPolicyEvaluationTime1;
    Date unquarantineTime1 = new Date(firstPolicyEvaluationTime1.getTime() + 2000);

    Date firstPolicyEvaluationTime2 = new Date(10000);
    Date lastEvaluationTime2 = new Date(firstPolicyEvaluationTime2.getTime() + 1000);
    Date quarantineTime2 = firstPolicyEvaluationTime2;
    Date unquarantineTime2 = new Date(firstPolicyEvaluationTime2.getTime() + 2000);

    RepositoryComponent repositoryComponent1 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "testPathname1", "testHash", componentIdentifier, firstPolicyEvaluationTime1, quarantineTime1,
        unquarantineTime1);
    RepositoryComponent repositoryComponent2 = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "testPathname2", "testHash", componentIdentifier, firstPolicyEvaluationTime2, quarantineTime2,
        unquarantineTime2);

    repositoryComponent1.setLastEvaluationTime(lastEvaluationTime1);
    repositoryComponent1.setUnquarantineTimeForMonitoring(unquarantineTime1);
    repositoryComponentDAO.update(repositoryComponent1);
    repositoryComponent2.setLastEvaluationTime(lastEvaluationTime2);
    repositoryComponent2.setUnquarantineTimeForMonitoring(unquarantineTime2);
    repositoryComponentDAO.update(repositoryComponent2);

    PolicyEvaluationTimestampsDTO policyEvaluationTimestampsDTO =
        repositoryService.getPolicyEvaluationTimestamps(repository.getId(), componentIdentifier);

    assertThat(policyEvaluationTimestampsDTO.firstPolicyEvaluationTime).isEqualTo(firstPolicyEvaluationTime1);
    assertThat(policyEvaluationTimestampsDTO.latestPolicyEvaluationTime).isEqualTo(lastEvaluationTime2);
    assertThat(policyEvaluationTimestampsDTO.quarantineTime).isEqualTo(quarantineTime2);
    assertThat(policyEvaluationTimestampsDTO.unquarantineTime).isEqualTo(unquarantineTime2);
    assertThat(policyEvaluationTimestampsDTO.autoUnquarantined).isNull();
  }

  @Test
  public void testUpdateProprietaryComponentNamePattern() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_MAVEN);
    ProprietaryComponentNamePattern pattern =
        tempEntity.newProprietaryComponentNamePattern(repo, "testNamespacePattern", "testNamePattern");
    // Sanity check
    pattern = proprietaryComponentNamePatternDAO.getById(pattern.getId());
    assertThat(pattern.isEnabled()).isTrue();

    ProprietaryComponentNamePatternDTO proprietaryComponentNamePatternDTO =
        new ProprietaryComponentNamePatternDTO(pattern.getId(), pattern.getFormat(), pattern.getNamespacePattern(),
            pattern.getNamePattern(), repoManager.getInstanceId(), repoManager.getName(), repo.getPublicId(),
            false /* enabled */);

    repositoryService.updateProprietaryComponentNamePattern(proprietaryComponentNamePatternDTO);
    pattern = proprietaryComponentNamePatternDAO.getById(pattern.getId());
    assertThat(pattern.isEnabled()).isFalse();

    verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(proprietaryComponentNameDetector);
  }

  @Test
  public void testUpdateProprietaryComponentNamePattern_NullRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryService.updateProprietaryComponentNamePattern(null))
        .withMessage("Missing request parameters");
  }

  @Test
  public void testUpdateProprietaryComponentNamePattern_PatternDoesNotExist() {
    ProprietaryComponentNamePatternDTO proprietaryComponentNamePatternDTO = new ProprietaryComponentNamePatternDTO();
    proprietaryComponentNamePatternDTO.id = "does-not-exist";
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryService.updateProprietaryComponentNamePattern(proprietaryComponentNamePatternDTO))
        .withMessage("Cannot find a proprietary component name pattern with ID=does-not-exist");
  }

  @Test
  public void testGetUnconfiguredRepositoryManagers() {
    RepositoryManager configuredRepoManager = tempEntity.newRepositoryManager();
    configuredRepoManager.setConfigured(true);
    configuredRepoManager.setConfigureTime(new Date());
    repositoryManagerDAO.update(configuredRepoManager);

    RepositoryManager nexusUnconfiguredRepoManager = tempEntity.newRepositoryManager();
    nexusUnconfiguredRepoManager.setProductName("Nexus");
    nexusUnconfiguredRepoManager.setProductVersion("3.60.0");
    nexusUnconfiguredRepoManager.setUserAgent("Nexus/3.60.0-SNAPSHOT (PRO; Windows 10; 10.0; amd64; 1.8.0_352)");
    repositoryManagerDAO.update(nexusUnconfiguredRepoManager);

    RepositoryManager artifactoryUnconfiguredRepoManager = tempEntity.newRepositoryManager();
    artifactoryUnconfiguredRepoManager.setProductName("Firewall_For_Jfrog_Artifactory");
    artifactoryUnconfiguredRepoManager.setProductVersion("2.48.0");
    artifactoryUnconfiguredRepoManager.setUserAgent(
        "Firewall_For_Jfrog_Artifactory/2.48.0-SNAPSHOT (PRO; Windows 10; 10.0; amd64; 1.8.0_352)");
    repositoryManagerDAO.update(artifactoryUnconfiguredRepoManager);

    // By default, Firewall Onboarding is enabled
    // Disable Firewall Onboarding
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
    try {
      assertThat(repositoryService.getUnconfiguredRepositoryManagers().isEmpty());
    }
    finally {
      SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);
    }

    List<RepositoryManager> repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
    assertThat(repoManagers).hasSize(2);
    assertThat(repoManagers).usingRecursiveFieldByFieldElementComparator(
        RecursiveComparisonConfiguration.builder().withIgnoredFields(JPA.IGNORE_FIELDS).build())
        .containsExactlyInAnyOrder(nexusUnconfiguredRepoManager, artifactoryUnconfiguredRepoManager);
  }

  @Test
  public void testGetUnconfiguredRepositoryManagers_ProductNameAndVersion() {
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);
    RepositoryManager unconfiguredRepoManager = tempEntity.newRepositoryManager();
    unconfiguredRepoManager.setUserAgent("Nexus/3.60.0-SNAPSHOT (PRO; Windows 10; 10.0; amd64; 1.8.0_352)");
    repositoryManagerDAO.update(unconfiguredRepoManager);
    // Sanity checks
    assertThat(unconfiguredRepoManager.getProductName()).isNull();
    assertThat(unconfiguredRepoManager.getProductVersion()).isNull();

    // No product name and version
    List<RepositoryManager> repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
    assertThat(repoManagers).isEmpty();

    // Unsupported product name
    unconfiguredRepoManager.setProductName("Foo");
    repositoryManagerDAO.update(unconfiguredRepoManager);
    repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
    assertThat(repoManagers).isEmpty();

    // Nexus supported product name, but unsupported version
    unconfiguredRepoManager.setProductName("Nexus");
    unconfiguredRepoManager.setProductVersion("3.58.0");
    repositoryManagerDAO.update(unconfiguredRepoManager);
    repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
    assertThat(repoManagers).isEmpty();

    // Nexus supported product name and supported snapshot version
    unconfiguredRepoManager.setProductVersion("3.60.0-SNAPSHOT");
    repositoryManagerDAO.update(unconfiguredRepoManager);
    repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
    assertThat(repoManagers.get(0).getId()).isEqualTo(unconfiguredRepoManager.getId());
    assertThat(repoManagers.get(0).getInstanceId()).isEqualTo(unconfiguredRepoManager.getInstanceId());
    assertThat(repoManagers).hasSize(1);

    // Nexus supported product name and first supported release version
    unconfiguredRepoManager.setProductVersion("3.60.0");
    repositoryManagerDAO.update(unconfiguredRepoManager);
    repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
    assertThat(repoManagers.get(0).getId()).isEqualTo(unconfiguredRepoManager.getId());
    assertThat(repoManagers.get(0).getInstanceId()).isEqualTo(unconfiguredRepoManager.getInstanceId());
    assertThat(repoManagers).hasSize(1);

    unconfiguredRepoManager.setProductVersion("3.60.0-01");
    repositoryManagerDAO.update(unconfiguredRepoManager);
    repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
    assertThat(repoManagers.get(0).getId()).isEqualTo(unconfiguredRepoManager.getId());
    assertThat(repoManagers.get(0).getInstanceId()).isEqualTo(unconfiguredRepoManager.getInstanceId());
    assertThat(repoManagers).hasSize(1);

    // Nexus supported product name and later supported release version
    unconfiguredRepoManager.setProductVersion("3.60.1");
    repositoryManagerDAO.update(unconfiguredRepoManager);
    repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
    assertThat(repoManagers.get(0).getId()).isEqualTo(unconfiguredRepoManager.getId());
    assertThat(repoManagers.get(0).getInstanceId()).isEqualTo(unconfiguredRepoManager.getInstanceId());
    assertThat(repoManagers).hasSize(1);

    // Nexus supported product name and later supported release version
    unconfiguredRepoManager.setProductVersion("3.60.0");
    repositoryManagerDAO.update(unconfiguredRepoManager);
    repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
    assertThat(repoManagers.get(0).getId()).isEqualTo(unconfiguredRepoManager.getId());
    assertThat(repoManagers.get(0).getInstanceId()).isEqualTo(unconfiguredRepoManager.getInstanceId());
    assertThat(repoManagers).hasSize(1);

    // Artifactory supported product name, but unsupported snapshot version
    unconfiguredRepoManager.setProductName("Firewall_For_Jfrog_Artifactory");
    unconfiguredRepoManager.setProductVersion("2.4.7-SNAPSHOT");
    repositoryManagerDAO.update(unconfiguredRepoManager);
    repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
    assertThat(repoManagers).isEmpty();

    // Artifactory supported product name, but unsupported release version
    unconfiguredRepoManager.setProductName("Firewall_For_Jfrog_Artifactory");
    unconfiguredRepoManager.setProductVersion("2.4.7");
    repositoryManagerDAO.update(unconfiguredRepoManager);
    repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
    assertThat(repoManagers).isEmpty();

    // Artifactory supported product name and supported snapshot version
    unconfiguredRepoManager.setProductVersion("2.4.8-SNAPSHOT");
    repositoryManagerDAO.update(unconfiguredRepoManager);
    repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
    assertThat(repoManagers.get(0).getId()).isEqualTo(unconfiguredRepoManager.getId());
    assertThat(repoManagers.get(0).getInstanceId()).isEqualTo(unconfiguredRepoManager.getInstanceId());
    assertThat(repoManagers).hasSize(1);

    // Artifactory supported product name and first supported release version
    unconfiguredRepoManager.setProductVersion("2.4.8");
    repositoryManagerDAO.update(unconfiguredRepoManager);
    repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
    assertThat(repoManagers.get(0).getId()).isEqualTo(unconfiguredRepoManager.getId());
    assertThat(repoManagers.get(0).getInstanceId()).isEqualTo(unconfiguredRepoManager.getInstanceId());
    assertThat(repoManagers).hasSize(1);

    // Artifactory supported product name and later supported snapshot version
    unconfiguredRepoManager.setProductVersion("2.4.9-SNAPSHOT");
    repositoryManagerDAO.update(unconfiguredRepoManager);
    repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
    assertThat(repoManagers.get(0).getId()).isEqualTo(unconfiguredRepoManager.getId());
    assertThat(repoManagers.get(0).getInstanceId()).isEqualTo(unconfiguredRepoManager.getInstanceId());
    assertThat(repoManagers).hasSize(1);

    // Artifactory supported product name and later supported release version
    unconfiguredRepoManager.setProductVersion("2.4.9");
    repositoryManagerDAO.update(unconfiguredRepoManager);
    repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
    assertThat(repoManagers.get(0).getId()).isEqualTo(unconfiguredRepoManager.getId());
    assertThat(repoManagers.get(0).getInstanceId()).isEqualTo(unconfiguredRepoManager.getInstanceId());
    assertThat(repoManagers).hasSize(1);
  }

  private void assertProprietaryComponentNamePattern(
      ProprietaryComponentNamePatternDTO actual,
      ProprietaryComponentNamePattern expected)
  {
    Repository expectedRepository = repositoryDAO.getById(expected.getRepositoryId());
    RepositoryManager expectedRepositoryManager =
        repositoryManagerDAO.getById(expectedRepository.getRepositoryManagerId());

    assertThat(actual.namespacePattern).isEqualTo(expected.getNamespacePattern());
    assertThat(actual.namePattern).isEqualTo(expected.getNamePattern());
    assertThat(actual.id).isEqualTo(expected.getId());
    assertThat(actual.repositoryManagerInstanceId).isEqualTo(expectedRepositoryManager.getInstanceId());
    assertThat(actual.repositoryPublicId).isEqualTo(expectedRepository.getPublicId());
    assertThat(actual.format).isEqualTo(expected.getFormat());
    assertThat(actual.enabled).isEqualTo(expected.isEnabled());
  }

  @Test
  public void testGetRepositoriesByRepositoryManagerId() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository1 = tempEntity.newRepository(repositoryManager, "testRepoMaven", "maven2");
    Repository repository2 = tempEntity.newRepository(repositoryManager, "testRepoUnsupported", "unsupportedFormat");

    RepositoriesDTO result =
        repositoryService.getRepositoriesByRepositoryManagerId(repositoryManager.getId());

    List<String> repositoryIds =
        result.repositories.stream().map(dto -> dto.repository.getId()).collect(Collectors.toList());
    assertThat(repositoryIds).containsExactlyInAnyOrder(repository1.getId(), repository2.getId());
  }

  @Test
  public void testConfigureRepositories_NotExistingRepositoryManager() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryService.configureRepositories("repositoryManagerId",
            Collections.singletonList(new Repository())))
        .withMessage("Cannot find a repository manager with ID repositoryManagerId.");
  }

  @Test
  public void testConfigureRepositories_NotExistingRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = new Repository(repositoryManager.getId(), "testRepoName");

    // Call the service
    repositoryService.configureRepositories(repositoryManager.getId(), Collections.singletonList(repository));

    // No other updates in DB
    Repository repositoryInDB =
        repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(repositoryManager.getInstanceId(),
            repository.getPublicId());
    assertThat(repositoryInDB).isNull();
  }

  @Test
  public void testConfigureRepositories_ProxyRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testRepoName", RepositoryType.proxy, "maven2");
    repository.setAuditEnabled(false);
    repository.setQuarantineEnabled(false);
    repository.setPolicyCompliantComponentSelectionEnabled(false);
    repositoryDAO.update(repository);

    // Enable quarantine
    repository.setQuarantineEnabled(true);
    Date beforeConfig = new Date();
    // Call the service
    repositoryService.configureRepositories(repositoryManager.getId(), Collections.singletonList(repository));
    Date afterConfig = new Date();

    repository = repositoryDAO.getById(repository.getId());

    assertThat(repository.getName()).isEqualTo("testRepoName");
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();
    assertThat(repository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(repository.getLastManualConfigureTime()).isAfterOrEqualTo(beforeConfig).isBeforeOrEqualTo(afterConfig);

    // Disable quarantine
    repository.setQuarantineEnabled(false);
    beforeConfig = new Date();
    // Call the service
    repositoryService.configureRepositories(repositoryManager.getId(), Collections.singletonList(repository));
    afterConfig = new Date();

    repository = repositoryDAO.getById(repository.getId());

    assertThat(repository.getName()).isEqualTo("testRepoName");
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isFalse();
    assertThat(repository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(repository.getLastManualConfigureTime()).isAfterOrEqualTo(beforeConfig).isBeforeOrEqualTo(afterConfig);
  }

  @Test
  public void testConfigureRepositories_ProxyRepository_Npm() {
    // npm is a special case because we have to enable/disable policy compliant component selection too.
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testRepoName", RepositoryType.proxy, "npm");
    repository.setAuditEnabled(true);
    repository.setQuarantineEnabled(false);
    repository.setPolicyCompliantComponentSelectionEnabled(false);
    repositoryDAO.update(repository);

    // Enable quarantine
    repository.setQuarantineEnabled(true);
    Date beforeConfig = new Date();
    // Call the service
    repositoryService.configureRepositories(repositoryManager.getId(), Collections.singletonList(repository));
    Date afterConfig = new Date();

    repository = repositoryDAO.getById(repository.getId());

    assertThat(repository.getName()).isEqualTo("testRepoName");
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();
    assertThat(repository.isPolicyCompliantComponentSelectionEnabled()).isTrue();
    assertThat(repository.getLastManualConfigureTime()).isAfterOrEqualTo(beforeConfig).isBeforeOrEqualTo(afterConfig);

    // Disable quarantine
    repository.setQuarantineEnabled(false);
    beforeConfig = new Date();
    // Call the service
    repositoryService.configureRepositories(repositoryManager.getId(), Collections.singletonList(repository));
    afterConfig = new Date();

    repository = repositoryDAO.getById(repository.getId());

    assertThat(repository.getName()).isEqualTo("testRepoName");
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isFalse();
    assertThat(repository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(repository.getLastManualConfigureTime()).isAfterOrEqualTo(beforeConfig).isBeforeOrEqualTo(afterConfig);
  }

  @Test
  public void testConfigureRepositories_HostedRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testRepoName", RepositoryType.hosted, "npm");
    repository.setNamespaceConfusionProtectionEnabled(false);
    repositoryDAO.update(repository);

    // Enable NamespaceConfusionProtection
    repository.setNamespaceConfusionProtectionEnabled(true);
    Date beforeConfig = new Date();
    // Call the service
    repositoryService.configureRepositories(repositoryManager.getId(), Collections.singletonList(repository));
    Date afterConfig = new Date();

    repository = repositoryDAO.getById(repository.getId());

    assertThat(repository.getName()).isEqualTo("testRepoName");
    assertThat(repository.isNamespaceConfusionProtectionEnabled()).isTrue();
    assertThat(repository.getLastManualConfigureTime()).isAfterOrEqualTo(beforeConfig).isBeforeOrEqualTo(afterConfig);

    // Disable NamespaceConfusionProtection
    repository.setNamespaceConfusionProtectionEnabled(false);
    beforeConfig = new Date();
    // Call the service
    repositoryService.configureRepositories(repositoryManager.getId(), Collections.singletonList(repository));
    afterConfig = new Date();

    repository = repositoryDAO.getById(repository.getId());

    assertThat(repository.getName()).isEqualTo("testRepoName");
    assertThat(repository.isNamespaceConfusionProtectionEnabled()).isFalse();
    assertThat(repository.getLastManualConfigureTime()).isAfterOrEqualTo(beforeConfig).isBeforeOrEqualTo(afterConfig);
  }

  @Test
  public void testConfigureRepositories_ExceptionForOneRepositoryDoesNotStopProcessingOfOtherRepositories() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository existingRepository1 = tempEntity.newRepository(repositoryManager, "testRepoName1");
    Repository existingRepository2 = tempEntity.newRepository(repositoryManager, "testRepoName2");
    existingRepository1.setPublicId(existingRepository2.getPublicId());
    existingRepository1.setAuditEnabled(false);
    Repository existingRepository3 = tempEntity.newRepository(repositoryManager, "testRepoName3", "npm");
    existingRepository3.setQuarantineEnabled(true);
    existingRepository3.setPolicyCompliantComponentSelectionEnabled(true);

    Date beforeConfig = new Date();
    // Call the service
    repositoryService.configureRepositories(repositoryManager.getId(),
        Arrays.asList(existingRepository1, existingRepository3));
    Date afterConfig = new Date();

    Repository repository = repositoryDAO.getById(existingRepository3.getId());

    assertThat(repository.getName()).isEqualTo("testRepoName3");
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();
    assertThat(repository.isPolicyCompliantComponentSelectionEnabled()).isTrue();
    assertThat(repository.isNamespaceConfusionProtectionEnabled()).isFalse();
    assertThat(repository.getLastManualConfigureTime()).isAfterOrEqualTo(beforeConfig).isBeforeOrEqualTo(afterConfig);
  }

  @Test
  public void testConfigureRepositories_RepositoryManagerConfiguredStatus() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Date beforeConfig = new Date();
    // Call the service
    repositoryService.configureRepositories(repositoryManager.getId(), Collections.emptyList());
    Date afterConfig = new Date();

    repositoryManager = repositoryManagerDAO.getByInstanceId(repositoryManager.getInstanceId());
    assertThat(repositoryManager.isConfigured()).isTrue();
    assertThat(repositoryManager.getConfigureTime()).isAfterOrEqualTo(beforeConfig).isBeforeOrEqualTo(afterConfig);
  }

  @Test
  public void testSetPolicyAction_PolicyAtRootOrgLevel() {
    testSetPolicyAction(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testSetPolicyAction_PolicyAtRepositoryContainerLevel() {
    testSetPolicyAction(RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  private void testSetPolicyAction(String policyOwnerId) {
    Policy policy = tempEntity.newPolicy(policyOwnerId);
    // Sanity check
    assertThat(policy.getActions()).isEmpty();

    // Policy doesn't have any actions. Set action to FAIL for the proxy stage.
    repositoryService.setPolicyAction(policy.getName(), FailActionType.ID);
    Policy foundPolicy = policyDAO.getById(policy.getId());
    assertThat(foundPolicy.getActions()).hasSize(1);
    assertThat(foundPolicy.getActions().get(ProxyStageType.ID)).isEqualTo(FailActionType.ID);
    // Set it again - no changes
    repositoryService.setPolicyAction(policy.getName(), FailActionType.ID);
    foundPolicy = policyDAO.getById(policy.getId());
    assertThat(foundPolicy.getActions()).hasSize(1);
    assertThat(foundPolicy.getActions().get(ProxyStageType.ID)).isEqualTo(FailActionType.ID);

    // Policy has action FAIL for the proxy stage. Remove it.
    repositoryService.setPolicyAction(policy.getName(), null);
    foundPolicy = policyDAO.getById(policy.getId());
    assertThat(foundPolicy.getActions()).isEmpty();
    // Set it again - no changes
    repositoryService.setPolicyAction(policy.getName(), null);
    foundPolicy = policyDAO.getById(policy.getId());
    assertThat(foundPolicy.getActions()).isEmpty();

    // Policy has other actions. Don't touch them.
    foundPolicy.setAction(BuildStageType.ID, WarnActionType.ID);
    policyDAO.update(foundPolicy);
    repositoryService.setPolicyAction(policy.getName(), null);
    foundPolicy = policyDAO.getById(policy.getId());
    assertThat(foundPolicy.getActions()).hasSize(1);
    assertThat(foundPolicy.getActions().get(BuildStageType.ID)).isEqualTo(WarnActionType.ID);

    repositoryService.setPolicyAction(policy.getName(), FailActionType.ID);
    foundPolicy = policyDAO.getById(policy.getId());
    assertThat(foundPolicy.getActions()).hasSize(2);
    assertThat(foundPolicy.getActions().get(BuildStageType.ID)).isEqualTo(WarnActionType.ID);
    assertThat(foundPolicy.getActions().get(ProxyStageType.ID)).isEqualTo(FailActionType.ID);

    repositoryService.setPolicyAction(policy.getName(), null);
    foundPolicy = policyDAO.getById(policy.getId());
    assertThat(foundPolicy.getActions()).hasSize(1);
    assertThat(foundPolicy.getActions().get(BuildStageType.ID)).isEqualTo(WarnActionType.ID);
  }

  @Test
  public void testSetPolicyAction_PolicyDoesNotExist() {
    // Sanity check
    assertThat(policyDAO.getAll()).isEmpty();

    repositoryService.setPolicyAction("NoSuchPolicy", FailActionType.ID);

    assertThat(policyDAO.getAll()).isEmpty();
  }

  @Test
  public void testConfigureFirewallOnboarding() {
    FirewallOnboardingOptionsDTO firewallOnboardingOptionsDTO = new FirewallOnboardingOptionsDTO();
    firewallOnboardingOptionsDTO.supplyChainAttacksProtectionEnabled = true;
    firewallOnboardingOptionsDTO.namespaceConfusionProtectionEnabled = true;

    Policy securityMaliciousPolicy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Security-Malicious");
    Policy integrityRatingPolicy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Integrity-Rating");
    Policy securityNamespaceConflictPolicy =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Security-Namespace Conflict");
    // Sanity checks
    assertThat(securityMaliciousPolicy.getActions()).isEmpty();
    assertThat(integrityRatingPolicy.getActions()).isEmpty();
    assertThat(securityNamespaceConflictPolicy.getActions()).isEmpty();

    // Enable firewall onboarding options
    repositoryService.configureFirewallOnboarding(firewallOnboardingOptionsDTO);
    Policy foundPolicy = policyDAO.getById(securityMaliciousPolicy.getId());
    assertThat(foundPolicy.getActions()).hasSize(1);
    assertThat(foundPolicy.getActions().get(ProxyStageType.ID)).isEqualTo(FailActionType.ID);
    foundPolicy = policyDAO.getById(integrityRatingPolicy.getId());
    assertThat(foundPolicy.getActions()).hasSize(1);
    assertThat(foundPolicy.getActions().get(ProxyStageType.ID)).isEqualTo(FailActionType.ID);
    foundPolicy = policyDAO.getById(securityNamespaceConflictPolicy.getId());
    assertThat(foundPolicy.getActions()).hasSize(1);
    assertThat(foundPolicy.getActions().get(ProxyStageType.ID)).isEqualTo(FailActionType.ID);

    // Disable firewall onboarding options
    repositoryService.configureFirewallOnboarding(firewallOnboardingOptionsDTO);
    firewallOnboardingOptionsDTO.supplyChainAttacksProtectionEnabled = false;
    firewallOnboardingOptionsDTO.namespaceConfusionProtectionEnabled = false;

    repositoryService.configureFirewallOnboarding(firewallOnboardingOptionsDTO);
    foundPolicy = policyDAO.getById(securityMaliciousPolicy.getId());
    assertThat(foundPolicy.getActions()).isEmpty();
    foundPolicy = policyDAO.getById(integrityRatingPolicy.getId());
    assertThat(foundPolicy.getActions()).isEmpty();
    foundPolicy = policyDAO.getById(securityNamespaceConflictPolicy.getId());
    assertThat(foundPolicy.getActions()).isEmpty();
  }

  @Test
  public void testUpdateName_Success() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    repositoryService.updateName(repositoryManager.getId(), "Repo Name2");

    RepositoryManager foundRepositoryManager = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(foundRepositoryManager.getName()).isEqualTo("Repo Name2");
    assertThat(foundRepositoryManager.getNameLowercaseNoWhitespace()).isEqualTo("reponame2");
  }

  @Test
  public void testUpdateName_idNull() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryService.updateName(null, "Repo Name2"))
        .withMessage("RepositoryManager with ID null does not exist.");
  }

  @Test
  public void testGetRepositoryManagers() {
    RepositoryManager repoManagerOne = tempEntity.newRepositoryManager();
    RepositoryManager repoManagerTwo = tempEntity.newRepositoryManager();

    List<RepositoryManager> repoManagers = repositoryService.getRepositoryManagers();

    assertThat(repoManagers).usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(Arrays.asList(repoManagerOne, repoManagerTwo));
  }

  @Test
  public void testGetProprietaryComponentNamePatternsByOwner() {
    RepositoryManager repoManager1 = tempEntity.newRepositoryManager();
    Repository repo1 =
        tempEntity.newRepository(repoManager1, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_MAVEN);
    ProprietaryComponentNamePattern pattern1 =
        tempEntity.newProprietaryComponentNamePattern(repo1, "testNamespacePattern1", "testNamePattern1");
    ProprietaryComponentNamePattern pattern2 =
        tempEntity.newProprietaryComponentNamePattern(repo1, "testNamespacePattern2", "testNamePattern2");

    RepositoryManager repoManager2 = tempEntity.newRepositoryManager();
    Repository repo2 =
        tempEntity.newRepository(repoManager2, "testPublicId1", RepositoryType.hosted,
            ComponentIdentifier.FORMAT_MAVEN);
    ProprietaryComponentNamePattern pattern3 =
        tempEntity.newProprietaryComponentNamePattern(repo2, "testNamespacePattern3", "testNamePattern3");
    Repository repo3 =
        tempEntity.newRepository(repoManager2, "testPublicId2", RepositoryType.hosted,
            ComponentIdentifier.FORMAT_MAVEN);
    ProprietaryComponentNamePattern pattern4 =
        tempEntity.newProprietaryComponentNamePattern(repo3, "testNamespacePattern4", "testNamePattern4");

    ProprietaryComponentNamePatternRequest request = new ProprietaryComponentNamePatternRequest();
    request.page = 1;
    request.pageSize = 2;
    request.searchFilters = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SearchFilter(
        ProprietaryComponentNamePatternFilter.SearchFilter.FilterableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        "testNamePattern"));
    request.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        true /* asc */, 1 /* sortPriority */));

    // Repository Level - result must include only patterns of repo1
    ProprietaryComponentNamePatternsPage result =
        repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY, repo1.getId(), request);
    assertThat(result.hasNextPage).isFalse();
    assertThat(result.proprietaryComponentNamePatterns).hasSize(2);
    assertProprietaryComponentNamePattern(result.proprietaryComponentNamePatterns.get(0), pattern1);
    assertProprietaryComponentNamePattern(result.proprietaryComponentNamePatterns.get(1), pattern2);

    request.pageSize = 3;

    // Repository Manager Level - result must include only patterns of repos in repoManager2
    result =
        repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_MANAGER, repoManager2.getId(),
            request);
    assertThat(result.hasNextPage).isFalse();
    assertThat(result.proprietaryComponentNamePatterns).hasSize(2);
    assertProprietaryComponentNamePattern(result.proprietaryComponentNamePatterns.get(0), pattern3);
    assertProprietaryComponentNamePattern(result.proprietaryComponentNamePatterns.get(1), pattern4);

    // Repository Container Level - result must include patterns of all repos
    result =
        repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID, request);
    assertThat(result.hasNextPage).isTrue();
    assertThat(result.proprietaryComponentNamePatterns).hasSize(3);
    assertProprietaryComponentNamePattern(result.proprietaryComponentNamePatterns.get(0), pattern1);
    assertProprietaryComponentNamePattern(result.proprietaryComponentNamePatterns.get(1), pattern2);
    assertProprietaryComponentNamePattern(result.proprietaryComponentNamePatterns.get(2), pattern3);
  }

  @Test
  public void testGetProprietaryComponentNamePatternsByOwner_InvalidOwnerType() {
    ProprietaryComponentNamePatternRequest request = new ProprietaryComponentNamePatternRequest();
    request.page = 1;
    request.pageSize = 1;

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(
            () -> repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.APPLICATION, "invalid",
                request))
        .withMessage("Invalid owner type: " + OwnerType.APPLICATION);
  }

  @Test
  public void testGetProprietaryComponentNamePatternsByOwner_InvalidRepositoryManagerId() {
    ProprietaryComponentNamePatternRequest request = new ProprietaryComponentNamePatternRequest();
    request.page = 1;
    request.pageSize = 1;

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_MANAGER, "invalid",
                request))
        .withMessage("RepositoryManager with ID invalid does not exist.");
  }

  @Test
  public void testGetProprietaryComponentNamePatternsByOwner_InvalidRepositoryId() {
    ProprietaryComponentNamePatternRequest request = new ProprietaryComponentNamePatternRequest();
    request.page = 1;
    request.pageSize = 1;

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY, "invalid",
                request))
        .withMessage("Repository with ID invalid does not exist.");
  }

  @Test
  public void testGetProprietaryComponentNamePatternsByOwner_NullRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_MANAGER,
            "managerId", null))
        .withMessage("Missing request parameters");
  }

  @Test
  public void testGetProprietaryComponentNamePatternsByOwner_ValidatesPageNumber() {
    ProprietaryComponentNamePatternRequest request = new ProprietaryComponentNamePatternRequest();

    request.page = 0;
    request.pageSize = 1;
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_MANAGER,
            "managerId", request))
        .withMessage("Page and Page size must be greater than 0");

    request.page = -1;
    request.pageSize = 1;
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_MANAGER,
            "managerId", request))
        .withMessage("Page and Page size must be greater than 0");
  }

  @Test
  public void testGetProprietaryComponentNamePatternsByOwner_ValidatesPageSize() {
    ProprietaryComponentNamePatternRequest request = new ProprietaryComponentNamePatternRequest();

    request.page = 1;
    request.pageSize = 0;
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_MANAGER,
            "managerId", request))
        .withMessage("Page and Page size must be greater than 0");

    request.page = 1;
    request.pageSize = -1;
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_MANAGER,
            "managerId", request))
        .withMessage("Page and Page size must be greater than 0");
  }
}
