/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

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

  @Column(name = "grandfather_time")
  private Date grandfatherTime;

  @Column(name = "fix_time")
  private Date fixTime;

  @Column(name = "seen_by_primary_evaluation")
  private boolean seenByPrimaryEvaluation;

  @Column(name = "seen_by_monitoring_evaluation")
  private boolean seenByMonitoringEvaluation;

  @Column(name = "grandfather_applied")
  private boolean grandfatherApplied;

  public PolicyViolation() {
  }

  public PolicyViolation(PolicyEvaluation evaluation,
                         String policyId,
                         String policyName,
                         int threatLevel,
                         PolicyThreatCategory threatCategory,
                         String hash,
                         ComponentIdentifier componentIdentifier,
                         String constraintFactsJson,
                         String filename)
  {
    super(policyId, policyName, threatLevel, threatCategory, hash, componentIdentifier, constraintFactsJson);
    applicationId = evaluation.getApplicationId();
    stageTypeId = evaluation.getStageTypeId();
    openTime = evaluation.getTime();
    this.filename = filename;
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

  public Date getOpenTime() {
    return openTime;
  }

  public void setOpenTime(Date openTime) {
    this.openTime = openTime;
  }

  @Override
  public boolean isGrandfathered() {
    return grandfatherTime != null;
  }

  public Date getGrandfatherTime() {
    return grandfatherTime;
  }

  public void setGrandfatherTime(Date grandfatherTime) {
    this.grandfatherTime = grandfatherTime;
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

  @Transient
  public boolean isActive() {
    return !isFixed() && !isWaived() && !isGrandfathered();
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

  public boolean isGrandfatherApplied() {
    return grandfatherApplied;
  }

  public void setGrandfatherApplied(boolean grandfatherApplied) {
    this.grandfatherApplied = grandfatherApplied;
  }

  @Override
  public String toString() {
    return "PolicyViolation [id=" + id + ", applicationId=" + getApplicationId() + ", stageTypeId=" + getStageTypeId()
        + ", openTime=" + getOpenTime() + "(" + getOpenTime().getTime() + "), waiveTime=" + getWaiveTime()
        + ", fixTime=" + getFixTime() + ", policyId=" + getPolicyId() + ", policyName=" + getPolicyName()
        + ", threatLevel=" + getThreatLevel() + ", threatCategory=" + getThreatCategory() + ", hash=" + getHash()
        + ", componentIdentifier=" + getComponentIdentifier() + ", actionTypeId=" + getActionTypeId()
        + ", grandfatherApplied=" + isGrandfatherApplied() + "]";
  }
}
