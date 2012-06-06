/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.ci.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.util.IOUtil;

public final class Report
{
    public static ReportEntry getEntry( final File report, final String path )
        throws IOException
    {
        final String name = normalizePath( path );

        final File audit = new File( report.getParentFile(), "audit" + File.separatorChar + name );
        final File cache = new File( report.getParentFile(), "cache" + File.separatorChar + name );

        final long timeLastCached = cache.lastModified();
        if ( timeLastCached > Math.max( audit.lastModified(), report.lastModified() ) )
        {
            final InputStream is = new FileInputStream( cache );
            try
            {
                return new ReportEntry( name, timeLastCached, IOUtil.toByteArray( is ) );
            }
            finally
            {
                IOUtil.close( is );
            }
        }

        ReportEntry entry = extract( report, name );
        if ( entry != null && audit.exists() )
        {
            entry = augment( entry, audit, cache );
        }

        return entry;
    }

    private static ReportEntry augment( final ReportEntry entry, final File audit, final File cache )
        throws IOException
    {
        cache.getAbsoluteFile().getParentFile().mkdirs();
        final byte[] buf = DataStore.augmentTable( entry.buf, audit );
        final OutputStream os = new FileOutputStream( cache );
        try
        {
            IOUtil.copy( buf, os );
        }
        finally
        {
            IOUtil.close( os );
        }
        return new ReportEntry( entry.name, System.currentTimeMillis(), buf );
    }

    private static ReportEntry extract( final File report, final String name )
        throws IOException
    {
        final ZipFile archive = new ZipFile( report );
        try
        {
            final ZipEntry entry = archive.getEntry( name );
            if ( entry != null )
            {
                final byte[] buf = IOUtil.toByteArray( archive.getInputStream( entry ) );
                return new ReportEntry( entry.getName(), entry.getTime(), buf );
            }
        }
        finally
        {
            archive.close(); // closes all InputStreams retrieved from this archive
        }
        return null;
    }

    private static String normalizePath( final String path )
    {
        if ( null == path || path.length() == 0 )
        {
            return "index.html";
        }
        boolean seenSlash = true;
        StringBuilder buf = null;
        for ( int i = 0, len = path.length(); i < len; i++ )
        {
            final char c = path.charAt( i );
            final boolean isSlash = '/' == c;
            if ( seenSlash && isSlash )
            {
                if ( buf == null )
                {
                    buf = new StringBuilder( path.subSequence( 0, i ) );
                }
            }
            else if ( buf != null )
            {
                buf.append( c );
            }
            seenSlash = isSlash;
        }
        if ( seenSlash && buf != null )
        {
            buf.append( "index.html" );
        }
        return buf != null ? buf.toString() : path;
    }
}
