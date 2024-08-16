/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.insight.model.HasStringId;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @since 1.181
 */
@Entity
@Table(name = "policy_waiver_reason")
public class PolicyWaiverReason
    implements HasStringId
{
  @Id
  @Column(name = "waiver_reason_id")
  private String id;

  @Column(name = "type")
  private String type;

  @Column(name = "reason_text")
  private String reasonText;

  public PolicyWaiverReason() {
  }

  public PolicyWaiverReason(String reasonText) {
    this.reasonText = reasonText;
  }

  public PolicyWaiverReason(String type, String reasonText) {
    this.type = type;
    this.reasonText = reasonText;
  }

  public PolicyWaiverReason(String id, String type, String reasonText) {
    this.id = id;
    this.type = type;
    this.reasonText = reasonText;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getReasonText() {
    return reasonText;
  }

  public void setReasonText(String reasonText) {
    this.reasonText = reasonText;
  }
}
