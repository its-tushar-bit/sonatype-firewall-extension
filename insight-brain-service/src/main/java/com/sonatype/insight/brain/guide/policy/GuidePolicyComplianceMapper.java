/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.policy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.guide.api.dto.policy.GuideConstraintViolation;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceLevel;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceSummary;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyViolation;
import com.sonatype.insight.brain.guide.api.dto.policy.GuideTriggerReference;
import com.sonatype.insight.brain.guide.api.dto.policy.GuideViolationReason;
import com.sonatype.insight.brain.guide.api.dto.policy.GuideWaiverInfo;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.policy.evaluator.PolicyResults;

/**
 * Pure function mapping {@link PolicyResults} for a single component into the wire-shape
 * {@link GuidePolicyCompliance} record. No I/O, no DI.
 *
 * <p>
 * Relies on the {@code ComponentPolicyEvaluator} invariant that every emitted {@link PolicyAlert}
 * carries at least one {@link ComponentFact} — the "main" component is read via {@code
 * getComponentFacts().getFirst()}. The evaluator dereferences the first fact unconditionally too, so
 * the invariant holds in practice; were it ever violated this would throw {@link
 * java.util.NoSuchElementException} rather than soft-fail.
 */
public final class GuidePolicyComplianceMapper
{
  private static final String ACTION_NONE = "none";

  private GuidePolicyComplianceMapper() {
  }

  /**
   * Map the batch {@link PolicyResults} into the {@link GuidePolicyCompliance} wire shape for a
   * single {@code component}. The evaluator fires one Drools session over every component in the
   * request, so this filters {@code results} to the alerts whose main component matches
   * {@code component} (by {@code ComponentIdentifier}) and rolls them up:
   * <ul>
   * <li>active alerts &rarr; {@code violations} (waived=false) and feed {@code summary}'s
   * {@code highestThreatLevel}, {@code worstAction} (fail &gt; warn &gt; notify &gt; none),
   * {@code activeViolationCount}, and the per-{@link PolicyThreatCategory} counts;
   * <li>waived alerts &rarr; {@code violations} (waived=true, with {@link GuideWaiverInfo} when the
   * waiver lookup resolves) and {@code waivedViolationCount}; they do not count against
   * compliance.
   * </ul>
   * {@code complianceLevel} is derived from the worst active action and the waived count (see
   * {@link #complianceLevel}), and {@code compliant} is {@code complianceLevel != FAIL}.
   *
   * @param results
   *          batch evaluation results (active + waived alerts across all evaluated components)
   * @param component
   *          the component to extract compliance for; alerts are matched to it by component identifier
   * @param ownerId
   *          the evaluation owner id, echoed into the wire shape
   * @param stage
   *          the evaluated stage; its {@code stageTypeId} is echoed into the wire shape
   * @param policiesById
   *          applicable policies keyed by id, used to resolve each violation's threat category
   * @param ownerTypeByOwnerId
   *          pre-resolved waiver-owner types, used to populate each waiver's {@code scopeOwnerType}
   * @return the full-shape compliance for {@code component}; callers may reduce it to the badge
   *         ({@code compliant} + {@code complianceLevel}) — see {@link GuidePolicyResponseEnricher}
   *         and {@code McpPolicyCompliance}
   */
  public static GuidePolicyCompliance toCompliance(
      PolicyResults results,
      Component component,
      String ownerId,
      Stage stage,
      Map<String, Policy> policiesById,
      Map<String, OwnerType> ownerTypeByOwnerId)
  {
    List<GuidePolicyViolation> violations = new ArrayList<>();
    Map<String, Integer> counts = emptyCategoryCounts();
    int highestThreatLevel = 0;
    String worstAction = ACTION_NONE;
    int activeCount = 0;
    int waivedCount = 0;

    for (PolicyAlert active : results.getActiveAlerts()) {
      if (!matchesComponent(active, component)) {
        continue;
      }
      GuidePolicyViolation v = mapViolation(active, false, null, policiesById, ownerTypeByOwnerId);
      violations.add(v);
      activeCount++;
      if (v.threatLevel() > highestThreatLevel) {
        highestThreatLevel = v.threatLevel();
      }
      worstAction = mostSevereAction(worstAction, v.actions());
      String category = categoryFor(active.getTrigger(), policiesById);
      counts.merge(category, 1, Integer::sum);
    }

    for (PolicyAlert waived : results.getWaivedAlerts()) {
      if (!matchesComponent(waived, component)) {
        continue;
      }
      ComponentFact cf = waived.getTrigger().getComponentFacts().getFirst();
      PolicyWaiver waiver = results.getPolicyWaiver(cf);
      // The waived-alerts loop is itself the authoritative "this is waived" signal, so pass
      // waived=true unconditionally. The waiver lookup can miss (it is keyed by ComponentFact
      // identity); when it does, only the optional GuideWaiverInfo detail is dropped, not the flag.
      GuidePolicyViolation v = mapViolation(waived, true, waiver, policiesById, ownerTypeByOwnerId);
      violations.add(v);
      waivedCount++;
    }

    GuidePolicyComplianceLevel level = complianceLevel(worstAction, waivedCount);
    GuidePolicyComplianceSummary summary = new GuidePolicyComplianceSummary(
        highestThreatLevel, worstAction, activeCount, waivedCount, counts);
    return new GuidePolicyCompliance(
        level != GuidePolicyComplianceLevel.FAIL, level, stage.getStageTypeId(), ownerId, summary, violations);
  }

  private static boolean matchesComponent(PolicyAlert alert, Component component) {
    ComponentFact cf = alert.getTrigger().getComponentFacts().getFirst();
    return component.getComponentIdentifier().equals(cf.getComponentIdentifier());
  }

