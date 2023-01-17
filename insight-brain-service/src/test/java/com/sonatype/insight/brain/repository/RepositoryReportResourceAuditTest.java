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
import com.sonatype.insight.brain.model.repository.RepositoryComponent;

import org.junit.Before;
import org.junit.Test;

public class RepositoryReportResourceAuditTest
    extends AbstractComponentInfoResourceAuditTest
{
  private Repository repository;

  @Before
  public void before() {
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

  /**
   * @deprecated The tested method is deprecated. To be removed when the Repository Results View migration to React is
   * completed (Epic: https://issues.sonatype.org/browse/CLM-20597)
   */
  @Test
  @Deprecated
  public void testGetReportDetails() throws Exception {
    repositoryResourceRequest().path(RepositoryReportResource.DETAILS_PATH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, null);
    assertRepositoryData(auditDTO, repository);
  }

  /**
   * @deprecated The tested method is deprecated. To be removed when the Repository Results View migration to React is
   * completed (Epic: https://issues.sonatype.org/browse/CLM-20597)
   */
  @Test
  @Deprecated
  public void testGetReportDetails_Unauthorized() throws Exception {
    repositoryResourceRequest().path(RepositoryReportResource.DETAILS_PATH).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_REPOSITORY_RESULTS, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetPolicyThreats() throws Exception {
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), "dir/path");

    restPolicyThreatRequest(repositoryComponent.getPathname()).get();
    AuditDTO auditDTO = assertAuditComponentInfo(repository, repositoryComponent.getComponentIdentifier(),
        repositoryComponent.getHash());
    assertCustomData(auditDTO, "componentPathname", repositoryComponent.getPathname());
  }

  @Test
  public void testGetPolicyThreats_NonExistent() throws Exception {
    restPolicyThreatRequest("non-existent/path").get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "not-found");
    assertCustomData(auditDTO, "componentPathname", "non-existent/path");
  }

  @Test
  public void testGetPolicyThreats_Unauthorized() throws Exception {
    restPolicyThreatRequest("a/path").with(unauthorizedUser()).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  private HttpRequest restPolicyThreatRequest(final String pathname) {
    return restRequest().path(RepositoryReportResource.RESOURCE_PATH, RepositoryReportResource.POLICY_THREAT_PATH)
        .parameter(repository.getId(), pathname);
  }

  private HttpRequest repositoryResourceRequest() {
    return restRequest().path(RepositoryReportResource.RESOURCE_PATH).parameter(repository.getId());
  }
}
