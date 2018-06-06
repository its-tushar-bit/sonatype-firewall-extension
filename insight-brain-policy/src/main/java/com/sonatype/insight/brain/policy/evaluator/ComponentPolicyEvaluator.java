/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.policy.DroolsGenerator;
import com.sonatype.insight.brain.utils.ComponentFactUtil;
import com.sonatype.insight.json.store.JsonUtils;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.definition.KnowledgePackage;
import org.drools.io.ResourceFactory;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ComponentPolicyEvaluator
{
  private static final Logger log = LoggerFactory.getLogger(ComponentPolicyEvaluator.class);

  static final Comparator<MatchFact> MATCHES_BY_POLICY_COMPONENT_CONSTRAINT_CONDITION = new Comparator<MatchFact>()
  {
    @Override
    public int compare(final MatchFact lhs, final MatchFact rhs) {
      return index(lhs).compareTo(index(rhs));
    }

    private String index(final MatchFact fact) {
      // doesn't have to be lexically correct, just need to impose consistent ordering
      return fact != null ? fact.getPolicyId() + '|' + fact.getComponent().getDisplayName() + '|'
          + fact.getComponent().getHash() + '|' + fact.getConstraintId() + '|' + fact.getConditionIndex() + '|'
          + asString(fact.getConditionTriggers()) : "";
    }

    private String asString(List<ConditionTrigger> conditionTriggers) {
      return conditionTriggers.stream()
          .map(trigger -> trigger.getConditionIndex() + "|" + JsonUtils.format(trigger.getTrigger()))
          .collect(Collectors.joining("|"));
    }
  };

  private final PolicyWaiverEvaluator waiverEvaluator;

  @Inject
  public ComponentPolicyEvaluator(PolicyWaiverEvaluator waiverEvaluator) {
    this.waiverEvaluator = waiverEvaluator;
  }

  public List<PolicyAlert> evaluate(String ownerId, Stage stage, List<Component> components) {
    return evaluate(ownerId, stage, components, false /* forMonitoring */).getActiveAlerts();
  }

  public PolicyResults evaluate(String ownerId, Stage stage, List<Component> components, boolean forMonitoring) {
    List<Policy> policies = new PolicyDAO().getApplicableByOwnerId(ownerId);
    return evaluate(ownerId, stage, policies, components, forMonitoring);
  }

  // Package visibility for tests only
  PolicyResults evaluate(final String applicationId,
                         final Stage stage,
                         final List<Policy> policies,
                         final List<Component> components)
  {
    return evaluate(applicationId, stage, policies, components, false /* forMonitoring */);
  }

  private PolicyResults evaluate(final String ownerId,
                                 final Stage stage,
                                 final List<Policy> policies,
                                 final List<Component> components,
                                 boolean forMonitoring)
  {
    final long start = System.currentTimeMillis();

    List<MatchFact> facts = evaluateFacts(policies, components);
    PolicyWaiverResults policyWaiverResults = waiverEvaluator.applyWaivers(ownerId, facts);
    PolicyResults policyResults = new PolicyResults();
    toPolicyResults(policies, policyWaiverResults.getActiveFacts(), stage, forMonitoring, policyResults);
    toPolicyResults(policies, policyWaiverResults.getWaivedFacts(), stage, forMonitoring, policyResults);

    log.debug("Evaluated {} policies on {} components in {} ms", policies.size(), components.size(),
        System.currentTimeMillis() - start);

    return policyResults;
  }

  static void toPolicyResults(final List<Policy> policies,
                              final List<MatchFact> matchFacts,
                              final Stage stage,
                              boolean forMonitoring,
                              PolicyResults policyResults)
  {
    // Ordering of matchFacts should result in consistent alerts.
    matchFacts.sort(MATCHES_BY_POLICY_COMPONENT_CONSTRAINT_CONDITION);

    Map<String, Policy> policiesById = policies.stream().collect(Collectors.toMap(Policy::getId, Function.identity()));
    for (MatchFact matchFact : matchFacts) {
      Policy policy = policiesById.get(matchFact.getPolicyId());
      PolicyFact policyFact = new PolicyFact(policy.getId(), policy.getName(), policy.getThreatLevel());

      Component component = matchFact.getComponent();
      ComponentFact componentFact = new ComponentFact(component.getComponentIdentifier(), component.getHash());
      componentFact.addPathnames(component.getPathnames());
      ComponentFactUtil.injectDisplayName(componentFact);

      Constraint constraint = policy.getConstraintById(matchFact.getConstraintId());
      ConstraintFact constraintFact = new ConstraintFact(constraint.getId(), constraint.getName(),
          constraint.getOperator().name());

      List<Condition> conditions = constraint.getConditions();
      int conditionIndex = matchFact.getConditionIndex();
      if (conditionIndex >= 0) {
        ConditionFact conditionFact = createConditionFact(conditions.get(conditionIndex), conditionIndex, matchFact);
        constraintFact.addConditionFact(conditionFact);
      }
      else {
        for (Condition condition : conditions) {
          ConditionFact conditionFact = createConditionFact(condition, conditionIndex, matchFact);
          constraintFact.addConditionFact(conditionFact);
        }
      }

      componentFact.addConstraintFact(constraintFact);
      policyFact.addComponentFact(componentFact);

      Notifications notifications = policy.getNotifications().getApplicable(stage.getStageTypeId(), forMonitoring);
      PolicyNotification policyNotification = new PolicyNotification(policyFact, notifications);
      List<? extends Action> actions = policy.toActions(stage.getStageTypeId(), forMonitoring);
      PolicyAlert policyAlert = new PolicyAlert(policyFact, actions);

      PolicyWaiver policyWaiverForComponentFact = matchFact.getPolicyWaiver();
      if (policyWaiverForComponentFact != null) {
        policyResults.addWaivedAlert(policyAlert);
        policyResults.addPolicyWaiver(componentFact, policyWaiverForComponentFact);
      }
      else {
        policyResults.addActiveAlert(policyAlert);
        policyResults.addActiveNotification(policyNotification);
      }
    }
  }

  public static ConditionFact createConditionFact(Condition condition, int conditionIndex) {
    return createConditionFact(condition, conditionIndex, null /* matchFact */);
  }

  public static ConditionFact createConditionFact(Condition condition,
                                                  int conditionIndex,
                                                  MatchFact matchFact)
  {
    final ConditionType conditionType = ConditionTypes.getById(condition.getConditionTypeId());

    String summary = conditionType.explainCondition(condition);
    String reason = conditionType.explainMatch(condition, matchFact);

    ConditionFact conditionFact = new ConditionFact(condition.getConditionTypeId(), conditionIndex, summary, reason);
    if (matchFact != null && !matchFact.getConditionTriggers().isEmpty()) {
      ConditionTrigger conditionTrigger = matchFact.getConditionTriggerByConditionIndex(condition.getConditionIndex());
      if (conditionTrigger != null) {
        conditionFact.setTriggerJson(JsonUtils.format(conditionTrigger));
      }
    }
    return conditionFact;
  }

  static List<MatchFact> evaluateFacts(final List<Policy> policies, final List<Component> components) {
    final String droolsCode = DroolsGenerator.get(policies);

    final KnowledgeBuilder droolsKnowledgeBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
    droolsKnowledgeBuilder.add(ResourceFactory.newReaderResource(new StringReader(droolsCode)), ResourceType.DRL);
    if (droolsKnowledgeBuilder.hasErrors()) {
      throw new RuntimeException("Failed to load the policies: " + droolsKnowledgeBuilder.getErrors().toString());
    }
    final Collection<KnowledgePackage> droolsKnowledgePackages = droolsKnowledgeBuilder.getKnowledgePackages();
    final KnowledgeBase droolsKnowledgeBase = KnowledgeBaseFactory.newKnowledgeBase();
    droolsKnowledgeBase.addKnowledgePackages(droolsKnowledgePackages);
    final StatefulKnowledgeSession droolsSession = droolsKnowledgeBase.newStatefulKnowledgeSession();

    for (final Component component : components) {
      droolsSession.insert(component);
    }

    droolsSession.fireAllRules();

    return getMatchFacts(droolsSession);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static List<MatchFact> getMatchFacts(StatefulKnowledgeSession droolsSession) {
    return new ArrayList<>((Collection) droolsSession.getObjects(new ObjectFilter()
    {
      @Override
      public boolean accept(final Object object) {
        return object instanceof MatchFact;
      }
    }));
  }
}
