/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;

public class PolicyDigesterTest
{
    @Test
    public void testDigest_Nothing()
    {
        final List<PolicyAlert> oldAlerts = Collections.emptyList();
        final List<PolicyAlert> newAlerts = Collections.emptyList();

        final List<PolicyAlert>[] results = PolicyDigester.digestPolicyAlerts( newAlerts, oldAlerts );

        assertThat( results, nullValue() );
    }

    @Test
    public void testDigest_UnknownPolicyAlert()
    {
        final List<PolicyAlert> oldAlerts = Collections.emptyList();
        final List<PolicyAlert> newAlerts = defaultPolicyAlerts();

        final List<PolicyAlert>[] results = PolicyDigester.digestPolicyAlerts( newAlerts, oldAlerts );

        assertThat( results[0], contains( newAlerts.get( 0 ) ) );
        assertThat( results[1], empty() );
    }

    @Test
    public void testDigest_NoChange()
    {
        final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
        final List<PolicyAlert> newAlerts = defaultPolicyAlerts();

        final List<PolicyAlert>[] results = PolicyDigester.digestPolicyAlerts( newAlerts, oldAlerts );

        assertThat( results, nullValue() );
    }

    @Test
    public void testDigest_ClearedPolicyAlert()
    {
        final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
        final List<PolicyAlert> newAlerts = Collections.emptyList();

        final List<PolicyAlert>[] results = PolicyDigester.digestPolicyAlerts( newAlerts, oldAlerts );

        assertThat( results[0], empty() );
        assertThat( results[1], contains( oldAlerts.get( 0 ) ) );
    }

    @Test
    public void testDigest_UnknownPolicyAlertBefore()
    {
        final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
        final List<PolicyAlert> newAlerts = defaultPolicyAlerts();

        newAlerts.add( 0, new PolicyAlert( policyFact( "policy_1", "Policy 1", 0 ), Collections.<Action> emptyList() ) );

        final List<PolicyAlert>[] results = PolicyDigester.digestPolicyAlerts( newAlerts, oldAlerts );

        assertThat( results[0], contains( newAlerts.get( 0 ) ) );
        assertThat( results[1], empty() );
    }

    @Test
    public void testDigest_UnknownPolicyAlertAfter()
    {
        final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
        final List<PolicyAlert> newAlerts = defaultPolicyAlerts();

        newAlerts.add( new PolicyAlert( policyFact( "policy_8", "Policy 8", 0 ), Collections.<Action> emptyList() ) );

        final List<PolicyAlert>[] results = PolicyDigester.digestPolicyAlerts( newAlerts, oldAlerts );

        assertThat( results[0], contains( newAlerts.get( 1 ) ) );
        assertThat( results[1], empty() );
    }

    @Test
    public void testDigest_UnknownPolicyAlertBeforeAndAfter()
    {
        final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
        final List<PolicyAlert> newAlerts = defaultPolicyAlerts();

        newAlerts.add( 0, new PolicyAlert( policyFact( "policy_1", "Policy 1", 0 ), Collections.<Action> emptyList() ) );
        newAlerts.add( new PolicyAlert( policyFact( "policy_8", "Policy 8", 0 ), Collections.<Action> emptyList() ) );

        final List<PolicyAlert>[] results = PolicyDigester.digestPolicyAlerts( newAlerts, oldAlerts );

        assertThat( results[0], contains( newAlerts.get( 0 ), newAlerts.get( 2 ) ) );
        assertThat( results[1], empty() );
    }

    @Test
    public void testDigest_UnknownPolicyAlertBeforeAndAfterClearedPolicyAlert()
    {
        final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
        final List<PolicyAlert> newAlerts = new ArrayList<PolicyAlert>();

        newAlerts.add( new PolicyAlert( policyFact( "policy_1", "Policy 1", 0 ), Collections.<Action> emptyList() ) );
        newAlerts.add( new PolicyAlert( policyFact( "policy_8", "Policy 8", 0 ), Collections.<Action> emptyList() ) );

        final List<PolicyAlert>[] results = PolicyDigester.digestPolicyAlerts( newAlerts, oldAlerts );

        assertThat( results[0], contains( newAlerts.get( 0 ), newAlerts.get( 1 ) ) );
        assertThat( results[1], contains( oldAlerts.get( 0 ) ) );
    }

