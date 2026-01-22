/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.applications;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomApplicationListSummaryDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomApplicationsSortableField;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Singleton
@Path(ApplicationsResource.RESOURCE_BASE_PATH)
public class ApplicationsResource
{
  public static final String RESOURCE_BASE_PATH = "rest/sbom";

  public static final String SBOMS_APPLICATIONS_PATH = "/applications";

  public final SbomApplicationsService sbomApplicationsService;

  @Inject
  public ApplicationsResource(final SbomApplicationsService sbomApplicationsService) {
    this.sbomApplicationsService = sbomApplicationsService;
  }

  @GET
  @Path(SBOMS_APPLICATIONS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  public SbomApplicationListSummaryDTO getApplicationDetails(
      @QueryParam("applicationName") String applicationName,
      @DefaultValue("IMPORT_DATE") @QueryParam("sortBy") SbomApplicationsSortableField sortBy,
      @DefaultValue("false") @QueryParam("asc") boolean asc,
      @DefaultValue("1") @QueryParam("page") int page,
      @DefaultValue("50") @QueryParam("pageSize") int pageSize
  )
  {
    return sbomApplicationsService.getApplications(applicationName, sortBy, asc, page, pageSize );
  }
}
