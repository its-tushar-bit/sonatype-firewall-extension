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
        final File auditFile = new File( reportFile.getParentFile(), "audit" + File.separatorChar + name );
        final File cacheFile = new File( reportFile.getParentFile(), "cache" + File.separatorChar + name );

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
        final File auditFile = new File( reportFile.getParentFile(), "audit" + File.separatorChar + name );
        try
        {
            logData( auditFile, user, ip, parseData( IOUtil.toByteArray( data ) ) );
        }
        finally
        {
            IOUtil.close( data );
        }
        summarize( reportFile, "data.json" );
    }

    public static void migrateChanges( final File oldReportFile, final File newReportFile )
        throws IOException
    {
        final File oldAuditDir = new File( oldReportFile.getParentFile(), "audit" );
        final File newAuditDir = new File( newReportFile.getParentFile(), "audit" );

        FileUtils.copyDirectory( oldAuditDir, newAuditDir );
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

    private static ReportEntry summarize( final File reportFile, final String name )
        throws IOException
    {
        // final ObjectNode security = parseData( getEntry( reportFile, "security.json" ).buf );
        // final ObjectNode licenses = parseData( getEntry( reportFile, "licenses.json" ).buf );
        // final ObjectNode deps = parseData( getEntry( reportFile, "dependencies.json" ).buf );
        final ObjectNode data = parseData( getEntry( reportFile, name ).buf );

        final byte[] buf = streamData( data );

        final File cacheFile = new File( reportFile.getParentFile(), "cache" + File.separatorChar + name );
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
}