    @Test
    public void testDigest_UnknownComponentFactBefore()
    {
        final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
        final PolicyFact trigger = oldAlerts.get( 0 ).getTrigger();

        final ComponentFact oldFact = trigger.getComponentFacts().get( 0 );
        final ComponentFact newFact = componentFact( "1G", "A", "V" );

        final List<PolicyAlert> newAlerts = Arrays.asList( oldAlerts.get( 0 ).cloneWith( trigger.cloneWith( newFact, oldFact ) ) );

        final List<PolicyAlert>[] results = PolicyDigester.digestPolicyAlerts( newAlerts, oldAlerts );

        assertThat( results[0], contains( any( PolicyAlert.class ) ) );
        assertThat( results[0].get( 0 ).getTrigger().getComponentFacts(), contains( newFact ) );
        assertThat( results[1], empty() );
    }

    @Test
    public void testDigest_UnknownComponentFactAfter()
    {
        final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
        PolicyFact trigger = oldAlerts.get( 0 ).getTrigger();

        final ComponentFact oldFact = trigger.getComponentFacts().get( 0 );
        final ComponentFact newFact = componentFact( "G1", "A", "V" );

        final List<PolicyAlert> newAlerts = Arrays.asList( oldAlerts.get( 0 ).cloneWith( trigger.cloneWith( oldFact, newFact ) ) );

        final List<PolicyAlert>[] results = PolicyDigester.digestPolicyAlerts( newAlerts, oldAlerts );

        assertThat( results[0], contains( any( PolicyAlert.class ) ) );
        assertThat( results[0].get( 0 ).getTrigger().getComponentFacts(), contains( newFact ) );
        assertThat( results[1], empty() );
    }

    @Test
    public void testDigest_UnknownConstraintFactBefore()
    {
        final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
        final PolicyFact trigger = oldAlerts.get( 0 ).getTrigger();
        final ComponentFact component = trigger.getComponentFacts().get( 0 );

        final ConstraintFact oldFact = component.getConstraintFacts().get( 0 );
        final ConstraintFact newFact = constraintFact( "constraint_1", "Constraint 1" );

        final List<PolicyAlert> newAlerts =
            Arrays.asList( oldAlerts.get( 0 ).cloneWith( trigger.cloneWith( component.cloneWith( newFact, oldFact ) ) ) );

        final List<PolicyAlert>[] results = PolicyDigester.digestPolicyAlerts( newAlerts, oldAlerts );

        assertThat( results[0], contains( any( PolicyAlert.class ) ) );
        assertThat( results[0].get( 0 ).getTrigger().getComponentFacts().get( 0 ).getConstraintFacts(),
                    contains( newFact ) );
        assertThat( results[1], empty() );
    }

    @Test
    public void testDigest_UnknownConstraintFactAfter()
    {
        final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
        final PolicyFact trigger = oldAlerts.get( 0 ).getTrigger();
        final ComponentFact component = trigger.getComponentFacts().get( 0 );

        final ConstraintFact oldFact = component.getConstraintFacts().get( 0 );
        final ConstraintFact newFact = constraintFact( "constraint_8", "Constraint 8" );

        final List<PolicyAlert> newAlerts =
            Arrays.asList( oldAlerts.get( 0 ).cloneWith( trigger.cloneWith( component.cloneWith( oldFact, newFact ) ) ) );

        final List<PolicyAlert>[] results = PolicyDigester.digestPolicyAlerts( newAlerts, oldAlerts );

        assertThat( results[0], contains( any( PolicyAlert.class ) ) );
        assertThat( results[0].get( 0 ).getTrigger().getComponentFacts().get( 0 ).getConstraintFacts(),
                    contains( newFact ) );
        assertThat( results[1], empty() );
    }

