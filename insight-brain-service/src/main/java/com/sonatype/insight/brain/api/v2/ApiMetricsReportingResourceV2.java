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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingFlattenedDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiMetricsReportingQueryDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiMetricsReportingServiceV2;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.joda.time.DateTime;

/**
 * @since 1.52
 */
@Named
@Timed
@Path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiMetricsReportingResourceV2.PATH)
@Consumes(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(LicensedFeature.SUCCESS_METRICS)
public class ApiMetricsReportingResourceV2
{
  // Visible for testing
  static final int DEFAULT_CHUNK_SIZE = 1000;

  public static final String PATH = "/metrics";

  // Visible for testing
  static int chunkSize = DEFAULT_CHUNK_SIZE;

  private final ApiMetricsReportingServiceV2 metricsReportingService;

  @Inject
  public ApiMetricsReportingResourceV2(final ApiMetricsReportingServiceV2 metricsReportingService) {
    this.metricsReportingService = metricsReportingService;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_SUCCESS_METRICS)
  @Operation(description = "Use this method to retrieve metrics data such as policy evaluation metrics, " +
      "violation and remediation metrics aggregated monthly or weekly." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "Select the media type JSON or csv for the preferred output format.",
            content = {
              @Content(
                  mediaType = "text/csv"),
              @Content(
                  mediaType = MediaType.APPLICATION_JSON,
                  array = @ArraySchema(schema = @Schema(implementation = ApiMetricsReportingDTOV2.class)))
            })
      })
  public Response getMetrics(ApiMetricsReportingQueryDTOV2 queryDTO) {
    metricsReportingService.validate(queryDTO);
    List<Application> applications = metricsReportingService.getApplications(queryDTO);
    metricsReportingService.auditExportMetricsReport(queryDTO, applications);
    StreamingOutput stream = os -> {
      try (SequenceWriter writer = new ObjectMapper().writer().writeValuesAsArray(os)) {
        DateTime now = new DateTime();
        for (int beginIndex = 0; beginIndex < applications.size(); beginIndex += chunkSize) {
          int endIndex = Math.min(beginIndex + chunkSize, applications.size());
          List<ApiMetricsReportingDTOV2> dtos =
              metricsReportingService.getMetrics(queryDTO, now, applications.subList(beginIndex, endIndex));
          writer.writeAll(dtos);
          writer.flush();
        }
      }
    };
    return Response.ok(stream).build();
  }

  @POST
  @Produces("text/csv")
  @Audited(AuditEvent.EXPORT_SUCCESS_METRICS)
  public Response getFlattenedMetrics(ApiMetricsReportingQueryDTOV2 queryDTO) {
    metricsReportingService.validate(queryDTO);
    List<Application> applications = metricsReportingService.getApplications(queryDTO);
    metricsReportingService.auditExportMetricsReport(queryDTO, applications);
    StreamingOutput stream = os -> {
      CsvMapper csvMapper = new CsvMapper();
      try (SequenceWriter writer = csvMapper
          .writer(csvMapper.schemaFor(ApiMetricsReportingFlattenedDTOV2.class).withHeader())
          .writeValuesAsArray(os))
      {
        DateTime now = new DateTime();
        for (int beginIndex = 0; beginIndex < applications.size(); beginIndex += chunkSize) {
          int endIndex = Math.min(beginIndex + chunkSize, applications.size());
          List<ApiMetricsReportingFlattenedDTOV2> dtos =
              metricsReportingService.getFlattenedMetrics(queryDTO, now, applications.subList(beginIndex, endIndex));
          writer.writeAll(dtos);
          writer.flush();
        }
      }
    };
    return Response.ok(stream).build();
  }
}
