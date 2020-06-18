/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.6
 */
@Entity
@Table(name = "policy_waiver")
public class PolicyWaiver
    implements HasStringId
{
  @Id
  @Column(name = "policy_waiver_id")
  private String id;

  @Column(name = "hash")
  private String hash;

  @Column(name = "policy_id")
  private String policyId;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "comment")
  private String comment;

  @Column(name = "create_time")
  private Date createTime;

  /**
   * @since 1.53
   */
  @Column(name = "constraint_facts_json")
  private String constraintFactsJson;

  @Transient
  private List<ConstraintFact> constraintFacts;

  public PolicyWaiver() {
  }

  public PolicyWaiver(String policyId, String ownerId, String comment) {
    this.policyId = policyId;
    this.ownerId = ownerId;
    this.comment = comment;
  }

  public PolicyWaiver(String hash, String policyId, String ownerId, String comment) {
    this(policyId, ownerId, comment);
    setHash(hash);
  }

  public PolicyWaiver(String hash,
                      String policyId,
                      String ownerId,
                      List<ConstraintFact> constraintFacts,
                      String comment)
  {
    this(policyId, ownerId, comment);
    setHash(hash);
    setConstraintFacts(constraintFacts);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = HashHelper.truncateHash(hash);
  }

  public String getPolicyId() {
    return policyId;
  }

  public void setPolicyId(String policyId) {
    this.policyId = policyId;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getConstraintFactsJson() {
    return constraintFactsJson;
  }

  public void setConstraintFactsJson(String constraintFactsJson) {
    if (StringUtils.isBlank(constraintFactsJson)) {
      constraintFactsJson = null;
    }
    this.constraintFactsJson = constraintFactsJson;
    constraintFacts = null;
  }

  public void setConstraintFacts(List<ConstraintFact> constraintFacts) {
    if (constraintFacts == null || constraintFacts.isEmpty()) {
      this.constraintFacts = null;
      constraintFactsJson = null;
    }
    else {
      this.constraintFacts = constraintFacts;
      constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);
    }
  }

  public List<ConstraintFact> getConstraintFacts() {
    if (constraintFacts == null && !StringUtils.isBlank(constraintFactsJson)) {
      try {
        constraintFacts = Arrays.asList(JsonUtils.parse(constraintFactsJson, ConstraintFact[].class));
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed to read constraint facts for policy waiver " + id, e);
      }
    }
    return constraintFacts;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }
}
