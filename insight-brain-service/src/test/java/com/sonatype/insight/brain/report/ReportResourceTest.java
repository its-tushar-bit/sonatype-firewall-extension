/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

import java.io.File;
import java.net.URL;
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

public class ReportResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testEmbedReport()
        throws Exception
    {
        String appId = "ReportResourceTest_AppId";
        String scanId = "ReportResourceTest_ScanId";

        String resourcePrefix =
            RestAccess.BASE_URL + ReportResource.SERVICE_PATH.replace( "{appId}", appId ).replace( "{scanId}", scanId );

        File saasReportFile = getReportResponseFile( appId, scanId );
        saasReportFile.delete();

        URL testReportResultUrl = getClass().getResource( "/ReportResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        ZipFile zipFile = new ZipFile( saasReportFile );
        Enumeration<? extends ZipEntry> e = zipFile.entries();
        while ( e.hasMoreElements() )
        {
            ZipEntry entry = e.nextElement();
            Response response = RestAccess.get( resourcePrefix + "/embedReport/" + entry.getName() );
            String contentType = response.getContentType();
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
}
