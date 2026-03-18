/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;
import com.sonatype.insight.scan.model.ClientScanType;

/**
 * @since 1.11
 */
@Entity
@Table(name = "policy_evaluation")
public class PolicyEvaluation
    implements HasStringId
{
  @Id
  @Column(name = "policy_evaluation_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "stage_type_id")
  private String stageTypeId;

  @Column(name = "scan_id")
  private String scanId;

  // Whether the policy evaluation was for a scan that was already obsolete at the time the evaluation happened.
  @Column(name = "for_obsolete_scan")
  private boolean isForObsoleteScan;

  @Column(name = "reevaluation")
  private boolean isReevaluation;

  @Column(name = "for_monitoring")
  private boolean isForMonitoring;

  @Column(name = "time")
  private Date time;

  @Column(name = "commit_hash")
  private String commitHash;

  /**
   * @since 1.98
   */
  @Column(name = "initiator")
  private String initiator;

  /**
   * @since 1.105
   */
  @Column(name = "scan_trigger_type")
  @Enumerated(EnumType.STRING)
  private ScanTriggerType scanTriggerType;

  /**
   * @since 1.160.0
   *        Entities before the version 1.160 will have a null value, which indicates an unknown clientScanType.
   */
  @Column(name = "client_scan_type")
  @Enumerated(EnumType.STRING)
  private ClientScanType clientScanType;

  @Column(name = "branch_name")
  private String branchName;

  public PolicyEvaluation() {
  }

  public PolicyEvaluation(
      String applicationId,
      String stageTypeId,
      String scanId,
      String initiator,
      ScanTriggerType scanTriggerType)
  {
    this.applicationId = applicationId;
    this.stageTypeId = stageTypeId;
    this.scanId = scanId;
    this.initiator = initiator;
    this.scanTriggerType = scanTriggerType;
  }

  public PolicyEvaluation(
      String applicationId,
      String stageTypeId,
      String scanId,
      boolean isReevaluation,
      boolean isForMonitoring,
      String initiator,
      ScanTriggerType scanTriggerType,
      ClientScanType clientScanType)
  {
    this.applicationId = applicationId;
    this.stageTypeId = stageTypeId;
    this.scanId = scanId;
    this.isReevaluation = isReevaluation;
    this.isForMonitoring = isForMonitoring;
    this.initiator = initiator;
    this.scanTriggerType = scanTriggerType;
    this.clientScanType = clientScanType;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getScanId() {
    return scanId;
  }

  public void setScanId(String scanId) {
    this.scanId = scanId;
  }

  public boolean isReevaluation() {
    return isReevaluation;
  }

  public void setReevaluation(boolean isReevaluation) {
    this.isReevaluation = isReevaluation;
  }

  public boolean isForMonitoring() {
    return isForMonitoring;
  }

  public void setForMonitoring(boolean isForMonitoring) {
    this.isForMonitoring = isForMonitoring;
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

  public Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }

  public boolean isForObsoleteScan() {
    return isForObsoleteScan;
  }

  public void setForObsoleteScan(boolean isForObsoleteScan) {
    this.isForObsoleteScan = isForObsoleteScan;
  }

  public String getCommitHash() {
    return commitHash;
  }

  public void setCommitHash(final String commitHash) {
    this.commitHash = commitHash;
  }

  public String getInitiator() {
    return initiator;
  }

  public void setInitiator(final String initiator) {
    this.initiator = initiator;
  }

  public ScanTriggerType getScanTriggerType() {
    return scanTriggerType;
  }

  public void setScanTriggerType(ScanTriggerType scanTriggerType) {
    this.scanTriggerType = scanTriggerType;
  }

  public ClientScanType getClientScanType() {
    return clientScanType;
  }

  public void setClientScanType(ClientScanType clientScanType) {
    this.clientScanType = clientScanType;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public boolean wasInternallyTriggered() {
    return ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING == scanTriggerType
        || ScanTriggerType.SOURCE_CONTROL_INTERNAL_PULL_REQUEST == scanTriggerType
        || ScanTriggerType.SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING == scanTriggerType;
  }

  @Override
  public String toString() {
    return "PolicyEvaluation{" +
        "id='" + id + '\'' +
        ", applicationId='" + applicationId + '\'' +
        ", stageTypeId='" + stageTypeId + '\'' +
        ", scanId='" + scanId + '\'' +
        ", isForObsoleteScan=" + isForObsoleteScan +
        ", isReevaluation=" + isReevaluation +
        ", isForMonitoring=" + isForMonitoring +
        ", time=" + time +
        ", commitHash='" + commitHash + '\'' +
        ", initiator='" + initiator + '\'' +
        ", scanTriggerType=" + scanTriggerType +
        ", clientScanType=" + clientScanType +
        ", branchName='" + branchName + '\'' +
        '}';
  }
}
