/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

public class RepositoryReportResourceAuditTest
    extends AbstractAuditTest
{
  private Repository repository;

  @Before
  public void before() {
    repository = tempEntity.newRepository("repoPublicId");
  }

  @Test
  public void testGetSummary() throws Exception {
    repositoryResourceRequest().path(RepositoryReportResource.SUMMARY).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, null);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetSummary_Unauthorized() throws Exception {
    repositoryResourceRequest().path(RepositoryReportResource.SUMMARY).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetReportDetails() throws Exception {
    repositoryResourceRequest().path(RepositoryReportResource.DETAILS_PATH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, null);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetReportDetails_Unauthorized() throws Exception {
    repositoryResourceRequest().path(RepositoryReportResource.DETAILS_PATH).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  private HttpRequest repositoryResourceRequest() {
    return restRequest().path(RepositoryReportResource.RESOURCE_PATH).parameter(repository.getId());
  }
}
