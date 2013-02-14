/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.scan;

import java.io.File;
import java.util.Properties;

public class ScannerConfiguration
{

    private File workDir;

    private Properties scanOptions;

    public ScannerConfiguration()
    {
        workDir = new File( System.getProperty( "java.io.tmpdir", "" ) ).getAbsoluteFile();
        scanOptions = new Properties();
    }

    public File getWorkDir()
    {
        return workDir;
    }

    public ScannerConfiguration setWorkDir( final File workDir )
    {
        this.workDir = workDir;
        return this;
    }

    public Properties getScanOptions()
    {
        return scanOptions;
    }

    public ScannerConfiguration setScanOptions( final Properties scanOptions )
    {
        this.scanOptions.clear();
        if ( scanOptions != null )
        {
            this.scanOptions.putAll( scanOptions );
        }
        return this;
    }

    public ScannerConfiguration setScanOption( final String key, final String value )
    {
        if ( value == null )
        {
            scanOptions.remove( key );
        }
        else
        {
            scanOptions.setProperty( key, value );
        }
        return this;
    }

}
