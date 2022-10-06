/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.nexus.iq.manager.PullRequestResult;

/**
 * Collect additional details of an automated Pull Request operation to hopefully better inform clients of how this
 * feature benefits them, and to assist in supporting them when problems are detected.
 */
public class EnhancedPullRequestResult
{
  public static final String EXCEPTION_MESSAGE =
      "An error happened trying to create this PR, look in server logs for more information.";

  public static final String SUCCESS_MESSAGE =
      "A pull request was successfully created to remediate policy violations related to \"%s\".";

  public static final String FAILURE_MESSAGE = "We were unable to remediate policy violations related to \"%s\". " +
      "This usually indicates that we could not find a direct dependency in the project configuration.";

  private PullRequestResult timing;

  private Date startTime;

  private ComponentIdentifier target;

  private String title;

  private boolean exceptionThrown;

  public EnhancedPullRequestResult() {
    // for Jackson
  }

  public EnhancedPullRequestResult(
      final PullRequestResult timing,
      final Date startTime,
      final ComponentIdentifier target,
      final String title,
      final boolean exceptionThrown)
  {
    this.timing = timing;
    this.startTime = startTime;
    this.target = target;
    this.title = title;
    this.exceptionThrown = exceptionThrown;
  }

  public PullRequestResult getTiming() {
    return timing;
  }

  public Date getStartTime() {
    return startTime;
  }

  public ComponentIdentifier getTarget() {
    return target;
  }

  public String getTitle() {
    return title;
  }

  public boolean isExceptionThrown() {
    return exceptionThrown;
  }

  public String getReasoning() {
    return intuitReasoning(timing, exceptionThrown);
  }

  @Override
  public String toString() {
    return "EnhancedPullRequestResult{" +
        "timing=" + timing +
        ", startTime=" + startTime +
        ", target=" + target +
        ", title='" + title + '\'' +
        ", exceptionThrown=" + exceptionThrown +
        ", reasoning='" + getReasoning() + '\'' +
        '}';
  }

  /**
   * Create a textual description of the outcome based on the timing information gathered during a Pull Request
   * operation.
   */
  private String intuitReasoning(final PullRequestResult timing, final boolean exceptionThrown) {
    if (exceptionThrown) {
      return EXCEPTION_MESSAGE;
    }

    if (timing != null && timing.isSuccessful()) {
      return String.format(SUCCESS_MESSAGE, ComponentDisplayNameUtil.fromIdentifier(target));
    }

    return String.format(FAILURE_MESSAGE, ComponentDisplayNameUtil.fromIdentifier(target));
  }
}
