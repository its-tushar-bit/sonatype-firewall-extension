/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.PolicyFact;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.rule.Action;
import com.sonatype.insight.brain.model.rule.LogicalOperator;
import com.sonatype.insight.brain.model.rule.MarkAsFailedActionType;
import com.sonatype.insight.brain.model.rule.Rule;
import com.sonatype.insight.brain.model.rule.SecurityVulnerabilityPresentType;
import com.sonatype.insight.brain.model.rule.SimpleCondition;

public class PolicyEvaluatorTest
{
    // TODO We need a lot more tests here

    @Test
    public void testEvaluate()
    {
        // Create policy rules
        List<Rule> rules = new ArrayList<Rule>();
        Rule rule1 = new Rule();
        rule1.setId( "RuleId1" );
        rule1.setName( "Rule Name 1" );
        rule1.setOperator( LogicalOperator.AND );
        SimpleCondition condition1 = new SimpleCondition();
        condition1.setConditionTypeId( SecurityVulnerabilityPresentType.ID );
        condition1.setOperator( "present" );
        rule1.addCondition( condition1 );
        Action action = new Action();
        action.setActionTypeId( MarkAsFailedActionType.ID );
        rule1.addAction( action );
        rules.add( rule1 );

        List<Component> components = new ArrayList<Component>();
        // A component with one security vulnerability
        Component component1 = new Component();
        component1.setGroupId( "PolicyEvaluatorTest_G1" );
        component1.setArtifactId( "PolicyEvaluatorTest_A1" );
        component1.setVersion( "PolicyEvaluatorTest_V1" );
        SecurityVulnerability securityVulnerability = new SecurityVulnerability();
        securityVulnerability.setRefId( "PolicyEvaluatorTest_SecVuln1" );
        securityVulnerability.setSource( "osvdb" );
        securityVulnerability.setScore( 3F );
        component1.addSecurityVulnerability( securityVulnerability );
        components.add( component1 );
        // A component without security vulnerabilities
        Component component2 = new Component();
        component2.setGroupId( "PolicyEvaluatorTest_G2" );
        component2.setArtifactId( "PolicyEvaluatorTest_A2" );
        component2.setVersion( "PolicyEvaluatorTest_V2" );
        components.add( component2 );
        // Evaluate the policy
        List<PolicyFact> policyFacts = new PolicyEvaluator().evaluate( rules, components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 1, policyFacts.size() );
        PolicyFact policyFact = policyFacts.get( 0 );
        Assert.assertEquals( component1, policyFact.getComponent() );
        Assert.assertEquals( "RuleId1", policyFact.getRuleId() );
        Assert.assertEquals( "Rule Name 1", policyFact.getRuleName() );
        Assert.assertEquals( MarkAsFailedActionType.ID, policyFact.getKind() );
    }
}
