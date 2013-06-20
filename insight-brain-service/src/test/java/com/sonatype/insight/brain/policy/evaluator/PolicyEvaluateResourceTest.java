/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.mail.Message;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.ning.http.client.Response;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.PolicyResource;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateResource.MailPolicyAlertCounts;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class PolicyEvaluateResourceTest
    extends AbstractResourceTest
{
    private Response addPolicy( final String applicationPublicId, final Policy policy )
        throws Exception
    {
        final Response response =
            RestAccess.post( getRestBaseUrl()
                                 + PolicyResource.SERVICE_PATH.replace( "{policyOwnerId}", applicationPublicId ),
                             JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        return response;
    }

    private Response updatePolicy( final String applicationPublicId, final Policy policy )
        throws Exception
    {
        final Response response =
            RestAccess.put( getRestBaseUrl()
                                + PolicyResource.SERVICE_PATH.replace( "{policyOwnerId}", applicationPublicId ),
                            JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );

        return response;
    }

    @Test
    public void testEvaluate_MultipleMatchesForSameGAV()
        throws Exception
    {
        String applicationPublicId = "testEvaluate_MultipleMatchesForSameGAV_AppId";
        createApplication( applicationPublicId );
        String scanId = "testEvaluate_MultipleMatchesForSameGAV_ScanId";
        String licenseFingerprint = "testEvaluate_MultipleMatchesForSameGAV_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );

        File saasReportFile = getReportResponseFile( licenseFingerprint, scanId );
        saasReportFile.delete();

        Constraint constraintLicense =
            new Constraint( null /* constraintId */, "Constraint License", LogicalOperator.AND );
        Condition condition1 = new Condition( LicenseConditionType.ID, "is", "UNSPECIFIED" );
        constraintLicense.addCondition( condition1 );
        Constraint constraintSV = new Constraint( null /* constraintId */, "Constraint SV", LogicalOperator.AND );
        Condition condition2 = new Condition( SecurityVulnerabilityConditionType.ID, "present" );
        constraintSV.addCondition( condition2 );

        Action action = new Action( FailActionType.ID );

        Policy policy1 = new Policy( null /* policyId */, "Policy 1" );
        policy1.setThreatLevel( 5 );
        policy1.addConstraint( constraintLicense );
        policy1.addConstraint( constraintSV );
        policy1.addAction( BuildStageType.ID, action );
        Response response = addPolicy( applicationPublicId, policy1 );
        policy1 = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );
        constraintLicense = policy1.getConstraints().get( 0 );
        constraintSV = policy1.getConstraints().get( 1 );

        Stage stage = new Stage( BuildStageType.ID );

        // The report file is not available yet
        response = RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 404, response );

        // Simulate that the report is available
        URL testReportFileUrl =
            getClass().getResource( "/PolicyEvaluateResourceTest/MultipleMatchesForSameGAV/report.zip" );
        FileUtils.copyFile( new File( testReportFileUrl.getFile() ), saasReportFile );
        response = RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 200, response );
        PolicyEvaluationResult policyEval =
            JsonHelpers.fromJson( response.getResponseBody(), PolicyEvaluationResult.class );
        Assert.assertNotNull( policyEval );
        Assert.assertEquals( 3, policyEval.getAffectedComponentCount() );
        Assert.assertEquals( 0, policyEval.getCriticalComponentCount() );
        Assert.assertEquals( 3, policyEval.getSevereComponentCount() );
        Assert.assertEquals( 0, policyEval.getModerateComponentCount() );
        List<PolicyAlert> policyAlerts = policyEval.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        AbstractPolicyEvaluationTest.assertFactCounts( 2, 3, policyAlerts.get( 0 ) );
        Component expectedComponentExact = new Component( "tomcat", "tomcat-util", "5.0.28", MatchState.EXACT );
        expectedComponentExact.setHash( "3102cdd0edd5a05afe00" );
        Component expectedComponentSimilar1 = new Component( "tomcat", "tomcat-util", "5.0.28", MatchState.SIMILAR );
        expectedComponentSimilar1.setHash( "d29a75f9056e0b040f09" );
        Component expectedComponentSimilar2 = new Component( "tomcat", "tomcat-util", "5.0.28", MatchState.SIMILAR );
        expectedComponentSimilar2.setHash( "707df42012875442b9df" );
        AbstractPolicyEvaluationTest.assertContainsPolicyAlert( expectedComponentExact, policy1.getId(), "Policy 1",
                                                                FailActionType.ID, constraintLicense.getId(),
                                                                "Constraint License", LicenseConditionType.ID,
                                                                policyAlerts );
        AbstractPolicyEvaluationTest.assertContainsPolicyAlert( expectedComponentExact, policy1.getId(), "Policy 1",
                                                                FailActionType.ID, constraintSV.getId(),
                                                                "Constraint SV", SecurityVulnerabilityConditionType.ID,
                                                                policyAlerts );
        AbstractPolicyEvaluationTest.assertContainsPolicyAlert( expectedComponentSimilar1, policy1.getId(), "Policy 1",
                                                                FailActionType.ID, constraintLicense.getId(),
                                                                "Constraint License", LicenseConditionType.ID,
                                                                policyAlerts );
        AbstractPolicyEvaluationTest.assertContainsPolicyAlert( expectedComponentSimilar1, policy1.getId(), "Policy 1",
                                                                FailActionType.ID, constraintSV.getId(),
                                                                "Constraint SV", SecurityVulnerabilityConditionType.ID,
                                                                policyAlerts );
        AbstractPolicyEvaluationTest.assertContainsPolicyAlert( expectedComponentSimilar2, policy1.getId(), "Policy 1",
                                                                FailActionType.ID, constraintLicense.getId(),
                                                                "Constraint License", LicenseConditionType.ID,
                                                                policyAlerts );
        AbstractPolicyEvaluationTest.assertContainsPolicyAlert( expectedComponentSimilar2, policy1.getId(), "Policy 1",
                                                                FailActionType.ID, constraintSV.getId(),
                                                                "Constraint SV", SecurityVulnerabilityConditionType.ID,
                                                                policyAlerts );
    }

    @Test
    public void testEvaluate_ManuallyIdentifiedComponent()
        throws Exception
    {
        String applicationPublicId = "testEvaluate_ManuallyIdentifiedComponent_AppId";
        createApplication( applicationPublicId );
        String scanId = "testEvaluate_ManuallyIdentifiedComponent_ScanId";
        String licenseFingerprint = "testEvaluate_ManuallyIdentifiedComponent_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );

        File saasReportFile = getReportResponseFile( licenseFingerprint, scanId );
        saasReportFile.delete();

        Constraint constraint1 = new Constraint( null /* constraintId */, "Constraint 1", LogicalOperator.AND );
        Condition condition1 = new Condition( MatchStateConditionType.ID, "is", "exact" );
        constraint1.addCondition( condition1 );

        Action action = new Action( FailActionType.ID );

        Policy policy1 = new Policy( null /* policyId */, "Policy 1" );
        policy1.setThreatLevel( 5 );
        policy1.addConstraint( constraint1 );
        policy1.addAction( BuildStageType.ID, action );
        Response response = addPolicy( applicationPublicId, policy1 );
        policy1 = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );
        constraint1 = policy1.getConstraints().get( 0 );

        Stage stage = new Stage( BuildStageType.ID );

        String hash = "5801a1a27a36f88e2089";
        String groupId = "G";
        String artifactId = "A";
        String version = "V";
        HashGAV hashGAV = new HashGAV( hash, groupId, artifactId, version, null /* extension */, null /* classifier */);
        HashGAVDAO hashGAVDAO = new HashGAVDAO();
        hashGAVDAO.insert( hashGAV );
        // The report file is not available yet
        response = RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 404, response );

        // Simulate that the report is available
        URL testReportFileUrl =
            getClass().getResource( "/PolicyEvaluateResourceTest/ManuallyIdentifiedComponent/report.zip" );
        FileUtils.copyFile( new File( testReportFileUrl.getFile() ), saasReportFile );
        response = RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( stage ) );
        hashGAVDAO.delete( hashGAV );
        assertResponseStatus( 200, response );
        PolicyEvaluationResult policyEval =
            JsonHelpers.fromJson( response.getResponseBody(), PolicyEvaluationResult.class );
        Assert.assertNotNull( policyEval );
        Assert.assertEquals( 1, policyEval.getAffectedComponentCount() );
        Assert.assertEquals( 0, policyEval.getCriticalComponentCount() );
        Assert.assertEquals( 1, policyEval.getSevereComponentCount() );
        Assert.assertEquals( 0, policyEval.getModerateComponentCount() );
        List<PolicyAlert> policyAlerts = policyEval.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        AbstractPolicyEvaluationTest.assertFactCounts( 1, 1, policyAlerts.get( 0 ) );
        Component expectedComponentExact = new Component( groupId, artifactId, version, MatchState.EXACT );
        expectedComponentExact.setHash( hash );
        AbstractPolicyEvaluationTest.assertContainsPolicyAlert( expectedComponentExact, policy1.getId(), "Policy 1",
                                                                FailActionType.ID, constraint1.getId(), "Constraint 1",
                                                                MatchStateConditionType.ID, policyAlerts );
    }

    @Test
    public void testEvaluate()
        throws Exception
    {
        final String applicationPublicId = "PolicyEvaluateResourceTest_AppId";
        createApplication( applicationPublicId );
        final String scanId = "PolicyEvaluateResourceTest_ScanId";
        String licenseFingerprint = "PolicyEvaluateResourceTest_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );
        
        final File saasReportFile = getReportResponseFile( licenseFingerprint, scanId );
        saasReportFile.delete();

        final Constraint constraint1 =
            new Constraint( "C1", "PolicyEvaluateResourceTest constraint 1", LogicalOperator.AND );
        final Condition condition1 = new Condition( SecurityVulnerabilityConditionType.ID, "present" );
        constraint1.addCondition( condition1 );
        final Policy policy1 = new Policy( "P1", "PolicyEvaluateResourceTest policy1" );
        policy1.setThreatLevel( 8 );
        policy1.addConstraint( constraint1 );
        final Action notifyAction = new Action( NotifyActionType.ID );
        notifyAction.setTarget( "manager@test.corp" );
        policy1.addAction( BuildStageType.ID, notifyAction );
        final Action notifyAction2 = new Action( NotifyActionType.ID );
        notifyAction2.setTarget( "john.doe@test.corp" );
        policy1.addAction( BuildStageType.ID, notifyAction2 );
        addPolicy( applicationPublicId, policy1 );

        final Constraint constraint2 =
            new Constraint( "C2", "PolicyEvaluateResourceTest constraint 2", LogicalOperator.AND );
        final Condition condition2 = new Condition( SecurityVulnerabilityConditionType.ID, "present" );
        constraint2.addCondition( condition2 );
        // same conditions, but lower threat-level => analysis should show highest threat-level
        final Policy policy2 = new Policy( "P2", "PolicyEvaluateResourceTest policy2" );
        policy2.setThreatLevel( 3 );
        policy2.addConstraint( constraint2 );
        addPolicy( applicationPublicId, policy2 );

        final Stage stage = new Stage( BuildStageType.ID );

        // The report file is not available yet
        Response response = RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 404, response );

        // Simulate that the report is available
        final URL testReportFileUrl = getClass().getResource( "/PolicyEvaluateResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportFileUrl.getFile() ), saasReportFile );

        final List<Message> messagesA = Mailbox.get( "manager@test.corp" );
        final List<Message> messagesB = Mailbox.get( "john.doe@test.corp" );

        messagesA.clear();
        messagesB.clear();

        // evaluate policy
        response = RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 200, response );
        PolicyEvaluationResult policyEval = JsonHelpers.fromJson( response.getResponseBody(), PolicyEvaluationResult.class );
        Assert.assertNotNull( policyEval );
        Assert.assertEquals( 7, policyEval.getAffectedComponentCount() );
        Assert.assertEquals( 7, policyEval.getCriticalComponentCount() );
        Assert.assertEquals( 0, policyEval.getSevereComponentCount() );
        Assert.assertEquals( 0, policyEval.getModerateComponentCount() );
        List<PolicyAlert> policyAlerts = policyEval.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 2, policyAlerts.size() );
        AbstractPolicyEvaluationTest.assertFactCounts( 1, 7, policyAlerts.get( 0 ) );

        // check the calculated policy threat
        response = RestAccess.get( getThreatsURL( applicationPublicId, scanId ) );
        assertResponseStatus( 200, response );
        final JsonNode policyThreats = JsonUtils.parse( response.getResponseBody() ).get( "aaData" );
        Assert.assertNotNull( policyThreats );
        Assert.assertTrue( policyThreats.size() > 0 );
        Assert.assertEquals( 8, policyThreats.get( 0 ).get( "policyThreatLevel" ).asInt() );

        // notification message should also have been sent
        Assert.assertEquals( 1, messagesA.size() );
        Assert.assertTrue( messagesA.get( 0 ).getSubject().contains( "Policy" ) );
        Assert.assertEquals( 1, messagesB.size() );
        Assert.assertTrue( messagesB.get( 0 ).getSubject().contains( "Policy" ) );

        messagesA.clear();
        messagesB.clear();

        // evaluate policy again
        response = RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 200, response );
        policyEval = JsonHelpers.fromJson( response.getResponseBody(), PolicyEvaluationResult.class );
        Assert.assertNotNull( policyEval );
        policyAlerts = policyEval.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 2, policyAlerts.size() );
        AbstractPolicyEvaluationTest.assertFactCounts( 1, 7, policyAlerts.get( 0 ) );

        // notification message should not have been sent since the results are the same
        Assert.assertTrue( messagesA.isEmpty() );
        Assert.assertTrue( messagesB.isEmpty() );
    }

    @Test
    public void testPolicyThreatLevelCounts()
        throws Exception
    {
        final String applicationPublicId = "PolicyThreatCountResourceTest_AppId";
        createApplication( applicationPublicId );
        final String scanId = "PolicyThreatCountResourceTest_ScanId";
        String licenseFingerprint = "PolicyThreatCountResourceTest_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );

        final File saasReportFile = getReportResponseFile( licenseFingerprint, scanId );
        saasReportFile.delete();

        final Constraint constraint =
            new Constraint( "C1", "PolicyThreatCountResourceTest constraint 1", LogicalOperator.AND );
        final Condition condition = new Condition( SecurityVulnerabilityConditionType.ID, "present" );
        constraint.addCondition( condition );
        Policy policy = new Policy( "P1", "PolicyThreatCountResourceTest policy1" );
        policy.setThreatLevel( 1 );
        policy.addConstraint( constraint );
        Response addPolicyResponse = addPolicy( applicationPublicId, policy );
        policy = JsonHelpers.fromJson( addPolicyResponse.getResponseBody(), Policy.class );

        final Stage stage = new Stage( BuildStageType.ID );

        // The report file is not available yet
        Response response = RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 404, response );

        // Simulate that the report is available
        final URL testReportFileUrl = getClass().getResource( "/PolicyEvaluateResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportFileUrl.getFile() ), saasReportFile );

        // Threat Level 1 Should not show up in any counts
        response = RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 200, response );

        PolicyEvaluationResult policyEval = JsonHelpers.fromJson( response.getResponseBody(), PolicyEvaluationResult.class );
        Assert.assertNotNull( policyEval );
        Assert.assertEquals( 7, policyEval.getAffectedComponentCount() );
        Assert.assertEquals( 0, policyEval.getCriticalComponentCount() );
        Assert.assertEquals( 0, policyEval.getSevereComponentCount() );
        Assert.assertEquals( 0, policyEval.getModerateComponentCount() );

        policy.setThreatLevel( 2 );
        updatePolicy( applicationPublicId, policy );

        // Threat Level 2 should show up as moderate
        response = RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 200, response );
        policyEval = JsonHelpers.fromJson( response.getResponseBody(), PolicyEvaluationResult.class );
        Assert.assertNotNull( policyEval );
        Assert.assertEquals( 7, policyEval.getAffectedComponentCount() );
        Assert.assertEquals( 0, policyEval.getCriticalComponentCount() );
        Assert.assertEquals( 0, policyEval.getSevereComponentCount() );
        Assert.assertEquals( 7, policyEval.getModerateComponentCount() );

        policy.setThreatLevel( 4 );
        updatePolicy( applicationPublicId, policy );

        // Threat Level 4 should show up as severe
        response = RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 200, response );
        policyEval = JsonHelpers.fromJson( response.getResponseBody(), PolicyEvaluationResult.class );
        Assert.assertNotNull( policyEval );
        Assert.assertEquals( 7, policyEval.getAffectedComponentCount() );
        Assert.assertEquals( 0, policyEval.getCriticalComponentCount() );
        Assert.assertEquals( 7, policyEval.getSevereComponentCount() );
        Assert.assertEquals( 0, policyEval.getModerateComponentCount() );

        policy.setThreatLevel( 8 );
        updatePolicy( applicationPublicId, policy );

        // Threat Level 8 should show up as severe
        response = RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 200, response );
        policyEval = JsonHelpers.fromJson( response.getResponseBody(), PolicyEvaluationResult.class );
        Assert.assertNotNull( policyEval );
        Assert.assertEquals( 7, policyEval.getAffectedComponentCount() );
        Assert.assertEquals( 7, policyEval.getCriticalComponentCount() );
        Assert.assertEquals( 0, policyEval.getSevereComponentCount() );
        Assert.assertEquals( 0, policyEval.getModerateComponentCount() );
    }

    @Test
    public void testEvaluate_MultiLicense()
        throws Exception
    {
        String applicationPublicId = "testEvaluate_MultiLicense_AppId";
        createApplication( applicationPublicId );
        String scanId = "testEvaluate_MultiLicense_ScanId";
        String licenseFingerprint = "testEvaluate_MultiLicense_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );
        
        File saasReportFile = getReportResponseFile( licenseFingerprint, scanId );
        saasReportFile.delete();

        Constraint constraint1 = new Constraint( null /* constraintId */, "Constraint 1", LogicalOperator.AND );
        Condition condition1 = new Condition( LicenseConditionType.ID, "is", "GPL-2.0" );
        constraint1.addCondition( condition1 );

        Action action = new Action( FailActionType.ID );

        Policy policy1 = new Policy( null /* policyId */, "Policy 1" );
        policy1.setThreatLevel( 5 );
        policy1.addConstraint( constraint1 );
        policy1.addAction( BuildStageType.ID, action );
        Response response = addPolicy( applicationPublicId, policy1 );
        policy1 = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );
        constraint1 = policy1.getConstraints().get( 0 );

        Stage stage = new Stage( BuildStageType.ID );

        // The report file is not available yet
        response = RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 404, response );

        // Simulate that the report is available
        URL testReportFileUrl = getClass().getResource( "/PolicyEvaluateResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportFileUrl.getFile() ), saasReportFile );
        response = RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 200, response );
        PolicyEvaluationResult policyEval =
            JsonHelpers.fromJson( response.getResponseBody(), PolicyEvaluationResult.class );
        Assert.assertNotNull( policyEval );
        Assert.assertEquals( 3, policyEval.getAffectedComponentCount() );
        Assert.assertEquals( 0, policyEval.getCriticalComponentCount() );
        Assert.assertEquals( 3, policyEval.getSevereComponentCount() );
        Assert.assertEquals( 0, policyEval.getModerateComponentCount() );
        List<PolicyAlert> policyAlerts = policyEval.getAlerts();
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 1, policyAlerts.size() );
        AbstractPolicyEvaluationTest.assertFactCounts( 1, 3, policyAlerts.get( 0 ) );
        Component expectedComponent = new Component( "org.webjars", "select2", "3.2", MatchState.EXACT );
        expectedComponent.setHash( "f2e35e4a21f07d25710f" );
        AbstractPolicyEvaluationTest.assertContainsPolicyAlert( expectedComponent, policy1.getId(), "Policy 1",
                                                                FailActionType.ID, constraint1.getId(), "Constraint 1",
                                                                LicenseConditionType.ID, policyAlerts );
    }

    @Test
    public void testNotificationEmailModel()
        throws Exception
    {
        final String applicationPublicId = "PolicyEvaluateResourceTest_AppId";
        createApplication( applicationPublicId );
        final String scanId = "PolicyEvaluateResourceTest_ScanId";
        String licenseFingerprint = "PolicyEvaluateResourceTest_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );
        
        final File saasReportFile = getReportResponseFile( licenseFingerprint, scanId );
        saasReportFile.delete();

        final Constraint constraint1 =
            new Constraint( "C1", "PolicyEvaluateResourceTest constraint 1", LogicalOperator.AND );
        constraint1.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        final Policy policy1 = new Policy( "P1", "PolicyEvaluateResourceTest policy1" );
        policy1.setThreatLevel( 8 );
        policy1.addConstraint( constraint1 );
        addPolicy( applicationPublicId, policy1 );

        final Constraint constraint2 =
            new Constraint( "C2", "PolicyEvaluateResourceTest constraint 2", LogicalOperator.AND );
        constraint2.addCondition( new Condition( CoordinatesConditionType.ID, "match", "tomcat" ) );
        final Policy policy2 = new Policy( "P2", "PolicyEvaluateResourceTest policy2" );
        policy2.setThreatLevel( 4 );
        policy2.addConstraint( constraint2 );
        addPolicy( applicationPublicId, policy2 );

        final Constraint constraint3 =
            new Constraint( "C3", "PolicyEvaluateResourceTest constraint 3", LogicalOperator.AND );
        constraint3.addCondition( new Condition( CoordinatesConditionType.ID, "match", "org.*" ) );
        final Policy policy3 = new Policy( "P3", "PolicyEvaluateResourceTest policy3" );
        policy3.setThreatLevel( 3 );
        policy3.addConstraint( constraint3 );
        addPolicy( applicationPublicId, policy3 );

        final Constraint constraint4 =
            new Constraint( "C4", "PolicyEvaluateResourceTest constraint 1", LogicalOperator.AND );
        constraint4.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "absent" ) );
        final Policy policy4 = new Policy( "P4", "PolicyEvaluateResourceTest policy4" );
        policy4.setThreatLevel( 0 );
        policy4.addConstraint( constraint4 );
        addPolicy( applicationPublicId, policy4 );

        final Stage stage = new Stage( BuildStageType.ID );

        final URL testReportFileUrl = getClass().getResource( "/PolicyEvaluateResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportFileUrl.getFile() ), saasReportFile );

        String serverUrl = "http://localhost/";
        String cdnUrl = "http://cdn.localhost/";

        Response response = RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 200, response );
        PolicyEvaluationResult policyEval =
            JsonHelpers.fromJson( response.getResponseBody(), PolicyEvaluationResult.class );
        List<PolicyAlert> policyAlerts = policyEval.getAlerts();
        Map<String, Object> model =
            PolicyEvaluateResource.createPolicyMailModel( serverUrl, cdnUrl, applicationPublicId, scanId, stage,
                                                          policyAlerts );
        Assert.assertNotNull( model );
        Assert.assertEquals( policyAlerts, model.get( "policyAlerts" ) );
        Assert.assertEquals( cdnUrl, model.get( "cdnUrl" ) );
        Assert.assertEquals( serverUrl + ReportResource.getReportPath( applicationPublicId, scanId ),
                             model.get( "detailedReportUrl" ) );
        Assert.assertEquals( 7, model.get( "policyThreatRedCount" ) );
        Assert.assertEquals( 3, model.get( "policyThreatOrangeCount" ) );
        Assert.assertEquals( 13, model.get( "policyThreatYellowCount" ) );
        Assert.assertEquals( 21, model.get( "policyThreatBlueCount" ) );
        Assert.assertEquals( "Build", model.get( "policyThreatStage" ) );
        Assert.assertEquals( applicationPublicId, model.get( "policyThreatApp" ) );
        Assert.assertNotNull( model.get( "policyThreatTime" ) );
    }

    @Test
    public void testNotificationEmailSubject()
        throws Exception
    {
        Assert.assertEquals( "Policy Alert: 1 critical violation out of 15",
                             PolicyEvaluateResource.createPolicyMailSubject( new MailPolicyAlertCounts( 1, 2, 3, 4, 5 ) ) );
        Assert.assertEquals( "Policy Alert: 2 severe violations out of 14",
                             PolicyEvaluateResource.createPolicyMailSubject( new MailPolicyAlertCounts( 0, 2, 3, 4, 5 ) ) );
        Assert.assertEquals( "Policy Alert: 3 moderate violations out of 12",
                             PolicyEvaluateResource.createPolicyMailSubject( new MailPolicyAlertCounts( 0, 0, 3, 4, 5 ) ) );
        Assert.assertEquals( "Policy Alert: 9 neutral violations out of 9",
                             PolicyEvaluateResource.createPolicyMailSubject( new MailPolicyAlertCounts( 0, 0, 0, 4, 5 ) ) );
        Assert.assertEquals( "Policy Alert: 5 neutral violations out of 5",
                             PolicyEvaluateResource.createPolicyMailSubject( new MailPolicyAlertCounts( 0, 0, 0, 0, 5 ) ) );
    }

    @Test
    public void testErrorReport()
        throws Exception
    {
        final String applicationPublicId = "PolicyEvaluateResourceTest_AppId";
        createApplication( applicationPublicId );
        final String scanId = "PolicyEvaluateResourceTest_ScanId";
        String licenseFingerprint = "PolicyEvaluateResourceTest_LicenseFingerprint";
        setLicenseFingerprint( licenseFingerprint );

        final File saasReportFile = getReportResponseFile( licenseFingerprint, scanId );
        saasReportFile.delete();

        final URL testReportFileUrl = getClass().getResource( "/PolicyEvaluateResourceTest/empty_report.zip" );
        FileUtils.copyFile( new File( testReportFileUrl.getFile() ), saasReportFile );

        Response response =
            RestAccess.post( getServiceURL( applicationPublicId, scanId ), JsonHelpers.asJson( Stage.ID_BUILD ) );
        assertResponseStatus( 400, response );
    }

    private String getServiceURL( final String appId, final String scanId )
    {
        return getRestBaseUrl() + PolicyEvaluateResource.SERVICE_PATH.replace( "{applicationPublicId}", appId )
            + "?scanId=" + scanId;
    }

    private String getThreatsURL( final String applicationPublicId, final String scanId )
    {
        return getRestBaseUrl()
            + ReportResource.SERVICE_PATH.replace( "{applicationPublicId}", applicationPublicId ).replace( "{scanId}",
                                                                                                           scanId )
            + "/embedReport/policythreats.json";
    }
}
