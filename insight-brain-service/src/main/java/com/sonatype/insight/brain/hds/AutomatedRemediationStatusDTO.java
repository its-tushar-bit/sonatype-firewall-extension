/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.hds;

import java.util.Objects;

import com.sonatype.insight.brain.git.ManualPullRequestImpossibilityReason;

public abstract sealed class AutomatedRemediationStatusDTO
{
  public final AutomatedRemediationStatus status;

  protected AutomatedRemediationStatusDTO(AutomatedRemediationStatus status) {
    this.status = status;
  }

  public static final class ManualPullRequestNotPossibleDTO
      extends AutomatedRemediationStatusDTO
  {
    public final ManualPullRequestImpossibilityReason reason;

    public ManualPullRequestNotPossibleDTO(final ManualPullRequestImpossibilityReason reason) {
      super(AutomatedRemediationStatus.MANUAL_PULL_REQUEST_NOT_POSSIBLE);
      this.reason = reason;
    }
  }

  public static final class ManualPullRequestPossibleDTO
      extends AutomatedRemediationStatusDTO
  {
    public ManualPullRequestPossibleDTO() {
      super(AutomatedRemediationStatus.MANUAL_PULL_REQUEST_POSSIBLE);
    }
  }

  public static final class PullRequestCreationPendingDTO
      extends AutomatedRemediationStatusDTO
  {
    public PullRequestCreationPendingDTO() {
      super(AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);
    }
  }

  public static final class PullRequestCreationFailedDTO
      extends AutomatedRemediationStatusDTO
  {
    public final String reason;

    public PullRequestCreationFailedDTO(final String reason) {
      super(AutomatedRemediationStatus.PULL_REQUEST_CREATION_FAILED);
      this.reason = Objects.requireNonNull(reason);
    }
  }

  public static final class PullRequestDTO
      extends AutomatedRemediationStatusDTO
  {
    public final String url;

    public PullRequestDTO(final String url) {
      super(AutomatedRemediationStatus.PULL_REQUEST);
      this.url = Objects.requireNonNull(url);
    }
  }
}
