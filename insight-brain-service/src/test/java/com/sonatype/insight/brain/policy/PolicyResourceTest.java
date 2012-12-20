/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;
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

        final JsonStore store = JsonUtils.fileStore( new File( "target/test-brain-work/policy/" + appId ) );

        Assert.assertEquals( 0, store.modificationCount() );

        // Add a policy
        Policy policy = new Policy();
        policy.setName( "PolicyResourceTest new policy" );
        Response response = RestAccess.post( getServiceURL( appId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        final Policy policy1 = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );
        Assert.assertNotNull( policy1.getId() );
        Assert.assertEquals( "PolicyResourceTest new policy", policy1.getName() );

        Assert.assertEquals( 1, store.modificationCount() );

        // Get all policies
        response = RestAccess.get( getServiceURL( appId ) );
        assertResponseStatus( 200, response );
        Policy[] policies = JsonHelpers.fromJson( response.getResponseBody(), Policy[].class );
        Assert.assertNotNull( policies );
        Assert.assertEquals( 1, policies.length );
        Assert.assertEquals( policy1.getId(), policies[0].getId() );
        Assert.assertEquals( policy1.getName(), policies[0].getName() );

        Assert.assertEquals( 1, store.modificationCount() );

        Assert.assertEquals( JsonUtils.asTree( new Policy[] { policy1 } ),
                             store.history( null, "policy.json" ).get( "aaData" ) );

        // Update a policy
        policy = policies[0];
        policy.setName( "PolicyResourceTest updated policy" );
        response = RestAccess.put( getServiceURL( appId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        final Policy policy2 = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );
        Assert.assertEquals( "PolicyResourceTest updated policy", policy2.getName() );

        Assert.assertEquals( 2, store.modificationCount() );

        // Get all policies
        response = RestAccess.get( getServiceURL( appId ) );
        assertResponseStatus( 200, response );
        policies = JsonHelpers.fromJson( response.getResponseBody(), Policy[].class );
        Assert.assertNotNull( policies );
        Assert.assertEquals( 1, policies.length );
        Assert.assertEquals( policy2.getId(), policies[0].getId() );
        Assert.assertEquals( policy2.getName(), policies[0].getName() );

        Assert.assertEquals( 2, store.modificationCount() );

        Assert.assertEquals( JsonUtils.asTree( new Policy[] { policy2, policy1 } ),
                             store.history( null, "policy.json" ).get( "aaData" ) );

        // Delete a policy
        policy = policies[0];
        response = RestAccess.delete( getServiceURL( appId, policy.getId() ) );
        assertResponseStatus( 204, response );

        Assert.assertEquals( 3, store.modificationCount() );

        // Get all policies
        response = RestAccess.get( getServiceURL( appId ) );
        assertResponseStatus( 200, response );
        policies = JsonHelpers.fromJson( response.getResponseBody(), Policy[].class );
        Assert.assertNotNull( policies );
        Assert.assertEquals( 0, policies.length );

        Assert.assertEquals( 3, store.modificationCount() );

        Assert.assertEquals( JsonUtils.asTree( new Policy[] { policy2, policy1 } ),
                             store.history( null, "policy.json" ).get( "aaData" ) );
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
