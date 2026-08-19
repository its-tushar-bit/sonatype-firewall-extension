/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.resultsview;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.140.0
 */
@Named
@Timed
@Path(RepositoryResultsResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.FIREWALL)
public class RepositoryResultsResource
{
  public static final String RESOURCE_PATH = "api/experimental/repositories";

  public static final String DETAILS_BY_OWNER_PATH =
      "/{ownerType: repository_container|repository_manager|repository}/{ownerId}/results/details";

  private final RepositoryResultsService repositoryResultsService;

  @Inject
  public RepositoryResultsResource(final RepositoryResultsService repositoryResultsService) {
    this.repositoryResultsService = repositoryResultsService;
  }

  @POST
  @Path(DETAILS_BY_OWNER_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_REPOSITORY_RESULTS)
  public RepositoryResultsDetailsResponseDto getDetails(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      final RepositoryResultsDetailsRequestDto detailsRequest)
  {
    return repositoryResultsService.getDetails(ownerType, ownerId, detailsRequest);
  }
}
