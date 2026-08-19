/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

public class Policy
{
  private String id;

  private String name;

  /**
   * @since 1.6
   */
  private String ownerId;

  private int threatLevel = 5;

  private boolean legacyViolationAllowed;

  private List<Constraint> constraints;

  private Map<String, String> actions = new HashMap<>();

  private Notifications notifications = new Notifications();

  private String droolsCode;

  private boolean policyActionsOverrideAllowed;

  private Map<String, Map<String, String>> policyActionsOverrides;

  /**
   * @since TBD
   */
  private boolean policyNotificationsOverrideAllowed;

  /**
   * @since TBD
   */
  private Map<String, Notifications> policyNotificationsOverrides;

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

  /**
   * @deprecated Use isLegacyViolationAllowed
   */
  @Deprecated
  public boolean isPolicyViolationGrandfatheringAllowed() {
    return legacyViolationAllowed;
  }

  /**
   * @deprecated Use setLegacyViolationAllowed
   */
  @JsonProperty(access = Access.WRITE_ONLY)
  @Deprecated
  public void setPolicyViolationGrandfatheringAllowed(boolean policyViolationGrandfatheringAllowed) {
    setLegacyViolationAllowed(policyViolationGrandfatheringAllowed);
  }

  public boolean isLegacyViolationAllowed() {
    return legacyViolationAllowed;
  }

  public void setLegacyViolationAllowed(boolean legacyViolationAllowed) {
    this.legacyViolationAllowed = legacyViolationAllowed;
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
   *          and ending with root org. This is used for overrides.
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
    result.addAll(getEffectiveNotifications(ownerIds).getApplicable(stageId, continuousMonitoring).toActions());
    return result;
  }

  private Map<String, String> getActionOverrides(final List<String> ownerIds) {
    if (!isActionsOverrideApplicable(ownerIds) || policyActionsOverrides == null || policyActionsOverrides.isEmpty()) {
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

  private boolean isActionsOverrideApplicable(final List<String> ownerIds) {
    return policyActionsOverrideAllowed && ownerIds != null;
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
   *
   * @param ownerId the id of the org or app to which the override should be applied to
   * @param policyActionsOverride actions mapped to stage
   */
  public void addPolicyActionsOverride(final String ownerId, final Map<String, String> policyActionsOverride) {
    if (policyActionsOverrides == null) {
      policyActionsOverrides = new LinkedHashMap<>();
    }
    policyActionsOverrides.put(ownerId, policyActionsOverride);
  }

  public boolean isPolicyNotificationsOverrideAllowed() {
    return policyNotificationsOverrideAllowed;
  }

  public void setPolicyNotificationsOverrideAllowed(boolean policyNotificationsOverrideAllowed) {
    this.policyNotificationsOverrideAllowed = policyNotificationsOverrideAllowed;
  }

  public Map<String, Notifications> getPolicyNotificationsOverrides() {
    return policyNotificationsOverrides;
  }

  public void setPolicyNotificationsOverrides(Map<String, Notifications> policyNotificationsOverrides) {
    this.policyNotificationsOverrides = policyNotificationsOverrides;
  }

  private Notifications getNotificationsOverride(List<String> ownerIds) {
    if (!isNotificationsOverrideApplicable(ownerIds) || policyNotificationsOverrides == null ||
        policyNotificationsOverrides.isEmpty())
    {
      return null;
    }

    for (int i = 0; i < ownerIds.size() - 1; i++) {
      Notifications notificationsOverride = policyNotificationsOverrides.get(ownerIds.get(i));
      // walk through the hierarchy stops in front of the policy owner
      if (notificationsOverride != null || ownerIds.get(i + 1).equals(this.getOwnerId())) {
        return notificationsOverride;
      }
    }

    return null;
  }

  private boolean isNotificationsOverrideApplicable(List<String> ownerIds) {
    return policyNotificationsOverrideAllowed && ownerIds != null;
  }

  /**
   * Add notifications override to this policy
   *
   * @param ownerId the id of the org or app to which the override should be applied to
   * @param policyNotificationsOverride notifications mapped to stages
   */
  public void addPolicyNotificationsOverride(String ownerId, Notifications policyNotificationsOverride) {
    if (policyNotificationsOverrides == null) {
      policyNotificationsOverrides = new LinkedHashMap<>();
    }
    policyNotificationsOverrides.put(ownerId, policyNotificationsOverride);
  }

  public Notifications getEffectiveNotifications(List<String> ownerIds) {
    Notifications notificationsOverride = getNotificationsOverride(ownerIds);
    return notificationsOverride != null ? notificationsOverride : notifications;
  }
}
