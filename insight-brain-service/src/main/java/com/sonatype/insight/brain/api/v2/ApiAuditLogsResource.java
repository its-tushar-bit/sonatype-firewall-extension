/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiAuditLogsService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * curl -u admin:admin123 "http://localhost:8070/api/v2/auditLogs?startUtcDate=2024-03-10&endUtcDate=2024-03-13"
 * >audit.log
 */
@Named
@Path(PublicApiPaths.AUDIT_LOGS_RESOURCE_PATH)
@Tag(name = "Audit Logs",
    description = "Use this REST API to access the IQ Server audit logs.")

public class ApiAuditLogsResource
{
  private final ApiAuditLogsService apiAuditLogsService;

  @Inject
  public ApiAuditLogsResource(final ApiAuditLogsService apiAuditLogsService) {
    this.apiAuditLogsService = apiAuditLogsService;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Operation(description =
      "Use this method to retrieve the audit events for the specified time period." +
          "\n" +
          "\n" +
          "Permissions required: Access Audit Log",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response text contains lines of audit events in chronologically ascending order.",
              content =
                  {
                      @Content(mediaType = MediaType.TEXT_PLAIN)
                  })
      }
  )

  @Audited(AuditEvent.EXPORT_AUDIT_LOG)
  public StreamingOutput getAuditLogs(
      @Parameter(description = "Enter the start UTC date in the format (yyyy-mm-dd).")
      @QueryParam("startUtcDate") final String startUtcDate,
      @Parameter(description = "Enter the end UTC date in the format (yyyy-mm-dd).")
      @QueryParam("endUtcDate") final String endUtcDate)
  {
    return apiAuditLogsService.getAuditLogs(startUtcDate, endUtcDate);
  }
}
