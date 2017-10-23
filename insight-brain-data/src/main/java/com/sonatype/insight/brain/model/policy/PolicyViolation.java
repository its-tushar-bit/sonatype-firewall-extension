/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.model.HasStringId;

import com.google.common.base.Joiner;
import org.codehaus.plexus.util.StringUtils;

/**
 * @since 1.11
 */
@Entity
@Table(name = "policy_violation")
public class PolicyViolation
    extends AbstractPolicyViolation
    implements HasStringId
{
  static final char PATHNAMES_DELIMITER_CHAR = '\n';

  /** The pathnames delimiter character escaped for regular expressions. */
  static final String PATHNAMES_DELIMITER_REGEX = "\\" + PATHNAMES_DELIMITER_CHAR;

  @Id
  @Column(name = "policy_violation_id")
  private String id;

  @Column(name = "policy_evaluation_id")
  private String policyEvaluationId;

  @Column(name = "pathnames")
  private String pathnamesString;

  @Transient
  private List<String> pathnames;

  @Column(name = "notifications")
  private String notificationsString;

  @Transient
  private List<String> notifications;

  public PolicyViolation() {
  }

  public PolicyViolation(PolicyEvaluation evaluation,
                         String policyId,
                         String policyName,
                         int threatLevel,
                         PolicyThreatCategory threatCategory,
                         String hash,
                         ComponentIdentifier componentIdentifier,
                         String constraintFactsJson,
                         String pathnames)
  {
    super(evaluation.getTime(), policyId, policyName, threatLevel, threatCategory, hash, componentIdentifier,
        constraintFactsJson);
    this.policyEvaluationId = evaluation.getId();
    setPathnamesString(pathnames);
  }

  public PolicyViolation(PolicyEvaluation evaluation,
                         Policy policy,
                         String hash,
                         ComponentIdentifier componentIdentifier,
                         List<ConstraintFact> constraintFacts,
                         List<String> pathnames)
  {
    this(evaluation, policy.getId(), policy.getName(), policy.getThreatLevel(), policy.getThreatCategory(), hash,
        componentIdentifier, constraintFacts, pathnames);
  }

  public PolicyViolation(PolicyEvaluation evaluation,
                         String policyId,
                         String policyName,
                         int threatLevel,
                         PolicyThreatCategory threatCategory,
                         String hash,
                         ComponentIdentifier componentIdentifier,
                         List<ConstraintFact> constraintFacts,
                         List<String> pathnames)
  {
    super(evaluation.getTime(), policyId, policyName, threatLevel, threatCategory, hash, componentIdentifier,
        constraintFacts);
    this.policyEvaluationId = evaluation.getId();
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

  public List<String> getPathnames() {
    if (pathnames == null && !StringUtils.isBlank(pathnamesString)) {
      pathnames = Arrays.asList(pathnamesString.split(PATHNAMES_DELIMITER_REGEX));
    }

    return pathnames;
  }

  public String getNotificationsString() {
    return notificationsString;
  }

  /**
   * Only used by JPA.
   */
  @SuppressWarnings("unused")
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
    return "PolicyViolation [id=" + id + ", policyEvaluationId=" + policyEvaluationId + ", time=" + getTime() + "("
        + getTime().getTime() + "), policyId=" + getPolicyId() + ", policyName=" + getPolicyName() + ", threatLevel="
        + getThreatLevel() + ", threatCategory=" + getThreatCategory() + ", hash=" + getHash()
        + ", componentIdentifier=" + getComponentIdentifier() + ", actionTypeId=" + getActionTypeId() + "]";
  }
}
