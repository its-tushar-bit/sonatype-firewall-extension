/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import org.junit.Assert;
import org.junit.Test;

public class InsightConfigTest
{
    @Test
    public void testBaseUrl()
    {
        InsightConfig config = new InsightConfig();
        Assert.assertEquals( "http://localhost:8070/", config.getBaseUrl() );

        config.getHttpConfiguration().setPort( 1234 );
        Assert.assertEquals( "http://localhost:1234/", config.getBaseUrl() );

        config.setBaseUrl( "https://clm.sonatype.com" );
        Assert.assertEquals( "https://clm.sonatype.com/", config.getBaseUrl() );

        config.setBaseUrl( "https://clm.sonatype.com/" );
        Assert.assertEquals( "https://clm.sonatype.com/", config.getBaseUrl() );

        config.setBaseUrl( null );
        Assert.assertEquals( "http://localhost:1234/", config.getBaseUrl() );
    }
}
