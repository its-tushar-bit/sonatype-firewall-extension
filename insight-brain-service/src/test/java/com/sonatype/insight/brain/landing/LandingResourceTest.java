/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.test.RestAccess;

public class LandingResourceTest
    extends AbstractResourceTest
{

    @Test
    public void testHome()
        throws Exception
    {
        Response response = RestAccess.get( getRestBaseUrl() );
        assertResponseStatus( 303, response );
        assertEquals( getRestBaseUrl() + InsightBrainService.APPLICATION_ASSET_PATH.substring( 1 ) + "index.html",
                      response.getHeader( "Location" ) );
    }

}
