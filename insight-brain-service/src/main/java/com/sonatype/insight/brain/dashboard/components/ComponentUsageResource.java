/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dashboard.DashboardResource;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

/**
 * Nexus One Component Detail where-used APIs (CLM-43959).
 * <p>
 * Hash-indexed SQL over {@code owner_component}; not the Classic N+1
 * {@code ComponentDetailService#getApplicationDetailsByHash} path.
 */
@Named
@Timed
@Path(DashboardResource.RESOURCE_PATH)
@HasFeature(SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI)
public class ComponentUsageResource
{
  public static final String COMPONENTS_USAGE_APPLICATIONS_PATH = "components/usage/applications";

  public static final String COMPONENTS_USAGE_ORGANIZATIONS_PATH = "components/usage/organizations";

  private final ComponentUsageService service;

  @Inject
  public ComponentUsageResource(final ComponentUsageService service) {
    this.service = service;
  }

  @POST
  @Path(COMPONENTS_USAGE_APPLICATIONS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getComponentUsageApplicationsExceptionMeter")
  @Audited(AuditEvent.VIEW_DASHBOARD_COMPONENT_DETAILS)
  public ComponentUsageApplicationsResponseDTO listApplications(final ComponentUsageRequestDTO request) {
    return service.listApplications(request);
  }

  @POST
  @Path(COMPONENTS_USAGE_ORGANIZATIONS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getComponentUsageOrganizationsExceptionMeter")
  @Audited(AuditEvent.VIEW_DASHBOARD_COMPONENT_DETAILS)
  public ComponentUsageOrganizationsResponseDTO listOrganizations(final ComponentUsageRequestDTO request) {
    return service.listOrganizations(request);
  }
}
