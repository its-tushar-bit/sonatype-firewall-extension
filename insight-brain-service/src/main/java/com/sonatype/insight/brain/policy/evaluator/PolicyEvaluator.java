/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;

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

public class PolicyEvaluator
{
  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluator.class);

  static final Comparator<MatchFact> MATCHES_BY_POLICY_COMPONENT_CONSTRAINT_CONDITION = new Comparator<MatchFact>()
  {
    @Override
    public int compare(final MatchFact lhs, final MatchFact rhs) {
      return index(lhs).compareTo(index(rhs));
    }

    private String index(final MatchFact fact) {
      // doesn't have to be lexically correct, just need to impose consistent ordering
      return fact != null ? fact.getPolicyId() + '|' + fact.getComponent().getGAV() + '|'
          + fact.getComponent().getHash() + '|' + fact.getConstraintId() + '|' + fact.getConditionNumber() : "";
    }
  };

  private final PolicyWaiverEvaluator waiverEvaluator = new PolicyWaiverEvaluator();

  public List<PolicyAlert> evaluate(String applicationId, Stage stage, PolicyDAO policyDAO, List<Component> components)
  {
    return evaluate(applicationId, stage, policyDAO, components, false /* forMonitoring */).getActiveAlerts();
  }

  public PolicyResults evaluate(String applicationId, Stage stage, PolicyDAO policyDAO, List<Component> components,
      boolean forMonitoring)
  {
    List<Policy> policies = policyDAO.getApplicableByOwnerId(applicationId);
    return evaluate(applicationId, stage, policies, components, forMonitoring);
  }

  // Package visibility for tests only
  List<PolicyAlert> evaluate(final String applicationId, final Stage stage, final List<Policy> policies,
      final List<Component> components)
  {
    return evaluate(applicationId, stage, policies, components, false /* forMonitoring */).getActiveAlerts();
  }

  private PolicyResults evaluate(final String applicationId, final Stage stage, final List<Policy> policies,
      final List<Component> components, boolean forMonitoring)
  {
    final long start = System.currentTimeMillis();

    List<MatchFact> facts = evaluateFacts(applicationId, policies, components);
    PolicyWaiverResults policyWaiverResults = waiverEvaluator.applyWaivers(applicationId, facts);
    PolicyResults policyResults = new PolicyResults();
    policyResults.setActiveAlerts(createAlerts(policies, policyWaiverResults.getActiveFacts(), stage, forMonitoring));
    policyResults.setWaivedAlerts(createAlerts(policies, policyWaiverResults.getWaivedFacts(), stage, forMonitoring));

    log.debug("Evaluated policies in {} millisecs", System.currentTimeMillis() - start);

    return policyResults;
  }

  /**
   * Creates one PolicyAlert for each policy for which there are MatchFacts.
   */
  static List<PolicyAlert> createAlerts(final List<Policy> policies, final List<MatchFact> facts, final Stage stage,
      boolean forMonitoring)
  {
    // Ordering of facts + slicing with LinkedHashMap should = consistent alerts
    Collections.sort(facts, MATCHES_BY_POLICY_COMPONENT_CONSTRAINT_CONDITION);

    final List<PolicyAlert> alerts = new ArrayList<PolicyAlert>();
    for (final Entry<Policy, List<MatchFact>> byPolicy : byPolicy(policies, facts).entrySet()) {
      final Policy policy = byPolicy.getKey();
      final PolicyFact policyFact = new PolicyFact(policy.getId(), policy.getName(), policy.getThreatLevel());
      for (final Entry<Component, List<MatchFact>> byComponent : byComponent(byPolicy.getValue()).entrySet()) {
        final Component component = byComponent.getKey();
        final ComponentFact componentFact = new ComponentFact(component.getGroupId(), component.getArtifactId(),
            component.getVersion(), component.getHash());
        for (final Entry<Constraint, List<MatchFact>> byConstraints : byConstraint(policy.getConstraints(),
            byComponent.getValue()).entrySet()) {
          final Constraint constraint = byConstraints.getKey();
          final ConstraintFact constraintFact = new ConstraintFact(constraint.getId(), constraint.getName(), constraint
              .getOperator().name());
          for (final MatchFact fact : byConstraints.getValue()) {
            final List<Condition> conditions = constraint.getConditions();
            final int num = fact.getConditionNumber();
            if (num >= 0) {
              constraintFact.addConditionFact(createConditionFact(conditions.get(num), component));
            }
            else {
              for (final Condition condition : conditions) {
                constraintFact.addConditionFact(createConditionFact(condition, component));
              }
            }
          }
          if (!constraintFact.getConditionFacts().isEmpty()) {
            componentFact.addConstraintFact(constraintFact);
          }
        }
        if (!componentFact.getConstraintFacts().isEmpty()) {
          policyFact.addComponentFact(componentFact);
        }
      }
      if (!policyFact.getComponentFacts().isEmpty()) {
        List<? extends Action> actions;
        if (forMonitoring) {
          actions = policy.getMonitorNotifyActions();
        }
        else {
          actions = policy.getActions(stage.getStageTypeId());
        }
        alerts.add(new PolicyAlert(policyFact, actions));
      }
    }
    return alerts;
  }

  public static ConditionFact createConditionFact(Condition condition, Component component) {
    final ConditionType<?> conditionType = ConditionTypes.getById(condition.getConditionTypeId());

    String summary = conditionType.explainCondition(condition);
    String reason = conditionType.explainMatch(condition, component);

    return new ConditionFact(condition.getConditionTypeId(), summary, reason);
  }

  private static Map<Policy, List<MatchFact>> byPolicy(final List<Policy> policies, final List<MatchFact> facts) {
    final Map<String, Policy> policiesById = new HashMap<String, Policy>();
    for (final Policy policy : policies) {
      policiesById.put(policy.getId(), policy);
    }
    final Map<Policy, List<MatchFact>> byPolicy = new LinkedHashMap<Policy, List<MatchFact>>();
    for (final MatchFact fact : facts) {
      final Policy policy = policiesById.get(fact.getPolicyId());
      List<MatchFact> partition = byPolicy.get(policy);
      if (partition == null) {
        byPolicy.put(policy, partition = new ArrayList<MatchFact>());
      }
      partition.add(fact);
    }
    return byPolicy;
  }

  private static Map<Constraint, List<MatchFact>> byConstraint(final List<Constraint> constraints,
      final List<MatchFact> facts)
  {
    final Map<String, Constraint> constraintsById = new HashMap<String, Constraint>();
    for (final Constraint constraint : constraints) {
      constraintsById.put(constraint.getId(), constraint);
    }
    final Map<Constraint, List<MatchFact>> byConstraint = new LinkedHashMap<Constraint, List<MatchFact>>();
    for (final MatchFact fact : facts) {
      final Constraint constraint = constraintsById.get(fact.getConstraintId());
      List<MatchFact> partition = byConstraint.get(constraint);
      if (partition == null) {
        byConstraint.put(constraint, partition = new ArrayList<MatchFact>());
      }
      partition.add(fact);
    }
    return byConstraint;
  }

  private static Map<Component, List<MatchFact>> byComponent(final List<MatchFact> facts) {
    final Map<Component, List<MatchFact>> byComponent = new LinkedHashMap<Component, List<MatchFact>>();
    for (final MatchFact fact : facts) {
      List<MatchFact> partition = byComponent.get(fact.getComponent());
      if (partition == null) {
        byComponent.put(fact.getComponent(), partition = new ArrayList<MatchFact>());
      }
      partition.add(fact);
    }
    return byComponent;
  }

  static List<MatchFact> evaluateFacts(final String applicationId, final List<Policy> policies,
      final List<Component> components)
  {
    final String droolsCode = new DroolsGenerator().generate(applicationId, policies);
    // Most probably this is too much logging, but it's good for debugging for now
    log.debug("Generated drools code:\n{}", droolsCode);

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
    return new ArrayList<MatchFact>((Collection) droolsSession.getObjects(new ObjectFilter()
    {
      @Override
      public boolean accept(final Object object) {
        return object instanceof MatchFact;
      }
    }));
  }
}
