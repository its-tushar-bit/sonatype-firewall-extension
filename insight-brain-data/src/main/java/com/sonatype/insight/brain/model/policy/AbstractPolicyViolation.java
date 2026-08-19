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

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.brain.utils.Sha1Util;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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

  /**
   * The policy violation constraint facts used to be stored in the same db table, as json string, in the
   * constraint_facts_json column and mapped to the constraintFactsJson field in this class, and they were loaded
   * automatically from constraintFactsJson.
   * That was changed to store them in a separate table policy_violation_constraint_facts and linked here by their ID,
   * in order to reduce the db size and the memory consumption.
   * After this change, the old constraint_facts_json db column was not removed because it is used by the data migrator.
   * This column is now mapped to the deprecatedConstraintFactsJson field, which should only be used by the migrator.
   * The constraint facts are not loaded automatically anymore. This saves memory everywhere where policy violations are
   * used and the constraint facts are not needed.
   * Any code that needs the constraint facts must call the (Repository)PolicyViolationDAO.loadConstraintFacts() method
   * explicitly.
   */
  @Column(name = "constraint_facts_id")
  private String constraintFactsId;

  @Transient
  private List<ConstraintFact> constraintFacts;

  /**
   * To be used only by the AbstractPolicyViolationConstraintFactsJsonAsyncDbMigration class and its tests.
   */
  @Column(name = "constraint_facts_json")
  private String deprecatedConstraintFactsJson;

  @Column(name = "action_type_id")
  private String actionTypeId;

  /**
   * New for repo policy violations
   *
   * @since 1.76
   */
  @Column(name = "policy_waiver_id")
  private String policyWaiverId;

  @Column(name = "policy_waiver_comment")
  private String policyWaiverComment;

  @Column(name = "waive_time")
  private Date waiveTime;

  public AbstractPolicyViolation() {
  }

  protected AbstractPolicyViolation(
      String policyId,
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
    return JsonUtils.writeUnformatted(getConstraintFacts());
  }

  /**
   * To be used only for tests
   */
  public void clearConstraintFacts() {
    this.constraintFacts = null;
  }

  public void setConstraintFacts(List<ConstraintFact> constraintFacts) {
    if (CollectionUtils.isEmpty(constraintFacts)) {
      throw new IllegalArgumentException("ConstraintFacts cannot be null or empty.");
    }

    this.constraintFacts = constraintFacts;
  }

  @Override
  public List<ConstraintFact> getConstraintFacts() {
    if (constraintFacts != null) {
      return constraintFacts;
    }

    // Since (Repository)PolicyViolationConstraintFactsJsonAsyncDbMigration runs async, there is a time window where the
    // system has to be able to use migrated and unmigrated policy violations.
    String json = deprecatedConstraintFactsJson;
    if (!StringUtils.isBlank(json)) {
      try {
        constraintFacts = Arrays.asList(JsonUtils.parse(json, ConstraintFact[].class));
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed to read constraint facts for policy violation " + getId(), e);
      }
    }

    if (constraintFacts != null) {
      return constraintFacts;
    }

    throw new IllegalStateException("Constraint facts are not loaded yet for policyViolationId=" + getId()
        + ". Use the " + getClass().getSimpleName()
        + "DAO.loadConstraintFacts() method to load the constraint facts explicitly.");
  }

  public boolean constraintFactsAreLoaded() {
    return !CollectionUtils.isEmpty(constraintFacts);
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
    return constraintFactsId;
  }

  public void setConstraintFactsId(final String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("ConstraintFactsId cannot be null or empty.");
    }

    this.constraintFactsId = id;
  }

  public static String calculateConstraintFactsId(String constraintFactsJson) {
    return Sha1Util.halfSha1(constraintFactsJson);
  }

  /**
   * To be used only by the AbstractPolicyViolationConstraintFactsJsonAsyncDbMigration class and its tests.
   */
  public String getDeprecatedConstraintFactsJson() {
    return deprecatedConstraintFactsJson;
  }

  /**
   * To be used only by the AbstractPolicyViolationConstraintFactsJsonAsyncDbMigration class and its tests.
   */
  public void setDeprecatedConstraintFactsJson(String deprecatedConstraintFactsJson) {
    this.deprecatedConstraintFactsJson = deprecatedConstraintFactsJson;
  }
}
