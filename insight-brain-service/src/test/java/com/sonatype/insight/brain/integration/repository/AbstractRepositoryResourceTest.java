/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractRepositoryResourceTest
    extends AbstractResourceTest
{
  private static final String REPO_PUBLIC_ID = "publicId";

  private static final RepositoryDAO repositoryDAO = new RepositoryDAO();

  protected abstract HttpRequest summaryRequest();

  protected abstract HttpRequest quarantineRequest();

  protected abstract HttpRequest enableRequest();

  @Test
  public void testSetEnabled_True() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    HttpResponse response = enableRequest().parameter(repositoryManager.getInstanceId(), repository.getPublicId(), true)
        .post();
    assertResponseStatus(204, response);

    repository = repositoryDAO.getById(repository.getId());

    assertThat(repository).isNotNull();
    assertThat(repository.isEnabled()).isTrue();
  }

  @Test
  public void testSetQuarantine() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true);

    HttpResponse response = quarantineRequest()
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId(), true).post();
    assertResponseStatus(204, response);

    repository = repositoryDAO.getById(repository.getId());

    assertThat(repository).isNotNull();
    assertThat(repository.isQuarantineEnabled()).isTrue();
  }

  @Test
  public void testGetPolicyEvaluationSummary() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, true);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, "path1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 4, "path2",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, "path3",
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "path4",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));
    tempEntity.newRepositoryComponent(repository.getId(), "/blah", new Date(), null);

    HttpResponse response = summaryRequest().parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .get();

    assertResponseStatus(200, response);
    RepositoryPolicyEvaluationSummary policyEvaluationSummary = response
        .getBody(RepositoryPolicyEvaluationSummary.class);
    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(3);
    assertThat(policyEvaluationSummary.getQuarantinedComponentCount()).isEqualTo(1);
  }

  @Test
  public void testGetPolicyEvaluationSummary_NoRepository() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    String repositoryId = "NonExistentRepositoryId";

    HttpResponse response = summaryRequest().parameter(repositoryManager.getInstanceId(), repositoryId).get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Cannot find a repository with repositoryManagerInstanceId=" + repositoryManager.getInstanceId() +
            " and publicId=" + repositoryId + ".");
  }

  @Test
  public void testGetPolicyEvaluationSummary_RepositoryDisabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);
    String repositoryId = repository.getPublicId();

    HttpResponse response = summaryRequest().parameter(repositoryManager.getInstanceId(), repositoryId).get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Repository " + repositoryId + " is disabled.");
  }

  @Test
  public void testEvaluateComponents() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID, false);

    ComponentEvaluationDataRequestList componentEvaluationDataRequestList = new ComponentEvaluationDataRequestList();

    HttpResponse response = restRequest().path(RepositoryResource.EVALUATE_COMPONENTS_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId()).body(componentEvaluationDataRequestList)
        .post();
    assertResponseStatus(204, response);
  }

  @Test
  public void testRemoveComponent() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);

    HttpResponse response = restRequest().path(RepositoryResource.COMPONENTS_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId(), "somepath/subpath").delete();
    assertResponseStatus(204, response);
  }

  @Test
  public void testGetUnquarantinedComponents() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPO_PUBLIC_ID);
    Date now = new Date();
    String pathname = "test/pathname";
    tempEntity.newRepositoryComponent(repository.getId(), pathname, now, now);

    HttpResponse response = restRequest().path(RepositoryResource.UNQUARANTINED_COMPONENTS_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .query("sinceUtcTimestamp=" + (now.getTime())).get();
    assertResponseStatus(200, response);
    UnquarantinedComponentList result = response.getBody(UnquarantinedComponentList.class);
    assertThat(result.pathnames).containsExactly(pathname);
  }
}
