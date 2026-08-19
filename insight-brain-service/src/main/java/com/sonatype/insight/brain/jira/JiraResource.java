/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;

/**
 * Returns Jira Project and Issues types for use in policy notifications
 *
 * @since 1.21.0
 */
@Named
@Timed
@Path(JiraResource.RESOURCE_PATH)
public class JiraResource
{
  public static final String RESOURCE_PATH = "rest/jira";

  public static final String ENABLED_PATH = "enabled";

  public static final String PROJECT_PATH = "project";

  private final JiraService jiraService;

  @Inject
  public JiraResource(final JiraService jiraService) {
    this.jiraService = jiraService;
  }

  @GET
  @Path(JiraResource.ENABLED_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public boolean isEnabled() {
    return jiraService.isEnabled();
  }

  @GET
  @Path(JiraResource.PROJECT_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<JiraProject> getProjectsWithAcceptableIssueTypes() throws IOException {
    return jiraService.getProjectsWithAcceptableIssueTypes();
  }
}
