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
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseCategoryConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

public class LicenseCategoryConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
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

        assertContainsPolicyAlert( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintIdIs",
                                   "Constraint Name Is", policyAlerts );
        assertContainsPolicyAlert( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintIdIsNot",
                                   "Constraint Name IsNot", policyAlerts );
    }

    @Test
    public void testValidateCondition_ValueNotSupported()
    {
        Condition condition = new Condition( LicenseCategoryConditionType.ID, "is", "abc" );
        try
        {
            new LicenseCategoryConditionType().validateCondition( condition );
            Assert.fail( "Expected InvalidConditionException" );
        }
        catch ( InvalidConditionException expected )
        {
            if ( !expected.getMessage().endsWith( "Value not supported: abc" ) )
            {
                throw expected;
            }
        }
    }
}
