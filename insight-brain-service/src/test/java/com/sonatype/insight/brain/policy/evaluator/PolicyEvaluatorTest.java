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
import com.sonatype.insight.brain.model.rule.SecurityVulnerabilityPresentConditionType;
import com.sonatype.insight.brain.model.rule.SimpleCondition;

public class PolicyEvaluatorTest
{
    // TODO We need a lot more tests here

    @Test
    public void testEvaluate_TwoRulesWithSimpleConditions()
    {
        // Create policy rules
        final List<Rule> rules = new ArrayList<Rule>();
        final Rule rule1 = new Rule( "RuleId1", "Rule Name 1", LogicalOperator.AND );
        rule1.addCondition( new SimpleCondition( SecurityVulnerabilityPresentConditionType.ID, "present" ) );
        rule1.addAction( new Action( MarkAsFailedActionType.ID ) );
        rules.add( rule1 );
        final Rule rule2 = new Rule( "RuleId2", "Rule Name 2", LogicalOperator.AND );
        rule2.addCondition( new SimpleCondition( LicenseCategoryConditionType.ID, "is", "Weak Copyleft" ) );
        rule2.addAction( new Action( MarkAsFailedActionType.ID ) );
        rules.add( rule2 );

        final List<Component> components = new ArrayList<Component>();
        // A component with one security vulnerability
        final Component component1 = new Component( "g1", "a1", "v1" );
        component1.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv1", 3F ) );
        components.add( component1 );
        // A component with license threat "Weak Copyleft"
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component2 );
        // Evaluate the policy
        final List<PolicyFact> policyFacts = new PolicyEvaluator().evaluate( rules, components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 2, policyFacts.size() );
        assertContainsPolicyFact( component1, "RuleId1", "Rule Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component2, "RuleId2", "Rule Name 2", MarkAsFailedActionType.ID, policyFacts );
    }

    @Test
    public void testEvaluate_SecurityVulnerabilityPresentConditionType()
    {
        // Create policy rules
        final List<Rule> rules = new ArrayList<Rule>();
        final Rule ruleSVPresent = new Rule( "RuleIdSVPresent", "Rule Name SVPresent", LogicalOperator.AND );
        ruleSVPresent.addCondition( new SimpleCondition( SecurityVulnerabilityPresentConditionType.ID, "present" ) );
        ruleSVPresent.addAction( new Action( MarkAsFailedActionType.ID ) );
        rules.add( ruleSVPresent );
        final Rule ruleSVAbsent = new Rule( "RuleIdSVAbsent", "Rule Name SVAbsent", LogicalOperator.AND );
        ruleSVAbsent.addCondition( new SimpleCondition( SecurityVulnerabilityPresentConditionType.ID, "absent" ) );
        ruleSVAbsent.addAction( new Action( MarkAsFailedActionType.ID ) );
        rules.add( ruleSVAbsent );

        final List<Component> components = new ArrayList<Component>();
        // A component without security vulnerabilities
        final Component component1 = new Component( "g1", "a1", "v1" );
        components.add( component1 );
        // A component with one security vulnerability
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv2", 3F ) );
        components.add( component2 );
        // Evaluate the policy
        final List<PolicyFact> policyFacts = new PolicyEvaluator().evaluate( rules, components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 2, policyFacts.size() );
        assertContainsPolicyFact( component1, "RuleIdSVAbsent", "Rule Name SVAbsent", MarkAsFailedActionType.ID,
                                  policyFacts );
        assertContainsPolicyFact( component2, "RuleIdSVPresent", "Rule Name SVPresent", MarkAsFailedActionType.ID,
                                  policyFacts );
    }

