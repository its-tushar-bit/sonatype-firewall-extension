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
        final String appId = "ReportClientTest_AppId";
        final String scanId = "ReportClientTest_ScanId";
        final File saasReportFile = getReportResponseFile( appId, scanId );
        saasReportFile.delete();

        // The report is not available
        final ReportClient reportClient = new ReportClient( brain.getClientConfiguration(), appId, scanId );
        ServletResult servletResult = reportClient.embedReport( "index.html" );
        Assert.assertEquals( 404, servletResult.status() );

        // Simulate that the report is available
        final URL testReportFileUrl = getClass().getResource( "/ReportClientTest/report.zip" );
        FileUtils.copyFile( new File( testReportFileUrl.getFile() ), saasReportFile );
        servletResult = reportClient.embedReport( "index.html" );
        Assert.assertEquals( 200, servletResult.status() );
        final String html = servletResult.text();
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
        final String appId = "ReportClientTest_AppId";
        final String scanId = "ReportClientTest_ScanId";
        final File saasReportFile = getReportResponseFile( appId, scanId );
        saasReportFile.delete();

        // The report is not available
        final ReportClient reportClient = new ReportClient( brain.getClientConfiguration(), appId, scanId );
        ServletResult servletResult = reportClient.printReport( "ReportClientTest_ProjectName", 17 );
        Assert.assertEquals( 404, servletResult.status() );

        // Simulate that the report is available
        final URL testReportFileUrl = getClass().getResource( "/ReportClientTest/report.zip" );
        FileUtils.copyFile( new File( testReportFileUrl.getFile() ), saasReportFile );
        servletResult = reportClient.printReport( "ReportClientTest_ProjectName", 17 /* buildNumber */);
        Assert.assertEquals( 200, servletResult.status() );
        final byte[] pdf = servletResult.data();
        Assert.assertNotNull( pdf );
    }

    @Test
    public void testAugmentData()
        throws Exception
    {
        final String appId = "ReportClientTest_AppId";
        final String scanId = "ReportClientTest_ScanId";
        final File saasReportFile = getReportResponseFile( appId, scanId );
        saasReportFile.delete();
        final String jsonData = "[ { \"data\" : [ { \"principle\" : \"true\", \"scream\" : \"Eureka\" } ] } ]";

        final ReportClient reportClient = new ReportClient( brain.getClientConfiguration(), appId, scanId );
        final ServletResult servletResult =
            reportClient.augmentData( "physics.json", jsonData, "Archimedes" /* user */, "Syracuse" /* where */);
        Assert.assertEquals( 200, servletResult.status() );
    }

    @Test
    public void testAuditLog()
        throws Exception
    {
        final String appId = "ReportClientTest_AppId";
        final String scanId = "ReportClientTest_ScanId";
        final File saasReportFile = getReportResponseFile( appId, scanId );
        saasReportFile.delete();
        final String jsonData = "[ { \"data\" : [ { \"principle\" : \"true\", \"scream\" : \"Eureka\" } ] } ]";

        // Should get empty audit log because there is no data
        final ReportClient reportClient = new ReportClient( brain.getClientConfiguration(), appId, scanId );
        ServletResult servletResult = reportClient.auditLog( "physics.json", "{\"scream\" : \"Eureka\"}" /* key */);
        Assert.assertEquals( 200, servletResult.status() );
        byte[] auditLog = servletResult.data();
        Assert.assertNotNull( auditLog );
        Assert.assertTrue( auditLog.length == 0 );

        // Add some data
        servletResult =
            reportClient.augmentData( "physics.json", jsonData, "Archimedes" /* user */, "Syracuse" /* where */);
        Assert.assertEquals( 200, servletResult.status() );

        // Should get not empty audit log
        servletResult = reportClient.auditLog( "physics.json", "{\"scream\" : \"Eureka\"}" /* key */);
        Assert.assertEquals( 200, servletResult.status() );
        auditLog = servletResult.data();
        Assert.assertNotNull( auditLog );
        Assert.assertTrue( auditLog.length > 0 );
    }

    @Test
    public void testArtifactDetails()
        throws Exception
    {
        final String appId = "ReportClientTest_AppId";
        final String scanId = "ReportClientTest_ScanId";
        final File saasReportFile = getReportResponseFile( appId, scanId );
        saasReportFile.delete();

        final ReportClient reportClient = new ReportClient( brain.getClientConfiguration(), appId, scanId );
        final ServletResult servletResult = reportClient.artifactDetails( "groupId1", "artifactId1", "version1" );
        Assert.assertEquals( 200, servletResult.status() );
        final byte[] artifactDetails = servletResult.data();
        Assert.assertNotNull( artifactDetails );
        Assert.assertTrue( artifactDetails.length > 0 );
    }
}
