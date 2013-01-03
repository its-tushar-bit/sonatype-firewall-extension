/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.net.URL;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.ning.http.client.Response;
import com.sonatype.insight.brain.model.policy.Action;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.PolicyResource;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class PolicyEvaluateResourceTest
    extends AbstractResourceTest
{
    private Response addPolicy( final String appId, final Policy policy )
        throws Exception
    {
        final Response response =
            RestAccess.post( getRestBaseUrl() + PolicyResource.SERVICE_PATH.replace( "{appId}", appId ),
                             JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        return response;
    }

    @Test
    public void testEvaluate()
        throws Exception
    {
        final String appId = "PolicyEvaluateResourceTest_AppId";
        final String scanId = "PolicyEvaluateResourceTest_ScanId";
        final File saasReportFile = getReportResponseFile( appId, scanId );
        saasReportFile.delete();

        final Constraint constraint1 =
            new Constraint( "C1", "PolicyEvaluateResourceTest constraint 1", LogicalOperator.AND );
        final Condition condition1 = new Condition();
        condition1.setConditionTypeId( SecurityVulnerabilityConditionType.ID );
        condition1.setOperator( "present" );
        constraint1.addCondition( condition1 );

        final Action action = new Action();
        action.setActionTypeId( NotifyActionType.ID );

        final Policy policy1 = new Policy();
        policy1.setId( "P1" );
        policy1.setName( "PolicyEvaluateResourceTest policy1" );
        policy1.setThreatLevel( 8 );
        policy1.addConstraint( constraint1 );
        addPolicy( appId, policy1 );

        final Constraint constraint2 =
            new Constraint( "C2", "PolicyEvaluateResourceTest constraint 2", LogicalOperator.AND );
        constraint2.addCondition( condition1 );

        // same conditions, but lower threat-level => analysis should show highest threat-level
        final Policy policy2 = new Policy();
        policy2.setId( "P2" );
        policy2.setName( "PolicyEvaluateResourceTest policy2" );
        policy2.setThreatLevel( 3 );
        policy2.addConstraint( constraint2 );
        policy1.addAction( BuildStageType.ID, action );
        addPolicy( appId, policy2 );

        final Stage stage = new Stage( BuildStageType.ID );

        // The report file is not available yet
        Response response = RestAccess.post( getServiceURL( appId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 404, response );

        // Simulate that the report is available
        final URL testReportFileUrl = getClass().getResource( "/PolicyEvaluateResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportFileUrl.getFile() ), saasReportFile );
        response = RestAccess.post( getServiceURL( appId, scanId ), JsonHelpers.asJson( stage ) );
        assertResponseStatus( 200, response );
        final PolicyAlert[] policyAlerts = JsonHelpers.fromJson( response.getResponseBody(), PolicyAlert[].class );
        Assert.assertNotNull( policyAlerts );
        Assert.assertEquals( 2, policyAlerts.length );
        SecurityVulnerabilityConditionTypeTest.assertFactCounts( 1, 6, policyAlerts[0] );

        // check the calculated policy threat
        response = RestAccess.get( getThreatsURL( appId, scanId ) );
        assertResponseStatus( 200, response );
        final JsonNode policyThreats = JsonUtils.parse( response.getResponseBody() ).get( "aaData" );
        Assert.assertNotNull( policyThreats );
        Assert.assertTrue( policyThreats.size() > 0 );
        Assert.assertEquals( 8, policyThreats.get( 0 ).get( "policyThreatLevel" ).asInt() );
    }

    private String getServiceURL( final String appId, final String scanId )
    {
        return getRestBaseUrl() + PolicyEvaluateResource.SERVICE_PATH.replace( "{appId}", appId ) + "?scanId=" + scanId
            + "&stageTypeId=" + BuildStageType.ID;
    }

    private String getThreatsURL( final String appId, final String scanId )
    {
        return getRestBaseUrl() + ReportResource.SERVICE_PATH.replace( "{appId}", appId ).replace( "{scanId}", scanId )
            + "/embedReport/policythreats.json";
    }
}
