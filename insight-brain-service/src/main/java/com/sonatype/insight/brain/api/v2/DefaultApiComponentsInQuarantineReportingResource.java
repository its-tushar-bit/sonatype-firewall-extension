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
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentsInQuarantineDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentsInQuarantineReportingService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.77
 */
@Named
@Timed
@Path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + DefaultApiComponentsInQuarantineReportingResource.PATH)
public class DefaultApiComponentsInQuarantineReportingResource implements ApiComponentsInQuarantineReportingResource
{
  public static final String PATH = "/components/quarantined";

  private final ApiComponentsInQuarantineReportingService service;

  @Inject
  public DefaultApiComponentsInQuarantineReportingResource(final ApiComponentsInQuarantineReportingService service) {
    this.service = service;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_QUARANTINED_COMPONENTS)
  public ApiComponentsInQuarantineDTO getComponentsInQuarantine() {
    return service.getComponentsInQuarantine();
  }
}
