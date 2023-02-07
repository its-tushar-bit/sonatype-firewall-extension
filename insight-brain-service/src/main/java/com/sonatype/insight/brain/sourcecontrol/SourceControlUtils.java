/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.io.File;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.nexus.scm.SourceControlProvider;

public interface SourceControlUtils
{
  GitRepositoryInfo getGitRepositoryInfoForApplication(String id);

  GitRepositoryInfo getGitRepositoryInfoForRepository(String orgId, String sanitizeUrl,
                                                      SourceControlProvider sourceControlProvider);

  String getScmUserIdForApplication(String applicationId);

  File getCheckoutDirectory(Application applicationId);

  File getCheckoutDirectory(String applicationId);

  void deleteCheckoutDirectory(Application application);

  boolean isBitbucketCloud(GitRepositoryInfo gitRepositoryInfo);

  boolean isScmEnabled(GitRepositoryInfo gitRepositoryInfo);

  boolean isScmEnabled(String applicationId);

  void deleteCheckoutDirectory(String applicationId);
}
