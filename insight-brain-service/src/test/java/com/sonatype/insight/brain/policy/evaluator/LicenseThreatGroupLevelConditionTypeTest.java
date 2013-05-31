/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
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

    private String applicationId;

    private LicenseThreatGroup licenseThreatGroup2;

    private LicenseThreatGroup licenseThreatGroup5;

    @Before
    public void before()
    {
        Application application = new Application();
        application.setName( "test" );
        application.setPublicId( "LicenseThreatGroupLevelConditionTypeTest_AppId" );
        new ApplicationDAO().insert( application, false /* createLicenseThreatGroups */);
        applicationId = application.getId();

        licenseThreatGroup2 = new LicenseThreatGroup( applicationId, "Level 2", 2 );
        licenseThreatGroupDAO.insert( licenseThreatGroup2 );
        licenseThreatGroup5 = new LicenseThreatGroup( applicationId, "Level 5", 5 );
        licenseThreatGroupDAO.insert( licenseThreatGroup5 );
    }

    @After
    public void after()
    {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByIdNotNull( applicationId );
        String organizationId = application.getOrganizationId();
        applicationDAO.delete( application );

        if ( organizationId != null )
        {
            OrganizationDAO organizationDAO = new OrganizationDAO();
            organizationDAO.delete( organizationDAO.getByIdNotNull( organizationId ) );
        }
    }

    private Constraint createConstraint( String operator, String value )
    {
        return createConstraint( "ConstraintId1", "Constraint Name 1", LicenseThreatGroupLevelConditionType.ID,
                                 operator, value );
    }

    @Test
    public void testExplainMatchLessOrEqual()
    {
        Condition condition = new Condition( LicenseThreatGroupLevelConditionType.ID, "<=", "5" );
        Component component1 = new Component( "g1", "a1", "v1", MatchState.EXACT );
        Assert.assertEquals( "Found no License Threat Groups with Level <= 5",
                             new LicenseThreatGroupLevelConditionType().explainMatch( condition, component1 ) );

        component1.addLicenseThreatGroup( licenseThreatGroup2 );
        Assert.assertEquals( "Found 'Level 2' License Threat Group with Level <= 5",
                             new LicenseThreatGroupLevelConditionType().explainMatch( condition, component1 ) );

        component1.addLicenseThreatGroup( licenseThreatGroup5 );
        Assert.assertEquals( "Found 'Level 2' and 'Level 5' License Threat Groups with Level <= 5",
                             new LicenseThreatGroupLevelConditionType().explainMatch( condition, component1 ) );

        condition.setValue( "2" );
        Assert.assertEquals( "Found 'Level 2' License Threat Group with Level <= 2",
                             new LicenseThreatGroupLevelConditionType().explainMatch( condition, component1 ) );
    }

    @Test
    public void testExplainMatchGreaterOrEqual()
    {
        Condition condition = new Condition( LicenseThreatGroupLevelConditionType.ID, ">=", "2" );
        Component component1 = new Component( "g1", "a1", "v1", MatchState.EXACT );
        Assert.assertEquals( "Found no License Threat Groups with Level >= 2",
                             new LicenseThreatGroupLevelConditionType().explainMatch( condition, component1 ) );

        component1.addLicenseThreatGroup( licenseThreatGroup2 );
        Assert.assertEquals( "Found 'Level 2' License Threat Group with Level >= 2",
                             new LicenseThreatGroupLevelConditionType().explainMatch( condition, component1 ) );

        component1.addLicenseThreatGroup( licenseThreatGroup5 );
        Assert.assertEquals( "Found 'Level 2' and 'Level 5' License Threat Groups with Level >= 2",
                             new LicenseThreatGroupLevelConditionType().explainMatch( condition, component1 ) );

        condition.setValue( "5" );
        Assert.assertEquals( "Found 'Level 5' License Threat Group with Level >= 5",
                             new LicenseThreatGroupLevelConditionType().explainMatch( condition, component1 ) );
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

    @Test
    public void testEvaluate_LicenseThreatGroupFromOrganization()
    {
        Organization organization = new Organization();
        organization.setName( "testEvaluate-LicenseThreatGroupFromOrganization" );
        new OrganizationDAO().insert( organization, false /* createLicenseThreatGroups */);

        LicenseThreatGroup orgLicenseThreatGroup =
            new LicenseThreatGroup( organization.getId(), "testEvaluate-LicenseThreatGroupFromOrganization", 7 );
        new LicenseThreatGroupDAO().insert( orgLicenseThreatGroup );
        LicenseThreatGroupLicense licenseThreatGroupLicense =
            new LicenseThreatGroupLicense( organization.getId(), orgLicenseThreatGroup.getId(), "Apache-2.0" );
        new LicenseThreatGroupLicenseDAO().insert( licenseThreatGroupLicense );

        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByIdNotNull( applicationId );
        application.setOrganizationId( organization.getId() );
        applicationDAO.update( application );

        // Create policy constraints
        Constraint constraint = createConstraint( ">=", "7" );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        ComponentDAO componentDAO = new ComponentDAO();
        List<Component> components = new ArrayList<Component>();
        Component component1 = new Component( "g1", "a1", "v1", MatchState.EXACT );
        component1.addDeclaredLicenseId( "Apache-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component1 );
        components.add( component1 );
        Component component2 = new Component( "g2", "a2", "v2", MatchState.EXACT );
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
                                   "Constraint Name 1", LicenseThreatGroupLevelConditionType.ID, policyAlerts );
    }
}
