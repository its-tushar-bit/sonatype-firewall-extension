/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.hds;

import java.util.Objects;

import com.sonatype.insight.brain.git.ManualPullRequestImpossibilityReason;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "status")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AutomatedRemediationStatusDTO.ManualPullRequestNotPossibleDTO.class,
      name = "MANUAL_PULL_REQUEST_NOT_POSSIBLE"),
  @JsonSubTypes.Type(value = AutomatedRemediationStatusDTO.ManualPullRequestPossibleDTO.class,
      name = "MANUAL_PULL_REQUEST_POSSIBLE"),
  @JsonSubTypes.Type(value = AutomatedRemediationStatusDTO.PullRequestCreationPendingDTO.class,
      name = "PULL_REQUEST_CREATION_PENDING"),
  @JsonSubTypes.Type(value = AutomatedRemediationStatusDTO.PullRequestCreationFailedDTO.class,
      name = "PULL_REQUEST_CREATION_FAILED"),
  @JsonSubTypes.Type(value = AutomatedRemediationStatusDTO.PullRequestDTO.class, name = "PULL_REQUEST")
})
public abstract sealed class AutomatedRemediationStatusDTO
{
  private static final Logger log = LoggerFactory.getLogger(AutomatedRemediationStatusDTO.class);

  public final AutomatedRemediationStatus status;

  protected AutomatedRemediationStatusDTO(AutomatedRemediationStatus status) {
    this.status = status;
  }

  public static final class ManualPullRequestNotPossibleDTO
      extends AutomatedRemediationStatusDTO
  {
    public final ManualPullRequestImpossibilityReason reason;

    @JsonCreator
    public ManualPullRequestNotPossibleDTO(@JsonProperty("reason") final ManualPullRequestImpossibilityReason reason) {
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
    public final String id;

    @JsonCreator
    public PullRequestCreationPendingDTO(@JsonProperty("id") final String id) {
      super(AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);
      this.id = Objects.requireNonNull(id);
    }
  }

  public static final class PullRequestCreationFailedDTO
      extends AutomatedRemediationStatusDTO
  {
    public final String reason;

    @JsonCreator
    public PullRequestCreationFailedDTO(@JsonProperty("reason") final String reason) {
      super(AutomatedRemediationStatus.PULL_REQUEST_CREATION_FAILED);
      this.reason = Objects.requireNonNull(reason);
    }
  }

  public static final class PullRequestDTO
      extends AutomatedRemediationStatusDTO
  {
    public final String url;

    public final Integer pullRequestId;

    @JsonCreator
    public PullRequestDTO(
        @JsonProperty("url") final String url,
        @JsonProperty("pullRequestId") final Integer pullRequestId)
    {
      super(AutomatedRemediationStatus.PULL_REQUEST);
      this.url = Objects.requireNonNull(url);
      this.pullRequestId = pullRequestId;
    }
  }

  /**
   * Get the pull request status from a SourceControlEvent
   *
   * @param sourceControlEvent the source control event associated with the pull request
   * @return the pull request status
   */
  public static AutomatedRemediationStatusDTO fromSourceControlEvent(
      final SourceControlEvent sourceControlEvent)
  {
    if (!isRemediationEvent(sourceControlEvent)) {
      throw new IllegalArgumentException(String.format(
          "Source control event with ID '%s' is not a remediation event.", sourceControlEvent.getId()));
    }

    switch (sourceControlEvent.getEventStatus()) {
      case SourceControlEvent.EVENT_STATUS_NEW, SourceControlEvent.EVENT_STATUS_IN_PROGRESS -> {
        return new PullRequestCreationPendingDTO(sourceControlEvent.getId());
      }
      case SourceControlEvent.EVENT_STATUS_ERROR -> {
        String reason = sourceControlEvent.getEventStatusDetails() != null
            ? sourceControlEvent.getEventStatusDetails()
            : "An unknown error occurred.";
        return new PullRequestCreationFailedDTO(reason);
      }
      case SourceControlEvent.EVENT_STATUS_COMPLETE -> {
        String prLink = sourceControlEvent.getEventStatusDetails();
        if (prLink == null) {
          throw new IllegalStateException(
              String.format("URL missing from pull request for id '%s'.",
                  sourceControlEvent.getId()));
        }

        Integer prPullRequestId = null;
        if (sourceControlEvent.getPullRequestNumber() <= 0) {
          log.debug(("Pull request ID missing or invalid for event ID '{}'."), sourceControlEvent.getId());
        }
        else {
          prPullRequestId = sourceControlEvent.getPullRequestNumber();
        }

        return new PullRequestDTO(prLink, prPullRequestId);
      }
      default -> throw new IllegalStateException(String.format(
          "Unsupported event status '%s'.", sourceControlEvent.getEventStatus()));
    }
  }

  private static boolean isRemediationEvent(SourceControlEvent event) {
    return SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT.equals(event.getEventType())
        || SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT.equals(event.getEventType());
  }
}
