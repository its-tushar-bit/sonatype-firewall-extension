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

    private String repositoryId;

    private String repositoryName;

    private String repositoryFormat;

    public ScannerConfiguration()
    {
        workDir = getDefaultWorkDir();
        scanOptions = new Properties();
    }

    private static File getDefaultWorkDir()
    {
        return new File( System.getProperty( "java.io.tmpdir", "" ) ).getAbsoluteFile();
    }

    public File getWorkDir()
    {
        return workDir;
    }

    public ScannerConfiguration setWorkDir( final File workDir )
    {
        this.workDir = ( workDir != null ) ? workDir : getDefaultWorkDir();
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

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public String getRepositoryName()
    {
        return repositoryName;
    }

    public String getRepositoryFormat()
    {
        return repositoryFormat;
    }

    public ScannerConfiguration setRepository( String id, String format, String name )
    {
        repositoryId = id;
        repositoryFormat = format;
        repositoryName = name;
        return this;
    }

}
