/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.thirdpartyscans.BomPageSbomSummaryDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Parameter;

@Named
@Timed
@Singleton
@Path(SbomComponentsResource.RESOURCE_BASE_PATH)
public class SbomComponentsResource
{
  public static final String RESOURCE_BASE_PATH = "rest/sbom";

  public static final String SBOMS_APPLICATIONS_PATH = "/applications";

  static final String SBOMS_APPLICATION_PATH = SBOMS_APPLICATIONS_PATH + "/{applicationId}";

  static final String SBOM_VERSIONS_PATH = SBOMS_APPLICATION_PATH + "/versions";

  public static final String SBOM_VERSION_PATH = SBOM_VERSIONS_PATH + "/{version}";

  public static final String SBOM_METADATA_PATH = SBOM_VERSION_PATH + "/sbomMetadata";

  static final String SBOM_SUMMARY_PATH = SBOM_VERSION_PATH + "/summary";

  public static final String COMPONENT_DETAILS_PATH = SBOM_VERSION_PATH + "/components/{componentHash}";

  private final SbomComponentsService service;

  @Inject
  public SbomComponentsResource(SbomComponentsService sbomComponentsService) {
    this.service = sbomComponentsService;
  }

  @GET
  @Path(COMPONENT_DETAILS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Audited(AuditEvent.VIEW_SBOM_COMPONENT_DETAILS)
  public CDPSbomComponentDetailsDTO getComponentsDetails(@PathParam("applicationId") String applicationId,
                                                         @PathParam("version") String sbomVersion,
                                                         @PathParam("componentHash") String componentHash)
  {
    return service.getSbomComponentDetails(applicationId, sbomVersion, componentHash);
  }

  @GET
  @Path(SBOM_METADATA_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces(MediaType.APPLICATION_JSON)
  public BomPageMetadataDTO getBomPageMetadata(
      @PathParam("applicationId") String applicationId,
      @PathParam("version") String sbomVersion)
  {
    return service.getBomPageMetadata(applicationId, sbomVersion);
  }

  @GET
  @Path(SBOM_SUMMARY_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces(MediaType.APPLICATION_JSON)
  public BomPageSbomSummaryDTO getSbomSummary(
      @Parameter(description = "The internal id of the application",
          required = true) @PathParam("applicationId") String applicationId,

      @Parameter(description = "URL Encoded version value of the sbom to query its summary",
          required = true) @PathParam("version") String version)
  {
    return service.getSbomSummaryForComponents(applicationId, version);
  }
}
