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
 * @since 1.12
 */
@Entity
@Table(name = "waived_policy_violation")
public class WaivedPolicyViolation
    implements HasStringId
{
  @Id
  @Column(name = "policy_violation_id")
  private String id;

  @Column(name = "policy_waiver_id")
  private String policyWaiverId;

  @Column(name = "comment")
  private String comment;

  public WaivedPolicyViolation() {
  }

  public WaivedPolicyViolation(String policyViolationId, String policyWaiverId, String comment) {
    this.id = policyViolationId;
    this.policyWaiverId = policyWaiverId;
    this.comment = comment;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getPolicyWaiverId() {
    return policyWaiverId;
  }

  public void setPolicyWaiverId(String policyWaiverId) {
    this.policyWaiverId = policyWaiverId;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}
