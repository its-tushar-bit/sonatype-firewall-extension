/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry.autowaivers;

import java.util.Date;

import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public record AutoPolicyWaiverTelemetry(
    String autoPolicyWaiverId,
    OwnerType ownerType,
    String ownerId,
    Integer threatLevel,
    Boolean reachable,
    Boolean pathForward,
    String creatorId,
    String creatorName,
    Long createTime,
    AutoPolicyWaiverAction action,
    PolicyViolation policyViolation)
{
  public AutoPolicyWaiverTelemetry(
      AutoPolicyWaiver autoPolicyWaiver,
      OwnerType ownerType,
      AutoPolicyWaiverAction action,
      PolicyViolation policyViolation)
  {
    this(
        autoPolicyWaiver.getId(),
        ownerType,
        autoPolicyWaiver.getOwnerId(),
        autoPolicyWaiver.getThreatLevel(),
        autoPolicyWaiver.hasReachability(),
        autoPolicyWaiver.hasPathForward(),
        autoPolicyWaiver.getCreatorId(),
        autoPolicyWaiver.getCreatorName(),
        autoPolicyWaiver.getCreateTime().getTime(),
        action,
        policyViolation);
  }

  public static AutoPolicyWaiverTelemetry from(
      final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO,
      final OwnerType ownerType,
      final AutoPolicyWaiverAction action,
      final PolicyViolation policyViolation)
  {
    return new AutoPolicyWaiverTelemetry(
        apiAutoPolicyWaiverDTO.autoPolicyWaiverId,
        ownerType,
        apiAutoPolicyWaiverDTO.ownerId,
        apiAutoPolicyWaiverDTO.threatLevel,
        apiAutoPolicyWaiverDTO.reachability,
        apiAutoPolicyWaiverDTO.pathForward,
        apiAutoPolicyWaiverDTO.creatorId,
        apiAutoPolicyWaiverDTO.creatorName,
        apiAutoPolicyWaiverDTO.createTime.getTime(),
        action,
        policyViolation);
  }

  public AutoPolicyWaiver toAutoPolicyWaiver() {
    return new AutoPolicyWaiver(
        autoPolicyWaiverId,
        ownerId,
        threatLevel != null ? threatLevel : 0,
        reachable != null ? reachable : false,
        pathForward != null ? pathForward : false,
        creatorId,
        creatorName,
        new Date(createTime));
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(autoPolicyWaiverId)
        .append(ownerId)
        .append(ownerType)
        .append(policyViolation != null ? policyViolation.getId() : "")
        .append(action.toString())
        .toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AutoPolicyWaiverTelemetry telemetry) {
      return new EqualsBuilder()
          .append(autoPolicyWaiverId, telemetry.autoPolicyWaiverId)
          .append(ownerId, telemetry.ownerId)
          .append(ownerType, telemetry.ownerType)
          .append(policyViolation != null ? policyViolation.getId() : "",
              telemetry.policyViolation != null ? telemetry.policyViolation.getId() : "")
          .append(action.toString(), telemetry.action.toString())
          .isEquals();
    }
    return false;
  }

  public enum AutoPolicyWaiverAction
  {
    CREATE,
    UPDATE,
    DELETE,
    APPLY
  }
}
