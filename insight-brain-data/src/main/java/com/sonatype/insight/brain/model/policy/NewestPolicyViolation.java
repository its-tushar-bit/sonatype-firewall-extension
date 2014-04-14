/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * The first occurrence (in time) of a policy violation which still exists, in the scope of an application.
 * 
 * @since 1.11
 */
@Entity
@Table(name = "newest_policy_violation")
public class NewestPolicyViolation
    implements HasStringId
{
  @Id
  @Column(name = "policy_violation_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "stage_type_id")
  private String stageTypeId;

  public NewestPolicyViolation() {
  }

  public NewestPolicyViolation(String policyViolationId, String applicationId, String stageTypeId) {
    id = policyViolationId;
    this.applicationId = applicationId;
    this.stageTypeId = stageTypeId;
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
}
