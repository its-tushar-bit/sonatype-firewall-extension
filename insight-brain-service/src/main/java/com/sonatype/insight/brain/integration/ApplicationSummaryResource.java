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
import com.sonatype.insight.brain.organization.ApplicationService;

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

  private final ApplicationSummaryAdapter applicationAdapter;

  private final ApplicationService applicationService;

  @Inject
  public ApplicationSummaryResource(final ApplicationSummaryAdapter applicationAdapter,
      final ApplicationService applicationService) {
    this.applicationAdapter = applicationAdapter;
    this.applicationService = applicationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicationSummaryList getApplications() {
    return applicationAdapter.convert(applicationService.getApplications());
  }
}
