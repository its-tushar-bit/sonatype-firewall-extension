/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;

/**
 * @since 1.11
 */
@Entity
@Table(name = "policy_violation")
public class PolicyViolation
    extends AbstractPolicyViolation
{
  @Id
  @Column(name = "policy_violation_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "stage_type_id")
  private String stageTypeId;

  @Column(name = "filename")
  private String filename;

  @Column(name = "open_time")
  private Date openTime;

  @Column(name = "legacy_violation_time")
  private Date legacyViolationTime;

  @Column(name = "fix_time")
  private Date fixTime;

  @Column(name = "seen_by_primary_evaluation")
  private boolean seenByPrimaryEvaluation;

  @Column(name = "seen_by_monitoring_evaluation")
  private boolean seenByMonitoringEvaluation;

  @Column(name = "legacy_violation_applied")
  private boolean legacyViolationApplied;

  @Column(name = "reachability_status")
  @Enumerated(EnumType.STRING)
  private ReachabilityStatus reachabilityStatus;

  @Column(name = "auto_policy_waiver_id")
  private String autoPolicyWaiverId;

  public PolicyViolation() {
  }

  public PolicyViolation(PolicyEvaluation evaluation,
                         Policy policy,
                         String hash,
                         ComponentIdentifier componentIdentifier,
                         List<ConstraintFact> constraintFacts,
                         String filename)
  {
    this(evaluation, policy.getId(), policy.getName(), policy.getThreatLevel(), policy.getThreatCategory(), hash,
        componentIdentifier, constraintFacts, filename);
  }

  public PolicyViolation(PolicyEvaluation evaluation,
                         String policyId,
                         String policyName,
                         int threatLevel,
                         PolicyThreatCategory threatCategory,
                         String hash,
                         ComponentIdentifier componentIdentifier,
                         List<ConstraintFact> constraintFacts,
                         String filename)
  {
    super(policyId, policyName, threatLevel, threatCategory, hash, componentIdentifier, constraintFacts);
    applicationId = evaluation.getApplicationId();
    stageTypeId = evaluation.getStageTypeId();
    openTime = evaluation.getTime();
    this.filename = filename;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  @Override
  public String getStageTypeId() {
    return stageTypeId;
  }

  public void setStageTypeId(String stageTypeId) {
    this.stageTypeId = stageTypeId;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  @Override
  public Date getOpenTime() {
    return openTime;
  }

  public void setOpenTime(Date openTime) {
    this.openTime = openTime;
  }

  @Override
  public boolean isLegacyViolation() {
    return legacyViolationTime != null;
  }

  public Date getLegacyViolationTime() {
    return legacyViolationTime;
  }

  public void setLegacyViolationTime(Date legacyViolationTime) {
    this.legacyViolationTime = legacyViolationTime;
  }

  public boolean isFixed() {
    return getFixTime() != null;
  }

  public Date getFixTime() {
    return fixTime;
  }

  public void setFixTime(Date fixTime) {
    if (this.fixTime != null && fixTime == null) {
      throw new IllegalStateException("Cannot un-fix a policy violation.");
    }
    this.fixTime = fixTime;
  }

  public boolean isSeenByPrimaryEvaluation() {
    return seenByPrimaryEvaluation;
  }

  public void setSeenByPrimaryEvaluation(boolean seenByPrimaryEvaluation) {
    this.seenByPrimaryEvaluation = seenByPrimaryEvaluation;
  }

  public boolean isSeenByMonitoringEvaluation() {
    return seenByMonitoringEvaluation;
  }

  public void setSeenByMonitoringEvaluation(boolean seenByMonitoringEvaluation) {
    this.seenByMonitoringEvaluation = seenByMonitoringEvaluation;
  }

  /**
   * Lifecycle semantics: excludes legacy violations (treat as warnings).
   * For Firewall enforcement, use {@link #isActiveForFirewall()}.
   */
  @Transient
  public boolean isActive() {
    return !isFixed() && !isWaived() && !isLegacyViolation();
  }

  /**
   * Firewall semantics: ignores legacy violations completely.
   * Unlike {@link #isActive()}, legacy violations are treated as active (not excluded).
   */
  @Transient
  public boolean isActiveForFirewall() {
    return !isFixed() && !isWaived();
  }

  @Transient
  public boolean isAutoWaived() {
    return isWaived() && autoPolicyWaiverId != null;
  }

  /**
   * @return the earlier of the fixTime or waiveTime, sorting nulls higher
   */
  @Transient
  public Date getFixOrWaiveTime() {
    Date actualFixTime = getFixTime();
    Date waiveTime = getWaiveTime();

    if (actualFixTime == null) {
      return waiveTime;
    }
    else if (waiveTime == null) {
      return actualFixTime;
    }
    else {
      return actualFixTime.compareTo(waiveTime) > 0 ? waiveTime : actualFixTime;
    }
  }

  public boolean isLegacyViolationApplied() {
    return this.legacyViolationApplied;
  }

  public void setLegacyViolationApplied(boolean legacyPolicyViolationApplied) {
    this.legacyViolationApplied = legacyPolicyViolationApplied;

  }

  public ReachabilityStatus getReachabilityStatus() {
    return this.reachabilityStatus;
  }

  public void setReachabilityStatus(ReachabilityStatus reachabilityStatus) {
    this.reachabilityStatus = reachabilityStatus;
  }

  public String getAutoPolicyWaiverId() {
    return autoPolicyWaiverId;
  }

  public void setAutoPolicyWaiverId(String id) {
    this.autoPolicyWaiverId = id;
  }

  @Transient
  @Override
  public String getOwnerId() {
    return getApplicationId();
  }

  @Override
  public String toString() {
    return "PolicyViolation [id=" + id + ", applicationId=" + getApplicationId() + ", stageTypeId=" + getStageTypeId()
        + ", openTime=" + getOpenTime() + "(" + getOpenTime().getTime() + "), waiveTime=" + getWaiveTime()
        + ", fixTime=" + getFixTime() + ", policyId=" + getPolicyId() + ", policyName=" + getPolicyName()
        + ", threatLevel=" + getThreatLevel() + ", threatCategory=" + getThreatCategory() + ", hash=" + getHash()
        + ", componentIdentifier=" + getComponentIdentifier() + ", actionTypeId=" + getActionTypeId()
        + ", legacyViolationApplied=" + isLegacyViolationApplied() + ", reachabilityStatus=" + getReachabilityStatus() +
        " ]";
  }
}
