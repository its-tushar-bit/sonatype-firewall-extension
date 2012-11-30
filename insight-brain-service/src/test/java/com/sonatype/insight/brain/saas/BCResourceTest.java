/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;

public class BCResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testValidate()
        throws Exception
    {
        Response response;

        response = RestAccess.get( RestAccess.BASE_URL + BCResource.SERVICE_PATH + "/validate/alpha" );
        assertResponseStatus( 200, response );
        Assert.assertEquals( "OK", response.getResponseBody() );

        invalidateAppId( "alpha", "Expired" );

        // validate service always returns 200, the actual result is in the response body
        response = RestAccess.get( RestAccess.BASE_URL + BCResource.SERVICE_PATH + "/validate/alpha" );
        assertResponseStatus( 200, response );
        Assert.assertEquals( "Expired", response.getResponseBody() );
    }

    // @Test
    // public void testScan()
    // throws Exception
    // {
    // }

    // @Test
    // public void testReport()
    // throws Exception
    // {
    // }

    // @Test
    // public void testArtifact()
    // throws Exception
    // {
    // }
}
