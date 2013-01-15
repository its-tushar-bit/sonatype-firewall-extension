/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class ConditionValueTypeResourceTest
    extends AbstractResourceTest
{
    private ApplicationDAO applicationDAO = new ApplicationDAO();

    @Test
    public void testGetConditionValueTypes()
        throws Exception
    {
        // Create an application
        String appPublicId = "ConditionValueTypeResourceTest_AppId";
        Application application = new Application();
        application.setPublicId( appPublicId );
        applicationDAO.insert( application );
        
        final Response response = RestAccess.get( getServiceURL( appPublicId ) );
        assertResponseStatus( 200, response );
        final Object[] conditionValueTypes = JsonHelpers.fromJson( response.getResponseBody(), Object[].class );
        Assert.assertNotNull( conditionValueTypes );
        Assert.assertTrue( conditionValueTypes.length > 0 );
    }

    private String getServiceURL( String appId )
    {
        return getRestBaseUrl() + ConditionValueTypeResource.SERVICE_PATH.replace( "{applicationPublicId}", appId );
    }
}
