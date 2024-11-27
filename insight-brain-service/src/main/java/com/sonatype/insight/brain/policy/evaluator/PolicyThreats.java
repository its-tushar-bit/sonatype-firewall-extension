/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * DTO describing the contents of the {@code policythreats.json} used to provide information about policy violations
 * specifically for consumption by the report.
 *
 * @since 1.9
 */
public class PolicyThreats
{
  // designator of format version to help interop
  public int version;

  public String stageTypeId;

  // components violating policies, in no particular order
  public List<Component> aaData = new ArrayList<>();

  public static class Component
  {
    // identification
    public String hash;

    /**
     * Legacy field only used by {@code WaivedPolicyViolationMigrator}. Use
     * {@link PolicyThreats.Component#componentIdentifier}.
     */
    @Deprecated
    @JsonInclude(Include.NON_NULL)
    public String groupId;

    /**
     * Legacy field only used by {@code WaivedPolicyViolationMigrator}. Use
     * {@link PolicyThreats.Component#componentIdentifier}.
     */
    @Deprecated
    @JsonInclude(Include.NON_NULL)
    public String artifactId;

    /**
     * Legacy field only used by {@code WaivedPolicyViolationMigrator}. Use
     * {@link PolicyThreats.Component#componentIdentifier}.
     */
    @Deprecated
    @JsonInclude(Include.NON_NULL)
    public String version;

    /**
     * @since 1.13.0
     */
    public ComponentIdentifier componentIdentifier;

    // top critical violation among active violations, mostly for backward-compat
    public String policyId;

    public String policyName;

    public int policyThreatLevel;

    // active and waived violations, in no particular order, for backward compatibility
    public List<PolicyViolation> activeViolations = new ArrayList<>();

    public List<PolicyViolation> waivedViolations = new ArrayList<>();

    /**
     * @since 1.50
     */
    public List<PolicyViolation> allViolations = new ArrayList<>();
  }

  public static class PolicyViolation
  {
    public String policyId;

    /**
     * @since 1.70
     */
    public String policyViolationId;

    public String policyName;

    public int policyThreatLevel;

    public String policyOwnerId;

    public String policyOwnerType;

    /**
     * @since 1.50
     */
    public boolean waived;

    /**
     * @since 1.180
     */
    @JsonInclude(Include.NON_NULL)
    public boolean waivedWithAutoWaiver;

    /**
     * @since 1.50
     * @deprecated Use {@link PolicyThreats.PolicyViolation#legacyViolation}.
     */
    @Deprecated
    public boolean grandfathered;

    /**
     * @since 1.168
     */
    public boolean legacyViolation;

    /**
     * @since 1.53
     */
    public String constraintFactsJson;

    public List<PolicyAction> actions = new ArrayList<>();

    public List<PolicyConstraint> constraints = new ArrayList<>();

    /**
     * @since 1.61
     */
    public String policyThreatCategory;

    /**
     * @since 1.81
     */
    public ReachabilityStatus reachabilityStatus;
  }

  public static class PolicyAction
  {
    public String actionType;

    public String actionSummary;
  }

  public static class PolicyConstraint
  {
    public String constraintId;

    public String constraintName;

    public String constraintOperator;

    public List<PolicyCondition> conditions = new ArrayList<>();
  }

  public static class PolicyCondition
  {
    public String conditionType;

    public String conditionSummary;

    public String conditionReason;

    public PolicyConditionTriggerReference conditionTriggerReference;
  }

  public static class PolicyConditionTriggerReference
  {
    public String value;

    public TriggerReference.Type type;
  }
}
