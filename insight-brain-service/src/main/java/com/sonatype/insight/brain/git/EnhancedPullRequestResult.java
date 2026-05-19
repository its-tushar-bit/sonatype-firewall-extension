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

  private boolean manualPR;

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
    this(timing, startTime, target, title, exceptionThrown, false);
  }

  public EnhancedPullRequestResult(
      final PullRequestResult timing,
      final Date startTime,
      final ComponentIdentifier target,
      final String title,
      final boolean exceptionThrown,
      final boolean manualPR)
  {
    this.timing = timing;
    this.startTime = startTime;
    this.target = target;
    this.title = title;
    this.exceptionThrown = exceptionThrown;
    this.manualPR = manualPR;
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

  public boolean isManualPR() {
    return manualPR;
  }

  public String getReasoning() {
    return intuitReasoning(timing, exceptionThrown);
  }

  /**
   * Classify this result for downstream consumers (DAO + UI).
   * <p>
   * Intended to be called only on failure paths — i.e. when the wrapped
   * {@link PullRequestResult} is not successful, or when {@code exceptionThrown}
   * is true. Today's only callers ({@code PullRequestTask} at the
   * {@code !isSuccessful()} branch and the outer {@code catch}) honor this; if
   * a future caller invokes {@code getCategory()} on a successful result the
   * method returns {@code UNKNOWN} defensively rather than throwing, but the
   * intended contract is failure-path-only.
   */
  public PullRequestFailureCategory getCategory() {
    if (exceptionThrown) {
      return PullRequestFailureCategory.SCM_ERROR;
    }
    if (timing != null && timing.isSuccessful()) {
      // Defensive: getCategory() is meant for failure paths. See Javadoc above.
      return PullRequestFailureCategory.UNKNOWN;
    }
    // Both `timing == null` and `timing.isSuccessful() == false` branches map to
    // MANIFEST_COMPONENT_NOT_FOUND so the category stays consistent with
    // intuitReasoning(), which surfaces FAILURE_MESSAGE for both of those cases.
    return PullRequestFailureCategory.MANIFEST_COMPONENT_NOT_FOUND;
  }

  @Override
  public String toString() {
    return "EnhancedPullRequestResult{" +
        "timing=" + timing +
        ", startTime=" + startTime +
        ", target=" + target +
        ", title='" + title + '\'' +
        ", exceptionThrown=" + exceptionThrown +
        ", manualPR=" + manualPR +
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
