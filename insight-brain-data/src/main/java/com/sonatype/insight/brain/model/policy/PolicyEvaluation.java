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

import com.sonatype.clm.dto.model.ci.config.MetadataSource;
import com.sonatype.insight.model.HasStringId;
import com.sonatype.insight.scan.model.ClientScanType;

import static com.sonatype.nexus.git.utils.repository.RepositoryUrlFinderUtils.maskCredentialsFromUrl;

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

  @Column(name = "owner_id")
  private String ownerId;

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

  @Column(name = "scm_repository_url", length = 2048)
  private String scmRepositoryUrl;

  @Column(name = "commit_hash_source", length = 50)
  @Enumerated(EnumType.STRING)
  private MetadataSource commitHashSource;

  @Column(name = "branch_name_source", length = 50)
  @Enumerated(EnumType.STRING)
  private MetadataSource branchNameSource;

  @Column(name = "scm_repository_url_source", length = 50)
  @Enumerated(EnumType.STRING)
  private MetadataSource scmRepositoryUrlSource;

  public PolicyEvaluation() {
  }

  public PolicyEvaluation(
      String ownerId,
      String stageTypeId,
      String scanId,
      String initiator,
      ScanTriggerType scanTriggerType)
  {
    this.ownerId = ownerId;
    this.stageTypeId = stageTypeId;
    this.scanId = scanId;
    this.initiator = initiator;
    this.scanTriggerType = scanTriggerType;
  }

  public PolicyEvaluation(
      String ownerId,
      String stageTypeId,
      String scanId,
      boolean isReevaluation,
      boolean isForMonitoring,
      String initiator,
      ScanTriggerType scanTriggerType,
      ClientScanType clientScanType)
  {
    this.ownerId = ownerId;
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

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
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

  public String getScmRepositoryUrl() {
    return scmRepositoryUrl;
  }

  public void setScmRepositoryUrl(String scmRepositoryUrl) {
    this.scmRepositoryUrl = scmRepositoryUrl;
  }

  public MetadataSource getCommitHashSource() {
    return commitHashSource;
  }

  public void setCommitHashSource(MetadataSource commitHashSource) {
    this.commitHashSource = commitHashSource;
  }

  public MetadataSource getBranchNameSource() {
    return branchNameSource;
  }

  public void setBranchNameSource(MetadataSource branchNameSource) {
    this.branchNameSource = branchNameSource;
  }

  public MetadataSource getScmRepositoryUrlSource() {
    return scmRepositoryUrlSource;
  }

  public void setScmRepositoryUrlSource(MetadataSource scmRepositoryUrlSource) {
    this.scmRepositoryUrlSource = scmRepositoryUrlSource;
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
        ", ownerId='" + ownerId + '\'' +
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
        ", scmRepositoryUrl='" + (scmRepositoryUrl != null ? maskCredentialsFromUrl(scmRepositoryUrl) : null) + '\'' +
        ", commitHashSource=" + commitHashSource +
        ", branchNameSource=" + branchNameSource +
        ", scmRepositoryUrlSource=" + scmRepositoryUrlSource +
        '}';
  }
}
