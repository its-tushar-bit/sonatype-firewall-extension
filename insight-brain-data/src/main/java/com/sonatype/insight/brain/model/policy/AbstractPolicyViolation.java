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
import com.sonatype.insight.brain.utils.Sha1Util;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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

  @Column(name = "constraint_facts_id")
  private String constraintFactsId;

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
    // Lazy load constraint facts from PolicyViolationConstraintFacts table when needed
    if (constraintFactsJson == null && isNotBlank(constraintFactsId)) {
      constraintFactsJson = PolicyViolationConstraintFactsDAOProvider.getConstraintFactsJson(constraintFactsId);
    }
    return constraintFactsJson;
  }

  // This is to support the migration of the constraint facts from the JSON to an ID without triggering a read
  @Deprecated
  public String getConstraintFactsJsonWithoutLoading() {
    return constraintFactsJson;
  }

  public void setConstraintFactsJson(String constraintFactsJson) {
    if (StringUtils.isBlank(constraintFactsJson)) {
      throw new IllegalArgumentException("ConstraintFactsJson cannot be null or empty.");
    }

    this.constraintFactsJson = constraintFactsJson;
    constraintFacts = null;
  }

  /**
   * This method nulls the JSON so that it doesn't get written to the DB but retains the in memory constraintFacts
   * which can still be used to prevent unnecessary lazy loading and database round trips
   */
  public void clearConstraintFactsJson() {
    this.constraintFactsJson = null;
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
    // Short circuit to avoid a trip to the database to get the JSON if we already have the constraint facts
    if (constraintFacts != null) {
      return constraintFacts;
    }

    String json = getConstraintFactsJson();
    if (!StringUtils.isBlank(json)) {
      try {
        constraintFacts = Arrays.asList(JsonUtils.parse(json, ConstraintFact[].class));
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed to read constraint facts for policy violation " + getId(), e);
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

  public boolean isWaived() {
    return getWaiveTime() != null;
  }

  // to be overridden in subclasses which support legacy policy violation
  public boolean isLegacyViolation() {
    return false;
  }

  public abstract String getStageTypeId();

  public abstract Date getOpenTime();

  public abstract String getOwnerId();

  @Override
  public String getConstraintFactsId() {
    if (isBlank(constraintFactsId) && isNotBlank(constraintFactsJson)) {
      constraintFactsId = calculateConstraintFactsId(constraintFactsJson);
    }

    return constraintFactsId;
  }

  public static String calculateConstraintFactsId(String constraintFactsJson) {
    return Sha1Util.halfSha1(constraintFactsJson);
  }

  public void setConstraintFactsId(final String id) {
    this.constraintFactsId = id;
  }
}
