/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.11
 */
@Named
@Timed
@Path(ComponentDetailResource.RESOURCE_PATH)
public class ComponentDetailResource
{
  public static final String RESOURCE_PATH = "rest/componentDetails";

  private final ComponentDetailService componentDetailService;

  @Inject
  public ComponentDetailResource(ComponentDetailService componentDetailService) {
    this.componentDetailService = componentDetailService;
  }

  @GET
  @Path("applications")
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_DASHBOARD_COMPONENT_DETAILS)
  @ProductLicenseEnforcementPoint(LicensedFeature.APPLICATION_REPORTS)
  public List<ApplicationComponentDetailsDTO> getApplicationDetailsByHash(@QueryParam("hash") String hash) {
    return componentDetailService.getApplicationDetailsByHash(hash);
  }

  @GET
  @Path("name")
  @Produces(MediaType.APPLICATION_JSON)
  public ComponentDisplayName getComponentNameByHash(@QueryParam("hash") String hash) {
    return componentDetailService.getComponentNameByHash(hash);
  }

  @GET
  @Path("nameByIdentifier")
  @Produces(MediaType.APPLICATION_JSON)
  public ComponentDisplayName getComponentNameByComponentIdentifier(
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier)
  {
    return ComponentDisplayNameUtil.fromIdentifier(componentIdentifier);
  }
}