    @Test
    public void testDigest_UnknownConditionFactBefore()
    {
        final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
        final PolicyFact trigger = oldAlerts.get( 0 ).getTrigger();
        final ComponentFact component = trigger.getComponentFacts().get( 0 );
        final ConstraintFact constraint = component.getConstraintFacts().get( 0 );

        final ConditionFact oldFact = constraint.getConditionFacts().get( 0 );
        final ConditionFact newFact = conditionFact( CoordinatesConditionType.ID, "match", "*" );

        final List<PolicyAlert> newAlerts =
            Arrays.asList( oldAlerts.get( 0 ).cloneWith( trigger.cloneWith( component.cloneWith( constraint.cloneWith( newFact, oldFact ) ) ) ) );

        final List<PolicyAlert>[] results = PolicyDigester.digestPolicyAlerts( newAlerts, oldAlerts );

        assertThat( results[0], contains( any( PolicyAlert.class ) ) );
        assertThat( results[0].get( 0 ).getTrigger().getComponentFacts().get( 0 ).getConstraintFacts().get( 0 ).getConditionFacts(),
                    contains( newFact ) );
        assertThat( results[1], empty() );
    }

    @Test
    public void testDigest_UnknownConditionFactAfter()
    {
        final List<PolicyAlert> oldAlerts = defaultPolicyAlerts();
        final PolicyFact trigger = oldAlerts.get( 0 ).getTrigger();
        final ComponentFact component = trigger.getComponentFacts().get( 0 );
        final ConstraintFact constraint = component.getConstraintFacts().get( 0 );

        final ConditionFact oldFact = constraint.getConditionFacts().get( 0 );
        final ConditionFact newFact = conditionFact( SecurityVulnerabilityConditionType.ID, "present" );

        final List<PolicyAlert> newAlerts =
            Arrays.asList( oldAlerts.get( 0 ).cloneWith( trigger.cloneWith( component.cloneWith( constraint.cloneWith( oldFact, newFact ) ) ) ) );

        final List<PolicyAlert>[] results = PolicyDigester.digestPolicyAlerts( newAlerts, oldAlerts );

        assertThat( results[0], contains( any( PolicyAlert.class ) ) );
        assertThat( results[0].get( 0 ).getTrigger().getComponentFacts().get( 0 ).getConstraintFacts().get( 0 ).getConditionFacts(),
                    contains( newFact ) );
        assertThat( results[1], empty() );
    }

    private static List<PolicyAlert> defaultPolicyAlerts()
    {
        final List<PolicyAlert> policyAlerts = new ArrayList<PolicyAlert>();
        policyAlerts.add( new PolicyAlert( defaultPolicyFact(), Collections.<Action> emptyList() ) );
        return policyAlerts;
    }

    private static PolicyFact defaultPolicyFact()
    {
        final ConditionFact conditionFact = conditionFact( MatchStateConditionType.ID, "is", "exact" );
        final ConstraintFact constraintFact = constraintFact( "constraint_4", "Constraint 4" );
        constraintFact.addConditionFact( conditionFact );
        final ComponentFact componentFact = componentFact( "G", "A", "V" );
        componentFact.addConstraintFact( constraintFact );
        final PolicyFact policyFact = policyFact( "policy_4", "Policy 4", 0 );
        policyFact.addComponentFact( componentFact );
        return policyFact;
    }

    private static PolicyFact policyFact( final String id, final String name, final int threatLevel )
    {
        return new PolicyFact( id, name, threatLevel );
    }

    private static ComponentFact componentFact( final String groupId, final String artifactId, final String version )
    {
        return new ComponentFact( groupId, artifactId, version, null /* hash */);
    }

    private static ConstraintFact constraintFact( final String id, final String name )
    {
        return new ConstraintFact( id, name );
    }

    private static ConditionFact conditionFact( final String conditionTypeId, final String operator, final String value )
    {
        final Condition condition = new Condition();
        condition.setConditionTypeId( conditionTypeId );
        condition.setOperator( operator );
        condition.setValue( value );
        return PolicyEvaluator.createConditionFact( condition, null /* component */);
    }

    private static ConditionFact conditionFact( final String conditionTypeId, final String operator )
    {
        final Condition condition = new Condition();
        condition.setConditionTypeId( conditionTypeId );
        condition.setOperator( operator );
        return PolicyEvaluator.createConditionFact( condition, null /* component */);
    }
}
