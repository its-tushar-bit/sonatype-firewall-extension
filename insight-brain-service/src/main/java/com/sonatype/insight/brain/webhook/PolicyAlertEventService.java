/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.webhook.dto.ApplicationSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

/**
 * @since 1.64.0
 */
@Named
@Singleton
public class PolicyAlertEventService
{
  private static final Logger log = LoggerFactory.getLogger(PolicyAlertEventService.class);

  private final AsyncEventBus asyncEventBus;

  private final CurrentUser currentUser;

  private final ApplicationDAO applicationDAO;

  @Inject
  public PolicyAlertEventService(
      final AsyncEventBus asyncEventBus,
      final CurrentUser currentUser,
      final ApplicationDAO applicationDAO)
  {
    this.asyncEventBus = asyncEventBus;
    this.currentUser = currentUser;
    this.applicationDAO = applicationDAO;
  }

  public void postEvent(
      final PolicyEvaluation policyEvaluation,
      final PolicyEvaluationResult policyEvaluationResult,
      final Application application,
      final List<PolicyAlert> waivedAlerts,
      final List<PolicyAlert> fixedAlerts)
  {
    try {
      final ApplicationSummary applicationSummary = populateApplicationSummary(policyEvaluation);
      final ApplicationEvaluationEvent applicationEvaluationEvent =
          ApplicationEvaluationEventService
              .buildEvent(policyEvaluation, policyEvaluationResult, currentUser, application);

      // group by target
      Map<String, List<PolicyFact>> groupedAlerts = policyEvaluationResult.getAlerts()
          .stream()
          .flatMap(this::getPolicyFactsByWebhookTarget)
          .collect(groupingBy(SimpleEntry::getKey, mapping(SimpleEntry::getValue, toList())));

      Map<String, List<PolicyFact>> groupedWaivedAlerts = waivedAlerts.stream()
          .flatMap(this::getPolicyFactsByWebhookTarget)
          .collect(groupingBy(SimpleEntry::getKey, mapping(SimpleEntry::getValue, toList())));

      Map<String, List<PolicyFact>> groupedFixedAlerts = fixedAlerts.stream()
          .flatMap(this::getPolicyFactsByWebhookTarget)
          .collect(groupingBy(SimpleEntry::getKey, mapping(SimpleEntry::getValue, toList())));

      groupedWaivedAlerts.forEach((key, value) -> groupedAlerts.putIfAbsent(key, Collections.emptyList()));
      groupedFixedAlerts.forEach((key, value) -> groupedAlerts.putIfAbsent(key, Collections.emptyList()));

      // post events
      groupedAlerts.entrySet()
          .stream()
          .map(entry -> createPolicyAlertEvent(applicationSummary, applicationEvaluationEvent, entry))
          .forEach(asyncEventBus::post);
    }
    catch (RuntimeException e) {
      log.error("Webhook not posted due to exception.", e);
    }
  }

  private Stream<SimpleEntry<String, PolicyFact>> getPolicyFactsByWebhookTarget(final PolicyAlert alert) {
    return alert.getActions()
        .stream()
        .filter(action -> Objects.equals(action.getTargetType(), NotifyActionType.TARGET_TYPE_WEBHOOK))
        .map(action -> new SimpleEntry<>(action.getTarget(), alert.getTrigger()));
  }

  private PolicyAlertEvent createPolicyAlertEvent(
      final ApplicationSummary applicationSummary,
      final ApplicationEvaluationEvent applicationEvaluationEvent,
      final Entry<String, List<PolicyFact>> groupedAlerts)
  {
    final PolicyAlertEvent event = new PolicyAlertEvent(groupedAlerts.getKey());
    event.applicationEvaluation = applicationEvaluationEvent;
    event.application = applicationSummary;
    event.policyFacts = groupedAlerts.getValue();
    event.initiator = currentUser.getUsernameOrSystem();
    return event;
  }

  private ApplicationSummary populateApplicationSummary(PolicyEvaluation policyEvaluation) {
    final Application application = applicationDAO.getByIdNotNull(policyEvaluation.getOwnerId());
    final ApplicationSummary applicationSummary = new ApplicationSummary();
    applicationSummary.id = policyEvaluation.getOwnerId();
    applicationSummary.publicId = application.getPublicId();
    applicationSummary.name = application.getName();
    applicationSummary.organizationId = application.getOrganizationId();

    return applicationSummary;
  }
}
