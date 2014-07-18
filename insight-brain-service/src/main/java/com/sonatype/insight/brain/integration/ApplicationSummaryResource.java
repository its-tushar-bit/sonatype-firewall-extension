/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.application.ApplicationSummaryList;

/**
 * Application rest resource for integration with other tools such as Sonar
 *
 * @since 1.11.0
 */
@Named
@Path(ApplicationSummaryResource.SERVICE_PATH)
public class ApplicationSummaryResource
{
  public static final String SERVICE_PATH = "rest/integration/applications";

  private final ApplicationSummaryService applicationSummaryService;

  @Inject
  public ApplicationSummaryResource(final ApplicationSummaryService applicationSummaryService) {
    this.applicationSummaryService = applicationSummaryService;
  }

  /**
   * Gets all applications the current user has read access to, sorted by (case-insensitive) name.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicationSummaryList getApplications() {
    return applicationSummaryService.getApplications();
  }
}
