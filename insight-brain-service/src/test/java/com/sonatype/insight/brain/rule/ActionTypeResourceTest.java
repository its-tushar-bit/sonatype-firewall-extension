/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.rule;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class ActionTypeResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testGetActionTypes()
        throws Exception
    {
        final Response response = RestAccess.get( getServiceURL() );
        assertResponseStatus( 200, response );
        final Object[] actionTypes = JsonHelpers.fromJson( response.getResponseBody(), Object[].class );
        Assert.assertNotNull( actionTypes );
        Assert.assertTrue( actionTypes.length > 0 );
    }

    private String getServiceURL()
    {
        return RestAccess.BASE_URL + ActionTypeResource.SERVICE_PATH;
    }
}
