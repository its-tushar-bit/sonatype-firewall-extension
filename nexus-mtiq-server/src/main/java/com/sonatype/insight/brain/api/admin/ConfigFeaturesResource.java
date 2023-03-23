/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.service.ConfigFeaturesService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.service.banning.MTIQFeatureService;
import com.sonatype.insight.license.model.Feature;

@Named
@MtiqAdminEndpoint
@Path(AdminApiPaths.ADMIN_CONFIG_FEATURES_PATH)
public class ConfigFeaturesResource
{
  public static final String FEATURE = "{feature}";

  public static final String ALL = "all";

  private final MTIQFeatureService mtiqFeatureService;

  private final ConfigFeaturesService configFeaturesService;

  @Inject
  public ConfigFeaturesResource(MTIQFeatureService mtiqFeatureService, ConfigFeaturesService configFeaturesService) {
    this.mtiqFeatureService = mtiqFeatureService;
    this.configFeaturesService = configFeaturesService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Set<Feature> getFeatures() {
    return configFeaturesService.getFeatures();
  }

  @GET
  @Path(ALL)
  @Produces(MediaType.APPLICATION_JSON)
  public Set<Feature> getAllFeatures() {
    return configFeaturesService.getAllFeatures();
  }

  @POST
  @Audited(AuditEvent.SET_FEATURES)
  @Path(FEATURE)
  public void enableFeature(@PathParam("feature") String feature) {
    mtiqFeatureService.enableFeature(feature);
  }

  @DELETE
  @Audited(AuditEvent.UNSET_FEATURES)
  @Path(FEATURE)
  public void disableFeature(@PathParam("feature") String feature) {
    mtiqFeatureService.disableFeature(feature);
  }
}
