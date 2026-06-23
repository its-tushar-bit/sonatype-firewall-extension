/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.consumption.Aggregation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionDailyHistoryDTO;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionDateRange;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionHistoryBreakdownDTO;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionHistoryEntryDTO;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionSummaryDTO;
import com.sonatype.insight.brain.service.consumption.dto.ConsumptionTopAppsResponseDTO;
import com.sonatype.insight.brain.utils.Csv;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST resource for consumption summary, history, and CSV export.
 *
 * @since 1.204
 */
@Named
@Singleton
@Timed
@Path(ConsumptionResource.RESOURCE_PATH)
public class ConsumptionResource
{
  private static final Logger log = LoggerFactory.getLogger(ConsumptionResource.class);

  public static final String RESOURCE_PATH = "api/v2/consumption";

  static final String SUMMARY_PATH = "summary";

  static final String HISTORY_PATH = "history";

  static final String EXPORT_PATH = "export";

  static final String EXPORT_FILE_PREFIX = "consumption";

  private static final Set<Permission> REQUIRED_PERMISSIONS =
      EnumSet.of(Permission.CONFIGURE_SYSTEM, Permission.VIEW_USAGE);

  private final ConsumptionService consumptionService;

  private final ProductLicense productLicense;

  private final PermissionService permissionService;

  private final DateRangeValidator dateRangeValidator;

  @Inject
  public ConsumptionResource(
      ConsumptionService consumptionService,
      ProductLicense productLicense,
      PermissionService permissionService,
      DateRangeValidator dateRangeValidator)
  {
    this.consumptionService = consumptionService;
    this.productLicense = productLicense;
    this.permissionService = permissionService;
    this.dateRangeValidator = dateRangeValidator;
  }

