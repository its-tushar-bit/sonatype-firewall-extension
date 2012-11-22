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
import com.sonatype.insight.brain.model.rule.LicenseCategoryConditionType;
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
        Rule rule2 = new Rule();
        rule2.setId( "RuleId2" );
        rule2.setName( "Rule Name 2" );
        rule2.setOperator( LogicalOperator.AND );
        SimpleCondition condition2 = new SimpleCondition();
        condition2.setConditionTypeId( LicenseCategoryConditionType.ID );
        condition2.setOperator( "is" );
        condition2.setValue( "Weak Copyleft" );
        rule2.addCondition( condition2 );
        action = new Action();
        action.setActionTypeId( MarkAsFailedActionType.ID );
        rule2.addAction( action );
        rules.add( rule2 );

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
        // A component with license threat "Weak Copyleft"
        Component component2 = new Component();
        component2.setGroupId( "PolicyEvaluatorTest_G2" );
        component2.setArtifactId( "PolicyEvaluatorTest_A2" );
        component2.setVersion( "PolicyEvaluatorTest_V2" );
        component2.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component2 );
        // Evaluate the policy
        List<PolicyFact> policyFacts = new PolicyEvaluator().evaluate( rules, components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 2, policyFacts.size() );
        assertContainsPolicyFact( component1, "RuleId1", "Rule Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component2, "RuleId2", "Rule Name 2", MarkAsFailedActionType.ID, policyFacts );
    }

    private void assertContainsPolicyFact( Component expectedComponent, String expectedRuleId, String expectedRuleName,
                                           String expectedKind, List<PolicyFact> actual )
    {
        for ( PolicyFact actualPolicyFact : actual )
        {
            if ( expectedComponent == actualPolicyFact.getComponent()
                && expectedRuleId.equals( actualPolicyFact.getRuleId() )
                && expectedRuleName.equals( actualPolicyFact.getRuleName() )
                && expectedKind.equals( actualPolicyFact.getKind() ) )
            {
                return;
            }
        }

        Assert.fail();
    }
}
