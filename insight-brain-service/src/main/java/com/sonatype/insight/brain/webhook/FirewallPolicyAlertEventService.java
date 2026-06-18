/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.webhook.dto.FirewallPolicyAlertComponentDTO;
import com.sonatype.insight.brain.webhook.dto.FirewallPolicyAlertViolationDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @since 1.205.0 */
@Named
@Singleton
public class FirewallPolicyAlertEventService
{
  private static final Logger log = LoggerFactory.getLogger(FirewallPolicyAlertEventService.class);

  private final AsyncEventBus asyncEventBus;

  private final CurrentUser currentUser;

  @Inject
  public FirewallPolicyAlertEventService(final AsyncEventBus asyncEventBus, final CurrentUser currentUser) {
    this.asyncEventBus = asyncEventBus;
    this.currentUser = currentUser;
  }

  public void postEvent(
      final Repository repository,
      final String pathname,
      final String hash,
      final Date quarantineTime,
      final List<PolicyAlert> activeAlerts)
  {
    if (activeAlerts == null || activeAlerts.isEmpty()) {
      return;
    }
    try {
      Map<String, List<FirewallPolicyAlertViolationDTO>> violationsByTarget =
          groupViolationsByWebhookTarget(activeAlerts, pathname, hash);

      if (violationsByTarget.isEmpty()) {
        log.debug("No webhook targets configured for any active alert in repository {} pathname {} — skipping event",
            repository.getId(), pathname);
        return;
      }

      String initiator = currentUser.getUsernameOrSystem();

      violationsByTarget.forEach((targetId, violations) -> {
        FirewallPolicyAlertEvent event = new FirewallPolicyAlertEvent(targetId);
        event.repositoryId = repository.getId();
        event.repositoryPublicId = repository.getPublicId();
        event.repositoryFormat = repository.getFormat();
        event.quarantineTime = quarantineTime;
        event.violations = violations;
        event.initiator = initiator;
        asyncEventBus.post(event);
      });
    }
    catch (RuntimeException e) {
      log.error("FirewallPolicyAlertEvent not posted for repository {} pathname {} due to exception.",
          repository.getId(), pathname, e);
    }
  }

  private Map<String, List<FirewallPolicyAlertViolationDTO>> groupViolationsByWebhookTarget(
      final List<PolicyAlert> activeAlerts,
      final String pathname,
      final String hash)
  {
    Map<String, List<FirewallPolicyAlertViolationDTO>> byTarget = new HashMap<>();

    for (PolicyAlert alert : activeAlerts) {
      List<String> webhookTargets = getWebhookTargets(alert);
      if (webhookTargets.isEmpty()) {
        continue;
      }
      FirewallPolicyAlertViolationDTO violation = toViolationDTO(alert.getTrigger(), pathname, hash);
      if (violation.componentFacts.isEmpty()) {
        log.warn("No matching component facts for policy {} hash {} — skipping violation",
            alert.getTrigger().getPolicyId(), hash);
        continue;
      }
      for (String targetId : webhookTargets) {
        byTarget.computeIfAbsent(targetId, k -> new ArrayList<>()).add(violation);
      }
    }
    return byTarget;
  }

  private List<String> getWebhookTargets(final PolicyAlert alert) {
    List<String> targets = new ArrayList<>();
    if (alert.getActions() == null) {
      return targets;
    }
    for (Action action : alert.getActions()) {
      if (Objects.equals(action.getTargetType(), NotifyActionType.TARGET_TYPE_WEBHOOK)) {
        targets.add(action.getTarget());
      }
    }
    return targets;
  }

  private FirewallPolicyAlertViolationDTO toViolationDTO(
      final PolicyFact policyFact,
      final String pathname,
      final String hash)
  {
    FirewallPolicyAlertViolationDTO dto = new FirewallPolicyAlertViolationDTO();
    dto.policyId = policyFact.getPolicyId();
    dto.policyName = policyFact.getPolicyName();
    dto.threatLevel = policyFact.getThreatLevel();
    dto.policyViolationId = policyFact.getPolicyViolationId();

    for (ComponentFact componentFact : policyFact.getComponentFacts()) {
      FirewallPolicyAlertComponentDTO componentDTO = new FirewallPolicyAlertComponentDTO();
      // Filter to the pathname/hash actually being persisted — Firewall is per-component, the alert
      // may carry facts for siblings that aren't in the same persistRepositoryComponent() call.
      if (!matchesPersistedComponent(componentFact, pathname, hash)) {
        continue;
      }
      componentDTO.hash = componentFact.getHash();
      componentDTO.displayName = componentFact.getDisplayName() != null
          ? componentFact.getDisplayName().toString()
          : "Unknown Component";
      if (componentFact.getComponentIdentifier() != null) {
        componentDTO.componentIdentifier =
            ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentFact.getComponentIdentifier());
      }
      if (componentFact.getPathnames() != null) {
        componentDTO.pathNames = new ArrayList<>(componentFact.getPathnames());
      }
      if (componentFact.getConstraintFacts() != null) {
        for (ConstraintFact constraintFact : componentFact.getConstraintFacts()) {
          componentDTO.constraintFacts.add(new ConstraintFactDTO(constraintFact));
        }
      }
      dto.componentFacts.add(componentDTO);
    }
    return dto;
  }

  private boolean matchesPersistedComponent(final ComponentFact fact, final String pathname, final String hash) {
    if (!Objects.equals(fact.getHash(), hash)) {
      return false;
    }
    if (fact.getPathnames() == null) {
      return true;
    }
    return fact.getPathnames().contains(pathname);
  }
}
