/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Returns Jira Project and Issues types for use in policy notifications
 *
 * @since 1.21.0
 */
@Named
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
