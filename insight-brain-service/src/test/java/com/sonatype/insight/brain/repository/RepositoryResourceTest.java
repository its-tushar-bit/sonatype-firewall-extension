/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDTO;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternFilter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dto.repository.RepositoriesDTO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.organization.IconUtils;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryResourceTest
    extends AbstractResourceTest
{
  private ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO;

  private RepositoryManagerDAO repositoryManagerDAO;

  private RepositoryDAO repositoryDAO;

  private PolicyDAO policyDAO;

  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Before
  public void setUp() {
    proprietaryComponentNamePatternDAO = lookup(ProprietaryComponentNamePatternDAO.class);
    repositoryManagerDAO = lookup(RepositoryManagerDAO.class);
    repositoryDAO = lookup(RepositoryDAO.class);
    policyDAO = lookup(PolicyDAO.class);
    systemConfigurationPropertyDAO = lookup(SystemConfigurationPropertyDAO.class);
  }

  @Test
  public void testUnquarantineComponent() throws Exception {
    Repository repo = tempEntity.newRepository();
    String path = "dir/path";
    HttpResponse response = restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.UNQUARANTINE_PATH)
        .parameter(repo.getId(), path)
        .post();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find a component with path " + path + " in repository with ID " + repo.getId() + ".");
  }

  @Test
  public void testGetRepositories() throws Exception {
    Repository repo = tempEntity.newRepository();

    HttpResponse response = restRequest().path(RepositoryResource.RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    RepositoriesDTO actual = response.getBody(RepositoriesDTO.class);

    assertThat(actual.repositories).hasSize(1);
    RepositoryDTO actualRepo = actual.repositories.get(0);
    assertThat(actualRepo.repository.getId()).isEqualTo(repo.getId());
    assertThat(actualRepo.repository.getPublicId()).isEqualTo(repo.getPublicId());
    assertThat(actualRepo.managerInstanceId)
        .isEqualTo(repositoryManagerDAO.getById(repo.getRepositoryManagerId()).getInstanceId());
  }

  @Test
  public void testGetRepository() throws Exception {
    Repository repo = tempEntity.newRepository();

    HttpResponse response = restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORY_PATH)
        .parameter(repo.getId())
        .get();
    assertResponseStatus(200, response);
    RepositoryDTO actual = response.getBody(RepositoryDTO.class);

    assertThat(actual.repository).isNotNull();
    assertThat(actual.repository.getId()).isEqualTo(repo.getId());
    assertThat(actual.repository.getPublicId()).isEqualTo(repo.getPublicId());
    assertThat(actual.managerInstanceId)
        .isEqualTo(repositoryManagerDAO.getById(repo.getRepositoryManagerId()).getInstanceId());
  }

  @Test
  public void testReevaluateRepository() throws Exception {
    Repository repo = tempEntity.newRepository();

    HttpResponse response = restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.EVALUATE_PATH)
        .parameter(repo.getId())
        .post();
    assertResponseStatus(204, response);
  }

  @Test
  public void testDeleteRepository() throws Exception {
    Repository repo = tempEntity.newRepository();

    HttpResponse deleteResponse = restRequest()
        .path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORY_PATH)
        .parameter(repo.getId())
        .delete();
    assertResponseStatus(204, deleteResponse);
    assertThat(repositoryDAO.getById(repo.getId())).isNull();
  }

  @Test
  public void testReevaluateRepositoryComponent() throws Exception {
    Repository repo = tempEntity.newRepository();
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());

    // Setup the mocked hds return
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = component.getHash();
    componentEvaluationData.matchState = MatchState.EXACT.getId();
    componentEvaluationData.declaredLicenses = new HashSet<>();
    componentEvaluationData.observedLicenses = new HashSet<>();
    hdsResult.components.add(componentEvaluationData);
    getHdsServer().respondWith(hdsResult).atUri("/rest/component/details/firewall");

    HttpResponse response = restRequest()
        .path(RepositoryResource.RESOURCE_PATH, RepositoryResource.EVALUATE_COMPONENT_PATH)
        .parameter(repo.getId(), component.getHash())
        .post();
    assertResponseStatus(204, response);
  }

  @Test
  public void testGetPolicyEvaluationTimestamps() throws Exception {
    Repository repo = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("testPackageId", "testVersion");
    Date firstPolicyEvaluationTime = new Date();
    Date quarantineTime = new Date();
    Date unquarantineTime = new Date();
    tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT, "testPathname", "testHash", componentIdentifier,
        firstPolicyEvaluationTime, quarantineTime, unquarantineTime);

    HttpResponse response =
        restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.POLICY_EVALUATION_TIMESTAMPS_PATH)
            .parameter(repo.getId())
            .query("componentIdentifier", componentIdentifier)
            .get();
    assertResponseStatus(200, response);
    PolicyEvaluationTimestampsDTO policyEvaluationTimestampsDTO = response.getBody(PolicyEvaluationTimestampsDTO.class);

    assertThat(policyEvaluationTimestampsDTO.firstPolicyEvaluationTime).isEqualTo(firstPolicyEvaluationTime);
    assertThat(policyEvaluationTimestampsDTO.latestPolicyEvaluationTime).isEqualTo(firstPolicyEvaluationTime);
    assertThat(policyEvaluationTimestampsDTO.quarantineTime).isEqualTo(quarantineTime);
    assertThat(policyEvaluationTimestampsDTO.unquarantineTime).isEqualTo(unquarantineTime);
    assertThat(policyEvaluationTimestampsDTO.autoUnquarantined).isFalse();
  }

  @Test
  public void testGetPolicyViolations() throws Exception {
    Repository repo = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(RepositoryContainer.SINGLETON);
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repo.getId(), "testPathname");
    RepositoryPolicyViolation repositoryPolicyViolation = tempEntity.newRepositoryPolicyViolation(repo.getId(), 8,
        repositoryComponent.getPathname(), false /* isWaived */, Action.ID_FAIL, policy.getId(), policy.getName(),
        repositoryComponent.getComponentIdentifier());

    HttpResponse response =
        restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.POLICY_VIOLATIONS_PATH)
            .parameter(repo.getId(), repositoryComponent.getPathname())
            .get();

    assertResponseStatus(200, response);
    RepositoryPolicyViolationDTO[] repositoryPolicyViolationDTOs =
        response.getBody(RepositoryPolicyViolationDTO[].class);
    assertThat(repositoryPolicyViolationDTOs).hasSize(1);
    RepositoryPolicyViolationDTO repositoryPolicyViolationDTO = repositoryPolicyViolationDTOs[0];
    assertThat(repositoryPolicyViolationDTO.policyViolationId).isEqualTo(repositoryPolicyViolation.getId());
    assertThat(repositoryPolicyViolationDTO.componentIdentifier.getFormat())
        .isEqualTo(repositoryComponent.getComponentIdentifier().getFormat());
    assertThat(repositoryPolicyViolationDTO.componentIdentifier.getCoordinates())
        .isEqualTo(repositoryComponent.getComponentIdentifier().getCoordinates());
    assertThat(repositoryPolicyViolationDTO.componentDisplayName.getName())
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(repositoryComponent.getComponentIdentifier()).getName());
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
  public void testGetPolicyViolation() throws Exception {
    Repository repo = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(RepositoryContainer.SINGLETON);
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repo.getId(), "testPathname");
    RepositoryPolicyViolation repositoryPolicyViolation = tempEntity.newRepositoryPolicyViolation(repo.getId(), 8,
        repositoryComponent.getPathname(), false /* isWaived */, Action.ID_FAIL, policy.getId(), policy.getName(),
        repositoryComponent.getComponentIdentifier());

    HttpResponse response =
        restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.POLICY_VIOLATION_PATH)
            .parameter(repo.getId(), repositoryPolicyViolation.getId())
            .get();

    assertResponseStatus(200, response);
    RepositoryPolicyViolationDTO repositoryPolicyViolationDTO = response.getBody(RepositoryPolicyViolationDTO.class);
    assertThat(repositoryPolicyViolationDTO.policyViolationId).isEqualTo(repositoryPolicyViolation.getId());
    assertThat(repositoryPolicyViolationDTO.componentIdentifier.getFormat())
        .isEqualTo(repositoryComponent.getComponentIdentifier().getFormat());
    assertThat(repositoryPolicyViolationDTO.componentIdentifier.getCoordinates())
        .isEqualTo(repositoryComponent.getComponentIdentifier().getCoordinates());
    assertThat(repositoryPolicyViolationDTO.componentDisplayName.getName())
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(repositoryComponent.getComponentIdentifier()).getName());
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
  public void testUpdateProprietaryComponentNamePattern() throws Exception {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_MAVEN);
    ProprietaryComponentNamePattern pattern = tempEntity.newProprietaryComponentNamePattern(
        repo, "testNamespacePattern", "testNamePattern");
    ProprietaryComponentNamePatternDTO proprietaryComponentNamePatternDTO =
        new ProprietaryComponentNamePatternDTO(pattern.getId(), pattern.getFormat(), pattern.getNamespacePattern(),
            pattern.getNamePattern(), repoManager.getInstanceId(), repoManager.getName(), repo.getPublicId(),
            false /* enabled */);
    // Sanity check
    assertThat(proprietaryComponentNamePatternDAO.getById(pattern.getId()).isEnabled()).isTrue();

    HttpResponse response =
        restRequest()
            .path(RepositoryResource.RESOURCE_PATH, RepositoryResource.PROPRIETARY_COMPONENT_NAME_PATTERN_UPDATE_PATH)
            .body(proprietaryComponentNamePatternDTO)
            .post();

    assertResponseStatus(204, response);
    assertThat(proprietaryComponentNamePatternDAO.getById(pattern.getId()).isEnabled()).isFalse();
  }

  @Test
  public void testGetUnconfiguredRepositoryManagers() throws Exception {
    systemConfigurationPropertyDAO.set(
        SystemConfigurationPropertyFeature.INTERNAL_FIREWALL_ONBOARDING_ENABLED.getPropertyName(),
        Boolean.TRUE.toString());

    RepositoryManager configuredRepoManager = tempEntity.newRepositoryManager();
    configuredRepoManager.setConfigured(true);
    configuredRepoManager.setConfigureTime(new Date());
    repositoryManagerDAO.update(configuredRepoManager);

    RepositoryManager unconfiguredRepoManager = tempEntity.newRepositoryManager();
    unconfiguredRepoManager.setUserAgent("Nexus/3.60.0-01 (PRO; Windows 10; 10.0; amd64; 1.8.0_352)");
    unconfiguredRepoManager.setProductName("Nexus");
    unconfiguredRepoManager.setProductVersion("3.60.0-01");
    repositoryManagerDAO.update(unconfiguredRepoManager);

    HttpResponse response = restRequest()
        .path(RepositoryResource.RESOURCE_PATH, RepositoryResource.UNCONFIGURED_REPOSITORY_MANAGERS_PATH)
        .get();

    assertResponseStatus(200, response);

    RepositoryManager[] repoManagers = response.getBody(RepositoryManager[].class);
    assertThat(repoManagers[0].getId()).isEqualTo(unconfiguredRepoManager.getId());
    assertThat(repoManagers[0].getInstanceId()).isEqualTo(unconfiguredRepoManager.getInstanceId());
    assertThat(repoManagers).hasSize(1);
  }

  @Test
  public void testGetRepositoriesByRepositoryManagerId() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository1 = tempEntity.newRepository(repositoryManager, "testRepoMaven", "maven2");
    Repository repository2 = tempEntity.newRepository(repositoryManager, "testRepoUnsupported", "unsupportedFormat");

    HttpResponse response =
        restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORIES_PATH)
            .parameter(repositoryManager.getId())
            .get();

    assertResponseStatus(200, response);
    RepositoriesDTO result = response.getBody(RepositoriesDTO.class);
    List<String> repositoryIds =
        result.repositories.stream().map(dto -> dto.repository.getId()).collect(Collectors.toList());
    assertThat(repositoryIds).containsExactlyInAnyOrder(repository1.getId(), repository2.getId());
  }

  @Test
  public void testConfigureRepositories() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testRepoName", "npm");
    repository.setQuarantineEnabled(true);
    repository.setPolicyCompliantComponentSelectionEnabled(true);

    Date beforeConfig = new Date();
    HttpResponse response =
        restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.CONFIGURE_REPOSITORIES_PATH)
            .parameter(repositoryManager.getId())
            .body(Collections.singletonList(repository))
            .put();
    Date afterConfig = new Date();

    assertResponseStatus(204, response);

    repositoryManager = repositoryManagerDAO.getByInstanceIdNotNull(repositoryManager.getInstanceId());
    assertThat(repositoryManager.isConfigured()).isEqualTo(true);
    assertThat(repositoryManager.getConfigureTime()).isAfterOrEqualTo(beforeConfig).isBeforeOrEqualTo(afterConfig);

    repository = repositoryDAO.getById(repository.getId());
    assertThat(repository.getName()).isEqualTo("testRepoName");
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.isQuarantineEnabled()).isTrue();
    assertThat(repository.isPolicyCompliantComponentSelectionEnabled()).isTrue();
    assertThat(repository.isNamespaceConfusionProtectionEnabled()).isFalse();
    assertThat(repository.getLastManualConfigureTime()).isAfterOrEqualTo(beforeConfig).isBeforeOrEqualTo(afterConfig);
  }

  @Test
  public void testConfigureFirewallOnboarding() throws Exception {
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

    HttpResponse response =
        restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.CONFIGURE_FIREWALL_ONBOARDING_PATH)
            .body(firewallOnboardingOptionsDTO)
            .put();
    assertResponseStatus(204, response);

    Policy foundPolicy = policyDAO.getById(securityMaliciousPolicy.getId());
    assertThat(foundPolicy.getActions()).hasSize(1);
    assertThat(foundPolicy.getActions().get(ProxyStageType.ID)).isEqualTo(FailActionType.ID);
    foundPolicy = policyDAO.getById(integrityRatingPolicy.getId());
    assertThat(foundPolicy.getActions()).hasSize(1);
    assertThat(foundPolicy.getActions().get(ProxyStageType.ID)).isEqualTo(FailActionType.ID);
    foundPolicy = policyDAO.getById(securityNamespaceConflictPolicy.getId());
    assertThat(foundPolicy.getActions()).hasSize(1);
    assertThat(foundPolicy.getActions().get(ProxyStageType.ID)).isEqualTo(FailActionType.ID);
  }

  @Test
  public void testUpdateName() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    HttpResponse response = restRequest().path(RepositoryResource.RESOURCE_PATH,
        RepositoryResource.UPDATE_REPOSITORY_MANAGER_NAME_PATH)
        .parameter(repositoryManager.getId(), "name2")
        .put();

    assertResponseStatus(204, response);

    RepositoryManager repositoryManagerResult = repositoryManagerDAO.getById(repositoryManager.getId());

    assertThat(repositoryManagerResult.getName()).isEqualTo("name2");
  }

  @Test
  public void testGetProprietaryComponentNamePatternsByOwner() throws Exception {
    RepositoryManager repoManager1 = tempEntity.newRepositoryManager();
    Repository repo1 =
        tempEntity.newRepository(repoManager1, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_MAVEN);
    ProprietaryComponentNamePattern pattern1 =
        tempEntity.newProprietaryComponentNamePattern(repo1, "testNamespacePattern1", "testNamePattern1");

    RepositoryManager repoManager2 = tempEntity.newRepositoryManager();
    Repository repo2 =
        tempEntity.newRepository(repoManager2, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_MAVEN);
    ProprietaryComponentNamePattern pattern2 =
        tempEntity.newProprietaryComponentNamePattern(repo2, "testNamespacePattern2", "testNamePattern2");

    ProprietaryComponentNamePatternRequest request = new ProprietaryComponentNamePatternRequest();
    request.page = 1;
    request.pageSize = 1;
    request.searchFilters = Collections
        .singletonList(new ProprietaryComponentNamePatternFilter.SearchFilter(
            ProprietaryComponentNamePatternFilter.SearchFilter.FilterableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
            "testNamePattern"));
    request.sortFields = Collections.singletonList(new ProprietaryComponentNamePatternFilter.SortField(
        ProprietaryComponentNamePatternFilter.SortField.SortableField.PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
        true /* asc */, 1 /* sortPriority */));

    // Rest request at Repository Level, response must include only patterns of repo1
    HttpResponse response =
        restRequest().path(RepositoryResource.RESOURCE_PATH,
            RepositoryResource.PROPRIETARY_COMPONENT_NAME_PATTERN_BY_OWNER_PATH)
            .parameter(OwnerType.REPOSITORY, repo1.getId())
            .body(request)
            .post();

    assertResponseStatus(200, response);
    ProprietaryComponentNamePatternsPage proprietaryComponentNamePatternsPage =
        response.getBody(ProprietaryComponentNamePatternsPage.class);
    assertThat(proprietaryComponentNamePatternsPage.hasNextPage).isFalse();
    assertThat(proprietaryComponentNamePatternsPage.proprietaryComponentNamePatterns).hasSize(1);
    ProprietaryComponentNamePatternDTO patternDTO =
        proprietaryComponentNamePatternsPage.proprietaryComponentNamePatterns.get(0);
    assertThat(patternDTO.id).isEqualTo(pattern1.getId());
    assertThat(patternDTO.repositoryManagerInstanceId).isEqualTo(repoManager1.getInstanceId());
    assertThat(patternDTO.repositoryPublicId).isEqualTo(repo1.getPublicId());
    assertThat(patternDTO.format).isEqualTo(pattern1.getFormat());
    assertThat(patternDTO.namespacePattern).isEqualTo(pattern1.getNamespacePattern());
    assertThat(patternDTO.namePattern).isEqualTo(pattern1.getNamePattern());
    assertThat(patternDTO.enabled).isEqualTo(pattern1.isEnabled());

    // Rest request at Repository Manager Level, response must include only patterns of repos in repoManager2
    response =
        restRequest().path(RepositoryResource.RESOURCE_PATH,
            RepositoryResource.PROPRIETARY_COMPONENT_NAME_PATTERN_BY_OWNER_PATH)
            .parameter(OwnerType.REPOSITORY_MANAGER, repoManager2.getId())
            .body(request)
            .post();

    assertResponseStatus(200, response);
    proprietaryComponentNamePatternsPage = response.getBody(ProprietaryComponentNamePatternsPage.class);
    assertThat(proprietaryComponentNamePatternsPage.hasNextPage).isFalse();
    assertThat(proprietaryComponentNamePatternsPage.proprietaryComponentNamePatterns).hasSize(1);
    patternDTO =
        proprietaryComponentNamePatternsPage.proprietaryComponentNamePatterns.get(0);
    assertThat(patternDTO.id).isEqualTo(pattern2.getId());
    assertThat(patternDTO.repositoryManagerInstanceId).isEqualTo(repoManager2.getInstanceId());
    assertThat(patternDTO.repositoryPublicId).isEqualTo(repo2.getPublicId());
    assertThat(patternDTO.format).isEqualTo(pattern2.getFormat());
    assertThat(patternDTO.namespacePattern).isEqualTo(pattern2.getNamespacePattern());
    assertThat(patternDTO.namePattern).isEqualTo(pattern2.getNamePattern());
    assertThat(patternDTO.enabled).isEqualTo(pattern2.isEnabled());

    // Rest request at Repository Container Level, response must include patterns of all repos
    request.pageSize = 2;
    response =
        restRequest().path(RepositoryResource.RESOURCE_PATH,
            RepositoryResource.PROPRIETARY_COMPONENT_NAME_PATTERN_BY_OWNER_PATH)
            .parameter(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID)
            .body(request)
            .post();

    assertResponseStatus(200, response);
    proprietaryComponentNamePatternsPage = response.getBody(ProprietaryComponentNamePatternsPage.class);
    assertThat(proprietaryComponentNamePatternsPage.hasNextPage).isFalse();
    assertThat(proprietaryComponentNamePatternsPage.proprietaryComponentNamePatterns).hasSize(2);
    ProprietaryComponentNamePatternDTO patternDTO1 =
        proprietaryComponentNamePatternsPage.proprietaryComponentNamePatterns.get(0);
    assertThat(patternDTO1.id).isEqualTo(pattern1.getId());
    assertThat(patternDTO1.repositoryManagerInstanceId).isEqualTo(repoManager1.getInstanceId());
    assertThat(patternDTO1.repositoryPublicId).isEqualTo(repo1.getPublicId());
    assertThat(patternDTO1.format).isEqualTo(pattern1.getFormat());
    assertThat(patternDTO1.namespacePattern).isEqualTo(pattern1.getNamespacePattern());
    assertThat(patternDTO1.namePattern).isEqualTo(pattern1.getNamePattern());
    assertThat(patternDTO1.enabled).isEqualTo(pattern1.isEnabled());

    ProprietaryComponentNamePatternDTO patternDTO2 =
        proprietaryComponentNamePatternsPage.proprietaryComponentNamePatterns.get(1);
    assertThat(patternDTO2.id).isEqualTo(pattern2.getId());
    assertThat(patternDTO2.repositoryManagerInstanceId).isEqualTo(repoManager2.getInstanceId());
    assertThat(patternDTO2.repositoryPublicId).isEqualTo(repo2.getPublicId());
    assertThat(patternDTO2.format).isEqualTo(pattern2.getFormat());
    assertThat(patternDTO2.namespacePattern).isEqualTo(pattern2.getNamespacePattern());
    assertThat(patternDTO2.namePattern).isEqualTo(pattern2.getNamePattern());
    assertThat(patternDTO2.enabled).isEqualTo(pattern2.isEnabled());
  }

  @Test
  public void testGetAndSetRepositoryManagerIcon() throws Exception {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();

    // Add invalid icon
    byte[] defaultIconByteArray = IconUtils.loadInvalidIcon();
    HttpResponse response =
        restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORY_MANAGER_ICON_PATH)
            .parameter(repoManager.getId())
            .part("hasRobotSource", "false")
            .part("file", "defaulticon_repository_manager.png", defaultIconByteArray)
            .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("defaulticon_repository_manager.png is not a valid image."
        + " Make sure the image is in PNG, JPEG, GIF, BMP, or WBMP format.");

    // Get icon (default icon)
    HttpResponse iconResponse =
        restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORY_MANAGER_ICON_PATH)
            .parameter(repoManager.getId())
            .get();
    assertResponseStatus(307, iconResponse);
    assertThat(iconResponse.getHeader("Location"))
        .isEqualTo(getRestBaseUrl() + "assets/img/defaulticon_repository_manager.png");

    // Add icon
    defaultIconByteArray = IconUtils.loadIconFromProductAssets("defaulticon_repository_manager.png");
    response = restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORY_MANAGER_ICON_PATH)
        .parameter(repoManager.getId())
        .part("hasRobotSource", "false")
        .part("file", "defaulticon_repository_manager.png", defaultIconByteArray)
        .post();
    assertResponseStatus(200, response);

    // Get icon
    iconResponse =
        restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORY_MANAGER_ICON_PATH)
            .parameter(repoManager.getId())
            .get();
    assertResponseStatus(200, iconResponse);
    BufferedImage icon;
    try (InputStream iconStream = iconResponse.getBodyStream()) {
      icon = ImageIO.read(iconStream);
    }
    assertThat(icon).isNotNull();
    assertThat(icon.getHeight()).isEqualTo(420);
    assertThat(icon.getWidth()).isEqualTo(420);

    // Update icon
    response = restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORY_MANAGER_ICON_PATH)
        .parameter(repoManager.getId())
        .part("hasRobotSource", "false")
        .post();
    assertResponseStatus(200, response);

    // Get icon when repo manager does not exist
    repositoryManagerDAO.delete(repoManager);
    assertThat(repositoryManagerDAO.getById(repoManager.getId())).isNull();

    iconResponse =
        restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORY_MANAGER_ICON_PATH)
            .parameter(repoManager.getId())
            .get();
    assertResponseStatus(404, iconResponse);
  }

  @Test
  public void testGenerateIcon() throws Exception {
    HttpResponse response =
        restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.GENERATE_ICON_PATH)
            .parameter("hash")
            .get();
    assertResponseStatus(200, response);
    assertThat(response.getBodyBytes()).isNotNull();
  }
}
