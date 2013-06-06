/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class LicenseThreatGroupLicenseResourceTest
    extends AbstractResourceTest
{
    private void testSetGet( String ownerType, String ownerPublicId, String ownerId )
        throws Exception
    {
        // Create an application and a group
        LicenseThreatGroupDAO groupDAO = new LicenseThreatGroupDAO();
        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setOwnerId( ownerId );
        group.setName( "My group" );
        group.setThreatLevel( 4 );
        groupDAO.insert( group );

        // Get
        Response response = RestAccess.get( getServiceURL( ownerType, ownerPublicId, group.getId() ) );
        assertResponseStatus( 200, response );
        LicenseThreatGroupLicense[] licenseThreatGroupLicenses =
            JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroupLicense[].class );
        Assert.assertNotNull( licenseThreatGroupLicenses );
        Assert.assertEquals( 0, licenseThreatGroupLicenses.length );

        // Set
        Set<String> licenseIds = new LinkedHashSet<String>();
        licenseIds.add( "GPL-2.0" );
        licenseIds.add( "Apache-2.0" );
        response =
            RestAccess.put( getServiceURL( ownerType, ownerPublicId, group.getId() ), JsonHelpers.asJson( licenseIds ) );
        assertResponseStatus( 200, response );

        // Get
        licenseThreatGroupLicenses =
            JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroupLicense[].class );
        Assert.assertNotNull( licenseThreatGroupLicenses );
        Assert.assertEquals( 2, licenseThreatGroupLicenses.length );
        assertLicenseThreatGroupLicense( ownerId, group.getId(), "Apache-2.0", licenseThreatGroupLicenses[0] );
        assertLicenseThreatGroupLicense( ownerId, group.getId(), "GPL-2.0", licenseThreatGroupLicenses[1] );
    }

    @Test
    public void testSetGet_Application()
        throws Exception
    {
        String appPublicId = "LicenseThreatGroupLicenseResourceTest_AppId";
        Application application = createApplication( appPublicId, false /* createLicenseThreatGroups */);
        testSetGet( "application", appPublicId, application.getId() );
    }

    @Test
    public void testSetGet_Organization()
        throws Exception
    {
        Organization organization =
            createOrganization( "testSetGet-Organization", false /* createLicenseThreatGroups */);
        testSetGet( "organization", organization.getId(), organization.getId() );
    }

    private void assertLicenseThreatGroupLicense( String ownerId, String licenseThreatGroupId, String licenseId,
                                                  LicenseThreatGroupLicense actual )
    {
        Assert.assertEquals( ownerId, actual.getOwnerId() );
        Assert.assertEquals( licenseThreatGroupId, actual.getLicenseThreatGroupId() );
        Assert.assertEquals( licenseId, actual.getLicenseId() );
    }

    private String getServiceURL( String ownerType, String ownerId, String licenseThreatGroupId )
    {
        return getRestBaseUrl()
            + LicenseThreatGroupLicenseResource.SERVICE_PATH.replace( "{ownerType: application|organization}",
                                                                      ownerType ).replace( "{ownerId}", ownerId ).replace( "{licenseThreatGroupId}",
                                                                                                                           licenseThreatGroupId );
    }
}
