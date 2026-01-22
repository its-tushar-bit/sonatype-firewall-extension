/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
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
  @Produces("text/csv")
  @Audited(AuditEvent.EXPORT_AUDIT_LOG)
  @HasFeature(SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING)
  public Response exportUserActivity(
      @QueryParam("startUtcDate") final String startUtcDate,
      @QueryParam("endUtcDate") final String endUtcDate,
      @QueryParam("username") final String username,
      @DefaultValue("1000") @QueryParam("limit") final Integer limit,
      @DefaultValue("0") @QueryParam("offset") final Integer offset,
      @QueryParam("activityTypes") final List<String> activityTypes,
      @QueryParam("domains") final List<String> domains,
      @QueryParam("errorTypes") final List<String> errorTypes)
  {
    List<ApiActivityEventDTO> activities = userActivityService.getAllUserActivitiesForExport(
        startUtcDate, endUtcDate, username, limit, offset, activityTypes, domains, errorTypes);

    final String fileName = username != null && !username.trim().isEmpty()
        ? "user_activity_detail"
        : "user_activity_all";
    return Csv.generate(Response.ok(), fileName, ApiActivityEventDTO.getCsvHeader(), activities)
        .build();
  }
}
