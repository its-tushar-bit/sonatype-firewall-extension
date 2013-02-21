/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.model.brain.ScanReceipt;

public class ScanClientTest
    extends AbstractBrainServiceTest
{

    private static final String APP_ID = "ScanClientTest_AppId";

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @AfterClass
    public static void afterClass()
    {
        DataSourceFactory.unloadAll();
    }

    @BeforeClass
    public static void createApplication()
    {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = new Application();
        application.setPublicId( APP_ID );
        applicationDAO.insert( application );
    }

    private void assertMatch( String pattern, String text )
    {
        assertTrue( text + " does not match pattern " + pattern, text != null && text.matches( pattern ) );
    }

    @Test
    public void testUploadCiScan_AllGood()
        throws Exception
    {
        Configuration config = brain.getClientConfiguration();
        ScanReceipt receipt = new ScanClient( config, APP_ID ).uploadCiScan( tmpDir.newFile( "scan.xml.gz" ) );
        assertEquals( "SCAN-ID", receipt.getScanId() );
    }

    @Test
    public void testUploaCiScan_InvalidAppId()
        throws Exception
    {
        Configuration config = brain.getClientConfiguration();
        try
        {
            new ScanClient( config, "invalid-id" ).uploadCiScan( tmpDir.newFile( "scan.xml.gz" ) );
            fail( "Upload should have failed due to invalid app ID" );
        }
        catch ( IOException e )
        {
            assertMatch( "(?i).*404.*", e.getMessage() );
        }
    }

    @Test
    public void testUploadRepoManScan_AllGood()
        throws Exception
    {
        Configuration config = brain.getClientConfiguration();
        ScanReceipt receipt = new ScanClient( config, APP_ID ).uploadRepoManScan( tmpDir.newFile( "scan.xml.gz" ) );
        assertEquals( "SCAN-ID", receipt.getScanId() );
        assertNotNull( receipt.getReportUrl() );
    }

    @Test
    public void testUploadRepoManScan_InvalidAppId()
        throws Exception
    {
        Configuration config = brain.getClientConfiguration();
        try
        {
            new ScanClient( config, "invalid-id" ).uploadRepoManScan( tmpDir.newFile( "scan.xml.gz" ) );
            fail( "Upload should have failed due to invalid app ID" );
        }
        catch ( IOException e )
        {
            assertMatch( "(?i).*404.*", e.getMessage() );
        }
    }

}
