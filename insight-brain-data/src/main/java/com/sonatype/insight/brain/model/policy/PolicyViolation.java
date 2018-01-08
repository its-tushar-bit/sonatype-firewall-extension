/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.io.File;
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

import com.google.common.annotations.VisibleForTesting;
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

  static final char NOTIFICATIONS_DELIMITER_CHAR = '\n';

  /** The notifications delimiter character escaped for regular expressions. */
  static final String NOTIFICATIONS_DELIMITER_REGEX = "\\" + NOTIFICATIONS_DELIMITER_CHAR;

  @Id
  @Column(name = "policy_violation_id")
  private String id;

  @Column(name = "policy_evaluation_id")
  private String policyEvaluationId;

  @Column(name = "pathnames")
  private String pathnames;

  @Column(name = "notifications")
  private String notificationsString;

  @Transient
  private List<String> notifications;
  
  @Transient
  private String filename;

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
                         String filename)
  {
    super(evaluation.getTime(), policyId, policyName, threatLevel, threatCategory, hash, componentIdentifier,
        constraintFactsJson);
    this.policyEvaluationId = evaluation.getId();
    this.pathnames = filename;
  }

  public PolicyViolation(PolicyEvaluation evaluation,
                         Policy policy,
                         String hash,
                         ComponentIdentifier componentIdentifier,
                         List<ConstraintFact> constraintFacts,
                         String filename)
  {
    this(evaluation, policy.getId(), policy.getName(), policy.getThreatLevel(), policy.getThreatCategory(), hash,
        componentIdentifier, constraintFacts, filename);
  }

  public PolicyViolation(PolicyEvaluation evaluation,
                         String policyId,
                         String policyName,
                         int threatLevel,
                         PolicyThreatCategory threatCategory,
                         String hash,
                         ComponentIdentifier componentIdentifier,
                         List<ConstraintFact> constraintFacts,
                         String filename)
  {
    super(evaluation.getTime(), policyId, policyName, threatLevel, threatCategory, hash, componentIdentifier,
        constraintFacts);
    this.policyEvaluationId = evaluation.getId();
    this.pathnames = filename;
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
  
  public String getFilename() {
    // Return the first pathname's filename, unless pathnames/first pathname is blank, then return null
    if (filename != null) {
      return filename;
    }
    if (StringUtils.isBlank(pathnames)) {
      return null;
    }
    String firstPath = pathnames.trim().split(PATHNAMES_DELIMITER_REGEX)[0];
    if (StringUtils.isBlank(firstPath)) {
      return null;
    }
    filename = new File(firstPath).getName();
    return filename;
  }

  @VisibleForTesting
  void setPathnames(String pathnames) {
    filename = null;
    this.pathnames = pathnames;
  }

  public void setPathnames(List<String> pathnames) {
    if (pathnames == null || pathnames.isEmpty()) {
      // If the path names are null or empty we want to persist a null value.
      setPathnames((String) null);
    }
    else {
      setPathnames(Joiner.on(PATHNAMES_DELIMITER_CHAR).skipNulls().join(pathnames));
    }
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
