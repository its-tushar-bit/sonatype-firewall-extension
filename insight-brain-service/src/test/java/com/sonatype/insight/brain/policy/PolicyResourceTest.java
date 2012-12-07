/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class PolicyResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testCRUD()
        throws Exception
    {
        final String appId = "PolicyResourceTest";

        // Add a policy
        Policy policy = new Policy();
        policy.setName( "PolicyResourceTest new policy" );
        Response response = RestAccess.post( getServiceURL( appId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        Policy policy1 = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );
        Assert.assertNotNull( policy1.getId() );
        Assert.assertEquals( "PolicyResourceTest new policy", policy1.getName() );

        // Get all policies
        response = RestAccess.get( getServiceURL( appId ) );
        assertResponseStatus( 200, response );
        Policy[] policies = JsonHelpers.fromJson( response.getResponseBody(), Policy[].class );
        Assert.assertNotNull( policies );
        Assert.assertEquals( 1, policies.length );
        Assert.assertEquals( policy1.getId(), policies[0].getId() );
        Assert.assertEquals( policy1.getName(), policies[0].getName() );

        // Update a policy
        policy = policy1;
        policy.setName( "PolicyResourceTest updated policy" );
        response = RestAccess.put( getServiceURL( appId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        policy1 = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );
        Assert.assertEquals( "PolicyResourceTest updated policy", policy1.getName() );

        // Get all policies
        response = RestAccess.get( getServiceURL( appId ) );
        assertResponseStatus( 200, response );
        policies = JsonHelpers.fromJson( response.getResponseBody(), Policy[].class );
        Assert.assertNotNull( policies );
        Assert.assertEquals( 1, policies.length );
        Assert.assertEquals( policy1.getId(), policies[0].getId() );
        Assert.assertEquals( policy1.getName(), policies[0].getName() );

        // Delete a policy
        policy = policy1;
        response = RestAccess.delete( getServiceURL( appId, policy.getId() ) );
        assertResponseStatus( 204, response );

        // Get all policies
        response = RestAccess.get( getServiceURL( appId ) );
        assertResponseStatus( 200, response );
        policies = JsonHelpers.fromJson( response.getResponseBody(), Policy[].class );
        Assert.assertNotNull( policies );
        Assert.assertEquals( 0, policies.length );
    }

    private String getServiceURL( final String appId )
    {
        return getRestBaseUrl() + PolicyResource.SERVICE_PATH.replace( "{appId}", appId );
    }

    private String getServiceURL( final String appId, final String policyId )
    {
        return getServiceURL( appId ) + "/" + policyId;
    }
}
