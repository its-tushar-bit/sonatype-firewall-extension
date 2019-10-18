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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import org.codehaus.plexus.util.StringUtils;

/**
 * @since 1.17
 */
@MappedSuperclass
public abstract class AbstractPolicyViolation
    extends HasComponentId
    implements PolicyViolationComparable, HasStringId
{
  @Column(name = "policy_id")
  private String policyId;

  @Column(name = "policy_name")
  private String policyName;

  @Column(name = "threat_level")
  private int threatLevel;

  @Column(name = "threat_category")
  @Enumerated(EnumType.STRING)
  private PolicyThreatCategory threatCategory;

  @Column(name = "hash")
  private String hash;

  @Column(name = "constraint_facts_json")
  private String constraintFactsJson;

  @Column(name = "action_type_id")
  private String actionTypeId;

  /**
   * New for repo policy violations
   * @since 1.76
   */
  @Column(name = "policy_waiver_id")
  private String policyWaiverId;

  @Column(name = "policy_waiver_comment")
  private String policyWaiverComment;

  @Column(name = "waive_time")
  private Date waiveTime;

  @Transient
  private List<ConstraintFact> constraintFacts;

  public AbstractPolicyViolation() {
  }

  protected AbstractPolicyViolation(String policyId,
                                    String policyName,
                                    int threatLevel,
                                    PolicyThreatCategory threatCategory,
                                    String hash,
                                    ComponentIdentifier componentIdentifier,
                                    String constraintFactsJson)
  {
    this.policyId = policyId;
    this.policyName = policyName;
    this.threatLevel = threatLevel;
    this.threatCategory = threatCategory;
    this.hash = hash;
    setComponentIdentifier(componentIdentifier);
    setConstraintFactsJson(constraintFactsJson);
  }

  protected AbstractPolicyViolation(String policyId,
                                    String policyName,
                                    int threatLevel,
                                    PolicyThreatCategory threatCategory,
                                    String hash,
                                    ComponentIdentifier componentIdentifier,
                                    List<ConstraintFact> constraintFacts)
  {
    this.policyId = policyId;
    this.policyName = policyName;
    this.threatLevel = threatLevel;
    this.threatCategory = threatCategory;
    this.hash = hash;
    setComponentIdentifier(componentIdentifier);
    setConstraintFacts(constraintFacts);
  }

  @Override
  public String getPolicyId() {
    return policyId;
  }

  public void setPolicyId(String policyId) {
    this.policyId = policyId;
  }

  @Override
  public String getPolicyName() {
    return policyName;
  }

  public void setPolicyName(String policyName) {
    this.policyName = policyName;
  }

  @Override
  public int getThreatLevel() {
    return threatLevel;
  }

  public void setThreatLevel(int threatLevel) {
    this.threatLevel = threatLevel;
  }

  @Override
  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public PolicyThreatCategory getThreatCategory() {
    return threatCategory;
  }

  public void setThreatCategory(PolicyThreatCategory threatCategory) {
    this.threatCategory = threatCategory;
  }

  public String getConstraintFactsJson() {
    return constraintFactsJson;
  }

  public void setConstraintFactsJson(String constraintFactsJson) {
    if (StringUtils.isEmpty(constraintFactsJson)) {
      throw new IllegalArgumentException("ConstraintFactsJson cannot be null or empty.");
    }
    this.constraintFactsJson = constraintFactsJson;
    constraintFacts = null;
  }

  public void setConstraintFacts(List<ConstraintFact> constraintFacts) {
    if (constraintFacts == null || constraintFacts.isEmpty()) {
      throw new IllegalArgumentException("ConstraintFacts cannot be null or empty.");
    }

    this.constraintFacts = constraintFacts;
    constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);
  }

  @Override
  public List<ConstraintFact> getConstraintFacts() {
    if (constraintFacts == null && !StringUtils.isEmpty(constraintFactsJson)) {
      try {
        constraintFacts = Arrays.asList(JsonUtils.parse(constraintFactsJson, ConstraintFact[].class));
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return constraintFacts;
  }

  public String getActionTypeId() {
    return actionTypeId;
  }

  public void setActionTypeId(String actionTypeId) {
    this.actionTypeId = actionTypeId;
  }

  public String getPolicyWaiverId() {
    return policyWaiverId;
  }

  public void setPolicyWaiverId(String policyWaiverId) {
    this.policyWaiverId = policyWaiverId;
  }

  public String getPolicyWaiverComment() {
    return policyWaiverComment;
  }

  public void setPolicyWaiverComment(String policyWaiverComment) {
    this.policyWaiverComment = policyWaiverComment;
  }

  public Date getWaiveTime() {
    return waiveTime;
  }

  public void setWaiveTime(Date waiveTime) {
    if (this.waiveTime != null && waiveTime == null) {
      throw new IllegalStateException("Cannot un-waive a policy violation.");
    }
    this.waiveTime = waiveTime;
  }
}
