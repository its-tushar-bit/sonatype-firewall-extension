/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

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

  @Column(name = "reevaluation")
  private boolean isReevaluation;

  @Column(name = "for_monitoring")
  private boolean isForMonitoring;

  @Column(name = "time")
  private Date time;

  public PolicyEvaluation() {
  }

  public PolicyEvaluation(String applicationId, String stageTypeId, String scanId) {
    this.applicationId = applicationId;
    this.stageTypeId = stageTypeId;
    this.scanId = scanId;
  }

  public PolicyEvaluation(String applicationId, String stageTypeId, String scanId, boolean isReevaluation,
      boolean isForMonitoring)
  {
    this.applicationId = applicationId;
    this.stageTypeId = stageTypeId;
    this.scanId = scanId;
    this.isReevaluation = isReevaluation;
    this.isForMonitoring = isForMonitoring;
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
}