    @Test
    public void testEvaluate_LicenseCategoryConditionType()
    {
        // Create policy rules
        final List<Rule> rules = new ArrayList<Rule>();
        final Rule ruleSVIs = new Rule( "RuleIdIs", "Rule Name Is", LogicalOperator.AND );
        ruleSVIs.addCondition( new SimpleCondition( LicenseCategoryConditionType.ID, "is", "Weak Copyleft" ) );
        ruleSVIs.addAction( new Action( MarkAsFailedActionType.ID ) );
        rules.add( ruleSVIs );
        final Rule ruleSVIsNot = new Rule( "RuleIdIsNot", "Rule Name IsNot", LogicalOperator.AND );
        ruleSVIsNot.addCondition( new SimpleCondition( LicenseCategoryConditionType.ID, "is not", "Weak Copyleft" ) );
        ruleSVIsNot.addAction( new Action( MarkAsFailedActionType.ID ) );
        rules.add( ruleSVIsNot );

        final List<Component> components = new ArrayList<Component>();
        final Component component1 = new Component( "g1", "a1", "v1" );
        component1.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component1 );
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.setLicenseThreat( "LIBERAL" );
        components.add( component2 );
        // Evaluate the policy
        final List<PolicyFact> policyFacts = new PolicyEvaluator().evaluate( rules, components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 2, policyFacts.size() );
        assertContainsPolicyFact( component1, "RuleIdIs", "Rule Name Is", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component2, "RuleIdIsNot", "Rule Name IsNot", MarkAsFailedActionType.ID, policyFacts );
    }

    @Test
    public void testEvaluate_OneRuleWithCompositeConditionAll()
    {
        // Create policy rules
        final List<Rule> rules = new ArrayList<Rule>();
        final Rule rule1 = new Rule( "RuleId1", "Rule Name 1", LogicalOperator.AND );
        rule1.addCondition( new SimpleCondition( SecurityVulnerabilityPresentConditionType.ID, "present" ) );
        rule1.addCondition( new SimpleCondition( LicenseCategoryConditionType.ID, "is", "Weak Copyleft" ) );
        rule1.addAction( new Action( MarkAsFailedActionType.ID ) );
        rules.add( rule1 );

        final List<Component> components = new ArrayList<Component>();
        // A component with one security vulnerability
        final Component component1 = new Component( "g1", "a1", "v1" );
        component1.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv1", 3F ) );
        components.add( component1 );
        // Evaluate the policy
        List<PolicyFact> policyFacts = new PolicyEvaluator().evaluate( rules, components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 0, policyFacts.size() );
        // A component with license threat "Weak Copyleft"
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component2 );
        // Evaluate the policy
        policyFacts = new PolicyEvaluator().evaluate( rules, components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 0, policyFacts.size() );
        // A component with one security vulnerability and license threat "Weak Copyleft"
        final Component component3 = new Component( "g3", "a3", "v3" );
        component3.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv2", 3F ) );
        component3.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component3 );
        // Evaluate the policy
        policyFacts = new PolicyEvaluator().evaluate( rules, components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 1, policyFacts.size() );
        assertContainsPolicyFact( component3, "RuleId1", "Rule Name 1", MarkAsFailedActionType.ID, policyFacts );
        // Another component with one security vulnerability and license threat "Weak Copyleft"
        final Component component4 = new Component( "g4", "a4", "v4" );
        component4.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv4", 3F ) );
        component4.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component4 );
        // Evaluate the policy
        policyFacts = new PolicyEvaluator().evaluate( rules, components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 2, policyFacts.size() );
        assertContainsPolicyFact( component3, "RuleId1", "Rule Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component4, "RuleId1", "Rule Name 1", MarkAsFailedActionType.ID, policyFacts );
    }

    @Test
    public void testEvaluate_OneRuleWithCompositeConditionAny()
    {
        // Create policy rules
        final List<Rule> rules = new ArrayList<Rule>();
        final Rule rule1 = new Rule( "RuleId1", "Rule Name 1", LogicalOperator.OR );
        rule1.addCondition( new SimpleCondition( SecurityVulnerabilityPresentConditionType.ID, "present" ) );
        rule1.addCondition( new SimpleCondition( LicenseCategoryConditionType.ID, "is", "Weak Copyleft" ) );
        rule1.addAction( new Action( MarkAsFailedActionType.ID ) );
        rules.add( rule1 );

        final List<Component> components = new ArrayList<Component>();
        // A component with one security vulnerability
        final Component component1 = new Component( "g1", "a1", "v1" );
        component1.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv1", 3F ) );
        components.add( component1 );
        // Evaluate the policy
        List<PolicyFact> policyFacts = new PolicyEvaluator().evaluate( rules, components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 1, policyFacts.size() );
        assertContainsPolicyFact( component1, "RuleId1", "Rule Name 1", MarkAsFailedActionType.ID, policyFacts );
        // A component with license threat "Weak Copyleft"
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component2 );
        // Evaluate the policy
        policyFacts = new PolicyEvaluator().evaluate( rules, components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 2, policyFacts.size() );
        assertContainsPolicyFact( component1, "RuleId1", "Rule Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component2, "RuleId1", "Rule Name 1", MarkAsFailedActionType.ID, policyFacts );
        // A component with one security vulnerability and license threat "Weak Copyleft"
        final Component component3 = new Component( "g3", "a3", "v3" );
        component3.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv2", 3F ) );
        component3.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component3 );
        // Evaluate the policy
        policyFacts = new PolicyEvaluator().evaluate( rules, components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 3, policyFacts.size() );
        assertContainsPolicyFact( component1, "RuleId1", "Rule Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component2, "RuleId1", "Rule Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component3, "RuleId1", "Rule Name 1", MarkAsFailedActionType.ID, policyFacts );
        // Another component with one security vulnerability and license threat "Weak Copyleft"
        final Component component4 = new Component( "g4", "a4", "v4" );
        component4.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv4", 3F ) );
        component4.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component4 );
        // Evaluate the policy
        policyFacts = new PolicyEvaluator().evaluate( rules, components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 4, policyFacts.size() );
        assertContainsPolicyFact( component1, "RuleId1", "Rule Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component2, "RuleId1", "Rule Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component3, "RuleId1", "Rule Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component4, "RuleId1", "Rule Name 1", MarkAsFailedActionType.ID, policyFacts );
    }

    private void assertContainsPolicyFact( final Component expectedComponent, final String expectedRuleId,
                                           final String expectedRuleName, final String expectedKind,
                                           final List<PolicyFact> actual )
    {
        for ( final PolicyFact actualPolicyFact : actual )
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
