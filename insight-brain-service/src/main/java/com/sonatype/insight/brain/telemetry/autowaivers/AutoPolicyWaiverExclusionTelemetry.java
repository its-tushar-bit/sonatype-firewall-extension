/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry.autowaivers;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public record AutoPolicyWaiverExclusionTelemetry(
    String autoPolicyWaiverExclusionId,
    OwnerType ownerType,
    String ownerId,
    Integer threadLevel,
    String autoPolicyWaiverId,
    AutoPolicyWaiverExclusionAction action)
{
  public AutoPolicyWaiverExclusionTelemetry(
      AutoPolicyWaiverExclusion autoPolicyWaiverExclusion,
      OwnerType ownerType,
      AutoPolicyWaiverExclusionAction action)
  {
    this(
        autoPolicyWaiverExclusion.getId(),
        ownerType,
        autoPolicyWaiverExclusion.getOwnerId(),
        autoPolicyWaiverExclusion.getThreatLevel(),
        autoPolicyWaiverExclusion.getAutoPolicyWaiverId(),
        action);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(autoPolicyWaiverExclusionId)
        .append(ownerType)
        .append(ownerId)
        .append(threadLevel)
        .append(autoPolicyWaiverId)
        .append(action.toString())
        .toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AutoPolicyWaiverExclusionTelemetry telemetry) {
      return new EqualsBuilder()
          .append(autoPolicyWaiverExclusionId, telemetry.autoPolicyWaiverExclusionId)
          .append(ownerType, telemetry.ownerType)
          .append(ownerId, telemetry.ownerId)
          .append(threadLevel, telemetry.threadLevel)
          .append(autoPolicyWaiverId, telemetry.autoPolicyWaiverId)
          .append(action.toString(), telemetry.action.toString())
          .isEquals();
    }
    return false;
  }

  public enum AutoPolicyWaiverExclusionAction
  {
    CREATE,
    DELETE
  }
}
