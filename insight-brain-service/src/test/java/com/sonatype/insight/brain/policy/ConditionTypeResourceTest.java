/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.policy.ConditionTypeResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class ConditionTypeResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testGetConditionTypes()
        throws Exception
    {
        final Response response = RestAccess.get( getServiceURL() );
        assertResponseStatus( 200, response );
        final Object[] conditionTypes = JsonHelpers.fromJson( response.getResponseBody(), Object[].class );
        Assert.assertNotNull( conditionTypes );
        Assert.assertTrue( conditionTypes.length > 0 );
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + ConditionTypeResource.SERVICE_PATH;
    }
}
