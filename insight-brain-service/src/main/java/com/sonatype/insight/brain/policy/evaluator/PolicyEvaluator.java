/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.facts.ComponentFact;
import com.sonatype.insight.brain.model.policy.facts.ConditionFact;
import com.sonatype.insight.brain.model.policy.facts.ConstraintFact;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.policy.facts.PolicyFact;

public class PolicyEvaluator
{
    private static final Logger log = LoggerFactory.getLogger( PolicyEvaluator.class );

    public List<PolicyAlert> evaluate( final String applicationId, final Stage stage, final List<Policy> policies,
                                       final List<Component> components )
    {
        long start = System.currentTimeMillis();

        final List<MatchFact> facts = evaluateFacts( applicationId, policies, components );
        final List<PolicyAlert> alerts = createAlerts( policies, facts, stage );

        log.debug( "Evaluated policies in {} millisecs", System.currentTimeMillis() - start );

        return alerts;
    }

    private static List<PolicyAlert> createAlerts( final List<Policy> policies, final List<MatchFact> facts,
                                                   final Stage stage )
    {
        final List<PolicyAlert> alerts = new ArrayList<PolicyAlert>();
        for ( final Entry<Policy, List<MatchFact>> byPolicy : byPolicy( policies, facts ).entrySet() )
        {
            final Policy policy = byPolicy.getKey();
            final PolicyFact policyFact = new PolicyFact( policy );
            for ( final Entry<Component, List<MatchFact>> byComponent : byComponent( byPolicy.getValue() ).entrySet() )
            {
                final Component component = byComponent.getKey();
                final ComponentFact componentFact = new ComponentFact( component );
                for ( final Entry<Constraint, List<MatchFact>> byConstraints : byConstraint( policy.getConstraints(),
                                                                                             byComponent.getValue() ).entrySet() )
                {
                    final Constraint constraint = byConstraints.getKey();
                    final ConstraintFact constraintFact = new ConstraintFact( constraint );
                    for ( final MatchFact fact : byConstraints.getValue() )
                    {
                        final int num = fact.getConditionNumber();
                        if ( num >= 0 )
                        {
                            final Condition condition = constraint.getConditions().get( num );
                            constraintFact.addConditionFact( new ConditionFact( condition, component ) );
                        }
                        else
                        {
                            for ( final Condition condition : constraint.getConditions() )
                            {
                                constraintFact.addConditionFact( new ConditionFact( condition, component ) );
                            }
                        }
                    }
                    componentFact.addConstraintFact( constraintFact );
                }
                policyFact.addComponentFact( componentFact );
            }
            alerts.add( new PolicyAlert( policyFact, policy.getActions( stage.getStageTypeId() ) ) );
        }
        return alerts;
    }

    private static Map<Policy, List<MatchFact>> byPolicy( final List<Policy> policies, final List<MatchFact> facts )
    {
        final Map<String, Policy> policiesById = new HashMap<String, Policy>();
        for ( final Policy policy : policies )
        {
            policiesById.put( policy.getId(), policy );
        }
        final Map<Policy, List<MatchFact>> byPolicy = new HashMap<Policy, List<MatchFact>>();
        for ( final MatchFact fact : facts )
        {
            final Policy policy = policiesById.get( fact.getPolicyId() );
            List<MatchFact> partition = byPolicy.get( policy );
            if ( partition == null )
            {
                byPolicy.put( policy, partition = new ArrayList<MatchFact>() );
            }
            partition.add( fact );
        }
        return byPolicy;
    }

    private static Map<Constraint, List<MatchFact>> byConstraint( final List<Constraint> constraints,
                                                                  final List<MatchFact> facts )
    {
        final Map<String, Constraint> constraintsById = new HashMap<String, Constraint>();
        for ( final Constraint constraint : constraints )
        {
            constraintsById.put( constraint.getId(), constraint );
        }
        final Map<Constraint, List<MatchFact>> byConstraint = new HashMap<Constraint, List<MatchFact>>();
        for ( final MatchFact fact : facts )
        {
            final Constraint constraint = constraintsById.get( fact.getConstraintId() );
            List<MatchFact> partition = byConstraint.get( constraint );
            if ( partition == null )
            {
                byConstraint.put( constraint, partition = new ArrayList<MatchFact>() );
            }
            partition.add( fact );
        }
        return byConstraint;
    }

    private static Map<Component, List<MatchFact>> byComponent( final List<MatchFact> facts )
    {
        final Map<Component, List<MatchFact>> byComponent = new IdentityHashMap<Component, List<MatchFact>>();
        for ( final MatchFact fact : facts )
        {
            List<MatchFact> partition = byComponent.get( fact.getComponent() );
            if ( partition == null )
            {
                byComponent.put( fact.getComponent(), partition = new ArrayList<MatchFact>() );
            }
            partition.add( fact );
        }
        return byComponent;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private static List<MatchFact> evaluateFacts( final String applicationId, final List<Policy> policies,
                                                  final List<Component> components )
    {
        final String droolsCode = new DroolsGenerator().generate( applicationId, policies );
        // Most probably this is too much logging, but it's good for debugging for now
        log.debug( "Generated drools code:\n{}", droolsCode );

        final KnowledgeBuilder droolsKnowledgeBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        droolsKnowledgeBuilder.add( ResourceFactory.newReaderResource( new StringReader( droolsCode ) ),
                                    ResourceType.DRL );
        if ( droolsKnowledgeBuilder.hasErrors() )
        {
            throw new RuntimeException( "Failed to load the policies: " + droolsKnowledgeBuilder.getErrors().toString() );
        }
        final Collection<KnowledgePackage> droolsKnowledgePackages = droolsKnowledgeBuilder.getKnowledgePackages();
        final KnowledgeBase droolsKnowledgeBase = KnowledgeBaseFactory.newKnowledgeBase();
        droolsKnowledgeBase.addKnowledgePackages( droolsKnowledgePackages );
        final StatefulKnowledgeSession droolsSession = droolsKnowledgeBase.newStatefulKnowledgeSession();

        for ( final Component component : components )
        {
            droolsSession.insert( component );
        }

        droolsSession.fireAllRules();

        return new ArrayList<MatchFact>( (Collection) droolsSession.getObjects( new ObjectFilter()
        {
            @Override
            public boolean accept( final Object object )
            {
                return object instanceof MatchFact;
            }
        } ) );
    }
}
