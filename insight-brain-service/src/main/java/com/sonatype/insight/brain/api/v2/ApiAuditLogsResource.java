/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiAuditLogsService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

/**
 * curl -u admin:admin123 "http://localhost:8070/api/v2/auditLogs?startUtcDate=2024-03-10&endUtcDate=2024-03-13"
 * >audit.log
 */
@Named
@Path(PublicApiPaths.AUDIT_LOGS_RESOURCE_PATH)
public class ApiAuditLogsResource
{
  private final ApiAuditLogsService apiAuditLogsService;

  @Inject
  public ApiAuditLogsResource(final ApiAuditLogsService apiAuditLogsService) {
    this.apiAuditLogsService = apiAuditLogsService;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Audited(AuditEvent.EXPORT_AUDIT_LOG)
  public StreamingOutput getAuditLogs(
      @QueryParam("startUtcDate") final String startUtcDate,
      @QueryParam("endUtcDate") final String endUtcDate)
  {
    return apiAuditLogsService.getAuditLogs(startUtcDate, endUtcDate);
  }
}
