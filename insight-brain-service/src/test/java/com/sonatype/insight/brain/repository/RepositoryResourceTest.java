/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dto.repository.RepositoriesDTO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
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
}
