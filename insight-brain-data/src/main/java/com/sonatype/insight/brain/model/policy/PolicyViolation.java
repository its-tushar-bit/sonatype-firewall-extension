/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import com.google.common.base.Joiner;
import org.codehaus.plexus.util.StringUtils;

/**
 * @since 1.11
 */
@Entity
@Table(name = "policy_violation")
public class PolicyViolation
  extends HasComponentId
  implements HasStringId
{
  static final char NOTIFICATIONS_DELIMITER_CHAR = '\n';

  /** The notifications delimiter character escaped for regular expressions. */
  static final String NOTIFICATIONS_DELIMITER_REGEX = "\\" + NOTIFICATIONS_DELIMITER_CHAR;

  static final char PATHNAMES_DELIMITER_CHAR = '\n';

  /** The pathnames delimiter character escaped for regular expressions. */
  static final String PATHNAMES_DELIMITER_REGEX = "\\" + PATHNAMES_DELIMITER_CHAR;

  @Id
  @Column(name = "policy_violation_id")
  private String id;

  @Column(name = "policy_evaluation_id")
  private String policyEvaluationId;

  @Column(name = "time")
  private Date time;

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

  @Column(name = "pathnames")
  private String pathnamesString;

  @Column(name = "action_type_id")
  private String actionTypeId;

  @Column(name = "notifications")
  private String notificationsString;

  /**
   * @since 1.12
   */
  @Column(name = "waived")
  private boolean isWaived;

  @Transient
  private List<ConstraintFact> constraintFacts;

  @Transient
  private List<String> pathnames;

  @Transient
  private List<String> notifications;

  public PolicyViolation() {
  }

  public PolicyViolation(PolicyEvaluation evaluation, String policyId, String policyName, int threatLevel,
      PolicyThreatCategory threatCategory, String hash, ComponentIdentifier componentIdentifier,
      String constraintFactsJson, String pathnames)
  {
    this.policyEvaluationId = evaluation.getId();
    this.time = evaluation.getTime();
    this.policyId = policyId;
    this.policyName = policyName;
    this.threatLevel = threatLevel;
    this.threatCategory = threatCategory;
    this.hash = hash;
    setComponentIdentifier(componentIdentifier);
    setConstraintFactsJson(constraintFactsJson);
    setPathnamesString(pathnames);
  }

  public PolicyViolation(PolicyEvaluation evaluation, Policy policy, String hash,
      ComponentIdentifier componentIdentifier, List<ConstraintFact> constraintFacts, List<String> pathnames)
  {
    this(evaluation, policy.getId(), policy.getName(), policy.getThreatLevel(), policy.getThreatCategory(), hash,
        componentIdentifier, constraintFacts, pathnames);
  }

  public PolicyViolation(PolicyEvaluation evaluation, String policyId, String policyName, int threatLevel,
      PolicyThreatCategory threatCategory, String hash, ComponentIdentifier componentIdentifier,
      List<ConstraintFact> constraintFacts, List<String> pathnames)
  {
    this.policyEvaluationId = evaluation.getId();
    this.time = evaluation.getTime();
    this.policyId = policyId;
    this.policyName = policyName;
    this.threatLevel = threatLevel;
    this.threatCategory = threatCategory;
    this.hash = hash;
    setComponentIdentifier(componentIdentifier);
    setConstraintFacts(constraintFacts);
    setPathnames(pathnames);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getPolicyEvaluationId() {
    return policyEvaluationId;
  }

  public void setPolicyEvaluationId(String policyEvaluationId) {
    this.policyEvaluationId = policyEvaluationId;
  }

  public String getPolicyId() {
    return policyId;
  }

  public void setPolicyId(String policyId) {
    this.policyId = policyId;
  }

  public String getPolicyName() {
    return policyName;
  }

  public void setPolicyName(String policyName) {
    this.policyName = policyName;
  }

  public int getThreatLevel() {
    return threatLevel;
  }

  public void setThreatLevel(int threatLevel) {
    this.threatLevel = threatLevel;
  }

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

  String getPathnamesString() {
    return this.pathnamesString;
  }

  private void setPathnamesString(String pathnames) {
    this.pathnamesString = StringUtils.isBlank(pathnames) ? null : pathnames;
    this.pathnames = null;
  }

  public void setPathnames(List<String> pathnames) {
    if (pathnames == null || pathnames.isEmpty()) {
      // If the path names are null we want to persist a null value.
      this.pathnames = null;
      this.pathnamesString = null;
      return;
    }

    this.pathnames = pathnames;

    pathnamesString = Joiner.on(PATHNAMES_DELIMITER_CHAR).skipNulls().join(this.pathnames);
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
    constraintFactsJson = JsonUtils.format(constraintFacts);
  }

  public List<ConstraintFact> getConstraintFacts() {
    if (constraintFacts == null && !StringUtils.isEmpty(constraintFactsJson)) {
      try {
        constraintFacts = Arrays.asList(JsonUtils.parse(constraintFactsJson, ConstraintFact[].class));
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return constraintFacts;
  }

  public List<String> getPathnames() {
    if (pathnames == null && !StringUtils.isBlank(pathnamesString)) {
      pathnames = Arrays.asList(pathnamesString.split(PATHNAMES_DELIMITER_REGEX));
    }

    return pathnames;
  }

  public Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }

  public String getActionTypeId() {
    return actionTypeId;
  }

  public void setActionTypeId(String actionTypeId) {
    this.actionTypeId = actionTypeId;
  }

  public String getNotificationsString() {
    return notificationsString;
  }

  @SuppressWarnings("unused")
  /**
   * Only used by JPA.
   */
  private void setNotificationsString(String notificationsString) {
    this.notificationsString = notificationsString;
    notifications = null;
  }

  public void setNotifications(List<String> notifications) {
    if (notifications == null || notifications.isEmpty()) {
      this.notifications = Collections.emptyList();
      notificationsString = null;
      return;
    }

    this.notifications = notifications;
    notificationsString = Joiner.on(NOTIFICATIONS_DELIMITER_CHAR).skipNulls().join(notifications);
  }

  public List<String> getNotifications() {
    if (notifications == null) {
      if (!StringUtils.isBlank(notificationsString)) {
        notifications = Arrays.asList(notificationsString.split(NOTIFICATIONS_DELIMITER_REGEX));
      }
      else {
        notifications = Collections.emptyList();
      }
    }

    return notifications;
  }

  @Override
  public String toString() {
    return "PolicyViolation [id=" + id + ", policyEvaluationId=" + policyEvaluationId + ", time=" + time + "("
        + time.getTime() + "), policyId=" + policyId + ", policyName=" + policyName + ", threatLevel=" + threatLevel
        + ", threatCategory=" + threatCategory + ", hash=" + hash + ", componentIdentifier=" + getComponentIdentifier()
        + ", actionTypeId=" + actionTypeId + "]";
  }

  public boolean isWaived() {
    return isWaived;
  }

  public void setWaived(boolean isWaived) {
    if (this.isWaived && !isWaived) {
      throw new IllegalStateException("Cannot un-waive a policy violation.");
    }
    this.isWaived = isWaived;
  }
}
