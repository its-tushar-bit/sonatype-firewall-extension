/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.resultsview;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class RepositoryResultsResourceAuditTest
    extends AbstractAuditTest
{
  private RepositoryManager repositoryManager;

  private Repository repository;

  private RepositoryResultsDetailsRequestDto detailsRequest;

  @Before
  public void setup() {
    repositoryManager = tempEntity.newRepositoryManager();
    repository = tempEntity.newRepository(repositoryManager, "publicId");
    detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
  }

  @Test
  public void testGetDetails_RepositoryContainer() throws Exception {
    restRequest().path(RepositoryResultsResource.RESOURCE_PATH, RepositoryResultsResource.DETAILS_BY_OWNER_PATH)
        .parameter("repository_container", RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .body(detailsRequest)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, null);
    assertRepositoryContainerData(auditDTO);
  }

  @Test
  public void testGetDetails_RepositoryManager() throws Exception {
    restRequest().path(RepositoryResultsResource.RESOURCE_PATH, RepositoryResultsResource.DETAILS_BY_OWNER_PATH)
        .parameter("repository_manager", repositoryManager.getId())
        .body(detailsRequest)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, null);
    assertRepositoryManagerData(auditDTO, repositoryManager);
  }

  @Test
  public void testGetDetails_Repository() throws Exception {
    restRequest().path(RepositoryResultsResource.RESOURCE_PATH, RepositoryResultsResource.DETAILS_BY_OWNER_PATH)
        .parameter("repository", repository.getId())
        .body(detailsRequest)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, null);
    assertRepositoryData(auditDTO, repository);
  }
}
