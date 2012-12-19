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
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.policy.Action;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.facts.ComponentFact;
import com.sonatype.insight.brain.model.policy.facts.ConstraintFact;
import com.sonatype.insight.brain.model.policy.facts.PolicyFact;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

public class PolicyEvaluatorTest
{
    // TODO We need a lot more tests here

    @Test
    public void testEvaluate_TwoConstraintsWithConditions()
    {
        final Stage stage = new Stage( BuildStageType.ID );

        // Create policy constraints
        final List<Constraint> constraints = new ArrayList<Constraint>();
        final Constraint constraint1 = new Constraint( "ConstraintId1", "Constraint Name 1", LogicalOperator.AND );
        constraint1.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        constraints.add( constraint1 );
        final Constraint constraint2 = new Constraint( "ConstraintId2", "Constraint Name 2", LogicalOperator.AND );
        constraint2.addCondition( new Condition( LicenseCategoryConditionType.ID, "is", "Weak Copyleft" ) );
        constraints.add( constraint2 );

        final Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( stage.getStageTypeId(), new Action( FailActionType.ID ) );

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
        final List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 2, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsComponentFact( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );
        assertContainsComponentFact( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );
    }

    @Test
    public void testEvaluate_SecurityVulnerabilityConditionType()
    {
        final Stage stage = new Stage( BuildStageType.ID );

        // Create policy constraints
        final List<Constraint> constraints = new ArrayList<Constraint>();
        final Constraint constraintSVPresent =
            new Constraint( "ConstraintIdSVPresent", "Constraint Name SVPresent", LogicalOperator.AND );
        constraintSVPresent.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        constraints.add( constraintSVPresent );
        final Constraint constraintSVAbsent =
            new Constraint( "ConstraintIdSVAbsent", "Constraint Name SVAbsent", LogicalOperator.AND );
        constraintSVAbsent.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "absent" ) );
        constraints.add( constraintSVAbsent );

        final Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( stage.getStageTypeId(), new Action( FailActionType.ID ) );

        final List<Component> components = new ArrayList<Component>();
        // A component without security vulnerabilities
        final Component component1 = new Component( "g1", "a1", "v1" );
        components.add( component1 );
        // A component with one security vulnerability
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv2", 3F ) );
        components.add( component2 );

        // Evaluate the policy
        final List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 2, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsComponentFact( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );
        assertContainsComponentFact( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );
    }

    @Test
    public void testEvaluate_LicenseCategoryConditionType()
    {
        final Stage stage = new Stage( BuildStageType.ID );

        // Create policy constraints
        final List<Constraint> constraints = new ArrayList<Constraint>();
        final Constraint constraintSVIs = new Constraint( "ConstraintIdIs", "Constraint Name Is", LogicalOperator.AND );
        constraintSVIs.addCondition( new Condition( LicenseCategoryConditionType.ID, "is", "Weak Copyleft" ) );
        constraints.add( constraintSVIs );
        final Constraint constraintSVIsNot =
            new Constraint( "ConstraintIdIsNot", "Constraint Name IsNot", LogicalOperator.AND );
        constraintSVIsNot.addCondition( new Condition( LicenseCategoryConditionType.ID, "is not", "Weak Copyleft" ) );
        constraints.add( constraintSVIsNot );

        final Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( stage.getStageTypeId(), new Action( FailActionType.ID ) );

        final List<Component> components = new ArrayList<Component>();
        final Component component1 = new Component( "g1", "a1", "v1" );
        component1.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component1 );
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.setLicenseThreat( "LIBERAL" );
        components.add( component2 );

        // Evaluate the policy
        final List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 2, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsComponentFact( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );
        assertContainsComponentFact( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );
    }

    @Test
    public void testEvaluate_OneConstraintWithCompositeConditionAll()
    {
        final Stage stage = new Stage( BuildStageType.ID );

        // Create policy constraints
        final List<Constraint> constraints = new ArrayList<Constraint>();
        final Constraint constraint1 = new Constraint( "ConstraintId1", "Constraint Name 1", LogicalOperator.AND );
        constraint1.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        constraint1.addCondition( new Condition( LicenseCategoryConditionType.ID, "is", "Weak Copyleft" ) );
        constraints.add( constraint1 );

        final Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( stage.getStageTypeId(), new Action( FailActionType.ID ) );

        final List<Component> components = new ArrayList<Component>();
        // A component with one security vulnerability
        final Component component1 = new Component( "g1", "a1", "v1" );
        component1.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv1", 3F ) );
        components.add( component1 );

        // Evaluate the policy
        List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 0, policyAlerts.size() );

        // A component with license threat "Weak Copyleft"
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component2 );

        // Evaluate the policy
        policyAlerts = new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 0, policyAlerts.size() );

        // A component with one security vulnerability and license threat "Weak Copyleft"
        final Component component3 = new Component( "g3", "a3", "v3" );
        component3.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv2", 3F ) );
        component3.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component3 );

        // Evaluate the policy
        policyAlerts = new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 1, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsComponentFact( component3, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );

        // Another component with one security vulnerability and license threat "Weak Copyleft"
        final Component component4 = new Component( "g4", "a4", "v4" );
        component4.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv4", 3F ) );
        component4.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component4 );

        // Evaluate the policy
        policyAlerts = new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 1, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsComponentFact( component3, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );
        assertContainsComponentFact( component4, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );
    }

    @Test
    public void testEvaluate_OneConstraintWithCompositeConditionAny()
    {
        final Stage stage = new Stage( BuildStageType.ID );

        // Create policy constraints
        final List<Constraint> constraints = new ArrayList<Constraint>();
        final Constraint constraint1 = new Constraint( "ConstraintId1", "Constraint Name 1", LogicalOperator.OR );
        constraint1.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        constraint1.addCondition( new Condition( LicenseCategoryConditionType.ID, "is", "Weak Copyleft" ) );
        constraints.add( constraint1 );

        final Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( stage.getStageTypeId(), new Action( FailActionType.ID ) );

        final List<Component> components = new ArrayList<Component>();
        // A component with one security vulnerability
        final Component component1 = new Component( "g1", "a1", "v1" );
        component1.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv1", 3F ) );
        components.add( component1 );

        // Evaluate the policy
        List<PolicyAlert> policyAlerts = new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 1, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsComponentFact( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );

        // A component with license threat "Weak Copyleft"
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component2 );

        // Evaluate the policy
        policyAlerts = new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 1, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsComponentFact( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );
        assertContainsComponentFact( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );

        // A component with one security vulnerability and license threat "Weak Copyleft"
        final Component component3 = new Component( "g3", "a3", "v3" );
        component3.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv2", 3F ) );
        component3.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component3 );

        // Evaluate the policy
        policyAlerts = new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 1, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsComponentFact( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );
        assertContainsComponentFact( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );
        assertContainsComponentFact( component3, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );

        // Another component with one security vulnerability and license threat "Weak Copyleft"
        final Component component4 = new Component( "g4", "a4", "v4" );
        component4.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv4", 3F ) );
        component4.setLicenseThreat( "WEAKCOPYLEFT" );
        components.add( component4 );

        // Evaluate the policy
        policyAlerts = new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 1, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsComponentFact( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );
        assertContainsComponentFact( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );
        assertContainsComponentFact( component3, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );
        assertContainsComponentFact( component4, "PolicyId1", "Policy Name 1", FailActionType.ID, policyAlerts );
    }

    private static void assertContainsComponentFact( final Component expectedComponent, final String expectedPolicyId,
                                                     final String expectedPolicyName,
                                                     final String expectedActionTypeId, final List<PolicyAlert> actual )
    {
        for ( final PolicyAlert actualPolicyAlert : actual )
        {
            final PolicyFact actualPolicyFact = actualPolicyAlert.getTrigger();
            if ( expectedPolicyId.equals( actualPolicyFact.getPolicyId() )
                && expectedPolicyName.equals( actualPolicyFact.getPolicyName() ) )
            {
                for ( final ConstraintFact actualConstraintFact : actualPolicyFact.getConstraintFacts() )
                {
                    for ( final ComponentFact actualComponentFact : actualConstraintFact.getComponentFacts() )
                    {
                        if ( expectedComponent.getGroupId().equals( actualComponentFact.getGroupId() )
                            && expectedComponent.getArtifactId().equals( actualComponentFact.getArtifactId() )
                            && expectedComponent.getVersion().equals( actualComponentFact.getVersion() ) )
                        {
                            for ( final Action actualAction : actualPolicyAlert.getActions() )
                            {
                                if ( expectedActionTypeId.equals( actualAction.getActionTypeId() ) )
                                {
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }

        Assert.fail();
    }
}
