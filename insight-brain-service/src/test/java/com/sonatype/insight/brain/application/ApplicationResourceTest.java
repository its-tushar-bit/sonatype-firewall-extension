/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.application;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.multipart.ByteArrayPartSource;
import com.ning.http.multipart.FilePart;
import com.ning.http.multipart.StringPart;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationManagementSummary;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class ApplicationResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testReservedApplicationId()
        throws Exception
    {
        AsyncHttpClient.BoundRequestBuilder builder = RestAccess.getClient().preparePost( getServiceURL() );
        builder.addBodyPart( new StringPart( "applicationName", "testReservedApplicationId" ) );
        builder.addBodyPart( new StringPart( "applicationPublicId", Policy.ORGANIZATION_OWNER_PUBLIC_ID ) );
        builder.addBodyPart( new StringPart( "hasRobotSource", "false" ) );
        builder.addBodyPart( new FilePart( "file", new ByteArrayPartSource( "defaulticon_application.png", new byte[0] ) ) );
        Future<Response> futureResponse = builder.execute();

        Response response = futureResponse.get();
        assertResponseStatus( 400, response );
        Assert.assertEquals( Policy.ORGANIZATION_OWNER_PUBLIC_ID + " is not allowed as application ID.",
                             response.getResponseBody() );
    }

    @Test
    public void testValidate()
        throws Exception
    {
        final String applicationPublicId = "ApplicationResourceTest-testValidate-AppId";
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByPublicId( applicationPublicId );
        Assert.assertNull( application );

        application = new Application();
        application.setPublicId( applicationPublicId );
        application.setName( "ApplicationResourceTest-testValidate-AppName" );
        applicationDAO.insert( application );

        Response response = RestAccess.get( getValidateApplicationIdServiceURL( applicationPublicId ) );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), equalTo( "OK" ) );

        applicationDAO.delete( application );

        // validate service always returns 200, the actual result is in the response body
        response = RestAccess.get( getValidateApplicationIdServiceURL( applicationPublicId ) );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), equalTo( "Invalid application id " + applicationPublicId ) );
    }

    @Test
    public void testAddDeleteApplication()
        throws Exception
    {
        final String applicationPublicId = "testID";
        final String applicationName = "test-application-name";

        // Test Get Icon (default icon)
        ClassLoader classLoader = ApplicationResourceTest.class.getClassLoader();
        InputStream iconStream = classLoader.getResourceAsStream( "assets/assets/img/defaulticon_application.png" );
        Assert.assertNotNull( iconStream );
        byte[] defaultIconByteArray = null;
        ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();
        try
        {
            for ( int b = 0; ( b = iconStream.read() ) != -1; )
            {
                imageOutputStream.write( b );
            }
            defaultIconByteArray = imageOutputStream.toByteArray();
        }
        finally
        {
            imageOutputStream.close();
            iconStream.close();
        }

        Assert.assertNotNull( defaultIconByteArray );
        Assert.assertNotEquals( 0, defaultIconByteArray.length );

        // Test Add Application
        AsyncHttpClient.BoundRequestBuilder builder = RestAccess.getClient().preparePost( getServiceURL() );
        builder.addBodyPart( new StringPart( "applicationName", applicationName ) );
        builder.addBodyPart( new StringPart( "applicationPublicId", applicationPublicId ) );
        builder.addBodyPart( new StringPart( "hasRobotSource", "false" ) );
        builder.addBodyPart(
            new FilePart( "file", new ByteArrayPartSource( "defaulticon_application.png", defaultIconByteArray ) ) );
        Future<Response> futureResponse = builder.execute();

        Response response = futureResponse.get();
        assertResponseStatus( 200, response );

        ApplicationManagementSummary applicationManagementSummary =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicationManagementSummary.class );

        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getByPublicIdNotNull( applicationPublicId );

        Assert.assertNotNull( application );
        Assert.assertEquals( application.getId(), applicationManagementSummary.getId() );
        Assert.assertEquals( applicationPublicId, applicationManagementSummary.getPublicId() );
        Assert.assertEquals( applicationName, applicationManagementSummary.getName() );

        // Test Get Icon (from added application)
        Response iconResponse = RestAccess.get( getServiceURL() + "/icon/" + applicationPublicId );

        assertResponseStatus( 200, iconResponse );
        iconStream = iconResponse.getResponseBodyAsStream();
        BufferedImage icon = null;
        try
        {
            icon = ImageIO.read( iconStream );
        }
        finally
        {
            iconStream.close();
        }
        Assert.assertNotNull( icon );
        Assert.assertEquals( 420, icon.getHeight() );
        Assert.assertEquals( 420, icon.getWidth() );

        // Test update
        builder = RestAccess.getClient().preparePost( getServiceURL() );

        builder.addBodyPart( new StringPart( "applicationId", application.getId() ) );
        builder.addBodyPart( new StringPart( "applicationName", applicationName + "updated" ) );
        builder.addBodyPart( new StringPart( "applicationPublicId", applicationPublicId ) );
        builder.addBodyPart( new StringPart( "hasRobotSource", "false" ) );
        futureResponse = builder.execute();
        response = futureResponse.get();

        assertResponseStatus( 200, response );
        applicationManagementSummary =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicationManagementSummary.class );

        Assert.assertEquals( application.getId(), applicationManagementSummary.getId() );
        Assert.assertEquals( applicationPublicId, applicationManagementSummary.getPublicId() );
        Assert.assertEquals( applicationName + "updated", applicationManagementSummary.getName() );

        // Verify non alpha numeric name fails
        builder = RestAccess.getClient().preparePost( getServiceURL() );
        builder.addBodyPart( new StringPart( "applicationName", "Non Alphanumeric Name !!!!!" ) );
        builder.addBodyPart( new StringPart( "applicationPublicId", applicationPublicId ) );
        builder.addBodyPart( new StringPart( "hasRobotSource", "false" ) );
        futureResponse = builder.execute();

        response = futureResponse.get();
        assertResponseStatus( 400, response );

        // Test delete
        response = RestAccess.delete( getServiceURL() + "/" + applicationPublicId );
        application = applicationDAO.getByPublicId( applicationPublicId );

        assertResponseStatus( 204, response );
        Assert.assertNull( application );

        iconResponse = RestAccess.get( getServiceURL() + "/icon/" + applicationPublicId );
        assertResponseStatus( 404, iconResponse );
    }

    @Test
    public void testGetApplications()
        throws Exception
    {
        // Test GetApplications
        final String applicationPublicId = "ApplicationResourceTest-getApplicationsTest-AppId";
        final String applicationName = "ApplicationResourceTest-getApplicationsTest-Name";
        Application application = createApplication( applicationPublicId, applicationName );

        Response response = RestAccess.get( getServiceURL() );
        assertResponseStatus( 200, response );

        ApplicationManagementSummary[] applications =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicationManagementSummary[].class );
        Assert.assertNotNull( applications );

        Assert.assertEquals( Arrays.asList( applications ).toString(), 1, applications.length );
        Assert.assertEquals( application.getId(), applications[0].getId() );
        Assert.assertEquals( application.getName(), applications[0].getName() );

        // Test GetApplication
        response = RestAccess.get( getApplicationServiceUrl( applicationPublicId ) );
        assertResponseStatus( 200, response );

        ApplicationManagementSummary applicationSummary =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicationManagementSummary.class );
        Assert.assertNotNull( applicationSummary );
        Assert.assertEquals( application.getId(), applicationSummary.getId() );
        Assert.assertEquals( application.getName(), applicationSummary.getName() );
    }

    @Test
    public void testGetApplicationNames()
        throws Exception
    {
        final String applicationPublicId = "ApplicationResourceTest-getApplicationNamesTest-AppId";
        final String applicationName = "ApplicationResourceTest-getApplicationNamesTest-Name";
        createApplication( applicationPublicId, applicationName );

        Response response = RestAccess.get( getServiceURL() + "/services/names" );
        assertResponseStatus( 200, response );

        @SuppressWarnings( "unchecked" )
        Map<String, String> applicationNames = JsonHelpers.fromJson( response.getResponseBody(), Map.class );
        Assert.assertNotNull( applicationNames );

        Assert.assertEquals( applicationNames.toString(), 1, applicationNames.size() );
        Assert.assertTrue( applicationNames.containsKey( applicationPublicId ) );
        Assert.assertTrue( applicationNames.containsValue( applicationName ) );
    }

    private String getValidateApplicationIdServiceURL( String applicationPublicId )
    {
        return getServiceURL() + '/' + ApplicationResource.VALIDATE_PATH.replace( "{applicationPublicId}",
                                                                                  applicationPublicId );
    }

    private String getApplicationServiceUrl( String applicationPublicId )
    {
        return getServiceURL() + '/' + ApplicationResource.GET_APPLICATION_PATH.replace( "{applicationPublicId}",
                                                                                         applicationPublicId );
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + ApplicationResource.SERVICE_PATH;
    }
}
