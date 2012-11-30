/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;
import java.net.URL;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.client.utils.ServletResult;

public class ReportClientTest
    extends AbstractBrainServiceTest
{
    @Test
    public void testEmbedReport()
        throws Exception
    {
        String appId = "ReportClientTest_AppId";
        String scanId = "ReportClientTest_ScanId";
        File saasReportFile = getReportResponseFile( appId, scanId );
        saasReportFile.delete();

        // The report is not available
        ReportClient reportClient = new ReportClient( brain.getClientConfiguration(), appId, scanId );
        ServletResult servletResult = reportClient.embedReport( "index.html" );
        Assert.assertEquals( 404, servletResult.status() );

        // Simulate that the report is available
        URL testReportFileUrl = getClass().getResource( "/ReportClientTest/report.zip" );
        FileUtils.copyFile( new File( testReportFileUrl.getFile() ), saasReportFile );
        servletResult = reportClient.embedReport( "index.html" );
        Assert.assertEquals( 200, servletResult.status() );
        String html = servletResult.text();
        Assert.assertNotNull( html );
        Assert.assertTrue( html, html.contains( "<html" ) );
        Assert.assertTrue( html, html.contains( "</html>" ) );

        servletResult = reportClient.embedReport( "does-not-exist.fata-morgana" );
        Assert.assertEquals( 404, servletResult.status() );
    }

    @Test
    public void testPrintReport()
        throws Exception
    {
        String appId = "ReportClientTest_AppId";
        String scanId = "ReportClientTest_ScanId";
        File saasReportFile = getReportResponseFile( appId, scanId );
        saasReportFile.delete();

        // The report is not available
        ReportClient reportClient = new ReportClient( brain.getClientConfiguration(), appId, scanId );
        ServletResult servletResult = reportClient.printReport( "ReportClientTest_ProjectName", 17 );
        Assert.assertEquals( 500, servletResult.status() );

        // Simulate that the report is available
        URL testReportFileUrl = getClass().getResource( "/ReportClientTest/report.zip" );
        FileUtils.copyFile( new File( testReportFileUrl.getFile() ), saasReportFile );
        servletResult = reportClient.printReport( "ReportClientTest_ProjectName", 17 );
        Assert.assertEquals( 200, servletResult.status() );
        byte[] pdf = servletResult.data();
        Assert.assertNotNull( pdf );
    }
}
