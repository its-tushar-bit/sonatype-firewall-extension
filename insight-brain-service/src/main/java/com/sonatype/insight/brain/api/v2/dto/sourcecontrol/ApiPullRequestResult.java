/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.sourcecontrol;

import java.util.Date;

/**
 * @since 1.97
 */
public class ApiPullRequestResult
{
  public Date startTime;

  public String title;

  public boolean exceptionThrown;

  public boolean successful;

  public long totalTime;

  public String reasoning;
}
