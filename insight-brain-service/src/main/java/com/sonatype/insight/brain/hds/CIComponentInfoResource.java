/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentLicenses;
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentSecurityVulnerabilities;
import com.sonatype.insight.brain.model.OwnerType;

import com.codahale.metrics.annotation.Timed;

@Path(CIComponentInfoResource.RESOURCE_PATH)
@Named
@Timed
public class CIComponentInfoResource
{
  public static final String RESOURCE_PATH = "rest/ci/componentDetails";

  static final String COMPONENT_DETAILS_PATH = "{ownerType: application|repository}/{ownerId}";

  public static final String LICENSES_PATH = COMPONENT_DETAILS_PATH + "/licenses";

  public static final String VULNERABILITIES_PATH = COMPONENT_DETAILS_PATH + "/vulnerabilities";

  private final ComponentInfoService componentInfoService;

  @Context
  private HttpServletRequest httpRequest;

  @Inject
  public CIComponentInfoResource(ComponentInfoService componentInfoService) {
    this.componentInfoService = componentInfoService;
    componentInfoService.setToolName("ci");
  }

  @GET
  @Path(COMPONENT_DETAILS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public NamedComponentDetails getComponentDetails(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      @QueryParam("componentIdentifier") ComponentIdentifier identifier,
      @QueryParam("matchState") String matchState,
      @QueryParam("hash") String hash,
      @QueryParam("proprietary") boolean proprietary,
      @QueryParam("identificationSource") String identificationSource,
      @QueryParam("scanId") String scanId) throws IOException
  {
    return componentInfoService.getComponentDetails_ReadPermission(ownerType, ownerId, identifier, matchState, hash,
        proprietary, httpRequest, identificationSource, scanId);
  }

  /**
   * @deprecated since 1.48. Not used by Insight or plugins, but left here as our customers use these APIs.
   */
  @Deprecated
  @GET
  @Path(COMPONENT_DETAILS_PATH + "/list")
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public ComponentDetailsList getComponentDetailsList(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      @QueryParam("componentIdentifier") ComponentIdentifier identifier,
      @QueryParam("matchState") String matchState)
  {
    return componentInfoService.getComponentDetailsList_ReadPermission(ownerType, ownerId, identifier, matchState);
  }

  /**
   * @since 1.48
   */
  @GET
  @Path(COMPONENT_DETAILS_PATH + "/allVersions")
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public ComponentVersionInfoDTO getComponentVersionInfo(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @QueryParam("identificationSource") String identificationSource,
      @QueryParam("scanId") String scanId)
  {
    return componentInfoService.getComponentVersionInfo_ReadPermission(ownerType, ownerId,
        componentIdentifier, identificationSource, scanId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(LICENSES_PATH)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public ComponentLicenses getLicenses(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @QueryParam("identificationSource") String identificationSource,
      @QueryParam("scanId") String scanId) throws IOException
  {
    return componentInfoService
        .getLicenses(ownerType, ownerId, componentIdentifier, httpRequest, identificationSource, scanId);
  }

  /**
   * @since 1.18.0
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(VULNERABILITIES_PATH)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public ComponentSecurityVulnerabilities getSecurityVulnerabilities(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      @QueryParam("hash") final String hash,
      @QueryParam("componentIdentifier") final ComponentIdentifier componentIdentifier,
      @QueryParam("identificationSource") final String identificationSource,
      @QueryParam("scanId") final String scanId) throws IOException
  {
    return componentInfoService
        .getSecurityVulnerabilities(ownerType, ownerId, hash, componentIdentifier, httpRequest, identificationSource,
            scanId);
  }
}
