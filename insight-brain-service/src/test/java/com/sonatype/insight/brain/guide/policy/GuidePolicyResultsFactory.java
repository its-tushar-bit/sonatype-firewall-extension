/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.policy;

import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.policy.evaluator.PolicyResults;
import com.sonatype.insight.brain.policy.evaluator.PolicyResultsAccess;

/**
 * Hand-builds a {@link PolicyResults} object with {@link PolicyAlert}s containing real
 * {@link PolicyFact}/{@link ComponentFact}/{@link ConstraintFact} structure — enough for
 * {@link GuidePolicyComplianceMapper} to walk. Lives in test source to keep production
 * code free of test-only fixture knowledge.
 */
final class GuidePolicyResultsFactory
{
  private final PolicyResults results;

  private PolicyAlert lastAlert;

  private GuidePolicyResultsFactory(PolicyResults base) {
    this.results = base;
  }

  static GuidePolicyResultsFactory with(PolicyResults base) {
    return new GuidePolicyResultsFactory(base);
  }

  GuidePolicyResultsFactory activeAlert(
      Component component,
      String policyId,
      String policyName,
      int threatLevel,
      String actionId,
      ConstraintFact constraint)
  {
    PolicyFact pf = new PolicyFact(policyId, policyName, threatLevel);
    ComponentFact cf = new ComponentFact(component.getComponentIdentifier(), component.getHash());
    cf.addConstraintFact(constraint);
    pf.addComponentFact(cf);
    PolicyAlert alert = new PolicyAlert(pf, List.of(new Action(actionId)));
    PolicyResultsAccess.addActiveAlert(results, alert);
    this.lastAlert = alert;
    return this;
  }

  GuidePolicyResultsFactory waivedAlert(
      Component component,
      String policyId,
      String policyName,
      int threatLevel,
      String actionId,
      ConstraintFact constraint)
  {
    PolicyFact pf = new PolicyFact(policyId, policyName, threatLevel);
    ComponentFact cf = new ComponentFact(component.getComponentIdentifier(), component.getHash());
    cf.addConstraintFact(constraint);
    pf.addComponentFact(cf);
    PolicyAlert alert = new PolicyAlert(pf, List.of(new Action(actionId)));
    PolicyResultsAccess.addWaivedAlert(results, alert);
    this.lastAlert = alert;
    return this;
  }

  PolicyAlert lastAlert() {
    return lastAlert;
  }

  static ConstraintFact constraint(String id, String name, ConditionFact... conditions) {
    ConstraintFact cf = new ConstraintFact(id, name, "ALL");
    Arrays.stream(conditions).forEach(cf::addConditionFact);
    return cf;
  }

  /**
   * Build a {@link ConditionFact} carrying a {@code reason} string and (optionally) a
   * {@link TriggerReference}. Pass {@code triggerType=null} for conditions that don't
   * surface a structured trigger (most do not — only the security-vuln conditions emit
   * {@link TriggerReference.Type#SECURITY_VULNERABILITY_REFID}).
   */
  static ConditionFact reason(String reason, TriggerReference.Type triggerType, String triggerValue) {
    TriggerReference ref = (triggerType == null) ? null : new TriggerReference(triggerType, triggerValue);
    return new ConditionFact("typeId", 0, "summary", reason, ref);
  }

  PolicyResults build() {
    return results;
  }
}
