/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class SourceControlPullRequestServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private SourceControlPullRequestService service;

  @Test(expected = UnauthenticatedException.class)
  public void testGetPullRequestStatus_Unauthenticated() {
    service.getPullRequestStatus("pullRequestId");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPullRequestStatus_Unauthorized() {
    login();
    service.getPullRequestStatus(tempEntity.newSourceControlEvaluationEvent(app).getId());
  }

  @Test(expected = NotFoundException.class)
  public void testGetPullRequestStatus_Authorized() {
    grantReadPermission(app.getId());
    service.getPullRequestStatus(tempEntity.newSourceControlEvaluationEvent(app).getId());
  }
}
