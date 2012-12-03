/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.isIn;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.data.DataStore;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.detector.MagicMimeMimeDetector;

public class ReportResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testEmbedReport()
        throws Exception
    {
        final String appId = "ReportResourceTest_AppId";
        final String scanId = "ReportResourceTest_ScanId";

        final String resourcePrefix =
            RestAccess.BASE_URL + ReportResource.SERVICE_PATH.replace( "{appId}", appId ).replace( "{scanId}", scanId );

        final File saasReportFile = getReportResponseFile( appId, scanId );
        saasReportFile.delete();

        final URL testReportResultUrl = getClass().getResource( "/ReportResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        final ZipFile zipFile = new ZipFile( saasReportFile );
        final Enumeration<? extends ZipEntry> e = zipFile.entries();
        while ( e.hasMoreElements() )
        {
            final ZipEntry entry = e.nextElement();
            final Response response = RestAccess.get( resourcePrefix + "/embedReport/" + entry.getName() );
            final String contentType = response.getContentType();
            assertResponseStatus( 200, response );

            if ( "data.json".equals( entry.getName() ) )
            {
                String expected = IOUtil.toString( zipFile.getInputStream( entry ), "UTF-8" );

                // embedded report processor removes the duplicate key findings
                expected = expected.replaceFirst( "(?s)keyFindings.*text\"", "keyFindings\" : [ { \"text\"" );
                // embedded report processor removes trailing zeros from arrays
                expected = expected.replaceAll( ", \\[ 0, 0, 0 \\] \\]", " ]" );

                assertThat( expected, equalToIgnoringWhiteSpace( response.getResponseBody() ) );
            }
            else if ( "badges.json".equals( entry.getName() ) )
            {
                assertThat( new int[] { 6, 6, 6 },
                            equalTo( DataStore.parseData( response.getResponseBodyAsBytes(), int[].class ) ) );
            }
            else if ( contentType.startsWith( "text" ) || contentType.endsWith( "json" ) )
            {
                assertThat( IOUtil.toString( zipFile.getInputStream( entry ), "UTF-8" ),
                            equalToIgnoringWhiteSpace( response.getResponseBody() ) );
            }
            else
            {
                assertThat( IOUtil.toByteArray( zipFile.getInputStream( entry ) ),
                            equalTo( IOUtil.toByteArray( response.getResponseBodyAsStream() ) ) );
            }
        }

        zipFile.close();
    }

    @Test
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public void testPrintReport()
        throws Exception
    {
        final String appId = "ReportResourceTest_AppId";
        final String scanId = "ReportResourceTest_ScanId";

        final String resourcePrefix =
            RestAccess.BASE_URL + ReportResource.SERVICE_PATH.replace( "{appId}", appId ).replace( "{scanId}", scanId );

        final File saasReportFile = getReportResponseFile( appId, scanId );
        saasReportFile.delete();

        final URL testReportResultUrl = getClass().getResource( "/ReportResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        final Response response = RestAccess.get( resourcePrefix + "/printReport" );
        assertResponseStatus( 200, response );

        // validate content type and check the actual content is really a PDF
        assertThat( "application/pdf", equalTo( response.getContentType() ) );
        final Collection mimeTypes = new MagicMimeMimeDetector().getMimeTypes( response.getResponseBodyAsStream() );
        assertThat( new MimeType( "application/pdf" ), isIn( mimeTypes ) );
    }

    @Test
    public void testArtifactDetails()
        throws Exception
    {
    }

    @Test
    public void testAugmentData()
        throws Exception
    {
    }

    @Test
    public void testAuditLog()
        throws Exception
    {
    }
}
