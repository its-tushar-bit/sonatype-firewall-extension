/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
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
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationLog;
import com.sonatype.insight.brain.saas.CIResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
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
        byte[] defaultIconByteArray = IconUtils.loadInvalidIcon();
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
        testValidIconResponse( iconResponse );

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

    @Test
    public void testSyncIcon()
        throws IOException, ExecutionException, InterruptedException
    {
        final String applicationPublicId = "testID";
        Application application = createApplication( applicationPublicId );

        byte[] defaultIconByteArray = loadDefaultIcon();

        // Test Sync Update Application Icon
        AsyncHttpClient.BoundRequestBuilder builder = RestAccess.getClient().preparePost( getSetSyncIconServiceUrl() );
        builder.addBodyPart( new StringPart( "applicationId", application.getId() ) );
        builder.addBodyPart( new StringPart( "hasRobotSource", "false" ) );
        builder.addBodyPart(
            new FilePart( "file", new ByteArrayPartSource( "defaulticon_application.png", defaultIconByteArray ) ) );
        Future<Response> futureResponse = builder.execute();
        Response response = futureResponse.get();
        assertResponseStatus( 303, response );
        Assert.assertEquals( getRestBaseUrl() + "assets/index.html", response.getHeader( "Location" ) );

        // Test Sync Fail Update Application Icon
        builder = RestAccess.getClient().preparePost( getSetSyncIconServiceUrl() );
        builder.addBodyPart( new StringPart( "applicationId", application.getId() ) );
        builder.addBodyPart( new StringPart( "hasRobotSource", "false" ) );
        builder.addBodyPart( new FilePart( "file", new ByteArrayPartSource( "defaulticon_application.png",
                                                                            IconUtils.loadInvalidIcon() ) ) );
        futureResponse = builder.execute();
        response = futureResponse.get();
        assertResponseStatus( 303, response );
        Assert.assertEquals(
            getRestBaseUrl() + "assets/index.html?errorMessage=defaulticon_application.png+is+not+a+valid+image.",
            response.getHeader( "Location" ) );
    }

    private byte[] loadDefaultIcon()
        throws IOException
    {
        return IconUtils.loadIcon( "defaulticon_application.png" );
    }

    private void testValidIconResponse( Response iconResponse )
        throws Exception
    {
        assertResponseStatus( 200, iconResponse );
        Assert.assertNotNull( iconResponse.getResponseBodyAsBytes() );
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
    }
    
    @Test
    public void testDeleteApplicationWithData()
        throws Exception
    {
        final ApplicationDAO applicationDAO = new ApplicationDAO();
        final PolicyDAO policyDAO = new PolicyDAO( brain.getWorkDir() );

        final String applicationPublicId = "testDeleteApplicationWithScan_PublicId";
        final String applicationName = "testDeleteApplicationWithScanAppName";

        Application application = new Application();
        application.setName( applicationName );
        application.setPublicId( applicationPublicId );

        applicationDAO.insert( application );

        final String licenseFingerprint = "testDeleteApplicationWithScan_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );

        File saasScanFile = getScanResponseFile( licenseFingerprint );
        saasScanFile.delete();

        URL testScanResultUrl = getClass().getResource( "/CIResourceTest/scan.json" );
        FileUtils.copyFile( new File( testScanResultUrl.getFile() ), saasScanFile );

        Response response = RestAccess.put( getScanURL( applicationPublicId ), "" );

        assertResponseStatus( 200, response );

        final String applicationId = application.getId();

        // TODO ideally, need to create these directories by calling into appropriate REST endpoints
        final InsightConfig insightConfig = new InsightConfig();
        insightConfig.setSonatypeWork( brain.getWorkDir().getAbsolutePath() );
        final InsightWork insightWork = new InsightWork( insightConfig );
        createDirectory( insightWork.getScanDir( applicationId ) );
        createDirectory( insightWork.getAuditDir( applicationId ) );
        createDirectory( insightWork.getReportDir( applicationId ) );
        createDirectory( policyDAO.getPolicyDir( applicationId ) );
        
        response = RestAccess.delete( getServiceURL() + "/" + applicationPublicId );
        application = applicationDAO.getByPublicId( applicationPublicId );

        assertResponseStatus( 204, response );
        Assert.assertNull( application );

        Assert.assertEquals( 0, policyDAO.getByOwnerId( applicationId ).size() );
        Assert.assertFalse( insightWork.getScanDir( applicationId ).exists() );
        Assert.assertFalse( insightWork.getAuditDir( applicationId ).exists() );
        Assert.assertFalse( insightWork.getReportDir( applicationId ).exists() );
        Assert.assertFalse( policyDAO.getPolicyDir( applicationId ).exists() );
    }

    @Test
    public void testDeleteNonExistingApplication()
        throws Exception
    {
        ApplicationDAO applicationDAO = new ApplicationDAO();

        final String applicationPublicId = "testDeleteApplicationWithScan_PublicId";
        final String applicationName = "testDeleteApplicationWithScanAppName";

        Application application = new Application();
        application.setName( applicationName );
        application.setPublicId( applicationPublicId );

        applicationDAO.insert( application );

        Response response = RestAccess.delete( getServiceURL() + "/" + applicationPublicId );
        application = applicationDAO.getByPublicId( applicationPublicId );

        assertResponseStatus( 204, response );
        Assert.assertNull( application );

        response = RestAccess.delete( getServiceURL() + "/" + applicationPublicId );

        assertResponseStatus( 404, response );
        Assert.assertEquals( "Could not find an application with public id " + applicationPublicId + ".",
                             response.getResponseBody() );
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
        
        Map<String, com.sonatype.insight.brain.model.policy.PolicyEvaluation> policyEvaluations = applications[0].getPolicyEvaluations();
        String[] stageTypeIds = policyEvaluations.keySet().toArray( new String[0] );
        
        Assert.assertNotNull( policyEvaluations );
        Assert.assertEquals( 2, policyEvaluations.size() );
        Assert.assertEquals( Stage.ID_BUILD, stageTypeIds[0] );
        Assert.assertEquals( Stage.ID_BUILD, policyEvaluations.get( stageTypeIds[0] ).getStage().getStageTypeId() );
        Assert.assertEquals( scanId2, policyEvaluations.get( stageTypeIds[0] ).getScanId() );
        Assert.assertEquals( Stage.ID_RELEASE, stageTypeIds[1] );
        Assert.assertEquals( Stage.ID_RELEASE, policyEvaluations.get( stageTypeIds[1] ).getStage().getStageTypeId() );
        Assert.assertEquals( scanId1, policyEvaluations.get( stageTypeIds[1] ).getScanId() );

        Map<String, PolicyEvaluationResult> policyEvaluationsResults = applications[0].getPolicyEvaluationsResults();
        stageTypeIds = policyEvaluationsResults.keySet().toArray( new String[0] );

        Assert.assertNotNull( policyEvaluationsResults );
        Assert.assertEquals( 2, policyEvaluationsResults.size() );
        Assert.assertEquals( Stage.ID_BUILD, stageTypeIds[0] );
        Assert.assertEquals( 7, policyEvaluationsResults.get( stageTypeIds[0] ).getAffectedComponentCount() );
        Assert.assertEquals( 0, policyEvaluationsResults.get( stageTypeIds[0] ).getCriticalComponentCount() );
        Assert.assertEquals( 0, policyEvaluationsResults.get( stageTypeIds[0] ).getModerateComponentCount() );
        Assert.assertEquals( 7, policyEvaluationsResults.get( stageTypeIds[0] ).getSevereComponentCount() );
        Assert.assertEquals( Stage.ID_RELEASE, stageTypeIds[1] );
        Assert.assertEquals( 7, policyEvaluationsResults.get( stageTypeIds[1] ).getAffectedComponentCount() );
        Assert.assertEquals( 0, policyEvaluationsResults.get( stageTypeIds[1] ).getCriticalComponentCount() );
        Assert.assertEquals( 0, policyEvaluationsResults.get( stageTypeIds[1] ).getModerateComponentCount() );
        Assert.assertEquals( 7, policyEvaluationsResults.get( stageTypeIds[1] ).getSevereComponentCount() );

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

        policyEvaluations = applicationSummary.getPolicyEvaluations();
        stageTypeIds = policyEvaluations.keySet().toArray( new String[0] );

        Assert.assertNotNull( policyEvaluations );
        Assert.assertEquals( 2, policyEvaluations.size() );
        Assert.assertEquals( Stage.ID_BUILD, stageTypeIds[0] );
        Assert.assertEquals( Stage.ID_BUILD, policyEvaluations.get( stageTypeIds[0] ).getStage().getStageTypeId() );
        Assert.assertEquals( scanId2, applications[0].getPolicyEvaluations().get( stageTypeIds[0] ).getScanId() );
        Assert.assertEquals( Stage.ID_RELEASE, stageTypeIds[1] );
        Assert.assertEquals( Stage.ID_RELEASE, policyEvaluations.get( stageTypeIds[1] ).getStage().getStageTypeId() );
        Assert.assertEquals( scanId1, applications[0].getPolicyEvaluations().get( stageTypeIds[1] ).getScanId() );

        policyEvaluationsResults = applicationSummary.getPolicyEvaluationsResults();
        stageTypeIds = policyEvaluationsResults.keySet().toArray( new String[0] );

        Assert.assertNotNull( policyEvaluationsResults );
        Assert.assertEquals( 2, policyEvaluationsResults.size() );
        Assert.assertEquals( Stage.ID_BUILD, stageTypeIds[0] );
        Assert.assertEquals( 7, policyEvaluationsResults.get( stageTypeIds[0] ).getAffectedComponentCount() );
        Assert.assertEquals( 0, policyEvaluationsResults.get( stageTypeIds[0] ).getCriticalComponentCount() );
        Assert.assertEquals( 0, policyEvaluationsResults.get( stageTypeIds[0] ).getModerateComponentCount() );
        Assert.assertEquals( 7, policyEvaluationsResults.get( stageTypeIds[0] ).getSevereComponentCount() );
        Assert.assertEquals( Stage.ID_RELEASE, stageTypeIds[1] );
        Assert.assertEquals( 7, policyEvaluationsResults.get( stageTypeIds[1] ).getAffectedComponentCount() );
        Assert.assertEquals( 0, policyEvaluationsResults.get( stageTypeIds[1] ).getCriticalComponentCount() );
        Assert.assertEquals( 0, policyEvaluationsResults.get( stageTypeIds[1] ).getModerateComponentCount() );
        Assert.assertEquals( 7, policyEvaluationsResults.get( stageTypeIds[1] ).getSevereComponentCount() );
    }

    @Test( timeout = 10000 )
    public void testGetApplications_DoesNotContactSaasAndPotentiallyBlockToGetLastPolicyAlerts()
        throws Exception
    {
        final String applicationPublicId = "ApplicationResourceTest-AppId";
        final String applicationName = "ApplicationResourceTest-Name";
        final String appId = createApplication( applicationPublicId, applicationName ).getId();
        final String scanId = "ApplicationResourceTest-ScanId";

        // create eval log entry pointing at missing report
        PolicyEvaluationLog evalLog = new PolicyEvaluationLog( brain.getAuditDir( appId ) );
        evalLog.add( new Stage( Stage.ID_BUILD ), scanId, false, "anonymous", "127.0.0.1" );
        setSaasResponseForURI( "/rest/ci/report?scanId=" + scanId, "Not Found", 404 );

        Response response = RestAccess.get( getServiceURL() );
        assertResponseStatus( 200, response );

        ApplicationManagementSummary[] applications =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicationManagementSummary[].class );
        Assert.assertNotNull( applications );
        Assert.assertEquals( 1, applications.length );
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

    @Test
    public void testUpdateApplication_AssignToOrganizationWithConflictingPolicy()
        throws Exception
    {
        Organization org = createOrganization( "orgName" );
        Application app = createApplication( "appPublicId", "appName", false, false );

        PolicyDAO policyDAO = new PolicyDAO( brain.getWorkDir() );
        String policyName = "A policy Name";

        Policy policy1 = new Policy( null, policyName );
        Constraint constraint1 = new Constraint( null, "constraint 1", LogicalOperator.AND );
        constraint1.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy1.addConstraint( constraint1 );
        policyDAO.insert( org.getId(), policy1 );

        Policy policy2 = new Policy( null, policyName );
        Constraint constraint2 = new Constraint( null, "constraint 1", LogicalOperator.AND );
        constraint2.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy2.addConstraint( constraint2 );
        policyDAO.insert( app.getId(), policy2 );

        app.setOrganizationId( org.getId() );

        Response response = RestAccess.put( getServiceURL(), JsonHelpers.asJson( app ) );
        assertResponseStatus( 400, response );
        Assert.assertEquals( "The following policies collide with policies of the parent organization: "
                                 + policy2.getName(), response.getResponseBody() );

        policy2.setName( policyName.replaceAll( "\\s", "" ).toLowerCase( Locale.ENGLISH ) );
        policyDAO.update( app.getId(), policy2 );

        response = RestAccess.put( getServiceURL(), JsonHelpers.asJson( app ) );
        assertResponseStatus( 400, response );
        Assert.assertEquals( "The following policies collide with policies of the parent organization: "
                                 + policy2.getName(), response.getResponseBody() );
    }

    @Test
    public void testUpdateApplication_AssignToOrganizationWhenHavingInvalidPolicyNames()
        throws Exception
    {
        Organization org = createOrganization( "orgName" );
        Application app = createApplication( "appPublicId", "appName", false, false );

        FileUtils.copyURLToFile( getClass().getResource( "/ApplicationResourceTest/invalid-policy-name.json" ),
                                 new File( new File( new File( brain.getWorkDir(), "policy" ), app.getId() ),
                                           "policy.json" ) );

        app.setOrganizationId( org.getId() );

        Response response = RestAccess.put( getServiceURL(), JsonHelpers.asJson( app ) );
        assertResponseStatus( 400, response );
        Assert.assertEquals( "The following policies have invalid names: Legacy Policy  with invalid name !?",
                             response.getResponseBody() );
    }

    @Test
    public void testGenerateIcon()
        throws Exception
    {
        String hashcode = "abababababababababab";
        String url = getGenerateIconServiceUrl( hashcode );
        String saasUrl = "rest/application/icon/generate/" + hashcode;
        setSaasResponseForURI( saasUrl, 200, loadDefaultIcon() );
        Response response = RestAccess.get( url );
        testValidIconResponse( response );
    }

    private void createDirectory( File dir )
    {
        if ( !dir.isDirectory() )
        {
            Assert.assertTrue( "create directory " + dir.getAbsolutePath(), dir.mkdirs() );
        }
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

    private String getSetSyncIconServiceUrl()
    {
        return getServiceURL() + "/" + ApplicationResource.ICON_PATH_SYNC;
    }

    private String getGenerateIconServiceUrl( String hashcode )
    {
        return getServiceURL() + "/" + ApplicationResource.GENERATE_ICON_PATH.replace( "{hashcode}", hashcode );
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
