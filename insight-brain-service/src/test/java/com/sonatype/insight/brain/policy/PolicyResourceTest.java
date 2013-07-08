/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ning.http.client.Response;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.Color;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ValidationResult;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.PolicyResource.ApplicablePolicies;
import com.sonatype.insight.brain.policy.PolicyResource.PoliciesByOwner;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class PolicyResourceTest
    extends AbstractResourceTest
{
    private static final String APP = "application";

    private static final String ORG = "organization";

    @Test
    public void testExportImport_Insert()
        throws Exception
    {
        String applicationPublicId = "PolicyResourceTest-testExportImport-Insert";
        Application application = createApplication( applicationPublicId, false /* createLicenseThreatGroups */);
        String appId = application.getId();

        Label label = new Label( appId, "label1", Color.blue );
        LabelDAO labelDAO = new LabelDAO();
        labelDAO.insert( label );

        LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroup( appId, "LicenseThreatGroup1", 3 );
        LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
        licenseThreatGroupDAO.insert( licenseThreatGroup );

        LicenseThreatGroupLicense licenseThreatGroupLicense =
            new LicenseThreatGroupLicense( appId, licenseThreatGroup.getId(), "GPL-2.0" );
        LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();
        licenseThreatGroupLicenseDAO.insert( licenseThreatGroupLicense );

        Policy policy = new Policy();
        policy.setName( "Policy1" );
        Constraint constraint1 = new Constraint();
        constraint1.setName( "Constraint1" );
        constraint1.addCondition( new Condition( LabelConditionType.ID, "is", label.getId() ) );
        policy.addConstraint( constraint1 );
        Constraint constraint2 = new Constraint();
        constraint2.setName( "Constraint2" );
        constraint2.addCondition( new Condition( LicenseThreatGroupConditionType.ID, "is", licenseThreatGroup.getId() ) );
        policy.addConstraint( constraint2 );
        policy.addAction( BuildStageType.ID, new Action( Action.ID_FAIL ) );
        Response response = RestAccess.post( getServiceURL( APP, applicationPublicId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );

        // Export
        response = RestAccess.get( getServiceURL( "application", applicationPublicId ) + "/export" );
        assertResponseStatus( 200, response );
        PolicyExportResult policyExportResult =
            JsonHelpers.fromJson( response.getResponseBody(), PolicyExportResult.class );
        Assert.assertNotNull( policyExportResult );
        Assert.assertTrue( !policyExportResult.policies.isEmpty() );
        Assert.assertTrue( !policyExportResult.labels.isEmpty() );
        Assert.assertTrue( !policyExportResult.licenseThreatGroups.isEmpty() );
        Assert.assertTrue( !policyExportResult.licenseThreatGroupLicenses.isEmpty() );

        // Import
        String newApplicationPublicId = applicationPublicId + "1";
        response = RestAccess.put( getServiceURL( APP, newApplicationPublicId ) + "/import", JsonHelpers.asJson( policyExportResult ) );
        assertResponseStatus( 200, response );
        PolicyImportResult policyImportResult =
            JsonHelpers.fromJson( response.getResponseBody(), PolicyImportResult.class );
        Assert.assertNotNull( policyImportResult );
        Assert.assertEquals( newApplicationPublicId, policyImportResult.applicationName );
        Assert.assertTrue( policyImportResult.applicationURL,
                           policyImportResult.applicationURL.endsWith( "/policy-assets/index.html?appId="
                               + newApplicationPublicId ) );
        application = new ApplicationDAO().getByName( policyImportResult.applicationName );
        applicationsToDelete.add( application );
        Assert.assertNotNull( application );
        List<Label> labels = labelDAO.getByApplicationId( application.getId() );
        Assert.assertEquals( 1, labels.size() );
        Assert.assertEquals( label.getLabel(), labels.get( 0 ).getLabel() );
        Assert.assertEquals( label.getColor(), labels.get( 0 ).getColor() );
        List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId( application.getId() );
        Assert.assertEquals( 1, licenseThreatGroups.size() );
        Assert.assertEquals( licenseThreatGroup.getName(), licenseThreatGroups.get( 0 ).getName() );
        List<LicenseThreatGroupLicense> licenseThreatGroupLicenses =
            licenseThreatGroupLicenseDAO.getByOwnerId( application.getId() );
        Assert.assertEquals( 1, licenseThreatGroupLicenses.size() );
        Assert.assertEquals( licenseThreatGroupLicense.getLicenseId(),
                             licenseThreatGroupLicenses.get( 0 ).getLicenseId() );
        response = RestAccess.get( getServiceURL( APP, newApplicationPublicId ) );
        assertResponseStatus( 200, response );
        Policy[] policies = JsonHelpers.fromJson( response.getResponseBody(), Policy[].class );
        Assert.assertEquals( 1, policies.length );
        Assert.assertEquals( policy.getName(), policies[0].getName() );
        ValidationResult policyValidationResult = policies[0].validate( application.getId() );
        Assert.assertTrue( policyValidationResult.toMessageString(), policyValidationResult.isValid() );

        // Import again for a different application
        newApplicationPublicId = applicationPublicId + "2";
        response = RestAccess.put( getServiceURL( APP, newApplicationPublicId ) + "/import", JsonHelpers.asJson( policyExportResult ) );
        assertResponseStatus( 200, response );
        policyImportResult = JsonHelpers.fromJson( response.getResponseBody(), PolicyImportResult.class );
        Assert.assertNotNull( policyImportResult );
        Assert.assertEquals( newApplicationPublicId, policyImportResult.applicationName );
        Assert.assertTrue( policyImportResult.applicationURL,
                           policyImportResult.applicationURL.endsWith( "/policy-assets/index.html?appId="
                               + newApplicationPublicId ) );
        application = new ApplicationDAO().getByName( policyImportResult.applicationName );
        applicationsToDelete.add( application );
        Assert.assertNotNull( application );
        labels = labelDAO.getByApplicationId( application.getId() );
        Assert.assertEquals( 1, labels.size() );
        Assert.assertEquals( label.getLabel(), labels.get( 0 ).getLabel() );
        Assert.assertEquals( label.getColor(), labels.get( 0 ).getColor() );
        licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId( application.getId() );
        Assert.assertEquals( 1, licenseThreatGroups.size() );
        Assert.assertEquals( licenseThreatGroup.getName(), licenseThreatGroups.get( 0 ).getName() );
        licenseThreatGroupLicenses = licenseThreatGroupLicenseDAO.getByOwnerId( application.getId() );
        Assert.assertEquals( 1, licenseThreatGroupLicenses.size() );
        Assert.assertEquals( licenseThreatGroupLicense.getLicenseId(),
                             licenseThreatGroupLicenses.get( 0 ).getLicenseId() );
        response = RestAccess.get( getServiceURL( APP, newApplicationPublicId ) );
        assertResponseStatus( 200, response );
        policies = JsonHelpers.fromJson( response.getResponseBody(), Policy[].class );
        Assert.assertEquals( 1, policies.length );
        Assert.assertEquals( policy.getName(), policies[0].getName() );
        policyValidationResult = policies[0].validate( application.getId() );
        Assert.assertTrue( policyValidationResult.toMessageString(), policyValidationResult.isValid() );
    }

    @Test
    public void testExportImport_Update()
        throws Exception
    {
        String applicationPublicId = "PolicyResourceTest-testExportImport-Update";
        Application application = createApplication( applicationPublicId, false /* createLicenseThreatGroups */);
        String appId = application.getId();

        LabelDAO labelDAO = new LabelDAO();
        Label label1 = new Label( appId, "label1", Color.blue );
        labelDAO.insert( label1 );
        Label label2 = new Label( appId, "label2", Color.red );
        labelDAO.insert( label2 );

        LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroup( appId, "LicenseThreatGroup1", 3 );
        LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
        licenseThreatGroupDAO.insert( licenseThreatGroup );

        LicenseThreatGroupLicense licenseThreatGroupLicense =
            new LicenseThreatGroupLicense( appId, licenseThreatGroup.getId(), "GPL-2.0" );
        LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();
        licenseThreatGroupLicenseDAO.insert( licenseThreatGroupLicense );

        Policy policy = new Policy();
        policy.setName( "Policy1" );
        Constraint constraint1 = new Constraint();
        constraint1.setName( "Constraint1" );
        constraint1.addCondition( new Condition( LabelConditionType.ID, "is", label1.getId() ) );
        policy.addConstraint( constraint1 );
        Constraint constraint2 = new Constraint();
        constraint2.setName( "Constraint2" );
        constraint2.addCondition( new Condition( LicenseThreatGroupConditionType.ID, "is", licenseThreatGroup.getId() ) );
        policy.addConstraint( constraint2 );
        policy.addAction( BuildStageType.ID, new Action( Action.ID_FAIL ) );
        Response response = RestAccess.post( getServiceURL( APP, applicationPublicId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );

        // Export
        response = RestAccess.get( getServiceURL( APP, applicationPublicId ) + "/export" );
        assertResponseStatus( 200, response );
        PolicyExportResult policyExportResult =
            JsonHelpers.fromJson( response.getResponseBody(), PolicyExportResult.class );
        Assert.assertNotNull( policyExportResult );
        Assert.assertTrue( !policyExportResult.policies.isEmpty() );
        Assert.assertTrue( !policyExportResult.labels.isEmpty() );
        Assert.assertTrue( !policyExportResult.licenseThreatGroups.isEmpty() );
        Assert.assertTrue( !policyExportResult.licenseThreatGroupLicenses.isEmpty() );

        // Delete and re-create one label - it should be reset by import (matched by label case insensitive)
        labelDAO.delete( label1 );
        label1 = new Label( appId, label1.getLabel().toUpperCase( Locale.ENGLISH ), Color.black );
        labelDAO.insert( label1 );
        // Delete one label - it should be re-created by the import.
        labelDAO.delete( label2 );
        // Add a new label - it should be deleted by the import.
        Label label3 = new Label( appId, "label3", Color.red );
        labelDAO.insert( label3 );

        // Import
        response = RestAccess.put( getServiceURL( APP, applicationPublicId ) + "/import", JsonHelpers.asJson( policyExportResult ) );
        assertResponseStatus( 200, response );
        PolicyImportResult policyImportResult =
            JsonHelpers.fromJson( response.getResponseBody(), PolicyImportResult.class );
        Assert.assertNotNull( policyImportResult );
        Assert.assertEquals( application.getName(), policyImportResult.applicationName );
        Assert.assertTrue( policyImportResult.applicationURL,
                           policyImportResult.applicationURL.endsWith( "/policy-assets/index.html?appId="
                               + applicationPublicId ) );
        application = new ApplicationDAO().getByName( policyImportResult.applicationName );
        applicationsToDelete.add( application );
        Assert.assertNotNull( application );
        List<Label> labels = labelDAO.getByApplicationId( application.getId() );
        Assert.assertEquals( 2, labels.size() );
        Assert.assertEquals( label1.getId(), labels.get( 0 ).getId() );
        Assert.assertEquals( "label1", labels.get( 0 ).getLabel() );
        Assert.assertEquals( Color.blue, labels.get( 0 ).getColor() );
        Assert.assertNotEquals( label2.getId(), labels.get( 1 ).getId() );
        Assert.assertEquals( label2.getLabel(), labels.get( 1 ).getLabel() );
        Assert.assertEquals( label2.getColor(), labels.get( 1 ).getColor() );
        List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerId( application.getId() );
        Assert.assertEquals( 1, licenseThreatGroups.size() );
        Assert.assertEquals( licenseThreatGroup.getName(), licenseThreatGroups.get( 0 ).getName() );
        Assert.assertNotEquals( licenseThreatGroup.getId(), licenseThreatGroups.get( 0 ).getId() );
        List<LicenseThreatGroupLicense> licenseThreatGroupLicenses =
            licenseThreatGroupLicenseDAO.getByOwnerId( application.getId() );
        Assert.assertEquals( 1, licenseThreatGroupLicenses.size() );
        Assert.assertEquals( licenseThreatGroupLicense.getLicenseId(),
                             licenseThreatGroupLicenses.get( 0 ).getLicenseId() );
        Assert.assertNotEquals( licenseThreatGroupLicense.getId(), licenseThreatGroupLicenses.get( 0 ).getId() );
        response = RestAccess.get( getServiceURL( APP, applicationPublicId ) );
        assertResponseStatus( 200, response );
        Policy[] policies = JsonHelpers.fromJson( response.getResponseBody(), Policy[].class );
        Assert.assertEquals( 1, policies.length );
        Assert.assertEquals( policy.getName(), policies[0].getName() );
        Assert.assertNotEquals( policy.getId(), policies[0].getId() );
        ValidationResult policyValidationResult = policies[0].validate( application.getId() );
        Assert.assertTrue( policyValidationResult.toMessageString(), policyValidationResult.isValid() );
    }

    @Test
    public void testExportImport_ExceedsLicensedApplicationCount()
        throws Exception
    {
        setApplicationLimit( 1 );

        String applicationPublicId = "testExportImport_ExceedsLicensedApplicationCount";
        createApplication( applicationPublicId, false /* createLicenseThreatGroups */);

        // Export
        Response response = RestAccess.get( getServiceURL( APP, applicationPublicId ) + "/export" );
        assertResponseStatus( 200, response );
        PolicyExportResult policyExportResult =
            JsonHelpers.fromJson( response.getResponseBody(), PolicyExportResult.class );
        Assert.assertNotNull( policyExportResult );

        // Import
        response = RestAccess.put( getServiceURL( APP, applicationPublicId + "1" ) + "/import", JsonHelpers.asJson( policyExportResult ) );
        assertResponseStatus( 402, response );
        Assert.assertEquals( "You have exceeded the licensed limit of 1 applications.", response.getResponseBody() );
    }

    @Test
    public void testCRUD_ApplicationLevel()
        throws Exception
    {
        String applicationPublicId = "PolicyResourceTest_testCRUD";
        Application application = createApplication( applicationPublicId );
        String appId = application.getId();

        JsonStore store = JsonUtils.fileStore( new File( brain.getWorkDir(), "policy/" + appId ) );

        testCRUD( APP, applicationPublicId, store );
    }

    @Test
    public void testCRUD_OrganizationLevel()
        throws Exception
    {
        String orgId = createOrganization( "test" ).getId();

        JsonStore store = JsonUtils.fileStore( new File( brain.getWorkDir(), "policy/" + orgId ) );

        testCRUD( ORG, orgId, store );
    }

    private void testCRUD( String ownerType, String ownerId, JsonStore store )
        throws Exception
    {
        Assert.assertEquals( 0, store.modificationCount() );

        // Add a policy
        Policy policy = new Policy();
        policy.setName( "PolicyResourceTest new policy" );
        Constraint constraint = new Constraint();
        constraint.setName( "PolicyResourceTest new constraint" );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        Response response = RestAccess.post( getServiceURL( ownerType, ownerId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        final Policy policy1 = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );
        Assert.assertNotNull( policy1.getId() );
        Assert.assertEquals( "PolicyResourceTest new policy", policy1.getName() );

        Assert.assertEquals( 1, store.modificationCount() );

        // Get all policies
        response = RestAccess.get( getServiceURL( ownerType, ownerId ) );
        assertResponseStatus( 200, response );
        Policy[] policies = JsonHelpers.fromJson( response.getResponseBody(), Policy[].class );
        Assert.assertNotNull( policies );
        Assert.assertEquals( 1, policies.length );
        Assert.assertEquals( policy1.getId(), policies[0].getId() );
        Assert.assertEquals( policy1.getName(), policies[0].getName() );

        Assert.assertEquals( 1, store.modificationCount() );

        ObjectNode json;

        json = (ObjectNode) store.history( null, "policy.json" ).get( "aaData" ).get( 0 );
        json = json.without( Arrays.asList( "user", "ip", "where", "time", "filename" ) );
        Assert.assertEquals( JsonUtils.asTree( policy1 ), json );

        // Update a policy
        policy = policies[0];
        policy.setName( "PolicyResourceTest updated policy" );
        response = RestAccess.put( getServiceURL( ownerType, ownerId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        final Policy policy2 = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );
        Assert.assertEquals( "PolicyResourceTest updated policy", policy2.getName() );

        Assert.assertEquals( 2, store.modificationCount() );

        // Get all policies
        response = RestAccess.get( getServiceURL( ownerType, ownerId ) );
        assertResponseStatus( 200, response );
        policies = JsonHelpers.fromJson( response.getResponseBody(), Policy[].class );
        Assert.assertNotNull( policies );
        Assert.assertEquals( 1, policies.length );
        Assert.assertEquals( policy2.getId(), policies[0].getId() );
        Assert.assertEquals( policy2.getName(), policies[0].getName() );

        Assert.assertEquals( 2, store.modificationCount() );

        json = (ObjectNode) store.history( null, "policy.json" ).get( "aaData" ).get( 0 );
        json = json.without( Arrays.asList( "user", "ip", "where", "time", "filename" ) );
        Assert.assertEquals( JsonUtils.asTree( policy2 ), json );

        json = (ObjectNode) store.history( null, "policy.json" ).get( "aaData" ).get( 1 );
        json = json.without( Arrays.asList( "user", "ip", "where", "time", "filename" ) );
        Assert.assertEquals( JsonUtils.asTree( policy1 ), json );

        // Delete a policy
        policy = policies[0];
        response = RestAccess.delete( getServiceURL( ownerType, ownerId, policy.getId() ) );
        assertResponseStatus( 204, response );

        Assert.assertEquals( 3, store.modificationCount() );

        // Get all policies
        response = RestAccess.get( getServiceURL( ownerType, ownerId ) );
        assertResponseStatus( 200, response );
        policies = JsonHelpers.fromJson( response.getResponseBody(), Policy[].class );
        Assert.assertNotNull( policies );
        Assert.assertEquals( 0, policies.length );

        Assert.assertEquals( 3, store.modificationCount() );

        json = (ObjectNode) store.history( null, "policy.json" ).get( "aaData" ).get( 0 );
        json = json.without( Arrays.asList( "user", "ip", "where", "time", "filename" ) );
        Assert.assertEquals( JsonUtils.asTree( policy2 ), json );

        json = (ObjectNode) store.history( null, "policy.json" ).get( "aaData" ).get( 1 );
        json = json.without( Arrays.asList( "user", "ip", "where", "time", "filename" ) );
        Assert.assertEquals( JsonUtils.asTree( policy1 ), json );
    }

    @Test
    public void testCreateInvalidPolicy_AppLevel()
        throws Exception
    {
        String applicationPublicId = "PolicyResourceTest_testCreateInvalidPolicy";
        Application application = createApplication( applicationPublicId );
        String appId = application.getId();
        JsonStore store = JsonUtils.fileStore( new File( brain.getWorkDir(), "policy/" + appId ) );
        testCreateInvalidPolicy( APP, applicationPublicId, store );
    }

    @Test
    public void testCreateInvalidPolicy_OrgLevel()
        throws Exception
    {
        String orgId = createOrganization( "test" ).getId();
        JsonStore store = JsonUtils.fileStore( new File( brain.getWorkDir(), "policy/" + orgId ) );
        testCreateInvalidPolicy( ORG, orgId, store );
    }

    private void testCreateInvalidPolicy( String ownerType, String ownerId, JsonStore store )
        throws Exception
    {
        Assert.assertEquals( 0, store.modificationCount() );

        Policy policy = new Policy();
        policy.setName( null );
        Constraint constraint = new Constraint();
        constraint.setName( "PolicyResourceTest new constraint" );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        Response response = RestAccess.post( getServiceURL( ownerType, ownerId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 400, response );
        Assert.assertEquals( "The policy name must not be null or empty", response.getResponseBody() );
    }

    @Test
    public void testUpdateInvalidPolicy_AppLevel()
        throws Exception
    {
        String applicationPublicId = "PolicyResourceTest_testUpdateInvalidPolicy";
        Application application = createApplication( applicationPublicId );
        String appId = application.getId();
        JsonStore store = JsonUtils.fileStore( new File( brain.getWorkDir(), "policy/" + appId ) );
        testUpdateInvalidPolicy( APP, applicationPublicId, store );
    }

    @Test
    public void testUpdateInvalidPolicy_OrgLevel()
        throws Exception
    {
        String orgId = createOrganization( "test" ).getId();
        JsonStore store = JsonUtils.fileStore( new File( brain.getWorkDir(), "policy/" + orgId ) );
        testUpdateInvalidPolicy( ORG, orgId, store );
    }

    private void testUpdateInvalidPolicy(String ownerType, String ownerId, JsonStore store)
        throws Exception
    {
        Assert.assertEquals( 0, store.modificationCount() );

        // Create a valid policy
        Policy policy = new Policy();
        policy.setName( "PolicyResourceTest_testUpdateInvalidPolicy" );
        Constraint constraint = new Constraint();
        constraint.setName( "PolicyResourceTest new constraint" );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        Response response = RestAccess.post( getServiceURL( ownerType, ownerId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        policy = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );

        // Update invalid policy
        policy.setName( null );
        response = RestAccess.put( getServiceURL( ownerType, ownerId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 400, response );
        Assert.assertEquals( "The policy name must not be null or empty", response.getResponseBody() );
    }

    private void assertPoliciesByOwner( String ownerId, String ownerName, String ownerType, int policyCount,
                                        PoliciesByOwner actual )
    {

    }

    @Test
    public void testGetApplicablePolicies()
        throws Exception
    {
        // Create an organization and an application
        String orgName = "testGetApplicablePoliciesOrg";
        String orgId = createOrganization( orgName ).getId();
        String appName = "testGetApplicablePoliciesApp";
        String appPublicId = appName;
        Application app = new Application( appPublicId, appName, orgId );
        ApplicationDAO appDAO = new ApplicationDAO();
        appDAO.insert( app );
        applicationsToDelete.add( app );
        String appId = app.getId();

        // Verify the applicable policies for the application
        Response response = RestAccess.get( getServiceURL( APP, appPublicId ) + "/applicable" );
        assertResponseStatus( 200, response );
        ApplicablePolicies applicablePolicies =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicablePolicies.class );
        Assert.assertNotNull( applicablePolicies );
        Assert.assertEquals( 2, applicablePolicies.policiesByOwner.size() );
        assertPoliciesByOwner( appId, appName, "application", 0, applicablePolicies.policiesByOwner.get( 0 ) );
        assertPoliciesByOwner( orgId, orgName, "organization", 0, applicablePolicies.policiesByOwner.get( 1 ) );

        // Verify the applicable policies for the organization
        response = RestAccess.get( getServiceURL( ORG, orgId ) + "/applicable" );
        assertResponseStatus( 200, response );
        applicablePolicies = JsonHelpers.fromJson( response.getResponseBody(), ApplicablePolicies.class );
        Assert.assertNotNull( applicablePolicies );
        Assert.assertEquals( 1, applicablePolicies.policiesByOwner.size() );
        assertPoliciesByOwner( orgId, orgName, "organization", 0, applicablePolicies.policiesByOwner.get( 0 ) );

        // Create a policy for the application
        Policy appPolicy = new Policy();
        appPolicy.setName( "testGetApplicablePolicies App Policy" );
        Constraint constraint = new Constraint();
        constraint.setName( "testGetApplicablePolicies App constraint" );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        appPolicy.addConstraint( constraint );
        response = RestAccess.post( getServiceURL( APP, appPublicId ), JsonHelpers.asJson( appPolicy ) );
        assertResponseStatus( 200, response );
        appPolicy = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );

        // Verify the applicable policies for the application
        response = RestAccess.get( getServiceURL( APP, appPublicId ) + "/applicable" );
        assertResponseStatus( 200, response );
        applicablePolicies = JsonHelpers.fromJson( response.getResponseBody(), ApplicablePolicies.class );
        Assert.assertNotNull( applicablePolicies );
        Assert.assertEquals( 2, applicablePolicies.policiesByOwner.size() );
        assertPoliciesByOwner( appId, appName, "application", 1, applicablePolicies.policiesByOwner.get( 0 ) );
        assertPoliciesByOwner( orgId, orgName, "organization", 0, applicablePolicies.policiesByOwner.get( 1 ) );
        Assert.assertEquals( appPolicy.getId(), applicablePolicies.policiesByOwner.get( 0 ).policies.get( 0 ).getId() );

        // Verify the applicable policies for the organization
        response = RestAccess.get( getServiceURL( ORG, orgId ) + "/applicable" );
        assertResponseStatus( 200, response );
        applicablePolicies = JsonHelpers.fromJson( response.getResponseBody(), ApplicablePolicies.class );
        Assert.assertNotNull( applicablePolicies );
        Assert.assertEquals( 1, applicablePolicies.policiesByOwner.size() );
        assertPoliciesByOwner( orgId, orgName, "organization", 0, applicablePolicies.policiesByOwner.get( 0 ) );

        // Create a policy for the organization
        Policy orgPolicy = new Policy();
        orgPolicy.setName( "testGetApplicablePolicies Org Policy" );
        constraint = new Constraint();
        constraint.setName( "testGetApplicablePolicies Org constraint" );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        orgPolicy.addConstraint( constraint );
        response = RestAccess.post( getServiceURL( ORG, orgId ), JsonHelpers.asJson( orgPolicy ) );
        assertResponseStatus( 200, response );
        orgPolicy = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );

        // Verify the applicable policies for the application
        response = RestAccess.get( getServiceURL( APP, appPublicId ) + "/applicable" );
        assertResponseStatus( 200, response );
        applicablePolicies = JsonHelpers.fromJson( response.getResponseBody(), ApplicablePolicies.class );
        Assert.assertNotNull( applicablePolicies );
        Assert.assertEquals( 2, applicablePolicies.policiesByOwner.size() );
        assertPoliciesByOwner( appId, appName, "application", 1, applicablePolicies.policiesByOwner.get( 0 ) );
        assertPoliciesByOwner( orgId, orgName, "organization", 0, applicablePolicies.policiesByOwner.get( 1 ) );
        Assert.assertEquals( appPolicy.getId(), applicablePolicies.policiesByOwner.get( 0 ).policies.get( 0 ).getId() );
        Assert.assertEquals( orgPolicy.getId(), applicablePolicies.policiesByOwner.get( 1 ).policies.get( 0 ).getId() );

        // Verify the applicable policies for the organization
        response = RestAccess.get( getServiceURL( ORG, orgId ) + "/applicable" );
        assertResponseStatus( 200, response );
        applicablePolicies = JsonHelpers.fromJson( response.getResponseBody(), ApplicablePolicies.class );
        Assert.assertNotNull( applicablePolicies );
        Assert.assertEquals( 1, applicablePolicies.policiesByOwner.size() );
        assertPoliciesByOwner( orgId, orgName, "organization", 0, applicablePolicies.policiesByOwner.get( 0 ) );
        Assert.assertEquals( orgPolicy.getId(), applicablePolicies.policiesByOwner.get( 0 ).policies.get( 0 ).getId() );
    }

    private String getServiceURL( final String ownerType, final String ownerId )
    {
        return getRestBaseUrl() + expandRestUrl( PolicyResource.SERVICE_PATH, ownerType, ownerId );
    }

    private String getServiceURL( final String ownerType, final String ownerId, final String policyId )
    {
        return getServiceURL( ownerType, ownerId ) + "/" + policyId;
    }
}
