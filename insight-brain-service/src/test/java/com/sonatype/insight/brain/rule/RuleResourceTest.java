/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.rule;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.model.rule.Rule;
import com.sonatype.insight.brain.service.TestInsightBrainService;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class RuleResourceTest
{
    private TestInsightBrainService service;

    @Before
    public void startService()
        throws Exception
    {
        service = new TestInsightBrainService();
        service.run( new String[] { "server" } );
    }

    @After
    public void stopService()
        throws Exception
    {
        if (service!= null)
        {
            service.stop();
        }
    }

    @Test
    public void testCRUD()
        throws Exception
    {
        String appId = "RuleResourceTest";

        // Add a rule
        Rule rule = new Rule();
        rule.setName( "RuleResourceTest new rule" );
        Response response = RestAccess.post( getServiceURL( appId ), JsonHelpers.asJson( rule ) );
        assertResponseStatus( 200, response );
        Rule rule1 = JsonHelpers.fromJson( response.getResponseBody(), Rule.class );
        Assert.assertNotNull( rule1.getId() );
        Assert.assertEquals( "RuleResourceTest new rule", rule1.getName() );

        // Get all rules
        response = RestAccess.get( getServiceURL( appId ) );
        assertResponseStatus( 200, response );
        Rule[] rules = JsonHelpers.fromJson( response.getResponseBody(), Rule[].class );
        Assert.assertNotNull( rules );
        Assert.assertEquals( 1, rules.length );
        Assert.assertEquals( rule1.getId(), rules[0].getId() );
        Assert.assertEquals( rule1.getName(), rules[0].getName() );

        // Update a rule
        rule = rule1;
        rule.setName( "RuleResourceTest updated rule" );
        response = RestAccess.put( getServiceURL( appId ), JsonHelpers.asJson( rule ) );
        assertResponseStatus( 200, response );
        rule1 = JsonHelpers.fromJson( response.getResponseBody(), Rule.class );
        Assert.assertEquals( "RuleResourceTest updated rule", rule1.getName() );

        // Get all rules
        response = RestAccess.get( getServiceURL( appId ) );
        assertResponseStatus( 200, response );
        rules = JsonHelpers.fromJson( response.getResponseBody(), Rule[].class );
        Assert.assertNotNull( rules );
        Assert.assertEquals( 1, rules.length );
        Assert.assertEquals( rule1.getId(), rules[0].getId() );
        Assert.assertEquals( rule1.getName(), rules[0].getName() );

        // Delete a rule
        rule = rule1;
        response =
            RestAccess.delete( getServiceURL( appId ), null /* username */, null /* password */,
                               JsonHelpers.asJson( rule ) );
        assertResponseStatus( 204, response );

        // Get all rules
        response = RestAccess.get( getServiceURL( appId ) );
        assertResponseStatus( 200, response );
        rules = JsonHelpers.fromJson( response.getResponseBody(), Rule[].class );
        Assert.assertNotNull( rules );
        Assert.assertEquals( 0, rules.length );
    }

    private String getServiceURL( String appId )
    {
        return RestAccess.BASE_URL + "rest/rule/" + appId;
    }

    protected static void assertResponseStatus( int expectedStatus, Response response )
        throws IOException
    {
        int actualStatus = response.getStatusCode();
        Assert.assertEquals( "URI:" + response.getUri() + ", StatusText:" + response.getStatusText()
            + ", ResponseBody:" + response.getResponseBody(), expectedStatus, actualStatus );
    }
}
