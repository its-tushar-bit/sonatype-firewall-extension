/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

import java.util.Date;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

// Internal endpoint reachable by the IQ web client (session/CSRF) AND server-to-server callers
// using IQ API-token (HTTP Basic) auth. Experimental; gated by SLO_VIOLATION_FEED (off by default):
// @HasFeature returns 404 (NotFoundException) when the feature is disabled, so the endpoint's existence
// is not leaked.
@Named
@Timed
@Path(SloViolationsRestResource.RESOURCE_PATH)
@HasFeature(SystemConfigurationPropertyFeature.SLO_VIOLATION_FEED)
public class SloViolationsRestResource
{
  // Path param is named applicationPublicId (not applicationId) so AuditContainerRequestFilter resolves it via
  // public-id lookup (getByPublicId); a param literally named applicationId would be resolved via internal-id
  // getById() and leave the audit record's application null.
  static final String RESOURCE_PATH = "rest/slo/{applicationPublicId}/violations";

  static final String DEFAULT_PAGE_SIZE = "50";

  private final SloViolationFeedService sloViolationFeedService;

  @Inject
  SloViolationsRestResource(final SloViolationFeedService sloViolationFeedService) {
    this.sloViolationFeedService = sloViolationFeedService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_SLO_VIOLATIONS)
  public SloViolationFeedResults getSloViolations(
      @PathParam("applicationPublicId") final String applicationPublicId,
      @QueryParam("stageId") final String stageId,
      @QueryParam("updatedSince") final Long updatedSinceEpochMillis,
      @QueryParam("afterViolationId") final String afterViolationId,
      @DefaultValue(DEFAULT_PAGE_SIZE) @QueryParam("pageSize") final int pageSize)
  {
    final Date updatedSince = updatedSinceEpochMillis == null ? null : new Date(updatedSinceEpochMillis);
    return sloViolationFeedService.getSloViolations(
        applicationPublicId, stageId, updatedSince, afterViolationId, pageSize);
  }
}
