/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.File;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ning.http.client.Response;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
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

        final JsonStore store = JsonUtils.fileStore( new File( brain.getWorkDir(), "policy/" + appId ) );

        Assert.assertEquals( 0, store.modificationCount() );

        // Add a policy
        Policy policy = new Policy();
        policy.setName( "PolicyResourceTest new policy" );
        Constraint constraint = new Constraint();
        constraint.setName( "PolicyResourceTest new constraint" );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
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

        ObjectNode json;

        json = (ObjectNode) store.history( null, "policy.json" ).get( "aaData" ).get( 0 );
        json = json.without( Arrays.asList( "user", "ip", "where", "time", "filename" ) );
        Assert.assertEquals( JsonUtils.asTree( policy1 ), json );

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

        json = (ObjectNode) store.history( null, "policy.json" ).get( "aaData" ).get( 0 );
        json = json.without( Arrays.asList( "user", "ip", "where", "time", "filename" ) );
        Assert.assertEquals( JsonUtils.asTree( policy2 ), json );

        json = (ObjectNode) store.history( null, "policy.json" ).get( "aaData" ).get( 1 );
        json = json.without( Arrays.asList( "user", "ip", "where", "time", "filename" ) );
        Assert.assertEquals( JsonUtils.asTree( policy1 ), json );

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

        json = (ObjectNode) store.history( null, "policy.json" ).get( "aaData" ).get( 0 );
        json = json.without( Arrays.asList( "user", "ip", "where", "time", "filename" ) );
        Assert.assertEquals( JsonUtils.asTree( policy2 ), json );

        json = (ObjectNode) store.history( null, "policy.json" ).get( "aaData" ).get( 1 );
        json = json.without( Arrays.asList( "user", "ip", "where", "time", "filename" ) );
        Assert.assertEquals( JsonUtils.asTree( policy1 ), json );
    }

    @Test
    public void testCreateInvalidPolicy()
        throws Exception
    {
        String appId = "PolicyResourceTest_testCreateInvalidPolicy";
        JsonStore store = JsonUtils.fileStore( new File( brain.getWorkDir(), "policy/" + appId ) );
        Assert.assertEquals( 0, store.modificationCount() );

        Policy policy = new Policy();
        policy.setName( null );
        Constraint constraint = new Constraint();
        constraint.setName( "PolicyResourceTest new constraint" );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        Response response = RestAccess.post( getServiceURL( appId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 400, response );
        Assert.assertEquals( "The policy name must not be null or empty", response.getResponseBody() );
    }

    @Test
    public void testUpdateInvalidPolicy()
        throws Exception
    {
        String appId = "PolicyResourceTest_testUpdateInvalidPolicy";
        JsonStore store = JsonUtils.fileStore( new File( brain.getWorkDir(), "policy/" + appId ) );
        Assert.assertEquals( 0, store.modificationCount() );

        // Create a valid policy
        Policy policy = new Policy();
        policy.setName( "PolicyResourceTest_testUpdateInvalidPolicy" );
        Constraint constraint = new Constraint();
        constraint.setName( "PolicyResourceTest new constraint" );
        constraint.addCondition( new Condition( SecurityVulnerabilityConditionType.ID, "present" ) );
        policy.addConstraint( constraint );
        Response response = RestAccess.post( getServiceURL( appId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        policy = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );

        // Update invalid policy
        policy.setName( null );
        response = RestAccess.put( getServiceURL( appId ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 400, response );
        Assert.assertEquals( "The policy name must not be null or empty", response.getResponseBody() );
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
