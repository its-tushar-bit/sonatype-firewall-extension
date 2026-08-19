/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.17.0
 */
@Named
@Timed
@Path(RepositoryReportResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.REPOSITORY_REPORTS)
public class RepositoryReportResource
{
  static final String SUMMARY = "summary";

  static final String RESOURCE_PATH = "rest/repositories/{repositoryId}/report";

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
}
