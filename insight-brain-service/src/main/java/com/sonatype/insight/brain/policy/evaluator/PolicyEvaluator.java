/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.sonatype.insight.brain.model.policy.Action;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.facts.ComponentFact;
import com.sonatype.insight.brain.model.policy.facts.ConstraintFact;
import com.sonatype.insight.brain.model.policy.facts.PolicyFact;

public class PolicyEvaluator
{
    private static final Logger log = LoggerFactory.getLogger( PolicyEvaluator.class );

    public List<PolicyAlert> evaluate( final Stage stage, final List<Policy> policies, final List<Component> components )
    {
        final String droolsCode = new DroolsGenerator().generate( stage, policies );
        // Most probably this is too much logging, but it's good for debugging for now
        log.debug( "Generated drools code:\n{}", droolsCode );

        final KnowledgeBuilder droolsKnowledgeBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        droolsKnowledgeBuilder.add( ResourceFactory.newReaderResource( new StringReader( droolsCode ) ),
                                    ResourceType.DRL );
        if ( droolsKnowledgeBuilder.hasErrors() )
        {
            throw new RuntimeException( "Failed to load the policies:" + droolsKnowledgeBuilder.getErrors().toString() );
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

        final Collection<Object> componentFacts = droolsSession.getObjects( new ObjectFilter()
        {
            @Override
            public boolean accept( final Object object )
            {
                return object instanceof ComponentFact;
            }
        } );

        // TODO Aggregate/deduplicate component facts
        final List<PolicyAlert> result = new ArrayList<PolicyAlert>();
        if ( componentFacts == null || componentFacts.isEmpty() )
        {
            return result;
        }

        final Map<String, ConstraintFact> constraintFactsById = new LinkedHashMap<String, ConstraintFact>();

        for ( final Policy policy : policies )
        {
            for ( final Constraint constraint : policy.getConstraints() )
            {
                constraintFactsById.put( constraint.getId(), new ConstraintFact( constraint ) );
            }
        }

        for ( final Object o : componentFacts )
        {
            final ComponentFact componentFact = (ComponentFact) o;
            final ConstraintFact constraintFact = constraintFactsById.get( componentFact.getConstraintId() );
            if ( constraintFact != null )
            {
                constraintFact.addComponentFact( componentFact );
            }
        }

        for ( final Policy policy : policies )
        {
            PolicyFact policyFact = new PolicyFact( policy );
            for ( final Constraint constraint : policy.getConstraints() )
            {
                final ConstraintFact constraintFact = constraintFactsById.get( constraint.getId() );
                if ( constraintFact.getComponentFacts() != null )
                {
                    policyFact.addConstraintFact( constraintFact );
                }
            }
            if ( policyFact.getConstraintFacts() != null )
            {
                final List<Action> actions = policy.getActions( stage.getStageTypeId() );
                if ( actions != null )
                {
                    result.add( new PolicyAlert( policyFact, new ArrayList<Action>( actions ) ) );
                }
            }
        }

        return result;
    }
}
