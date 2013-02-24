/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.client.utils.UrlUtils;
import com.sonatype.insight.json.store.JsonUtils;
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
        final String applicationPublicId = "ReportResourceTest_AppId";
        createApplication( applicationPublicId );
        final String scanId = "ReportResourceTest_ScanId";

        final String resourcePrefix = getServiceURL( applicationPublicId, scanId );

        final File saasReportFile = getReportResponseFile( applicationPublicId, scanId );
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

                String actual = response.getResponseBody();

                // embedded report processor adds a new licenseCounts property
                actual = actual.replaceAll( ",\\s*\"licenseCounts\" : \\[[^\\]]*\\]", "" );

                assertThat( actual, equalToIgnoringWhiteSpace( expected ) );
            }
            else if ( "badges.json".equals( entry.getName() ) )
            {
                assertThat( JsonUtils.parse( response.getResponseBodyAsBytes(), int[].class ), equalTo( new int[] { 6,
                    6, 6 } ) );
            }
            else if ( "licenses.json".equals( entry.getName() ) )
            {
                String actual = response.getResponseBody();

                // embedded report processor adds a new licenseThreatLevel property
                actual = actual.replaceAll( ",\\s*\"licenseThreatLevel\" : \\d+", "" );

                assertThat( actual,
                            equalToIgnoringWhiteSpace( IOUtil.toString( zipFile.getInputStream( entry ), "UTF-8" ) ) );
            }
            else if ( "licensethreats.json".equals( entry.getName() ) )
            {
                // embedded report processor radically changes structure of this file, so can't compare content
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
        final String applicationPublicId = "ReportResourceTest_AppId";
        createApplication( applicationPublicId );
        final String scanId = "ReportResourceTest_ScanId";

        final String resourcePrefix = getServiceURL( applicationPublicId, scanId );

        final File saasReportFile = getReportResponseFile( applicationPublicId, scanId );
        saasReportFile.delete();

        final URL testReportResultUrl = getClass().getResource( "/ReportResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        final Response response;
        try
        {
            response = RestAccess.get( resourcePrefix + "/printReport?projectName=Test%20Project&buildNumber=8" );
            assertResponseStatus( 200, response );
            assertThat( response.getHeader( "Content-Disposition" ),
                        stringContainsInOrder( Arrays.asList( "attachment; filename=", "Test%20Project-8-", ".pdf" ) ) );
        }
        finally
        {
            Pdf.destroy();
        }

        // validate content type and check the actual content is really a PDF
        assertThat( response.getContentType(), equalTo( "application/pdf" ) );
        final Collection<?> mimeTypes = new MagicMimeMimeDetector().getMimeTypes( response.getResponseBodyAsStream() );
        assertThat( mimeTypes, contains( (Object) new MimeType( "application/pdf" ) ) );
    }

    @Test
    public void testArtifactDetails()
        throws Exception
    {
        final String applicationPublicId = "ReportResourceTest_AppId";
        createApplication( applicationPublicId );
        final String scanId = "ReportResourceTest_ScanId";

        final String resourcePrefix = getServiceURL( applicationPublicId, scanId );
        final String query = "?groupId=org.springframework&artifactId=spring-core&version=2.5.6";
        final Response response = RestAccess.get( resourcePrefix + "/artifactDetails" + query );
        assertResponseStatus( 200, response );

        assertThat( response.getResponseBody(), stringContainsInOrder( Arrays.asList( "\"groupId\"",
                                                                                      "\"org.springframework\"",
                                                                                      "\"artifactId\"",
                                                                                      "\"spring-core\"", "\"version\"",
                                                                                      "\"2.5.6\"" ) ) );
    }

    @Test
    public void testAugmentDataAndAuditLog()
        throws Exception
    {
        final String applicationPublicId = "ReportResourceTest_AppId";
        createApplication( applicationPublicId );
        final String scanId = "ReportResourceTest_ScanId";

        final String resourcePrefix = getServiceURL( applicationPublicId, scanId );

        final File saasReportFile = getReportResponseFile( applicationPublicId, scanId );
        saasReportFile.delete();

        final URL testReportResultUrl = getClass().getResource( "/ReportResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        final String query = "security.json?user=test&where=ReportResourceTest";

        // attempt a bad edit (no augmented data)
        Response response = RestAccess.post( resourcePrefix + "/augmentData/" + query, "" );
        assertResponseStatus( 400, response ); // bad request; no changes

        // verify nothing has changed
        response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), not( containsString( "\"state\" : \"accepted\"" ) ) );

        // edit the state
        final String edit = "{ \"hash\" : \"964cd74171f427720480\", \"state\" : \"accepted\" }";
        response = RestAccess.post( resourcePrefix + "/augmentData/" + query, edit );
        assertResponseStatus( 200, response );

        // verify the state has changed
        response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), containsString( "\"state\" : \"accepted\"" ) );

        // check the audit log reflects this change
        response =
            RestAccess.get( resourcePrefix + "/auditLog/security.json?key="
                + UrlUtils.encodeUrlComponent( "{\"hash\":\"964cd74171f427720480\"}" ) );
        assertResponseStatus( 200, response );

        final String feed =
            "{ \"aaData\" : [ { \"hash\" : \"964cd74171f427720480\", \"state\" : \"accepted\", \"user\" : \"test\", \"ip\" : \"127.0.0.1\", \"where\" : \"ReportResourceTest\", \"filename\" : \"security.json\" } ] }";

        assertThat( response.getResponseBody().replaceFirst( "\"time\" : [0-9]+,", "" ),
                    equalToIgnoringWhiteSpace( feed ) );
    }

    @Test
    public void testRefreshOnlyOnChange()
        throws Exception
    {
        final String applicationPublicId = "ReportResourceTest_AppId";
        Application application = createApplication( applicationPublicId );
        String appId = application.getId();
        final String scanId = "ReportResourceTest_ScanId";

        final String resourcePrefix = getServiceURL( applicationPublicId, scanId );

        final File saasReportFile = getReportResponseFile( applicationPublicId, scanId );
        saasReportFile.delete();

        final URL testReportResultUrl = getClass().getResource( "/ReportResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        final String query = "security.json?user=test&where=ReportResourceTest";

        // verify nothing has changed
        Response response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), not( containsString( "\"state\" : \"accepted\"" ) ) );

        // edit the state
        final String edit = "{ \"hash\" : \"964cd74171f427720480\", \"state\" : \"accepted\" }";
        response = RestAccess.post( resourcePrefix + "/augmentData/" + query, edit );
        assertResponseStatus( 200, response );

        // check the audit log reflects this change
        response =
            RestAccess.get( resourcePrefix + "/auditLog/security.json?key="
                + UrlUtils.encodeUrlComponent( "{\"hash\":\"964cd74171f427720480\"}" ) );
        assertResponseStatus( 200, response );

        final String feed =
            "{ \"aaData\" : [ { \"hash\" : \"964cd74171f427720480\", \"state\" : \"accepted\", \"user\" : \"test\", \"ip\" : \"127.0.0.1\", \"where\" : \"ReportResourceTest\", \"filename\" : \"security.json\" } ] }";

        assertThat( response.getResponseBody().replaceFirst( "\"time\" : [0-9]+,", "" ),
                    equalToIgnoringWhiteSpace( feed ) );

        // force the internal modification count to make it look like we're already up-to-date
        int oldModCount = ReportResource.MODIFICATION_COUNTS.put( appId + '-' + scanId, 888 );

        // verify nothing has changed
        response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), not( containsString( "\"state\" : \"accepted\"" ) ) );

        // put back the accurate modification count, which should lead to a refresh
        ReportResource.MODIFICATION_COUNTS.put( appId + '-' + scanId, oldModCount );

        // verify the state has changed
        response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), containsString( "\"state\" : \"accepted\"" ) );
    }

    @Test
    public void testCanAuditNonReportData()
        throws Exception
    {
        final String applicationPublicId = "ReportResourceTest_AppId";
        createApplication( applicationPublicId );
        final String scanId = "ReportResourceTest_ScanId";

        final String resourcePrefix = getServiceURL( applicationPublicId, scanId );

        final File saasReportFile = getReportResponseFile( applicationPublicId, scanId );
        saasReportFile.delete();

        final URL testReportResultUrl = getClass().getResource( "/ReportResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        final String query = "extra.json?user=test&where=ReportResourceTest";

        // audit non-report data
        final String extra = "{ \"policy\" : \"TEST\", \"result\" : \"OK\" }";
        Response response = RestAccess.post( resourcePrefix + "/augmentData/" + query, extra );
        assertResponseStatus( 200, response );

        // verify can still access report
        response = RestAccess.get( resourcePrefix + "/embedReport/security.json" );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), not( containsString( "\"state\" : \"accepted\"" ) ) );

        // check the audit log reflects this change
        response = RestAccess.get( resourcePrefix + "/auditLog/extra.json" );
        assertResponseStatus( 200, response );

        final String feed =
            "{ \"aaData\" : [ { \"policy\" : \"TEST\", \"result\" : \"OK\", \"user\" : \"test\", \"ip\" : \"127.0.0.1\", \"where\" : \"ReportResourceTest\", \"filename\" : \"extra.json\" } ] }";

        assertThat( response.getResponseBody().replaceFirst( "\"time\" : [0-9]+,", "" ),
                    equalToIgnoringWhiteSpace( feed ) );
    }

    private String getServiceURL( final String appId, final String scanId )
    {
        return getRestBaseUrl()
            + ReportResource.SERVICE_PATH.replace( "{applicationPublicId}", appId ).replace( "{scanId}", scanId );
    }
}
