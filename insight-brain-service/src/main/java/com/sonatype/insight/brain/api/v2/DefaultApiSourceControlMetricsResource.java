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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiPullRequestResults;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.97
 */
@Named
@Timed
@Path(value = PublicApiPaths.SOURCE_CONTROL_METRICS_PATH_V2)
public class DefaultApiSourceControlMetricsResource implements ApiSourceControlMetricsResource
{
  private static final String OWNER_TYPE = "{ownerType:application|organization}";

  private static final String OWNER_ID = "{internalOwnerId}";

  /* paths are package private for use in tests */
  static final String BY_OWNER = OWNER_TYPE + "/" + OWNER_ID;

  private final ApiSourceControlService sourceControlService;

  @Inject
  public DefaultApiSourceControlMetricsResource(final ApiSourceControlService apiSourceControlService) {
    this.sourceControlService = apiSourceControlService;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  public ApiPullRequestResults getSourceControl(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId)
  {
    return sourceControlService.getSourceControlMetricsForApplication(ownerType, internalOwnerId);
  }
}
