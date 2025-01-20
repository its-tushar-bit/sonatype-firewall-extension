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
 * @since 1.107
 */
@Entity
@Table(name = "auto_unquarantine_policy_condition_type")
public class AutoUnquarantinePolicyConditionType
    implements HasStringId
{
  @Id
  @Column(name = "condition_type_id")
  private String id;

  public AutoUnquarantinePolicyConditionType() {
  }

  public AutoUnquarantinePolicyConditionType(final String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }
}
