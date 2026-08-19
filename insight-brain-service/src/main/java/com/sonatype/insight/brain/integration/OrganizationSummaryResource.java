/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

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
@Path(OrganizationSummaryResource.RESOURCE_PATH)
public class OrganizationSummaryResource
{
  public static final String RESOURCE_PATH = "rest/integration/organizations";

  public static final String GOAL_PARAM = "goal";

  private static final Logger log = LoggerFactory.getLogger(OrganizationSummaryResource.class);

  private final OrganizationSummaryService organizationSummaryService;

  @Inject
  public OrganizationSummaryResource(final OrganizationSummaryService organizationSummaryService) {
    this.organizationSummaryService = organizationSummaryService;
  }

  /**
   * Gets all organizations for which the current user has permissions required for the specified goal, sorted by
   * (case-insensitive) name.
   *
   * @param goal The goal for getting the list of organizations.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public OrganizationSummaryList getOrganizations(@QueryParam("goal") Goal goal) {
    log.debug("Received request to get organizations for goal {}", goal);
    return organizationSummaryService.getOrganizations(goal);
  }
}
