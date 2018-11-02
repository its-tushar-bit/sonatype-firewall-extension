/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class RepositoryResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testDeleteRepository() throws Exception {
    Repository repository = tempEntity.newRepository();

    restRequest(repository.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "repositoryManagerInstanceId",
        new RepositoryManagerDAO().getById(repository.getRepositoryManagerId()).getInstanceId());
  }

  @Test
  public void testDeleteRepository_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();

    restRequest(repository.getId()).with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_REPOSITORY, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  private HttpRequest restRequest(String repositoryId) {
    return restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORY_PATH)
        .parameter(repositoryId);
  }
}
