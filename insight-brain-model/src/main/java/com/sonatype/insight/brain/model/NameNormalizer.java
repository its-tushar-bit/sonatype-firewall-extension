/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.Locale;

public class NameNormalizer
{
    private NameNormalizer()
    {
    }

    public static String normalize( String name )
    {
        if ( name != null )
        {
            // The name is whitespace and case insensitive
            return name.replaceAll( "\\s", "" ).toLowerCase( Locale.ENGLISH );
        }
        else
        {
            return null;
        }
    }
}
