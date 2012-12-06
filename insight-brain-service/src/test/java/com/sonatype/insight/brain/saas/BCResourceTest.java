/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

import java.io.File;
import java.net.URL;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;

public class BCResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testValidate()
        throws Exception
    {
        final String appId = "BCResourceTest_AppId";

        Response response = RestAccess.get( getServiceURL() + "/validate/" + appId );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), equalTo( "OK" ) );

        invalidateAppId( appId, "Expired" );

        // validate service always returns 200, the actual result is in the response body
        response = RestAccess.get( getServiceURL() + "/validate/" + appId );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), equalTo( "Expired" ) );
    }

    @Test
    public void testScan()
        throws Exception
    {
        final String appId = "BCResourceTest_AppId";
        final File saasScanFile = getScanResponseFile( appId );
        saasScanFile.delete();

        final URL testScanResultUrl = getClass().getResource( "/BCResourceTest/scan.json" );
        FileUtils.copyFile( new File( testScanResultUrl.getFile() ), saasScanFile );

        final Response response = RestAccess.put( getServiceURL() + "/scan/" + appId, "" );

        assertResponseStatus( 200, response );

        assertThat( response.getResponseBody(), equalToIgnoringWhiteSpace( FileUtils.fileRead( saasScanFile, "UTF-8" ) ) );
    }

    @Test
    public void testReport()
        throws Exception
    {
        final String appId = "BCResourceTest_AppId";
        final String scanId = "BCResourceTest_ScanId";
        final File saasReportFile = getReportResponseFile( appId, scanId );
        saasReportFile.delete();

        final URL testReportResultUrl = getClass().getResource( "/BCResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        final Response response = RestAccess.get( getServiceURL() + "/report/" + appId + "?scanId=" + scanId );

        assertResponseStatus( 200, response );

        assertThat( IOUtil.toByteArray( response.getResponseBodyAsStream() ),
                    equalTo( IOUtil.toByteArray( testReportResultUrl.openStream() ) ) );
    }

    @Test
    public void testArtifact()
        throws Exception
    {
        final String scanId = "BCResourceTest_ScanId";

        final String query = scanId + "?groupId=org.springframework&artifactId=spring-core&version=2.5.6";
        Response response = RestAccess.get( getServiceURL() + "/artifact/" + query );
        assertResponseStatus( 307, response );

        response = RestAccess.get( response.getHeader( "Location" ) );
        assertResponseStatus( 200, response );

        assertThat( response.getResponseBody(), equalTo( query ) );
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + BCResource.SERVICE_PATH;
    }
}
