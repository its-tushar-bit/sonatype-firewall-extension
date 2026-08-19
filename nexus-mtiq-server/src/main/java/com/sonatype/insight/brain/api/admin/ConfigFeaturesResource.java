/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.service.ConfigFeaturesService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.license.model.Feature;

@Named
@MtiqAdminEndpoint
@Path(AdminApiPaths.ADMIN_TENANT_CONFIG_FEATURES_PATH)
public class ConfigFeaturesResource
{
  public static final String FEATURE = "{feature}";

  public static final String ALL = "all";

  private final ConfigFeaturesService configFeaturesService;

  @Inject
  public ConfigFeaturesResource(ConfigFeaturesService configFeaturesService) {
    this.configFeaturesService = configFeaturesService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Set<Feature> getFeatures(@PathParam("tenantSlug") String tenantSlug) {
    return configFeaturesService.getFeatures(tenantSlug);
  }

  @GET
  @Path(ALL)
  @Produces(MediaType.APPLICATION_JSON)
  public Set<Feature> getAllFeatures(@PathParam("tenantSlug") String tenantSlug) {
    return configFeaturesService.getAllFeatures(tenantSlug);
  }

  @POST
  @Audited(AuditEvent.SET_FEATURES)
  @Path(FEATURE)
  public void enableFeature(@PathParam("tenantSlug") String tenantSlug, @PathParam("feature") String feature) {
    configFeaturesService.enableFeature(tenantSlug, feature);
  }

  @DELETE
  @Audited(AuditEvent.UNSET_FEATURES)
  @Path(FEATURE)
  public void disableFeature(@PathParam("tenantSlug") String tenantSlug, @PathParam("feature") String feature) {
    configFeaturesService.disableFeature(tenantSlug, feature);
  }
}
