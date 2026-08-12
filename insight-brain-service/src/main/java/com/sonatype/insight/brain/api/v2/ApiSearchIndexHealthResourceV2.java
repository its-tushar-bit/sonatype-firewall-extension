/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchIndexAnalyzeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchIndexJobRequestDTO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.searchindex.SearchIndexEstateSnapshot;
import com.sonatype.insight.brain.model.searchindex.SearchIndexGeneration;
import com.sonatype.insight.brain.model.searchindex.SearchIndexHealth;
import com.sonatype.insight.brain.model.searchindex.SearchIndexJob;
import com.sonatype.insight.brain.model.searchindex.SearchIndexJobEvent;
import com.sonatype.insight.brain.search.index.SearchIndexHealthService;
import com.sonatype.insight.brain.search.index.SearchIndexJobService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.NotFoundException;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Analyze + job control for Nexus One Index Health (CLM-43997).
 * <p>
 * Every endpoint costs a constant number of round trips, on precomputed CURRENT rows rather than on
 * anything that scales with the estate. Constant is not the same as read-only: Analyze rewrites the
 * derived status block, which is what lets everything else read it without recomputing.
 */
@Named
@Timed
@Path(PublicApiPaths.ADVANCED_SEARCH_RESOURCE_PATH_V2)
@Tag(name = "Advanced Search Index Health",
    description = "Index Analyze status and rebuild/cleanup job control.")
public class ApiSearchIndexHealthResourceV2
{
  static final String ANALYZE_PATH = "index/analyze";

  static final String JOBS_PATH = "index/jobs";

  private final SearchIndexHealthService healthService;

  private final SearchIndexJobService jobService;

  @Inject
  public ApiSearchIndexHealthResourceV2(
      final SearchIndexHealthService healthService,
      final SearchIndexJobService jobService)
  {
    this.healthService = healthService;
    this.jobService = jobService;
  }

  @GET
  @Path(ANALYZE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Operation(description = "Return current index health (green/yellow/red), recommendation, and estate snapshot. "
      + "Works on precomputed CURRENT rows in a constant number of round trips, refreshing the stored "
      + "derived status as it goes.")
  @ApiResponse(responseCode = "200", description = "Analyze status", useReturnTypeSchema = true)
  public ApiSearchIndexAnalyzeDTO analyze() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.verifyEnabled();
    healthService.refreshDerivedStatus();

    SearchIndexHealth health = healthService.getCurrentHealth();
    SearchIndexEstateSnapshot estate = healthService.getCurrentEstate();
    ApiSearchIndexAnalyzeDTO dto = new ApiSearchIndexAnalyzeDTO();
    dto.healthStatus = health.getHealthStatus();
    dto.recommendedOp = health.getRecommendedOp();
    dto.queueLagSeconds = health.getQueueLagSeconds();
    dto.pendingChangeCount = health.getPendingChangeCount();
    dto.failedChangeCount = health.getFailedChangeCount();
    dto.nouxUnlockState = health.getNouxUnlockState();
    dto.activeJobId = health.getActiveJobId();
    dto.servingGenerationId = healthService.getServingGeneration().map(SearchIndexGeneration::getId).orElse(null);
    dto.buildingGenerationId = healthService.getBuildingGeneration().map(SearchIndexGeneration::getId).orElse(null);
    dto.lastSuccessfulCutoverAt = health.getLastSuccessfulCutoverAt();
    dto.lastCleanupAt = health.getLastCleanupAt();
    if (estate != null) {
      dto.applicationCount = estate.getApplicationCount();
      dto.violationCount = estate.getViolationCount();
      dto.componentCount = estate.getComponentCount();
      dto.etaLowMinutes = estate.getEtaLowMinutes();
      dto.etaHighMinutes = estate.getEtaHighMinutes();
      dto.advancedSearchEnabled = estate.isAdvancedSearchEnabled();
      dto.estateCapturedAt = estate.getCapturedAt();
    }
    return dto;
  }

  @POST
  @Path(JOBS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Operation(description = "Start an index job (FULL_REBUILD or FIRST_TIME_INDEX).")
  @ApiResponse(responseCode = "200", description = "Created job", useReturnTypeSchema = true)
  public SearchIndexJob startJob(final ApiSearchIndexJobRequestDTO body) {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.verifyEnabled();
    // startJob owns validation and defaulting for both fields, so this only unpacks the body.
    return jobService.startJob(
        body == null ? null : body.jobType,
        body == null ? null : body.trigger);
  }

  @GET
  @Path(JOBS_PATH + "/active")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Operation(description = "Return the active index job, if any.")
  public SearchIndexJob getActiveJob() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.verifyEnabled();
    return jobService.getActiveJob().orElse(null);
  }

  @GET
  @Path(JOBS_PATH + "/{jobId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Operation(description = "Return a job by id.")
  public SearchIndexJob getJob(@PathParam("jobId") final String jobId) {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.verifyEnabled();
    return jobService.getJob(jobId).orElseThrow(() -> new NotFoundException("Index job not found: " + jobId));
  }

  @GET
  @Path(JOBS_PATH + "/{jobId}/events")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Operation(description = "Return structured job events (activity log).")
  public List<SearchIndexJobEvent> getJobEvents(
      @PathParam("jobId") final String jobId,
      @DefaultValue("100") @QueryParam("limit") final int limit)
  {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.verifyEnabled();
    jobService.getJob(jobId).orElseThrow(() -> new NotFoundException("Index job not found: " + jobId));
    return jobService.getJobEvents(jobId, Math.min(Math.max(limit, 1), 500));
  }

  @POST
  @Path(JOBS_PATH + "/cancel")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Operation(description = "Cancel the active job; Lucene keeps serving the blue index.")
  public SearchIndexJob cancelActiveJob() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.verifyEnabled();
    return jobService.cancelActiveJob();
  }
}
