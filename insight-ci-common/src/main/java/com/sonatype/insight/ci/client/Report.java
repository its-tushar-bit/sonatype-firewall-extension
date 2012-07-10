/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.ci.client;

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

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;

public final class Report
{
    public static ReportEntry getEntry( final File reportFile, final String name )
        throws IOException
    {
        final File cacheFile = getCacheFile( reportFile, name );
        if ( cacheFile.canRead() )
        {
            return new ReportEntry( name, cacheFile.lastModified(), fetch( cacheFile ) );
        }
        return extractEntry( reportFile, name );
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

    public static int[] applyChanges( final File reportFile, final File auditDir )
        throws IOException
    {
        final ContainerNode<?> security = applyChanges( reportFile, "security.json", auditDir );
        final ContainerNode<?> licenses = applyChanges( reportFile, "licenses.json", auditDir );

        if ( isSample( reportFile ) )
        {
            return null; // don't update the badges or summary data
        }

        final JsonNode gavDepths = parseData( extractEntry( reportFile, "dependencies.json" ).buf ).get( "gavDepths" );

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

        int securityAlerts = 0;
        int licenseAlerts = 0;
        int buildAlerts = 0;

        final ArrayList<int[]> securityPunchCard = new ArrayList<int[]>();
        final ArrayList<int[]> licensePunchCard = new ArrayList<int[]>();

        final Set<String> gavs = new HashSet<String>();
        for ( final JsonNode row : security.get( "aaData" ) )
        {
            final String status = row.path( "status" ).asText();
            if ( !"Not Applicable".equals( status ) )
            {
                final double severity = row.path( "score" ).asDouble();
                final int threatIndex = 10 - (int) Math.floor( severity );

                securityCounts[threatIndex < 0 ? 0 : threatIndex < 10 ? threatIndex : 9]++;

                final String gav = gav( row );
                if ( gavs.add( gav ) )
                {
                    insecureArtifactCount++;
                }

                securityAlerts++;
                if ( !"Acknowledged".equals( status ) )
                {
                    buildAlerts++;
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

        for ( final JsonNode row : licenses.get( "aaData" ) )
        {
            String threat = row.path( "overriddenLicenseThreat" ).asText();
            if ( StringUtils.isBlank( threat ) || "null".equals( threat ) )
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
                licenseAlerts++;

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

        cache( getCacheFile( reportFile, "data.json" ), data.toString().getBytes( "UTF-8" ) );

        final StringBuilder badges = new StringBuilder( "[" );
        badges.append( securityAlerts ).append( ',' );
        badges.append( licenseAlerts ).append( ',' );
        badges.append( buildAlerts ).append( ']' );

        cache( getCacheFile( reportFile, "badges.json" ), badges.toString().getBytes( "UTF-8" ) );

        return new int[] { securityAlerts, licenseAlerts, buildAlerts };
    }

    private static ContainerNode<?> applyChanges( final File reportFile, final String name, final File auditDir )
        throws IOException
    {
        ContainerNode<?> table = parseData( extractEntry( reportFile, name ).buf );

        table = Auditing.applyAugmentedData( table, auditDir, name );
        cache( getCacheFile( reportFile, name ), streamData( table ) );

        return table;
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

    static String gav( final JsonNode row )
    {
        final StringBuilder buf = new StringBuilder();
        buf.append( row.get( "groupId" ).asText() ).append( ':' );
        buf.append( row.get( "artifactId" ).asText() ).append( ':' );
        buf.append( row.get( "version" ).asText() );
        return buf.toString();
    }

    private static boolean isSample( final File reportFile )
        throws IOException
    {
        final ZipFile archive = new ZipFile( reportFile );
        try
        {
            return archive.getEntry( "sample.txt" ) != null;
        }
        finally
        {
            archive.close();
        }
    }

    private static File getCacheDir( final File reportFile )
    {
        return new File( reportFile.getParentFile(), "report.cache" );
    }

    private static File getCacheFile( final File reportFile, final String name )
    {
        return new File( getCacheDir( reportFile ), name );
    }

    private static void cache( final File cacheFile, final byte[] buf )
        throws IOException
    {
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
    }

    private static byte[] fetch( final File cacheFile )
        throws IOException
    {
        final InputStream is = new FileInputStream( cacheFile );
        try
        {
            return IOUtil.toByteArray( is );
        }
        finally
        {
            IOUtil.close( is );
        }
    }
}
