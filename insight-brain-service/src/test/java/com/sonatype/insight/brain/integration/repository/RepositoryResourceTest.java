/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RepositoryResourceTest
    extends AbstractResourceTest
{
  private static final RepositoryDAO repositoryDAO = new RepositoryDAO();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(RepositoryResource.SERVICE_PATH);
  }

  @Test
  public void testEnableRepository() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "publicId", false);

    HttpResponse response = restRequest().parameter(repositoryManager.getInstanceId(), repository.getPublicId()).post();
    assertResponseStatus(204, response);

    repository = repositoryDAO.getById(repository.getId());

    assertNotNull(repository);
    assertTrue(repository.isEnabled());
  }

  @Test
  public void testEvaluateComponents() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "publicId", false);

    ComponentEvaluationDataRequestList componentEvaluationDataRequestList = new ComponentEvaluationDataRequestList();

    HttpResponse response = restRequest().path(RepositoryResource.EVALUATE_COMPONENTS_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .body(componentEvaluationDataRequestList).post();
    assertResponseStatus(204, response);
  }
}
