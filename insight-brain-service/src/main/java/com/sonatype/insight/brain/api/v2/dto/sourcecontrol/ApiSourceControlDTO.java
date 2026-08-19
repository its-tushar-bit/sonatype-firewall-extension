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

  public String authenticationType;

  public String baseBranch;

  public Boolean closePrOnFailedChecksEnabled;

  public Boolean closePrAfterDaysOpenEnabled;

  public Integer closePrAfterDays;

  /**
   * @deprecated Deprecated in 1.124. Can be removed in March 2022.
   *             Use remediationPullRequestsEnabled instead.
   */
  @Deprecated
  public Boolean enablePullRequests;

  public Boolean remediationPullRequestsEnabled;

  /**
   * @deprecated Deprecated in 1.124. Can be removed in March 2022.
   *             Use statusChecksEnabled instead.
   */
  @Deprecated
  public Boolean enableStatusChecks;

  public Boolean statusChecksEnabled;

  public Boolean pullRequestCommentingEnabled;

  public Boolean sourceControlEvaluationsEnabled;

  public String sourceControlScanTarget;

  public Boolean sshEnabled;

  public Boolean commitStatusEnabled;

  public Boolean manualPullRequestsEnabled;

  public Boolean innerSourceAutomatedUpdatesEnabled;

  public Boolean nonGoldenPullRequestsEnabled;
}
