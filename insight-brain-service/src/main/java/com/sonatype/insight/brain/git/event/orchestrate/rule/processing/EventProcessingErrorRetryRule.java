/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.processing;

import java.net.UnknownHostException;

import com.sonatype.nexus.scm.api.access.control.ExclusiveAccessRequestTimeoutException;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class EventProcessingErrorRetryRule
{
  public boolean shouldRetry(Exception e) {
    return e instanceof UnknownHostException
        || e instanceof ExclusiveAccessRequestTimeoutException
        || (null != e && !isBlank(e.getMessage()) && e.getMessage().contains("abuse detection"));
  }
}
