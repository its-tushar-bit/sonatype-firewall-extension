/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.application.ApplicationSummaryList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application rest resource for integration with other tools such as Sonar
 *
 * @since 1.11.0
 */
@Named
@Path(ApplicationSummaryResource.RESOURCE_PATH)
public class ApplicationSummaryResource
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationSummaryResource.class);

  public static final String RESOURCE_PATH = "rest/integration/applications";

  static final String GOAL_PARAM = "goal";

  private final ApplicationSummaryService applicationSummaryService;

  @Inject
  public ApplicationSummaryResource(final ApplicationSummaryService applicationSummaryService) {
    this.applicationSummaryService = applicationSummaryService;
  }

  /**
   * Gets all applications for which the current user has permissions required for the specified goal, sorted by
   * (case-insensitive) name.
   * 
   * @param goal The goal for getting the list of applications. Defaults to READ permission for backward compatibility
   *          (Jenkins/Hudson plugin <= 2.12.1, Bamboo plugin <=1.0.0, Eclipse plugin <= 2.8.0, SonarQube plugin <=
   *          1.0.2, Nexus plugins <= 3.0.0).
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicationSummaryList getApplications(@QueryParam(GOAL_PARAM) Goal goal) {
    log.debug("Received request to get applications for goal {}", goal);
    return applicationSummaryService.getApplications(goal);
  }
}