  private static GuidePolicyViolation mapViolation(
      PolicyAlert alert,
      boolean waived,
      PolicyWaiver waiver,
      Map<String, Policy> policiesById,
      Map<String, OwnerType> ownerTypeByOwnerId)
  {
    PolicyFact pf = alert.getTrigger();
    List<String> actions = alert.getActions()
        .stream()
        .map(Action::getActionTypeId)
        .toList();

    List<GuideConstraintViolation> constraintViolations = new ArrayList<>();
    ComponentFact cf = pf.getComponentFacts().getFirst();
    // getConstraintFacts()/getConditionFacts() are assumed non-null for alerts emitted by the
    // ComponentPolicyEvaluator, but defaulting to an empty list keeps the mapper safe if a
    // future alert shape (e.g. third-party scan results) ever returns null instead.
    for (ConstraintFact cfact : Objects.requireNonNullElse(cf.getConstraintFacts(), List.<ConstraintFact>of())) {
      List<GuideViolationReason> reasons = new ArrayList<>();
      for (ConditionFact condition : Objects.requireNonNullElse(cfact.getConditionFacts(), List.<ConditionFact>of())) {
        GuideTriggerReference triggerRef = null;
        // ConditionFact.getReference() — note: NOT getTriggerReference(). Returns
        // com.sonatype.clm.dto.model.policy.TriggerReference whose getType() returns the
        // enum TriggerReference.Type; we expose .name() in the wire shape so consumers see
        // a stable string like "SECURITY_VULNERABILITY_REFID".
        TriggerReference ref = condition.getReference();
        if (ref != null) {
          // getType() is set for all known ComponentPolicyEvaluator condition types, but guard
          // against null so a future or third-party condition that omits the enum yields a null
          // type rather than NPE-ing the whole mapper (which the soft-fail would otherwise swallow
          // as "no policy data").
          triggerRef = new GuideTriggerReference(
              ref.getType() != null ? ref.getType().name() : null,
              ref.getValue());
        }
        reasons.add(new GuideViolationReason(condition.getReason(), triggerRef));
      }
      constraintViolations.add(
          new GuideConstraintViolation(cfact.getConstraintId(), cfact.getConstraintName(), reasons));
    }

    // PolicyWaiver does not expose scopeOwnerType — it carries only ownerId. The wire shape
    // needs the owner's OwnerType (organization/application/repository/etc.), so the
    // evaluator pre-resolves a map id->OwnerType and passes it via ownerTypeByOwnerId.
    // Date->Instant conversion: PolicyWaiver.getExpiryTime() returns java.util.Date.
    String scopeOwnerType = null;
    if (waiver != null) {
      OwnerType type = ownerTypeByOwnerId.get(waiver.getOwnerId());
      scopeOwnerType = type == null ? null : type.name().toLowerCase(Locale.ROOT);
    }
    Instant expiry = (waiver == null || waiver.getExpiryTime() == null)
        ? null
        : waiver.getExpiryTime().toInstant();
    GuideWaiverInfo waiverInfo = (waiver == null)
        ? null
        : new GuideWaiverInfo(
            scopeOwnerType,
            waiver.getOwnerId(),
            expiry,
            waiver.getComment());

    return new GuidePolicyViolation(
        pf.getPolicyId(), pf.getPolicyName(), pf.getThreatLevel(), actions,
        waived, waiverInfo, constraintViolations);
  }

  private static String mostSevereAction(String current, List<String> alertActions) {
    return alertActions.stream()
        .reduce(current, GuidePolicyComplianceMapper::compareActions);
  }

  /**
   * Derive the green/amber/red badge level from the worst <em>active</em> action and the waived
   * count. {@code FAIL} (red) when an active violation carries a fail action; {@code WARN} (amber)
   * when the worst active action is warn, or there are violations but they're all waived; otherwise
   * {@code PASS} (green) — a clean component, or one whose only active actions are notify/none. The
   * companion {@code compliant} boolean is {@code level != FAIL}.
   */
  private static GuidePolicyComplianceLevel complianceLevel(String worstActiveAction, int waivedCount) {
    if (Action.ID_FAIL.equals(worstActiveAction)) {
      return GuidePolicyComplianceLevel.FAIL;
    }
    if (Action.ID_WARN.equals(worstActiveAction) || waivedCount > 0) {
      return GuidePolicyComplianceLevel.WARN;
    }
    return GuidePolicyComplianceLevel.PASS;
  }

  /**
   * Severity order: fail > warn > notify > none. Mirrors
   * {@link com.sonatype.insight.brain.repository.hosted.HostedEvaluationResultMapper}'s order.
   */
  private static String compareActions(String left, String right) {
    return rank(right) > rank(left) ? right : left;
  }

  private static int rank(String action) {
    if (Action.ID_FAIL.equals(action)) {
      return 3;
    }
    if (Action.ID_WARN.equals(action)) {
      return 2;
    }
    if (Action.ID_NOTIFY.equals(action)) {
      return 1;
    }
    return 0; // ACTION_NONE
  }

  private static String categoryFor(
      PolicyFact pf,
      Map<String, Policy> policiesById)
  {
    Policy policy = policiesById.get(pf.getPolicyId());
    if (policy == null) {
      return PolicyThreatCategory.OTHER.name();
    }
    PolicyThreatCategory category = policy.getThreatCategory();
    return category == null ? PolicyThreatCategory.OTHER.name() : category.name();
  }

  private static Map<String, Integer> emptyCategoryCounts() {
    // Seed every PolicyThreatCategory so the wire shape's category set always matches what
    // categoryFor() can emit (category.name()) — a new enum variant flows through automatically.
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (PolicyThreatCategory category : PolicyThreatCategory.values()) {
      counts.put(category.name(), 0);
    }
    return counts;
  }
}
