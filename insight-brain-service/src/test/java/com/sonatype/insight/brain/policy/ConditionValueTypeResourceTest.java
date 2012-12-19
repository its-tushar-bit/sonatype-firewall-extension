/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class ConditionValueTypeResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testGetConditionValueTypes()
        throws Exception
    {
        final Response response = RestAccess.get( getServiceURL() );
        assertResponseStatus( 200, response );
        final Object[] conditionValueTypes = JsonHelpers.fromJson( response.getResponseBody(), Object[].class );
        Assert.assertNotNull( conditionValueTypes );
        Assert.assertTrue( conditionValueTypes.length > 0 );
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + ConditionValueTypeResource.SERVICE_PATH;
    }
}
