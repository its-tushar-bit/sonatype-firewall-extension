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
 * Nexus One Components portfolio list endpoint (Martha V1).
 * <p>
 * Shares {@code rest/dashboard} with {@link DashboardResource} and
 * {@link com.sonatype.insight.brain.dashboard.applications.ApplicationsListResource};
 * sub-paths must stay disjoint.
 */
@Named
@Timed
@Path(DashboardResource.RESOURCE_PATH)
@HasFeature(SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI)
public class ComponentsListResource
{
  public static final String COMPONENTS_LIST_PATH = "components/list";

  private final ComponentsListService service;

  @Inject
  public ComponentsListResource(final ComponentsListService service) {
    this.service = service;
  }

  @POST
  @Path(COMPONENTS_LIST_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getComponentsListExceptionMeter")
  @Audited(AuditEvent.VIEW_NEXUS_ONE_COMPONENTS_LIST)
  public ComponentsListResponseDTO listComponents(final ComponentsListRequestDTO request) {
    return service.listComponents(request);
  }
}
