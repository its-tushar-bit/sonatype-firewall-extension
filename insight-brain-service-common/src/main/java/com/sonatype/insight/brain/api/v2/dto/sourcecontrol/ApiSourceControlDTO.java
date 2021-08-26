/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.sourcecontrol;

public class ApiSourceControlDTO
{
  public String id;

  public String ownerId;

  public String repositoryUrl;

  public String username;

  public String token;

  public String provider;

  public String baseBranch;

  public Boolean remediationPullRequestsEnabled;

  public Boolean statusChecksEnabled;

  public Boolean pullRequestCommentingEnabled;

  public Boolean sourceControlScansEnabled;

  public String sourceControlScanTarget;
}
