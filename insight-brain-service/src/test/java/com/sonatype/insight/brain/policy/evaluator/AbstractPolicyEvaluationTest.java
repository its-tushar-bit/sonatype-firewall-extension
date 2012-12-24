/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;

import org.junit.Assert;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Action;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.facts.ComponentFact;
import com.sonatype.insight.brain.model.policy.facts.ConstraintFact;
import com.sonatype.insight.brain.model.policy.facts.PolicyFact;

public abstract class AbstractPolicyEvaluationTest
{
    protected Constraint createConstraint( String constraintId, String constraintName, String conditionTypeId,
                                           String operator, String value )
    {
        Condition condition = new Condition();
        condition.setConditionTypeId( conditionTypeId );
        condition.setOperator( operator );
        condition.setValue( value );
        Constraint constraint = new Constraint( constraintId, constraintName, LogicalOperator.AND );
        constraint.addCondition( condition );
        return constraint;
    }

    public static void assertFactCounts( int expectedConstraintFactCount, int expectedComponentFactCount,
                                         PolicyAlert actualPolicyAlert )
    {
        List<ConstraintFact> constraintFacts = actualPolicyAlert.getTrigger().getConstraintFacts();
        Assert.assertEquals( "Incorrect number of constraint facts", expectedConstraintFactCount,
                             constraintFacts.size() );

        int actualComponentFactCount = 0;
        for ( ConstraintFact constraintFact : constraintFacts )
        {
            actualComponentFactCount += constraintFact.getComponentFacts().size();
        }
        Assert.assertEquals( "Incorrect number of component facts", expectedComponentFactCount,
                             actualComponentFactCount );
    }

    public static void assertContainsPolicyAlert( Component expectedComponent, String expectedPolicyId,
                                                  String expectedPolicyName, String actionTypeId,
                                                  String expectedConstraintId, String expectedConstraintName,
                                                  List<PolicyAlert> actual )
    {
        for ( PolicyAlert actualPolicyAlert : actual )
        {
            PolicyFact policyFact = actualPolicyAlert.getTrigger();
            if ( expectedPolicyId.equals( policyFact.getPolicyId() )
                && expectedPolicyName.equals( policyFact.getPolicyName() )
                && policyAlertContainsAction( actualPolicyAlert, actionTypeId ) )
            {
                for ( ConstraintFact constraintFact : policyFact.getConstraintFacts() )
                {
                    if ( expectedConstraintId.equals( constraintFact.getConstraintId() )
                        && expectedConstraintName.equals( constraintFact.getConstraintName() ) )
                    {
                        for ( ComponentFact componentFact : constraintFact.getComponentFacts() )
                        {
                            if ( expectedComponent.getGroupId().equals( componentFact.getGroupId() )
                                && expectedComponent.getArtifactId().equals( componentFact.getArtifactId() )
                                && expectedComponent.getVersion().equals( componentFact.getVersion() )
                                && expectedConstraintId.equals( componentFact.getConstraintId() ) )
                            {
                                return;
                            }
                        }
                    }
                }
            }
        }

        Assert.fail();
    }

    private static boolean policyAlertContainsAction( PolicyAlert actualPolicyAlert, String actionTypeId )
    {
        for ( Action action : actualPolicyAlert.getActions() )
        {
            if ( actionTypeId.equals( action.getActionTypeId() ) )
            {
                return true;
            }
        }
        return false;
    }
}
