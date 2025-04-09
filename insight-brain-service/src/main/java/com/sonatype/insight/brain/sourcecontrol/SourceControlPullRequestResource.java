/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO;

import com.codahale.metrics.annotation.Timed;
import jakarta.inject.Inject;

@Named
@Timed
@Path(SourceControlPullRequestResource.RESOURCE_PATH)
public class SourceControlPullRequestResource
{
  public static final String RESOURCE_PATH = "rest/sourceControl/pullRequest";

  public static final String STATUS_PATH = "{id}";

  private final SourceControlPullRequestService sourceControlPullRequestService;

  @Inject
  public SourceControlPullRequestResource(final SourceControlPullRequestService sourceControlPullRequestService) {
    this.sourceControlPullRequestService = sourceControlPullRequestService;
  }

  @GET
  @Path(STATUS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public AutomatedRemediationStatusDTO getPullRequestStatus(@PathParam("id") final String id) {
    return sourceControlPullRequestService.getPullRequestStatus(id);
  }
}
