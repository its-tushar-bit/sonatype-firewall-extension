/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class HashHelperTest
{
    private final String longHash = "123456789012345678901";

    /**
     * 20 characters as currently specified in HashHelper
     */
    private final String expectedTruncatedHash = "12345678901234567890";

    @Before
    public void preconditions()
    {
        assertTrue( longHash.length() > 20 );
    }

    @Test
    public void testTruncateHash()
    {
        assertEquals( expectedTruncatedHash, HashHelper.truncateHash( longHash ) );
    }
}
