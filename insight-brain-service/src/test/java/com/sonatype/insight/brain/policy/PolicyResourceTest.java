/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.File;
import java.util.Arrays;

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
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class PolicyResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testExportImport()
        throws Exception
    {
        String applicationPublicId = "PolicyResourceTest_testExportImport";
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
        Response response = RestAccess.post( getServiceURL( applicationPublicId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );

        // Export
        response = RestAccess.get( getServiceURL( applicationPublicId ) + "/export" );
        assertResponseStatus( 200, response );
        PolicyExportResult policyExportResult =
            JsonHelpers.fromJson( response.getResponseBody(), PolicyExportResult.class );
        Assert.assertNotNull( policyExportResult );
        Assert.assertNotNull( policyExportResult.filename );
        File exportFile = new File( policyExportResult.filename );
        Assert.assertTrue( exportFile.getAbsolutePath(), exportFile.exists() );

        // Import
        String newApplicationPublicId = applicationPublicId + "1";
        response = RestAccess.put( getServiceURL( newApplicationPublicId ) + "/import", exportFile );
        assertResponseStatus( 200, response );
        PolicyImportResult policyImportResult =
            JsonHelpers.fromJson( response.getResponseBody(), PolicyImportResult.class );
        Assert.assertNotNull( policyImportResult );
        Assert.assertEquals( PolicyResource.IMPORT_APPLICATION_NAME, policyImportResult.applicationName );
        Assert.assertTrue( policyImportResult.applicationURL,
                           policyImportResult.applicationURL.endsWith( "/policy-assets/index.html?appId="
                               + newApplicationPublicId ) );
        application = new ApplicationDAO().getByName( policyImportResult.applicationName );
        applicationsToDelete.add( application );
        Assert.assertNotNull( application );
        Assert.assertEquals( 1, labelDAO.getByApplicationId( application.getId() ).size() );
        Assert.assertEquals( 1, licenseThreatGroupDAO.getByApplicationId( application.getId() ).size() );
        Assert.assertEquals( 1, licenseThreatGroupLicenseDAO.getByApplicationId( application.getId() ).size() );
        response = RestAccess.get( getServiceURL( newApplicationPublicId ) );
        assertResponseStatus( 200, response );
        Policy[] policies = JsonHelpers.fromJson( response.getResponseBody(), Policy[].class );
        Assert.assertEquals( 1, policies.length );

        // Import again for a different application
        newApplicationPublicId = applicationPublicId + "2";
        response = RestAccess.put( getServiceURL( newApplicationPublicId ) + "/import", exportFile );
        assertResponseStatus( 200, response );
        policyImportResult = JsonHelpers.fromJson( response.getResponseBody(), PolicyImportResult.class );
        Assert.assertNotNull( policyImportResult );
        Assert.assertFalse( PolicyResource.IMPORT_APPLICATION_NAME.equals( policyImportResult.applicationName ) );
        Assert.assertTrue( policyImportResult.applicationName.startsWith( PolicyResource.IMPORT_APPLICATION_NAME + " " ) );
        Assert.assertTrue( policyImportResult.applicationURL,
                           policyImportResult.applicationURL.endsWith( "/policy-assets/index.html?appId="
                               + newApplicationPublicId ) );
        application = new ApplicationDAO().getByName( policyImportResult.applicationName );
        applicationsToDelete.add( application );
        Assert.assertNotNull( application );
        Assert.assertEquals( 1, labelDAO.getByApplicationId( application.getId() ).size() );
        Assert.assertEquals( 1, licenseThreatGroupDAO.getByApplicationId( application.getId() ).size() );
        Assert.assertEquals( 1, licenseThreatGroupLicenseDAO.getByApplicationId( application.getId() ).size() );
        response = RestAccess.get( getServiceURL( newApplicationPublicId ) );
        assertResponseStatus( 200, response );
        policies = JsonHelpers.fromJson( response.getResponseBody(), Policy[].class );
        Assert.assertEquals( 1, policies.length );

        exportFile.delete();
    }

    @Test
    public void testCRUD_ApplicationLevel()
        throws Exception
    {
        String applicationPublicId = "PolicyResourceTest_testCRUD";
        Application application = createApplication( applicationPublicId );
        String appId = application.getId();

        JsonStore store = JsonUtils.fileStore( new File( brain.getWorkDir(), "policy/" + appId ) );

        Assert.assertEquals( 0, store.modificationCount() );

        testCRUD( applicationPublicId, store );
    }

    private void testCRUD( String policyOwnerId, JsonStore store )
        throws Exception
    {
        // Add a policy
        Policy policy = new Policy();
        policy.setName( "PolicyResourceTest new policy" );
        Constraint constraint = new Constraint();
        constraint.setName( "PolicyResourceTest new constraint" );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        Response response = RestAccess.post( getServiceURL( policyOwnerId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        final Policy policy1 = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );
        Assert.assertNotNull( policy1.getId() );
        Assert.assertEquals( "PolicyResourceTest new policy", policy1.getName() );

        Assert.assertEquals( 1, store.modificationCount() );

        // Get all policies
        response = RestAccess.get( getServiceURL( policyOwnerId ) );
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
        response = RestAccess.put( getServiceURL( policyOwnerId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        final Policy policy2 = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );
        Assert.assertEquals( "PolicyResourceTest updated policy", policy2.getName() );

        Assert.assertEquals( 2, store.modificationCount() );

        // Get all policies
        response = RestAccess.get( getServiceURL( policyOwnerId ) );
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
        response = RestAccess.delete( getServiceURL( policyOwnerId, policy.getId() ) );
        assertResponseStatus( 204, response );

        Assert.assertEquals( 3, store.modificationCount() );

        // Get all policies
        response = RestAccess.get( getServiceURL( policyOwnerId ) );
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
    public void testCreateInvalidPolicy()
        throws Exception
    {
        String applicationPublicId = "PolicyResourceTest_testCreateInvalidPolicy";
        Application application = createApplication( applicationPublicId );
        String appId = application.getId();
        JsonStore store = JsonUtils.fileStore( new File( brain.getWorkDir(), "policy/" + appId ) );
        Assert.assertEquals( 0, store.modificationCount() );

        Policy policy = new Policy();
        policy.setName( null );
        Constraint constraint = new Constraint();
        constraint.setName( "PolicyResourceTest new constraint" );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        Response response = RestAccess.post( getServiceURL( applicationPublicId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 400, response );
        Assert.assertEquals( "The policy name must not be null or empty", response.getResponseBody() );
    }

    @Test
    public void testUpdateInvalidPolicy()
        throws Exception
    {
        String applicationPublicId = "PolicyResourceTest_testUpdateInvalidPolicy";
        Application application = createApplication( applicationPublicId );
        String appId = application.getId();
        JsonStore store = JsonUtils.fileStore( new File( brain.getWorkDir(), "policy/" + appId ) );
        Assert.assertEquals( 0, store.modificationCount() );

        // Create a valid policy
        Policy policy = new Policy();
        policy.setName( "PolicyResourceTest_testUpdateInvalidPolicy" );
        Constraint constraint = new Constraint();
        constraint.setName( "PolicyResourceTest new constraint" );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        Response response = RestAccess.post( getServiceURL( applicationPublicId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        policy = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );

        // Update invalid policy
        policy.setName( null );
        response = RestAccess.put( getServiceURL( applicationPublicId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 400, response );
        Assert.assertEquals( "The policy name must not be null or empty", response.getResponseBody() );
    }

    private String getServiceURL( final String policyOwnerId )
    {
        return getRestBaseUrl() + PolicyResource.SERVICE_PATH.replace( "{policyOwnerId}", policyOwnerId );
    }

    private String getServiceURL( final String policyOwnerId, final String policyId )
    {
        return getServiceURL( policyOwnerId ) + "/" + policyId;
    }
}
