/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "persisted_policy_evaluation_polling_result")
public class PersistedPolicyEvaluationPollingResult
    implements HasStringId
{
  @Id
  @Column(name = "persisted_policy_evaluation_polling_result_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "status_id")
  private String statusId;

  @Column(name = "policy_evaluation_polling_result_json")
  private String policyEvaluationPollingResultJson;

  @Column(name = "create_time")
  private Date createTime;

  public PersistedPolicyEvaluationPollingResult() {
  }

  public PersistedPolicyEvaluationPollingResult(
      String applicationId,
      String statusId,
      PolicyEvaluationPollingResult policyEvaluationPollingResult)
  {
    this.applicationId = applicationId;
    this.statusId = statusId;
    setPolicyEvaluationPollingResult(policyEvaluationPollingResult);
    this.createTime = new Date();
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

  public String getStatusId() {
    return statusId;
  }

  public void setStatusId(String statusId) {
    this.statusId = statusId;
  }

  public PolicyEvaluationPollingResult getPolicyEvaluationPollingResult() {
    try {
      return JsonUtils.parse(policyEvaluationPollingResultJson, PolicyEvaluationPollingResult.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void setPolicyEvaluationPollingResult(PolicyEvaluationPollingResult policyEvaluationPollingResult) {
    policyEvaluationPollingResultJson = JsonUtils.writeUnformatted(policyEvaluationPollingResult);
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }
}
