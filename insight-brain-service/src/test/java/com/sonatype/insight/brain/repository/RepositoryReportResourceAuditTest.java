/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.hds.AbstractComponentInfoResourceAuditTest;
import com.sonatype.insight.brain.model.repository.Repository;

import org.junit.Before;
import org.junit.Test;

public class RepositoryReportResourceAuditTest
    extends AbstractComponentInfoResourceAuditTest
{
  private Repository repository;

  @Before
  public void setUp() {
    repository = tempEntity.newRepository("repoPublicId");
  }

  @Test
  public void testGetRepositorySummary() throws Exception {
    repositoryResourceRequest().path(RepositoryReportResource.SUMMARY).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, null);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetRepositorySummary_Unauthorized() throws Exception {
    repositoryResourceRequest().path(RepositoryReportResource.SUMMARY).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  private HttpRequest repositoryResourceRequest() {
    return restRequest().path(RepositoryReportResource.RESOURCE_PATH).parameter(repository.getId());
  }
}
