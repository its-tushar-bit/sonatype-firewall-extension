/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.definition.KnowledgePackage;
import org.drools.io.ResourceFactory;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.PolicyFact;
import com.sonatype.insight.brain.model.rule.Rule;

public class PolicyEvaluator
{
    public List<PolicyFact> evaluate( List<Rule> rules, List<Component> components )
    {
        String droolsCode = new DroolsGenerator().generate( rules );

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
        for ( Object o : policyFacts )
        {
            result.add( (PolicyFact) o );
        }
        return result;
    }
}
