/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
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
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

public class PolicyEvaluatorTest
    extends AbstractPolicyEvaluationTest
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
        constraint2.addCondition( new Condition( LicenseConditionType.ID, "is", "Apache-2.0" ) );
        constraints.add( constraint2 );

        final Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( stage.getStageTypeId(), new Action( FailActionType.ID ) );

        final List<Component> components = new ArrayList<Component>();
        // A component with one security vulnerability
        final Component component1 = new Component( "g1", "a1", "v1" );
        component1.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv1", 3F ) );
        components.add( component1 );
        // A component with license category "Weak Copyleft"
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.addDeclaredLicenseName( "Apache-2.0" );
        components.add( component2 );

        // Evaluate the policy
        final List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 2, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsPolicyAlert( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
        assertContainsPolicyAlert( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId2",
                                   "Constraint Name 2", policyAlerts );
    }

    @Test
    public void testEvaluate_OneConstraintWithCompositeConditionAll()
    {
        final Stage stage = new Stage( BuildStageType.ID );

        // Create policy constraints
        final List<Constraint> constraints = new ArrayList<Constraint>();
        final Constraint constraint1 = new Constraint( "ConstraintId1", "Constraint Name 1", LogicalOperator.AND );
        constraint1.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        constraint1.addCondition( new Condition( LicenseConditionType.ID, "is", "Apache-2.0" ) );
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

        // A component with license category "Weak Copyleft"
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.addDeclaredLicenseName( "Apache-2.0" );
        components.add( component2 );

        // Evaluate the policy
        policyAlerts = new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 0, policyAlerts.size() );

        // A component with one security vulnerability and license category "Weak Copyleft"
        final Component component3 = new Component( "g3", "a3", "v3" );
        component3.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv2", 3F ) );
        component3.addDeclaredLicenseName( "Apache-2.0" );
        components.add( component3 );

        // Evaluate the policy
        policyAlerts = new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 1, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsPolicyAlert( component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );

        // Another component with one security vulnerability and license category "Weak Copyleft"
        final Component component4 = new Component( "g4", "a4", "v4" );
        component4.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv4", 3F ) );
        component4.addDeclaredLicenseName( "Apache-2.0" );
        components.add( component4 );

        // Evaluate the policy
        policyAlerts = new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 1, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsPolicyAlert( component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
        assertContainsPolicyAlert( component4, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
    }

    @Test
    public void testEvaluate_OneConstraintWithCompositeConditionAny()
    {
        final Stage stage = new Stage( BuildStageType.ID );

        // Create policy constraints
        final List<Constraint> constraints = new ArrayList<Constraint>();
        final Constraint constraint1 = new Constraint( "ConstraintId1", "Constraint Name 1", LogicalOperator.OR );
        constraint1.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        constraint1.addCondition( new Condition( LicenseConditionType.ID, "is", "Apache-2.0" ) );
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

        assertContainsPolicyAlert( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );

        // A component with license category "Weak Copyleft"
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.addDeclaredLicenseName( "Apache-2.0" );
        components.add( component2 );

        // Evaluate the policy
        policyAlerts = new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 1, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsPolicyAlert( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
        assertContainsPolicyAlert( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );

        // A component with one security vulnerability and license category "Weak Copyleft"
        final Component component3 = new Component( "g3", "a3", "v3" );
        component3.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv2", 3F ) );
        component3.addDeclaredLicenseName( "Apache-2.0" );
        components.add( component3 );

        // Evaluate the policy
        policyAlerts = new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 1, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsPolicyAlert( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
        assertContainsPolicyAlert( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
        assertContainsPolicyAlert( component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );

        // Another component with one security vulnerability and license category "Weak Copyleft"
        final Component component4 = new Component( "g4", "a4", "v4" );
        component4.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv4", 3F ) );
        component4.addDeclaredLicenseName( "Apache-2.0" );
        components.add( component4 );

        // Evaluate the policy
        policyAlerts = new PolicyEvaluator().evaluate( stage, Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 1, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsPolicyAlert( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
        assertContainsPolicyAlert( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
        assertContainsPolicyAlert( component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
        assertContainsPolicyAlert( component4, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
    }

    @Test
    public void testEvaluate_ContextBasedActions()
    {
        // Create policy constraints
        final List<Constraint> constraints = new ArrayList<Constraint>();
        final Constraint constraint1 = new Constraint( "ConstraintId1", "Constraint Name 1", LogicalOperator.AND );
        constraint1.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        constraints.add( constraint1 );
        final Constraint constraint2 = new Constraint( "ConstraintId2", "Constraint Name 2", LogicalOperator.AND );
        constraint2.addCondition( new Condition( LicenseConditionType.ID, "is", "Apache-2.0" ) );
        constraints.add( constraint2 );

        final Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );

        policy.addAction( DevelopStageType.ID, new Action( WarnActionType.ID ) );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );
        policy.addAction( ReleaseStageType.ID, new Action( NotifyActionType.ID ) );
        policy.getActions( ReleaseStageType.ID ).get( 0 ).setTarget( "manager@some.com" );

        final List<Component> components = new ArrayList<Component>();
        // A component with one security vulnerability
        final Component component1 = new Component( "g1", "a1", "v1" );
        component1.addSecurityVulnerability( new SecurityVulnerability( "osvdb", "sv1", 3F ) );
        components.add( component1 );
        // A component with license category "Weak Copyleft"
        final Component component2 = new Component( "g2", "a2", "v2" );
        component2.addDeclaredLicenseName( "Apache-2.0" );
        components.add( component2 );

        List<PolicyAlert> policyAlerts;
        Action[] actions;

        // Evaluate the policy when developing
        policyAlerts =
            new PolicyEvaluator().evaluate( new Stage( DevelopStageType.ID ), Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        actions = policyAlerts.get( 0 ).getActions();
        Assert.assertEquals( 1, actions.length );
        Assert.assertEquals( WarnActionType.ID, actions[0].getActionTypeId() );
        Assert.assertNull( actions[0].getTarget() );

        // Evaluate the policy when building
        policyAlerts =
            new PolicyEvaluator().evaluate( new Stage( BuildStageType.ID ), Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        actions = policyAlerts.get( 0 ).getActions();
        Assert.assertEquals( 1, actions.length );
        Assert.assertEquals( FailActionType.ID, actions[0].getActionTypeId() );
        Assert.assertNull( actions[0].getTarget() );

        // Evaluate the policy when releasing
        policyAlerts =
            new PolicyEvaluator().evaluate( new Stage( ReleaseStageType.ID ), Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        actions = policyAlerts.get( 0 ).getActions();
        Assert.assertEquals( 1, actions.length );
        Assert.assertEquals( NotifyActionType.ID, actions[0].getActionTypeId() );
        Assert.assertEquals( "manager@some.com", actions[0].getTarget() );
    }
}
