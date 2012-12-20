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
import com.sonatype.insight.brain.model.policy.Action;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.DeclaredLicenseInListConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

public class DeclaredLicenseInListConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
    private Constraint createConstraint( String conditionTypeId, String operator, String value )
    {
        return createConstraint( "ConstraintId1", "Constraint Name 1", conditionTypeId, operator, value );
    }

    @Test
    public void testEvaluateInList()
    {
        // Create policy constraints
        Constraint constraint =
            createConstraint( DeclaredLicenseInListConditionType.ID, "in list", "Apache-2.0,UNSPECIFIED" );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        Component component1 = new Component( "g1", "a1", "v1" );
        component1.addDeclaredLicenseName( "Not Provided" );
        components.add( component1 );
        Component component2 = new Component( "g2", "a2", "v2" );
        component2.addDeclaredLicenseName( "AFL-1.2" );
        components.add( component2 );

        // Evaluate the policy
        List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( new Stage( BuildStageType.ID ), Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 1, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsPolicyAlert( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
    }

    @Test
    public void testEvaluateNotInList()
    {
        // Create policy constraints
        Constraint constraint =
            createConstraint( DeclaredLicenseInListConditionType.ID, "not in list", "Apache-2.0,UNSPECIFIED" );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        Component component1 = new Component( "g1", "a1", "v1" );
        component1.addDeclaredLicenseName( "Not Provided" );
        components.add( component1 );
        Component component2 = new Component( "g2", "a2", "v2" );
        component2.addDeclaredLicenseName( "AFL-1.2,Apache-2.0" );
        components.add( component2 );

        // Evaluate the policy
        List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( new Stage( BuildStageType.ID ), Arrays.asList( policy ), components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        Assert.assertEquals( 1, policyAlerts.get( 0 ).getTrigger().getConstraintFacts().size() );

        assertContainsPolicyAlert( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
    }
}
