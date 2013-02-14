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
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class LicenseThreatGroupLicenseResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testSetGet()
        throws Exception
    {
        // Create an application and a group
        String appPublicId = "LicenseThreatGroupLicenseResourceTest_AppId";
        Application application = createApplication( appPublicId );
        LicenseThreatGroupDAO groupDAO = new LicenseThreatGroupDAO();
        for ( LicenseThreatGroup licenseThreatGroup : groupDAO.getByApplicationId( application.getId() ) )
        {
            groupDAO.delete( licenseThreatGroup );
        }
        LicenseThreatGroup group = new LicenseThreatGroup();
        group.setApplicationId( application.getId() );
        group.setName( "My group" );
        group.setThreatLevel( 4 );
        groupDAO.insert( group );

        // Get
        Response response = RestAccess.get( getServiceURL( appPublicId, group.getId() ) );
        assertResponseStatus( 200, response );
        LicenseThreatGroupLicense[] licenseThreatGroupLicenses =
            JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroupLicense[].class );
        Assert.assertNotNull( licenseThreatGroupLicenses );
        Assert.assertEquals( 0, licenseThreatGroupLicenses.length );

        // Set
        Set<String> licenseIds = new LinkedHashSet<String>();
        licenseIds.add( "GPL-2.0" );
        licenseIds.add( "Apache-2.0" );
        response = RestAccess.put( getServiceURL( appPublicId, group.getId() ), JsonHelpers.asJson( licenseIds ) );
        assertResponseStatus( 200, response );

        // Get
        licenseThreatGroupLicenses =
            JsonHelpers.fromJson( response.getResponseBody(), LicenseThreatGroupLicense[].class );
        Assert.assertNotNull( licenseThreatGroupLicenses );
        Assert.assertEquals( 2, licenseThreatGroupLicenses.length );
        assertLicenseThreatGroupLicense( application.getId(), group.getId(), "Apache-2.0",
                                         licenseThreatGroupLicenses[0] );
        assertLicenseThreatGroupLicense( application.getId(), group.getId(), "GPL-2.0", licenseThreatGroupLicenses[1] );
    }

    private void assertLicenseThreatGroupLicense( String applicationId, String licenseThreatGroupId, String licenseId,
                                                  LicenseThreatGroupLicense actual )
    {
        Assert.assertEquals( applicationId, actual.getApplicationId() );
        Assert.assertEquals( licenseThreatGroupId, actual.getLicenseThreatGroupId() );
        Assert.assertEquals( licenseId, actual.getLicenseId() );
    }

    private String getServiceURL( final String appId, String licenseThreatGroupId )
    {
        return getRestBaseUrl()
            + LicenseThreatGroupLicenseResource.SERVICE_PATH.replace( "{applicationPublicId}", appId ).replace( "{licenseThreatGroupId}",
                                                                                                                licenseThreatGroupId );
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
