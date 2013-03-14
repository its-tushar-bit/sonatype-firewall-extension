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

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

public class AgeInDaysConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
    private Constraint createConstraint( String operator, String value )
    {
        return createConstraint( "ConstraintId1", "Constraint Name 1", AgeInDaysConditionType.ID, operator, value );
    }

    @Test
    public void testEvaluateOlderThan()
    {
        // Create policy constraints
        Constraint constraint = createConstraint( "older than", "11" );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        // A component without age
        Component component1 = new Component( "g1", "a1", "v1", MatchState.EXACT );
        components.add( component1 );
        // A component with age 10
        Component component2 = new Component( "g2", "a2", "v2", MatchState.EXACT );
        component2.setCatalogDate( System.currentTimeMillis() - 10 * AgeInDaysConditionType.DAY_IN_MILLISECONDS - 1 );
        components.add( component2 );
        // A component with age 20
        Component component3 = new Component( "g3", "a3", "v3", MatchState.EXACT );
        component3.setCatalogDate( System.currentTimeMillis() - 20 * AgeInDaysConditionType.DAY_IN_MILLISECONDS - 1 );
        components.add( component3 );
        // Evaluate the policy
        List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( null /* applicationId */, new Stage( BuildStageType.ID ),
                                            Arrays.asList( policy ), components );
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        assertFactCounts( 1, 1, policyAlerts.get( 0 ) );
        assertContainsPolicyAlert( component3, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
    }

    @Test
    public void testEvaluateYoungerThan()
    {
        // Create policy constraints
        Constraint constraint = createConstraint( "younger than", "11" );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        // A component without age
        Component component1 = new Component( "g1", "a1", "v1", MatchState.EXACT );
        components.add( component1 );
        // A component with age 10
        Component component2 = new Component( "g2", "a2", "v2", MatchState.EXACT );
        component2.setCatalogDate( System.currentTimeMillis() - 10 * AgeInDaysConditionType.DAY_IN_MILLISECONDS - 1 );
        components.add( component2 );
        // A component with age 20
        Component component3 = new Component( "g3", "a3", "v3", MatchState.EXACT );
        component3.setCatalogDate( System.currentTimeMillis() - 20 * AgeInDaysConditionType.DAY_IN_MILLISECONDS - 1 );
        components.add( component3 );
        // Evaluate the policy
        List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( null /* applicationId */, new Stage( BuildStageType.ID ),
                                            Arrays.asList( policy ), components );
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        assertFactCounts( 1, 1, policyAlerts.get( 0 ) );
        assertContainsPolicyAlert( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
    }

    @Test
    public void testValidateCondition_ValueNotANumber()
    {
        Condition condition = new Condition( AgeInDaysConditionType.ID, "older than", "abc" );
        try
        {
            new AgeInDaysConditionType().validateCondition( condition, null /* applicationId */);
            Assert.fail( "Expected InvalidConditionException" );
        }
        catch ( InvalidConditionException expected )
        {
            if ( !expected.getMessage().endsWith( "Invalid age (in days): abc" ) )
            {
                throw expected;
            }
        }
    }
}
