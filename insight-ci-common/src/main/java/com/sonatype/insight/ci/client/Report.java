/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.ci.client;

import static com.sonatype.insight.ci.client.DataStore.augmentTable;
import static com.sonatype.insight.ci.client.DataStore.logData;
import static com.sonatype.insight.ci.client.DataStore.parseData;
import static com.sonatype.insight.ci.client.DataStore.streamData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import com.fasterxml.jackson.databind.node.ObjectNode;

public final class Report
{
    public static ReportEntry getEntry( final File reportFile, final String name )
        throws IOException
    {
        final File auditFile = getAuditFile( reportFile, name );
        final File cacheFile = getCacheFile( reportFile, name );

        final long timeLastCached = cacheFile.lastModified();
        if ( timeLastCached > Math.max( auditFile.lastModified(), reportFile.lastModified() ) )
        {
            final InputStream is = new FileInputStream( cacheFile );
            try
            {
                return new ReportEntry( name, timeLastCached, IOUtil.toByteArray( is ) );
            }
            finally
            {
                IOUtil.close( is );
            }
        }

        if ( "data.json".equals( name ) )
        {
            return recalculate( reportFile, "data.json" );
        }

        final ReportEntry entry = extractEntry( reportFile, name );
        if ( entry != null && auditFile.exists() )
        {
            return applyChanges( entry, auditFile, cacheFile );
        }

        return entry;
    }

    public static void augmentEntry( final File reportFile, final String name, final InputStream data,
                                     final String user, final String ip )
        throws IOException
    {
        final File auditFile = getAuditFile( reportFile, name );
        try
        {
            logData( auditFile, user, ip, parseData( IOUtil.toByteArray( data ) ) );
        }
        finally
        {
            IOUtil.close( data );
        }
        FileUtils.cleanDirectory( getCacheDir( reportFile ) );
    }

    public static void migrateChanges( final File oldReportFile, final File newReportFile )
        throws IOException
    {
        FileUtils.copyDirectory( getAuditDir( oldReportFile ), getAuditDir( newReportFile ) );
    }

    public static String toEntryName( final String path )
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

    private static ReportEntry recalculate( final File reportFile, final String name )
        throws IOException
    {
        final ObjectNode security = parseData( getEntry( reportFile, "security.json" ).buf );
        final ObjectNode licenses = parseData( getEntry( reportFile, "licenses.json" ).buf );
        final ObjectNode deps = parseData( getEntry( reportFile, "dependencies.json" ).buf );

        //
        //
        //
        // ...replace with data recalculation...
        final ObjectNode data = parseData( extractEntry( reportFile, name ).buf );
        // ...replace with data recalculation...
        //
        //
        //

        final byte[] buf = streamData( data );

        final File cacheFile = getCacheFile( reportFile, name );
        cacheFile.getAbsoluteFile().getParentFile().mkdirs();
        final OutputStream os = new FileOutputStream( cacheFile );
        try
        {
            IOUtil.copy( buf, os );
        }
        finally
        {
            IOUtil.close( os );
        }
        return new ReportEntry( name, System.currentTimeMillis(), buf );
    }

    private static ReportEntry applyChanges( final ReportEntry entry, final File auditFile, final File cacheFile )
        throws IOException
    {
        cacheFile.getAbsoluteFile().getParentFile().mkdirs();
        final byte[] buf = streamData( augmentTable( parseData( entry.buf ), auditFile ) );
        final OutputStream os = new FileOutputStream( cacheFile );
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

    private static ReportEntry extractEntry( final File reportFile, final String name )
        throws IOException
    {
        final ZipFile archive = new ZipFile( reportFile );
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

    private static File getAuditDir( final File reportFile )
    {
        return new File( reportFile.getParentFile(), "audit" );
    }

    private static File getCacheDir( final File reportFile )
    {
        return new File( reportFile.getParentFile(), "cache" );
    }

    private static File getAuditFile( final File reportFile, final String name )
    {
        return new File( getAuditDir( reportFile ), name );
    }

    private static File getCacheFile( final File reportFile, final String name )
    {
        return new File( getCacheDir( reportFile ), name );
    }
}
