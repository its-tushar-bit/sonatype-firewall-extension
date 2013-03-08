/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.application;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationManagementSummary;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ApplicationResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testValidate()
        throws Exception
    {
        final String applicationPublicId = "ApplicationResourceTest_testValidate_AppId";
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByPublicId( applicationPublicId );
        Assert.assertNull( application );

        Response response = RestAccess.get( getValidateApplicationIdServiceURL( applicationPublicId ) );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), equalTo( "OK" ) );

        invalidateAppId( applicationPublicId, "Expired" );

        // validate service always returns 200, the actual result is in the response body
        response = RestAccess.get( getValidateApplicationIdServiceURL( applicationPublicId ) );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), equalTo( "Expired" ) );

        application = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        applicationDAO.delete( application );
    }

    @Test
    public void testGetApplications()
        throws Exception
    {
        final String applicationPublicId = "ApplicationResourceTest_getApplicationsTest_AppId";
        createApplication( applicationPublicId );
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByPublicId( applicationPublicId );

        Response response = RestAccess.get( getServiceURL() );
        assertResponseStatus( 200, response );

        ApplicationManagementSummary[] applications =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicationManagementSummary[].class );
        Assert.assertNotNull( applications );
        // Freemium application created by super
        Assert.assertEquals( 2, applications.length );
        Assert.assertEquals( application.getId(), applications[1].getId() );
    }

    private String getValidateApplicationIdServiceURL( String applicationPublicId )
    {
        return getServiceURL() + '/' + ApplicationResource.VALIDATE_PATH.replace( "{applicationPublicId}",
                                                                                  applicationPublicId );
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + ApplicationResource.SERVICE_PATH;
    }
}
