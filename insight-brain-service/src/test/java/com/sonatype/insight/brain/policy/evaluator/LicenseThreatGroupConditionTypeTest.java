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
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

public class LicenseThreatGroupConditionTypeTest
    extends AbstractPolicyEvaluationTest
{
    private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

    private ComponentDAO componentDAO = new ComponentDAO();

    private String applicationId;

    @Before
    public void before()
    {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = new Application();
        application.setName( "test" );
        application.setPublicId( "LicenseThreatGroupConditionTypeTest_AppId" );
        applicationDAO.insert( application );
        applicationId = application.getId();

        LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroup( applicationId, "Copyleft", 8 );
        new LicenseThreatGroupDAO().insert( licenseThreatGroup );
        LicenseThreatGroupLicense licenseThreatGroupLicense =
            new LicenseThreatGroupLicense( applicationId, licenseThreatGroup.getId(), "GPL-2.0" );
        new LicenseThreatGroupLicenseDAO().insert( licenseThreatGroupLicense );

        licenseThreatGroup = new LicenseThreatGroup( applicationId, "Liberal", 2 );
        new LicenseThreatGroupDAO().insert( licenseThreatGroup );
        licenseThreatGroupLicense =
            new LicenseThreatGroupLicense( applicationId, licenseThreatGroup.getId(), "Apache-2.0" );
        new LicenseThreatGroupLicenseDAO().insert( licenseThreatGroupLicense );
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
        return createConstraint( "ConstraintId1", "Constraint Name 1", LicenseThreatGroupConditionType.ID, operator,
                                 value );
    }

    @Test
    public void testExplainMatchIs()
    {
        LicenseThreatGroup licenseThreatGroup =
            licenseThreatGroupDAO.getByOwnerIdAndLicenseId( applicationId, "GPL-2.0" );

        Condition condition = new Condition( LicenseThreatGroupConditionType.ID, "is", licenseThreatGroup.getId() );
        Component component1 = new Component( "g1", "a1", "v1", MatchState.EXACT );
        Assert.assertEquals( "Found no License Threat Groups",
                             new LicenseThreatGroupConditionType().explainMatch( condition, component1 ) );

        component1.addDeclaredLicenseId( "Apache-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component1 );
        Assert.assertEquals( "Found 'Liberal' License Threat Group",
                             new LicenseThreatGroupConditionType().explainMatch( condition, component1 ) );

        component1.addDeclaredLicenseId( "GPL-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component1 );
        Assert.assertEquals( "Found 'Liberal' and 'Copyleft' License Threat Groups",
                             new LicenseThreatGroupConditionType().explainMatch( condition, component1 ) );
    }

    @Test
    public void testExplainMatchIsNot()
    {
        LicenseThreatGroup licenseThreatGroup =
            licenseThreatGroupDAO.getByOwnerIdAndLicenseId( applicationId, "GPL-2.0" );

        Condition condition = new Condition( LicenseThreatGroupConditionType.ID, "is not", licenseThreatGroup.getId() );
        Component component1 = new Component( "g1", "a1", "v1", MatchState.EXACT );
        Assert.assertEquals( "Found no License Threat Groups",
                             new LicenseThreatGroupConditionType().explainMatch( condition, component1 ) );

        component1.addDeclaredLicenseId( "Apache-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component1 );
        Assert.assertEquals( "Found 'Liberal' License Threat Group",
                             new LicenseThreatGroupConditionType().explainMatch( condition, component1 ) );

        component1.addDeclaredLicenseId( "GPL-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component1 );
        Assert.assertEquals( "Found 'Liberal' and 'Copyleft' License Threat Groups",
                             new LicenseThreatGroupConditionType().explainMatch( condition, component1 ) );
    }

    @Test
    public void testEvaluateIs_Declared()
    {
        LicenseThreatGroup licenseThreatGroup =
            licenseThreatGroupDAO.getByOwnerIdAndLicenseId( applicationId, "GPL-2.0" );

        // Create policy constraints
        Constraint constraint = createConstraint( "is", licenseThreatGroup.getId() );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

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

        assertContainsPolicyAlert( component2, "PolicyId1", "Policy Name 1", FailActionType.ID, "ConstraintId1",
                                   "Constraint Name 1", LicenseThreatGroupConditionType.ID, policyAlerts );
    }

    @Test
    public void testEvaluateIsNot_Declared()
    {
        LicenseThreatGroup licenseThreatGroup =
            licenseThreatGroupDAO.getByOwnerIdAndLicenseId( applicationId, "GPL-2.0" );

        // Create policy constraints
        Constraint constraint = createConstraint( "is not", licenseThreatGroup.getId() );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

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
                                   "Constraint Name 1", LicenseThreatGroupConditionType.ID, policyAlerts );
    }

    @Test
    public void testEvaluateIs_Observed()
    {
        LicenseThreatGroup licenseThreatGroup =
            licenseThreatGroupDAO.getByOwnerIdAndLicenseId( applicationId, "GPL-2.0" );

        // Create policy constraints
        Constraint constraint = createConstraint( "is", licenseThreatGroup.getId() );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        Component component1 = new Component( "g1", "a1", "v1", MatchState.EXACT );
        component1.addObservedLicenseId( "Apache-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component1 );
        components.add( component1 );
        Component component2 = new Component( "g2", "a2", "v2", MatchState.EXACT );
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
                                   "Constraint Name 1", LicenseThreatGroupConditionType.ID, policyAlerts );
    }

    @Test
    public void testEvaluateIsNot_Observed()
    {
        LicenseThreatGroup licenseThreatGroup =
            licenseThreatGroupDAO.getByOwnerIdAndLicenseId( applicationId, "GPL-2.0" );

        // Create policy constraints
        Constraint constraint = createConstraint( "is not", licenseThreatGroup.getId() );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        Component component1 = new Component( "g1", "a1", "v1", MatchState.EXACT );
        component1.addObservedLicenseId( "Apache-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component1 );
        components.add( component1 );
        Component component2 = new Component( "g2", "a2", "v2", MatchState.EXACT );
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
                                   "Constraint Name 1", LicenseThreatGroupConditionType.ID, policyAlerts );
    }

    @Test
    public void testEvaluateIs_Overridden()
    {
        LicenseThreatGroup licenseThreatGroup =
            licenseThreatGroupDAO.getByOwnerIdAndLicenseId( applicationId, "GPL-2.0" );

        // Create policy constraints
        Constraint constraint = createConstraint( "is", licenseThreatGroup.getId() );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        Component component1 = new Component( "g1", "a1", "v1", MatchState.EXACT );
        component1.addDeclaredLicenseId( "Apache-2.0" );
        component1.addObservedLicenseId( "Apache-2.0" );
        component1.addOverriddenLicenseId( "GPL-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component1 );
        components.add( component1 );
        Component component2 = new Component( "g2", "a2", "v2", MatchState.EXACT );
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
                                   "Constraint Name 1", LicenseThreatGroupConditionType.ID, policyAlerts );
    }

    @Test
    public void testEvaluateIsNot_Overridden()
    {
        LicenseThreatGroup licenseThreatGroup =
            licenseThreatGroupDAO.getByOwnerIdAndLicenseId( applicationId, "GPL-2.0" );

        // Create policy constraints
        Constraint constraint = createConstraint( "is not", licenseThreatGroup.getId() );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

        List<Component> components = new ArrayList<Component>();
        Component component1 = new Component( "g1", "a1", "v1", MatchState.EXACT );
        component1.addDeclaredLicenseId( "Apache-2.0" );
        component1.addObservedLicenseId( "Apache-2.0" );
        component1.addOverriddenLicenseId( "GPL-2.0" );
        componentDAO.loadLicenseThreatGroups( applicationId, component1 );
        components.add( component1 );
        Component component2 = new Component( "g2", "a2", "v2", MatchState.EXACT );
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
                                   "Constraint Name 1", LicenseThreatGroupConditionType.ID, policyAlerts );
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

    @Test
    public void testEvaluate_LicenseThreatGroupFromOrganization()
    {
        Organization organization = new Organization();
        organization.setName( "testEvaluate-LicenseThreatGroupFromOrganization" );
        new OrganizationDAO().insert( organization, false /* createLicenseThreatGroups */);

        LicenseThreatGroup orgLicenseThreatGroup =
            new LicenseThreatGroup( organization.getId(), "testEvaluate-LicenseThreatGroupFromOrganization", 5 );
        new LicenseThreatGroupDAO().insert( orgLicenseThreatGroup );
        LicenseThreatGroupLicense licenseThreatGroupLicense =
            new LicenseThreatGroupLicense( organization.getId(), orgLicenseThreatGroup.getId(), "Apache-2.0" );
        new LicenseThreatGroupLicenseDAO().insert( licenseThreatGroupLicense );

        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByIdNotNull( applicationId );
        application.setOrganizationId( organization.getId() );
        applicationDAO.update( application );

        // Create policy constraints
        Constraint constraint = createConstraint( "is", orgLicenseThreatGroup.getId() );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );

        // Create policy
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );

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
                                   "Constraint Name 1", LicenseThreatGroupConditionType.ID, policyAlerts );
    }
}
