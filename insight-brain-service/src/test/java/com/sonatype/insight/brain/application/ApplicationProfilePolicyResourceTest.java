/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.application;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.ApplicationProfileDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationProfilePolicyDAO;
import com.sonatype.insight.brain.model.ApplicationProfile;
import com.sonatype.insight.brain.model.ApplicationProfilePolicy;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class ApplicationProfilePolicyResourceTest
    extends AbstractResourceTest
{
    private ApplicationProfile applicationProfile;

    @After
    public void cleanUp()
    {
        if ( applicationProfile != null )
        {
            new ApplicationProfileDAO().delete( applicationProfile );
        }
    }

    @Test
    public void testGetSet()
        throws Exception
    {
        ApplicationProfileDAO applicationProfileDAO = new ApplicationProfileDAO();
        applicationProfile = new ApplicationProfile( "My app profile" );
        applicationProfileDAO.insert( applicationProfile );
        String applicationProfileId = applicationProfile.getId();

        ApplicationProfilePolicyDAO dao = new ApplicationProfilePolicyDAO();

        // Get
        Response response = RestAccess.get( getServiceURL( applicationProfileId ) );
        assertResponseStatus( 200, response );
        ApplicationProfilePolicy[] applicationProfilePolicies =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicationProfilePolicy[].class );
        Assert.assertNotNull( applicationProfilePolicies );
        Assert.assertEquals( 0, applicationProfilePolicies.length );

        // Set
        Set<String> policyIds = new LinkedHashSet<String>();
        policyIds.add( "policyId1" );
        policyIds.add( "policyId2" );
        dao.set( applicationProfileId, policyIds );
        response = RestAccess.put( getServiceURL( applicationProfileId ), JsonHelpers.asJson( policyIds ) );
        assertResponseStatus( 200, response );
        applicationProfilePolicies =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicationProfilePolicy[].class );
        Assert.assertNotNull( applicationProfilePolicies );
        Assert.assertEquals( 2, applicationProfilePolicies.length );

        // Get
        response = RestAccess.get( getServiceURL( applicationProfileId ) );
        assertResponseStatus( 200, response );
        applicationProfilePolicies =
            JsonHelpers.fromJson( response.getResponseBody(), ApplicationProfilePolicy[].class );
        Assert.assertNotNull( applicationProfilePolicies );
        Assert.assertEquals( 2, applicationProfilePolicies.length );
    }

    private String getServiceURL( String applicationProfileId )
    {
        return getRestBaseUrl()
            + ApplicationProfilePolicyResource.SERVICE_PATH.replace( "{applicationProfileId}", applicationProfileId );
    }
}
