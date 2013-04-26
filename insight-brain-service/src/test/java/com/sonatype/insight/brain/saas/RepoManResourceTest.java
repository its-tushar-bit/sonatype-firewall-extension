/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.CLMEnforcementPoint;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class RepoManResourceTest
    extends AbstractResourceTest
{

    private String getServiceURL()
    {
        return getRestBaseUrl() + RepoManResource.SERVICE_PATH;
    }

    @Test
    public void testUploadScan()
        throws Exception
    {
        final String applicationPublicId = "RepoManResourceTest_AppId";
        final String licenseFingerprint = "RepoManResourceTest_LicenseFingerprint";
        createApplication( applicationPublicId );
        getLicenseFingerprinter().setDummyLicenseFingerprint( licenseFingerprint );

        final File saasScanFile = getScanResponseFile( licenseFingerprint );
        saasScanFile.delete();

        final URL testScanResultUrl = getClass().getResource( "/RepoManResourceTest/scan.json" );
        FileUtils.copyFile( new File( testScanResultUrl.getFile() ), saasScanFile );

        final Response response = RestAccess.put( getServiceURL() + "/scan/" + applicationPublicId, "" );

        assertResponseStatus( 200, response );

        ScanReceipt scanReceipt = JsonHelpers.fromJson( response.getResponseBody(), ScanReceipt.class );
        assertNotNull( scanReceipt );
        assertEquals( "f75365d9d93b4f1ea2dd8457a25dc44d", scanReceipt.getScanId() );
        assertEquals( Long.valueOf( 30 ), scanReceipt.getTimeToReport() );
        assertEquals( "rest/report/RepoManResourceTest_AppId/f75365d9d93b4f1ea2dd8457a25dc44d/embedReport/",
                      scanReceipt.getReportUrl() );
    }
    
    @Test
    public void testUploadScan_Unlicensed()
        throws Exception
    {
        uninstallLicense();
        Response response = RestAccess.put( getServiceURL() + "/scan/unlicensedappid", "" );
        assertResponseStatus( 402, response );
    }

    @Test
    public void testUploadScan_EnforcementPointUnlicensed()
        throws Exception
    {
        //note this enforcement point should not apply to this request
        getLicenseManager().setEnforcementPoints( CLMEnforcementPoint.Build );

        Response response = RestAccess.put( getServiceURL() + "/scan/unlicensedappid", "" );
        assertResponseStatus( 402, response );
    }
}
