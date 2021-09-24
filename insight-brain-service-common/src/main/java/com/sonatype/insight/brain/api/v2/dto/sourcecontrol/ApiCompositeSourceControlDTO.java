/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.sourcecontrol;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ApiCompositeSourceControlDTO
{
  public String id;

  public String ownerId;

  public String repositoryUrl;

  public ApiCompositeValueDTO<String> provider = new ApiCompositeValueDTO<>();

  public ApiCompositeValueDTO<String> username = new ApiCompositeValueDTO<>();

  public ApiCompositeValueDTO<String> token = new ApiCompositeValueDTO<>();

  public ApiCompositeValueDTO<String> baseBranch = new ApiCompositeValueDTO<>();

  public ApiCompositeValueDTO<Boolean> remediationPullRequestsEnabled = new ApiCompositeValueDTO<>();

  public ApiCompositeValueDTO<Boolean> statusChecksEnabled = new ApiCompositeValueDTO<>();

  public ApiCompositeValueDTO<Boolean> pullRequestCommentingEnabled = new ApiCompositeValueDTO<>();

  public ApiCompositeValueDTO<Boolean> sourceControlScansEnabled = new ApiCompositeValueDTO<>();

  public ApiCompositeValueDTO<String> sourceControlScanTarget = new ApiCompositeValueDTO<>();

  // TODO INT-5455 remove Json Ignore when the UI is ready to be committed & released
  @JsonIgnore
  public ApiCompositeValueDTO<Boolean> sshEnabled = new ApiCompositeValueDTO<>();
}
