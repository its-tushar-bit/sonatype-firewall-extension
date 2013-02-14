/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.scan;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class DefaultScanner
    implements Scanner
{

    private final ScannerConfiguration config;

    private final List<RepositoryItem> items;

    public DefaultScanner( final ScannerConfiguration config )
    {
        this.config = config;
        items = new ArrayList<RepositoryItem>( 128 );
    }

    @Override
    public void add( final RepositoryItem item )
    {
        if ( item != null )
        {
            items.add( item );
        }
    }

    @Override
    public File scan()
        throws IOException
    {
        throw new IOException( "Not yet implemented" );
    }

}