  @GET
  @Path(SUMMARY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Current billing window consumption summary with activity breakdown. "
      + "Requires CONFIGURE_SYSTEM or VIEW_USAGE.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Summary retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Missing permission")
      })
  public Response getSummary(
      @Parameter(
          description = "Start date in YYYY-MM-DD format, inclusive. Optional. Must accompany endDate.") @QueryParam("startDate") String startDate,
      @Parameter(
          description = "End date in YYYY-MM-DD format, inclusive. Optional. Must accompany startDate. Max 366-day range inclusive.") @QueryParam("endDate") String endDate)
  {
    requireAccess();
    Optional<ConsumptionDateRange> range =
        dateRangeValidator.validate(Optional.ofNullable(startDate), Optional.ofNullable(endDate), 366);
    ConsumptionSummaryDTO summary = consumptionService.getCurrentMonthSummary(getSubscriptionDay(), getTier(), range);
    return Response.ok(summary).build();
  }

  @GET
  @Path(HISTORY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "12-month consumption history. Requires CONFIGURE_SYSTEM or VIEW_USAGE.",
      responses = {
        @ApiResponse(responseCode = "200", description = "History retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Missing permission")
      })
  public Response getHistory(
      @Parameter(
          description = "Start date in YYYY-MM-DD format, inclusive. Optional. Must accompany endDate.") @QueryParam("startDate") String startDate,
      @Parameter(
          description = "End date in YYYY-MM-DD format, inclusive. Optional. Must accompany startDate. Max 366-day range inclusive.") @QueryParam("endDate") String endDate)
  {
    requireAccess();
    Optional<ConsumptionDateRange> range =
        dateRangeValidator.validate(Optional.ofNullable(startDate), Optional.ofNullable(endDate), 366);
    List<ConsumptionHistoryEntryDTO> history = consumptionService.getMonthlyHistory(getSubscriptionDay(), range);
    return Response.ok(history).build();
  }

  @GET
  @Path("history/breakdown")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Consumption history with per-activity breakdown; aggregation=daily|weekly|monthly. "
      + "Requires CONFIGURE_SYSTEM or VIEW_USAGE.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Breakdown retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid aggregation"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Missing permission")
      })
  public Response getHistoryBreakdown(
      @QueryParam("aggregation") @DefaultValue("monthly") Aggregation aggregation,
      @Parameter(
          description = "Start date in YYYY-MM-DD format, inclusive. Optional. Must accompany endDate.") @QueryParam("startDate") String startDate,
      @Parameter(
          description = "End date in YYYY-MM-DD format, inclusive. Optional. Must accompany startDate. Max 366-day range inclusive.") @QueryParam("endDate") String endDate)
  {
    requireAccess();
    Optional<ConsumptionDateRange> range =
        dateRangeValidator.validate(Optional.ofNullable(startDate), Optional.ofNullable(endDate), 366);
    List<ConsumptionHistoryBreakdownDTO> breakdown =
        consumptionService.getHistoryWithBreakdown(aggregation, getSubscriptionDay(), range);
    return Response.ok(breakdown).build();
  }

  @GET
  @Path("history/by-source")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "12-month consumption history grouped by source. Requires CONFIGURE_SYSTEM or VIEW_USAGE.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Missing permission")
      })
  public Response getHistoryBySource(
      @Parameter(
          description = "Start date in YYYY-MM-DD format, inclusive. Optional. Must accompany endDate.") @QueryParam("startDate") String startDate,
      @Parameter(
          description = "End date in YYYY-MM-DD format, inclusive. Optional. Must accompany startDate. Max 366-day range inclusive.") @QueryParam("endDate") String endDate)
  {
    requireAccess();
    Optional<ConsumptionDateRange> range =
        dateRangeValidator.validate(Optional.ofNullable(startDate), Optional.ofNullable(endDate), 366);
    List<ConsumptionHistoryBreakdownDTO> breakdown =
        consumptionService.getMonthlyHistoryBySource(getSubscriptionDay(), range);
    return Response.ok(breakdown).build();
  }

  @GET
  @Path("history/by-stage")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      description = "12-month consumption history grouped by application stage. "
          + "Events without a matching policy_evaluation row are bucketed as 'Unknown'. "
          + "Requires CONFIGURE_SYSTEM or VIEW_USAGE.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Missing permission")
      })
  public Response getHistoryByStage(
      @Parameter(
          description = "Start date in YYYY-MM-DD format, inclusive. Optional. Must accompany endDate.") @QueryParam("startDate") String startDate,
      @Parameter(
          description = "End date in YYYY-MM-DD format, inclusive. Optional. Must accompany startDate. Max 366-day range inclusive.") @QueryParam("endDate") String endDate)
  {
    requireAccess();
    Optional<ConsumptionDateRange> range =
        dateRangeValidator.validate(Optional.ofNullable(startDate), Optional.ofNullable(endDate), 366);
    List<ConsumptionHistoryBreakdownDTO> breakdown =
        consumptionService.getMonthlyHistoryByStage(getSubscriptionDay(), range);
    return Response.ok(breakdown).build();
  }

  @GET
  @Path("top-apps")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Top consuming apps for current billing month. Requires CONFIGURE_SYSTEM or VIEW_USAGE.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Missing permission")
      })
  public Response getTopApps(
      @Parameter(
          description = "Start date in YYYY-MM-DD format, inclusive. Optional. Must accompany endDate.") @QueryParam("startDate") String startDate,
      @Parameter(
          description = "End date in YYYY-MM-DD format, inclusive. Optional. Must accompany startDate. Max 366-day range inclusive.") @QueryParam("endDate") String endDate)
  {
    requireAccess();
    Optional<ConsumptionDateRange> range =
        dateRangeValidator.validate(Optional.ofNullable(startDate), Optional.ofNullable(endDate), 366);
    ConsumptionTopAppsResponseDTO topApps = consumptionService.getAllConsumingApps(getSubscriptionDay(), range);
    return Response.ok(topApps).build();
  }

  @GET
  @Path("daily-history")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Daily history for last 30 days with cumulative totals, daily average, peak day. "
      + "Requires CONFIGURE_SYSTEM or VIEW_USAGE.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Missing permission")
      })
  public Response getDailyHistory(
      @Parameter(
          description = "Start date in YYYY-MM-DD format, inclusive. Optional. Must accompany endDate.") @QueryParam("startDate") String startDate,
      @Parameter(
          description = "End date in YYYY-MM-DD format, inclusive. Optional. Must accompany startDate. Max 92-day range inclusive.") @QueryParam("endDate") String endDate)
  {
    requireAccess();
    Optional<ConsumptionDateRange> range =
        dateRangeValidator.validate(Optional.ofNullable(startDate), Optional.ofNullable(endDate), 92);
    ConsumptionDailyHistoryDTO dto = consumptionService.getDailyHistory(getSubscriptionDay(), range);
    return Response.ok(dto).build();
  }

  @GET
  @Path(EXPORT_PATH)
  @Produces("text/csv")
  @Audited(AuditEvent.EXPORT_CONSUMPTION_HISTORY)
  @Operation(description = "Export consumption history as CSV. When startDate/endDate are omitted, defaults to "
      + "12-month history. Requires CONFIGURE_SYSTEM or VIEW_USAGE.",
      responses = {
        @ApiResponse(responseCode = "200", description = "CSV body"),
        @ApiResponse(responseCode = "400", description = "Invalid date range"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Missing permission")
      })
  public Response exportCsv(
      @Parameter(
          description = "Start date in YYYY-MM-DD format, inclusive. Optional. Must accompany endDate.") @QueryParam("startDate") String startDate,
      @Parameter(
          description = "End date in YYYY-MM-DD format, inclusive. Optional. Must accompany startDate. Max 366-day range inclusive.") @QueryParam("endDate") String endDate)
  {
    requireAccess();
    Optional<ConsumptionDateRange> range =
        dateRangeValidator.validate(Optional.ofNullable(startDate), Optional.ofNullable(endDate), 366);
    List<ConsumptionHistoryEntryDTO> history = consumptionService.getMonthlyHistory(getSubscriptionDay(), range);
    return Csv.generate(Response.ok(), EXPORT_FILE_PREFIX, ConsumptionHistoryEntryDTO.getCsvHeader(), history).build();
  }

  private void requireAccess() {
    if (!isAuthenticated()) {
      throw new UnauthenticatedException("Authentication required");
    }
    SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.verifyEnabled();
    if (!isAuthorized()) {
      throw new UnauthorizedException("Insufficient permissions: requires CONFIGURE_SYSTEM or VIEW_USAGE");
    }
  }

  private boolean isAuthenticated() {
    try {
      return SecurityUtils.getSubject().isAuthenticated();
    }
    catch (Exception e) {
      log.debug("Error checking authentication status", e);
      return false;
    }
  }

  private boolean isAuthorized() {
    try {
      org.apache.shiro.subject.Subject subject = SecurityUtils.getSubject();
      Set<Permission> granted = permissionService.validatePermission(
          subject, OwnerType.GLOBAL, "global", REQUIRED_PERMISSIONS);
      return !granted.isEmpty();
    }
    catch (Exception e) {
      log.debug("Error checking authorization", e);
      return false;
    }
  }

  private int getSubscriptionDay() {
    // TODO(CLM-39593): resolve from license effective date.
    return BillingWindowUtil.DEFAULT_SUBSCRIPTION_DAY;
  }

  private String getTier() {
    return ConsumptionTierResolver.resolveTier(productLicense);
  }
}
