/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

import org.apache.openjpa.persistence.DataCache;

/**
 * This entity is immutable so it is safe to cache it and caching reduces the need for database round trips and means
 * the constraint facts json can be lazy loaded by the relating entities that require it.
 */
@DataCache(timeout = 10000)
@Cacheable
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
