/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.hds.DefaultHdsClient;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.integration.ApplicationSummaryResourceConstants.GOAL_PARAM;
import static com.sonatype.insight.brain.integration.ApplicationSummaryResourceConstants.ORG_ID_PARAM;
import static com.sonatype.insight.brain.integration.ApplicationSummaryResourceConstants.VERIFY_OR_CREATE_APPLICATION_PATH;

/**
 * Application rest resource for integration with other tools such as Sonar
 *
 * @since 1.11.0
 */
@Named
@Timed
@Path(ApplicationSummaryResourceConstants.RESOURCE_PATH)
public class DefaultApplicationSummaryResource
    implements ApplicationSummaryResource
{
  private static final Logger log = LoggerFactory.getLogger(DefaultApplicationSummaryResource.class);

  private final ApplicationSummaryService applicationSummaryService;

  @Inject
  public DefaultApplicationSummaryResource(final ApplicationSummaryService applicationSummaryService) {
    this.applicationSummaryService = applicationSummaryService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Override
  public ApplicationSummaryList getApplications(@QueryParam(GOAL_PARAM) Goal goal) {
    log.debug("Received request to get applications for goal {}", goal);
    return applicationSummaryService.getApplications(goal);
  }

  @POST
  @Path(VERIFY_OR_CREATE_APPLICATION_PATH)
  @Produces("text/plain")
  @Override
  public boolean verifyOrCreateApplication(@PathParam("applicationPublicId") String applicationPublicId,
                                           @QueryParam(GOAL_PARAM) Goal goal,
                                           @QueryParam(ORG_ID_PARAM) String organizationId,
                                           @Context HttpServletRequest request)
  {
    log.debug("Received request to verify access for or create application with public ID {} and goal {}.",
        applicationPublicId, goal);
    return applicationSummaryService.verifyOrCreateApplication(applicationPublicId, organizationId, goal,
        DefaultHdsClient.getClientUserAgent(request));
  }
}
