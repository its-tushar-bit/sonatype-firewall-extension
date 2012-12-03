/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.not;

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

                assertThat( response.getResponseBody(), equalToIgnoringWhiteSpace( expected ) );
            }
            else if ( "badges.json".equals( entry.getName() ) )
            {
                assertThat( DataStore.parseData( response.getResponseBodyAsBytes(), int[].class ), equalTo( new int[] {
                    6, 6, 6 } ) );
            }
            else if ( contentType.startsWith( "text" ) || contentType.endsWith( "json" ) )
            {
                assertThat( response.getResponseBody(),
                            equalToIgnoringWhiteSpace( IOUtil.toString( zipFile.getInputStream( entry ), "UTF-8" ) ) );
            }
            else
            {
                assertThat( IOUtil.toByteArray( response.getResponseBodyAsStream() ),
                            equalTo( IOUtil.toByteArray( zipFile.getInputStream( entry ) ) ) );
            }
        }

        zipFile.close();
    }

    @Test
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
        assertThat( response.getContentType(), equalTo( "application/pdf" ) );
        final Collection<?> mimeTypes = new MagicMimeMimeDetector().getMimeTypes( response.getResponseBodyAsStream() );
        assertThat( mimeTypes, contains( (Object) new MimeType( "application/pdf" ) ) );
    }

    @Test
    public void testArtifactDetails()
        throws Exception
    {
        final String appId = "ReportResourceTest_AppId";
        final String scanId = "ReportResourceTest_ScanId";

        final String resourcePrefix =
            RestAccess.BASE_URL + ReportResource.SERVICE_PATH.replace( "{appId}", appId ).replace( "{scanId}", scanId );

        final String query = "?groupId=org.springframework&artifactId=spring-core&version=2.5.6";
        final Response response = RestAccess.get( resourcePrefix + "/artifactDetails" + query );
        assertResponseStatus( 200, response );

        assertThat( response.getResponseBody(), equalToIgnoringWhiteSpace( scanId + query ) );
    }

    @Test
    public void testAugmentData()
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

        final String query = "security.json?user=test&where=ReportResourceTest";
        Response response = RestAccess.post( resourcePrefix + "/augmentData/" + query, "" );
        assertResponseStatus( 400, response ); // bad request; no changes

        response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );

        assertThat( response.getResponseBody(), not( containsString( "\"state\" : \"accepted\"" ) ) );

        final String edit = "{ \"hash\" : \"964cd74171f427720480\", \"state\" : \"accepted\" }";

        response = RestAccess.post( resourcePrefix + "/augmentData/" + query, edit );
        assertResponseStatus( 200, response );

        response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );

        assertThat( response.getResponseBody(), containsString( "\"state\" : \"accepted\"" ) );
    }
}
