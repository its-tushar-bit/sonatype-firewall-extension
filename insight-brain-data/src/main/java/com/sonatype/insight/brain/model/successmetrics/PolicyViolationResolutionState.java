/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.successmetrics;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.model.HasStringId;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.brain.model.policy.PolicyViolationComparable;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.clm.dto.model.policy.Stage;

/**
 * @since 1.33
 */
@Entity
@Table(name = "policy_violation_resolution_state")
public class PolicyViolationResolutionState
    extends HasComponentId
    implements HasStringId, PolicyViolationComparable
{
  @Id
  @Column(name = "policy_violation_resolution_state_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "first_occurrence_time")
  private Date firstOccurrenceTime;

  @Column(name = "policy_id")
  private String policyId;

  @Column(name = "policy_name")
  private String policyName;

  @Column(name = "threat_level")
  private int threatLevel;

  @Column(name = "threat_category")
  @Enumerated(EnumType.STRING)
  private PolicyThreatCategory threatCategory;

  @Column(name = "hash")
  private String hash;

  @Column(name = "develop_stage_type")
  private boolean developStageType = false;

  @Column(name = "build_stage_type")
  private boolean buildStageType = false;

  @Column(name = "stage_release_stage_type")
  private boolean stageReleaseStageType = false;

  @Column(name = "release_stage_type")
  private boolean releaseStageType = false;

  @Column(name = "operate_stage_type")
  private boolean operateStageType = false;

  @Column(name = "proxy_stage_type")
  private boolean proxyStageType = false;

  public PolicyViolationResolutionState() {
  }

  public PolicyViolationResolutionState(String applicationId, PolicyViolation policyViolation) {
    this.applicationId = applicationId;
    this.firstOccurrenceTime = policyViolation.getOpenTime();
    this.policyId = policyViolation.getPolicyId();
    this.policyName = policyViolation.getPolicyName();
    this.threatLevel = policyViolation.getThreatLevel();
    this.threatCategory = policyViolation.getThreatCategory();
    this.hash = policyViolation.getHash();

    // use the string-based setter to avoid JSON parsing overhead.
    copyComponentIdentifierFrom(policyViolation);
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

  public Date getFirstOccurrenceTime() {
    return firstOccurrenceTime;
  }

  public void setFirstOccurrenceTime(Date firstOccurrenceTime) {
    this.firstOccurrenceTime = firstOccurrenceTime;
  }

  public boolean getDevelopStageType() {
    return developStageType;
  }

  public void setDevelopStageType(boolean developStageType) {
    this.developStageType = developStageType;
  }

  public boolean getBuildStageType() {
    return buildStageType;
  }

  public void setBuildStageType(boolean buildStageType) {
    this.buildStageType = buildStageType;
  }

  public boolean getStageReleaseStageType() {
    return stageReleaseStageType;
  }

  public void setStageReleaseStageType(boolean stageReleaseStageType) {
    this.stageReleaseStageType = stageReleaseStageType;
  }

  public boolean getReleaseStageType() {
    return releaseStageType;
  }

  public void setReleaseStageType(boolean releaseStageType) {
    this.releaseStageType = releaseStageType;
  }

  public boolean getOperateStageType() {
    return operateStageType;
  }

  public void setOperateStageType(boolean operateStageType) {
    this.operateStageType = operateStageType;
  }

  public boolean getProxyStageType() {
    return proxyStageType;
  }

  public void setProxyStageType(boolean proxyStageType) {
    this.proxyStageType = proxyStageType;
  }

  public void setStageTypeById(String stageTypeId, boolean value) {
    switch (stageTypeId) {
      case Stage.ID_DEVELOP:
        setDevelopStageType(value);
        break;
      case Stage.ID_BUILD:
        setBuildStageType(value);
        break;
      case Stage.ID_STAGE_RELEASE:
        setStageReleaseStageType(value);
        break;
      case Stage.ID_RELEASE:
        setReleaseStageType(value);
        break;
      case Stage.ID_OPERATE:
        setOperateStageType(value);
        break;
      case Stage.ID_PROXY:
        setProxyStageType(value);
        break;
      default:
        throw new IllegalArgumentException("Unknown stageType");
    }
  }

  public void setStageTypeById(String stageTypeId) {
    setStageTypeById(stageTypeId, true);
  }

  public boolean isClearedInAllStages() {
    return !(developStageType || buildStageType || stageReleaseStageType || releaseStageType || operateStageType
        || proxyStageType);
  }

  @Override
  public String getPolicyId() {
    return policyId;
  }

  public void setPolicyId(String policyId) {
    this.policyId = policyId;
  }

  @Override
  public String getPolicyName() {
    return policyName;
  }

  public void setPolicyName(String policyName) {
    this.policyName = policyName;
  }

  @Override
  public int getThreatLevel() {
    return threatLevel;
  }

  public void setThreatLevel(int threatLevel) {
    this.threatLevel = threatLevel;
  }

  public PolicyThreatCategory getThreatCategory() {
    return threatCategory;
  }

  public void setThreatCategory(final PolicyThreatCategory threatCategory) {
    this.threatCategory = threatCategory;
  }

  @Override
  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }
}
