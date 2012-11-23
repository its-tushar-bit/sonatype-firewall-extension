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
import com.sonatype.insight.brain.model.component.PolicyFact;
import com.sonatype.insight.brain.model.rule.Rule;

public class PolicyEvaluator
{
    private static final Logger log = LoggerFactory.getLogger( PolicyEvaluator.class );

    public List<PolicyFact> evaluate( List<Rule> rules, List<Component> components )
    {
        String droolsCode = new DroolsGenerator().generate( rules );
        // Most probably this is too much logging, but it's good for debugging for now
        log.debug( "Generated drools code:\n{}", droolsCode );

        KnowledgeBuilder droolsKnowledgeBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        droolsKnowledgeBuilder.add( ResourceFactory.newReaderResource( new StringReader( droolsCode ) ),
                                    ResourceType.DRL );
        if ( droolsKnowledgeBuilder.hasErrors() )
        {
            throw new RuntimeException( "Failed to load the rules:" + droolsKnowledgeBuilder.getErrors().toString() );
        }
        Collection<KnowledgePackage> droolsKnowledgePackages = droolsKnowledgeBuilder.getKnowledgePackages();
        KnowledgeBase droolsKnowledgeBase = KnowledgeBaseFactory.newKnowledgeBase();
        droolsKnowledgeBase.addKnowledgePackages( droolsKnowledgePackages );
        StatefulKnowledgeSession droolsSession = droolsKnowledgeBase.newStatefulKnowledgeSession();

        for ( Component component : components )
        {
            droolsSession.insert( component );
        }
        droolsSession.fireAllRules();
        Collection<Object> policyFacts = droolsSession.getObjects( new ObjectFilter()
        {
            @Override
            public boolean accept( Object object )
            {
                return ( object instanceof PolicyFact );
            }
        } );

        // TODO Aggregate/deduplicate policy facts
        List<PolicyFact> result = new ArrayList<PolicyFact>();
        if ( policyFacts == null || policyFacts.isEmpty() )
        {
            return result;
        }

        Map<String, Rule> rulesById = new LinkedHashMap<String, Rule>();
        for ( Rule rule : rules )
        {
            rulesById.put( rule.getId(), rule );
        }
        for ( Object o : policyFacts )
        {
            PolicyFact policyFact = (PolicyFact) o;
            policyFact.setRuleName( rulesById.get( policyFact.getRuleId() ).getName() );
            result.add( policyFact );
        }
        return result;
    }
}
