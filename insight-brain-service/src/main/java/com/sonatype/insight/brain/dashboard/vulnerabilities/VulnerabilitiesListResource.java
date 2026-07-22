/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dashboard.DashboardResource;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.utils.Csv;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * Nexus One Vulnerabilities list and blast-radius CSV export (Martha V1 / CLM-42216).
 * <p>
 * Shares {@code rest/dashboard} with {@link DashboardResource} and the other dashboard resources;
 * sub-paths must stay disjoint.
 */
@Named
@Timed
@Path(DashboardResource.RESOURCE_PATH)
@HasFeature(SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI)
public class VulnerabilitiesListResource
{
  public static final String VULNERABILITIES_LIST_PATH = "vulnerabilities/list";

  public static final String VULNERABILITIES_EXPORT_PATH = "vulnerabilities/export";

  private final VulnerabilitiesListService service;

  private final VulnerabilitiesExportService exportService;

  @Inject
  public VulnerabilitiesListResource(
      final VulnerabilitiesListService service,
      final VulnerabilitiesExportService exportService)
  {
    this.service = service;
    this.exportService = exportService;
  }

  @POST
  @Path(VULNERABILITIES_LIST_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getVulnerabilitiesListExceptionMeter")
  @Audited(AuditEvent.VIEW_NEXUS_ONE_VULNERABILITIES_LIST)
  public VulnerabilitiesListResponseDTO listVulnerabilities(final VulnerabilitiesListRequestDTO request) {
    return service.listVulnerabilities(request);
  }

  /**
   * My Scan Data blast-radius CSV: one row per (vulnerability × scan target). Multipart
   * {@code filter} JSON mirrors the list request (minus pagination). Catalog tab is rejected.
   */
  @POST
  @Path(VULNERABILITIES_EXPORT_PATH)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces("text/csv")
  @ExceptionMetered(name = "exportVulnerabilitiesExceptionMeter")
  @Audited(AuditEvent.EXPORT_NEXUS_ONE_VULNERABILITIES_LIST)
  public Response exportVulnerabilities(@FormDataParam("filter") final VulnerabilitiesListRequestDTO request) {
    List<VulnerabilitiesBlastRadiusRowDTO> rows = exportService.exportBlastRadius(request);
    return Csv.generateWithUtf8Bom(
        Response.ok(),
        "vulnerabilities-blast-radius",
        VulnerabilitiesBlastRadiusRowDTO.getCsvHeader(),
        rows).build();
  }
}
