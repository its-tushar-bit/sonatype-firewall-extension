/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

public class NameValidator
{
    public static void validate( String name )
    {
        if ( name == null || name.trim().isEmpty() )
        {
            throw new InvalidNameException( "Name is required." );
        }
        for ( char c : name.toCharArray() )
        {
            if ( !Character.isLetterOrDigit( c ) && c != '-' && c != ' ' )
            {
                throw new InvalidNameException( "Name must be alpha numeric." );
            }
        }
        if ( name.startsWith( " " ) || name.endsWith( " " ) || name.indexOf( "  " ) > 0 )
        {
            throw new InvalidNameException(
                                            "Name must not have leading or trailing spaces, or have two spaces in a row." );
        }
    }
}
