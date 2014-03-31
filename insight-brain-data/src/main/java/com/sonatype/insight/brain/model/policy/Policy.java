/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.NotifyAction;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Policy
{
  private static final Logger log = LoggerFactory.getLogger(Policy.class);

  private static final Predicate<String> IS_SECURITY = new StartsWithPredicate("security");

  private static final Predicate<String> IS_LICENSE = new StartsWithPredicate("license");

  private static final Predicate<String> IS_AGE = new StartsWithPredicate("age");

  private static final Predicate<String> IS_POPULARITY = new StartsWithPredicate("relativepopularity");

  private String id;

  private String name;

  /**
   * @since 1.6
   */
  private String ownerId;

  private boolean enabled = true;

  private int threatLevel = 5;

  private List<Constraint> constraints;

  private Map<String, List<Action>> actions;

  private List<NotifyAction> monitorNotifyActions;

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

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public int getThreatLevel() {
    return threatLevel;
  }

  public void setThreatLevel(final int threatLevel) {
    this.threatLevel = threatLevel;
  }

  public List<Constraint> getConstraints() {
    return constraints;
  }

  public void setConstraints(final List<Constraint> constraints) {
    this.constraints = constraints;
  }

  public void addConstraint(final Constraint constraint) {
    if (constraints == null) {
      constraints = new ArrayList<Constraint>();
    }
    constraints.add(constraint);
  }

  public Map<String, List<Action>> getActions() {
    return actions;
  }

  public void setActions(final Map<String, List<Action>> actions) {
    this.actions = actions;
  }

  public List<Action> getActions(final String stageTypeId) {
    return actions != null ? actions.get(stageTypeId) : null;
  }

  public void setActions(final String stageTypeId, final List<Action> stageActions) {
    if (actions == null) {
      actions = new HashMap<String, List<Action>>();
    }
    actions.put(stageTypeId, stageActions);
  }

  public void addAction(final String stageTypeId, final Action action) {
    List<Action> stageActions = getActions(stageTypeId);
    if (stageActions == null) {
      setActions(stageTypeId, stageActions = new ArrayList<Action>());
    }
    stageActions.add(action);
  }

  public ValidationResult validate(String ownerId) {
    return validate(ownerId, false);
  }

  public ValidationResult validate(String ownerId, boolean forEvaluation) {
    log.debug("Validating " + this.toString());

    ValidationResult result = new ValidationResult();
    if (forEvaluation) {
      // if only doing evaluation, go with lenient name validation to support legacy policies
      if (name == null || name.trim().isEmpty()) {
        result.addError("The policy name must not be null or empty");
      }
    }
    else {
      // if inserting/updating a policy, go with strict name validation
      try {
        NameHelper.validate("The policy name", name);
      }
      catch (InvalidNameException e) {
        result.addError(e.getMessage());
      }
    }
    if (constraints == null || constraints.isEmpty()) {
      result.addError("Policy '" + name + "' has no constraints");
    }
    else {
      ValidationResult constraintResult = new ValidationResult();
      Set<String> constraintNames = new LinkedHashSet<String>();
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
        constraintResult.merge(constraint.validate(ownerId));
      }
      if (!constraintResult.isValid()) {
        result.addError("Policy '" + name + "' has invalid constraints:");
        result.merge(constraintResult);
      }
    }

    if (actions != null) {
      ValidationResult actionResult = new ValidationResult();
      for (String stageTypeId : actions.keySet()) {

        StageType stageType = StageTypes.getById(stageTypeId);
        if (stageType == null) {
          actionResult.addError("Invalid stage type id: '" + stageTypeId + "'");
        }
        
        for (Action action : actions.get(stageTypeId)) {
          ActionType actionType = ActionTypes.getById(action.getActionTypeId());

          if (actionType == null) {
            actionResult.addError("Invalid action type id: '" + action.getActionTypeId() + "'");
          }
          else {
            actionResult.merge(actionType.validateAction(action));
          }
        }
      }
      if (!actionResult.isValid()) {
        result.addError("Policy '" + name + "' has invalid actions:");
        result.merge(actionResult);
      }
    }

    if (monitorNotifyActions != null) {
      ActionType notifyActionType = ActionTypes.getById(NotifyActionType.ID);
      ValidationResult monitorNotifyActionResult = new ValidationResult();
      for (NotifyAction notifyAction : monitorNotifyActions) {
        monitorNotifyActionResult.merge(notifyActionType.validateAction(notifyAction));
      }
      if (!monitorNotifyActionResult.isValid()) {
        result.addError("Policy '" + name + "' has invalid monitor notification actions:");
        result.merge(monitorNotifyActionResult);
      }
    }

    if (!result.isValid()) {
      log.debug("Validation result: " + result.toMessageString());
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

  public List<NotifyAction> getMonitorNotifyActions() {
    return monitorNotifyActions;
  }

  public void setMonitorNotifyActions(List<NotifyAction> monitorNotifyActions) {
    this.monitorNotifyActions = monitorNotifyActions;
  }

  public void addMonitorNotifyAction(NotifyAction notifyAction) {
    if (monitorNotifyActions == null) {
      monitorNotifyActions = new ArrayList<>();
    }
    monitorNotifyActions.add(notifyAction);
  }

  /**
   * @since 1.11
   */
  public PolicyThreatCategory getThreatCategory() {
    Set<String> conditionTypeIds = new LinkedHashSet<>();
    for (Constraint constraint : getConstraints()) {
      for (Condition condition : constraint.getConditions()) {
        conditionTypeIds.add(condition.getConditionTypeId().toLowerCase(Locale.ENGLISH));
      }
    }

    return determineCategory(conditionTypeIds);
  }

  private static PolicyThreatCategory determineCategory(Set<String> conditionTypeIds) {
    // A policy can have conditions in more than one category, but only one category is considered as *the* policy
    // threat category, in this order:
    if (Sets.filter(conditionTypeIds, IS_SECURITY).size() > 0) {
      return PolicyThreatCategory.SECURITY;
    }
    if (Sets.filter(conditionTypeIds, IS_LICENSE).size() > 0) {
      return PolicyThreatCategory.LICENSE;
    }
    if (Sets.filter(conditionTypeIds, IS_AGE).size() > 0
        || Sets.filter(conditionTypeIds, IS_POPULARITY).size() > 0) {
      return PolicyThreatCategory.QUALITY;
    }
    return PolicyThreatCategory.OTHER;
  }

  /**
   * Predicate wrapper for finding Strings starting with a given value in a collection.
   */
  private static class StartsWithPredicate
      implements Predicate<String>
  {
    private final String startsWith;

    StartsWithPredicate(String startsWith) {
      this.startsWith = startsWith;
    }

    @Override
    public boolean apply(@Nullable final String input) {
      return input.startsWith(startsWith);
    }
  }
}
