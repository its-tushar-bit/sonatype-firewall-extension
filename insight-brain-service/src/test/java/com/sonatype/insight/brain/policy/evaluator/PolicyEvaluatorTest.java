/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.PolicyFact;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.policy.Action;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LicenseCategoryConditionType;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.MarkAsFailedActionType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.SimpleCondition;

public class PolicyEvaluatorTest
{
    // TODO We need a lot more tests here

    @Test
    public void testEvaluate_TwoConstraintsWithSimpleConditions()
    {
        // Create policy constraints
        final List<Constraint> constraints = new ArrayList<Constraint>();
        final Constraint constraint1 = new Constraint( "ConstraintId1", "Constraint Name 1", LogicalOperator.AND );
        constraint1.addCondition( new SimpleCondition( SecurityVulnerabilityConditionType.ID, "present" ) );
        constraints.add( constraint1 );
        final Constraint constraint2 = new Constraint( "ConstraintId2", "Constraint Name 2", LogicalOperator.AND );
        constraint2.addCondition( new SimpleCondition( LicenseCategoryConditionType.ID, "is", "Weak Copyleft" ) );
        constraints.add( constraint2 );

        final Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( new Action( MarkAsFailedActionType.ID ) );

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
        final List<PolicyFact> policyFacts = new PolicyEvaluator().evaluate( Arrays.asList( policy ), components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 2, policyFacts.size() );
        assertContainsPolicyFact( component1, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component2, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
    }

    @Test
    public void testEvaluate_SecurityVulnerabilityConditionType()
    {
        // Create policy constraints
        final List<Constraint> constraints = new ArrayList<Constraint>();
        final Constraint constraintSVPresent =
            new Constraint( "ConstraintIdSVPresent", "Constraint Name SVPresent", LogicalOperator.AND );
        constraintSVPresent.addCondition( new SimpleCondition( SecurityVulnerabilityConditionType.ID, "present" ) );
        constraints.add( constraintSVPresent );
        final Constraint constraintSVAbsent =
            new Constraint( "ConstraintIdSVAbsent", "Constraint Name SVAbsent", LogicalOperator.AND );
        constraintSVAbsent.addCondition( new SimpleCondition( SecurityVulnerabilityConditionType.ID, "absent" ) );
        constraints.add( constraintSVAbsent );

        final Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( new Action( MarkAsFailedActionType.ID ) );

        final List<Component> components = new ArrayList<Component>();
        // A component without security vulnerabilities
        final Component component1 = new Component( "g1", "a1", "v1" );
        components.add( component1 );
        // A component with one security vulnerability
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv2", 3F ) );
        components.add( component2 );
        // Evaluate the policy
        final List<PolicyFact> policyFacts = new PolicyEvaluator().evaluate( Arrays.asList( policy ), components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 2, policyFacts.size() );
        assertContainsPolicyFact( component1, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component2, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
    }

    @Test
    public void testEvaluate_LicenseCategoryConditionType()
    {
        // Create policy constraints
        final List<Constraint> constraints = new ArrayList<Constraint>();
        final Constraint constraintSVIs = new Constraint( "ConstraintIdIs", "Constraint Name Is", LogicalOperator.AND );
        constraintSVIs.addCondition( new SimpleCondition( LicenseCategoryConditionType.ID, "is", "Weak Copyleft" ) );
        constraints.add( constraintSVIs );
        final Constraint constraintSVIsNot =
            new Constraint( "ConstraintIdIsNot", "Constraint Name IsNot", LogicalOperator.AND );
        constraintSVIsNot.addCondition( new SimpleCondition( LicenseCategoryConditionType.ID, "is not", "Weak Copyleft" ) );
        constraints.add( constraintSVIsNot );

        final Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( new Action( MarkAsFailedActionType.ID ) );

        final List<Component> components = new ArrayList<Component>();
        final Component component1 = new Component( "g1", "a1", "v1" );
        component1.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component1 );
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.setLicenseThreat( "LIBERAL" );
        components.add( component2 );
        // Evaluate the policy
        final List<PolicyFact> policyFacts = new PolicyEvaluator().evaluate( Arrays.asList( policy ), components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 2, policyFacts.size() );
        assertContainsPolicyFact( component1, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component2, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
    }

    @Test
    public void testEvaluate_OneConstraintWithCompositeConditionAll()
    {
        // Create policy constraints
        final List<Constraint> constraints = new ArrayList<Constraint>();
        final Constraint constraint1 = new Constraint( "ConstraintId1", "Constraint Name 1", LogicalOperator.AND );
        constraint1.addCondition( new SimpleCondition( SecurityVulnerabilityConditionType.ID, "present" ) );
        constraint1.addCondition( new SimpleCondition( LicenseCategoryConditionType.ID, "is", "Weak Copyleft" ) );
        constraints.add( constraint1 );

        final Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( new Action( MarkAsFailedActionType.ID ) );

        final List<Component> components = new ArrayList<Component>();
        // A component with one security vulnerability
        final Component component1 = new Component( "g1", "a1", "v1" );
        component1.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv1", 3F ) );
        components.add( component1 );
        // Evaluate the policy
        List<PolicyFact> policyFacts = new PolicyEvaluator().evaluate( Arrays.asList( policy ), components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 0, policyFacts.size() );
        // A component with license threat "Weak Copyleft"
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component2 );
        // Evaluate the policy
        policyFacts = new PolicyEvaluator().evaluate( Arrays.asList( policy ), components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 0, policyFacts.size() );
        // A component with one security vulnerability and license threat "Weak Copyleft"
        final Component component3 = new Component( "g3", "a3", "v3" );
        component3.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv2", 3F ) );
        component3.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component3 );
        // Evaluate the policy
        policyFacts = new PolicyEvaluator().evaluate( Arrays.asList( policy ), components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 1, policyFacts.size() );
        assertContainsPolicyFact( component3, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
        // Another component with one security vulnerability and license threat "Weak Copyleft"
        final Component component4 = new Component( "g4", "a4", "v4" );
        component4.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv4", 3F ) );
        component4.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component4 );
        // Evaluate the policy
        policyFacts = new PolicyEvaluator().evaluate( Arrays.asList( policy ), components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 2, policyFacts.size() );
        assertContainsPolicyFact( component3, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component4, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
    }

    @Test
    public void testEvaluate_OneConstraintWithCompositeConditionAny()
    {
        // Create policy constraints
        final List<Constraint> constraints = new ArrayList<Constraint>();
        final Constraint constraint1 = new Constraint( "ConstraintId1", "Constraint Name 1", LogicalOperator.OR );
        constraint1.addCondition( new SimpleCondition( SecurityVulnerabilityConditionType.ID, "present" ) );
        constraint1.addCondition( new SimpleCondition( LicenseCategoryConditionType.ID, "is", "Weak Copyleft" ) );
        constraints.add( constraint1 );

        final Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( new Action( MarkAsFailedActionType.ID ) );

        final List<Component> components = new ArrayList<Component>();
        // A component with one security vulnerability
        final Component component1 = new Component( "g1", "a1", "v1" );
        component1.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv1", 3F ) );
        components.add( component1 );
        // Evaluate the policy
        List<PolicyFact> policyFacts = new PolicyEvaluator().evaluate( Arrays.asList( policy ), components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 1, policyFacts.size() );
        assertContainsPolicyFact( component1, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
        // A component with license threat "Weak Copyleft"
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component2 );
        // Evaluate the policy
        policyFacts = new PolicyEvaluator().evaluate( Arrays.asList( policy ), components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 2, policyFacts.size() );
        assertContainsPolicyFact( component1, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component2, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
        // A component with one security vulnerability and license threat "Weak Copyleft"
        final Component component3 = new Component( "g3", "a3", "v3" );
        component3.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv2", 3F ) );
        component3.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component3 );
        // Evaluate the policy
        policyFacts = new PolicyEvaluator().evaluate( Arrays.asList( policy ), components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 3, policyFacts.size() );
        assertContainsPolicyFact( component1, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component2, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component3, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
        // Another component with one security vulnerability and license threat "Weak Copyleft"
        final Component component4 = new Component( "g4", "a4", "v4" );
        component4.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv4", 3F ) );
        component4.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component4 );
        // Evaluate the policy
        policyFacts = new PolicyEvaluator().evaluate( Arrays.asList( policy ), components );
        Assert.assertNotNull( policyFacts );
        Assert.assertEquals( 4, policyFacts.size() );
        assertContainsPolicyFact( component1, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component2, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component3, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
        assertContainsPolicyFact( component4, "PolicyId1", "Policy Name 1", MarkAsFailedActionType.ID, policyFacts );
    }

    private static void assertContainsPolicyFact( final Component expectedComponent, final String expectedPolicyId,
                                                  final String expectedPolicyName, final String expectedKind,
                                                  final List<PolicyFact> actual )
    {
        for ( final PolicyFact actualPolicyFact : actual )
        {
            if ( expectedComponent == actualPolicyFact.getComponent()
                && expectedPolicyId.equals( actualPolicyFact.getPolicyId() )
                && expectedPolicyName.equals( actualPolicyFact.getPolicyName() )
                && expectedKind.equals( actualPolicyFact.getKind() ) )
            {
                return;
            }
        }

        Assert.fail();
    }
}
