/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.utils.ScopeOwnerUtils;
import com.sonatype.insight.json.store.ApiDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;

/**
 * @since 1.76
 */
public class ApiPolicyWaiverDTO
{
  @JsonInclude(Include.NON_EMPTY)
  public String policyWaiverId;

  @JsonInclude(Include.NON_EMPTY)
  public String policyViolationId;

  public String comment;

  @JsonInclude(Include.NON_EMPTY)
  @ApiDateFormat
  public Date createTime;

  @JsonInclude(Include.NON_EMPTY)
  @ApiDateFormat
  public Date expiryTime;

  @JsonInclude(Include.NON_NULL)
  public Boolean isObsolete;

  /**
   * @since 1.79
   */
  @JsonInclude(Include.NON_EMPTY)
  public String scopeOwnerType;

  /**
   * @since 1.79
   */
  @JsonInclude(Include.NON_EMPTY)
  public String scopeOwnerId;

  /**
   * @since 1.79
   */
  @JsonInclude(Include.NON_EMPTY)
  public String scopeOwnerName;

  /**
   * @since 1.92
   */
  public String hash;

  /**
   * @since 1.92
   */
  public String policyId;

  /**
   * @since 1.122
   */
  @JsonInclude(Include.NON_EMPTY)
  public String vulnerabilityId;

  /**
   * @since 1.125
   */
  @JsonInclude(Include.NON_NULL)
  public String policyName;

  /**
   * @since 1.125
   */
  @JsonInclude(Include.NON_NULL)
  public List<ConstraintFact> constraintFacts;

  /**
   * @since 1.125
   */
  @JsonInclude(Include.NON_NULL)
  public String constraintFactsJson;

  /**
   * @since 1.125
   */
  @JsonInclude(Include.NON_NULL)
  public String componentName;

  /**
   * @since 1.132
   */
  public String creatorId;

  /**
   * @since 1.132
   */
  public String creatorName;

  /**
   * @since 1.140
   */
  public ComponentMatcherStrategyForWaiver matcherStrategy;

  /**
   * @since 1.140
   */
  public String associatedPackageUrl;

  /**
   * @since 1.147
   */
  public ApiComponentIdentifierDTOV2 componentIdentifier;

  /**
   * @since 1.147
   */
  @JsonInclude(Include.NON_NULL)
  public Integer threatLevel;

  /**
   * @since 1.147
   */
  @JsonProperty(access = Access.READ_ONLY)
  public ComponentDisplayName getDisplayName() {
    return this.componentIdentifier == null
        ? null
        : ComponentDisplayNameUtil.fromIdentifier(this.componentIdentifier.toComponentIdentifier());
  }

  /**
   * @since 1.159
   */
  @JsonInclude(Include.NON_NULL)
  public Boolean componentUpgradeAvailable;

  /**
   * @since 1.181
   */
  public String reasonText;

  /**
   * @since 1.185
   */
  public boolean expireWhenRemediationAvailable;

  /**
   * @since 1.186
   */
  public String policyWaiverReasonId;

  /**
   * @since 1.193
   */
  public boolean forContainerImage;

  public boolean forContainerImageComponent;

  public static ApiPolicyWaiverDTO toDto(
      PolicyWaiver policyWaiver,
      PolicyWaiverReason policyWaiverReason,
      Owner owner)
  {
    ApiPolicyWaiverDTO dto = new ApiPolicyWaiverDTO();

    dto.policyWaiverId = policyWaiver.getId();
    dto.comment = policyWaiver.getComment();
    dto.createTime = policyWaiver.getCreateTime();
    dto.expiryTime = policyWaiver.getExpiryTime();
    dto.hash = policyWaiver.getHash();
    dto.policyId = policyWaiver.getPolicyId();
    dto.creatorId = policyWaiver.getCreatorId();
    dto.creatorName = policyWaiver.getCreatorName();
    dto.componentUpgradeAvailable = policyWaiver.isComponentUpgradeAvailable();
    dto.expireWhenRemediationAvailable = policyWaiver.isExpireWhenRemediationAvailable();
    dto.forContainerImage = policyWaiver.isForContainerImage();
    dto.forContainerImageComponent = policyWaiver.isForContainerImageComponent();

    if (policyWaiver.getComponentIdentifier() != null) {
      dto.componentIdentifier = ApiComponentIdentifierDTOV2
          .fromComponentIdentifier(policyWaiver.getComponentIdentifier());
    }

    if (owner != null) {
      dto.scopeOwnerId = owner.getId();
      dto.scopeOwnerType = ScopeOwnerUtils.getScopeOwnerType(owner.getType(), owner.getId());
      dto.scopeOwnerName = owner.getName();
    }

    if (policyWaiver.getComponentMatchStrategy() != null) {
      dto.matcherStrategy = policyWaiver.getComponentMatchStrategy();
      if (policyWaiver.getComponentMatchStrategy() != ALL_COMPONENTS) {
        dto.associatedPackageUrl = policyWaiver.getAssociatedPackageUrl();
      }
    }

    if (policyWaiver.getConstraintFacts() != null) {
      policyWaiver.getConstraintFacts()
          .stream()
          .flatMap(constraintFact -> constraintFact.getConditionFacts().stream().map(ConditionFact::getReference))
          .filter(Objects::nonNull)
          .filter(triggerReference -> triggerReference.getType().equals(SECURITY_VULNERABILITY_REFID))
          .map(TriggerReference::getValue)
          .findFirst()
          .ifPresent(vulnerabilityId -> dto.vulnerabilityId = vulnerabilityId);
    }

    if (policyWaiverReason != null) {
      dto.reasonText = policyWaiverReason.getReasonText();
      dto.policyWaiverReasonId = policyWaiverReason.getId();
    }

    return dto;
  }

  public static ApiPolicyWaiverDTO toDto(
      PolicyWaiver policyWaiver,
      PolicyWaiverReason policyWaiverReason,
      Owner owner,
      String policyViolationId)
  {
    ApiPolicyWaiverDTO dto = toDto(policyWaiver, policyWaiverReason, owner);
    dto.policyViolationId = policyViolationId;

    return dto;
  }

  public static ApiPolicyWaiverDTO toDtoWithConstraints(
      PolicyWaiver policyWaiver,
      PolicyWaiverReason policyWaiverReason,
      Owner owner,
      String policyViolationId)
  {
    ApiPolicyWaiverDTO dto = toDto(policyWaiver, policyWaiverReason, owner, policyViolationId);
    dto.constraintFactsJson = policyWaiver.getConstraintFactsJson();
    dto.constraintFacts = policyWaiver.getConstraintFacts();

    return dto;
  }
}
