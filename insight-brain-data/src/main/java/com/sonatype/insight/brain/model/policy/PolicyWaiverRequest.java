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
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
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

@Entity
@Table(name = "policy_waiver_request")
public class PolicyWaiverRequest
    implements HasStringId
{
  @Id
  @Column(name = "policy_waiver_request_id")
  private String id;

  @Column(name = "hash")
  private String hash;

  @Column(name = "policy_id")
  private String policyId;

  @Column(name = "owner_id")
  private String ownerId;

  /**
   * ID of the policy violation requested to be waived. Can be an application or repository policy violation.
   */
  @Column(name = "policy_violation_id")
  private String policyViolationId;

  @Column(name = "comment")
  private String comment;

  @Column(name = "request_time")
  private Date requestTime;

  @Column(name = "expiry_time")
  private Date expiryTime;

  @Column(name = "constraint_facts_json")
  private String constraintFactsJson;

  @Transient
  private List<ConstraintFact> constraintFacts;

  @Column(name = "associated_package_url")
  private String associatedPackageUrl;

  @Column(name = "component_match_strategy")
  @Enumerated(EnumType.STRING)
  private ComponentMatcherStrategyForWaiver componentMatchStrategy;

  @Column(name = "component_upgrade_available")
  private Boolean componentUpgradeAvailable;

  @Column(name = "waiver_reason_id")
  private String waiverReasonId;

  @Column(name = "expire_when_remediation_available")
  private boolean expireWhenRemediationAvailable;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private PolicyWaiverRequestStatus status = PolicyWaiverRequestStatus.REQUESTED;

  @Column(name = "requester_id")
  private String requesterId;

  @Column(name = "requester_name")
  private String requesterName;

  @Column(name = "note_to_reviewer")
  private String noteToReviewer;

  @Column(name = "reviewer_id")
  private String reviewerId;

  @Column(name = "reviewer_name")
  private String reviewerName;

  @Column(name = "review_time")
  private Date reviewTime;

  @Column(name = "rejection_reason")
  private String rejectionReason;

  @Column(name = "policy_waiver_id")
  private String policyWaiverId;

  @Transient
  private ComponentIdentifier componentIdentifier;

  public PolicyWaiverRequest() {
  }

  public PolicyWaiverRequest(String policyId, String ownerId, String comment) {
    this.policyId = policyId;
    this.ownerId = ownerId;
    this.comment = comment;
  }

  public PolicyWaiverRequest(String hash, String policyId, String ownerId, String comment) {
    this(policyId, ownerId, comment);
    setHash(hash);
  }

  public PolicyWaiverRequest(
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

  public PolicyWaiverRequest(
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

  public PolicyWaiverRequest(
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

  public PolicyWaiverRequest setHash(String hash) {
    this.hash = HashHelper.truncateHash(hash);
    return this;
  }

  public String getPolicyId() {
    return policyId;
  }

  public PolicyWaiverRequest setPolicyId(String policyId) {
    this.policyId = policyId;
    return this;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public PolicyWaiverRequest setOwnerId(String ownerId) {
    this.ownerId = ownerId;
    return this;
  }

  public String getConstraintFactsJson() {
    return constraintFactsJson;
  }

  public PolicyWaiverRequest setConstraintFactsJson(String constraintFactsJson) {
    if (StringUtils.isBlank(constraintFactsJson)) {
      constraintFactsJson = null;
    }
    this.constraintFactsJson = constraintFactsJson;
    constraintFacts = null;
    return this;
  }

  public PolicyWaiverRequest setConstraintFacts(List<ConstraintFact> constraintFacts) {
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

  public PolicyWaiverRequest setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public Date getExpiryTime() {
    return expiryTime;
  }

  public PolicyWaiverRequest setExpiryTime(Date expiryTime) {
    this.expiryTime = expiryTime;
    return this;
  }

  public String getAssociatedPackageUrl() {
    return associatedPackageUrl;
  }

  public PolicyWaiverRequest setAssociatedPackageUrl(String associatedPackageUrl) {
    this.associatedPackageUrl = associatedPackageUrl;
    return this;
  }

  public ComponentMatcherStrategyForWaiver getComponentMatchStrategy() {
    return componentMatchStrategy;
  }

  public PolicyWaiverRequest setComponentMatchStrategy(ComponentMatcherStrategyForWaiver componentMatchStrategy) {
    this.componentMatchStrategy = componentMatchStrategy;
    return this;
  }

  public Boolean isComponentUpgradeAvailable() {
    return componentUpgradeAvailable;
  }

  public PolicyWaiverRequest setComponentUpgradeAvailable(Boolean componentUpgradeAvailable) {
    this.componentUpgradeAvailable = componentUpgradeAvailable;
    return this;
  }

  public String getWaiverReasonId() {
    return waiverReasonId;
  }

  public PolicyWaiverRequest setWaiverReasonId(String waiverReasonId) {
    this.waiverReasonId = waiverReasonId;
    return this;
  }

  public boolean isExpireWhenRemediationAvailable() {
    return expireWhenRemediationAvailable;
  }

  public PolicyWaiverRequest setExpireWhenRemediationAvailable(boolean expireWhenRemediationAvailable) {
    this.expireWhenRemediationAvailable = expireWhenRemediationAvailable;
    return this;
  }

  public PolicyWaiverRequestStatus getStatus() {
    return status;
  }

  public PolicyWaiverRequest setStatus(final PolicyWaiverRequestStatus status) {
    this.status = status;
    return this;
  }

  public String getRequesterId() {
    return requesterId;
  }

  public PolicyWaiverRequest setRequesterId(final String requesterId) {
    this.requesterId = requesterId;
    return this;
  }

  public String getRequesterName() {
    return requesterName;
  }

  public PolicyWaiverRequest setRequesterName(final String requesterName) {
    this.requesterName = requesterName;
    return this;
  }

  public String getNoteToReviewer() {
    return noteToReviewer;
  }

  public PolicyWaiverRequest setNoteToReviewer(String noteToReviewer) {
    this.noteToReviewer = noteToReviewer;
    return this;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public PolicyWaiverRequest setRejectionReason(final String rejectionReason) {
    this.rejectionReason = rejectionReason;
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

  public String getPolicyWaiverId() {
    return policyWaiverId;
  }

  public PolicyWaiverRequest setPolicyWaiverId(String policyWaiverId) {
    this.policyWaiverId = policyWaiverId;
    return this;
  }

  public Date getRequestTime() {
    return requestTime;
  }

  public PolicyWaiverRequest setRequestTime(Date requestTime) {
    this.requestTime = requestTime;
    return this;
  }

  public String getReviewerId() {
    return reviewerId;
  }

  public PolicyWaiverRequest setReviewerId(String reviewerId) {
    this.reviewerId = reviewerId;
    return this;
  }

  public String getReviewerName() {
    return reviewerName;
  }

  public PolicyWaiverRequest setReviewerName(String reviewerName) {
    this.reviewerName = reviewerName;
    return this;
  }

  public Date getReviewTime() {
    return reviewTime;
  }

  public PolicyWaiverRequest setReviewTime(Date reviewTime) {
    this.reviewTime = reviewTime;
    return this;
  }

  public String getPolicyViolationId() {
    return policyViolationId;
  }

  public PolicyWaiverRequest setPolicyViolationId(String policyViolationId) {
    this.policyViolationId = policyViolationId;
    return this;
  }
}
