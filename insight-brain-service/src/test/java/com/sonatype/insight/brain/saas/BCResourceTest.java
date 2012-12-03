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
        String appId = "BCResourceTest_AppId";

        Response response;

        response = RestAccess.get( RestAccess.BASE_URL + BCResource.SERVICE_PATH + "/validate/" + appId );
        assertResponseStatus( 200, response );
        assertThat( "OK", equalToIgnoringWhiteSpace( response.getResponseBody() ) );

        invalidateAppId( appId, "Expired" );

        // validate service always returns 200, the actual result is in the response body
        response = RestAccess.get( RestAccess.BASE_URL + BCResource.SERVICE_PATH + "/validate/" + appId );
        assertResponseStatus( 200, response );
        assertThat( "Expired", equalToIgnoringWhiteSpace( response.getResponseBody() ) );
    }

    @Test
    public void testScan()
        throws Exception
    {
        String appId = "BCResourceTest_AppId";
        File saasScanFile = getScanResponseFile( appId );
        saasScanFile.delete();

        URL testScanResultUrl = getClass().getResource( "/BCResourceTest/scan.json" );
        FileUtils.copyFile( new File( testScanResultUrl.getFile() ), saasScanFile );

        Response response = RestAccess.put( RestAccess.BASE_URL + BCResource.SERVICE_PATH + "/scan/" + appId, "" );

        assertResponseStatus( 200, response );

        assertThat( FileUtils.fileRead( saasScanFile, "UTF-8" ), equalToIgnoringWhiteSpace( response.getResponseBody() ) );
    }

    @Test
    public void testReport()
        throws Exception
    {
        String appId = "BCResourceTest_AppId";
        String scanId = "BCResourceTest_ScanId";
        File saasReportFile = getReportResponseFile( appId, scanId );
        saasReportFile.delete();

        URL testReportResultUrl = getClass().getResource( "/BCResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        Response response =
            RestAccess.get( RestAccess.BASE_URL + BCResource.SERVICE_PATH + "/report/" + appId + "?scanId=" + scanId );

        assertResponseStatus( 200, response );

        assertThat( IOUtil.toByteArray( testReportResultUrl.openStream() ),
                    equalTo( IOUtil.toByteArray( response.getResponseBodyAsStream() ) ) );
    }

    @Test
    public void testArtifact()
        throws Exception
    {
        String scanId = "BCResourceTest_ScanId";

        String query = scanId + "?groupId=org.springframework&artifactId=spring-core&version=2.5.6";
        Response response = RestAccess.get( RestAccess.BASE_URL + BCResource.SERVICE_PATH + "/artifact/" + query );
        assertResponseStatus( 307, response );

        response = RestAccess.get( response.getHeader( "Location" ) );
        assertResponseStatus( 200, response );

        assertThat( query, equalToIgnoringWhiteSpace( response.getResponseBody() ) );
    }
}
