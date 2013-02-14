/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.scan;

public class ScannerFactory
{

    public Scanner forConfiguration( final ScannerConfiguration config )
    {
        if ( config == null )
        {
            throw new IllegalArgumentException( "scanner configuration missing" );
        }
        return new DefaultScanner( config );
    }

}
