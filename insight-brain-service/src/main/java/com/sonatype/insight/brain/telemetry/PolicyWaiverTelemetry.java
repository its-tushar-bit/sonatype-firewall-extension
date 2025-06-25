/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;

public class PolicyWaiverTelemetry
{
  private final String policyWaiverId;

  private final String ownerType;

  private final String ownerId;

  private final String componentFormat;

  private final Long violationTime;

  private final Long waiverTime;

  private final Long waiverExpiration;

  private final String componentHash;

  private final String stageId;

  private String waiverReason;

  @JsonProperty("isForContainerImage")
  private final boolean isForContainerImage;

  @JsonProperty("isForContainerImageComponent")
  private final boolean isForContainerImageComponent;

  public PolicyWaiverTelemetry(
      final PolicyWaiver policyWaiver,
      final String ownerType,
      final ComponentIdentifier componentIdentifier,
      final String stageId,
      final Date violationTime)
  {
    this(policyWaiver.getId(),
        ownerType,
        policyWaiver.getOwnerId(),
        componentIdentifier,
        policyWaiver.getHash(),
        violationTime,
        policyWaiver.getCreateTime(),
        policyWaiver.getExpiryTime(),
        stageId,
        policyWaiver.isForContainerImage(),
        policyWaiver.isForContainerImageComponent());
  }

  PolicyWaiverTelemetry(
      final String policyWaiverId,
      final String ownerType,
      final String ownerId,
      final ComponentIdentifier componentIdentifier,
      final String hash,
      final Date violationTime,
      final Date waiverTime,
      final Date waiverExpiration,
      final String stageId,
      final boolean isForContainerImage,
      final boolean isForContainerImageComponent)
  {
    this.policyWaiverId = policyWaiverId;
    this.ownerType = ownerType;
    this.ownerId = ownerId;
    this.componentFormat = componentIdentifier == null ? null : componentIdentifier.getFormat();
    this.componentHash = hash;
    this.violationTime = violationTime == null ? null : violationTime.toInstant().toEpochMilli();
    this.waiverTime =
        waiverTime == null ? null : waiverTime.toInstant().toEpochMilli();
    this.waiverExpiration =
        waiverExpiration == null ? null : waiverExpiration.toInstant().toEpochMilli();
    this.stageId = stageId;
    this.isForContainerImage = isForContainerImage;
    this.isForContainerImageComponent = isForContainerImageComponent;
  }

  public String getPolicyWaiverId() {
    return policyWaiverId;
  }

  public String getOwnerType() {
    return ownerType;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public String getComponentFormat() {
    return componentFormat;
  }

  public Long getViolationTime() {
    return violationTime;
  }

  public String getWaiverReason() {
    return waiverReason;
  }

  public Long getWaiverTime() {
    return waiverTime;
  }

  public Long getWaiverExpiration() {
    return waiverExpiration;
  }

  public String getComponentHash() {
    return componentHash;
  }

  public String getStageId() {
    return stageId;
  }

  public PolicyWaiverTelemetry withWaiverReason(PolicyWaiverReason policyWaiverReason) {
    if (null != policyWaiverReason) {
      waiverReason = policyWaiverReason.getReasonText();
    }
    return this;
  }

  public boolean isForContainerImage() {
    return isForContainerImage;
  }

  public boolean isForContainerImageComponent() {
    return isForContainerImageComponent;
  }
}
