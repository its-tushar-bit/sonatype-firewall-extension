/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;

import java.io.IOException;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.PullRequestSubmissionDTO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.pullrequestcreationservice.ManualPullRequestCreationService;
import com.sonatype.insight.brain.git.pullrequestcreationservice.PullRequestSubmissionResultDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;

@Named
@Singleton
public class SourceControlPullRequestService
{
  private final ApplicationDAO applicationDAO;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final ManualPullRequestCreationService manualPullRequestCreationService;

  @Inject
  public SourceControlPullRequestService(
      final ApplicationDAO applicationDAO,
      final SourceControlEventDAO sourceControlEventDAO,
      final ManualPullRequestCreationService manualPullRequestCreationService)
  {
    this.applicationDAO = applicationDAO;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.manualPullRequestCreationService = manualPullRequestCreationService;
  }

  public AutomatedRemediationStatusDTO getPullRequestStatus(final String id) {
    checkAuthenticated();
    SourceControlEvent sourceControlEvent = sourceControlEventDAO.getByIdNotNull(id);
    Application application = applicationDAO.getByIdNotNull(sourceControlEvent.getApplicationId());
    return getPullRequestStatus(application, sourceControlEvent);
  }

  @Authorize(permission = Permission.READ)
  AutomatedRemediationStatusDTO getPullRequestStatus(
      @AuthzContext(Key.APPLICATION) final Application application,
      final SourceControlEvent sourceControlEvent)
  {
    try {
      return AutomatedRemediationStatusDTO.fromSourceControlEvent(sourceControlEvent);
    }
    catch (IllegalArgumentException e) {
      throw new NotFoundException(
          "Pull request not found for application '" + application.getPublicId() + "' and for id '" +
              sourceControlEvent.getId() + "'.");
    }
  }

  public PullRequestSubmissionResultDTO createPullRequest(
      final PullRequestSubmissionDTO pullRequestSubmission) throws IOException
  {
    checkAuthenticated();
    validatePullRequestSubmission(pullRequestSubmission);
    Application application;
    try {
      application = applicationDAO.getByIdNotNull(pullRequestSubmission.applicationId());
    }
    catch (NotFoundException e) {
      throw new BadRequestException("Application not found for id '" + pullRequestSubmission.applicationId() + "'.");
    }
    return createPullRequest(application, pullRequestSubmission);
  }

  @Authorize(permission = Permission.CREATE_PULL_REQUESTS)
  PullRequestSubmissionResultDTO createPullRequest(
      @AuthzContext(Key.OWNER) final Owner owner,
      final PullRequestSubmissionDTO pullRequestSubmission) throws IOException
  {
    return manualPullRequestCreationService.createManualRemediationPullRequest(
        owner.getId(),
        pullRequestSubmission.scanId(),
        pullRequestSubmission.componentIdentifier(),
        pullRequestSubmission.targetVersion(),
        pullRequestSubmission.identificationSource(),
        Boolean.TRUE.equals(pullRequestSubmission.isDirectDependency()));
  }

  private void validatePullRequestSubmission(final PullRequestSubmissionDTO pullRequestSubmission) {
    if (pullRequestSubmission == null) {
      throw new BadRequestException("Pull request submission cannot be null");
    }
    if (pullRequestSubmission.applicationId() == null) {
      throw new BadRequestException("Application ID cannot be null");
    }
    if (pullRequestSubmission.scanId() == null) {
      throw new BadRequestException("Scan ID cannot be null");
    }
    if (pullRequestSubmission.componentIdentifier() == null) {
      throw new BadRequestException("Component identifier cannot be null");
    }
    if (pullRequestSubmission.targetVersion() == null) {
      throw new BadRequestException("Target version cannot be null");
    }
    if (pullRequestSubmission.identificationSource() == null) {
      throw new BadRequestException("Identification source cannot be null");
    }
  }

  private static void checkAuthenticated() {
    Object principal = SecurityUtils.getSubject().getPrincipal();
    if (principal == null) {
      throw new UnauthenticatedException("Anonymous access forbidden");
    }
  }
}
