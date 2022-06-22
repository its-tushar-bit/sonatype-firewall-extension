/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Policy
{
  private static final Logger log = LoggerFactory.getLogger(Policy.class);

  private String id;

  private String name;

  /**
   * @since 1.6
   */
  private String ownerId;

  private int threatLevel = 5;

  /**
   * @since 1.50
   */
  private boolean policyViolationGrandfatheringAllowed;

  private List<Constraint> constraints;

  private Map<String, String> actions = new HashMap<>();

  private Notifications notifications = new Notifications();

  private String droolsCode;

  private boolean policyActionsOverrideAllowed;

  private Map<String, Map<String, String>> policyActionsOverrides;

  public Policy() {
  }

  public Policy(final String id, final String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public int getThreatLevel() {
    return threatLevel;
  }

  public void setThreatLevel(final int threatLevel) {
    this.threatLevel = threatLevel;
  }

  public boolean isPolicyViolationGrandfatheringAllowed() {
    return policyViolationGrandfatheringAllowed;
  }

  public void setPolicyViolationGrandfatheringAllowed(boolean policyViolationGrandfatheringAllowed) {
    this.policyViolationGrandfatheringAllowed = policyViolationGrandfatheringAllowed;
  }

  public List<Constraint> getConstraints() {
    return constraints;
  }

  public void setConstraints(final List<Constraint> constraints) {
    this.constraints = constraints;
  }

  public void addConstraint(final Constraint constraint) {
    if (constraints == null) {
      constraints = new ArrayList<>();
    }
    constraints.add(constraint);
  }

  public Map<String, String> getActions() {
    return actions;
  }

  public void setActions(final Map<String, String> actions) {
    this.actions = actions != null ? actions : new HashMap<>();
  }

  public void setAction(String stageId, String actionId) {
    this.actions.put(stageId, actionId);
  }

  public Notifications getNotifications() {
    return notifications;
  }

  public void setNotifications(Notifications notifications) {
    this.notifications = notifications != null ? notifications : new Notifications();
  }

  /**
   *
   * @param stageId the id of the stage to retrieve actions for
   * @param continuousMonitoring is this for continuous monitoring
   * @param ownerIds optional, sorted list of ids of all the owners up in the hierarchy starting from the application
   *                 and ending with root org. This is used for actions overrides.
   * @return list of Action objects
   */
  public List<Action> toActions(String stageId, boolean continuousMonitoring, List<String> ownerIds) {
    List<Action> result = new ArrayList<>();

    if (!continuousMonitoring) {
      // walk the hierarchy of owners to retrieve actionOverrides for the nearest parent
      Map<String, String> actionOverrides = getActionOverrides(ownerIds);

      Map<String, String> effectiveActions = actionOverrides != null ? actionOverrides : actions;
      String actionId = effectiveActions.get(stageId);
      if (actionId != null) {
        result.add(new Action(actionId));
      }
    }
    result.addAll(notifications.getApplicable(stageId, continuousMonitoring).toActions());
    return result;
  }

  private Map<String, String> getActionOverrides(final List<String> ownerIds) {
    if (!isOverrideApplicable(ownerIds) || policyActionsOverrides == null || policyActionsOverrides.isEmpty()) {
      return null;
    }

    for (int i = 0; i < ownerIds.size() - 1; i++) {
      Map<String, String> actionOverrides = policyActionsOverrides.get(ownerIds.get(i));
      // walk through the hierarchy stops in front of the policy owner
      if (actionOverrides != null || ownerIds.get(i + 1).equals(this.getOwnerId())) {
        return actionOverrides;
      }
    }

    return null;
  }

  private boolean isOverrideApplicable(final List<String> ownerIds) {
    return policyActionsOverrideAllowed && ownerIds != null;
  }

  public ValidationResult validate(TransactionContext tx, String ownerId) {
    log.debug("Validating {}", this);

    ValidationResult result = new ValidationResult();
    try {
      NameHelper.validate("The policy name", name);
    }
    catch (InvalidNameException e) {
      result.addError(e.getMessage());
    }
    if (constraints == null || constraints.isEmpty()) {
      result.addError("Policy '" + name + "' has no constraints");
    }
    else {
      ValidationResult constraintResult = new ValidationResult();
      Set<String> constraintNames = new LinkedHashSet<>();
      for (Constraint constraint : constraints) {
        String constraintName = constraint.getName();
        if (constraintName != null && !constraintName.trim().isEmpty()) {
          if (constraintNames.contains(constraintName)) {
            constraintResult.addError("Duplicate constraint name '" + constraintName + "'");
          }
          else {
            constraintNames.add(constraintName);
          }
        }
        constraintResult.merge(constraint.validate(tx, ownerId));
      }
      if (!constraintResult.isValid()) {
        result.addError("Policy '" + name + "' has invalid constraints:");
        result.merge(constraintResult);
      }
    }

    ValidationResult actionResult = new ValidationResult();
    for (String stageId : actions.keySet()) {
      if (StageTypes.getById(stageId) == null) {
        actionResult.addError("Invalid stage: '" + stageId + "'");
      }

      String actionId = actions.get(stageId);
      if (!Action.ID_FAIL.equals(actionId) && !Action.ID_WARN.equals(actionId)) {
        actionResult.addError("Invalid action for stage '" + stageId + "': '" + actionId + "'");
      }
    }
    if (!actionResult.isValid()) {
      result.addError("Policy '" + name + "' has invalid actions:");
      result.merge(actionResult);
    }

    ValidationResult notificationsResult = notifications.validate();
    if (!notificationsResult.isValid()) {
      result.addError("Policy '" + name + "' has invalid notifications:");
      result.merge(notificationsResult);
    }

    if (getThreatLevel() < 0 || getThreatLevel() > 10) {
      result.addError("Policy '" + name + "' has threat level outside of valid range 0-10: " + getThreatLevel());
    }

    if (!result.isValid()) {
      log.debug("Validation result: {}", result.toMessageString());
    }

    return result;
  }

  @Override
  public String toString() {
    return "Policy [id=" + id + ", name=" + name + "]";
  }

  public Constraint getConstraintById(String constraintId) {
    for (Constraint constraint : constraints) {
      if (constraint.getId() != null && constraint.getId().equals(constraintId)) {
        return constraint;
      }
    }
    return null;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  /**
   * A policy can have conditions in more than one category, but only one category is considered as *the* policy
   * threat category, in this order: SECURITY, LICENSE, QUALITY, OTHER.
   *
   * @since 1.11
   */
  @JsonIgnore
  public PolicyThreatCategory getThreatCategory() {
    SortedSet<PolicyThreatCategory> threatCategories = new TreeSet<>();
    for (Constraint constraint : getConstraints()) {
      for (Condition condition : constraint.getConditions()) {
        ConditionType conditionType = ConditionTypes.getById(condition.getConditionTypeId());
        threatCategories.add(conditionType.getThreatCategory());
      }
    }
    return PolicyThreatCategory.getCategory(threatCategories);
  }

  @JsonIgnore
  public String getDroolsCode() {
    return droolsCode;
  }

  public void setDroolsCode(String droolsCode) {
    this.droolsCode = droolsCode;
  }

  public boolean isPolicyActionsOverrideAllowed() {
    return policyActionsOverrideAllowed;
  }

  public void setPolicyActionsOverrideAllowed(final boolean policyActionsOverrideAllowed) {
    this.policyActionsOverrideAllowed = policyActionsOverrideAllowed;
  }

  public Map<String, Map<String, String>> getPolicyActionsOverrides() {
    return policyActionsOverrides;
  }

  public void setPolicyActionsOverrides(final Map<String, Map<String, String>> policyActionsOverrides) {
    this.policyActionsOverrides = policyActionsOverrides;
  }

  /**
   * Add action override to this policy
   * @param ownerId  the id of the org or app to which the override should be applied to
   * @param policyActionsOverride actions mapped to stage
   */
  public void addPolicyActionsOverride(final String ownerId, final Map<String, String> policyActionsOverride) {
    if (policyActionsOverrides == null) {
      policyActionsOverrides = new LinkedHashMap<>();
    }
    policyActionsOverrides.put(ownerId, policyActionsOverride);
  }
}
