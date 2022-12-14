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
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternFilter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dto.repository.RepositoriesDTO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryResourceTest
    extends AbstractResourceTest
{
  private Repository repo;

  @Before
  public void setup() {
    repo = tempEntity.newRepository();
  }

  @Test
  public void testUnquarantineComponent() throws Exception {
    String path = "dir/path";
    HttpResponse response = restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.UNQUARANTINE_PATH)
        .parameter(repo.getId(), path).post();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot find a component with path " + path + " in repository with ID " + repo.getId() + ".");
  }

  @Test
  public void testGetRepositories() throws Exception {
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
    HttpResponse response = restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.EVALUATE_PATH)
        .parameter(repo.getId()).post();
    assertResponseStatus(204, response);
  }

  @Test
  public void testDeleteRepository() throws Exception {
    HttpResponse deleteResponse = restRequest()
        .path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORY_PATH).parameter(repo.getId()).delete();
    assertResponseStatus(204, deleteResponse);
    assertThat(new RepositoryDAO().getById(repo.getId())).isNull();
  }

  @Test
  public void testReevaluateRepositoryComponent() throws Exception {
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
    repo = tempEntity.newRepository(repoManager, "testRepoPublicId");
    ProprietaryComponentNamePattern pattern1 = tempEntity.newProprietaryComponentNamePattern(
        repoManager.getInstanceId(), repo.getPublicId(), "maven", "testNamespacePattern1", "testNamePattern1");
    tempEntity.newProprietaryComponentNamePattern(
        repoManager.getInstanceId(), repo.getPublicId(), "maven", "testNamespacePattern2", "testNamePattern2");
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
  }
}
