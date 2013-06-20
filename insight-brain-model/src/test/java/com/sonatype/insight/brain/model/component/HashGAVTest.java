/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HashGAVTest
{
    @Test
    public void testTruncatedHash()
    {
        String hash = "123456789012345678901";
        assertTrue( hash.length() > 20 );
        String truncatedHash = hash.substring( 0, 20 );

        HashGAV hashGAV =
            new HashGAV( hash, null /* groupId */, null /* artifactId */, null /* version */, null /* extension */,
                         null /* classifier */);
        assertEquals( truncatedHash, hashGAV.getHash() );

        hashGAV = new HashGAV();
        hashGAV.setHash( hash );
        assertEquals( truncatedHash, hashGAV.getHash() );
    }
}
