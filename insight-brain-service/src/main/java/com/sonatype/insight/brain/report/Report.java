/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.ws.rs.core.Response.ResponseBuilder;

import org.codehaus.plexus.util.IOUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.json.store.JsonUtils;

public final class Report
{
    private static enum ReportType
    {
        FULL, SAMPLE, ERROR
    }

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

    public static void putEntry( final File reportFile, final String name, final byte[] buf )
        throws IOException
    {
        cache( getCacheFile( reportFile, name ), buf );
    }

    public static void putEntry( final File reportFile, final String name, final String text )
        throws IOException
    {
        putEntry( reportFile, name, text.getBytes( "UTF-8" ) );
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

    private static void embedApplicationPublicId( String applicationId, File reportFile )
        throws IOException
    {
        Application application = new ApplicationDAO().getByIdNotNull( applicationId );
        String filename = "index.html";
        ReportEntry reportEntry = extractEntry( reportFile, filename );
        String indexHtmlContent = new String( reportEntry.buf, Charset.forName( "UTF-8" ) );
        indexHtmlContent =
            indexHtmlContent.replace( "applicationId = ''", "applicationId = '" + application.getPublicId() + "'" );
        cache( getCacheFile( reportFile, filename ), indexHtmlContent.getBytes( "UTF-8" ) );
    }

    public static int[] applyChanges( final String appId, final File reportFile, final File auditDir )
        throws IOException
    {
        final ReportType reportType = getType( reportFile );

        if ( ReportType.ERROR.equals( reportType ) )
        {
            return new int[] { -1, -1 };
        }

        embedApplicationPublicId( appId, reportFile );

        final ReportEntry policyReportEntry = getEntry( reportFile, "policythreats.json" );

        final ContainerNode<?> security = applyChanges( reportFile, "security.json", auditDir );
        final ContainerNode<?> licenses = applyChanges( reportFile, "licenses.json", auditDir );
        final ContainerNode<?> partialMatched = applyChanges( reportFile, "partialmatched.json", auditDir );

        for ( final String name : JsonUtils.fileStore( auditDir ).list() )
        {
            if ( !"security.json".equals( name ) && !"licenses.json".equals( name )
                && !"partialmatched.json".equals( name ) )
            {
                applyChanges( reportFile, name, auditDir );
            }
        }

        if ( ReportType.SAMPLE.equals( reportType ) )
        {
            return JsonUtils.parse( extractEntry( reportFile, "badges.json" ).buf, int[].class );
        }

        final JsonNode gavDepths =
            JsonUtils.parse( extractEntry( reportFile, "dependencies.json" ).buf ).get( "gavDepths" );

        /*
         * TODO: extract basic calculation method so it can be shared with the insight-scan-processor
         */

        final int[] policyCounts = new int[11];
        final int[] securityCounts = new int[10];
        final int[] licenseCounts = new int[11];

        int policyComponentCount = 0;
        int insecureArtifactCount = 0;

        int securityAlerts = 0;
        int licenseAlerts = 0;
        int buildAlerts = 0;

        final ArrayList<int[]> securityPunchCard = new ArrayList<int[]>();
        final ArrayList<int[]> licensePunchCard = new ArrayList<int[]>();

        if ( policyReportEntry != null )
        {
            for ( final JsonNode row : JsonUtils.parse( policyReportEntry.buf ).get( "aaData" ) )
            {
                final int level = row.path( "policyThreatLevel" ).asInt();
                policyCounts[level < 0 ? 0 : level < 11 ? level : 10]++;
                if ( level >= 2 )
                {
                    policyComponentCount++;
                }
            }
        }

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

        ComponentDAO componentDAO = new ComponentDAO();
        for ( JsonNode licenseJsonNode : licenses.get( "aaData" ) )
        {
            final Component component = componentDAO.getComponent( appId, licenseJsonNode );
            ObjectNode licenseNode = (ObjectNode) licenseJsonNode;
            Integer threatLevel = component.getLicenseThreatLevel();
            licenseNode.put( "effectiveLicenseThreat", threatLevel );

            if ( threatLevel != null )
            {
                threatLevel = Math.min( 10, Math.max( 0, threatLevel ) );
                licenseCounts[threatLevel]++;
                if ( threatLevel > 0 )
                {
                    // Punch card expects 0 to be the highest threat with 2 being the lowest
                    final int threatDepth = threatLevel < 4 ? 2 : threatLevel < 8 ? 1 : 0;
                    for ( final JsonNode level : gavDepths.path( component.getGAV() ) )
                    {
                        final int index = level.asInt() - 1;
                        while ( index >= licensePunchCard.size() )
                        {
                            licensePunchCard.add( new int[3] );
                        }
                        licensePunchCard.get( index )[threatDepth]++;
                    }
                    licenseAlerts++;
                }
            }
        }

        for ( JsonNode licenseJsonNode : partialMatched.get( "aaData" ) )
        {
            final ArrayNode matchedComponentNodes = (ArrayNode) licenseJsonNode.get( "matchDetails" );
            for ( JsonNode matchedComponentJsonNode : matchedComponentNodes )
            {
                final Component matchedComponent = new ComponentDAO().getComponent( appId, matchedComponentJsonNode );
                ObjectNode matchedComponentNode = (ObjectNode) matchedComponentJsonNode;
                matchedComponentNode.put( "effectiveLicenseThreat", matchedComponent.getLicenseThreatLevel() );
            }
        }

        cache( getCacheFile( reportFile, "licenses.json" ), JsonUtils.generate( licenses ) );
        cache( getCacheFile( reportFile, "partialmatched.json" ), JsonUtils.generate( partialMatched ) );
        writeLicenseThreatsToReportFile( appId, reportFile );

        final ObjectNode data = JsonUtils.parse( extractEntry( reportFile, "data.json" ).buf );
        fill( data.putArray( "policyCounts" ), policyCounts );
        data.put( "policyComponentCount", policyComponentCount );
        fill( data.putArray( "securityCounts" ), securityCounts );
        data.put( "insecureArtifactCount", insecureArtifactCount );
        fill( data.putArray( "effectiveLicenseCounts" ), licenseCounts );
        fill( data.putArray( "securityPunchCard" ), securityPunchCard );
        fill( data.putArray( "licensePunchCard" ), licensePunchCard );
        filterKeyFindings( data, security );

        cache( getCacheFile( reportFile, "data.json" ), JsonUtils.generate( data ) );

        final StringBuilder badges = new StringBuilder( "[" );
        badges.append( securityAlerts ).append( ',' );
        badges.append( licenseAlerts ).append( ',' );
        badges.append( buildAlerts ).append( ']' );

        cache( getCacheFile( reportFile, "badges.json" ), badges.toString().getBytes( "UTF-8" ) );

        return new int[] { securityAlerts, licenseAlerts, buildAlerts };
    }

    private static void writeLicenseThreatsToReportFile( final String appId, final File reportFile )
        throws IOException
    {
        MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode licenseThreatsJson = mapper.createObjectNode();
        ObjectNode licenseTable = mapper.createObjectNode();
        for ( MultiLicense multiLicense : multiLicenseDAO.getAll() )
        {
            Integer threatLevel =
                multiLicenseDAO.getLicenseThreatLevelByApplicationIdAndMultiLicenseId( appId, multiLicense.getId() );
            licenseTable.put( multiLicense.getShortDisplayName(), threatLevel );
        }
        licenseThreatsJson.put( "aaData", licenseTable );
        cache( getCacheFile( reportFile, "licensethreats.json" ), JsonUtils.generate( licenseThreatsJson ) );
    }

    /**
     * @deprecated As of INSIGHT-4409, key findings are now longer included in the reports.
     */
    private static void filterKeyFindings( final ObjectNode data, final ContainerNode<?> security )
    {
        final Set<String> textSet = new HashSet<String>();

        ArrayNode sourceFindings = (ArrayNode) data.get( "keyFindings" );
        if ( sourceFindings == null )
        {
            sourceFindings = data.putArray( "keyFindings" );
        }

        final Iterator<JsonNode> sourceIter = sourceFindings.elements();

        // simply iterate through the list, and dump any items that are duplicate key findings, or that are marked as
        // 'Not Applicable'
        while ( sourceIter.hasNext() )
        {
            final JsonNode sourceFinding = sourceIter.next();

            final String text = asText( sourceFinding.get( "text" ) );

            // if we already have this keyFinding to be shown, no need to show others
            if ( textSet.contains( text ) )
            {
                sourceIter.remove();
            }
            else
            {
                final JsonNode svNode = sourceFinding.get( "sv" );

                // if svNode is null we are dealing with a freemium report, so simply add the key finding text
                if ( svNode == null )
                {
                    textSet.add( text );
                }
                else
                {
                    boolean foundMatch = false;

                    // need to compare against each row in the security data to find a match, and decide if the match is
                    // applicable
                    for ( final JsonNode row : security.get( "aaData" ) )
                    {
                        final Iterator<String> iter = svNode.fieldNames();

                        boolean recordMatch = true;

                        // simple agnostic means to check the coordinates
                        while ( iter.hasNext() )
                        {
                            final String key = iter.next();

                            final String sourceVal = asText( svNode.get( key ) );
                            final String targetVal = asText( row.get( key ) );

                            if ( !( sourceVal == null && targetVal == null || sourceVal != null
                                && sourceVal.equals( targetVal ) ) )
                            {
                                recordMatch = false;
                                break;
                            }
                        }

                        foundMatch = recordMatch;

                        // if we found a match, check the status, if not applicable, junk it
                        if ( recordMatch && "Not Applicable".equals( asText( row.get( "status" ) ) ) )
                        {
                            sourceIter.remove();
                            break;
                        }
                        else if ( recordMatch )
                        {
                            textSet.add( text );
                            break;
                        }
                    }

                    // This is a case that shouldn't be hit besides in dev, if no match
                    // found in the security table, dump it
                    if ( !foundMatch )
                    {
                        sourceIter.remove();
                    }
                }
            }
        }
    }

    private static String asText( final JsonNode node )
    {
        String text;
        if ( node != null )
        {
            text = node.asText();
            // NOTE: asText() turns null into the string "null", cf.
            // https://github.com/FasterXML/jackson-databind/issues/25
            if ( "null".equals( text ) )
            {
                text = null;
            }
        }
        else
        {
            text = null;
        }
        return text;
    }

    public static void printPdf( final File reportFile, final String projectName, final int buildNumber,
                                 final ResponseBuilder response )
        throws IOException
    {
        Pdf.generate( reportFile, getCacheDir( reportFile ), ReportType.SAMPLE.equals( getType( reportFile ) ),
                      projectName, buildNumber, response );
    }

    public static void deletePdf( final File reportFile )
    {
        Pdf.delete( reportFile );
    }

    private static ContainerNode<?> applyChanges( final File reportFile, final String name, final File auditDir )
        throws IOException
    {
        ContainerNode<?> table = null;
        final ReportEntry entry = extractEntry( reportFile, name );
        if ( entry != null )
        {
            table = JsonUtils.fileStore( auditDir ).augment( JsonUtils.parse( entry.buf ), name );
            cache( getCacheFile( reportFile, name ), JsonUtils.generate( table ) );
        }
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

    private static ReportType getType( final File reportFile )
        throws IOException
    {
        final ZipFile archive = new ZipFile( reportFile );
        try
        {
            if ( archive.getEntry( "sample.txt" ) != null )
            {
                return ReportType.SAMPLE;
            }
            if ( archive.getEntry( "security.json" ) == null && archive.getEntry( "licenses.json" ) == null
                && archive.getEntry( "badges.json" ) == null )
            {
                return ReportType.ERROR;
            }
            return ReportType.FULL;
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

    private static void fill( final ArrayNode node, final int[] data )
    {
        for ( final int d : data )
        {
            node.add( d );
        }
    }

    private static void fill( final ArrayNode node, final List<int[]> datas )
    {
        for ( final int[] data : datas )
        {
            fill( node.addArray(), data );
        }
    }
}
