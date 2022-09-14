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

import com.sonatype.clm.dto.model.organization.OrganizationSummaryList;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Organization rest resource for integration with other tools
 *
 * @since 1.144.0
 */
@Named
@Timed
@Path(DefaultOrganizationSummaryResource.RESOURCE_PATH)
public class DefaultOrganizationSummaryResource
    implements OrganizationSummaryResource
{
  public static final String RESOURCE_PATH = "rest/integration/organizations";

  public static final String GOAL_PARAM = "goal";

  private static final Logger log = LoggerFactory.getLogger(DefaultOrganizationSummaryResource.class);

  private final OrganizationSummaryService organizationSummaryService;

  @Inject
  public DefaultOrganizationSummaryResource(final OrganizationSummaryService organizationSummaryService) {
    this.organizationSummaryService = organizationSummaryService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Override
  public OrganizationSummaryList getOrganizations(@QueryParam("goal") Goal goal) {
    log.debug("Received request to get organizations for goal {}", goal);
    return organizationSummaryService.getOrganizations(goal);
  }
}
