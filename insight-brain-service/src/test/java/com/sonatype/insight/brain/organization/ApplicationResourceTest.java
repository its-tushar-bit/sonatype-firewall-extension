/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
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
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource;
import com.sonatype.insight.brain.saas.CIResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

/// TODO This class doesn't properly cleanup after failed tests
public class ApplicationResourceTest
    extends AbstractResourceTest
{
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
    public void testCRUD()
        throws Exception
    {
        final String applicationPublicId = "testID";
        final String applicationName = "test-application-name";

        Organization organization = createOrganization( "ApplicationResourceTest" );

        // Test Add Application
        Application application = new Application();
        application.setName( applicationName );
        application.setPublicId( applicationPublicId );
        application.setOrganizationId( organization.getId() );

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
        Assert.assertEquals( application.getOrganizationId(), applicationManagementSummary.getOrganizationId() );

        // Test Add Invalid Icon
        byte[] defaultIconByteArray = loadInvalidIcon();
        AsyncHttpClient.BoundRequestBuilder builder = RestAccess.getClient().preparePost( getSetIconServiceUrl() );
        builder.addBodyPart( new StringPart( "applicationId", application.getId() ) );
        builder.addBodyPart( new StringPart( "hasRobotSource", "false" ) );
        builder.addBodyPart( new FilePart( "file", new ByteArrayPartSource( "defaulticon_application.png",
                                                                            defaultIconByteArray ) ) );
        Future<Response> futureResponse = builder.execute();

        response = futureResponse.get();
        assertResponseStatus( 400, response );
        Assert.assertEquals( "defaulticon_application.png is not a valid image.", response.getResponseBody() );

        // Test Get Icon (default icon)
        defaultIconByteArray = loadDefaultIcon();
        Response iconResponse = RestAccess.get( getGetIconServiceUrl( applicationPublicId ) );
        assertResponseStatus( 307, iconResponse );
        Assert.assertEquals( getRestBaseUrl() + "assets/img/defaulticon_application.png",
                             iconResponse.getHeader( "Location" ) );

        // Test Add Application Icon
        builder = RestAccess.getClient().preparePost( getSetIconServiceUrl() );
        builder.addBodyPart( new StringPart( "applicationId", application.getId() ) );
        builder.addBodyPart( new StringPart( "hasRobotSource", "false" ) );
        builder.addBodyPart(
            new FilePart( "file", new ByteArrayPartSource( "defaulticon_application.png", defaultIconByteArray ) ) );
        futureResponse = builder.execute();
        response = futureResponse.get();
        assertResponseStatus( 204, response );

        // Test Get Icon (from added application)
        iconResponse = RestAccess.get( getGetIconServiceUrl( applicationPublicId ) );
        assertResponseStatus( 200, iconResponse );
        InputStream iconStream = iconResponse.getResponseBodyAsStream();
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
        assertResponseStatus( 200, response );
        applicationManagementSummary =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicationManagementSummary.class );
        Assert.assertEquals( application.getId(), applicationManagementSummary.getId() );
        Assert.assertEquals( applicationPublicId, applicationManagementSummary.getPublicId() );
        Assert.assertEquals( applicationName + "updated", applicationManagementSummary.getName() );

        // Test icon update
        builder = RestAccess.getClient().preparePost( getSetIconServiceUrl() );
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
        assertResponseStatus( 204, response );
        application = applicationDAO.getByPublicId( applicationPublicId );
        Assert.assertNull( application );
        Assert.assertEquals( 0, policyDAO.getByOwnerId( applicationManagementSummary.getId() ).size() );

        // Default icon redirect should be returned
        iconResponse = RestAccess.get( getServiceURL() + "/icon/" + applicationPublicId );
        assertResponseStatus( 307, iconResponse );
        Assert.assertEquals( getRestBaseUrl() + "assets/img/defaulticon_application.png",
                             iconResponse.getHeader( "Location" ) );
    }

    private byte[] loadInvalidIcon()
        throws IOException
    {
        byte[] iconByteArray = null;
        ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();

        ClassLoader classLoader = ApplicationResourceTest.class.getClassLoader();
        InputStream iconStream = classLoader.getResourceAsStream( "assets/assets/util/AngularCommon.js" );
        Assert.assertNotNull( iconStream );
        try
        {
            for ( int b = 0; ( b = iconStream.read() ) != -1; )
            {
                imageOutputStream.write( b );
            }
            iconByteArray = imageOutputStream.toByteArray();
        }
        finally
        {
            IOUtil.close( imageOutputStream );
            IOUtil.close( iconStream );
        }

        return iconByteArray;
    }

    private byte[] loadDefaultIcon()
        throws IOException
    {
        byte[] defaultIconByteArray = null;
        ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();

        ClassLoader classLoader = ApplicationResourceTest.class.getClassLoader();
        InputStream iconStream = classLoader.getResourceAsStream( "assets/assets/img/defaulticon_application.png" );
        Assert.assertNotNull( iconStream );
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
            IOUtil.close( imageOutputStream );
            IOUtil.close( iconStream );
        }

        Assert.assertNotNull( defaultIconByteArray );
        Assert.assertNotEquals( 0, defaultIconByteArray.length );

        return defaultIconByteArray;
    }

    @Test
    public void testAddApplication_exceedsLicense()
        throws Exception
    {
        setApplicationLimit( 1 );

        createApplication( "testAddApplication_exceedsLicense_id" );

        // Test Add Application, which should fail with 402 since we exceeded the limit
        Application application = new Application();
        application.setName( "testAddApplication_exceedsLicense_id_new_name" );
        application.setPublicId( "testAddApplication_exceedsLicense_id_new_id" );

        Response response = RestAccess.post( getServiceURL(), JsonHelpers.asJson( application ) );
        assertResponseStatus( 402, response );
        Assert.assertEquals( "You have exceeded the licensed limit of 1 applications.", response.getResponseBody() );
    }

    @Test
    public void testGetApplications()
        throws Exception
    {
        // Create an application
        final String applicationPublicId = "ApplicationResourceTest-getApplicationsTest-AppId";
        final String applicationName = "ApplicationResourceTest-getApplicationsTest-Name";
        final String licenseFingerprint = "ApplicationResourceTest-getApplicationsTest-LicenseFingerprint";

        Application application = createApplication( applicationPublicId, applicationName );
        setLicenseFingerprint( licenseFingerprint );

        // Create policy
        PolicyDAO policyDAO = new PolicyDAO( brain.getWorkDir() );
        Policy policy1 = new Policy();
        policy1.setName( "policy 1" );
        Constraint constraint1 = new Constraint( null, "constraint 1", LogicalOperator.AND );
        constraint1.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy1.addConstraint( constraint1 );
        policyDAO.insert( application.getId(), policy1 );
        final String scanId1 = "ScanId1", scanId2 = "ScanId2";
        final File saasReportFile1 = getReportResponseFile( licenseFingerprint, scanId1 );
        FileUtils.copyURLToFile( getClass().getResource( "/PolicyEvaluateResourceTest/report.zip" ), saasReportFile1 );
        FileUtils.copyFile( saasReportFile1, getReportResponseFile( licenseFingerprint, scanId2 ) );

        // Eval policy
        Response response = RestAccess.post( getEvalURL( applicationPublicId, scanId1 ),
                                             JsonHelpers.asJson( new Stage( Stage.ID_BUILD ) ) );
        assertResponseStatus( 200, response );
        response = RestAccess.post( getEvalURL( applicationPublicId, scanId1 ),
                                    JsonHelpers.asJson( new Stage( Stage.ID_RELEASE ) ) );
        assertResponseStatus( 200, response );
        response = RestAccess.post( getEvalURL( applicationPublicId, scanId2 ),
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

        // Scans count
        final File saasScanFile = getScanResponseFile( licenseFingerprint );
        saasScanFile.delete();

        final URL testScanResultUrl = getClass().getResource( "/CIResourceTest/scan.json" );
        FileUtils.copyFile( new File( testScanResultUrl.getFile() ), saasScanFile );

        RestAccess.put( getScanURL( applicationPublicId ), "" );

        response = RestAccess.get( getServiceURL() );
        assertResponseStatus( 200, response );

        applications = JsonHelpers.fromJson( response.getResponseBody(), ApplicationManagementSummary[].class );
        Assert.assertNotNull( applications );
        Assert.assertEquals( 1, applications[0].getScansCount() );

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

        @SuppressWarnings( "unchecked" ) Map<String, String> applicationNames =
            JsonHelpers.fromJson( response.getResponseBody(), Map.class );
        Assert.assertNotNull( applicationNames );

        Assert.assertEquals( applicationNames.toString(), 1, applicationNames.size() );
        Assert.assertTrue( applicationNames.containsKey( applicationPublicId ) );
        Assert.assertTrue( applicationNames.containsValue( applicationName ) );
    }

    @Test
    public void testAddApplication_NoOrganization()
        throws Exception
    {
        String applicationPublicId = "testAddApplication_NoOrganization";
        String applicationName = "testAddApplication-NoOrganization";

        Application application = new Application();
        application.setName( applicationName );
        application.setPublicId( applicationPublicId );

        Response response = RestAccess.post( getServiceURL(), JsonHelpers.asJson( application ) );
        assertResponseStatus( 400, response );
        Assert.assertEquals( "Applications must have a parent organization.", response.getResponseBody() );
    }

    @Test
    public void testUpdateApplication_NoOrganization()
        throws Exception
    {
        Application application = createApplication( "testUpdateApplication_NoOrganization" );

        application.setOrganizationId( null );

        Response response = RestAccess.put( getServiceURL(), JsonHelpers.asJson( application ) );
        assertResponseStatus( 400, response );
        Assert.assertEquals( "Applications must have a parent organization.", response.getResponseBody() );
    }

    @Test
    public void testUpdateApplication_ChangeOrganization()
        throws Exception
    {
        Application application = createApplication( "testUpdateApplication_ChangeOrganization" );

        application.setOrganizationId( "newOrganizationId" );

        Response response = RestAccess.put( getServiceURL(), JsonHelpers.asJson( application ) );
        assertResponseStatus( 400, response );
        Assert.assertEquals( "Cannot change the parent organization of an application.", response.getResponseBody() );
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

    private String getGetIconServiceUrl( String applicationPublicId )
    {
        return getServiceURL() + "/"
            + ApplicationResource.GET_APPLICATION_ICON_PATH.replace( "{applicationPublicId}", applicationPublicId );
    }

    private String getSetIconServiceUrl()
    {
        return getServiceURL() + "/" + ApplicationResource.ICON_PATH;
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

    private String getScanURL( final String appId )
    {
        return getRestBaseUrl() + CIResource.SERVICE_PATH + "/scan/" + appId;
    }
}
