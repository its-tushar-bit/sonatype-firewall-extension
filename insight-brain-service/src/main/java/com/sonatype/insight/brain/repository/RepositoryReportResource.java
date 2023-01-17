/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.17.0
 */
@Named
@Timed
@Path(RepositoryReportResource.RESOURCE_PATH)
public class RepositoryReportResource
{
  public static final String SUMMARY = "summary";

  static final String DETAILS_PATH = "details";

  static final String POLICY_THREAT_PATH = "policyThreat/{pathname: .+}";

  public static final String RESOURCE_PATH = "rest/repositories/{repositoryId}/report";

  private final RepositoryService repositoryService;

  @Inject
  public RepositoryReportResource(final RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  @GET
  @Path(SUMMARY)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_REPOSITORY_RESULTS)
  public RepositorySummary getRepositorySummary(@PathParam("repositoryId") String repositoryId) {
    return repositoryService.getRepositorySummary(repositoryId);
  }

  /**
   * @deprecated Replaced in 1.140 with {@link RepositoryResultsResource#getDetails(String,
   * RepositoryResultsDetailsRequestDto)}. To be removed when the Repository Results View migration to React is
   * completed (Epic: https://issues.sonatype.org/browse/CLM-20597)
   */
  @GET
  @Path(DETAILS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_REPOSITORY_RESULTS)
  @Deprecated
  public List<RepositoryReportDetail> getReportDetails(@PathParam("repositoryId") final String repositoryId,
                                                       @QueryParam("hash") String hash,
                                                       @QueryParam("pathname") String pathname)
  {
    return repositoryService.getReportDetails(repositoryId, hash, pathname);
  }

  /**
   * @deprecated Use {@link RepositoryResource#getPolicyViolations(String, String)} instead.
   *             To be removed when the Repository Results View migration to React is
   *             completed (Epic: https://issues.sonatype.org/browse/CLM-20597)
   * 
   * @since 1.18.0
   */
  @Deprecated
  @GET
  @Path(POLICY_THREAT_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public DeprecatedRepositoryPolicyThreatDTO getPolicyThreats(
      @PathParam("repositoryId") final String repositoryId,
      @PathParam("pathname") final String pathname)
  {
    return repositoryService.getPolicyThreats(repositoryId, pathname);
  }
}
