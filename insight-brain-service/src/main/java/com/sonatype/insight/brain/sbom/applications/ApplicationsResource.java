/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.applications;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomApplicationSummaryDTO;
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
  public List<SbomApplicationSummaryDTO> getApplicationDetails(
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
