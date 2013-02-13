/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.util.StringUtils;
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
        List<ComponentFact> componentFacts = actualPolicyAlert.getTrigger().getComponentFacts();
        Assert.assertEquals( "Incorrect number of component facts", expectedComponentFactCount, componentFacts.size() );

        int actualConstraintFactCount = 0;
        Set<String> observeredConstraints = new HashSet<String>();
        for ( ComponentFact componentFact : componentFacts )
        {
            for ( ConstraintFact constraintFact : componentFact.getConstraintFacts() )
            {
                if ( observeredConstraints.add( constraintFact.getConstraintId() ) )
                {
                    actualConstraintFactCount++;
                }
            }
        }
        Assert.assertEquals( "Incorrect number of constraint facts", expectedConstraintFactCount,
                             actualConstraintFactCount );
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
                for ( ComponentFact componentFact : policyFact.getComponentFacts() )
                {
                    if ( StringUtils.equals( expectedComponent.getGroupId(), componentFact.getGroupId() )
                        && StringUtils.equals( expectedComponent.getArtifactId(), componentFact.getArtifactId() )
                        && StringUtils.equals( expectedComponent.getVersion(), componentFact.getVersion() )
                        && StringUtils.equals( expectedComponent.getHash(), componentFact.getHash() ) )
                    {
                        for ( ConstraintFact constraintFact : componentFact.getConstraintFacts() )
                        {
                            if ( expectedConstraintId.equals( constraintFact.getConstraintId() )
                                && expectedConstraintName.equals( constraintFact.getConstraintName() ) )
                            {
                                return;
                            }
                        }
                    }
                }
            }
        }

        Assert.fail( toString( actual ) );
    }

    private static String toString( List<PolicyAlert> policyAlerts )
    {
        StringBuilder result = new StringBuilder();
        for ( PolicyAlert policyAlert : policyAlerts )
        {
            result.append( policyAlert.getTrigger().toString() );
        }
        return result.toString();
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
