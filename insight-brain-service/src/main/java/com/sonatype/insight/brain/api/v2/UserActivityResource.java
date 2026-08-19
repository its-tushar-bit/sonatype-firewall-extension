/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiUserActivityDetailDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserActivityFilterOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserActivitySummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiActivityEventDTO;
import com.sonatype.insight.brain.api.v2.service.UserActivityService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.utils.Csv;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.197.0
 */
@Named
@Path(PublicApiPaths.USER_ACTIVITY_RESOURCE_PATH)
public class UserActivityResource
{
  private final UserActivityService userActivityService;

  @Inject
  public UserActivityResource(final UserActivityService userActivityService) {
    this.userActivityService = userActivityService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_AUDIT_LOG)
  @HasFeature(SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING)
  public ApiUserActivitySummaryDTO getUserActivitySummary(
      @QueryParam("startUtcDate") final String startUtcDate,
      @QueryParam("endUtcDate") final String endUtcDate,
      @QueryParam("username") final String username,
      @DefaultValue("100") @QueryParam("limit") final Integer limit,
      @DefaultValue("0") @QueryParam("offset") final Integer offset)
  {
    return userActivityService.getUserActivitySummary(startUtcDate, endUtcDate, username, limit, offset);
  }

  @GET
  @Path("/{username}")
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_AUDIT_LOG)
  @HasFeature(SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING)
  public ApiUserActivityDetailDTO getUserActivityDetail(
      @PathParam("username") final String username,
      @QueryParam("startUtcDate") final String startUtcDate,
      @QueryParam("endUtcDate") final String endUtcDate,
      @DefaultValue("100") @QueryParam("limit") final Integer limit,
      @DefaultValue("0") @QueryParam("offset") final Integer offset,
      @QueryParam("activityTypes") final List<String> activityTypes,
      @QueryParam("domains") final List<String> domains,
      @QueryParam("errorTypes") final List<String> errorTypes)
  {
    return userActivityService.getUserActivityDetail(startUtcDate, endUtcDate, username, limit, offset,
        activityTypes, domains, errorTypes);
  }

  @GET
  @Path("/filterOptions")
  @Produces(MediaType.APPLICATION_JSON)
  @HasFeature(SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING)
  public ApiUserActivityFilterOptionsDTO getFilterOptions() {
    return userActivityService.getFilterOptions();
  }

  @GET
  @Path("/export")
  @Produces("text/csv; charset=utf-8")
  @Audited(AuditEvent.EXPORT_AUDIT_LOG)
  @HasFeature(SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING)
  public Response exportUserActivity(
      @QueryParam("startUtcDate") final String startUtcDate,
      @QueryParam("endUtcDate") final String endUtcDate,
      @QueryParam("username") final String username,
      @QueryParam("limit") final Integer limit,
      @QueryParam("offset") final Integer offset,
      @QueryParam("activityTypes") final Set<String> activityTypes,
      @QueryParam("domains") final Set<String> domains,
      @QueryParam("errorTypes") final Set<String> errorTypes)
  {
    final String fileNamePrefix = StringUtils.isNotBlank(username)
        ? "user_activity_detail"
        : "user_activity_all";

    // Validation (date range + non-negative limit/offset) runs synchronously inside
    // streamAllUserActivitiesForExport so a BadRequestException becomes HTTP 400 before
    // Response.ok(streamingOutput) commits the 200 status. limit/offset are applied to the
    // lazy stream by the service.
    Stream<ApiActivityEventDTO> events = userActivityService.streamAllUserActivitiesForExport(
        startUtcDate, endUtcDate, username, limit, offset, activityTypes, domains, errorTypes);

    // flushPerRow=true: ALB / corporate-proxy idle timeouts can drop the connection if the
    // response stalls during a sparse 30-day export (CLM-38045).
    return Csv.generate(Response.ok(), fileNamePrefix, ApiActivityEventDTO.getCsvHeader(), events, true).build();
  }
}
