/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

import java.io.File;
import java.net.URL;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;

public class RepoManResourceTest
    extends AbstractResourceTest
{

    private String getServiceURL()
    {
        return getRestBaseUrl() + RepoManResource.SERVICE_PATH;
    }

    @Test
    public void testScan()
        throws Exception
    {
        final String applicationPublicId = "RepoManResourceTest_AppId";
        createApplication( applicationPublicId );
        final File saasScanFile = getScanResponseFile( applicationPublicId );
        saasScanFile.delete();

        final URL testScanResultUrl = getClass().getResource( "/RepoManResourceTest/scan.json" );
        FileUtils.copyFile( new File( testScanResultUrl.getFile() ), saasScanFile );

        final Response response = RestAccess.put( getServiceURL() + "/scan/" + applicationPublicId, "" );

        assertResponseStatus( 200, response );

        assertThat( response.getResponseBody(), equalToIgnoringWhiteSpace( FileUtils.fileRead( saasScanFile, "UTF-8" ) ) );
    }

}
