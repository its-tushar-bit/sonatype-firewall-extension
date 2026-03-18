/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.PaginationResponseBuilder;
import com.sonatype.insight.brain.hds.ComponentVersionInfoDTO;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.repository.RepositoryPolicyViolationDTO;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.125.0
 */
@Named
@Timed
@Path(QuarantinedComponentResource.RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(LicensedFeature.FIREWALL)
public class QuarantinedComponentResource
{
  public static final String RESOURCE_PATH = "rest/repositories/quarantinedComponent/";

  public static final String QUARANTINED_COMPONENT_PATH = "{token}";

  public static final String QUARANTINED_COMPONENT_OVERVIEW_PATH = QUARANTINED_COMPONENT_PATH + "/overview";

  public static final String QUARANTINED_COMPONENT_VERSION_REMEDIATION_PATH =
      QUARANTINED_COMPONENT_PATH + "/remediation";

  public static final String QUARANTINED_COMPONENT_VERSION_DETAILS_PATH =
      QUARANTINED_COMPONENT_PATH + "/details";

  public static final String QUARANTINED_COMPONENT_POLICY_VIOLATIONS_PATH =
      QUARANTINED_COMPONENT_PATH + "/policyViolations";

  public static final String QUARANTINED_COMPONENT_OTHER_VERSIONS_PATH = QUARANTINED_COMPONENT_PATH + "/otherVersions";

  private final QuarantinedComponentService quarantinedComponentService;

  @Context
  private HttpServletRequest httpRequest;

  @Inject
  public QuarantinedComponentResource(final QuarantinedComponentService quarantinedComponentService) {
    this.quarantinedComponentService = quarantinedComponentService;
  }

  @GET
  @Path(QUARANTINED_COMPONENT_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public QuarantinedComponentDto getQuarantinedComponent(@PathParam("token") String token) {
    return quarantinedComponentService.getQuarantinedComponent(token);
  }

  @GET
  @Path(QUARANTINED_COMPONENT_OVERVIEW_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public QuarantinedComponentOverviewDto getQuarantinedComponentOverview(@PathParam("token") String token) {
    return quarantinedComponentService.getQuarantinedComponentOverview(token);
  }

  @GET
  @Path(QUARANTINED_COMPONENT_POLICY_VIOLATIONS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<RepositoryPolicyViolationDTO> getQuarantinedComponentPolicyViolations(@PathParam("token") String token) {
    return quarantinedComponentService.getQuarantinedComponentPolicyViolations(token);
  }

  @GET
  @Path(QUARANTINED_COMPONENT_VERSION_REMEDIATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ComponentVersionInfoDTO getQuarantinedComponentVersionRemediation(@PathParam("token") String token) {
    return quarantinedComponentService.getQuarantineComponentVersionRemediation(token);
  }

  @GET
  @Path(QUARANTINED_COMPONENT_VERSION_DETAILS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public NamedComponentDetails getQuarantinedComponentVersionDetails(
      @PathParam("token") String token,
      @QueryParam("version") String version) throws IOException
  {
    return quarantinedComponentService.getQuarantinedComponentVersionDetails(token, httpRequest, version);
  }

  @GET
  @Path(QUARANTINED_COMPONENT_OTHER_VERSIONS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getQuarantinedComponentOtherVersions(
      @Context UriInfo uriInfo,
      @PathParam("token") String token,
      @DefaultValue("1") @QueryParam("page") int page,
      @DefaultValue("5") @QueryParam("pageSize") int pageSize,
      @DefaultValue("true") @QueryParam("asc") boolean asc)
  {
    final ApiPageResult<String> result = quarantinedComponentService
        .getQuarantinedComponentOtherVersions(token, page, pageSize, asc);

    return new PaginationResponseBuilder<>(uriInfo.getAbsolutePath().getPath(), page, pageSize, result)
        .queryParameters(uriInfo.getQueryParameters())
        .build();
  }
}
