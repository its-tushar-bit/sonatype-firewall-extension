/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.12
 */
@Entity
@Table(name = "last_policy_evaluation")
public class LastPolicyEvaluation
    implements HasStringId
{
  @Id
  @Column(name = "policy_evaluation_id")
  private String policyEvaluationId;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "stage_type_id")
  private String stageTypeId;

  public LastPolicyEvaluation() {
  }

  public LastPolicyEvaluation(final String policyEvaluationId, final String ownerId, final String stageTypeId) {
    this.policyEvaluationId = policyEvaluationId;
    this.ownerId = ownerId;
    this.stageTypeId = stageTypeId;
  }

  @Override
  public String getId() {
    return policyEvaluationId;
  }

  @Override
  public void setId(String id) {
    policyEvaluationId = id;
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

  @Override
  public String toString() {
    return "LastPolicyEvaluation{" + "policyEvaluationId='" + policyEvaluationId + '\'' + ", ownerId='"
        + ownerId + '\'' + ", stageTypeId='" + stageTypeId + '\'' + '}';
  }
}
