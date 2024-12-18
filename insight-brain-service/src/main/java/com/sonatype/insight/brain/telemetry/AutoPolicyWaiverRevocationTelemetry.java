/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public record AutoPolicyWaiverRevocationTelemetry(
    String autoPolicyWaiverRevocationId,
    OwnerType ownerType,
    String ownerId,
    Integer threadLevel,
    String autoPolicyWaiverId,
    AutoPolicyWaiverRevocationAction action
)
{
  public AutoPolicyWaiverRevocationTelemetry(
      AutoPolicyWaiverRevocation autoPolicyWaiverRevocation,
      OwnerType ownerType, AutoPolicyWaiverRevocationAction action)
  {
    this(
        autoPolicyWaiverRevocation.getId(),
        ownerType,
        autoPolicyWaiverRevocation.getOwnerId(),
        autoPolicyWaiverRevocation.getThreatLevel(),
        autoPolicyWaiverRevocation.getAutoPolicyWaiverId(),
        action);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(autoPolicyWaiverRevocationId)
        .append(ownerType)
        .append(ownerId)
        .append(threadLevel)
        .append(autoPolicyWaiverId)
        .append(action.toString())
        .toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AutoPolicyWaiverRevocationTelemetry telemetry) {
      return new EqualsBuilder()
          .append(autoPolicyWaiverRevocationId, telemetry.autoPolicyWaiverRevocationId)
          .append(ownerType, telemetry.ownerType)
          .append(ownerId, telemetry.ownerId)
          .append(threadLevel, telemetry.threadLevel)
          .append(autoPolicyWaiverId, telemetry.autoPolicyWaiverId)
          .append(action.toString(), telemetry.action.toString())
          .isEquals();
    }
    return false;
  }

  public enum AutoPolicyWaiverRevocationAction
  {
    CREATE,
    DELETE
  }
}
