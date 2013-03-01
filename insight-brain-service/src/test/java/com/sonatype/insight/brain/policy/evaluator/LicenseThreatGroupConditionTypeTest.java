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
import com.sonatype.insight.brain.dataaccess.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

public class LicenseThreatGroupConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
    private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

    private ComponentDAO componentDAO = new ComponentDAO();

    private static String applicationId;

    @BeforeClass
    public static void beforeClass()
    {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = new Application();
        application.setPublicId( "LicenseThreatGroupConditionTypeTest_AppId" );
        applicationDAO.insert( application );
        applicationId = application.getId();
    }

    @AfterClass
    public static void afterClass()
    {
        DataSourceFactory.clear_ForTestsOnly();
    }

    private Constraint createConstraint( String operator, String value )
    {
        return createConstraint( "ConstraintId1", "Constraint Name 1", LicenseThreatGroupConditionType.ID, operator,
                                 value );
    }

    @Test
    public void testEvaluateIs_Declared()
    {
        LicenseThreatGroup licenseThreatGroup =
            licenseThreatGroupDAO.getByApplicationIdAndLicenseId( applicationId, "GPL-2.0" );

        // Create policy constraints
        Constraint constraint = createConstraint( "is", licenseThreatGroup.getId() );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        Component component1 = new Component( "g1", "a1", "v1" );
        component1.addDeclaredLicenseId( "Apache-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component1 );
        components.add( component1 );
        Component component2 = new Component( "g2", "a2", "v2" );
        component2.addDeclaredLicenseId( "GPL-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component2 );
        components.add( component2 );

        // Evaluate the policy
        List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( applicationId, new Stage( BuildStageType.ID ), Arrays.asList( policy ),
                                            components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        assertFactCounts( 1, 1, policyAlerts.get( 0 ) );

        assertContainsPolicyAlert( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
    }

    @Test
    public void testEvaluateIsNot_Declared()
    {
        LicenseThreatGroup licenseThreatGroup =
            licenseThreatGroupDAO.getByApplicationIdAndLicenseId( applicationId, "GPL-2.0" );

        // Create policy constraints
        Constraint constraint = createConstraint( "is not", licenseThreatGroup.getId() );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        Component component1 = new Component( "g1", "a1", "v1" );
        component1.addDeclaredLicenseId( "Apache-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component1 );
        components.add( component1 );
        Component component2 = new Component( "g2", "a2", "v2" );
        component2.addDeclaredLicenseId( "GPL-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component2 );
        components.add( component2 );

        // Evaluate the policy
        List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( applicationId, new Stage( BuildStageType.ID ), Arrays.asList( policy ),
                                            components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        assertFactCounts( 1, 1, policyAlerts.get( 0 ) );

        assertContainsPolicyAlert( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
    }

    @Test
    public void testEvaluateIs_Observed()
    {
        LicenseThreatGroup licenseThreatGroup =
            licenseThreatGroupDAO.getByApplicationIdAndLicenseId( applicationId, "GPL-2.0" );

        // Create policy constraints
        Constraint constraint = createConstraint( "is", licenseThreatGroup.getId() );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        Component component1 = new Component( "g1", "a1", "v1" );
        component1.addObservedLicenseId( "Apache-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component1 );
        components.add( component1 );
        Component component2 = new Component( "g2", "a2", "v2" );
        component2.addObservedLicenseId( "GPL-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component2 );
        components.add( component2 );

        // Evaluate the policy
        List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( applicationId, new Stage( BuildStageType.ID ), Arrays.asList( policy ),
                                            components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        assertFactCounts( 1, 1, policyAlerts.get( 0 ) );

        assertContainsPolicyAlert( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
    }

    @Test
    public void testEvaluateIsNot_Observed()
    {
        LicenseThreatGroup licenseThreatGroup =
            licenseThreatGroupDAO.getByApplicationIdAndLicenseId( applicationId, "GPL-2.0" );

        // Create policy constraints
        Constraint constraint = createConstraint( "is not", licenseThreatGroup.getId() );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        Component component1 = new Component( "g1", "a1", "v1" );
        component1.addObservedLicenseId( "Apache-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component1 );
        components.add( component1 );
        Component component2 = new Component( "g2", "a2", "v2" );
        component2.addObservedLicenseId( "GPL-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component2 );
        components.add( component2 );

        // Evaluate the policy
        List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( applicationId, new Stage( BuildStageType.ID ), Arrays.asList( policy ),
                                            components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        assertFactCounts( 1, 1, policyAlerts.get( 0 ) );

        assertContainsPolicyAlert( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
    }

    @Test
    public void testEvaluateIs_Overridden()
    {
        LicenseThreatGroup licenseThreatGroup =
            licenseThreatGroupDAO.getByApplicationIdAndLicenseId( applicationId, "GPL-2.0" );

        // Create policy constraints
        Constraint constraint = createConstraint( "is", licenseThreatGroup.getId() );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        Component component1 = new Component( "g1", "a1", "v1" );
        component1.addDeclaredLicenseId( "Apache-2.0" );
        component1.addObservedLicenseId( "Apache-2.0" );
        component1.addOverriddenLicenseId( "GPL-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component1 );
        components.add( component1 );
        Component component2 = new Component( "g2", "a2", "v2" );
        component2.addDeclaredLicenseId( "GPL-2.0" );
        component2.addObservedLicenseId( "GPL-2.0" );
        component2.addOverriddenLicenseId( "Apache-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component2 );
        components.add( component2 );

        // Evaluate the policy
        List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( applicationId, new Stage( BuildStageType.ID ), Arrays.asList( policy ),
                                            components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        assertFactCounts( 1, 1, policyAlerts.get( 0 ) );

        assertContainsPolicyAlert( component1, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
    }

    @Test
    public void testEvaluateIsNot_Overridden()
    {
        LicenseThreatGroup licenseThreatGroup =
            licenseThreatGroupDAO.getByApplicationIdAndLicenseId( applicationId, "GPL-2.0" );

        // Create policy constraints
        Constraint constraint = createConstraint( "is not", licenseThreatGroup.getId() );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        Component component1 = new Component( "g1", "a1", "v1" );
        component1.addDeclaredLicenseId( "Apache-2.0" );
        component1.addObservedLicenseId( "Apache-2.0" );
        component1.addOverriddenLicenseId( "GPL-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component1 );
        components.add( component1 );
        Component component2 = new Component( "g2", "a2", "v2" );
        component2.addDeclaredLicenseId( "GPL-2.0" );
        component2.addObservedLicenseId( "GPL-2.0" );
        component2.addOverriddenLicenseId( "Apache-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component2 );
        components.add( component2 );

        // Evaluate the policy
        List<PolicyAlert> policyAlerts =
            new PolicyEvaluator().evaluate( applicationId, new Stage( BuildStageType.ID ), Arrays.asList( policy ),
                                            components );

        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        assertFactCounts( 1, 1, policyAlerts.get( 0 ) );

        assertContainsPolicyAlert( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", policyAlerts );
    }

    @Test
    public void testValidateCondition_InvalidLicenseThreatGroupId()
    {
        Condition condition = new Condition( LicenseThreatGroupConditionType.ID, "is", "abc" );
        try
        {
            new LicenseThreatGroupConditionType().validateCondition( condition, applicationId );
            Assert.fail( "Expected InvalidConditionException" );
        }
        catch ( InvalidConditionException expected )
        {
            if ( !expected.getMessage().endsWith( "Invalid license threat group id: abc" ) )
            {
                throw expected;
            }
        }
    }
}
