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

@Entity
@Table(name = "policy_violation_constraint_facts")
public class PolicyViolationConstraintFacts
    implements HasStringId
{
  @Id
  @Column(name = "policy_violation_constraint_facts_id")
  private String id;

  @Column(name = "constraint_facts_json")
  private String constraintFactsJson;

  public PolicyViolationConstraintFacts() {
  }

  public PolicyViolationConstraintFacts(final String sha1Hash, final String constraintFactsJson) {
    this.id = sha1Hash;
    this.constraintFactsJson = constraintFactsJson;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getConstraintFactsJson() {
    return constraintFactsJson;
  }

  public void setConstraintFactsJson(final String constraintFactsJson) {
    this.constraintFactsJson = constraintFactsJson;
  }
}
