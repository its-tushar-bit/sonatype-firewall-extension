/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.resultsview;

import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class RepositoryResultsResourceAuditTest
    extends AbstractAuditTest
{
  private Repository repository;

  private RepositoryResultsDetailsRequestDto detailsRequest;

  @Before
  public void setup() {
    repository = tempEntity.newRepository();
    detailsRequest = new RepositoryResultsDetailsRequestDto();
    detailsRequest.page = 1;
    detailsRequest.pageSize = 50;
  }

  @Test
  public void testGetDetails() throws Exception {
    restRequest().path(RepositoryResultsResource.RESOURCE_PATH)
        .parameter(repository.getId()).body(detailsRequest).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, null);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetDetails_Unauthorized() throws Exception {
    restRequest().path(RepositoryResultsResource.RESOURCE_PATH)
        .parameter(repository.getId())
        .body(detailsRequest).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }
}
