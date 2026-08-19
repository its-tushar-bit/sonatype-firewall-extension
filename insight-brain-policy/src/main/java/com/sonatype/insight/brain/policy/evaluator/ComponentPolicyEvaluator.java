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
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Owner;
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
import com.sonatype.insight.brain.policy.comparison.ConstraintFactsListComparator;
import com.sonatype.insight.brain.utils.ComponentFactUtil;
import com.sonatype.insight.brain.utils.FirewallForContainerImagesHelper;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.ObjectFilter;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ComponentPolicyEvaluator
{
  private static final Logger log = LoggerFactory.getLogger(ComponentPolicyEvaluator.class);

  // Tenant safe because each key contains multiple UUIDs in specific orders unique to each tenant, see CLM-27348
  private static final LoadingCache<String, Object> droolsCodeKiaBase = CacheBuilder.newBuilder()
      .concurrencyLevel(20)
      .expireAfterAccess(24, TimeUnit.HOURS)
      .build(CacheLoader.from(Object::new));

  static final Comparator<PolicyFact> POLICY_FACT_COMPARATOR = new Comparator<>()
  {
    @Override
    public int compare(final PolicyFact policyFact1, final PolicyFact policyFact2) {
      int result = policyFact1.getPolicyId().compareTo(policyFact2.getPolicyId());
      if (result != 0) {
        return result;
      }

      ComponentFact componentFact1 = policyFact1.getComponentFacts().get(0);
      String componentName1 = Objects.toString(componentFact1.getDisplayName());
      ComponentFact componentFact2 = policyFact2.getComponentFacts().get(0);
      String componentName2 = Objects.toString(componentFact2.getDisplayName());
      result = componentName1.compareTo(componentName2);
      if (result != 0) {
        return result;
      }

      return ConstraintFactsListComparator.CONSTRAINT_FACTS_LIST_COMPARATOR.compare(componentFact1.getConstraintFacts(),
          componentFact2.getConstraintFacts());
    }
  };

  private final PolicyWaiverDAO policyWaiverDAO;

  private final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

  private final FirewallForContainerImagesHelper firewallForContainerImagesHelper;

  @Inject
  public ComponentPolicyEvaluator(
      final PolicyWaiverDAO policyWaiverDAO,
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
      final FirewallForContainerImagesHelper firewallForContainerImagesHelper)
  {
    this.policyWaiverDAO = policyWaiverDAO;
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.firewallForContainerImagesHelper = firewallForContainerImagesHelper;
  }

  public List<PolicyAlert> evaluate(String ownerId, Stage stage, List<Component> components) {
    return evaluate(ownerId, stage, components, false /* forMonitoring */).getActiveAlerts();
  }

  public PolicyResults evaluate(String ownerId, Stage stage, List<Component> components, boolean forMonitoring) {
    List<Policy> policies = policyDAO.getApplicableByOwnerIdWithHierarchy(ownerId);
    return evaluate(ownerId, stage, policies, components, forMonitoring);
  }

  // Package visibility for tests only
  PolicyResults evaluate(
      final String applicationId,
      final Stage stage,
      final List<Policy> policies,
      final List<Component> components)
  {
    return evaluate(applicationId, stage, policies, components, false /* forMonitoring */);
  }

  public PolicyResults evaluate(
      final String ownerId,
      final Stage stage,
      final List<Policy> policies,
      final List<Component> components,
      boolean forMonitoring)
  {
    final long start = System.currentTimeMillis();

    List<MatchFact> matchFacts = evaluateFacts(policies, components);
    List<PolicyFact> policyFacts = toPolicyFacts(policies, matchFacts);
    PolicyResults policyResults = toPolicyResults(ownerId, policies, policyFacts, stage, forMonitoring);

    log.debug("Evaluated {} policies on {} components in {} ms", policies.size(), components.size(),
        System.currentTimeMillis() - start);

    return policyResults;
  }

  PolicyResults toPolicyResults(
      String ownerId,
      final List<Policy> policies,
      final List<PolicyFact> policyFacts,
      final Stage stage,
      boolean forMonitoring)
  {
    // Ordering of policyFacts should result in consistent alerts.
    policyFacts.sort(POLICY_FACT_COMPARATOR);

    PolicyResults policyResults = new PolicyResults();

    Owner owner = ownerDAO.getById(ownerId);
    List<String> ownerIdsForPolicies =
        firewallForContainerImagesHelper.getApplicableOwnersForPolicies(stage.getStageTypeId(), owner);

    Map<String, Policy> policiesById = policies.stream().collect(Collectors.toMap(Policy::getId, Function.identity()));
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getActiveApplicableByOwnerId(ownerId);
    for (PolicyFact policyFact : policyFacts) {
      Policy policy = policiesById.get(policyFact.getPolicyId());

      Notifications notifications =
          policy.getEffectiveNotifications(ownerIdsForPolicies).getApplicable(stage.getStageTypeId(), forMonitoring);
      PolicyNotification policyNotification = new PolicyNotification(policyFact, notifications);
      List<Action> actions = policy.toActions(stage.getStageTypeId(), forMonitoring, ownerIdsForPolicies);
      PolicyAlert policyAlert = new PolicyAlert(policyFact, actions);

      PolicyWaiver policyWaiver = getApplicablePolicyWaiver(policyWaivers, policyFact);
      if (policyWaiver != null) {
        policyResults.addWaivedAlert(policyAlert);
        policyResults.addPolicyWaiver(policyFact.getComponentFacts().get(0), policyWaiver);
      }
      else {
        policyResults.addActiveAlert(policyAlert);
        policyResults.addActiveNotification(policyNotification);
      }
    }

    return policyResults;
  }

  private static PolicyWaiver getApplicablePolicyWaiver(List<PolicyWaiver> policyWaivers, PolicyFact policyFact) {
    PolicyWaiver legacyWaiver = null;

    for (PolicyWaiver policyWaiver : policyWaivers) {
      if (policyWaiver.isForContainerImage()) {
        continue;
      }
      PolicyWaiverMatcherWrapper policyWaiverMatcher = new PolicyWaiverMatcherWrapper(policyWaiver);
      if (policyWaiverMatcher.matchesPolicyId(policyFact.getPolicyId())) {
        ComponentFact mainComponentFact = policyFact.getComponentFacts().get(0);
        if (policyWaiverMatcher.matchesComponent(mainComponentFact)) {
          if (policyWaiverMatcher.isLegacyWaiver()) {
            // This is a legacy waiver (before Brain 1.53). It matches the policy fact, but there may be a more specific
            // waiver.
            legacyWaiver = policyWaiver;
          }
          else if (policyWaiverMatcher.matchesConstraintFacts(mainComponentFact.getConstraintFacts())) {
            return policyWaiver;
          }
        }
      }
    }

    return legacyWaiver;
  }

  // Visible for tests
  static List<PolicyFact> toPolicyFacts(List<Policy> policies, List<MatchFact> matchFacts) {
    List<PolicyFact> policyFacts = new ArrayList<>();

    Map<String, Policy> policiesById = policies.stream().collect(Collectors.toMap(Policy::getId, Function.identity()));
    for (MatchFact matchFact : matchFacts) {
      Policy policy = policiesById.get(matchFact.getPolicyId());
      policyFacts.add(toPolicyFact(policy, matchFact));
    }

    return policyFacts;
  }

  private static PolicyFact toPolicyFact(Policy policy, MatchFact matchFact) {
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
      ConditionFact conditionFact = createConditionFact(conditions.get(conditionIndex), matchFact);
      constraintFact.addConditionFact(conditionFact);
    }
    else {
      for (Condition condition : conditions) {
        ConditionFact conditionFact = createConditionFact(condition, matchFact);
        constraintFact.addConditionFact(conditionFact);
      }
    }

    componentFact.addConstraintFact(constraintFact);
    policyFact.addComponentFact(componentFact);

    return policyFact;
  }

  public static ConditionFact createConditionFact(Condition condition, MatchFact matchFact) {
    final ConditionType conditionType = ConditionTypes.getById(condition.getConditionTypeId());

    String summary = conditionType.explainCondition(condition);
    String reason = conditionType.explainMatch(condition, matchFact);
    TriggerReference reference = conditionType.getTriggerReference(condition, matchFact);

    ConditionFact conditionFact = new ConditionFact(condition.getConditionTypeId(), condition.getConditionIndex(),
        summary, reason, reference);
    if (matchFact != null && !matchFact.getConditionTriggers().isEmpty()) {
      ConditionTrigger conditionTrigger = matchFact.getConditionTriggerByConditionIndex(condition.getConditionIndex());
      if (conditionTrigger != null) {
        conditionFact.setTriggerJson(JsonUtils.writeUnformatted(conditionTrigger));
      }
    }
    return conditionFact;
  }

  static List<MatchFact> evaluateFacts(final List<Policy> policies, final List<Component> components) {
    policies.sort(Comparator.comparing(Policy::getId));
    final String droolsCode = DroolsGenerator.get(policies);

    KieBase kieBase;
    try {
      kieBase = (KieBase) droolsCodeKiaBase.get(droolsCode, () -> {
        log.debug("KieBase cache miss. Loading KieBase to cache for policies.");
        KnowledgeBuilder droolsKnowledgeBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        droolsKnowledgeBuilder.add(ResourceFactory.newReaderResource(new StringReader(droolsCode)), ResourceType.DRL);
        if (droolsKnowledgeBuilder.hasErrors()) {
          throw new RuntimeException("Failed to load the policies: " + droolsKnowledgeBuilder.getErrors().toString());
        }
        return droolsKnowledgeBuilder.newKieBase();
      });
    }
    catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
    final KieSession droolsSession = kieBase.newKieSession();
    try {
      for (final Component component : components) {
        droolsSession.insert(component);
      }

      droolsSession.fireAllRules();

      List<MatchFact> matchFacts = getMatchFacts(droolsSession);
      return matchFacts;
    }
    finally {
      droolsSession.dispose();
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static List<MatchFact> getMatchFacts(KieSession droolsSession) {
    return new ArrayList<>((Collection) droolsSession.getObjects(new ObjectFilter()
    {
      @Override
      public boolean accept(final Object object) {
        return object instanceof MatchFact;
      }
    }));
  }
}
