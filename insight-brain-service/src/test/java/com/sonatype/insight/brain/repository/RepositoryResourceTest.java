/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.HashSet;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.integration.repository.RepositoryService.RepositoriesDTO;
import com.sonatype.insight.brain.integration.repository.RepositoryService.RepositoryDTO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

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
    assertThat(response.getBodyText(), is("Cannot find a component with path " + path + " in repository with ID "
        + repo.getId() + "."));
  }

  @Test
  public void testGetRepositories() throws Exception {
    HttpResponse response = restRequest().path(RepositoryResource.RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    RepositoriesDTO actual = response.getBody(RepositoriesDTO.class);

    assertNotNull(actual.repositories);
    assertThat(actual.repositories, hasSize(1));
    RepositoryDTO actualRepo = actual.repositories.get(0);
    assertThat(actualRepo.repository.getId(), is(repo.getId()));
    assertThat(actualRepo.repository.getPublicId(), is(repo.getPublicId()));
    assertThat(actualRepo.managerInstanceId, is(new RepositoryManagerDAO().getById(repo.getRepositoryManagerId())
        .getInstanceId()));
  }

  @Test
  public void testGetRepository() throws Exception {
    HttpResponse response = restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORY_PATH)
        .parameter(repo.getId()).get();
    assertResponseStatus(200, response);
    RepositoryDTO actual = response.getBody(RepositoryDTO.class);

    assertNotNull(actual.repository);
    assertThat(actual.repository.getId(), is(repo.getId()));
    assertThat(actual.repository.getPublicId(), is(repo.getPublicId()));
    assertThat(actual.managerInstanceId, is(new RepositoryManagerDAO().getById(repo.getRepositoryManagerId())
        .getInstanceId()));
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
    assertNull(new RepositoryDAO().getById(repo.getId()));
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
    getInsightServer().setResponseForURI("/rest/component/details/firewall", hdsResult, 200);

    HttpResponse response = restRequest()
        .path(RepositoryResource.RESOURCE_PATH, RepositoryResource.EVALUATE_COMPONENT_PATH)
        .parameter(repo.getId(), component.getHash()).post();
    assertResponseStatus(204, response);
  }
}
