/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.6
 */
@Entity
@Table(name = "policy_waiver")
public class PolicyWaiver
    implements HasStringId
{
  @Id
  @Column(name = "policy_waiver_id")
  private String id;

  @Column(name = "hash")
  private String hash;

  @Column(name = "policy_id")
  private String policyId;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "comment")
  private String comment;

  @Column(name = "is_for_container_image")
  private boolean isForContainerImage;

  @Column(name = "is_for_container_image_component")
  private boolean isForContainerImageComponent;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "expiry_time")
  private Date expiryTime;

  @Column(name = "creator_id")
  private String creatorId;

  @Column(name = "creator_name")
  private String creatorName;

  /**
   * @since 1.53
   */
  @Column(name = "constraint_facts_json")
  private String constraintFactsJson;

  @Transient
  private List<ConstraintFact> constraintFacts;

  /**
   * @since 1.140
   */
  @Column(name = "associated_package_url")
  private String associatedPackageUrl;

  /**
   * @since 1.140
   */
  @Column(name = "component_match_strategy")
  @Enumerated(EnumType.STRING)
  private ComponentMatcherStrategyForWaiver componentMatchStrategy;

  /**
   * @since 1.159
   */
  @Column(name = "component_upgrade_available")
  private Boolean componentUpgradeAvailable;

  /**
   * @since 1.181
   */
  @Column(name = "waiver_reason_id")
  private String waiverReasonId;

  /**
   * @since 1.185
   */
  @Column(name = "expire_when_remediation_available")
  private boolean expireWhenRemediationAvailable;

  /**
   * @since 1.186
   */
  @Column(name = "last_renewal_old_expiry_date")
  private Date lastRenewalOldExpiryDate;

  /**
   * @since 1.186
   */
  @Column(name = "last_renewed_by")
  private String lastRenewedBy;

  /**
   * @since 1.186
   */
  @Column(name = "last_renewed_at")
  private Date lastRenewedAt;

  /**
   * @since 1.186
   */
  @Column(name = "last_renewal_comment")
  private String lastRenewalComment;

  /**
   * @since 1.186
   */
  @Column(name = "last_renewal_reason_id")
  private String lastRenewalReasonId;

  /**
   * @since 1.140
   */
  @Transient
  private ComponentIdentifier componentIdentifier;

  public PolicyWaiver() {
  }

  public PolicyWaiver(String policyId, String ownerId, String comment) {
    this.policyId = policyId;
    this.ownerId = ownerId;
    this.comment = comment;
  }

  public PolicyWaiver(String hash, String policyId, String ownerId, String comment) {
    this(policyId, ownerId, comment);
    setHash(hash);
  }

  public PolicyWaiver(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      String comment)
  {
    this(policyId, ownerId, comment);
    setHash(hash);
    setConstraintFacts(constraintFacts);
  }

  public PolicyWaiver(
      String hash,
      String policyId,
      String ownerId,
      List<ConstraintFact> constraintFacts,
      String associatedPackageUrl,
      ComponentMatcherStrategyForWaiver componentMatchStrategy,
      String comment)
  {
    this(hash, policyId, ownerId, constraintFacts, comment);
    setAssociatedPackageUrl(associatedPackageUrl);
    setComponentMatchStrategy(componentMatchStrategy);
  }

  public PolicyWaiver(
      String hash,
      String policyId,
      String ownerId,
      String associatedPackageUrl,
      ComponentMatcherStrategyForWaiver componentMatchStrategy,
      String comment)
  {
    this(hash, policyId, ownerId, null, associatedPackageUrl, componentMatchStrategy, comment);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getHash() {
    return hash;
  }

  public PolicyWaiver setHash(String hash) {
    this.hash = HashHelper.truncateHash(hash);
    return this;
  }

  public String getPolicyId() {
    return policyId;
  }

  public PolicyWaiver setPolicyId(String policyId) {
    this.policyId = policyId;
    return this;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public PolicyWaiver setOwnerId(String ownerId) {
    this.ownerId = ownerId;
    return this;
  }

  public String getConstraintFactsJson() {
    return constraintFactsJson;
  }

  public PolicyWaiver setConstraintFactsJson(String constraintFactsJson) {
    if (StringUtils.isBlank(constraintFactsJson)) {
      constraintFactsJson = null;
    }
    this.constraintFactsJson = constraintFactsJson;
    constraintFacts = null;
    return this;
  }

  public PolicyWaiver setConstraintFacts(List<ConstraintFact> constraintFacts) {
    if (constraintFacts == null || constraintFacts.isEmpty()) {
      this.constraintFacts = null;
      constraintFactsJson = null;
    }
    else {
      this.constraintFacts = constraintFacts;
      constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);
    }
    return this;
  }

  public List<ConstraintFact> getConstraintFacts() {
    if (constraintFacts == null && !StringUtils.isBlank(constraintFactsJson)) {
      try {
        constraintFacts = Arrays.asList(JsonUtils.parse(constraintFactsJson, ConstraintFact[].class));
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed to read constraint facts for policy waiver " + id, e);
      }
    }
    return constraintFacts;
  }

  public String getComment() {
    return comment;
  }

  public PolicyWaiver setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public boolean isForContainerImage() {
    return isForContainerImage;
  }

  public boolean isForContainerImageComponent() {
    return isForContainerImageComponent;
  }

  public PolicyWaiver setForContainerImage(final boolean forContainerImage) {
    isForContainerImage = forContainerImage;
    return this;
  }

  public PolicyWaiver setForContainerImageComponent(final boolean forContainerImageComponent) {
    isForContainerImageComponent = forContainerImageComponent;
    return this;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public PolicyWaiver setCreateTime(Date createTime) {
    this.createTime = createTime;
    return this;
  }

  public Date getExpiryTime() {
    return expiryTime;
  }

  public PolicyWaiver setExpiryTime(Date expiryTime) {
    this.expiryTime = expiryTime;
    return this;
  }

  public String getCreatorId() {
    return creatorId;
  }

  public PolicyWaiver setCreatorId(String creatorId) {
    this.creatorId = creatorId;
    return this;
  }

  public String getCreatorName() {
    return creatorName;
  }

  public PolicyWaiver setCreatorName(String creatorName) {
    this.creatorName = creatorName;
    return this;
  }

  public String getAssociatedPackageUrl() {
    return associatedPackageUrl;
  }

  public PolicyWaiver setAssociatedPackageUrl(String associatedPackageUrl) {
    this.associatedPackageUrl = associatedPackageUrl;
    return this;
  }

  public ComponentMatcherStrategyForWaiver getComponentMatchStrategy() {
    return componentMatchStrategy;
  }

  public PolicyWaiver setComponentMatchStrategy(ComponentMatcherStrategyForWaiver componentMatchStrategy) {
    this.componentMatchStrategy = componentMatchStrategy;
    return this;
  }

  public Boolean isComponentUpgradeAvailable() {
    return componentUpgradeAvailable;
  }

  public PolicyWaiver setComponentUpgradeAvailable(Boolean componentUpgradeAvailable) {
    this.componentUpgradeAvailable = componentUpgradeAvailable;
    return this;
  }

  public String getWaiverReasonId() {
    return waiverReasonId;
  }

  public PolicyWaiver setWaiverReasonId(String waiverReasonId) {
    this.waiverReasonId = waiverReasonId;
    return this;
  }

  public boolean isExpireWhenRemediationAvailable() {
    return expireWhenRemediationAvailable;
  }

  public PolicyWaiver setExpireWhenRemediationAvailable(boolean expireWhenRemediationAvailable) {
    this.expireWhenRemediationAvailable = expireWhenRemediationAvailable;
    return this;
  }

  public ComponentIdentifier getComponentIdentifier() {
    if (componentIdentifier == null) {
      if (associatedPackageUrl == null) {
        return null;
      }
      componentIdentifier = ComponentIdentifierAdapter.toComponentIdentifier(associatedPackageUrl);
    }
    return componentIdentifier;
  }

  public Date getLastRenewalOldExpiryDate() {
    return lastRenewalOldExpiryDate;
  }

  public PolicyWaiver setLastRenewalOldExpiryDate(Date lastRenewalOldExpiryDate) {
    this.lastRenewalOldExpiryDate = lastRenewalOldExpiryDate;
    return this;
  }

  public String getLastRenewedBy() {
    return lastRenewedBy;
  }

  public PolicyWaiver setLastRenewedBy(String lastRenewedBy) {
    this.lastRenewedBy = lastRenewedBy;
    return this;
  }

  public Date getLastRenewedAt() {
    return lastRenewedAt;
  }

  public PolicyWaiver setLastRenewedAt(Date lastRenewedAt) {
    this.lastRenewedAt = lastRenewedAt;
    return this;
  }

  public String getLastRenewalComment() {
    return lastRenewalComment;
  }

  public PolicyWaiver setLastRenewalComment(String lastRenewalComment) {
    this.lastRenewalComment = lastRenewalComment;
    return this;
  }

  public String getLastRenewalReasonId() {
    return lastRenewalReasonId;
  }

  public PolicyWaiver setLastRenewalReasonId(String lastRenewalReasonId) {
    this.lastRenewalReasonId = lastRenewalReasonId;
    return this;
  }

  public enum ComponentMatcherStrategyForWaiver
  {
    DEFAULT,
    EXACT_COMPONENT,
    ALL_COMPONENTS,
    ALL_VERSIONS;

    @Override
    public String toString() {
      return values()[this.ordinal()].name();
    }
  }
}
