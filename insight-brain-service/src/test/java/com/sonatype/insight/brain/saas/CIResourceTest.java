/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.stringContainsInOrder;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;

public class CIResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testValidate()
        throws Exception
    {
<<<<<<< HEAD:insight-brain-service/src/test/java/com/sonatype/insight/brain/saas/CIResourceTest.java
        final String appId = "CIResourceTest_AppId";
=======
        final String applicationPublicId = "BCResourceTest_AppId";
        Application application = new ApplicationDAO().getByPublicId( applicationPublicId );
        Assert.assertNull( application );
>>>>>>> Added condition type for labels INSIGHT-4046 INSIGHT-4048:insight-brain-service/src/test/java/com/sonatype/insight/brain/saas/BCResourceTest.java

        Response response = RestAccess.get( getServiceURL() + "/validate/" + applicationPublicId );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), equalTo( "OK" ) );

        invalidateAppId( applicationPublicId, "Expired" );

        // validate service always returns 200, the actual result is in the response body
        response = RestAccess.get( getServiceURL() + "/validate/" + applicationPublicId );
        assertResponseStatus( 200, response );
        assertThat( response.getResponseBody(), equalTo( "Expired" ) );
    }

    @Test
    public void testScan()
        throws Exception
    {
<<<<<<< HEAD:insight-brain-service/src/test/java/com/sonatype/insight/brain/saas/CIResourceTest.java
        final String appId = "CIResourceTest_AppId";
        final File saasScanFile = getScanResponseFile( appId );
=======
        final String applicationPublicId = "BCResourceTest_AppId";
        createApplication( applicationPublicId );
        final File saasScanFile = getScanResponseFile( applicationPublicId );
>>>>>>> Added condition type for labels INSIGHT-4046 INSIGHT-4048:insight-brain-service/src/test/java/com/sonatype/insight/brain/saas/BCResourceTest.java
        saasScanFile.delete();

        final URL testScanResultUrl = getClass().getResource( "/CIResourceTest/scan.json" );
        FileUtils.copyFile( new File( testScanResultUrl.getFile() ), saasScanFile );

        final Response response = RestAccess.put( getServiceURL() + "/scan/" + applicationPublicId, "" );

        assertResponseStatus( 200, response );

        assertThat( response.getResponseBody(), equalToIgnoringWhiteSpace( FileUtils.fileRead( saasScanFile, "UTF-8" ) ) );
    }

    @Test
    public void testReport()
        throws Exception
    {
<<<<<<< HEAD:insight-brain-service/src/test/java/com/sonatype/insight/brain/saas/CIResourceTest.java
        final String appId = "CIResourceTest_AppId";
        final String scanId = "CIResourceTest_ScanId";
        final File saasReportFile = getReportResponseFile( appId, scanId );
=======
        final String applicationPublicId = "BCResourceTest_AppId";
        createApplication( applicationPublicId );
        final String scanId = "BCResourceTest_ScanId";
        final File saasReportFile = getReportResponseFile( applicationPublicId, scanId );
>>>>>>> Added condition type for labels INSIGHT-4046 INSIGHT-4048:insight-brain-service/src/test/java/com/sonatype/insight/brain/saas/BCResourceTest.java
        saasReportFile.delete();

        final URL testReportResultUrl = getClass().getResource( "/CIResourceTest/report.zip" );
        FileUtils.copyFile( new File( testReportResultUrl.getFile() ), saasReportFile );

        final Response response =
            RestAccess.get( getServiceURL() + "/report/" + applicationPublicId + "?scanId=" + scanId );

        assertResponseStatus( 200, response );

        assertThat( IOUtil.toByteArray( response.getResponseBodyAsStream() ),
                    equalTo( IOUtil.toByteArray( testReportResultUrl.openStream() ) ) );
    }

    @Test
    public void testArtifact()
        throws Exception
    {
<<<<<<< HEAD:insight-brain-service/src/test/java/com/sonatype/insight/brain/saas/CIResourceTest.java
        final String scanId = "CIResourceTest_ScanId";
=======
        final String applicationPublicId = "BCResourceTest_AppId";
        Application application = createApplication( applicationPublicId );
        String appId = application.getId();
        final String scanId = "BCResourceTest_ScanId";
        File scanDir = brain.getInsightWork().getReportDir( appId, scanId );
        scanDir.mkdirs();
>>>>>>> Added condition type for labels INSIGHT-4046 INSIGHT-4048:insight-brain-service/src/test/java/com/sonatype/insight/brain/saas/BCResourceTest.java

        final String query = scanId + "?groupId=org.springframework&artifactId=spring-core&version=2.5.6";
        Response response = RestAccess.get( getServiceURL() + "/artifact/" + query );
        assertResponseStatus( 307, response );

        response = RestAccess.get( response.getHeader( "Location" ) );
        assertResponseStatus( 200, response );

        assertThat( response.getResponseBody(), stringContainsInOrder( Arrays.asList( "\"groupId\"",
                                                                                      "\"org.springframework\"",
                                                                                      "\"artifactId\"",
                                                                                      "\"spring-core\"", "\"version\"",
                                                                                      "\"2.5.6\"" ) ) );
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + CIResource.SERVICE_PATH;
    }
}
