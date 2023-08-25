/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.HttpResponse;
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
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testUnquarantineComponent() throws Exception {
    Repository repo = tempEntity.newRepository();
    String path = "dir/path";
    HttpResponse response = restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.UNQUARANTINE_PATH)
        .parameter(repo.getId(), path).post();
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
        .isEqualTo(new RepositoryManagerDAO().getById(repo.getRepositoryManagerId()).getInstanceId());
  }

  @Test
  public void testGetRepository() throws Exception {
    Repository repo = tempEntity.newRepository();

    HttpResponse response = restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORY_PATH)
        .parameter(repo.getId()).get();
    assertResponseStatus(200, response);
    RepositoryDTO actual = response.getBody(RepositoryDTO.class);

    assertThat(actual.repository).isNotNull();
    assertThat(actual.repository.getId()).isEqualTo(repo.getId());
    assertThat(actual.repository.getPublicId()).isEqualTo(repo.getPublicId());
    assertThat(actual.managerInstanceId)
        .isEqualTo(new RepositoryManagerDAO().getById(repo.getRepositoryManagerId()).getInstanceId());
  }

  @Test
  public void testReevaluateRepository() throws Exception {
    Repository repo = tempEntity.newRepository();

    HttpResponse response = restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.EVALUATE_PATH)
        .parameter(repo.getId()).post();
    assertResponseStatus(204, response);
  }

  @Test
  public void testDeleteRepository() throws Exception {
    Repository repo = tempEntity.newRepository();

    HttpResponse deleteResponse = restRequest()
        .path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORY_PATH).parameter(repo.getId()).delete();
    assertResponseStatus(204, deleteResponse);
    assertThat(new RepositoryDAO().getById(repo.getId())).isNull();
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
        .parameter(repo.getId(), component.getHash()).post();
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
            .parameter(repo.getId()).query("componentIdentifier", componentIdentifier).get();
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
            .parameter(repo.getId(), repositoryComponent.getPathname()).get();

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
            .parameter(repo.getId(), repositoryPolicyViolation.getId()).get();

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
  public void testGetProprietaryComponentNamePatterns() throws Exception {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_MAVEN);
    ProprietaryComponentNamePattern pattern1 =
        tempEntity.newProprietaryComponentNamePattern(repo, "testNamespacePattern1", "testNamePattern1");
    tempEntity.newProprietaryComponentNamePattern(repo, "testNamespacePattern2", "testNamePattern2");
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

    HttpResponse response =
        restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.PROPRIETARY_COMPONENT_NAME_PATTERN_PATH)
            .body(request).post();

    assertResponseStatus(200, response);
    ProprietaryComponentNamePatternsPage proprietaryComponentNamePatternsPage =
        response.getBody(ProprietaryComponentNamePatternsPage.class);
    assertThat(proprietaryComponentNamePatternsPage.hasNextPage).isTrue();
    assertThat(proprietaryComponentNamePatternsPage.proprietaryComponentNamePatterns).hasSize(1);
    ProprietaryComponentNamePatternDTO patternDTO =
        proprietaryComponentNamePatternsPage.proprietaryComponentNamePatterns.get(0);
    assertThat(patternDTO.id).isEqualTo(pattern1.getId());
    assertThat(patternDTO.repositoryManagerInstanceId).isEqualTo(repoManager.getInstanceId());
    assertThat(patternDTO.repositoryPublicId).isEqualTo(repo.getPublicId());
    assertThat(patternDTO.format).isEqualTo(pattern1.getFormat());
    assertThat(patternDTO.namespacePattern).isEqualTo(pattern1.getNamespacePattern());
    assertThat(patternDTO.namePattern).isEqualTo(pattern1.getNamePattern());
    assertThat(patternDTO.enabled).isEqualTo(pattern1.isEnabled());
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
    assertThat(new ProprietaryComponentNamePatternDAO().getById(pattern.getId()).isEnabled()).isTrue();

    HttpResponse response =
        restRequest()
            .path(RepositoryResource.RESOURCE_PATH, RepositoryResource.PROPRIETARY_COMPONENT_NAME_PATTERN_UPDATE_PATH)
            .body(proprietaryComponentNamePatternDTO).post();

    assertResponseStatus(204, response);
    assertThat(new ProprietaryComponentNamePatternDAO().getById(pattern.getId()).isEnabled()).isFalse();
  }

  @Test
  public void testGetUnconfiguredRepositoryManagers() throws Exception {
    RepositoryManager configuredRepoManager = tempEntity.newRepositoryManager();
    configuredRepoManager.setConfigured(true);
    configuredRepoManager.setConfigureTime(new Date());
    new RepositoryManagerDAO().update(configuredRepoManager);

    RepositoryManager unconfiguredRepoManager = tempEntity.newRepositoryManager();
    unconfiguredRepoManager.setUserAgent("Nexus/3.60.0-01 (PRO; Windows 10; 10.0; amd64; 1.8.0_352)");
    unconfiguredRepoManager.setProductName("Nexus");
    unconfiguredRepoManager.setProductVersion("3.60.0-01");
    new RepositoryManagerDAO().update(unconfiguredRepoManager);

    HttpResponse response = restRequest()
        .path(RepositoryResource.RESOURCE_PATH, RepositoryResource.UNCONFIGURED_REPOSITORY_MANAGERS_PATH).get();

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
            .parameter(repositoryManager.getId()).get();

    assertResponseStatus(200, response);
    Repository[] repositories = response.getBody(Repository[].class);
    assertThat(repositories).extracting(Repository::getId) //
        .containsExactlyInAnyOrder(repository1.getId(), repository2.getId());
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
            .body(Collections.singletonList(repository)).put();
    Date afterConfig = new Date();

    assertResponseStatus(204, response);

    repositoryManager = new RepositoryManagerDAO().getByInstanceIdNotNull(repositoryManager.getInstanceId());
    assertThat(repositoryManager.isConfigured()).isEqualTo(true);
    assertThat(repositoryManager.getConfigureTime()).isAfterOrEqualTo(beforeConfig).isBeforeOrEqualTo(afterConfig);

    repository = new RepositoryDAO().getById(repository.getId());
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
            .body(firewallOnboardingOptionsDTO).put();
    assertResponseStatus(204, response);

    PolicyDAO policyDAO = new PolicyDAO();
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

    RepositoryManager repositoryManagerResult = new RepositoryManagerDAO().getById(repositoryManager.getId());

    assertThat(repositoryManagerResult.getName()).isEqualTo("name2");
  }
}
