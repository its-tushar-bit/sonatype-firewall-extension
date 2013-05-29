/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class LicenseThreatGroupResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testDelete_ApplicationIdMismatch()
        throws Exception
    {
        String appPublicId1 = "LicenseThreatGroupResourceTest_AppId1";
        Application application1 = createApplication( appPublicId1 );
        String appPublicId2 = "LicenseThreatGroupResourceTest_AppId2";
        Application application2 = createApplication( appPublicId2 );

        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setOwnerId( application1.getId() );
        group.setName( "AAA My group" );
        group.setThreatLevel( 4 );
        Response response = RestAccess.post( getServiceURL( appPublicId1 ), JsonHelpers.asJson( group ) );
        assertResponseStatus( 200, response );
        group = JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroup.class );

        response = RestAccess.delete( getServiceURL( appPublicId2 ) + "/" + group.getId() );
        assertResponseStatus( 404, response );
        Assert.assertEquals( "Cannot find a license threat group with id " + group.getId() + " for owner id "
            + application2.getId(), response.getResponseBody() );
        // Verify that the group was not deleted
        response = RestAccess.get( getServiceURL( appPublicId1 ) );
        assertResponseStatus( 200, response );
        LicenseThreatGroup[] groups = JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroup[].class );
        Assert.assertNotNull( groups );
        Assert.assertEquals( LicenseThreatGroupDAO.DEFAULT_LICENSE_THREAT_GROUP_COUNT + 1, groups.length );
        assertLicenseThreatGroup( application1.getId(), "AAA My group", 4, groups[0] );
    }

    private void testCRUD( String ownerPublicId, String ownerId )
        throws Exception
    {
        // Get all groups
        Response response = RestAccess.get( getServiceURL( ownerPublicId ) );
        assertResponseStatus( 200, response );
        LicenseThreatGroup[] groups = JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroup[].class );
        Assert.assertNotNull( groups );
        int initialLicenseThreatGroupCount = groups.length;

        // Add a group
        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setOwnerId( ownerId );
        group.setName( "AAA My group" );
        group.setThreatLevel( 10 );
        response = RestAccess.post( getServiceURL( ownerPublicId ), JsonHelpers.asJson( group ) );
        assertResponseStatus( 200, response );
        group = JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroup.class );
        assertLicenseThreatGroup( ownerId, "AAA My group", 10, group );

        // Get all groups
        response = RestAccess.get( getServiceURL( ownerPublicId ) );
        assertResponseStatus( 200, response );
        groups = JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroup[].class );
        Assert.assertNotNull( groups );
        Assert.assertEquals( initialLicenseThreatGroupCount + 1, groups.length );
        assertLicenseThreatGroup( ownerId, "AAA My group", 10, groups[0] );

        // Update a group
        group.setName( "AAA My updated group" );
        response = RestAccess.put( getServiceURL( ownerPublicId ), JsonHelpers.asJson( group ) );
        assertResponseStatus( 200, response );
        group = JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroup.class );
        assertLicenseThreatGroup( ownerId, "AAA My updated group", 10, group );

        // Get all groups
        response = RestAccess.get( getServiceURL( ownerPublicId ) );
        assertResponseStatus( 200, response );
        groups = JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroup[].class );
        Assert.assertNotNull( groups );
        Assert.assertEquals( initialLicenseThreatGroupCount + 1, groups.length );
        assertLicenseThreatGroup( ownerId, "AAA My updated group", 10, groups[0] );

        // Delete a group
        response = RestAccess.delete( getServiceURL( ownerPublicId ) + "/" + group.getId() );
        assertResponseStatus( 204, response );

        // Get all groups
        response = RestAccess.get( getServiceURL( ownerPublicId ) );
        assertResponseStatus( 200, response );
        groups = JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroup[].class );
        Assert.assertNotNull( groups );
        Assert.assertEquals( initialLicenseThreatGroupCount, groups.length );
    }

    @Test
    public void testCRUD_Application()
        throws Exception
    {
        String appPublicId = "LicenseThreatGroupResourceTest_AppId";
        Application application = createApplication( appPublicId );
        testCRUD( appPublicId, application.getId() );
    }

    @Test
    public void testCRUD_Organization()
        throws Exception
    {
        Organization organization = createOrganization( "testCRUD-Organization", true /* createLicenseThreatGroups */);
        testCRUD( organization.getId(), organization.getId() );
    }

    private void assertLicenseThreatGroup( String ownerId, String name, int threatLevel, LicenseThreatGroup actual )
    {
        Assert.assertEquals( ownerId, actual.getOwnerId() );
        Assert.assertEquals( name, actual.getName() );
        Assert.assertEquals( threatLevel, actual.getThreatLevel() );
    }

    private String getServiceURL( final String ownerId )
    {
        return getRestBaseUrl() + LicenseThreatGroupResource.SERVICE_PATH.replace( "{ownerId}", ownerId );
    }
}
