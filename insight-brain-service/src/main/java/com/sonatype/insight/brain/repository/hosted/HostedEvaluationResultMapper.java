/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;

/**
 * Converts the output of {@link com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator}
 * into a {@link HostedEvaluationResult} for the synchronous hosted-enforcement response.
 * <p>
 * Mapping rules:
 * <ul>
 * <li>{@code blocked} is {@code true} iff any {@link PolicyAlert} has an {@link Action} with
 * {@code actionTypeId = FAIL}.</li>
 * <li>{@code policyAction} is the highest-severity action observed across all alerts, where
 * {@code FAIL > WARN > null}. {@code NOTIFY} is not surfaced because it represents a
 * silent side-effect, not a user-facing decision.</li>
 * <li>{@code highestThreatLevel} is the maximum {@link PolicyFact#getThreatLevel()} across
 * all alerts, or {@code 0} if there are no alerts.</li>
 * <li>{@code blockingViolations} contains only alerts whose actions include {@code FAIL}.
 * Warnings are intentionally not surfaced — the developer-facing contract is "what did
 * you need to fix to deploy?", not "what advisory items fired?".</li>
 * </ul>
 */
@Named
@Singleton
public class HostedEvaluationResultMapper
{
  /**
   * Build a {@link HostedEvaluationResult} for a single-component evaluation.
   *
   * @param evaluationResult IQ evaluator output — must contain exactly one component result.
   *          Null or empty lists are treated as "no violations" (allow).
   * @param evaluationUrl the URL to include in the response; populated even on allow so
   *          NXRM / audit consumers always have a stable link.
   * @param correlationId the correlation id supplied by NXRM; echoed back unchanged.
   * @param componentId the IQ-side componentId for the uploaded artifact (may be null if
   *          evaluation did not produce one, e.g. in degraded paths).
   * @return the mapped response envelope; never null.
   */
  public HostedEvaluationResult map(
      final RepositoryComponentEvaluationDataList evaluationResult,
      final String evaluationUrl,
      final String correlationId,
      final String componentId)
  {
    List<PolicyAlert> alerts = extractAlerts(evaluationResult);

    boolean blocked = false;
    boolean hasWarn = false;
    int highestThreatLevel = 0;
    List<HostedBlockingViolation> policyViolations = new ArrayList<>();

    for (PolicyAlert alert : alerts) {
      if (alert == null) {
        continue;
      }
      PolicyFact trigger = alert.getTrigger();
      if (trigger != null) {
        highestThreatLevel = Math.max(highestThreatLevel, trigger.getThreatLevel());
      }
      boolean alertBlocks = false;
      for (Action action : safe(alert.getActions())) {
        if (Action.ID_FAIL.equals(action.getActionTypeId())) {
          alertBlocks = true;
          blocked = true;
        }
        else if (Action.ID_WARN.equals(action.getActionTypeId())) {
          hasWarn = true;
        }
      }
      if (alertBlocks) {
        policyViolations.addAll(toPolicyViolations(trigger));
      }
    }

    // Locale.ROOT prevents Turkish-locale "i" → "İ" surprises; Action IDs are ASCII so the
    // result is identical to default-locale upper-casing across all environments.
    String policyAction = blocked
        ? Action.ID_FAIL.toUpperCase(Locale.ROOT)
        : hasWarn
            ? Action.ID_WARN.toUpperCase(Locale.ROOT)
            : null;

    return new HostedEvaluationResult(
        blocked,
        policyAction,
        highestThreatLevel,
        evaluationUrl,
        policyViolations,
        correlationId,
        componentId);
  }

  private static List<PolicyAlert> extractAlerts(final RepositoryComponentEvaluationDataList result) {
    if (result == null || result.componentEvalResults == null || result.componentEvalResults.isEmpty()) {
      return List.of();
    }
    // Sync enforcement always evaluates one component at a time — take its alerts.
    // Defensive: if caller unexpectedly passes multiple results, aggregate across them.
    List<PolicyAlert> all = new ArrayList<>();
    for (RepositoryComponentEvaluationData data : result.componentEvalResults) {
      if (data != null && data.policyAlerts != null) {
        all.addAll(data.policyAlerts);
      }
    }
    return all;
  }

  private static List<HostedBlockingViolation> toPolicyViolations(final PolicyFact trigger) {
    if (trigger == null) {
      return List.of();
    }
    List<HostedBlockingViolation> out = new ArrayList<>();
    String policyName = trigger.getPolicyName();
    for (ComponentFact componentFact : safe(trigger.getComponentFacts())) {
      String componentIdentifier = componentIdentifierOf(componentFact);
      List<ConstraintFact> constraints = safe(componentFact.getConstraintFacts());
      if (constraints.isEmpty()) {
        // Defensive: surface a violation row even without a named constraint.
        out.add(new HostedBlockingViolation(policyName, null, null, componentIdentifier));
        continue;
      }
      for (ConstraintFact constraint : constraints) {
        out.add(new HostedBlockingViolation(
            policyName,
            constraint.getConstraintName(),
            constraint.getOperatorName(),
            componentIdentifier));
      }
    }
    return out;
  }

  private static String componentIdentifierOf(final ComponentFact componentFact) {
    if (componentFact == null || componentFact.getComponentIdentifier() == null) {
      return null;
    }
    // ComponentIdentifier.toString() renders as "format/namespace:name:version" — close enough
    // for developer display and log correlation. Purl-style would be nicer; acceptable for 1.0.
    return componentFact.getComponentIdentifier().toString();
  }

  private static <T> List<T> safe(final List<T> list) {
    return list == null ? List.of() : list;
  }
}
