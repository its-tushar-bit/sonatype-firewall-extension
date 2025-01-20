/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.ide;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * SDEV-228
 * This table records the last timestamp of a user using an IDE plugin to initiate a policy evaluation.
 *
 * @since 1.162.0
 */
@Entity
@Table(name = "user_ide_policy_evaluation")
public class UserIdePolicyEvaluation
    implements HasStringId
{
  @Id
  @Column(name = "user_ide_policy_evaluation_id")
  private String id;

  @Column(name = "username")
  private String username;

  @Column(name = "last_evaluation_time")
  private Date lastEvaluationTime;

  public UserIdePolicyEvaluation() {
  }

  public UserIdePolicyEvaluation(String username, Date lastEvaluationTime) {
    this.username = username;
    this.lastEvaluationTime = lastEvaluationTime;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Date getLastEvaluationTime() {
    return lastEvaluationTime;
  }

  public void setLastEvaluationTime(Date lastEvaluationTime) {
    this.lastEvaluationTime = lastEvaluationTime;
  }

  @Override
  public String toString() {
    return "UserIdePolicyEvaluation [username=" + username + ", lastEvaluationTime="
        + lastEvaluationTime + "]";
  }
}
