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
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class LicenseThreatGroupLicenseResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testDelete_ApplicationIdMismatch()
        throws Exception
    {
        String appPublicId1 = "LicenseThreatGroupLicenseResourceTest_AppId1";
        Application application1 = createApplication( appPublicId1 );
        String appPublicId2 = "LicenseThreatGroupLicenseResourceTest_AppId2";
        createApplication( appPublicId2 );

        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setApplicationId( application1.getId() );
        group.setName( "My group" );
        group.setThreatLevel( 4 );
        new LicenseThreatGroupDAO().insert( group );

        LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
        licenseThreatGroupLicense.setLicenseThreatGroupId( group.getId() );
        licenseThreatGroupLicense.setLicenseId( "UNSPECIFIED" );
        Response response =
            RestAccess.post( getServiceURL( appPublicId1 ), JsonHelpers.asJson( licenseThreatGroupLicense ) );
        assertResponseStatus( 200, response );
        licenseThreatGroupLicense = JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroupLicense.class );

        response = RestAccess.delete( getServiceURL( appPublicId2 ) + "/" + licenseThreatGroupLicense.getId() );
        assertResponseStatus( 404, response );
        Assert.assertEquals( "Cannot find a license threat group license with id " + licenseThreatGroupLicense.getId()
            + " for application id " + appPublicId2, response.getResponseBody() );
        // Verify that the object was not deleted
        response = RestAccess.get( getServiceURL( appPublicId1 ) + "/" + group.getId() );
        assertResponseStatus( 200, response );
        LicenseThreatGroupLicense[] licenseThreatGroupLicenses =
            JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroupLicense[].class );
        Assert.assertNotNull( licenseThreatGroupLicenses );
        Assert.assertEquals( 1, licenseThreatGroupLicenses.length );
        assertLicenseThreatGroupLicense( application1.getId(), group.getId(), "UNSPECIFIED",
                                         licenseThreatGroupLicenses[0] );
    }

    @Test
    public void testCRUD()
        throws Exception
    {
        // Create an application and a group
        String appPublicId = "LicenseThreatGroupLicenseResourceTest_AppId";
        Application application = createApplication( appPublicId );
        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setApplicationId( application.getId() );
        group.setName( "My group" );
        group.setThreatLevel( 4 );
        new LicenseThreatGroupDAO().insert( group );

        // Get all
        Response response = RestAccess.get( getServiceURL( appPublicId ) + "/" + group.getId() );
        assertResponseStatus( 200, response );
        LicenseThreatGroupLicense[] licenseThreatGroupLicenses =
            JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroupLicense[].class );
        Assert.assertNotNull( licenseThreatGroupLicenses );
        Assert.assertEquals( 0, licenseThreatGroupLicenses.length );

        // Add
        LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
        licenseThreatGroupLicense.setApplicationId( application.getId() );
        licenseThreatGroupLicense.setLicenseThreatGroupId( group.getId() );
        licenseThreatGroupLicense.setLicenseId( "UNSPECIFIED" );
        response = RestAccess.post( getServiceURL( appPublicId ), JsonHelpers.asJson( licenseThreatGroupLicense ) );
        assertResponseStatus( 200, response );
        licenseThreatGroupLicense = JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroupLicense.class );
        assertLicenseThreatGroupLicense( application.getId(), group.getId(), "UNSPECIFIED", licenseThreatGroupLicense );

        // Get all
        response = RestAccess.get( getServiceURL( appPublicId ) + "/" + group.getId() );
        assertResponseStatus( 200, response );
        licenseThreatGroupLicenses =
            JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroupLicense[].class );
        Assert.assertNotNull( licenseThreatGroupLicenses );
        Assert.assertEquals( 1, licenseThreatGroupLicenses.length );
        assertLicenseThreatGroupLicense( application.getId(), group.getId(), "UNSPECIFIED",
                                         licenseThreatGroupLicenses[0] );

        // Delete
        response = RestAccess.delete( getServiceURL( appPublicId ) + "/" + licenseThreatGroupLicense.getId() );
        assertResponseStatus( 204, response );

        // Get all
        response = RestAccess.get( getServiceURL( appPublicId ) + "/" + group.getId() );
        assertResponseStatus( 200, response );
        licenseThreatGroupLicenses =
            JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroupLicense[].class );
        Assert.assertNotNull( licenseThreatGroupLicenses );
        Assert.assertEquals( 0, licenseThreatGroupLicenses.length );
    }

    private void assertLicenseThreatGroupLicense( String applicationId, String licenseThreatGroupId,
                                                  String multiLicenseId, LicenseThreatGroupLicense actual )
    {
        Assert.assertEquals( applicationId, actual.getApplicationId() );
        Assert.assertEquals( licenseThreatGroupId, actual.getLicenseThreatGroupId() );
        Assert.assertEquals( multiLicenseId, actual.getLicenseId() );
    }

    private String getServiceURL( final String appId )
    {
        return getRestBaseUrl()
            + LicenseThreatGroupLicenseResource.SERVICE_PATH.replace( "{applicationPublicId}", appId );
    }

    @Override
    protected void cleanupApplication( Application application )
    {
        LicenseThreatGroupDAO dao = new LicenseThreatGroupDAO();
        for ( LicenseThreatGroup licenseThreatGroup : dao.getByApplicationId( application.getId() ) )
        {
            dao.delete( licenseThreatGroup );
        }
        super.cleanupApplication( application );
    }
}
