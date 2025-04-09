/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestCreationFailedDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestCreationPendingDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
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

  @Inject
  public SourceControlPullRequestService(
      final ApplicationDAO applicationDAO,
      final SourceControlEventDAO sourceControlEventDAO)
  {
    this.applicationDAO = applicationDAO;
    this.sourceControlEventDAO = sourceControlEventDAO;
  }

  public AutomatedRemediationStatusDTO getPullRequestStatus(final String id) {
    checkAuthenticated();
    SourceControlEvent sourceControlEvent = sourceControlEventDAO.getByIdNotNull(id);
    Application application = applicationDAO.getByIdNotNull(sourceControlEvent.getApplicationId());
    return getPullRequestStatus(id, application, sourceControlEvent);
  }

  @Authorize(permission = Permission.READ)
  AutomatedRemediationStatusDTO getPullRequestStatus(
      final String id,
      @AuthzContext(Key.APPLICATION) final Application application,
      final SourceControlEvent sourceControlEvent)
  {
    if (!SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT.equals(sourceControlEvent.getEventType()) &&
        !SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT.equals(sourceControlEvent.getEventType())) {
      throw new NotFoundException(
          "Pull request not found for application '" + application.getPublicId() + "' and for id '" + id +
              "'.");
    }

    switch (sourceControlEvent.getEventStatus()) {
      case SourceControlEvent.EVENT_STATUS_NEW, SourceControlEvent.EVENT_STATUS_IN_PROGRESS -> {
        return new PullRequestCreationPendingDTO();
      }
      case SourceControlEvent.EVENT_STATUS_ERROR -> {
        String reason = sourceControlEvent.getEventStatusDetails() == null ? "An unknown error occurred." :
            sourceControlEvent.getEventStatusDetails();
        return new PullRequestCreationFailedDTO(reason);
      }
      case SourceControlEvent.EVENT_STATUS_COMPLETE -> {
        if (sourceControlEvent.getEventStatusDetails() == null) {
          throw new IllegalStateException("URL missing from pull request for id '" + id + "'.");
        }
        return new PullRequestDTO(sourceControlEvent.getEventStatusDetails());
      }
      default ->
          throw new IllegalStateException("Unsupported event status '" + sourceControlEvent.getEventStatus() + "'.");
    }
  }

  private static void checkAuthenticated() {
    Object principal = SecurityUtils.getSubject().getPrincipal();
    if (principal == null) {
      throw new UnauthenticatedException("Anonymous access forbidden");
    }
  }
}
