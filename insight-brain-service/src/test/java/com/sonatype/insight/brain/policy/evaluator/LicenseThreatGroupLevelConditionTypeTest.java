/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

public class LicenseThreatGroupLevelConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
    private static LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

    private static String applicationId;

    private static LicenseThreatGroup licenseThreatGroup2;

    private static LicenseThreatGroup licenseThreatGroup5;

    @BeforeClass
    public static void beforeClass()
    {
        Application application = new Application();
        application.setPublicId( "LicenseThreatGroupLevelConditionTypeTest_AppId" );
        new ApplicationDAO().insert( application );
        applicationId = application.getId();

        // Delete all existing license threat groups
        for ( LicenseThreatGroup licenseThreatGroup : licenseThreatGroupDAO.getByApplicationId( applicationId ) )
        {
            licenseThreatGroupDAO.delete( licenseThreatGroup );
        }

        licenseThreatGroup2 = new LicenseThreatGroup( applicationId, "Level 2", 2 );
        licenseThreatGroupDAO.insert( licenseThreatGroup2 );
        licenseThreatGroup5 = new LicenseThreatGroup( applicationId, "Level 5", 5 );
        licenseThreatGroupDAO.insert( licenseThreatGroup5 );
    }

    @AfterClass
    public static void afterClass()
    {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByIdNotNull( applicationId );
        applicationDAO.delete( application );
    }

    private Constraint createConstraint( String operator, String value )
    {
        return createConstraint( "ConstraintId1", "Constraint Name 1", LicenseThreatGroupLevelConditionType.ID,
                                 operator, value );
    }

    @Test
    public void testEvaluateLessOrEqual()
    {
        // Create policy constraints
        Constraint constraint = createConstraint( "<=", "2" );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        Component component1 = new Component( "g1", "a1", "v1", MatchState.EXACT );
        component1.addLicenseThreatGroup( licenseThreatGroup2 );
        components.add( component1 );
        Component component2 = new Component( "g2", "a2", "v2", MatchState.EXACT );
        component2.addLicenseThreatGroup( licenseThreatGroup5 );
        components.add( component2 );

        // Evaluate the policy
        List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( applicationId, new Stage( BuildStageType.ID ), Arrays.asList( policy ),
                                            components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        assertFactCounts( 1, 1, policyAlerts.get( 0 ) );

        assertContainsPolicyAlert( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", LicenseThreatGroupLevelConditionType.ID, policyAlerts );
    }

    @Test
    public void testEvaluateGreaterOrEqual()
    {
        // Create policy constraints
        Constraint constraint = createConstraint( ">=", "5" );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        Component component1 = new Component( "g1", "a1", "v1", MatchState.EXACT );
        component1.addLicenseThreatGroup( licenseThreatGroup2 );
        components.add( component1 );
        Component component2 = new Component( "g2", "a2", "v2", MatchState.EXACT );
        component2.addLicenseThreatGroup( licenseThreatGroup5 );
        components.add( component2 );

        // Evaluate the policy
        List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( applicationId, new Stage( BuildStageType.ID ), Arrays.asList( policy ),
                                            components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        assertFactCounts( 1, 1, policyAlerts.get( 0 ) );

        assertContainsPolicyAlert( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", LicenseThreatGroupLevelConditionType.ID, policyAlerts );
    }

    @Test
    public void testValidateCondition_InvalidLicenseThreatGroupLevel()
    {
        Condition condition = new Condition( LicenseThreatGroupLevelConditionType.ID, "<=", "abc" );
        try
        {
            new LicenseThreatGroupLevelConditionType().validateCondition( condition, applicationId );
            Assert.fail( "Expected InvalidConditionException" );
        }
        catch ( InvalidConditionException expected )
        {
            if ( !expected.getMessage().endsWith( "Invalid license threat group level: abc" ) )
            {
                throw expected;
            }
        }
    }
}
