/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scm.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Data transfer object for source control event logging
 */
@JsonInclude(Include.NON_EMPTY)
public class SourceControlEventLogDTO
{
  public String eventType;

  public String eventTimestamp;

  public String userName;

  public String tenant;

  public String applicationId;

  public String applicationPublicId;

  public String applicationName;

  public String organizationId;

  public String organizationName;

  public String scmProvider;

  public String repositoryUrl;

  public String pullRequestNumber;

  public Integer violationsAppeared;

  public Integer violationsCleared;

  public String errorMessage;
}
