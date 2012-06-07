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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import com.fasterxml.jackson.databind.JsonNode;

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

        final File cacheDir = getCacheDir( reportFile );
        cacheDir.getAbsoluteFile().mkdirs();
        FileUtils.cleanDirectory( cacheDir );
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
        final JsonNode gavDepths = parseData( extractEntry( reportFile, "dependencies.json" ).buf ).get( "gavDepths" );

        final JsonNode security = parseData( getEntry( reportFile, "security.json" ).buf ).get( "aaData" );
        final JsonNode licenses = parseData( getEntry( reportFile, "licenses.json" ).buf ).get( "aaData" );

        /*
         * TODO: extract basic calculation method so it can be shared with the insight-scan-processor
         */

        final int[] securityCounts = new int[10];

        int insecureArtifactCount = 0;
        int copyleftLicenseCount = 0;
        int weakcopyleftLicenseCount = 0;
        int liberalLicenseCount = 0;
        int nonStandardLicenseCount = 0;
        int notProvidedLicenseCount = 0;

        final ArrayList<int[]> securityPunchCard = new ArrayList<int[]>();
        final ArrayList<int[]> licensePunchCard = new ArrayList<int[]>();

        final Set<String> gavs = new HashSet<String>();
        for ( final JsonNode row : security )
        {
            final String status = row.path( "status" ).asText();
            if ( "".equals( status ) || "New".equals( status ) || "Confirmed".equals( status ) )
            {
                final double severity = row.path( "score" ).asDouble();
                final int threatIndex = 10 - (int) Math.floor( severity );

                securityCounts[threatIndex < 0 ? 0 : threatIndex < 10 ? threatIndex : 9]++;

                final String gav = gav( row );
                if ( gavs.add( gav ) )
                {
                    insecureArtifactCount++;
                }

                final int counter = severity < 4 ? 2 : severity < 8 ? 1 : 0;
                for ( final JsonNode level : gavDepths.path( gav ) )
                {
                    final int index = level.asInt() - 1;
                    while ( index >= securityPunchCard.size() )
                    {
                        securityPunchCard.add( new int[3] );
                    }
                    securityPunchCard.get( index )[counter]++;
                }
            }
        }

        for ( final JsonNode row : licenses )
        {
            String threat = row.path( "overriddenLicenseThreat" ).asText();
            if ( "".equals( threat ) )
            {
                threat = row.path( "effectiveLicenseThreat" ).asText();
            }

            final int counter;
            if ( "COPYLEFT".equals( threat ) )
            {
                copyleftLicenseCount++;
                counter = 0;
            }
            else if ( "WEAKCOPYLEFT".equals( threat ) )
            {
                weakcopyleftLicenseCount++;
                counter = 2;
            }
            else if ( "LIBERAL".equals( threat ) )
            {
                liberalLicenseCount++;
                counter = -1;
            }
            else if ( "NON-STANDARD".equals( threat ) )
            {
                nonStandardLicenseCount++;
                counter = 1;
            }
            else
            {
                notProvidedLicenseCount++;
                counter = 1;
            }

            if ( counter >= 0 )
            {
                for ( final JsonNode level : gavDepths.path( gav( row ) ) )
                {
                    final int index = level.asInt() - 1;
                    while ( index >= licensePunchCard.size() )
                    {
                        licensePunchCard.add( new int[3] );
                    }
                    licensePunchCard.get( index )[counter]++;
                }
            }
        }

        final StringBuilder data = new StringBuilder();
        data.append( "{\"securityCounts\":" ).append( Arrays.toString( securityCounts ) );
        data.append( ",\"insecureArtifactCount\":" ).append( insecureArtifactCount );
        data.append( ",\"copyleftLicenseCount\":" ).append( copyleftLicenseCount );
        data.append( ",\"weakcopyleftLicenseCount\":" ).append( weakcopyleftLicenseCount );
        data.append( ",\"liberalLicenseCount\":" ).append( liberalLicenseCount );
        data.append( ",\"nonStandardLicenseCount\":" ).append( nonStandardLicenseCount );
        data.append( ",\"notProvidedLicenseCount\":" ).append( notProvidedLicenseCount );
        data.append( ",\"securityPunchCard\":" ).append( Arrays.deepToString( securityPunchCard.toArray() ) );
        data.append( ",\"licensePunchCard\":" ).append( Arrays.deepToString( licensePunchCard.toArray() ) );
        data.append( '}' );

        final byte[] buf = data.toString().getBytes( "UTF-8" );

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

    private static String gav( final JsonNode row )
    {
        final StringBuilder buf = new StringBuilder();
        buf.append( row.get( "groupId" ).asText() ).append( ':' );
        buf.append( row.get( "artifactId" ).asText() ).append( ':' );
        buf.append( row.get( "version" ).asText() );
        return buf.toString();
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
