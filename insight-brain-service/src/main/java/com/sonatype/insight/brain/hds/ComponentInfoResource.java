/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentLicenses;
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentMultiLicenses;
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentSecurityVulnerabilities;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.repository.hosted.HrcOwnerTypeFeatureGuard;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

@Path(ComponentInfoResource.RESOURCE_PATH)
@Named
@Timed
@ProductLicenseEnforcementPoint(LicensedFeature.COMPONENT_EVALUATION)
public class ComponentInfoResource
{
  public static final String RESOURCE_PATH = "rest/ci/componentDetails";

  static final String COMPONENT_DETAILS_PATH =
      "{ownerType: application|repository|hosted_repository_component}/{ownerId}";

  public static final String LICENSES_PATH = COMPONENT_DETAILS_PATH + "/licenses";

  public static final String MULTI_LICENSES_PATH =
      "{ownerType: application|repository|organization|hosted_repository_component}/{ownerId}/multiLicenses";

  static final String MULTI_LICENSES_LEGAL_REVIEWER_PATH = MULTI_LICENSES_PATH + "/legalReviewer";

  public static final String VULNERABILITIES_PATH = COMPONENT_DETAILS_PATH + "/vulnerabilities";

  private final ComponentInfoService componentInfoService;

  @Inject
  public ComponentInfoResource(ComponentInfoService componentInfoService) {
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
      @QueryParam("scanId") String scanId,
      @QueryParam("dependencyType") String dependencyType,
      @Context HttpServletRequest httpRequest) throws IOException
  {
    HrcOwnerTypeFeatureGuard.requireHrcFeatureIfHrc(ownerType);
    return componentInfoService.getComponentDetails_ReadPermission(ownerType, ownerId, identifier, matchState, hash,
        proprietary, httpRequest, identificationSource, scanId, DependencyType.getById(dependencyType));
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
    HrcOwnerTypeFeatureGuard.requireHrcFeatureIfHrc(ownerType);
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
      @QueryParam("stageId") String stageId,
      @QueryParam("identificationSource") String identificationSource,
      @QueryParam("scanId") String scanId,
      @QueryParam("dependencyType") String dependencyTypeId)
  {
    HrcOwnerTypeFeatureGuard.requireHrcFeatureIfHrc(ownerType);
    return componentInfoService.getComponentVersionInfo(ownerType, ownerId,
        componentIdentifier, stageId, identificationSource, scanId, DependencyType.getById(dependencyTypeId));
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
      @QueryParam("scanId") String scanId,
      @Context HttpServletRequest httpRequest) throws IOException
  {
    HrcOwnerTypeFeatureGuard.requireHrcFeatureIfHrc(ownerType);
    return componentInfoService
        .getLicenses(ownerType, ownerId, componentIdentifier, httpRequest, identificationSource, scanId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(MULTI_LICENSES_PATH)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public ComponentMultiLicenses getMultiLicenses(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @QueryParam("identificationSource") String identificationSource,
      @QueryParam("scanId") String scanId,
      @Context HttpServletRequest httpRequest) throws IOException
  {
    HrcOwnerTypeFeatureGuard.requireHrcFeatureIfHrc(ownerType);
    return componentInfoService.getMultiLicensesForRead(ownerType, ownerId, componentIdentifier, httpRequest,
        identificationSource, scanId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(MULTI_LICENSES_LEGAL_REVIEWER_PATH)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public ComponentMultiLicenses getMultiLicensesForLegalReviewer(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @QueryParam("identificationSource") String identificationSource,
      @QueryParam("scanId") String scanId,
      @Context HttpServletRequest httpRequest) throws IOException
  {
    HrcOwnerTypeFeatureGuard.requireHrcFeatureIfHrc(ownerType);
    return componentInfoService.getMultiLicensesForLegalReviewer(ownerType, ownerId, componentIdentifier, httpRequest,
        identificationSource, scanId);
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
      @QueryParam("scanId") final String scanId,
      @Context HttpServletRequest httpRequest) throws IOException
  {
    HrcOwnerTypeFeatureGuard.requireHrcFeatureIfHrc(ownerType);
    return componentInfoService
        .getSecurityVulnerabilities(ownerType, ownerId, hash, componentIdentifier, httpRequest, identificationSource,
            scanId);
  }
}
