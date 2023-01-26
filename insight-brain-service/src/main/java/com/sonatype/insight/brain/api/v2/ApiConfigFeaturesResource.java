/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

@Named
@Path(PublicApiPaths.CONFIG_FEATURES_PATH)
public class ApiConfigFeaturesResource
{
  public static final String FEATURE = "{feature}";

  private final ApiConfigFeaturesService apiConfigFeaturesService;

  @Inject
  public ApiConfigFeaturesResource(ApiConfigFeaturesService apiConfigFeaturesService) {
    this.apiConfigFeaturesService = apiConfigFeaturesService;
  }

  @POST
  @Audited(AuditEvent.SET_FEATURES)
  @Path(FEATURE)
  public void enabledFeature(@PathParam("feature") String feature) {
    apiConfigFeaturesService.enableFeature(feature);
  }

  @DELETE
  @Audited(AuditEvent.UNSET_FEATURES)
  @Path(FEATURE)
  public void disableFeature(@PathParam("feature") String feature) {
    apiConfigFeaturesService.disableFeature(feature);
  }
}
