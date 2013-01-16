/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class FeatureResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testFeatures()
        throws Exception
    {
        // Get all features
        Response response = RestAccess.get( getServiceURL() );
        assertResponseStatus( 200, response );
        String[] features = JsonHelpers.fromJson( response.getResponseBody(), String[].class );
        Assert.assertNotNull( features );
        Assert.assertTrue( Arrays.asList( features ).contains( "policy" ) );
        Assert.assertTrue( Arrays.asList( features ).contains( "labels" ) );
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + FeaturesResource.SERVICE_PATH;
    }
}
