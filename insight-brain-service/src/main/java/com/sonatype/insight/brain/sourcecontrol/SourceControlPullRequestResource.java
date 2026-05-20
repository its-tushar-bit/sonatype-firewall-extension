/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.io.IOException;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.PullRequestSubmissionDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.git.pullrequestcreationservice.PullRequestSubmissionResultDTO;
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

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.INITIATE_PULL_REQUEST)
  public PullRequestSubmissionResultDTO createPullRequest(
      final PullRequestSubmissionDTO pullRequestSubmission) throws IOException
  {
    return sourceControlPullRequestService.createPullRequest(pullRequestSubmission);
  }
}
