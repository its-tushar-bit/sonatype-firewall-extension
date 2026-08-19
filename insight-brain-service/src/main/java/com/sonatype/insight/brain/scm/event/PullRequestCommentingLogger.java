/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scm.event;

import java.util.Date;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;

/**
 * Specialized logger for pull request commenting events
 */
public class PullRequestCommentingLogger
    extends AbstractSourceControlEventLogger
{
  public PullRequestCommentingLogger(
      final Date logTimestamp,
      final Application application,
      final Organization organization,
      final GitRepositoryInfo gitRepositoryInfo,
      final CurrentUser currentUser)
  {
    super(logTimestamp, application, organization, gitRepositoryInfo, currentUser);
  }
}
