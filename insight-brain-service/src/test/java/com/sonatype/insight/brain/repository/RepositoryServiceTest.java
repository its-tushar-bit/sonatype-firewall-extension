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

import javax.inject.Inject;

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
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDTO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternFilter;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternFilter.SortField.SortableField;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.hds.FirewallAuditHdsClient;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternUpdater;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
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
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.apache.commons.lang3.time.DateUtils;
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

public class RepositoryServiceTest extends AbstractComponentTest
{
  private static final String MANUAL_REPO_MAN_INSTANCE_ID = "manualDeleteRepoManagerInstanceId";

  private static final String REPO_MAN_INSTANCE_ID = "repoManagerInstanceId";

  private static final String REPO_PUBLIC_ID = "repoPublicId";

  @Rule
  public LogOutput policyViolationLoggerOutput = new LogOutput(
      AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Inject
  private RepositoryService repositoryService;

  @Inject
  private ProprietaryComponentNameDetector proprietaryComponentNameDetector;

  private final RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  private static final RepositoryDAO repositoryDAO = new RepositoryDAO();

  private final RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private final ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO =
      new ProprietaryComponentNamePatternDAO();

  @Mock
  private FirewallAuditHdsClient auditHdsClient;

  @Mock
  private HdsClient hdsClient;

  @Mock
  private FirewallQuarantineHdsClient quarantineHdsClient;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClient);
    binder.bind(FirewallAuditHdsClient.class).toInstance(auditHdsClient);
    binder.bind(FirewallQuarantineHdsClient.class).toInstance(quarantineHdsClient);
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    super.configure(binder);
  }

  @Before
  public void before() {
    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat = new HashMap<>();
    firewallIgnorePatterns.regexpsByRepositoryFormat.put("maven2", Collections.singletonList("a"));
    lenient().when(hdsClient.get(eq(FirewallIgnorePatterns.class),
        eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH))).thenReturn(firewallIgnorePatterns);
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
        .withMessage("Cannot find a repository with ID foobar.");
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
      Date lastEvaluationTime = repositoryComponentDAO.getByRepositoryId(repository.getId()).get(0)
          .getLastEvaluationTime();
      assertThat(lastEvaluationTime).isAfterOrEqualTo(beforeEvaluation);
    });
  }

  @Test
  public void testReevaluateRepository_UnknownId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryService.reevaluateRepository("foobar"))
        .withMessage("Cannot find a repository with ID foobar.");
  }

  @Test
  public void testDeleteRepository() {
    Repository repository = tempEntity.newRepository();
    repositoryService.deleteRepository(repository.getId());
    assertThat(repositoryDAO.getById(repository.getId())).isNull();
  }

  @Test
  public void testDeleteRepository_UnknownId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> repositoryService.deleteRepository("foobar"))
        .withMessage("Cannot find a repository with ID foobar.");
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
  public void testGetRepositoriesByIds() {
    List<String> repoIds = IntStream.range(0, 10)
        .mapToObj(number -> tempEntity.newRepository().getId())
        .collect(Collectors.toList());

    String repoId1 = repoIds.get(0);
    String repoId2 = repoIds.get(1);

    Set<Repository> actual = repositoryService.getRepositoriesByIds(new HashSet<>(Arrays.asList(repoId1, repoId2)));
    assertThat(actual).hasSize(2);

    List<String> actualIds = actual.stream().map(Repository::getId).collect(Collectors.toList());

    assertThat(repoId1).isIn(actualIds);
    assertThat(repoId2).isIn(actualIds);
  }

  @Test
  public void testGetRepositoriesByIds_Null() {
    List<String> repoIds = IntStream.range(0, 5)
        .mapToObj(number -> tempEntity.newRepository().getId())
        .collect(Collectors.toList());

    Set<Repository> actual = repositoryService.getRepositoriesByIds(null);

    actual.forEach(repo -> assertThat(repo.getId()).isIn(repoIds));
  }

  @Test
  public void testGetRepositoriesByIds_Empty() {
    List<String> repoIds = IntStream.range(0, 5)
        .mapToObj(number -> tempEntity.newRepository().getId())
        .collect(Collectors.toList());

    Set<Repository> actual = repositoryService.getRepositoriesByIds(Collections.emptySet());

    actual.forEach(repo -> assertThat(repo.getId()).isIn(repoIds));
  }

  private Policy createQuarantiningPolicy(Repository repository) {
    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());
    policy.setAction(ProxyStageType.ID, Action.ID_FAIL);
    new PolicyDAO().update(policy);
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
    PolicyViolationLogDTOAssert
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
  public void testGetProprietaryComponentNamePatterns_NullRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryService.getProprietaryComponentNamePatterns(null))
        .withMessage("Missing request parameters");
  }

  @Test
  public void testGetProprietaryComponentNamePatterns_ValidatesPageNumber() {
    ProprietaryComponentNamePatternRequest request = new ProprietaryComponentNamePatternRequest();

    request.page = 0;
    request.pageSize = 1;
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryService.getProprietaryComponentNamePatterns(request))
        .withMessage("Page and Page size must be greater than 0");

    request.page = -1;
    request.pageSize = 1;
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryService.getProprietaryComponentNamePatterns(request))
        .withMessage("Page and Page size must be greater than 0");
  }

  @Test
  public void testGetProprietaryComponentNamePatterns_ValidatesPageSize() {
    ProprietaryComponentNamePatternRequest request = new ProprietaryComponentNamePatternRequest();

    request.page = 1;
    request.pageSize = 0;
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryService.getProprietaryComponentNamePatterns(request))
        .withMessage("Page and Page size must be greater than 0");

    request.page = 1;
    request.pageSize = -1;
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> repositoryService.getProprietaryComponentNamePatterns(request))
        .withMessage("Page and Page size must be greater than 0");
  }

  @Test
  public void testGetProprietaryComponentNamePatterns() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_MAVEN);
    ProprietaryComponentNamePattern pattern1 =
        tempEntity.newProprietaryComponentNamePattern(repo, "testNamespacePattern1", "testNamePattern1");
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(repo,
        "testNamespacePattern2", "testNamePattern2", false /* enabled */);

    // Result must indicate next page exists
    ProprietaryComponentNamePatternRequest request = new ProprietaryComponentNamePatternRequest();
    request.page = 1;
    request.pageSize = 1;
    request.searchFilters = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SearchFilter(
        ProprietaryComponentNamePatternFilter.SearchFilter.FilterableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        "testNamePattern"));
    request.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        true /* asc */, 1 /* sortPriority */));

    ProprietaryComponentNamePatternsPage result = repositoryService.getProprietaryComponentNamePatterns(request);
    assertThat(result.hasNextPage).isTrue();
    assertThat(result.proprietaryComponentNamePatterns).hasSize(1);
    assertProprietaryComponentNamePattern(result.proprietaryComponentNamePatterns.get(0), pattern1);

    // Result must indicate next page doesn't exist
    request.page = 1;
    request.pageSize = 2;
    request.searchFilters = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SearchFilter(
        ProprietaryComponentNamePatternFilter.SearchFilter.FilterableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        "testNamePattern"));
    request.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        true /* asc */, 1 /* sortPriority */));

    result = repositoryService.getProprietaryComponentNamePatterns(request);
    assertThat(result.hasNextPage).isFalse();
    assertThat(result.proprietaryComponentNamePatterns).hasSize(2);
    assertProprietaryComponentNamePattern(result.proprietaryComponentNamePatterns.get(0), pattern1);
    assertProprietaryComponentNamePattern(result.proprietaryComponentNamePatterns.get(1), pattern2);
  }

  @Test
  public void testGetProprietaryComponentNamePatterns_sortByEnabled() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_MAVEN);
    ProprietaryComponentNamePattern pattern1 =
        tempEntity.newProprietaryComponentNamePattern(repo, "testNamespacePattern1", "testNamePattern1", true);
    ProprietaryComponentNamePattern pattern2 = tempEntity.newProprietaryComponentNamePattern(repo,
        "testNamespacePattern2", "testNamePattern2", false /* enabled */);

    ProprietaryComponentNamePatternRequest request = new ProprietaryComponentNamePatternRequest();
    request.page = 1;
    request.pageSize = 2;
    request.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        SortableField.ENABLED, true /* asc */, 1 /* sortPriority */));

    ProprietaryComponentNamePatternsPage result = repositoryService.getProprietaryComponentNamePatterns(request);
    assertThat(result.proprietaryComponentNamePatterns).hasSize(2);
    assertProprietaryComponentNamePattern(result.proprietaryComponentNamePatterns.get(0), pattern2);
    assertProprietaryComponentNamePattern(result.proprietaryComponentNamePatterns.get(1), pattern1);
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
            pattern.getNamePattern(), repoManager.getInstanceId(), repo.getPublicId(), false /* enabled */);

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

    RepositoryManager unconfiguredRepoManager = tempEntity.newRepositoryManager();
    unconfiguredRepoManager.setUserAgent("Nexus/3.56.0-SNAPSHOT (PRO; Windows 10; 10.0; amd64; 1.8.0_352)");
    repositoryManagerDAO.update(unconfiguredRepoManager);

    // By default, Firewall Onboarding is disabled
    assertThat(repositoryService.getUnconfiguredRepositoryManagers().isEmpty());

    // Enable Firewall Onboarding
    SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(true);
    try {
      List<RepositoryManager> repoManagers = repositoryService.getUnconfiguredRepositoryManagers();
      assertThat(repoManagers.get(0).getId()).isEqualTo(unconfiguredRepoManager.getId());
      assertThat(repoManagers.get(0).getInstanceId()).isEqualTo(unconfiguredRepoManager.getInstanceId());
      assertThat(repoManagers).hasSize(1);
    }
    finally {
      SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.setEnabled(false);
      // Sanity check
      assertThat(repositoryService.getUnconfiguredRepositoryManagers().isEmpty());
    }
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

    List<Repository> repositories =
        repositoryService.getRepositoriesByRepositoryManagerId(repositoryManager.getId());

    assertThat(repositories).extracting(Repository::getId) //
        .containsExactlyInAnyOrder(repository1.getId(), repository2.getId());
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
}
