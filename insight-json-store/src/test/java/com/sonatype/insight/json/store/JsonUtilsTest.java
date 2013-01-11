/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

public class JsonUtilsTest
{
    @Test
    public void testIsNull()
        throws IOException
    {
        Assert.assertTrue( JsonUtils.isNull( null ) );
        Assert.assertTrue( JsonUtils.isNull( NullNode.getInstance() ) );

        JsonNode jsonNode = JsonUtils.parse( "{\"a\":\"text value\",\"b\":null}" );
        Assert.assertNotNull( jsonNode );
        Assert.assertFalse( JsonUtils.isNull( jsonNode ) );
        Assert.assertFalse( JsonUtils.isNull( jsonNode.get( "a" ) ) );
        Assert.assertTrue( JsonUtils.isNull( jsonNode.get( "b" ) ) );
        Assert.assertTrue( JsonUtils.isNull( jsonNode.get( "c" ) ) );
    }
}
