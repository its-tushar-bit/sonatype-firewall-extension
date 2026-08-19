/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.resource;

import java.io.IOException;
import java.util.List;

import com.sonatype.guide.api.controller.GuideSecurityEventsApi;
import com.sonatype.guide.api.dto.AffectedComponentVersion;
import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.guide.api.dto.SecurityEventDetailDocument;
import com.sonatype.guide.api.dto.SecurityEventDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersionRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideSecurityEventSearchRequest;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * JAX-RS resource exposing the Security Events API under the Guide namespace, backed by the
 * {@code security-events-v1} index through the HDS search proxy.
 *
 * <p>
 * Provides list and detail endpoints for security events. Access is gated at the filter level by
 * {@link com.sonatype.insight.brain.security.SearchLicenseFilter} and depends on both the license
 * and the deployment tenancy:
 * <ul>
 * <li>Guide license ({@link LicensedFeature#GUIDE_SEARCH}) — single-tenant only</li>
 * <li>AI Developer license ({@link LicensedFeature#AI_DEVELOPER}) — available in both single-tenant
 * and multi-tenant deployments when the SKU matches the tenancy: the self-hosted
 * {@code AiDeveloper} SKU single-tenant, the {@code AiDeveloperSaas} SKU multi-tenant</li>
 * </ul>
 */
@Named
@Singleton
@Path("/api/v2/guide/security-events")
@Produces(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(value = LicensedFeature.GUIDE_SEARCH, anyOf = LicensedFeature.AI_DEVELOPER)
public class GuideSecurityEventsResource
    implements GuideSecurityEventsApi
{
  private final SearchApiClient searchApiClient;

  @Inject
  public GuideSecurityEventsResource(SearchApiClient searchApiClient) {
    this.searchApiClient = searchApiClient;
  }

  @GET
  @Path("/search")
  @Override
  public ApiSearchResponse<SecurityEventDocument> searchSecurityEvents(
      @QueryParam("query") String query,
      @QueryParam("offset") Integer offset,
      @QueryParam("limit") Integer limit,
      @QueryParam("sortField") String sortField,
      @QueryParam("sortOrder") String sortOrder,
      @QueryParam("severities") List<String> severities,
      @QueryParam("threatTypes") List<String> threatTypes,
      @QueryParam("knownExploited") Boolean knownExploited,
      @QueryParam("affectedEcosystems") List<String> affectedEcosystems) throws IOException
  {
    GuideSecurityEventSearchRequest request = new GuideSecurityEventSearchRequest(
        query, offset, limit, sortField, sortOrder,
        severities, threatTypes, knownExploited, affectedEcosystems);

    return searchApiClient.searchSecurityEvents(request);
  }

  @GET
  @Path("/{id}")
  @Override
  public SecurityEventDetailDocument getSecurityEventById(
      @PathParam("id") String eventId) throws IOException
  {
    GuideValidation.requireNonBlankId(eventId, "eventId");
    return searchApiClient.getSecurityEventById(eventId);
  }

  @GET
  @Path("/{id}/affected-components")
  @Override
  public ApiSearchResponse<AffectedComponentVersion> getSecurityEventAffectedComponents(
      @PathParam("id") String id,
      @QueryParam("query") String query,
      @QueryParam("offset") Integer offset,
      @QueryParam("limit") Integer limit,
      @QueryParam("sortField") String sortField,
      @QueryParam("sortOrder") String sortOrder) throws IOException
  {
    GuideValidation.requireNonBlankId(id, "id");
    return searchApiClient.getSecurityEventAffectedComponents(
        new GuideAffectedComponentVersionRequest(id, query, offset, limit, sortField, sortOrder));
  }
}
