/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.componentsearch.api;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.componentsearch.dto.ComponentSearchPageResultDTO;
import com.sonatype.insight.brain.componentsearch.model.ComponentMatchSortField;
import com.sonatype.insight.brain.componentsearch.service.CveAffectedComponentSearchService;
import com.sonatype.insight.error.exception.BadRequestException;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.CsvMediaType.TEXT_CSV;
import static com.sonatype.insight.brain.utils.HttpHeaderUtils.buildContentDispositionHeaderValue;
import static java.nio.charset.StandardCharsets.UTF_8;
import static jakarta.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;

/**
 * REST API for searching applications by components.
 */
@Named
@Timed
@Path(PublicApiPaths.COMPONENT_SEARCH_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Component Search", description = "Search applications containing specific components")
public class ApiComponentSearchResource
{
  private static final Logger log = LoggerFactory.getLogger(ApiComponentSearchResource.class);

  /**
   * CVE identifier for the React2Shell vulnerability.
   */
  private static final String REACT2SHELL_CVE_ID = "CVE-2025-55182";

  private static final String REPORT_FILE_PREFIX = "react2shell-vulnerability-report";

  private static final String CSV_HEADER =
      "Application Name,Application ID,Stage,Component Name,Component Version," +
          "Vulnerability ID,Recommended Action,Last Evaluation," +
          "Active Waiver,Violating,Evaluation,";

  // 2 KB - note that we want to flush frequently to avoid timeouts
  private static final int STREAM_BUFFER_SIZE = 2048;

  private static final String TIMESTAMP_FORMAT = "yyyyMMdd-HHmmss";

  private static final int CACHE_MAX_AGE_SECONDS = 300; // 5 minutes

  private static final long KEEP_ALIVE_INTERVAL_MS = 30_000; // 30 seconds

  private static final long KEEP_ALIVE_CHECK_INTERVAL_MS = 15_000; // Check every 15 seconds

  // Lazy holder for shared keep-alive executor - thread only created if endpoint is actually used
  private static class KeepAliveExecutorHolder
  {
    private static final ScheduledExecutorService INSTANCE = new ScheduledThreadPoolExecutor(1);
  }

  private static ScheduledExecutorService getKeepAliveExecutor() {
    return KeepAliveExecutorHolder.INSTANCE;
  }

  protected final CveAffectedComponentSearchService cveAffectedComponentSearchService;

  @Inject
  public ApiComponentSearchResource(final CveAffectedComponentSearchService cveAffectedComponentSearchService) {
    this.cveAffectedComponentSearchService = cveAffectedComponentSearchService;
  }

  @GET
  @Path("/downloadComponentSearchReport")
  @Produces(TEXT_CSV)
  @Operation(
      description = "Export component search results as CSV (streaming). " +
          "Identifies applications containing components affected by one or more CVEs. " +
          "Multiple CVE IDs can be specified using multiple cveId query parameters " +
          "(e.g., ?cveId=CVE-2025-1&cveId=CVE-2025-2). " +
          "If no CVE ID is specified, defaults to CVE-2025-55182 (React2Shell) for backwards compatibility. " +
          "Results are streamed to avoid memory issues with large datasets. " +
          "Keep-alive mechanism prevents ALB timeouts during long-running queries. " +
          "<p>" +
          "Permissions Required: View IQ Elements")
  @ApiResponse(responseCode = "200", description = "CSV file containing the component search results")
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public void exportComponentSearchReport(
      @Parameter(description = "CVE identifier(s). Can be specified multiple times for multiple CVEs. " +
          "Defaults to CVE-2025-55182 if not specified.",
          example = "CVE-2025-55182", required = false) @QueryParam("cveId") Set<String> cveIds,
      @Context HttpServletResponse httpServletResponse) throws IOException
  {
    if (CollectionUtils.isEmpty(cveIds)) {
      cveIds = Set.of(REACT2SHELL_CVE_ID);
    }

    String filename = generateCsvFilename(REPORT_FILE_PREFIX);

    // Configure response headers - must be set before writing any content
    httpServletResponse.setContentType(TEXT_CSV);
    httpServletResponse.setHeader(CONTENT_DISPOSITION, buildContentDispositionHeaderValue(filename));
    httpServletResponse.setHeader(CACHE_CONTROL, "public, max-age=" + CACHE_MAX_AGE_SECONDS);
    httpServletResponse.setBufferSize(0); // Disable buffering for immediate streaming

    streamCsvReport(cveIds, httpServletResponse, KEEP_ALIVE_INTERVAL_MS, KEEP_ALIVE_CHECK_INTERVAL_MS);
  }

  private String generateCsvFilename(final String prefix) {
    String timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
    return prefix + "-" + timestamp + ".csv";
  }

  void streamCsvReport(
      final Set<String> cveIds,
      final HttpServletResponse httpServletResponse,
      final long keepAliveIntervalMs,
      final long keepAliveCheckIntervalMs) throws IOException
  {
    try (BufferedWriter writer = new BufferedWriter(
        new OutputStreamWriter(httpServletResponse.getOutputStream(), UTF_8), STREAM_BUFFER_SIZE))
    {

      // Write header and immediately commit response to start streaming
      writer.write(CSV_HEADER);
      writer.flush();
      httpServletResponse.flushBuffer();

      AtomicLong lastFlushTime = new AtomicLong(System.currentTimeMillis());

      // Start background keep-alive task to prevent ALB timeouts
      ScheduledFuture<?> keepAliveTask = startKeepAliveTask(
          writer, httpServletResponse, lastFlushTime, keepAliveIntervalMs, keepAliveCheckIntervalMs);

      try {
        // Stream data rows as they're produced
        cveAffectedComponentSearchService.searchCveAffectedComponentsStreaming(cveIds)
            .forEach(applicationComponentMatchDTO -> {
              try {
                String dataLine = applicationComponentMatchDTO.toCsvLine();
                synchronized (writer) {
                  writer.write("\r\n");
                  writer.write(dataLine);
                  writer.write(","); // Trailing comma for extra column to absorb keep-alive spaces
                  writer.flush();
                  httpServletResponse.flushBuffer();
                }

                lastFlushTime.set(System.currentTimeMillis());
              }
              catch (IOException e) {
                log.error("Error while writing CSV line", e);
              }
            });

        // Complete final row
        synchronized (writer) {
          writer.write("\r\n");
          writer.flush();
          httpServletResponse.flushBuffer();
        }
      }
      finally {
        keepAliveTask.cancel(false);
      }
    }
    catch (Exception e) {
      log.error("Error streaming component search results", e);
      // Only send error if response hasn't been committed yet
      if (!httpServletResponse.isCommitted()) {
        httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Failed to generate CSV report: " + e.getMessage());
      }
      throw new WebApplicationException("Failed to generate CSV report", e);
    }
  }

  /**
   * Starts a background task that sends keep-alive spaces to prevent ALB timeouts during long-running queries. The task
   * checks every 15 seconds and writes a space if 30+ seconds have passed since the last data write. Spaces are written
   * to the trailing CSV column to maintain valid CSV structure.
   *
   * @param writer the CSV writer
   * @param httpServletResponse the servlet response for forcing buffer flush
   * @param lastFlushTime atomic timestamp of last write operation
   * @param keepAliveIntervalMs minimum milliseconds of inactivity before sending keep-alive (30 seconds)
   * @param keepAliveCheckIntervalMs how often to check if keep-alive is needed (15 seconds)
   * @return scheduled future that can be cancelled when streaming completes
   */
  private ScheduledFuture<?> startKeepAliveTask(
      BufferedWriter writer,
      HttpServletResponse httpServletResponse,
      AtomicLong lastFlushTime,
      long keepAliveIntervalMs,
      long keepAliveCheckIntervalMs)
  {
    return getKeepAliveExecutor().scheduleAtFixedRate(() -> {
      try {
        long timeSinceLastFlush = System.currentTimeMillis() - lastFlushTime.get();
        if (timeSinceLastFlush >= keepAliveIntervalMs) {
          synchronized (writer) {
            writer.write(" "); // Write single space to extend trailing column
            writer.flush();
            httpServletResponse.flushBuffer(); // Force data to wire
          }

          lastFlushTime.set(System.currentTimeMillis());
        }
      }
      catch (IOException e) {
        log.warn("Keep-alive write failed (this may be expected if client disconnected)", e);
      }
      catch (Exception e) {
        log.error("Unexpected error in keep-alive task", e);
      }
    }, keepAliveCheckIntervalMs, keepAliveCheckIntervalMs, TimeUnit.MILLISECONDS);
  }

  @GET
  @Path("/cveAffectedComponents")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      description = "Retrieve paginated list of applications containing components affected by one or more CVEs. " +
          "Multiple CVE IDs can be specified using multiple cveId query parameters " +
          "(e.g., ?cveId=CVE-2025-1&cveId=CVE-2025-2). " +
          "Default page number is 1, default page size is 10. " +
          "Results can be sorted by any column. " +
          "Default sorting (when sortBy is not specified): applicationName (asc), " +
          "then componentName (asc), then cveId (asc). " +
          "When sortBy is explicitly specified, only single-field sorting is applied " +
          "with the specified sortOrder (default: asc). " +
          "<p>" +
          "Permissions Required: View IQ Elements")
  @ApiResponse(responseCode = "200", description = "Paginated list of affected applications and components")
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public ComponentSearchPageResultDTO getCveAffectedComponents(
      @Parameter(description = "CVE identifier(s). Can be specified multiple times for multiple CVEs.",
          example = "CVE-2025-55182", required = true) @QueryParam("cveId") Set<String> cveIds,

      @Parameter(
          description = "Page number (1-indexed, minimum: 1, default: 1)") @QueryParam("pageNumber") @DefaultValue("1") @Min(1) Integer pageNumber,

      @Parameter(
          description = "Number of items per page (1-1000, default: 10)") @QueryParam("pageSize") @DefaultValue("10") @Min(1) @Max(1000) Integer pageSize,

      @Parameter(description = "Sort field: applicationName, applicationId, componentName, " +
          "evaluationDate, stage, activeWaiver, violating, cveId. " +
          "When not specified, sorts by applicationName (asc), then componentName (asc), then cveId (asc)") @QueryParam("sortBy") ComponentMatchSortField sortBy,

      @Parameter(
          description = "Sort order: asc or desc, default: asc") @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder)
  {
    if (CollectionUtils.isEmpty(cveIds)) {
      throw new BadRequestException("At least one CVE ID is required");
    }

    validateSortOrder(sortOrder);
    return cveAffectedComponentSearchService.searchCveAffectedComponentsPaginated(
        cveIds, pageNumber, pageSize, sortBy, sortOrder);
  }

  private void validateSortOrder(final String sortOrder) {
    if (!"asc".equalsIgnoreCase(sortOrder) && !"desc".equalsIgnoreCase(sortOrder)) {
      throw new BadRequestException("sortOrder must be either 'asc' or 'desc'");
    }
  }
}
