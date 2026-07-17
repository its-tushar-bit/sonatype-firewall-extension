/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.resource;

import com.sonatype.insight.brain.guide.api.dto.ApiOrgAppsResponse;
import com.sonatype.insight.brain.guide.api.dto.ApiOwnerSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.OwnerSummary;
import com.sonatype.insight.brain.guide.api.dto.ApiTopOrgsResponse;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.context.PolicyContextOwnersService;
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
import jakarta.ws.rs.core.Response;

/**
 * REST resource backing the Guide SPA owner picker. Exposes four narrow, purpose-built
 * endpoints under {@code /api/v2/policy-context/owners/}. Each endpoint permission-filters
 * on the same permissions Guide policy evaluation uses ({@code EVALUATE_APPLICATION} for
 * orgs, {@code EVALUATE_COMPONENT} for apps); business logic lives in
 * {@link PolicyContextOwnersService}. This resource only handles HTTP concerns: parameter
 * capping, query validation, and delegation.
 *
 * <p>
 * <b>Ancestor-path disclosure:</b> responses include an {@code ancestorPath} for permitted
 * owners so the picker can render breadcrumbs. That path may name ancestor orgs the caller
 * does not have {@code READ} on — the picker's permission model is {@code EVALUATE_*}, and
 * hiding parent names would produce holed breadcrumbs and defeat the UX. This is an
 * intentional, scoped disclosure limited to owners the caller can already evaluate against.
 */
@Named
@Singleton
@Path("/api/v2/policy-context/owners")
@Produces(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(LicensedFeature.GUIDE_SEARCH)
public class ApiPolicyContextOwnersResource
{
  private static final int TOP_ORGS_DEFAULT_LIMIT = 20;

  private static final int TOP_ORGS_MAX_LIMIT = 100;

  private static final int ORG_APPS_DEFAULT_LIMIT = 500;

  private static final int ORG_APPS_MAX_LIMIT = 500;

  private static final int SEARCH_DEFAULT_LIMIT = 10;

  private static final int SEARCH_MAX_LIMIT = 50;

  private static final int SEARCH_MIN_QUERY_LENGTH = 3;

  private static final int SEARCH_MAX_QUERY_LENGTH = 200;

  private final PolicyContextOwnersService ownersService;

  @Inject
  public ApiPolicyContextOwnersResource(PolicyContextOwnersService ownersService) {
    this.ownersService = ownersService;
  }

  @GET
  @Path("/top-orgs")
  public ApiTopOrgsResponse getTopOrgs(@QueryParam("limit") Integer limit) {
    int effectiveLimit = capLimit(limit, TOP_ORGS_DEFAULT_LIMIT, TOP_ORGS_MAX_LIMIT);
    return ownersService.getTopOrgs(effectiveLimit);
  }

  @GET
  @Path("/orgs/{orgId}/apps")
  public ApiOrgAppsResponse getOrgApps(
      @PathParam("orgId") String orgId,
      @QueryParam("limit") Integer limit)
  {
    int effectiveLimit = capLimit(limit, ORG_APPS_DEFAULT_LIMIT, ORG_APPS_MAX_LIMIT);
    return ownersService.getOrgApps(orgId, effectiveLimit);
  }

  @GET
  @Path("/search")
  public ApiOwnerSearchResponse searchOwners(
      @QueryParam("query") String query,
      @QueryParam("type") String type,
      @QueryParam("limit") Integer limit)
  {
    if (query == null || query.isBlank()) {
      throw new GuideApiException(Response.Status.BAD_REQUEST, "query parameter is required");
    }
    String normalized = query.strip();
    if (normalized.length() < SEARCH_MIN_QUERY_LENGTH) {
      throw new GuideApiException(Response.Status.BAD_REQUEST,
          "query must be at least " + SEARCH_MIN_QUERY_LENGTH + " characters");
    }
    if (query.length() > SEARCH_MAX_QUERY_LENGTH) {
      throw new GuideApiException(Response.Status.BAD_REQUEST,
          "query must not exceed " + SEARCH_MAX_QUERY_LENGTH + " characters");
    }
    if (type != null && !type.isEmpty()
        && !"all".equalsIgnoreCase(type) && !"org".equalsIgnoreCase(type) && !"app".equalsIgnoreCase(type))
    {
      throw new GuideApiException(Response.Status.BAD_REQUEST,
          "type must be one of: all, org, app");
    }
    int effectiveLimit = capLimit(limit, SEARCH_DEFAULT_LIMIT, SEARCH_MAX_LIMIT);
    return ownersService.searchOwners(normalized, type, effectiveLimit);
  }

  @GET
  @Path("/{ownerId}")
  public OwnerSummary resolveOwner(@PathParam("ownerId") String ownerId) {
    return ownersService.resolveOwner(ownerId);
  }

  private static int capLimit(Integer value, int defaultValue, int maxValue) {
    if (value == null) {
      return defaultValue;
    }
    if (value < 1) {
      throw new GuideApiException(Response.Status.BAD_REQUEST, "limit must be at least 1");
    }
    if (value > maxValue) {
      throw new GuideApiException(Response.Status.BAD_REQUEST,
          "limit must not exceed " + maxValue);
    }
    return value;
  }
}
