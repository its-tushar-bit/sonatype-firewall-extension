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

  public PolicyWaiverRequest(String hash,
                      String policyId,
                      String ownerId,
                      List<ConstraintFact> constraintFacts,
                      String comment)
  {
    this(policyId, ownerId, comment);
    setHash(hash);
    setConstraintFacts(constraintFacts);
  }

  public PolicyWaiverRequest(String hash,
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

  public PolicyWaiverRequest(String hash,
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

  public void setHash(String hash) {
    this.hash = HashHelper.truncateHash(hash);
  }

  public String getPolicyId() {
    return policyId;
  }

  public void setPolicyId(String policyId) {
    this.policyId = policyId;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getConstraintFactsJson() {
    return constraintFactsJson;
  }

  public void setConstraintFactsJson(String constraintFactsJson) {
    if (StringUtils.isBlank(constraintFactsJson)) {
      constraintFactsJson = null;
    }
    this.constraintFactsJson = constraintFactsJson;
    constraintFacts = null;
  }

  public void setConstraintFacts(List<ConstraintFact> constraintFacts) {
    if (constraintFacts == null || constraintFacts.isEmpty()) {
      this.constraintFacts = null;
      constraintFactsJson = null;
    }
    else {
      this.constraintFacts = constraintFacts;
      constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);
    }
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

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Date getExpiryTime() {
    return expiryTime;
  }

  public void setExpiryTime(Date expiryTime) {
    this.expiryTime = expiryTime;
  }

  public String getAssociatedPackageUrl() {
    return associatedPackageUrl;
  }

  public void setAssociatedPackageUrl(String associatedPackageUrl) {
    this.associatedPackageUrl = associatedPackageUrl;
  }

  public ComponentMatcherStrategyForWaiver getComponentMatchStrategy() {
    return componentMatchStrategy;
  }

  public void setComponentMatchStrategy(ComponentMatcherStrategyForWaiver componentMatchStrategy) {
    this.componentMatchStrategy = componentMatchStrategy;
  }

  public Boolean isComponentUpgradeAvailable() {
    return componentUpgradeAvailable;
  }

  public void setComponentUpgradeAvailable(Boolean componentUpgradeAvailable) {
    this.componentUpgradeAvailable = componentUpgradeAvailable;
  }

  public String getWaiverReasonId() {
    return waiverReasonId;
  }

  public void setWaiverReasonId(String waiverReasonId) {
    this.waiverReasonId = waiverReasonId;
  }

  public boolean isExpireWhenRemediationAvailable() {
    return expireWhenRemediationAvailable;
  }

  public void setExpireWhenRemediationAvailable(boolean expireWhenRemediationAvailable) {
    this.expireWhenRemediationAvailable = expireWhenRemediationAvailable;
  }

  public PolicyWaiverRequestStatus getStatus() {
    return status;
  }

  public void setStatus(final PolicyWaiverRequestStatus status) {
    this.status = status;
  }

  public String getRequesterId() {
    return requesterId;
  }

  public void setRequesterId(final String requesterId) {
    this.requesterId = requesterId;
  }

  public String getRequesterName() {
    return requesterName;
  }

  public void setRequesterName(final String requesterName) {
    this.requesterName = requesterName;
  }

  public String getNoteToReviewer() {
    return noteToReviewer;
  }

  public void setNoteToReviewer(String noteToReviewer) {
    this.noteToReviewer = noteToReviewer;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public void setRejectionReason(final String rejectionReason) {
    this.rejectionReason = rejectionReason;
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

  public void setPolicyWaiverId(String policyWaiverId) {
    this.policyWaiverId = policyWaiverId;
  }

  public Date getRequestTime() {
    return requestTime;
  }

  public void setRequestTime(Date requestTime) {
    this.requestTime = requestTime;
  }

  public String getReviewerId() {
    return reviewerId;
  }

  public void setReviewerId(String reviewerId) {
    this.reviewerId = reviewerId;
  }

  public String getReviewerName() {
    return reviewerName;
  }

  public void setReviewerName(String reviewerName) {
    this.reviewerName = reviewerName;
  }

  public Date getReviewTime() {
    return reviewTime;
  }

  public void setReviewTime(Date reviewTime) {
    this.reviewTime = reviewTime;
  }

  public String getPolicyViolationId() {
    return policyViolationId;
  }

  public void setPolicyViolationId(String policyViolationId) {
    this.policyViolationId = policyViolationId;
  }
}
