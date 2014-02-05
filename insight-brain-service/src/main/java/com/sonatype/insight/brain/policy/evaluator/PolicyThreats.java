/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO describing the contents of the {@code policythreats.json} used to provide information about policy violations
 * specifically for consumption by the report.
 * 
 * @since 1.9
 */
class PolicyThreats
{
  // designator of format version to help interop
  public int version;

  // components violating policies, in no particular order
  public List<Component> aaData = new ArrayList<>();

  public static class Component
  {
    // identification
    public String hash;
    public String groupId;
    public String artifactId;
    public String version;

    // top critical violation among active violations, mostly for backward-compat
    public String policyId;
    public String policyName;
    public int policyThreatLevel;

    // active and waived violations, in no particular order
    public List<PolicyViolation> activeViolations = new ArrayList<>();
    public List<PolicyViolation> waivedViolations = new ArrayList<>();
  }

  public static class PolicyViolation
  {
    public String policyId;
    public String policyName;
    public int policyThreatLevel;

    public List<PolicyAction> actions = new ArrayList<>();

    public List<PolicyConstraint> constraints = new ArrayList<>();
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
  }
}
