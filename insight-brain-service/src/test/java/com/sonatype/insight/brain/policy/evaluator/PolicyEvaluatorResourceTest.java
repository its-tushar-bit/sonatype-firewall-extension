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

import com.ning.http.client.Response;
import com.sonatype.insight.brain.model.component.PolicyFact;
import com.sonatype.insight.brain.model.rule.Action;
import com.sonatype.insight.brain.model.rule.LogicalOperator;
import com.sonatype.insight.brain.model.rule.MarkAsFailedActionType;
import com.sonatype.insight.brain.model.rule.Rule;
import com.sonatype.insight.brain.model.rule.SecurityVulnerabilityPresentConditionType;
import com.sonatype.insight.brain.model.rule.SimpleCondition;
import com.sonatype.insight.brain.rule.RuleResourceTest;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class PolicyEvaluatorResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testEvaluate()
        throws Exception
    {
        final String appId = "PolicyEvaluatorResourceTest_AppId";
        final String scanId = "PolicyEvaluatorResourceTest_ScanId";
        final File saasReportFile = getReportResponseFile( appId, scanId );
        saasReportFile.delete();

        final Rule rule = new Rule();
        rule.setName( "PolicyEvaluatorResourceTest rule 1" );
        rule.setOperator( LogicalOperator.AND );
        final SimpleCondition condition1 = new SimpleCondition();
        condition1.setConditionTypeId( SecurityVulnerabilityPresentConditionType.ID );
        condition1.setOperator( "present" );
        rule.addCondition( condition1 );
        final Action action = new Action();
        action.setActionTypeId( MarkAsFailedActionType.ID );
        rule.addAction( action );
        RuleResourceTest.addRule( appId, rule );

        // The report file is not available yet
        Response response = RestAccess.get( getServiceURL( appId, scanId ) );
        assertResponseStatus( 404, response );

        // Simulate that the report is available
        final URL testReportFileUrl = getClass().getResource( "/PolicyEvaluatorResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportFileUrl.getFile() ), saasReportFile );
        response = RestAccess.get( getServiceURL( appId, scanId ) );
        assertResponseStatus( 200, response );
        final PolicyFact[] policyFacts = JsonHelpers.fromJson( response.getResponseBody(), PolicyFact[].class );
        Assert.assertNotNull( policyFacts );
        Assert.assertTrue( policyFacts.length > 0 );
    }

    private String getServiceURL( final String appId, final String scanId )
    {
        return RestAccess.BASE_URL + PolicyEvaluatorResource.SERVICE_PATH.replace( "{appId}", appId ) + "/" + scanId;
    }
}
