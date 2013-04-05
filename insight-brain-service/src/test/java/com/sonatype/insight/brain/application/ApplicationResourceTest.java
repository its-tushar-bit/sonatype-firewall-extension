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
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.multipart.ByteArrayPartSource;
import com.ning.http.multipart.FilePart;
import com.ning.http.multipart.StringPart;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationManagementSummary;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
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
        Application application = new Application();
        application.setName( "testReservedApplicationId" );
        application.setPublicId( Policy.ORGANIZATION_OWNER_PUBLIC_ID );

        Response response = RestAccess.post( getServiceURL(), JsonHelpers.asJson( application ) );
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

        // Test Add Application
        Application application = new Application();
        application.setName( applicationName );
        application.setPublicId( applicationPublicId );

        Response response = RestAccess.post( getServiceURL(), JsonHelpers.asJson( application ) );

        assertResponseStatus( 200, response );

        ApplicationManagementSummary applicationManagementSummary =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicationManagementSummary.class );

        ApplicationDAO applicationDAO = new ApplicationDAO();
        application = applicationDAO.getByPublicIdNotNull( applicationPublicId );

        Assert.assertNotNull( application );
        Assert.assertEquals( application.getId(), applicationManagementSummary.getId() );
        Assert.assertEquals( applicationPublicId, applicationManagementSummary.getPublicId() );
        Assert.assertEquals( applicationName, applicationManagementSummary.getName() );

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

        // Test Add Application Icon
        AsyncHttpClient.BoundRequestBuilder builder = RestAccess.getClient().preparePost( getIconServiceUrl() );
        builder.addBodyPart( new StringPart( "applicationId", application.getId() ) );
        builder.addBodyPart( new StringPart( "hasRobotSource", "false" ) );
        builder.addBodyPart(
            new FilePart( "file", new ByteArrayPartSource( "defaulticon_application.png", defaultIconByteArray ) ) );
        Future<Response> futureResponse = builder.execute();

        response = futureResponse.get();
        assertResponseStatus( 204, response );

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

        // Test application update
        application.setName( applicationName + "updated" );

        response = RestAccess.put( getServiceURL(), JsonHelpers.asJson( application ) );

        applicationManagementSummary =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicationManagementSummary.class );

        Assert.assertEquals( application.getId(), applicationManagementSummary.getId() );
        Assert.assertEquals( applicationPublicId, applicationManagementSummary.getPublicId() );
        Assert.assertEquals( applicationName + "updated", applicationManagementSummary.getName() );

        // Test icon update
        builder = RestAccess.getClient().preparePost( getIconServiceUrl() );

        builder.addBodyPart( new StringPart( "applicationId", application.getId() ) );
        builder.addBodyPart( new StringPart( "hasRobotSource", "false" ) );
        futureResponse = builder.execute();
        response = futureResponse.get();

        assertResponseStatus( 204, response );

        // Verify non alpha numeric name fails
        application.setName( "Non Alphanumeric Name !!!!!" );

        response = RestAccess.put( getServiceURL(), JsonHelpers.asJson( application ) );

        assertResponseStatus( 400, response );

        // Create policy to be deleted along app
        PolicyDAO policyDAO = new PolicyDAO( brain.getWorkDir() );
        Policy policy1 = new Policy();
        policy1.setName( "PolicyDAOTest new policy 1" );
        Constraint constraint1 = new Constraint( null, "PolicyDAOTest new constraint 1", LogicalOperator.AND );
        constraint1.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy1.addConstraint( constraint1 );
        policyDAO.insert( application.getId(), policy1 );

        // Test delete
        response = RestAccess.delete( getServiceURL() + "/" + applicationPublicId );
        application = applicationDAO.getByPublicId( applicationPublicId );

        assertResponseStatus( 204, response );
        Assert.assertNull( application );
        Assert.assertEquals( 0, policyDAO.getByOwnerId( applicationManagementSummary.getId() ).size() );

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

        // Create policy
        PolicyDAO policyDAO = new PolicyDAO( brain.getWorkDir() );
        Policy policy1 = new Policy();
        policy1.setName( "policy 1" );
        Constraint constraint1 = new Constraint( null, "constraint 1", LogicalOperator.AND );
        constraint1.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy1.addConstraint( constraint1 );
        policyDAO.insert( application.getId(), policy1 );
        final String scanId1 = "ScanId1", scanId2 = "ScanId2";
        final File saasReportFile1 = getReportResponseFile( applicationPublicId, scanId1 );
        FileUtils.copyURLToFile( getClass().getResource( "/PolicyEvaluateResourceTest/report.zip" ), saasReportFile1 );
        FileUtils.copyFile( saasReportFile1, getReportResponseFile( applicationPublicId, scanId2 ) );

        // Eval policy
        Response response =
            RestAccess.post( getEvalURL( applicationPublicId, scanId1 ),
                             JsonHelpers.asJson( new Stage( Stage.ID_BUILD ) ) );
        assertResponseStatus( 200, response );
        response =
            RestAccess.post( getEvalURL( applicationPublicId, scanId1 ),
                             JsonHelpers.asJson( new Stage( Stage.ID_RELEASE ) ) );
        assertResponseStatus( 200, response );
        response =
            RestAccess.post( getEvalURL( applicationPublicId, scanId2 ),
                             JsonHelpers.asJson( new Stage( Stage.ID_BUILD ) ) );
        assertResponseStatus( 200, response );

        response = RestAccess.get( getServiceURL() );
        assertResponseStatus( 200, response );

        ApplicationManagementSummary[] applications =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicationManagementSummary[].class );
        Assert.assertNotNull( applications );

        Assert.assertEquals( Arrays.asList( applications ).toString(), 1, applications.length );
        Assert.assertEquals( application.getId(), applications[0].getId() );
        Assert.assertEquals( application.getName(), applications[0].getName() );
        Assert.assertNotNull( applications[0].getPolicyEvaluations() );
        Assert.assertEquals( 2, applications[0].getPolicyEvaluations().size() );
        Assert.assertEquals( Stage.ID_BUILD,
                             applications[0].getPolicyEvaluations().get( 0 ).getStage().getStageTypeId() );
        Assert.assertEquals( scanId2, applications[0].getPolicyEvaluations().get( 0 ).getScanId() );
        Assert.assertEquals( Stage.ID_RELEASE,
                             applications[0].getPolicyEvaluations().get( 1 ).getStage().getStageTypeId() );
        Assert.assertEquals( scanId1, applications[0].getPolicyEvaluations().get( 1 ).getScanId() );

        // Test GetApplication
        response = RestAccess.get( getApplicationServiceUrl( applicationPublicId ) );
        assertResponseStatus( 200, response );

        ApplicationManagementSummary applicationSummary =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicationManagementSummary.class );
        Assert.assertNotNull( applicationSummary );
        Assert.assertEquals( application.getId(), applicationSummary.getId() );
        Assert.assertEquals( application.getName(), applicationSummary.getName() );
        Assert.assertNotNull( applications[0].getPolicyEvaluations() );
        Assert.assertEquals( 2, applications[0].getPolicyEvaluations().size() );
        Assert.assertEquals( Stage.ID_BUILD,
                             applications[0].getPolicyEvaluations().get( 0 ).getStage().getStageTypeId() );
        Assert.assertEquals( scanId2, applications[0].getPolicyEvaluations().get( 0 ).getScanId() );
        Assert.assertEquals( Stage.ID_RELEASE,
                             applications[0].getPolicyEvaluations().get( 1 ).getStage().getStageTypeId() );
        Assert.assertEquals( scanId1, applications[0].getPolicyEvaluations().get( 1 ).getScanId() );
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

        @SuppressWarnings("unchecked") Map<String, String> applicationNames =
            JsonHelpers.fromJson( response.getResponseBody(), Map.class );
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

    private String getIconServiceUrl()
    {
        return getServiceURL() + "/icon";
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + ApplicationResource.SERVICE_PATH;
    }

    private String getEvalURL( final String appId, final String scanId )
    {
        return getRestBaseUrl() + PolicyEvaluateResource.SERVICE_PATH.replace( "{applicationPublicId}", appId )
            + "?scanId=" + scanId;
    }
}
